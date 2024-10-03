/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import java.util.*;
import java.util.stream.Stream;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import com.evolveum.midpoint.authentication.api.*;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.util.AuthenticationSequenceTypeUtil;
import com.evolveum.midpoint.security.api.AuthenticationAnonymousChecker;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

/**
 * Authentication token on top of hierarchy.
 * Wrapper for all authentication modules.
 * Contains method and variables for finding out the current status of authentication.
 * Also contains method and variables for easy recovery of authentication flow.
 *
 * @author skublik
 */

public class MidpointAuthentication extends AbstractAuthenticationToken implements AuthenticationAnonymousChecker {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointAuthentication.class);

    /**
     * Configuration of sequence from xml
     */
    private AuthenticationSequenceType sequence;
    private Map<Class<?>, Object> sharedObjects;    //todo may be wrong place


    /**
     * Authentications for modules of sequence
     */
    private final List<ModuleAuthentication> authentications = new ArrayList<>();

    /**
     * Channel defining scope of authentication, eg. rest, gui, reset password ...
     */
    private AuthenticationChannel authenticationChannel;

    /**
     * Authentication module created basic on configuration of module
     */
    private List<AuthModule<?>> authModules = new ArrayList<>();

    private Object principal;
    private Object credential;
    private String sessionId;
    private Collection<? extends GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;

    public static final int NO_PROCESSING_MODULE_INDEX = -2;
    public static final int NO_MODULE_FOUND_INDEX = -1;
    private boolean overLockoutMaxAttempts = false;

    /**
     * Indicates if the overall state of the authentication was already recorded.
     * It should be recorded only for whole sequence and after the whole sequence
     * was reliably evaluated. E.g. all modules run and authentication failed, or
     * authentication was successful.
     */
    private boolean alreadyAudited;

    /**
     * Usable for some types of authentication modules (e.g. for archetypeSelection module).
     * Archetype object provides a reference to object template which can be used
     * for getting the correlators (please, see more information about "lost my username"
     * functionality).
     */
    private String archetypeOid;
    private boolean archetypeSelected;

    /**
     * Indicates if the profile of midpoint principal was compiled after successful authentication.
     * It should be recorded only for whole sequence and after the whole sequence
     * was reliably evaluated. E.g. all modules run and authentication was successful.
     */
    private boolean alreadyCompiledGui;

    public MidpointAuthentication(AuthenticationSequenceType sequence) {
        super(null);
        this.sequence = sequence;
    }

    public List<AuthModule<?>> getAuthModules() {
        return authModules;
    }

    public void setAuthModules(List<AuthModule<?>> authModules) {
        if (!this.authModules.isEmpty()) {
            List<AuthModule<?>> modules = new ArrayList<>(this.authModules);
            RemoveUnusedSecurityFilterPublisher.get().publishCustomEvent(modules);
        }
        this.authModules = authModules;
    }

    public AuthenticationSequenceType getSequence() {
        return sequence;
    }

    public String getSequenceIdentifier() {
        return AuthenticationSequenceTypeUtil.getSequenceIdentifier(sequence);
    }

    public void setSequence(AuthenticationSequenceType sequence) {
        this.sequence = sequence;
    }

    public Map<Class<?>, Object> getSharedObjects() {
        return sharedObjects;
    }

    public void setSharedObjects(Map<Class<?>, Object> sharedObjects) {
        this.sharedObjects = sharedObjects;
    }

    public AuthenticationChannel getAuthenticationChannel() {
        return authenticationChannel;
    }

    public void setAuthenticationChannel(AuthenticationChannel authenticationChannel) {
        this.authenticationChannel = authenticationChannel;
    }

    public List<ModuleAuthentication> getAuthentications() {
        return authentications;
    }

    public void addAuthentications(ModuleAuthentication authentication) {
        getAuthentications().add(authentication);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        //noinspection unchecked
        return (Collection<GrantedAuthority>) authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public Object getCredentials() {
        return credential;
    }

    public void setCredential(Object credential) {
        this.credential = credential;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public void setPrincipal(Object principal) {
        this.principal = principal;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        throw new IllegalArgumentException("This method is not supported");
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public boolean isAuthenticated() {
        List<AuthenticationSequenceModuleType> modules = sequence.getModule();
        if (modules.isEmpty() && !AuthUtil.isClusterAuthentication(MidpointAuthentication.this)) {
                return false;
        }

        if (shouldEvaluateAuthentication()) {
            return allProcessedModulesWithNecessityAreSuccessful() && wasSuccessfullyProcessedOtherThanUnsufficientModules();
        }
        return allAuthenticationModulesExist()
                && wasSuccessfullyProcessedOtherThanUnsufficientModules()
                && (allModulesAreSuccessful() || getAuthenticationModuleNecessityDecision());
    }

    private boolean wasSuccessfullyProcessedOtherThanUnsufficientModules() {
        long sufficientModuleCount = getAuthentications().stream()
                .filter(module -> AuthenticationModuleState.SUCCESSFULLY == module.getState())
                .filter(ModuleAuthentication::isSufficient)
                .count();
        return sufficientModuleCount > 0;
    }

    private boolean shouldEvaluateAuthentication() {
        boolean successfulSufficientModuleFound = false;
        List<ModuleAuthentication> modules = getAuthentications();
        for (ModuleAuthentication module : modules) {
            if (module.getNecessity() == AuthenticationSequenceModuleNecessityType.SUFFICIENT && module.getState() == AuthenticationModuleState.SUCCESSFULLY) {
                successfulSufficientModuleFound = true;
                break;
            }
        }
        return successfulSufficientModuleFound;
    }

    private boolean allAuthenticationModulesExist() {
        return sequence.getModule()
                .stream()
                .allMatch(m -> getAuthenticationByIdentifier(m) != null);
    }

    private boolean getAuthenticationModuleNecessityDecision() {
        return allModulesAreProcessed()
                && ((allModulesNecessitySufficientOrOptional() && atLeastOneSuccessfulModuleExists())
                     || (getRequisiteModulesDecision() && getRequiredModulesDecision() && lasModuleIsSufficientAndSuccessful()));
    }

    private boolean lasModuleIsSufficientAndSuccessful() {
        List<ModuleAuthentication> module = getAuthentications();
        ModuleAuthentication lastModule = module.get(module.size() - 1);
        if (AuthenticationSequenceModuleNecessityType.SUFFICIENT == lastModule.getNecessity()) {
            return lastModule.getState() == AuthenticationModuleState.SUCCESSFULLY;
        }
        return true;
    }

    private boolean allModulesNecessitySufficientOrOptional() {
        return sequence.getModule()
                .stream()
                .allMatch(m -> Stream.of(AuthenticationSequenceModuleNecessityType.SUFFICIENT, AuthenticationSequenceModuleNecessityType.OPTIONAL)
                        .anyMatch(n -> n.equals(m.getNecessity())));
    }

    private boolean allModulesAreProcessed() {
        return allModulesStateMatch(
                AuthenticationModuleState.SUCCESSFULLY,
                AuthenticationModuleState.FAILURE,
                AuthenticationModuleState.CALLED_OFF,
                AuthenticationModuleState.FAILURE_CONFIGURATION);
    }

    private boolean atLeastOneSuccessfulModuleExists() {
        return sequence.getModule()
                .stream()
                .anyMatch(m -> getAuthenticationByIdentifier(m) != null &&
                        AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }

    private boolean allModulesStateMatch(AuthenticationModuleState... states) {
        return sequence.getModule()
                .stream()
                .allMatch(m -> Arrays.stream(states).anyMatch(s ->
                        getAuthenticationByIdentifier(m) != null && s.equals(getAuthenticationByIdentifier(m).getState())));
    }

    private boolean getRequiredModulesDecision() {
        return sequence.getModule()
                        .stream()
                                .noneMatch(m -> AuthenticationSequenceModuleNecessityType.REQUIRED.equals(m.getNecessity()))
                || allRequiredModulesAreSuccessful();
    }

    private boolean getRequisiteModulesDecision() {
        return sequence.getModule()
                        .stream()
                                .noneMatch(m -> AuthenticationSequenceModuleNecessityType.REQUISITE.equals(m.getNecessity()))
                || allRequisiteModulesAreSuccessful();
    }

    private boolean allModulesAreSuccessful() {
        return sequence.getModule()
                .stream()
                .allMatch(m -> AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }

    private boolean allProcessedModulesWithNecessityAreSuccessful() {
        List<ModuleAuthentication> processedModules = getAuthentications();
        List<ModuleAuthentication> requiredAndRequisiteModules = processedModules.stream()
                .filter(this::isRequiredOrRequisiteOrSufficient)
                .toList();

        long notSuccessfulRequiredAndRequisiteModules = requiredAndRequisiteModules.stream()
                .filter(requiredAndRequisite -> requiredAndRequisite.getState() != AuthenticationModuleState.SUCCESSFULLY)
                .count();
        return notSuccessfulRequiredAndRequisiteModules == 0;
    }

    private boolean isRequiredOrRequisiteOrSufficient(ModuleAuthentication processedModule) {
        return isNecessity(AuthenticationSequenceModuleNecessityType.REQUIRED, processedModule) ||
                isNecessity(AuthenticationSequenceModuleNecessityType.REQUISITE, processedModule);
    }
    private boolean isNecessity(AuthenticationSequenceModuleNecessityType necessity, ModuleAuthentication processedModule) {
         return necessity == processedModule.getNecessity();
    }
    private boolean allRequisiteModulesAreSuccessful() {
        return !nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType.REQUISITE);
    }

    private boolean allRequiredModulesAreSuccessful() {
        return !nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType.REQUIRED);
    }

    private boolean nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType moduleNecessity) {
        List<AuthenticationSequenceModuleType> modules = sequence.getModule();
        return modules.stream()
                .anyMatch(m -> moduleNecessity.equals(m.getNecessity())
                        && (getAuthenticationByIdentifier(m) == null
                        || !AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState())));
    }

    private ModuleAuthentication getAuthenticationByIdentifier(AuthenticationSequenceModuleType module) {
        if (module == null) {
            return null;
        }
        String moduleIdentifier = StringUtils.isNotEmpty(module.getIdentifier()) ? module.getIdentifier() : module.getName();
        for (ModuleAuthentication authentication : getAuthentications()) {
            if (authentication.getModuleIdentifier().equals(moduleIdentifier)) {
                return authentication;
            }
        }
        return null;
    }

    public int getIndexOfProcessingModule(boolean createEmptyAuthenticationIfNeeded) {
        if (getAuthentications().isEmpty()) {
            if (createEmptyAuthenticationIfNeeded) {
                addAuthentications(getAuthModules().get(0).getBaseModuleAuthentication());
            }
            return 0;
        }
        ModuleAuthentication authentication = getProcessingModuleAuthentication();
        if (authentication != null) {
            return getIndexOfModule(authentication);
        }
        int actualSize = getAuthentications().size();
        int endSize = getAuthModules().size();
        if (actualSize < endSize) {
            if (createEmptyAuthenticationIfNeeded) {
                addAuthentications(getAuthModules().get(actualSize).getBaseModuleAuthentication());
            }
            return actualSize;
        }
        if (allModulesAreAuthenticated()) {
            return NO_PROCESSING_MODULE_INDEX;
        }
        return NO_MODULE_FOUND_INDEX;
    }

    private boolean allModulesAreAuthenticated() {
        int actualSize = getAuthentications().size();
        int endSize = getAuthModules().size();
        return actualSize == endSize && isAuthenticated();
    }

    public boolean isFinished() {
        int actualSize = getAuthentications().size();
        int endSize = getAuthModules().size();
        return successfulProcessedSufficientModule() || processingModuleIsRequisiteAndFailure() || (actualSize == endSize && allModulesAreProcessed());
    }

    private boolean successfulProcessedSufficientModule() {
        boolean successOneSufficientModule = false;

        for (AuthenticationSequenceModuleType module : getSequence().getModule()) {
            if (AuthenticationSequenceModuleNecessityType.REQUISITE == module.getNecessity()
                    || AuthenticationSequenceModuleNecessityType.OPTIONAL == module.getNecessity()) {
                continue;
            }
            if (AuthenticationSequenceModuleNecessityType.REQUIRED == module.getNecessity()) {
                ModuleAuthentication authentication = getAuthenticationByIdentifier(module);
                if (authentication == null
                        || AuthenticationModuleState.SUCCESSFULLY != authentication.getState()) {
                    return false;
                }
            }
            if (AuthenticationSequenceModuleNecessityType.SUFFICIENT == module.getNecessity()) {
                ModuleAuthentication authentication = getAuthenticationByIdentifier(module);
                if (authentication == null) {
                    continue;
                }
                if (AuthenticationModuleState.SUCCESSFULLY == authentication.getState()) {
                    successOneSufficientModule = true;
                }
            }
        }

        return successOneSufficientModule;
    }

    private boolean processingModuleIsRequisiteAndFailure() {
        List<ModuleAuthentication> moduleAuthentications = getAuthentications();
        for (ModuleAuthentication moduleAuthentication : moduleAuthentications) {
            if (AuthenticationSequenceModuleNecessityType.REQUISITE == moduleAuthentication.getNecessity()
                    && AuthenticationModuleState.FAILURE == moduleAuthentication.getState()) {
                return true;
            }
        }
        return false;
    }

    public int getIndexOfModule(ModuleAuthentication authentication) {
        Validate.notNull(authentication);

        for (int i = 0; i < getAuthModules().size(); i++) {
            AuthModule module = getAuthModules().get(i);
            if (module.getModuleIdentifier().equals(authentication.getModuleIdentifier())) {
                return i;
            }
        }
        return NO_MODULE_FOUND_INDEX;
    }

    public ModuleAuthentication getProcessingModuleAuthentication() {
        for (ModuleAuthentication authentication : getAuthentications()) {
            if (authentication.getState().equals(AuthenticationModuleState.LOGIN_PROCESSING)
                    || authentication.getState().equals(AuthenticationModuleState.LOGOUT_PROCESSING)) {
                if (authentication.applicable()) {
                    return authentication;
                } else {
                    authentication.setState(AuthenticationModuleState.CALLED_OFF);
                }
            }
        }
        return null;
    }

    public String getProcessingModuleAuthenticationIdentifier() {
        ModuleAuthentication moduleAuthentication = getProcessingModuleAuthentication();
        return moduleAuthentication == null ? null : moduleAuthentication.getModuleIdentifier();
    }

    public boolean isAuthenticationFailed() {
        return !isAuthenticated()
                && getProcessingModuleAuthentication() == null
                && getAuthentications().size() == getAuthModules().size();
    }

    public boolean isOverLockoutMaxAttempts() {
        return overLockoutMaxAttempts;
    }

    public void setOverLockoutMaxAttempts(boolean overLockoutMaxAttempts) {
        this.overLockoutMaxAttempts = overLockoutMaxAttempts;
    }

    @Override
    public String getName() {
        if (getPrincipal() instanceof MidPointPrincipal) {
            return ((MidPointPrincipal) getPrincipal()).getUsername();
        }
        return "";
    }

    public String getUsername() {
        if (getPrincipal() instanceof MidPointPrincipal) {
            return ((MidPointPrincipal) getPrincipal()).getUsername();
        }
        ModuleAuthentication moduleAuthentication = getFirstFailedAuthenticationModule();
        if (moduleAuthentication != null) {
            AutheticationFailedData failureData = moduleAuthentication.getFailureData();
            if (failureData != null) {
                return failureData.getUsername();
            }
        }
        return "";
    }

    public String getFailedReason() {
        ModuleAuthentication moduleAuthentication = getFirstFailedAuthenticationModule();
        if (moduleAuthentication != null) {
            AutheticationFailedData failureData = moduleAuthentication.getFailureData();
            if (failureData != null) {
                return failureData.getFailureMessage();
            }
        }
        return "";
    }

    public ModuleAuthentication getFirstFailedAuthenticationModule() {
        List<ModuleAuthentication> moduleAuthentications = getAuthentications();
        ModuleAuthentication found = null;
        for (ModuleAuthentication moduleAuthentication : moduleAuthentications) {
            if (AuthenticationModuleState.FAILURE == moduleAuthentication.getState() && found == null) {
                found = moduleAuthentication;
                if (found.getFailureData() != null) {
                    return found;
                }
                continue;
            }
            if (AuthenticationModuleState.FAILURE == moduleAuthentication.getState()
                    && Objects.equals(found.getOrder(), moduleAuthentication.getOrder())
                    && moduleAuthentication.getFailureData() != null) {
                return moduleAuthentication;
            }
        }
        return found;
    }

    public AuthenticationException getAuthenticationExceptionIfExists() {
        ModuleAuthentication moduleAuthentication = getFirstFailedAuthenticationModule();
        if (moduleAuthentication == null) {
            return null;
        }
        AutheticationFailedData failedData = moduleAuthentication.getFailureData();
        if (failedData == null) {
            return null;
        }
        return failedData.getAuthenticationException();
    }

    public List<ModuleAuthentication> getParallelProcessingModules() {
        int indexOfProcessingModule = getIndexOfProcessingModule(false);
        if (indexOfProcessingModule == NO_PROCESSING_MODULE_INDEX) {
            return new ArrayList<>();
        }
        return getParallelProcessingModules(indexOfProcessingModule);
    }

    private List<ModuleAuthentication> getParallelProcessingModules(int actualIndex) {
        List<ModuleAuthentication> parallelProcessingModules = new ArrayList<>();
        ModuleAuthentication authentication = getAuthentications().get(actualIndex);
        if (authentication == null) {
            return parallelProcessingModules;
        }

        if (actualIndex > 0) {
            for (int i = actualIndex - 1; i >= 0; i--) {
                if (getAuthModules().get(i) != null
                        && authentication.getOrder().equals(getAuthModules().get(i).getOrder())) {
                    parallelProcessingModules.add(getAuthModules().get(i).getBaseModuleAuthentication());
                } else {
                    break;
                }
            }
        }
        parallelProcessingModules.add(authentication);
        for (int i = actualIndex + 1; i < getAuthModules().size(); i++) {
            if (getAuthModules().get(i) != null
                    && authentication.getOrder().equals(getAuthModules().get(i).getOrder())) {
                parallelProcessingModules.add(getAuthModules().get(i).getBaseModuleAuthentication());
            }
        }

        return parallelProcessingModules;
    }

    public int resolveParallelModules(HttpServletRequest request, int actualIndex) {
        if (actualIndex < 0) {
            return actualIndex;
        }

        if (getAuthenticationChannel() != null
                && !SecurityUtil.isRestAndActuatorChannel(getAuthenticationChannel().getChannelId())) {
            return actualIndex;
        }

        String header = request.getHeader("Authorization");
        if (header == null) {
            return actualIndex;
        }

        String type = header.split(" ")[0];
        List<ModuleAuthentication> parallelProcessingModules = getParallelProcessingModules(actualIndex);
        int resolvedIndex = NO_MODULE_FOUND_INDEX;
        for (ModuleAuthentication parallelProcessingModule : parallelProcessingModules) {
            int usedIndex = getAuthentications().indexOf(parallelProcessingModule);
            if (AuthUtil.resolveTokenTypeByModuleType(parallelProcessingModule.getModuleTypeName()).equalsIgnoreCase(type)
                    && resolvedIndex == NO_MODULE_FOUND_INDEX) {
                parallelProcessingModule.setState(AuthenticationModuleState.LOGIN_PROCESSING);
                if (usedIndex != NO_MODULE_FOUND_INDEX) {
                    resolvedIndex = usedIndex;
                } else {
                    resolvedIndex = getAuthentications().size();
                }
            } else {
                parallelProcessingModule.setState(AuthenticationModuleState.FAILURE);
            }
            if (usedIndex == NO_MODULE_FOUND_INDEX) {
                getAuthentications().add(parallelProcessingModule);
            } else {
                getAuthentications().set(usedIndex, parallelProcessingModule);
            }
        }
        if (resolvedIndex == NO_MODULE_FOUND_INDEX) {
            throw new IllegalArgumentException("Couldn't find module with type '" + type + "' in sequence '"
                    + getSequenceIdentifier() + "'");
        }
        return resolvedIndex;
    }

    public boolean isLast(ModuleAuthentication moduleAuthentication) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicyForPrincipal();
        if (securityPolicy != null) {
            int currentModulesSize = authModules.size();
            AuthenticationSequenceType seq = SecurityPolicyUtil.findSequenceByIdentifier(securityPolicy, sequence.getIdentifier());
            if (seq != null && currentModulesSize != seq.getModule().size()) {
                return false;
            }
        }
        if (getAuthentications().isEmpty()) {
            return false;
        }
        int index = getIndexOfModule(moduleAuthentication);
        if (index == NO_MODULE_FOUND_INDEX) {
            return false;
        }
        if (index == getAuthModules().size() - 1) {
            return true;
        }
        return moduleAuthentication.getOrder().equals(getAuthModules().get(getAuthModules().size() - 1).getOrder());
    }

    @Override
    public boolean isAnonymous() {
        List<ModuleAuthentication> moduleAuthentications = getAuthentications();
        if (moduleAuthentications == null || moduleAuthentications.size() != 1) {
            return false;
        }
        Authentication moduleAuthentication = moduleAuthentications.get(0).getAuthentication();
        return moduleAuthentication instanceof AnonymousAuthenticationToken;
    }

    public boolean hasSucceededAuthentication() {
        return getAuthentications().stream().anyMatch(auth -> AuthenticationModuleState.SUCCESSFULLY.equals(auth.getState()));
    }

    public boolean wrongConfiguredSufficientModuleExists() {
        AuthenticationSequenceModuleType sufficientModule = sequence.getModule().stream()
                .filter(m -> AuthenticationSequenceModuleNecessityType.SUFFICIENT.equals(m.getNecessity())).findFirst().orElse(null);
        return sufficientModule != null &&  !nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType.REQUIRED)
                && sequence.getModule().stream().anyMatch(m -> m.getOrder() > sufficientModule.getOrder());
    }

    public boolean authenticationShouldBeAborted() {
        return AuthenticationSequenceModuleNecessityType.REQUISITE.equals(getProcessingModuleNecessity())
                && AuthenticationModuleState.FAILURE.equals(getProcessingModuleState());
    }

    public AuthenticationSequenceModuleNecessityType getProcessingModuleNecessity() {
        ModuleAuthentication authentication = getProcessingModuleAuthentication();
        return authentication != null ? authentication.getNecessity() : null;
    }


    private AuthenticationModuleState getProcessingModuleState() {
        ModuleAuthentication authentication = getProcessingModuleAuthentication();
        return authentication != null ? authentication.getState() : null;
    }

    public SecurityPolicyType resolveSecurityPolicyForPrincipal() {
        SecurityPolicyType securityPolicy = null;
        if (principal instanceof GuiProfiledPrincipal guiProfiledPrincipal) {
            securityPolicy = guiProfiledPrincipal.getApplicableSecurityPolicy();
        }
        return securityPolicy;
    }

    public boolean isAlreadyAudited() {
        return alreadyAudited;
    }

    public void setAlreadyAudited(boolean alreadyAudited) {
        this.alreadyAudited = alreadyAudited;
    }

    public void setArchetypeOid(String archetypeOid) {
        this.archetypeOid = archetypeOid;
    }

    public String getArchetypeOid() {
        return archetypeOid;
    }

    public void setArchetypeSelected(boolean archetypeSelected) {
        this.archetypeSelected = archetypeSelected;
    }

    public boolean isArchetypeDefined() {
        return StringUtils.isNotEmpty(archetypeOid) || archetypeSelected;
    }

    public ModuleAuthentication getProcessingModuleOrThrowException() {
        ModuleAuthentication moduleAuthentication = getProcessingModuleAuthentication();
        if (moduleAuthentication == null) {
            LOGGER.debug("Couldn't find processing module authentication {}", this);    //todo temporary decision for mid-8727
            throw new AuthenticationServiceException("web.security.auth.module.null");
        }
        return moduleAuthentication;
    }

    public void setToken(Authentication token) {
        ModuleAuthentication moduleAuthentication = getProcessingModuleOrThrowException();
        moduleAuthentication.setAuthentication(token);
    }

    public boolean isAlreadyCompiledGui() {
        return alreadyCompiledGui;
    }

    public void setAlreadyCompiledGui(boolean alreadyCompiledGui) {
        this.alreadyCompiledGui = alreadyCompiledGui;
    }

    /**
     * Restart this authentication, so next request start from one module in authentication sequence.
     */
    public void restart() {
        getAuthentications().clear();
        getAuthorities().clear();
        principal = null;
        credential = null;
        alreadyAudited = false;
        overLockoutMaxAttempts = false;
        alreadyCompiledGui = false;
        archetypeSelected = false;
        archetypeOid = null;
    }
}
