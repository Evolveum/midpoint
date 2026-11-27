/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.web.security.LoggingRequestCycleListener;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler.RedirectPolicy;
import org.apache.wicket.core.request.mapper.StalePageException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests exception handling behavior in {@link LoggingRequestCycleListener}.
 */
public class LoggingRequestCycleListenerTest {

    /**
     * MID-9998: Verifies that Wicket stale page recovery, e.g. after browser
     * view-source, is handled by re-rendering the stale page instead of being
     * converted to the error page / HTTP 500.
     */
    @Test
    public void testStalePageExceptionIsRecoveredInsteadOfConvertedToPageError() {
        WicketTester tester = new WicketTester();
        try {
            LoggingRequestCycleListener listener = new LoggingRequestCycleListener(tester.getApplication());
            TestPage page = new TestPage();

            IRequestHandler handler = listener.onException(null, new StalePageException(page));

            Assert.assertTrue(handler instanceof RenderPageRequestHandler);

            RenderPageRequestHandler renderHandler = (RenderPageRequestHandler) handler;
            Assert.assertSame(renderHandler.getPage(), page);
            Assert.assertSame(renderHandler.getRedirectPolicy(), RedirectPolicy.ALWAYS_REDIRECT);
        } finally {
            tester.destroy();
        }
    }

    /**
     * Pairs of resource URLs that fail while being served, and similar URLs that are not found.
     */
    @DataProvider
    public Object[][] resourceRequests() {
        return new Object[][] {
                { "wicket/resource/java.lang.String/$-$/$-$", "wicket/resource/java.lang.Nope/$-$/$-$" },
                { "wicket/resource/org.apache.wicket.Application/$-$/$-$/$-$",
                        "wicket/resource/org.apache.wicket.Nope/$-$/$-$/$-$" },
                { "wicket/resource/java.util.ArrayList/x", "wicket/resource/java.util.Nope/x" },
                { "wicket/resource/java.lang.String/$-$/$-$/META-INF/MANIFEST.MF",
                        "wicket/resource/java.lang.String/$-$/$-$/META-INF/NOPE.MF" },
                { "wicket/resource/java.lang.String/$-$/$-$/org/apache/wicket/Application.class",
                        "wicket/resource/java.lang.String/$-$/$-$/org/apache/wicket/Nope.class" },
        };
    }

    /**
     * Failed resource requests are answered with 404, the same as resources that are not found.
     */
    @Test(dataProvider = "resourceRequests")
    public void testFailedResourceRequestRespondsWithNotFound(String failingUrl, String notFoundUrl) {
        int failingStatus = executeResourceRequest(failingUrl);
        int notFoundStatus = executeResourceRequest(notFoundUrl);

        Assert.assertEquals(failingStatus, 404, "Status for " + failingUrl);
        Assert.assertEquals(notFoundStatus, failingStatus, "Status for " + notFoundUrl);
    }

    /**
     * Executes the request with midPoint's parent folder placeholder and {@link LoggingRequestCycleListener}.
     * Requests not handled by Wicket end with 404 in the servlet filter chain, which the tester does not have.
     */
    private int executeResourceRequest(String url) {
        WicketTester tester = new WicketTester();
        try {
            tester.getApplication().getResourceSettings().setParentFolderPlaceholder("$-$");
            tester.getApplication().getRequestCycleListeners()
                    .add(new LoggingRequestCycleListener(tester.getApplication()));

            tester.getRequest().setUrl(Url.parse(url));
            if (!tester.processRequest()) {
                return HttpServletResponse.SC_NOT_FOUND;
            }
            return tester.getLastResponse().getStatus();
        } finally {
            tester.destroy();
        }
    }

    private static class TestPage extends WebPage {
    }
}
