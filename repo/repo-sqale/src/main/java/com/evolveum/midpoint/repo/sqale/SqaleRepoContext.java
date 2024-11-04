/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType;

import com.querydsl.sql.types.ArrayType;
import com.querydsl.sql.types.EnumAsObjectType;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.QuerydslJsonbType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MGlobalMetadata;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QGlobalMetadata;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReferenceType;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * SQL repository context adding support for QName cache.
 */
public class SqaleRepoContext extends SqlRepoContext {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleRepoContext.class);

    private final String schemaChangeNumberLabel;

    private final int schemaChangeNumberValue;

    private final UriCache uriCache;
    private final ExtItemCache extItemCache;

    private FullTextSearchConfigurationType fullTextSearchConfig;

    public SqaleRepoContext(
            JdbcRepositoryConfiguration jdbcRepositoryConfiguration,
            DataSource dataSource,
            SchemaService schemaService,
            QueryModelMappingRegistry mappingRegistry, String schemaChangeNumberLabel, int schemaChangeNumberValue) {
        super(jdbcRepositoryConfiguration, dataSource, schemaService, mappingRegistry);

        this.schemaChangeNumberLabel = schemaChangeNumberLabel;
        this.schemaChangeNumberValue = schemaChangeNumberValue;

        // each enum type must be registered if we want to map it as objects (to PG enum types)
        querydslConfig.register(new EnumAsObjectType<>(AccessCertificationCampaignStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(ActivationStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(AdministrativeAvailabilityStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(AuditEventStageType.class));
        querydslConfig.register(new EnumAsObjectType<>(AuditEventTypeType.class));
        querydslConfig.register(new EnumAsObjectType<>(EffectivePrivilegesModificationType.class));
        querydslConfig.register(new EnumAsObjectType<>(AvailabilityStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(ChangeType.class)); // used in old audit
        querydslConfig.register(new EnumAsObjectType<>(ChangeTypeType.class));
        querydslConfig.register(new EnumAsObjectType<>(CorrelationSituationType.class));
        querydslConfig.register(new EnumAsObjectType<>(LockoutStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(MContainerType.class));
        querydslConfig.register(new EnumAsObjectType<>(MExtItemHolderType.class));
        querydslConfig.register(new EnumAsObjectType<>(MExtItemCardinality.class));
        querydslConfig.register(new EnumAsObjectType<>(MObjectType.class));
        querydslConfig.register(new EnumAsObjectType<>(MReferenceType.class));
        querydslConfig.register(new EnumAsObjectType<>(NodeOperationalStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(ObjectProcessingStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(OperationExecutionRecordTypeType.class));
        querydslConfig.register(new EnumAsObjectType<>(OperationResultStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(ResourceAdministrativeStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(ShadowKindType.class));
        querydslConfig.register(new EnumAsObjectType<>(SynchronizationSituationType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskAutoScalingModeType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskBindingType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskExecutionStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskRecurrenceType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskSchedulingStateType.class));
        querydslConfig.register(new EnumAsObjectType<>(TaskWaitingReasonType.class));
        querydslConfig.register(new EnumAsObjectType<>(ThreadStopActionType.class));
        querydslConfig.register(new EnumAsObjectType<>(TimeIntervalStatusType.class));
        querydslConfig.register(new EnumAsObjectType<>(ExecutionModeType.class));
        querydslConfig.register(new EnumAsObjectType<>(PredefinedConfigurationType.class));


        // JSONB type support
        querydslConfig.register(new QuerydslJsonbType());
        querydslConfig.register(new ArrayType<>(
                Array.newInstance(Jsonb.class, 0).getClass(), "jsonb"));

        uriCache = new UriCache();
        extItemCache = new ExtItemCache();
    }

    @PostConstruct
    public void initialize() {
        // skip version check if option was defined or option value is "true" (equals ignore case)
        String skipVersionCheck = System.getProperty(MidpointConfiguration.MIDPOINT_SKIP_VERSION_CHECK + "1");
        if (BooleanUtils.isNotTrue(Boolean.parseBoolean(skipVersionCheck))) {
            checkDBSchemaVersion();
        }

        clearCaches();
    }

    private void checkDBSchemaVersion(){
        LOGGER.debug("Checking DB schema version.");

        try (JdbcSession session = this.newJdbcSession().startReadOnlyTransaction()) {
            MGlobalMetadata metadata = session.newQuery().from(QGlobalMetadata.DEFAULT)
                    .select(QGlobalMetadata.DEFAULT)
                    .where(QGlobalMetadata.DEFAULT.name.eq(schemaChangeNumberLabel))
                    .limit(1)
                    .fetchOne();
            String current = metadata != null ? metadata.value : null;
            Integer currentAsInt = current != null ? Integer.valueOf(current) : null;

            if (!Objects.equals(currentAsInt, schemaChangeNumberValue)) {
                throw new SystemException("Can't initialize sqale repository context, database schema version (" + current
                        + ") doesn't match expected value (" + schemaChangeNumberValue + ") for label '" + schemaChangeNumberLabel
                        + "'. Seems like mismatch between midPoint executable version and DB schema version. Maybe DB schema was not updated?");
            }

            LOGGER.debug("DB schema version check OK.");
        }
    }

    // This has nothing to do with "repo cache" which is higher than this.
    public void clearCaches() {
        uriCache.initialize(this::newJdbcSession);
        extItemCache.initialize(this::newJdbcSession);
    }

    /**
     * Supports search for URI ID by QName or String or any other type using `toString`.
     */
    public @NotNull Integer searchCachedUriId(@NotNull Object uri) {
        if (uri instanceof QName) {
            return uriCache.searchId(uri);
        } else {
            return uriCache.searchId(uri.toString());
        }
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
    public Integer processCacheableUri(Object uri) {
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

    public String resolveIdToUri(Integer uriId) {
        return uriId != null
                ? uriCache.resolveToUri(uriId)
                : null;
    }

    public QName resolveUriIdToQName(Integer uriId) {
        return uriId != null
                ? QNameUtil.uriToQName(uriCache.resolveToUri(uriId))
                : null;
    }

    public @NotNull MExtItem resolveExtensionItem(@NotNull MExtItem.Key extItemKey) {
        return extItemCache.resolveExtensionItem(extItemKey);
    }

    public @Nullable MExtItem getExtensionItem(Integer id) {
        return extItemCache.getExtensionItem(id);
    }

    public @Nullable MExtItem getExtensionItem(MExtItem.Key extItemKey) {
        return extItemCache.getExtensionItem(extItemKey);
    }

    public void setFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearchConfig) {
        this.fullTextSearchConfig = fullTextSearchConfig;
    }

    /**
     * Returns string with words for full-text index, or null if there is nothing to index.
     * This also checks whether the configuration is enabled.
     */
    public String fullTextIndex(ObjectType object) {
        if (FullTextSearchUtil.isEnabled(fullTextSearchConfig)) {
            Set<String> words = FullTextSearchUtil.createWords(fullTextSearchConfig, object);
            if (words != null) {
                // The first/last space allows searching for start/ends with word or whole words.
                // TODO: Currently, this is not supported on the query side because space separates
                //  AND components and no other character was discussed yet.
                return ' ' + String.join(" ", words) + ' ';
            }
        }
        return null;
    }

    public <S extends ObjectType> boolean requiresFullTextReindex(
            Collection<? extends ItemDelta<?, ?>> modifications, PrismObject<S> prismObject) {
        return FullTextSearchUtil.isObjectTextInfoRecomputationNeeded(
                fullTextSearchConfig, prismObject.getCompileTimeClass(), modifications);
    }

    public byte[] createFullResult(OperationResultType operationResult) {
        try {
            // Note that escaping invalid characters and using toString for unsupported types
            // is safe in the context of operation result serialization.
            return createStringSerializer()
                    .options(SerializationOptions.createEscapeInvalidCharacters()
                            .serializeUnsupportedTypesAsString(true)
                            .skipWhitespaces(true))
                    .serializeRealValue(operationResult, SchemaConstantsGenerated.C_OPERATION_RESULT)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception", e);
        }
    }

    public Collection<ExtensionProcessor.ExtItemInfo> findConflictingExtensionItem(ExtensionProcessor.ExtItemInfo extItemInfo) {
        return extItemCache.findConflictingExtensions(extItemInfo.item).stream().map(v -> {
            var ret = new ExtensionProcessor.ExtItemInfo();
            ret.item = v;
            return ret;
        }).toList();
    }

    @Override
    protected ParsingContext createParsingContext() {
        return super.createParsingContext()
                .enableLazyDeserializationFor(ValueMetadataType.COMPLEX_TYPE);
    }
}
