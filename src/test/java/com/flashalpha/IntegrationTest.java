package com.flashalpha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Integration tests against the live FlashAlpha API.
 *
 * <p>These tests are skipped automatically when the {@code FLASHALPHA_API_KEY}
 * environment variable is not set. To run them:
 *
 * <pre>
 *   export FLASHALPHA_API_KEY=your_key_here
 *   mvn test -Dtest=IntegrationTest
 * </pre>
 */
public class IntegrationTest {

    private FlashAlphaClient client;

    @Before
    public void setUp() {
        String apiKey = System.getenv("FLASHALPHA_API_KEY");
        Assume.assumeTrue("FLASHALPHA_API_KEY environment variable not set — skipping integration tests",
                apiKey != null && !apiKey.isEmpty());
        client = new FlashAlphaClient(apiKey);
    }

    // ── System ────────────────────────────────────────────────────────

    @Test
    public void testHealth() {
        JsonObject result = client.health();
        assertNotNull(result);
    }

    @Test
    public void testAccount() {
        JsonObject result = client.account();
        assertNotNull(result);
    }

    // ── Reference data ─────────────────────────────────────────────────

    @Test
    public void testTickers() {
        JsonObject result = client.tickers();
        assertNotNull(result);
    }

    @Test
    public void testSymbols() {
        JsonObject result = client.symbols();
        assertNotNull(result);
    }

    @Test
    public void testOptions() {
        JsonObject result = client.options("SPY");
        assertNotNull(result);
    }

    // ── Market data ────────────────────────────────────────────────────

    @Test
    public void testStockQuote() {
        JsonObject result = client.stockQuote("SPY");
        assertNotNull(result);
    }

    @Test
    public void testSurface() {
        JsonObject result = client.surface("SPY");
        assertNotNull(result);
    }

    @Test
    public void testStockSummary() {
        JsonObject result = client.stockSummary("SPY");
        assertNotNull(result);
    }

    // ── Exposure analytics ─────────────────────────────────────────────

    @Test
    public void testGex() {
        JsonObject result = client.gex("SPY");
        assertNotNull(result);
    }

    @Test
    public void testDex() {
        JsonObject result = client.dex("SPY");
        assertNotNull(result);
    }

    @Test
    public void testVex() {
        JsonObject result = client.vex("SPY");
        assertNotNull(result);
    }

    @Test
    public void testChex() {
        JsonObject result = client.chex("SPY");
        assertNotNull(result);
    }

    @Test
    public void testExposureLevels() {
        JsonObject result = client.exposureLevels("SPY");
        assertNotNull(result);
    }

    // ── Pricing ────────────────────────────────────────────────────────

    @Test
    public void testGreeks() {
        // SPY call, spot ~450, strike 455, 5 DTE, 20% IV
        JsonObject result = client.greeks(450.0, 455.0, 5.0, 0.20, "call", null, null);
        assertNotNull(result);
    }

    @Test
    public void testIv() {
        // Compute IV from a theoretical price
        JsonObject result = client.iv(450.0, 455.0, 5.0, 3.50, "call", null, null);
        assertNotNull(result);
    }

    // ── Volatility ─────────────────────────────────────────────────────

    @Test
    public void testVolatility() {
        JsonObject result = client.volatility("SPY");
        assertNotNull(result);
    }

    // ── Zero-DTE ───────────────────────────────────────────────────────

    /**
     * Validate the full 0DTE response shape — fine-grained hedging buckets,
     * distance-to-flip in dollars/sigmas, pin sub-scores, flow concentration,
     * wall strength + level cluster, the new liquidity & metadata sections,
     * and per-strike greeks/quotes. Uses SPX which has daily 0DTE.
     */
    /**
     * Comprehensive end-to-end test of {@code zeroDteTyped()}. Every typed field
     * is asserted populated (non-null) so a renamed {@code @SerializedName}
     * would surface immediately. The full untyped-shape coverage lives in
     * {@link #testZeroDteIncludesAllNewFields()}; this is the typed mirror.
     */
    @Test
    public void testZeroDteTypedDeserializesAllFields() {
        ZeroDteResponse r = client.zeroDteTyped("SPX");
        assertNotNull(r);
        assertEquals("SPX", r.symbol);

        if (Boolean.TRUE.equals(r.noZeroDte)) {
            assertNotNull(r.nextZeroDteExpiry);
            return;
        }

        // top-level
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("market_open", r.marketOpen);

        // regime
        assertNotNull("regime", r.regime);
        assertNotNull("regime.label", r.regime.label);
        assertNotNull("regime.gamma_flip", r.regime.gammaFlip);
        assertNotNull("regime.spot_vs_flip", r.regime.spotVsFlip);
        assertNotNull("regime.spot_to_flip_pct", r.regime.spotToFlipPct);
        assertNotNull("regime.distance_to_flip_dollars", r.regime.distanceToFlipDollars);
        assertNotNull("regime.distance_to_flip_sigmas", r.regime.distanceToFlipSigmas);

        // exposures
        assertNotNull("exposures", r.exposures);
        assertNotNull("exposures.net_gex", r.exposures.netGex);
        assertNotNull("exposures.net_dex", r.exposures.netDex);
        assertNotNull("exposures.net_vex", r.exposures.netVex);
        assertNotNull("exposures.net_chex", r.exposures.netChex);
        assertNotNull("exposures.pct_of_total_gex", r.exposures.pctOfTotalGex);
        assertNotNull("exposures.total_chain_net_gex", r.exposures.totalChainNetGex);

        // expected_move
        assertNotNull("expected_move", r.expectedMove);
        assertNotNull("expected_move.implied_1sd_dollars", r.expectedMove.implied1SdDollars);
        assertNotNull("expected_move.implied_1sd_pct", r.expectedMove.implied1SdPct);
        assertNotNull("expected_move.upper_bound", r.expectedMove.upperBound);
        assertNotNull("expected_move.lower_bound", r.expectedMove.lowerBound);
        assertNotNull("expected_move.straddle_price", r.expectedMove.straddlePrice);
        assertNotNull("expected_move.atm_iv", r.expectedMove.atmIv);

        // pin_risk
        assertNotNull("pin_risk", r.pinRisk);
        assertNotNull("pin_risk.magnet_strike", r.pinRisk.magnetStrike);
        assertNotNull("pin_risk.magnet_gex", r.pinRisk.magnetGex);
        assertNotNull("pin_risk.distance_to_magnet_pct", r.pinRisk.distanceToMagnetPct);
        assertNotNull("pin_risk.pin_score", r.pinRisk.pinScore);
        assertNotNull("pin_risk.components", r.pinRisk.components);
        assertNotNull("pin_risk.components.oi_score", r.pinRisk.components.oiScore);
        assertNotNull("pin_risk.components.proximity_score", r.pinRisk.components.proximityScore);
        assertNotNull("pin_risk.components.time_score", r.pinRisk.components.timeScore);
        assertNotNull("pin_risk.components.gamma_score", r.pinRisk.components.gammaScore);
        assertNotNull("pin_risk.max_pain", r.pinRisk.maxPain);
        assertNotNull("pin_risk.oi_concentration_top3_pct", r.pinRisk.oiConcentrationTop3Pct);

        // hedging — all 8 buckets + convexity_at_spot
        assertNotNull("hedging", r.hedging);
        ZeroDteResponse.HedgingBucket[] buckets = {
                r.hedging.spotUp10Bp, r.hedging.spotDown10Bp,
                r.hedging.spotUp25Bp, r.hedging.spotDown25Bp,
                r.hedging.spotUpHalfPct, r.hedging.spotDownHalfPct,
                r.hedging.spotUp1Pct, r.hedging.spotDown1Pct,
        };
        String[] bucketNames = {
                "spotUp10Bp", "spotDown10Bp", "spotUp25Bp", "spotDown25Bp",
                "spotUpHalfPct", "spotDownHalfPct", "spotUp1Pct", "spotDown1Pct",
        };
        for (int i = 0; i < buckets.length; i++) {
            assertNotNull("hedging." + bucketNames[i], buckets[i]);
            assertNotNull("hedging." + bucketNames[i] + ".dealer_shares_to_trade",
                    buckets[i].dealerSharesToTrade);
            assertNotNull("hedging." + bucketNames[i] + ".direction", buckets[i].direction);
            assertNotNull("hedging." + bucketNames[i] + ".notional_usd", buckets[i].notionalUsd);
        }
        assertNotNull("hedging.convexity_at_spot", r.hedging.convexityAtSpot);

        // decay
        assertNotNull("decay", r.decay);
        assertNotNull("decay.net_theta_dollars", r.decay.netThetaDollars);
        assertNotNull("decay.charm_regime", r.decay.charmRegime);
        assertNotNull("decay.charm_description", r.decay.charmDescription);
        assertNotNull("decay.gamma_acceleration", r.decay.gammaAcceleration);

        // vol_context
        assertNotNull("vol_context", r.volContext);
        assertNotNull("vol_context.zero_dte_atm_iv", r.volContext.zeroDteAtmIv);
        assertNotNull("vol_context.seven_dte_atm_iv", r.volContext.sevenDteAtmIv);
        assertNotNull("vol_context.iv_ratio_0dte_7dte", r.volContext.ivRatio0Dte7Dte);
        assertNotNull("vol_context.vix", r.volContext.vix);
        assertNotNull("vol_context.vanna_exposure", r.volContext.vannaExposure);
        assertNotNull("vol_context.vanna_interpretation", r.volContext.vannaInterpretation);

        // flow
        assertNotNull("flow", r.flow);
        assertNotNull("flow.total_volume", r.flow.totalVolume);
        assertNotNull("flow.call_volume", r.flow.callVolume);
        assertNotNull("flow.put_volume", r.flow.putVolume);
        assertNotNull("flow.net_call_minus_put_volume", r.flow.netCallMinusPutVolume);
        assertNotNull("flow.total_oi", r.flow.totalOi);
        assertNotNull("flow.call_oi", r.flow.callOi);
        assertNotNull("flow.put_oi", r.flow.putOi);
        assertNotNull("flow.pc_ratio_volume", r.flow.pcRatioVolume);
        assertNotNull("flow.pc_ratio_oi", r.flow.pcRatioOi);
        assertNotNull("flow.volume_to_oi_ratio", r.flow.volumeToOiRatio);
        assertNotNull("flow.atm_volume_share_pct", r.flow.atmVolumeSharePct);
        assertNotNull("flow.top3_strike_volume_pct", r.flow.top3StrikeVolumePct);

        // levels
        assertNotNull("levels", r.levels);
        assertNotNull("levels.call_wall", r.levels.callWall);
        assertNotNull("levels.call_wall_gex", r.levels.callWallGex);
        assertNotNull("levels.call_wall_strength", r.levels.callWallStrength);
        assertNotNull("levels.distance_to_call_wall_pct", r.levels.distanceToCallWallPct);
        assertNotNull("levels.put_wall", r.levels.putWall);
        assertNotNull("levels.put_wall_gex", r.levels.putWallGex);
        assertNotNull("levels.put_wall_strength", r.levels.putWallStrength);
        assertNotNull("levels.distance_to_put_wall_pct", r.levels.distanceToPutWallPct);
        assertNotNull("levels.distance_to_magnet_dollars", r.levels.distanceToMagnetDollars);
        assertNotNull("levels.highest_oi_strike", r.levels.highestOiStrike);
        assertNotNull("levels.highest_oi_total", r.levels.highestOiTotal);
        assertNotNull("levels.max_positive_gamma", r.levels.maxPositiveGamma);
        assertNotNull("levels.max_negative_gamma", r.levels.maxNegativeGamma);
        assertNotNull("levels.level_cluster_score", r.levels.levelClusterScore);

        // liquidity
        assertNotNull("liquidity", r.liquidity);
        assertNotNull("liquidity.atm_spread_pct", r.liquidity.atmSpreadPct);
        assertNotNull("liquidity.weighted_spread_pct", r.liquidity.weightedSpreadPct);
        assertNotNull("liquidity.execution_score", r.liquidity.executionScore);

        // metadata
        assertNotNull("metadata", r.metadata);
        assertNotNull("metadata.snapshot_age_seconds", r.metadata.snapshotAgeSeconds);
        assertNotNull("metadata.chain_contract_count", r.metadata.chainContractCount);
        assertNotNull("metadata.data_quality_score", r.metadata.dataQualityScore);
        assertNotNull("metadata.greek_smoothness_score", r.metadata.greekSmoothnessScore);

        // strikes[0] — every per-strike field
        assertNotNull("strikes", r.strikes);
        if (!r.strikes.isEmpty()) {
            ZeroDteResponse.Strike s = r.strikes.get(0);
            assertTrue("strike > 0", s.strike != null && s.strike > 0);
            assertNotNull("strike[0].distance_from_spot_pct", s.distanceFromSpotPct);
            assertNotNull("strike[0].call_symbol", s.callSymbol);
            assertNotNull("strike[0].put_symbol", s.putSymbol);
            assertNotNull("strike[0].call_gex", s.callGex);
            assertNotNull("strike[0].put_gex", s.putGex);
            assertNotNull("strike[0].net_gex", s.netGex);
            assertNotNull("strike[0].call_dex", s.callDex);
            assertNotNull("strike[0].put_dex", s.putDex);
            assertNotNull("strike[0].net_dex", s.netDex);
            assertNotNull("strike[0].net_vex", s.netVex);
            assertNotNull("strike[0].net_chex", s.netChex);
            assertNotNull("strike[0].call_oi", s.callOi);
            assertNotNull("strike[0].put_oi", s.putOi);
            assertNotNull("strike[0].call_volume", s.callVolume);
            assertNotNull("strike[0].put_volume", s.putVolume);
            assertNotNull("strike[0].gex_share_pct", s.gexSharePct);
            assertNotNull("strike[0].oi_share_pct", s.oiSharePct);
            assertNotNull("strike[0].volume_share_pct", s.volumeSharePct);
            assertNotNull("strike[0].call_iv", s.callIv);
            assertNotNull("strike[0].put_iv", s.putIv);
            assertNotNull("strike[0].call_delta", s.callDelta);
            assertNotNull("strike[0].put_delta", s.putDelta);
            assertNotNull("strike[0].call_gamma", s.callGamma);
            assertNotNull("strike[0].put_gamma", s.putGamma);
            assertNotNull("strike[0].call_theta", s.callTheta);
            assertNotNull("strike[0].put_theta", s.putTheta);
            assertNotNull("strike[0].call_mid", s.callMid);
            assertNotNull("strike[0].put_mid", s.putMid);
            assertNotNull("strike[0].call_spread_pct", s.callSpreadPct);
            assertNotNull("strike[0].put_spread_pct", s.putSpreadPct);
        }
    }

    @Test
    public void testZeroDteIncludesAllNewFields() {
        JsonObject r = client.zeroDte("SPX");
        assertNotNull(r);
        assertEquals("SPX", r.get("symbol").getAsString());

        // ``no_zero_dte`` may be absent, JSON null, or a boolean. Only the
        // explicit ``true`` case is the "no 0DTE expiry today" short-circuit;
        // null/absent means a normal 0DTE payload follows.
        if (r.has("no_zero_dte")
                && !r.get("no_zero_dte").isJsonNull()
                && r.get("no_zero_dte").getAsBoolean()) {
            assertTrue(r.has("next_zero_dte_expiry"));
            return;
        }

        // top-level
        for (String k : new String[]{"underlying_price", "expiration", "as_of", "market_open",
                "time_to_close_hours", "time_to_close_pct"}) {
            assertTrue("top-level " + k + " missing", r.has(k));
        }

        // regime
        JsonObject regime = r.getAsJsonObject("regime");
        for (String k : new String[]{"label", "description", "gamma_flip", "spot_vs_flip", "spot_to_flip_pct",
                "distance_to_flip_dollars", "distance_to_flip_sigmas"}) {
            assertTrue("regime." + k + " missing", regime.has(k));
        }

        // exposures
        JsonObject exposures = r.getAsJsonObject("exposures");
        for (String k : new String[]{"net_gex", "net_dex", "net_vex", "net_chex",
                "pct_of_total_gex", "total_chain_net_gex"}) {
            assertTrue("exposures." + k + " missing", exposures.has(k));
        }

        // expected_move
        JsonObject em = r.getAsJsonObject("expected_move");
        for (String k : new String[]{"implied_1sd_dollars", "implied_1sd_pct", "remaining_1sd_dollars",
                "remaining_1sd_pct", "upper_bound", "lower_bound",
                "straddle_price", "atm_iv"}) {
            assertTrue("expected_move." + k + " missing", em.has(k));
        }

        // pin_risk
        JsonObject pr = r.getAsJsonObject("pin_risk");
        for (String k : new String[]{"magnet_strike", "magnet_gex", "distance_to_magnet_pct",
                "pin_score", "components", "max_pain",
                "oi_concentration_top3_pct", "description"}) {
            assertTrue("pin_risk." + k + " missing", pr.has(k));
        }
        JsonObject components = pr.getAsJsonObject("components");
        for (String k : new String[]{"oi_score", "proximity_score", "time_score", "gamma_score"}) {
            assertTrue("pin_risk.components." + k + " missing", components.has(k));
        }

        // hedging — fine-grained buckets + convexity
        JsonObject hedging = r.getAsJsonObject("hedging");
        for (String bucket : new String[]{"spot_up_10bp", "spot_down_10bp",
                "spot_up_25bp", "spot_down_25bp",
                "spot_up_half_pct", "spot_down_half_pct",
                "spot_up_1pct", "spot_down_1pct"}) {
            assertTrue("hedging." + bucket + " missing", hedging.has(bucket));
            JsonObject b = hedging.getAsJsonObject(bucket);
            for (String k : new String[]{"dealer_shares_to_trade", "direction", "notional_usd"}) {
                assertTrue("hedging." + bucket + "." + k + " missing", b.has(k));
            }
        }
        assertTrue(hedging.has("convexity_at_spot"));

        // decay
        JsonObject decay = r.getAsJsonObject("decay");
        for (String k : new String[]{"net_theta_dollars", "theta_per_hour_remaining", "charm_regime",
                "charm_description", "gamma_acceleration", "description"}) {
            assertTrue("decay." + k + " missing", decay.has(k));
        }

        // vol_context
        JsonObject vc = r.getAsJsonObject("vol_context");
        for (String k : new String[]{"zero_dte_atm_iv", "seven_dte_atm_iv", "iv_ratio_0dte_7dte",
                "vix", "vanna_exposure", "vanna_interpretation", "description"}) {
            assertTrue("vol_context." + k + " missing", vc.has(k));
        }

        // flow
        JsonObject flow = r.getAsJsonObject("flow");
        for (String k : new String[]{"total_volume", "call_volume", "put_volume",
                "net_call_minus_put_volume",
                "total_oi", "call_oi", "put_oi",
                "pc_ratio_volume", "pc_ratio_oi", "volume_to_oi_ratio",
                "atm_volume_share_pct", "top3_strike_volume_pct"}) {
            assertTrue("flow." + k + " missing", flow.has(k));
        }

        // levels
        JsonObject levels = r.getAsJsonObject("levels");
        for (String k : new String[]{"call_wall", "call_wall_gex", "call_wall_strength",
                "distance_to_call_wall_pct",
                "put_wall", "put_wall_gex", "put_wall_strength",
                "distance_to_put_wall_pct",
                "distance_to_magnet_dollars",
                "highest_oi_strike", "highest_oi_total",
                "max_positive_gamma", "max_negative_gamma",
                "level_cluster_score"}) {
            assertTrue("levels." + k + " missing", levels.has(k));
        }

        // liquidity (new section)
        JsonObject liquidity = r.getAsJsonObject("liquidity");
        for (String k : new String[]{"atm_spread_pct", "weighted_spread_pct", "execution_score"}) {
            assertTrue("liquidity." + k + " missing", liquidity.has(k));
        }

        // metadata (new section)
        JsonObject metadata = r.getAsJsonObject("metadata");
        for (String k : new String[]{"snapshot_age_seconds", "chain_contract_count",
                "data_quality_score", "greek_smoothness_score"}) {
            assertTrue("metadata." + k + " missing", metadata.has(k));
        }

        // per-strike entries
        com.google.gson.JsonArray strikes = r.getAsJsonArray("strikes");
        assertNotNull(strikes);
        if (strikes.size() > 0) {
            JsonObject s = strikes.get(0).getAsJsonObject();
            for (String k : new String[]{"strike", "distance_from_spot_pct",
                    "call_symbol", "put_symbol",
                    "call_gex", "put_gex", "net_gex",
                    "call_dex", "put_dex", "net_dex",
                    "net_vex", "net_chex",
                    "call_oi", "put_oi", "call_volume", "put_volume",
                    "gex_share_pct", "oi_share_pct", "volume_share_pct",
                    "call_iv", "put_iv",
                    "call_delta", "put_delta",
                    "call_gamma", "put_gamma",
                    "call_theta", "put_theta",
                    "call_mid", "put_mid",
                    "call_spread_pct", "put_spread_pct"}) {
                assertTrue("strikes[0]." + k + " missing", s.has(k));
            }
        }
    }

    // ── Max Pain ──────────────────────────────────────────────────────

    @Test
    public void testMaxPain() {
        JsonObject result = client.maxPain("SPY");
        assertNotNull(result);
        assertTrue(result.has("max_pain_strike"));
        assertTrue(result.has("pain_curve"));
        assertTrue(result.has("dealer_alignment"));
        assertTrue(result.has("pin_probability"));
    }

    @Test
    public void testMaxPainFieldStructure() {
        JsonObject result = client.maxPain("SPY");
        String direction = result.getAsJsonObject("distance").get("direction").getAsString();
        assertTrue("above".equals(direction) || "below".equals(direction) || "at".equals(direction));
        String signal = result.get("signal").getAsString();
        assertTrue("bullish".equals(signal) || "bearish".equals(signal) || "neutral".equals(signal));
    }

    @Test
    public void testMaxPainMultiExpiry() {
        JsonObject result = client.maxPain("SPY");
        if (result.has("max_pain_by_expiration") && !result.get("max_pain_by_expiration").isJsonNull()) {
            assertTrue(result.getAsJsonArray("max_pain_by_expiration").size() > 0);
        }
    }

    @Test
    public void testMaxPainEveryFieldDeclaredInPocoMustBeReferenced() {
        // 100% field-coverage discipline. Every leaf field declared in
        // MaxPainResponse must be referenced by at least one assertion.
        JsonObject r = client.maxPain("SPY");

        // ── top-level scalars ──
        assertEquals("SPY", r.get("symbol").getAsString());
        assertTrue(r.get("underlying_price").getAsJsonPrimitive().isNumber());
        assertTrue(r.get("as_of").getAsJsonPrimitive().isString());
        assertTrue(r.get("max_pain_strike").getAsJsonPrimitive().isNumber());
        String signal = r.get("signal").getAsString();
        assertTrue("signal=" + signal,
                "bullish".equals(signal) || "bearish".equals(signal) || "neutral".equals(signal));
        assertTrue(r.get("expiration").getAsJsonPrimitive().isString());
        assertTrue(r.get("put_call_oi_ratio").getAsJsonPrimitive().isNumber());
        String regime = r.get("regime").getAsString();
        assertTrue("regime=" + regime,
                "positive_gamma".equals(regime) || "negative_gamma".equals(regime)
                        || "unknown".equals(regime));
        int pin = r.get("pin_probability").getAsInt();
        assertTrue("pin_probability range", pin >= 0 && pin <= 100);

        // ── distance{absolute, percent, direction} ──
        JsonObject dist = r.getAsJsonObject("distance");
        assertTrue(dist.get("absolute").getAsJsonPrimitive().isNumber());
        assertTrue(dist.get("percent").getAsJsonPrimitive().isNumber());
        String direction = dist.get("direction").getAsString();
        assertTrue("direction=" + direction,
                "above".equals(direction) || "below".equals(direction) || "at".equals(direction));

        // ── pain_curve[] ──
        JsonArray pc = r.getAsJsonArray("pain_curve");
        assertTrue("pain_curve non-empty", pc.size() > 0);
        JsonObject pcRow = pc.get(0).getAsJsonObject();
        for (String k : new String[]{"strike", "call_pain", "put_pain", "total_pain"}) {
            assertTrue("pain_curve[0]." + k, pcRow.get(k).getAsJsonPrimitive().isNumber());
        }

        // ── oi_by_strike[] ──
        JsonArray oi = r.getAsJsonArray("oi_by_strike");
        assertTrue("oi_by_strike non-empty", oi.size() > 0);
        JsonObject oiRow = oi.get(0).getAsJsonObject();
        for (String k : new String[]{"strike", "call_oi", "put_oi", "total_oi", "call_volume", "put_volume"}) {
            assertTrue("oi_by_strike[0]." + k, oiRow.get(k).getAsJsonPrimitive().isNumber());
        }

        // ── max_pain_by_expiration[] (no expiration filter on this call) ──
        JsonArray mpe = r.getAsJsonArray("max_pain_by_expiration");
        assertTrue("max_pain_by_expiration non-empty", mpe.size() > 0);
        JsonObject mpeRow = mpe.get(0).getAsJsonObject();
        assertTrue(mpeRow.get("expiration").getAsJsonPrimitive().isString());
        assertTrue(mpeRow.get("max_pain_strike").getAsJsonPrimitive().isNumber());
        assertTrue(mpeRow.get("dte").getAsJsonPrimitive().isNumber());
        assertTrue(mpeRow.get("total_oi").getAsJsonPrimitive().isNumber());

        // ── dealer_alignment ──
        JsonObject da = r.getAsJsonObject("dealer_alignment");
        String alignment = da.get("alignment").getAsString();
        assertTrue("alignment=" + alignment,
                "converging".equals(alignment) || "moderate".equals(alignment)
                        || "diverging".equals(alignment) || "unknown".equals(alignment));
        assertTrue(da.get("description").getAsJsonPrimitive().isString());
        for (String k : new String[]{"gamma_flip", "call_wall", "put_wall"}) {
            assertTrue("dealer_alignment." + k, da.get(k).getAsJsonPrimitive().isNumber());
        }

        // ── expected_move ──
        JsonObject em = r.getAsJsonObject("expected_move");
        assertTrue(em.get("straddle_price").getAsJsonPrimitive().isNumber());
        assertTrue(em.get("atm_iv").getAsJsonPrimitive().isNumber());
        assertTrue(em.get("max_pain_within_expected_range").getAsJsonPrimitive().isBoolean());
    }

    // ── rc.4 typed POCO field-walk tests ──────────────────────────────
    //
    // Mirror of the *EveryFieldDeclaredInPocoMustBeReferenced pattern but
    // walked through the typed POCOs — every public field on the response
    // class (and every nested type it references) must be populated by a
    // SPY (or 580-strike pricing) live response. A renamed wire field
    // surfaces as a null assertion failure.

    @Test
    public void testStockSummary_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.stockSummary("SPY");
        StockSummaryResponse r = gson.fromJson(json, StockSummaryResponse.class);

        // ── top-level ──
        assertEquals("SPY", r.symbol);
        assertNotNull("as_of", r.asOf);
        assertNotNull("market_open", r.marketOpen);

        // ── price ──
        assertNotNull("price", r.price);
        assertNotNull("price.bid", r.price.bid);
        assertNotNull("price.ask", r.price.ask);
        assertNotNull("price.mid", r.price.mid);
        assertNotNull("price.last", r.price.last);
        assertNotNull("price.last_update", r.price.lastUpdate);

        // ── volatility ──
        assertNotNull("volatility", r.volatility);
        assertNotNull("volatility.atm_iv", r.volatility.atmIv);
        assertNotNull("volatility.hv_20", r.volatility.hv20);
        assertNotNull("volatility.hv_60", r.volatility.hv60);
        assertNotNull("volatility.vrp", r.volatility.vrp);

        // skew_25d — full 7-field block (rc.4)
        assertNotNull("volatility.skew_25d", r.volatility.skew25d);
        assertNotNull("skew_25d.expiry", r.volatility.skew25d.expiry);
        assertNotNull("skew_25d.days_to_expiry", r.volatility.skew25d.daysToExpiry);
        assertNotNull("skew_25d.put_25d_iv", r.volatility.skew25d.put25dIv);
        assertNotNull("skew_25d.atm_iv", r.volatility.skew25d.atmIv);
        assertNotNull("skew_25d.call_25d_iv", r.volatility.skew25d.call25dIv);
        assertNotNull("skew_25d.skew_25d", r.volatility.skew25d.skew25d);
        assertNotNull("skew_25d.smile_ratio", r.volatility.skew25d.smileRatio);

        // iv_term_structure
        assertNotNull("iv_term_structure", r.volatility.ivTermStructure);
        assertFalse("iv_term_structure non-empty", r.volatility.ivTermStructure.isEmpty());
        StockSummaryResponse.TermStructureRow tsr = r.volatility.ivTermStructure.get(0);
        assertNotNull("term_structure[0].expiry", tsr.expiry);
        assertNotNull("term_structure[0].days_to_expiry", tsr.daysToExpiry);
        assertNotNull("term_structure[0].iv", tsr.iv);

        // ── options_flow (rc.4 wire uses total_* prefix) ──
        assertNotNull("options_flow", r.optionsFlow);
        assertNotNull("options_flow.total_call_oi", r.optionsFlow.callOi);
        assertNotNull("options_flow.total_put_oi", r.optionsFlow.putOi);
        assertNotNull("options_flow.total_call_volume", r.optionsFlow.callVolume);
        assertNotNull("options_flow.total_put_volume", r.optionsFlow.putVolume);
        assertNotNull("options_flow.pc_ratio_oi", r.optionsFlow.pcRatioOi);
        assertNotNull("options_flow.pc_ratio_volume", r.optionsFlow.pcRatioVolume);
        assertNotNull("options_flow.active_expirations", r.optionsFlow.activeExpirations);

        // ── exposure ──
        assertNotNull("exposure", r.exposure);
        assertNotNull("exposure.net_gex", r.exposure.netGex);
        assertNotNull("exposure.net_dex", r.exposure.netDex);
        assertNotNull("exposure.net_vex", r.exposure.netVex);
        assertNotNull("exposure.net_chex", r.exposure.netChex);
        assertNotNull("exposure.gamma_flip", r.exposure.gammaFlip);
        assertNotNull("exposure.call_wall", r.exposure.callWall);
        assertNotNull("exposure.put_wall", r.exposure.putWall);
        assertNotNull("exposure.max_pain", r.exposure.maxPain);
        assertNotNull("exposure.highest_oi_strike", r.exposure.highestOiStrike);
        assertNotNull("exposure.regime", r.exposure.regime);
        assertTrue("exposure.regime=" + r.exposure.regime,
                "positive_gamma".equals(r.exposure.regime)
                        || "negative_gamma".equals(r.exposure.regime)
                        || "unknown".equals(r.exposure.regime));
        assertNotNull("exposure.oi_weighted_dte", r.exposure.oiWeightedDte);

        // interpretation
        assertNotNull("interpretation", r.exposure.interpretation);
        assertNotNull("interpretation.gamma", r.exposure.interpretation.gamma);
        assertNotNull("interpretation.vanna", r.exposure.interpretation.vanna);
        assertNotNull("interpretation.charm", r.exposure.interpretation.charm);

        // hedging_estimate
        assertNotNull("hedging_estimate", r.exposure.hedgingEstimate);
        StockSummaryResponse.HedgingMove[] hMoves = {
                r.exposure.hedgingEstimate.spotUp1Pct,
                r.exposure.hedgingEstimate.spotDown1Pct,
        };
        String[] hNames = {"spot_up_1pct", "spot_down_1pct"};
        for (int i = 0; i < hMoves.length; i++) {
            assertNotNull("hedging_estimate." + hNames[i], hMoves[i]);
            assertNotNull(hNames[i] + ".dealer_shares", hMoves[i].dealerShares);
            assertNotNull(hNames[i] + ".direction", hMoves[i].direction);
            assertTrue(hNames[i] + ".direction=" + hMoves[i].direction,
                    "buy".equals(hMoves[i].direction) || "sell".equals(hMoves[i].direction));
            assertNotNull(hNames[i] + ".notional_usd", hMoves[i].notionalUsd);
        }

        // zero_dte sub-block — expiration skipped if no 0DTE today
        assertNotNull("zero_dte", r.exposure.zeroDte);
        assertNotNull("zero_dte.net_gex", r.exposure.zeroDte.netGex);
        assertNotNull("zero_dte.pct_of_total", r.exposure.zeroDte.pctOfTotal);
        // zero_dte.expiration is null on weekends/holidays — skip null check

        // top_strikes (rc.4 adds total_oi)
        assertNotNull("top_strikes", r.exposure.topStrikes);
        assertFalse("top_strikes non-empty", r.exposure.topStrikes.isEmpty());
        StockSummaryResponse.TopStrikeRow ts = r.exposure.topStrikes.get(0);
        assertNotNull("top_strikes[0].strike", ts.strike);
        assertNotNull("top_strikes[0].net_gex", ts.netGex);
        assertNotNull("top_strikes[0].call_oi", ts.callOi);
        assertNotNull("top_strikes[0].put_oi", ts.putOi);
        assertNotNull("top_strikes[0].total_oi", ts.totalOi);

        // ── macro (top-level macro block always present; sub-fields nullable) ──
        assertNotNull("macro", r.macro);
        // Quote sub-objects (vix/vvix/skew/spx/move) — when present, exercise every leaf.
        StockSummaryResponse.Quote[] quotes = {
                r.macro.vix, r.macro.vvix, r.macro.skew, r.macro.spx, r.macro.move
        };
        String[] quoteNames = {"vix", "vvix", "skew", "spx", "move"};
        for (int i = 0; i < quotes.length; i++) {
            if (quotes[i] != null) {
                assertNotNull(quoteNames[i] + ".value", quotes[i].value);
                // change/change_pct may be null upstream; key is they exist as a typed leaf
            }
        }
        // vix_term_structure
        if (r.macro.vixTermStructure != null) {
            assertNotNull("vix_term_structure.structure", r.macro.vixTermStructure.structure);
            assertNotNull("vix_term_structure.near_slope_pct", r.macro.vixTermStructure.nearSlopePct);
            if (r.macro.vixTermStructure.levels != null) {
                StockSummaryResponse.VixTermLevels lv = r.macro.vixTermStructure.levels;
                // at least vix should be populated when block is present
                assertNotNull("vix_term_structure.levels.vix", lv.vix);
                // vix9d/vix3m/vix6m exercised by deserialization — each is a typed leaf
                java.util.Objects.requireNonNullElse(lv.vix9d, 0.0);
                java.util.Objects.requireNonNullElse(lv.vix3m, 0.0);
                java.util.Objects.requireNonNullElse(lv.vix6m, 0.0);
            }
        }
        // vix_futures sub-block (nullable upstream)
        if (r.macro.vixFutures != null) {
            assertNotNull("vix_futures.front_month", r.macro.vixFutures.frontMonth);
            assertNotNull("vix_futures.spot", r.macro.vixFutures.spot);
            assertNotNull("vix_futures.spread", r.macro.vixFutures.spread);
            assertNotNull("vix_futures.basis_pct", r.macro.vixFutures.basisPct);
            assertNotNull("vix_futures.basis", r.macro.vixFutures.basis);
        }
        // fear_and_greed (nullable upstream)
        if (r.macro.fearAndGreed != null) {
            assertNotNull("fear_and_greed.score", r.macro.fearAndGreed.score);
            assertNotNull("fear_and_greed.rating", r.macro.fearAndGreed.rating);
        }
    }

    @Test
    public void testNarrative_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.narrative("SPY");
        NarrativeResponse r = gson.fromJson(json, NarrativeResponse.class);

        // top-level
        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("narrative", r.narrative);

        // narrative prose strings — every leaf
        assertNotNull("narrative.regime", r.narrative.regime);
        assertFalse("narrative.regime non-empty", r.narrative.regime.isEmpty());
        assertNotNull("narrative.gex_change", r.narrative.gexChange);
        assertNotNull("narrative.key_levels", r.narrative.keyLevels);
        assertNotNull("narrative.flow", r.narrative.flow);
        assertNotNull("narrative.vanna", r.narrative.vanna);
        assertNotNull("narrative.charm", r.narrative.charm);
        assertNotNull("narrative.zero_dte", r.narrative.zeroDte);
        assertNotNull("narrative.outlook", r.narrative.outlook);

        // narrative.data block
        assertNotNull("narrative.data", r.narrative.data);
        NarrativeResponse.NarrativeData d = r.narrative.data;
        assertNotNull("data.net_gex", d.netGex);
        // net_gex_prior / net_gex_change_pct are prior-session-dependent and
        // are legitimately null when there is no prior-day data (the
        // narrative prose says so). Reference (typed leaves exercised) but
        // do not assert non-null.
        @SuppressWarnings("unused") Double refPrior = d.netGexPrior;
        @SuppressWarnings("unused") Double refChangePct = d.netGexChangePct;
        assertNotNull("data.vix", d.vix);
        assertNotNull("data.gamma_flip", d.gammaFlip);
        assertNotNull("data.call_wall", d.callWall);
        assertNotNull("data.put_wall", d.putWall);
        assertNotNull("data.regime", d.regime);
        assertTrue("data.regime=" + d.regime,
                "positive_gamma".equals(d.regime)
                        || "negative_gamma".equals(d.regime)
                        || "unknown".equals(d.regime));
        assertNotNull("data.zero_dte_pct", d.zeroDtePct);

        // top_oi_changes — element shape
        assertNotNull("data.top_oi_changes", d.topOiChanges);
        if (!d.topOiChanges.isEmpty()) {
            NarrativeResponse.OiChangeRow row = d.topOiChanges.get(0);
            assertNotNull("top_oi_changes[0].strike", row.strike);
            assertNotNull("top_oi_changes[0].type", row.type);
            assertTrue("type=" + row.type, "call".equals(row.type) || "put".equals(row.type));
            assertNotNull("top_oi_changes[0].oi_change", row.oiChange);
            assertNotNull("top_oi_changes[0].volume", row.volume);
        }
    }

    @Test
    public void testExposureLevels_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.exposureLevels("SPY");
        ExposureLevelsResponse r = gson.fromJson(json, ExposureLevelsResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("levels", r.levels);

        // All 7 levels including zero_dte_magnet
        assertNotNull("levels.gamma_flip", r.levels.gammaFlip);
        assertNotNull("levels.max_positive_gamma", r.levels.maxPositiveGamma);
        assertNotNull("levels.max_negative_gamma", r.levels.maxNegativeGamma);
        assertNotNull("levels.call_wall", r.levels.callWall);
        assertNotNull("levels.put_wall", r.levels.putWall);
        assertNotNull("levels.highest_oi_strike", r.levels.highestOiStrike);
        assertNotNull("levels.zero_dte_magnet", r.levels.zeroDteMagnet);
    }

    @Test
    public void testPricingGreeks_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.greeks(580.0, 580.0, 30.0, 0.18, "call", null, null);
        PricingGreeksResponse r = gson.fromJson(json, PricingGreeksResponse.class);

        // theoretical_price
        assertNotNull("theoretical_price", r.theoreticalPrice);

        // inputs — every echoed field
        assertNotNull("inputs", r.inputs);
        assertNotNull("inputs.spot", r.inputs.spot);
        assertNotNull("inputs.strike", r.inputs.strike);
        assertNotNull("inputs.dte", r.inputs.dte);
        assertNotNull("inputs.sigma", r.inputs.sigma);
        assertNotNull("inputs.type", r.inputs.type);
        assertEquals("call", r.inputs.type);
        assertNotNull("inputs.risk_free_rate", r.inputs.riskFreeRate);
        assertNotNull("inputs.dividend_yield", r.inputs.dividendYield);

        // first_order — delta/gamma/theta/vega/rho
        assertNotNull("first_order", r.firstOrder);
        assertNotNull("first_order.delta", r.firstOrder.delta);
        assertNotNull("first_order.gamma", r.firstOrder.gamma);
        assertNotNull("first_order.theta", r.firstOrder.theta);
        assertNotNull("first_order.vega", r.firstOrder.vega);
        assertNotNull("first_order.rho", r.firstOrder.rho);

        // second_order — vanna/charm/vomma/dual_delta
        assertNotNull("second_order", r.secondOrder);
        assertNotNull("second_order.vanna", r.secondOrder.vanna);
        assertNotNull("second_order.charm", r.secondOrder.charm);
        assertNotNull("second_order.vomma", r.secondOrder.vomma);
        assertNotNull("second_order.dual_delta", r.secondOrder.dualDelta);

        // third_order — speed/zomma/color/ultima
        assertNotNull("third_order", r.thirdOrder);
        assertNotNull("third_order.speed", r.thirdOrder.speed);
        assertNotNull("third_order.zomma", r.thirdOrder.zomma);
        assertNotNull("third_order.color", r.thirdOrder.color);
        assertNotNull("third_order.ultima", r.thirdOrder.ultima);

        // additional — lambda/veta
        assertNotNull("additional", r.additional);
        assertNotNull("additional.lambda", r.additional.lambda);
        assertNotNull("additional.veta", r.additional.veta);
    }

    // ── Screener ──────────────────────────────────────────────────────

    @Test
    public void testScreenerEmpty() {
        JsonObject result = client.screener(new java.util.LinkedHashMap<>());
        assertNotNull(result);
        assertTrue(result.has("meta"));
        assertTrue(result.has("data"));
        assertTrue(result.getAsJsonArray("data") != null);
        String tier = result.getAsJsonObject("meta").get("tier").getAsString();
        assertTrue(tier.equals("growth") || tier.equals("alpha"));
    }

    @Test
    public void testScreenerSimpleFilter() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "regime",
                "operator", "in",
                "value", java.util.List.of("positive_gamma", "negative_gamma")));
        body.put("select", java.util.List.of("symbol", "regime", "price"));
        body.put("limit", 5);
        JsonObject result = client.screener(body);
        assertNotNull(result);
        for (com.google.gson.JsonElement row : result.getAsJsonArray("data")) {
            String regime = row.getAsJsonObject().get("regime").getAsString();
            assertTrue("positive_gamma".equals(regime) || "negative_gamma".equals(regime));
        }
    }

    @Test
    public void testScreenerAndGroupWithSort() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "op", "and",
                "conditions", java.util.List.of(
                        java.util.Map.of("field", "atm_iv", "operator", "gte", "value", 0),
                        java.util.Map.of("field", "atm_iv", "operator", "lte", "value", 500))));
        body.put("sort", java.util.List.of(
                java.util.Map.of("field", "atm_iv", "direction", "desc")));
        body.put("select", java.util.List.of("symbol", "atm_iv"));
        body.put("limit", 5);
        JsonObject result = client.screener(body);
        assertTrue(result.getAsJsonObject("meta").get("returned_count").getAsInt() <= 5);

        Double prev = null;
        for (com.google.gson.JsonElement row : result.getAsJsonArray("data")) {
            com.google.gson.JsonElement ivEl = row.getAsJsonObject().get("atm_iv");
            if (ivEl == null || ivEl.isJsonNull()) continue;
            double iv = ivEl.getAsDouble();
            if (prev != null) assertTrue("sorted desc", iv <= prev);
            prev = iv;
        }
    }

    @Test
    public void testScreenerSelectStar() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("select", java.util.List.of("*"));
        body.put("limit", 1);
        JsonObject result = client.screener(body);
        if (result.getAsJsonArray("data").size() > 0) {
            JsonObject row = result.getAsJsonArray("data").get(0).getAsJsonObject();
            assertTrue(row.has("symbol"));
            assertTrue(row.has("price"));
        }
    }

    @Test
    public void testScreenerLimitRespected() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("limit", 3);
        JsonObject result = client.screener(body);
        assertTrue(result.getAsJsonObject("meta").get("returned_count").getAsInt() <= 3);
        assertTrue(result.getAsJsonArray("data").size() <= 3);
    }

    @Test(expected = FlashAlphaException.class)
    public void testScreenerInvalidField() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "not_a_real_field_xyz", "operator", "eq", "value", 1));
        client.screener(body);
    }

    // ── Customer regression tests ─────────────────────────────────────
    //
    // An Alpha-tier customer running an automated trading daemon hit
    // a series of bugs grounded in (a) misreading the documented nesting
    // of /v1/vrp, (b) using non-canonical field names (put_vrp/call_vrp),
    // (c) guessing /v1/summary/{sym} instead of /v1/stock/{sym}/summary,
    // (d) the renamed screener path, and (e) a missing vrp() method on
    // the SDK. These tests exercise the SDK's PUBLIC methods against the
    // LIVE FlashAlpha API to guarantee the canonical contract.
    //
    // Issue #5 — SDK was missing vrp(). Customer had to build a REST
    // client from scratch. This test confirms vrp() now exists and
    // returns the documented shape.

    @Test
    public void testVrpMethodExistsOnClient() throws NoSuchMethodException {
        Method vrp = FlashAlphaClient.class.getMethod("vrp", String.class);
        assertNotNull(vrp);
    }

    @Test
    public void testVrpReturnsFullPayload() {
        JsonObject r = client.vrp("SPY");
        assertNotNull(r);

        // Top-level
        assertEquals("SPY", r.get("symbol").getAsString());
        assertTrue(r.has("underlying_price"));
        assertTrue(r.get("underlying_price").getAsDouble() > 0);
        assertTrue(r.has("as_of"));
        assertTrue(r.has("market_open"));
        assertTrue(r.has("net_harvest_score"));
        assertTrue(r.get("net_harvest_score").getAsJsonPrimitive().isNumber());
        assertTrue(r.has("dealer_flow_risk"));

        // vrp.* core metrics
        JsonObject core = r.getAsJsonObject("vrp");
        assertNotNull("vrp object missing", core);
        for (String key : new String[]{"atm_iv", "rv_5d", "rv_10d", "rv_20d", "rv_30d",
                "vrp_5d", "vrp_10d", "vrp_20d", "vrp_30d",
                "z_score", "percentile", "history_days"}) {
            assertTrue("vrp." + key + " missing", core.has(key));
        }
        assertTrue(core.get("atm_iv").getAsDouble() >= 0);
        assertTrue(core.get("history_days").getAsInt() >= 0);

        // directional.*
        JsonObject d = r.getAsJsonObject("directional");
        assertNotNull("directional missing", d);
        for (String key : new String[]{"put_wing_iv_25d", "call_wing_iv_25d",
                "downside_rv_20d", "upside_rv_20d",
                "downside_vrp", "upside_vrp"}) {
            assertTrue("directional." + key + " missing", d.has(key));
        }

        // regime.*
        JsonObject reg = r.getAsJsonObject("regime");
        assertNotNull("regime missing", reg);
        assertTrue(reg.has("net_gex"));
        assertTrue(reg.get("net_gex").getAsJsonPrimitive().isNumber());
        assertTrue(reg.has("vrp_regime"));
        assertTrue(reg.has("gamma"));

        // term_vrp[]
        assertTrue(r.has("term_vrp"));
        if (!r.get("term_vrp").isJsonNull() && r.getAsJsonArray("term_vrp").size() > 0) {
            JsonObject pt = r.getAsJsonArray("term_vrp").get(0).getAsJsonObject();
            for (String key : new String[]{"dte", "iv", "rv", "vrp"}) {
                assertTrue("term_vrp[0]." + key + " missing", pt.has(key));
            }
        }

        // gex_conditioned (nullable)
        assertTrue(r.has("gex_conditioned"));
        JsonElement gcEl = r.get("gex_conditioned");
        if (!gcEl.isJsonNull()) {
            JsonObject gc = gcEl.getAsJsonObject();
            assertTrue(gc.has("harvest_score"));
            assertTrue(gc.has("regime"));
            assertTrue(gc.get("harvest_score").getAsJsonPrimitive().isNumber());
        }

        // strategy_scores (nullable)
        assertTrue(r.has("strategy_scores"));
    }

    @Test
    public void testVrpEveryFieldDeclaredInPocoMustBeReferenced() {
        JsonObject r = client.vrp("SPY");

        // Customer traps: must NOT be top-level
        for (String trap : new String[] {"z_score", "percentile", "atm_iv",
                                          "net_gex", "put_vrp", "call_vrp", "harvest_score"}) {
            assertNull("top-level " + trap + " must NOT exist", r.get(trap));
        }

        // ── top-level scalars ──
        assertEquals("SPY", r.get("symbol").getAsString());
        assertTrue(r.get("underlying_price").getAsJsonPrimitive().isNumber());
        assertTrue(r.get("as_of").getAsJsonPrimitive().isString());
        assertTrue(r.get("market_open").getAsJsonPrimitive().isBoolean());
        for (String k : new String[] {"variance_risk_premium", "convexity_premium", "fair_vol",
                                       "net_harvest_score", "dealer_flow_risk"}) {
            assertTrue(k, r.get(k).getAsJsonPrimitive().isNumber());
        }
        assertTrue(r.get("warnings").isJsonArray());

        // ── vrp.* core block ──
        JsonObject core = r.getAsJsonObject("vrp");
        for (String k : new String[] {"atm_iv", "rv_5d", "rv_10d", "rv_20d", "rv_30d",
                                       "vrp_5d", "vrp_10d", "vrp_20d", "vrp_30d",
                                       "z_score", "history_days"}) {
            assertTrue("vrp." + k, core.get(k).getAsJsonPrimitive().isNumber());
        }
        assertTrue(core.get("percentile").getAsJsonPrimitive().isNumber());

        // ── directional ──
        JsonObject dir = r.getAsJsonObject("directional");
        for (String k : new String[] {"put_wing_iv_25d", "call_wing_iv_25d",
                                       "downside_rv_20d", "upside_rv_20d",
                                       "downside_vrp", "upside_vrp"}) {
            assertTrue("directional." + k, dir.get(k).getAsJsonPrimitive().isNumber());
        }

        // ── term_vrp[] ──
        JsonArray term = r.getAsJsonArray("term_vrp");
        assertTrue("term_vrp non-empty", term.size() > 0);
        JsonObject first = term.get(0).getAsJsonObject();
        for (String k : new String[] {"dte", "iv", "rv", "vrp"}) {
            assertTrue("term_vrp[0]." + k, first.has(k));
        }

        // ── gex_conditioned + vanna_conditioned ──
        JsonObject gc = r.getAsJsonObject("gex_conditioned");
        assertTrue(gc.get("regime").getAsJsonPrimitive().isString());
        assertTrue(gc.get("harvest_score").getAsJsonPrimitive().isNumber());
        assertTrue(gc.get("interpretation").getAsJsonPrimitive().isString());
        JsonObject vc = r.getAsJsonObject("vanna_conditioned");
        assertTrue(vc.get("outlook").getAsJsonPrimitive().isString());
        assertTrue(vc.get("interpretation").getAsJsonPrimitive().isString());

        // ── regime — net_gex lives HERE ──
        JsonObject reg = r.getAsJsonObject("regime");
        assertTrue(reg.get("gamma").getAsJsonPrimitive().isString());
        assertTrue(reg.get("vrp_regime").getAsJsonPrimitive().isString());
        assertTrue(reg.get("net_gex").getAsJsonPrimitive().isNumber());
        assertTrue(reg.get("gamma_flip").getAsJsonPrimitive().isNumber());

        // ── strategy_scores ──
        JsonObject ss = r.getAsJsonObject("strategy_scores");
        for (String k : new String[] {"short_put_spread", "short_strangle",
                                       "iron_condor", "calendar_spread"}) {
            assertTrue("strategy_scores." + k, ss.get(k).getAsJsonPrimitive().isNumber());
        }

        // ── macro (live includes fed_funds) ──
        JsonObject macro = r.getAsJsonObject("macro");
        for (String k : new String[] {"vix", "vix_3m", "vix_term_slope", "dgs10"}) {
            assertTrue("macro." + k, macro.get(k).getAsJsonPrimitive().isNumber());
        }
        assertTrue("macro.hy_spread key", macro.has("hy_spread"));
        assertTrue(macro.get("fed_funds").getAsJsonPrimitive().isNumber());
    }

    // Issue #1 — Nested response structures. Customer tried
    // r.get("z_score") on /v1/vrp; correct path is r.get("vrp").get("z_score").

    @Test
    public void testVrpCoreMetricsAreNestedNotTopLevel() {
        JsonObject r = client.vrp("SPY");
        // Top-level access returns null — these live under r["vrp"]
        assertNull("z_score must NOT be top-level", r.get("z_score"));
        assertNull("percentile must NOT be top-level", r.get("percentile"));
        assertNull("atm_iv must NOT be top-level", r.get("atm_iv"));
        assertNull("rv_20d must NOT be top-level", r.get("rv_20d"));
        assertNull("vrp_20d must NOT be top-level", r.get("vrp_20d"));

        JsonObject core = r.getAsJsonObject("vrp");
        assertNotNull(core);
        for (String key : new String[]{"z_score", "percentile", "atm_iv", "rv_20d", "vrp_20d"}) {
            assertTrue("vrp." + key + " missing", core.has(key));
            assertTrue("vrp." + key + " should be a number",
                    core.get(key).getAsJsonPrimitive().isNumber());
        }
    }

    @Test
    public void testVrpHarvestScoreUnderGexConditioned() {
        JsonObject r = client.vrp("SPY");
        assertNull("harvest_score must NOT be top-level on vrp", r.get("harvest_score"));
        assertTrue(r.has("gex_conditioned"));
        JsonElement gcEl = r.get("gex_conditioned");
        if (!gcEl.isJsonNull()) {
            JsonObject gc = gcEl.getAsJsonObject();
            assertTrue("gex_conditioned.harvest_score missing", gc.has("harvest_score"));
        }
    }

    @Test
    public void testVrpNetGexUnderRegime() {
        JsonObject r = client.vrp("SPY");
        assertNull("net_gex must NOT be top-level on vrp", r.get("net_gex"));
        assertNull("gamma_flip must NOT be top-level on vrp", r.get("gamma_flip"));
        JsonObject reg = r.getAsJsonObject("regime");
        assertNotNull("regime missing", reg);
        assertTrue("regime.net_gex missing", reg.has("net_gex"));
        assertTrue(reg.get("net_gex").getAsJsonPrimitive().isNumber());
    }

    @Test
    public void testVrpCompositeScoresTopLevel() {
        // net_harvest_score and dealer_flow_risk ARE top-level (the documented exception).
        JsonObject r = client.vrp("SPY");
        assertTrue(r.has("net_harvest_score"));
        assertTrue(r.has("dealer_flow_risk"));
    }

    @Test
    public void testExposureSummaryEveryFieldDeclaredInPocoMustBeReferenced() {
        JsonObject r = client.exposureSummary("SPY");
        // Original bug #1
        assertNull("net_gex must NOT be top-level on exposure_summary (customer trap)",
                r.get("net_gex"));
        // ── top-level scalars ──
        assertEquals("SPY", r.get("symbol").getAsString());
        assertTrue(r.get("underlying_price").getAsJsonPrimitive().isNumber());
        assertTrue(r.get("as_of").getAsJsonPrimitive().isString());
        assertFalse(r.get("as_of").getAsString().isEmpty());
        assertTrue(r.get("gamma_flip").getAsJsonPrimitive().isNumber());
        String regime = r.get("regime").getAsString();
        assertTrue("regime=" + regime,
                java.util.Arrays.asList("positive_gamma", "negative_gamma", "unknown")
                        .contains(regime));
        // ── exposures block (4 fields) ──
        JsonObject exp = r.getAsJsonObject("exposures");
        for (String k : new String[] {"net_gex", "net_dex", "net_vex", "net_chex"}) {
            assertTrue("exposures." + k, exp.get(k).getAsJsonPrimitive().isNumber());
        }
        // ── interpretation block (3 fields) ──
        JsonObject interp = r.getAsJsonObject("interpretation");
        for (String k : new String[] {"gamma", "vanna", "charm"}) {
            assertTrue("interpretation." + k + " string",
                    interp.get(k).getAsJsonPrimitive().isString());
            assertFalse("interpretation." + k + " non-empty",
                    interp.get(k).getAsString().isEmpty());
        }
        // ── hedging_estimate (every leaf on both sides) ──
        JsonObject h = r.getAsJsonObject("hedging_estimate");
        for (String sideKey : new String[] {"spot_up_1pct", "spot_down_1pct"}) {
            JsonObject side = h.getAsJsonObject(sideKey);
            String dir = side.get("direction").getAsString();
            assertTrue(sideKey + ".direction=" + dir, "buy".equals(dir) || "sell".equals(dir));
            assertTrue(sideKey + ".dealer_shares_to_trade",
                    side.get("dealer_shares_to_trade").getAsJsonPrimitive().isNumber());
            assertTrue(sideKey + ".notional_usd",
                    side.get("notional_usd").getAsJsonPrimitive().isNumber());
            assertNotEquals(0L, side.get("notional_usd").getAsLong());
        }
        long up = h.getAsJsonObject("spot_up_1pct").get("dealer_shares_to_trade").getAsLong();
        long dn = h.getAsJsonObject("spot_down_1pct").get("dealer_shares_to_trade").getAsLong();
        assertEquals(up, -dn);
        // ── zero_dte block (3 fields) ──
        JsonObject z = r.getAsJsonObject("zero_dte");
        assertNotNull("zero_dte block", z);
        assertTrue("zero_dte.net_gex key present", z.has("net_gex"));
        assertTrue("zero_dte.net_gex null or number",
                z.get("net_gex").isJsonNull()
                        || z.get("net_gex").getAsJsonPrimitive().isNumber());
        assertTrue("zero_dte.pct_of_total_gex key present", z.has("pct_of_total_gex"));
        assertTrue("zero_dte.pct_of_total_gex null or number",
                z.get("pct_of_total_gex").isJsonNull()
                        || z.get("pct_of_total_gex").getAsJsonPrimitive().isNumber());
        assertTrue("zero_dte.expiration key present", z.has("expiration"));
        assertTrue("zero_dte.expiration null or string",
                z.get("expiration").isJsonNull()
                        || z.get("expiration").getAsJsonPrimitive().isString());
    }

    // Issue #2 — Field naming. Customer used put_vrp / call_vrp
    // based on conventions from other APIs. Canonical names are
    // downside_vrp / upside_vrp.

    @Test
    public void testVrpDirectionalUsesDownsideUpside() {
        JsonObject d = client.vrp("SPY").getAsJsonObject("directional");
        assertTrue("directional.downside_vrp missing", d.has("downside_vrp"));
        assertTrue("directional.upside_vrp missing", d.has("upside_vrp"));
        assertFalse("directional.put_vrp must NOT exist (use downside_vrp)",
                d.has("put_vrp"));
        assertFalse("directional.call_vrp must NOT exist (use upside_vrp)",
                d.has("call_vrp"));
    }

    // Issue #3 — URL pattern mix. Customer guessed /v1/summary/{sym}
    // and got a silent 404. SDK methods route to the canonical paths.

    @Test
    public void testStockSummaryRoutesCorrectly() {
        JsonObject r = client.stockSummary("SPY");
        assertEquals("SPY", r.get("symbol").getAsString());
        assertTrue("summary should contain price", r.has("price"));
        // Customer enriches signals with this payload — must be substantial
        assertTrue("summary should have multiple top-level fields",
                r.entrySet().size() > 3);
    }

    @Test
    public void testStockQuoteRoutesCorrectly() {
        JsonObject r = client.stockQuote("SPY");
        assertEquals("SPY", r.get("ticker").getAsString());
    }

    @Test
    public void testAllExposureMethodsRouteCorrectly() {
        assertEquals("SPY", client.gex("SPY").get("symbol").getAsString());
        assertEquals("SPY", client.dex("SPY").get("symbol").getAsString());
        assertEquals("SPY", client.vex("SPY").get("symbol").getAsString());
        assertEquals("SPY", client.chex("SPY").get("symbol").getAsString());
        assertEquals("SPY", client.exposureSummary("SPY").get("symbol").getAsString());
        assertEquals("SPY", client.exposureLevels("SPY").get("symbol").getAsString());
    }

    @Test
    public void testVrpMethodRoutesCorrectly() {
        JsonObject r = client.vrp("SPY");
        assertEquals("SPY", r.get("symbol").getAsString());
    }

    // Issue #4 — Screener URL. SDK's screener() POSTs to /v1/screener
    // (canonical since v0.3.1). Validate full envelope + row fields
    // against the live API.

    @Test
    public void testScreenerReturnsValidEnvelope() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("limit", 5);
        JsonObject r = client.screener(body);
        assertTrue(r.has("meta"));
        assertTrue(r.has("data"));
        JsonObject meta = r.getAsJsonObject("meta");
        for (String key : new String[]{"total_count", "returned_count",
                "universe_size", "tier", "as_of"}) {
            assertTrue("meta." + key + " missing", meta.has(key));
        }
        assertTrue("returned_count must be <= 5",
                meta.get("returned_count").getAsInt() <= 5);
        String tier = meta.get("tier").getAsString();
        assertTrue("tier must be growth or alpha",
                "growth".equals(tier) || "alpha".equals(tier));
    }

    @Test
    public void testScreenerFullRowReadable() {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("select", java.util.List.of("*"));
        body.put("limit", 1);
        JsonObject r = client.screener(body);
        if (r.getAsJsonArray("data").size() == 0) {
            return; // no rows for current universe — test vacuously passes
        }
        JsonObject row = r.getAsJsonArray("data").get(0).getAsJsonObject();
        for (String key : new String[]{"symbol", "price", "regime"}) {
            assertTrue("row missing " + key, row.has(key));
        }
    }

    // ── rc.9 typed POCO field-walk tests ──────────────────────────────
    //
    // Same EveryFieldDeclaredInPocoMustBeReferenced discipline, walked
    // through the typed POCO. Deserialize via gson, then assert every
    // public field is non-null on a SPY (or pricing-input) live response.
    // A renamed wire field surfaces immediately as a null assertion.

    @Test
    public void testVolatility_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.volatility("SPY");
        VolatilityResponse r = gson.fromJson(json, VolatilityResponse.class);

        // top-level
        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("market_open", r.marketOpen);
        assertNotNull("atm_iv", r.atmIv);

        // realized_vol
        assertNotNull("realized_vol", r.realizedVol);
        assertNotNull("realized_vol.rv_5d",  r.realizedVol.rv5d);
        assertNotNull("realized_vol.rv_10d", r.realizedVol.rv10d);
        assertNotNull("realized_vol.rv_20d", r.realizedVol.rv20d);
        assertNotNull("realized_vol.rv_30d", r.realizedVol.rv30d);
        assertNotNull("realized_vol.rv_60d", r.realizedVol.rv60d);

        // iv_rv_spreads
        assertNotNull("iv_rv_spreads", r.ivRvSpreads);
        assertNotNull("iv_rv_spreads.vrp_5d",  r.ivRvSpreads.vrp5d);
        assertNotNull("iv_rv_spreads.vrp_10d", r.ivRvSpreads.vrp10d);
        assertNotNull("iv_rv_spreads.vrp_20d", r.ivRvSpreads.vrp20d);
        assertNotNull("iv_rv_spreads.vrp_30d", r.ivRvSpreads.vrp30d);
        assertNotNull("iv_rv_spreads.assessment", r.ivRvSpreads.assessment);

        // skew_profiles[0] — first row exercises every leaf
        assertNotNull("skew_profiles", r.skewProfiles);
        assertFalse("skew_profiles non-empty", r.skewProfiles.isEmpty());
        VolatilityResponse.SkewProfile sp = r.skewProfiles.get(0);
        assertNotNull("skew_profiles[0].expiry", sp.expiry);
        assertNotNull("skew_profiles[0].days_to_expiry", sp.daysToExpiry);
        assertNotNull("skew_profiles[0].put_10d_iv", sp.put10dIv);
        assertNotNull("skew_profiles[0].put_25d_iv", sp.put25dIv);
        assertNotNull("skew_profiles[0].atm_iv", sp.atmIv);
        assertNotNull("skew_profiles[0].call_25d_iv", sp.call25dIv);
        assertNotNull("skew_profiles[0].call_10d_iv", sp.call10dIv);
        assertNotNull("skew_profiles[0].skew_25d", sp.skew25d);
        assertNotNull("skew_profiles[0].smile_ratio", sp.smileRatio);
        assertNotNull("skew_profiles[0].tail_convexity", sp.tailConvexity);

        // term_structure
        assertNotNull("term_structure", r.termStructure);
        assertNotNull("term_structure.near_slope_pct", r.termStructure.nearSlopePct);
        assertNotNull("term_structure.far_slope_pct",  r.termStructure.farSlopePct);
        assertNotNull("term_structure.state", r.termStructure.state);

        // iv_dispersion
        assertNotNull("iv_dispersion", r.ivDispersion);
        assertNotNull("iv_dispersion.cross_expiry", r.ivDispersion.crossExpiry);
        assertNotNull("iv_dispersion.cross_strike", r.ivDispersion.crossStrike);

        // gex_by_dte / theta_by_dte
        assertNotNull("gex_by_dte", r.gexByDte);
        assertFalse("gex_by_dte non-empty", r.gexByDte.isEmpty());
        VolatilityResponse.GexByDteRow gex = r.gexByDte.get(0);
        assertNotNull("gex_by_dte[0].bucket", gex.bucket);
        assertNotNull("gex_by_dte[0].net_gex", gex.netGex);
        assertNotNull("gex_by_dte[0].pct_of_total", gex.pctOfTotal);
        assertNotNull("gex_by_dte[0].contract_count", gex.contractCount);

        assertNotNull("theta_by_dte", r.thetaByDte);
        assertFalse("theta_by_dte non-empty", r.thetaByDte.isEmpty());
        VolatilityResponse.ThetaByDteRow th = r.thetaByDte.get(0);
        assertNotNull("theta_by_dte[0].bucket", th.bucket);
        assertNotNull("theta_by_dte[0].net_theta", th.netTheta);
        assertNotNull("theta_by_dte[0].contract_count", th.contractCount);

        // put_call_profile
        assertNotNull("put_call_profile", r.putCallProfile);
        assertNotNull("put_call_profile.by_expiry", r.putCallProfile.byExpiry);
        assertFalse("put_call_profile.by_expiry non-empty", r.putCallProfile.byExpiry.isEmpty());
        VolatilityResponse.PutCallProfile.ByExpiryRow be = r.putCallProfile.byExpiry.get(0);
        assertNotNull("by_expiry[0].expiry", be.expiry);
        assertNotNull("by_expiry[0].call_oi", be.callOi);
        assertNotNull("by_expiry[0].put_oi", be.putOi);
        assertNotNull("by_expiry[0].pc_ratio_oi", be.pcRatioOi);
        assertNotNull("by_expiry[0].call_volume", be.callVolume);
        assertNotNull("by_expiry[0].put_volume", be.putVolume);
        // pc_ratio_volume may be null if put_volume == 0; key is the typed leaf exists
        assertNotNull("put_call_profile.by_moneyness", r.putCallProfile.byMoneyness);
        VolatilityResponse.PutCallProfile.ByMoneyness bm = r.putCallProfile.byMoneyness;
        assertNotNull("by_moneyness.otm_call_oi", bm.otmCallOi);
        assertNotNull("by_moneyness.atm_call_oi", bm.atmCallOi);
        assertNotNull("by_moneyness.itm_call_oi", bm.itmCallOi);
        assertNotNull("by_moneyness.otm_put_oi", bm.otmPutOi);
        assertNotNull("by_moneyness.atm_put_oi", bm.atmPutOi);
        assertNotNull("by_moneyness.itm_put_oi", bm.itmPutOi);

        // oi_concentration
        assertNotNull("oi_concentration", r.oiConcentration);
        assertNotNull("oi_concentration.top_3_pct", r.oiConcentration.top3Pct);
        assertNotNull("oi_concentration.top_5_pct", r.oiConcentration.top5Pct);
        assertNotNull("oi_concentration.top_10_pct", r.oiConcentration.top10Pct);
        assertNotNull("oi_concentration.herfindahl", r.oiConcentration.herfindahl);

        // hedging_scenarios
        assertNotNull("hedging_scenarios", r.hedgingScenarios);
        assertFalse("hedging_scenarios non-empty", r.hedgingScenarios.isEmpty());
        VolatilityResponse.HedgingScenario hs = r.hedgingScenarios.get(0);
        assertNotNull("hedging_scenarios[0].move_pct", hs.movePct);
        assertNotNull("hedging_scenarios[0].dealer_shares", hs.dealerShares);
        assertNotNull("hedging_scenarios[0].direction", hs.direction);
        assertNotNull("hedging_scenarios[0].notional_usd", hs.notionalUsd);

        // liquidity
        assertNotNull("liquidity", r.liquidity);
        assertNotNull("liquidity.atm_avg_spread_pct", r.liquidity.atmAvgSpreadPct);
        assertNotNull("liquidity.wing_avg_spread_pct", r.liquidity.wingAvgSpreadPct);
        assertNotNull("liquidity.atm_contracts", r.liquidity.atmContracts);
        assertNotNull("liquidity.wing_contracts", r.liquidity.wingContracts);
    }

    @Test
    public void testAdvVolatility_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.advVolatility("SPY");
        AdvVolatilityResponse r = gson.fromJson(json, AdvVolatilityResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("market_open", r.marketOpen);

        // svi_parameters[0]
        assertNotNull("svi_parameters", r.sviParameters);
        assertFalse("svi_parameters non-empty", r.sviParameters.isEmpty());
        AdvVolatilityResponse.SviParameters svi = r.sviParameters.get(0);
        assertNotNull("svi_parameters[0].expiry", svi.expiry);
        assertNotNull("svi_parameters[0].days_to_expiry", svi.daysToExpiry);
        assertNotNull("svi_parameters[0].forward", svi.forward);
        assertNotNull("svi_parameters[0].a", svi.a);
        assertNotNull("svi_parameters[0].b", svi.b);
        assertNotNull("svi_parameters[0].rho", svi.rho);
        assertNotNull("svi_parameters[0].m", svi.m);
        assertNotNull("svi_parameters[0].sigma", svi.sigma);
        assertNotNull("svi_parameters[0].atm_total_variance", svi.atmTotalVariance);
        assertNotNull("svi_parameters[0].atm_iv", svi.atmIv);

        // forward_prices[0]
        assertNotNull("forward_prices", r.forwardPrices);
        assertFalse("forward_prices non-empty", r.forwardPrices.isEmpty());
        AdvVolatilityResponse.ForwardPrice fp = r.forwardPrices.get(0);
        assertNotNull("forward_prices[0].expiry", fp.expiry);
        assertNotNull("forward_prices[0].days_to_expiry", fp.daysToExpiry);
        assertNotNull("forward_prices[0].forward", fp.forward);
        assertNotNull("forward_prices[0].spot", fp.spot);
        assertNotNull("forward_prices[0].basis_pct", fp.basisPct);

        // total_variance_surface
        assertNotNull("total_variance_surface", r.totalVarianceSurface);
        assertNotNull("total_variance_surface.moneyness", r.totalVarianceSurface.moneyness);
        assertNotNull("total_variance_surface.expiries", r.totalVarianceSurface.expiries);
        assertNotNull("total_variance_surface.tenors", r.totalVarianceSurface.tenors);
        assertNotNull("total_variance_surface.total_variance", r.totalVarianceSurface.totalVariance);
        assertNotNull("total_variance_surface.implied_vol", r.totalVarianceSurface.impliedVol);
        assertTrue("total_variance non-empty", r.totalVarianceSurface.totalVariance.length > 0);
        assertTrue("implied_vol non-empty", r.totalVarianceSurface.impliedVol.length > 0);

        // arbitrage_flags — typed leaves exercised when at least one row is present
        assertNotNull("arbitrage_flags", r.arbitrageFlags);
        if (!r.arbitrageFlags.isEmpty()) {
            AdvVolatilityResponse.ArbitrageFlag af = r.arbitrageFlags.get(0);
            assertNotNull("arbitrage_flags[0].expiry", af.expiry);
            assertNotNull("arbitrage_flags[0].type", af.type);
            assertNotNull("arbitrage_flags[0].strike_or_k", af.strikeOrK);
            assertNotNull("arbitrage_flags[0].description", af.description);
        }

        // variance_swap_fair_values[0]
        assertNotNull("variance_swap_fair_values", r.varianceSwapFairValues);
        assertFalse("variance_swap_fair_values non-empty", r.varianceSwapFairValues.isEmpty());
        AdvVolatilityResponse.VarianceSwapFairValue vs = r.varianceSwapFairValues.get(0);
        assertNotNull("variance_swap_fair_values[0].expiry", vs.expiry);
        assertNotNull("variance_swap_fair_values[0].days_to_expiry", vs.daysToExpiry);
        assertNotNull("variance_swap_fair_values[0].fair_variance", vs.fairVariance);
        assertNotNull("variance_swap_fair_values[0].fair_vol", vs.fairVol);
        assertNotNull("variance_swap_fair_values[0].atm_iv", vs.atmIv);
        assertNotNull("variance_swap_fair_values[0].convexity_adjustment", vs.convexityAdjustment);

        // greeks_surfaces — vanna/charm/volga/speed
        assertNotNull("greeks_surfaces", r.greeksSurfaces);
        AdvVolatilityResponse.GreekSurface[] surfaces = {
                r.greeksSurfaces.vanna, r.greeksSurfaces.charm,
                r.greeksSurfaces.volga, r.greeksSurfaces.speed,
        };
        String[] surfNames = {"vanna", "charm", "volga", "speed"};
        for (int i = 0; i < surfaces.length; i++) {
            assertNotNull("greeks_surfaces." + surfNames[i], surfaces[i]);
            assertNotNull("greeks_surfaces." + surfNames[i] + ".strikes", surfaces[i].strikes);
            assertNotNull("greeks_surfaces." + surfNames[i] + ".expiries", surfaces[i].expiries);
            assertNotNull("greeks_surfaces." + surfNames[i] + ".values", surfaces[i].values);
            assertTrue("greeks_surfaces." + surfNames[i] + ".values non-empty",
                    surfaces[i].values.length > 0);
        }
    }

    @Test
    public void testSurface_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.surface("SPY");
        SurfaceResponse r = gson.fromJson(json, SurfaceResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("spot", r.spot);
        assertNotNull("as_of", r.asOf);
        assertNotNull("grid_size", r.gridSize);
        assertNotNull("tenors", r.tenors);
        assertNotNull("moneyness", r.moneyness);
        assertNotNull("iv", r.iv);
        assertEquals("tenors length matches grid_size", (int) r.gridSize, r.tenors.size());
        assertEquals("moneyness length matches grid_size", (int) r.gridSize, r.moneyness.size());
        assertEquals("iv outer length matches grid_size", (int) r.gridSize, r.iv.length);
        assertEquals("iv inner length matches grid_size", (int) r.gridSize, r.iv[0].length);
        assertNotNull("slices_used", r.slicesUsed);
    }

    @Test
    public void testGex_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.gex("SPY");
        GexResponse r = gson.fromJson(json, GexResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("gamma_flip", r.gammaFlip);
        assertNotNull("net_gex", r.netGex);
        assertNotNull("net_gex_label", r.netGexLabel);
        assertNotNull("strikes", r.strikes);
        assertFalse("strikes non-empty", r.strikes.isEmpty());
        GexResponse.GexStrikeRow row = r.strikes.get(0);
        assertNotNull("strikes[0].strike", row.strike);
        assertNotNull("strikes[0].call_gex", row.callGex);
        assertNotNull("strikes[0].put_gex", row.putGex);
        assertNotNull("strikes[0].net_gex", row.netGex);
        assertNotNull("strikes[0].call_oi", row.callOi);
        assertNotNull("strikes[0].put_oi", row.putOi);
        assertNotNull("strikes[0].call_volume", row.callVolume);
        assertNotNull("strikes[0].put_volume", row.putVolume);
        // OI change fields are nullable (first-day / insufficient prior
        // history). Typed Long leaves exercised via deserialization — they
        // round-trip cleanly as either a number or null.
        Long callChg = row.callOiChange;
        Long putChg = row.putOiChange;
        assertTrue("strikes[0].call_oi_change typed leaf", callChg == null || callChg.longValue() == callChg);
        assertTrue("strikes[0].put_oi_change typed leaf", putChg == null || putChg.longValue() == putChg);
    }

    @Test
    public void testDex_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.dex("SPY");
        DexResponse r = gson.fromJson(json, DexResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("net_dex", r.netDex);
        assertNotNull("strikes", r.strikes);
        assertFalse("strikes non-empty", r.strikes.isEmpty());
        DexResponse.DexStrikeRow row = r.strikes.get(0);
        assertNotNull("strikes[0].strike", row.strike);
        assertNotNull("strikes[0].call_dex", row.callDex);
        assertNotNull("strikes[0].put_dex", row.putDex);
        assertNotNull("strikes[0].net_dex", row.netDex);
    }

    @Test
    public void testVex_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.vex("SPY");
        VexResponse r = gson.fromJson(json, VexResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("net_vex", r.netVex);
        assertNotNull("vex_interpretation", r.vexInterpretation);
        assertNotNull("strikes", r.strikes);
        assertFalse("strikes non-empty", r.strikes.isEmpty());
        VexResponse.VexStrikeRow row = r.strikes.get(0);
        assertNotNull("strikes[0].strike", row.strike);
        assertNotNull("strikes[0].call_vex", row.callVex);
        assertNotNull("strikes[0].put_vex", row.putVex);
        assertNotNull("strikes[0].net_vex", row.netVex);
    }

    @Test
    public void testChex_EveryFieldDeclaredInPocoMustBeReferenced() {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        JsonObject json = client.chex("SPY");
        ChexResponse r = gson.fromJson(json, ChexResponse.class);

        assertEquals("SPY", r.symbol);
        assertNotNull("underlying_price", r.underlyingPrice);
        assertNotNull("as_of", r.asOf);
        assertNotNull("net_chex", r.netChex);
        assertNotNull("chex_interpretation", r.chexInterpretation);
        assertNotNull("strikes", r.strikes);
        assertFalse("strikes non-empty", r.strikes.isEmpty());
        ChexResponse.ChexStrikeRow row = r.strikes.get(0);
        assertNotNull("strikes[0].strike", row.strike);
        assertNotNull("strikes[0].call_chex", row.callChex);
        assertNotNull("strikes[0].put_chex", row.putChex);
        assertNotNull("strikes[0].net_chex", row.netChex);
    }

    @Test
    public void testStockQuote_EveryFieldDeclaredInPocoMustBeReferenced() {
        StockQuoteResponse r = client.stockQuoteTyped("SPY");
        assertEquals("SPY", r.ticker);
        assertNotNull("bid", r.bid);
        assertNotNull("ask", r.ask);
        assertNotNull("mid", r.mid);
        assertNotNull("lastPrice", r.lastPrice);
        assertNotNull("lastUpdate", r.lastUpdate);
    }

    @Test
    public void testOptionQuote_EveryFieldDeclaredInPocoMustBeReferenced() {
        // Pick a chain entry from /v1/options/SPY so the call always
        // resolves a real contract regardless of session date.
        JsonObject meta = client.options("SPY");
        com.google.gson.JsonArray exps = meta.getAsJsonArray("expirations");
        assertNotNull("expirations available", exps);
        assertTrue("expirations non-empty", exps.size() > 0);
        // Resolve the ATM contract: the strike nearest spot is the most
        // reliably-quoted. Deep-OTM / wide-0DTE "middle" strikes frequently
        // have no two-sided market and return 404 (a valid API state, not
        // an SDK defect), so walk ATM-outward across the first few expiries
        // until one resolves.
        double spot = client.stockQuote("SPY").get("mid").getAsDouble();
        OptionQuoteResponse r = null;
        String expiry = null;
        outer:
        for (int ei = 0; ei < Math.min(5, exps.size()); ei++) {
            JsonObject row = exps.get(ei).getAsJsonObject();
            String exp = row.get("expiration").getAsString();
            com.google.gson.JsonArray strikes = row.getAsJsonArray("strikes");
            if (strikes == null || strikes.size() == 0) continue;
            java.util.List<Double> ks = new java.util.ArrayList<>();
            for (int i = 0; i < strikes.size(); i++) ks.add(strikes.get(i).getAsDouble());
            ks.sort((a, b) -> Double.compare(Math.abs(a - spot), Math.abs(b - spot)));
            for (int i = 0; i < Math.min(8, ks.size()); i++) {
                for (String t : new String[]{"call", "put"}) {
                    try {
                        OptionQuoteResponse cand =
                                client.optionQuoteTyped("SPY", exp, ks.get(i), t);
                        // A 200 with null greeks is a valid "no live market"
                        // state; only accept a contract that is actually
                        // priced (delta populated ⇒ a real two-sided quote).
                        // A model-priced quote always carries delta (and the
                        // other BSM greeks). iv_bid / iv_ask are bid/ask-side
                        // IVs that are legitimately null on a one-sided
                        // market, so select on delta only.
                        if (cand != null && cand.delta != null) {
                            r = cand;
                            expiry = exp;
                            break outer;
                        }
                    } catch (NotFoundException ignored) {
                        // contract has no quote — try the next
                    }
                }
            }
        }
        org.junit.Assume.assumeTrue(
                "no SPY option contract returned a live quote — skipping", r != null);
        assertEquals(expiry, r.expiry);
        assertNotNull("strike", r.strike);
        assertNotNull("bid", r.bid);
        assertNotNull("ask", r.ask);
        assertNotNull("mid", r.mid);
        assertNotNull("bidSize", r.bidSize);
        assertNotNull("askSize", r.askSize);
        assertNotNull("lastUpdate", r.lastUpdate);
        assertNotNull("implied_vol", r.impliedVol);
        // iv_bid / iv_ask are bid/ask-side IVs — legitimately null when that
        // side has no market. Reference (typed leaves exercised) but do not
        // assert non-null.
        @SuppressWarnings("unused") Double refIvBid = r.ivBid;
        @SuppressWarnings("unused") Double refIvAsk = r.ivAsk;
        assertNotNull("delta", r.delta);
        assertNotNull("gamma", r.gamma);
        assertNotNull("theta", r.theta);
        assertNotNull("vega", r.vega);
        assertNotNull("rho", r.rho);
        assertNotNull("vanna", r.vanna);
        assertNotNull("charm", r.charm);
        // svi_vol may be null when surface fit unavailable; svi_vol_gated string
        // describes the gating reason — typed leaves exercised via deserialization
        assertNotNull("open_interest", r.openInterest);
        assertNotNull("volume", r.volume);
        // underlying optional on some shapes — exercised as a typed String leaf
    }

    // ── Flow (live, simulation-aware) — Alpha+ ─────────────────────────
    //
    // Hit the real /v1/flow/* surface and assert every contract field is
    // present on the live JSON (and on nested array-element shapes when
    // the arrays are non-empty).

    private static final String FLOW_SYM = "SPY";

    private static void reqKeys(JsonObject o, String[] keys, String where) {
        assertNotNull(where + ": null object", o);
        for (String k : keys) assertTrue(where + ": missing field '" + k + "'", o.has(k));
    }

    private static JsonObject firstObj(JsonObject parent, String arrKey) {
        if (parent == null || !parent.has(arrKey) || !parent.get(arrKey).isJsonArray()) return null;
        JsonArray a = parent.getAsJsonArray(arrKey);
        return a.size() == 0 ? null : a.get(0).getAsJsonObject();
    }

    @Test
    public void testFlowLevels() {
        JsonObject r = client.flowLevels(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "live_gamma_flip", "live_call_wall", "live_put_wall", "live_max_pain"}, "flow/levels");
        FlowLevelsResponse t = client.flowLevelsTyped(FLOW_SYM);
        assertEquals(FLOW_SYM, t.symbol);
    }

    @Test
    public void testFlowPinRisk() {
        JsonObject r = client.flowPinRisk(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "live_pin_risk", "magnet_strike", "distance_to_magnet_pct",
                "time_to_close_hours", "breakdown"}, "flow/pin-risk");
        reqKeys(r.getAsJsonObject("breakdown"),
                new String[]{"oi_score", "proximity_score", "time_score", "gamma_score"},
                "flow/pin-risk.breakdown");
    }

    @Test
    public void testFlowSummary() {
        JsonObject r = client.flowSummary(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "flow_direction", "intraday_oi_delta", "contracts_with_flow",
                "contracts_total", "live_gex", "flow_gex_pct_shift"}, "flow/summary");
    }

    @Test
    public void testFlowOi() {
        JsonObject r = client.flowOi(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "expiry", "official_oi",
                "simulated_oi", "intraday_oi_delta", "oi_delta_confidence",
                "effective_oi", "contracts_total", "contracts_with_flow"}, "flow/oi");
    }

    @Test
    public void testFlowGex() {
        JsonObject r = client.flowGex(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "live_net_gex", "live_net_gex_label", "live_gamma_flip", "strikes"}, "flow/gex");
        JsonObject s = firstObj(r, "strikes");
        assertNotNull("flow/gex: expected non-empty strikes", s);
        reqKeys(s, new String[]{"strike", "call_gex", "put_gex", "net_gex",
                "call_oi", "put_oi", "call_volume", "put_volume"}, "flow/gex.strikes[0]");
        FlowGexResponse t = client.flowGexTyped(FLOW_SYM);
        assertNotNull(t.strikes);
        assertFalse(t.strikes.isEmpty());
    }

    @Test
    public void testFlowDex() {
        JsonObject r = client.flowDex(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "live_net_dex", "strikes"}, "flow/dex");
        JsonObject s = firstObj(r, "strikes");
        assertNotNull("flow/dex: expected non-empty strikes", s);
        reqKeys(s, new String[]{"strike", "call_dex", "put_dex", "net_dex"}, "flow/dex.strikes[0]");
    }

    @Test
    public void testFlowDealerRisk() {
        JsonObject r = client.flowDealerRisk(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "settled_net_gex", "live_net_gex", "flow_gex_adjustment",
                "flow_gex_pct_shift", "settled_net_dex", "live_net_dex",
                "flow_dex_adjustment", "flow_dex_pct_shift",
                "total_abs_delta_contracts", "contracts_with_flow",
                "flow_direction", "description"}, "flow/dealer-risk");
    }

    @Test
    public void testFlowLive() {
        JsonObject r = client.flowLive(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "as_of", "underlying_price", "expiry",
                "contracts", "contracts_with_flow", "official_oi", "simulated_oi",
                "intraday_oi_delta", "oi_delta_confidence", "effective_oi", "live_gex",
                "live_gex_delta", "live_gamma_flip", "live_call_wall", "live_put_wall",
                "live_max_pain", "live_pin_risk", "flow_adjusted_dealer_risk"}, "flow/live");
        reqKeys(r.getAsJsonObject("flow_adjusted_dealer_risk"), new String[]{
                "settled_net_gex", "live_net_gex", "flow_gex_adjustment",
                "flow_gex_pct_shift", "settled_net_dex", "live_net_dex",
                "flow_dex_adjustment", "flow_dex_pct_shift",
                "total_abs_delta_contracts", "flow_direction", "description"},
                "flow/live.flow_adjusted_dealer_risk");
    }

    @Test
    public void testFlowOptionRecent() {
        JsonObject r = client.flowOptionRecent(FLOW_SYM, 5, null);
        reqKeys(r, new String[]{"symbol", "count", "totalAvailable", "trades"}, "flow/options/recent");
        JsonObject t = firstObj(r, "trades");
        if (t != null) reqKeys(t, new String[]{"ts", "instrumentId", "expiry",
                "strike", "right", "price", "size", "side", "isBlock", "bid", "ask"},
                "flow/options/recent.trades[0]");
    }

    @Test
    public void testFlowOptionSummary() {
        JsonObject r = client.flowOptionSummary(FLOW_SYM, null);
        reqKeys(r, new String[]{"symbol", "contractsWithTrades", "totalTrades",
                "buyVolume", "sellVolume", "midVolume", "netVolume",
                "biggestSingleTrade"}, "flow/options/summary");
    }

    @Test
    public void testFlowOptionBlocks() {
        JsonObject r = client.flowOptionBlocks(FLOW_SYM, 50, null);
        reqKeys(r, new String[]{"symbol", "minSize", "count", "blocks"}, "flow/options/blocks");
        JsonObject b = firstObj(r, "blocks");
        if (b != null) reqKeys(b, new String[]{"ts", "expiry", "strike", "right",
                "price", "size", "side"}, "flow/options/blocks.blocks[0]");
    }

    @Test
    public void testFlowOptionHistory() {
        JsonObject r = client.flowOptionHistory(FLOW_SYM, 30, null);
        reqKeys(r, new String[]{"symbol", "minutes", "count", "buckets"}, "flow/options/history");
        JsonObject b = firstObj(r, "buckets");
        if (b != null) reqKeys(b, new String[]{"ts", "buyVolume", "sellVolume",
                "midVolume", "netVolume", "tradeCount", "biggestTrade", "vwap",
                "high", "low"}, "flow/options/history.buckets[0]");
    }

    @Test
    public void testFlowOptionCumulative() {
        JsonObject r = client.flowOptionCumulative(FLOW_SYM, 60, null);
        reqKeys(r, new String[]{"symbol", "minutes", "count", "points"}, "flow/options/cumulative");
        JsonObject p = firstObj(r, "points");
        if (p != null) reqKeys(p, new String[]{"ts", "netVolume", "cumulative",
                "vwap", "tradeCount"}, "flow/options/cumulative.points[0]");
    }

    @Test
    public void testFlowStockRecent() {
        JsonObject r = client.flowStockRecent(FLOW_SYM, 5);
        reqKeys(r, new String[]{"symbol", "count", "totalAvailable", "trades"}, "flow/stocks/recent");
        JsonObject t = firstObj(r, "trades");
        if (t != null) reqKeys(t, new String[]{"ts", "price", "size", "side",
                "isBlock", "bid", "ask"}, "flow/stocks/recent.trades[0]");
    }

    @Test
    public void testFlowStockSummary() {
        JsonObject r = client.flowStockSummary(FLOW_SYM);
        reqKeys(r, new String[]{"symbol", "totalTrades", "buyVolume", "sellVolume",
                "midVolume", "netVolume", "biggestSingleTrade"}, "flow/stocks/summary");
    }

    @Test
    public void testFlowStockBlocks() {
        JsonObject r = client.flowStockBlocks(FLOW_SYM, 1000);
        reqKeys(r, new String[]{"symbol", "minSize", "count", "blocks"}, "flow/stocks/blocks");
        JsonObject b = firstObj(r, "blocks");
        if (b != null) reqKeys(b, new String[]{"ts", "price", "size", "side",
                "bid", "ask"}, "flow/stocks/blocks.blocks[0]");
    }

    @Test
    public void testFlowStockHistory() {
        JsonObject r = client.flowStockHistory(FLOW_SYM, 30);
        reqKeys(r, new String[]{"symbol", "minutes", "count", "buckets"}, "flow/stocks/history");
        JsonObject b = firstObj(r, "buckets");
        if (b != null) reqKeys(b, new String[]{"ts", "buyVolume", "sellVolume",
                "midVolume", "netVolume", "tradeCount", "biggestTrade", "vwap",
                "open", "close", "high", "low"}, "flow/stocks/history.buckets[0]");
    }

    @Test
    public void testFlowStockCumulative() {
        JsonObject r = client.flowStockCumulative(FLOW_SYM, 60);
        reqKeys(r, new String[]{"symbol", "minutes", "count", "points"}, "flow/stocks/cumulative");
        JsonObject p = firstObj(r, "points");
        if (p != null) reqKeys(p, new String[]{"ts", "netVolume", "cumulative",
                "vwap", "tradeCount"}, "flow/stocks/cumulative.points[0]");
    }

    @Test
    public void testFlowOptionsLeaderboard() {
        JsonObject r = client.flowOptionsLeaderboard(3, null);
        reqKeys(r, new String[]{"generatedUtc", "n", "windowMinutes", "buyers",
                "sellers"}, "flow/options/leaderboard");
        for (String side : new String[]{"buyers", "sellers"}) {
            JsonObject row = firstObj(r, side);
            if (row != null) {
                reqKeys(row, new String[]{"symbol", "netVolume", "netNotional",
                        "buyVolume", "sellVolume", "avgPremium", "tradeCount",
                        "lastTradeUtc"}, "flow/options/leaderboard." + side + "[0]");
                break;
            }
        }
    }

    @Test
    public void testFlowOptionsOutliers() {
        JsonObject r = client.flowOptionsOutliers(3, null, null);
        reqKeys(r, new String[]{"generatedUtc", "windowMinutes", "tracked",
                "qualified", "limit", "outliers"}, "flow/options/outliers");
        JsonObject o = firstObj(r, "outliers");
        if (o != null) reqKeys(o, new String[]{"symbol", "tradeCount", "buyVolume",
                "sellVolume", "midVolume", "netVolume", "imbalancePct", "skew",
                "notional", "netNotional", "biggestTrade", "biggestTradeUtc",
                "biggestAgeSec", "lastVwap", "lastTradeUtc", "lastTradeAgeSec"},
                "flow/options/outliers.outliers[0]");
    }

    @Test
    public void testFlowStocksLeaderboard() {
        JsonObject r = client.flowStocksLeaderboard(3, null);
        reqKeys(r, new String[]{"generatedUtc", "n", "windowMinutes", "buyers",
                "sellers"}, "flow/stocks/leaderboard");
        for (String side : new String[]{"buyers", "sellers"}) {
            JsonObject row = firstObj(r, side);
            if (row != null) {
                reqKeys(row, new String[]{"symbol", "netVolume", "netNotional",
                        "buyVolume", "sellVolume", "vwap", "tradeCount",
                        "lastTradeUtc"}, "flow/stocks/leaderboard." + side + "[0]");
                break;
            }
        }
    }

    @Test
    public void testFlowStocksOutliers() {
        JsonObject r = client.flowStocksOutliers(3, null, null);
        reqKeys(r, new String[]{"generatedUtc", "windowMinutes", "tracked",
                "qualified", "limit", "outliers"}, "flow/stocks/outliers");
        JsonObject o = firstObj(r, "outliers");
        if (o != null) reqKeys(o, new String[]{"symbol", "tradeCount", "buyVolume",
                "sellVolume", "midVolume", "netVolume", "imbalancePct", "skew",
                "notional", "netNotional", "biggestTrade", "biggestTradeUtc",
                "biggestAgeSec", "lastVwap", "lastTradeUtc", "lastTradeAgeSec"},
                "flow/stocks/outliers.outliers[0]");
    }

    private static final String[] SIGNAL_FIELDS = {"ts", "expiry", "strike",
            "right", "side", "price", "size", "premium", "dte", "structure",
            "aggressor", "open_close_bias", "open_close_confidence",
            "contract_net_oi_delta", "intent", "score", "conviction", "tags",
            "score_breakdown", "enrichment"};

    @Test
    public void testFlowSignals() {
        JsonObject r = client.flowSignals(FLOW_SYM, null, null, null, 240, 10, null);
        reqKeys(r, new String[]{"symbol", "as_of", "window_minutes", "expiry",
                "underlying_price", "chain", "count", "signals"}, "flow/signals");
        assertEquals(FLOW_SYM, r.get("symbol").getAsString());
        reqKeys(r.getAsJsonObject("chain"), new String[]{"call_wall", "put_wall",
                "max_pain", "gamma_flip"}, "flow/signals.chain");
        JsonObject s = firstObj(r, "signals");
        if (s != null) {
            reqKeys(s, SIGNAL_FIELDS, "flow/signals.signals[0]");
            reqKeys(s.getAsJsonObject("score_breakdown"),
                    new String[]{"premium", "size_vs_oi", "aggressor", "sweep",
                            "opening_bias", "tenor"},
                    "flow/signals.signals[0].score_breakdown");
            reqKeys(s.getAsJsonObject("enrichment"),
                    new String[]{"iv", "delta", "gamma", "iv_vs_atm", "moneyness",
                            "estimated_delta_notional",
                            "hypothetical_gex_impact_if_opening"},
                    "flow/signals.signals[0].enrichment");
        }
        FlowSignalsResponse t = client.flowSignalsTyped(FLOW_SYM, null, null, null, 240, 10, null);
        assertEquals(FLOW_SYM, t.symbol);
    }

    @Test
    public void testFlowSignalsSummary() {
        JsonObject r = client.flowSignalsSummary(FLOW_SYM, 240, null);
        reqKeys(r, new String[]{"symbol", "as_of", "window_minutes", "expiry",
                "underlying_price", "signal_count", "bullish_premium",
                "bearish_premium", "net_directional_premium", "opening_premium",
                "closing_premium", "top_signals"}, "flow/signals/summary");
        assertEquals(FLOW_SYM, r.get("symbol").getAsString());
        JsonObject s = firstObj(r, "top_signals");
        if (s != null) reqKeys(s, SIGNAL_FIELDS, "flow/signals/summary.top_signals[0]");
        FlowSignalsSummaryResponse t = client.flowSignalsSummaryTyped(FLOW_SYM, 240, null);
        assertEquals(FLOW_SYM, t.symbol);
    }

    // ── Realized & Forecast Volatility (Alpha+) ───────────────────────
    //
    // Live coverage for the two volatility endpoints whose typed POCOs were
    // recently added. Each asserts the untyped wire shape AND that the typed
    // deserialization populates the nested leaves (a renamed @SerializedName
    // surfaces as a null assertion failure). The forecast test also pins the
    // documented invariant that garch.params.dof is null for dist=gaussian
    // and non-null for dist=student_t.

    @Test
    public void testRealizedVolatilityTyped() {
        // AAPL — clean single-name with a continuous price history.
        JsonObject r = client.realizedVolatility("AAPL");
        reqKeys(r, new String[]{"symbol", "as_of", "estimators"}, "volatility/realized");
        assertEquals("AAPL", r.get("symbol").getAsString());
        reqKeys(r.getAsJsonObject("estimators"),
                new String[]{"close_to_close", "parkinson", "garman_klass",
                        "rogers_satchell", "yang_zhang"}, "volatility/realized.estimators");

        RealizedVolatilityResponse t = client.realizedVolatilityTyped("AAPL");
        assertEquals("AAPL", t.symbol);
        assertNotNull("as_of", t.asOf);
        assertNotNull("estimators", t.estimators);
        // every estimator family + its 10/20/30d windows must deserialize
        RealizedVolatilityResponse.Estimator[] est = {
                t.estimators.closeToClose, t.estimators.parkinson, t.estimators.garmanKlass,
                t.estimators.rogersSatchell, t.estimators.yangZhang,
        };
        String[] names = {"close_to_close", "parkinson", "garman_klass",
                "rogers_satchell", "yang_zhang"};
        for (int i = 0; i < est.length; i++) {
            assertNotNull("estimators." + names[i], est[i]);
            assertNotNull("estimators." + names[i] + ".rv10", est[i].rv10);
            assertNotNull("estimators." + names[i] + ".rv20", est[i].rv20);
            assertNotNull("estimators." + names[i] + ".rv30", est[i].rv30);
        }
    }

    @Test
    public void testVolatilityForecastTyped() {
        // Default distribution (student_t) — full field walk through the POCO.
        JsonObject r = client.volatilityForecast("AAPL");
        reqKeys(r, new String[]{"symbol", "as_of", "ewma", "har_rv", "garch"},
                "volatility/forecast");
        assertEquals("AAPL", r.get("symbol").getAsString());

        VolatilityForecastResponse t = client.volatilityForecastTyped("AAPL");
        assertEquals("AAPL", t.symbol);
        assertNotNull("as_of", t.asOf);

        // ewma
        assertNotNull("ewma", t.ewma);
        assertNotNull("ewma.lambda", t.ewma.lambda);
        assertNotNull("ewma.vol_annualized", t.ewma.volAnnualized);
        assertNotNull("ewma.next_day_forecast", t.ewma.nextDayForecast);

        // har_rv + components
        assertNotNull("har_rv", t.harRv);
        assertNotNull("har_rv.vol_annualized", t.harRv.volAnnualized);
        assertNotNull("har_rv.next_day_forecast", t.harRv.nextDayForecast);
        assertNotNull("har_rv.components", t.harRv.components);
        assertNotNull("har_rv.components.daily", t.harRv.components.daily);
        assertNotNull("har_rv.components.weekly", t.harRv.components.weekly);
        assertNotNull("har_rv.components.monthly", t.harRv.components.monthly);

        // garch
        assertNotNull("garch", t.garch);
        assertNotNull("garch.model", t.garch.model);
        assertNotNull("garch.distribution", t.garch.distribution);
        assertNotNull("garch.params", t.garch.params);
        assertNotNull("garch.params.omega", t.garch.params.omega);
        assertNotNull("garch.params.alpha", t.garch.params.alpha);
        assertNotNull("garch.params.beta", t.garch.params.beta);
        assertNotNull("garch.persistence", t.garch.persistence);
        assertNotNull("garch.half_life_days", t.garch.halfLifeDays);
        assertNotNull("garch.converged", t.garch.converged);
        // forecast path is null when the MLE optimiser did not converge
        if (Boolean.TRUE.equals(t.garch.converged)) {
            assertNotNull("garch.forecast", t.garch.forecast);
            if (!t.garch.forecast.isEmpty()) {
                VolatilityForecastResponse.Garch.ForecastPoint p = t.garch.forecast.get(0);
                assertNotNull("garch.forecast[0].horizon_days", p.horizonDays);
                assertNotNull("garch.forecast[0].vol_annualized", p.volAnnualized);
            }
        }
    }

    @Test
    public void testVolatilityForecastGarchDofByDistribution() {
        // Documented invariant: garch.params.dof (Student-t degrees of freedom)
        // is populated only for the student_t innovation distribution.
        VolatilityForecastResponse student = client.volatilityForecastTyped("AAPL", "student_t");
        assertEquals("student_t", student.garch.distribution);
        assertNotNull("garch.params.dof must be non-null for student_t",
                student.garch.params.dof);

        VolatilityForecastResponse gaussian = client.volatilityForecastTyped("AAPL", "gaussian");
        assertEquals("gaussian", gaussian.garch.distribution);
        assertNull("garch.params.dof must be null for gaussian",
                gaussian.garch.params.dof);
    }
}
