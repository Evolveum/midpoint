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
package com.evolveum.midpoint.web.page.admin.resources;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.component.util.SummaryTagSimple;
import com.evolveum.midpoint.web.model.ContainerableFromPrismObjectModel;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.TaskType;

public class ResourceSummaryPanel extends ObjectSummaryPanel<ResourceType> {
	private static final long serialVersionUID = 1L;

	private static final String ID_UP_DOWN_TAG = "upDownTag";
	private IModel<PrismObject<ResourceType>> model;

	public ResourceSummaryPanel(String id, IModel<PrismObject<ResourceType>> model, ModelServiceLocator serviceLocator) {
		super(id, ResourceType.class, model, serviceLocator);
		initLayoutCommon(serviceLocator);
		this.model = model;
	}
	
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		boolean down = ResourceTypeUtil.isDown(model.getObject().asObjectable());
		IModel<ResourceType> containerModel = new ContainerableFromPrismObjectModel<>(model);
		SummaryTagSimple<ResourceType> summaryTag = new SummaryTagSimple<ResourceType>(ID_UP_DOWN_TAG, containerModel) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void initialize(ResourceType object) {
				if (!down) {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
					setLabel(getString("ResourceSummaryPanel.UP"));
				} else {
					setIconCssClass(GuiStyleConstants.CLASS_ICON_ACTIVATION_INACTIVE);
					setLabel(getString("ResourceSummaryPanel.DOWN"));
				}
			}
		};
		addTag(summaryTag);
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-resource";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-resource";
	}

	@Override
	protected boolean isIdentifierVisible() {
		return false;
	}
}
