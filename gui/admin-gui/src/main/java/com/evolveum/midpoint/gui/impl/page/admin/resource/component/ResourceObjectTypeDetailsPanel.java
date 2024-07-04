/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.shadow.ResourceAttributePanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.getObjectClassName;

@PanelType(name = "schemaHandlingDetails")
public class ResourceObjectTypeDetailsPanel extends MultivalueContainerDetailsPanel<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypeDetailsPanel(String id, ResourceDetailsModel resourceDetailsModel, ContainerPanelConfigurationType config) {
        super(id, createModel(resourceDetailsModel, config), true, config);
    }

    private static IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> createModel(ResourceDetailsModel resourceDetailsModel, ContainerPanelConfigurationType config) {
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
    protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper instanceof PrismContainerWrapper) {
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;
    }

    @Override
    protected @NotNull List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(createStringResource("Attributes")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ResourceAttributePanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), ResourceObjectTypeDefinitionType.F_ATTRIBUTE), getConfig());
            }
        });
        return tabs;
    }

    @Override
    protected DisplayNamePanel<ResourceObjectTypeDefinitionType> createDisplayNamePanel(String displayNamePanelId) {
        return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {

            @Override
            protected IModel<String> createHeaderModel() {
                return new ReadOnlyModel<>(() -> loadHeaderModel(getModelObject()) );
            }

            @Override
            protected IModel<List<String>> getDescriptionLabelsModel() {
                return new ReadOnlyModel<>(() -> loadDescriptionModel(getModelObject()));
            }

        };
    }

    private String loadHeaderModel(ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        if (resourceObjectTypeDefinitionType.getDisplayName() != null) {
            return resourceObjectTypeDefinitionType.getDisplayName();
        }
        return getString("SchemaHandlingType.objectType");
    }

    private List<String> loadDescriptionModel(ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        List<String> description = new ArrayList<>();
        if (resourceObjectTypeDefinitionType.getKind() != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.kind", resourceObjectTypeDefinitionType.getKind()));
        }
        if (resourceObjectTypeDefinitionType.getIntent() != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.intent", resourceObjectTypeDefinitionType.getIntent()));
        }
        QName objectClassName = getObjectClassName(resourceObjectTypeDefinitionType);
        if (objectClassName != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.objectClass", objectClassName.getLocalPart()));
        }
        return description;
    }
}
