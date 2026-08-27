/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.authentication.impl.MidpointAutowiredBeanFactoryObjectPostProcessor;
import com.evolveum.midpoint.authentication.impl.channel.IdentityRecoveryAuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.util.MidpointRequestMatchers;
import com.evolveum.midpoint.model.api.ModelInteractionService;

import com.evolveum.midpoint.security.api.Authorization;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.ArchetypeSelectionModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

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
    @Autowired private ModelInteractionService modelInteractionService;

    private final PreLogoutFilter preLogoutFilter = new PreLogoutFilter();


    public MidpointAuthFilter(Map<Class<?>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
    }

    public PreLogoutFilter getPreLogoutFilter() {
        return preLogoutFilter;
    }

    public void createFilterForAuthenticatedRequest() {
        ModuleWebSecurityConfigurer<?, ?> module =
                objectObjectPostProcessor.postProcess(new ModuleWebSecurityConfigurer<>());
        module.setObjectPostProcessor(objectObjectPostProcessor);
        if (objectObjectPostProcessor instanceof MidpointAutowiredBeanFactoryObjectPostProcessor midpointPostProcessor) {
            midpointPostProcessor.setAfterInitialization();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        doFilterInternal(request, response, chain);
    }

    private MidpointAuthentication getMidpointAuthentication() {
        return (MidpointAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        MidpointAuthentication mpAuthentication = getMidpointAuthentication();

        resetRetryableModuleIfNeeded(mpAuthentication);

        if (shouldAbortAuthentication(mpAuthentication)) {
            clearAuthentication(httpRequest);
            mpAuthentication = getMidpointAuthentication();
        }

        // Use the default GUI entry point for unauthenticated root requests,
        // it resolves the actual home page after successful authentication.
        if (isRootPage(httpRequest) && (mpAuthentication == null || !mpAuthentication.isAuthenticated())) {
            new DefaultRedirectStrategy().sendRedirect(
                    httpRequest, (HttpServletResponse) response, AuthConstants.DEFAULT_PATH_AFTER_LOGIN);
            return;
        }

        if (isPermitAllPage(httpRequest) && (mpAuthentication == null || !mpAuthentication.isAuthenticated())) {
            chain.doFilter(request, response);
            return;
        }

        // Once the wrapper is created the auth modules (and their filters) have been built and registered
        // in the ObjectPostProcessor's disposableBeans list. From here on every exit path must run through
        // removingFiltersAfterProcessing in the finally block, otherwise the filters built for this request
        // leak (e.g. for ignored local paths such as /actuator/health on the sessionless actuator channel).
        AuthenticationWrapper authWrapper = initAuthenticationWrapper(mpAuthentication, httpRequest);
        try {
            initPrincipalService(mpAuthentication, authWrapper);
            if (authWrapper.isIgnoredLocalPath(httpRequest)) {
                chain.doFilter(request, response);
                return;
            }

            if (authWrapper.getSequence() == null) {
                IllegalArgumentException ex = new IllegalArgumentException(getMessageSequenceIsNull(httpRequest, authWrapper));
                LOGGER.error(ex.getMessage(), ex);
                ((HttpServletResponse) response).sendError(401, "web.security.provider.invalid");
                return;
            }
            setLogoutPath(request, response);

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

            if (!response.isCommitted()) {
                executeAuthenticationFilter(mpAuthentication, authWrapper, httpRequest, response, chain);
            }
        } finally {
            removingFiltersAfterProcessing(mpAuthentication, authWrapper, httpRequest);
        }
    }

    @VisibleForTesting
    boolean resolveErrorWithWrongConfigurationOfModules(
            MidpointAuthentication mpAuthentication,
            int indexOfProcessingModule,
            HttpServletRequest httpRequest,
            ServletResponse response) throws IOException {
        if (mpAuthentication == null) {
            return false;
        }

        if (mpAuthentication.getAuthModules().stream()
                .noneMatch(module ->
                        AuthenticationModuleState.FAILURE_CONFIGURATION == module.getBaseModuleAuthentication().getState())) {
            return false;
        }

        if (indexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            return false;
        }

        if (AuthenticationModuleState.FAILURE_CONFIGURATION ==
                mpAuthentication.getAuthModules().get(indexOfProcessingModule).getBaseModuleAuthentication().getState()) {
            InternalAuthenticationServiceException ex = new InternalAuthenticationServiceException(
                    "web.security.flexAuth.wrong.auth.modules.config");
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                AuthSequenceUtil.saveException(httpRequest, ex);
            }

            if (indexOfProcessingModule == 0) {
                // Stop the broken authentication flow after returning the 401 response.
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return true;
            }
        }
        return false;
    }

    private void executeAuthenticationFilter(
            MidpointAuthentication mpAuthentication,
            AuthenticationWrapper authWrapper,
            HttpServletRequest httpRequest,
            ServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (mpAuthentication != null && authWrapper.getAuthModules().size() != mpAuthentication.getAuthModules().size()) {
            mpAuthentication.setAuthModules(authWrapper.getAuthModules());
        }

        // Re-enter identification when its form is submitted for an existing authentication flow.
        int indexOfProcessingModule = resetToIdentificationModuleIfRequested(
                mpAuthentication, authWrapper.getAuthModules(), authWrapper.getSequence(), httpRequest);
        if (indexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            indexOfProcessingModule = getIndexOfCurrentProcessingModule(mpAuthentication, httpRequest);
        }

        int originalIndexOfProcessingModule = indexOfProcessingModule;

        boolean restartNeeded = needCreateNewAuthenticationToken(mpAuthentication, indexOfProcessingModule, httpRequest);
        if (restartNeeded) {
            indexOfProcessingModule = initNewAuthenticationToken(authWrapper, httpRequest, (HttpServletResponse) response);
            mpAuthentication = AuthUtil.getMidpointAuthentication();
        }

        if (originalIndexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            originalIndexOfProcessingModule = indexOfProcessingModule;
        }

        // Do not continue with the authentication filter after the configuration error was handled.
        if (resolveErrorWithWrongConfigurationOfModules(
                mpAuthentication, originalIndexOfProcessingModule, httpRequest, response)) {
            return;
        }

        setAuthenticationChanel(mpAuthentication, authWrapper);

        if (skipNonApplicableModule(mpAuthentication, indexOfProcessingModule, httpRequest, (HttpServletResponse) response)) {
            return;
        }

        runFilters(authWrapper, indexOfProcessingModule, chain, httpRequest, response);
    }

    /**
     * Pre-flight applicability check.
     *
     * When a module reports that it is not applicable for the current user
     * (e.g. a TOTP module with {@code acceptEmpty=true} when the user has no
     * TOTP credentials registered), we skip the module's filter chain entirely:
     * the module is marked as {@link AuthenticationModuleState#CALLED_OFF}, the
     * security context is persisted and, if the overall authentication sequence
     * is now complete, the user is redirected straight to the success URL.
     *
     * Without this check the request would fall through the module's filter chain
     * without any authentication happening and without any redirect, causing the
     * browser to land on the raw sequence URL which has no Wicket page mapped to
     * it (resulting in a 404).
     *
     * @return {@code true} when the module was called off and a redirect was
     *         issued (caller must not call {@link #runFilters} afterwards),
     *         {@code false} otherwise.
     */
    private boolean skipNonApplicableModule(
            MidpointAuthentication mpAuthentication,
            int moduleIndex,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (mpAuthentication == null || moduleIndex < 0) {
            return false;
        }
        List<ModuleAuthentication> authentications = mpAuthentication.getAuthentications();
        if (moduleIndex >= authentications.size()) {
            return false;
        }
        ModuleAuthentication module = authentications.get(moduleIndex);
        if (module.applicable()) {
            return false;
        }

        LOGGER.debug("Module '{}' is not applicable for the current user – marking as CALLED_OFF.",
                module.getModuleIdentifier());

        module.setState(AuthenticationModuleState.CALLED_OFF);

        if (!AuthSequenceUtil.isRecordSessionLessAccessChannel(request)) {
            saveAuthenticationContext(request, response);
        }

        if (mpAuthentication.isAuthenticated()) {
            String redirectUrl = mpAuthentication.getAuthenticationChannel()
                    .getPathAfterSuccessfulAuthentication();
            LOGGER.debug("Authentication sequence complete after calling off module '{}' – redirecting to '{}'.",
                    module.getModuleIdentifier(), redirectUrl);
            new DefaultRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return true;
        }

        return false;
    }

    private boolean shouldAbortAuthentication(MidpointAuthentication mpAuthentication) {
        return mpAuthentication == null || mpAuthentication.authenticationShouldBeAborted();
    }

    private void removingFiltersAfterProcessing(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper, HttpServletRequest httpRequest) {
        if (!AuthSequenceUtil.isClusterSequence(httpRequest) && AuthSequenceUtil.isRecordSessionLessAccessChannel(httpRequest)) {
            if (mpAuthentication == null) {
                if (authWrapper != null && authWrapper.getAuthModules() != null) {
                    removeUnusedSecurityFilterPublisher.publishCustomEvent(authWrapper.getAuthModules());
                }
            } else {
                removeUnusedSecurityFilterPublisher.publishCustomEvent(mpAuthentication.getAuthModules());
            }
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
                chain, authWrapper.getAuthModules().get(indexOfProcessingModule).getSecurityFilterChain().getFilters());
            vfc.doFilter(httpRequest, response);
    }

    private void setAuthenticationChanel(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper) {
        if (mpAuthentication != null && mpAuthentication.getAuthenticationChannel() == null) {
            mpAuthentication.setAuthenticationChannel(authWrapper.getAuthenticationChannel());
        }
    }

    private int initNewAuthenticationToken(
            AuthenticationWrapper authWrapper, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (AuthSequenceUtil.isClusterSequence(httpRequest)) {
            createMpAuthentication(httpRequest, authWrapper);
            return 0;
        } else {
            return restartAuthFlow(httpRequest, authWrapper, httpResponse);
        }
    }

    private boolean needCreateNewAuthenticationToken(MidpointAuthentication mpAuthentication, int indexOfActualProcessingModule, HttpServletRequest httpRequest) {
        boolean restartNeeded =  AuthSequenceUtil.isClusterSequence(httpRequest)
                || needRestartAuthFlow(indexOfActualProcessingModule, mpAuthentication);

        if (!restartNeeded) {
            ModuleAuthentication authentication = mpAuthentication.getAuthentications().get(indexOfActualProcessingModule);
            if (AuthenticationModuleState.FAILURE_CONFIGURATION == authentication.getState()) {
                return true;
            }
        }

        return restartNeeded;
    }

    private void setLogoutPath(ServletRequest request, ServletResponse response) {
        getPreLogoutFilter().doFilter(request, response);
    }

    private boolean wasNotFoundAuthModule(AuthenticationWrapper authWrapper) {
        return authWrapper.getAuthModules() == null || authWrapper.getAuthModules().isEmpty();
    }

    private boolean isRequestAuthenticated(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper) {
        return mpAuthentication != null && mpAuthentication.isAuthenticated()
                && authWrapper.sequenceIdentifiersMatch(mpAuthentication.getSequence());
    }

    private String getMessageSequenceIsNull(HttpServletRequest httpRequest, AuthenticationWrapper authWrapper) {
        String message = "Couldn't find sequence for URI '" + httpRequest.getRequestURI();
        if (authWrapper.getSecurityPolicy() != null) {
            message += "' in authentication of Security Policy with oid " + authWrapper.getSecurityPolicy().getOid();
        } else {
            message += "' in default authentication.";
        }
        return message;
    }

    private AuthenticationWrapper initAuthenticationWrapper(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        return new AuthenticationWrapper(
                authenticationManager,
                authModuleRegistry,
                sharedObjects,
                removeUnusedSecurityFilterPublisher,
                systemObjectCache,
                modelInteractionService)
                .create(mpAuthentication, httpRequest, taskManager, authChannelRegistry);
    }

    private void initPrincipalService(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper) {
        if (mpAuthentication == null || authWrapper == null) {
            return;
        }
        if (authWrapper.getAuthenticationChannel() instanceof IdentityRecoveryAuthenticationChannel channel) {
            var identityRecoveryService = channel.getIdentityRecoveryService();
            var midpointPrincipal = MidPointPrincipal.create(identityRecoveryService);
            identityRecoveryService.getAuthorization().forEach(
                    a -> midpointPrincipal.addAuthorization(Authorization.create(a, "identity recovery service")));
            mpAuthentication.setPrincipal(midpointPrincipal);
        }
    }


    private boolean isPermitAllPage(HttpServletRequest request) {
        return AuthSequenceUtil.isPermitAll(request) && !AuthSequenceUtil.isLoginPage(request);
    }

    private boolean isRootPage(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return "".equals(servletPath) || "/".equals(servletPath);
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

    private int restartAuthFlow(
            HttpServletRequest httpRequest, AuthenticationWrapper authWrapper, HttpServletResponse httpResponse) {
        createMpAuthentication(httpRequest, authWrapper);
        MidpointAuthentication mpAuthentication = AuthUtil.getMidpointAuthentication();
        if (!AuthSequenceUtil.isRecordSessionLessAccessChannel(httpRequest)) {
            saveAuthenticationContext(httpRequest, httpResponse);
        }
        return mpAuthentication.resolveParallelModules(httpRequest, 0);
    }

    private void saveAuthenticationContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextRepository contextRepository =
                (SecurityContextRepository) sharedObjects.get(SecurityContextRepository.class);
        contextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);
    }

    private void createMpAuthentication(HttpServletRequest httpRequest, AuthenticationWrapper authWrapper) {
        authWrapper.buildMidPointAuthentication(httpRequest);
    }

    private void resetRetryableModuleIfNeeded(MidpointAuthentication mpAuthentication) {
        if (mpAuthentication != null) {
            mpAuthentication.resetLastFailedModuleForRetry();
        }
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

    /**
     * Resets the authentication flow when a focus-identification or archetype-selection form is submitted
     * for an existing authentication flow. The flow is restarted from the submitted module; the state of the
     * modules executed before it (e.g. an already selected archetype) is kept.
     *
     * This prevents state from another authentication attempt in the same HTTP session from being reused
     * for the current identification request.
     *
     * @return index of the module that has to process the request after the reset,
     *         {@link MidpointAuthentication#NO_MODULE_FOUND_INDEX} when the request is no such submit
     */
    @VisibleForTesting
    int resetToIdentificationModuleIfRequested(
            MidpointAuthentication mpAuthentication,
            List<AuthModule<?>> authModules,
            AuthenticationSequenceType sequence,
            HttpServletRequest request) {
        if (mpAuthentication == null || !"POST".equals(request.getMethod())
                || authModules.isEmpty() || sequence.getModule().isEmpty()) {
            return MidpointAuthentication.NO_MODULE_FOUND_INDEX;
        }

        int submittedModuleIndex = indexOfSubmittedIdentificationModule(authModules, request);
        if (submittedModuleIndex == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            return MidpointAuthentication.NO_MODULE_FOUND_INDEX;
        }

        restartFlowFromModule(mpAuthentication, authModules, sequence, submittedModuleIndex);
        return submittedModuleIndex;
    }

    /**
     * Returns the index of the identification module (focus identification or archetype selection)
     * whose login page is the target of the request.
     */
    private int indexOfSubmittedIdentificationModule(List<AuthModule<?>> authModules, HttpServletRequest request) {
        for (int i = 0; i < authModules.size(); i++) {
            ModuleAuthentication moduleAuthentication = authModules.get(i).getBaseModuleAuthentication();
            if (isIdentificationModule(moduleAuthentication)
                    && isRequestForAuthenticationModuleLoginPage(request, moduleAuthentication)) {
                return i;
            }
        }
        return MidpointAuthentication.NO_MODULE_FOUND_INDEX;
    }

    /** Modules that establish the identity used for resolving the security policy of the flow. */
    private boolean isIdentificationModule(ModuleAuthentication moduleAuthentication) {
        return moduleAuthentication instanceof FocusIdentificationModuleAuthentication
                || moduleAuthentication instanceof ArchetypeSelectionModuleAuthentication;
    }

    /**
     * Restarts the flow from the module at the given index. The flow keeps the given module and the ones
     * executed before it, together with their processed state and the archetype selected by them.
     */
    private void restartFlowFromModule(
            MidpointAuthentication mpAuthentication,
            List<AuthModule<?>> authModules,
            AuthenticationSequenceType sequence,
            int moduleIndex) {
        List<AuthModule<?>> keptModules = List.copyOf(authModules.subList(0, moduleIndex + 1));
        List<ModuleAuthentication> keptProcessedModules =
                processedStateOfModules(mpAuthentication, keptModules.subList(0, moduleIndex));
        String archetypeOid = mpAuthentication.getArchetypeOid();

        mpAuthentication.restart();
        mpAuthentication.setSequence(AuthSequenceUtil.sequenceWithFirstExecutedModulesOnly(sequence, moduleIndex + 1));
        mpAuthentication.setAuthModules(keptModules);
        keptProcessedModules.forEach(mpAuthentication::addAuthentication);
        mpAuthentication.addAuthentication(keptModules.get(moduleIndex).getBaseModuleAuthentication());

        if (containsSuccessfulArchetypeSelection(keptProcessedModules)) {
            mpAuthentication.setArchetypeOid(archetypeOid);
            mpAuthentication.setArchetypeSelected(true);
        }
    }

    /** Returns the processed state of the given modules, i.e. of the ones executed before the reset point. */
    private List<ModuleAuthentication> processedStateOfModules(
            MidpointAuthentication mpAuthentication, List<AuthModule<?>> modules) {
        List<ModuleAuthentication> processedState = new ArrayList<>();
        for (AuthModule<?> module : modules) {
            mpAuthentication.getAuthentications().stream()
                    .filter(processed -> Objects.equals(processed.getModuleIdentifier(), module.getModuleIdentifier()))
                    .findFirst()
                    .ifPresent(processedState::add);
        }
        return processedState;
    }

    private boolean containsSuccessfulArchetypeSelection(List<ModuleAuthentication> processedModules) {
        return processedModules.stream()
                .anyMatch(module -> module instanceof ArchetypeSelectionModuleAuthentication
                        && AuthenticationModuleState.SUCCESSFULLY == module.getState());
    }

    /**
     * Checks whether the request targets a login page configured for the given authentication module.
     */
    private boolean isRequestForAuthenticationModuleLoginPage(HttpServletRequest request, ModuleAuthentication moduleAuthentication) {
        List<String> pageUrls = DescriptorLoaderImpl.getPageUrlsByAuthName(moduleAuthentication.getModuleTypeName());
        return pageUrls != null && pageUrls.stream()
                .map(MidpointRequestMatchers::pathMatcher)
                .anyMatch(matcher -> matcher.matches(request));
    }

    private void processingOfAuthenticatedRequest(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(AuthSequenceUtil.isUrlForAuthProcessing(httpRequest)) {
            new DefaultRedirectStrategy().sendRedirect(httpRequest, (HttpServletResponse) response, "/");
            return;
        }

        int i = 1;
        for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
            if (AuthenticationModuleState.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                i = mpAuthentication.getIndexOfModule(moduleAuthentication);
            }
        }

        VirtualFilterChain vfc = new VirtualFilterChain(
                chain, mpAuthentication.getAuthModules().get(i).getSecurityFilterChain().getFilters());
        vfc.doFilter(httpRequest, response);
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
}

