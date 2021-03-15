/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import static com.evolveum.midpoint.schema.util.SecurityPolicyUtil.NO_CUSTOM_IGNORED_LOCAL_PATH;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidpointAuthenticationManager;
import com.evolveum.midpoint.web.security.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.module.ModuleWebSecurityConfig;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
public class MidpointAuthFilter extends GenericFilterBean {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAuthFilter.class);
    private final Map<Class<?>, Object> sharedObjects;

    @Autowired private ObjectPostProcessor<Object> objectObjectPostProcessor;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private AuthModuleRegistryImpl authModuleRegistry;
    @Autowired private AuthChannelRegistryImpl authChannelRegistry;
    @Autowired private MidpointAuthenticationManager authenticationManager;
    @Autowired private PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired private RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;

    private volatile AuthenticationsPolicyType defaultAuthenticationPolicy;

    private final PreLogoutFilter preLogoutFilter = new PreLogoutFilter();

    private Map<String, List<AuthModule>> authModulesOfSpecificSequences = new HashMap<>();

    public MidpointAuthFilter(Map<Class<?>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
    }

    public PreLogoutFilter getPreLogoutFilter() {
        return preLogoutFilter;
    }

    public void createFilterForAuthenticatedRequest() {
        ModuleWebSecurityConfig<?> module =
                objectObjectPostProcessor.postProcess(new ModuleWebSecurityConfig<>(null));
        module.setObjectPostProcessor(objectObjectPostProcessor);
    }

    /**
     * Creates default authentication policy because the configured one is empty.
     * Either <b>authentication</b> element is missing, or it has no <b>sequence</b> elements.
     * However, if there are some <b>ignoreLocalPath</b> elements defined (not null or empty),
     * they override the default ignored paths.
     * <p>
     * The default policy is cached for this filter, if there are changes affecting the default
     * policy (e.g. only changes to <b>ignoreLocalPath</b> without any <b>sequence</b> elements),
     * midPoint must be restarted.
     */
    private AuthenticationsPolicyType getDefaultAuthenticationPolicy(
            List<String> customIgnoredLocalPaths) throws SchemaException {
        if (defaultAuthenticationPolicy == null) {
            defaultAuthenticationPolicy = SecurityPolicyUtil.createDefaultAuthenticationPolicy(
                    customIgnoredLocalPaths, prismContext.getSchemaRegistry());
        }
        return defaultAuthenticationPolicy;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        doFilterInternal(request, response, chain);
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        //request for permit all page (for example errors and login pages)
        if (SecurityUtils.isPermitAll(httpRequest) && !SecurityUtils.isLoginPage(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();

        AuthenticationsPolicyType authenticationsPolicy;
        CredentialsPolicyType credentialsPolicy = null;
        PrismObject<SecurityPolicyType> securityPolicy = null;
        try {
            securityPolicy = getSecurityPolicy();
            authenticationsPolicy = getAuthenticationPolicy(securityPolicy);
            if (securityPolicy != null) {
                credentialsPolicy = securityPolicy.asObjectable().getCredentials();
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load Authentication policy", e);
            try {
                authenticationsPolicy = getDefaultAuthenticationPolicy(NO_CUSTOM_IGNORED_LOCAL_PATH);
            } catch (SchemaException schemaException) {
                LOGGER.error("Couldn't get default authentication policy");
                throw new IllegalArgumentException("Couldn't get default authentication policy", e);
            }
        }

        //is path for which is ignored authentication
        if (SecurityUtils.isIgnoredLocalPath(authenticationsPolicy, httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticationSequenceType sequence =
                getAuthenticationSequence(mpAuthentication, httpRequest, authenticationsPolicy);
        if (sequence == null) {
            throw new IllegalArgumentException("Couldn't find sequence for URI '" + httpRequest.getRequestURI()
                    + "' in authentication of Security Policy with oid " + securityPolicy.getOid());
        }

        //change generic logout path to logout path for actual module
        getPreLogoutFilter().doFilter(request, response);

        AuthenticationChannel authenticationChannel = SecurityUtils.buildAuthChannel(authChannelRegistry, sequence);

        try {
            List<AuthModule> authModules;
            if (SecurityUtils.isSpecificSequence(httpRequest)) {
                if (authModulesOfSpecificSequences.keySet().contains(sequence.getName())) {
                    authModules = authModulesOfSpecificSequences.get(sequence.getName());
                    if (authModules != null) {
                        for (AuthModule authModule : authModules) {
                            if (authModule != null && authModule.getConfiguration() != null) {
                                authenticationManager.getProviders().clear();
                                for (AuthenticationProvider authenticationProvider : authModule.getConfiguration().getAuthenticationProviders()) {
                                    authenticationManager.getProviders().add(authenticationProvider);
                                }
                            }
                        }
                    }
                } else {
                    authModules = createAuthenticationModuleBySequence(
                            mpAuthentication, sequence, httpRequest, authenticationsPolicy.getModules(), authenticationChannel, credentialsPolicy);
                    authModulesOfSpecificSequences.put(sequence.getName(), authModules);
                }
            } else {
                authModules = createAuthenticationModuleBySequence(
                        mpAuthentication, sequence, httpRequest, authenticationsPolicy.getModules(), authenticationChannel, credentialsPolicy);
            }

            //authenticated request
            if (mpAuthentication != null && mpAuthentication.isAuthenticated() && sequence.equals(mpAuthentication.getSequence())) {
                processingOfAuthenticatedRequest(mpAuthentication, httpRequest, response, chain);
                return;
            }

            //couldn't find authentication modules
            if (authModules == null || authModules.size() == 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(httpRequest)
                            + "has no filters");
                }
                throw new AuthenticationServiceException("Couldn't find filters for sequence " + sequence.getName());
            }

            int indexOfProcessingModule;

            resolveErrorWithMoreModules(mpAuthentication, httpRequest);

            if (SecurityUtils.isSpecificSequence(httpRequest)) {
                indexOfProcessingModule = 0;
                createMpAuthentication(httpRequest, sequence, authModules);
                mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
            } else {
                indexOfProcessingModule = getIndexOfActualProcessingModule(mpAuthentication, httpRequest);
                if (needRestartAuthFlow(indexOfProcessingModule)) {
                    indexOfProcessingModule = restartAuthFlow(httpRequest, sequence, authModules);
                    mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
                }
            }

            if (mpAuthentication.getAuthenticationChannel() == null) {
                mpAuthentication.setAuthenticationChannel(authenticationChannel);
            }

            MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(
                    chain, authModules.get(indexOfProcessingModule).getSecurityFilterChain().getFilters());
            vfc.doFilter(httpRequest, response);
        } finally {
            if (!SecurityUtils.isSpecificSequence(httpRequest) && httpRequest.getSession(false) == null && mpAuthentication instanceof MidpointAuthentication) {
                removeUnusedSecurityFilterPublisher.publishCustomEvent(mpAuthentication);
            }
        }
    }

    private boolean needRestartAuthFlow(int indexOfProcessingModule) {
        // if index == -1 indicate restart authentication flow
        return indexOfProcessingModule == -1;
    }

    private int restartAuthFlow(HttpServletRequest httpRequest, AuthenticationSequenceType sequence, List<AuthModule> authModules) {
        createMpAuthentication(httpRequest, sequence, authModules);
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return mpAuthentication.resolveParallelModules(httpRequest, 0);
    }

    private void createMpAuthentication(HttpServletRequest httpRequest, AuthenticationSequenceType sequence, List<AuthModule> authModules) {
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.getContext().setAuthentication(new MidpointAuthentication(sequence));
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
        mpAuthentication.setAuthModules(authModules);
        mpAuthentication.setSessionId(httpRequest.getSession(false) != null ? httpRequest.getSession(false).getId() : RandomStringUtils.random(30, true, true).toUpperCase());
        mpAuthentication.addAuthentications(authModules.get(0).getBaseModuleAuthentication());
    }

    private void resolveErrorWithMoreModules(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
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
    }

    private int getIndexOfActualProcessingModule(MidpointAuthentication mpAuthentication, HttpServletRequest request) {
        int indexOfProcessingModule = -1;
        // if exist authentication (authentication flow is processed) find actual processing module
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            indexOfProcessingModule = mpAuthentication.getIndexOfProcessingModule(true);
            indexOfProcessingModule = mpAuthentication.resolveParallelModules(request, indexOfProcessingModule);
        }
        return indexOfProcessingModule;
    }

    private List<AuthModule> createAuthenticationModuleBySequence(MidpointAuthentication mpAuthentication, AuthenticationSequenceType sequence,
            HttpServletRequest httpRequest, AuthenticationModulesType modules, AuthenticationChannel authenticationChannel, CredentialsPolicyType credentialsPolicy) {
        List<AuthModule> authModules;
        //change sequence of authentication during another sequence
        if (mpAuthentication == null || !sequence.equals(mpAuthentication.getSequence())) {
            SecurityContextHolder.getContext().setAuthentication(null);
            authenticationManager.getProviders().clear();
            authModules = SecurityUtils.buildModuleFilters(
                    authModuleRegistry, sequence, httpRequest, modules,
                    credentialsPolicy, sharedObjects, authenticationChannel);
        } else {
            authModules = mpAuthentication.getAuthModules();
        }
        return authModules;
    }

    private AuthenticationSequenceType getAuthenticationSequence(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest, AuthenticationsPolicyType authenticationsPolicy) {
        AuthenticationSequenceType sequence;
        // permitAll pages (login, select ID for saml ...) during processing of modules
        if (mpAuthentication != null && SecurityUtils.isLoginPage(httpRequest)) {
            sequence = mpAuthentication.getSequence();
        } else {
            sequence = SecurityUtils.getSequenceByPath(httpRequest, authenticationsPolicy, taskManager.getLocalNodeGroups());
        }

        // use same sequence if focus is authenticated and channel id of new sequence is same
        if (mpAuthentication != null && !mpAuthentication.getSequence().equals(sequence) && mpAuthentication.isAuthenticated()
                && (((sequence != null && sequence.getChannel() != null && mpAuthentication.getAuthenticationChannel().matchChannel(sequence)))
                || mpAuthentication.getAuthenticationChannel().getChannelId().equals(SecurityUtils.findChannelByRequest(httpRequest)))) {
            //change logout path to new sequence
            if (SecurityUtils.isBasePathForSequence(httpRequest, sequence)) {
                mpAuthentication.getAuthenticationChannel().setPathAfterLogout(httpRequest.getServletPath());
                ModuleAuthentication authenticatedModule = SecurityUtils.getAuthenticatedModule();
                authenticatedModule.setInternalLogout(true);
            }
            sequence = mpAuthentication.getSequence();

        }
        return sequence;
    }

    private AuthenticationsPolicyType getAuthenticationPolicy(
            PrismObject<SecurityPolicyType> securityPolicy) throws SchemaException {

        if (securityPolicy == null || securityPolicy.asObjectable().getAuthentication() == null) {
            // there is no <authentication> element, we want default without any changes
            return getDefaultAuthenticationPolicy(NO_CUSTOM_IGNORED_LOCAL_PATH);
        } else if (securityPolicy.asObjectable().getAuthentication().getSequence() == null
                || securityPolicy.asObjectable().getAuthentication().getSequence().isEmpty()) {
            // in this case we want to honour eventual <ignoreLocalPath> elements
            return getDefaultAuthenticationPolicy(
                    securityPolicy.asObjectable().getAuthentication().getIgnoredLocalPath());
        } else {
            return securityPolicy.asObjectable().getAuthentication();
        }
    }

    private PrismObject<SecurityPolicyType> getSecurityPolicy() throws SchemaException {
        return systemObjectCache.getSecurityPolicy();
    }

    private void processingOfAuthenticatedRequest(MidpointAuthentication mpAuthentication, ServletRequest httpRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
            if (StateOfModule.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                int i = mpAuthentication.getIndexOfModule(moduleAuthentication);
                MidpointAuthFilter.VirtualFilterChain vfc = new MidpointAuthFilter.VirtualFilterChain(chain,
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

        private VirtualFilterChain(FilterChain chain, List<Filter> additionalFilters) {
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
            } else {
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
}

