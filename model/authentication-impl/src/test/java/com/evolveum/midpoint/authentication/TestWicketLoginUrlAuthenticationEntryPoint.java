/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.authentication.impl.WicketRedirectStrategy;
import com.evolveum.midpoint.authentication.impl.entry.point.WicketLoginUrlAuthenticationEntryPoint;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;

import org.apache.wicket.request.http.WebRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests login redirects produced by the Wicket authentication entry point.
 */
public class TestWicketLoginUrlAuthenticationEntryPoint extends AbstractHigherUnitTest {

    /**
     * Verifies that an unauthenticated Wicket Ajax request redirects to the login page
     * with the servlet context path preserved.
     */
    @Test
    public void testAjaxLoginRedirectIncludesContextPath() throws Exception {
        WicketLoginUrlAuthenticationEntryPoint entryPoint =
                new WicketLoginUrlAuthenticationEntryPoint("/login");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/midpoint/home/default");
        request.setContextPath("/midpoint");
        request.setServletPath("/home/default");
        request.addHeader(WebRequest.HEADER_AJAX, "true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new AuthenticationCredentialsNotFoundException("missing authentication"));

        assertEquals(
                "Wrong Wicket Ajax redirect target",
                "/midpoint/login",
                response.getHeader("Ajax-Location"));
        assertEquals(
                "Wrong Wicket Ajax redirect body",
                "<ajax-response><redirect><![CDATA[/midpoint/login]]></redirect></ajax-response>",
                response.getContentAsString());
    }

    /**
     * Verifies that Wicket Ajax redirects prefix only application-local paths and
     * leave already resolved, relative, and external URLs unchanged.
     */
    @Test(dataProvider = "redirectUrls")
    public void testWicketAjaxRedirectUrlHandling(String contextPath, String url, String expectedUrl) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + "/home/default");
        request.setContextPath(contextPath);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new WicketRedirectStrategy().sendRedirect(request, response, url);

        assertEquals(
                "Wrong Wicket Ajax redirect target",
                expectedUrl,
                response.getHeader("Ajax-Location"));
        assertEquals(
                "Wrong Wicket Ajax redirect body",
                "<ajax-response><redirect><![CDATA[" + expectedUrl + "]]></redirect></ajax-response>",
                response.getContentAsString());
    }

    @DataProvider
    public Object[][] redirectUrls() {
        return new Object[][] {
                { "/midpoint", "/login", "/midpoint/login" },
                { "/midpoint", "/midpoint/login", "/midpoint/login" },
                { "/midpoint", "http://localhost:8080/login", "http://localhost:8080/login" },
                { "/midpoint", "https://example.com/login", "https://example.com/login" },
                { "/midpoint", "//example.com/login", "//example.com/login" },
                { "/midpoint", "login", "login" },
                { "", "/login", "/login" },
        };
    }
}
