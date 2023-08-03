/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.api.evaluator.context;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public class NodeAuthenticationContext extends AbstractAuthenticationContext {

    private String enteredPassword;
    private String remoteName;

    public NodeAuthenticationContext(String remoteName, String username, String enderedPassword) {
        super(username, null, null, null);
        this.enteredPassword = enderedPassword;
        this.remoteName = remoteName;
    }

    @Override
    public String getEnteredCredential() {
        return enteredPassword;
    }

    public String getRemoteName() {
        return remoteName;
    }
}
