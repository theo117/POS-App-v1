import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CartService
{
    private final BigDecimal taxRatePercent;
    private final List<CartItem> items = new ArrayList<>();

    public CartService(BigDecimal taxRatePercent)
    {
        this.taxRatePercent = taxRatePercent;
    }

    public void addProduct(Product product)
    {
        for (CartItem item : items)
        {
            if (item.getProduct().getName().equals(product.getName()))
            {
                item.incrementQuantity();
                return;
            }
        }

        items.add(new CartItem(product));
    }

    public void removeItem(int index)
    {
        items.remove(index);
    }

    public void clear()
    {
        items.clear();
    }

    public boolean isEmpty()
    {
        return items.isEmpty();
    }

    public List<CartItem> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    public int getQuantityForProduct(String productName)
    {
        for (CartItem item : items)
        {
            if (item.getProduct().getName().equals(productName))
            {
                return item.getQuantity();
            }
        }

        return 0;
    }

    public CartTotals calculateTotals()
    {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items)
        {
            subtotal = subtotal.add(item.getLineTotal());
        }

        subtotal = MoneyUtils.scale(subtotal);
        BigDecimal tax = MoneyUtils.scale(subtotal.multiply(taxRatePercent).divide(BigDecimal.valueOf(100)));
        BigDecimal total = MoneyUtils.scale(subtotal.add(tax));
        return new CartTotals(subtotal, tax, total);
    }
}
