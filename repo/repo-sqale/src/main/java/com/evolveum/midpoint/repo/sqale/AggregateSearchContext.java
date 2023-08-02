package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.SQLQuery;

import java.util.*;
import java.util.function.Function;

public class AggregateSearchContext {

    private final SqaleQueryContext<? extends Containerable, FlexibleRelationalPathBase<Object>, Object> context;
    private final AggregateQuery<?> apiQuery;
    private final SqaleRepoContext sqlRepoContext;

    private final Map<AggregateQuery.ResultItem, ResultMapping> mapping = new LinkedHashMap<>();


    private final List<Expression<?>> selectExpressions = new ArrayList<>();
    private final List<Expression<?>> groupExpressions = new ArrayList<>();
    private final List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();


    public AggregateSearchContext(AggregateQuery<?> query, SqaleQueryContext<? extends Containerable, FlexibleRelationalPathBase<Object>, Object> queryContext, OperationResult result) {
        apiQuery = query;
        context = queryContext;
        sqlRepoContext = context.repositoryContext();
    }

    public SearchResultList<PrismContainerValue<?>> search() throws SchemaException, RepositoryException {
        computeMapping();
        context.beforeQuery();
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            var query = createQueryWithoutPaging(jdbcSession);
            // FIXME: Add Paging
            List<Tuple> results = query.fetch();
            SearchResultList<PrismContainerValue<?>> ret = new SearchResultList<>();
            for (var partial: results) {
                ret.add(toPrismContainerValue(partial, jdbcSession));
            }
            return ret;
        }
    }

    public int count() throws SchemaException, RepositoryException {
        computeMapping();
        context.beforeQuery();
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            return (int) createQueryWithoutPaging(jdbcSession).fetchCount();
        }
    }

    private SQLQuery<Tuple> createQueryWithoutPaging(JdbcSession jdbcSession) {
        return context.sqlQuery().clone(jdbcSession.connection())
                .select(selectExpressions.toArray(new Expression[]{}))
                .groupBy(groupExpressions.toArray(new Expression[]{}))
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[]{}));
    }


    private void computeMapping() throws RepositoryException, SchemaException {
        for (var item : apiQuery.getItems()) {
            SqlQueryContext.ResolveResult<?, ?> resolved;
            ResultMapping mapped = null;
            if (item instanceof AggregateQuery.Count count) {
                // Currently we support only counting of self paths.
                mapped = new CountMapping(count, context);
            } else if (item instanceof AggregateQuery.Dereference) {
                resolved = context.resolvePathWithJoins(item.getPath());
                mapped = new ReferenceMapping(item, resolved, true);
            } else if (item instanceof AggregateQuery.Retrieve) {
                resolved = context.resolvePathWithJoins(item.getPath());
                // Here we should somehow cleverly map select fields to reader
                mapped = mappedFromResolved(item, resolved);
            }
            check(mapped != null, SchemaException::new, "Can not map item {}", item.getPath());
            // Special Handling
            mapping.put(item, mapped);
            selectExpressions.addAll(mapped.selectExpressions());
            groupExpressions.addAll(mapped.groupExpressions());
        }

        for (var ord : apiQuery.getOrdering()) {
            if (ord instanceof AggregateQuery.AggregateOrdering agg) {
                var direction = ord.getDirection() == OrderDirection.DESCENDING ? Order.DESC : Order.ASC;
                var mapped = mapping.get(agg.getItem());
                for (var path : mapped.orderingExpressions()) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    var specifier = new OrderSpecifier(direction, path);
                    orderSpecifiers.add(specifier);
                }
            }
        }
    }

    private PrismContainerValue<?> toPrismContainerValue(Tuple partial, JdbcSession jdbcSession) throws SchemaException {
        var container = PrismContext.get().itemFactory().createContainerValue();
        for (var mapping : mapping.values()) {
            container.add(mapping.prismItemFrom(partial,jdbcSession), false);
        }
        return container;
    }

    private ResultMapping mappedFromResolved(AggregateQuery.ResultItem item, SqlQueryContext.ResolveResult<?,?> resolved) {
        // Simple Types handling
        if (item.getDefinition() instanceof PrismReferenceDefinition) {
            return new ReferenceMapping(item, resolved, false);
        } else if (item.getDefinition() instanceof PrismObjectDefinition) {
            // Handle prism object definition
        } else if (item.getDefinition() instanceof PrismPropertyDefinition) {
            if (PolyStringType.COMPLEX_TYPE.equals(item.getDefinition().getTypeName())) {
                // Handle PolyString
                return new PolyStringMapping(item, resolved);
            }
            // Handle prism property definition
        }


        return null;
    }


    private static abstract class ResultMapping {

        protected final AggregateQuery.ResultItem item;

        public ResultMapping(AggregateQuery.ResultItem item) {
            this.item = item;

        }

        public boolean isDistinct() {
            return true;
        }
        public Collection<Expression<?>> selectExpressions() {
            return Collections.emptyList();
        }

        public Collection<? extends Expression<?>> groupExpressions() {
            return Collections.emptyList();
        }

        public abstract Item<?,?> prismItemFrom(Tuple partial, JdbcSession session) throws SchemaException;

        public AggregateQuery.ResultItem getItem() {
            return item;
        }

        public List<Expression<?>> orderingExpressions() {
            return Collections.emptyList();
        }

        protected final String pathName(String suffix) {
            return "_" + item.getName().getLocalPart() +"_" + suffix;
        }
    }


    private static class CountMapping extends ResultMapping {

        private final NumberPath<Long> count;
        private final NumberExpression<Long> expression;

        public CountMapping(AggregateQuery.Count item, SqaleQueryContext<?, ?, ?> context) {
            super(item);
            count = Expressions.numberPath(Long.class, pathName("count"));
            expression = context.path().count().as(count);
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return ImmutableList.of(expression);
        }

        @Override
        public boolean isDistinct() {
            return false;
        }

        @Override
        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) {
            var countVal = partial.get(count);
            PrismProperty<Long> property = PrismContext.get().itemFactory().createProperty(item.getName());
            property.setRealValue(countVal);
            return property;
        }

        @Override
        public List<Expression<?>> orderingExpressions() {
            return Collections.singletonList(count);
        }
    }

    private static <E extends Exception>  void check(boolean test, Function<String, E> generator, String template, Object... args) throws E {
        if (!test) {
            throw generator.apply(Strings.lenientFormat(template, args));
        }
    }

    private static class PolyStringMapping extends ResultMapping {

        private final StringPath normPath;
        private final StringPath origPath;

        private final List<Expression<?>> expressions;

        public PolyStringMapping(AggregateQuery.ResultItem item, SqlQueryContext.ResolveResult<?, ?> resolved) {
            super(item);
            normPath = Expressions.stringPath(pathName("norm"));
            origPath = Expressions.stringPath(pathName("orig"));
            var processor = resolved.mapper.createFilterProcessor(resolved.context);
            //noinspection rawtypes
            if (processor instanceof PolyStringItemFilterProcessor poly) {
                expressions = ImmutableList.of(poly.getNormPath().as(normPath), poly.getOrigPath().as(origPath));
            } else {
                throw new UnsupportedOperationException("Only Polystring filter is supported");
            }
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return expressions;
        }

        @Override
        public Collection<? extends Expression<?>> groupExpressions() {
            return ImmutableList.of(normPath, origPath);
        }

        @Override
        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) {
            var property = PrismContext.get().itemFactory().createProperty(item.getName());
            var orig = partial.get(origPath);
            var norm = partial.get(normPath);
            if (orig != null && norm != null) {
                property.setRealValue(new PolyString( orig, norm));
            }
            return property;
        }
    }

    private class ReferenceMapping extends ResultMapping {

        private final UuidPath oidPath;

        private final EnumPath<MObjectType> typePath;

        private final NumberPath<Integer> relationPath;

        private final boolean dereference;

        private final List<Expression<?>> expressions = new ArrayList<>();
        private final List<Expression<?>> groupExpressions =  new ArrayList<>();


        private QObject<com.evolveum.midpoint.repo.sqale.qmodel.object.MObject> derefPath;
        private QObjectMapping<ObjectType, QObject<MObject>, MObject> derefMapping;

        public ReferenceMapping(AggregateQuery.ResultItem item, SqlQueryContext.ResolveResult<?, ?> resolved, boolean dereference) {
            super(item);
            this.dereference = dereference;
            oidPath = new UuidPath(PathMetadataFactory.forVariable(pathName("oid")));
            typePath = Expressions.enumPath(MObjectType.class, pathName("type"));
            relationPath = Expressions.numberPath(Integer.class, pathName( "relation_id"));
            groupExpressions.add(oidPath);
            groupExpressions.add(typePath);
            groupExpressions.add(relationPath);

            var processor = resolved.mapper.createFilterProcessor(resolved.context);
            if (processor instanceof RefItemFilterProcessor ref) {
                expressions.add(ref.getOidPath().as(oidPath));
                expressions.add(ref.getRelationIdPath().as(relationPath));
                expressions.add(ref.getTypePath().as(typePath));

                if (dereference) {
                    var schemaTargetType = ((PrismReferenceDefinition) item.getDefinition()).getTargetTypeName();
                    schemaTargetType = schemaTargetType != null ? schemaTargetType : ObjectType.COMPLEX_TYPE;
                    var targetClass = PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectType(schemaTargetType);
                    //noinspection rawtypes,unchecked
                    derefMapping = (QObjectMapping) sqlRepoContext.getMappingBySchemaType(targetClass);
                    derefPath = derefMapping.newAlias(pathName( "deref"));
                    resolved.context.sqlQuery().leftJoin(derefPath).on(derefPath.oid.eq(ref.getOidPath()));

                    // Add everything from selectExpressions
                    for (var expr: derefMapping.selectExpressions(derefPath,null)) {
                        expressions.add(expr);
                        groupExpressions.add(expr);
                    }
                }
            } else {
                throw new UnsupportedOperationException("Only Polystring filter is supported");
            }
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return expressions;
        }

        @Override
        public Collection<? extends Expression<?>> groupExpressions() {
            return groupExpressions;
        }

        @Override
        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) throws SchemaException {
            var property = PrismContext.get().itemFactory().createReference(item.getName());

            var oid = partial.get(oidPath) != null ? partial.get(oidPath).toString() : null;
            if (oid != null) {
                var typeClass = Objects.requireNonNull(partial.get(typePath)).getSchemaType();
                var typeQName = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(typeClass).getTypeName();


                var refValue = PrismContext.get().itemFactory().createReferenceValue(oid, typeQName);

                var relationRepo = partial.get(relationPath);
                if (relationRepo != null) {
                    var relationQName =  sqlRepoContext.resolveUriIdToQName(relationRepo);
                    refValue.setRelation(relationQName);
                }
                if (dereference) {
                    if (partial.get(derefPath.fullObject) != null) {
                        var mapped = derefMapping.toSchemaObject(partial, derefPath, session, null);
                        refValue.setObject(mapped.asPrismObject());
                    }
                }
                property.add(refValue);
            }
            return property;
        }

    }

}
