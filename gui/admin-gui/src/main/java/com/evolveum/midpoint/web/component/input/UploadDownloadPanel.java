/*
 * Copyright (c) 2010-2018 Evolveum et al. and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImageUploadProcessingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import jakarta.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;

/**
 * @author shood
 * @author lazyman
 * @author katkav
 */
public class UploadDownloadPanel extends InputPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(UploadDownloadPanel.class);

    private static final String DEFAULT_CONTENT_TYPE = "text/plain";

    private static final String ID_BUTTON_DOWNLOAD = "download";
    private static final String ID_BUTTON_DELETE = "remove";
    private static final String ID_INPUT_FILE = "fileInput";

    private final boolean isReadOnly;

    private List<String> allowedUploadContentTypes = new ArrayList<>();

    public UploadDownloadPanel(String id, boolean isReadOnly) {
        super(id);
        this.isReadOnly = isReadOnly;
    }

    public List<String> getAllowedUploadContentTypes() {
        return allowedUploadContentTypes;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final FileUploadField fileUpload = new FileUploadField(ID_INPUT_FILE) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public String[] getInputAsArray() {
                List<String> input = new ArrayList<>();
                try {
                    input.add(new String(IOUtils.toByteArray(getInputStream())));
                } catch (IOException e) {
                    LOGGER.error("Unable to define file content type: {}", e.getLocalizedMessage());
                }
                return input.toArray(new String[0]);
            }
        };

        Form<?> form = this.findParent(Form.class);

        fileUpload.add(new AjaxFormSubmitBehavior(form, "change") {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);
                UploadDownloadPanel.this.uploadFilePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                UploadDownloadPanel.this.uploadFilePerformed(target);
            }
        });
        fileUpload.add(new VisibleBehaviour(() -> !isReadOnly));
        fileUpload.add((IValidator<List<FileUpload>>) validatable -> {

            List<FileUpload> list = validatable.getValue();
            if (list == null || list.isEmpty()) {
                return;
            }

            if (getAllowedUploadContentTypes().isEmpty()) {
                return;
            }

            final String label = fileUpload.getLabel() != null ? fileUpload.getLabel().getObject() : fileUpload.getId();

            try {
                for (FileUpload fu : list) {
                    final String contentType = fu.getContentType();

                    if (!FileValidatorUtil.isValidContentType(contentType, FileValidatorUtil.getMimeTypes(getAllowedUploadContentTypes()))) {
                        String msg = getPageBase().getString("UploadDownloadPanel.validationContentNotAllowed", label, contentType);
                        validatable.error(new ValidationError(msg));
                        continue;
                    }

                    if (!FileValidatorUtil.isValidMagicNumber(contentType, getInputStream())) {// TODO if change to fixedFormat handle check differently
                        String msg = getPageBase().getString("UploadDownloadPanel.validationContentNotMatchAllowed", label, contentType);
                        validatable.error(new ValidationError(msg));
                    }
                }
            } catch (MimeTypeParseException ex) {
                String msg = getPageBase().getString("UploadDownloadPanel.validationContentNotAllowed", label, ex.getMessage());
                validatable.error(new ValidationError(msg));
            } catch (IOException ex) {
                String msg = getPageBase().getString("UploadDownloadPanel.validationContentNotMatchAllowed", label, ex.getMessage());
                validatable.error(new ValidationError(msg));
            }
        });
        fileUpload.setOutputMarkupId(true);
        add(fileUpload);

        final AjaxDownloadBehaviorFromStream downloadBehavior = new AjaxDownloadBehaviorFromStream() {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                return UploadDownloadPanel.this.getInputStream();
            }
        };
        downloadBehavior.setContentType(getDownloadContentType());
        downloadBehavior.setFileName(getDownloadFileName());
        add(downloadBehavior);

        add(new AjaxSubmitButton(ID_BUTTON_DOWNLOAD) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                downloadPerformed(downloadBehavior, target);
            }
        });

        AjaxSubmitButton deleteButton = new AjaxSubmitButton(ID_BUTTON_DELETE) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                removeFilePerformed(target);
            }
        };
        deleteButton.add(new VisibleBehaviour(() -> !isReadOnly));
        add(deleteButton);

        add(new VisibleBehaviour(() -> !isReadOnly));
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getInputFile();
    }

    private FileUpload getFileUpload() {
        FileUploadField file = getInputFile();
        return file.getFileUpload();
    }

    private ImageUploadProcessingType getImageUploadProcessingConfig() {
        FocusType principalFocus = getPageBase().getPrincipalFocus();
        if (!(principalFocus instanceof UserType)) {
            return null;
        }
        AdminGuiConfigurationType adminGui = ((UserType) principalFocus).getAdminGuiConfiguration();
        if (adminGui == null) {
            return null;
        }
        return adminGui.getImageUploadProcessing();
    }

    public void uploadFilePerformed(AjaxRequestTarget target) {
        Component input = getInputFile();
        try {
            ImageUploadProcessingType imageUploadProcessingConfig = getImageUploadProcessingConfig();
            System.out.println(imageUploadProcessingConfig);// TODO ImageUploadProcessingType as arg to ImageSanitationUtil
            FileUpload uploadedFile = getFileUpload();
            updateValue(ImageSanitizationUtil.sanitizeImage(uploadedFile.getBytes(), true));
            LOGGER.trace("Upload file success.");
            input.success(getString("UploadPanel.message.uploadSuccess"));
        } catch (ImageSanitizationException e) {
            LOGGER.trace("Sanitization of upload file error.", e);
            final String errorMessage = getString("UploadPanel.message.sanitizationUploadError") + " " + e.getMessage();
            input.error(errorMessage);
            input.getParent().getFeedbackMessages().add(new FeedbackMessage(input, errorMessage, FeedbackMessage.WARNING));
        } catch (Exception e) {
            LOGGER.trace("Upload file error.", e);
            final String errorMessage = getString("UploadPanel.message.uploadError") + " " + e.getMessage();
            input.error(errorMessage);
            input.getParent().getFeedbackMessages().add(new FeedbackMessage(input, errorMessage, FeedbackMessage.WARNING));
        }
    }

    public void removeFilePerformed(AjaxRequestTarget target) {
        Component input = getInputFile();
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

    public InputStream getInputStream() {
        return null;
    }

    public String getDownloadFileName() {
        return null;
    }

    public String getDownloadContentType() {
        try (InputStream is = getInputStream()) {
            if (is == null) {
                return DEFAULT_CONTENT_TYPE;
            }

            String contentType = URLConnection.guessContentTypeFromStream(is);
            if (StringUtils.isNotEmpty(contentType)) {
                return contentType;
            }
        } catch (IOException ex) {
            LOGGER.error("Unable to define download file content type: {}", ex.getLocalizedMessage());
        }

        return DEFAULT_CONTENT_TYPE;
    }

    private void downloadPerformed(AjaxDownloadBehaviorFromStream downloadBehavior,
            AjaxRequestTarget target) {
        downloadBehavior.initiate(target);
    }

    private FileUploadField getInputFile() {
        return (FileUploadField) get(ID_INPUT_FILE);
    }
}
