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
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

public class ResourceTasksPanel extends Panel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String DOT_CLASS = ResourceTasksPanel.class.getName() + ".";

	private static final String OPERATION_LOAD_TASKS = DOT_CLASS + "loadTasks";
	
	private static final String ID_TASKS_TABLE = "taskTable";
	
	private static final String ID_RUN_NOW = "runNow";
	private static final String ID_RESUME = "resume";
	private static final String ID_SUSPEND = "suspend";
	
	
	private PageBase pageBase;
	
	private boolean editable;
	
//	private ListModel<TaskType> model;
	
	
	public ResourceTasksPanel(String id, boolean editable, ListModel<TaskType> tasks, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		this.editable = editable;
		
		
		initLayout(tasks);
	}
	
	public ResourceTasksPanel(String id, boolean editable, final IModel<PrismObject<ResourceType>> resourceModel, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		this.editable = editable;
		
		ListModel<TaskType> model = createTaskModel(resourceModel.getObject());
		initLayout(model);
	}
	
	private ListModel<TaskType> createTaskModel(PrismObject<ResourceType> object) {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASKS);
		List<PrismObject<TaskType>> tasks = WebModelServiceUtils
				.searchObjects(TaskType.class,
						ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF,
								TaskType.class, pageBase.getPrismContext(),
								object.getOid())),
						result, pageBase);
		List<TaskType> tasksType = new ArrayList<TaskType>();
		for (PrismObject<TaskType> task : tasks) {
			tasksType.add(task.asObjectable());
		}
		return new ListModel<>(tasksType);
		
	}
	
	private void initLayout(final ListModel<TaskType> tasks){
		final MainObjectListPanel<TaskType> tasksPanel = new MainObjectListPanel<TaskType>(ID_TASKS_TABLE, TaskType.class, null, pageBase) {
			
			@Override
			protected BaseSortableDataProvider<SelectableBean<TaskType>> getProvider() {
				return new ListDataProvider2(pageBase, tasks);
			}
			
			@Override
			protected List<InlineMenuItem> createInlineMenu() {
				// TODO Auto-generated method stub
				return null;
			}
			
			
			@Override
			public void objectDetailsPerformed(AjaxRequestTarget target, TaskType task) {
				// TODO Auto-generated method stub
//				super.objectDetailsPerformed(target, task);
				PageParameters parameters = new PageParameters();
		        parameters.add(OnePageParameterEncoder.PARAMETER, task.getOid());
		        setResponsePage(new PageTaskEdit(parameters));
			}

			@Override
			protected void newObjectPerformed(AjaxRequestTarget target) {
				setResponsePage(PageTaskAdd.class);
				
			}

			@Override
			protected List<IColumn<SelectableBean<TaskType>, String>> createColumns() {
				return ColumnUtils.getDefaultTaskColumns();
			}
		};
//		final ObjectListPanel<TaskType> tasksPanel = new ObjectListPanel<TaskType>(ID_TASKS_TABLE, TaskType.class, pageBase){
//			
//			@Override
//			protected BaseSortableDataProvider<SelectableBean<TaskType>> getProvider() {
//				return new ListDataProvider2(pageBase, tasks);
//			}
//			
//			@Override
//			public boolean isEditable() {
//				return ResourceTasksPanel.this.editable;
//			}
//			
//			@Override
//			public void objectDetailsPerformed(AjaxRequestTarget target, TaskType task) {
//				// TODO Auto-generated method stub
//				super.objectDetailsPerformed(target, task);
//				PageParameters parameters = new PageParameters();
//		        parameters.add(OnePageParameterEncoder.PARAMETER, task.getOid());
//		        setResponsePage(new PageTaskEdit(parameters));
//			}
//		};
//		tasksPanel.setEditable(false);
		add(tasksPanel);
		
		AjaxButton runNow = new AjaxButton(ID_RUN_NOW, pageBase.createStringResource("pageTaskEdit.button.runNow")) {
		
			@Override
			public void onClick(AjaxRequestTarget target) {
				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
				
				OperationResult result = TaskOperationUtils.runNowPerformed(pageBase.getTaskService(), oids);
				pageBase.showResult(result);
				target.add(pageBase.getFeedbackPanel());
				
			}
		};
		add(runNow);
		
		AjaxButton resume = new AjaxButton(ID_RESUME, pageBase.createStringResource("pageTaskEdit.button.resume")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
				
				OperationResult result = TaskOperationUtils.resumePerformed(pageBase.getTaskService(), oids);
				pageBase.showResult(result);
				target.add(pageBase.getFeedbackPanel());
				
			}
		};
		add(resume);
		
		AjaxButton suspend = new AjaxButton(ID_SUSPEND, pageBase.createStringResource("pageTaskEdit.button.suspend")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
				
				OperationResult result = TaskOperationUtils.suspendPerformed(pageBase.getTaskService(), oids);
				pageBase.showResult(result);
				target.add(pageBase.getFeedbackPanel());
				
			}
		};
		add(suspend);
	}
	
	private ObjectListPanel<TaskType> getTaskListPanel(){
		return (ObjectListPanel<TaskType>) get(ID_TASKS_TABLE);
	}
	
	private List<String> createOidList(List<TaskType> tasks){
		List<String> oids = new ArrayList<>();
		for (TaskType task : tasks){
			oids.add(task.getOid());
		}
		return oids;
	}
	
	

}
