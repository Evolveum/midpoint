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

import java.util.ArrayList;
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
import com.evolveum.midpoint.util.QNameUtil;
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

/**
 * @author semancik
 *
 */
public class AssignmentEvaluator<F extends FocusType> {
	
	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

	private RepositoryService repository;
	private ObjectDeltaObject<F> focusOdo;
	private LensContext<F> lensContext;
	private String channel;
	private ObjectResolver objectResolver;
	private SystemObjectCache systemObjectCache;
	private PrismContext prismContext;
	private MappingFactory mappingFactory;
	private ActivationComputer activationComputer;
	XMLGregorianCalendar now;
	private boolean evaluateConstructions = true;
	private PrismObject<SystemConfigurationType> systemConfiguration;
	private MappingEvaluator mappingEvaluator;
	
	public RepositoryService getRepository() {
		return repository;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}
	
	public ObjectDeltaObject<F> getFocusOdo() {
		return focusOdo;
	}

	public void setFocusOdo(ObjectDeltaObject<F> userOdo) {
		this.focusOdo = userOdo;
	}

	public LensContext<F> getLensContext() {
		return lensContext;
	}

	public void setLensContext(LensContext<F> lensContext) {
		this.lensContext = lensContext;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public SystemObjectCache getSystemObjectCache() {
		return systemObjectCache;
	}

	public void setSystemObjectCache(SystemObjectCache systemObjectCache) {
		this.systemObjectCache = systemObjectCache;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public MappingFactory getMappingFactory() {
		return mappingFactory;
	}

	public void setMappingFactory(MappingFactory mappingFactory) {
		this.mappingFactory = mappingFactory;
	}

	public ActivationComputer getActivationComputer() {
		return activationComputer;
	}

	public void setActivationComputer(ActivationComputer activationComputer) {
		this.activationComputer = activationComputer;
	}

	public XMLGregorianCalendar getNow() {
		return now;
	}

	public void setNow(XMLGregorianCalendar now) {
		this.now = now;
	}

	public boolean isEvaluateConstructions() {
		return evaluateConstructions;
	}

	public void setEvaluateConstructions(boolean evaluateConstructions) {
		this.evaluateConstructions = evaluateConstructions;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}

	public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}
	
	public MappingEvaluator getMappingEvaluator() {
		return mappingEvaluator;
	}

	public void setMappingEvaluator(MappingEvaluator mappingEvaluationHelper) {
		this.mappingEvaluator = mappingEvaluationHelper;
	}

	public EvaluatedAssignmentImpl<F> evaluate(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi, 
			boolean evaluateOld, ObjectType source, String sourceDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, assignmentIdi);
		EvaluatedAssignmentImpl<F> evalAssignment = new EvaluatedAssignmentImpl<>();
		evalAssignment.setAssignmentIdi(assignmentIdi);
		AssignmentPathImpl assignmentPath = new AssignmentPathImpl();
		AssignmentPathSegmentImpl assignmentPathSegment = new AssignmentPathSegmentImpl(assignmentIdi, true);
		assignmentPathSegment.setSource(source);
		assignmentPathSegment.setEvaluationOrder(getInitialEvaluationOrder(assignmentIdi, evaluateOld));
		assignmentPathSegment.setValidityOverride(true);
		assignmentPathSegment.setProcessMembership(true);
		
		evaluateAssignment(evalAssignment, assignmentPathSegment, evaluateOld, PlusMinusZero.ZERO, true, source, sourceDescription, assignmentPath, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment evaluation finished:\n{}", evalAssignment.debugDump());
		}
		
		return evalAssignment;
	}
	
	private EvaluationOrder getInitialEvaluationOrder(
			ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi, boolean evaluateOld) {
		AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
		QName relation = null;
		if (assignmentType.getTargetRef() != null) {
			relation = assignmentType.getTargetRef().getRelation();
		}
		return EvaluationOrderImpl.ZERO.advance(relation);
	}

	private <O extends ObjectType> void evaluateAssignment(EvaluatedAssignmentImpl<F> evalAssignment, AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, PlusMinusZero mode, boolean isParentValid, ObjectType source, String sourceDescription,
			AssignmentPathImpl assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, evalAssignment);
		
		LOGGER.trace("Evaluate assignment {} (matching order: {}, mode: {})", assignmentPath, assignmentPathSegment.isMatchingOrder(),
				mode);
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = assignmentPathSegment.getAssignmentIdi();
		AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
		
		checkSchema(assignmentType, sourceDescription);
		
		QName relation = null;
		if (assignmentType.getTargetRef() != null) {
			relation = assignmentType.getTargetRef().getRelation();
		}
		
		List<PrismObject<O>> targets = null;
		if (assignmentType.getTarget() != null) {
			targets = new ArrayList<>(1);
			targets.add((PrismObject<O>) assignmentType.getTarget().asPrismObject());
		} else if (assignmentType.getTargetRef() != null) {
            try {
                targets = resolveTargets(assignmentType, assignmentPathSegment, source, sourceDescription, assignmentPath, task, result);
            } catch (ObjectNotFoundException ex) {
                // Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
                // an exception would prohibit any operations with the users that have the role, including removal of the reference.
                // The failure is recorded in the result and we will log it. It should be enough.
                LOGGER.error(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
                // For OrgType references we trigger the reconciliation (see MID-2242)
                evalAssignment.setForceRecon(true);
            }
		}
		
		LOGGER.trace("Targets in {}: {}", source, targets);
		if (targets != null) {
			for (PrismObject<O> target: targets) {
				evaluateAssignmentWithResolvedTarget(evalAssignment, assignmentPathSegment, evaluateOld, mode, isParentValid, source,
						sourceDescription, assignmentPath, assignmentType, relation, target, task, result);
			}
		} else {
			evaluateAssignmentWithResolvedTarget(evalAssignment, assignmentPathSegment, evaluateOld, mode, isParentValid, source,
					sourceDescription, assignmentPath, assignmentType, null, null, task, result);
		}
	}

	/**
	 *  Continues with assignment evaluation: Either there is a non-null (resolved) target, passed in "target" parameter,
	 *  or traditional options stored in assignmentType (construction or focus mappings). TargetRef from assignmentType is ignored.
 	 */
	private <O extends ObjectType> void evaluateAssignmentWithResolvedTarget(EvaluatedAssignmentImpl<F> evalAssignment, AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, PlusMinusZero mode, boolean isParentValid, ObjectType source, String sourceDescription,
			AssignmentPathImpl assignmentPath, AssignmentType assignmentType, QName relation, PrismObject<O> target, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		if (target != null && evalAssignment.getTarget() == null) {
			evalAssignment.setTarget(target);
		}

		if (target != null) {
			if (target.getOid().equals(source.getOid())) {
				throw new PolicyViolationException("The "+source+" refers to itself in assignment/inducement");
			}

			LOGGER.trace("Checking for role cycle, comparing segment order {} with path order {}", assignmentPathSegment.getEvaluationOrder(), assignmentPath.getEvaluationOrder());
			if (assignmentPath.containsTarget(target.asObjectable()) && assignmentPathSegment.getEvaluationOrder().equals(assignmentPath.getEvaluationOrder())) {
				LOGGER.debug("Role cycle detected for target {} in {}", ObjectTypeUtil.toShortString(target), assignmentPath);
				throw new PolicyViolationException("Attempt to assign "+target+" creates a role cycle");
			}
			
			AssignmentPathSegment previousAssignmentPathSegment = assignmentPath.last();
			if (previousAssignmentPathSegment != null) {
				if (target.canRepresent(AbstractRoleType.class) && previousAssignmentPathSegment.isDelegation()) {
					if (((AbstractRoleType)target.asObjectable()).isDelegable() != Boolean.TRUE) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Skipping evaluation of {} because it delegates to a non-delagable target {}",
								FocusTypeUtil.dumpAssignment(assignmentType), target);
						}
						return;
					}
				}
			}
		}
		
		assignmentPath.add(assignmentPathSegment);
		
		MappingType conditionType = assignmentType.getCondition();
		if (conditionType != null) {
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);
			PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateMappingAsCondition(conditionType,
					assignmentType, source, assignmentPathVariables, task, result);
			boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
			boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
			PlusMinusZero condMode = ExpressionUtil.computeConditionResultMode(condOld, condNew);
			if (condMode == null || (condMode == PlusMinusZero.ZERO && !condNew)) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Skipping evaluation of {} because of condition result ({} -> {}: {})",
							FocusTypeUtil.dumpAssignment(assignmentType), condOld, condNew, condMode);
				}
				assignmentPath.remove(assignmentPathSegment);
				evalAssignment.setValid(false);
				return;
			}
			PlusMinusZero origMode = mode;
			mode = PlusMinusZero.compute(mode, condMode);
			LOGGER.trace("Evaluated condition in assignment {} -> {}: {} + {} = {}", condOld, condNew, origMode, condMode, mode);
		}
		
		boolean isValid = LensUtil.isAssignmentValid(focusOdo.getNewObject().asObjectable(), 
				assignmentType, now, activationComputer);
		if (isValid || assignmentPathSegment.isValidityOverride()) {
		
			if (assignmentType.getConstruction() != null) {
				
				if (evaluateConstructions && assignmentPathSegment.isMatchingOrder()) {
					prepareConstructionEvaluation(evalAssignment, assignmentPathSegment, evaluateOld, mode, 
							isParentValid && isValid, source, sourceDescription, 
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else if (assignmentType.getFocusMappings() != null) {
				
				if (evaluateConstructions && assignmentPathSegment.isMatchingOrder()) {
					evaluateFocusMappings(evalAssignment, assignmentPathSegment, evaluateOld, source, sourceDescription,
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else if (target != null) {
				
				evaluateAssignmentTarget(evalAssignment, assignmentPathSegment, evaluateOld, mode,
						isParentValid && isValid, (FocusType)target.asObjectable(), source, assignmentType.getTargetRef().getRelation(), 
						sourceDescription, assignmentPath, task, result);
				
			} else if (assignmentType.getPolicyRule() != null) {

				if (evaluateConstructions && assignmentPathSegment.isMatchingOrder()) {
					evaluatePolicyRule(evalAssignment, true, assignmentPathSegment, evaluateOld, mode,
							isParentValid && isValid, source, sourceDescription,
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				if (evaluateConstructions && assignmentPathSegment.isMatchingOrderPlusOne()) {
					evaluatePolicyRule(evalAssignment, false, assignmentPathSegment, evaluateOld, mode,
							isParentValid && isValid, source, sourceDescription,
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else {
				// Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
				// an exception would prohibit any operations with the users that have the role, including removal of the reference.
				LOGGER.debug("No target or construction in assignment in {}, ignoring it", source);
                //result.recordWarning("No target or construction in assignment in " + source + ", ignoring it.");
			}
			
		} else {
			LOGGER.trace("Skipping evaluation of assignment {} because it is not valid", assignmentType);
		}
		evalAssignment.setValid(isValid);
		
		assignmentPath.remove(assignmentPathSegment);
	}

	private void prepareConstructionEvaluation(EvaluatedAssignmentImpl<F> evaluatedAssignment, AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, PlusMinusZero mode, boolean isValid, ObjectType source, String sourceDescription,
			AssignmentPathImpl assignmentPath, ObjectType orderOneObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(assignmentPathSegment.getAssignmentIdi(), evaluateOld);
		ConstructionType constructionType = assignmentTypeNew.getConstruction();
		
		LOGGER.trace("Preparing construction '{}' in {}", constructionType.getDescription(), source);

		Construction<F> construction = new Construction<>(constructionType, source);
		// We have to clone here as the path is constantly changing during evaluation
		construction.setAssignmentPath(assignmentPath.clone());
		construction.setFocusOdo(focusOdo);
		construction.setLensContext(lensContext);
		construction.setObjectResolver(objectResolver);
		construction.setPrismContext(prismContext);
		construction.setMappingFactory(mappingFactory);
		construction.setMappingEvaluator(mappingEvaluator);
		construction.setOriginType(OriginType.ASSIGNMENTS);
		construction.setChannel(channel);
		construction.setOrderOneObject(orderOneObject);
		construction.setValid(isValid);
		
		// Do not evaluate the construction here. We will do it in the second pass. Just prepare everything to be evaluated.
		switch (mode) {
			case PLUS:
				evaluatedAssignment.addConstructionPlus(construction);
				break;
			case ZERO:
				evaluatedAssignment.addConstructionZero(construction);
				break;
			case MINUS:
				evaluatedAssignment.addConstructionMinus(construction);
				break;
		}
	}
	
	private void evaluateFocusMappings(EvaluatedAssignmentImpl<F> evaluatedAssignment, AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, ObjectType source, String sourceDescription,
			AssignmentPathImpl assignmentPath, ObjectType orderOneObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(assignmentPathSegment.getAssignmentIdi(), evaluateOld);
		MappingsType mappingsType = assignmentTypeNew.getFocusMappings();
		
		LOGGER.trace("Evaluate focus mappings '{}' in {} ({} mappings)",
				mappingsType.getDescription(), source, mappingsType.getMapping().size());
		AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);

		for (MappingType mappingType: mappingsType.getMapping()) {
			Mapping mapping = LensUtil.createFocusMapping(mappingFactory, lensContext, mappingType, source, focusOdo, 
					assignmentPathVariables, systemConfiguration, now, sourceDescription, task, result);
			if (mapping == null) {
				continue;
			}
			// TODO: time constratins?
			mappingEvaluator.evaluateMapping(mapping, lensContext, task, result);
			evaluatedAssignment.addFocusMapping(mapping);
		}
	}
	
	private void evaluatePolicyRule(EvaluatedAssignmentImpl<F> evaluatedAssignment, boolean focusRule,
			AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, PlusMinusZero mode, boolean isValid, ObjectType source, String sourceDescription,
			AssignmentPathImpl assignmentPath, ObjectType orderOneObject, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(assignmentPathSegment.getAssignmentIdi(), evaluateOld);
		PolicyRuleType policyRuleType = assignmentTypeNew.getPolicyRule();
		
		LOGGER.trace("Evaluating {} policy rule '{}' in {}", focusRule ? "focus" : "target", policyRuleType.getName(), source);
		
		EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(policyRuleType, assignmentPath.clone());

		if (focusRule) {
			evaluatedAssignment.addFocusPolicyRule(policyRule);
		} else {
			evaluatedAssignment.addTargetPolicyRule(policyRule);
			if (appliesDirectly(policyRule, evaluatedAssignment)) {
				evaluatedAssignment.addThisTargetPolicyRule(policyRule);
			}
		}
	}

	private boolean appliesDirectly(EvaluatedPolicyRuleImpl policyRule, EvaluatedAssignmentImpl<F> evalAssignment) {
		AssignmentPath assignmentPath = policyRule.getAssignmentPath();
		if (assignmentPath.isEmpty()) {
			throw new IllegalStateException("Assignment path for " + policyRule + " is empty; in " + evalAssignment);
		}
		if (assignmentPath.size() == 1) {
			//return true;        // the rule is part of the first assignment target object
			throw new IllegalStateException("Assignment path for " + policyRule + " is of size 1; in " + evalAssignment);
		}
		// TODO think out this again
		// The basic idea is that if we get the rule by an inducement, it does NOT apply directly to
		// the assignment in question. But we should elaborate this later.
		return assignmentPath.getSegments().get(1).isAssignment();
	}

	private <O extends ObjectType> List<PrismObject<O>> resolveTargets(AssignmentType assignmentType, AssignmentPathSegment assignmentPathSegment, ObjectType source, String sourceDescription, AssignmentPathImpl assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ObjectReferenceType targetRef = assignmentType.getTargetRef();
		String oid = targetRef.getOid();
		
		// Target is referenced, need to fetch it
		Class<O> clazz = null;
		if (targetRef.getType() != null) {
			clazz = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
			if (clazz == null) {
				throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignmentType + " in " + sourceDescription);
			}
		} else {
			throw new SchemaException("Missing type in target reference in " + assignmentType + " in " + sourceDescription);
		}
		
		if (oid == null) {
			LOGGER.trace("Resolving dynamic target ref");
			if (targetRef.getFilter() == null){
				throw new SchemaException("The OID and filter are both null in assignment targetRef in "+source);
			}
			
			List<PrismObject<O>> targets = resolveTargetsFromFilter(clazz, assignmentPathSegment, source, targetRef.getFilter(), sourceDescription, assignmentPath, task, result);
			return targets;
			
		} else {
			LOGGER.trace("Resolving target {}:{} from repository", clazz.getSimpleName(), oid);
			PrismObject<O> target = null;
			try {
				target = repository.getObject(clazz, oid, null, result);
	        } catch (SchemaException e) {
	        	throw new SchemaException(e.getMessage() + " in " + sourceDescription, e);
	        }
			// Not handling object not found exception here. Caller will handle that.
			
	        if (target == null) {
	            throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+sourceDescription);
	        }
	        
	        List<PrismObject<O>> targets = new ArrayList<>(1);
	        targets.add(target);
	        return targets;
		}
		
	}
	
	private <O extends ObjectType> List<PrismObject<O>> resolveTargetsFromFilter(Class<O> clazz, AssignmentPathSegment assignmentPathSegment, ObjectType source, SearchFilterType filter, String sourceDescription, AssignmentPathImpl assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
//		SearchFilterType filter = targetRef.getFilter();
		ExpressionEnvironment<F> env = new ExpressionEnvironment<>();
		env.setLensContext(lensContext);
		env.setCurrentResult(result);
		env.setCurrentTask(task);
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
		try {
			
			PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
			ExpressionVariables variables = Utils.getDefaultExpressionVariables(source, null, null, systemConfiguration.asObjectable());
			variables.addVariableDefinition(ExpressionConstants.VAR_SOURCE, assignmentPathSegment.getOrderOneObject());
			AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);
			if (assignmentPathVariables != null) {
				Utils.addAssignmentPathVariables(assignmentPathVariables, variables);
			}
	
			ObjectFilter origFilter = QueryConvertor.parseFilter(filter, clazz, prismContext);
			ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(origFilter, variables, getMappingFactory().getExpressionFactory(), prismContext, " evaluating resource filter expression ", task, result);
			
			if (evaluatedFilter == null){
				throw new SchemaException("The OID is null and filter could not be evaluated in assignment targetRef in "+source);
			}
			
			
	        SearchResultList<PrismObject<O>> targets = repository.searchObjects(clazz, ObjectQuery.createObjectQuery(evaluatedFilter), null, result);
	        
	        if (org.apache.commons.collections.CollectionUtils.isEmpty(targets)){
	        	throw new IllegalArgumentException("Got null target from repository, filter:"+evaluatedFilter+", class:"+clazz+" (should not happen, probably a bug) in "+sourceDescription);
	        }
	        
	        return targets;
        
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
        
	}
		
	private void evaluateAssignmentTarget(EvaluatedAssignmentImpl<F> assignment, AssignmentPathSegmentImpl assignmentPathSegment,
			boolean evaluateOld, PlusMinusZero mode, boolean isValid, FocusType targetType, ObjectType source, QName relation, String sourceDescription,
			AssignmentPathImpl assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, assignment);
		
		assignmentPathSegment.setTarget(targetType);
		assignmentPathSegment.setRelation(relation);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Evaluating TARGET:\n{}", assignmentPathSegment.debugDump(1));
		}
		
		if (targetType instanceof AbstractRoleType) {
			// OK, just go on
			
		} else if (targetType instanceof UserType) {
			if (!DeputyUtils.isDelegationRelation(relation)) {
				throw new SchemaException("Unsupported relation " + relation + " for assignment of target type " + targetType + " in " + sourceDescription);
			}
		} else {
			throw new SchemaException("Unknown assignment target type " + targetType + " in " + sourceDescription);
		}
		
		if (!LensUtil.isFocusValid(targetType, now, activationComputer)) {
			LOGGER.trace("Skipping evaluation of " + targetType + " because it is not valid");
			return;
		}
		
		if (targetType instanceof AbstractRoleType) {
			MappingType conditionType = ((AbstractRoleType)targetType).getCondition();
			if (conditionType != null) {
	            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);
				PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateMappingAsCondition(conditionType,
						null, source, assignmentPathVariables, task, result);
				boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
				boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
				PlusMinusZero condMode = ExpressionUtil.computeConditionResultMode(condOld, condNew);
				if (condMode == null || (condMode == PlusMinusZero.ZERO && !condNew)) {
					LOGGER.trace("Skipping evaluation of "+targetType+" because of condition result ({} -> {}: {})",
							condOld, condNew, condMode);
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
		evalAssignmentTarget.setEvaluateConstructions(assignmentPathSegment.isMatchingOrder());
		evalAssignmentTarget.setAssignment(assignmentPath.last().getAssignment());
		evalAssignmentTarget.setDirectlyAssigned(assignmentPath.size() == 1);
		evalAssignmentTarget.setAssignmentPath(assignmentPath.clone());
		assignment.addRole(evalAssignmentTarget, mode);
		
		if (mode != PlusMinusZero.MINUS && assignmentPathSegment.isProcessMembership()) {
			PrismReferenceValue refVal = new PrismReferenceValue();
			refVal.setObject(targetType.asPrismObject());
			refVal.setTargetType(ObjectTypes.getObjectType(targetType.getClass()).getTypeQName());
			refVal.setRelation(relation);
			refVal.setTargetName(targetType.getName().toPolyString());

			if (assignmentPath.getSegments().stream().anyMatch(aps -> DeputyUtils.isDelegationAssignment(aps.getAssignment()))) {
				LOGGER.trace("Adding target {} to delegationRef", targetType);
				assignment.addDelegationRefVal(refVal);
			} else {
				if (targetType instanceof AbstractRoleType) {
					LOGGER.trace("Adding target {} to membershipRef", targetType);
					assignment.addMembershipRefVal(refVal);
				}
			}
			
			if (targetType instanceof OrgType) {
				LOGGER.trace("Adding target {} to orgRef", targetType);
				assignment.addOrgRefVal(refVal);
			} else {
				LOGGER.trace("NOT adding target {} to orgRef: {}", targetType, assignmentPath);
			}	
		}
		
		if (!DeputyUtils.isMembershipRelation(relation) && !DeputyUtils.isDelegationRelation(relation)) {
			LOGGER.trace("Cutting evaluation of " + targetType + " because it is neither membership nor delegation relation ({})", relation);
			return;
		}
		
		EvaluationOrder evaluationOrder = assignmentPath.getEvaluationOrder();
		ObjectType orderOneObject;
		
		if (evaluationOrder.getSummaryOrder() == 1) {
			orderOneObject = targetType;
		} else {
			AssignmentPathSegment last = assignmentPath.last();
			if (last != null && last.getSource() != null) {
				orderOneObject = last.getSource();
			} else {
				orderOneObject = targetType;
			}
		}
	
		if (targetType instanceof AbstractRoleType) {
			for (AssignmentType roleInducement : ((AbstractRoleType)targetType).getInducement()) {
				if (!isApplicable(roleInducement.getFocusType(), (AbstractRoleType)targetType)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping application of inducement {} because the focusType does not match (specified: {}, actual: {})",
								FocusTypeUtil.dumpAssignment(roleInducement), roleInducement.getFocusType(), targetType.getClass().getSimpleName());
					}
					continue;
				}
				if (!isAllowedByLimitations(assignmentPathSegment, roleInducement)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Skipping application of inducement {} because it is limited",
								FocusTypeUtil.dumpAssignment(roleInducement));
					}
					continue;
				}
				ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> roleInducementIdi = new ItemDeltaItem<>();
				roleInducementIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleInducement));
				roleInducementIdi.recompute();
				AssignmentPathSegmentImpl subAssignmentPathSegment = new AssignmentPathSegmentImpl(roleInducementIdi, false);
				subAssignmentPathSegment.setSource(targetType);
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
				String subSourceDescription = targetType+" in "+sourceDescription;
				evaluateAssignment(assignment, subAssignmentPathSegment, evaluateOld, mode, isValid, targetType, subSourceDescription, assignmentPath, task, result);
			}
		}
		
		for (AssignmentType roleAssignment : targetType.getAssignment()) {
			if (DeputyUtils.isDelegationRelation(relation)) {
				// We have to handle assignments as though they were inducements here.
				if (!isAllowedByLimitations(assignmentPathSegment, roleAssignment)) {
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
			AssignmentPathSegmentImpl subAssignmentPathSegment = new AssignmentPathSegmentImpl(roleAssignmentIdi, true);
			subAssignmentPathSegment.setSource(targetType);
			String subSourceDescription = targetType+" in "+sourceDescription;
			QName subrelation = null;
			if (roleAssignment.getTargetRef() != null) {
				subrelation = roleAssignment.getTargetRef().getRelation();
			}
			subAssignmentPathSegment.setEvaluationOrder(evaluationOrder.advance(subrelation));
			subAssignmentPathSegment.setOrderOneObject(orderOneObject);
			if (targetType instanceof AbstractRoleType) {
				subAssignmentPathSegment.setProcessMembership(false);
			} else {
				// We want to process membership in case of deputy and similar user->user assignments
				subAssignmentPathSegment.setProcessMembership(true);
			}
			evaluateAssignment(assignment, subAssignmentPathSegment, evaluateOld, mode, isValid, targetType, subSourceDescription, assignmentPath, task, result);
		}
		
		if (evaluationOrder.getSummaryOrder() == 1 && targetType instanceof AbstractRoleType) {
			
			for(AuthorizationType authorizationType: ((AbstractRoleType)targetType).getAuthorization()) {
				Authorization authorization = createAuthorization(authorizationType, targetType.toString());
				assignment.addAuthorization(authorization);
			}
			if (((AbstractRoleType)targetType).getAdminGuiConfiguration() != null) {
				assignment.addAdminGuiConfiguration(((AbstractRoleType)targetType).getAdminGuiConfiguration());
			}
			
			PolicyConstraintsType policyConstraints = ((AbstractRoleType)targetType).getPolicyConstraints();
			if (policyConstraints != null) {
				assignment.addLegacyPolicyConstraints(policyConstraints, assignmentPath.clone(), targetType);
			}
		}
	}

	private <O extends ObjectType> boolean containsOtherOrgs(AssignmentPath assignmentPath, FocusType thisOrg) {
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

	private boolean isApplicable(QName focusType, AbstractRoleType roleType) throws SchemaException {
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

	private void assertSource(ObjectType source, EvaluatedAssignment<F> assignment) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
		}
	}
	
	private void assertSource(ObjectType source, ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignmentIdi.getAnyItem()+")");
		}
	}
	
	private void checkSchema(AssignmentType assignmentType, String sourceDescription) throws SchemaException {
		PrismContainerValue<AssignmentType> assignmentContainerValue = assignmentType.asPrismContainerValue();
		PrismContainerable<AssignmentType> assignmentContainer = assignmentContainerValue.getParent();
		if (assignmentContainer == null) {
			throw new SchemaException("The assignment "+assignmentType+" does not have a parent in "+sourceDescription);
		}
		if (assignmentContainer.getDefinition() == null) {
			throw new SchemaException("The assignment "+assignmentType+" does not have definition in "+sourceDescription);
		}
		PrismContainer<Containerable> extensionContainer = assignmentContainerValue.findContainer(AssignmentType.F_EXTENSION);
		if (extensionContainer != null) {
			if (extensionContainer.getDefinition() == null) {
				throw new SchemaException("Extension does not have a definition in assignment "+assignmentType+" in "+sourceDescription);
			}
			for (Item<?,?> item: extensionContainer.getValue().getItems()) {
				if (item == null) {
					throw new SchemaException("Null item in extension in assignment "+assignmentType+" in "+sourceDescription);
				}
				if (item.getDefinition() == null) {
					throw new SchemaException("Item "+item+" has no definition in extension in assignment "+assignmentType+" in "+sourceDescription);
				}
			}
		}
	}
	
	public PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateMappingAsCondition(MappingType conditionType, 
			AssignmentType sourceAssignment, ObjectType source,
            AssignmentPathVariables assignmentPathVariables,
            Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		String desc;
		if (sourceAssignment == null) {
			desc = "condition in " + source; 
		} else {
			desc = "condition in assignment in " + source;
		}
		Mapping.Builder<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> builder = mappingFactory.createMappingBuilder();
		builder = builder.mappingType(conditionType)
				.contextDescription(desc)
				.sourceContext(focusOdo)
				.originType(OriginType.ASSIGNMENTS)
				.originObject(source)
				.defaultTargetDefinition(new PrismPropertyDefinitionImpl<>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, prismContext));

		builder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		builder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		builder.addVariableDefinition(ExpressionConstants.VAR_SOURCE, source);
		builder.setRootNode(focusOdo);
        LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables);

		Mapping<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

		mappingEvaluator.evaluateMapping(mapping, lensContext, task, result);
		
		return mapping.getOutputTriple();
	}


}
