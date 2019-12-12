/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;
import org.apache.commons.lang3.Validate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.*;

/**
 * @author skublik
 */

public class MidpointAuthentication extends AbstractAuthenticationToken {

    private final List<AuthenticationSequenceModuleType> modules;

    private AuthenticationSequenceType sequence;

    private List<ModuleAuthentication> authentications = new ArrayList<ModuleAuthentication>();

    private List<AuthModule> authModules;

    private Object principal;

    private Object credential;

    private Collection<GrantedAuthority> authorities = AuthorityUtils.NO_AUTHORITIES;

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

    public List<ModuleAuthentication> getAuthentications() {
        return authentications;
    }

    public void addAuthentications(ModuleAuthentication authentication) {
        getAuthentications().add(authentication);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<GrantedAuthority> authorities) {
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
        return getAuthentications().indexOf(authentication);
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
}
