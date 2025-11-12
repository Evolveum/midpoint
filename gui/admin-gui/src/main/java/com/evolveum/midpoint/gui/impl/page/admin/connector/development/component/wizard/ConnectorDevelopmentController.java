/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardController;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.AbstractWizardPartItem;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConnectorDevelopmentController extends AbstractWizardController<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentController.class);

    public void initNewObjectClass() {
        int index = addWizardPartAfter(new InitObjectClassConnectorDevPartItem(getHelper()), ConnectorDevelopmentStatusType.INIT);
        setActiveWizardPartIndex(index);
        fireActiveStepChanged();
    }

    public void initNewRelation() {
        int index = addWizardPartAfter(new InitRelationConnectorDevPartItem(getHelper()), ConnectorDevelopmentStatusType.INIT_OBJECT_CLASS);
        setActiveWizardPartIndex(index);
        fireActiveStepChanged();
    }

    enum ConnectorDevelopmentStatusType {
        INIT,
        BASIC,
        INIT_OBJECT_CLASS,
        OBJECT_CLASS_SCHEMA,
        OBJECT_CLASS_SEARCH_ALL,
        INIT_RELATION,
        RELATION,
        NEXT
    }

    public ConnectorDevelopmentController(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected @NotNull List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> createWizardPartItems() {

        List<AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel>> list = new ArrayList<>();

        InitConnectorDevPartItem initConnector = new InitConnectorDevPartItem(getHelper());
        if (!initConnector.isComplete()) {
            return List.of(new InitConnectorDevPartItem(getHelper()));
        }
        list.add(new BasicConnectorDevPartItem(getHelper()));

        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer != null && !objectClassContainer.getValues().isEmpty()) {
                objectClassContainer.getValues().stream()
                        .filter(value -> value.getStatus() != ValueStatus.DELETED)
                        .forEach(value -> {
                            InitObjectClassConnectorDevPartItem partItem = new InitObjectClassConnectorDevPartItem(getHelper());
                            partItem.setParameter(value.getRealValue().getName());
                            list.add(partItem);
                        });
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine wizard parts for object classes.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("ConnectorDevelopmentController.couldntDetermineObjectClasses");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
        }

        try {
            PrismContainerWrapper<ConnDevRelationInfoType> objectClassContainer = getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_RELATION));
            if (objectClassContainer != null && !objectClassContainer.getValues().isEmpty()) {
                objectClassContainer.getValues().stream()
                        .filter(value -> value.getStatus() != ValueStatus.DELETED)
                        .forEach(value -> {
                            InitRelationConnectorDevPartItem partItem = new InitRelationConnectorDevPartItem(getHelper());
                            partItem.setParameter(value.getRealValue().getName());
                            list.add(partItem);
                        });
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine wizard parts for object classes.", e);
            String message = getObjectDetailsModel().getPageAssignmentHolder().getString("ConnectorDevelopmentController.couldntDetermineObjectClasses");
            getObjectDetailsModel().getPageAssignmentHolder().error(message);
        }

        list.add(new NextConnectorDevPartItem(getHelper()));


        return list;
    }

    @Override
    protected void resolveFinishPart(AbstractWizardPartItem<ConnectorDevelopmentType, ConnectorDevelopmentDetailsModel> oldActiveStatus) {
        showSummaryPanel();
    }
}
