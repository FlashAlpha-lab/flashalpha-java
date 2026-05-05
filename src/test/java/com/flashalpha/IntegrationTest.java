package com.flashalpha;

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

        if (r.has("no_zero_dte") && r.get("no_zero_dte").getAsBoolean()) {
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
                java.util.Arrays.asList("positive_gamma", "negative_gamma", "neutral", "undetermined")
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
}
