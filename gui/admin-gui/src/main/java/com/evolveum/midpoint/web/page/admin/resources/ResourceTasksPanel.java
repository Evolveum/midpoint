package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceTasksPanel extends Panel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String ID_TASKS_TABLE = "taskTable";
	
	private static final String ID_RUN_NOW = "runNow";
	private static final String ID_RESUME = "resume";
	private static final String ID_SUSPEND = "suspend";
	
	
	private PageBase pageBase;
	
	
	public ResourceTasksPanel(String id, ListModel<TaskType> tasks, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		initLayout(tasks);
	}
	
	private void initLayout(final ListModel<TaskType> tasks){
		final ObjectListPanel<TaskType> tasksPanel = new ObjectListPanel<TaskType>(ID_TASKS_TABLE, TaskType.class, pageBase){
			
			@Override
			protected BaseSortableDataProvider<SelectableBean<TaskType>> getProvider() {
				return new ListDataProvider2(pageBase, tasks);
			}
		};
//		tasksPanel.setProvider(new ListDataProvider2(getPageBase(), tasks));
		tasksPanel.setEditable(false);
		tasksPanel.setMultiSelect(true);
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
