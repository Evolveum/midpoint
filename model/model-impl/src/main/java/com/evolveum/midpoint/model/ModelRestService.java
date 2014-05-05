package com.evolveum.midpoint.model;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.jws.WebMethod;
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

import antlr.Utils;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.rest.PATCH;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

@Service
@Produces({"application/xml", "application/json"})
public class ModelRestService {
	
	@Autowired(required= true)
	private ModelCrudService model;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);
	
	public static final long WAIT_FOR_TASK_STOP = 2000L;
	private static final String OPTIONS = "options";
	
	public ModelRestService(){
		
	}
	
	
	
	@GET
	@Path("/{type}/{id}")
//	@Produces({"application/xml"})
	public <T extends ObjectType> Response getObject(@PathParam("type") String type, @PathParam("id") String id){
		LOGGER.info("model rest service for get operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("get");
//		try{
			
			Class<T> clazz = ObjectTypes.getClassFromRestType(type);
			
		
		try {
			PrismObject<T> object = model.getObject(clazz, id, null, task, parentResult);
			ResponseBuilder builder = Response.ok();
			builder.entity(object);
			return builder.build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).type(MediaType.TEXT_HTML).entity(e.getMessage()).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
	}
	
	
	
	@POST
	@Path("/{type}")
//	@Produces({"text/html", "application/xml"})
	@Consumes({"application/xml", "application/json"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, PrismObject<T> object, @QueryParam("options") List<String> options, @Context UriInfo uriInfo){
		LOGGER.info("model rest service for add operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("add");
		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
			return Response.status(Status.BAD_REQUEST).entity(
					"Request to add object of type "
							+ object.getCompileTimeClass().getSimpleName()
							+ " to the collection of " + type).type(MediaType.TEXT_HTML).build();
		}
		
		
		ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
		
		String oid;
		try {
			oid = model.addObject(object, modelExecuteOptions, task, parentResult);
			LOGGER.info("returned oid :  {}", oid );
			
			URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
			ResponseBuilder builder = clazz.isAssignableFrom(TaskType.class) ? Response.accepted().location(resourceURI) : Response.created(resourceURI);
			
			return builder.build();
		} catch (ObjectAlreadyExistsException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
	}

	@PUT
	@Path("/{type}/{id}")
//	@Produces({"text/html", "application/xml"})
	public <T extends ObjectType> Response addObject(@PathParam("type") String type, @PathParam("id") String id, PrismObject<T> object, @QueryParam("options") List<String> options, @Context UriInfo uriInfo, @Context Request request){
	
LOGGER.info("model rest service for add operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("add");
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		if (!object.getCompileTimeClass().equals(clazz)){
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
		try {
			oid = model.addObject(object, modelExecuteOptions, task, parentResult);
			LOGGER.info("returned oid :  {}", oid );
			
			URI resourceURI = uriInfo.getAbsolutePathBuilder().path(oid).build(oid);
			ResponseBuilder builder = clazz.isAssignableFrom(TaskType.class) ? Response.accepted().location(resourceURI) : Response.created(resourceURI);
			
			return builder.build();
		} catch (ObjectAlreadyExistsException e) {
			return Response.serverError().entity(e.getMessage()).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
	
	}
	
	@DELETE
	@Path("/{type}/{id}")
//	@Produces({"text/html", "application/xml"})
	public Response deleteObject(@PathParam("type") String type, @PathParam("id") String id, @QueryParam("options") List<String> options){

		LOGGER.info("model rest service for delete operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("delete");
		
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		
		try {
			if (clazz.isAssignableFrom(TaskType.class)){
				model.suspendAndDeleteTasks(MiscUtil.createCollection(id), WAIT_FOR_TASK_STOP, true, parentResult);
				parentResult.computeStatus();
				
				if (parentResult.isSuccess()){
					return Response.noContent().build();
				}
				
				return Response.serverError().entity(parentResult.getMessage()).build();
				
			} 
			
			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
			
			model.deleteObject(clazz, id, modelExecuteOptions, task, parentResult);
			return Response.noContent().build();
			
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConsistencyViolationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		
	}
	
	
	@PATCH
	@Path("/{type}/{oid}")
//	@Produces({"text/html", "application/xml"})
	public <T extends ObjectType> Response modifyObject(@PathParam("type") String type, @PathParam("oid") String oid, 
			ObjectModificationType modificationType, @QueryParam("options") List<String> options){
		
		LOGGER.info("model rest service for modify operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("modifyObject");
		
		
		Class clazz = ObjectTypes.getClassFromRestType(type);
		
		
		
		
		try {
			ModelExecuteOptions modelExecuteOptions = ModelExecuteOptions.fromRestOptions(options);
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(modificationType, clazz, prismContext);
			model.modifyObject(clazz, oid, modifications, modelExecuteOptions, task, parentResult);
			return Response.noContent().build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ExpressionEvaluationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectAlreadyExistsException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (PolicyViolationException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
	}
	
	@POST
	@Path("/notifyChange")
	public Response notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription, @Context UriInfo uriInfo){
		LOGGER.info("model rest service for notify change operation start");

		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("find shadow owner");
		try {
			model.notifyChange(changeDescription, parentResult, task);
			return Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject").build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (ObjectAlreadyExistsException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
	}


	
	@GET
	@Path("/shadows/{oid}/owner")
//	@Produces({"text/html", "application/xml"})
	public Response findShadowOwner(@PathParam("oid") String shadowOid){
		
		LOGGER.info("model rest service for find shadow owner operation start");

		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("find shadow owner");
		
		
		try {
			PrismObject<UserType> user = model.findShadowOwner(shadowOid, task, parentResult);
			return Response.ok().entity(user).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
	
	}

	@POST
	@Path("/{type}/search")
//	@Produces({"text/html", "application/xml"})
	public Response searchObjects(@PathParam("type") String type, QueryType queryType){
	
		LOGGER.info("model rest service for find shadow owner operation start");

		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("find shadow owner");

		Class clazz = ObjectTypes.getClassFromRestType(type);
		try {	
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(clazz, queryType, prismContext);
		
//		Collection<SelectorOptions<GetOperationOptions>> options = MiscSchemaUtil.optionsTypeToOptions(optionsType);
		
		List<PrismObject<? extends ShadowType>> objects = model.searchObjects(clazz, query, null, task, parentResult);
		
		ObjectListType listType = new ObjectListType();
		for (PrismObject<? extends ObjectType> o : objects) {
			listType.getObject().add(o.asObjectable());
		}
		
		return Response.ok().entity(listType).build();
		
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		
	}

	@POST
	@Path("/resources/{resourceOid}/import/{objectClass}")
//	@Produces({"text/html", "application/xml"})
	public Response importFromResource(@PathParam("resourceOid") String resourceOid, @PathParam("objectClass") String objectClass, @Context MessageContext mc, @Context UriInfo uriInfo)
			{
	
		LOGGER.info("model rest service for import from resource operation start");

		UserType user = (UserType) mc.get("authenticatedUser");
		
		Task task = taskManager.createTaskInstance();
		task.setOwner(user.asPrismObject());
		
		OperationResult parentResult = new OperationResult("find shadow owner");

		
		QName objClass = new QName(MidPointConstants.NS_RI, objectClass);
		
		try {
			model.importFromResource(resourceOid, objClass, task, parentResult);
			return Response.seeOther((uriInfo.getBaseUriBuilder().path(this.getClass(), "getObject").build(ObjectTypes.TASK.getRestType(), task.getOid()))).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SchemaException e) {
			return Response.status(Status.CONFLICT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (CommunicationException e) {
			return Response.status(Status.GATEWAY_TIMEOUT).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (ConfigurationException e) {
			return Response.status(Status.BAD_GATEWAY).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		} catch (SecurityViolationException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
		
		
	}

	@POST
	@Path("/resources/{resourceOid}/test")
//	@Produces({"text/html", "application/xml"})
	public Response testResource(@PathParam("resourceOid") String resourceOid){

		LOGGER.info("model rest service for test resource operation start");

		Task task = taskManager.createTaskInstance();

		try {
			OperationResult result = model.testResource(resourceOid, task);
			return Response.ok(result).build();
		} catch (ObjectNotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_HTML).build();
		}
	
	}
	
	@POST
	@Path("/tasks/{oid}/suspend")
    public Response suspendTasks(@PathParam("oid") String taskOid) {
		OperationResult parentResult = new OperationResult("suspend task.");
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        boolean suspended = model.suspendTasks(taskOids, WAIT_FOR_TASK_STOP, parentResult);
        
        parentResult.computeStatus();
        
        if (parentResult.isSuccess()){
        	return Response.noContent().build();
        } 
        
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        
    }

//	@DELETE
//	@Path("tasks/{oid}/suspend")
    public Response suspendAndDeleteTasks(@PathParam("oid") String taskOid) {
    	OperationResult parentResult = new OperationResult("suspend task.");
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        model.suspendAndDeleteTasks(taskOids, WAIT_FOR_TASK_STOP, true, parentResult);
        
        if (parentResult.isSuccess()){
        	return Response.accepted().build();
        } 
        
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
    }
	
	
	@POST
	@Path("/tasks/{oid}/resume")
    public Response resumeTasks(@PathParam("oid") String taskOid) {
		OperationResult parentResult = new OperationResult("suspend task.");
		Collection<String> taskOids = MiscUtil.createCollection(taskOid);
        model.resumeTasks(taskOids, parentResult);
        
        parentResult.computeStatus();
        
        if (parentResult.isSuccess()){
        	return Response.accepted().build();
        } 
        
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(parentResult.getMessage()).build();
        
        
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
}
