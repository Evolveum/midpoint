/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public class PerformanceCategoryInfo {
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
