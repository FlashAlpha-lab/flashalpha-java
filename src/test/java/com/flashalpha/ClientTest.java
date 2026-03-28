package com.flashalpha;

import com.google.gson.JsonObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FlashAlphaClient} using OkHttp MockWebServer.
 */
public class ClientTest {

    private MockWebServer server;
    private FlashAlphaClient client;

    private static final String FAKE_KEY = "test-key-123";
    private static final String OK_JSON = "{\"status\":\"ok\"}";

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new FlashAlphaClient(FAKE_KEY, server.url("/").toString());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    // ── Helper ────────────────────────────────────────────────────────

    private void enqueue(int code, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private void enqueueOk() {
        enqueue(200, OK_JSON);
    }

    // ── API key header ─────────────────────────────────────────────────

    @Test
    public void testApiKeyHeaderIsSent() throws Exception {
        enqueueOk();
        client.health();
        RecordedRequest req = server.takeRequest();
        assertEquals(FAKE_KEY, req.getHeader("X-Api-Key"));
    }

    // ── System endpoints ───────────────────────────────────────────────

    @Test
    public void testHealth() throws Exception {
        enqueueOk();
        JsonObject result = client.health();
        assertEquals("ok", result.get("status").getAsString());
        RecordedRequest req = server.takeRequest();
        assertEquals("/health", req.getPath());
    }

    @Test
    public void testAccount() throws Exception {
        enqueueOk();
        client.account();
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/account", req.getPath());
    }

    // ── Market data ────────────────────────────────────────────────────

    @Test
    public void testStockQuote() throws Exception {
        enqueueOk();
        client.stockQuote("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/stockquote/SPY", req.getPath());
    }

    @Test
    public void testOptionQuoteNoParams() throws Exception {
        enqueueOk();
        client.optionQuote("AAPL");
        RecordedRequest req = server.takeRequest();
        assertEquals("/optionquote/AAPL", req.getPath());
    }

    @Test
    public void testOptionQuoteWithParams() throws Exception {
        enqueueOk();
        client.optionQuote("AAPL", "2024-01-19", 150.0, "call");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/optionquote/AAPL"));
        assertTrue(path.contains("expiry=2024-01-19"));
        assertTrue(path.contains("strike=150.0"));
        assertTrue(path.contains("type=call"));
    }

    @Test
    public void testSurface() throws Exception {
        enqueueOk();
        client.surface("SPX");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/surface/SPX", req.getPath());
    }

    @Test
    public void testStockSummary() throws Exception {
        enqueueOk();
        client.stockSummary("TSLA");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/stock/TSLA/summary", req.getPath());
    }

    // ── Historical ─────────────────────────────────────────────────────

    @Test
    public void testHistoricalStockQuoteWithDate() throws Exception {
        enqueueOk();
        client.historicalStockQuote("SPY", "2024-01-10", null);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/historical/stockquote/SPY"));
        assertTrue(path.contains("date=2024-01-10"));
    }

    @Test
    public void testHistoricalOptionQuote() throws Exception {
        enqueueOk();
        client.historicalOptionQuote("SPY", "2024-01-10", "14:30", "2024-01-19", 450.0, "put");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.contains("date=2024-01-10"));
        assertTrue(path.contains("time=14%3A30"));
        assertTrue(path.contains("expiry=2024-01-19"));
        assertTrue(path.contains("type=put"));
    }

    // ── Exposure analytics ─────────────────────────────────────────────

    @Test
    public void testGexNoParams() throws Exception {
        enqueueOk();
        client.gex("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/gex/SPY", req.getPath());
    }

    @Test
    public void testGexWithParams() throws Exception {
        enqueueOk();
        client.gex("SPY", "2024-01-19", 100);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.contains("expiration=2024-01-19"));
        assertTrue(path.contains("min_oi=100"));
    }

    @Test
    public void testDex() throws Exception {
        enqueueOk();
        client.dex("QQQ");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/dex/QQQ", req.getPath());
    }

    @Test
    public void testDexWithExpiration() throws Exception {
        enqueueOk();
        client.dex("QQQ", "2024-01-19");
        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("expiration=2024-01-19"));
    }

    @Test
    public void testVex() throws Exception {
        enqueueOk();
        client.vex("SPX");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/vex/SPX", req.getPath());
    }

    @Test
    public void testChex() throws Exception {
        enqueueOk();
        client.chex("IWM");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/chex/IWM", req.getPath());
    }

    @Test
    public void testExposureLevels() throws Exception {
        enqueueOk();
        client.exposureLevels("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/levels/SPY", req.getPath());
    }

    @Test
    public void testExposureSummary() throws Exception {
        enqueueOk();
        client.exposureSummary("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/summary/SPY", req.getPath());
    }

    @Test
    public void testNarrative() throws Exception {
        enqueueOk();
        client.narrative("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/narrative/SPY", req.getPath());
    }

    @Test
    public void testZeroDteNoParams() throws Exception {
        enqueueOk();
        client.zeroDte("SPX");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/zero-dte/SPX", req.getPath());
    }

    @Test
    public void testZeroDteWithStrikeRange() throws Exception {
        enqueueOk();
        client.zeroDte("SPX", 0.05);
        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("strike_range=0.05"));
    }

    @Test
    public void testExposureHistoryNoParams() throws Exception {
        enqueueOk();
        client.exposureHistory("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/exposure/history/SPY", req.getPath());
    }

    @Test
    public void testExposureHistoryWithDays() throws Exception {
        enqueueOk();
        client.exposureHistory("SPY", 30);
        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("days=30"));
    }

    // ── Pricing ────────────────────────────────────────────────────────

    @Test
    public void testGreeks() throws Exception {
        enqueueOk();
        client.greeks(450.0, 455.0, 5.0, 0.20, "call", null, null);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/pricing/greeks"));
        assertTrue(path.contains("spot=450.0"));
        assertTrue(path.contains("sigma=0.2"));
        assertTrue(path.contains("type=call"));
    }

    @Test
    public void testGreeksWithRAndQ() throws Exception {
        enqueueOk();
        client.greeks(450.0, 455.0, 5.0, 0.20, "put", 0.05, 0.01);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.contains("r=0.05"));
        assertTrue(path.contains("q=0.01"));
    }

    @Test
    public void testIv() throws Exception {
        enqueueOk();
        client.iv(450.0, 455.0, 5.0, 3.50, "call", null, null);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/pricing/iv"));
        assertTrue(path.contains("price=3.5"));
    }

    @Test
    public void testKelly() throws Exception {
        enqueueOk();
        client.kelly(450.0, 455.0, 5.0, 0.20, 2.50, 0.10, "call", null, null);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/pricing/kelly"));
        assertTrue(path.contains("premium=2.5"));
        assertTrue(path.contains("mu=0.1"));
    }

    // ── Volatility ─────────────────────────────────────────────────────

    @Test
    public void testVolatility() throws Exception {
        enqueueOk();
        client.volatility("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/volatility/SPY", req.getPath());
    }

    @Test
    public void testAdvVolatility() throws Exception {
        enqueueOk();
        client.advVolatility("SPX");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/adv_volatility/SPX", req.getPath());
    }

    // ── Reference data ─────────────────────────────────────────────────

    @Test
    public void testTickers() throws Exception {
        enqueueOk();
        client.tickers();
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/tickers", req.getPath());
    }

    @Test
    public void testOptions() throws Exception {
        enqueueOk();
        client.options("AAPL");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/options/AAPL", req.getPath());
    }

    @Test
    public void testSymbols() throws Exception {
        enqueueOk();
        client.symbols();
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/symbols", req.getPath());
    }

    // ── Error handling ─────────────────────────────────────────────────

    @Test
    public void testAuthenticationException() {
        enqueue(401, "{\"detail\":\"Invalid API key\"}");
        try {
            client.health();
            fail("Expected AuthenticationException");
        } catch (AuthenticationException e) {
            assertEquals(401, e.getStatusCode());
            assertTrue(e.getMessage().contains("Invalid API key"));
        }
    }

    @Test
    public void testTierRestrictedException() {
        enqueue(403, "{\"detail\":\"Upgrade required\",\"current_plan\":\"free\",\"required_plan\":\"growth\"}");
        try {
            client.narrative("SPY");
            fail("Expected TierRestrictedException");
        } catch (TierRestrictedException e) {
            assertEquals(403, e.getStatusCode());
            assertEquals("free", e.getCurrentPlan());
            assertEquals("growth", e.getRequiredPlan());
        }
    }

    @Test
    public void testNotFoundException() {
        enqueue(404, "{\"detail\":\"Symbol not found\"}");
        try {
            client.gex("INVALID");
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
            assertEquals(404, e.getStatusCode());
        }
    }

    @Test
    public void testRateLimitException() {
        server.enqueue(new MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setHeader("Retry-After", "60")
                .setBody("{\"detail\":\"Rate limit exceeded\"}"));
        try {
            client.gex("SPY");
            fail("Expected RateLimitException");
        } catch (RateLimitException e) {
            assertEquals(429, e.getStatusCode());
            assertEquals(Integer.valueOf(60), e.getRetryAfter());
        }
    }

    @Test
    public void testServerException() {
        enqueue(500, "{\"detail\":\"Internal server error\"}");
        try {
            client.health();
            fail("Expected ServerException");
        } catch (ServerException e) {
            assertEquals(500, e.getStatusCode());
        }
    }

    @Test
    public void testServerExceptionNonJson() {
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "text/plain")
                .setBody("Service Unavailable"));
        try {
            client.health();
            fail("Expected ServerException");
        } catch (ServerException e) {
            assertEquals(503, e.getStatusCode());
            assertEquals("Service Unavailable", e.getMessage());
        }
    }

    // ── Constructor validation ─────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void testNullApiKeyThrows() {
        new FlashAlphaClient(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyApiKeyThrows() {
        new FlashAlphaClient("");
    }

    // ── Response parsing ───────────────────────────────────────────────

    @Test
    public void testResponseIsCorrectlyParsed() throws Exception {
        enqueue(200, "{\"gex\":12345.67,\"symbol\":\"SPY\"}");
        JsonObject result = client.gex("SPY");
        assertEquals("SPY", result.get("symbol").getAsString());
        assertEquals(12345.67, result.get("gex").getAsDouble(), 0.001);
    }
}
