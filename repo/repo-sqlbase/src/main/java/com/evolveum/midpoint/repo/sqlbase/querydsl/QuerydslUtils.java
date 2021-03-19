/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.util.*;
import java.util.function.BiConsumer;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.util.exception.SystemException;

// TODO MID-6318, MID-6319 review what needed (let's say in 2021), drop the rest
public enum QuerydslUtils {
    ;

    /**
     * Returns configuration for Querydsl based on the used database type.
     */
    public static Configuration querydslConfiguration(SupportedDatabase databaseType) {
        Configuration querydslConfiguration;
        switch (databaseType) {
            case H2:
                querydslConfiguration =
                        new Configuration(H2Templates.DEFAULT);
                break;
            case MYSQL:
            case MARIADB:
                querydslConfiguration =
                        new Configuration(MySQLTemplates.DEFAULT);
                break;
            case POSTGRESQL:
                querydslConfiguration =
                        new Configuration(PostgreSQLTemplates.DEFAULT);
                break;
            case SQLSERVER:
                querydslConfiguration =
                        new Configuration(MidpointSQLServerTemplates.DEFAULT);
                break;
            case ORACLE:
                querydslConfiguration =
                        new Configuration(MidpointOracleTemplates.DEFAULT);
                break;
            default:
                throw new SystemException(
                        "Unsupported database type " + databaseType + " for Querydsl config");
        }

        // See InstantType javadoc for the reasons why we need this to support Instant.
        // Alternatively we may stick to Timestamp and go on with our miserable lives. ;-)
        querydslConfiguration.register(new QuerydslInstantType());

        // register other repository implementation specific types (like enums) out of this call

        // logger on com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger
        // DEBUG = show query, TRACE = add parameter values too (bindings)
        querydslConfiguration.addListener(new SqlLogger());
        return querydslConfiguration;
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
    public static <O, M> Map<O, Collection<M>> mapOneToMany(
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
    public static <O, M> Map<O, Collection<M>> mapOneToMany(
            Collection<Tuple> rawResult,
            Expression<O> onePath,
            Expression<M> manyPath) {
        return mapOneToMany(rawResult, onePath, manyPath, null);
    }
}
