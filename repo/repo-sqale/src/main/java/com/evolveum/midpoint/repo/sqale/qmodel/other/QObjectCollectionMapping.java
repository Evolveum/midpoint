/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * Mapping between {@link QObjectCollection} and {@link ObjectCollectionType}.
 */
public class QObjectCollectionMapping
        extends QAssignmentHolderMapping<ObjectCollectionType, QObjectCollection, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "oc";

    public static QObjectCollectionMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QObjectCollectionMapping(repositoryContext);
    }

    private QObjectCollectionMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QObjectCollection.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectCollectionType.class, QObjectCollection.class, repositoryContext);
    }

    @Override
    protected QObjectCollection newAliasInstance(String alias) {
        return new QObjectCollection(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
