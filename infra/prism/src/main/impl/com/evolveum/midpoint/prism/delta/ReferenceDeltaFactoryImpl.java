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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public class ReferenceDeltaFactoryImpl implements DeltaFactory.Reference {

	@NotNull private final PrismContext prismContext;

	public ReferenceDeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public ReferenceDelta create(ItemPath path, PrismReferenceDefinition definition) {
		return new ReferenceDeltaImpl(path, definition, prismContext);
	}

	@Override
	public ReferenceDelta createModificationReplace(QName name, PrismObjectDefinition<? extends Objectable> objectDefinition,
			PrismReferenceValue referenceValue) {
		return ReferenceDeltaImpl.createModificationReplace(ItemName.fromQName(name), objectDefinition, referenceValue);
	}
}
