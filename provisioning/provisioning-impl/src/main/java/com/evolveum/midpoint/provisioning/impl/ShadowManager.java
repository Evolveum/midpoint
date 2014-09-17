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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.aspectj.weaver.patterns.NamePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
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
//		else if (FailedOperationTypeType.MODIFY == conflictedShadow.getFailedOperationType()) {
//			ObjectDeltaType objDeltaType = conflictedShadow.getObjectChange();
//			if (objDeltaType != null) {
//				Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
//						objDeltaType.getModification(), account.getDefinition());
//				objectDelta = ObjectDelta.createModifyDelta(conflictedShadow.getOid(), modifications,
//						AccountShadowType.class, prismContext);
//
//			}
//		}
//		else if (FailedOperationTypeType.DELETE == conflictedShadow.getFailedOperationType()){
//			objectDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, conflictedShadow.getOid(), prismContext);
//		}
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
	 * @param parentResult
	 * 
	 * @return current unchanged shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 * @throws SchemaException
	 * @throws ConfigurationException 
	 */
	public PrismObject<ShadowType> lookupShadowInRepository(PrismObject<ShadowType> resourceShadow,
			RefinedObjectClassDefinition rObjClassDef, ResourceType resource, OperationResult parentResult) 
					throws SchemaException, ConfigurationException {

		ObjectQuery query = createSearchShadowQuery(resourceShadow, rObjClassDef, resource, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter:\n{}",
					query.debugDump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<ShadowType>> results;

		results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<ShadowType> result : results) {
				LOGGER.trace("Search result:\n{}", result.debugDump());
			}
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		return results.get(0);
	}
	
	public PrismObject<ShadowType> lookupShadowByName( 
			PrismObject<ShadowType> resourceShadow, RefinedObjectClassDefinition rObjClassDef, 
			ResourceType resource, OperationResult parentResult) 
					throws SchemaException, ConfigurationException {

		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(resourceShadow);
		ResourceAttribute<?> secondaryIdentifier = null;
		if (secondaryIdentifiers.size() < 1){
			LOGGER.trace("Shadow does not contain secondary idetifier. Skipping lookup shadows according to name.");
			return null;
		}
		
		secondaryIdentifier = secondaryIdentifiers.iterator().next();
		LOGGER.trace("Shadow secondary identifier {}", secondaryIdentifier);
		
		AndFilter filter = AndFilter.createAnd(
				RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource), 
				EqualFilter.createEqual(secondaryIdentifier.getPath(), secondaryIdentifier.getDefinition(),
				getNormalizedValue(secondaryIdentifier, rObjClassDef)));
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter on secondary identifier:\n{}",
					query.debugDump());
		}

		// TODO: check for errors
		List<PrismObject<ShadowType>> results;

		results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
//		if (results.size() > 1) {
//			for (PrismObject<ShadowType> result : results) {
//				LOGGER.trace("Search result:\n{}", result.debugDump());
//			}
//			LOGGER.error("More than one shadows found for " + resourceShadow);
//			// TODO: Better error handling later
//			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
//		}
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
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		return conflictingShadows.get(0);
	}

	private <T> PrismPropertyValue<T> getNormalizedValue(PrismProperty<T> attr, RefinedObjectClassDefinition rObjClassDef) throws SchemaException {
		RefinedAttributeDefinition refinedAttributeDefinition = rObjClassDef.findAttributeDefinition(attr.getElementName());
		QName matchingRuleQName = refinedAttributeDefinition.getMatchingRuleQName();
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, refinedAttributeDefinition.getTypeName());
		PrismPropertyValue<T> origPValue = attr.getValue();
		if (matchingRule != null) {
			T normalizedValue = matchingRule.normalize(origPValue.getValue());
			PrismPropertyValue<T> normalizedPValue = origPValue.clone();
			normalizedPValue.setValue(normalizedValue);
			return normalizedPValue;
		} else {
			return origPValue;
		}
	}


    // beware, may return null if an shadow that was to be marked as DEAD, was deleted in the meantime
	public PrismObject<ShadowType> findOrCreateShadowFromChange(ResourceType resource, Change<ShadowType> change,
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<ShadowType>> accountList = searchAccountByIdenifiers(change, objectClassDefinition, resource, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one account with the identifier " + change.getIdentifiers() + ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		PrismObject<ShadowType> newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null || change.getObjectDelta().getChangeType() != ChangeType.DELETE) {
				newShadow = createNewAccountFromChange(change, resource, objectClassDefinition, parentResult);

				try {
					String oid = repositoryService.addObject(newShadow, null, parentResult);
					newShadow.setOid(oid);
					if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
						change.getObjectDelta().setOid(oid);
					}
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add account " + SchemaDebugUtil.prettyPrint(newShadow)
							+ " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.trace("Created account shadow object: {}", newShadow);
			}

		} else {
			// Account was found in repository
			newShadow = accountList.get(0);
			
            if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
					Collection<? extends ItemDelta> deadDeltas = PropertyDelta
							.createModificationReplacePropertyCollection(ShadowType.F_DEAD,
									newShadow.getDefinition(), true);
					try {
						repositoryService.modifyObject(ShadowType.class, newShadow.getOid(), deadDeltas,
								parentResult);
					} catch (ObjectAlreadyExistsException e) {
						parentResult.recordFatalError(
								"Can't add account " + SchemaDebugUtil.prettyPrint(newShadow)
										+ " to the repository. Reason: " + e.getMessage(), e);
						throw new IllegalStateException(e.getMessage(), e);
					} catch (ObjectNotFoundException e) {
						parentResult.recordWarning("Account shadow " + SchemaDebugUtil.prettyPrint(newShadow)
								+ " was probably deleted from the repository in the meantime. Exception: "
								+ e.getMessage(), e);
						return null;
					}
				} 
				
			
			
		}
		

		return newShadow;
	}
	
	private PrismObject<ShadowType> createNewAccountFromChange(Change<ShadowType> change, ResourceType resource, 
			RefinedObjectClassDefinition objectClassDefinition,
			OperationResult parentResult) throws SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException {

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
			shadow = createRepositoryShadow(shadow, resource, objectClassDefinition);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
			throw new SchemaException("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
		}

		parentResult.recordSuccess();
		return shadow;
	}
	
	private List<PrismObject<ShadowType>> searchAccountByIdenifiers(Change<ShadowType> change, 
			RefinedObjectClassDefinition rOcDef, ResourceType resource, OperationResult parentResult)
			throws SchemaException {

		ObjectQuery query = createSearchShadowQuery(change.getIdentifiers(), rOcDef, resource, prismContext, parentResult);

		List<PrismObject<ShadowType>> accountList = null;
		try {
			accountList = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search account according to the identifiers: " + change.getIdentifiers() + ". Reason: "
							+ ex.getMessage(), ex);
			throw new SchemaException("Failed to search account according to the identifiers: "
					+ change.getIdentifiers() + ". Reason: " + ex.getMessage(), ex);
		}
		return accountList;
	}
	
	private ObjectQuery createSearchShadowQuery(Collection<ResourceAttribute<?>> identifiers,
			RefinedObjectClassDefinition rOcDef, ResourceType resource, PrismContext prismContext, 
			OperationResult parentResult) throws SchemaException {
		List<ObjectFilter> conditions = new ArrayList<ObjectFilter>();
		for (PrismProperty<?> identifier : identifiers) {
			PrismPropertyValue<?> identifierValue = identifier.getValue();
			RefinedAttributeDefinition rAttrDef = rOcDef.findAttributeDefinition(identifier.getElementName());
			Object normalizedIdentifierValue = getNormalizedAttributeValue(identifierValue, rAttrDef);
			//new ItemPath(ShadowType.F_ATTRIBUTES)
			PrismPropertyDefinition def = identifier.getDefinition();
			EqualFilter filter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, def.getName()), def, new PrismPropertyValue(normalizedIdentifierValue));
			conditions.add(filter);
		}

		if (conditions.size() < 1) {
			throw new SchemaException("Identifier not specifier. Cannot create search query by identifier.");
		}
		
		RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource);
		conditions.add(resourceRefFilter);

		ObjectFilter filter = null;
		if (conditions.size() > 1) {
			filter = AndFilter.createAnd(conditions);
		} else {
			filter = conditions.get(0);
		}

		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return query;
	}

	private ObjectQuery createSearchShadowQuery(PrismObject<ShadowType> resourceShadow, 
			RefinedObjectClassDefinition rObjClassDef, ResourceType resource,
			PrismContext prismContext, OperationResult parentResult) throws SchemaException {
		// XPathHolder xpath = createXpathHolder();
		ResourceAttributeContainer attributesContainer = ShadowUtil
				.getAttributesContainer(resourceShadow);
		PrismProperty identifier = attributesContainer.getIdentifier();

		Collection<PrismPropertyValue<Object>> idValues = identifier.getValues();
		// Only one value is supported for an identifier
		if (idValues.size() > 1) {
			// LOGGER.error("More than one identifier value is not supported");
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("More than one identifier value is not supported");
		}
		if (idValues.size() < 1) {
			// LOGGER.error("The identifier has no value");
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("The identifier has no value");
		}

		// We have all the data, we can construct the filter now
		// Document doc = DOMUtil.getDocument();
		// Element filter;
		// List<Element> identifierElements =
		// prismContext.getPrismDomProcessor().serializeItemToDom(identifier,
		// doc);
		ObjectFilter filter = null;
		try {
			// TODO TODO TODO TODO: set matching rule instead of null
			PrismPropertyDefinition def = identifier.getDefinition();
			filter = AndFilter.createAnd(
					RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource), 
					EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, def.getName()), def, getNormalizedValue(identifier, rObjClassDef)));
		} catch (SchemaException e) {
			// LOGGER.error("Schema error while creating search filter: {}",
			// e.getMessage(), e);
			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
		}

		ObjectQuery query = ObjectQuery.createObjectQuery(filter);

		// LOGGER.trace("created query " + DOMUtil.printDom(filter));

		return query;
	}
	
	public void searchObjectsIterativeRepository(
			RefinedObjectClassDefinition objectClassDef,
			final ResourceType resourceType, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			com.evolveum.midpoint.schema.ResultHandler<ShadowType> repoHandler, OperationResult parentResult) throws SchemaException {
		
		ObjectQuery repoQuery = query.clone();
		processQueryMatchingRules(repoQuery, objectClassDef);
		
		repositoryService.searchObjectsIterative(ShadowType.class, repoQuery, repoHandler, options, parentResult);
		
	}

	/**
	 * Visit the query and normalize values (or set matching rules) as needed
	 */
	private void processQueryMatchingRules(ObjectQuery repoQuery, final RefinedObjectClassDefinition objectClassDef) {
		ObjectFilter filter = repoQuery.getFilter();
		Visitor visitor = new Visitor() {
			@Override
			public void visit(ObjectFilter filter) {
				try {
					processQueryMatchingRuleFilter(filter, objectClassDef);
				} catch (SchemaException e) {
					throw new SystemException(e);
				}
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
		if (parentPath == null || !parentPath.equals(SchemaConstants.PATH_ATTRIBUTES)) {
			return;
		}
		QName attrName = eqFilter.getElementName();
		RefinedAttributeDefinition rAttrDef = objectClassDef.findAttributeDefinition(attrName);
		QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
		if (matchingRuleQName == null) {
			return;
		}
		MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());
		if (matchingRule == null) {
			// TODO: warning?
			return;
		}
		List<PrismValue> newValues = new ArrayList<PrismValue>();
		for (PrismPropertyValue<T> ppval: eqFilter.getValues()) {
			T normalizedRealValue = matchingRule.normalize(ppval.getValue());
			PrismPropertyValue<T> newPPval = ppval.clone();
			newPPval.setValue(normalizedRealValue);
			newValues.add(newPPval);
		}
		eqFilter.getValues().clear();
		eqFilter.getValues().addAll((Collection) newValues);
		LOGGER.trace("Replacing values for attribute {} in search filter with normalized values because there is a matching rule, normalized values: {}",
				attrName, newValues);
	}

	/**
	 * Create a copy of a shadow that is suitable for repository storage.
	 */
	public PrismObject<ShadowType> createRepositoryShadow(PrismObject<ShadowType> shadow, ResourceType resource,
			RefinedObjectClassDefinition objectClassDefinition)
			throws SchemaException {

		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(shadow);

		normalizeAttributes(shadow, objectClassDefinition);
		
		PrismObject<ShadowType> repoShadow = shadow.clone();
		ResourceAttributeContainer repoAttributesContainer = ShadowUtil
				.getAttributesContainer(repoShadow);

		// Clean all repoShadow attributes and add only those that should be
		// there
		repoAttributesContainer.clear();
		Collection<ResourceAttribute<?>> identifiers = attributesContainer.getIdentifiers();
		for (PrismProperty<?> p : identifiers) {
			repoAttributesContainer.add(p.clone());
		}

		Collection<ResourceAttribute<?>> secondaryIdentifiers = attributesContainer.getSecondaryIdentifiers();
		for (PrismProperty<?> p : secondaryIdentifiers) {
			repoAttributesContainer.add(p.clone());
		}

		ShadowType repoShadowType = repoShadow.asObjectable();

        setKindIfNecessary(repoShadowType, objectClassDefinition);
//        setIntentIfNecessary(repoShadowType, objectClassDefinition);

        // We don't want to store credentials in the repo
		repoShadowType.setCredentials(null);

		// additional check if the shadow doesn't contain resource, if yes,
		// convert to the resource reference.
		if (repoShadowType.getResource() != null) {
			repoShadowType.setResource(null);
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}

		// if shadow does not contain resource or resource reference, create it
		// now
		if (repoShadowType.getResourceRef() == null) {
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}

		if (repoShadowType.getName() == null) {
			repoShadowType.setName(new PolyStringType(ProvisioningUtil.determineShadowName(shadow)));
		}

		if (repoShadowType.getObjectClass() == null) {
			repoShadowType.setObjectClass(attributesContainer.getDefinition().getTypeName());
		}
		
		if (repoShadowType.isProtectedObject() != null){
			repoShadowType.setProtectedObject(null);
		}
	
		return repoShadow;
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
	
	public <T> void normalizeDeltas(Collection<? extends ItemDelta<PrismPropertyValue<T>>> deltas,
			RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		// TODO Auto-generated method stub
		for (ItemDelta<PrismPropertyValue<T>> delta : deltas){
			if (!ShadowType.F_ATTRIBUTES.equals(ItemPath.getName(delta.getPath().first()))){
				continue;
			}
			RefinedAttributeDefinition rAttrDef = objectClassDefinition.findAttributeDefinition(delta.getElementName());
			if (rAttrDef == null){
				throw new SchemaException("Failed to normalize attribute: " + delta.getElementName()+ ". Definition for this attribute doesn't exist.");
			}
			normalizeDelta(delta, rAttrDef);
		}
		
		
	}
	
	private <T> void normalizeDelta(ItemDelta<PrismPropertyValue<T>> delta, RefinedAttributeDefinition rAttrDef) throws SchemaException{
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
	
	private <T> void normalizeValues(Collection<PrismPropertyValue<T>> values, MatchingRule<T> matchingRule){
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
