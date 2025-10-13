/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlation;

import com.evolveum.midpoint.model.api.correlation.TemplateCorrelationConfiguration;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;

import com.evolveum.midpoint.model.impl.lens.identities.IdentityManagementConfigurationImpl;
import com.evolveum.midpoint.model.impl.lens.indexing.IndexingConfigurationImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import com.evolveum.midpoint.schema.util.ObjectTemplateTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class TemplateCorrelationConfigurationImpl implements TemplateCorrelationConfiguration {

    @Nullable private final ObjectTemplateType expandedObjectTemplate;
    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;
    @NotNull private final IndexingConfiguration indexingConfiguration;
    @NotNull private final PathKeyedMap<ItemCorrelationDefinitionType> correlationDefinitionMap = new PathKeyedMap<>();

    private TemplateCorrelationConfigurationImpl(
            @Nullable ObjectTemplateType expandedObjectTemplate,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @NotNull IndexingConfiguration indexingConfiguration) {
        this.expandedObjectTemplate = expandedObjectTemplate;
        this.identityManagementConfiguration = identityManagementConfiguration;
        this.indexingConfiguration = indexingConfiguration;
    }

    public static @NotNull TemplateCorrelationConfigurationImpl of(@Nullable ObjectTemplateType objectTemplate)
            throws ConfigurationException {
        TemplateCorrelationConfigurationImpl config = new TemplateCorrelationConfigurationImpl(
                objectTemplate,
                IdentityManagementConfigurationImpl.of(objectTemplate),
                IndexingConfigurationImpl.of(objectTemplate));
        if (objectTemplate != null) {
            addCorrelationDefinitionBeans(config, objectTemplate);
        }
        return config;
    }

    private static void addCorrelationDefinitionBeans(
            TemplateCorrelationConfigurationImpl config, ObjectTemplateType objectTemplate) throws ConfigurationException {
        for (ObjectTemplateItemDefinitionType itemDef : objectTemplate.getItem()) {
            ItemCorrelationDefinitionType correlationDef = itemDef.getCorrelation();
            if (correlationDef != null) {
                ItemPathType ref = MiscUtil.configNonNull(itemDef.getRef(), () -> "No ref in " + itemDef);
                config.correlationDefinitionMap.put(ref.getItemPath(), correlationDef);
            }
        }
    }

    @Override
    public @Nullable ObjectTemplateType getExpandedObjectTemplate() {
        return expandedObjectTemplate;
    }

    @Override
    public @NotNull IdentityManagementConfiguration getIdentityManagementConfiguration() {
        return identityManagementConfiguration;
    }

    @Override
    public @NotNull IndexingConfiguration getIndexingConfiguration() {
        return indexingConfiguration;
    }

    @Override
    public @NotNull PathKeyedMap<ItemCorrelationDefinitionType> getCorrelationDefinitionMap() {
        return correlationDefinitionMap;
    }

    @Override
    public @Nullable QName getDefaultMatchingRuleName(@NotNull ItemPath itemPath) throws ConfigurationException {
        if (expandedObjectTemplate == null) {
            return null;
        }
        ObjectTemplateItemDefinitionType itemDef = ObjectTemplateTypeUtil.findItemDefinition(expandedObjectTemplate, itemPath);
        return itemDef != null ? itemDef.getMatchingRule() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "objectTemplate=" + expandedObjectTemplate +
                ", identityManagementConfiguration=" + identityManagementConfiguration +
                ", indexingConfiguration=" + indexingConfiguration +
                ", correlationDefinitionMap: " + correlationDefinitionMap.keySet() +
                '}';
    }
}
