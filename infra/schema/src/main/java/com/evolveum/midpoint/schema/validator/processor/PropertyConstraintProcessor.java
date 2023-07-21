/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
public class PropertyConstraintProcessor implements UpgradeObjectProcessor<ObjectType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.CRITICAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.SEAMLESS;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchObjectTypeAndPathTemplate(
                object, path, ArchetypeType.class, ItemPath.create(
                        ArchetypeType.F_ARCHETYPE_POLICY,
                        ArchetypePolicyType.F_PROPERTY_CONSTRAINT))
                || matchObjectTypeAndPathTemplate(
                object, path, SystemConfigurationType.class, ItemPath.create(
                        SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                        ObjectPolicyConfigurationType.F_PROPERTY_CONSTRAINT));
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        PrismContainer<?> container = object.findContainer(path);

        ItemPath itemConstraint = ItemPath.create(path.allExceptLast(), ArchetypePolicyType.F_ITEM_CONSTRAINT);
        PrismContainer<?> newContainer = object.findOrCreateContainer(itemConstraint);

        for (PrismContainerValue<?> value : container.getValues()) {
            newContainer.getValues().add((PrismContainerValue) value.clone());
        }

        container.getParent().remove(container);

        return true;
    }
}
