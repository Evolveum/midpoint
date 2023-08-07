/*
 * Copyright (C) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.DeeplyFreezableList;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionClassProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionMethodProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionPackageProfileType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionPermissionProfileType;

import org.jetbrains.annotations.NotNull;

/**
 * Compiled expression permission profile.
 * Compiled from {@link ExpressionPermissionProfileType}.
 *
 * Immutable.
 *
 * It is the basic building block of https://docs.evolveum.com/midpoint/reference/expressions/expressions/profiles/[Expression
 * Profiles] functionality; contained within {@link ScriptLanguageExpressionProfile}.
 *
 * TODO is this a good name?
 *
 * @author Radovan Semancik
 */
public class ExpressionPermissionProfile extends AbstractFreezable implements Serializable {

    @NotNull private final String identifier;
    @NotNull private final AccessDecision defaultDecision;
    @NotNull private final DeeplyFreezableList<ExpressionPermissionPackageProfileType> packageProfiles;
    @NotNull private final DeeplyFreezableList<ExpressionPermissionClassProfileType> classProfiles;

    private ExpressionPermissionProfile(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            @NotNull DeeplyFreezableList<ExpressionPermissionPackageProfileType> packageProfiles,
            @NotNull DeeplyFreezableList<ExpressionPermissionClassProfileType> classProfiles) {
        this.identifier = identifier;
        this.defaultDecision = defaultDecision;
        this.packageProfiles = packageProfiles;
        this.classProfiles = classProfiles;
    }

    /** Creates semi-frozen profile (lists are open). */
    public static ExpressionPermissionProfile open(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision) {
        return new ExpressionPermissionProfile(
                identifier,
                defaultDecision,
                new DeeplyFreezableList<>(),
                new DeeplyFreezableList<>());
    }

    /** Creates frozen profile. */
    public static ExpressionPermissionProfile closed(
            @NotNull String identifier,
            @NotNull AccessDecision defaultDecision,
            @NotNull List<ExpressionPermissionPackageProfileType> packageProfiles,
            @NotNull List<ExpressionPermissionClassProfileType> classProfiles) {
        var profile = new ExpressionPermissionProfile(
                identifier,
                defaultDecision,
                new DeeplyFreezableList<>(CloneUtil.toImmutableContainerablesList(packageProfiles)),
                new DeeplyFreezableList<>(CloneUtil.toImmutableContainerablesList(classProfiles)));
        profile.freeze();
        return profile;
    }

    @Override
    protected void performFreeze() {
        packageProfiles.freeze();
        classProfiles.freeze();
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    boolean hasRestrictions() {
        return !classProfiles.isEmpty()
                || !packageProfiles.isEmpty()
                || defaultDecision != AccessDecision.ALLOW;
    }

    @NotNull AccessDecision decideClassAccess(String className, String methodName) {
        ExpressionPermissionClassProfileType classProfile = getClassProfile(className);
        if (classProfile == null) {
            ExpressionPermissionPackageProfileType packageProfile = getPackageProfileByClassName(className);
            if (packageProfile == null) {
                return defaultDecision;
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
            if (isMemberClass(packageProfile, className)) {
                return packageProfile;
            }
        }
        return null;
    }

    private boolean isMemberClass(ExpressionPermissionPackageProfileType packageProfile, String className) {
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

    private ExpressionPermissionMethodProfileType getMethodProfile(
            ExpressionPermissionClassProfileType classProfile, String methodName) {
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
    private void addClassAccessRule(String className, String methodName, AccessDecision decision) {
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

    @Override
    public String toString() {
        return "ExpressionPermissionProfile[" +
                "identifier=" + identifier + ", " +
                "decision=" + defaultDecision + ", " +
                "packageProfiles: " + packageProfiles.size() + ", " +
                "classProfiles: " + classProfiles.size() + ']';
    }
}
