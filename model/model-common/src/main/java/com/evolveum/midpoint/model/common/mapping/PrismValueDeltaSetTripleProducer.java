/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;

public interface PrismValueDeltaSetTripleProducer<V extends PrismValue, D extends ItemDefinition> extends HumanReadableDescribable {

    QName getMappingQName();

    /**
     * Null output triple means "the mapping is not applicable", e.g. due to the
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

    /**
     * Identifier of this producer; e.g. mapping name.
     */
    String getIdentifier();

    default boolean isStrong() {
        return getStrength() == MappingStrengthType.STRONG;
    }

    default boolean isNormal() {
        return getStrength() == MappingStrengthType.NORMAL;
    }

    default boolean isWeak() {
        return getStrength() == MappingStrengthType.WEAK;
    }
}
