/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.conndev;

import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ServiceClient} request construction: the object-class extraction
 * request must carry the connector intent in the {@code intent} query parameter only when it
 * is set, leaving {@code apiType} and {@code skipCache} handling unchanged.
 */
public class ServiceClientTest {

    private static final String API_BASE = "http://localhost:8090/api/v1/";
    private static final String SESSION_ID = "00000000-0000-0000-0000-000000000000";
    private static final String ENDPOINT = "digester/{sessionId}/classes";

    private HttpPost postRequest(String apiType, String intent, boolean skipCache) throws URISyntaxException {
        var client = new ServiceClient(API_BASE, null, SESSION_ID, null, null, null);
        return client.new Job(API_BASE + ENDPOINT, apiType, intent, skipCache).postBuilder();
    }

    private String query(String apiType, String intent, boolean skipCache) throws URISyntaxException {
        var rawQuery = postRequest(apiType, intent, skipCache).getUri().getRawQuery();
        return rawQuery != null ? rawQuery : "";
    }

    @Test
    public void intentForwardedWhenSet() throws URISyntaxException {
        assertThat(query("rest", "itsm", false)).contains("intent=itsm");
        assertThat(query("scim", "management_itsm", false)).contains("intent=management_itsm");
        assertThat(query(null, "management", false)).contains("intent=management");
    }

    @Test
    public void intentOmittedWhenNotSet() throws URISyntaxException {
        assertThat(query("rest", null, false)).doesNotContain("intent");
        // Legacy constructors must keep working without the intent parameter
        var client = new ServiceClient(API_BASE, null, SESSION_ID, null, null, null);
        var legacyQuery = client.new Job(API_BASE + ENDPOINT, "rest", false).postBuilder().getUri().getRawQuery();
        assertThat(legacyQuery).isNotNull().doesNotContain("intent");
    }

    @Test
    public void apiTypeAndSkipCacheUnaffected() throws URISyntaxException {
        var query = query("rest", "itsm", true);
        assertThat(query)
                .contains("apiType=rest")
                .contains("skipCache=true")
                .contains("intent=itsm");

        assertThat(query(null, null, false)).isBlank();
    }

    @Test
    public void sessionIdSubstituted() throws URISyntaxException {
        assertThat(postRequest("rest", "itsm", false).getUri().getPath())
                .isEqualTo("/api/v1/digester/" + SESSION_ID + "/classes");
    }
}
