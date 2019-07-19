/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AceEditor;
import org.apache.wicket.model.IModel;

/**
 *  Unfinished.
 */
public class InternalsMemoryPanel extends BasePanel<Void> {
	private static final long serialVersionUID = 1L;

	private static final String ID_INFORMATION = "information";

	private static final String OPERATION_GET_MEMORY_INFORMATION = InternalsMemoryPanel.class.getName() + ".getMemoryInformation";

	InternalsMemoryPanel(String id) {
		super(id);
		initLayout();
	}

	private void initLayout() {
		AceEditor informationText = new AceEditor(ID_INFORMATION, new IModel<String>() {
			@Override
			public String getObject() {
				return getMemoryInformation();
			}

			@Override
			public void setObject(String object) {
				// nothing to do here
			}
		});
		informationText.setReadonly(true);
		informationText.setHeight(300);
		informationText.setResizeToMaxHeight(true);
		informationText.setMode(null);
		add(informationText);

	}

	@SuppressWarnings("Duplicates")
	private String getMemoryInformation() {
		try {
			Task task = getPageBase().createSimpleTask(OPERATION_GET_MEMORY_INFORMATION);
			return getPageBase().getModelDiagnosticService().getMemoryInformation(task, task.getResult());
		} catch (Throwable t) {
			// TODO show feedback message as well
			return ExceptionUtil.printStackTrace(t);
		}
	}
}
