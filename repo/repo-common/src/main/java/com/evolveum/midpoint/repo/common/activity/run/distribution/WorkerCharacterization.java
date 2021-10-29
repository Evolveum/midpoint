/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.distribution;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * Characterizes a worker task that should be present in a system.
 *
 * Used to compare "to be" state of worker tasks with the current one.
 */
class WorkerCharacterization {

    /**
     * Execution group this task should be a part of.
     * Currently the execution group is the node ID where the task will run.
     */
    final String group;

    /**
     * Name of the worker task. It is given by the configuration, although there is a default
     * naming scheme that is used almost all the time.
     */
    final String name;

    /**
     * Should this task be a scavenger?
     */
    final boolean scavenger;

    private WorkerCharacterization(String group, String name, boolean scavenger) {
        this.group = group;
        this.name = name;
        this.scavenger = scavenger;
    }

    /**
     * Creates worker characterization for given parameters.
     */
    static WorkerCharacterization forParameters(String group, String name, boolean scavenger) {
        return new WorkerCharacterization(group, name, scavenger);
    }

    public static Optional<WorkerCharacterization> find(@NotNull Collection<WorkerCharacterization> characterizations,
            String group, String name, boolean scavenger) {
        WorkerCharacterization lookingFor = WorkerCharacterization.forParameters(group, name, scavenger);
        return characterizations.stream()
                .filter(c -> c.equals(lookingFor))
                .findAny();
    }

    public static Optional<WorkerCharacterization> find(@NotNull Collection<WorkerCharacterization> characterizations,
            String group, boolean scavenger) {
        return characterizations.stream()
                .filter(c -> c.matches(group, scavenger))
                .findAny();
    }

    public static Optional<WorkerCharacterization> find(@NotNull Collection<WorkerCharacterization> characterizations,
            String group) {
        return characterizations.stream()
                .filter(c -> c.matches(group))
                .findAny();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof WorkerCharacterization)) { return false; }
        WorkerCharacterization workerCharacterization = (WorkerCharacterization) o;
        return matches(workerCharacterization.group, workerCharacterization.scavenger, workerCharacterization.name);
    }

    private boolean matches(String group, boolean scavenger, String name) {
        return matches(group, scavenger) &&
                Objects.equals(this.name, name);
    }

    private boolean matches(String group, boolean scavenger) {
        return matches(group) &&
                this.scavenger == scavenger;
    }

    private boolean matches(String group) {
        return Objects.equals(this.group, group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name);
    }

    @Override
    public String toString() {
        return "[" + group + ", " + name + (scavenger ? " (scavenger)" : "") + "]";
    }
}
