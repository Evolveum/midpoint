/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

@SuppressWarnings("unused")
public class AddRemoveAttributeValuesProcessor implements UpgradeObjectProcessor<ResourceType> {

    private static final ItemPath PATH_NATIVE = ItemPath.create(
            ResourceType.F_CAPABILITIES, CapabilitiesType.F_NATIVE, SchemaConstantsGenerated.R_CAP_ADD_REMOVE_ATTRIBUTE_VALUES);

    private static final ItemPath PATH_CONFIGURED = ItemPath.create(
            ResourceType.F_CAPABILITIES, CapabilitiesType.F_CONFIGURED, SchemaConstantsGenerated.R_CAP_ADD_REMOVE_ATTRIBUTE_VALUES);

    private static final List<ItemPath> PATHS = Arrays.asList(PATH_NATIVE, PATH_CONFIGURED);

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
        return matchObjectTypeAndPathTemplate(object, path, ResourceType.class, PATH_NATIVE)
                || matchObjectTypeAndPathTemplate(object, path, ResourceType.class, PATH_CONFIGURED);
    }

    @Override
    public boolean process(PrismObject<ResourceType> object, ItemPath path) {
        ItemPath collectionPath = path.allExceptLast();
        CapabilityCollectionType collection = object.findContainer(collectionPath).getRealValue(CapabilityCollectionType.class);

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
