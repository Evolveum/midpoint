/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.TypeFilter;

import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignment;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemRelationResolver;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValueUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.FilterOperation;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Filter processor for reference item paths embedded in table as three columns.
 * OID is represented by UUID column, type by ID (see {@link MObjectType}) and relation
 * by Integer (foreign key) to {@link QUri}.
 */
public class RefItemFilterProcessor extends ItemValueFilterProcessor<ValueFilter<?, ?>> {

    // only oidPath is strictly not-null, but then the filter better not ask for type or relation
    private final UuidPath oidPath;
    @Nullable private final EnumPath<MObjectType> typePath;
    @Nullable private final NumberPath<Integer> relationIdPath;
    @Nullable private final StringPath targetNamePath;

    public <Q extends FlexibleRelationalPathBase<R>, R> RefItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            Function<Q, UuidPath> rootToOidPath,
            @Nullable Function<Q, EnumPath<MObjectType>> rootToTypePath,
            @Nullable Function<Q, NumberPath<Integer>> rootToRelationIdPath,
            @Nullable Function<Q, StringPath> rootToTargetNamePath) {
        this(context,
                rootToOidPath.apply(context.path()),
                rootToTypePath != null ? rootToTypePath.apply(context.path()) : null,
                rootToRelationIdPath != null ? rootToRelationIdPath.apply(context.path()) : null,
                rootToTargetNamePath != null ? rootToTargetNamePath.apply(context.path()) : null);
    }

    // exposed mainly for RefTableItemFilterProcessor
    <Q extends FlexibleRelationalPathBase<R>, R> RefItemFilterProcessor(
            SqlQueryContext<?, Q, R> context,
            UuidPath oidPath,
            @Nullable EnumPath<MObjectType> typePath,
            @Nullable NumberPath<Integer> relationIdPath,
            @Nullable StringPath targetNamePath) {
        super(context);
        this.oidPath = oidPath;
        this.typePath = typePath;
        this.relationIdPath = relationIdPath;
        this.targetNamePath = targetNamePath;
    }

    @Override
    public Predicate process(ValueFilter<?, ?> filter) throws RepositoryException {
        if (!(filter instanceof RefFilter)) {
            return processSpecialCases(filter);
        }
        if (filter instanceof RefFilterWithRepoPath) {
            return processRepoFilter((RefFilterWithRepoPath) filter);
        }

        RefFilter refFilter = (RefFilter) filter;
        QName targetTypeName = Objects.requireNonNull(refFilter.getDefinition()).getTargetTypeName();
        ObjectFilter targetFilter = refFilter.getFilter();
        if (refFilter.hasNoValue()) {
            if (targetFilter == null) {
                return refFilter.isOidNullAsAny() ? null : oidPath.isNull();
            } else {
                return targetFilterPredicate(targetTypeName, targetFilter);
            }
        }

        // We definitely have one or more values from here on.
        List<PrismReferenceValue> values = Objects.requireNonNull(refFilter.getValues());
        if (values.size() == 1) {
            var value = values.get(0);
            Predicate predicate = processSingleValue(refFilter, value);
            if (targetFilter != null) {
                var targetType = Optional.ofNullable(value.getTargetType())
                        .orElse(targetTypeName);
                predicate = ExpressionUtils.and(predicate, targetFilterPredicate(targetType, targetFilter));
            }
            return predicate;
        }

        // Here it's multi-value, optionally with a target filter.
        Predicate predicate = null;
        for (PrismReferenceValue ref : values) {
            predicate = ExpressionUtils.or(predicate, processSingleValue(refFilter, ref));
        }

        if (targetFilter != null) {
            predicate = ExpressionUtils.and(predicate, targetFilterPredicate(targetTypeName, targetFilter));
        }

        return predicate;
    }

    private Predicate targetFilterPredicate(@Nullable QName targetType, ObjectFilter targetFilter)
            throws RepositoryException {
        if (targetFilter instanceof TypeFilter tf) {
            // If target filter is simple type filter we should be good to go without throwing in AssignmentHolderType exist query.
            // This should be doable also when there are and/or/not filter combinations with type filter (probably later).
            QName type = tf.getType();
            type = type != null ? type : ObjectType.COMPLEX_TYPE;

            Class<? extends QObject> filterQueryType =
                    MObjectType.fromTypeQName(type).getQueryType();

            SqlQueryContext<?, ? extends QObject, ?> subquery = (SqlQueryContext) context.subquery(filterQueryType);
            subquery.sqlQuery().where(subquery.path().oid.eq(oidPath));
            subquery.processFilter(targetFilter);

            return subquery.sqlQuery().exists();
        }

        targetType = targetType != null ? targetType : ObjectType.COMPLEX_TYPE;
        var targetClass = context.prismContext().getSchemaRegistry().getCompileTimeClassForObjectType(targetType);
        // TODO: This works fine, but LEFT JOIN should be considered for cases when the query is ordered by the target item.
        // This is relevant for referenceSearch. When only where is used, subquery (AKA semi-join) is fine, often even preferable.
        // But when ordering is added, we end up with both EXISTS subquery and LEFT JOIN for the order.
        // See also: com.evolveum.midpoint.repo.sqale.mapping.RefTableTargetResolver#resolve()
        var subquery = context.subquery(context.repositoryContext().getMappingBySchemaType(targetClass));
        var targetPath = subquery.path(QObject.class);
        subquery.sqlQuery().where(oidPath.eq(targetPath.oid));
        subquery.processFilter(targetFilter);
        return subquery.sqlQuery().exists();
    }

    /**
     * Process reference filter used in {@link ReferencedByFilterProcessor}.
     */
    private Predicate processRepoFilter(RefFilterWithRepoPath filter) {
        return relationPredicate(oidPath.eq(filter.getOidPath()), filter.getRelation());
    }

    private Predicate processSingleValue(RefFilter filter, PrismReferenceValue ref) {
        Predicate predicate = null;
        if (ref.getOid() != null) {
            predicate = predicateWithNotTreated(oidPath,
                    oidPath.eq(UUID.fromString(ref.getOid())));
        } else if (!filter.isOidNullAsAny()) {
            predicate = oidPath.isNull();
        }

        // Audit sometimes does not use target type path
        if (typePath != null) {
            if (ref.getTargetType() != null) {
                MObjectType objectType = MObjectType.fromTypeQName(ref.getTargetType());
                predicate = ExpressionUtils.and(predicate,
                        predicateWithNotTreated(typePath, typePath.eq(objectType)));
            } else if (!filter.isTargetTypeNullAsAny()) {
                predicate = ExpressionUtils.and(predicate, typePath.isNull());
            }
        }

        // Audit tables do not use relation paths
        predicate = relationPredicate(predicate, ref.getRelation());

        if (targetNamePath != null && ref.getTargetName() != null) {
            predicate = ExpressionUtils.and(predicate,
                    predicateWithNotTreated(targetNamePath,
                            targetNamePath.eq(ref.getTargetName().getOrig())));
        }
        return predicate;
    }

    private Predicate relationPredicate(Predicate predicate, QName relation) {
        if (relationIdPath != null) {
            if (relation == null || !relation.equals(PrismConstants.Q_ANY)) {
                Integer relationId = ((SqaleQueryContext<?, ?, ?>) context)
                        .searchCachedRelationId(relation);
                predicate = ExpressionUtils.and(predicate,
                        predicateWithNotTreated(relationIdPath, relationIdPath.eq(relationId)));
            } // else relation == Q_ANY, no additional predicate needed
        }
        return predicate;
    }

    // region ref-comparison for iterative search

    /**
     * This covers ref comparison used for iterative search.
     * This is not model compliant filter and can only be constructed inside repository.
     * In that case, it is a comparison filter with a single value of type {@link ReferenceRowValue}.
     */
    @SuppressWarnings("DataFlowIssue")
    private Predicate processSpecialCases(ValueFilter<?, ?> filter) throws QueryException {
        FilterOperation operation = operation(filter);
        ReferenceRowValue ref = filter.getSingleValue().getRealValue();
        QObjectReference<?> refPath = (QObjectReference<?>) context.path();

        UUID ownerOid = SqaleUtils.oidToUuidMandatory(ref.ownerOid);
        UUID targetOid = SqaleUtils.oidToUuidMandatory(ref.targetOid);
        int relationId = ((SqaleQueryContext<?, ?, ?>) context).searchCachedRelationId(ref.relation);
        if (operation.operator == Ops.GT) {
            return refPath.ownerOid.gt(ownerOid)
                    .or(refPath.ownerOid.eq(ownerOid).and(relationIdPath.gt(relationId)))
                    .or(refPath.ownerOid.eq(ownerOid).and(relationIdPath.eq(relationId)).and(oidPath.gt(targetOid)));
        } else if (operation.operator == Ops.LT) {
            return refPath.ownerOid.lt(ownerOid)
                    .or(refPath.ownerOid.eq(ownerOid).and(relationIdPath.lt(relationId)))
                    .or(refPath.ownerOid.eq(ownerOid).and(relationIdPath.eq(relationId)).and(oidPath.lt(targetOid)));
        }
        throw new QueryException("Special case internal reference filter supports only GT/LT - filter: " + filter);
    }

    public static class ReferenceRowValue implements Serializable {
        // Order of these fields reflects the order of PK columns
        private final String ownerOid;
        // Container IDs later, as needed, or use subtype for container-owned references.
        private final QName relation;
        private final String targetOid;

        public static ReferenceRowValue from(ObjectReferenceType ref) {
            PrismObject<?> parent = PrismValueUtil.getParentObject(ref.asReferenceValue());
            if (parent == null) {
                throw new IllegalArgumentException("Unknown parent/owner for reference: " + ref);
            }

            return new ReferenceRowValue(
                    parent.getOid(), ref.getRelation(), ref.getOid());
        }

        public ReferenceRowValue(String ownerOid, QName relation, String targetOid) {
            this.ownerOid = Objects.requireNonNull(ownerOid);
            this.relation = Objects.requireNonNull(relation);
            this.targetOid = Objects.requireNonNull(targetOid);
        }
    }
    // endregion

    public UuidPath getOidPath() {
        return oidPath;
    }

    public NumberPath<Integer> getRelationIdPath() {
        return relationIdPath;
    }

    public EnumPath<MObjectType> getTypePath() {
        return typePath;
    }

}
