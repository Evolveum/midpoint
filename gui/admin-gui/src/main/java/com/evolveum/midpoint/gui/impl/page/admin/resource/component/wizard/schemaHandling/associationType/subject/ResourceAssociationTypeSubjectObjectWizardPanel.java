/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardWithChoicePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.ObjectAssociationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.SubjectAssociationStepPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Experimental
public class ResourceAssociationTypeSubjectObjectWizardPanel extends AbstractWizardWithChoicePanel<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> {

    public ResourceAssociationTypeSubjectObjectWizardPanel(String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    public void showTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTypePreview());
    }

    protected void initLayout() {
        add(createChoiceFragment(createTypePreview()));
    }

    @Override
    protected Component createTypePreview() {
        return new AssociationSubjectObjectWizardChoicePanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void onTileClickPerformed(AssociationSubjectObjectPreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case SUBJECTS:
                        showWizardFragment(
                                target,
                                new WizardPanel(
                                        getIdOfWizardPanel(),
                                        new WizardModelBasic(createSubjectStep())));
                        break;
                    case OBJECTS:
                        showWizardFragment(
                                target,
                                new WizardPanel(
                                        getIdOfWizardPanel(),
                                        new WizardModelBasic(createObjectStep())));
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeSubjectObjectWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private @NotNull List<WizardStep> createSubjectStep() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new SubjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
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
                OperationResult result = ResourceAssociationTypeSubjectObjectWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                } else {
                    onExitPerformed(target);
                }
            }
        });
        return steps;
    }

    private @NotNull List<WizardStep> createObjectStep() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new ObjectAssociationStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                onExitPerformed(target);
                return false;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTypePreviewFragment(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = ResourceAssociationTypeSubjectObjectWizardPanel.this.onSavePerformed(target);
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
