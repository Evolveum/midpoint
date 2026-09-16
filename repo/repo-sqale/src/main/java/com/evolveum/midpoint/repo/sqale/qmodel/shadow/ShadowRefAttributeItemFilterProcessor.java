/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;

import com.querydsl.core.types.Predicate;

import javax.xml.namespace.QName;

public class ShadowRefAttributeItemFilterProcessor extends RefTableItemFilterProcessor<QShadowReferenceAttribute, MShadowReferenceAttribute, QShadow, MShadow> {

    private final QName name;
    private final Integer nameId;

    public ShadowRefAttributeItemFilterProcessor(QName name, SqlQueryContext<Object, QShadow, MShadow> ctx, QShadowReferenceAttributeMapping referenceMapping) {
        super(ctx, referenceMapping);
        this.name = name;
        this.nameId = ((SqaleRepoContext) ctx.repositoryContext()).processCacheableUri(name);
    }

    @Override
    protected Predicate corellationPredicate(QShadowReferenceAttribute ref) {
        return ref.pathId.eq(nameId).and(super.corellationPredicate(ref));

    }
}
