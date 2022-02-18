/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import java.io.Serializable;

/**
 * Mapping prepared for evaluation (or already evaluated).
 * It is a "parsed" mapping, in contrast with {@link MappingType} bean that contains the mapping definition.
 *
 * @param <V> type of {@link PrismValue} the mapping produces
 * @param <D> type of {@link ItemDefinition} of the item the mapping produces (currently unused!)
 *
 * @author semancik
 */
public interface Mapping<V extends PrismValue, D extends ItemDefinition<?>> extends Serializable {

    /**
     * Returns elapsed time in milliseconds.
     */
    Long getEtime();

    <T extends Serializable> T getStateProperty(String propertyName);

    <T extends Serializable> T setStateProperty(String propertyName, T value);

    PrismValueDeltaSetTriple<V> getOutputTriple();

    ItemPath getOutputPath() throws SchemaException;

    /**
     * Returns true if the condition is at least partially satisfied, i.e. it is not "false -> false".
     *
     * Precondition: the condition is evaluated. Otherwise a {@link NullPointerException} is thrown.
     */
    @Experimental
    boolean isConditionSatisfied();
}
