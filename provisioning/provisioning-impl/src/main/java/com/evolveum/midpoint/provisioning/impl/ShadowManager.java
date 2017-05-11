/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Responsibilities:
 *     Communicate with the repo
 *     Store results in the repo shadows
 *     Clean up shadow inconsistencies in repo
 *     
 * Limitations:
 *     Do NOT communicate with the resource
 *     means: do NOT do anything with the connector
 * 
 * @author Katarina Valalikova
 * @author Radovan Semancik
 *
 */
@Component
public class ShadowManager {
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	@Autowired(required = true)
	private Clock clock;

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private TaskManager taskManager;

	@Autowired(required = true)
	private MatchingRuleRegistry matchingRuleRegistry;
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowManager.class);
		
	
	public void deleteConflictedShadowFromRepo(PrismObject<ShadowType> shadow, OperationResult parentResult){
		
		try{
			
			repositoryService.deleteObject(shadow.getCompileTimeClass(), shadow.getOid(), parentResult);
		
		} catch (Exception ex){
			throw new SystemException(ex.getMessage(), ex);
		}
		
	}
	
	public ResourceOperationDescription createResourceFailureDescription(
			PrismObject<ShadowType> conflictedShadow, ResourceType resource, OperationResult parentResult){
		ResourceOperationDescription failureDesc = new ResourceOperationDescription();
		failureDesc.setCurrentShadow(conflictedShadow);
		ObjectDelta<ShadowType> objectDelta = null;
		if (FailedOperationTypeType.ADD == conflictedShadow.asObjectable().getFailedOperationType()) {
			objectDelta = ObjectDelta.createAddDelta(conflictedShadow);
		} 
		failureDesc.setObjectDelta(objectDelta);
		failureDesc.setResource(resource.asPrismObject());
		failureDesc.setResult(parentResult);
		failureDesc.setSourceChannel(SchemaConstants.CHANGE_CHANNEL_DISCOVERY.getLocalPart());
		
		return failureDesc;
	}

	/**
	 * Locates the appropriate Shadow in repository that corresponds to the
	 * provided resource object.
	 *
	 * DEAD flag is cleared - in memory as well as in repository.
	 * 
	 * @param parentResult
	 * 
	 * @return current shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 */
	public PrismObject<ShadowType> lookupShadowInRepository(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		ObjectQuery query = createSearchShadowQuery(ctx, resourceShadow, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter:\n{}",
					query.debugDump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		 List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		 MiscSchemaUtil.reduceSearchResult(results);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<ShadowType> result : results) {
				LOGGER.trace("Search result:\n{}", result.debugDump());
			}
			LOGGER.error("More than one shadow found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadow found for " + resourceShadow);
		}
		PrismObject<ShadowType> shadow = results.get(0);
		checkConsistency(shadow);

		if (Boolean.TRUE.equals(shadow.asObjectable().isDead())) {
			LOGGER.debug("Repository shadow {} is marked as dead - resetting the flag", ObjectTypeUtil.toShortString(shadow));
			shadow.asObjectable().setDead(false);
			List<ItemDelta<?, ?>> deltas = DeltaBuilder.deltaFor(ShadowType.class, prismContext).item(ShadowType.F_DEAD).replace().asItemDeltas();
			try {
				repositoryService.modifyObject(ShadowType.class, shadow.getOid(), deltas, parentResult);
			} catch (ObjectAlreadyExistsException e) {
				throw new SystemException("Unexpected exception when resetting 'dead' flag: " + e.getMessage(), e);
			}
		}
		return shadow;
	}

	public PrismObject<ShadowType> lookupShadowInRepository(ProvisioningContext ctx, ResourceAttributeContainer identifierContainer,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		ObjectQuery query = createSearchShadowQuery(ctx, identifierContainer.getValue().getItems(), false, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter (repo):\n{}",
					query.debugDump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<ShadowType>> results;

		results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		MiscSchemaUtil.reduceSearchResult(results);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			LOGGER.error("More than one shadow found in repository for " + identifierContainer);
			if (LOGGER.isDebugEnabled()) {
				for (PrismObject<ShadowType> result : results) {
					LOGGER.debug("Conflicting shadow (repo):\n{}", result.debugDump());
				}
			}
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found in repository for " + identifierContainer);
		}

		PrismObject<ShadowType> shadow = results.get(0);
		checkConsistency(shadow);
		return shadow;
	}

	public PrismObject<ShadowType> lookupConflictingShadowBySecondaryIdentifiers( 
			ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(resourceShadow);
		List<PrismObject<ShadowType>> results = lookupShadowsBySecondaryIdentifiers(ctx, secondaryIdentifiers, parentResult);
		
		if (results == null || results.size() == 0) {
			return null;
		}

		List<PrismObject<ShadowType>> conflictingShadows = new ArrayList<PrismObject<ShadowType>>();
		for (PrismObject<ShadowType> shadow: results){
			ShadowType repoShadowType = shadow.asObjectable();
			if (shadow != null) {
				if (repoShadowType.getFailedOperationType() == null){
					LOGGER.trace("Found shadow is ok, returning null");
					continue;
				} 
				if (repoShadowType.getFailedOperationType() != null && FailedOperationTypeType.ADD != repoShadowType.getFailedOperationType()){
					continue;
				}
				conflictingShadows.add(shadow);
			}
		}
		
		if (conflictingShadows.isEmpty()){
			return null;
		}
		
		if (conflictingShadows.size() > 1) {
			for (PrismObject<ShadowType> result : conflictingShadows) {
				LOGGER.trace("Search result:\n{}", result.debugDump());
			}
			LOGGER.error("More than one shadow found for " + secondaryIdentifiers);
			if (LOGGER.isDebugEnabled()) {
				for (PrismObject<ShadowType> conflictingShadow: conflictingShadows) {
					LOGGER.debug("Conflicting shadow:\n{}", conflictingShadow.debugDump());
				}
			}
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + secondaryIdentifiers);
		}

		PrismObject<ShadowType> shadow = conflictingShadows.get(0);
		checkConsistency(shadow);
		return shadow;

	}
	
	
	public PrismObject<ShadowType> lookupShadowBySecondaryIdentifiers( 
			ProvisioningContext ctx, Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		List<PrismObject<ShadowType>> shadows = lookupShadowsBySecondaryIdentifiers(ctx, secondaryIdentifiers, parentResult);
		if (shadows == null || shadows.isEmpty()) {
			return null;
		}
		if (shadows.size() > 1) {
			LOGGER.error("Too many shadows ({}) for secondary identifiers {}: ", shadows.size(), secondaryIdentifiers, 
					shadows);
			throw new ConfigurationException("Too many shadows ("+shadows.size()+") for secondary identifiers "+secondaryIdentifiers);
		}
		return shadows.get(0);
	}

		
	private List<PrismObject<ShadowType>> lookupShadowsBySecondaryIdentifiers( 
			ProvisioningContext ctx, Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
			
		if (secondaryIdentifiers.size() < 1){
			LOGGER.trace("Shadow does not contain secondary identifier. Skipping lookup shadows according to name.");
			return null;
		}

		S_FilterEntry q = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.block();
		for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
			// There may be identifiers that come from associations and they will have parent set to association/identifiers
			// For the search to succeed we need all attribute to have "attributes" parent path.
			secondaryIdentifier = ShadowUtil.fixAttributePath(secondaryIdentifier);
			q = q.item(secondaryIdentifier.getPath(), secondaryIdentifier.getDefinition())
					.eq(getNormalizedValue(secondaryIdentifier, ctx.getObjectClassDefinition()))
					.or();
		}
		ObjectQuery query = q.none().endBlock()
				.and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
				.build();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter on secondary identifier:\n{}",
					query.debugDump());
		}

		// TODO: check for errors
		List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		MiscSchemaUtil.reduceSearchResult(results);

		LOGGER.trace("lookupShadow found {} objects", results.size());
		if (LOGGER.isTraceEnabled() && results.size() == 1) {
			LOGGER.trace("lookupShadow found\n{}", results.get(0).debugDump(1));
		}
		
		return results;

	}

	private void checkConsistency(PrismObject<ShadowType> shadow) {
		ProvisioningUtil.checkShadowActivationConsistency(shadow);
	}
	
	private <T> ObjectFilter createAttributeEqualFilter(ProvisioningContext ctx,
			ResourceAttribute<T> secondaryIdentifier) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(secondaryIdentifier.getPath(), secondaryIdentifier.getDefinition())
				.eq(getNormalizedValue(secondaryIdentifier, ctx.getObjectClassDefinition()))
				.buildFilter();
	}
	
	private <T> List<PrismPropertyValue<T>> getNormalizedValue(PrismProperty<T> attr, RefinedObjectClassDefinition rObjClassDef) throws SchemaException {
		RefinedAttributeDefinition<T> refinedAttributeDefinition = rObjClassDef.findAttributeDefinition(attr.getElementName());
		QName matchingRuleQName = refinedAttributeDefinition.getMatchingRuleQName();
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, refinedAttributeDefinition.getTypeName());
		List<PrismPropertyValue<T>> normalized = new ArrayList<>();
		for (PrismPropertyValue<T> origPValue : attr.getValues()){
			T normalizedValue = matchingRule.normalize(origPValue.getValue());
			PrismPropertyValue<T> normalizedPValue = origPValue.clone();
			normalizedPValue.setValue(normalizedValue);
			normalized.add(normalizedPValue);
		}
		return normalized;
		
	}

    // beware, may return null if an shadow that was to be marked as DEAD, was deleted in the meantime
	public PrismObject<ShadowType> findOrAddShadowFromChange(ProvisioningContext ctx, Change change,
			OperationResult parentResult) throws SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<ShadowType>> accountList = searchShadowByIdenifiers(ctx, change, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one shadow with the identifier " + change.getIdentifiers() + ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		PrismObject<ShadowType> newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null || change.getObjectDelta().getChangeType() != ChangeType.DELETE) {
				newShadow = createNewShadowFromChange(ctx, change, parentResult);

				try {
					ConstraintsChecker.onShadowAddOperation(newShadow.asObjectable());
					String oid = repositoryService.addObject(newShadow, null, parentResult);
					newShadow.setOid(oid);
					if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
						change.getObjectDelta().setOid(oid);
					}
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add " + SchemaDebugUtil.prettyPrint(newShadow)
							+ " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.debug("Added new shadow (from change): {}", newShadow);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Added new shadow (from change):\n{}", newShadow.debugDump());
				}
			}

		} else {
			// Account was found in repository
			newShadow = accountList.get(0);
			
            if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
					Collection<? extends ItemDelta> deadDeltas = PropertyDelta
							.createModificationReplacePropertyCollection(ShadowType.F_DEAD,
									newShadow.getDefinition(), true);
					try {
						ConstraintsChecker.onShadowModifyOperation(deadDeltas);
						repositoryService.modifyObject(ShadowType.class, newShadow.getOid(), deadDeltas,
								parentResult);
					} catch (ObjectAlreadyExistsException e) {
						parentResult.recordFatalError(
								"Can't add " + SchemaDebugUtil.prettyPrint(newShadow)
										+ " to the repository. Reason: " + e.getMessage(), e);
						throw new IllegalStateException(e.getMessage(), e);
					} catch (ObjectNotFoundException e) {
						parentResult.recordWarning("Shadow " + SchemaDebugUtil.prettyPrint(newShadow)
								+ " was probably deleted from the repository in the meantime. Exception: "
								+ e.getMessage(), e);
						return null;
					}
				} 
				
			
			
		}

		return newShadow;
	}
	
	public PrismObject<ShadowType> findOrAddShadowFromChangeGlobalContext(ProvisioningContext globalCtx, Change change,
			OperationResult parentResult) throws SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<ShadowType>> accountList = searchShadowByIdenifiers(globalCtx, change, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one shadow with the identifier " + change.getIdentifiers() + ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		PrismObject<ShadowType> newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null || change.getObjectDelta().getChangeType() != ChangeType.DELETE) {
				newShadow = createNewShadowFromChange(globalCtx, change, parentResult);

				try {
					ConstraintsChecker.onShadowAddOperation(newShadow.asObjectable());
					String oid = repositoryService.addObject(newShadow, null, parentResult);
					newShadow.setOid(oid);
					if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
						change.getObjectDelta().setOid(oid);
					}
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add " + SchemaDebugUtil.prettyPrint(newShadow)
							+ " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.debug("Added new shadow (from global change): {}", newShadow);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Added new shadow (from global change):\n{}", newShadow.debugDump());
				}
			}

		} else {
			// Account was found in repository
			newShadow = accountList.get(0);
			
            if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
					Collection<? extends ItemDelta> deadDeltas = PropertyDelta
							.createModificationReplacePropertyCollection(ShadowType.F_DEAD,
									newShadow.getDefinition(), true);
					try {
						ConstraintsChecker.onShadowModifyOperation(deadDeltas);
						repositoryService.modifyObject(ShadowType.class, newShadow.getOid(), deadDeltas,
								parentResult);
					} catch (ObjectAlreadyExistsException e) {
						parentResult.recordFatalError(
								"Can't add " + SchemaDebugUtil.prettyPrint(newShadow)
										+ " to the repository. Reason: " + e.getMessage(), e);
						throw new IllegalStateException(e.getMessage(), e);
					} catch (ObjectNotFoundException e) {
						parentResult.recordWarning("Shadow " + SchemaDebugUtil.prettyPrint(newShadow)
								+ " was probably deleted from the repository in the meantime. Exception: "
								+ e.getMessage(), e);
						return null;
					}
				} 
				
			
			
		}

		return newShadow;
	}
	
	private PrismObject<ShadowType> createNewShadowFromChange(ProvisioningContext ctx, Change change,
			OperationResult parentResult) throws SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException {

		PrismObject<ShadowType> shadow = change.getCurrentShadow();
		
		if (shadow == null){
			//try to look in the delta, if there exists some account to be added
			if (change.getObjectDelta() != null && change.getObjectDelta().isAdd()){
				shadow = (PrismObject<ShadowType>) change.getObjectDelta().getObjectToAdd();
			}
		}
		
		if (shadow == null){
			throw new IllegalStateException("Could not create shadow from change description. Neither current shadow, nor delta containing shadow exits.");
		}
		
		try {
			shadow = createRepositoryShadow(ctx, shadow);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't create shadow from identifiers: "
					+ change.getIdentifiers());
			throw new SchemaException("Can't create shadow from identifiers: "
					+ change.getIdentifiers());
		}

		parentResult.recordSuccess();
		return shadow;
	}
	
	private List<PrismObject<ShadowType>> searchShadowByIdenifiers(ProvisioningContext ctx, Change change, OperationResult parentResult)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
		ObjectQuery query = createSearchShadowQuery(ctx, identifiers, true, prismContext, parentResult);

		List<PrismObject<ShadowType>> accountList = null;
		try {
			accountList = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search shadow according to the identifiers: " + identifiers + ". Reason: "
							+ ex.getMessage(), ex);
			throw new SchemaException("Failed to search shadow according to the identifiers: "
					+ identifiers + ". Reason: " + ex.getMessage(), ex);
		}
		MiscSchemaUtil.reduceSearchResult(accountList);
		return accountList;
	}
	
	private ObjectQuery createSearchShadowQuery(ProvisioningContext ctx, Collection<ResourceAttribute<?>> identifiers, boolean primaryIdentifiersOnly,
			PrismContext prismContext, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		S_AtomicFilterEntry q = QueryBuilder.queryFor(ShadowType.class, prismContext);

		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		for (PrismProperty<?> identifier : identifiers) {
			RefinedAttributeDefinition rAttrDef;
			PrismPropertyValue<?> identifierValue = identifier.getValue();
			if (objectClassDefinition == null) {
				// If there is no specific object class definition then the identifier definition 
				// must be the same in all object classes and that means that we can use
				// definition from any of them.
				RefinedObjectClassDefinition anyDefinition = ctx.getRefinedSchema().getRefinedDefinitions().iterator().next();
				rAttrDef = anyDefinition.findAttributeDefinition(identifier.getElementName());
				if (primaryIdentifiersOnly && !anyDefinition.isPrimaryIdentifier(identifier.getElementName())) {
					continue;
				}
			} else {
				if (primaryIdentifiersOnly && !objectClassDefinition.isPrimaryIdentifier(identifier.getElementName())) {
					continue;
				}
				rAttrDef = objectClassDefinition.findAttributeDefinition(identifier.getElementName());
			}

			String normalizedIdentifierValue = (String) getNormalizedAttributeValue(identifierValue, rAttrDef);
			PrismPropertyDefinition<String> def = (PrismPropertyDefinition<String>) identifier.getDefinition();
			q = q.itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getName()).eq(normalizedIdentifierValue).and();
		}

		if (identifiers.size() < 1) {
			throw new SchemaException("Identifier not specified. Cannot create search query by identifier.");
		}

		if (objectClassDefinition != null) {
			q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectClassDefinition.getTypeName()).and();
		}
		return q.item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid()).build();
	}

	private ObjectQuery createSearchShadowQuery(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow, 
			PrismContext prismContext, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceShadow);
		PrismProperty identifier = attributesContainer.getPrimaryIdentifier();

		Collection<PrismPropertyValue<Object>> idValues = identifier.getValues();
		// Only one value is supported for an identifier
		if (idValues.size() > 1) {
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("More than one identifier value is not supported");
		}
		if (idValues.size() < 1) {
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("The identifier has no value");
		}

		// We have all the data, we can construct the filter now
		try {
			// TODO TODO TODO TODO: set matching rule instead of null
			PrismPropertyDefinition def = identifier.getDefinition();
			return QueryBuilder.queryFor(ShadowType.class, prismContext)
					.itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getName()).eq(getNormalizedValue(identifier, ctx.getObjectClassDefinition()))
					.and().item(ShadowType.F_OBJECT_CLASS).eq(resourceShadow.getPropertyRealValue(ShadowType.F_OBJECT_CLASS, QName.class))
					.and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
					.build();
		} catch (SchemaException e) {
			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
		}
	}
	
	public SearchResultMetadata searchObjectsIterativeRepository(
			ProvisioningContext ctx, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			com.evolveum.midpoint.schema.ResultHandler<ShadowType> repoHandler, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		
		ObjectQuery repoQuery = query.clone();
		processQueryMatchingRules(repoQuery, ctx.getObjectClassDefinition());
		
		return repositoryService.searchObjectsIterative(ShadowType.class, repoQuery, repoHandler, options, false, parentResult);	// TODO think about strictSequential flag
	}

	/**
	 * Visit the query and normalize values (or set matching rules) as needed
	 */
	private void processQueryMatchingRules(ObjectQuery repoQuery, final RefinedObjectClassDefinition objectClassDef) {
		ObjectFilter filter = repoQuery.getFilter();
		Visitor visitor = f -> {
			try {
				processQueryMatchingRuleFilter(f, objectClassDef);
			} catch (SchemaException e) {
				throw new SystemException(e);
			}
		};
		filter.accept(visitor);
	}
	
	private <T> void processQueryMatchingRuleFilter(ObjectFilter filter, RefinedObjectClassDefinition objectClassDef) throws SchemaException {
		if (!(filter instanceof EqualFilter)) {
			return;
		}
		EqualFilter<T> eqFilter = (EqualFilter)filter;
		ItemPath parentPath = eqFilter.getParentPath();
		if (parentPath == null || !parentPath.equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
			return;
		}
		QName attrName = eqFilter.getElementName();
		RefinedAttributeDefinition rAttrDef = objectClassDef.findAttributeDefinition(attrName);
		if (rAttrDef == null) {
			throw new SchemaException("Unknown attribute "+attrName+" in filter "+filter);
		}
		QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
		if (matchingRuleQName == null) {
			return;
		}
		Class<?> valueClass = null;
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
		List<PrismValue> newValues = new ArrayList<PrismValue>();
		if (eqFilter.getValues() != null) {
			for (PrismPropertyValue<T> ppval : eqFilter.getValues()) {
				T normalizedRealValue = matchingRule.normalize(ppval.getValue());
				PrismPropertyValue<T> newPPval = ppval.clone();
				newPPval.setValue(normalizedRealValue);
				newValues.add(newPPval);
				if (normalizedRealValue != null) {
					valueClass = normalizedRealValue.getClass();
				}
			}
			eqFilter.getValues().clear();
			eqFilter.getValues().addAll((Collection) newValues);
			LOGGER.trace("Replacing values for attribute {} in search filter with normalized values because there is a matching rule, normalized values: {}",
					attrName, newValues);
		}
		if (eqFilter.getMatchingRule() == null) {
			QName supportedMatchingRule = valueClass != null ?
					repositoryService.getApproximateSupportedMatchingRule(valueClass, matchingRuleQName) : matchingRuleQName;
			eqFilter.setMatchingRule(supportedMatchingRule);
			LOGGER.trace("Setting matching rule to {} (supported by repo as a replacement for {} to search for {})",
					supportedMatchingRule, matchingRuleQName, valueClass);
		}
	}

	// Used when new resource object is discovered
	public PrismObject<ShadowType> addDiscoveredRepositoryShadow(ProvisioningContext ctx,
			PrismObject<ShadowType> resourceShadow, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Adding new shadow from resource object: {}", resourceShadow.debugDump());
		}
		PrismObject<ShadowType> repoShadow = createRepositoryShadow(ctx, resourceShadow);
		ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable());
		String oid = repositoryService.addObject(repoShadow, null, parentResult);
		repoShadow.setOid(oid);
		LOGGER.debug("Added new shadow (from resource object): {}", repoShadow);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Added new shadow (from resource object):\n{}", repoShadow.debugDump());
		}
		return repoShadow;
	}
	
	// Used after ADD operation on resource
	public String addNewRepositoryShadow(ProvisioningContext ctx, AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResult, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		PrismObject<ShadowType> resourceShadow = addResult.getReturnValue();
		PrismObject<ShadowType> repoShadow = createRepositoryShadow(ctx, resourceShadow);

		if (repoShadow == null) {
			parentResult
					.recordFatalError("Error while creating account shadow object to save in the reposiotory. Shadow is null.");
			throw new IllegalStateException(
					"Error while creating account shadow object to save in the reposiotory. Shadow is null.");
		}

		addPendingOperationAdd(repoShadow, addResult);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Adding repository shadow\n{}", repoShadow.debugDump());
		}
		String oid = null;
		try {
			ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable());
			oid = repositoryService.addObject(repoShadow, null, parentResult);

		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. The OID is not supplied and it is
			// generated by the repo
			// If it happens, it must be a repo bug. Therefore it is safe to
			// convert to runtime exception
			parentResult.recordFatalError(
					"Couldn't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException(
					"Couldn't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
		}
		repoShadow.setOid(oid);

		LOGGER.trace("Object added to the repository successfully.");

		parentResult.recordSuccess();

		return repoShadow.getOid();
	}
	
	private void addPendingOperationAdd(PrismObject<ShadowType> repoShadow, 
			AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResult) throws SchemaException {
		if (!addResult.isInProgress()) {
			return;
		}
		PrismObject<ShadowType> resourceShadow = addResult.getReturnValue();
		ShadowType repoShadowType = repoShadow.asObjectable();
		
		ObjectDelta<ShadowType> addDelta = resourceShadow.createAddDelta();
		ObjectDeltaType addDeltaType = DeltaConvertor.toObjectDeltaType(addDelta);
		
		PendingOperationType pendingOperation = new PendingOperationType();
		pendingOperation.setDelta(addDeltaType);
		pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
		pendingOperation.setResultStatus(OperationResultStatusType.IN_PROGRESS);
		pendingOperation.setAsynchronousOperationReference(addResult.getOperationResult().getAsynchronousOperationReference());
		repoShadowType.getPendingOperation().add(pendingOperation);
		repoShadowType.setExists(false);
	}

	private void addPendingOperationModify(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> pendingModifications, 
			OperationResult resourceOperationResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		
		ObjectDelta<ShadowType> pendingDelta = shadow.createModifyDelta();
		for (ItemDelta pendingModification: pendingModifications) {
			pendingDelta.addModification(pendingModification.clone());
		}
		
		addPendingOperationDelta(ctx, shadow, pendingDelta, resourceOperationResult, parentResult);
	}
		
	
	private void addPendingOperationDelete(ProvisioningContext ctx, PrismObject<ShadowType> oldRepoShadow,
			OperationResult resourceOperationResult, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		
		ObjectDelta<ShadowType> pendingDelta = oldRepoShadow.createDeleteDelta();
		
		addPendingOperationDelta(ctx, oldRepoShadow, pendingDelta, resourceOperationResult, parentResult);
	}

	private void addPendingOperationDelta(ProvisioningContext ctx, PrismObject<ShadowType> shadow, ObjectDelta<ShadowType> pendingDelta,
			OperationResult resourceOperationResult, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		ObjectDeltaType pendingDeltaType = DeltaConvertor.toObjectDeltaType(pendingDelta);
		
		PendingOperationType pendingOperation = new PendingOperationType();
		pendingOperation.setDelta(pendingDeltaType);
		pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
		pendingOperation.setResultStatus(OperationResultStatusType.IN_PROGRESS);
		pendingOperation.setAsynchronousOperationReference(resourceOperationResult.getAsynchronousOperationReference());
		
		Collection repoDeltas = new ArrayList<>(1);
		ContainerDelta<PendingOperationType> cdelta = ContainerDelta.createDelta(ShadowType.F_PENDING_OPERATION, shadow.getDefinition());
		cdelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
		repoDeltas.add(cdelta);
		
		try {
			repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}
	
	/**
	 * Create a copy of a shadow that is suitable for repository storage. 
	 */
	private PrismObject<ShadowType> createRepositoryShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);
		
		PrismObject<ShadowType> repoShadow = shadow.clone();
		ShadowType repoShadowType = repoShadow.asObjectable();
		ResourceAttributeContainer repoAttributesContainer = ShadowUtil
				.getAttributesContainer(repoShadow);

		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
		if (cachingStrategy == CachingStategyType.NONE) {
			// Clean all repoShadow attributes and add only those that should be
			// there
			repoAttributesContainer.clear();
			Collection<ResourceAttribute<?>> primaryIdentifiers = attributesContainer.getPrimaryIdentifiers();
			for (PrismProperty<?> p : primaryIdentifiers) {
				repoAttributesContainer.add(p.clone());
			}

			Collection<ResourceAttribute<?>> secondaryIdentifiers = attributesContainer.getSecondaryIdentifiers();
			for (PrismProperty<?> p : secondaryIdentifiers) {
				repoAttributesContainer.add(p.clone());
			}

			// Also add all the attributes that act as association identifiers.
			// We will need them when the shadow is deleted (to remove the shadow from entitlements).
			RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
			for (RefinedAssociationDefinition associationDef: objectClassDefinition.getAssociationDefinitions()) {
				if (associationDef.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
					QName valueAttributeName = associationDef.getResourceObjectAssociationType().getValueAttribute();
					if (repoAttributesContainer.findAttribute(valueAttributeName) == null) {
						ResourceAttribute<Object> valueAttribute = attributesContainer.findAttribute(valueAttributeName);
						if (valueAttribute != null) {
							repoAttributesContainer.add(valueAttribute.clone());
						}
					}
				}
			}

			repoShadowType.setCachingMetadata(null);

			ProvisioningUtil.cleanupShadowActivation(repoShadowType);

		} else if (cachingStrategy == CachingStategyType.PASSIVE) {
			// Do not need to clear anything. Just store all attributes and add metadata.
			CachingMetadataType cachingMetadata = new CachingMetadataType();
			cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
			repoShadowType.setCachingMetadata(cachingMetadata);

		} else {
			throw new ConfigurationException("Unknown caching strategy "+cachingStrategy);
		}

        setKindIfNecessary(repoShadowType, ctx.getObjectClassDefinition());
//        setIntentIfNecessary(repoShadowType, objectClassDefinition);

        // Store only password meta-data in repo
        CredentialsType creds = repoShadowType.getCredentials();
        if (creds != null) {
        	PasswordType passwordType = creds.getPassword();
        	if (passwordType != null) {
        		ProvisioningUtil.cleanupShadowPassword(passwordType);
        		PrismObject<UserType> owner = null;
        		if (ctx.getTask() != null) {
        			owner = ctx.getTask().getOwner();
        		}
        		ProvisioningUtil.addPasswordMetadata(passwordType, clock.currentTimeXMLGregorianCalendar(), owner);
        	}
        	// TODO: other credential types - later
        }

		// additional check if the shadow doesn't contain resource, if yes,
		// convert to the resource reference.
		if (repoShadowType.getResource() != null) {
			repoShadowType.setResource(null);
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource()));
		}

		// if shadow does not contain resource or resource reference, create it
		// now
		if (repoShadowType.getResourceRef() == null) {
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource()));
		}

		if (repoShadowType.getName() == null) {
			repoShadowType.setName(new PolyStringType(ShadowUtil.determineShadowName(shadow)));
		}

		if (repoShadowType.getObjectClass() == null) {
			repoShadowType.setObjectClass(attributesContainer.getDefinition().getTypeName());
		}
		
		if (repoShadowType.isProtectedObject() != null){
			repoShadowType.setProtectedObject(null);
		}
		
		normalizeAttributes(repoShadow, ctx.getObjectClassDefinition());

		return repoShadow;
	}
	
	public void modifyShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications, 
			OperationResult resourceOperationResult, OperationResult parentResult) 
			throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
		LOGGER.trace("Updating repository shadow, resourceOperationResult={}, {} modifications", resourceOperationResult.getStatus(), modifications.size());
		if (resourceOperationResult.isInProgress()) {
			addPendingOperationModify(ctx, shadow, modifications, resourceOperationResult, parentResult);
		} else {
			modifyShadowAttributes(ctx, shadow, modifications, parentResult);
		}
	}
		
	/**
	 * Really modifies shadow attributes. It applies the changes. It is used for synchronous operations and also for
	 * applying the results of completed asynchronous operations. 
	 */
	public void modifyShadowAttributes(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications, 
			OperationResult parentResult) 
			throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException { 
		Collection<? extends ItemDelta> shadowChanges = extractRepoShadowChanges(ctx, shadow, modifications);
		if (shadowChanges != null && !shadowChanges.isEmpty()) {
			LOGGER.trace(
					"Detected shadow changes. Start to modify shadow in the repository, applying modifications {}",
					DebugUtil.debugDump(shadowChanges));
			try {
				ConstraintsChecker.onShadowModifyOperation(shadowChanges);
				repositoryService.modifyObject(ShadowType.class, shadow.getOid(), shadowChanges, parentResult);
				LOGGER.trace("Shadow changes processed successfully.");
			} catch (ObjectAlreadyExistsException ex) {
				throw new SystemException(ex);
			}
		}	
	}
	
	@SuppressWarnings("rawtypes")
	private Collection<? extends ItemDelta> extractRepoShadowChanges(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> objectChange)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
		Collection<ItemDelta> repoChanges = new ArrayList<ItemDelta>();
		for (ItemDelta itemDelta : objectChange) {
			if (new ItemPath(ShadowType.F_ATTRIBUTES).equivalent(itemDelta.getParentPath())) {
				QName attrName = itemDelta.getElementName();
				if (objectClassDefinition.isSecondaryIdentifier(attrName)) {
					// Change of secondary identifier means object rename. We also need to change $shadow/name
					// TODO: change this to displayName attribute later
					String newName = null;
					if (itemDelta.getValuesToReplace() != null && !itemDelta.getValuesToReplace().isEmpty()) {
						newName = ((PrismPropertyValue)itemDelta.getValuesToReplace().iterator().next()).getValue().toString();
					} else if (itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty()) {
						newName = ((PrismPropertyValue)itemDelta.getValuesToAdd().iterator().next()).getValue().toString();
					}
					PropertyDelta<PolyString> nameDelta = PropertyDelta.createReplaceDelta(shadow.getDefinition(), ShadowType.F_NAME, new PolyString(newName));
					repoChanges.add(nameDelta);
				}
				if (!ProvisioningUtil.shouldStoreAtributeInShadow(objectClassDefinition, attrName, cachingStrategy)) {
					continue;
				}
			} else if (new ItemPath(ShadowType.F_ACTIVATION).equivalent(itemDelta.getParentPath())) {
				if (!ProvisioningUtil.shouldStoreActivationItemInShadow(itemDelta.getElementName(), cachingStrategy)) {
					continue;
				}
			} else if (new ItemPath(ShadowType.F_ACTIVATION).equivalent(itemDelta.getPath())) {		// should not occur, but for completeness...
				for (PrismContainerValue<ActivationType> valueToAdd : ((ContainerDelta<ActivationType>) itemDelta).getValuesToAdd()) {
					ProvisioningUtil.cleanupShadowActivation(valueToAdd.asContainerable());
				}
				for (PrismContainerValue<ActivationType> valueToReplace : ((ContainerDelta<ActivationType>) itemDelta).getValuesToReplace()) {
					ProvisioningUtil.cleanupShadowActivation(valueToReplace.asContainerable());
				}
			} else if (SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
				continue;
			}
			normalizeDelta(itemDelta, objectClassDefinition);
			repoChanges.add(itemDelta);
		}
		return repoChanges;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<ItemDelta> updateShadow(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			Collection<? extends ItemDelta> aprioriDeltas, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, resourceShadow.getOid(), null, result);
		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		Collection<ItemDelta> repoShadowChanges = new ArrayList<ItemDelta>();
		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);

		for (RefinedAttributeDefinition attrDef: objectClassDefinition.getAttributeDefinitions()) {
			if (ProvisioningUtil.shouldStoreAtributeInShadow(objectClassDefinition, attrDef.getName(), cachingStrategy)) {
				ResourceAttribute<Object> resourceAttr = ShadowUtil.getAttribute(resourceShadow, attrDef.getName());
				PrismProperty<Object> repoAttr = repoShadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()));
				PropertyDelta attrDelta;
				if (repoAttr == null && repoAttr == null) {
					continue;
				}
				if (repoAttr == null) {
					attrDelta = attrDef.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, attrDef.getName()));
					attrDelta.setValuesToReplace(PrismValue.cloneCollection(resourceAttr.getValues()));
				} else {
					attrDelta = repoAttr.diff(resourceAttr);
//					LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}", repoAttr==null?null:repoAttr.debugDump(1), resourceAttr==null?null:resourceAttr.debugDump(1), attrDelta==null?null:attrDelta.debugDump(1));
				}
				if (attrDelta != null && !attrDelta.isEmpty()) {
					normalizeDelta(attrDelta, attrDef);
					repoShadowChanges.add(attrDelta);
				}
			}
		}

		// TODO: reflect activation updates on cached shadow

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updating repo shadow {}:\n{}", resourceShadow.getOid(), DebugUtil.debugDump(repoShadowChanges));
		}
		try {
			repositoryService.modifyObject(ShadowType.class, resourceShadow.getOid(), repoShadowChanges, result);
		} catch (ObjectAlreadyExistsException e) {
			// We are not renaming the object here. This should not happen.
			throw new SystemException(e.getMessage(), e);
		}
		return repoShadowChanges;
	}
	
	/**
	 * Updates repository shadow based on shadow from resource. Handles rename cases,
	 * change of auxiliary object classes, etc.
	 * @returns repository shadow as it should look like after the update
	 */
	@SuppressWarnings("unchecked")
	public PrismObject<ShadowType> updateShadow(ProvisioningContext ctx, PrismObject<ShadowType> currentResourceShadow,
			PrismObject<ShadowType> oldRepoShadow, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
		
		RefinedObjectClassDefinition ocDef = ctx.computeCompositeObjectClassDefinition(currentResourceShadow);
		ObjectDelta<ShadowType> shadowDelta = oldRepoShadow.createModifyDelta();
		PrismContainer<Containerable> currentResourceAttributesContainer = currentResourceShadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismContainer<Containerable> oldRepoAttributesContainer = oldRepoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		ShadowType oldRepoShadowType = oldRepoShadow.asObjectable();
		ShadowType currentResourceShadowType = currentResourceShadow.asObjectable();

		if (oldRepoShadowType.isExists() != currentResourceShadowType.isExists()) {
			// Resource object obviously exists when we have got here
			shadowDelta.addModificationReplaceProperty(ShadowType.F_EXISTS, currentResourceShadowType.isExists());
		}
		
		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);

		for (Item<?, ?> currentResourceItem: currentResourceAttributesContainer.getValue().getItems()) {
			if (currentResourceItem instanceof PrismProperty<?>) {
				PrismProperty<?> currentResourceAttrProperty = (PrismProperty<?>)currentResourceItem;
				RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(currentResourceAttrProperty.getElementName());
				if (ProvisioningUtil.shouldStoreAtributeInShadow(ocDef, attrDef.getName(), cachingStrategy)) {
					MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
					PrismProperty<Object> oldRepoAttributeProperty = oldRepoAttributesContainer.findProperty(currentResourceAttrProperty.getElementName());
					if (oldRepoAttributeProperty == null ) {
						PropertyDelta<?> attrAddDelta = currentResourceAttrProperty.createDelta();
						for (PrismPropertyValue pval: currentResourceAttrProperty.getValues()) {
							Object normalizedRealValue;
							if (matchingRule == null) {
								normalizedRealValue = pval.getValue();
							} else {
								normalizedRealValue = matchingRule.normalize(pval.getValue());
							}
							attrAddDelta.addValueToAdd(new PrismPropertyValue(normalizedRealValue));
							LOGGER.trace("CURRENT ATTR:\n{}\nATTR DELTA:\n{}", currentResourceAttrProperty.debugDump(1), attrAddDelta.debugDump(1));
						}
						shadowDelta.addModification(attrAddDelta);
					} else {
						if (attrDef.isSingleValue()) {
							Object currentResourceRealValue = currentResourceAttrProperty.getRealValue();
							Object currentResourceNormalizedRealValue;
							if (matchingRule == null) {
								currentResourceNormalizedRealValue = currentResourceRealValue;
							} else {
								currentResourceNormalizedRealValue = matchingRule.normalize(currentResourceRealValue);
							}
							if (!currentResourceNormalizedRealValue.equals(oldRepoAttributeProperty.getRealValue())) {
								LOGGER.trace("CURRENT ATTR:\n{}\ncurrentResourceNormalizedRealValue: {}", currentResourceAttrProperty.debugDump(1), currentResourceNormalizedRealValue);
								shadowDelta.addModificationReplaceProperty(currentResourceAttrProperty.getPath(), currentResourceNormalizedRealValue);
							}
						} else {
							PrismProperty<Object> normalizedCurrentResourceAttrProperty = (PrismProperty<Object>) currentResourceAttrProperty.clone();
							if (matchingRule != null) {
								for (PrismPropertyValue pval: normalizedCurrentResourceAttrProperty.getValues()) {
									Object normalizedRealValue = matchingRule.normalize(pval.getValue());
									pval.setValue(normalizedRealValue);
								}
							}
							PropertyDelta<Object> attrDiff = oldRepoAttributeProperty.diff(normalizedCurrentResourceAttrProperty);
							LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}",
									oldRepoAttributeProperty==null?null:oldRepoAttributeProperty.debugDump(1),
									normalizedCurrentResourceAttrProperty==null?null:normalizedCurrentResourceAttrProperty.debugDump(1),
									attrDiff==null?null:attrDiff.debugDump(1));
							if (attrDiff != null && !attrDiff.isEmpty()) {
								attrDiff.setParentPath(new ItemPath(ShadowType.F_ATTRIBUTES));
								shadowDelta.addModification(attrDiff);
							}
							
						}
					}
				}
			}
		}
		
		for (Item<?, ?> oldRepoItem: oldRepoAttributesContainer.getValue().getItems()) {
			if (oldRepoItem instanceof PrismProperty<?>) {
				PrismProperty<?> oldRepoAttrProperty = (PrismProperty<?>)oldRepoItem;
				RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(oldRepoAttrProperty.getElementName());
				PrismProperty<Object> currentAttribute = currentResourceAttributesContainer.findProperty(oldRepoAttrProperty.getElementName());
				if (attrDef == null || !ProvisioningUtil.shouldStoreAtributeInShadow(ocDef, attrDef.getName(), cachingStrategy) ||
						currentAttribute == null) {
					// No definition for this property it should not be there or no current value: remove it from the shadow
					PropertyDelta<?> oldRepoAttrPropDelta = oldRepoAttrProperty.createDelta();
					oldRepoAttrPropDelta.addValuesToDelete((Collection)PrismPropertyValue.cloneCollection(oldRepoAttrProperty.getValues()));
					shadowDelta.addModification(oldRepoAttrPropDelta);
				}
			}
		}
				
		PolyString currentShadowName = ShadowUtil.determineShadowName(currentResourceShadow);
		PolyString oldRepoShadowName = oldRepoShadow.getName();
		if (!currentShadowName.equalsOriginalValue(oldRepoShadowName)) {			
			PropertyDelta<?> shadowNameDelta = PropertyDelta.createModificationReplaceProperty(ShadowType.F_NAME, 
					oldRepoShadow.getDefinition(),currentShadowName);
			shadowDelta.addModification(shadowNameDelta);
		}
		
		PropertyDelta<QName> auxOcDelta = (PropertyDelta)PrismProperty.diff(
				oldRepoShadow.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS),
				currentResourceShadow.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS));
		if (auxOcDelta != null) {
			shadowDelta.addModification(auxOcDelta);
		}
		
		if (cachingStrategy == CachingStategyType.NONE) {
			if (oldRepoShadowType.getCachingMetadata() != null) {
				shadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA);
			}

		} else if (cachingStrategy == CachingStategyType.PASSIVE) {

			compareUpdateProperty(shadowDelta, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, currentResourceShadow, oldRepoShadow);
			compareUpdateProperty(shadowDelta, SchemaConstants.PATH_ACTIVATION_VALID_FROM, currentResourceShadow, oldRepoShadow);
			compareUpdateProperty(shadowDelta, SchemaConstants.PATH_ACTIVATION_VALID_TO, currentResourceShadow, oldRepoShadow);
			compareUpdateProperty(shadowDelta, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, currentResourceShadow, oldRepoShadow);

			CachingMetadataType cachingMetadata = new CachingMetadataType();
			cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
			shadowDelta.addModificationReplaceProperty(ShadowType.F_CACHING_METADATA, cachingMetadata);

		} else {
			throw new ConfigurationException("Unknown caching strategy "+cachingStrategy);
		}

		if (!shadowDelta.isEmpty()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Updating repo shadow {} with delta:\n{}", oldRepoShadow, shadowDelta.debugDump(1));
			}
			ConstraintsChecker.onShadowModifyOperation(shadowDelta.getModifications());
			try {
				repositoryService.modifyObject(ShadowType.class, oldRepoShadow.getOid(), shadowDelta.getModifications(), parentResult);
			} catch (ObjectAlreadyExistsException e) {
				// This should not happen for shadows
				throw new SystemException(e.getMessage(), e);
			}

			PrismObject<ShadowType> newRepoShadow = oldRepoShadow.clone();
			shadowDelta.applyTo(newRepoShadow);
			return newRepoShadow;
			
		} else {
			LOGGER.trace("No need to update repo shadow {} (empty delta)", oldRepoShadow);
			return oldRepoShadow;
		}		
	}

	private <T> void compareUpdateProperty(ObjectDelta<ShadowType> shadowDelta,
			ItemPath itemPath, PrismObject<ShadowType> currentResourceShadow, PrismObject<ShadowType> oldRepoShadow) {
		PrismProperty<T> currentProperty = currentResourceShadow.findProperty(itemPath);
		PrismProperty<T> oldProperty = oldRepoShadow.findProperty(itemPath);
		PropertyDelta<T> itemDelta = PrismProperty.diff(oldProperty, currentProperty);
		if (itemDelta != null && !itemDelta.isEmpty()) {
			shadowDelta.addModification(itemDelta);
		}
	}
	
	public void deleteShadow(ProvisioningContext ctx, PrismObject<ShadowType> oldRepoShadow, OperationResult resourceOperationResult, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		LOGGER.trace("Deleting repository {}, resourceOperationResult={}", oldRepoShadow, 
				resourceOperationResult==null?null:resourceOperationResult.getStatus());
		if (resourceOperationResult != null && resourceOperationResult.isInProgress()) {
			addPendingOperationDelete(ctx, oldRepoShadow, resourceOperationResult, parentResult);
		} else {
			repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
		}
	}

	/**
	 * Re-reads the shadow, re-evaluates the identifiers and stored values, updates them if necessary. Returns
	 * fixed shadow.  
	 */
	public PrismObject<ShadowType> fixShadow(ProvisioningContext ctx, PrismObject<ShadowType> origRepoShadow,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
		PrismObject<ShadowType> currentRepoShadow = repositoryService.getObject(ShadowType.class, origRepoShadow.getOid(), null, parentResult);
		ProvisioningContext shadowCtx = ctx.spawn(currentRepoShadow);
		RefinedObjectClassDefinition ocDef = shadowCtx.getObjectClassDefinition();
		PrismContainer<Containerable> attributesContainer = currentRepoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer != null) {
			ObjectDelta<ShadowType> shadowDelta = currentRepoShadow.createModifyDelta();
			for (Item<?, ?> item: attributesContainer.getValue().getItems()) {
				if (item instanceof PrismProperty<?>) {
					PrismProperty<?> attrProperty = (PrismProperty<?>)item;
					RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(attrProperty.getElementName());
					if (attrDef == null) {
						// No definition for this property, it should not be in the shadow
						PropertyDelta<?> oldRepoAttrPropDelta = attrProperty.createDelta();
						oldRepoAttrPropDelta.addValuesToDelete((Collection)PrismPropertyValue.cloneCollection(attrProperty.getValues()));
						shadowDelta.addModification(oldRepoAttrPropDelta);
					} else {
						MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
						List<PrismPropertyValue> valuesToAdd = null;
						List<PrismPropertyValue> valuesToDelete = null;
						for (PrismPropertyValue attrVal: attrProperty.getValues()) {
							Object currentRealValue = attrVal.getValue();
							Object normalizedRealValue = matchingRule.normalize(currentRealValue);
							if (!normalizedRealValue.equals(currentRealValue)) {
								if (attrDef.isSingleValue()) {
									shadowDelta.addModificationReplaceProperty(attrProperty.getPath(), normalizedRealValue);
									break;
								} else {
									if (valuesToAdd == null) {
										valuesToAdd = new ArrayList<>();
									}
									valuesToAdd.add(new PrismPropertyValue(normalizedRealValue));
									if (valuesToDelete == null) {
										valuesToDelete = new ArrayList<>();
									}
									valuesToDelete.add(new PrismPropertyValue(currentRealValue));
								}
							}
						}
						PropertyDelta attrDelta = attrProperty.createDelta(attrProperty.getPath());
						if (valuesToAdd != null) {
							attrDelta.addValuesToAdd(valuesToAdd);
						}
						if (valuesToDelete != null) {
							attrDelta.addValuesToDelete(valuesToDelete);
						}
						shadowDelta.addModification(attrDelta);
					}
				}
			}
			if (!shadowDelta.isEmpty()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Fixing shadow {} with delta:\n{}", origRepoShadow, shadowDelta.debugDump());
				}
				try {
					repositoryService.modifyObject(ShadowType.class, origRepoShadow.getOid(), shadowDelta.getModifications(), parentResult);
				} catch (ObjectAlreadyExistsException e) {
					// This should not happen for shadows
					throw new SystemException(e.getMessage(), e);
				}
				shadowDelta.applyTo(currentRepoShadow);
			} else {
				LOGGER.trace("No need to fixing shadow {} (empty delta)", origRepoShadow);
			}
		} else {
			LOGGER.trace("No need to fixing shadow {} (no atttributes)", origRepoShadow);
		}
		return currentRepoShadow;
	}

	public void setKindIfNecessary(ShadowType repoShadowType, RefinedObjectClassDefinition objectClassDefinition) {
        if (repoShadowType.getKind() == null && objectClassDefinition != null) {
            repoShadowType.setKind(objectClassDefinition.getKind());
        }
    }
    
    public void setIntentIfNecessary(ShadowType repoShadowType, RefinedObjectClassDefinition objectClassDefinition) {
        if (repoShadowType.getIntent() == null && objectClassDefinition.getIntent() != null) {
            repoShadowType.setIntent(objectClassDefinition.getIntent());
        }
    }

    public void normalizeAttributes(PrismObject<ShadowType> shadow, RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		for (ResourceAttribute<?> attribute: ShadowUtil.getAttributes(shadow)) {
			RefinedAttributeDefinition rAttrDef = objectClassDefinition.findAttributeDefinition(attribute.getElementName());
			normalizeAttribute(attribute, rAttrDef);			
		}
	}

	private <T> void normalizeAttribute(ResourceAttribute<T> attribute, RefinedAttributeDefinition rAttrDef) throws SchemaException {
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
		if (matchingRule != null) {
			for (PrismPropertyValue<T> pval: attribute.getValues()) {
				T normalizedRealValue = matchingRule.normalize(pval.getValue());
				pval.setValue(normalizedRealValue);
			}
		}
	}
	
	public <T> void normalizeDeltas(Collection<? extends ItemDelta<PrismPropertyValue<T>,PrismPropertyDefinition<T>>> deltas,
			RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		for (ItemDelta<PrismPropertyValue<T>,PrismPropertyDefinition<T>> delta : deltas) {
			normalizeDelta(delta, objectClassDefinition);
		}
	}
	
	public <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>,PrismPropertyDefinition<T>> delta,
			RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		if (!ShadowType.F_ATTRIBUTES.equals(ItemPath.getName(delta.getPath().first()))){
			return;
		}
		RefinedAttributeDefinition rAttrDef = objectClassDefinition.findAttributeDefinition(delta.getElementName());
		if (rAttrDef == null){
			throw new SchemaException("Failed to normalize attribute: " + delta.getElementName()+ ". Definition for this attribute doesn't exist.");
		}
		normalizeDelta(delta, rAttrDef);		
	}
	
	private <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>,PrismPropertyDefinition<T>> delta, RefinedAttributeDefinition rAttrDef) throws SchemaException{
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
		if (matchingRule != null) {
			if (delta.getValuesToReplace() != null){
				normalizeValues(delta.getValuesToReplace(), matchingRule);
			}
			if (delta.getValuesToAdd() != null){
				normalizeValues(delta.getValuesToAdd(), matchingRule);
			}
			
			if (delta.getValuesToDelete() != null){
				normalizeValues(delta.getValuesToDelete(), matchingRule);
			}
		}
	}
	
	private <T> void normalizeValues(Collection<PrismPropertyValue<T>> values, MatchingRule<T> matchingRule) throws SchemaException {
		for (PrismPropertyValue<T> pval: values) {
			T normalizedRealValue = matchingRule.normalize(pval.getValue());
			pval.setValue(normalizedRealValue);
		}
	}
	
	<T> T getNormalizedAttributeValue(PrismPropertyValue<T> pval, RefinedAttributeDefinition rAttrDef) throws SchemaException {
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
		if (matchingRule != null) {
			T normalizedRealValue = matchingRule.normalize(pval.getValue());
			return normalizedRealValue;
		} else {
			return pval.getValue();
		}
	}
	
	private <T> Collection<T> getNormalizedAttributeValues(ResourceAttribute<T> attribute, RefinedAttributeDefinition rAttrDef) throws SchemaException {
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(rAttrDef.getMatchingRuleQName(), rAttrDef.getTypeName());
		if (matchingRule == null) {
			return attribute.getRealValues();
		} else {
			Collection<T> normalizedValues = new ArrayList<T>();
			for (PrismPropertyValue<T> pval: attribute.getValues()) {
				T normalizedRealValue = matchingRule.normalize(pval.getValue());
				normalizedValues.add(normalizedRealValue);
			}
			return normalizedValues;
		}
	}

	public <T> boolean compareAttribute(RefinedObjectClassDefinition refinedObjectClassDefinition,
			ResourceAttribute<T> attributeA, T... valuesB) throws SchemaException {
		RefinedAttributeDefinition refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
		Collection<T> valuesA = getNormalizedAttributeValues(attributeA, refinedAttributeDefinition);
		return MiscUtil.unorderedCollectionEquals(valuesA, Arrays.asList(valuesB));
	}
	
	public <T> boolean compareAttribute(RefinedObjectClassDefinition refinedObjectClassDefinition,
			ResourceAttribute<T> attributeA, ResourceAttribute<T> attributeB) throws SchemaException {
		RefinedAttributeDefinition refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
		Collection<T> valuesA = getNormalizedAttributeValues(attributeA, refinedAttributeDefinition);
		
		refinedAttributeDefinition = refinedObjectClassDefinition.findAttributeDefinition(attributeA.getElementName());
		Collection<T> valuesB = getNormalizedAttributeValues(attributeB, refinedAttributeDefinition);
		return MiscUtil.unorderedCollectionEquals(valuesA, valuesB);
	}
}
