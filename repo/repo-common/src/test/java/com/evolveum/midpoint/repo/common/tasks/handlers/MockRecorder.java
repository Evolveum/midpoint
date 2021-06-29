/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.ListUtils.synchronizedList;

@Component
public class MockRecorder implements DebugDumpable {

    @SuppressWarnings("unchecked")
    private final List<String> executions = synchronizedList(new ArrayList<>());

    public void recordExecution(String value) {
        executions.add(value);
    }

    public List<String> getExecutions() {
        return executions;
    }

    public void reset() {
        executions.clear();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "MockRecorder", indent);
        DebugUtil.debugDumpWithLabel(sb, "executions", executions, indent + 1);
        return sb.toString();
    }
}
