/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.*;

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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class TaskResultTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_OPERATION_RESULT = "operationResult";
	private static final String ID_SHOW_RESULT = "showResult";

	private static final Trace LOGGER = TraceManager.getTrace(TaskResultTabPanel.class);

	public TaskResultTabPanel(String id, Form mainForm,
			LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel,
			IModel<TaskDto> taskDtoModel, PageBase pageBase) {
		super(id, mainForm, taskWrapperModel);
		initLayout(taskDtoModel, pageBase);
		setOutputMarkupId(true);
	}

	private void initLayout(final IModel<TaskDto> taskDtoModel, final PageBase pageBase) {
		SortableDataProvider<OperationResult, String> provider = new ListDataProvider<>(this,
				new PropertyModel<List<OperationResult>>(taskDtoModel, TaskDto.F_OP_RESULT));
		TablePanel resultTablePanel = new TablePanel<>(ID_OPERATION_RESULT, provider, initResultColumns(pageBase));
		resultTablePanel.setStyle("padding-top: 0px;");
		resultTablePanel.setShowPaging(false);
		resultTablePanel.setOutputMarkupId(true);
		add(resultTablePanel);

		AjaxFallbackLink<Void> showResult = new AjaxFallbackLink<Void>(ID_SHOW_RESULT) {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onClick(Optional<AjaxRequestTarget> optionalTarget) {
				AjaxRequestTarget target = optionalTarget.get();
				OperationResult opResult = taskDtoModel.getObject().getTaskOperationResult();
				OperationResultPanel body = new OperationResultPanel(
						pageBase.getMainPopupBodyId(),
						new Model<>(OpResult.getOpResult(pageBase, opResult)),
						pageBase);
				body.setOutputMarkupId(true);
				pageBase.showMainPopup(body, target);
			}
			
			
		};
			
		add(showResult);
		
	}

	private List<IColumn<OperationResult, String>> initResultColumns(PageBase pageBase) {
		List<IColumn<OperationResult, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.token"), "token"));
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.operation"), "operation"));
		columns.add(new PropertyColumn<>(createStringResource("pageTaskEdit.opResult.status"), "status"));
		columns.add(new AbstractColumn<OperationResult, String>(createStringResource("pageTaskEdit.opResult.timestamp")){
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<OperationResult>> cellItem, String componentId,
									 IModel<OperationResult> rowModel) {
				Label label = new Label(componentId, new IModel<String>() {
					@Override
					public String getObject() {
						Long resultEndTime = rowModel.getObject().getEnd();
						return resultEndTime != null && resultEndTime > 0 ?
								WebComponentUtil.getShortDateTimeFormattedValue(new Date(), pageBase) : "";
					}
				});
				cellItem.add(label);
			}
		});
		columns.add(new AbstractColumn<OperationResult, String>(createStringResource("pageTaskEdit.opResult.message"), "message") {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<OperationResult>> cellItem, String componentId,
					IModel<OperationResult> rowModel) {
				Label label = new Label(componentId, new IModel<String>() {
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
