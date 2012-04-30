/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageImportFile extends PageAdminConfiguration {

    private static final String OPERATION_IMPORT_FILE = "pageImportFile.importFile";

    private LoadableModel<ImportOptionsType> model;

    public PageImportFile() {
        model = new LoadableModel<ImportOptionsType>(false) {

            @Override
            protected ImportOptionsType load() {
                return MiscSchemaUtil.getDefaultImportOptions();
            }
        };
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        ImportOptionsPanel importOptions = new ImportOptionsPanel("importOptions", model);
        mainForm.add(importOptions);

        FileUploadField fileInput = new FileUploadField("fileInput");
        fileInput.setOutputMarkupId(true);
        mainForm.add(fileInput);

        //from example
//        mainForm.setMultiPart(true);
//        mainForm.setMaxSize(Bytes.kilobytes(100));        
        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("importButton",
                createStringResource("pageImportFile.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);
    }

    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_IMPORT_FILE);

        FileUploadField file = (FileUploadField) get("mainForm:fileInput");
        final FileUpload uploadedFile = file.getFileUpload();
        if (uploadedFile == null) {
            error("Uploaded file is null.");
            target.add(getFeedbackPanel());
        }

        try {
            // Create new file
            MidPointApplication application=getMidpointApplication();
            WebApplicationConfiguration config = application.getWebApplicationConfiguration();
            File folder = new File(config.getImportFolder());
            System.out.println(folder.getPath() + ": " + folder.getAbsolutePath());
            if (!folder.exists() || !folder.isDirectory()) {
                folder.mkdir();
            }

            File newFile = new File(folder, uploadedFile.getClientFileName());
            // Check new file, delete if it already exists
            if (newFile.exists()) {
                newFile.delete();
            }
            // Save file
            Task task = getTaskManager().createTaskInstance(OPERATION_IMPORT_FILE);
            newFile.createNewFile();
            uploadedFile.writeTo(newFile);

            //todo ENCODING to UTF-8 !!!!!
            getModelService().importObjectsFromStream(newFile.inputStream(), model.getObject(), task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't import file.", ex);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
