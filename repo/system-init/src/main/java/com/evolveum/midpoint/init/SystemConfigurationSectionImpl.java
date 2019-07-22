/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.common.configuration.api.SystemConfigurationSection;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import org.apache.commons.configuration.Configuration;

/**
 *
 */
public class SystemConfigurationSectionImpl implements SystemConfigurationSection {

	private static final String LOG_FILE_CONFIG_KEY = "logFile";
	private static final String JMAP_CONFIG_KEY = "jmap";

	private final Configuration configuration;

	SystemConfigurationSectionImpl(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getJmap() {
		String configured = getStringKey(JMAP_CONFIG_KEY);
		if (configured != null) {
			return configured;
		}
		String javaHome = System.getenv(MidPointConstants.JAVA_HOME_ENVIRONMENT_VARIABLE);
		if (javaHome != null) {
			return javaHome + "/bin/jmap";
		}
		return "jmap";          // Let's give it a chance. Maybe it's on the path.
	}

	@Override
	public String getLogFile() {
		return getStringKey(LOG_FILE_CONFIG_KEY);
	}

	private String getStringKey(String key) {
		if (configuration != null && configuration.containsKey(key)) {
			return (configuration.getString(key));
		} else {
			return null;
		}
	}
}
