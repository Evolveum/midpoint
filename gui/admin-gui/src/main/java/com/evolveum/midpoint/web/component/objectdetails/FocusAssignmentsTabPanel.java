/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author semancik
 */
public class FocusAssignmentsTabPanel<F extends FocusType> extends AbstractObjectTabPanel {
	private static final long serialVersionUID = 1L;
	
	private static final String ID_ASSIGNMENTS = "assignmentsContainer";
	private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
	
	private static final String MODAL_ID_ASSIGNMENTS_PREVIEW = "assignmentsPreviewPopup";

	private static final Trace LOGGER = TraceManager.getTrace(FocusAssignmentsTabPanel.class);
	
	private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

	public FocusAssignmentsTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
		super(id, mainForm, focusWrapperModel, page);
		this.assignmentsModel = assignmentsModel;
		initLayout();
	}
	
	private void initLayout() {

		WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);
		AssignmentTablePanel panel = new AssignmentTablePanel(ID_ASSIGNMENTS_PANEL,
				createStringResource("FocusType.assignment"), assignmentsModel) {

            @Override
            protected boolean getAssignmentMenuVisibility() {
                return  !getObjectWrapper().isReadonly();
            }

            @Override
			protected boolean isShowAllAssignmentsVisible(){
				PrismContainer assignmentContainer = getObjectWrapper().getObject().findContainer(new ItemPath(FocusType.F_ASSIGNMENT));
				return assignmentContainer != null && assignmentContainer.getDefinition() != null ?
						assignmentContainer.getDefinition().canRead() : super.isShowAllAssignmentsVisible();
			}

            @Override
			protected void showAllAssignments(AjaxRequestTarget target) {
                List<AssignmentsPreviewDto> assignmentsPreviewDtos = ((PageAdminFocus) getPageBase()).recomputeAssignmentsPerformed(target);
				AssignmentPreviewDialog dialog = new AssignmentPreviewDialog(getPageBase().getMainPopupBodyId(),
                        assignmentsPreviewDtos, new ArrayList<String>(), getPageBase());
				getPageBase().showMainPopup(dialog, target);
			}

		};
		assignments.add(panel);
	}

	public boolean isAssignmentsModelChanged(){
		return getAssignmentTablePanel().isModelChanged();
	}

	private AssignmentTablePanel getAssignmentTablePanel(){
		return (AssignmentTablePanel) get(ID_ASSIGNMENTS).get(ID_ASSIGNMENTS_PANEL);
	}

}
