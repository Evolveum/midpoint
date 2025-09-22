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
            .name("OpenProject OpenAPI specificication")
            .description("OpenAPI specification")
            .uri("https://www.openproject.org/docs/api/v3/spec.yml");

    public RestTestBackend(ConnDevBeans beans, ConnectorDevelopmentType connDev, Task task, OperationResult result) {
        super(beans, connDev, task, result);
    }

    @Override
    public List<ConnDevDocumentationSourceType> discoverDocumentation() {
        return super.discoverDocumentation();
    }

    @Override
    public void processDocumentation() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        if (developmentObject().getApplication().getApplicationName().getNorm().contains("openproject")) {
            var documentations = new ArrayList<ProcessedDocumentation>();
            documentations.add(downloadAndCache(OPENAPI));

            var delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                    .item(ConnectorDevelopmentType.F_PROCESSED_DOCUMENTATION)
                    .addRealValues(documentations.stream().map(ProcessedDocumentation::toBean).toList())
                    .<ConnectorDevelopmentType>asObjectDelta(developmentObject().getOid());
            beans.modelService.executeChanges(List.of(delta), null, task, result);
        }
        try {
            super.processDocumentation();
        } catch (Exception e) {
            // Continue with already processed documentation
        }
    }
}
