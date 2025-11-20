/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardController;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic.BasicInformationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic.DocumentationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.ConnectionConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassesConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation.RelationshipsConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary.ConnectorDevelopmentWizardSummaryPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRelationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConnectorDevelopmentController extends AbstractWizardController<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentController.class);

    enum ConnectorDevelopmentStatusType {
        INIT,
        BASIC,
        OBJECT_CLASSES,
        INIT_OBJECT_CLASS,
        OBJECT_CLASS_SCHEMA,
        OBJECT_CLASS_SEARCH_ALL,
        RELATIONSHIPS,
        INIT_RELATIONSHIP,
        RELATIONSHIP,
        NEXT
    }

    public ConnectorDevelopmentController(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    public void initNewObjectClass(AjaxRequestTarget target) {
        int index = addWizardPartOnEnd(new InitObjectClassConnectorDevPartItem(getHelper()));
        clearInProgressPart();
        setActiveWizardPartIndex(index);
        fireActiveStepChanged();
        target.add(getPanel());
    }

    public void initNewRelationship(AjaxRequestTarget target) {
        int index = addWizardPartOnEnd(new InitRelationshipConnectorDevPartItem(getHelper()));
        clearInProgressPart();
        setActiveWizardPartIndex(index);
        fireActiveStepChanged();
        target.add(getPanel());
    }

    public void editBasicInformation(AjaxRequestTarget target) {
        setActiveStepById(BasicInformationConnectorStepPanel.PANEL_TYPE);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    public void editConnection(AjaxRequestTarget target) {
        setActiveStepById(ConnectionConnectorStepPanel.PANEL_TYPE);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    public void editDocumentation(AjaxRequestTarget target) {
        setActiveStepById(DocumentationConnectorStepPanel.PANEL_TYPE);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    public void showObjectClassesPanel(AjaxRequestTarget target) {
        setActiveStepById(ObjectClassesConnectorStepPanel.PANEL_TYPE);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    public void showRelationshipsPanel(AjaxRequestTarget target) {
        setActiveStepById(RelationshipsConnectorStepPanel.PANEL_TYPE);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    @Override
    protected @NotNull List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> createWizardPartItems() {

        InitConnectorDevPartItem initConnector = new InitConnectorDevPartItem(getHelper());
        if (!initConnector.isComplete()) {
            return List.of(new InitConnectorDevPartItem(getHelper()));
        }
        return createBasicPartItems();
    }

    private List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> createBasicPartItems() {
        List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> list = new ArrayList<>();
        list.add(new BasicConnectorDevPartItem(getHelper()));

        if (ConnectorDevelopmentWizardUtil.existContainerValue(
                getObjectWrapper(), ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS))) {
            list.add(new ObjectClassesConnectorDevPartItem(getHelper()));
        }

        if (ConnectorDevelopmentWizardUtil.existContainerValue(
                getObjectWrapper(), ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION))) {
            list.add(new RelationshipsConnectorDevPartItem(getHelper()));
        }

        InitObjectClassConnectorDevPartItem objectClassPartItem = null;
        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer != null && !objectClassContainer.getValues().isEmpty()) {
                for (PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue : objectClassContainer.getValues()) {
                    if (objectClassValue.getStatus() == ValueStatus.DELETED) {
                        continue;
                    }
                    InitObjectClassConnectorDevPartItem partItem = new InitObjectClassConnectorDevPartItem(getHelper());
                    partItem.setParameter(objectClassValue.getRealValue().getName());
                    if (!partItem.isComplete()) {
                        objectClassPartItem = partItem;
                        break;
                    }
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine wizard parts for object classes.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("ConnectorDevelopmentController.couldntDetermineObjectClasses");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
        }

        if (objectClassPartItem != null) {
            list.add(objectClassPartItem);
            return list;
        }

        try {
            PrismContainerWrapper<ConnDevRelationInfoType> relationshipsContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_RELATION));

            for (PrismContainerValueWrapper<ConnDevRelationInfoType> relationshipValue : relationshipsContainer.getValues()) {
                if (relationshipValue.getStatus() == ValueStatus.DELETED) {
                    continue;
                }
                InitRelationshipConnectorDevPartItem partItem = new InitRelationshipConnectorDevPartItem(getHelper());
                partItem.setParameter(relationshipValue.getRealValue().getName());
                if (!partItem.isComplete()) {
                    list.add(partItem);
                    break;
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine wizard parts for relationships.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("ConnectorDevelopmentController.couldntDetermineRelationships");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
        }
        return list;
    }

    @Override
    protected WizardStep createSummaryPanel() {
        return new ConnectorDevelopmentWizardSummaryPanel(getObjectDetailsModel());
    }

    @Override
    protected void resolveFinishPart(AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> oldActivePartItem) {
        if (oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.INIT) {
            setPartItems(createBasicPartItems());
            refresh();
            return;
        }

        if (oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.INIT_OBJECT_CLASS) {
            int index = addWizardPartOnEnd(new NextConnectorDevPartItem(getHelper()));
            clearInProgressPart();
            setActiveWizardPartIndex(index);
            return;
        }

        if (oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.INIT_RELATIONSHIP) {
            removeActivePartItem();
            clearActivePart();
            clearInProgressPart();
            showSummaryPanel();
            return;
        }

        super.resolveFinishPart(oldActivePartItem);
    }
}
