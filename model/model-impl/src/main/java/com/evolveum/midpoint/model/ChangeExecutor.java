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

package com.evolveum.midpoint.model;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;

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

    public void executeChanges(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult parentResult) throws
            ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, 
            SecurityViolationException {
    	OperationResult result = parentResult.createSubresult(ChangeExecutor.class.getName() + ".executeChanges");
    	result.addParam("changes", changes);
    	try {
	        for (ObjectDelta<? extends ObjectType> change : changes) {
	            executeChange(change, task, result);
	        }
	        result.computeStatus();
    	} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			result.recordFatalError(e);
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

    public <F extends ObjectType, P extends ObjectType> void executeChanges(LensContext<F,P> syncContext, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	OperationResult result = parentResult.createSubresult(ChangeExecutor.class+".executeChanges");
    	
    	try {
	    	LensFocusContext<F> focusContext = syncContext.getFocusContext();
	    	if (focusContext != null) {
		        ObjectDelta<F> userDelta = focusContext.getWaveDelta();
		        if (userDelta != null) {
		
		            executeChange(userDelta, task, result);
		
		            // userDelta is composite, mixed from primary and secondary. The OID set into
		            // it will be lost ... unless we explicitly save it
		            focusContext.setOid(userDelta.getOid());
		        } else {
		            LOGGER.trace("Skipping change execute, because user delta is null");
		        }
	    	}
	
	        for (LensProjectionContext<P> accCtx : syncContext.getProjectionContexts()) {
	        	if (accCtx.getWave() != syncContext.getWave()) {
	        		continue;
	        	}
	            ObjectDelta<P> accDelta = accCtx.getDelta();
	            if (accDelta == null || accDelta.isEmpty()) {
	                if (LOGGER.isTraceEnabled()) {
	                	LOGGER.trace("No change for account " + accCtx.getResourceShadowDiscriminator());
	                	LOGGER.trace("Delta:\n{}", accDelta == null ? null : accDelta.dump());
	                }
	                if (focusContext != null) {
	                	updateAccountLinks(focusContext.getObjectNew(), accCtx, task, result);
	                }
	                continue;
	            }
	            
	            executeChange(accDelta, task, result);
	            
	            // To make sure that the OID is set (e.g. after ADD operation)
	            accCtx.setOid(accDelta.getOid());
	            if (focusContext != null) {
	            	updateAccountLinks(focusContext.getObjectNew(), accCtx, task, result);
	            }
	        }
	        
	        if (LOGGER.isTraceEnabled()) {
	            LOGGER.trace("Context after change execution:\n{}", syncContext.dump(false));
	        }
	        
	        result.computeStatus();
	        
    	} catch (SchemaException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ObjectAlreadyExistsException e) {
			result.recordFatalError(e);
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
    private <F extends ObjectType, P extends ObjectType> void updateAccountLinks(PrismObject<F> prismObject, LensProjectionContext<P> accCtx,
    		Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
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

        if (accCtx.getPolicyDecision() == PolicyDecision.UNLINK || accCtx.getPolicyDecision() == PolicyDecision.DELETE) {
            // Link should NOT exist
            for (ObjectReferenceType accountRef : userTypeNew.getAccountRef()) {
                if (accountRef.getOid().equals(accountOid)) {
                    // Linked, need to unlink
                    unlinkAccount(userTypeNew.getOid(), accountOid, task, result);
//                    LOGGER.trace("Start to update situation in account after unlinking it from the user.");
//    				updateSituationInAccount(task, null, accountOid, result);
//    				LOGGER.trace("Situation in the account was updated to {}.", "null");
                }
            }
            
            //update account situation only if the account was not deleted
//			if (accCtx.getDelta() != null && !accCtx.getDelta().isDelete() && !accCtx.getDelta().getOid().equals(accountOid)) {
				LOGGER.trace("Account unlinked from the user, updating also situation in account.");
				updateSituationInAccount(task, null, accountOid, result);
				LOGGER.trace("Situation in the account was updated to {}.", "null");
//			}
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
            linkAccount(userTypeNew.getOid(), accountOid, task, result);
            //be sure, that the situation is set correctly
            LOGGER.trace("Updating situation after account was linked.");
            updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountOid, result);
            LOGGER.trace("Situation in account was updated to {}.", SynchronizationSituationType.LINKED);
        }
    }

    private void linkAccount(String userOid, String accountOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException {

        LOGGER.trace("Linking account " + accountOid + " to user " + userOid);
        PrismReferenceValue accountRef = new PrismReferenceValue();
        accountRef.setOid(accountOid);
        accountRef.setTargetType(AccountShadowType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationAddCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef); 

        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
//        updateSituationInAccount(task, SynchronizationSituationType.LINKED, accountRef, result);
    }

	private PrismObjectDefinition<UserType> getUserDefinition() {
		return userDefinition;
	}

	private void unlinkAccount(String userOid, String accountOid, Task task, OperationResult result) throws
            ObjectNotFoundException, SchemaException {

        LOGGER.trace("Unlinking account " + accountOid + " to user " + userOid);
        PrismReferenceValue accountRef = new PrismReferenceValue();
        accountRef.setOid(accountOid);
        accountRef.setTargetType(AccountShadowType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationDeleteCollection(
        		UserType.F_ACCOUNT_REF, getUserDefinition(), accountRef); 
        
        try {
            cacheRepositoryService.modifyObject(UserType.class, userOid, accountRefDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
        
      //setting new situation to account
//        updateSituationInAccount(task, null, accountRef, result);

    }
	
    private void updateSituationInAccount(Task task, SynchronizationSituationType situation, String accountRef, OperationResult parentResult) throws ObjectNotFoundException, SchemaException{

		List<PropertyDelta> syncSituationDeltas = new ArrayList<PropertyDelta>();

		PropertyDelta syncSituationDelta = PropertyDelta.createReplaceDelta(accountDefinition,
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION, situation);
		syncSituationDeltas.add(syncSituationDelta);

		// new situation description
		SynchronizationSituationDescriptionType syncSituationDescription = new SynchronizationSituationDescriptionType();
		syncSituationDescription.setSituation(situation);
		syncSituationDescription.setChannel(task.getHandlerUri());
		syncSituationDescription.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
		
		syncSituationDelta = PropertyDelta.createDelta(new PropertyPath(
				ResourceObjectShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION), accountDefinition);
		syncSituationDelta.addValueToAdd(new PrismPropertyValue(syncSituationDescription));
		syncSituationDeltas.add(syncSituationDelta);
		
		try {
			modifyProvisioningObject(AccountShadowType.class, accountRef, syncSituationDeltas, parentResult);
		} catch (ObjectNotFoundException ex) {
			// HACK: it can happen, if the account was deleted previously and
						// now only the link should be removed..It's expected, that it
						// should happen, but how to recognize it in provisoning that it is
						// expected?
			for (OperationResult subResults : parentResult.getSubresults()){
				subResults.computeStatus();
				subResults.muteError();
			}
			parentResult.computeStatus();
			parentResult.muteError();
			LOGGER.trace("Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			
		}
		
		
	}

	private <T extends ObjectType> void executeChange(ObjectDelta<T> objectDelta, Task task, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }
        
        // Other types than user type may not be definition-complete (e.g. accounts and resources are completed in provisioning)
        if (UserType.class.isAssignableFrom(objectDelta.getObjectTypeClass())) {
        	objectDelta.assertDefinitions();
        }
        
    	if (LOGGER.isTraceEnabled()) {
    		LOGGER.trace("\n---[ EXECUTE delta of {} {} ]---------------------\n{}" +
    				"\n--------------------------------------------------", new Object[]{
    				objectDelta.getObjectTypeClass().getSimpleName(), objectDelta.getOid(),
    				objectDelta.dump()});
    	}

        if (objectDelta.getChangeType() == ChangeType.ADD) {
            executeAddition(objectDelta, result);

        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
            executeModification(objectDelta, result);

        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
            executeDeletion(objectDelta, result);
        }
        
    }

    private <T extends ObjectType> void executeAddition(ObjectDelta<T> change, OperationResult result) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

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
            oid = addProvisioningObject(objectToAdd, result);
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

    private <T extends ObjectType> void executeDeletion(ObjectDelta<T> change, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            deleteProvisioningObject(objectTypeClass, oid, result);
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private <T extends ObjectType> void executeModification(ObjectDelta<T> change, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(change.getOid(), change.getModifications(), result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            modifyProvisioningObject(objectTypeClass, change.getOid(), change.getModifications(), result);
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

    private String addProvisioningObject(PrismObject<? extends ObjectType> object, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        if (object.canRepresent(ResourceObjectShadowType.class)) {
            ResourceObjectShadowType shadow = (ResourceObjectShadowType) object.asObjectable();
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        try {
            ProvisioningScriptsType scripts = getScripts(object.asObjectable(), result);
            String oid = provisioning.addObject(object, scripts, result);
            return oid;
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (ObjectAlreadyExistsException ex) {
            throw ex;
        } catch (CommunicationException ex) {
            throw ex;
        } catch (ConfigurationException e) {
			throw e;
        } catch (RuntimeException ex) {
            throw new SystemException(ex.getMessage(), ex);
		}
    }

    private void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException {

        try {
            // TODO: scripts
            provisioning.deleteObject(objectTypeClass, oid, null, result);
        } catch (ObjectNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
    }

    private void modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            Collection<? extends ItemDelta> modifications, OperationResult result) throws ObjectNotFoundException {

        try {
            // TODO: scripts
            provisioning.modifyObject(objectTypeClass, oid, modifications, null, result);
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
