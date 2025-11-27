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
        OBJECT_CLASS_CREATE,
        OBJECT_CLASS_UPDATE,
        OBJECT_CLASS_DELETE,
        RELATIONSHIPS,
        INIT_RELATIONSHIP,
        RELATIONSHIP,
        NEXT
    }

    public ConnectorDevelopmentController(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    public void initNewObjectClass(AjaxRequestTarget target) {
        setPartItem(new InitObjectClassConnectorDevPartItem(getHelper()), target);
    }

    public void initNewRelationship(AjaxRequestTarget target) {
        setPartItem(new InitRelationshipConnectorDevPartItem(getHelper()), target);
    }

    public void editBasicInformation(AjaxRequestTarget target) {
        setActiveStepById(BasicInformationConnectorStepPanel.PANEL_TYPE, target);
    }

    public void editConnection(AjaxRequestTarget target) {
        setActiveStepById(ConnectionConnectorStepPanel.PANEL_TYPE, target);
    }

    public void editDocumentation(AjaxRequestTarget target) {
        setActiveStepById(DocumentationConnectorStepPanel.PANEL_TYPE, target);
    }

    public void editSchema(String objectClassName, AjaxRequestTarget target) {
        setPartItem(new SchemaConnectorDevPartItem(getHelper()), objectClassName, target);
    }

    public void editSearchAll(String objectClassName, AjaxRequestTarget target) {
        setPartItem(new SearchAllConnectorDevPartItem(getHelper()), objectClassName, target);
    }

    public void editCreateOp(String objectClassName, AjaxRequestTarget target) {
        setPartItem(new CreateConnectorDevPartItem(getHelper()), objectClassName, target);
    }

    public void editUpdateOp(String objectClassName, AjaxRequestTarget target) {
        setPartItem(new UpdateConnectorDevPartItem(getHelper()), objectClassName, target);
    }

    public void editDeleteOp(String objectClassName, AjaxRequestTarget target) {
        setPartItem(new DeleteConnectorDevPartItem(getHelper()), objectClassName, target);
    }

    public void showObjectClassesPanel(AjaxRequestTarget target) {
        setActiveStepById(ObjectClassesConnectorStepPanel.PANEL_TYPE, target);
    }

    public void showRelationshipsPanel(AjaxRequestTarget target) {
        setActiveStepById(RelationshipsConnectorStepPanel.PANEL_TYPE, target);
    }

    private void setActiveStepById(String stepId, AjaxRequestTarget target) {
        setActiveStepById(stepId);
        fireActiveStepChanged(getActiveStep());
        target.add(getPanel());
    }

    private void setPartItem(
            AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> partItem,
            AjaxRequestTarget target) {
        setPartItem(partItem, null, target);
    }

    private void setPartItem(
            AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> partItem,
            String objectClassName,
            AjaxRequestTarget target) {
        setPartItems(createBasicPartItems());
        refresh();
        partItem.setParameter(objectClassName);
        int index = addWizardPartOnEnd(partItem);
        clearInProgressPart();
        setActiveWizardPartIndex(index);
        fireActiveStepChanged();
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

        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer != null && !objectClassContainer.getValues().isEmpty()) {
                for (PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue : objectClassContainer.getValues()) {
                    if (objectClassValue.getStatus() == ValueStatus.DELETED) {
                        continue;
                    }

                    String objectClassName = objectClassValue.getRealValue().getName();

                    InitObjectClassConnectorDevPartItem partItem = new InitObjectClassConnectorDevPartItem(getHelper());
                    partItem.setParameter(objectClassName);
                    if (!partItem.isComplete()) {
                        list.add(partItem);
                        return list;
                    }

                    if (isPartInProgress(new SchemaConnectorDevPartItem(getHelper()), list, objectClassName)
                            || isPartInProgress(new SearchAllConnectorDevPartItem(getHelper()), list, objectClassName)
                            || isPartInProgress(new CreateConnectorDevPartItem(getHelper()), list, objectClassName)
                            || isPartInProgress(new UpdateConnectorDevPartItem(getHelper()), list, objectClassName)
                            || isPartInProgress(new DeleteConnectorDevPartItem(getHelper()), list, objectClassName)) {
                        return list;
                    }
                }
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine wizard parts for object classes.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("ConnectorDevelopmentController.couldntDetermineObjectClasses");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
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

    private boolean isPartInProgress(
            AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> partItem,
            List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> list,
            String objectClassName) {
        partItem.setParameter(objectClassName);
        if (partItem.isStarted() && !partItem.isComplete()) {
            list.add(partItem);
            return true;
        }
        return false;
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

        if (oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.INIT_OBJECT_CLASS
                || oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.OBJECT_CLASS_SCHEMA
                || oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.OBJECT_CLASS_SEARCH_ALL
                || oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.OBJECT_CLASS_CREATE
                || oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.OBJECT_CLASS_UPDATE
                || oldActivePartItem.getIdentifierForWizardStatus() == ConnectorDevelopmentStatusType.OBJECT_CLASS_DELETE) {
            setPartItems(createBasicPartItems());
            refresh();
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
