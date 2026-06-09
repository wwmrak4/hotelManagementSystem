package uk.co.hms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.co.hms.client.CustomerProfileClient;
import uk.co.hms.client.HotelInventoryClient;
import uk.co.hms.client.HotelPricingClient;
import uk.co.hms.exception.AvailabilityServiceException;
import uk.co.hms.exception.HotelInventoryClientException;
import uk.co.hms.model.AvailabilityRequest;
import uk.co.hms.model.AvailabilityResponse;
import uk.co.hms.model.CustomerProfileResponse;
import uk.co.hms.model.RoomPricingRequest;
import uk.co.hms.model.RoomPricingResponse;
import uk.co.hms.model.RoomTypesResponse;
import uk.co.hms.model.enums.LoyaltyTier;
import uk.co.hms.model.enums.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {
    @Mock
    private HotelInventoryClient hotelInventoryClient;

    @Mock
    private HotelPricingClient hotelPricingClient;

    @Mock
    private CustomerProfileClient customerProfileClient;

    @InjectMocks
    private AvailabilityService availabilityService;

    private final Long hotelId = 1L;
    private final Long customerId = 10L;
    private CustomerProfileResponse customerProfileResponse;
    private RoomTypesResponse roomTypesResponse;
    private RoomPricingResponse pricingResponse;

    @BeforeEach
    void setup() {
        roomTypesResponse = new RoomTypesResponse(hotelId,
                Map.of(RoomType.SINGLE, 2, RoomType.DOUBLE, 1));
        pricingResponse = new RoomPricingResponse(Map.of(RoomType.SINGLE, new BigDecimal("100"),
                RoomType.DOUBLE, new BigDecimal("150")));
        customerProfileResponse = new CustomerProfileResponse(
                10L, LoyaltyTier.GOLD, 100);
    }

    @Test
    void shouldReturnAvailabilitySuccessfully_whenCorrectDataProvided() throws AvailabilityServiceException {
        AvailabilityRequest request = validAvailabilityRequest(hotelId, customerId);

        setupCommonMocks(customerProfileResponse);

        AvailabilityResponse response =
                availabilityService.getAvailability(request);
        assertNotNull(response);
        assertEquals(hotelId, response.getHotelId());
        assertEquals(customerId, response.getCustomerId());
        assertNotNull(response.getAvailableRooms());
        assertEquals(2, response.getAvailableRooms().size());
        AvailabilityResponse.AvailableRooms singleRoom =
                response.getAvailableRooms().stream()
                        .filter(r -> r.getRoomType() == RoomType.SINGLE)
                        .findFirst()
                        .orElseThrow();
        assertEquals(RoomType.SINGLE, singleRoom.getRoomType());
        assertEquals(new BigDecimal("70.00"), singleRoom.getPrice());
        assertEquals(Integer.valueOf(2), singleRoom.getCount());
        verify(hotelInventoryClient, times(1)).fetchRoomTypes(hotelId);
        verify(hotelPricingClient, times(1)).fetchRoomPrices(any());
        verify(customerProfileClient, times(1)).fetchCustomerProfile(customerId);
    }

    @Test
    void shouldSendCorrectDataToClientsInvoked_whenCorrectDataProvided() throws Exception,
            AvailabilityServiceException {
        AvailabilityRequest request = validAvailabilityRequest(hotelId, customerId);

        setupCommonMocks(customerProfileResponse);

        availabilityService.getAvailability(request);
        ArgumentCaptor<RoomPricingRequest> captor =
                ArgumentCaptor.forClass(RoomPricingRequest.class);
        verify(hotelPricingClient, times(1))
                .fetchRoomPrices(captor.capture());
        RoomPricingRequest capturedRequest = captor.getValue();
        assertEquals(hotelId, capturedRequest.getHotelId());
        assertEquals(LocalDate.of(2026, 1, 10), capturedRequest.getCheckInDate());
        assertEquals(LocalDate.of(2026, 1, 12), capturedRequest.getCheckOutDate());
        assertNotNull(capturedRequest.getRoomTypes());
        assertEquals(2, capturedRequest.getRoomTypes().size());
        assertTrue(capturedRequest.getRoomTypes().contains(RoomType.SINGLE));
        assertTrue(capturedRequest.getRoomTypes().contains(RoomType.DOUBLE));

        verify(hotelInventoryClient, times(1)).fetchRoomTypes(hotelId);
        verify(customerProfileClient, times(1)).fetchCustomerProfile(customerId);
    }

    @Test
    void shouldThrowAvailabilityServiceException_whenRoomTypesEmpty() {
        AvailabilityRequest request = validAvailabilityRequest(1L, 10L);

        when(hotelInventoryClient.fetchRoomTypes(1L))
                .thenReturn(ResponseEntity.ok(new RoomTypesResponse(1L, Map.of())));

        assertThrows(AvailabilityServiceException.class,
                () -> availabilityService.getAvailability(request));
        verify(hotelInventoryClient, times(1)).fetchRoomTypes(any());
        verify(hotelPricingClient, never()).fetchRoomPrices(any());
        verify(customerProfileClient, never()).fetchCustomerProfile(any());
    }

    @Test
    void shouldThrowAvailabilityServiceException_whenPricingEmpty() {
        AvailabilityRequest request = validAvailabilityRequest(1L, 10L);

        when(hotelInventoryClient.fetchRoomTypes(1L))
                .thenReturn(ResponseEntity.ok(roomTypesResponse));
        when(hotelPricingClient.fetchRoomPrices(any()))
                .thenReturn(ResponseEntity.ok(new RoomPricingResponse(Map.of())));

        assertThrows(AvailabilityServiceException.class,
                () -> availabilityService.getAvailability(request));
        verify(hotelInventoryClient, times(1)).fetchRoomTypes(any());
        verify(hotelPricingClient, times(1)).fetchRoomPrices(any());
        verify(customerProfileClient, never()).fetchCustomerProfile(any());
    }

    @Test
    void shouldThrowHotelInventoryClientException_whenRoomTypesResponseIsNull() {
        when(hotelInventoryClient.fetchRoomTypes(1L))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(HotelInventoryClientException.class,
                () -> availabilityService.getAvailability(
                        validAvailabilityRequest(1L, 10L)
                )
        );
        verify(hotelInventoryClient, times(1)).fetchRoomTypes(any());
        verify(hotelPricingClient, never()).fetchRoomPrices(any());
        verify(customerProfileClient, never()).fetchCustomerProfile(any());
    }

    @Test
    void shouldNotReturnRoomTypeInResponse_whenPricingMissingForThatRoomType() throws Exception,
            AvailabilityServiceException {
        when(hotelInventoryClient.fetchRoomTypes(hotelId))
                .thenReturn(ResponseEntity.ok(
                        new RoomTypesResponse(hotelId,
                                Map.of(
                                        RoomType.SINGLE, 2,
                                        RoomType.DOUBLE, 1
                                ))
                ));
        when(hotelPricingClient.fetchRoomPrices(any()))
                .thenReturn(ResponseEntity.ok(
                        new RoomPricingResponse(
                                Map.of(RoomType.SINGLE, new BigDecimal("100"))
                        )
                ));
        when(customerProfileClient.fetchCustomerProfile(customerId))
                .thenReturn(ResponseEntity.ok(customerProfileResponse));

        AvailabilityResponse response =
                availabilityService.getAvailability(
                        validAvailabilityRequest(hotelId, customerId));

        assertEquals(1, response.getAvailableRooms().size());
        assertEquals(RoomType.SINGLE,
                response.getAvailableRooms().get(0).getRoomType());
        verify(hotelInventoryClient, times(1)).fetchRoomTypes(hotelId);
        verify(hotelPricingClient, times(1)).fetchRoomPrices(any());
        verify(customerProfileClient, times(1)).fetchCustomerProfile(customerId);
    }

    @Test
    void shouldApplyNoDiscount_whenCustomerLoyaltyTierIsNull() throws AvailabilityServiceException {
        AvailabilityRequest request = validAvailabilityRequest(hotelId, customerId);
        CustomerProfileResponse profile =
                new CustomerProfileResponse(customerId, null, 100);

        setupCommonMocks(profile);

        AvailabilityResponse response =
                availabilityService.getAvailability(request);
        AvailabilityResponse.AvailableRooms room =
                response.getAvailableRooms().stream()
                        .filter(r -> r.getRoomType() == RoomType.SINGLE)
                        .findFirst()
                        .orElseThrow();
        assertEquals(new BigDecimal(100), room.getPrice());
    }

    private AvailabilityRequest validAvailabilityRequest(Long hotelId, Long customerId) {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .hotelId(hotelId)
                .customerId(customerId)
                .checkInDate(LocalDate.of(2026, 1, 10))
                .checkOutDate(LocalDate.of(2026, 1, 12))
                .build();

        return request;
    }

    private void setupCommonMocks(CustomerProfileResponse customerProfileResponse) {
        when(hotelInventoryClient.fetchRoomTypes(hotelId))
                .thenReturn(ResponseEntity.ok(roomTypesResponse));
        when(hotelPricingClient.fetchRoomPrices(any()))
                .thenReturn(ResponseEntity.ok(pricingResponse));
        when(customerProfileClient.fetchCustomerProfile(customerId))
                .thenReturn(ResponseEntity.ok(customerProfileResponse));
    }
}