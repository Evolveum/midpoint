package com.evolveum.midpoint.repo.sql.pure.mapping;

import static java.util.stream.Collectors.toMap;

import static com.evolveum.midpoint.repo.sql.pure.SqlQueryContext.MAX_ID_IN_FOR_TO_MANY_FETCH;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.sql.SQLQuery;

/**
 * Mapper/fetcher of many detail records for one master record.
 * This also takes care of the execution ({@link #execute(Supplier, List)} as it is easier
 * (and maybe nicer) to contain all the parametrized types in a single class.
 *
 * @param <R> type of master row
 * @param <I> type of row-PK/detail-FK
 * @param <DQ> detail Q-type, this will be the base of the select
 * @param <DR> detail row type (from result)
 */
public class SqlDetailFetchMapper<R, I, DQ extends EntityPath<DR>, DR> {

    private final Function<R, I> rowToId;
    private final DQ detailAlias;
    private final SimpleExpression<I> detailFkPath;
    private final Function<DR, I> detailToMasterId;
    private final BiConsumer<R, DR> masterDetailConsumer;

    // If used extensively we better add a builder for it as it has 5 complex arguments.
    public SqlDetailFetchMapper(
            Function<R, I> rowToId,
            DQ detailAlias,
            SimpleExpression<I> detailFkPath,
            Function<DR, I> detailToMasterId,
            BiConsumer<R, DR> masterDetailConsumer) {
        this.rowToId = rowToId;
        this.detailAlias = detailAlias;
        this.detailFkPath = detailFkPath;
        this.detailToMasterId = detailToMasterId;
        this.masterDetailConsumer = masterDetailConsumer;
    }

    // following methods are provided as shortcuts and for readability of the client code

    public void execute(Supplier<SQLQuery<?>> querySupplier, List<R> data) {
        // partitioning recursively calls the same method on sub-limit result lists
        if (data.size() > MAX_ID_IN_FOR_TO_MANY_FETCH) {
            Lists.partition(data, MAX_ID_IN_FOR_TO_MANY_FETCH)
                    .forEach(partition -> execute(querySupplier, partition));
        }

        Map<I, R> rowById = data.stream()
                .collect(toMap(rowToId, row -> row));
        SQLQuery<DR> query = querySupplier.get()
                .select(detailAlias)
                .from(detailAlias)
                .where(detailFkPath.in(rowById.keySet()));
        System.out.println("SQL detail query = " + query);
        List<DR> details = query.fetch();
        for (DR detail : details) {
            masterDetailConsumer.accept(
                    rowById.get(detailToMasterId.apply(detail)),
                    detail);
        }
    }
}
