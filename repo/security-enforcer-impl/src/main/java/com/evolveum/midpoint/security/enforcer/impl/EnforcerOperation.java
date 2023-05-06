/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;

/**
 * Represents a {@link SecurityEnforcer} operation: access determination or filter building.
 */
class EnforcerOperation<O extends ObjectType> {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    /** Principal to be used: either current or externally-provided one. */
    @Nullable final MidPointPrincipal principal;

    /** Username of the {@link #principal} */
    @Nullable final String username;

    /** {@link OwnerResolver} to be used during this operation. */
    @Nullable final OwnerResolver ownerResolver;

    /** Is tracing enabled for this operation? Present here to avoid repeated determination that takes some CPU cycles. */
    final boolean traceEnabled;

    /** Useful Spring beans. */
    @NotNull final Beans b;

    @NotNull final Task task;

    EnforcerOperation(
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

    Collection<String> getDelegatorsForRequestor() {
        return getDelegators(OtherPrivilegesLimitationType.F_CASE_MANAGEMENT_WORK_ITEMS);
    }

    @SuppressWarnings("SameParameterValue")
    private Collection<String> getDelegators(ItemName... limitationItemNames) {
        Collection<String> rv = new HashSet<>();
        if (principal != null) {
            for (DelegatorWithOtherPrivilegesLimitations delegator :
                    principal.getDelegatorWithOtherPrivilegesLimitationsCollection()) {
                for (ItemName limitationItemName : limitationItemNames) {
                    if (SchemaDeputyUtil.limitationsAllow(delegator.getLimitations(), limitationItemName)) {
                        rv.add(delegator.getDelegator().getOid());
                        break;
                    }
                }
            }
        }
        return rv;
    }

    Collection<String> getDelegatorsForRelatedObjects() {
        // Beware: This is called for both tasks and cases.
        // We do not allow delegators here. Each user should see only cases and tasks related to him (personally).
        return emptySet();
    }

    Collection<String> getDelegatorsForAssignee() {
        return getDelegators(OtherPrivilegesLimitationType.F_CASE_MANAGEMENT_WORK_ITEMS);
    }

    String[] getSelfAndOtherOids(Collection<String> otherOids) {
        if (principal == null) {
            return new String[0];
        }
        List<String> rv = new ArrayList<>(otherOids.size() + 1);
        CollectionUtils.addIgnoreNull(rv, getPrincipalOid());
        rv.addAll(otherOids);
        return rv.toArray(new String[0]);
    }
}
