/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Optional;

/**
 * @author lskublik
 */
public abstract class WizardVariableStepPanel extends WizardStepPanel {

    private final List<AbstractWizardItemVariableStepPanel> variables;

    public WizardVariableStepPanel(List<AbstractWizardItemVariableStepPanel> variables){
        this.variables = variables;
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        variables.forEach(step -> step.getStepWizardPanel().init(wizard));
    }

    @Override
    protected void onBeforeRender() {
        initLayout();
        super.onBeforeRender();
    }

    private void initLayout() {
        Optional<AbstractWizardItemVariableStepPanel> stepPanel = variables.stream()
                .filter(AbstractWizardItemVariableStepPanel::isApplicable)
                .findFirst();

        if (stepPanel.isPresent()) {
            addOrReplace(stepPanel.get().getStepWizardPanel());
            getWizard().fireActiveStepChanged(stepPanel.get().getStepWizardPanel());
        } else {
            NotFoundWizardVariableStepPanel panel = new NotFoundWizardVariableStepPanel(){
                @Override
                public IModel<String> getTitle() {
                    return WizardVariableStepPanel.this.getTitle();
                }
            };
            panel.init(getWizard());
            addOrReplace(panel);
        }
    }

    @Override
    public VisibleEnableBehaviour getStepsBehaviour() {
        if (getWizard().getSteps().size() <= 1) {
            return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
        }
        return super.getStepsBehaviour();
    }

    @Override
    public abstract IModel<String> getTitle();

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public String getStepId() {
        return "wizard-variable-step-panel";
    }
}
