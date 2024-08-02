/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

public abstract class AssociationInboundWizardChoicePanel
        extends ResourceWizardChoicePanel<AssociationInboundWizardChoicePanel.AssociationInboundEvaluatorTileType> {

    private final WizardPanelHelper<MappingType, ResourceDetailsModel> helper;

    public AssociationInboundWizardChoicePanel(
            String id,
            WizardPanelHelper<MappingType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), AssociationInboundEvaluatorTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-8 col-10 gap-3 m-auto"));
    }

    public enum AssociationInboundEvaluatorTileType implements TileEnum {

        BASIC("fa fa-circle"),
        MAPPING("fa fa-arrow-right-to-bracket"),
        SYNCHRONIZATION("fa fa-arrows-rotate"),
        CORRELATION("fa fa-code-branch");
//        ACTIVATION("fa fa-toggle-off");

        private final String icon;

        AssociationInboundEvaluatorTileType(String icon) {
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
        return getPageBase().createStringResource("AssociationInboundWizardChoicePanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<MappingType>> getValueModel() {
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
        return getPageBase().createStringResource("AssociationInboundWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationInboundWizardChoicePanel.text");
    }
}
