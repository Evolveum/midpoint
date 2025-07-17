/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.api.util.SmartIntegrationConstants;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 * REST service used for the communication with the smart integration service.
 * The typical client is midPoint Studio.
 */
@RestController
@RequestMapping({ "/ws/smart-integration", "/rest/smart-integration", "/api/smart-integration" })
public class SmartIntegrationRestController extends AbstractRestController {

    private static final String CLASS_DOT = SmartIntegrationRestController.class.getName() + ".";

    private static final String OPERATION_SUGGEST_OBJECT_TYPES = CLASS_DOT + "suggestObjectTypes";
    private static final String OPERATION_SUGGEST_FOCUS_TYPE = CLASS_DOT + "suggestFocusType";

    @Autowired private SmartIntegrationService smartIntegrationService;

//    /**
//     * Suggests object types (and their delineations) for the given resource and object class.
//     *
//     * Returned body contains the serialized form of {@link ObjectTypesSuggestionType}.
//     */
//    @GetMapping(SmartIntegrationConstants.RPC_SUGGEST_OBJECT_TYPES)
//    public ResponseEntity<?> suggestObjectTypes(
//            @RequestParam("resourceOid") String resourceOid,
//            @RequestParam("objectClassName") String objectClassName) {
//        var task = initRequest();
//        var result = createSubresult(task, OPERATION_SUGGEST_OBJECT_TYPES);
//        try {
//            var types = smartIntegrationService.suggestObjectTypes(
//                    resourceOid, new QName(NS_RI, objectClassName), task, result);
//            return createResponse(HttpStatus.OK, types, result);
//        } catch (Throwable t) {
//            return handleException(result, t);
//        } finally {
//            finishRequest(task, result);
//        }
//    }

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
            return createResponse(HttpStatus.OK, focusTypeName.getLocalPart(), result);
        } catch (Throwable t) {
            return handleException(result, t);
        } finally {
            finishRequest(task, result);
        }
    }
}
