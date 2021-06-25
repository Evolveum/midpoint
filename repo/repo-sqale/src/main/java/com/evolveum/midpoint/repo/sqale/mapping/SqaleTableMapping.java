/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.*;
import com.evolveum.midpoint.repo.sqale.filtering.ArrayPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping superclass with common functions for {@link QObject} and non-objects (e.g. containers).
 * See javadoc in {@link QueryTableMapping} for more.
 *
 * Mappings are often initialized using static `init*(repositoryContext)` methods, various
 * suffixes are used for these reasons:
 *
 * * To differentiate various instances for the same mapping type, e.g. various references
 * stored in separate tables;
 * * or to avoid return type clash of the `init` method, even though they are static
 * and technically independent Java meddles too much.
 *
 * Some subclasses (typically containers and refs) track their instances and avoid unnecessary
 * instance creation for the same init method; the instance is available via matching `get*()`.
 * Other subclasses (most objects) don't have `get()` methods but can be obtained using
 * {@link QueryModelMappingRegistry#getByQueryType(Class)}, or by schema type (e.g. in tests).
 *
 * @param <S> schema type
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 * @see QueryTableMapping
 */
public abstract class SqaleTableMapping<S, Q extends FlexibleRelationalPathBase<R>, R>
        extends QueryTableMapping<S, Q, R>
        implements SqaleMappingMixin<S, Q, R> {

    protected SqaleTableMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);
    }

    public SqaleRepoContext repositoryContext() {
        return (SqaleRepoContext) super.repositoryContext();
    }

    /**
     * Returns the mapper creating the string filter/delta processors from context.
     */
    @Override
    protected ItemSqlMapper<Q, R> stringMapper(
            Function<Q, StringPath> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new SimpleItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the integer filter/delta processors from context.
     */
    @Override
    public ItemSqlMapper<Q, R> integerMapper(
            Function<Q, NumberPath<Integer>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new SimpleItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the boolean filter/delta processors from context.
     */
    @Override
    protected ItemSqlMapper<Q, R> booleanMapper(
            Function<Q, BooleanPath> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new SimpleItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the UUID filter/delta processors from context.
     */
    @Override
    protected ItemSqlMapper<Q, R> uuidMapper(Function<Q, UuidPath> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new SimpleItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the timestamp filter/delta processors from context.
     */
    @Override
    protected <T extends Comparable<T>> ItemSqlMapper<Q, R> timestampMapper(
            Function<Q, DateTimePath<T>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new TimestampItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new TimestampItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the polystring filter/delta processors from context.
     */
    @Override
    protected ItemSqlMapper<Q, R> polyStringMapper(
            @NotNull Function<Q, StringPath> origMapping,
            @NotNull Function<Q, StringPath> normMapping) {
        return new SqaleItemSqlMapper<>(
                ctx -> new PolyStringItemFilterProcessor(ctx, origMapping, normMapping),
                ctx -> new PolyStringItemDeltaProcessor(ctx, origMapping, normMapping),
                origMapping);
    }

    /**
     * Returns the mapper creating the reference filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> refMapper(
            Function<Q, UuidPath> rootToOidPath,
            Function<Q, EnumPath<MObjectType>> rootToTypePath,
            Function<Q, NumberPath<Integer>> rootToRelationIdPath) {
        return new SqaleItemSqlMapper<>(
                ctx -> new RefItemFilterProcessor(ctx,
                        rootToOidPath, rootToTypePath, rootToRelationIdPath),
                ctx -> new RefItemDeltaProcessor(ctx,
                        rootToOidPath, rootToTypePath, rootToRelationIdPath));
    }

    /**
     * Returns the mapper creating the cached URI filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> uriMapper(
            Function<Q, NumberPath<Integer>> rootToPath) {
        return new SqaleItemSqlMapper<>(
                ctx -> new UriItemFilterProcessor(ctx, rootToPath),
                ctx -> new UriItemDeltaProcessor(ctx, rootToPath));
    }

    /**
     * Returns the mapper creating the enum filter/delta processors from context.
     */
    public <E extends Enum<E>> ItemSqlMapper<Q, R> enumMapper(
            @NotNull Function<Q, EnumPath<E>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new EnumItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new EnumItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating string multi-value filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> multiStringMapper(
            Function<Q, ArrayPath<String[], String>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new ArrayPathItemFilterProcessor<String, String>(
                        ctx, rootToQueryItem, "TEXT", String.class, null),
                ctx -> new ArrayItemDeltaProcessor<String, String>(
                        ctx, rootToQueryItem, String.class, null));
    }

    /**
     * Returns the mapper creating cached URI multi-value filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> multiUriMapper(
            Function<Q, ArrayPath<Integer[], Integer>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new ArrayPathItemFilterProcessor<>(
                        ctx, rootToQueryItem, "INTEGER", Integer.class,
                        ((SqaleRepoContext) ctx.repositoryContext())::searchCachedUriId),
                ctx -> new ArrayItemDeltaProcessor<>(ctx, rootToQueryItem, Integer.class,
                        ((SqaleRepoContext) ctx.repositoryContext())::processCacheableUri));
    }

    @Override
    public S toSchemaObject(R row) {
        throw new UnsupportedOperationException("Use toSchemaObject(Tuple,...)");
    }

    /**
     * Transforms row Tuple containing {@link R} under entity path and extension columns.
     */
    @Override
    public S toSchemaObject(Tuple tuple, Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        S schemaObject = toSchemaObject(tuple.get(entityPath));
        processExtensionColumns(schemaObject, tuple, entityPath);
        return schemaObject;
    }

    @SuppressWarnings("unused")
    protected void processExtensionColumns(S schemaObject, Tuple tuple, Q entityPath) {
        // empty by default, can be overridden
    }

    /**
     * Returns {@link ObjectReferenceType} with specified oid, proper type based on
     * {@link MObjectType} and, optionally, target name/description.
     * Returns {@code null} if OID is null.
     * Fails if OID is not null and {@code repoObjectType} is null.
     */
    @Nullable
    protected ObjectReferenceType objectReferenceType(
            @Nullable String oid, MObjectType repoObjectType, String targetName) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid)
                .type(repositoryContext().schemaClassToQName(repoObjectType.getSchemaType()))
                .description(targetName)
                .targetName(targetName);
    }

    /**
     * Trimming the value to the column size from column metadata (must be specified).
     */
    protected @Nullable String trim(
            @Nullable String value, @NotNull ColumnMetadata columnMetadata) {
        if (!columnMetadata.hasSize()) {
            throw new IllegalArgumentException(
                    "trimString with column metadata without specified size: " + columnMetadata);
        }
        return MiscUtil.trimString(value, columnMetadata.getSize());
    }

    /**
     * Returns ID for relation QName creating new {@link QUri} row in DB as needed.
     * Relation is normalized before consulting the cache.
     * Never returns null, returns default ID for configured default relation.
     */
    protected Integer processCacheableRelation(QName qName) {
        return repositoryContext().processCacheableRelation(qName);
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    protected Integer processCacheableUri(String uri) {
        return uri != null
                ? repositoryContext().processCacheableUri(uri)
                : null;
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    protected Integer processCacheableUri(QName qName) {
        return qName != null
                ? repositoryContext().processCacheableUri(QNameUtil.qNameToUri(qName))
                : null;
    }

    /**
     * Returns IDs as Integer array for URI strings creating new cache row in DB as needed.
     * Returns null for null or empty list on input.
     */
    protected Integer[] processCacheableUris(List<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return null;
        }
        return uris.stream()
                .map(uri -> processCacheableUri(uri))
                .toArray(Integer[]::new);
    }

    protected @Nullable UUID oidToUUid(@Nullable String oid) {
        return oid != null ? UUID.fromString(oid) : null;
    }

    protected MObjectType schemaTypeToObjectType(QName schemaType) {
        return schemaType == null ? null :
                MObjectType.fromSchemaType(repositoryContext().qNameToSchemaClass(schemaType));
    }

    protected void setPolyString(PolyStringType polyString,
            Consumer<String> origConsumer, Consumer<String> normConsumer) {
        if (polyString != null) {
            origConsumer.accept(polyString.getOrig());
            normConsumer.accept(polyString.getNorm());
        }
    }

    protected void setReference(ObjectReferenceType ref,
            Consumer<UUID> targetOidConsumer, Consumer<MObjectType> targetTypeConsumer,
            Consumer<Integer> relationIdConsumer) {
        if (ref != null) {
            targetOidConsumer.accept(oidToUUid(ref.getOid()));
            targetTypeConsumer.accept(schemaTypeToObjectType(ref.getType()));
            relationIdConsumer.accept(processCacheableRelation(ref.getRelation()));
        }
    }

    protected <REF extends MReference, OQ extends FlexibleRelationalPathBase<OR>, OR> void storeRefs(
            @NotNull OR ownerRow, @NotNull List<ObjectReferenceType> refs,
            @NotNull QReferenceMapping<?, REF, OQ, OR> mapping, @NotNull JdbcSession jdbcSession) {
        if (!refs.isEmpty()) {
            refs.forEach(ref -> mapping.insert(ref, ownerRow, jdbcSession));
        }
    }

    protected String[] listToArray(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        return strings.toArray(String[]::new);
    }

    /** Convenient insert shortcut when the row is fully populated. */
    protected void insert(R row, JdbcSession jdbcSession) {
        jdbcSession.newInsert(defaultAlias())
                .populate(row)
                .execute();
    }

    /**
     * Adds extension container mapping, mainly the resolver for the extension container path.
     *
     * @param <C> schema type of the extension container
     */
    public <C extends Containerable> void addExtensionMapping(
            @NotNull ItemName itemName,
            @NotNull Class<C> nestedSchemaType,
            @NotNull Function<Q, JsonbPath> rootToPath) {
        ExtensionMapping<C, Q, R> mapping =
                new ExtensionMapping<>(nestedSchemaType, queryType(), rootToPath);
        addRelationResolver(itemName, new ExtensionMappingResolver<>(mapping, rootToPath));
    }

    /** Converts extension container to the JSONB value. */
    protected Jsonb processExtensions(Containerable extContainer, MExtItemHolderType holderType) {
        if (extContainer == null) {
            return null;
        }

        return new ExtensionProcessor(repositoryContext())
                .processExtensions(extContainer, holderType);
    }
}
