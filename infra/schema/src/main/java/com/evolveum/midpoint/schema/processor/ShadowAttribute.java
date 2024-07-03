/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Access to both {@link ShadowSimpleAttribute} and {@link ShadowReferenceAttribute}.
 */
public interface ShadowAttribute<
        V extends PrismValue,
        D extends ShadowAttributeDefinition<V, D, RV, SA>,
        RV,
        SA extends ShadowAttribute<V, D, RV, SA>> {

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

    void applyDefinitionFrom(ResourceObjectDefinition objectDefinition) throws SchemaException;

    D getDefinitionRequired();

    ItemDelta<?, ?> createReplaceDelta();
}
