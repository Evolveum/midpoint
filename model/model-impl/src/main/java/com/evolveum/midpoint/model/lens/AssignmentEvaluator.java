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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.SimpleDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;

/**
 * @author semancik
 *
 */
public class AssignmentEvaluator<F extends FocusType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEvaluator.class);

	private RepositoryService repository;
	private ObjectDeltaObject<F> userOdo;
	private LensContext<F> lensContext;
	private String channel;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private MappingFactory mappingFactory;
	private ActivationComputer activationComputer;
	XMLGregorianCalendar now;
	private boolean evaluateConstructions = true;
	
	public RepositoryService getRepository() {
		return repository;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}
	
	public ObjectDeltaObject<F> getUserOdo() {
		return userOdo;
	}

	public void setUserOdo(ObjectDeltaObject<F> userOdo) {
		this.userOdo = userOdo;
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

	public SimpleDelta<EvaluatedAssignment> evaluate(SimpleDelta<AssignmentType> assignmentTypeDelta, ObjectType source, String sourceDescription,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		SimpleDelta<EvaluatedAssignment> delta = new SimpleDelta<EvaluatedAssignment>();
		delta.setType(assignmentTypeDelta.getType());
		for (AssignmentType assignmentType : assignmentTypeDelta.getChange()) {
			assertSource(source, assignmentType);
			EvaluatedAssignment assignment = evaluate(assignmentType, source, sourceDescription, task, result);
			delta.getChange().add(assignment);
		}
		return delta;
	}
	
	public EvaluatedAssignment evaluate(AssignmentType assignmentType, ObjectType source, String sourceDescription, 
			Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignmentType);
		EvaluatedAssignment evalAssignment = new EvaluatedAssignment();
		AssignmentPath assignmentPath = new AssignmentPath();
		AssignmentPathSegment assignmentPathSegment = new AssignmentPathSegment(assignmentType, null);
		assignmentPathSegment.setSource(source);
		assignmentPathSegment.setEvaluationOrder(1);
		assignmentPathSegment.setEvaluateConstructions(true);
		assignmentPathSegment.setValidityOverride(true);
		
		evaluateAssignment(evalAssignment, assignmentPathSegment, source, sourceDescription, assignmentPath, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Assignment evaluation finished:\n{}", evalAssignment.debugDump());
		}
		
		return evalAssignment;
	}
	
	private void evaluateAssignment(EvaluatedAssignment evalAssignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, evalAssignment);
		
		assignmentPath.add(assignmentPathSegment);
		LOGGER.trace("Evaluate assignment {} (eval costr: {})", assignmentPath, assignmentPathSegment.isEvaluateConstructions());
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		
		checkSchema(assignmentType, sourceDescription);
		
		boolean isValid = LensUtil.isValid(assignmentType, now, activationComputer);
		if (isValid || assignmentPathSegment.isValidityOverride()) {
		
			if (assignmentType.getAccountConstruction() != null || assignmentType.getConstruction() != null) {
				
				if (evaluateConstructions && assignmentPathSegment.isEvaluateConstructions()) {
					evaluateConstruction(evalAssignment, assignmentPathSegment, source, sourceDescription, 
							assignmentPath, assignmentPathSegment.getOrderOneObject(), task, result);
				}
				
			} else if (assignmentType.getTarget() != null) {
				
				evaluateTarget(evalAssignment, assignmentPathSegment, assignmentType.getTarget(), source, null, sourceDescription,
						assignmentPath, task, result);
				
			} else if (assignmentType.getTargetRef() != null) {
				
				evaluateTargetRef(evalAssignment, assignmentPathSegment, assignmentType.getTargetRef(), source, sourceDescription, 
						assignmentPath, task, result);
	
			} else {
				throw new SchemaException("No target or construcion in assignment in " + source);
			}
			
		} else {
			LOGGER.trace("Skipping evaluation of assignment {} because it is not valid", assignmentType);
		}
		evalAssignment.setValid(isValid);
		
		assignmentPath.remove(assignmentPathSegment);
	}

	private void evaluateConstruction(EvaluatedAssignment evaluatedAssignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, ObjectType orderOneObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, evaluatedAssignment);
		
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		ConstructionType constructionType = assignmentType.getConstruction();
		if (constructionType == null) {
			constructionType = assignmentType.getAccountConstruction();
			if (constructionType.getKind() == null) {
				constructionType.setKind(ShadowKindType.ACCOUNT);
			}
		}
		
		LOGGER.trace("Evaluate construction '{}' in {}", constructionType.getDescription(), source);

		Construction<F> construction = new Construction<F>(constructionType, source);
		// We have to clone here as the path is constantly changing during evaluation
		construction.setAssignmentPath(assignmentPath.clone());
		construction.setUserOdo(userOdo);
		construction.setLensContext(lensContext);
		construction.setObjectResolver(objectResolver);
		construction.setPrismContext(prismContext);
		construction.setMappingFactory(mappingFactory);
		construction.setOriginType(OriginType.ASSIGNMENTS);
		construction.setChannel(channel);
		construction.setOrderOneObject(orderOneObject);
		
		construction.evaluate(task, result);
		
		evaluatedAssignment.addConstruction(construction);
	}

	private void evaluateTargetRef(EvaluatedAssignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectReferenceType targetRef, ObjectType source,
			String sourceDescription, AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		String oid = targetRef.getOid();
		if (oid == null) {
			throw new SchemaException("The OID is null in assignment targetRef in "+ObjectTypeUtil.toShortString(source));
		}
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = null;
		if (targetRef.getType() != null) {
			clazz = (Class) prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
			if (clazz == null) {
				throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignment + " in " + sourceDescription);
			}
		} else {
			throw new SchemaException("Missing type in target reference in " + assignment + " in " + sourceDescription);
		}
		PrismObject<? extends ObjectType> target = null;
		try {
			target = repository.getObject(clazz, oid, null, result);
			if (target == null) {
				throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+sourceDescription);
			}
		} catch (ObjectNotFoundException ex) {
			// Do not throw an exception. We don't have referential integrity. Therefore if a role is deleted then throwing
			// an exception would prohibit any operations with the users that have the role, including removal of the reference.
			// The failure is recorded in the result and we will log it. It should be enough.
			LOGGER.error(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
//			throw new ObjectNotFoundException(ex.getMessage()+" in assignment target reference in "+sourceDescription,ex);
		}
		
		if (target != null) {
			evaluateTarget(assignment, assignmentPathSegment, target.asObjectable(), source, targetRef.getRelation(), 
					sourceDescription, assignmentPath, task, result);
		}
	}


	private void evaluateTarget(EvaluatedAssignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType target, 
			ObjectType source, QName relation, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		assignmentPathSegment.setTarget(target);
		if (target instanceof AbstractRoleType) {
			evaluateAbstractRole(assignment, assignmentPathSegment, (AbstractRoleType)target, source, sourceDescription, 
					assignmentPath, task, result);
			if (target instanceof OrgType && assignmentPath.getEvaluationOrder() == 1) {
				PrismReferenceValue refVal = new PrismReferenceValue();
				refVal.setObject(target.asPrismObject());
				refVal.setRelation(relation);
				assignment.addOrgRefVal(refVal);
			} 
		} else {
			throw new SchemaException("Unknown assignment target type "+ObjectTypeUtil.toShortString(target)+" in "+sourceDescription);
		}
	}

	private void evaluateAbstractRole(EvaluatedAssignment assignment, AssignmentPathSegment assignmentPathSegment, 
			AbstractRoleType role, ObjectType source, String sourceDescription,
			AssignmentPath assignmentPath, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		int evaluationOrder = assignmentPath.getEvaluationOrder();
		ObjectType orderOneObject;
		if (evaluationOrder == 1) {
			orderOneObject = role;
		} else {
			AssignmentPathSegment last = assignmentPath.last();
			if (last != null && last.getOrderOneObject() != null) {
				orderOneObject = last.getOrderOneObject();
			} else {
				orderOneObject = role;
			}
		}
		for (AssignmentType roleInducement : role.getInducement()) {
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleInducement, null);
			roleAssignmentPathSegment.setSource(role);
			String subSourceDescription = role+" in "+sourceDescription;
			Integer inducementOrder = roleInducement.getOrder();
			if (inducementOrder == null) {
				inducementOrder = 1;
			}
			if (inducementOrder == evaluationOrder) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("E{}: evaluate inducement({}) {} in {}",
						new Object[]{evaluationOrder, inducementOrder, dumpAssignment(roleInducement), role});
				}
				roleAssignmentPathSegment.setEvaluateConstructions(true);
				roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder);
				roleAssignmentPathSegment.setOrderOneObject(orderOneObject);
				evaluateAssignment(assignment, roleAssignmentPathSegment, role, subSourceDescription, assignmentPath, task, result);
//			} else if (inducementOrder < assignmentPath.getEvaluationOrder()) {
//				LOGGER.trace("Follow({}) inducement({}) in role {}",
//						new Object[]{evaluationOrder, inducementOrder, source});
//				roleAssignmentPathSegment.setEvaluateConstructions(false);
//				roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder+1);
//				evaluateAssignment(assignment, roleAssignmentPathSegment, role, subSourceDescription, assignmentPath, task, result);
			} else {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("E{}: NOT evaluate inducement({}) {} in {}",
						new Object[]{evaluationOrder, inducementOrder, dumpAssignment(roleInducement), role});
				}
			}
		}
		for (AssignmentType roleAssignment : role.getAssignment()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("E{}: follow assignment {} in {}",
					new Object[]{evaluationOrder, dumpAssignment(roleAssignment), role});
			}
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleAssignment, null);
			roleAssignmentPathSegment.setSource(role);
			String subSourceDescription = role+" in "+sourceDescription;
			roleAssignmentPathSegment.setEvaluateConstructions(false);
			roleAssignmentPathSegment.setEvaluationOrder(evaluationOrder+1);
			roleAssignmentPathSegment.setOrderOneObject(orderOneObject);
			evaluateAssignment(assignment, roleAssignmentPathSegment, role, subSourceDescription, assignmentPath, task, result);
		}
		for(AuthorizationType authorizationType: role.getAuthorization()) {
			Authorization authorization = createAuthorization(authorizationType);
			assignment.addAuthorization(authorization);
		}
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

	private void assertSource(ObjectType source, EvaluatedAssignment assignment) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
		}
	}
	
	private void assertSource(ObjectType source, AssignmentType assignmentType) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignmentType+")");
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

}
