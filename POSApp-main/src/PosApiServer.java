import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class PosApiServer
{
    private static final BigDecimal TAX_RATE_PERCENT = new BigDecimal("15.0");
    private static final DateTimeFormatter RECEIPT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final int port;
    private final DatabaseManager databaseManager;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final WebSessionManager webSessionManager;
    private final Path staticDirectory;
    private final boolean useClasspathStaticAssets;
    private final ReceiptService receiptService;
    private HttpServer server;

    public PosApiServer(
        int port,
        DatabaseManager databaseManager,
        ProductRepository productRepository,
        InventoryRepository inventoryRepository,
        SaleRepository saleRepository,
        UserRepository userRepository,
        WebSessionManager webSessionManager
    )
    {
        this.port = port;
        this.databaseManager = databaseManager;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.webSessionManager = webSessionManager;
        this.staticDirectory = resolveStaticDirectory();
        this.useClasspathStaticAssets = this.staticDirectory == null;
        this.receiptService = new ReceiptService(RECEIPT_TIME_FORMAT);
    }

    public void start() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public void stop()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    public String getBaseUrl()
    {
        return "http://localhost:" + port + "/";
    }

    private Path resolveStaticDirectory()
    {
        Path[] candidates = new Path[] {
            Path.of("modern-ui"),
            Path.of("POSApp-main", "modern-ui")
        };

        for (Path candidate : candidates)
        {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized))
            {
                return normalized;
            }
        }

        return null;
    }

    private final class ApiHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            try
            {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                Map<String, String> queryParameters = parseQuery(requestUri.getRawQuery());
                String method = exchange.getRequestMethod();
                UserAccount currentUser = authenticateRequest(exchange);

                if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path))
                {
                    writeJson(exchange, 200, login(exchange));
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && "/api/auth/logout".equals(path))
                {
                    requireAuthenticated(currentUser);
                    logout(exchange);
                    writeJson(exchange, 200, Map.of("message", "Logged out."));
                    return;
                }

                if ("GET".equalsIgnoreCase(method) && "/api/auth/session".equals(path))
                {
                    requireAuthenticated(currentUser);
                    writeJson(exchange, 200, sessionResponse(currentUser));
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && "/api/sales".equals(path))
                {
                    requireAuthenticated(currentUser);
                    writeJson(exchange, 200, createSale(exchange));
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && "/api/sales/refund".equals(path))
                {
                    requireAdmin(currentUser);
                    writeJson(exchange, 200, refundSale(exchange));
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && "/api/admin/products".equals(path))
                {
                    requireAdmin(currentUser);
                    writeJson(exchange, 200, saveProduct(exchange));
                    return;
                }

                if ("POST".equalsIgnoreCase(method) && "/api/admin/inventory/adjust".equals(path))
                {
                    requireAdmin(currentUser);
                    writeJson(exchange, 200, adjustInventory(exchange));
                    return;
                }

                if (!"GET".equalsIgnoreCase(method))
                {
                    writeJson(exchange, 405, Map.of("error", "Method not allowed."));
                    return;
                }

                if ("/api".equals(path) || "/api/".equals(path))
                {
                    requireAuthenticated(currentUser);
                    writeJson(exchange, 200, Map.of(
                        "name", "JavaPOS Modern API",
                        "version", "1",
                        "endpoints", List.of(
                            "/api/health",
                            "/api/dashboard",
                            "/api/products",
                            "/api/reports/today",
                            "/api/inventory/low-stock",
                            "/api/sales/recent",
                            "/api/sales"
                        )
                    ));
                    return;
                }

                if ("/api/health".equals(path))
                {
                    writeJson(exchange, 200, Map.of(
                        "status", "ok",
                        "databasePath", databaseManager.getDatabasePath().toString(),
                        "uiPath", useClasspathStaticAssets ? "classpath:/modern-ui" : staticDirectory.toString()
                    ));
                    return;
                }

                if ("/api/dashboard".equals(path))
                {
                    requireAuthenticated(currentUser);
                    writeJson(exchange, 200, buildDashboardResponse());
                    return;
                }

                if ("/api/products".equals(path))
                {
                    requireAuthenticated(currentUser);
                    String query = queryParameters.getOrDefault("query", "").trim();
                    int limit = parsePositiveInt(queryParameters.get("limit"), 48);
                    writeJson(exchange, 200, buildProductsResponse(query, limit));
                    return;
                }

                if ("/api/admin/products".equals(path))
                {
                    requireAdmin(currentUser);
                    List<Product> products = productRepository.findAll();
                    writeJson(exchange, 200, Map.of(
                        "count", products.size(),
                        "products", productsToList(products)
                    ));
                    return;
                }

                if ("/api/reports/today".equals(path))
                {
                    requireAuthenticated(currentUser);
                    writeJson(exchange, 200, buildTodayReport());
                    return;
                }

                if ("/api/inventory/low-stock".equals(path))
                {
                    requireAuthenticated(currentUser);
                    int threshold = parsePositiveInt(queryParameters.get("threshold"), 10);
                    writeJson(exchange, 200, buildLowStockResponse(threshold));
                    return;
                }

                if ("/api/sales/recent".equals(path))
                {
                    requireAuthenticated(currentUser);
                    int limit = parsePositiveInt(queryParameters.get("limit"), 12);
                    writeJson(exchange, 200, buildRecentSalesResponse(limit));
                    return;
                }

                if ("/api/sales/receipt".equals(path))
                {
                    requireAuthenticated(currentUser);
                    int saleId = parsePositiveInt(queryParameters.get("saleId"), -1);
                    if (saleId <= 0)
                    {
                        throw new IllegalArgumentException("A valid saleId is required.");
                    }
                    writeJson(exchange, 200, buildReceiptResponse(saleId));
                    return;
                }

                if ("/api/closeout/today".equals(path))
                {
                    requireAdmin(currentUser);
                    writeJson(exchange, 200, buildTodayCloseoutResponse());
                    return;
                }

                if ("/api/admin/inventory/movements".equals(path))
                {
                    requireAdmin(currentUser);
                    int limit = parsePositiveInt(queryParameters.get("limit"), 20);
                    writeJson(exchange, 200, Map.of("movements", stockMovementsToList(inventoryRepository.findRecentMovements(limit))));
                    return;
                }

                if ("/api/admin/runtime".equals(path))
                {
                    requireAdmin(currentUser);
                    writeJson(exchange, 200, buildRuntimeResponse());
                    return;
                }

                if ("/api/admin/backup".equals(path))
                {
                    requireAdmin(currentUser);
                    sendBackup(exchange);
                    return;
                }

                writeJson(exchange, 404, Map.of("error", "Not found."));
            }
            catch (IllegalArgumentException ex)
            {
                writeJson(exchange, 400, Map.of("error", ex.getMessage()));
            }
            catch (SecurityException ex)
            {
                int statusCode = "FORBIDDEN".equals(ex.getMessage()) ? 403 : 401;
                writeJson(exchange, statusCode, Map.of("error", statusCode == 403 ? "Forbidden." : "Authentication required."));
            }
            catch (SQLException ex)
            {
                writeJson(exchange, 500, Map.of("error", ex.getMessage()));
            }
            catch (Exception ex)
            {
                writeJson(exchange, 500, Map.of("error", "Unexpected server error.", "detail", ex.getMessage()));
            }
        }
    }

    private final class StaticHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()))
            {
                sendBytes(exchange, 405, "text/plain; charset=utf-8", "Method not allowed.".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String rawPath = exchange.getRequestURI().getPath();
            if (rawPath.startsWith("/api"))
            {
                sendBytes(exchange, 404, "text/plain; charset=utf-8", "Not found.".getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (useClasspathStaticAssets)
            {
                serveClasspathAsset(exchange, rawPath);
                return;
            }

            String relativePath = "/".equals(rawPath) ? "index.html" : rawPath.substring(1);
            Path target = staticDirectory.resolve(relativePath).normalize();
            if (!target.startsWith(staticDirectory) || !Files.exists(target) || Files.isDirectory(target))
            {
                target = staticDirectory.resolve("index.html");
            }

            sendBytes(exchange, 200, contentType(target), Files.readAllBytes(target));
        }
    }

    private void serveClasspathAsset(HttpExchange exchange, String rawPath) throws IOException
    {
        String resourcePath = "/".equals(rawPath) ? "modern-ui/index.html" : "modern-ui/" + rawPath.substring(1);
        InputStream stream = PosApiServer.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null)
        {
            stream = PosApiServer.class.getClassLoader().getResourceAsStream("modern-ui/index.html");
            resourcePath = "modern-ui/index.html";
        }

        if (stream == null)
        {
            sendBytes(exchange, 404, "text/plain; charset=utf-8", "Not found.".getBytes(StandardCharsets.UTF_8));
            return;
        }

        try (InputStream assetStream = stream)
        {
            sendBytes(exchange, 200, contentType(Path.of(resourcePath)), assetStream.readAllBytes());
        }
    }

    private Map<String, Object> buildDashboardResponse() throws SQLException
    {
        LocalDate today = LocalDate.now();
        SalesSummary todaySummary = saleRepository.getSalesSummary(today, today);
        List<PaymentSummary> paymentSummaries = saleRepository.getPaymentSummaries(today, today);
        List<TopProductSummary> topProducts = saleRepository.getTopProducts(today, today, 6);
        List<Product> lowStockProducts = inventoryRepository.findLowStockProducts(10);
        List<SaleRecord> recentSales = saleRepository.findRecentSales(8);
        List<Product> products = productRepository.findAll();

        LinkedHashSet<String> categories = new LinkedHashSet<>();
        int activeCount = 0;
        int totalStockUnits = 0;
        for (Product product : products)
        {
            categories.add(product.getCategory());
            totalStockUnits += product.getStockQuantity();
            if (product.isActive())
            {
                activeCount++;
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("store", Map.of(
            "productCount", products.size(),
            "activeProductCount", activeCount,
            "categoryCount", categories.size(),
            "stockUnits", totalStockUnits
        ));
        response.put("today", summaryToMap(todaySummary));
        response.put("payments", paymentSummariesToList(paymentSummaries));
        response.put("topProducts", topProductsToList(topProducts));
        response.put("lowStock", productsToList(lowStockProducts));
        response.put("recentSales", salesToList(recentSales));
        response.put("categories", new ArrayList<>(categories));
        return response;
    }

    private Map<String, Object> buildProductsResponse(String query, int limit) throws SQLException
    {
        List<Product> products = query.isEmpty()
            ? productRepository.findMenuProducts(limit)
            : productRepository.searchActiveProducts(query, limit);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("count", products.size());
        response.put("products", productsToList(products));
        return response;
    }

    private Map<String, Object> buildTodayReport() throws SQLException
    {
        LocalDate today = LocalDate.now();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("date", String.valueOf(today));
        response.put("summary", summaryToMap(saleRepository.getSalesSummary(today, today)));
        response.put("payments", paymentSummariesToList(saleRepository.getPaymentSummaries(today, today)));
        response.put("topProducts", topProductsToList(saleRepository.getTopProducts(today, today, 8)));
        return response;
    }

    private Map<String, Object> buildLowStockResponse(int threshold) throws SQLException
    {
        List<Product> products = inventoryRepository.findLowStockProducts(threshold);
        return Map.of(
            "threshold", threshold,
            "count", products.size(),
            "products", productsToList(products)
        );
    }

    private Map<String, Object> buildRecentSalesResponse(int limit) throws SQLException
    {
        List<SaleRecord> sales = saleRepository.findRecentSales(limit);
        return Map.of(
            "count", sales.size(),
            "sales", salesToList(sales)
        );
    }

    private Map<String, Object> buildReceiptResponse(int saleId) throws SQLException
    {
        SaleRecord matchedSale = saleRepository.findSaleById(saleId);
        if (matchedSale == null)
        {
            throw new IllegalArgumentException("Sale not found.");
        }

        List<SaleItemRecord> items = saleRepository.findSaleItems(saleId);
        return Map.of(
            "saleId", saleId,
            "receipt", receiptService.buildReceiptFromSale(matchedSale, items)
        );
    }

    private Map<String, Object> buildTodayCloseoutResponse() throws SQLException
    {
        LocalDate today = LocalDate.now();
        return Map.of(
            "date", String.valueOf(today),
            "summary", summaryToMap(saleRepository.getSalesSummary(today, today)),
            "payments", paymentSummariesToList(saleRepository.getPaymentSummaries(today, today)),
            "topProducts", topProductsToList(saleRepository.getTopProducts(today, today, 5)),
            "recentSales", salesToList(saleRepository.findSales(today, today, 20))
        );
    }

    private Map<String, Object> buildRuntimeResponse()
    {
        return Map.of(
            "databasePath", databaseManager.getDatabasePath().toString(),
            "applicationDataPath", databaseManager.getApplicationDataDirectory().toString(),
            "accessUrls", NetworkUtils.getAccessibleUrls(port)
        );
    }

    private Map<String, Object> createSale(HttpExchange exchange) throws IOException, SQLException
    {
        Map<String, String> parameters = parseQuery(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String paymentMethod = parameters.getOrDefault("paymentMethod", "Cash").trim();
        String itemsValue = parameters.getOrDefault("items", "").trim();
        String cashValue = parameters.getOrDefault("cashAmount", "").trim();

        if (itemsValue.isEmpty())
        {
            throw new IllegalArgumentException("Add at least one item before checkout.");
        }

        List<CartItem> cartItems = buildCartItems(itemsValue);
        if (cartItems.isEmpty())
        {
            throw new IllegalArgumentException("No valid cart items were provided.");
        }

        CartTotals totals = calculateTotals(cartItems);
        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal changeAmount = BigDecimal.ZERO;

        if ("Cash".equalsIgnoreCase(paymentMethod))
        {
            if (cashValue.isEmpty())
            {
                throw new IllegalArgumentException("Cash amount is required for cash payments.");
            }

            cashAmount = parseNonNegativeMoney(cashValue, "Cash amount");
            if (cashAmount.compareTo(totals.getTotal()) < 0)
            {
                throw new IllegalArgumentException("Cash amount is less than the order total.");
            }
            changeAmount = MoneyUtils.scale(cashAmount.subtract(totals.getTotal()));
            paymentMethod = "Cash";
        }

        saleRepository.saveSale(cartItems, totals, paymentMethod, cashAmount, changeAmount);
        String receipt = receiptService.buildReceipt(cartItems, totals, paymentMethod, cashAmount, changeAmount);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sale completed.");
        response.put("receipt", receipt);
        response.put("totals", summaryToMap(new SalesSummary(cartItems.size(), totals.getSubtotal(), totals.getTax(), totals.getTotal())));
        response.put("cashAmount", decimal(cashAmount));
        response.put("changeAmount", decimal(changeAmount));
        return response;
    }

    private Map<String, Object> refundSale(HttpExchange exchange) throws IOException, SQLException
    {
        Map<String, String> parameters = parseQuery(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        int saleId = parsePositiveInt(parameters.get("saleId"), -1);
        if (saleId <= 0)
        {
            throw new IllegalArgumentException("A valid saleId is required.");
        }

        saleRepository.refundSale(saleId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sale refunded.");
        response.put("saleId", saleId);
        response.put("closeout", buildTodayCloseoutResponse());
        return response;
    }

    private Map<String, Object> saveProduct(HttpExchange exchange) throws IOException, SQLException
    {
        Map<String, String> parameters = parseQuery(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        Integer productId = parsePositiveInt(parameters.get("id"), -1);
        String name = parameters.getOrDefault("name", "").trim();
        String category = parameters.getOrDefault("category", "").trim();
        String priceText = parameters.getOrDefault("price", "").trim();
        String displayOrderText = parameters.getOrDefault("displayOrder", "").trim();
        String barcode = parameters.getOrDefault("barcode", "").trim();
        boolean active = !"false".equalsIgnoreCase(parameters.getOrDefault("active", "true"));
        int stockQuantity = parseNonNegativeInt(parameters.getOrDefault("stockQuantity", "0"), "Stock quantity");

        if (name.isEmpty() || category.isEmpty() || priceText.isEmpty())
        {
            throw new IllegalArgumentException("Name, category, and price are required.");
        }

        BigDecimal price = parsePositiveMoney(priceText, "Price");
        int displayOrder = parseNonNegativeInt(displayOrderText.isEmpty() ? "0" : displayOrderText, "Display order");
        Product product = new Product(
            productId > 0 ? productId : null,
            name,
            price,
            category,
            displayOrder,
            active,
            stockQuantity,
            barcode.isEmpty() ? null : barcode
        );

        Product savedProduct;
        if (productId > 0)
        {
            productRepository.update(product);
            savedProduct = productRepository.findById(productId);
        }
        else
        {
            savedProduct = productRepository.save(product);
        }

        return Map.of(
            "message", "Product saved.",
            "product", productsToList(List.of(savedProduct)).get(0)
        );
    }

    private Map<String, Object> adjustInventory(HttpExchange exchange) throws IOException, SQLException
    {
        Map<String, String> parameters = parseQuery(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        int productId = parsePositiveInt(parameters.get("productId"), -1);
        int quantityDelta = Integer.parseInt(parameters.getOrDefault("quantityDelta", "0"));
        String movementType = parameters.getOrDefault("movementType", "ADJUSTMENT").trim();
        String note = parameters.getOrDefault("note", "").trim();

        if (productId <= 0 || quantityDelta == 0)
        {
            throw new IllegalArgumentException("Product and non-zero stock change are required.");
        }

        inventoryRepository.adjustStock(productId, quantityDelta, movementType, note);
        Product updatedProduct = productRepository.findById(productId);
        return Map.of(
            "message", "Inventory updated.",
            "product", productsToList(List.of(updatedProduct)).get(0)
        );
    }

    private List<CartItem> buildCartItems(String itemsValue) throws SQLException
    {
        List<CartItem> cartItems = new ArrayList<>();
        String[] entries = itemsValue.split(",");

        for (String entry : entries)
        {
            if (entry.isBlank())
            {
                continue;
            }

            String[] parts = entry.split(":");
            if (parts.length != 2)
            {
                throw new IllegalArgumentException("Cart items must be productId:quantity pairs.");
            }

            int productId = Integer.parseInt(parts[0].trim());
            int quantity = Integer.parseInt(parts[1].trim());
            if (productId <= 0 || quantity <= 0)
            {
                throw new IllegalArgumentException("Cart item product IDs and quantities must be positive.");
            }

            Product product = productRepository.findById(productId);
            if (product == null || !product.isActive())
            {
                throw new IllegalArgumentException("Product is unavailable.");
            }

            CartItem item = new CartItem(product);
            for (int count = 1; count < quantity; count++)
            {
                item.incrementQuantity();
            }
            cartItems.add(item);
        }

        return cartItems;
    }

    private CartTotals calculateTotals(List<CartItem> cartItems)
    {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems)
        {
            subtotal = subtotal.add(item.getLineTotal());
        }

        subtotal = MoneyUtils.scale(subtotal);
        BigDecimal tax = MoneyUtils.scale(subtotal.multiply(TAX_RATE_PERCENT).divide(BigDecimal.valueOf(100)));
        BigDecimal total = MoneyUtils.scale(subtotal.add(tax));
        return new CartTotals(subtotal, tax, total);
    }

    private List<Map<String, Object>> productsToList(List<Product> products)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Product product : products)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", product.getId());
            row.put("name", product.getName());
            row.put("price", decimal(product.getUnitPrice()));
            row.put("category", product.getCategory());
            row.put("displayOrder", product.getDisplayOrder());
            row.put("active", product.isActive());
            row.put("stockQuantity", product.getStockQuantity());
            row.put("barcode", product.getBarcode());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> salesToList(List<SaleRecord> sales)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SaleRecord sale : sales)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", sale.getId());
            row.put("createdAt", String.valueOf(sale.getCreatedAt()));
            row.put("paymentMethod", sale.getPaymentMethod());
            row.put("subtotal", decimal(sale.getSubtotal()));
            row.put("tax", decimal(sale.getTax()));
            row.put("total", decimal(sale.getTotal()));
            row.put("cashAmount", decimal(sale.getCashAmount()));
            row.put("changeAmount", decimal(sale.getChangeAmount()));
            row.put("status", sale.getStatus());
            row.put("relatedSaleId", sale.getRelatedSaleId());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> paymentSummariesToList(List<PaymentSummary> summaries)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PaymentSummary summary : summaries)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("paymentMethod", summary.getPaymentMethod());
            row.put("transactionCount", summary.getTransactionCount());
            row.put("totalAmount", decimal(summary.getTotalAmount()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> topProductsToList(List<TopProductSummary> summaries)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TopProductSummary summary : summaries)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productName", summary.getProductName());
            row.put("quantitySold", summary.getQuantitySold());
            row.put("revenue", decimal(summary.getRevenue()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> stockMovementsToList(List<StockMovementRecord> movements)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StockMovementRecord movement : movements)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("createdAt", String.valueOf(movement.getCreatedAt()));
            row.put("productName", movement.getProductName());
            row.put("movementType", movement.getMovementType());
            row.put("quantityDelta", movement.getQuantityDelta());
            row.put("stockBefore", movement.getStockBefore());
            row.put("stockAfter", movement.getStockAfter());
            row.put("note", movement.getNote());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> summaryToMap(SalesSummary summary)
    {
        return Map.of(
            "transactionCount", summary.getTransactionCount(),
            "subtotal", decimal(summary.getSubtotal()),
            "tax", decimal(summary.getTax()),
            "total", decimal(summary.getTotal())
        );
    }

    private String decimal(BigDecimal value)
    {
        return value == null ? null : value.toPlainString();
    }

    private Map<String, String> parseQuery(String rawQuery) throws IOException
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty())
        {
            return parameters;
        }

        String[] parts = rawQuery.split("&");
        for (String part : parts)
        {
            if (part.isEmpty())
            {
                continue;
            }

            int separatorIndex = part.indexOf('=');
            String key = separatorIndex >= 0 ? part.substring(0, separatorIndex) : part;
            String value = separatorIndex >= 0 ? part.substring(separatorIndex + 1) : "";
            parameters.put(
                URLDecoder.decode(key, StandardCharsets.UTF_8.name()),
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            );
        }

        return parameters;
    }

    private int parsePositiveInt(String value, int defaultValue)
    {
        if (value == null || value.trim().isEmpty())
        {
            return defaultValue;
        }

        try
        {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        }
        catch (NumberFormatException ex)
        {
            return defaultValue;
        }
    }

    private int parseNonNegativeInt(String value, String fieldName)
    {
        if (value == null || value.trim().isEmpty())
        {
            return 0;
        }

        try
        {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0)
            {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return parsed;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(fieldName + " must be a valid whole number.");
        }
    }

    private BigDecimal parsePositiveMoney(String value, String fieldName)
    {
        BigDecimal amount = parseNonNegativeMoney(value, fieldName);
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return amount;
    }

    private BigDecimal parseNonNegativeMoney(String value, String fieldName)
    {
        if (value == null || value.trim().isEmpty())
        {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try
        {
            BigDecimal amount = MoneyUtils.scale(new BigDecimal(value.trim()));
            if (amount.compareTo(BigDecimal.ZERO) < 0)
            {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return amount;
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException(fieldName + " must be a valid amount.");
        }
    }

    private void writeJson(HttpExchange exchange, int statusCode, Map<String, ?> data) throws IOException
    {
        byte[] body = JsonUtils.toJson(data).getBytes(StandardCharsets.UTF_8);
        sendBytes(exchange, statusCode, "application/json; charset=utf-8", body);
    }

    private void sendBytes(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody())
        {
            outputStream.write(body);
        }
    }

    private void sendBackup(HttpExchange exchange) throws IOException
    {
        String fileName = "javapos-export-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        byte[] body = JsonUtils.toJson(buildBackupExport()).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody())
        {
            outputStream.write(body);
        }
    }

    private Map<String, Object> buildBackupExport() throws IOException
    {
        try
        {
            Map<String, Object> export = new LinkedHashMap<>();
            export.put("exportedAt", String.valueOf(LocalDateTime.now()));
            export.put("runtime", buildRuntimeResponse());
            export.put("products", productsToList(productRepository.findAll()));
            export.put("sales", salesToList(saleRepository.findSales(null, null, 100000)));
            export.put("saleItems", queryRows(
                "SELECT sale_id, product_name, unit_price, quantity, line_total FROM sale_items ORDER BY id ASC"
            ));
            export.put("stockMovements", stockMovementsToList(inventoryRepository.findRecentMovements(100000)));
            export.put("users", queryRows(
                "SELECT username, role, active, must_change_password FROM users ORDER BY username ASC"
            ));
            return export;
        }
        catch (SQLException ex)
        {
            throw new IOException("Unable to build export snapshot. " + ex.getMessage(), ex);
        }
    }

    private List<Map<String, Object>> queryRows(String sql) throws SQLException
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (
            Connection connection = databaseManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        )
        {
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next())
            {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 1; index <= columnCount; index++)
                {
                    Object value = resultSet.getObject(index);
                    row.put(resultSet.getMetaData().getColumnLabel(index), value == null ? null : String.valueOf(value));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private String contentType(Path file)
    {
        String fileName = file.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".css"))
        {
            return "text/css; charset=utf-8";
        }
        if (fileName.endsWith(".js"))
        {
            return "application/javascript; charset=utf-8";
        }
        if (fileName.endsWith(".html"))
        {
            return "text/html; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private Map<String, Object> login(HttpExchange exchange) throws IOException, SQLException
    {
        Map<String, String> parameters = parseQuery(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String username = parameters.getOrDefault("username", "").trim();
        String password = parameters.getOrDefault("password", "");
        String newPassword = parameters.getOrDefault("newPassword", "");

        UserAccount user = userRepository.authenticate(username, password);
        if (user == null || !user.isActive())
        {
            throw new SecurityException("UNAUTHORIZED");
        }

        if (user.mustChangePassword())
        {
            if (newPassword.trim().isEmpty())
            {
                return Map.of("mustChangePassword", true);
            }
            userRepository.forcePasswordChange(user.getId(), newPassword);
            user = userRepository.authenticate(username, newPassword);
        }

        String token = webSessionManager.createSession(user);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("user", userToMap(user));
        response.put("mustChangePassword", false);
        return response;
    }

    private void logout(HttpExchange exchange)
    {
        webSessionManager.removeSession(readToken(exchange));
    }

    private Map<String, Object> sessionResponse(UserAccount user)
    {
        return Map.of("user", userToMap(user));
    }

    private Map<String, Object> userToMap(UserAccount user)
    {
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "role", user.getRole(),
            "active", user.isActive(),
            "admin", user.isAdmin()
        );
    }

    private UserAccount authenticateRequest(HttpExchange exchange)
    {
        return webSessionManager.getUser(readToken(exchange));
    }

    private String readToken(HttpExchange exchange)
    {
        return exchange.getRequestHeaders().getFirst("X-Session-Token");
    }

    private void requireAuthenticated(UserAccount user)
    {
        if (user == null)
        {
            throw new SecurityException("UNAUTHORIZED");
        }
    }

    private void requireAdmin(UserAccount user)
    {
        requireAuthenticated(user);
        if (!user.isAdmin())
        {
            throw new SecurityException("FORBIDDEN");
        }
    }
}
