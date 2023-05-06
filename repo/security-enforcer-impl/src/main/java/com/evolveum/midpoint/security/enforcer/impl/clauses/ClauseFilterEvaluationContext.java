/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.clauses;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Provides functionality specific for filter processing. */
public interface ClauseFilterEvaluationContext extends ClauseEvaluationContext {

    void addConjunction(ObjectFilter increment);

    @NotNull ObjectFilterExpressionEvaluator createFilterEvaluator();

    @NotNull PrismObjectDefinition<?> getObjectDefinition();

    @NotNull Class<? extends ObjectType> getObjectType();

    @Nullable ObjectFilter getOriginalFilter();

    boolean maySkipOnSearch();
}
