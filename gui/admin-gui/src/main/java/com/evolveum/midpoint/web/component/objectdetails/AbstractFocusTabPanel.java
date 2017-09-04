/*
 * Copyright (c) 2016 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public abstract class AbstractFocusTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AbstractFocusTabPanel.class);

	private LoadableModel<List<AssignmentDto>> assignmentsModel;
	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;

	public AbstractFocusTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<F>> focusWrapperModel,
			LoadableModel<List<AssignmentDto>> assignmentsModel,
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel,
			PageBase pageBase) {
		super(id, mainForm, focusWrapperModel, pageBase);
		this.assignmentsModel = assignmentsModel;
		this.projectionModel = projectionModel;
	}

	public LoadableModel<List<AssignmentDto>> getAssignmentsModel() {
		return assignmentsModel;
	}

	public LoadableModel<List<FocusSubwrapperDto<ShadowType>>> getProjectionModel() {
		return projectionModel;
	}

}
