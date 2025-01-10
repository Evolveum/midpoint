/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardChoicePanelWithSeparatedCreatePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Basic wizard configuration panel of attribute. Show choice panel and to mediate showing of container configuration panel.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 *
 * @author lskublik
 */
public class AttributeTypeWizardPanel extends AbstractWizardChoicePanelWithSeparatedCreatePanel<ResourceAttributeDefinitionType> {

    public AttributeTypeWizardPanel(
            String id,
            WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void initLayout() {
        add(createChoiceFragment(createTypePreview()));
    }

    @Override
    protected AttributeBasicWizardPanel createNewTypeWizard(String id, WizardPanelHelper<ResourceAttributeDefinitionType, ResourceDetailsModel> helper) {
        return new AttributeBasicWizardPanel(id, helper);
    }

    @Override
    protected AttributeTypeWizardChoicePanel createTypePreview() {
        return new AttributeTypeWizardChoicePanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(AttributeTypeConfigurationTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showAttributeTypeBasic(target);
                        break;
                    case VOLATILITY:
                        showTableForVolatility(target);
                        break;
                    case LIMITATION:
                        showAttributeTypeLimitation(target);
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                AttributeTypeWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showAttributeTypeBasic(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AttributeBasicWizardPanel(getIdOfChoicePanel(), createHelper(true))
        );
    }

    private void showAttributeTypeLimitation(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AttributeLimitationWizardPanel(getIdOfChoicePanel(), createHelper(true))
        );
    }


    private void showTableForVolatility(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AttributeVolatilityTableWizardPanel(getIdOfChoicePanel(), createHelper(ResourceAttributeDefinitionType.F_VOLATILITY, false)));
    }
}
