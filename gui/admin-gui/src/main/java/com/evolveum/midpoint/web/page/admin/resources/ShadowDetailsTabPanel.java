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
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.wicket.request.resource.PackageResourceReference;

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

		PrismPanel<ShadowType> panel = new PrismPanel<>(ID_ACCOUNT, new ContainerWrapperListFromObjectWrapperModel(getObjectWrapperModel(), null),
				new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), getMainForm(),
				null, getPageBase());
		add(panel);
	}
}
