/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.authentication.impl.channel.IdentityRecoveryAuthenticationChannel;
import com.evolveum.midpoint.model.api.ModelInteractionService;

import com.evolveum.midpoint.security.api.Authorization;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.configurer.ModuleWebSecurityConfigurer;

import com.evolveum.midpoint.authentication.impl.util.AuthModuleImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;

import com.evolveum.midpoint.security.api.MidPointPrincipal;

import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.config.annotation.ObjectPostProcessor;
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

        AuthenticationWrapper authWrapper = initAuthenticationWrapper(mpAuthentication, httpRequest);
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

        try {
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

    private void resolveErrorWithWrongConfigurationOfModules(
            MidpointAuthentication mpAuthentication,
            int indexOfProcessingModule,
            HttpServletRequest httpRequest,
            ServletResponse response) {
        if(mpAuthentication == null) {
            return;
        }

        if (!mpAuthentication.getAuthModules().stream()
                .anyMatch(module ->
                        AuthenticationModuleState.FAILURE_CONFIGURATION == module.getBaseModuleAuthentication().getState())) {
            return;
        }

        if (indexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            return;
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
                try {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } catch (IOException e) {
                    //ignore it end throw authentication exception
                }
                throw ex;

            }
        }
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

        int indexOfProcessingModule = getIndexOfCurrentProcessingModule(mpAuthentication, httpRequest);

        int originalIndexOfProcessingModule = indexOfProcessingModule;

        boolean restartNeeded = needCreateNewAuthenticationToken(mpAuthentication, indexOfProcessingModule, httpRequest);
        if (restartNeeded) {
            indexOfProcessingModule = initNewAuthenticationToken(authWrapper, httpRequest, (HttpServletResponse) response);
            mpAuthentication = AuthUtil.getMidpointAuthentication();
        }

        if (originalIndexOfProcessingModule == MidpointAuthentication.NO_MODULE_FOUND_INDEX) {
            originalIndexOfProcessingModule = indexOfProcessingModule;
        }

        resolveErrorWithWrongConfigurationOfModules(mpAuthentication, originalIndexOfProcessingModule, httpRequest, response);

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

    private void removingFiltersAfterProcessing(MidpointAuthentication mpAuthentication, AuthenticationWrapper authWrapper, HttpServletRequest httpRequest) {
        if (!AuthSequenceUtil.isClusterSequence(httpRequest) && httpRequest.getSession(false) == null) {
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
                chain, ((AuthModuleImpl) authWrapper.getAuthModules().get(indexOfProcessingModule)).getSecurityFilterChain().getFilters());
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
        return authWrapper.getAuthModules() == null || authWrapper.getAuthModules().size() == 0;
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

        VirtualFilterChain vfc = new VirtualFilterChain(chain,
                ((AuthModuleImpl) mpAuthentication.getAuthModules().get(i)).getSecurityFilterChain().getFilters());
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

