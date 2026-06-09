package uk.co.hms.model;

import lombok.Builder;
import lombok.Getter;
import uk.co.hms.model.enums.RoomType;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AvailabilityResponse {
    private Long customerId;
    private Long hotelId;
    private List<AvailableRooms> availableRooms;

    @Getter
    @Builder
    public static class AvailableRooms {
        private RoomType roomType;
        private BigDecimal price;
        private Integer count;
    }
}

