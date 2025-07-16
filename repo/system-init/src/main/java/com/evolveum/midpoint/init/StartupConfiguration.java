/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

public class StartupConfiguration implements MidpointConfiguration, EnvironmentAware {

    private static final String SAFE_MODE = "safeMode";
    private static final String PROFILING_ENABLED = "profilingEnabled";
    private static final String PROFILING_MODE = "profilingMode";
    private static final String FILE_INDIRECTION_SUFFIX = "fileIndirectionSuffix";

    private static final String DEFAULT_FILE_INDIRECTION_SUFFIX = "_FILE";
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.xml";
    private static final String LOGBACK_CONFIG_FILENAME = "logback.xml";
    private static final String LOGBACK_EXTRA_CONFIG_FILENAME = "logback-extra.xml";

    private static final Trace LOGGER = TraceManager.getTrace(StartupConfiguration.class);

    /**
     * List of configuration keys or JVM override keys that should hide their values.
     * Short keys are used for dumps to the log, qualified JVM argument keys are for About page.
     */
    public static final List<String> SENSITIVE_CONFIGURATION_VARIABLES = Arrays.asList(
            "jdbcPassword",
            "keyStorePassword",
            "midpoint.repository.dataSource",
            "midpoint.repository.jdbcUrl",
            "midpoint.repository.jdbcUsername",
            "midpoint.repository.jdbcPassword",
            "midpoint.audit.dataSource",
            "midpoint.audit.jdbcUrl",
            "midpoint.audit.jdbcUsername",
            "midpoint.audit.jdbcPassword",
            "midpoint.keystore.keyStorePassword",
            MidpointConfiguration.ADMINISTRATOR_INITIAL_PASSWORD
    );
    public static final String SENSITIVE_VALUE_OUTPUT = "[*****]";

    /**
     * For troubleshooting, enable it via JVM argument: -Dmidpoint.printSensitiveValues
     * This only allows the printing in the log, About page always hides sensitive values.
     */
    private static final boolean PRINT_SENSITIVE_VALUES = System.getProperty("midpoint.printSensitiveValues") != null;

    private boolean silent = false;

    private XMLConfiguration config;

    /**
     * Normalized name of midPoint home directory.
     * After successful initialization (see {@link #init()} it always ends with /.
     * TODO: this normalization is safe, but ugly when used in XML configs like
     * ${midpoint.home}/keystore.jceks - which, on the other hand, would be ugly without /.
     * Better yet is to normalize home directory NOT to end with / and use Paths.get(...)
     * and similar constructs in Java code and never string concatenation.
     * Also - maybe even this can be typed to Path in the end.
     */
    private String midPointHomePath;

    private final String configFilename;

    private Environment environment;

    /**
     * Default constructor
     */
    StartupConfiguration() {
        this.configFilename = System.getProperty(
                MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY,
                DEFAULT_CONFIG_FILE_NAME);
    }

    /**
     * Alternative constructor for use in the tests.
     */
    @SuppressWarnings("unused")
    public StartupConfiguration(String midPointHome, String configFilename) {
        this.midPointHomePath = midPointHome;
        this.configFilename = configFilename;
    }

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
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

    // TODO do we want it for each getConfiguration call and on DEBUG level?
    //  getConfiguration(String) is called from 18 places + from more when getRootConfiguration() is considered
    //  This is better after the init of this class just once - or as trace if in getConfig calls.
    private void dumpConfiguration(String componentName, Configuration sub) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Configuration for {}:", componentName);
            Iterator<String> i = sub.getKeys();
            while (i.hasNext()) {
                String key = i.next();
                LOGGER.debug("    {} = '{}'", key, valuePrintout(key, sub.getString(key)));
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
        // This is not a good practice. But some components such as reports rely on well-formatted midpoint.home system property.
        System.setProperty(MIDPOINT_HOME_PROPERTY, midPointHome);
        return midPointHome;
    }

    private String normalizeDirectoryPath(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    private void loadConfiguration() {
        File configFile = new File(midPointHomePath, configFilename);
        printToSysout("Loading midPoint configuration from file " + configFile.getAbsolutePath());
        LOGGER.info("Loading midPoint configuration from file {}", configFile.getAbsolutePath());
        try {
            // If the config name is set explicitly, we don't want to unpack the default file.
            if (!configFile.exists()
                    && System.getProperty(MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY) == null) {
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
        LOGGER.info("Configuration file {} does not exist, the default one will be extracted.", configFile);
        boolean success = ClassPathUtil.extractFileFromClassPath(configFilename, configFile.getPath());
        if (!success || !configFile.exists()) {
            String message = "Unable to extract configuration file " + configFilename + " from classpath";
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
            // This will be logged by default logging configuration
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

        // This configures only our prefixes, not the default ones like expr (which are also a security concern).
        // Note that the Lookup.lookup(var) is called when get*() is called, so it can be evaluated every time.
        FileBasedConfigurationBuilder<XMLConfiguration> builder =
                new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(
                                new Parameters()
                                        .xml()
                                        .setFileName(filename)
                                        .setPrefixLookups(lookups));
        /*
        On debug level this shows stacktrace for:
        DEBUG org.apache.commons.beanutils.FluentPropertyBeanIntrospector - Exception is:
        java.beans.IntrospectionException: bad write method arg count:
        public final void org.apache.commons.configuration2.AbstractConfiguration.setProperty
        This is reportedly beanutils over-strictness issue but is nowhere close to be fixed.
        Jira for commons-configuration can be also found, but they rely on beanutils fix.
        */
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
                    LOGGER.trace("Property '{}' was read from '{}': '{}'",
                            valueKey, filename, valuePrintout(key, value));
                } catch (IOException e) {
                    String message = "Couldn't read the value of configuration key '" + valueKey
                            + "' from the file '" + filename + "': " + e;
                    LoggingUtils.logUnexpectedException(LOGGER, message, e);
                    System.err.println(message);
                    throw new SystemException(e);
                }
            }
        });
    }

    /**
     * Returns provided value for printing or string replacement for sensitive values (passwords).
     */
    private String valuePrintout(String key, Object value) {
        return PRINT_SENSITIVE_VALUES
                || SENSITIVE_CONFIGURATION_VARIABLES.stream().noneMatch(s -> key.contains(s))
                ? String.valueOf(value)
                : SENSITIVE_VALUE_OUTPUT;
    }

    public static boolean isPrintSensitiveValues() {
        return PRINT_SENSITIVE_VALUES;
    }

    private String readFile(String filename) throws IOException {
        Path filePath = Path.of(filename.replace("${midpoint.home}", midPointHomePath))
                .toAbsolutePath(); // this provides better diagnostics when the file is not found
        // Files.readString leaves final line terminator on Linux as part of the value.
        // This implementation tolerates also a single empty trailing line (but not more).
        return String.join("\n",
                Files.readAllLines(filePath, StandardCharsets.UTF_8));
    }

    private void applyEnvironmentProperties() {
        Properties properties = System.getProperties();
        properties.forEach((key, value) -> {
            LOGGER.trace("System property {} = '{}'", key, valuePrintout(String.valueOf(key), value));
            if (key instanceof String && ((String) key).startsWith("midpoint.")) {
                overrideProperty((String) key, value);
            }
        });
        if (environment != null) {
            for (PropertySource<?> ps : ((AbstractEnvironment) environment).getPropertySources()) {
                if (ps instanceof EnumerablePropertySource) {
                    for (String name : ((EnumerablePropertySource<?>) ps).getPropertyNames()) {
                        if (name.startsWith("midpoint.")) {
                            String value = environment.getProperty(name);
                            LOGGER.trace("Environment property {} = '{}'", name, valuePrintout(name, value));
                            overrideProperty(name, value);
                        }
                    }
                }
            }
        }
    }

    private void overrideProperty(String key, Object value) {
        LOGGER.debug("Overriding property {} to '{}'", key, valuePrintout(key, value));
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

    @Override
    public boolean keyMatches(String key, String... regexPatterns) {
        String value = config.getString(key);
        if (value == null) {
            // we are able to match null values too and we're very flexible here
            return regexPatterns == null || regexPatterns.length == 0 || regexPatterns[0] == null;
        }

        return Arrays.stream(regexPatterns)
                .anyMatch(regex -> value.matches(regex));
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
        if (config == null) {
            return "StartupConfiguration{null config}";
        }
        Iterator<String> i = config.getKeys();
        StringBuilder sb = new StringBuilder("StartupConfiguration{");
        boolean moreItems = false;
        while (i.hasNext()) {
            if (moreItems) {
                sb.append("'; ");
            }
            String key = i.next();
            sb.append(key);
            sb.append(" = '");
            sb.append(valuePrintout(key, config.getString(key)));
            moreItems = true;
        }
        return sb.append('}').toString();
    }
}
