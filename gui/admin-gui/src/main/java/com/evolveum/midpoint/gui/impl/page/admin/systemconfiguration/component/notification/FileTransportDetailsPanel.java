/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileTransportConfigurationType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class FileTransportDetailsPanel extends MultivalueContainerDetailsPanel<FileTransportConfigurationType> {

    public FileTransportDetailsPanel(String id, IModel<PrismContainerValueWrapper<FileTransportConfigurationType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public FileTransportDetailsPanel(String id, IModel<PrismContainerValueWrapper<FileTransportConfigurationType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<FileTransportConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("FileTransportConfigurationType.label");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> getModelObject().getName();
            }

            @Override
            protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
                WebMarkupContainer c = new WebMarkupContainer(idTypeImage);
                c.setVisible(false);
                return c;
            }
        };

        return d;
    }
}
