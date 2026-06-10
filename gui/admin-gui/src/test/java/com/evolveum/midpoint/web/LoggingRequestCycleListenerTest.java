/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.web.security.LoggingRequestCycleListener;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler.RedirectPolicy;
import org.apache.wicket.core.request.mapper.StalePageException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.Assert;
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

    private static class TestPage extends WebPage {
    }
}
