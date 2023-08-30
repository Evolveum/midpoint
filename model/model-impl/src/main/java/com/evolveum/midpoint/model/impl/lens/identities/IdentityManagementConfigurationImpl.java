/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiSourceItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Wraps all the configuration related to management of `identities` container, correlation, and so on.
 *
 * PRELIMINARY VERSION - e.g. no support for object template inclusion, etc
 */
public class IdentityManagementConfigurationImpl implements IdentityManagementConfiguration {

    @NotNull private final ObjectTemplateType objectTemplate;
    @NotNull private final PathKeyedMap<IdentityItemConfiguration> itemsMap;

    private IdentityManagementConfigurationImpl(ObjectTemplateType objectTemplate) throws ConfigurationException {
        this.objectTemplate = objectTemplate != null ? objectTemplate : new ObjectTemplateType();
        this.itemsMap = extractItemsConfiguration(this.objectTemplate);
    }

    private static PathKeyedMap<IdentityItemConfiguration> extractItemsConfiguration(@NotNull ObjectTemplateType objectTemplate)
            throws ConfigurationException {
        PathKeyedMap<IdentityItemConfiguration> itemConfigurationMap = new PathKeyedMap<>();
        for (ObjectTemplateItemDefinitionType itemDefBean : objectTemplate.getItem()) {
            MultiSourceItemDefinitionType multiSourceDefBean = itemDefBean.getMultiSource();
            if (multiSourceDefBean != null) {
                IdentityItemConfiguration configuration = IdentityItemConfigurationImpl.of(itemDefBean, multiSourceDefBean);
                itemConfigurationMap.put(configuration.getPath(), configuration);
            }
        }
        return itemConfigurationMap;
    }

    public static @NotNull IdentityManagementConfiguration of(@Nullable ObjectTemplateType objectTemplate)
            throws ConfigurationException {
        return new IdentityManagementConfigurationImpl(objectTemplate);
    }

    @Override
    public @NotNull Collection<IdentityItemConfiguration> getItems() {
        return itemsMap.values();
    }

    @Override
    public @Nullable IdentityItemConfiguration getForPath(@NotNull ItemPath path) {
        return itemsMap.get(path);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "objectTemplate=" + objectTemplate +
                ", items: " + itemsMap.keySet() +
                '}';
    }
}
