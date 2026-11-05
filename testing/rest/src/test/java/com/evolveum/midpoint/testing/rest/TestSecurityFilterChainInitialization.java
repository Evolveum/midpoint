/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;

/**
 * End-to-end checks that public resources and application endpoints retain
 * their expected HTTP security behavior.
 */
public class TestSecurityFilterChainInitialization extends RestServiceInitializer {

    private static final String[] PUBLIC_STATIC_RESOURCE_PATHS = {
            "/css/overlay.css",
            "/js/overlay.js",
            "/img/logo.jpg",
            "/fonts/font-evosome.woff",
            "/static/midpoint.css",
            "/favicon.ico",
    };

    @LocalServerPort private int serverPort;

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return jsonProvider;
    }

    @Test
    public void test100PublicStaticResourcesRemainAccessibleWithInvalidSession() {
        for (String path : PUBLIC_STATIC_RESOURCE_PATHS) {
            WebClient client = prepareRootClient(null, null);
            client.header("Cookie", "JSESSIONID=invalid-session");
            client.path(path);

            Response response = client.get();

            assertThat(response.getStatus())
                    .as("status for %s", path)
                    .isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getHeaderString("Location")).as("redirect for %s", path).isNull();
        }
    }

    @Test
    public void test110StaticWebResourceDoesNotRequireAuthentication() {
        String path = "/static-web/nonexistent.css";
        WebClient client = prepareRootClient(null, null);
        client.header("Cookie", "JSESSIONID=invalid-session");
        client.path(path);

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).isNull();
    }

    @Test
    public void test115WicketResourceDoesNotRequireAuthentication() {
        String path = "/wicket/resource/nonexistent";
        WebClient client = prepareRootClient(null, null);
        client.header("Cookie", "JSESSIONID=invalid-session");
        client.path(path);

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).isNull();
    }

    @Test
    public void test120LoginStillUsesNormalPermitAllHandling() {
        WebClient client = prepareRootClient(null, null);
        WebClient.getConfig(client).getRequestContext().put(Message.MAINTAIN_SESSION, true);
        client.path("/login");

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.FOUND.getStatusCode());
        URI location = response.getLocation();
        assertThat(location).isNotNull();

        URI resolvedLocation = client.getCurrentURI().resolve(location);
        assertThat(resolvedLocation.getPath()).isEqualTo("/login");
        assertThat(resolvedLocation.getQuery()).as("Wicket render URL query").isNotBlank();
        response.close();

        client.to(resolvedLocation.toString(), true);
        Response renderedLoginPage = client.get();

        assertThat(renderedLoginPage.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(renderedLoginPage.getHeaderString("Location")).isNull();
        renderedLoginPage.close();
    }

    @Test
    public void test130ProtectedGuiRequestRedirectsToLogin() {
        WebClient client = prepareRootClient(null, null);
        client.path("/admin/users");

        Response response = client.get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).contains("/login");
    }

    @Test
    public void test140ActuatorHealthUsesConfiguredBasicAuthentication() {
        WebClient anonymous = prepareRootClient(null, null);
        anonymous.header("Cookie", "JSESSIONID=invalid-session");
        anonymous.path("/actuator/health");

        Response anonymousResponse = anonymous.get();

        assertThat(anonymousResponse.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
        assertThat(anonymousResponse.getHeaderString("Location")).isNull();
        assertThat(anonymousResponse.getHeaderString("WWW-Authenticate")).containsIgnoringCase("Basic");

        WebClient administrator = prepareRootClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        administrator.path("/actuator/health");

        Response administratorResponse = administrator.get();

        assertThat(administratorResponse.getStatus())
                .isIn(Response.Status.OK.getStatusCode(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        assertThat(administratorResponse.getHeaderString("Location")).isNull();
    }

    @Test
    public void test150RestRequestStillUsesDynamicAuthenticationAndAuthorization() {
        WebClient anonymous = prepareRootClient(null, null);
        anonymous.path("/ws/rest/users");
        assertThat(anonymous.get().getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        WebClient unauthorized = prepareRootClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
        unauthorized.path("/ws/rest/users");
        assertThat(unauthorized.get().getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());

        WebClient administrator = prepareRootClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        administrator.path("/ws/rest/users");
        assertThat(administrator.get().getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    private WebClient prepareRootClient(String username, String password) {
        WebClient client = WebClient.create("http://localhost:" + serverPort);
        client.accept(getAcceptHeader());
        createAuthorizationHeader(client, username, password);
        return client;
    }
}
