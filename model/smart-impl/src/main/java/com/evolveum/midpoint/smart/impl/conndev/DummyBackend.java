package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.activity.ConnDevBeans;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

public class DummyBackend extends ConnectorDevelopmentBackend {

    public DummyBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    public ConnDevApplicationInfoType discoverBasicInformation(RunningTask task, OperationResult result) {
        // FIXME: Other backends will have HTTP calls here
        return new ConnDevApplicationInfoType()
                .applicationName("Dummy Connector")
                .description("Dummy Backend Application")
                .version("2.0")
                .integrationType(ConnDevIntegrationType.DUMMY);

    }

    public List<ConnDevAuthInfoType> discoverAuthorizationInformation(RunningTask task, OperationResult result) {
        return List.of(
                new ConnDevAuthInfoType()
                        .name("Basic Authorization")
                        .type("basic")
                        .quirks("Username is `apiKey` and password is API Token.")
        );
    }

    public List<ConnDevDocumentationSourceType> discoverDocumentation() {
        return List.of(
                new  ConnDevDocumentationSourceType()
                        .name("Dummy Project - OpenAPI specification")
                        .description("OpenAPI documentation for Dummy Project")
                        .contentType("application/yaml")
                        .uri("https://community.openproject.org/api/v3/spec.yml"),
                new  ConnDevDocumentationSourceType()
                        .name("Dummy Project - API Docuemntation")
                        .description("OpenAPI documentation for Dummy Project")
                        .contentType("application/html")
                        .uri("https://community.openproject.org/api/v3")
        );
    }
}
