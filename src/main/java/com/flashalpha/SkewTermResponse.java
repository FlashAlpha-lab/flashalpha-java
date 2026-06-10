package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/volatility/skew-term/{symbol}} (Growth+) —
 * skew term structure with vol-desk naming conventions. For each expiry: ATM IV,
 * 25Δ and 10Δ wing IVs, and the named conventions {@code skew_25d},
 * {@code risk_reversal_25d}, {@code butterfly_25d}, plus {@code tail_convexity}.
 *
 * <p>Obtain via {@link FlashAlphaClient#skewTermTyped(String)}.
 */
public final class SkewTermResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** Per-expiry skew metrics. */
    @SerializedName("expiries")
    public List<SkewExpiry> expiries;

    /** One expiry's skew profile. */
    public static final class SkewExpiry {
        @SerializedName("expiry") public String expiry;
        @SerializedName("dte") public Integer dte;
        /** At-the-money IV (%). */
        @SerializedName("atm_iv") public Double atmIv;
        @SerializedName("put_25d_iv") public Double put25dIv;
        @SerializedName("call_25d_iv") public Double call25dIv;
        @SerializedName("put_10d_iv") public Double put10dIv;
        @SerializedName("call_10d_iv") public Double call10dIv;
        /** {@code put_25d_iv − call_25d_iv}. Positive ⇒ put skew dominant. */
        @SerializedName("skew_25d") public Double skew25d;
        /** {@code call_25d_iv − put_25d_iv} (= −skew_25d). */
        @SerializedName("risk_reversal_25d") public Double riskReversal25d;
        /** {@code (call_25d + put_25d)/2 − atm}. Wing premium over ATM. */
        @SerializedName("butterfly_25d") public Double butterfly25d;
        /** Second difference of the put wing. Positive ⇒ steep tail. */
        @SerializedName("tail_convexity") public Double tailConvexity;
    }
}
