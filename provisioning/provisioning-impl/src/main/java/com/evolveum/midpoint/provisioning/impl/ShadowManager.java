/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.OptimisticLockingRunner;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RecordPendingOperationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceConsistencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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

	@Autowired private Clock clock;
	@Autowired private PrismContext prismContext;
	@Autowired private TaskManager taskManager;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired private Protector protector;
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowManager.class);
	
	public PrismObject<ShadowType> getRepoShadow(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		return repositoryService.getObject(ShadowType.class, oid, null, result);
	}
	
	public void deleteConflictedShadowFromRepo(PrismObject<ShadowType> shadow, OperationResult parentResult){
		
		try{
			
			repositoryService.deleteObject(shadow.getCompileTimeClass(), shadow.getOid(), parentResult);
		
		} catch (Exception ex){
			throw new SystemException(ex.getMessage(), ex);
		}
		
	}
	
	/**
	 * Locates the appropriate Shadow in repository that corresponds to the
	 * provided resource object.
	 *
	 * @return current shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 */
	public PrismObject<ShadowType> lookupLiveShadowInRepository(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		ObjectQuery query = createSearchShadowQueryByPrimaryIdentifier(ctx, resourceShadow, prismContext,
				parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for shadow using filter:\n{}", query.debugDump());
		}

		 List<PrismObject<ShadowType>> foundShadows = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		 MiscSchemaUtil.reduceSearchResult(foundShadows);

		LOGGER.trace("lookupShadow found {} objects", foundShadows.size());
		
		PrismObject<ShadowType> liveShadow = eliminateDeadShadows(foundShadows, parentResult);
		
		if (liveShadow == null) {
			return null;
		}
		ShadowType repoShadowType = liveShadow.asObjectable();
		if (ShadowUtil.isDead(repoShadowType)) {
			// Note: never reset dead shadow flag. Once the shadow's dead, it stays dead.
			throw new SystemException("Dead repo shadow found when expecting live shadow. "
					+ "resourceShadow="+ShadowUtil.shortDumpShadow(resourceShadow)+", repoShadow="+ShadowUtil.shortDumpShadow(liveShadow));
		}
		if (!ShadowUtil.isExists(repoShadowType)) {
			// This is where gestation quantum state collapses.
			// Or maybe the account was created and we have found it before the original thread could mark the shadow as alive.
			// Marking the shadow as existent should not cause much harm. It should only speed up things a little.
			// And it also avoids shadow duplication.
			liveShadow = markShadowExists(liveShadow, parentResult);
		}
		checkConsistency(liveShadow);

		return liveShadow;
	}
	
	public PrismObject<ShadowType> eliminateDeadShadows(List<PrismObject<ShadowType>> shadows, OperationResult result) {
		if (shadows == null || shadows.isEmpty()) {
			return null;
		}
		
		PrismObject<ShadowType> liveShadow = null;
		for (PrismObject<ShadowType> shadow: shadows) {
			ShadowType shadowType = shadow.asObjectable();
			if (!ShadowUtil.isDead(shadowType)) {
				if (liveShadow == null) {
					liveShadow = shadow;
				} else {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("More than one live conflicting shadow found {} and {}:\n{}",
								liveShadow, shadow, DebugUtil.debugDump(shadows, 1));
					}
					// TODO: handle "more than one shadow" case MID-4490
					String msg = "Found more than one live conflicting shadows: "+liveShadow+" and " + shadow;
					result.recordFatalError(msg);
					throw new IllegalStateException(msg);
				}
			}
		}
		return liveShadow;
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
	
	public Collection<PrismObject<ShadowType>> lookForPreviousDeadShadows(ProvisioningContext ctx,
			PrismObject<ShadowType> inputShadow, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		List<PrismObject<ShadowType>> deadShadows = new ArrayList<>();
		ObjectQuery query = createSearchShadowQueryByPrimaryIdentifier(ctx, inputShadow, prismContext, parentResult);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching for dead shadows using filter:\n{}",
					query==null?null:query.debugDump(1));
		}
		if (query == null) {
			// No primary identifier. So there are obviously no relevant previous dead shadows.
			return deadShadows;
		}

		List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
		MiscSchemaUtil.reduceSearchResult(results);

		LOGGER.trace("looking for previous dead shadows, found {} objects", results.size());

		for (PrismObject<ShadowType> foundShadow : results) {
			if (Boolean.TRUE.equals(foundShadow.asObjectable().isDead())) {
				deadShadows.add(foundShadow);
			}
		}
		return deadShadows;
	}

	public PrismObject<ShadowType> lookupConflictingShadowBySecondaryIdentifiers( 
			ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow, OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(resourceShadow);
		List<PrismObject<ShadowType>> results = lookupShadowsBySecondaryIdentifiers(ctx, secondaryIdentifiers, parentResult);
		
		if (results == null || results.size() == 0) {
			return null;
		}

		List<PrismObject<ShadowType>> conflictingShadows = new ArrayList<>();
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
			LOGGER.error("Too many shadows ({}) for secondary identifiers {}: {}", shadows.size(), secondaryIdentifiers, shadows);
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
			ConfigurationException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, EncryptionException {

		// Try to locate existing shadow in the repository
		List<PrismObject<ShadowType>> accountList = searchShadowByIdenifiers(ctx, change, parentResult);

		// We normally do not want dead shadows here. Normally we should not receive any change notifications about dead
		// shadows anyway. And dead shadows may get into the way. E.g. account is deleted and then it is quickly re-created.
		// In that case we will get ADD change notification and there is a dead shadow in repo. But we do not want to use that
		// dead shadow. The notification is about a new (re-create) account. We want to create new shadow.
		PrismObject<ShadowType> foundShadow = eliminateDeadShadows(accountList, parentResult);

		if (foundShadow == null) {
			// account was not found in the repository, create it now

			if ((change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE)) {
				if (accountList.isEmpty()) {
					// Delete. No shadow is OK here.
					return null;
				} else {
					// Delete of shadow that is already dead.
					return accountList.get(0);
				}
				
			} else {

				foundShadow = createNewShadowFromChange(ctx, change, parentResult);

				try {
					ConstraintsChecker.onShadowAddOperation(foundShadow.asObjectable());
					String oid = repositoryService.addObject(foundShadow, null, parentResult);
					foundShadow.setOid(oid);
					if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
						change.getObjectDelta().setOid(oid);
					}
				} catch (ObjectAlreadyExistsException e) {
					parentResult.recordFatalError("Can't add " + foundShadow + " to the repository. Reason: " + e.getMessage(), e);
					throw new IllegalStateException(e.getMessage(), e);
				}
				LOGGER.debug("Added new shadow (from change): {}", foundShadow);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Added new shadow (from change):\n{}", foundShadow.debugDump(1));
				}
			}

		}

		return foundShadow;
	}
	
	// This is really invoked only for delete case. It is mostly copy&paste with findOrAddShadowFromChange.
	// TODO: not very elegant, cleanup
	public PrismObject<ShadowType> findOrAddShadowFromChangeGlobalContext(ProvisioningContext globalCtx, Change change,
			OperationResult parentResult) throws SchemaException, CommunicationException,
			ConfigurationException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, EncryptionException {

		// Try to locate existing shadow in the repository
		List<PrismObject<ShadowType>> accountList = searchShadowByIdenifiers(globalCtx, change, parentResult);
		
		// We normally do not want dead shadows here. Normally we should not receive any change notifications about dead
		// shadows anyway. And dead shadows may get into the way. E.g. account is deleted and then it is quickly re-created.
		// In that case we will get ADD change notification and there is a dead shadow in repo. But we do not want to use that
		// dead shadow. The notification is about a new (re-create) account. We want to create new shadow.
		PrismObject<ShadowType> newShadow = eliminateDeadShadows(accountList, parentResult);
		
		if (newShadow == null) {
			
			if ((change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE)) {
				if (accountList.isEmpty()) {
					// Delete. No shadow is OK here.
					return null;
				} else {
					// Delete of shadow that is already dead.
					return accountList.get(0);
				}
				
			} else {
				// All situations except delete: create new shadow
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
			// Live shadow was found in repository
			
            if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
            		List<ItemDelta<?, ?>> deadDeltas = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
            			.item(ShadowType.F_DEAD).replace(true)
            			.item(ShadowType.F_EXISTS).replace(false)
            			.asItemDeltas();
					try {
						ConstraintsChecker.onShadowModifyOperation(deadDeltas);
						repositoryService.modifyObject(ShadowType.class, newShadow.getOid(), deadDeltas, parentResult);
					} catch (ObjectAlreadyExistsException e) {
						parentResult.recordFatalError(
								"Can't add " + newShadow + " to the repository. Reason: " + e.getMessage(), e);
						throw new IllegalStateException(e.getMessage(), e);
					} catch (ObjectNotFoundException e) {
						parentResult.recordWarning("Shadow " + newShadow + " was probably deleted from the repository in the meantime. Exception: "
								+ e.getMessage(), e);
						return null;
					}
					
					ObjectDelta.applyTo(newShadow, deadDeltas);
				} 
		}

		return newShadow;
	}
	
	private PrismObject<ShadowType> createNewShadowFromChange(ProvisioningContext ctx, Change change,
			OperationResult parentResult) throws SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, EncryptionException {

		PrismObject<ShadowType> shadow = change.getCurrentShadow();
		
		if (shadow == null){
			//try to look in the delta, if there exists some account to be added
			if (change.getObjectDelta() != null) {
				if (change.getObjectDelta().isAdd()) {
					shadow = (PrismObject<ShadowType>) change.getObjectDelta().getObjectToAdd();
				} else if (change.getObjectDelta().isDelete()) {
					// Sanity checks. We can remove them later when entire sync code is cleaned up.
					ShadowType shadowType = shadow.asObjectable();
					if (!ShadowUtil.isDead(shadowType)) {
						throw new IllegalStateException("Deleted "+shadow+" not dead");
					}
					if (ShadowUtil.isExists(shadowType)) {
						throw new IllegalStateException("Deleted "+shadow+" exists");
					}
				}
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

	private ObjectQuery createSearchShadowQueryByPrimaryIdentifier(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow, 
			PrismContext prismContext, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceShadow);
		PrismProperty identifier = attributesContainer.getPrimaryIdentifier();
		if (identifier == null) {
			return null;
		}

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
		
		return repositoryService.searchObjectsIterative(ShadowType.class, repoQuery, repoHandler, options, true, parentResult);
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
		if (!parentPath.equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
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
		List<PrismValue> newValues = new ArrayList<>();
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
			PrismObject<ShadowType> resourceShadow, OperationResult parentResult) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {
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
	
	public void addNewProposedShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState, 
			Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException, EncryptionException {
		if (!isUseProposedShadows(ctx)) {
			return;
		}
		
		PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
		if (repoShadow != null) {
			// TODO: should we add pending operation here?
			return;
		}
		
		// This is wrong: MID-4833
		repoShadow = createRepositoryShadow(ctx, shadowToAdd);
		repoShadow.asObjectable().setLifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);
		opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
		addPendingOperationAdd(repoShadow, shadowToAdd, opState, task.getTaskIdentifier());
		
		ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable());
		String oid = repositoryService.addObject(repoShadow, null, result);
		repoShadow.setOid(oid);
		LOGGER.trace("Proposed shadow added to the repository: {}", repoShadow);
		opState.setRepoShadow(repoShadow);
	}
	
	private boolean isUseProposedShadows(ProvisioningContext ctx) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceConsistencyType consistency = ctx.getResource().getConsistency();
		if (consistency == null) {
			return false;
		}
		return BooleanUtils.isTrue(consistency.isUseProposedShadows());
	}
	
	/**
	 * Record results of ADD operation to the shadow.
	 */
	public void recordAddResult(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {
		if (opState.getRepoShadow() == null) {
			recordAddResultNewShadow(ctx, shadowToAdd, opState, parentResult);
		} else {
			// We know that we have existing shadow. This may be proposed shadow,
			// or a shadow with failed add operation that was just re-tried
			recordAddResultExistingShadow(ctx, shadowToAdd, opState, parentResult);
		}
	}
	
	/**
	 * Add new active shadow to repository. It is executed after ADD operation on resource.
	 * There are several scenarios. The operation may have been executed (synchronous operation),
	 * it may be executing (asynchronous operation) or the operation may be delayed due to grouping.
	 * This is indicated by the execution status in the opState parameter. 
	 */
	private void recordAddResultNewShadow(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {
		//	TODO: check for proposed Shadow. There may be a proposed shadow even if we do not have explicit proposed shadow OID
		// (e.g. in case that the add operation failed). If proposed shadow is present do modify instead of add.
		
		PrismObject<ShadowType> resourceShadow = shadowToAdd;
		if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
			resourceShadow = opState.getAsyncResult().getReturnValue();
		}
		
		PrismObject<ShadowType> repoShadow = createRepositoryShadow(ctx, resourceShadow);
		opState.setRepoShadow(repoShadow);

		if (repoShadow == null) {
			parentResult
					.recordFatalError("Error while creating account shadow object to save in the reposiotory. Shadow is null.");
			throw new IllegalStateException(
					"Error while creating account shadow object to save in the reposiotory. Shadow is null.");
		}

		if (!opState.isCompleted()) {
			addPendingOperationAdd(repoShadow, resourceShadow, opState, null);
		}
		
		addCreateMetadata(repoShadow);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Adding repository shadow\n{}", repoShadow.debugDump(1));
		}
		String oid = null;
		
		try {
			
			ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable());
			oid = repositoryService.addObject(repoShadow, null, parentResult);

		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. The OID is not supplied and it is generated by the repo.
			// If it happens, it must be a repo bug.
			parentResult.recordFatalError(
					"Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException(
					"Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
		}
		repoShadow.setOid(oid);
		opState.setRepoShadow(repoShadow);

		LOGGER.trace("Active shadow added to the repository: {}", repoShadow);

		parentResult.recordSuccess();
	}
	
	

	@SuppressWarnings("unchecked")
	private void recordAddResultExistingShadow(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadowToAdd,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult)
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		
		final PrismObject<ShadowType> resourceShadow;
		if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
			resourceShadow = opState.getAsyncResult().getReturnValue();
		} else {
			resourceShadow = shadowToAdd;
		}
		
		PrismObject<ShadowType> repoShadow = opState.getRepoShadow();

		ObjectDelta<ShadowType> requestDelta = resourceShadow.createAddDelta();
		Collection<ItemDelta> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
		computeUpdateShadowAttributeChanges(ctx, internalShadowModifications, resourceShadow, repoShadow);
		addModifyMetadataDeltas(repoShadow, internalShadowModifications);
				
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updading repository shadow\n{}", DebugUtil.debugDump(internalShadowModifications, 1));
		}

		repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), internalShadowModifications, parentResult);

		LOGGER.trace("Repository shadow updated");

		parentResult.recordSuccess();
	}
	
	private List<ItemDelta> computeInternalShadowModifications(ProvisioningContext ctx,
			ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
			ObjectDelta<ShadowType> requestDelta) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
		List<ItemDelta> shadowModifications = new ArrayList<>();
		
		if (opState.hasPendingOperations()) {
		
			XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
			collectPendingOperationUpdates(shadowModifications, opState, null, now);
			
		} else {
			
			if (!opState.isCompleted()) {
				
				PrismContainerDefinition<PendingOperationType> containerDefinition = repoShadow.getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);
				ContainerDelta<PendingOperationType> pendingOperationDelta =containerDefinition.createEmptyDelta(ShadowType.F_PENDING_OPERATION);
				PendingOperationType pendingOperation = createPendingOperation(requestDelta, opState, null);
				pendingOperationDelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
				shadowModifications.add(pendingOperationDelta);
				opState.addPendingOperation(pendingOperation);
							
			}
		}
		
		if (opState.isCompleted() && opState.isSuccess()) {
			if (requestDelta.isDelete()) {
				addDeadShadowDeltas(repoShadow, opState.getAsyncResult(), shadowModifications);
			} else {
				if (!ShadowUtil.isExists(repoShadow.asObjectable())) {
					shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_EXISTS, null));
				}
			}
		}
		
		// TODO: this is wrong. Provisioning should not change lifecycle states. Just for compatibility. MID-4833
		if (isUseProposedShadows(ctx)) {
			String currentLifecycleState = repoShadow.asObjectable().getLifecycleState();
			if (currentLifecycleState != null && !currentLifecycleState.equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
				shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE));
			}
		}
		
		return shadowModifications;
	}
	
	public void addDeadShadowDeltas(PrismObject<ShadowType> repoShadow, AsynchronousOperationResult asyncResult, List<ItemDelta> shadowModifications) {
		LOGGER.trace("Marking shadow {} as dead", repoShadow);
		if (ShadowUtil.isExists(repoShadow.asObjectable())) {
			shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_EXISTS, Boolean.FALSE));
		}
		if (!ShadowUtil.isDead(repoShadow.asObjectable())) {
			shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_DEAD, Boolean.TRUE));
		}
	}
	
	private <T> PropertyDelta<T> createShadowPropertyReplaceDelta(PrismObject<ShadowType> repoShadow, QName propName, T value) {
		PrismPropertyDefinition<T> def = repoShadow.getDefinition().findPropertyDefinition(ItemName.fromQName(propName));
		PropertyDelta<T> delta = def.createEmptyDelta(ItemPath.create(propName));
		if (value == null) {
			delta.setValueToReplace();
		} else {
			delta.setRealValuesToReplace(value);
		}
		return delta;
	}
	
	/**
	 * Record results of an operation that have thrown exception.
	 * This happens after the error handler is processed - and only for those
	 * cases when the handler has re-thrown the exception.
	 */
	public void recordOperationException(
			ProvisioningContext ctx,
			ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
			ObjectDelta<ShadowType> delta,
			OperationResult parentResult) 
					throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
		if (repoShadow == null) {
			// Shadow does not exist. As this operation immediately ends up with an error then
			// we not even bother to create a shadow.
			return;
		}
		
		Collection<ItemDelta> shadowChanges = new ArrayList<>();
		
		if (opState.hasPendingOperations()) {
			XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
			collectPendingOperationUpdates(shadowChanges, opState, OperationResultStatus.FATAL_ERROR, now);
		}
		
		if (delta.isAdd()) {
			// This means we have failed add operation here. We tried to add object,
			// but we have failed. Which means that this shadow is now dead.
			shadowChanges.add(
				DeltaBuilder.deltaFor(ShadowType.class, prismContext)
					.item(ShadowType.F_DEAD).replace(true)
				.asItemDelta()
			);
		}
		
		if (shadowChanges.isEmpty()) {
			return;
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updading repository shadow (after error handling)\n{}", DebugUtil.debugDump(shadowChanges, 1));
		}

		repositoryService.modifyObject(ShadowType.class, opState.getRepoShadow().getOid(), shadowChanges, parentResult);
	}
	
	

	private void collectPendingOperationUpdates(Collection<ItemDelta> shadowChanges,
			ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
			OperationResultStatus implicitStatus,
			XMLGregorianCalendar now) {
		
		PrismContainerDefinition<PendingOperationType> containerDefinition = opState.getRepoShadow().getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);
		
		OperationResultStatus opStateResultStatus = opState.getResultStatus();
		if (opStateResultStatus == null) {
			opStateResultStatus = implicitStatus;
		}
		OperationResultStatusType opStateResultStatusType = null;
		if (opStateResultStatus != null) {
			opStateResultStatusType = opStateResultStatus.createStatusType();
		}
		String asynchronousOperationReference = opState.getAsynchronousOperationReference();
		
		for (PendingOperationType pendingOperation: opState.getPendingOperations()) {
			if (pendingOperation.asPrismContainerValue().getId() == null) {
				// This must be a new operation
				ContainerDelta<PendingOperationType> cdelta = new ContainerDeltaImpl<>(ShadowType.F_PENDING_OPERATION, containerDefinition, prismContext);
				cdelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
				shadowChanges.add(cdelta);
			} else {
				UniformItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
				
				if (!opState.getExecutionStatus().equals(pendingOperation.getExecutionStatus())) {
					PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
							PendingOperationType.F_EXECUTION_STATUS, opState.getExecutionStatus());
					shadowChanges.add(executionStatusDelta);
					
					if (opState.getExecutionStatus().equals(PendingOperationExecutionStatusType.EXECUTING) && pendingOperation.getOperationStartTimestamp() == null) {
						PropertyDelta<XMLGregorianCalendar> timestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
								PendingOperationType.F_OPERATION_START_TIMESTAMP, now);
						shadowChanges.add(timestampDelta);
					}
					
					if (opState.getExecutionStatus().equals(PendingOperationExecutionStatusType.COMPLETED) && pendingOperation.getCompletionTimestamp() == null) {
						PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
								PendingOperationType.F_COMPLETION_TIMESTAMP, now);
						shadowChanges.add(completionTimestampDelta);
					}
				}
				
				if (pendingOperation.getRequestTimestamp() == null) {
					// This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
					// Therefore unprecise timestamp is better than no timestamp.
					PropertyDelta<XMLGregorianCalendar> timestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
							PendingOperationType.F_REQUEST_TIMESTAMP, now);
					shadowChanges.add(timestampDelta);
				}
				
				if (opStateResultStatusType == null) {
					if (pendingOperation.getResultStatus() != null) {
						PropertyDelta<OperationResultStatusType> resultStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
								PendingOperationType.F_RESULT_STATUS, null);
						shadowChanges.add(resultStatusDelta);
					}
				} else {
					if (!opStateResultStatusType.equals(pendingOperation.getResultStatus())) {
						PropertyDelta<OperationResultStatusType> resultStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
								PendingOperationType.F_RESULT_STATUS, opStateResultStatusType);
						shadowChanges.add(resultStatusDelta);
					}
				}
				
				if (asynchronousOperationReference != null && !asynchronousOperationReference.equals(pendingOperation.getAsynchronousOperationReference())) {
					PropertyDelta<String> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
							PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOperationReference);
					shadowChanges.add(executionStatusDelta);
				}
				
				if (opState.getOperationType() != null && !opState.getOperationType().equals(pendingOperation.getType())) {
					PropertyDelta<PendingOperationTypeType> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
							PendingOperationType.F_TYPE, opState.getOperationType());
					shadowChanges.add(executionStatusDelta);
				}
			}
		}
		
	}
	
	private <T> PropertyDelta<T> createPendingOperationDelta(PrismContainerDefinition<PendingOperationType> containerDefinition, UniformItemPath containerPath, QName propName, T valueToReplace) {
		PrismPropertyDefinition<T> propDef = containerDefinition.findPropertyDefinition(ItemName.fromQName(propName));
		PropertyDelta<T> propDelta = new PropertyDeltaImpl<>(containerPath.append(propName), propDef, prismContext);
		if (valueToReplace == null) {
			propDelta.setValueToReplace();
		} else {
			propDelta.setRealValuesToReplace(valueToReplace);
		}
		return propDelta;
	}

	private void addPendingOperationAdd(
			PrismObject<ShadowType> repoShadow,
			PrismObject<ShadowType> resourceShadow,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState, String asyncOperationReference)
					throws SchemaException {
		
		ShadowType repoShadowType = repoShadow.asObjectable();
		PendingOperationType pendingOperation = createPendingOperation(resourceShadow.createAddDelta(), opState, asyncOperationReference);
		repoShadowType.getPendingOperation().add(pendingOperation);
		opState.addPendingOperation(pendingOperation);
		repoShadowType.setExists(false);
	}
	
	private PendingOperationType createPendingOperation(
			ObjectDelta<ShadowType> requestDelta,
			ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
			String asyncOperationReference) throws SchemaException {
		ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(requestDelta);
		PendingOperationType pendingOperation = new PendingOperationType();
		pendingOperation.setType(opState.getOperationType());
		pendingOperation.setDelta(deltaType);
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		pendingOperation.setRequestTimestamp(now);
		if (PendingOperationExecutionStatusType.EXECUTING.equals(opState.getExecutionStatus())) {
			pendingOperation.setOperationStartTimestamp(now);
		}
		pendingOperation.setExecutionStatus(opState.getExecutionStatus());
		pendingOperation.setResultStatus(opState.getResultStatusType());
		if (opState.getAttemptNumber() != null) {
			pendingOperation.setAttemptNumber(opState.getAttemptNumber());
			pendingOperation.setLastAttemptTimestamp(now);
		}
		if (asyncOperationReference != null) {
			pendingOperation.setAsynchronousOperationReference(asyncOperationReference);
		} else {
			pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
		}
		return pendingOperation;
	}
	
	// returns conflicting operation (pending delta) if there is any
	public PendingOperationType checkAndRecordPendingDeleteOperationBeforeExecution(ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			ProvisioningOperationState<AsynchronousOperationResult> opState,
			Task task, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		
		ObjectDelta<ShadowType> proposedDelta = shadow.createDeleteDelta();
		return checkAndRecordPendingOperationBeforeExecution(ctx, shadow, proposedDelta, opState, task, parentResult);
	}
	
	private PendingOperationType findExistingPendingOperation(PrismObject<ShadowType> currentShadow, ObjectDelta<ShadowType> proposedDelta, boolean processInProgress) throws SchemaException {
		for (PendingOperationType pendingOperation: currentShadow.asObjectable().getPendingOperation()) {
			OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
			if (!isInProgressOrRequested(resultStatus, processInProgress)) {
				continue;
			}
			ObjectDeltaType deltaType = pendingOperation.getDelta();
			if (deltaType == null) {
				continue;
			}
			ObjectDelta<Objectable> delta = DeltaConvertor.createObjectDelta(deltaType, prismContext);
			if (!matchPendingDelta(delta, proposedDelta)) {
				continue;
			}
			return pendingOperation;
		}
		return null;
	}
	
	private boolean isInProgressOrRequested(OperationResultStatusType resultStatus, boolean processInProgress) {
		if (resultStatus == null) {
			return true;
		}
		if (processInProgress && resultStatus == OperationResultStatusType.IN_PROGRESS) {
			return true;
		}
		return false;
	}

	public PendingOperationType checkAndRecordPendingModifyOperationBeforeExecution(ProvisioningContext ctx,
			PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			Task task, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectDelta<ShadowType> proposedDelta = createProposedModifyDelta(repoShadow, modifications);
		if (proposedDelta == null) {
			return null;
		}
		return checkAndRecordPendingOperationBeforeExecution(ctx, repoShadow, proposedDelta, opState, task, parentResult);
	}
	
	private ObjectDelta<ShadowType> createProposedModifyDelta(PrismObject<ShadowType> repoShadow, Collection<? extends ItemDelta> modifications) {
		Collection<ItemDelta> resourceModifications = new ArrayList<>(modifications.size());
		for (ItemDelta modification: modifications) {
			if (ProvisioningUtil.isResourceModification(modification)) {
				resourceModifications.add(modification);
			}
		}
		if (resourceModifications.isEmpty()) {
			return null;
		}
		return createModifyDelta(repoShadow, resourceModifications);
	}
	
	private ObjectDelta<ShadowType> createModifyDelta(PrismObject<ShadowType> repoShadow, Collection<? extends ItemDelta> modifications) {
		ObjectDelta<ShadowType> delta = repoShadow.createModifyDelta();
		delta.addModifications(ItemDelta.cloneCollection(modifications));
		return delta;
	}
	
	// returns conflicting operation (pending delta) if there is any
	private <A extends AsynchronousOperationResult> PendingOperationType checkAndRecordPendingOperationBeforeExecution(ProvisioningContext ctx,
				PrismObject<ShadowType> repoShadow, ObjectDelta<ShadowType> proposedDelta, 
				ProvisioningOperationState<A> opState,
				Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ResourceType resource = ctx.getResource();
		ResourceConsistencyType consistencyType = resource.getConsistency();
		if (consistencyType == null) {
			return null;
		}
		
		OptimisticLockingRunner<ShadowType,PendingOperationType> runner = new OptimisticLockingRunner.Builder<ShadowType,PendingOperationType>()
			.object(repoShadow)
			.result(parentResult)
			.repositoryService(repositoryService)
			.maxNumberOfAttempts(10)
			.delayRange(20)
			.build();
		
		try {
			
			return runner.run(
					(object) -> {
						Boolean avoidDuplicateOperations = consistencyType.isAvoidDuplicateOperations();
						if (BooleanUtils.isTrue(avoidDuplicateOperations)) {
							PendingOperationType existingPendingOperation = findExistingPendingOperation(object, proposedDelta, true);
							if (existingPendingOperation != null) {
								LOGGER.debug("Found duplicate operation for {} of {}: {}", proposedDelta.getChangeType(), object, existingPendingOperation);
								return existingPendingOperation;
							}
						}
						
						if (ResourceTypeUtil.getRecordPendingOperations(resource) != RecordPendingOperationsType.ALL) {
							return null;
						}
						
						LOGGER.trace("Storing pending operation for {} of {}", proposedDelta.getChangeType(), object);
						recordRequestedPendingOperationDelta(ctx, object, proposedDelta, opState, object.getVersion(), parentResult);
						LOGGER.trace("Stored pending operation for {} of {}", proposedDelta.getChangeType(), object);
						
						// Yes, really return null. We are supposed to return conflicting operation (if found).
						// But in this case there is no conflict. This operation does not conflict with itself.
						return null;
					}
			);
			
		} catch (ObjectAlreadyExistsException e) {
			// should not happen
			throw new SystemException(e);
		}
			
	}

	private <A extends AsynchronousOperationResult> void recordRequestedPendingOperationDelta(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
			ObjectDelta<ShadowType> pendingDelta, ProvisioningOperationState<A> opState, String readVersion,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, PreconditionViolationException {
		ObjectDeltaType pendingDeltaType = DeltaConvertor.toObjectDeltaType(pendingDelta);
				
		PendingOperationType pendingOperation = new PendingOperationType();
		pendingOperation.setDelta(pendingDeltaType);
		pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
		if (opState != null) {
			pendingOperation.setExecutionStatus(opState.getExecutionStatus());
			pendingOperation.setResultStatus(opState.getResultStatusType());
			pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
		}
		
		Collection repoDeltas = new ArrayList<>(1);
		ContainerDelta<PendingOperationType> cdelta = ContainerDeltaImpl.createDelta(ShadowType.F_PENDING_OPERATION, shadow.getDefinition());
		cdelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
		repoDeltas.add(cdelta);
		
		ModificationPrecondition<ShadowType> precondition = null;
		
		if (readVersion != null) {
			precondition = new VersionPrecondition<>(readVersion);
		}
		
		try {
			repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, precondition, null, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			// should not happen
			throw new SystemException(e);
		}
		
		// We have re-read shadow here. We need to get the pending operation in a form as it was stored. We need id in the operation.
		// Otherwise we won't be able to update it.
		PrismObject<ShadowType> newShadow = repositoryService.getObject(ShadowType.class, shadow.getOid(), null, parentResult);
		PendingOperationType storedPendingOperation = findExistingPendingOperation(newShadow, pendingDelta, true);
		if (storedPendingOperation == null) {
			// cannot find my own operation?
			throw new IllegalStateException("Cannot find my own operation "+pendingOperation+" in "+newShadow);
		}
		opState.addPendingOperation(storedPendingOperation);
	}

	private boolean matchPendingDelta(ObjectDelta<Objectable> pendingDelta, ObjectDelta<ShadowType> proposedDelta) {
		return pendingDelta.equivalent(proposedDelta);
	}

	public <A extends AsynchronousOperationResult> void updatePendingOperations(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			ProvisioningOperationState<A> opState,
			List<PendingOperationType> pendingExecutionOperations,
			XMLGregorianCalendar now,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
		Collection<? extends ItemDelta> repoDeltas = new ArrayList<>();
		OperationResultStatusType resultStatus = opState.getResultStatusType();
		String asynchronousOperationReference = opState.getAsynchronousOperationReference();
		PendingOperationExecutionStatusType executionStatus = opState.getExecutionStatus();
		
		for (PendingOperationType existingPendingOperation: pendingExecutionOperations) {
			UniformItemPath containerPath = existingPendingOperation.asPrismContainerValue().getPath();
			addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_EXECUTION_STATUS, executionStatus, shadow.getDefinition());
			addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_RESULT_STATUS, resultStatus, shadow.getDefinition());
			addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOperationReference, shadow.getDefinition());
			if (existingPendingOperation.getRequestTimestamp() == null) {
				// This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
				// Therefore unprecise timestamp is better than no timestamp.
				addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_REQUEST_TIMESTAMP, now, shadow.getDefinition());
			}
			if (executionStatus == PendingOperationExecutionStatusType.COMPLETED && existingPendingOperation.getCompletionTimestamp() == null) {
				addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_COMPLETION_TIMESTAMP, now, shadow.getDefinition());
			}
			if (executionStatus == PendingOperationExecutionStatusType.EXECUTING && existingPendingOperation.getOperationStartTimestamp() == null) {
				addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now, shadow.getDefinition());
			}
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Updating pending operations in {}:\n{}\nbased on opstate: {}", shadow, DebugUtil.debugDump(repoDeltas, 1), opState.shortDump());
		}
		
		try {
			repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			throw new SystemException(ex);
		}
	}
	
	private <T> void addPropertyDelta(Collection repoDeltas, UniformItemPath containerPath, QName propertyName, T propertyValue, PrismObjectDefinition<ShadowType> shadowDef) {
		UniformItemPath propPath = containerPath.append(propertyName);
		PropertyDelta<T> delta;
		if (propertyValue == null) {
			delta = PropertyDeltaImpl.createModificationReplaceProperty(propPath, shadowDef /* no value */);
		} else {
			delta = PropertyDeltaImpl.createModificationReplaceProperty(propPath, shadowDef,
				propertyValue);
		}
		repoDeltas.add(delta);
	}
	
	/**
	 * Create a copy of a shadow that is suitable for repository storage. 
	 */
	private PrismObject<ShadowType> createRepositoryShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException, EncryptionException {

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

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType creds = repoShadowType.getCredentials();
        if (creds != null) {
        	PasswordType passwordType = creds.getPassword();
        	if (passwordType != null) {
        		preparePasswordForStorage(passwordType, ctx.getObjectClassDefinition());
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
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource(), prismContext));
		}

		// if shadow does not contain resource or resource reference, create it
		// now
		if (repoShadowType.getResourceRef() == null) {
			repoShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(ctx.getResource(), prismContext));
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
	
	private void preparePasswordForStorage(PasswordType passwordType,
			RefinedObjectClassDefinition objectClassDefinition) throws SchemaException, EncryptionException {
		ProtectedStringType passwordValue = passwordType.getValue();
		if (passwordValue == null) {
			return;
		}		
		CachingStategyType cachingStategy = getPasswordCachingStrategy(objectClassDefinition);
		if (cachingStategy != null && cachingStategy != CachingStategyType.NONE) {
			if (!passwordValue.isHashed()) {
				protector.hash(passwordValue);
			}
			return;
		} else {
			ProvisioningUtil.cleanupShadowPassword(passwordType);
		}
	}
	
	private CachingStategyType getPasswordCachingStrategy(RefinedObjectClassDefinition objectClassDefinition) {
		ResourcePasswordDefinitionType passwordDefinition = objectClassDefinition.getPasswordDefinition();
		if (passwordDefinition == null) {
			return null;
		}
		CachingPolicyType passwordCachingPolicy = passwordDefinition.getCaching();
		if (passwordCachingPolicy == null) {
			return null;
		}
		return passwordCachingPolicy.getCachingStategy();
	}

	public void recordModifyResult(
				ProvisioningContext ctx, 
				PrismObject<ShadowType> oldRepoShadow, 
				Collection<? extends ItemDelta> requestedModifications, 
				ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
				XMLGregorianCalendar now,
				OperationResult parentResult) 
						throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException, EncryptionException {
		
		ObjectDelta<ShadowType> requestDelta = opState.getRepoShadow().createModifyDelta();
		requestDelta.addModifications(ItemDelta.cloneCollection(requestedModifications));
		
		List<ItemDelta> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
		
		List<ItemDelta> modifications;
		if (opState.isCompleted()) {
			modifications = MiscUtil.join(requestedModifications, (List)internalShadowModifications);
		} else {
			modifications = internalShadowModifications;
		}
		addModifyMetadataDeltas(opState.getRepoShadow(), modifications);
		LOGGER.trace("Updating repository {} after MODIFY operation {}, {} repository shadow modifications", oldRepoShadow, opState, requestedModifications.size());
		
		modifyShadowAttributes(ctx, oldRepoShadow, modifications, parentResult);

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
					"There are repository shadow changes, applying modifications {}",
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
	
	public boolean isRepositoryOnlyModification(Collection<? extends ItemDelta> modifications) {
		for (ItemDelta itemDelta : modifications) {
			if (isResourceModification(itemDelta)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isResourceModification(ItemDelta itemDelta) {
		UniformItemPath path = itemDelta.getPath();
		UniformItemPath parentPath = itemDelta.getParentPath();
		if (ShadowType.F_ATTRIBUTES.equivalent(parentPath)) {
			return true;
		}
		if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(path)) {
			return true;
		}
		if (ShadowType.F_ASSOCIATION.equivalent(parentPath)) {
			return true;
		}
		if (ShadowType.F_ASSOCIATION.equivalent(path)) {
			return true;
		}
		if (ShadowType.F_ACTIVATION.equivalent(parentPath)) {
			return true;
		}
		if (ShadowType.F_ACTIVATION.equivalent(path)) {		// should not occur, but for completeness...
			return true;
		}
		if (SchemaConstants.PATH_CREDENTIALS_PASSWORD.equivalent(parentPath)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private Collection<? extends ItemDelta> extractRepoShadowChanges(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> objectChange)
			throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
		Collection<ItemDelta> repoChanges = new ArrayList<>();
		for (ItemDelta itemDelta : objectChange) {
			if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
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
					PropertyDelta<PolyString> nameDelta = PropertyDeltaImpl.createReplaceDelta(shadow.getDefinition(), ShadowType.F_NAME, new PolyString(newName));
					repoChanges.add(nameDelta);
				}
				if (!ProvisioningUtil.shouldStoreAtributeInShadow(objectClassDefinition, attrName, cachingStrategy)) {
					continue;
				}
			} else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getParentPath())) {
				if (!ProvisioningUtil.shouldStoreActivationItemInShadow(itemDelta.getElementName(), cachingStrategy)) {
					continue;
				}
			} else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getPath())) {		// should not occur, but for completeness...
				for (PrismContainerValue<ActivationType> valueToAdd : ((ContainerDelta<ActivationType>) itemDelta).getValuesToAdd()) {
					ProvisioningUtil.cleanupShadowActivation(valueToAdd.asContainerable());
				}
				for (PrismContainerValue<ActivationType> valueToReplace : ((ContainerDelta<ActivationType>) itemDelta).getValuesToReplace()) {
					ProvisioningUtil.cleanupShadowActivation(valueToReplace.asContainerable());
				}
			} else if (SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
				addPasswordDelta(repoChanges, itemDelta, objectClassDefinition);
				continue;
			}
			normalizeDelta(itemDelta, objectClassDefinition);
			repoChanges.add(itemDelta);
		}
		
		
		return repoChanges;
	}
	
	private void addPasswordDelta(Collection<ItemDelta> repoChanges, ItemDelta requestedPasswordDelta, RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
		if (!(requestedPasswordDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE))) {
			return;
		}
		CachingStategyType cachingStategy = getPasswordCachingStrategy(objectClassDefinition);
		if (cachingStategy == null || cachingStategy == CachingStategyType.NONE) {
			return;
		}
		PropertyDelta<ProtectedStringType> passwordValueDelta = (PropertyDelta<ProtectedStringType>)requestedPasswordDelta;
		hashValues(passwordValueDelta.getValuesToAdd());
		hashValues(passwordValueDelta.getValuesToReplace());
		repoChanges.add(requestedPasswordDelta);
		if (!(requestedPasswordDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE))) {
			return;
		}
	}

	
	private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> pvals) throws SchemaException {
		if (pvals == null) {
			return;
		}
		for (PrismPropertyValue<ProtectedStringType> pval: pvals) {
			ProtectedStringType psVal = pval.getValue();
			if (psVal == null) {
				return;
			}
			if (psVal.isHashed()) {
				return;
			}
			try {
				protector.hash(psVal);
			} catch (EncryptionException e) {
				throw new SchemaException("Cannot hash value", e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<ItemDelta> updateShadow(ProvisioningContext ctx, PrismObject<ShadowType> resourceShadow,
			Collection<? extends ItemDelta> aprioriDeltas, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, resourceShadow.getOid(), null, result);
		
		Collection<ItemDelta> repoShadowChanges = new ArrayList<>();
		
		computeUpdateShadowAttributeChanges(ctx, repoShadowChanges, resourceShadow, repoShadow);

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
	
	private void computeUpdateShadowAttributeChanges(ProvisioningContext ctx, Collection<ItemDelta> repoShadowChanges, 
			PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
		for (RefinedAttributeDefinition attrDef: objectClassDefinition.getAttributeDefinitions()) {
			if (ProvisioningUtil.shouldStoreAtributeInShadow(objectClassDefinition, attrDef.getName(), cachingStrategy)) {
				ResourceAttribute<Object> resourceAttr = ShadowUtil.getAttribute(resourceShadow, attrDef.getName());
				PrismProperty<Object> repoAttr = repoShadow.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getName()));
				PropertyDelta attrDelta;
				if (resourceAttr == null && repoAttr == null) {
					continue;
				}
				ResourceAttribute<Object> normalizedResourceAttribute = resourceAttr.clone();
				normalizeAttribute(normalizedResourceAttribute, attrDef);
				if (repoAttr == null) {
					attrDelta = attrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getName()));
					attrDelta.setValuesToReplace(PrismValue.cloneCollection(normalizedResourceAttribute.getValues()));
				} else {
					attrDelta = repoAttr.diff(normalizedResourceAttribute);
//					LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}", repoAttr==null?null:repoAttr.debugDump(1), normalizedResourceAttribute==null?null:normalizedResourceAttribute.debugDump(1), attrDelta==null?null:attrDelta.debugDump(1));
				}
				if (attrDelta != null && !attrDelta.isEmpty()) {
					normalizeDelta(attrDelta, attrDef);
					repoShadowChanges.add(attrDelta);
				}
			}
		}

		// TODO: reflect activation updates on cached shadow
	}
	
	/**
	 * Updates repository shadow based on shadow from resource. Handles rename cases,
	 * change of auxiliary object classes, etc.
	 * @returns repository shadow as it should look like after the update
	 */
	@SuppressWarnings("unchecked")
	public PrismObject<ShadowType> updateShadow(ProvisioningContext ctx, PrismObject<ShadowType> currentResourceShadow,
			PrismObject<ShadowType> oldRepoShadow, ShadowState shadowState, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
		
		RefinedObjectClassDefinition ocDef = ctx.computeCompositeObjectClassDefinition(currentResourceShadow);
		ObjectDelta<ShadowType> shadowDelta = oldRepoShadow.createModifyDelta();
		PrismContainer<Containerable> currentResourceAttributesContainer = currentResourceShadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismContainer<Containerable> oldRepoAttributesContainer = oldRepoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		ShadowType oldRepoShadowType = oldRepoShadow.asObjectable();
		ShadowType currentResourceShadowType = currentResourceShadow.asObjectable();

		CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);

		for (Item<?, ?> currentResourceItem: currentResourceAttributesContainer.getValue().getItems()) {
			if (currentResourceItem instanceof PrismProperty<?>) {
				PrismProperty<Object> currentResourceAttrProperty = (PrismProperty<Object>) currentResourceItem;
				RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(currentResourceAttrProperty.getElementName());
				if (ProvisioningUtil.shouldStoreAtributeInShadow(ocDef, attrDef.getName(), cachingStrategy)) {
					MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
					PrismProperty<Object> oldRepoAttributeProperty = oldRepoAttributesContainer.findProperty(currentResourceAttrProperty.getElementName());
					if (oldRepoAttributeProperty == null) {
						PropertyDelta<Object> attrAddDelta = currentResourceAttrProperty.createDelta();
						for (PrismPropertyValue<?> pval: currentResourceAttrProperty.getValues()) {
							attrAddDelta.addRealValuesToAdd(matchingRule.normalize(pval.getValue()));
						}
						if (attrAddDelta.getDefinition().getTypeName() == null) {
							throw new SchemaException("No definition in "+attrAddDelta);
						}
						shadowDelta.addModification(attrAddDelta);
					} else {
						if (attrDef.isSingleValue()) {
							Object currentResourceRealValue = currentResourceAttrProperty.getRealValue();
							Object currentResourceNormalizedRealValue;
							currentResourceNormalizedRealValue = matchingRule.normalize(currentResourceRealValue);
							if (!currentResourceNormalizedRealValue.equals(oldRepoAttributeProperty.getRealValue())) {
								PropertyDelta delta = shadowDelta.addModificationReplaceProperty(currentResourceAttrProperty.getPath(), currentResourceNormalizedRealValue);
								delta.setDefinition(currentResourceAttrProperty.getDefinition());
								if (delta.getDefinition().getTypeName() == null) {
									throw new SchemaException("No definition in "+delta);
								}
							}
						} else {
							PrismProperty<Object> normalizedCurrentResourceAttrProperty = currentResourceAttrProperty.clone();
							for (PrismPropertyValue pval: normalizedCurrentResourceAttrProperty.getValues()) {
								Object normalizedRealValue = matchingRule.normalize(pval.getValue());
								pval.setValue(normalizedRealValue);
							}
							PropertyDelta<Object> attrDiff = oldRepoAttributeProperty.diff(normalizedCurrentResourceAttrProperty);
//							LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}",
//									oldRepoAttributeProperty==null?null:oldRepoAttributeProperty.debugDump(1),
//									normalizedCurrentResourceAttrProperty==null?null:normalizedCurrentResourceAttrProperty.debugDump(1),
//									attrDiff==null?null:attrDiff.debugDump(1));
							if (attrDiff != null && !attrDiff.isEmpty()) {
								attrDiff.setParentPath(ShadowType.F_ATTRIBUTES);
								if (attrDiff.getDefinition().getTypeName() == null) {
									throw new SchemaException("No definition in "+attrDiff);
								}
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
					oldRepoAttrPropDelta.addValuesToDelete((Collection) PrismPropertyValueImpl.cloneCollection(oldRepoAttrProperty.getValues()));
					if (oldRepoAttrPropDelta.getDefinition().getTypeName() == null) {
						throw new SchemaException("No definition in "+oldRepoAttrPropDelta);
					}
					shadowDelta.addModification(oldRepoAttrPropDelta);
				}
			}
		}
				
		PolyString currentShadowName = ShadowUtil.determineShadowName(currentResourceShadow);
		PolyString oldRepoShadowName = oldRepoShadow.getName();
		if (!currentShadowName.equalsOriginalValue(oldRepoShadowName)) {			
			PropertyDelta<?> shadowNameDelta = PropertyDeltaImpl.createModificationReplaceProperty(ShadowType.F_NAME,
					oldRepoShadow.getDefinition(),currentShadowName);
			shadowDelta.addModification(shadowNameDelta);
		}
		
		PropertyDelta<QName> auxOcDelta = (PropertyDelta)PrismPropertyImpl.diff(
				oldRepoShadow.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS),
				currentResourceShadow.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS));
		if (auxOcDelta != null) {
			shadowDelta.addModification(auxOcDelta);
		}
		
		// Resource object obviously exists in this case. However, we do not want to mess with isExists flag in some
		// situations (e.g. in CORPSE state) as this existence may be just a quantum illusion.
		if (shadowState == ShadowState.CONCEPTION || shadowState == ShadowState.GESTATION) {
			PropertyDelta<Boolean> existsDelta = shadowDelta.createPropertyModification(ShadowType.F_EXISTS);
			existsDelta.setRealValuesToReplace(true);
			shadowDelta.addModification(existsDelta);
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
		PropertyDelta<T> itemDelta = PrismPropertyImpl.diff(oldProperty, currentProperty);
		if (itemDelta != null && !itemDelta.isEmpty()) {
			shadowDelta.addModification(itemDelta);
		}
	}
	
	public PrismObject<ShadowType> recordDeleteResult(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> oldRepoShadow, 
			ProvisioningOperationState<AsynchronousOperationResult> opState,
			ProvisioningOperationOptions options,
			XMLGregorianCalendar now,
			OperationResult parentResult) 
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
		
		if (ProvisioningOperationOptions.isForce(options)) {
			LOGGER.trace("Deleting repository {} (force delete): {}", oldRepoShadow, opState);
			repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
			return null;
		}
		
		if (!opState.hasPendingOperations() && opState.isCompleted()) {
			if (oldRepoShadow.asObjectable().getPendingOperation().isEmpty() && opState.isSuccess()) {
				LOGGER.trace("Deleting repository {}: {}", oldRepoShadow, opState);
				repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
				return null;
			} else {
				// There are unexpired pending operations in the shadow. We cannot delete the shadow yet.
				// Therefore just mark shadow as dead.
				LOGGER.trace("Keeping dead {} because of pending operations or operation result", oldRepoShadow);
				return markShadowTombstone(oldRepoShadow, parentResult);
			}
		}
		LOGGER.trace("Recording pending delete operation in repository {}: {}", oldRepoShadow, opState);
		ObjectDelta<ShadowType> requestDelta = oldRepoShadow.createDeleteDelta();
		List<ItemDelta> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
		addModifyMetadataDeltas(opState.getRepoShadow(), internalShadowModifications);
		
		LOGGER.trace("Updating repository {} after DELETE operation {}, {} repository shadow modifications", oldRepoShadow, opState, internalShadowModifications.size());
		modifyShadowAttributes(ctx, oldRepoShadow, internalShadowModifications, parentResult);
		ObjectDelta.applyTo(oldRepoShadow, (List)internalShadowModifications);
		return oldRepoShadow;
	}
	
	public void deleteShadow(ProvisioningContext ctx, PrismObject<ShadowType> oldRepoShadow, OperationResult parentResult) 
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		LOGGER.trace("Deleting repository {}", oldRepoShadow);
		repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
	}
	
	
	public PrismObject<ShadowType> markShadowExists(PrismObject<ShadowType> repoShadow, OperationResult parentResult) throws SchemaException {
		List<ItemDelta<?, ?>> shadowChanges = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
			.item(ShadowType.F_EXISTS).replace(true)
		.asItemDeltas();
		LOGGER.trace("Marking shadow {} as existent", repoShadow);
		try {
			repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), shadowChanges, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			// Should not happen, this is not a rename
			new SystemException(e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			// Cannot be more dead
			LOGGER.trace("Attempt to mark shadow {} as existent found that no such shadow exists", repoShadow);
			return null;
		}
		ObjectDelta.applyTo(repoShadow, shadowChanges);
		return repoShadow;
	}
	
	public PrismObject<ShadowType> markShadowTombstone(PrismObject<ShadowType> repoShadow, OperationResult parentResult) throws SchemaException {
		if (repoShadow == null) {
			return null;
		}
		List<ItemDelta<?, ?>> shadowChanges = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
			.item(ShadowType.F_DEAD).replace(true)
			.item(ShadowType.F_EXISTS).replace(false)
		.asItemDeltas();
		LOGGER.trace("Marking shadow {} as tombstone", repoShadow);
		try {
			repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), shadowChanges, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			// Should not happen, this is not a rename
			new SystemException(e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			// Cannot be more dead
			LOGGER.trace("Attempt to mark shadow {} as tombstone found that no such shadow exists", repoShadow);
			return null;
		}
		ObjectDelta.applyTo(repoShadow, shadowChanges);
		return repoShadow;
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
		if (attributesContainer != null && attributesContainer.getValue() != null) {
			ObjectDelta<ShadowType> shadowDelta = currentRepoShadow.createModifyDelta();
			for (Item<?, ?> item: attributesContainer.getValue().getItems()) {
				if (item instanceof PrismProperty<?>) {
					PrismProperty<Object> attrProperty = (PrismProperty<Object>)item;
					RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(attrProperty.getElementName());
					if (attrDef == null) {
						// No definition for this property, it should not be in the shadow
						PropertyDelta<?> oldRepoAttrPropDelta = attrProperty.createDelta();
						oldRepoAttrPropDelta.addValuesToDelete((Collection)PrismPropertyValueImpl.cloneCollection(attrProperty.getValues()));
						shadowDelta.addModification(oldRepoAttrPropDelta);
					} else {
						attrProperty.applyDefinition(attrDef);
						MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
						List<Object> valuesToAdd = null;
						List<Object> valuesToDelete = null;
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
									valuesToAdd.add(normalizedRealValue);
									if (valuesToDelete == null) {
										valuesToDelete = new ArrayList<>();
									}
									valuesToDelete.add(currentRealValue);
								}
							}
						}
						PropertyDelta<Object> attrDelta = attrProperty.createDelta(attrProperty.getPath());
						if (valuesToAdd != null) {
							attrDelta.addRealValuesToAdd(valuesToAdd);
						}
						if (valuesToDelete != null) {
							attrDelta.addRealValuesToDelete(valuesToDelete);
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
		if (!delta.getPath().startsWithName(ShadowType.F_ATTRIBUTES)) {
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
			Collection<T> normalizedValues = new ArrayList<>();
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

	// Just minimal metadata for now, maybe we need to expand that later
	// those are needed to properly manage dead shadows
	private void addCreateMetadata(PrismObject<ShadowType> repoShadow) {
		ShadowType repoShadowType = repoShadow.asObjectable();
		MetadataType metadata = repoShadowType.getMetadata();
		if (metadata != null) {
			return;
		}
		metadata = new MetadataType();
		repoShadowType.setMetadata(metadata);
		metadata.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());
	}
	
	// Just minimal metadata for now, maybe we need to expand that later
	// those are needed to properly manage dead shadows
	private void addModifyMetadataDeltas(PrismObject<ShadowType> repoShadow, Collection<ItemDelta> shadowChanges) {
		PropertyDelta<XMLGregorianCalendar> modifyTimestampDelta = ItemDelta.findPropertyDelta(shadowChanges, SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
		if (modifyTimestampDelta != null) {
			return;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Metadata not found, adding minimal metadata. Modifications:\n{}", DebugUtil.debugDump(shadowChanges, 1));
		}
		PrismPropertyDefinition<XMLGregorianCalendar> def = repoShadow.getDefinition().findPropertyDefinition(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
		modifyTimestampDelta = def.createEmptyDelta(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
		modifyTimestampDelta.setRealValuesToReplace(clock.currentTimeXMLGregorianCalendar());
		shadowChanges.add(modifyTimestampDelta);
	}
}
