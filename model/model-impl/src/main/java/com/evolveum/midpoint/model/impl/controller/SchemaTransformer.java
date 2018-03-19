/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.util.ModelUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Transforms the schema and objects by applying security constraints,
 * object template schema refinements, etc.
 *
 * @author semancik
 */
@Component
public class SchemaTransformer {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;

	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;

	@Autowired
	private PrismContext prismContext;

	// TODO why are the following two methods distinct? Clarify their names.
	public <T extends ObjectType> void applySchemasAndSecurityToObjectTypes(List<T> objectTypes,
			GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options,
			AuthorizationPhaseType phase, Task task, OperationResult result)
					throws SecurityViolationException, SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
		for (int i = 0; i < objectTypes.size(); i++) {
			PrismObject<T> object = (PrismObject<T>) objectTypes.get(i).asPrismObject();
			object = object.cloneIfImmutable();
			objectTypes.set(i, object.asObjectable());
			applySchemasAndSecurity(object, rootOptions, options, phase, task, result);
		}
	}

	public <T extends ObjectType> void applySchemasAndSecurityToObjects(List<PrismObject<T>> objects,
			GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options,
			AuthorizationPhaseType phase, Task task, OperationResult result)
					throws SecurityViolationException, SchemaException {
		for (int i = 0; i < objects.size(); i++) {
			PrismObject<T> object = objects.get(i);
			object = object.cloneIfImmutable();
			objects.set(i, object);
			applySchemaAndSecurityToObject(object, rootOptions, options, phase, task, result);
		}
	}

	// Expecting that C is a direct child of T.
	// Expecting that container values point to their respective parents (in order to evaluate the security!)
	public <C extends Containerable, T extends ObjectType>
	SearchResultList<C> applySchemasAndSecurityToContainers(SearchResultList<C> originalResultList, Class<T> parentObjectType, QName childItemName,
			GetOperationOptions rootOptions, Collection<SelectorOptions<GetOperationOptions>> options, AuthorizationPhaseType phase, Task task, OperationResult result)
			throws SecurityViolationException, SchemaException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException, CommunicationException {

		List<C> newValues = new ArrayList<>();
		Map<PrismObject<T>,Object> processedParents = new IdentityHashMap<>();
		final ItemPath childItemPath = new ItemPath(childItemName);

		for (C value: originalResultList) {
			Long originalId = value.asPrismContainerValue().getId();
			if (originalId == null) {
				throw new SchemaException("No ID in container value " + value);
			}
			PrismObject<T> parent = ObjectTypeUtil.getParentObject(value);
			boolean wasProcessed;
			if (parent != null) {
				wasProcessed = processedParents.containsKey(parent);
			} else {
				// temporary solution TODO reconsider
				parent = prismContext.createObject(parentObjectType);
				PrismContainer<C> childContainer = parent.findOrCreateItem(childItemPath, PrismContainer.class);
				childContainer.add(value.asPrismContainerValue());
				wasProcessed = false;
			}
			if (!wasProcessed) {
				// TODO what if parent is immutable?
				applySchemasAndSecurity(parent, rootOptions, options, phase, task, result);
				processedParents.put(parent, null);
			}
			PrismContainer<C> updatedChildContainer = parent.findContainer(childItemPath);
			if (updatedChildContainer != null) {
				PrismContainerValue<C> updatedChildValue = updatedChildContainer.getValue(originalId);
				if (updatedChildValue != null) {
					newValues.add(updatedChildValue.asContainerable());
				}
			}
		}
		return new SearchResultList<>(newValues, originalResultList.getMetadata());
	}

	private <T extends ObjectType> void applySchemaAndSecurityToObject(PrismObject<T> object, GetOperationOptions rootOptions,
			Collection<SelectorOptions<GetOperationOptions>> options, AuthorizationPhaseType phase, Task task, OperationResult result) throws SecurityViolationException {
		OperationResult subresult = new OperationResult(SchemaTransformer.class.getName()+".applySchemasAndSecurityToObjects");
		try {
            applySchemasAndSecurity(object, rootOptions, options, phase, task, subresult);
        } catch (IllegalArgumentException | IllegalStateException | SchemaException |ConfigurationException |ObjectNotFoundException | ExpressionEvaluationException | CommunicationException e) {
            LOGGER.error("Error post-processing object {}: {}", object, e.getMessage(), e);
            OperationResultType fetchResult = object.asObjectable().getFetchResult();
            if (fetchResult == null) {
                fetchResult = subresult.createOperationResultType();
                object.asObjectable().setFetchResult(fetchResult);
            } else {
                fetchResult.getPartialResults().add(subresult.createOperationResultType());
            }
            fetchResult.setStatus(OperationResultStatusType.FATAL_ERROR);
        } catch (SecurityViolationException e) {
        	// We cannot go on and leave this object in the result set. The object was not post-processed.
        	// Leaving it in the result set may leak information.
        	result.recordFatalError(e);
        	throw e;
		}
	}
	
	private <O extends ObjectType> void authorizeOptions(GetOperationOptions rootOptions, PrismObject<O> object, ObjectDelta<O> delta, AuthorizationPhaseType phase, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		if (GetOperationOptions.isRaw(rootOptions)) {
			securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), phase, AuthorizationParameters.Builder.buildObjectDelta(object, delta), null, task, result);
		}
	}

	/**
	 * Validate the objects, apply security to the object definition, remove any non-visible properties (security),
	 * apply object template definitions and so on. This method is called for
	 * any object that is returned from the Model Service.  
	 */
	public <O extends ObjectType> void applySchemasAndSecurity(PrismObject<O> object, GetOperationOptions rootOptions,
			Collection<SelectorOptions<GetOperationOptions>> options,
			AuthorizationPhaseType phase, Task task, OperationResult parentResult)
					throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
		LOGGER.trace("applySchemasAndSecurity starting");
    	OperationResult result = parentResult.createMinorSubresult(SchemaTransformer.class.getName()+".applySchemasAndSecurity");
    	authorizeOptions(rootOptions, object, null, phase, task, result);
    	validateObject(object, rootOptions, result);

    	PrismObjectDefinition<O> objectDefinition = object.deepCloneDefinition(true, this::setFullAccessFlags);

    	ObjectSecurityConstraints securityConstraints;
    	try {
	    	securityConstraints = securityEnforcer.compileSecurityConstraints(object, null, task, result);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Security constraints for {}:\n{}", object, securityConstraints==null?"null":securityConstraints.debugDump());
			}
			if (securityConstraints == null) {
				SecurityUtil.logSecurityDeny(object, "because no security constraints are defined (default deny)");
				throw new AuthorizationException("Access denied");
			}
    	} catch (SecurityViolationException | SchemaException | RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}

    	if (phase == null) {
    		if (!GetOperationOptions.isExecutionPhase(rootOptions)) {
    			applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, AuthorizationPhaseType.REQUEST, task, result);
    		}
    		applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, AuthorizationPhaseType.EXECUTION, task, result);
    	} else {
    		if (phase == AuthorizationPhaseType.REQUEST && GetOperationOptions.isExecutionPhase(rootOptions)) {
    			// Skip application of security constraints for request phase.
    			// The caller asked to skip evaluation of request authorization, so everything is allowed here.
    		} else {
    			applySchemasAndSecurityPhase(object, securityConstraints, objectDefinition, rootOptions, phase, task, result);
    		}
    	}

		ObjectTemplateType objectTemplateType;
		try {
			objectTemplateType = determineObjectTemplate(object, AuthorizationPhaseType.REQUEST, result);
		} catch (ConfigurationException | ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		}
		applyObjectTemplateToObject(object, objectTemplateType, result);

		if (CollectionUtils.isNotEmpty(options)) {
			Map<DefinitionProcessingOption, Collection<ItemPath>> definitionProcession = SelectorOptions.extractOptionValues(options, (o) -> o.getDefinitionProcessing());
			if (CollectionUtils.isNotEmpty(definitionProcession.get(DefinitionProcessingOption.NONE))) {
				throw new UnsupportedOperationException("'NONE' definition processing is not supported now");
			}
			Collection<ItemPath> onlyIfExists = definitionProcession.get(DefinitionProcessingOption.ONLY_IF_EXISTS);
			if (CollectionUtils.isNotEmpty(onlyIfExists)) {
				if (onlyIfExists.size() != 1 || !ItemPath.isNullOrEmpty(onlyIfExists.iterator().next())) {
					throw new UnsupportedOperationException("'ONLY_IF_EXISTS' definition processing is currently supported on root level only; not on " + onlyIfExists);
				}
				Collection<ItemPath> full = definitionProcession.get(DefinitionProcessingOption.FULL);
				object.trimDefinitionTree(full);
			}
		}

		result.computeStatus();
		result.recordSuccessIfUnknown();
		LOGGER.trace("applySchemasAndSecurity finishing");			// to allow folding in log viewer
    }
	
	public void setFullAccessFlags(ItemDefinition<?> itemDef) {		
		itemDef.setCanRead(true);
		itemDef.setCanAdd(true);
		itemDef.setCanModify(true);
	}

	private <O extends ObjectType> void applySchemasAndSecurityPhase(PrismObject<O> object, ObjectSecurityConstraints securityConstraints, PrismObjectDefinition<O> objectDefinition,
			GetOperationOptions rootOptions, AuthorizationPhaseType phase, Task task, OperationResult result)
					throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		Validate.notNull(phase);
		try {
			AuthorizationDecisionType globalReadDecision = securityConstraints.getActionDecision(ModelAuthorizationAction.READ.getUrl(), phase);
			if (globalReadDecision == AuthorizationDecisionType.DENY) {
				// shortcut
				SecurityUtil.logSecurityDeny(object, "because the authorization denies access");
				throw new AuthorizationException("Access denied");
			}

			AuthorizationDecisionType globalAddDecision = securityConstraints.getActionDecision(ModelAuthorizationAction.ADD.getUrl(), phase);
			AuthorizationDecisionType globalModifyDecision = securityConstraints.getActionDecision(ModelAuthorizationAction.MODIFY.getUrl(), phase);
			applySecurityConstraints(object.getValue().getItems(), securityConstraints, globalReadDecision,
					globalAddDecision, globalModifyDecision, phase);
			if (object.isEmpty()) {
				// let's make it explicit
				SecurityUtil.logSecurityDeny(object, "because the subject has not access to any item");
				throw new AuthorizationException("Access denied");
			}

			applySecurityConstraintsItemDef(objectDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints, globalReadDecision, globalAddDecision, globalModifyDecision, phase);
		} catch (SecurityViolationException | RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	public void applySecurityConstraints(List<Item<?,?>> items, ObjectSecurityConstraints securityConstraints,
			AuthorizationDecisionType defaultReadDecision, AuthorizationDecisionType defaultAddDecision, AuthorizationDecisionType defaultModifyDecision,
			AuthorizationPhaseType phase) {
		LOGGER.trace("applySecurityConstraints(items): items={}, phase={}, defaults R={}, A={}, M={}",
				items, phase, defaultReadDecision, defaultAddDecision, defaultModifyDecision);
		if (items == null) {
			return;
		}
		Iterator<Item<?,?>> iterator = items.iterator();
		while (iterator.hasNext()) {
			Item<?,?> item = iterator.next();
			ItemPath itemPath = item.getPath();
			ItemDefinition<?> itemDef = item.getDefinition();
			if (itemDef != null && itemDef.isElaborate()) {
				LOGGER.trace("applySecurityConstraints(item): {}: skip (elaborate)", itemPath);
				continue;
			}
			ItemPath nameOnlyItemPath = itemPath.namedSegmentsOnly();
			AuthorizationDecisionType itemReadDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.READ.getUrl(), defaultReadDecision, phase);
			AuthorizationDecisionType itemAddDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.ADD.getUrl(), defaultReadDecision, phase);
			AuthorizationDecisionType itemModifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.MODIFY.getUrl(), defaultReadDecision, phase);
			LOGGER.trace("applySecurityConstraints(item): {}: decisions R={}, A={}, M={}",
					itemPath, itemReadDecision, itemAddDecision, itemModifyDecision);
			if (itemDef != null) {
				if (itemReadDecision != AuthorizationDecisionType.ALLOW) {
					((ItemDefinitionImpl) itemDef).setCanRead(false);
				}
				if (itemAddDecision != AuthorizationDecisionType.ALLOW) {
					((ItemDefinitionImpl) itemDef).setCanAdd(false);
				}
				if (itemModifyDecision != AuthorizationDecisionType.ALLOW) {
					((ItemDefinitionImpl) itemDef).setCanModify(false);
				}
			}
			if (item instanceof PrismContainer<?>) {
				if (itemReadDecision == AuthorizationDecisionType.DENY) {
					// Explicitly denied access to the entire container
					iterator.remove();
				} else {
					// No explicit decision (even ALLOW is not final here as something may be denied deeper inside)
					AuthorizationDecisionType subDefaultReadDecision = defaultReadDecision;
					if (itemReadDecision == AuthorizationDecisionType.ALLOW) {
						// This means allow to all subitems unless otherwise denied.
						subDefaultReadDecision = AuthorizationDecisionType.ALLOW;
					}
					boolean itemWasEmpty = item.isEmpty();		// to prevent removal of originally empty items
					List<? extends PrismContainerValue<?>> values = ((PrismContainer<?>)item).getValues();
					Iterator<? extends PrismContainerValue<?>> vi = values.iterator();
					while (vi.hasNext()) {
						PrismContainerValue<?> cval = vi.next();
						List<Item<?,?>> subitems = cval.getItems();
						if (subitems != null && !subitems.isEmpty()) {	// second condition is to prevent removal of originally empty values
							applySecurityConstraints(subitems, securityConstraints, subDefaultReadDecision, itemAddDecision, itemModifyDecision, phase);
							if (subitems.isEmpty()) {
								vi.remove();
							}
						}
					}
					if (!itemWasEmpty && item.isEmpty()) {
						iterator.remove();
					}
				}
			} else {
				if (itemReadDecision == AuthorizationDecisionType.DENY || (itemReadDecision == null && defaultReadDecision == null)) {
					iterator.remove();
				}
			}
		}
	}

	public <D extends ItemDefinition> void applySecurityConstraints(D itemDefinition, ObjectSecurityConstraints securityConstraints,
            AuthorizationPhaseType phase) {
		if (phase == null) {
			applySecurityConstraintsPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.REQUEST);
			applySecurityConstraintsPhase(itemDefinition, securityConstraints, AuthorizationPhaseType.EXECUTION);
		} else {
			applySecurityConstraintsPhase(itemDefinition, securityConstraints, phase);
		}
	}

	private <D extends ItemDefinition> void applySecurityConstraintsPhase(D itemDefinition, ObjectSecurityConstraints securityConstraints,
            AuthorizationPhaseType phase) {
		Validate.notNull(phase);
		LOGGER.trace("applySecurityConstraints(itemDefs): def={}, phase={}", itemDefinition, phase);
		applySecurityConstraintsItemDef(itemDefinition, new IdentityHashMap<>(), ItemPath.EMPTY_PATH, securityConstraints,
				null, null, null, phase);

	}

	private <D extends ItemDefinition> void applySecurityConstraintsItemDef(D itemDefinition,
			IdentityHashMap<ItemDefinition, Object> definitionsSeen,
			ItemPath nameOnlyItemPath, ObjectSecurityConstraints securityConstraints, AuthorizationDecisionType defaultReadDecision,
			AuthorizationDecisionType defaultAddDecision, AuthorizationDecisionType defaultModifyDecision,
            AuthorizationPhaseType phase) {

		boolean thisWasSeen = definitionsSeen.containsKey(itemDefinition);
		definitionsSeen.put(itemDefinition, null);

		AuthorizationDecisionType readDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.READ.getUrl(), defaultReadDecision, phase);
		AuthorizationDecisionType addDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.ADD.getUrl(), defaultAddDecision, phase);
		AuthorizationDecisionType modifyDecision = computeItemDecision(securityConstraints, nameOnlyItemPath, ModelAuthorizationAction.MODIFY.getUrl(), defaultModifyDecision, phase);

		boolean anySubElementRead = false;
		boolean anySubElementAdd = false;
		boolean anySubElementModify = false;
		if (itemDefinition instanceof PrismContainerDefinition<?> && !thisWasSeen) {
			PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>)itemDefinition;
			List<? extends ItemDefinition> subDefinitions = ((PrismContainerDefinition<?>)containerDefinition).getDefinitions();
			for (ItemDefinition subDef: subDefinitions) {
				ItemPath subPath = new ItemPath(nameOnlyItemPath, subDef.getName());
				if (subDef.isElaborate()) {
					LOGGER.trace("applySecurityConstraints(itemDef): {}: skip (elaborate)", subPath);
					continue;
				}
				if (!subDef.getName().equals(ShadowType.F_ATTRIBUTES)) { // Shadow attributes have special handling
					applySecurityConstraintsItemDef(subDef, definitionsSeen, subPath, securityConstraints,
					    readDecision, addDecision, modifyDecision, phase);
				}
				if (subDef.canRead()) {
					anySubElementRead = true;
				}
				if (subDef.canAdd()) {
					anySubElementAdd = true;
				}
				if (subDef.canModify()) {
					anySubElementModify = true;
				}
			}
		}

		LOGGER.trace("applySecurityConstraints(itemDef): {}: decisions R={}, A={}, M={}; subelements R={}, A={}, M={}",
				nameOnlyItemPath, readDecision, addDecision, modifyDecision, anySubElementRead, anySubElementAdd, anySubElementModify);

		if (readDecision != AuthorizationDecisionType.ALLOW) {
			((ItemDefinitionImpl) itemDefinition).setCanRead(false);
		}
		if (addDecision != AuthorizationDecisionType.ALLOW) {
			((ItemDefinitionImpl) itemDefinition).setCanAdd(false);
		}
		if (modifyDecision != AuthorizationDecisionType.ALLOW) {
			((ItemDefinitionImpl) itemDefinition).setCanModify(false);
		}
		
		if (anySubElementRead) {
			((ItemDefinitionImpl) itemDefinition).setCanRead(true);
		}
		if (anySubElementAdd) {
			((ItemDefinitionImpl) itemDefinition).setCanAdd(true);
		}
		if (anySubElementModify) {
			((ItemDefinitionImpl) itemDefinition).setCanModify(true);
		}
	}

    public AuthorizationDecisionType computeItemDecision(ObjectSecurityConstraints securityConstraints, ItemPath nameOnlyItemPath, String actionUrl,
			AuthorizationDecisionType defaultDecision, AuthorizationPhaseType phase) {
    	AuthorizationDecisionType explicitDecision = securityConstraints.findItemDecision(nameOnlyItemPath, actionUrl, phase);
//    	LOGGER.trace("Explicit decision for {} ({} {}): {}", nameOnlyItemPath, actionUrl, phase, explicitDecision);
    	if (explicitDecision != null) {
    		return explicitDecision;
    	} else {
    		return defaultDecision;
    	}
	}

    public <O extends ObjectType> ObjectTemplateType determineObjectTemplate(PrismObject<O> object, AuthorizationPhaseType phase, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
    	PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
    	if (systemConfiguration == null) {
    		return null;
    	}
    	ObjectPolicyConfigurationType objectPolicyConfiguration = ModelUtils.determineObjectPolicyConfiguration(object, systemConfiguration.asObjectable());
    	if (objectPolicyConfiguration == null) {
    		return null;
    	}
    	ObjectReferenceType objectTemplateRef = objectPolicyConfiguration.getObjectTemplateRef();
    	if (objectTemplateRef == null) {
    		return null;
    	}
    	PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), null, result);
    	return template.asObjectable();
    }

    public <O extends ObjectType> ObjectTemplateType determineObjectTemplate(Class<O> objectClass, AuthorizationPhaseType phase, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
    	PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
    	if (systemConfiguration == null) {
    		return null;
    	}
    	ObjectPolicyConfigurationType objectPolicyConfiguration = ModelUtils.determineObjectPolicyConfiguration(objectClass, null, systemConfiguration.asObjectable());
    	if (objectPolicyConfiguration == null) {
    		return null;
    	}
    	ObjectReferenceType objectTemplateRef = objectPolicyConfiguration.getObjectTemplateRef();
    	if (objectTemplateRef == null) {
    		return null;
    	}
    	PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, objectTemplateRef.getOid(), null, result);
    	return template.asObjectable();
    }

    public <O extends ObjectType> void applyObjectTemplateToDefinition(PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplateType, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (objectTemplateType == null) {
			return;
		}
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(ObjectTemplateType.class, includeRef.getOid(), null, result);
			applyObjectTemplateToDefinition(objectDefinition, subTemplate.asObjectable(), result);
		}
		for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
                ItemPathType ref = templateItemDefType.getRef();
                if (ref == null) {
				throw new SchemaException("No 'ref' in item definition in "+objectTemplateType);
                }
                ItemPath itemPath = ref.getItemPath();
                ItemDefinition itemDef = objectDefinition.findItemDefinition(itemPath);
                if (itemDef != null) {
                    applyObjectTemplateItem(itemDef, templateItemDefType, "item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplateType);
                } else {
                    OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToDefinition");
                    subResult.recordPartialError("No definition for item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplateType);
                    continue;
                }
		}
	}

	private <O extends ObjectType> void applyObjectTemplateToObject(PrismObject<O> object, ObjectTemplateType objectTemplateType, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (objectTemplateType == null) {
			return;
		}
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> subTemplate = cacheRepositoryService.getObject(ObjectTemplateType.class, includeRef.getOid(), null, result);
			applyObjectTemplateToObject(object, subTemplate.asObjectable(), result);
		}
		for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
			ItemPathType ref = templateItemDefType.getRef();
			if (ref == null) {
				throw new SchemaException("No 'ref' in item definition in "+objectTemplateType);
			}
			ItemPath itemPath = ref.getItemPath();
			ItemDefinition itemDefFromObject = object.getDefinition().findItemDefinition(itemPath);
            if (itemDefFromObject != null) {
                applyObjectTemplateItem(itemDefFromObject, templateItemDefType, "item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplateType);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToObject");
                subResult.recordPartialError("No definition for item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplateType);
                continue;
            }
			Item<?, ?> item = object.findItem(itemPath);
			if (item != null) {
				ItemDefinition itemDef = item.getDefinition();
				if (itemDef != itemDefFromObject) {
					applyObjectTemplateItem(itemDef, templateItemDefType, "item "+itemPath+" in " + object
							+ " as specified in item definition in "+objectTemplateType);
				}
			}

		}
	}

	private <IV extends PrismValue,ID extends ItemDefinition> void applyObjectTemplateItem(ID itemDef,
			ObjectTemplateItemDefinitionType templateItemDefType, String desc) throws SchemaException {
		if (itemDef == null) {
			throw new SchemaException("No definition for "+desc);
		}

		String displayName = templateItemDefType.getDisplayName();
		if (displayName != null) {
			((ItemDefinitionImpl) itemDef).setDisplayName(displayName);
		}

		Integer displayOrder = templateItemDefType.getDisplayOrder();
		if (displayOrder != null) {
			((ItemDefinitionImpl) itemDef).setDisplayOrder(displayOrder);
		}

		Boolean emphasized = templateItemDefType.isEmphasized();
		if (emphasized != null) {
			((ItemDefinitionImpl) itemDef).setEmphasized(emphasized);
		}

		List<PropertyLimitationsType> limitations = templateItemDefType.getLimitations();
		if (limitations != null) {
			PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsType(limitations, LayerType.PRESENTATION);
			if (limitationsType != null) {
				if (limitationsType.getMinOccurs() != null) {
					((ItemDefinitionImpl) itemDef).setMinOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMinOccurs()));
				}
				if (limitationsType.getMaxOccurs() != null) {
					((ItemDefinitionImpl) itemDef).setMaxOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMaxOccurs()));
				}
				if (limitationsType.isIgnore() != null) {
					((ItemDefinitionImpl) itemDef).setIgnored(limitationsType.isIgnore());
				}
				PropertyAccessType accessType = limitationsType.getAccess();
				if (accessType != null) {
					if (accessType.isAdd() != null) {
						((ItemDefinitionImpl) itemDef).setCanAdd(accessType.isAdd());
					}
					if (accessType.isModify() != null) {
						((ItemDefinitionImpl) itemDef).setCanModify(accessType.isModify());
					}
					if (accessType.isRead() != null) {
						((ItemDefinitionImpl) itemDef).setCanRead(accessType.isRead());
					}
				}
			}
		}

		ObjectReferenceType valueEnumerationRef = templateItemDefType.getValueEnumerationRef();
		if (valueEnumerationRef != null) {
			PrismReferenceValue valueEnumerationRVal = MiscSchemaUtil.objectReferenceTypeToReferenceValue(valueEnumerationRef);
			((ItemDefinitionImpl) itemDef).setValueEnumerationRef(valueEnumerationRVal);
		}
	}


	private <T extends ObjectType> void validateObject(PrismObject<T> object, GetOperationOptions options, OperationResult result) {
		try {
			if (InternalsConfig.readEncryptionChecks) {
				CryptoUtil.checkEncrypted(object);
			}
			if (!InternalsConfig.consistencyChecks) {
				return;
			}
			Class<T> type = object.getCompileTimeClass();
			boolean tolerateRaw = GetOperationOptions.isTolerateRawData(options);
			if (type == ResourceType.class || ShadowType.class.isAssignableFrom(type) || type == ReportType.class) {
				// We tolerate raw values for resource and shadows in case the user has requested so
				tolerateRaw = GetOperationOptions.isRaw(options);
			}
			if (hasError(object, result)) {
				// If there is an error then the object might not be complete.
				// E.g. we do not have a complete dynamic schema to apply to the object
				// Tolerate some raw meat in that case.
				tolerateRaw = true;
			}
			if (InternalsConfig.consistencyChecks) {
				object.checkConsistence(true, !tolerateRaw, ConsistencyCheckScope.THOROUGH);
			}
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
		}
	}

	private <T extends ObjectType> boolean hasError(PrismObject<T> object, OperationResult result) {
		if (result != null && result.isError()) {		// actually, result is pretty tiny here - does not include object fetch/get operation
			return true;
		}
		OperationResultType fetchResult = object.asObjectable().getFetchResult();
		if (fetchResult != null &&
				(fetchResult.getStatus() == OperationResultStatusType.FATAL_ERROR ||
				fetchResult.getStatus() == OperationResultStatusType.PARTIAL_ERROR)) {
			return true;
		}
		return false;
	}

}
