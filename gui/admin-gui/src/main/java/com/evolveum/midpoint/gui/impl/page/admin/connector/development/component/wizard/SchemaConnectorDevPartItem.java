/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController.ConnectorDevelopmentStatusType;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema.SchemaObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

public class SchemaConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    protected SchemaConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isStarted() {
        return ConnectorDevelopmentWizardUtil.isOperationStarted(
                getObjectDetailsModel(),
                ConnectorDevelopmentArtifacts.KnownArtifactType.NATIVE_SCHEMA_DEFINITION,
                getParameter()) || ConnectorDevelopmentWizardUtil.isOperationStarted(
                getObjectDetailsModel(),
                ConnectorDevelopmentArtifacts.KnownArtifactType.CONNID_SCHEMA_DEFINITION,
                getParameter());
    }

    @Override
    public boolean isComplete() {
        if (getParameter() == null) {
            return false;
        }

        return ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                getObjectDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.NATIVE_SCHEMA_DEFINITION, getParameter())
                && ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                getObjectDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.CONNID_SCHEMA_DEFINITION, getParameter());
    }

    @Override
    protected List<WizardParentStep> createWizardSteps() {
        SchemaObjectClassConnectorStepPanel objectClassStepsParent = new SchemaObjectClassConnectorStepPanel(getHelper());
        String objectClassName = getParameter();
        if (objectClassName == null) {
            objectClassName = ConnectorDevelopmentWizardUtil.getNameOfNewObjectClass(getObjectDetailsModel());
        }
        objectClassStepsParent.setObjectClass(objectClassName);
        return List.of(objectClassStepsParent);
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.OBJECT_CLASS_SCHEMA;
    }
}
