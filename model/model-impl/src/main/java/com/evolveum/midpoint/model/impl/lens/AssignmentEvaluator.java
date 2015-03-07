/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluationHelper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Transformer;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

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
	private PrismContext prismContext;
	private MappingFactory mappingFactory;
	private ActivationComputer activationComputer;
	XMLGregorianCalendar now;
	private boolean evaluateConstructions = true;
	private PrismObject<SystemConfigurationType> systemConfiguration;
	private MappingEvaluationHelper mappingEvaluationHelper;
	
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
	
	public MappingEvaluationHelper getMappingEvaluationHelper() {
		return mappingEvaluationHelper;
	}

	public void setMappingEvaluationHelper(MappingEvaluationHelper mappingEvaluationHelper) {
		this.mappingEvaluationHelper = mappingEvaluationHelper;
	}

	public EvaluatedAssignmentImpl<F> evaluate(ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi, 
			boolean evaluateOld, ObjectType source, String sourceDescription, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, assignmentIdi);
		EvaluatedAssignmentImpl<F> evalAssignment = new EvaluatedAssignmentImpl<>();
		evalAssignment.setAssignmentIdi(assignmentIdi);
		AssignmentPath assignmentPath = new AssignmentPath();
		AssignmentPathSegment assignmentPathSegment = new AssignmentPathSegment(assignmentIdi, null);
		assignmentPathSegment.setSource(source);
		assignmentPathSegment.setEvaluationOrder(1);
		assignmentPathSegment.setEvaluateConstructions(true);
		assignmentPathSegment.setValidityOverride(true);
		
		evaluateAssignment(evalAssignment, assignmentPathSegment, evaluateOld, PlusMinusZero.ZERO, source, sourceDescription, assignmentPath, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment evaluation finished:\n{}", evalAssignment.debugDump());
		}
		
		return evalAssignment;
	}
	
	private void evaluateAssignment(EvaluatedAssignmentImpl<F> evalAssignment, AssignmentPathSegment assignmentPathSegment, 
			boolean evaluateOld, PlusMinusZero mode, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, evalAssignment);
		
		LOGGER.trace("Evaluate assignment {} (eval constr: {}, mode: {})", new Object[]{
				assignmentPath, assignmentPathSegment.isEvaluateConstructions(), mode});
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = assignmentPathSegment.getAssignmentIdi();
		AssignmentType assignmentType = LensUtil.getAssignmentType(assignmentIdi, evaluateOld);
		
		checkSchema(assignmentType, sourceDescription);
		
		PrismObject<?> target = null;
		if (assignmentType.getTarget() != null) {
			target = assignmentType.getTarget().asPrismObject();
		} else if (assignmentType.getTargetRef() != null) {
            try {
                target = resolveTarget(assignmentType, source, sourceDescription, task, result);
            } catch (ObjectNotFoundException ex) {
                // Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
                // an exception would prohibit any operations with the users that have the role, including removal of the reference.
                // The failure is recorded in the result and we will log it. It should be enough.
                LOGGER.error(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
                // For OrgType references we trigger the reconciliation (see MID-2242)
                evalAssignment.setForceRecon(true);
            }
		}
		if (target != null && evalAssignment.getTarget() == null) {
			evalAssignment.setTarget(target);
		}

		if (target != null) {
			if (target.getOid().equals(source.getOid())) {
				throw new PolicyViolationException("The "+source+" refers to itself in assignment/inducement");
			}
			if (assignmentPath.containsTarget((ObjectType) target.asObjectable())) {
				throw new PolicyViolationException("Attempt to assign "+target+" creates a role cycle");
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
				LOGGER.trace("Skipping evaluation of "+assignmentType+" because of condition result ({} -> {}: {})",
						new Object[]{ condOld, condNew, condMode });
				assignmentPath.remove(assignmentPathSegment);
				evalAssignment.setValid(false);
				return;
			}
			PlusMinusZero origMode = mode;
			mode = PlusMinusZero.compute(mode, condMode);
			LOGGER.trace("Evaluated condition in assignment {} -> {}: {} + {} = {}", new Object[]{
					condOld, condNew, origMode, condMode, mode });
		}
		
		boolean isValid = LensUtil.isValid(assignmentType, now, activationComputer);
		if (isValid || assignmentPathSegment.isValidityOverride()) {
		
			if (assignmentType.getConstruction() != null) {
				
				if (evaluateConstructions && assignmentPathSegment.isEvaluateConstructions()) {
					prepareConstructionEvaluation(evalAssignment, assignmentPathSegment, evaluateOld, mode, source, sourceDescription, 
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else if (assignmentType.getFocusMappings() != null) {
				
				if (evaluateConstructions && assignmentPathSegment.isEvaluateConstructions()) {
					evaluateFocusMappings(evalAssignment, assignmentPathSegment, evaluateOld, source, sourceDescription, 
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else if (target != null) {
				
				evaluateTarget(evalAssignment, assignmentPathSegment, evaluateOld, mode, target, source, assignmentType.getTargetRef().getRelation(), sourceDescription,
						assignmentPath, task, result);
				
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

	private void prepareConstructionEvaluation(EvaluatedAssignmentImpl<F> evaluatedAssignment, AssignmentPathSegment assignmentPathSegment, 
			boolean evaluateOld, PlusMinusZero mode, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, ObjectType orderOneObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(assignmentPathSegment.getAssignmentIdi(), evaluateOld);
		ConstructionType constructionType = assignmentTypeNew.getConstruction();
		
		LOGGER.trace("Preparing construction '{}' in {}", constructionType.getDescription(), source);

		Construction<F> construction = new Construction<F>(constructionType, source);
		// We have to clone here as the path is constantly changing during evaluation
		construction.setAssignmentPath(assignmentPath.clone());
		construction.setFocusOdo(focusOdo);
		construction.setLensContext(lensContext);
		construction.setObjectResolver(objectResolver);
		construction.setPrismContext(prismContext);
		construction.setMappingFactory(mappingFactory);
		construction.setOriginType(OriginType.ASSIGNMENTS);
		construction.setChannel(channel);
		construction.setOrderOneObject(orderOneObject);
		
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
	
	private void evaluateFocusMappings(EvaluatedAssignmentImpl<F> evaluatedAssignment, AssignmentPathSegment assignmentPathSegment, 
			boolean evaluateOld, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, ObjectType orderOneObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		AssignmentType assignmentTypeNew = LensUtil.getAssignmentType(assignmentPathSegment.getAssignmentIdi(), evaluateOld);
		MappingsType mappingsType = assignmentTypeNew.getFocusMappings();
		
		LOGGER.trace("Evaluate focus mappings '{}' in {} ({} mappings)", 
				new Object[]{mappingsType.getDescription(), source, mappingsType.getMapping().size()});
		AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);

		for (MappingType mappingType: mappingsType.getMapping()) {
			Mapping mapping = LensUtil.createFocusMapping(mappingFactory, lensContext, mappingType, source, focusOdo, 
					assignmentPathVariables, systemConfiguration, now, sourceDescription, result);
			if (mapping == null) {
				continue;
			}
			// TODO: time constratins?
			LensUtil.evaluateMapping(mapping, lensContext, task, result);
			evaluatedAssignment.addFocusMapping(mapping);
		}
	}

	private PrismObject<?> resolveTarget(AssignmentType assignmentType, ObjectType source, String sourceDescription, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ObjectReferenceType targetRef = assignmentType.getTargetRef();
		String oid = targetRef.getOid();
		if (oid == null) {
			throw new SchemaException("The OID is null in assignment targetRef in "+source);
		}
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = null;
		if (targetRef.getType() != null) {
			clazz = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
			if (clazz == null) {
				throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignmentType + " in " + sourceDescription);
			}
		} else {
			throw new SchemaException("Missing type in target reference in " + assignmentType + " in " + sourceDescription);
		}
		PrismObject<? extends ObjectType> target = null;
        target = repository.getObject(clazz, oid, null, result);
        if (target == null) {
            throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+sourceDescription);
        }
        // Handling ObjectNotFoundException - we just pass it to the caller

		return target;
	}


	private void evaluateTarget(EvaluatedAssignmentImpl<F> assignment, AssignmentPathSegment assignmentPathSegment, 
			boolean evaluateOld, PlusMinusZero mode, PrismObject<?> target, ObjectType source, QName relation, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, assignment);
		ObjectType targetType = (ObjectType) target.asObjectable();
		assignmentPathSegment.setTarget(targetType);
		if (targetType instanceof AbstractRoleType) {
			boolean roleConditionTrue = evaluateAbstractRole(assignment, assignmentPathSegment, evaluateOld, mode, (AbstractRoleType)targetType, source, sourceDescription, 
					assignmentPath, task, result);
			if (roleConditionTrue && mode != PlusMinusZero.MINUS && targetType instanceof OrgType && assignmentPath.getEvaluationOrder() == 1) {
				PrismReferenceValue refVal = new PrismReferenceValue();
				refVal.setObject(targetType.asPrismObject());
				refVal.setRelation(relation);
				assignment.addOrgRefVal(refVal);
			} 
		} else {
			throw new SchemaException("Unknown assignment target type "+ObjectTypeUtil.toShortString(targetType)+" in "+sourceDescription);
		}
	}

	private boolean evaluateAbstractRole(EvaluatedAssignmentImpl<F> assignment, AssignmentPathSegment assignmentPathSegment, 
			boolean evaluateOld, PlusMinusZero mode, AbstractRoleType roleType, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException {
		assertSource(source, assignment);
		
		MappingType conditionType = roleType.getCondition();
		if (conditionType != null) {
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);
			PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateMappingAsCondition(conditionType,
					null, source, assignmentPathVariables, task, result);
			boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
			boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
			PlusMinusZero condMode = ExpressionUtil.computeConditionResultMode(condOld, condNew);
			if (condMode == null || (condMode == PlusMinusZero.ZERO && !condNew)) {
				LOGGER.trace("Skipping evaluation of "+roleType+" because of condition result ({} -> {}: {})",
						new Object[]{ condOld, condNew, condMode });
				return false;
			}
			PlusMinusZero origMode = mode;
			mode = PlusMinusZero.compute(mode, condMode);
			LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", new Object[]{
					roleType, condOld, condNew, origMode, condMode, mode });

		}
		
		EvaluatedAbstractRoleImpl evalRole = new EvaluatedAbstractRoleImpl();
		evalRole.setRole(roleType.asPrismObject());
		assignment.addRole(evalRole, mode);
		
		int evaluationOrder = assignmentPath.getEvaluationOrder();
		ObjectType orderOneObject;
		if (evaluationOrder == 1) {
			orderOneObject = roleType;
		} else {
			AssignmentPathSegment last = assignmentPath.last();
			if (last != null && last.getOrderOneObject() != null) {
				orderOneObject = last.getOrderOneObject();
			} else {
				orderOneObject = roleType;
			}
		}
		for (AssignmentType roleInducement : roleType.getInducement()) {
			if (!isApplicable(roleInducement.getFocusType(), roleType)){
				continue;
			}
			ItemDeltaItem<PrismContainerValue<AssignmentType>> roleInducementIdi = new ItemDeltaItem<>();
			roleInducementIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleInducement));
			roleInducementIdi.recompute();
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleInducementIdi, null);
			roleAssignmentPathSegment.setSource(roleType);
			String subSourceDescription = roleType+" in "+sourceDescription;
			Integer inducementOrder = roleInducement.getOrder();
			if (inducementOrder == null) {
				inducementOrder = 1;
			}
			if (inducementOrder == evaluationOrder) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("E{}: evaluate inducement({}) {} in {}",
						new Object[]{evaluationOrder, inducementOrder, dumpAssignment(roleInducement), roleType});
				}
				roleAssignmentPathSegment.setEvaluateConstructions(true);
				roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder);
				roleAssignmentPathSegment.setOrderOneObject(orderOneObject);
				evaluateAssignment(assignment, roleAssignmentPathSegment, evaluateOld, mode, roleType, subSourceDescription, assignmentPath, task, result);
//			} else if (inducementOrder < assignmentPath.getEvaluationOrder()) {
//				LOGGER.trace("Follow({}) inducement({}) in role {}",
//						new Object[]{evaluationOrder, inducementOrder, source});
//				roleAssignmentPathSegment.setEvaluateConstructions(false);
//				roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder+1);
//				evaluateAssignment(assignment, roleAssignmentPathSegment, role, subSourceDescription, assignmentPath, task, result);
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("E{}: NOT evaluate inducement({}) {} in {}",
						new Object[]{evaluationOrder, inducementOrder, dumpAssignment(roleInducement), roleType});
				}
			}
		}
		for (AssignmentType roleAssignment : roleType.getAssignment()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("E{}: follow assignment {} in {}",
					new Object[]{evaluationOrder, dumpAssignment(roleAssignment), roleType});
			}
			ItemDeltaItem<PrismContainerValue<AssignmentType>> roleAssignmentIdi = new ItemDeltaItem<>();
			roleAssignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(roleAssignment));
			roleAssignmentIdi.recompute();
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleAssignmentIdi, null);
			roleAssignmentPathSegment.setSource(roleType);
			String subSourceDescription = roleType+" in "+sourceDescription;
			roleAssignmentPathSegment.setEvaluateConstructions(false);
			roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder+1);
			roleAssignmentPathSegment.setOrderOneObject(orderOneObject);
			evaluateAssignment(assignment, roleAssignmentPathSegment, evaluateOld, mode, roleType, subSourceDescription, assignmentPath, task, result);
		}
		for(AuthorizationType authorizationType: roleType.getAuthorization()) {
			Authorization authorization = createAuthorization(authorizationType);
			assignment.addAuthorization(authorization);
		}
		
		return mode != PlusMinusZero.MINUS;
		
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
						new Object[] { roleType, focusClass, lensContext.getFocusClass()});
			}
			return false;
		}
		return true;
	}
	
	private QName getTargetType(AssignmentPathSegment assignmentPathSegment){
		return assignmentPathSegment.getTarget().asPrismObject().getDefinition().getName();
	}
	
	public static String dumpAssignment(AssignmentType assignmentType) { 
		StringBuilder sb = new StringBuilder();
		if (assignmentType.getConstruction() != null) {
			sb.append("Constr '"+assignmentType.getConstruction().getDescription()+"' ");
		}
		if (assignmentType.getTargetRef() != null) {
			sb.append("-> ").append(assignmentType.getTargetRef().getOid());
		}
		return sb.toString();
	}


	private Authorization createAuthorization(AuthorizationType authorizationType) {
		Authorization authorization = new Authorization(authorizationType);
		return authorization;
	}

	private void assertSource(ObjectType source, EvaluatedAssignment<F> assignment) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
		}
	}
	
	private void assertSource(ObjectType source, ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi) {
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
			for (Item<?> item: extensionContainer.getValue().getItems()) {
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
		Mapping<? extends PrismPropertyValue<Boolean>> mapping = mappingFactory.createMapping(conditionType,
				desc);
		
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_SOURCE, source);
		mapping.setSourceContext(focusOdo);
		mapping.setRootNode(focusOdo);
		mapping.setOriginType(OriginType.ASSIGNMENTS);
		mapping.setOriginObject(source);

        LensUtil.addAssignmentPathVariables(mapping, assignmentPathVariables);

		ItemDefinition outputDefinition = new PrismPropertyDefinition<Boolean>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, prismContext);
		mapping.setDefaultTargetDefinition(outputDefinition);

		LensUtil.evaluateMapping(mapping, lensContext, task, result);
		
		return (PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>>) mapping.getOutputTriple();
	}


}
