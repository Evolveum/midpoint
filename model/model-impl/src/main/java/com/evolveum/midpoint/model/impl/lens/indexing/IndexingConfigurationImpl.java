/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.indexing;

import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemIndexingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * TODO
 *
 * PRELIMINARY VERSION - e.g. no support for object template inclusion, etc
 */
public class IndexingConfigurationImpl implements IndexingConfiguration {

    @NotNull private final ObjectTemplateType objectTemplate;
    @NotNull private final PathKeyedMap<IndexingItemConfiguration> itemsMap;

    private IndexingConfigurationImpl(ObjectTemplateType objectTemplate) throws ConfigurationException {
        this.objectTemplate = objectTemplate != null ? objectTemplate : new ObjectTemplateType();
        this.itemsMap = extractItemsConfiguration(this.objectTemplate);
    }

    public static @NotNull IndexingConfiguration of(@Nullable ObjectTemplateType objectTemplate) throws ConfigurationException {
        return new IndexingConfigurationImpl(objectTemplate);
    }

    private static PathKeyedMap<IndexingItemConfiguration> extractItemsConfiguration(
            @NotNull ObjectTemplateType objectTemplate)
            throws ConfigurationException {
        PathKeyedMap<IndexingItemConfiguration> itemConfigurationMap = new PathKeyedMap<>();
        for (ObjectTemplateItemDefinitionType itemDefBean : objectTemplate.getItem()) {
            ItemIndexingDefinitionType itemIndexingDefBean = itemDefBean.getIndexing();
            IndexingItemConfiguration itemConfiguration;
            if (itemIndexingDefBean != null) {
                itemConfiguration = IndexingItemConfigurationImpl.of(itemDefBean, itemIndexingDefBean);
            } else if (itemDefBean.getMultiSource() != null) {
                // "Multi-source" items are indexed by default (TODO how can that be turned off?)
                itemConfiguration = IndexingItemConfigurationImpl.of(itemDefBean, new ItemIndexingDefinitionType());
            } else {
                continue;
            }
            itemConfigurationMap.put(itemConfiguration.getPath(), itemConfiguration);
        }
        return itemConfigurationMap;
    }

    @Override
    public @NotNull Collection<IndexingItemConfiguration> getItems() throws ConfigurationException {
        return itemsMap.values();
    }

    @Override
    public @Nullable IndexingItemConfiguration getForPath(@NotNull ItemPath path) {
        return itemsMap.get(path);
    }

    // TODO improve --- TODO what if empty config is legal?
    @Override
    public boolean hasNoItems() {
        return itemsMap.values().isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "objectTemplate=" + objectTemplate +
                ", items: " + itemsMap.keySet() +
                '}';
    }
}
