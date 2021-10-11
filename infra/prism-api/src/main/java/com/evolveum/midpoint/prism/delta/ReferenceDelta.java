/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public interface ReferenceDelta extends ItemDelta<PrismReferenceValue,PrismReferenceDefinition> {

    @Override
    Class<PrismReference> getItemClass();

    @Override
    void setDefinition(PrismReferenceDefinition definition);

    @Override
    void applyDefinition(PrismReferenceDefinition definition) throws SchemaException;

    boolean isApplicableToType(Item item);

    @Override
    ReferenceDelta clone();

}
