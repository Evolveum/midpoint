/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An engine that creates EvaluatedAssignment from an assignment IDI. It collects induced roles, constructions,
 * authorizations, policy rules, and so on.
 *
 * @author semancik
 */
public class AssignmentEvaluator<F extends FocusType> {
	
	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

	// "Configuration parameters"
	private final RepositoryService repository;
	private final ObjectDeltaObject<F> focusOdo;
	private final LensContext<F> lensContext;
	private final String channel;
	private final ObjectResolver objectResolver;
	private final SystemObjectCache systemObjectCache;
	private final PrismContext prismContext;
	private final MappingFactory mappingFactory;
	private final ActivationComputer activationComputer;
	private final XMLGregorianCalendar now;
	private final boolean loginMode;		// restricted mode, evaluating only authorizations and gui config (TODO name)
	private final PrismObject<SystemConfigurationType> systemConfiguration;
	private final MappingEvaluator mappingEvaluator;
	private final EvaluatedAssignmentTargetCache evaluatedAssignmentTargetCache;

	private AssignmentEvaluator(Builder<F> builder) {
		repository = builder.repository;
		focusOdo = builder.focusOdo;
		lensContext = builder.lensContext;
		channel = builder.channel;
		objectResolver = builder.objectResolver;
		systemObjectCache = builder.systemObjectCache;
		prismContext = builder.prismContext;
		mappingFactory = builder.mappingFactory;
		activationComputer = builder.activationComputer;
		now = builder.now;
		loginMode = builder.loginMode;
		systemConfiguration = builder.systemConfiguration;
		mappingEvaluator = builder.mappingEvaluator;
		evaluatedAssignmentTargetCache = new EvaluatedAssignmentTargetCache();
	}

	public RepositoryService getRepository() {
		return repository;
	}

	@SuppressWarnings("unused")
	public ObjectDeltaObject<F> getFocusOdo() {
		return focusOdo;
	}

	public LensContext<F> getLensContext() {
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
	
	public void reset() {
		evaluatedAssignmentTargetCache.reset();
	}

	// This is to reduce the number of parameters passed between methods in this class.
	// Moreover, it highlights the fact that identity of objects referenced here is fixed for any invocation of the evaluate() method.
	// (There is single EvaluationContext instance for any call to evaluate().)
	private class EvaluationContext {
		private final EvaluatedAssignmentImpl<F> evalAssignment;
		private final AssignmentPathImpl assignmentPath;
		// The primary assignment mode tells whether the primary assignment was added, removed or it is unchanged.
		// The primary assignment is the first assignment in the assignmen path, the assignment that is located in the
		// focal object.
		private final PlusMinusZero primaryAssignmentMode;
		private final boolean evaluateOld;
		private final Task task;
		private final OperationResult result;
		public EvaluationContext(EvaluatedAssignmentImpl<F> evalAssignment,
				AssignmentPathImpl assignmentPath, PlusMinusZero primaryAssignmentMode, boolean evaluateOld, Task task, OperationResult result) {
			this.evalAssignment = evalAssignment;
			this.assignmentPath = assignmentPath;
			this.primaryAssignmentMode = primaryAssignmentMode;
			this.evaluateOld = evaluateOld;
			this.task = task;
			this.result = result;
		}
	}

	public EvaluatedAssignmentImpl<F> evaluate(
			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
			PlusMinusZero primaryAssignmentMode, boolean evaluateOld, ObjectType source, String sourceDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {

		assertSourceNotNull(source, assignmentIdi);

		EvaluationContext ctx = new EvaluationContext(
				new EvaluatedAssignmentImpl<>(assignmentIdi),
				new AssignmentPathImpl(),
				primaryAssignmentMode, evaluateOld, task, result);

		AssignmentPathSegmentImpl segment = new AssignmentPathSegmentImpl(source, sourceDescription, assignmentIdi, true);
		segment.setEvaluationOrder(getInitialEvaluationOrder(assignmentIdi, ctx));
		segment.setEvaluationOrderForTarget(EvaluationOrderImpl.ZERO);
		segment.setValidityOverride(true);
		segment.setPathToSourceValid(true);
		segment.setProcessMembership(true);
		segment.setRelation(getRelation(getAssignmentType(segment, ctx)));

		evaluateFromSegment(segment, PlusMinusZero.ZERO, ctx);

		LOGGER.trace("Assignment evaluation finished:\n{}", ctx.evalAssignment.debugDumpLazily());
		return ctx.evalAssignment;
	}
	
	private EvaluationOrder getInitialEvaluationOrder(
			ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
			EvaluationContext ctx) {
		AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, ctx.evaluateOld);
		return EvaluationOrderImpl.ZERO.advance(getRelation(assignmentType));
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
	private void evaluateFromSegment(AssignmentPathSegmentImpl segment, PlusMinusZero relativeMode, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
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
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("*** Path (with current segment already added):\n{}", ctx.assignmentPath.debugDump());
		}

		boolean evaluateContent = true;
		AssignmentType assignmentType = getAssignmentType(segment, ctx);
		MappingType assignmentCondition = assignmentType.getCondition();
		if (assignmentCondition != null) {
			AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
			PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(assignmentCondition,
					assignmentType, segment.source, assignmentPathVariables, ctx);
			boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
			boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
			PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
			if (modeFromCondition == null) { // removed "|| (condMode == PlusMinusZero.ZERO && !condNew)" as it is always false
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
			}
		}

		boolean isValid = evaluateContent && evaluateSegmentContent(segment, relativeMode, ctx);

		ctx.assignmentPath.removeLast(segment);
		if (ctx.assignmentPath.isEmpty()) {		// direct assignment
			ctx.evalAssignment.setValid(isValid);
		}
	}

	// "content" means "payload + targets" here
	private <O extends ObjectType> boolean evaluateSegmentContent(AssignmentPathSegmentImpl segment,
			PlusMinusZero relativeMode, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

		assert ctx.assignmentPath.last() == segment;

		final boolean isDirectAssignment = ctx.assignmentPath.size() == 1;

		AssignmentType assignmentType = getAssignmentType(segment, ctx);
		boolean isAssignmentValid = LensUtil.isAssignmentValid(focusOdo.getNewObject().asObjectable(), assignmentType, now, activationComputer);
		if (isAssignmentValid || segment.isValidityOverride()) {
			// Note: validityOverride is currently the same as "isDirectAssignment" - which is very probably OK.
			// Direct assignments are visited even if they are not valid (i.e. effectively disabled).
			// It is because we need to collect e.g. assignment policy rules for them.
			// Also because we could have deltas that disable/enable these assignments.
			boolean reallyValid = segment.isPathToSourceValid() && isAssignmentValid;
			if (!loginMode && segment.isMatchingOrder()) {
				if (assignmentType.getConstruction() != null) {
					collectConstruction(segment, relativeMode, reallyValid, ctx);
				}
				if (assignmentType.getPersonaConstruction() != null) {
					collectPersonaConstruction(segment, relativeMode, reallyValid, ctx);
				}
				if (assignmentType.getFocusMappings() != null) {
					// Here we ignore "reallyValid". It is OK, because reallyValid can be false here only when
					// evaluating direct assignments; and invalid ones are marked as such via EvaluatedAssignment.isValid.
					// (This is currently ignored by downstream processing, but that's another story. Will be fixed soon.)
					if (isNonNegative(relativeMode)) {
						evaluateFocusMappings(segment, ctx);
					}
				}
			}
			if (assignmentType.getPolicyRule() != null && !loginMode) {
				// We can ignore "reallyValid" for the same reason as for focus mappings.
				if (isNonNegative(relativeMode)) {
					if (segment.isMatchingOrder()) {
						collectPolicyRule(true, segment, ctx);
					}
					if (segment.isMatchingOrderForTarget()) {
						collectPolicyRule(false, segment, ctx);
					}
				}
			}
			if (assignmentType.getTarget() != null || assignmentType.getTargetRef() != null) {
				QName relation = getRelation(assignmentType);
				if (loginMode && !ObjectTypeUtil.processRelationOnLogin(relation)) {
					LOGGER.trace("Skipping processing of assignment target {} because relation {} is configured for login skip", assignmentType.getTargetRef().getOid(), relation);
					// Skip - to optimize logging-in, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
					// We want to make this configurable in the future MID-3581
				} else if (!loginMode && !isChanged(ctx.primaryAssignmentMode) && !ObjectTypeUtil.processRelationOnRecompute(relation)) {
					LOGGER.debug("Skipping processing of assignment target {} because relation {} is configured for recompute skip (mode={})", assignmentType.getTargetRef().getOid(), relation, relativeMode);
					// Skip - to optimize recompute, we skip all assignments with non-membership/non-delegation relations (e.g. approver, owner, etc)
					// never skip this if assignment has changed. We want to process this, e.g. to enforce min/max assignee rules
					// We want to make this configurable in the future MID-3581
					
					// Important: but we still want this to be reflected in roleMembershipRef
					if ((isNonNegative(relativeMode)) && segment.isProcessMembership()) {
						if (assignmentType.getTargetRef().getOid() != null) {
							collectMembership(assignmentType.getTargetRef(), relation, ctx);
						} else {
							// no OID, so we have to resolve the filter
							for (PrismObject<ObjectType> targetObject : getTargets(segment, ctx)) {
								ObjectType target = targetObject.asObjectable();
								if (target instanceof FocusType) {
									collectMembership((FocusType) target, relation, ctx);
								}
							}
						}
					}
				} else {
					List<PrismObject<O>> targets = getTargets(segment, ctx);
					LOGGER.trace("Targets in {}, assignment ID {}: {}", segment.source, assignmentType.getId(), targets);
					if (isDirectAssignment) {
						setEvaluatedAssignmentTarget(segment, targets, ctx);
					}
					for (PrismObject<O> target : targets) {
						if (hasCycle(segment, target, ctx)) {
							continue;
						}
						if (isDelegationToNonDelegableTarget(assignmentType, target, ctx)) {
							continue;
						}
						evaluateSegmentTarget(segment, relativeMode, reallyValid, (FocusType) target.asObjectable(), relation, ctx);
					}
				}
			}
		} else {
			LOGGER.trace("Skipping evaluation of assignment {} because it is not valid", assignmentType);
		}
		return isAssignmentValid;
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

	private void collectConstruction(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);

		// TODO why "assignmentTypeNew" when we retrieve old one in some situations?
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
		ConstructionType constructionType = assignmentTypeNew.getConstruction();
		
		LOGGER.trace("Preparing construction '{}' in {}", constructionType.getDescription(), segment.source);

		Construction<F> construction = new Construction<>(constructionType, segment.source);
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
		
		// Do not evaluate the construction here. We will do it in the second pass. Just prepare everything to be evaluated.
		if (mode == null) {
			return;				// null mode (i.e. plus + minus) means 'ignore the payload'
		}
		ctx.evalAssignment.addConstruction(construction, mode);
	}
	
	private void collectPersonaConstruction(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);
		if (mode == null) {
			return;				// null mode (i.e. plus + minus) means 'ignore the payload'
		}
		
		// TODO why "assignmentTypeNew" when we retrieve old one in some situations?
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
		PersonaConstructionType constructionType = assignmentTypeNew.getPersonaConstruction();
		
		LOGGER.trace("Preparing persona construction '{}' in {}", constructionType.getDescription(), segment.source);
		
		PersonaConstruction<F> construction = new PersonaConstruction<>(constructionType, segment.source);
		// We have to clone here as the path is constantly changing during evaluation
		construction.setAssignmentPath(ctx.assignmentPath.clone());
		construction.setFocusOdo(focusOdo);
		construction.setLensContext(lensContext);
		construction.setObjectResolver(objectResolver);
		construction.setPrismContext(prismContext);
		construction.setOriginType(OriginType.ASSIGNMENTS);
		construction.setChannel(channel);
		construction.setValid(isValid);
		
		ctx.evalAssignment.addPersonaConstruction(construction, mode);
	}
	
	private void evaluateFocusMappings(AssignmentPathSegmentImpl segment, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);

		// TODO why "assignmentTypeNew" when we consider also old values?
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
		MappingsType mappingsType = assignmentTypeNew.getFocusMappings();
		
		LOGGER.trace("Evaluate focus mappings '{}' in {} ({} mappings)",
				mappingsType.getDescription(), segment.source, mappingsType.getMapping().size());
		AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);

		for (MappingType mappingType: mappingsType.getMapping()) {
			Mapping mapping = mappingEvaluator.createFocusMapping(mappingFactory, lensContext, mappingType, segment.source, focusOdo,
					assignmentPathVariables, systemConfiguration, now, segment.sourceDescription, ctx.task, ctx.result);
			if (mapping == null) {
				continue;
			}
			// TODO: time constratins?
			mappingEvaluator.evaluateMapping(mapping, lensContext, ctx.task, ctx.result);
			ctx.evalAssignment.addFocusMapping(mapping);
		}
	}

	private void collectPolicyRule(boolean focusRule, AssignmentPathSegmentImpl segment, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
		PolicyRuleType policyRuleType = assignmentTypeNew.getPolicyRule();
		
		LOGGER.trace("Collecting {} policy rule '{}' in {}", focusRule ? "focus" : "target", policyRuleType.getName(), segment.source);
		
		EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(policyRuleType, ctx.assignmentPath.clone());

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
	private <O extends ObjectType> List<PrismObject<O>> getTargets(AssignmentPathSegmentImpl segment, EvaluationContext ctx) throws SchemaException, ExpressionEvaluationException {
		AssignmentType assignmentType = getAssignmentType(segment, ctx);
		if (assignmentType.getTarget() != null) {
			return Collections.singletonList((PrismObject<O>) assignmentType.getTarget().asPrismObject());
		} else if (assignmentType.getTargetRef() != null) {
			try {
				return resolveTargets(segment, ctx);
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
	private <O extends ObjectType> List<PrismObject<O>> resolveTargets(AssignmentPathSegmentImpl segment, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
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
			if (targetRef.getFilter() == null){
				throw new SchemaException("The OID and filter are both null in assignment targetRef in "+segment.source);
			}
			return resolveTargetsFromFilter(targetClass, targetRef.getFilter(), segment, ctx);
		} else {
			LOGGER.trace("Resolving target {}:{} from repository", targetClass.getSimpleName(), oid);
			PrismObject<O> target;
			try {
				target = repository.getObject(targetClass, oid, null, ctx.result);
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
			EvaluationContext ctx) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(lensContext, null, ctx.task, ctx.result));
		try {
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(ctx.result);
			ExpressionVariables variables = Utils.getDefaultExpressionVariables(segment.source, null, null, systemConfiguration.asObjectable());
			variables.addVariableDefinition(ExpressionConstants.VAR_SOURCE, segment.getOrderOneObject());
			AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
			if (assignmentPathVariables != null) {
				Utils.addAssignmentPathVariables(assignmentPathVariables, variables);
			}
	
			ObjectFilter origFilter = QueryConvertor.parseFilter(filter, targetClass, prismContext);
			ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(origFilter, variables, getMappingFactory().getExpressionFactory(), prismContext, " evaluating resource filter expression ", ctx.task, ctx.result);
			if (evaluatedFilter == null) {
				throw new SchemaException("The OID is null and filter could not be evaluated in assignment targetRef in "+segment.source);
			}

			return repository.searchObjects(targetClass, ObjectQuery.createObjectQuery(evaluatedFilter), null, ctx.result);
			// we don't check for no targets here; as we don't care for referential integrity
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}
		
	private void evaluateSegmentTarget(AssignmentPathSegmentImpl segment, PlusMinusZero relativeMode, boolean isValid,
			FocusType targetType, QName relation, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);

		assert ctx.assignmentPath.last() == segment;
		
		segment.setTarget(targetType);
		segment.setRelation(relation);			// probably not needed
		
		if (evaluatedAssignmentTargetCache.canSkip(segment, ctx.primaryAssignmentMode)) {
			LOGGER.trace("Skipping evaluation of segment {} because we have seen the target before", segment);
			return;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Evaluating segment TARGET:\n{}", segment.debugDump(1));
		}

		checkRelationWithTarget(segment, targetType, relation);

		boolean isTargetValid = LensUtil.isFocusValid(targetType, now, activationComputer);
		if (!isTargetValid) {
			if (!segment.isValidityOverride()) {
				LOGGER.trace("Skipping evaluation of {} because it is not valid and validityOverride is not set", targetType);
				return;
			} else {
				isValid = false;
			}
		}
		
		InternalMonitor.recordRoleEvaluation(targetType, true);
		
		if (isTargetValid && targetType instanceof AbstractRoleType) {
			MappingType roleCondition = ((AbstractRoleType)targetType).getCondition();
			if (roleCondition != null) {
	            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
				PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(roleCondition,
						null, segment.source, assignmentPathVariables, ctx);
				boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
				boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
				PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
				if (modeFromCondition == null) {		// removed "|| (condMode == PlusMinusZero.ZERO && !condNew)" because it's always false
					LOGGER.trace("Skipping evaluation of {} because of condition result ({} -> {}: null)",
							targetType, condOld, condNew);
					return;
				}
				PlusMinusZero origMode = relativeMode;
				relativeMode = PlusMinusZero.compute(relativeMode, modeFromCondition);
				LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", targetType, condOld, condNew,
						origMode, modeFromCondition, relativeMode);
			}
		}
		
		EvaluatedAssignmentTargetImpl evalAssignmentTarget = new EvaluatedAssignmentTargetImpl(
				targetType.asPrismObject(),
				segment.isMatchingOrder(),	// evaluateConstructions: exact meaning of this is to be revised
				ctx.assignmentPath.clone(),
				segment.getAssignment(),
				isValid);
		ctx.evalAssignment.addRole(evalAssignmentTarget, relativeMode);

		// we need to evaluate assignments also for disabled targets, because of target policy rules
		for (AssignmentType roleAssignment : targetType.getAssignment()) {
			evaluateAssignment(segment, relativeMode, isValid, ctx, targetType, relation, roleAssignment);
		}

		if ((isNonNegative(relativeMode)) && segment.isProcessMembership()) {
			if (isTargetValid || !ObjectTypeUtil.isMembershipRelation(relation)) {
				// we want to collect approver/owner/whatever-non-membership relations also for disabled targets (MID-3942)
				collectMembership(targetType, relation, ctx);
			}
		}

		if (!isTargetValid) {
			return;
		}

		// We continue evaluation even if the relation is non-membership and non-delegation.
		// Computation of isMatchingOrder will ensure that we won't collect any unwanted content.
		
		if (targetType instanceof AbstractRoleType) {
			for (AssignmentType roleInducement : ((AbstractRoleType)targetType).getInducement()) {
				evaluateInducement(segment, relativeMode, isValid, ctx, targetType, roleInducement);
			}
		}

		//boolean matchesOrder = AssignmentPathSegmentImpl.computeMatchingOrder(segment.getEvaluationOrder(), 1, Collections.emptyList());
		if (segment.isMatchingOrder() && targetType instanceof AbstractRoleType && isNonNegative(relativeMode)) {
			for (AuthorizationType authorizationType: ((AbstractRoleType)targetType).getAuthorization()) {
				Authorization authorization = createAuthorization(authorizationType, targetType.toString());
				if (!ctx.evalAssignment.getAuthorizations().contains(authorization)) {
					ctx.evalAssignment.addAuthorization(authorization);
				}
			}
			AdminGuiConfigurationType adminGuiConfiguration = ((AbstractRoleType) targetType).getAdminGuiConfiguration();
			if (adminGuiConfiguration != null && !ctx.evalAssignment.getAdminGuiConfigurations().contains(adminGuiConfiguration)) {
				ctx.evalAssignment.addAdminGuiConfiguration(adminGuiConfiguration);
			}
			PolicyConstraintsType policyConstraints = ((AbstractRoleType)targetType).getPolicyConstraints();
			if (policyConstraints != null) {
				ctx.evalAssignment.addLegacyPolicyConstraints(policyConstraints, ctx.assignmentPath.clone(), targetType);
			}
		}
		
		evaluatedAssignmentTargetCache.recordProcessing(segment, ctx.primaryAssignmentMode);
		
		LOGGER.trace("Evaluating segment target DONE for {}", segment);
	}

	// TODO revisit this
	private ObjectType getOrderOneObject(AssignmentPathSegmentImpl segment) {
		EvaluationOrder evaluationOrder = segment.getEvaluationOrder();
		if (evaluationOrder.getSummaryOrder() == 1) {
			return segment.getTarget();
		} else {
			if (segment.getSource() != null) {		// should be always the case...
				return segment.getSource();
			} else {
				return segment.getTarget();
			}
		}
	}

	private void evaluateAssignment(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx,
			FocusType targetType, QName relation, AssignmentType roleAssignment)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {

		ObjectType orderOneObject = getOrderOneObject(segment);

		if (ObjectTypeUtil.isDelegationRelation(relation)) {
			// We have to handle assignments as though they were inducements here.
			if (!isInducementAllowedByLimitations(segment, roleAssignment)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Skipping application of delegated assignment {} because it is limited in the delegation",
							FocusTypeUtil.dumpAssignment(roleAssignment));
				}
				return;
			}
		}
		QName nextRelation = getRelation(roleAssignment);
		EvaluationOrder nextEvaluationOrder = segment.getEvaluationOrder().advance(nextRelation);
		EvaluationOrder nextEvaluationOrderForTarget = segment.getEvaluationOrderForTarget().advance(nextRelation);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("orig EO({}): follow assignment {} {}; new EO({})",
					segment.getEvaluationOrder().shortDump(), targetType, FocusTypeUtil.dumpAssignment(roleAssignment), nextEvaluationOrder);
		}
		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> roleAssignmentIdi = new ItemDeltaItem<>();
		roleAssignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleAssignment));
		roleAssignmentIdi.recompute();
		String nextSourceDescription = targetType+" in "+segment.sourceDescription;
		AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl(targetType, nextSourceDescription, roleAssignmentIdi, true);
		nextSegment.setRelation(nextRelation);
		nextSegment.setEvaluationOrder(nextEvaluationOrder);
		nextSegment.setEvaluationOrderForTarget(nextEvaluationOrderForTarget);
		nextSegment.setOrderOneObject(orderOneObject);
		// TODO why??? this should depend on evaluation order
		if (targetType instanceof AbstractRoleType) {
			nextSegment.setProcessMembership(false);			// evaluation order of an assignment is probably too high (TODO but not in case of inducements going back into zero or negative orders!)
		} else {
			// We want to process membership in case of deputy and similar user->user assignments
			nextSegment.setProcessMembership(true);
		}
		nextSegment.setPathToSourceValid(isValid);
		assert !ctx.assignmentPath.isEmpty();
		evaluateFromSegment(nextSegment, mode, ctx);
	}

	private void evaluateInducement(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid, EvaluationContext ctx,
			FocusType targetType, AssignmentType inducement)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {

		ObjectType orderOneObject = getOrderOneObject(segment);

		if (!isInducementApplicableToFocusType(inducement.getFocusType())) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Skipping application of inducement {} because the focusType does not match (specified: {}, actual: {})",
						FocusTypeUtil.dumpAssignment(inducement), inducement.getFocusType(), targetType.getClass().getSimpleName());
			}
			return;
		}
		if (!isInducementAllowedByLimitations(segment, inducement)) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Skipping application of inducement {} because it is limited", FocusTypeUtil.dumpAssignment(inducement));
			}
			return;
		}
		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> roleInducementIdi = new ItemDeltaItem<>();
		roleInducementIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(inducement));
		roleInducementIdi.recompute();
		String subSourceDescription = targetType+" in "+segment.sourceDescription;
		AssignmentPathSegmentImpl nextSegment = new AssignmentPathSegmentImpl(targetType, subSourceDescription, roleInducementIdi, false);

		boolean nextIsMatchingOrder = AssignmentPathSegmentImpl.computeMatchingOrder(
				segment.getEvaluationOrder(), nextSegment.getAssignment());
		boolean nextIsMatchingOrderForTarget = AssignmentPathSegmentImpl.computeMatchingOrder(
				segment.getEvaluationOrderForTarget(), nextSegment.getAssignment());

		Holder<EvaluationOrder> nextEvaluationOrderHolder = new Holder<>(segment.getEvaluationOrder().clone());
		Holder<EvaluationOrder> nextEvaluationOrderForTargetHolder = new Holder<>(segment.getEvaluationOrderForTarget().clone());
		adjustOrder(nextEvaluationOrderHolder, nextEvaluationOrderForTargetHolder, inducement.getOrderConstraint(), inducement.getOrder(), ctx.assignmentPath, nextSegment);
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
					segment.getEvaluationOrder().shortDump(), targetType, FocusTypeUtil.dumpInducementConstraints(inducement),
					FocusTypeUtil.dumpAssignment(inducement), nextEvaluationOrderHolder.getValue().shortDump());
		}
		assert !ctx.assignmentPath.isEmpty();
		evaluateFromSegment(nextSegment, mode, ctx);
	}

	private void adjustOrder(Holder<EvaluationOrder> evaluationOrderHolder, Holder<EvaluationOrder> targetEvaluationOrderHolder,
			List<OrderConstraintsType> constraints, Integer order, AssignmentPathImpl assignmentPath,
			AssignmentPathSegmentImpl nextSegment) {

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
//				MultiSet<QName> backRelations = new HashMultiSet<>();
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
						if (!ObjectTypeUtil.isDelegationRelation(segment.getRelation())) {
							// backRelations.add(segment.getRelation());
							assignmentsSeen++;
							LOGGER.trace("Going back {}: relation at assignment -{} (position -{}): {}", summaryBackwards,
									assignmentsSeen, assignmentPath.size() - i, segment.getRelation());
						}
					} else {
						AssignmentType inducement = segment.getAssignment();
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
							constraint.getResetOrder(), constraint.getRelation(), summaryBackwards, evaluationOrderHolder.getValue().getSummaryOrder(), constraint);
				}
			}
		} else {
			EvaluationOrder beforeChange = evaluationOrderHolder.getValue().clone();
			for (OrderConstraintsType constraint : constraints) {
				if (constraint.getResetOrder() != null) {
					assert constraint.getRelation() != null;		// already processed above
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
	private final void makeUndefined(Holder<EvaluationOrder>... holders) {
		for (Holder<EvaluationOrder> holder : holders) {
			holder.setValue(EvaluationOrderImpl.UNDEFINED);
		}
	}

	private void collectMembership(FocusType targetType, QName relation, EvaluationContext ctx) {
		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setObject(targetType.asPrismObject());
		refVal.setTargetType(ObjectTypes.getObjectType(targetType.getClass()).getTypeQName());
		refVal.setRelation(relation);
		refVal.setTargetName(targetType.getName().toPolyString());
		
		collectMembershipRefVal(refVal, targetType.getClass(), relation, targetType, ctx);
	}
	
	private void collectMembership(ObjectReferenceType targetRef, QName relation, EvaluationContext ctx) {
		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setOid(targetRef.getOid());
		refVal.setTargetType(targetRef.getType());
		refVal.setRelation(relation);
		refVal.setTargetName(targetRef.getTargetName());
		
		Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(targetRef.getType()).getClassDefinition();
		collectMembershipRefVal(refVal, targetClass, relation, targetRef, ctx);
	}
	
	private void collectMembershipRefVal(PrismReferenceValue membershipRefVal, Class<? extends ObjectType> targetClass, QName relation, Object targetDesc, EvaluationContext ctx) {

		if (ctx.assignmentPath.getSegments().stream().anyMatch(aps -> DeputyUtils.isDelegationAssignment(aps.getAssignment()))) {
			addIfNotThere(ctx.evalAssignment.getDelegationRefVals(), ctx.evalAssignment::addDelegationRefVal, membershipRefVal,
					"delegationRef", targetDesc);
		} else {
			if (AbstractRoleType.class.isAssignableFrom(targetClass)) {
				addIfNotThere(ctx.evalAssignment.getMembershipRefVals(), ctx.evalAssignment::addMembershipRefVal, membershipRefVal,
						"membershipRef", targetDesc);
			}
		}
		if (OrgType.class.isAssignableFrom(targetClass) && (ObjectTypeUtil.isDefaultRelation(relation) || ObjectTypeUtil.isManagerRelation(relation))) {
			addIfNotThere(ctx.evalAssignment.getOrgRefVals(), ctx.evalAssignment::addOrgRefVal, membershipRefVal,
					"orgRef", targetDesc);
		}
	}

	private void addIfNotThere(Collection<PrismReferenceValue> collection, Consumer<PrismReferenceValue> setter,
			PrismReferenceValue refVal, String collectionName, Object targetDesc) {
		if (!collection.contains(refVal)) {
			LOGGER.trace("Adding target {} to {}", targetDesc, collectionName);
			setter.accept(refVal);
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

	private void checkRelationWithTarget(AssignmentPathSegmentImpl segment, FocusType targetType, QName relation)
			throws SchemaException {
		if (targetType instanceof AbstractRoleType) {
			// OK, just go on
		} else if (targetType instanceof UserType) {
			if (!ObjectTypeUtil.isDelegationRelation(relation)) {
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
	
	private boolean isInducementAllowedByLimitations(AssignmentPathSegment segment, AssignmentType roleInducement) {
		AssignmentSelectorType limitation = segment.getAssignment().getLimitTargetContent();
		return limitation == null || FocusTypeUtil.selectorMatches(limitation, roleInducement);
	}

	private Authorization createAuthorization(AuthorizationType authorizationType, String sourceDesc) {
		Authorization authorization = new Authorization(authorizationType);
		authorization.setSourceDescription(sourceDesc);
		return authorization;
	}

	private void assertSourceNotNull(ObjectType source, EvaluatedAssignment<F> assignment) {
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
		return LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
	}

	private void checkSchema(AssignmentPathSegmentImpl segment, EvaluationContext ctx) throws SchemaException {
		AssignmentType assignmentType = getAssignmentType(segment, ctx);
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
			throw new UnsupportedOperationException("Multiple targets for direct focus assignment are not supported: " + segment.getAssignment());
		} else if (!targets.isEmpty()) {
			ctx.evalAssignment.setTarget(targets.get(0));
		}
	}

	public PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateCondition(MappingType condition,
			AssignmentType sourceAssignment, ObjectType source, AssignmentPathVariables assignmentPathVariables,
			EvaluationContext ctx) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		String desc;
		if (sourceAssignment == null) {
			desc = "condition in " + source; 
		} else {
			desc = "condition in assignment in " + source;
		}
		Mapping.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = mappingFactory.createMappingBuilder();
		builder = builder.mappingType(condition)
				.contextDescription(desc)
				.sourceContext(focusOdo)
				.originType(OriginType.ASSIGNMENTS)
				.originObject(source)
				.defaultTargetDefinition(new PrismPropertyDefinitionImpl<>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, prismContext))
				.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_SOURCE, source)
				.rootNode(focusOdo);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables);

		Mapping<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

		mappingEvaluator.evaluateMapping(mapping, lensContext, ctx.task, ctx.result);
		
		return mapping.getOutputTriple();
	}

	@Nullable
	private QName getRelation(AssignmentType assignmentType) {
		return assignmentType.getTargetRef() != null ?
				ObjectTypeUtil.normalizeRelation(assignmentType.getTargetRef().getRelation()) : null;
	}

	public static final class Builder<F extends FocusType> {
		private RepositoryService repository;
		private ObjectDeltaObject<F> focusOdo;
		private LensContext<F> lensContext;
		private String channel;
		private ObjectResolver objectResolver;
		private SystemObjectCache systemObjectCache;
		private PrismContext prismContext;
		private MappingFactory mappingFactory;
		private ActivationComputer activationComputer;
		private XMLGregorianCalendar now;
		private boolean loginMode = false;
		private PrismObject<SystemConfigurationType> systemConfiguration;
		private MappingEvaluator mappingEvaluator;

		public Builder() {
		}

		public Builder<F> repository(RepositoryService val) {
			repository = val;
			return this;
		}

		public Builder<F> focusOdo(ObjectDeltaObject<F> val) {
			focusOdo = val;
			return this;
		}

		public Builder<F> lensContext(LensContext<F> val) {
			lensContext = val;
			return this;
		}

		public Builder<F> channel(String val) {
			channel = val;
			return this;
		}

		public Builder<F> objectResolver(ObjectResolver val) {
			objectResolver = val;
			return this;
		}

		public Builder<F> systemObjectCache(SystemObjectCache val) {
			systemObjectCache = val;
			return this;
		}

		public Builder<F> prismContext(PrismContext val) {
			prismContext = val;
			return this;
		}

		public Builder<F> mappingFactory(MappingFactory val) {
			mappingFactory = val;
			return this;
		}

		public Builder<F> activationComputer(ActivationComputer val) {
			activationComputer = val;
			return this;
		}

		public Builder<F> now(XMLGregorianCalendar val) {
			now = val;
			return this;
		}

		public Builder<F> loginMode(boolean val) {
			loginMode = val;
			return this;
		}

		public Builder<F> systemConfiguration(PrismObject<SystemConfigurationType> val) {
			systemConfiguration = val;
			return this;
		}

		public Builder<F> mappingEvaluator(MappingEvaluator val) {
			mappingEvaluator = val;
			return this;
		}

		public AssignmentEvaluator<F> build() {
			return new AssignmentEvaluator<>(this);
		}
	}
}
