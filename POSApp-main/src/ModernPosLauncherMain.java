import java.awt.Desktop;
import java.io.InputStream;
import java.net.BindException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

public class ModernPosLauncherMain
{
    public static void main(String[] args) throws Exception
    {
        configureRuntime();

        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();

        ProductRepository productRepository = new ProductRepository(databaseManager);
        productRepository.seedDefaultsIfEmpty();

        InventoryRepository inventoryRepository = new InventoryRepository(databaseManager, productRepository);
        SaleRepository saleRepository = new SaleRepository(databaseManager, productRepository, inventoryRepository);

        UserRepository userRepository = new UserRepository(databaseManager);
        userRepository.seedDefaultsIfEmpty();

        int port = findAvailablePort(resolvePort());
        PosApiServer server = startServer(
            port,
            databaseManager,
            productRepository,
            inventoryRepository,
            saleRepository,
            userRepository
        );

        String baseUrl = server.getBaseUrl();
        System.out.println("JavaPOS web launcher running at " + baseUrl);
        for (String url : NetworkUtils.getAccessibleUrls(port))
        {
            System.out.println("Access URL: " + url);
        }
        waitForHealth(baseUrl);
        openBrowser(baseUrl);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        new CountDownLatch(1).await();
    }

    private static PosApiServer startServer(
        int port,
        DatabaseManager databaseManager,
        ProductRepository productRepository,
        InventoryRepository inventoryRepository,
        SaleRepository saleRepository,
        UserRepository userRepository
    ) throws Exception
    {
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
        return server;
    }

    private static void openBrowser(String baseUrl)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(URI.create(baseUrl));
            }
        }
        catch (Exception ex)
        {
            System.out.println("Open the browser manually: " + baseUrl);
        }
    }

    private static void waitForHealth(String baseUrl)
    {
        String healthUrl = baseUrl + "api/health";
        for (int attempt = 0; attempt < 45; attempt++)
        {
            try (InputStream ignored = new URL(healthUrl).openStream())
            {
                return;
            }
            catch (Exception ex)
            {
                try
                {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException interruptedException)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        System.out.println("Health check did not respond in time. Open the browser manually: " + baseUrl);
    }

    private static void configureRuntime()
    {
        PosApiMain.configureRuntime();
    }

    private static int resolvePort()
    {
        return PosApiMain.resolvePort();
    }

    private static int findAvailablePort(int preferredPort) throws Exception
    {
        for (int offset = 0; offset < 20; offset++)
        {
            int candidate = preferredPort + offset;
            try
            {
                var probe = new java.net.ServerSocket();
                probe.setReuseAddress(true);
                probe.bind(new java.net.InetSocketAddress("127.0.0.1", candidate));
                probe.close();
                return candidate;
            }
            catch (BindException ex)
            {
                // Try the next port.
            }
        }

        throw new IllegalStateException("Unable to find a free local port starting at " + preferredPort + ".");
    }
}
