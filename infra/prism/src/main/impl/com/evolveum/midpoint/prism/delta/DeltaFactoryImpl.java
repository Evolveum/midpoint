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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class DeltaFactoryImpl implements DeltaFactory {

	@NotNull private final PrismContext prismContext;

	public DeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public <T> DeltaSetTriple<T> createDeltaSetTriple() {
		return new DeltaSetTripleImpl<>();
	}

	@Override
	public <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple() {
		return new PrismValueDeltaSetTripleImpl<>();
	}

	@Override
	public <O extends Objectable> ObjectDelta<O> createObjectDelta(Class<O> type, ChangeType changeType) {
		return new ObjectDeltaImpl<>(type, changeType, prismContext);
	}

	@Override
	public <T> PropertyDelta<T> createPropertyDelta(ItemPath path, PrismPropertyDefinition<T> definition) {
		return new PropertyDeltaImpl<>(path, definition, prismContext);
	}
}
