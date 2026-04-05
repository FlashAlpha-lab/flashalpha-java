package com.flashalpha;

import com.google.gson.JsonObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

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
}
