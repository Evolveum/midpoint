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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class LensUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);
	
	public static <F extends ObjectType, P extends ObjectType> void traceContext(Trace logger, String activity, String phase, 
			boolean important,  LensContext<F,P> context, boolean showTriples) throws SchemaException {
        if (logger.isTraceEnabled()) {
        	logger.trace("Lens context:\n"+
            		"---[ {} context {} ]--------------------------------\n"+
            		"{}\n",
            		new Object[]{activity, phase, context.dump(showTriples)});
        }
    }
	
	public static <F extends ObjectType, P extends ObjectType> ResourceType getResource(LensContext<F,P> context,
			String resourceOid, ProvisioningService provisioningService, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resourceType = context.getResource(resourceOid);
		if (resourceType == null) {
			// Fetching from provisioning to take advantage of caching and
			// pre-parsed schema
			resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, null, result)
					.asObjectable();
			context.rememberResource(resourceType);
		}
		return resourceType;
	}
	
	public static String refineAccountType(String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
		RefinedObjectClassDefinition accountDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, intent);
		if (accountDefinition == null) {
			throw new SchemaException("No account definition for intent="+intent+" in "+resource);
		}
		return accountDefinition.getIntent();
	}
	
	public static LensProjectionContext<ShadowType> getAccountContext(LensContext<UserType,ShadowType> context,
			PrismObject<ShadowType> equivalentAccount, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		return getAccountContext(context, ShadowUtil.getResourceOid(equivalentAccount.asObjectable()), 
				equivalentAccount.asObjectable().getIntent(), provisioningService, prismContext, result);
	}
	
	public static LensProjectionContext<ShadowType> getAccountContext(LensContext<UserType,ShadowType> context,
			String resourceOid, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountType);
		return context.findProjectionContext(rsd);
	}
	
	public static LensProjectionContext<ShadowType> getOrCreateAccountContext(LensContext<UserType,ShadowType> context,
			String resourceOid, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountType);
		return getOrCreateAccountContext(context, rsd);
	}
		
	public static LensProjectionContext<ShadowType> getOrCreateAccountContext(LensContext<UserType,ShadowType> context,
			ResourceShadowDiscriminator rsd) {
		LensProjectionContext<ShadowType> accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext == null) {
			accountSyncContext = context.createProjectionContext(rsd);
			ResourceType resource = context.getResource(rsd.getResourceOid());
			accountSyncContext.setResource(resource);
		}
		return accountSyncContext;
	}
	
	
	/**
	 * Consolidate the mappings of a single property to a delta. It takes the convenient structure of ItemValueWithOrigin triple.
	 * It produces the delta considering the mapping exclusion, authoritativeness and strength.
     *
     * filterExistingValues: if true, then values that already exist in the item are not added (and those that don't exist are not removed)
	 */
	public static <V extends PrismValue> ItemDelta<V> consolidateTripleToDelta(ItemPath itemPath, 
    		DeltaSetTriple<? extends ItemValueWithOrigin<V>> triple, ItemDefinition itemDefinition, 
    		ItemDelta<V> aprioriItemDelta, PrismContainer<?> itemContainer, ValueMatcher<?> valueMatcher,
    		boolean addUnchangedValues, boolean filterExistingValues, String contextDescription, boolean applyWeak) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
    	
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Consolidating {} triple:\n{}", itemPath, triple.dump());
		}
		
		ItemDelta<V> itemDelta = itemDefinition.createEmptyDelta(itemPath);
		
		Item<V> itemExisting = null;
		if (itemContainer != null) {
            itemExisting = itemContainer.findItem(itemPath);
		}
		
		// We will process each attribute individually.
        Collection<V> allValues = collectAllValues(triple);
        for (V value : allValues) {
        	
        	// Check what to do with the value using the usual "triple routine". It means that if a value is
        	// in zero set than we need no delta, plus set means add delta and minus set means delete delta.
        	// The first set that the value is present determines the result.
            Collection<ItemValueWithOrigin<V>> zeroPvwos =
                    collectPvwosFromSet(value, triple.getZeroSet());
            Collection<ItemValueWithOrigin<V>> plusPvwos =
                    collectPvwosFromSet(value, triple.getPlusSet());
            Collection<ItemValueWithOrigin<V>> minusPvwos =
                    collectPvwosFromSet(value, triple.getMinusSet());
            
            if (LOGGER.isTraceEnabled()) {
            	LOGGER.trace("PVWOs for value {}:\nzero = {}\nplus = {}\nminus = {}",
            			new Object[]{value, zeroPvwos, plusPvwos, minusPvwos});
            }
            
            boolean zeroHasStrong = false;
            if (!zeroPvwos.isEmpty()) {
            	for (ItemValueWithOrigin<?> pvwo : zeroPvwos) {
                    Mapping<?> mapping = pvwo.getMapping();
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	zeroHasStrong = true;
                    }
            	}
            }
            
            if (zeroHasStrong && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value, true)) {
            	throw new PolicyViolationException("Attempt to delete value "+value+" from item "+itemPath
            			+" but that value is mandated by a strong mapping (in "+contextDescription+")");
            }
            if (!zeroPvwos.isEmpty() && !addUnchangedValues) {
                // Value unchanged, nothing to do
                LOGGER.trace("Value {} unchanged, doing nothing", value);
                continue;
            }
            
            Mapping<?> exclusiveMapping = null;
            Collection<ItemValueWithOrigin<V>> pvwosToAdd = null;
            if (addUnchangedValues) {
                pvwosToAdd = MiscUtil.union(zeroPvwos, plusPvwos);
            } else {
                pvwosToAdd = plusPvwos;
            }

            if (!pvwosToAdd.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<?> pvwoToAdd : pvwosToAdd) {
                    Mapping<?> mapping = pvwoToAdd.getMapping();
                    if (mapping.getStrength() != MappingStrengthType.WEAK) {
                        weakOnly = false;
                    }
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	hasStrong = true;
                    }
                    if (mapping.isExclusive()) {
                        if (exclusiveMapping == null) {
                            exclusiveMapping = mapping;
                        } else {
                            String message = "Exclusion conflict in " + contextDescription + ", item " + itemPath +
                                    ", conflicting constructions: " + exclusiveMapping + " and " + mapping;
                            LOGGER.error(message);
                            throw new ExpressionEvaluationException(message);
                        }
                    }
                }
                if (weakOnly) {
                    // Postpone processing of weak values until we process all other values
                    LOGGER.trace("Value {} mapping is weak in item {}, postponing processing in {}",
                    		new Object[]{value, itemPath, contextDescription});
                    continue;
                }
                if (hasStrong && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value, true)) {
                	throw new PolicyViolationException("Attempt to delete value "+value+" from item "+itemPath
                			+" but that value is mandated by a strong mapping (in "+contextDescription+")");
                }
                if (!hasStrong && (aprioriItemDelta != null && !aprioriItemDelta.isEmpty())) {
                    // There is already a delta, skip this
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta that is more concrete, " +
                    		"skipping adding in {}", new Object[]{value, itemPath, contextDescription});
                    continue;
                }
                if (filterExistingValues && hasValue(itemExisting, value, valueMatcher)) {
                	LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
                			new Object[]{value, itemPath, contextDescription});
                	continue;
                }
                LOGGER.trace("Value {} added to delta for item {} in {}", new Object[]{value, itemPath, contextDescription});
                itemDelta.addValueToAdd((V)value.clone());
                continue;
            }

            // We need to check for empty plus set. Values that are both in plus and minus are considered to be added.
            // So check for that special case here to avoid removing them.
            if (!minusPvwos.isEmpty() && plusPvwos.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	boolean hasAuthoritative = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<?> pvwo : minusPvwos) {
                    Mapping<?> mapping = pvwo.getMapping();
                    if (mapping.getStrength() != MappingStrengthType.WEAK) {
                        weakOnly = false;
                    }
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	hasStrong = true;
                    }
                    if (mapping.isAuthoritative()) {
                        hasAuthoritative = true;
                    }
                }
                if (!hasAuthoritative) {
                	LOGGER.trace("Value {} has no authoritative mapping for item {}, skipping deletion in {}",
                			new Object[]{value, itemPath, contextDescription});
                	continue;
                }
                if (!hasStrong && (aprioriItemDelta != null && !aprioriItemDelta.isEmpty())) {
                    // There is already a delta, skip this
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta that is more concrete, skipping deletion in {}",
                    		new Object[]{value, itemPath, contextDescription});
                    continue;
                }
                if (weakOnly && (itemExisting != null && !itemExisting.isEmpty())) {
                    // There is already a value, skip this
                    LOGGER.trace("Value {} mapping is weak and the item {} already has a value, skipping deletion in {}",
                    		new Object[]{value, itemPath, contextDescription});
                    continue;
                }
                
                if (weakOnly && !applyWeak && (itemExisting == null || itemExisting.isEmpty())){
                	 // There is a weak mapping on a property, but we do not have full account available, so skipping deletion of the value is better way
                    LOGGER.trace("Value {} mapping is weak and the full account could not be fetched, skipping deletion in {}",
                    		new Object[]{value, itemPath, contextDescription});
                    continue;
                }
                if (filterExistingValues && !hasValue(itemExisting, value, valueMatcher)) {
                	LOGGER.trace("Value {} NOT deleted to delta for item {} the item does not have that value in {}",
                			new Object[]{value, itemPath, contextDescription});
                	continue;
                }
                LOGGER.trace("Value {} deleted to delta for item {} in {}",
                		new Object[]{ value, itemPath, contextDescription});
                itemDelta.addValueToDelete((V)value.clone());
            }
            
            if (!zeroPvwos.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	boolean hasAuthoritative = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<?> pvwo : zeroPvwos) {
                    Mapping<?> mapping = pvwo.getMapping();
                    if (mapping.getStrength() != MappingStrengthType.WEAK) {
                        weakOnly = false;
                    }
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	hasStrong = true;
                    }
                    if (mapping.isAuthoritative()) {
                        hasAuthoritative = true;
                    }
                }
            
	            if (aprioriItemDelta != null && aprioriItemDelta.isReplace()) {
	            	// Any strong mappings in the zero set needs to be re-applied as otherwise the replace will destroy it
	                if (hasStrong) {
	                	LOGGER.trace("Value {} added to delta for item {} in {} because there is strong mapping in the zero set", new Object[]{value, itemPath, contextDescription});
	                    itemDelta.addValueToAdd((V)value.clone());
	                    continue;
	                }
	            }
            }
        }
        
        Item<V> itemNew = null;
        if (itemContainer != null) {
        	itemNew = itemContainer.findItem(itemPath);
        }
		if (!hasValue(itemNew, itemDelta)) {
			// The application of computed delta results in no value, apply weak mappings
			Collection<? extends ItemValueWithOrigin<V>> nonNegativePvwos = triple.getNonNegativeValues();
			Collection<V> valuesToAdd = addWeakValues(nonNegativePvwos, OriginType.ASSIGNMENTS, applyWeak);
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = addWeakValues(nonNegativePvwos, OriginType.OUTBOUND, applyWeak);
			}
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = addWeakValues(nonNegativePvwos, null, applyWeak);
			}
			LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
					new Object[]{itemPath, contextDescription, valuesToAdd});
			itemDelta.addValuesToAdd(valuesToAdd);
		} else {
			LOGGER.trace("Existing values for item {} in {}, weak mapping processing skipped",
					new Object[]{itemPath, contextDescription});
		}
        
        return itemDelta;
        
    }
	
	private static <V extends PrismValue> boolean hasValue(Item<V> item, ItemDelta<V> itemDelta) throws SchemaException {
		if (item == null || item.isEmpty()) {
			if (itemDelta != null && itemDelta.addsAnyValue()) {
				return true;
			} else {
				return false;
			}
		} else {
			if (itemDelta == null || itemDelta.isEmpty()) {
				return true;
			} else {
				Item<V> clonedItem = item.clone();
				itemDelta.applyTo(clonedItem);
				return !clonedItem.isEmpty();
			}
		}
	}
	
	private static <V extends PrismValue> Collection<V> addWeakValues(Collection<? extends ItemValueWithOrigin<V>> pvwos, OriginType origin, boolean applyWeak) {
		Collection<V> values = new ArrayList<V>();
		for (ItemValueWithOrigin<V> pvwo: pvwos) {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.WEAK && applyWeak) {
				if (origin == null || origin == pvwo.getPropertyValue().getOriginType()) {
					values.add((V)pvwo.getPropertyValue().clone());
				}
			}
		}
		return values;
	}
	
	private static <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue, ValueMatcher<?> valueMatcher) {
		if (existingUserItem == null) {
			return false;
		}
		if (valueMatcher != null && newValue instanceof PrismPropertyValue) {
			return valueMatcher.hasRealValue((PrismProperty)existingUserItem, (PrismPropertyValue)newValue);
		} else {
			return existingUserItem.contains(newValue, true);
		}
	}
	
	private static <V extends PrismValue> Collection<V> collectAllValues(DeltaSetTriple<? extends ItemValueWithOrigin<V>> triple) {
        Collection<V> allValues = new HashSet<V>();
        collectAllValuesFromSet(allValues, triple.getZeroSet());
        collectAllValuesFromSet(allValues, triple.getPlusSet());
        collectAllValuesFromSet(allValues, triple.getMinusSet());
        return allValues;
    }

    private static <V extends PrismValue> void collectAllValuesFromSet(Collection<V> allValues,
            Collection<? extends ItemValueWithOrigin<V>> collection) {
        if (collection == null) {
            return;
        }
        for (ItemValueWithOrigin<V> pvwo : collection) {
        	V pval = pvwo.getPropertyValue();
        	if (!PrismValue.containsRealValue(allValues, pval)) {
        		allValues.add(pval);
        	}
        }
    }
    
    private static <V extends PrismValue> Collection<ItemValueWithOrigin<V>> collectPvwosFromSet(V pvalue,
            Collection<? extends ItemValueWithOrigin<V>> deltaSet) {
    	Collection<ItemValueWithOrigin<V>> pvwos = new ArrayList<ItemValueWithOrigin<V>>();
        for (ItemValueWithOrigin<V> setPvwo : deltaSet) {
        	if (setPvwo.equalsRealValue(pvalue)) {
        		pvwos.add(setPvwo);
        	}
        }
        return pvwos;
    }

    public static PropertyDelta<XMLGregorianCalendar> createActivationTimestampDelta(ActivationStatusType status, XMLGregorianCalendar now,
    		PrismContainerDefinition<ActivationType> activationDefinition, OriginType origin) {
    	QName timestampPropertyName;
		if (status == ActivationStatusType.ENABLED) {
			timestampPropertyName = ActivationType.F_ENABLE_TIMESTAMP;
		} else if (status == ActivationStatusType.DISABLED) {
			timestampPropertyName = ActivationType.F_DISABLE_TIMESTAMP;
		} else if (status == ActivationStatusType.ARCHIVED) {
			timestampPropertyName = ActivationType.F_ARCHIVE_TIMESTAMP;
		} else {
			throw new IllegalArgumentException("Unknown activation status "+status);
		}
		
		PrismPropertyDefinition<XMLGregorianCalendar> timestampDef = activationDefinition.findPropertyDefinition(timestampPropertyName);
		PropertyDelta<XMLGregorianCalendar> timestampDelta 
				= timestampDef.createEmptyDelta(new ItemPath(UserType.F_ACTIVATION, timestampPropertyName));
		timestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(now, origin, null));
		return timestampDelta;
    }

	public static void moveTriggers(LensProjectionContext<ShadowType> projCtx, LensFocusContext<UserType> focusCtx) throws SchemaException {
		ObjectDelta<ShadowType> projSecondaryDelta = projCtx.getSecondaryDelta();
		if (projSecondaryDelta == null) {
			return;
		}
		Collection<? extends ItemDelta> modifications = projSecondaryDelta.getModifications();
		Iterator<? extends ItemDelta> iterator = modifications.iterator();
		while (iterator.hasNext()) {
			ItemDelta projModification = iterator.next();
			LOGGER.trace("MOD: {}\n{}", projModification.getPath(), projModification.dump());
			if (projModification.getPath().equals(SchemaConstants.PATH_TRIGGER)) {
				focusCtx.swallowToProjectionWaveSecondaryDelta(projModification);
				iterator.remove();
			}
		}
	}
	
	public static <V extends PrismValue, F extends ObjectType, P extends ObjectType> void evaluateMapping(Mapping<V> mapping, LensContext<F, P> lensContext, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ModelExpressionThreadLocalHolder.setLensContext(lensContext);
		ModelExpressionThreadLocalHolder.setCurrentResult(parentResult);
		try {
			mapping.evaluate(parentResult);
		} finally {
			ModelExpressionThreadLocalHolder.resetLensContext();
			ModelExpressionThreadLocalHolder.resetCurrentResult();
		}
	}
	
	public static void loadFullAccount(LensProjectionContext<ShadowType> accCtx, ProvisioningService provisioningService,
			OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		if (accCtx.isFullShadow()) {
			// already loaded
			return;
		}
		if (accCtx.isAdd()) {
			// nothing to load yet
			return;
		}
		LOGGER.trace("Loading full account {} from provisioning", accCtx);
		
		try{
		PrismObject<ShadowType> objectOld = provisioningService.getObject(ShadowType.class,
				accCtx.getOid(), GetOperationOptions.createDoNotDiscovery(), null, result);
		// TODO: use setLoadedObject() instead?
		accCtx.setObjectCurrent(objectOld);
		ShadowType oldShadow = objectOld.asObjectable();
		accCtx.determineFullShadowFlag(oldShadow.getFetchResult());
		
		} catch (ObjectNotFoundException ex){
			if (accCtx.isDelete()){
				//this is OK, shadow was deleted, but we will continue in processing with old shadow..and set it as full so prevent from other full loading
				accCtx.setFullShadow(true);
			} else 
				throw ex;
		}
		
		accCtx.recompute();

		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded full account:\n{}", accCtx.dump());
		}
	}

	public static Object getIterationVariableValue(LensProjectionContext<ShadowType> accCtx) {
		Integer iterationOld = null;
		PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
		if (shadowCurrent != null) {
			iterationOld = shadowCurrent.asObjectable().getIteration();
		}
		if (iterationOld == null) {
			return accCtx.getIteration();
		}
		PrismPropertyDefinition<Integer> propDef = new PrismPropertyDefinition<Integer>(ExpressionConstants.VAR_ITERATION,
				ExpressionConstants.VAR_ITERATION, DOMUtil.XSD_INT, accCtx.getPrismContext());
		PrismProperty<Integer> propOld = propDef.instantiate();
		propOld.setRealValue(iterationOld);
		PropertyDelta<Integer> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION));
		propDelta.setValueToReplace(new PrismPropertyValue<Integer>(accCtx.getIteration()));
		PrismProperty<Integer> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIteration());
		ItemDeltaItem<PrismPropertyValue<Integer>> idi = new ItemDeltaItem<PrismPropertyValue<Integer>>(propOld, propDelta, propNew);
		return idi;
	}

	public static Object getIterationTokenVariableValue(LensProjectionContext<ShadowType> accCtx) {
		String iterationTokenOld = null;
		PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
		if (shadowCurrent != null) {
			iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
		}
		if (iterationTokenOld == null) {
			return accCtx.getIterationToken();
		}
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinition<String>(
				ExpressionConstants.VAR_ITERATION_TOKEN, ExpressionConstants.VAR_ITERATION_TOKEN,
				DOMUtil.XSD_STRING, accCtx.getPrismContext());
		PrismProperty<String> propOld = propDef.instantiate();
		propOld.setRealValue(iterationTokenOld);
		PropertyDelta<String> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION_TOKEN));
		propDelta.setValueToReplace(new PrismPropertyValue<String>(accCtx.getIterationToken()));
		PrismProperty<String> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIterationToken());
		ItemDeltaItem<PrismPropertyValue<String>> idi = new ItemDeltaItem<PrismPropertyValue<String>>(propOld, propDelta, propNew);
		return idi;
	}
    
}
