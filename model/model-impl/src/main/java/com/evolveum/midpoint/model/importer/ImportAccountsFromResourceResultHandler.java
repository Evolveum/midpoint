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
package com.evolveum.midpoint.model.importer;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Iterative search result handler for "import from resource" task.
 * 
 * This class is called back from the searchObjectsIterative() operation of the
 * provisioning service. It does most of the work of the "import" operation
 * 
 * It will actually setup the "create" change notification for all objects and
 * invoke a ResourceObjectChangeListener interface.
 * 
 * @see ImportAccountsFromResourceTaskHandler
 * 
 * @author Radovan Semancik
 * 
 */
public class ImportAccountsFromResourceResultHandler implements ResultHandler {

	private ResourceObjectChangeListener objectChangeListener;
	private Task task;
	private ResourceType resource;
	private long progress;
	private boolean stopOnError;

	private static final Trace logger = TraceManager.getTrace(ImportAccountsFromResourceResultHandler.class);

	public ImportAccountsFromResourceResultHandler(ResourceType resource, Task task,
			ResourceObjectChangeListener objectChangeListener) {
		super();
		this.objectChangeListener = objectChangeListener;
		this.task = task;
		this.resource = resource;
		progress = 0;
		stopOnError = true;
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
		progress++;

		OperationResult result = parentResult.createSubresult(ImportAccountsFromResourceResultHandler.class
				.getName() + ".handle");
		result.addParam("object", object);
		result.addContext(OperationResult.CONTEXT_PROGRESS, progress);

		if (objectChangeListener == null) {
			logger.warn("No object change listener set for import task, ending the task");
			result.recordFatalError("No object change listener set for import task, ending the task");
			return false;
		}

		// We are going to pretend that all of the objects were just created.
		// That will efficiently import them to the IDM repository

		// TODO: Error handling
		ResourceObjectShadowType newShadow = (ResourceObjectShadowType) object;

		ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_IMPORT));
		change.setResource(resource);
		ObjectChangeAdditionType addChange = new ObjectChangeAdditionType();
		addChange.setObject(newShadow);
		change.setObjectChange(addChange);
		// We should provide shadow in the state before the change. But we are
		// pretending that it has
		// not existed before, so we will not provide it.

		logger.debug("Going to call notification with new object: " + DebugUtil.prettyPrint(newShadow));
		try {

			// Invoke the change notification
			objectChangeListener.notifyChange(change, result);

		} catch (Exception ex) {
			logger.error("Change notication listener failed for import of object {}: {}: ", new Object[] {
					newShadow, ex.getClass().getSimpleName(), ex.getMessage(), ex });
			result.recordPartialError("failed to import", ex);
			return !isStopOnError();
		}

		// Check if we haven't been interrupted
		if (task.canRun()) {
			result.computeStatus("Failed to import.");
			// Everything OK, signal to continue
			return true;
		} else {
			result.recordPartialError("Interrupted");
			// Signal to stop
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
