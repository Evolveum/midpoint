/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationInvocationRecord;

import java.util.Date;

/**
 *     This is a blueprint for single method call, or ProfilingEvent as we call it. In here, we capture some
 *     attributes for each method call, specifically:
 *     className with package name
 *     method name
 *     objectType with which method works (or deltaType for some model methods)
 *     executionTimestamp - when method call was performed
 *     estimatedTime - method call duration
 *
 *
 *  @author shood
 * */
//@Deprecated
public class ProfilingDataLog {

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    private String className;
    private String methodName;
    long executionTimestamp;
    long estimatedTime;
    Object[] args;

    //this is here for profiling events captured from servlet requests
    private String sessionID = null;

    public ProfilingDataLog(String className, String method, long est, long exeTimestamp, Object[] args){
        this.className = className;
        this.methodName = method;
        this.estimatedTime = est;
        this.executionTimestamp = exeTimestamp;
        this.args = args;
    }

    public ProfilingDataLog(String method, String uri, String sessionID, long est, long exec){
        this.methodName = method;
        this.className = uri;
        this.sessionID = sessionID;
        this.estimatedTime = est;
        this.executionTimestamp = exec;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

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

//    public Object[] retrieveMethodArguments(ProceedingJoinPoint pjp){
//        return pjp.getArgs();
//    }

    public void logProfilingEvent(Trace LOGGER){
        LOGGER.info(className + "->" + methodName + " est: " + formatExecutionTime(estimatedTime));
    }

    public void appendToLogger(boolean afterTest){
        Date date = new Date(executionTimestamp);

        //If we are printing request filter event, there are no arguments, but sessionID instead
        if(sessionID != null){
            if(afterTest){
                LOGGER.info("    EST: {} EXECUTED: {} SESSION: {}", formatExecutionTime(estimatedTime), date, sessionID);
            } else {
                LOGGER.debug("    EST: {} EXECUTED: {} SESSION: {}", formatExecutionTime(estimatedTime), date, sessionID);
            }
        } else{
            if(afterTest){
                LOGGER.info("    EST: {} EXECUTED: {} ARGS: {}", formatExecutionTime(estimatedTime), date, args);
            } else {
                LOGGER.debug("    EST: {} EXECUTED: {} ARGS: {}", formatExecutionTime(estimatedTime), date, args);
            }

        }
    }

    private static String formatExecutionTime(long est){
        StringBuilder sb = new StringBuilder();

        OperationInvocationRecord.formatExecutionTime(sb, est);
        sb.append(" ms.");

        return sb.toString();
    }
}
