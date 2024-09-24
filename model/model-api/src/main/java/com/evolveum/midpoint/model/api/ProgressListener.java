/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.model.api.context.ModelContext;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.CLOCKWORK;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * An interface that model uses to report operation progress to any interested party (e.g. GUI or WS client).
 * Useful for long-running operations, like provisioning a focus object with many projections.
 */
public interface ProgressListener {

    /**
     * Reports a progress achieved. The idea is to provide as much information as practically possible,
     * so the client could take whatever it wants.
     *
     * Obviously, the method should not take too much time in order not to slow down the main execution routine.
     *
     * @param modelContext Current context of the model operation.
     * @param progressInformation Specific progress information.
     */
    void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation);

    boolean isAbortRequested();

    /** "Installs" a listener: creates a list of listeners consisting of existing ones plus a new one. */
    public static @NotNull Collection<ProgressListener> add(Collection<ProgressListener> listeners, ProgressListener newOne) {
        var newList = new ArrayList<>(MiscUtil.emptyIfNull(listeners));
        newList.add(newOne);
        return newList;
    }

    /** Just collects model contexts as they are reported by the clockwork. */
    class Collecting implements ProgressListener {

        private ModelContext<?> lastModelContext;
        private ProgressInformation lastProgressInformation;

        @Override
        public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
            lastModelContext = modelContext;
            lastProgressInformation = progressInformation;
        }

        /** Returns the model context, while checking it is really the expected (final) one. */
        public @NotNull ModelContext<?> getFinalContext() {
            // Some sanity checks, to avoid providing misleading information if the listeners are not working as we expect
            stateCheck(lastProgressInformation != null, "No progress information was collected");
            stateCheck(lastProgressInformation.getActivityType() == CLOCKWORK,
                    "Last progress information type is not CLOCKWORK: %s", lastProgressInformation);
            stateCheck(lastProgressInformation.getStateType() == EXITING,
                    "Last progress information is not EXITING: %s", lastProgressInformation);
            stateCheck(lastModelContext != null, "No model context was collected");
            return lastModelContext;
        }

        @Override
        public boolean isAbortRequested() {
            return false; // This listener does not support aborting (it's not connected to the user interface)
        }
    }
}
