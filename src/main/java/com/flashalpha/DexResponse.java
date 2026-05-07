package com.flashalpha;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Typed response model for {@code GET /v1/exposure/dex/{symbol}}.
 *
 * <p>Strike-by-strike net dealer delta exposure (DEX) plus the headline
 * {@link #netDex} scalar.
 */
public final class DexResponse {

    @SerializedName("symbol")
    public String symbol;

    @SerializedName("underlying_price")
    public Double underlyingPrice;

    @SerializedName("as_of")
    public String asOf;

    /** Sum of {@code DexStrikeRow.netDex} across all strikes. */
    @SerializedName("net_dex")
    public Double netDex;

    /** Per-strike DEX breakdown rows. */
    @SerializedName("strikes")
    public List<DexStrikeRow> strikes;

    /** One strike of the DEX breakdown. */
    public static final class DexStrikeRow {
        @SerializedName("strike") public Double strike;
        /** Dealer delta exposure from calls at this strike. */
        @SerializedName("call_dex") public Double callDex;
        /** Dealer delta exposure from puts at this strike. */
        @SerializedName("put_dex") public Double putDex;
        /** {@code call_dex + put_dex}. */
        @SerializedName("net_dex") public Double netDex;
    }
}
