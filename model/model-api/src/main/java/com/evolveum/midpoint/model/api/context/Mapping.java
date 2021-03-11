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
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public interface Mapping<V extends PrismValue, D extends ItemDefinition> extends Serializable {

    /**
     * Returns elapsed time in milliseconds.
     */
    Long getEtime();

    <T extends Serializable> T getStateProperty(String propertyName);

    <T extends Serializable> T setStateProperty(String propertyName, T value);

    PrismValueDeltaSetTriple<V> getOutputTriple();

    ItemPath getOutputPath() throws SchemaException;
}
