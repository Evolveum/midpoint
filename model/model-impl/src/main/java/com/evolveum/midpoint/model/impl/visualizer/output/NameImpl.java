/*
 * Copyright (c) 2010-2016 Evolveum
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
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author mederly
 */
public class NameImpl implements Name {

	private final String simpleName;
	private String displayName;
	private String id;
	private String description;

	public NameImpl(String simpleName) {
		Validate.notNull(simpleName);
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
	public String toString() {
		return toDebugDump();
	}

	public String toDebugDump() {
		return simpleName + " (" + displayName + "), id=" + id;
	}
}
