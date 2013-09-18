package com.evolveum.midpoint.model;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

@Service
public class ModelRestService {
	
	@Autowired(required= true)
	private ModelCrudService model;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	
	private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);
	
	public ModelRestService(){
		
	}
	
	
	
	@GET
	@Path("/{type}/{id}")
	public Objectable getObject(@PathParam("type") String type, @PathParam("id") String id){
		LOGGER.info("model rest service for get operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("get");
		try{
			
			Class clazz = ObjectTypes.getClassFromRestType(type);
			
		PrismObject object = model.getObject(clazz, id, null, task, parentResult);
		
		return object.asObjectable();
		} catch (Exception ex){
			Response.serverError().build();
		}
		
		return null;
		
	}
	
	@GET
	@Path("/example")
	public Response getOperation(){
		LOGGER.info("model rest service for example operation start");
		
		return Response.status(Status.ACCEPTED).build();
		
	}
	
	@POST
	@Path("/objects")
	public String addObject(ObjectType object){
		LOGGER.info("model rest service for add operation start");
		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult("add");
		try{
		String oid = model.addObject(object.asPrismObject(), task, parentResult);
		
		LOGGER.info("returned oid :  {}", oid );
		return oid;
		} catch (Exception ex){
			LOGGER.info("exception occured :  {}", ex.getMessage(), ex );
			Response.serverError().build();
		}
		
		return null;
		
	}

}
