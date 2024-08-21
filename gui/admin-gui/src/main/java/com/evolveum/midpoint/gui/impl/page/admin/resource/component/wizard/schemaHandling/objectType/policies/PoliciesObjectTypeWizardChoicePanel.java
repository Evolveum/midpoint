/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class PoliciesObjectTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<PoliciesObjectTypeWizardChoicePanel.PoliciesPreviewTileType> {

    private final WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper;

    public PoliciesObjectTypeWizardChoicePanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), PoliciesPreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 gap-3 m-auto"));
    }

    public enum PoliciesPreviewTileType implements TileEnum {

        DEFAULT_OPERATION_POLICY("fa fa-circle"),
        MARKING("fa-solid fa-tag");

        private final String icon;

        PoliciesPreviewTileType(String icon) {
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
    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("PoliciesObjectTypeWizardChoicePanel.exit");
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
        return getTextModel();
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PoliciesObjectTypeWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PoliciesObjectTypeWizardChoicePanel.text");
    }
}
