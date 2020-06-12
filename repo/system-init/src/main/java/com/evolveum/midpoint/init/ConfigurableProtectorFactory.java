/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.configuration2.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.impl.crypto.KeyStoreBasedProtectorImpl;
import com.evolveum.midpoint.util.SystemUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ConfigurableProtectorFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurableProtectorFactory.class);

    @Autowired private MidpointConfiguration configuration;

    private ProtectorConfiguration protectorConfig;

    public void init() {
        Configuration config = configuration.getConfiguration(MidpointConfiguration.PROTECTOR_CONFIGURATION);
        protectorConfig = new ProtectorConfiguration(config);

        String keyStorePath = protectorConfig.getKeyStorePath();
        if (keyStorePath == null) {
            throw new SystemException("Keystore path not defined");
        }

        File ks = new File(keyStorePath);
        if (ks.exists()) {
            return;
        }

        try {
            KeyStore keystore = KeyStore.getInstance("jceks");
            char[] password = "changeit".toCharArray();

            keystore.load(null, password);

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();

            keystore.setKeyEntry("default", secretKey, "midpoint".toCharArray(), null);

            try (FileOutputStream fos = new FileOutputStream(keyStorePath)) {
                try {
                    SystemUtil.setPrivateFilePermissions(keyStorePath);
                } catch (IOException e) {
                    LOGGER.warn("Unable to set file permissions for keystore {}: {}", keyStorePath, e.getMessage(), e);
                    // Non-critical, continue
                }
                keystore.store(fos, password);
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't generate keystore, reason: " + ex.getMessage(), ex);
        }
    }

    public MidpointConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MidpointConfiguration configuration) {
        this.configuration = configuration;
    }

    public Protector getProtector() {
        // We cannot use KeyStoreBasedProtectorBuilder here, because there is no prism context yet.
        // This means that system-init will depend on prism-impl.
        KeyStoreBasedProtectorImpl protector = new KeyStoreBasedProtectorImpl();
        protector.setEncryptionKeyAlias(protectorConfig.getEncryptionKeyAlias());
        protector.setKeyStorePassword(protectorConfig.getKeyStorePassword());
        protector.setKeyStorePath(protectorConfig.getKeyStorePath());
        protector.setEncryptionAlgorithm(protectorConfig.getXmlCipher());
        protector.init();
        return protector;
    }
}
