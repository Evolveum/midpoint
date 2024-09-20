/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.RemoveUnusedSecurityFilterPublisher;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.MidpointProviderManager;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.authentication.impl.util.AuthenticationSequenceModuleCreator;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.schema.util.SecurityPolicyUtil.NO_CUSTOM_IGNORED_LOCAL_PATH;

class AuthenticationWrapper {
    private static final Trace LOGGER = TraceManager.getTrace(AuthenticationWrapper.class);

    private AuthenticationsPolicyType authenticationsPolicy;
    private CredentialsPolicyType credentialsPolicy = null;
    private PrismObject<SecurityPolicyType> securityPolicy = null;
    private List<AuthModule<?>> authModules;
    private AuthenticationSequenceType sequence = null;
    private AuthenticationChannel authenticationChannel;

    private final MidpointProviderManager authenticationManager;
    private final AuthModuleRegistryImpl authModuleRegistry;
    private final Map<Class<?>, Object> sharedObjects;
    private final RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher;

    private final ModelInteractionService modelInteractionService;
    private final SystemObjectCache systemObjectCache;

    private final Map<String, List<AuthModule<?>>> authModulesOfSpecificSequences = new HashMap<>();
    private volatile AuthenticationsPolicyType defaultAuthenticationPolicy;

    AuthenticationWrapper(
            MidpointProviderManager authenticationManager,
            AuthModuleRegistryImpl authModuleRegistry,
            Map<Class<?>, Object> sharedObjects,
            RemoveUnusedSecurityFilterPublisher removeUnusedSecurityFilterPublisher,
            SystemObjectCache systemObjectCache,
            ModelInteractionService modelInteractionService) {
        this.authenticationManager = authenticationManager;
        this.authModuleRegistry = authModuleRegistry;
        this.sharedObjects = sharedObjects;
        this.removeUnusedSecurityFilterPublisher = removeUnusedSecurityFilterPublisher;
        this.systemObjectCache = systemObjectCache;
        this.modelInteractionService = modelInteractionService;
    }

    public AuthenticationWrapper create(
            MidpointAuthentication mpAuthentication,
            HttpServletRequest httpRequest,
            TaskManager taskManager,
            AuthChannelRegistryImpl authChannelRegistry) {
        resolvePolicies(mpAuthentication, taskManager);
        initializeAuthenticationSequence(mpAuthentication, httpRequest, taskManager);
        initializeAuthenticationChannel(authChannelRegistry);
        initAuthenticationModule(mpAuthentication, httpRequest);
        return this;
    }

    private void resolvePolicies(MidpointAuthentication mpAuthentication, TaskManager taskManager) {
        try {
            securityPolicy = resolveSecurityPolicy(mpAuthentication, taskManager);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load security policy", e);
        }
        try {
            authenticationsPolicy = getAuthenticationPolicy(securityPolicy);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get default authentication policy");
            throw new IllegalArgumentException("Couldn't get default authentication policy", e);
        }

        this.credentialsPolicy = securityPolicy != null ? securityPolicy.asObjectable().getCredentials() : null;

    }

    private PrismObject<SecurityPolicyType> resolveSecurityPolicy(MidpointAuthentication mpAuthentication, TaskManager taskManager) throws SchemaException {
        SecurityPolicyType securityPolicyType = null;
        if (mpAuthentication != null) {
            securityPolicyType = mpAuthentication.resolveSecurityPolicyForPrincipal();
            if (securityPolicyType == null && mpAuthentication.isArchetypeDefined()) {
                securityPolicyType = loadSecurityPolicyForArchetype(mpAuthentication.getArchetypeOid(), taskManager);
            }
        }
        return securityPolicyType == null ? getGlobalSecurityPolicy() : securityPolicyType.asPrismObject();
    }

    private SecurityPolicyType loadSecurityPolicyForArchetype(String archetypeOid, TaskManager taskManager) {
        try {
            var operation = "loadSecurityPolicyForArchetype";
            Task task = taskManager.createTaskInstance(operation);
            OperationResult result = new OperationResult(operation);
            return modelInteractionService.getSecurityPolicy(null, archetypeOid, task, result);
        } catch (Exception ex) {
            LOGGER.debug("Couldn't load security policy for archetype");
        }
        return null;
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


    private PrismObject<SecurityPolicyType> getGlobalSecurityPolicy() throws SchemaException {
        return systemObjectCache.getSecurityPolicy();
    }

    public String getSequenceIdentifier() {
        return AuthSequenceUtil.getAuthSequenceIdentifier(sequence);
    }

    public boolean isIgnoredLocalPath(HttpServletRequest httpRequest) {
        if (authenticationsPolicy == null || authenticationsPolicy.getIgnoredLocalPath().isEmpty()) {
            return false;
        }
        List<String> ignoredPaths = authenticationsPolicy.getIgnoredLocalPath();
        for (String ignoredPath : ignoredPaths) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(ignoredPath);
            if (matcher.matches(httpRequest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * initializing sequence based on the current authentication request. even when the sequence is already preset
     * in the midPoint authentication, we will try check current authentication policy and find the sequence to
     * have the most fresh sequence. E.g. at the beginning when the midPoint authentication represents
     * anonymous token, we only have global sequence. But after (usually first step such as focusIdentifier
     * or archetype selection) we can obtain more specific sequence based on those data.
     */
    private void initializeAuthenticationSequence(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest, TaskManager taskManager) {
        if (mpAuthentication != null && AuthSequenceUtil.isLoginPage(httpRequest)) {
            if (mpAuthentication.getAuthenticationChannel() != null && !mpAuthentication.getAuthenticationChannel()
                    .getChannelId().equals(AuthSequenceUtil.findChannelByRequest(httpRequest))
                    && AuthSequenceUtil.getSequenceByPath(httpRequest, authenticationsPolicy, taskManager.getLocalNodeGroups()) == null) {
                return;
            }
            String sequenceIdentifier = mpAuthentication.getSequenceIdentifier();
            if (StringUtils.isNotBlank(sequenceIdentifier)) {
                this.sequence = authenticationsPolicy.getSequence().stream().filter(sequenceType -> sequenceIdentifier.equals(sequenceType.getIdentifier())).findFirst().orElse(null);
            }
            if (sequence == null) {
                this.sequence = mpAuthentication.getSequence();
            }
        } else {
            this.sequence = AuthSequenceUtil.getSequenceByPath(httpRequest, authenticationsPolicy, taskManager.getLocalNodeGroups());
        }

        if (sequence != null && isEqualChannelIdForAuthenticatedUser(mpAuthentication, httpRequest)) {
            changeLogoutToNewSequence(mpAuthentication, httpRequest);
            this.sequence = mpAuthentication.getSequence();
        }

    }

    private void initializeAuthenticationChannel(AuthChannelRegistryImpl authChannelRegistry) {
        this.authenticationChannel = AuthSequenceUtil.buildAuthChannel(authChannelRegistry, sequence);
    }

    private boolean isEqualChannelIdForAuthenticatedUser(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        return mpAuthentication != null
                && !sequenceIdentifiersMatch(mpAuthentication.getSequence(), sequence)
                && mpAuthentication.isAuthenticated()
                && (((sequence != null
                    && sequence.getChannel() != null
                    && mpAuthentication.getAuthenticationChannel().matchChannel(sequence)))
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

    public boolean sequenceIdentifiersMatch(AuthenticationSequenceType seq) {
        return sequenceIdentifiersMatch(sequence, seq);
    }

    private boolean sequenceIdentifiersMatch(AuthenticationSequenceType seq1, AuthenticationSequenceType seq2) {
        String seqIdentifier1 = AuthSequenceUtil.getAuthSequenceIdentifier(seq1);
        String seqIdentifier2 = AuthSequenceUtil.getAuthSequenceIdentifier(seq2);
        return seqIdentifier1 != null && StringUtils.equals(seqIdentifier1, seqIdentifier2);
    }

    private void initAuthenticationModule(MidpointAuthentication mpAuthentication, HttpServletRequest httpRequest) {
        if (!AuthSequenceUtil.isClusterSequence(httpRequest)) {
            this.authModules = createAuthenticationModuleBySequence(mpAuthentication, httpRequest);
            return;
        }

        if (authModulesOfSpecificSequences.containsKey(this.getSequenceIdentifier())) {
            this.authModules = authModulesOfSpecificSequences.get(this.getSequenceIdentifier());
            if (this.authModules != null) {
                for (AuthModule authModule : this.authModules) {
                    List<AuthenticationProvider> authenticationProviders = authModule.getAuthenticationProviders();
                    if (authenticationProviders != null) {
                        authenticationManager.getProviders().clear();
                        authenticationManager.getProviders().addAll(authenticationProviders);
                    }
                }
            }
        } else {
            this.authModules = createAuthenticationModuleBySequence(mpAuthentication,  httpRequest);
            authModulesOfSpecificSequences.put(this.getSequenceIdentifier(), this.authModules);
        }
    }

    private List<AuthModule<?>> createAuthenticationModuleBySequence(MidpointAuthentication mpAuthentication,
            HttpServletRequest httpRequest) {
        List<AuthModule<?>> authModules;
        boolean processingDifferentSequence = processingDifferentAuthenticationSequence(mpAuthentication, this.sequence);
        if (processingDifferentSequence || sequence.getModule().size() != mpAuthentication.getSequence().getModule().size()
                || StringUtils.isNotEmpty(mpAuthentication.getArchetypeOid())) {
            authenticationManager.getProviders().clear();
            //noinspection unchecked
            authModules = new AuthenticationSequenceModuleCreator<>(
                    authModuleRegistry,
                    this.sequence,
                    httpRequest,
                    this.authenticationsPolicy.getModules(),
                    this.authenticationChannel)
                    .credentialsPolicy(this.credentialsPolicy)
                    .sharedObjects(sharedObjects)
                    .create();
            if (processingDifferentSequence) {
                clearAuthentication(httpRequest);
            }
            updateMidpointAuthenticationModules(this.sequence, authModules, mpAuthentication);
        } else {
            authModules = mpAuthentication.getAuthModules();
        }
        return authModules;
    }


    private boolean processingDifferentAuthenticationSequence(MidpointAuthentication mpAuthentication, AuthenticationSequenceType sequence) {
        return mpAuthentication == null || !sequenceIdentifiersMatch(sequence, mpAuthentication.getSequence());
    }

    private void updateMidpointAuthenticationModules(AuthenticationSequenceType sequence, List<AuthModule<?>> authModules, MidpointAuthentication mpAuthentication) {
        if (mpAuthentication == null) {
            return;
        }
        mpAuthentication.setAuthModules(authModules);
        mpAuthentication.setSequence(sequence);
    }

    private void clearAuthentication(HttpServletRequest httpRequest) {
        Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (!AuthSequenceUtil.isClusterSequence(httpRequest) && oldAuthentication instanceof MidpointAuthentication) {
            removeUnusedSecurityFilterPublisher.publishCustomEvent(
                    ((MidpointAuthentication) oldAuthentication).getAuthModules());
        }
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public PrismObject<SecurityPolicyType> getSecurityPolicy() {
        return securityPolicy;
    }

    public AuthenticationSequenceType getSequence() {
        return sequence;
    }

    public List<AuthModule<?>> getAuthModules() {
        return authModules;
    }

    public AuthenticationChannel getAuthenticationChannel() {
        return authenticationChannel;
    }

    public void buildMidPointAuthentication(HttpServletRequest httpRequest) {
        MidpointAuthentication mpAuthentication = new MidpointAuthentication(sequence);
        mpAuthentication.setSharedObjects(sharedObjects);
        mpAuthentication.setAuthModules(authModules);
        mpAuthentication.setAuthenticationChannel(authenticationChannel);
        mpAuthentication.setSessionId(httpRequest.getSession(false) != null ?
                httpRequest.getSession(false).getId() : RandomStringUtils.random(30, true, true).toUpperCase());
        mpAuthentication.addAuthentications(authModules.get(0).getBaseModuleAuthentication());
        clearAuthentication(httpRequest);
        SecurityContextHolder.getContext().setAuthentication(mpAuthentication);
    }

}
