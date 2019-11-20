/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.security.module.AuthModule;
import com.evolveum.midpoint.web.security.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.security.module.factory.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.module.factory.ModuleFactory;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

import static org.springframework.security.saml.util.StringUtils.stripStartingSlashes;

/**
 * @author lazyman
 * @author lskublik
 */
public class SecurityUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityUtils.class);

    private static final String DEFAULT_CHANNEL = "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#user";

    public static MidPointUserProfilePrincipal getPrincipalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return getPrincipalUser(authentication);
    }

    private static final Map<String, String> MY_MAP;
    static {
        Map<String, String> map = new HashMap<String, String>();
//        map.put("auth", "http://midpoint.evolveum.com/xml/ns/public/model/channels-3#user");
        MY_MAP = Collections.unmodifiableMap(map);
    }

    public static MidPointUserProfilePrincipal getPrincipalUser(Authentication authentication) {
        if (authentication == null) {
            LOGGER.trace("Authentication not available in security context.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        if (principal instanceof MidPointUserProfilePrincipal) {
            return (MidPointUserProfilePrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            // silently ignore to avoid filling the logs
            return null;
        }
        LOGGER.debug("Principal user in security context holder is {} ({}) but not type of {}",
                principal, principal.getClass(), MidPointUserProfilePrincipal.class.getName());
        return null;
    }

    public static boolean isMenuAuthorized(MainMenuItem item) {
        Class clazz = item.getPageClass();
        return clazz == null || isPageAuthorized(clazz);
    }

    public static boolean isMenuAuthorized(MenuItem item) {
        Class clazz = item.getPageClass();
        return isPageAuthorized(clazz);
    }

    public static boolean isPageAuthorized(Class page) {
        if (page == null) {
            return false;
        }

        PageDescriptor descriptor = (PageDescriptor) page.getAnnotation(PageDescriptor.class);
        if (descriptor == null ){
            return false;
        }


        AuthorizationAction[] actions = descriptor.action();
        List<String> list = new ArrayList<>();
        if (actions != null) {
            for (AuthorizationAction action : actions) {
                list.add(action.actionUri());
            }
        }

        return WebComponentUtil.isAuthorized(list.toArray(new String[list.size()]));
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

    public static AuthenticationSequenceType getSequence(String localePath, AuthenticationsPolicyType authenticationPolicy){
        if (authenticationPolicy == null || authenticationPolicy.getSequence() == null
                || authenticationPolicy.getSequence().isEmpty()) {
            return null;
        }
        String[] partsOfLocalPath = stripStartingSlashes(localePath).split("/");

        if (partsOfLocalPath.length < 2) {
            String usedChannel;
            if (partsOfLocalPath.length == 1 && MY_MAP.containsKey(partsOfLocalPath[0])) {
                usedChannel = MY_MAP.get(partsOfLocalPath[0]);
            } else {
                usedChannel = DEFAULT_CHANNEL;
            }

            AuthenticationSequenceType sequence = searchSequence(usedChannel, true, authenticationPolicy);
            return sequence;
        }
        if (partsOfLocalPath[0].equals("auth")) {
            if (partsOfLocalPath[1].equals("default")) {
                AuthenticationSequenceType sequence = searchSequence(DEFAULT_CHANNEL, true, authenticationPolicy);
                return  sequence;
            }
            AuthenticationSequenceType sequence = searchSequence(partsOfLocalPath[1], false, authenticationPolicy);
            if (sequence == null) {
                LOGGER.debug("Couldn't find sequence by preffix {}, so try default channel", partsOfLocalPath[1]);
                sequence = searchSequence(DEFAULT_CHANNEL, true, authenticationPolicy);
            }
            return sequence;
        }
        String usedChannel;
        if (MY_MAP.containsKey(partsOfLocalPath[0])) {
            usedChannel = MY_MAP.get(partsOfLocalPath[0]);
        } else {
            usedChannel = DEFAULT_CHANNEL;
        }

        AuthenticationSequenceType sequence = searchSequence(usedChannel, true, authenticationPolicy);
        return sequence;

    }

    private static AuthenticationSequenceType searchSequence(String comparisonAttribute, boolean useOnlyChannel, AuthenticationsPolicyType authenticationPolicy) {
        Validate.notBlank(comparisonAttribute);
        for (AuthenticationSequenceType sequence : authenticationPolicy.getSequence()) {
            if (sequence != null && sequence.getChannel() != null) {
                if (useOnlyChannel && comparisonAttribute.equals(sequence.getChannel().getChannelId())
                        && Boolean.TRUE.equals(sequence.getChannel().isDefault())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()){
                        return null;
                    }
                    return sequence;
                } else if (!useOnlyChannel && comparisonAttribute.equals(sequence.getChannel().getUrlSuffix())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()){
                        return null;
                    }
                    return sequence;
                }
            }
        }
        return null;
    }

    public static List<AuthModule> buildModuleFilters(AuthModuleRegistryImpl authRegistry, AuthenticationSequenceType sequence,
                                                      ServletRequest request, AuthenticationModulesType authenticationModulesType,
                                                      Map<Class<? extends Object
                                                              >, Object> sharedObjects) {
        Validate.notNull(authRegistry);
        Validate.notEmpty(sequence.getModule());
        List<AuthenticationSequenceModuleType> sequenceModules = getSortedModules(sequence);
        List<AuthModule> authModules = new ArrayList<AuthModule>();

        sequenceModules.forEach(sequenceModule -> {
            try {
                AbstractAuthenticationModuleType module = getModuleByName(sequenceModule.getName(), authenticationModulesType);
                ModuleFactory moduleFactory = authRegistry.findModelFactory(module);
                AuthModule authModule = moduleFactory.createModuleFilter(module, sequence.getChannel().getUrlSuffix(), request, sharedObjects);
                authModules.add(authModule);
            } catch (Exception e) {
                LOGGER.error("Couldn't build filter for module moduleFactory", e);
            }
        });
        if (authModules.isEmpty()) {
            return null;
        }
        return authModules;
    }

    private static AbstractAuthenticationModuleType getModuleByName(String name, AuthenticationModulesType authenticationModulesType){
        List<AbstractAuthenticationModuleType> modules = new ArrayList<AbstractAuthenticationModuleType>();
        modules.addAll(authenticationModulesType.getLoginForm());
        modules.addAll(authenticationModulesType.getSaml2());
        modules.addAll(authenticationModulesType.getHttpBasic());
        modules.addAll(authenticationModulesType.getHttpHeader());
        modules.addAll(authenticationModulesType.getHttpSecQ());
        modules.addAll(authenticationModulesType.getMailNonce());
        modules.addAll(authenticationModulesType.getOidc());
        modules.addAll(authenticationModulesType.getSecurityQuestionsForm());
        modules.addAll(authenticationModulesType.getSmsNonce());

        for (AbstractAuthenticationModuleType module: modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    public static ModuleFactory getFactoryByName(AuthModuleRegistryImpl authRegistry, String name, AuthenticationModulesType authenticationModulesType){
        AbstractAuthenticationModuleType module = getModuleByName(name, authenticationModulesType);
        if (module != null) {
            return authRegistry.findModelFactory(module);
        }
        return null;
    }

    public static List<AuthenticationSequenceModuleType> getSortedModules(AuthenticationSequenceType sequence){
        Validate.notNull(sequence);
        ArrayList<AuthenticationSequenceModuleType> modules = new ArrayList<AuthenticationSequenceModuleType>();
        modules.addAll(sequence.getModule());
        Validate.notNull(modules);
        Validate.notEmpty(modules);
        Comparator<AuthenticationSequenceModuleType> comparator =
                (f1,f2) -> {

                    Integer f1Order = f1.getOrder();
                    Integer f2Order = f2.getOrder();

                    if (f1Order == null) {
                        if (f2Order != null) {
                            return 1;
                        }
                        return 0;
                    }

                    if (f2Order == null) {
                        if (f1Order != null) {
                            return -1;
                        }
                    }
                    return Integer.compare(f1Order, f2Order);
                };
        modules.sort(comparator);
        return Collections.unmodifiableList(modules);
    }

    public static boolean isPermitAll(HttpServletRequest request) {
        for (String url: DescriptorLoader.getPermitAllUrls()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return true;
            }
        }
        String servletPath = request.getServletPath();
        if ("".equals(servletPath) || "/".equals(servletPath)) {
            // Special case, this is in fact "magic" redirect to home page or login page. It handles autz in its own way.
            return true;
        }
        return false;
    }

    public static ModuleAuthentication getProcessingModule(boolean required) {
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if (actualAuthentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (required && moduleAuthentication == null) {
                LOGGER.error("Couldn't find processing module authentication {}", mpAuthentication);
                throw new AuthenticationServiceException("web.security.auth.module.null"); //TODO localization
            }
            return moduleAuthentication;
        } else if (required) {
            LOGGER.error("Type of actual authentication in security context isn't MidpointAuthentication");
            throw new AuthenticationServiceException("web.security.auth.wrong.type"); //TODO localization
        }
        return null;
    }

    public static void saveException(HttpServletRequest request,
                                     AuthenticationException exception) {
        HttpSession session = request.getSession(false);

        request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }
}
