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

import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class ResourceState implements Serializable {

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
