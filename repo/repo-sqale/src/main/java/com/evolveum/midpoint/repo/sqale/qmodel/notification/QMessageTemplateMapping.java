/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.notification;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * Mapping between {@link QMessageTemplate} and {@link MessageTemplateType}.
 */
public class QMessageTemplateMapping
        extends QAssignmentHolderMapping<MessageTemplateType, QMessageTemplate, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "mt";

    public static QMessageTemplateMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QMessageTemplateMapping(repositoryContext);
    }

    private QMessageTemplateMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QMessageTemplate.TABLE_NAME, DEFAULT_ALIAS_NAME,
                MessageTemplateType.class, QMessageTemplate.class, repositoryContext);
    }

    @Override
    protected QMessageTemplate newAliasInstance(String alias) {
        return new QMessageTemplate(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
