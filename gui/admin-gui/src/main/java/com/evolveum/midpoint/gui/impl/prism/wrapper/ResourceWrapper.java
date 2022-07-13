/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author skublik
 */
public class ResourceWrapper extends PrismObjectWrapperImpl<ResourceType> {

    private static final long serialVersionUID = 1L;

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
                removingMetadataForSuperOrigin(deltas);
                objectDelta.mergeModifications(deltas);
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

    private void removingMetadataForSuperOrigin(Collection<ItemDelta<PrismValue, ItemDefinition>> deltas) {
        deltas.forEach(delta -> {
            removingMetadataFromValues(delta.getValuesToAdd());
            removingMetadataFromValues(delta.getValuesToReplace());
            removingMetadataFromValues(delta.getValuesToDelete());
        });
    }

    private void removingMetadataFromValues(Collection<PrismValue> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> {
            if (hasValueMetadata(value)) {
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
            if (hasValueMetadata(((PrismValue) value))) {
                return true;
            }
            containsValueWithoutMetadata.set(true);
            return false;
        });
        return containsValueWithoutMetadata.get();
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
