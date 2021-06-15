/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;

import com.evolveum.midpoint.util.DebugDumpable;

@Deprecated
public class TaskPartProgressInformation implements DebugDumpable, Serializable {

    public String getPartOrHandlerUri() {
        return null;
    }

    public boolean isComplete() {
        return false;
    }

    public BucketsProgressInformation getBucketsProgress() {
        return null;
    }

    public ItemsProgressInformation getItemsProgress() {
        return null;
    }

    @Override
    public String debugDump(int indent) {
        return "N/A";
    }

    public String toHumanReadableString(boolean longForm) {
        return "N/A";
    }
}
