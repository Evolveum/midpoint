/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.common.configuration.api;

import org.apache.commons.configuration.Configuration;
import org.w3c.dom.Document;

/**
 * @author mamut
 */

public interface MidpointConfiguration {
	
	// Names of system properties. Note that they are also copied into config.xml-loaded configuration.
	String MIDPOINT_SILENT_PROPERTY = "midpoint.silent";
	String MIDPOINT_HOME_PROPERTY = "midpoint.home";
	String MIDPOINT_LOGGING_ALT_ENABLED_PROPERTY = "midpoint.logging.alt.enabled";

	// TODO read these from the MidpointConfiguration instead of system properties.
	// It will provide greater flexibility, as the administrator will be able to set them permanently in config.xml.
	String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
	String MIDPOINT_JMX_HOST_NAME_PROPERTY = "midpoint.jmxHostName";
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

    boolean isSafeMode();

	boolean isProfilingEnabled();
}
