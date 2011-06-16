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
 */
package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.Date;

import com.evolveum.midpoint.web.util.FacesUtils;

/**
 * 
 * @author lazyman
 * 
 */
public class ResourceSync implements Serializable {

	private static final long serialVersionUID = -7590740268253015859L;

	private boolean enabled;
	private int pollingInterval;
	private Date lastRunTime;
	private long timeToProcess;
	private String message;
	
	public boolean isEnabled() {
		return enabled;
	}

	public String getSyncTitle() {
		if (enabled) {
			return "Synchronization enabled";
		}
		return "Synchronization disabled";
	}

	public String getStatus() {
		if (enabled) {
			return FacesUtils.translateKey("Enabled");
		}

		return FacesUtils.translateKey("Disabled");
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(int pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	public String getLastRunTimeString() {
		if (getLastRunTime() == null) {
			return "Unknown";
		}

		return FacesUtils.formatDate(getLastRunTime());
	}

	public Date getLastRunTime() {
		return lastRunTime;
	}

	public void setLastRunTime(Date lastRunTime) {
		this.lastRunTime = lastRunTime;
	}

	public long getTimeToProcess() {
		return timeToProcess;
	}

	public void setTimeToProcess(long timeToProcess) {
		this.timeToProcess = timeToProcess;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
