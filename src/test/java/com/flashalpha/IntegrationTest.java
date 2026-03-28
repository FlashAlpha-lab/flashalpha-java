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
}
