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

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author mederly
 */
public class ApprovalProcessesPreviewPanel extends BasePanel<List<ApprovalProcessExecutionInformationDto>> {

	private static final String ID_PROCESSES = "processes";
	private static final String ID_NAME = "name";
	private static final String ID_PREVIEW = "preview";

	public ApprovalProcessesPreviewPanel(String id, IModel<List<ApprovalProcessExecutionInformationDto>> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		ListView<ApprovalProcessExecutionInformationDto> list = new ListView<ApprovalProcessExecutionInformationDto>(ID_PROCESSES, getModel()) {
			@Override
			protected void populateItem(ListItem<ApprovalProcessExecutionInformationDto> item) {
				item.add(new Label(ID_NAME, LoadableModel.create(() -> {
					String targetName = item.getModelObject().getTargetName();
					if (targetName != null) {
						return ApprovalProcessesPreviewPanel.this.getString("ApprovalProcessesPreviewPanel.processRelatedTo", targetName);
					} else {
						return getString("ApprovalProcessesPreviewPanel.process");
					}
				}, false)));
				item.add(new ApprovalProcessExecutionInformationPanel(ID_PREVIEW, item.getModel()));
			}
		};
		add(list);
	}

}
