/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;

/**
 * @author katkav
 *
 */
@Component
public class UploadDownloadPanelFactory<T> extends AbstractGuiComponentFactory<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return DOMUtil.XSD_BASE64BINARY.equals(wrapper.getTypeName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        return new UploadDownloadPanel(panelCtx.getComponentId(), false) { //getModel().getObject().isReadonly()

            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getStream() {
                T object = panelCtx.getRealValueModel().getObject();
                if (object instanceof String) {
                    return new ByteArrayInputStream(((String)object).getBytes());
                }
                return object != null ? new ByteArrayInputStream((byte[]) object) : new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public void updateValue(byte[] file) {
                panelCtx.getRealValueModel().setObject((T) file);
            }

            @Override
            public void uploadFileFailed(AjaxRequestTarget target) {
                super.uploadFileFailed(target);
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
    }

}
