/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class ObjectSetUtil {

    public static ObjectSetType setFromRef(ObjectReferenceType ref, QName defaultTypeName) {
        if (ref == null) {
            return new ObjectSetType(PrismContext.get())
                    .objectType(defaultTypeName);
        } else if (ref.getOid() != null) {
            return new ObjectSetType(PrismContext.get())
                    .objectType(getTypeName(ref, defaultTypeName))
                    .objectQuery(createOidQuery(ref.getOid()));
        } else {
            return new ObjectSetType(PrismContext.get())
                    .objectType(getTypeName(ref, defaultTypeName))
                    .objectQuery(new QueryType()
                            .filter(ref.getFilter()));
        }
    }

    private static QueryType createOidQuery(@NotNull String oid) {
        try {
            return PrismContext.get().getQueryConverter().createQueryType(
                    PrismContext.get().queryFor(ObjectType.class)
                            .id(oid)
                            .build());
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    private static QName getTypeName(ObjectReferenceType ref, QName defaultTypeName) {
        return ref.getType() != null ? ref.getType() : defaultTypeName;
    }
}
