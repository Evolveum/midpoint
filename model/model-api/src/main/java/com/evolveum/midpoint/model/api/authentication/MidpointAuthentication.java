/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author skublik
 */

public class MidpointAuthentication extends AbstractAuthenticationToken {

    private final List<AuthenticationSequenceModuleType> modules;

    private AuthenticationSequenceType sequence;

    private AuthenticationChannel authenticationChannel;

    private List<ModuleAuthentication> authentications = new ArrayList<ModuleAuthentication>();

    private List<AuthModule> authModules;

    private Object principal;

    private Object credential;

    private String sessionId;

    private Collection<? extends GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;

    public MidpointAuthentication(AuthenticationSequenceType sequence) {
        super(null);
        this.modules = SecurityPolicyUtil.getSortedModules(sequence);
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
        return (Collection<GrantedAuthority>) authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public List<AuthenticationSequenceModuleType> getModules() {
        return Collections.unmodifiableList(modules);
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
        List<AuthenticationSequenceModuleType> modules = getModules();
        if (modules.isEmpty()) {
            return false;
        }
        for (AuthenticationSequenceModuleType module : modules) {
            ModuleAuthentication authentication = getAuthenticationByName(module.getName());
            if (authentication == null) {
                continue;
            }
            //TODO we will complete after supporting of full "necessity"
            if (AuthenticationSequenceModuleNecessityType.SUFFICIENT.equals(module.getNecessity())) {
                if (StateOfModule.SUCCESSFULLY.equals(authentication.getState())) {
                    return true;
                }
            }

        }
        return false;
    }

    public ModuleAuthentication getAuthenticationByName(String moduleName) {
        for (ModuleAuthentication authentication : getAuthentications()) {
            if (authentication.getNameOfModule().equals(moduleName)) {
                return authentication;
            }
        }
        return null;
    }

    public boolean isProcessing() {
        if (!getAuthentications().isEmpty()) {
            for (ModuleAuthentication authModule : getAuthentications()) {
                if (StateOfModule.LOGIN_PROCESSING.equals(authModule.getState())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getIndexOfProcessingModule( boolean createEmptyAuthenticationIfNeeded){
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
        if (isAuthenticated()) {
            return -2;
        }
        int actualSize = getAuthentications().size();
        int endSize = getAuthModules().size();
        if (actualSize < endSize) {
            if (createEmptyAuthenticationIfNeeded) {
                addAuthentications(getAuthModules().get(actualSize).getBaseModuleAuthentication());
            }
            return actualSize;
        }
        return  -1;
//        throw new IllegalStateException("Couldn't find index of processing module");
    }

    public int getIndexOfModule(ModuleAuthentication authentication){
        Validate.notNull(authentication);

        for (int i = 0; i < getModules().size(); i++) {
            if (getModules().get(i).getName().equals(authentication.getNameOfModule())) {
                return i;
            }
        }
        return -1;
    }

    public ModuleAuthentication getProcessingModuleAuthentication() {
        for (ModuleAuthentication authentication : getAuthentications()) {
            if (authentication.getState().equals(StateOfModule.LOGIN_PROCESSING)
                || authentication.getState().equals(StateOfModule.LOGOUT_PROCESSING)) {
                return authentication;
            }
        }
        return null;
    }

    public boolean isAuthenticationFailed() {
        if (!isAuthenticated() && getProcessingModuleAuthentication() == null
                && getAuthentications().size() == getAuthModules().size()) {
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        if (getPrincipal() instanceof MidPointPrincipal) {
            return ((MidPointPrincipal) getPrincipal()).getUsername();
        }
        return "";
    }

    public List<ModuleAuthentication> getParallelProcessingModules() {
        int indexOfProcessingModule =  getIndexOfProcessingModule(false);
        if (indexOfProcessingModule == -2) {
            return new ArrayList<ModuleAuthentication>();
        }
        return getParallelProcessingModules(indexOfProcessingModule);
    }

    private List<ModuleAuthentication> getParallelProcessingModules(int actualIndex) {
        List<ModuleAuthentication> parallelProcesingModules = new ArrayList<ModuleAuthentication>();
        ModuleAuthentication processingModule = getAuthentications().get(actualIndex);
        AuthenticationSequenceModuleType processingModuleType = getModules().get(actualIndex);
        if (processingModule == null) {
            return parallelProcesingModules;
        }

        if (actualIndex > 0) {
            for (int i = actualIndex - 1; i >= 0; i--) {
                if (getModules().get(i) != null
                        && processingModuleType.getOrder() == getModules().get(i).getOrder()) {
                    parallelProcesingModules.add(getAuthModules().get(i).getBaseModuleAuthentication());
                } else {
                    break;
                }
            }
        }
        parallelProcesingModules.add(processingModule);
        for (int i = actualIndex + 1; i < getModules().size(); i++) {
            if (getModules().get(i) != null
                    && processingModuleType.getOrder() == getModules().get(i).getOrder()) {
                parallelProcesingModules.add(getAuthModules().get(i).getBaseModuleAuthentication());
            }
        }



        return parallelProcesingModules;
    }

    public int resolveParallelModules(HttpServletRequest request, int actualIndex) {
        String header = request.getHeader("Authorization");
        if (header == null){
            return actualIndex;
        }

        String type = header.split(" ")[0];
        List<ModuleAuthentication> parallelProcesingModules = getParallelProcessingModules(actualIndex);
        int resolvedIndex = -1;
        for (ModuleAuthentication parallelProcesingModule : parallelProcesingModules) {
            int usedIndex = getAuthentications().indexOf(parallelProcesingModule);
            if (parallelProcesingModule.getNameOfModuleType().toLowerCase().equals(type.toLowerCase())
            && resolvedIndex == -1) {
                parallelProcesingModule.setState(StateOfModule.LOGIN_PROCESSING);
                if (usedIndex != -1) {
                    resolvedIndex = usedIndex;
                } else {
                    resolvedIndex = getAuthentications().size();
                }
            } else {
                parallelProcesingModule.setState(StateOfModule.FAILURE);
            }
            if (usedIndex == -1) {
                getAuthentications().add(parallelProcesingModule);
            } else {
                getAuthentications().set(usedIndex, parallelProcesingModule);
            }
        }
        if (resolvedIndex == -1){
            throw new IllegalArgumentException("Couldn't find module with type '" + type + "' in sequence '"
                    + getSequence().getName() + "'");
        }
        return resolvedIndex;
    }

    public boolean isLast(ModuleAuthentication moduleAuthentication){
        if (getAuthentications().isEmpty()) {
            return false;
        }
        int index = getIndexOfModule(moduleAuthentication);
        if (index == -1) {
            return false;
        }
        if (index == getModules().size()-1) {
            return true;
        }
        return getModules().get(index).getOrder().equals(getModules().get(getModules().size()-1).getOrder());
    }
}
