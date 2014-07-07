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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.CompiletimeConfig;
import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class LensUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);
	
	public static <F extends ObjectType, P extends ObjectType> void traceContext(Trace logger, String activity, String phase, 
			boolean important,  LensContext<F,P> context, boolean showTriples) throws SchemaException {
		// The provided information is useless, this is now a pointless noise now
//    	if (important && logger.isDebugEnabled()) {
//    		StringBuilder sb = new StringBuilder("Lens context ");
//    		sb.append(activity);
//    		sb.append(" after ");
//    		sb.append(phase);
//    		sb.append(":");
//    		boolean empty = true;
//    		for (ObjectDelta objectDelta: context.getAllChanges()) {
//    			if (objectDelta.isEmpty()) {
//    				continue;
//    			}
//    			sb.append("\n");
//    			sb.append(objectDelta.toString());
//    			empty = false;
//    		}
//    		if (empty) {
//    			sb.append(" no change");
//    		}
//    		logger.debug(sb.toString());
//    	}
        if (logger.isTraceEnabled()) {
        	logger.trace("Lens context:\n"+
            		"---[ {} CONTEXT after {} ]--------------------------------\n"+
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
			resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, result)
					.asObjectable();
			context.rememberResource(resourceType);
		}
		return resourceType;
	}
	
	public static String refineAccountType(String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		RefinedAccountDefinition accountDefinition = refinedSchema.getAccountDefinition(intent);
		if (accountDefinition == null) {
			throw new SchemaException("No account definition for intent="+intent+" in "+resource);
		}
		return accountDefinition.getAccountTypeName();
	}
	
	public static LensProjectionContext<AccountShadowType> getAccountContext(LensContext<UserType,AccountShadowType> context,
			PrismObject<AccountShadowType> equivalentAccount, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		return getAccountContext(context, ResourceObjectShadowUtil.getResourceOid(equivalentAccount.asObjectable()), 
				equivalentAccount.asObjectable().getIntent(), provisioningService, prismContext, result);
	}
	
	public static LensProjectionContext<AccountShadowType> getAccountContext(LensContext<UserType,AccountShadowType> context,
			String resourceOid, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountType);
		return context.findProjectionContext(rsd);
	}
	
	public static LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			String resourceOid, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountType);
		return getOrCreateAccountContext(context, rsd);
	}
		
	public static LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			ResourceShadowDiscriminator rsd) {
		LensProjectionContext<AccountShadowType> accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext == null) {
			accountSyncContext = context.createProjectionContext(rsd);
			ResourceType resource = context.getResource(rsd.getResourceOid());
			accountSyncContext.setResource(resource);
		}
		return accountSyncContext;
	}
	
	public static <F extends ObjectType, P extends ObjectType> LensContext<F, P> objectDeltasToContext(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ProvisioningService provisioningService, 
			PrismContext prismContext, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ObjectDelta<F> focusDelta = null;
		Collection<ObjectDelta<P>> projectionDeltas = new ArrayList<ObjectDelta<P>>(deltas.size());
		Class<F> focusClass = null;
		Class<P> projectionClass = null;
		// Sort deltas to focus and projection deltas, check if the classes are correct;
		for (ObjectDelta<? extends ObjectType> delta: deltas) {
			Class<? extends ObjectType> typeClass = delta.getObjectTypeClass();
			Validate.notNull(typeClass, "Object type class is null in "+delta);
			if (isFocalClass(typeClass)) {
				if (CompiletimeConfig.CONSISTENCY_CHECKS) {
					// Focus delta has to be complete with all the definition already in place
					delta.checkConsistence(false, true, true);
				}
				focusClass = (Class<F>) typeClass;
				projectionClass = checkProjectionClass(projectionClass, (Class<P>) getProjectionClass(focusClass));
				Validate.notNull(projectionClass, "No projection class for focus "+focusClass);
				if (!delta.isAdd() && delta.getOid() == null) {
					throw new IllegalArgumentException("Delta "+delta+" does not have an OID");
				}
				focusDelta = (ObjectDelta<F>) delta;
			} else {
				// This must be projection delta
				projectionClass = checkProjectionClass(projectionClass, (Class<P>) typeClass);
				projectionDeltas.add((ObjectDelta<P>) delta);
			}
		}
		
		if (focusClass == null) {
			focusClass = determineFocusClass(projectionClass);
		}
		LensContext<F, P> context = new LensContext<F, P>(focusClass, projectionClass, prismContext);
		context.setChannel(task.getChannel());
		if (focusDelta != null) {
			LensFocusContext<F> focusContext = context.createFocusContext();
			focusContext.setPrimaryDelta(focusDelta);
		}
		for (ObjectDelta<P> projectionDelta: projectionDeltas) {
			LensProjectionContext<P> projectionContext = context.createProjectionContext();
			projectionContext.setPrimaryDelta(projectionDelta);
			// We are little bit more liberal regarding projection deltas. 
			if (ResourceObjectShadowType.class.isAssignableFrom(projectionClass)) {
				// If the deltas represent shadows we tolerate missing attribute definitions.
				// We try to add the definitions by calling provisioning
				provisioningService.applyDefinition((ObjectDelta<? extends ResourceObjectShadowType>)projectionDelta, result);
			} else {
				// This check will fail giving a better information what's wrong then just throwing an exception here.
				projectionDelta.checkConsistence(false, true, true);
			}		
			if (projectionDelta instanceof ShadowDiscriminatorObjectDelta) {
				ShadowDiscriminatorObjectDelta<P> shadowDelta = (ShadowDiscriminatorObjectDelta<P>)projectionDelta;
				projectionContext.setResourceShadowDiscriminator(shadowDelta.getDiscriminator());
			} else {
				if (!projectionDelta.isAdd() && projectionDelta.getOid() == null) {
					throw new IllegalArgumentException("Delta "+projectionDelta+" does not have an OID");
				}
			}
		}

		// This forces context reload before the next projection
		context.rot();
		
		if (CompiletimeConfig.CONSISTENCY_CHECKS) context.checkConsistence();
		return context;
	}
	
	public static <F extends ObjectType, P extends ObjectType> Class<F> determineFocusClass(Class<P> projectionClass) {
		if (projectionClass == AccountShadowType.class) {
			return (Class<F>) UserType.class;
		}
		return null;
	}

	private static <T extends ObjectType> Class<T> checkProjectionClass(Class<T> oldProjectionClass, Class<T> newProjectionClass) {
		if (oldProjectionClass == null) {
			return newProjectionClass;
		} else {
			if (oldProjectionClass != oldProjectionClass) {
				throw new IllegalArgumentException("Mixed projection classes in the deltas, got both "+oldProjectionClass+" and "+oldProjectionClass);
			}
			return oldProjectionClass;
		}
	}

	public static <T extends ObjectType> boolean isFocalClass(Class<T> clazz) {
		// TODO!!!!!!!!!!!!
		if (UserType.class.isAssignableFrom(clazz)) {
			return true;
		}
		return false;
	}
	
	public static <F extends ObjectType, P extends ObjectType> Class<P> getProjectionClass(Class<F> focusClass) {
		// TODO!!!!!!!!!!!!
		if (UserType.class.isAssignableFrom(focusClass)) {
			return (Class<P>) AccountShadowType.class;
		}
		return null;
	}
	
	/**
	 * Consolidate the mappings of a single property to a delta. It takes the convenient structure of ItemValueWithOrigin triple.
	 * It produces the delta considering the mapping exclusion, authoritativeness and strength. 
	 */
	public static <V extends PrismValue> ItemDelta<V> consolidateTripleToDelta(ItemPath itemPath, 
    		DeltaSetTriple<? extends ItemValueWithOrigin<V>> triple, ItemDefinition itemDefinition, 
    		ItemDelta<V> aprioriItemDelta, PrismContainer<?> itemContainer,
    		boolean addUnchangedValues, boolean filterExistingValues, String contextDescription) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
    	
		ItemDelta<V> itemDelta = itemDefinition.createEmptyDelta(itemPath);
		
		Item<V> itemExisting = null;
		if (itemContainer != null) {
            itemExisting = itemContainer.findItem(itemPath);
		}
		
		// We will process each attribute individually.
        Collection<V> allValues = collectAllValues(triple);
        for (V value : allValues) {
        	
        	// Check what to do with the value using the usual "triple routine". It means that if a value is
        	// in zero set than we need no delta, plus set means add delta and minus set means delte delta.
        	// The first set that the value is present determines the result.
            Collection<ItemValueWithOrigin<V>> zeroPvwos =
                    collectPvwosFromSet(value, triple.getZeroSet());
            Collection<ItemValueWithOrigin<V>> plusPvwos =
                    collectPvwosFromSet(value, triple.getPlusSet());
            Collection<ItemValueWithOrigin<V>> minusPvwos =
                    collectPvwosFromSet(value, triple.getMinusSet());
            
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
            if (!plusPvwos.isEmpty() && !minusPvwos.isEmpty()) {
                // Value added and removed. Ergo no change.
                LOGGER.trace("Value {} added and removed, doing nothing", value);
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
                if (filterExistingValues && hasValue(itemExisting, value)) {
                	LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
                			new Object[]{value, itemPath, contextDescription});
                	continue;
                }
                LOGGER.trace("Value {} added to delta for item {} in {}", new Object[]{value, itemPath, contextDescription});
                itemDelta.addValueToAdd((V)value.clone());
                continue;
            }

            if (!minusPvwos.isEmpty()) {
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
                if (filterExistingValues && !hasValue(itemExisting, value)) {
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
			Collection<V> valuesToAdd = addWeakValues(nonNegativePvwos, OriginType.ASSIGNMENTS);
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = addWeakValues(nonNegativePvwos, OriginType.OUTBOUND);
			}
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = addWeakValues(nonNegativePvwos, null);
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
	
	private static <V extends PrismValue> Collection<V> addWeakValues(Collection<? extends ItemValueWithOrigin<V>> pvwos, OriginType origin) {
		Collection<V> values = new ArrayList<V>();
		for (ItemValueWithOrigin<V> pvwo: pvwos) {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.WEAK) {
				if (origin == null || origin == pvwo.getPropertyValue().getOriginType()) {
					values.add((V)pvwo.getPropertyValue().clone());
				}
			}
		}
		return values;
	}
	
	private static <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue) {
		if (existingUserItem == null) {
			return false;
		}
		return existingUserItem.contains(newValue, true);
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

}
