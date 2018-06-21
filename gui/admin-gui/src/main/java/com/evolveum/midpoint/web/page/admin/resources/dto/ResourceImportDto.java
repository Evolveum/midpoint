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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;
import java.util.Date;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author Katuska
 */
public class ResourceImportDto implements Serializable {

	private String name;
	private Date launchTime;
	private Date finishTime;
	private String lastStatus;
	private long numberOfErrors;
	private OperationResult lastError;
	private long progress;
	private boolean running;

	public ResourceImportDto(){
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
