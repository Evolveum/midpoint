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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

// todo tests
@SuppressWarnings("unused")
public class RoleManagementDefaultCollectionProcessor implements UpgradeObjectProcessor<SystemConfigurationType> {

    @Override
    public UpgradePhase getPhase() {
        // todo before in 4.7.* but after in 4.4.*
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
        return matchesTypeAndHasPathItem(object, path, SystemConfigurationType.class);
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) {
        // todo implement
        return false;
    }
}
