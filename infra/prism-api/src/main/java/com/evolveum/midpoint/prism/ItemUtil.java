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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.PropertyDelta;

/**
 *
 */
public class ItemUtil {
	public static <T> PropertyDelta<T> diff(PrismProperty<T> a, PrismProperty<T> b) {
			if (a == null) {
				if (b == null) {
					return null;
				}
				PropertyDelta<T> delta = b.createDelta();
				delta.addValuesToAdd(PrismValueCollectionsUtil.cloneCollection(b.getValues()));
				return delta;
			} else {
				return a.diff(b);
			}
		}

	public static <T> T getRealValue(PrismProperty<T> property) {
    	return property != null ? property.getRealValue() : null;
	}
}
