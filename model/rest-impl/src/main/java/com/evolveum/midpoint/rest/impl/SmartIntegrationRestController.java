/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.util.SmartIntegrationConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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

    private static final String OPERATION_SUGGEST_OBJECT_TYPES = CLASS_DOT + "suggestObjectTypes";
    private static final String OPERATION_SUGGEST_CORRELATIONS = CLASS_DOT + "suggestCorrelations";
    private static final String OPERATION_SUGGEST_MAPPINGS = CLASS_DOT + "suggestMappings";
    private static final String OPERATION_SUGGEST_FOCUS_TYPE = CLASS_DOT + "suggestFocusType";
    private static final String OPERATION_SUGGEST_ASSOCIATION_TYPE = CLASS_DOT + "suggestAssociations";

    private static final int TIMEOUT = 1000;

    @Autowired private SmartIntegrationService smartIntegrationService;
    @Autowired private ModelService modelService;

    /**
     * Suggests object types (and their delineations) for the given resource and object class.
     *
     * Returned body contains the serialized form of {@link ObjectTypesSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_OBJECT_TYPES)
    public ResponseEntity<?> suggestObjectTypes(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("objectClass") String objectClass) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_OBJECT_TYPES);

        try {
            QName objectClassQName = QName.valueOf(objectClass);
            var oid = smartIntegrationService.submitSuggestObjectTypesOperation(resourceOid, objectClassQName, task, result);
            result.setBackgroundTaskOid(oid);

            var suggestionOperationStatus = smartIntegrationService.getSuggestObjectTypesOperationStatus(oid, task, result);

            do {
                Thread.sleep(TIMEOUT);
                suggestionOperationStatus = smartIntegrationService.getSuggestObjectTypesOperationStatus(oid, task, result);
            } while (suggestionOperationStatus.isExecuting());

            if (suggestionOperationStatus.getStatus().equals(OperationResultStatusType.SUCCESS)) {
                return createResponse(HttpStatus.OK, suggestionOperationStatus.getResult(), result);
            } else {
                throw new IllegalStateException();
            }
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }

    /**
     * Suggests correlation for the given resource, kind and intent.
     *
     * Returned body contains the serialized form of {@link CorrelationSuggestionsType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_CORRELATIONS)
    public ResponseEntity<?> suggestCorrelations(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_CORRELATIONS);

        try {
            ResourceObjectTypeIdentification resourceObjectTypeIdentification = ResourceObjectTypeIdentification.of(
                    ShadowKindType.fromValue(kind),
                    intent
            );
            var oid = smartIntegrationService.submitSuggestCorrelationOperation(resourceOid, resourceObjectTypeIdentification, task, result);
            result.setBackgroundTaskOid(oid);

            var suggestionOperationStatus = smartIntegrationService.getSuggestCorrelationOperationStatus(oid, task, result);

            do {
                Thread.sleep(TIMEOUT);
                suggestionOperationStatus = smartIntegrationService.getSuggestCorrelationOperationStatus(oid, task, result);
            } while (suggestionOperationStatus.isExecuting());

            if (suggestionOperationStatus.getStatus().equals(OperationResultStatusType.SUCCESS)) {
                return createResponse(HttpStatus.OK, suggestionOperationStatus.getResult(), result);
            } else {
                throw new IllegalStateException();
            }
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }

    /**
     * Suggests mapping for the given resource, kind and intent.
     *
     * Returned body contains the serialized form of {@link MappingsSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_MAPPINGS)
    public ResponseEntity<?> suggestMappings(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent,
            @RequestParam("isInbound") Boolean isInbound) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_MAPPINGS);

        try {
            ResourceObjectTypeIdentification resourceObjectTypeIdentification = ResourceObjectTypeIdentification.of(
                    ShadowKindType.fromValue(kind),
                    intent
            );
            var oid = smartIntegrationService.submitSuggestMappingsOperation(resourceOid, resourceObjectTypeIdentification, isInbound, task, result);
            result.setBackgroundTaskOid(oid);

            var suggestionOperationStatus = smartIntegrationService.getSuggestMappingsOperationStatus(oid, task, result);

            do {
                Thread.sleep(TIMEOUT);
                suggestionOperationStatus = smartIntegrationService.getSuggestMappingsOperationStatus(oid, task, result);
            } while (suggestionOperationStatus.isExecuting());

            if (suggestionOperationStatus.getStatus().equals(OperationResultStatusType.SUCCESS)) {
                return createResponse(HttpStatus.OK, suggestionOperationStatus.getResult(), result);
            } else {
                throw new IllegalStateException();
            }
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }

    /**
     * Suggests association type for the given resource
     *
     * Returned body contains the serialized form of {@link AssociationSuggestionType}.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_ASSOCIATION_TYPE)
    public ResponseEntity<?> suggestAssociations(
            @RequestParam("resourceOid") String resourceOid) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_ASSOCIATION_TYPE);

        try {
            var oid = smartIntegrationService.submitSuggestAssociationsOperation(resourceOid, task, result);
            result.setBackgroundTaskOid(oid);

            var suggestionOperationStatus = smartIntegrationService.getSuggestAssociationsOperationStatus(oid, task, result);

            do {
                Thread.sleep(TIMEOUT);
                suggestionOperationStatus = smartIntegrationService.getSuggestAssociationsOperationStatus(oid, task, result);
            } while (suggestionOperationStatus.isExecuting());

            if (suggestionOperationStatus.getStatus().equals(OperationResultStatusType.SUCCESS)) {
                return createResponse(HttpStatus.OK, suggestionOperationStatus.getResult(), result);
            } else {
                throw new IllegalStateException();
            }
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }

    /**
     * Suggests a discrete focus type for the application (resource) object type.
     *
     * Returned body contains the local part of the QName representing the focus type, e.g. `UserType`.
     * In the future, we may return a full QName, but for now we keep it simple. We spare the client from
     * having to parse the string representing the QName.
     */
    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_FOCUS_TYPE)
    public ResponseEntity<?> suggestFocusType(
            @RequestParam("resourceOid") String resourceOid,
            @RequestParam("kind") String kind,
            @RequestParam("intent") String intent) {
        var task = initRequest();
        var result = createSubresult(task, OPERATION_SUGGEST_FOCUS_TYPE);
        try {
            var typeIdentification = ResourceObjectTypeIdentification.of(ShadowKindType.fromValue(kind), intent);
            var focusTypeName = smartIntegrationService.suggestFocusType(resourceOid, typeIdentification, task, result);
            return createResponse(HttpStatus.OK, focusTypeName.getFocusType().getLocalPart(), result);
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }
}
