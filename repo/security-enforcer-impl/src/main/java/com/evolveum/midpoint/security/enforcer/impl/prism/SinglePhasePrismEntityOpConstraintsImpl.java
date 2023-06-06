/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/**
 * Compiled operation constraints for given {@link Item} or {@link PrismValue}.
 *
 * Related to given {@link AuthorizationPhaseType}.
 *
 * For constraints covering multiple phases, please see {@link TwoPhasesPrismEntityOpConstraintsImpl}.
 */
public abstract class SinglePhasePrismEntityOpConstraintsImpl<CI extends PrismEntityCoverageInformation>
        implements UpdatablePrismEntityOpConstraints {

    @NotNull final AuthorizationPhaseType phase;

    @NotNull final CI allowed;
    @NotNull final CI denied;

    SinglePhasePrismEntityOpConstraintsImpl(@NotNull AuthorizationPhaseType phase, @NotNull CI allowed, @NotNull CI denied) {
        this.phase = phase;
        this.allowed = allowed;
        this.denied = denied;
    }

    @Override
    public @NotNull AccessDecision getDecision() {
        PrismEntityCoverage denyCoverage = denied.getCoverage();
        if (denyCoverage == PrismEntityCoverage.FULL) {
            return AccessDecision.DENY;
        } else if (denyCoverage == PrismEntityCoverage.NONE) {
            PrismEntityCoverage allowedCoverage = allowed.getCoverage();
            switch (allowedCoverage) {
                case FULL:
                    return AccessDecision.ALLOW;
                case PARTIAL:
                    return AccessDecision.DEFAULT;
                case NONE:
                    return AccessDecision.DENY;
                default:
                    throw new AssertionError(allowedCoverage);
            }
        } else {
            assert denyCoverage == PrismEntityCoverage.PARTIAL;
            PrismEntityCoverage allowedCoverage = allowed.getCoverage();
            switch (allowedCoverage) {
                case FULL:
                case PARTIAL:
                    return AccessDecision.DEFAULT;
                case NONE:
                    return AccessDecision.DENY;
                default:
                    throw new AssertionError(allowedCoverage);
            }
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                String.format("%s for %s [%s]\n", getDebugLabel(), phase, getClass().getSimpleName()),
                indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Allowed", allowed, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Denied", denied, indent + 1);
        return sb.toString();
    }

    abstract String getDebugLabel();

    public static class ForItemContent extends SinglePhasePrismEntityOpConstraintsImpl<PrismItemCoverageInformation>
            implements UpdatablePrismEntityOpConstraints.ForItemContent {

        ForItemContent(
                @NotNull AuthorizationPhaseType phase,
                @NotNull PrismItemCoverageInformation allowed,
                @NotNull PrismItemCoverageInformation denied) {
            super(phase, allowed, denied);
        }

        @Override
        public @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForValueContent getValueConstraints(@NotNull PrismValue value) {
            return new SinglePhasePrismEntityOpConstraintsImpl.ForValueContent(
                    phase,
                    allowed.getValueCoverageInformation(value),
                    denied.getValueCoverageInformation(value));
        }

        @Override
        String getDebugLabel() {
            return "Item-attached operation constraints";
        }
    }

    public static class ForValueContent
            extends SinglePhasePrismEntityOpConstraintsImpl<PrismValueCoverageInformation>
            implements UpdatablePrismEntityOpConstraints.ForValueContent {

        public ForValueContent(@NotNull AuthorizationPhaseType phase) {
            this(phase,
                    PrismValueCoverageInformation.noCoverage(),
                    PrismValueCoverageInformation.noCoverage());
        }

        ForValueContent(
                @NotNull AuthorizationPhaseType phase,
                @NotNull PrismValueCoverageInformation allowed,
                @NotNull PrismValueCoverageInformation denied) {
            super(phase, allowed, denied);
        }

        @Override
        public @NotNull SinglePhasePrismEntityOpConstraintsImpl.ForItemContent getItemConstraints(@NotNull ItemName name) {
            return new SinglePhasePrismEntityOpConstraintsImpl.ForItemContent(
                    phase,
                    allowed.getItemCoverageInformation(name),
                    denied.getItemCoverageInformation(name));
        }

        public void applyAuthorization(
                @NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            var authorization = evaluation.getAuthorization();
            if (authorization.matchesPhase(phase)) {
                PrismValueCoverageInformation coverageIncrement =
                        PrismValueCoverageInformation.forAuthorization(value, evaluation);
                if (coverageIncrement != null) {
                    if (authorization.isAllow()) {
                        allowed.merge(coverageIncrement);
                    } else {
                        denied.merge(coverageIncrement);
                    }
                }
            }
        }

        @Override
        String getDebugLabel() {
            return "Value-attached operation constraints";
        }
    }
}
