/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.tag;

import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;

/**
 * Mapping between {@link QMark} and {@link MarkType}.
 */
public class QMarkMapping
        extends QAssignmentHolderMapping<MarkType, QMark, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "mark";
    private static QMarkMapping instance;

    public static QMarkMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QMarkMapping(repositoryContext);
        }
        return getInstance();
    }

    public static QMarkMapping getInstance() {
        return Objects.requireNonNull(instance);
    }



    private QMarkMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QMark.TABLE_NAME, DEFAULT_ALIAS_NAME,
                MarkType.class, QMark.class, repositoryContext);
    }

    @Override
    protected QMark newAliasInstance(String alias) {
        return new QMark(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
