import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils
{
    private MoneyUtils()
    {
    }

    public static BigDecimal scale(BigDecimal amount)
    {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal amount)
    {
        return "R " + scale(amount).toPlainString();
    }
}
