/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
