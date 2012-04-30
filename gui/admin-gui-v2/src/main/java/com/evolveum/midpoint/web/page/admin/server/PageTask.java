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

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class PageTask extends PageAdminTasks {

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
        //todo implement
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
        mainForm.add(type);

        AjaxLink browse = new AjaxLink("browse") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                browsePerformed(target);
            }
        };
        mainForm.add(browse);

        TextField<String> name = new TextField<String>("name", new Model<String>()); //todo to model
        mainForm.add(name);

        CheckBox immediately = new CheckBox("immediately", new Model<Boolean>()); //todo model to dto
        mainForm.add(immediately);

        TextField<Integer> interval = new TextField<Integer>("interval", new Model<Integer>()); //todo to model
        mainForm.add(interval);

        TextField<String> cron = new TextField<String>("cron", new Model<String>()); //todo to model
        mainForm.add(cron);

        CheckBox tightlyBound = new CheckBox("tightlyBound", new Model<Boolean>()); //todo model to dto
        mainForm.add(tightlyBound);

        CheckBox runUntilNodeDown = new CheckBox("runUntilNodeDown", new Model<Boolean>()); // todo to model
        mainForm.add(runUntilNodeDown);

        initButtons(mainForm);
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

        //todo change to something better and add i18n
        TaskManager manager = getTaskManager();
        List<String> list  =manager.getAllTaskCategories();
        if (list != null) {
            Collections.sort(list);

            categories.addAll(list);
        }

        return categories;
    }

    private void savePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void browsePerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
