package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/signals/{symbol}/summary}
 * (Alpha+). Sums classified premium across the window into
 * bullish/bearish and opening/closing buckets — a cheap "smart-money
 * tilt" read for one underlying.
 */
public final class FlowSignalsSummaryResponse {

    /** Underlying ticker echoed from the request path. */
    @SerializedName("symbol")
    public String symbol;

    /** Timestamp this snapshot was computed for (ISO-8601 UTC). */
    @SerializedName("as_of")
    public String asOf;

    /** Look-back window applied (minutes). */
    @SerializedName("window_minutes")
    public Integer windowMinutes;

    /** Expiration filter echoed back, or null. */
    @SerializedName("expiry")
    public String expiry;

    /** Spot mid at the snapshot time. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /**
     * Total signal count in the window (full count, not the
     * {@link #topSignals} length).
     */
    @SerializedName("signal_count")
    public Integer signalCount;

    /** Sum of signal premium with {@code intent == "bullish"}. */
    @SerializedName("bullish_premium")
    public Double bullishPremium;

    /** Sum of signal premium with {@code intent == "bearish"}. */
    @SerializedName("bearish_premium")
    public Double bearishPremium;

    /** {@code bullish_premium - bearish_premium}. */
    @SerializedName("net_directional_premium")
    public Double netDirectionalPremium;

    /** Sum of signal premium with {@code open_close_bias == "opening_bias"}. */
    @SerializedName("opening_premium")
    public Double openingPremium;

    /** Sum of signal premium with {@code open_close_bias == "closing_bias"}. */
    @SerializedName("closing_premium")
    public Double closingPremium;

    /** Highest-scoring signals (≤ 10). Same shape as {@link FlowSignal}. */
    @SerializedName("top_signals")
    public List<FlowSignal> topSignals;
}
