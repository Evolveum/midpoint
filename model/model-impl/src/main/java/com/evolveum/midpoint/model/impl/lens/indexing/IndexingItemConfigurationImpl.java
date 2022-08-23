/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.indexing;

import com.evolveum.midpoint.model.api.indexing.IndexedItemValueNormalizer;
import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IndexedItemNormalizationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemIndexingDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IndexingItemConfigurationImpl implements Serializable, IndexingItemConfiguration {

    @NotNull private final String name;

    /** Beware, the path segments may be unqualified! */
    @NotNull private final ItemPath path;

    // contain at least one element
    @NotNull private final Collection<IndexedItemValueNormalizer> normalizers;

    private IndexingItemConfigurationImpl(
            @NotNull String name,
            @NotNull ItemPath path,
            @NotNull Collection<IndexedItemValueNormalizer> normalizers) {
        this.name = name;
        this.path = path;
        this.normalizers = normalizers;
        assert !normalizers.isEmpty();
    }

    @NotNull public static IndexingItemConfiguration of(
            @NotNull ItemRefinedDefinitionType itemDefBean,
            @NotNull ItemIndexingDefinitionType indexingDefBean)
            throws ConfigurationException {
        ItemPath path = MiscUtil.configNonNull(
                        itemDefBean.getRef(),
                        () -> "No 'ref' in " + itemDefBean)
                .getItemPath();
        String explicitName = indexingDefBean.getIndexedItemName();
        String indexedItemName = explicitName != null ? explicitName : deriveName(path, itemDefBean);
        return new IndexingItemConfigurationImpl(
                indexedItemName,
                path,
                createNormalizers(indexedItemName, indexingDefBean));
    }

    private static Collection<IndexedItemValueNormalizer> createNormalizers(
            @NotNull String indexedItemName,
            @NotNull ItemIndexingDefinitionType indexingDefBean) {
        List<IndexedItemValueNormalizer> normalizations = new ArrayList<>();
        for (IndexedItemNormalizationDefinitionType normalizationBean : indexingDefBean.getNormalization()) {
            normalizations.add(
                    IndexedItemValueNormalizerImpl.create(indexedItemName, normalizationBean));
        }
        if (normalizations.isEmpty()) {
            normalizations.add(
                    IndexedItemValueNormalizerImpl.create(
                            indexedItemName, new IndexedItemNormalizationDefinitionType()._default(true)));
        }
        return normalizations;
    }

    private static @NotNull String deriveName(ItemPath path, ItemRefinedDefinitionType itemDefBean)
            throws ConfigurationException {
        return MiscUtil.configNonNull(
                path.lastName(),
                () -> "No name in path '" + path + "' in " + itemDefBean).getLocalPart();
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull ItemName getQualifiedName() {
        return new ItemName(SchemaConstants.NS_IDENTITY, getName());
    }

    @Override
    public @NotNull ItemPath getPath() {
        return path;
    }

    @Override
    public @NotNull Collection<IndexedItemValueNormalizer> getNormalizers() {
        return normalizers;
    }

    @Override
    public IndexedItemValueNormalizer findNormalizer(@Nullable String index) throws ConfigurationException {
        if (index == null) {
            return getDefaultNormalizer();
        } else {
            List<IndexedItemValueNormalizer> matching = normalizers.stream()
                    .filter(n -> n.getName().equals(index))
                    .collect(Collectors.toList());
            return MiscUtil.extractSingleton(
                    matching,
                    () -> new ConfigurationException(
                            String.format("Multiple normalizations named '%s': %s", index, matching)));
        }
    }

    @Override
    public IndexedItemValueNormalizer getDefaultNormalizer() throws ConfigurationException {
        if (normalizers.size() == 1) {
            return normalizers.iterator().next();
        } else {
            List<IndexedItemValueNormalizer> matching = normalizers.stream()
                    .filter(IndexedItemValueNormalizer::isDefault)
                    .collect(Collectors.toList());
            return MiscUtil.extractSingleton(
                    matching,
                    () -> new ConfigurationException(
                            String.format("Multiple default normalizations: %s", matching)));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                ", path=" + path +
                '}';
    }
}
