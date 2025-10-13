/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase.querydsl;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;

/**
 * Extension of {@link RelationalPathBase} that adapts the Q-class to midPoint
 * (mainly extension columns) while preserving all the features provided by Querydsl.
 *
 * The subclasses of this class for midPoint tables are written manually.
 * The following procedure describes the migration from originally generated Querydsl class
 * to the current state and while not used for newer classes can offer some answers:
 *
 * . Extend from this class instead of {@link RelationalPathBase}.
 *
 * . Extract constants for all column metadata from {@link #addMetadata} method calls.
 * Remove index information from them (column order, nothing to do with DB indexes).
 *
 * . Use {@link Types#VARCHAR} for text columns, never {@link Types#NVARCHAR},
 * the proper DB type will be used as needed (e.g. NVARCHAR for SQL Server).
 * {@link Types#NVARCHAR} is not supported in PG driver at all and would cause problems.
 *
 * . Rename the column name to conform with SQL Server (if still relevant), because it is
 * case-sensitive even about column names if *_CS_* collation is used!
 *
 * . Rewrite path fields so they use `create*()` methods from this super-class.
 *
 * . Now `addMetadata()` calls can be removed, including usages from constructors.
 *
 * . Prune constructors, two should be enough (see existing Q-classes).
 *
 * . Introduce `TABLE_NAME` constant - keep the names lowercase for MySQL (don't ask).
 *
 * . Rename path fields as needed (missing uppercase for words), also in related bean (M-class).
 *
 * . Unsuitable path types can be changed, e.g. date/time related.
 * The same changes must be done for the related field in the M-class.
 * Exotic type support can be added to Querydsl configuration (see usages of InstantType).
 *
 * . Remove default static final aliases, {@link QueryTableMapping} for the table will be
 * responsible for providing aliases, including default ones.
 * This better handles extension columns, static default alias would not easily know about them).
 *
 * . Simplify bean (M-class) to public fields with no setters/getters.
 *
 * . Add PK-based equals/hashCode to beans (not critical, but handy for grouping transformations).
 *
 * . Now it's time to create `@code QYourTypeMapping`, see any subclass of {@link QueryTableMapping}
 * as example, then register the mapping with {@link QueryModelMappingRegistry#register}.
 * The registration call typically appears in some bean configuration class.
 *
 * @param <R> row ("entity bean") type - typically a pure DTO bean for the table mapped by Q-type
 */
public class FlexibleRelationalPathBase<R> extends RelationalPathBase<R> {

    public static final String DEFAULT_SCHEMA_NAME = "PUBLIC";

    private static final long serialVersionUID = -3374516272567011096L;

    private final Map<String, Path<?>> propertyNameToPath = new LinkedHashMap<>();

    public FlexibleRelationalPathBase(
            Class<? extends R> type, String pathVariable, String schema, String table) {
        super(type, forVariable(pathVariable), schema, table);
    }

    /** Creates {@link NumberPath} for a number property and registers column metadata for it. */
    protected <A extends Number & Comparable<?>> NumberPath<A> createNumber(
            String property, Class<A> type, ColumnMetadata columnMetadata) {
        return addMetadata(createNumber(property, type), columnMetadata);
    }

    /** Creates {@link NumberPath} for an Integer property and registers column metadata for it. */
    protected NumberPath<Integer> createInteger(
            String property, ColumnMetadata columnMetadata) {
        return createNumber(property, Integer.class, columnMetadata);
    }

    /** Creates {@link NumberPath} for a Long property and registers column metadata for it. */
    protected NumberPath<Long> createLong(
            String property, ColumnMetadata columnMetadata) {
        return createNumber(property, Long.class, columnMetadata);
    }

    /** Creates {@link BooleanPath} for a property and registers column metadata for it. */
    protected BooleanPath createBoolean(
            String property, ColumnMetadata columnMetadata) {
        return addMetadata(createBoolean(property), columnMetadata);
    }

    /** Creates {@link StringPath} and for a property registers column metadata for it. */
    public StringPath createString(String property, ColumnMetadata columnMetadata) {
        return addMetadata(createString(property), columnMetadata);
    }

    /** Creates {@link EnumPath} for a property and registers column metadata for it. */
    protected <A extends Enum<A>> EnumPath<A> createEnum(
            String property, Class<A> type, ColumnMetadata columnMetadata) {
        return addMetadata(createEnum(property, type), columnMetadata);
    }

    /** Creates {@link DateTimePath} for a property and registers column metadata for it. */
    @SuppressWarnings("rawtypes")
    protected <A extends Comparable> DateTimePath<A> createDateTime(
            String property, Class<? super A> type, ColumnMetadata columnMetadata) {
        return addMetadata(createDateTime(property, type), columnMetadata);
    }

    /**
     * Creates {@link DateTimePath} for an {@link Instant} property
     * and registers column metadata for it.
     */
    protected DateTimePath<Instant> createInstant(
            String property, ColumnMetadata columnMetadata) {
        return createDateTime(property, Instant.class, columnMetadata);
    }

    /** Creates byte array path for a property and registers column metadata for it. */
    protected ArrayPath<byte[], Byte> createByteArray(
            String property, ColumnMetadata columnMetadata) {
        return addMetadata(createArray(property, byte[].class), columnMetadata);
    }

    protected <E> ArrayPath<E[], E> createArray(
            String property, Class<E[]> elementType, ColumnMetadata columnMetadata) {
        return addMetadata(createArray(property, elementType), columnMetadata);
    }

    /** Creates {@link UuidPath} path for a property and registers column metadata for it. */
    protected UuidPath createUuid(
            String property, ColumnMetadata columnMetadata) {
        return addMetadata(add(new UuidPath(forProperty(property))), columnMetadata);
    }

    /**
     * Works like default {@link RelationalPathBase#addMetadata(Path, ColumnMetadata)}
     * and on top of it adds the information necessary to use dynamic/extension columns
     * using methods like {@link #getPath(String)}.
     */
    @Override
    protected <P extends Path<?>> P addMetadata(P path, ColumnMetadata metadata) {
        String pathName = path.getMetadata().getName();
        Path<?> overridden = propertyNameToPath.put(pathName, path);
        if (overridden != null) {
            throw new IllegalArgumentException(
                    "Trying to override metadata for query path " + pathName);
        }
        return super.addMetadata(path, metadata);
    }

    /**
     * Returns {@link Path} expression by the property name.
     * This is useful for dynamic/extension columns that are not otherwise directly accessible.
     */
    public @Nullable Path<?> getPath(@NotNull String propertyName) {
        return propertyNameToPath.get(propertyName);
    }
}
