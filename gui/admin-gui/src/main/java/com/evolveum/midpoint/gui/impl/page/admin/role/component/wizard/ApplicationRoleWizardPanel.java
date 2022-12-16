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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ApplicationRoleWizardPanel extends AbstractWizardPanel<RoleType, FocusDetailsModels<RoleType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ApplicationRoleWizardPanel.class);

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

//    protected ResourceObjectTypeWizardPanel createObjectTypeWizard(
//            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
//
//        ResourceWizardPanelHelper<ResourceObjectTypeDefinitionType> helper = new ResourceWizardPanelHelper<>(getAssignmentHolderDetailsModel(), valueModel) {
//
//            @Override
//            public void onExitPerformed(AjaxRequestTarget target) {
//                showWizardPanel(createTablePanel(), target);
//            }
//
//            @Override
//            public OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
//                return ApplicationRoleWizardPanel.this.onSaveResourcePerformed(target);
//            }
//        };
//        ResourceObjectTypeWizardPanel wizard = new ResourceObjectTypeWizardPanel(ID_WIZARD_PANEL, helper);
//        wizard.setOutputMarkupId(true);
//        return wizard;
//    }

//    protected ResourceObjectTypeTableWizardPanel createTablePanel() {
//        return new ResourceObjectTypeTableWizardPanel(ID_WIZARD_PANEL, getAssignmentHolderDetailsModel()) {
//            @Override
//            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
//                showWizardPanel(createObjectTypeWizard(valueModel), target);
//            }
//
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        };
//    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSaveResourcePerformed(target);
        if (!result.isError()) {
            WebComponentUtil.createToastForCreateObject(target, this, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

//    private PreviewResourceDataWizardPanel createPreviewResourceDataWizardPanel() {
//        return new PreviewResourceDataWizardPanel(ID_WIZARD_PANEL, getAssignmentHolderDetailsModel()) {
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        };
//    }

    private void exitToPreview(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ApplicationRoleWizardPreviewPanel(getIdOfChoicePanel(), getHelper().getDetailsModel()) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
                            case CONFIGURE_CONSTRUCTION:
//                        showWizardPanel(createPreviewResourceDataWizardPanel(), target);
                                break;
                        }
                    }
                });
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }

}
