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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 *  Unfinished.
 */
public class InternalsMemoryPanel extends BasePanel<Void> {
	private static final long serialVersionUID = 1L;

	private static final String ID_INFORMATION = "information";
	private static final String ID_SHOW = "show";

	private static final String OPERATION_GET_MEMORY_INFORMATION = InternalsMemoryPanel.class.getName() + ".getMemoryInformation";

	InternalsMemoryPanel(String id) {
		super(id);
		initLayout();
	}

	private IModel<String> informationModel = Model.of((String) null);

	@SuppressWarnings("Duplicates")
	private void initLayout() {

		setOutputMarkupId(true);

		AceEditor informationTextArea = new AceEditor(ID_INFORMATION, informationModel);
		informationTextArea.setReadonly(true);
		informationTextArea.setResizeToMaxHeight(true);
		informationTextArea.setMode(null);
		informationTextArea.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return informationModel.getObject() != null;
			}
		});
		add(informationTextArea);

		add(new AjaxButton(ID_SHOW, createStringResource("InternalsMemoryPanel.button.show")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				executeShow(target);
			}
		});
	}

	private void executeShow(AjaxRequestTarget target) {
		Task task = getPageBase().createSimpleTask(OPERATION_GET_MEMORY_INFORMATION);
		OperationResult result = task.getResult();

		try {
			String information = getPageBase().getModelDiagnosticService().getMemoryInformation(task, result);
			informationModel.setObject(information);
		} catch (Throwable t) {
			result.recordFatalError(getString("InternalsMemoryPanel.message.executeShow.fatalError"), t);
			informationModel.setObject(ExceptionUtil.printStackTrace(t));
		} finally {
			result.computeStatusIfUnknown();
		}
		getPageBase().showResult(result);
		target.add(this, getPageBase().getFeedbackPanel());
	}
}
