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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@SuppressWarnings("unused")
public class ObjectFormsProcessor implements UpgradeObjectProcessor<ObjectType> {

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
        return UpgradeType.MANUAL;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        return matchParentTypeAndItemName(object, path, AdminGuiConfigurationType.class, AdminGuiConfigurationType.F_OBJECT_FORMS);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        return false;
    }

    @Override
    public String upgradeDescription(PrismObject<ObjectType> object, ItemPath path) {
        return "Forms has to be converted to panels.";  // todo maybe link to doc?
    }
}
