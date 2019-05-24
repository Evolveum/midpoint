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

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.ShadowPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;


/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowDetailsTabPanel extends AbstractObjectTabPanel<ShadowType> {

	private static final long serialVersionUID = 1L;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(ShadowDetailsTabPanel.class);
	
	private static final String ID_ACCOUNT = "account";
	

	public ShadowDetailsTabPanel(String id, Form<PrismObjectWrapper<ShadowType>> mainForm,
								 LoadableModel<PrismObjectWrapper<ShadowType>> objectWrapperModel) {
		super(id, mainForm, objectWrapperModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}
	
	

	private void initLayout() {
		ShadowPanel shadowPanel = new ShadowPanel(ID_ACCOUNT, (IModel) getObjectWrapperModel());
		add(shadowPanel);
	}

	
}
