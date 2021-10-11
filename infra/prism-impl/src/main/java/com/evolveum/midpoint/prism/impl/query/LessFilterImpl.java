/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.LessFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public final class LessFilterImpl<T> extends ComparativeFilterImpl<T> implements LessFilter<T> {

    private LessFilterImpl(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
            @Nullable QName matchingRule,
            @Nullable PrismPropertyValue<T> value, @Nullable ExpressionWrapper expression,
            @Nullable ItemPath rightHandSidePath, @Nullable ItemDefinition rightHandSideDefinition, boolean equals) {
        super(path, definition, matchingRule, value, expression, rightHandSidePath, rightHandSideDefinition, equals);
    }

    // factory methods

    // empty (can be filled-in later)
    @NotNull
    public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> definition, boolean equals) {
        return new LessFilterImpl<>(itemPath, definition, null, null, null, null, null, equals);
    }

    // value
    @NotNull
    public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> definition,
            QName matchingRule, Object anyValue, boolean equals, @NotNull PrismContext prismContext) {
        PrismPropertyValue<T> propertyValue = anyValueToPropertyValue(prismContext, anyValue);
        return new LessFilterImpl<>(itemPath, definition, matchingRule, propertyValue, null, null, null, equals);
    }

    // expression-related
    @NotNull
    public static <T> LessFilter<T> createLess(@NotNull ItemPath itemPath, PrismPropertyDefinition<T> itemDefinition, QName matchingRule,
            @NotNull ExpressionWrapper expressionWrapper, boolean equals) {
        return new LessFilterImpl<>(itemPath, itemDefinition, matchingRule, null, expressionWrapper, null, null, equals);
    }

    // right-side-related
    @NotNull
    public static <T> LessFilter<T> createLess(@NotNull ItemPath propertyPath, PrismPropertyDefinition<T> definition,
            QName matchingRule, @NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
        return new LessFilterImpl<>(propertyPath, definition, matchingRule, null, null, rightSidePath, rightSideDefinition, equals);
    }

    @Override
    public LessFilterImpl<T> clone() {
        return new LessFilterImpl<>(getFullPath(), getDefinition(), getMatchingRule(), getClonedValue(), getExpression(),
                getRightHandSidePath(), getRightHandSideDefinition(), isEquals());
    }

    @Override
    protected String getFilterName() {
        return isEquals() ? "LESS-OR-EQUAL" : "LESS";
    }

    @Override
    public boolean equals(Object obj, boolean exact) {
        return obj instanceof LessFilter && super.equals(obj, exact);
    }

    @Override
    boolean processComparisonResult(int result) {
        return isEquals() ? result <= 0 : result < 0;
    }
}
