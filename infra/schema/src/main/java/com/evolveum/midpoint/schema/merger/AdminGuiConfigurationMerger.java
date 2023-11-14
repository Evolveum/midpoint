/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;

public class AdminGuiConfigurationMerger extends BaseItemMerger<PrismContainer<AdminGuiConfigurationType>> {

    public AdminGuiConfigurationMerger(@Nullable OriginMarker originMarker) {
        super(originMarker);
    }

    @Override
    protected void mergeInternal(
            @NotNull PrismContainer<AdminGuiConfigurationType> target,
            @NotNull PrismContainer<AdminGuiConfigurationType> source)
            throws ConfigurationException, SchemaException {

        AdminGuiConfigurationMergeManager manager = new AdminGuiConfigurationMergeManager();

    }
}
