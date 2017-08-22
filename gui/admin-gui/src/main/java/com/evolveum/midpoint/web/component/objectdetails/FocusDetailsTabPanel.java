/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.List;

import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class FocusDetailsTabPanel<F extends FocusType> extends AbstractFocusTabPanel<F> {
	private static final long serialVersionUID = 1L;
	
	protected static final String ID_FOCUS_FORM = "focusDetails";
	
	private static final Trace LOGGER = TraceManager.getTrace(FocusDetailsTabPanel.class);

	public FocusDetailsTabPanel(String id, Form mainForm, 
			LoadableModel<ObjectWrapper<F>> focusWrapperModel, 
			LoadableModel<List<AssignmentDto>> assignmentsModel, 
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
			PageBase pageBase) {
		super(id, mainForm, focusWrapperModel, assignmentsModel, projectionModel, pageBase);
		initLayout();
	}
	
	private void initLayout() {

		PrismObjectPanel<F> panel = new PrismObjectPanel<F>(ID_FOCUS_FORM, getObjectWrapperModel(),
				new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), getMainForm(), getPageBase());
		add(panel);
	}

}
