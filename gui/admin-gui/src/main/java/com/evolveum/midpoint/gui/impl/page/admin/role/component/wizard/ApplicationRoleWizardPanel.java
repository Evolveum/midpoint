/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;

import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;

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
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new AccessApplicationStepPanel(getHelper().getDetailsModel()){
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ApplicationRoleWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new BasicInformationStepPanel(getHelper().getDetailsModel()) {

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                ApplicationRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ApplicationRoleWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
            exitToPreview(target);
        }
    }

    protected void exitToPreview(AjaxRequestTarget target) {
        getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
        showChoiceFragment(
                target,
                new RoleWizardChoicePanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
                            case CONFIGURE_CONSTRUCTION:
                                showConstructionWizard(target);
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

    private void showConstructionWizard(AjaxRequestTarget target){

        showChoiceFragment(
                target,
                new ConstructionWizardPanel(getIdOfChoicePanel(), createHelper())
        );

    }

    private WizardPanelHelper<AssignmentType, FocusDetailsModels<RoleType>> createHelper() {
        return new WizardPanelHelper<>(getAssignmentHolderModel(), null) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                exitToPreview(target);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return onSavePerformed(target);
            }
        };
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

    enum PreviewTileType implements TileEnum {

        CONFIGURE_GOVERNANCE_MEMBERS("fa fa-users"),
        CONFIGURE_MEMBERS("fa fa-users"),
        CONFIGURE_CONSTRUCTION("fa fa-retweet");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }
}
