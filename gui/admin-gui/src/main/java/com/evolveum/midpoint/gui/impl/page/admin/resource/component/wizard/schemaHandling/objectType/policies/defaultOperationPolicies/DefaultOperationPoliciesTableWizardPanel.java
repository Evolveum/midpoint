/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.defaultOperationPolicies;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

/**
 * @author lskublik
 */
@PanelType(name = "rw-defaultOperationPolicy")
@PanelInstance(identifier = "rw-defaultOperationPolicy",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "DefaultOperationPoliciesTableWizardPanel.headerLabel", icon = "fa fa-balance-scale"))
public abstract class DefaultOperationPoliciesTableWizardPanel extends AbstractResourceWizardBasicPanel<ResourceObjectTypeDefinitionType> {

    private static final String PANEL_TYPE = "rw-defaultOperationPolicy";
    private static final String ID_TABLE = "table";

    public DefaultOperationPoliciesTableWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        DefaultOperationPoliciesTable table = new DefaultOperationPoliciesTable(ID_TABLE, getValueModel(), getConfiguration()){
            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<DefaultOperationPolicyConfigurationType>> rowModel, List<PrismContainerValueWrapper<DefaultOperationPolicyConfigurationType>> listItems) {
                showPolicyStepWizard(target, rowModel);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    protected abstract void showPolicyStepWizard(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<DefaultOperationPolicyConfigurationType>> rowModel);

    @Override
    protected String getSaveLabelKey() {
        return "DefaultOperationPoliciesTableWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("DefaultOperationPoliciesTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("DefaultOperationPoliciesTableWizardPanel.subText");
    }

    protected DefaultOperationPoliciesTable getTable() {
        return (DefaultOperationPoliciesTable) get(ID_TABLE);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
