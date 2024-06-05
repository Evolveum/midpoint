/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.complexType;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author lskublik
 */

public class ComplexTypeBasicWizardPanel extends AbstractWizardPanel<ComplexTypeDefinitionType, AssignmentHolderDetailsModel<SchemaType>> {

    public ComplexTypeBasicWizardPanel(String id, WizardPanelHelper<ComplexTypeDefinitionType, AssignmentHolderDetailsModel<SchemaType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(
                getIdOfWizardPanel(),
                new WizardModel(createBasicSteps()))));
    }

    protected List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSettingComplexTypeStepPanel(getAssignmentHolderModel(), getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ComplexTypeBasicWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return new VisibleBehaviour(() -> false);
            }
        });

        steps.add(new PrismItemDefinitionsTableWizardPanel(getAssignmentHolderModel(), getValueModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                OperationResult result = ComplexTypeBasicWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    onExitPerformed(target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ComplexTypeBasicWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }
}
