/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;
import java.util.Date;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author Katuska
 */
public class ResourceImportController implements Serializable {

	private String name;
	private Date launchTime;
	private Date finishTime;
	private String lastStatus;
	private long numberOfErrors;
	private OperationResult lastError;
	private long progress;
	private boolean running;
	
	public ResourceImportController(){
		//TODO
	}

	public String getName() {
		return name;
	}

	public Date getLaunchTime() {
		return launchTime;
	}

	public String getLaunchTimeString() {
		return launchTime.toString();
	}

	public String getFinishTimeString() {
		return finishTime.toString();
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
}
