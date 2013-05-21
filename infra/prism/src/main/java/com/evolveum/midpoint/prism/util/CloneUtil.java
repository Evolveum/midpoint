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
package com.evolveum.midpoint.prism.util;

import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;

/**
 * @author semancik
 *
 */
public class CloneUtil {
	
	public static <T> T clone(T orig) {
		if (orig == null) {
			return null;
		}
		Class<? extends Object> origClass = orig.getClass();
		if (origClass.isPrimitive()) {
			return orig;
		}
		if (orig instanceof PolyString) {
			// PolyString is immutable
			return (T)orig;
		}
		if (orig instanceof Item<?>) {
			return (T) ((Item<?>)orig).clone();
		}
		if (orig instanceof PrismValue) {
			return (T) ((PrismValue)orig).clone();
		}
		if (orig instanceof ObjectDelta<?>) {
			return (T) ((ObjectDelta<?>)orig).clone();
		}
		if (orig instanceof ItemDelta<?>) {
			return (T) ((ItemDelta<?>)orig).clone();
		}
		if (orig instanceof Definition) {
			return (T) ((Definition)orig).clone();
		}
		if (orig instanceof Serializable) {
			// Brute force
			return (T)SerializationUtils.clone((Serializable)orig);
		}
		throw new IllegalArgumentException("Cannot clone "+orig+" ("+origClass+")");
	}

}
