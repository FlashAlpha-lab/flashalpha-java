package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/gex/{symbol}} (Alpha+). Live (flow-adjusted) GEX with the same per-strike shape as {@link GexResponse} (reuses {@link GexResponse.GexStrikeRow}).
 */
public final class FlowGexResponse extends FlashAlphaResponse {

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

    /** Live net GEX across the chain (dollars per 1% spot move). */
    @SerializedName("live_net_gex")
    public Double liveNetGex;

    /** Categorical regime label (e.g. "positive", "negative"). Safe to surface verbatim. */
    @SerializedName("live_net_gex_label")
    public String liveNetGexLabel;

    /** Live gamma-flip spot, or null if no sign change. */
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

    /** Per-strike breakdown (identical schema to settled GEX). */
    @SerializedName("strikes")
    public List<GexResponse.GexStrikeRow> strikes;
}
