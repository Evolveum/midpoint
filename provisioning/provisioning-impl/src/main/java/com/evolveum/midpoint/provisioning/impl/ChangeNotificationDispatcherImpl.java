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
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class ChangeNotificationDispatcherImpl implements ChangeNotificationDispatcher {
	
	private List<ResourceObjectChangeListener> changeListeners = new ArrayList<ResourceObjectChangeListener>();
	private List<ResourceOperationListener> operationListeners = new ArrayList<ResourceOperationListener>();
	
	private static final Trace LOGGER = TraceManager.getTrace(ChangeNotificationDispatcherImpl.class);
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#registerNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void registerNotificationListener(ResourceObjectChangeListener listener) {
		if (changeListeners.contains(listener)) {
			LOGGER.warn(
					"Resource object change listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			changeListeners.add(listener);
		}

	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#registerNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void registerNotificationListener(ResourceOperationListener listener) {
		if (operationListeners.contains(listener)) {
			LOGGER.warn(
					"Resource object change listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			operationListeners.add(listener);
		}

	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#unregisterNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void unregisterNotificationListener(ResourceOperationListener listener) {
		changeListeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#unregisterNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
	 */
	@Override
	public synchronized void unregisterNotificationListener(ResourceObjectChangeListener listener) {
		operationListeners.remove(listener);
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyChange(com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowChangeDescriptionType, com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
		Validate.notNull(change, "Change description of resource object shadow must not be null.");
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION change notification\n{} ", change.dump());
		}
		
		if (InternalsConfig.consistencyChecks) change.checkConsistence();
		
		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceObjectChangeListener listener : changeListeners) {
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyChange(change, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by object change listener {}: {}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyChange").recordWarning("Change listener has thrown unexpected exception", e);
                    throw e;
				}
			}
		} else {
			LOGGER.warn("Change notification received but listener list is empty, there is nobody to get the message");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyFailure(ResourceOperationDescription failureDescription,
			Task task, OperationResult parentResult) {
		Validate.notNull(failureDescription, "Operation description of resource object shadow must not be null.");
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Resource operation failure notification\n{} ", failureDescription.dump());
		}
		
		failureDescription.checkConsistence();
		
		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : operationListeners) {
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyFailure(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation failure listener {}: {}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyFailure").recordWarning("Operation failure listener has thrown unexpected exception", e);
				}
			}
		} else {
			LOGGER.debug("Operation failure received but listener list is empty, there is nobody to get the message");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifySuccess(ResourceOperationDescription failureDescription,
			Task task, OperationResult parentResult) {
		Validate.notNull(failureDescription, "Operation description of resource object shadow must not be null.");
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Resource operation success notification\n{} ", failureDescription.dump());
		}
		
		failureDescription.checkConsistence();
		
		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : operationListeners) {
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifySuccess(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation success listener {}: {}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifySuccess").recordWarning("Operation success listener has thrown unexpected exception", e);
				}
			}
		} else {
			LOGGER.debug("Operation success received but listener list is empty, there is nobody to get the message");
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyInProgress(ResourceOperationDescription failureDescription,
			Task task, OperationResult parentResult) {
		Validate.notNull(failureDescription, "Operation description of resource object shadow must not be null.");
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Resource operation in-progress notification\n{} ", failureDescription.dump());
		}
		
		failureDescription.checkConsistence();
		
		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : operationListeners) {
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyInProgress(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation in-progress listener {}: {}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyInProgress").recordWarning("Operation in-progress listener has thrown unexpected exception", e);
				}
			}
		} else {
			LOGGER.debug("Operation in-progress received but listener list is empty, there is nobody to get the message");
		}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName()
	 */
	@Override
	public String getName() {
		return "object change notification dispatcher";
	}

}
