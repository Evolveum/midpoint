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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Files;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;

/**
 * @author lazyman
 */
public class PageImportFile extends PageAdminConfiguration {
	
	@Autowired
    Task task;
	
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
            	savePerformed(target, form);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSaveError(target, form);
            }
        };
        mainForm.add(saveButton);
    }
    
    private void savePerformed(AjaxRequestTarget target, Form<?> form) {
    	OperationResult result = new OperationResult("aaaaaaaaaaaaaaaa");
    	
    	try{
        	FileUploadField file = (FileUploadField)form.get("fileInput");
        	
        	if(file.getFileUpload() != null){
        		
        		System.out.println(">>>>>>>>>>>"+file.getFileUpload().getClientFileName());
        		System.out.println(">>>>>>>>>>>"+file.getFileUpload().writeToTempFile());
        		
        		// Create new file
        		File newFile = null;
				try {
					newFile = new File(file.getFileUpload().writeToTempFile(), file.getFileUpload().getClientFileName());
					newFile.createNewFile();
				} catch (Exception ex) {
					throw new IllegalStateException("Unable to create file", ex);
				}
        		
				// Check new file, delete if it already existed
				checkFileExists(newFile);
				
        		// Save file
        		MidPointApplication application = PageImportFile.this.getMidpointApplication();
                ModelService modelService = application.getModel();
                modelService.importObjectsFromFile(newFile, model.getObject(), task, result);
        	} 
        } catch (Exception ex) {
        	throw new IllegalStateException("Unable to write file", ex);
        }
    }
    
    public void onSaveError(AjaxRequestTarget target, Form form) {
    	//todo implement
    }
    
    private void checkFileExists(File newFile)
    {
        if (newFile.exists())
        {
            // Try to delete the file
            if (!Files.remove(newFile))
            {
                throw new IllegalStateException("Unable to overwrite " + newFile.getAbsolutePath());
            }
        }
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
