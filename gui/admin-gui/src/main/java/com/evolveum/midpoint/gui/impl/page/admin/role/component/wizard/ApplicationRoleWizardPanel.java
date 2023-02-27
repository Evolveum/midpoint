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
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeOutboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.model.IModel;

import java.awt.*;
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
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
//            WebComponentUtil.createToastForCreateObject(target, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private void exitToPreview(AjaxRequestTarget target) {
        getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
        showChoiceFragment(
                target,
                new RoleWizardPreviewPanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
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

//        WizardModel wizardModel = new WizardModel(createConstructionSteps(valueModel));
//        if (StringUtils.isNotEmpty(stepId)) {
//            wizardModel.setActiveStepById(stepId);
//        }
//        showWizardFragment(
//                target,
//                new WizardPanel(getIdOfWizardPanel(), wizardModel));
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

//    private List<WizardStep> createConstructionSteps(IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
//        List<WizardStep> steps = new ArrayList<>();
//
//        ConstructionResourceStepPanel selectResource =
//                new ConstructionResourceStepPanel(getHelper().getDetailsModel(), valueModel) {
//
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        };
//
//        steps.add(selectResource);
//
//        steps.add(new ConstructionResourceObjectTypeStepPanel(getHelper().getDetailsModel(), selectResource.getValueModel()){
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        });
//
//        steps.add(new ConstructionGroupStepPanel(getHelper().getDetailsModel(), selectResource.getValueModel()){
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        });
//
//        steps.add( new ConstructionOutboundMappingsStepPanel(getHelper().getDetailsModel(), selectResource.getValueModel()) {
//            @Override
//            protected void inEditOutboundValue(IModel<PrismContainerValueWrapper<MappingType>> rowModel, AjaxRequestTarget target) {
//                showOutboundAttributeMappingWizardFragment(target, rowModel, selectResource.getValueModel());
//            }
//
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//
//            @Override
//            protected void onSubmitPerformed(AjaxRequestTarget target) {
//                super.onSubmitPerformed(target);
//                ApplicationRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
//            }
//        });
//
//        return steps;
//    }
//
//    private void showOutboundAttributeMappingWizardFragment(
//            AjaxRequestTarget target,
//            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
//            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
//        showWizardFragment(
//                target,
//                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createOutboundAttributeMappingSteps(rowModel, valueModel))));
//    }
//
//    private List<WizardStep> createOutboundAttributeMappingSteps(
//            IModel<PrismContainerValueWrapper<MappingType>> rowModel,
//            IModel<PrismContainerValueWrapper<AssignmentType>> valueModel) {
//        List<WizardStep> steps = new ArrayList<>();
//        steps.add(new AttributeOutboundStepPanel<>(getAssignmentHolderModel(), rowModel) {
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                showConstructionWizard(target, valueModel, ConstructionOutboundMappingsStepPanel.PANEL_TYPE);
//            }
//        });
//        return steps;
//    }

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
