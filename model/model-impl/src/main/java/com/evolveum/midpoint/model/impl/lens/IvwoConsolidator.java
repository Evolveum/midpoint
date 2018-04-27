/**
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

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.SimpleVisitor;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

/**
 * Consolidate the mappings of a single item to a delta. It takes the convenient structure of ItemValueWithOrigin triple.
 * It produces the delta considering the mapping exclusion, authoritativeness and strength.
 *
 * filterExistingValues: if true, then values that already exist in the item are not added (and those that don't exist are not removed)
 * 
 * @author semancik
 */
public class IvwoConsolidator<V extends PrismValue, D extends ItemDefinition, I extends ItemValueWithOrigin<V,D>> {
	
	private static final Trace LOGGER = TraceManager.getTrace(IvwoConsolidator.class);
	
	private ItemPath itemPath;
	private DeltaSetTriple<I> ivwoTriple;
	private D itemDefinition;
	private ItemDelta<V,D> aprioriItemDelta;
	private PrismContainer<?> itemContainer;
	private ValueMatcher valueMatcher;
	private Comparator<V> comparator;
	private boolean addUnchangedValues;
	private boolean filterExistingValues;
	private boolean isExclusiveStrong;
	private String contextDescription;
	private StrengthSelector strengthSelector = StrengthSelector.ALL;
	
	public ItemPath getItemPath() {
		return itemPath;
	}

	public void setItemPath(ItemPath itemPath) {
		this.itemPath = itemPath;
	}

	public DeltaSetTriple<I> getIvwoTriple() {
		return ivwoTriple;
	}

	public void setIvwoTriple(DeltaSetTriple<I> ivwoTriple) {
		this.ivwoTriple = ivwoTriple;
	}

	public D getItemDefinition() {
		return itemDefinition;
	}

	public void setItemDefinition(D itemDefinition) {
		this.itemDefinition = itemDefinition;
	}

	public ItemDelta<V, D> getAprioriItemDelta() {
		return aprioriItemDelta;
	}

	public void setAprioriItemDelta(ItemDelta<V, D> aprioriItemDelta) {
		this.aprioriItemDelta = aprioriItemDelta;
	}

	public PrismContainer<?> getItemContainer() {
		return itemContainer;
	}

	public void setItemContainer(PrismContainer<?> itemContainer) {
		this.itemContainer = itemContainer;
	}

	public ValueMatcher getValueMatcher() {
		return valueMatcher;
	}

	public void setValueMatcher(ValueMatcher valueMatcher) {
		this.valueMatcher = valueMatcher;
	}

	public Comparator<V> getComparator() {
		return comparator;
	}

	public void setComparator(Comparator<V> comparator) {
		this.comparator = comparator;
	}

	public boolean isAddUnchangedValues() {
		return addUnchangedValues;
	}

	public void setAddUnchangedValues(boolean addUnchangedValues) {
		this.addUnchangedValues = addUnchangedValues;
	}

	public boolean isFilterExistingValues() {
		return filterExistingValues;
	}

	public void setFilterExistingValues(boolean filterExistingValues) {
		this.filterExistingValues = filterExistingValues;
	}

	public boolean isExclusiveStrong() {
		return isExclusiveStrong;
	}

	public void setExclusiveStrong(boolean isExclusiveStrong) {
		this.isExclusiveStrong = isExclusiveStrong;
	}

	public String getContextDescription() {
		return contextDescription;
	}

	public void setContextDescription(String contextDescription) {
		this.contextDescription = contextDescription;
	}

	public StrengthSelector getStrengthSelector() {
		return strengthSelector;
	}

	public void setStrengthSelector(StrengthSelector strengthSelector) {
		this.strengthSelector = strengthSelector;
	}

	@NotNull
	public ItemDelta<V,D> consolidateToDelta() throws ExpressionEvaluationException, PolicyViolationException, SchemaException {
		if (strengthSelector.isNone()) {
			LOGGER.trace("Consolidation of {} skipped as strength selector is 'none'", itemPath);
			return null;
		}
		
		boolean isAssignment = new ItemPath(FocusType.F_ASSIGNMENT).equivalent(itemPath);

		ItemDelta<V,D> itemDelta = itemDefinition.createEmptyDelta(itemPath);

		Item<V,D> itemExisting = null;
		if (itemContainer != null) {
            itemExisting = itemContainer.findItem(itemPath);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Consolidating {} IVwO triple:\n{}\n  Apriori Delta:\n{}\n  Existing item:\n{}",
					itemPath, ivwoTriple.debugDump(1),
					DebugUtil.debugDump(aprioriItemDelta, 2),
					DebugUtil.debugDump(itemExisting, 2));
		}

        Collection<V> allValues = collectAllValues();

        final MutableBoolean itemHasStrongMutable = new MutableBoolean(false);
        SimpleVisitor<I> visitor = pvwo -> {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.STRONG) {
				itemHasStrongMutable.setValue(true);
			}
		};
		ivwoTriple.simpleAccept(visitor);
        boolean ignoreNormalMappings = itemHasStrongMutable.booleanValue() && isExclusiveStrong;

        // We will process each value individually. I really mean each value. This whole method deals with
        // a single item (e.g. attribute). But this loop iterates over every potential value of that item.
        for (V value : allValues) {

        	LOGGER.trace("  consolidating value: {}", value);
        	// Check what to do with the value using the usual "triple routine". It means that if a value is
        	// in zero set than we need no delta, plus set means add delta and minus set means delete delta.
        	// The first set that the value is present determines the result.
            Collection<ItemValueWithOrigin<V,D>> zeroIvwos =
                    collectIvwosFromSet(value, ivwoTriple.getZeroSet(), false);
            Collection<ItemValueWithOrigin<V,D>> plusIvwos =
                    collectIvwosFromSet(value, ivwoTriple.getPlusSet(), false);
            Collection<ItemValueWithOrigin<V,D>> minusIvwos =
                    collectIvwosFromSet(value, ivwoTriple.getMinusSet(), true);

	        LOGGER.trace("PVWOs for value {}:\nzero = {}\nplus = {}\nminus = {}", value, zeroIvwos, plusIvwos, minusIvwos);

            PrismValueDeltaSetTripleProducer<V,D> zeroStrongMapping = null;
            if (!zeroIvwos.isEmpty()) {
            	for (ItemValueWithOrigin<V,D> pvwo : zeroIvwos) {
            		PrismValueDeltaSetTripleProducer<V,D> mapping = pvwo.getMapping();
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	zeroStrongMapping = mapping;
                    }
            	}
            }

            if (zeroStrongMapping != null && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value, true)) {
            	throw new PolicyViolationException("Attempt to delete value " + value + " from item " + itemPath
            			+ " but that value is mandated by a strong " + zeroStrongMapping.toHumanReadableDescription()
            			+ " (for " + contextDescription + ")");
            }
            if (!zeroIvwos.isEmpty() && !addUnchangedValues) {
                // Value unchanged, nothing to do
                LOGGER.trace("Value {} unchanged, doing nothing", value);
                continue;
            }

            PrismValueDeltaSetTripleProducer<V, D> exclusiveMapping = null;
            Collection<ItemValueWithOrigin<V,D>> pvwosToAdd;
            if (addUnchangedValues) {
                pvwosToAdd = MiscUtil.union(zeroIvwos, plusIvwos);
            } else {
                pvwosToAdd = plusIvwos;
            }

            if (!pvwosToAdd.isEmpty()) {
            	boolean hasOnlyWeakMappings = true;
            	boolean hasAtLeastOneStrongMapping = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<V,D> pvwoToAdd : pvwosToAdd) {
                	PrismValueDeltaSetTripleProducer<V,D> mapping = pvwoToAdd.getMapping();
                    if (mapping.getStrength() != MappingStrengthType.WEAK) {
                        hasOnlyWeakMappings = false;
                    }
                    if (mapping.getStrength() == MappingStrengthType.STRONG) {
                    	hasAtLeastOneStrongMapping = true;
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
                if (hasOnlyWeakMappings) {
                    // Postpone processing of weak values until we process all other values
                    LOGGER.trace("Value {} mapping is weak in item {}, postponing processing in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (!strengthSelector.isNormal() && !strengthSelector.isStrong()) {
                	LOGGER.trace("Value {} has only strong/normal mapping and we are processing only weak mappings now, in item {} in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (!hasAtLeastOneStrongMapping && ignoreNormalMappings) {
                	LOGGER.trace("Value {} mapping is normal in item {} and we have exclusiveStrong, skipping processing in {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (hasAtLeastOneStrongMapping && aprioriItemDelta != null && aprioriItemDelta.isValueToDelete(value, true)) {
                	throw new PolicyViolationException("Attempt to delete value "+value+" from item "+itemPath
                			+" but that value is mandated by a strong mapping (in "+contextDescription+")");
                }
                if (!hasAtLeastOneStrongMapping && (aprioriItemDelta != null && !aprioriItemDelta.isEmpty())) {
                    // There is already a delta, skip this
                    LOGGER.trace("Value {} mapping is not strong and the item {} already has a delta that is more concrete, " +
                    		"skipping adding in {}", value, itemPath, contextDescription);
                    continue;
                }
                if (filterExistingValues && hasValue(itemExisting, value)) {
                	LOGGER.trace("Value {} NOT added to delta for item {} because the item already has that value in {}",
							value, itemPath, contextDescription);
                	continue;
                }
                LOGGER.trace("Value {} added to delta as ADD for item {} in {}", value, itemPath, contextDescription);
                itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value, isAssignment, pvwosToAdd));
                continue;
            }

            // We need to check for empty plus set. Values that are both in plus and minus are considered to be added.
            // So check for that special case here to avoid removing them.
            if (!minusIvwos.isEmpty() && plusIvwos.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	boolean hasAuthoritative = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<V,D> pvwo : minusIvwos) {
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

                if (weakOnly && !strengthSelector.isWeak() && (itemExisting == null || itemExisting.isEmpty())){
                	 // There is a weak mapping on a property, but we do not have full account available, so skipping deletion of the value is better way
                    LOGGER.trace("Value {} mapping is weak and the full account could not be fetched, skipping deletion in {} with context {}",
							value, itemPath, contextDescription);
                    continue;
                }
                if (filterExistingValues && !hasValue(itemExisting, value)) {
                	LOGGER.trace("Value {} NOT add to delta as DELETE because item {} the item does not have that value in {} (matcher: {})",
							value, itemPath, contextDescription, valueMatcher);
                	continue;
                }
                LOGGER.trace("Value {} added to delta as DELETE for item {} in {}",
						value, itemPath, contextDescription);
                itemDelta.addValueToDelete((V)value.clone());
            }

            if (!zeroIvwos.isEmpty()) {
            	boolean weakOnly = true;
            	boolean hasStrong = false;
            	boolean hasAuthoritative = false;
            	// There may be several mappings that imply that value. So check them all for
                // exclusions and strength
                for (ItemValueWithOrigin<V,D> pvwo : zeroIvwos) {
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
	                    itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(value, isAssignment, zeroIvwos));
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
			Collection<? extends ItemValueWithOrigin<V,D>> nonNegativePvwos = ivwoTriple.getNonNegativeValues();
			Collection<ItemValueWithOrigin<V,D>> valuesToAdd = selectWeakValues(nonNegativePvwos, OriginType.ASSIGNMENTS);
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = selectWeakValues(nonNegativePvwos, OriginType.OUTBOUND);
			}
			if (valuesToAdd.isEmpty()) {
				valuesToAdd = selectWeakValues(nonNegativePvwos, null);
			}
			LOGGER.trace("No value for item {} in {}, weak mapping processing yielded values: {}",
					itemPath, contextDescription, valuesToAdd);
			for (ItemValueWithOrigin<V, D> valueWithOrigin : valuesToAdd) {
				itemDelta.addValueToAdd(LensUtil.cloneAndApplyMetadata(valueWithOrigin.getItemValue(), isAssignment, singleton(valueWithOrigin)));
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
	
	private Collection<V> collectAllValues() throws SchemaException {
		Collection<V> allValues = new HashSet<>();
		collectAllValuesFromSet(allValues, ivwoTriple.getZeroSet());
		collectAllValuesFromSet(allValues, ivwoTriple.getPlusSet());
		collectAllValuesFromSet(allValues, ivwoTriple.getMinusSet());
		return allValues;
	}
	
	private <T> void collectAllValuesFromSet(Collection<V> allValues,
            Collection<? extends ItemValueWithOrigin<V,D>> collection) throws SchemaException {
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
	
	private Collection<ItemValueWithOrigin<V,D>> collectIvwosFromSet(V pvalue, Collection<? extends ItemValueWithOrigin<V,D>> deltaSet, boolean keepValidInvalid) throws SchemaException {
    	Collection<ItemValueWithOrigin<V,D>> ivwos = new ArrayList<>();
        for (ItemValueWithOrigin<V,D> setIvwo : deltaSet) {
        	if (!shouldProcessMapping(setIvwo.getMapping().getStrength())) {
        		continue;
        	}
        	if (!setIvwo.isValid()) {
        		if (!keepValidInvalid) {
        			continue;
        		}
        		if (!setIvwo.wasValid()) {
        			continue;
        		}
        		// valid -> invalid change. E.g. disabled assignment. We need to process that
        	}
        	if (setIvwo.equalsRealValue(pvalue, valueMatcher)) {
        		ivwos.add(setIvwo);
        	}
        }
        return ivwos;
    }
	
	private boolean shouldProcessMapping(MappingStrengthType mappingStrength) {
		
		if (mappingStrength == MappingStrengthType.STRONG && !strengthSelector.isStrong()) {
			return false;
		}
		if (mappingStrength == MappingStrengthType.NORMAL && !strengthSelector.isNormal()) {
			return false;
		}
		if (mappingStrength == MappingStrengthType.WEAK && !strengthSelector.isWeak()) {
			return false;
		}
		
		return true;
	}
	
	private <V extends PrismValue, D extends ItemDefinition> Collection<ItemValueWithOrigin<V,D>> selectWeakValues(Collection<? extends ItemValueWithOrigin<V,D>> pvwos, OriginType origin) {
		Collection<ItemValueWithOrigin<V,D>> values = new ArrayList<>();
		for (ItemValueWithOrigin<V,D> pvwo: pvwos) {
			if (pvwo.getMapping().getStrength() == MappingStrengthType.WEAK && strengthSelector.isWeak()) {
				if (origin == null || origin == pvwo.getItemValue().getOriginType()) {
					values.add(pvwo);
				}
			}
		}
		return values;
	}
	
	private boolean hasValue(Item<V,D> item, ItemDelta<V,D> itemDelta) throws SchemaException {
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

	private boolean hasValue(Item<V,D> existingUserItem, V newValue) {
		if (existingUserItem == null) {
			return false;
		}
		if (valueMatcher != null && newValue instanceof PrismPropertyValue) {
			return valueMatcher.hasRealValue((PrismProperty)existingUserItem, (PrismPropertyValue)newValue);
		} else {
			return existingUserItem.contains(newValue, true, comparator);
		}
	}

}
