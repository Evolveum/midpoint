/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.SimpleDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class AssignmentEvaluator {

	private RepositoryService repository;
	private PrismObject<UserType> user;
	private ObjectResolver objectResolver;
	private PrismContext prismContext;
	private ValueConstructionFactory valueConstructionFactory;
	
	public RepositoryService getRepository() {
		return repository;
	}

	public void setRepository(RepositoryService repository) {
		this.repository = repository;
	}

	public PrismObject<UserType> getUser() {
		return user;
	}

	public void setUser(PrismObject<UserType> user) {
		this.user = user;
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

	public ValueConstructionFactory getValueConstructionFactory() {
		return valueConstructionFactory;
	}

	public void setValueConstructionFactory(ValueConstructionFactory valueConstructionFactory) {
		this.valueConstructionFactory = valueConstructionFactory;
	}

	public SimpleDelta<Assignment> evaluate(SimpleDelta<AssignmentType> assignmentTypeDelta, ObjectType source,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		SimpleDelta<Assignment> delta = new SimpleDelta<Assignment>();
		delta.setType(assignmentTypeDelta.getType());
		for (AssignmentType assignmentType : assignmentTypeDelta.getChange()) {
			assertSource(source, assignmentType);
			Assignment assignment = evaluate(assignmentType, source, result);
			delta.getChange().add(assignment);
		}
		return delta;
	}
	
	public Assignment evaluate(AssignmentType assignmentType, ObjectType source, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignmentType);
		Assignment assignment = new Assignment();
		AssignmentPath assignmentPath = new AssignmentPath();
		AssignmentPathSegment assignmentPathSegment = new AssignmentPathSegment(assignmentType, null);
		
		evaluateAssignment(assignment, assignmentPathSegment, source, assignmentPath, result);
		
		return assignment;
	}
	
	private void evaluateAssignment(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, 
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		assignmentPath.add(assignmentPathSegment);
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		
		if (assignmentType.getAccountConstruction()!=null) {
			
			evaluateConstruction(assignment, assignmentPathSegment, source, assignmentPath, result);
			
		} else if (assignmentType.getTarget() != null) {
			
			evaluateTarget(assignment, assignmentPathSegment, assignmentType.getTarget(), source, assignmentPath, result);
			
		} else if (assignmentType.getTargetRef() != null) {
			
			evaluateTargetRef(assignment, assignmentPathSegment, assignmentType.getTargetRef(), source, assignmentPath, result);

		} else {
			throw new SchemaException("No target or accountConstrucion in assignment in "+ObjectTypeUtil.toShortString(source));
		}
		
		assignmentPath.remove(assignmentPathSegment);
	}

	private void evaluateConstruction(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType source, 
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, assignment);
		
		AssignmentType assignmentType = assignmentPathSegment.getAssignmentType();
		AccountConstruction accContruction = new AccountConstruction(assignmentType.getAccountConstruction(),
				source);
		// We have to clone here as the path is constantly changing during evaluation
		accContruction.setAssignmentPath(assignmentPath.clone());
		accContruction.setUser(user);		
		accContruction.setObjectResolver(objectResolver);
		accContruction.setPrismContext(prismContext);
		accContruction.setValueConstructionFactory(valueConstructionFactory);
		
		accContruction.evaluate(result);
		
		assignment.addAccountConstruction(accContruction);
	}

	private void evaluateTargetRef(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectReferenceType targetRef, ObjectType source,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		String oid = targetRef.getOid();
		if (oid == null) {
			throw new SchemaException("The OID is null in assignment targetRef in "+ObjectTypeUtil.toShortString(source));
		}
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = null;
		if (targetRef.getType() != null) {
			clazz = (Class<? extends ObjectType>) prismContext.getSchemaRegistry().determineCompileTimeClass(targetRef.getType());
			if (clazz == null) {
				throw new SchemaException("Cannot determine type from " + targetRef.getType() + " in target reference in " + assignment + " in " + source);
			}
		} else {
			throw new SchemaException("Missing type in target reference in " + assignment + " in " + source);
		}
		PrismObject<? extends ObjectType> target = null;
		try {
			target = repository.getObject(clazz, oid, null, result);
			if (target == null) {
				throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+ObjectTypeUtil.toShortString(source));
			}
		} catch (ObjectNotFoundException ex) {
			throw new ObjectNotFoundException(ex.getMessage()+" in assignment target reference in "+ObjectTypeUtil.toShortString(source),ex);
		}
		
		evaluateTarget(assignment, assignmentPathSegment, target.asObjectable(), source, assignmentPath, result);
	}


	private void evaluateTarget(Assignment assignment, AssignmentPathSegment assignmentPathSegment, ObjectType target, ObjectType source, 
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		assignmentPathSegment.setTarget(target);
		if (target instanceof RoleType) {
			evaluateRole(assignment, assignmentPathSegment, (RoleType)target, source, assignmentPath, result);
		} else {
			throw new SchemaException("Unknown assignment target type "+ObjectTypeUtil.toShortString(target)+" in "+ObjectTypeUtil.toShortString(source));
		}
	}

	private void evaluateRole(Assignment assignment, AssignmentPathSegment assignmentPathSegment, RoleType role, ObjectType source,
			AssignmentPath assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		for (AssignmentType roleAssignment : role.getAssignment()) {
			AssignmentPathSegment roleAssignmentPathSegment = new AssignmentPathSegment(roleAssignment, null);
			evaluateAssignment(assignment, roleAssignmentPathSegment, role, assignmentPath, result);
		}
	}

	private void assertSource(ObjectType source, Assignment assignment) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignment+")");
		}
	}
	
	private void assertSource(ObjectType source, AssignmentType assignmentType) {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null (while evaluating assignment "+assignmentType+")");
		}
	}

}
