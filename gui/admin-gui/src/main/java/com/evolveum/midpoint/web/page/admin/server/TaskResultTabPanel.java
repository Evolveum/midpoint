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
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author semancik
 */
public class TaskResultTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_OPERATION_RESULT = "operationResult";
	private static final String ID_SHOW_RESULT = "showResult";

	private static final Trace LOGGER = TraceManager.getTrace(TaskResultTabPanel.class);

	public TaskResultTabPanel(String id, Form mainForm,
			LoadableModel<ObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel, pageBase);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel, final PageBase pageBase) {
		SortableDataProvider<OperationResult, String> provider = new ListDataProvider<>(this,
				new PropertyModel<List<OperationResult>>(taskDtoModel, TaskDto.F_OP_RESULT));
		TablePanel resultTablePanel = new TablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns());
		resultTablePanel.setStyle("padding-top: 0px;");
		resultTablePanel.setShowPaging(false);
		resultTablePanel.setOutputMarkupId(true);
		add(resultTablePanel);

		add(new AjaxFallbackLink(ID_SHOW_RESULT) {
			@Override
            public void onClick(AjaxRequestTarget target) {
				OperationResult opResult = taskDtoModel.getObject().getTaskOperationResult();
				OperationResultPanel body = new OperationResultPanel(
						pageBase.getMainPopupBodyId(),
						new Model<>(OpResult.getOpResult(pageBase, opResult)),
						pageBase);
				body.setOutputMarkupId(true);
				pageBase.showMainPopup(body, target);
			}
		});
	}

	private List<IColumn<OperationResult, String>> initResultColumns() {
		List<IColumn<OperationResult, String>> columns = new ArrayList<IColumn<OperationResult, String>>();
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.token"), "token"));
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.status"), "status"));
		columns.add(new AbstractColumn<OperationResult, String>(createStringResource("pageTaskEdit.opResult.message"), "message") {
			@Override
			public void populateItem(Item<ICellPopulator<OperationResult>> cellItem, String componentId,
					IModel<OperationResult> rowModel) {
				Label label = new Label(componentId, new AbstractReadOnlyModel<String>() {
					@Override
					public String getObject() {
						return WebComponentUtil.nl2br(rowModel.getObject().getMessage());
					}
				});
				label.setEscapeModelStrings(false);
				cellItem.add(label);
			}
		});
		//columns.add(new PropertyColumn(createStringResource("pageTaskEdit.opResult.message"), "message"));
		return columns;
	}

	@Override
	public Collection<Component> getComponentsToUpdate() {
		return Collections.singleton(get(ID_OPERATION_RESULT));
	}

}
