/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Represents data about iterative operation that starts.
 *
 * TODO better name
 */
@Experimental
public class IterativeOperationStartInfo {

    private final IterationItemInformation item;
    private final long startTimestamp;
    private final Long partStartTimestamp;
    private final String partUri;

    /**
     * If present, we use this object to increment the structured progress on operation completion.
     * This is useful because there is a lot of shared information: part uri, and qualified outcome.
     *
     * TODO make this final?
     */
    private StructuredProgressCollector structuredProgressCollector;

    public IterativeOperationStartInfo(IterationItemInformation item) {
        this(item, null);
    }

    public IterativeOperationStartInfo(IterationItemInformation item, String partUri) {
        this(item, System.currentTimeMillis(), partUri, null);
    }

    public IterativeOperationStartInfo(IterationItemInformation item, String partUri, Long partStartTimestamp) {
        this(item, System.currentTimeMillis(), partUri, partStartTimestamp);
    }

    public IterativeOperationStartInfo(IterationItemInformation item, long startTimestamp, String partUri, Long partStartTimestamp) {
        this.item = item;
        this.startTimestamp = startTimestamp;
        this.partUri = partUri;
        this.partStartTimestamp = partStartTimestamp;
    }

    public IterationItemInformation getItem() {
        return item;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public Long getPartStartTimestamp() {
        return partStartTimestamp;
    }

    public String getPartUri() {
        return partUri;
    }

    public StructuredProgressCollector getStructuredProgressCollector() {
        return structuredProgressCollector;
    }

    public void setStructuredProgressCollector(StructuredProgressCollector structuredProgressCollector) {
        this.structuredProgressCollector = structuredProgressCollector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item=" + item +
                ", startTimestamp=" + startTimestamp +
                ", partStartTimestamp=" + partStartTimestamp +
                ", partUri=" + partUri +
                ", structuredProgressCollector=" + structuredProgressCollector +
                '}';
    }
}
