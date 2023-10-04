/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

@SuppressWarnings("unused")
public class AddRemoveAttributeValuesProcessor implements UpgradeObjectProcessor<ResourceType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.NECESSARY;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, CapabilityCollectionType.class, SchemaConstantsGenerated.R_CAP_ADD_REMOVE_ATTRIBUTE_VALUES);
    }

    @Override
    public boolean process(PrismObject<ResourceType> object, ItemPath path) {
        CapabilityCollectionType collection = getItemParent(object, path);

        JAXBElement<AddRemoveAttributeValuesCapabilityType> addRemoveValuesElement =
                findElement(collection, SchemaConstantsGenerated.R_CAP_ADD_REMOVE_ATTRIBUTE_VALUES, AddRemoveAttributeValuesCapabilityType.class);

        AddRemoveAttributeValuesCapabilityType addRemoveValues = addRemoveValuesElement.getValue();
        if (addRemoveValues.isEnabled() == null) {
            collection.getAny().remove(addRemoveValuesElement);
            return true;
        }

        JAXBElement<UpdateCapabilityType> updateElement =
                findElement(collection, SchemaConstantsGenerated.R_CAP_UPDATE, UpdateCapabilityType.class);
        if (updateElement == null) {
            updateElement = new JAXBElement<>(SchemaConstantsGenerated.R_CAP_UPDATE, UpdateCapabilityType.class, new UpdateCapabilityType());
            collection.getAny().add(updateElement);
        }

        updateElement.getValue().setAddRemoveAttributeValues(addRemoveValues.isEnabled());

        collection.getAny().remove(addRemoveValuesElement);

        return true;
    }

    private <T> JAXBElement<T> findElement(CapabilityCollectionType collection, QName name, Class<T> type) {
        return (JAXBElement<T>) collection.getAny().stream()
                .filter(e -> {
                    JAXBElement element = (JAXBElement) e;
                    return name.equals(element.getName());
                })
                .findFirst()
                .orElse(null);
    }
}
