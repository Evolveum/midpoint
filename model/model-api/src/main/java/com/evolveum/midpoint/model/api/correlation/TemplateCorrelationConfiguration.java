/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationDefinitionType;

import org.jetbrains.annotations.NotNull;

/**
 * All pieces of configuration data that are relevant for correlation obtainable from an object template.
 *
 * TODO reconsider this class
 */
public interface TemplateCorrelationConfiguration {

    @NotNull IdentityManagementConfiguration getIdentityManagementConfiguration();

    @NotNull IndexingConfiguration getIndexingConfiguration();

    @NotNull PathKeyedMap<ItemCorrelationDefinitionType> getCorrelationDefinitionMap();
}
