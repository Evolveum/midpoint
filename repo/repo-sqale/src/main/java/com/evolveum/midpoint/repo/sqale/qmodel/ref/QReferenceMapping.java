/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.filtering.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.QOwnedByMapping;
import com.evolveum.midpoint.repo.sqale.mapping.RefTableTargetResolver;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.DefaultItemSqlMapper;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Base mapping between {@link QReference} subclasses and {@link ObjectReferenceType}.
 * See subtypes for mapping instances for specific tables and see {@link MReferenceType} as well.
 *
 * @param <Q> type of entity path for the reference table
 * @param <R> row type related to the {@link Q}
 * @param <OQ> query type of the reference owner
 * @param <OR> row type of the reference owner (related to {@link OQ})
 */
public class QReferenceMapping<
        Q extends QReference<R, OR>,
        R extends MReference,
        OQ extends FlexibleRelationalPathBase<OR>,
        OR>
        extends SqaleTableMapping<ObjectReferenceType, Q, R>
        implements QOwnedByMapping<ObjectReferenceType, R, OR> {

    // see also subtype specific alias names defined for instances below
    public static final String DEFAULT_ALIAS_NAME = "ref";

    public static final Map<Class<?>, Map<ItemPath, QReferenceMapping<?, ?, ?, ?>>>
            MAPPING_BY_OWNER_TYPE_AND_PATH = new HashMap<>();

    protected final ItemPath referencePath;

    public static QReferenceMapping<?, ?, ?, ?> init(@NotNull SqaleRepoContext repositoryContext) {
        return new QReferenceMapping<>(
                QReference.TABLE_NAME, DEFAULT_ALIAS_NAME, QReference.CLASS, repositoryContext,
                QObjectMapping::getObjectMapping);
    }

    public static void registerByOwnerTypeAndPath(
            Class<?> ownerType, ItemPath itemPath, QReferenceMapping<?, ?, ?, ?> mapping) {
        Map<ItemPath, QReferenceMapping<?, ?, ?, ?>> ownerSubmap =
                MAPPING_BY_OWNER_TYPE_AND_PATH.computeIfAbsent(ownerType, k -> new HashMap<>());
        ownerSubmap.put(itemPath, mapping);
    }

    public static QReferenceMapping<?, ?, ?, ?> getByOwnerTypeAndPath(Class<?> ownerType, ItemPath itemPath) {
        // get() is not enough, we need to support sub-typing as well
        for (var entry : MAPPING_BY_OWNER_TYPE_AND_PATH.entrySet()) {
            Class<?> ownerTypeCandidate = entry.getKey();
            if (ownerTypeCandidate.isAssignableFrom(ownerType)) {
                QReferenceMapping<?, ?, ?, ?> mapping = entry.getValue().get(itemPath);
                if (mapping != null) {
                    return mapping;
                }
            }
        }
        return null;
    }

    protected <TQ extends QObject<TR>, TR extends MObject> QReferenceMapping(
            String tableName,
            String defaultAliasName,
            Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        this(tableName, defaultAliasName, queryType, repositoryContext, targetMappingSupplier,
                null, null, null, null);
    }

    /**
     * Constructor with owner related parameters to support reference search.
     *
     * @param queryType Querydsl query type related to this mapping
     * @param targetMappingSupplier supplier of the query mapping for the reference target
     * @param ownerMappingSupplier optional supplier for reference owner, needed for .. (parent) resolution
     * and support of reference search
     * @param ownerJoin JOIN function for parent resolution
     * @param ownerType needed for registration for reference search later; this cannot be taken
     * from the ownerMappingSupplier yet, because the supplier may still return null at this moment
     * @param referencePath item path to the reference in the owner
     * @param <OS> reference owner schema type, needed only for safer typing in the constructor
     * @param <TQ> type of target entity path
     * @param <TR> row type related to the {@link TQ}
     */
    protected <OS, TQ extends QObject<TR>, TR extends MObject> QReferenceMapping(
            String tableName,
            String defaultAliasName,
            Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier,
            @Nullable Supplier<QueryTableMapping<OS, OQ, OR>> ownerMappingSupplier,
            @Nullable BiFunction<Q, OQ, Predicate> ownerJoin,
            @Nullable Class<?> ownerType,
            @Nullable ItemPath referencePath) {
        super(tableName, defaultAliasName, ObjectReferenceType.class, queryType, repositoryContext);

        if (ownerMappingSupplier != null) {
            if (ownerJoin == null || ownerType == null || referencePath == null) {
                throw new IllegalArgumentException("When owner mapping is provided,"
                        + " owner JOIN function, owner type and referencePath must be provided too."
                        + " Mapping for table '" + tableName + "', default alias '" + defaultAliasName + "'.");
            }
            addRelationResolver(PrismConstants.T_PARENT,
                    // Mapping supplier is used to avoid cycles in the initialization code.
                    TableRelationResolver.usingJoin(ownerMappingSupplier, ownerJoin));

            // This supports REF(.) used in reference searches.
            addItemMapping(ItemName.SELF_NAME, new DefaultItemSqlMapper<>(
                    ctx -> new RefItemFilterProcessor(ctx,
                            q -> q.targetOid,
                            q -> q.targetType,
                            q -> q.relationId,
                            null)));

            registerByOwnerTypeAndPath(ownerType, referencePath, this);
        }

        this.referencePath = referencePath;

        addRelationResolver(PrismConstants.T_OBJECT_REFERENCE,
                new RefTableTargetResolver<>(targetMappingSupplier));
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QReference<>(MReference.class, alias);
    }

    /** Defines a contract for creating the reference for the provided owner row. */
    public R newRowObject(OR ownerRow) {
        throw new UnsupportedOperationException(
                "Reference bean creation for owner row called on abstract reference mapping");
    }

    /**
     * Returns a bi-function that constructs correlation query predicate for owner and reference.
     */
    public BiFunction<OQ, Q, Predicate> correlationPredicate() {
        throw new UnsupportedOperationException(
                "correlationPredicate not supported on abstract reference mapping");
    }

    /**
     * There is no need to override this, only reference creation is different and that is covered
     * by {@link QReferenceMapping#newRowObject(Object)} including setting FK columns.
     * All the other columns are based on a single schema type, so there is no variation.
     */
    @Override
    public R insert(ObjectReferenceType reference, OR ownerRow, JdbcSession jdbcSession) {
        R row = newRowObject(ownerRow);
        // row.referenceType is DB generated, must be kept NULL, but it will match referenceType
        row.relationId = processCacheableRelation(reference.getRelation());
        row.targetOid = UUID.fromString(reference.getOid());
        row.targetType = schemaTypeToObjectType(reference.getType());

        insert(row, jdbcSession);
        return row;
    }
}
