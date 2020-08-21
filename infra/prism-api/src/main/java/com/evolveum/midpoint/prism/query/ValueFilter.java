/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.query;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ValueFilter<V extends PrismValue, D extends ItemDefinition>
        extends ObjectFilter, ItemFilter, Itemable {

    @NotNull
    @Override
    ItemPath getFullPath();

    @NotNull
    ItemPath getParentPath();

    @NotNull
    ItemName getElementName();

    @Nullable
    D getDefinition();

    void setDefinition(@Nullable D definition);

    @Nullable
    QName getMatchingRule();

    void setMatchingRule(@Nullable QName matchingRule);

    @Nullable
    List<V> getValues();

    /**
     * Returns single value or {@code null}, throws exception if multiple values are present.
     */
    @Nullable
    V getSingleValue();

    /**
     * @param value value, has to be parent-less
     */
    void setValue(V value);

    @Nullable
    ExpressionWrapper getExpression();

    void setExpression(@Nullable ExpressionWrapper expression);

    @Nullable
    ItemPath getRightHandSidePath();

    void setRightHandSidePath(@Nullable ItemPath rightHandSidePath);

    @Nullable
    ItemDefinition getRightHandSideDefinition();

    void setRightHandSideDefinition(@Nullable ItemDefinition rightHandSideDefinition);

    @Override
    ItemPath getPath();

    boolean isRaw();

    // TODO revise
    @Override
    boolean match(PrismContainerValue cvalue, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException;

    //@NotNull
    //Collection<PrismValue> getObjectItemValues(PrismContainerValue value);

    // TODO revise
//    @NotNull
//    Item getFilterItem() throws SchemaException;

    @Override
    ValueFilter<V, D> clone();

    @Override
    boolean equals(Object o, boolean exact);

    //String getFilterName();

    //void debugDump(int indent, StringBuilder sb);

    //String toString(StringBuilder sb){

    @Override
    void checkConsistence(boolean requireDefinitions);
}
