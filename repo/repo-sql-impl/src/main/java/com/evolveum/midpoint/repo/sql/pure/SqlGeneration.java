package com.evolveum.midpoint.repo.sql.pure;

import static com.querydsl.core.group.GroupBy.groupBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;

import com.querydsl.core.Tuple;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QMap;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.pure.querymodel.mapping.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditEventRecord;
import com.evolveum.midpoint.repo.sql.query.QueryException;

// TODO MID-6318 can go after done, there are some ideas for column extensions + generation of beans
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
    }

    private static void extensionExperiments1() throws SQLException {
        try (Connection conn = getConnection()) {
            QAuditEventRecord aer = new QAuditEventRecord("aer");
            System.out.println("M_AUDIT_EVENT.meta.size = " + aer.all().length);
            System.out.println("M_AUDIT_DELTA.meta.size = " + aer.all().length);

            QMap auditDelta = Projections.map(aer.all());
            QMap auditEvent = Projections.map(aer.all());
//        List<Tuple> result = queryFactory
            List<?> result = newQuery(conn)
                    // this way we don't use M-beans, which is more flexible, and still get close to "select whole entity A+B"
//                .select(expand(M_AUDIT_EVENT.id, M_AUDIT_DELTA))

                    // This is also interesting, we instruct to create map for auditDelta paths.
                    // .all() above is necessary, otherwise the map contains only one M-bean, which we want to avoid
                    // Also, we want to extract this expression as variable, so we can use it later, e.g. to get from a tuple, or mapOneToMany processing, etc.
//                .select(aer.id, auditDelta)
                    .select(aer)
                    .from(aer)
//                .leftJoin(M_AUDIT_EVENT.auditDeltaFk, M_AUDIT_DELTA)
                    .fetch();
//        Map<Long, Collection<Map<Expression<?>, ?>>> mapResult =
//                mapOneToMany(result, M_AUDIT_EVENT.id, auditDelta);
//        System.out.println("result = " + Joiner.on("\n").withKeyValueSeparator(" = ").join(mapResult));
//        System.out.println(mapResult.size());

            System.out.println(result);
        }
    }

    /**
     * Expand the list of paths (provided as vararg) so that each {@link RelationalPath}
     * is represented by all its columns.
     * This generates expression array that results in the query of tuples which does not
     * require any backing beans.
     * <p>
     * TODO: maybe we want to convert them to QMap or a tuple?
     */
    private static Expression<?>[] expand(Path<?>... paths) {
        List<Expression<?>> pathsCombined = new ArrayList<>();
        for (Path<?> path : paths) {
            if (path instanceof RelationalPath) {
                pathsCombined.addAll(((RelationalPath<?>) path).getColumns());
            } else {
                pathsCombined.add(path);
            }
        }
        return pathsCombined.toArray(new Expression<?>[0]);
    }

    private static void examples() throws SQLException, QueryException {
        try (Connection conn = getConnection()) {
            QAuditEventRecord aer = QAuditEventRecordMapping.INSTANCE.defaultAlias();
            System.out.println(aer);
            System.out.println("\nColumns: " + aer.getColumns());
            System.out.println("\nAnnotated element: " + aer.getAnnotatedElement());
            System.out.println("\nFKs: " + aer.getForeignKeys());
            System.out.println("\nInverse FKs: " + aer.getInverseForeignKeys());
            System.out.println();

            System.out.println("audit size = " + newQuery(conn)
                    .select(aer)
                    .from(aer)
                    .fetchCount());

            QAuditDelta ad = new QAuditDelta("mad");
            SQLQuery<Tuple> query = newQuery(conn)
                    .select(aer, ad)
//                .select(M_AUDIT_EVENT.id, M_AUDIT_DELTA.checksum)
//                .from(M_AUDIT_EVENT)
                    // leftJoin if we want also events without deltas
//                .join(M_AUDIT_EVENT.auditDeltaFk, M_AUDIT_DELTA)
                    // alternatively:
                    // .join(M_AUDIT_DELTA).on(M_AUDIT_DELTA.recordId.eq(M_AUDIT_EVENT.id))
//                .orderBy(M_AUDIT_EVENT.id.asc())
                    .from(aer, ad)
                    .where(aer.id.eq(ad.recordId)) // this replaces "join-on", but only inner
                    .where(aer.id.eq(452L)); // "Works on my computer! :-)"

            List<Tuple> result = query.fetch();
            System.out.println("result = " + result);
            System.out.println("\nsize: " + result.size());

            System.out.println("\ncount: " + query.transform(groupBy(aer.id).as(ad.count())));
            Map<?, ?> transform = query.transform(GroupBy.groupBy(aer.id).as(GroupBy.list(ad)));
            System.out.println("transform = " + transform);

            // "manual" transformation of one-to-many to proper graph
            List<Tuple> plainResult = newQuery(conn)
                    .select(aer, ad)
                    .from(aer)
                    .leftJoin(aer.auditDeltaFk, ad)
                    // alternatively:
                    // .leftJoin(M_AUDIT_DELTA).on(M_AUDIT_DELTA.recordId.eq(M_AUDIT_EVENT.id))
//                .orderBy(M_AUDIT_EVENT.id.asc())
                    .where(aer.id.eq(452L))
                    .fetch();
            Map<MAuditEventRecord, Collection<MAuditDelta>> resultMap =
                    mapOneToMany(plainResult, aer, ad, (o, m) -> o.addDelta(m));
            System.out.println("\nFinal result" + resultMap);

            System.out.println("deltas for 1st item: " + resultMap.keySet().iterator().next().deltas);
        }
    }

    /**
     * Resolves one-to-many relations between two paths from the {@link Tuple}-based result.
     * Returns map with "one" entities as keys (preserving original order) and related "many"
     * entities as a collection in the value for each key.
     * <p>
     * Optional accumulator can call further processing on both objects for each "many" item
     * with "one" being internalized to the actual key in the resulting map.
     * This solves the problem when the same entity is represented by different instances.
     * Without this it wouldn't be possible to accumulate "many" in the collection owned by "one".
     * <p>
     * Note that proper equals/hashCode must be implemented for {@code <O>} type.
     *
     * @param rawResult collection of tuples, unprocessed result
     * @param onePath path expression designating "one" role of the relationship
     * @param manyPath path expression designating "many" role of the relationship
     * @param manyAccumulator optional, called for each row with respective "one" and "many" items
     * (always the same "one" instance is used for the group matching one key, see details above)
     * @param <O> type of "one" role
     * @param <M> type of "many" role
     * @return map of one->[many*] with keys in the original iterating order
     */
    private static <O, M> Map<O, Collection<M>> mapOneToMany(
            Collection<Tuple> rawResult,
            Expression<O> onePath,
            Expression<M> manyPath,
            @Nullable BiConsumer<O, M> manyAccumulator) {

        Map<O, O> canonicalKey = new HashMap<>();
        Map<O, Collection<M>> result = new LinkedHashMap<>();
        for (Tuple row : rawResult) {
            O oneItem = Objects.requireNonNull(row.get(onePath),
                    "result for path " + onePath + " not found in tuple " + row);
            M manyItem = Objects.requireNonNull(row.get(manyPath),
                    "result for path " + manyPath + " not found in tuple " + row);

            oneItem = canonicalKey.computeIfAbsent(oneItem, v -> v);
            result.computeIfAbsent(oneItem, o -> new ArrayList<>())
                    .add(manyItem);

            if (manyAccumulator != null) {
                manyAccumulator.accept(oneItem, manyItem);
            }
        }
        return result;
    }

    /**
     * Like {@link #mapOneToMany(Collection, Expression, Expression, BiConsumer)},
     * just without any consumer for additional processing.
     */
    private static <O, M> Map<O, Collection<M>> mapOneToMany(
            Collection<Tuple> rawResult,
            Expression<O> onePath,
            Expression<M> manyPath) {
        return mapOneToMany(rawResult, onePath, manyPath, null);
    }

    private static Connection getConnection() {
        try {
            return java.sql.DriverManager.getConnection("jdbc:h2:tcp://localhost:5437/midpoint", "sa", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static SQLQuery<Object> newQuery(Connection connection) {
        return new SQLQuery<>(connection, new Configuration(SQLTemplates.DEFAULT));
    }
}
