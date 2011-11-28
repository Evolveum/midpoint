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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.DeltaSetTriple;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.ObjectDelta;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDelta;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author semancik
 *
 */
@Component
public class UserSynchronizer {
	
	private static final Trace LOGGER = TraceManager.getTrace(UserSynchronizer.class);
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
	private AssignmentProcessor assignmentProcessor;

	@Autowired(required = true)
	private OutboundProcessor outboundProcessor;

	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;

	@Autowired(required=true)
	private ValueConstructionFactory valueConstructionFactory;
	
	public void synchronizeUser(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		loadUser(context, result);
		context.recomputeUserNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after loadUser and recompute:\n{}",context.dump());
		}
		
		processInbound(context);
		context.recomputeUserNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after inbound and recompute:\n{}",context.dump());
		}
		
		processUserPolicy(context, result);
		context.recomputeUserNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after userPolicy and recompute:\n{}",context.dump());
			LOGGER.trace("User delta:\n{}",context.getUserDelta().dump());
		}
		
		assignmentProcessor.processAssignments(context, result);
		context.recomputeNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after assignments and recompute:\n{}",context.dump());
		}

		outboundProcessor.processOutbound(context, result);
		context.recomputeNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after outbound and recompute:\n{}",context.dump());
		}

		consolidateValues(context, result);
		context.recomputeNew();
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Context after consolidation and recompute:\n{}",context.dump());
		}

	}

	private void loadUser(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (context.getUserOld() != null) {
			// already loaded
			return;
		}
		ObjectDelta<UserType> userPrimaryDelta = context.getUserPrimaryDelta();
		if (userPrimaryDelta == null) {
			// no change to user
			// TODO: where to get OID from????
			throw new UnsupportedOperationException("TODO");
		}
		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			// null oldUser is OK
			return;
		}
		String userOid = userPrimaryDelta.getOid();
		
		UserType userType = cacheRepositoryService.getObject(UserType.class, userOid, null, result);
		context.setUserTypeOld(userType);
		Schema commonSchema = schemaRegistry.getCommonSchema();
		MidPointObject<UserType> user = commonSchema.parseObjectType(userType);
		context.setUserOld(user);
	}

	private void processInbound(SyncContext context) {
		// Loop through the account changes, apply inbound expressions
	}
	
	private void processUserPolicy(SyncContext context, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
		
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
		return getDefaultUserTemplate(result);
	}

	private UserTemplateType getDefaultUserTemplate(OperationResult result) throws ObjectNotFoundException, SchemaException {
		SystemConfigurationType systemConfigurationType = cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
		if (systemConfigurationType == null) {
			// throw new SystemException("System configuration object is null (should not happen!)");
			// This should not happen, but it happens in tests. And it is a convenient short cut. Tolerate it for now.
			return null;
		}
		return systemConfigurationType.getDefaultUserTemplate();
	}

	/**
	 * Converts delta set triples to a secondary account deltas.
	 */
	private void consolidateValues(SyncContext context, OperationResult result) throws SchemaException {
		
		for (AccountSyncContext accCtx: context.getAccountContexts()) {
			
			ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();
			
			if (accountDelta.getChangeType() == ChangeType.ADD) {
				consolidateValuesAddAccount(context, accCtx, result);
			} else if (accountDelta.getChangeType() == ChangeType.MODIFY) {
				consolidateValuesModifyAccount(context, accCtx, result);
			}
			// TODO
			
		}
		
	}

	private void consolidateValuesAddAccount(SyncContext context, AccountSyncContext accCtx, OperationResult result) throws SchemaException {
		
		Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap = accCtx.getAttributeValueDeltaSetTripleMap();
		ResourceAccountType rat = accCtx.getResourceAccountType();
		ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();
		
		RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
		if (rAccount == null) {
			LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}",rat,context.dump());
			throw new IllegalStateException("Definition for account type "+rat+" not found in the context, but it should be there");
		}
		
		AccountShadowType newAccountType = rAccount.createBlankShadow();
		ObjectDefinition<AccountShadowType> accountTypeDefinition = rAccount.getObjectDefinition();
		MidPointObject<AccountShadowType> newAccount = accountTypeDefinition.parseObjectType(newAccountType);
		
		PropertyContainer attributesContainer = newAccount.findOrCreatePropertyContainer(SchemaConstants.I_ATTRIBUTES);
		
		Collection<QName> attrNames = new HashSet<QName>();
		attrNames.addAll(attributeValueDeltaMap.keySet());
		
		for (QName attributeName : attrNames) {
			RefinedAttributeDefinition refinedAttributeDefinition = rAccount.getAttributeDefinition(attributeName);
			ResourceObjectAttribute attr = refinedAttributeDefinition.instantiate();
			attributesContainer.add(attr);
			
			Collection<ValueConstruction> valueConstructions = null;
			DeltaSetTriple<ValueConstruction> deltaTriple = attributeValueDeltaMap.get(attributeName);
			
			if (deltaTriple != null) {
				valueConstructions = deltaTriple.getNonNegativeValues();
			}
			if (valueConstructions == null) {
				valueConstructions = new HashSet<ValueConstruction>();
			}
			
			// TODO: initial, exclusive
			
			for (ValueConstruction valueConstruction: valueConstructions) {
				Property output = valueConstruction.getOutput();
				attr.addValues(output.getValues());
			}			
			
		}
		
		accountDelta.setObjectToAdd(newAccount);
		context.setAccountSecondaryDelta(rat, accountDelta);
		
	}

	private void consolidateValuesModifyAccount(SyncContext context, AccountSyncContext accCtx,
			OperationResult result) throws SchemaException {
		
		Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap = accCtx.getAttributeValueDeltaSetTripleMap();
		ResourceAccountType rat = accCtx.getResourceAccountType();
		ObjectDelta<AccountShadowType> accountDelta = accCtx.getAccountDelta();
		
		RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
		if (rAccount == null) {
			LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}",rat,context.dump());
			throw new IllegalStateException("Definition for account type "+rat+" not found in the context, but it should be there");
		}
				
		PropertyPath parentPath = new PropertyPath(SchemaConstants.I_ATTRIBUTES);
		
		for (Entry<QName, DeltaSetTriple<ValueConstruction>> entry: attributeValueDeltaMap.entrySet()) {
			QName attributeName = entry.getKey();
			DeltaSetTriple<ValueConstruction> triple = entry.getValue();
			
			PropertyDelta propDelta = null;
			
			// TODO: initial, exclusive
			
			Collection<Object> allValues = collectAllValues(triple);
			for (Object value: allValues) {
				if (isValueInSet(value, triple.getZeroSet())) {
					// Value unchanged, nothing to do
					continue;
				}
				boolean isInPlusSet = isValueInSet(value, triple.getPlusSet());
				boolean isInMinusSet = isValueInSet(value, triple.getMinusSet());
				if (isInPlusSet && isInMinusSet) {
					// Value added and removed. Ergo no change.
					continue;
				}
				if (propDelta == null) {
					propDelta = new PropertyDelta(parentPath, attributeName);
				}
				if (isInPlusSet) {
					propDelta.addValueToAdd(value);
				}
				if (isInMinusSet) {
					propDelta.addValueToDelete(value);
				}
			}
			
			if (propDelta != null) {
				accountDelta.addModification(propDelta);
			}
			
		}

		
	}

	private Collection<Object> collectAllValues(DeltaSetTriple<ValueConstruction> triple) {
		Collection<Object> allValues = new HashSet<Object>();
		collectAllValuesFromSet(allValues, triple.getZeroSet());
		collectAllValuesFromSet(allValues, triple.getPlusSet());
		collectAllValuesFromSet(allValues, triple.getMinusSet());
		return allValues;
	}

	private void collectAllValuesFromSet(Collection<Object> allValues, Collection<ValueConstruction> set) {
		if (set == null) {
			return;
		}
		for (ValueConstruction valConstr: set) {
			collectAllValuesFromValueConstruction(allValues, valConstr);
		}
	}

	private void collectAllValuesFromValueConstruction(Collection<Object> allValues,
			ValueConstruction valConstr) {
		Property output = valConstr.getOutput();
		if (output == null) {
			return;
		}
		allValues.addAll(output.getValues());
	}

	private boolean isValueInSet(Object value, Collection<ValueConstruction> set) {
		// Stupid implementation, but easy to write. TODO: optimize
		Collection<Object> allValues = new HashSet<Object>();
		collectAllValuesFromSet(allValues, set);
		return allValues.contains(value);
	}
	
}
