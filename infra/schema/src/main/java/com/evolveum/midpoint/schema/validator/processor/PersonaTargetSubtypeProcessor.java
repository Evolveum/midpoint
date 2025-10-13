/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.UpgradeObjectProcessor;
import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

@SuppressWarnings("unused")
public class PersonaTargetSubtypeProcessor implements UpgradeObjectProcessor<ObjectType> {

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
        return UpgradeType.MANUAL;
    }

    @Override
    public boolean isApplicable(PrismObject<?> object, ItemPath path) {
        if (!PersonaConstructionType.F_TARGET_SUBTYPE.equivalent(path.lastName())) {
            return false;
        }

        ItemPath allExceptLast = path.allExceptLast();
        Item<?, ?> item = object.findItem(allExceptLast);

        return item.getRealValue() instanceof PersonaConstructionType;
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) {
        return false;
    }

    @Override
    public String upgradeDescription(PrismObject<ObjectType> object, ItemPath path) {
        return "Matching of personas by subtype values is not supported anymore. Any such use should be migrated to the use of archetypes.";
    }
}
