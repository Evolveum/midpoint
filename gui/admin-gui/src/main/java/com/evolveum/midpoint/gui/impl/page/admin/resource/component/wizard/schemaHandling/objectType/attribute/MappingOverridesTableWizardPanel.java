/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lskublik
 */

@PanelType(name = "rw-attributes")
@PanelInstance(identifier = "rw-attributes",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "MappingOverridesTableWizardPanel.text", icon = "fa fa-shuffle"))
public abstract class MappingOverridesTableWizardPanel<C extends Containerable> extends AbstractResourceWizardBasicPanel<C> {

    private static final String PANEL_TYPE = "rw-attributes";

    private static final String ID_TABLE = "table";

    public MappingOverridesTableWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        MappingOverrideTable<C> table = new MappingOverrideTable<>(ID_TABLE, getValueModel(), getConfiguration()) {
            @Override
            protected ItemName getItemNameOfContainerWithMappings() {
                return MappingOverridesTableWizardPanel.this.getItemNameOfContainerWithMappings();
            }

            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel, List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> listItems) {
                inEditNewValue(rowModel, target);
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    protected abstract ItemName getItemNameOfContainerWithMappings();

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected MappingOverrideTable getTable() {
        return (MappingOverrideTable) get(ID_TABLE);
    }

    @Override
    protected String getSaveLabelKey() {
        return "MappingOverridesTableWizardPanel.saveButton";
    }

    protected abstract void inEditNewValue(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> value, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("MappingOverridesTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
