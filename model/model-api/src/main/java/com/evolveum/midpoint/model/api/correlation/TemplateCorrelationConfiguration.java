/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * All pieces of configuration data that are relevant for correlation obtainable from an object template.
 *
 * TODO reconsider this class
 */
public interface TemplateCorrelationConfiguration {

    @Nullable ObjectTemplateType getExpandedObjectTemplate();

    @NotNull IdentityManagementConfiguration getIdentityManagementConfiguration();

    @NotNull IndexingConfiguration getIndexingConfiguration();

    @NotNull PathKeyedMap<ItemCorrelationDefinitionType> getCorrelationDefinitionMap();

    @Nullable QName getDefaultMatchingRuleName(@NotNull ItemPath itemPath) throws ConfigurationException;
}
