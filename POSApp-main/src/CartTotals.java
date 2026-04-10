import java.math.BigDecimal;

public class CartTotals
{
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;

    public CartTotals(BigDecimal subtotal, BigDecimal tax, BigDecimal total)
    {
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
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
