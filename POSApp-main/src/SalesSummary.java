import java.math.BigDecimal;

public class SalesSummary
{
    private final int transactionCount;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;

    public SalesSummary(int transactionCount, BigDecimal subtotal, BigDecimal tax, BigDecimal total)
    {
        this.transactionCount = transactionCount;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
    }

    public int getTransactionCount()
    {
        return transactionCount;
    }

    public BigDecimal getSubtotal()
    {
        return subtotal;
    }

    public BigDecimal getTax()
    {
        return tax;
    }

    public BigDecimal getTotal()
    {
        return total;
    }
}
