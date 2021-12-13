/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.authentication;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.authentication.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.security.util.ModuleType;
import com.evolveum.midpoint.authentication.api.StateOfModule;

import org.apache.commons.lang3.Validate;
import org.springframework.security.core.Authentication;

/**
 * @author skublik
 */

public class ModuleAuthenticationImpl implements ModuleAuthentication {

    private Authentication authentication;

    private String nameOfModule;

    private ModuleType type;

    private StateOfModule state;

    private String prefix;

    private final String nameOfType;

    private QName focusType;

    private boolean internalLogout = false;

    public ModuleAuthenticationImpl(String nameOfType) {
        Validate.notNull(nameOfType);
        this.nameOfType = nameOfType;
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public String getNameOfModuleType() {
        return nameOfType;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getNameOfModule() {
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

    public StateOfModule getState() {
        return state;
    }

    public void setState(StateOfModule state) {
        this.state = state;
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
        ModuleAuthenticationImpl module = new ModuleAuthenticationImpl(getNameOfModuleType());
        clone(module);
        return module;
    }

    protected void clone (ModuleAuthenticationImpl module) {
        module.setState(this.getState());
        module.setNameOfModule(this.nameOfModule);
        module.setType(this.getType());
        module.setPrefix(this.getPrefix());
        module.setFocusType(this.getFocusType());
    }

    public void setInternalLogout(boolean internalLogout) {
        this.internalLogout = internalLogout;
    }

    public boolean isInternalLogout() {
        return internalLogout;
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
}
