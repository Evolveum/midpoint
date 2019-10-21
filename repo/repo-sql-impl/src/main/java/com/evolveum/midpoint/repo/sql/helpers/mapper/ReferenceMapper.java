/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ReferenceMapper<T> implements Mapper<Referencable, T> {

    protected ObjectReferenceType buildReference(Referencable input) {
        ObjectReferenceType objectRef;
        if (input instanceof ObjectReferenceType) {
            objectRef = (ObjectReferenceType) input;
        } else {
            objectRef = new ObjectReferenceType();
            objectRef.setupReferenceValue(input.asReferenceValue());
        }

        return objectRef;
    }
}
