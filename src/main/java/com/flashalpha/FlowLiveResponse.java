package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response for {@code GET /v1/flow/live/{symbol}} (Alpha+). Everything-at-once convenience bundle: OI simulator state + live exposure + live levels + pin risk + the nested dealer-risk block.
 */
public final class FlowLiveResponse extends FlashAlphaResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Spot mid at the snapshot time. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Total contracts tracked for the underlying. */
    @SerializedName("contracts")
    public Integer contracts;

    /** Contracts that printed at least one trade today. */
    @SerializedName("contracts_with_flow")
    public Integer contractsWithFlow;

    /** Official exchange OI from the settled snapshot. */
    @SerializedName("official_oi")
    public Long officialOi;

    /** Intraday simulated OI. */
    @SerializedName("simulated_oi")
    public Long simulatedOi;

    /** {@code simulated_oi - official_oi} (signed). */
    @SerializedName("intraday_oi_delta")
    public Long intradayOiDelta;

    /** Confidence 0-1 in the intraday OI estimate. */
    @SerializedName("oi_delta_confidence")
    public Double oiDeltaConfidence;

    /** OI actually used by the live analytics (blended). */
    @SerializedName("effective_oi")
    public Long effectiveOi;

    /** Live net GEX (dollars per 1% spot move). */
    @SerializedName("live_gex")
    public Double liveGex;

    /** Live net DEX (dollars). Named {@code live_gex_delta} on the wire. */
    @SerializedName("live_gex_delta")
    public Double liveGexDelta;

    /** Live gamma-flip spot, or null. */
    @SerializedName("live_gamma_flip")
    public Double liveGammaFlip;

    /**
     * Why {@link #liveGammaFlip} is populated or withheld. Reads {@code "available"}
     * when a flip level was published, otherwise a reason code such as
     * {@code "no_boundary"}, {@code "insufficient_local_coverage"} or
     * {@code "sensitive_root"}. New codes can be added at any time, so treat
     * any value other than {@code "available"} as no flip level available.
     *
     * <p>The wire name is {@code gamma_flip_status}, <em>not</em>
     * {@code live_gamma_flip_status}, even though the value it explains is
     * {@code live_gamma_flip}.
     */
    @SerializedName("gamma_flip_status")
    public String gammaFlipStatus;

    /** Live call wall strike, or null. */
    @SerializedName("live_call_wall")
    public Double liveCallWall;

    /** Live put wall strike, or null. */
    @SerializedName("live_put_wall")
    public Double livePutWall;

    /** Live max-pain strike, or null. */
    @SerializedName("live_max_pain")
    public Double liveMaxPain;

    /** Composite 0-100 pin-risk score. */
    @SerializedName("live_pin_risk")
    public Integer livePinRisk;

    /** Nested settled-vs-live dealer-risk block. */
    @SerializedName("flow_adjusted_dealer_risk")
    public FlowAdjustedDealerRisk flowAdjustedDealerRisk;


    /**
     * Nested dealer-risk block. Identical to {@link FlowDealerRiskResponse}
     * minus {@code contracts_with_flow} (carried on the parent envelope).
     */
    public static final class FlowAdjustedDealerRisk {
        /** Net GEX from the settled snapshot. */
        @SerializedName("settled_net_gex") public Double settledNetGex;
        /** Net GEX from the live flow-adjusted book. */
        @SerializedName("live_net_gex") public Double liveNetGex;
        /** {@code live_net_gex - settled_net_gex} (dollars). */
        @SerializedName("flow_gex_adjustment") public Double flowGexAdjustment;
        /** % GEX shift from flow; null when the settled baseline is zero. */
        @SerializedName("flow_gex_pct_shift") public Double flowGexPctShift;
        /** Net DEX from the settled snapshot. */
        @SerializedName("settled_net_dex") public Double settledNetDex;
        /** Net DEX from the live flow-adjusted book. */
        @SerializedName("live_net_dex") public Double liveNetDex;
        /** {@code live_net_dex - settled_net_dex} (dollars). */
        @SerializedName("flow_dex_adjustment") public Double flowDexAdjustment;
        /** % DEX shift from flow; null when the settled baseline is zero. */
        @SerializedName("flow_dex_pct_shift") public Double flowDexPctShift;
        /** Absolute delta-weighted contracts traded today (flow magnitude). */
        @SerializedName("total_abs_delta_contracts") public Long totalAbsDeltaContracts;
        /** Net classified flow direction. */
        @SerializedName("flow_direction") public String flowDirection;
        /** Plain-English summary. Safe to surface verbatim. */
        @SerializedName("description") public String description;
    }
}
