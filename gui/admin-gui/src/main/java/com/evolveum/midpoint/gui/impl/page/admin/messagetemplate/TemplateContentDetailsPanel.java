/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizedMessageTemplateContentType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TemplateContentDetailsPanel extends MultivalueContainerDetailsPanel<LocalizedMessageTemplateContentType> {

    public TemplateContentDetailsPanel(String id, IModel<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public TemplateContentDetailsPanel(String id, IModel<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<LocalizedMessageTemplateContentType> createDisplayNamePanel(String displayNamePanelId) {
        // so far no display name panel needed
        DisplayNamePanel d = new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return createStringResource("LocalizedMessageTemplateContentType.language");
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> getModelObject().getLanguage();
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
