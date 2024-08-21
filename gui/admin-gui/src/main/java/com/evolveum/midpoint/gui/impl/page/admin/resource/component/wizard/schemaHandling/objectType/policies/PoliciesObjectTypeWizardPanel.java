/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardWithChoicePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lskublik
 */

@Experimental
public class PoliciesObjectTypeWizardPanel extends AbstractWizardWithChoicePanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(PoliciesObjectTypeWizardPanel.class);

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
                        showWizardFragment(
                                target,
                                new WizardPanel(
                                        getIdOfWizardPanel(),
                                        new WizardModel(createDefaultOperationPolicyStep())));
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

    private void showMakingWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new MarkingWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(false))
        );
    }

    private List<WizardStep> createDefaultOperationPolicyStep() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new DefaultOperationPolicyStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                onExitPerformed(target);
                return false;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTypePreview());
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = PoliciesObjectTypeWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                } else {
                    onExitPerformed(target);
                }
            }
        });
        return steps;
    }
}
