/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType.F_INCLUDE_REF;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

/**
 * Mapping between {@link QObjectTemplate} and {@link ObjectTemplateType}.
 */
public class QObjectTemplateMapping
        extends QObjectMapping<ObjectTemplateType, QObjectTemplate, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "ot";

    public static final QObjectTemplateMapping INSTANCE = new QObjectTemplateMapping();

    private QObjectTemplateMapping() {
        super(QObjectTemplate.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectTemplateType.class, QObjectTemplate.class);

        addRefMapping(F_INCLUDE_REF, QObjectReferenceMapping.INSTANCE_INCLUDE);
    }

    @Override
    protected QObjectTemplate newAliasInstance(String alias) {
        return new QObjectTemplate(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }

    @Override
    public void storeRelatedEntities(@NotNull MObject row,
            @NotNull ObjectTemplateType schemaObject, @NotNull JdbcSession jdbcSession) {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        storeRefs(row, schemaObject.getIncludeRef(),
                QObjectReferenceMapping.INSTANCE_INCLUDE, jdbcSession);
    }
}
