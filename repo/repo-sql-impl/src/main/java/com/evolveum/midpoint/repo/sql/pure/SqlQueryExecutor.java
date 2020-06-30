package com.evolveum.midpoint.repo.sql.pure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMappingConfig;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;

/**
 * Component just under the service that orchestrates query transformation and execution.
 */
@Component
public class SqlQueryExecutor {

    // TODO configuration should reflect the used DB of course
    public static final Configuration QUERYDSL_CONFIGURATION =
            new Configuration(SQLTemplates.DEFAULT);

    @Autowired
    private DataSourceFactory dataSourceFactory;

    public <T extends Containerable> SearchResultList<T> list(
            @NotNull Class<T> prismType, // ignored for the moment
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) throws QueryException {

        // TODO use?
        boolean distinctRequested = GetOperationOptions.isDistinct(SelectorOptions.findRootOptions(options));

        QueryModelMapping<?, ?> rootMapping = QueryModelMappingConfig.getByModelType(prismType);
        FlexibleRelationalPathBase<?> root = rootMapping.defaultAlias();
        SqlQueryContext context = new SqlQueryContext(root, rootMapping);

        // add conditions (with exists clauses as necessary)
        ObjectFilter filter = query != null ? query.getFilter() : null;
        if (filter != null) {
            processFilter(context, filter);
        }

        // TODO: what if we declare AuditEventRecordType, but we want transformed result?
        // some mapping function as an argument?
        // some builder to construct the whole definition that will be an argument?

        PageOf<Tuple> result = executeQuery(context);

        PageOf<AuditEventRecordType> map = result
                .map(t -> (MAuditEventRecord) t.get(root))
                .map(AuditEventRecordSqlTransformer::toAuditEventRecordType);
        //noinspection unchecked
        return (SearchResultList<T>) createSearchResultList(map);
    }

    private void processFilter(SqlQueryContext context, ObjectFilter filter) throws QueryException {
        if (filter instanceof PropertyValueFilter) {
            PropertyValueFilter<?> propertyValueFilter = (PropertyValueFilter<?>) filter;
            System.out.println("filter = " + propertyValueFilter);
            Object path = propertyValueFilter.getFullPath().first();
            System.out.println("path = " + path);
            FlexibleRelationalPathBase<?> pathWithCondition = context.path();
            if (propertyValueFilter instanceof EqualFilter<?>) {
                // hardcoded condition from test110SearchAllAuditEventsOfSomeType - TODO: now just do it magically
                QAuditEventRecord root = context.root(QAuditEventRecord.class);
                BooleanExpression predicate = root.eventtype.eq(AuditEventTypeType.ADD_OBJECT.ordinal());
                context.addPredicate(predicate);
//                ExpressionUtils.eq(context.mapping().)
//                context.mapping()
            } else {
                throw new QueryException("Unsupported filter " + filter);
            }
            /*
            EQUAL: eventType,PPV(AuditEventTypeType:ADD_OBJECT)
            how to resolve eventType to actual PathName?

            PropertyValueFilter valFilter = (PropertyValueFilter) filter;
            ItemPath path = valFilter.getFullPath();
            ItemDefinition definition = valFilter.getDefinition();

            ProperDataSearchResult propDefRes = resolver.findProperDataDefinition(baseEntityDefinition, path, definition, JpaPropertyDefinition.class,
                    context.getPrismContext());
            if (propDefRes == null) {
                throw new QueryException("Couldn't find a proper data item to query, given base entity " + baseEntityDefinition + " and this filter: " + valFilter.debugDump());
            }
                return new PropertyRestriction(context, valFilter, propDefRes.getEntityDefinition(), parent, propDefRes.getLinkDefinition());
             */
        } else {
            throw new QueryException("Unsupported filter " + filter);
        }
    }

    public PageOf<Tuple> executeQuery(SqlQueryContext context) throws QueryException {
        try (Connection connection = getConnection()) {
            FlexibleRelationalPathBase<?> root = context.root();
            SQLQuery<Tuple> query = context.query(connection)
                    .select(Projections.tuple(root))
                    // TODO add paging
//                    .offset(2)
//                    .limit(2)
                    ;
            long count = query.fetchCount();

            return new PageOf<>(query.fetch(), PageOf.PAGE_NO_PAGINATION, 0, count);
        } catch (SQLException e) {
            throw new QueryException(e.toString(), e);
        }
    }

    @NotNull
    public <T extends Containerable> SearchResultList<T> createSearchResultList(PageOf<T> result) {
        SearchResultMetadata metadata = new SearchResultMetadata();
        metadata.setApproxNumberOfAllResults((int) result.totalCount());
        return new SearchResultList<>(result.content(), metadata);
    }

    private Connection getConnection() throws SQLException {
        return dataSourceFactory.getDataSource().getConnection();
    }
}
