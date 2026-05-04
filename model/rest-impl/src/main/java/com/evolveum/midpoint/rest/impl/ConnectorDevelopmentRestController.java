package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.ConnectorGeneratorConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    @Autowired private ConnectorDevelopmentService connectorDevelopmentService;
    @Autowired private ModelService modelService;

    @PostMapping(ConnectorGeneratorConstants.RPC_UPSERT_CONNECTOR_DEVELOPMENT_TYPE)
    public ResponseEntity<?> upsertConnectorDevelopmentType(
            @RequestBody @NotNull ConnectorDevelopmentType connectorDevelopmentType
    ) {
        var task = initRequest();
        var result = createSubresult(task, CLASS_DOT + ConnectorGeneratorConstants.OPERATION_UPSERT_CONNECTOR_DEVELOPMENT_TYPE);

        try {
            var prismObject = connectorDevelopmentType.asPrismObject();
            ObjectDelta<? extends ObjectType> delta;

            try {
                var foundObject = modelService.getObject(
                        ConnectorDevelopmentType.class,
                        Optional.ofNullable(connectorDevelopmentType.getOid())
                                .filter(o -> !o.isEmpty())
                                .orElseThrow(() -> new ObjectNotFoundException("OID is missing")),
                        null,
                        task,
                        result
                );
                delta = foundObject.diff(prismObject);
            } catch (ObjectNotFoundException e) {
                delta = DeltaFactory.Object.createAddDelta(prismObject);
            }

            var deltaOperations = modelService.executeChanges(
                    Collections.singletonList(delta),
                    null,
                    task,
                    result
            );

            return createResponse(
                    delta.getChangeType().equals(ChangeType.ADD)
                            ? HttpStatus.CREATED
                            : HttpStatus.OK,
                    modelService.getObject(
                            ConnectorDevelopmentType.class,
                            delta.getChangeType().equals(ChangeType.ADD)
                                    ? deltaOperations.iterator().next().getOid()
                                    : delta.getOid(),
                            null,
                            task,
                            result
                    ).asObjectable(),
                    result
            );
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            return handleException(result, e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperation(
            @RequestParam("operationName") @NotNull String operationName,
            @RequestParam("connectorDevelopmentOid") String connectorDevelopmentOid
    ) {
        try {
            switch (operationName) {
                case ConnectorGeneratorConstants.OPERATION_CREATE_CONNECTOR -> submitOperation(
                        connectorDevelopmentOid,
                        ConnectorGeneratorConstants.OPERATION_CREATE_CONNECTOR,
                        ConnectorDevelopmentOperation::submitDiscoverBasicInformation
                );
                case ConnectorGeneratorConstants.OPERATION_DISCOVER_DOCUMENTATION -> submitOperation(
                        connectorDevelopmentOid,
                        ConnectorGeneratorConstants.OPERATION_DISCOVER_DOCUMENTATION,
                        ConnectorDevelopmentOperation::submitDiscoverDocumentation
                );
                case ConnectorGeneratorConstants.OPERATION_PROCESS_DOCUMENTATION -> submitOperation(
                        connectorDevelopmentOid,
                        ConnectorGeneratorConstants.OPERATION_PROCESS_DOCUMENTATION,
                        ConnectorDevelopmentOperation::submitProcessDocumentation
                );
                default -> ResponseEntity.badRequest().body("Unsupported operation: " + operationName);
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_STATUS)
    public ResponseEntity<?> getDiscoverBasicInformationStatus(@RequestParam("statusOid") @NotNull String statusOid) {
        return handleStatus(
                statusOid,
                ConnectorGeneratorConstants.OPERATION_DISCOVER_BASIC_INFORMATION,
                (token, task, result) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverBasicInformationStatus(token, task, result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getDiscoverDocumentationStatus(@RequestParam("statusOid") @NotNull String statusOid) {
        return handleStatus(
                statusOid,
                ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS,
                (token, task, result) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverBasicInformationStatus(token, task, result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getProcessDocumentationStatus(@RequestParam("statusOid") @NotNull String statusOid) {
        return handleStatus(
                statusOid,
                ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS,
                (token, task, result) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverBasicInformationStatus(token, task, result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_GENERATE_ARTIFACT_STATUS)
    public ResponseEntity<?> getGenerateArtifactStatus(@RequestParam("statusOid") @NotNull String statusOid) {
        return handleStatus(
                statusOid,
                ConnectorGeneratorConstants.OPERATION_GENERATE_ARTIFACT,
                (token, task, result) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverBasicInformationStatus(token, task, result);
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private ResponseEntity<?> submitOperation(
            String connectorDevelopmentOid,
            String operationName,
            TriFunction<ConnectorDevelopmentOperation, Task, OperationResult, String> submitter
    ) {
        var task = initRequest();
        var result = createSubresult(task, CLASS_DOT + operationName);

        try {
            var operation = getConnectorDevelopmentOperation(connectorDevelopmentOid, task, result);
            var token = submitter.apply(operation, task, result);
            return createResponse(HttpStatus.OK, token, result);
        } catch (SchemaException |
                ObjectNotFoundException |
                ExpressionEvaluationException |
                SecurityViolationException |
                CommunicationException |
                ConfigurationException e) {
            return handleException(result, e);
        }
    }

    private ResponseEntity<?> handleStatus(
            String token,
            String operationName,
            TriFunction<String, Task, OperationResult, StatusInfo<?>> statusFetcher
    ) {
        var task = initRequest();
        var result = createSubresult(task, CLASS_DOT + operationName);

        try {
            var status = statusFetcher.apply(token, task, result);
            return createResponse(HttpStatus.OK, status, result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    private ConnectorDevelopmentOperation getConnectorDevelopmentOperation(
            String connectorDevelopmentOid,
            Task task,
            OperationResult result
    ) throws SchemaException,
            ExpressionEvaluationException,
            SecurityViolationException,
            CommunicationException,
            ConfigurationException,
            ObjectNotFoundException
    {
        PrismObject<ConnectorDevelopmentType> connectorDevelopmentType = modelService.getObject(
                ConnectorDevelopmentType.class,
                connectorDevelopmentOid,
                null,
                task,
                result
        );

        return connectorDevelopmentService.continueFrom(connectorDevelopmentType.asObjectable());
    }
}
