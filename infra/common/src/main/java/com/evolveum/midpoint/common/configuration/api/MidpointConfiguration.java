/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.configuration.api;

import org.apache.commons.configuration.Configuration;
import org.jetbrains.annotations.NotNull;

/**
 * @author mamut
 */

public interface MidpointConfiguration {

    // Names of system properties. Note that they are also copied into config.xml-loaded configuration.
    String MIDPOINT_SILENT_PROPERTY = "midpoint.silent";
    String MIDPOINT_HOME_PROPERTY = "midpoint.home";
    String MIDPOINT_LOGGING_ALT_ENABLED_PROPERTY = "midpoint.logging.alt.enabled";
    String MIDPOINT_LOGGING_ALT_FILENAME_PROPERTY = "midpoint.logging.alt.filename";
    String MIDPOINT_LOGGING_ALT_PREFIX_PROPERTY = "midpoint.logging.alt.prefix";
    String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
    String MIDPOINT_NODE_ID_SOURCE_PROPERTY = "midpoint.nodeIdSource";
    @Deprecated String MIDPOINT_JMX_HOST_NAME_PROPERTY = "midpoint.jmxHostName";
    String MIDPOINT_URL_PROPERTY = "midpoint.url";
    String MIDPOINT_HOST_NAME_PROPERTY = "midpoint.hostName";
    String MIDPOINT_HTTP_PORT_PROPERTY = "midpoint.httpPort";

    // TODO read these from the MidpointConfiguration instead of system properties.
    // It will provide greater flexibility, as the administrator will be able to set them permanently in config.xml.
    String MIDPOINT_SCHRODINGER_PROPERTY = "midpoint.schrodinger";

    // names of configuration sections
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
    /**
     * Reference to midpoint-system.properties generated in system-init component.
     * It is generated during the build.
     * This file contains various system properties such midPoint version, build ID, build timestamp and so on.
     * This is a base path (without extension).
     */
    String MIDPOINT_SYSTEM_PROPERTIES_BASE_PATH = "midpoint-system";

    String getMidpointHome();

    /**
     * Get configuration for symbolic name of the component from configuration subsystem.
     *
     * @param component
     *            name of the component
     *            Samples of names:
     *            <li>
     *              <ul>repository -> midpoint.repository</ul>
     *              <ul> provisioning -> midpoint.provisioning</ul>
     *              <ul> model -> midpoint.model</ul>
     *            </li>
     *
     * @return Configuration object
     *         Sample how to get config value: {@code config.getInt("port", 1234);}
     */
    Configuration getConfiguration(String component);

    Configuration getConfiguration();

    boolean isSafeMode();

    /**
     * @return true if the profiling interceptor should be loaded
     */
    boolean isProfilingEnabled();

    @NotNull
    ProfilingMode getProfilingMode();

    @NotNull
    SystemConfigurationSection getSystemSection();
}
