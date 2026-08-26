package com.flashalpha;

import com.google.gson.Gson;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The envelope lives on {@link FlashAlphaResponse} rather than being repeated on each
 * of the 75 response models. That is only sound if the base actually binds through
 * Gson, so these tests exercise the binding rather than the declaration - a model that
 * quietly stopped extending the base would still compile.
 */
public class ResponseEnvelopeTest {

    private static final String BODY = "{"
            + "\"symbol\":\"SPY\","
            + "\"net_gex\":1234.5,"
            + "\"endpoint_version\":\"2026.08.25\","
            + "\"data_as_of\":{"
            + "\"node\":\"fa2\","
            + "\"equity_feed\":\"2026-08-25T18:48:58.204Z\","
            + "\"equity_options_feed\":\"2026-08-25T18:48:57.900Z\","
            + "\"index_feed\":null,"
            + "\"index_options_feed\":null,"
            + "\"futures_feed\":null,"
            + "\"futures_options_feed\":null,"
            + "\"flow_feed\":\"2026-08-25T18:48:55.100Z\","
            + "\"oi_feed\":\"2026-08-24T20:00:00.000Z\","
            + "\"macro_feed\":\"2026-08-25T18:45:00.000Z\""
            + "}}";

    private final Gson gson = new Gson();

    @Test
    public void envelopeBindsThroughTheBaseClass() {
        GexResponse gex = gson.fromJson(BODY, GexResponse.class);

        assertEquals("SPY", gex.symbol);
        assertEquals("2026.08.25", gex.endpointVersion);
        assertNotNull("data_as_of did not bind through the base class", gex.dataAsOf);
        assertEquals("fa2", gex.dataAsOf.node);
        assertEquals("2026-08-25T18:48:58.204Z", gex.dataAsOf.equityFeed);
        assertEquals("2026-08-25T18:48:57.900Z", gex.dataAsOf.equityOptionsFeed);
        assertEquals("2026-08-25T18:48:55.100Z", gex.dataAsOf.flowFeed);
        assertEquals("2026-08-25T18:45:00.000Z", gex.dataAsOf.macroFeed);
    }

    /**
     * A feed the node has never seen is null, which is not the same as the feed being
     * unhealthy. Distinguishing the two is the point of the field, so null must survive
     * as null rather than collapsing to an empty string.
     */
    @Test
    public void unseenFeedsStayNull() {
        GexResponse gex = gson.fromJson(BODY, GexResponse.class);

        assertNull(gex.dataAsOf.indexFeed);
        assertNull(gex.dataAsOf.futuresFeed);
        assertNull(gex.dataAsOf.futuresOptionsFeed);
    }

    /**
     * Settled open interest is published once per session, so it trails the response by
     * design. Assert it passes through untouched rather than being normalised toward
     * response time, because that trailing gap is the signal a caller checks.
     */
    @Test
    public void settledOpenInterestTrailsUnmodified() {
        GexResponse gex = gson.fromJson(BODY, GexResponse.class);

        assertEquals("2026-08-24T20:00:00.000Z", gex.dataAsOf.oiFeed);
    }

    /** Responses predating the envelope must still parse; both members stay null. */
    @Test
    public void preEnvelopeResponsesStillParse() {
        GexResponse gex = gson.fromJson("{\"symbol\":\"SPY\",\"net_gex\":1.0}", GexResponse.class);

        assertEquals("SPY", gex.symbol);
        assertNull(gex.endpointVersion);
        assertNull(gex.dataAsOf);
    }

    /**
     * Guard the sweep itself: every response model must reach the base. Trusting that
     * one regex touched all 75 files is exactly the assumption worth testing, and a
     * model added later would otherwise slip through silently.
     *
     * <p>The class list is read from the compiled output rather than hardcoded, so the
     * guard cannot drift out of step with the source tree.
     */
    @Test
    public void everyResponseModelCarriesTheEnvelope() throws Exception {
        List<String> missing = new ArrayList<>();
        int checked = 0;

        for (String name : responseClassNames()) {
            Class<?> type = Class.forName("com.flashalpha." + name);
            if (Modifier.isAbstract(type.getModifiers())) {
                continue;
            }
            checked++;
            if (!FlashAlphaResponse.class.isAssignableFrom(type)) {
                missing.add(name);
            }
        }

        assertTrue("parsed no response models; the guard is not actually checking anything",
                checked > 0);
        assertTrue("response models not extending FlashAlphaResponse: " + missing,
                missing.isEmpty());
    }

    /** Names every *Response model from the source tree. */
    private static List<String> responseClassNames() throws Exception {
        File dir = new File("src/main/java/com/flashalpha");
        assertTrue("source directory not found: " + dir.getAbsolutePath(), dir.isDirectory());

        List<String> names = new ArrayList<>();
        File[] files = dir.listFiles();
        assertNotNull(files);
        for (File f : files) {
            String n = f.getName();
            if (n.endsWith("Response.java")) {
                names.add(n.substring(0, n.length() - ".java".length()));
            }
        }
        return names;
    }

    /**
     * The envelope must serialize back flat, under the wire names, rather than nesting
     * or renaming - round-tripping is how callers persist a response alongside its
     * provenance.
     */
    @Test
    public void envelopeRoundTripsUnderWireNames() {
        GexResponse gex = gson.fromJson(BODY, GexResponse.class);

        String out = gson.toJson(gex);

        assertTrue("data_as_of missing from output: " + out, out.contains("\"data_as_of\""));
        assertTrue("endpoint_version missing from output: " + out, out.contains("\"endpoint_version\""));
        assertTrue("oi_feed missing from output: " + out, out.contains("\"oi_feed\""));
        assertFalse("camelCase leaked to the wire: " + out, out.contains("\"dataAsOf\""));
    }

    /** Sanity check that the fixture in this file is the shape the API actually sends. */
    @Test
    public void fixtureCoversEveryDeclaredFeed() {
        for (java.lang.reflect.Field f : DataAsOf.class.getDeclaredFields()) {
            String wire = f.getAnnotation(com.google.gson.annotations.SerializedName.class).value();
            assertTrue("fixture omits " + wire, BODY.contains("\"" + wire + "\""));
        }
    }
}
