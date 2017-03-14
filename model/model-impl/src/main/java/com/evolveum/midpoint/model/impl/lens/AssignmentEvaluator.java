/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
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

	// This is to reduce the number of parameters passed between methods in this class.
	// Moreover, it highlights the fact that identity of objects referenced here is fixed for any invocation of the evaluate() method.
	// (There is single EvaluationContext instance for any call to evaluate().)
	private class EvaluationContext {
		private final EvaluatedAssignmentImpl<F> evalAssignment;
		private final AssignmentPathImpl assignmentPath;
		private final boolean evaluateOld;
		private final Task task;
		private final OperationResult result;
		public EvaluationContext(EvaluatedAssignmentImpl<F> evalAssignment,
				AssignmentPathImpl assignmentPath, boolean evaluateOld, Task task, OperationResult result) {
			this.evalAssignment = evalAssignment;
			this.assignmentPath = assignmentPath;
			this.evaluateOld = evaluateOld;
			this.task = task;
			this.result = result;
		}
	}

	public EvaluatedAssignmentImpl<F> evaluate(
			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi,
			boolean evaluateOld, ObjectType source, String sourceDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {

		assertSourceNotNull(source, assignmentIdi);

		EvaluationContext ctx = new EvaluationContext(
				new EvaluatedAssignmentImpl<>(assignmentIdi),
				new AssignmentPathImpl(),
				evaluateOld, task, result);

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
	 * @param mode
	 *
	 * Where to put constructions and target roles/orgs/services (PLUS/MINUS/ZERO/null; null means "nowhere").
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
	private void evaluateFromSegment(AssignmentPathSegmentImpl segment, PlusMinusZero mode, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("*** Evaluate from segment: {}", segment);
			LOGGER.trace("*** Evaluation order - standard:   {}, matching: {}", segment.getEvaluationOrder(), segment.isMatchingOrder());
			LOGGER.trace("*** Evaluation order - for target: {}, matching: {}", segment.getEvaluationOrderForTarget(), segment.isMatchingOrderForTarget());
			LOGGER.trace("*** mode: {}, process membership: {}", mode, segment.isProcessMembership());
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
				PlusMinusZero origMode = mode;
				mode = PlusMinusZero.compute(mode, modeFromCondition);
				LOGGER.trace("Evaluated condition in assignment {} -> {}: {} + {} = {}", condOld, condNew, origMode,
						modeFromCondition, mode);
			}
		}

		boolean isValid = evaluateContent && evaluateSegmentContent(segment, mode, ctx);

		ctx.assignmentPath.removeLast(segment);
		if (ctx.assignmentPath.isEmpty()) {		// direct assignment
			ctx.evalAssignment.setValid(isValid);
		}
	}

	// "content" means "payload + targets" here
	private <O extends ObjectType> boolean evaluateSegmentContent(AssignmentPathSegmentImpl segment,
			PlusMinusZero mode, EvaluationContext ctx)
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
			if (assignmentType.getConstruction() != null && !loginMode && segment.isMatchingOrder()) {
				collectConstruction(segment, mode, reallyValid, ctx);
			}
			if (assignmentType.getFocusMappings() != null && !loginMode && segment.isMatchingOrder()) {
				// Here we ignore "reallyValid". It is OK, because reallyValid can be false here only when
				// evaluating direct assignments; and invalid ones are marked as such via EvaluatedAssignment.isValid.
				// (This is currently ignored by downstream processing, but that's another story. Will be fixed soon.)
				if (isNonNegative(mode)) {
					evaluateFocusMappings(segment, ctx);
				}
			}
			if (assignmentType.getPolicyRule() != null && !loginMode) {
				// We can ignore "reallyValid" for the same reason as for focus mappings.
				if (isNonNegative(mode)) {
					if (segment.isMatchingOrder()) {
						collectPolicyRule(true, segment, ctx);
					}
					if (segment.isMatchingOrderForTarget()) {
						collectPolicyRule(false, segment, ctx);
					}
				}
			}
			if (assignmentType.getTarget() != null || assignmentType.getTargetRef() != null) {
				List<PrismObject<O>> targets = getTargets(segment, ctx);
				LOGGER.trace("Targets in {}, assignment ID {}: {}", segment.source, assignmentType.getId(), targets);

				if (isDirectAssignment) {
					setEvaluatedAssignmentTarget(segment, targets, ctx);
				}

				QName relation = getRelation(assignmentType);
				for (PrismObject<O> target : targets) {
					checkCycle(segment, target, ctx);
					if (isDelegationToNonDelegableTarget(assignmentType, target, ctx)) {
						continue;
					}
					evaluateSegmentTarget(segment, mode, reallyValid, (FocusType)target.asObjectable(), relation, ctx);
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

	private <O extends ObjectType> void checkCycle(AssignmentPathSegmentImpl segment, @NotNull PrismObject<O> target,
			EvaluationContext ctx) throws PolicyViolationException {
		if (target.getOid().equals(segment.source.getOid())) {
			throw new PolicyViolationException("The "+segment.source+" refers to itself in assignment/inducement");
		}
		// removed condition "&& segment.getEvaluationOrder().equals(ctx.assignmentPath.getEvaluationOrder())"
		// as currently it is always true
		// TODO reconsider this
		if (ctx.assignmentPath.containsTarget(target.asObjectable())) {
			LOGGER.debug("Role cycle detected for target {} in {}", ObjectTypeUtil.toShortString(target), ctx.assignmentPath);
			throw new PolicyViolationException("Attempt to assign "+target+" creates a role cycle");
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
		switch (mode) {
			case PLUS:
				ctx.evalAssignment.addConstructionPlus(construction);
				break;
			case ZERO:
				ctx.evalAssignment.addConstructionZero(construction);
				break;
			case MINUS:
				ctx.evalAssignment.addConstructionMinus(construction);
				break;
		}
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
			Mapping mapping = LensUtil.createFocusMapping(mappingFactory, lensContext, mappingType, segment.source, focusOdo,
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
		
	private void evaluateSegmentTarget(AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid,
			FocusType targetType, QName relation, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);

		assert ctx.assignmentPath.last() == segment;
		
		segment.setTarget(targetType);
		segment.setRelation(relation);			// probably not needed
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Evaluating segment TARGET:\n{}", segment.debugDump(1));
		}

		checkRelationWithTarget(segment, targetType, relation);

		if (!LensUtil.isFocusValid(targetType, now, activationComputer)) {
			LOGGER.trace("Skipping evaluation of {} because it is not valid", targetType);
			return;
		}
		
		if (targetType instanceof AbstractRoleType) {
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
				PlusMinusZero origMode = mode;
				mode = PlusMinusZero.compute(mode, modeFromCondition);
				LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", targetType, condOld, condNew,
						origMode, modeFromCondition, mode);
			}
		}
		
		EvaluatedAssignmentTargetImpl evalAssignmentTarget = new EvaluatedAssignmentTargetImpl(
				targetType.asPrismObject(),
				segment.isMatchingOrder(),	// evaluateConstructions: exact meaning of this is to be revised
				ctx.assignmentPath.clone(),
				segment.getAssignment(),
				isValid);
		ctx.evalAssignment.addRole(evalAssignmentTarget, mode);
		
		if ((isNonNegative(mode)) && segment.isProcessMembership()) {
			evaluateMembership(targetType, relation, ctx);
		}

		// We continue evaluation even if the relation is non-membership and non-delegation.
		// Computation of isMatchingOrder will ensure that we won't collect any unwanted content.
		
		if (targetType instanceof AbstractRoleType) {
			for (AssignmentType roleInducement : ((AbstractRoleType)targetType).getInducement()) {
				evaluateInducement(segment, mode, isValid, ctx, targetType, roleInducement);
			}
		}
		for (AssignmentType roleAssignment : targetType.getAssignment()) {
			evaluateAssignment(segment, mode, isValid, ctx, targetType, relation, roleAssignment);
		}

		boolean matchesOrder = AssignmentPathSegmentImpl.computeMatchingOrder(segment.getEvaluationOrder(), 1, Collections.emptyList());
		if (matchesOrder && targetType instanceof AbstractRoleType && isNonNegative(mode)) {
			for (AuthorizationType authorizationType: ((AbstractRoleType)targetType).getAuthorization()) {
				Authorization authorization = createAuthorization(authorizationType, targetType.toString());
				ctx.evalAssignment.addAuthorization(authorization);
			}
			if (((AbstractRoleType)targetType).getAdminGuiConfiguration() != null) {
				ctx.evalAssignment.addAdminGuiConfiguration(((AbstractRoleType)targetType).getAdminGuiConfiguration());
			}
			PolicyConstraintsType policyConstraints = ((AbstractRoleType)targetType).getPolicyConstraints();
			if (policyConstraints != null) {
				ctx.evalAssignment.addLegacyPolicyConstraints(policyConstraints, ctx.assignmentPath.clone(), targetType);
			}
		}
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

		if (DeputyUtils.isDelegationRelation(relation)) {
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

		EvaluationOrder nextEvaluationOrder, nextEvaluationOrderForTarget;
		if (inducement.getOrder() != null && inducement.getOrder() > 1) {
			nextEvaluationOrder = segment.getEvaluationOrder().decrease(inducement.getOrder()-1);		// TODO what about relations?
			nextEvaluationOrderForTarget = segment.getEvaluationOrderForTarget().decrease(inducement.getOrder()-1);		// TODO what about relations?
			if (nextEvaluationOrder.getSummaryOrder() <= 0) {		// TODO TODO TODO TODO TODO
				nextEvaluationOrder = EvaluationOrderImpl.UNDEFINED;
				nextEvaluationOrderForTarget = EvaluationOrderImpl.UNDEFINED;
			}
		} else {
			nextEvaluationOrder = segment.getEvaluationOrder();
			nextEvaluationOrderForTarget = segment.getEvaluationOrderForTarget();
		}
		// TODO undefined if intervals
		nextSegment.setEvaluationOrder(nextEvaluationOrder, nextIsMatchingOrder);
		nextSegment.setEvaluationOrderForTarget(nextEvaluationOrderForTarget, nextIsMatchingOrderForTarget);

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
					FocusTypeUtil.dumpAssignment(inducement), nextEvaluationOrder.shortDump());
		}
		assert !ctx.assignmentPath.isEmpty();
		evaluateFromSegment(nextSegment, mode, ctx);
	}

	private void evaluateMembership(FocusType targetType, QName relation, EvaluationContext ctx) {
		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setObject(targetType.asPrismObject());
		refVal.setTargetType(ObjectTypes.getObjectType(targetType.getClass()).getTypeQName());
		refVal.setRelation(relation);
		refVal.setTargetName(targetType.getName().toPolyString());

		if (ctx.assignmentPath.getSegments().stream().anyMatch(aps -> DeputyUtils.isDelegationAssignment(aps.getAssignment()))) {
			LOGGER.trace("Adding target {} to delegationRef", targetType);
			ctx.evalAssignment.addDelegationRefVal(refVal);
		} else {
			if (targetType instanceof AbstractRoleType) {
				LOGGER.trace("Adding target {} to membershipRef", targetType);
				ctx.evalAssignment.addMembershipRefVal(refVal);
			}
		}

		if (targetType instanceof OrgType) {
			LOGGER.trace("Adding target {} to orgRef", targetType);
			ctx.evalAssignment.addOrgRefVal(refVal);
		}
	}

	private boolean isNonNegative(PlusMinusZero mode) {
		// mode == null is also considered negative, because it is a combination of PLUS and MINUS;
		// so the net result is that for both old and new state there exists an unsatisfied condition on the path.
		return mode == PlusMinusZero.ZERO || mode == PlusMinusZero.PLUS;
	}

	private void checkRelationWithTarget(AssignmentPathSegmentImpl segment, FocusType targetType, QName relation)
			throws SchemaException {
		if (targetType instanceof AbstractRoleType) {
			// OK, just go on
		} else if (targetType instanceof UserType) {
			if (!DeputyUtils.isDelegationRelation(relation)) {
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
		return assignmentType.getTargetRef() != null ? assignmentType.getTargetRef().getRelation() : null;
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
