/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work.workers;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.BucketingUtil;
import com.evolveum.midpoint.task.api.Task;

import java.util.Objects;

/**
 * TODO name of this class
 */
class WorkerSpec {
    final String group;
    final String name;
    final boolean scavenger;

    WorkerSpec(String group, String name, boolean scavenger) {
        this.group = group;
        this.name = name;
        this.scavenger = scavenger;
    }

    static WorkerSpec forTask(Task task, ActivityPath activityPath) {
        return new WorkerSpec(
                task.getGroup(),
                PolyString.getOrig(task.getName()),
                BucketingUtil.isScavenger(task.getWorkState(), activityPath));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof WorkerSpec)) { return false; }
        WorkerSpec workerSpec = (WorkerSpec) o;
        return Objects.equals(group, workerSpec.group) &&
                Objects.equals(name, workerSpec.name);
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
