/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

public class ObjectTemplateSqlTransformer
        extends ObjectSqlTransformer<ObjectTemplateType, QObjectTemplate, MObject> {

    public ObjectTemplateSqlTransformer(
            SqlTransformerSupport transformerSupport, QObjectTemplateMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public void storeRelatedEntities(@NotNull MObject row,
            @NotNull ObjectTemplateType schemaObject, @NotNull JdbcSession jdbcSession) {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        storeRefs(row, schemaObject.getIncludeRef(),
                QObjectReferenceMapping.INSTANCE_INCLUDE, jdbcSession);
    }
}
