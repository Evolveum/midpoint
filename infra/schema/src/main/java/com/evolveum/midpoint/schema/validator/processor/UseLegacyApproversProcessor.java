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
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

@SuppressWarnings("unused")
public class UseLegacyApproversProcessor implements UpgradeObjectProcessor<SystemConfigurationType> {

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
        return matchParentTypeAndItemName(object, path, WfConfigurationType.class,
                WfConfigurationType.F_USE_LEGACY_APPROVERS_SPECIFICATION);
    }

    @Override
    public boolean process(PrismObject<SystemConfigurationType> object, ItemPath path) throws Exception {
        WfConfigurationType wfConfiguration = getItemParent(object, path);
        if (wfConfiguration == null) {
            return false;
        }

        wfConfiguration.setUseLegacyApproversSpecification(null);

        wfConfiguration.asPrismContainerValue();
        if (wfConfiguration.asPrismContainerValue().isEmpty()) {
            object.asObjectable().setWorkflowConfiguration(null);
        }

        return true;
    }
}
