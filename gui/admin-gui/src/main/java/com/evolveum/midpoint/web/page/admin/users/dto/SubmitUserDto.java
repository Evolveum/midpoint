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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

/**
 * @author mserbak
 */
public class SubmitUserDto implements Serializable {
	private String attribute;
	private String oldValue;
	private String newValue;
	private boolean secondaryValue;
	private String originType;

	public SubmitUserDto(String attribute, String oldValue, String newValue, String originType, boolean secondaryValue) {
		this.attribute = attribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.originType = originType;
		this.secondaryValue = secondaryValue;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	public boolean isSecondaryValue() {
		return secondaryValue;
	}
	
	public String getOriginType() {
		return originType;
	}

	public boolean isDeletedValue() {
		return null == newValue || newValue.equals("");
	}
}
