/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@SuppressWarnings("unused")
public class ReconcileAffectedProcessor implements UpgradeObjectProcessor<ObjectType> {

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
        Item item = object.findItem(path);
        if (item == null) {
            return false;
        }

        PrismContainerValue value = item.getParent();
        if (!(value.getRealValue() instanceof ModelExecuteOptionsType)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        Item item = object.findItem(path);
        if (item == null) {
            return false;
        }

        PrismContainerValue<ModelExecuteOptionsType> value = item.getParent();

        ModelExecuteOptionsType options = value.getRealValue();
        options.setReconcileAffected(null);

        if (value.isEmpty()) {
            PrismContainer modelExecuteOptions = (PrismContainer) value.getParent();
            if (modelExecuteOptions != null) {
                modelExecuteOptions.getParent().remove(modelExecuteOptions);
            }
        }

        return true;
    }
}
