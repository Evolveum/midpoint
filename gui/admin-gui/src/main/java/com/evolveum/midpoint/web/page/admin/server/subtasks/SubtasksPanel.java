/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.subtasks;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.TaskDtoTablePanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.session.TasksStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author mederly
 */
public class SubtasksPanel extends BasePanel<List<TaskDto>> {

    private static final String ID_SUBTASKS_TABLE = "subtasksTable";
    
    private boolean workflowsEnabled;

    public SubtasksPanel(String id, IModel<List<TaskDto>> model, boolean workflowsEnabled) {
        super(id, model);
        this.workflowsEnabled = workflowsEnabled;
        
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    private void initLayout() {
    	
    	TaskDtoTablePanel table = new TaskDtoTablePanel(ID_SUBTASKS_TABLE) {
    		@Override
    		protected S_AtomicFilterExit customizedObjectQuery(S_AtomicFilterEntry q) {
    			List<String> oids= new ArrayList<String>();
    			for(TaskDto taskDto : SubtasksPanel.this.getModelObject()) {
    				oids.add(taskDto.getOid());
    			}
    			String[] arrayOfOid = new String[oids.size()];
    			return q.id(oids.toArray(arrayOfOid));
    		}
    		
    		@Override
    		protected TasksStorage getTaskStorage() {
    			return getPageBase().getSessionStorage().getSubtasks();
    		}
    		
    		@Override
    		protected boolean isVisibleShowSubtask() {
    			return false;
    		}
    		
    		@Override
    		protected boolean defaultShowSubtasks() {
    			return true;
    		}
    		
    		@Override
    		protected List<IColumn<TaskDto, String>> initCustomTaskColumns() {
    			List<IColumn<TaskDto, String>> columns = new ArrayList<>();
    	        columns.add(createTaskKindColumn());
    	        columns.add(TaskDtoTablePanel.createTaskExecutionStatusColumn(this, "SubtasksPanel.label.executionState"));
    	        columns.add(TaskDtoTablePanel.createProgressColumn(getPageBase(), "SubtasksPanel.label.progress",
			            this::isProgressComputationEnabled));
    	        columns.add(TaskDtoTablePanel.createTaskResultStatusColumn(this, "SubtasksPanel.label.result"));
    			return columns;
    		}
    	};
    	add(table);

    }
    
    public EnumPropertyColumn createTaskKindColumn() {
		return new EnumPropertyColumn(createStringResource("SubtasksPanel.label.kind"), TaskDto.F_TASK_KIND) {

			@Override
			protected String translate(Enum en) {
				return createStringResource(en).getString();
			}
		};
	}
    
}
