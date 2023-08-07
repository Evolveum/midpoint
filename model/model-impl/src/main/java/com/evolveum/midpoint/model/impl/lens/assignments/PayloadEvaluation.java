/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule.TargetType;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.construction.*;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AssignedFocusMappingEvaluationRequest;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluation of assignment payload i.e. constructions (resource/persona), focus mappings
 * and policy rules (focus and target).
 */
class PayloadEvaluation<AH extends AssignmentHolderType> extends AbstractEvaluation<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(PayloadEvaluation.class);

    PayloadEvaluation(AssignmentPathSegmentImpl segment, EvaluationContext<AH> ctx) {
        super(segment, ctx);
    }

    void evaluate() throws SchemaException {
        assert ctx.assignmentPath.last() == segment;
        assert segment.getOverallConditionState().isNotAllFalse();
        checkIfAlreadyEvaluated();

        if (ctx.ae.loginMode) {
            // In login mode we are interested only in authorization and GUI configuration data
            // that is present in roles.
            LOGGER.trace("Skipping evaluation of payload of assignment {} because of login mode", segment.assignment);
        } else if (!segment.isAssignmentActive() && !segment.direct) {
            LOGGER.trace("Skipping evaluation of payload of assignment {} because it is not valid and it's not the directly assigned one", segment.assignment);
        } else {
            // Directly assigned assignments are visited even if they are not valid (i.e. effectively disabled) - see below

            if (segment.isMatchingOrder) {
                collectResourceObjectConstruction(); // constructions (from invalid direct assignments) are collected
                collectPersonaConstruction(); // constructions (from invalid direct assignments) are collected
                if (segment.isFullPathActive()) {
                    collectFocusMappings(); // but mappings from invalid direct assignments are not
                }
                if (segment.isNonNegativeRelativeRelativityMode()) {
                    // object policy rules from invalid assignments are collected (why?) but only if non-negative (why?)
                    collectObjectPolicyRule();
                }
            }

            if (segment.isMatchingOrderForTarget) {
                // Target policy rules from non-valid direct assignments are collected because of e.g. approvals or SoD.
                // But we consider only non-negative ones (why?)
                if (segment.isNonNegativeRelativeRelativityMode()) {
                    collectTargetPolicyRule();
                }
            }
        }
    }

    private void collectResourceObjectConstruction() {
        ConstructionType constructionBean = segment.assignment.getConstruction();
        if (constructionBean != null) {
            LOGGER.trace("Preparing construction '{}' in {}", constructionBean.getDescription(), segment.source);

            AssignedConstructionBuilder<AH> builder = new AssignedConstructionBuilder<>();
            populateConstructionBuilder(builder, constructionBean, segment.assignmentOrigin);
            AssignedResourceObjectConstruction<AH> construction = builder.build();

            // Do not evaluate the construction here. We will do it in the second pass. Just prepare everything to be evaluated.
            ctx.evalAssignment.addConstruction(construction, segment.getAbsoluteAssignmentRelativityMode()); // TODO
        }
    }

    private void collectPersonaConstruction() {
        PersonaConstructionType constructionBean = segment.assignment.getPersonaConstruction();
        if (constructionBean != null) {
            LOGGER.trace("Preparing persona construction '{}' in {}", constructionBean.getDescription(), segment.source);

            PersonaConstructionBuilder<AH> builder = new PersonaConstructionBuilder<>();
            populateConstructionBuilder(builder, constructionBean, segment.assignmentOrigin);
            PersonaConstruction<AH> construction = builder.build();

            ctx.evalAssignment.addPersonaConstruction(construction, segment.getAbsoluteAssignmentRelativityMode()); // TODO
        }
    }

    private <ACT extends AbstractConstructionType> void populateConstructionBuilder(
            AbstractConstructionBuilder<AH, ACT, ? extends EvaluatedAbstractConstruction<AH>, ?> builder,
            ACT constructionBean,
            @NotNull ConfigurationItemOrigin constructionOrigin) {
        builder.constructionBean(constructionBean, constructionOrigin)
                .assignmentPath(ctx.assignmentPath.clone()) // We have to clone here as the path is constantly changing during evaluation
                .source(segment.source)
                .lensContext(ctx.ae.lensContext)
                .now(ctx.ae.now)
                .originType(OriginType.ASSIGNMENTS)
                .valid(segment.isFullPathActive() && segment.getOverallConditionState().isNewTrue());
    }

    private void collectFocusMappings() throws SchemaException {
        MappingsType mappingsBean = segment.assignment.getFocusMappings();
        if (mappingsBean != null) {
            LOGGER.trace("Request evaluation of focus mappings '{}' in {} ({} mappings)",
                    mappingsBean.getDescription(), segment.source, mappingsBean.getMapping().size());
            @NotNull AssignmentPathVariables assignmentPathVariables = ctx.assignmentPath.computePathVariablesRequired();

            for (MappingType mappingBean : mappingsBean.getMapping()) {
                PlusMinusZero relativityMode = segment.getRelativeAssignmentRelativityMode(); /* TODO */
                if (relativityMode != null) {
                    AssignedFocusMappingEvaluationRequest request =
                            new AssignedFocusMappingEvaluationRequest(
                                    mappingBean,
                                    segment.assignmentOrigin.child(
                                            AssignmentType.F_FOCUS_MAPPINGS, mappingBean.getId()),
                                    segment.source,
                                    ctx.evalAssignment,
                                    relativityMode,
                                    assignmentPathVariables,
                                    segment.sourceDescription);
                    ctx.evalAssignment.addFocusMappingEvaluationRequest(request);
                } else {
                    // This can occur because overall condition state can be "true,false->false", making relative
                    // condition state to be false->false.
                }
            }
        }
    }

    private void collectObjectPolicyRule() {
        var policyRule = segment.assignmentConfigItem.getPolicyRule();
        if (policyRule != null) {
            LOGGER.trace("Collecting object policy rule '{}' in {}", policyRule.getName(), segment.source);
            ctx.evalAssignment.addObjectPolicyRule(
                    createEvaluatedPolicyRule(policyRule, TargetType.OBJECT));
        }
    }

    private void collectTargetPolicyRule() {
        var policyRule = segment.assignmentConfigItem.getPolicyRule();
        if (policyRule != null) {
            boolean appliesDirectly = appliesDirectly(ctx.assignmentPath);
            LOGGER.trace("Collecting target policy rule '{}' in {} (applies directly = {})",
                    policyRule.getName(), segment.source, appliesDirectly);
            ctx.evalAssignment.addTargetPolicyRule(
                    createEvaluatedPolicyRule(
                            policyRule,
                            appliesDirectly ? TargetType.DIRECT_ASSIGNMENT_TARGET : TargetType.INDIRECT_ASSIGNMENT_TARGET));
        }
    }

    /**
     * Decides whether the policy rule (pointed to by `assignmentPath`) is attached directly to the target of the current
     * `evaluatedAssignment` or not. For example, if `jack` is `captain` that induces `sailor`, then any rules attached
     * (possibly via metaroles) to `captain` are considered to apply directly to this evaluated assignment target,
     * whereas any rules attached (possibly via metaroles) to `sailor` are not.
     *
     * We assume there are no deputy relations except for potentially the first one (focus -> eval assignment target).
     */
    private boolean appliesDirectly(AssignmentPathImpl assignmentPath) {
        assert !assignmentPath.isEmpty();
        long zeroOrderCount = assignmentPath.getSegments().stream()
                .filter(seg -> seg.getEvaluationOrderForTarget().getSummaryOrder() == 0)
                .count();
        return zeroOrderCount == 1;
    }

    private @NotNull EvaluatedPolicyRuleImpl createEvaluatedPolicyRule(PolicyRuleConfigItem policyRuleCI, TargetType targetType) {
        return new EvaluatedPolicyRuleImpl(
                policyRuleCI.clone(), // TODO why clone?
                PolicyRuleTypeUtil.createId(segment.getSourceOid(), segment.getAssignmentId()),
                ctx.assignmentPath.clone(),
                ctx.evalAssignment,
                targetType);
    }
}
