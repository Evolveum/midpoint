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

public class JsonHalBackend extends RestBackend {

    private static final ConnDevDocumentationSourceType OPENAPI = new ConnDevDocumentationSourceType()
            .name("OpenProject OpenAPI specificaiton")
            .description("OpenAPI specification")
            .uri("file:///home/evolveum/vaia/vaia-foundry/eval-data/wp1/openproject/docs/spec.yml");

    public JsonHalBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation() {
        return List.of(OPENAPI);
    }

    @Override
    public void processDocumentation() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        var documentations = new ArrayList<ProcessedDocumentation>();
        documentations.add(downloadAndCache(OPENAPI));

        var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                .addRealValues(documentations.stream().map(ProcessedDocumentation::toBean).toList())
                .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
        beans.modelService.executeChanges(List.of(delta), null, task, result);
    }
}
