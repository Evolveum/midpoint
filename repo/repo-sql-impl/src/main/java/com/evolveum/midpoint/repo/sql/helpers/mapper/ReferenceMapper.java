/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
