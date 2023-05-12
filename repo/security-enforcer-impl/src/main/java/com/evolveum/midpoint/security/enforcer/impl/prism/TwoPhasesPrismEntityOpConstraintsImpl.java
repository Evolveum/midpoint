/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.security.enforcer.api.PrismEntityOpConstraints;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/** TODO */
public abstract class TwoPhasesPrismEntityOpConstraintsImpl<OC extends SinglePhasePrismEntityOpConstraintsImpl<?>>
        implements PrismEntityOpConstraints {

    @NotNull final OC request;
    @NotNull final OC execution;

    TwoPhasesPrismEntityOpConstraintsImpl(
            @NotNull OC request,
            @NotNull OC execution) {
        this.request = request;
        this.execution = execution;
    }

    @Override
    public @NotNull AccessDecision getDecision() {
        AccessDecision requestDecision = request.getDecision();
        if (requestDecision == AccessDecision.DENY) {
            return AccessDecision.DENY;
        } else {
            return AccessDecision.combine(
                    requestDecision,
                    execution.getDecision());
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                String.format("%s [%s]\n",
                        getDebugLabel(), getClass().getSimpleName()),
                indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Request phase", request, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Execution phase", execution, indent + 1);
        return sb.toString();
    }

    abstract String getDebugLabel();

    public static class ForItemContent
            extends TwoPhasesPrismEntityOpConstraintsImpl<SinglePhasePrismEntityOpConstraintsImpl.ForItemContent>
            implements UpdatablePrismEntityOpConstraints.ForItemContent {

        ForItemContent(
                @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForItemContent request,
                @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForItemContent execution) {
            super(request, execution);
        }

        @Override
        public @NotNull TwoPhasesPrismEntityOpConstraintsImpl.ForValueContent getValueConstraints(@NotNull PrismValue value) {
            return new TwoPhasesPrismEntityOpConstraintsImpl.ForValueContent(
                    request.getValueConstraints(value),
                    execution.getValueConstraints(value));
        }

        @Override
        String getDebugLabel() {
            return "Two-phases item-attached operation constraints";
        }
    }

    public static class ForValueContent extends TwoPhasesPrismEntityOpConstraintsImpl<SinglePhasePrismEntityOpConstraintsImpl.ForValueContent>
            implements UpdatablePrismEntityOpConstraints.ForValueContent {

        public ForValueContent() {
            this(
                    new SinglePhasePrismEntityOpConstraintsImpl.ForValueContent(AuthorizationPhaseType.REQUEST),
                    new SinglePhasePrismEntityOpConstraintsImpl.ForValueContent(AuthorizationPhaseType.EXECUTION));
        }

        ForValueContent(
                @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForValueContent request,
                @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForValueContent execution) {
            super(request, execution);
        }

        @Override
        public @NotNull TwoPhasesPrismEntityOpConstraintsImpl.ForItemContent getItemConstraints(@NotNull ItemName name) {
            return new TwoPhasesPrismEntityOpConstraintsImpl.ForItemContent(
                    request.getItemConstraints(name),
                    execution.getItemConstraints(name));
        }

        public void applyAuthorization(
                @NotNull PrismObject<? extends ObjectType> object, @NotNull AuthorizationEvaluation evaluation)
                throws ConfigurationException {
            request.applyAuthorization(object, evaluation);
            execution.applyAuthorization(object, evaluation);
        }

        @Override
        String getDebugLabel() {
            return "Two-phases value-attached operation constraints";
        }
    }
}
