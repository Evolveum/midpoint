/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.io.Serial;
import java.io.Serializable;

/**
 * Specifies limitations on execution of a script expression in given language.
 *
 * Part of {@link ExpressionEvaluatorProfile}.
 */
@NullMarked
public interface ScriptLanguageExpressionProfile extends Serializable {

    final class EmptyImpl implements ScriptLanguageExpressionProfile {

        @Serial private static final long serialVersionUID = 0L;
        private final AccessDecision defaultDecision;

        EmptyImpl(AccessDecision defaultDecision) {
            this.defaultDecision = defaultDecision;
        }

        @Override
        public boolean isTypeChecking() {
            return false; // We don't require type checking. It provides additional constraints which we don't want to impose.
        }

        @Override
        public @Nullable ExpressionPermissionProfile getPermissionProfile() {
            return null;
        }

        @Override
        public AccessDecision getDefaultDecision() {
            return defaultDecision;
        }
    }

    /** Allows all features of the given language. */
    ScriptLanguageExpressionProfile FULL = new EmptyImpl(AccessDecision.ALLOW);

    /** Disallows given language completely. */
    ScriptLanguageExpressionProfile NONE = new EmptyImpl(AccessDecision.DENY);

    static ScriptLanguageExpressionProfile full() {
        return FULL;
    }

    static ScriptLanguageExpressionProfile none() {
        return NONE;
    }

    /** Decision to be used if permission profile (if there's any) does not provide its own. */
    AccessDecision getDefaultDecision();

    /**
     * Should we apply strict type checking when evaluating the script? It is a prerequisite for using permission profiles,
     * i.e. if turned off, the execution will NOT start with permission profile set.
     *
     * Currently used only for Groovy.
     */
    boolean isTypeChecking();

    /** Details about what packages, classes and methods are allowed to be used in the script. */
    @Nullable ExpressionPermissionProfile getPermissionProfile();

    default boolean hasRestrictions() {
        var permissionProfile = getPermissionProfile();
        return permissionProfile != null && permissionProfile.hasRestrictions();
    }

    default AccessDecision decideClassAccess(String className, String methodName) {
        var permissionProfile = getPermissionProfile();
        if (permissionProfile == null) {
            return getDefaultDecision();
        }
        AccessDecision permissionDecision = permissionProfile.decideClassAccess(className, methodName);
        if (permissionDecision == AccessDecision.DEFAULT) {
            return getDefaultDecision();
        }
        return permissionDecision;
    }

    default AccessDecision decidePackageAccess(String packageName) {
        var permissionProfile = getPermissionProfile();
        if (permissionProfile == null) {
            return getDefaultDecision();
        }
        AccessDecision permissionDecision = permissionProfile.decidePackageAccess(packageName);
        if (permissionDecision == AccessDecision.DEFAULT) {
            return getDefaultDecision();
        }
        return permissionDecision;
    }

    static ScriptLanguageExpressionProfile forDecision(AccessDecision decision) {
        return switch (decision) {
            case ALLOW -> full();
            case DEFAULT, DENY -> none();
        };
    }
}
