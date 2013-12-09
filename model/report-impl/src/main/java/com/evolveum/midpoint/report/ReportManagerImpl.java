/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.report;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;


/**
 * @author lazyman, garbika
 */
@Component
public class ReportManagerImpl implements ReportManager, ChangeHook {
	
    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";
  //private static final Trace LOGGER = TraceManager.getTrace(ReportManagerImpl.class);
 
    private static final String CLASS_NAME_WITH_DOT = ReportManagerImpl.class.getName() + ".";
	
    private static final String ADD_REPORT = CLASS_NAME_WITH_DOT + "addReport";
    private static final String MODIFY_REPORT = CLASS_NAME_WITH_DOT + "modifyReport";
    private static final String DELETE_REPORT = CLASS_NAME_WITH_DOT + "deleteReport";
    private static final String COUNT_REPORT = CLASS_NAME_WITH_DOT + "countReport";
	
	@Autowired
    private HookRegistry hookRegistry;
    
	@Autowired
    private ModelService modelService;
    
	@Autowired
    private PrismContext prismContext;
	
	@Autowired
	protected TaskManager taskManager;
	
    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }
    
    @Override
    public String addReport(PrismObject<ReportType> object, OperationResult parentResult) 
    		throws ObjectAlreadyExistsException, SchemaException, SecurityViolationException, 
    		PolicyViolationException, ConfigurationException, CommunicationException,
    		ExpressionEvaluationException, ObjectNotFoundException 
    {
    	Validate.notNull(object, "Object must not be null.");
		Validate.notNull(parentResult, "Result type must not be null.");

		object.checkConsistence();
		
		OperationResult result = parentResult.createSubresult(ADD_REPORT);
		result.addParams(new String[] { "object" }, object);
		String oid = null;

		
		RepositoryCache.enter();
		try {
/*
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Entering addObject with {}", object);
				LOGGER.trace(object.dump());
			}
			
			if (options == null) {
				if (StringUtils.isNotEmpty(objectType.getVersion())) {
					options = ModelExecuteOptions.createOverwrite();
				}
			}
	*/		
			ObjectDelta<ReportType> objectDelta = ObjectDelta.createAddDelta(object);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			modelService.executeChanges(deltas, null, null, result);
			
			oid = objectDelta.getOid();

			result.computeStatus();
			result.cleanupResult();
		}
		catch (ObjectAlreadyExistsException ex) 
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		} 
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (SecurityViolationException ex)
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (PolicyViolationException ex)
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (ConfigurationException ex)
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (CommunicationException ex)
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (ExpressionEvaluationException ex)
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		}
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(parentResult, ex);
			throw ex;
		} 
		finally
		{
			RepositoryCache.exit();
		}

		return oid;
    }

    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param report
     * @param task
     * @param parentResult describes report which has to be created
     */
    @Override
    public void runReport(PrismObject<ReportType> report, Task task, OperationResult parentResult) {
        //todo implement
    }
    public PrismObject<ReportType> getReport(String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) 
    		throws ObjectNotFoundException, SchemaException, ConfigurationException, CommunicationException, SecurityViolationException 
    {
    	return modelService.getObject(ReportType.class, oid, options, null, parentResult);
    }

    /**
     * Transforms change:
     * 1/ ReportOutputType DELETE to MODIFY some attribute to mark it for deletion.
     * 2/ ReportType ADD and MODIFY should compute jasper design and styles if necessary
     *
     * @param context
     * @param task
     * @param result
     * @return
     */
    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {
        //todo implement
        return HookOperationMode.FOREGROUND;
    }

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
    	
    }
    @Override
    public List<PrismObject<ReportType>> searchReports(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) 
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
    	return modelService.searchObjects(ReportType.class, query, options, null, parentResult);
    }

    @Override
    public int countReports(ObjectQuery query, OperationResult parentResult) throws SchemaException 
    {
    	//LOGGER.trace("begin::countReport()");
        int count = 0;
        OperationResult result = parentResult.createSubresult(COUNT_REPORT);
        try {
        	count = modelService.countObjects(ReportType.class, query, null, null, result);
            result.recordSuccess();
        } 
        catch (Exception ex) 
        {
            result.recordFatalError("Couldn't count objects.", ex);
        }
        //LOGGER.trace("end::countReport()");
        return count;
    }

    @Override
    public void modifyReport(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) 
    		throws ObjectAlreadyExistsException, SchemaException, SecurityViolationException, 
    		PolicyViolationException, ConfigurationException, CommunicationException,
    		ExpressionEvaluationException, ObjectNotFoundException 
    {
    	
    	Validate.notNull(modifications, "Object modification must not be null.");
		Validate.notEmpty(oid, "Change oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");
/*
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Modifying object with oid {}", oid);
			LOGGER.trace(DebugUtil.debugDump(modifications));
		}

		if (modifications.isEmpty()) {
			LOGGER.warn("Calling modifyObject with empty modificaiton set");
			return;
		}
*/
		ItemDelta.checkConsistence(modifications);
		// TODO: check definitions, but tolerate missing definitions in <attributes>

		OperationResult result = parentResult.createSubresult(MODIFY_REPORT);
		result.addCollectionOfSerializablesAsParam("modifications", modifications);

		RepositoryCache.enter();

		try {

			ObjectDelta<ReportType> objectDelta = (ObjectDelta<ReportType>) ObjectDelta.createModifyDelta(oid, modifications, ReportType.class, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
			modelService.executeChanges(deltas, null, null, result);

            result.computeStatus();
			
        } /*
    } catch (ExpressionEvaluationException ex) {
		LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
		result.recordFatalError(ex);
		throw ex;
	} catch (ObjectNotFoundException ex) {
		LOGGER.error("model.modifyObject failed: {}", ex.getMessage(), ex);
		result.recordFatalError(ex);
		throw ex;*/
		catch (ObjectAlreadyExistsException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ObjectNotFoundException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (SchemaException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (ExpressionEvaluationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (CommunicationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 		
		catch (ConfigurationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (PolicyViolationException ex)
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		}
		catch (SecurityViolationException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		catch (RuntimeException ex) 
		{
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} 
		finally
		{
			RepositoryCache.exit();
		}
		
    }

    @Override
    public void deleteReport(String oid, OperationResult parentResult) 
    		throws ObjectNotFoundException, ConsistencyViolationException,
    		CommunicationException, SchemaException, ConfigurationException, PolicyViolationException,
    		SecurityViolationException{
    	
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(parentResult, "Result type must not be null.");

		Task task = taskManager.createTaskInstance(DELETE_REPORT);
		OperationResult result = parentResult.createSubresult(DELETE_REPORT);
		result.addParams(new String[] { "task" }, task.getResult());
		result.addParams(new String[] { "oid" }, oid);
		

		RepositoryCache.enter();
	
		try {
			ObjectDelta<ReportType> delta = ObjectDelta.createDeleteDelta(ReportType.class, oid, prismContext);

			//LOGGER.trace("Deleting object with oid {}.", new Object[] { oid });
			
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
			ModelExecuteOptions options = new ModelExecuteOptions();
			options.setRaw(true);
			
			modelService.executeChanges(deltas, options, task, result);
			
			result.computeStatus();
			//result.recordSuccess();

		} catch (ObjectNotFoundException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (CommunicationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (SecurityViolationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (RuntimeException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw new SystemException(ex.getMessage(), ex);
		} catch (ExpressionEvaluationException ex) {
			ModelUtils.recordFatalError(result, ex);
			throw new SystemException(ex.getMessage(), ex);
		} finally {
			RepositoryCache.exit();
		}
    }

  
    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
