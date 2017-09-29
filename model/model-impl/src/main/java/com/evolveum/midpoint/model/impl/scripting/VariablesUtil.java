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

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class VariablesUtil {

	private static final Trace LOGGER = TraceManager.getTrace(VariablesUtil.class);

	// We create immutable versions of prism variables to avoid unnecessary downstream cloning
	@NotNull
	static Map<String, Object> initialPreparation(Map<String, Object> initialVariables) {
		HashMap<String, Object> rv = new HashMap<>();
		if (initialVariables != null) {
			for (Map.Entry<String, Object> entry : initialVariables.entrySet()) {
				rv.put(entry.getKey(), makeImmutable(entry.getValue()));
			}
		}
		return rv;
	}

	@NotNull
	public static Map<String, Object> cloneIfNecessary(@NotNull Map<String, Object> variables) {
		HashMap<String, Object> rv = new HashMap<>();
		variables.forEach((key, value) -> rv.put(key, cloneIfNecessary(key, value)));
		return rv;
	}

	@Nullable
	public static Object cloneIfNecessary(String name, Object value) {
		if (value instanceof PrismValue || value instanceof Item) {
			return makeImmutable(value);
		} else {
			try {
				return CloneUtil.clone(value);
			} catch (Throwable t) {
				LOGGER.warn("Scripting variable value {} of type {} couldn't be cloned. Using original.", name, value.getClass());
				return value;
			}
		}
	}

	@Nullable
	public static Object makeImmutable(Object value) {
		if (value instanceof PrismValue) {
			PrismValue pval = (PrismValue) value;
			if (!pval.isImmutable()) {
				PrismValue clone = pval.clone();
				clone.setImmutable(true);
				return clone;
			} else {
				return pval;
			}
		} else if (value instanceof Item) {
			Item item = (Item) value;
			if (!item.isImmutable()) {
				Item clone = item.clone();
				clone.setImmutable(true);
				return clone;
			} else {
				return item;
			}
		} else {
			return value;
		}
	}

}
