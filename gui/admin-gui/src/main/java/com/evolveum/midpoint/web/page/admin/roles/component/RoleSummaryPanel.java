/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.roles.component;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author semancik
 *
 */
public class RoleSummaryPanel extends FocusSummaryPanel<RoleType> {
	private static final long serialVersionUID = 8087858942603720878L;

	public RoleSummaryPanel(String id, IModel model, ModelServiceLocator serviceLocator) {
		super(id, RoleType.class, model, serviceLocator);
	}

	@Override
	protected QName getDisplayNamePropertyName() {
		return RoleType.F_NAME;			// TODO F_DISPLAY_NAME ?
	}

	@Override
	protected QName getTitlePropertyName() {
		return RoleType.F_ROLE_TYPE;
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-role";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-role";
	}

}
