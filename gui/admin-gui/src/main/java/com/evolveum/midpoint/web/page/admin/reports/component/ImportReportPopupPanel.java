/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.util.file.File;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public class ImportReportPopupPanel extends BasePanel<ReportType> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ImportReportPopupPanel.class);

    private static final String DOT_CLASS = ImportReportPopupPanel.class.getName() + ".";
    private static final String OPERATION_CREATE_REPORT_DATA = DOT_CLASS + "createReportData";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_POPUP_FEEDBACK = "popupFeedback";
    private static final String ID_IMPORT_BUTTON = "import";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_CHOSE_FILE = "choseFile";
    private static final String ID_NAME_FOR_DATA = "reportDataName";
    private static final String ID_FILE_AS_NAME = "fileAsString";

    private static final String CSV_SUFFIX = ".csv"; //Import report now support only csv format, so we use it


    public ImportReportPopupPanel(String id, @NotNull ReportType report) {
        super(id, Model.of(report));
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
        feedback.setOutputMarkupId(true);
        mainForm.add(feedback);

        Model<String> nameModel = Model.of("");
        TextPanel nameField = new TextPanel(ID_NAME_FOR_DATA, nameModel);
        nameField.setOutputMarkupId(true);
        mainForm.add(nameField);

        Model<String> fileStringModel = Model.of("");
        TextAreaPanel fileStringField = new TextAreaPanel(ID_FILE_AS_NAME, fileStringModel, 5);
        fileStringField.setOutputMarkupId(true);
        mainForm.add(fileStringField);

        AjaxSubmitButton importButton = new AjaxSubmitButton(ID_IMPORT_BUTTON,
                createStringResource("PageReports.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                importConfirmPerformed(target, nameModel, fileStringModel);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                FeedbackAlerts feedback = (FeedbackAlerts) getForm().get(ID_POPUP_FEEDBACK);
                target.add(feedback);
            }
        };
        mainForm.add(importButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ImportReportPopupPanel.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        mainForm.add(cancelButton);

        FileUploadField choseFile = new FileUploadField(ID_CHOSE_FILE);
        mainForm.add(choseFile);

    }

    private String getDataTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss.SSS");
        return formatDate.format(createDate);
    }

    private void importConfirmPerformed(AjaxRequestTarget target, Model<String> nameModel, Model<String> fileStringImport) {
        String dataName;
        if (nameModel == null || StringUtils.isEmpty(nameModel.getObject())) {

            dataName = getModelObject().getName().getOrig() + "-IMPORT " + getDataTime();
        } else {
            dataName = nameModel.getObject();
        }

        // Create new file
        MidPointApplication application = getPageBase().getMidpointApplication();
        WebApplicationConfiguration config = application.getWebApplicationConfiguration();
        String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        File importDir = new File(midpointHome, "import");
        if (!importDir.exists() || !importDir.isDirectory()) {
            if (!importDir.mkdir()) {
                LOGGER.error("Couldn't create import dir {}", importDir);
                FeedbackAlerts feedback = getFeedbackAlertsPanel();
                feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.createImportDir", importDir).getString());
                target.add(feedback);
                return;
            }
        }

        FileUpload uploadedFile = getUploadedFile();

        if (uploadedFile == null && StringUtils.isEmpty(fileStringImport.getObject())) {
            LOGGER.error("Please upload file for import");
            FeedbackAlerts feedback = getFeedbackAlertsPanel();
            feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.uploadFile", importDir).getString());
            target.add(feedback);
            return;
        }

        String newFilePath;
        if (uploadedFile != null) {
            String fileName = FilenameUtils.removeExtension(uploadedFile.getClientFileName()) + " " + getDataTime()
                    + CSV_SUFFIX;
            File newFile = new File(importDir, fileName);
            // Check new file, delete if it already exists
            if (newFile.exists()) {
                newFile.delete();
            }
            // Save file

            try {
                newFile.createNewFile();
                FileUtils.copyInputStreamToFile(uploadedFile.getInputStream(), newFile);
                newFilePath = newFile.getAbsolutePath();
            } catch (IOException e) {
                LOGGER.error("Couldn't create new file " + newFile.getAbsolutePath(), e);
                FeedbackAlerts feedback = getFeedbackAlertsPanel();
                feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.createImportFile", newFile.getAbsolutePath()).getString());
                target.add(feedback);
                return;
            }
        } else {
            newFilePath = new File(importDir, dataName + CSV_SUFFIX).getAbsolutePath();
            try {
                Files.write(Paths.get(newFilePath), fileStringImport.getObject().getBytes());
            } catch (IOException e) {
                LOGGER.error("Couldn't create new file " + newFilePath, e);
                FeedbackAlerts feedback = getFeedbackAlertsPanel();
                feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.createImportFile", newFilePath).getString());
                target.add(feedback);
                return;
            }
        }

        ReportDataType reportImportData = null;
        try {
            @NotNull PrismObject<ReportDataType> prismObject = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ReportDataType.class).instantiate();
            reportImportData = prismObject.asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't instantiate new Report Data from definition", e);
            FeedbackAlerts feedback = getFeedbackAlertsPanel();
            feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.createInstantiateReportData").getString());
            target.add(feedback);
            return;
        }
        reportImportData.setName(new PolyStringType(dataName));
        reportImportData.setFilePath(newFilePath);
        ObjectReferenceType reportRef = new ObjectReferenceType();
        reportRef.setType(ReportType.COMPLEX_TYPE);
        reportRef.setOid(getModelObject().getOid());
        reportImportData.setReportRef(reportRef);
        Collection<ObjectDelta<? extends ObjectType>> deltas = Collections.singleton(reportImportData.asPrismObject().createAddDelta());
        Task task = getPageBase().createSimpleTask(OPERATION_CREATE_REPORT_DATA);
        try {
            Collection<ObjectDeltaOperation<? extends ObjectType>> retDeltas = getPageBase().getModelService().executeChanges(deltas, null, task, task.getResult());
            reportImportData = (ReportDataType) retDeltas.iterator().next().getObjectDelta().getObjectToAdd().asObjectable();
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Report Data with name " + dataName + " already exists", e);
            FeedbackAlerts feedback = getFeedbackAlertsPanel();
            feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.importReportDataAlreadyExists", dataName).getString());
            target.add(feedback);
            return;
        } catch (Exception e) {
            LOGGER.error("Couldn't create new Report Data with name " + dataName, e);
            FeedbackAlerts feedback = getFeedbackAlertsPanel();
            feedback.error(getPageBase().createStringResource("ImportReportPopupPanel.message.error.createImportReportData", dataName).getString());
            target.add(feedback);
            return;
        }

        importConfirmPerformed(target, reportImportData);
    }

    protected void importConfirmPerformed(AjaxRequestTarget target, ReportDataType reportImportData) {
    }

    private FileUpload getUploadedFile() {
        FileUploadField file = (FileUploadField) get(
                createComponentPath(ID_MAIN_FORM, ID_CHOSE_FILE));
        return file.getFileUpload();
    }

    private FeedbackAlerts getFeedbackAlertsPanel() {
        return (FeedbackAlerts) get(createComponentPath(ID_MAIN_FORM, ID_POPUP_FEEDBACK));
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 100;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("ImportReportPopupPanel.title");
    }
}
