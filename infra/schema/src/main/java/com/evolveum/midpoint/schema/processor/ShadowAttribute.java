/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Access to both {@link ShadowSimpleAttribute} and {@link ShadowReferenceAttribute}.
 */
public interface ShadowAttribute<
        V extends PrismValue,
        D extends ShadowAttributeDefinition<V, D, RV, SA>,
        RV,
        SA extends ShadowAttribute<V, D, RV, SA>> extends ShortDumpable {

    ItemName getElementName();

    D getDefinition();

    ShadowAttribute<V, D, RV, SA> clone();

    void setIncomplete(boolean incomplete);

    boolean isIncomplete();

    boolean hasNoValues();

    void addValueSkipUniquenessCheck(V value) throws SchemaException;

    SA createImmutableClone();

    ItemDelta<?, ?> createDelta();

    ItemDelta<?, ?> createDelta(ItemPath path);

    SA cloneComplex(CloneStrategy strategy);

    // No-op if there's the same attribute definition already
    void applyDefinitionFrom(ResourceObjectDefinition objectDefinition) throws SchemaException;

    D getDefinitionRequired();

    ItemDelta<?, ?> createReplaceDelta();

    /** Correctly typed return value. The getValues() method is not typed like this due to parameterized types chaos. */
    List<V> getAttributeValues();

    @Override
    default void shortDump(StringBuilder sb) {
        sb.append(getElementName().getLocalPart());
        var values = getAttributeValues();
        if (values.isEmpty()) {
            sb.append(" (no values)");
        } else {
            sb.append("=");
            if (values.size() == 1) {
                sb.append(shortDumpValue(values.get(0)));
            } else {
                sb.append(
                        values.stream()
                                .map(v -> shortDumpValue(v))
                                .collect(Collectors.joining(", ", "[", "]")));
            }
        }
    }

    private String shortDumpValue(Object v) {
        if (v instanceof ShortDumpable shortDumpable) {
            return shortDumpable.shortDump();
        } else if (v instanceof PrismPropertyValue<?> ppv) {
            var real = ppv.getRealValue();
            if (real != null) {
                return PrettyPrinter.prettyPrint(real);
            } else {
                return ppv.toHumanReadableString();
            }
        } else { // should not occur
            return String.valueOf(v);
        }
    }

    default void clearParent() {
        ((Item<?, ?>) this).setParent(null);
    }
}
