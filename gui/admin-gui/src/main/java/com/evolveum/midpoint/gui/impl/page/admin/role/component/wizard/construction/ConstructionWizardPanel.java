/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.*;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

public class ConstructionWizardPanel<AR extends AbstractRoleType> extends AbstractWizardPanel<AssignmentType, FocusDetailsModels<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionWizardPanel.class);

    public ConstructionWizardPanel(String id, WizardPanelHelper<AssignmentType, FocusDetailsModels<AR>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createConstructionSteps(getValueModel())))));
    }

    private void showConstructionWizard(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel,
            String stepId) {
        WizardModel wizardModel = new WizardModel(createConstructionSteps(valueModel));
        if (StringUtils.isNotEmpty(stepId)) {
            wizardModel.setActiveStepById(stepId);
        }
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), wizardModel));
    }

    private List<WizardStep> createConstructionSteps(IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();

        ConstructionResourceStepPanel<AR> selectResource =
                new ConstructionResourceStepPanel<>(getHelper().getDetailsModel(), valueModel) {

                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        super.onExitPerformed(target);
                        ConstructionWizardPanel.this.onExitPerformed(target);
                    }
                };

        steps.add(selectResource);

        steps.add(new ConstructionResourceObjectTypeStepPanel<>(getHelper().getDetailsModel(), selectResource.getValueModel()){
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ConstructionWizardPanel.this.onExitPerformed(target);
            }
        });

        List<AbstractWizardItemVariableStepPanel> variableSteps = new ArrayList<>();

        variableSteps.add(new AbstractWizardItemVariableStepPanel() {
            @Override
            public boolean isApplicable() {
                return !isKind(selectResource.getValueModel(), ShadowKindType.ENTITLEMENT);
            }

            @Override
            public AbstractWizardStepPanel<?> createStepWizardPanel() {
                return new ConstructionGroupStepPanel<>(getHelper().getDetailsModel(), selectResource.getValueModel()){
                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        super.onExitPerformed(target);
                        ConstructionWizardPanel.this.onExitPerformed(target);
                    }
                };
            }
        });

        variableSteps.add(new AbstractWizardItemVariableStepPanel() {
            @Override
            public boolean isApplicable() {
                return isKind(selectResource.getValueModel(), ShadowKindType.ENTITLEMENT);
            }

            @Override
            public AbstractWizardStepPanel<?> createStepWizardPanel() {
                return new ConstructionResourceObjectTypeMembershipStepPanel<>(getHelper().getDetailsModel(), selectResource.getValueModel()){
                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        super.onExitPerformed(target);
                        ConstructionWizardPanel.this.onExitPerformed(target);
                    }
                };
            }
        });

        steps.add(new WizardVariableStepPanel(variableSteps){
            @Override
            public IModel<String> getTitle() {
                return createStringResource("ConstructionWizardPanel.membership");
            }
        });

        steps.add( new ConstructionOutboundMappingsStepPanel<>(getHelper().getDetailsModel(), selectResource.getValueModel()) {
            @Override
            protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> rowModel, AjaxRequestTarget target) {
                showOutboundAttributeMappingWizardFragment(target, rowModel, selectResource.getValueModel());
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
                super.onExitPerformed(target);
                ConstructionWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = ConstructionWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }
        });

        return steps;
    }

    private boolean isKind(IModel<PrismContainerValueWrapper<AssignmentType>> valueModel, ShadowKindType shadowKindType) {
        if (valueModel == null || valueModel.getObject() == null) {
            return false;
        }

        try {
            PrismPropertyWrapper<Object> kind = valueModel.getObject().findProperty(
                    ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND));
            if (kind.getValue() == null || kind.getValue().getRealValue() == null) {
                return false;
            }

            return shadowKindType == kind.getValue().getRealValue();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find property for kind in " + valueModel.getObject(), e);
        }
        return false;
    }

    private void showOutboundAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createOutboundAttributeMappingSteps(rowModel, valueModel))));
    }

    private List<WizardStep> createOutboundAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new ConstructionOutboundMainStepPanel<>(getAssignmentHolderModel(), rowModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showConstructionWizard(target, valueModel, ConstructionOutboundMappingsStepPanel.PANEL_TYPE);
            }
        });
        steps.add(new ConstructionOutboundOptionalStepPanel(getAssignmentHolderModel(), rowModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showConstructionWizard(target, valueModel, ConstructionOutboundMappingsStepPanel.PANEL_TYPE);
            }
        });
        return steps;
    }
}
