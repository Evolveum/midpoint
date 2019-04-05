/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UcfChangeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 *
 */
@SuppressWarnings("unused")
public class UcfChangeUtil {

	public static UcfChangeType createForNewObject(QName objectClassName, Map<QName, Object> attributes,
			PrismContext prismContext) throws SchemaException {
		ShadowType shadow = new ShadowType(prismContext);
		copyAttributes(attributes, shadow.asPrismObject().findOrCreateContainer(ShadowType.F_ATTRIBUTES).getValue(), prismContext);
		UcfChangeType change = new UcfChangeType();
		ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(shadow.asPrismObject());
		change.setObjectClass(objectClassName);
		change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
		return change;
	}

	private static void copyAttributes(Map<QName, Object> attributes, PrismContainerValue<?> target, PrismContext prismContext)
			throws SchemaException {
		for (Map.Entry<QName, Object> entry : attributes.entrySet()) {
			PrismProperty<Object> attribute = prismContext.itemFactory().createProperty(entry.getKey());
			attribute.setValue(prismContext.itemFactory().createPropertyValue(entry.getValue()));
			target.add(attribute);
		}
	}

	public static UcfChangeType create(QName objectClassName, Map<QName, Object> identifiers, ObjectDeltaType delta, PrismContext prismContext)
			throws SchemaException {
		UcfChangeType change = new UcfChangeType();
		change.setObjectClass(objectClassName);
		change.setIdentifiers(new ShadowAttributesType(prismContext));
		copyAttributes(identifiers, change.getIdentifiers().asPrismContainerValue(), prismContext);
		change.setObjectDelta(delta);
		return change;
	}
}
