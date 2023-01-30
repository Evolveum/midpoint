/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.security.api.AuthenticationAnonymousChecker;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * wrapper for all authentication modules, basic authentication token
 *
 * @author skublik
 */

public class MidpointAuthentication extends AbstractAuthenticationToken implements AuthenticationAnonymousChecker {

    /**
     * Configuration of sequence from xml
     */
    private AuthenticationSequenceType sequence;
    private Map<Class<?>, Object> sharedObjects;    //todo may be wrong place


    /**
     * Authentications for modules of sequence
     */
    private final List<ModuleAuthentication> authentications = new ArrayList<>();

//    /**
//     * Configuration of modules for sequence
//     */
//    private final List<AuthenticationSequenceModuleType> modules;

    /**
     * Channel defining scope of authentication, etc. rest, gui, reset password ...
     */
    private AuthenticationChannel authenticationChannel;

    /**
     * Authentication module created basic on configuration of module
     */
    private List<AuthModule> authModules = new ArrayList<>();

    private Object principal;
    private Object credential;
    private String sessionId;
    private Collection<? extends GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;
    public static final int NO_PROCESSING_MODULE_INDEX = -2;
    public static final int NO_MODULE_FOUND_INDEX = -1;
    private boolean merged = false;


    public MidpointAuthentication(AuthenticationSequenceType sequence) {
        super(null);
//        this.modules = SecurityPolicyUtil.getSortedModules(sequence);
        this.sequence = sequence;
    }

    public List<AuthModule> getAuthModules() {
        return authModules;
    }

    public void setAuthModules(List<AuthModule> authModules) {
        this.authModules = authModules;
    }

    public AuthenticationSequenceType getSequence() {
        return sequence;
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

//    private List<AuthenticationSequenceModuleType> getModules() {
//        return Collections.unmodifiableList(modules);
//    }

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
        if (modules.isEmpty()) {
            return false;
        }
        return allAuthenticationModulesExist() && (allModulesAreSuccessful() || getAuthenticationModuleNecessityDecision());
    }

    private boolean allAuthenticationModulesExist() {
        return sequence.getModule()
                .stream()
                .allMatch(m -> getAuthenticationByIdentifier(m) != null);
    }

    private boolean getAuthenticationModuleNecessityDecision() {
        return allModulesAreProcessed()
                && (allModulesNecessityMatch(AuthenticationSequenceModuleNecessityType.OPTIONAL, AuthenticationSequenceModuleNecessityType.SUFFICIENT)
                && atLeastOneSuccessfulModuleExists()
                || getSufficientModulesDecision() && getRequisiteModulesDecision()
                && getRequiredModulesDecision() && getOptionalModulesDecision());
    }

    private boolean allModulesAreProcessed() {
        return allModulesStateMatch(AuthenticationModuleState.SUCCESSFULLY, AuthenticationModuleState.FAILURE, AuthenticationModuleState.CALLED_OFF);
    }

    private boolean allModulesNecessityMatch(AuthenticationSequenceModuleNecessityType... moduleNecessity) {
        return sequence.getModule()
                .stream()
                .allMatch(m -> Arrays.stream(moduleNecessity).anyMatch(n -> n.equals(m.getNecessity())));
    }

    private boolean atLeastOneSuccessfulModuleExists() {
        return sequence.getModule()
                .stream()
                .anyMatch(m -> AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }

    private boolean allModulesStateMatch(AuthenticationModuleState... states) {
        return sequence.getModule()
                .stream()
                .allMatch(m -> Arrays.stream(states).anyMatch(s -> s.equals(getAuthenticationByIdentifier(m).getState())));
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

    private boolean getSufficientModulesDecision() {
        return sequence.getModule()
                        .stream()
                                .noneMatch(m -> AuthenticationSequenceModuleNecessityType.SUFFICIENT.equals(m.getNecessity()))
                || sequence.getModule()
                .stream()
                .anyMatch(m -> AuthenticationSequenceModuleNecessityType.SUFFICIENT.equals(m.getNecessity())
                        && AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }

    private boolean getOptionalModulesDecision() {
        return !sequence.getModule()
                        .stream()
                                .allMatch(m -> AuthenticationSequenceModuleNecessityType.OPTIONAL.equals(m.getNecessity()))
                || sequence.getModule()
                .stream()
                .anyMatch(m -> AuthenticationSequenceModuleNecessityType.OPTIONAL.equals(m.getNecessity())
                        && AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }

    private boolean allModulesAreSuccessful() {
        return sequence.getModule()
                .stream()
                .allMatch(m -> AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState()));
    }


    private boolean allRequisiteModulesAreSuccessful() {
        return !nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType.REQUISITE);
    }

    private boolean allRequiredModulesAreSuccessful() {
        return !nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType.REQUIRED);
    }

    public boolean nonSuccessfulModuleExists(AuthenticationSequenceModuleNecessityType moduleNecessity) {
        List<AuthenticationSequenceModuleType> modules = sequence.getModule();
        return modules.stream()
                .anyMatch(m -> moduleNecessity.equals(m.getNecessity())
                        && (getAuthenticationByIdentifier(m) == null
                        || !AuthenticationModuleState.SUCCESSFULLY.equals(getAuthenticationByIdentifier(m).getState())));
    }

    public ModuleAuthentication getAuthenticationByIdentifier(AuthenticationSequenceModuleType module) {
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

    public boolean isProcessing() {
        if (!getAuthentications().isEmpty()) {
            for (ModuleAuthentication authModule : getAuthentications()) {
                if (AuthenticationModuleState.LOGIN_PROCESSING.equals(authModule.getState())) {
                    return true;
                }
            }
        }
        return false;
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

    public int getIndexOfModule(ModuleAuthentication authentication) {
        Validate.notNull(authentication);

        for (int i = 0; i < getAuthModules().size(); i++) {
            if (getAuthModules().get(i).getModuleIdentifier().equals(authentication.getModuleIdentifier())) {
                return i;
            }
        }
        return NO_MODULE_FOUND_INDEX;
    }

    public ModuleAuthentication getProcessingModuleAuthentication() {
        for (ModuleAuthentication authentication : getAuthentications()) {
            if (authentication.getState().equals(AuthenticationModuleState.LOGIN_PROCESSING)
                    || authentication.getState().equals(AuthenticationModuleState.LOGOUT_PROCESSING)) {
                return authentication;
            }
        }
        return null;
    }

    public boolean isAuthenticationFailed() {
        return !isAuthenticated()
                && getProcessingModuleAuthentication() == null
                && getAuthentications().size() == getAuthModules().size();
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    @Override
    public String getName() {
        if (getPrincipal() instanceof MidPointPrincipal) {
            return ((MidPointPrincipal) getPrincipal()).getUsername();
        }
        return "";
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
        AuthModule processingModule = getAuthModules().get(actualIndex);
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
                    + getSequence().getIdentifier() + "'");
        }
        return resolvedIndex;
    }

    public boolean isLast(ModuleAuthentication moduleAuthentication) {
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
        if (moduleAuthentication instanceof AnonymousAuthenticationToken) {
            return true;
        }
        return false;
    }

    public boolean hasSucceededAuthentication() {
        return getAuthentications().stream().anyMatch(auth -> AuthenticationModuleState.SUCCESSFULLY.equals(auth.getState()));
    }

    public boolean canContinueAfterCredentialsCheckFail() {
        if (AuthenticationSequenceModuleNecessityType.REQUISITE.equals(getProcessingModuleNecessity())) {
            return false;
        }
        if (AuthenticationSequenceModuleNecessityType.REQUIRED.equals(getProcessingModuleNecessity()) && !isLast(getProcessingModuleAuthentication())) {
            return true;
        }
        if (allModulesNecessityMatch(AuthenticationSequenceModuleNecessityType.OPTIONAL, AuthenticationSequenceModuleNecessityType.SUFFICIENT)
                && !hasSucceededAuthentication() && isLast(getProcessingModuleAuthentication())) {
            return false;
        }
        return true;
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

    private boolean isProcessingModuleFailure() {
        return AuthenticationModuleState.FAILURE.equals(getProcessingModuleState());
    }


    private AuthenticationModuleState getProcessingModuleState() {
        ModuleAuthentication authentication = getProcessingModuleAuthentication();
        return authentication != null ? authentication.getState() : null;
    }

}
