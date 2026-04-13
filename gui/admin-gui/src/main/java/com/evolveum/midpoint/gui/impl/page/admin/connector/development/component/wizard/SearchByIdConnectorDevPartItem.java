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
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchByIdObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;

import org.jetbrains.annotations.NotNull;

public class SearchByIdConnectorDevPartItem extends OperationConnectorDevPartItem {

    protected SearchByIdConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ConnectorDevelopmentArtifacts.@NotNull KnownArtifactType getArtifactType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_BY_ID_DEFINITION;
    }

    @Override
    protected @NotNull AbstractObjectClassConnectorStepPanel createObjectClassStepsParent() {
        return new SearchByIdObjectClassConnectorStepPanel(getHelper());
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.OBJECT_CLASS_SEARCH_BY_ID;
    }
}
