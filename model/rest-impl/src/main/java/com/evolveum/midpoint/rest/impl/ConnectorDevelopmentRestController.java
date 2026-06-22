/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.ConnectorGeneratorConstants;
import com.evolveum.midpoint.model.api.util.SmartIntegrationOperationExecutor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    public static final String CONTINUE_FROM = CLASS_DOT + "continueFrom";
    public static final String OPERATION_CREATE_CONNECTOR = CLASS_DOT + "CreateConnector";
    public static final String OPERATION_DISCOVER_BASIC_INFORMATION = CLASS_DOT + "DiscoverBasicInformation";
    public static final String OPERATION_DISCOVER_DOCUMENTATION = CLASS_DOT + "DiscoverDocumentation";
    public static final String OPERATION_PROCESS_DOCUMENTATION = CLASS_DOT + "ProcessDocumentation";
    public static final String OPERATION_GENERATE_ARTIFACT = CLASS_DOT + "GenerateArtifact";
    public static final String OPERATION_GENERATE_NATIVE_SCHEMA = CLASS_DOT + "GenerateNativeSchema";
    public static final String OPERATION_GENERATE_CONN_ID_SCHEMA = CLASS_DOT + "GenerateConnIdSchema";
    public static final String OPERATION_GENERATE_AUTHENTICATION_SCRIPT = CLASS_DOT + "GenerateAuthenticationScript";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_INFORMATION = CLASS_DOT + "DiscoverObjectClassInformation";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_ATTRIBUTES = CLASS_DOT + "DiscoverObjectClassAttribute";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_ENDPOINTS = CLASS_DOT + "DiscoverObjectClassEndpoints";

    @Autowired private ConnectorDevelopmentService connectorDevelopmentService;
    @Autowired private MidpointConfiguration configuration;
    @Autowired private ModelService modelService;

    @GetMapping(ConnectorGeneratorConstants.RPC_CONTINUE_FROM)
    public ResponseEntity<?> continueFrom(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, CONTINUE_FROM);

        try {
            return createResponse(
                    HttpStatus.OK,
                    getConnectorDevelopmentOperation(oid, task, result),
                    result
            );
        } catch (Exception e) {
            return handleException(result, e);
        } finally {
            finishRequest(task, result);
        }
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationCreateConnector(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_CREATE_CONNECTOR);

        return submitOperation(
                oid,
                task,
                result,
                (operation) -> operation.submitCreateConnector(task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_STATUS_INFO)
    public ResponseEntity<?> getCreateConnectorStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_CREATE_CONNECTOR);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getCreateConnectorStatus(token, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverBasicInformation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_BASIC_INFORMATION);

        return submitOperation(
                oid,
                task,
                result,
                (operation) -> operation.submitDiscoverBasicInformation(task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_STATUS_INFO)
    public ResponseEntity<?> getDiscoverBasicInformationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_BASIC_INFORMATION);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getDiscoverBasicInformationStatus(token, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_DOCUMENTATION);

        return submitOperation(
                oid,
                task,
                result,
                (operation) -> operation.submitDiscoverDocumentation(task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS_INFO)
    public ResponseEntity<?> getDiscoverDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_DOCUMENTATION);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getDiscoverDocumentationStatus(token, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationProcessDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_PROCESS_DOCUMENTATION);

        return submitOperation(
                oid,
                task,
                result,
                (operation) -> operation.submitProcessDocumentation(task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_STATUS_INFO)
    public ResponseEntity<?> getProcessDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_PROCESS_DOCUMENTATION);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getProcessDocumentationStatus(token, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_GENERATE_NATIVE_SCHEMA_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationGenerateArtifact(
            @RequestParam("oid") @NotNull String oid,
            @RequestParam("objectClass") @NotNull String objectClass,
            @RequestParam("retry") boolean retry
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_GENERATE_NATIVE_SCHEMA);

        return submitOperation(
                oid,
                task,
                result,
                (operation) ->
                        operation.submitGenerateNativeSchema(objectClass, retry, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_GENERATE_CONN_ID_SCHEMA_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationGenerateConnIdSchema(
            @RequestParam("oid") @NotNull String oid,
            @RequestParam("objectClass") @NotNull String objectClass,
            @RequestParam("retry") boolean retry
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_GENERATE_CONN_ID_SCHEMA);

        return submitOperation(
                oid,
                task,
                result,
                (operation) ->
                        operation.submitGenerateConnIdSchema(objectClass, retry, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_GENERATE_AUTHENTICATION_SCRIPT_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationGenerateAuthenticationScript(
            @RequestParam("oid") @NotNull String oid,
            @RequestParam("retry") boolean retry
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_GENERATE_AUTHENTICATION_SCRIPT);

        return submitOperation(
                oid,
                task,
                result,
                (operation) ->
                        operation.submitGenerateAuthenticationScript(retry, task, result)
        );
    }

    @PostMapping(ConnectorGeneratorConstants.RPC_GENERATE_ARTIFACT_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationGenerateArtifact(
            @RequestParam("oid") @NotNull String oid,
            @RequestParam("artifactType") @NotNull ConnDevArtifactType artifactType,
            @RequestParam("retry") boolean retry
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_GENERATE_ARTIFACT);

        return submitOperation(
                oid,
                task,
                result,
                (operation) ->
                        operation.submitGenerateArtifact(artifactType, retry, task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_GENERATE_ARTIFACT_STATUS_INFO)
    public ResponseEntity<?> getGenerateArtifactStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_GENERATE_ARTIFACT);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getGenerateArtifactStatus(token, task, result)
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DOWNLOAD_CONNECTOR)
    public ResponseEntity<?> downloadConnector(
            @RequestParam("name") @NotNull String name,
            HttpServletResponse response
    ) throws IOException {
        Configuration config = configuration.getConfiguration(MidpointConfiguration.ICF_CONFIGURATION);
        List<Object> dirs = config.getList("scanDirectory");

        File downloadDirectory = dirs.stream()
                .filter(d -> d != null && d.toString().contains("connid-connectors"))
                .findFirst()
                .map(Object::toString)
                .map(File::new)
                .orElse(null);

        if (downloadDirectory == null || !downloadDirectory.exists()) {
            return ResponseEntity.internalServerError().body("Base connector directory configuration not found.");
        }

        File connectorDir = new File(downloadDirectory, name);

        String canonicalBase = downloadDirectory.getCanonicalPath();
        String canonicalTarget = connectorDir.getCanonicalPath();

        if (!canonicalTarget.equals(canonicalBase + "/" + name)) {
            return ResponseEntity.internalServerError().body("Invalid name or version format.");
        }

        if (!connectorDir.exists() || !connectorDir.isDirectory()) {
            return ResponseEntity.internalServerError().body("Connector directory not found.");
        }

        response.setContentType("application/jar");
        String cleanFileName = name.replaceAll("[^a-zA-Z0-9.-]", "_") + ".jar";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanFileName + "\"");

        try (JarOutputStream jarOutputStream = new JarOutputStream(response.getOutputStream())) {
            jarDirectory(connectorDir, connectorDir, jarOutputStream);
            jarOutputStream.flush();
        }

        return ResponseEntity.ok().build();
    }

    private void jarDirectory(File rootDir, File currentFile, JarOutputStream jarOutputStream) throws IOException {
        if (currentFile == null || !currentFile.exists()) {
            return;
        }

        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    jarDirectory(rootDir, file, jarOutputStream);
                }
            }
        } else if (currentFile.isFile()) {
            String relativePath = rootDir.toURI().relativize(currentFile.toURI()).getPath();

            JarEntry entry = new JarEntry(relativePath);
            jarOutputStream.putNextEntry(entry);

            byte[] buffer = new byte[4096];
            try (FileInputStream fis = new FileInputStream(currentFile)) {
                int length;
                while ((length = fis.read(buffer)) >= 0) {
                    jarOutputStream.write(buffer, 0, length);
                }
            }
            jarOutputStream.closeEntry();
        }
    }

    private ConnectorDevelopmentOperation getConnectorDevelopmentOperation(
            String oid,
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
                oid,
                null,
                task,
                result
        );

        return connectorDevelopmentService.continueFrom(connectorDevelopmentType.asObjectable());
    }

    private ResponseEntity<?> submitOperation(
            String oid,
            Task task,
            OperationResult result,
            SmartIntegrationOperationExecutor<ConnectorDevelopmentOperation, String> operationExecutor
    ) {
        try {
            return createResponse(
                    HttpStatus.OK,
                    operationExecutor.execute(
                            getConnectorDevelopmentOperation(oid, task, result)
                    ),
                    result
            );
        } catch (Exception e) {
            return handleException(result, e);
        } finally {
            finishRequest(task, result);
        }
    }

    private ResponseEntity<?> handleStatusInfo(
            Task task,
            OperationResult result,
            SmartIntegrationOperationExecutor<ConnectorDevelopmentService, StatusInfo<?>> serviceExecutor
    ) {
        try {
            var statusInfo = serviceExecutor.execute(connectorDevelopmentService);
            var smartIntegrationOperationStatusInfoType = new SmartIntegrationOperationStatusInfoType();
            smartIntegrationOperationStatusInfoType.setStatus(statusInfo.getStatus());
            smartIntegrationOperationStatusInfoType.setMessage(statusInfo.getLocalizedMessage());
            smartIntegrationOperationStatusInfoType.setResult(getOperationResult(statusInfo));

            return createResponse(HttpStatus.OK, smartIntegrationOperationStatusInfoType, result);
        } catch (Exception e) {
            return handleException(result, e);
        } finally {
            finishRequest(task, result);
        }
    }

    private @NonNull AbstractSmartIntegrationOperationResultType getOperationResult(StatusInfo<?> statusInfo) {
        var abstractSmartIntegrationOperationResultType = new AbstractSmartIntegrationOperationResultType();

        if (statusInfo.getResult() instanceof ConnDevCreateConnectorResultType connDevCreateConnectorResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevCreateConnectorResult(connDevCreateConnectorResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverGlobalInformationResultType connDevDiscoverGlobalInformationResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverGlobalInformationResult(connDevDiscoverGlobalInformationResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverDocumentationResultType connDevDiscoverDocumentationResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverDocumentationResult(connDevDiscoverDocumentationResultType);
        } else if (statusInfo.getResult() instanceof ConnDevProcessDocumentationResultType connDevProcessDocumentationResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevProcessDocumentationResult(connDevProcessDocumentationResultType);
        } else if (statusInfo.getResult() instanceof ConnDevGenerateArtifactResultType connDevGenerateArtifactResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevGenerateArtifactResult(connDevGenerateArtifactResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverObjectClassInformationResultType connDevDiscoverObjectClassInformationResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverObjectClassInformationResult(connDevDiscoverObjectClassInformationResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverObjectClassAttributesResultType connDevDiscoverObjectClassAttributesResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverObjectClassAttributesResult(connDevDiscoverObjectClassAttributesResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverObjectClassEndpointsResultType connDevDiscoverObjectClassEndpointsResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverObjectClassEndpointsResult(connDevDiscoverObjectClassEndpointsResultType);
        } else if (statusInfo.getResult() instanceof ConnDevRefreshScimSchemaResultType connDevRefreshScimSchemaResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevRefreshScimSchemaResult(connDevRefreshScimSchemaResultType);
        } else if (statusInfo.getResult() instanceof ConnDevDiscoverConnectivityEndpointResultType connDevDiscoverConnectivityEndpointResultType) {
            abstractSmartIntegrationOperationResultType.setConnDevDiscoverConnectivityEndpointResult(connDevDiscoverConnectivityEndpointResultType);
        }

        return abstractSmartIntegrationOperationResultType;
    }
}
