/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardChoicePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

/**
 * Choice panel with titles for configuration of attribute containers.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 */

public abstract class AttributeTypeWizardChoicePanel
        extends ResourceWizardChoicePanel<AttributeTypeWizardChoicePanel.AttributeTypeConfigurationTileType> {

    private final WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper;

    public AttributeTypeWizardChoicePanel(
            String id,
            WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper.getDetailsModel(), AttributeTypeConfigurationTileType.class);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(AttributeAppender.append("class", "col-xxl-10 col-12 gap-3 m-auto"));
    }

    public enum AttributeTypeConfigurationTileType implements TileEnum {

        BASIC("fa fa-circle"),
        LIMITATION("fa fa-triangle-exclamation"),
        VOLATILITY("fa fa-diagram-next");

        private final String icon;

        AttributeTypeConfigurationTileType(String icon) {
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
        return getPageBase().createStringResource("AttributeTypeWizardChoicePanel.exit");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> getValueModel() {
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
        return getPageBase().createStringResource("AttributeTypeWizardChoicePanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AttributeTypeWizardChoicePanel.text");
    }
}
