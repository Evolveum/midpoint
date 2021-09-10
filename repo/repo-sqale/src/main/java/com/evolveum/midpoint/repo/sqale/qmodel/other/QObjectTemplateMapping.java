/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType.F_INCLUDE_REF;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

/**
 * Mapping between {@link QObjectTemplate} and {@link ObjectTemplateType}.
 */
public class QObjectTemplateMapping
        extends QAssignmentHolderMapping<ObjectTemplateType, QObjectTemplate, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "ot";

    private static QObjectTemplateMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectTemplateMapping initObjectTemplateMapping(
            @NotNull SqaleRepoContext repositoryContext) {
        instance = new QObjectTemplateMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectTemplateMapping getObjectTemplateMapping() {
        return Objects.requireNonNull(instance);
    }

    private QObjectTemplateMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QObjectTemplate.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectTemplateType.class, QObjectTemplate.class, repositoryContext);

        addRefMapping(F_INCLUDE_REF, QObjectReferenceMapping.initForInclude(repositoryContext));
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
            @NotNull ObjectTemplateType schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        storeRefs(row, schemaObject.getIncludeRef(),
                QObjectReferenceMapping.getForInclude(), jdbcSession);
    }
}
