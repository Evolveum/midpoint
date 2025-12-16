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
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class OperationConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    protected OperationConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isStarted() {
        return ConnectorDevelopmentWizardUtil.isOperationStarted(
                getObjectDetailsModel(),
                getArtifactType(),
                getParameter());
    }

    @Override
    public boolean isComplete() {
        if (getParameter() == null) {
            return false;
        }

        return ConnectorDevelopmentWizardUtil.isScriptConfirmed(
                getObjectDetailsModel(), getArtifactType(), getParameter());
    }

    protected abstract ConnectorDevelopmentArtifacts.@NotNull KnownArtifactType getArtifactType();

    @Override
    protected List<WizardParentStep> createWizardSteps() {
        AbstractObjectClassConnectorStepPanel objectClassStepsParent = createObjectClassStepsParent();
        String objectClassName = getParameter();
        if (objectClassName == null) {
            objectClassName = ConnectorDevelopmentWizardUtil.getNameOfNewObjectClass(getObjectDetailsModel());
            setParameter(objectClassName);
        }
        IModel<String> objectClassNameModel = new IModel<>() {
            @Override
            public String getObject() {
                return getParameter();
            }

            @Override
            public void setObject(String object) {
                setParameter(object);
            }
        };
        objectClassStepsParent.setObjectClassNameModel(objectClassNameModel);
        return List.of(objectClassStepsParent);
    }

    protected abstract @NotNull AbstractObjectClassConnectorStepPanel createObjectClassStepsParent();
}
