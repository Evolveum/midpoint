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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;

import javax.xml.namespace.QName;

@SuppressWarnings("unused")
public class DefaultObjectTypeProcessor implements UpgradeObjectProcessor<ObjectType> {

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
        return matchParentTypeAndItemName(object, path, SearchBoxConfigurationType.class, SearchBoxConfigurationType.F_DEFAULT_OBJECT_TYPE);
    }

    @Override
    public boolean process(PrismObject<ObjectType> object, ItemPath path) throws Exception {
        SearchBoxConfigurationType config = getItemParent(object, path);
        QName type = config.getDefaultObjectType();

        ObjectTypeSearchItemConfigurationType typeConfig = config.getObjectTypeConfiguration();
        if (typeConfig == null) {
            typeConfig = new ObjectTypeSearchItemConfigurationType();
            config.setObjectTypeConfiguration(typeConfig);
        }

        if (typeConfig.getDefaultValue() == null) {
            typeConfig.setDefaultValue(type);
        }
        config.setDefaultObjectType(null);

        return true;
    }
}
