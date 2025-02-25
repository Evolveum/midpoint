/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import static org.springframework.http.ResponseEntity.status;

import static com.evolveum.midpoint.security.api.RestAuthorizationAction.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.RestHandlerMethod;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@RestController
@RequestMapping({ "/ws/rest", "/rest/model", "/api/model" })
public class ModelRestController extends AbstractRestController {

    public static final String GET_OBJECT_PATH = "/{type}/{id}";

    private static final String CURRENT = "current";
    private static final long WAIT_FOR_TASK_STOP = 2000L;
    private static final String METADATA_SUFFIX = "@metadata";

    @Autowired private ModelCrudService model;
    @Autowired private ModelDiagnosticService modelDiagnosticService;
    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private ModelService modelService;
    @Autowired private BulkActionsService bulkActionsService;
    @Autowired private TaskService taskService;
    @Autowired private CaseService caseService;

    @RestHandlerMethod(authorization = GENERATE_VALUE)
    @PostMapping("/{type}/{oid}/generate")
    public ResponseEntity<?> generateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "generateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = generateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RPC_GENERATE_VALUE)
    @PostMapping("/rpc/generate")
    public ResponseEntity<?> generateValueRpc(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("generateValueRpc");

        ResponseEntity<?> response = generateValue(null, policyItemsDefinition, task, result);
        finishRequest(task, result);

        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> generateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult parentResult) {

        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
        } else {
            try {
                modelInteraction.generateValue(object, policyItemsDefinition, task, parentResult);
                parentResult.computeStatusIfUnknown();
                if (parentResult.isSuccess()) {
                    response = createResponse(HttpStatus.OK, policyItemsDefinition, parentResult, true);
                } else {
                    response = createResponse(HttpStatus.BAD_REQUEST, parentResult, parentResult);
                }

            } catch (Exception ex) {
                parentResult.recordFatalError("Failed to generate value, " + ex.getMessage(), ex);
                response = handleException(parentResult, ex);
            }
        }
        return response;
    }

    @RestHandlerMethod(authorization = VALIDATE_VALUE)
    @PostMapping("/{type}/{oid}/validate")
    public ResponseEntity<?> validateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("validateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = validateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RPC_VALIDATE_VALUE)
    @PostMapping("/rpc/validate")
    public ResponseEntity<?> validateValue(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("validateValue");

        ResponseEntity<?> response = validateValue(null, policyItemsDefinition, task, result);
        finishRequest(task, result);
        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> validateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult result) {
        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", result);
            finishRequest(task, result);
            return response;
        }

        if (CollectionUtils.isEmpty(policyItemsDefinition.getPolicyItemDefinition())) {
            response = createBadPolicyItemsDefinitionResponse("No definitions for items", result);
            finishRequest(task, result);
            return response;
        }

        try {
            modelInteraction.validateValue(object, policyItemsDefinition, task, result);

            result.computeStatusIfUnknown();
            if (result.isAcceptable()) {
                response = createResponse(HttpStatus.OK, policyItemsDefinition, result, true);
            } else {
                response = ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            }

        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }

        return response;
    }

    private ResponseEntity<?> createBadPolicyItemsDefinitionResponse(
            String message, OperationResult parentResult) {
        logger.error(message);
        parentResult.recordFatalError(message);
        return status(HttpStatus.BAD_REQUEST).body(parentResult);
    }

    @RestHandlerMethod(authorization = GET_VALUE_POLICY)
    @GetMapping("/users/{id}/policy")
    public ResponseEntity<?> getValuePolicyForUser(
            @PathVariable("id") String oid) {
        logger.debug("getValuePolicyForUser start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getValuePolicyForUser");

        ResponseEntity<?> response;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());
            PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, result);

            CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(user, task, result);

            response = createResponse(HttpStatus.OK, policy, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);

        logger.debug("getValuePolicyForUser finish");
        return response;
    }

    @RestHandlerMethod(authorization = GET_OBJECT)
    @GetMapping(GET_OBJECT_PATH)
    public ResponseEntity<?> getObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        logger.debug("model rest service for get operation start");

        Task task = initRequest();
        OperationResult result = createSubresult(task, "getObject");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Collection<SelectorOptions<GetOperationOptions>> getOptions =
                GetOperationOptions.fromRestOptions(options, include, exclude,
                        resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object;
            if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
                object = getCurrentNodeObject(getOptions, task, result);
            } else {
                object = model.getObject(clazz, id, getOptions, task, result);
            }
            if (exclude != null) {
                removeExcludes(object, exclude);
            }

            response = createResponse(HttpStatus.OK, object, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    private PrismObject<? extends ObjectType> getCurrentNodeObject(Collection<SelectorOptions<GetOperationOptions>> getOptions,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        String nodeId = taskManager.getNodeId();
        ObjectQuery query = prismContext.queryFor(NodeType.class)
                .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                .build();
        List<PrismObject<NodeType>> objects = model.searchObjects(NodeType.class, query, getOptions, task, result);
        if (objects.isEmpty()) {
            throw new ObjectNotFoundException("Current node (id " + nodeId + ") couldn't be found.", NodeType.class, nodeId);
        } else if (objects.size() > 1) {
            throw new IllegalStateException("More than one 'current' node (id " + nodeId + ") found.");
        } else {
            return objects.get(0);
        }
    }

    @RestHandlerMethod(authorization = GET_SELF)
    @GetMapping("/self/")
    public ResponseEntity<?> getSelfAlt() {
        return getSelf();
    }

    @RestHandlerMethod(authorization = GET_SELF)
    @GetMapping("/self")
    public ResponseEntity<?> getSelf() {
        logger.debug("model rest service for get operation start");
        Task task = initRequest();
        OperationResult result = createSubresult(task, "self");
        ResponseEntity<?> response;

        try {
            String loggedInUserOid = SecurityUtil.getPrincipalOidIfAuthenticated();
            PrismObject<UserType> user = model.getObject(UserType.class, loggedInUserOid, null, task, result);
            response = createResponse(HttpStatus.OK, user, result, true);
            result.recordSuccessIfUnknown();
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(logger, e);
            response = status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PostMapping("/{type}/")
    public <T extends ObjectType> ResponseEntity<?> addObjectAlt(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        return addObject(type, options, object);
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PostMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> addObject(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("addObject");

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        if (object.getCompileTimeClass() == null || !object.getCompileTimeClass().equals(clazz)) {
            String simpleName = object.getCompileTimeClass() != null ? object.getCompileTimeClass().getSimpleName() : null;
            result.recordFatalError("Request to add object of type " + simpleName + " to the collection of " + type);
            finishRequest(task, result);
            return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

        String oid;
        ResponseEntity<?> response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, result);
            logger.debug("returned oid: {}", oid);

            if (oid != null) {
                response = createResponseWithLocation(
                        clazz.isAssignableFrom(TaskType.class) ? HttpStatus.ACCEPTED : HttpStatus.CREATED,
                        uriGetObject(type, oid),
                        result);
            } else {
                // OID might be null e.g. if the object creation is a subject of workflow approval
                response = createResponse(HttpStatus.ACCEPTED, result);
            }
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    public @NotNull URI uriGetObject(@PathVariable("type") String type, String oid) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(controllerBasePath() + GET_OBJECT_PATH)
                .build(type, oid);
    }

    @RestHandlerMethod(authorization = GET_OBJECTS)
    @GetMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> searchObjectsByType(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjectsByType");

        //noinspection unchecked
        Class<T> clazz = (Class<T>) ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {

            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

            List<PrismObject<T>> objects = modelService.searchObjects(clazz, null, searchOptions, task, result);
            ObjectListType listType = new ObjectListType();
            for (PrismObject<T> object : objects) {
                listType.getObject().add(object.asObjectable());
            }

            response = createResponse(HttpStatus.OK, listType, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = ADD_OBJECT)
    @PutMapping("/{type}/{id}")
    public <T extends ObjectType> ResponseEntity<?> addObject(
            @PathVariable("type") String type,
            // TODO is it OK that this is not used or at least asserted?
            @SuppressWarnings("unused") @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("addObject");

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        Class<T> objectClazz = Objects.requireNonNull(object.getCompileTimeClass());
        if (!clazz.equals(objectClazz)) {
            finishRequest(task, result);
            result.recordFatalError(
                    "Request to add object of type %s to the collection of %s".formatted(objectClazz.getSimpleName(), type));
            return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
        if (modelExecuteOptions == null) {
            modelExecuteOptions = ModelExecuteOptions.create();
        }
        // TODO: Do we want to overwrite in any case? Because of PUT?
        //  This was original logic... and then there's that ignored ID.
        modelExecuteOptions.overwrite();

        String oid;
        ResponseEntity<?> response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, result);
            logger.debug("returned oid : {}", oid);

            response = createResponseWithLocation(
                    clazz.isAssignableFrom(TaskType.class) ? HttpStatus.ACCEPTED : HttpStatus.CREATED,
                    uriGetObject(type, oid),
                    result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }
        result.computeStatus();

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = DELETE_OBJECT)
    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<?> deleteObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options) {

        logger.debug("model rest service for delete operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("deleteObject");

        ResponseEntity<?> response;
        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        if (ObjectType.class.equals(clazz)) {
            result.recordFatalError("Type object (path /objects/) does not support deletion, use concrete type.");
            response = createResponse(HttpStatus.METHOD_NOT_ALLOWED, result);
        } else {
            try {
                if (clazz.isAssignableFrom(TaskType.class)) {
                    taskService.suspendAndDeleteTask(id, WAIT_FOR_TASK_STOP, true, task, result);
                    result.computeStatus();
                    finishRequest(task, result);
                    if (result.isSuccess()) {
                        return ResponseEntity.noContent().build();
                    }
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(result.getMessage());
                }

                ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

                model.deleteObject(clazz, id, modelExecuteOptions, task, result);
                response = createResponse(HttpStatus.NO_CONTENT, result);
            } catch (Exception ex) {
                response = handleException(result, ex);
            }
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PostMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPost(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {
        return modifyObjectPatch(type, oid, options, modificationType);
    }

    @RestHandlerMethod(authorization = MODIFY_OBJECT)
    @PatchMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPatch(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {

        logger.debug("model rest service for modify operation start");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("modifyObjectPatch");
        ResponseEntity<?> response;
        if (ObjectType.class.equals(clazz)) {
            result.recordFatalError("Type 'object' (path /objects/) does not support modifications, use concrete type.");
            response = createResponse(HttpStatus.METHOD_NOT_ALLOWED, result);
        } else {
            try {
                ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
                Collection<? extends ItemDelta<?, ?>> modifications = DeltaConvertor.toModifications(modificationType, clazz);
                model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, result);
                response = createResponse(HttpStatus.NO_CONTENT, result);
            } catch (Exception ex) {
                result.recordFatalError("Could not modify object. " + ex.getMessage(), ex);
                response = handleException(result, ex);
            }
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = NOTIFY_CHANGE)
    @PostMapping("/notifyChange")
    public ResponseEntity<?> notifyChange(
            @RequestBody ResourceObjectShadowChangeDescriptionType changeDescription) {
        logger.debug("model rest service for notify change operation start");
        Validate.notNull(changeDescription, "Chnage description must not be null");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("notifyChange");

        ResponseEntity<?> response;
        try {
            modelService.notifyChange(changeDescription, task, result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = FIND_SHADOW_OWNER)
    @GetMapping("/shadows/{oid}/owner")
    public ResponseEntity<?> findShadowOwner(
            @PathVariable("oid") String shadowOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("findShadowOwner");

        ResponseEntity<?> response;
        try {
            PrismObject<? extends FocusType> focus = modelService.searchShadowOwner(shadowOid, null, task, result);
            response = createResponse(HttpStatus.OK, focus, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = IMPORT_SHADOW)
    @PostMapping("/shadows/{oid}/import")
    public ResponseEntity<?> importShadow(
            @PathVariable("oid") String shadowOid) {
        logger.debug("model rest service for import shadow from resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("importShadow");

        ResponseEntity<?> response;
        try {
            modelService.importFromResource(shadowOid, task, result);

            response = createResponse(HttpStatus.OK, result, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = SEARCH_OBJECTS)
    @PostMapping("/{type}/search")
    public ResponseEntity<?> searchObjects(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
            @RequestParam(value = "returnTotalCount", required = false) Boolean returnTotalCount,
            @RequestBody QueryType queryType) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);

            ObjectListType listType = new ObjectListType();
            for (PrismObject<? extends ObjectType> o : objects) {
                if (exclude != null) {
                    removeExcludes(o, exclude);
                }
                listType.getObject().add(o.asObjectable());
            }

            HttpHeaders headers = null;
            if (Boolean.TRUE.equals(returnTotalCount)) {
                ObjectQuery countQuery = query.clone();
                countQuery.setPaging(null);
                int totalCount = modelService.countObjects(clazz, countQuery, searchOptions, task, result);
                headers = addHeader("X-Total-Count", String.valueOf(totalCount), headers);
            }

            response = createResponse(HttpStatus.OK, listType, result, true, headers);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    private HttpHeaders addHeader(String headerName, String headerValue, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(headerName, headerValue);
        return headers;
    }

    private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude)
            throws SchemaException {
        final List<String> metadataPaths = new ArrayList<>();
        final List<String> dataPaths = new ArrayList<>();
        for (final String pathToExclude : exclude) {
            if (pathToExclude.endsWith(METADATA_SUFFIX)) {
                metadataPaths.add(pathToExclude.substring(0, pathToExclude.lastIndexOf(METADATA_SUFFIX)));
            } else {
                dataPaths.add(pathToExclude);
            }
        }

        object.getValue().removePaths(ItemPathCollectionsUtil.pathListFromStrings(dataPaths, prismContext));
        object.getValue().removeMetadataFromPaths(ItemPathCollectionsUtil.pathListFromStrings(metadataPaths,
                prismContext));
    }

    @RestHandlerMethod(authorization = IMPORT_FROM_RESOURCE)
    @PostMapping("/resources/{resourceOid}/import/{objectClass}")
    public ResponseEntity<?> importFromResource(
            @PathVariable("resourceOid") String resourceOid,
            @PathVariable("objectClass") String objectClass) {
        logger.debug("model rest service for import from resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("importFromResource");

        QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
        ResponseEntity<?> response;
        try {
            modelService.importFromResource(resourceOid, objClass, task, result);
            response = createResponseWithLocation(
                    HttpStatus.SEE_OTHER,
                    uriGetObject(ObjectTypes.TASK.getRestType(), task.getOid()),
                    result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = TEST_RESOURCE)
    @PostMapping("/resources/{resourceOid}/test")
    public ResponseEntity<?> testResource(
            @PathVariable("resourceOid") String resourceOid) {
        logger.debug("model rest service for test resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("testResource");

        ResponseEntity<?> response;
        OperationResult testResult = null;
        try {
            testResult = modelService.testResource(resourceOid, task, result);
            response = createResponse(HttpStatus.OK, testResult, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        if (testResult != null) {
            result.getSubresults().add(testResult);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = SUSPEND_TASK)
    @PostMapping("/tasks/{oid}/suspend")
    public ResponseEntity<?> suspendTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("suspendTask");

        ResponseEntity<?> response;
        try {
            taskService.suspendTask(taskOid, WAIT_FOR_TASK_STOP, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RESUME_TASK)
    @PostMapping("/tasks/{oid}/resume")
    public ResponseEntity<?> resumeTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("resumeTask");

        ResponseEntity<?> response;
        try {
            taskService.resumeTask(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.ACCEPTED, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RUN_TASK)
    @PostMapping("tasks/{oid}/run")
    public ResponseEntity<?> scheduleTaskNow(
            @PathVariable("oid") String taskOid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("scheduleTaskNow");

        ResponseEntity<?> response;
        try {
            taskService.scheduleTaskNow(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = EXECUTE_SCRIPT)
    @PostMapping("/rpc/executeScript")
    public ResponseEntity<?> executeScript(
            @RequestParam(value = "asynchronous", required = false) Boolean asynchronous,
            @RequestBody ExecuteScriptType command) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeScript");

        ResponseEntity<?> response;
        try {
            if (Boolean.TRUE.equals(asynchronous)) {
                var taskOid = modelInteraction.submitScriptingExpression(command, task, result);
                response = createResponseWithLocation(
                        HttpStatus.CREATED,
                        uriGetObject(ObjectTypes.TASK.getRestType(), taskOid),
                        result);
            } else {
                BulkActionExecutionResult executionResult = bulkActionsService.executeBulkAction(
                        // detached because of REST origin
                        ExecuteScriptConfigItem.of(command, ConfigurationItemOrigin.rest()),
                        VariablesMap.emptyMap(),
                        BulkActionExecutionOptions.create(),
                        task,
                        result);
                ExecuteScriptResponseType responseData = new ExecuteScriptResponseType()
                        .result(result.createOperationResultType())
                        .output(new ExecuteScriptOutputType()
                                .consoleOutput(executionResult.getConsoleOutput())
                                .dataOutput(PipelineData.prepareXmlData(executionResult.getDataOutput(), command.getOptions())));
                response = createResponse(HttpStatus.OK, responseData, result);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Couldn't execute script.", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = COMPARE_OBJECT)
    @PostMapping("/rpc/compare")
    public <T extends ObjectType> ResponseEntity<?> compare(
            @RequestParam(value = "readOptions", required = false) List<String> restReadOptions,
            @RequestParam(value = "compareOptions", required = false) List<String> restCompareOptions,
            @RequestParam(value = "ignoreItems", required = false) List<String> restIgnoreItems,
            @RequestBody PrismObject<T> clientObject) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("compare");

        ResponseEntity<?> response;
        try {
            List<ItemPath> ignoreItemPaths = ItemPathCollectionsUtil.pathListFromStrings(restIgnoreItems, prismContext);
            final GetOperationOptions getOpOptions = GetOperationOptions.fromRestOptions(restReadOptions, DefinitionProcessingOption.ONLY_IF_EXISTS);
            Collection<SelectorOptions<GetOperationOptions>> readOptions =
                    getOpOptions != null ? SelectorOptions.createCollection(getOpOptions) : null;
            ModelCompareOptions compareOptions = ModelCompareOptions.fromRestOptions(restCompareOptions);
            CompareResultType compareResult = modelService.compareObject(clientObject, readOptions, compareOptions, ignoreItemPaths, task, result);

            response = createResponse(HttpStatus.OK, compareResult, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_LOG_SIZE)
    @GetMapping(value = "/log/size",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLogFileSize() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLogFileSize");

        ResponseEntity<?> response;
        try {
            long size = modelDiagnosticService.getLogFileSize(task, result);
            response = createResponse(HttpStatus.OK, String.valueOf(size), result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_LOG)
    @GetMapping(value = "/log",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLog(
            @RequestParam(value = "fromPosition", required = false) Long fromPosition,
            @RequestParam(value = "maxSize", required = false) Long maxSize) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLog");

        ResponseEntity<?> response;
        try {
            LogFileContentType content = modelDiagnosticService.getLogFileContent(fromPosition, maxSize, task, result);

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            builder.header("ReturnedDataPosition", String.valueOf(content.getAt()));
            builder.header("ReturnedDataComplete", String.valueOf(content.isComplete()));
            builder.header("CurrentLogFileSize", String.valueOf(content.getLogFileSize()));
            response = builder.body(content.getContent());
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get log file content: fromPosition={}, maxSize={}", ex, fromPosition, maxSize);
            response = handleExceptionNoLog(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RESET_CREDENTIAL)
    @PostMapping("/users/{oid}/credential")
    public ResponseEntity<?> executeCredentialReset(
            @PathVariable("oid") String oid,
            @RequestBody ExecuteCredentialResetRequestType executeCredentialResetRequest) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeCredentialReset");

        ResponseEntity<?> response;
        try {
            PrismObject<UserType> user = modelService.getObject(UserType.class, oid, null, task, result);

            ExecuteCredentialResetResponseType executeCredentialResetResponse = modelInteraction.executeCredentialsReset(user, executeCredentialResetRequest, task, result);
            response = createResponse(HttpStatus.OK, executeCredentialResetResponse, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;

    }

    @RestHandlerMethod(authorization = GET_THREADS)
    @GetMapping(value = "/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getThreadsDump() {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getThreadsDump(task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get threads dump", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_TASKS_THREADS)
    @GetMapping(value = "/tasks/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getRunningTasksThreadsDump() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getRunningTasksThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getRunningTasksThreadsDump(task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get running tasks threads dump", ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = GET_TASK_THREADS)
    @GetMapping(value = "/tasks/{oid}/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getTaskThreadsDump(
            @PathVariable("oid") String oid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getTaskThreadsDump");

        ResponseEntity<?> response;
        try {
            String dump = taskService.getTaskThreadsDump(oid, task, result);
            response = ResponseEntity.ok(dump);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(logger, "Cannot get task threads dump for task " + oid, ex);
            response = handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = COMPLETE_WORK_ITEM)
    @PostMapping("/cases/{oid}/workItems/{id}/complete")
    public ResponseEntity<?> completeWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId,
            @RequestBody AbstractWorkItemOutputType output
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("completeWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.completeWorkItem(id, output, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = DELEGATE_WORK_ITEM)
    @PostMapping("/cases/{oid}/workItems/{id}/delegate")
    public ResponseEntity<?> delegateWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId,
            @RequestBody WorkItemDelegationRequestType delegationRequest
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("delegateWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.delegateWorkItem(id, delegationRequest, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = CLAIM_WORK_ITEM)
    @PostMapping("/cases/{oid}/workItems/{id}/claim")
    public ResponseEntity<?> claimWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("claimWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.claimWorkItem(id, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = RELEASE_WORK_ITEM)
    @PostMapping("/cases/{oid}/workItems/{id}/release")
    public ResponseEntity<?> releaseWorkItem(
            @PathVariable("oid") String caseOid,
            @PathVariable("id") Long workItemId
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("releaseWorkItem");

        ResponseEntity<?> response;
        try {
            WorkItemId id = WorkItemId.of(caseOid, workItemId);
            caseService.releaseWorkItem(id, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @RestHandlerMethod(authorization = CANCEL_CASE)
    @PostMapping("/cases/{oid}/cancel")
    public ResponseEntity<?> cancelCase(
            @PathVariable("oid") String caseOid
    ) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("cancelCase");

        ResponseEntity<?> response;
        try {
            caseService.cancelCase(caseOid, task, result);

            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }
}
