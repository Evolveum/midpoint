/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Defines value selectors and their clauses.
 *
 * The selector and each clause contains the logic needed to determine its applicability,
 * see {@link com.evolveum.midpoint.schema.selector.spec.ValueSelector#matches(
 * com.evolveum.midpoint.prism.PrismValue, com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext)}
 * and {@link com.evolveum.midpoint.schema.selector.spec.ValueSelector#applyFilters(
 * com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext)} methods.
 */
package com.evolveum.midpoint.schema.selector.spec;
