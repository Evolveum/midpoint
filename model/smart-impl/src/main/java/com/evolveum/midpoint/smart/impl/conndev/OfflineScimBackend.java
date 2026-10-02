package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

/**
 * Offline backend for SCIM connectors. SCIM is a specialization of REST (mirroring
 * {@code ScimBackend extends RestBackend}); the skeleton generator produces SCIM-flavored
 * artifacts (the {@code scim} authentication namespace, the built-in {@code id} to {@code UID}
 * and {@code userName} to {@code NAME} ConnId mappings).
 */
public class OfflineScimBackend extends OfflineRestBackend {

    public OfflineScimBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }
}
