/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.util;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.web.csrf.CsrfToken;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuAuthzUtil;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;

/**
 * @author lazyman
 * @author lskublik
 */
public class SecurityUtils {

    public static final String DEFAULT_LOGOUT_PATH = "/logout";

    public static boolean isMenuAuthorized(MainMenuItem item) {
        Class<?> clazz = item.getPageClass();
        return clazz == null || isPageAuthorized(clazz);
    }

    public static boolean isMenuAuthorized(MenuItem item) {
        Class<? extends WebPage> clazz = item.getPageClass();
        List<String> authz = LeftMenuAuthzUtil.getAuthorizationsForPage(clazz);
        if (CollectionUtils.isNotEmpty(authz)) {
            return WebComponentUtil.isAuthorized(authz);
        }
        return isPageAuthorized(clazz);
    }

    public static boolean isCollectionMenuAuthorized(MenuItem item) {
        Class<? extends WebPage> clazz = item.getPageClass();
        List<String> authz = LeftMenuAuthzUtil.getAuthorizationsForView(clazz);
        if (CollectionUtils.isNotEmpty(authz)) {
            return WebComponentUtil.isAuthorized(authz);
        }
        return isPageAuthorized(clazz);
    }

    public static boolean isPageAuthorized(Class<?> page) {
        if (page == null) {
            return false;
        }

        PageDescriptor descriptor = page.getAnnotation(PageDescriptor.class);
        if (descriptor == null) {
            return false;
        }

        AuthorizationAction[] actions = descriptor.action();
        List<String> list = new ArrayList<>();
        for (AuthorizationAction action : actions) {
            list.add(action.actionUri());
        }

        return WebComponentUtil.isAuthorized(list.toArray(new String[0]));
    }

    public static List<String> getPageAuthorizations(Class<?> page) {
        List<String> list = new ArrayList<>();
        if (page == null) {
            return list;
        }

        PageDescriptor descriptor = page.getAnnotation(PageDescriptor.class);
        if (descriptor == null) {
            return list;
        }

        AuthorizationAction[] actions = descriptor.action();
        for (AuthorizationAction action : actions) {
            list.add(action.actionUri());
        }
        return list;
    }

    public static WebMarkupContainer createHiddenInputForCsrf(String id) {
        WebMarkupContainer field = new WebMarkupContainer(id) {

            @Override
            public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                super.onComponentTagBody(markupStream, openTag);

                appendHiddenInputForCsrf(getResponse());
            }
        };
        field.setRenderBodyOnly(true);

        return field;
    }

    public static void appendHiddenInputForCsrf(Response resp) {
        CsrfToken csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return;
        }

        String parameterName = csrfToken.getParameterName();
        String value = csrfToken.getToken();

        resp.write("<input type=\"hidden\" name=\"" + parameterName + "\" value=\"" + value + "\"/>");
    }

    public static CsrfToken getCsrfToken() {
        Request req = RequestCycle.get().getRequest();
        HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();

        return (CsrfToken) httpReq.getAttribute("_csrf");
    }

    /**
     * name attribute is deprecated, getSequenceByIdentifier should be used instead
     * @param name
     * @param authenticationPolicy
     * @return
     */
    @Deprecated
    public static AuthenticationSequenceType getSequenceByName(String name, AuthenticationsPolicyType authenticationPolicy) {
        if (authenticationPolicy == null || authenticationPolicy.getSequence() == null
                || authenticationPolicy.getSequence().isEmpty()) {
            return null;
        }

        Validate.notBlank(name, "Name for searching of sequence is blank");
        for (AuthenticationSequenceType sequence : authenticationPolicy.getSequence()) {
            if (sequence != null) {
                if (name.equals(sequence.getName()) || name.equals(sequence.getIdentifier())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()) {
                        return null;
                    }
                    return sequence;
                }
            }
        }
        return null;
    }

    public static AuthenticationSequenceType getSequenceByIdentifier(String identifier, AuthenticationsPolicyType authenticationPolicy) {
        if (authenticationPolicy == null || CollectionUtils.isEmpty(authenticationPolicy.getSequence())) {
            return null;
        }

        Validate.notBlank(identifier, "Identifier for searching of sequence is blank");
        for (AuthenticationSequenceType sequence : authenticationPolicy.getSequence()) {
            if (sequence != null) {
                if (identifier.equals(sequence.getIdentifier())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()) {
                        return null;
                    }
                    return sequence;
                }
            }
        }
        return null;
    }

    public static String getPathForLogoutWithContextPath(String contextPath, @NotNull String prefix) {
        return StringUtils.isNotEmpty(contextPath)
                ? "/" + AuthUtil.stripSlashes(contextPath) + getPathForLogout(prefix)
                : getPathForLogout(prefix);
    }

    private static String getPathForLogout(@NotNull String prefix) {
        return "/" + AuthUtil.stripSlashes(prefix) + DEFAULT_LOGOUT_PATH;
    }

    public static boolean sequenceExists(AuthenticationsPolicyType policy, String identifier) {
        return getSequenceByIdentifier(identifier, policy) != null || getSequenceByName(identifier, policy) != null;
    }

    public static String getChannelUrlSuffixFromAuthSequence(String sequenceIdentifier, SecurityPolicyType securityPolicy) {
        if (securityPolicy == null) {
            return null;
        }
        AuthenticationSequenceType sequence = getSequenceByIdentifier(sequenceIdentifier, securityPolicy.getAuthentication());
        if (sequence == null) {
            sequence = SecurityUtils.getSequenceByName(sequenceIdentifier, securityPolicy.getAuthentication());
        }
        if (sequence == null) {
            return null;
        }
        var channel = sequence.getChannel();
        if (channel == null) {
            return null;
        }
        return channel.getUrlSuffix();
    }

    public static ArchetypeSelectionModuleType getArchetypeSelectionAuthModule(SecurityPolicyType securityPolicy) {
        if (securityPolicy == null || securityPolicy.getAuthentication() == null
                || securityPolicy.getAuthentication().getModules() == null) {
            return null;
        }
        var policy = securityPolicy.getLoginNameRecovery();
        if (policy == null) {
            return null;
        }
        var sequenceIdentifier = policy.getAuthenticationSequenceIdentifier();
        var sequence = getSequenceByIdentifier(sequenceIdentifier, securityPolicy.getAuthentication());
        if (sequence == null) {
            return null;
        }
        List<AuthenticationSequenceModuleType> modules = sequence.getModule();
        for (AuthenticationSequenceModuleType module : modules) {
            var recoveryModuleIdentifier = module.getIdentifier();
            List<ArchetypeSelectionModuleType> archetypeBasedModules =
                    securityPolicy.getAuthentication().getModules().getArchetypeSelection();
            var archetypeSelectionModule = archetypeBasedModules
                    .stream()
                    .filter(m -> m.getIdentifier().equals(recoveryModuleIdentifier))
                    .findFirst()
                    .orElse(null);
            if (archetypeSelectionModule != null) {
                return archetypeSelectionModule;
            }
        }
        return null;
    }
}
