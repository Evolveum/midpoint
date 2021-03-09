/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query.lang;

import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.prism.query.ObjectFilter;

interface FilterSerializer<T extends ObjectFilter> {

    @SuppressWarnings("unchecked")
    default void castAndWrite(ObjectFilter source, QueryWriter target) throws PrismQuerySerialization.NotSupportedException {
        write((T) source, target);
    }

    void write(T source, QueryWriter target) throws PrismQuerySerialization.NotSupportedException;

}
