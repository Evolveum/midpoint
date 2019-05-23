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
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowDetailsTabPanel extends AbstractObjectTabPanel<ShadowType> {

	private static final long serialVersionUID = 1L;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(ShadowDetailsTabPanel.class);
	
	private static final String ID_ATTRIBUTES = "attributes";
	private static final String ID_ASSOCIATIONS = "associations";
	private static final String ID_ACTIVATION = "activation";
	private static final String ID_PASSWORD = "password";
	private static final String ID_ERROR = "error";


	public ShadowDetailsTabPanel(String id, Form<PrismObjectWrapper<ShadowType>> mainForm,
								 LoadableModel<PrismObjectWrapper<ShadowType>> objectWrapperModel) {
		super(id, mainForm, objectWrapperModel);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		try {
			
			
			ShadowType shadowType = getObjectWrapper().getObject().getRealValue();
			
			Panel attributesPanel = getPageBase().initItemPanel(ID_ATTRIBUTES, ShadowAttributesType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ShadowType.F_ATTRIBUTES), 
					itemWrapper -> WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType));
			add(attributesPanel);
    		
    		Panel associationsPanel = getPageBase().initItemPanel(ID_ASSOCIATIONS, ShadowAssociationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ShadowType.F_ASSOCIATION), 
    				itemWrapper -> WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType));
    		add(associationsPanel);
    		
    		
    		Panel activationPanel = getPageBase().initItemPanel(ID_ACTIVATION, ActivationType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ShadowType.F_ACTIVATION), 
    				itemWrapper -> WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType));
    		add(activationPanel);
			
    		Panel passwordPanel = getPageBase().initItemPanel(ID_PASSWORD, PasswordType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)), 
    				itemWrapper -> WebComponentUtil.checkShadowActivationAndPasswordVisibility(itemWrapper, shadowType));
    		add(passwordPanel);
		} catch (SchemaException e) {
			getSession().error("Cannot create panels for shadow, reason: " + e.getMessage());
			LOGGER.error("Cannot create panels for shadow, reason: {}", e.getMessage(), e);
			
		}
	}

	
}
