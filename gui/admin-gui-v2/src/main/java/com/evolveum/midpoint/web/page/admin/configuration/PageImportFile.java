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

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class PageImportFile extends PageAdminConfiguration {

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

        CheckBox protectedByEncryption = new CheckBox("protectedByEncryption",
                new PropertyModel<Boolean>(model, "encryptProtectedValues"));
        mainForm.add(protectedByEncryption);
        CheckBox fetchResourceSchema = new CheckBox("fetchResourceSchema",
                new PropertyModel<Boolean>(model, "fetchResourceSchema"));
        mainForm.add(fetchResourceSchema);
        CheckBox keepOid = new CheckBox("keepOid",
                new PropertyModel<Boolean>(model, "keepOid"));
        mainForm.add(keepOid);
        CheckBox overwriteExistingObject = new CheckBox("overwriteExistingObject",
                new PropertyModel<Boolean>(model, "overwrite"));
        mainForm.add(overwriteExistingObject);
        CheckBox referentialIntegrity = new CheckBox("referentialIntegrity",
                new PropertyModel<Boolean>(model, "referentialIntegrity"));
        mainForm.add(referentialIntegrity);
        CheckBox summarizeErrors = new CheckBox("summarizeErrors",
                new PropertyModel<Boolean>(model, "summarizeErrors"));
        mainForm.add(summarizeErrors);
        CheckBox summarizeSuccesses = new CheckBox("summarizeSuccesses",
                new PropertyModel<Boolean>(model, "summarizeSuccesses"));
        mainForm.add(summarizeSuccesses);
        CheckBox validateDynamicSchema = new CheckBox("validateDynamicSchema",
                new PropertyModel<Boolean>(model, "validateDynamicSchema"));
        mainForm.add(validateDynamicSchema);
        CheckBox validateStaticSchema = new CheckBox("validateStaticSchema",
                new PropertyModel<Boolean>(model, "validateStaticSchema"));
        mainForm.add(validateStaticSchema);
        TextField<Integer> errors = new TextField<Integer>("errors",
                new PropertyModel<Integer>(model, "stopAfterErrors"));
        mainForm.add(errors);

        FileUploadField fileInput = new FileUploadField("fileInput");
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
                //todo implement

                model.reset();
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }
        };
        mainForm.add(saveButton);
    }

    //example
//    protected void onSubmit() {
//        final List<FileUpload> uploads = fileUploadField.getFileUploads();
//        if (uploads != null) {
//            for (FileUpload upload : uploads) {
//                // Create a new file
//                File newFile = new File(getUploadFolder(), upload.getClientFileName());
//
//                // Check new file, delete if it already existed
//                checkFileExists(newFile);
//                try {
//                    // Save to new file
//                    newFile.createNewFile();
//                    upload.writeTo(newFile);
//
//                    UploadPage.this.info("saved file: " + upload.getClientFileName());
//                } catch (Exception e) {
//                    throw new IllegalStateException("Unable to write file", e);
//                }
//            }
//        }
//    }
}
