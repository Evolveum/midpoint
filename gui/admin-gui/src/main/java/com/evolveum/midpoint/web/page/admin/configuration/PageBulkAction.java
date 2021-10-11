/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
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
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/config/bulk", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_BULK_ACTION_URL,
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
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        CheckBox async = new CheckBox(ID_ASYNC, new PropertyModel<>(model, BulkActionDto.F_ASYNC));
        mainForm.add(async);

        AceEditor editor = new AceEditor(ID_EDITOR, new PropertyModel<>(model, BulkActionDto.F_SCRIPT));
        mainForm.add(editor);

        AjaxSubmitButton start = new AjaxSubmitButton(ID_START, createStringResource("PageBulkAction.button.start")) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
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

        Object parsed = null;
        try {
            parsed = getPrismContext().parserFor(bulkActionDto.getScript()).parseRealValue();
            if (parsed == null) {
                result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.provided").getString());
            } else if (!(parsed instanceof ExecuteScriptType) && !(parsed instanceof ScriptingExpressionType)) {
                result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.notBulkAction", "{scripting-3}ScriptingExpressionType", parsed.getClass()).getString());
            }
        } catch (SchemaException|RuntimeException e) {
            result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.parse").getString(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse bulk action object", e);
        }

        if (parsed != null) {
            if (bulkActionDto.isAsync()) {
                try {
                    if (parsed instanceof ExecuteScriptType) {
                        getScriptingService().evaluateExpressionInBackground((ExecuteScriptType) parsed, task, result);
                    } else {
                        //noinspection ConstantConditions
                        getScriptingService().evaluateExpressionInBackground((ScriptingExpressionType) parsed, task, result);
                    }
                    result.recordStatus(OperationResultStatus.IN_PROGRESS, createStringResource("PageBulkAction.message.startPerformed.inProgress", task.getName()).getString());
                } catch (SchemaException | SecurityViolationException | ExpressionEvaluationException | ObjectNotFoundException
                        | CommunicationException | ConfigurationException | ClassCastException e) {
                    result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.submit").getString(), e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't submit bulk action to execution", e);
                }
            } else {
                try {
                    //noinspection ConstantConditions
                    ScriptExecutionResult executionResult =
                            parsed instanceof ExecuteScriptType ?
                                    getScriptingService().evaluateExpression((ExecuteScriptType) parsed, VariablesMap.emptyMap(),
                                            false, task, result) :
                                    getScriptingService().evaluateExpression((ScriptingExpressionType) parsed, task, result);
                    result.recordStatus(OperationResultStatus.SUCCESS, createStringResource("PageBulkAction.message.startPerformed.success", executionResult.getDataOutput().size()).getString());
                    result.addReturn("console", executionResult.getConsoleOutput());
                    result.addArbitraryObjectCollectionAsReturn("data", executionResult.getDataOutput());
                } catch (ScriptExecutionException | SchemaException | SecurityViolationException | ExpressionEvaluationException
                        | ObjectNotFoundException | CommunicationException | ConfigurationException | ClassCastException e) {
                    result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.execute").getString(), e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute bulk action", e);
                }
            }
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
