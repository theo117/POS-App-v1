import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaleRepository
{
    private static final DateTimeFormatter DB_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public SaleRepository(DatabaseManager databaseManager, ProductRepository productRepository, InventoryRepository inventoryRepository)
    {
        this.databaseManager = databaseManager;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public void saveSale(List<CartItem> cartItems, CartTotals totals, String paymentMethod, BigDecimal cashAmount, BigDecimal changeAmount) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                validateAndReserveStock(connection, cartItems);
                int saleId = insertSale(connection, totals, paymentMethod, cashAmount, changeAmount, "COMPLETED", null);
                insertSaleItems(connection, saleId, cartItems);
                connection.commit();
            }
            catch (SQLException ex)
            {
                connection.rollback();
                throw ex;
            }
            finally
            {
                connection.setAutoCommit(true);
            }
        }
    }

    public void refundSale(int saleId) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                SaleRecord originalSale = findSaleById(connection, saleId);
                if (originalSale == null)
                {
                    throw new SQLException("Sale not found.");
                }
                if (!"COMPLETED".equals(originalSale.getStatus()))
                {
                    throw new SQLException("Only completed sales can be refunded.");
                }

                List<SaleItemRecord> originalItems = findSaleItems(connection, saleId);
                int refundSaleId = insertSale(
                    connection,
                    new CartTotals(originalSale.getSubtotal().negate(), originalSale.getTax().negate(), originalSale.getTotal().negate()),
                    originalSale.getPaymentMethod(),
                    originalSale.getCashAmount() == null ? BigDecimal.ZERO : originalSale.getCashAmount().negate(),
                    originalSale.getChangeAmount() == null ? BigDecimal.ZERO : originalSale.getChangeAmount().negate(),
                    "REFUND",
                    originalSale.getId()
                );
                insertRefundSaleItems(connection, refundSaleId, originalItems);
                markSaleAsRefunded(connection, saleId);
                restoreStock(connection, originalItems);
                connection.commit();
            }
            catch (SQLException ex)
            {
                connection.rollback();
                throw ex;
            }
            finally
            {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<SaleRecord> findRecentSales(int limit) throws SQLException
    {
        return findSales(null, null, limit);
    }

    public List<SaleRecord> findSales(LocalDate startDate, LocalDate endDate, int limit) throws SQLException
    {
        List<SaleRecord> sales = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, created_at, payment_method, subtotal, tax, total, cash_amount, change_amount, status, related_sale_id "
                    + "FROM sales WHERE (? IS NULL OR created_at >= ?) "
                    + "AND (? IS NULL OR created_at < ?) "
                    + "ORDER BY id DESC LIMIT ?"
            )
        )
        {
            bindDateRange(statement, startDate, endDate);
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    sales.add(mapSale(resultSet));
                }
            }
        }

        return sales;
    }

    public List<SaleItemRecord> findSaleItems(int saleId) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            return findSaleItems(connection, saleId);
        }
    }

    public SalesSummary getSalesSummary(LocalDate startDate, LocalDate endDate) throws SQLException
    {
        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) AS transaction_count, "
                    + "COALESCE(SUM(subtotal), 0) AS subtotal, "
                    + "COALESCE(SUM(tax), 0) AS tax, "
                    + "COALESCE(SUM(total), 0) AS total "
                    + "FROM sales WHERE (? IS NULL OR created_at >= ?) "
                    + "AND (? IS NULL OR created_at < ?)"
            )
        )
        {
            bindDateRange(statement, startDate, endDate);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return new SalesSummary(
                        resultSet.getInt("transaction_count"),
                        resultSet.getBigDecimal("subtotal"),
                        resultSet.getBigDecimal("tax"),
                        resultSet.getBigDecimal("total")
                    );
                }
            }
        }

        return new SalesSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public List<PaymentSummary> getPaymentSummaries(LocalDate startDate, LocalDate endDate) throws SQLException
    {
        List<PaymentSummary> summaries = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT payment_method, COUNT(*) AS transaction_count, COALESCE(SUM(total), 0) AS total_amount "
                    + "FROM sales WHERE (? IS NULL OR created_at >= ?) "
                    + "AND (? IS NULL OR created_at < ?) "
                    + "GROUP BY payment_method ORDER BY total_amount DESC, payment_method ASC"
            )
        )
        {
            bindDateRange(statement, startDate, endDate);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    summaries.add(new PaymentSummary(
                        resultSet.getString("payment_method"),
                        resultSet.getInt("transaction_count"),
                        resultSet.getBigDecimal("total_amount")
                    ));
                }
            }
        }

        return summaries;
    }

    public List<TopProductSummary> getTopProducts(LocalDate startDate, LocalDate endDate, int limit) throws SQLException
    {
        List<TopProductSummary> products = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT si.product_name, SUM(si.quantity) AS quantity_sold, COALESCE(SUM(si.line_total), 0) AS revenue "
                    + "FROM sale_items si "
                    + "INNER JOIN sales s ON s.id = si.sale_id "
                    + "WHERE (? IS NULL OR s.created_at >= ?) "
                    + "AND (? IS NULL OR s.created_at < ?) "
                    + "GROUP BY si.product_name "
                    + "ORDER BY quantity_sold DESC, revenue DESC, si.product_name ASC "
                    + "LIMIT ?"
            )
        )
        {
            bindDateRange(statement, startDate, endDate);
            statement.setInt(5, limit);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    products.add(new TopProductSummary(
                        resultSet.getString("product_name"),
                        resultSet.getInt("quantity_sold"),
                        resultSet.getBigDecimal("revenue")
                    ));
                }
            }
        }

        return products;
    }

    private int insertSale(Connection connection, CartTotals totals, String paymentMethod, BigDecimal cashAmount, BigDecimal changeAmount, String status, Integer relatedSaleId) throws SQLException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sales(created_at, payment_method, subtotal, tax, total, cash_amount, change_amount, status, related_sale_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
        )
        {
            statement.setString(1, LocalDateTime.now().format(DB_TIME_FORMAT));
            statement.setString(2, paymentMethod);
            statement.setBigDecimal(3, totals.getSubtotal());
            statement.setBigDecimal(4, totals.getTax());
            statement.setBigDecimal(5, totals.getTotal());
            statement.setBigDecimal(6, cashAmount.signum() == 0 ? null : cashAmount);
            statement.setBigDecimal(7, changeAmount.signum() == 0 ? null : changeAmount);
            statement.setString(8, status);
            if (relatedSaleId == null)
            {
                statement.setNull(9, Types.INTEGER);
            }
            else
            {
                statement.setInt(9, relatedSaleId);
            }
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Unable to create sale record.");
    }

    private void insertSaleItems(Connection connection, int saleId, List<CartItem> cartItems) throws SQLException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sale_items(sale_id, product_name, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?)"
            )
        )
        {
            for (CartItem item : cartItems)
            {
                statement.setInt(1, saleId);
                statement.setString(2, item.getProduct().getName());
                statement.setBigDecimal(3, item.getProduct().getUnitPrice());
                statement.setInt(4, item.getQuantity());
                statement.setBigDecimal(5, item.getLineTotal());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private void insertRefundSaleItems(Connection connection, int refundSaleId, List<SaleItemRecord> originalItems) throws SQLException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sale_items(sale_id, product_name, unit_price, quantity, line_total) VALUES (?, ?, ?, ?, ?)"
            )
        )
        {
            for (SaleItemRecord item : originalItems)
            {
                statement.setInt(1, refundSaleId);
                statement.setString(2, item.getProductName());
                statement.setBigDecimal(3, item.getUnitPrice());
                statement.setInt(4, -item.getQuantity());
                statement.setBigDecimal(5, item.getLineTotal().negate());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void validateAndReserveStock(Connection connection, List<CartItem> cartItems) throws SQLException
    {
        for (CartItem item : cartItems)
        {
            Product storedProduct = productRepository.findById(connection, item.getProduct().getId());
            if (storedProduct == null)
            {
                throw new SQLException("Product no longer exists: " + item.getProduct().getName());
            }

            if (storedProduct.getStockQuantity() < item.getQuantity())
            {
                throw new SQLException("Insufficient stock for " + storedProduct.getName() + ". Available: " + storedProduct.getStockQuantity());
            }
        }

        for (CartItem item : cartItems)
        {
            Product storedProduct = productRepository.findById(connection, item.getProduct().getId());
            int updatedStock = storedProduct.getStockQuantity() - item.getQuantity();
            productRepository.updateStock(connection, storedProduct.getId(), updatedStock);
            inventoryRepository.recordSaleMovement(connection, storedProduct, item.getQuantity());
        }
    }

    private void restoreStock(Connection connection, List<SaleItemRecord> originalItems) throws SQLException
    {
        for (SaleItemRecord item : originalItems)
        {
            Product product = findProductByName(connection, item.getProductName());
            if (product == null)
            {
                throw new SQLException("Product no longer exists: " + item.getProductName());
            }

            int updatedStock = product.getStockQuantity() + item.getQuantity();
            productRepository.updateStock(connection, product.getId(), updatedStock);
            inventoryRepository.recordRefundMovement(connection, product, item.getQuantity());
        }
    }

    private void markSaleAsRefunded(Connection connection, int saleId) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE sales SET status = 'REFUNDED' WHERE id = ?"))
        {
            statement.setInt(1, saleId);
            statement.executeUpdate();
        }
    }

    private SaleRecord findSaleById(Connection connection, int saleId) throws SQLException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, created_at, payment_method, subtotal, tax, total, cash_amount, change_amount, status, related_sale_id FROM sales WHERE id = ?"
            )
        )
        {
            statement.setInt(1, saleId);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return mapSale(resultSet);
                }
            }
        }
        return null;
    }

    private List<SaleItemRecord> findSaleItems(Connection connection, int saleId) throws SQLException
    {
        List<SaleItemRecord> items = new ArrayList<>();

        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT product_name, unit_price, quantity, line_total FROM sale_items WHERE sale_id = ? ORDER BY id ASC"
            )
        )
        {
            statement.setInt(1, saleId);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    items.add(new SaleItemRecord(
                        resultSet.getString("product_name"),
                        resultSet.getBigDecimal("unit_price"),
                        resultSet.getInt("quantity"),
                        resultSet.getBigDecimal("line_total")
                    ));
                }
            }
        }

        return items;
    }

    private Product findProductByName(Connection connection, String productName) throws SQLException
    {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, price, category, display_order, active, stock_quantity, barcode FROM products WHERE name = ?"
            )
        )
        {
            statement.setString(1, productName);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return new Product(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getString("category"),
                        resultSet.getInt("display_order"),
                        resultSet.getInt("active") == 1,
                        resultSet.getInt("stock_quantity"),
                        resultSet.getString("barcode")
                    );
                }
            }
        }
        return null;
    }

    private SaleRecord mapSale(ResultSet resultSet) throws SQLException
    {
        return new SaleRecord(
            resultSet.getInt("id"),
            LocalDateTime.parse(resultSet.getString("created_at"), DB_TIME_FORMAT),
            resultSet.getString("payment_method"),
            resultSet.getBigDecimal("subtotal"),
            resultSet.getBigDecimal("tax"),
            resultSet.getBigDecimal("total"),
            resultSet.getBigDecimal("cash_amount"),
            resultSet.getBigDecimal("change_amount"),
            resultSet.getString("status"),
            (Integer) resultSet.getObject("related_sale_id")
        );
    }

    private void bindDateRange(PreparedStatement statement, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        setDateParameter(statement, 1, startDate == null ? null : startDate.atStartOfDay());
        setDateParameter(statement, 2, startDate == null ? null : startDate.atStartOfDay());
        setDateParameter(statement, 3, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
        setDateParameter(statement, 4, endDate == null ? null : endDate.plusDays(1).atStartOfDay());
    }

    private void setDateParameter(PreparedStatement statement, int parameterIndex, LocalDateTime value) throws SQLException
    {
        if (value == null)
        {
            statement.setNull(parameterIndex, Types.VARCHAR);
        }
        else
        {
            statement.setString(parameterIndex, value.format(DB_TIME_FORMAT));
        }
    }
}
