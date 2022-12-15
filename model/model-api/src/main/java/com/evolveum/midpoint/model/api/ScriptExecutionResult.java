/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a script execution.
 */
public class ScriptExecutionResult {

    private final String consoleOutput;
    private final List<PipelineItem> dataOutput; // unmodifiable + always non-null

    public ScriptExecutionResult(String consoleOutput, List<PipelineItem> dataOutput) {
        this.consoleOutput = consoleOutput;
        if (dataOutput == null) {
            dataOutput = new ArrayList<>();
        }
        this.dataOutput = Collections.unmodifiableList(dataOutput);
    }

    public String getConsoleOutput() {
        return consoleOutput;
    }

    public List<PipelineItem> getDataOutput() {
        return dataOutput;
    }
}
