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
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.string.StringValue;

import java.io.Serializable;

/**
 * @author shood
 */
@PageDescriptor(url = "/admin/report", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#report",
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description")})
public class PageReport<T extends Serializable> extends PageAdminReports {

    private static Trace LOGGER = TraceManager.getTrace(PageReport.class);

    private static final String DOT_CLASS = PageReport.class.getName() + ".";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadReport";
    private static final String OPERATION_SAVE_REPORT = DOT_CLASS + "saveReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_TEMPLATE_EDITOR = "templateEditor";
    private static final String ID_TEMPLATE_STYLE_EDITOR = "templateStyleEditor";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private LoadableModel<PrismObject<ReportType>> model;

    public PageReport() {
        model = new LoadableModel<PrismObject<ReportType>>(false) {

            @Override
            protected PrismObject<ReportType> load() {
                return loadReport();
            }
        };

        initLayout();
    }

    private PrismObject<ReportType> loadReport() {
        StringValue reportOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

        OperationResult result = new OperationResult(OPERATION_LOAD_REPORT);
        PrismObject<ReportType> prismReport = WebModelUtils.loadObject(ReportType.class, reportOid.toString(), result, this);

        if (prismReport == null) {
            throw new RestartResponseException(PageReports.class);
        }

        return prismReport;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        TextFormGroup name = new TextFormGroup(ID_NAME, new PrismPropertyModel<>(model, ObjectType.F_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        mainForm.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
                new PrismPropertyModel<>(model, ObjectType.F_DESCRIPTION),
                createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        mainForm.add(description);

        AceEditor templateEditor = new AceEditor(ID_TEMPLATE_EDITOR,
                new PrismPropertyModel<>(model, ReportType.F_TEMPLATE));
        mainForm.add(templateEditor);

        AceEditor templateStyleEditor = new AceEditor(ID_TEMPLATE_STYLE_EDITOR,
                new PrismPropertyModel<>(model, ReportType.F_TEMPLATE_STYLE));
        mainForm.add(templateStyleEditor);

//        List<ITab> tabs = new ArrayList<ITab>();
//        tabs.add(new AbstractTab(createStringResource("pageReport.tab.panelConfig")) {
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return new ReportConfigurationPanel(panelId, model);
//            }
//        });
//
//        tabs.add(new AbstractTab(createStringResource("pageReport.tab.aceEditor")) {
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                return initAceEditorPanel(panelId);
//            }
//        });
//
//        mainForm.add(new TabbedPanel(ID_TAB_PANEL, tabs));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSavePerformed(target);
            }
        };
        mainForm.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_CANCEL_BUTTON, createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onCancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE_REPORT);
        try {
            Task task = createSimpleTask(OPERATION_SAVE_REPORT);

            PrismObject<ReportType> newReport = model.getObject();
            PrismObject<ReportType> oldReport = WebModelUtils.loadObject(ReportType.class, newReport.getOid(),
                    result, this);

            if (oldReport != null) {
                ObjectDelta<ReportType> delta = oldReport.diff(newReport);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }
        } catch (Exception e) {
            result.recordFatalError("Couldn't save report.", e);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageReports.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    protected void onCancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageReports.class);
    }
}
