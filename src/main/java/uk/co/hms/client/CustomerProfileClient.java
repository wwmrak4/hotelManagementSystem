package uk.co.hms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.co.hms.model.CustomerProfileResponse;

@FeignClient(name = "customerProfileApiClient", url = "${customer.profile.api.endpoint}")
public interface CustomerProfileClient {

    @GetMapping(value = "${customer.profile.api.version}/customer-profile" )
    ResponseEntity<CustomerProfileResponse> fetchCustomerProfile(@PathVariable Long customerId);
}