/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.progress.ProgressDto;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPropagationUserControlType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordChangeSecurityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public class MyPasswordsDto implements Serializable {

    public static final String F_ACCOUNTS = "accounts";
    public static final String F_PASSWORD = "password";
    public static final String F_OLD_PASSWORD = "oldPassword";


    private PrismObject<? extends FocusType> focus;

    private List<PasswordAccountDto> accounts;
    private Map<String, ValuePolicyType> passwordPolicies;
    private ProtectedStringType password;
    private CredentialsPropagationUserControlType propagation;
    private PasswordChangeSecurityType passwordChangeSecurity;
    private String oldPassword;
    private ProgressDto progress = new ProgressDto();


    public List<PasswordAccountDto> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        return accounts;
    }

    public Map<String, ValuePolicyType> getPasswordPolicies() {
        if (passwordPolicies == null) {
            passwordPolicies = new HashMap<>();
        }
        return passwordPolicies;
    }

    public void addPasswordPolicy(@NotNull ValuePolicyType policyType) {
        getPasswordPolicies().put(policyType.getOid(), policyType);
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

    public ValuePolicyType getFocusPolicy() {
        for(PasswordAccountDto accountDto : getAccounts()) {
            if (accountDto.isMidpoint()){
                if (accountDto.getPasswordValuePolicyOid() != null) {
                    return getPasswordPolicies().get(accountDto.getPasswordValuePolicyOid());
                }
                break;
            }
        }
        return null;
    }

    public ProgressDto getProgress() {
        return progress;
    }

    public void setProgress(ProgressDto progress) {
        this.progress = progress;
    }
}
