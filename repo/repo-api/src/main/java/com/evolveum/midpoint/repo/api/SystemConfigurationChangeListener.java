/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.xml.ns._public.common.common_4.SystemConfigurationType;
import org.jetbrains.annotations.Nullable;

/**
 * Listener that needs to receive notifications related to system configuration object changes.
 */
public interface SystemConfigurationChangeListener {
	/**
	 * Updates the listener's internal state with the configuration provided.
	 *
	 * @param value Current value of the system configuration object. It is 'null' if the object does not exist.
	 *              Usually listeners keep their current state in such cases, but if needed, it will have the information
	 *              about missing sysconfig object, so it could act accordingly.
	 *
	 * @return false if the update was not successful, so it needs to be repeated. The same effect is when
	 * a runtime exception is thrown.
	 */
	boolean update(@Nullable SystemConfigurationType value);
}
