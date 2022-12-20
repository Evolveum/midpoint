/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.tag;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Mapping between {@link QTag} and {@link TagType}.
 */
public class QTagMapping
        extends QAssignmentHolderMapping<TagType, QTag, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "tag";

    public static QTagMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QTagMapping(repositoryContext);
    }

    private QTagMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QTag.TABLE_NAME, DEFAULT_ALIAS_NAME,
                TagType.class, QTag.class, repositoryContext);
    }

    @Override
    protected QTag newAliasInstance(String alias) {
        return new QTag(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
