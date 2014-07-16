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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.mutable.MutableBoolean;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.Source;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class LensUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(LensUtil.class);
	
	public static <F extends ObjectType> void traceContext(Trace logger, String activity, String phase, 
			boolean important,  LensContext<F> context, boolean showTriples) throws SchemaException {
        if (logger.isTraceEnabled()) {
        	logger.trace("Lens context:\n"+
            		"---[ {} context {} ]--------------------------------\n"+
            		"{}\n",
            		new Object[]{activity, phase, context.dump(showTriples)});
        }
    }
	
	public static <F extends ObjectType> ResourceType getResource(LensContext<F> context,
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
	
	public static String refineProjectionIntent(ShadowKindType kind, String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
		RefinedObjectClassDefinition rObjClassDef = refinedSchema.getRefinedDefinition(kind, intent);
		if (rObjClassDef == null) {
			throw new SchemaException("No projection definition for kind="+kind+" intent="+intent+" in "+resource);
		}
		return rObjClassDef.getIntent();
	}
	
	public static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context,
			PrismObject<ShadowType> equivalentAccount, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ShadowType equivalentAccountType = equivalentAccount.asObjectable();
		ShadowKindType kind = ShadowUtil.getKind(equivalentAccountType);
		return getProjectionContext(context, ShadowUtil.getResourceOid(equivalentAccountType), 
				kind, equivalentAccountType.getIntent(), provisioningService, 
				prismContext, result);
	}
	
	public static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context,
			String resourceOid, ShadowKindType kind, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String refinedIntent = refineProjectionIntent(kind, intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, refinedIntent);
		return context.findProjectionContext(rsd);
	}
	
	public static <F extends FocusType> LensProjectionContext getOrCreateProjectionContext(LensContext<F> context,
			String resourceOid, ShadowKindType kind, String intent, ProvisioningService provisioningService, PrismContext prismContext, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resource = getResource(context, resourceOid, provisioningService, result);
		String accountType = refineProjectionIntent(kind, intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, accountType);
		return getOrCreateProjectionContext(context, rsd);
	}
		
	public static <F extends ObjectType> LensProjectionContext getOrCreateProjectionContext(LensContext<F> context,
			ResourceShadowDiscriminator rsd) {
		LensProjectionContext accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext == null) {
			accountSyncContext = context.createProjectionContext(rsd);
			ResourceType resource = context.getResource(rsd.getResourceOid());
			accountSyncContext.setResource(resource);
		}
		return accountSyncContext;
	}
	
	public static <F extends ObjectType> LensProjectionContext createAccountContext(LensContext<F> context, ResourceShadowDiscriminator rsd){
		return new LensProjectionContext(context, rsd);
	}
	
	
	/**
	 * Consolidate the mappings of a single item to a delta. It takes the convenient structure of ItemValueWithOrigin triple.
	 * It produces the delta considering the mapping exclusion, authoritativeness and strength.
     *
     * filterExistingValues: if true, then values that already exist in the item are not added (and those that don't exist are not removed)
	 */
	public static <V extends PrismValue, I extends ItemValueWithOrigin<V>> ItemDelta<V> consolidateTripleToDelta(ItemPath itemPath, 
    		DeltaSetTriple<I> triple, ItemDefinition itemDefinition, 
    		ItemDelta<V> aprioriItemDelta, PrismContainer<?> itemContainer, ValueMatcher<?> valueMatcher, Comparator<V> comparator,
    		boolean addUnchangedValues, boolean filterExistingValues, boolean isExclusiveStrong, 
    		String contextDescription, boolean applyWeak) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
    			
		ItemDelta<V> itemDelta = itemDefinition.createEmptyDelta(itemPath);
		
		Item<V> itemExisting = null;
		if (itemContainer != null) {
            itemExisting = itemContainer.findItem(itemPath);
		}
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Consolidating {} triple:\n{}\nApriori Delta:\n{}\nExisting item:\n{}", 
					new Object[]{
						itemPath, triple.debugDump(), 
						aprioriItemDelta==null?"null":aprioriItemDelta.debugDump(),
						itemExisting==null?"null":itemExisting.debugDump(),
					});
		}
		
        Collection<V> allValues = collectAllValues(triple);
        
        final MutableBoolean itemHasStrongMutable = new MutableBoolean(false);
        SimpleVisitor<I> visitor = new SimpleVisitor<I>() {
			@Override
			public void visit(I pvwo) {
				if (pvwo.getMapping().getStrength() == MappingStrengthType.STRONG) {
					itemHasStrongMutable.setValue(true);
				}
			}
		};
		triple.accept(visitor);
        boolean ignoreNormalMappings = itemHasStrongMutable.booleanValue() && isExclusiveStrong;
        
        // We will process each value individually. I really mean each value. This whole method deals with
        // a single item (e.g. attribute). But this loop iterates over every potential value of that item.
        for (V value : allValues) {
        	
        	LOGGER.trace("item existing: {}, value: {}", itemExisting, value);
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
                if (!hasStrong && ignoreNormalMappings) {
                	LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
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
                if (filterExistingValues && hasValue(itemExisting, value, valueMatcher, comparator)) {
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
                if (filterExistingValues && !hasValue(itemExisting, value, valueMatcher, comparator)) {
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
	
	private static <V extends PrismValue> boolean hasValue(Item<V> existingUserItem, V newValue, ValueMatcher<?> valueMatcher, Comparator<V> comparator) {
		if (existingUserItem == null) {
			return false;
		}
		if (valueMatcher != null && newValue instanceof PrismPropertyValue) {
			return valueMatcher.hasRealValue((PrismProperty)existingUserItem, (PrismPropertyValue)newValue);
		} else {
			return existingUserItem.contains(newValue, true, comparator);
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
				= timestampDef.createEmptyDelta(new ItemPath(FocusType.F_ACTIVATION, timestampPropertyName));
		timestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(now, origin, null));
		return timestampDelta;
    }

	public static <F extends ObjectType> void moveTriggers(LensProjectionContext projCtx, LensFocusContext<F> focusCtx) throws SchemaException {
		ObjectDelta<ShadowType> projSecondaryDelta = projCtx.getSecondaryDelta();
		if (projSecondaryDelta == null) {
			return;
		}
		Collection<? extends ItemDelta> modifications = projSecondaryDelta.getModifications();
		Iterator<? extends ItemDelta> iterator = modifications.iterator();
		while (iterator.hasNext()) {
			ItemDelta projModification = iterator.next();
			LOGGER.trace("MOD: {}\n{}", projModification.getPath(), projModification.debugDump());
			if (projModification.getPath().equivalent(SchemaConstants.PATH_TRIGGER)) {
				focusCtx.swallowToProjectionWaveSecondaryDelta(projModification);
				iterator.remove();
			}
		}
	}
	
	public static <V extends PrismValue, F extends ObjectType> void evaluateMapping(
			Mapping<V> mapping, LensContext<F> lensContext, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ModelExpressionThreadLocalHolder.pushLensContext(lensContext);
		ModelExpressionThreadLocalHolder.pushCurrentResult(parentResult);
		ModelExpressionThreadLocalHolder.pushCurrentTask(task);
		try {
			mapping.evaluate(task, parentResult);
		} finally {
			ModelExpressionThreadLocalHolder.popLensContext();
			ModelExpressionThreadLocalHolder.popCurrentResult();
			ModelExpressionThreadLocalHolder.popCurrentTask();
			if (lensContext.getDebugListener() != null) {
				lensContext.getDebugListener().afterMappingEvaluation(lensContext, mapping);
			}
		}
	}

    public static <V extends PrismValue, F extends ObjectType> void evaluateScript(
            ScriptExpression scriptExpression, LensContext<F> lensContext, ExpressionVariables variables, String shortDesc, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        ModelExpressionThreadLocalHolder.pushLensContext(lensContext);
        ModelExpressionThreadLocalHolder.pushCurrentResult(parentResult);
        ModelExpressionThreadLocalHolder.pushCurrentTask(task);
        try {
            scriptExpression.evaluate(variables, ScriptExpressionReturnTypeType.SCALAR, false, shortDesc, parentResult);
        } finally {
            ModelExpressionThreadLocalHolder.popLensContext();
            ModelExpressionThreadLocalHolder.popCurrentResult();
            ModelExpressionThreadLocalHolder.popCurrentTask();
//			if (lensContext.getDebugListener() != null) {
//				lensContext.getDebugListener().afterScriptEvaluation(lensContext, scriptExpression);
//			}
        }
    }

	public static <F extends ObjectType> void loadFullAccount(LensContext<F> context, LensProjectionContext accCtx, ProvisioningService provisioningService,
			OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		if (accCtx.isFullShadow()) {
			// already loaded
			return;
		}
		if (accCtx.isAdd()) {
			// nothing to load yet
			return;
		}
		ResourceShadowDiscriminator discr = accCtx.getResourceShadowDiscriminator();
		if (discr != null && discr.getOrder() > 0) {
			// It may be just too early to load the projection
			if (LensUtil.hasLowerOrderContext(context, accCtx) && (context.getExecutionWave() < accCtx.getWave())) {
				// We cannot reliably load the context now
				return;
			}
		}
		LOGGER.trace("Loading full account {} from provisioning", accCtx);
		
		try{
			PrismObject<ShadowType> objectOld = provisioningService.getObject(ShadowType.class,
					accCtx.getOid(), SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()),
					null, result);
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
			LOGGER.trace("Loaded full account:\n{}", accCtx.debugDump());
		}
	}

	public static Object getIterationVariableValue(LensProjectionContext accCtx) {
		Integer iterationOld = null;
		PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
		if (shadowCurrent != null) {
			iterationOld = shadowCurrent.asObjectable().getIteration();
		}
		if (iterationOld == null) {
			return accCtx.getIteration();
		}
		PrismPropertyDefinition<Integer> propDef = new PrismPropertyDefinition<Integer>(ExpressionConstants.VAR_ITERATION,
				DOMUtil.XSD_INT, accCtx.getPrismContext());
		PrismProperty<Integer> propOld = propDef.instantiate();
		propOld.setRealValue(iterationOld);
		PropertyDelta<Integer> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION));
		propDelta.setValueToReplace(new PrismPropertyValue<Integer>(accCtx.getIteration()));
		PrismProperty<Integer> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIteration());
		ItemDeltaItem<PrismPropertyValue<Integer>> idi = new ItemDeltaItem<PrismPropertyValue<Integer>>(propOld, propDelta, propNew);
		return idi;
	}

	public static Object getIterationTokenVariableValue(LensProjectionContext accCtx) {
		String iterationTokenOld = null;
		PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
		if (shadowCurrent != null) {
			iterationTokenOld = shadowCurrent.asObjectable().getIterationToken();
		}
		if (iterationTokenOld == null) {
			return accCtx.getIterationToken();
		}
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinition<String>(
				ExpressionConstants.VAR_ITERATION_TOKEN, DOMUtil.XSD_STRING, accCtx.getPrismContext());
		PrismProperty<String> propOld = propDef.instantiate();
		propOld.setRealValue(iterationTokenOld);
		PropertyDelta<String> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION_TOKEN));
		propDelta.setValueToReplace(new PrismPropertyValue<String>(accCtx.getIterationToken()));
		PrismProperty<String> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIterationToken());
		ItemDeltaItem<PrismPropertyValue<String>> idi = new ItemDeltaItem<PrismPropertyValue<String>>(propOld, propDelta, propNew);
		return idi;
	}
	
	/**
	 * Extracts the delta from this projection context and also from all other projection contexts that have 
	 * equivalent discriminator.
	 */
	public static <F extends ObjectType, T> PropertyDelta<T> findAPrioriDelta(LensContext<F> context,
			LensProjectionContext projCtx, ItemPath projectionPropertyPath) throws SchemaException {
		PropertyDelta<T> aPrioriDelta = null;
		for (LensProjectionContext aProjCtx: findRelatedContexts(context, projCtx)) {
			ObjectDelta<ShadowType> aProjDelta = aProjCtx.getDelta();
			if (aProjDelta != null) {
				PropertyDelta<T> aPropProjDelta = aProjDelta.findPropertyDelta(projectionPropertyPath);
				if (aPropProjDelta != null) {
					if (aPrioriDelta == null) {
						aPrioriDelta = aPropProjDelta.clone();
					} else {
						aPrioriDelta.merge(aPropProjDelta);
					}
				}
			}
		}
		return aPrioriDelta;
	}
	
	/**
	 * Extracts the delta from this projection context and also from all other projection contexts that have 
	 * equivalent discriminator.
	 */
	public static <F extends ObjectType, T> ObjectDelta<ShadowType> findAPrioriDelta(LensContext<F> context,
			LensProjectionContext projCtx) throws SchemaException {
		ObjectDelta<ShadowType> aPrioriDelta = null;
		for (LensProjectionContext aProjCtx: findRelatedContexts(context, projCtx)) {
			ObjectDelta<ShadowType> aProjDelta = aProjCtx.getDelta();
			if (aProjDelta != null) {
				if (aPrioriDelta == null) {
					aPrioriDelta = aProjDelta.clone();
				} else {
					aPrioriDelta.merge(aProjDelta);
				}
			}
		}
		return aPrioriDelta;
	}
	
	/**
	 * Returns a list of context that have equivalent discriminator with the reference context. Ordered by "order" in the
	 * discriminator.
	 */
	public static <F extends ObjectType> List<LensProjectionContext> findRelatedContexts(
			LensContext<F> context, LensProjectionContext refProjCtx) {
		List<LensProjectionContext> projCtxs = new ArrayList<LensProjectionContext>();
		ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
		if (refDiscr == null) {
			return projCtxs;
		}
		for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
			ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
			if (refDiscr.equivalent(aDiscr)) {
				projCtxs.add(aProjCtx);
			}
		}
		Comparator<? super LensProjectionContext> orderComparator = new Comparator<LensProjectionContext>() {
			@Override
			public int compare(LensProjectionContext ctx1, LensProjectionContext ctx2) {
				int order1 = ctx1.getResourceShadowDiscriminator().getOrder();
				int order2 = ctx2.getResourceShadowDiscriminator().getOrder();
				return Integer.compare(order1, order2);
			}
		};
		Collections.sort(projCtxs, orderComparator);
		return projCtxs;
	}

	public static <F extends ObjectType> boolean hasLowerOrderContext(LensContext<F> context,
			LensProjectionContext refProjCtx) {
		ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
		for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
			ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
			if (refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() > aDiscr.getOrder())) {
				return true;
			}
		}
		return false;
	}
	
	public static <F extends ObjectType> LensProjectionContext findLowerOrderContext(LensContext<F> context,
			LensProjectionContext refProjCtx) {
		int minOrder = -1;
		LensProjectionContext foundCtx = null;
		ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
		for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
			ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
			if (refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() > aDiscr.getOrder())) {
				if (minOrder < 0 || (aDiscr.getOrder() < minOrder)) {
					minOrder = aDiscr.getOrder();
					foundCtx = aProjCtx;
				}
			}
		}
		return foundCtx;
	}
	
	public static <T extends ObjectType, F extends ObjectType> void setContextOid(LensContext<F> context,
			LensElementContext<T> objectContext, String oid) {
		objectContext.setOid(oid);
		// Check if we need to propagate this oid also to higher-order contexts
		if (!(objectContext instanceof LensProjectionContext)) {
			return;
		}
		LensProjectionContext refProjCtx = (LensProjectionContext)objectContext;
		ResourceShadowDiscriminator refDiscr = refProjCtx.getResourceShadowDiscriminator();
		if (refDiscr == null) {
			return;
		}
		for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
			ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
			if (aDiscr != null && refDiscr.equivalent(aDiscr) && (refDiscr.getOrder() < aDiscr.getOrder())) {
				aProjCtx.setOid(oid);
			}
		}
	}

	public static PrismObject<SystemConfigurationType> getSystemConfiguration(LensContext context, RepositoryService repositoryService, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
		if (systemConfiguration == null) {
			systemConfiguration = Utils.getSystemConfiguration(repositoryService, result);
			context.setSystemConfiguration(systemConfiguration);
		}
		return systemConfiguration;
	}

    public static <F extends FocusType> PrismObjectDefinition<F> getFocusDefinition(LensContext<F> context) {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return null;
        }
        Class<F> typeClass = focusContext.getObjectTypeClass();
        return context.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(typeClass);
    }
    
    public static int determineMaxIterations(IterationSpecificationType iterationSpecType) {
    	if (iterationSpecType != null) {
			return iterationSpecType.getMaxIterations();
		} else {
			return 0;
		}
    }
    
    public static <F extends ObjectType> String formatIterationToken(LensContext<F> context, 
			LensElementContext<?> accountContext, IterationSpecificationType iterationType, 
			int iteration, ExpressionFactory expressionFactory, ExpressionVariables variables, 
			Task task, OperationResult result) 
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		if (iterationType == null) {
			return formatIterationTokenDefault(iteration);
		}
		ExpressionType tokenExpressionType = iterationType.getTokenExpression();
		if (tokenExpressionType == null) {
			return formatIterationTokenDefault(iteration);
		}
		PrismPropertyDefinition<String> outputDefinition = new PrismPropertyDefinition<String>(ExpressionConstants.VAR_ITERATION_TOKEN,
				DOMUtil.XSD_STRING, context.getPrismContext());
		Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(tokenExpressionType, outputDefinition , "iteration token expression in "+accountContext.getHumanReadableName(), result);
		
		Collection<Source<?>> sources = new ArrayList<Source<?>>();
		PrismPropertyDefinition<Integer> inputDefinition = new PrismPropertyDefinition<Integer>(ExpressionConstants.VAR_ITERATION,
				DOMUtil.XSD_INT, context.getPrismContext());
		inputDefinition.setMaxOccurs(1);
		PrismProperty<Integer> input = inputDefinition.instantiate();
		input.add(new PrismPropertyValue<Integer>(iteration));
		ItemDeltaItem<PrismPropertyValue<Integer>> idi = new ItemDeltaItem<PrismPropertyValue<Integer>>(input);
		Source<PrismPropertyValue<Integer>> iterationSource = new Source<PrismPropertyValue<Integer>>(idi, ExpressionConstants.VAR_ITERATION);
		sources.add(iterationSource);
		
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources , variables, 
				"iteration token expression in "+accountContext.getHumanReadableName(), task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(expressionContext);
		Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
		if (outputValues.isEmpty()) {
			return "";
		}
		if (outputValues.size() > 1) {
			throw new ExpressionEvaluationException("Iteration token expression in "+accountContext.getHumanReadableName()+" returned more than one value ("+outputValues.size()+" values)");
		}
		String realValue = outputValues.iterator().next().getValue();
		if (realValue == null) {
			return "";
		}
		return realValue;
	}
    
    public static String formatIterationTokenDefault(int iteration) {
		if (iteration == 0) {
			return "";
		}
		return Integer.toString(iteration);
	}
    
    public static <F extends ObjectType> boolean evaluateIterationCondition(LensContext<F> context, 
    		LensElementContext<?> accountContext, IterationSpecificationType iterationType, 
    		int iteration, String iterationToken, boolean beforeIteration, 
			ExpressionFactory expressionFactory, ExpressionVariables variables, Task task, OperationResult result) 
					throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException {
		if (iterationType == null) {
			return true;
		}
		ExpressionType expressionType;
		String desc;
		if (beforeIteration) {
			expressionType = iterationType.getPreIterationCondition();
			desc = "pre-iteration expression in "+accountContext.getHumanReadableName();
		} else {
			expressionType = iterationType.getPostIterationCondition();
			desc = "post-iteration expression in "+accountContext.getHumanReadableName();
		}
		if (expressionType == null) {
			return true;
		}
		PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinition<Boolean>(ExpressionConstants.OUTPUT_ELMENT_NAME,
				DOMUtil.XSD_BOOLEAN, context.getPrismContext());
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, outputDefinition , desc, result);
		
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, iteration);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);
		
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(expressionContext);
		Collection<PrismPropertyValue<Boolean>> outputValues = outputTriple.getNonNegativeValues();
		if (outputValues.isEmpty()) {
			return false;
		}
		if (outputValues.size() > 1) {
			throw new ExpressionEvaluationException(desc+" returned more than one value ("+outputValues.size()+" values)");
		}
		Boolean realValue = outputValues.iterator().next().getValue();
		if (realValue == null) {
			return false;
		}
		return realValue;

	}
    
    public static boolean isValid(AssignmentType assignmentType, XMLGregorianCalendar now, ActivationComputer activationComputer) {
		ActivationType activationType = assignmentType.getActivation();
		if (activationType == null) {
			return true;
		}
		TimeIntervalStatusType validityStatus = activationComputer.getValidityStatus(activationType, now);
		ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(activationType, validityStatus, ActivationStatusType.ENABLED);
		return effectiveStatus == ActivationStatusType.ENABLED;
	}
    
    public static <V extends PrismValue, F extends FocusType> Mapping<V> createFocusMapping(final MappingFactory mappingFactory,
    		final LensContext<F> context, final MappingType mappingType, ObjectType originObject, 
			ObjectDeltaObject<F> focusOdo, AssignmentPathVariables assignmentPathVariables, PrismObject<SystemConfigurationType> configuration,
			XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
    	Integer iteration = null;
    	String iterationToken = null;
    	if (focusOdo.getNewObject() != null) {
    		F focusNewType = focusOdo.getNewObject().asObjectable();
    		iteration = focusNewType.getIteration();
    		iterationToken = focusNewType.getIterationToken();
    	} else if (focusOdo.getOldObject() != null) {
    		F focusOldType = focusOdo.getOldObject().asObjectable();
    		iteration = focusOldType.getIteration();
    		iterationToken = focusOldType.getIterationToken();
    	}
    	return createFocusMapping(mappingFactory, context, mappingType, originObject, focusOdo, assignmentPathVariables,
    			iteration, iterationToken, configuration, now, contextDesc, result);
    }
    
    public static <V extends PrismValue, F extends FocusType> Mapping<V> createFocusMapping(final MappingFactory mappingFactory,
    		final LensContext<F> context, final MappingType mappingType, ObjectType originObject, 
			ObjectDeltaObject<F> focusOdo, AssignmentPathVariables assignmentPathVariables, 
			Integer iteration, String iterationToken, PrismObject<SystemConfigurationType> configuration,
			XMLGregorianCalendar now, String contextDesc, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Mapping<V> mapping = mappingFactory.createMapping(mappingType, contextDesc);
		
		if (!mapping.isApplicableToChannel(context.getChannel())) {
			return null;
		}
		
		mapping.setSourceContext(focusOdo);
		mapping.setTargetContext(context.getFocusContext().getObjectDefinition());
		mapping.setRootNode(focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION, iteration);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);
		mapping.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, configuration);
		addAssignmentPathVariables(mapping, assignmentPathVariables);
		mapping.setOriginType(OriginType.USER_POLICY);
		mapping.setOriginObject(originObject);
		mapping.setNow(now);

		ItemPath itemPath = mapping.getOutputPath();
		
		PrismObject<F> focusNew = focusOdo.getNewObject();
		if (focusNew != null) {
			Item<V> existingUserItem = (Item<V>) focusNew.findItem(itemPath);
			if (existingUserItem != null && !existingUserItem.isEmpty() 
					&& mapping.getStrength() == MappingStrengthType.WEAK) {
				// This valueConstruction only applies if the property does not have a value yet.
				// ... but it does
				return null;
			}
		}

		StringPolicyResolver stringPolicyResolver = new StringPolicyResolver() {
			private ItemPath outputPath;
			private ItemDefinition outputDefinition;
			@Override
			public void setOutputPath(ItemPath outputPath) {
				this.outputPath = outputPath;
			}
			
			@Override
			public void setOutputDefinition(ItemDefinition outputDefinition) {
				this.outputDefinition = outputDefinition;
			}
			
			@Override
			public StringPolicyType resolve() {
				if (outputDefinition.getName().equals(PasswordType.F_VALUE)) {
					ValuePolicyType passwordPolicy = context.getEffectivePasswordPolicy();
					if (passwordPolicy == null) {
						return null;
					}
					return passwordPolicy.getStringPolicy();
				}
				if (mappingType.getExpression() != null){
					List<JAXBElement<?>> evaluators = mappingType.getExpression().getExpressionEvaluator();
					if (evaluators != null){
						for (JAXBElement jaxbEvaluator : evaluators){
							Object object = jaxbEvaluator.getValue();
							if (object != null && object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null){
								ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
								try{
								ValuePolicyType valuePolicyType = mappingFactory.getObjectResolver().resolve(ref, ValuePolicyType.class, 
										null, "resolving value policy for generate attribute "+ outputDefinition.getName()+" value", new OperationResult("Resolving value policy"));
								if (valuePolicyType != null){
									return valuePolicyType.getStringPolicy();
								}
								} catch (Exception ex){
									throw new SystemException(ex.getMessage(), ex);
								}
							}
						}
						
					}
				}
				return null;
				
			}
		};
		mapping.setStringPolicyResolver(stringPolicyResolver);

		return mapping;
	}
    
    public static AssignmentPathVariables computeAssignmentPathVariables(AssignmentPath assignmentPath) throws SchemaException {
    	if (assignmentPath == null || assignmentPath.isEmpty()) {
    		return null;
    	}
    	AssignmentPathVariables vars = new AssignmentPathVariables();
    	Iterator<AssignmentPathSegment> iterator = assignmentPath.getSegments().iterator();
		while (iterator.hasNext()) {
			AssignmentPathSegment segment = iterator.next();
			AssignmentType segmentAssignmentType = segment.getAssignmentType();
			PrismContainerValue<AssignmentType> segmentAssignmentCVal = segmentAssignmentType.asPrismContainerValue();
			PrismContainerDefinition<AssignmentType> assignmentDef = segmentAssignmentCVal.getParent().getDefinition();
			
			// Magic assignment
			if (vars.getMagicAssignment() == null) {
				PrismContainerValue<AssignmentType> magicAssignment = segmentAssignmentCVal.clone();
				// Make sure that the magic assignment has a valid parent so it can be serialized
				PrismContainer<AssignmentType> assignmentCont = assignmentDef.instantiate();
				assignmentCont.add(magicAssignment);
				vars.setMagicAssignment(magicAssignment);
			} else {
				// Collect extension values from the assignment extension
				PrismContainer<Containerable> magicExtension = vars.getMagicAssignment().findOrCreateContainer(AssignmentType.F_EXTENSION);
				mergeExtension(magicExtension, segmentAssignmentCVal.findContainer(AssignmentType.F_EXTENSION));
			}
			
			// Collect extension values from the source object extension
			PrismContainer<Containerable> magicExtension = vars.getMagicAssignment().findOrCreateContainer(AssignmentType.F_EXTENSION);
			ObjectType segmentSource = segment.getSource();
			if (segmentSource != null) {
				mergeExtension(magicExtension, segmentSource.asPrismObject().findContainer(AssignmentType.F_EXTENSION));
			}
			
			// immediate assignment (use assignment from previous iteration)
			vars.setImmediateAssignment(vars.getThisAssignment());
			
			// this assignment
			PrismContainerValue<AssignmentType> thisAssignment = segmentAssignmentCVal.clone();
			// Make sure that the assignment has a valid parent so it can be serialized
			PrismContainer<AssignmentType> assignmentCont = assignmentDef.instantiate();
			assignmentCont.add(thisAssignment);
			vars.setThisAssignment(thisAssignment);
			
			if (iterator.hasNext() && segmentSource instanceof AbstractRoleType) {
				vars.setImmediateRole(segmentSource.asPrismObject());
			}
		}
		
		AssignmentType focusAssignment = assignmentPath.getFirstAssignment();
		vars.setFocusAssignment(focusAssignment.asPrismContainerValue());
		
		return vars;
    }
    
    private static void mergeExtension(PrismContainer<Containerable> magicExtension, PrismContainer<Containerable> segmentExtension) throws SchemaException {
		if (segmentExtension != null && !segmentExtension.getValue().isEmpty()) {
			for (Item<?> segmentItem: segmentExtension.getValue().getItems()) {
				Item<?> magicItem = magicExtension.findItem(segmentItem.getElementName());
				if (magicItem == null) {
					magicExtension.add(segmentItem.clone());
				}
			}
		}
	}
    
    public static <V extends PrismValue> void addAssignmentPathVariables(Mapping<V> mapping, AssignmentPathVariables assignmentPathVariables) {
    	if (assignmentPathVariables != null ) {
			mapping.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, assignmentPathVariables.getMagicAssignment());
			mapping.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ASSIGNMENT, assignmentPathVariables.getImmediateAssignment());
			mapping.addVariableDefinition(ExpressionConstants.VAR_THIS_ASSIGNMENT, assignmentPathVariables.getThisAssignment());
			mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS_ASSIGNMENT, assignmentPathVariables.getFocusAssignment());
			mapping.addVariableDefinition(ExpressionConstants.VAR_IMMEDIATE_ROLE, assignmentPathVariables.getImmediateRole());
		}
    }
    
    public static <F extends ObjectType> void checkContextSanity(LensContext<F> context, String activityDescription, 
			OperationResult result) throws SchemaException, PolicyViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext != null) {
			PrismObject<F> focusObjectNew = focusContext.getObjectNew();
			if (focusObjectNew != null) {
				PolyStringType namePolyType = focusObjectNew.asObjectable().getName();
				if (namePolyType == null) {
					throw new SchemaException("Focus "+focusObjectNew+" does not have a name after "+activityDescription);
				}
				ObjectTypeTemplateType objectPolicyConfigurationType = focusContext.getObjectPolicyConfigurationType();
				if (objectPolicyConfigurationType != null && BooleanUtils.isTrue(objectPolicyConfigurationType.isOidNameBoundMode())) {
					ObjectDelta<F> focusDelta = focusContext.getDelta();
					if (focusDelta != null) {
						if (focusDelta.isAdd()) {
							if (namePolyType != null) {
								// name delta is OK, but it has to match
								if (focusObjectNew.getOid() != null) {
									if (!focusObjectNew.getOid().equals(namePolyType.getOrig())) {
										throw new PolicyViolationException("Cannot set name to a value different than OID in name-oid bound mode");
									}
								}
							}
						} else {
							PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(FocusType.F_NAME);
							if (nameDelta != null) {
								throw new PolicyViolationException("Cannot change name in name-oid bound mode");
							}
						}
					}
				}
			}
		}
	}
}
