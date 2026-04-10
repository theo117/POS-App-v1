import java.time.LocalDateTime;

public class StockMovementRecord
{
    private final LocalDateTime createdAt;
    private final String productName;
    private final String movementType;
    private final int quantityDelta;
    private final int stockBefore;
    private final int stockAfter;
    private final String note;

    public StockMovementRecord(LocalDateTime createdAt, String productName, String movementType, int quantityDelta, int stockBefore, int stockAfter, String note)
    {
        this.createdAt = createdAt;
        this.productName = productName;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.note = note;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public String getProductName()
    {
        return productName;
    }

    public String getMovementType()
    {
        return movementType;
    }

    public int getQuantityDelta()
    {
        return quantityDelta;
    }

    public int getStockBefore()
    {
        return stockBefore;
    }

    public int getStockAfter()
    {
        return stockAfter;
    }

    public String getNote()
    {
        return note;
    }
}
