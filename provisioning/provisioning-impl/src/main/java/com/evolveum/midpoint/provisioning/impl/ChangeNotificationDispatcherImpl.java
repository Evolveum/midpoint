/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class ChangeNotificationDispatcherImpl implements ChangeNotificationDispatcher {

	private boolean filterProtectedObjects = true;
	private List<ResourceObjectChangeListener> changeListeners = new ArrayList<>();
	private List<ResourceOperationListener> operationListeners = new ArrayList<>();
	private List<ResourceEventListener> eventListeners = new ArrayList<>();

	private static final Trace LOGGER = TraceManager.getTrace(ChangeNotificationDispatcherImpl.class);

	public boolean isFilterProtectedObjects() {
		return filterProtectedObjects;
	}

	public void setFilterProtectedObjects(boolean filterProtectedObjects) {
		this.filterProtectedObjects = filterProtectedObjects;
	}

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
					"Resource operation listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			operationListeners.add(listener);
		}

	}
	@Override
	public synchronized void registerNotificationListener(ResourceEventListener listener) {
		if (eventListeners.contains(listener)) {
			LOGGER.warn(
					"Resource event listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			eventListeners.add(listener);
		}

	}

	@Override
	public void unregisterNotificationListener(ResourceEventListener listener) {
		eventListeners.remove(listener);

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


	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
		Validate.notNull(change, "Change description of resource object shadow must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION change notification\n{} ", change.debugDump());
		}

		if (InternalsConfig.consistencyChecks) change.checkConsistence();

		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceObjectChangeListener listener : new ArrayList<>(changeListeners)) {		// sometimes there is registration/deregistration from within
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyChange(change, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by object change listener {}: {}", e.getClass(), listener.getName(),
							e.getMessage(), e);
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
			LOGGER.trace("Resource operation failure notification\n{} ", failureDescription.debugDump());
		}

		failureDescription.checkConsistence();

		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : new ArrayList<>(operationListeners)) {		// sometimes there is registration/deregistration from within
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyFailure(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation failure listener {}: {}-{}", new Object[]{
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
			LOGGER.trace("Resource operation success notification\n{} ", failureDescription.debugDump());
		}

		failureDescription.checkConsistence();

		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : new ArrayList<>(operationListeners)) {		// sometimes there is registration/deregistration from within
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifySuccess(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation success listener {}: {}-{}", new Object[]{
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
			LOGGER.trace("Resource operation in-progress notification\n{} ", failureDescription.debugDump());
		}

		failureDescription.checkConsistence();

		if ((null != changeListeners) && (!changeListeners.isEmpty())) {
			for (ResourceOperationListener listener : new ArrayList<>(operationListeners)) {		// sometimes there is registration/deregistration from within
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyInProgress(failureDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by operation in-progress listener {}: {}-{}", new Object[]{
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

	@Override
	public void notifyEvent(ResourceEventDescription eventDescription,
			Task task, OperationResult parentResult) throws SchemaException,
			CommunicationException, ConfigurationException,
			SecurityViolationException, ObjectNotFoundException,
			GenericConnectorException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		Validate.notNull(eventDescription, "Event description must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("SYNCHRONIZATION change notification\n{} ", eventDescription.debugDump());
		}

		if (filterProtectedObjects && eventDescription.isProtected()) {
			LOGGER.trace("Skipping dispatching of {} because it is protected", eventDescription);
			return;
		}

//		if (InternalsConfig.consistencyChecks) eventDescription.checkConsistence();

		if ((null != eventListeners) && (!eventListeners.isEmpty())) {
			for (ResourceEventListener listener : new ArrayList<>(eventListeners)) {			// sometimes there is registration/deregistration from within
				//LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
				try {
					listener.notifyEvent(eventDescription, task, parentResult);
				} catch (RuntimeException e) {
					LOGGER.error("Exception {} thrown by event listener {}: {}-{}", new Object[]{
							e.getClass(), listener.getName(), e.getMessage(), e });
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyEvent").recordWarning("Event listener has thrown unexpected exception", e);
                    throw e;
				}
			}
		} else {
			LOGGER.warn("Event notification received but listener list is empty, there is nobody to get the message");
		}

	}


}
