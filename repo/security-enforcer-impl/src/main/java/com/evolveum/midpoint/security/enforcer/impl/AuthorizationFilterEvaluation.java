/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.enforcer.impl.EnforcerFilterOperation.traceFilter;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.FilterCreationUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSelectorType;

/**
 * Evaluation of given {@link Authorization} aimed to determine a "security filter", i.e. additional filter to be
 * applied in searching/filtering situation.
 *
 * It is a part of {@link EnforcerFilterOperation}.
 */
class AuthorizationFilterEvaluation<T> extends AuthorizationEvaluation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    @NotNull private final Class<T> filterType;
    @Nullable private final ObjectFilter originalFilter;
    @NotNull private final List<? extends OwnedObjectSelectorType> objectSelectors;
    @NotNull private final String selectorLabel;
    private final boolean includeSpecial;
    private ObjectFilter autzFilter = null;

    AuthorizationFilterEvaluation(
            @NotNull Class<T> filterType,
            @Nullable ObjectFilter originalFilter,
            @NotNull Authorization authorization,
            @NotNull List<? extends OwnedObjectSelectorType> objectSelectors,
            @NotNull String selectorLabel,
            boolean includeSpecial,
            @NotNull EnforcerOperation op,
            @NotNull OperationResult result) {
        super(authorization, op, result);
        this.filterType = filterType;
        this.originalFilter = originalFilter;
        this.includeSpecial = includeSpecial;
        this.objectSelectors = objectSelectors;
        this.selectorLabel = selectorLabel;
    }

    boolean computeFilter()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        boolean applicable;

        if (objectSelectors.isEmpty()) {

            LOGGER.trace("      No {} specification in authorization (authorization is universally applicable)", selectorLabel);
            autzFilter = FilterCreationUtil.createAll();
            applicable = true;

        } else {

            applicable = false;
            for (OwnedObjectSelectorType objectSelector : objectSelectors) {
                ObjectSelectorFilterEvaluation<T> processor =
                        new ObjectSelectorFilterEvaluation<>(
                                objectSelector, filterType, originalFilter, Set.of(), "TODO",
                                selectorLabel, this, result);

                processor.processFilter(includeSpecial);
                if (processor.isApplicable()) {
                    autzFilter = ObjectQueryUtil.filterOr(autzFilter, processor.getSecurityFilter());
                    applicable = true; // At least one selector is applicable => the whole authorization is applicable
                }
            }
        }
        traceFilter(op, "for authorization (applicable: " + applicable + ")", authorization, autzFilter);
        return applicable;
    }

    ObjectFilter getAutzFilter() {
        return autzFilter;
    }
}
