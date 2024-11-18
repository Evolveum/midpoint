/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.*;

import java.util.*;
import java.util.function.BiFunction;

import com.evolveum.axiom.concepts.CheckedFunction;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistryState;
import com.evolveum.midpoint.repo.sqlbase.SqlBaseOperationTracker;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleMappingMixin;
import com.evolveum.midpoint.repo.sqale.qmodel.common.*;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.collect.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.RepositoryObjectDiagnosticData;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.RepositoryMappingException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.xml.namespace.QName;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 *
 * @param <S> schema type of the object
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QObjectMapping<S extends ObjectType, Q extends QObject<R>, R extends MObject>
        extends SqaleTableMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "o";

    private static QObjectMapping<?, ?, ?> instance;
    @Nullable
    private PathSet fullObjectSkips;

    private final SchemaRegistryState.DerivationKey<ItemDefinition<?>> derivationKey;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectMapping<?, ?, ?> initObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QObjectMapping<>(
                QObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectType.class, QObject.CLASS,
                repositoryContext);
        return instance;
    }

    private final Map<ItemName, FullObjectItemMapping<?,?>> separatellySerializedItems = new HashMap<>();

    // Explanation in class Javadoc for SqaleTableMapping
    public static QObjectMapping<?, ?, ?> getObjectMapping() {
        return Objects.requireNonNull(instance);
    }

    private boolean storeSplitted = true;


    protected QObjectMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);

        derivationKey = SchemaRegistryState.derivationKeyFrom(getClass(), "Definition");

        addItemMapping(PrismConstants.T_ID, uuidMapper(q -> q.oid));
        addItemMapping(F_NAME, polyStringMapper(
                q -> q.nameOrig, q -> q.nameNorm));
        addRefMapping(F_TENANT_REF,
                q -> q.tenantRefTargetOid,
                q -> q.tenantRefTargetType,
                q -> q.tenantRefRelationId,
                QOrgMapping::getOrgMapping);

        addRefMapping(F_EFFECTIVE_MARK_REF,
                QObjectReferenceMapping.initForEffectiveMark(repositoryContext));

        addItemMapping(F_LIFECYCLE_STATE, stringMapper(q -> q.lifecycleState));
        // version/cidSeq is not mapped for queries or deltas, it's managed by repo explicitly

        addItemMapping(F_POLICY_SITUATION, multiUriMapper(q -> q.policySituations));
        addItemMapping(F_SUBTYPE, multiStringMapper(q -> q.subtypes));
        // full-text is not item mapping, but filter on the whole object
        addExtensionMapping(F_EXTENSION, MExtItemHolderType.EXTENSION, q -> q.ext);

        addNestedMapping(F_METADATA, MetadataType.class)
                .addRefMapping(MetadataType.F_CREATOR_REF,
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addRefMapping(MetadataType.F_MODIFIER_REF,
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(MetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp))
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        QObjectReferenceMapping.initForCreateApprover(repositoryContext))
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        QObjectReferenceMapping.initForModifyApprover(repositoryContext));

        addRefMapping(F_PARENT_ORG_REF,
                QObjectReferenceMapping.initForParentOrg(repositoryContext));

        addContainerTableMapping(F_OPERATION_EXECUTION,
                QOperationExecutionMapping.init(repositoryContext),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid))); // TODO: separate fullObject fields
        addContainerTableMapping(F_TRIGGER,
                QTriggerMapping.init(repositoryContext),
                joinOn((o, trg) -> o.oid.eq(trg.ownerOid)));

        var valueMetadataMapping = addNestedMapping(InfraItemName.METADATA, ValueMetadataType.class);
        valueMetadataMapping.addNestedMapping(ValueMetadataType.F_STORAGE, StorageMetadataType.class)
                .addRefMapping(StorageMetadataType.F_CREATOR_REF,
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(StorageMetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(StorageMetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addRefMapping(StorageMetadataType.F_MODIFIER_REF,
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(StorageMetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(StorageMetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp));
        valueMetadataMapping.addNestedMapping(ValueMetadataType.F_PROCESS, ProcessMetadataType.class)
                .addRefMapping(ProcessMetadataType.F_CREATE_APPROVER_REF,
                        QObjectReferenceMapping.initForCreateApprover(repositoryContext))
                .addRefMapping(ProcessMetadataType.F_MODIFY_APPROVER_REF,
                        QObjectReferenceMapping.initForModifyApprover(repositoryContext));
    }

    @Override
    @MustBeInvokedByOverriders
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        var paths = new ArrayList<Path<?>>();
        paths.add(entity.oid);
        paths.add(entity.objectType);
        paths.add(entity.version);
        if (isExcludeFullObject(options)) {
            // We have options to exclude everything, so at least we should fetch name, since lot of code assumes name is present
            paths.add( entity.nameOrig);
            paths.add(entity.nameNorm);
        } else {
           paths.add(entity.fullObject);
        }
        // TODO: there is currently no support for index-only extensions (from entity.ext).
        //  See how QShadowMapping.loadIndexOnly() is used, and probably compose the result of this call
        //  using super... call in the subclasses. (joining arrays? providing mutable list?)
        return paths.toArray(new Path<?>[0]);
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QObject<>(MObject.class, alias);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MObject();
    }

    // region transformation
    @Override
    public S toSchemaObject(
            @NotNull Tuple row,
            @NotNull Q entityPath,
            @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {

        UUID oid = Objects.requireNonNull(row.get(entityPath.oid));
        var repoType = Objects.requireNonNull(row.get(entityPath.objectType));
        S ret;
        byte[] fullObject = row.get(entityPath.fullObject);
        if (fullObject == null) {
            // Full Object was excluded.
            // We have exclude (dont retrieve) options for whole object, so we just return oid
            //noinspection unchecked
            ret = (S) repoType.createObject()
                .oid(oid.toString())
                .name(new PolyStringType(new PolyString(row.get(entityPath.nameOrig), row.get(entityPath.nameNorm))));
            SqaleUtils.markWithoutFullObject(ret);
        } else {
            // We load full object

            ret = parseSchemaObject(fullObject, oid.toString());
            if (GetOperationOptions.isAttachDiagData(SelectorOptions.findRootOptions(options))) {
                RepositoryObjectDiagnosticData diagData = new RepositoryObjectDiagnosticData(fullObject.length);
                ret.asPrismContainer().setUserData(RepositoryService.KEY_DIAG_DATA, diagData);
            }
        }
        ret.version(Objects.requireNonNull(row.get(entityPath.version)).toString());
        upgradeLegacyMetadataToValueMetadata(ret);
        return ret;
    }

    protected boolean isExcludeAll(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null) {
            return false;
        }
        boolean rootExcluded = false;
        for (var option : options) {
            if (option.getOptions() != null) {
                if (option.isRoot() && option.getOptions().getRetrieve() == RetrieveOption.EXCLUDE) {
                    rootExcluded = true;
                }
                if (option.getOptions().getRetrieve() == RetrieveOption.INCLUDE) {
                    return false;
                }
            }
        }
        return rootExcluded;
    }

    protected boolean isExcludeFullObject(@Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null) {
            return false;
        }
        boolean rootExcluded = false;
        for (var option : options) {
            if (option.getOptions() != null) {
                if (option.isRoot() && option.getOptions().getRetrieve() == RetrieveOption.EXCLUDE) {
                    rootExcluded = true;
                }
                if (option.getOptions().getRetrieve() == RetrieveOption.INCLUDE) {
                    if (!separatellySerializedItems.containsKey(option.getItemPath().firstName())) {
                        return false;
                    }
                }
            }
        }
        return rootExcluded;
    }

    private void upgradeLegacyMetadataToValueMetadata(S ret) {
        var legacyMeta = ret.getMetadata();
        if (legacyMeta == null || !ret.asPrismContainerValue().getValueMetadata().isEmpty()) {
            return;
        }

        var converted = ValueMetadataTypeUtil.fromLegacy(legacyMeta);
        converted.setId(1L);

        try {
            ret.asPrismContainerValue().getValueMetadata().add(converted.asPrismContainerValue());
            ret.setMetadata(null);
            ret.asPrismObject().setUserData(SqaleUtils.REINDEX_NEEDED, true);

        } catch (SchemaException e) {
            e.getMessage(); // Should not happen.
        }

    }

    /**
     * The same function as in overridden method, but softer exception handling.
     * This targets cases like {@link RepositoryService#searchObjects} where single wrong object
     * should not spoil the whole result list.
     */
    @Override
    public S toSchemaObjectCompleteSafe(
            Tuple tuple,
            Q entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession,
            boolean forceFull) {
        var result = SqlBaseOperationTracker.parsePrimary();
        try {
            return toSchemaObjectComplete(tuple, entityPath, options, jdbcSession, forceFull);
        } catch (SchemaException e) {
            try {
                PrismObject<S> errorObject = prismContext().createObject(schemaType());
                //noinspection ConstantConditions - this must not be null, the column is not
                String oid = tuple.get(entityPath.oid).toString();
                errorObject.setOid(oid);
                errorObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                logger.warn("Unreadable object with OID {}, reason: {}\n"
                        + "Surrogate object with error message as a name will be used.", oid, e.toString());
                return errorObject.asObjectable();
            } catch (SchemaException ex) {
                throw new RepositoryMappingException("Schema exception [" + ex + "] while handling schema exception: " + e, e);
            }
        } finally {
            result.close();
        }
    }

    /**
     * Override this to fill additional row attributes after calling this super version.
     *
     * *This must be called with active JDBC session* so it can create new {@link QUri} rows.
     * As this is intended for inserts *DO NOT* set {@link MObject#objectType} to any value,
     * it must be NULL otherwise the DB will complain about the value for the generated column.
     *
     * OID may be null, hence the method does NOT create any sub-entities, see
     * {@link #storeRelatedEntities(MObject, ObjectType, JdbcSession)}.
     * Try to keep order of fields here, in M-class (MObject for this one) and in SQL the same.
     */
    @SuppressWarnings("DuplicatedCode") // see comment for metadata lower
    @NotNull
    public R toRowObjectWithoutFullObject(S schemaObject, JdbcSession jdbcSession) {
        R row = newRowObject();

        row.oid = SqaleUtils.oidToUuid(schemaObject.getOid());
        // objectType MUST be left NULL for INSERT, it's determined by PG
        setPolyString(schemaObject.getName(), o -> row.nameOrig = o, n -> row.nameNorm = n);
        // fullObject is managed outside this method
        setReference(schemaObject.getTenantRef(),
                o -> row.tenantRefTargetOid = o,
                t -> row.tenantRefTargetType = t,
                r -> row.tenantRefRelationId = r);
        row.lifecycleState = schemaObject.getLifecycleState();
        // containerIdSeq is managed outside this method
        row.version = SqaleUtils.objectVersionAsInt(schemaObject);

        // complex DB fields
        row.policySituations = processCacheableUris(schemaObject.getPolicySituation());
        row.subtypes = stringsToArray(schemaObject.getSubtype());
        row.fullTextInfo = repositoryContext().fullTextIndex(schemaObject);
        row.ext = processExtensions(schemaObject.getExtension(), MExtItemHolderType.EXTENSION);

        // This is duplicate code with QAssignmentMapping.insert, but making interface
        // and needed setters (fields are not "interface-able") would create much more code.
        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            legacyMetadataToRowObject(row, metadata);
        }
        // Value metadata are preferred
        var valueMetadataPcv = schemaObject.asPrismContainerValue().getValueMetadata().getAnyValue();
        if (valueMetadataPcv != null) {
            valueMetadataToRowObject(row, (ValueMetadataType) valueMetadataPcv.asContainerable());
        }

        return row;
    }

    private void legacyMetadataToRowObject(R row, MetadataType metadata) {
        setReference(metadata.getCreatorRef(),
                o -> row.creatorRefTargetOid = o,
                t -> row.creatorRefTargetType = t,
                r -> row.creatorRefRelationId = r);
        row.createChannelId = processCacheableUri(metadata.getCreateChannel());
        row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

        setReference(metadata.getModifierRef(),
                o -> row.modifierRefTargetOid = o,
                t -> row.modifierRefTargetType = t,
                r -> row.modifierRefRelationId = r);
        row.modifyChannelId = processCacheableUri(metadata.getModifyChannel());
        row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
    }

    private void valueMetadataToRowObject(R row, ValueMetadataType metadata) {
        var storage = metadata.getStorage();
        if (storage != null) {
            setReference(storage.getCreatorRef(),
                    o -> row.creatorRefTargetOid = o,
                    t -> row.creatorRefTargetType = t,
                    r -> row.creatorRefRelationId = r);
            row.createChannelId = processCacheableUri(storage.getCreateChannel());
            row.createTimestamp = MiscUtil.asInstant(storage.getCreateTimestamp());

            setReference(storage.getModifierRef(),
                    o -> row.modifierRefTargetOid = o,
                    t -> row.modifierRefTargetType = t,
                    r -> row.modifierRefRelationId = r);
            row.modifyChannelId = processCacheableUri(storage.getModifyChannel());
            row.modifyTimestamp = MiscUtil.asInstant(storage.getModifyTimestamp());
        }
    }

    /**
     * Stores other entities related to the main object row like containers, references, etc.
     * This is not part of {@link #toRowObjectWithoutFullObject} because it requires known OID
     * which is not assured before calling that method.
     *
     * *Always call this super method first in overriding methods.*
     *
     * @param row master row for the added object("aggregate root")
     * @param schemaObject schema objects for which the details are stored
     * @param jdbcSession JDBC session used to insert related rows
     */
    public void storeRelatedEntities(
            @NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        Objects.requireNonNull(row.oid);

        // We're after insert, we can set this for the needs of owned entities (assignments).
        row.objectType = MObjectType.fromSchemaType(schemaObject.getClass());

        MetadataType metadata = schemaObject.getMetadata();
        if (metadata != null) {
            storeRefs(row, metadata.getCreateApproverRef(),
                    QObjectReferenceMapping.getForCreateApprover(), jdbcSession);
            storeRefs(row, metadata.getModifyApproverRef(),
                    QObjectReferenceMapping.getForModifyApprover(), jdbcSession);
        }

        // complete effective marks
        storeRefs(row, getEffectiveMarks(schemaObject), QObjectReferenceMapping.getForEffectiveMark(), jdbcSession);

        List<TriggerType> triggers = schemaObject.getTrigger();
        if (!triggers.isEmpty()) {
            triggers.forEach(t -> QTriggerMapping.get().insert(t, row, jdbcSession));
        }

        List<OperationExecutionType> operationExecutions = schemaObject.getOperationExecution();
        if (!operationExecutions.isEmpty()) {
            for (var oe : operationExecutions) {
                QOperationExecutionMapping.get().insert(oe, row, jdbcSession);
            }
        }

        storeRefs(row, schemaObject.getParentOrgRef(),
                QObjectReferenceMapping.getForParentOrg(), jdbcSession);

        // FIXME: Store fullObjects here?
    }

    private @NotNull List<ObjectReferenceType> getEffectiveMarks(@NotNull S schemaObject) {
        // TODO: Should we also add marks from statementPolicy (include?) - that way they would be available
        // for search even without recompute.
        // Just adding them directly here, will break delta add / delete
        //        List<ObjectReferenceType> ret = new ArrayList<>();
        //
        //        for (PolicyStatementType policy : schemaObject.getPolicyStatement()) {
        //            if (PolicyStatementTypeType.APPLY.equals(policy.getType())) {
        //                // We ensure mark is in effective marks list indexed in repository
        //                var mark = policy.getMarkRef();
        //                if (mark != null) {
        //                    ret.add(mark);
        //                }
        //            }
        //        }
        return schemaObject.getEffectiveMarkRef();


    }

    /**
     * Serializes schema object and sets {@link R#fullObject}.
     */
    public void setFullObject(R row, S schemaObject) throws SchemaException {
        var version = schemaObject.getVersion();
        if (schemaObject.getOid() == null || version == null) {
            throw new IllegalArgumentException(
                    "Serialized object must have assigned OID and version: " + schemaObject);
        }

        // We do not want version to be stored inside object, otherwise we will need to
        // recompute full object on every change to separately serialized items
        // We do not create clone without version because it would create additional memory constraints
        schemaObject.version(null);
        try {
            row.fullObject = createFullObject(schemaObject);
        } finally {
            // The users of repository expects version to be present
            schemaObject.version(version);
        }
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(Collection<SelectorOptions<GetOperationOptions>> options, @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        var ret = new ArrayList<>(super.updateGetOptions(options, modifications));
        // reindex = true - we need to fetch all items


        // Walk deltas
        boolean onlySeparatellySerialized = true;
        for (var modification : modifications) {
            var path = modification.getPath().firstName();
            var separate = separatellySerializedItems.get(path);
            if (separate == null) {
                onlySeparatellySerialized = false;
            } else {
                ret.add(SelectorOptions.create(UniformItemPath.from(path), GetOperationOptions.createRetrieve()));
            }
        }
        if (onlySeparatellySerialized) {
            // Add Exclude All option
            ret.add(SelectorOptions.create(UniformItemPath.from(ItemPath.EMPTY_PATH), GetOperationOptions.createDontRetrieve()));
        }
        return ret;
    }

    @Override
    public <C extends Containerable, TQ extends QContainer<TR, R>, TR extends MContainer> SqaleMappingMixin<S, Q, R> addContainerTableMapping(
            @NotNull ItemName itemName, @NotNull QContainerMapping<C, TQ, TR, R> containerMapping, @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
        if (containerMapping instanceof QContainerWithFullObjectMapping mappingWithFullObject) {
            return addFullObjectContainerTableMapping(itemName, (QContainerWithFullObjectMapping) containerMapping, true, (BiFunction) joinPredicate);
        }
        return super.addContainerTableMapping(itemName, containerMapping, joinPredicate);
    }

    @Override
    public <TQ extends QReference<TR, R>, TR extends MReference> SqaleMappingMixin<S, Q, R> addRefMapping(@NotNull QName itemName, @NotNull QReferenceMapping<TQ, TR, Q, R> referenceMapping) {
        if (referenceMapping instanceof QSeparatelySerializedItem<?,?> casted) {
            separatellySerializedItems.put((ItemName) itemName, new FullObjectItemMapping(casted, true));
        }
        return super.addRefMapping(itemName, referenceMapping);
    }

    public <C extends Containerable, TQ extends QContainerWithFullObject<TR, R>, TR extends MContainerWithFullObject> SqaleMappingMixin<S, Q, R> addFullObjectContainerTableMapping(
            @NotNull ItemName itemName, @NotNull QContainerWithFullObjectMapping<C, TQ, TR, R> containerMapping, boolean includeByDefault, @NotNull BiFunction<Q, TQ, Predicate> joinPredicate) {
        separatellySerializedItems.put(itemName, new FullObjectItemMapping(containerMapping, includeByDefault));
        return super.addContainerTableMapping(itemName, containerMapping, joinPredicate);
    }

        @Override
    protected final PathSet fullObjectItemsToSkip() {
        if (fullObjectSkips == null) {
            var pathSet = new PathSet();
            if (storeSplitted) {
                for (var mapping : separatellySerializedItems.values()) {
                    pathSet.add(mapping.getPath());
                }
            }
            customizeFullObjectItemsToSkip(pathSet);
            pathSet.freeze();
            fullObjectSkips = pathSet;
        }
        return fullObjectSkips;
    }

    private class FullObjectItemMapping<IQ extends FlexibleRelationalPathBase<IR>, IR> {

        protected final QSeparatelySerializedItem<IQ,IR> mapping;
        protected final boolean includedByDefault;

        public FullObjectItemMapping(QSeparatelySerializedItem mapping, boolean includedByDefault) {
            this.mapping = mapping;
            this.includedByDefault = includedByDefault;
        }

        public ItemPath getPath() {
            return mapping.getItemPath();
        }

        public boolean isIncluded(Collection<SelectorOptions<GetOperationOptions>> options) {
            if (includedByDefault) {
                ItemPath matchedPath = ItemPath.EMPTY_PATH;
                RetrieveOption matchedOption = RetrieveOption.INCLUDE;
                if (options == null) {
                    options = Collections.emptyList();
                }
                for (var option : options) {
                    if (!option.getItemPath().isSubPathOrEquivalent(getPath())) {
                        // path is unrelevant
                        continue;
                    }
                    if (matchedPath.isSubPathOrEquivalent(option.getItemPath())) {
                        matchedPath = option.getItemPath();
                        if (option.getOptions().getRetrieve() != null) {
                            matchedOption = option.getOptions().getRetrieve();
                        }
                    }
                }
                return matchedOption == RetrieveOption.INCLUDE;
            }
            return SelectorOptions.hasToFetchPathNotRetrievedByDefault(getPath(), options);
        }

        public Multimap<UUID, Tuple> fetchChildren(Collection<UUID> oidList, JdbcSession jdbcSession, Set<UUID> toMigrate) throws SchemaException {
            Multimap<UUID, Tuple> ret = MultimapBuilder.hashKeys().arrayListValues().build();

            var q = mapping.createAlias();
            var query = jdbcSession.newQuery()
                    .from(q)
                    .select(mapping.fullObjectExpressions(q)) // no complications here, we load it whole
                    .where(mapping.allOwnedBy(q, oidList))
                    .orderBy(mapping.orderSpecifier(q));
            for (var row : query.fetch()) {
                // All assignments should have full object present / legacy assignments should be kept
                var owner =  mapping.getOwner(row,q);
                if (mapping.hasFullObject(row,q)) {
                    ret.put(owner, row);
                } else {
                    // Indexed value did not contained full object, we should mark it for reindex
                    toMigrate.add(owner);
                }
            }
            return ret;
        }

        public void applyToSchemaObject(S target, Collection<Tuple> values) throws SchemaException {
            if (values.isEmpty()) {
                // Do not create empty items
                return;
            }
            var container = target.asPrismObject().findOrCreateItem(getPath(), (Class) mapping.getPrismItemType());
            var alias = mapping.createAlias();


            if (container.isEmpty() || container.isIncomplete()) {
                // If container is not empty and or not incomplete - it contained data from previous versions (not splitted)
                // so we should not populate it with splitted

                try {
                    if (container instanceof PrismContainerImpl<?> impl) {
                        impl.startStrictModifications();
                    }
                    container.setIncomplete(false);
                    var parsedValues = values.parallelStream().map(v -> {
                        try {
                            return mapping.toSchemaObjectEmbedded(v, alias);
                        } catch (SchemaException e) {
                            throw new TunnelException(e);
                        }
                    }).toList();
                    for (var val : parsedValues) {
                        // FIXME: Some better addition method should be necessary.
                        // Check if value is present...
                        ((Item) container).addIgnoringEquivalents(val);
                    }
                } catch (TunnelException e) {
                    if (e.getCause() instanceof SchemaException schemaEx) {
                        throw schemaEx;
                    } else if (e.getCause() instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw e;
                } finally {
                    if (container instanceof PrismContainerImpl<?> impl) {
                        impl.stopStrictModifications();
                    }
                }
            }
        }

    }

    protected void customizeFullObjectItemsToSkip(PathSet mutableSet) {
        // NOOP for overrides
    }

    @Override
    public ResultListRowTransformer<S, Q, R> createRowTransformer(SqlQueryContext<S, Q, R> sqlQueryContext, JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {

        if (isExcludeAll(options)) {
            // If exlude options is all, use default row transformer
            return super.createRowTransformer(sqlQueryContext, jdbcSession, options);
        }

        // here we should load external objects

        Map<MObjectType, Set<FullObjectItemMapping>> itemsToFetch = new HashMap<>();
        Multimap<FullObjectItemMapping, UUID> oidsToFetch = HashMultimap.create();

        // Set of objects, which should be reindexed (they are stored without full objects in nested tables)
        Set<UUID> objectsToReindex = new HashSet<>();

        Map<FullObjectItemMapping, Multimap<UUID, PrismValue>> mappingToData = new HashMap<>();

        return new ResultListRowTransformer<S, Q, R>() {

            @Override
            public void beforeTransformation(List<Tuple> tuples, Q entityPath) throws SchemaException {
                for (var tuple : tuples) {
                    var objectType = tuple.get(entityPath.objectType);
                    var fetchItems = itemsToFetch.get(objectType);

                    // If we did not resolved list of items to already fetch based on object type, we resolve it now.
                    if (fetchItems == null) {
                        var objMapping = (QObjectMapping) sqlQueryContext.repositoryContext().getMappingByQueryType((Class) objectType.getQueryType());

                        if (objMapping.storeSplitted) {
                            fetchItems = new HashSet<>();
                            for (var rawMapping : objMapping.separatellySerializedItems.values()) {
                                @SuppressWarnings("unchecked")
                                var mapping = (FullObjectItemMapping) rawMapping;
                                if (mapping.isIncluded(options)) {
                                    mappingToData.put(mapping, ImmutableMultimap.of());
                                    fetchItems.add(mapping);
                                }
                            }
                        } else {
                            fetchItems = Collections.emptySet();
                        }
                    }

                    // For each item to fetch we maintain seperate entry in map
                    for (var item : fetchItems) {
                        oidsToFetch.put(item, tuple.get(entityPath.oid));
                    }
                }

                for (var mapping : mappingToData.entrySet()) {
                    var result = SqlBaseOperationTracker.fetchChildren(mapping.getKey().mapping.tableName());
                    try {
                        mapping.setValue(mapping.getKey().fetchChildren(oidsToFetch.get(mapping.getKey()), jdbcSession, objectsToReindex));
                    } finally {
                        result.close();
                    }
                }
            }

            @Override
            public S transform(Tuple tuple, Q entityPath) {
                // Parsing full object
                S baseObject = toSchemaObjectCompleteSafe(tuple, entityPath, options, jdbcSession, false);
                var uuid = tuple.get(entityPath.oid);
                if (!storeSplitted) {
                    return baseObject;
                }
                if (objectsToReindex.contains(uuid)) {
                    // Object is in legacy form (splitted items does not have full object, reindex is recommended
                    // This mark is checked during update from original state read by repository
                    // which forces reindex as part of udpate
                    baseObject.asPrismObject().setUserData(SqaleUtils.REINDEX_NEEDED, true);
                }
                var childrenResult = SqlBaseOperationTracker.parseChildren("all");
                try {
                    for (var entry : mappingToData.entrySet()) {
                        var mapping = entry.getKey();
                        try {
                            mapping.applyToSchemaObject(baseObject, entry.getValue().get(uuid));
                        } catch (SchemaException e) {
                            throw new SystemException(e);
                        }
                    }
                } finally {
                    childrenResult.close();
                }
                resolveReferenceNames(baseObject, jdbcSession, options);
                return baseObject;
            }
        };
    }

    @VisibleForTesting
    public int additionalSelectsByDefault() {
        if (storeSplitted) {
            return (int) separatellySerializedItems.entrySet().stream().filter(e -> e.getValue().includedByDefault).count();
        }
        return 0;
    }

    @VisibleForTesting
    public void setStoreSplitted(boolean storeSplitted) {
        this.storeSplitted = storeSplitted;
        fullObjectSkips = null; // Needs to be recomputed
    }

    /**
     * If mapping supports force reindex
     *
     * @return True if reindex is supported for specified objects.
     */
    public boolean isReindexSupported() {
        return true;
    }
    // endregion

    @Override
    protected SchemaRegistryState.DerivationKey<ItemDefinition<?>> definitionDerivationKey() {
        return derivationKey;
    }

    private CheckedFunction<SchemaRegistryState, ItemDefinition<?>, SystemException> definitionDerivation = (registry) ->
        registry.findObjectDefinitionByCompileTimeClass(schemaType());

    @Override
    protected CheckedFunction<SchemaRegistryState, ItemDefinition<?>, SystemException> definitionDerivation() {
        return definitionDerivation;
    }
}
