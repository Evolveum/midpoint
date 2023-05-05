/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;

abstract class AuthorizationProcessor {

    @NotNull final Authorization authorization;
    @NotNull private final Lazy<String> lazyDescription;

    @NotNull final AutzContext ctx;
    @Nullable final MidPointPrincipal principal;
    @Nullable final OwnerResolver ownerResolver;
    @NotNull final Beans b;
    @NotNull private final Task task;
    @NotNull final OperationResult result;

    AuthorizationProcessor(
            @NotNull Authorization authorization,
            @NotNull AutzContext ctx,
            @NotNull OperationResult result) {
        this.authorization = authorization;
        this.ctx = ctx;
        this.principal = ctx.principal;
        this.ownerResolver = ctx.ownerResolver;
        this.b = ctx.b;
        this.task = ctx.task;
        this.result = result;
        this.lazyDescription = Lazy.from(() -> this.authorization.getHumanReadableDesc());
    }

    ObjectFilterExpressionEvaluator createFilterEvaluator(String desc) {
        return filter -> {
            if (filter == null) {
                return null;
            }
            VariablesMap variables = new VariablesMap();
            PrismObject<? extends FocusType> subject = principal != null ? principal.getFocus().asPrismObject() : null;
            PrismObjectDefinition<? extends FocusType> def;
            if (subject != null) {
                def = subject.getDefinition();
                if (def == null) {
                    def = b.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(subject.asObjectable().getClass());
                }
                variables.addVariableDefinition(ExpressionConstants.VAR_SUBJECT, subject, def);
            } else {
                // ???
            }

            return ExpressionUtil.evaluateFilterExpressions(filter, variables, MiscSchemaUtil.getExpressionProfile(), b.expressionFactory, b.prismContext,
                    "expression in " + desc + " in authorization " + getDesc(), task, result);
        };
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

    String getDesc() {
        return lazyDescription.get();
    }
}
