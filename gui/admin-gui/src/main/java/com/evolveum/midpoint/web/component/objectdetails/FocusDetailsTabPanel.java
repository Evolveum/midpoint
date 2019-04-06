/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public class FocusDetailsTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	private static final long serialVersionUID = 1L;

	protected static final String ID_FOCUS_FORM = "focusDetails";	
	
	private static final String ID_MAIN_PANEL = "main";
	private static final String ID_ACTIVATION_PANEL = "activation";
	private static final String ID_PASSWORD_PANEL = "password";

	private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsTabPanel.class);

	public FocusDetailsTabPanel(String id, Form mainForm,
			LoadableModel<PrismObjectWrapper<F>> focusWrapperModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel) {
		super(id, mainForm, focusWrapperModel, projectionModel);
		
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {
		
		try {
			Panel main = getPageBase().initPanel(ID_MAIN_PANEL, getObjectWrapper().getTypeName(), new PrismContainerWrapperModel<F, F>(getObjectWrapperModel(), ItemPath.EMPTY_PATH), false);
			add(main);
			Panel activation = getPageBase().initPanel(ID_ACTIVATION_PANEL, ActivationType.COMPLEX_TYPE, new PrismContainerWrapperModel<F,ActivationType>(getObjectWrapperModel(), FocusType.F_ACTIVATION), false);
			add(activation);
			Panel password = getPageBase().initPanel(ID_PASSWORD_PANEL, PasswordType.COMPLEX_TYPE, new PrismContainerWrapperModel<F, PasswordType>(getObjectWrapperModel(), ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD)), false);
			add(password);
		} catch (SchemaException e) {
			LOGGER.error("Could not create focu details panel. Reason: ", e.getMessage(), e);
		}
	}
	
	private List<ItemPath> getVisibleContainers() {
		List<ItemPath> paths = new ArrayList<>();
		paths.add(ItemPath.EMPTY_PATH);
		paths.add(SchemaConstants.PATH_ACTIVATION);
		paths.add(SchemaConstants.PATH_PASSWORD);
		if (WebModelServiceUtils.isEnableExperimentalFeature(getPageBase())) {
			paths.add(AbstractRoleType.F_DATA_PROTECTION);
		}
		return paths;
	}
	

}
