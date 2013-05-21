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

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageImportObject extends PageAdminConfiguration {

	private static final Trace LOGGER = TraceManager.getTrace(PageImportObject.class);
	private static final String DOT_CLASS = PageImportObject.class.getName() + ".";
	private static final String OPERATION_IMPORT_FILE = DOT_CLASS + "importFile";
	private static final String OPERATION_IMPORT_XML = DOT_CLASS + "importXml";

	private IModel<String> xmlEditorModel;
	private LoadableModel<ImportOptionsType> model;
	private boolean isImportFromFile = true;
	private final List<String> importTypesList = new ArrayList<String>();
	private Model<String> selected = new Model<String>(getString("pageImportObject.importFromFile"));

	public PageImportObject() {
		model = new LoadableModel<ImportOptionsType>(false) {

			@Override
			protected ImportOptionsType load() {
				return MiscSchemaUtil.getDefaultImportOptions();
			}
		};
		xmlEditorModel = new Model<String>(null);

		initLayout();
	}

	private void initLayout() {
		importTypesList.add(getString("pageImportObject.importFromFile"));
		importTypesList.add(getString("pageImportObject.useEmbeddedEditor"));

		final Form mainForm = new Form("mainForm");
		add(mainForm);

		final WebMarkupContainer container = new WebMarkupContainer("container");
		container.setOutputMarkupId(true);
		mainForm.add(container);

		RadioChoice<String> importTypes = new RadioChoice<String>("importTypes", selected, importTypesList);
		importTypes.add(new AjaxFormChoiceComponentUpdatingBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				PageImportObject.this.isImportFromFile = selected.getObject().equals(importTypesList.get(0));
				target.add(container);
			}
		});
		mainForm.add(importTypes);

		Label chooseFileText = new Label("chooseFile", getString("pageImportObject.chooseFile"));
		chooseFileText.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return isImportFromFile;
			}
		});
		container.add(chooseFileText);

		ImportOptionsPanel importOptions = new ImportOptionsPanel("importOptions", model);
		container.add(importOptions);

		FileUploadField fileInput = new FileUploadField("fileInput");
		fileInput.setOutputMarkupId(true);
		fileInput.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return isImportFromFile;
			}

		});
		container.add(fileInput);

		WebMarkupContainer aceContainer = new WebMarkupContainer("aceContainer");
		aceContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return !isImportFromFile;
			}

		});
		container.add(aceContainer);

		AceEditor<String> xmlEditor = new AceEditor<String>("aceEditor", xmlEditorModel);
		xmlEditor.setOutputMarkupId(true);
		aceContainer.add(xmlEditor);

		// from example
		// mainForm.setMultiPart(true);
		// mainForm.setMaxSize(Bytes.kilobytes(100));
		initButtons(container);
	}

	private void initButtons(final WebMarkupContainer container) {
		AjaxSubmitLinkButton saveFileButton = new AjaxSubmitLinkButton("importFileButton",
				createStringResource("pageImportObject.button.import")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				saveFilePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};

		saveFileButton.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return isImportFromFile;
			}

		});
		container.add(saveFileButton);

		AjaxSubmitLinkButton saveXmlButton = new AjaxSubmitLinkButton("importXmlButton",
				createStringResource("pageImportObject.button.import")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				saveXmlPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};

		saveXmlButton.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return !isImportFromFile;
			}

		});
		container.add(saveXmlButton);
	}

	private void saveFilePerformed(AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_IMPORT_FILE);

		FileUploadField file = (FileUploadField) get("mainForm:container:fileInput");
		final FileUpload uploadedFile = file.getFileUpload();
		if (uploadedFile == null) {
			error(getString("pageImportObject.message.nullFile"));
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
			Task task = createSimpleTask(OPERATION_IMPORT_FILE);
			newFile.createNewFile();
			uploadedFile.writeTo(newFile);

			InputStreamReader reader = new InputStreamReader(new FileInputStream(newFile), "utf-8");
			stream = new ReaderInputStream(reader, reader.getEncoding());
			getModelService().importObjectsFromStream(stream, model.getObject(), task, result);

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't import file.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't import file", ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
			if (newFile != null) {
				FileUtils.deleteQuietly(newFile);
			}
		}

		showResult(result);
		target.add(getFeedbackPanel());
	}

	private void saveXmlPerformed(AjaxRequestTarget target) {
		String xml = xmlEditorModel.getObject();
		if (StringUtils.isEmpty(xml)) {
			error(getString("pageImportObject.message.emptyXml"));
			target.add(getFeedbackPanel());

			return;
		}

		OperationResult result = new OperationResult(OPERATION_IMPORT_XML);
		InputStream stream = null;
		try {
			Task task = createSimpleTask(OPERATION_IMPORT_XML);

			stream = IOUtils.toInputStream(xml, "utf-8");
			getModelService().importObjectsFromStream(stream, model.getObject(), task, result);

			result.recomputeStatus();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't import object.", ex);
			LoggingUtils.logException(LOGGER, "Error occured during xml import", ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}

		if (result.isSuccess()) {
			xmlEditorModel.setObject(null);
		}

		showResult(result);
		target.add(getFeedbackPanel());
	}
}
