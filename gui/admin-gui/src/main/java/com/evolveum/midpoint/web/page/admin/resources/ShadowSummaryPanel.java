/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowType;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowSummaryPanel extends ObjectSummaryPanel<ShadowType> {

	private static final long serialVersionUID = 1L;

	public ShadowSummaryPanel(String id, IModel<PrismObject<ShadowType>> model, ModelServiceLocator locator) {
		super(id, ShadowType.class, model, locator);

		initLayoutCommon(locator);
	}

	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();


		// todo implement custom layout
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON;	//todo fix
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-resource";	//todo fix
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-resource";	//todo fix
	}
}
