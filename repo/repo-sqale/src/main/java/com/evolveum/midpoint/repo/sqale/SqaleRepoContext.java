/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import com.querydsl.sql.types.EnumAsObjectType;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.jsonb.QuerydslJsonbType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReferenceType;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * SQL repository context adding support for QName cache.
 */
public class SqaleRepoContext extends SqlRepoContext {

    private final UriCache uriCache;
    private final ExtItemCache extItemCache;

    public SqaleRepoContext(
            JdbcRepositoryConfiguration jdbcRepositoryConfiguration,
            DataSource dataSource,
            SchemaService schemaService,
            QueryModelMappingRegistry mappingRegistry) {
        super(jdbcRepositoryConfiguration, dataSource, schemaService, mappingRegistry);

        // each enum type must be registered if we want to map it as objects (to PG enum types)
        querydslConfig.register(new EnumAsObjectType<>(AccessCertificationCampaignStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(ActivationStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(AvailabilityStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(MContainerType.class));
        querydslConfig.register(new EnumAsObjectType<>(MExtItemHolderType.class));
        querydslConfig.register(new EnumAsObjectType<>(MExtItemCardinality.class));
        querydslConfig.register(new EnumAsObjectType<>(MObjectType.class));
        querydslConfig.register(new EnumAsObjectType<>(MReferenceType.class));
        querydslConfig.register(new EnumAsObjectType<>(LockoutStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(OperationExecutionRecordTypeType.class));
        querydslConfig.register(new EnumAsObjectType<>(OperationResultStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(OrientationType.class));
        querydslConfig.register(new EnumAsObjectType<>(ResourceAdministrativeStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(ShadowKindType.class));
        querydslConfig.register(new EnumAsObjectType<>(SynchronizationSituationType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskBindingType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskExecutionStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskRecurrenceType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskWaitingReasonType.class));
        querydslConfig.register(new EnumAsObjectType<>(ThreadStopActionType.class));
        querydslConfig.register(new EnumAsObjectType<>(TimeIntervalStatusType.class));

        // JSONB type support
        querydslConfig.register(new QuerydslJsonbType());

        uriCache = new UriCache();
        extItemCache = new ExtItemCache();
    }

    // This has nothing to do with "repo cache" which is higher than this.
    @PostConstruct
    public void clearCaches() {
        uriCache.initialize(this::newJdbcSession);
        extItemCache.initialize(this::newJdbcSession);
    }

    /** @see UriCache#searchId(String) */
    public Integer searchCachedUriId(String uri) {
        return uriCache.searchId(uri);
    }

    /**
     * Returns ID for relation QName or {@link UriCache#UNKNOWN_ID} without going to the database.
     * Relation is normalized before consulting {@link UriCache}.
     * Never returns null; returns default ID for configured default relation if provided with null.
     */
    public @NotNull Integer searchCachedRelationId(QName qName) {
        return searchCachedUriId(QNameUtil.qNameToUri(normalizeRelation(qName)));
    }

    /** Returns ID for URI creating new cache row in DB as needed. */
    public Integer processCacheableUri(String uri) {
        return uriCache.processCacheableUri(uri);
    }

    /**
     * Returns ID for relation QName creating new {@link QUri} row in DB as needed.
     * Relation is normalized before consulting the cache.
     * Never returns null, returns default ID for configured default relation.
     */
    public Integer processCacheableRelation(QName qName) {
        return processCacheableUri(
                QNameUtil.qNameToUri(normalizeRelation(qName)));
    }

    public @NotNull MExtItem resolveExtensionItem(@NotNull MExtItem.Key extItemKey) {
        return extItemCache.resolveExtensionItem(extItemKey);
    }
}
