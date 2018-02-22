/*
 * Copyright (c) 2013-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.rest.Convertor;
import com.evolveum.midpoint.model.impl.rest.ConvertorInterface;
import com.evolveum.midpoint.model.impl.rest.PATCH;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
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
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author katkav
 * @author semancik
 */
@Service
@Produces({"application/xml", "application/json", "application/yaml"})
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
	public static final String OPERATION_TEST_RESOURCE = CLASS_DOT + "testResource";
	public static final String OPERATION_SUSPEND_TASKS = CLASS_DOT + "suspendTasks";
	public static final String OPERATION_SUSPEND_AND_DELETE_TASKS = CLASS_DOT + "suspendAndDeleteTasks";
	public static final String OPERATION_RESUME_TASKS = CLASS_DOT + "resumeTasks";
	public static final String OPERATION_SCHEDULE_TASKS_NOW = CLASS_DOT + "scheduleTasksNow";
	public static final String OPERATION_EXECUTE_SCRIPT = CLASS_DOT + "executeScript";
	public static final String OPERATION_COMPARE = CLASS_DOT + "compare";
	public static final String OPERATION_GET_LOG_FILE_CONTENT = CLASS_DOT + "getLogFileContent";
	public static final String OPERATION_GET_LOG_FILE_SIZE = CLASS_DOT + "getLogFileSize";
	public static final String OPERATION_VALIDATE_VALUE = CLASS_DOT +  "validateValue";
	public static final String OPERATION_VALIDATE_VALUE_RPC = CLASS_DOT +  "validateValueRpc";
	public static final String OPERATION_GENERATE_VALUE = CLASS_DOT +  "generateValue";
	public static final String OPERATION_GENERATE_VALUE_RPC = CLASS_DOT +  "generateValueRpc";

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
	@Autowired private Protector protector;
	@Autowired private ResourceValidator resourceValidator;

	private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);

	private static final long WAIT_FOR_TASK_STOP = 2000L;

	public ModelRestService() {
		// nothing to do
	}

	@POST
	@Path("/{type}/{oid}/generate")
	@Consumes({"application/xml", "application/json", "application/yaml"})
	@Produces({"application/xml", "application/json", "application/yaml"})
	public <O extends ObjectType> Response generateValue(@PathParam("type") String type,
			@PathParam("oid") String oid, PolicyItemsDefinitionType policyItemsDefinition,
			@Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_GENERATE_VALUE);

		Class<O> clazz = ObjectTypes.getClassFromRestType(type);

		Response response = null;
		try {
			PrismObject<O> object = model.getObject(clazz, oid, null, task, parentResult);
			response = generateValue(object, policyItemsDefinition, task, parentResult);
		} catch (Exception ex) {
			parentResult.computeStatus();
			response = RestServiceUtil.handleException(parentResult, ex);

		}

		finishRequest(task);
		return response;
	
	}
	
	@POST
	@Path("/rpc/generate")
	@Consumes({"application/xml", "application/json", "application/yaml"})
	@Produces({"application/xml", "application/json", "application/yaml"})
	public <O extends ObjectType> Response generateValue(PolicyItemsDefinitionType policyItemsDefinition,
			@Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_GENERATE_VALUE_RPC);

		Response response = generateValue(null, policyItemsDefinition, task, parentResult);
		finishRequest(task);
		
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
	@Consumes({"application/xml", "application/json", "application/yaml"})
	@Produces({"application/xml", "application/json", "application/yaml"})
	public <O extends ObjectType> Response validateValue(@PathParam("type") String type, @PathParam("oid") String oid, PolicyItemsDefinitionType policyItemsDefinition, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_VALIDATE_VALUE);

		Class<O> clazz = ObjectTypes.getClassFromRestType(type);
		Response response = null;
		try {
			PrismObject<O> object = model.getObject(clazz, oid, null, task, parentResult);
			response = validateValue(object, policyItemsDefinition, task, parentResult);
		} catch (Exception ex) {
			parentResult.computeStatus();
			response = RestServiceUtil.handleException(parentResult, ex);
		}
		
		finishRequest(task);
		return response;
	}
	
	@POST
	@Path("/rpc/validate")
	@Consumes({"application/xml", "application/json", "application/yaml"})
	@Produces({"application/xml", "application/json", "application/yaml"})
	public <O extends ObjectType> Response validateValue(PolicyItemsDefinitionType policyItemsDefinition, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_VALIDATE_VALUE);

		Response response = validateValue(null, policyItemsDefinition, task, parentResult);
		finishRequest(task);
		return response;
		
	}
	
	private <O extends ObjectType> Response validateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition, Task task, OperationResult parentResult) {
		Response response;
		if (policyItemsDefinition == null) {
			response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
			finishRequest(task);
			return response;
			
		}
		
		if (CollectionUtils.isEmpty(policyItemsDefinition.getPolicyItemDefinition())) {
			response = createBadPolicyItemsDefinitionResponse("No definitions for items", parentResult);
			finishRequest(task);
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

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_GET);

		Response response;
		try {

			Collection<SelectorOptions<GetOperationOptions>> options =
					SelectorOptions.createCollection(GetOperationOptions.createRaw());
			PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, parentResult);

			CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(user, task, parentResult);

			response = RestServiceUtil.createResponse(Response.Status.OK, policy, parentResult);
//			ResponseBuilder builder = Response.ok();
//			builder.entity(policy);
//			response = builder.build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);

		LOGGER.debug("getValuePolicyForUser finish");

		return response;
	}

	@GET
	@Path("/{type}/{id}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response getObject(@PathParam("type") String type, @PathParam("id") String id,
			@QueryParam("options") List<String> options,
			@QueryParam("include") List<String> include,
			@QueryParam("exclude") List<String> exclude,
			@Context MessageContext mc){
		LOGGER.debug("model rest service for get operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_GET);

		Class<T> clazz = ObjectTypes.getClassFromRestType(type);
		Collection<SelectorOptions<GetOperationOptions>> getOptions = GetOperationOptions.fromRestOptions(options, include, exclude, DefinitionProcessingOption.ONLY_IF_EXISTS);
		Response response;

		try {
			PrismObject<T> object;
			if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
				String nodeId = taskManager.getNodeId();
				ObjectQuery query = QueryBuilder.queryFor(NodeType.class, prismContext)
						.item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
						.build();
			 	List<PrismObject<NodeType>> objects = model.searchObjects(NodeType.class, query, getOptions, task, parentResult);
				if (objects.isEmpty()) {
					throw new ObjectNotFoundException("Current node (id " + nodeId + ") couldn't be found.");
				} else if (objects.size() > 1) {
					throw new IllegalStateException("More than one 'current' node (id " + nodeId + ") found.");
				} else {
					object = (PrismObject<T>) objects.get(0);
				}
			} else {
				object = model.getObject(clazz, id, getOptions, task, parentResult);
			}
			removeExcludes(object, exclude);		// temporary measure until fixed in repo

			response = RestServiceUtil.createResponse(Response.Status.OK, object, parentResult);
//			ResponseBuilder builder = Response.ok();
//			builder.entity(object);
//			response = builder.build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@GET
	@Path("/self")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response getSelf(@Context MessageContext mc){
		LOGGER.debug("model rest service for get operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SELF);

		Response response;

		try {
			UserType loggedInUser = SecurityUtil.getPrincipal().getUser();
			PrismObject<UserType> user = model.getObject(UserType.class, loggedInUser.getOid(), null, task, parentResult);
			response = RestServiceUtil.createResponse(Response.Status.OK, user, parentResult, true);
//			ResponseBuilder builder = Response.ok();
//			builder.entity(user);
//			response = builder.build();
			parentResult.recordSuccessIfUnknown();
		} catch (SecurityViolationException | ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			response = RestServiceUtil.handleException(parentResult, e);
		}

		finishRequest(task);
		return response;
	}


	@POST
	@Path("/{type}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, PrismObject<T> object,
													 @QueryParam("options") List<String> options,
			@Context UriInfo uriInfo, @Context MessageContext mc) {
		LOGGER.debug("model rest service for add operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_ADD_OBJECT);

		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
			finishRequest(task);
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

			ResponseBuilder builder;

			if (oid != null) {
				URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
				response = clazz.isAssignableFrom(TaskType.class) ?		// TODO not the other way around?
						RestServiceUtil.createResponse(Response.Status.ACCEPTED, resourceURI, parentResult) : RestServiceUtil.createResponse(Response.Status.CREATED, resourceURI, parentResult);
//				builder = clazz.isAssignableFrom(TaskType.class) ?		// TODO not the other way around?
//						Response.accepted().location(resourceURI) : Response.created(resourceURI);
			} else {
				// OID might be null e.g. if the object creation is a subject of workflow approval
//				builder = Response.accepted();			// TODO is this ok ?
				response = RestServiceUtil.createResponse(Response.Status.ACCEPTED, parentResult);
			}
			// (not used currently)
			//validateIfRequested(object, options, builder, task, parentResult);
//			response = builder.build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@GET
	@Path("/{type}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response searchObjectsByType(@PathParam("type") String type, @QueryParam("options") List<String> options,
			@Context UriInfo uriInfo, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SEARCH_OBJECTS);

		Class<T> clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {

			Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, null, null, DefinitionProcessingOption.ONLY_IF_EXISTS);
			
			
			List<T> objects = new ArrayList<>();
			ResultHandler<T> handler = new ResultHandler<T>() {
				
				@Override
				public boolean handle(PrismObject<T> object, OperationResult parentResult) {
					return objects.add(object.asObjectable());
				}
			};
			
			SearchResultMetadata searchMetadata = modelService.searchObjectsIterative(clazz, null, handler, searchOptions, task, parentResult);

			ObjectListType listType = new ObjectListType();
			listType.getObject().addAll(objects);

			response = RestServiceUtil.createResponse(Response.Status.OK, listType, parentResult, true);
//			response = Response.ok().entity(listType).build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	private ObjectType convert(Class clazz, PrismObject<? extends ObjectType> o, OperationResultType result) {
		ObjectType objType = null;
		try {
			objType = (ObjectType) prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz).instantiate().asObjectable();
			objType.setOid(o.getOid());
			objType.setName(o.asObjectable().getName());
			return objType;
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			return objType;
		}


	}

	// currently unused; but potentially useful in future
	private <T extends ObjectType> void validateIfRequested(PrismObject<?> object,
			List<String> options, ResponseBuilder builder, Task task,
			OperationResult parentResult) {
		if (options != null && options.contains(VALIDATE) && object.asObjectable() instanceof ResourceType) {
			ValidationResult validationResult = resourceValidator
					.validate((PrismObject<ResourceType>) object, Scope.THOROUGH, null, task, parentResult);
			builder.entity(validationResult.toValidationResultType());			// TODO move to parentResult, and return the result!
		}
	}

	@PUT
	@Path("/{type}/{id}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, @PathParam("id") String id,
			PrismObject<T> object, @QueryParam("options") List<String> options, @Context UriInfo uriInfo,
			@Context Request request, @Context MessageContext mc){

		LOGGER.debug("model rest service for add operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_ADD_OBJECT);

		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
			finishRequest(task);
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
			// (not used currently)
			//validateIfRequested(object, options, builder, task, parentResult);
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}
		parentResult.computeStatus();
//		Response response = RestServiceUtil.createResultHeaders(builder, parentResult).build();

		finishRequest(task);
		return response;
	}

	@DELETE
	@Path("/{type}/{id}")
//	@Produces({"text/html", "application/xml"})
	public Response deleteObject(@PathParam("type") String type, @PathParam("id") String id,
			@QueryParam("options") List<String> options, @Context MessageContext mc){

		LOGGER.debug("model rest service for delete operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_DELETE_OBJECT);

		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {
			if (clazz.isAssignableFrom(TaskType.class)){
				model.suspendAndDeleteTasks(MiscUtil.createCollection(id), WAIT_FOR_TASK_STOP, true, task, parentResult);
				parentResult.computeStatus();
				finishRequest(task);
				if (parentResult.isSuccess()){
					return Response.noContent().build();
				}

				return Response.serverError().entity(parentResult.getMessage()).build();

			}

			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);

			model.deleteObject(clazz, id, modelExecuteOptions, task, parentResult);
//			response = Response.noContent().build();
			response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/{type}/{oid}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response modifyObjectPost(@PathParam("type") String type, @PathParam("oid") String oid,
			ObjectModificationType modificationType, @QueryParam("options") List<String> options, @Context MessageContext mc) {
		return modifyObjectPatch(type, oid, modificationType, options, mc);
	}

	@PATCH
	@Path("/{type}/{oid}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public <T extends ObjectType> Response modifyObjectPatch(@PathParam("type") String type, @PathParam("oid") String oid,
			ObjectModificationType modificationType, @QueryParam("options") List<String> options, @Context MessageContext mc) {

		LOGGER.debug("model rest service for modify operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_MODIFY_OBJECT);

		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {
			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(modificationType, clazz, prismContext);
			model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, parentResult);
//			response = Response.noContent().build();
			response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
		} catch (Exception ex) {
			parentResult.recordFatalError("Could not modify object. " + ex.getMessage(), ex);
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/notifyChange")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public Response notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription,
			@Context UriInfo uriInfo, @Context MessageContext mc) {
		LOGGER.debug("model rest service for notify change operation start");
		Validate.notNull(changeDescription, "Chnage description must not be null");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_NOTIFY_CHANGE);

		Response response;
		try {
			model.notifyChange(changeDescription, parentResult, task);
			response = RestServiceUtil.createResponse(Response.Status.OK, parentResult);
//			return Response.ok().build();
//			String oldShadowOid = changeDescription.getOldShadowOid();
//			if (oldShadowOid != null){
//				URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oldShadowOid).build(oldShadowOid);
//				return Response.accepted().location(resourceURI).build();
//			} else {
//				changeDescription.get
//			}
//			response = Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject").build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}



	@GET
	@Path("/shadows/{oid}/owner")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public Response findShadowOwner(@PathParam("oid") String shadowOid, @Context MessageContext mc){

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_FIND_SHADOW_OWNER);

		Response response;
		try {
			PrismObject<UserType> user = model.findShadowOwner(shadowOid, task, parentResult);
//			response = Response.ok().entity(user).build();
			response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, user, parentResult);
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/{type}/search")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public Response searchObjects(@PathParam("type") String type, QueryType queryType,
			@QueryParam("options") List<String> options,
			@QueryParam("include") List<String> include,
			@QueryParam("exclude") List<String> exclude,
			@Context MessageContext mc){

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SEARCH_OBJECTS);

		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {
			ObjectQuery query = QueryJaxbConvertor.createObjectQuery(clazz, queryType, prismContext);
			Collection<SelectorOptions<GetOperationOptions>> searchOptions = GetOperationOptions.fromRestOptions(options, include, exclude, DefinitionProcessingOption.ONLY_IF_EXISTS);
			List<PrismObject<? extends ObjectType>> objects = model.searchObjects(clazz, query, searchOptions, task, parentResult);

			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ObjectType> o : objects) {
				removeExcludes(o, exclude);		// temporary measure until fixed in repo
				listType.getObject().add(o.asObjectable());
			}

//			response = Response.ok().entity(listType).build();
			response = RestServiceUtil.createResponse(Response.Status.OK, listType, parentResult, true);
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude) {
		object.getValue().removePaths(ItemPath.fromStringList(exclude));
	}

	@POST
	@Path("/resources/{resourceOid}/import/{objectClass}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public Response importFromResource(@PathParam("resourceOid") String resourceOid, @PathParam("objectClass") String objectClass,
			@Context MessageContext mc, @Context UriInfo uriInfo) {
		LOGGER.debug("model rest service for import from resource operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_IMPORT_FROM_RESOURCE);

		QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
		Response response;
		try {
			model.importFromResource(resourceOid, objClass, task, parentResult);
			response = RestServiceUtil.createResponse(Response.Status.SEE_OTHER, (uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject")
					.build(ObjectTypes.TASK.getRestType(), task.getOid())), parentResult);
//			response = Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject")
//			.build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		parentResult.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/resources/{resourceOid}/test")
//	@Produces({"text/html", "application/xml"})
	public Response testResource(@PathParam("resourceOid") String resourceOid, @Context MessageContext mc) {
		LOGGER.debug("model rest service for test resource operation start");

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_TEST_RESOURCE);

		Response response;
		OperationResult testResult = null;
		try {
			testResult = model.testResource(resourceOid, task);
			response = RestServiceUtil.createResponse(Response.Status.OK, testResult, parentResult);
//			response = Response.ok(testResult).build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		if (testResult != null) {
			parentResult.getSubresults().add(testResult);
		}

		finishRequest(task);
		return response;
	}

	@POST
	@Path("/tasks/{oid}/suspend")
    public Response suspendTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SUSPEND_TASKS);

		Response response;
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
		try {
			model.suspendTasks(taskOids, WAIT_FOR_TASK_STOP, task, parentResult);
			parentResult.computeStatus();
			response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, task, parentResult);
//			if (parentResult.isSuccess()){
//				response = Response.noContent().build();
//			} else {
//				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
//			}
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		finishRequest(task);
		return response;
    }

//	@DELETE
//	@Path("tasks/{oid}/suspend")
//    public Response suspendAndDeleteTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {
//
//    	Task task = RestServiceUtil.initRequest(mc);
//		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SUSPEND_AND_DELETE_TASKS);
//
//		Response response;
//		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
//		try {
//			model.suspendAndDeleteTasks(taskOids, WAIT_FOR_TASK_STOP, true, parentResult);
//
//			parentResult.computeStatus();
//			if (parentResult.isSuccess()) {
//				response = Response.accepted().build();
//			} else {
//				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
//			}
//		} catch (Exception ex) {
//			response = RestServiceUtil.handleException(parentResult, ex);
//		}
//
//		finishRequest(task);
//		return response;
//    }

	@POST
	@Path("/tasks/{oid}/resume")
    public Response resumeTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_RESUME_TASKS);

		Response response;
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
		try {
			model.resumeTasks(taskOids, task, parentResult);

			parentResult.computeStatus();
			response = RestServiceUtil.createResponse(Response.Status.ACCEPTED, parentResult);
//			if (parentResult.isSuccess()) {
//				response = Response.accepted().build();
//			} else {
//				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
//			}
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		finishRequest(task);
		return response;
    }


	@POST
	@Path("tasks/{oid}/run")
    public Response scheduleTasksNow(@PathParam("oid") String taskOid, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult parentResult = task.getResult().createSubresult(OPERATION_SCHEDULE_TASKS_NOW);

		Collection<String> taskOids = MiscUtil.createCollection(taskOid);

		Response response;
		try {
			model.scheduleTasksNow(taskOids, task, parentResult);

			parentResult.computeStatus();

			response = RestServiceUtil.createResponse(Response.Status.NO_CONTENT, parentResult);
//			if (parentResult.isSuccess()) {
//				response = Response.accepted().build();
//			} else {
//				response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
//			}
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(parentResult, ex);
		}

		finishRequest(task);
		return response;
    }

	public static class ExecuteScriptConvertor implements ConvertorInterface {
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
	//	@Produces({"text/html", "application/xml"})
	@Consumes({"application/xml" })
	public Response executeScript(@Convertor(ExecuteScriptConvertor.class) ExecuteScriptType command,
			@QueryParam("asynchronous") Boolean asynchronous, @Context UriInfo uriInfo, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = task.getResult().createSubresult(OPERATION_EXECUTE_SCRIPT);

		Response response;
		try {
			if (Boolean.TRUE.equals(asynchronous)) {
				scriptingService.evaluateExpressionInBackground(command, task, result);
				URI resourceUri = uriInfo.getAbsolutePathBuilder().path(task.getOid()).build(task.getOid());
				response = RestServiceUtil.createResponse(Response.Status.CREATED, resourceUri, result);
			} else {
				ScriptExecutionResult executionResult = scriptingService.evaluateExpression(command, Collections.emptyMap(), task, result);
				ExecuteScriptResponseType responseData = new ExecuteScriptResponseType()
						.result(result.createOperationResultType())
						.output(new ExecuteScriptOutputType()
								.consoleOutput(executionResult.getConsoleOutput())
								.dataOutput(ModelWebService.prepareXmlData(executionResult.getDataOutput(), command.getOptions())));
				response = RestServiceUtil.createResponse(Response.Status.OK, responseData, result);
			}
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(result, ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script.", ex);
		}
		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/rpc/compare")
	//	@Produces({"text/html", "application/xml"})
	@Consumes({"application/xml" })
	public <T extends ObjectType> Response compare(PrismObject<T> clientObject,
			@QueryParam("readOptions") List<String> restReadOptions,
			@QueryParam("compareOptions") List<String> restCompareOptions,
			@QueryParam("ignoreItems") List<String> restIgnoreItems,
			@Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = task.getResult().createSubresult(OPERATION_COMPARE);

		Response response;
		try {
			ResponseBuilder builder;
			List<ItemPath> ignoreItemPaths = ItemPath.fromStringList(restIgnoreItems);
			final GetOperationOptions getOpOptions = GetOperationOptions.fromRestOptions(restReadOptions, DefinitionProcessingOption.ONLY_IF_EXISTS);
			Collection<SelectorOptions<GetOperationOptions>> readOptions =
					getOpOptions != null ? SelectorOptions.createCollection(getOpOptions) : null;
			ModelCompareOptions compareOptions = ModelCompareOptions.fromRestOptions(restCompareOptions);
			CompareResultType compareResult = modelService.compareObject(clientObject, readOptions, compareOptions, ignoreItemPaths, task, result);

			response = RestServiceUtil.createResponse(Response.Status.OK, compareResult, result);
//			builder = Response.ok();
//			builder.entity(compareResult);
//
//			response = builder.build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(result, ex);
		}

		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@GET
	@Path("/log/size")
	@Produces({"text/plain"})
	public Response getLogFileSize(@Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = task.getResult().createSubresult(OPERATION_GET_LOG_FILE_SIZE);

		Response response;
		try {
			long size = modelDiagnosticService.getLogFileSize(task, result);

			response = RestServiceUtil.createResponse(Response.Status.OK, String.valueOf(size), result);
//			ResponseBuilder builder = Response.ok();
//			builder.entity(String.valueOf(size));
//			response = builder.build();
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(result, ex);
		}

		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@GET
	@Path("/log")
	@Produces({"text/plain"})
	public Response getLog(@QueryParam("fromPosition") Long fromPosition, @QueryParam("maxSize") Long maxSize, @Context MessageContext mc) {

		Task task = RestServiceUtil.initRequest(mc);
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
			response = RestServiceUtil.handleException(result, ex);
		}

		result.computeStatus();
		finishRequest(task);
		return response;
	}

	@POST
	@Path("/users/{oid}/credential")
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, "application/yaml"})
	public Response executeCredentialReset(@PathParam("oid") String oid, ExecuteCredentialResetRequestType executeCredentialResetRequest, @Context MessageContext mc) {
		Task task = RestServiceUtil.initRequest(mc);
		OperationResult result = task.getResult().createSubresult(OPERATION_GET_LOG_FILE_CONTENT);

		Response response;
		try {
			PrismObject<UserType> user = modelService.getObject(UserType.class, oid, null, task, result);
			
			ExecuteCredentialResetResponseType executeCredentialResetResponse = modelInteraction.executeCredentialsReset(user, executeCredentialResetRequest, task, result);
			response = RestServiceUtil.createResponse(Response.Status.OK, executeCredentialResetResponse, result);
		} catch (Exception ex) {
			response = RestServiceUtil.handleException(result, ex);
		}

		result.computeStatus();
		finishRequest(task);
		return response;

	}

	//    @GET
//    @Path("tasks/{oid}")
//    public Response getTaskByIdentifier(@PathParam("oid") String identifier) throws SchemaException, ObjectNotFoundException {
//    	OperationResult parentResult = new OperationResult("getTaskByIdentifier");
//        PrismObject<TaskType> task = model.getTaskByIdentifier(identifier, null, parentResult);
//
//        return Response.ok(task).build();
//    }
//
//
//    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) {
//        return model.deactivateServiceThreads(timeToWait, parentResult);
//    }
//
//    public void reactivateServiceThreads(OperationResult parentResult) {
//        model.reactivateServiceThreads(parentResult);
//    }
//
//    public boolean getServiceThreadsActivationState() {
//        return model.getServiceThreadsActivationState();
//    }
//
//    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
//        model.stopSchedulers(nodeIdentifiers, parentResult);
//    }
//
//    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult) {
//        return model.stopSchedulersAndTasks(nodeIdentifiers, waitTime, parentResult);
//    }
//
//    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
//        model.startSchedulers(nodeIdentifiers, parentResult);
//    }
//
//    public void synchronizeTasks(OperationResult parentResult) {
//    	model.synchronizeTasks(parentResult);
//    }

	private void finishRequest(Task task) {
		RestServiceUtil.finishRequest(task, securityHelper);
	}

}
