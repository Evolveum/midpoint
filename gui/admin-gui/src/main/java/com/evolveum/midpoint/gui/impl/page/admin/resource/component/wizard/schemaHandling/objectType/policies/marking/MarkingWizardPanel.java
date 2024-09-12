/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.marking;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.defaultOperationPolicies.DefaultOperationPoliciesTableWizardPanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowMarkingConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.Collection;

/**
 * @author lskublik
 */
public class MarkingWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(MarkingWizardPanel.class);

    public MarkingWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected MarkingTableWizardPanel createTablePanel() {
        return new MarkingTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void showPatternStepWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowMarkingConfigurationType>> rowModel) {
                WizardPanelHelper<ShadowMarkingConfigurationType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getAssignmentHolderDetailsModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        showUnsavedChangesToast(target);
                        showChoiceFragment(target, createTablePanel());
                    }
                };
                showChoiceFragment(target, new PatternWizardPanel(getIdOfChoicePanel(), helper));
            }
        };
    }
}
