/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author semancik
 * @author katkav
 *
 */
public class ServiceSummaryPanel extends FocusSummaryPanel<ServiceType> {
	private static final long serialVersionUID = -5457446213855746564L;

	public ServiceSummaryPanel(String id, IModel model, ModelServiceLocator serviceLocator) {
		super(id, ServiceType.class, model, serviceLocator);
	}

	@Override
	protected QName getDisplayNamePropertyName() {
		return ServiceType.F_DISPLAY_NAME;
	}

	@Override
	protected QName getTitlePropertyName() {
		return ServiceType.F_IDENTIFIER;
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-service";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-service";
	}

}
