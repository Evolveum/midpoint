/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.ActionStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.ReactionMainSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.ReactionOptionalSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationReactionTableWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectPatternType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowMarkingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
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

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                onExitPerformed(target);
                return false;
            }
        });

        return steps;
    }
}
