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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.JasperReportConfigurationPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportConfigurationPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.Base64Model;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.RawValidationError;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private static final String OPERATION_VALIDATE_REPORT = DOT_CLASS + "validateReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";

    private LoadableModel<ReportDto> model;

    public PageReport() {
        model = new LoadableModel<ReportDto>(false) {

            @Override
            protected ReportDto load() {
                return loadReport();
            }
        };

        initLayout();
    }

    private ReportDto loadReport() {
        StringValue reportOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

        OperationResult result = new OperationResult(OPERATION_LOAD_REPORT);
        PrismObject<ReportType> prismReport = WebModelUtils.loadObject(ReportType.class, reportOid.toString(), result, this);

        if (prismReport == null) {
            LOGGER.error("Couldn't load report.");
            throw new RestartResponseException(PageReports.class);
        }
        
        return new ReportDto(prismReport.asObjectable());

//        return prismReport;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageReport.basic")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ReportConfigurationPanel(panelId, model);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageReport.jasperTemplate")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
            	return new JasperReportConfigurationPanel(panelId, model);
//                IModel<String> title = PageReport.this.createStringResource("PageReport.jasperTemplate");
//                IModel<String> data = new Base64Model(new PrismPropertyModel<>(model, ReportType.F_TEMPLATE));
//                return new AceEditorPanel(panelId, title, data);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageReport.jasperTemplateStyle")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                IModel<String> title = PageReport.this.createStringResource("PageReport.jasperTemplateStyle");
                IModel<String> data = new Base64Model(new PropertyModel(model, "templateStyle"));
                return new AceEditorPanel(panelId, title, data);
            }
        });
//        tabs.add(new AbstractTab(createStringResource("PageReport.fullXml")) {
//
//            @Override
//            public WebMarkupContainer getPanel(String panelId) {
//                IModel<String> title = PageReport.this.createStringResource("PageReport.fullXml");
//
//                AceEditorPanel panel = new AceEditorPanel(panelId, title, createFullXmlModel());
//                panel.getEditor().add(createFullXmlValidator());
//                return panel;
//            }
//        });

        TabbedPanel reportTabPanel = new TabbedPanel(ID_TAB_PANEL, tabs){
            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        super.onError(target, form);

                        target.add(getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                    }
                };
            }
        };
        reportTabPanel.setOutputMarkupId(true);

        mainForm.add(reportTabPanel);

        initButtons(mainForm);
    }

    private IValidator<String> createFullXmlValidator() {
        return new IValidator<String>() {

            @Override
            public void validate(IValidatable<String> validatable) {
                String value = validatable.getValue();

                OperationResult result = new OperationResult(OPERATION_VALIDATE_REPORT);
                Holder<PrismObject<ReportType>> reportHolder = new Holder<>(null);

                try {
                    validateObject(value, reportHolder, true, result);

                    if(!result.isAcceptable()){
                        result.recordFatalError("Could not validate object", result.getCause());
                        validatable.error(new RawValidationError(new OpResult(result)));
                    }
                } catch (Exception e){
                    LOGGER.error("Validation problem occured." + e.getMessage());
                    result.recordFatalError("Could not validate object.", e);
                    validatable.error(new RawValidationError(new OpResult(result)));
                }
            }
        };
    }

    private IModel<String> createFullXmlModel() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                PrismObject report = model.getObject().getObject();
                if (report == null) {
                    return null;
                }

                try {
                    return getPrismContext().serializeObjectToString(report, PrismContext.LANG_XML);
                } catch (SchemaException ex) {
                    getSession().error(getString("PageReport.message.cantSerializeFromObjectToString") + ex);
                    throw new RestartResponseException(PageError.class);
                }
            }

            @Override
            public void setObject(String object) {
                OperationResult result = new OperationResult(OPERATION_VALIDATE_REPORT);
                Holder<PrismObject<ReportType>> reportHolder = new Holder<>(null);

                try {
                    validateObject(object, reportHolder, true, result);
                    model.getObject().setObject(reportHolder.getValue());
                } catch (Exception e){
                    LOGGER.error("Could not set object. Validation problem occured." + result.getMessage());
                    result.recordFatalError("Could not set object. Validation problem occured,", e);
                    showResultInSession(result);
                }
            }

            @Override
            public void detach() {
            }
        };
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

            //TODO TODO TODO
            PrismObject<ReportType> newReport = model.getObject().getObject();
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
