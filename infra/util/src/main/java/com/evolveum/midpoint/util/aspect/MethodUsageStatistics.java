/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationInvocationRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  This class provides functionality as a holder for method performance statistics. Currently, we are monitoring
 *  and updating following statistics:
 *     longest method call - MAX
 *     quickest method call - MIN
 *     average length of method call - MEAN
 *     number of method calls - USAGE_COUNT
 *
 *  We also update these statistics every time specific method call is captured by MidpointInterceptor class and then
 *  processed by AspectProfilingFilters. This is performed in update() method that is synchronized for obvious
 *  reasons.
 *
 *  @author shood
 * */
public class MethodUsageStatistics {

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    private long min = Long.MAX_VALUE;
    private long max = 0;
    private long mean = 0;
    private long processTimeMean = 0;
    private long usageCount = 1;
    private long currentTopTenMin = Long.MAX_VALUE;
    private ProfilingDataManager.Subsystem subsystem;
    private List<ProfilingDataLog> slowestMethodList = Collections.synchronizedList(new ArrayList<ProfilingDataLog>());

    public MethodUsageStatistics(ProfilingDataLog logEvent, ProfilingDataManager.Subsystem subsystem){
        long estTime = logEvent.getEstimatedTime();

        this.min = estTime;
        this.max = estTime;
        this.usageCount++;
        this.currentTopTenMin = estTime;
        this.subsystem = subsystem;
        this.mean = estTime;

    }

    public long getProcessTimeMean() {
        return processTimeMean;
    }

    public void setProcessTimeMean(long processTimeMean) {
        this.processTimeMean = processTimeMean;
    }

    public ProfilingDataManager.Subsystem getSubsystem() {
        return subsystem;
    }

    public void setSubsystem(ProfilingDataManager.Subsystem subsystem) {
        this.subsystem = subsystem;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMean() {
        return mean;
    }

    public void setMean(long mean) {
        this.mean = mean;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(long usageCount) {
        this.usageCount = usageCount;
    }

    public long getCurrentTopTenMin() {
        return currentTopTenMin;
    }

    public void setCurrentTopTenMin(long currentTopTenMin) {
        this.currentTopTenMin = currentTopTenMin;
    }

    public List<ProfilingDataLog> getSlowestMethodList() {
        return slowestMethodList;
    }

    public void setSlowestMethodList(List<ProfilingDataLog> slowestMethodList) {
        this.slowestMethodList = slowestMethodList;
    }

    public synchronized void update(ProfilingDataLog logEvent){
        long currentEst = logEvent.getEstimatedTime();

        if(this.min > currentEst) {
            this.min = currentEst;
        }

        if(this.max < currentEst) {
            this.max = currentEst;
        }

        calculateMeanIterative(currentEst);
        usageCount++;

    }

    private void calculateMeanIterative(long currentEst){
        this.mean += (currentEst - this.mean)/this.usageCount;
    }

    public void updateProcessTimeList(long est){
        this.processTimeMean += (est - this.processTimeMean)/this.usageCount;
    }

    public void appendToLogger(boolean afterTest){
        ProfilingDataLog log = this.slowestMethodList.get(0);

        if(afterTest){
            LOGGER.info("{}->{}: CALLS: {} MAX: {} MIN: {} MEAN: {} PROCESS_TIME_MEAN: {}",
                    log.getClassName(), log.getMethodName(), usageCount, formatExecutionTime(max),
                    formatExecutionTime(min), formatExecutionTime(this.mean), formatExecutionTime(processTimeMean));
        } else {
            LOGGER.debug("{}->{}: CALLS: {} MAX: {} MIN: {} MEAN: {} PROCESS_TIME_MEAN: {}",
                    log.getClassName(), log.getMethodName(), usageCount, formatExecutionTime(max),
                    formatExecutionTime(min), formatExecutionTime(this.mean), formatExecutionTime(processTimeMean));
        }


        for(ProfilingDataLog l: this.slowestMethodList) {
            l.appendToLogger(afterTest);
        }
    }

    private static String formatExecutionTime(long est){
        StringBuilder sb = new StringBuilder();

        OperationInvocationRecord.formatExecutionTime(sb, est);
        sb.append(" ms.");

        return sb.toString();
    }
}
