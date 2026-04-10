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

import java.util.Collection;
import java.util.List;

/**
 * Wizard step for selecting endpoint for Search By ID (search one / read by ID) script generation.
 */
@PanelType(name = "cdw-search-by-id-endpoints")
@PanelInstance(identifier = "cdw-search-by-id-endpoints",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.searchByIdEndpoints", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchByIdEndpointsConnectorStepPanel extends EndpointsConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-search-by-id-endpoints";

    public SearchByIdEndpointsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
                                                 IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper, objectClassModel);
    }

    @Override
    protected Collection<ConnDevHttpEndpointIntentType> getEndpointIntents() {
        return List.of(ConnDevHttpEndpointIntentType.GET_BY_ID);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdEndpoints");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdEndpoints.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.searchByIdEndpoints.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected ItemPath getScriptItemName() {
        return ConnDevObjectClassInfoType.F_SEARCH_ID_OPERATION;
    }
}
