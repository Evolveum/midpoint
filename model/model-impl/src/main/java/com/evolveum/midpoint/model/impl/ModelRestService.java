/*
 * Copyright (c) 2013-2015 Evolveum
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

import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.rest.PATCH;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Service
@Produces({"application/xml", "application/json"})
public class ModelRestService {
	
	@Autowired(required= true)
	private ModelCrudService model;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required = true)
	private AuditService auditService;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);
	
	public static final long WAIT_FOR_TASK_STOP = 2000L;
	private static final String OPTIONS = "options";
	
	public ModelRestService() {
		// nothing to do
	}
	
	@GET
	@Path("/{type}/{id}")
//	@Produces({"application/xml"})
	public <T extends ObjectType> Response getObject(@PathParam("type") String type, @PathParam("id") String id,
			@Context MessageContext mc){
		LOGGER.info("model rest service for get operation start");
		
		Task task = taskManager.createTaskInstance("get");
		OperationResult parentResult = task.getResult();
		initRequest(task, mc);
		
		Class<T> clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		
		try {
			PrismObject<T> object = model.getObject(clazz, id, null, task, parentResult);
			ResponseBuilder builder = Response.ok();
			builder.entity(object);
			response = builder.build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			response =  Response.status(Status.CONFLICT).type(MediaType.TEXT_HTML).entity(e.getMessage()).build();
		} catch (CommunicationException e) {
			response =  Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response =  Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response =  Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}


	@POST
	@Path("/{type}")
//	@Produces({"text/html", "application/xml"})
	@Consumes({"application/xml", "application/json"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, PrismObject<T> object, @QueryParam("options") List<String> options, 
			@Context UriInfo uriInfo, @Context MessageContext mc) {
		LOGGER.info("model rest service for add operation start");
		
		Task task = taskManager.createTaskInstance("add");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
			auditLogout(task);
			return Response.status(Status.BAD_REQUEST).entity(
					"Request to add object of type "
							+ object.getCompileTimeClass().getSimpleName()
							+ " to the collection of " + type).type(MediaType.TEXT_HTML).build();
		}
		
		
		ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
		
		String oid;
		Response response;
		try {
			oid = model.addObject(object, modelExecuteOptions, task, parentResult);
			LOGGER.info("returned oid :  {}", oid );
			
			URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
			ResponseBuilder builder = clazz.isAssignableFrom(TaskType.class) ? Response.accepted().location(resourceURI) : Response.created(resourceURI);
			
			response = builder.build();
		} catch (ObjectAlreadyExistsException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}

	@PUT
	@Path("/{type}/{id}")
//	@Produces({"text/html", "application/xml"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, @PathParam("id") String id, 
			PrismObject<T> object, @QueryParam("options") List<String> options, @Context UriInfo uriInfo, 
			@Context Request request, @Context MessageContext mc){
	
		LOGGER.info("model rest service for add operation start");

		Task task = taskManager.createTaskInstance("add");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
			auditLogout(task);
			return Response.status(Status.BAD_REQUEST).entity(
					"Request to add object of type "
							+ object.getCompileTimeClass().getSimpleName()
							+ " to the collection of " + type).type(MediaType.TEXT_HTML).build();
		}
		
		ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
		if (modelExecuteOptions == null || !ModelExecuteOptions.isOverwrite(modelExecuteOptions)){
			modelExecuteOptions = ModelExecuteOptions.createOverwrite();
		}
		
		String oid;
		Response response;
		try {
			oid = model.addObject(object, modelExecuteOptions, task, parentResult);
			LOGGER.info("returned oid :  {}", oid );
			
			URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
			ResponseBuilder builder = clazz.isAssignableFrom(TaskType.class) ? Response.accepted().location(resourceURI) : Response.created(resourceURI);
			
			response = builder.build();
		} catch (ObjectAlreadyExistsException e) {
			response = Response.serverError().entity(e.getMessage()).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}
	
	@DELETE
	@Path("/{type}/{id}")
//	@Produces({"text/html", "application/xml"})
	public Response deleteObject(@PathParam("type") String type, @PathParam("id") String id, 
			@QueryParam("options") List<String> options, @Context MessageContext mc){

		LOGGER.info("model rest service for delete operation start");
		
		Task task = taskManager.createTaskInstance("delete");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {
			if (clazz.isAssignableFrom(TaskType.class)){
				model.suspendAndDeleteTasks(MiscUtil.createCollection(id), WAIT_FOR_TASK_STOP, true, parentResult);
				parentResult.computeStatus();
				auditLogout(task);
				if (parentResult.isSuccess()){
					return Response.noContent().build();
				}
				
				return Response.serverError().entity(parentResult.getMessage()).build();
				
			} 
			
			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
			
			model.deleteObject(clazz, id, modelExecuteOptions, task, parentResult);
			response = Response.noContent().build();
			
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConsistencyViolationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}
	
	
	@PATCH
	@Path("/{type}/{oid}")
//	@Produces({"text/html", "application/xml"})
	public <T extends ObjectType> Response modifyObject(@PathParam("type") String type, @PathParam("oid") String oid, 
			ObjectModificationType modificationType, @QueryParam("options") List<String> options, @Context MessageContext mc) {
		
		LOGGER.info("model rest service for modify operation start");
		
		Task task = taskManager.createTaskInstance("modifyObject");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {
			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(modificationType, clazz, prismContext);
			model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, parentResult);
			response = Response.noContent().build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectAlreadyExistsException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}
	
	@POST
	@Path("/notifyChange")
	public Response notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription, 
			@Context UriInfo uriInfo, @Context MessageContext mc) {
		LOGGER.info("model rest service for notify change operation start");

		Task task = taskManager.createTaskInstance("notifyChange");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Response response;
		try {
			model.notifyChange(changeDescription, parentResult, task);
			response = Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject").build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (ObjectAlreadyExistsException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}


	
	@GET
	@Path("/shadows/{oid}/owner")
//	@Produces({"text/html", "application/xml"})
	public Response findShadowOwner(@PathParam("oid") String shadowOid, @Context MessageContext mc){
		
		LOGGER.info("model rest service for find shadow owner operation start");

		Task task = taskManager.createTaskInstance("find shadow owner");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Response response;
		try {
			PrismObject<UserType> user = model.findShadowOwner(shadowOid, task, parentResult);
			response = Response.ok().entity(user).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}

	@POST
	@Path("/{type}/search")
//	@Produces({"text/html", "application/xml"})
	public Response searchObjects(@PathParam("type") String type, QueryType queryType, @Context MessageContext mc){
	
		LOGGER.info("model rest service for find shadow owner operation start");

		Task task = taskManager.createTaskInstance("searchObjects");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();

		Class clazz = ObjectTypes.getClassFromRestType(type);
		Response response;
		try {	
			ObjectQuery query = QueryJaxbConvertor.createObjectQuery(clazz, queryType, prismContext);
		
			List<PrismObject<? extends ShadowType>> objects = model.searchObjects(clazz, query, null, task, parentResult);
		
			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ObjectType> o : objects) {
				listType.getObject().add(o.asObjectable());
			}
		
			response = Response.ok().entity(listType).build();
		
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}

	@POST
	@Path("/resources/{resourceOid}/import/{objectClass}")
//	@Produces({"text/html", "application/xml"})
	public Response importFromResource(@PathParam("resourceOid") String resourceOid, @PathParam("objectClass") String objectClass, 
			@Context MessageContext mc, @Context UriInfo uriInfo) {	
		LOGGER.info("model rest service for import from resource operation start");

		Task task = taskManager.createTaskInstance("importFromResource");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();

		QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
		Response response;
		try {
			model.importFromResource(resourceOid, objClass, task, parentResult);
			response = Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject").build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			response = Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			response = Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			response = Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		parentResult.computeStatus();
		auditLogout(task);
		return response;
	}

	@POST
	@Path("/resources/{resourceOid}/test")
//	@Produces({"text/html", "application/xml"})
	public Response testResource(@PathParam("resourceOid") String resourceOid, @Context MessageContext mc) {
		LOGGER.info("model rest service for test resource operation start");

		Task task = taskManager.createTaskInstance("testResource");
		initRequest(task, mc);

		Response response;
		try {
			OperationResult result = model.testResource(resourceOid, task);
			response = Response.ok(result).build();
		} catch (ObjectNotFoundException e) {
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
	
		auditLogout(task);
		return response;
	}
	
	@POST
	@Path("/tasks/{oid}/suspend")
    public Response suspendTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {
		
		Task task = taskManager.createTaskInstance("suspendTasks");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Response response;
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        boolean suspended = model.suspendTasks(taskOids, WAIT_FOR_TASK_STOP, parentResult);
        
        parentResult.computeStatus();
        
        if (parentResult.isSuccess()){
        	response = Response.noContent().build();
        } else {
        	response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        }
        
		auditLogout(task);
		return response;
    }

//	@DELETE
//	@Path("tasks/{oid}/suspend")
    public Response suspendAndDeleteTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {
    	Task task = taskManager.createTaskInstance("suspendAndDeleteTasks");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();
		
		Response response;
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        model.suspendAndDeleteTasks(taskOids, WAIT_FOR_TASK_STOP, true, parentResult);
        
        parentResult.computeStatus();
        if (parentResult.isSuccess()){
        	response = Response.accepted().build();
        } else {
        	response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        }
        
		auditLogout(task);
		return response;
    }
	
	
	@POST
	@Path("/tasks/{oid}/resume")
    public Response resumeTasks(@PathParam("oid") String taskOid, @Context MessageContext mc) {
		Task task = taskManager.createTaskInstance("resumeTasks");
		initRequest(task, mc);
		OperationResult parentResult = task.getResult();

		Response response;
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        model.resumeTasks(taskOids, parentResult);
        
        parentResult.computeStatus();
        
        if (parentResult.isSuccess()){
        	response = Response.accepted().build();
        } else {
        	response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        }
        
		auditLogout(task);
		return response;
    }


	@POST
	@Path("tasks/{oid}/run")
    public Response scheduleTasksNow(@PathParam("oid") String taskOid) {
		OperationResult parentResult = new OperationResult("suspend task.");
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
    
        model.scheduleTasksNow(taskOids, parentResult);
        
        parentResult.computeStatus();
        
        if (parentResult.isSuccess()){
        	return Response.accepted().build();
        } 
        
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        
        
    }
	
//    @GET
//    @Path("tasks/{oid}")
    public Response getTaskByIdentifier(@PathParam("oid") String identifier) throws SchemaException, ObjectNotFoundException {
    	OperationResult parentResult = new OperationResult("suspend task.");
        PrismObject<TaskType> task = model.getTaskByIdentifier(identifier, null, parentResult);
        
        return Response.ok(task).build();
    }

    
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) {
        return model.deactivateServiceThreads(timeToWait, parentResult);
    }

    public void reactivateServiceThreads(OperationResult parentResult) {
        model.reactivateServiceThreads(parentResult);
    }

    public boolean getServiceThreadsActivationState() {
        return model.getServiceThreadsActivationState();
    }

    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        model.stopSchedulers(nodeIdentifiers, parentResult);
    }

    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long waitTime, OperationResult parentResult) {
        return model.stopSchedulersAndTasks(nodeIdentifiers, waitTime, parentResult);
    }

    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        model.startSchedulers(nodeIdentifiers, parentResult);
    }

    public void synchronizeTasks(OperationResult parentResult) {
    	model.synchronizeTasks(parentResult);
    }

    public List<String> getAllTaskCategories() {
        return model.getAllTaskCategories();
    }

    public String getHandlerUriForCategory(String category) {
        return model.getHandlerUriForCategory(category);
    }
	
    
    private ModelExecuteOptions getOptions(UriInfo uriInfo){
    	List<String> options = uriInfo.getQueryParameters().get(OPTIONS);
		return ModelExecuteOptions.fromRestOptions(options);
    }
    
	private void initRequest(Task task, MessageContext mc) {
		UserType user = (UserType) mc.get("authenticatedUser");
		task.setOwner(user.asPrismObject());
		auditLoginSuccess(task);
	}
    
    private void auditLoginSuccess(Task task) {
		AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        PrismObject<UserType> owner = task.getOwner();
        if (owner != null) {
	        record.setInitiator(owner);
	        PolyStringType name = owner.asObjectable().getName();
	        if (name != null) {
	        	record.setParameter(name.getOrig());
	        }
        }

        record.setChannel(SchemaConstants.CHANNEL_REST_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setSessionIdentifier(task.getTaskIdentifier());
        
        record.setOutcome(OperationResultStatus.SUCCESS);
		auditService.audit(record, task);
	}
    
    private void auditLogout(Task task) {
		AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
		PrismObject<UserType> owner = task.getOwner();
        if (owner != null) {
	        record.setInitiator(owner);
	        PolyStringType name = owner.asObjectable().getName();
	        if (name != null) {
	        	record.setParameter(name.getOrig());
	        }
        }

        record.setChannel(SchemaConstants.CHANNEL_REST_URI);
        record.setTimestamp(System.currentTimeMillis());
        record.setSessionIdentifier(task.getTaskIdentifier());
        
        record.setOutcome(OperationResultStatus.SUCCESS);

        auditService.audit(record, task);
	}
}
