/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.marking;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowMarkingConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lskublik
 */
public class PatternWizardPanel extends AbstractWizardPanel<ShadowMarkingConfigurationType, ResourceDetailsModel> {

    public PatternWizardPanel(String id, WizardPanelHelper<ShadowMarkingConfigurationType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createPatternSteps()))));
    }

    private List<WizardStep> createPatternSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new PatternStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                PatternWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
