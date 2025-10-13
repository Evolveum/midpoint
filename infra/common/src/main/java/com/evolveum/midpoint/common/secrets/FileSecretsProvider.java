/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileSecretsProviderType;

/**
 * @see ContainerSecretsProvider
 */
public class FileSecretsProvider extends ContainerSecretsProvider<FileSecretsProviderType> {

    public FileSecretsProvider(@NotNull FileSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    protected @NotNull File getParentDirectory() {
        String parentDirectoryPath = getConfiguration().getParentDirectoryPath();
        if (parentDirectoryPath == null) {
            throw new SystemException("No parent directory defined for secrets provider " + getIdentifier());
        }

        return new File(parentDirectoryPath);
    }
}
