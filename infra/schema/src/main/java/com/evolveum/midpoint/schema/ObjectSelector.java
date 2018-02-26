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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.ShortDumpable;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class ObjectSelector implements Serializable, ShortDumpable {

	private ItemPath path;

	public ObjectSelector(ItemPath path) {
		super();
		this.path = path;
	}

	public ItemPath getPath() {
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

}
