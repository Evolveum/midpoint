package com.evolveum.midpoint.repo.sql.pure;

import java.time.Instant;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

/**
 * Extension of {@link RelationalPathBase} that provides midPoint specific features.
 * This allows to add metadata about Prism objects in addition to pure DB metadata.
 * <p>
 * Typical migration from originally generated Querydsl class:
 * <ul>
 * <li>extend from this class instead of {@code RelationalPathBase}</li>
 * <li>rename fields as needed (missing uppercase for words), also in related bean</li>
 * <li>rename static final field to capitalized table name (this will stand out + fix checkstyle)</li>
 * <li>simplify bean to public fields and no setters/getters</li>
 * <li>add PK-based equals/hashCode to beans (not critical, but handy for grouping transformations)</li>
 * <li>TODO: add prism-related metadata?</li>
 * <li>TODO: change types as needed, e.g. date/time-related? (also in bean)</li>
 * </ul>
 *
 * @param <T> entity type - typically a pure DTO bean for the table mapped by Q-type
 */
public abstract class FlexibleRelationalPathBase<T> extends RelationalPathBase<T> {

    private static final long serialVersionUID = -3374516272567011096L;

    public FlexibleRelationalPathBase(
            Class<? extends T> type, PathMetadata metadata, String schema, String table) {
        super(type, metadata, schema, table);
    }

    /**
     * Creates {@link NumberPath} for a number property and registers column metadata for it.
     */
    protected <A extends Number & Comparable<?>> NumberPath<A> createNumber(
            String property, Class<A> type, ColumnMetadata columnMetadata) {
        return addMetadata(createNumber(property, type), columnMetadata);
    }

    /**
     * Creates {@link NumberPath} for an Integer property and registers column metadata for it.
     */
    protected NumberPath<Integer> createInteger(
            String property, ColumnMetadata columnMetadata) {
        return createNumber(property, Integer.class, columnMetadata);
    }

    /**
     * Creates {@link NumberPath} for a Long property and registers column metadata for it.
     */
    protected NumberPath<Long> createLong(
            String property, ColumnMetadata columnMetadata) {
        return createNumber(property, Long.class, columnMetadata);
    }

    /**
     * Creates {@link StringPath} and for a property registers column metadata for it.
     */
    protected StringPath createString(String property, ColumnMetadata columnMetadata) {
        return addMetadata(createString(property), columnMetadata);
    }

    /**
     * Creates {@link DateTimePath} for a property and registers column metadata for it.
     */
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
}
