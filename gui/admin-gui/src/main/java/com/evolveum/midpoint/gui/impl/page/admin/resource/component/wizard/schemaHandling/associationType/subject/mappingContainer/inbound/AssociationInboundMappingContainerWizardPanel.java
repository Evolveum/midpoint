/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class AssociationInboundMappingContainerWizardPanel extends AbstractWizardPanel<ShadowAssociationDefinitionType, ResourceDetailsModel> {

    public AssociationInboundMappingContainerWizardPanel(String id, WizardPanelHelper<ShadowAssociationDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected void initLayout() {
        add(createChoiceFragment(createTableWizardPanel()));
    }

    private Component createTableWizardPanel() {
        return new AssociationInboundMappingContainerTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void onClickCreateMapping(PrismContainerValueWrapper<MappingType> valueWrapper, AjaxRequestTarget target) {
                showWizardPanel(target, valueWrapper, false);
            }

            @Override
            protected void onTileClick(AjaxRequestTarget target, MappingTile modelObject) {
                showWizardPanel(target, (PrismContainerValueWrapper<MappingType>) modelObject.getValue(), true);
            }
        };
    }

    private void showWizardPanel(AjaxRequestTarget target, PrismContainerValueWrapper<MappingType> value, boolean showChoicePanel) {
        WizardPanelHelper<MappingType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                getHelper().refreshValueModel();
                showChoiceFragment(target, createTableWizardPanel());
            }

            @Override
            public IModel<PrismContainerValueWrapper<MappingType>> getDefaultValueModel() {
                return new LoadableDetachableModel<>() {
                    @Override
                    protected PrismContainerValueWrapper<MappingType> load() {
                        return value;
                    }
                };
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return AssociationInboundMappingContainerWizardPanel.this.onSavePerformed(target);
            }
        };
        AssociationInboundEvaluatorWizardPanel panel = new AssociationInboundEvaluatorWizardPanel(getIdOfChoicePanel(), helper);
        panel.setShowChoicePanel(showChoicePanel);
        showChoiceFragment(target, panel);
    }
}
