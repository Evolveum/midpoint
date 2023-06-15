/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Provides all the necessary support for evaluating selectors and their clauses, i.e. for calling methods:
 *
 * - {@link com.evolveum.midpoint.schema.selector.spec.ValueSelector#matches(PrismValue, MatchingContext)}
 * - {@link com.evolveum.midpoint.schema.selector.spec.ValueSelector#toFilter(FilteringContext)}
 *
 * I.e., contains the evaluation context objects and all their components.
 * There is quite a number of them, as the evaluation may be complex for e.g. subjected selector clauses.
 */
package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismValue;
