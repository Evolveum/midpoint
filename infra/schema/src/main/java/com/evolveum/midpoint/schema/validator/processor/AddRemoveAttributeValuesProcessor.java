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
    public String getIdentifier() {
        return "AddRemoveAttributeValues";
    }

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
        if (!ResourceType.class.equals(object.getCompileTimeClass())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean process(PrismObject<ResourceType> object) {
        ResourceType resource = object.asObjectable();
        CapabilitiesType capabilities = resource.getCapabilities();
        if (capabilities == null) {
            return false;
        }

        boolean changed = process(capabilities.getConfigured());

        return process(capabilities.getNative()) || changed;
    }

    private boolean process(CapabilityCollectionType collection) {
        AddRemoveAttributeValuesCapabilityType addRemoveValues = collection.getAddRemoveAttributeValues();
        if (addRemoveValues == null || addRemoveValues.isEnabled() == null) {
            return false;
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
