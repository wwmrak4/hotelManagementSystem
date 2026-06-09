package uk.co.hms.model;

import uk.co.hms.model.enums.RoomType;
import java.math.BigDecimal;
import java.util.Map;

public record RoomPricingResponse(
    Map<RoomType, BigDecimal> roomTypesPrices)
{
}
