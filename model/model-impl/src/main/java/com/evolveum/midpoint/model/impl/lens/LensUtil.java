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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.mutable.MutableBoolean;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.getSingleValue;
import static java.util.Collections.singleton;

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
					activity, phase, context.dump(showTriples));
        }
    }

	public static <F extends ObjectType> ResourceType getResourceReadOnly(LensContext<F> context,
																  String resourceOid, ProvisioningService provisioningService, Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ResourceType resourceType = context.getResource(resourceOid);
		if (resourceType == null) {
			// Fetching from provisioning to take advantage of caching and
			// pre-parsed schema
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
			resourceType = provisioningService.getObject(ResourceType.class, resourceOid, options, task, result)
					.asObjectable();
			context.rememberResource(resourceType);
		}
		return resourceType;
	}

	public static <F extends ObjectType> ResourceType getResourceReadOnly(LensContext<F> context, String resourceOid, ObjectResolver objectResolver,
																  Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ResourceType resourceType = context.getResource(resourceOid);
		if (resourceType == null) {
			ObjectReferenceType ref = new ObjectReferenceType();
			ref.setType(ResourceType.COMPLEX_TYPE);
			ref.setOid(resourceOid);
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
			resourceType = objectResolver.resolve(ref, ResourceType.class, options, "resource fetch in lens", task, result);
			context.rememberResource(resourceType);
		}
		return resourceType;
	}

	public static String refineProjectionIntent(ShadowKindType kind, String intent, ResourceType resource, PrismContext prismContext) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
		RefinedObjectClassDefinition rObjClassDef = refinedSchema.getRefinedDefinition(kind, intent);
		if (rObjClassDef == null) {
			throw new SchemaException("No projection definition for kind="+kind+" intent="+intent+" in "+resource);
		}
		return rObjClassDef.getIntent();
	}

	public static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context, PrismObject<ShadowType> equivalentAccount,
																				   ProvisioningService provisioningService, PrismContext prismContext,
																				   Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ShadowType equivalentAccountType = equivalentAccount.asObjectable();
		ShadowKindType kind = ShadowUtil.getKind(equivalentAccountType);
		return getProjectionContext(context, ShadowUtil.getResourceOid(equivalentAccountType),
				kind, equivalentAccountType.getIntent(), provisioningService,
				prismContext, task, result);
	}

	public static <F extends FocusType> LensProjectionContext getProjectionContext(LensContext<F> context, String resourceOid,
																				   ShadowKindType kind, String intent,
																				   ProvisioningService provisioningService, PrismContext prismContext,
																				   Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ResourceType resource = getResourceReadOnly(context, resourceOid, provisioningService, task, result);
		String refinedIntent = refineProjectionIntent(kind, intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, refinedIntent);
		return context.findProjectionContext(rsd);
	}

	public static <F extends ObjectType> LensProjectionContext getOrCreateProjectionContext(LensContext<F> context,
			ResourceShadowDiscriminator rsd) {
		LensProjectionContext accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext == null) {
			accountSyncContext = context.createProjectionContext(rsd);
			ResourceType resource = context.getResource(rsd.getResourceOid());
			accountSyncContext.setResource(resource);
		}
		accountSyncContext.setDoReconciliation(context.isDoReconciliationForAllProjections());
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
	@NotNull
	public static <V extends PrismValue, D extends ItemDefinition, I extends ItemValueWithOrigin<V,D>>
		ItemDelta<V,D> consolidateTripleToDelta(
			ItemPath itemPath, DeltaSetTriple<I> triple, D itemDefinition,
    		ItemDelta<V,D> aprioriItemDelta, PrismContainer<?> itemContainer, ValueMatcher<?> valueMatcher, Comparator<V> comparator,
    		boolean addUnchangedValues, boolean filterExistingValues, boolean isExclusiveStrong,
    		String contextDescription, boolean applyWeak) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {

		boolean isAssignment = new ItemPath(FocusType.F_ASSIGNMENT).equivalent(itemPath);

		ItemDelta<V,D> itemDelta = itemDefinition.createEmptyDelta(itemPath);

		Item<V,D> itemExisting = null;
		if (itemContainer != null) {
            itemExisting = itemContainer.findItem(itemPath);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Consolidating {} triple:\n{}\nApriori Delta:\n{}\nExisting item:\n{}",
					itemPath, triple.debugDump(1),
					DebugUtil.debugDump(aprioriItemDelta, 1),
					DebugUtil.debugDump(itemExisting, 1));
		}

        Collection<V> allValues = collectAllValues(triple, valueMatcher);

        final MutableBoolean itemHasStrongMutable = new MutableBoolean(false);
        SimpleVisitor<I> visitor = pvwo -> {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.STRONG) {
				itemHasStrongMutable.setValue(true);
			}
		};
		triple.simpleAccept(visitor);
        boolean ignoreNormalMappings = itemHasStrongMutable.booleanValue() && isExclusiveStrong;

        // We will process each value individually. I really mean each value. This whole method deals with
        // a single item (e.g. attribute). But this loop iterates over every potential value of that item.
        for (V value : allValues) {

        	LOGGER.trace("  consolidating value: {}", value);
        	// Check what to do with the value using the usual "triple routine". It means that if a value is
        	// in zero set than we need no delta, plus set means add delta and minus set means delete delta.
        	// The first set that the value is present determines the result.
            Collection<ItemValueWithOrigin<V,D>> zeroPvwos =
                    collectPvwosFromSet(value, triple.getZeroSet(), valueMatcher);
            Collection<ItemValueWithOrigin<V,D>> plusPvwos =
                    collectPvwosFromSet(value, triple.getPlusSet(), valueMatcher);
            Collection<ItemValueWithOrigin<V,D>> minusPvwos =
                    collectPvwosFromSet(value, triple.getMinusSet(), valueMatcher);

	        LOGGER.trace("PVWOs for value {}:\nzero = {}\nplus = {}\nminus = {}", value, zeroPvwos, plusPvwos, minusPvwos);

            boolean zeroHasStrong = false;
            if (!zeroPvwos.isEmpty()) {
            	for (ItemValueWithOrigin<V,D> pvwo : zeroPvwos) {
            		PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
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

            PrismValueDeltaSetTripleProducer<V, D> exclusiveMapping = null;
            Collection<ItemValueWithOrigin<V,D>> pvwosToAdd;
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
                for (ItemValueWithOrigin<V,D> pvwoToAdd : pvwosToAdd) {
                	PrismValueDeltaSetTripleProducer<V,D> mapping = pvwoToAdd.getMapping();
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
							value, itemPath, contextDescription);
                    continue;
                }
                if (!hasStrong && ignoreNormalMappings) {
                	LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (hasStrong && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value, true)) {
                	throw new PolicyViolationException("Attempt to delete value "+value+" from item "+itemPath
                			+" but that value is mandated by a strong mapping (in "+contextDescription+")");
                }
                if (!hasStrong && (aprioriItemDelta != null && !aprioriItemDelta.isEmpty())) {
                    // There is already a delta, skip this
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta that is more concrete, " +
                    		"skipping adding in {}", value, itemPath, contextDescription);
                    continue;
                }
                if (filterExistingValues && hasValue(itemExisting, value, valueMatcher, comparator)) {
                	LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
							value, itemPath, contextDescription);
                	continue;
                }
                LOGGER.trace("Value {} added to delta as ADD for item {} in {}", value, itemPath, contextDescription);
                itemDelta.addValueToAdd(cloneAndApplyMetadata(value, isAssignment, pvwosToAdd));
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
                for (ItemValueWithOrigin<V,D> pvwo : minusPvwos) {
                	PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
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
							value, itemPath, contextDescription);
                	continue;
                }
                if (!hasStrong && (aprioriItemDelta != null && !aprioriItemDelta.isEmpty())) {
                    // There is already a delta, skip this
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta that is more concrete, skipping deletion in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (weakOnly && (itemExisting != null && !itemExisting.isEmpty())) {
                    // There is already a value, skip this
                    LOGGER.trace("Value {} mapping is weak and the item {} already has a value, skipping deletion in {}",
							value, itemPath, contextDescription);
                    continue;
                }

                if (weakOnly && !applyWeak && (itemExisting == null || itemExisting.isEmpty())){
                	 // There is a weak mapping on a property, but we do not have full account available, so skipping deletion of the value is better way
                    LOGGER.trace("Value {} mapping is weak and the full account could not be fetched, skipping deletion in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (filterExistingValues && !hasValue(itemExisting, value, valueMatcher, comparator)) {
                	LOGGER.trace("Value {} NOT add to delta as DELETE because item {} the item does not have that value in {} (matcher: {})",
							value, itemPath, contextDescription, valueMatcher);
                	continue;
                }
                LOGGER.trace("Value {} added to delta as DELETE for item {} in {}",
						value, itemPath, contextDescription);
                itemDelta.addValueToDelete((V)value.clone());
            }

            if (!zeroPvwos.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	boolean hasAuthoritative = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<V,D> pvwo : zeroPvwos) {
                	PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
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
	                	LOGGER.trace("Value {} added to delta for item {} in {} because there is strong mapping in the zero set",
								value, itemPath, contextDescription);
	                    itemDelta.addValueToAdd(cloneAndApplyMetadata(value, isAssignment, zeroPvwos));
	                    continue;
	                }
	            }
            }
        }

        Item<V,D> itemNew = null;
        if (itemContainer != null) {
        	itemNew = itemContainer.findItem(itemPath);
        }
		if (!hasValue(itemNew, itemDelta)) {
			// The application of computed delta results in no value, apply weak mappings
			Collection<? extends ItemValueWithOrigin<V,D>> nonNegativePvwos = triple.getNonNegativeValues();
			Collection<ItemValueWithOrigin<V,D>> valuesToAdd = selectWeakValues(nonNegativePvwos, OriginType.ASSIGNMENTS, applyWeak);
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = selectWeakValues(nonNegativePvwos, OriginType.OUTBOUND, applyWeak);
			}
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = selectWeakValues(nonNegativePvwos, null, applyWeak);
			}
			LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
					itemPath, contextDescription, valuesToAdd);
			for (ItemValueWithOrigin<V, D> valueWithOrigin : valuesToAdd) {
				itemDelta.addValueToAdd(cloneAndApplyMetadata(valueWithOrigin.getItemValue(), isAssignment, singleton(valueWithOrigin)));
			}
		} else {
			LOGGER.trace("Existing values for item {} in {}, weak mapping processing skipped", itemPath, contextDescription);
		}

		if (itemExisting != null) {
			List<V> existingValues = itemExisting.getValues();
			if (existingValues != null) {
				itemDelta.setEstimatedOldValues(PrismValue.cloneCollection(existingValues));
			}
		}

        return itemDelta;
    }

	public static <V extends PrismValue, D extends ItemDefinition> V cloneAndApplyMetadata(V value, boolean isAssignment,
			Collection<ItemValueWithOrigin<V, D>> origins) throws SchemaException {
		return cloneAndApplyMetadata(value, isAssignment, () -> getAutoCreationIdentifier(origins));
	}

	public static <V extends PrismValue> Collection<V> cloneAndApplyMetadata(Collection<V> values, boolean isAssignment,
			MappingType mapping) throws SchemaException {
		List<V> rv = new ArrayList<>();
		for (V value : values) {
			rv.add(cloneAndApplyMetadata(value, isAssignment, mapping::getName));
		}
		return rv;
	}

	public static <V extends PrismValue> V cloneAndApplyMetadata(V value, boolean isAssignment,
			MappingType mapping) throws SchemaException {
		return cloneAndApplyMetadata(value, isAssignment, mapping::getName);
	}

	private static <V extends PrismValue> V cloneAndApplyMetadata(V value, boolean isAssignment,
			Supplier<String> autoCreateIdentifierSupplier) throws SchemaException {
		//noinspection unchecked
		V cloned = (V) value.clone();
		if (isAssignment && cloned instanceof PrismContainerValue) {
			String autoCreationIdentifier = autoCreateIdentifierSupplier.get();
			if (autoCreationIdentifier != null) {
				//noinspection unchecked
				PrismContainer<MetadataType> metadataContainer = ((PrismContainerValue) cloned).findOrCreateContainer(AssignmentType.F_METADATA);
				metadataContainer.getOrCreateValue().asContainerable().setAutoCreateIdentifier(autoCreationIdentifier);
			}
		}
		return cloned;
	}

	private static <V extends PrismValue, D extends ItemDefinition> String getAutoCreationIdentifier(Collection<ItemValueWithOrigin<V, D>> origins) {
		// let's ignore conflicts (name1 vs name2, named vs unnamed) for now
		for (ItemValueWithOrigin<V, D> origin : origins) {
			if (origin.getMapping() != null && origin.getMapping().getIdentifier() != null) {
				return origin.getMapping().getIdentifier();
			}
		}
		return null;
	}

	private static <V extends PrismValue, D extends ItemDefinition> boolean hasValue(Item<V,D> item, ItemDelta<V,D> itemDelta) throws SchemaException {
		if (item == null || item.isEmpty()) {
			return itemDelta != null && itemDelta.addsAnyValue();
		} else {
			if (itemDelta == null || itemDelta.isEmpty()) {
				return true;
			} else {
				Item<V,D> clonedItem = item.clone();
				itemDelta.applyToMatchingPath(clonedItem);
				return !clonedItem.isEmpty();
			}
		}
	}

	private static <V extends PrismValue, D extends ItemDefinition> Collection<ItemValueWithOrigin<V,D>> selectWeakValues(Collection<? extends ItemValueWithOrigin<V,D>> pvwos, OriginType origin, boolean applyWeak) {
		Collection<ItemValueWithOrigin<V,D>> values = new ArrayList<>();
		for (ItemValueWithOrigin<V,D> pvwo: pvwos) {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.WEAK && applyWeak) {
				if (origin == null || origin == pvwo.getItemValue().getOriginType()) {
					values.add(pvwo);
				}
			}
		}
		return values;
	}

	private static <V extends PrismValue, D extends ItemDefinition> boolean hasValue(Item<V,D> existingUserItem, V newValue, ValueMatcher<?> valueMatcher, Comparator<V> comparator) {
		if (existingUserItem == null) {
			return false;
		}
		if (valueMatcher != null && newValue instanceof PrismPropertyValue) {
			return valueMatcher.hasRealValue((PrismProperty)existingUserItem, (PrismPropertyValue)newValue);
		} else {
			return existingUserItem.contains(newValue, true, comparator);
		}
	}

	private static <V extends PrismValue, D extends ItemDefinition> Collection<V> collectAllValues
			(DeltaSetTriple<? extends ItemValueWithOrigin<V,D>> triple, ValueMatcher<?> valueMatcher) throws SchemaException {
        Collection<V> allValues = new HashSet<>();
        collectAllValuesFromSet(allValues, triple.getZeroSet(), valueMatcher);
        collectAllValuesFromSet(allValues, triple.getPlusSet(), valueMatcher);
        collectAllValuesFromSet(allValues, triple.getMinusSet(), valueMatcher);
        return allValues;
    }

    private static <V extends PrismValue, D extends ItemDefinition, T> void collectAllValuesFromSet(Collection<V> allValues,
            Collection<? extends ItemValueWithOrigin<V,D>> collection, ValueMatcher<T> valueMatcher) throws SchemaException {
        if (collection == null) {
            return;
        }
        for (ItemValueWithOrigin<V,D> pvwo : collection) {
        	V pval = pvwo.getItemValue();
        	if (valueMatcher == null) {
	        	if (!PrismValue.containsRealValue(allValues, pval)) {
	        		allValues.add(pval);
	        	}
        	} else {
        		boolean found = false;
        		for (V valueFromAllvalues: allValues) {
        			if (valueMatcher.match(((PrismPropertyValue<T>)valueFromAllvalues).getValue(),
        					((PrismPropertyValue<T>)pval).getValue())) {
        				found = true;
        				break;
        			}
        		}
        		if (!found) {
        			allValues.add(pval);
        		}
        	}
        }
    }

    private static <V extends PrismValue, D extends ItemDefinition> Collection<ItemValueWithOrigin<V,D>> collectPvwosFromSet(V pvalue,
            Collection<? extends ItemValueWithOrigin<V,D>> deltaSet, ValueMatcher<?> valueMatcher) throws SchemaException {
    	Collection<ItemValueWithOrigin<V,D>> pvwos = new ArrayList<>();
        for (ItemValueWithOrigin<V,D> setPvwo : deltaSet) {
        	if (setPvwo.equalsRealValue(pvalue, valueMatcher)) {
        		pvwos.add(setPvwo);
        	}
        }
        return pvwos;
    }

    public static PropertyDelta<XMLGregorianCalendar> createActivationTimestampDelta(ActivationStatusType status, XMLGregorianCalendar now,
    		PrismContainerDefinition<ActivationType> activationDefinition, OriginType origin) {
    	QName timestampPropertyName;
		if (status == null || status == ActivationStatusType.ENABLED) {
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

	public static Object getIterationVariableValue(LensProjectionContext accCtx) {
		Integer iterationOld = null;
		PrismObject<ShadowType> shadowCurrent = accCtx.getObjectCurrent();
		if (shadowCurrent != null) {
			iterationOld = shadowCurrent.asObjectable().getIteration();
		}
		if (iterationOld == null) {
			return accCtx.getIteration();
		}
		PrismPropertyDefinition<Integer> propDef = new PrismPropertyDefinitionImpl<>(ExpressionConstants.VAR_ITERATION,
				DOMUtil.XSD_INT, accCtx.getPrismContext());
		PrismProperty<Integer> propOld = propDef.instantiate();
		propOld.setRealValue(iterationOld);
		PropertyDelta<Integer> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION));
		propDelta.setValueToReplace(new PrismPropertyValue<Integer>(accCtx.getIteration()));
		PrismProperty<Integer> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIteration());
		ItemDeltaItem<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> idi = new ItemDeltaItem<>(propOld, propDelta, propNew);
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
		PrismPropertyDefinition<String> propDef = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.VAR_ITERATION_TOKEN, DOMUtil.XSD_STRING, accCtx.getPrismContext());
		PrismProperty<String> propOld = propDef.instantiate();
		propOld.setRealValue(iterationTokenOld);
		PropertyDelta<String> propDelta = propDef.createEmptyDelta(new ItemPath(ExpressionConstants.VAR_ITERATION_TOKEN));
		propDelta.setValueToReplace(new PrismPropertyValue<String>(accCtx.getIterationToken()));
		PrismProperty<String> propNew = propDef.instantiate();
		propNew.setRealValue(accCtx.getIterationToken());
		ItemDeltaItem<PrismPropertyValue<String>,PrismPropertyDefinition<String>> idi = new ItemDeltaItem<>(propOld, propDelta, propNew);
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

	public static <F extends ObjectType> boolean hasDependentContext(LensContext<F> context,
			LensProjectionContext targetProjectionContext) {
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			for (ResourceObjectTypeDependencyType dependency: projectionContext.getDependencies()) {
				if (isDependencyTargetContext(projectionContext, targetProjectionContext, dependency)) {
					return true;
				}
			}
		}
		return false;
	}

	public static <F extends ObjectType> boolean isDependencyTargetContext(LensProjectionContext sourceProjContext, LensProjectionContext targetProjectionContext, ResourceObjectTypeDependencyType dependency) {
		ResourceShadowDiscriminator refDiscr = new ResourceShadowDiscriminator(dependency,
				sourceProjContext.getResource().getOid(), sourceProjContext.getKind());
		return targetProjectionContext.compareResourceShadowDiscriminator(refDiscr, false);
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
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (iterationType == null) {
			return formatIterationTokenDefault(iteration);
		}
		ExpressionType tokenExpressionType = iterationType.getTokenExpression();
		if (tokenExpressionType == null) {
			return formatIterationTokenDefault(iteration);
		}
		PrismPropertyDefinition<String> outputDefinition = new PrismPropertyDefinitionImpl<>(ExpressionConstants.VAR_ITERATION_TOKEN,
				DOMUtil.XSD_STRING, context.getPrismContext());
		Expression<PrismPropertyValue<String>,PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(tokenExpressionType, outputDefinition , "iteration token expression in "+accountContext.getHumanReadableName(), task, result);

		Collection<Source<?,?>> sources = new ArrayList<>();
		PrismPropertyDefinitionImpl<Integer> inputDefinition = new PrismPropertyDefinitionImpl<>(ExpressionConstants.VAR_ITERATION,
				DOMUtil.XSD_INT, context.getPrismContext());
		inputDefinition.setMaxOccurs(1);
		PrismProperty<Integer> input = inputDefinition.instantiate();
		input.add(new PrismPropertyValue<Integer>(iteration));
		ItemDeltaItem<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> idi = new ItemDeltaItem<>(input);
		Source<PrismPropertyValue<Integer>,PrismPropertyDefinition<Integer>> iterationSource = new Source<>(idi, ExpressionConstants.VAR_ITERATION);
		sources.add(iterationSource);

		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(sources , variables,
				"iteration token expression in "+accountContext.getHumanReadableName(), task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, task, result);
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
					throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
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
		PrismPropertyDefinition<Boolean> outputDefinition = new PrismPropertyDefinitionImpl<>(
				ExpressionConstants.OUTPUT_ELEMENT_NAME,
				DOMUtil.XSD_BOOLEAN, context.getPrismContext());
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(expressionType, outputDefinition , desc, task, result);

		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION, iteration);
		variables.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, iterationToken);

		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, context, null, task, result);
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

    /**
     * Used for assignments and similar objects that do not have separate lifecycle.
     */
    public static boolean isAssignmentValid(FocusType focus, AssignmentType assignmentType, XMLGregorianCalendar now, ActivationComputer activationComputer) {
    	String focusLifecycleState = focus.getLifecycleState();
		if (!activationComputer.lifecycleHasActiveAssignments(focusLifecycleState)) {
			return false;
		}
		return isValid(assignmentType.getLifecycleState(), assignmentType.getActivation(), now, activationComputer);
	}

	public static boolean isFocusValid(FocusType focus, XMLGregorianCalendar now, ActivationComputer activationComputer) {
		return isValid(focus.getLifecycleState(), focus.getActivation(), now, activationComputer);
	}

	private static boolean isValid(String lifecycleState, ActivationType activationType, XMLGregorianCalendar now, ActivationComputer activationComputer) {
		TimeIntervalStatusType validityStatus = activationComputer.getValidityStatus(activationType, now);
		ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(lifecycleState, activationType, validityStatus);
		return effectiveStatus == ActivationStatusType.ENABLED;
	}

	public static AssignmentPathVariables computeAssignmentPathVariables(AssignmentPathImpl assignmentPath) throws SchemaException {
    	if (assignmentPath == null || assignmentPath.isEmpty()) {
    		return null;
    	}
    	AssignmentPathVariables vars = new AssignmentPathVariables();
    	vars.setAssignmentPath(assignmentPath.clone());

    	Iterator<AssignmentPathSegmentImpl> iterator = assignmentPath.getSegments().iterator();
		while (iterator.hasNext()) {
			AssignmentPathSegmentImpl segment = iterator.next();
			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> segmentAssignmentIdi = segment.getAssignmentIdi();

			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> magicAssignmentIdi;
			// Magic assignment
			if (vars.getMagicAssignment() == null) {
				magicAssignmentIdi = segmentAssignmentIdi.clone();
				vars.setMagicAssignment(magicAssignmentIdi);
			} else {
				// Collect extension values from the assignment extension
				magicAssignmentIdi = vars.getMagicAssignment();
				mergeExtension(magicAssignmentIdi, segmentAssignmentIdi);
			}

			// Collect extension values from the source object extension
			ObjectType segmentSource = segment.getSource();
			if (segmentSource != null) {
				mergeExtension(magicAssignmentIdi, segmentSource.asPrismObject());
			}

			// immediate assignment (use assignment from previous iteration)
			vars.setImmediateAssignment(vars.getThisAssignment());

			// this assignment
			ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> thisAssignment = segmentAssignmentIdi.clone();
			vars.setThisAssignment(thisAssignment);

			if (iterator.hasNext() && segmentSource instanceof AbstractRoleType) {
				vars.setImmediateRole((PrismObject<? extends AbstractRoleType>) segmentSource.asPrismObject());
			}
		}

		AssignmentPathSegmentImpl focusAssignmentSegment = assignmentPath.first();
		vars.setFocusAssignment(focusAssignmentSegment.getAssignmentIdi().clone());

		// a bit of hack -- TODO reconsider in 3.7
		// objects are already cloned
		convertToLegacy(vars.getMagicAssignment());
		convertToLegacy(vars.getThisAssignment());
		convertToLegacy(vars.getFocusAssignment());
		convertToLegacy(vars.getImmediateAssignment());

		return vars;
    }

	private static void convertToLegacy(
			ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> idi) {
		if (idi == null || idi.getDelta() == null || idi.getSubItemDeltas() != null) {
			return;
		}
		// Legacy approach (when adding/removing assignments) was: itemOld+itemNew = value, delta = null
		// This was recently changed, to provide precise information (add = null->itemNew, delete = itemOld->null).
		// However, to not break scripts before 3.6 release we provide imitation of old behavior here.
		// (Moreover, for magic assignment the delta is not correct anyway.)
		if (idi.getDelta().isAdd() || idi.getDelta().isReplace()) {
			idi.setItemOld(idi.getItemNew().clone());
		} else {
			idi.setItemNew(idi.getItemOld().clone());
		}
		idi.setDelta(null);
	}

	private static void mergeExtension(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi, ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcIdi) throws SchemaException {
		mergeExtension(destIdi.getItemOld(), srcIdi.getItemOld());
		mergeExtension(destIdi.getItemNew(), srcIdi.getItemNew());
    	if (srcIdi.getDelta() != null || srcIdi.getSubItemDeltas() != null) {
    		throw new UnsupportedOperationException("Merge of IDI with deltas not supported");
    	}
	}

    private static void mergeExtension(Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem,
			Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> srcItem) throws SchemaException {
    	if (srcItem == null || dstItem == null) {
    		return;
    	}
    	PrismContainer<Containerable> srcExtension = ((PrismContainer<AssignmentType>)srcItem).findContainer(AssignmentType.F_EXTENSION);
    	mergeExtensionContainers(dstItem, srcExtension);
	}

    private static <O extends ObjectType> void mergeExtension(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> destIdi,
			PrismObject<O> srcObject) throws SchemaException {
    	if (srcObject == null) {
    		return;
    	}

    	PrismContainer<Containerable> srcExtension = srcObject.findContainer(ObjectType.F_EXTENSION);

    	mergeExtensionContainers(destIdi.getItemNew(), srcExtension);
    	mergeExtensionContainers(destIdi.getItemOld(), srcExtension);
    }

    private static void  mergeExtensionContainers(Item<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> dstItem, PrismContainer<Containerable> srcExtension) throws SchemaException {
    	if (dstItem == null) {
    		return;
    	}
    	PrismContainer<AssignmentType> dstContainer = (PrismContainer<AssignmentType>) dstItem;
    	if (srcExtension != null && !srcExtension.getValues().isEmpty()) {
		    PrismContainer<?> dstExtensionContainer = dstContainer.findOrCreateContainer(AssignmentType.F_EXTENSION);
		    PrismContainerValue<?> dstExtensionContainerValue = dstExtensionContainer.getValues().isEmpty()
				    ? dstExtensionContainer.createNewValue() : dstExtensionContainer.getValue();
		    ObjectTypeUtil.mergeExtension(dstExtensionContainerValue, srcExtension.getValue());
	    }
	}

	public static <V extends PrismValue,D extends ItemDefinition> Mapping.Builder<V,D> addAssignmentPathVariables(Mapping.Builder<V,D> builder, AssignmentPathVariables assignmentPathVariables) {
    	ExpressionVariables expressionVariables = new ExpressionVariables();
		Utils.addAssignmentPathVariables(assignmentPathVariables, expressionVariables);
		return builder.addVariableDefinitions(expressionVariables.getMap());
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
				ObjectPolicyConfigurationType objectPolicyConfigurationType = focusContext.getObjectPolicyConfigurationType();
				checkObjectPolicy(focusContext, objectPolicyConfigurationType);
			}
		}
	}

	private static <F extends ObjectType> void checkObjectPolicy(LensFocusContext<F> focusContext, ObjectPolicyConfigurationType objectPolicyConfigurationType) throws SchemaException, PolicyViolationException {
		if (objectPolicyConfigurationType == null) {
			return;
		}
		PrismObject<F> focusObjectNew = focusContext.getObjectNew();
		ObjectDelta<F> focusDelta = focusContext.getDelta();

		for (PropertyConstraintType propertyConstraintType: objectPolicyConfigurationType.getPropertyConstraint()) {
			ItemPath itemPath = propertyConstraintType.getPath().getItemPath();
			if (BooleanUtils.isTrue(propertyConstraintType.isOidBound())) {
				if (focusDelta != null) {
					if (focusDelta.isAdd()) {
						PrismProperty<Object> propNew = focusObjectNew.findProperty(itemPath);
						if (propNew != null) {
							// prop delta is OK, but it has to match
							if (focusObjectNew.getOid() != null) {
								if (!focusObjectNew.getOid().equals(propNew.getRealValue().toString())) {
									throw new PolicyViolationException("Cannot set "+itemPath+" to a value different than OID in oid bound mode");
								}
							}
						}
					} else {
						PropertyDelta<Object> nameDelta = focusDelta.findPropertyDelta(itemPath);
						if (nameDelta != null) {
							if (nameDelta.isReplace()) {
								Collection<PrismPropertyValue<Object>> valuesToReplace = nameDelta.getValuesToReplace();
								if (valuesToReplace.size() == 1) {
									String stringValue = valuesToReplace.iterator().next().getValue().toString();
									if (focusContext.getOid().equals(stringValue)) {
										// This is OK. It is most likely a correction made by a recompute.
										continue;
									}
								}
							}
							throw new PolicyViolationException("Cannot change "+itemPath+" in oid bound mode");
						}
					}
				}
			}
		}

		// Deprecated
		if (BooleanUtils.isTrue(objectPolicyConfigurationType.isOidNameBoundMode())) {
			if (focusDelta != null) {
				if (focusDelta.isAdd()) {
					PolyStringType namePolyType = focusObjectNew.asObjectable().getName();
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

	public static PrismContainer<AssignmentType> createAssignmentSingleValueContainerClone(@NotNull AssignmentType assignmentType) throws SchemaException {
		// Make it appear to be single-value. Therefore paths without segment IDs will work.
		return assignmentType.asPrismContainerValue().asSingleValuedContainer(SchemaConstantsGenerated.C_ASSIGNMENT);
	}

	public static AssignmentType getAssignmentType(ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi, boolean old) {
		return PrismContainerValue.asContainerable(assignmentIdi.getSingleValue(old));
	}


	public static <F extends ObjectType> String getChannel(LensContext<F> context, Task task) {
    	if (context != null && context.getChannel() != null){
    		return context.getChannel();
    	} else if (task.getChannel() != null){
    		return task.getChannel();
    	}
    	return null;
    }

	public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ItemDelta<?,?> itemDelta) {
		if (itemDelta.getEstimatedOldValues() != null) {
			return;
		}
		if (ctx.getObjectOld() != null) {
			Item<PrismValue, ItemDefinition> itemOld = ctx.getObjectOld().findItem(itemDelta.getPath());
			if (itemOld != null) {
				itemDelta.setEstimatedOldValues((Collection) PrismValue.cloneCollection(itemOld.getValues()));
			} else {
				// get the old data from current object. Still better estimate than nothing
				if (ctx.getObjectCurrent() != null) {
					 itemOld = ctx.getObjectCurrent().findItem(itemDelta.getPath());
					 if (itemOld != null) {
						 itemDelta.setEstimatedOldValues((Collection) PrismValue.cloneCollection(itemOld.getValues()));
					 }
				}
			}
		}
	}

	public static <O extends ObjectType> void setDeltaOldValue(LensElementContext<O> ctx, ObjectDelta<O> objectDelta) {
		if (objectDelta == null) {
			return;
		}
		if (!objectDelta.isModify()) {
			return;
		}
		for (ItemDelta<?, ?> modification: objectDelta.getModifications()) {
			setDeltaOldValue(ctx, modification);
		}
	}

	public static <F extends ObjectType> LensObjectDeltaOperation<F> createObjectDeltaOperation(ObjectDelta<F> focusDelta, OperationResult result,
																								LensElementContext<F> focusContext, LensProjectionContext projCtx) {
		return createObjectDeltaOperation(focusDelta, result, focusContext, projCtx, null);
	}

	// projCtx may or may not be present (object itself can be focus or projection)
	public static <T extends ObjectType> LensObjectDeltaOperation<T> createObjectDeltaOperation(ObjectDelta<T> objectDelta, OperationResult result,
																								LensElementContext<T> objectContext,
																								LensProjectionContext projCtx,
																								ResourceType resource) {
		LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<T>(objectDelta.clone());
		objectDeltaOp.setExecutionResult(result);
		PrismObject<T> object = objectContext.getObjectAny();
		if (object != null) {
			PolyString name = object.getName();
			if (name == null && object.asObjectable() instanceof ShadowType) {
				try {
					name = ShadowUtil.determineShadowName((PrismObject<ShadowType>) object);
					if (name == null) {
						LOGGER.debug("No name for shadow:\n{}", object.debugDump());
					} else if (name.getNorm() == null) {
						name.recompute(new PrismDefaultPolyStringNormalizer());
					}
				} catch (SchemaException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine name for shadow -- continuing with no name; shadow:\n{}", e, object.debugDump());
				}
			}
			objectDeltaOp.setObjectName(name);
		}
		if (resource == null && projCtx != null) {
			resource = projCtx.getResource();
		}
		if (resource != null) {
			objectDeltaOp.setResourceOid(resource.getOid());
			objectDeltaOp.setResourceName(PolyString.toPolyString(resource.getName()));
		} else if (objectContext instanceof LensProjectionContext) {
			objectDeltaOp.setResourceOid(((LensProjectionContext) objectContext).getResourceOid());
		}
		return objectDeltaOp;
	}

	@Deprecated
	public static boolean isDelegationRelation(QName relation) {
		return ObjectTypeUtil.isDelegationRelation(relation);
	}

	public static void triggerRule(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger<?>> triggers,
			Collection<String> policySituations) {

		LOGGER.debug("Policy rule {} triggered: {}", rule.getName(), triggers);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Policy rule {} triggered:\n{}", rule.getName(), DebugUtil.debugDump(triggers, 1));
		}

		((EvaluatedPolicyRuleImpl) rule).addTriggers(triggers);
		CollectionUtils.addIgnoreNull(policySituations, rule.getPolicySituation());
	}

	public static void triggerConstraintLegacy(EvaluatedPolicyRuleTrigger trigger, Collection<String> policySituations) throws PolicyViolationException {

		LOGGER.debug("Legacy policy rule triggered: {}", trigger);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Legacy Policy rule triggered:\n{}", trigger.debugDump(1));
		}

		if (trigger.getConstraint().getEnforcement() == null || trigger.getConstraint().getEnforcement() == PolicyConstraintEnforcementType.ENFORCE) {
			throw new PolicyViolationException(trigger.getMessage());
		}
	}



	public static void processRuleWithException(@NotNull EvaluatedPolicyRule rule, Collection<EvaluatedPolicyRuleTrigger <?>> triggers,
			 PolicyExceptionType policyException) {

		LOGGER.debug("Policy rule {} would be triggered, but there is an exception for it. Not triggering", rule.getName());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Policy rule {} would be triggered, but there is an exception for it:\nTriggers:\n{}\nException:\n{}",
					rule.getName(), DebugUtil.debugDump(triggers, 1), policyException);
		}
		((EvaluatedPolicyRuleImpl)rule).addPolicyException(policyException);
	}


	public static void partialExecute(String componentName, ProjectorComponentRunnable runnable, Supplier<PartialProcessingTypeType> optionSupplier)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
			PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
		partialExecute(componentName, runnable, optionSupplier, null);
	}

	public static void partialExecute(String componentName, ProjectorComponentRunnable runnable,
			Supplier<PartialProcessingTypeType> optionSupplier, OperationResult result)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
			PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PreconditionViolationException {
		PartialProcessingTypeType option = optionSupplier.get();
		if (option == PartialProcessingTypeType.SKIP) {
			LOGGER.debug("Skipping projector component {} because partial execution option is set to {}", componentName, option);
		} else {
			LOGGER.trace("Projector component started: {}", componentName);
			try {
				runnable.run();
				LOGGER.trace("Projector component finished: {}", componentName);
			} catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
					| PolicyViolationException | ExpressionEvaluationException | ObjectAlreadyExistsException | PreconditionViolationException | RuntimeException | Error e) {
				LOGGER.trace("Projector component error: {}: {}: {}", componentName, e.getClass().getSimpleName(), e.getMessage());
				if (result != null) {
					result.recordFatalError(e);
				}
				throw e;
			}

		}
	}

	public static void checkMaxIterations(int iteration, int maxIterations, String conflictMessage, String humanReadableName)
			throws ObjectAlreadyExistsException {
		if (iteration > maxIterations) {
			StringBuilder sb = new StringBuilder();
			if (iteration == 1) {
				sb.append("Error processing ");
			} else {
				sb.append("Too many iterations (").append(iteration).append(") for ");
			}
			sb.append(humanReadableName);
			if (iteration == 1) {
				sb.append(": constraint violation: ");
			} else {
				sb.append(": cannot determine values that satisfy constraints: ");
			}
			if (conflictMessage != null) {
				sb.append(conflictMessage);
			}
			throw new ObjectAlreadyExistsException(sb.toString());
		}
	}

	public static boolean needsFullShadowForCredentialProcessing(LensProjectionContext projCtx) throws SchemaException {
		RefinedObjectClassDefinition refinedProjDef = projCtx.getStructuralObjectClassDefinition();
		if (refinedProjDef == null) {
			return false;
		}

		List<MappingType> outboundMappingType = refinedProjDef.getPasswordOutbound();
		if (outboundMappingType == null) {
			return false;
		}
		for (MappingType mappingType: outboundMappingType) {
			if (mappingType.getStrength() == MappingStrengthType.STRONG || mappingType.getStrength() == MappingStrengthType.WEAK) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPasswordReturnedByDefault(LensProjectionContext projCtx) {
		CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil.getEffectiveCapability(projCtx.getResource(), CredentialsCapabilityType.class);
		return CapabilityUtil.isPasswordReturnedByDefault(credentialsCapabilityType);
	}

	public static boolean evaluateBoolean(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result,
				DOMUtil.XSD_BOOLEAN, false, null);
	}

	public static String evaluateString(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result,
				DOMUtil.XSD_STRING, null, null);
	}

	public static LocalizableMessageType evaluateLocalizableMessageType(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
			OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Function<Object, Object> additionalConvertor = (o) -> {
			if (o == null || o instanceof LocalizableMessageType) {
				return o;
			} else if (o instanceof LocalizableMessage) {
				return LocalizationUtil.createLocalizableMessageType((LocalizableMessage) o);
			} else {
				return new LocalizableMessageType().fallbackMessage(String.valueOf(o));
			}
		};
		return evaluateExpressionSingle(expressionBean, expressionVariables, contextDescription, expressionFactory, prismContext,
				task, result, LocalizableMessageType.COMPLEX_TYPE, null, additionalConvertor);
	}

	public static <T> T evaluateExpressionSingle(ExpressionType expressionBean, ExpressionVariables expressionVariables,
			String contextDescription, ExpressionFactory expressionFactory, PrismContext prismContext, Task task,
			OperationResult result, QName typeName,
			T defaultValue, Function<Object, Object> additionalConvertor)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismPropertyDefinition<T> resultDef = new PrismPropertyDefinitionImpl<>(
				new QName(SchemaConstants.NS_C, "result"), typeName, prismContext);
		Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression =
				expressionFactory.makeExpression(expressionBean, resultDef, contextDescription, task, result);
		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, expressionVariables, contextDescription, task, result);
		context.setAdditionalConvertor(additionalConvertor);
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple = ModelExpressionThreadLocalHolder
				.evaluateExpressionInContext(expression, context, task, result);
		List<T> results = exprResultTriple.getZeroSet().stream()
				.map(ppv -> (T) ppv.getRealValue())
				.collect(Collectors.toList());
		return getSingleValue(results, defaultValue, contextDescription);
	}

	@NotNull
	public static LocalizableMessageType createLocalizableMessageType(LocalizableMessageTemplateType template,
			ExpressionVariables var, ExpressionFactory expressionFactory, PrismContext prismContext,
			Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, SecurityViolationException {
		LocalizableMessageType rv = new LocalizableMessageType();
		if (template.getKey() != null) {
			rv.setKey(template.getKey());
		} else if (template.getKeyExpression() != null) {
			rv.setKey(evaluateString(template.getKeyExpression(), var, "localizable message key expression", expressionFactory, prismContext, task, result));
		}
		if (!template.getArgument().isEmpty() && !template.getArgumentExpression().isEmpty()) {
			throw new IllegalArgumentException("Both argument and argumentExpression items are non empty");
		} else if (!template.getArgumentExpression().isEmpty()) {
			for (ExpressionType argumentExpression : template.getArgumentExpression()) {
				LocalizableMessageType argument = evaluateLocalizableMessageType(argumentExpression, var,
						"localizable message argument expression", expressionFactory, prismContext, task, result);
				rv.getArgument().add(new LocalizableMessageArgumentType().localizable(argument));
			}
		} else {
			// TODO allow localizable messages templates here
			rv.getArgument().addAll(template.getArgument());
		}
		if (template.getFallbackMessage() != null) {
			rv.setFallbackMessage(template.getFallbackMessage());
		} else if (template.getFallbackMessageExpression() != null) {
			rv.setFallbackMessage(evaluateString(template.getFallbackMessageExpression(), var,
					"localizable message fallback expression", expressionFactory, prismContext, task, result));
		}
		return rv;
	}
}
