/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.util.ScriptingBeansUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.api.BulkActionExecutionResult;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.configuration.dto.BulkActionDto;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/bulk", matchUrlForSecurity = "/admin/config/bulk")
        },
        action = {
        @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
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

    private final IModel<BulkActionDto> model = new Model<>(new BulkActionDto());

    public PageBulkAction() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
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

        if (StringUtils.isEmpty(bulkActionDto.getScript())) {
            warn(getString("PageBulkAction.message.emptyString"));
            target.add(getFeedbackPanel());
            return;
        }

        ExecuteScriptType typed = null;
        try {
            Object parsed = getPrismContext().parserFor(bulkActionDto.getScript()).parseRealValue();
            if (parsed == null) {
                result.recordFatalError(
                        createStringResource("PageBulkAction.message.startPerformed.fatalError.provided").getString());
            } else if (!(parsed instanceof ExecuteScriptType) && !(parsed instanceof ScriptingExpressionType)) {
                result.recordFatalError(
                        createStringResource(
                                "PageBulkAction.message.startPerformed.fatalError.notBulkAction",
                                "{scripting-3}ExecuteScriptType",
                                "{scripting-3}ScriptingExpressionType",
                                parsed.getClass()).getString());
            } else {
                typed = ScriptingBeansUtil.asExecuteScriptCommand(parsed);
            }
        } catch (SchemaException | RuntimeException e) {
            result.recordFatalError(createStringResource("PageBulkAction.message.startPerformed.fatalError.parse").getString(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse bulk action object", e);
        }

        if (typed != null) {
            if (bulkActionDto.isAsync()) {
                try {
                    getModelInteractionService().submitScriptingExpression(typed, task, result);
                    result.recordStatus(
                            OperationResultStatus.IN_PROGRESS,
                            createStringResource(
                                    "PageBulkAction.message.startPerformed.inProgress",
                                    task.getName()).getString());
                } catch (CommonException | ClassCastException e) {
                    result.recordFatalError(
                            createStringResource(
                                    "PageBulkAction.message.startPerformed.fatalError.submit").getString(), e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't submit bulk action to execution", e);
                }
            } else {
                try {
                    //noinspection ConstantConditions
                    BulkActionExecutionResult executionResult =
                            getBulkActionsService().executeBulkAction(
                                    ExecuteScriptConfigItem.of(typed, ConfigurationItemOrigin.user()),
                                    VariablesMap.emptyMap(),
                                    false,
                                    task,
                                    result);
                    result.recordStatus(
                            OperationResultStatus.SUCCESS,
                            createStringResource(
                                    "PageBulkAction.message.startPerformed.success",
                                    executionResult.getDataOutput().size()).getString());
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
