import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InventoryRepository
{
    private static final DateTimeFormatter DB_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DatabaseManager databaseManager;
    private final ProductRepository productRepository;

    public InventoryRepository(DatabaseManager databaseManager, ProductRepository productRepository)
    {
        this.databaseManager = databaseManager;
        this.productRepository = productRepository;
    }

    public void adjustStock(int productId, int quantityDelta, String movementType, String note) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            connection.setAutoCommit(false);
            try
            {
                Product product = productRepository.findById(connection, productId);
                if (product == null)
                {
                    throw new SQLException("Product not found.");
                }

                int stockBefore = product.getStockQuantity();
                int stockAfter = stockBefore + quantityDelta;
                if (stockAfter < 0)
                {
                    throw new SQLException("Stock cannot go below zero.");
                }

                productRepository.updateStock(connection, productId, stockAfter);
                insertMovement(connection, product, movementType, quantityDelta, stockBefore, stockAfter, note);
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

    public List<Product> findLowStockProducts(int threshold) throws SQLException
    {
        List<Product> products = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, price, category, display_order, active, stock_quantity, barcode "
                    + "FROM products WHERE active = 1 AND stock_quantity <= ? ORDER BY stock_quantity ASC, name ASC"
            )
        )
        {
            statement.setInt(1, threshold);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    products.add(new Product(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("price"),
                        resultSet.getString("category"),
                        resultSet.getInt("display_order"),
                        resultSet.getInt("active") == 1,
                        resultSet.getInt("stock_quantity"),
                        resultSet.getString("barcode")
                    ));
                }
            }
        }

        return products;
    }

    public List<StockMovementRecord> findRecentMovements(int limit) throws SQLException
    {
        List<StockMovementRecord> movements = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT created_at, product_name, movement_type, quantity_delta, stock_before, stock_after, note "
                    + "FROM stock_movements ORDER BY id DESC LIMIT ?"
            )
        )
        {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    movements.add(new StockMovementRecord(
                        LocalDateTime.parse(resultSet.getString("created_at"), DB_TIME_FORMAT),
                        resultSet.getString("product_name"),
                        resultSet.getString("movement_type"),
                        resultSet.getInt("quantity_delta"),
                        resultSet.getInt("stock_before"),
                        resultSet.getInt("stock_after"),
                        resultSet.getString("note")
                    ));
                }
            }
        }

        return movements;
    }

    public void recordSaleMovement(Connection connection, Product product, int quantitySold) throws SQLException
    {
        int stockBefore = product.getStockQuantity();
        int stockAfter = stockBefore - quantitySold;
        insertMovement(connection, product, "SALE", -quantitySold, stockBefore, stockAfter, "Sale completed");
    }

    public void recordRefundMovement(Connection connection, Product product, int quantityRestored) throws SQLException
    {
        int stockBefore = product.getStockQuantity();
        int stockAfter = stockBefore + quantityRestored;
        insertMovement(connection, product, "REFUND", quantityRestored, stockBefore, stockAfter, "Sale refunded");
    }

    private void insertMovement(Connection connection, Product product, String movementType, int quantityDelta, int stockBefore, int stockAfter, String note) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO stock_movements(product_id, product_name, movement_type, quantity_delta, stock_before, stock_after, note, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ))
        {
            statement.setInt(1, product.getId());
            statement.setString(2, product.getName());
            statement.setString(3, movementType);
            statement.setInt(4, quantityDelta);
            statement.setInt(5, stockBefore);
            statement.setInt(6, stockAfter);
            statement.setString(7, note);
            statement.setString(8, LocalDateTime.now().format(DB_TIME_FORMAT));
            statement.executeUpdate();
        }
    }
}
