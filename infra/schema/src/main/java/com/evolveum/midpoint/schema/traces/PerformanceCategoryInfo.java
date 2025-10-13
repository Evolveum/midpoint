/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;

@Experimental
public class PerformanceCategoryInfo implements Serializable {

    private static final long serialVersionUID = -5470613343339388018L;

    private long ownTime;
    private long totalTime;
    private int ownCount;
    private int totalCount;

    public long getOwnTime() {
        return ownTime;
    }
    public void setOwnTime(long ownTime) {
        this.ownTime = ownTime;
    }
    public long getTotalTime() {
        return totalTime;
    }
    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }
    public int getOwnCount() {
        return ownCount;
    }
    public void setOwnCount(int ownCount) {
        this.ownCount = ownCount;
    }
    public int getTotalCount() {
        return totalCount;
    }
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
