/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.crypto.AESProtector;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

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

        //todo remove this stuff, or only test clause
        if (protectorConfig.getKeyStorePath().endsWith("keystore.jceks")) {
            //hack to try 2 class paths
            if (!ClassPathUtil.extractFileFromClassPath("com/../../keystore.jceks", protectorConfig.getKeyStorePath())) {
                ClassPathUtil.extractFileFromClassPath("keystore.jceks", protectorConfig.getKeyStorePath());
            }
        } else if (protectorConfig.getKeyStorePath().endsWith("test-keystore.jceks")) {
            if (!ClassPathUtil.extractFileFromClassPath("com/../../test-keystore.jceks", protectorConfig.getKeyStorePath())) {
                ClassPathUtil.extractFileFromClassPath("test-keystore.jceks", protectorConfig.getKeyStorePath());
            }
        } else {
            LOGGER.error("Unable to find/extract keystore file {} from classpath.",
                    new Object[]{protectorConfig.getKeyStorePath()});
        }
    }

    public Protector getProtector() {
        AESProtector protector = new AESProtector();
        protector.setEncryptionKeyAlias(protectorConfig.getEncryptionKeyAlias());
        protector.setKeyStorePassword(protectorConfig.getKeyStorePassword());
        protector.setKeyStorePath(protectorConfig.getKeyStorePath());
        protector.init();

        return protector;
    }
}
