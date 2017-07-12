/*
 * Copyright (c) 2010-2017 Evolveum
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
@Deprecated
public class ProfilingDataLog {

    private static Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

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
                LOGGER.info("    EST: {} EXECUTED: {} SESSION: {}", new Object[]{formatExecutionTime(estimatedTime), date, sessionID});
            } else {
                LOGGER.debug("    EST: {} EXECUTED: {} SESSION: {}", new Object[]{formatExecutionTime(estimatedTime), date, sessionID});
            }
        } else{
            if(afterTest){
                LOGGER.info("    EST: {} EXECUTED: {} ARGS: {}", new Object[]{formatExecutionTime(estimatedTime), date, args});
            } else {
                LOGGER.debug("    EST: {} EXECUTED: {} ARGS: {}", new Object[]{formatExecutionTime(estimatedTime), date, args});
            }

        }
    }

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
    }
}
