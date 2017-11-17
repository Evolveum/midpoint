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
package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import java.io.InputStream;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;

/**
 * @author shood
 * @author lazyman
 * @author katkav
 */
public class UploadDownloadPanel extends InputPanel {

    private static final Trace LOGGER = TraceManager.getTrace(UploadDownloadPanel.class);

    private static final String ID_BUTTON_DOWNLOAD = "download";
    private static final String ID_BUTTON_DELETE = "remove";
    private static final String ID_INPUT_FILE = "fileInput";
    private String downloadFileName = null;
    private String downloadContentType = "text/plain";

    public UploadDownloadPanel(String id, boolean isReadOnly) {
        super(id);
        initLayout(isReadOnly);
    }

    private void initLayout(final boolean isReadOnly) {
        final FileUploadField fileUpload = new FileUploadField(ID_INPUT_FILE);
        Form form = this.findParent(Form.class);
        fileUpload.add(new AjaxFormSubmitBehavior(form, "change")
        {
            @Override
            protected void onSubmit ( AjaxRequestTarget target )
            {
                super.onSubmit(target);
                UploadDownloadPanel.this.uploadFilePerformed(target);
            }

            @Override
            protected void onError (AjaxRequestTarget target){
                super.onError(target);
                UploadDownloadPanel.this.uploadFilePerformed(target);
            }
        } );
        fileUpload.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }
        });
        fileUpload.setOutputMarkupId(true);
        add(fileUpload);

        final AjaxDownloadBehaviorFromStream downloadBehavior = new AjaxDownloadBehaviorFromStream() {

            @Override
            protected InputStream initStream() {
                return getStream();
            }
		};
        downloadBehavior.setContentType(getDownloadContentType());
        downloadBehavior.setFileName(getDownloadFileName());
		add(downloadBehavior);
		
        add(new AjaxSubmitButton(ID_BUTTON_DOWNLOAD) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                downloadPerformed(downloadBehavior, target);
            }
        });

        AjaxSubmitButton delete = new AjaxSubmitButton(ID_BUTTON_DELETE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                removeFilePerformed(target);
            }
        };
        delete.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }
        });
        add(delete);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT_FILE);
    }

    private FileUpload getFileUpload() {
        FileUploadField file = (FileUploadField) get(ID_INPUT_FILE);
        return file.getFileUpload();
    }

    public void uploadFilePerformed(AjaxRequestTarget target) {
        Component input = get(ID_INPUT_FILE);
        try {
            FileUpload uploadedFile = getFileUpload();
            updateValue(uploadedFile.getBytes());
            LOGGER.trace("Upload file success.");
            input.success(getString("UploadPanel.message.uploadSuccess"));
        } catch (Exception e) {
            LOGGER.trace("Upload file error.", e);
            input.error(getString("UploadPanel.message.uploadError") + " " + e.getMessage());
        }
    }

    public void removeFilePerformed(AjaxRequestTarget target) {
        Component input = get(ID_INPUT_FILE);
        try {
            updateValue(null);
            LOGGER.trace("Remove file success.");
            input.success(getString("UploadPanel.message.removeSuccess"));
        } catch (Exception e) {
            LOGGER.trace("Remove file error.", e);
            input.error(getString("UploadPanel.message.removeError") + " " + e.getMessage());
        }
    }

    public void uploadFileFailed(AjaxRequestTarget target) {
        LOGGER.trace("Upload file validation failed.");
    }

    public void updateValue(byte[] file) {
    }

    public InputStream getStream() {
        return null;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public String getDownloadContentType() {
        return downloadContentType;
    }
    
    private void downloadPerformed(AjaxDownloadBehaviorFromStream downloadBehavior,
			AjaxRequestTarget target) {
		downloadBehavior.initiate(target);
	}

    private FileUploadField getInputFile(){
        return (FileUploadField)get(ID_INPUT_FILE);
    }
}
