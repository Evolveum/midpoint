/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.other;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;

/**
 * Mapping between {@link QFunctionLibrary} and {@link FunctionLibraryType}.
 */
public class QFunctionLibraryMapping
        extends QAssignmentHolderMapping<FunctionLibraryType, QFunctionLibrary, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "flib";

    public static QFunctionLibraryMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QFunctionLibraryMapping(repositoryContext);
    }

    private QFunctionLibraryMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QFunctionLibrary.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FunctionLibraryType.class, QFunctionLibrary.class, repositoryContext);
    }

    @Override
    protected QFunctionLibrary newAliasInstance(String alias) {
        return new QFunctionLibrary(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
