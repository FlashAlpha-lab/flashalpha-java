package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/volatility/realized/{symbol}} (Alpha+) —
 * range-based realized (historical) volatility estimators computed over
 * 10 / 20 / 30-day windows.
 *
 * <p>Five fixed estimator families are returned: close-to-close, Parkinson,
 * Garman-Klass, Rogers-Satchell, and Yang-Zhang. Each shares the same
 * {@code rv10 / rv20 / rv30} window shape, modelled once by the reusable
 * {@link Estimator} class. All {@code rvNN} values are annualised realized
 * volatility in percent and are nullable (boxed {@code Double}).
 *
 * <p>Obtain via {@link FlashAlphaClient#realizedVolatilityTyped(String)}.
 */
public final class RealizedVolatilityResponse {

    /** Resolved, upper-cased underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** ISO 8601 UTC time the response was built. */
    @SerializedName("as_of")
    public String asOf;

    /** Spot used for the computation (nullable). */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** The five realized-vol estimator families. */
    @SerializedName("estimators")
    public Estimators estimators;

    /** Container for the five fixed estimator families. */
    public static final class Estimators {
        @SerializedName("close_to_close") public Estimator closeToClose;
        @SerializedName("parkinson") public Estimator parkinson;
        @SerializedName("garman_klass") public Estimator garmanKlass;
        @SerializedName("rogers_satchell") public Estimator rogersSatchell;
        @SerializedName("yang_zhang") public Estimator yangZhang;
    }

    /**
     * One estimator's annualised realized vol (percent) across the fixed
     * 10 / 20 / 30-day windows. All values nullable.
     */
    public static final class Estimator {
        /** 10-day annualised realized vol (percent, nullable). */
        @SerializedName("rv10") public Double rv10;
        /** 20-day annualised realized vol (percent, nullable). */
        @SerializedName("rv20") public Double rv20;
        /** 30-day annualised realized vol (percent, nullable). */
        @SerializedName("rv30") public Double rv30;
    }
}
