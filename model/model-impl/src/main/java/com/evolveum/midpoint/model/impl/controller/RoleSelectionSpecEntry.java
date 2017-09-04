/**
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.DisplayableValueImpl;

/**
 * @author semancik
 *
 */
public class RoleSelectionSpecEntry extends DisplayableValueImpl<String> {

	boolean negative = false;

	public RoleSelectionSpecEntry(String value, String label, String description) {
		super(value, label, description);
	}

	public boolean isNegative() {
		return negative;
	}

	public void setNegative(boolean negative) {
		this.negative = negative;
	}

	public void negate() {
		this.negative = !this.negative;
	}

	public static void negate(Collection<RoleSelectionSpecEntry> col) {
		if (col == null) {
			return;
		}
		for (RoleSelectionSpecEntry entry: col) {
			entry.negate();
		}
	}

	public static boolean hasNegative(Collection<RoleSelectionSpecEntry> col) {
		if (col == null) {
			return false;
		}
		for (RoleSelectionSpecEntry entry: col) {
			if (entry.isNegative()) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasNegativeValue(Collection<RoleSelectionSpecEntry> col, String value) {
		if (col == null) {
			return false;
		}
		for (RoleSelectionSpecEntry entry: col) {
			if (entry.isNegative() && value.equals(entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	public static Collection<RoleSelectionSpecEntry> getPositive(Collection<RoleSelectionSpecEntry> col) {
		if (col == null) {
			return null;
		}
		Collection<RoleSelectionSpecEntry> out = new ArrayList<>();
		for (RoleSelectionSpecEntry entry: col) {
			if (!entry.isNegative()) {
				out.add(entry);
			}
		}
		return out;
	}
}
