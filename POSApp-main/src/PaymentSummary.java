import java.math.BigDecimal;

public class PaymentSummary
{
    private final String paymentMethod;
    private final int transactionCount;
    private final BigDecimal totalAmount;

    public PaymentSummary(String paymentMethod, int transactionCount, BigDecimal totalAmount)
    {
        this.paymentMethod = paymentMethod;
        this.transactionCount = transactionCount;
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod()
    {
        return paymentMethod;
    }

    public int getTransactionCount()
    {
        return transactionCount;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }
}
