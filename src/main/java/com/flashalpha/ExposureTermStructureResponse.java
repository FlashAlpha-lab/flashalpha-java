package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/exposure/term-structure/{symbol}} (Growth+) —
 * per-greek exposure (GEX / DEX / VEX / CHEX) aggregated by DTE bucket and also
 * rolled up per expiry, so one call returns the same shape as four full-chain
 * {@code /v1/exposure/*} calls grouped by time.
 *
 * <p>Obtain via {@link FlashAlphaClient#exposureTermStructureTyped(String)}.
 */
public final class ExposureTermStructureResponse {

    /** Resolved underlying symbol. */
    @SerializedName("symbol")
    public String symbol;

    /** Spot price used for the aggregation. */
    @SerializedName("underlying_price")
    public Double underlyingPrice;

    /** ISO 8601 UTC build time. */
    @SerializedName("as_of")
    public String asOf;

    /** Net greeks aggregated by DTE bucket ({@code 0-7d}, {@code 8-30d}, {@code 31-60d}, {@code 61-180d}, {@code 180d+}). */
    @SerializedName("by_dte_bucket")
    public List<DteBucket> byDteBucket;

    /** Net greeks rolled up per expiry, ascending. */
    @SerializedName("by_expiry")
    public List<ExpiryRow> byExpiry;

    /** One DTE-bucket aggregate. */
    public static final class DteBucket {
        /** Bucket name, e.g. {@code "0-7d"}. */
        @SerializedName("bucket") public String bucket;
        /** Inclusive {@code [lower, upper]} DTE bounds (final bucket carries {@code int.MaxValue}). */
        @SerializedName("dte_range") public List<Long> dteRange;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        /** Number of contracts feeding the bucket. */
        @SerializedName("contract_count") public Integer contractCount;
    }

    /** One per-expiry row. */
    public static final class ExpiryRow {
        @SerializedName("expiration") public String expiration;
        @SerializedName("dte") public Integer dte;
        @SerializedName("is_opex") public Boolean isOpex;
        @SerializedName("is_triple_witching") public Boolean isTripleWitching;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("net_dex") public Double netDex;
        @SerializedName("net_vex") public Double netVex;
        @SerializedName("net_chex") public Double netChex;
        /** This expiry's {@code |net_gex|} as a share of total chain GEX (0-100). */
        @SerializedName("pct_of_chain_gex") public Double pctOfChainGex;
    }
}
