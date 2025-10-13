/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EmbeddedObjectReferenceMapper extends ReferenceMapper<RSimpleEmbeddedReference> {

    @Override
    public RSimpleEmbeddedReference map(Referencable input, MapperContext context) {
        ObjectReferenceType objectRef = buildReference(input);

        ObjectTypeUtil.normalizeRelation(objectRef, context.getRelationRegistry());

        RSimpleEmbeddedReference rref = new RSimpleEmbeddedReference();
        RSimpleEmbeddedReference.fromJaxb(objectRef, rref, context.getRelationRegistry());

        return rref;
    }
}

