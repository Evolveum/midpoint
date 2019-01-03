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

package com.evolveum.midpoint.prism.equivalence;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * A strategy used to determine equivalence of prism items and values.
 *
 * This is quite generic interface. We expect that usually it will not be implemented directly, because comparing prism
 * structures is a complex undertaking. The usual approach will be using ParameterizedEquivalenceStrategy that contains
 * a set of parameters that drive equals/hashCode methods built into prism structures.
 *
 * However, if anyone would need the ultimate flexibility, he is free to implement this interface from scratch.
 *
 * (Note that not all methods in prism API accept this generic form of equivalence strategy. For example, diff(..) methods
 * are limited to ParameterizedEquivalenceStrategy at least for now.)
 *
 * @see ParameterizedEquivalenceStrategy for explanation of individual strategies.
 */

public interface EquivalenceStrategy {

	ParameterizedEquivalenceStrategy LITERAL = ParameterizedEquivalenceStrategy.LITERAL;
	ParameterizedEquivalenceStrategy NOT_LITERAL = ParameterizedEquivalenceStrategy.NOT_LITERAL;
	ParameterizedEquivalenceStrategy IGNORE_METADATA = ParameterizedEquivalenceStrategy.IGNORE_METADATA;
	ParameterizedEquivalenceStrategy IGNORE_METADATA_CONSIDER_DIFFERENT_IDS = ParameterizedEquivalenceStrategy.IGNORE_METADATA_CONSIDER_DIFFERENT_IDS;
	ParameterizedEquivalenceStrategy LITERAL_IGNORE_METADATA = ParameterizedEquivalenceStrategy.LITERAL_IGNORE_METADATA;
	ParameterizedEquivalenceStrategy REAL_VALUE = ParameterizedEquivalenceStrategy.REAL_VALUE;
	ParameterizedEquivalenceStrategy REAL_VALUE_CONSIDER_DIFFERENT_IDS = ParameterizedEquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS;

	boolean equals(Item<?,?> first, Item<?,?> second);
	boolean equals(PrismValue first, PrismValue second);

	int hashCode(Item<?,?> item);
	int hashCode(PrismValue value);
}
