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
public class DeltaFactoryImpl implements DeltaFactory {

	@NotNull private final PrismContext prismContext;
	@NotNull private final PropertyDeltaFactoryImpl propertyDeltaFactory;
	@NotNull private final ReferenceDeltaFactoryImpl referenceDeltaFactory;
	@NotNull private final ContainerDeltaFactoryImpl containerDeltaFactory;
	@NotNull private final ObjectDeltaFactoryImpl objectDeltaFactory;

	public DeltaFactoryImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
		this.propertyDeltaFactory = new PropertyDeltaFactoryImpl(prismContext);
		this.referenceDeltaFactory = new ReferenceDeltaFactoryImpl(prismContext);
		this.containerDeltaFactory = new ContainerDeltaFactoryImpl(prismContext);
		this.objectDeltaFactory = new ObjectDeltaFactoryImpl(prismContext);
	}

	@Override
	public Property property() {
		return propertyDeltaFactory;
	}

	@Override
	public Reference reference() {
		return referenceDeltaFactory;
	}

	@Override
	public Container container() {
		return containerDeltaFactory;
	}

	@Override
	public Object object() {
		return objectDeltaFactory;
	}

	@Override
	public <T> DeltaSetTriple<T> createDeltaSetTriple() {
		return new DeltaSetTripleImpl<>();
	}

	@Override
	public <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple() {
		return new PrismValueDeltaSetTripleImpl<>();
	}

}
