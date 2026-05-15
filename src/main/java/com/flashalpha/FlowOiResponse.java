package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/oi/{symbol}} (Alpha+). Settled (official) OI vs the intraday simulated OI. This endpoint does NOT return {@code underlying_price}.
 */
public final class FlowOiResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Official exchange OI from the settled snapshot (sum across the chain). */
    @SerializedName("official_oi")
    public Long officialOi;

    /** Intraday simulated OI (official + estimated open/close from the tape). */
    @SerializedName("simulated_oi")
    public Long simulatedOi;

    /** {@code simulated_oi - official_oi} (signed). */
    @SerializedName("intraday_oi_delta")
    public Long intradayOiDelta;

    /** Confidence 0-1 in the intraday OI estimate (trade-tape coverage). */
    @SerializedName("oi_delta_confidence")
    public Double oiDeltaConfidence;

    /** OI actually used by the live analytics (blended). */
    @SerializedName("effective_oi")
    public Long effectiveOi;

    /** Total contracts tracked for the underlying. */
    @SerializedName("contracts_total")
    public Integer contractsTotal;

    /** Contracts that printed at least one trade today. */
    @SerializedName("contracts_with_flow")
    public Integer contractsWithFlow;
}
