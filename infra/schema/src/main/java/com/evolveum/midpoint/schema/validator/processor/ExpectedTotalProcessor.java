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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@SuppressWarnings("unused")
public class ExpectedTotalProcessor implements UpgradeObjectProcessor<TaskType> {

    @Override
    public UpgradePhase getPhase() {
        return UpgradePhase.BEFORE;
    }

    @Override
    public UpgradePriority getPriority() {
        return UpgradePriority.OPTIONAL;
    }

    @Override
    public UpgradeType getType() {
        return UpgradeType.MANUAL;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, TaskType.class, TaskType.F_EXPECTED_TOTAL);
    }

    @Override
    public String upgradeDescription(PrismObject<TaskType> object, ItemPath path) {
        return "No action needed currently. It will not be removed for now, since not all tasks were migrated to activities yet.";
    }

    @Override
    public boolean process(PrismObject<TaskType> object, ItemPath path) throws Exception {
        return false;
    }
}
