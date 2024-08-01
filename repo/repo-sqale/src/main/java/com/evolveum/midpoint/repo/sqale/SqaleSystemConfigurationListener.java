/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

public class SqaleSystemConfigurationListener {

    private final SqaleRepoContext repositoryContext;

    public SqaleSystemConfigurationListener(SqaleRepoContext repositoryContext) {
        this.repositoryContext = repositoryContext;
    }

    public void update(@Nullable RepositoryConfigurationType repository) {
        if (repository != null) {
            updateImpl(repository);
        } else {
            applyDefaults();
        }
    }

    /** Applies configuration from RepositoryConfigurationType **/
    private void updateImpl(RepositoryConfigurationType repository) {
        enablePartitioningOnAdd(ShadowType.class, valueOrDefault(repository.getAutoCreatePartitionsOnAdd(), false));
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private void applyDefaults() {
        enablePartitioningOnAdd(ShadowType.class, false);
    }

    private void enablePartitioningOnAdd(Class<?> type, boolean value) {
        var mapping = (SqaleTableMapping) repositoryContext.getMappingBySchemaType(type);
        if (mapping.getPartitionManager() != null) {
            mapping.getPartitionManager().setPartitionCreationOnAdd(value);
        }
    }

}
