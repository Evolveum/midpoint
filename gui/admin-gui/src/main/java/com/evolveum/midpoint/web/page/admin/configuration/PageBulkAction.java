/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
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
import com.evolveum.midpoint.web.page.admin.configuration.dto.BulkActionDto;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.bind.JAXBElement;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/bulk", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#bulkAction",
                label = "PageBulkAction.auth.bulkAction.label", description = "PageBulkAction.auth.bulkAction.description")
})
public class PageBulkAction extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageBulkAction.class);

    private static final String DOT_CLASS = PageBulkAction.class.getName() + ".";
    private static final String OPERATION_PERFORM_BULK = "performBulkAction";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_START = "start";
    private static final String ID_EDITOR = "editor";
    private static final String ID_ASYNC = "async";

    private IModel<BulkActionDto> model = new Model<>(new BulkActionDto());

    public PageBulkAction() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        CheckBox async = new CheckBox(ID_ASYNC, new PropertyModel<Boolean>(model, BulkActionDto.F_ASYNC));
        mainForm.add(async);

        AceEditor editor = new AceEditor(ID_EDITOR, new PropertyModel<String>(model, BulkActionDto.F_SCRIPT));
        mainForm.add(editor);

        AjaxSubmitButton start = new AjaxSubmitButton(ID_START, createStringResource("PageBulkAction.button.start")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                startPerformed(target);
            }
        };
        mainForm.add(start);
    }

    private void startPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_PERFORM_BULK);
        OperationResult result = new OperationResult(OPERATION_PERFORM_BULK);

        BulkActionDto bulkActionDto = model.getObject();

        if(StringUtils.isEmpty(bulkActionDto.getScript())){
            warn(getString("PageBulkAction.message.emptyString"));
            target.add(getFeedbackPanel());
            return;
        }

        ScriptingExpressionType expression = null;
        try {
            Object parsed = getPrismContext().parseAnyValue(bulkActionDto.getScript(), PrismContext.LANG_XML);
            if (parsed == null) {
                result.recordFatalError("No bulk action object was provided.");
            }
            if (parsed instanceof JAXBElement) {
                parsed = ((JAXBElement) parsed).getValue();
            }
            if (parsed instanceof ScriptingExpressionType) {
                expression = (ScriptingExpressionType) parsed;
            } else {
                result.recordFatalError("Provided XML text is not a bulk action object. An instance of {scripting-3}ScriptingExpressionType is expected; you have provided " + parsed.getClass() + " instead.");
            }
        } catch (SchemaException|RuntimeException e) {
            result.recordFatalError("Couldn't parse bulk action object", e);
            LoggingUtils.logException(LOGGER, "Couldn't parse bulk action object", e);
        }

        if (expression != null) {
            if (bulkActionDto.isAsync()) {
                try {
                    getScriptingService().evaluateExpressionInBackground(expression, task, result);
                    result.recordStatus(OperationResultStatus.IN_PROGRESS, task.getName() + " has been successfully submitted to execution");
                } catch (SchemaException|SecurityViolationException e) {
                    result.recordFatalError("Couldn't submit bulk action to execution", e);
                    LoggingUtils.logException(LOGGER, "Couldn't submit bulk action to execution", e);
                }
            } else {
                try {
                    ScriptExecutionResult executionResult = getScriptingService().evaluateExpression(expression, task, result);
                    result.recordStatus(OperationResultStatus.SUCCESS, "Action executed. Returned " + executionResult.getDataOutput().size() + " item(s). Console and data output available via 'Export to XML' function.");
                    result.addReturn("console", executionResult.getConsoleOutput());
                    result.addCollectionOfSerializablesAsReturn("data", executionResult.getDataOutput());
                } catch (ScriptExecutionException|SchemaException|SecurityViolationException e) {
                    result.recordFatalError("Couldn't execute bulk action", e);
                    LoggingUtils.logException(LOGGER, "Couldn't execute bulk action", e);
                }
            }
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
