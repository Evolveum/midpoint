/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DockerSecretsProviderType;

/**
 * Custom implementation of {@link ContainerSecretsProvider} for Docker secrets.
 * Secrets directory is defined by Docker and it is not configurable.
 *
 * It is always /run/secrets on Linux and C:\ProgramData\Docker\secrets on Windows.
 *
 * @see ContainerSecretsProvider
 */
public class DockerSecretsProvider extends ContainerSecretsProvider<DockerSecretsProviderType> {

    private static final File PARENT_DIRECTORY_WINDOWS = new File("C:\\ProgramData\\Docker\\secrets");

    private static final File PARENT_DIRECTORY_LINUX = new File("/run/secrets");

    public DockerSecretsProvider(DockerSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    protected @NotNull File getParentDirectory() {
        return SystemUtils.IS_OS_WINDOWS ? PARENT_DIRECTORY_WINDOWS : PARENT_DIRECTORY_LINUX;
    }
}
