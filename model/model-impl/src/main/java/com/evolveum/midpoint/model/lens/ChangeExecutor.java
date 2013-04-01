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

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationParameters;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.controller.ModelUtils;
import com.evolveum.midpoint.model.sync.SynchronizationSituation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
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
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
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
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

	private static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";
	private static final String OPERATION_EXECUTE = ChangeExecutor.class.getName() + ".execute";
	private static final String OPERATION_EXECUTE_FOCUS = OPERATION_EXECUTE + ".focus";
	private static final String OPERATION_EXECUTE_PROJECTION = OPERATION_EXECUTE + ".projection";
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
    
    @Autowired(required = true)
	private ExpressionFactory expressionFactory;
    
    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismObjectDefinition<AccountShadowType> accountDefinition = null;
    
    @PostConstruct
    private void locateUserDefinition() {
    	userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
    	accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
    }

    public <F extends ObjectType, P extends ObjectType> void executeChanges(LensContext<F,P> syncContext, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, RewindException, ExpressionEvaluationException {
    	
    	OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE);
    	
    	LensFocusContext<F> focusContext = syncContext.getFocusContext();
    	if (focusContext != null) {
	        ObjectDelta<F> userDelta = focusContext.getWaveDelta(syncContext.getExecutionWave());
	        if (userDelta != null) {
	
	        	OperationResult subResult = result.createSubresult(OPERATION_EXECUTE_FOCUS);
	        	try {
	        		
		            executeDelta(userDelta, focusContext, syncContext, null, task, subResult);
		
	                if (UserType.class.isAssignableFrom(userDelta.getObjectTypeClass()) && task.getRequesteeOid() == null) {
	                    task.setRequesteeOidImmediate(userDelta.getOid(), subResult);
	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Set requestee OID to " + userDelta.getOid() + "; task = " + task.dump());
	                    }
	                }
	                
	                subResult.computeStatus();
	                
	        	} catch (SchemaException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ObjectNotFoundException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ObjectAlreadyExistsException e) {
	    			subResult.computeStatus();
	    			if (!subResult.isSuccess()) {
	    				subResult.recordFatalError(e);
	    			}
	    			result.computeStatusComposite();
	    			throw e;
	    		} catch (CommunicationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (ConfigurationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (SecurityViolationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (RewindException e) {
	    			subResult.recordHandledError(e);
	    			result.computeStatusComposite();
	    			throw e;
	    		} catch (ExpressionEvaluationException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		} catch (RuntimeException e) {
	        		recordFatalError(subResult, result, null, e);
	    			throw e;
	    		}  
	        } else {
	            LOGGER.trace("Skipping focus change execute, because user delta is null");
	        }
    	}

        for (LensProjectionContext<P> accCtx : syncContext.getProjectionContexts()) {
        	if (accCtx.getWave() != syncContext.getExecutionWave()) {
        		continue;
			}
        	OperationResult subResult = result.createSubresult(OPERATION_EXECUTE_PROJECTION);
        	subResult.addContext("discriminator", accCtx.getResourceShadowDiscriminator());
			try {
				ObjectDelta<P> accDelta = accCtx.getExecutableDelta();
				if (accCtx.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
					if (syncContext.getFocusContext().getDelta() != null
							&& syncContext.getFocusContext().getDelta().isDelete()
							&& syncContext.getOptions() != null
							&& ModelExecuteOptions.isForce(syncContext.getOptions())) {
						if (accDelta == null) {
							accDelta = ObjectDelta.createDeleteDelta(accCtx.getObjectTypeClass(),
									accCtx.getOid(), prismContext);
						}
					}
					if (accDelta != null && accDelta.isDelete()) {

						executeDelta(accDelta, accCtx, syncContext, accCtx.getResource(), task, subResult);

						// accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.DELETE);
					}
				} else {
					if (accDelta == null || accDelta.isEmpty()) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("No change for account "
									+ accCtx.getResourceShadowDiscriminator());
							LOGGER.trace("Delta:\n{}", accDelta == null ? null : accDelta.dump());
						}
						if (focusContext != null) {
							updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task,
									subResult);
						}
						subResult.computeStatus();
						subResult.recordNotApplicableIfUnknown();
						continue;
					}

					executeDelta(accDelta, accCtx, syncContext, accCtx.getResource(), task, subResult);

				}

				if (focusContext != null) {
					updateAccountLinks(focusContext.getObjectNew(), focusContext, accCtx, task, subResult);
				}
				
				subResult.computeStatus();
				subResult.recordNotApplicableIfUnknown();
				
			} catch (SchemaException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectNotFoundException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ObjectAlreadyExistsException e) {
				// in his case we do not need to set account context as
				// broken, instead we need to restart projector for this
				// context to recompute new account or find out if the
				// account was already linked..
				subResult.recordFatalError(e);
				continue;
			} catch (CommunicationException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ConfigurationException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (SecurityViolationException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (ExpressionEvaluationException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			} catch (RuntimeException e) {
				subResult.recordFatalError(e);
				accCtx.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
				continue;
			}
		}
        
        // Result computation here needs to be slightly different
        result.computeStatusComposite();

    }

	private void recordFatalError(OperationResult subResult, OperationResult result, String message, Throwable e) {
		if (message == null) {
			message = e.getMessage();
		}
		subResult.recordFatalError(e);
		if (result != null) {
			result.computeStatusComposite();
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
        	LensObjectDeltaOperation<UserType> userDeltaOp = new LensObjectDeltaOperation<UserType>(userDelta);
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
        	LensObjectDeltaOperation<UserType> userDeltaOp = new LensObjectDeltaOperation<UserType>(userDelta);
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
			String changedOid = provisioning.modifyObject(AccountShadowType.class, accountRef,
					syncSituationDeltas, null, ProvisioningOperationOptions.createCompletePostponed(false),
					task, result);
//			modifyProvisioningObject(AccountShadowType.class, accountRef, syncSituationDeltas, ProvisioningOperationOptions.createCompletePostponed(false), task, result);
		} catch (ObjectNotFoundException ex) {
			// if the object not found exception is thrown, it's ok..probably
			// the account was deleted by previous execution of changes..just
			// log in the trace the message for the user.. 
			LOGGER.trace("Situation in account could not be updated. Account not found on the resource. Skipping modifying situation in account");
			return;
		} catch (Exception ex) {
            throw new SystemException(ex.getMessage(), ex);
        }
		// if everything is OK, add result of the situation modification to the
		// parent result
		result.recordSuccess();
		parentResult.addSubresult(result);
		
	}
    
	private <T extends ObjectType, F extends ObjectType, P extends ObjectType>
    	void executeDelta(ObjectDelta<T> objectDelta, LensElementContext<T> objectContext, LensContext<F,P> context,
    			ResourceType resource, Task task, OperationResult parentResult) 
    			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException,
    			ConfigurationException, SecurityViolationException, RewindException, ExpressionEvaluationException {
		
        if (objectDelta == null) {
            throw new IllegalArgumentException("Null change");
        }
        
        if (alreadyExecuted(objectDelta, objectContext)) {
        	LOGGER.debug("Skipping execution of delta because it was already executed: {}", objectContext);
        	return;
        }
        
        if (CONSISTENCY_CHECKS) objectDelta.checkConsistence();
        
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
	            executeAddition(objectDelta, context, provisioningOptions, resource, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
	        	executeModification(objectDelta, context, provisioningOptions, resource, task, result);
	        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
	            executeDeletion(objectDelta, context, provisioningOptions, resource, task, result);
	        }
	        
	        // To make sure that the OID is set (e.g. after ADD operation)
	        objectContext.setOid(objectDelta.getOid());
	        
    	} finally {
    		
    		result.computeStatus();
    		LensObjectDeltaOperation<T> objectDeltaOp = new LensObjectDeltaOperation<T>(objectDelta.clone());
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
	
	private <T extends ObjectType, F extends ObjectType, P extends ObjectType> boolean alreadyExecuted(
			ObjectDelta<T> objectDelta, LensElementContext<T> objectContext) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Checking for already executed delta:\n{}\nIn deltas:\n{}",
					objectDelta.dump(), DebugUtil.debugDump(objectContext.getExecutedDeltas()));
		}
		return ObjectDeltaOperation.containsDelta(objectContext.getExecutedDeltas(), objectDelta);
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

    private <T extends ObjectType, F extends ObjectType, P extends ObjectType> void executeAddition(ObjectDelta<T> change, 
    		LensContext<F, P> context, ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result) 
    				throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, 
    				ConfigurationException, SecurityViolationException, RewindException, ExpressionEvaluationException {

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
            oid = addProvisioningObject(objectToAdd, context, options, resource, task, result);
            if (oid == null) {
            	throw new SystemException("Provisioning addObject returned null OID while adding " + objectToAdd);
            }
            result.addReturn("createdAccountOid", oid);
        } else {
            oid = cacheRepositoryService.addObject(objectToAdd, null, result);
            if (oid == null) {
            	throw new SystemException("Repository addObject returned null OID while adding " + objectToAdd);
            }
        }
        change.setOid(oid);
    }

    private <T extends ObjectType, F extends ObjectType, P extends ObjectType> void executeDeletion(ObjectDelta<T> change, 
    		LensContext<F,P> context, ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result) throws
            ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        String oid = change.getOid();
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.deleteTask(oid, result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            deleteProvisioningObject(objectTypeClass, oid, context, options, resource, task, result);
        } else {
            cacheRepositoryService.deleteObject(objectTypeClass, oid, result);
        }
    }

    private <T extends ObjectType, F extends ObjectType, P extends ObjectType> void executeModification(ObjectDelta<T> change, 
    		LensContext<F, P> context, ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, RewindException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (change.isEmpty()) {
            // Nothing to do
            return;
        }
        Class<T> objectTypeClass = change.getObjectTypeClass();

        if (TaskType.class.isAssignableFrom(objectTypeClass)) {
            taskManager.modifyTask(change.getOid(), change.getModifications(), result);
        } else if (ObjectTypes.isClassManagedByProvisioning(objectTypeClass)) {
            String oid = modifyProvisioningObject(objectTypeClass, change.getOid(), change.getModifications(), context, options, resource, task, result);
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

    private <F extends ObjectType, P extends ObjectType> String addProvisioningObject(PrismObject<? extends ObjectType> object, 
    		LensContext<F, P> context, ProvisioningOperationOptions options, ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, RewindException, ExpressionEvaluationException {

        if (object.canRepresent(ResourceObjectShadowType.class)) {
            ResourceObjectShadowType shadow = (ResourceObjectShadowType) object.asObjectable();
            String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadow);
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is null in shadow");
            }
        }

        ProvisioningScriptsType scripts = prepareScripts(object, context, ProvisioningOperationTypeType.ADD, resource, result);
        String oid = provisioning.addObject(object, scripts, options, task, result);
        return oid;
    }

    private <F extends ObjectType, P extends ObjectType> void deleteProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
    		LensContext<F, P> context, ProvisioningOperationOptions options, ResourceType resource, Task task, 
            OperationResult result) throws ObjectNotFoundException, ObjectAlreadyExistsException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

    	PrismObject<? extends ObjectType> shadowToModify = provisioning.getObject(objectTypeClass, oid, GetOperationOptions.createNoFetch(), result);
    	ProvisioningScriptsType scripts = prepareScripts(shadowToModify, context, ProvisioningOperationTypeType.DELETE, resource, result);
        provisioning.deleteObject(objectTypeClass, oid, options, scripts, task, result);
    }

    private <F extends ObjectType, P extends ObjectType> String modifyProvisioningObject(Class<? extends ObjectType> objectTypeClass, String oid,
            Collection<? extends ItemDelta> modifications, LensContext<F, P> context, ProvisioningOperationOptions options, 
            ResourceType resource, Task task, OperationResult result) throws ObjectNotFoundException, RewindException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

    	PrismObject<? extends ObjectType> shadowToModify = provisioning.getObject(objectTypeClass, oid, GetOperationOptions.createNoFetch(), result);
    	ProvisioningScriptsType scripts = prepareScripts(shadowToModify, context, ProvisioningOperationTypeType.MODIFY, resource, result);
        String changedOid = provisioning.modifyObject(objectTypeClass, oid, modifications, scripts, options, task, result);
        return changedOid;
    }

    private <F extends ObjectType, P extends ObjectType> ProvisioningScriptsType prepareScripts(
    		PrismObject<? extends ObjectType> changedObject, LensContext<F, P> context, 
    		ProvisioningOperationTypeType operation, ResourceType resource, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	
    	if (!changedObject.canRepresent(ResourceObjectShadowType.class)) {
    		return null;
    	}
    	
    	ProvisioningScriptsType resourceScripts = resource.getScripts();
    	PrismObject<? extends ResourceObjectShadowType> resourceObject = (PrismObject<? extends ResourceObjectShadowType>) changedObject;
        
        PrismObject<F> user = null;
		if (context.getFocusContext() != null){
			if (context.getFocusContext().getObjectNew() != null){
			user = context.getFocusContext().getObjectNew();
			} else if (context.getFocusContext().getObjectNew() != null){
				user = context.getFocusContext().getObjectOld();
			}	
		}
        
        Map<QName, Object> variables = getDefaultExpressionVariables((PrismObject<UserType>) user, resourceObject, resource.asPrismObject());
        return evaluateScript(resourceScripts, operation, variables, result);
      
    }
//    
//	private <F extends ObjectType, P extends ObjectType> ProvisioningScriptsType getScripts(
//			String oid, LensContext<F, P> context, ProvisioningOperationTypeType operation,
//			OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
//			ConfigurationException, SecurityViolationException {
//		
//		PrismObject user = null;
//		if (context.getFocusContext() != null && context.getFocusContext().getClass().isAssignableFrom(UserType.class)){
//			if (context.getFocusContext().getObjectNew() != null){
//			user = context.getFocusContext().getObjectNew();
//			} else if (context.getFocusContext().getObjectNew() != null){
//				user = context.getFocusContext().getObjectOld();
//			}
//			
//		}
//		
//		LensProjectionContext projectionContext = context.findProjectionContextByOid(oid);
//		PrismObject<AccountShadowType> shadow = null;
//		if (projectionContext.getObjectNew() != null){
//			shadow = projectionContext.getObjectNew();
//		} else if (projectionContext.getObjectOld() != null){
//			shadow = projectionContext.getObjectOld();
//		}
//		
//		ResourceType resource = projectionContext.getResource();
//		
//		ProvisioningScriptsType resourceScripts = resource.getScripts();
//		
//		Map<QName, Object> variables = getDefaultXPathVariables(user, shadow, resource.asPrismObject());
//		
//		return evaluateScript(resourceScripts, operation, variables, result);
//	}
	
	
	private ProvisioningScriptsType evaluateScript(ProvisioningScriptsType resourceScripts, ProvisioningOperationTypeType operation, Map<QName, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
		  ProvisioningScriptsType outScripts = new ProvisioningScriptsType();
	        if (resourceScripts != null) {
	        	for (ProvisioningScriptType script: resourceScripts.getScript()) {
	        		if (script.getOperation().contains(operation)) {
	        			for (ProvisioningScriptArgumentType argument : script.getArgument()){
	        				evaluateScript(argument, variables, result);
	        			}
	        			outScripts.getScript().add(script);
	        		}
	        	}
	        }

	        return outScripts;
	}
    
    private void evaluateScript(ProvisioningScriptArgumentType argument, Map<QName, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException{
    	
    	QName FAKE_SCRIPT_ARGUMENT_NAME = new QName(SchemaConstants.NS_C, "arg");
    	
    	PrismPropertyDefinition scriptArgumentDefinition = new PrismPropertyDefinition(FAKE_SCRIPT_ARGUMENT_NAME,
				FAKE_SCRIPT_ARGUMENT_NAME, DOMUtil.XSD_STRING, prismContext);
    	
    	String shortDesc = "Provisioning script argument expression";
    	Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(argument, scriptArgumentDefinition, shortDesc, result);
    	
    	
    	ExpressionEvaluationParameters params = new ExpressionEvaluationParameters(null, variables, shortDesc, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(params);
		if (outputTriple == null) {
			return;
		}
		
		//replace dynamic script with static value..
		argument.getExpressionEvaluator().clear();
		Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
		for (PrismPropertyValue<String> val : nonNegativeValues){
			Element value = DOMUtil.createElement(SchemaConstants.C_VALUE);
			value.setTextContent(val.getValue());
			JAXBElement<Element> el = new JAXBElement(SchemaConstants.C_VALUE, Element.class, value);
			argument.getExpressionEvaluator().add(el);
		}
		
		return;
    }
    
    private Map<QName, Object> getDefaultExpressionVariables(PrismObject<UserType> user, 
    		PrismObject<? extends ResourceObjectShadowType> account, PrismObject<ResourceType> resource) {		
		Map<QName, Object> variables = new HashMap<QName, Object>();
		variables.put(SchemaConstants.I_USER, user);
		variables.put(SchemaConstants.I_ACCOUNT, account);
		variables.put(SchemaConstants.I_RESOURCE, resource);
		return variables;
	}

}
