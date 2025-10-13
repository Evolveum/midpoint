/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectReferenceMapper extends ReferenceMapper<RObjectReference> {

    @Override
    public RObjectReference map(Referencable input, MapperContext context) {
        ObjectReferenceType objectRef = buildReference(input);

        ObjectTypeUtil.normalizeRelation(objectRef, context.getRelationRegistry());

        RObject owner = (RObject) context.getOwner();

        Class<? extends ObjectType> jaxbObjectType = RObjectType.getType(owner.getClass()).getJaxbClass();

        RReferenceType refType = RReferenceType.getOwnerByQName(jaxbObjectType, context.getDelta().getPath().lastName());

        return RUtil.jaxbRefToRepo(objectRef, owner, refType, context.getRelationRegistry());
    }
}

