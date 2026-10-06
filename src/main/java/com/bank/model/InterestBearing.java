package com.bank.model;

import java.math.BigDecimal;

/**
 * Interface representing interest-earning accounts or financial products.
 * <p>
 * Implemented by deposit accounts that accrue compound or simple interest over time.
 * </p>
 */
public interface InterestBearing {

    /**
     * Calculates the monthly interest accrued on the current balance based on an annual rate.
     *
     * @param annualRatePercent the annual percentage rate (e.g. 4.00 for 4%)
     * @return monthly interest accrued, scaled to 2 decimal places with HALF_UP rounding
     */
    BigDecimal calculateMonthlyInterest(BigDecimal annualRatePercent);
}
