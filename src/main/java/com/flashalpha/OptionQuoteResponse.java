package com.flashalpha;

import com.google.gson.annotations.SerializedName;

/**
 * Typed response model for {@code GET /optionquote/{ticker}} (Growth+, live only).
 *
 * <p>One option contract's bid / ask / mid, sizes, last update, full
 * greeks (incl. vanna and charm), open interest, volume, IV (and
 * IV-bid / IV-ask), and an optional SVI-fitted vol.
 *
 * <p><b>Camel-case fields:</b> {@code lastUpdate}, {@code bidSize},
 * {@code askSize} are camelCase on the wire (unlike most FlashAlpha
 * payloads which are snake_case).
 *
 * <p>The list endpoint variant returns an array of these objects; the
 * single-contract variant (all of {@code expiry} + {@code strike} +
 * {@code type} supplied) returns a single object.
 */
public final class OptionQuoteResponse {

    /** Option type: {@code "call"} or {@code "put"}. */
    @SerializedName("type")
    public String type;

    /** Expiration date, e.g. {@code "2024-01-19"}. */
    @SerializedName("expiry")
    public String expiry;

    @SerializedName("strike")
    public Double strike;

    @SerializedName("bid")
    public Double bid;

    @SerializedName("ask")
    public Double ask;

    /** {@code (bid + ask) / 2}. */
    @SerializedName("mid")
    public Double mid;

    /** camelCase on the wire. */
    @SerializedName("bidSize")
    public Long bidSize;

    /** camelCase on the wire. */
    @SerializedName("askSize")
    public Long askSize;

    /** camelCase on the wire — last quote update, ET wall-clock. */
    @SerializedName("lastUpdate")
    public String lastUpdate;

    /** Implied volatility from the mid (annualised, decimal). */
    @SerializedName("implied_vol")
    public Double impliedVol;

    /** Implied volatility evaluated at the bid. */
    @SerializedName("iv_bid")
    public Double ivBid;

    /** Implied volatility evaluated at the ask. */
    @SerializedName("iv_ask")
    public Double ivAsk;

    @SerializedName("delta") public Double delta;
    @SerializedName("gamma") public Double gamma;
    @SerializedName("theta") public Double theta;
    @SerializedName("vega")  public Double vega;
    @SerializedName("rho")   public Double rho;
    @SerializedName("vanna") public Double vanna;
    @SerializedName("charm") public Double charm;

    /** SVI-fitted implied volatility. {@code null} when surface fit is unavailable. */
    @SerializedName("svi_vol")
    public Double sviVol;

    /** Gating reason for SVI vol availability — surfaced as a string label. */
    @SerializedName("svi_vol_gated")
    public String sviVolGated;

    @SerializedName("open_interest")
    public Long openInterest;

    @SerializedName("volume")
    public Long volume;

    /** Underlying ticker. Optional — present on some response shapes. */
    @SerializedName("underlying")
    public String underlying;
}
