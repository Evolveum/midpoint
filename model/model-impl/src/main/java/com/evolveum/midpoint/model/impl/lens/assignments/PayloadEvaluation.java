/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.construction.Construction;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PersonaConstruction;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AssignedFocusMappingEvaluationRequest;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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
                collectConstruction(); // constructions (from invalid direct assignments) are collected
                collectPersonaConstruction(); // constructions (from invalid direct assignments) are collected
                if (segment.isFullPathActive()) {
                    collectFocusMappings(); // but mappings from invalid direct assignments are not
                }
                if (segment.isNonNegativeRelativeRelativityMode()) {
                    collectFocusPolicyRule(); // focus policy rules from invalid assignments are collected (why?) but only if non-negative (why?)
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

    private void collectConstruction() {
        ConstructionType constructionBean = segment.assignment.getConstruction();
        if (constructionBean != null) {

            LOGGER.trace("Preparing construction '{}' in {}", constructionBean.getDescription(), segment.source);

            Construction<AH, EvaluatedConstructionImpl<AH>> construction = new Construction<>(constructionBean, segment.source);
            // We have to clone here as the path is constantly changing during evaluation
            construction.setAssignmentPath(ctx.assignmentPath.clone());
            construction.setFocusOdoAbsolute(ctx.ae.focusOdoAbsolute); // TODO
            construction.setLensContext(ctx.ae.lensContext);
            construction.setObjectResolver(ctx.ae.objectResolver);
            construction.setPrismContext(ctx.ae.prismContext);
            construction.setMappingFactory(ctx.ae.mappingFactory);
            construction.setMappingEvaluator(ctx.ae.mappingEvaluator);
            construction.setNow(ctx.ae.now);
            construction.setContextLoader(ctx.ae.contextLoader);
            construction.setOriginType(OriginType.ASSIGNMENTS);
            construction.setChannel(ctx.ae.channel);
            construction.setOrderOneObject(segment.varThisObject);
            construction.setValid(segment.isFullPathActive() && segment.getOverallConditionState().isNewTrue());

            // Do not evaluate the construction here. We will do it in the second pass. Just prepare everything to be evaluated.
            ctx.evalAssignment.addConstruction(construction, segment.getAbsoluteAssignmentRelativityMode()); // TODO
        }
    }

    private void collectPersonaConstruction() {
        PersonaConstructionType constructionBean = segment.assignment.getPersonaConstruction();
        if (constructionBean != null) {

            LOGGER.trace("Preparing persona construction '{}' in {}", constructionBean.getDescription(), segment.source);

            PersonaConstruction<AH> construction = new PersonaConstruction<>(constructionBean, segment.source);
            // We have to clone here as the path is constantly changing during evaluation
            construction.setAssignmentPath(ctx.assignmentPath.clone());
            construction.setFocusOdoAbsolute(ctx.ae.focusOdoAbsolute); // TODO
            construction.setLensContext(ctx.ae.lensContext);
            construction.setObjectResolver(ctx.ae.objectResolver);
            construction.setPrismContext(ctx.ae.prismContext);
            construction.setOriginType(OriginType.ASSIGNMENTS);
            construction.setChannel(ctx.ae.channel);
            construction.setValid(segment.isFullPathActive() && segment.getOverallConditionState().isNewTrue());

            ctx.evalAssignment.addPersonaConstruction(construction, segment.getAbsoluteAssignmentRelativityMode()); // TODO
        }
    }

    private void collectFocusMappings() throws SchemaException {
        MappingsType mappingsBean = segment.assignment.getFocusMappings();
        if (mappingsBean != null) {
            LOGGER.trace("Request evaluation of focus mappings '{}' in {} ({} mappings)",
                    mappingsBean.getDescription(), segment.source, mappingsBean.getMapping().size());
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);

            for (MappingType mappingBean : mappingsBean.getMapping()) {
                PlusMinusZero relativityMode = segment.getRelativeAssignmentRelativityMode(); /* TODO */
                if (relativityMode != null) {
                    AssignedFocusMappingEvaluationRequest request = new AssignedFocusMappingEvaluationRequest(mappingBean, segment.source,
                            ctx.evalAssignment, relativityMode, assignmentPathVariables, segment.sourceDescription);
                    ctx.evalAssignment.addFocusMappingEvaluationRequest(request);
                } else {
                    // This can occur because overall condition state can be "true,false->false", making relative
                    // condition state to be false->false.
                }
            }
        }
    }

    private void collectFocusPolicyRule() {
        PolicyRuleType policyRuleBean = segment.assignment.getPolicyRule();
        if (policyRuleBean != null) {
            LOGGER.trace("Collecting focus policy rule '{}' in {}", policyRuleBean.getName(), segment.source);
            ctx.evalAssignment.addFocusPolicyRule(createEvaluatedPolicyRule(policyRuleBean));
        }
    }

    private void collectTargetPolicyRule() {
        PolicyRuleType policyRuleBean = segment.assignment.getPolicyRule();
        if (policyRuleBean != null) {
            LOGGER.trace("Collecting target policy rule '{}' in {}", policyRuleBean.getName(), segment.source);

            EvaluatedPolicyRuleImpl policyRule = createEvaluatedPolicyRule(policyRuleBean);
            if (appliesDirectly(ctx.assignmentPath)) {
                ctx.evalAssignment.addThisTargetPolicyRule(policyRule);
            } else {
                ctx.evalAssignment.addOtherTargetPolicyRule(policyRule);
            }
        }
    }

    @NotNull
    private EvaluatedPolicyRuleImpl createEvaluatedPolicyRule(PolicyRuleType policyRuleBean) {
        return new EvaluatedPolicyRuleImpl(policyRuleBean.clone(), ctx.assignmentPath.clone(), ctx.evalAssignment, ctx.ae.prismContext);
    }

    private boolean appliesDirectly(AssignmentPathImpl assignmentPath) {
        assert !assignmentPath.isEmpty();
        // TODO what about deputy relation which does not increase summaryOrder?
        long zeroOrderCount = assignmentPath.getSegments().stream()
                .filter(seg -> seg.getEvaluationOrderForTarget().getSummaryOrder() == 0)
                .count();
        return zeroOrderCount == 1;
    }
}
