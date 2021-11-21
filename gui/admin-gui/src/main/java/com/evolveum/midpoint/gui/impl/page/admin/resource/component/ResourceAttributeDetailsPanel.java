/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

@PanelType(name = "attributeDefinitionDetails")
public class ResourceAttributeDetailsPanel extends MultivalueContainerDetailsPanel<ResourceAttributeDefinitionType> {

    public ResourceAttributeDetailsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, createModel(model, config), true, config);
    }

    private static IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> createModel(ResourceDetailsModel resourceDetailsModel, ContainerPanelConfigurationType config) {
        return new ReadOnlyModel<>(() -> {
            try {
                return resourceDetailsModel.getObjectWrapper().findContainerValue(config.getPath().getItemPath());
            } catch (SchemaException e) {
                e.printStackTrace();
            }
            return null;
        });
    }


    @Override
    protected DisplayNamePanel<ResourceAttributeDefinitionType> createDisplayNamePanel(String displayNamePanelId) {
        return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {
            @Override
            protected IModel<String> createHeaderModel() {
                return createDisplayNameForRefinedItem(getModelObject());
            }

            @Override
            protected IModel<String> getDescriptionLabelModel() {
                return new ReadOnlyModel<>(() -> getModelObject().getDescription());
            }
        };
    }

    private IModel<String> createDisplayNameForRefinedItem(ResourceAttributeDefinitionType refinedItem) {
        return new ReadOnlyModel<>(() -> {
            if (refinedItem.getDisplayName() != null) {
                return refinedItem.getDisplayName();
            }

            if (refinedItem.getRef() != null) {
                return refinedItem.getRef().toString();
            }

            return getPageBase().createStringResource("feedbackMessagePanel.message.undefined").getString();
        });

    }

    @Override
    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
        return getItemVisibilityForBasicPanel(itemWrapper);
    }

    protected ItemVisibility getItemVisibilityForBasicPanel(ItemWrapper<?, ?> itemWrapper) {
        return ItemVisibility.AUTO;
    }
}
