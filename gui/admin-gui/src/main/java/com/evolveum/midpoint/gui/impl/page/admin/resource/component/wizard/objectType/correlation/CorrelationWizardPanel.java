/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPanelHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class CorrelationWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    public CorrelationWizardPanel(String id, ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected CorrelationItemsTableWizardPanel createTablePanel() {
        return new CorrelationItemsTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void showTableForItemRefs(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                ResourceWizardPanelHelper<ItemsSubCorrelatorType> helper = new ResourceWizardPanelHelper<>(getResourceModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showChoiceFragment(target, createTablePanel());
                    }
                };
                showChoiceFragment(target, new CorrelationItemRefsTableWizardPanel(getIdOfChoicePanel(), helper));
            }
        };
    }
}
