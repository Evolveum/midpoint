/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public abstract class AbstractAuthenticationContext {

    private String username;

    private Class<? extends FocusType> principalType;

    private List<ObjectReferenceType> requireAssignments;

    private boolean supportActivationByChannel = true;

    public AbstractAuthenticationContext(String username, Class<? extends FocusType> principalType) {
        this(username, principalType, null);
    }

    public AbstractAuthenticationContext(String username, Class<? extends FocusType> principalType, List<ObjectReferenceType> requireAssignment){
        this.username = username;
        this.requireAssignments = requireAssignment;
        this.principalType = principalType;
    }

    public String getUsername() {
        return username;
    }

    public Class<? extends FocusType> getPrincipalType() {
        return principalType;
    }

    public void setSupportActivationByChannel(boolean supportActivationByChannel) {
        this.supportActivationByChannel = supportActivationByChannel;
    }

    public boolean isSupportActivationByChannel() {
        return supportActivationByChannel;
    }

    public List<ObjectReferenceType> getRequireAssignments() {
        return requireAssignments;
    }

    public abstract Object getEnteredCredential();

}
