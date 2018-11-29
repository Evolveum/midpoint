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

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 *
 */
public class TaskTestUtil {

	public static ItemDelta<?, ?> createExtensionDelta(PrismPropertyDefinition definition, Object realValue) {
		PrismProperty<Object> property = (PrismProperty<Object>) definition.instantiate();
		property.setRealValue(realValue);
		return PropertyDeltaImpl
				.createModificationReplaceProperty(ItemPath.create(TaskType.F_EXTENSION, property.getElementName()), definition, realValue);
	}


}
