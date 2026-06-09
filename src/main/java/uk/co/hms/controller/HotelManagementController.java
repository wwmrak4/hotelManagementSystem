package uk.co.hms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.hms.exception.AvailabilityServiceException;
import uk.co.hms.model.AvailabilityRequest;
import uk.co.hms.model.AvailabilityResponse;
import uk.co.hms.service.AvailabilityService;

@Slf4j
@RestController
@RequestMapping(value = "/api/hotel-management/${hms.api.version}")
@RequiredArgsConstructor
public class HotelManagementController {
    @Autowired
    AvailabilityService availabilityService;

    @PostMapping(value = "/availability", produces = "application/json")
    public ResponseEntity<AvailabilityResponse> getAvailability(
            @Valid AvailabilityRequest availabilityRequest, BindingResult result)
            throws AvailabilityServiceException, BindException {

        if (result.hasErrors()) {
            throw new BindException(result);
        }

        log.info("Received availability request for hotelId={}, customerId={}",
                availabilityRequest.getHotelId(),
                availabilityRequest.getCustomerId());

        final AvailabilityResponse response = availabilityService.getAvailability(availabilityRequest);

        return ResponseEntity.ok(response);
    }
}
