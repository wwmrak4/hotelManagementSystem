package uk.co.hms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.co.hms.exception.*;
import uk.co.hms.model.ApiErrorResult;
import uk.co.hms.model.Error;
import java.util.List;
import static java.util.stream.Collectors.toList;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    @ResponseBody
    ResponseEntity<ApiErrorResult> handleBindException(final HttpServletRequest request,
                                                       final BindException ex) {
        log.error("BindException: ", ex);
        ApiErrorResult apiErrorResult = new ApiErrorResult();
        List<uk.co.hms.model.Error> errors = ex.getFieldErrors()
                .stream()
                .map(fe -> uk.co.hms.model.Error.builder()
                        .code(fe.getField())
                        .description(fe.getDefaultMessage()).build())
                .collect(toList());
        errors.forEach(apiErrorResult::add);
        return new ResponseEntity<>(apiErrorResult, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    ResponseEntity<ApiErrorResult> handleValidationException(final HttpServletRequest request,
                                                             final ValidationException ex) {
        log.error("ValidationException : ", ex);
        return new ResponseEntity<>(ex.getApiErrorResult(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(AvailabilityServiceException.class)
    @ResponseBody
    ResponseEntity handleAvailabilityServiceException(final HttpServletRequest request,
                                                      final AvailabilityServiceException ex) {
        log.error("AvailabilityServiceException: ", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(uk.co.hms.model.Error.builder()
                        .description(ex.getMessage()).build());
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler({
            HotelInventoryClientException.class,
            HotelPricingClientException.class,
            CustomerProfileClientException.class
    })
    @ResponseBody
    ResponseEntity<Error> handleHttpClientsExceptions(
            final HttpServletRequest request,
            final Exception ex) {

        log.error("Exception: ", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(uk.co.hms.model.Error.builder()
                        .description(ex.getMessage()).build());
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(FeignException.class)
    @ResponseBody
    public ResponseEntity handleFeignException(final HttpServletRequest request,
                                               final FeignException ex) {
        String exceptionMessage = String.format(
                "API call failed. %s %s -> status %s",
                ex.request().httpMethod(),
                ex.request().url(),
                ex.status()
        );
        log.error(exceptionMessage);

        return new ResponseEntity<>(uk.co.hms.model.Error.builder()
                .description(exceptionMessage)
                .build(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    ResponseEntity handleOtherExceptions(HttpServletRequest req, Exception ex) {
        log.error("Exception: ", ex);

        return new ResponseEntity<>(Error.builder()
                .description(ex.getMessage())
                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
