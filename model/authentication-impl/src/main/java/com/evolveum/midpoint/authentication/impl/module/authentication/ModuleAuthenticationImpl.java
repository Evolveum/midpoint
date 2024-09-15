/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.authentication.api.AutheticationFailedData;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author skublik
 *
 */

public class ModuleAuthenticationImpl implements ModuleAuthentication {

    private Authentication authentication;

    private String nameOfModule;

    private ModuleType type;

    private AuthenticationModuleState state;

    private String prefix;

    private final String nameOfType;

    private QName focusType;

    private boolean internalLogout = false;

    private final AuthenticationSequenceModuleType sequenceModule;

    private AuthenticationSequenceModuleNecessityType necessity;

    private final Integer order;

    /**
     * Indicate, is a module can exist on its own. E.g. when module is not sufficient,
     * it means that the module itself cannot perform 'safe' authentication and it is
     * mandatory to add another module so the authentication might be successful. If
     * there is more than one module, but all marked as not sufficient, authentication
     * must not pass.
     */
    private boolean sufficient = true;

    /**
     * Indicates, if it is allowed to skip a module when no such credentials (required
     * by module) are defined.
     */
    private boolean acceptEmpty;

    private AutheticationFailedData failureData;

    public ModuleAuthenticationImpl(String nameOfType, AuthenticationSequenceModuleType sequenceModule) {
        Validate.notNull(nameOfType);
        this.nameOfType = nameOfType;
        this.sequenceModule = sequenceModule;
        this.necessity = sequenceModule.getNecessity();
        this.order = sequenceModule.getOrder();
        this.acceptEmpty = getAcceptEmpty(sequenceModule);
        resolveDefaults();
    }

    private boolean getAcceptEmpty(AuthenticationSequenceModuleType sequenceModule) {
        if (sequenceModule == null) {
            //TODO should this happen?
            return false;
        }
        return BooleanUtils.isTrue(sequenceModule.isAcceptEmpty());
    }

    public boolean canSkipWhenEmptyCredentials() {
        return acceptEmpty;
    }


    private void resolveDefaults() {
        setState(AuthenticationModuleState.LOGIN_PROCESSING);

        if (this.necessity == null) {
            this.necessity = AuthenticationSequenceModuleNecessityType.SUFFICIENT;
        }
    }
    public String getModuleTypeName() {
        return nameOfType;
    }

    @Override
    public AuthenticationSequenceModuleNecessityType getNecessity() {
        return necessity;
    }

    @Override
    public Integer getOrder() {
        return order;
    }

    @Override
    public boolean applicable() {
        return true;
    }

    AuthenticationSequenceModuleType getSequenceModule() {
        return sequenceModule;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getModuleIdentifier() {
        return nameOfModule;
    }

    public void setNameOfModule(String nameOfModule) {
        this.nameOfModule = nameOfModule;
    }

    public ModuleType getType() {
        return type;
    }

    protected void setType(ModuleType type) {
        this.type = type;
    }

    public AuthenticationModuleState getState() {
        return state;
    }

    public void setState(AuthenticationModuleState state) {
        this.state = state;
    }

    public void recordFailure(AuthenticationException e) {
        this.state = AuthenticationModuleState.FAILURE;
        if (failureData == null) {
            failureData = new AutheticationFailedData(null, null);
        }
        failureData.setAuthenticationException(e);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public QName getFocusType() {
        return focusType;
    }

    public void setFocusType(QName focusType) {
        this.focusType = focusType;
    }

    public ModuleAuthenticationImpl clone() {
        ModuleAuthenticationImpl module = new ModuleAuthenticationImpl(getModuleTypeName(), getSequenceModule());
        clone(module);
        return module;
    }

    protected void clone (ModuleAuthenticationImpl module) {
        module.setState(this.getState());
        module.setNameOfModule(this.nameOfModule);
        module.setType(this.getType());
        module.setPrefix(this.getPrefix());
        module.setFocusType(this.getFocusType());
        module.setSufficient(this.isSufficient());
    }

    public void setInternalLogout(boolean internalLogout) {
        this.internalLogout = internalLogout;
    }

    public boolean isInternalLogout() {
        return internalLogout;
    }

    @Override
    public boolean isSufficient() {
        return sufficient;
    }

    @Override
    public void setSufficient(boolean sufficient) {
        this.sufficient = sufficient;
    }

    @Override
    public AutheticationFailedData getFailureData() {
        return failureData;
    }

    @Override
    public void setFailureData(AutheticationFailedData failureData) {
        this.failureData = failureData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleAuthenticationImpl that = (ModuleAuthenticationImpl) o;
        return  Objects.equals(nameOfModule, that.nameOfModule) &&
                type == that.type &&
                Objects.equals(prefix, that.prefix) &&
                Objects.equals(nameOfType, that.nameOfType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameOfModule, type, prefix, nameOfType);
    }

    @Override
    public DisplayType getDisplay() {
        if (sequenceModule == null) {
            return null;
        }
        return sequenceModule.getDisplay();
    }

    @Override
    public GuiActionType getAction() {
        if (sequenceModule == null) {
            return null;
        }
        return sequenceModule.getAction();
    }
}
