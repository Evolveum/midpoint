/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.logging.Trace;

/**
 *  This class provides functionality as a holder for method performance statistics. Currently, we are monitoring
 *  and updating following statistics:
 *     longest method call - MAX
 *     quickest method call - MIN
 *     average length of method call - MEAN
 *     number of method calls - USAGE_COUNT
 *
 *  We also update these statistics every time specific method call is captured by MidpointAspect class and then
 *  processed by AspectProfilingFilters. This is performed in update() method that is synchronized for obvious
 *  reasons.
 *
 *  @author shood
 * */
public class MethodUsageStatistics {

    /* Attributes - member */
    private long min = Long.MAX_VALUE;
    private long max = 0;
    private long mean = 0;
    private long usageCount = 0;
    private long currentTopTenMin = Long.MAX_VALUE;

    /* Some print constants */
    private static final String PRINT_USAGE = ": CALLS: ";
    private static final String PRINT_MIN = " MIN: ";
    private static final String PRINT_MAX = " MAX: ";
    private static final String PRINT_MEAN = " MEAN: ";
    private static final String PRINT_NEWLINE = "\n";

    /*
    *   Constructor
    * */
    public MethodUsageStatistics(ProfilingDataLog logEvent){
        long estTime = logEvent.getEstimatedTime();

        this.min = estTime;
        this.max = estTime;
        this.mean = estTime;
        this.usageCount++;
        this.currentTopTenMin = estTime;

    }   //MethodUsageStatistics

    /* GETTERS AND SETTERS */
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

    /* BEHAVIOR */
    public synchronized void update(ProfilingDataLog logEvent){
        long currentEst = logEvent.getEstimatedTime();

        usageCount++;

        if(this.min > currentEst)
            this.min = currentEst;

        if(this.max < currentEst)
            this.max = currentEst;

        this.mean = calculateMean(currentEst);

    }   //update

    /*
    *   Calculates current mean value
    * */
    private long calculateMean(long est){
        return ((this.mean+est)/2);
    }   //calculateMean

    /*
    *   Appends method usage statistics to log file
    * */
    public String appendToLogger(){
        StringBuilder sb = new StringBuilder();

        sb.append(PRINT_USAGE);
        sb.append(usageCount);
        sb.append(PRINT_MAX);
        sb.append(formatExecutionTime(max));
        sb.append(PRINT_MIN);
        sb.append(formatExecutionTime(min));
        sb.append(PRINT_MEAN);
        sb.append(formatExecutionTime(mean));
        sb.append(PRINT_NEWLINE);

        return sb.toString();
    }   //appendToLogger

    /* STATIC HELPER METHODS */
    /*
    *   Formats execution time
    * */
    private static String formatExecutionTime(long est){
        StringBuilder sb = new StringBuilder();

        sb.append((long) (est / 1000000));
        sb.append('.');
        long mikros = (long) (est / 1000) % 1000;
        if (mikros < 100) {
            sb.append('0');
        }
        if (mikros < 10) {
            sb.append('0');
        }
        sb.append(mikros);
        sb.append(" ms.");

        return sb.toString();
    }   //formatExecutionTime

}
