/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.handler;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Duration;

/**
 * Keeps the state of the role-mining activity (clustering, pattern detection, etc).
 */
public class RoleAnalysisProgressIncrement implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisProgressIncrement.class);

    private String title;
    private String subTitle;
    private int operationCountToProcess;
    private boolean isActive;
    private int actualStatus;
    private final int stepsCount;
    private int currentStep;
    private String objectId;
    private final long startTime;
    private long endTime;

    /**
     * Called when an external progress value (currently, for tasks) has to be incremented.
     *
     * TODO what about serializability? Currently we assume we'll never use deserialized version of this class isntance
     */
    @Nullable private transient final Runnable progressIncrementer;

    public int getPercentage() {
        return percentage;
    }

    private int percentage;

    public String getDuration() {
        long elapsedTime = endTime - startTime;

        return Duration.ofMillis(elapsedTime)
                .toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    public RoleAnalysisProgressIncrement(String title, int stepsCount) {
        this(title, stepsCount, null);
    }

    public RoleAnalysisProgressIncrement(String title, int stepsCount, @Nullable Runnable progressIncrementer) {
        this.startTime = System.currentTimeMillis();
        this.endTime = System.currentTimeMillis();
        this.title = title;
        this.stepsCount = stepsCount;
        this.isActive = true;
        this.actualStatus = 0;
        this.currentStep = 0;
        this.progressIncrementer = progressIncrementer;
    }

    public void iterateActualStatus() {
        this.actualStatus++;
        this.endTime = System.currentTimeMillis();
        this.percentage = (int) ((actualStatus / (double) operationCountToProcess) * 100);
        log();
    }

    public void enterNewStep(String subTitle) {
        if (progressIncrementer != null) {
            progressIncrementer.run();
        }
        this.currentStep++;
        this.operationCountToProcess = 0;
        this.actualStatus = 0;
        this.subTitle = subTitle;
        this.percentage = 0;
        log();
    }

    @Override
    public String toString() {
        return String.format("Step{%d/%d} - Status{%s, subOperation='%s',percentage=%d processedCount=%d,"
                        + " isActive=%b, actualStatus=%d} Object Id{%s} Duration{%s}",
                currentStep, stepsCount, title, subTitle, percentage, operationCountToProcess,
                isActive, actualStatus, getObjectId(), getDuration());
    }

    public void log() {
        LOGGER.debug(
                "Step {}/{} - Status=[{}, subOperation='{}', percentage={} processedCount={}, "
                        + "isActive={}, actualStatus={}] Object Id={} Duration={}",
                currentStep, stepsCount, title, subTitle, percentage, operationCountToProcess, isActive,
                actualStatus, getObjectId(), getDuration());
    }

    public void setOperationCountToProcess(int operationCountToProcess) {
        this.operationCountToProcess = operationCountToProcess;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getTitle() {
        return title;
    }

    public int getOperationCountToProcess() {
        return operationCountToProcess;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public int getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(int actualStatus) {
        this.actualStatus = actualStatus;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
}
