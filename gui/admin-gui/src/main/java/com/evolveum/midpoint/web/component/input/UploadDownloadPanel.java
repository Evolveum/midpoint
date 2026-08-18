/*
 * Copyright (c) 2010-2018 Evolveum et al. and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.model.api.authentication.EffectiveFileUploadPolicy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.validator.FileUploadContentValidationException;
import com.evolveum.midpoint.web.component.input.validator.FileValidatorUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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

    private ItemPath uploadItemPath;
    private transient byte[] validatedUploadedFile;

    public UploadDownloadPanel(String id, boolean isReadOnly) {
        super(id);
        this.isReadOnly = isReadOnly;
    }

    public ItemPath getUploadItemPath() {
        return uploadItemPath;
    }

    public void setUploadItemPath(ItemPath uploadItemPath) {
        this.uploadItemPath = uploadItemPath;
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
                UploadDownloadPanel.this.uploadFileFailed(target);
            }
        });
        fileUpload.add(new VisibleBehaviour(() -> !isReadOnly));
        fileUpload.add((IValidator<List<FileUpload>>) validatable -> {

            List<FileUpload> list = validatable.getValue();
            validatedUploadedFile = null;
            if (list == null || list.isEmpty()) {
                return;
            }

            final String label = fileUpload.getLabel() != null ? fileUpload.getLabel().getObject() : fileUpload.getId();

            try {
                EffectiveFileUploadPolicy policy = getEffectiveFileUploadPolicy();

                for (FileUpload fu : list) {
                    byte[] uploadedBytes = fu.getBytes();

                    if (policy.isContentTypeCheckEnabled()) {
                        FileValidatorUtil.validateUploadContent(
                                uploadedBytes,
                                fu.getContentType(),
                                policy.getAllowedContentTypes());
                    }

                    validatedUploadedFile = sanitizeUploadedFile(uploadedBytes, policy);
                }
            } catch (FileUploadContentValidationException ex) {
                validatedUploadedFile = null;
                validatable.error(createValidationError(getValidationMessageKey(ex), label, ex.getMessage()));
            } catch (ImageSanitizationException ex) {
                validatedUploadedFile = null;
                validatable.error(createValidationError("UploadDownloadPanel.validationImageProcessingFailed", label, ex.getMessage()));
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

    private ValidationError createValidationError(String key, Object... params) {
        String msg = getPageBase().getString(key, params);
        return new ValidationError(msg);
    }

    private String getValidationMessageKey(FileUploadContentValidationException ex) {
        return switch (ex.getReason()) {
            case CONTENT_TYPE_MISMATCH -> "UploadDownloadPanel.validationContentNotMatchAllowed";
            case NOT_ALLOWED -> "UploadDownloadPanel.validationContentNotAllowed";
            case UNRECOGNIZED_CONTENT -> "UploadDownloadPanel.validationContentUnrecognized";
            case MALFORMED_MIME_TYPE -> "UploadDownloadPanel.validationContentTypeMalformed";
        };
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getInputFile();
    }

    public void uploadFilePerformed(AjaxRequestTarget target) {
        Component input = getInputFile();
        try {
            updateValue(validatedUploadedFile);

            LOGGER.trace("Upload file success.");
            input.success(getString("UploadPanel.message.uploadSuccess"));
        } catch (Exception e) {
            LOGGER.trace("Upload file error.", e);
            final String errorMessage = getString("UploadPanel.message.uploadError") + " " + e.getMessage();
            input.error(errorMessage);
        } finally {
            validatedUploadedFile = null;
        }
    }

    protected byte[] sanitizeUploadedFile(byte[] uploadedBytes, EffectiveFileUploadPolicy policy)
            throws ImageSanitizationException {
        return ImageSanitizationUtil.sanitizeImage(
                uploadedBytes,
                policy.getConvertImageTo(),
                policy.isStripMetadata());
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
        validatedUploadedFile = null;
        LOGGER.trace("Upload file validation failed.");
    }

    /**
     * Resolves the effective upload policy for the item represented by this panel.
     *
     * @return resolved upload validation and processing policy
     */
    protected EffectiveFileUploadPolicy getEffectiveFileUploadPolicy() {
        return getPageBase()
                .getCompiledGuiProfile()
                .getFileUploadPolicy(uploadItemPath);
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

            String contentType = FileValidatorUtil.detectContentType(IOUtils.toByteArray(is));
            if (contentType != null) {
                return contentType;
            }
        } catch (IOException ex) {
            LOGGER.error("Unable to define download file content type: {}", ex.getLocalizedMessage());
        }

        return DEFAULT_CONTENT_TYPE;
    }

    private void downloadPerformed(AjaxDownloadBehaviorFromStream downloadBehavior,
            AjaxRequestTarget target) {
        downloadBehavior.setContentType(getDownloadContentType());
        downloadBehavior.setFileName(getDownloadFileName());
        downloadBehavior.initiate(target);
    }

    private FileUploadField getInputFile() {
        return (FileUploadField) get(ID_INPUT_FILE);
    }
}
