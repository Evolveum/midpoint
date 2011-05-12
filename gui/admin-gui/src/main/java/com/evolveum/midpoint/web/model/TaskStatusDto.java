/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model;

import java.io.Serializable;

/**
 *
 * @author Katuska
 */
public class TaskStatusDto implements Serializable {
    
	private static final long serialVersionUID = -5358337966691482206L;
	private String finishTime;
    private String lastStatus;
    private DiagnosticMessageDto lastError;
    private String name;
    private String launchTime;
    private String numberOfErrors;
    private String progress;
    private String running;


    
    public TaskStatusDto() {
    }


    

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public DiagnosticMessageDto getLastError() {
        return lastError;
    }

    public void setLastError(DiagnosticMessageDto lastError) {
        this.lastError = lastError;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(String launchTime) {
        this.launchTime = launchTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumberOfErrors() {
        return numberOfErrors;
    }

    public void setNumberOfErrors(String numberOfErrors) {
        this.numberOfErrors = numberOfErrors;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getRunning() {
        return running;
    }

    public void setRunning(String running) {
        this.running = running;
    }

    
}
