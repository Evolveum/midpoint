/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public abstract class ResourceWizardPreviewPanel extends ResourceWizardChoicePanel<ResourceWizardPreviewPanel.PreviewTileType> {

    public ResourceWizardPreviewPanel(String id, ResourceDetailsModel resourceModel) {
        super(id, resourceModel, PreviewTileType.class);
    }

    enum PreviewTileType implements TileEnum {

        PREVIEW_DATA("fa fa-magnifying-glass"),
        CONFIGURE_OBJECT_TYPES("fa fa-object-group");

        private String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.title");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.text");
    }
}
