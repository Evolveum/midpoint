/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.lens;

import static com.evolveum.midpoint.common.CompiletimeConfig.CONSISTENCY_CHECKS;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SynchronizationSituationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

	private static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";
	private static final String OPERATION_LINK_ACCOUNT = ChangeExecutor.class.getName() + ".linkAccount";
	private static final String OPERATION_UNLINK_ACCOUNT = ChangeExecutor.class.getName() + ".unlinkAccount";

    @Autowired(required = true)
    private transient TaskManager taskManager;

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioning;
    
    @Autowired(required = true)
    private PrismContext prismContext;
    
    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismObjectDefinition<AccountShadowType> accountDefinition = null;
    
    @PostConstruct
    private void locateUserDefinition() {
    	userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    	accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
    }

    public <F extends ObjectType, P extends ObjectType> void executeChanges(LensContext<F,P> syncContext, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {
    	
    	OperationResult result = parentResult.createSubresult(ChangeExecutor.class+".executeChanges");
    	
    	try {
	    	LensFocusContext<F> focusContext = syncContext.getFocusContext();
	    	if (focusContext != null) {
		        ObjectDelta<F> userDelta = focusContext.getWaveDelta(syncContext.getExecutionWave());
		        if (userDelta != null) {
		
		            executeDelta(userDelta, focusContext, syncContext, task, result);
		
                    if (UserType.class.isAssignableFrom(userDelta.getObjectTypeClass()) && task.getRequesteeOid() == null) {
                        task.setRequesteeOidImmediate(userDelta.getOid(), result);
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Set requestee OID to " + userDelta.getOid());
                        }
                    }
		        } else {
		            LOGGER.trace("Skipping change execute, because user delta is null");
		        }
	    	}
	
	        for (LensProjectionContext<P> accCtx : syncContext.getProjectionContexts()) {
	        	if (accCtx.getWave() != syncContext.getExecutionWave()) {
	        		continue;
	        	}
	            ObjectDelta<P> accDelta = accCtx.getExecutableDelta();
	            if (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN){
	            	if (syncContext.getFocusContext().getDelta() != null
							&& syncContext.getFocusContext().getDelta().isDelete() && syncContext.getOptions() != null
							&& ModelExecuteOptions.isForce(syncContext.getOptions())) {
						if (accDelta == null){
							accDelta = ObjectDelta.createDeleteDelta(accCtx.getObjectTypeClass(), accCtx.getOid(), prismContext);
						}
	            	}
	            	if (accDelta != null && accDelta.isDelete()){
	            	
						 executeDelta(accDelta, accCtx, syncContext, task, result);
			 	            
//						accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
	            	}
	            } else{
	            	if (accDelta == null || accDelta.isEmpty()) {
		                if (LOGGER.isTraceEnabled()) {
		                	LOGGER.trace("No change for account " + accCtx.getResourceShadowDiscriminator());
		                	LOGGER.trace("Delta:\n{}", accDelta == null ? null : accDelta.dump());
		                }
		                if (focusContext != null) {
		                	updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task, result);
		                }
		                continue;
		            }
	            	
	            	 executeDelta(accDelta, accCtx, syncContext, task, result);

	            }
	            
	            if (focusContext != null) {
	            	updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task, result);
	            }
	        }
	        
	        result.computeStatus();
	        
    	} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			result.computeStatus();
			if (!result.isSuccess()) {
				result.recordFatalError(e);
			}
			throw e;
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (RuntimeException e) {
			result.recordFatalError(e);
			throw e;
    	}  
        
    }

    /**
     * Make sure that the account is linked (or unlinked) as needed.
     */
    private <F extends ObjectType, P extends ObjectType> void updateAccountLinks(PrismObject<F> prismObject,
    		LensFocusContext<F> focusContext, LensProjectionContext<P> accCtx,
    		Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, RewindException {
    	if (prismObject == null) {
    		return;
    	}
        F objectTypeNew = prismObject.asObjectable();
        if (!(objectTypeNew instanceof UserType)) {
        	return;
        }
        UserType userTypeNew = (UserType) objectTypeNew;
        String accountOid = accCtx.getOid();
        if (accountOid == null) {
            throw new IllegalStateException("Account has null OID, this should not happen");
        }

        if (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.UNLINK 
        		|| accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.DELETE
        		|| accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
            // Link should NOT exist
        	
        	PrismReference accountRef = userTypeNew.asPrismObject().findReference(UserType.F_ACCOUNT_REF);
        	if (accountRef != null) {
        		for (PrismReferenceValue accountRefVal: accountRef.getValues()) {
        			if (accountRefVal.getOid().equals(accountOid)) {
                        // Linked, need to unlink
                        unlinkAccount(userTypeNew.getOid(), accountRefVal, (LensFocusContext<UserType>) focusContext, task, result);
                    }
        		}
        		
        	}
            
            //update account situation only if the account was not deleted
        	if (accCtx != null && !accCtx.isDelete()) {
				LOGGER.trace("Account {} unlinked from the user, updating also situation in account.", accountOid);	
				updateSituationInAccount(task, null, accountOid, result);
				LOGGER.trace("Situation in the account was updated to {}.", "null");
			}
            // Not linked, that's OK

        } else {
            // Link should exist
        	
            for (ObjectReferenceType accountRef : userTypeNew.getAccountRef()) {
                if (accountOid.equals(accountRef.getOid())) {
                    // Already linked, nothing to do, only be sure, the situation is set with the good value
                	LOGGER.trace("Updating situation in already linked account.");
                	updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountOid, result);
                	LOGGER.trace("Situation in account was updated to {}.", SynchronizationSituationType.LINKED);
                	return;
                }
            }
            // Not linked, need to link
            linkAccount(userTypeNew.getOid(), accountOid, (LensFocusContext<UserType>) focusContext, task, result);
            //be sure, that the situation is set correctly
            LOGGER.trace("Updating situation after account was linked.");
            updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountOid, result);
            LOGGER.trace("Situation in account was updated to {}.", SynchronizationSituationType.LINKED);
        }
    }

    private void linkAccount(String userOid, String accountOid, LensElementContext<UserType> userContext, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {

        LOGGER.trace("Linking account " + accountOid + " to user " + userOid);
        
        OperationResult result = parentResult.createSubresult(OPERATION_LINK_ACCOUNT);
        
        PrismReferenceValue accountRef = new PrismReferenceValue();
        accountRef.setOid(accountOid);
        accountRef.setTargetType(AccountShadowType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationAddCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef); 

        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        } finally {
        	result.computeStatus();
        	ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, accountRefDeltas, UserType.class, prismContext);
            ObjectDeltaOperation<UserType> userDeltaOp = new ObjectDeltaOperation<UserType>(userDelta);
            userDeltaOp.setExecutionResult(result);
    		userContext.addToExecutedDeltas(userDeltaOp);
        }
//        updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountRef, result);
        
    }

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return userDefinition;
	}

	private void unlinkAccount(String userOid, PrismReferenceValue accountRef, LensElementContext<UserType> userContext, Task task, OperationResult parentResult) throws
            ObjectNotFoundException, SchemaException {

        LOGGER.trace("Deleting accountRef " + accountRef + " from user " + userOid);
        
        OperationResult result = parentResult.createSubresult(OPERATION_UNLINK_ACCOUNT);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationDeleteCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef.clone()); 
        
        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
        	result.recordFatalError(ex);
            throw new SystemException(ex);
        } finally {
        	result.computeStatus();
        	ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, accountRefDeltas, UserType.class, prismContext);
            ObjectDeltaOperation<UserType> userDeltaOp = new ObjectDeltaOperation<UserType>(userDelta);
            userDeltaOp.setExecutionResult(result);
    		userContext.addToExecutedDeltas(userDeltaOp);
        }
        
      //setting new situation to account
//        updateSituationInAccount(task, null, accountRef, result);

    }
	
    private void updateSituationInAccount(Task task, SynchronizationSituationType situation, String accountRef, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, RewindException{

    	OperationResult result = new OperationResult("Updating situation in account (Model)");
    	result.addParam("situation", situation);
    	result.addParam("accountRef", accountRef);
		
    	PrismObject<AccountShadowType> account = null;
    	try{
    		account = provisioning.getObject(AccountShadowType.class, accountRef, GetOperationOptions.createNoFetch(), result);
    	} catch (Exception ex){
    		LOGGER.trace("Problem with getting account, skipping modifying siatuation in account.");
			return;
    	}
    	
    	List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.createSynchronizationSituationDescriptionDelta(account, situation, task.getChannel());
		PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(account, situation);
		if (syncSituationDelta != null){
		syncSituationDeltas.add(syncSituationDelta);
		}
		try {
			modifyProvisioningObject(AccountShadowType.class, accountRef, syncSituationDeltas, ProvisioningOperationOptions.createCompletePostponed(false), task, result);
		} catch (ObjectNotFoundException ex) {
			// if the object not found exception is thrown, it's ok..probably
			// the account was deleted by previous execution of changes..just
			// log in the trace the message for the user.. 
			LOGGER.trace("Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			return;
		}
		// if everything is OK, add result of the situation modification to the
		// parent result
		result.recordSuccess();
		parentResult.addSubresult(result);
		
	}
    
	private <T extends ObjectType, F extends ObjectType, P extends ObjectType>
    	void executeDelta(ObjectDelta<T> objectDelta, LensElementContext<T> objectContext, LensContext<F,P> context, Task task, OperationResult parentResult) 
    			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
    			ConfigurationException, SecurityViolationException, RewindException {
		
        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }
        
        // Other types than user type may not be definition-complete (e.g. accounts and resources are completed in provisioning)
        if (UserType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
        	objectDelta.assertDefinitions();
        }
        
    	if (LOGGER.isTraceEnabled()) {
    		logDeltaExecution(objectDelta, context, null);
    	}

    	OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE_DELTA);
    	
    	try {
    		
    		ProvisioningOperationOptions provisioningOptions = copyFromModelOptions(context.getOptions());
   
    		if (context.getChannel() != null && context.getChannel().equals(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON))){
    			provisioningOptions.setCompletePostponed(false);
    		}
    	
	        if (objectDelta.getChangeType() == ChangeType.ADD) {
	            executeAddition(objectDelta, provisioningOptions, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
	        	executeModification(objectDelta, provisioningOptions, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
	            executeDeletion(objectDelta, provisioningOptions, task, result);
	        }
	        
	        // To make sure that the OID is set (e.g. after ADD operation)
	        objectContext.setOid(objectDelta.getOid());
	        
    	} finally {
    		
    		result.computeStatus();
    		ObjectDeltaOperation<T> objectDeltaOp = new ObjectDeltaOperation<T>(objectDelta.clone());
	        objectDeltaOp.setExecutionResult(result);
	        objectContext.addToExecutedDeltas(objectDeltaOp);
        
	        if (LOGGER.isDebugEnabled()) {
	        	if (LOGGER.isTraceEnabled()) {
	        		LOGGER.trace("EXECUTION result {}", result.getLastSubresult());
	        	} else {
	        		// Execution of deltas was not logged yet
	        		logDeltaExecution(objectDelta, context, result.getLastSubresult());
	        	}
	    	}
    	}
    }
	
	private ProvisioningOperationOptions copyFromModelOptions(ModelExecuteOptions options) {
		ProvisioningOperationOptions provisioningOptions = new ProvisioningOperationOptions();
		if (options == null){
			return provisioningOptions;
		}
		
		provisioningOptions.setForce(options.getForce());
		return provisioningOptions;
	}

	private <T extends ObjectType, F extends ObjectType, P extends ObjectType>
				void logDeltaExecution(ObjectDelta<T> objectDelta, LensContext<F,P> context, OperationResult result) {
		StringBuilder sb = new StringBuilder();
		sb.append("---[ ");
		if (result == null) {
			sb.append("Going to EXECUTE");
		} else {
			sb.append("EXECUTED");
		}
		sb.append(" delta of ").append(objectDelta.getObjectTypeClass().getSimpleName());
		sb.append(" ]---------------------\n");
		DebugUtil.debugDumpLabel(sb, "Channel", 0);
		sb.append(" ").append(context.getChannel()).append("\n");
		DebugUtil.debugDumpLabel(sb, "Wave", 0);
		sb.append(" ").append(context.getExecutionWave()).append("\n");
		sb.append(objectDelta.dump());
		sb.append("\n");
		if (result != null) {
			DebugUtil.debugDumpLabel(sb, "Result", 0);
			sb.append(" ").append(result.getStatus()).append(": ").append(result.getMessage());
		}
		sb.append("\n--------------------------------------------------");
		
		LOGGER.debug("\n{}", sb);
	}

    private <T extends ObjectType> void executeAddition(ObjectDelta<T> change, ProvisioningOperationOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException {

        PrismObject<T> objectToAdd = change.getObjectToAdd();

        if (change.getModifications() != null) {
            for (ItemDelta delta : change.getModifications()) {
                delta.applyTo(objectToAdd);
            }
            change.getModifications().clear();
        }

        T objectTypeToAdd = objectToAdd.asObjectable();

        String oid = null;
        if (objectTypeToAdd instanceof TaskType) {
            oid = addTask((TaskType) objectTypeToAdd, result);
        } else if (ObjectTypes.isManagedByProvisioning(objectTypeToAdd)) {
            oid = addProvisioningObject(objectToAdd, options, task, result);
            if (oid == null) {
            	throw new SystemException("Provisioning addObject returned null OID while adding " + objectToAdd);
            }
            result.addReturn("createdAccountOid", oid);
        } else {
            oid = cacheRepositoryService.addObject(objectToAdd, result);
            if (oid == null) {
            	throw new SystemException("Repository addObject returned null OID while adding " + objectToAdd);
            }
        }
        change.setOid(oid);
    }

    private <T extends ObjectType> void executeDeletion(ObjectDelta<T> change, ProvisioningOperationOptions options, Task task, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            deleteProvisioningObject(objectTypeClass, oid, options, task, result);
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private <T extends ObjectType> void executeModification(ObjectDelta<T> change, ProvisioningOperationOptions options, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, RewindException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(change.getOid(), change.getModifications(), result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            String oid = modifyProvisioningObject(objectTypeClass, change.getOid(), change.getModifications(), options, task, result);
            if (!oid.equals(change.getOid())){
            	change.setOid(oid);
            }
        } else {
            cacheRepositoryService.modifyObject(objectTypeClass, change.getOid(), change.getModifications(), result);
        }
    }

    private String addTask(TaskType task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException {
        try {
            return taskManager.addTask(task.asPrismObject(), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't add object {} to task manager", ex, task.getName());
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private String addProvisioningObject(PrismObject<? extends ObjectType> object, ProvisioningOperationOptions options, Task task, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, RewindException {

        if (object.canRepresent(ResourceObjectShadowType.class)) {
            ResourceObjectShadowType shadow = (ResourceObjectShadowType) object.asObjectable();
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        try {
            ProvisioningScriptsType scripts = getScripts(object.asObjectable(), result);
            String oid = provisioning.addObject(object, scripts, options, task, result);
            return oid;
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            throw new RewindException(ex);
        } catch (CommunicationException ex) {
            throw ex;
        } catch (ConfigurationException e) {
			throw e;
        } catch (RuntimeException ex) {
            throw new SystemException(ex.getMessage(), ex);
		}
    }

    private void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid, ProvisioningOperationOptions options, Task task, 
            OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException {

        try {
            // TODO: scripts
            provisioning.deleteObject(objectTypeClass, oid, options, null, task, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex);
        }
    }

    private String modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options, Task task, OperationResult result) throws ObjectNotFoundException, RewindException {

        try {
            // TODO: scripts
            String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, null, options, task, result);
            return changedOid;
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private ProvisioningScriptsType getScripts(ObjectType object, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	ProvisioningScriptsType scripts = null;
        if (object instanceof ResourceType) {
            ResourceType resource = (ResourceType) object;
            scripts = resource.getScripts();
        } else if (object instanceof ResourceObjectShadowType) {
            ResourceObjectShadowType resourceObject = (ResourceObjectShadowType) object;
            if (resourceObject.getResource() != null) {
                scripts = resourceObject.getResource().getScripts();
            } else {
                String resourceOid = ResourceObjectShadowUtil.getResourceOid(resourceObject);
                ResourceType resObject = provisioning.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
                scripts = resObject.getScripts();
            }
        }

        if (scripts == null) {
            scripts = new ProvisioningScriptsType();
        }

        return scripts;
    }

}
