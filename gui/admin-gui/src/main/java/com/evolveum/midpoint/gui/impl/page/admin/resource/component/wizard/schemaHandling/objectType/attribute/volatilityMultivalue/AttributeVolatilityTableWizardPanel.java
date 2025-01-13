/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowItemVolatilityType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * Wizard panel for table of ShadowItemDependencyType.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 *
 * @author lskublik
 */

@PanelType(name = "rw-attribute-volatility")
@PanelInstance(identifier = "rw-attribute-volatility",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "AttributeVolatilityTableWizardPanel.text", icon = "fa-diagram-next"))
public class AttributeVolatilityTableWizardPanel extends AbstractResourceWizardBasicPanel<ShadowItemVolatilityType> {

    private static final String PANEL_TYPE = "rw-attribute-volatility";

    private static final String ID_TABLE = "table";

    public AttributeVolatilityTableWizardPanel(
            String id,
            WizardPanelHelper<ShadowItemVolatilityType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        AttributeVolatilityTable table = new AttributeVolatilityTable(ID_TABLE, getValueModel(), getConfiguration());

        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        return getTable().isValidFormComponents(target);
    }

    protected AttributeVolatilityTable getTable() {
        return (AttributeVolatilityTable) get(ID_TABLE);
    }

    @Override
    protected String getSaveLabelKey() {
        return "AttributeVolatilityTableWizardPanel.saveButton";
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("AttributeVolatilityTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AttributeVolatilityTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AttributeVolatilityTableWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-10";
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }
}
