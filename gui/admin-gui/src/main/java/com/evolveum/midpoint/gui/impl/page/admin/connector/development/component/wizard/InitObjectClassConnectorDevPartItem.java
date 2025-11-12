/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController.ConnectorDevelopmentStatusType;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevArtifactType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.commons.lang3.Strings;

import java.util.List;

public class InitObjectClassConnectorDevPartItem extends AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(InitObjectClassConnectorDevPartItem.class);

    protected InitObjectClassConnectorDevPartItem(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public boolean isComplete() {
        if (getParameter() == null) {
            return false;
        }

        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer == null || objectClassContainer.getValues().isEmpty()) {
                return false;
            }

            PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassContainer.getValues().stream()
                    .filter(value -> Strings.CS.equals(getParameter(), value.getRealValue().getName()))
                    .findFirst()
                    .orElse(null);
            if (objectClassValue == null) {
                return false;
            }
            PrismContainerWrapper<ConnDevArtifactType> searchAllContainer = objectClassValue.findContainer(ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION);
            if (searchAllContainer == null) {
                return false;
            }

            PrismContainerValueWrapper<ConnDevArtifactType> searchAllValue = searchAllContainer.getValue();
            PrismPropertyWrapper<String> filename = searchAllValue.findProperty(ConnDevArtifactType.F_FILENAME);
            if (filename == null || filename.getValue() == null || filename.getValue().getRealValue() == null) {
                return false;
            }

            PrismPropertyWrapper<Boolean> confirm = searchAllValue.findProperty(ConnDevArtifactType.F_CONFIRM);
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
        ObjectClassConnectorStepPanel objectClassStepsParent = new ObjectClassConnectorStepPanel(getHelper());
        String objectClassName = getParameter();
        if (objectClassName == null) {
            objectClassName = ConnectorDevelopmentWizardUtil.getNameOfNewObjectClass((ConnectorDevelopmentDetailsModel) getObjectDetailsModel());
        }
        objectClassStepsParent.setObjectClass(objectClassName);
        return List.of(objectClassStepsParent);
    }

    @Override
    public Enum<?> getIdentifierForWizardStatus() {
        return ConnectorDevelopmentStatusType.INIT_OBJECT_CLASS;
    }
}
