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
 *  Implementation of EquivalenceStrategy that uses a parametrization of built-in equals/hashCode/diff methods.
 *
 *  These strategies are still in progress and (most probably) will be changed before 4.0 release.
 *
 *  The difference between REAL_VALUE and IGNORE_METADATA is to be established yet.
 *
 *  Basically, REAL_VALUE is oriented towards the effective content of the item or value.
 *  Contrary to IGNORE_METADATA it ignores element names and reference filters (if OID is present).
 */
public class ParameterizedEquivalenceStrategy implements EquivalenceStrategy {

	/**
	 * The (almost) highest level of recognition. Useful e.g. for comparing values for the purpose of XML editing.
	 * Still, ignores e.g. definitions, parent objects, immutability flag, etc.
	 *
	 * Corresponds to pre-4.0 flags ignoreMetadata = false, literal = true.
	 */
	public static final ParameterizedEquivalenceStrategy LITERAL;

	/**
	 * As LITERAL but ignores XML namespace prefixes.
	 * Also fills-in default relation name if not present (when comparing reference values).
	 *
	 * Currently this is the default for equals/hashCode.
	 *
	 * Corresponds to pre-4.0 flags ignoreMetadata = false, literal = false.
	 */
	public static final ParameterizedEquivalenceStrategy NOT_LITERAL;

	/**
	 * Ignores metadata, typically operational items and values, container IDs, and origin information.
	 *
	 * Currently this is the default for diff.
	 *
	 * Corresponds to pre-4.0 flags ignoreMetadata = true, literal = false.
	 */
	public static final ParameterizedEquivalenceStrategy IGNORE_METADATA;

	/**
	 * As IGNORE_METADATA, but takes XML namespace prefixes into account.
	 *
	 * It is not clear in which situations this should be needed. But we include it here for compatibility reasons.
	 * Historically it is used on a few places in midPoint.
	 *
	 * Corresponds to pre-4.0 flags ignoreMetadata = true, literal = true.
	 */
	public static final ParameterizedEquivalenceStrategy LITERAL_IGNORE_METADATA;

	/**
	 * Compares the real content if prism structures.
	 * Corresponds to "equalsRealValue" method used in pre-4.0.
	 *
	 * It is to be seen if operational data should be considered in this mode (they are ignored now).
	 * So, currently this is the most lax way of determining equivalence.
	 */
	public static final ParameterizedEquivalenceStrategy REAL_VALUE;

	static {
		LITERAL = new ParameterizedEquivalenceStrategy();
		LITERAL.compareElementNames = true;
		LITERAL.consideringValueOrigin = true;
		LITERAL.literalDomComparison = true;
		LITERAL.consideringContainerIds = true;
		LITERAL.consideringOperationalData = true;
		LITERAL.consideringReferenceFilters = true;

		NOT_LITERAL = new ParameterizedEquivalenceStrategy();
		NOT_LITERAL.compareElementNames = true;
		NOT_LITERAL.consideringValueOrigin = true;
		NOT_LITERAL.literalDomComparison = false;
		NOT_LITERAL.consideringContainerIds = true;
		NOT_LITERAL.consideringOperationalData = true;
		NOT_LITERAL.consideringReferenceFilters = true;

		IGNORE_METADATA = new ParameterizedEquivalenceStrategy();
		IGNORE_METADATA.compareElementNames = true; //???
		IGNORE_METADATA.consideringValueOrigin = false;
		IGNORE_METADATA.literalDomComparison = false;
		IGNORE_METADATA.consideringContainerIds = false;
		IGNORE_METADATA.consideringOperationalData = false;
		IGNORE_METADATA.consideringReferenceFilters = true;

		LITERAL_IGNORE_METADATA = new ParameterizedEquivalenceStrategy();
		LITERAL_IGNORE_METADATA.compareElementNames = true;
		LITERAL_IGNORE_METADATA.consideringValueOrigin = false;
		LITERAL_IGNORE_METADATA.literalDomComparison = true;
		LITERAL_IGNORE_METADATA.consideringContainerIds = false;
		LITERAL_IGNORE_METADATA.consideringOperationalData = false;
		LITERAL_IGNORE_METADATA.consideringReferenceFilters = true;

		REAL_VALUE = new ParameterizedEquivalenceStrategy();
		REAL_VALUE.compareElementNames = false;
		REAL_VALUE.consideringValueOrigin = false;
		REAL_VALUE.literalDomComparison = false;
		REAL_VALUE.consideringContainerIds = false;
		REAL_VALUE.consideringOperationalData = false;
		REAL_VALUE.consideringReferenceFilters = false;
	}

	private boolean compareElementNames;
	private boolean consideringValueOrigin;
	private boolean literalDomComparison;
	private boolean consideringContainerIds;
	private boolean consideringOperationalData;
	private boolean consideringReferenceFilters;

	@Override
	public boolean equals(Item<?, ?> first, Item<?, ?> second) {
		return first == second || first != null && second != null && first.equals(second, this);
	}

	@Override
	public boolean equals(PrismValue first, PrismValue second) {
		return first == second || first != null && second != null && first.equals(second, this);
	}

	@Override
	public int hashCode(Item<?, ?> item) {
		return item != null ? item.hashCode(this) : 0;
	}

	@Override
	public int hashCode(PrismValue value) {
		return value != null ? value.hashCode(this) : 0;
	}

	public boolean isConsideringDefinitions() {
		return false;
	}

	public boolean isConsideringElementNames() {
		return compareElementNames;
	}

	public void setCompareElementNames(boolean compareElementNames) {
		this.compareElementNames = compareElementNames;
	}

	public boolean isConsideringValueOrigin() {
		return consideringValueOrigin;
	}

	public void setConsideringValueOrigin(boolean consideringValueOrigin) {
		this.consideringValueOrigin = consideringValueOrigin;
	}

	public boolean isLiteralDomComparison() {
		return literalDomComparison;
	}

	public void setLiteralDomComparison(boolean literalDomComparison) {
		this.literalDomComparison = literalDomComparison;
	}

	public boolean isConsideringContainerIds() {
		return consideringContainerIds;
	}

	public void setConsideringContainerIds(boolean consideringContainerIds) {
		this.consideringContainerIds = consideringContainerIds;
	}

	public boolean isConsideringOperationalData() {
		return consideringOperationalData;
	}

	public void setConsideringOperationalData(boolean consideringOperationalData) {
		this.consideringOperationalData = consideringOperationalData;
	}

	public boolean isConsideringReferenceFilters() {
		return consideringReferenceFilters;
	}

	public void setConsideringReferenceFilters(boolean consideringReferenceFilters) {
		this.consideringReferenceFilters = consideringReferenceFilters;
	}
}
