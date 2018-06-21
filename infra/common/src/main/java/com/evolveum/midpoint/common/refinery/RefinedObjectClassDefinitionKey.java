/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import javax.xml.namespace.QName;


/**
 * @author semancik
 *
 */
class RefinedObjectClassDefinitionKey {
	private QName typeName;
	private String intent;

	public RefinedObjectClassDefinitionKey(RefinedObjectClassDefinition rObjectClassDefinition) {
		typeName = rObjectClassDefinition.getTypeName();
		intent = rObjectClassDefinition.getIntent();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intent == null) ? 0 : intent.hashCode());
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RefinedObjectClassDefinitionKey other = (RefinedObjectClassDefinitionKey) obj;
		if (intent == null) {
			if (other.intent != null) {
				return false;
			}
		} else if (!intent.equals(other.intent)) {
			return false;
		}
		if (typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!typeName.equals(other.typeName)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(type=" + typeName + ", intent=" + intent + ")";
	}
}
