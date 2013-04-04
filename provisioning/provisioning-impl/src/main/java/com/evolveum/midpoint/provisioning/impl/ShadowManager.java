/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

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
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowManager.class);
		
	
	public <T extends ShadowType> void deleteConflictedShadowFromRepo(PrismObject<T> shadow, OperationResult parentResult){
		
		try{
			
			repositoryService.deleteObject(shadow.getCompileTimeClass(), shadow.getOid(), parentResult);
		
		} catch (Exception ex){
			throw new SystemException(ex.getMessage(), ex);
		}
		
	}
	
	public <T extends ShadowType> ResourceOperationDescription createResourceFailureDescription(
			PrismObject<T> conflictedShadow, ResourceType resource, OperationResult parentResult){
		ResourceOperationDescription failureDesc = new ResourceOperationDescription();
		failureDesc.setCurrentShadow(conflictedShadow);
		ObjectDelta<T> objectDelta = null;
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
	public <T extends ShadowType> PrismObject<T> lookupShadowInRepository(Class<T> type, PrismObject<T> resourceShadow,
			ResourceType resource, OperationResult parentResult) throws SchemaException, ConfigurationException {

		ObjectQuery query = createSearchShadowQuery(resourceShadow, resource, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter:\n{}",
					query.dump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<T>> results;

		results = repositoryService.searchObjects(type, query, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<T> result : results) {
				LOGGER.trace("Search result:\n{}", result.dump());
			}
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		return results.get(0);
	}

	public <T extends ShadowType> PrismObject<T> lookupShadowByName(Class<T> type, 
			PrismObject<T> resourceShadow, ResourceType resource, OperationResult parentResult) 
					throws SchemaException, ConfigurationException {

		Collection<ResourceAttribute<?>> secondaryIdentifiers = ResourceObjectShadowUtil.getSecondaryIdentifiers(resourceShadow);
		ResourceAttribute<?> secondaryIdentifier = null;
		if (secondaryIdentifiers.size() < 1){
			LOGGER.trace("Shadow does not contain secondary idetifier. Skipping lookup shadows according to name.");
			return null;
		}
		
		secondaryIdentifier = secondaryIdentifiers.iterator().next();
		LOGGER.trace("Shadow secondary identifier {}", secondaryIdentifier);
		
		AndFilter filter = AndFilter.createAnd(RefFilter.createReferenceEqual(ShadowType.class,
				ShadowType.F_RESOURCE_REF, prismContext, resource.getOid()), EqualsFilter.createEqual(
				new ItemPath(ShadowType.F_ATTRIBUTES), secondaryIdentifier.getDefinition(),
				secondaryIdentifier.getValue()));
//		ObjectQuery query = ShadowCacheUtil.createSearchShadowQuery(resourceShadow, resource, prismContext,
//				parentResult);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter on secondary identifier:\n{}",
					query.dump());
		}
//		PagingType paging = new PagingType();

		// TODO: check for errors
		List<PrismObject<T>> results;

		results = repositoryService.searchObjects(type, query, parentResult);

		LOGGER.trace("lookupShadow found {} objects", results.size());

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			for (PrismObject<T> result : results) {
				LOGGER.trace("Search result:\n{}", result.dump());
			}
			LOGGER.error("More than one shadows found for " + resourceShadow);
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceShadow);
		}

		PrismObject<T> repoShadow = results.get(0);
		T repoShadowType = repoShadow.asObjectable();
		if (repoShadow != null) {
			if (repoShadowType.getFailedOperationType() == null){
				LOGGER.trace("Found shadow is ok, returning null");
				return null;
			} 
			if (repoShadowType.getFailedOperationType() != null && FailedOperationTypeType.ADD != repoShadowType.getFailedOperationType()){
				return null;
			}
		}
		return repoShadow;
	}

	public <T extends ShadowType> PrismObject<T> findOrCreateShadowFromChange(
			Class<T> type, ResourceType resource, Change<T> change,
			ObjectClassComplexTypeDefinition objectClassDefinition, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, ConfigurationException, SecurityViolationException {

		// Try to locate existing shadow in the repository
		List<PrismObject<T>> accountList = searchAccountByIdenifiers(type, change, resource, parentResult);

		if (accountList.size() > 1) {
			String message = "Found more than one account with the identifier " + change.getIdentifiers() + ".";
			LOGGER.error(message);
			parentResult.recordFatalError(message);
			throw new IllegalArgumentException(message);
		}

		PrismObject<T> newShadow = null;

		if (accountList.isEmpty()) {
			// account was not found in the repository, create it now

			if (change.getObjectDelta() == null || !(change.getObjectDelta().getChangeType() == ChangeType.DELETE)) {
				try {
					newShadow = createNewAccountFromChange(change, resource, objectClassDefinition, parentResult);
				} catch (ObjectNotFoundException ex) {
					throw ex;
				}

				try {
					String oid = repositoryService.addObject(newShadow, null, parentResult);
					newShadow.setOid(oid);
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
		}
		

		return newShadow;
	}
	
	private <T extends ShadowType> PrismObject<T> createNewAccountFromChange(Change<T> change, ResourceType resource, 
			ObjectClassComplexTypeDefinition objectClassDefinition,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException, ConfigurationException,
			SecurityViolationException {

		PrismObject<T> shadow = change.getCurrentShadow();
		try {
			shadow = createRepositoryShadow(shadow, resource);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
			throw new SchemaException("Can't create account shadow from identifiers: "
					+ change.getIdentifiers());
		}

		parentResult.recordSuccess();
		return shadow;
	}
	
	private <T extends ShadowType> List<PrismObject<T>> searchAccountByIdenifiers(Class<T> type, Change<T> change, ResourceType resource, OperationResult parentResult)
			throws SchemaException {

		ObjectQuery query = createSearchShadowQuery(change.getIdentifiers(), resource, prismContext, parentResult);

		List<PrismObject<T>> accountList = null;
		try {
			accountList = repositoryService.searchObjects(type, query, parentResult);
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
			ResourceType resource, PrismContext prismContext, OperationResult parentResult) throws SchemaException {
		List<ObjectFilter> conditions = new ArrayList<ObjectFilter>();
		for (PrismProperty<?> identifier : identifiers) {
			EqualsFilter filter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES),
					identifier.getDefinition(), identifier.getValue());
			conditions.add(filter);
		}

		if (conditions.size() < 1) {
			throw new SchemaException("Identifier not specifier. Cannot create search query by identifier.");
		}
		
		RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.class, 
				ShadowType.F_RESOURCE_REF, resource.asPrismObject());
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

	private <T extends ShadowType> ObjectQuery createSearchShadowQuery(PrismObject<T> resourceShadow, ResourceType resource,
			PrismContext prismContext, OperationResult parentResult) throws SchemaException {
		// XPathHolder xpath = createXpathHolder();
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil
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
			// filter = QueryUtil.createAndFilter(doc,
			// QueryUtil.createEqualRefFilter(doc, null,
			// SchemaConstants.I_RESOURCE_REF, resource.getOid()), QueryUtil
			// .createEqualFilterFromElements(doc, xpath, identifierElements,
			// resourceShadow
			// .asPrismObject().getPrismContext()));
			filter = AndFilter.createAnd(RefFilter.createReferenceEqual(ShadowType.class,
					ShadowType.F_RESOURCE_REF, prismContext, resource.getOid()), EqualsFilter.createEqual(
					new ItemPath(ShadowType.F_ATTRIBUTES), identifier.getDefinition(),
					identifier.getValues()));
		} catch (SchemaException e) {
			// LOGGER.error("Schema error while creating search filter: {}",
			// e.getMessage(), e);
			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
		}

		ObjectQuery query = ObjectQuery.createObjectQuery(filter);

		// LOGGER.trace("created query " + DOMUtil.printDom(filter));

		return query;
	}

	/**
	 * Create a copy of a shadow that is suitable for repository storage.
	 */
	public <T extends ShadowType> PrismObject<T> createRepositoryShadow(PrismObject<T> shadow, ResourceType resource)
			throws SchemaException {

		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(shadow);

		PrismObject<T> repoShadow = shadow.clone();
		ResourceAttributeContainer repoAttributesContainer = ResourceObjectShadowUtil
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

		// We don't want to store credentials in the repo
		T repoShadowType = repoShadow.asObjectable();
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
			repoShadowType.setName(ShadowCacheUtil.determineShadowName(shadow));
		}

		if (repoShadowType.getObjectClass() == null) {
			repoShadowType.setObjectClass(attributesContainer.getDefinition().getTypeName());
		}

		return repoShadow;
	}

}
