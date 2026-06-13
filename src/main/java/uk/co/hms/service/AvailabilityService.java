package uk.co.hms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.co.hms.client.HotelInventoryClient;
import uk.co.hms.client.HotelPricingClient;
import uk.co.hms.client.CustomerProfileClient;
import uk.co.hms.exception.AvailabilityServiceException;
import uk.co.hms.exception.CustomerProfileClientException;
import uk.co.hms.exception.HotelInventoryClientException;
import uk.co.hms.exception.HotelPricingClientException;
import uk.co.hms.model.AvailabilityRequest;
import uk.co.hms.model.AvailabilityResponse;
import uk.co.hms.model.CustomerProfileResponse;
import uk.co.hms.model.RoomPricingRequest;
import uk.co.hms.model.RoomPricingResponse;
import uk.co.hms.model.RoomTypesResponse;
import uk.co.hms.model.enums.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {
    private final HotelInventoryClient hotelInventoryClient;
    private final HotelPricingClient hotelPricingClient;
    private final CustomerProfileClient customerProfileClient;

    public AvailabilityResponse getAvailability(
            AvailabilityRequest availabilityRequest) throws AvailabilityServiceException {
        Long hotelId = availabilityRequest.getHotelId();
        Long customerId = availabilityRequest.getCustomerId();
        LocalDate checkInDate = availabilityRequest.getCheckInDate();
        LocalDate checkOutDate = availabilityRequest.getCheckOutDate();

        log.debug("Fetching availability for hotelId: {}, customerId: {}, checkInDate: {}, checkOutDate: {}",
                hotelId, customerId, checkInDate, checkOutDate);

        ResponseEntity<RoomTypesResponse> roomTypesResponseEntity =
                hotelInventoryClient.fetchRoomTypes(hotelId);
        RoomTypesResponse roomTypesResponse = Optional.ofNullable(roomTypesResponseEntity)
                .map(ResponseEntity::getBody)
                .orElseThrow(() -> new HotelInventoryClientException(
                        "Error fetching room types via hotel inventory api for hotelId: " + hotelId));
        log.debug("Successfully fetched room types via hotel inventory api for hotelId: {}", hotelId);
        validateHotelInventoryResponse(roomTypesResponse, hotelId);

        ResponseEntity<RoomPricingResponse> roomPricingResponseEntity =
                hotelPricingClient.fetchRoomPrices(RoomPricingRequest.builder()
                        .hotelId(hotelId)
                        .checkInDate(checkInDate)
                        .checkOutDate(checkOutDate)
                        .roomTypes(roomTypesResponseEntity.getBody().roomTypesAndCount().keySet()
                                .stream()
                                .toList())
                        .build());
        RoomPricingResponse roomPricingResponse = Optional.ofNullable(roomPricingResponseEntity)
                .map(ResponseEntity::getBody)
                .orElseThrow(() -> new HotelPricingClientException(
                        "Error fetching room pricing information via hotel pricing api for hotelId: " + hotelId));
        log.debug("Successfully fetched room pricing information via hotel pricing api for hotelId: {}", hotelId);
        validateHotelPricingResponse(roomPricingResponse, hotelId);

        ResponseEntity<CustomerProfileResponse> customerProfileResponseEntity =
                customerProfileClient.fetchCustomerProfile(customerId);
        CustomerProfileResponse customerProfileResponse = Optional.ofNullable(customerProfileResponseEntity)
                .map(ResponseEntity::getBody)
                .orElseThrow(() -> new CustomerProfileClientException(
                        "Error fetching customer profile information via customer profile api for customerId: " + customerId));
        log.debug("Successfully fetched customer profile information via customer profile api for customerId: {}", customerId);

        BigDecimal roomDiscount = getCustomerRoomDiscount(customerProfileResponseEntity.getBody());

        return buildAvailabilityResponse(roomTypesResponseEntity.getBody(), roomPricingResponseEntity.getBody(),
                availabilityRequest, roomDiscount);
    }

    private AvailabilityResponse buildAvailabilityResponse(RoomTypesResponse roomTypesResponse,
             RoomPricingResponse roomPricingResponse, AvailabilityRequest availabilityRequest, BigDecimal roomDiscountMultiplier) {

        List<AvailabilityResponse.AvailableRooms> availableRooms = roomTypesResponse.roomTypesAndCount()
                .entrySet()
                .stream()
                .filter(entry ->
                        roomPricingResponse.roomTypesPrices()
                                .get(entry.getKey()) != null
                )
                .map(entry -> {
                    RoomType roomType = entry.getKey();
                    Integer roomCount = entry.getValue();

                    BigDecimal baseRoomPrice = roomPricingResponse.roomTypesPrices()
                            .get(roomType);
                    BigDecimal finalRoomPrice = baseRoomPrice.multiply(roomDiscountMultiplier);

                    return AvailabilityResponse.AvailableRooms.builder()
                            .roomType(roomType)
                            .count(roomCount)
                            .price(finalRoomPrice)
                            .build();
                })
                .toList();

        return AvailabilityResponse.builder()
                .customerId(availabilityRequest.getCustomerId())
                .hotelId(availabilityRequest.getHotelId())
                .availableRooms(availableRooms)
                .build();
    }

    private void validateHotelPricingResponse(RoomPricingResponse roomPricingResponse, Long hotelId) throws AvailabilityServiceException {
        if (roomPricingResponse.roomTypesPrices() == null || roomPricingResponse.roomTypesPrices().isEmpty()) {
            throw new AvailabilityServiceException("No pricing data available for hotelId=" + hotelId);
        }
    }

    private void validateHotelInventoryResponse(RoomTypesResponse roomTypesResponse, Long hotelId) throws AvailabilityServiceException {
        if (roomTypesResponse.roomTypesAndCount() == null || roomTypesResponse.roomTypesAndCount().isEmpty()) {
            throw new AvailabilityServiceException("No room types in hotel inventory api response for " +
                    "hotelId=" + hotelId);
        }
    }

    private BigDecimal getCustomerRoomDiscount(CustomerProfileResponse customerProfileResponse) {
        return customerProfileResponse.loyaltyTier() == null
                ? BigDecimal.ONE
                : customerProfileResponse.loyaltyTier().getDiscount();
        }
    }