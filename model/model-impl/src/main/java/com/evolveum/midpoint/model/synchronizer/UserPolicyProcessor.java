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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * Processor to handle user template and possible also other user "policy" elements.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class UserPolicyProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(UserPolicyProcessor.class);
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
	@Autowired(required=true)
	private ValueConstructionFactory valueConstructionFactory;

	public void processUserPolicy(SyncContext context, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		
		UserTemplateType userTemplate = determineUserTemplate(context, result); 
		
		if (userTemplate == null) {
			// No applicable template
			return;
		}
		
		applyUserTemplate(context, userTemplate, result);
	}

	private void applyUserTemplate(SyncContext context, UserTemplateType userTemplate, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		LOGGER.trace("Applying "+ObjectTypeUtil.toShortString(userTemplate)+" to "+context.getUserNew());
		
		ObjectDelta<UserType> userSecondaryDelta = context.getUserSecondaryDelta();
		for (PropertyConstructionType propConstr: userTemplate.getPropertyConstruction()) {
			XPathHolder propertyXPath = new XPathHolder(propConstr.getProperty());
			PropertyPath propertyPath = new PropertyPath(propertyXPath);
			
			ObjectDefinition<UserType> userDefinition = getUserDefinition();
			PropertyDefinition propertyDefinition = userDefinition.findPropertyDefinition(propertyPath);
			if (propertyDefinition == null) {
				throw new SchemaException("The property "+propertyPath+" is not a valid user property, defined in "+ObjectTypeUtil.toShortString(userTemplate));
			}
			
			ValueConstructionType valueConstructionType = propConstr.getValueConstruction();
			ValueConstruction valueConstruction = valueConstructionFactory.createValueConstruction(valueConstructionType, propertyDefinition, 
					"user template expression for "+propertyDefinition.getName()+" while processing user " + context.getUserNew());
			
			Property existingValue = context.getUserNew().findProperty(propertyPath);
			if (existingValue != null && !existingValue.isEmpty() && valueConstruction.isInitial()) {
				// This valueConstruction only applies if the property does not have a value yet.
				// ... but it does
				continue;
			}
			
			evaluateUserTemplateValueConstruction(valueConstruction, propertyDefinition, context, result);

			Property output = valueConstruction.getOutput();
			PropertyDelta propDelta = new PropertyDelta(propertyPath);
			
			if (propertyDefinition.isMultiValue()) {
				propDelta.addValuesToAdd(output.getValues());
			} else {
				propDelta.setValuesToReplace(output.getValues());	
			}
			
			if (userSecondaryDelta == null) {
				userSecondaryDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY);
				context.setUserSecondaryDelta(userSecondaryDelta);
			}
			userSecondaryDelta.addModification(propDelta);
		}
		
	}

	private ObjectDefinition<UserType> getUserDefinition() {
		return schemaRegistry.getCommonSchema().findObjectDefinition(UserType.class);
	}

	private void evaluateUserTemplateValueConstruction(
			ValueConstruction valueConstruction, PropertyDefinition propertyDefinition, SyncContext context, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, context.getUserNew());
		// TODO: variables
		// TODO: root node
		
		valueConstruction.evaluate(result);
		
	}

	private UserTemplateType determineUserTemplate(SyncContext context, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (context.getUserTemplate() != null) {
			return context.getUserTemplate();
		}
		return null;
	}

	
}
