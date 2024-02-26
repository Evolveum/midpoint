/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileSecretsProviderType;

/**
 * Secrets provider that reads secrets from files.
 * Each secret is stored as separate file where secret name is file name and secret value is file content.
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
