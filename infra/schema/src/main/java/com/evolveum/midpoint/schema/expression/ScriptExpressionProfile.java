/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class ScriptExpressionProfile implements Serializable {

    private final String language;
    private AccessDecision decision;
    private Boolean typeChecking;
    private ExpressionPermissionProfile permissionProfile;

    public ScriptExpressionProfile(String language) {
        super();
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public void setDecision(AccessDecision decision) {
        this.decision = decision;
    }

    public Boolean isTypeChecking() {
        return typeChecking;
    }

    public void setTypeChecking(Boolean typeChecking) {
        this.typeChecking = typeChecking;
    }

    public ExpressionPermissionProfile getPermissionProfile() {
        return permissionProfile;
    }

    public void setPermissionProfile(ExpressionPermissionProfile permissionProfile) {
        this.permissionProfile = permissionProfile;
    }

    public boolean hasRestrictions() {
        if (permissionProfile == null) {
            return false;
        }
        return permissionProfile.hasRestrictions();
    }

    public AccessDecision decideClassAccess(String className, String methodName) {
        if (permissionProfile == null) {
            return decision;
        }
        AccessDecision permissionDecision = permissionProfile.decideClassAccess(className, methodName);
        if (permissionDecision == null || permissionDecision == AccessDecision.DEFAULT) {
            return decision;
        }
        return permissionDecision;
    }


}
