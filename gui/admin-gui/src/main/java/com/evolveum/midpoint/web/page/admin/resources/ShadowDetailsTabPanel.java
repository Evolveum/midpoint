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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_4.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowDetailsTabPanel extends AbstractObjectTabPanel<ShadowType> {

	private static final long serialVersionUID = 1L;

	private static final String ID_ACCOUNT = "account";

	public ShadowDetailsTabPanel(String id, Form<ObjectWrapper<ShadowType>> mainForm,
								 LoadableModel<ObjectWrapper<ShadowType>> objectWrapperModel, PageBase pageBase) {
		super(id, mainForm, objectWrapperModel, pageBase);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		PrismPanel<ShadowType> panel = new PrismPanel<>(ID_ACCOUNT,
				new ContainerWrapperListFromObjectWrapperModel(getObjectWrapperModel(), getVisibleContainers()),
				null, getMainForm(), null, getPageBase());
		add(panel);
	}

	private List<ItemPath> getVisibleContainers() {
		// todo maybe everything should be visible, but for now this should be fine
		return Arrays.asList(
				ShadowType.F_ATTRIBUTES,
				ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD),
				ShadowType.F_ACTIVATION
		);
	}
}
