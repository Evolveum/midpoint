/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.other.QObjectTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.other.QObjectTemplateMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.MResource;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRoleMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetypeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadowMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMarkMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all sub-tables, see various static `get*()` methods below.
 * Mapping instances are initialized (`init*()` methods) in {@link QObjectMapping} subclasses.
 * Both `init*` and `get*` methods are flexibly parametrized to adapt to the client code.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 *
 * @param <OS> owner schema type
 * @param <OQ> query type of the reference owner
 * @param <OR> row type of the reference owner (related to {@link OQ})
 */
public class QObjectReferenceMapping<OS extends ObjectType, OQ extends QObject<OR>, OR extends MObject>
        extends QReferenceMapping<QObjectReference<OR>, MReference, OQ, OR> {

    public static QObjectReferenceMapping<?, ?, ?> instanceArchetype;
    public static QObjectReferenceMapping<?, ?, ?> instanceDelegated;
    public static QObjectReferenceMapping<?, QObjectTemplate, MObject> instanceInclude;
    public static QObjectReferenceMapping<?, ?, ?> instanceProjection;
    // word "object" not used, as it is already implied in the class name
    public static QObjectReferenceMapping<?, ?, ?> instanceCreateApprover;
    public static QObjectReferenceMapping<?, ?, ?> instanceModifyApprover;
    public static QObjectReferenceMapping<?, ?, ?> instanceParentOrg;
    public static QObjectReferenceMapping<?, ?, ?> instancePersona;
    public static QObjectReferenceMapping<?, QResource, MResource>
            instanceResourceBusinessConfigurationApprover;
    public static QObjectReferenceMapping<?, ?, ?> instanceRoleMembership;
    public static QObjectReferenceMapping<?, ?, ?> instanceEffectiveMark;

    private final Supplier<QueryTableMapping<OS, OQ, OR>> ownerMappingSupplier;

    // region static init/get methods
    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> initForArchetype(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceArchetype, repositoryContext)) {
            instanceArchetype = new QObjectReferenceMapping<>(
                    "m_ref_archetype", "refa", repositoryContext,
                    QArchetypeMapping::getArchetypeMapping);
        }
        return getForArchetype();
    }

    public static <OQ extends QObject<OR>, OR extends MObject>
    QObjectReferenceMapping<?, OQ, OR> getForArchetype() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, OQ, OR>) Objects.requireNonNull(instanceArchetype);
    }

    public static <OQ extends QObject<OR>, OR extends MObject>
    QObjectReferenceMapping<?, OQ, OR> initForDelegated(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceDelegated, repositoryContext)) {
            instanceDelegated = new QObjectReferenceMapping<>(
                    "m_ref_delegated", "refd", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForDelegated();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForDelegated() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instanceDelegated);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<?, Q, R>
    initForEffectiveMark(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceEffectiveMark, repositoryContext)) {
            instanceEffectiveMark = new QObjectReferenceMapping<>(
                    "m_ref_object_effective_mark", "refem", repositoryContext,
                    QMarkMapping::getInstance);
        }
        return getForEffectiveMark();
    }

    @SuppressWarnings("unchecked")
    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForEffectiveMark() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instanceEffectiveMark);
    }

    public static QObjectReferenceMapping<?, QObjectTemplate, MObject> initForInclude(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceInclude, repositoryContext)) {
            instanceInclude = new QObjectReferenceMapping<>(
                    "m_ref_include", "refi", repositoryContext,
                    QObjectTemplateMapping::getObjectTemplateMapping);
        }
        return instanceInclude;
    }

    public static QObjectReferenceMapping<?, QObjectTemplate, MObject> getForInclude() {
        return Objects.requireNonNull(instanceInclude);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<?, Q, R>
    initForProjection(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceProjection, repositoryContext)) {
            instanceProjection = new QObjectReferenceFullObjectMapping<>(
                    FocusType.class,
                    FocusType.F_LINK_REF,
                    "m_ref_projection", "refpj", repositoryContext,
                    QShadowMapping::getShadowMapping,
                    QFocusMapping::getFocusMapping,
                    (q, oq) -> q.ownerOid.eq(oq.oid),
                    FocusType.class,
                    FocusType.F_LINK_REF);
        }
        return getForProjection();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForProjection() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instanceProjection);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<?, Q, R>
    initForCreateApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceCreateApprover, repositoryContext)) {
            instanceCreateApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_create_approver", "refca", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForCreateApprover();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForCreateApprover() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instanceCreateApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<?, Q, R>
    initForModifyApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceModifyApprover, repositoryContext)) {
            instanceModifyApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_modify_approver", "refma", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForModifyApprover();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForModifyApprover() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instanceModifyApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<?, Q, R>
    initForParentOrg(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceParentOrg, repositoryContext)) {
            instanceParentOrg = new QObjectReferenceMapping<>(
                    "m_ref_object_parent_org", "refpo", repositoryContext,
                    QOrgMapping::getOrgMapping);
        }
        return getForParentOrg();
    }

    public static <OQ extends QObject<OR>, OR extends MObject>
    QObjectReferenceMapping<?, OQ, OR> getForParentOrg() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, OQ, OR>) Objects.requireNonNull(instanceParentOrg);
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> initForPersona(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instancePersona, repositoryContext)) {
            instancePersona = new QObjectReferenceMapping<>(
                    "m_ref_persona", "refp", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForPersona();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<?, Q, R> getForPersona() {
        //noinspection unchecked
        return (QObjectReferenceMapping<?, Q, R>) Objects.requireNonNull(instancePersona);
    }

    public static QObjectReferenceMapping<?, QResource, MResource>
    initForResourceBusinessConfigurationApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceResourceBusinessConfigurationApprover, repositoryContext)) {
            instanceResourceBusinessConfigurationApprover = new QObjectReferenceMapping<>(
                    "m_ref_resource_business_configuration_approver", "refrbca", repositoryContext,
                    QObjectMapping::getObjectMapping);
        }
        return instanceResourceBusinessConfigurationApprover;
    }

    public static QObjectReferenceMapping<?, QResource, MResource>
    getForResourceBusinessConfigurationApprover() {
        return Objects.requireNonNull(instanceResourceBusinessConfigurationApprover);
    }

    public static <OS extends ObjectType, OQ extends QObject<OR>, OR extends MObject> QObjectReferenceMapping<OS, OQ, OR>
    initForRoleMembership(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceRoleMembership, repositoryContext)) {
            instanceRoleMembership = new QObjectReferenceFullObjectMapping<>(
                    AssignmentHolderType.class,
                    AssignmentHolderType.F_ROLE_MEMBERSHIP_REF,
                    "m_ref_role_membership", "refrm", repositoryContext,
                    QAbstractRoleMapping::getAbstractRoleMapping,
                    QAssignmentHolderMapping::getAssignmentHolderMapping,
                    (q, oq) -> q.ownerOid.eq(oq.oid),
                    AssignmentHolderType.class,
                    AssignmentHolderType.F_ROLE_MEMBERSHIP_REF);
        }
        return getForRoleMembership();
    }



    public static <OS extends ObjectType, OQ extends QObject<OR>, OR extends MObject>
    QObjectReferenceMapping<OS, OQ, OR> getForRoleMembership() {
        //noinspection unchecked
        return (QObjectReferenceMapping<OS, OQ, OR>) Objects.requireNonNull(instanceRoleMembership);
    }
    // endregion

    private <TQ extends QObject<TR>, TR extends MObject> QObjectReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        this(tableName, defaultAliasName,
                repositoryContext, targetMappingSupplier,
                null, null, null, null);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <TQ extends QObject<TR>, TR extends MObject> QObjectReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier,
            @Nullable Supplier<QueryTableMapping<OS, OQ, OR>> ownerMappingSupplier,
            @Nullable BiFunction<QObjectReference<OR>, OQ, Predicate> ownerJoin,
            @Nullable Class<?> ownerType,
            @Nullable ItemPath referencePath) {
        super(tableName, defaultAliasName, (Class) QObjectReference.class,
                repositoryContext, targetMappingSupplier,
                ownerMappingSupplier, ownerJoin,
                ownerType, referencePath);

        this.ownerMappingSupplier = ownerMappingSupplier;
    }

    protected  <TQ extends QObject<TR>, TR extends MObject> QObjectReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            Class<? extends QObjectReference<?>> referenceType,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier,
            @Nullable Supplier<QueryTableMapping<OS, OQ, OR>> ownerMappingSupplier,
            @Nullable BiFunction<QObjectReference<OR>, OQ, Predicate> ownerJoin,
            @Nullable Class<?> ownerType,
            @Nullable ItemPath referencePath) {
        super(tableName, defaultAliasName, (Class) referenceType,
                repositoryContext, targetMappingSupplier,
                ownerMappingSupplier, ownerJoin,
                ownerType, referencePath);

        this.ownerMappingSupplier = ownerMappingSupplier;
    }

    @Override
    protected QObjectReference<OR> newAliasInstance(String alias) {
        return new QObjectReference<>(alias, tableName());
    }

    @Override
    public MReference newRowObject(MObject ownerRow) {
        MReference row = new MReference();
        row.ownerOid = ownerRow.oid;
        row.ownerType = ownerRow.objectType;
        return row;
    }

    @Override
    public BiFunction<OQ, QObjectReference<OR>, Predicate> correlationPredicate() {
        return (o, r) -> o.oid.eq(r.ownerOid);
    }

    /**
     * References are extracted from their owner objects inside {@link ResultListRowTransformer#beforeTransformation}.
     */
    @Override
    public ResultListRowTransformer<ObjectReferenceType, QObjectReference<OR>, MReference> createRowTransformer(
            SqlQueryContext<ObjectReferenceType, QObjectReference<OR>, MReference> sqlQueryContext, JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {
        // owner OID -> (target OID -> values)
        Map<UUID, Map<String, List<ObjectReferenceType>>> refsByOwnerAndTarget = new HashMap<>();
        Map<UUID, ObjectType> owners = new HashMap<>();
        return new ResultListRowTransformer<>() {
            @Override
            public void beforeTransformation(List<Tuple> rowTuples, QObjectReference<OR> entityPath) throws SchemaException {
                Set<UUID> ownerOids = rowTuples.stream()
                        .map(row -> Objects.requireNonNull(row.get(entityPath)).ownerOid)
                        .collect(Collectors.toSet());

                if (!ownerOids.isEmpty()) {
                    // TODO do we need get options here as well? Is there a scenario where we load container
                    //  and define what to load for referenced/owner object?
                    QueryTableMapping<OS, OQ, OR> mapping = Objects.requireNonNull(ownerMappingSupplier).get();
                    OQ o = mapping.defaultAlias();
                    List<Tuple> result = jdbcSession.newQuery()
                            .select(o.oid, o.fullObject)
                            .from(o)
                            .where(o.oid.in(ownerOids))
                            .fetch();
                    for (Tuple row : result) {
                        UUID oid = Objects.requireNonNull(row.get(o.oid));
                        OS owner = parseSchemaObject(row.get(o.fullObject), oid.toString(), mapping.schemaType());
                        owners.put(oid, owner);

                        PrismReference reference = owner.asPrismObject().findReference(referencePath);
                        refsByOwnerAndTarget.put(oid, reference.getRealValues().stream()
                                .map(r -> (ObjectReferenceType) r)
                                .collect(Collectors.groupingBy(r -> r.getOid(), Collectors.toList())));
                    }
                }
            }

            @Override
            public ObjectReferenceType transform(Tuple rowTuple, QObjectReference<OR> entityPath) {
                MReference row = Objects.requireNonNull(rowTuple.get(entityPath));
                Map<String, List<ObjectReferenceType>> refsByTarget = refsByOwnerAndTarget.get(row.ownerOid);
                List<ObjectReferenceType> candidates = refsByTarget != null ? refsByTarget.get(row.targetOid.toString()) : null;
                if (candidates == null || candidates.isEmpty()) {
                    // Row was stored separatelly.
                    try {
                        var candidate = toSchemaObject(row);
                        var owner = owners.get(row.ownerOid);
                        applyToOwner(owner, candidate);
                        return candidate;
                    } catch (SchemaException e) {
                        throw new IllegalStateException(e);
                    }
                }

                if (candidates.size() == 1) {
                    return candidates.get(0);
                }
                for (ObjectReferenceType candidate : candidates) {
                    if (QNameUtil.match(candidate.getType(), objectTypeToQName(row.targetType))
                            && QNameUtil.match(candidate.getRelation(), resolveUriIdToQName(row.relationId))) {
                        return candidate;
                    }
                }
                throw new IllegalStateException("Reference candidate not found for reference row "
                        + row + "! This should not have happened.");
            }
        };
    }

    protected void applyToOwner(ObjectType owner, ObjectReferenceType candidate) throws SchemaException {
        //
    }

    protected boolean requiresParent(Tuple t, QObjectReference<OR> entityPath) {
        return true;
    }

    @Override
    public void afterModify(SqaleUpdateContext<ObjectReferenceType, QObjectReference<OR>, MReference> updateContext) throws SchemaException {
        super.afterModify(updateContext);

    }
}
