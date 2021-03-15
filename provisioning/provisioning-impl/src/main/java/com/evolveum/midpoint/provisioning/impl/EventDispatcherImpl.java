/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.evolveum.midpoint.provisioning.api.*;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Dispatcher for various kinds of events emitted by provisioning module.
 *
 * @author Radovan Semancik
 */
@Component
public class EventDispatcherImpl implements EventDispatcher {

    // just to make sure we don't grow indefinitely
    private static final int LISTENERS_SOFT_LIMIT = 200;
    private static final int LISTENERS_HARD_LIMIT = 2000;

    private static final long LISTENERS_WARNING_INTERVAL = 2000;

    /** Dispatcher for "resource object change" events. */
    private final ResourceObjectChangeDispatcher resourceObjectChangeDispatcher = new ResourceObjectChangeDispatcher();

    /** Dispatcher for "shadow death" events. */
    private final ShadowDeathEventDispatcher shadowDeathDispatcher = new ShadowDeathEventDispatcher();

    /** Dispatcher for "resource operation" events. */
    private final ResourceOperationDispatcher resourceOperationDispatcher = new ResourceOperationDispatcher();

    /** Dispatcher for external events about changes on resource. */
    private final ExternalResourceEventDispatcher externalResourceEventDispatcher = new ExternalResourceEventDispatcher();

    private static final Trace LOGGER = TraceManager.getTrace(EventDispatcherImpl.class);

    //region ResourceObjectChangeListener
    @Override
    public synchronized void registerListener(ResourceObjectChangeListener listener) {
        resourceObjectChangeDispatcher.registerListener(listener);
    }

    @Override
    public synchronized void unregisterListener(ResourceObjectChangeListener listener) {
        resourceObjectChangeDispatcher.unregisterListener(listener);
    }

    @Override
    public void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
        resourceObjectChangeDispatcher.notifyChange(change, task, parentResult);
    }

    private static class ResourceObjectChangeDispatcher
            extends AbstractDispatcher<ResourceObjectChangeListener> {

        public void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult result) {
            LOGGER.trace("SYNCHRONIZATION change notification\n{} ", change.debugDumpLazily());
            if (InternalsConfig.consistencyChecks) change.checkConsistence();
            notify(listener -> listener.notifyChange(change, task, result), result);
        }

        @Override
        boolean shouldRethrowExceptions() {
            return true; // TODO reconsider
        }
    }
    //endregion

    //region ShadowDeathListener
    @Override
    public synchronized void registerListener(ShadowDeathListener listener) {
        shadowDeathDispatcher.registerListener(listener);
    }

    @Override
    public synchronized void unregisterListener(ShadowDeathListener listener) {
        shadowDeathDispatcher.unregisterListener(listener);
    }

    @Override
    public void notify(@NotNull ShadowDeathEvent event, Task task, OperationResult result) {
        shadowDeathDispatcher.notify(event, task, result);
    }

    private static class ShadowDeathEventDispatcher
            extends AbstractDispatcher<ShadowDeathListener> {

        public void notify(@NotNull ShadowDeathEvent event, Task task, OperationResult result) {
            LOGGER.trace("SHADOW DEATH change notification\n{} ", event.debugDumpLazily());
            notify(listener -> listener.notify(event, task, result), result);
        }

        @Override
        boolean shouldRethrowExceptions() {
            return true;
        }
    }
    //endregion

    //region ResourceOperationListener
    @Override
    public synchronized void registerListener(ResourceOperationListener listener) {
        resourceOperationDispatcher.registerListener(listener);
    }

    @Override
    public synchronized void unregisterListener(ResourceOperationListener listener) {
        resourceOperationDispatcher.unregisterListener(listener);
    }

    @Override
    public void notifySuccess(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
        resourceOperationDispatcher.notifySuccess(event, task, result);
    }

    @Override
    public void notifyFailure(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
        resourceOperationDispatcher.notifyFailure(event, task, result);
    }

    @Override
    public void notifyInProgress(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
        resourceOperationDispatcher.notifyInProgress(event, task, result);
    }

    private static class ResourceOperationDispatcher extends AbstractDispatcher<ResourceOperationListener> {
        void notifySuccess(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
            LOGGER.trace("Resource operation success notification\n{} ", event.debugDumpLazily());
            event.checkConsistence();
            notify(listener -> listener.notifySuccess(event, task, result), result);
        }

        void notifyFailure(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
            LOGGER.trace("Resource operation failure notification\n{} ", event.debugDumpLazily());
            event.checkConsistence();
            notify(listener -> listener.notifyFailure(event, task, result), result);
        }

        void notifyInProgress(@NotNull ResourceOperationDescription event, Task task, OperationResult result) {
            LOGGER.trace("Resource operation in-progress notification\n{} ", event.debugDumpLazily());
            event.checkConsistence();
            notify(listener -> listener.notifyInProgress(event, task, result), result);
        }
    }
    //endregion

    //region ResourceExternalEventListener
    @Override
    public synchronized void registerListener(ExternalResourceEventListener listener) {
        externalResourceEventDispatcher.registerListener(listener);
    }

    @Override
    public synchronized void unregisterListener(ExternalResourceEventListener listener) {
        externalResourceEventDispatcher.unregisterListener(listener);
    }

    @Override
    public void notifyEvent(ExternalResourceEvent event, Task task, OperationResult result) {
        externalResourceEventDispatcher.notifyEvent(event, task, result);
    }

    private static class ExternalResourceEventDispatcher extends AbstractDispatcher<ExternalResourceEventListener> {

        @SuppressWarnings("FieldCanBeLocal") // TODO implement setter if needed, or remove altogether
        private final boolean filterProtectedObjects = true;

        void notifyEvent(ExternalResourceEvent event, Task task, OperationResult result) {
            LOGGER.trace("SYNCHRONIZATION change notification\n{} ", event.debugDumpLazily());
            // if (InternalsConfig.consistencyChecks) eventDescription.checkConsistence(); // TODO why disabled?
            if (filterProtectedObjects && event.isProtected()) {
                LOGGER.trace("Skipping dispatching of {} because it is protected", event);
                return;
            }
            notify(listener -> listener.notifyEvent(event, task, result), result);
        }

        @Override
        boolean shouldRethrowExceptions() {
            return true; // This has to be present because we want to signal the exception to the caller
        }
    }
    //endregion

    @Override
    public String getName() {
        return "object change notification dispatcher";
    }

    private static abstract class AbstractDispatcher<L extends ProvisioningListener> {

        // use synchronized access only!
        private final List<L> listeners = new ArrayList<>();

        private long lastListenersWarning = 0;

        synchronized void registerListener(L listener) {
            Validate.notNull(listener);

            if (listeners.contains(listener)) {
                LOGGER.warn("Listener '{}' is already registered. Subsequent registration is ignored", listener);
            } else {
                listeners.add(listener);
                checkListenersCount();
            }
        }

        private void checkListenersCount() {
            String name = getClass().getSimpleName();
            int size = listeners.size();
            LOGGER.trace("Listeners in '{}': {}", name, size);
            if (size > LISTENERS_HARD_LIMIT) {
                throw new IllegalStateException("Possible listeners leak: number of listeners in " + name +
                        " exceeded the threshold of " + LISTENERS_SOFT_LIMIT);
            } else if (size > LISTENERS_SOFT_LIMIT && System.currentTimeMillis() - lastListenersWarning >= LISTENERS_WARNING_INTERVAL) {
                LOGGER.warn("Too many listeners in '{}': {} (soft limit: {}, hard limit: {})", name, size,
                        LISTENERS_SOFT_LIMIT, LISTENERS_HARD_LIMIT);
                lastListenersWarning = System.currentTimeMillis();
            }
        }

        synchronized void unregisterListener(L listener) {
            listeners.remove(listener);
        }

        private synchronized List<L> getListenersSnapshot() {
            return new ArrayList<>(listeners);
        }

        void notify(Consumer<L> notifier, OperationResult result) {
            List<L> listenersSnapshot = getListenersSnapshot();
            if (!listenersSnapshot.isEmpty()) {
                for (L listener : listenersSnapshot) {
                    try {
                        if (listener == null) {
                            throw new IllegalStateException("Change listener is null");
                        }
                        notifier.accept(listener);
                    } catch (RuntimeException e) {
                        LOGGER.error("Exception {} thrown by listener {} in {}: {}", e.getClass(),
                                listener != null ? listener.getName() : "(null)", getClass().getSimpleName(), e.getMessage(), e);
                        result.createSubresult(getClass().getName() + "notify")
                                .recordWarning("Listener has thrown unexpected exception", e);
                        if (shouldRethrowExceptions()) {
                            throw e;
                        }
                    }
                }
            } else {
                LOGGER.warn("Event in {} received but listener list is empty, there is nobody to get the message",
                        getClass().getSimpleName());
            }
        }

        boolean shouldRethrowExceptions() {
            return false;
        }
    }
}
