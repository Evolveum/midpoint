/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DockerSecretsProviderType;

public class DockerSecretsProvider extends SecretsProviderImpl<DockerSecretsProviderType> {

    private Charset charset;

    public DockerSecretsProvider(DockerSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    public void initialize() {
        super.initialize();

        DockerSecretsProviderType config = getConfiguration();
        charset = config.getCharset() != null ? Charset.forName(config.getCharset()) : StandardCharsets.UTF_8;
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, @NotNull Class<ST> type) throws EncryptionException {
        File parent;
        if (SystemUtils.IS_OS_WINDOWS) {
            parent = new File("C:\\ProgramData\\Docker\\secrets");
        } else {
            parent = new File("/run/secrets");
        }

        // to avoid path traversal
        String filename = new File(key).getName();
        File valueFile = new File(parent, filename);
        ST value = null;
        if (valueFile.exists() && valueFile.isFile() && valueFile.canRead()) {
            try (InputStream is = new FileInputStream(valueFile)) {
                String content = IOUtils.toString(is, charset);

                value = mapValue(content, type);
            } catch (IOException ex) {
                throw new EncryptionException("Couldn't read secret from " + valueFile.getAbsolutePath(), ex);
            }
        }

        return value;
    }
}
