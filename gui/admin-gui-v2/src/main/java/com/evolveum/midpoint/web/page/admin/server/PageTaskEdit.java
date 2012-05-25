/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ClusterStatusInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author lazyman
 */
public class PageTaskEdit extends PageAdminTasks {
	private static final String DOT_CLASS = PageTaskAdd.class.getName() + ".";
	public static final String PARAM_TASK_ID = "taskOid";
	private static final String OPERATION_LOAD_TASK = DOT_CLASS + "loadTask";
	private static final String OPERATION_SAVE_TASK = DOT_CLASS + "saveTask";
	private static final long ALLOWED_CLUSTER_INFO_AGE = 1200L;
	private IModel<TaskDto> model;
	private static boolean edit;

	public PageTaskEdit() {
		model = new LoadableModel<TaskDto>() {

			@Override
			protected TaskDto load() {
				return loadTask();
			}
		};
		initLayout();
	}
	
	private TaskDto loadTask() {
		OperationResult result = new OperationResult(OPERATION_LOAD_TASK);
		Task loadedTask = null;
		TaskManager manager = null;
		try {
			manager = getTaskManager();
			StringValue taskOid = getPageParameters().get(PARAM_TASK_ID);
			loadedTask = manager.getTask(taskOid.toString(), result);
			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get task.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}
		
		if (loadedTask == null) {
            getSession().error(getString("pageTaskEdit.message.cantTaskDetails"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw new RestartResponseException(PageResources.class);
        }
		ClusterStatusInformation info = manager.getRunningTasksClusterwide(ALLOWED_CLUSTER_INFO_AGE, result);
		return new TaskDto(loadedTask, info, manager);
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		//mainForm.add(new editableColumn(model, "name"));
		

		TextField<String> name = new TextField<String>("name", new PropertyModel<String>(model, "name"));
		mainForm.add(name);
		
		TextField<String> oid = new TextField<String>("oid", new PropertyModel<String>(model, "oid"));
		mainForm.add(oid);
		
	}

	private static class editableColumn<T extends Editable> extends EditablePropertyColumn<T> {

		public editableColumn(IModel<String> displayModel, String propertyExpression) {
			super(displayModel, propertyExpression);
		}

		@Override
		protected boolean isEditing(IModel<T> rowModel) {
			return edit;
		}

		@Override
		protected InputPanel createInputPanel(String componentId, IModel iModel) {
			InputPanel panel = super.createInputPanel(componentId, iModel);
			panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

			return panel;
		}

		/*
		 * private static class editableColumn<T extends Editable> extends
		 * PropertyColumn<T>{ String propertyExpression; IModel<String> model;
		 * 
		 * private editableColumn(IModel<String> model, String
		 * propertyExpression) { super(model, propertyExpression);
		 * this.propertyExpression = propertyExpression; this.model = model; }
		 * 
		 * protected boolean isEditing() { return edit; }
		 * 
		 * protected InputPanel createInputPanel(String componentId, IModel
		 * model) { InputPanel panel = new TextPanel(componentId, new
		 * PropertyModel(model, propertyExpression));
		 * panel.getBaseFormComponent().add(new
		 * EmptyOnBlurAjaxFormUpdatingBehaviour()); return panel; } }
		 */

		private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

			public EmptyOnBlurAjaxFormUpdatingBehaviour() {
				super("onBlur");
			}

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
			}
		}
	}
}
