/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for testing the resource, which is from repo (contains OID).
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
class TestConnectionOperationResourceInRepo extends AbstractTestConnectionOperation {

    TestConnectionOperationResourceInRepo(@NotNull PrismObject<ResourceType> resource, @NotNull Task task, @NotNull CommonBeans beans) {
        super(resource, task, beans);
    }

    @Override
    protected PrismObject<ResourceType> getResourceToComplete(OperationResult schemaResult) throws SchemaException, ObjectNotFoundException {
        return beans.cacheRepositoryService.getObject(
                ResourceType.class, resource.getOid(), null, schemaResult);
    }

    @Override
    protected String createOperationDescription() {
        String resourceOid = resource.getOid();
        return "test resource " + resourceOid + " connection";
    }

    @Override
    protected void setResourceAvailabilityStatus(AvailabilityStatusType status, String statusChangeReason, OperationResult result)
            throws ObjectNotFoundException {
        beans.resourceManager.modifyResourceAvailabilityStatus(
                resource.getOid(), status, statusChangeReason, task, result, false);
    }
}
