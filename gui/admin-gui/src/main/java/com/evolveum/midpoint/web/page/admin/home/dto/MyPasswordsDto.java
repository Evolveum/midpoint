/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPropagationUserControlType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordChangeSecurityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class MyPasswordsDto implements Serializable {

    public static final String F_ACCOUNTS = "accounts";
    public static final String F_PASSWORD = "password";
    public static final String F_OLD_PASSWORD = "oldPassword";


    private PrismObject<? extends FocusType> focus;

    private List<PasswordAccountDto> accounts;
    private ProtectedStringType password;
    private CredentialsPropagationUserControlType propagation;
    private PasswordChangeSecurityType passwordChangeSecurity;
    private String oldPassword;

    public List<PasswordAccountDto> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        return accounts;
    }

    public ProtectedStringType getPassword() {
        return password;
    }

    public void setPassword(ProtectedStringType password) {
        this.password = password;
    }

    public void setPropagation(CredentialsPropagationUserControlType propagation) {
        this.propagation = propagation;
    }

    public CredentialsPropagationUserControlType getPropagation() {
        return propagation;
    }

    public PasswordChangeSecurityType getPasswordChangeSecurity() {
        return passwordChangeSecurity;
    }

    public void setPasswordChangeSecurity(PasswordChangeSecurityType passwordChangeSecurity) {
        this.passwordChangeSecurity = passwordChangeSecurity;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public PrismObject<? extends FocusType> getFocus() {
        return focus;
    }

    public void setFocus(PrismObject<? extends FocusType> focus) {
        this.focus = focus;
    }

    public String getFocusOid() {
        return focus.getOid();
    }
}
