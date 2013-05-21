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
package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class SimpleDelta<T> {

	private DeltaType type;
	private Collection<T> change;

	public SimpleDelta() {
		super();
		// Construct list so it will maintain ordering
		// but we don't guarantee that. This is more-or-less just
		// for esthetic reasons (e.g. debug output).
		change = new ArrayList<T>();
	}

	public SimpleDelta(DeltaType type) {
		super();
		// Construct list so it will maintain ordering
		// but we don't guarantee that. This is more-or-less just
		// for esthetic reasons (e.g. debug output).
		change = new ArrayList<T>();
		this.type = type;
	}

	public DeltaType getType() {
		return type;
	}

	public void setType(DeltaType type) {
		this.type = type;
	}

	public Collection<T> getChange() {
		return change;
	}
	
	public void add(T object) {
		change.add(object);
	}
	
	@Override
	public String toString() {
		return "Delta(" + type + ",[" + change + "])";
	}

	public enum DeltaType {
		ADD, DELETE;
	}
	
}
