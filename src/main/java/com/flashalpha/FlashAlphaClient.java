package com.flashalpha;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin Java client for the FlashAlpha options analytics REST API.
 *
 * <p>Obtain an API key at <a href="https://flashalpha.com">flashalpha.com</a>.
 *
 * <pre>{@code
 * FlashAlphaClient client = new FlashAlphaClient(System.getenv("FLASHALPHA_API_KEY"));
 * JsonObject gex = client.gex("SPY");
 * System.out.println(gex);
 * }</pre>
 *
 * <p>All methods return a {@link JsonObject} parsed from the JSON response body.
 * On non-200 responses the client throws the appropriate subclass of
 * {@link FlashAlphaException}:
 * <ul>
 *   <li>401 → {@link AuthenticationException}</li>
 *   <li>403 → {@link TierRestrictedException}</li>
 *   <li>404 → {@link NotFoundException}</li>
 *   <li>429 → {@link RateLimitException}</li>
 *   <li>5xx → {@link ServerException}</li>
 * </ul>
 */
public class FlashAlphaClient {

    /** Default API base URL. */
    public static final String DEFAULT_BASE_URL = "https://lab.flashalpha.com";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http;
    private final Gson gson = new Gson();

    // ── Constructors ──────────────────────────────────────────────────

    /**
     * Create a client using the default base URL ({@value DEFAULT_BASE_URL}).
     *
     * @param apiKey Your FlashAlpha API key.
     */
    public FlashAlphaClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    /**
     * Create a client with a custom base URL (useful for testing).
     *
     * @param apiKey  Your FlashAlpha API key.
     * @param baseUrl Override base URL, e.g. {@code http://localhost:8080}.
     */
    public FlashAlphaClient(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey must not be null or empty");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    // ── Internal HTTP helpers ─────────────────────────────────────────

    private JsonObject get(String path) {
        return get(path, null);
    }

    private JsonObject get(String path, Map<String, String> params) {
        String url = baseUrl + path;
        if (params != null && !params.isEmpty()) {
            url = url + "?" + buildQuery(params);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", apiKey)
                .header("Accept", "application/json")
                .GET()
                .timeout(DEFAULT_TIMEOUT)
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlashAlphaException("HTTP request failed: " + e.getMessage(), 0, null);
        }

        return handleResponse(response);
    }

    private JsonObject post(String path, Object body) {
        String url = baseUrl + path;
        String jsonBody = body != null ? gson.toJson(body) : "{}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .timeout(DEFAULT_TIMEOUT)
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlashAlphaException("HTTP request failed: " + e.getMessage(), 0, null);
        }

        return handleResponse(response);
    }

    private JsonObject handleResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();

        if (status == 200) {
            return JsonParser.parseString(body).getAsJsonObject();
        }

        // Try to parse error body as JSON
        JsonObject jsonBody = null;
        try {
            jsonBody = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception ignored) {
            // body is not JSON; leave jsonBody null
        }

        String message = extractMessage(jsonBody, body);

        if (status == 401) {
            throw new AuthenticationException(message, jsonBody);
        }
        if (status == 403) {
            String currentPlan = jsonBody != null && jsonBody.has("current_plan")
                    ? jsonBody.get("current_plan").getAsString() : null;
            String requiredPlan = jsonBody != null && jsonBody.has("required_plan")
                    ? jsonBody.get("required_plan").getAsString() : null;
            throw new TierRestrictedException(message, jsonBody, currentPlan, requiredPlan);
        }
        if (status == 404) {
            throw new NotFoundException(message, jsonBody);
        }
        if (status == 429) {
            Integer retryAfter = null;
            String retryAfterHeader = response.headers().firstValue("Retry-After").orElse(null);
            if (retryAfterHeader != null) {
                try {
                    retryAfter = Integer.parseInt(retryAfterHeader);
                } catch (NumberFormatException ignored) {
                    // leave retryAfter null
                }
            }
            throw new RateLimitException(message, jsonBody, retryAfter);
        }
        if (status >= 500) {
            throw new ServerException(message, status, jsonBody);
        }

        throw new FlashAlphaException(message, status, jsonBody);
    }

    private static String extractMessage(JsonObject body, String rawText) {
        if (body != null) {
            if (body.has("message") && !body.get("message").isJsonNull()) {
                return body.get("message").getAsString();
            }
            if (body.has("detail") && !body.get("detail").isJsonNull()) {
                return body.get("detail").getAsString();
            }
        }
        return rawText != null ? rawText : "Unknown error";
    }

    private static String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ── Market Data ───────────────────────────────────────────────────

    /**
     * Live stock quote (bid/ask/mid/last).
     *
     * @param ticker Stock ticker symbol, e.g. {@code "SPY"}.
     */
    public JsonObject stockQuote(String ticker) {
        return get("/stockquote/" + ticker);
    }

    /**
     * Option quotes with greeks for all contracts on {@code ticker}.
     * Requires Growth+ plan.
     *
     * @param ticker Stock ticker symbol.
     */
    public JsonObject optionQuote(String ticker) {
        return optionQuote(ticker, null, null, null);
    }

    /**
     * Option quotes with greeks, filtered by expiry, strike, and/or type.
     * Requires Growth+ plan.
     *
     * @param ticker Stock ticker symbol.
     * @param expiry Expiration date filter, e.g. {@code "2024-01-19"} (nullable).
     * @param strike Strike price filter (nullable).
     * @param type   Option type: {@code "call"} or {@code "put"} (nullable).
     */
    public JsonObject optionQuote(String ticker, String expiry, Double strike, String type) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        if (strike != null) params.put("strike", String.valueOf(strike));
        if (type != null) params.put("type", type);
        return get("/optionquote/" + ticker, params.isEmpty() ? null : params);
    }

    /**
     * Volatility surface grid (public endpoint, no auth required).
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject surface(String symbol) {
        return get("/v1/surface/" + symbol);
    }

    /**
     * Comprehensive stock summary (price, volatility, exposure, macro).
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject stockSummary(String symbol) {
        return get("/v1/stock/" + symbol + "/summary");
    }

    // ── Historical ────────────────────────────────────────────────────

    /**
     * Historical stock quotes (minute-by-minute).
     *
     * @param ticker Stock ticker symbol.
     * @param date   Date in {@code YYYY-MM-DD} format.
     * @param time   Optional time filter in {@code HH:MM} format (nullable).
     */
    public JsonObject historicalStockQuote(String ticker, String date, String time) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("date", date);
        if (time != null) params.put("time", time);
        return get("/historical/stockquote/" + ticker, params);
    }

    /**
     * Historical option quotes (minute-by-minute).
     *
     * @param ticker Stock ticker symbol.
     * @param date   Date in {@code YYYY-MM-DD} format.
     * @param time   Optional time filter (nullable).
     * @param expiry Optional expiration date filter (nullable).
     * @param strike Optional strike price filter (nullable).
     * @param type   Optional option type filter (nullable).
     */
    public JsonObject historicalOptionQuote(String ticker, String date, String time,
                                            String expiry, Double strike, String type) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("date", date);
        if (time != null) params.put("time", time);
        if (expiry != null) params.put("expiry", expiry);
        if (strike != null) params.put("strike", String.valueOf(strike));
        if (type != null) params.put("type", type);
        return get("/historical/optionquote/" + ticker, params);
    }

    // ── Exposure Analytics ────────────────────────────────────────────

    /**
     * Gamma exposure (GEX) by strike.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject gex(String symbol) {
        return gex(symbol, null, null);
    }

    /**
     * Gamma exposure (GEX) by strike, with optional filters.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     * @param minOi      Minimum open interest filter (nullable).
     */
    public JsonObject gex(String symbol, String expiration, Integer minOi) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        if (minOi != null) params.put("min_oi", String.valueOf(minOi));
        return get("/v1/exposure/gex/" + symbol, params.isEmpty() ? null : params);
    }

    /**
     * Delta exposure (DEX) by strike.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject dex(String symbol) {
        return dex(symbol, null);
    }

    /**
     * Delta exposure (DEX) by strike, with optional expiration filter.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public JsonObject dex(String symbol, String expiration) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        return get("/v1/exposure/dex/" + symbol, params.isEmpty() ? null : params);
    }

    /**
     * Vanna exposure (VEX) by strike.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject vex(String symbol) {
        return vex(symbol, null);
    }

    /**
     * Vanna exposure (VEX) by strike, with optional expiration filter.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public JsonObject vex(String symbol, String expiration) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        return get("/v1/exposure/vex/" + symbol, params.isEmpty() ? null : params);
    }

    /**
     * Charm exposure (CHEX) by strike.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject chex(String symbol) {
        return chex(symbol, null);
    }

    /**
     * Charm exposure (CHEX) by strike, with optional expiration filter.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public JsonObject chex(String symbol, String expiration) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        return get("/v1/exposure/chex/" + symbol, params.isEmpty() ? null : params);
    }

    /**
     * Key support/resistance levels derived from options exposure.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureLevels(String symbol) {
        return get("/v1/exposure/levels/" + symbol);
    }

    /**
     * Full exposure summary (GEX/DEX/VEX/CHEX + hedging analysis).
     * Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureSummary(String symbol) {
        return get("/v1/exposure/summary/" + symbol);
    }

    /**
     * Verbal narrative analysis of options exposure. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject narrative(String symbol) {
        return get("/v1/exposure/narrative/" + symbol);
    }

    /**
     * Real-time 0DTE analytics: regime, expected move, pin risk, hedging, decay.
     * Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject zeroDte(String symbol) {
        return zeroDte(symbol, null);
    }

    /**
     * Real-time 0DTE analytics with configurable strike range.
     * Requires Growth+ plan.
     *
     * @param symbol      Underlying symbol.
     * @param strikeRange Percentage range around spot to include (nullable).
     */
    public JsonObject zeroDte(String symbol, Double strikeRange) {
        Map<String, String> params = new LinkedHashMap<>();
        if (strikeRange != null) params.put("strike_range", String.valueOf(strikeRange));
        return get("/v1/exposure/zero-dte/" + symbol, params.isEmpty() ? null : params);
    }

    /**
     * Daily exposure snapshots for trend analysis. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureHistory(String symbol) {
        return exposureHistory(symbol, null);
    }

    /**
     * Daily exposure snapshots for trend analysis. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     * @param days   Number of days of history to return (nullable = API default).
     */
    public JsonObject exposureHistory(String symbol, Integer days) {
        Map<String, String> params = new LinkedHashMap<>();
        if (days != null) params.put("days", String.valueOf(days));
        return get("/v1/exposure/history/" + symbol, params.isEmpty() ? null : params);
    }

    // ── Pricing & Sizing ──────────────────────────────────────────────

    /**
     * Full Black-Scholes-Merton greeks (first, second, and third order).
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param sigma  Annualised implied volatility (decimal, e.g. {@code 0.25}).
     * @param type   Option type: {@code "call"} or {@code "put"}.
     * @param r      Risk-free rate (nullable; API default used if null).
     * @param q      Continuous dividend yield (nullable; API default used if null).
     */
    public JsonObject greeks(double spot, double strike, double dte, double sigma,
                             String type, Double r, Double q) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("spot", String.valueOf(spot));
        params.put("strike", String.valueOf(strike));
        params.put("dte", String.valueOf(dte));
        params.put("sigma", String.valueOf(sigma));
        params.put("type", type);
        if (r != null) params.put("r", String.valueOf(r));
        if (q != null) params.put("q", String.valueOf(q));
        return get("/v1/pricing/greeks", params);
    }

    /**
     * Implied volatility derived from a market option price.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param price  Market price of the option.
     * @param type   Option type: {@code "call"} or {@code "put"}.
     * @param r      Risk-free rate (nullable).
     * @param q      Continuous dividend yield (nullable).
     */
    public JsonObject iv(double spot, double strike, double dte, double price,
                         String type, Double r, Double q) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("spot", String.valueOf(spot));
        params.put("strike", String.valueOf(strike));
        params.put("dte", String.valueOf(dte));
        params.put("price", String.valueOf(price));
        params.put("type", type);
        if (r != null) params.put("r", String.valueOf(r));
        if (q != null) params.put("q", String.valueOf(q));
        return get("/v1/pricing/iv", params);
    }

    /**
     * Kelly criterion optimal position sizing. Requires Growth+ plan.
     *
     * @param spot    Current underlying price.
     * @param strike  Option strike price.
     * @param dte     Days to expiration.
     * @param sigma   Annualised implied volatility.
     * @param premium Option premium paid.
     * @param mu      Expected annualised return of the underlying.
     * @param type    Option type: {@code "call"} or {@code "put"}.
     * @param r       Risk-free rate (nullable).
     * @param q       Continuous dividend yield (nullable).
     */
    public JsonObject kelly(double spot, double strike, double dte, double sigma,
                            double premium, double mu, String type, Double r, Double q) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("spot", String.valueOf(spot));
        params.put("strike", String.valueOf(strike));
        params.put("dte", String.valueOf(dte));
        params.put("sigma", String.valueOf(sigma));
        params.put("premium", String.valueOf(premium));
        params.put("mu", String.valueOf(mu));
        params.put("type", type);
        if (r != null) params.put("r", String.valueOf(r));
        if (q != null) params.put("q", String.valueOf(q));
        return get("/v1/pricing/kelly", params);
    }

    // ── Volatility Analytics ──────────────────────────────────────────

    /**
     * Comprehensive volatility analysis (term structure, skew, realized vs implied).
     * Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject volatility(String symbol) {
        return get("/v1/volatility/" + symbol);
    }

    /**
     * Advanced volatility analytics: SVI parameters, variance surface, arbitrage
     * detection, greeks surfaces, and variance swap. Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject advVolatility(String symbol) {
        return get("/v1/adv_volatility/" + symbol);
    }

    // ── Reference Data ────────────────────────────────────────────────

    /**
     * All available stock tickers supported by the API.
     */
    public JsonObject tickers() {
        return get("/v1/tickers");
    }

    /**
     * Option chain metadata (expirations and strikes) for a given ticker.
     *
     * @param ticker Stock ticker symbol.
     */
    public JsonObject options(String ticker) {
        return get("/v1/options/" + ticker);
    }

    /**
     * Currently queried symbols with live data available.
     */
    public JsonObject symbols() {
        return get("/v1/symbols");
    }

    // ── Account & System ──────────────────────────────────────────────

    // ── Screener ──────────────────────────────────────────────────────

    /**
     * Live options screener — filter and rank symbols by gamma exposure, VRP,
     * volatility, greeks, and more. Powered by an in-memory store updated every
     * 5-10s from live market data.
     *
     * <p>Growth: 10-symbol universe, up to 10 rows. Alpha: ~250 symbols, up to
     * 50 rows, formulas, and harvest/dealer-flow-risk scores.
     *
     * <pre>{@code
     * Map<String, Object> body = new LinkedHashMap<>();
     * Map<String, Object> filters = new LinkedHashMap<>();
     * filters.put("op", "and");
     * filters.put("conditions", List.of(
     *     Map.of("field", "regime", "operator", "eq", "value", "positive_gamma"),
     *     Map.of("field", "harvest_score", "operator", "gte", "value", 65)
     * ));
     * body.put("filters", filters);
     * body.put("sort", List.of(Map.of("field", "harvest_score", "direction", "desc")));
     * body.put("select", List.of("symbol", "price", "harvest_score", "dealer_flow_risk"));
     * JsonObject result = client.screener(body);
     * }</pre>
     *
     * @param body Request body. See the Screener spec for the full schema:
     *             {@code {filters, sort, select, formulas, limit, offset}}.
     *             Pass an empty map for the default universe.
     */
    public JsonObject screener(Map<String, Object> body) {
        return post("/v1/screener/live", body != null ? body : new LinkedHashMap<>());
    }

    /**
     * Live options screener with a raw request object (for callers that want
     * to pass a POJO or {@link com.google.gson.JsonObject}).
     */
    public JsonObject screener(Object body) {
        return post("/v1/screener/live", body);
    }

    // ── Account & System ──────────────────────────────────────────────

    /**
     * Account information and quota usage.
     */
    public JsonObject account() {
        return get("/v1/account");
    }

    /**
     * API health check (public endpoint, no auth required).
     */
    public JsonObject health() {
        return get("/health");
    }
}
