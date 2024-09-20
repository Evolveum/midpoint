/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.mapping.AssociationOutboundMappingWizardPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardChoicePanelWithSeparatedCreatePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSynchronizationExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * @author lskublik
 */
public class AssociationOutboundEvaluatorWizardPanel extends AbstractWizardChoicePanelWithSeparatedCreatePanel<MappingType> {

    public AssociationOutboundEvaluatorWizardPanel(
            String id,
            WizardPanelHelper<MappingType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected AssociationOutboundBasicWizardPanel createNewTypeWizard(String id, WizardPanelHelper<MappingType, ResourceDetailsModel> helper) {
        return new AssociationOutboundBasicWizardPanel(id, helper);
    }

    @Override
    protected AssociationOutboundWizardChoicePanel createTypePreview() {
        return new AssociationOutboundWizardChoicePanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(AssociationOutboundEvaluatorTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showChoiceFragment(target, createNewTypeWizard(getIdOfChoicePanel(), createHelper(true)));
                        break;
                    case MAPPING:
                        showTableForAttributesMappings(target);
                        break;
//                    case ACTIVATION:
//                        showActivationsWizard(target);
//                        break;
                }

            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                AssociationOutboundEvaluatorWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showActivationsWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new ActivationsWizardPanel(
                        getIdOfWizardPanel(),
                        createHelper(
                                ItemPath.create(
                                        SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION,
                                        AssociationConstructionExpressionEvaluatorType.F_ACTIVATION),
                                false))
        );
    }

    private void showTableForAttributesMappings(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new AssociationOutboundMappingWizardPanel(
                        getIdOfWizardPanel(),
                        createHelper(
                                SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION,
                                false))
        );
    }
}
