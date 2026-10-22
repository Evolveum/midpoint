/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import java.io.Serial;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.smart.api.conndev.DocumentationContentTypes;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

/**
 * Popup for uploading a documentation file in the connector generator wizard.
 */
public abstract class DocumentationUploadPopupPanel extends BasePanel<Void> implements Popupable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_POPUP_FEEDBACK = "popupFeedback";
    private static final String ID_FILE = "file";
    private static final String ID_UPLOAD_BUTTON = "upload";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footer;
    private FeedbackAlerts feedback;
    private FileUploadField fileUploadField;

    public DocumentationUploadPopupPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        footer = createFooter();
    }

    private void initLayout() {
        Form<?> mainForm = createUploadForm();
        add(mainForm);
    }

    private Form<?> createUploadForm() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        mainForm.setMultiPart(true);

        feedback = createFeedbackPanel();
        mainForm.add(feedback);

        fileUploadField = createFileUploadField();
        mainForm.add(fileUploadField);

        return mainForm;
    }

    private Fragment createFooter() {
        Fragment buttons = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        buttons.setOutputMarkupId(true);
        buttons.add(createCancelButton());
        buttons.add(createUploadButton());
        return buttons;
    }

    private FeedbackAlerts createFeedbackPanel() {
        FeedbackAlerts feedbackPanel = new FeedbackAlerts(ID_POPUP_FEEDBACK);
        feedbackPanel.setOutputMarkupId(true);
        return feedbackPanel;
    }

    private FileUploadField createFileUploadField() {
        FileUploadField field = new FileUploadField(ID_FILE);
        field.add(AttributeAppender.replace("accept", createAcceptedFileTypes()));
        return field;
    }

    private String createAcceptedFileTypes() {
        return DocumentationContentTypes.getSupportedSuffixes().stream()
                .sorted()
                .collect(Collectors.joining(",", "", ",text/*"));
    }

    private AjaxSubmitButton createUploadButton() {
        return new AjaxSubmitButton(ID_UPLOAD_BUTTON,
                createStringResource("DocumentationUploadPopupPanel.button.upload")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                uploadPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(feedback);
            }
        };
    }

    private AjaxButton createCancelButton() {
        return new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("Button.cancel")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
    }

    private void uploadPerformed(AjaxRequestTarget target) {
        FileUpload fileUpload = fileUploadField.getFileUpload();
        if (fileUpload == null) {
            feedback.error(createStringResource("DocumentationUploadPopupPanel.message.error.noFileSelected").getString());
            target.add(feedback);
            return;
        }

        if (!DocumentationContentTypes.isSupported(fileUpload.getContentType(), fileUpload.getClientFileName())) {
            feedback.error(createStringResource("DocumentationUploadPopupPanel.message.error.unsupportedFormat",
                    fileUpload.getClientFileName()).getString());
            target.add(feedback);
            return;
        }
        onFileUploaded(fileUpload, target);
    }


    protected abstract void onFileUploaded(FileUpload fileUpload, AjaxRequestTarget target);

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 300;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("DocumentationUploadPopupPanel.title");
    }
}
