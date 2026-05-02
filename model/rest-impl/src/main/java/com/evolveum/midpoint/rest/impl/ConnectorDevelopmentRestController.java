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
import java.util.function.BiFunction;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    private static final String OPERATION_UPSERT_CONNECTOR_DEVELOPMENT_TYPE = CLASS_DOT + "UpsertConnectorDevelopmentType";
    private static final String OPERATION_CREATE_CONNECTOR_STATUS = CLASS_DOT + "SubmitCreateConnector";
    public static final String OPERATION_DISCOVER_BASIC_INFORMATION_STATUS = "SubmitDiscoverBasicInformationStatus";
    public static final String OPERATION_DISCOVER_DOCUMENTATION_STATUS = "SubmitDiscoverDocumentationStatus";
    public static final String OPERATION_PROCESS_DOCUMENTATION_STATUS = "SubmitProcessDocumentationStatus";
    private static final String OPERATION_GENERATE_ARTIFACT_STATUS = CLASS_DOT + "SubmitGenerateArtifact";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_INFORMATION_STATUS = "SubmitDiscoverObjectClassInformationStatus";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_ATTRIBUTES_STATUS = "SubmitDiscoverObjectClassAttributeStatus";

    private static final int TIMEOUT = 1000;

    @Autowired private ConnectorDevelopmentService connectorDevelopmentService;
    @Autowired private ModelService modelService;

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

    private ResponseEntity<?> handleStatus(
            String connectorDevelopmentOid,
            String operationName,
            TriFunction<ConnectorDevelopmentOperation, Task, OperationResult, String> submitter,
            BiFunction<String, Task, StatusInfo<?>> statusFetcher
    ) {
        var task = initRequest();
        var result = createSubresult(task, operationName);

        try {
            var operation = getConnectorDevelopmentOperation(connectorDevelopmentOid, task, result);
            var token = submitter.apply(operation, task, result);
            StatusInfo<?> statusInfo;

            do {
                Thread.sleep(TIMEOUT);
                statusInfo = statusFetcher.apply(token, task);
            } while (statusInfo.isExecuting());

            return createResponse(HttpStatus.OK, statusInfo.getResult(), result);

        } catch (SchemaException |
                ObjectNotFoundException |
                ExpressionEvaluationException |
                SecurityViolationException |
                CommunicationException |
                ConfigurationException |
                InterruptedException e) {

            return handleException(result, e);
        }
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_UPSERT_CONNECTOR_DEVELOPMENT_TYPE)
    public ResponseEntity<?> upsertConnectorDevelopmentType(
            @RequestBody @NotNull ConnectorDevelopmentType connectorDevelopmentType
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_UPSERT_CONNECTOR_DEVELOPMENT_TYPE);

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

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_STATUS)
    public ResponseEntity<?> createConnectorStatus(
            @RequestParam("connectorDevelopmentOid") @NotNull String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_CREATE_CONNECTOR_STATUS,
                ConnectorDevelopmentOperation::submitCreateConnector,
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getCreateConnectorStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_CREATE_CONNECTOR_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_STATUS)
    public ResponseEntity<?> discoverBasicInformationStatus(
            @RequestParam("connectorDevelopmentOid") @NotNull String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_DISCOVER_BASIC_INFORMATION_STATUS,
                ConnectorDevelopmentOperation::submitDiscoverBasicInformation,
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverBasicInformationStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_DISCOVER_BASIC_INFORMATION_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getDiscoverDocumentationStatus(
            @RequestParam("connectorDevelopmentOid") String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_DISCOVER_DOCUMENTATION_STATUS,
                ConnectorDevelopmentOperation::submitDiscoverDocumentation,
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverDocumentationStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_DISCOVER_DOCUMENTATION_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_STATUS)
    public ResponseEntity<?> processDocumentationStatus(
            @RequestParam("connectorDevelopmentOid") @NotNull String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_PROCESS_DOCUMENTATION_STATUS,
                ConnectorDevelopmentOperation::submitProcessDocumentation,
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getProcessDocumentationStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_PROCESS_DOCUMENTATION_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_GENERATE_ARTIFACT_STATUS)
    public ResponseEntity<?> generateArtifactStatus(
            @RequestParam("connectorDevelopmentOid") @NotNull  String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_GENERATE_ARTIFACT_STATUS,
                (op, task, result) -> op.submitGenerateArtifact(null, false, task, result),
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getGenerateArtifactStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_GENERATE_ARTIFACT_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

//    TODO
//    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_OBJECT_CLASS_INFORMATION_STATUS)
//    public ResponseEntity<?> discoverObjectClassInfoStatus(
//            @RequestParam("connectorDevelopmentOid") @NotNull  String connectorDevelopmentOid
//    ) {
//    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_OBJECT_CLASS_ATTRIBUTES_STATUS)
    public ResponseEntity<?> discoverObjectClassAttributeStatus(
            @RequestParam("connectorDevelopmentOid") @NotNull  String connectorDevelopmentOid
    ) {
        return handleStatus(
                connectorDevelopmentOid,
                OPERATION_DISCOVER_OBJECT_CLASS_ATTRIBUTES_STATUS,
                (op, task, result) -> op.submitDiscoverObjectClassAttributes(null, task, result),
                (token, task) -> {
                    try {
                        return connectorDevelopmentService.getDiscoverObjectClassAttributesStatus(
                                token,
                                task,
                                createSubresult(task, OPERATION_DISCOVER_OBJECT_CLASS_ATTRIBUTES_STATUS));
                    } catch (SchemaException | ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
