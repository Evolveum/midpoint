/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.EndpointsConnectorStepPanel;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-search-endpoints")
@PanelInstance(identifier = "cdw-search-endpoints",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.searchAllEndpoints", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchAllEndpointsConnectorStepPanel extends EndpointsConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-search-endpoints";


    public SearchAllEndpointsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
                                                IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper, objectClassModel);
    }

    @Override
    protected ConnDevHttpEndpointIntentType getOperation() {
        return ConnDevHttpEndpointIntentType.SEARCH;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchAllEndpoints");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchAllEndpoints.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchAllEndpoints.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected ItemPath getScriptItemName() {
        return ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION;
    }
}
