/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound.BasicAssociationInboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.BasicAssociationOutboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingMainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.OutboundMappingOptionalConfigurationStepPanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundMappingMainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundMappingOptionalConfigurationStepPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class AssociationMappingWizardPanel<C extends Containerable> extends AbstractWizardPanel<C, ResourceDetailsModel> {

    public AssociationMappingWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel(MappingDirection.INBOUND)));
    }

    protected AssociationMappingsTableWizardPanel<C> createTablePanel(
            MappingDirection initialTab) {
        return new AssociationMappingsTableWizardPanel<>(getIdOfChoicePanel(), getHelper(), initialTab) {
            @Override
            protected void inEditInboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab) {
                showInboundAttributeMappingWizardFragment(target, value, initialTab);
            }

            @Override
            protected void inEditOutboundAttributeValue(IModel<PrismContainerValueWrapper<MappingType>> value, AjaxRequestTarget target, MappingDirection initialTab) {
                showOutboundAttributeMappingWizardFragment(target, value, initialTab);
            }

            @Override
            protected void showMainMappingConfigurationWizardFragment(AjaxRequestTarget target, boolean isInbound) {
                showWizardFragment(
                        target,
                        new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createMainMappingBasicSteps(isInbound))));
            }
        };
    }

    private @Nullable PrismContainerValueWrapper<MappingType> findMainMapping(boolean isInbound) {
        PrismContainerWrapper<MappingType> container = findMainMappingContainer(isInbound);
        if (container == null) {
            return null;
        }

        List<PrismContainerValueWrapper<MappingType>> values = container.getValues();
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private @Nullable PrismContainerWrapper<MappingType> findMainMappingContainer(boolean isInbound) {
        ItemPath path = ItemPath.create(
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                isInbound
                        ? ShadowAssociationDefinitionType.F_INBOUND
                        : ShadowAssociationDefinitionType.F_OUTBOUND);

        try {
            return getValueModel().getObject().findContainer(path);
        } catch (SchemaException e) {
            throw new IllegalStateException(
                    "Couldn't find container for " + (isInbound ? "inbound" : "outbound") + " mapping in the model.", e);
        }
    }

    private @NotNull @Unmodifiable List<WizardStep> createMainMappingBasicSteps(boolean isInbound) {
        WizardStep step = isInbound
                ? createInboundMainMappingStep()
                : createOutboundMainMappingStep();

        return List.of(step);
    }

    private @NotNull WizardStep createInboundMainMappingStep() {
        return new BasicAssociationInboundStepPanel(
                getAssignmentHolderModel(),
                () -> findMainMapping(true)) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = AssociationMappingWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                    return;
                }

                onExitPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                removeLastBreadcrumb();
                showTableFragment(target, MappingDirection.INBOUND);
            }
        };
    }

    private @NotNull WizardStep createOutboundMainMappingStep() {
        return new BasicAssociationOutboundStepPanel(
                getAssignmentHolderModel(),
                () -> findMainMapping(false)) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                OperationResult result = AssociationMappingWizardPanel.this.onSavePerformed(target);
                if (result == null || result.isError()) {
                    target.add(getFeedback());
                    refresh(target);
                    return;
                }

                removeLastBreadcrumb();
                onExitPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, MappingDirection.OUTBOUND);
            }
        };
    }

    private void showInboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createInboundAttributeMappingSteps(valueModel, initialTab))));
    }

    private @NotNull List<WizardStep> createInboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new InboundMappingMainConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        steps.add(new InboundMappingOptionalConfigurationStepPanel(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        return steps;
    }

    private void showOutboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createOutboundAttributeMappingSteps(valueModel, initialTab))));
    }

    private @NotNull List<WizardStep> createOutboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> valueModel, MappingDirection initialTab) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new OutboundMappingMainConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        steps.add(new OutboundMappingOptionalConfigurationStepPanel<>(getAssignmentHolderModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showTableFragment(target, initialTab);
            }
        });
        return steps;
    }

    private void showTableFragment(AjaxRequestTarget target, MappingDirection initialTab) {
        showChoiceFragment(target, createTablePanel(initialTab));
    }

}
