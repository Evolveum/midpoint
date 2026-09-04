/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingFixObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.ResourceTestConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.WaitingSchemaConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassSelectConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create.CreateObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create.CreateScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create.WaitingCreateConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete.DeleteObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete.DeleteScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete.WaitingDeleteConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.SchemaObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.SchemaScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.ShowSchemaConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.WaitingConnIdSchemaConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.WaitingNativeSchemaConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.WaitingObjectClassDetailsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllObjectsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdObjectConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchFilterObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchFilterObjectsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchFilterScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.WaitingSearchAllConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.WaitingSearchByIdConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.WaitingSearchFilterConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update.UpdateObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update.UpdateScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update.WaitingUpdateConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.sql.connection.SqlConnectionParametersConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Wizard strategy for SQL connectors. No HTTP endpoint-selection steps (JDBC connectors have no
 * concept of an HTTP endpoint) and a dedicated JDBC connection-parameters step instead of the
 * HTTP auth/base-URL flow.
 */
public class SqlConnectorWizardStrategy implements ConnectorWizardStrategy {

    private static final String NOT_APPLICABLE_TO_SQL = "This wizard step is HTTP-specific and does not apply to SQL connectors.";

    @Override
    public List<WizardStep> connectionSteps(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        return List.of(
                new SqlConnectionParametersConnectorStepPanel(helper),
                new ResourceTestConnectorStepPanel(helper, SqlConnectionParametersConnectorStepPanel.PANEL_TYPE),
                new WaitingSchemaConnectorStepPanel(helper));
    }

    @Override
    public List<WizardStep> initObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new ObjectClassSelectConnectorStepPanel(helper, objectClassModel),
                new WaitingObjectClassDetailsConnectorStepPanel(helper, objectClassModel),
                new WaitingNativeSchemaConnectorStepPanel(helper, objectClassModel),
                new WaitingConnIdSchemaConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SchemaObjectClassConnectorStepPanel.PANEL_TYPE),
                new SchemaScriptConnectorStepPanel(helper, objectClassModel),
                new ShowSchemaConnectorStepPanel(helper, objectClassModel),
                new WaitingSearchAllConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SearchAllObjectClassConnectorStepPanel.PANEL_TYPE),
                new SearchAllScriptConnectorStepPanel(helper, objectClassModel),
                new SearchAllObjectsConnectorStepPanel(helper, objectClassModel),
                new WaitingSearchByIdConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SearchByIdObjectClassConnectorStepPanel.PANEL_TYPE),
                new SearchByIdScriptConnectorStepPanel(helper, objectClassModel),
                new SearchByIdObjectConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> searchAllObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingSearchAllConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SearchAllObjectClassConnectorStepPanel.PANEL_TYPE),
                new SearchAllScriptConnectorStepPanel(helper, objectClassModel),
                new SearchAllObjectsConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> searchByIdObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingSearchByIdConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SearchByIdObjectClassConnectorStepPanel.PANEL_TYPE),
                new SearchByIdScriptConnectorStepPanel(helper, objectClassModel),
                new SearchByIdObjectConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> searchFilterObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingSearchFilterConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, SearchFilterObjectClassConnectorStepPanel.PANEL_TYPE),
                new SearchFilterScriptConnectorStepPanel(helper, objectClassModel),
                new SearchFilterObjectsConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> createObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingCreateConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, CreateObjectClassConnectorStepPanel.PANEL_TYPE),
                new CreateScriptConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> updateObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingUpdateConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, UpdateObjectClassConnectorStepPanel.PANEL_TYPE),
                new UpdateScriptConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public List<WizardStep> deleteObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        return List.of(
                new WaitingDeleteConnectorStepPanel(helper, objectClassModel),
                new WaitingFixObjectClassConnectorStepPanel(helper, objectClassModel, DeleteObjectClassConnectorStepPanel.PANEL_TYPE),
                new DeleteScriptConnectorStepPanel(helper, objectClassModel));
    }

    @Override
    public ItemName connectionUrlFieldName() {
        throw new UnsupportedOperationException(NOT_APPLICABLE_TO_SQL);
    }
}
