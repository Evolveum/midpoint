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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.bcel.generic.ACONST_NULL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.DeltaSetTriple;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.ObjectDelta;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyDelta;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author semancik
 *
 */
@Component
public class AssignmentProcessor {
	
	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private ObjectResolver objectResolver;
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
	@Autowired(required=true)
	private ValueConstructionFactory valueConstructionFactory;
	
	private static final Trace LOGGER = TraceManager.getTrace(AssignmentProcessor.class);

	public void processAssignments(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		
		Collection<AssignmentType> assignmentsOld = null;
		if (context.getUserTypeOld() != null) {
			assignmentsOld = context.getUserTypeOld().getAssignment();
		} else {
			assignmentsOld = new HashSet<AssignmentType>();
		}
		
		PropertyDelta assignmentDelta = context.getAssignmentDelta();
		
		LOGGER.trace("Assignment delta {}",assignmentDelta.dump());
		
		// TODO: preprocess assignment delta. It is is replace, then we need to convert it to: delete all existing assignments, add all new assignments
		Collection<AssignmentType> changedAssignments = assignmentDelta.getValues(AssignmentType.class);
		
		AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
		assignmentEvaluator.setRepository(repositoryService);
		assignmentEvaluator.setUser(context.getUserNew());
		assignmentEvaluator.setObjectResolver(objectResolver);
		assignmentEvaluator.setSchemaRegistry(schemaRegistry);
		assignmentEvaluator.setValueConstructionFactory(valueConstructionFactory);
		
		Map<ResourceAccountType,Collection<AccountConstruction>> zeroAccountMap = new HashMap<ResourceAccountType, Collection<AccountConstruction>>();
		Map<ResourceAccountType,Collection<AccountConstruction>> plusAccountMap = new HashMap<ResourceAccountType, Collection<AccountConstruction>>();
		Map<ResourceAccountType,Collection<AccountConstruction>> minusAccountMap = new HashMap<ResourceAccountType, Collection<AccountConstruction>>();
		
		LOGGER.trace("Old assignments {}",DebugUtil.prettyPrint(assignmentsOld));
		LOGGER.trace("Changed assignments {}",DebugUtil.prettyPrint(changedAssignments));

		ObjectType source = context.getUserTypeOld();
		if (source == null) {
			source = context.getUserNew().getOrParseObjectType(); 
		}
		
		Collection<AssignmentType> newAssignments = new HashSet<AssignmentType>();
		Collection<AssignmentType> allAssignments = MiscUtil.union(assignmentsOld, changedAssignments);
		for (AssignmentType assignmentType : allAssignments) {
		
			LOGGER.trace("Processing assignment {}",DebugUtil.prettyPrint(assignmentType));
			
			Assignment evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, source, result);
			
			if (assignmentsOld.contains(assignmentType)) {
				// TODO: remember old state
			}
			
			context.rememberResources(evaluatedAssignment.getResources(result));
			
			// Sort assignments to sets: unchanged (zero), added (plus), removed (minus)
			if (changedAssignments.contains(assignmentType)) {
				// There was some change
				
				if (assignmentDelta.isValueToAdd(assignmentType)) {
					collectToAccountMap(plusAccountMap, evaluatedAssignment, result);
				}
				if (assignmentDelta.isValueToDelete(assignmentType)) {
					collectToAccountMap(minusAccountMap, evaluatedAssignment, result);
				}
				
			} else {
				// No change in assignment
				collectToAccountMap(zeroAccountMap, evaluatedAssignment, result);
				newAssignments.add(assignmentType);
			}
		}
		
		if (LOGGER.isTraceEnabled()) {
			// Dump the maps
			LOGGER.trace("Account maps:\nZERO:\n{}\nPLUS:\n{}\nMINUS:\n{}\n", new Object[]{dumpAccountMap(zeroAccountMap),
					dumpAccountMap(plusAccountMap), dumpAccountMap(minusAccountMap)});
		}
		
		Collection<ResourceAccountType> allAccountTypes = MiscUtil.union(zeroAccountMap.keySet(), plusAccountMap.keySet(), minusAccountMap.keySet());
		for (ResourceAccountType rat: allAccountTypes) {
			
			if (rat.getResourceOid() == null) {
				throw new IllegalStateException("Resource OID null in ResourceAccountType during outbound processing");
			}
			if (rat.getAccountType() == null) {
				throw new IllegalStateException("Account type is null in ResourceAccountType during outbound processing");
			}
			
			DeltaSetTriple<AccountConstruction> accountDeltaSetTriple = new DeltaSetTriple<AccountConstruction>(zeroAccountMap.get(rat),
					plusAccountMap.get(rat), minusAccountMap.get(rat));
			
			Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap = computeAttributeValueDeltaMap(accountDeltaSetTriple);
//			System.out.println("BBB1: "+accountDeltaSetTriple.dump());
//			System.out.println("BBB2: "+attributeValueDeltaMap);
			
			if (zeroAccountMap.containsKey(rat)) {
				// The account existed before the change and should still exist
				processAccountModification(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
				continue;
			}
			
			if (plusAccountMap.containsKey(rat) && minusAccountMap.containsKey(rat)) {
				// Account was removed and added in the same operation, therefore keep its original state
				// TODO
				throw new UnsupportedOperationException("add+delete of account is not supprted yet");
				//continue;
			}
			
			if (plusAccountMap.containsKey(rat)) {
				// Account added
				processAccountAddition(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
				continue;
			}
			
			if (minusAccountMap.containsKey(rat)) {
				// Account removed
				processAccountDeletion(context, rat, accountDeltaSetTriple, attributeValueDeltaMap, result);
				continue;
			}
			
		}
	}	

	private void collectToAccountMap(Map<ResourceAccountType, Collection<AccountConstruction>> accountMap,
			Assignment evaluatedAssignment, OperationResult result) throws ObjectNotFoundException, SchemaException {
		for (AccountConstruction accountConstruction: evaluatedAssignment.getAccountConstructions()) {
			String resourceOid = accountConstruction.getResource(result).getOid();
			String accountType = accountConstruction.getAccountType();
			ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType);
			Collection<AccountConstruction> constructions = null;
			if (accountMap.containsKey(rat)) {
				constructions = accountMap.get(rat);
			} else {
				constructions = new HashSet<AccountConstruction>();
				accountMap.put(rat,constructions);
			}
			constructions.add(accountConstruction);
		}
	}

	private String dumpAccountMap(Map<ResourceAccountType, Collection<AccountConstruction>> accountMap) {
		StringBuilder sb = new StringBuilder();
		Set<Entry<ResourceAccountType, Collection<AccountConstruction>>> entrySet = accountMap.entrySet();
		Iterator<Entry<ResourceAccountType, Collection<AccountConstruction>>> i = entrySet.iterator();
		while(i.hasNext()) {
			Entry<ResourceAccountType, Collection<AccountConstruction>> entry = i.next();
			sb.append(entry.getKey()).append(": ");
			sb.append(DebugUtil.prettyPrint(entry.getValue()));
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private void processAccountAddition(SyncContext context, ResourceAccountType rat, 
			DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
			Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap,
			OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		 
		RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
		if (rAccount == null) {
			LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}",rat,context.dump());
			throw new IllegalStateException("Definition for account type "+rat+" not found in the context, but it should be there");
		}
		ObjectDelta<AccountShadowType> accountDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.ADD);
		// No need for OID. This is a new object, OID will be assigned by the repo
		
		AccountShadowType newAccountType = rAccount.createBlankShadow();
		ObjectDefinition<AccountShadowType> accountTypeDefinition = rAccount.getObjectDefinition();
		MidPointObject<AccountShadowType> newAccount = accountTypeDefinition.parseObjectType(newAccountType);
		
		PropertyContainer attributesContainer = newAccount.findOrCreatePropertyContainer(SchemaConstants.I_ATTRIBUTES);
		
		Collection<QName> attrNames = new HashSet<QName>();
		attrNames.addAll(attributeValueDeltaMap.keySet());
		attrNames.addAll(rAccount.getNamesOfAttributesWithOutboundExpressions());
		
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
			
			ValueConstruction evaluatedOutboundAccountConstruction = evaluateOutboundAccountConstruction(context, refinedAttributeDefinition, rAccount, result);
			if (evaluatedOutboundAccountConstruction != null) {
				valueConstructions.add(evaluatedOutboundAccountConstruction);
			}
			
			for (ValueConstruction valueConstruction: valueConstructions) {
				Property output = valueConstruction.getOutput();
				attr.addValues(output.getValues());
			}			
			
		}
		
		// TODO: compute attributes
		
		accountDelta.setObjectToAdd(newAccount);
		context.setAccountSecondaryDelta(rat, accountDelta);
		
	}


	private void processAccountModification(SyncContext context,
			ResourceAccountType rat, DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
			Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap, OperationResult result) throws SchemaException {

		RefinedAccountDefinition rAccount = context.getRefinedAccountDefinition(rat, schemaRegistry);
		if (rAccount == null) {
			LOGGER.error("Definition for account type {} not found in the context, but it should be there, dumping context:\n{}",rat,context.dump());
			throw new IllegalStateException("Definition for account type "+rat+" not found in the context, but it should be there");
		}
		
		ObjectDelta<AccountShadowType> accountDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.MODIFY);
		// TODO: oid
		
		context.setAccountSecondaryDelta(rat, accountDelta);
		PropertyPath parentPath = new PropertyPath(SchemaConstants.I_ATTRIBUTES);
		
		for (Entry<QName, DeltaSetTriple<ValueConstruction>> entry: attributeValueDeltaMap.entrySet()) {
			QName attributeName = entry.getKey();
			DeltaSetTriple<ValueConstruction> triple = entry.getValue();
			
			PropertyDelta propDelta = null;
			
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
	
	private boolean isValueInSet(Object value, Collection<ValueConstruction> set) {
		// Stupid implementation, but easy to write. TODO: optimize
		Collection<Object> allValues = new HashSet<Object>();
		collectAllValuesFromSet(allValues, set);
		return allValues.contains(value);
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

	private void processAccountDeletion(SyncContext context, ResourceAccountType rat,
			DeltaSetTriple<AccountConstruction> accountDeltaSetTriple,
			Map<QName, DeltaSetTriple<ValueConstruction>> attributeValueDeltaMap, OperationResult result) {
		
		ObjectDelta<AccountShadowType> accountDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.DELETE);
		// TODO: oid
		context.setAccountSecondaryDelta(rat, accountDelta);
		
		// TODO: compute attributes
		
	}
	
	private ValueConstruction evaluateOutboundAccountConstruction(SyncContext context, RefinedAttributeDefinition refinedAttributeDefinition, 
			RefinedAccountDefinition refinedAccountDefinition, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		ValueConstructionType outboundValueConstructionType = refinedAttributeDefinition.getOutboundValueConstructionType();
		if (outboundValueConstructionType == null) {
			return null;
		}
		
		ValueConstruction valueConstruction = valueConstructionFactory.createValueConstruction(outboundValueConstructionType, refinedAttributeDefinition, 
				"outbound expression for "+refinedAttributeDefinition.getName()+" in "+ObjectTypeUtil.toShortString(refinedAccountDefinition.getResourceType()));
		
		// FIXME: should be userNew, but that is not yet filled in
		valueConstruction.addVariableDefinition(ExpressionConstants.VAR_USER, context.getUserOld());
		// TODO: variables
		
		valueConstruction.evaluate(result);
		
		return valueConstruction;
	}
	
	private Map<QName, DeltaSetTriple<ValueConstruction>> computeAttributeValueDeltaMap(DeltaSetTriple<AccountConstruction> accountDeltaSetTriple) {
		
		Map<QName,DeltaSetTriple<ValueConstruction>> attrMap = new HashMap<QName, DeltaSetTriple<ValueConstruction>>();
		
		for (AccountConstruction ac: accountDeltaSetTriple.union()) { 
			
			for (ValueConstruction attrConstr: ac.getAttributeConstructions()) {
				
				Property output = attrConstr.getOutput();
				QName attrName = output.getName();
				DeltaSetTriple<ValueConstruction> valueTriple = attrMap.get(attrName);
				if (valueTriple == null) {
					valueTriple = new DeltaSetTriple<ValueConstruction>();
					attrMap.put(attrName, valueTriple);
				}
				valueTriple.distributeAs(attrConstr, accountDeltaSetTriple, ac);
				
			}
		}
		return attrMap;
	}
	
	
}
