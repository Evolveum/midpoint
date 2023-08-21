/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.evaluator.context;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public class PasswordAuthenticationContext extends AbstractAuthenticationContext {

    private String password;

    public String getPassword() {
        return password;
    }

    public PasswordAuthenticationContext(String username, String password, Class<? extends FocusType> principalType) {
        this(username,password, principalType, null, null);
    }

    public PasswordAuthenticationContext(String username, String password,
            Class<? extends FocusType> principalType,
            List<ObjectReferenceType> requireAssignments,
            AuthenticationChannel channel) {
        super(username, principalType, requireAssignments, channel);
        this.password = password;
    }

    @Override
    public Object getEnteredCredential() {
        return getPassword();
    }

}
