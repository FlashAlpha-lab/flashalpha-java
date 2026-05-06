package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/exposure/narrative/{symbol}} (Growth+).
 *
 * <p>FlashAlpha's <b>LLM-friendly verbal output</b> for dealer-flow
 * analytics. Every string field inside {@link #narrative} is a
 * server-side, plain-English explanation of the regime / GEX change /
 * key levels / flow / vanna / charm / 0DTE / forward outlook — and is
 * <i>safe to surface verbatim</i> in chat UIs, agent transcripts, or
 * end-user dashboards. The {@link Narrative#data} sub-block carries
 * the raw numbers behind each string so callers can build their own
 * visualisations without re-querying the underlying analytics
 * endpoints.
 *
 * <p>About FlashAlpha: real-time options dealer-flow analytics. See
 * <a href="https://lab.flashalpha.com">https://lab.flashalpha.com</a>
 * and <a href="https://flashalpha.com">https://flashalpha.com</a>.
 */
public final class NarrativeResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** Narrative + raw data block. Always populated. */
    @SerializedName("narrative")
    public Narrative narrative;

    /**
     * The verbal-output block. Each string here is server-generated
     * and safe to render verbatim — no client-side templating needed.
     */
    public static final class Narrative {
        /** Plain-English description of the dealer gamma regime. */
        @SerializedName("regime") public String regime;
        /** Plain-English summary of the day-over-day GEX change. */
        @SerializedName("gex_change") public String gexChange;
        /** Narrative for call/put walls, gamma flip, and the OI landscape. */
        @SerializedName("key_levels") public String keyLevels;
        /** Narrative for the volume / OI / put-call flow picture. */
        @SerializedName("flow") public String flow;
        /** Narrative for vanna positioning (IV → spot transmission). */
        @SerializedName("vanna") public String vanna;
        /** Narrative for charm decay (delta drift to expiry). */
        @SerializedName("charm") public String charm;
        /** Narrative for the same-day-expiration fragment of dealer flow. */
        @SerializedName("zero_dte") public String zeroDte;
        /** Forward-looking outlook stitched from the above components. */
        @SerializedName("outlook") public String outlook;

        /** Raw numbers backing each string above. */
        @SerializedName("data") public NarrativeData data;
    }

    /**
     * Numeric backing for each narrative string. Every field here is
     * derived from the underlying exposure / VIX / 0DTE pipelines and
     * is the value the verbal block was generated from.
     */
    public static final class NarrativeData {
        /** Net dealer gamma exposure (dollars per 1% spot move). */
        @SerializedName("net_gex") public Double netGex;
        /** Prior-day net GEX — basis for {@link #netGexChangePct}. */
        @SerializedName("net_gex_prior") public Double netGexPrior;
        /** Day-over-day GEX change as a percent of {@link #netGexPrior}. */
        @SerializedName("net_gex_change_pct") public Double netGexChangePct;
        /** CBOE VIX index level. */
        @SerializedName("vix") public Double vix;
        /** Strike where net dealer gamma crosses zero. */
        @SerializedName("gamma_flip") public Double gammaFlip;
        /** Strike with the highest absolute call GEX. */
        @SerializedName("call_wall") public Double callWall;
        /** Strike with the highest absolute put GEX. */
        @SerializedName("put_wall") public Double putWall;
        /**
         * Dealer gamma regime label —
         * {@code "positive_gamma"} | {@code "negative_gamma"} |
         * {@code "undetermined"}.
         */
        @SerializedName("regime") public String regime;
        /** 0DTE share of total chain GEX (0-100). */
        @SerializedName("zero_dte_pct") public Double zeroDtePct;
        /** Top strikes by absolute OI change vs prior session. */
        @SerializedName("top_oi_changes") public List<OiChangeRow> topOiChanges;
    }

    /** One row of the top-OI-changes list — shows where positioning shifted overnight. */
    public static final class OiChangeRow {
        @SerializedName("strike") public Double strike;
        /** {@code "call"} or {@code "put"}. */
        @SerializedName("type") public String type;
        /** Signed change in OI vs prior session. Positive = new positioning. */
        @SerializedName("oi_change") public Long oiChange;
        /** Today's contract volume at this strike/type. */
        @SerializedName("volume") public Long volume;
    }
}
