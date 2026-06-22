package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response for {@code GET /v1/earnings/dealer-positioning/{symbol}} — dealer
 * exposure analysis scoped to the earnings event: gamma flip / call &amp; put walls on
 * event-week expiries, net GEX bucketed by pre-event / event-week / post-event, charm
 * acceleration into the event, and the top strikes by absolute net GEX. Requires Alpha+.
 *
 * <p>Obtain via {@link FlashAlphaClient#earningsDealerPositioningTyped(String)}.
 */
public final class EarningsDealerPositioningResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    @SerializedName("earnings_date")
    public String earningsDate;

    /** Closest options expiry on or after the earnings date; {@code null} if none found. */
    @SerializedName("event_expiry")
    public String eventExpiry;

    @SerializedName("levels")
    public EarningsDealerLevels levels;

    @SerializedName("gex_by_dte_bucket")
    public List<EarningsGexBucket> gexByDteBucket;

    /** Up to 5 strikes ranked by absolute net GEX. */
    @SerializedName("top_strikes")
    public List<EarningsTopStrike> topStrikes;

    /** Ratio of event-expiry CHEX to full-chain CHEX; {@code null} when not computable. */
    @SerializedName("charm_acceleration")
    public Double charmAcceleration;

    /** {@code positive_gamma} / {@code negative_gamma} / {@code undetermined}. */
    @SerializedName("regime")
    public String regime;

    /** The {@code levels} block. */
    public static final class EarningsDealerLevels {
        @SerializedName("gamma_flip") public Double gammaFlip;
        @SerializedName("call_wall") public Double callWall;
        @SerializedName("put_wall") public Double putWall;
        @SerializedName("highest_oi_strike") public Double highestOiStrike;
    }

    /** One DTE bucket inside {@link #gexByDteBucket}. */
    public static final class EarningsGexBucket {
        /** {@code pre_event} / {@code event_week} / {@code post_event}. */
        @SerializedName("bucket") public String bucket;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("contract_count") public Integer contractCount;
    }

    /** One strike inside {@link #topStrikes}. */
    public static final class EarningsTopStrike {
        @SerializedName("strike") public Double strike;
        @SerializedName("net_gex") public Double netGex;
        @SerializedName("call_oi") public Long callOi;
        @SerializedName("put_oi") public Long putOi;
    }
}
