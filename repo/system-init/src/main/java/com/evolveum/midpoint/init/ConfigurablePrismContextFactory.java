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
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * @author lazyman
 */
public class ConfigurablePrismContextFactory extends MidPointPrismContextFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurablePrismContextFactory.class);
    private static final String CONFIGURATION_GLOBAL = "midpoint.global";        //todo move somewhere else
    private static final String EXTENSION_DIR = "extensionDir";
    private MidpointConfiguration configuration;

    public MidpointConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MidpointConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void registerExtensionSchemas(SchemaRegistry schemaRegistry) throws SchemaException {
        Configuration config = configuration.getConfiguration(CONFIGURATION_GLOBAL);
        if (config == null) {
            LOGGER.warn("Global part 'midpoint.global' is not defined in configuration file.");
            return;
        }
        String extensionDir = config.getString(EXTENSION_DIR);

        if (StringUtils.isEmpty(extensionDir)) {
            if (StringUtils.isNotEmpty(configuration.getMidpointHome())) {
                extensionDir = configuration.getMidpointHome() + "/schema";
            }
        }

        if (StringUtils.isNotEmpty(extensionDir)) {
            LOGGER.info("Loading extension schemas from folder '{}'.", new Object[]{extensionDir});
        } else {
            LOGGER.warn("Not loading extension schemas, extensionDir or even midpoint.home is not defined.");
            return;
        }

        try {
            File file = new File(extensionDir);
            if (!file.exists() || !file.isDirectory()) {
                LOGGER.warn("Extension dir '{}' does not exist, or is not a directory, skipping extension loading.",
                        new Object[]{extensionDir});
                return;
            }

            schemaRegistry.registerPrismSchemasFromDirectory(file);
        } catch (Exception ex) {
            throw new SchemaException(ex.getMessage(), ex);
        }
    }
}
