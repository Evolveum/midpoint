/*
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.model.common.mapping;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

public interface PrismValueDeltaSetTripleProducer<V extends PrismValue, D extends ItemDefinition> {

	QName getMappingQName();

	/**
	 * Null output tripple means "the mapping is not applicable", e.g. due to the
	 * condition being false.
	 * Empty output triple means "the mapping is applicable but there are no values".
	 */
	PrismValueDeltaSetTriple<V> getOutputTriple();

	MappingStrengthType getStrength();

	PrismValueDeltaSetTripleProducer<V, D> clone();

	boolean isExclusive();

	boolean isAuthoritative();

	/**
	 * Returns true if the mapping has no source. That means
	 * it has to be evaluated for any delta. This really applies
	 * only to normal-strength mappings.
	 */
	boolean isSourceless();

}