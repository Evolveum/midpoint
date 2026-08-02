/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import static org.testng.AssertJUnit.*;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * Tests handling of authentication modules that fail because of an invalid
 * or unavailable configuration.
 *
 * Verifies that a configuration failure in the first authentication module
 * terminates the authentication flow with an HTTP 401 response and stores the
 * corresponding exception for the error page.
 */
public class TestMidpointAuthFilterFailureConfiguration extends AbstractHigherUnitTest {

    private static final String WRONG_MODULES_CONFIG_MESSAGE =
            "web.security.flexAuth.wrong.auth.modules.config";

    @Test
    public void testFirstModuleConfigurationFailureReturnsUnauthorized() throws Exception {
        MidpointAuthentication authentication = createAuthenticationWithFailedFirstModule();

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/midpoint/home/default");
        request.setContextPath("/midpoint");
        request.setServletPath("/home/default");
        request.getSession(true);

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = new MidpointAuthFilter(Map.of())
                .resolveErrorWithWrongConfigurationOfModules(
                        authentication,
                        0,
                        request,
                        response);

        assertTrue("First module configuration failure should be terminal", handled);
        assertEquals(
                "Wrong response status",
                HttpServletResponse.SC_UNAUTHORIZED,
                response.getStatus());
        assertNull("Response must not redirect", response.getRedirectedUrl());
        assertNull("Response must not forward", response.getForwardedUrl());

        AuthenticationException storedException =
                (AuthenticationException) request.getSession()
                        .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        assertNotNull(
                "Authentication exception should be stored for the error page",
                storedException);
        assertEquals(
                "Wrong stored exception message",
                WRONG_MODULES_CONFIG_MESSAGE,
                storedException.getMessage());
    }

    private static MidpointAuthentication createAuthenticationWithFailedFirstModule() {
        AuthenticationSequenceModuleType sequenceModule =
                new AuthenticationSequenceModuleType();
        sequenceModule.setIdentifier("broken-oidc");
        sequenceModule.setOrder(1);

        AuthenticationSequenceType sequence = new AuthenticationSequenceType();
        sequence.setIdentifier("gui-default");
        sequence.getModule().add(sequenceModule);

        AuthModule<?> failedModule =
                AuthModuleImpl.buildFailedConfigurationModule(sequenceModule);

        MidpointAuthentication authentication = new MidpointAuthentication(sequence);
        authentication.setAuthModules(List.of(failedModule));
        authentication.addAuthentication(failedModule.getBaseModuleAuthentication());

        return authentication;
    }
}
