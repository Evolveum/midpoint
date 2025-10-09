/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.jetbrains.annotations.NotNull;

public abstract class SchemaHandlingWizardChoicePanel extends ResourceWizardChoicePanel<SchemaHandlingWizardChoicePanel.PreviewTileType> {

    public SchemaHandlingWizardChoicePanel(String id, ResourceDetailsModel resourceModel) {
        super(id, resourceModel, PreviewTileType.class);
    }

    enum PreviewTileType implements TileEnum {

        PREVIEW_DATA("fa fa-magnifying-glass"),
        CONFIGURE_OBJECT_TYPES("fa fa-object-group"),
        CONFIGURE_ASSOCIATION_TYPES("fa fa-code-compare");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return Model.of(getResourceName());
    }

    private String getResourceName() {
        return WebComponentUtil.getDisplayNameOrName(getAssignmentHolderDetailsModel().getObjectWrapper().getObject());
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.subText", getResourceName());
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.text", getResourceName());
    }

}
