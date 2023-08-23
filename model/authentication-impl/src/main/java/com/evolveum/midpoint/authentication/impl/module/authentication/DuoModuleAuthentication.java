/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

import org.springframework.security.authentication.AuthenticationServiceException;

import java.io.Serializable;

public class DuoModuleAuthentication extends RemoteModuleAuthenticationImpl implements RemoteModuleAuthentication, Serializable {

    private String duoState;

    private String username;

    public DuoModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.DUO, sequenceModule);
        setType(ModuleType.REMOTE);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        DuoModuleAuthentication module = new DuoModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setDuoState(this.getDuoState());
        module.setUserName(this.getUsername());
        module.setProviders(this.getProviders());

        clone(module);
        return module;
    }

    @Override
    public boolean applicable() {
        if (getUsername() != null) {
            return true;
        }

        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return false;
        }
        setUserName(principal.getUsername());

        return true;
    }

    public String getDuoState() {
        return duoState;
    }

    public void setDuoState(String duoState) {
        this.duoState = duoState;
    }

    public void setUserName(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
