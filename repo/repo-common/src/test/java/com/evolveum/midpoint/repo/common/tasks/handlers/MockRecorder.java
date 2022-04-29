/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static org.apache.commons.collections4.ListUtils.synchronizedList;

@Component
public class MockRecorder implements DebugDumpable {

    @SuppressWarnings("unchecked")
    private final List<String> executions = synchronizedList(new ArrayList<>());

    /** This is to verify that realization start timestamps in workers are the same. */
    @NotNull private final Set<XMLGregorianCalendar> realizationStartTimestamps = new HashSet<>();

    public void recordExecution(String value) {
        executions.add(value);
    }

    public List<String> getExecutions() {
        return executions;
    }

    public void recordRealizationStartTimestamp(XMLGregorianCalendar value) {
        realizationStartTimestamps.add(value);
    }

    public @NotNull Collection<XMLGregorianCalendar> getRealizationStartTimestamps() {
        return realizationStartTimestamps;
    }

    public void reset() {
        executions.clear();
        realizationStartTimestamps.clear();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "MockRecorder", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "executions", executions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "realization start timestamps", realizationStartTimestamps, indent + 1);
        return sb.toString();
    }
}
