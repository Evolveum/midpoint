/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;

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

    private static final String WICKET_SCOPE_ROOT = "/wicket/resource/org.apache.wicket.Application/$-$/$-$/$-$";

    private static final ResourcePathPair[] WICKET_RESOURCE_EXISTING_AND_MISSING_PATHS = {
            // classes
            new ResourcePathPair(WICKET_SCOPE_ROOT, "/wicket/resource/org.apache.wicket.Nope/$-$/$-$/$-$"),
            new ResourcePathPair("/wicket/resource/java.util.ArrayList/x", "/wicket/resource/java.util.Nope/x"),
            new ResourcePathPair(
                    "/wicket/resource/com.evolveum.midpoint.web.security.MidPointApplication/$-$/$-$/$-$/$-$/$-$/$-$",
                    "/wicket/resource/com.evolveum.midpoint.web.security.Nope/$-$/$-$/$-$/$-$/$-$/$-$"),
            // files that are never served
            new ResourcePathPair(WICKET_SCOPE_ROOT + "/META-INF/MANIFEST.MF", WICKET_SCOPE_ROOT + "/META-INF/NOPE.MF"),
            new ResourcePathPair(
                    WICKET_SCOPE_ROOT + "/org/springframework/boot/SpringApplication.class",
                    WICKET_SCOPE_ROOT + "/org/springframework/boot/Nope.class"),
            new ResourcePathPair(
                    "/wicket/resource/org.apache.wicket.Application/Application.class",
                    "/wicket/resource/org.apache.wicket.Application/Nope.class"),
            // file of servable type outside of the locations that UI libraries are served from
            new ResourcePathPair(
                    WICKET_SCOPE_ROOT + "/org/wicketstuff/select2/res/bootstrap/select2-bootstrap.css",
                    WICKET_SCOPE_ROOT + "/org/wicketstuff/select2/res/bootstrap/nope.css"),
    };

    private static final String[] WICKET_LIBRARY_RESOURCE_PATHS = {
            "/wicket/resource/org.apache.wicket.ajax.AbstractDefaultAjaxBehavior/res/js/wicket-ajax-jquery.js",
            "/wicket/resource/jp.try0.wicket.honeypot.behavior.HoneypotBehavior/HoneypotBehavior.min.js",
            // chart.js for chart panels, taken from webjar
            "/wicket/resource/com.evolveum.wicket.chartjs.ChartJsPanel/$-$/$-$/$-$/$-$/webjars/chartjs/"
                    + webjarVersion("org.webjars", "chartjs") + "/dist/chart.umd.js",
            // language file of select2 components, taken from webjar
            "/wicket/resource/de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference"
                    + "/webjars/select2/" + webjarVersion("org.webjars.npm", "select2") + "/dist/js/i18n/en.js",
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

    /**
     * Package resource requests must not reveal which classes and files exist on the classpath.
     * Existing ones are answered as the missing ones, with the common error page.
     */
    @Test
    public void test116WicketResourceDoesNotRevealClasspathContent() {
        for (ResourcePathPair pair : WICKET_RESOURCE_EXISTING_AND_MISSING_PATHS) {
            assertAnsweredAsNotFound(pair.existing());
            assertAnsweredAsNotFound(pair.missing());
        }
    }

    private void assertAnsweredAsNotFound(String path) {
        Response response = getAnonymously(path);

        assertThat(response.getStatus())
                .as("status for %s", path)
                .isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getHeaderString("Location")).as("redirect for %s", path).isNull();
        assertThat(response.getMediaType())
                .as("content type for %s", path)
                .matches(type -> type.isCompatible(MediaType.TEXT_HTML_TYPE), "is the HTML error page");
        response.close();
    }

    /**
     * Scripts of UI libraries are served as package resources, pages do not work without them.
     */
    @Test
    public void test117WicketLibraryResourcesAreServed() {
        for (String path : WICKET_LIBRARY_RESOURCE_PATHS) {
            Response response = getAnonymously(path);

            assertThat(response.getStatus())
                    .as("status for %s", path)
                    .isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.readEntity(String.class)).as("content of %s", path).isNotBlank();
        }
    }

    /**
     * Two package resource paths that differ only in whether their target is on the classpath.
     * A client must not be able to tell them apart from the response.
     *
     * @param existing path to a class or file that exists, but must not be served
     * @param missing the same path shape pointing to a class or file that does not exist
     */
    private record ResourcePathPair(String existing, String missing) {
    }

    /**
     * Version of webjar on the classpath, so the paths do not have to be changed with each library upgrade.
     */
    private static String webjarVersion(String groupId, String artifactId) {
        String path = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream is = TestSecurityFilterChainInitialization.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(Objects.requireNonNull(is, "Webjar not found: " + path));
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Response getAnonymously(String path) {
        WebClient client = prepareRootClient(null, null);
        client.accept(MediaType.WILDCARD);
        client.path(path);
        return client.get();
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
