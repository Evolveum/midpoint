package com.evolveum.midpoint.repo.sql.pure;

import static com.querydsl.core.group.GroupBy.groupBy;

import static com.evolveum.midpoint.repo.sql.metamodel.QAuditDelta.M_AUDIT_DELTA;
import static com.evolveum.midpoint.repo.sql.metamodel.QAuditEventRecord.M_AUDIT_EVENT;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;

import com.querydsl.core.Tuple;
import com.querydsl.core.group.GroupBy;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.Nullable;

// TODO MID-6319 must go after done
@Deprecated
public class SqlGeneration {

    public static void main(String[] args) throws Exception {
        org.h2.Driver.load();
        /* this requires querydsl-sql-codegen
        java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:h2:tcp://localhost:5437/midpoint", "sa", "");
        com.querydsl.sql.codegen.MetaDataExporter exporter = new com.querydsl.sql.codegen.MetaDataExporter();
        exporter.setPackageName("com.myproject.mydomain");
        exporter.setTargetFolder(new java.io.File("target/generated-sources/java"));
        exporter.setBeanSerializer(new com.querydsl.codegen.BeanSerializer());
        exporter.export(conn.getMetaData());
         */

        System.out.println(M_AUDIT_EVENT);
        System.out.println("\nColumns: " + M_AUDIT_EVENT.getColumns());
        System.out.println("\nAnnotated element: " + M_AUDIT_EVENT.getAnnotatedElement());
        System.out.println("\nFKs: " + M_AUDIT_EVENT.getForeignKeys());
        System.out.println("\nInverse FKs: " + M_AUDIT_EVENT.getInverseForeignKeys());
        System.out.println();

        SQLQueryFactory queryFactory = new SQLQueryFactory(SQLTemplates.DEFAULT, () ->
                getConnection());

        System.out.println("audit size = " + queryFactory.selectFrom(M_AUDIT_EVENT).fetchCount());

        SQLQuery<Tuple> query = queryFactory
                .select(M_AUDIT_EVENT, M_AUDIT_DELTA)
//                .select(M_AUDIT_EVENT.id, M_AUDIT_DELTA.checksum)
//                .from(M_AUDIT_EVENT)
                // leftJoin if we want also events without deltas
//                .join(M_AUDIT_EVENT._auditDeltaFk, M_AUDIT_DELTA)
                // alternatively:
                // .join(M_AUDIT_DELTA).on(M_AUDIT_DELTA.recordId.eq(M_AUDIT_EVENT.id))
//                .orderBy(M_AUDIT_EVENT.id.asc())
                .from(M_AUDIT_EVENT, M_AUDIT_DELTA)
                .where(M_AUDIT_EVENT.id.eq(M_AUDIT_DELTA.recordId)) // this replaces "join-on", but only inner
                .where(M_AUDIT_EVENT.id.eq(452L));

        List<Tuple> result = query.fetch();
        System.out.println("result = " + result);
        System.out.println("\nsize: " + result.size());

        System.out.println("\ncount: " + query.transform(groupBy(M_AUDIT_EVENT.id).as(M_AUDIT_DELTA.count())));
        Map<?, ?> transform = query.transform(GroupBy.groupBy(M_AUDIT_EVENT.id).as(GroupBy.list(M_AUDIT_DELTA)));
        System.out.println("transform = " + transform);

        // "manual" transformation
        List<Tuple> plainResult = queryFactory
                .select(M_AUDIT_EVENT, M_AUDIT_DELTA)
                .from(M_AUDIT_EVENT)
                .leftJoin(M_AUDIT_EVENT._auditDeltaFk, M_AUDIT_DELTA)
                // alternatively:
                // .leftJoin(M_AUDIT_DELTA).on(M_AUDIT_DELTA.recordId.eq(M_AUDIT_EVENT.id))
//                .orderBy(M_AUDIT_EVENT.id.asc())
                .where(M_AUDIT_EVENT.id.eq(452L))
                .fetch();
        System.out.println("\nFinal result"
                + mapOneToMany(plainResult, M_AUDIT_EVENT, M_AUDIT_DELTA, (o, m) -> o.addDelta(m)));
    }

    /**
     * Resolves one-to-many relations between two paths from the {@link Tuple}-based result.
     * Returns map with "one" entities as keys (preserving original order) and related "many"
     * entities as a collection in the value for each key.
     * Optional accumulator can call further processing on both objects for each "many" item.
     *
     * Note that proper equals/hashCode must be implemented for {@code <O>} type.
     *
     * @param rawResult collection of tuples, unprocessed result
     * @param onePath path expression designating "one" role of the relationship
     * @param manyPath path expression designating "many" role of the relationship
     * @param manyAccumulator optional, called for each row with respective "one" and "many" items
     * @param <O> type of "one" role
     * @param <M> type of "many" role
     * @return map of one->[many*] with keys in the original iterating order
     */
    private static <O, M> Map<O, Collection<M>> mapOneToMany(
            Collection<Tuple> rawResult,
            RelationalPathBase<O> onePath,
            RelationalPathBase<M> manyPath,
            @Nullable BiConsumer<O, M> manyAccumulator) {

        Map<O, Collection<M>> result = new LinkedHashMap<>();
        for (Tuple row : rawResult) {
            O oneItem = Objects.requireNonNull(row.get(onePath),
                    "result for path " + onePath + " not found in tuple " + row);
            M manyItem = Objects.requireNonNull(row.get(manyPath),
                    "result for path " + manyPath + " not found in tuple " + row);
            result.computeIfAbsent(oneItem, o -> new ArrayList<>())
                    .add(manyItem);

            if (manyAccumulator != null) {
                manyAccumulator.accept(oneItem, manyItem);
            }
        }
        return result;
    }

    private static Connection getConnection() {
        try {
            return java.sql.DriverManager.getConnection("jdbc:h2:tcp://localhost:5437/midpoint", "sa", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
