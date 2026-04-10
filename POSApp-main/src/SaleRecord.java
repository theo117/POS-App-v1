import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleRecord
{
    private final int id;
    private final LocalDateTime createdAt;
    private final String paymentMethod;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;
    private final BigDecimal cashAmount;
    private final BigDecimal changeAmount;
    private final String status;
    private final Integer relatedSaleId;

    public SaleRecord(int id, LocalDateTime createdAt, String paymentMethod, BigDecimal subtotal, BigDecimal tax, BigDecimal total, BigDecimal cashAmount, BigDecimal changeAmount, String status, Integer relatedSaleId)
    {
        this.id = id;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
        this.cashAmount = cashAmount;
        this.changeAmount = changeAmount;
        this.status = status;
        this.relatedSaleId = relatedSaleId;
    }

    public int getId()
    {
        return id;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
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

    public BigDecimal getCashAmount()
    {
        return cashAmount;
    }

    public BigDecimal getChangeAmount()
    {
        return changeAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public Integer getRelatedSaleId()
    {
        return relatedSaleId;
    }
}
