/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

public class SecurityEnforcerUtil {

    /**
     * Returns authorizations for the given principal, or from the current security context.
     *
     * Quite weird method: the principal should (mostly) come from the security context as well, but not necessarily.
     * This is quite a hack to avoid duplicating this functionality in different places.
     */
    public static @NotNull Collection<Authorization> getAuthorizations(@Nullable MidPointPrincipal principal) {
        if (principal != null) {
            return principal.getAuthorities();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .filter(a -> a instanceof Authorization)
                    .map(a -> (Authorization) a)
                    .toList();
        } else {
            return List.of();
        }
    }
}
