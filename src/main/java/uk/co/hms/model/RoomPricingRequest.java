package uk.co.hms.model;

import lombok.Builder;
import lombok.Getter;
import uk.co.hms.model.enums.RoomType;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class RoomPricingRequest {
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private List<RoomType> roomTypes;
}
