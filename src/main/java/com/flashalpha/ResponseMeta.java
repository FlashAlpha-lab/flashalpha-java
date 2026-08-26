package com.flashalpha;

import com.google.gson.Gson;

import java.net.http.HttpHeaders;

/**
 * The envelope for endpoints that return a bare JSON array.
 *
 * <p>Those responses have nowhere to put an envelope in the body, so the API sends it in
 * the {@code X-Data-As-Of} and {@code X-Endpoint-Version} headers instead. The
 * {@code *WithMetadata} accessors return this alongside the decoded body, which is the
 * only way to reach provenance for such an endpoint.
 */
public final class ResponseMeta {

    /** Identifies the deployment that produced the response. Null if the header was absent. */
    public final String endpointVersion;

    /** Per-feed freshness, or null if the header was absent or unparseable. */
    public final DataAsOf dataAsOf;

    ResponseMeta(String endpointVersion, DataAsOf dataAsOf) {
        this.endpointVersion = endpointVersion;
        this.dataAsOf = dataAsOf;
    }

    /**
     * Reads the envelope headers.
     *
     * <p>A malformed or absent {@code X-Data-As-Of} leaves {@link #dataAsOf} null rather
     * than failing the call: provenance is diagnostic, and losing it should never turn a
     * good response into an error.
     */
    static ResponseMeta from(HttpHeaders headers, Gson gson) {
        String version = headers.firstValue("X-Endpoint-Version").orElse(null);
        DataAsOf asOf = null;
        String raw = headers.firstValue("X-Data-As-Of").orElse(null);
        if (raw != null && !raw.isBlank()) {
            try {
                asOf = gson.fromJson(raw, DataAsOf.class);
            } catch (RuntimeException ignored) {
                // leave null
            }
        }
        return new ResponseMeta(version, asOf);
    }
}
