/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import com.evolveum.midpoint.provisioning.ucf.api.async.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Takes care for listening on all (both active and passive) sources for a given connector.
 *
 * Responsibilities:
 *
 * 1. Maintains a list of passive source and listening activities on active sources.
 * 2. Kicks passive sources to send out their messages to the message listener.
 * 3. Monitors listening activities on active sources (they send their messages to the listener by themselves).
 * 4. Accepts configuration updates.
 * 5. Holds the control - in {@link #listenForChanges(ConnectorConfiguration, Supplier)} method - while there are open
 *     passive sources and/or any listening activities on active sources and until externally requested to be stopped.
 */
class ConnectorListener {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorListener.class);

    private static final long RUNNABLE_CHECK_INTERVAL = 100L;

    /** Async update connector to which this listener belongs. */
    @NotNull private final AsyncUpdateConnectorInstance connectorInstance;

    /** Locking object for both sources and activities. */
    @NotNull private final Object sourcesAndActivitiesLock = new Object();

    /**
     * Current list of all source (both active and passive).
     * Guarded by: sourcesAndActivitiesLock
     */
    @NotNull private final List<AsyncUpdateSource> sources = new ArrayList<>();

    /**
     * List of current listening activities (on active sources).
     * Guarded by: sourcesAndActivitiesLock
     */
    @NotNull private final List<ListeningActivity> activities = new ArrayList<>();

    /** "Output" message listener. Usually here is the transformational listener that transforms messages to UCF API level. */
    @NotNull private final AsyncUpdateMessageListener messageListener;

    /** Set to true when the listening should be stopped. Once stopped, it remains stopped. */
    private volatile boolean stopped;

    ConnectorListener(@NotNull AsyncUpdateConnectorInstance connectorInstance,
            @NotNull AsyncUpdateMessageListener messageListener) {
        this.connectorInstance = connectorInstance;
        this.messageListener = messageListener;
    }

    /**
     * Listens for changes and ensures they are sent to the declared {@link #messageListener}. Returns the control when
     * canRunSupplier indicates 'false' and/or no more listening takes place.
     *
     * @param configuration Current connector configuration, containing mainly the list of sources.
     * @param canRunSupplier Signals when there is an external request to stop the listening process.
     */
    void listenForChanges(ConnectorConfiguration configuration, Supplier<Boolean> canRunSupplier) throws SchemaException {

        if (stopped) {
            LOGGER.warn("The listener is stopped."); // TODO or throw an exception?
            return;
        }

        start(configuration);
        try {
            main:
            while (canContinue(canRunSupplier)) {

                // passive sources
                boolean anyMessage = false;
                boolean anySourceOpen = false;
                for (PassiveAsyncUpdateSource source : getPassiveSourcesCopy()) {
                    if (source.isOpen()) {
                        anySourceOpen = true;
                        if (source.getNextUpdate(messageListener)) {
                            anyMessage = true;
                        }
                    }
                    if (!canContinue(canRunSupplier)) {
                        break main;
                    }
                }

                // active sources are processed asynchronously; here we only check if their listening activities are alive
                for (ListeningActivity listeningActivity : getActivitiesCopy()) {
                    if (listeningActivity.isAlive()) {
                        anySourceOpen = true;
                    }
                }

                if (!anySourceOpen) {
                    LOGGER.info("All message sources are closed now, stopping the listening in {}", connectorInstance);
                    break;
                }

                if (!anyMessage) {
                    //noinspection BusyWait
                    Thread.sleep(RUNNABLE_CHECK_INTERVAL);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.debug("Got InterruptedException", e);
            LOGGER.info("Listening was interrupted");
        } finally {
            stop();
        }
    }

    private boolean canContinue(Supplier<Boolean> canRunSupplier) {
        if (!Boolean.TRUE.equals(canRunSupplier.get())) {
            LOGGER.info("Stopping listening for changes because canRun is not true any more");
            return false;
        } else if (stopped) {
            LOGGER.warn("Stopping listening for changes as requested to do so (maybe because another one was started in the meanwhile)");
            return false;
        } else {
            return true;
        }
    }

    private void start(ConnectorConfiguration configuration) throws SchemaException {
        LOGGER.info("Starting listening in {}", connectorInstance); // todo debug
        Collection<AsyncUpdateSource> sources = connectorInstance.getSourceManager().createSources(configuration.getAllSources());
        try {
            for (AsyncUpdateSource source : sources) {
                addSource(source);
                if (source instanceof ActiveAsyncUpdateSource) {
                    addActivity(((ActiveAsyncUpdateSource) source).startListening(messageListener));
                }
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Got unexpected exception while starting the listener(s). "
                    + "Stopping the listening. In: {}", t, connectorInstance);
            stop();
            throw t;
        }
    }

    private void addActivity(ListeningActivity activity) {
        synchronized (sourcesAndActivitiesLock) {
            activities.add(activity);
        }
    }

    private void addSource(AsyncUpdateSource source) {
        synchronized (sourcesAndActivitiesLock) {
            sources.add(source);
        }
    }

    /** Stops all listening activities. Closes all the sources. Permanently. */
    public void stop() {
        stopInternal(true);
    }

    /** Stops and restarts listening with updated configuration. */
    void restart(ConnectorConfiguration configuration) {
        try {
            stopInternal(false);
            start(configuration);
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't restart listening activity in {}", e, connectorInstance);
            stop();
        }
    }

    private void stopInternal(boolean permanently) {
        LOGGER.info("Stopping listening in {} (permanently={})", connectorInstance, permanently); // todo debug

        if (permanently) {
            stopped = true;
        }

        List<ListeningActivity> activitiesCopy;
        List<AsyncUpdateSource> sourcesCopy;
        synchronized (sourcesAndActivitiesLock) {
            activitiesCopy = new ArrayList<>(activities);
            sourcesCopy = new ArrayList<>(sources);
            activities.clear();
            sources.clear();
        }

        for (ListeningActivity activity : activitiesCopy) {
            try {
                activity.stop();
            } catch (RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop listening on {} in {}", e, activity, connectorInstance);
            }
        }
        for (AsyncUpdateSource source : sourcesCopy) {
            try {
                source.close();
            } catch (RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close source {} in {}", e, source, connectorInstance);
            }
        }
    }

    @Override
    public String toString() {
        return "ConnectorListener{" + activities + "}";
    }

    @NotNull
    private List<PassiveAsyncUpdateSource> getPassiveSourcesCopy() {
        synchronized (sourcesAndActivitiesLock) {
            return sources.stream()
                    .filter(s -> s instanceof PassiveAsyncUpdateSource)
                    .map(s -> (PassiveAsyncUpdateSource) s)
                    .collect(Collectors.toList());
        }
    }

    @NotNull
    private List<ListeningActivity> getActivitiesCopy() {
        synchronized (sourcesAndActivitiesLock) {
            return new ArrayList<>(activities);
        }
    }
}
