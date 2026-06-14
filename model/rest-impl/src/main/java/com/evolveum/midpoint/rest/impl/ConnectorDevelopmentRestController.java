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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    public static final String OPERATION_CREATE_CONNECTOR = CLASS_DOT + "CreateConnector";
    public static final String OPERATION_DISCOVER_BASIC_INFORMATION = CLASS_DOT + "DiscoverBasicInformation";
    public static final String OPERATION_DISCOVER_DOCUMENTATION = CLASS_DOT + "DiscoverDocumentation";
    public static final String OPERATION_PROCESS_DOCUMENTATION = CLASS_DOT + "ProcessDocumentation";
    public static final String OPERATION_GENERATE_ARTIFACT = CLASS_DOT + "GenerateArtifact";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_INFORMATION = CLASS_DOT + "DiscoverObjectClassInformation";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_ATTRIBUTES = CLASS_DOT + "DiscoverObjectClassAttribute";
    public static final String OPERATION_DISCOVER_OBJECT_CLASS_ENDPOINTS = CLASS_DOT + "DiscoverObjectClassEndpoints";

    @Autowired private ConnectorDevelopmentService connectorDevelopmentService;
    @Autowired private MidpointConfiguration configuration;
    @Autowired private ModelService modelService;

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationCreateConnector(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_CREATE_CONNECTOR);

       try {
           var operation = getConnectorDevelopmentOperation(oid, task, result);
           return createResponse(HttpStatus.OK, operation.submitCreateConnector(task, result), result);
       } catch (Exception e) {
            return handleException(result, e);
       }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_STATUS)
    public ResponseEntity<?> getCreateConnectorStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_CREATE_CONNECTOR,
                ConnectorDevelopmentService::getCreateConnectorStatus,
                StatusInfo::getStatus
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_MESSAGE)
    public ResponseEntity<?> getCreateConnectorMessage(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_CREATE_CONNECTOR,
                ConnectorDevelopmentService::getCreateConnectorStatus,
                statusInfo -> Objects.requireNonNull(statusInfo.getMessage()).getFallbackMessage()
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_RESULT)
    public ResponseEntity<?> getCreateConnectorResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_CREATE_CONNECTOR,
                ConnectorDevelopmentService::getCreateConnectorStatus,
                StatusInfo::getResult
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverBasicInformation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_BASIC_INFORMATION);

        try {
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitDiscoverBasicInformation(task, result), result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_STATUS)
    public ResponseEntity<?> getDiscoverBasicInformationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_DISCOVER_BASIC_INFORMATION,
                ConnectorDevelopmentService::getDiscoverBasicInformationStatus,
                StatusInfo::getStatus
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_MESSAGE)
    public ResponseEntity<?> getDiscoverBasicInformationMessage(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_DISCOVER_BASIC_INFORMATION,
                ConnectorDevelopmentService::getDiscoverBasicInformationStatus,
                statusInfo -> Objects.requireNonNull(statusInfo.getMessage()).getFallbackMessage()
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_RESULT)
    public ResponseEntity<?> getDiscoverBasicInformationResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_DISCOVER_BASIC_INFORMATION,
                ConnectorDevelopmentService::getDiscoverBasicInformationStatus,
                StatusInfo::getResult
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_DISCOVER_DOCUMENTATION);

        try {
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitDiscoverDocumentation(task, result), result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getDiscoverDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_DISCOVER_DOCUMENTATION,
                ConnectorDevelopmentService::getDiscoverDocumentationStatus,
                StatusInfo::getStatus
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_MESSAGE)
    public ResponseEntity<?> getDiscoverDocumentationMessage(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_CREATE_CONNECTOR,
                ConnectorDevelopmentService::getCreateConnectorStatus,
                statusInfo -> Objects.requireNonNull(statusInfo.getMessage()).getFallbackMessage()
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_RESULT)
    public ResponseEntity<?> getDiscoverDocumentationResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_DISCOVER_DOCUMENTATION,
                ConnectorDevelopmentService::getDiscoverDocumentationStatus,
                StatusInfo::getResult
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationProcessDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_PROCESS_DOCUMENTATION);

        try {
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitProcessDocumentation(task, result), result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getProcessDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_PROCESS_DOCUMENTATION,
                ConnectorDevelopmentService::getProcessDocumentationStatus,
                StatusInfo::getStatus
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_MESSAGE)
    public ResponseEntity<?> getProcessDocumentationMessage(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_PROCESS_DOCUMENTATION,
                ConnectorDevelopmentService::getProcessDocumentationStatus,
                statusInfo -> Objects.requireNonNull(statusInfo.getMessage()).getFallbackMessage()
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_RESULT)
    public ResponseEntity<?> getProcessDocumentationResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(
                token,
                OPERATION_PROCESS_DOCUMENTATION,
                ConnectorDevelopmentService::getProcessDocumentationStatus,
                StatusInfo::getResult
        );
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DOWNLOAD_CONNECTOR)
    public ResponseEntity<?> downloadConnector(
            @RequestParam("bundleName") @NotNull String bundleName,
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

        File connectorDir = new File(downloadDirectory, bundleName);

        String canonicalBase = downloadDirectory.getCanonicalPath();
        String canonicalTarget = connectorDir.getCanonicalPath();

        if (!canonicalTarget.equals(canonicalBase + "/" + bundleName)) {
            return ResponseEntity.internalServerError().body("Invalid bundle name or version format.");
        }

        if (!connectorDir.exists() || !connectorDir.isDirectory()) {
            return ResponseEntity.internalServerError().body("Connector directory not found.");
        }

        response.setContentType("application/jar");
        String cleanFileName = bundleName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".jar";

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

    private ResponseEntity<?> handleStatusInfo(
            String token,
            String operationName,
            StatusInfoExecutor service,
            Function<StatusInfo<?>, Object> extractor
    ) {
        var task = initRequest();
        var result = createSubresult(task, operationName);

        try {
            var statusInfo = service.execute(connectorDevelopmentService, token, task, result);
            return createResponse(HttpStatus.OK, extractor.apply(statusInfo), result);
        } catch (Exception e) {
            return handleException(result, e);
        }
    }

    @FunctionalInterface
    public interface StatusInfoExecutor {
        StatusInfo<?> execute(ConnectorDevelopmentService service,
                String token,
                Task task,
                OperationResult result) throws SchemaException, ObjectNotFoundException;
    }
}
