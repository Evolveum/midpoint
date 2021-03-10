/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ReferenceSqlTransformer
        extends SqaleTransformerBase<ObjectReferenceType, QReference, MReference> {

    public ReferenceSqlTransformer(
            SqlTransformerSupport transformerSupport, QReferenceMapping mapping) {
        super(transformerSupport, mapping);
    }

    public MReference toRowObject(ObjectReferenceType schemaObject, JdbcSession jdbcSession) {
        MReference row = new MReference();
        // TODO ownerOid, referenceType
        row.relationId = resolveRelationToId(schemaObject.getRelation());
        row.targetOid = UUID.fromString(schemaObject.getOid());
        row.targetType = schemaTypeToCode(schemaObject.getType());
        return row;
    }
}
