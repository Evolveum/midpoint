package com.evolveum.midpoint.repo.sql.pure.mapping;

import static java.util.stream.Collectors.groupingBy;

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

import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Mapper/fetcher of many detail records for one master record.
 * Detail fetch/mapper know hows to fetch to-many details related to a master entity.
 * <p>
 * To load the details for the provided list of data use {@link #execute(Supplier, List)}.
 * To load the details for one master entity use {@link #execute(Supplier, R)}.
 * <p>
 * It is easier (and perhaps nicer) to contain all the parametrized types in a single class,
 * that is why execution methods are here.
 *
 * @param <R> type of master row
 * @param <I> type of row-PK/detail-FK
 * @param <DQ> detail Q-type, this will be the base of the select
 * @param <DR> detail row type (from result)
 */
public class SqlDetailFetchMapper<R, I, DQ extends EntityPath<DR>, DR> {

    private static final Trace LOGGER = TraceManager.getTrace(SqlDetailFetchMapper.class);

    private final Function<R, I> rowToId;
    private final Class<DQ> detailQueryType;
    private final Function<DQ, SimpleExpression<I>> detailFkPathFunction;
    private final Function<DR, I> detailToMasterId;
    private final BiConsumer<R, DR> masterDetailConsumer;

    // If used extensively we better add a builder for it as it has 5 complex arguments.
    public SqlDetailFetchMapper(
            Function<R, I> rowToId,
            Class<DQ> detailQueryType,
            Function<DQ, SimpleExpression<I>> detailFkPathFunction,
            Function<DR, I> detailToMasterId,
            BiConsumer<R, DR> masterDetailConsumer) {
        this.rowToId = rowToId;
        this.detailQueryType = detailQueryType;
        this.detailFkPathFunction = detailFkPathFunction;
        this.detailToMasterId = detailToMasterId;
        this.masterDetailConsumer = masterDetailConsumer;
    }

    public void execute(Supplier<SQLQuery<?>> querySupplier, List<R> data) throws QueryException {
        // partitioning recursively calls the same method on sub-limit result lists
        if (data.size() > MAX_ID_IN_FOR_TO_MANY_FETCH) {
            for (List<R> partition : Lists.partition(data, MAX_ID_IN_FOR_TO_MANY_FETCH)) {
                execute(querySupplier, partition);
            }
        }

        DQ dq = QueryModelMappingConfig.getByQueryType(detailQueryType).newAlias("det_");
        // it is possible we don't have distinct rows, we don't want to fail on it here
        Map<I, List<R>> rowById = data.stream()
                .collect(groupingBy(rowToId));
        SimpleExpression<I> detailFkPath = detailFkPathFunction.apply(dq);
        SQLQuery<DR> query = querySupplier.get()
                .select(dq)
                .from(dq)
                .where(detailFkPath.in(rowById.keySet()));
        LOGGER.debug("SQL detail query for list: {}", query);
        List<DR> details = query.fetch();
        for (DR detail : details) {
            for (R row : rowById.get(detailToMasterId.apply(detail))) {
                masterDetailConsumer.accept(row, detail);
            }
        }
    }

    public void execute(Supplier<SQLQuery<?>> querySupplier, R masterRow) throws QueryException {
        DQ dq = QueryModelMappingConfig.getByQueryType(detailQueryType).newAlias("det_");
        SimpleExpression<I> detailFkPath = detailFkPathFunction.apply(dq);
        SQLQuery<DR> query = querySupplier.get()
                .select(dq)
                .from(dq)
                .where(detailFkPath.eq(rowToId.apply(masterRow)));
        LOGGER.debug("SQL detail query for one entity: {}", query);
        List<DR> details = query.fetch();
        for (DR detail : details) {
            masterDetailConsumer.accept(masterRow, detail);
        }
    }
}
