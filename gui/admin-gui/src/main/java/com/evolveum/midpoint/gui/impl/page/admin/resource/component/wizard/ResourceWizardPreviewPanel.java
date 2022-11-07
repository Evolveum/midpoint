/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;

import org.jetbrains.annotations.NotNull;

public abstract class ResourceWizardPreviewPanel extends ResourceWizardChoicePanel<ResourceWizardPreviewPanel.PreviewTileType> {

    public ResourceWizardPreviewPanel(String id, ResourceDetailsModel resourceModel) {
        super(id, resourceModel, PreviewTileType.class);
    }

    enum PreviewTileType implements TileEnum {

        PREVIEW_DATA("fa fa-magnifying-glass"),
        CONFIGURE_OBJECT_TYPES("fa fa-object-group");

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
        String name = WebComponentUtil.getDisplayNameOrName(getResourceModel().getObjectWrapper().getObject());
        if (StringUtils.isEmpty(name)) {
            return getPageBase().createStringResource("ResourceWizardPreviewPanel.title");
        }
        return Model.of(name);
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ResourceWizardPreviewPanel.text");
    }

    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        getPageBase().navigateToNext(PageResources.class);
    }
}
