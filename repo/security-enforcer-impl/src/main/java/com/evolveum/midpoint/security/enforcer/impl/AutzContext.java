/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/** Most probably temporary; will morph to something better soon. */
@Experimental
class AutzContext {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @Nullable final MidPointPrincipal principal;
    @Nullable final String username;
    @Nullable final OwnerResolver ownerResolver;
    final boolean traceEnabled;
    @NotNull final Beans b;
    @NotNull final Task task;

    AutzContext(
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull Beans beans,
            @NotNull Task task) {
        this.principal = principal;
        this.username = principal != null ? principal.getUsername() : null;
        this.ownerResolver = ownerResolver != null ? ownerResolver : beans.securityContextManager.getUserProfileService();
        this.traceEnabled = LOGGER.isTraceEnabled();
        this.b = beans;
        this.task = task;
    }

    public Iterable<? extends Authorization> getAuthorizations() {
        if (principal == null) {
            // Anonymous access, possibly with elevated privileges
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                return authentication.getAuthorities().stream()
                        .filter(a -> a instanceof Authorization)
                        .map(a -> (Authorization) a)
                        .collect(Collectors.toList());
            } else {
                return List.of();
            }
        } else {
            return principal.getAuthorities();
        }
    }
}
