/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import static com.evolveum.midpoint.repo.sqale.ExtUtils.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.ExtUtils;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.delta.item.*;
import com.evolveum.midpoint.repo.sqale.filtering.ArrayPathItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.filtering.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality;
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
import com.evolveum.midpoint.util.exception.SystemException;
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

    // region extension processing

    /** Contains ext item from catalog and additional info needed for processing. */
    private static class ExtItemInfo {
        public MExtItem item;
        public QName defaultRefTargetType;

        public String getId() {
            return item.id.toString();
        }
    }

    protected Jsonb processExtensions(Containerable extContainer, MExtItemHolderType holderType) {
        if (extContainer == null) {
            return null;
        }

        Map<String, Object> extMap = new LinkedHashMap<>();
        PrismContainerValue<?> prismContainerValue = extContainer.asPrismContainerValue();
        for (Item<?, ?> item : prismContainerValue.getItems()) {
            try {
                ExtItemInfo extItemInfo = findExtensionItem(item, holderType);
                if (extItemInfo == null) {
                    continue; // not-indexed, skipping this item
                }
                extMap.put(extItemInfo.getId(), extItemValue(item, extItemInfo));
            } catch (RuntimeException e) {
                // If anything happens (like NPE in Map.of) we want to capture the "bad" item.
                throw new SystemException(
                        "Unexpected exception while processing extension item " + item, e);
            }
        }

        try {
            return Jsonb.from(extMap);
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    /** Returns ext item definition or null if the item is not indexed and should be skipped. */
    private ExtItemInfo findExtensionItem(Item<?, ?> item, MExtItemHolderType holderType) {
        Objects.requireNonNull(item, "Object for converting must not be null.");

        ItemDefinition<?> definition = item.getDefinition();
        MExtItem extItem = repositoryContext().resolveExtensionItem(definition, holderType);
        if (extItem == null) {
            return null;
        }

        // TODO review any need for shadow attributes, now they are stored fine, but the code here
        //  is way too simple compared to the old repo.

        ExtItemInfo info = new ExtItemInfo();
        info.item = extItem;
        if (definition instanceof PrismReferenceDefinition) {
            info.defaultRefTargetType = ((PrismReferenceDefinition) definition).getTargetTypeName();
        }

        return info;
    }

    private Object extItemValue(Item<?, ?> item, ExtItemInfo extItemInfo) {
        MExtItem extItem = extItemInfo.item;
        if (extItem.cardinality == MExtItemCardinality.ARRAY) {
            List<Object> vals = new ArrayList<>();
            for (Object realValue : item.getRealValues()) {
                vals.add(convertExtItemValue(realValue, extItemInfo));
            }
            return vals;
        } else {
            return convertExtItemValue(item.getRealValue(), extItemInfo);
        }
    }

    private Object convertExtItemValue(Object realValue, ExtItemInfo extItemInfo) {
        if (realValue instanceof String
                || realValue instanceof Number
                || realValue instanceof Boolean) {
            return realValue;
        }

        if (realValue instanceof PolyString) {
            PolyString poly = (PolyString) realValue;
            return Map.of(EXT_POLY_ORIG_KEY, poly.getOrig(),
                    EXT_POLY_NORM_KEY, poly.getNorm());
        }

        if (realValue instanceof Referencable) {
            Referencable ref = (Referencable) realValue;
            // we always want to store the type for consistent search results
            QName targetType = ref.getType();
            if (targetType == null) {
                targetType = extItemInfo.defaultRefTargetType;
            }
            if (targetType == null) {
                throw new IllegalArgumentException(
                        "Reference without target type can't be stored: " + ref);
            }
            return Map.of(EXT_REF_TARGET_OID_KEY, ref.getOid(),
                    EXT_REF_TARGET_TYPE_KEY, schemaTypeToObjectType(targetType),
                    EXT_REF_RELATION_KEY, processCacheableRelation(ref.getRelation()));
        }

        if (realValue instanceof Enum) {
            return realValue.toString();
        }

        if (realValue instanceof XMLGregorianCalendar) {
            // XMLGregorianCalendar stores only millis, but we cut it to 3 fraction digits
            // to make the behavior explicit and consistent.
            return ExtUtils.extensionDateTime((XMLGregorianCalendar) realValue);
        }

        throw new IllegalArgumentException(
                "Unsupported type '" + realValue.getClass() + "' for value '" + realValue + "'.");
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
        addRelationResolver(itemName, new ExtensionMappingResolver<>(mapping, itemName));
    }
    // endregion
}
