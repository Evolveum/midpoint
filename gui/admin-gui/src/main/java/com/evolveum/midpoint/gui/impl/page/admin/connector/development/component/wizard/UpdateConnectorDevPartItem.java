/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController.ConnectorDevelopmentStatusType;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update.UpdateObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;

public class UpdateConnectorDevPartItem extends OperationConnectorDevPartItem {

    protected UpdateConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    //TODO remove after implementation of testing
    @Override
    public boolean isStarted() {
        return ConnectorDevelopmentWizardUtil.existTask(
                WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT,
                getParameter(),
                getArtifactType(),
                getObjectDetailsModel().getConnectorDevelopmentOperation().getObject().getOid(),
                getObjectDetailsModel().getPageAssignmentHolder());
    }

    //TODO remove after implementation of testing
    @Override
    public boolean isComplete() {
        if (getParameter() == null) {
            return false;
        }

        return ConnectorDevelopmentWizardUtil.existScript(getObjectDetailsModel(), getArtifactType(), getParameter());
    }

    @Override
    protected ConnectorDevelopmentArtifacts.@NotNull KnownArtifactType getArtifactType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.UPDATE;
    }

    @Override
    protected @NotNull AbstractObjectClassConnectorStepPanel createObjectClassStepsParent() {
        return new UpdateObjectClassConnectorStepPanel(getHelper());
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.OBJECT_CLASS_UPDATE;
    }
}
