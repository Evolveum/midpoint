/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import java.util.List;

import com.evolveum.midpoint.model.api.util.ConnectorGeneratorConstants;
import com.evolveum.midpoint.model.api.util.SmartIntegrationConstants;
import com.evolveum.midpoint.model.api.util.SmartIntegrationOperationExecutor;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.namespace.QName;

/**
 * REST service used for the communication with the smart integration service.
 * The typical client is midPoint Studio.
 */
@RestController
@RequestMapping({ "/ws/smart-integration", "/rest/smart-integration", "/api/smart-integration" })
public class SmartIntegrationRestController extends AbstractRestController {

    private static final String CLASS_DOT = SmartIntegrationRestController.class.getName() + ".";

    private static final String OPERATION_SUGGEST_OBJECT_TYPES = CLASS_DOT + "SuggestObjectTypes";
    private static final String OPERATION_SUGGEST_CORRELATIONS = CLASS_DOT + "SuggestCorrelations";
    private static final String OPERATION_SUGGEST_MAPPINGS = CLASS_DOT + "SuggestMappings";
    private static final String OPERATION_SUGGEST_FOCUS_TYPE = CLASS_DOT + "SuggestFocusType";
    private static final String OPERATION_SUGGEST_ASSOCIATION_TYPE = CLASS_DOT + "SuggestAssociations";

    @Autowired private SmartIntegrationService smartIntegrationService;

    /**
     * Suggests object types (and their delineations) for the given resource and object class.
     *
     * Returned body contains the serialized form of {@link ObjectTypesSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_OBJECT_TYPES_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationSuggestObjectTypes(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("objectClass") String objectClass
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_OBJECT_TYPES);

        return submitOperation(
                task,
                result,
                (service) -> service.submitSuggestObjectTypesOperation(
                        resourceOid,
                        QName.valueOf(objectClass),
                        List.of(),
                        null,
                        null,
                        task,
                        result
                )
        );
    }

    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_OBJECT_TYPES_STATUS_INFO)
    public ResponseEntity<?> getSuggestObjectTypesStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_OBJECT_TYPES);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getSuggestObjectTypesOperationStatus(token, task, result)
        );
    }

    /**
     * Suggests correlation for the given resource, kind and intent.
     *
     * Returned body contains the serialized form of {@link CorrelationSuggestionsType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_CORRELATIONS_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationSuggestCorrelations(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_CORRELATIONS);

        return submitOperation(
                task,
                result,
                (service) -> service.submitSuggestCorrelationOperation(
                        resourceOid,
                        ResourceObjectTypeIdentification.of(
                                ShadowKindType.fromValue(kind),
                                intent
                        ),
                        List.of(),
                        false,
                        task,
                        result
                )
        );
    }

    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_CORRELATIONS_STATUS_INFO)
    public ResponseEntity<?> getSuggestCorrelationsStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_CORRELATIONS);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getSuggestCorrelationOperationStatus(token, task, result)
        );
    }

    /**
     * Suggests mapping for the given resource, kind and intent.
     *
     * Returned body contains the serialized form of {@link MappingsSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_MAPPINGS_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationSuggestMappings(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent,
            @RequestParam("isInbound") Boolean isInbound
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_MAPPINGS);

        return submitOperation(
                task,
                result,
                (service) -> service.submitSuggestMappingsOperation(
                        resourceOid,
                        ResourceObjectTypeIdentification.of(
                                ShadowKindType.fromValue(kind),
                                intent
                        ),
                        isInbound,
                        null,
                        List.of(
                                DataAccessPermissionType.SCHEMA_ACCESS,
                                DataAccessPermissionType.RAW_DATA_ACCESS
                        ),
                        false,
                        task,
                        result
                )
        );
    }

    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_MAPPINGS_STATUS_INFO)
    public ResponseEntity<?> getSuggestMappingsStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_MAPPINGS);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getSuggestMappingsOperationStatus(token, task, result)
        );
    }

    /**
     * Suggests association type for the given resource
     *
     * Returned body contains the serialized form of {@link AssociationSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_ASSOCIATION_TYPE_SUBMIT_OPERATION)
    public ResponseEntity<?> submitOperationSuggestAssociations(
            @RequestParam("resourceOid") String resourceOid
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_ASSOCIATION_TYPE);

        return submitOperation(
                task,
                result,
                (service) -> service.submitSuggestAssociationsOperation(
                        resourceOid,
                        task,
                        result
                )
        );
    }

    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_ASSOCIATION_TYPE_STATUS_INFO)
    public ResponseEntity<?> getSuggestAssociationStatus(
            @RequestParam("token") @NotNull String token
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_ASSOCIATION_TYPE);

        return handleStatusInfo(
                task,
                result,
                (service) -> service.getSuggestAssociationsOperationStatus(token, task, result)
        );
    }

    /**
     * Suggests a discrete focus type for the application (resource) object type.
     *
     * Returned body contains the local part of the QName representing the focus type, e.g. `UserType`.
     * In the future, we may return a full QName, but for now we keep it simple. We spare the client from
     * having to parse the string representing the QName.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_FOCUS_TYPE_SUBMIT_OPERATION)
    public ResponseEntity<?> suggestFocusType(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent
    ) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_FOCUS_TYPE);

        try {
            var typeIdentification = ResourceObjectTypeIdentification.of(ShadowKindType.fromValue(kind), intent);
            var focusTypeName = smartIntegrationService.suggestFocusType(
                    resourceOid, typeIdentification, List.of(DataAccessPermissionType.SCHEMA_ACCESS), task, result);
            return createResponse(HttpStatus.OK, focusTypeName.getFocusType().getLocalPart(), result);
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }

    private ResponseEntity<?> submitOperation(
            Task task,
            OperationResult result,
            SmartIntegrationOperationExecutor<SmartIntegrationService, String> operationExecutor
    ) {
        try {
            return createResponse(
                    HttpStatus.OK,
                    operationExecutor.execute(smartIntegrationService),
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
            SmartIntegrationOperationExecutor<SmartIntegrationService, StatusInfo<?>> serviceExecutor
    ) {
        try {
            var statusInfo = serviceExecutor.execute(smartIntegrationService);
            var smartIntegrationOperationStatusInfoType = new SmartIntegrationOperationStatusInfoType();
            smartIntegrationOperationStatusInfoType.setStatus(statusInfo.getStatus());
            smartIntegrationOperationStatusInfoType.setMessage(statusInfo.getLocalizedMessage());
            smartIntegrationOperationStatusInfoType.setResult(statusInfo.getResult());

            return createResponse(HttpStatus.OK, smartIntegrationOperationStatusInfoType, result);
        } catch (Exception e) {
            return handleException(result, e);
        } finally {
            finishRequest(task, result);
        }
    }
}
