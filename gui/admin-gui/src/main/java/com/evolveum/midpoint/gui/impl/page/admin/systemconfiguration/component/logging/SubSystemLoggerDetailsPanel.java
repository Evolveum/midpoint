/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.logging;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubSystemLoggerConfigurationType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SubSystemLoggerDetailsPanel extends MultivalueContainerDetailsPanel<SubSystemLoggerConfigurationType> {

    public SubSystemLoggerDetailsPanel(String id, IModel<PrismContainerValueWrapper<SubSystemLoggerConfigurationType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public SubSystemLoggerDetailsPanel(String id, IModel<PrismContainerValueWrapper<SubSystemLoggerConfigurationType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<SubSystemLoggerConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("SubSystemLoggerConfigurationType.label");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> {
                    LoggingComponentType component = getModelObject().getComponent();

                    return component != null ? component.value() : null;
                };
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
