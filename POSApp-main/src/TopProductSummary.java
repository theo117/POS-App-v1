import java.math.BigDecimal;

public class TopProductSummary
{
    private final String productName;
    private final int quantitySold;
    private final BigDecimal revenue;

    public TopProductSummary(String productName, int quantitySold, BigDecimal revenue)
    {
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getProductName()
    {
        return productName;
    }

    public int getQuantitySold()
    {
        return quantitySold;
    }

    public BigDecimal getRevenue()
    {
        return revenue;
    }
}
