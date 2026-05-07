package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/stock/{symbol}/summary}.
 *
 * <p>The single most expressive endpoint in the FlashAlpha API — a
 * one-shot dashboard view that fuses live price, the volatility surface
 * (ATM IV / HV / VRP / skew / term structure), full-chain options flow,
 * the dealer-exposure block (GEX / DEX / VEX / CHEX, gamma flip,
 * call/put walls, max pain, regime, hedging estimate, 0DTE share, top
 * strikes) and a macro overlay (VIX / VVIX / SKEW / SPX / MOVE plus VIX
 * term structure, VIX futures basis, fear-and-greed).
 *
 * <p><b>Dual mode:</b> when the request is authenticated with a valid
 * API key the response is a live snapshot computed from the current
 * options chain; when no key is supplied the API instead serves the
 * previous-day cached snapshot — same shape, same fields, just stale.
 * Use {@link #marketOpen} and {@link #asOf} to tell which one you got.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 *
 * <p>All numeric fields are boxed wrappers ({@link Double}, {@link Long},
 * {@link Integer}, {@link Boolean}) so {@code null} can represent values
 * the API could not compute.
 */
public final class StockSummaryResponse {

    /** Echoed from the request path (e.g. {@code "SPY"}). */
    @SerializedName("symbol")
    public String symbol;

    /** ET wall-clock timestamp this snapshot was computed for. */
    @SerializedName("as_of")
    public String asOf;

    /** {@code true} if NYSE was open at {@link #asOf}. After-hours snapshots may be stale. */
    @SerializedName("market_open")
    public Boolean marketOpen;

    /** Live price block — bid/ask/mid/last and last-update timestamp. */
    @SerializedName("price")
    public Price price;

    /** Volatility block — ATM IV, realized vol, VRP, skew, term structure. */
    @SerializedName("volatility")
    public Volatility volatility;

    /** Full-chain options flow — call/put OI and volume, P/C ratios, active expirations. */
    @SerializedName("options_flow")
    public OptionsFlow optionsFlow;

    /**
     * Dealer-exposure block. {@code null} when no options/greeks data is
     * loaded for this symbol (illiquid names, stale chain, or symbols
     * outside the FlashAlpha analytics universe).
     */
    @SerializedName("exposure")
    public Exposure exposure;

    /**
     * Macro context overlay (VIX, VVIX, SKEW, SPX, MOVE plus VIX term
     * structure / futures basis / fear-and-greed). Individual sub-fields
     * may be {@code null} when the upstream macro source is unavailable.
     */
    @SerializedName("macro")
    public Macro macro;

    // ── Price ──────────────────────────────────────────────────────────────

    /** Live price snapshot — best bid/ask/mid plus the most recent print. */
    public static final class Price {
        /** Best bid. */
        @SerializedName("bid") public Double bid;
        /** Best ask. */
        @SerializedName("ask") public Double ask;
        /** Bid-ask mid: {@code (bid + ask) / 2}. */
        @SerializedName("mid") public Double mid;
        /** Last trade print. */
        @SerializedName("last") public Double last;
        /** ET wall-clock timestamp of the last trade. */
        @SerializedName("last_update") public String lastUpdate;
    }

    // ── Volatility ─────────────────────────────────────────────────────────

    /**
     * Volatility block — fuses implied (forward-looking) and realized
     * (backward-looking) vol with directional skew and the IV term
     * structure. All vol numbers in this block are quoted as PERCENTS
     * (e.g. {@code 18.45} = 18.45%, not 0.1845).
     */
    public static final class Volatility {
        /** At-the-money implied volatility (annualised %, e.g. {@code 18.45} = 18.45%). */
        @SerializedName("atm_iv") public Double atmIv;

        /** Realized volatility over the trailing 20 trading days (annualised %). */
        @SerializedName("hv_20") public Double hv20;

        /** Realized volatility over the trailing 60 trading days (annualised %). */
        @SerializedName("hv_60") public Double hv60;

        /**
         * Variance risk premium: {@code atm_iv - hv_20} (in percentage
         * points, e.g. {@code 3.2} means IV is 3.2pp above 20-day RV).
         * Positive = options pricing more vol than the underlying actually
         * moved → premium for selling vol.
         */
        @SerializedName("vrp") public Double vrp;

        /** 25-delta directional skew block. */
        @SerializedName("skew_25d") public Skew25d skew25d;

        /**
         * IV term structure rows (one per active expiry). Server-side
         * filter: rows with IV &lt; 5% or &gt; 200% are dropped (those
         * are typically bad SVI fits, not real surface points).
         */
        @SerializedName("iv_term_structure") public List<TermStructureRow> ivTermStructure;
    }

    /**
     * 25-delta wing skew — the standard cross-vendor skew quote.
     * Negative {@link #skew25d} (puts richer than calls) is the typical
     * equity-index regime; positive skew flags catalyst-driven call demand.
     */
    public static final class Skew25d {
        /** ISO expiration date this skew row was measured at. */
        @SerializedName("expiry") public String expiry;
        /** Days to expiry from {@link StockSummaryResponse#asOf}. */
        @SerializedName("days_to_expiry") public Integer daysToExpiry;
        /** 25-delta put-wing IV (annualised %). */
        @SerializedName("put_25d_iv") public Double put25dIv;
        /** ATM IV at this tenor (annualised %) — the skew anchor. */
        @SerializedName("atm_iv") public Double atmIv;
        /** 25-delta call-wing IV (annualised %). */
        @SerializedName("call_25d_iv") public Double call25dIv;
        /** {@code put_25d_iv - call_25d_iv} — positive = puts richer than calls. */
        @SerializedName("skew_25d") public Double skew25d;
        /** {@code (put_25d_iv + call_25d_iv) / (2 * atm_iv)} — wing-vs-ATM smile shape. */
        @SerializedName("smile_ratio") public Double smileRatio;
    }

    /** One row of the IV term structure. */
    public static final class TermStructureRow {
        /** ISO expiration date. */
        @SerializedName("expiry") public String expiry;
        /** Days to expiry from {@link StockSummaryResponse#asOf}. */
        @SerializedName("days_to_expiry") public Integer daysToExpiry;
        /** ATM implied volatility at this tenor (annualised %). */
        @SerializedName("iv") public Double iv;
    }

    // ── Options flow ───────────────────────────────────────────────────────

    /** Full-chain OI / volume aggregates and put-call ratios. */
    public static final class OptionsFlow {
        /** Total call open interest across the chain. */
        @SerializedName("total_call_oi") public Long callOi;
        /** Total put open interest across the chain. */
        @SerializedName("total_put_oi") public Long putOi;
        /** Total call contract volume across the chain (today). */
        @SerializedName("total_call_volume") public Long callVolume;
        /** Total put contract volume across the chain (today). */
        @SerializedName("total_put_volume") public Long putVolume;
        /** Put-call ratio by open interest: {@code total_put_oi / total_call_oi}. */
        @SerializedName("pc_ratio_oi") public Double pcRatioOi;
        /** Put-call ratio by volume: {@code total_put_volume / total_call_volume}. */
        @SerializedName("pc_ratio_volume") public Double pcRatioVolume;
        /** Number of expirations with active OI. */
        @SerializedName("active_expirations") public Integer activeExpirations;
    }

    // ── Exposure ───────────────────────────────────────────────────────────

    /**
     * Dealer-exposure block — net Greek totals plus the canonical levels
     * (gamma flip, call/put walls, max pain, highest-OI strike), regime
     * label, plain-English interpretations, hedging estimate, 0DTE share,
     * the top strikes by net GEX, and the OI-weighted average DTE.
     *
     * <p>The whole block is {@code null} when no options/greeks data is
     * loaded for the symbol.
     */
    public static final class Exposure {
        /** Net dealer gamma exposure (dollars per 1% spot move). */
        @SerializedName("net_gex") public Double netGex;
        /** Net dealer delta exposure (shares-equivalent). */
        @SerializedName("net_dex") public Double netDex;
        /** Net dealer vanna exposure. */
        @SerializedName("net_vex") public Double netVex;
        /** Net dealer charm exposure. */
        @SerializedName("net_chex") public Double netChex;
        /** Strike where net dealer gamma crosses zero. */
        @SerializedName("gamma_flip") public Double gammaFlip;
        /** Strike with the highest absolute call GEX (dealer-side resistance). */
        @SerializedName("call_wall") public Double callWall;
        /** Strike with the highest absolute put GEX (dealer-side support). */
        @SerializedName("put_wall") public Double putWall;
        /** Max-pain strike — total option-holder pain minimised. */
        @SerializedName("max_pain") public Double maxPain;
        /** Strike with the largest combined call+put OI. */
        @SerializedName("highest_oi_strike") public Double highestOiStrike;

        /**
         * Dealer gamma regime: one of
         * {@code "positive_gamma"} | {@code "negative_gamma"} |
         * {@code "unknown"}.
         */
        @SerializedName("regime") public String regime;

        /** Plain-English interpretation of gamma / vanna / charm regimes. */
        @SerializedName("interpretation") public Interpretation interpretation;

        /** Estimated dealer hedging flow at +/- 1% spot moves. */
        @SerializedName("hedging_estimate") public HedgingEstimate hedgingEstimate;

        /** Same-day-expiration contribution to total GEX. */
        @SerializedName("zero_dte") public ZeroDteShare zeroDte;

        /** Top strikes by absolute net GEX (descending). */
        @SerializedName("top_strikes") public List<TopStrikeRow> topStrikes;

        /** OI-weighted average days-to-expiry across the chain. */
        @SerializedName("oi_weighted_dte") public Double oiWeightedDte;
    }

    /** Plain-English interpretation strings for each Greek regime. Safe to surface verbatim. */
    public static final class Interpretation {
        @SerializedName("gamma") public String gamma;
        @SerializedName("vanna") public String vanna;
        @SerializedName("charm") public String charm;
    }

    /**
     * Estimated dealer hedging flow at +/- 1% spot moves.
     *
     * <p><b>Sign convention diff vs zero-DTE:</b> on this endpoint
     * {@link HedgingMove#dealerShares} is a MAGNITUDE (always non-negative)
     * and the {@link HedgingMove#direction} string ({@code "buy"} /
     * {@code "sell"}) carries the sign. The zero-DTE endpoint instead
     * uses signed values directly. Don't naively reuse parsing logic
     * across the two.
     */
    public static final class HedgingEstimate {
        @SerializedName("spot_up_1pct")   public HedgingMove spotUp1Pct;
        @SerializedName("spot_down_1pct") public HedgingMove spotDown1Pct;
    }

    /** One side of the hedging-estimate block. */
    public static final class HedgingMove {
        /** Dealer share count to trade — MAGNITUDE on this endpoint, sign carried by {@link #direction}. */
        @SerializedName("dealer_shares") public Double dealerShares;
        /** {@code "buy"} or {@code "sell"} — direction of dealer flow under this scenario. */
        @SerializedName("direction") public String direction;
        /** Estimated dollar notional of the dealer trade. */
        @SerializedName("notional_usd") public Double notionalUsd;
    }

    /** Same-day-expiration contribution to full-chain GEX. */
    public static final class ZeroDteShare {
        /** Net dealer gamma exposure (dollars per 1% spot) at the 0DTE expiration. */
        @SerializedName("net_gex") public Double netGex;
        /** 0DTE GEX as a percent of full-chain total (0-100). */
        @SerializedName("pct_of_total") public Double pctOfTotal;
        /** ISO date of the 0DTE expiration this fragment summarises. */
        @SerializedName("expiration") public String expiration;
    }

    /** One row of the top-strikes-by-absolute-net-GEX list. */
    public static final class TopStrikeRow {
        @SerializedName("strike") public Double strike;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("call_oi") public Long callOi;
        @SerializedName("put_oi") public Long putOi;
        /** Combined call+put OI at this strike. */
        @SerializedName("total_oi") public Long totalOi;
    }

    // ── Macro ──────────────────────────────────────────────────────────────

    /**
     * Macro overlay — VIX / VVIX / SKEW / SPX / MOVE quotes plus the
     * VIX term structure, the VIX-futures basis approximation, and the
     * fear-and-greed index. Any sub-field can be {@code null} when the
     * upstream macro source is unavailable.
     */
    public static final class Macro {
        /** CBOE VIX index. */
        @SerializedName("vix") public Quote vix;
        /** CBOE VVIX (vol of VIX). */
        @SerializedName("vvix") public Quote vvix;
        /** CBOE SKEW index. */
        @SerializedName("skew") public Quote skew;
        /** S&amp;P 500 cash index. */
        @SerializedName("spx") public Quote spx;
        /** ICE BofAML MOVE index (Treasury vol). */
        @SerializedName("move") public Quote move;

        /** VIX term structure (1M / 3M / 6M / 9M points). */
        @SerializedName("vix_term_structure") public VixTermStructure vixTermStructure;

        /**
         * VIX-futures block. Note: {@link VixFutures#basis} is approximated
         * from {@link VixTermLevels#vix3m} vs VIX spot (NOT actual
         * front-month VIX futures prices) — useful as a proxy but
         * different from a real VX1 / VX2 contango readout.
         */
        @SerializedName("vix_futures") public VixFutures vixFutures;

        /** CNN-style fear-and-greed composite. */
        @SerializedName("fear_and_greed") public FearAndGreed fearAndGreed;
    }

    /** Standard quote: spot value with day-over-day change in absolute and percent. */
    public static final class Quote {
        @SerializedName("value") public Double value;
        @SerializedName("change") public Double change;
        @SerializedName("change_pct") public Double changePct;
    }

    /** VIX term structure block. */
    public static final class VixTermStructure {
        /** Sub-block of named tenor levels. */
        @SerializedName("levels") public VixTermLevels levels;
        /** Near-tenor slope: {@code (vix3m - vix) / vix * 100}. Positive = contango. */
        @SerializedName("near_slope_pct") public Double nearSlopePct;
        /** Plain-English structure label (e.g. {@code "contango"} / {@code "backwardation"}). */
        @SerializedName("structure") public String structure;
    }

    /** Named VIX term-structure level points. */
    public static final class VixTermLevels {
        /** VIX9D (9-day VIX). */
        @SerializedName("vix9d") public Double vix9d;
        /** VIX (30-day, the standard headline VIX index). */
        @SerializedName("vix")   public Double vix;
        /** VIX3M (3-month VIX). */
        @SerializedName("vix3m") public Double vix3m;
        /** VIX6M (6-month VIX). */
        @SerializedName("vix6m") public Double vix6m;
    }

    /**
     * VIX-futures snapshot.
     *
     * <p>{@link #basis} is approximated from VIX3M vs VIX spot — NOT a
     * real front-month futures price. Treat it as a directional proxy.
     */
    public static final class VixFutures {
        /** Front-month VIX futures price (proxied from VIX3M). */
        @SerializedName("front_month") public Double frontMonth;
        /** Spot VIX. */
        @SerializedName("spot") public Double spot;
        /** {@code front_month - spot}. */
        @SerializedName("spread") public Double spread;
        /** Basis as a percent of spot: {@code (front_month - spot) / spot * 100}. */
        @SerializedName("basis_pct") public Double basisPct;
        /** Plain-English structure label: {@code "contango"} or {@code "backwardation"}. */
        @SerializedName("basis") public String basis;
    }

    /** Fear-and-greed composite. */
    public static final class FearAndGreed {
        /** 0-100 score. */
        @SerializedName("score") public Integer score;
        /** Bucket label (e.g. {@code "Extreme Fear"}, {@code "Neutral"}, {@code "Extreme Greed"}). */
        @SerializedName("rating") public String rating;
    }
}
