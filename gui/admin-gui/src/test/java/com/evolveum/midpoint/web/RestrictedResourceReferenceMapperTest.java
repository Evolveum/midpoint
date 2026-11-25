/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.wicket.chartjs.BarChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;
import de.agilecoders.wicket.webjars.WicketWebjars;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import org.apache.wicket.ajax.WicketAjaxJQueryResourceReference;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.JQueryResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.web.security.RestrictedResourceReferenceMapper;

/**
 * Package resources are served only for known UI libraries, everything else is left unhandled
 * regardless of whether the class or file exists.
 */
public class RestrictedResourceReferenceMapperTest {

    private static final String WICKET_SCOPE_ROOT = "wicket/resource/org.apache.wicket.Application/$-$/$-$/$-$";
    private static final String CHARTJS_SCOPE_ROOT =
            "wicket/resource/com.evolveum.wicket.chartjs.ChartJsPanel/$-$/$-$/$-$/$-$";
    private static final String WEBJARS_SCOPE = "wicket/resource/"
            + "de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference";
    private static final String CHARTJS_VERSION = webjarVersion("org.webjars", "chartjs");
    private static final String SELECT2_VERSION = webjarVersion("org.webjars.npm", "select2");

    @DataProvider
    public Object[][] servedResources() {
        return new Object[][] {
                { "wicket/resource/org.apache.wicket.ajax.AbstractDefaultAjaxBehavior/res/js/wicket-ajax-jquery.js" },
                // jQuery bundled in Wicket, in the version Wicket uses by default
                { "wicket/resource/" + JQueryResourceReference.class.getName() + "/"
                        + JQueryResourceReference.getV3().getName() },
                // allowed location reached by walking up from the scope
                { WICKET_SCOPE_ROOT + "/org/apache/wicket/ajax/res/js/wicket-ajax-jquery.js" },
        };
    }

    @DataProvider
    public Object[][] notHandledResources() {
        return new Object[][] {
                // class existence
                { "wicket/resource/java.util.ArrayList/x" },
                { "wicket/resource/java.util.Nope/x" },
                { "wicket/resource/com.evolveum.midpoint.web.security.MidPointApplication/x" },
                { "wicket/resource/com.evolveum.midpoint.web.security.Nope/x" },
                // walking up from allowed scope
                { WICKET_SCOPE_ROOT },
                { WICKET_SCOPE_ROOT + "/$-$/$-$" },
                { WICKET_SCOPE_ROOT + "/META-INF/MANIFEST.MF" },
                { WICKET_SCOPE_ROOT + "/META-INF/NOPE.MF" },
                { WICKET_SCOPE_ROOT + "/java/util/ArrayList.class" },
                // servable files outside of allowed locations
                { WICKET_SCOPE_ROOT + "/org/wicketstuff/select2/res/bootstrap/select2-bootstrap.css" },
                { CHARTJS_SCOPE_ROOT + "/webjars/select2/" + SELECT2_VERSION + "/dist/js/select2.js" },
                { WEBJARS_SCOPE + "/webjars/chartjs/" + CHARTJS_VERSION + "/dist/chart.umd.js" },
                { CHARTJS_SCOPE_ROOT + "/META-INF/MANIFEST.MF" },
                // missing file in allowed scope
                { "wicket/resource/org.apache.wicket.Application/nope.js" },
                // existing file in allowed scope that must not be served
                { "wicket/resource/org.apache.wicket.Application/Application.class" },
                { "wicket/resource/org.apache.wicket.Application/Application.properties" },
        };
    }

    @Test(dataProvider = "servedResources")
    public void testAllowedResourceIsServed(String url) {
        Result result = execute(url);

        Assert.assertNull(result.exception, "Exception for " + url);
        Assert.assertTrue(result.handled, "Not handled: " + url);
        Assert.assertEquals(result.status, 200, "Status for " + url);
    }

    @Test(dataProvider = "notHandledResources")
    public void testOtherResourceIsNotHandled(String url) {
        Result result = execute(url);

        Assert.assertNull(result.exception, "Exception for " + url);
        Assert.assertFalse(result.handled, "Handled with status " + result.status + ": " + url);
    }

    /**
     * References as the UI libraries create them. Pages do not work without these.
     */
    @DataProvider
    public Object[][] libraryReferences() {
        return new Object[][] {
                { "wicket ajax", WicketAjaxJQueryResourceReference.get() },
                // the same reference as in select2 behavior
                { "select2 language", new WebjarsJavaScriptResourceReference("select2/current/dist/js/i18n/en.js") },
        };
    }

    @Test(dataProvider = "libraryReferences")
    public void testGeneratedUrlIsServed(String name, ResourceReference reference) {
        AtomicReference<Exception> exception = new AtomicReference<>();
        WicketTester tester = newTester(exception);
        try {
            Url url = tester.getApplication().getRootRequestMapper().mapHandler(
                    new ResourceReferenceRequestHandler(reference));
            Assert.assertNotNull(url, "No URL generated for " + name);

            tester.getRequest().setUrl(url);
            Assert.assertTrue(tester.processRequest(), "Not handled: " + url);
            Assert.assertNull(exception.get(), "Exception for " + url);
            Assert.assertEquals(tester.getLastResponse().getStatus(), 200, "Status for " + url);
        } finally {
            tester.destroy();
        }
    }

    /**
     * Chart panel takes chart.js from webjar by a reference that walks up from the panel package.
     * The reference is taken from the rendered panel, so the test follows the library.
     */
    @Test
    public void testChartLibraryOfRenderedPanelIsServed() {
        AtomicReference<Exception> exception = new AtomicReference<>();
        AtomicReference<ResourceReference> chartLibrary = new AtomicReference<>();
        WicketTester tester = newTester(exception);
        try {
            tester.getApplication().getHeaderResponseDecorators().add(response -> new DecoratingHeaderResponse(response) {

                @Override
                public void render(HeaderItem item) {
                    if (item instanceof JavaScriptReferenceHeaderItem script
                            && script.getReference().getScope() == ChartJsPanel.class) {
                        chartLibrary.set(script.getReference());
                    }
                    super.render(item);
                }
            });
            tester.startComponentInPage(
                    new ChartJsPanel<>("chart", Model.of(new BarChartConfiguration())),
                    Markup.of("<canvas wicket:id=\"chart\"></canvas>"));
            Assert.assertNotNull(chartLibrary.get(), "Chart panel rendered no script reference");

            Url url = tester.getApplication().getRootRequestMapper().mapHandler(
                    new ResourceReferenceRequestHandler(chartLibrary.get()));
            tester.getRequest().setUrl(url);
            Assert.assertTrue(tester.processRequest(), "Not handled: " + url);
            Assert.assertNull(exception.get(), "Exception for " + url);
            Assert.assertEquals(tester.getLastResponse().getStatus(), 200, "Status for " + url);
        } finally {
            tester.destroy();
        }
    }

    /**
     * Version of webjar on the classpath, so the paths do not have to be changed with each library upgrade.
     */
    private static String webjarVersion(String groupId, String artifactId) {
        String path = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream is = RestrictedResourceReferenceMapperTest.class.getResourceAsStream(path)) {
            Properties properties = new Properties();
            properties.load(Objects.requireNonNull(is, "Webjar not found: " + path));
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Result execute(String url) {
        AtomicReference<Exception> exception = new AtomicReference<>();
        WicketTester tester = newTester(exception);
        try {
            tester.getRequest().setUrl(Url.parse(url));
            boolean handled = tester.processRequest();
            int status = handled ? tester.getLastResponse().getStatus() : -1;
            return new Result(handled, status, exception.get());
        } finally {
            tester.destroy();
        }
    }

    private WicketTester newTester(AtomicReference<Exception> exception) {
        WicketTester tester = new WicketTester();
        tester.getApplication().getResourceSettings().setParentFolderPlaceholder("$-$");
        RestrictedResourceReferenceMapper.install(tester.getApplication());
        WicketWebjars.install(tester.getApplication());
        tester.getApplication().getRequestCycleListeners().add(new IRequestCycleListener() {

            @Override
            public IRequestHandler onException(RequestCycle cycle, Exception ex) {
                exception.set(ex);
                return null;
            }
        });
        return tester;
    }

    private record Result(boolean handled, int status, Exception exception) {
    }
}
