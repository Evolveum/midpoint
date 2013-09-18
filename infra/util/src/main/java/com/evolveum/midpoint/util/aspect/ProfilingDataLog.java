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

import java.util.Date;

/**
 *  TODO - add descriptive description
 *
 *
 *  @author shood
 * */
public class ProfilingDataLog {

    /* Some print constants */
    private static final String PRINT_EST = " , EST: ";
    private static final String PRINT_EXECUTED = " EXECUTED: ";
    private static final String PRINT_TAB = "\t";
    private static final String PRINT_NEW_LINE = "\n";

    /* Member Attributes */
    private String className;
    private String methodName;
    private String objectType;
    long executionTimestamp;
    long estimatedTime;

    /*
    *   Default constructor - provided if needed for some reason
    * */
    public ProfilingDataLog(){}

    /*
    *   Constructor - with parameters
    * */
    public ProfilingDataLog(String className, String method, long est, long exeTimestamp){
        this.className = className;
        this.methodName = method;
        this.estimatedTime = est;
        this.executionTimestamp = exeTimestamp;

    }   //ProfilingDataLog

    /* Getters and Setters */
    public long getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(long estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public long getExecutionTimestamp() {
        return executionTimestamp;
    }

    public void setExecutionTimestamp(long executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    /* Behavior */
    /*
    *   Prints profilingLog to provided LOGGER
    *   this method is here for test purposes only
    * */
    public void logProfilingEvent(Trace LOGGER){
        LOGGER.info(className + "->" + methodName + " " + objectType.toUpperCase() + " est: " + formatExecutionTime(estimatedTime));
    }   //logProfilingEvent

    /*
    *   Appends log event to logger
    * */
    public String appendToLogger(){
        StringBuilder sb = new StringBuilder();

        Date date = new Date(executionTimestamp);

        sb.append(PRINT_TAB);
        sb.append(objectType);
        sb.append(PRINT_EST);
        sb.append(formatExecutionTime(estimatedTime));
        sb.append(PRINT_EXECUTED);
        sb.append(date);
        sb.append(PRINT_NEW_LINE);

        return sb.toString();
    }   //appendToLogger

    /* =====STATIC HELPET METHODS===== */
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
