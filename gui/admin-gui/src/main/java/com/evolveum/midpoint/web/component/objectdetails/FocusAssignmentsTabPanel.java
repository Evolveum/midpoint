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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AbstractRoleAssignmentPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;

import java.util.List;

/**
 * @author semancik
 */
public class FocusAssignmentsTabPanel<F extends FocusType> extends AbstractObjectTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_ASSIGNMENTS = "assignmentsContainer";
	private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
	private static final String DOT_CLASS = FocusAssignmentsTabPanel.class.getName() + ".";
	private static final String OPERATION_GET_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "getAdminGuiConfiguration";

	private static final String MODAL_ID_ASSIGNMENTS_PREVIEW = "assignmentsPreviewPopup";

	private static final Trace LOGGER = TraceManager.getTrace(FocusAssignmentsTabPanel.class);

	private LoadableModel<List<AssignmentType>> assignmentsModel;

	public FocusAssignmentsTabPanel(String id, Form<?> mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel, PageBase page) {
		super(id, mainForm, focusWrapperModel, page);
		initLayout();
	}

	private void initLayout() {
		WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);
		ContainerWrapperFromObjectWrapperModel<AssignmentType, F> model = new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), new ItemPath(FocusType.F_ASSIGNMENT));
		AssignmentPanel panel = createPanel(ID_ASSIGNMENTS_PANEL, model);

		assignments.add(panel);
	}
	
	protected AssignmentPanel createPanel(String panelId, ContainerWrapperFromObjectWrapperModel<AssignmentType, F> model) {
		AbstractRoleAssignmentPanel panel = new AbstractRoleAssignmentPanel(panelId, model);
		return panel;
	}

}
