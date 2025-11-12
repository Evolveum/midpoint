/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController.ConnectorDevelopmentStatusType;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation.RelationConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Strings;

public class InitRelationConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(InitRelationConnectorDevPartItem.class);

    protected InitRelationConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isComplete() {
        if (getParameter() == null) {
            return false;
        }

        try {
            PrismContainerWrapper<ConnDevRelationInfoType> relationContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_RELATION));
            if (relationContainer == null || relationContainer.getValues().isEmpty()) {
                return false;
            }


            PrismContainerValueWrapper<ConnDevRelationInfoType> relationValue = relationContainer.getValues().stream()
                    .filter(value -> Strings.CS.equals(getParameter(), value.getRealValue().getName()))
                    .findFirst()
                    .orElse(null);
            if (relationValue == null) {
                return false;
            }
            PrismPropertyWrapper<Boolean> confirm = relationValue.findProperty(ConnDevRelationInfoType.F_CONFIRM);
            return confirm != null && confirm.getValue() != null && Boolean.TRUE.equals(confirm.getValue().getRealValue());

        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine whether the status is complete.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("InitConnectorDevStatusItem.couldntDetermineComplete");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
        }
        return false;
    }

    @Override
    protected List<WizardParentStep> createWizardSteps() {
        RelationConnectorStepPanel relationStepsParent = new RelationConnectorStepPanel(getHelper());
        relationStepsParent.setRelation(getRelationName());

        return List.of(relationStepsParent);
    }

    private String getRelationName() {
        if (getParameter() != null) {
            return getParameter();
        }

        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_RELATION));
            if (objectClassContainer == null || objectClassContainer.getValues().isEmpty()) {
                return null;
            }

            PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassContainer.getValues().stream()
                    .filter(value -> value.getStatus() == ValueStatus.ADDED)
                    .findFirst()
                    .orElse(null);

            if (objectClassValue != null) {
                return objectClassValue.getRealValue().getName();
            }
            return null;
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get object class name.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("InitConnectorDevStatusItem.couldntGetObjectClassName");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
            return null;
        }
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.INIT_RELATION;
    }
}
