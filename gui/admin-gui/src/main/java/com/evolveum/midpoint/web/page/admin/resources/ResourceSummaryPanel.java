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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ResourceSummaryPanel extends ObjectSummaryPanel<ResourceType> {
	private static final long serialVersionUID = 1L;

	private static final String ID_UP_DOWN_TAG = "upDownTag";
	private IModel<ResourceType> model;

	public ResourceSummaryPanel(String id, IModel<ResourceType> model, ModelServiceLocator serviceLocator) {
		super(id, ResourceType.class, model, serviceLocator);
	}

//	@Override
//	protected void onBeforeRender() {
//		super.onBeforeRender();
//	}

	@Override
	protected List<SummaryTag<ResourceType>> getSummaryTagComponentList(){
		boolean down = ResourceTypeUtil.isDown(getModelObject());

		List<SummaryTag<ResourceType>> summaryTagList = new ArrayList<>();

		SummaryTag<ResourceType> summaryTag = new SummaryTag<ResourceType>(ID_SUMMARY_TAG, getModel()) {
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
		summaryTagList.add(summaryTag);
		return summaryTagList;
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
