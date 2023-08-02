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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;

@SuppressWarnings("unused")
public class ResourceSynchronizationProcessor implements UpgradeObjectProcessor<ResourceType> {

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
        return matchParentTypeAndItemName(object, path, SynchronizationType.class, SynchronizationType.F_OBJECT_SYNCHRONIZATION)
                || matchParentTypeAndItemName(object, path, ObjectSynchronizationType.class, ObjectSynchronizationType.F_REACTION)
                || matchParentTypeAndItemName(object, path, ResourceObjectTypeDefinitionType.class, ResourceObjectTypeDefinitionType.F_AUXILIARY_OBJECT_CLASS)
                || matchParentTypeAndItemName(object, path, ResourceObjectTypeDefinitionType.class, ResourceObjectTypeDefinitionType.F_BASE_CONTEXT)
                || matchParentTypeAndItemName(object, path, ResourceObjectTypeDefinitionType.class, ResourceObjectTypeDefinitionType.F_SEARCH_HIERARCHY_SCOPE);
    }

    @Override
    public boolean process(PrismObject<ResourceType> object, ItemPath path) throws Exception {
        return false;
    }

    @Override
    public String upgradeDescription(PrismObject<ResourceType> object, ItemPath path) {
        return "In 4.6, we improved the style of configuration of the resource objects synchronization."
                + "To migrate, use the new format. See the documentation for more information - Resource Schema Handling and Synchronization.";
    }
}
