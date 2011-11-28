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

package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.Date;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.util.FacesUtils;

/**
 * 
 * @author Katuska
 */
public class TaskStatus implements Serializable {

	private static final long serialVersionUID = -5358337966691482206L;
	private String name;
	private Date launchTime;
	private Date finishTime;
	private String lastStatus;
	private long numberOfErrors;
	private OperationResult lastError;
	private long progress;
	private boolean running;

	public TaskStatus() {
		// TODO
	}

	public String getName() {
		return name;
	}

	public Date getLaunchTime() {
		return launchTime;
	}

	public String getLaunchTimeString() {
		return FacesUtils.formatDate(getLaunchTime());
	}

	public String getFinishTimeString() {
		return FacesUtils.formatDate(getFinishTime());
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public String getLastStatus() {
		return lastStatus;
	}

	public OperationResult getLastError() {
		return lastError;
	}

	public long getProgress() {
		return progress;
	}

	public boolean isRunning() {
		return running;
	}

	public long getNumberOfErrors() {
		return numberOfErrors;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLaunchTime(Date launchTime) {
		this.launchTime = launchTime;
	}

	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}

	public void setLastStatus(String lastStatus) {
		this.lastStatus = lastStatus;
	}

	public void setLastError(OperationResult lastError) {
		this.lastError = lastError;
	}

	public void setProgress(long progress) {
		this.progress = progress;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void setNumberOfErrors(long numberOfErrors) {
		this.numberOfErrors = numberOfErrors;
	}
}
