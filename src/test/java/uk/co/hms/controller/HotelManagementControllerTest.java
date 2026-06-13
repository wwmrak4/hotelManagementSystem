package uk.co.hms.controller;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.co.hms.client.CustomerProfileClient;
import uk.co.hms.client.HotelInventoryClient;
import uk.co.hms.client.HotelPricingClient;
import uk.co.hms.exception.HotelInventoryClientException;
import uk.co.hms.model.CustomerProfileResponse;
import uk.co.hms.model.RoomPricingRequest;
import uk.co.hms.model.RoomPricingResponse;
import uk.co.hms.model.RoomTypesResponse;
import uk.co.hms.model.enums.LoyaltyTier;
import uk.co.hms.model.enums.RoomType;
import uk.co.hms.service.AvailabilityService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HotelManagementController.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {AvailabilityService.class}))
@AutoConfigureMockMvc(addFilters = false)
class HotelManagementControllerTest {

    @MockitoBean
    private HotelInventoryClient hotelInventoryClient;
    @MockitoBean
    private HotelPricingClient hotelPricingClient;
    @MockitoBean
    private CustomerProfileClient customerProfileClient;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AvailabilityService availabilityService;

    private Long hotelId = 5L;
    private Long customerId = 1L;

    @Test
    void shouldReturnValidAvailabilityResponse_whenCorrectDataProvided() throws Exception {
        RoomTypesResponse roomTypesResponse = validRoomTypesResponse();
        RoomPricingResponse roomPricingResponse = validRoomPricingResponse();
        CustomerProfileResponse customerProfileResponse =
                new CustomerProfileResponse(1L, LoyaltyTier.GOLD, 20);

        when(hotelInventoryClient.fetchRoomTypes(hotelId)).thenReturn(ResponseEntity.ok(roomTypesResponse));
        when(hotelPricingClient.fetchRoomPrices(any(RoomPricingRequest.class)))
                .thenReturn(ResponseEntity.ok(roomPricingResponse));
        when(customerProfileClient.fetchCustomerProfile(customerId)).thenReturn(ResponseEntity.ok(customerProfileResponse));

        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1")
                        .param("checkInDate", "2026-08-01")
                        .param("checkOutDate", "2026-08-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value(5))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.availableRooms.length()").value(2))
                .andExpect(jsonPath("$.availableRooms[?(@.roomType=='SINGLE')].count").value(5))
                .andExpect(jsonPath("$.availableRooms[?(@.roomType=='DELUXE')].count").value(2))
                .andExpect(jsonPath("$.availableRooms[?(@.roomType=='SINGLE')].price")
                        .value(70.0))
                .andExpect(jsonPath("$.availableRooms[?(@.roomType=='DELUXE')].price")
                        .value(140.0));
    }

    @Test
    void shouldCallAllClientsWithExpectedArguments() throws Exception {
        RoomTypesResponse roomTypesResponse = validRoomTypesResponse();
        RoomPricingResponse roomPricingResponse = validRoomPricingResponse();
        CustomerProfileResponse customerProfileResponse =
                new CustomerProfileResponse(customerId, LoyaltyTier.GOLD, 20);

        when(hotelInventoryClient.fetchRoomTypes(hotelId))
                .thenReturn(ResponseEntity.ok(roomTypesResponse));
        when(hotelPricingClient.fetchRoomPrices(any(RoomPricingRequest.class)))
                .thenReturn(ResponseEntity.ok(roomPricingResponse));
        when(customerProfileClient.fetchCustomerProfile(customerId))
                .thenReturn(ResponseEntity.ok(customerProfileResponse));

        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1")
                        .param("checkInDate", "2026-08-01")
                        .param("checkOutDate", "2026-08-05"))
                .andExpect(status().isOk());
        ArgumentCaptor<Long> hotelIdCaptor =
                ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<RoomPricingRequest> pricingRequestCaptor =
                ArgumentCaptor.forClass(RoomPricingRequest.class);
        ArgumentCaptor<Long> customerIdCaptor =
                ArgumentCaptor.forClass(Long.class);
        verify(hotelInventoryClient)
                .fetchRoomTypes(hotelIdCaptor.capture());
        verify(hotelPricingClient)
                .fetchRoomPrices(pricingRequestCaptor.capture());
        verify(customerProfileClient)
                .fetchCustomerProfile(customerIdCaptor.capture());
        assertThat(hotelIdCaptor.getValue())
                .isEqualTo(5L);
        assertThat(customerIdCaptor.getValue())
                .isEqualTo(1L);
        RoomPricingRequest pricingRequest =
                pricingRequestCaptor.getValue();
        assertThat(pricingRequest.getHotelId())
                .isEqualTo(5L);
        assertThat(pricingRequest.getCheckInDate())
                .isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(pricingRequest.getCheckOutDate())
                .isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(pricingRequest.getRoomTypes())
                .containsExactlyInAnyOrder(
                        RoomType.SINGLE,
                        RoomType.DELUXE
                );
    }

    @Test
    void shouldReturnBadRequest_whenHotelIdMissing() throws Exception {
        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("customerId", "1")
                        .param("checkInDate", "2026-08-01")
                        .param("checkOutDate", "2026-08-05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenDatesMissing() throws Exception {
        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenDateFormatInvalid() throws Exception {
        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1")
                        .param("checkInDate", "01-08-2026")
                        .param("checkOutDate", "05-08-2026"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void shouldReturn503_whenInventoryClientFailsWithException(Exception exception) throws Exception {
        when(hotelInventoryClient.fetchRoomTypes(5L)).thenThrow(exception);

        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1")
                        .param("checkInDate", "2026-08-01")
                        .param("checkOutDate", "2026-08-05"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnInternalServerError_whenPricingDataIsEmpty() throws Exception {
        RoomTypesResponse roomTypesResponse = validRoomTypesResponse();
        RoomPricingResponse emptyPricing = new RoomPricingResponse(Collections.emptyMap());
        CustomerProfileResponse customerProfileResponse =
                new CustomerProfileResponse(1L, LoyaltyTier.GOLD, 20);

        when(hotelInventoryClient.fetchRoomTypes(hotelId))
                .thenReturn(ResponseEntity.ok(roomTypesResponse));
        when(hotelPricingClient.fetchRoomPrices(any()))
                .thenReturn(ResponseEntity.ok(emptyPricing));
        when(customerProfileClient.fetchCustomerProfile(customerId))
                .thenReturn(ResponseEntity.ok(customerProfileResponse));

        mockMvc.perform(post("/api/hotel-management/v1/availability")
                        .param("hotelId", "5")
                        .param("customerId", "1")
                        .param("checkInDate", "2026-08-01")
                        .param("checkOutDate", "2026-08-05"))
                .andExpect(status().isInternalServerError());
    }

    private RoomPricingResponse validRoomPricingResponse() {
        RoomPricingResponse roomPricingResponse =
                new RoomPricingResponse(
                        Map.of(
                                RoomType.SINGLE, new BigDecimal("100.00"),
                                RoomType.DELUXE, new BigDecimal("200.00")
                        )
                );

        return roomPricingResponse;
    }

    private RoomTypesResponse validRoomTypesResponse() {
        RoomTypesResponse roomTypesResponse =
                new RoomTypesResponse(5L,
                        Map.of(
                                RoomType.SINGLE, 5,
                                RoomType.DELUXE, 2
                        )
                );

        return roomTypesResponse;
    }

    static Stream<Arguments> exceptions() {
        return Stream.of(
                Arguments.of(feignException),
                Arguments.of(new HotelInventoryClientException("Inventory service error"))
        );
    }

    static FeignException feignException = FeignException.errorStatus(
            "fetchRoomTypes",
            Response.builder()
                    .status(500)
                    .reason("Internal Server Error")
                    .request(Request.create(
                            Request.HttpMethod.GET,
                            "/rooms",
                            Map.of(),
                            null,
                            StandardCharsets.UTF_8,
                            null
                    ))
                    .build()
    );
}