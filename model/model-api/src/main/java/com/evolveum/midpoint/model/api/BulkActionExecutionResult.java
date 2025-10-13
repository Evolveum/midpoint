/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a bulk action execution.
 */
public class BulkActionExecutionResult {

    private final String consoleOutput;
    private final List<PipelineItem> dataOutput; // unmodifiable + always non-null

    public BulkActionExecutionResult(String consoleOutput, List<PipelineItem> dataOutput) {
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
