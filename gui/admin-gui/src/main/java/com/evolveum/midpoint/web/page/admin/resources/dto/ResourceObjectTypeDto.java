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

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;

/**
 * @author lazyman
 */
public class ResourceObjectTypeDto implements Serializable {

	private static final long serialVersionUID = 4664988785770149299L;
	private String displayName;
	private String nativeObjectClass;
	private String help;
	private String type;

	public ResourceObjectTypeDto(ObjectClassComplexTypeDefinition definition) {
		Validate.notNull(definition, "Resource object definition can't be null.");

		displayName = definition.getDisplayName();
		if(displayName == null){
			displayName = "-";
		}

		nativeObjectClass = definition.getNativeObjectClass();
		help = definition.getHelp();
		if (definition.getTypeName() != null) {
			this.type = definition.getTypeName().getLocalPart();
		}
	}

	public String getDisplayName() {
		if (displayName == null) {
			return "";
		}
		return displayName;
	}

	public String getNativeObjectClass() {
		if (nativeObjectClass == null) {
			return "";
		}
		return nativeObjectClass;
	}

	public String getHelp() {
		if (help == null) {
			return "";
		}
		return help;
	}

	public String getType() {
		return type;
	}
}
