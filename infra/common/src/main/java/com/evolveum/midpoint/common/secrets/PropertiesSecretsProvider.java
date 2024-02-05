/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertiesSecretsProviderType;

public class PropertiesSecretsProvider extends CachedSecretsProvider<PropertiesSecretsProviderType> {

    private Charset charset;
    private File properties;

    public PropertiesSecretsProvider(PropertiesSecretsProviderType configuration) {
        super(configuration);
    }

    @Override
    public void init() {
        super.init();

        PropertiesSecretsProviderType config = getConfiguration();
        charset = config.getCharset() != null ? Charset.forName(config.getCharset()) : StandardCharsets.UTF_8;

        String path = config.getPropertiesFile();
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("No properties file specified in the properties secret provider "
                    + config.getIdentifier());
        }
        properties = new File(config.getPropertiesFile());
        if (!properties.exists() || !properties.isFile()) {
            throw new IllegalArgumentException(
                    "Properties file '" + path + "' specified in the properties secret provider " + config.getIdentifier()
                            + " does not exist or is not file");
        }
    }

    @Override
    protected <ST> ST resolveSecret(@NotNull String key, Class<ST> type) throws EncryptionException {
        return null;
    }
}
