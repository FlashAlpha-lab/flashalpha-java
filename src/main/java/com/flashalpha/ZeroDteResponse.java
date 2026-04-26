package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/exposure/zero-dte/{symbol}}.
 *
 * <p>Use {@link FlashAlphaClient#zeroDteTyped(String)} or
 * {@link FlashAlphaClient#zeroDteTyped(String, Double)} to obtain a populated
 * instance. The original {@link FlashAlphaClient#zeroDte(String)} method is
 * unchanged and continues to return {@link com.google.gson.JsonObject}.
 *
 * <p>On weekends/holidays or symbols with no 0DTE today, {@link #noZeroDte} is
 * {@code true} and most fields are {@code null} — only {@link #symbol},
 * {@link #asOf}, {@link #message}, and {@link #nextZeroDteExpiry} are populated.
 *
 * <p>All numeric fields are boxed wrappers ({@link Double}, {@link Long},
 * {@link Integer}) so that {@code null} can represent values the API could
 * not compute (insufficient data, market closed, etc.).
 */
public final class ZeroDteResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("expiration")
    public String expiration;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("market_open")
    public Boolean marketOpen;

    @SerializedName("time_to_close_hours")
    public Double timeToCloseHours;

    @SerializedName("time_to_close_pct")
    public Double timeToClosePct;

    @SerializedName("regime")
    public Regime regime;

    @SerializedName("exposures")
    public Exposures exposures;

    @SerializedName("expected_move")
    public ExpectedMove expectedMove;

    @SerializedName("pin_risk")
    public PinRisk pinRisk;

    @SerializedName("hedging")
    public Hedging hedging;

    @SerializedName("decay")
    public Decay decay;

    @SerializedName("vol_context")
    public VolContext volContext;

    @SerializedName("flow")
    public Flow flow;

    @SerializedName("levels")
    public Levels levels;

    @SerializedName("liquidity")
    public Liquidity liquidity;

    @SerializedName("metadata")
    public Metadata metadata;

    @SerializedName("strikes")
    public List<Strike> strikes;

    /** Optional — only present near close (&lt;5 min) when greeks may be unstable. */
    @SerializedName("warnings")
    public List<String> warnings;

    // ── No-0DTE fallback ───────────────────────────────────────────────────

    @SerializedName("no_zero_dte")
    public Boolean noZeroDte;

    @SerializedName("message")
    public String message;

    @SerializedName("next_zero_dte_expiry")
    public String nextZeroDteExpiry;

    // ── Nested types ───────────────────────────────────────────────────────

    public static final class Regime {
        @SerializedName("label") public String label;
        @SerializedName("description") public String description;
        @SerializedName("gamma_flip") public Double gammaFlip;
        @SerializedName("spot_vs_flip") public String spotVsFlip;
        @SerializedName("spot_to_flip_pct") public Double spotToFlipPct;
        @SerializedName("distance_to_flip_dollars") public Double distanceToFlipDollars;
        @SerializedName("distance_to_flip_sigmas") public Double distanceToFlipSigmas;
    }

    public static final class Exposures {
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        @SerializedName("pct_of_total_gex") public Double pctOfTotalGex;
        @SerializedName("total_chain_net_gex") public Double totalChainNetGex;
    }

    public static final class ExpectedMove {
        @SerializedName("implied_1sd_dollars") public Double implied1SdDollars;
        @SerializedName("implied_1sd_pct") public Double implied1SdPct;
        @SerializedName("remaining_1sd_dollars") public Double remaining1SdDollars;
        @SerializedName("remaining_1sd_pct") public Double remaining1SdPct;
        @SerializedName("upper_bound") public Double upperBound;
        @SerializedName("lower_bound") public Double lowerBound;
        @SerializedName("straddle_price") public Double straddlePrice;
        @SerializedName("atm_iv") public Double atmIv;
    }

    public static final class PinComponents {
        @SerializedName("oi_score") public Integer oiScore;
        @SerializedName("proximity_score") public Integer proximityScore;
        @SerializedName("time_score") public Integer timeScore;
        @SerializedName("gamma_score") public Integer gammaScore;
    }

    public static final class PinRisk {
        @SerializedName("magnet_strike") public Double magnetStrike;
        @SerializedName("magnet_gex") public Double magnetGex;
        @SerializedName("distance_to_magnet_pct") public Double distanceToMagnetPct;
        @SerializedName("pin_score") public Integer pinScore;
        @SerializedName("components") public PinComponents components;
        @SerializedName("max_pain") public Double maxPain;
        @SerializedName("oi_concentration_top3_pct") public Double oiConcentrationTop3Pct;
        @SerializedName("description") public String description;
    }

    public static final class HedgingBucket {
        @SerializedName("dealer_shares_to_trade") public Double dealerSharesToTrade;
        @SerializedName("direction") public String direction;
        @SerializedName("notional_usd") public Double notionalUsd;
    }

    public static final class Hedging {
        @SerializedName("spot_up_10bp") public HedgingBucket spotUp10Bp;
        @SerializedName("spot_down_10bp") public HedgingBucket spotDown10Bp;
        @SerializedName("spot_up_25bp") public HedgingBucket spotUp25Bp;
        @SerializedName("spot_down_25bp") public HedgingBucket spotDown25Bp;
        @SerializedName("spot_up_half_pct") public HedgingBucket spotUpHalfPct;
        @SerializedName("spot_down_half_pct") public HedgingBucket spotDownHalfPct;
        @SerializedName("spot_up_1pct") public HedgingBucket spotUp1Pct;
        @SerializedName("spot_down_1pct") public HedgingBucket spotDown1Pct;
        @SerializedName("convexity_at_spot") public Double convexityAtSpot;
    }

    public static final class Decay {
        @SerializedName("net_theta_dollars") public Double netThetaDollars;
        @SerializedName("theta_per_hour_remaining") public Double thetaPerHourRemaining;
        @SerializedName("charm_regime") public String charmRegime;
        @SerializedName("charm_description") public String charmDescription;
        @SerializedName("gamma_acceleration") public Double gammaAcceleration;
        @SerializedName("description") public String description;
    }

    public static final class VolContext {
        @SerializedName("zero_dte_atm_iv") public Double zeroDteAtmIv;
        @SerializedName("seven_dte_atm_iv") public Double sevenDteAtmIv;
        @SerializedName("iv_ratio_0dte_7dte") public Double ivRatio0Dte7Dte;
        @SerializedName("vix") public Double vix;
        @SerializedName("vanna_exposure") public Double vannaExposure;
        @SerializedName("vanna_interpretation") public String vannaInterpretation;
        @SerializedName("description") public String description;
    }

    public static final class Flow {
        @SerializedName("total_volume") public Long totalVolume;
        @SerializedName("call_volume") public Long callVolume;
        @SerializedName("put_volume") public Long putVolume;
        @SerializedName("net_call_minus_put_volume") public Long netCallMinusPutVolume;
        @SerializedName("total_oi") public Long totalOi;
        @SerializedName("call_oi") public Long callOi;
        @SerializedName("put_oi") public Long putOi;
        @SerializedName("pc_ratio_volume") public Double pcRatioVolume;
        @SerializedName("pc_ratio_oi") public Double pcRatioOi;
        @SerializedName("volume_to_oi_ratio") public Double volumeToOiRatio;
        @SerializedName("atm_volume_share_pct") public Double atmVolumeSharePct;
        @SerializedName("top3_strike_volume_pct") public Double top3StrikeVolumePct;
    }

    public static final class Levels {
        @SerializedName("call_wall") public Double callWall;
        @SerializedName("call_wall_gex") public Double callWallGex;
        @SerializedName("call_wall_strength") public Double callWallStrength;
        @SerializedName("distance_to_call_wall_pct") public Double distanceToCallWallPct;
        @SerializedName("put_wall") public Double putWall;
        @SerializedName("put_wall_gex") public Double putWallGex;
        @SerializedName("put_wall_strength") public Double putWallStrength;
        @SerializedName("distance_to_put_wall_pct") public Double distanceToPutWallPct;
        @SerializedName("distance_to_magnet_dollars") public Double distanceToMagnetDollars;
        @SerializedName("highest_oi_strike") public Double highestOiStrike;
        @SerializedName("highest_oi_total") public Long highestOiTotal;
        @SerializedName("max_positive_gamma") public Double maxPositiveGamma;
        @SerializedName("max_negative_gamma") public Double maxNegativeGamma;
        @SerializedName("level_cluster_score") public Integer levelClusterScore;
    }

    public static final class Liquidity {
        @SerializedName("atm_spread_pct") public Double atmSpreadPct;
        @SerializedName("weighted_spread_pct") public Double weightedSpreadPct;
        @SerializedName("execution_score") public Integer executionScore;
    }

    public static final class Metadata {
        @SerializedName("snapshot_age_seconds") public Double snapshotAgeSeconds;
        @SerializedName("chain_contract_count") public Integer chainContractCount;
        @SerializedName("data_quality_score") public Integer dataQualityScore;
        @SerializedName("greek_smoothness_score") public Integer greekSmoothnessScore;
    }

    public static final class Strike {
        @SerializedName("strike") public Double strike;
        @SerializedName("distance_from_spot_pct") public Double distanceFromSpotPct;
        @SerializedName("call_symbol") public String callSymbol;
        @SerializedName("put_symbol") public String putSymbol;
        @SerializedName("call_gex") public Double callGex;
        @SerializedName("put_gex") public Double putGex;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("call_dex") public Double callDex;
        @SerializedName("put_dex") public Double putDex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        @SerializedName("call_oi") public Long callOi;
        @SerializedName("put_oi") public Long putOi;
        @SerializedName("call_volume") public Long callVolume;
        @SerializedName("put_volume") public Long putVolume;
        @SerializedName("gex_share_pct") public Double gexSharePct;
        @SerializedName("oi_share_pct") public Double oiSharePct;
        @SerializedName("volume_share_pct") public Double volumeSharePct;
        @SerializedName("call_iv") public Double callIv;
        @SerializedName("put_iv") public Double putIv;
        @SerializedName("call_delta") public Double callDelta;
        @SerializedName("put_delta") public Double putDelta;
        @SerializedName("call_gamma") public Double callGamma;
        @SerializedName("put_gamma") public Double putGamma;
        @SerializedName("call_theta") public Double callTheta;
        @SerializedName("put_theta") public Double putTheta;
        @SerializedName("call_mid") public Double callMid;
        @SerializedName("put_mid") public Double putMid;
        @SerializedName("call_spread_pct") public Double callSpreadPct;
        @SerializedName("put_spread_pct") public Double putSpreadPct;
    }
}
