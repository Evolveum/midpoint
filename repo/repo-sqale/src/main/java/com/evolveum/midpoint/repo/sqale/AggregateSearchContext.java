package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefTableItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.ReferenceNameResolver;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
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

    private final List<ItemPath> resolveNamesPath = new ArrayList<>();


    public AggregateSearchContext(AggregateQuery<?> query, SqaleQueryContext<? extends Containerable, FlexibleRelationalPathBase<Object>, Object> queryContext, OperationResult result) {
        apiQuery = query;
        context = queryContext;
        sqlRepoContext = context.repositoryContext();
    }

    public SearchResultList<PrismContainerValue<?>> search() throws SchemaException, RepositoryException {
        computeMapping();

        var nameResolver = ReferenceNameResolver.from(apiQuery.isResolveNames() ? resolveNamesPath : Collections.emptyList());
        if (apiQuery.getFilter() != null) {
            context.processFilter(apiQuery.getFilter());
        }
        context.beforeQuery();
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            var query = createQueryWithoutPaging(jdbcSession);
            // FIXME: Add Paging
            List<Tuple> results = query.fetch();
            SearchResultList<PrismContainerValue<?>> ret = new SearchResultList<>();
            for (var partial: results) {
                var pcv = toPrismContainerValue(partial, jdbcSession);
                ret.add(nameResolver.resolve(pcv,jdbcSession));
            }
            return ret;
        }
    }

    public int count() throws SchemaException, RepositoryException {
        computeMapping();
        if (apiQuery.getFilter() != null) {
            context.processFilter(apiQuery.getFilter());
        }
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
                var processor = resolved.mapper.createFilterProcessor(resolved.context);
                if (processor instanceof RefItemFilterProcessor refItem) {
                    mapped = new ReferenceItemMapping(item, refItem, true);
                }
                if (processor instanceof RefTableItemFilterProcessor refTable) {
                    mapped = new ReferenceTableMapping(item, refTable, true);
                }
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
            var processor = resolved.mapper.createFilterProcessor(resolved.context);
            if (processor instanceof RefTableItemFilterProcessor tableRef) {
                processor = tableRef.asSingleItemFilterUsingJoin();
            }
            if (processor instanceof RefItemFilterProcessor refItem) {
                resolveNamesPath.add(item.getName());
                return new ReferenceItemMapping(item, refItem, false);
            }
        } else if (item.getDefinition() instanceof PrismObjectDefinition) {
            // Handle prism object definition
        } else if (item.getDefinition() instanceof PrismPropertyDefinition) {
            ItemValueFilterProcessor<?> processor = resolved.mapper.createFilterProcessor(resolved.context);
            if (processor instanceof PolyStringItemFilterProcessor<?> poly) {
                return new PolyStringMapping(item, poly);
            }
            if (processor instanceof SimpleItemFilterProcessor<?,?> simple) {
                return new SimpleValueMapping(item, simple);
            }
            if (processor instanceof EnumItemFilterProcessor<?> enumProc) {
                return new EnumValueMapping(item, enumProc);
            }
            if (processor instanceof UriItemFilterProcessor uriProc) {
                return new UriValueMapping(item, uriProc);
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

        public PolyStringMapping(AggregateQuery.ResultItem item, PolyStringItemFilterProcessor poly) {
            super(item);
            normPath = Expressions.stringPath(pathName("norm"));
            origPath = Expressions.stringPath(pathName("orig"));
            expressions = ImmutableList.of(poly.getNormPath().as(normPath), poly.getOrigPath().as(origPath));
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

    private class SimpleValueMapping extends ResultMapping {
        private final StringPath path;

        private final List<Expression<?>> expressions;

        public SimpleValueMapping(AggregateQuery.ResultItem item, SimpleItemFilterProcessor processor) {
            super(item);
            path = Expressions.stringPath(pathName("norm"));
            expressions = ImmutableList.of(ExpressionUtils.as(processor.getPath(), path));
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return expressions;
        }

        @Override
        public Collection<? extends Expression<?>> groupExpressions() {
            return ImmutableList.of(path);
        }

        @Override
        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) {
            var property = PrismContext.get().itemFactory().createProperty(item.getName());
            var value = partial.get(path);
            if (value != null) {
                property.setRealValue(value);
            }
            return property;
        }
    }

    private class EnumValueMapping extends ResultMapping {

        private final Path<?> path;
        private final List<Expression<?>> expressions;

        public EnumValueMapping(AggregateQuery.ResultItem item, EnumItemFilterProcessor<?> processor) {
            super(item);
            path = Expressions.path(processor.getPath().getType(),item.getName().getLocalPart());
            expressions = ImmutableList.of(processor.getPath().as(item.getName().getLocalPart()));
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return expressions;
        }

        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) {
            var property = PrismContext.get().itemFactory().createProperty(item.getName());
            var value = partial.get(path);
            if (value != null) {
                property.setRealValue(value);
            }
            return property;
        }

        @Override
        public Collection<? extends Expression<?>> groupExpressions() {
            return ImmutableList.of(path);
        }
    }

    private abstract class ReferenceMapping extends ResultMapping {


        private final UuidPath oidPath;

        private final EnumPath<MObjectType> typePath;

        private final NumberPath<Integer> relationPath;

        private final boolean dereference;

        private QObject<com.evolveum.midpoint.repo.sqale.qmodel.object.MObject> derefPath;
        private QObjectMapping<ObjectType, QObject<MObject>, MObject> derefMapping;

        private final List<Expression<?>> expressions = new ArrayList<>();
        private final List<Expression<?>> groupExpressions =  new ArrayList<>();

        public ReferenceMapping(AggregateQuery.ResultItem item, UuidPath oidPath, NumberPath<Integer> relationPath, EnumPath<MObjectType> typePath, boolean dereference) {
            super(item);
            this.dereference = dereference;
            this.oidPath = new UuidPath(PathMetadataFactory.forVariable(pathName("oid")));
            this.typePath = Expressions.enumPath(MObjectType.class, pathName("type"));
            this.relationPath = Expressions.numberPath(Integer.class, pathName( "relation_id"));
            groupExpressions.add(oidPath);
            groupExpressions.add(typePath);
            groupExpressions.add(relationPath);

            expressions.add(oidPath.as(this.oidPath));
            expressions.add(typePath.as(this.typePath));
            expressions.add(relationPath.as(this.relationPath));

            if (dereference) {
                var schemaTargetType = ((PrismReferenceDefinition) item.getDefinition()).getTargetTypeName();
                schemaTargetType = schemaTargetType != null ? schemaTargetType : ObjectType.COMPLEX_TYPE;
                var targetClass = PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectType(schemaTargetType);
                //noinspection rawtypes,unchecked
                derefMapping = (QObjectMapping) sqlRepoContext.getMappingBySchemaType(targetClass);
                derefPath = derefMapping.newAlias(pathName( "deref"));
                context.sqlQuery().leftJoin(derefPath).on(derefPath.oid.eq(oidPath));

                // Add everything from selectExpressions
                for (var expr: derefMapping.selectExpressions(derefPath,null)) {
                    expressions.add(expr);
                    groupExpressions.add(expr);
                }
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
    private class ReferenceItemMapping extends ReferenceMapping {
        public ReferenceItemMapping(AggregateQuery.ResultItem item, RefItemFilterProcessor ref, boolean dereference) {
            super(item, ref.getOidPath(), ref.getRelationIdPath(), ref.getTypePath(), dereference);
        }

    }

    private class ReferenceTableMapping extends ReferenceMapping {
        public ReferenceTableMapping(AggregateQuery.ResultItem item, RefTableItemFilterProcessor ref, boolean dereference) {
            super(item, null, null, null, dereference);
        }

    }

    private class UriValueMapping extends ResultMapping {

        private final NumberPath<Integer> path;
        private final List<Expression<?>> expressions;
        private final boolean qnameReturn;

        public UriValueMapping(AggregateQuery.ResultItem item, UriItemFilterProcessor processor) {
            super(item);
            path = Expressions.numberPath(Integer.class, pathName(""));
            expressions = ImmutableList.of(processor.getPath().as(path));
            qnameReturn = DOMUtil.XSD_QNAME.equals(item.getDefinition().getTypeName());
        }

        @Override
        public Collection<Expression<?>> selectExpressions() {
            return expressions;
        }

        @Override
        public Collection<? extends Expression<?>> groupExpressions() {
            return ImmutableList.of(path);
        }

        @Override
        public Item<?, ?> prismItemFrom(Tuple partial, JdbcSession session) {
            var property = PrismContext.get().itemFactory().createProperty(item.getName());
            var value = partial.get(path);
            if (value != null) {
                property.setRealValue(qnameReturn ? sqlRepoContext.resolveUriIdToQName(value): sqlRepoContext.resolveIdToUri(value));
            }
            return property;
        }
    }
}
