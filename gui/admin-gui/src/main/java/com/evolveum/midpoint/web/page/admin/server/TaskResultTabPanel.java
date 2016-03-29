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
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.model.PropertyWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class TaskResultTabPanel extends AbstractObjectTabPanel<TaskType> {
	private static final long serialVersionUID = 1L;

	protected static final String ID_RECURRENCE = "recurrence";

	private static final Trace LOGGER = TraceManager.getTrace(TaskResultTabPanel.class);

	public TaskResultTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		initLayout(pageBase);
	}
	
	private void initLayout(PageBase pageBase) {

		PrismPropertyPanel recurrencePanel = new PrismPropertyPanel<>(ID_RECURRENCE,
				new PropertyWrapperFromObjectWrapperModel(getObjectWrapperModel(), TaskType.F_RECURRENCE),
				null, pageBase);
		recurrencePanel.setLabelContainerVisible(false);
		add(recurrencePanel);

	}

}
