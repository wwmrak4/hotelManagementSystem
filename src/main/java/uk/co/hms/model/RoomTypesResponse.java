package uk.co.hms.model;

import uk.co.hms.model.enums.RoomType;
import java.util.Map;

public record RoomTypesResponse(
        Long hotelId,
        Map<RoomType, Integer> roomTypesAndCount
) {
}