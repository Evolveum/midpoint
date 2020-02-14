/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAuthFilter.class);
    private final Map<Class<?>, Object> sharedObjects;

    @Autowired
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    private SystemObjectCache systemObjectCache;

    @Autowired
    private AuthModuleRegistryImpl authModuleRegistry;

    @Autowired
    private AuthChannelRegistryImpl authChannelRegistry;

//    private SecurityFilterChain authenticatedFilter;
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
//        try {
//            HttpSecurity http = module.getNewHttpSecurity();
//            authenticatedFilter = http.build();
//        } catch (Exception e) {
//            LOGGER.error("Couldn't create filter for authenticated requests", e);
//        }
    }

    public AuthenticationsPolicyType getDefaultAuthenticationPolicy() {
        if (authenticationPolicy == null) {
            authenticationPolicy = SecurityPolicyUtil.createDefaultAuthenticationPolicy();
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

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (SecurityUtils.isPermitAll(httpRequest) && !SecurityUtils.isLoginPage(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();

        getPreLogoutFilter().doFilter(request, response);

//        //authenticated request
//        if (mpAuthentication != null && mpAuthentication.isAuthenticated()) {
//            internalProcessing(httpRequest, response, chain);
//            return;
//        }

        //load security policy with authentication
        AuthenticationsPolicyType authenticationsPolicy;
        CredentialsPolicyType credentialsPolicy = null;
        PrismObject<SecurityPolicyType> authPolicy = null;
        try {
            authPolicy = systemObjectCache.getSecurityPolicy(new OperationResult("load authentication policy"));

            //security policy without authentication
            if (authPolicy == null || authPolicy.asObjectable().getAuthentication() == null
                    || authPolicy.asObjectable().getAuthentication().getSequence() == null
                    || authPolicy.asObjectable().getAuthentication().getSequence().isEmpty()) {
                    authenticationsPolicy = getDefaultAuthenticationPolicy();
            } else {
                authenticationsPolicy = authPolicy.asObjectable().getAuthentication();
            }

            if (authPolicy != null) {
                credentialsPolicy = authPolicy.asObjectable().getCredentials();
            }

        } catch (SchemaException e) {
            LOGGER.error("Couldn't load Authentication policy", e);
            authenticationsPolicy = getDefaultAuthenticationPolicy();
        }

        if (SecurityUtils.isIgnoredLocalPath(authenticationsPolicy, httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticationSequenceType sequence;
        // permitAll pages (login, select ID for saml ...) during processing of modules
        if (mpAuthentication != null && SecurityUtils.isLoginPage(httpRequest)) {
            sequence = mpAuthentication.getSequence();
        } else {
            sequence = SecurityUtils.getSequenceByPath(httpRequest, authenticationsPolicy);
        }

        if (mpAuthentication != null && !mpAuthentication.getSequence().equals(sequence) && mpAuthentication.isAuthenticated()
                 && ((sequence != null && sequence.getChannel() != null
                && mpAuthentication.getAuthenticationChannel().getChannelId().equals(sequence.getChannel().getChannelId()))
        || mpAuthentication.getAuthenticationChannel().getChannelId().equals(SecurityUtils.findChannelByRequest(httpRequest)))) {
            sequence = mpAuthentication.getSequence();
        }

        if (sequence == null) {
            throw new IllegalArgumentException("Couldn't find sequence for URI '" + httpRequest.getRequestURI() + "' in authentication of Security Policy with oid " + authPolicy.getOid());
        }

        AuthenticationChannel authenticationChannel = SecurityUtils.buildAuthChannel(authChannelRegistry, sequence);

        List<AuthModule> authModules;
        //change sequence of authentication during another sequence
        if (mpAuthentication == null || !sequence.equals(mpAuthentication.getSequence())) {
            SecurityContextHolder.getContext().setAuthentication(null);
            authModules = SecurityUtils.buildModuleFilters(authModuleRegistry, sequence, httpRequest, authenticationsPolicy.getModules(),
                    credentialsPolicy, sharedObjects, authenticationChannel);
        } else {
            //authenticated request
            if (mpAuthentication != null && mpAuthentication.isAuthenticated()) {
                processingOfAuthenticatedRequest(mpAuthentication, httpRequest, response, chain);
                return;
            }
            authModules = mpAuthentication.getAuthModules();
        }

        //couldn't find authentication modules
        if (authModules == null || authModules.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(UrlUtils.buildRequestUrl(httpRequest)
                        +  "has no filters");
            }
            throw new AuthenticationServiceException("Couldn't find filters for sequence " + sequence.getName());
        }

        int indexOfProcessingModule = -1;
        // if exist authentication (authentication flow is processed) find actual processing module
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            indexOfProcessingModule = mpAuthentication.getIndexOfProcessingModule(true);
            indexOfProcessingModule = mpAuthentication.resolveParallelModules((HttpServletRequest) request, indexOfProcessingModule);
        }

        //authentication flow fail and exist more as one authentication module write error
        if (mpAuthentication != null && mpAuthentication.isAuthenticationFailed() && mpAuthentication.getAuthModules().size() > 1) {

            Exception actualException = (Exception) httpRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            String actualMessage;
            String restartFlowMessage = "web.security.flexAuth.restart.flow";
            if (actualException != null && StringUtils.isNotBlank(actualException.getMessage())) {
                actualMessage = actualException.getMessage() + ";" + restartFlowMessage;
            } else {
                actualMessage = restartFlowMessage;
            }
            AuthenticationException exception = new AuthenticationServiceException(actualMessage);
            SecurityUtils.saveException(httpRequest, exception);
        }

        // if index == -1 indicate restart authentication flow
        if (indexOfProcessingModule == -1) {
            SecurityContextHolder.getContext().setAuthentication(null);
            SecurityContextHolder.getContext().setAuthentication(new MidpointAuthentication(sequence));
            mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            mpAuthentication.setAuthModules(authModules);
            mpAuthentication.setSessionId(httpRequest.getSession().getId());
            indexOfProcessingModule = 0;
            mpAuthentication.addAuthentications(authModules.get(indexOfProcessingModule).getBaseModuleAuthentication());
            indexOfProcessingModule = mpAuthentication.resolveParallelModules((HttpServletRequest) request, indexOfProcessingModule);
        }

        if (mpAuthentication.getAuthenticationChannel() == null) {
            mpAuthentication.setAuthenticationChannel(authenticationChannel);
        }

        MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(httpRequest, chain, authModules.get(indexOfProcessingModule).getSecurityFilterChain().getFilters());
        vfc.doFilter(httpRequest, response);
    }

    private void processingOfAuthenticatedRequest(MidpointAuthentication mpAuthentication, ServletRequest httpRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
            if (StateOfModule.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                int i = mpAuthentication.getIndexOfModule(moduleAuthentication);
                MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(httpRequest, chain,
                        mpAuthentication.getAuthModules().get(i).getSecurityFilterChain().getFilters());
                vfc.doFilter(httpRequest, response);
            }
        }
    }

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

