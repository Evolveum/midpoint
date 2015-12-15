/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.page.admin.configuration.dto.RepoQueryDto;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 * @author mederly
 */
@PageDescriptor(url = "/admin/config/repoQuery", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION)
})
public class PageRepoQuery extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageRepoQuery.class);

    private static final String DOT_CLASS = PageRepoQuery.class.getName() + ".";
    private static final String OPERATION_PERFORM_QUERY = "performQuery";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_EXECUTE = "execute";
    private static final String ID_EDITOR = "editor";
    private static final String ID_ANSWER = "answer";

    private IModel<RepoQueryDto> model = new Model<>(new RepoQueryDto());

    public PageRepoQuery() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        AceEditor editor = new AceEditor(ID_EDITOR, new PropertyModel<String>(model, RepoQueryDto.F_QUERY));
        mainForm.add(editor);

        AjaxSubmitButton start = new AjaxSubmitButton(ID_EXECUTE, createStringResource("PageRepoQuery.button.execute")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                queryPerformed(target);
            }
        };
        mainForm.add(start);

        TextArea answer = new TextArea(ID_ANSWER, new PropertyModel(model, RepoQueryDto.F_ANSWER));
        answer.setOutputMarkupId(true);
        answer.setEnabled(false);
        mainForm.add(answer);
    }

    private void queryPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_PERFORM_QUERY);
        OperationResult result = new OperationResult(OPERATION_PERFORM_QUERY);

        RepoQueryDto repoQueryDto = model.getObject();

        String query = repoQueryDto.getQuery();
        if(StringUtils.isEmpty(query)){
            warn(getString("PageRepoQuery.message.emptyString"));
            target.add(getFeedbackPanel());
            return;
        }

        try {
            String answer = getModelDiagnosticService().executeRepositoryQuery(query, task, result);
            repoQueryDto.setAnswer(answer);
        } catch (SecurityViolationException|SchemaException|RuntimeException e) {
            result.recordFatalError("Couldn't execute query", e);
            LoggingUtils.logException(LOGGER, "Couldn't execute query", e);
            repoQueryDto.setAnswer(e.toString());
        } finally {
            result.computeStatus();
        }

        showResult(result);
        target.add(getFeedbackPanel());
        target.add(get(createComponentPath(ID_MAIN_FORM, ID_ANSWER)));
    }
}
