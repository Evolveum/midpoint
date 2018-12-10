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
