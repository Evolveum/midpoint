/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import java.util.*;

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

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.page",
            "com.evolveum.midpoint.web.page.admin",
            "com.evolveum.midpoint.web.page.admin.home",
            "com.evolveum.midpoint.web.page.admin.users",
            "com.evolveum.midpoint.web.page.admin.orgs",
            "com.evolveum.midpoint.web.page.admin.services",
            "com.evolveum.midpoint.web.page.admin.roles",
            "com.evolveum.midpoint.web.page.admin.resources",
            "com.evolveum.midpoint.web.page.admin.resources.content",
            "com.evolveum.midpoint.web.page.admin.workflow",
            "com.evolveum.midpoint.web.page.admin.server",
            "com.evolveum.midpoint.web.page.admin.reports",
            "com.evolveum.midpoint.web.page.admin.configuration",
            "com.evolveum.midpoint.web.page.admin.configuration.system",
            "com.evolveum.midpoint.web.page.admin.certification",
            "com.evolveum.midpoint.web.page.admin.valuePolicy",
            "com.evolveum.midpoint.web.page.admin.cases",
            "com.evolveum.midpoint.web.page.admin.archetype",
            "com.evolveum.midpoint.web.page.admin.objectCollection",
            "com.evolveum.midpoint.web.page.admin.objectTemplate",
            "com.evolveum.midpoint.web.page.login",
            "com.evolveum.midpoint.web.page.error",
            "com.evolveum.midpoint.web.page.forgetpassword",
            "com.evolveum.midpoint.web.page.self",
            "com.evolveum.midpoint.gui.impl.page.self",
            "com.evolveum.midpoint.web.component.prism.show",
            "com.evolveum.midpoint.gui.impl.page.admin",
            "com.evolveum.midpoint.gui.impl.page.admin.cases",
            "com.evolveum.midpoint.gui.impl.page.admin.org",
            "com.evolveum.midpoint.gui.impl.page.admin.resource",
            "com.evolveum.midpoint.gui.impl.page.admin.role",
            "com.evolveum.midpoint.gui.impl.page.admin.service",
            "com.evolveum.midpoint.gui.impl.page.admin.task",
            "com.evolveum.midpoint.gui.impl.page.admin.user",
            "com.evolveum.midpoint.gui.impl.page.admin.focus",
            "com.evolveum.midpoint.gui.impl.page.admin.objectcollection",
            "com.evolveum.midpoint.gui.impl.page.admin.objecttemplate",
            "com.evolveum.midpoint.gui.impl.page.admin.archetype",
            "com.evolveum.midpoint.gui.impl.page.admin.report",
            "com.evolveum.midpoint.gui.impl.page.login"
    };

    // All could be final, but then Checkstyle complains about lower-case, although these are not constants.
    private static Map<String, Class> urlClassMap = new HashMap<>();

    public static Map<String, Class> getUrlClassMap() {
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
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Error scanning packages for pages: {}", e.getMessage(), e);
            throw new SystemException("Error scanning packages for pages: " + e.getMessage(), e);
        }

    }

    private void scanPackagesForPages(MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        LOGGER.debug("Scanning packages for page annotations");

        Collection<Class<?>> classes = ClassPathUtil.scanClasses(PageDescriptor.class, PACKAGES_TO_SCAN);
        for (Class<?> clazz : classes) {
            if (!WebPage.class.isAssignableFrom(clazz)) {
                continue;
            }

            PageDescriptor descriptor = clazz.getAnnotation(PageDescriptor.class);
            mountPage(descriptor, clazz, application);
        }
    }

    private void mountPage(PageDescriptor descriptor, Class clazz, MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        for (Url url : descriptor.urls()) {
            IPageParametersEncoder encoder = descriptor.encoder().newInstance();

            LOGGER.trace("Mounting page '{}' to url '{}' with encoder '{}'.",
                    clazz.getName(), url, encoder.getClass().getSimpleName());

            application.mount(new ExactMatchMountedMapper(url.mountUrl(), clazz, encoder));
            String mountUrl = url.mountUrl();
            if (urlClassMap.containsKey(mountUrl)) {
                throw new IllegalStateException("Mount url '" + mountUrl + "' is already used by '" + urlClassMap.get(mountUrl).getName()
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
