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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author lazyman, mserbak
 */
public class PageTask extends PageAdminTasks {
	private static final long serialVersionUID = 2317887071933841581L;
	
	public static final String PARAM_TASK_ID = "taskOid";
	private IModel<TaskType> model;

	public PageTask() {
		model = new LoadableModel<TaskType>(false) {

			@Override
			protected TaskType load() {
				return loadTask();
			}
		};
		initLayout();
	}

	private TaskType loadTask() {
		// todo implement
		return new TaskType();
	}

	private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        DropDownChoice type = new DropDownChoice("type", new Model(), new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return createCategoryList();
            }
        });
        type.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        mainForm.add(type);

        AjaxLink browse = new AjaxLink("browse") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browsePerformed(target);
            }
        };
        mainForm.add(browse);
        
        TextField<String> name = new TextField<String>("name", new Model<String>()); //todo to model
        name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        mainForm.add(name);
        
        initScheduling(mainForm);
        
        CheckBox runUntilNodeDown = new CheckBox("runUntilNodeDown", new Model<Boolean>()); // todo to model
        runUntilNodeDown.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        mainForm.add(runUntilNodeDown);
        
        initButtons(mainForm);
    }
	
	private void initScheduling(final Form mainForm){
		final WebMarkupContainer boundContainer = new WebMarkupContainer("boundContainer");
		boundContainer.setVisible(false);
		mainForm.add(boundContainer);
        
        
        final WebMarkupContainer intervalContainer = new WebMarkupContainer("intervalContainer");
        intervalContainer.setVisible(false);
        mainForm.add(intervalContainer);
        
        final WebMarkupContainer cronContainer = new WebMarkupContainer("cronContainer");
        cronContainer.setVisible(false);
        mainForm.add(cronContainer);
        
        
		final IModel<Boolean> recurringCheck = new Model<Boolean>(false);
		final IModel<Boolean> boundCheck = new Model<Boolean>(false);
		
        AjaxCheckBox recurring = new AjaxCheckBox("recurring", recurringCheck){

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            	boundContainer.setVisible(recurringCheck.getObject());
            	intervalContainer.setVisible(recurringCheck.getObject());
            	if(recurringCheck.getObject() && !boundCheck.getObject()){
            		cronContainer.setVisible(true);
            	} else {
            		cronContainer.setVisible(false);
            	}
            	target.add(mainForm);
            }
        };
        recurring.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        mainForm.add(recurring);

        AjaxCheckBox bound = new AjaxCheckBox("bound", boundCheck){

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            	if(recurringCheck.getObject() && !boundCheck.getObject()){
            		cronContainer.setVisible(true);
            	} else {
            		cronContainer.setVisible(false);
            	}
            	target.add(mainForm);
            }
        };
        bound.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        boundContainer.add(bound);
        
        TextField<Integer> interval = new TextField<Integer>("interval", new Model<Integer>()); //todo to model
        interval.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        intervalContainer.add(interval);

        TextField<String> cron = new TextField<String>("cron", new Model<String>()); //todo to model
        cron.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        cronContainer.add(cron);
        
        final DateTimeField notStartBefore = new DateTimeField("notStartBeforeField"){
        	@Override 
            protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) { 
                    return DateTextField.forDatePattern(id, dateFieldModel,"dd/MMM/yyyy"); 
            } 
        };
        notStartBefore.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        notStartBefore.setOutputMarkupId(true);
        mainForm.add(notStartBefore);
        
		final DateTimeField notStartAfter = new DateTimeField("notStartAfterField"){
        	@Override 
            protected DateTextField newDateTextField(String id, PropertyModel dateFieldModel) { 
                    return DateTextField.forDatePattern(id, dateFieldModel,"dd/MMM/yyyy"); 
            }
        	
        };
        notStartAfter.add(new AjaxFormComponentUpdatingBehavior("onChange") {
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
			}
		});
        notStartAfter.setOutputMarkupId(true);
        mainForm.add(notStartAfter);
	}

	private void initButtons(final Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
				createStringResource("pageTask.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);

		AjaxLinkButton backButton = new AjaxLinkButton("backButton",
				createStringResource("pageTask.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageTasks.class);
			}
		};
		mainForm.add(backButton);
	}

	private List<String> createCategoryList() {
		List<String> categories = new ArrayList<String>();

		// todo change to something better and add i18n
		TaskManager manager = getTaskManager();
		List<String> list = manager.getAllTaskCategories();
		if (list != null) {
			Collections.sort(list);
			for (int i = 0; i < list.size(); i++) {
				StringResourceModel item = createStringResource("pageTask.category." + list.get(i));
				if (list.get(i) != "ImportFromFile" && list.get(i) != "Workflow") {
					categories.add(item.getString());
				}
			}
		}

		return categories;
	}

	private void savePerformed(AjaxRequestTarget target) {
		// todo implement
	}

	private void browsePerformed(AjaxRequestTarget target) {
		// todo implement
	}
	
	private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }
}
