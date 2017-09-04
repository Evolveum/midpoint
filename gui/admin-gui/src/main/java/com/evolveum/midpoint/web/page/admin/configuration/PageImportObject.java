/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.configuration.component.ImportOptionsPanel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author lazyman
 * @author mserbak
 */
@PageDescriptor(url = "/admin/config/import", action = {
		@AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL, label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL, label = "PageImportObject.auth.configImport.label", description = "PageImportObject.auth.configImport.description") })
public class PageImportObject extends PageAdminConfiguration {

	private static final Trace LOGGER = TraceManager.getTrace(PageImportObject.class);
	private static final String DOT_CLASS = PageImportObject.class.getName() + ".";
	private static final String OPERATION_IMPORT_FILE = DOT_CLASS + "importFile";
	private static final String OPERATION_IMPORT_XML = DOT_CLASS + "importXml";

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

	private LoadableModel<ImportOptionsType> optionsModel;
	private IModel<String> xmlEditorModel;

	private String dataLanguage;

	public PageImportObject() {
		optionsModel = new LoadableModel<ImportOptionsType>(false) {

			@Override
			protected ImportOptionsType load() {
				return MiscSchemaUtil.getDefaultImportOptions();
			}
		};
		xmlEditorModel = new Model<String>(null);

		initLayout();
	}

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);

		ImportOptionsPanel importOptions = new ImportOptionsPanel(ID_IMPORT_OPTIONS, optionsModel);
		mainForm.add(importOptions);

		final WebMarkupContainer input = new WebMarkupContainer(ID_INPUT);
		input.setOutputMarkupId(true);
		mainForm.add(input);

		final WebMarkupContainer buttonBar = new WebMarkupContainer(ID_BUTTON_BAR);
		buttonBar.setOutputMarkupId(true);
		mainForm.add(buttonBar);

		final IModel<Integer> groupModel = new Model<Integer>(INPUT_FILE);
		RadioGroup importRadioGroup = new RadioGroup(ID_IMPORT_RADIO_GROUP, groupModel);
		importRadioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(input);
				target.add(buttonBar);
			}
		});
		mainForm.add(importRadioGroup);

		Radio fileRadio = new Radio(ID_FILE_RADIO, new Model(INPUT_FILE), importRadioGroup);
		importRadioGroup.add(fileRadio);

		Radio xmlRadio = new Radio(ID_XML_RADIO, new Model(INPUT_XML), importRadioGroup);
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
		comp.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return type.equals(groupModel.getObject());
			}

		});
	}

	private void initButtons(WebMarkupContainer buttonBar, IModel<Integer> inputType) {
        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON, createStringResource("PageCertCampaign.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }
		};
		backButton.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return canRedirectBack();
			}
		});
		buttonBar.add(backButton);

		AjaxSubmitButton saveFileButton = new AjaxSubmitButton(ID_IMPORT_FILE_BUTTON,
				createStringResource("PageImportObject.button.import")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(false, OPERATION_IMPORT_FILE, target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		addVisibileForInputType(saveFileButton, INPUT_FILE, inputType);
		buttonBar.add(saveFileButton);

		AjaxSubmitButton saveXmlButton = new AjaxSubmitButton(ID_IMPORT_XML_BUTTON,
				createStringResource("PageImportObject.button.import")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(true, OPERATION_IMPORT_XML, target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
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
		private InputStream inputStream;
		private String dataLanguage;
		InputDescription(InputStream inputStream, String dataLanguage) {
			this.inputStream = inputStream;
			this.dataLanguage = dataLanguage;
		}
	}

	@NotNull
	private InputDescription getInputDescription(boolean editor) throws Exception {
		if (editor) {
			return new InputDescription(
					IOUtils.toInputStream(xmlEditorModel.getObject(), "utf-8"),
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
			uploadedFile.writeTo(newFile);

			String language = getPrismContext().detectLanguage(newFile);
			return new InputDescription(new FileInputStream(newFile), language);
		} finally {
			if (newFile != null) {
				FileUtils.deleteQuietly(newFile);
			}
		}
	}

	private void clearOldFeedback(){
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
		InputStream stream = null;

		try {
			Task task = createSimpleTask(operationName);
			InputDescription inputDescription = getInputDescription(raw);
			stream = inputDescription.inputStream;
			getModelService().importObjectsFromStream(stream, inputDescription.dataLanguage, optionsModel.getObject(), task, result);

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't import file.", ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import file", ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}

		}

		showResult(result);
		target.add(PageImportObject.this);
	}


}
