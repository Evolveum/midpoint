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

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class ResourceState implements Serializable {

	private OperationResultStatus lastAvailability;
	private OperationResultStatus overall;
	private OperationResultStatus confValidation;
	private OperationResultStatus conInitialization;
	private OperationResultStatus conConnection;
	private OperationResultStatus conSanity;
	private OperationResultStatus conSchema;
	private OperationResultStatus extra;
	private String extraName;

	public OperationResultStatus getOverall() {
		overall = updateOverallStatus();
		if (overall == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return overall;
	}

	public OperationResultStatus getConfValidation() {
		if (confValidation == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return confValidation;
	}

	public void setConfValidation(OperationResultStatus confValidation) {
		this.confValidation = confValidation;
	}

	public OperationResultStatus getConInitialization() {
		if (conInitialization == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return conInitialization;
	}

	public void setConInitialization(OperationResultStatus conInitialization) {
		this.conInitialization = conInitialization;
	}

	public OperationResultStatus getConConnection() {
		if (conConnection == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return conConnection;
	}

	public void setConConnection(OperationResultStatus conConnection) {
		this.conConnection = conConnection;
	}

	public OperationResultStatus getConSanity() {
		if (conSanity == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return conSanity;
	}

	public void setConSanity(OperationResultStatus conSanity) {
		this.conSanity = conSanity;
	}

	public OperationResultStatus getConSchema() {
		if (conSchema == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return conSchema;
	}

	public void setConSchema(OperationResultStatus conSchema) {
		this.conSchema = conSchema;
	}

	public OperationResultStatus getExtra() {
		if (extra == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return extra;
	}

	public void setExtra(OperationResultStatus extra) {
		this.extra = extra;
	}

	public String getExtraName() {
		if (StringUtils.isEmpty(extraName)) {
			return "Unknown";
		}
		return extraName;
	}

	public void setExtraName(String extraName) {
		this.extraName = extraName;
	}

	public OperationResultStatus getLastAvailability() {
		if (lastAvailability == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return lastAvailability;
	}

	public void setLastAvailability(OperationResultStatus lastAvailability) {
		this.lastAvailability = lastAvailability;
	}

	private OperationResultStatus updateOverallStatus() {
        OperationResultStatus overall = OperationResultStatus.UNKNOWN;
		overall = getOverallBasedOnPartialStatus(overall, getConConnection());
		overall = getOverallBasedOnPartialStatus(overall, getConfValidation());
		overall = getOverallBasedOnPartialStatus(overall, getConInitialization());
		overall = getOverallBasedOnPartialStatus(overall, getConSanity());
		overall = getOverallBasedOnPartialStatus(overall, getConSchema());
		overall = getOverallBasedOnPartialStatus(overall, getExtra());

		return overall;
	}

	private OperationResultStatus getOverallBasedOnPartialStatus(OperationResultStatus overall, OperationResultStatus partial) {
		switch (overall) {
			case UNKNOWN:
			case SUCCESS:
				if (!OperationResultStatus.UNKNOWN.equals(partial)) {
					overall = partial;
				}
				break;
			case WARNING:
				if (!OperationResultStatus.UNKNOWN.equals(partial) && !OperationResultStatus.SUCCESS.equals(partial)) {
					overall = partial;
				}
				break;
			case FATAL_ERROR:
                break;
            case PARTIAL_ERROR:
                break;
            case HANDLED_ERROR:
                break;
            case IN_PROGRESS:
                break;
		}

		return overall;
	}
}
