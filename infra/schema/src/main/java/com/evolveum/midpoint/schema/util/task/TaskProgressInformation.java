/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@Deprecated
public class TaskProgressInformation implements DebugDumpable, Serializable {

    /**
     * Total number of parts.
     */
    private final int allPartsCount;

    /**
     * Number of the part that is being currently executed or that was last executed. Starting at 1.
     */
    private final int currentPartNumber;

    /**
     * URI of the part that is being currently executed or that was last executed.
     */
    private final String currentPartUri;

    /**
     * Information on the progress in individual parts. Indexed by part URI.
     */
    private final Map<String, TaskPartProgressInformation> parts = new HashMap<>();

    private TaskProgressInformation(int allPartsCount, int currentPartNumber, String currentPartUri) {
        this.allPartsCount = allPartsCount;
        this.currentPartNumber = currentPartNumber;
        this.currentPartUri = currentPartUri;
    }

    /**
     * Precondition: the task contains fully retrieved and resolved subtasks.
     */
    public static TaskProgressInformation fromTaskTree(TaskType task) {
        throw new UnsupportedOperationException();
    }

    public int getAllPartsCount() {
        return allPartsCount;
    }

    public int getCurrentPartNumber() {
        return currentPartNumber;
    }

    /**
     * Returns current part URI (for internally partitioned tasks) or handler URI (for physically partitioned tasks).
     *
     * TODO clarify
     */
    public String getCurrentPartUri() {
        return currentPartUri;
    }

    public TaskPartProgressInformation getCurrentPartInformation() {
        return parts.get(currentPartUri);
    }

    public Map<String, TaskPartProgressInformation> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return "TaskProgressInformation{" +
                "allPartsCount=" + allPartsCount +
                ", currentPartNumber=" + currentPartNumber +
                ", currentPartUri=" + currentPartUri +
                ", parts=" + parts +
                '}';
    }

    public String toHumanReadableString(boolean longForm) {
        StringBuilder sb = new StringBuilder();
        currentPartToHumanReadableString(sb, longForm);
        if (isMultiPart()) {
            if (longForm) {
                sb.append(" in part ").append(getCurrentPartNumber()).append(" of ").append(getAllPartsCount());
            } else {
                sb.append(" in ").append(getCurrentPartNumber()).append("/").append(getAllPartsCount());
            }
        }
        return sb.toString();
    }

    private void currentPartToHumanReadableString(StringBuilder sb, boolean longForm) {
    }

    private boolean isMultiPart() {
        return getAllPartsCount() > 1;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Long form", toHumanReadableString(true), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Short form", toHumanReadableString(false), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "All parts count", allPartsCount, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Current part number", currentPartNumber, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Current part URI", currentPartUri, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Parts", parts, indent + 1);
        return sb.toString();
    }

    public void checkConsistence() {
    }
}
