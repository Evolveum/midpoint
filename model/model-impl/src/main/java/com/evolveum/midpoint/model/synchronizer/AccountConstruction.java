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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author semancik
 *
 */
public class AccountConstruction implements DebugDumpable, Dumpable {

	private List<AssignmentType> assignments;
	private AccountConstructionType accountConstructionType;
	private ObjectType source;
	private PrismObject<UserType> user;
	private ResourceType resource;
	private ObjectResolver objectResolver;
	private ValueConstructionFactory valueConstructionFactory;
	private Collection<ValueConstruction<?>> attributeConstructions;
	private RefinedAccountDefinition refinedAccountDefinition;
	private PrismContext prismContext;
	
	public AccountConstruction(AccountConstructionType accountConstructionType, ObjectType source) {
		this.accountConstructionType = accountConstructionType;
		this.source = source;
		this.assignments = new ArrayList<AssignmentType>();
		this.attributeConstructions = null;
	}

	public void setSource(ObjectType source) {
		this.source = source;
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

	PrismContext getPrismContext() {
		return prismContext;
	}

	void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public ValueConstructionFactory getValueConstructionFactory() {
		return valueConstructionFactory;
	}

	public void setValueConstructionFactory(ValueConstructionFactory valueConstructionFactory) {
		this.valueConstructionFactory = valueConstructionFactory;
	}

	public String getAccountType() {
		if (refinedAccountDefinition == null) {
			throw new IllegalStateException("Account type can only be fetched from evaluated AccountConstruction");
		}
		return refinedAccountDefinition.getAccountTypeName();
	}
	
	public Collection<ValueConstruction<?>> getAttributeConstructions() {
		return attributeConstructions;
	}

	public void addAssignment(AssignmentType assignment) {
		assignments.add(assignment);
	}

	public void addAssignments(List<AssignmentType> assignmentPath) {
		assignments.addAll(assignmentPath);
	}
	
	public ResourceType getResource(OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (resource == null) {
			if (accountConstructionType.getResource() != null) {
				resource = accountConstructionType.getResource();
			} else if (accountConstructionType.getResourceRef() != null) {
				try {
					resource = objectResolver.resolve(accountConstructionType.getResourceRef(), ResourceType.class,
							"account construction in "+ source , result);
				} catch (ObjectNotFoundException e) {
					throw new ObjectNotFoundException("Resource reference seems to be invalid in account construction in " + source + ": "+e.getMessage(), e);
				}
			}
			if (resource == null) {
				throw new SchemaException("No resource set in account construction in " + source);
			}
		}
		return resource;
	}
	
	public void evaluate(OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		evaluateAccountType(result);
		evaluateAttributes(result);
	}
	
	private void evaluateAccountType(OperationResult result) throws SchemaException, ObjectNotFoundException {
		String resourceOid = null;
		if (accountConstructionType.getResourceRef() != null) {
			resourceOid = accountConstructionType.getResourceRef().getOid();
		}
		if (accountConstructionType.getResource() != null) {
			resourceOid = accountConstructionType.getResource().getOid();
		}
		if (!getResource(result).getOid().equals(resourceOid)) {
			throw new IllegalStateException("The specified resource and the resource in construction does not match");
		}
		
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(getResource(result), prismContext);
		
		refinedAccountDefinition = refinedSchema.getAccountDefinition(accountConstructionType.getType());
		
		if (refinedAccountDefinition == null) {
			if (accountConstructionType.getType() != null) {
				throw new SchemaException("No account type '"+accountConstructionType.getType()+"' found in "+ObjectTypeUtil.toShortString(getResource(result))+" as specified in account construction in "+ObjectTypeUtil.toShortString(source));
			} else {
				throw new SchemaException("No default account type found in "+ObjectTypeUtil.toShortString(getResource(result))+" as specified in account construction in "+ObjectTypeUtil.toShortString(source));
			}
		}
	}

	private void evaluateAttributes(OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		attributeConstructions = new HashSet<ValueConstruction<?>>();
		for (ValueConstructionType attributeConstructionType : accountConstructionType.getAttribute()) {
			ValueConstruction<?> attributeConstruction = evaluateAttribute(attributeConstructionType, result);
			attributeConstructions.add(attributeConstruction);
		}
	}

	private ValueConstruction<?> evaluateAttribute(ValueConstructionType attributeConstructionType, OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		QName attrName = attributeConstructionType.getRef();
		if (attrName == null) {
			throw new SchemaException("Missing 'ref' in attribute construction in account construction in "+ObjectTypeUtil.toShortString(source));
		}
		PrismPropertyDefinition outputDefinition = findAttributeDefinition(attrName);
		if (outputDefinition == null) {
			throw new SchemaException("Attribute "+attrName+" not found in schema for account type "+getAccountType()+", "+ObjectTypeUtil.toShortString(getResource(result))+" as definied in "+ObjectTypeUtil.toShortString(source), attrName);
		}
		ValueConstruction<?> attributeConstruction = valueConstructionFactory.createValueConstruction(attributeConstructionType, outputDefinition, "in "+ObjectTypeUtil.toShortString(source));
		attributeConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, user);
		attributeConstruction.setRootNode(user);
		if (!assignments.isEmpty()) {
			AssignmentType assignmentType = assignments.get(0);
			attributeConstruction.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignmentType.asPrismContainerValue());
		}
		// TODO: other variables ?
		attributeConstruction.evaluate(result);
		return attributeConstruction;
	}

	private ResourceAttributeDefinition findAttributeDefinition(QName attributeName) {
		return refinedAccountDefinition.getObjectClassDefinition().findAttributeDefinition(attributeName);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignments == null) ? 0 : assignments.hashCode());
		result = prime * result + ((attributeConstructions == null) ? 0 : attributeConstructions.hashCode());
		result = prime * result
				+ ((refinedAccountDefinition == null) ? 0 : refinedAccountDefinition.hashCode());
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AccountConstruction other = (AccountConstruction) obj;
		if (assignments == null) {
			if (other.assignments != null)
				return false;
		} else if (!assignments.equals(other.assignments))
			return false;
		if (attributeConstructions == null) {
			if (other.attributeConstructions != null)
				return false;
		} else if (!attributeConstructions.equals(other.attributeConstructions))
			return false;
		if (refinedAccountDefinition == null) {
			if (other.refinedAccountDefinition != null)
				return false;
		} else if (!refinedAccountDefinition.equals(other.refinedAccountDefinition))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("AccountConstruction(");
		sb.append(refinedAccountDefinition.getResourceAccountType());
		sb.append(")");
		for (ValueConstruction attrConstr: attributeConstructions) {
			sb.append("\n");
			sb.append(attrConstr.debugDump(indent+1));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "AccountConstruction(" + attributeConstructions + ")";
	}
	
}
