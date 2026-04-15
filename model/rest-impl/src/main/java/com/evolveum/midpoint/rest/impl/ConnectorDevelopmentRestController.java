package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.ConnectorGeneratorConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    private static final String OPERATION_CONNECTOR_DEVELOPMENT_CONTINUE_FROM = CLASS_DOT + "ConnectorDevelopmentTypeCreate";
    private static final String OPERATION_CONNECTOR_DEVELOPMENT_DOCUMENTATION = CLASS_DOT + "ConnectorDevelopmentDocumentations";

    private static final int TIMEOUT = 1000;

    @Autowired private ConnectorDevelopmentService connectorDevelopmentService;
    @Autowired private ModelService modelService;

    @PostMapping(ConnectorGeneratorConstants.RPC_CONNECTOR_GENERATOR_CREATE_CONNECTOR_DEVELOPMENT)
    public ResponseEntity<?> createConnectorDevelopmentType(@RequestBody @NotNull ConnectorDevelopmentType connectorDevelopmentType) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_CONNECTOR_DEVELOPMENT_CONTINUE_FROM);

        try {
            var deltas = MiscSchemaUtil.createCollection(DeltaFactory.Object.createAddDelta(connectorDevelopmentType.asPrismObject()));
            var deltaOperations = modelService.executeChanges(deltas, null, task, result);
            var deltaOperation = deltaOperations.stream()
                    .filter(op -> op.getExecutionResult() != null &&
                            op.getExecutionResult().isSuccess())
                    .findFirst()
                    .orElseThrow(() -> new Exception("Failed to create Connector development"));

            return createResponse(HttpStatus.CREATED, deltaOperation.getObjectDelta().getObjectToAdd().asObjectable(), result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CONNECTOR_GENERATOR_DISCOVER_DOCUMENTATION)
    public ResponseEntity<?> getDiscoverDocumentation(@RequestParam("connectorDevelopmentOid") String connectorDevelopmentOid) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_CONNECTOR_DEVELOPMENT_DOCUMENTATION);

        try {
            PrismObject<ConnectorDevelopmentType> connectorDevelopmentType = modelService.getObject(
                    ConnectorDevelopmentType.class,
                    connectorDevelopmentOid,
                    null,
                    task,
                    result
            );

            ConnectorDevelopmentOperation connectorDevelopmentOperation = connectorDevelopmentService.continueFrom(
                    connectorDevelopmentType.asObjectable());
            result.setBackgroundTaskOid(connectorDevelopmentOperation.getObject().getOid());
            String token = connectorDevelopmentOperation.submitDiscoverDocumentation(task, result);
            var statusInfo = connectorDevelopmentService.getDiscoverDocumentationStatus(token, task, result);

            do {
                Thread.sleep(TIMEOUT);
                statusInfo = connectorDevelopmentService.getDiscoverDocumentationStatus(token, task, result);
            } while (statusInfo.isExecuting());

            return createResponse(HttpStatus.OK, statusInfo.getResult(), result);
        } catch (SchemaException |
                ObjectNotFoundException |
                ExpressionEvaluationException |
                SecurityViolationException |
                CommunicationException |
                ConfigurationException |
                InterruptedException e
        ) {
            return handleException(result, e);
        }
    }
}
