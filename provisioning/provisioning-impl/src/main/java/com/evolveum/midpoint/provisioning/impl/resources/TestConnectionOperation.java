/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;


import org.jetbrains.annotations.NotNull;

/**
 * Responsible for testing the resource which isn't in repo (doesn't contain OID).
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
class TestConnectionOperation extends AbstractTestConnectionOperation {

    TestConnectionOperation(@NotNull PrismObject<ResourceType> resource, @NotNull Task task, @NotNull CommonBeans beans) {
        super(resource, task, beans);
    }

    @Override
    protected PrismObject<ResourceType> getResourceToComplete(OperationResult schemaResult) {
        return this.resource;
    }

    @Override
    protected String createOperationDescription() {
        PolyString resourceName = resource.getName();
        if (resourceName != null) {
            return "test resource " + resourceName + " connection";
        } else {
            return "test resource connection, resource:" + resource;
        }
    }

    @Override
    protected void setResourceAvailabilityStatus(AvailabilityStatusType status, String statusChangeReason, OperationResult result) {
        beans.resourceManager.modifyResourceAvailabilityStatus(resource, status, statusChangeReason);
    }

}
