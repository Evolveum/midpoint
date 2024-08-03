/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class AssociationSubjectWizardChoicePanel
        extends ResourceWizardChoicePanel<AssociationSubjectWizardChoicePanel.AssociationSubjectPreviewTileType> {

    private final WizardPanelHelper<ShadowAssociationTypeSubjectDefinitionType, ResourceDetailsModel> helper;

    public AssociationSubjectWizardChoicePanel(
            String id,
            WizardPanelHelper<ShadowAssociationTypeSubjectDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), AssociationSubjectPreviewTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 gap-3 m-auto"));
    }

    public enum AssociationSubjectPreviewTileType implements TileEnum {

        OBJECTS("fa fa-circle"),
        INBOUND("fa fa-arrow-right-to-bracket"),
        OUTBOUND("fa fa-arrow-right-from-bracket");

        private final String icon;

        AssociationSubjectPreviewTileType(String icon) {
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
        return getPageBase().createStringResource("AssociationSubjectWizardChoicePanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType>> getValueModel() {
        return helper.getValueModel();
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("AssociationSubjectWizardChoicePanel.breadcrumb");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AssociationSubjectWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationSubjectWizardChoicePanel.text");
    }
}
