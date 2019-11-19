/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.auth.filter;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.boot.auth.module.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.boot.auth.module.AuthModule;
import com.evolveum.midpoint.web.boot.auth.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.boot.auth.module.factory.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.boot.auth.util.AuthUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 * @author skublik
 */

public class MidpointAuthFilter extends GenericFilterBean {
    private static final transient Trace LOGGER = TraceManager.getTrace(MidpointAuthFilter.class);
    private final Map<Class<? extends Object>, Object> sharedObjects;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Autowired
    private AuthModuleRegistryImpl authRegistry;

    private SecurityFilterChain authenticatedFilter;
    private AuthenticationsPolicyType authenticationPolicy;
    private PreLogoutFilter preLogoutFilter = new PreLogoutFilter();

    public MidpointAuthFilter(Map<Class<? extends Object>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
    }

    public PreLogoutFilter getPreLogoutFilter() {
        return preLogoutFilter;
    }

    public void createFilterForAuthenticatedRequest() {
        ModuleWebSecurityConfig module = objectObjectPostProcessor.postProcess(new ModuleWebSecurityConfig(null));
        module.setObjectPostProcessor(objectObjectPostProcessor);
        try {
            HttpSecurity http = module.getNewHttpSecurity();
            authenticatedFilter = http.build();
        } catch (Exception e) {
            LOGGER.error("Couldn't create filter for authenticated requests", e);
        }
    }

    public AuthenticationsPolicyType getDefaultAuthenticationPolicy() {
        if (authenticationPolicy == null) {
            authenticationPolicy = new AuthenticationsPolicyType();
            AuthenticationModulesType modules = new AuthenticationModulesType();
            AuthenticationModuleLoginFormType loginForm = new AuthenticationModuleLoginFormType();
            loginForm.name("loginForm");
            modules.loginForm(loginForm);
            authenticationPolicy.setModules(modules);
            AuthenticationSequenceType sequence = new AuthenticationSequenceType();
            sequence.name("admin-gui-default");
            AuthenticationSequenceChannelType channel = new AuthenticationSequenceChannelType();
            channel.setDefault(true);
            channel.channelId("http://midpoint.evolveum.com/xml/ns/public/model/channels-3#user");
            sequence.channel(channel);
            AuthenticationSequenceModuleType module = new AuthenticationSequenceModuleType();
            module.name("loginForm");
            module.order(1);
            module.necessity(AuthenticationSequenceModuleNecessityType.SUFFICIENT);
            sequence.module(module);
            authenticationPolicy.sequence(sequence);
        }
        return authenticationPolicy;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        doFilterInternal(request, response, chain);
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
                                  FilterChain chain) throws IOException, ServletException {

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        getPreLogoutFilter().doFilter(httpRequest, response);

        if ((mpAuthentication != null && (mpAuthentication.isAuthenticated()) || AuthUtil.isPermitAll(httpRequest))) {
            processingInMidpoint(httpRequest, response, chain);
            return;
        }

        AuthenticationsPolicyType authenticationsPolicy;
        try {
            PrismObject<SecurityPolicyType> authPolicy = systemObjectCache.getSecurityPolicy(new OperationResult("load authentication policy"));

            if (authPolicy == null || authPolicy.asObjectable().getAuthentication() == null) {
                authenticationsPolicy = getDefaultAuthenticationPolicy();
            } else {
                authenticationsPolicy = authPolicy.asObjectable().getAuthentication();
            }

        } catch (SchemaException e) {
            LOGGER.error("Couldn't load Authentication policy", e);
            authenticationsPolicy = getDefaultAuthenticationPolicy();
        }


        AuthenticationSequenceType sequence = AuthUtil.getSequence(httpRequest.getServletPath(), authenticationsPolicy);
        if (sequence == null) {
            throw new IllegalArgumentException("Couldn't find sequence for URI '" + httpRequest.getRequestURI() + "' in " + authenticationsPolicy);
        }

//        // authentication pages (login, select ID for saml ...) during processing of modules
//        if (AuthUtil.isPermitAll(httpRequest) && mpAuthentication != null && mpAuthentication.isProcessing()) {
//            chain.doFilter(request, response);
//            return;
//        }

        List<AuthModule> authModules;
        //change sequence of authentication during another sequence
        if (mpAuthentication == null || !sequence.equals(mpAuthentication.getSequence())) {
            SecurityContextHolder.getContext().setAuthentication(null);
            authModules = AuthUtil.buildModuleFilters(authRegistry, sequence, httpRequest, authenticationsPolicy.getModules(),
                    sharedObjects);
        } else {
            authModules = mpAuthentication.getAuthModules();
        }

//        this.filterChains = authModules;

        if (authModules == null || authModules.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(UrlUtils.buildRequestUrl(httpRequest)
                        +  "has no filters");
            }
            return;
        }

        int indexOfProcessingModule = -1;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            indexOfProcessingModule = mpAuthentication.getIndexOfProcessingModule(true);
        }

        if (mpAuthentication != null && mpAuthentication.isAuthenticationFailed()) {

            Exception actualException = (Exception) httpRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            String actualMessage;
            String restartFlowMessage = "Authentication failed, and as a consequence\twas restarted authentication flow"; //TODO localization
            if (actualException != null && StringUtils.isNotBlank(actualException.getMessage())) {
                actualMessage = actualException.getMessage() + ";" + restartFlowMessage;
            } else {
                actualMessage = restartFlowMessage;
            }
            AuthenticationException exception = new AuthenticationServiceException(actualMessage);
            AuthUtil.saveException(httpRequest, exception);
        }

        if (indexOfProcessingModule == -1) {
            SecurityContextHolder.getContext().setAuthentication(null);
            SecurityContextHolder.getContext().setAuthentication(new MidpointAuthentication(sequence));
            mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            mpAuthentication.setAuthModules(authModules);
            indexOfProcessingModule = 0;
            mpAuthentication.addAuthentications(authModules.get(indexOfProcessingModule).getBaseModuleAuthentication());
        }

        MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(httpRequest, chain, authModules.get(indexOfProcessingModule).getSecurityFilterChain().getFilters());
        vfc.doFilter(httpRequest, response);

    }

    private void processingInMidpoint(ServletRequest httpRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(httpRequest, chain, authenticatedFilter.getFilters());
        vfc.doFilter(httpRequest, response);
//        chain.doFilter(httpRequest,response);
    }

//    private List<Filter> getFilters(HttpServletRequest request) {
//        for (SecurityFilterChain chain : filterChains) {
//            if (chain.matches(request)) {
//                return chain.getFilters();
//            }
//        }
//
//        return null;
//    }

//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append("FilterChainProxy[");
//        sb.append("Filter Chains: ");
//        sb.append(filterChains);
//        sb.append("]");
//
//        return sb.toString();
//    }

    private static class VirtualFilterChain implements FilterChain {
        private final FilterChain originalChain;
        private final List<Filter> additionalFilters;
        private final int size;
        private int currentPosition = 0;

        private VirtualFilterChain(ServletRequest firewalledRequest,
                                   FilterChain chain, List<Filter> additionalFilters) {
            this.originalChain = chain;
            this.additionalFilters = additionalFilters;
            this.size = additionalFilters.size();
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (currentPosition == size) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl((HttpServletRequest) request)
                            + " reached end of additional filter chain; proceeding with original chain, if url is permit all");
                }

//                MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
//                //authentication pages (login, select ID for saml ...) during processing of modules
//                if (AuthUtil.isPermitAll((HttpServletRequest) request) && mpAuthentication != null && mpAuthentication.isProcessing()) {
//                    originalChain.doFilter(request, response);
//                    return;
//                }
                originalChain.doFilter(request, response);
            }
            else {
                currentPosition++;

                Filter nextFilter = additionalFilters.get(currentPosition - 1);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl((HttpServletRequest) request)
                            + " at position " + currentPosition + " of " + size
                            + " in additional filter chain; firing Filter: '"
                            + nextFilter.getClass().getSimpleName() + "'");
                }

                nextFilter.doFilter(request, response, this);
            }
        }
    }

    public interface FilterChainValidator {
        void validate(MidpointAuthFilter filterChainProxy);
    }

    private static class NullFilterChainValidator implements MidpointAuthFilter.FilterChainValidator {
        @Override
        public void validate(MidpointAuthFilter filterChainProxy) {
        }
    }

}

