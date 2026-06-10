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

    // ── Max Pain ───────────────────────────────────────────────────────

    @Test
    public void testMaxPain() throws Exception {
        enqueueOk();
        client.maxPain("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("GET", req.getMethod());
        assertEquals("/v1/maxpain/SPY", req.getPath());
    }

    @Test
    public void testMaxPainWithExpiration() throws Exception {
        enqueueOk();
        client.maxPain("SPY", "2026-04-17");
        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("expiration=2026-04-17"));
    }

    @Test
    public void testMaxPainWithoutExpiration() throws Exception {
        enqueueOk();
        client.maxPain("SPY");
        RecordedRequest req = server.takeRequest();
        assertFalse(req.getPath().contains("expiration"));
    }

    @Test
    public void testMaxPainParsesResponse() throws Exception {
        enqueue(200, "{\"max_pain_strike\":545,\"pin_probability\":68,\"dealer_alignment\":{\"alignment\":\"converging\"}}");
        JsonObject result = client.maxPain("SPY");
        assertEquals(545, result.get("max_pain_strike").getAsInt());
        assertEquals(68, result.get("pin_probability").getAsInt());
        assertEquals("converging", result.getAsJsonObject("dealer_alignment").get("alignment").getAsString());
    }

    @Test
    public void testMaxPainThrows403() throws Exception {
        enqueue(403, "{\"status\":\"ERROR\",\"error\":\"tier_restricted\",\"message\":\"Requires Growth.\",\"current_plan\":\"Free\",\"required_plan\":\"Growth\"}");
        try {
            client.maxPain("SPY");
            fail("expected TierRestrictedException");
        } catch (TierRestrictedException e) {
            assertEquals("Free", e.getCurrentPlan());
        }
    }

    // ── Screener ───────────────────────────────────────────────────────

    @Test
    public void testScreenerEmpty() throws Exception {
        enqueueOk();
        client.screener(new java.util.LinkedHashMap<>());
        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/v1/screener", req.getPath());
        assertEquals("application/json", req.getHeader("Content-Type"));
    }

    @Test
    public void testScreenerWithFilters() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> filters = new java.util.LinkedHashMap<>();
        filters.put("op", "and");
        filters.put("conditions", java.util.List.of(
                java.util.Map.of("field", "regime", "operator", "eq", "value", "positive_gamma"),
                java.util.Map.of("field", "harvest_score", "operator", "gte", "value", 65)));

        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", filters);
        body.put("sort", java.util.List.of(
                java.util.Map.of("field", "harvest_score", "direction", "desc")));
        body.put("select", java.util.List.of("symbol", "price", "harvest_score"));
        body.put("limit", 20);

        client.screener(body);
        RecordedRequest req = server.takeRequest();
        String bodyStr = req.getBody().readUtf8();
        assertTrue(bodyStr.contains("\"op\":\"and\""));
        assertTrue(bodyStr.contains("positive_gamma"));
        assertTrue(bodyStr.contains("\"limit\":20"));
    }

    @Test
    public void testScreenerXApiKeyHeaderSent() throws Exception {
        enqueueOk();
        client.screener(new java.util.LinkedHashMap<>());
        RecordedRequest req = server.takeRequest();
        assertEquals(FAKE_KEY, req.getHeader("X-Api-Key"));
        assertEquals("application/json", req.getHeader("Content-Type"));
    }

    @Test
    public void testScreenerLeafFilter() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "regime", "operator", "eq", "value", "positive_gamma"));
        client.screener(body);
        RecordedRequest req = server.takeRequest();
        String s = req.getBody().readUtf8();
        assertTrue(s.contains("\"field\":\"regime\""));
        assertTrue(s.contains("\"operator\":\"eq\""));
    }

    @Test
    public void testScreenerOrGroup() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "op", "or",
                "conditions", java.util.List.of(
                        java.util.Map.of("field", "vrp_regime", "operator", "eq", "value", "toxic_short_vol"),
                        java.util.Map.of("field", "vrp_regime", "operator", "eq", "value", "event_only"))));
        client.screener(body);
        RecordedRequest req = server.takeRequest();
        String s = req.getBody().readUtf8();
        assertTrue(s.contains("\"op\":\"or\""));
        assertTrue(s.contains("toxic_short_vol"));
    }

    @Test
    public void testScreenerNestedAndInsideOr() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> andGroup1 = java.util.Map.of(
                "op", "and",
                "conditions", java.util.List.of(
                        java.util.Map.of("field", "regime", "operator", "eq", "value", "positive_gamma"),
                        java.util.Map.of("field", "harvest_score", "operator", "gte", "value", 70)));
        java.util.Map<String, Object> andGroup2 = java.util.Map.of(
                "op", "and",
                "conditions", java.util.List.of(
                        java.util.Map.of("field", "regime", "operator", "eq", "value", "negative_gamma"),
                        java.util.Map.of("field", "atm_iv", "operator", "gte", "value", 50)));
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "op", "or",
                "conditions", java.util.List.of(andGroup1, andGroup2)));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"op\":\"or\""));
        assertTrue(s.contains("\"op\":\"and\""));
        assertTrue(s.contains("positive_gamma"));
        assertTrue(s.contains("negative_gamma"));
    }

    @Test
    public void testScreenerBetweenOperator() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "atm_iv", "operator", "between", "value", java.util.List.of(15, 25)));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"operator\":\"between\""));
        assertTrue(s.contains("[15,25]"));
    }

    @Test
    public void testScreenerInOperator() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "term_state", "operator", "in",
                "value", java.util.List.of("contango", "mixed")));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"operator\":\"in\""));
        assertTrue(s.contains("contango"));
    }

    @Test
    public void testScreenerIsNotNullOperator() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of("field", "vrp_regime", "operator", "is_not_null"));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"operator\":\"is_not_null\""));
    }

    @Test
    public void testScreenerCascadingFilters() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "op", "and",
                "conditions", java.util.List.of(
                        java.util.Map.of("field", "regime", "operator", "eq", "value", "positive_gamma"),
                        java.util.Map.of("field", "expiries.days_to_expiry", "operator", "lte", "value", 14),
                        java.util.Map.of("field", "strikes.call_oi", "operator", "gte", "value", 2000),
                        java.util.Map.of("field", "contracts.type", "operator", "eq", "value", "C"))));
        body.put("select", java.util.List.of("*"));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("expiries.days_to_expiry"));
        assertTrue(s.contains("strikes.call_oi"));
        assertTrue(s.contains("contracts.type"));
    }

    @Test
    public void testScreenerFormulas() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("formulas", java.util.List.of(
                java.util.Map.of("alias", "vrp_ratio", "expression", "atm_iv / rv_20d")));
        body.put("filters", java.util.Map.of(
                "formula", "vrp_ratio", "operator", "gte", "value", 1.2));
        body.put("sort", java.util.List.of(
                java.util.Map.of("formula", "vrp_ratio", "direction", "desc")));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"alias\":\"vrp_ratio\""));
        assertTrue(s.contains("atm_iv / rv_20d"));
        assertTrue(s.contains("\"formula\":\"vrp_ratio\""));
    }

    @Test
    public void testScreenerMultiSort() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("sort", java.util.List.of(
                java.util.Map.of("field", "dealer_flow_risk", "direction", "asc"),
                java.util.Map.of("field", "harvest_score", "direction", "desc")));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("dealer_flow_risk"));
        assertTrue(s.contains("\"direction\":\"asc\""));
        assertTrue(s.contains("\"direction\":\"desc\""));
    }

    @Test
    public void testScreenerPagination() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("limit", 10);
        body.put("offset", 10);
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("\"limit\":10"));
        assertTrue(s.contains("\"offset\":10"));
    }

    @Test
    public void testScreenerNegativeNumber() throws Exception {
        enqueueOk();
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "net_gex", "operator", "lt", "value", -500000));
        client.screener(body);
        String s = server.takeRequest().getBody().readUtf8();
        assertTrue(s.contains("-500000"));
    }

    @Test
    public void testScreenerParsesResponse() throws Exception {
        enqueue(200, "{\"meta\":{\"total_count\":7,\"tier\":\"alpha\",\"universe_size\":250},\"data\":[{\"symbol\":\"SPY\",\"price\":656.01}]}");
        JsonObject result = client.screener(new java.util.LinkedHashMap<>());
        assertEquals("alpha", result.getAsJsonObject("meta").get("tier").getAsString());
        assertEquals(7, result.getAsJsonObject("meta").get("total_count").getAsInt());
        assertEquals(656.01, result.getAsJsonArray("data").get(0).getAsJsonObject().get("price").getAsDouble(), 0.001);
    }

    @Test
    public void testScreenerThrowsOn400ValidationError() throws Exception {
        enqueue(400, "{\"status\":\"ERROR\",\"error\":\"validation_error\",\"message\":\"Field 'harvest_score' requires the Alpha plan or higher.\"}");
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("filters", java.util.Map.of(
                "field", "harvest_score", "operator", "gte", "value", 65));
        try {
            client.screener(body);
            fail("expected FlashAlphaException");
        } catch (FlashAlphaException e) {
            assertEquals(400, e.getStatusCode());
            assertTrue(e.getMessage().contains("Alpha"));
        }
    }

    @Test
    public void testScreenerThrowsOn403TierRestricted() throws Exception {
        enqueue(403, "{\"status\":\"ERROR\",\"error\":\"tier_restricted\",\"message\":\"Screener requires Growth plan.\",\"current_plan\":\"Free\",\"required_plan\":\"Growth\"}");
        try {
            client.screener(new java.util.LinkedHashMap<>());
            fail("expected TierRestrictedException");
        } catch (TierRestrictedException e) {
            assertEquals("Free", e.getCurrentPlan());
            assertEquals("Growth", e.getRequiredPlan());
        }
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

    // ── v1.1 new endpoint families ─────────────────────────────────────

    // Strategy signals

    @Test
    public void testStrategyZeroDte() throws Exception {
        enqueueOk();
        client.strategyZeroDte("SPX");
        RecordedRequest req = server.takeRequest();
        assertEquals("GET", req.getMethod());
        assertEquals("/v1/strategies/zero-dte/SPX", req.getPath());
    }

    @Test
    public void testStrategyZeroDteWithParams() throws Exception {
        enqueueOk();
        client.strategyZeroDte("SPX", "2026-04-17", 500, 5.0);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/strategies/zero-dte/SPX"));
        assertTrue(path.contains("expiry=2026-04-17"));
        assertTrue(path.contains("minOpenInterest=500"));
        assertTrue(path.contains("wingWidth=5.0"));
    }

    @Test
    public void testStrategyVolCarryParsesEnvelope() throws Exception {
        enqueue(200, "{\"strategy\":\"vol-carry\",\"symbol\":\"SPY\",\"decision\":\"sell_strangle\",\"score\":72}");
        StrategyDecisionResponse r = client.strategyVolCarryTyped("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/strategies/vol-carry/SPY", req.getPath());
        assertEquals("sell_strangle", r.decision);
        assertEquals(Integer.valueOf(72), r.score);
    }

    // Earnings

    @Test
    public void testEarningsCalendarDefaults() throws Exception {
        enqueueOk();
        client.earningsCalendar();
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/earnings/calendar", req.getPath());
    }

    @Test
    public void testEarningsCalendarWithParams() throws Exception {
        enqueueOk();
        client.earningsCalendar(7, "AAPL,MSFT", 3);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.contains("days=7"));
        assertTrue(path.contains("symbols=AAPL%2CMSFT"));
        assertTrue(path.contains("importance=3"));
    }

    @Test
    public void testEarningsExpectedMove() throws Exception {
        enqueueOk();
        client.earningsExpectedMove("AAPL");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/earnings/expected-move/AAPL", req.getPath());
    }

    // Structures (POST, pure math)

    @Test
    public void testStructurePnl() throws Exception {
        enqueueOk();
        StructureRequest request = new StructureRequest(java.util.List.of(
                StructureLeg.pnlLeg("sell", "put", 450.0, 3.20, 1),
                StructureLeg.pnlLeg("buy", "put", 440.0, 1.40, 1)));
        client.structurePnl(request);
        RecordedRequest req = server.takeRequest();
        assertEquals("POST", req.getMethod());
        assertEquals("/v1/structures/pnl", req.getPath());
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("\"action\":\"sell\""));
        assertTrue(body.contains("\"strike\":450.0"));
        assertTrue(body.contains("\"premium\":3.2"));
    }

    @Test
    public void testStructurePnlParsesResponse() throws Exception {
        enqueue(200, "{\"max_profit\":1.8,\"max_loss\":-8.2,\"breakevens\":[448.2]}");
        StructureRequest request = new StructureRequest(java.util.List.of(
                StructureLeg.pnlLeg("sell", "put", 450.0, 3.20, 1)));
        StructurePnlResponse r = client.structurePnlTyped(request);
        server.takeRequest();
        assertEquals(1.8, r.maxProfit, 0.001);
    }

    // Dispersion

    @Test
    public void testDispersion() throws Exception {
        enqueueOk();
        client.dispersion("SPX", "AAPL,MSFT,NVDA");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/dispersion"));
        assertTrue(path.contains("index=SPX"));
        assertTrue(path.contains("symbols=AAPL%2CMSFT%2CNVDA"));
    }

    @Test
    public void testDispersionWithWeightsAndHorizon() throws Exception {
        enqueueOk();
        client.dispersion("SPX", "AAPL,MSFT", "0.6,0.4", 30);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.contains("weights=0.6%2C0.4"));
        assertTrue(path.contains("horizon_days=30"));
    }

    // Expected move

    @Test
    public void testExpectedMove() throws Exception {
        enqueueOk();
        client.expectedMove("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/expected-move/SPY", req.getPath());
    }

    @Test
    public void testExpectedMoveWithExpiry() throws Exception {
        enqueueOk();
        client.expectedMove("SPY", "2026-04-17");
        RecordedRequest req = server.takeRequest();
        assertTrue(req.getPath().contains("expiry=2026-04-17"));
    }

    // Realized volatility + forecast

    @Test
    public void testRealizedVolatility() throws Exception {
        enqueueOk();
        client.realizedVolatility("AAPL");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/volatility/realized/AAPL", req.getPath());
    }

    @Test
    public void testVolatilityForecast() throws Exception {
        enqueueOk();
        client.volatilityForecast("AAPL");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/volatility/forecast/AAPL", req.getPath());
    }

    @Test
    public void testVolatilityForecastWithDist() throws Exception {
        enqueueOk();
        client.volatilityForecast("AAPL", "gaussian");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/volatility/forecast/AAPL"));
        assertTrue(path.contains("dist=gaussian"));
    }

    // VRP history + point-in-time

    @Test
    public void testVrpHistory() throws Exception {
        enqueueOk();
        client.vrpHistory("SPY", 30);
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/vrp/SPY/history"));
        assertTrue(path.contains("days=30"));
    }

    @Test
    public void testVrpWithDate() throws Exception {
        enqueueOk();
        client.vrp("SPY", "2026-03-20");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/vrp/SPY"));
        assertTrue(path.contains("date=2026-03-20"));
    }

    @Test
    public void testVrpBackCompatNoDate() throws Exception {
        enqueueOk();
        client.vrp("SPY");
        RecordedRequest req = server.takeRequest();
        assertEquals("/v1/vrp/SPY", req.getPath());
    }

    // Zero-DTE expiry overload

    @Test
    public void testZeroDteWithExpiry() throws Exception {
        enqueueOk();
        client.zeroDte("SPX", 0.05, "2026-04-17");
        RecordedRequest req = server.takeRequest();
        String path = req.getPath();
        assertTrue(path.startsWith("/v1/exposure/zero-dte/SPX"));
        assertTrue(path.contains("strike_range=0.05"));
        assertTrue(path.contains("expiry=2026-04-17"));
    }
}
