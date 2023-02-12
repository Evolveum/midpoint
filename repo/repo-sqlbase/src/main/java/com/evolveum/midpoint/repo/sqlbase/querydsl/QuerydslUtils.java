/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.sql.Timestamp;
import java.time.Instant;
import javax.xml.datatype.XMLGregorianCalendar;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;

public enum QuerydslUtils {
    ;

    public static final BooleanExpression EXPRESSION_TRUE = Expressions.asBoolean(true).isTrue();
    public static final BooleanExpression EXPRESSION_FALSE = Expressions.asBoolean(true).isFalse();
    public static final StringTemplate EXPRESSION_ONE = Expressions.stringTemplate("1");

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
            case POSTGRESQL:
                querydslConfiguration =
                        new Configuration(MidpointPostgreSQLTemplates.DEFAULT);
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

        return querydslConfiguration;
    }

    /**
     * Tries to convert value of type {@link XMLGregorianCalendar}
     * to paths of {@link Instant} (most likely used), {@link Timestamp} and {@link Long}.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> T convertTimestampToPathType(
            @NotNull Object value, @NotNull DateTimePath<T> path) {
        if (value.getClass() == path.getType()) {
            return (T) value;
        }

        long timestamp;
        if (value instanceof XMLGregorianCalendar) {
            timestamp = MiscUtil.asMillis((XMLGregorianCalendar) value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported temporal type " + value.getClass() + " for value: " + value);
        }
        Class<?> pathType = path.getType();
        if (Long.class.isAssignableFrom(pathType)) {
            value = timestamp;
        } else if (Instant.class.isAssignableFrom(pathType)) {
            value = Instant.ofEpochMilli(timestamp);
        } else if (Timestamp.class.isAssignableFrom(pathType)) {
            value = new Timestamp(timestamp);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported temporal type " + pathType + " for path: " + path);
        }
        return (T) value;
    }
}
