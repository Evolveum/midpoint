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
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AbstractRoleAssignmentPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
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
		ContainerWrapper<AssignmentType> assignmentsContainerWrapper = getObjectWrapper().findContainerWrapper(new ItemPath(FocusType.F_ASSIGNMENT));

		WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);

		AbstractRoleAssignmentPanel panel = new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS_PANEL, getAssignmentsListModel(assignmentsContainerWrapper),
				assignmentsContainerWrapper);
		assignments.add(panel);
	}

	private IModel<List<ContainerValueWrapper<AssignmentType>>> getAssignmentsListModel(ContainerWrapper<AssignmentType> assignmentsContainerWrapper){
		return new IModel<List<ContainerValueWrapper<AssignmentType>>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public List<ContainerValueWrapper<AssignmentType>> getObject() {
				List<ContainerValueWrapper<AssignmentType>> assignmentsList = new ArrayList<>();
				assignmentsContainerWrapper.getValues().forEach(a -> {
					if (!AssignmentsUtil.isPolicyRuleAssignment(a.getContainerValue().getValue()) && !AssignmentsUtil.isConsentAssignment(a.getContainerValue().getValue())
							&& AssignmentsUtil.isAssignmentRelevant(a.getContainerValue().getValue())) {
						assignmentsList.add(a);
					}
				});
//		Collections.sort(consentsList);
				return assignmentsList;
			}

			@Override
			public void setObject(List<ContainerValueWrapper<AssignmentType>> object){
				assignmentsContainerWrapper.getValues().clear();
				assignmentsContainerWrapper.getValues().addAll(object);
			}

			@Override
			public void detach(){};
		};
	}
}
