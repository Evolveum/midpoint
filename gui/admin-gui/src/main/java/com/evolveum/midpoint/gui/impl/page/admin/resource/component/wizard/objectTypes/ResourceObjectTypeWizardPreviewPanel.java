/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.model.IModel;

public abstract class ResourceObjectTypeWizardPreviewPanel extends ResourceWizardChoicePanel<ResourceObjectTypeWizardPreviewPanel.ResourceObjectTypePreviewTileType> {

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public ResourceObjectTypeWizardPreviewPanel(
            String id,
            ResourceDetailsModel resourceModel,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, resourceModel, ResourceObjectTypePreviewTileType.class);
        this.valueModel = valueModel;
    }

    enum ResourceObjectTypePreviewTileType implements TileEnum {

        PREVIEW_DATA("fa fa-magnifying-glass"),
        ATTRIBUTE_MAPPING("fa fa-retweet"),
        SYNCHRONIZATION_CONFIG("fa fa-arrows-rotate"),
//        CORRELATION_CONFIG("fa fa-code-branch"),
        CAPABILITIES_CONFIG("fab fa-react"),
        ACTIVATION("fa fa-shield"),
        CREDENTIALS("fa fa-shield"),
        ASSOCIATIONS("fa fa-shield");

        private String icon;

        ResourceObjectTypePreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return valueModel;
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.text");
    }
}
