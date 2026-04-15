package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import java.util.ArrayList;
import java.util.List;

public class RestTestBackend extends RestBackend {

    private static final ConnDevDocumentationSourceType OPENAPI = new ConnDevDocumentationSourceType()
            .name("OpenProject OpenAPI")
            .description("OpenAPI specification")
            .uri("https://www.openproject.org/docs/api/");

    public RestTestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation() {
        var ret = new ArrayList<>(super.discoverDocumentation());
        ret.add(OPENAPI);
        return ret;
    }
}
