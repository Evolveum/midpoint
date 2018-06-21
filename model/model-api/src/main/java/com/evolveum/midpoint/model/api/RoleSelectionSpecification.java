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
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DisplayableValue;

/**
 * @author semancik
 *
 */
public class RoleSelectionSpecification {

	private List<? extends DisplayableValue<String>> roleTypes = null;
	private ObjectFilter filter = null;

	/**
	 * Returns null if there is no information about role types that can or cannot be assigned.
     * Returns empty list list if the user is not authorized to assign anything.
	 */
	public List<? extends DisplayableValue<String>> getRoleTypes() {
		return roleTypes;
	}

	public void setNoRoleTypes() {
		roleTypes = new ArrayList<>();
	}

	public void addRoleType(DisplayableValue<String> roleType) {
		if (roleTypes == null) {
			roleTypes = new ArrayList<>();
		}
		((Collection)roleTypes).add(roleType);
	}

	public void addRoleTypes(Collection<? extends DisplayableValue<String>> roleTypes) {
		if (this.roleTypes == null) {
			this.roleTypes = new ArrayList<>();
		}
		this.roleTypes.addAll((Collection)roleTypes);
	}

	/**
	 * Returns "additional filter" that should be used to search for assignible roles.
	 * This filter should be AND-ed with any application level filter.
	 * It can return null. The null filter means "ALL" (AllFilter).
	 * If this returns NoneFilter then no roles can be assigned to the user.
	 */
	public ObjectFilter getFilter() {
		return filter;
	}

	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((roleTypes == null) ? 0 : roleTypes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoleSelectionSpecification other = (RoleSelectionSpecification) obj;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (roleTypes == null) {
			if (other.roleTypes != null)
				return false;
		} else if (!roleTypes.equals(other.roleTypes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RoleSelectionSpecification(" + roleTypes + ": "+filter+")";
	}


}
