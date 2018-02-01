/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectReferenceMapper implements Mapper<Referencable, RObjectReference> {

    @Override
    public RObjectReference map(Referencable input, MapperContext context) {
        ObjectReferenceType objectRef;
        if (input instanceof ObjectReferenceType) {
            objectRef = (ObjectReferenceType) input;
        } else {
            objectRef = new ObjectReferenceType();
            objectRef.setupReferenceValue(input.asReferenceValue());
        }

        ObjectTypeUtil.normalizeRelation(objectRef);

        RObject owner = (RObject) context.getOwner();

        Class<? extends ObjectType> jaxbObjectType = RObjectType.getType(owner.getClass()).getJaxbClass();

        ItemPath named = context.getDelta().getPath().namedSegmentsOnly();
        NameItemPathSegment last = named.lastNamed();
        RReferenceOwner refType = RReferenceOwner.getOwnerByQName(jaxbObjectType, last.getName());

        return RUtil.jaxbRefToRepo(objectRef, context.getPrismContext(), owner, refType);
    }
}

