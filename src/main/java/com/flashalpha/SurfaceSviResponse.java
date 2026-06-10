package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/surface/svi/{symbol}} (Alpha+) — the live
 * SVI-fitted volatility surface: calibrated {@code (a, b, rho, m, sigma)}
 * parameters per expiry slice, with the per-expiry forward, ATM total variance,
 * and ATM IV.
 *
 * <p>Obtain via {@link FlashAlphaClient#surfaceSviTyped(String)}.
 */
public final class SurfaceSviResponse {

    /** Resolved, upper-cased underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** Mid of the underlying. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** ISO 8601 UTC time the surface was built. */
    @SerializedName("as_of")
    public String asOf;

    /** True during the regular US equity session. */
    @SerializedName("market_open")
    public Boolean marketOpen;

    /** Per-expiry SVI parameter slices, ordered by {@code days_to_expiry}. */
    @SerializedName("svi_parameters")
    public List<SviSlice> sviParameters;

    /** One per-expiry calibrated SVI slice. */
    public static final class SviSlice {
        @SerializedName("expiry") public String expiry;
        @SerializedName("days_to_expiry") public Integer daysToExpiry;
        @SerializedName("forward") public Double forward;
        @SerializedName("a") public Double a;
        @SerializedName("b") public Double b;
        @SerializedName("rho") public Double rho;
        @SerializedName("m") public Double m;
        @SerializedName("sigma") public Double sigma;
        @SerializedName("atm_total_variance") public Double atmTotalVariance;
        /** ATM implied vol as a percentage. */
        @SerializedName("atm_iv") public Double atmIv;
    }
}
