/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.ShortDumpable;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author semancik
 *
 */
public class ObjectSelector implements Serializable, ShortDumpable {

	private UniformItemPath path;           // do not change to ItemPath unless equals/hashCode is adapted

	public ObjectSelector(UniformItemPath path) {
		super();
		this.path = path;
	}

	public UniformItemPath getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "ObjectSelector(" + path + ")";
	}

	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(path);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ObjectSelector))
			return false;
		ObjectSelector that = (ObjectSelector) o;
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}
}
