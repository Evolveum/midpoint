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

import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

/**
 *  @author shood
 * */
public class UploadPanel<T> extends InputPanel {

    private static final String ID_BUTTON_UPLOAD = "upload";
    private static final String ID_INPUT_FILE = "fileInput";
    private static final String ID_FEEDBACK = "feedback";

    public UploadPanel(String id){
        super(id);
        initLayout();
    }

    private void initLayout(){

        final FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);

        FileUploadField fileUpload = new FileUploadField(ID_INPUT_FILE);
        add(fileUpload);

        add(new AjaxSubmitButton(ID_BUTTON_UPLOAD) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                uploadFilePerformed(target);
            }
        });
    }

    @Override
    public FormComponent getBaseFormComponent(){
        return (FormComponent) get(ID_INPUT_FILE);
    }

    public FileUpload getFileUpload(){
        FileUploadField file = (FileUploadField) get(ID_INPUT_FILE);
        final FileUpload uploadedFile = file.getFileUpload();

        return uploadedFile;
    }

    public FeedbackPanel getFeedbackPanel(){
        return (FeedbackPanel)get(ID_FEEDBACK);
    }

    public void uploadFilePerformed(AjaxRequestTarget target){}
}
