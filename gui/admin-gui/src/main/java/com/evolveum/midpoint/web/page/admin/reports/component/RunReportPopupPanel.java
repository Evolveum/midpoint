/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

import org.jetbrains.annotations.NotNull;

public class RunReportPopupPanel extends BasePanel<ReportType> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RunReportPopupPanel.class);

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_RUN_BUTTON = "runReport";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_POPUP_FEEDBACK = "popupFeedback";

    private static final String ID_PARAMETERS = "parameters";
    private static final String ID_PARAMETER = "parameter";
    private static final String ID_TABLE = "table";

    private final boolean isRunnable;

    public RunReportPopupPanel(String id, final ReportType reportType) {
        this(id, reportType, true);
    }

    public RunReportPopupPanel(String id, final ReportType reportType, boolean isRunnable) {
        super(id, Model.of(reportType));
        this.isRunnable = isRunnable;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        FeedbackAlerts feedback = new FeedbackAlerts(ID_POPUP_FEEDBACK);
        ReportObjectsListPanel table = new ReportObjectsListPanel(ID_TABLE, getModel()){

            private final boolean checkViewAfterInitialize = true;
            @Override
            public Component getFeedbackPanel() {
                return feedback;
            }

            @Override
            protected boolean checkViewAfterInitialize() {
                if (checkViewAfterInitialize) {
                    return true;
                }
                return super.checkViewAfterInitialize();
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
        feedback.setFilter(new ComponentFeedbackMessageFilter(table) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean accept(FeedbackMessage message) {
                return true;
            }
        });
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        AjaxSubmitButton runButton = new AjaxSubmitButton(ID_RUN_BUTTON,
                createStringResource("runReportPopupContent.button.run")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                FeedbackAlerts feedback = (FeedbackAlerts) getForm().get(ID_POPUP_FEEDBACK);
                target.add(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                runConfirmPerformed(target);
            }
        };
        runButton.setOutputMarkupId(true);
        runButton.add(new VisibleBehaviour(() -> isRunnable));
        mainForm.add(runButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        mainForm.add(cancelButton);
    }

    public Task createSimpleTask(String operation, PrismObject<? extends FocusType> owner) {
        Task task = getPageBase().getTaskManager().createTaskInstance(operation);

        if (owner == null) {
            MidPointPrincipal user = AuthUtil.getPrincipalUser();
            if (user == null) {
                return task;
            } else {
                owner = user.getFocus().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = AuthUtil.getPrincipalUser();
        return createSimpleTask(operation, user != null ? user.getFocus().asPrismObject() : null);
    }

    private void runConfirmPerformed(AjaxRequestTarget target) {
        PrismContainerValue<ReportParameterType> reportParamValue;
        @NotNull PrismContainer<ReportParameterType> parameterContainer;
        try {
            PrismContainerDefinition<ReportParameterType> paramContainerDef = getPrismContext().getSchemaRegistry()
                    .findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
            parameterContainer = paramContainerDef.instantiate();

            ReportParameterType reportParam = new ReportParameterType();
            reportParamValue = reportParam.asPrismContainerValue();
            reportParamValue.revive(getPrismContext());
            parameterContainer.add(reportParamValue);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create container for report parameters");
            return;
        }
        VariablesMap variablesMap = getTable().getReportVariables();
        for (SearchFilterParameterType parameter : getModelObject().getObjectCollection().getParameter()) {
            if (variablesMap.get(parameter.getName()) != null && variablesMap.getValue(parameter.getName()) != null) {
                Class<?> clazz = getPrismContext().getSchemaRegistry().determineClassForType(parameter.getType());
                QName type = getPrismContext().getSchemaRegistry().determineTypeForClass(clazz);
                if (Containerable.class.isAssignableFrom(clazz)) {
                    LOGGER.error("Couldn't create container item for parameter " + parameter);
                    continue;
                }
                MutableItemDefinition def;
                String namespaceUri = SchemaConstants.NS_REPORT_EXTENSION + "/" + AbstractReportWorkDefinitionType.F_REPORT_PARAM;
                if (Referencable.class.isAssignableFrom(clazz)) {
                    def = getPrismContext().definitionFactory().createReferenceDefinition(
                            new QName(namespaceUri, parameter.getName()), type);
                    ((MutablePrismReferenceDefinition) def).setTargetTypeName(parameter.getTargetType());
                } else {
                    List values = WebComponentUtil.getAllowedValues(parameter, getPageBase());
                    if (CollectionUtils.isNotEmpty(values)) {
                        def = getPrismContext().definitionFactory().createPropertyDefinition(
                                new QName(namespaceUri, parameter.getName()), type, values, null).toMutable();
                    } else {
                        def = getPrismContext().definitionFactory().createPropertyDefinition(
                                new QName(namespaceUri, parameter.getName()), type);
                    }
                }
                def.setDynamic(true);
                def.setRuntimeSchema(true);
                def.setMaxOccurs(1);
                def.setMinOccurs(0);
                if (parameter.getDisplay() != null) {
                    String displayName = WebComponentUtil.getTranslatedPolyString(parameter.getDisplay().getLabel());
                    def.setDisplayName(displayName);
                    String help = WebComponentUtil.getTranslatedPolyString(parameter.getDisplay().getHelp());
                    def.setHelp(help);
                }
                if (parameter.getAllowedValuesLookupTable() != null) {
                    def.setValueEnumerationRef(parameter.getAllowedValuesLookupTable().asReferenceValue());
                }

                try {
                    Item item = def.instantiate();
                    if (item instanceof PrismReference) {
                        ObjectReferenceType ref = (ObjectReferenceType) variablesMap.getValue(parameter.getName());
                        item.add(ref.asReferenceValue());
                    } else {
                        PrismPropertyValue<Object> value = getPrismContext().itemFactory().createPropertyValue();
                        value.setValue(variablesMap.getValue(parameter.getName()));
                        item.add(value);
                    }
                    reportParamValue.add(item);

                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create item for parameter " + parameter);
                }
            }
        }
        runConfirmPerformed(target, getModelObject().asPrismObject(), parameterContainer);
    }

    protected void runConfirmPerformed(AjaxRequestTarget target, PrismObject<ReportType> reportType2,
            PrismContainer<ReportParameterType> reportParam) {
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("RunReportPopupPanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }

    private ReportObjectsListPanel getTable() {
        return (ReportObjectsListPanel) get(getPageBase().createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }
}
