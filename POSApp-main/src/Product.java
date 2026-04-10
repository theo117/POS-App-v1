import java.math.BigDecimal;

public class Product
{
    private final Integer id;
    private final String name;
    private final BigDecimal unitPrice;
    private final String category;
    private final int displayOrder;
    private final boolean active;
    private final int stockQuantity;
    private final String barcode;

    public Product(String name, BigDecimal unitPrice)
    {
        this(null, name, unitPrice, "MAIN", 0, true, 0, null);
    }

    public Product(Integer id, String name, BigDecimal unitPrice, String category, int displayOrder, boolean active, int stockQuantity, String barcode)
    {
        this.id = id;
        this.name = name;
        this.unitPrice = unitPrice;
        this.category = category;
        this.displayOrder = displayOrder;
        this.active = active;
        this.stockQuantity = stockQuantity;
        this.barcode = barcode;
    }

    public Integer getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public BigDecimal getUnitPrice()
    {
        return unitPrice;
    }

    public String getCategory()
    {
        return category;
    }

    public int getDisplayOrder()
    {
        return displayOrder;
    }

    public boolean isActive()
    {
        return active;
    }

    public int getStockQuantity()
    {
        return stockQuantity;
    }

    public String getBarcode()
    {
        return barcode;
    }
}
