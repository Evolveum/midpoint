/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

/**
 * Responsible for testing the resource which isn't in repo (doesn't contain OID).
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
class TestPartialConfigurationOperation extends TestConnectionOperation {

    TestPartialConfigurationOperation(@NotNull PrismObject<ResourceType> resource, @NotNull Task task, @NotNull CommonBeans beans) {
        super(resource, task, beans);
    }

    @Override
    protected String createOperationDescription() {
        PolyString resourceName = resource.getName();
        if (resourceName != null) {
            return "test partial configuration of resource " + resourceName;
        } else {
            return "test partial configuration of resource:" + resource;
        }
    }

    /**
     * Test partial configuration.
     *
     * @throws ObjectNotFoundException If the resource object cannot be found in repository (e.g. when trying to set its
     * availability status).
     */
    public void execute(OperationResult result){
        ConnectorSpec connectorSpec = beans.resourceManager.getDefaultConnectorSpec(resource);
        OperationResult connectorTestResult = createSubresultForTest(connectorSpec, result);

        try {
            testConnector(connectorSpec, null, connectorTestResult);
        } catch (ObjectNotFoundException e) {
            //should not occur, because using resource, which isn't in repo
            connectorTestResult.recordFatalError(e);
        }

        connectorTestResult.computeStatus();
    }

    @Override
    protected void testConnection(ConnectorInstance connector, OperationResult parentResult) {
        connector.testPartialConfiguration(parentResult);
    }

    @Override
    protected String getTestName() {
        return "Partial configuration test";
    }

    @Override
    protected boolean isTestingCapabilities() {
        return false;
    }

    @Override
    protected PrismObject<ResourceType> getResourceToComplete(OperationResult schemaResult) {
        return null;
    }

    @Override
    protected void setResourceAvailabilityStatus(AvailabilityStatusType status, String statusChangeReason, OperationResult result) {
        // don't save status to resource
    }
    protected boolean cachingConnector() {
        return false;
    }

}
