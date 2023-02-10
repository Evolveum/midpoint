/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.ExactMatchMountedMapper;

/**
 * @author lazyman
 */
public final class PageMounter implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(PageMounter.class);

    // All could be final, but then Checkstyle complains about lower-case, although these are not constants.
    @SuppressWarnings("FieldMayBeFinal")
    private static Map<String, Class<? extends WebPage>> urlClassMap = new HashMap<>();

    public static Map<String, Class<? extends WebPage>> getUrlClassMap() {
        return urlClassMap;
    }

    public void loadData(MidPointApplication application) {
        LOGGER.debug("Loading data from descriptor files.");

        try {
            urlClassMap.clear();

            scanPackagesForPages(application);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("loaded:\n{}", debugDump(1));
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Error scanning packages for pages: {}", e.getMessage(), e);
            throw new SystemException("Error scanning packages for pages: " + e.getMessage(), e);
        }
    }

    private void scanPackagesForPages(MidPointApplication application)
            throws ReflectiveOperationException {

        LOGGER.debug("Scanning packages for page annotations");

        Collection<Class<?>> classes = ClassPathUtil.scanClasses(PageDescriptor.class,
                StringUtils.joinWith(",", ClassPathUtil.DEFAULT_PACKAGE_TO_SCAN, application.getAdditionalPackagesToScan()));

        for (Class<?> clazz : classes) {
            if (!WebPage.class.isAssignableFrom(clazz)) {
                continue;
            }

            PageDescriptor descriptor = clazz.getAnnotation(PageDescriptor.class);
            //noinspection unchecked
            mountPage(descriptor, (Class<WebPage>) clazz, application);
        }
    }

    private void mountPage(PageDescriptor descriptor, Class<WebPage> clazz, MidPointApplication application)
            throws ReflectiveOperationException {

        for (Url url : descriptor.urls()) {
            IPageParametersEncoder encoder = descriptor.encoder().getDeclaredConstructor().newInstance();

            LOGGER.trace("Mounting page '{}' to url '{}' with encoder '{}'.",
                    clazz.getName(), url, encoder.getClass().getSimpleName());

            String mountUrl = url.mountUrl();
            if (mountUrl.matches(".*\\$\\{[a-zA-Z_\\-]+\\}.*")) {
                application.mount(new MountedMapper(mountUrl, clazz, encoder));
            } else {
                application.mount(new ExactMatchMountedMapper(mountUrl, clazz, encoder));
            }

            if (urlClassMap.containsKey(mountUrl)) {
                throw new IllegalStateException("Mount url '" + mountUrl
                        + "' is already used by '" + urlClassMap.get(mountUrl).getName()
                        + "'. Attempting to add another class '" + clazz.getName() + "'");
            }
            
            urlClassMap.put(mountUrl, clazz);
        }
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PageMounter\n");
        DebugUtil.debugDumpWithLabel(sb, "urlClassMap", urlClassMap, indent + 1);
        return sb.toString();
    }
}
