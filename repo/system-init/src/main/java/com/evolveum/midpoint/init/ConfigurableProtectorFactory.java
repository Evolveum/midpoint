/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;

/**
 * @author lazyman
 */
public class ConfigurableProtectorFactory {

    private static final String PROTECTOR_CONFIGURATION = "midpoint.keystore";
    private static final Trace LOGGER = TraceManager.getTrace(ConfigurableProtectorFactory.class);
    @Autowired(required = true)
    private MidpointConfiguration configuration;
    private ProtectorConfiguration protectorConfig;

    public void init() {
        Configuration config = configuration.getConfiguration(PROTECTOR_CONFIGURATION);
        protectorConfig = new ProtectorConfiguration(config);

        //Extract file if not exists
        if (config.getString("midpoint.home") == null) {
            return;
        }

        File ks = new File(protectorConfig.getKeyStorePath());
        if (ks.exists()) {
            return;
        }

        //todo improve
        FileOutputStream fos = null;
        try {
            KeyStore keystore = KeyStore.getInstance("jceks");
            char[] password = "changeit".toCharArray();

            keystore.load(null, password);

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();

            keystore.setKeyEntry("default", secretKey, "midpoint".toCharArray(), null);

            fos = new FileOutputStream(protectorConfig.getKeyStorePath());
            keystore.store(fos, password);
            fos.close();
        } catch (Exception ex) {
            throw new SystemException("Couldn't generate keystore, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public Protector getProtector() {
        ProtectorImpl protector = new ProtectorImpl();
        protector.setEncryptionKeyAlias(protectorConfig.getEncryptionKeyAlias());
        protector.setKeyStorePassword(protectorConfig.getKeyStorePassword());
        protector.setKeyStorePath(protectorConfig.getKeyStorePath());
        protector.setEncryptionAlgorithm(protectorConfig.getXmlCipher());
        protector.init();

        return protector;
    }
}
