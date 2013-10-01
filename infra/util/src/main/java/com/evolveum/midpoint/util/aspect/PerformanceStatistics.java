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
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.*;


/**
 *  This class simply collects basic information about midPoint performance, specifically basic
 *  CPU, memory usage and current thread state.
 *
 *  @author shood
 * */
public class PerformanceStatistics {

    /* LOGGER */
    private static Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    /* CONSTANTS */
    private int MB = 1024*1024;

    /* Performance attributes */

    //CPU
    private double cpuUsage;

    //Memory
    private long usedHeapMemory;
    private long committedHeapMemory;
    private long usedNonHeapMemory;
    private long committedNonHeapMemory;

    //Threads
    private int threadCount;
    private int daemonThreadCount;

    /*
    *   Constructor
    * */
    public PerformanceStatistics(){

        //Fetch and ...
        this.cpuUsage = fetchCpuUsage();
        fetchMemoryUsage();
        fetchThreadUsage();

        // ... dump to log
        dump();
    }   //PerformanceStatistics constructor

    /* BEHAVIOR */
    /*
    *   Fetches cpuUsage during 10 ms Thread.sleep() period
    * */
    private double fetchCpuUsage(){
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        long prevUpTime = runtimeMXBean.getUptime();
        long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();

        try{
            Thread.sleep(10);
        }catch (Exception ignored) {}

        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;

        double cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));

        return cpuUsage;
        //return operatingSystemMXBean.getSystemCpuLoad();
    }   //fetchCpuUsage

    /*
    *   Collects information about memory Usage
    * */
    private void fetchMemoryUsage(){
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memoryUsageNonHeap = memoryMXBean.getNonHeapMemoryUsage();
        MemoryUsage memoryUsageHeap = memoryMXBean.getHeapMemoryUsage();

        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();

        this.usedHeapMemory = totalMemory - freeMemory;
        this.committedHeapMemory = memoryUsageHeap.getCommitted();

        this.usedNonHeapMemory = memoryUsageNonHeap.getUsed();
        this.committedNonHeapMemory = memoryUsageNonHeap.getCommitted();

    }   //fetchMemoryUsage

    /*
    *   Get information about current thread usage
    * */
    private void fetchThreadUsage(){
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        this.threadCount = threadBean.getThreadCount();
        this.daemonThreadCount = threadBean.getDaemonThreadCount();
    }   //fetchThreadUsage

    /*
    *   Dump all performance statistics to log
    * */
    private void dump(){
        LOGGER.debug("CPU usage: {} %, heap memory used: {} MB, committed: {} MB, non-heap memory used: {} MB, committed: {} MB, Threads: {}, daemon threads: {}. ",
                new Object[]{formatCpuUsage(), usedHeapMemory/MB, committedHeapMemory/MB, usedNonHeapMemory/MB, committedNonHeapMemory/MB, threadCount, daemonThreadCount});
    }   //dump

    /*
    *   Returns formatted version of cpuUsage (currently to 2 decimal points)
    * */
    private String formatCpuUsage(){
        return String.format("%.2f", this.cpuUsage);
    }

    @Override
    public String toString(){
        String s = formatCpuUsage() + ", heap[used]: " + usedHeapMemory/MB + ", heap[committed]: " + committedHeapMemory/MB +
                ", non-heap[used]: " + usedNonHeapMemory/MB + ", non-heap[committed]: " + committedNonHeapMemory/MB + ", threads: " + threadCount +
                ", daemon threads: " + daemonThreadCount;

        return s;
    }

    /* GETTERS AND SETTERS */
    public long getUsedHeapMemory() {
        return usedHeapMemory;
    }

    public long getCommittedNonHeapMemory() {
        return committedNonHeapMemory;
    }

    public int getThreadCount() {
        return threadCount;
    }
}
