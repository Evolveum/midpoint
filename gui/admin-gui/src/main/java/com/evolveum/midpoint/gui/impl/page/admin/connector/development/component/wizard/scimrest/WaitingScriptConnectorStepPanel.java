/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public abstract class WaitingScriptConnectorStepPanel extends WaitingConnectorStepPanel {


    public WaitingScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    public void resetScript(PageBase pageBase) {
        if (getStatusModel() != null){
            getStatusModel().detach();
        }
        markAsReloaded();
        addOrReplace(createWaitingPanel());
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT;
    }

    @Override
    protected abstract ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType();

    @Override
    public boolean isCompleted() {
        PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue =
                ConnectorDevelopmentWizardUtil.getObjectClassValueWrapper(getDetailsModel(), getObjectClassName());
        if (!isReloaded() && ConnectorDevelopmentWizardUtil.existContainerValue(objectClassValue, ConnDevObjectClassInfoType.F_ATTRIBUTE)
                && ConnectorDevelopmentWizardUtil.existContainerValue(objectClassValue, getScriptType().itemName)) {
            return true;
        }

        return super.isCompleted();
    }
}
