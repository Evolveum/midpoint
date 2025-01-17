/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.authorization;

import com.evolveum.midpoint.authentication.api.authorization.*;
import com.evolveum.midpoint.security.api.*;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author lazyman
 */
@Component("descriptorLoader")
public final class DescriptorLoaderImpl implements DescriptorLoader {

    private static final Trace LOGGER = TraceManager.getTrace(DescriptorLoaderImpl.class);

    @Value("${midpoint.additionalPackagesToScan:}") private String additionalPackagesToScan;

    // All could be final, but then Checkstyle complains about lower-case, although these are not constants.
    private static Map<String, AuthorizationActionValue[]> actions = new HashMap<>();
    private static List<String> permitAllUrls = new ArrayList<>();
    private static List<String> loginPages = new ArrayList<>();
    private static Map<String, List<String>> mapForAuthPages = new HashMap<>();

    public static Map<String, AuthorizationActionValue[]> getActions() {
        return actions;
    }

    public static Collection<String> getPermitAllUrls() {
        return permitAllUrls;
    }

    public static List<String> getLoginPages() {
        return loginPages;
    }

    public static Map<String, List<String>> getMapForAuthPages() {
        return mapForAuthPages;
    }

    public static List<String> getPageUrlsByAuthName(String authName) {
        return mapForAuthPages.get(authName);
    }

    public static boolean existPageUrlByAuthName(String authName) {
        return mapForAuthPages.containsKey(authName);
    }

    public void loadData() {
        LOGGER.debug("Loading data from descriptor files.");

        try {
            scanPackagesForPages();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("loaded:\n{}", debugDump(1));
            }
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Error scanning packages for PageDescriptor classes: {}", e.getMessage(), e);
            throw new SystemException("Error scanning packages for PageDescriptor classes: " + e.getMessage(), e);
        }

    }

    private void scanPackagesForPages() throws InstantiationException, IllegalAccessException {
        Collection<Class<?>> classes = ClassPathUtil.scanClasses(PageDescriptor.class,
                StringUtils.joinWith(",", ClassPathUtil.DEFAULT_PACKAGE_TO_SCAN, additionalPackagesToScan));

        for (Class<?> clazz : classes) {
            PageDescriptor descriptor = clazz.getAnnotation(PageDescriptor.class);
            if (descriptor == null) {
                continue;
            }
            loadActions(descriptor);
        }
    }

    private void loadActions(PageDescriptor descriptor) {

        if (descriptor.loginPage()) {
            foreachUrl(descriptor, loginPages::add);
        }

        if (StringUtils.isNotEmpty(descriptor.authModule())) {
            List<String> urls = new ArrayList<>();
            foreachUrl(descriptor, urls::add);
            addAuthPage(descriptor, urls);
        }

        if (descriptor.permitAll()) {
            foreachUrl(descriptor, permitAllUrls::add);
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
            actions.add(new AuthorizationActionValue(AuthorizationConstants.AUTZ_GUI_ALL_URL,
                    AuthorizationConstants.AUTZ_GUI_ALL_LABEL, AuthorizationConstants.AUTZ_GUI_ALL_DESCRIPTION));
        }

        foreachUrl(descriptor, url -> DescriptorLoaderImpl.actions.put(url, actions.toArray(new AuthorizationActionValue[0])));
    }

    private void addAuthPage(PageDescriptor descriptor, List<String> urls) {
        if (existPageUrlByAuthName(descriptor.authModule())) {
            mapForAuthPages.get(descriptor.authModule()).addAll(urls);
        } else {
            mapForAuthPages.put(descriptor.authModule(), urls);
        }
    }

    private void foreachUrl(PageDescriptor descriptor, Consumer<String> urlConsumer) {
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
        DebugUtil.debugDumpWithLabelLn(sb, "permitAllUrls", permitAllUrls, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "loginPages", loginPages, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "mapForAuthPages", mapForAuthPages, indent + 1);
        return sb.toString();
    }
}
