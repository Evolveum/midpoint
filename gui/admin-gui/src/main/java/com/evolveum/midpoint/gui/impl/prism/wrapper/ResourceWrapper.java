/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author skublik
 */
public class ResourceWrapper extends PrismObjectWrapperImpl<ResourceType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ResourceWrapper.class);

    private static final Map<Class<?>, List<ItemName>> MERGE_IDENTIFIERS;

    static {
        MERGE_IDENTIFIERS = ImmutableMap.<Class<?>, List<ItemName>>builder()
                .put(ConnectorInstanceSpecificationType.class, Arrays.asList(
                        ConnectorInstanceSpecificationType.F_NAME))
                .put(ResourceObjectTypeDefinitionType.class, Arrays.asList(
                        ResourceObjectTypeDefinitionType.F_KIND,
                        ResourceObjectTypeDefinitionType.F_INTENT))
                .put(ResourceItemDefinitionType.class, Arrays.asList(
                        ResourceItemDefinitionType.F_REF))
                .put(PropertyLimitationsType.class, Arrays.asList(
                        PropertyLimitationsType.F_LAYER))
                .put(MappingType.class, Arrays.asList(
                        MappingType.F_NAME))
                .put(AbstractCorrelatorType.class, Arrays.asList(
                        AbstractCorrelatorType.F_NAME))
                .put(SynchronizationReactionType.class, Arrays.asList(
                        SynchronizationReactionType.F_NAME))
                .put(AbstractSynchronizationActionType.class, Arrays.asList(
                        AbstractSynchronizationActionType.F_NAME))
                .put(ResourceAttributeDefinitionType.class, Arrays.asList(
                        ResourceAttributeDefinitionType.F_REF))
                .build();
    }

    public ResourceWrapper(PrismObject<ResourceType> item, ItemStatus status) {
        super(item, status);
    }

    @Override
    public ObjectDelta<ResourceType> getObjectDelta() throws SchemaException {
        ObjectDelta<ResourceType> objectDelta = getPrismContext().deltaFor(getObject().getCompileTimeClass())
                .asObjectDelta(getObject().getOid());

        Collection<ItemDelta<PrismValue, ItemDefinition>> deltas = new ArrayList<>();
        for (ItemWrapper<?, ?> itemWrapper : getValue().getItems()) {
            Collection<ItemDelta<PrismValue, ItemDefinition>> delta = itemWrapper.getDelta();
            if (delta == null || delta.isEmpty()) {
                continue;
            }
            deltas.addAll(delta);
        }

        switch (getStatus()) {
            case ADDED:
                objectDelta.setChangeType(ChangeType.ADD);
                PrismObject<ResourceType> clone = (PrismObject<ResourceType>) getOldItem().clone();
                removingMetadataForSuperOrigin(clone);
                removingMetadataForSuperOrigin(deltas);
                for (ItemDelta d : deltas) {
                    d.applyTo(clone);
                }
                objectDelta.setObjectToAdd(clone);
                break;
            case NOT_CHANGED:
                objectDelta.mergeModifications(processModifyDeltas(deltas));
                break;
            case DELETED:
                objectDelta.setChangeType(ChangeType.DELETE);
                break;
        }

        if (!getShadowDeltas().isEmpty()) {
            objectDelta.addModifications(getShadowDeltas());
        }

        return objectDelta;
    }

    private Collection<ItemDelta<PrismValue, ItemDefinition>> processModifyDeltas(
            Collection<ItemDelta<PrismValue, ItemDefinition>> deltas) throws SchemaException {

        Collection<ItemDelta<PrismValue, ItemDefinition>> processedDeltas = new ArrayList<>();

        for (ItemDelta<PrismValue, ItemDefinition> delta : deltas) {
            if (delta.isDelete()) {
                List<PrismValue> valuesFromTemplate = new ArrayList<>();
                delta.getValuesToDelete().forEach(v -> {
                    if (isFromTemplate(v) || isFromTemplate(v.getParentContainerValue())) {
                        LOGGER.warn("Couldn't remove value, because is merged from resource template, value: " + v);
                        valuesFromTemplate.add(v);
                    }
                });
                delta.getValuesToDelete().removeAll(valuesFromTemplate);
                if (delta.isDelete()) {
                    ItemDelta<PrismValue, ItemDefinition> deleteDelta = delta.clone();
                    deleteDelta.clearValuesToAdd();
                    deleteDelta.clearValuesToReplace();
                    processedDeltas.add(deleteDelta);
                }
                if (!delta.isAdd() && !delta.isReplace()) {
                    continue;
                }
            }
            if (delta.getParentPath() == null && delta.getParentPath().isEmpty()) {
                processedDeltas.add(delta);
                continue;
            }

            Collection<PrismValue> processedValues = new ArrayList<>();
            if (delta.isAdd()) {
                processedValues.addAll(delta.getValuesToAdd());
            } else {
                processedValues.addAll(delta.getValuesToReplace());
            }

            Item<PrismValue, ItemDefinition> parentItem = getItem().findItem(delta.getPath());

            PrismContainerValue<?> parentContainerValue = parentItem.getParent();

            if (!isFromTemplate(parentContainerValue)) {
                processedDeltas.add(delta);
                continue;
            }

            PrismContainerValue foundValue = null;
            for (ItemDelta<PrismValue, ItemDefinition> processedDelta : processedDeltas) {
                if (processedDelta.isAdd()
                        && processedDelta.getValuesToAdd().iterator().next() instanceof PrismContainerValue
                        && processedDelta.getPath().isSubPath(delta.getPath())) {
                    for (PrismValue prismValue : processedDelta.getValuesToAdd()) {
                        if (prismValue.getPath().isSubPath(delta.getPath())) {
                            foundValue = (PrismContainerValue) prismValue;
                        }
                    }
                }
            }
            ItemDelta<PrismValue, ItemDefinition> newDelta =
                    processAddOrModifyDelta(parentContainerValue, processedValues, foundValue);
            if (newDelta != null) {
                processedDeltas.add(newDelta);
            }
        }
        return processedDeltas;
    }

    private ItemDelta<PrismValue, ItemDefinition> processAddOrModifyDelta(
            PrismContainerValue<?> parentContainerValue, Collection<PrismValue> processedValues, PrismContainerValue<?> valueOfExistingDelta) throws SchemaException {
        PrismValue value = processedValues.iterator().next();
        Itemable parent = value.getParent();
        Item newItem = parent.getDefinition().instantiate();
        for (PrismValue prismValue : processedValues) {
            newItem.add(prismValue.clone());
        }
        PrismContainerValue<?> newValue = createParentValueForAddDelta(parentContainerValue, newItem, valueOfExistingDelta);
        if (valueOfExistingDelta == null) {
            removingMetadataFromValues(Collections.singleton(newValue));
            ItemDelta newDelta = newValue.getDefinition().createEmptyDelta(newValue.getParent().getPath());
            newValue.setParent(null);
            newDelta.addValueToAdd(newValue);
            return newDelta;
        }
        return null;
    }

    private PrismContainerValue<?> createParentValueForAddDelta(
            PrismContainerValue<?> origParentValue, Item newChildItem, PrismContainerValue<?> valueOfExistingDelta) throws SchemaException {
        PrismContainerValue<?> parentValue = origParentValue.getParentContainerValue();

        PrismContainer newContainer = null;
        PrismContainerValue newValue = null;

        boolean isItemFound = false;
        if (valueOfExistingDelta != null) {
            ItemPath subPath = origParentValue.getPath().rest(valueOfExistingDelta.getPath().size());
            if (valueOfExistingDelta.find(subPath) != null) {
                isItemFound = true;
                newContainer = (PrismContainer) valueOfExistingDelta.find(subPath);
                newValue = newContainer.getValue(origParentValue.getId());
            }
        }

        if (newContainer == null) {
            newContainer = origParentValue.getDefinition().instantiate();
        }

        if (newValue == null) {
            newValue = newContainer.createNewValue();
            newValue.setId(origParentValue.getId());
            Class<?> typeClass = newValue.getComplexTypeDefinition().getTypeClass();
            if (typeClass == null) {
                typeClass = WebComponentUtil.qnameToClass(PrismContext.get(), newValue.getComplexTypeDefinition().getTypeName());
            }
            if (MERGE_IDENTIFIERS.containsKey(typeClass)) {
                for (ItemName path : MERGE_IDENTIFIERS.get(typeClass)) {
                    Item<PrismValue, ItemDefinition> item = origParentValue.findItem(path);
                    if (item != null && !item.isEmpty() && item.valuesStream().anyMatch(v -> !v.isEmpty())) {
                        Item newItem = newValue.findOrCreateItem(path);
                        for (PrismValue value : item.getValues()) {
                            if (!value.isEmpty()) {
                                newItem.add(value.clone());
                            }
                        }
                    }
                }
            }
        }

        if (!newValue.contains(newChildItem.getElementName())) {
            newChildItem.setParent(newValue);
            newValue.add(newChildItem);
        }

        if (valueOfExistingDelta != null && isItemFound) {
            removingMetadataFromValues(Collections.singleton(newValue));
            return valueOfExistingDelta;
        }

        if (isFromTemplate(parentValue)) {
            return createParentValueForAddDelta(parentValue, newContainer, valueOfExistingDelta);
        }

        newContainer.setParent(parentValue);
        return newValue;
    }

    private void removingMetadataForSuperOrigin(Collection<ItemDelta<PrismValue, ItemDefinition>> deltas) {
        deltas.forEach(delta -> removingMetadataForSuperOrigin(delta));
    }

    private void removingMetadataForSuperOrigin(ItemDelta<PrismValue, ItemDefinition> delta) {
        removingMetadataFromValues(delta.getValuesToAdd());
        removingMetadataFromValues(delta.getValuesToReplace());
        removingMetadataFromValues(delta.getValuesToDelete());
    }

    private void removingMetadataFromValues(Collection<PrismValue> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> {
            if (isFromTemplate(value)) {
                value.setValueMetadata((ValueMetadata) null);
            }
        });
    }

    private void removingMetadataForSuperOrigin(PrismObject<ResourceType> clone) {
        clone.getDefinition().getDefinitions().forEach(def -> {
            Item<PrismValue, ItemDefinition> item = clone.findItem(def.getItemName());
            removingMetadataForSuperOrigin(item);
            if (item != null && item.isEmpty()) {
                clone.remove(item);
            }
        });
    }

    private boolean removingMetadataForSuperOrigin(PrismContainer container) {
        AtomicBoolean containsValueWithoutMetadata = new AtomicBoolean(false);
        container.removeIf(value -> {
            AtomicBoolean containsValueWithoutMetadataForValue = new AtomicBoolean(false);
            container.getDefinition().getDefinitions().forEach(def -> {
                Item item = (Item) ((PrismValue) value).find(((ItemDefinition) def).getItemName());
                boolean containsValueWithoutMetadataForItem = removingMetadataForSuperOrigin(item);
                if (item.isEmpty()) {
                    ((PrismContainerValue) value).remove(item);
                }
                containsValueWithoutMetadataForValue.set(
                        containsValueWithoutMetadataForValue.get() || containsValueWithoutMetadataForItem);
            });
            containsValueWithoutMetadata.set(
                    containsValueWithoutMetadata.get() || containsValueWithoutMetadataForValue.get());
            return !containsValueWithoutMetadataForValue.get();
        });
        return containsValueWithoutMetadata.get();
    }

    private boolean removingMetadataForSuperOrigin(Item item) {
        AtomicBoolean containsValueWithoutMetadata = new AtomicBoolean(false);
        if (item == null) {
            return false;
        }
        item.removeIf(value -> {
            if (isFromTemplate(((PrismValue) value))) {
                return true;
            }
            containsValueWithoutMetadata.set(true);
            return false;
        });
        return containsValueWithoutMetadata.get();
    }

    private boolean isFromTemplate(PrismValue valueFromDelta) {
        if (valueFromDelta instanceof PrismObjectValue) {
            return false;
        }

        if (hasValueMetadata(valueFromDelta)) {
            return true;
        }
        Item<PrismValue, ItemDefinition> item = getItem().findItem(valueFromDelta.getParent().getPath());
        PrismContainerValue<?> value = item.getParent();
        while (!(value instanceof PrismObjectValue)) {
            if (hasValueMetadata(value)) {
                return true;
            }
            value = value.getParentContainerValue();
        }
        return false;
    }

    private boolean hasValueMetadata(PrismValue value) {
        if (value.hasValueMetadata()) {
            List<PrismContainerValue<Containerable>> metadataValues = value.getValueMetadata().getValues();

            if (metadataValues.size() == 1) {
                ProvenanceMetadataType provenance = ((ValueMetadataType) metadataValues.get(0).asContainerable()).getProvenance();
                if (provenance != null) {
                    List<ProvenanceAcquisitionType> acquisitionValues = provenance.getAcquisition();
                    if (acquisitionValues.size() == 1) {
                        ObjectReferenceType originRef = acquisitionValues.get(0).getOriginRef();
                        return originRef != null && StringUtils.isNotEmpty(originRef.getOid());
                    }
                }
            }
        }
        return false;
    }

    public Collection<ItemDelta> getDeltas() throws SchemaException {
        Collection<ItemDelta> deltas = new ArrayList<>();
        for (ItemWrapper<?, ?> itemWrapper : getValue().getItems()) {
            Collection<ItemDelta> delta = itemWrapper.getDelta();
            if (delta == null || delta.isEmpty()) {
                continue;
            }
            deltas.addAll(delta);
        }
        return deltas;
    }
}
