/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.Name;

/**
 * @author mederly
 */
public class NameImpl implements Name {

	private final String simpleName;
	private String displayName;
	private String id;
	private String description;
	private boolean namesAreResourceKeys;

	public NameImpl(String simpleName) {
		this.simpleName = simpleName;
	}

	@Override
	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
    public boolean namesAreResourceKeys() {
		return namesAreResourceKeys;
	}

	public void setNamesAreResourceKeys(boolean namesAreResourceKeys) {
		this.namesAreResourceKeys = namesAreResourceKeys;
	}

	@Override
	public String toString() {
		return toDebugDump();
	}

	public String toDebugDump() {
		return simpleName + " (" + displayName + "), id=" + id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NameImpl name = (NameImpl) o;

		if (namesAreResourceKeys != name.namesAreResourceKeys) {
			return false;
		}
		if (simpleName != null ? !simpleName.equals(name.simpleName) : name.simpleName != null) return false;
		if (displayName != null ? !displayName.equals(name.displayName) : name.displayName != null) return false;
		if (id != null ? !id.equals(name.id) : name.id != null) return false;
		return !(description != null ? !description.equals(name.description) : name.description != null);

	}

	@Override
	public int hashCode() {
		int result = simpleName != null ? simpleName.hashCode() : 0;
		result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		return result;
	}
}
