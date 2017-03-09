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
import com.evolveum.midpoint.model.api.context.AssignmentPath;
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
import com.evolveum.midpoint.schema.SearchResultList;
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

	public boolean isLoginMode() {
		return loginMode;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}

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
		segment.setValidityOverride(true);
		segment.setProcessMembership(true);

		evaluateFromSegment(segment, PlusMinusZero.ZERO, true, ctx);

		LOGGER.trace("Assignment evaluation finished:\n{}", ctx.evalAssignment.debugDumpLazily());
		return ctx.evalAssignment;
	}
	
	private EvaluationOrder getInitialEvaluationOrder(
			ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
			EvaluationContext ctx) {
		AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, ctx.evaluateOld);
		return EvaluationOrderImpl.ZERO.advance(getRelation(assignmentType));
	}

	// TODO explanation for mode/isParentValid parameters
	private <O extends ObjectType> void evaluateFromSegment(AssignmentPathSegmentImpl segment,
			PlusMinusZero mode, boolean isParentValid, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		LOGGER.trace("Evaluate assignment {} (matching order: {}, mode: {})", ctx.assignmentPath, segment.isMatchingOrder(), mode);

		assertSourceNotNull(segment.source, ctx.evalAssignment);
		checkSchema(segment, ctx);

		List<PrismObject<O>> targets = getTargets(segment, ctx);
		LOGGER.trace("Targets in {}: {}", segment.source, targets);
		if (targets != null) {
			for (PrismObject<O> target: targets) {
				evaluateFromSegmentWithTarget(segment, target, mode, isParentValid, ctx);
			}
		} else {
			evaluateFromSegmentWithTarget(segment, null, mode, isParentValid, ctx);
		}
	}

	/**
	 *  Continues with assignment evaluation: Either there is a non-null (resolved) target, passed in "target" parameter,
	 *  or traditional items stored in assignmentType (construction or focus mappings). TargetRef from assignmentType is ignored here.
 	 */
	private <O extends ObjectType> void evaluateFromSegmentWithTarget(AssignmentPathSegmentImpl segment,
			@Nullable PrismObject<O> target, PlusMinusZero mode, boolean isParentValid, EvaluationContext ctx)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		AssignmentType assignmentType = getAssignmentType(segment, ctx);
		if (target != null && ctx.evalAssignment.getTarget() == null) {
			// hopefully this is set the first time we are in this method
			// (probably yes, because if target==null then we won't continue downwards)
			ctx.evalAssignment.setTarget(target);
		}

		if (target != null) {
			checkCycle(segment, target, ctx);
			if (isDelegationToNonDelegableTarget(assignmentType, target, ctx)) {
				return;
			}
		}

		ctx.assignmentPath.add(segment);

		boolean evaluateContent = true;
		MappingType assignmentCondition = assignmentType.getCondition();
		if (assignmentCondition != null) {
			AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
			PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(assignmentCondition,
					assignmentType, segment.source, assignmentPathVariables, ctx);
			boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
			boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
			PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
			if (modeFromCondition == null) { // removed "|| (condMode == PlusMinusZero.ZERO && !condNew)" as it was always false
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

		boolean isValid = evaluateContent && evaluateSegmentContent(segment, target, mode, isParentValid, ctx);

		ctx.assignmentPath.removeLast(segment);
		if (ctx.assignmentPath.isEmpty()) {
			ctx.evalAssignment.setValid(isValid);		// TODO this may be called multiple times (for more targets) - deal with it
		}
	}

	private <O extends ObjectType> boolean evaluateSegmentContent(AssignmentPathSegmentImpl segment,
			@Nullable PrismObject<O> target, PlusMinusZero mode, boolean isParentValid, EvaluationContext ctx)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

		assert ctx.assignmentPath.last() == segment;

		AssignmentType assignmentType = getAssignmentType(segment, ctx);
		boolean isValid = LensUtil.isAssignmentValid(focusOdo.getNewObject().asObjectable(), assignmentType, now, activationComputer);
		if (isValid || segment.isValidityOverride()) {
			boolean empty = true;
			boolean reallyValid = isParentValid && isValid;
			// TODO for multiple targets, we collect constructions, mappings and rules multiple times - is that OK?
			if (assignmentType.getConstruction() != null) {
				if (!loginMode && segment.isMatchingOrder()) {
					collectConstruction(segment, mode, reallyValid, ctx);
				}
				empty = false;
			}
			if (assignmentType.getFocusMappings() != null) {
				if (!loginMode && segment.isMatchingOrder()) {
					// TODO what about mode and reallyValid???
					evaluateFocusMappings(segment, ctx);
				}
				empty = false;
			}
			if (assignmentType.getPolicyRule() != null) {
				if (!loginMode) {
					// TODO what about mode and reallyValid???
					if (segment.isMatchingOrder()) {
						collectPolicyRule(true, segment, mode, reallyValid, ctx);
					}
					if (segment.isMatchingOrderPlusOne()) {
						collectPolicyRule(false, segment, mode, reallyValid, ctx);
					}
				}
				empty = false;
			}
			if (target != null) {
				evaluateSegmentTarget(segment, mode, reallyValid, (FocusType)target.asObjectable(),
						getRelation(assignmentType), ctx);
				empty = false;
			}
			if (empty) {
				// Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
				// an exception would prohibit any operations with the users that have the role, including removal of the reference.
				LOGGER.debug("No content (target, construction, mapping, policy rule) in assignment in {}, ignoring it", segment.source);
			}
		} else {
			LOGGER.trace("Skipping evaluation of assignment {} because it is not valid", assignmentType);
		}
		return isValid;
	}

	private <O extends ObjectType> boolean isDelegationToNonDelegableTarget(AssignmentType assignmentType, @NotNull PrismObject<O> target,
			EvaluationContext ctx) {
		AssignmentPathSegment previousSegment = ctx.assignmentPath.last();
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
		LOGGER.trace("Checking for role cycle, comparing segment order {} with path order {}", segment.getEvaluationOrder(), ctx.assignmentPath.getEvaluationOrder());
		if (ctx.assignmentPath.containsTarget(target.asObjectable()) && segment.getEvaluationOrder().equals(ctx.assignmentPath.getEvaluationOrder())) {
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

	// TODO treat mode + isValid!!!!
	private void collectPolicyRule(boolean focusRule, AssignmentPathSegmentImpl segment, PlusMinusZero mode, boolean isValid,
			EvaluationContext ctx) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(segment.getAssignmentIdi(), ctx.evaluateOld);
		PolicyRuleType policyRuleType = assignmentTypeNew.getPolicyRule();
		
		LOGGER.trace("Collecting {} policy rule '{}' in {}", focusRule ? "focus" : "target", policyRuleType.getName(), segment.source);
		
		EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(policyRuleType, ctx.assignmentPath.clone());

		if (focusRule) {
			ctx.evalAssignment.addFocusPolicyRule(policyRule);
		} else {
			ctx.evalAssignment.addTargetPolicyRule(policyRule);
			if (appliesDirectly(ctx, policyRule)) {
				ctx.evalAssignment.addThisTargetPolicyRule(policyRule);
			}
		}
	}

	private boolean appliesDirectly(EvaluationContext ctx, EvaluatedPolicyRuleImpl policyRule) {
		assert !ctx.assignmentPath.isEmpty();
		if (ctx.assignmentPath.size() == 1) {
			// the rule is part of the first assignment target object
			throw new IllegalStateException("Assignment path for " + policyRule + " is of size 1; in " + ctx.evalAssignment);
		}
		// TODO think out this again
		// The basic idea is that if we get the rule by an inducement, it does NOT apply directly to
		// the assignment in question. But we should elaborate this later.
		return ctx.assignmentPath.getSegments().get(1).isAssignment();
	}

	@Nullable 	// null only if there's no target/targetRef
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
				return null;
			}
		} else {
			return null;
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
			if (evaluatedFilter == null){
				throw new SchemaException("The OID is null and filter could not be evaluated in assignment targetRef in "+segment.source);
			}

	        SearchResultList<PrismObject<O>> targets = repository.searchObjects(targetClass, ObjectQuery.createObjectQuery(evaluatedFilter), null, ctx.result);
	        if (targets.isEmpty()) {
	        	throw new IllegalArgumentException("Got null target from repository, filter:"+evaluatedFilter+", class:"+targetClass+" (should not happen, probably a bug) in "+segment.sourceDescription);
	        }
	        return targets;
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}
		
	private void evaluateSegmentTarget(AssignmentPathSegmentImpl segment,
			PlusMinusZero mode, boolean isValid, FocusType targetType, QName relation,
			EvaluationContext ctx) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSourceNotNull(segment.source, ctx.evalAssignment);

		assert ctx.assignmentPath.last() == segment;
		
		segment.setTarget(targetType);
		segment.setRelation(relation);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Evaluating TARGET:\n{}", segment.debugDump(1));
		}

		checkRelationWithTarget(segment, targetType, relation);

		if (!LensUtil.isFocusValid(targetType, now, activationComputer)) {
			LOGGER.trace("Skipping evaluation of " + targetType + " because it is not valid");
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
				PlusMinusZero condMode = ExpressionUtil.computeConditionResultMode(condOld, condNew);
				if (condMode == null) {		// removed "|| (condMode == PlusMinusZero.ZERO && !condNew)" because it's always false
					LOGGER.trace("Skipping evaluation of {} because of condition result ({} -> {}: null)",
							targetType, condOld, condNew);
					return;
				}
				PlusMinusZero origMode = mode;
				mode = PlusMinusZero.compute(mode, condMode);
				LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", targetType, condOld, condNew,
						origMode, condMode, mode);
			}
		}
		
		EvaluatedAssignmentTargetImpl evalAssignmentTarget = new EvaluatedAssignmentTargetImpl();
		evalAssignmentTarget.setTarget(targetType.asPrismObject());
		evalAssignmentTarget.setEvaluateConstructions(segment.isMatchingOrder());
		evalAssignmentTarget.setAssignment(segment.getAssignment());
		evalAssignmentTarget.setDirectlyAssigned(ctx.assignmentPath.size() == 1);
		evalAssignmentTarget.setAssignmentPath(ctx.assignmentPath.clone());
		ctx.evalAssignment.addRole(evalAssignmentTarget, mode);
		
		if (mode != PlusMinusZero.MINUS && segment.isProcessMembership()) {
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
			} else {
				LOGGER.trace("NOT adding target {} to orgRef: {}", targetType, ctx.assignmentPath);
			}	
		}
		
		if (!DeputyUtils.isMembershipRelation(relation) && !DeputyUtils.isDelegationRelation(relation)) {
			LOGGER.trace("Cutting evaluation of " + targetType + " because it is neither membership nor delegation relation ({})", relation);
			return;
		}
		
		EvaluationOrder evaluationOrder = ctx.assignmentPath.getEvaluationOrder();
		ObjectType orderOneObject;
		
		if (evaluationOrder.getSummaryOrder() == 1) {
			orderOneObject = targetType;
		} else {
			AssignmentPathSegment last = ctx.assignmentPath.last();
			if (last != null && last.getSource() != null) {
				orderOneObject = last.getSource();
			} else {
				orderOneObject = targetType;
			}
		}

		if (targetType instanceof AbstractRoleType) {
			for (AssignmentType roleInducement : ((AbstractRoleType)targetType).getInducement()) {
				if (!isApplicableToFocusType(roleInducement.getFocusType(), (AbstractRoleType)targetType)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping application of inducement {} because the focusType does not match (specified: {}, actual: {})",
								FocusTypeUtil.dumpAssignment(roleInducement), roleInducement.getFocusType(), targetType.getClass().getSimpleName());
					}
					continue;
				}
				if (!isAllowedByLimitations(segment, roleInducement)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping application of inducement {} because it is limited",
								FocusTypeUtil.dumpAssignment(roleInducement));
					}
					continue;
				}
				ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> roleInducementIdi = new ItemDeltaItem<>();
				roleInducementIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleInducement));
				roleInducementIdi.recompute();
				String subSourceDescription = targetType+" in "+segment.sourceDescription;
				AssignmentPathSegmentImpl subAssignmentPathSegment = new AssignmentPathSegmentImpl(targetType, subSourceDescription, roleInducementIdi, false);
				subAssignmentPathSegment.setEvaluationOrder(evaluationOrder);
				subAssignmentPathSegment.setOrderOneObject(orderOneObject);
				subAssignmentPathSegment.setProcessMembership(subAssignmentPathSegment.isMatchingOrder());

				// Originally we executed the following only if isMatchingOrder. However, sometimes we have to look even into
				// inducements with non-matching order: for example because we need to extract target-related policy rules
				// (these are stored with order of one less than orders for focus-related policy rules).
				//
				// We need to make sure NOT to extract anything other from such inducements. That's why we set e.g.
				// processMembership attribute to false for these inducements.
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("E({}): evaluate inducement({}) {} in {}",
							evaluationOrder.shortDump(), FocusTypeUtil.dumpInducementConstraints(roleInducement),
							FocusTypeUtil.dumpAssignment(roleInducement), targetType);
				}
				evaluateFromSegment(subAssignmentPathSegment, mode, isValid, ctx);
			}
		}
		
		for (AssignmentType roleAssignment : targetType.getAssignment()) {
			if (DeputyUtils.isDelegationRelation(relation)) {
				// We have to handle assignments as though they were inducements here.
				if (!isAllowedByLimitations(segment, roleAssignment)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping application of delegated assignment {} because it is limited in the delegation",
								FocusTypeUtil.dumpAssignment(roleAssignment));
					}
					continue;
				}
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("E({}): follow assignment {} in {}",
						evaluationOrder.shortDump(), FocusTypeUtil.dumpAssignment(roleAssignment), targetType);
			}
			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> roleAssignmentIdi = new ItemDeltaItem<>();
			roleAssignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleAssignment));
			roleAssignmentIdi.recompute();
			String subSourceDescription = targetType+" in "+segment.sourceDescription;
			AssignmentPathSegmentImpl subAssignmentPathSegment = new AssignmentPathSegmentImpl(targetType, subSourceDescription, roleAssignmentIdi, true);
			QName subrelation = getRelation(roleAssignment);

			subAssignmentPathSegment.setEvaluationOrder(evaluationOrder.advance(subrelation));
			subAssignmentPathSegment.setOrderOneObject(orderOneObject);
			if (targetType instanceof AbstractRoleType) {
				subAssignmentPathSegment.setProcessMembership(false);
			} else {
				// We want to process membership in case of deputy and similar user->user assignments
				subAssignmentPathSegment.setProcessMembership(true);
			}
			evaluateFromSegment(subAssignmentPathSegment, mode, isValid, ctx);
		}
		
		if (evaluationOrder.getSummaryOrder() == 1 && targetType instanceof AbstractRoleType) {
			
			for(AuthorizationType authorizationType: ((AbstractRoleType)targetType).getAuthorization()) {
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

	private boolean containsOtherOrgs(AssignmentPath assignmentPath, FocusType thisOrg) {
		for (AssignmentPathSegment segment: assignmentPath.getSegments()) {
			ObjectType segmentTarget = segment.getTarget();
			if (segmentTarget != null) {
				if (segmentTarget instanceof OrgType && !segmentTarget.getOid().equals(thisOrg.getOid())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isApplicableToFocusType(QName focusType, AbstractRoleType roleType) throws SchemaException {
		if (focusType == null) {
			return true;
		}
		
		Class focusClass = prismContext.getSchemaRegistry().determineCompileTimeClass(focusType);
		
		if (focusClass == null){
			throw new SchemaException("Could not determine class for " + focusType);
		}

		if (!focusClass.equals(lensContext.getFocusClass())) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Skipping evaluation of {} because it is applicable only for {} and not for {}",
						roleType, focusClass, lensContext.getFocusClass());
			}
			return false;
		}
		return true;
	}
	
	private boolean isAllowedByLimitations(AssignmentPathSegment assignmentPathSegment, AssignmentType roleInducement) {
		AssignmentSelectorType limitation = assignmentPathSegment.getAssignment().getLimitTargetContent();
		if (limitation == null) {
			return true;
		}
		return FocusTypeUtil.selectorMatches(limitation, roleInducement);
	}

	private QName getTargetType(AssignmentPathSegment assignmentPathSegment){
		return assignmentPathSegment.getTarget().asPrismObject().getDefinition().getName();
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
