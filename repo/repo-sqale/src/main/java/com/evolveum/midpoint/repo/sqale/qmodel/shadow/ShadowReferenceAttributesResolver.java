package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.delta.item.RefTableItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemRelationResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleNestedMapping;
import com.evolveum.midpoint.repo.sqale.update.NestedContainerUpdateContext;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class ShadowReferenceAttributesResolver
        implements SqaleItemRelationResolver<QShadow, MShadow, QShadowReferenceAttribute, MShadowReferenceAttribute> {

    private final DynamicMapper mapping = new DynamicMapper();
    private QShadowReferenceAttributeMapping referenceMapping;

    public ShadowReferenceAttributesResolver(QShadowReferenceAttributeMapping referenceMapping) {
        this.referenceMapping = referenceMapping;
    }

    @Override
    public SqaleUpdateContext<?, ?, ?> resolve(SqaleUpdateContext<?, QShadow, MShadow> context, ItemPath itemPath) throws RepositoryException {
        return new ShadowReferenceAttributesUpdateContext(context, mapping);
    }

    @Override
    public ResolutionResult<QShadowReferenceAttribute, MShadowReferenceAttribute> resolve(SqlQueryContext<?, QShadow, MShadow> context, boolean parent) {
        return new ResolutionResult(context, mapping);
    }

    private class DynamicMapper extends QueryModelMapping<ShadowType, QShadow, MShadow> {

        public DynamicMapper() {
            super(ShadowType.class, QShadow.class);
        }

        @Override
        public @Nullable ItemSqlMapper<QShadow, MShadow> getItemMapper(QName itemName) {
            return new SqaleItemSqlMapper<>(
                    ctx -> new ShadowRefAttributeItemFilterProcessor(itemName,ctx, referenceMapping),
                    ctx -> new ShadowRefAttributeItemDeltaProcessor(itemName, ctx, referenceMapping));
        }

        @Override
        public @Nullable ItemRelationResolver getRelationResolver(QName itemName) {
            var pathId = referenceMapping.repositoryContext().processCacheableUri(itemName);
            return TableRelationResolver.<QShadow, MShadow, ObjectReferenceType, QShadowReferenceAttribute, MShadowReferenceAttribute>usingJoin(() -> referenceMapping,
                    (s,r) -> r.pathId.eq(pathId).and(referenceMapping.correlationPredicate().apply(s,r))
            );
        }
    }
}
