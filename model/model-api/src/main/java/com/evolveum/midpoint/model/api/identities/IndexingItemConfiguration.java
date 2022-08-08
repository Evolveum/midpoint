/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.identities;

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

public class IndexingItemConfiguration implements Serializable {

    @NotNull private final String name;

    /** Beware, the path segments may be unqualified! */
    @NotNull private final ItemPath path;

    // contain at least one element
    @NotNull private final Collection<Normalization> normalizations;

    private IndexingItemConfiguration(
            @NotNull String name,
            @NotNull ItemPath path,
            @NotNull Collection<Normalization> normalizations) {
        this.name = name;
        this.path = path;
        this.normalizations = normalizations;
        assert !normalizations.isEmpty();
    }

    @NotNull public static IndexingItemConfiguration of(
            @NotNull ItemRefinedDefinitionType itemDefBean,
            @NotNull ItemIndexingDefinitionType indexingDefBean) throws ConfigurationException {
        ItemPath path = MiscUtil.configNonNull(
                        itemDefBean.getRef(),
                        () -> "No 'ref' in " + itemDefBean)
                .getItemPath();
        String explicitName = indexingDefBean.getIndexedItemName();
        String indexedItemName = explicitName != null ? explicitName : deriveName(path, itemDefBean);
        Collection<Normalization> normalizations = createNormalizations(indexedItemName, indexingDefBean);
        return new IndexingItemConfiguration(indexedItemName, path, normalizations);
    }

    private static Collection<Normalization> createNormalizations(
            @NotNull String indexedItemName, @NotNull ItemIndexingDefinitionType indexingDefBean) {
        List<Normalization> normalizations = new ArrayList<>();
        for (IndexedItemNormalizationDefinitionType normalizationBean : indexingDefBean.getNormalization()) {
            normalizations.add(
                    Normalization.create(indexedItemName, normalizationBean));
        }
        if (normalizations.isEmpty()) {
            normalizations.add(
                    Normalization.create(indexedItemName, new IndexedItemNormalizationDefinitionType()._default(true)));
        }
        return normalizations;
    }

    private static @NotNull String deriveName(ItemPath path, ItemRefinedDefinitionType itemDefBean)
            throws ConfigurationException {
        return MiscUtil.configNonNull(
                path.lastName(),
                () -> "No name in path '" + path + "' in " + itemDefBean).getLocalPart();
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull ItemName getQualifiedName() {
        return new ItemName(SchemaConstants.NS_IDENTITY, getName());
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    public @NotNull Collection<Normalization> getNormalizations() {
        return normalizations;
    }

    public Normalization findNormalization(@Nullable String index) throws ConfigurationException {
        if (index == null) {
            return getDefaultNormalization();
        } else {
            List<Normalization> matching = normalizations.stream()
                    .filter(n -> n.getName().equals(index))
                    .collect(Collectors.toList());
            return MiscUtil.extractSingleton(
                    matching,
                    () -> new ConfigurationException(
                            String.format("Multiple normalizations named '%s': %s", index, matching)));
        }
    }

    private Normalization getDefaultNormalization() throws ConfigurationException {
        if (normalizations.size() == 1) {
            return normalizations.iterator().next();
        } else {
            List<Normalization> matching = normalizations.stream()
                    .filter(Normalization::isDefault)
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
