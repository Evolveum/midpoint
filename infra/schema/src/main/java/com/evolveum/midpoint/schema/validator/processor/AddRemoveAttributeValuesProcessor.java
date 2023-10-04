/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AddRemoveAttributeValuesCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
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
        return matchParentTypeAndItemName(object, path, CapabilityCollectionType.class, CapabilityCollectionType.F_ADD_REMOVE_ATTRIBUTE_VALUES);
    }

    @Override
    public boolean process(PrismObject<ResourceType> object, ItemPath path) {
        CapabilityCollectionType collection = getItemParent(object, path);

        AddRemoveAttributeValuesCapabilityType addRemoveValues = collection.getAddRemoveAttributeValues();
        collection.setAddRemoveAttributeValues(null);
        if (addRemoveValues.isEnabled() == null) {
            return true;
        }

        UpdateCapabilityType update = collection.getUpdate();
        if (update == null) {
            update = new UpdateCapabilityType();
            collection.setUpdate(update);
        }

        update.setAddRemoveAttributeValues(addRemoveValues.isEnabled());

        return true;
    }
}
