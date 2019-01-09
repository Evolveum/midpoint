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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_4.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_4.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_4.ObjectDeltaType;

import java.util.Collection;

/**
 *
 */
public class ObjectDeltaUtil {
	public static boolean isEmpty(ObjectDeltaType deltaType) {
		if (deltaType == null) {
			return true;
		}
		if (deltaType.getChangeType() == ChangeTypeType.DELETE) {
			return false;
		} else if (deltaType.getChangeType() == ChangeTypeType.ADD) {
			return deltaType.getObjectToAdd() == null || deltaType.getObjectToAdd().asPrismObject().isEmpty();
		} else {
			for (ItemDeltaType itemDeltaType : deltaType.getItemDelta()) {
				if (!ItemDeltaUtil.isEmpty(itemDeltaType)) {
					return false;
				}
			}
			return true;
		}
	}

	public static <O extends Objectable> void applyTo(
			PrismObject<O> targetObject, Collection<? extends ItemDelta<?,?>> modifications) throws SchemaException {
		for (ItemDelta itemDelta : modifications) {
	        itemDelta.applyTo(targetObject);
	    }
	}
}
