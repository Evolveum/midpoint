/**
 * Copyright (c) 2011 Evolveum
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.sync;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Iterative search result handler for account synchronization. Works both for
 * reconciliation and import from resource.
 * 
 * This class is called back from the searchObjectsIterative() operation of the
 * provisioning service. It does most of the work of the "import" and resource
 * reconciliation operations.
 * 
 * @see ImportAccountsFromResourceTaskHandler
 * @see ReconciliationTaskHandler
 * 
 * @author Radovan Semancik
 * 
 */
public class SynchronizeAccountResultHandler implements ResultHandler {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizeAccountResultHandler.class);
	
	private ResourceObjectChangeListener objectChangeListener;
	private Task task;
	private ResourceType resource;
	private RefinedAccountDefinition refinedAccountDefinition;
	private QName sourceChannel;
	private String processShortName;
	private long progress;
	private long errors;
	private boolean stopOnError;
	private boolean forceAdd;

	public SynchronizeAccountResultHandler(ResourceType resource, RefinedAccountDefinition refinedAccountDefinition, Task task,
			ResourceObjectChangeListener objectChangeListener) {
		this.objectChangeListener = objectChangeListener;
		this.task = task;
		this.resource = resource;
		this.refinedAccountDefinition = refinedAccountDefinition;
		progress = 0;
		errors = 0;
		stopOnError = true;
		forceAdd = false;
		processShortName = "synchronization";
	}

	public boolean isForceAdd() {
		return forceAdd;
	}

	public void setForceAdd(boolean forceAdd) {
		this.forceAdd = forceAdd;
	}
	
	public QName getSourceChannel() {
		return sourceChannel;
	}

	public void setSourceChannel(QName sourceChannel) {
		this.sourceChannel = sourceChannel;
	}
	
	public String getProcessShortName() {
		return processShortName;
	}
	
	private String getProcessShortNameCapitalized() {
		return StringUtils.capitalize(processShortName);
	}

	public void setProcessShortName(String processShortName) {
		this.processShortName = processShortName;
	}

	/*
	 * This methods will be called for each search result. It means it will be
	 * called for each account on a resource. We will pretend that the account
	 * was created and invoke notification interface.
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ResultHandler#handle(com.evolveum
	 * .midpoint.xml.ns._public.common.common_1.ObjectType)
	 */
	@Override
	public boolean handle(ObjectType object, OperationResult parentResult) {
		if (object.getOid() == null) {
			throw new IllegalArgumentException("Object has null OID");
		}
		
		progress++;

		long startTime = System.currentTimeMillis();
		OperationResult result = parentResult.createSubresult(SynchronizeAccountResultHandler.class
				.getName() + ".handle");
		result.addParam("object", object);
		result.addContext(OperationResult.CONTEXT_PROGRESS, progress);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{} {} from {}",new Object[] {
					getProcessShortNameCapitalized(), ObjectTypeUtil.toShortString(object),ObjectTypeUtil.toShortString(resource)});
		}

		if (objectChangeListener == null) {
			LOGGER.warn("No object change listener set for {} task, ending the task", getProcessShortName());
			result.recordFatalError("No object change listener set for "+getProcessShortName()+" task, ending the task");
			return false;
		}

		// We are going to pretend that all of the objects were just created.
		// That will efficiently import them to the IDM repository

		try {
			AccountShadowType newShadow = (AccountShadowType) object;
	
			ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
			change.setSourceChannel(QNameUtil.qNameToUri(sourceChannel));
			change.setResource(resource);
			
			if (forceAdd) {
				// We should provide shadow in the state before the change. But we are
				// pretending that it has
				// not existed before, so we will not provide it.
				ObjectDelta<AccountShadowType> shadowDelta = new ObjectDelta<AccountShadowType>(
						AccountShadowType.class, ChangeType.ADD);
				MidPointObject<AccountShadowType> shadowToAdd = refinedAccountDefinition.getObjectDefinition().parseObjectType(newShadow);
				shadowDelta.setObjectToAdd(shadowToAdd);
				shadowDelta.setOid(newShadow.getOid());
				change.setObjectDelta(shadowDelta);
				
			} else {
				// No change, therefore the delta stays null. But we will set the current
				change.setCurrentShadow(newShadow);
			}

// No need to log. The notification dispatcher will log it all right
//			if (LOGGER.isDebugEnabled()) {
//				LOGGER.trace("{}: going to call notification with: {}", getProcessShortNameCapitalized(), change.dump());
//			}
			
			try {
				change.assertCorrectness();
			} catch (RuntimeException ex) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Change:\n{}",change.dump());
				}
				throw ex;
			}
			
			// Invoke the change notification
			objectChangeListener.notifyChange(change, task, result);
			long endTime = System.currentTimeMillis();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("{} object {} from resource {} done ({} ms)",new Object[]{
						getProcessShortNameCapitalized(), ObjectTypeUtil.toShortString(newShadow),
						ObjectTypeUtil.toShortString(resource), endTime - startTime});
			}
		} catch (Exception ex) {
			errors++;
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("{} of object {} from resource {} failed: {}", new Object[] {
						getProcessShortNameCapitalized(),
					ObjectTypeUtil.toShortString(object), ObjectTypeUtil.toShortString(resource), ex.getMessage(), ex });
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Change notication listener failed for {} of object {}: {}: ", new Object[] {
						getProcessShortName(),
						object, ex.getClass().getSimpleName(), ex.getMessage(), ex });
			}
			result.recordPartialError("failed to synchronize: "+ex.getMessage(), ex);
			return !isStopOnError();
		}
		
		// FIXME: hack. Hardcoded ugly summarization of successes
		if (result.isSuccess()) {
			result.getSubresults().clear();
		}

		// Check if we haven't been interrupted
		if (task.canRun()) {
			result.computeStatus();
			// Everything OK, signal to continue
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} of {} finished, result: {}", new Object[]{
						getProcessShortNameCapitalized(), ObjectTypeUtil.toShortString(object),result.dump()});
			}
			return true;
		} else {
			result.recordPartialError("Interrupted");
			// Signal to stop
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("{} from {} interrupted",new Object[]{
						getProcessShortNameCapitalized(),ObjectTypeUtil.toShortString(resource)});
			}
			return false;
		}
	}

	public long heartbeat() {
		// If we exist then we run. So just return the progress count.
		return progress;
	}

	public long getProgress() {
		return progress;
	}
	
	public long getErrors() {
		return errors;
	}

	/**
	 * Get the value of stopOnError
	 * 
	 * @return the value of stopOnError
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * Set the value of stopOnError
	 * 
	 * @param stopOnError
	 *            new value of stopOnError
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

}
