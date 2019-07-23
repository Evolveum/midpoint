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

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.assignment.SwitchAssignmentTypePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * @author semancik
 */
public class AssignmentHolderTypeAssignmentsTabPanel<AHT extends AssignmentHolderType> extends AbstractObjectTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_ASSIGNMENTS = "assignmentsContainer";
	private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
	private static final String DOT_CLASS = AssignmentHolderTypeAssignmentsTabPanel.class.getName() + ".";

	public AssignmentHolderTypeAssignmentsTabPanel(String id, Form<?> mainForm, LoadableModel<PrismObjectWrapper<AHT>> focusWrapperModel, PageBase page) {
		super(id, mainForm, focusWrapperModel);

	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);
		PrismContainerWrapperModel<AHT, AssignmentType> model = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), AssignmentHolderType.F_ASSIGNMENT);
		SwitchAssignmentTypePanel panel = createPanel(ID_ASSIGNMENTS_PANEL, model);

		assignments.add(panel);
	}
	
	protected SwitchAssignmentTypePanel createPanel(String panelId, PrismContainerWrapperModel<AHT, AssignmentType> model) {
		SwitchAssignmentTypePanel panel = new SwitchAssignmentTypePanel(panelId, model){
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isReadonly(){
				return AssignmentHolderTypeAssignmentsTabPanel.this.isReadonly();
			}
		};
		return panel;
	}

	protected boolean isReadonly(){
		return false;
	}

}