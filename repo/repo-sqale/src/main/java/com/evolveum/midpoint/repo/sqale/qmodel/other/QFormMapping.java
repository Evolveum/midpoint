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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormType;

/**
 * Mapping between {@link QForm} and {@link FormType}.
 */
public class QFormMapping extends QAssignmentHolderMapping<FormType, QForm, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "form";

    public static QFormMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QFormMapping(repositoryContext);
    }

    private QFormMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QForm.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FormType.class, QForm.class, repositoryContext);
    }

    @Override
    protected QForm newAliasInstance(String alias) {
        return new QForm(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
