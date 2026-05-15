package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/gex/{symbol}} (Alpha+). Live (flow-adjusted) GEX with the same per-strike shape as {@link GexResponse} (reuses {@link GexResponse.GexStrikeRow}).
 */
public final class FlowGexResponse {

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

    /** Per-strike breakdown (identical schema to settled GEX). */
    @SerializedName("strikes")
    public List<GexResponse.GexStrikeRow> strikes;
}
