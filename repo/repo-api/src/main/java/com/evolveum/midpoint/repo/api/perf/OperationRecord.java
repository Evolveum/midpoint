/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api.perf;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 *
 */
public class OperationRecord {

    private String kind;
    private Class<?> objectType;
    private long handle;
    private int attempts;
    private long startTime;
    private long totalTime; // in ms
    private long wastedTime;

    public OperationRecord(String kind, Class<?> objectType, long handle) {
        this.kind = kind;
        this.objectType = objectType;
        this.handle = handle;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "OperationRecord{" +
                "kind='" + kind + '\'' +
                ", objectType=" + getObjectTypeName() +
                ", handle=" + handle +
                ", attempts=" + attempts +
                ", startTime=" + new Date(startTime) +
                ", totalTime=" + totalTime +
                ", wastedTime=" + wastedTime +
                '}';
    }

    @NotNull
    public String getObjectTypeName() {
        return objectType != null ? objectType.getSimpleName() : "null";
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Class<?> getObjectType() {
        return objectType;
    }

    public void setObjectType(Class<?> objectType) {
        this.objectType = objectType;
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
