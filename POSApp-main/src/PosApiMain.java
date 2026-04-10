import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PosApiMain
{
    public static void main(String[] args) throws Exception
    {
        configureRuntime();
        int port = resolvePort();

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();

        ProductRepository productRepository = new ProductRepository(databaseManager);
        productRepository.seedDefaultsIfEmpty();

        InventoryRepository inventoryRepository = new InventoryRepository(databaseManager, productRepository);
        SaleRepository saleRepository = new SaleRepository(databaseManager, productRepository, inventoryRepository);

        UserRepository userRepository = new UserRepository(databaseManager);
        userRepository.seedDefaultsIfEmpty();

        PosApiServer server = new PosApiServer(
            port,
            databaseManager,
            productRepository,
            inventoryRepository,
            saleRepository,
            userRepository,
            new WebSessionManager()
        );
        server.start();

        System.out.println("JavaPOS modern UI running at " + server.getBaseUrl());
        System.out.println("Press Ctrl+C to stop.");
    }

    static void configureRuntime()
    {
        System.setProperty("java.security.egd", "file:/dev/urandom");
        System.setProperty("securerandom.source", "file:/dev/urandom");
        configureSqliteNativeLibrary();
    }

    private static void configureSqliteNativeLibrary()
    {
        if (System.getProperty("org.sqlite.lib.path") != null)
        {
            return;
        }

        String tempDirectory = System.getenv("TEMP");
        if (tempDirectory == null || tempDirectory.trim().isEmpty())
        {
            return;
        }

        try
        {
            Path tempPath = Path.of(tempDirectory);
            try (var paths = Files.list(tempPath))
            {
                Path sqliteLibrary = paths
                    .filter(path -> path.getFileName().toString().endsWith("-sqlitejdbc.dll"))
                    .findFirst()
                    .orElse(null);

                if (sqliteLibrary != null)
                {
                    System.setProperty("org.sqlite.lib.path", sqliteLibrary.getParent().toString());
                    System.setProperty("org.sqlite.lib.name", sqliteLibrary.getFileName().toString());
                }
            }
        }
        catch (IOException ex)
        {
            // Fall back to the driver's default extraction behavior.
        }
    }

    static int resolvePort()
    {
        String property = System.getProperty("javapos.port");
        if (property == null || property.trim().isEmpty())
        {
            return 8085;
        }

        try
        {
            return Integer.parseInt(property.trim());
        }
        catch (NumberFormatException ex)
        {
            return 8085;
        }
    }
}
