/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController.ConnectorDevelopmentStatusType;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic.BasicInformationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.ConnectionConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.WaitingObjectClassInformationStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import java.util.List;

public class InitConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(InitConnectorDevPartItem.class);

    protected InitConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isComplete() {
        return ConnectorDevelopmentWizardUtil.isInitObjectClassSearchAllOperationComplete(getObjectDetailsModel());
    }

    @Override
    protected List<WizardParentStep> createWizardSteps() {
        ObjectClassConnectorStepPanel objectClassStepsParent = new ObjectClassConnectorStepPanel(getHelper());
        String objectClassName = getParameter();
        if (objectClassName == null) {
            objectClassName = ConnectorDevelopmentWizardUtil.getNameOfNewObjectClass(getObjectDetailsModel());
        }
        objectClassStepsParent.setObjectClass(objectClassName);

        return List.of(
                new BasicInformationConnectorStepPanel(getHelper()),
                new ConnectionConnectorStepPanel(getHelper()),
                new WaitingObjectClassInformationStepPanel(getHelper()),
                objectClassStepsParent);
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.INIT;
    }
}
