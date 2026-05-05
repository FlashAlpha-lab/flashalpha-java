package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/vrp/{symbol}} (Alpha+).
 *
 * <p>The single most-misread response shape in the FlashAlpha API. Every
 * nested block exists for a reason — core metrics, directional skew, gamma
 * conditioning, vanna conditioning, regime, strategy scores, and macro
 * context are deliberately separated.
 *
 * <p><b>Common silent-null traps</b> (now type-checked at the SDK boundary):
 * <ul>
 *   <li>{@code response.zScore} ✗ → use {@code response.vrp.zScore}</li>
 *   <li>{@code response.percentile} ✗ → use {@code response.vrp.percentile}</li>
 *   <li>{@code response.atmIv} ✗ → use {@code response.vrp.atmIv}</li>
 *   <li>{@code response.putVrp} ✗ → use {@code response.directional.downsideVrp}</li>
 *   <li>{@code response.netGex} ✗ → use {@code response.regime.netGex}</li>
 *   <li>{@code response.harvestScore} (top-level) ✗ → use
 *     {@code response.gexConditioned.harvestScore};
 *     {@code response.netHarvestScore} is a SEPARATE composite.</li>
 * </ul>
 *
 * <p>Returns 403 {@code tier_restricted} for anything below Alpha plan.
 *
 * <p>All numeric fields are boxed wrappers ({@link Double}, {@link Integer})
 * so {@code null} can represent values the API could not compute.
 */
public final class VrpResponse {

    /** Echoed from the request path (e.g. "SPY"). */
    @SerializedName("symbol")
    public String symbol;

    /** Spot mid at {@link #asOf}. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** ET wall-clock timestamp this snapshot was computed for. */
    @SerializedName("as_of")
    public String asOf;

    /** True if NYSE was open at {@link #asOf}. */
    @SerializedName("market_open")
    public Boolean marketOpen;

    /** Core VRP metrics block. See {@link VrpCore}. */
    @SerializedName("vrp")
    public VrpCore vrp;

    /** {@code vrp_20d / 100} as a decimal. Same as {@code vrp.vrp20d / 100}. */
    @SerializedName("variance_risk_premium")
    public Double varianceRiskPremium;

    /** {@code fair_vol - atm_iv}. Curvature premium between IV smile and var-swap fair vol. */
    @SerializedName("convexity_premium")
    public Double convexityPremium;

    /** Variance-swap fair vol (annualised %). */
    @SerializedName("fair_vol")
    public Double fairVol;

    /** Directional VRP skew (downside vs upside). See {@link VrpDirectional}. */
    @SerializedName("directional")
    public VrpDirectional directional;

    /** Term structure — list of {@link VrpTermItem}. Empty when surface fitting fails. */
    @SerializedName("term_vrp")
    public List<VrpTermItem> termVrp;

    /** GEX-conditioned harvest score. See {@link VrpGexConditioned}. */
    @SerializedName("gex_conditioned")
    public VrpGexConditioned gexConditioned;

    /** Vanna-conditioned outlook. See {@link VrpVannaConditioned}. */
    @SerializedName("vanna_conditioned")
    public VrpVannaConditioned vannaConditioned;

    /** Regime snapshot block. {@code netGex} lives HERE, not at the top level. */
    @SerializedName("regime")
    public VrpRegime regime;

    /** 0-100 strategy suitability scores. {@code null} on historical when warmup is short. */
    @SerializedName("strategy_scores")
    public VrpStrategyScores strategyScores;

    /** 0-100 composite harvest signal. {@code null} on historical when warmup is short. */
    @SerializedName("net_harvest_score")
    public Integer netHarvestScore;

    /** 0-100 — risk that dealer hedging flow disrupts a short-vol harvest. */
    @SerializedName("dealer_flow_risk")
    public Integer dealerFlowRisk;

    /** Server-side warnings about data quality. Always present (possibly empty). */
    @SerializedName("warnings")
    public List<String> warnings;

    /** Macro context. See {@link VrpMacro}. */
    @SerializedName("macro")
    public VrpMacro macro;

    /**
     * Core VRP metrics block — the heart of the response.
     *
     * <p>The variance risk premium is the spread between IMPLIED volatility
     * (forward-looking, priced into options) and REALIZED volatility
     * (backward-looking, observed from spot returns). Positive VRP =
     * options pricing more vol than the underlying actually moved →
     * premium for selling vol.
     *
     * <p>Nested under {@code response.vrp} — NOT top-level.
     */
    public static final class VrpCore {
        /** At-the-money implied volatility (annualised %, e.g. 18.5 = 18.5%). */
        @SerializedName("atm_iv") public Double atmIv;
        /** Realized vol over trailing 5 trading days (annualised %). */
        @SerializedName("rv_5d") public Double rv5d;
        @SerializedName("rv_10d") public Double rv10d;
        @SerializedName("rv_20d") public Double rv20d;
        @SerializedName("rv_30d") public Double rv30d;
        /** Variance risk premium at this horizon: {@code atm_iv - rv_Nd}. */
        @SerializedName("vrp_5d") public Double vrp5d;
        @SerializedName("vrp_10d") public Double vrp10d;
        @SerializedName("vrp_20d") public Double vrp20d;
        @SerializedName("vrp_30d") public Double vrp30d;
        /** Z-score of current 20-day VRP. {@code null} when warmup is insufficient. */
        @SerializedName("z_score") public Double zScore;
        /** Percentile rank (0-100). {@code null} when warmup is short. */
        @SerializedName("percentile") public Integer percentile;
        @SerializedName("history_days") public Integer historyDays;
    }

    /**
     * Directional VRP skew. Use {@code downsideVrp} / {@code upsideVrp},
     * NOT {@code putVrp} / {@code callVrp} (those don't exist).
     */
    public static final class VrpDirectional {
        @SerializedName("put_wing_iv_25d") public Double putWingIv25d;
        @SerializedName("call_wing_iv_25d") public Double callWingIv25d;
        @SerializedName("downside_rv_20d") public Double downsideRv20d;
        @SerializedName("upside_rv_20d") public Double upsideRv20d;
        /** {@code put_wing_iv_25d - downside_rv_20d}. Positive = crash insurance rich. */
        @SerializedName("downside_vrp") public Double downsideVrp;
        /** {@code call_wing_iv_25d - upside_rv_20d}. Positive = upside calls rich. */
        @SerializedName("upside_vrp") public Double upsideVrp;
    }

    /** One row of the VRP term structure. */
    public static final class VrpTermItem {
        /** Days to expiry (e.g. 7, 14, 30, 60, 90). */
        @SerializedName("dte") public Integer dte;
        @SerializedName("iv") public Double iv;
        @SerializedName("rv") public Double rv;
        /** Tenor-matched VRP: {@code iv - rv}. */
        @SerializedName("vrp") public Double vrp;
    }

    /** VRP harvest score conditioned on the prevailing dealer-gamma regime. */
    public static final class VrpGexConditioned {
        @SerializedName("regime") public String regime;
        /** 0-100 composite. >70 = strong harvest signal; &lt;30 = avoid. */
        @SerializedName("harvest_score") public Double harvestScore;
        @SerializedName("interpretation") public String interpretation;
    }

    /** VRP outlook conditioned on net dealer vanna exposure. */
    public static final class VrpVannaConditioned {
        @SerializedName("outlook") public String outlook;
        @SerializedName("interpretation") public String interpretation;
    }

    /**
     * Regime snapshot block.
     *
     * <p>{@code netGex} lives HERE, not at the top level.
     */
    public static final class VrpRegime {
        @SerializedName("gamma") public String gamma;
        /** {@code "harvestable"} | {@code "selling_too_cheap"} | etc. {@code null} on historical with insufficient warmup. */
        @SerializedName("vrp_regime") public String vrpRegime;
        /** Net dealer gamma exposure in dollars per 1% spot move. */
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("gamma_flip") public Double gammaFlip;
    }

    /** 0-100 suitability scores for canonical short-vol strategies. Each field can be {@code null} when inputs aren't computable. */
    public static final class VrpStrategyScores {
        @SerializedName("short_put_spread") public Integer shortPutSpread;
        @SerializedName("short_strangle") public Integer shortStrangle;
        @SerializedName("iron_condor") public Integer ironCondor;
        @SerializedName("calendar_spread") public Integer calendarSpread;
    }

    /**
     * Macro-context snapshot used to condition the VRP outlook.
     *
     * <p>Note diffs across live vs historical:
     * <ul>
     *   <li>{@code hySpread}: live = {@code null}; historical = float.</li>
     *   <li>{@code fedFunds}: live = float; historical = field absent
     *     (deserializes as {@code null} on historical responses).</li>
     * </ul>
     */
    public static final class VrpMacro {
        /** CBOE VIX index level. */
        @SerializedName("vix") public Double vix;
        /** CBOE VIX3M (3-month VIX). */
        @SerializedName("vix_3m") public Double vix3m;
        /** {@code (vix_3m - vix) / vix * 100} — positive = contango. */
        @SerializedName("vix_term_slope") public Double vixTermSlope;
        /** 10-year US Treasury yield (%, FRED {@code DGS10}). */
        @SerializedName("dgs10") public Double dgs10;
        /** ICE BofA US HY OAS. Live currently {@code null}; historical populated. */
        @SerializedName("hy_spread") public Double hySpread;
        /** Fed Funds effective rate (%, FRED {@code DFF}). Live-only — absent on historical. */
        @SerializedName("fed_funds") public Double fedFunds;
    }
}
