package com.flashalpha;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * One scored unusual-flow signal — a coalesced view of one notable
 * (block-sized) print on a single contract. Same shape across
 * {@code GET /v1/flow/signals/{symbol}} and the {@code top_signals}
 * array of {@code GET /v1/flow/signals/{symbol}/summary}.
 */
public final class FlowSignal {

    /** Trade timestamp (ISO-8601 UTC). */
    @SerializedName("ts")
    public String ts;

    /** Contract expiry ({@code YYYY-MM-DD}). */
    @SerializedName("expiry")
    public String expiry;

    /** Contract strike price. */
    @SerializedName("strike")
    public Double strike;

    /** {@code "C"} (call) or {@code "P"} (put). */
    @SerializedName("right")
    public String right;

    /**
     * Upstream buy/sell/mid aggressor classification (distinct from the
     * NBBO {@link #aggressor} label).
     */
    @SerializedName("side")
    public String side;

    /** Trade price. */
    @SerializedName("price")
    public Double price;

    /** Trade size in contracts. */
    @SerializedName("size")
    public Long size;

    /** Dollar premium of this print: {@code price * size * 100}. */
    @SerializedName("premium")
    public Double premium;

    /** Days to expiry at trade time. */
    @SerializedName("dte")
    public Integer dte;

    /**
     * {@code "block"} (lone block-sized print) or {@code "sweep"} (≥2
     * same-side prints on one contract within ~500ms).
     */
    @SerializedName("structure")
    public String structure;

    /**
     * NBBO position at trade: {@code "above_ask"} / {@code "at_ask"} /
     * {@code "mid"} / {@code "at_bid"} / {@code "below_bid"}.
     */
    @SerializedName("aggressor")
    public String aggressor;

    /**
     * Contract-level OI-simulator inference: {@code "opening_bias"} /
     * {@code "closing_bias"} / {@code "unknown"}. Not a per-print label.
     */
    @SerializedName("open_close_bias")
    public String openCloseBias;

    /** Simulator confidence weight for the bias above. */
    @SerializedName("open_close_confidence")
    public Double openCloseConfidence;

    /**
     * Signed simulator estimate of contracts opened (+) or closed (−)
     * today on this contract.
     */
    @SerializedName("contract_net_oi_delta")
    public Long contractNetOiDelta;

    /**
     * {@code "bullish"} / {@code "bearish"} / {@code "neutral"}. Neutral
     * whenever {@link #openCloseBias} equals {@code "closing_bias"}
     * (can't attribute on unwinds) or {@link #side} equals {@code "mid"}.
     */
    @SerializedName("intent")
    public String intent;

    /** 0–100 composite ({@link #scoreBreakdown} components sum to this). */
    @SerializedName("score")
    public Integer score;

    /** {@code "low"} / {@code "medium"} / {@code "high"}. */
    @SerializedName("conviction")
    public String conviction;

    /**
     * Subset of {@code "sweep"}, {@code "block"}, {@code "opening"},
     * {@code "closing"}, {@code "0dte"}, {@code "whale"} (premium ≥ $1M),
     * {@code "golden"} (top decile in this response set AND score ≥ 70
     * absolute).
     */
    @SerializedName("tags")
    public List<String> tags;

    /** Score components — sum to {@link #score}. */
    @SerializedName("score_breakdown")
    public ScoreBreakdown scoreBreakdown;

    /** Chain-derived context (greeks, moneyness, estimated delta-notional). */
    @SerializedName("enrichment")
    public Enrichment enrichment;


    /**
     * Component contributions that sum to the headline {@link #score}.
     * Weights are server-tunable so absolute values may shift, but the
     * ordering of components is stable.
     */
    public static final class ScoreBreakdown {
        /** Premium-size contribution (the larger the dollar premium, the more points). */
        @SerializedName("premium") public Integer premium;
        /** Print size relative to the contract's open interest. */
        @SerializedName("size_vs_oi") public Integer sizeVsOi;
        /** NBBO aggressor strength — above-ask / at-ask earn more than mid. */
        @SerializedName("aggressor") public Integer aggressor;
        /** Sweep boost (≥2 same-side prints on one contract within ~500ms). */
        @SerializedName("sweep") public Integer sweep;
        /** OI-simulator opening-bias contribution. */
        @SerializedName("opening_bias") public Integer openingBias;
        /** Tenor (DTE) contribution — short-dated prints score differently than long-dated. */
        @SerializedName("tenor") public Integer tenor;
    }

    /**
     * Chain-derived context attached to a signal. All numeric fields are
     * {@code null} and {@link #moneyness} is {@code "unknown"} when the
     * contract isn't in the settled chain snapshot.
     */
    public static final class Enrichment {
        /** Contract implied vol (decimal, e.g. {@code 0.62} = 62%). */
        @SerializedName("iv") public Double iv;
        /** Contract delta (signed; positive for calls, negative for puts). */
        @SerializedName("delta") public Double delta;
        /** Contract gamma (per-share). */
        @SerializedName("gamma") public Double gamma;
        /** IV minus the nearest ATM IV (signed). */
        @SerializedName("iv_vs_atm") public Double ivVsAtm;
        /** {@code "OTM"} / {@code "ATM"} / {@code "ITM"} / {@code "unknown"}. */
        @SerializedName("moneyness") public String moneyness;
        /** Estimated dollar delta-notional of this print. */
        @SerializedName("estimated_delta_notional") public Double estimatedDeltaNotional;
        /**
         * Standalone gamma-$ this print would add if it were opening and
         * fully dealer-absorbed. <strong>Not</strong> applied to the live
         * chain — don't sum it against {@code /v1/flow/gex}.
         */
        @SerializedName("hypothetical_gex_impact_if_opening") public Double hypotheticalGexImpactIfOpening;
    }
}
