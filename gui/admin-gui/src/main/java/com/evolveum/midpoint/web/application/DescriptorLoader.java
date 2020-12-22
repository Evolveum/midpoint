/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.ExactMatchMountedMapper;

/**
 * @author lazyman
 */
public final class DescriptorLoader implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(DescriptorLoader.class);

    private static final String[] PACKAGES_TO_SCAN = {
            "com.evolveum.midpoint.web.page",
            "com.evolveum.midpoint.web.page.admin.home",
            "com.evolveum.midpoint.web.page.admin.users",
            "com.evolveum.midpoint.web.page.admin.services",
            "com.evolveum.midpoint.web.page.admin.roles",
            "com.evolveum.midpoint.web.page.admin.resources",
            "com.evolveum.midpoint.web.page.admin.resources.content",
            "com.evolveum.midpoint.web.page.admin.workflow",
            "com.evolveum.midpoint.web.page.admin.server",
            "com.evolveum.midpoint.web.page.admin.reports",
            "com.evolveum.midpoint.web.page.admin.configuration",
            "com.evolveum.midpoint.web.page.admin.certification",
            "com.evolveum.midpoint.web.page.admin.valuePolicy",
            "com.evolveum.midpoint.web.page.admin.cases",
            "com.evolveum.midpoint.web.page.admin.archetype",
            "com.evolveum.midpoint.web.page.admin.objectCollection",
            "com.evolveum.midpoint.web.page.login",
            "com.evolveum.midpoint.web.page.error",
            "com.evolveum.midpoint.web.page.forgetpassword",
            "com.evolveum.midpoint.web.page.self",
            "com.evolveum.midpoint.web.component.prism.show"
    };

    private static Map<String, DisplayableValue<String>[]> actions = new HashMap<>();
    private static List<String> permitAllUrls = new ArrayList<>();
    private static List<String> loginPages = new ArrayList<>();
    private static Map<String, Class> urlClassMap = new HashMap<>();

    public static Map<String, DisplayableValue<String>[]> getActions() {
        return actions;
    }

    public static Map<String, Class> getUrlClassMap() {
        return urlClassMap;
    }

    public static Collection<String> getPermitAllUrls() {
        return permitAllUrls;
    }

    public static List<String> getLoginPages() {
        return loginPages;
    }

    public void loadData(MidPointApplication application) {
        LOGGER.debug("Loading data from descriptor files.");

        try {
            scanPackagesForPages(application);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("loaded:\n{}", debugDump(1));
            }
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Error scanning packages for pages: {}", e.getMessage(), e);
            throw new SystemException("Error scanning packages for pages: "+e.getMessage(), e);
        }



    }

    private void scanPackagesForPages(MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        for (String pac : PACKAGES_TO_SCAN) {
            LOGGER.debug("Scanning package package {} for page annotations", pac);

            Set<Class<?>> classes = ClassPathUtil.listClasses(pac);
            for (Class<?> clazz : classes) {
                if (!WebPage.class.isAssignableFrom(clazz)) {
                    continue;
                }

                PageDescriptor descriptor = clazz.getAnnotation(PageDescriptor.class);
                if (descriptor == null) {
                    continue;
                }

                mountPage(descriptor, clazz, application);
                loadActions(descriptor);
            }
        }
    }

    private void loadActions(PageDescriptor descriptor) {

        if(descriptor.loginPage()) {
            foreachUrl(descriptor, url -> loginPages.add(url));
        }

        if (descriptor.permitAll()) {
            foreachUrl(descriptor, url -> permitAllUrls.add(url));
            return;
        }

        List<AuthorizationActionValue> actions = new ArrayList<>();

        //avoid of setting guiAll authz for "public" pages (e.g. login page)
        if (descriptor.action() == null || descriptor.action().length == 0) {
            return;
        }

        boolean canAccess = true;

        for (AuthorizationAction action : descriptor.action()) {
            actions.add(new AuthorizationActionValue(action.actionUri(), action.label(), action.description()));
            if (AuthorizationConstants.AUTZ_NO_ACCESS_URL.equals(action.actionUri())) {
                canAccess = false;
                break;
            }
        }

        //add http://.../..#guiAll authorization only for displayable pages, not for pages used for development..
        if (canAccess) {

//            actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
            actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                    AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        }

        foreachUrl(descriptor, url -> this.actions.put(url, actions.toArray(new DisplayableValue[actions.size()])));
    }

    private void foreachUrl(PageDescriptor descriptor, Consumer<String> urlConsumer) {
        for (String url : descriptor.url()) {
            urlConsumer.accept(buildPrefixUrl(url));
        }

        for (Url url : descriptor.urls()) {
            String urlForSecurity = url.matchUrlForSecurity();
            if (StringUtils.isEmpty(urlForSecurity)) {
                urlForSecurity = buildPrefixUrl(url.mountUrl());
            }
            urlConsumer.accept(urlForSecurity);
        }
    }

    public String buildPrefixUrl(String url) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);

        if (!url.endsWith("/")) {
            sb.append("/");
        }
        sb.append("**");

        return sb.toString();
    }

    private void mountPage(PageDescriptor descriptor, Class clazz, MidPointApplication application)
            throws InstantiationException, IllegalAccessException {

        //todo remove for cycle later
        for (String url : descriptor.url()) {
            IPageParametersEncoder encoder = descriptor.encoder().newInstance();

            LOGGER.trace("Mounting page '{}' to url '{}' with encoder '{}'.",
                    clazz.getName(), url, encoder.getClass().getSimpleName());

            application.mount(new ExactMatchMountedMapper(url, clazz, encoder));
            urlClassMap.put(url, clazz);
        }

        for (Url url : descriptor.urls()) {
            IPageParametersEncoder encoder = descriptor.encoder().newInstance();

            LOGGER.trace("Mounting page '{}' to url '{}' with encoder '{}'.",
                    clazz.getName(), url, encoder.getClass().getSimpleName());

            application.mount(new ExactMatchMountedMapper(url.mountUrl(), clazz, encoder));
            urlClassMap.put(url.mountUrl(), clazz);
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
        sb.append("DescriptorLoader\n");
        DebugUtil.debugDumpWithLabelLn(sb, "actions", actions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "urlClassMap", urlClassMap, indent + 1);
        return sb.toString();
    }
}
