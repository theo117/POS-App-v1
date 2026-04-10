import java.math.BigDecimal;

public class SaleItemRecord
{
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineTotal;

    public SaleItemRecord(String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal)
    {
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public String getProductName()
    {
        return productName;
    }

    public BigDecimal getUnitPrice()
    {
        return unitPrice;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public BigDecimal getLineTotal()
    {
        return lineTotal;
    }
}
