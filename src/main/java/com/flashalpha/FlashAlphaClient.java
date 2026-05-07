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

    /** URL-escape a single path segment (e.g. a ticker) — escapes / ? % etc. */
    private static String _seg(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

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
        return get("/stockquote/" + _seg(ticker));
    }

    /**
     * Strongly-typed variant of {@link #stockQuote(String)}. Returns a
     * populated {@link StockQuoteResponse} with named fields. The original
     * untyped method is unchanged.
     *
     * @param ticker Stock ticker symbol.
     */
    public StockQuoteResponse stockQuoteTyped(String ticker) {
        JsonObject raw = stockQuote(ticker);
        return gson.fromJson(raw, StockQuoteResponse.class);
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
        return get("/optionquote/" + _seg(ticker), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #optionQuote(String, String, Double, String)}.
     * Returns a populated {@link OptionQuoteResponse} when all three filters
     * ({@code expiry} + {@code strike} + {@code type}) are supplied. For
     * less-specific calls the API returns an array; in that case prefer the
     * raw {@link #optionQuote(String, String, Double, String)} method and
     * deserialize each element with the configured {@link Gson} instance.
     *
     * @param ticker Stock ticker symbol.
     * @param expiry Expiration date filter (must be non-null for a single-object response).
     * @param strike Strike price filter (must be non-null for a single-object response).
     * @param type   Option type filter (must be non-null for a single-object response).
     */
    public OptionQuoteResponse optionQuoteTyped(String ticker, String expiry, Double strike, String type) {
        JsonObject raw = optionQuote(ticker, expiry, strike, type);
        return gson.fromJson(raw, OptionQuoteResponse.class);
    }

    /**
     * Strongly-typed variant of {@link #optionQuote(String)} returning a
     * single populated {@link OptionQuoteResponse}. Note: when no
     * {@code expiry} / {@code strike} / {@code type} filters are supplied
     * the live API returns an array — prefer the parameterised
     * {@link #optionQuoteTyped(String, String, Double, String)} overload.
     *
     * @param ticker Stock ticker symbol.
     */
    public OptionQuoteResponse optionQuoteTyped(String ticker) {
        return optionQuoteTyped(ticker, null, null, null);
    }

    /**
     * Volatility surface grid (public endpoint, no auth required).
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject surface(String symbol) {
        return get("/v1/surface/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #surface(String)}. Returns a populated
     * {@link SurfaceResponse} with named fields. The original untyped method
     * is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public SurfaceResponse surfaceTyped(String symbol) {
        JsonObject raw = surface(symbol);
        return gson.fromJson(raw, SurfaceResponse.class);
    }

    /**
     * Comprehensive stock summary (price, volatility, exposure, macro).
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject stockSummary(String symbol) {
        return get("/v1/stock/" + _seg(symbol) + "/summary");
    }

    /**
     * Strongly-typed variant of {@link #stockSummary(String)}. Returns a
     * populated {@link StockSummaryResponse} with named fields. The original
     * untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public StockSummaryResponse stockSummaryTyped(String symbol) {
        JsonObject raw = stockSummary(symbol);
        return gson.fromJson(raw, StockSummaryResponse.class);
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
        return get("/historical/stockquote/" + _seg(ticker), params);
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
        return get("/historical/optionquote/" + _seg(ticker), params);
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
        return get("/v1/exposure/gex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #gex(String)}. Returns a populated
     * {@link GexResponse}. The original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public GexResponse gexTyped(String symbol) {
        return gexTyped(symbol, null, null);
    }

    /**
     * Strongly-typed variant of {@link #gex(String, String, Integer)}.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     * @param minOi      Minimum open interest filter (nullable).
     */
    public GexResponse gexTyped(String symbol, String expiration, Integer minOi) {
        JsonObject raw = gex(symbol, expiration, minOi);
        return gson.fromJson(raw, GexResponse.class);
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
        return get("/v1/exposure/dex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #dex(String)}. Returns a populated
     * {@link DexResponse}. The original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public DexResponse dexTyped(String symbol) {
        return dexTyped(symbol, null);
    }

    /**
     * Strongly-typed variant of {@link #dex(String, String)}.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public DexResponse dexTyped(String symbol, String expiration) {
        JsonObject raw = dex(symbol, expiration);
        return gson.fromJson(raw, DexResponse.class);
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
        return get("/v1/exposure/vex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #vex(String)}. Returns a populated
     * {@link VexResponse}. The original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public VexResponse vexTyped(String symbol) {
        return vexTyped(symbol, null);
    }

    /**
     * Strongly-typed variant of {@link #vex(String, String)}.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public VexResponse vexTyped(String symbol, String expiration) {
        JsonObject raw = vex(symbol, expiration);
        return gson.fromJson(raw, VexResponse.class);
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
        return get("/v1/exposure/chex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #chex(String)}. Returns a populated
     * {@link ChexResponse}. The original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public ChexResponse chexTyped(String symbol) {
        return chexTyped(symbol, null);
    }

    /**
     * Strongly-typed variant of {@link #chex(String, String)}.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     */
    public ChexResponse chexTyped(String symbol, String expiration) {
        JsonObject raw = chex(symbol, expiration);
        return gson.fromJson(raw, ChexResponse.class);
    }

    /**
     * Key support/resistance levels derived from options exposure.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureLevels(String symbol) {
        return get("/v1/exposure/levels/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #exposureLevels(String)}. Returns a
     * populated {@link ExposureLevelsResponse} with named fields. The original
     * untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public ExposureLevelsResponse exposureLevelsTyped(String symbol) {
        JsonObject raw = exposureLevels(symbol);
        return gson.fromJson(raw, ExposureLevelsResponse.class);
    }

    /**
     * Full exposure summary (GEX/DEX/VEX/CHEX + hedging analysis).
     * Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureSummary(String symbol) {
        return get("/v1/exposure/summary/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #exposureSummary(String)}. Returns a
     * populated {@link ExposureSummaryResponse} with named fields. The original
     * untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public ExposureSummaryResponse exposureSummaryTyped(String symbol) {
        JsonObject raw = exposureSummary(symbol);
        return gson.fromJson(raw, ExposureSummaryResponse.class);
    }

    /**
     * Verbal narrative analysis of options exposure. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject narrative(String symbol) {
        return get("/v1/exposure/narrative/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #narrative(String)}. Returns a populated
     * {@link NarrativeResponse} with named fields. The original untyped method
     * is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public NarrativeResponse narrativeTyped(String symbol) {
        JsonObject raw = narrative(symbol);
        return gson.fromJson(raw, NarrativeResponse.class);
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
        return get("/v1/exposure/zero-dte/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #zeroDte(String)}. Returns a populated
     * {@link ZeroDteResponse} with named fields for every documented value.
     * The original {@link #zeroDte(String)} method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public ZeroDteResponse zeroDteTyped(String symbol) {
        return zeroDteTyped(symbol, null);
    }

    /**
     * Strongly-typed variant of {@link #zeroDte(String, Double)} with configurable
     * strike range. Returns a populated {@link ZeroDteResponse}.
     *
     * @param symbol      Underlying symbol.
     * @param strikeRange Percentage range around spot to include (nullable).
     */
    public ZeroDteResponse zeroDteTyped(String symbol, Double strikeRange) {
        JsonObject raw = zeroDte(symbol, strikeRange);
        return gson.fromJson(raw, ZeroDteResponse.class);
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
        return get("/v1/exposure/history/" + _seg(symbol), params.isEmpty() ? null : params);
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
     * Strongly-typed variant of {@link #greeks(double, double, double, double, String, Double, Double)}.
     * Returns a populated {@link PricingGreeksResponse}. The original untyped
     * method is unchanged.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param sigma  Annualised implied volatility (decimal, e.g. {@code 0.25}).
     * @param type   Option type: {@code "call"} or {@code "put"}.
     * @param r      Risk-free rate (nullable; API default used if null).
     * @param q      Continuous dividend yield (nullable; API default used if null).
     */
    public PricingGreeksResponse greeksTyped(double spot, double strike, double dte, double sigma,
                                             String type, Double r, Double q) {
        JsonObject raw = greeks(spot, strike, dte, sigma, type, r, q);
        return gson.fromJson(raw, PricingGreeksResponse.class);
    }

    /**
     * Strongly-typed convenience overload of
     * {@link #greeksTyped(double, double, double, double, String, Double, Double)}
     * with explicit option type and API-default rate / yield.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param sigma  Annualised implied volatility (decimal, e.g. {@code 0.25}).
     * @param type   Option type: {@code "call"} or {@code "put"}.
     */
    public PricingGreeksResponse greeksTyped(double spot, double strike, double dte, double sigma,
                                             String type) {
        return greeksTyped(spot, strike, dte, sigma, type, null, null);
    }

    /**
     * Strongly-typed convenience overload of
     * {@link #greeksTyped(double, double, double, double, String, Double, Double)}
     * defaulting to {@code type="call"} and API-default rate / yield.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param sigma  Annualised implied volatility (decimal, e.g. {@code 0.25}).
     */
    public PricingGreeksResponse greeksTyped(double spot, double strike, double dte, double sigma) {
        return greeksTyped(spot, strike, dte, sigma, "call", null, null);
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
        return get("/v1/volatility/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #volatility(String)}. Returns a
     * populated {@link VolatilityResponse} with named fields for the
     * realized-vol ladder, IV-RV spreads, skew profiles, term structure,
     * GEX / theta by DTE, put-call profile, OI concentration, hedging
     * scenarios, and liquidity blocks. The original untyped method is
     * unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public VolatilityResponse volatilityTyped(String symbol) {
        JsonObject raw = volatility(symbol);
        return gson.fromJson(raw, VolatilityResponse.class);
    }

    /**
     * Advanced volatility analytics: SVI parameters, variance surface, arbitrage
     * detection, greeks surfaces, and variance swap. Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject advVolatility(String symbol) {
        return get("/v1/adv_volatility/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #advVolatility(String)}. Returns a
     * populated {@link AdvVolatilityResponse} with named fields for the
     * SVI parameters, forward prices, total variance surface, arbitrage
     * flags, variance-swap fair values, and greeks surfaces blocks. The
     * original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public AdvVolatilityResponse advVolatilityTyped(String symbol) {
        JsonObject raw = advVolatility(symbol);
        return gson.fromJson(raw, AdvVolatilityResponse.class);
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
        return get("/v1/options/" + _seg(ticker));
    }

    /**
     * Currently queried symbols with live data available.
     */
    public JsonObject symbols() {
        return get("/v1/symbols");
    }

    // ── Account & System ──────────────────────────────────────────────

    // ── Max Pain ──────────────────────────────────────────────────────

    /**
     * Max pain analysis with dealer alignment, pain curve, OI breakdown,
     * expected move, pin probability, and multi-expiry calendar.
     * Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject maxPain(String symbol) {
        return maxPain(symbol, null);
    }

    /**
     * Max pain analysis filtered to a single expiry.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable, format {@code yyyy-MM-dd}).
     */
    public JsonObject maxPain(String symbol, String expiration) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        return get("/v1/maxpain/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Strongly-typed variant of {@link #maxPain(String)}. Returns a populated
     * {@link MaxPainResponse} with named fields. The original untyped method
     * is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public MaxPainResponse maxPainTyped(String symbol) {
        return maxPainTyped(symbol, null);
    }

    /**
     * Strongly-typed variant of {@link #maxPain(String, String)} filtered to a
     * single expiry.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable, format {@code yyyy-MM-dd}).
     */
    public MaxPainResponse maxPainTyped(String symbol, String expiration) {
        JsonObject raw = maxPain(symbol, expiration);
        return gson.fromJson(raw, MaxPainResponse.class);
    }

    // ── VRP (Variance Risk Premium) ───────────────────────────────────

    /**
     * Variance risk premium analytics — the implied-vs-realized vol spread,
     * conditioned on dealer gamma and vanna regime, with strategy scores for
     * harvesting. Requires Alpha+ plan.
     *
     * <p>Returns a nested payload. Key access paths (note: most metrics are
     * NOT top-level — drill into the nested objects):
     * <ul>
     *   <li>Top-level: {@code symbol}, {@code underlying_price}, {@code as_of},
     *       {@code market_open}, {@code net_harvest_score},
     *       {@code dealer_flow_risk}</li>
     *   <li>{@code response.getAsJsonObject("vrp")} → {@code z_score},
     *       {@code percentile}, {@code atm_iv},
     *       {@code rv_5d/10d/20d/30d}, {@code vrp_5d/10d/20d/30d},
     *       {@code history_days}</li>
     *   <li>{@code response.getAsJsonObject("directional")} →
     *       {@code put_wing_iv_25d}, {@code call_wing_iv_25d},
     *       {@code downside_rv_20d}, {@code upside_rv_20d},
     *       {@code downside_vrp}, {@code upside_vrp}
     *       (NOT {@code put_vrp} / {@code call_vrp})</li>
     *   <li>{@code response.getAsJsonObject("regime")} → {@code gamma},
     *       {@code vrp_regime}, {@code net_gex}, {@code gamma_flip}</li>
     *   <li>{@code response.getAsJsonObject("gex_conditioned")} →
     *       {@code regime}, {@code harvest_score}, {@code interpretation}
     *       (object may be JSON null)</li>
     *   <li>{@code response.getAsJsonObject("strategy_scores")} →
     *       {@code short_put_spread}, {@code short_strangle},
     *       {@code iron_condor}, {@code calendar_spread}
     *       (object may be JSON null)</li>
     *   <li>{@code response.getAsJsonArray("term_vrp")} → array of
     *       {@code {dte, iv, rv, vrp}} term-structure points</li>
     * </ul>
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject vrp(String symbol) {
        return get("/v1/vrp/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #vrp(String)}. Returns a populated
     * {@link VrpResponse} with named fields for the nested VRP, directional,
     * regime, gex_conditioned, strategy_scores, and term_vrp groups. The
     * original untyped method is unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public VrpResponse vrpTyped(String symbol) {
        JsonObject raw = vrp(symbol);
        return gson.fromJson(raw, VrpResponse.class);
    }

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
        return post("/v1/screener", body != null ? body : new LinkedHashMap<>());
    }

    /**
     * Live options screener with a raw request object (for callers that want
     * to pass a POJO or {@link com.google.gson.JsonObject}).
     */
    public JsonObject screener(Object body) {
        return post("/v1/screener", body);
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
