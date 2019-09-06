/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * TEMPORARY. Unify with ItemDeltaCollectionsUtil
 */
public class PropertyDeltaCollectionsUtil {

	public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> modifications, ItemPath propertyPath) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getPath().equivalent(propertyPath)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }

	public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> modifications, QName propertyName) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getParentPath().isEmpty() &&
    				QNameUtil.match(delta.getElementName(), propertyName)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }
}
