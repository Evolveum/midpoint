/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.wicket.Component;

import java.util.stream.Collectors;

/**
 * @author mederly
 */
public class WfGuiUtil {

	/**
	 * Creates localized process instance name from the workflow context (if possible); otherwise returns null.
	 */
	public static String getLocalizedProcessName(WfContextType wfc, Component component) {
		if (wfc != null && !wfc.getLocalizableProcessInstanceName().isEmpty()) {
			return wfc.getLocalizableProcessInstanceName().stream()
					.map(p -> WebComponentUtil.resolveLocalizableMessage(p, component))
					.collect(Collectors.joining(" "));
		} else {
			return null;
		}
	}

	public static String getLocalizedTaskName(WfContextType wfc, Component component) {
		if (wfc != null && wfc.getLocalizableTaskName() != null) {
			return WebComponentUtil.resolveLocalizableMessage(wfc.getLocalizableTaskName(), component);
		} else {
			return null;
		}
	}
}
