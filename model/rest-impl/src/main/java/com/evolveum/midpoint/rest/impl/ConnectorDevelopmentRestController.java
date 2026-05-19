package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
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

import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping({ "/ws/connector-generator", "/rest/connector-generator", "/api/connector-generator" })
public class ConnectorDevelopmentRestController extends AbstractRestController {

    private static final String CLASS_DOT = ConnectorDevelopmentRestController.class.getName() + ".";

    public static final String UPSERT_CONNECTOR_DEVELOPMENT_TYPE = CLASS_DOT + "UpsertConnectorDevelopmentType";
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

    @PostMapping(ConnectorGeneratorConstants.RPC_UPSERT_CONNECTOR_DEVELOPMENT_TYPE)
    public ResponseEntity<?> upsertConnectorDevelopmentType(
            @RequestBody @NotNull ConnectorDevelopmentType connectorDevelopmentType
    ) {
        var task = initRequest();
        var result = createSubresult(task, UPSERT_CONNECTOR_DEVELOPMENT_TYPE);

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

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationCreateConnector(
            @RequestParam("oid") @NotNull String oid
    ) {
       try {
           var task = initRequest();
           var result = createSubresult(task, OPERATION_CREATE_CONNECTOR);
           var operation = getConnectorDevelopmentOperation(oid, task, result);
           return createResponse(HttpStatus.OK, operation.submitCreateConnector(task, result), result);
       } catch (Exception e) {
            return handleException(e);
       }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_STATUS)
    public ResponseEntity<?> getCreateConnectorStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_CREATE_CONNECTOR, ConnectorDevelopmentService::getCreateConnectorStatus, StatusInfo::getStatus);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_CREATE_CONNECTOR_RESULT)
    public ResponseEntity<?> getCreateConnectorResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_CREATE_CONNECTOR, ConnectorDevelopmentService::getCreateConnectorStatus, StatusInfo::getResult);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverBasicInformation(
            @RequestParam("oid") @NotNull String oid
    ) {
        try {
            var task = initRequest();
            var result = createSubresult(task, OPERATION_DISCOVER_BASIC_INFORMATION);
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitDiscoverBasicInformation(task, result), result);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_STATUS)
    public ResponseEntity<?> getDiscoverBasicInformationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_DISCOVER_BASIC_INFORMATION, ConnectorDevelopmentService::getDiscoverBasicInformationStatus, StatusInfo::getStatus);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_BASIC_INFORMATION_RESULT)
    public ResponseEntity<?> getDiscoverBasicInformationResult(
            @RequestParam("token") @NotNull String token
    )  {
        return handleStatusInfo(token, OPERATION_DISCOVER_BASIC_INFORMATION, ConnectorDevelopmentService::getDiscoverBasicInformationStatus, StatusInfo::getResult);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationDiscoverDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        try {
            var task = initRequest();
            var result = createSubresult(task, OPERATION_DISCOVER_DOCUMENTATION);
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitDiscoverDocumentation(task, result), result);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getDiscoverDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_DISCOVER_DOCUMENTATION, ConnectorDevelopmentService::getDiscoverDocumentationStatus, StatusInfo::getStatus);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DISCOVER_DOCUMENTATION_RESULT)
    public ResponseEntity<?> getDiscoverDocumentationResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_DISCOVER_DOCUMENTATION, ConnectorDevelopmentService::getDiscoverDocumentationStatus, StatusInfo::getResult);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationProcessDocumentation(
            @RequestParam("oid") @NotNull String oid
    ) {
        try {
            var task = initRequest();
            var result = createSubresult(task, OPERATION_PROCESS_DOCUMENTATION);
            var operation = getConnectorDevelopmentOperation(oid, task, result);
            return createResponse(HttpStatus.OK, operation.submitProcessDocumentation(task, result), result);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_STATUS)
    public ResponseEntity<?> getProcessDocumentationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_PROCESS_DOCUMENTATION, ConnectorDevelopmentService::getProcessDocumentationStatus, StatusInfo::getStatus);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_PROCESS_DOCUMENTATION_RESULT)
    public ResponseEntity<?> getProcessDocumentationResult(
            @RequestParam("token") @NotNull String token
    ) {
        return handleStatusInfo(token, OPERATION_PROCESS_DOCUMENTATION, ConnectorDevelopmentService::getProcessDocumentationStatus, StatusInfo::getResult);
    }

    @GetMapping(ConnectorGeneratorConstants.RPC_DOWNLOAD_CONNECTOR)
    public ResponseEntity<?> downloadConnector(
            @RequestParam("bundleName") @NotNull String bundleName,
            @RequestParam("version") @NotNull String version
    ) {
        Configuration config = configuration.getConfiguration(MidpointConfiguration.ICF_CONFIGURATION);
        List<Object> dirs = config.getList("scanDirectory");

        File downloadDirectory = dirs.stream()
                .filter(d -> d != null && d.toString().contains("connid-connectors"))
                .findFirst()
                .map(Object::toString)
                .map(File::new)
                .orElse(null);

        if (downloadDirectory == null || !downloadDirectory.exists()) {
            return handleException(new RuntimeException("Base connector directory configuration not found."));
        }

        String targetDirName = bundleName + "." + version;
        File connectorDir = new File(downloadDirectory, targetDirName);

        try {
            String canonicalBase = downloadDirectory.getCanonicalPath();
            String canonicalTarget = connectorDir.getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalBase)) {
                return handleException(new SecurityException("Invalid bundle name or version format."));
            }
        } catch (IOException e) {
            return handleException(new RuntimeException("Failed to validate directory paths.", e));
        }

        if (!connectorDir.exists() || !connectorDir.isDirectory()) {
            return handleException(new RuntimeException("Connector directory not found."));
        }

        File[] filesToZip = connectorDir.listFiles();
        if (filesToZip == null || filesToZip.length == 0) {
            return handleException(new RuntimeException("Connector directory is empty."));
        }

        StreamingResponseBody responseBody = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream, java.nio.charset.StandardCharsets.UTF_8)) {
                byte[] buffer = new byte[4096];

                for (File file : filesToZip) {
                    if (file != null && file.exists() && file.isFile()) {
                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zipOut.putNextEntry(zipEntry);

                        try (FileInputStream fis = new FileInputStream(file)) {
                            int length;
                            while ((length = fis.read(buffer)) >= 0) {
                                zipOut.write(buffer, 0, length);
                            }
                        }
                        zipOut.closeEntry();
                    }
                }
                zipOut.flush();
            } catch (IOException e) {
                throw new RuntimeException("Error streaming ZIP file data", e);
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));

        String cleanFileName = targetDirName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".zip";
        headers.setContentDisposition(ContentDisposition.attachment().filename(cleanFileName).build());

        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
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
        try {
            var task = initRequest();
            var result = createSubresult(task, operationName);
            var statusInfo = service.execute(connectorDevelopmentService, token, task, result);
            return createResponse(HttpStatus.OK, extractor.apply(statusInfo), result);
        } catch (Exception e) {
            return handleException(e);
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
