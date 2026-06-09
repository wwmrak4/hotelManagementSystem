package uk.co.hms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.co.hms.model.RoomPricingRequest;
import uk.co.hms.model.RoomPricingResponse;

@FeignClient(name = "hotelPricingApiClient", url = "${hotel.pricing.api.endpoint}")
public interface HotelPricingClient {

    @GetMapping(value = "${hotel.pricing.api.version}/room-prices" )
    ResponseEntity<RoomPricingResponse> fetchRoomPrices(@RequestBody RoomPricingRequest roomPricingRequest);
}