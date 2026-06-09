package uk.co.hms.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApiErrorResult {

    private List<Error> errors = new ArrayList<>();

    public void add(Error error) {
        errors.add(error);
    }
}
