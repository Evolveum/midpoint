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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class BusinessRoleWizardPanel extends AbstractWizardPanel<RoleType, FocusDetailsModels<RoleType>> {



    public BusinessRoleWizardPanel(String id, WizardPanelHelper<RoleType, FocusDetailsModels<RoleType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicInformationStepPanel(getHelper().getDetailsModel()){
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }
        });

        steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel()){
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                BusinessRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        });

        return steps;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
//            WebComponentUtil.createToastForCreateObject(target, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private void exitToPreview(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new RoleWizardPreviewPanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
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
        CONFIGURE_MEMBERS("fa fa-users");

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
