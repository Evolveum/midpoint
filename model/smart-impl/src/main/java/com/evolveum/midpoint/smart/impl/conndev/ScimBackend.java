package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * SCIM backend. Schema discovery is connector-agnostic and lives in {@link ConnectorDevelopmentBackend}
 * (via the shared {@code conndev_ObjectClass} dev model); on top of that the SCIM connector exports the
 * raw discovered schema ({@code conndev_ScimSchema}/{@code conndev_ScimResource} — full /Schemas and
 * /ResourceTypes JSON), which is forwarded to the generation service as well: it carries details the
 * precomputed mapping does not (complex attributes, extension schemas).
 */
public class ScimBackend extends RestBackend {

    private static final String CONNDEV_SCIM_SCHEMA = "conndev_ScimSchema";
    private static final String CONNDEV_SCIM_RESOURCE = "conndev_ScimResource";

    public ScimBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    protected List<String> devDocumentationObjectClasses() {
        var objectClasses = new ArrayList<>(super.devDocumentationObjectClasses());
        objectClasses.add(CONNDEV_SCIM_SCHEMA);
        objectClasses.add(CONNDEV_SCIM_RESOURCE);
        return objectClasses;
    }
}
