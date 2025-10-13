/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ImportOptionsPanel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;

/**
 * @author lazyman
 * @author mserbak
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/import", matchUrlForSecurity = "/admin/config/import")
        },
        action = {
        @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL, label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL, label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description") })
public class PageImportObject extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageImportObject.class);
    private static final String DOT_CLASS = PageImportObject.class.getName() + ".";
    private static final String OPERATION_IMPORT_FILE = DOT_CLASS + "importFile";
    private static final String OPERATION_IMPORT = DOT_CLASS + "import";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_IMPORT_OPTIONS = "importOptions";
    private static final String ID_IMPORT_RADIO_GROUP = "importRadioGroup";
    private static final String ID_FILE_RADIO = "fileRadio";
    private static final String ID_XML_RADIO = "xmlRadio";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_IMPORT_FILE_BUTTON = "importFileButton";
    private static final String ID_IMPORT_XML_BUTTON = "importXmlButton";
    private static final String ID_INPUT = "input";
    private static final String ID_INPUT_ACE = "inputAce";
    private static final String ID_LANGUAGE_PANEL = "languagePanel";
    private static final String ID_ACE_EDITOR = "aceEditor";
    private static final String ID_INPUT_FILE_LABEL = "inputFileLabel";
    private static final String ID_INPUT_FILE = "inputFile";
    private static final String ID_FILE_INPUT = "fileInput";

    private static final Integer INPUT_FILE = 1;
    private static final Integer INPUT_XML = 2;

    private final LoadableModel<ImportOptionsType> optionsModel;
    private final IModel<Boolean> fullProcessingModel;
    private final IModel<String> xmlEditorModel;

    private String dataLanguage;

    public PageImportObject() {
        optionsModel = new LoadableModel<ImportOptionsType>(false) {

            @Override
            protected ImportOptionsType load() {
                return MiscSchemaUtil.getDefaultImportOptions();
            }
        };
        fullProcessingModel = Model.of(Boolean.TRUE);
        xmlEditorModel = new Model<>(null);

        initLayout();
    }

    private void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setMultiPart(true);
        add(mainForm);

        ImportOptionsPanel importOptions = new ImportOptionsPanel(ID_IMPORT_OPTIONS, optionsModel, fullProcessingModel);
        mainForm.add(importOptions);

        final WebMarkupContainer input = new WebMarkupContainer(ID_INPUT);
        input.setOutputMarkupId(true);
        mainForm.add(input);

        WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
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
        addVisibileForInputType(inputAce, INPUT_XML, groupModel);
        input.add(inputAce);

        dataLanguage = determineDataLanguage();

        DataLanguagePanel<List> languagePanel = new DataLanguagePanel<List>(ID_LANGUAGE_PANEL, dataLanguage, List.class, this) {
            @Override
            protected void onLanguageSwitched(AjaxRequestTarget target, int index, String updatedLanguage, String objectString) {
                dataLanguage = updatedLanguage;
                xmlEditorModel.setObject(objectString);
                addOrReplaceEditor(inputAce);
                target.add(mainForm);
            }

            @Override
            protected String getObjectStringRepresentation() {
                return xmlEditorModel.getObject();
            }
        };
        inputAce.add(languagePanel);
        addOrReplaceEditor(inputAce);

        WebMarkupContainer inputFileLabel = new WebMarkupContainer(ID_INPUT_FILE_LABEL);
        addVisibileForInputType(inputFileLabel, INPUT_FILE, groupModel);
        input.add(inputFileLabel);

        WebMarkupContainer inputFile = new WebMarkupContainer(ID_INPUT_FILE);
        addVisibileForInputType(inputFile, INPUT_FILE, groupModel);
        input.add(inputFile);

        FileUploadField fileInput = new FileUploadField(ID_FILE_INPUT);
        inputFile.add(fileInput);

        initButtons(buttonBar, groupModel);
    }

    private void addOrReplaceEditor(WebMarkupContainer inputAce) {
        AceEditor editor = new AceEditor(ID_ACE_EDITOR, xmlEditorModel);
        editor.setOutputMarkupId(true);
        editor.setModeForDataLanguage(dataLanguage);
        editor.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        inputAce.addOrReplace(editor);
    }

    private void addVisibileForInputType(Component comp, final Integer type,
            final IModel<Integer> groupModel) {
        comp.add(new VisibleBehaviour(() -> type.equals(groupModel.getObject())));
    }

    private void initButtons(WebMarkupContainer buttonBar, IModel<Integer> inputType) {
        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageCertCampaign.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
        };
        backButton.add(new VisibleBehaviour(() -> canRedirectBack()));
        buttonBar.add(backButton);

        AjaxSubmitButton saveFileButton = new AjaxSubmitButton(ID_IMPORT_FILE_BUTTON,
                createStringResource("PageImportObject.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(false, OPERATION_IMPORT_FILE, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibileForInputType(saveFileButton, INPUT_FILE, inputType);
        buttonBar.add(saveFileButton);

        AjaxSubmitButton saveXmlButton = new AjaxSubmitButton(ID_IMPORT_XML_BUTTON,
                createStringResource("PageImportObject.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                String operation = OPERATION_IMPORT + "." + dataLanguage;
                savePerformed(true, operation, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        addVisibileForInputType(saveXmlButton, INPUT_XML, inputType);
        buttonBar.add(saveXmlButton);
    }

    private FileUpload getUploadedFile() {
        FileUploadField file = (FileUploadField) get(
                createComponentPath(ID_MAIN_FORM, ID_INPUT, ID_INPUT_FILE, ID_FILE_INPUT));
        return file.getFileUpload();
    }

    private boolean validateInput(boolean raw) {
        if (raw) {
            return StringUtils.isNotEmpty(xmlEditorModel.getObject());
        }
        return getUploadedFile() != null;

    }

    private static class InputDescription {
        private final InputStream inputStream;
        private final String dataLanguage;

        InputDescription(InputStream inputStream, String dataLanguage) {
            this.inputStream = inputStream;
            this.dataLanguage = dataLanguage;
        }
    }

    @NotNull
    private InputDescription getInputDescription(boolean editor) throws Exception {
        if (editor) {
            return new InputDescription(
                    IOUtils.toInputStream(xmlEditorModel.getObject(), StandardCharsets.UTF_8),
                    dataLanguage);
        }
        File newFile = null;
        try {
            // Create new file
            MidPointApplication application = getMidpointApplication();
            WebApplicationConfiguration config = application.getWebApplicationConfiguration();
            File folder = new File(config.getImportFolder());
            if (!folder.exists() || !folder.isDirectory()) {
                folder.mkdir();
            }

            FileUpload uploadedFile = getUploadedFile();
            newFile = new File(folder, uploadedFile.getClientFileName());
            // Check new file, delete if it already exists
            if (newFile.exists()) {
                newFile.delete();
            }
            // Save file

            newFile.createNewFile();

            FileUtils.copyInputStreamToFile(uploadedFile.getInputStream(), newFile);

            String language = getPrismContext().detectLanguage(newFile);
            return new InputDescription(new FileInputStream(newFile), language);
        } finally {
            if (newFile != null) {
                FileUtils.deleteQuietly(newFile);
            }
        }
    }

    private void clearOldFeedback() {
        getSession().getFeedbackMessages().clear();
        getFeedbackMessages().clear();
    }

    private void savePerformed(boolean raw, String operationName, AjaxRequestTarget target) {
        clearOldFeedback();

        OperationResult result = new OperationResult(operationName);

        if (!validateInput(raw)) {
            error(getString("pageImportObject.message.nullFile"));
            target.add(getFeedbackPanel());

            return;
        }

        try {
            Task task = createSimpleTask(operationName);
            InputDescription inputDescription = getInputDescription(raw);
            try (InputStream stream = inputDescription.inputStream) {
                ImportOptionsType options = optionsModel.getObject();
                if (isTrue(fullProcessingModel.getObject())) {
                    options.setModelExecutionOptions(new ModelExecuteOptionsType(getPrismContext()).raw(false));
                } else {
                    options.setModelExecutionOptions(null);
                }
                getModelService().importObjectsFromStream(stream, inputDescription.dataLanguage, options, task, result);

                result.recomputeStatus();
            }
        } catch (Exception ex) {
            result.recordFatalError(getString("PageImportObject.message.savePerformed.fatalError"), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import file", ex);
        }

        showResult(result);
        if (result.isFatalError()) {
            target.add(getFeedbackPanel());
        } else {
            target.add(PageImportObject.this);
        }
    }
}
