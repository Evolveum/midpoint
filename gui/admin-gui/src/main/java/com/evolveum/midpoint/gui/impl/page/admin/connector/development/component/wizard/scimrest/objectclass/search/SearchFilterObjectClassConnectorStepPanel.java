/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Parent wizard step for Search Filter script generation flow.
 */
@PanelType(name = "cdw-object-class-searchFilter")
@PanelInstance(identifier = "cdw-object-class-searchFilter",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.objectClassSearchFilter", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchFilterObjectClassConnectorStepPanel extends AbstractObjectClassConnectorStepPanel implements WizardParentStep {

    public static final String PANEL_TYPE = "cdw-object-class-searchFilter";

    public SearchFilterObjectClassConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public List<WizardStep> createChildrenSteps() {
        return List.of(
                new SearchFilterEndpointsConnectorStepPanel(getHelper(), getObjectClassModel()),
                new WaitingSearchFilterConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SearchFilterScriptConnectorStepPanel(getHelper(), getObjectClassModel()),
                new SearchFilterObjectsConnectorStepPanel(getHelper(), getObjectClassModel()));
    }

    @Override
    protected String getTitleKey() {
        return "PageConnectorDevelopment.wizard.step.objectClassSearchFilter";
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClassSearchFilter.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClassSearchFilter.subText");
    }
}
