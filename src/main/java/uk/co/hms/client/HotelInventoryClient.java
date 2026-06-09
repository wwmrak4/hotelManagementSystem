package uk.co.hms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.hms.model.RoomTypesResponse;

@FeignClient(name = "HotelInventoryApiClient", url = "${hotel.inventory.api.endpoint}")
public interface HotelInventoryClient {

    @GetMapping(value = "${hotel.inventory.api.version}/room-types" )
    ResponseEntity<RoomTypesResponse> fetchRoomTypes(@PathVariable Long hotelId);
}
