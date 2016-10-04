/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

/**
 * This is a class that statically holds current system configuration.
 * Used for these exceptional cases when it's simply not efficient to retrieve system configuration every time from the repository.
 * (e.g. in GUI when determining whether to enable experimental parts of midPoint code).
 *
 * As a rule, we DO NOT provide whole configuration object in order to avoid misuse of this feature.
 * We only provide carefully selected getter methods.
 *
 * @author mederly
 */
@Deprecated
public class SystemConfigurationHolder {

    private static SystemConfigurationType currentConfiguration;

    public static void setCurrentConfiguration(SystemConfigurationType currentConfiguration) {
        SystemConfigurationHolder.currentConfiguration = currentConfiguration;
    }

    public static boolean isExperimentalCodeEnabled() {
        return SystemConfigurationTypeUtil.isExperimentalCodeEnabled(currentConfiguration);
    }

    public static AccessCertificationConfigurationType getCertificationConfiguration() {
        if (currentConfiguration != null) {
            return currentConfiguration.getAccessCertification();
        } else {
            return null;
        }
    }

	public static WfConfigurationType getWorkflowConfiguration() {
		if (currentConfiguration != null) {
			return currentConfiguration.getWorkflowConfiguration();
		} else {
			return null;
		}
	}
}
