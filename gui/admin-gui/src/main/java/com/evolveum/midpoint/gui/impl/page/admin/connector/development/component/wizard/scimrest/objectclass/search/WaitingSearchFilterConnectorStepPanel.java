/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingObjectClassScriptConnectorStepPanel;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Waiting step for Search Filter script generation.
 */
@PanelType(name = "cdw-connector-waiting-search-filter")
@PanelInstance(identifier = "cdw-connector-waiting-search-filter",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingSearchFilter", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingSearchFilterConnectorStepPanel extends WaitingObjectClassScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-search-filter";

    public WaitingSearchFilterConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper, objectClassModel);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        var realValue = getObjectClassModel().getObject().getRealValue();

        return getDetailsModel().getConnectorDevelopmentOperation().submitGenerateSearchFilterScript(
                realValue.getName(), realValue.getEndpoint(), regenerate, task, result);
    }

    @Override
    protected String getKeyForStoringToken() {
        return SearchFilterScriptConnectorStepPanel.TASK_SEARCH_FILTER_SCRIPTS_KEY;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingSearchFilter");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingSearchFilter.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingSearchFilter.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-cogs");
    }

    @Override
    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_FILTER_DEFINITION;
    }
}
