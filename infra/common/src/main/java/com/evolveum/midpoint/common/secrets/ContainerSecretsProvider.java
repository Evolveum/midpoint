/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerSecretsProviderType;

/**
 * Secrets provider that reads secrets from files.
 * Each secret is stored as separate file where secret name is file name and secret value is file content.
 * Parent directory for secrets has to be defined in configuration.
 */
public abstract class ContainerSecretsProvider<T extends ContainerSecretsProviderType> extends SecretsProviderImpl<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerSecretsProvider.class);

    private File parentDirectory;

    private Charset charset;

    public ContainerSecretsProvider(T configuration) {
        super(configuration);
    }

    @Override
    public void initialize() {
        super.initialize();

        parentDirectory = getParentDirectory();

        T config = getConfiguration();
        charset = config.getCharset() != null ? Charset.forName(config.getCharset()) : StandardCharsets.UTF_8;
    }

    protected abstract @NotNull File getParentDirectory();

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        // to avoid path traversal
        String filename = new File(key).getName();
        File valueFile = new File(parentDirectory, filename);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Reading secret from {}", valueFile.getAbsolutePath());
        }

        ST value = null;
        if (valueFile.exists() && valueFile.isFile() && valueFile.canRead()) {
            try (Reader reader = new FileReader(valueFile)) {
                byte[] content = IOUtils.toByteArray(reader, charset);

                value = mapValue(content, type);
            } catch (IOException ex) {
                throw new EncryptionException("Couldn't read secret from " + valueFile.getAbsolutePath(), ex);
            }
        }

        return value;
    }
}
