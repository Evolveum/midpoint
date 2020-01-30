/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.common.rest.Converter;
import com.evolveum.midpoint.common.rest.ConverterInterface;
import com.evolveum.midpoint.model.impl.rest.PATCH;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptOutputType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * @author katkav
 * @author semancik
 */
@Service
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
public class ModelRestService {

    public static final String CLASS_DOT = ModelRestService.class.getName() + ".";

    public static final String OPERATION_REST_SERVICE = CLASS_DOT + "restService";
    public static final String OPERATION_GET = CLASS_DOT + "get";
    public static final String OPERATION_SELF = CLASS_DOT + "self";
    public static final String OPERATION_ADD_OBJECT = CLASS_DOT + "addObject";
    public static final String OPERATION_DELETE_OBJECT = CLASS_DOT + "deleteObject";
    public static final String OPERATION_MODIFY_OBJECT = CLASS_DOT + "modifyObject";
    public static final String OPERATION_NOTIFY_CHANGE = CLASS_DOT + "notifyChange";
    public static final String OPERATION_FIND_SHADOW_OWNER = CLASS_DOT + "findShadowOwner";
    public static final String OPERATION_SEARCH_OBJECTS = CLASS_DOT + "searchObjects";
    public static final String OPERATION_IMPORT_FROM_RESOURCE = CLASS_DOT + "importFromResource";
    public static final String OPERATION_IMPORT_SHADOW_FROM_RESOURCE = CLASS_DOT + "importShadowFromResource";
    public static final String OPERATION_TEST_RESOURCE = CLASS_DOT + "testResource";
    public static final String OPERATION_SUSPEND_TASK = CLASS_DOT + "suspendTask";
    public static final String OPERATION_SUSPEND_AND_DELETE_TASK = CLASS_DOT + "suspendAndDeleteTask";
    public static final String OPERATION_RESUME_TASK = CLASS_DOT + "resumeTask";
    public static final String OPERATION_SCHEDULE_TASK_NOW = CLASS_DOT + "scheduleTaskNow";
    public static final String OPERATION_EXECUTE_SCRIPT = CLASS_DOT + "executeScript";
    public static final String OPERATION_COMPARE = CLASS_DOT + "compare";
    public static final String OPERATION_GET_LOG_FILE_CONTENT = CLASS_DOT + "getLogFileContent";
    public static final String OPERATION_GET_LOG_FILE_SIZE = CLASS_DOT + "getLogFileSize";
    public static final String OPERATION_VALIDATE_VALUE = CLASS_DOT +  "validateValue";
    public static final String OPERATION_VALIDATE_VALUE_RPC = CLASS_DOT +  "validateValueRpc";
    public static final String OPERATION_GENERATE_VALUE = CLASS_DOT +  "generateValue";
    public static final String OPERATION_GENERATE_VALUE_RPC = CLASS_DOT +  "generateValueRpc";
    public static final String OPERATION_EXECUTE_CREDENTIAL_RESET = CLASS_DOT + "executeCredentialReset";
    public static final String OPERATION_EXECUTE_CLUSTER_EVENT = CLASS_DOT + "executeClusterCacheInvalidationEvent";
    public static final String OPERATION_GET_LOCAL_SCHEDULER_INFORMATION = CLASS_DOT + "getLocalSchedulerInformation";
    public static final String OPERATION_STOP_LOCAL_SCHEDULER = CLASS_DOT + "stopScheduler";
    public static final String OPERATION_START_LOCAL_SCHEDULER = CLASS_DOT + "startScheduler";
    public static final String OPERATION_STOP_LOCAL_TASK = CLASS_DOT + "stopLocalTask";
    public static final String OPERATION_GET_THREADS_DUMP = CLASS_DOT + "getThreadsDump";
    public static final String OPERATION_GET_RUNNING_TASKS_THREADS_DUMP = CLASS_DOT + "getRunningTasksThreadsDump";
    public static final String OPERATION_GET_TASK_THREADS_DUMP = CLASS_DOT + "getTaskThreadsDump";

    private static final String CURRENT = "current";
    private static final String VALIDATE = "validate";

    @Autowired private ModelCrudService model;
    @Autowired private ScriptingService scriptingService;
    @Autowired private ModelService modelService;
    @Autowired private ModelDiagnosticService modelDiagnosticService;
    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private PrismContext prismContext;
    @Autowired private SecurityHelper securityHelper;
    @Autowired private ValuePolicyProcessor policyProcessor;
    @Autowired private TaskManager taskManager;
    @Autowired private TaskService taskService;
    @Autowired private Protector protector;
    @Autowired private ResourceValidator resourceValidator;

    @Autowired private CacheDispatcher cacheDispatcher;

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private AuditService auditService;

    private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);

    private static final long WAIT_FOR_TASK_STOP = 2000L;

    public ModelRestService() {
        // nothing to do
    }

    @POST
    @Path("/{type}/{oid}/generate")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response generateValue(@PathParam("type") String type,
            @PathParam("oid") String oid, PolicyItemsDefinitionType policyItemsDefinition,
            @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_GENERATE_VALUE);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);

        Response response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, parentResult);
            response = generateValue(object, policyItemsDefinition, task, parentResult);
        } catch (Exception ex) {
            parentResult.computeStatus();
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;

    }

    @POST
    @Path("/rpc/generate")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response generateValue(PolicyItemsDefinitionType policyItemsDefinition,
            @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_GENERATE_VALUE_RPC);

        Response response = generateValue(null, policyItemsDefinition, task, parentResult);
        finishRequest(task, mc.getHttpServletRequest());

        return response;
    }

    private <O extends ObjectType> Response generateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition, Task task, OperationResult parentResult){
        Response response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
        } else {
            try {
                modelInteraction.generateValue(object, policyItemsDefinition, task, parentResult);
                parentResult.computeStatusIfUnknown();
                if (parentResult.isSuccess()) {
                    response = RestServiceUtil.createResponse(Response.Status.OK, policyItemsDefinition, parentResult, true);
                } else {
                    response = RestServiceUtil.createResponse(Response.Status.BAD_REQUEST, parentResult, parentResult);
                }

            } catch (Exception ex) {
                parentResult.recordFatalError("Failed to generate value, " + ex.getMessage(), ex);
                response = RestServiceUtil.handleException(parentResult, ex);
            }
        }
        return response;
    }

    @POST
    @Path("/{type}/{oid}/validate")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response validateValue(@PathParam("type") String type, @PathParam("oid") String oid, PolicyItemsDefinitionType policyItemsDefinition, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_VALIDATE_VALUE);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Response response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, parentResult);
            response = validateValue(object, mc.getHttpServletRequest(), policyItemsDefinition, task, parentResult);
        } catch (Exception ex) {
            parentResult.computeStatus();
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/rpc/validate")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response validateValue(PolicyItemsDefinitionType policyItemsDefinition, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_VALIDATE_VALUE);

        Response response = validateValue(null, mc.getHttpServletRequest(), policyItemsDefinition, task, parentResult);
        finishRequest(task, mc.getHttpServletRequest());
        return response;

    }

    private <O extends ObjectType> Response validateValue(PrismObject<O> object, HttpServletRequest httpRequest, PolicyItemsDefinitionType policyItemsDefinition, Task task, OperationResult parentResult) {
        Response response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
            finishRequest(task, httpRequest);
            return response;

        }

        if (CollectionUtils.isEmpty(policyItemsDefinition.getPolicyItemDefinition())) {
            response = createBadPolicyItemsDefinitionResponse("No definitions for items", parentResult);
            finishRequest(task, httpRequest);
            return response;
        }


            try {
                modelInteraction.validateValue(object, policyItemsDefinition, task, parentResult);

                parentResult.computeStatusIfUnknown();
                ResponseBuilder responseBuilder;
                if (parentResult.isAcceptable()) {
                    response = RestServiceUtil.createResponse(Response.Status.OK, policyItemsDefinition, parentResult, true);
                } else {
                    responseBuilder = Response.status(Status.CONFLICT).entity(parentResult);
                    response = responseBuilder.build();
                }

            } catch (Exception ex) {
                parentResult.computeStatus();
                response = RestServiceUtil.handleException(parentResult, ex);
            }


        return response;
    }

    private Response createBadPolicyItemsDefinitionResponse(String message, OperationResult parentResult) {
        LOGGER.error(message);
        parentResult.recordFatalError(message);
        return Response.status(Status.BAD_REQUEST).entity(parentResult).build();
    }

    @GET
    @Path("/users/{id}/policy")
    public Response getValuePolicyForUser(@PathParam("id") String oid, @Context MessageContext mc) {
        LOGGER.debug("getValuePolicyForUser start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_GET);

        Response response;
        try {

            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());
            PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, parentResult);

            CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(user, task, parentResult);

            response = RestServiceUtil.createResponse(Response.Status.OK, policy, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());

        LOGGER.debug("getValuePolicyForUser finish");

        return response;
    }

    @GET
    @Path("/{type}/{id}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response getObject(@PathParam("type") String type, @PathParam("id") String id,
            @QueryParam("options") List<String> options,
            @QueryParam("include") List<String> include,
            @QueryParam("exclude") List<String> exclude,
            @QueryParam("resolveNames") List<String> resolveNames,
            @Context MessageContext mc){
        LOGGER.debug("model rest service for get operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_GET);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Collection<SelectorOptions<GetOperationOptions>> getOptions = GetOperationOptions.fromRestOptions(options, include,
                exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
        Response response;

        try {
            PrismObject<? extends ObjectType> object;
            if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
                String nodeId = taskManager.getNodeId();
                ObjectQuery query = prismContext.queryFor(NodeType.class)
                        .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                        .build();
                 List<PrismObject<NodeType>> objects = model.searchObjects(NodeType.class, query, getOptions, task, parentResult);
                if (objects.isEmpty()) {
                    throw new ObjectNotFoundException("Current node (id " + nodeId + ") couldn't be found.");
                } else if (objects.size() > 1) {
                    throw new IllegalStateException("More than one 'current' node (id " + nodeId + ") found.");
                } else {
                    object = objects.get(0);
                }
            } else {
                object = model.getObject(clazz, id, getOptions, task, parentResult);
            }
            removeExcludes(object, exclude);        // temporary measure until fixed in repo

            response = RestServiceUtil.createResponse(Response.Status.OK, object, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/self")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response getSelf(@Context MessageContext mc){
        LOGGER.debug("model rest service for get operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SELF);

        Response response;

        try {
            UserType loggedInUser = SecurityUtil.getPrincipal().getUser();
            PrismObject<UserType> user = model.getObject(UserType.class, loggedInUser.getOid(), null, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.OK, user, parentResult, true);
            parentResult.recordSuccessIfUnknown();
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            response = RestServiceUtil.handleException(parentResult, e);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }


    @POST
    @Path("/{type}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public <T extends ObjectType> Response addObject(@PathParam("type") String type, PrismObject<T> object,
                                                     @QueryParam("options") List<String> options,
            @Context UriInfo uriInfo, @Context MessageContext mc) {
        LOGGER.debug("model rest service for add operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_ADD_OBJECT);

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        if (!object.getCompileTimeClass().equals(clazz)){
            finishRequest(task, mc.getHttpServletRequest());
            parentResult.recordFatalError("Request to add object of type "
                    + object.getCompileTimeClass().getSimpleName() + " to the collection of " + type);
            return RestServiceUtil.createErrorResponseBuilder(Status.BAD_REQUEST, parentResult).build();
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

        String oid;
        Response response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, parentResult);
            LOGGER.debug("returned oid :  {}", oid );

            if (oid != null) {
                URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
                response = clazz.isAssignableFrom(TaskType.class) ?        // TODO not the other way around?
                        RestServiceUtil.createResponse(Response.Status.ACCEPTED, resourceURI, parentResult) : RestServiceUtil.createResponse(Response.Status.CREATED, resourceURI, parentResult);
            } else {
                // OID might be null e.g. if the object creation is a subject of workflow approval
                response = RestServiceUtil.createResponse(Response.Status.ACCEPTED, parentResult);
            }
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/{type}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public <T extends ObjectType> Response searchObjectsByType(@PathParam("type") String type, @QueryParam("options") List<String> options,
            @QueryParam("include") List<String> include, @QueryParam("exclude") List<String> exclude,
            @QueryParam("resolveNames") List<String> resolveNames,
            @Context UriInfo uriInfo, @Context MessageContext mc) {
        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SEARCH_OBJECTS);

        //noinspection unchecked
        Class<T> clazz = (Class<T>) ObjectTypes.getClassFromRestType(type);
        Response response;
        try {

            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

            List<PrismObject<T>> objects = modelService.searchObjects(clazz, null, searchOptions, task, parentResult);
            ObjectListType listType = new ObjectListType();
            for (PrismObject<T> object : objects) {
                listType.getObject().add(object.asObjectable());
            }

            response = RestServiceUtil.createResponse(Response.Status.OK, listType, parentResult, true);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    // currently unused; but potentially useful in future
    @SuppressWarnings("unused")
    private void validateIfRequested(PrismObject<?> object,
            List<String> options, ResponseBuilder builder, Task task,
            OperationResult parentResult) {
        if (options != null && options.contains(VALIDATE) && object.asObjectable() instanceof ResourceType) {
            ValidationResult validationResult = resourceValidator
                    .validate((PrismObject<ResourceType>) object, Scope.THOROUGH, null, task, parentResult);
            builder.entity(validationResult.toValidationResultType());            // TODO move to parentResult, and return the result!
        }
    }

    @PUT
    @Path("/{type}/{id}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public <T extends ObjectType> Response addObject(@PathParam("type") String type, @PathParam("id") String id,
            PrismObject<T> object, @QueryParam("options") List<String> options, @Context UriInfo uriInfo,
            @Context Request request, @Context MessageContext mc){

        LOGGER.debug("model rest service for add operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_ADD_OBJECT);

        Class<?> clazz = ObjectTypes.getClassFromRestType(type);
        if (!object.getCompileTimeClass().equals(clazz)){
            finishRequest(task, mc.getHttpServletRequest());
            parentResult.recordFatalError("Request to add object of type "
                    + object.getCompileTimeClass().getSimpleName()
                    + " to the collection of " + type);
            return RestServiceUtil.createErrorResponseBuilder(Status.BAD_REQUEST, parentResult).build();
        }

        ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
        if (modelExecuteOptions == null) {
            modelExecuteOptions = ModelExecuteOptions.createOverwrite();
        } else if (!ModelExecuteOptions.isOverwrite(modelExecuteOptions)){
            modelExecuteOptions.setOverwrite(Boolean.TRUE);
        }

        String oid;
        Response response;
        try {
            oid = model.addObject(object, modelExecuteOptions, task, parentResult);
            LOGGER.debug("returned oid : {}", oid);

            URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
            response = clazz.isAssignableFrom(TaskType.class) ?
                    RestServiceUtil.createResponse(Response.Status.ACCEPTED, resourceURI, parentResult) : RestServiceUtil.createResponse(Response.Status.CREATED, resourceURI, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }
        parentResult.computeStatus();

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @DELETE
    @Path("/{type}/{id}")
    public Response deleteObject(@PathParam("type") String type, @PathParam("id") String id,
            @QueryParam("options") List<String> options, @Context MessageContext mc){

        LOGGER.debug("model rest service for delete operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_DELETE_OBJECT);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Response response;
        try {
            if (clazz.isAssignableFrom(TaskType.class)) {
                taskService.suspendAndDeleteTask(id, WAIT_FOR_TASK_STOP, true, task, parentResult);
                parentResult.computeStatus();
                finishRequest(task, mc.getHttpServletRequest());
                if (parentResult.isSuccess()) {
                    return Response.noContent().build();
                }
                return Response.serverError().entity(parentResult.getMessage()).build();
            }

            ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

            model.deleteObject(clazz, id, modelExecuteOptions, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/{type}/{oid}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response modifyObjectPost(@PathParam("type") String type, @PathParam("oid") String oid,
            ObjectModificationType modificationType, @QueryParam("options") List<String> options, @Context MessageContext mc) {
        return modifyObjectPatch(type, oid, modificationType, options, mc);
    }

    @PATCH
    @Path("/{type}/{oid}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response modifyObjectPatch(@PathParam("type") String type, @PathParam("oid") String oid,
            ObjectModificationType modificationType, @QueryParam("options") List<String> options, @Context MessageContext mc) {

        LOGGER.debug("model rest service for modify operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_MODIFY_OBJECT);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Response response;
        try {
            ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
            Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(modificationType, clazz, prismContext);
            model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
        } catch (Exception ex) {
            parentResult.recordFatalError("Could not modify object. " + ex.getMessage(), ex);
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/notifyChange")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription,
            @Context UriInfo uriInfo, @Context MessageContext mc) {
        LOGGER.debug("model rest service for notify change operation start");
        Validate.notNull(changeDescription, "Chnage description must not be null");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_NOTIFY_CHANGE);

        Response response;
        try {
            modelService.notifyChange(changeDescription, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.OK, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/shadows/{oid}/owner")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response findShadowOwner(@PathParam("oid") String shadowOid, @Context MessageContext mc){

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_FIND_SHADOW_OWNER);

        Response response;
        try {
            PrismObject<UserType> user = modelService.findShadowOwner(shadowOid, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.OK, user, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/shadows/{oid}/import")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response importShadow(@PathParam("oid") String shadowOid, @Context MessageContext mc, @Context UriInfo uriInfo) {
        LOGGER.debug("model rest service for import shadow from resource operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_IMPORT_SHADOW_FROM_RESOURCE);

        Response response;
        try {
            modelService.importFromResource(shadowOid, task, parentResult);

            response = RestServiceUtil.createResponse(Response.Status.OK, parentResult, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/{type}/search")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response searchObjects(@PathParam("type") String type, QueryType queryType,
            @QueryParam("options") List<String> options,
            @QueryParam("include") List<String> include,
            @QueryParam("exclude") List<String> exclude,
            @QueryParam("resolveNames") List<String> resolveNames,
            @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SEARCH_OBJECTS);

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Response response;
        try {
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, queryType);
            Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include,
                    exclude, resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);
            List<? extends PrismObject<? extends ObjectType>> objects =
                    model.searchObjects(clazz, query, searchOptions, task, parentResult);

            ObjectListType listType = new ObjectListType();
            for (PrismObject<? extends ObjectType> o : objects) {
                removeExcludes(o, exclude);        // temporary measure until fixed in repo
                listType.getObject().add(o.asObjectable());
            }

            response = RestServiceUtil.createResponse(Response.Status.OK, listType, parentResult, true);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude) throws SchemaException {
        object.getValue().removePaths(ItemPathCollectionsUtil.pathListFromStrings(exclude, prismContext));
    }

    @POST
    @Path("/resources/{resourceOid}/import/{objectClass}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response importFromResource(@PathParam("resourceOid") String resourceOid, @PathParam("objectClass") String objectClass,
            @Context MessageContext mc, @Context UriInfo uriInfo) {
        LOGGER.debug("model rest service for import from resource operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_IMPORT_FROM_RESOURCE);

        QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
        Response response;
        try {
            modelService.importFromResource(resourceOid, objClass, task, parentResult);
            response = RestServiceUtil.createResponse(Response.Status.SEE_OTHER, (uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject")
                    .build(ObjectTypes.TASK.getRestType(), task.getOid())), parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/resources/{resourceOid}/test")
    public Response testResource(@PathParam("resourceOid") String resourceOid, @Context MessageContext mc) {
        LOGGER.debug("model rest service for test resource operation start");

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_TEST_RESOURCE);

        Response response;
        OperationResult testResult = null;
        try {
            testResult = modelService.testResource(resourceOid, task);
            response = RestServiceUtil.createResponse(Response.Status.OK, testResult, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        if (testResult != null) {
            parentResult.getSubresults().add(testResult);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/tasks/{oid}/suspend")
    public Response suspendTask(@PathParam("oid") String taskOid, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SUSPEND_TASK);

        Response response;
        try {
            taskService.suspendTask(taskOid, WAIT_FOR_TASK_STOP, task, parentResult);
            parentResult.computeStatus();
            response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, task, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/tasks/{oid}/resume")
    public Response resumeTask(@PathParam("oid") String taskOid, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_RESUME_TASK);

        Response response;
        try {
            taskService.resumeTask(taskOid, task, parentResult);
            parentResult.computeStatus();
            response = RestServiceUtil.createResponse(Response.Status.ACCEPTED, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }


    @POST
    @Path("tasks/{oid}/run")
    public Response scheduleTaskNow(@PathParam("oid") String taskOid, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SCHEDULE_TASK_NOW);

        Response response;
        try {
            taskService.scheduleTaskNow(taskOid, task, parentResult);
            parentResult.computeStatus();
            response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(parentResult, ex);
        }

        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    public static class ExecuteScriptConverter implements ConverterInterface {
        public ExecuteScriptType convert(@NotNull Object input) {
            if (input instanceof ExecuteScriptType) {
                return (ExecuteScriptType) input;
            } else if (input instanceof ScriptingExpressionType) {
                return ScriptingExpressionEvaluator.createExecuteScriptCommand((ScriptingExpressionType) input);
            } else {
                throw new IllegalArgumentException("Wrong input value: " + input);
            }
        }
    }

    @POST
    @Path("/rpc/executeScript")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response executeScript(@Converter(ExecuteScriptConverter.class) ExecuteScriptType command,
            @QueryParam("asynchronous") Boolean asynchronous, @Context UriInfo uriInfo, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_EXECUTE_SCRIPT);

        Response response;
        try {
            if (Boolean.TRUE.equals(asynchronous)) {
                scriptingService.evaluateExpressionInBackground(command, task, result);
                URI resourceUri = uriInfo.getAbsolutePathBuilder().path(task.getOid()).build(task.getOid());
                response = RestServiceUtil.createResponse(Response.Status.CREATED, resourceUri, result);
            } else {
                ScriptExecutionResult executionResult = scriptingService.evaluateExpression(command, VariablesMap.emptyMap(),
                        false, task, result);
                ExecuteScriptResponseType responseData = new ExecuteScriptResponseType()
                        .result(result.createOperationResultType())
                        .output(new ExecuteScriptOutputType()
                                .consoleOutput(executionResult.getConsoleOutput())
                                .dataOutput(ModelWebService.prepareXmlData(executionResult.getDataOutput(), command.getOptions())));
                response = RestServiceUtil.createResponse(Response.Status.OK, responseData, result);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script.", ex);
            response = RestServiceUtil.handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/rpc/compare")
    @Consumes({"application/xml" })
    public <T extends ObjectType> Response compare(PrismObject<T> clientObject,
            @QueryParam("readOptions") List<String> restReadOptions,
            @QueryParam("compareOptions") List<String> restCompareOptions,
            @QueryParam("ignoreItems") List<String> restIgnoreItems,
            @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_COMPARE);

        Response response;
        try {
            List<ItemPath> ignoreItemPaths = ItemPathCollectionsUtil.pathListFromStrings(restIgnoreItems, prismContext);
            final GetOperationOptions getOpOptions = GetOperationOptions.fromRestOptions(restReadOptions, DefinitionProcessingOption.ONLY_IF_EXISTS);
            Collection<SelectorOptions<GetOperationOptions>> readOptions =
                    getOpOptions != null ? SelectorOptions.createCollection(getOpOptions) : null;
            ModelCompareOptions compareOptions = ModelCompareOptions.fromRestOptions(restCompareOptions);
            CompareResultType compareResult = modelService.compareObject(clientObject, readOptions, compareOptions, ignoreItemPaths, task, result);

            response = RestServiceUtil.createResponse(Response.Status.OK, compareResult, result);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/log/size")
    @Produces({"text/plain"})
    public Response getLogFileSize(@Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_LOG_FILE_SIZE);

        Response response;
        try {
            long size = modelDiagnosticService.getLogFileSize(task, result);

            response = RestServiceUtil.createResponse(Response.Status.OK, String.valueOf(size), result);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/log")
    @Produces({"text/plain"})
    public Response getLog(@QueryParam("fromPosition") Long fromPosition, @QueryParam("maxSize") Long maxSize, @Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_LOG_FILE_CONTENT);

        Response response;
        try {
            LogFileContentType content = modelDiagnosticService.getLogFileContent(fromPosition, maxSize, task, result);

            ResponseBuilder builder = Response.ok();
            builder.entity(content.getContent());
            builder.header("ReturnedDataPosition", content.getAt());
            builder.header("ReturnedDataComplete", content.isComplete());
            builder.header("CurrentLogFileSize", content.getLogFileSize());

            response = builder.build();

        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get log file content: fromPosition={}, maxSize={}", ex, fromPosition, maxSize);
            response = RestServiceUtil.handleExceptionNoLog(result, ex);
        }

        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @POST
    @Path("/users/{oid}/credential")
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, RestServiceUtil.APPLICATION_YAML})
    public Response executeCredentialReset(@PathParam("oid") String oid, ExecuteCredentialResetRequestType executeCredentialResetRequest, @Context MessageContext mc) {
        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_EXECUTE_CREDENTIAL_RESET);

        Response response;
        try {
            PrismObject<UserType> user = modelService.getObject(UserType.class, oid, null, task, result);

            ExecuteCredentialResetResponseType executeCredentialResetResponse = modelInteraction.executeCredentialsReset(user, executeCredentialResetRequest, task, result);
            response = RestServiceUtil.createResponse(Response.Status.OK, executeCredentialResetResponse, result);
        } catch (Exception ex) {
            response = RestServiceUtil.handleException(result, ex);
        }

        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;


    }

    @GET
    @Path("/threads")
    @Produces({"text/plain"})
    public Response getThreadsDump(@Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_THREADS_DUMP);

        Response response;
        try {
            String dump = taskService.getThreadsDump(task, result);
            response = Response.ok(dump).build();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get threads dump", ex);
            response = RestServiceUtil.handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/tasks/threads")
    @Produces({"text/plain"})
    public Response getRunningTasksThreadsDump(@Context MessageContext mc) {

        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_RUNNING_TASKS_THREADS_DUMP);

        Response response;
        try {
            String dump = taskService.getRunningTasksThreadsDump(task, result);
            response = Response.ok(dump).build();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get running tasks threads dump", ex);
            response = RestServiceUtil.handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    @GET
    @Path("/tasks/{oid}/threads")
    @Produces({"text/plain"})
    public Response getTaskThreadsDump(@PathParam("oid") String oid, @Context MessageContext mc) {
        Task task = initRequest(mc);
        OperationResult result = task.getResult().createSubresult(OPERATION_GET_TASK_THREADS_DUMP);

        Response response;
        try {
            String dump = taskService.getTaskThreadsDump(oid, task, result);
            response = Response.ok(dump).build();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get task threads dump for task " + oid, ex);
            response = RestServiceUtil.handleExceptionNoLog(result, ex);
        }
        result.computeStatus();
        finishRequest(task, mc.getHttpServletRequest());
        return response;
    }

    private void finishRequest(Task task, HttpServletRequest request) {
        if (isExperimentalEnabled()) {
            auditEvent(request);
            SecurityContextHolder.getContext().setAuthentication(null);
        } else {
            RestServiceUtil.finishRequest(task, securityHelper);
        }
    }

    private Task initRequest(MessageContext mc) {
        Task task;
        if (isExperimentalEnabled()) {
            task = RestServiceUtil.initRequest(taskManager);
        } else {
            task = RestServiceUtil.initRequest(mc);
        }
        return task;
    }

    private boolean isExperimentalEnabled() {
        boolean isExperimentalEnabled = false;
        try {
            isExperimentalEnabled = SystemConfigurationTypeUtil.isExperimentalCodeEnabled(
                    systemObjectCache.getSystemConfiguration(new OperationResult("Load System Config")).asObjectable());
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load system configuration", e);
        }
        return isExperimentalEnabled;
    }

    public void auditEvent(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        String name = null;
        if (principal instanceof MidPointPrincipal) {
            name = ((MidPointPrincipal) principal).getUsername();
        } else if (principal != null){
            return;
        }
        PrismObject<UserType> user = principal!= null ? ((MidPointPrincipal)principal).getUser().asPrismObject() : null;

        Task task = taskManager.createTaskInstance();
        task.setOwner(user);
        task.setChannel(SchemaConstants.CHANNEL_REST_URI);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
        record.setInitiator(user);
        record.setParameter(name);

        record.setChannel(SchemaConstants.CHANNEL_REST_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);

        // probably not needed, as audit service would take care of it; but it doesn't hurt so let's keep it here
        record.setHostIdentifier(request.getLocalName());
        record.setRemoteHostAddress(request.getLocalAddr());
        record.setNodeIdentifier(taskManager.getNodeId());
        record.setSessionIdentifier(request.getRequestedSessionId());

        auditService.audit(record, task);
    }
}
