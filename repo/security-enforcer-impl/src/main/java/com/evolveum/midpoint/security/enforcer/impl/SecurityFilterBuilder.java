/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.FilterGizmo;

import org.jetbrains.annotations.NotNull;

/** TODO */
class SecurityFilterBuilder<F> {

    @NotNull private final FilterGizmo<F> gizmo;
    @NotNull private final QueryAutzItemPaths queryItemsSpec;

    private F securityFilterAllow = null;
    private F securityFilterDeny = null;

    SecurityFilterBuilder(@NotNull FilterGizmo<F> gizmo, @NotNull QueryAutzItemPaths queryItemsSpec) {
        this.gizmo = gizmo;
        this.queryItemsSpec = queryItemsSpec;
    }

    void addAllow(F increment, Authorization authority) {
        securityFilterAllow = gizmo.or(securityFilterAllow, increment);
        SecurityEnforcerImpl.traceFilter("securityFilterAllow", authority, securityFilterAllow, gizmo);
        if (!gizmo.isNone(increment)) {
            queryItemsSpec.collectItems(authority);
        }
    }

    void addDeny(F increment) {
        securityFilterDeny = gizmo.or(securityFilterDeny, increment);
    }

    void trace(Authorization authority) {
        SecurityEnforcerImpl.traceFilter("securityFilterAllow", authority, securityFilterAllow, gizmo);
        SecurityEnforcerImpl.traceFilter("securityFilterDeny", authority, securityFilterDeny, gizmo);
    }

    F getSecurityFilterAllow() {
        return securityFilterAllow;
    }

    F getSecurityFilterDeny() {
        return securityFilterDeny;
    }
}
