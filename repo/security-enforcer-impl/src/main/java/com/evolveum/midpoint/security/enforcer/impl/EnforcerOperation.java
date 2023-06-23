/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.traces.details.AbstractTraceEvent;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;
import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Represents a {@link SecurityEnforcer} operation: access determination or filter building.
 */
class EnforcerOperation {

    /** Principal to be used: either current or externally-provided one. */
    @Nullable final MidPointPrincipal principal;

    /** Username of the {@link #principal} */
    @Nullable final String username;

    /** {@link OwnerResolver} to be used during this operation. */
    @Nullable final OwnerResolver ownerResolver;

    @NotNull final ProcessingTracer<AbstractTraceEvent> tracer;

    /** Useful Spring beans. */
    @NotNull final Beans b;

    @NotNull final Task task;

    EnforcerOperation(
            @Nullable MidPointPrincipal principal,
            @Nullable OwnerResolver ownerResolver,
            @NotNull SecurityEnforcer.Options options,
            @NotNull Beans beans,
            @NotNull Task task) {
        this.principal = principal;
        this.username = principal != null ? principal.getUsername() : null;
        this.tracer = createTracer(options);
        this.ownerResolver = ownerResolver != null ? ownerResolver : beans.securityContextManager.getUserProfileService();
        this.b = beans;
        this.task = task;
    }

    // temporary
    private ProcessingTracer<AbstractTraceEvent> createTracer(SecurityEnforcer.Options options) {
        return new LogBasedEnforcerAndSelectorTracer(options.logCollector());
    }

    Collection<Authorization> getAuthorizations() {
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

    String getPrincipalOid() {
        return principal != null ? principal.getOid() : null;
    }

    FocusType getPrincipalFocus() {
        return principal != null ? principal.getFocus() : null;
    }

    /** TODO does the method belongs here? */
    Set<String> getAllSelfOids(@NotNull DelegatorSelection delegation) {
        Set<String> all = new HashSet<>();
        CollectionUtils.addIgnoreNull(all, getPrincipalOid());
        if (delegation != DelegatorSelection.NO_DELEGATOR) {
            all.addAll(getDelegators(getLimitationType(delegation)));
        }
        return all;
    }

    /** TODO does the method belongs here? */
    Set<String> getAllSelfPlusRolesOids(@NotNull DelegatorSelection delegation) {
        RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
        Set<String> all = new HashSet<>();
        var principal = getPrincipalFocus();
        if (principal != null) {
            principal.getRoleMembershipRef().stream()
                    .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                    .forEach(ref -> all.add(Objects.requireNonNull(ref.getOid())));
        }
        if (delegation != DelegatorSelection.NO_DELEGATOR) {
            all.addAll(getDelegatedMembership(getLimitationType(delegation)));
        }
        return all;
    }

    private static @NotNull OtherPrivilegesLimitations.Type getLimitationType(@NotNull DelegatorSelection delegation) {
        return switch (delegation) {
            case CASE_MANAGEMENT -> OtherPrivilegesLimitations.Type.CASES;
            case ACCESS_CERTIFICATION -> OtherPrivilegesLimitations.Type.ACCESS_CERTIFICATION;
            default -> throw new AssertionError(delegation);
        };
    }

    private Set<String> getDelegators(
            @Nullable OtherPrivilegesLimitations.Type limitationType) {
        if (principal != null) {
            return principal.getDelegatorsFor(limitationType);
        } else {
            return Set.of();
        }
    }

    private Set<String> getDelegatedMembership(
            @Nullable OtherPrivilegesLimitations.Type limitationType) {
        if (principal != null) {
            return principal.getDelegatedMembershipFor(limitationType);
        } else {
            return Set.of();
        }
    }
}
