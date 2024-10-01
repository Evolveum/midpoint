/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.*;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.schema.result.OperationResult;
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

public class FocusMappingWizardPanel<AR extends AbstractRoleType> extends AbstractWizardPanel<AssignmentType, FocusDetailsModels<AR>> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusMappingWizardPanel.class);

    public FocusMappingWizardPanel(String id, WizardPanelHelper<AssignmentType, FocusDetailsModels<AR>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createFocusMappingSteps(getValueModel())))));
    }

    private void showFocusMappingWizard(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel,
            String stepId) {
        WizardModel wizardModel = new WizardModel(createFocusMappingSteps(valueModel));
        if (StringUtils.isNotEmpty(stepId)) {
            getPageBase().getPageParameters().set(WizardModel.PARAM_STEP, stepId);
            wizardModel.setActiveStepById(stepId);
        }
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), wizardModel));
    }

    private List<WizardStep> createFocusMappingSteps(IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();

        BasicFocusMappingStepPanel<AR> basic =
                new BasicFocusMappingStepPanel<>(getHelper().getDetailsModel(), valueModel) {

                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        super.onExitPerformed(target);
                        FocusMappingWizardPanel.this.onExitPerformed(target);
                    }
                };

        steps.add(basic);

        steps.add( new FocusMappingMappingsStepPanel<>(getHelper().getDetailsModel(), valueModel) {
            @Override
            protected void inEditMappingValue(IModel<PrismContainerValueWrapper<MappingType>> rowModel, AjaxRequestTarget target) {
                showMappingAttributeMappingWizardFragment(target, rowModel, valueModel);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
                super.onExitPerformed(target);
                FocusMappingWizardPanel.this.onExitPerformed(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = FocusMappingWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }
        });

        return steps;
    }

    private void showMappingAttributeMappingWizardFragment(
            AjaxRequestTarget target,
            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createMappingAttributeMappingSteps(rowModel, valueModel))));
    }

    private List<WizardStep> createMappingAttributeMappingSteps(
            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new FocusMappingMappingMainStepPanel<>(getAssignmentHolderModel(), rowModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showFocusMappingWizard(target, valueModel, FocusMappingMappingsStepPanel.PANEL_TYPE);
            }
        });
        steps.add(new FocusMappingMappingOptionalStepPanel(getAssignmentHolderModel(), rowModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showFocusMappingWizard(target, valueModel, FocusMappingMappingsStepPanel.PANEL_TYPE);
            }
        });
        return steps;
    }
}
