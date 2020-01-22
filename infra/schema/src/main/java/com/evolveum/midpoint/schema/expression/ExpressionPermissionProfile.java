/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionClassProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionMethodProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionPackageProfileType;

/**
 * Compiled expression permission profile.
 * Compiled from ExpressionPermissionProfileType.
 *
 * @author Radovan Semancik
 */
public class ExpressionPermissionProfile {

    private final String identifier;
    private AccessDecision decision;
    private final List<ExpressionPermissionPackageProfileType> packageProfiles = new ArrayList<>();
    private final List<ExpressionPermissionClassProfileType> classProfiles = new ArrayList<>();

    public ExpressionPermissionProfile(String identifier) {
        super();
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public AccessDecision getDecision() {
        return decision;
    }

    public void setDecision(AccessDecision decision) {
        this.decision = decision;
    }

    public List<ExpressionPermissionPackageProfileType> getPackageProfiles() {
        return packageProfiles;
    }

    public List<ExpressionPermissionClassProfileType> getClassProfiles() {
        return classProfiles;
    }

    public boolean hasRestrictions() {
        return !classProfiles.isEmpty() || !packageProfiles.isEmpty() || decision != AccessDecision.ALLOW;
    }

    public AccessDecision decideClassAccess(String className, String methodName) {
        ExpressionPermissionClassProfileType classProfile = getClassProfile(className);
        if (classProfile == null) {
            ExpressionPermissionPackageProfileType packageProfile = getPackageProfileByClassName(className);
            if (packageProfile == null) {
                return decision;
            } else {
                return AccessDecision.translate(packageProfile.getDecision());
            }
        }
        ExpressionPermissionMethodProfileType methodProfile = getMethodProfile(classProfile, methodName);
        if (methodProfile == null) {
            return AccessDecision.translate(classProfile.getDecision());
        } else {
            return AccessDecision.translate(methodProfile.getDecision());
        }
    }

    private ExpressionPermissionPackageProfileType getPackageProfileByClassName(String className) {
        for (ExpressionPermissionPackageProfileType packageProfile : packageProfiles) {
            if (isMemeberClass(packageProfile, className)) {
                return packageProfile;
            }
        }
        return null;
    }



    private boolean isMemeberClass(ExpressionPermissionPackageProfileType packageProfile, String className) {
        // TODO Maybe too simple. But this will do for now.
        return className.startsWith(packageProfile.getName());
    }

    private ExpressionPermissionClassProfileType getClassProfile(String className) {
        for (ExpressionPermissionClassProfileType classProfile : classProfiles) {
            if (className.equals(classProfile.getName())) {
                return classProfile;
            }
        }
        return null;
    }

    private void add(ExpressionPermissionClassProfileType classProfile) {
        classProfiles.add(classProfile);
    }

    private ExpressionPermissionMethodProfileType getMethodProfile(ExpressionPermissionClassProfileType classProfile, String methodName) {
        if (methodName == null) {
            return null;
        }
        for (ExpressionPermissionMethodProfileType methodProfile : classProfile.getMethod()) {
            if (methodName.equals(methodProfile.getName())) {
                return methodProfile;
            }
        }
        return null;
    }

    /**
     * Used to easily set up access for built-in class access rules.
     */
    public void addClassAccessRule(String className, String methodName, AccessDecision decision) {
        ExpressionPermissionClassProfileType classProfile = getClassProfile(className);
        if (classProfile == null) {
            classProfile = new ExpressionPermissionClassProfileType();
            classProfile.setName(className);
            add(classProfile);
        }
        if (methodName == null) {
            classProfile.setDecision(decision.getAuthorizationDecisionType());
        } else {
            ExpressionPermissionMethodProfileType methodProfile = getMethodProfile(classProfile, methodName);
            if (methodProfile == null) {
                methodProfile = new ExpressionPermissionMethodProfileType();
                methodProfile.setName(methodName);
                methodProfile.setDecision(decision.getAuthorizationDecisionType());
                classProfile.getMethod().add(methodProfile);
            } else {
                methodProfile.setDecision(decision.getAuthorizationDecisionType());
            }
        }

    }


    /**
     * Used to easily set up access for built-in class access rules (convenience).
     */
    public void addClassAccessRule(Class<?> clazz, String methodName, AccessDecision decision) {
        addClassAccessRule(clazz.getName(), methodName, decision);
    }

    public void addClassAccessRule(Class<?> clazz, AccessDecision decision) {
        addClassAccessRule(clazz.getName(), null, decision);
    }
}
