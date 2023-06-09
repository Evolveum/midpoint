/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PlusMinusZeroType;

/**
 * An engine that creates EvaluatedAssignment from an assignment IDI. It collects induced roles, constructions,
 * authorizations, policy rules, and so on.
 *
 * This is the main entry point to the whole "assignments" mechanism at the level of a single assignment.
 *
 * It can be called repeatedly for different assignments of the same focus.
 *
 * @author semancik
 */
public class AssignmentEvaluator<AH extends AssignmentHolderType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

    private static final String OP_EVALUATE = AssignmentEvaluator.class.getName()+".evaluate";

    // Context of use

    final LensContext<AH> lensContext;
    final ObjectDeltaObject<AH> focusOdoAbsolute;
    final ObjectDeltaObject<AH> focusOdoRelative;
    final LifecycleStateModelType focusStateModel;
    final XMLGregorianCalendar now;
    final PrismObject<SystemConfigurationType> systemConfiguration;
    /**
     * Simplified evaluation mode: evaluating only authorizations and gui config.
     * Necessary during login.
     */
    final boolean loginMode;

    // Spring beans and bean-like objects used

    final ReferenceResolver referenceResolver;
    final RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
    final PrismContext prismContext = PrismContext.get();
    final MappingFactory mappingFactory = ModelBeans.get().mappingFactory;
    final ActivationComputer activationComputer = ModelBeans.get().activationComputer;
    final MappingEvaluator mappingEvaluator = ModelBeans.get().mappingEvaluator;

    // Evaluation state

    final EvaluatedAssignmentTargetCache evaluatedAssignmentTargetCache;
    private final MemberOfEngine memberOfEngine;

    private AssignmentEvaluator(Builder<AH> builder) {
        referenceResolver = builder.referenceResolver;
        focusOdoAbsolute = builder.focusOdoAbsolute;
        focusOdoRelative = builder.focusOdoRelative;
        lensContext = builder.lensContext;
        now = builder.now;
        loginMode = builder.loginMode;
        systemConfiguration = builder.systemConfiguration;
        evaluatedAssignmentTargetCache = new EvaluatedAssignmentTargetCache();
        memberOfEngine = new MemberOfEngine();

        LensFocusContext<AH> focusContext = lensContext.getFocusContext();
        if (focusContext != null) {
            focusStateModel = focusContext.getLifecycleModel();
        } else {
            focusStateModel = null;
        }
    }

    public void reset(boolean alsoMemberOfInvocations) {
        evaluatedAssignmentTargetCache.reset();
        if (alsoMemberOfInvocations) {
            memberOfEngine.clearInvocations();
        }
    }

    /**
     * Main entry point: evaluates a given focus-attached (direct) assignment.
     * Returns a complex structure called {@link EvaluatedAssignmentImpl}.
     *
     * @param primaryAssignmentMode Not well defined. Do not use for new things.
     * Please see {@link EvaluationContext#primaryAssignmentMode}.
     * @param evaluateOld If true, we take the 'old' value from assignmentIdi. If false, we take the 'new' one.
     * @param source FIXME The role of this parameter is quite unclear. It looks like that it is filled-in using objectNew or objectCurrent
     *                 depending on some strange condition in AssignmentTripleEvaluator
     */
    public EvaluatedAssignmentImpl<AH> evaluate(
            ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
            PlusMinusZero primaryAssignmentMode, boolean evaluateOld, AssignmentHolderType source, String sourceDescription,
            AssignmentOrigin origin, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .addArbitraryObjectAsParam("primaryAssignmentMode", primaryAssignmentMode)
                .addParam("evaluateOld", evaluateOld)
                .addArbitraryObjectAsParam("source", source)
                .addParam("sourceDescription", sourceDescription)
                .addArbitraryObjectAsParam("origin", origin)
                .build();
        AssignmentEvaluationTraceType trace;
        if (result.isTracingNormal(AssignmentEvaluationTraceType.class)) {
            trace = new AssignmentEvaluationTraceType()
                    .assignmentOld(LensUtil.cloneResolveResource(getAssignmentBean(assignmentIdi, true), lensContext))
                    .assignmentNew(LensUtil.cloneResolveResource(getAssignmentBean(assignmentIdi, false), lensContext))
                    .primaryAssignmentMode(PlusMinusZeroType.fromValue(primaryAssignmentMode))
                    .evaluateOld(evaluateOld)
                    .textSource(source != null ? source.asPrismObject().debugDump() : "null")
                    .sourceDescription(sourceDescription);
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = new EvaluatedAssignmentImpl<>(assignmentIdi, evaluateOld, origin, prismContext);

            EvaluationContext<AH> ctx = new EvaluationContext<>(
                    evaluatedAssignment,
                    new AssignmentPathImpl(),
                    primaryAssignmentMode, evaluateOld, task, this);

            evaluatedAssignmentTargetCache.resetForNextAssignment();

            AssignmentPathSegmentImpl firstSegment = new AssignmentPathSegmentImpl.Builder()
                    .source(source)
                    .sourceDescription(sourceDescription)
                    .assignmentIdi(assignmentIdi)
                    .isAssignment()
                    .evaluateOld(evaluateOld)
                    .evaluationOrder(getInitialEvaluationOrder(evaluatedAssignment.getNormalizedRelation()))
                    .evaluationOrderForTarget(EvaluationOrderImpl.zero(relationRegistry))
                    .pathToSourceValid(true)
                    .pathToSourceConditionState(ConditionState.allTrue())
                    .direct(true)
                    .build();

            PathSegmentEvaluation<AH> firstSegmentEvaluation = new PathSegmentEvaluation<>(firstSegment, ctx, result);
            firstSegmentEvaluation.evaluate();

            setEvaluatedAssignmentValidity(ctx, firstSegment);
            setEvaluatedAssignmentTarget(firstSegmentEvaluation, firstSegmentEvaluation.ctx);

            LOGGER.trace("Assignment evaluation finished:\n{}", ctx.evalAssignment.debugDumpLazily());
            if (ctx.evalAssignment.getTarget() != null) {
                result.addContext("assignmentTargetName", PolyString.getOrig(ctx.evalAssignment.getTarget().getName()));
            }
            if (trace != null) {
                trace.setTextResult(ctx.evalAssignment.debugDump());
            }
            result.computeStatusIfUnknown();
            return ctx.evalAssignment;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        }
    }

    /**
     * Sets evaluatedAssignment.valid property (with unclear semantics) in some strange way,
     * mixing activity and condition state. TODO reconsider this.
     */
    private void setEvaluatedAssignmentValidity(EvaluationContext<AH> ctx, AssignmentPathSegmentImpl segment) {
        boolean validityValue = ctx.evalAssignment.isVirtual() ||
                segment.isAssignmentActive() && segment.getOverallConditionState().isNotAllFalse();
        ctx.evalAssignment.setValid(validityValue);
    }

    private void setEvaluatedAssignmentTarget(PathSegmentEvaluation<AH> firstSegmentEvaluation, EvaluationContext<AH> ctx) {
        assert ctx.evalAssignment.getTarget() == null;
        List<PrismObject<? extends ObjectType>> targets = firstSegmentEvaluation.getTargets();
        if (targets.size() > 1) {
            throw new UnsupportedOperationException("Multiple targets for direct focus assignment are not supported: " + firstSegmentEvaluation.segment.assignment);
        } else if (!targets.isEmpty()) {
            ctx.evalAssignment.setTarget(targets.get(0));
        }
    }

    VariablesMap getAssignmentEvaluationVariables() {
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_LOGIN_MODE, loginMode, Boolean.class);
        // e.g. AssignmentEvaluator itself, model context, etc (when needed)
        return variables;
    }

    private AssignmentType getAssignmentBean(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean old) {
        PrismContainerValue<AssignmentType> pcv = assignmentIdi.getSingleValue(old);
        return pcv != null ? pcv.asContainerable() : null;
    }

    private EvaluationOrder getInitialEvaluationOrder(QName relation) {
        return EvaluationOrderImpl.zero(relationRegistry).advance(relation);
    }

    @SuppressWarnings("unused") // Can be used from scripts
    public boolean isMemberOf(String targetOid) {
        // What version of focus should we use?
        return memberOfEngine.isMemberOf(focusOdoAbsolute.getNewObject(), targetOid);
    }

    public boolean isMemberOfInvocationResultChanged(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple) {
        return memberOfEngine.isMemberOfInvocationResultChanged(evaluatedAssignmentTriple);
    }

    public static final class Builder<AH extends AssignmentHolderType> {
        private ReferenceResolver referenceResolver;
        private ObjectDeltaObject<AH> focusOdoAbsolute;
        private ObjectDeltaObject<AH> focusOdoRelative;
        private LensContext<AH> lensContext;
        private XMLGregorianCalendar now;
        private boolean loginMode = false;
        private PrismObject<SystemConfigurationType> systemConfiguration;

        public Builder() {
        }

        public Builder<AH> referenceResolver(ReferenceResolver val) {
            referenceResolver = val;
            return this;
        }

        public Builder<AH> focusOdo(ObjectDeltaObject<AH> val) {
            focusOdoAbsolute = val;
            focusOdoRelative = val;
            return this;
        }

        public Builder<AH> focusOdoAbsolute(ObjectDeltaObject<AH> val) {
            focusOdoAbsolute = val;
            return this;
        }

        public Builder<AH> focusOdoRelative(ObjectDeltaObject<AH> val) {
            focusOdoRelative = val;
            return this;
        }

        public Builder<AH> lensContext(LensContext<AH> val) {
            lensContext = val;
            return this;
        }

        public Builder<AH> now(XMLGregorianCalendar val) {
            now = val;
            return this;
        }

        public Builder<AH> loginMode(boolean val) {
            loginMode = val;
            return this;
        }

        public Builder<AH> systemConfiguration(PrismObject<SystemConfigurationType> val) {
            systemConfiguration = val;
            return this;
        }

        public AssignmentEvaluator<AH> build() {
            return new AssignmentEvaluator<>(this);
        }
    }
}
