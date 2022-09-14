/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType.F_ROW;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.PathSet;

import com.google.common.base.Strings;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * Mapping between {@link QLookupTable} and {@link LookupTableType}.
 */
public class QLookupTableMapping
        extends QAssignmentHolderMapping<LookupTableType, QLookupTable, MLookupTable> {

    public static final String DEFAULT_ALIAS_NAME = "lt";
    private static QLookupTableMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QLookupTableMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QLookupTableMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QLookupTableMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QLookupTableMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QLookupTable.TABLE_NAME, DEFAULT_ALIAS_NAME,
                LookupTableType.class, QLookupTable.class, repositoryContext);

        addContainerTableMapping(F_ROW,
                QLookupTableRowMapping.init(repositoryContext),
                joinOn((o, t) -> o.oid.eq(t.ownerOid)));
    }

    @Override
    protected QLookupTable newAliasInstance(String alias) {
        return new QLookupTable(alias);
    }

    @Override
    public MLookupTable newRowObject() {
        return new MLookupTable();
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MLookupTable lookupTable,
            @NotNull LookupTableType schemaObject,
            @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(lookupTable, schemaObject, jdbcSession);

        List<LookupTableRowType> rows = schemaObject.getRow();
        if (!rows.isEmpty()) {
            rows.forEach(row ->
                    QLookupTableRowMapping.get().insert(row, lookupTable, jdbcSession));
        }
    }

    @Override
    protected PathSet fullObjectItemsToSkip() {
        return PathSet.of(F_ROW);
    }

    @Override
    public LookupTableType toSchemaObjectInternal(
            Tuple rowTuple, QLookupTable entityPath, Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession session, boolean forceFull) throws SchemaException {
        LookupTableType base = super.toSchemaObjectInternal(rowTuple, entityPath, options, session, forceFull);

        if (forceFull || SelectorOptions.hasToFetchPathNotRetrievedByDefault(F_ROW, options)) {
            @Nullable GetOperationOptions rowOptions = findLookupTableGetOption(options);
            appendLookupTableRows(rowTuple.get(0, UUID.class), base, rowOptions, session);
        }

        return base;
    }

    private void appendLookupTableRows(
            UUID ownerOid, LookupTableType base, GetOperationOptions rowOptions, JdbcSession session) {
        try {
            RelationalValueSearchQuery queryDef = rowOptions == null ? null : rowOptions.getRelationalValueSearchQuery();
            QLookupTableRowMapping rowMapping = QLookupTableRowMapping.get();
            QLookupTableRow alias = rowMapping.defaultAlias();

            BooleanExpression whereQuery = appendConditions(alias, alias.ownerOid.eq(ownerOid), queryDef);
            SQLQuery<MLookupTableRow> query = session.newQuery()
                    .from(alias)
                    .select(alias)
                    .where(whereQuery);

            query = pagingAndOrdering(query, queryDef, rowMapping, alias);

            List<MLookupTableRow> result = query.fetch();

            for (MLookupTableRow r : result) {
                LookupTableRowType lookupRow = new LookupTableRowType().key(r.key);
                if (r.labelOrig != null || r.labelNorm != null) {
                    lookupRow.label(PolyString.toPolyStringType(new PolyString(r.labelOrig, r.labelNorm)));
                }
                lookupRow.lastChangeTimestamp(MiscUtil.asXMLGregorianCalendar(r.lastChangeTimestamp));
                lookupRow.value(r.value);
                lookupRow.asPrismContainerValue().setId(r.cid);
                base.getRow().add(lookupRow);
            }
        } catch (QueryException e) {
            throw new SystemException("Unable to fetch nested table rows", e);
        }
    }

    private BooleanExpression appendConditions(QLookupTableRow alias,
            BooleanExpression base, RelationalValueSearchQuery queryDef) throws QueryException {
        if (queryDef == null || queryDef.getColumn() == null
                || queryDef.getSearchType() == null || Strings.isNullOrEmpty(queryDef.getSearchValue())) {
            return base;
        }
        String value = queryDef.getSearchValue();
        StringPath path = (StringPath) QLookupTableRowMapping.get()
                .itemMapper(queryDef.getColumn())
                .primaryPath(alias, null);
        BooleanExpression right;
        if (LookupTableRowType.F_LABEL.equals(queryDef.getColumn())) {
            path = alias.labelNorm;
            var poly = new PolyString(value);
            poly.recompute(prismContext().getDefaultPolyStringNormalizer());
            value = poly.getNorm();
        }

        switch (queryDef.getSearchType()) {
            case EXACT:
                right = path.eq(value);
                break;
            case STARTS_WITH:
                right = path.startsWith(value);
                break;
            case SUBSTRING:
                right = path.contains(value);
                break;
            default:
                throw new IllegalStateException();
        }
        return base.and(right);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <R> SQLQuery<R> pagingAndOrdering(SQLQuery<R> query, RelationalValueSearchQuery queryDef,
            QLookupTableRowMapping rowMapping, QLookupTableRow alias) throws QueryException {
        if (queryDef != null && queryDef.getPaging() != null) {
            var paging = queryDef.getPaging();
            if (paging.getOffset() != null) {
                query = query.offset(paging.getOffset());
            }
            if (paging.getMaxSize() != null) {
                query = query.limit(paging.getMaxSize());
            }
            for (ObjectOrdering ordering : paging.getOrderingInstructions()) {
                Order direction = ordering.getDirection() == OrderDirection.DESCENDING ? Order.DESC : Order.ASC;
                var mapper = rowMapping.itemMapper(ordering.getOrderBy().firstToQName());
                Expression path = mapper.primaryPath(alias, null);
                if (ItemPath.equivalent(LookupTableRowType.F_LABEL, ordering.getOrderBy())) {
                    // old repository uses normalized form for ordering
                    path = alias.labelNorm;
                }
                query.orderBy(new OrderSpecifier<>(direction, path));
            }
        }
        return query;
    }

    private GetOperationOptions findLookupTableGetOption(Collection<SelectorOptions<GetOperationOptions>> options) {
        Collection<SelectorOptions<GetOperationOptions>> filtered = SelectorOptions.filterRetrieveOptions(options);
        for (SelectorOptions<GetOperationOptions> option : filtered) {
            ObjectSelector selector = option.getSelector();
            if (selector == null) {
                // Ignore this. These are top-level options. There will not
                // apply to lookup table
                continue;
            }
            if (LookupTableType.F_ROW.equivalent(selector.getPath())) {
                return option.getOptions();
            }
        }

        return null;
    }
}
