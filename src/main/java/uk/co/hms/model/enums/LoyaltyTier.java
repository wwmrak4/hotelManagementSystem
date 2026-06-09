package uk.co.hms.model.enums;

import java.math.BigDecimal;

public enum LoyaltyTier {
    GOLD(new BigDecimal("0.70")),
    SILVER(new BigDecimal("0.80")),
    BRONZE(new BigDecimal("0.90"));

    private final BigDecimal discount;

    LoyaltyTier(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }
}
