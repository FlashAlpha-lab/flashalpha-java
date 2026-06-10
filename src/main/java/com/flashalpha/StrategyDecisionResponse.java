package com.flashalpha;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Shared typed response model for every FlashAlpha Strategy Signals endpoint —
 * {@code GET /v1/strategies/{strategy}/{symbol}}.
 *
 * <p>All ten strategy methods return the SAME decision envelope; only the
 * {@link #metrics} bag and the {@link #regime} label change between strategies.
 * Use the strategy-named client methods to obtain a populated instance:
 * {@link FlashAlphaClient#strategyFlowAnomalyTyped(String)},
 * {@link FlashAlphaClient#strategyExpiryPositioningTyped(String)},
 * {@link FlashAlphaClient#strategyZeroDteTyped(String)},
 * {@link FlashAlphaClient#strategyDealerRegimeTyped(String)},
 * {@link FlashAlphaClient#strategyVolCarryTyped(String)},
 * {@link FlashAlphaClient#strategyYieldEnhancementTyped(String)},
 * {@link FlashAlphaClient#strategySurfaceAnomalyTyped(String)},
 * {@link FlashAlphaClient#strategySkewTyped(String)},
 * {@link FlashAlphaClient#strategyTermStructureTyped(String)}, and
 * {@link FlashAlphaClient#strategyTailPricingTyped(String)}.
 *
 * <p>Each endpoint scores a discretionary options strategy (directional flow
 * anomaly, OPEX pin / expiry positioning, 0DTE range compression, dealer gamma
 * regime, vol-carry / VRP, covered-call / cash-secured-put yield enhancement,
 * SVI surface anomaly, 25-delta skew, ATM term structure, downside-tail
 * pricing) and emits a {@link #decision}, a 0-100 {@link #score}, ranked
 * tradeable {@link #bestStructures}, plain-English {@link #why} / {@link #avoidIf}
 * rationale, optional {@link #riskFlags}, and a {@link #dataQuality} gate.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics — gamma / delta /
 * vanna / charm exposure, dealer hedging, 0DTE pin risk, max pain, VRP, and now
 * strategy-suitability scoring. See <a href="https://flashalpha.com">flashalpha.com</a>.
 */
public final class StrategyDecisionResponse {

    /** Which strategy produced this result (e.g. {@code "flow_anomaly"}, {@code "vol_carry"}). */
    @SerializedName("strategy")
    public String strategy;

    /** Resolved, upper-cased underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** ISO 8601 UTC time the decision was built. */
    @SerializedName("timestamp")
    public String timestamp;

    /** One of {@code insufficient_data}, {@code avoid}, {@code neutral}, {@code candidate}. Derived from {@link #score}. */
    @SerializedName("decision")
    public String decision;

    /** 0-100 strategy score that drives {@link #decision}. */
    @SerializedName("score")
    public Integer score;

    /** 0-1 weight reflecting input quality / sample size. */
    @SerializedName("confidence")
    public Double confidence;

    /** Strategy-specific regime label. Vocabulary differs per endpoint (see each method's Javadoc / the API docs). */
    @SerializedName("regime")
    public String regime;

    /** Ranked candidate structures (may be empty; pure-signal endpoints always return an empty array). */
    @SerializedName("best_structures")
    public List<Structure> bestStructures;

    /**
     * Strategy-specific key/value bag. Keys vary per endpoint; {@code underlying_price}
     * is always present. Values are kept as raw {@link JsonElement} because the shape
     * differs across strategies (numbers, strings, nested arrays of term points, etc.).
     */
    @SerializedName("metrics")
    public Map<String, JsonElement> metrics;

    /** Optional risk callouts (often empty). */
    @SerializedName("risk_flags")
    public List<RiskFlag> riskFlags;

    /** Human-readable rationale for the decision. */
    @SerializedName("why")
    public List<String> why;

    /** Conditions under which the read should be discarded. */
    @SerializedName("avoid_if")
    public List<String> avoidIf;

    /** Gate on this before acting — {@code score} (0-100) and {@code warnings[]}. */
    @SerializedName("data_quality")
    public DataQuality dataQuality;

    // ── Nested types ───────────────────────────────────────────────────────

    /** One ranked tradeable structure. */
    public static final class Structure {
        /** 1-based rank (best first). */
        @SerializedName("rank") public Integer rank;
        /** Structure name, e.g. {@code "short_put_spread"}, {@code "iron_fly"}. */
        @SerializedName("structure") public String structure;
        /** Target expiry of the structure ({@code yyyy-MM-dd}). */
        @SerializedName("expiry") public String expiry;
        /** Legs of the structure. */
        @SerializedName("legs") public List<Leg> legs;
        /** Net credit collected (nullable). */
        @SerializedName("credit") public Double credit;
        /** Net debit paid (nullable). */
        @SerializedName("debit") public Double debit;
        /** Max profit at expiry (nullable when unbounded). */
        @SerializedName("max_profit") public Double maxProfit;
        /** Max loss at expiry (nullable when unbounded). */
        @SerializedName("max_loss") public Double maxLoss;
        /** Breakeven underlying prices. */
        @SerializedName("breakevens") public List<Double> breakevens;
        /** 0-100 edge score for this structure. */
        @SerializedName("edge_score") public Integer edgeScore;
        /** 0-1 liquidity score for this structure. */
        @SerializedName("liquidity_score") public Double liquidityScore;
    }

    /** One leg of a {@link Structure}. */
    public static final class Leg {
        /** {@code "buy"} or {@code "sell"}. */
        @SerializedName("action") public String action;
        /** {@code "call"} or {@code "put"}. */
        @SerializedName("type") public String type;
        /** Strike price. */
        @SerializedName("strike") public Double strike;
        /** Leg delta. */
        @SerializedName("delta") public Double delta;
        /** Per-contract premium. */
        @SerializedName("premium") public Double premium;
        /** Number of contracts. */
        @SerializedName("quantity") public Integer quantity;
    }

    /** One risk callout. */
    public static final class RiskFlag {
        /** {@code "low"} / {@code "medium"} / {@code "high"}. */
        @SerializedName("severity") public String severity;
        /** Machine-readable code, e.g. {@code "EARNINGS_BEFORE_EXPIRY"}. */
        @SerializedName("code") public String code;
        /** Human-readable message. */
        @SerializedName("message") public String message;
    }

    /** Data-quality gate. */
    public static final class DataQuality {
        /** 0-100 input-quality composite. */
        @SerializedName("score") public Integer score;
        /** Quality warnings (may be empty). */
        @SerializedName("warnings") public List<String> warnings;
    }
}
