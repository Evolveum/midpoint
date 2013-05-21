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

import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class ResourceState implements Serializable {

	private ResourceStatus lastAvailability;
	private ResourceStatus overall;
	private ResourceStatus confValidation;
	private ResourceStatus conInitialization;
	private ResourceStatus conConnection;
	private ResourceStatus conSanity;
	private ResourceStatus conSchema;
	private ResourceStatus extra;
	private String extraName;

	public ResourceStatus getOverall() {
		overall = updateOverallStatus();
		if (overall == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return overall;
	}

	public ResourceStatus getConfValidation() {
		if (confValidation == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return confValidation;
	}

	public void setConfValidation(ResourceStatus confValidation) {
		this.confValidation = confValidation;
	}

	public ResourceStatus getConInitialization() {
		if (conInitialization == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return conInitialization;
	}

	public void setConInitialization(ResourceStatus conInitialization) {
		this.conInitialization = conInitialization;
	}

	public ResourceStatus getConConnection() {
		if (conConnection == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return conConnection;
	}

	public void setConConnection(ResourceStatus conConnection) {
		this.conConnection = conConnection;
	}

	public ResourceStatus getConSanity() {
		if (conSanity == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return conSanity;
	}

	public void setConSanity(ResourceStatus conSanity) {
		this.conSanity = conSanity;
	}

	public ResourceStatus getConSchema() {
		if (conSchema == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return conSchema;
	}

	public void setConSchema(ResourceStatus conSchema) {
		this.conSchema = conSchema;
	}

	public ResourceStatus getExtra() {
		if (extra == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return extra;
	}

	public void setExtra(ResourceStatus extra) {
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
	
	public ResourceStatus getLastAvailability() {
		if (lastAvailability == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return lastAvailability;
	}

	public void setLastAvailability(ResourceStatus lastAvailability) {
		this.lastAvailability = lastAvailability;
	}

	private ResourceStatus updateOverallStatus() {
		ResourceStatus overall = ResourceStatus.NOT_TESTED;
		overall = getOverallBasedOnPartialStatus(overall, getConConnection());
		overall = getOverallBasedOnPartialStatus(overall, getConfValidation());
		overall = getOverallBasedOnPartialStatus(overall, getConInitialization());
		overall = getOverallBasedOnPartialStatus(overall, getConSanity());
		overall = getOverallBasedOnPartialStatus(overall, getConSchema());
		overall = getOverallBasedOnPartialStatus(overall, getExtra());

		return overall;
	}

	private ResourceStatus getOverallBasedOnPartialStatus(ResourceStatus overall, ResourceStatus partial) {
		switch (overall) {
			case NOT_TESTED:
			case SUCCESS:
				if (!ResourceStatus.NOT_TESTED.equals(partial)) {
					overall = partial;
				}
				break;
			case WARNING:
				if (!ResourceStatus.NOT_TESTED.equals(partial) && !ResourceStatus.SUCCESS.equals(partial)) {
					overall = partial;
				}
				break;
			case ERROR:
		}

		return overall;
	}
}
