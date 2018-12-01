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

import com.evolveum.midpoint.prism.xnode.XNode;

import javax.xml.namespace.QName;

/**
 *  Factory for items (property, reference, container, object) and item values.
 *
 *  Eliminates the need of calls like "new PrismPropertyValue(...)" in midPoint 3.x.
 */
public interface ItemFactory {

	PrismValue createPrismValue(Object realValue);

	<T> PrismProperty<T> createPrismProperty(QName itemName);

	<T> PrismPropertyValue<T> createPrismPropertyValue(T content);

	<T> PrismPropertyValue<T> createPrismPropertyValue(XNode rawContent);

	PrismReferenceValue createPrismReferenceValue(PrismObject<?> target);

	PrismReferenceValue createPrismReferenceValue(String targetOid);
}
