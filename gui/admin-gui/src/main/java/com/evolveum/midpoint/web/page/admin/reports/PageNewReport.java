/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;

@PageDescriptor(url = "/admin/reports/create", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_REPORT_CREATE_URL,
                label = "PageNewReport.auth.reports.label",
                description = "PageNewReport.auth.reports.description") })
public class PageNewReport extends PageAdmin {

    private static final Trace LOGGER = TraceManager.getTrace(PageNewReport.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_IMPORT_RADIO_GROUP = "importRadioGroup";
    private static final String ID_FILE_RADIO = "fileRadio";
    private static final String ID_XML_RADIO = "xmlRadio";
    private static final String ID_IMPORT_FILE_BUTTON = "importFileButton";
    private static final String ID_IMPORT_XML_BUTTON = "importXmlButton";
    private static final String ID_INPUT = "input";
    private static final String ID_INPUT_ACE = "inputAce";
    private static final String ID_ACE_EDITOR = "aceEditor";
    private static final String ID_INPUT_FILE_LABEL = "inputFileLabel";
    private static final String ID_INPUT_FILE = "inputFile";
    private static final String ID_FILE_INPUT = "fileInput";

    private static final String OPERATION_IMPORT_REPORT_XML = "Import Report from XML";
    private static final String OPERATION_IMPORT_REPORT = "Import Report from file";

    private static final Integer INPUT_FILE = 1;
    private static final Integer INPUT_XML = 2;

    private final Model<String> xmlEditorModel;

    public PageNewReport() {
        xmlEditorModel = new Model<>(null);

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        final WebMarkupContainer input = new WebMarkupContainer(ID_INPUT);
        input.setOutputMarkupId(true);
        mainForm.add(input);

        final WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
        buttonBar.setOutputMarkupId(true);
        mainForm.add(buttonBar);

        final IModel<Integer> groupModel = new Model<>(INPUT_FILE);
        RadioGroup<Integer> importRadioGroup = new RadioGroup<>(ID_IMPORT_RADIO_GROUP, groupModel);
        importRadioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(input);
                target.add(buttonBar);
            }
        });
        mainForm.add(importRadioGroup);

        Radio<Integer> fileRadio = new Radio<>(ID_FILE_RADIO, new Model<>(INPUT_FILE), importRadioGroup);
        importRadioGroup.add(fileRadio);

        Radio<Integer> xmlRadio = new Radio<>(ID_XML_RADIO, new Model<>(INPUT_XML), importRadioGroup);
        importRadioGroup.add(xmlRadio);

        WebMarkupContainer inputAce = new WebMarkupContainer(ID_INPUT_ACE);
        addVisibleForInputType(inputAce, INPUT_XML, groupModel);
        input.add(inputAce);

        AceEditor aceEditor = new AceEditor(ID_ACE_EDITOR, xmlEditorModel);
        aceEditor.setOutputMarkupId(true);
        inputAce.add(aceEditor);

        WebMarkupContainer inputFileLabel = new WebMarkupContainer(ID_INPUT_FILE_LABEL);
        addVisibleForInputType(inputFileLabel, INPUT_FILE, groupModel);
        input.add(inputFileLabel);

        WebMarkupContainer inputFile = new WebMarkupContainer(ID_INPUT_FILE);
        addVisibleForInputType(inputFile, INPUT_FILE, groupModel);
        input.add(inputFile);

        FileUploadField fileInput = new FileUploadField(ID_FILE_INPUT);
        inputFile.add(fileInput);

        initButtons(buttonBar, groupModel);
    }

    private void addVisibleForInputType(Component comp, final Integer type, final IModel<Integer> groupModel) {
        comp.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return type.equals(groupModel.getObject());
            }
        });
    }

    private void initButtons(WebMarkupContainer buttonBar, IModel<Integer> inputType) {
        AjaxSubmitButton saveFileButton = new AjaxSubmitButton(ID_IMPORT_FILE_BUTTON,
                createStringResource("PageNewReport.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                importReportFromFilePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibleForInputType(saveFileButton, INPUT_FILE, inputType);
        buttonBar.add(saveFileButton);

        AjaxSubmitButton saveXmlButton = new AjaxSubmitButton(ID_IMPORT_XML_BUTTON,
                createStringResource("PageNewReport.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                importReportFromStreamPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibleForInputType(saveXmlButton, INPUT_XML, inputType);
        buttonBar.add(saveXmlButton);
    }

    private void importReportFromFilePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_IMPORT_REPORT);

        FileUploadField file = (FileUploadField) get(createComponentPath(ID_MAIN_FORM, ID_INPUT, ID_INPUT_FILE, ID_FILE_INPUT));
        final FileUpload uploadedFile = file.getFileUpload();
        if (uploadedFile == null) {
            error(getString("PageNewReport.message.nullFile"));
            target.add(getFeedbackPanel());

            return;
        }

        InputStream stream = null;
        File newFile = null;
        try {
            // Create new file
            MidPointApplication application = getMidpointApplication();
            WebApplicationConfiguration config = application.getWebApplicationConfiguration();
            File folder = new File(config.getImportFolder());
            if (!folder.exists() || !folder.isDirectory()) {
                folder.mkdir();
            }

            newFile = new File(folder, uploadedFile.getClientFileName());
            // Check new file, delete if it already exists
            if (newFile.exists()) {
                newFile.delete();
            }
            // Save file
            newFile.createNewFile();
            FileUtils.copyInputStreamToFile(uploadedFile.getInputStream(), newFile);

            InputStreamReader reader = new InputStreamReader(new FileInputStream(newFile), StandardCharsets.UTF_8);
            stream = new ReaderInputStream(reader, reader.getEncoding());
            byte[] reportIn = IOUtils.toByteArray(stream);

            setResponsePage(new PageJasperReport(new ReportDto(Base64.encodeBase64(reportIn))));
        } catch (Exception ex) {
            result.recordFatalError(getString("PageImportObject.message.savePerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import file", ex);
        } finally {
            IOUtils.closeQuietly(stream);
            FileUtils.deleteQuietly(newFile);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void importReportFromStreamPerformed(AjaxRequestTarget target) {
        String xml = xmlEditorModel.getObject();
        if (StringUtils.isEmpty(xml)) {
            error(getString("PageNewReport.message.emptyXml"));
            target.add(getFeedbackPanel());

            return;
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_REPORT_XML);
        try {
            setResponsePage(new PageJasperReport(new ReportDto(Base64.encodeBase64(xml.getBytes()))));
        } catch (Exception ex) {
            result.recordFatalError(getString("PageNewReport.message.importReportFromStreamPerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Error occured during xml import", ex);
        }

        if (result.isSuccess()) {
            xmlEditorModel.setObject(null);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
