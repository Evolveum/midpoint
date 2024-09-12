/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.defaultOperationPolicies.DefaultOperationPoliciesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.marking.MarkingWizardPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardWithChoicePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * @author lskublik
 */

@Experimental
public class PoliciesObjectTypeWizardPanel extends AbstractWizardWithChoicePanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    public PoliciesObjectTypeWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void showTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTypePreview());
    }

    protected void initLayout() {
            add(createChoiceFragment(createTypePreview()));
    }

    @Override
    protected Component createTypePreview() {
        return new PoliciesObjectTypeWizardChoicePanel(getIdOfChoicePanel(), getHelper()) {
            @Override
            protected void onTileClickPerformed(PoliciesPreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case DEFAULT_OPERATION_POLICY:
                        showDefaultOperationPoliciesWizardPanel(target);
                        break;
                    case MARKING:
                        showMakingWizardPanel(target);
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                PoliciesObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showDefaultOperationPoliciesWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new DefaultOperationPoliciesWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(false))
        );
    }

    private void showMakingWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new MarkingWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(false))
        );
    }
}
