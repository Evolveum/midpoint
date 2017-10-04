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
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.schema.SchemaRegistryImpl;
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

    // This is a hack to facilitate having separate extension schema directories for individual tests.
    // It would be better to declare it as instance attribute but then it's not easy to set it up
    // in TestNG tests (before midPoint is started).
    private static String extensionDirOverride;

    public static void setExtensionDirOverride(String extensionDirOverride) {
        ConfigurablePrismContextFactory.extensionDirOverride = extensionDirOverride;
    }

    ConfigurablePrismContextFactory() {
		super();
	}

    public MidpointConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(MidpointConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void registerExtensionSchemas(SchemaRegistryImpl schemaRegistry) throws SchemaException {
        Configuration config = configuration.getConfiguration(CONFIGURATION_GLOBAL);
        if (config == null) {
            LOGGER.warn("Global part 'midpoint.global' is not defined in configuration file.");
        }

        String extensionDir;
        if (extensionDirOverride != null) {
            extensionDir = extensionDirOverride;
        } else if (config != null) {
            extensionDir = config.getString(EXTENSION_DIR); // potentially null
        } else {
            extensionDir = null;
        }

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
