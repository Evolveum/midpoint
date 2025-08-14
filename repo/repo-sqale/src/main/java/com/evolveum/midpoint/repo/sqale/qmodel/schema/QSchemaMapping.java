/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.schema;

import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMark;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;

/**
 * Mapping between {@link QMark} and {@link MarkType}.
 */
public class QSchemaMapping
        extends QObjectMapping<SchemaType, QSchema, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "schema";
    private static QSchemaMapping instance;

    public static QSchemaMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QSchemaMapping(repositoryContext);
        }
        return getInstance();
    }

    public static QSchemaMapping getInstance() {
        return Objects.requireNonNull(instance);
    }



    private QSchemaMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSchema.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SchemaType.class, QSchema.class, repositoryContext);
    }

    @Override
    protected QSchema newAliasInstance(String alias) {
        return new QSchema(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
