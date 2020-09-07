/*
 * Copyright (c) 2010-2018 Evolveum et al. and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author shood
 * @author lazyman
 * @author katkav
 */
public class UploadDownloadPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(UploadDownloadPanel.class);

    private static final String ID_BUTTON_DOWNLOAD = "download";
    private static final String ID_BUTTON_DELETE = "remove";
    private static final String ID_INPUT_FILE = "fileInput";

    private static final String DOWNLOAD_CONTENT_TYPE = "text/plain";

    private final String downloadFileName = null;

    public UploadDownloadPanel(String id, boolean isReadOnly) {
        super(id);
        initLayout(isReadOnly);
    }

    private void initLayout(final boolean isReadOnly) {
        final FileUploadField fileUpload = new FileUploadField(ID_INPUT_FILE) {
            private static final long serialVersionUID = 1L;

            @Override
            public String[] getInputAsArray() {
                List<String> input = new ArrayList<>();
                try {
                    input.add(new String(IOUtils.toByteArray(getStream())));
                } catch (IOException e) {
                    LOGGER.error("Unable to define file content type: {}", e.getLocalizedMessage());
                }
                return input.toArray(new String[0]);
            }
        };
        Form form = this.findParent(Form.class);
        fileUpload.add(new AjaxFormSubmitBehavior(form, "change") {
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
        fileUpload.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }
        });
        fileUpload.setOutputMarkupId(true);
        add(fileUpload);

        final AjaxDownloadBehaviorFromStream downloadBehavior = new AjaxDownloadBehaviorFromStream() {
            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream initStream() {
                InputStream is = getStream();
                try {
                    String newContentType = URLConnection.guessContentTypeFromStream(is);
                    if (StringUtils.isNotEmpty(newContentType)) {
                        setContentType(newContentType);
                    }
                } catch (IOException ex) {
                    LOGGER.error("Unable to define download file content type: {}", ex.getLocalizedMessage());
                }
                return is;
            }
        };
        downloadBehavior.setContentType(getDownloadContentType());
        downloadBehavior.setFileName(getDownloadFileName());
        add(downloadBehavior);

        add(new AjaxSubmitButton(ID_BUTTON_DOWNLOAD) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                downloadPerformed(downloadBehavior, target);
            }
        });

        AjaxSubmitButton deleteButton = new AjaxSubmitButton(ID_BUTTON_DELETE) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                removeFilePerformed(target);
            }
        };
        deleteButton.add(new VisibleBehaviour(() -> !isReadOnly));
        add(deleteButton);

        add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }

        });
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
        return DOWNLOAD_CONTENT_TYPE;
    }

    private void downloadPerformed(AjaxDownloadBehaviorFromStream downloadBehavior,
            AjaxRequestTarget target) {
        downloadBehavior.initiate(target);
    }

    private FileUploadField getInputFile() {
        return (FileUploadField) get(ID_INPUT_FILE);
    }
}
