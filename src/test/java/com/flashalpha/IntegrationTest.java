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
    public void testExposureSummaryNetGexUnderExposures() {
        JsonObject r = client.exposureSummary("SPY");
        assertNull("net_gex must NOT be top-level on exposure_summary (customer trap)",
                r.get("net_gex"));
        assertTrue("exposures block missing", r.has("exposures"));
        JsonObject exp = r.getAsJsonObject("exposures");
        assertTrue("exposures.net_gex missing", exp.has("net_gex"));
        assertTrue(exp.get("net_gex").getAsJsonPrimitive().isNumber());
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
