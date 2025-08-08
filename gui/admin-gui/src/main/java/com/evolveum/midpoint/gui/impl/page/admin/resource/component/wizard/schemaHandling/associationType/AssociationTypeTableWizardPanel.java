/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.AssociationTypesPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypesTableWizardPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "rw-associationTypes")
@PanelInstance(identifier = "rw-associationTypes",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AssociationTypeTableWizardPanel.title", icon = "fa fa-code-compare"))
public abstract class AssociationTypeTableWizardPanel extends SchemaHandlingTypesTableWizardPanel<ShadowAssociationTypeDefinitionType> {

    private static final String PANEL_TYPE = "rw-associationTypes";

    public AssociationTypeTableWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected String getPanelType(){
        return PANEL_TYPE;
    }

    @Override
    protected void initPanel(String panelId) {
        AssociationTypesPanel panel = new AssociationTypesPanel(panelId, getAssignmentHolderDetailsModel(), getConfiguration()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                AssociationTypeTableWizardPanel.this.onEditValue(valueModel, target);
            }

            @Override
            protected void onNewValue(PrismContainerValue<ShadowAssociationTypeDefinitionType> value, IModel<PrismContainerWrapper<ShadowAssociationTypeDefinitionType>> containerModel, AjaxRequestTarget target, boolean isDuplicate) {
                AssociationTypeTableWizardPanel.this.onNewValue(value, containerModel, getObjectDetailsModels().createWrapperContext(), target, isDuplicate);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AssociationTypeTableWizardPanel.subText");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("AssociationTypeTableWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationTypeTableWizardPanel.text");
    }
}
