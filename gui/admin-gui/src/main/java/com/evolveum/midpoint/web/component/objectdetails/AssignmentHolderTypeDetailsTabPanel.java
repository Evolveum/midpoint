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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
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
public class AssignmentHolderTypeDetailsTabPanel<AHT extends AssignmentHolderType> extends AbstractObjectTabPanel<AHT> {
	private static final long serialVersionUID = 1L;

	protected static final String ID_FOCUS_FORM = "focusDetails";	

	private static final String ID_MAIN_PANEL = "main";
	private static final String ID_ACTIVATION_PANEL = "activation";
	private static final String ID_PASSWORD_PANEL = "password";

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderTypeDetailsTabPanel.class);

	public AssignmentHolderTypeDetailsTabPanel(String id, Form mainForm,
			LoadableModel<PrismObjectWrapper<AHT>> focusWrapperModel) {
		super(id, mainForm, focusWrapperModel);

	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {

		try {
			Panel main = getPageBase().initItemPanel(ID_MAIN_PANEL, getObjectWrapper().getTypeName(), 
					PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.EMPTY_PATH), null, true);
			add(main);
			Panel activation = getPageBase().initItemPanel(ID_ACTIVATION_PANEL, ActivationType.COMPLEX_TYPE, 
					PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ACTIVATION), null, true);
			add(activation);
			Panel password = getPageBase().initItemPanel(ID_PASSWORD_PANEL, PasswordType.COMPLEX_TYPE, 
					PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD)), null, true);
			add(password);
		} catch (SchemaException e) {
			LOGGER.error("Could not create focus details panel. Reason: {}", e.getMessage(), e);
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
