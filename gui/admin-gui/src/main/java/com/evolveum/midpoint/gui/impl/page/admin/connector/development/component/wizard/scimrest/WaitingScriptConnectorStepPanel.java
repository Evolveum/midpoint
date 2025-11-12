/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevScriptIntentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/**
 * @author lskublik
 */
public abstract class WaitingScriptConnectorStepPanel extends WaitingConnectorStepPanel {


    public WaitingScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    public void resetScript(PageBase pageBase) {
        getStatusModel().detach();
        addOrReplace(createWaitingPanel());
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT;
    }

    @Override
    protected abstract ConnDevScriptIntentType getScriptIntent();
}
