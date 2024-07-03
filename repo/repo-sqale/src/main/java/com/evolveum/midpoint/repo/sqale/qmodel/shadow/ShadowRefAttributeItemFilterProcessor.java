package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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
