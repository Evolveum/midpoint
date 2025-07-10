/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.configuration.api;

import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;

public interface MidpointConfiguration {

    // Names of system properties. Note that they are also copied into config.xml-loaded configuration.
    String MIDPOINT_SILENT_PROPERTY = "midpoint.silent";
    String MIDPOINT_HOME_PROPERTY = "midpoint.home";
    String MIDPOINT_SCHRODINGER_PROPERTY = "midpoint.schrodinger";
    String MIDPOINT_LOGGING_ALT_ENABLED_PROPERTY = "midpoint.logging.alt.enabled";
    String MIDPOINT_LOGGING_ALT_FILENAME_PROPERTY = "midpoint.logging.alt.filename";
    String MIDPOINT_LOGGING_ALT_PREFIX_PROPERTY = "midpoint.logging.alt.prefix";

    String MIDPOINT_SKIP_VERSION_CHECK="midpoint.skipVersionCheck";

    String USER_HOME_PROPERTY = "user.home";

    /** Property for config file name override; if empty, default value is used. */
    String MIDPOINT_CONFIG_FILE_PROPERTY = "midpoint.configFile";

    // Other commonly-used configuration properties
    String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
    String MIDPOINT_NODE_ID_SOURCE_PROPERTY = "midpoint.nodeIdSource";
    String MIDPOINT_URL_PROPERTY = "midpoint.url";
    String MIDPOINT_HOST_NAME_PROPERTY = "midpoint.hostName";
    String MIDPOINT_HTTP_PORT_PROPERTY = "midpoint.httpPort";

    // Names of configuration sections
    String AUDIT_CONFIGURATION = "midpoint.audit";
    String SYSTEM_CONFIGURATION = "midpoint.system";
    String GLOBAL_CONFIGURATION = "midpoint.global";
    String PROTECTOR_CONFIGURATION = "midpoint.keystore";
    String REPOSITORY_CONFIGURATION = "midpoint.repository";
    String ROOT_MIDPOINT_CONFIGURATION = "midpoint";
    String CONSTANTS_CONFIGURATION = "midpoint.constants";
    String ICF_CONFIGURATION = "midpoint.icf";
    String TASK_MANAGER_CONFIGURATION = "midpoint.taskManager";
    String DOT_CONFIGURATION = "midpoint.dot";
    String WEB_APP_CONFIGURATION = "midpoint.webApplication";
    String WORKFLOW_CONFIGURATION = "midpoint.workflow";
    String INTERNALS_CONFIGURATION = "midpoint.internals";

    String ADMINISTRATOR_INITIAL_PASSWORD = "midpoint.administrator.initialPassword";

    /**
     * System property for Smart Integration service URL override. To be used mainly for testing purposes.
     *
     * Accessed via {@link System#getProperty(String)}, not via configuration subsystem
     * (this is to be reconsidered in the future).
     */
    String SMART_INTEGRATION_SERVICE_URL_OVERRIDE = "midpoint.smartIntegration.serviceUrlOverride";

    /**
     * Reference to midpoint-system.properties generated in system-init component.
     * It is generated during the build.
     * This file contains various system properties such midPoint version, build ID, build timestamp and so on.
     * This is a base path (without extension).
     */
    String MIDPOINT_SYSTEM_PROPERTIES_BASE_PATH = "midpoint-system";

    /**
     * @return midPoint home directory. Currently, it is the same value as in "midpoint.home" system property.
     */
    String getMidpointHome();

    /**
     * Get configuration for symbolic name of the component from configuration subsystem.
     *
     * @param component name of the component
     * Samples of names:
     * <li>
     *   <ul>repository -> midpoint.repository</ul>
     *   <ul>provisioning -> midpoint.provisioning</ul>
     * </li>
     * @return Configuration object
     * Sample how to get config value: {@code config.getInt("port", 1234);}
     */
    Configuration getConfiguration(String component);

    /**
     * @return Global configuration.
     */
    Configuration getConfiguration();

    /**
     * @return True if we are running in safe mode (the exact meaning gradually evolves; but the overall idea is to make
     * midPoint barely usable to be able to fix the worst problems preventing it from running normally).
     */
    boolean isSafeMode();

    /**
     * @return True if the profiling interceptor should be loaded.
     */
    @SuppressWarnings("unused")
    // It is actually used from ctx-interceptor.xml
    boolean isProfilingEnabled();

    /**
     * @return Current profiling mode e.g. on, off, dynamic.
     */
    @NotNull
    ProfilingMode getProfilingMode();

    /**
     * @return "midpoint.system" section of the system configuration
     */
    @NotNull
    SystemConfigurationSection getSystemSection();

    /**
     * Returns true if the configuration key matches any of provided regular expression patterns.
     * Provided pattern must match the whole value, see {@link String#matches(java.lang.String)}.
     * Some examples:
     * <ul>
     * <li>{@code x\.y.*} - value starts with {@code x.y} (dot must be escaped, in Java String
     * syntax double backslash must be used!)</li>
     * <li>{@code (?i)sql} - value is exactly SQL, ignoring casing</li>
     * </ul>
     *
     * @param key key from configuration, starting at {@code configuration} element (root),
     * can be complex, e.g. {@code midpoint.repository.type}.
     * @param regexPatterns regular expression patterns that must match the whole value.
     * It is possible to match {@code null} too, if no regex pattern is used, or if the first
     * pattern itself is null.
     */
    @SuppressWarnings("unused")
    // used in SpEL expressions
    boolean keyMatches(String key, String... regexPatterns);
}
