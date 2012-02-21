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
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
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
		List<AssignmentType> assignmentPath = new ArrayList<AssignmentType>();
		
		evaluateAssignment(assignment,assignmentType, source, assignmentPath, result);
		
		
		return assignment;
	}
	
	private void evaluateAssignment(Assignment assignment, AssignmentType assignmentType, ObjectType source, 
			List<AssignmentType> assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		
		assignmentPath.add(assignmentType);
		
		if (assignmentType.getAccountConstruction()!=null) {
			
			evaluateConstruction(assignment,assignmentType, source, assignmentPath, result);
			
		} else if (assignmentType.getTarget() != null) {
			
			evaluateTarget(assignment,assignmentType.getTarget(), source, assignmentPath, result);
			
		} else if (assignmentType.getTargetRef() != null) {

			evaluateTargetRef(assignment,assignmentType.getTargetRef(), source, assignmentPath, result);

		} else {
			throw new SchemaException("No target or accountConstrucion in assignment in "+ObjectTypeUtil.toShortString(source));
		}
		
		assignmentPath.remove(assignmentType);
	}

	private void evaluateConstruction(Assignment assignment, AssignmentType assignmentType, ObjectType source, 
			List<AssignmentType> assignmentPath, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		assertSource(source, assignment);
		AccountConstruction accContruction = new AccountConstruction(assignmentType.getAccountConstruction(),
				source);
		accContruction.addAssignments(assignmentPath);
		accContruction.setUser(user);		
		accContruction.setObjectResolver(objectResolver);
		accContruction.setPrismContext(prismContext);
		accContruction.setValueConstructionFactory(valueConstructionFactory);
		
		accContruction.evaluate(result);
		
		assignment.addAccountConstruction(accContruction);
	}

	private void evaluateTargetRef(Assignment assignment, ObjectReferenceType targetRef, ObjectType source,
			List<AssignmentType> assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		String oid = targetRef.getOid();
		if (oid == null) {
			throw new SchemaException("The OID is null in assignment targetRef in "+ObjectTypeUtil.toShortString(source));
		}
		// Target is referenced, need to fetch it
		Class<? extends ObjectType> clazz = ObjectType.class;
		if (targetRef.getType() != null) {
			clazz = ObjectTypes.getObjectTypeFromTypeQName(targetRef.getType()).getClassDefinition();
		}
		ObjectType target = null;
		try {
			target = repository.getObject(clazz, oid, null, result).asObjectable();
			if (target == null) {
				throw new IllegalArgumentException("Got null target from repository, oid:"+oid+", class:"+clazz+" (should not happen, probably a bug) in "+ObjectTypeUtil.toShortString(source));
			}
		} catch (ObjectNotFoundException ex) {
			throw new ObjectNotFoundException(ex.getMessage()+" in assignment target reference in "+ObjectTypeUtil.toShortString(source),ex);
		}
		evaluateTarget(assignment, target, source, assignmentPath, result);
	}


	private void evaluateTarget(Assignment assignment, ObjectType target, ObjectType source, 
			List<AssignmentType> assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		if (target instanceof RoleType) {
			evaluateRole(assignment, (RoleType)target, source, assignmentPath, result);
		} else {
			throw new SchemaException("Unknown assignment target type "+ObjectTypeUtil.toShortString(target)+" in "+ObjectTypeUtil.toShortString(source));
		}
	}

	private void evaluateRole(Assignment assignment, RoleType role, ObjectType source,
			List<AssignmentType> assignmentPath, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		assertSource(source, assignment);
		for (AssignmentType roleAssignment : role.getAssignment()) {
			evaluateAssignment(assignment, roleAssignment, role, assignmentPath, result);
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
