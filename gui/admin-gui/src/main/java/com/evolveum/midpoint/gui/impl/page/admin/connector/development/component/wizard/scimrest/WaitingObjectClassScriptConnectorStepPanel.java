/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

public abstract class WaitingObjectClassScriptConnectorStepPanel extends WaitingScriptConnectorStepPanel{

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel;

    public WaitingObjectClassScriptConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper);
        this.objectClassModel = objectClassModel;
    }

    public IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> getObjectClassModel() {
        return objectClassModel;
    }

    @Override
    public boolean isCompleted() {
        if (getObjectClassModel().getObject() == null
                || getObjectClassModel().getObject().getRealValue() == null
                || StringUtils.isEmpty(getObjectClassModel().getObject().getRealValue().getName())) {
            return false;
        }

        return super.isCompleted();
    }

    @Override
    protected String getObjectClassName() {
        return getObjectClassModel().getObject().getRealValue().getName();
    }
}
