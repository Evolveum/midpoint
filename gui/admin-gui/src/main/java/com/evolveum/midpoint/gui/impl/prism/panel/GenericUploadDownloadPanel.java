/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class GenericUploadDownloadPanel<T> extends UploadDownloadPanel {

    private IModel<T> realValueModel;

    public GenericUploadDownloadPanel(String id, IModel<T> realValueModel, boolean isReadOnly) {
        super(id, isReadOnly);
        this.realValueModel = realValueModel;
    }

    @Override
    public InputStream getStream() {
        T object = realValueModel.getObject();
        if (object instanceof String) {
            return new ByteArrayInputStream(((String) object).getBytes());
        }
        return object != null ? new ByteArrayInputStream((byte[]) object) : new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public void updateValue(byte[] file) {
        realValueModel.setObject((T) file);
    }

    @Override
    public void uploadFileFailed(AjaxRequestTarget target) {
        super.uploadFileFailed(target);
        target.add(((PageBase) getPage()).getFeedbackPanel());
    }

}
