package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Typed response for {@code GET /v1/flow/signals/{symbol}} (Alpha+). The
 * scored, classified unusual-flow feed: each notable print in the
 * look-back window is coalesced into a signal, scored 0–100, and ranked
 * highest score first.
 */
public final class FlowSignalsResponse {

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

    /** Settled-chain reference levels. */
    @SerializedName("chain")
    public Chain chain;

    /** Number of signals returned (after server-side filtering). */
    @SerializedName("count")
    public Integer count;

    /** Highest-scoring first. */
    @SerializedName("signals")
    public List<FlowSignal> signals;


    /**
     * Settled-chain reference levels echoed alongside the signals.
     * Computed once per request from the settled snapshot — independent
     * of the live flow surface. All fields are {@code null} when the
     * chain snapshot is unavailable.
     */
    public static final class Chain {
        /** Strike with the largest settled call GEX — upside dealer-defended level. */
        @SerializedName("call_wall") public Double callWall;
        /** Strike with the largest settled put GEX — downside dealer-defended level. */
        @SerializedName("put_wall") public Double putWall;
        /** Strike where total option-holder loss is maximized at expiry. */
        @SerializedName("max_pain") public Double maxPain;
        /** Settled gamma-flip strike (sign change of net GEX across the chain). */
        @SerializedName("gamma_flip") public Double gammaFlip;
    }
}
