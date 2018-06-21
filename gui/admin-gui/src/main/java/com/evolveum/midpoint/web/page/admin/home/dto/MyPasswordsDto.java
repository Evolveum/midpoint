/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPropagationUserControlType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordChangeSecurityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class MyPasswordsDto implements Serializable {

    public static final String F_ACCOUNTS = "accounts";
    public static final String F_PASSWORD = "password";
    public static final String F_OLD_PASSWORD = "oldPassword";

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
}
