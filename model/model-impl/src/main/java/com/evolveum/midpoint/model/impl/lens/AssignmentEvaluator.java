/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentOrigin;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AssignedFocusMappingEvaluationRequest;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.PlusMinusZeroType;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An engine that creates EvaluatedAssignment from an assignment IDI. It collects induced roles, constructions,
 * authorizations, policy rules, and so on.
 *
 * @author semancik
 */
public class AssignmentEvaluator<AH extends AssignmentHolderType> {

    private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

    private static final String OP_EVALUATE = AssignmentEvaluator.class.getName()+".evaluate";
    private static final String OP_EVALUATE_FROM_SEGMENT = AssignmentEvaluator.class.getName()+".evaluateFromSegment";

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

    // "Configuration parameters"
    private final RepositoryService repository;
    private final ObjectDeltaObject<AH> focusOdo;
    private final LensContext<AH> lensContext;
    private final String channel;
    private final ObjectResolver objectResolver;
    private final SystemObjectCache systemObjectCache;
    private final RelationRegistry relationRegistry;
    private final PrismContext prismContext;
    private final MappingFactory mappingFactory;
    private final ActivationComputer activationComputer;
    private final XMLGregorianCalendar now;
    private final boolean loginMode;        // restricted mode, evaluating only authorizations and gui config (TODO name)
    private final PrismObject<SystemConfigurationType> systemConfiguration;
    private final MappingEvaluator mappingEvaluator;
    private final EvaluatedAssignmentTargetCache evaluatedAssignmentTargetCache;
    private final LifecycleStateModelType focusStateModel;

    // Evaluation state
    private final List<MemberOfInvocation> memberOfInvocations = new ArrayList<>();         // experimental

    private AssignmentEvaluator(Builder<AH> builder) {
        repository = builder.repository;
        focusOdo = builder.focusOdo;
        lensContext = builder.lensContext;
        channel = builder.channel;
        objectResolver = builder.objectResolver;
        systemObjectCache = builder.systemObjectCache;
        relationRegistry = builder.relationRegistry;
        prismContext = builder.prismContext;
        mappingFactory = builder.mappingFactory;
        activationComputer = builder.activationComputer;
        now = builder.now;
        loginMode = builder.loginMode;
        systemConfiguration = builder.systemConfiguration;
        mappingEvaluator = builder.mappingEvaluator;
        evaluatedAssignmentTargetCache = new EvaluatedAssignmentTargetCache();

        LensFocusContext<AH> focusContext = lensContext.getFocusContext();
        if (focusContext != null) {
            focusStateModel = focusContext.getLifecycleModel();
        } else {
            focusStateModel = null;
        }
    }

    public RepositoryService getRepository() {
        return repository;
    }

    @SuppressWarnings("unused")
    public ObjectDeltaObject<AH> getFocusOdo() {
        return focusOdo;
    }

    public LensContext<AH> getLensContext() {
        return lensContext;
    }

    public String getChannel() {
        return channel;
    }

    public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public SystemObjectCache getSystemObjectCache() {
        return systemObjectCache;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public MappingFactory getMappingFactory() {
        return mappingFactory;
    }

    public ActivationComputer getActivationComputer() {
        return activationComputer;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    @SuppressWarnings("unused")
    public boolean isLoginMode() {
        return loginMode;
    }

    public PrismObject<SystemConfigurationType> getSystemConfiguration() {
        return systemConfiguration;
    }

    @SuppressWarnings("unused")
    public MappingEvaluator getMappingEvaluator() {
        return mappingEvaluator;
    }

    public void reset(boolean alsoMemberOfInvocations) {
        evaluatedAssignmentTargetCache.reset();
        if (alsoMemberOfInvocations) {
            memberOfInvocations.clear();
        }
    }

    // This is to reduce the number of parameters passed between methods in this class.
    // Moreover, it highlights the fact that identity of objects referenced here is fixed for any invocation of the evaluate() method.
    // (There is single EvaluationContext instance for any call to evaluate().)
    private class EvaluationContext {
        @NotNull private final EvaluatedAssignmentImpl<AH> evalAssignment;
        @NotNull private final AssignmentPathImpl assignmentPath;
        // The primary assignment mode tells whether the primary assignment was added, removed or it is unchanged.
        // The primary assignment is the first assignment in the assignment path, the assignment that is located in the
        // focal object.
        private final PlusMinusZero primaryAssignmentMode;
        private final boolean evaluateOld;
        private final Task task;
        private EvaluationContext(@NotNull EvaluatedAssignmentImpl<AH> evalAssignment,
                @NotNull AssignmentPathImpl assignmentPath,
                PlusMinusZero primaryAssignmentMode, boolean evaluateOld, Task task) {
            this.evalAssignment = evalAssignment;
            this.assignmentPath = assignmentPath;
            this.primaryAssignmentMode = primaryAssignmentMode;
            this.evaluateOld = evaluateOld;
            this.task = task;
        }
    }

    /**
     * evaluateOld: If true, we take the 'old' value from assignmentIdi. If false, we take the 'new' one.
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
            trace = new AssignmentEvaluationTraceType(prismContext)
                    .assignmentOld(CloneUtil.clone(getAssignmentBean(assignmentIdi, true)))
                    .assignmentNew(CloneUtil.clone(getAssignmentBean(assignmentIdi, false)))
                    .primaryAssignmentMode(PlusMinusZeroType.fromValue(primaryAssignmentMode))
                    .evaluateOld(evaluateOld)
                    .textSource(source != null ? source.asPrismObject().debugDump() : "null")
                    .sourceDescription(sourceDescription);
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            assertSourceNotNull(source, assignmentIdi);

            EvaluatedAssignmentImpl<AH> evalAssignmentImpl = new EvaluatedAssignmentImpl<>(assignmentIdi, evaluateOld, origin, prismContext);

            EvaluationContext ctx = new EvaluationContext(
                    evalAssignmentImpl,
                    new AssignmentPathImpl(prismContext),
                    primaryAssignmentMode, evaluateOld, task);

            evaluatedAssignmentTargetCache.resetForNextAssignment();

            AssignmentPathSegmentImpl segment = new AssignmentPathSegmentImpl(source, sourceDescription, assignmentIdi, true,
                    evaluateOld, relationRegistry, prismContext);
            segment.setEvaluationOrder(getInitialEvaluationOrder(assignmentIdi, ctx));
            segment.setEvaluationOrderForTarget(EvaluationOrderImpl.zero(relationRegistry));
            segment.setValidityOverride(true);
            segment.setPathToSourceValid(true);
            segment.setProcessMembership(true);
            segment.setRelation(getRelation(getAssignmentType(segment, ctx)));

            evaluateFromSegment(segment, PlusMinusZero.ZERO, ctx, result);

            if (segment.getTarget() != null) {
                result.addContext("assignmentTargetName", PolyString.getOrig(segment.getTarget().getName()));
            }

            LOGGER.trace("Assignment evaluation finished:\n{}", ctx.evalAssignment.debugDumpLazily());
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

    private AssignmentType getAssignmentBean(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean old) {
        PrismContainerValue<AssignmentType> pcv = assignmentIdi.getSingleValue(old);
        return pcv != null ? pcv.asContainerable() : null;
    }

    private EvaluationOrder getInitialEvaluationOrder(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            EvaluationContext ctx) {
        AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, ctx.evaluateOld);
        return EvaluationOrderImpl.zero(relationRegistry).advance(getRelation(assignmentType));
    }

    /**
     * @param relativeMode
     *
     * Where to put constructions and target roles/orgs/services (PLUS/MINUS/ZERO/null; null means "nowhere").
     * This is a mode relative to the primary assignment. It does NOT tell whether the assignment as a whole
     * is added or removed. It tells whether the part of the assignment that we are processing is to be
     * added or removed. This may happen, e.g. if a condition in an existing assignment turns from false to true.
     * In that case the primary assignment mode is ZERO, but the relative mode is PLUS.
     * The relative mode always starts at ZERO, even for added or removed assignments.
     *
     * This depends on the status of conditions. E.g. if condition evaluates 'false -> true' (i.e. in old
     * state the value is false, and in new state the value is true), then the mode is PLUS.
     *
     * This "triples algebra" is based on the following two methods:
     *
     * @see ExpressionUtil#computeConditionResultMode(boolean, boolean) - Based on condition values "old+new" determines
     * into what set (PLUS/MINUS/ZERO/none) should the result be placed. Irrespective of what is the current mode. So,
     * in order to determine "real" place where to put it (i.e. the new mode) the following method is used.
     *
     * @see PlusMinusZero#compute(PlusMinusZero, PlusMinusZero) - Takes original mode and the mode from recent condition
     * and determines the new mode (commutatively):
     *
     * PLUS + PLUS/ZERO = PLUS
     * MINUS + MINUS/ZERO = MINUS
     * ZERO + ZERO = ZERO
     * PLUS + MINUS = none
     *
     * This is quite straightforward, although the last rule deserves a note. If we have an assignment that was originally
     * disabled and becomes enabled by the current delta (i.e. PLUS), and that assignment contains an inducement that was originally
     * enabled and becomes disabled (i.e. MINUS), the result is that the (e.g.) constructions within the inducement were not
     * present in the old state (because assignment was disabled) and are not present in the new state (because inducement is disabled).
     *
     * Note: this parameter could be perhaps renamed to "tripleMode" or "destination" or something like that.
     */
    private void evaluateFromSegment(AssignmentPathSegmentImpl segment, PlusMinusZero relativeMode, EvaluationContext ctx,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.subresult(OP_EVALUATE_FROM_SEGMENT)
                .setMinor()
                .addParam("segment", segment.shortDump())
                .addArbitraryObjectAsParam("relativeMode", relativeMode)
                .build();
        AssignmentSegmentEvaluationTraceType trace;
        if (result.isTracingNormal(AssignmentSegmentEvaluationTraceType.class)) {
            trace = new AssignmentSegmentEvaluationTraceType(prismContext)
                    .segment(segment.toAssignmentPathSegmentType(true))
                    .mode(PlusMinusZeroType.fromValue(relativeMode));
            result.addTrace(trace);
        } else {
            trace = null;
        }
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("*** Evaluate from segment: {}", segment);
                LOGGER.trace("*** Evaluation order - standard:   {}, matching: {}", segment.getEvaluationOrder(), segment.isMatchingOrder());
                LOGGER.trace("*** Evaluation order - for target: {}, matching: {}", segment.getEvaluationOrderForTarget(), segment.isMatchingOrderForTarget());
                LOGGER.trace("*** mode: {}, process membership: {}", relativeMode, segment.isProcessMembership());
                LOGGER.trace("*** path to source valid: {}, validity override: {}", segment.isPathToSourceValid(), segment.isValidityOverride());
            }

            assertSourceNotNull(segment.source, ctx.evalAssignment);
            checkSchema(segment, ctx);

            ctx.assignmentPath.add(segment);
            LOGGER.trace("*** Path (with current segment already added):\n{}", ctx.assignmentPath.debugDumpLazily());

            boolean evaluateContent;
            AssignmentType assignmentType = getAssignmentType(segment, ctx);
            MappingType assignmentCondition = assignmentType.getCondition();
            if (assignmentCondition != null) {
                AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
                PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(assignmentCondition,
                        segment.source, assignmentPathVariables,
                        "condition in assignment in " + segment.getSourceDescription(), ctx, result);
                boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
                boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
                PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
                if (modeFromCondition == null) {
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Skipping evaluation of {} because of condition result ({} -> {}: {})",
                                FocusTypeUtil.dumpAssignment(assignmentType), condOld, condNew, null);
                    }
                    evaluateContent = false;
                } else {
                    PlusMinusZero origMode = relativeMode;
                    relativeMode = PlusMinusZero.compute(relativeMode, modeFromCondition);
                    LOGGER.trace("Evaluated condition in assignment {} -> {}: {} + {} = {}", condOld, condNew, origMode,
                            modeFromCondition, relativeMode);
                    evaluateContent = true;
                }
            } else {
                evaluateContent = true;
            }

            if (ctx.assignmentPath.isEmpty() && ctx.evalAssignment.isVirtual()) {
                segment.setValidityOverride(ctx.evalAssignment.isVirtual());
            }

            boolean isValid =
                    (evaluateContent && evaluateSegmentContent(segment, relativeMode, ctx, result)) || ctx.evalAssignment.isVirtual();

            ctx.assignmentPath.removeLast(segment);
            if (ctx.assignmentPath.isEmpty()) {        // direct assignment
                ctx.evalAssignment.setValid(isValid);
            }

            LOGGER.trace("evalAssignment isVirtual {} ", ctx.evalAssignment.isVirtual());
            if (segment.getSource() != null) {
                result.addContext("segmentSourceName", PolyString.getOrig(segment.getSource().getName()));
            }
            if (segment.getTarget() != null) {
                result.addContext("segmentTargetName", PolyString.getOrig(segment.getTarget().getName()));
            }
            result.addArbitraryObjectAsReturn("relativeMode", relativeMode);
            result.addReturn("evaluateContent", evaluateContent);
            result.addReturn("isValid", isValid);
            if (trace != null) {
                trace.setTextResult(segment.debugDump());
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // "content" means "payload + targets" here
    private <O extends ObjectType> boolean evaluateSegmentContent(AssignmentPathSegmentImpl segment,
            PlusMinusZero relativeMode, EvaluationContext ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        assert ctx.assignmentPath.last() == segment;

        final boolean isDirectAssignment = ctx.assignmentPath.size() == 1;

        AssignmentType assignment = getAssignmentType(segment, ctx);

        // Assignment validity is checked with respect to the assignment source, not to the focus object.
        // So, if (e.g.) focus is in "draft" state, only validity of direct assignments should be influenced by this fact.
        // Other assignments (e.g. from roles to metaroles) should be considered valid, provided these roles are
        // in active lifecycle states. See also MID-6114.
        AssignmentHolderType source = segment.isMatchingOrder() ? focusOdo.getNewObject().asObjectable() : segment.getSource();
        boolean isAssignmentValid = LensUtil.isAssignmentValid(source, assignment, now, activationComputer, focusStateModel);
        if (isAssignmentValid || segment.isValidityOverride()) {
            // Note: validityOverride is currently the same as "isDirectAssignment" - which is very probably OK.
            // Direct assignments are visited even if they are not valid (i.e. effectively disabled).
            // It is because we need to collect e.g. assignment policy rules for them.
            // Also because we could have deltas that disable/enable these assignments.
            boolean reallyValid = segment.isPathToSourceValid() && isAssignmentValid;
            if (!loginMode && segment.isMatchingOrder()) {
                if (assignment.getConstruction() != null) {
                    collectConstruction(segment, relativeMode, reallyValid, ctx);
                }
                if (assignment.getPersonaConstruction() != null) {
                    collectPersonaConstruction(segment, relativeMode, reallyValid, ctx);
                }
                if (assignment.getFocusMappings() != null) {
                    if (reallyValid && relativeMode != null) {     // null relative mode means both PLUS and MINUS
                        collectFocusMappings(segment, relativeMode, ctx);
                    }
                }
            }
            if (!loginMode && assignment.getPolicyRule() != null) {
                // Here we ignore "reallyValid". It is OK, because reallyValid can be false here only when
                // evaluating direct assignments; and invalid ones are marked as such via EvaluatedAssignment.isValid.
                // TODO is it ok?
                if (isNonNegative(relativeMode)) {
                    if (segment.isMatchingOrder()) {
                        collectPolicyRule(true, segment, ctx);
                    }
                    if (segment.isMatchingOrderForTarget()) {
                        collectPolicyRule(false, segment, ctx);
                    }
                }
            }
            if (assignment.getTargetRef() != null) {
                QName relation = getRelation(assignment);
                if (loginMode && !relationRegistry.isProcessedOnLogin(relation)) {
                    LOGGER.trace("Skipping processing of assignment target {} because relation {} is configured for login skip", assignment.getTargetRef().getOid(), relation);
                    // Skip - to optimize logging-in, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
                    // We want to make this configurable in the future MID-3581
                } else if (!loginMode && !isChanged(ctx.primaryAssignmentMode) &&
                        !relationRegistry.isProcessedOnRecompute(relation) && !shouldEvaluateAllAssignmentRelationsOnRecompute()) {
                    LOGGER.debug("Skipping processing of assignment target {} because relation {} is configured for recompute skip (mode={})", assignment.getTargetRef().getOid(), relation, relativeMode);
                    // Skip - to optimize recompute, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
                    // never skip this if assignment has changed. We want to process this, e.g. to enforce min/max assignee rules
                    // We want to make this configurable in the future MID-3581

                    // Important: but we still want this to be reflected in roleMembershipRef
                    if ((isNonNegative(relativeMode)) && segment.isProcessMembership()) {
                        if (assignment.getTargetRef().getOid() != null) {
                            addToMembershipLists(assignment.getTargetRef(), relation, ctx);
                        } else {
                            // no OID, so we have to resolve the filter
                            for (PrismObject<ObjectType> targetObject : getTargets(segment, ctx, result)) {
                                ObjectType target = targetObject.asObjectable();
                                if (target instanceof FocusType) {
                                    addToMembershipLists((FocusType) target, relation, ctx);
                                }
                            }
                        }
                    }
                } else {
                    List<PrismObject<O>> targets = getTargets(segment, ctx, result);
                    LOGGER.trace("Targets in {}, assignment ID {}: {}", segment.source, assignment.getId(), targets);
                    if (isDirectAssignment) {
                        setEvaluatedAssignmentTarget(segment, targets, ctx);
                    }
                    for (PrismObject<O> target : targets) {
                        if (hasCycle(segment, target, ctx)) {
                            continue;
                        }
                        if (isDelegationToNonDelegableTarget(assignment, target, ctx)) {
                            continue;
                        }
                        evaluateSegmentTarget(segment, relativeMode, reallyValid, (AssignmentHolderType) target.asObjectable(), relation, ctx, result);
                    }
                }
            }
        } else {
            LOGGER.trace("Skipping evaluation of assignment {} because it is not valid", assignment);
        }
        return isAssignmentValid;
    }

    private boolean shouldEvaluateAllAssignmentRelationsOnRecompute() {
        return ModelExecuteOptions.isEvaluateAllAssignmentRelationsOnRecompute(lensContext.getOptions());
    }

    private <O extends ObjectType> boolean isDelegationToNonDelegableTarget(AssignmentType assignmentType, @NotNull PrismObject<O> target,
            EvaluationContext ctx) {
        AssignmentPathSegment previousSegment = ctx.assignmentPath.beforeLast(1);
        if (previousSegment == null || !previousSegment.isDelegation() || !target.canRepresent(AbstractRoleType.class)) {
            return false;
        }
        if (!Boolean.TRUE.equals(((AbstractRoleType)target.asObjectable()).isDelegable())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping evaluation of {} because it delegates to a non-delegable target {}",
                        FocusTypeUtil.dumpAssignment(assignmentType), target);
            }
            return true;
        } else {
            return false;
        }
    }

    // number of times any given target is allowed to occur in the assignment path
    private static final int MAX_TARGET_OCCURRENCES = 2;

    private <O extends ObjectType> boolean hasCycle(AssignmentPathSegmentImpl segment, @NotNull PrismObject<O> target,
            EvaluationContext ctx) throws PolicyViolationException {
        // TODO reconsider this
        if (target.getOid().equals(segment.source.getOid())) {
            throw new PolicyViolationException("The "+segment.source+" refers to itself in assignment/inducement");
        }
        // removed condition "&& segment.getEvaluationOrder().equals(ctx.assignmentPath.getEvaluationOrder())"
        // as currently it is always true
        // TODO reconsider this
        int count = ctx.assignmentPath.countTargetOccurrences(target.asObjectable());
        if (count >= MAX_TARGET_OCCURRENCES) {
            LOGGER.debug("Max # of target occurrences ({}) detected for target {} in {} - stopping evaluation here",
                    MAX_TARGET_OCCURRENCES, ObjectTypeUtil.toShortString(target), ctx.assignmentPath);
            return true;
        } else {
            return false;
        }
    }

    private void collectConstruction(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx) {
        assertSourceNotNull(segment.source, ctx.evalAssignment);

        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        ConstructionType constructionType = assignmentType.getConstruction();

        LOGGER.trace("Preparing construction '{}' in {}", constructionType.getDescription(), segment.source);

        Construction<AH> construction = new Construction<>(constructionType, segment.source);
        // We have to clone here as the path is constantly changing during evaluation
        construction.setAssignmentPath(ctx.assignmentPath.clone());
        construction.setFocusOdo(focusOdo);
        construction.setLensContext(lensContext);
        construction.setObjectResolver(objectResolver);
        construction.setPrismContext(prismContext);
        construction.setMappingFactory(mappingFactory);
        construction.setMappingEvaluator(mappingEvaluator);
        construction.setOriginType(OriginType.ASSIGNMENTS);
        construction.setChannel(channel);
        construction.setOrderOneObject(segment.getOrderOneObject());
        construction.setValid(isValid);
        construction.setRelativityMode(mode);

        // Do not evaluate the construction here. We will do it in the second pass. Just prepare everything to be evaluated.
        if (mode == null) {
            return;                // null mode (i.e. plus + minus) means 'ignore the payload'
        }
        ctx.evalAssignment.addConstruction(construction, mode);
    }

    private void collectPersonaConstruction(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx) {
        assertSourceNotNull(segment.source, ctx.evalAssignment);
        if (mode == null) {
            return;                // null mode (i.e. plus + minus) means 'ignore the payload'
        }

        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        PersonaConstructionType constructionType = assignmentType.getPersonaConstruction();

        LOGGER.trace("Preparing persona construction '{}' in {}", constructionType.getDescription(), segment.source);

        PersonaConstruction<AH> construction = new PersonaConstruction<>(constructionType, segment.source);
        // We have to clone here as the path is constantly changing during evaluation
        construction.setAssignmentPath(ctx.assignmentPath.clone());
        construction.setFocusOdo(focusOdo);
        construction.setLensContext(lensContext);
        construction.setObjectResolver(objectResolver);
        construction.setPrismContext(prismContext);
        construction.setOriginType(OriginType.ASSIGNMENTS);
        construction.setChannel(channel);
        construction.setValid(isValid);
        construction.setRelativityMode(mode);

        ctx.evalAssignment.addPersonaConstruction(construction, mode);
    }

    private void collectFocusMappings(AssignmentPathSegmentImpl segment, @NotNull PlusMinusZero relativeMode, EvaluationContext ctx)
            throws SchemaException {
        assertSourceNotNull(segment.source, ctx.evalAssignment);

        AssignmentType assignmentBean = getAssignmentType(segment, ctx);
        MappingsType mappingsBean = assignmentBean.getFocusMappings();

        LOGGER.trace("Request evaluation of focus mappings '{}' in {} ({} mappings)",
                mappingsBean.getDescription(), segment.source, mappingsBean.getMapping().size());
        AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);

        for (MappingType mappingBean: mappingsBean.getMapping()) {
            AssignedFocusMappingEvaluationRequest request = new AssignedFocusMappingEvaluationRequest(mappingBean, segment.source,
                    ctx.evalAssignment, relativeMode, assignmentPathVariables, segment.sourceDescription);
            ctx.evalAssignment.addFocusMappingEvaluationRequest(request);
        }
    }

    private void collectPolicyRule(boolean focusRule, AssignmentPathSegmentImpl segment, EvaluationContext ctx) {
        assertSourceNotNull(segment.source, ctx.evalAssignment);

        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        PolicyRuleType policyRuleType = assignmentType.getPolicyRule();

        LOGGER.trace("Collecting {} policy rule '{}' in {}", focusRule ? "focus" : "target", policyRuleType.getName(), segment.source);

        EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(policyRuleType.clone(), ctx.assignmentPath.clone(), prismContext);

        if (focusRule) {
            ctx.evalAssignment.addFocusPolicyRule(policyRule);
        } else {
            if (appliesDirectly(ctx.assignmentPath)) {
                ctx.evalAssignment.addThisTargetPolicyRule(policyRule);
            } else {
                ctx.evalAssignment.addOtherTargetPolicyRule(policyRule);
            }
        }
    }

    private boolean appliesDirectly(AssignmentPathImpl assignmentPath) {
        assert !assignmentPath.isEmpty();
        // TODO what about deputy relation which does not increase summaryOrder?
        long zeroOrderCount = assignmentPath.getSegments().stream()
                .filter(seg -> seg.getEvaluationOrderForTarget().getSummaryOrder() == 0)
                .count();
        return zeroOrderCount == 1;
    }

    @NotNull
    private <O extends ObjectType> List<PrismObject<O>> getTargets(AssignmentPathSegmentImpl segment, EvaluationContext ctx,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        if (assignmentType.getTargetRef() != null) {
            try {
                return resolveTargets(segment, ctx, result);
            } catch (ObjectNotFoundException ex) {
                // Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
                // an exception would prohibit any operations with the users that have the role, including removal of the reference.
                // The failure is recorded in the result and we will log it. It should be enough.
                LOGGER.error(ex.getMessage()+" in assignment target reference in "+segment.sourceDescription,ex);
                // For OrgType references we trigger the reconciliation (see MID-2242)
                ctx.evalAssignment.setForceRecon(true);
                return Collections.emptyList();
            }
        } else {
            throw new IllegalStateException("Both target and targetRef are null. We should not be here. Assignment: " + assignmentType);
        }
    }

    @NotNull
    private <O extends ObjectType> List<PrismObject<O>> resolveTargets(AssignmentPathSegmentImpl segment, EvaluationContext ctx,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        String oid = targetRef.getOid();

        // Target is referenced, need to fetch it
        Class<O> targetClass;
        if (targetRef.getType() != null) {
            targetClass = prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
            if (targetClass == null) {
                throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignmentType + " in " + segment.sourceDescription);
            }
        } else {
            throw new SchemaException("Missing type in target reference in " + assignmentType + " in " + segment.sourceDescription);
        }

        if (oid == null) {
            LOGGER.trace("Resolving dynamic target ref");
            if (targetRef.getFilter() == null) {
                throw new SchemaException("The OID and filter are both null in assignment targetRef in "+segment.source);
            }
            return resolveTargetsFromFilter(targetClass, targetRef.getFilter(), segment, ctx, result);
        } else {
            LOGGER.trace("Resolving target {}:{} from repository", targetClass.getSimpleName(), oid);
            PrismObject<O> target;
            try {
                target = repository.getObject(targetClass, oid, null, result);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + segment.sourceDescription, e);
            }
            // Not handling object not found exception here. Caller will handle that.
            if (target == null) {
                throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+targetClass+" (should not happen, probably a bug) in "+segment.sourceDescription);
            }
            return Collections.singletonList(target);
        }
    }

    @NotNull
    private <O extends ObjectType> List<PrismObject<O>> resolveTargetsFromFilter(Class<O> targetClass,
            SearchFilterType filter, AssignmentPathSegmentImpl segment,
            EvaluationContext ctx, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException{
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(lensContext, null, ctx.task, result));
        try {
            PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
            ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(segment.source, null, null, systemConfiguration.asObjectable(), prismContext);
            variables.put(ExpressionConstants.VAR_SOURCE, segment.getOrderOneObject(), ObjectType.class);
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
            if (assignmentPathVariables != null) {
                ModelImplUtils.addAssignmentPathVariables(assignmentPathVariables, variables, getPrismContext());
            }
            variables.addVariableDefinitions(getAssignmentEvaluationVariables());
            ObjectFilter origFilter = prismContext.getQueryConverter().parseFilter(filter, targetClass);
            // TODO: expression profile should be determined from the holding object archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(origFilter, variables, expressionProfile, getMappingFactory().getExpressionFactory(), prismContext, " evaluating resource filter expression ", ctx.task, result);
            if (evaluatedFilter == null) {
                throw new SchemaException("The OID is null and filter could not be evaluated in assignment targetRef in "+segment.source);
            }

            return repository.searchObjects(targetClass, prismContext.queryFactory().createQuery(evaluatedFilter), null, result);
            // we don't check for no targets here; as we don't care for referential integrity
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private ExpressionVariables getAssignmentEvaluationVariables() {
        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_LOGIN_MODE, loginMode, Boolean.class);
        // e.g. AssignmentEvaluator itself, model context, etc (when needed)
        return variables;
    }

    // Note: This method must be single-return after targetEvaluationInformation is established.
    private void evaluateSegmentTarget(AssignmentPathSegmentImpl segment, PlusMinusZero relativeMode, boolean isAssignmentPathValid,
            AssignmentHolderType target, QName relation, EvaluationContext ctx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
        assertSourceNotNull(segment.source, ctx.evalAssignment);

        assert ctx.assignmentPath.last() == segment;

        segment.setTarget(target);
        segment.setRelation(relation);            // probably not needed

        if (evaluatedAssignmentTargetCache.canSkip(segment, ctx.primaryAssignmentMode)) {
            LOGGER.trace("Skipping evaluation of segment {} because it is idempotent and we have seen the target before", segment);
            InternalMonitor.recordRoleEvaluationSkip(target, true);
            return;
        }

        LOGGER.trace("Evaluating segment TARGET:\n{}", segment.debugDumpLazily(1));

        checkRelationWithTarget(segment, target, relation);

        LifecycleStateModelType targetStateModel = ArchetypeManager.determineLifecycleModel(target.asPrismObject(), systemConfiguration);
        boolean isTargetValid = LensUtil.isFocusValid(target, now, activationComputer, targetStateModel);
        boolean isPathAndTargetValid = isAssignmentPathValid && isTargetValid;

        LOGGER.debug("Evaluating RBAC [{}]", ctx.assignmentPath.shortDumpLazily());
        InternalMonitor.recordRoleEvaluation(target, true);

        AssignmentTargetEvaluationInformation targetEvaluationInformation;
        if (isPathAndTargetValid) {
            // Cache it immediately, even before evaluation. So if there is a cycle in the role path
            // then we can detect it and skip re-evaluation of aggressively idempotent roles.
            //
            // !!! Ensure we will not return without updating this object (except for exceptions). So please keep this
            //     method a single-return one after this point. We did not want to complicate things using try...finally.
            //
            targetEvaluationInformation = evaluatedAssignmentTargetCache.recordProcessing(segment, ctx.primaryAssignmentMode);
        } else {
            targetEvaluationInformation = null;
        }
        int targetPolicyRulesOnEntry = ctx.evalAssignment.getAllTargetsPolicyRulesCount();

        boolean skipOnConditionResult = false;

        if (isTargetValid && target instanceof AbstractRoleType) {
            MappingType roleCondition = ((AbstractRoleType)target).getCondition();
            if (roleCondition != null) {
                AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
                PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(roleCondition,
                        segment.source, assignmentPathVariables,
                        "condition in " + segment.getTargetDescription(), ctx, result);
                boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
                boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
                PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
                if (modeFromCondition == null) {
                    skipOnConditionResult = true;
                    LOGGER.trace("Skipping evaluation of {} because of condition result ({} -> {}: null)",
                            target, condOld, condNew);
                } else {
                    PlusMinusZero origMode = relativeMode;
                    relativeMode = PlusMinusZero.compute(relativeMode, modeFromCondition);
                    LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", target, condOld, condNew,
                            origMode, modeFromCondition, relativeMode);
                }
            }
        }

        if (!skipOnConditionResult) {
            EvaluatedAssignmentTargetImpl evalAssignmentTarget = new EvaluatedAssignmentTargetImpl(
                    target.asPrismObject(),
                    segment.isMatchingOrder(),    // evaluateConstructions: exact meaning of this is to be revised
                    ctx.assignmentPath.clone(),
                    getAssignmentType(segment, ctx),
                    isPathAndTargetValid);
            ctx.evalAssignment.addRole(evalAssignmentTarget, relativeMode);

            // we need to evaluate assignments also for disabled targets, because of target policy rules
            // ... but only for direct ones!
            if (isTargetValid || ctx.assignmentPath.size() == 1) {
                for (AssignmentType nextAssignment : target.getAssignment()) {
                    evaluateAssignment(segment, relativeMode, isPathAndTargetValid, ctx, target, relation, nextAssignment, result);
                }
            }

            // we need to collect membership also for disabled targets (provided the assignment itself is enabled): MID-4127
            if (isNonNegative(relativeMode) && segment.isProcessMembership()) {
                addToMembershipLists(target, relation, ctx);
            }

            if (isTargetValid) {
                if (isNonNegative(relativeMode)) {
                    setAsTenantRef(target, ctx);
                }

                // We continue evaluation even if the relation is non-membership and non-delegation.
                // Computation of isMatchingOrder will ensure that we won't collect any unwanted content.

                if (target instanceof AbstractRoleType) {
                    for (AssignmentType roleInducement : ((AbstractRoleType) target).getInducement()) {
                        evaluateInducement(segment, relativeMode, isPathAndTargetValid, ctx, target, roleInducement, result);
                    }
                }

                if (segment.isMatchingOrder() && target instanceof AbstractRoleType && isNonNegative(relativeMode)) {
                    for (AuthorizationType authorizationType : ((AbstractRoleType) target).getAuthorization()) {
                        Authorization authorization = createAuthorization(authorizationType, target.toString());
                        if (!ctx.evalAssignment.getAuthorizations().contains(authorization)) {
                            ctx.evalAssignment.addAuthorization(authorization);
                        }
                    }
                    AdminGuiConfigurationType adminGuiConfiguration = ((AbstractRoleType) target).getAdminGuiConfiguration();
                    if (adminGuiConfiguration != null && !ctx.evalAssignment.getAdminGuiConfigurations()
                            .contains(adminGuiConfiguration)) {
                        ctx.evalAssignment.addAdminGuiConfiguration(adminGuiConfiguration);
                    }
                }
            }
        }
        int targetPolicyRulesOnExit = ctx.evalAssignment.getAllTargetsPolicyRulesCount();

        LOGGER.trace("Evaluating segment target DONE for {}; target policy rules: {} -> {}", segment, targetPolicyRulesOnEntry,
                targetPolicyRulesOnExit);
        if (targetEvaluationInformation != null) {
            targetEvaluationInformation.setBringsTargetPolicyRules(targetPolicyRulesOnExit > targetPolicyRulesOnEntry);
        }
    }

    // TODO revisit this
    private ObjectType getOrderOneObject(AssignmentPathSegmentImpl segment) {
        EvaluationOrder evaluationOrder = segment.getEvaluationOrder();
        if (evaluationOrder.getSummaryOrder() == 1) {
            return segment.getTarget();
        } else {
            if (segment.getSource() != null) {        // should be always the case...
                return segment.getSource();
            } else {
                return segment.getTarget();
            }
        }
    }

    private void evaluateAssignment(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx,
            AssignmentHolderType target, QName relation, AssignmentType nextAssignment, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        ObjectType orderOneObject = getOrderOneObject(segment);

        if (relationRegistry.isDelegation(relation)) {
            // We have to handle assignments as though they were inducements here.
            if (!isAllowedByLimitations(segment, nextAssignment, ctx)) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Skipping application of delegated assignment {} because it is limited in the delegation",
                            FocusTypeUtil.dumpAssignment(nextAssignment));
                }
                return;
            }
        }
        QName nextRelation = getRelation(nextAssignment);
        EvaluationOrder nextEvaluationOrder = segment.getEvaluationOrder().advance(nextRelation);
        EvaluationOrder nextEvaluationOrderForTarget = segment.getEvaluationOrderForTarget().advance(nextRelation);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("orig EO({}): follow assignment {} {}; new EO({})",
                    segment.getEvaluationOrder().shortDump(), target, FocusTypeUtil.dumpAssignment(nextAssignment), nextEvaluationOrder);
        }
        String nextSourceDescription = target+" in "+segment.sourceDescription;
        AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl(target, nextSourceDescription, nextAssignment, true, relationRegistry, prismContext);
        nextSegment.setRelation(nextRelation);
        nextSegment.setEvaluationOrder(nextEvaluationOrder);
        nextSegment.setEvaluationOrderForTarget(nextEvaluationOrderForTarget);
        nextSegment.setOrderOneObject(orderOneObject);
        nextSegment.setPathToSourceValid(isValid);
        /*
         * We obviously want to process membership from the segment if it's of matching order.
         *
         * But we want to do that also for targets obtained via delegations. The current (approximate) approach is to
         * collect membership from all assignments of any user that we find on the assignment path.
         *
         * TODO: does this work for invalid (effectiveStatus = disabled) assignments?
         */
        boolean isUser = target instanceof UserType;
        nextSegment.setProcessMembership(nextSegment.isMatchingOrder() || isUser);
        assert !ctx.assignmentPath.isEmpty();
        evaluateFromSegment(nextSegment, mode, ctx, result);
    }

    private void evaluateInducement(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx,
            AssignmentHolderType target, AssignmentType inducement, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

        ObjectType orderOneObject = getOrderOneObject(segment);

        if (!isInducementApplicableToFocusType(inducement.getFocusType())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping application of inducement {} because the focusType does not match (specified: {}, actual: {})",
                        FocusTypeUtil.dumpAssignment(inducement), inducement.getFocusType(), target.getClass().getSimpleName());
            }
            return;
        }
        if (!isAllowedByLimitations(segment, inducement, ctx)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping application of inducement {} because it is limited", FocusTypeUtil.dumpAssignment(inducement));
            }
            return;
        }
        String subSourceDescription = target+" in "+segment.sourceDescription;
        AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl(target, subSourceDescription, inducement, false, relationRegistry, prismContext);
        // note that 'old' and 'new' values for assignment in nextSegment are the same
        boolean nextIsMatchingOrder = AssignmentPathSegmentImpl.computeMatchingOrder(
                segment.getEvaluationOrder(), nextSegment.getAssignmentNew());
        boolean nextIsMatchingOrderForTarget = AssignmentPathSegmentImpl.computeMatchingOrder(
                segment.getEvaluationOrderForTarget(), nextSegment.getAssignmentNew());

        Holder<EvaluationOrder> nextEvaluationOrderHolder = new Holder<>(segment.getEvaluationOrder().clone());
        Holder<EvaluationOrder> nextEvaluationOrderForTargetHolder = new Holder<>(segment.getEvaluationOrderForTarget().clone());
        adjustOrder(nextEvaluationOrderHolder, nextEvaluationOrderForTargetHolder, inducement.getOrderConstraint(), inducement.getOrder(), ctx.assignmentPath, nextSegment, ctx);
        nextSegment.setEvaluationOrder(nextEvaluationOrderHolder.getValue(), nextIsMatchingOrder);
        nextSegment.setEvaluationOrderForTarget(nextEvaluationOrderForTargetHolder.getValue(), nextIsMatchingOrderForTarget);

        nextSegment.setOrderOneObject(orderOneObject);
        nextSegment.setPathToSourceValid(isValid);
        nextSegment.setProcessMembership(nextIsMatchingOrder);
        nextSegment.setRelation(getRelation(inducement));

        // Originally we executed the following only if isMatchingOrder. However, sometimes we have to look even into
        // inducements with non-matching order: for example because we need to extract target-related policy rules
        // (these are stored with order of one less than orders for focus-related policy rules).
        //
        // We need to make sure NOT to extract anything other from such inducements. That's why we set e.g.
        // processMembership attribute to false for these inducements.
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("orig EO({}): evaluate {} inducement({}) {}; new EO({})",
                    segment.getEvaluationOrder().shortDump(), target, FocusTypeUtil.dumpInducementConstraints(inducement),
                    FocusTypeUtil.dumpAssignment(inducement), nextEvaluationOrderHolder.getValue().shortDump());
        }
        assert !ctx.assignmentPath.isEmpty();
        evaluateFromSegment(nextSegment, mode, ctx, result);
    }

    private void adjustOrder(Holder<EvaluationOrder> evaluationOrderHolder, Holder<EvaluationOrder> targetEvaluationOrderHolder,
            List<OrderConstraintsType> constraints, Integer order, AssignmentPathImpl assignmentPath,
            AssignmentPathSegmentImpl nextSegment, EvaluationContext ctx) {

        if (constraints.isEmpty()) {
            if (order == null || order == 1) {
                return;
            } else if (order <= 0) {
                throw new IllegalStateException("Wrong inducement order: it must be positive but it is " + order + " instead");
            }
            // converting legacy -> new specification
            int currentOrder = evaluationOrderHolder.getValue().getSummaryOrder();
            if (order > currentOrder) {
                LOGGER.trace("order of the inducement ({}) is greater than the current evaluation order ({}), marking as undefined",
                        order, currentOrder);
                makeUndefined(evaluationOrderHolder, targetEvaluationOrderHolder);
                return;
            }
            // i.e. currentOrder >= order, i.e. currentOrder > order-1
            int newOrder = currentOrder - (order - 1);
            assert newOrder > 0;
            constraints = Collections.singletonList(new OrderConstraintsType(prismContext)
                    .order(order)
                    .resetOrder(newOrder));
        }

        OrderConstraintsType summaryConstraints = ObjectTypeUtil.getConstraintFor(constraints, null);
        Integer resetSummaryTo = summaryConstraints != null && summaryConstraints.getResetOrder() != null ?
                summaryConstraints.getResetOrder() : null;

        if (resetSummaryTo != null) {
            int summaryBackwards = evaluationOrderHolder.getValue().getSummaryOrder() - resetSummaryTo;
            if (summaryBackwards < 0) {
                // or should we throw an exception?
                LOGGER.warn("Cannot move summary order backwards to a negative value ({}). Current order: {}, requested order: {}",
                        summaryBackwards, evaluationOrderHolder.getValue().getSummaryOrder(), resetSummaryTo);
                makeUndefined(evaluationOrderHolder, targetEvaluationOrderHolder);
                return;
            } else if (summaryBackwards > 0) {
//                MultiSet<QName> backRelations = new HashMultiSet<>();
                int assignmentsSeen = 0;
                int i = assignmentPath.size()-1;
                while (assignmentsSeen < summaryBackwards) {
                    if (i < 0) {
                        LOGGER.trace("Cannot move summary order backwards by {}; only {} assignments segment seen: {}",
                                summaryBackwards, assignmentsSeen, assignmentPath);
                        makeUndefined(evaluationOrderHolder, targetEvaluationOrderHolder);
                        return;
                    }
                    AssignmentPathSegmentImpl segment = assignmentPath.getSegments().get(i);
                    if (segment.isAssignment()) {
                        if (!relationRegistry.isDelegation(segment.getRelation())) {
                            // backRelations.add(segment.getRelation());
                            assignmentsSeen++;
                            LOGGER.trace("Going back {}: relation at assignment -{} (position -{}): {}", summaryBackwards,
                                    assignmentsSeen, assignmentPath.size() - i, segment.getRelation());
                        }
                    } else {
                        AssignmentType inducement = segment.getAssignment(ctx.evaluateOld);        // for i>0 returns value regardless of evaluateOld
                        for (OrderConstraintsType constraint : inducement.getOrderConstraint()) {
                            if (constraint.getResetOrder() != null && constraint.getRelation() != null) {
                                LOGGER.debug("Going back {}: an inducement with non-summary resetting constraint found"
                                        + " in the chain (at position -{}): {} in {}", summaryBackwards, assignmentPath.size()-i,
                                        constraint, segment);
                                makeUndefined(evaluationOrderHolder, targetEvaluationOrderHolder);
                                return;
                            }
                        }
                        if (segment.getLastEqualOrderSegmentIndex() != null) {
                            i = segment.getLastEqualOrderSegmentIndex();
                            continue;
                        }
                    }
                    i--;
                }
                nextSegment.setLastEqualOrderSegmentIndex(i);
                evaluationOrderHolder.setValue(assignmentPath.getSegments().get(i).getEvaluationOrder());
                targetEvaluationOrderHolder.setValue(assignmentPath.getSegments().get(i).getEvaluationOrderForTarget());
            } else {
                // summaryBackwards is 0 - nothing to change
            }
            for (OrderConstraintsType constraint : constraints) {
                if (constraint.getRelation() != null && constraint.getResetOrder() != null) {
                    LOGGER.warn("Ignoring resetOrder (with a value of {} for {}) because summary order was already moved backwards by {} to {}: {}",
                            constraint.getResetOrder(), constraint.getRelation(), summaryBackwards,
                            evaluationOrderHolder.getValue().getSummaryOrder(), constraint);
                }
            }
        } else {
            EvaluationOrder beforeChange = evaluationOrderHolder.getValue().clone();
            for (OrderConstraintsType constraint : constraints) {
                if (constraint.getResetOrder() != null) {
                    assert constraint.getRelation() != null;        // already processed above
                    int currentOrder = evaluationOrderHolder.getValue().getMatchingRelationOrder(constraint.getRelation());
                    int newOrder = constraint.getResetOrder();
                    if (newOrder > currentOrder) {
                        LOGGER.warn("Cannot increase evaluation order for {} from {} to {}: {}", constraint.getRelation(),
                                currentOrder, newOrder, constraint);
                    } else if (newOrder < currentOrder) {
                        evaluationOrderHolder.setValue(evaluationOrderHolder.getValue().resetOrder(constraint.getRelation(), newOrder));
                        LOGGER.trace("Reset order for {} from {} to {} -> {}", constraint.getRelation(), currentOrder, newOrder, evaluationOrderHolder.getValue());
                    } else {
                        LOGGER.trace("Keeping order for {} at {} -> {}", constraint.getRelation(), currentOrder, evaluationOrderHolder.getValue());
                    }
                }
            }
            Map<QName, Integer> difference = beforeChange.diff(evaluationOrderHolder.getValue());
            targetEvaluationOrderHolder.setValue(targetEvaluationOrderHolder.getValue().applyDifference(difference));
        }

        if (evaluationOrderHolder.getValue().getSummaryOrder() <= 0) {
            makeUndefined(evaluationOrderHolder, targetEvaluationOrderHolder);
        }
        if (!targetEvaluationOrderHolder.getValue().isValid()) {
            // some extreme cases like the one described in TestAssignmentProcessor2.test520
            makeUndefined(targetEvaluationOrderHolder);
        }
        if (!evaluationOrderHolder.getValue().isValid()) {
            throw new AssertionError("Resulting evaluation order path is invalid: " + evaluationOrderHolder.getValue());
        }
    }

    @SafeVarargs
    private final void makeUndefined(Holder<EvaluationOrder>... holders) {      // final because of SafeVarargs (on java8)
        for (Holder<EvaluationOrder> holder : holders) {
            holder.setValue(EvaluationOrderImpl.UNDEFINED);
        }
    }

    private void addToMembershipLists(AssignmentHolderType targetToAdd, QName relation, EvaluationContext ctx) {
        PrismReferenceValue valueToAdd = prismContext.itemFactory().createReferenceValue();
        valueToAdd.setObject(targetToAdd.asPrismObject());
        valueToAdd.setTargetType(ObjectTypes.getObjectType(targetToAdd.getClass()).getTypeQName());
        valueToAdd.setRelation(relation);
        valueToAdd.setTargetName(targetToAdd.getName().toPolyString());

        addToMembershipLists(valueToAdd, targetToAdd.getClass(), relation, targetToAdd, ctx);
    }

    private void setAsTenantRef(AssignmentHolderType targetToSet, EvaluationContext ctx) {
        if (targetToSet instanceof OrgType) {
            if (BooleanUtils.isTrue(((OrgType)targetToSet).isTenant()) && ctx.evalAssignment.getTenantOid() == null) {
                if (ctx.assignmentPath.hasOnlyOrgs()) {
                    ctx.evalAssignment.setTenantOid(targetToSet.getOid());
                }
            }
        }
    }

    private void addToMembershipLists(ObjectReferenceType referenceToAdd, QName relation, EvaluationContext ctx) {
        PrismReferenceValue valueToAdd = prismContext.itemFactory().createReferenceValue();
        valueToAdd.setOid(referenceToAdd.getOid());
        valueToAdd.setTargetType(referenceToAdd.getType());
        valueToAdd.setRelation(relation);
        valueToAdd.setTargetName(referenceToAdd.getTargetName());

        Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(referenceToAdd.getType()).getClassDefinition();
        addToMembershipLists(valueToAdd, targetClass, relation, referenceToAdd, ctx);
    }

    private void addToMembershipLists(PrismReferenceValue valueToAdd, Class<? extends ObjectType> targetClass, QName relation,
            Object targetDesc, EvaluationContext ctx) {
        if (ctx.assignmentPath.containsDelegation(ctx.evaluateOld, relationRegistry)) {
            addIfNotThere(ctx.evalAssignment.getDelegationRefVals(), valueToAdd, "delegationRef", targetDesc);
        } else {
            if (AbstractRoleType.class.isAssignableFrom(targetClass)) {
                addIfNotThere(ctx.evalAssignment.getMembershipRefVals(), valueToAdd, "membershipRef", targetDesc);
            }
        }
        if (OrgType.class.isAssignableFrom(targetClass) && relationRegistry.isStoredIntoParentOrgRef(relation)) {
            addIfNotThere(ctx.evalAssignment.getOrgRefVals(), valueToAdd, "orgRef", targetDesc);
        }
        if (ArchetypeType.class.isAssignableFrom(targetClass)) {
            addIfNotThere(ctx.evalAssignment.getArchetypeRefVals(), valueToAdd, "archetypeRef", targetDesc);
        }
    }

    private void addIfNotThere(Collection<PrismReferenceValue> collection, PrismReferenceValue valueToAdd, String collectionName,
            Object targetDesc) {
        if (!collection.contains(valueToAdd)) {
            LOGGER.trace("Adding target {} to {}", targetDesc, collectionName);
            collection.add(valueToAdd);
        } else {
            LOGGER.trace("Would add target {} to {}, but it's already there", targetDesc, collectionName);
        }
    }

    private boolean isNonNegative(PlusMinusZero mode) {
        // mode == null is also considered negative, because it is a combination of PLUS and MINUS;
        // so the net result is that for both old and new state there exists an unsatisfied condition on the path.
        return mode == PlusMinusZero.ZERO || mode == PlusMinusZero.PLUS;
    }

    private boolean isChanged(PlusMinusZero mode) {
        // mode == null is also considered negative, because it is a combination of PLUS and MINUS;
        // so the net result is that for both old and new state there exists an unsatisfied condition on the path.
        return mode == PlusMinusZero.PLUS || mode == PlusMinusZero.MINUS;
    }

    private void checkRelationWithTarget(AssignmentPathSegmentImpl segment, AssignmentHolderType targetType, QName relation)
            throws SchemaException {
        if (targetType instanceof AbstractRoleType || targetType instanceof TaskType) { //TODO:
            // OK, just go on
        } else if (targetType instanceof UserType) {
            if (!relationRegistry.isDelegation(relation)) {
                throw new SchemaException("Unsupported relation " + relation + " for assignment of target type " + targetType + " in " + segment.sourceDescription);
            }
        } else {
            throw new SchemaException("Unknown assignment target type " + targetType + " in " + segment.sourceDescription);
        }
    }

    private boolean isInducementApplicableToFocusType(QName inducementFocusType) throws SchemaException {
        if (inducementFocusType == null) {
            return true;
        }
        Class<?> inducementFocusClass = prismContext.getSchemaRegistry().determineCompileTimeClass(inducementFocusType);
        if (inducementFocusClass == null) {
            throw new SchemaException("Could not determine class for " + inducementFocusType);
        }
        if (lensContext.getFocusClass() == null) {
            // should not occur; it would be probably safe to throw an exception here
            LOGGER.error("No focus class in lens context; inducement targeted at focus type {} will not be applied:\n{}",
                    inducementFocusType, lensContext.debugDump());
            return false;
        }
        return inducementFocusClass.isAssignableFrom(lensContext.getFocusClass());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAllowedByLimitations(AssignmentPathSegment segment, AssignmentType nextAssignment, EvaluationContext ctx) {
        AssignmentType currentAssignment = segment.getAssignment(ctx.evaluateOld);
        AssignmentSelectorType targetLimitation = currentAssignment.getLimitTargetContent();
        if (isDeputyDelegation(nextAssignment)) {       // delegation of delegation
            return targetLimitation != null && BooleanUtils.isTrue(targetLimitation.isAllowTransitive());
        } else {
            // As for the case of targetRef==null: we want to pass target-less assignments (focus mappings, policy rules etc)
            // from the delegator to delegatee. To block them we should use order constraints (but also for assignments?).
            return targetLimitation == null || nextAssignment.getTargetRef() == null ||
                    FocusTypeUtil.selectorMatches(targetLimitation, nextAssignment, prismContext);
        }
    }

    private boolean isDeputyDelegation(AssignmentType assignmentType) {
        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        return targetRef != null && relationRegistry.isDelegation(targetRef.getRelation());
    }

    private Authorization createAuthorization(AuthorizationType authorizationType, String sourceDesc) {
        Authorization authorization = new Authorization(authorizationType);
        authorization.setSourceDescription(sourceDesc);
        return authorization;
    }

    private void assertSourceNotNull(ObjectType source, EvaluatedAssignment<AH> assignment) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
        }
    }

    private void assertSourceNotNull(ObjectType source, ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignmentIdi.getAnyItem()+")");
        }
    }

    private AssignmentType getAssignmentType(AssignmentPathSegmentImpl segment, EvaluationContext ctx) {
        return segment.getAssignment(ctx.evaluateOld);
    }

    private void checkSchema(AssignmentPathSegmentImpl segment, EvaluationContext ctx) throws SchemaException {
        AssignmentType assignmentType = getAssignmentType(segment, ctx);
        //noinspection unchecked
        PrismContainerValue<AssignmentType> assignmentContainerValue = assignmentType.asPrismContainerValue();
        PrismContainerable<AssignmentType> assignmentContainer = assignmentContainerValue.getParent();
        if (assignmentContainer == null) {
            throw new SchemaException("The assignment "+assignmentType+" does not have a parent in "+segment.sourceDescription);
        }
        if (assignmentContainer.getDefinition() == null) {
            throw new SchemaException("The assignment "+assignmentType+" does not have definition in "+segment.sourceDescription);
        }
        PrismContainer<Containerable> extensionContainer = assignmentContainerValue.findContainer(AssignmentType.F_EXTENSION);
        if (extensionContainer != null) {
            if (extensionContainer.getDefinition() == null) {
                throw new SchemaException("Extension does not have a definition in assignment "+assignmentType+" in "+segment.sourceDescription);
            }

            if (extensionContainer.getValue().getItems() == null) {
                throw new SchemaException("Extension without items in assignment " + assignmentType + " in " + segment.sourceDescription + ", empty extension tag?");
            }

            for (Item<?,?> item: extensionContainer.getValue().getItems()) {
                if (item == null) {
                    throw new SchemaException("Null item in extension in assignment "+assignmentType+" in "+segment.sourceDescription);
                }
                if (item.getDefinition() == null) {
                    throw new SchemaException("Item "+item+" has no definition in extension in assignment "+assignmentType+" in "+segment.sourceDescription);
                }
            }
        }
    }

    private <O extends ObjectType> void setEvaluatedAssignmentTarget(AssignmentPathSegmentImpl segment,
            @NotNull List<PrismObject<O>> targets, EvaluationContext ctx) {
        assert ctx.evalAssignment.getTarget() == null;
        if (targets.size() > 1) {
            throw new UnsupportedOperationException("Multiple targets for direct focus assignment are not supported: " + segment.getAssignment(ctx.evaluateOld));
        } else if (!targets.isEmpty()) {
            ctx.evalAssignment.setTarget(targets.get(0));
        }
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateCondition(MappingType condition,
            ObjectType source, AssignmentPathVariables assignmentPathVariables, String contextDescription, EvaluationContext ctx,
            OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        MappingImpl.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = mappingFactory.createMappingBuilder();
        builder = builder.mappingType(condition)
                .mappingKind(MappingKindType.ASSIGNMENT_CONDITION)
                .contextDescription(contextDescription)
                .sourceContext(focusOdo)
                .originType(OriginType.ASSIGNMENTS)
                .originObject(source)
                .defaultTargetDefinition(prismContext.definitionFactory().createPropertyDefinition(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN))
                .addVariableDefinitions(getAssignmentEvaluationVariables())
                .rootNode(focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_EVALUATOR, this, AssignmentEvaluator.class);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables, prismContext);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        mappingEvaluator.evaluateMapping(mapping, lensContext, ctx.task, result);

        return mapping.getOutputTriple();
    }

    @Nullable
    private QName getRelation(AssignmentType assignmentType) {
        return assignmentType.getTargetRef() != null ?
                relationRegistry.normalizeRelation(assignmentType.getTargetRef().getRelation()) : null;
    }

    /*
     * This "isMemberOf iteration" section is an experimental implementation of MID-5366.
     *
     * The main idea: In role/assignment/inducement conditions we test the membership not by querying roleMembershipRef
     * on focus object but instead we call assignmentEvaluator.isMemberOf() method. This method - by default - inspects
     * roleMembershipRef but also records the check result. Later, when assignment evaluation is complete, AssignmentProcessor
     * will ask if all of these check results are still valid. If they are not, it requests re-evaluation of all the assignments,
     * using updated check results.
     *
     * This should work unless there are some cyclic dependencies (like "this sentence is a lie" paradox).
     */
    public boolean isMemberOf(String targetOid) {
        if (targetOid == null) {
            throw new IllegalArgumentException("Null targetOid in isMemberOf call");
        }
        MemberOfInvocation existingInvocation = findInvocation(targetOid);
        if (existingInvocation != null) {
            return existingInvocation.result;
        } else {
            boolean result = computeIsMemberOfDuringEvaluation(targetOid);
            memberOfInvocations.add(new MemberOfInvocation(targetOid, result));
            return result;
        }
    }

    private MemberOfInvocation findInvocation(String targetOid) {
        List<MemberOfInvocation> matching = memberOfInvocations.stream()
                .filter(invocation -> targetOid.equals(invocation.targetOid))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            return null;
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new IllegalStateException("More than one matching MemberOfInvocation for targetOid='" + targetOid + "': " + matching);
        }
    }

    private boolean computeIsMemberOfDuringEvaluation(String targetOid) {
        // TODO Or should we consider evaluateOld?
        PrismObject<AH> focus = focusOdo.getNewObject();
        return focus != null && containsMember(focus.asObjectable().getRoleMembershipRef(), targetOid);
    }

    public boolean isMemberOfInvocationResultChanged(DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple) {
        if (!memberOfInvocations.isEmpty()) {
            // Similar code is in AssignmentProcessor.processMembershipAndDelegatedRefs -- check that if changing the business logic
            List<ObjectReferenceType> membership = evaluatedAssignmentTriple.getNonNegativeValues().stream()
                    .filter(EvaluatedAssignmentImpl::isValid)
                    .flatMap(evaluatedAssignment -> evaluatedAssignment.getMembershipRefVals().stream())
                    .map(ref -> ObjectTypeUtil.createObjectRef(ref, false))
                    .collect(Collectors.toList());
            LOGGER.trace("Computed new membership: {}", membership);
            return updateMemberOfInvocations(membership);
        } else {
            return false;
        }
    }

    private boolean updateMemberOfInvocations(List<ObjectReferenceType> newMembership) {
        boolean changed = false;
        for (MemberOfInvocation invocation : memberOfInvocations) {
            boolean newResult = containsMember(newMembership, invocation.targetOid);
            if (newResult != invocation.result) {
                LOGGER.trace("Invocation result changed for {} - new one is '{}'", invocation, newResult);
                invocation.result = newResult;
                changed = true;
            }
        }
        return changed;
    }

    // todo generalize a bit (e.g. by including relation)
    private boolean containsMember(List<ObjectReferenceType> membership, String targetOid) {
        return membership.stream().anyMatch(ref -> targetOid.equals(ref.getOid()));
    }

    public static final class Builder<AH extends AssignmentHolderType> {
        private RepositoryService repository;
        private ObjectDeltaObject<AH> focusOdo;
        private LensContext<AH> lensContext;
        private String channel;
        private ObjectResolver objectResolver;
        private SystemObjectCache systemObjectCache;
        private RelationRegistry relationRegistry;
        private PrismContext prismContext;
        private MappingFactory mappingFactory;
        private ActivationComputer activationComputer;
        private XMLGregorianCalendar now;
        private boolean loginMode = false;
        private PrismObject<SystemConfigurationType> systemConfiguration;
        private MappingEvaluator mappingEvaluator;

        public Builder() {
        }

        public Builder<AH> repository(RepositoryService val) {
            repository = val;
            return this;
        }

        public Builder<AH> focusOdo(ObjectDeltaObject<AH> val) {
            focusOdo = val;
            return this;
        }

        public Builder<AH> lensContext(LensContext<AH> val) {
            lensContext = val;
            return this;
        }

        public Builder<AH> channel(String val) {
            channel = val;
            return this;
        }

        public Builder<AH> objectResolver(ObjectResolver val) {
            objectResolver = val;
            return this;
        }

        public Builder<AH> systemObjectCache(SystemObjectCache val) {
            systemObjectCache = val;
            return this;
        }

        public Builder<AH> relationRegistry(RelationRegistry val) {
            relationRegistry = val;
            return this;
        }

        public Builder<AH> prismContext(PrismContext val) {
            prismContext = val;
            return this;
        }

        public Builder<AH> mappingFactory(MappingFactory val) {
            mappingFactory = val;
            return this;
        }

        public Builder<AH> activationComputer(ActivationComputer val) {
            activationComputer = val;
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

        public Builder<AH> mappingEvaluator(MappingEvaluator val) {
            mappingEvaluator = val;
            return this;
        }

        public AssignmentEvaluator<AH> build() {
            return new AssignmentEvaluator<>(this);
        }
    }

    private static class MemberOfInvocation {
        private final String targetOid;
        private boolean result;

        private MemberOfInvocation(String targetOid, boolean result) {
            this.targetOid = targetOid;
            this.result = result;
        }

        @Override
        public String toString() {
            return "MemberOfInvocation{" +
                    "targetOid='" + targetOid + '\'' +
                    ", result=" + result +
                    '}';
        }
    }
}
