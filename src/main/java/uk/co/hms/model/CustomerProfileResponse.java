package uk.co.hms.model;

import uk.co.hms.model.enums.LoyaltyTier;

public record CustomerProfileResponse(
        Long customerId,
        LoyaltyTier loyaltyTier,
        Integer loyaltyPoints
) {
}

