/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;

import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.BasicConstructionStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionGroupStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionResourceStepPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ApplicationRoleWizardPanel extends AbstractWizardPanel<RoleType, FocusDetailsModels<RoleType>> {

    public ApplicationRoleWizardPanel(String id, WizardPanelHelper<RoleType, FocusDetailsModels<RoleType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new AccessApplicationStepPanel(getHelper().getDetailsModel()));

        steps.add(new BasicInformationStepPanel(getHelper().getDetailsModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                ApplicationRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        });

        return steps;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSaveResourcePerformed(target);
        if (!result.isError()) {
            WebComponentUtil.createToastForCreateObject(target, this, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private void exitToPreview(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ApplicationRoleWizardPreviewPanel(getIdOfChoicePanel(), getHelper().getDetailsModel()) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
                            case CONFIGURE_CONSTRUCTION:
                                showWizardFragment(
                                        target,
                                        new WizardPanel(
                                                getIdOfWizardPanel(),
                                                new WizardModel(createConstructionSteps())
                                        ));
                                break;
                            case CONFIGURE_MEMBERS:
                                showMembersPanel(target);
                                break;
                            case CONFIGURE_GOVERNANCE_MEMBERS:
                                showGovernanceMembersPanel(target);
                                break;
                        }
                    }
                });
    }

    private List<WizardStep> createConstructionSteps() {
        List<WizardStep> steps = new ArrayList<>();

        ConstructionResourceStepPanel selectResource =
                new ConstructionResourceStepPanel(getHelper().getDetailsModel()) {

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        };

        steps.add(selectResource);

        steps.add(new BasicConstructionStepPanel(getHelper().getDetailsModel(), selectResource.getValueModel()){
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });

        steps.add(new ConstructionGroupStepPanel(getHelper().getDetailsModel(), selectResource.getValueModel()){
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                ApplicationRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        });

        return steps;
    }

    private void showGovernanceMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new GovernanceMembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    private void showMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new MembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }

}
