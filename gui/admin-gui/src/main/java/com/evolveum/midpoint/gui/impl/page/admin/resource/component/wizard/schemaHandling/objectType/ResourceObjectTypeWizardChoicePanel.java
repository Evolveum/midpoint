/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;

import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public abstract class ResourceObjectTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<ResourceObjectTypeWizardChoicePanel.ResourceObjectTypePreviewTileType> {

    private final WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper;

    public ResourceObjectTypeWizardChoicePanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), ResourceObjectTypePreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-10 col-12 choice-tiles-container-8 gap-3 m-auto"));
    }

    public enum ResourceObjectTypePreviewTileType implements TileEnum {

        BASIC("fa fa-circle"),
        ATTRIBUTE_MAPPING("fa fa-retweet"),
        SYNCHRONIZATION("fa fa-arrows-rotate"),
        CORRELATION("fa fa-code-branch"),
        CAPABILITIES("fa fa-atom"),
        ACTIVATION("fa fa-toggle-off"),
        CREDENTIALS("fa fa-key"),
        POLICIES("fa fa-balance-scale");

        private final String icon;

        ResourceObjectTypePreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    @Override
    protected boolean addDefaultTile() {
        return false;
    }

    @Override
    protected void addCustomButtons(RepeatingView buttons) {
        AjaxIconButton previewData = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-magnifying-glass"),
                getPageBase().createStringResource("ResourceObjectTypePreviewTileType.PREVIEW_DATA")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showPreviewDataObjectType(target);
            }
        };
        previewData.showTitleAsLabel(true);
        previewData.add(AttributeAppender.append("class", "btn btn-primary"));
        buttons.add(previewData);
    }

    protected void showPreviewDataObjectType(AjaxRequestTarget target) {
    }

    @Override
    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("ResourceObjectTypeWizardPreviewPanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
        return helper.getValueModel();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
            }
        };
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
