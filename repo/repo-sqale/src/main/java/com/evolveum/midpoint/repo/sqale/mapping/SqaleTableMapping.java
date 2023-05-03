/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.delta.item.*;
import com.evolveum.midpoint.repo.sqale.filtering.ArrayPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.JsonbPolysPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.UuidItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryObjectParseResult;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.SimpleItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.ItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.RepositoryMappingException;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Mapping superclass with common functions for {@link QObject} and non-objects (e.g. containers).
 * See javadoc in {@link QueryTableMapping} for more.
 *
 * Mappings are typically initialized using static `init*(repositoryContext)` methods, various
 * suffixes are used for these reasons:
 *
 * * To differentiate various instances for the same mapping type, e.g. various references
 * stored in separate tables.
 * * To avoid return type clash of the `init` methods in the hierarchy.
 * Even though they are static and technically independent, Java meddles too much.
 * * And finally, to avoid accidental use of static method from the superclass (this should not
 * be even a thing!).
 *
 * For object mappings the reuse is not that important and mapping is simply reinitialized.
 * For container and ref mappings the same instance can be reused from various subclasses
 * of object mapping and reuse is desired.
 * Initialization method does not check only `null` but also forces reinitialization if different
 * `repositoryContext` is provided; this is only used for testing purposes.
 * Mappings are not built to be run with multiple repository contexts in the same runtime.
 *
 * [IMPORTANT]
 * ====
 * The mappings are created in the constructors and subtypes depend on their supertypes and objects
 * depend on their parts (container/ref tables).
 * This does not create any confusion and `init` methods can be called multiple times from
 * various objects, whatever comes first initializes the mapping and the rest reuses it.
 *
 * *But cross-references can cause recursive initialization and stack overflow* and must be solved
 * differently, either after all the mappings are initialized or the mappings must be provided
 * indirectly/lazily, e.g. using {@link Supplier}, etc.
 * ====
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

    protected static boolean needsInitialization(
            SqaleTableMapping<?, ?, ?> instance, SqaleRepoContext repositoryContext) {
        return instance == null || instance.repositoryContext() != repositoryContext;
    }

    @Override
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
                ctx -> new SinglePathItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    protected ItemSqlMapper<Q, R> binaryMapper(
            Function<Q, ArrayPath<byte[], Byte>> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new SimpleItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new SinglePathItemDeltaProcessor<>(ctx, rootToQueryItem),
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
                ctx -> new SinglePathItemDeltaProcessor<>(ctx, rootToQueryItem),
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
                ctx -> new SinglePathItemDeltaProcessor<>(ctx, rootToQueryItem),
                rootToQueryItem);
    }

    /**
     * Returns the mapper creating the UUID filter/delta processors from context.
     */
    @Override
    protected ItemSqlMapper<Q, R> uuidMapper(Function<Q, UuidPath> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new UuidItemFilterProcessor(ctx, rootToQueryItem),
                ctx -> new SinglePathItemDeltaProcessor<>(ctx, rootToQueryItem),
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
                ctx -> new PolyStringItemFilterProcessor<>(ctx, origMapping, normMapping),
                ctx -> new PolyStringItemDeltaProcessor(ctx, origMapping, normMapping),
                origMapping);
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
     * Returns the mapper creating poly-string multi-value filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> multiPolyStringMapper(
            @NotNull Function<Q, JsonbPath> rootToQueryItem) {
        return new SqaleItemSqlMapper<>(
                ctx -> new JsonbPolysPathItemFilterProcessor<>(ctx, rootToQueryItem),
                ctx -> new JsonbPolysItemDeltaProcessor(ctx, rootToQueryItem));
    }

    /**
     * Returns the mapper creating string multi-value filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> multiStringMapper(
            Function<Q, ArrayPath<String[], String>> rootToQueryItem) {
        return multiValueMapper(rootToQueryItem, String.class, "TEXT", null, null);
    }

    /**
     * Returns the mapper creating cached URI multi-value filter/delta processors from context.
     */
    protected ItemSqlMapper<Q, R> multiUriMapper(
            Function<Q, ArrayPath<Integer[], Integer>> rootToQueryItem) {
        return multiValueMapper(rootToQueryItem, Integer.class, "INTEGER",
                repositoryContext()::searchCachedUriId,
                repositoryContext()::processCacheableUri);
    }

    /**
     * Returns the mapper creating general array-stored multi-value filter/delta processors.
     *
     * @param <VT> real-value type from schema
     * @param <ST> stored type (e.g. String for TEXT[])
     * @param dbType name of the type for element in DB (without []) for the cast part of the condition
     * @param elementType class necessary for array creation; must be a class convertable to {@code dbType} by PG JDBC driver
     */
    protected <VT, ST> ItemSqlMapper<Q, R> multiValueMapper(
            Function<Q, ArrayPath<ST[], ST>> rootToQueryItem,
            Class<ST> elementType,
            String dbType,
            @Nullable Function<VT, ST> queryConversionFunction,
            @Nullable Function<VT, ST> updateConversionFunction) {
        return new SqaleItemSqlMapper<>(
                ctx -> new ArrayPathItemFilterProcessor<>(
                        ctx, rootToQueryItem, dbType, elementType, queryConversionFunction),
                ctx -> new ArrayItemDeltaProcessor<>(
                        ctx, rootToQueryItem, elementType, updateConversionFunction));
    }

    @Override
    public S toSchemaObject(R row) throws SchemaException {
        throw new UnsupportedOperationException(
                "Not implemented for " + getClass() + ". Perhaps use toSchemaObject(Tuple,...)?");
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
    protected ObjectReferenceType objectReference(
            @Nullable UUID oid, MObjectType repoObjectType, Integer relationId) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid.toString())
                .type(objectTypeToQName(repoObjectType))
                .relation(resolveUriIdToQName(relationId));
    }

    /**
     * Object reference with target name.
     */
    @Nullable
    protected ObjectReferenceType objectReference(
            @Nullable UUID oid, MObjectType repoObjectType, String targetName) {
        if (oid == null) {
            return null;
        }
        if (repoObjectType == null) {
            throw new IllegalArgumentException(
                    "NULL object type provided for object reference with OID " + oid);
        }

        return new ObjectReferenceType()
                .oid(oid.toString())
                .type(objectTypeToQName(repoObjectType))
                .description(targetName)
                .targetName(targetName);
    }

    @Nullable
    protected QName objectTypeToQName(MObjectType objectType) {
        return objectType != null
                ? repositoryContext().schemaClassToQName(objectType.getSchemaType())
                : null;
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

    public String resolveIdToUri(Integer uriId) {
        return repositoryContext().resolveIdToUri(uriId);
    }

    public QName resolveUriIdToQName(Integer uriId) {
        return repositoryContext().resolveUriIdToQName(uriId);
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

    protected void setReference(Referencable ref,
            Consumer<UUID> targetOidConsumer,
            Consumer<MObjectType> targetTypeConsumer,
            Consumer<Integer> relationIdConsumer) {
        if (ref != null) {
            if (ref.getType() == null) {
                ref = SqaleUtils.referenceWithTypeFixed(ref);
            }
            targetOidConsumer.accept(SqaleUtils.oidToUUid(ref.getOid()));
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

    protected String[] stringsToArray(Collection<String> strings) {
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
     */
    public void addExtensionMapping(
            @NotNull ItemName itemName,
            @NotNull MExtItemHolderType holderType,
            @NotNull Function<Q, JsonbPath> rootToPath) {
        ExtensionMapping<Q, R> mapping =
                new ExtensionMapping<>(holderType, queryType(), rootToPath, repositoryContext());
        addRelationResolver(itemName, new ExtensionMappingResolver<>(mapping, rootToPath));
        addItemMapping(itemName, new SqaleItemSqlMapper<>(
                ctx -> new ExtensionContainerDeltaProcessor<>(ctx, mapping, rootToPath)));
    }

    /** Converts extension container to the JSONB value. */
    protected Jsonb processExtensions(Containerable extContainer, MExtItemHolderType holderType) {
        if (extContainer == null) {
            return null;
        }

        return new ExtensionProcessor(repositoryContext())
                .processExtensions(extContainer, holderType);
    }

    protected S parseSchemaObject(byte[] fullObject, String identifier) throws SchemaException {
        return parseSchemaObject(fullObject, identifier, schemaType());
    }

    protected <T> T parseSchemaObject(byte[] fullObject, String identifier, Class<T> clazz) throws SchemaException {
        String serializedForm = fullObject != null
                ? new String(fullObject, StandardCharsets.UTF_8)
                : null;
        try {
            RepositoryObjectParseResult<T> result =
                    repositoryContext().parsePrismObject(serializedForm, clazz);
            T schemaObject = result.prismValue;
            if (result.parsingContext.hasWarnings()) {
                logger.warn("Object {} parsed with {} warnings",
                        schemaObject.toString(),
                        result.parsingContext.getWarnings().size());
            }
            return schemaObject;
        } catch (SchemaException | RuntimeException | Error e) {
            // This is a serious thing. We have corrupted serialized form in the repo.
            // This may happen even during system init. We want really loud and detailed error here.
            // The stacktrace is not reported here, there is a rethrow, and the client code can do that if needed.
            // The message is enough to fix the problem.
            logger.error("Couldn't parse object {} {}: {}: {}\nSerialized form: '{}'",
                    clazz.getSimpleName(), identifier,
                    e.getClass().getName(), e.getMessage(), serializedForm);
            throw e;
        }
    }

    /** Creates serialized (byte array) form of an object or a container. */
    public <C extends Containerable> byte[] createFullObject(C container) throws SchemaException {
        repositoryContext().normalizeAllRelations(container.asPrismContainerValue());
        return repositoryContext().createStringSerializer()
                .itemsToSkip(fullObjectItemsToSkip())
                .options(SerializationOptions
                        .createSerializeReferenceNamesForNullOids()
                        .skipIndexOnly(true)
                        .skipTransient(true)
                        .skipWhitespaces(true))
                .serialize(container.asPrismContainerValue())
                .getBytes(StandardCharsets.UTF_8);
    }

    protected Collection<? extends QName> fullObjectItemsToSkip() {
        return Collections.emptyList();
    }

    // TODO: this resolves the names after toSchemaObject(3 params), but...
    //  called version of toSchemaObject() doesn't have forceFull flag, so sometimes we need to override this version.
    //  But calling this via super...() means it can't resolve the names in refs from additional parts
    //  that are not loaded yet (which is done after super call).
    //  What is the recommended template for override? Wouldn't using this method with additional stuff in the middle be better?
    //  1. call toSchemaObject(rowTuple, entityPath, options) - just like here, don't call this 5 param method via super!
    //  2. do additional stuff, utilizing forceFull flag (and even jdbcSession if necessary)
    //  3. call resolveReferenceNames just like in this method - at the end
    //  Alternative:
    //  - make this method final
    //  - override only toSchemaObject - but add forceFull flag (do we need jdbcSession as well? so far not)
    public S toSchemaObject(
            Tuple rowTuple,
            Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession,
            boolean forceFull) throws SchemaException {
        return toSchemaObject(rowTuple, entityPath, options);
    }

    public S toSchemaObjectWithResolvedNames(
            Tuple rowTuple,
            Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession,
            boolean forceFull) throws SchemaException {
        S schemaObject = toSchemaObject(rowTuple, entityPath, options, jdbcSession, forceFull);
        schemaObject = resolveReferenceNames(schemaObject, jdbcSession, options);
        return schemaObject;
    }

    public S toSchemaObjectSafe(
            Tuple tuple,
            Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession,
            boolean forceFull) {
        try {
            return toSchemaObjectWithResolvedNames(tuple, entityPath, options, jdbcSession, forceFull);
        } catch (SchemaException e) {
            throw new RepositoryMappingException(e);
        }
    }

    protected <O> O resolveReferenceNames(O object, JdbcSession session, Collection<SelectorOptions<GetOperationOptions>> options) {
        // TODO: Performance: This could be transaction shared object
        return ReferenceNameResolver.from(options).resolve(object, session);
    }

    @Override
    public ResultListRowTransformer<S, Q, R> createRowTransformer(
            SqlQueryContext<S, Q, R> sqlQueryContext, JdbcSession jdbcSession) {
        return (tuple, entityPath, options) ->
                toSchemaObjectSafe(tuple, entityPath, options, jdbcSession, false);
    }
}
