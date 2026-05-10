import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductRepository
{
    private final DatabaseManager databaseManager;

    public ProductRepository(DatabaseManager databaseManager)
    {
        this.databaseManager = databaseManager;
    }

    public void seedDefaultsIfEmpty() throws SQLException
    {
        if (!findAll().isEmpty())
        {
            return;
        }

        save(new Product(null, "Still Water", new BigDecimal("12.50"), "DRINK", 1, true, 50, "100001"));
        save(new Product(null, "Large Pizza", new BigDecimal("108.50"), "MAIN", 2, true, 20, "100002"));
        save(new Product(null, "Chocolate Milkshake", new BigDecimal("14.50"), "DRINK", 3, true, 30, "100003"));
        save(new Product(null, "Orange Juice", new BigDecimal("14.50"), "DRINK", 4, true, 30, "100004"));
        save(new Product(null, "Bubblegum Milkshake", new BigDecimal("14.50"), "DRINK", 5, true, 25, "100005"));
        save(new Product(null, "Strawberry Milkshake", new BigDecimal("14.50"), "DRINK", 6, true, 25, "100006"));
        save(new Product(null, "Pasta", new BigDecimal("40.00"), "MAIN", 7, true, 18, "100007"));
        save(new Product(null, "Chicken Burger", new BigDecimal("45.50"), "MAIN", 8, true, 18, "100008"));
        save(new Product(null, "Cappuccino", new BigDecimal("15.00"), "DRINK", 9, true, 40, "100009"));
        save(new Product(null, "Vanilla Cake", new BigDecimal("20.00"), "DESSERT", 10, true, 15, "100010"));
        save(new Product(null, "Ribs", new BigDecimal("60.50"), "MAIN", 11, true, 12, "100011"));
        save(new Product(null, "Coffee", new BigDecimal("15.00"), "DRINK", 12, true, 40, "100012"));
        save(new Product(null, "Red Velvet Cake", new BigDecimal("20.00"), "DESSERT", 13, true, 15, "100013"));
        save(new Product(null, "Vanilla Milkshake", new BigDecimal("14.50"), "DRINK", 14, true, 25, "100014"));
        save(new Product(null, "Beef Burger", new BigDecimal("45.50"), "MAIN", 15, true, 18, "100015"));
        save(new Product(null, "Chocolate Cake", new BigDecimal("20.00"), "DESSERT", 16, true, 15, "100016"));
        save(new Product(null, "Hake Fish", new BigDecimal("35.50"), "SEAFOOD", 17, true, 10, "100017"));
        save(new Product(null, "Prawns", new BigDecimal("80.00"), "SEAFOOD", 18, true, 8, "100018"));
    }

    public List<Product> findMenuProducts(int limit) throws SQLException
    {
        List<Product> products = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, price, category, display_order, active, stock_quantity, barcode "
                    + "FROM products WHERE active = 1 ORDER BY display_order ASC, name ASC LIMIT ?"
            )
        )
        {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    products.add(mapProduct(resultSet));
                }
            }
        }

        return products;
    }

    public List<Product> findAll() throws SQLException
    {
        List<Product> products = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, price, category, display_order, active, stock_quantity, barcode FROM products ORDER BY display_order ASC, name ASC"
            );
            ResultSet resultSet = statement.executeQuery()
        )
        {
            while (resultSet.next())
            {
                products.add(mapProduct(resultSet));
            }
        }

        return products;
    }

    public Product save(Product product) throws SQLException
    {
        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO products(name, price, category, display_order, active, stock_quantity, barcode) VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
        )
        {
            statement.setString(1, product.getName());
            statement.setBigDecimal(2, product.getUnitPrice());
            statement.setString(3, product.getCategory());
            statement.setInt(4, product.getDisplayOrder());
            statement.setInt(5, product.isActive() ? 1 : 0);
            statement.setInt(6, product.getStockQuantity());
            statement.setString(7, product.getBarcode());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    return new Product(
                        generatedKeys.getInt(1),
                        product.getName(),
                        product.getUnitPrice(),
                        product.getCategory(),
                        product.getDisplayOrder(),
                        product.isActive(),
                        product.getStockQuantity(),
                        product.getBarcode()
                    );
                }
            }
        }

        return product;
    }

    public void update(Product product) throws SQLException
    {
        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "UPDATE products SET name = ?, price = ?, category = ?, display_order = ?, active = ?, stock_quantity = ?, barcode = ? WHERE id = ?"
            )
        )
        {
            statement.setString(1, product.getName());
            statement.setBigDecimal(2, product.getUnitPrice());
            statement.setString(3, product.getCategory());
            statement.setInt(4, product.getDisplayOrder());
            statement.setInt(5, product.isActive() ? 1 : 0);
            statement.setInt(6, product.getStockQuantity());
            statement.setString(7, product.getBarcode());
            statement.setInt(8, product.getId());
            statement.executeUpdate();
        }
    }

    public Product findById(Connection connection, int id) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id, name, price, category, display_order, active, stock_quantity, barcode FROM products WHERE id = ?"
        ))
        {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (resultSet.next())
                {
                    return mapProduct(resultSet);
                }
            }
        }

        return null;
    }

    public Product findById(int id) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            return findById(connection, id);
        }
    }

    public void updateStock(Connection connection, int productId, int newStockQuantity) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE products SET stock_quantity = ? WHERE id = ?"
        ))
        {
            statement.setInt(1, newStockQuantity);
            statement.setInt(2, productId);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException
    {
        try (Connection connection = databaseManager.getConnection())
        {
            if (hasStockMovementHistory(connection, id))
            {
                deactivate(connection, id);
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE id = ?"))
            {
                statement.setInt(1, id);
                statement.executeUpdate();
            }
        }
    }

    private boolean hasStockMovementHistory(Connection connection, int productId) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM stock_movements WHERE product_id = ?"
        ))
        {
            statement.setInt(1, productId);
            try (ResultSet resultSet = statement.executeQuery())
            {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void deactivate(Connection connection, int productId) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE products SET active = 0 WHERE id = ?"
        ))
        {
            statement.setInt(1, productId);
            statement.executeUpdate();
        }
    }

    public List<Product> searchActiveProducts(String query, int limit) throws SQLException
    {
        List<Product> products = new ArrayList<>();

        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, price, category, display_order, active, stock_quantity, barcode "
                    + "FROM products WHERE active = 1 AND (LOWER(name) LIKE ? OR barcode LIKE ?) "
                    + "ORDER BY CASE WHEN barcode = ? THEN 0 ELSE 1 END, name ASC LIMIT ?"
            )
        )
        {
            String likeQuery = "%" + query.toLowerCase() + "%";
            statement.setString(1, likeQuery);
            statement.setString(2, "%" + query + "%");
            statement.setString(3, query);
            statement.setInt(4, limit);
            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    products.add(mapProduct(resultSet));
                }
            }
        }

        return products;
    }

    private Product mapProduct(ResultSet resultSet) throws SQLException
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
