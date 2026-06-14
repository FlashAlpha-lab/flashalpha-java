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

    // ── Flow (live, simulation-aware) — requires the Alpha plan ────────
    //
    // Analytics endpoints (snake_case) fold today's intraday trade tape
    // into the settled book. All accept an optional expiry ("YYYY-MM-DD").
    // Each untyped method returns a JsonObject; the matching *Typed method
    // deserializes into the POJO. Raw flow endpoints proxy camelCase JSON.

    /** Live gamma flip / call &amp; put walls / max pain. Requires the Alpha plan. */
    public JsonObject flowLevels(String symbol) { return flowLevels(symbol, null); }

    /** Live levels, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowLevels(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/levels/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowLevels(String)}. */
    public FlowLevelsResponse flowLevelsTyped(String symbol) { return flowLevelsTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowLevels(String, String)}. */
    public FlowLevelsResponse flowLevelsTyped(String symbol, String expiry) {
        return gson.fromJson(flowLevels(symbol, expiry), FlowLevelsResponse.class);
    }

    /** 0DTE pin-risk score + component breakdown. Requires the Alpha plan. */
    public JsonObject flowPinRisk(String symbol) { return flowPinRisk(symbol, null); }

    /** Pin risk, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowPinRisk(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/pin-risk/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowPinRisk(String)}. */
    public FlowPinRiskResponse flowPinRiskTyped(String symbol) { return flowPinRiskTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowPinRisk(String, String)}. */
    public FlowPinRiskResponse flowPinRiskTyped(String symbol, String expiry) {
        return gson.fromJson(flowPinRisk(symbol, expiry), FlowPinRiskResponse.class);
    }

    /** At-a-glance flow direction + headline GEX shift. Requires the Alpha plan. */
    public JsonObject flowSummary(String symbol) { return flowSummary(symbol, null); }

    /** Flow summary, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowSummary(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/summary/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowSummary(String)}. */
    public FlowSummaryResponse flowSummaryTyped(String symbol) { return flowSummaryTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowSummary(String, String)}. */
    public FlowSummaryResponse flowSummaryTyped(String symbol, String expiry) {
        return gson.fromJson(flowSummary(symbol, expiry), FlowSummaryResponse.class);
    }

    /** Open-interest simulator state (official vs intraday). Requires the Alpha plan. */
    public JsonObject flowOi(String symbol) { return flowOi(symbol, null); }

    /** OI simulator state, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowOi(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/oi/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOi(String)}. */
    public FlowOiResponse flowOiTyped(String symbol) { return flowOiTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowOi(String, String)}. */
    public FlowOiResponse flowOiTyped(String symbol, String expiry) {
        return gson.fromJson(flowOi(symbol, expiry), FlowOiResponse.class);
    }

    /** Live (flow-adjusted) GEX + per-strike profile. Requires the Alpha plan. */
    public JsonObject flowGex(String symbol) { return flowGex(symbol, null); }

    /** Live GEX, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowGex(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/gex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowGex(String)}. */
    public FlowGexResponse flowGexTyped(String symbol) { return flowGexTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowGex(String, String)}. */
    public FlowGexResponse flowGexTyped(String symbol, String expiry) {
        return gson.fromJson(flowGex(symbol, expiry), FlowGexResponse.class);
    }

    /** Live (flow-adjusted) DEX + per-strike profile. Requires the Alpha plan. */
    public JsonObject flowDex(String symbol) { return flowDex(symbol, null); }

    /** Live DEX, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowDex(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/dex/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowDex(String)}. */
    public FlowDexResponse flowDexTyped(String symbol) { return flowDexTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowDex(String, String)}. */
    public FlowDexResponse flowDexTyped(String symbol, String expiry) {
        return gson.fromJson(flowDex(symbol, expiry), FlowDexResponse.class);
    }

    /** Settled-vs-live dealer GEX/DEX + flow adjustment. Requires the Alpha plan. */
    public JsonObject flowDealerRisk(String symbol) { return flowDealerRisk(symbol, null); }

    /** Dealer risk, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowDealerRisk(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/dealer-risk/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowDealerRisk(String)}. */
    public FlowDealerRiskResponse flowDealerRiskTyped(String symbol) { return flowDealerRiskTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowDealerRisk(String, String)}. */
    public FlowDealerRiskResponse flowDealerRiskTyped(String symbol, String expiry) {
        return gson.fromJson(flowDealerRisk(symbol, expiry), FlowDealerRiskResponse.class);
    }

    /** Everything-at-once live flow bundle (convenience). Requires the Alpha plan. */
    public JsonObject flowLive(String symbol) { return flowLive(symbol, null); }

    /** Live bundle, sliced to one expiration cycle ({@code expiry}, nullable). Requires Alpha. */
    public JsonObject flowLive(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/live/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowLive(String)}. */
    public FlowLiveResponse flowLiveTyped(String symbol) { return flowLiveTyped(symbol, null); }

    /** Strongly-typed variant of {@link #flowLive(String, String)}. */
    public FlowLiveResponse flowLiveTyped(String symbol, String expiry) {
        return gson.fromJson(flowLive(symbol, expiry), FlowLiveResponse.class);
    }

    /** Recent option trades, newest-first ({@code limit} 1-500, both nullable). Requires Alpha. */
    public JsonObject flowOptionRecent(String symbol, Integer limit, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/recent", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionRecent(String, Integer, String)}. */
    public FlowOptionRecentResponse flowOptionRecentTyped(String symbol, Integer limit, String expiry) {
        return gson.fromJson(flowOptionRecent(symbol, limit, expiry), FlowOptionRecentResponse.class);
    }

    /** Per-underlying option-flow aggregates ({@code expiry} nullable). Requires Alpha. */
    public JsonObject flowOptionSummary(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/summary", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionSummary(String, String)}. */
    public FlowOptionSummaryResponse flowOptionSummaryTyped(String symbol, String expiry) {
        return gson.fromJson(flowOptionSummary(symbol, expiry), FlowOptionSummaryResponse.class);
    }

    /** Large option prints ({@code minSize}/{@code expiry} nullable). Requires Alpha. */
    public JsonObject flowOptionBlocks(String symbol, Integer minSize, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minSize != null) params.put("minSize", String.valueOf(minSize));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/blocks", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionBlocks(String, Integer, String)}. */
    public FlowOptionBlocksResponse flowOptionBlocksTyped(String symbol, Integer minSize, String expiry) {
        return gson.fromJson(flowOptionBlocks(symbol, minSize, expiry), FlowOptionBlocksResponse.class);
    }

    /** Per-minute option-flow buckets ({@code minutes}/{@code expiry} nullable). Requires Alpha. */
    public JsonObject flowOptionHistory(String symbol, Integer minutes, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/history", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionHistory(String, Integer, String)}. */
    public FlowOptionHistoryResponse flowOptionHistoryTyped(String symbol, Integer minutes, String expiry) {
        return gson.fromJson(flowOptionHistory(symbol, minutes, expiry), FlowOptionHistoryResponse.class);
    }

    /** Cumulative option net-flow series ({@code minutes}/{@code expiry} nullable). Requires Alpha. */
    public JsonObject flowOptionCumulative(String symbol, Integer minutes, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/cumulative", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionCumulative(String, Integer, String)}. */
    public FlowOptionCumulativeResponse flowOptionCumulativeTyped(String symbol, Integer minutes, String expiry) {
        return gson.fromJson(flowOptionCumulative(symbol, minutes, expiry), FlowOptionCumulativeResponse.class);
    }

    /** Recent stock trades, newest-first ({@code limit} 1-500, nullable). Requires Alpha. */
    public JsonObject flowStockRecent(String symbol, Integer limit) {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        return get("/v1/flow/stocks/" + _seg(symbol) + "/recent", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStockRecent(String, Integer)}. */
    public FlowStockRecentResponse flowStockRecentTyped(String symbol, Integer limit) {
        return gson.fromJson(flowStockRecent(symbol, limit), FlowStockRecentResponse.class);
    }

    /** Per-symbol stock-flow aggregates. Requires the Alpha plan. */
    public JsonObject flowStockSummary(String symbol) {
        return get("/v1/flow/stocks/" + _seg(symbol) + "/summary", null);
    }

    /** Strongly-typed variant of {@link #flowStockSummary(String)}. */
    public FlowStockSummaryResponse flowStockSummaryTyped(String symbol) {
        return gson.fromJson(flowStockSummary(symbol), FlowStockSummaryResponse.class);
    }

    /** Large stock prints ({@code minSize} nullable). Requires the Alpha plan. */
    public JsonObject flowStockBlocks(String symbol, Integer minSize) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minSize != null) params.put("minSize", String.valueOf(minSize));
        return get("/v1/flow/stocks/" + _seg(symbol) + "/blocks", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStockBlocks(String, Integer)}. */
    public FlowStockBlocksResponse flowStockBlocksTyped(String symbol, Integer minSize) {
        return gson.fromJson(flowStockBlocks(symbol, minSize), FlowStockBlocksResponse.class);
    }

    /** Per-minute stock-flow buckets w/ OHLC ({@code minutes} nullable). Requires Alpha. */
    public JsonObject flowStockHistory(String symbol, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/stocks/" + _seg(symbol) + "/history", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStockHistory(String, Integer)}. */
    public FlowStockHistoryResponse flowStockHistoryTyped(String symbol, Integer minutes) {
        return gson.fromJson(flowStockHistory(symbol, minutes), FlowStockHistoryResponse.class);
    }

    /** Cumulative stock net-flow series ({@code minutes} nullable). Requires Alpha. */
    public JsonObject flowStockCumulative(String symbol, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/stocks/" + _seg(symbol) + "/cumulative", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStockCumulative(String, Integer)}. */
    public FlowStockCumulativeResponse flowStockCumulativeTyped(String symbol, Integer minutes) {
        return gson.fromJson(flowStockCumulative(symbol, minutes), FlowStockCumulativeResponse.class);
    }

    /** Cross-symbol option-flow leaderboard ({@code n}/{@code windowMinutes} nullable). Requires Alpha. */
    public JsonObject flowOptionsLeaderboard(Integer n, Integer windowMinutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (n != null) params.put("n", String.valueOf(n));
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        return get("/v1/flow/options/leaderboard", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionsLeaderboard(Integer, Integer)}. */
    public FlowOptionLeaderboardResponse flowOptionsLeaderboardTyped(Integer n, Integer windowMinutes) {
        return gson.fromJson(flowOptionsLeaderboard(n, windowMinutes), FlowOptionLeaderboardResponse.class);
    }

    /** Cross-symbol option-flow outliers ({@code limit}/{@code minTrades}/{@code windowMinutes} nullable). Requires Alpha. */
    public JsonObject flowOptionsOutliers(Integer limit, Integer minTrades, Integer windowMinutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (minTrades != null) params.put("minTrades", String.valueOf(minTrades));
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        return get("/v1/flow/options/outliers", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowOptionsOutliers(Integer, Integer, Integer)}. */
    public FlowOptionOutliersResponse flowOptionsOutliersTyped(Integer limit, Integer minTrades, Integer windowMinutes) {
        return gson.fromJson(flowOptionsOutliers(limit, minTrades, windowMinutes), FlowOptionOutliersResponse.class);
    }

    /** Cross-symbol stock-flow leaderboard ({@code n}/{@code windowMinutes} nullable). Requires Alpha. */
    public JsonObject flowStocksLeaderboard(Integer n, Integer windowMinutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (n != null) params.put("n", String.valueOf(n));
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        return get("/v1/flow/stocks/leaderboard", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStocksLeaderboard(Integer, Integer)}. */
    public FlowStockLeaderboardResponse flowStocksLeaderboardTyped(Integer n, Integer windowMinutes) {
        return gson.fromJson(flowStocksLeaderboard(n, windowMinutes), FlowStockLeaderboardResponse.class);
    }

    /** Cross-symbol stock-flow outliers ({@code limit}/{@code minTrades}/{@code windowMinutes} nullable). Requires Alpha. */
    public JsonObject flowStocksOutliers(Integer limit, Integer minTrades, Integer windowMinutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (minTrades != null) params.put("minTrades", String.valueOf(minTrades));
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        return get("/v1/flow/stocks/outliers", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowStocksOutliers(Integer, Integer, Integer)}. */
    public FlowStockOutliersResponse flowStocksOutliersTyped(Integer limit, Integer minTrades, Integer windowMinutes) {
        return gson.fromJson(flowStocksOutliers(limit, minTrades, windowMinutes), FlowStockOutliersResponse.class);
    }

    /**
     * Scored unusual-flow feed for one underlying. Requires the Alpha plan.
     * Each notable print is coalesced into a signal, classified
     * (block/sweep, NBBO aggressor, opening/closing bias, intent), and
     * scored 0–100 with a transparent component breakdown. Ranked highest
     * score first. All filter parameters are nullable.
     *
     * @param minScore      Drop signals below this 0–100 threshold.
     * @param intent        Filter to {@code "bullish"} / {@code "bearish"} /
     *                      {@code "neutral"}.
     * @param structure     Filter to {@code "block"} / {@code "sweep"}.
     * @param windowMinutes Look-back window in minutes (1–10080, default 240).
     * @param limit         Max signals returned (1–500, default 50).
     * @param expiry        Slice the chain to one expiration cycle
     *                      ({@code YYYY-MM-DD}).
     */
    public JsonObject flowSignals(String symbol, Integer minScore, String intent,
                                  String structure, Integer windowMinutes, Integer limit,
                                  String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (minScore != null) params.put("minScore", String.valueOf(minScore));
        if (intent != null) params.put("intent", intent);
        if (structure != null) params.put("structure", structure);
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/signals/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowSignals(String, Integer, String, String, Integer, Integer, String)}. */
    public FlowSignalsResponse flowSignalsTyped(String symbol, Integer minScore, String intent,
                                                String structure, Integer windowMinutes, Integer limit,
                                                String expiry) {
        return gson.fromJson(flowSignals(symbol, minScore, intent, structure, windowMinutes, limit, expiry),
                FlowSignalsResponse.class);
    }

    /**
     * Net bullish/bearish + opening/closing premium roll-up plus the top
     * 10 signals. Cheap "smart-money tilt" read. Requires the Alpha plan.
     * Both filter parameters are nullable.
     */
    public JsonObject flowSignalsSummary(String symbol, Integer windowMinutes, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/signals/" + _seg(symbol) + "/summary", params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #flowSignalsSummary(String, Integer, String)}. */
    public FlowSignalsSummaryResponse flowSignalsSummaryTyped(String symbol, Integer windowMinutes, String expiry) {
        return gson.fromJson(flowSignalsSummary(symbol, windowMinutes, expiry), FlowSignalsSummaryResponse.class);
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
     * Strongly-typed variant of {@link #iv(double, double, double, double, String, Double, Double)}.
     * Returns a populated {@link PricingIvResponse}. The original untyped
     * method is unchanged.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param price  Market price of the option.
     * @param type   Option type: {@code "call"} or {@code "put"}.
     * @param r      Risk-free rate (nullable).
     * @param q      Continuous dividend yield (nullable).
     */
    public PricingIvResponse ivTyped(double spot, double strike, double dte, double price,
                                     String type, Double r, Double q) {
        JsonObject raw = iv(spot, strike, dte, price, type, r, q);
        return gson.fromJson(raw, PricingIvResponse.class);
    }

    /**
     * Strongly-typed convenience overload of
     * {@link #ivTyped(double, double, double, double, String, Double, Double)}
     * with API-default rate / yield.
     *
     * @param spot   Current underlying price.
     * @param strike Option strike price.
     * @param dte    Days to expiration.
     * @param price  Market price of the option.
     * @param type   Option type: {@code "call"} or {@code "put"}.
     */
    public PricingIvResponse ivTyped(double spot, double strike, double dte, double price,
                                     String type) {
        return ivTyped(spot, strike, dte, price, type, null, null);
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

    /**
     * Strongly-typed variant of {@link #kelly(double, double, double, double, double, double, String, Double, Double)}.
     * Returns a populated {@link PricingKellyResponse}. The original untyped
     * method is unchanged.
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
    public PricingKellyResponse kellyTyped(double spot, double strike, double dte, double sigma,
                                           double premium, double mu, String type,
                                           Double r, Double q) {
        JsonObject raw = kelly(spot, strike, dte, sigma, premium, mu, type, r, q);
        return gson.fromJson(raw, PricingKellyResponse.class);
    }

    /**
     * Strongly-typed convenience overload of
     * {@link #kellyTyped(double, double, double, double, double, double, String, Double, Double)}
     * with API-default rate / yield.
     *
     * @param spot    Current underlying price.
     * @param strike  Option strike price.
     * @param dte     Days to expiration.
     * @param sigma   Annualised implied volatility.
     * @param premium Option premium paid.
     * @param mu      Expected annualised return of the underlying.
     * @param type    Option type: {@code "call"} or {@code "put"}.
     */
    public PricingKellyResponse kellyTyped(double spot, double strike, double dte, double sigma,
                                           double premium, double mu, String type) {
        return kellyTyped(spot, strike, dte, sigma, premium, mu, type, null, null);
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
     * Strongly-typed variant of {@link #tickers()}. Returns a populated
     * {@link TickersResponse}. The original untyped method is unchanged.
     */
    public TickersResponse tickersTyped() {
        JsonObject raw = tickers();
        return gson.fromJson(raw, TickersResponse.class);
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
     * Strongly-typed variant of {@link #options(String)}. Returns a populated
     * {@link OptionsMetaResponse}. The original untyped method is unchanged.
     *
     * @param ticker Stock ticker symbol.
     */
    public OptionsMetaResponse optionsTyped(String ticker) {
        JsonObject raw = options(ticker);
        return gson.fromJson(raw, OptionsMetaResponse.class);
    }

    /**
     * Currently queried symbols with live data available.
     */
    public JsonObject symbols() {
        return get("/v1/symbols");
    }

    /**
     * Strongly-typed variant of {@link #symbols()}. Returns a populated
     * {@link SymbolsResponse}. The original untyped method is unchanged.
     */
    public SymbolsResponse symbolsTyped() {
        JsonObject raw = symbols();
        return gson.fromJson(raw, SymbolsResponse.class);
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

    /**
     * Strongly-typed variant of {@link #screener(Map)}. Returns a populated
     * {@link ScreenerResponse} — {@code meta} is fully typed; {@code data}
     * rows are kept as raw {@link JsonObject} because the column shape
     * depends on the request {@code select} clause and tier.
     *
     * @param body Request body. See the Screener spec for the full schema.
     */
    public ScreenerResponse screenerTyped(Map<String, Object> body) {
        JsonObject raw = screener(body);
        return gson.fromJson(raw, ScreenerResponse.class);
    }

    /**
     * Strongly-typed variant of {@link #screener(Object)}.
     *
     * @param body Request body (POJO or {@link com.google.gson.JsonObject}).
     */
    public ScreenerResponse screenerTyped(Object body) {
        JsonObject raw = screener(body);
        return gson.fromJson(raw, ScreenerResponse.class);
    }

    // ── Account & System ──────────────────────────────────────────────

    /**
     * Account information and quota usage.
     */
    public JsonObject account() {
        return get("/v1/account");
    }

    /**
     * Strongly-typed variant of {@link #account()}. Returns a populated
     * {@link AccountResponse}. The original untyped method is unchanged.
     */
    public AccountResponse accountTyped() {
        JsonObject raw = account();
        return gson.fromJson(raw, AccountResponse.class);
    }

    /**
     * API health check (public endpoint, no auth required).
     */
    public JsonObject health() {
        return get("/health");
    }

    /**
     * Strongly-typed variant of {@link #health()}. Returns a populated
     * {@link HealthResponse}. The original untyped method is unchanged.
     */
    public HealthResponse healthTyped() {
        JsonObject raw = health();
        return gson.fromJson(raw, HealthResponse.class);
    }

    // ══════════════════════════════════════════════════════════════════
    //  v1.1 — new endpoint families
    //  (surface/svi, exposure sheet/term-structure/basket/oi-diff,
    //   liquidity, skew-term, spot-vol-correlation, dispersion, vix-state,
    //   universe, expected-move, vrp history, dealer-premium, zero-dte flow,
    //   flow stock bars, strategy signals ×10, earnings ×8, structures ×2,
    //   screener fields). All preserve back-compatibility: existing
    //   signatures are untouched; new params arrive as additional overloads.
    // ══════════════════════════════════════════════════════════════════

    // ── Market Data (additional) ──────────────────────────────────────

    /**
     * Live SVI-fitted volatility surface — calibrated {@code (a, b, rho, m, sigma)}
     * parameters per expiry slice with per-expiry forward, ATM total variance, and
     * ATM IV. Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject surfaceSvi(String symbol) {
        return get("/v1/surface/svi/" + _seg(symbol));
    }

    /** Strongly-typed variant of {@link #surfaceSvi(String)} → {@link SurfaceSviResponse}. */
    public SurfaceSviResponse surfaceSviTyped(String symbol) {
        return gson.fromJson(surfaceSvi(symbol), SurfaceSviResponse.class);
    }

    // ── Exposure Analytics (additional) ───────────────────────────────

    /**
     * Unified per-strike exposure sheet joining GEX/DEX/VEX/CHEX and DAG, with
     * chain totals, the Line-in-the-Sand inflection strike, gamma peaks, and
     * OPEX / triple-witching flags. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureSheet(String symbol) {
        return exposureSheet(symbol, null, null);
    }

    /**
     * Exposure sheet with optional expiration + minimum-OI filters.
     *
     * @param symbol     Underlying symbol.
     * @param expiration Expiration date filter (nullable).
     * @param minOi      Minimum open interest filter (nullable).
     */
    public JsonObject exposureSheet(String symbol, String expiration, Integer minOi) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiration != null) params.put("expiration", expiration);
        if (minOi != null) params.put("min_oi", String.valueOf(minOi));
        return get("/v1/exposure/sheet/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #exposureSheet(String)} → {@link ExposureSheetResponse}. */
    public ExposureSheetResponse exposureSheetTyped(String symbol) {
        return exposureSheetTyped(symbol, null, null);
    }

    /** Strongly-typed variant of {@link #exposureSheet(String, String, Integer)}. */
    public ExposureSheetResponse exposureSheetTyped(String symbol, String expiration, Integer minOi) {
        return gson.fromJson(exposureSheet(symbol, expiration, minOi), ExposureSheetResponse.class);
    }

    /**
     * Exposure term structure — net GEX/DEX/VEX/CHEX aggregated by DTE bucket and
     * rolled up per expiry. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject exposureTermStructure(String symbol) {
        return get("/v1/exposure/term-structure/" + _seg(symbol));
    }

    /** Strongly-typed variant of {@link #exposureTermStructure(String)} → {@link ExposureTermStructureResponse}. */
    public ExposureTermStructureResponse exposureTermStructureTyped(String symbol) {
        return gson.fromJson(exposureTermStructure(symbol), ExposureTermStructureResponse.class);
    }

    /**
     * Weighted cross-symbol exposure basket — aggregate net GEX/DEX/VEX/CHEX
     * across up to 50 symbols. Requires Growth+ plan.
     *
     * @param symbols Comma-separated symbol list (required), e.g. {@code "AAPL,MSFT,NVDA"}.
     * @param weights Comma-separated weights (nullable; equal-weighted when omitted).
     */
    public JsonObject exposureBasket(String symbols, String weights) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbols", symbols);
        if (weights != null) params.put("weights", weights);
        return get("/v1/exposure/basket", params);
    }

    /** Convenience overload — equal-weighted basket. */
    public JsonObject exposureBasket(String symbols) {
        return exposureBasket(symbols, null);
    }

    /** Strongly-typed variant of {@link #exposureBasket(String, String)} → {@link ExposureBasketResponse}. */
    public ExposureBasketResponse exposureBasketTyped(String symbols, String weights) {
        return gson.fromJson(exposureBasket(symbols, weights), ExposureBasketResponse.class);
    }

    /** Strongly-typed, equal-weighted variant of {@link #exposureBasket(String)}. */
    public ExposureBasketResponse exposureBasketTyped(String symbols) {
        return exposureBasketTyped(symbols, null);
    }

    /**
     * Day-over-day open-interest deltas (today minus prior trading day), top-N
     * changes by absolute magnitude, and call/put aggregate totals. Requires
     * Growth+ plan.
     *
     * @param symbol Underlying symbol.
     * @param topN   Number of top changes to return (nullable).
     */
    public JsonObject oiDiff(String symbol, Integer topN) {
        Map<String, String> params = new LinkedHashMap<>();
        if (topN != null) params.put("topN", String.valueOf(topN));
        return get("/v1/exposure/oi-diff/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Convenience overload — default top-N. */
    public JsonObject oiDiff(String symbol) {
        return oiDiff(symbol, null);
    }

    /** Strongly-typed variant of {@link #oiDiff(String, Integer)} → {@link OiDiffResponse}. */
    public OiDiffResponse oiDiffTyped(String symbol, Integer topN) {
        return gson.fromJson(oiDiff(symbol, topN), OiDiffResponse.class);
    }

    /** Strongly-typed variant of {@link #oiDiff(String)}. */
    public OiDiffResponse oiDiffTyped(String symbol) {
        return oiDiffTyped(symbol, null);
    }

    /**
     * Real-time 0DTE analytics with configurable strike range AND a specific
     * expiry. Back-compatible extension of {@link #zeroDte(String, Double)}.
     * Requires Growth+ plan.
     *
     * @param symbol      Underlying symbol.
     * @param strikeRange Percentage range around spot to include (nullable).
     * @param expiry      Expiration date to target ({@code yyyy-MM-dd}, nullable).
     */
    public JsonObject zeroDte(String symbol, Double strikeRange, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (strikeRange != null) params.put("strike_range", String.valueOf(strikeRange));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/exposure/zero-dte/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #zeroDte(String, Double, String)} → {@link ZeroDteResponse}. */
    public ZeroDteResponse zeroDteTyped(String symbol, Double strikeRange, String expiry) {
        return gson.fromJson(zeroDte(symbol, strikeRange, expiry), ZeroDteResponse.class);
    }

    // ── Volatility (additional) ───────────────────────────────────────

    /**
     * Per-expiry options execution / liquidity score (0-100) — ATM bid-ask
     * spread %, OI-weighted spread %, ATM OI depth, chain-level OI-weighted
     * score, and best/worst expiry. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject liquidity(String symbol) {
        return get("/v1/liquidity/" + _seg(symbol));
    }

    /** Strongly-typed variant of {@link #liquidity(String)} → {@link LiquidityResponse}. */
    public LiquidityResponse liquidityTyped(String symbol) {
        return gson.fromJson(liquidity(symbol), LiquidityResponse.class);
    }

    /**
     * Skew term structure with vol-desk naming — per expiry ATM IV, 25Δ / 10Δ
     * wing IVs, {@code skew_25d}, {@code risk_reversal_25d}, {@code butterfly_25d},
     * and {@code tail_convexity}. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject skewTerm(String symbol) {
        return get("/v1/volatility/skew-term/" + _seg(symbol));
    }

    /** Strongly-typed variant of {@link #skewTerm(String)} → {@link SkewTermResponse}. */
    public SkewTermResponse skewTermTyped(String symbol) {
        return gson.fromJson(skewTerm(symbol), SkewTermResponse.class);
    }

    /**
     * Spot-vol correlation — rolling correlation between spot returns and ATM IV
     * changes (the empirical leverage / spot-vol-beta signature). Requires
     * Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject spotVolCorrelation(String symbol) {
        return get("/v1/volatility/spot-vol-correlation/" + _seg(symbol));
    }

    /**
     * Implied-vs-realized correlation (dispersion / vol-arb math) between an
     * index and a user-supplied constituent basket, with per-constituent
     * contribution to basket vol. Requires Alpha+ plan.
     *
     * @param index       Index symbol (required), e.g. {@code "SPX"}.
     * @param symbols     Comma-separated constituent symbols (required, max 50).
     * @param weights     Comma-separated weights (nullable; equal-weighted when omitted).
     * @param horizonDays Realized-correlation lookback in days (nullable; default 20).
     */
    public JsonObject dispersion(String index, String symbols, String weights, Integer horizonDays) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("index", index);
        params.put("symbols", symbols);
        if (weights != null) params.put("weights", weights);
        if (horizonDays != null) params.put("horizon_days", String.valueOf(horizonDays));
        return get("/v1/dispersion", params);
    }

    /** Convenience overload — equal weights, default horizon. */
    public JsonObject dispersion(String index, String symbols) {
        return dispersion(index, symbols, null, null);
    }

    /**
     * Straddle-implied expected move per expiry, derived from ATM implied
     * volatility. Requires Basic+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject expectedMove(String symbol) {
        return expectedMove(symbol, null);
    }

    /**
     * Expected move filtered to a single expiry.
     *
     * @param symbol Underlying symbol.
     * @param expiry Expiration date filter ({@code yyyy-MM-dd}, nullable).
     */
    public JsonObject expectedMove(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/expected-move/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #expectedMove(String)} → {@link ExpectedMoveResponse}. */
    public ExpectedMoveResponse expectedMoveTyped(String symbol) {
        return expectedMoveTyped(symbol, null);
    }

    /** Strongly-typed variant of {@link #expectedMove(String, String)}. */
    public ExpectedMoveResponse expectedMoveTyped(String symbol, String expiry) {
        return gson.fromJson(expectedMove(symbol, expiry), ExpectedMoveResponse.class);
    }

    /**
     * Range-based realized (historical) volatility estimators over 10 / 20 /
     * 30-day windows — close-to-close, Parkinson, Garman-Klass,
     * Rogers-Satchell, and Yang-Zhang. All values annualised in percent.
     * Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject realizedVolatility(String symbol) {
        return get("/v1/volatility/realized/" + _seg(symbol));
    }

    /**
     * Strongly-typed variant of {@link #realizedVolatility(String)} →
     * {@link RealizedVolatilityResponse}. The original untyped method is
     * unchanged.
     *
     * @param symbol Underlying symbol.
     */
    public RealizedVolatilityResponse realizedVolatilityTyped(String symbol) {
        return gson.fromJson(realizedVolatility(symbol), RealizedVolatilityResponse.class);
    }

    /**
     * Conditional volatility forecasts from EWMA ({@code lambda = 0.94}),
     * HAR-RV, and GARCH(1,1) MLE — annualised vols, GARCH parameters,
     * persistence / half-life diagnostics, and a multi-horizon forecast
     * path. Uses the default Student-t innovation distribution. Requires
     * Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject volatilityForecast(String symbol) {
        return volatilityForecast(symbol, null);
    }

    /**
     * Volatility forecast with a selectable GARCH innovation distribution.
     *
     * @param symbol Underlying symbol.
     * @param dist   GARCH innovation distribution — {@code "student_t"}
     *               (default) or {@code "gaussian"} (nullable ⇒ student_t).
     */
    public JsonObject volatilityForecast(String symbol, String dist) {
        Map<String, String> params = new LinkedHashMap<>();
        if (dist != null) params.put("dist", dist);
        return get("/v1/volatility/forecast/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #volatilityForecast(String)} → {@link VolatilityForecastResponse}. */
    public VolatilityForecastResponse volatilityForecastTyped(String symbol) {
        return volatilityForecastTyped(symbol, null);
    }

    /** Strongly-typed variant of {@link #volatilityForecast(String, String)}. */
    public VolatilityForecastResponse volatilityForecastTyped(String symbol, String dist) {
        return gson.fromJson(volatilityForecast(symbol, dist), VolatilityForecastResponse.class);
    }

    // ── VRP (additional) ──────────────────────────────────────────────

    /**
     * Variance risk premium analytics for a specific historical {@code date}.
     * Back-compatible extension of {@link #vrp(String)}. Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     * @param date   Point-in-time date ({@code yyyy-MM-dd}, nullable ⇒ latest).
     */
    public JsonObject vrp(String symbol, String date) {
        Map<String, String> params = new LinkedHashMap<>();
        if (date != null) params.put("date", date);
        return get("/v1/vrp/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Strongly-typed variant of {@link #vrp(String, String)} → {@link VrpResponse}. */
    public VrpResponse vrpTyped(String symbol, String date) {
        return gson.fromJson(vrp(symbol, date), VrpResponse.class);
    }

    /**
     * VRP history — a trailing series of daily VRP snapshots. Requires Alpha+ plan.
     *
     * @param symbol Underlying symbol.
     * @param days   Number of trailing days (nullable).
     */
    public JsonObject vrpHistory(String symbol, Integer days) {
        Map<String, String> params = new LinkedHashMap<>();
        if (days != null) params.put("days", String.valueOf(days));
        return get("/v1/vrp/" + _seg(symbol) + "/history", params.isEmpty() ? null : params);
    }

    /** Convenience overload — default lookback. */
    public JsonObject vrpHistory(String symbol) {
        return vrpHistory(symbol, null);
    }

    // ── Macro / Universe ──────────────────────────────────────────────

    /** Composite VIX-state read (level, regime, term structure). Requires Growth+ plan. */
    public JsonObject vixState() {
        return get("/v1/macro/vix-state");
    }

    /**
     * Curated tier-1 / tier-2 symbol directory (the pre-warmed universe).
     * Public — no auth required.
     *
     * @param sort  {@code "tier"} or {@code "symbol"} (nullable).
     * @param limit Max rows, clamped to [1, 1000] (nullable).
     */
    public JsonObject universe(String sort, Integer limit) {
        Map<String, String> params = new LinkedHashMap<>();
        if (sort != null) params.put("sort", sort);
        if (limit != null) params.put("limit", String.valueOf(limit));
        return get("/v1/universe", params.isEmpty() ? null : params);
    }

    /** Convenience overload — default sort and limit. */
    public JsonObject universe() {
        return universe(null, null);
    }

    // ── Flow (additional) ─────────────────────────────────────────────

    /**
     * Full-tape Net Dealer Premium roll-up over a configurable window.
     * Requires the Alpha plan.
     *
     * @param symbol        Underlying symbol.
     * @param windowMinutes Look-back window in minutes (nullable; default 240).
     * @param expiry        Filter to a single expiry ({@code yyyy-MM-dd}, nullable).
     */
    public JsonObject flowDealerPremium(String symbol, Integer windowMinutes, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (windowMinutes != null) params.put("windowMinutes", String.valueOf(windowMinutes));
        if (expiry != null) params.put("expiry", expiry);
        return get("/v1/flow/options/" + _seg(symbol) + "/dealer-premium", params.isEmpty() ? null : params);
    }

    /** Convenience overload — default window, all expiries. */
    public JsonObject flowDealerPremium(String symbol) {
        return flowDealerPremium(symbol, null, null);
    }

    /**
     * Per-resolution stock-flow OHLC bars over a window. Requires the Alpha plan.
     *
     * @param symbol     Underlying symbol.
     * @param resolution Bar resolution (required): {@code 1s/1m/5m/15m/30m/1h/4h}.
     * @param minutes    Look-back window in minutes (nullable).
     */
    public JsonObject flowStockBars(String symbol, String resolution, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("resolution", resolution);
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/stocks/" + _seg(symbol) + "/bars", params);
    }

    // ── Zero-DTE Flow ─────────────────────────────────────────────────

    /**
     * Intraday 0DTE flow snapshot — same shape as {@link #zeroDte(String)} plus a
     * net {@code flow_direction}. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     */
    public JsonObject flowZeroDteSnapshot(String symbol) {
        return flowZeroDteSnapshot(symbol, null);
    }

    /**
     * Intraday 0DTE flow snapshot sliced to one expiration cycle. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     * @param expiry Expiration date filter, {@code YYYY-MM-DD} (nullable).
     */
    public JsonObject flowZeroDteSnapshot(String symbol, String expiry) {
        Map<String, String> params = new LinkedHashMap<>();
        if (expiry != null && !expiry.isBlank()) params.put("expiry", expiry);
        return get("/v1/flow/zero-dte/snapshot/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Intraday 0DTE time series of bucketed flow. Requires Growth+ plan.
     *
     * @param symbol  Underlying symbol.
     * @param bar     Bar size: {@code 30s/1m/5m/15m} (nullable).
     * @param minutes Look-back window in minutes (nullable).
     */
    public JsonObject flowZeroDteSeries(String symbol, String bar, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (bar != null) params.put("bar", bar);
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/zero-dte/series/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Intraday 0DTE dealer hedge-flow series. Requires Growth+ plan.
     *
     * @param symbol  Underlying symbol.
     * @param side    {@code all/calls/puts} (nullable).
     * @param bar     Bar size: {@code 30s/1m/5m/15m} (nullable).
     * @param minutes Look-back window in minutes (nullable).
     */
    public JsonObject flowZeroDteHedgeFlow(String symbol, String side, String bar, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (side != null) params.put("side", side);
        if (bar != null) params.put("bar", bar);
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/zero-dte/hedge-flow/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Intraday 0DTE strike × time heatmap of a chosen metric. Requires Alpha+ plan.
     *
     * @param symbol  Underlying symbol.
     * @param metric  {@code gex/dex/vex/chex/oi/signed_flow} (nullable).
     * @param mode    {@code raw/delta} (nullable).
     * @param bar     Bar size — {@code 1m} only (nullable).
     * @param minutes Look-back window in minutes (nullable).
     */
    public JsonObject flowZeroDteHeatmap(String symbol, String metric, String mode, String bar, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (metric != null) params.put("metric", metric);
        if (mode != null) params.put("mode", mode);
        if (bar != null) params.put("bar", bar);
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/zero-dte/heatmap/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Intraday 0DTE per-strike signed flow series. Requires Alpha+ plan.
     *
     * @param symbol  Underlying symbol.
     * @param bar     Bar size — {@code 1m} only (nullable).
     * @param minutes Look-back window in minutes (nullable).
     */
    public JsonObject flowZeroDteStrikeFlow(String symbol, String bar, Integer minutes) {
        Map<String, String> params = new LinkedHashMap<>();
        if (bar != null) params.put("bar", bar);
        if (minutes != null) params.put("minutes", String.valueOf(minutes));
        return get("/v1/flow/zero-dte/strike-flow/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /**
     * Cross-symbol 0DTE flow leaderboard (no symbol path segment). Requires Alpha+ plan.
     */
    public JsonObject flowZeroDteLeaderboard() {
        return flowZeroDteLeaderboard(null, null);
    }

    /**
     * Cross-symbol 0DTE flow leaderboard ranked by a chosen metric. Requires Alpha+ plan.
     *
     * @param metric {@code heat/pin_risk/abs_flow/charm_intensity} (nullable).
     * @param n      Number of symbols to return, 1–100 (nullable).
     */
    public JsonObject flowZeroDteLeaderboard(String metric, Integer n) {
        Map<String, String> params = new LinkedHashMap<>();
        if (metric != null) params.put("metric", metric);
        if (n != null) params.put("n", String.valueOf(n));
        return get("/v1/flow/zero-dte/leaderboard", params.isEmpty() ? null : params);
    }

    // ── Strategy Signals (×10) — shared StrategyDecisionResponse envelope ─

    private JsonObject strategy(String kind, String symbol, Map<String, String> params) {
        return get("/v1/strategies/" + kind + "/" + _seg(symbol), params == null || params.isEmpty() ? null : params);
    }

    private StrategyDecisionResponse strategyTyped(String kind, String symbol, Map<String, String> params) {
        return gson.fromJson(strategy(kind, symbol, params), StrategyDecisionResponse.class);
    }

    /** Directional flow-anomaly strategy signal. Requires Growth+ plan. */
    public JsonObject strategyFlowAnomaly(String symbol, String expiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        return strategy("flow-anomaly", symbol, p);
    }

    /** Directional flow-anomaly strategy signal (latest expiry). */
    public JsonObject strategyFlowAnomaly(String symbol) { return strategyFlowAnomaly(symbol, null); }

    /** Strongly-typed variant of {@link #strategyFlowAnomaly(String)} → {@link StrategyDecisionResponse}. */
    public StrategyDecisionResponse strategyFlowAnomalyTyped(String symbol) {
        return gson.fromJson(strategyFlowAnomaly(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyFlowAnomaly(String, String)}. */
    public StrategyDecisionResponse strategyFlowAnomalyTyped(String symbol, String expiry) {
        return gson.fromJson(strategyFlowAnomaly(symbol, expiry), StrategyDecisionResponse.class);
    }

    /**
     * OPEX pin / expiry-positioning strategy signal. Requires Basic+ plan.
     *
     * @param symbol           Underlying symbol.
     * @param expiry           Target expiry (nullable).
     * @param minOpenInterest  Minimum OI filter (nullable).
     * @param wingWidth        Wing width in strikes (nullable).
     */
    public JsonObject strategyExpiryPositioning(String symbol, String expiry, Integer minOpenInterest, Double wingWidth) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        if (minOpenInterest != null) p.put("minOpenInterest", String.valueOf(minOpenInterest));
        if (wingWidth != null) p.put("wingWidth", String.valueOf(wingWidth));
        return strategy("expiry-positioning", symbol, p);
    }

    /** Expiry-positioning strategy signal (defaults). */
    public JsonObject strategyExpiryPositioning(String symbol) { return strategyExpiryPositioning(symbol, null, null, null); }

    /** Strongly-typed variant of {@link #strategyExpiryPositioning(String)}. */
    public StrategyDecisionResponse strategyExpiryPositioningTyped(String symbol) {
        return gson.fromJson(strategyExpiryPositioning(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyExpiryPositioning(String, String, Integer, Double)}. */
    public StrategyDecisionResponse strategyExpiryPositioningTyped(String symbol, String expiry, Integer minOpenInterest, Double wingWidth) {
        return gson.fromJson(strategyExpiryPositioning(symbol, expiry, minOpenInterest, wingWidth), StrategyDecisionResponse.class);
    }

    /**
     * 0DTE range-compression strategy signal. Requires Growth+ plan (and the
     * 0DTE entitlement).
     *
     * @param symbol           Underlying symbol.
     * @param expiry           Target expiry (nullable).
     * @param minOpenInterest  Minimum OI filter (nullable).
     * @param wingWidth        Wing width in strikes (nullable).
     */
    public JsonObject strategyZeroDte(String symbol, String expiry, Integer minOpenInterest, Double wingWidth) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        if (minOpenInterest != null) p.put("minOpenInterest", String.valueOf(minOpenInterest));
        if (wingWidth != null) p.put("wingWidth", String.valueOf(wingWidth));
        return strategy("zero-dte", symbol, p);
    }

    /** 0DTE strategy signal (defaults). */
    public JsonObject strategyZeroDte(String symbol) { return strategyZeroDte(symbol, null, null, null); }

    /** Strongly-typed variant of {@link #strategyZeroDte(String)}. */
    public StrategyDecisionResponse strategyZeroDteTyped(String symbol) {
        return gson.fromJson(strategyZeroDte(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyZeroDte(String, String, Integer, Double)}. */
    public StrategyDecisionResponse strategyZeroDteTyped(String symbol, String expiry, Integer minOpenInterest, Double wingWidth) {
        return gson.fromJson(strategyZeroDte(symbol, expiry, minOpenInterest, wingWidth), StrategyDecisionResponse.class);
    }

    /** Dealer gamma-regime strategy signal. Requires Growth+ plan. */
    public JsonObject strategyDealerRegime(String symbol, String expiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        return strategy("dealer-regime", symbol, p);
    }

    /** Dealer-regime strategy signal (latest expiry). */
    public JsonObject strategyDealerRegime(String symbol) { return strategyDealerRegime(symbol, null); }

    /** Strongly-typed variant of {@link #strategyDealerRegime(String)}. */
    public StrategyDecisionResponse strategyDealerRegimeTyped(String symbol) {
        return gson.fromJson(strategyDealerRegime(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyDealerRegime(String, String)}. */
    public StrategyDecisionResponse strategyDealerRegimeTyped(String symbol, String expiry) {
        return gson.fromJson(strategyDealerRegime(symbol, expiry), StrategyDecisionResponse.class);
    }

    /**
     * Vol-carry / VRP strategy signal. Requires Alpha+ plan.
     *
     * @param symbol           Underlying symbol.
     * @param expiry           Target expiry (nullable).
     * @param minOpenInterest  Minimum OI filter (nullable).
     * @param targetShortDelta Target short-leg delta (nullable).
     * @param maxWidth         Max spread width (nullable).
     * @param minCredit        Minimum net credit (nullable).
     */
    public JsonObject strategyVolCarry(String symbol, String expiry, Integer minOpenInterest,
                                       Double targetShortDelta, Double maxWidth, Double minCredit) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        if (minOpenInterest != null) p.put("minOpenInterest", String.valueOf(minOpenInterest));
        if (targetShortDelta != null) p.put("targetShortDelta", String.valueOf(targetShortDelta));
        if (maxWidth != null) p.put("maxWidth", String.valueOf(maxWidth));
        if (minCredit != null) p.put("minCredit", String.valueOf(minCredit));
        return strategy("vol-carry", symbol, p);
    }

    /** Vol-carry strategy signal (defaults). */
    public JsonObject strategyVolCarry(String symbol) { return strategyVolCarry(symbol, null, null, null, null, null); }

    /** Strongly-typed variant of {@link #strategyVolCarry(String)}. */
    public StrategyDecisionResponse strategyVolCarryTyped(String symbol) {
        return gson.fromJson(strategyVolCarry(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyVolCarry(String, String, Integer, Double, Double, Double)}. */
    public StrategyDecisionResponse strategyVolCarryTyped(String symbol, String expiry, Integer minOpenInterest,
                                                          Double targetShortDelta, Double maxWidth, Double minCredit) {
        return gson.fromJson(strategyVolCarry(symbol, expiry, minOpenInterest, targetShortDelta, maxWidth, minCredit),
                StrategyDecisionResponse.class);
    }

    /**
     * Yield-enhancement (covered-call / cash-secured-put) strategy signal.
     * Requires Growth+ plan.
     *
     * @param symbol                       Underlying symbol.
     * @param expiry                       Target expiry (nullable).
     * @param targetDelta                  Target short-leg delta (nullable).
     * @param minOpenInterest              Minimum OI filter (nullable).
     * @param structure                    Structure preference (nullable).
     * @param excludeEarningsBeforeExpiry  Skip names with earnings before expiry (nullable).
     */
    public JsonObject strategyYieldEnhancement(String symbol, String expiry, Double targetDelta,
                                               Integer minOpenInterest, String structure,
                                               Boolean excludeEarningsBeforeExpiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        if (targetDelta != null) p.put("targetDelta", String.valueOf(targetDelta));
        if (minOpenInterest != null) p.put("minOpenInterest", String.valueOf(minOpenInterest));
        if (structure != null) p.put("structure", structure);
        if (excludeEarningsBeforeExpiry != null) p.put("excludeEarningsBeforeExpiry", String.valueOf(excludeEarningsBeforeExpiry));
        return strategy("yield-enhancement", symbol, p);
    }

    /** Yield-enhancement strategy signal (defaults). */
    public JsonObject strategyYieldEnhancement(String symbol) {
        return strategyYieldEnhancement(symbol, null, null, null, null, null);
    }

    /** Strongly-typed variant of {@link #strategyYieldEnhancement(String)}. */
    public StrategyDecisionResponse strategyYieldEnhancementTyped(String symbol) {
        return gson.fromJson(strategyYieldEnhancement(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyYieldEnhancement(String, String, Double, Integer, String, Boolean)}. */
    public StrategyDecisionResponse strategyYieldEnhancementTyped(String symbol, String expiry, Double targetDelta,
                                                                  Integer minOpenInterest, String structure,
                                                                  Boolean excludeEarningsBeforeExpiry) {
        return gson.fromJson(strategyYieldEnhancement(symbol, expiry, targetDelta, minOpenInterest, structure,
                excludeEarningsBeforeExpiry), StrategyDecisionResponse.class);
    }

    /** SVI surface-anomaly strategy signal. Requires Alpha+ plan. */
    public JsonObject strategySurfaceAnomaly(String symbol, String expiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        return strategy("surface-anomaly", symbol, p);
    }

    /** Surface-anomaly strategy signal (latest expiry). */
    public JsonObject strategySurfaceAnomaly(String symbol) { return strategySurfaceAnomaly(symbol, null); }

    /** Strongly-typed variant of {@link #strategySurfaceAnomaly(String)}. */
    public StrategyDecisionResponse strategySurfaceAnomalyTyped(String symbol) {
        return gson.fromJson(strategySurfaceAnomaly(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategySurfaceAnomaly(String, String)}. */
    public StrategyDecisionResponse strategySurfaceAnomalyTyped(String symbol, String expiry) {
        return gson.fromJson(strategySurfaceAnomaly(symbol, expiry), StrategyDecisionResponse.class);
    }

    /** 25-delta skew strategy signal. Requires Growth+ plan. */
    public JsonObject strategySkew(String symbol, String expiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        return strategy("skew", symbol, p);
    }

    /** Skew strategy signal (latest expiry). */
    public JsonObject strategySkew(String symbol) { return strategySkew(symbol, null); }

    /** Strongly-typed variant of {@link #strategySkew(String)}. */
    public StrategyDecisionResponse strategySkewTyped(String symbol) {
        return gson.fromJson(strategySkew(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategySkew(String, String)}. */
    public StrategyDecisionResponse strategySkewTyped(String symbol, String expiry) {
        return gson.fromJson(strategySkew(symbol, expiry), StrategyDecisionResponse.class);
    }

    /** ATM term-structure strategy signal. Requires Growth+ plan. */
    public JsonObject strategyTermStructure(String symbol) {
        return strategy("term-structure", symbol, null);
    }

    /** Strongly-typed variant of {@link #strategyTermStructure(String)}. */
    public StrategyDecisionResponse strategyTermStructureTyped(String symbol) {
        return gson.fromJson(strategyTermStructure(symbol), StrategyDecisionResponse.class);
    }

    /** Downside-tail-pricing strategy signal. Requires Growth+ plan. */
    public JsonObject strategyTailPricing(String symbol, String expiry) {
        Map<String, String> p = new LinkedHashMap<>();
        if (expiry != null) p.put("expiry", expiry);
        return strategy("tail-pricing", symbol, p);
    }

    /** Tail-pricing strategy signal (latest expiry). */
    public JsonObject strategyTailPricing(String symbol) { return strategyTailPricing(symbol, null); }

    /** Strongly-typed variant of {@link #strategyTailPricing(String)}. */
    public StrategyDecisionResponse strategyTailPricingTyped(String symbol) {
        return gson.fromJson(strategyTailPricing(symbol), StrategyDecisionResponse.class);
    }

    /** Strongly-typed variant of {@link #strategyTailPricing(String, String)}. */
    public StrategyDecisionResponse strategyTailPricingTyped(String symbol, String expiry) {
        return gson.fromJson(strategyTailPricing(symbol, expiry), StrategyDecisionResponse.class);
    }

    // ── Earnings (×8) ─────────────────────────────────────────────────

    /**
     * Upcoming earnings calendar over a forward window. Requires Growth+ plan.
     *
     * @param days       Forward window in days (nullable; default 14).
     * @param symbols    Comma-separated symbol filter (nullable).
     * @param importance Minimum importance rating (nullable).
     */
    public JsonObject earningsCalendar(Integer days, String symbols, Integer importance) {
        Map<String, String> params = new LinkedHashMap<>();
        if (days != null) params.put("days", String.valueOf(days));
        if (symbols != null) params.put("symbols", symbols);
        if (importance != null) params.put("importance", String.valueOf(importance));
        return get("/v1/earnings/calendar", params.isEmpty() ? null : params);
    }

    /** Earnings calendar (defaults). */
    public JsonObject earningsCalendar() { return earningsCalendar(null, null, null); }

    /** Straddle-implied earnings expected move for one symbol. Requires Growth+ plan. */
    public JsonObject earningsExpectedMove(String symbol) {
        return get("/v1/earnings/expected-move/" + _seg(symbol));
    }

    /**
     * Historical post-earnings move history for one symbol. Requires Growth+ plan.
     *
     * @param symbol Underlying symbol.
     * @param limit  Max prior events (nullable).
     */
    public JsonObject earningsHistory(String symbol, Integer limit) {
        Map<String, String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        return get("/v1/earnings/history/" + _seg(symbol), params.isEmpty() ? null : params);
    }

    /** Earnings history (default limit). */
    public JsonObject earningsHistory(String symbol) { return earningsHistory(symbol, null); }

    /** Earnings IV-crush profile for one symbol. Requires Growth+ plan. */
    public JsonObject earningsIvCrush(String symbol) {
        return get("/v1/earnings/iv-crush/" + _seg(symbol));
    }

    /** Earnings variance-risk-premium read for one symbol. Requires Alpha+ plan. */
    public JsonObject earningsVrp(String symbol) {
        return get("/v1/earnings/vrp/" + _seg(symbol));
    }

    /** Earnings dealer-positioning read for one symbol. Requires Alpha+ plan. */
    public JsonObject earningsDealerPositioning(String symbol) {
        return get("/v1/earnings/dealer-positioning/" + _seg(symbol));
    }

    /** Suggested earnings option structures for one symbol. Requires Alpha+ plan. */
    public JsonObject earningsStrategies(String symbol) {
        return get("/v1/earnings/strategies/" + _seg(symbol));
    }

    /**
     * Cross-sectional earnings screener ranked by VRP richness, cheapest move,
     * highest crush, or importance. Requires Alpha+ plan.
     *
     * @param sort          Ranking key (nullable; default {@code vrp_richest}).
     * @param limit         Max rows, 1-50 (nullable).
     * @param days          Forward window in days (nullable).
     * @param minImportance Minimum importance rating (nullable).
     */
    public JsonObject earningsScreener(String sort, Integer limit, Integer days, Integer minImportance) {
        Map<String, String> params = new LinkedHashMap<>();
        if (sort != null) params.put("sort", sort);
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (days != null) params.put("days", String.valueOf(days));
        if (minImportance != null) params.put("min_importance", String.valueOf(minImportance));
        return get("/v1/earnings/screener", params.isEmpty() ? null : params);
    }

    /** Earnings screener (defaults). */
    public JsonObject earningsScreener() { return earningsScreener(null, null, null, null); }

    // ── Structures (POST, pure-math) ──────────────────────────────────

    /**
     * At-expiry P&amp;L curve, breakevens, and max profit/loss for an arbitrary
     * multi-leg structure. Pure math — no symbol resolution. Requires Basic+ plan.
     *
     * @param request Legs + optional underlying-price range and sample count.
     */
    public JsonObject structurePnl(StructureRequest request) {
        return post("/v1/structures/pnl", request);
    }

    /** Strongly-typed variant of {@link #structurePnl(StructureRequest)} → {@link StructurePnlResponse}. */
    public StructurePnlResponse structurePnlTyped(StructureRequest request) {
        return gson.fromJson(structurePnl(request), StructurePnlResponse.class);
    }

    /**
     * Aggregate Black-Scholes Greeks across a multi-leg position (each leg carries
     * its own expiry + implied vol). Pure math. Requires Basic+ plan.
     *
     * @param request Legs + spot + optional valuation date / rate / dividend yield.
     */
    public JsonObject structureGreeks(StructureGreeksRequest request) {
        return post("/v1/structures/greeks", request);
    }

    /** Strongly-typed variant of {@link #structureGreeks(StructureGreeksRequest)} → {@link StructureGreeksResponse}. */
    public StructureGreeksResponse structureGreeksTyped(StructureGreeksRequest request) {
        return gson.fromJson(structureGreeks(request), StructureGreeksResponse.class);
    }

    // ── Screener (additional) ─────────────────────────────────────────

    /** Screener field catalogue (every selectable / filterable field). Requires any key (Free+). */
    public JsonObject screenerFields() {
        return get("/v1/screener/fields");
    }
}
