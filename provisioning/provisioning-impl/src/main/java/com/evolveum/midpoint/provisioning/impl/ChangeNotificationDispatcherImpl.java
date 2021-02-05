/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.exception.*;

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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class ChangeNotificationDispatcherImpl implements ChangeNotificationDispatcher {

    // just to make sure we don't grow indefinitely
    private static final int LISTENERS_SOFT_LIMIT = 200;
    private static final int LISTENERS_HARD_LIMIT = 2000;

    private static final long LISTENERS_WARNING_INTERVAL = 2000;

    private long lastListenersWarning = 0;

    private static final String CLASS_NAME_WITH_DOT = ChangeNotificationDispatcherImpl.class.getName() + ".";

    private boolean filterProtectedObjects = true;
    private final List<ResourceObjectChangeListener> changeListeners = new ArrayList<>();     // use synchronized access only!
    private final List<ResourceOperationListener> operationListeners = new ArrayList<>();
    private final List<ResourceEventListener> eventListeners = new ArrayList<>();

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
        Validate.notNull(listener);

        if (changeListeners.contains(listener)) {
            LOGGER.warn(
                    "Resource object change listener '{}' is already registered. Subsequent registration is ignored",
                    listener);
        } else {
            changeListeners.add(listener);
            checkSize(changeListeners, "changeListeners");
        }
    }

    private void checkSize(List<?> listeners, String name) {
        int size = listeners.size();
        LOGGER.trace("Listeners of type '{}': {}", name, size);
        if (size > LISTENERS_HARD_LIMIT) {
            throw new IllegalStateException("Possible listeners leak: number of " + name + " exceeded the threshold of " + LISTENERS_SOFT_LIMIT);
        } else if (size > LISTENERS_SOFT_LIMIT && System.currentTimeMillis() - lastListenersWarning >= LISTENERS_WARNING_INTERVAL) {
            LOGGER.warn("Too many listeners of type '{}': {} (soft limit: {}, hard limit: {})", name, size,
                    LISTENERS_SOFT_LIMIT, LISTENERS_HARD_LIMIT);
            lastListenersWarning = System.currentTimeMillis();
        }
    }

    private synchronized <T> List<T> getListenersSnapshot(List<T> listeners) {
        if (listeners != null) {
            return new ArrayList<>(listeners);
        } else {
            return new ArrayList<>();
        }
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager#registerNotificationListener(com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener)
     */
    @Override
    public synchronized void registerNotificationListener(ResourceOperationListener listener) {
        Validate.notNull(listener);

        if (operationListeners.contains(listener)) {
            LOGGER.warn(
                    "Resource operation listener '{}' is already registered. Subsequent registration is ignored",
                    listener);
        } else {
            operationListeners.add(listener);
            checkSize(operationListeners, "operationListeners");
        }

    }
    @Override
    public synchronized void registerNotificationListener(ResourceEventListener listener) {
        Validate.notNull(listener);

        if (eventListeners.contains(listener)) {
            LOGGER.warn(
                    "Resource event listener '{}' is already registered. Subsequent registration is ignored",
                    listener);
        } else {
            eventListeners.add(listener);
            checkSize(eventListeners, "eventListeners");
        }

    }

    @Override
    public synchronized void unregisterNotificationListener(ResourceEventListener listener) {
        eventListeners.remove(listener);

    }

    @Override
    public synchronized void unregisterNotificationListener(ResourceOperationListener listener) {
        operationListeners.remove(listener);
    }

    @Override
    public synchronized void unregisterNotificationListener(ResourceObjectChangeListener listener) {
        changeListeners.remove(listener);
    }

    @Override
    public void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
        Validate.notNull(change, "Change description of resource object shadow must not be null.");

        LOGGER.trace("SYNCHRONIZATION change notification\n{} ", change.debugDumpLazily());

        if (InternalsConfig.consistencyChecks) change.checkConsistence();

        // sometimes there is registration/deregistration from within
        List<ResourceObjectChangeListener> changeListenersSnapshot = getListenersSnapshot(changeListeners);
        if (!changeListenersSnapshot.isEmpty()) {
            for (ResourceObjectChangeListener listener : changeListenersSnapshot) {
                try {
                    if (listener == null) {
                        throw new IllegalStateException("Change listener is null");
                    }
                    listener.notifyChange(change, task, parentResult);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception {} thrown by object change listener {}: {}", e.getClass(),
                            listener != null ? listener.getName() : "(null)", e.getMessage(), e);
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyChange")
                            .recordWarning("Change listener has thrown unexpected exception", e);
                    throw e;
                }
            }
        } else {
            LOGGER.warn("Change notification received but listener list is empty, there is nobody to get the message");
        }
    }

    @Override
    public void notifyFailure(ResourceOperationDescription failureDescription, Task task, OperationResult parentResult) {
        Validate.notNull(failureDescription, "Operation description of resource object shadow must not be null.");

        LOGGER.trace("Resource operation failure notification\n{} ", failureDescription.debugDumpLazily());

        failureDescription.checkConsistence();

        List<ResourceOperationListener> operationListenersSnapshot = getListenersSnapshot(operationListeners);
        if (!operationListenersSnapshot.isEmpty()) {
            for (ResourceOperationListener listener : operationListenersSnapshot) {
                //LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
                try {
                    listener.notifyFailure(failureDescription, task, parentResult);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception {} thrown by operation failure listener {}: {}-{}", e.getClass(), listener.getName(), e.getMessage(), e);
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyFailure")
                            .recordWarning("Operation failure listener has thrown unexpected exception", e);
                }
            }
        } else {
            LOGGER.debug("Operation failure received but listener list is empty, there is nobody to get the message");
        }
    }

    @Override
    public void notifySuccess(ResourceOperationDescription successDescription, Task task, OperationResult parentResult) {
        Validate.notNull(successDescription, "Operation description of resource object shadow must not be null.");

        LOGGER.trace("Resource operation success notification\n{} ", successDescription.debugDumpLazily());

        successDescription.checkConsistence();

        List<ResourceOperationListener> operationListenersSnapshot = getListenersSnapshot(operationListeners);
        if (!operationListenersSnapshot.isEmpty()) {
            for (ResourceOperationListener listener : operationListenersSnapshot) {
                //LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
                try {
                    listener.notifySuccess(successDescription, task, parentResult);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception {} thrown by operation success listener {}: {}-{}", e.getClass(), listener.getName(), e.getMessage(), e);
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifySuccess")
                            .recordWarning("Operation success listener has thrown unexpected exception", e);
                }
            }
        } else {
            LOGGER.debug("Operation success received but listener list is empty, there is nobody to get the message");
        }
    }

    @Override
    public void notifyInProgress(ResourceOperationDescription inProgressDescription,
            Task task, OperationResult parentResult) {
        Validate.notNull(inProgressDescription, "Operation description of resource object shadow must not be null.");

        LOGGER.trace("Resource operation in-progress notification\n{} ", inProgressDescription.debugDumpLazily());

        inProgressDescription.checkConsistence();

        List<ResourceOperationListener> operationListenersSnapshot = getListenersSnapshot(operationListeners);
        if (!operationListenersSnapshot.isEmpty()) {
            for (ResourceOperationListener listener : operationListenersSnapshot) {
                //LOGGER.trace("Listener: {}", listener.getClass().getSimpleName());
                try {
                    listener.notifyInProgress(inProgressDescription, task, parentResult);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception {} thrown by operation in-progress listener {}: {}-{}", e.getClass(), listener.getName(), e.getMessage(), e);
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyInProgress")
                            .recordWarning("Operation in-progress listener has thrown unexpected exception", e);
                }
            }
        } else {
            LOGGER.debug("Operation in-progress received but listener list is empty, there is nobody to get the message");
        }
    }

    @Override
    public String getName() {
        return "object change notification dispatcher";
    }

    @Override
    public void notifyEvent(ResourceEventDescription eventDescription,
            Task task, OperationResult parentResult) throws CommonException, GenericConnectorException {
        Validate.notNull(eventDescription, "Event description must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SYNCHRONIZATION change notification\n{} ", eventDescription.debugDump());
        }

        if (filterProtectedObjects && eventDescription.isProtected()) {
            LOGGER.trace("Skipping dispatching of {} because it is protected", eventDescription);
            return;
        }

//        if (InternalsConfig.consistencyChecks) eventDescription.checkConsistence();

        List<ResourceEventListener> eventListenersSnapshot = getListenersSnapshot(eventListeners);
        if (!eventListenersSnapshot.isEmpty()) {
            for (ResourceEventListener listener : eventListenersSnapshot) {
                try {
                    listener.notifyEvent(eventDescription, task, parentResult);
                } catch (RuntimeException e) {
                    LOGGER.error("Exception {} thrown by event listener {}: {}-{}", e.getClass(), listener.getName(), e.getMessage(), e);
                    parentResult.createSubresult(CLASS_NAME_WITH_DOT + "notifyEvent")
                            .recordWarning("Event listener has thrown unexpected exception", e);
                    throw e;
                }
            }
        } else {
            LOGGER.warn("Event notification received but listener list is empty, there is nobody to get the message");
        }
    }
}
