/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.common.configuration.api.SystemConfigurationSection;
import com.evolveum.midpoint.init.interpol.HostnameLookup;
import com.evolveum.midpoint.init.interpol.RandomLookup;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.SystemUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class StartupConfiguration implements MidpointConfiguration {

    private static final String SAFE_MODE = "safeMode";
    private static final String PROFILING_ENABLED = "profilingEnabled";
    private static final String PROFILING_MODE = "profilingMode";
    private static final String FILE_INDIRECTION_SUFFIX = "fileIndirectionSuffix";

    private static final String DEFAULT_FILE_INDIRECTION_SUFFIX = "_FILE";
    private static final String DEFAULT_CONFIG_FILE_NAME = "config.xml";
    private static final String LOGBACK_CONFIG_FILENAME = "logback.xml";
    private static final String LOGBACK_EXTRA_CONFIG_FILENAME = "logback-extra.xml";

    private static final Trace LOGGER = TraceManager.getTrace(StartupConfiguration.class);

    private boolean silent = false;

    private XMLConfiguration config;

    /**
     * Normalized name of midPoint home directory.
     * After successful initialization (see {@link #init()} it always ends with /.
     * TODO: this normalization is safe, but ugly when used in XML configs like
     * ${midpoint.home}/keystore.jceks - which, on the ohter hand, would be ugly without /.
     * Better yet is to normalize home directory NOT to end with / and use Paths.get(...)
     * and similar constructs in Java code and never string concatenation.
     * Also - maybe even this can be typed to Path in the end.
     */
    private String midPointHomePath;

    private final String configFilename;

    /**
     * Default constructor
     */
    StartupConfiguration() {
        this.configFilename = DEFAULT_CONFIG_FILE_NAME;
    }

    /**
     * Alternative constructor for use in the tests.
     */
    @SuppressWarnings("unused")
    public StartupConfiguration(String midPointHome, String configFilename) {
        this.midPointHomePath = midPointHome;
        this.configFilename = configFilename;
    }

    /**
     * Get current configuration file name
     */
    @SuppressWarnings("WeakerAccess")
    public String getConfigFilename() {
        return configFilename;
    }

    @Override
    public String getMidpointHome() {
        return midPointHomePath;
    }

    @Override
    public Configuration getConfiguration(@NotNull String componentName) {
        Configuration sub = config.subset(componentName);
        dumpConfiguration(componentName, sub);
        return sub;
    }

    @Override
    public Configuration getConfiguration() {
        dumpConfiguration("<root>", config);
        return config;
    }

    private void dumpConfiguration(String componentName, Configuration sub) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Configuration for {}:", componentName);
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                LOGGER.debug("    {} = {}", key, sub.getString(key));
            }
        }
    }

    /**
     * Initialize system configuration
     */
    public void init() {
        silent = Boolean.getBoolean(MIDPOINT_SILENT_PROPERTY);
        midPointHomePath = determineMidpointHome();

        setupInitialLoggingFromHomeDirectory();
        new ApplicationHomeSetup(silent, midPointHomePath).init();

        loadConfiguration();

        if (isSafeMode()) {
            LOGGER.info("Safe mode is ON; setting tolerateUndeclaredPrefixes to TRUE");
            QNameUtil.setTolerateUndeclaredPrefixes(true);
        }

        // Make sure that this is called very early in the startup sequence.
        // This is needed to properly initialize the resources
        // (the "org/apache/xml/security/resource/xmlsecurity" resource bundle error)
        WSSConfig.init();
    }

    private String determineMidpointHome() {
        String midPointHome;
        String midPointHomeProperty = System.getProperty(MIDPOINT_HOME_PROPERTY);
        if (StringUtils.isNotEmpty(midPointHomeProperty)) {
            midPointHome = normalizeDirectoryPath(midPointHomeProperty);
        } else {
            midPointHome = normalizeDirectoryPath(System.getProperty(USER_HOME_PROPERTY)) + "midpoint";
            LOGGER.info("{} system property is not set, using default value of {}", MIDPOINT_HOME_PROPERTY, midPointHome);
        }
        // This is not really good practice. But some components such as reports rely on well-formatted midpoint.home system property.
        System.setProperty(MIDPOINT_HOME_PROPERTY, midPointHome);
        return midPointHome;
    }

    private String normalizeDirectoryPath(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private void loadConfiguration() {
        File configFile = new File(midPointHomePath, this.getConfigFilename());
        printToSysout("Loading midPoint configuration from file " + configFile);
        LOGGER.info("Loading midPoint configuration from file {}", configFile);
        try {
            if (!configFile.exists()) {
                extractConfigurationFile(configFile);
            }
            createXmlConfiguration(configFile.getPath());
        } catch (ConfigurationException e) {
            String message = "Unable to read configuration file [" + configFile + "]: " + e.getMessage();
            LOGGER.error(message);
            printToSysout(message);
            throw new SystemException(message, e);      // there's no point in continuing with midpoint initialization
        }
    }

    private void extractConfigurationFile(File configFile) {
        LOGGER.warn("Configuration file {} does not exists. Need to do extraction ...", configFile);
        boolean success = ClassPathUtil.extractFileFromClassPath(this.getConfigFilename(), configFile.getPath());
        if (!success || !configFile.exists()) {
            String message = "Unable to extract configuration file " + this.getConfigFilename() + " from classpath";
            LOGGER.error(message);
            printToSysout(message);
            throw new SystemException(message);
        }

        try {
            SystemUtil.setPrivateFilePermissions(configFile.getPath());
        } catch (IOException ex) {
            String message = "Unable to set permissions for configuration file [" + configFile + "]: " + ex.getMessage();
            LOGGER.warn(message);
            printToSysout(message);
            // Non-critical, continue
        }
    }

    private void printToSysout(String message) {
        if (!silent) {
            System.out.println(message);
        }
    }

    private void setupInitialLoggingFromHomeDirectory() {
        File logbackConfigFile;
        boolean clear;

        File standardLogbackConfigFile = new File(midPointHomePath, LOGBACK_CONFIG_FILENAME);
        File extraLogbackConfigFile = new File(midPointHomePath, LOGBACK_EXTRA_CONFIG_FILENAME);

        if (standardLogbackConfigFile.exists()) {
            logbackConfigFile = standardLogbackConfigFile;
            clear = true;
        } else if (extraLogbackConfigFile.exists()) {
            logbackConfigFile = extraLogbackConfigFile;
            clear = false;
        } else {
            return;
        }

        LOGGER.info("Loading logging configuration from {} ({})", logbackConfigFile,
                clear ? "clearing default configuration" : "extending default configuration");
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (clear) {
            context.reset();
        }
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(logbackConfigFile);
        } catch (Exception e) {
            // This will logged by default logging configuration
            LOGGER.error("Error loading additional logging configuration: {}", e.getMessage(), e);
            // If normal logging fails make sure it is logged by web container
            e.printStackTrace();
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private void createXmlConfiguration(String filename) throws ConfigurationException {
        Map<String, Lookup> lookups = new HashMap<>(
                ConfigurationInterpolator.getDefaultPrefixLookups());
        lookups.put(RandomLookup.PREFIX, new RandomLookup());
        lookups.put(HostnameLookup.PREFIX, new HostnameLookup());

        FileBasedConfigurationBuilder<XMLConfiguration> builder =
                new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(
                                new Parameters()
                                        .xml()
                                        .setFileName(filename)
                                        .setPrefixLookups(lookups)
                        );
        config = builder.getConfiguration();
        config.addProperty(MIDPOINT_HOME_PROPERTY, midPointHomePath);
        applyEnvironmentProperties();
        resolveFileReferences();
    }

    private void resolveFileReferences() {
        String fileIndirectionSuffix = getFileIndirectionSuffix();
        config.getKeys().forEachRemaining(key -> {
            if (key.endsWith(fileIndirectionSuffix)) {
                String filename = config.getString(key);
                String valueKey = StringUtils.removeEnd(key, fileIndirectionSuffix);
                try {
                    String value = readFile(filename);
                    overrideProperty(valueKey, value);
                    LOGGER.trace("Property '{}' was read from '{}': '{}'", valueKey, filename, value);
                } catch (IOException e) {
                    String message =
                            "Couldn't read the value of configuration key '" + valueKey + "' from the file '" + filename + "': "
                                    + e.getMessage();
                    LoggingUtils.logUnexpectedException(LOGGER, message, e);
                    System.err.println(message);
                }
            }
        });
    }

    private String readFile(String filename) throws IOException {
        try (FileReader reader = new FileReader(filename)) {
            List<String> lines = IOUtils.readLines(reader);
            return String.join("\n", lines);
        }
    }

    private void applyEnvironmentProperties() {
        Properties properties = System.getProperties();
        properties.forEach((key, value) -> {
            LOGGER.trace("Property {} = '{}'", key, value);
            if (key instanceof String && ((String) key).startsWith("midpoint.")) {
                overrideProperty((String) key, value);
            }
        });
    }

    private void overrideProperty(String key, Object value) {
        LOGGER.debug("Overriding property {} to '{}'", key, value);
        config.setProperty(key, value);
    }

    @Override
    public boolean isSafeMode() {
        Configuration c = getRootConfiguration();
        return c != null && c.getBoolean(SAFE_MODE, false);
    }

    @Override
    public boolean isProfilingEnabled() {
        return getProfilingMode() != ProfilingMode.OFF;
    }

    @NotNull
    @Override
    public ProfilingMode getProfilingMode() {
        Configuration c = getRootConfiguration();
        if (c == null) {
            return ProfilingMode.OFF;
        } else {
            String profilingMode = c.getString(PROFILING_MODE, null);
            if (profilingMode != null) {
                return ProfilingMode.fromValue(profilingMode);
            } else {
                return c.getBoolean(PROFILING_ENABLED, false) ? ProfilingMode.ON : ProfilingMode.OFF;
            }
        }
    }

    @NotNull
    @Override
    public SystemConfigurationSection getSystemSection() {
        return new SystemConfigurationSectionImpl(getConfiguration(SYSTEM_CONFIGURATION));
    }

    private String getFileIndirectionSuffix() {
        Configuration c = getRootConfiguration();
        if (c == null) {
            return DEFAULT_FILE_INDIRECTION_SUFFIX;
        } else {
            return c.getString(FILE_INDIRECTION_SUFFIX, DEFAULT_FILE_INDIRECTION_SUFFIX);
        }
    }

    private Configuration getRootConfiguration() {
        return getConfiguration(ROOT_MIDPOINT_CONFIGURATION);
    }

    @Override
    public String toString() {
        Iterator<String> i = config.getKeys();
        StringBuilder sb = new StringBuilder();
        while (i.hasNext()) {
            String key = i.next();
            sb.append(key);
            sb.append(" = ");
            sb.append(config.getString(key));
            sb.append("; ");
        }
        return sb.toString();
    }
}
