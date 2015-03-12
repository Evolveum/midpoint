/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.model.api;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DisplayableValue;

/**
 * @author semancik
 *
 */
public class RoleSelectionSpecification {
	
	private List<DisplayableValue<String>> roleTypes = new ArrayList<DisplayableValue<String>>();

	public List<DisplayableValue<String>> getRoleTypes() {
		return roleTypes;
	}

	public void addRoleType(DisplayableValue<String> roleType) {
		roleTypes.add(roleType);
	}

	@Override
	public String toString() {
		return "RoleSelectionSpecification(" + roleTypes + ")";
	}
	
	
}
