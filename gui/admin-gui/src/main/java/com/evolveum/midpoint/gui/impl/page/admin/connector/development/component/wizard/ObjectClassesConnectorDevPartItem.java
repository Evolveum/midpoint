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
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassesConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

public class ObjectClassesConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

//    private static final Trace LOGGER = TraceManager.getTrace(NextConnectorDevStatusItem.class);

    protected ObjectClassesConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isComplete() {
        return true;
    }


    @Override
    protected List<WizardParentStep> createWizardSteps() {
        return List.of(new ObjectClassesConnectorStepPanel(getHelper()));
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.OBJECT_CLASSES;
    }
}
