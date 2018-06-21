/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.GuiFeature;
import com.evolveum.midpoint.schema.util.AdminGuiConfigTypeUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

/**
 * Behaviour class that determines visibility of (configurable) UI feature.
 *
 * @author semancik
 */
public class FeatureVisibleEnableBehaviour extends VisibleEnableBehaviour {
	private static final long serialVersionUID = 1L;

	UserInterfaceElementVisibilityType visibility;

	public FeatureVisibleEnableBehaviour(GuiFeature feature, AdminGuiConfigurationType adminGuiConfig) {
		super();
		visibility = AdminGuiConfigTypeUtil.getFeatureVisibility(adminGuiConfig, feature.getUri());
	}

	@Override
	public boolean isVisible() {
		return AdminGuiConfigTypeUtil.isVisible(visibility, this::isVisibleAutomatic);
    }

	/**
	 * Method to be overridden in subclasses, determines automatic visibility of
	 * the feature. It will be called only if needed.
	 */
	public boolean isVisibleAutomatic() {
		return true;
	}
}
