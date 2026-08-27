/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.testng.Assert.expectThrows;
import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.net.ServerSocket;

import jakarta.servlet.ServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.impl.module.configuration.OidcClientModuleWebSecurityConfiguration;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcClientAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidcOpenIdProviderType;

/**
 * Tests OIDC client configuration behavior when issuer discovery is unavailable.
 *
 * Verifies that issuer discovery failures are preserved when no complete
 * explicit provider fallback is configured, and that explicitly configured
 * authorization and token endpoints are used when such a fallback is
 * available.
 */
public class TestOidcClientModuleWebSecurityConfiguration extends AbstractHigherUnitTest {

    @Test
    public void testIssuerDiscoveryFailureIsPropagated() {
        IllegalArgumentException ex = expectThrows(
                IllegalArgumentException.class,
                () -> OidcClientModuleWebSecurityConfiguration.build(
                        oidcModule(providerWithIssuerOnly()),
                        "gui-default",
                        null,
                        request()));

        assertTrue(
                "Wrong top-level message: " + ex.getMessage(),
                ex.getMessage().contains("Unable to resolve OIDC issuer configuration"));
        assertNotNull("Issuer discovery failure cause was not preserved", ex.getCause());
        assertCauseChainDoesNotContain(ex, "authorizationUri cannot be empty");
    }

    @Test
    public void testExplicitProviderEndpointsAreUsedWhenIssuerDiscoveryFails() {
        OidcClientModuleWebSecurityConfiguration configuration =
                OidcClientModuleWebSecurityConfiguration.build(
                        oidcModule(providerWithExplicitEndpoints()),
                        "gui-default",
                        null,
                        request());

        ClientRegistration registration =
                configuration.getClientRegistrationRepository()
                        .findByRegistrationId("oidc-registration");

        assertNotNull("No client registration", registration);
        assertEquals(
                "https://idp.example.test/oauth2/authorize",
                registration.getProviderDetails().getAuthorizationUri());
        assertEquals(
                "https://idp.example.test/oauth2/token",
                registration.getProviderDetails().getTokenUri());
        assertNull(
                "JWK Set URI should not be required for registration construction",
                registration.getProviderDetails().getJwkSetUri());
    }

    @DataProvider(name = "incompleteProviderEndpoints")
    public Object[][] incompleteProviderEndpoints() {
        return new Object[][] {
                {
                        new OidcOpenIdProviderType()
                                .issuerUri(unavailableIssuerUri())
                                .tokenUri("https://idp.example.test/oauth2/token")
                },
                {
                        new OidcOpenIdProviderType()
                                .issuerUri(unavailableIssuerUri())
                                .authorizationUri("https://idp.example.test/oauth2/authorize")
                }
        };
    }

    @Test(dataProvider = "incompleteProviderEndpoints")
    public void testIncompleteExplicitProviderEndpointsDoNotHideIssuerDiscoveryFailure(
            OidcOpenIdProviderType provider) {
        IllegalArgumentException ex = expectThrows(
                IllegalArgumentException.class,
                () -> OidcClientModuleWebSecurityConfiguration.build(
                        oidcModule(provider),
                        "gui-default",
                        null,
                        request()));

        assertTrue(
                "Wrong top-level message: " + ex.getMessage(),
                ex.getMessage().contains("Unable to resolve OIDC issuer configuration"));
        assertNotNull("Issuer discovery failure cause was not preserved", ex.getCause());
        assertCauseChainDoesNotContain(ex, "authorizationUri cannot be empty");
        assertCauseChainDoesNotContain(ex, "tokenUri cannot be empty");
    }

    private static OidcAuthenticationModuleType oidcModule(
            OidcOpenIdProviderType provider) {
        OidcClientAuthenticationModuleType client =
                new OidcClientAuthenticationModuleType()
                        .registrationId("oidc-registration")
                        .clientId("midpoint")
                        .openIdProvider(provider);

        return new OidcAuthenticationModuleType()
                .identifier("gui-oidc")
                .client(client);
    }

    private static OidcOpenIdProviderType providerWithIssuerOnly() {
        return new OidcOpenIdProviderType()
                .issuerUri(unavailableIssuerUri());
    }

    private static OidcOpenIdProviderType providerWithExplicitEndpoints() {
        return new OidcOpenIdProviderType()
                .issuerUri(unavailableIssuerUri())
                .authorizationUri("https://idp.example.test/oauth2/authorize")
                .tokenUri("https://idp.example.test/oauth2/token");
    }

    private static String unavailableIssuerUri() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort() + "/realms/test";
        } catch (IOException e) {
            throw new AssertionError("Couldn't allocate an unavailable issuer port", e);
        }
    }

    private static ServletRequest request() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/midpoint/home/default");
        request.setContextPath("/midpoint");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        return request;
    }

    private static void assertCauseChainDoesNotContain(
            Throwable throwable, String unexpected) {
        for (Throwable current = throwable;
             current != null;
             current = current.getCause()) {
            assertFalse(
                    "Cause chain unexpectedly contains: " + unexpected,
                    current.getMessage() != null
                            && current.getMessage().contains(unexpected));
        }
    }
}
