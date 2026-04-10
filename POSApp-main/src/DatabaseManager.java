import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager
{
    private static final String DATABASE_FILE_NAME = "javapos.db";
    private final String jdbcUrl;
    private final Path databasePath;
    private final Path appDirectory;

    public DatabaseManager()
    {
        this.appDirectory = resolveApplicationDataDirectory();
        this.databasePath = appDirectory.resolve(DATABASE_FILE_NAME);
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toString();
    }

    public Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(jdbcUrl);
    }

    public void initialize() throws SQLException
    {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement())
        {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS products ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE,"
                    + "price DECIMAL(10,2) NOT NULL,"
                    + "category TEXT NOT NULL,"
                    + "display_order INTEGER NOT NULL,"
                    + "active INTEGER NOT NULL DEFAULT 1,"
                    + "stock_quantity INTEGER NOT NULL DEFAULT 0,"
                    + "barcode TEXT"
                    + ")"
            );
            ensureColumnExists(statement, "products", "stock_quantity", "INTEGER NOT NULL DEFAULT 0");
            ensureColumnExists(statement, "products", "barcode", "TEXT");
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sales ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "created_at TEXT NOT NULL,"
                    + "payment_method TEXT NOT NULL,"
                    + "subtotal DECIMAL(10,2) NOT NULL,"
                    + "tax DECIMAL(10,2) NOT NULL,"
                    + "total DECIMAL(10,2) NOT NULL,"
                    + "cash_amount DECIMAL(10,2),"
                    + "change_amount DECIMAL(10,2),"
                    + "status TEXT NOT NULL DEFAULT 'COMPLETED',"
                    + "related_sale_id INTEGER"
                    + ")"
            );
            ensureColumnExists(statement, "sales", "status", "TEXT NOT NULL DEFAULT 'COMPLETED'");
            ensureColumnExists(statement, "sales", "related_sale_id", "INTEGER");
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE,"
                    + "password TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "active INTEGER NOT NULL DEFAULT 1,"
                    + "must_change_password INTEGER NOT NULL DEFAULT 0"
                    + ")"
            );
            ensureColumnExists(statement, "users", "must_change_password", "INTEGER NOT NULL DEFAULT 0");
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sale_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "sale_id INTEGER NOT NULL,"
                    + "product_name TEXT NOT NULL,"
                    + "unit_price DECIMAL(10,2) NOT NULL,"
                    + "quantity INTEGER NOT NULL,"
                    + "line_total DECIMAL(10,2) NOT NULL,"
                    + "FOREIGN KEY (sale_id) REFERENCES sales(id)"
                    + ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS stock_movements ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "product_id INTEGER NOT NULL,"
                    + "product_name TEXT NOT NULL,"
                    + "movement_type TEXT NOT NULL,"
                    + "quantity_delta INTEGER NOT NULL,"
                    + "stock_before INTEGER NOT NULL,"
                    + "stock_after INTEGER NOT NULL,"
                    + "note TEXT,"
                    + "created_at TEXT NOT NULL,"
                    + "FOREIGN KEY (product_id) REFERENCES products(id)"
                    + ")"
            );
        }
    }

    private void ensureColumnExists(Statement statement, String tableName, String columnName, String columnDefinition) throws SQLException
    {
        try (ResultSetAdapter result = new ResultSetAdapter(statement.executeQuery("PRAGMA table_info(" + tableName + ")")))
        {
            while (result.next())
            {
                if (columnName.equalsIgnoreCase(result.getString("name")))
                {
                    return;
                }
            }
        }

        statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
    }

    private Path resolveApplicationDataDirectory()
    {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path resolvedDirectory;

        if (localAppData != null && !localAppData.trim().isEmpty())
        {
            resolvedDirectory = Paths.get(localAppData, "JavaPOS");
        }
        else
        {
            resolvedDirectory = Paths.get(System.getProperty("user.home"), ".javapos");
        }

        try
        {
            Files.createDirectories(resolvedDirectory);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to create application data directory: " + resolvedDirectory, ex);
        }

        return resolvedDirectory;
    }

    public Path getDatabasePath()
    {
        return databasePath;
    }

    public Path getApplicationDataDirectory()
    {
        return appDirectory;
    }

    private static final class ResultSetAdapter implements AutoCloseable
    {
        private final java.sql.ResultSet resultSet;

        private ResultSetAdapter(java.sql.ResultSet resultSet)
        {
            this.resultSet = resultSet;
        }

        private boolean next() throws SQLException
        {
            return resultSet.next();
        }

        private String getString(String columnLabel) throws SQLException
        {
            return resultSet.getString(columnLabel);
        }

        @Override
        public void close() throws SQLException
        {
            resultSet.close();
        }
    }
}
