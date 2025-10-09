/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProviderType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

/**
 * Details panel for secrets provider
 */
public class SecretProviderDetailsPanel<SPT extends SecretsProviderType> extends MultivalueContainerDetailsPanel<SPT> {

    public SecretProviderDetailsPanel(String id, IModel<PrismContainerValueWrapper<SPT>> model, boolean addDefaultPanel) {
        super(id, model, addDefaultPanel);
    }

    public SecretProviderDetailsPanel(String id, IModel<PrismContainerValueWrapper<SPT>> model, boolean addDefaultPanel, ContainerPanelConfigurationType config) {
        super(id, model, addDefaultPanel, config);
    }

    @Override
    protected DisplayNamePanel<SPT> createDisplayNamePanel(String displayNamePanelId) {
        return new DisplayNamePanel<>(displayNamePanelId, Model.of(getModelObject().getRealValue())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return () -> {
                    if (getModelObject().getDisplay() != null && getModelObject().getDisplay().getLabel() != null) {
                        return LocalizationUtil.translatePolyString(getModelObject().getDisplay().getLabel());
                    }
                    return getModelObject().getIdentifier();
                };
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return () -> getModelObject().getDescription();
            }

            @Override
            protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
                WebMarkupContainer c = new WebMarkupContainer(idTypeImage);
                c.setVisible(false);
                return c;
            }
        };
    }
}
