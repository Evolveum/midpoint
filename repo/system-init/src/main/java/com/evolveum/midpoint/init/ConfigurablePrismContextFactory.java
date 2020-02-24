/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class ConfigurablePrismContextFactory extends MidPointPrismContextFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurablePrismContextFactory.class);
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
        Configuration config = configuration.getConfiguration(MidpointConfiguration.GLOBAL_CONFIGURATION);
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
                extensionDir = Paths.get(configuration.getMidpointHome(), "schema").toString();
            }
        }

        if (StringUtils.isNotEmpty(extensionDir)) {
            LOGGER.info("Loading extension schemas from folder '{}'.", new Object[] { extensionDir });
        } else {
            LOGGER.warn("Not loading extension schemas, extensionDir or even midpoint.home is not defined.");
            return;
        }

        try {
            File file = new File(extensionDir);
            if (!file.exists() || !file.isDirectory()) {
                LOGGER.warn("Extension dir '{}' does not exist, or is not a directory, skipping extension loading.",
                        new Object[] { extensionDir });
                return;
            }

            schemaRegistry.registerPrismSchemasFromDirectory(file);
        } catch (Exception ex) {
            throw new SchemaException(ex.getMessage(), ex);
        }
    }
}
