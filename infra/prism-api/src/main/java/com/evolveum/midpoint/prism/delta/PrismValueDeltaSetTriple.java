/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * DeltaSetTriple that is limited to hold prism values. By limiting to the PrismValue descendants we gain advantage to be
 * cloneable and ability to compare real values.
 *
 * @author Radovan Semancik
 */
public interface PrismValueDeltaSetTriple<V extends PrismValue> extends DeltaSetTriple<V>, Visitable {

    /**
     * Distributes a value in this triple similar to the placement of other value in the other triple.
     * E.g. if the value "otherMember" is in the zero set in "otherTriple" then "myMember" will be placed
     * in zero set in this triple.
     */
    <O extends PrismValue> void distributeAs(V myMember, PrismValueDeltaSetTriple<O> otherTriple, O otherMember);

    Class<V> getValueClass();

    Class<?> getRealValueClass();

    boolean isRaw();

    void applyDefinition(ItemDefinition itemDefinition) throws SchemaException;

    /**
     * Sets specified source type for all values in all sets
     */
    void setOriginType(OriginType sourceType);

    /**
     * Sets specified origin object for all values in all sets
     */
    void setOriginObject(Objectable originObject);

    void removeEmptyValues(boolean allowEmptyValues);

    PrismValueDeltaSetTriple<V> clone();

    void checkConsistence();

    @Override
    void accept(Visitor visitor);

    void checkNoParent();

}
