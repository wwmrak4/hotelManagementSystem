package uk.co.hms.exception;

import lombok.Getter;
import uk.co.hms.model.ApiErrorResult;

@Getter
public class ValidationException extends RuntimeException {

    private ApiErrorResult apiErrorResult;

    public ValidationException(ApiErrorResult apiErrorResult) {
        this.apiErrorResult = apiErrorResult;
    }
}