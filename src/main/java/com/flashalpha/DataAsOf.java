package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * When each upstream feed last delivered to the node that served the response.
 *
 * <p>Present on every successful response as {@code data_as_of}. The shape is fixed:
 * every field exists on every endpoint, and a field is {@code null} when that node has
 * not received anything on that feed since it started.
 *
 * <p>Spot and options are reported separately because they arrive over different pipes
 * and fail independently - an index chain can be current while the index level behind
 * it is not, and one timestamp cannot express that.
 *
 * <p>Read each feed against its OWN cadence rather than against {@code as_of}.
 * {@link #oiFeed} dated to the previous session's close is correct, because settled
 * open interest is published once per session: on a Monday the newest figure that
 * exists is Friday's. {@link #equityOptionsFeed} an hour behind during the regular
 * session is not correct.
 *
 * <p>A timestamp evidences that the feed delivered recently. It does not assert that
 * every contract in a chain is equally current: an illiquid strike may not have quoted
 * for hours while its feed is healthy.
 */
public final class DataAsOf {

    /** Which node answered. Nodes hydrate independently, so their feeds can differ. */
    @SerializedName("node")
    public String node;

    /** Equity and ETF spot quotes. Ticks in seconds during market hours. */
    @SerializedName("equity_feed")
    public String equityFeed;

    /** Equity and ETF option quotes. Ticks in seconds during market hours. */
    @SerializedName("equity_options_feed")
    public String equityOptionsFeed;

    /** Index spot - SPX, RUT, VIX and the other index roots. Ticks in seconds during market hours. */
    @SerializedName("index_feed")
    public String indexFeed;

    /** Index option quotes. Ticks in seconds during market hours. */
    @SerializedName("index_options_feed")
    public String indexOptionsFeed;

    /** Futures prices. Ticks in seconds during the futures session. */
    @SerializedName("futures_feed")
    public String futuresFeed;

    /** Futures option quotes. Ticks in seconds during the futures session. */
    @SerializedName("futures_options_feed")
    public String futuresOptionsFeed;

    /** Classified options and stock trade tape. Ticks in seconds during market hours. */
    @SerializedName("flow_feed")
    public String flowFeed;

    /**
     * Settled open interest, dated to the prior 16:00 ET close. Published once per
     * session, so trailing {@code as_of} by a day - or by three across a weekend - is
     * correct rather than stale.
     */
    @SerializedName("oi_feed")
    public String oiFeed;

    /**
     * VIX, VVIX, SKEW, MOVE, SPX and Fear &amp; Greed. Unlike the other feeds this
     * reports its OLDEST component, because these are independent series rather than
     * one pipe.
     */
    @SerializedName("macro_feed")
    public String macroFeed;
}
