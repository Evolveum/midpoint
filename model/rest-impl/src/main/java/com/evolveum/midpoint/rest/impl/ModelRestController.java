/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.rest.impl;

import static com.evolveum.midpoint.security.api.RestMethod.*;

import static org.springframework.http.ResponseEntity.status;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;

import com.evolveum.midpoint.security.api.RestMethod;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * All these methods callable via REST should be protected by the {@link #authorize(RestMethod, Task, OperationResult)} call.
 */
@RestController
@RequestMapping({ "/ws/rest", "/rest/model", "/api/model" })
public class ModelRestController extends AbstractRestController {

    public static final String GET_OBJECT_PATH = "/{type}/{id}";

    private static final String CURRENT = "current";
    private static final long WAIT_FOR_TASK_STOP = 2000L;

    @Autowired private ModelCrudService model;
    @Autowired private ModelDiagnosticService modelDiagnosticService;
    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private ModelService modelService;
    @Autowired private BulkActionsService bulkActionsService;
    @Autowired private TaskService taskService;

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
            authorize(GENERATE_VALUE, task, result);
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = generateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }
        finishRequest(task, result);
        return response;
    }

    @PostMapping("/rpc/generate")
    public ResponseEntity<?> generateValueRpc(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "generateValueRpc");

        ResponseEntity<?> response;
        try {
            authorize(RPC_GENERATE_VALUE, task, result);
            response = generateValue(null, policyItemsDefinition, task, result);
        } catch (Exception e) {
            result.computeStatus();
            response = handleException(result, e);
        }
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

    @PostMapping("/{type}/{oid}/validate")
    public ResponseEntity<?> validateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "validateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            authorize(VALIDATE_VALUE, task, result);
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, result);
            response = validateValue(object, policyItemsDefinition, task, result);
        } catch (Exception ex) {
            result.computeStatus();
            response = handleException(result, ex);
        }
        finishRequest(task, result);
        return response;
    }

    @PostMapping("/rpc/validate")
    public ResponseEntity<?> validateValue(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "validateValue");

        ResponseEntity<?> response;
        try {
            authorize(RPC_VALIDATE_VALUE, task, result);
            response = validateValue(null, policyItemsDefinition, task, result);
        } catch (Exception e) {
            result.computeStatus();
            response = handleException(result, e);
        }
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

    @GetMapping("/users/{id}/policy")
    public ResponseEntity<?> getValuePolicyForUser(
            @PathVariable("id") String oid) {
        logger.debug("getValuePolicyForUser start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getValuePolicyForUser");

        ResponseEntity<?> response;
        try {
            authorize(GET_VALUE_POLICY, task, result);
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
            authorize(GET_OBJECT, task, result);
            PrismObject<? extends ObjectType> object;
            if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
                object = getCurrentNodeObject(getOptions, task, result);
            } else {
                object = model.getObject(clazz, id, getOptions, task, result);
            }
            removeExcludes(object, exclude); // temporary measure until fixed in repo

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

    @GetMapping("/self/")
    public ResponseEntity<?> getSelfAlt() {
        return getSelf();
    }

    @GetMapping("/self")
    public ResponseEntity<?> getSelf() {
        logger.debug("model rest service for get operation start");
        Task task = initRequest();
        OperationResult result = createSubresult(task, "self");
        ResponseEntity<?> response;

        try {
            authorize(GET_SELF, task, result);
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

    @PostMapping("/{type}/")
    public <T extends ObjectType> ResponseEntity<?> addObjectAlt(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        return addObject(type, options, object);
    }

    @PostMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> addObject(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody @NotNull PrismObject<T> object) {
        logger.debug("model rest service for add operation start");

        Task task = initRequest();
        OperationResult result = createSubresult(task, "addObject");

        String oid;
        ResponseEntity<?> response;
        try {
            authorize(POST_OBJECT, task, result);

            Class<?> clazz = ObjectTypes.getClassFromRestType(type);
            if (object.getCompileTimeClass() == null || !object.getCompileTimeClass().equals(clazz)) {
                String simpleName = object.getCompileTimeClass() != null ? object.getCompileTimeClass().getSimpleName() : null;
                result.recordFatalError("Request to add object of type " + simpleName + " to the collection of " + type);
                finishRequest(task, result);
                return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
            }

            ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

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

    @NotNull
    public URI uriGetObject(@PathVariable("type") String type, String oid) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(controllerBasePath() + GET_OBJECT_PATH)
                .build(type, oid);
    }

    @GetMapping("/{type}")
    public <T extends ObjectType> ResponseEntity<?> searchObjectsByType(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {
        Task task = initRequest();
        OperationResult result = createSubresult(task, "searchObjectsByType");

        //noinspection unchecked
        Class<T> clazz = (Class<T>) ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            authorize(GET_OBJECTS, task, result);

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
            authorize(PUT_OBJECT, task, result);
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
                authorize(DELETE_OBJECT, task, result);
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

    @PostMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPost(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {
        return modifyObjectPatch(type, oid, options, modificationType);
    }

    @PatchMapping("/{type}/{oid}")
    public ResponseEntity<?> modifyObjectPatch(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestBody ObjectModificationType modificationType) {

        logger.debug("model rest service for modify operation start");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);

        Task task = initRequest();
        OperationResult result = createSubresult(task, "modifyObjectPatch");
        ResponseEntity<?> response;
        if (ObjectType.class.equals(clazz)) {
            result.recordFatalError("Type 'object' (path /objects/) does not support modifications, use concrete type.");
            response = createResponse(HttpStatus.METHOD_NOT_ALLOWED, result);
        } else {
            try {
                authorize(MODIFY_OBJECT, task, result);
                ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
                Collection<? extends ItemDelta<?, ?>> modifications = DeltaConvertor.toModifications(modificationType, clazz, prismContext);
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

    @PostMapping("/notifyChange")
    public ResponseEntity<?> notifyChange(
            @RequestBody ResourceObjectShadowChangeDescriptionType changeDescription) {
        logger.debug("model rest service for notify change operation start");
        Validate.notNull(changeDescription, "Chnage description must not be null");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("notifyChange");

        ResponseEntity<?> response;
        try {
            authorize(NOTIFY_CHANGE, task, result);
            modelService.notifyChange(changeDescription, task, result);
            response = createResponse(HttpStatus.OK, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @GetMapping("/shadows/{oid}/owner")
    public ResponseEntity<?> findShadowOwner(
            @PathVariable("oid") String shadowOid) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "findShadowOwner");

        ResponseEntity<?> response;
        try {
            authorize(FIND_SHADOW_OWNER, task, result);
            PrismObject<? extends FocusType> focus = modelService.searchShadowOwner(shadowOid, null, task, result);
            response = createResponse(HttpStatus.OK, focus, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @PostMapping("/shadows/{oid}/import")
    public ResponseEntity<?> importShadow(
            @PathVariable("oid") String shadowOid) {
        logger.debug("model rest service for import shadow from resource operation start");

        Task task = initRequest();
        OperationResult result = createSubresult(task, "importShadow");

        ResponseEntity<?> response;
        try {
            authorize(IMPORT_SHADOW, task, result);
            modelService.importFromResource(shadowOid, task, result);

            response = createResponse(HttpStatus.OK, result, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @PostMapping("/{type}/search")
    public ResponseEntity<?> searchObjects(
            @PathVariable("type") String type,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
            @RequestBody QueryType queryType) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("searchObjects");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            authorize(SEARCH_OBJECTS, task, result);
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, result);

            ObjectListType listType = new ObjectListType();
            for (PrismObject<? extends ObjectType> o : objects) {
                removeExcludes(o, exclude);        // temporary measure until fixed in repo
                listType.getObject().add(o.asObjectable());
            }

            response = createResponse(HttpStatus.OK, listType, result, true);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude)
            throws SchemaException {
        object.getValue().removePaths(
                ItemPathCollectionsUtil.pathListFromStrings(exclude, prismContext));
    }

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
            authorize(IMPORT_FROM_RESOURCE, task, result);
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

    @PostMapping("/resources/{resourceOid}/test")
    public ResponseEntity<?> testResource(
            @PathVariable("resourceOid") String resourceOid) {
        logger.debug("model rest service for test resource operation start");

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("testResource");

        ResponseEntity<?> response;
        OperationResult testResult = null;
        try {
            authorize(TEST_RESOURCE, task, result);
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

    @PostMapping("/tasks/{oid}/suspend")
    public ResponseEntity<?> suspendTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("suspendTask");

        ResponseEntity<?> response;
        try {
            authorize(SUSPEND_TASK, task, result);
            taskService.suspendTask(taskOid, WAIT_FOR_TASK_STOP, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, task, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @PostMapping("/tasks/{oid}/resume")
    public ResponseEntity<?> resumeTask(
            @PathVariable("oid") String taskOid) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("resumeTask");

        ResponseEntity<?> response;
        try {
            authorize(RESUME_TASK, task, result);
            taskService.resumeTask(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.ACCEPTED, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @PostMapping("tasks/{oid}/run")
    public ResponseEntity<?> scheduleTaskNow(
            @PathVariable("oid") String taskOid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("scheduleTaskNow");

        ResponseEntity<?> response;
        try {
            authorize(RUN_TASK, task, result);
            taskService.scheduleTaskNow(taskOid, task, result);
            result.computeStatus();
            response = createResponse(HttpStatus.NO_CONTENT, result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        finishRequest(task, result);
        return response;
    }

    @PostMapping("/rpc/executeScript")
    public ResponseEntity<?> executeScript(
            @RequestParam(value = "asynchronous", required = false) Boolean asynchronous,
            @RequestBody ExecuteScriptType command) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeScript");

        ResponseEntity<?> response;
        try {
            authorize(EXECUTE_SCRIPT, task, result);
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

    @PostMapping("/rpc/compare")
//    @Consumes({ "application/xml" }) TODO do we need to limit it to XML?
    public <T extends ObjectType> ResponseEntity<?> compare(
            @RequestParam(value = "readOptions", required = false) List<String> restReadOptions,
            @RequestParam(value = "compareOptions", required = false) List<String> restCompareOptions,
            @RequestParam(value = "ignoreItems", required = false) List<String> restIgnoreItems,
            @RequestBody PrismObject<T> clientObject) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("compare");

        ResponseEntity<?> response;
        try {
            authorize(COMPARE_OBJECT, task, result);
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

    @GetMapping(value = "/log/size",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLogFileSize() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLogFileSize");

        ResponseEntity<?> response;
        try {
            authorize(GET_LOG_SIZE, task, result);
            long size = modelDiagnosticService.getLogFileSize(task, result);
            response = createResponse(HttpStatus.OK, String.valueOf(size), result);
        } catch (Exception ex) {
            response = handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, result);
        return response;
    }

    @GetMapping(value = "/log",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getLog(
            @RequestParam(value = "fromPosition", required = false) Long fromPosition,
            @RequestParam(value = "maxSize", required = false) Long maxSize) {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getLog");

        ResponseEntity<?> response;
        try {
            authorize(GET_LOG, task, result);
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

    @PostMapping("/users/{oid}/credential")
    public ResponseEntity<?> executeCredentialReset(
            @PathVariable("oid") String oid,
            @RequestBody ExecuteCredentialResetRequestType executeCredentialResetRequest) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("executeCredentialReset");

        ResponseEntity<?> response;
        try {
            authorize(RESET_CREDENTIAL, task, result);
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

    @GetMapping(value = "/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getThreadsDump() {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getThreadsDump");

        ResponseEntity<?> response;
        try {
            authorize(GET_THREADS, task, result);
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

    @GetMapping(value = "/tasks/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getRunningTasksThreadsDump() {

        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getRunningTasksThreadsDump");

        ResponseEntity<?> response;
        try {
            authorize(GET_TASKS_THREADS, task, result);
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

    @GetMapping(value = "/tasks/{oid}/threads",
            produces = { MediaType.TEXT_PLAIN_VALUE, MimeTypeUtils.ALL_VALUE })
    public ResponseEntity<?> getTaskThreadsDump(
            @PathVariable("oid") String oid) {
        Task task = initRequest();
        OperationResult result = task.getResult().createSubresult("getTaskThreadsDump");

        ResponseEntity<?> response;
        try {
            authorize(GET_TASK_THREADS, task, result);
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
}
