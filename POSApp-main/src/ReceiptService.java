import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptService
{
    private final DateTimeFormatter receiptTimeFormat;

    public ReceiptService(DateTimeFormatter receiptTimeFormat)
    {
        this.receiptTimeFormat = receiptTimeFormat;
    }

    public String buildReceipt(List<CartItem> items, CartTotals totals, String paymentMethod, BigDecimal cashAmount, BigDecimal changeAmount)
    {
        StringBuilder receipt = new StringBuilder();

        receipt.append("POS RECEIPT\n");
        receipt.append("Date: ").append(LocalDateTime.now().format(receiptTimeFormat)).append('\n');
        receipt.append("Payment: ").append(paymentMethod).append("\n\n");
        receipt.append("Items:\n");

        for (CartItem item : items)
        {
            receipt.append("- ")
                .append(item.getProduct().getName())
                .append(" x")
                .append(item.getQuantity())
                .append("  ")
                .append(MoneyUtils.format(item.getLineTotal()))
                .append('\n');
        }

        receipt.append('\n');
        receipt.append("Subtotal: ").append(MoneyUtils.format(totals.getSubtotal())).append('\n');
        receipt.append("Tax: ").append(MoneyUtils.format(totals.getTax())).append('\n');
        receipt.append("Total: ").append(MoneyUtils.format(totals.getTotal())).append('\n');

        if ("Cash".equals(paymentMethod))
        {
            receipt.append("Cash: ").append(MoneyUtils.format(cashAmount)).append('\n');
            receipt.append("Change: ").append(MoneyUtils.format(changeAmount)).append('\n');
        }

        return receipt.toString();
    }

    public String buildReceiptFromSale(SaleRecord sale, List<SaleItemRecord> items)
    {
        StringBuilder receipt = new StringBuilder();
        receipt.append("POS RECEIPT\n");
        receipt.append("Date: ").append(sale.getCreatedAt().format(receiptTimeFormat)).append('\n');
        receipt.append("Payment: ").append(sale.getPaymentMethod()).append('\n');
        receipt.append("Status: ").append(sale.getStatus()).append("\n\n");
        receipt.append("Items:\n");

        for (SaleItemRecord item : items)
        {
            receipt.append("- ")
                .append(item.getProductName())
                .append(" x")
                .append(item.getQuantity())
                .append("  ")
                .append(MoneyUtils.format(item.getLineTotal()))
                .append('\n');
        }

        receipt.append('\n');
        receipt.append("Subtotal: ").append(MoneyUtils.format(sale.getSubtotal())).append('\n');
        receipt.append("Tax: ").append(MoneyUtils.format(sale.getTax())).append('\n');
        receipt.append("Total: ").append(MoneyUtils.format(sale.getTotal())).append('\n');
        if (sale.getCashAmount() != null)
        {
            receipt.append("Cash: ").append(MoneyUtils.format(sale.getCashAmount())).append('\n');
        }
        if (sale.getChangeAmount() != null)
        {
            receipt.append("Change: ").append(MoneyUtils.format(sale.getChangeAmount())).append('\n');
        }
        if (sale.getRelatedSaleId() != null)
        {
            receipt.append("Related Sale: ").append(sale.getRelatedSaleId()).append('\n');
        }

        return receipt.toString();
    }
}
