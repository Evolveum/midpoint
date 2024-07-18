/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanelWithChoicePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.AssociationInboundMappingContainerTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.AssociationInboundMappingContainerWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.AssociationOutboundMappingContainerTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.AssociationOutboundMappingContainerWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

@Experimental
public class ResourceAssociationTypeSubjectWizardPanel extends AbstractWizardPanelWithChoicePanel<ShadowAssociationTypeSubjectDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAssociationTypeSubjectWizardPanel.class);

    public ResourceAssociationTypeSubjectWizardPanel(String id, WizardPanelHelper<ShadowAssociationTypeSubjectDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void showTypePreviewFragment(AjaxRequestTarget target) {

    }

    protected void initLayout() {
            add(createChoiceFragment(createTypePreview()));
    }

    @Override
    protected Component createTypePreview() {
        return new AssociationSubjectWizardChoicePanel(getIdOfChoicePanel(), getHelper()) {
            @Override
            protected void onTileClickPerformed(AssociationSubjectPreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case OBJECTS:
                        showWizardFragment(
                                target,
                                new WizardPanel(
                                        getIdOfWizardPanel(),
                                        new WizardModel(createSubjectStep())));
                        break;
                    case INBOUND:
                        showInboundWizardPanel(target);
                        break;
                    case OUTBOUND:
                        showOutboundWizardPanel(target);
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeSubjectWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showOutboundWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AssociationOutboundMappingContainerWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                        ShadowAssociationDefinitionType.F_OUTBOUND),
                                false))
        );
    }

    private void showInboundWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AssociationInboundMappingContainerWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                        ShadowAssociationDefinitionType.F_INBOUND),
                                false))
        );
    }

    private List<WizardStep> createSubjectStep() {
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
                OperationResult result = ResourceAssociationTypeSubjectWizardPanel.this.onSavePerformed(target);
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
