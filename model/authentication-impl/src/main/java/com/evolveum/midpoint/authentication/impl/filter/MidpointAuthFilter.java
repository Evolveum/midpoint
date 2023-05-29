/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import static com.evolveum.midpoint.schema.util.SecurityPolicyUtil.NO_CUSTOM_IGNORED_LOCAL_PATH;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
    @Autowired private MidpointProviderManager authenticationManager;
    @Autowired private TaskManager taskManager;
    @Autowired private RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;

    private volatile AuthenticationsPolicyType defaultAuthenticationPolicy;

    private final PreLogoutFilter preLogoutFilter = new PreLogoutFilter();

    private final Map<String, List<AuthModule>> authModulesOfSpecificSequences = new HashMap<>();

    public MidpointAuthFilter(Map<Class<?>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
    }

    public PreLogoutFilter getPreLogoutFilter() {
        return preLogoutFilter;
    }

    public void createFilterForAuthenticatedRequest() {
        ModuleWebSecurityConfigurer<?> module =
                objectObjectPostProcessor.postProcess(new ModuleWebSecurityConfigurer<>(null));
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
                    customIgnoredLocalPaths, PrismContext.get().getSchemaRegistry());
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
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();

        validateAuthenticationCanContinue(mpAuthentication, httpRequest);

        if (isPermitAllPage(httpRequest) && (mpAuthentication == null || !mpAuthentication.isAuthenticated())) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticationWrapper authWrapper = initAuthenticationWrapper(mpAuthentication);
        if (authWrapper.isIgnoredLocalPath(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        authWrapper.initializeAuthenticationSequence(mpAuthentication, httpRequest, taskManager);
        if (authWrapper.sequence == null) {
            IllegalArgumentException ex = new IllegalArgumentException(getMessageSequenceIsNull(httpRequest, authWrapper));
            LOGGER.error(ex.getMessage(), ex);
            ((HttpServletResponse) response).sendError(401, "web.security.provider.invalid");
            return;
        }
        setLogoutPath(request, response);

        authWrapper.initializeAuthenticationChannel(authChannelRegistry);
        try {
            initAuthenticationModule(mpAuthentication, authWrapper, httpRequest);
            if (isRequestAuthenticated(mpAuthentication, authWrapper)) {
                processingOfAuthenticatedRequest(mpAuthentication, httpRequest, response, chain);
                return;
            }

            if (wasNotFoundAuthModule(authWrapper)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(httpRequest)
                            + "has no authentication module");
                }
                throw new AuthenticationServiceException("Couldn't find authentication module for sequence " + authWrapper.getSequenceIdentifier());
            }
            resolveErrorWithMoreModules(mpAuthentication, httpRequest);

            executeAuthenticationFilter(mpAuthentication, authWrapper, httpRequest, response, chain);
        } finally {
            removingFiltersAfterProcessing(mpAuthentication, httpRequest);
        }
    }

    private void executeAuthenticationFilter(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper, HttpServletRequest httpRequest, ServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        int indexOfProcessingModule = getIndexOfCurrentProcessingModule(mpAuthentication, httpRequest);
        boolean restartNeeded = needCreateNewAuthenticationToken(mpAuthentication, indexOfProcessingModule, httpRequest);
        if (restartNeeded) {
            indexOfProcessingModule = initNewAuthenticationToken(authWrapper, httpRequest);
            mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
        }
        setAuthenticationChanel(mpAuthentication, authWrapper);
        runFilters(authWrapper, indexOfProcessingModule, chain, httpRequest, response);
    }

    private void validateAuthenticationCanContinue(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        if (mpAuthentication == null) {
            return;
        }
        if (mpAuthentication.authenticationShouldBeAborted()) {
            clearAuthentication(httpRequest);
        }
    }

    private void removingFiltersAfterProcessing(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        if (!AuthSequenceUtil.isClusterSequence(httpRequest) && httpRequest.getSession(false) == null && mpAuthentication != null) {
            removeUnusedSecurityFilterPublisher.publishCustomEvent(mpAuthentication.getAuthModules());
        }
    }

    private void clearAuthentication(HttpServletRequest httpRequest) {
        Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (!AuthSequenceUtil.isClusterSequence(httpRequest) && oldAuthentication instanceof MidpointAuthentication) {
            removeUnusedSecurityFilterPublisher.publishCustomEvent(
                    ((MidpointAuthentication) oldAuthentication).getAuthModules());
        }
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private void runFilters(AuthenticationWrapper authWrapper, int indexOfProcessingModule, FilterChain chain,
            HttpServletRequest httpRequest, ServletResponse response) throws ServletException, IOException {
        VirtualFilterChain vfc = new VirtualFilterChain(
                chain, ((AuthModuleImpl) authWrapper.authModules.get(indexOfProcessingModule)).getSecurityFilterChain().getFilters());
        vfc.doFilter(httpRequest, response);
    }

    private void setAuthenticationChanel(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper) {
        if (mpAuthentication != null && mpAuthentication.getAuthenticationChannel() == null) {
            mpAuthentication.setAuthenticationChannel(authWrapper.authenticationChannel);
        }
    }

    private int initNewAuthenticationToken(AuthenticationWrapper authWrapper, HttpServletRequest httpRequest) {
        if (AuthSequenceUtil.isClusterSequence(httpRequest)) {
            createMpAuthentication(httpRequest, authWrapper);
            return 0;
        } else {
            return restartAuthFlow(httpRequest, authWrapper);
        }
    }

    private boolean needCreateNewAuthenticationToken(MidpointAuthentication mpAuthentication, int indexOfActualProcessingModule, HttpServletRequest httpRequest) {
        return AuthSequenceUtil.isClusterSequence(httpRequest)
                || needRestartAuthFlow(indexOfActualProcessingModule, mpAuthentication);
    }

    private void setLogoutPath(ServletRequest request, ServletResponse response) {
        getPreLogoutFilter().doFilter(request, response);
    }

    private boolean wasNotFoundAuthModule(AuthenticationWrapper authWrapper) {
        return authWrapper.authModules == null || authWrapper.authModules.size() == 0;
    }

    private boolean isRequestAuthenticated(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper) {
        return mpAuthentication != null && mpAuthentication.isAuthenticated()
                && sequenceIdentifiersMatch(authWrapper.sequence, mpAuthentication.getSequence());
    }

    private boolean sequenceIdentifiersMatch(AuthenticationSequenceType seq1, AuthenticationSequenceType seq2) {
        String seqIdentifier1 = AuthSequenceUtil.getAuthSequenceIdentifier(seq1); //StringUtils.isNotEmpty(seq1.getIdentifier()) ? seq1.getIdentifier() : seq1.getName();
        String seqIdentifier2 = AuthSequenceUtil.getAuthSequenceIdentifier(seq2); //StringUtils.isNotEmpty(seq2.getIdentifier()) ? seq2.getIdentifier() : seq2.getName();
        return seqIdentifier1 != null && StringUtils.equals(seqIdentifier1, seqIdentifier2);
    }

    private void initAuthenticationModule(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper, HttpServletRequest httpRequest) {
        if (AuthSequenceUtil.isClusterSequence(httpRequest)) {
            if (authModulesOfSpecificSequences.containsKey(authWrapper.getSequenceIdentifier())) {
                authWrapper.authModules = authModulesOfSpecificSequences.get(authWrapper.getSequenceIdentifier());
                if (authWrapper.authModules != null) {
                    for (AuthModule authModule : authWrapper.authModules) {
                        if (authModule != null && ((AuthModuleImpl) authModule).getConfiguration() != null) {
                            authenticationManager.getProviders().clear();
                            for (AuthenticationProvider authenticationProvider : ((AuthModuleImpl) authModule).getConfiguration().getAuthenticationProviders()) {
                                authenticationManager.getProviders().add(authenticationProvider);
                            }
                        }
                    }
                }
            } else {
                authWrapper.authModules = createAuthenticationModuleBySequence(mpAuthentication, authWrapper, httpRequest);
                authModulesOfSpecificSequences.put(authWrapper.getSequenceIdentifier(), authWrapper.authModules);
            }
        } else {
            authWrapper.authModules = createAuthenticationModuleBySequence(mpAuthentication, authWrapper, httpRequest);
        }
    }

    private String getMessageSequenceIsNull(HttpServletRequest httpRequest, AuthenticationWrapper authWrapper) {
        String message = "Couldn't find sequence for URI '" + httpRequest.getRequestURI();
        if (authWrapper.securityPolicy != null) {
            message += "' in authentication of Security Policy with oid " + authWrapper.securityPolicy.getOid();
        } else {
            message += "' in default authentication.";
        }
        return message;
    }

//    private AuthenticationWrapper defineAuthenticationWrapper(MidpointAuthentication mpAuthentication) {
//        AuthenticationWrapper wrapper = new AuthenticationWrapper();
//        try {
//            wrapper.securityPolicy = resolveSecurityPolicy(mpAuthentication);
//            wrapper.authenticationsPolicy = getAuthenticationPolicy(wrapper.securityPolicy);
//            if (wrapper.securityPolicy != null) {
//                wrapper.credentialsPolicy = wrapper.securityPolicy.asObjectable().getCredentials();
//            }
//        } catch (SchemaException e) {
//            LOGGER.error("Couldn't load Authentication policy", e);
//            try {
//                wrapper.authenticationsPolicy = getDefaultAuthenticationPolicy(NO_CUSTOM_IGNORED_LOCAL_PATH);
//            } catch (SchemaException schemaException) {
//                LOGGER.error("Couldn't get default authentication policy");
//                throw new IllegalArgumentException("Couldn't get default authentication policy", e);
//            }
//        }
//        return wrapper;
//    }

    private AuthenticationWrapper initAuthenticationWrapper(MidpointAuthentication mpAuthentication) {
        PrismObject<SecurityPolicyType> securityPolicy = null;
        try {
            securityPolicy = resolveSecurityPolicy(mpAuthentication);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load security policy", e);
        }

        AuthenticationsPolicyType authenticationsPolicy = null;
        try {
            authenticationsPolicy = getAuthenticationPolicy(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get default authentication policy");
            throw new IllegalArgumentException("Couldn't get default authentication policy", e);
        }

        return new AuthenticationWrapper(securityPolicy, authenticationsPolicy);

//        AuthenticationWrapper wrapper = new AuthenticationWrapper();
//        try {
//            wrapper.securityPolicy =
//            wrapper.authenticationsPolicy = getAuthenticationPolicy(wrapper.securityPolicy);
//            if (wrapper.securityPolicy != null) {
//                wrapper.credentialsPolicy = wrapper.securityPolicy.asObjectable().getCredentials();
//            }
//        } catch (SchemaException e) {
//            LOGGER.error("Couldn't load Authentication policy", e);
//            try {
//                wrapper.authenticationsPolicy = getDefaultAuthenticationPolicy(NO_CUSTOM_IGNORED_LOCAL_PATH);
//            } catch (SchemaException schemaException) {
//                LOGGER.error("Couldn't get default authentication policy");
//                throw new IllegalArgumentException("Couldn't get default authentication policy", e);
//            }
//        }
//        return wrapper;
    }

    private PrismObject<SecurityPolicyType> resolveSecurityPolicy(MidpointAuthentication mpAuthentication) throws SchemaException {
        SecurityPolicyType securityPolicyType = mpAuthentication == null ? null : mpAuthentication.resolveSecurityPolicy();
        return securityPolicyType == null ? getGlobalSecurityPolicy() : securityPolicyType.asPrismObject();
    }

    private PrismObject<SecurityPolicyType> getGlobalSecurityPolicy() throws SchemaException {
        return systemObjectCache.getSecurityPolicy();
    }

    private boolean isPermitAllPage(HttpServletRequest request) {
        return AuthSequenceUtil.isPermitAll(request) && !AuthSequenceUtil.isLoginPage(request);
    }

    private boolean needRestartAuthFlow(int indexOfProcessingModule, MidpointAuthentication mpAuthentication) {
        // if index == -1 indicate restart authentication flow
        return (isNotIdentifiedFocus(mpAuthentication) && isAlreadyAudited(mpAuthentication)) || indexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX;
    }

    private boolean isAlreadyAudited(MidpointAuthentication mpAuthentication) {
        if (mpAuthentication == null) {
            return true;
        }
        return mpAuthentication.isAlreadyAudited();
    }

    private boolean isNotIdentifiedFocus(MidpointAuthentication mpAuthentication) {
        if (mpAuthentication == null) {
            return true;
        }

        Object principal = mpAuthentication.getPrincipal();
        if (!(principal instanceof MidPointPrincipal)) {
            return true;
        }

        return ((MidPointPrincipal) principal).getFocus() == null;
    }

    private int restartAuthFlow(HttpServletRequest httpRequest, AuthenticationWrapper authWrapper) {
        createMpAuthentication(httpRequest, authWrapper);
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
        return mpAuthentication.resolveParallelModules(httpRequest, 0);
    }

    private void createMpAuthentication(HttpServletRequest httpRequest, AuthenticationWrapper authWrapper) {
        MidpointAuthentication mpAuthentication = new MidpointAuthentication(authWrapper.sequence);
        mpAuthentication.setSharedObjects(sharedObjects);
        mpAuthentication.setAuthModules(authWrapper.authModules);
        mpAuthentication.setSessionId(httpRequest.getSession(false) != null ?
                httpRequest.getSession(false).getId() : RandomStringUtils.random(30, true, true).toUpperCase());
        mpAuthentication.addAuthentications(authWrapper.authModules.get(0).getBaseModuleAuthentication());
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            mpAuthentication.setPrincipal(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }
        clearAuthentication(httpRequest);
        SecurityContextHolder.getContext().setAuthentication(mpAuthentication);
    }

    //todo decide if we still need it
    private void resolveErrorWithMoreModules(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        if (existMoreAsOneAuthModule(mpAuthentication)) {
            Exception actualException = (Exception) httpRequest.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            String actualMessage;
            String restartFlowMessage = "web.security.flexAuth.restart.flow";
            if (actualException != null && StringUtils.isNotBlank(actualException.getMessage())) {
                actualMessage = actualException.getMessage() + ";" + restartFlowMessage;
            } else {
                actualMessage = restartFlowMessage;
            }
            AuthenticationException exception = new AuthenticationServiceException(actualMessage);
            AuthSequenceUtil.saveException(httpRequest, exception);
        }
    }

    private boolean existMoreAsOneAuthModule(MidpointAuthentication mpAuthentication) {
        return mpAuthentication != null && mpAuthentication.isAuthenticationFailed() && mpAuthentication.getAuthModules().size() > 1;
    }

    private int getIndexOfCurrentProcessingModule(MidpointAuthentication mpAuthentication, HttpServletRequest request) {
        int indexOfProcessingModule = MidpointAuthentication.NO_MODULE_FOUND_INDEX;
        // if exist authentication (authentication flow is processed) find actual processing module
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            indexOfProcessingModule = mpAuthentication.getIndexOfProcessingModule(true);
            indexOfProcessingModule = mpAuthentication.resolveParallelModules(request, indexOfProcessingModule);
        }
        return indexOfProcessingModule;
    }

    private List<AuthModule> createAuthenticationModuleBySequence(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper,
            HttpServletRequest httpRequest) {
        List<AuthModule> authModules;
        if (processingDifferentAuthenticationSequence(mpAuthentication, authWrapper.sequence)) {
            clearAuthentication(httpRequest);
            authenticationManager.getProviders().clear();
            authModules = AuthSequenceUtil.buildModuleFilters(
                    authModuleRegistry, authWrapper.sequence, httpRequest, authWrapper.authenticationsPolicy.getModules(),
                    authWrapper.credentialsPolicy, sharedObjects, authWrapper.authenticationChannel);
        } else {
            authModules = mpAuthentication.getAuthModules();
        }
        return authModules;
    }

    private boolean processingDifferentAuthenticationSequence(MidpointAuthentication mpAuthentication, AuthenticationSequenceType sequence) {
        return mpAuthentication == null || !sequenceIdentifiersMatch(sequence, mpAuthentication.getSequence());
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

    private void processingOfAuthenticatedRequest(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
            if (AuthenticationModuleState.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                if(AuthSequenceUtil.isUrlForAuthProcessing(httpRequest)) {
                    new DefaultRedirectStrategy().sendRedirect(httpRequest, (HttpServletResponse) response, "/");
                    return;
                }
                int i = mpAuthentication.getIndexOfModule(moduleAuthentication);
                VirtualFilterChain vfc = new VirtualFilterChain(chain,
                        ((AuthModuleImpl) mpAuthentication.getAuthModules().get(i)).getSecurityFilterChain().getFilters());
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
                // If the previous filter has already returned a response, skip the original filter
                // to prevent duplicate response writes
                if (response.isCommitted()) {
                    return;
                }
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

    private class AuthenticationWrapper {
        AuthenticationsPolicyType authenticationsPolicy;
        CredentialsPolicyType credentialsPolicy = null;
        PrismObject<SecurityPolicyType> securityPolicy = null;
        List<AuthModule> authModules;
        AuthenticationSequenceType sequence = null;
        AuthenticationChannel authenticationChannel;

        AuthenticationWrapper(PrismObject<SecurityPolicyType> securityPolicy, AuthenticationsPolicyType authenticationsPolicy) {
            this.securityPolicy = securityPolicy;
            this.authenticationsPolicy = authenticationsPolicy;
            this.credentialsPolicy = securityPolicy != null ? securityPolicy.asObjectable().getCredentials() : null;
        }

        public String getSequenceIdentifier() {
            return AuthSequenceUtil.getAuthSequenceIdentifier(sequence);
        }

        public boolean isIgnoredLocalPath(HttpServletRequest httpRequest) {
            if (authenticationsPolicy != null && authenticationsPolicy.getIgnoredLocalPath() != null
                    && !authenticationsPolicy.getIgnoredLocalPath().isEmpty()) {
                List<String> ignoredPaths = authenticationsPolicy.getIgnoredLocalPath();
                for (String ignoredPath : ignoredPaths) {
                    AntPathRequestMatcher matcher = new AntPathRequestMatcher(ignoredPath);
                    if (matcher.matches(httpRequest)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void initializeAuthenticationSequence(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest, TaskManager taskManager) {
            if (mpAuthentication != null && AuthSequenceUtil.isLoginPage(httpRequest)) {
                if (mpAuthentication.getAuthenticationChannel() != null && !mpAuthentication.getAuthenticationChannel()
                        .getChannelId().equals(AuthSequenceUtil.findChannelByRequest(httpRequest))
                        && AuthSequenceUtil.getSequenceByPath(httpRequest, authenticationsPolicy, taskManager.getLocalNodeGroups()) == null) {
                    return;
                }
                this.sequence = mpAuthentication.getSequence();
            } else {
                this.sequence = AuthSequenceUtil.getSequenceByPath(httpRequest, authenticationsPolicy, taskManager.getLocalNodeGroups());
            }

            if (sequence != null && isEqualChannelIdForAuthenticatedUser(mpAuthentication, httpRequest)) {
                changeLogoutToNewSequence(mpAuthentication, httpRequest);
                this.sequence = mpAuthentication.getSequence();
            }
        }

        public void initializeAuthenticationChannel(AuthChannelRegistryImpl authChannelRegistry) {
            this.authenticationChannel = AuthSequenceUtil.buildAuthChannel(authChannelRegistry, sequence);
        }

        private boolean isEqualChannelIdForAuthenticatedUser(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
            return mpAuthentication != null && !sequenceIdentifiersMatch(mpAuthentication.getSequence(), sequence) && mpAuthentication.isAuthenticated()
                    && (((sequence != null && sequence.getChannel() != null && mpAuthentication.getAuthenticationChannel().matchChannel(sequence)))
                    || mpAuthentication.getAuthenticationChannel().getChannelId().equals(AuthSequenceUtil.findChannelByRequest(httpRequest)));
        }

        private void changeLogoutToNewSequence(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
            if (AuthSequenceUtil.isBasePathForSequence(httpRequest, sequence)) {
                mpAuthentication.getAuthenticationChannel().setPathAfterLogout(httpRequest.getServletPath());
                ModuleAuthenticationImpl authenticatedModule = (ModuleAuthenticationImpl) AuthUtil.getAuthenticatedModule();
                if (authenticatedModule != null) {
                    authenticatedModule.setInternalLogout(true);
                }
            }
        }


    }
}

