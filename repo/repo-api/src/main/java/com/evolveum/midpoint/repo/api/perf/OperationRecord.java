/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.api.perf;

import java.util.Date;

/**
 *
 */
public class OperationRecord {

    private String kind;
    private long handle;
    private int attempts;
    private long startTime;
    private long totalTime;
    private long wastedTime;

    public OperationRecord(String kind, long handle) {
        this.kind = kind;
        this.handle = handle;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "OperationRecord{" +
                "kind='" + kind + '\'' +
                ", handle=" + handle +
                ", attempts=" + attempts +
                ", startTime=" + new Date(startTime) +
                ", totalTime=" + totalTime +
                ", wastedTime=" + wastedTime +
                '}';
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public long getHandle() {
        return handle;
    }

    public void setHandle(long handle) {
        this.handle = handle;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public long getWastedTime() {
        return wastedTime;
    }

    public void setWastedTime(long wastedTime) {
        this.wastedTime = wastedTime;
    }
}
