/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.ObjectAssociationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.ResourceAssociationTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.ResourceAssociationTypeSubjectWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypeWizardPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceAssociationTypeWizardPanel extends SchemaHandlingTypeWizardPanel<ShadowAssociationTypeDefinitionType> {

    public ResourceAssociationTypeWizardPanel(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected ResourceAssociationTypeBasicWizardPanel createNewTypeWizard(String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        return new ResourceAssociationTypeBasicWizardPanel(id, helper);
    }

    @Override
    protected ResourceAssociationTypeWizardChoicePanel createTypePreview() {
        return new ResourceAssociationTypeWizardChoicePanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(ResourceAssociationTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showResourceObjectTypeBasic(target);
                        break;
                    case SUBJECT:
                        showSubjectWizard(target);
                        break;
                    case OBJECT:
                        showWizardFragment(
                                target,
                                new WizardPanel(
                                        getIdOfWizardPanel(),
                                        new WizardModel(createObjectStep())));
                        break;
//                    case ATTRIBUTE_MAPPING:
//                        showTableForAttributesMappings(target);
//                        break;
//                    case REFERENCE_MAPPING:
//                        showTableForReferenceMappings(target);
//                        break;
//                    case SYNCHRONIZATION:
//                        showSynchronizationConfigWizard(target);
//                        break;
//                    case CORRELATION:
//                        showCorrelationItemsTable(target);
//                        break;
//                    case ACTIVATION:
//                        showActivationsWizard(target);
//                        break;
                }

            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showSubjectWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ResourceAssociationTypeSubjectWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                false))
        );
    }

    private List<WizardStep> createObjectStep() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new ObjectAssociationStepPanel(
                getAssignmentHolderModel(),
                PrismContainerValueWrapperModel.fromContainerValueWrapper(
                        getValueModel(),
                        ShadowAssociationTypeDefinitionType.F_OBJECT)) {
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
                OperationResult result = ResourceAssociationTypeWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                } else {
                    onExitPerformed(target);
                }
            }
        });
        return steps;
    }

    private void showResourceObjectTypeBasic(AjaxRequestTarget target) {
        ResourceAssociationTypeBasicWizardPanel wizard =
                new ResourceAssociationTypeBasicWizardPanel(getIdOfChoicePanel(), createHelper(true));
        wizard.setShowChoicePanel(false);
        showChoiceFragment(target, wizard);
    }

//    private void showCorrelationItemsTable(AjaxRequestTarget target) {
//        showChoiceFragment(
//                target,
//                new CorrelationWizardPanel(
//                        getIdOfChoicePanel(),
//                        createHelper(
//                                ItemPath.create(
//                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION),
//                                        //ShadowAssociationDefinitionType.F_CORRELATION),
//                                false))
//        );
//    }
//
//    private void showSynchronizationConfigWizard(AjaxRequestTarget target) {
//        showWizardFragment(
//                target,
//                new SynchronizationWizardPanel<>(
//                        getIdOfWizardPanel(),
//                        createHelper(
//                                ItemPath.create(
//                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION),
//                                        //ShadowAssociationDefinitionType.F_SYNCHRONIZATION),
//                                false))
//        );
//    }
//
//    private void showActivationsWizard(AjaxRequestTarget target) {
//        showWizardFragment(
//                target,
//                new ActivationsWizardPanel(
//                        getIdOfWizardPanel(),
//                        createHelper(
//                                ItemPath.create(
//                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
//                                        ShadowAssociationDefinitionType.F_ACTIVATION),
//                                false))
//        );
//    }
//
//    private void showTableForAttributesMappings(AjaxRequestTarget target) {
//        showWizardFragment(
//                target,
//                new AttributeMappingWizardPanel<>(
//                        getIdOfWizardPanel(),
//                        createHelper(
//                                ItemPath.create(
//                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION),
//                                false))
//        );
//    }
//
//    private void showTableForReferenceMappings(AjaxRequestTarget target) {
//        showWizardFragment(
//                target,
//                new ReferenceMappingWizardPanel(
//                        getIdOfWizardPanel(),
//                        createHelper(
//                                ItemPath.create(
//                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION),
//                                false))
//        );
//    }
}
