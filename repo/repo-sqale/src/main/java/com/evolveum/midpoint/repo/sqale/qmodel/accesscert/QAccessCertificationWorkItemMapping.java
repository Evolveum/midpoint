/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;
import static com.querydsl.core.types.dsl.Expressions.stringTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.ReferenceNameResolver;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mapping between {@link QAccessCertificationWorkItem} and {@link AccessCertificationWorkItemType}.
 */
public class QAccessCertificationWorkItemMapping
        extends QContainerMapping<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem, MAccessCertificationCase> {

    public static final String DEFAULT_ALIAS_NAME = "acwi";

    private static QAccessCertificationWorkItemMapping instance;

    public static QAccessCertificationWorkItemMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QAccessCertificationWorkItemMapping(repositoryContext);
        }
        return get();
    }

    public static QAccessCertificationWorkItemMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAccessCertificationWorkItemMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAccessCertificationWorkItem.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationWorkItemType.class, QAccessCertificationWorkItem.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QAccessCertificationCaseMapping::getAccessCertificationCaseMapping,
                        (q, p) -> q.ownerOid.eq(p.ownerOid)
                                .and(q.accessCertCaseCid.eq(p.cid))));

        addItemMapping(F_CLOSE_TIMESTAMP, timestampMapper(q -> q.closeTimestamp));
        // TODO: iteration -> campaignIteration
        addItemMapping(F_ITERATION, integerMapper(q -> q.campaignIteration));
        addNestedMapping(F_OUTPUT, AbstractWorkItemOutputType.class)
                .addItemMapping(AbstractWorkItemOutputType.F_OUTCOME, stringMapper(q -> q.outcome));
        addItemMapping(F_OUTPUT_CHANGE_TIMESTAMP, timestampMapper(q -> q.outputChangeTimestamp));
        addRefMapping(F_PERFORMER_REF,
                q -> q.performerRefTargetOid,
                q -> q.performerRefTargetType,
                q -> q.performerRefRelationId,
                QUserMapping::getUserMapping);

        addRefMapping(F_ASSIGNEE_REF,
                QAccessCertificationWorkItemReferenceMapping
                        .initForCaseWorkItemAssignee(repositoryContext));
        addRefMapping(F_CANDIDATE_REF,
                QAccessCertificationWorkItemReferenceMapping
                        .initForCaseWorkItemCandidate(repositoryContext));

        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));

    }

    @Override
    public AccessCertificationWorkItemType toSchemaObject(MAccessCertificationWorkItem row) {
        AccessCertificationWorkItemType acwi = new AccessCertificationWorkItemType()
                .id(row.cid)
                .closeTimestamp(asXMLGregorianCalendar(row.closeTimestamp))
                .iteration(row.campaignIteration)
                .outputChangeTimestamp(asXMLGregorianCalendar(row.outputChangeTimestamp))
                .performerRef(objectReference(row.performerRefTargetOid,
                        row.performerRefTargetType, row.performerRefRelationId))
                .stageNumber(row.stageNumber);

        if (row.outcome != null) {
            acwi.output(new AbstractWorkItemOutputType().outcome(row.outcome));
        }

        return acwi;
    }

    @Override
    protected QAccessCertificationWorkItem newAliasInstance(String alias) {
        return new QAccessCertificationWorkItem(alias);
    }

    @Override
    public MAccessCertificationWorkItem newRowObject() {
        return new MAccessCertificationWorkItem();
    }

    @Override
    public MAccessCertificationWorkItem newRowObject(MAccessCertificationCase ownerRow) {
        MAccessCertificationWorkItem row = newRowObject();
        row.ownerOid = ownerRow.ownerOid;
        row.accessCertCaseCid = ownerRow.cid;
        return row;
    }

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @Override
    @SuppressWarnings("DuplicatedCode")
    public MAccessCertificationWorkItem insert(
            AccessCertificationWorkItemType workItem,
            MAccessCertificationCase caseRow,
            JdbcSession jdbcSession) {
        MAccessCertificationWorkItem row = initRowObject(workItem, caseRow);

        row.closeTimestamp = MiscUtil.asInstant(workItem.getCloseTimestamp());
        // TODO: iteration -> campaignIteration
        row.campaignIteration = workItem.getIteration();

        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output != null) {
            row.outcome = output.getOutcome();
        }

        row.outputChangeTimestamp = MiscUtil.asInstant(workItem.getOutputChangeTimestamp());

        setReference(workItem.getPerformerRef(),
                o -> row.performerRefTargetOid = o,
                t -> row.performerRefTargetType = t,
                r -> row.performerRefRelationId = r);

        row.stageNumber = workItem.getStageNumber();

        insert(row, jdbcSession);

        storeRefs(row, workItem.getAssigneeRef(),
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemAssignee(), jdbcSession);
        storeRefs(row, workItem.getCandidateRef(),
                QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemCandidate(), jdbcSession);

        return row;
    }

    @Override
    public ResultListRowTransformer<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem> createRowTransformer(
            SqlQueryContext<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem> sqlQueryContext,
            JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options) {
        return new WorkItemRowTransformer(sqlQueryContext, jdbcSession, options);
    }

    /**
     * Row transformer for AccessCertificationWorkItem that batch-loads Campaign and Case data
     * in beforeTransformation to avoid N+1 query problem.
     *
     * <p>Uses "clone-based isolation" strategy where each WorkItem is returned with its own
     * isolated Campaign > Case > WorkItem hierarchy. This is required for Model layer's
     * security processing and GUI column access patterns.
     *
     * @see #transform(Tuple, QAccessCertificationWorkItem) for why clone is required
     */
    private class WorkItemRowTransformer implements
            ResultListRowTransformer<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem> {

        private final SqlQueryContext<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem> sqlQueryContext;
        private final JdbcSession jdbcSession;

        // Per-batch caches (cleared in beforeTransformation)
        private Map<UUID, PrismObject<AccessCertificationCampaignType>> campaignCache = new HashMap<>();
        private Map<String, AccessCertificationCaseType> caseCache = new HashMap<>(); // key: "ownerOid_caseCid"
        private Map<String, List<ObjectReferenceType>> assigneeRefCache = new HashMap<>(); // key: "ownerOid_workItemCid"
        private Map<String, List<ObjectReferenceType>> candidateRefCache = new HashMap<>();

        // Clone optimization: reuse isolated Campaign/Case for consecutive WorkItems from same Case
        private String lastCaseKey = null;
        private PrismObject<AccessCertificationCampaignType> lastIsolatedCampaign = null;
        private AccessCertificationCaseType lastIsolatedCase = null;

        // Cross-batch cache for resolved reference names
        private final ReferenceNameResolver sharedResolver;

        WorkItemRowTransformer(
                SqlQueryContext<AccessCertificationWorkItemType, QAccessCertificationWorkItem, MAccessCertificationWorkItem> sqlQueryContext,
                JdbcSession jdbcSession,
                Collection<SelectorOptions<GetOperationOptions>> options) {
            this.sqlQueryContext = sqlQueryContext;
            this.jdbcSession = jdbcSession;
            this.sharedResolver = ReferenceNameResolver.from(options);
        }

        @Override
        public void beforeTransformation(List<Tuple> tuples, QAccessCertificationWorkItem entityPath) throws SchemaException {
            // Clear per-batch caches (but preserve sharedResolver across batches)
            campaignCache.clear();
            caseCache.clear();
            assigneeRefCache.clear();
            candidateRefCache.clear();
            lastCaseKey = null;
            lastIsolatedCampaign = null;
            lastIsolatedCase = null;

            // Collect unique Campaign OIDs, Case CIDs, WorkItem CIDs, and WorkItem keys from this batch
            Set<UUID> campaignOids = new HashSet<>();
            Map<UUID, Set<Long>> ownerToCaseCids = new HashMap<>();
            Set<Long> workItemCids = new HashSet<>();
            Set<String> workItemKeys = new HashSet<>();

            for (Tuple tuple : tuples) {
                MAccessCertificationWorkItem row = Objects.requireNonNull(tuple.get(entityPath));
                campaignOids.add(row.ownerOid);
                ownerToCaseCids.computeIfAbsent(row.ownerOid, k -> new HashSet<>()).add(row.accessCertCaseCid);
                workItemCids.add(row.cid);
                workItemKeys.add(row.ownerOid + "_" + row.cid);
            }

            // Batch load Campaigns
            loadCampaignsBatch(campaignOids);

            // Batch load Cases with filtered WorkItems from fullObject
            // This provides full WorkItem attributes (output/comment, deadline, etc.) while filtering out
            // unnecessary WorkItems (those not in this batch)
            loadCasesBatchWithFilteredWorkItems(ownerToCaseCids, workItemCids);

            // Batch load WorkItem references (assigneeRef, candidateRef) from reference tables
            // (performerRef is in WorkItem row itself, no separate load needed)
            loadWorkItemRefsBatch(campaignOids, workItemCids, workItemKeys);

            // Batch resolve names and display names for all references
            // (sharedResolver caches across batches, so redundant OIDs are automatically skipped)
            Set<UUID> refOidsToLoad = new HashSet<>();

            // Collect OIDs from Case references (objectRef, targetRef, tenantRef, orgRef)
            for (AccessCertificationCaseType caseType : caseCache.values()) {
                collectRefOid(caseType.getObjectRef(), refOidsToLoad);
                collectRefOid(caseType.getTargetRef(), refOidsToLoad);
                collectRefOid(caseType.getTenantRef(), refOidsToLoad);
                collectRefOid(caseType.getOrgRef(), refOidsToLoad);
            }

            // Collect OIDs from WorkItem references (assigneeRef, candidateRef, performerRef)
            for (List<ObjectReferenceType> refs : assigneeRefCache.values()) {
                for (ObjectReferenceType ref : refs) {
                    collectRefOid(ref, refOidsToLoad);
                }
            }
            for (List<ObjectReferenceType> refs : candidateRefCache.values()) {
                for (ObjectReferenceType ref : refs) {
                    collectRefOid(ref, refOidsToLoad);
                }
            }
            // Collect performerRef OIDs from WorkItem rows
            for (Tuple tuple : tuples) {
                MAccessCertificationWorkItem row = tuple.get(entityPath);
                if (row != null && row.performerRefTargetOid != null) {
                    refOidsToLoad.add(row.performerRefTargetOid);
                }
            }

            // Batch resolve with display names
            sharedResolver.batchResolve(jdbcSession, refOidsToLoad, true);

            // Apply resolved names to Case references (will be inherited by clone)
            for (AccessCertificationCaseType caseType : caseCache.values()) {
                setDisplayNameOnRef(caseType.getObjectRef());
                setDisplayNameOnRef(caseType.getTargetRef());
                setDisplayNameOnRef(caseType.getTenantRef());
                setDisplayNameOnRef(caseType.getOrgRef());
            }

            // Apply resolved names to WorkItem references
            for (List<ObjectReferenceType> refs : assigneeRefCache.values()) {
                for (ObjectReferenceType ref : refs) {
                    setDisplayNameOnRef(ref);
                }
            }
            for (List<ObjectReferenceType> refs : candidateRefCache.values()) {
                for (ObjectReferenceType ref : refs) {
                    setDisplayNameOnRef(ref);
                }
            }
        }

        private void collectRefOid(ObjectReferenceType ref, Set<UUID> oids) {
            if (ref != null && ref.getOid() != null) {
                oids.add(UUID.fromString(ref.getOid()));
            }
        }

        /**
         * Set a lightweight object with display name on the reference for GUI column use.
         * Uses sharedResolver's cached name and display name.
         */
        private void setDisplayNameOnRef(ObjectReferenceType ref) {
            if (ref == null || ref.getOid() == null) {
                return;
            }
            UUID oid = UUID.fromString(ref.getOid());
            ReferenceNameResolver.ResolvedNames resolved = sharedResolver.getResolvedNames(oid);
            if (resolved == null) {
                return;
            }

            // Set targetName (name attribute) for ObjectReferenceColumn
            if (resolved.name() != null) {
                ref.setTargetName(PolyStringType.fromOrig(resolved.name()));
            }

            // Set object with name and displayName/fullName for GUI columns
            if (resolved.name() != null || resolved.displayName() != null) {
                ObjectType obj = createMinimalObjectForDisplay(ref.getType(), resolved.name(), resolved.displayName());
                if (obj != null) {
                    obj.setOid(ref.getOid());
                    ref.asReferenceValue().setObject(obj.asPrismObject());
                }
            }
        }

        /**
         * Create minimal ObjectType subclass with name and display name set.
         * Name is always set to prevent NPE. DisplayName/fullName is set based on type.
         */
        private ObjectType createMinimalObjectForDisplay(QName type, String name, String displayName) {
            if (type == null) {
                return null;
            }
            ObjectType obj;
            if (UserType.COMPLEX_TYPE.equals(type)) {
                UserType user = new UserType();
                if (displayName != null) {
                    user.setFullName(PolyStringType.fromOrig(displayName));
                }
                obj = user;
            } else if (RoleType.COMPLEX_TYPE.equals(type)
                    || ServiceType.COMPLEX_TYPE.equals(type)
                    || ArchetypeType.COMPLEX_TYPE.equals(type)
                    || OrgType.COMPLEX_TYPE.equals(type)) {
                // For AbstractRole types, set displayName
                RoleType role = new RoleType();
                if (displayName != null) {
                    role.setDisplayName(PolyStringType.fromOrig(displayName));
                }
                obj = role;
            } else {
                obj = new ObjectType() {};
            }
            // Always set name to prevent NPE when getName() is called
            if (name != null) {
                obj.setName(PolyStringType.fromOrig(name));
            }
            return obj;
        }

        private void loadCampaignsBatch(Set<UUID> campaignOids) {
            if (campaignOids.isEmpty()) {
                return;
            }
            QAccessCertificationCampaignMapping mapping =
                    QAccessCertificationCampaignMapping.getAccessCertificationCampaignMapping();
            QAccessCertificationCampaign qCampaign = mapping.defaultAlias();

            List<Tuple> rows = jdbcSession.newQuery()
                    .select(qCampaign.oid, qCampaign.fullObject)
                    .from(qCampaign)
                    .where(qCampaign.oid.in(campaignOids))
                    .fetch();

            for (Tuple row : rows) {
                UUID oid = row.get(qCampaign.oid);
                byte[] fullObject = row.get(qCampaign.fullObject);
                try {
                    AccessCertificationCampaignType campaign = mapping.parseSchemaObject(fullObject, oid.toString());
                    campaignCache.put(oid, campaign.asPrismObject());
                } catch (SchemaException e) {
                    throw new SystemException("Failed to parse campaign " + oid, e);
                }
            }
        }

        /**
         * Load Cases with filtered WorkItems from fullObject.
         * This method fetches Case fullObject and filters the embedded WorkItem array using
         * PostgreSQL JSONB functions to include only WorkItems in the current batch.
         * This provides full WorkItem attributes (output/comment, deadline, name, etc.) that
         * are only available in fullObject, while avoiding loading unnecessary WorkItems.
         *
         * <p>Note: Currently fullObject is stored as bytea, requiring convert_from(fullObject, 'UTF8')::jsonb
         * conversion in SQL. If fullObject column is changed to JSONB type in the future, this SQL
         * can be simplified and optimized by removing the convert_from/cast overhead:
         * <pre>
         * -- Current (bytea):  (convert_from(fullObject, 'UTF8')::jsonb)->'case'
         * -- Future (jsonb):   fullObject->'case'
         * </pre>
         */
        private void loadCasesBatchWithFilteredWorkItems(
                Map<UUID, Set<Long>> ownerToCaseCids,
                Set<Long> workItemCids) {
            if (ownerToCaseCids.isEmpty()) {
                return;
            }

            QAccessCertificationCaseMapping caseMapping =
                    QAccessCertificationCaseMapping.getAccessCertificationCaseMapping();
            QAccessCertificationCase qCase = caseMapping.defaultAlias();

            // Build WorkItem CID list as SQL literal for JSONB filtering
            // These are Long values so safe to embed directly (no SQL injection risk)
            String workItemCidsLiteral = workItemCids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            // SQL expression to filter WorkItems in the JSONB fullObject:
            // 1. Convert fullObject from bytea to text, then to jsonb, and extract the 'case' object
            // 2. Filter the 'workItem' array to keep only WorkItems with @id in our batch
            // 3. Reconstruct the JSON with filtered workItem array
            // Note: convert_from({0}, 'UTF8') converts bytea to text, then ::jsonb parses as JSON
            // Note: 'case' is a PostgreSQL reserved word, so we use ->'case' with single quotes
            //       which requires careful escaping in stringTemplate
            String sql = "jsonb_set(" +
                    "(convert_from({0}, 'UTF8')::jsonb)->'case', " +
                    "'{workItem}', " +
                    "(SELECT COALESCE(jsonb_agg(wi), '[]'::jsonb) " +
                    "FROM jsonb_array_elements((convert_from({0}, 'UTF8')::jsonb)->'case'->'workItem') as wi " +
                    "WHERE (wi->>'@id')::bigint IN (" + workItemCidsLiteral + "))" +
                    ")";
            var filteredCaseJson = stringTemplate(sql, qCase.fullObject);

            // Build query with precise (ownerOid, cid) filtering
            var query = jdbcSession.newQuery()
                    .from(qCase)
                    .select(qCase.ownerOid, qCase.cid, filteredCaseJson);

            if (ownerToCaseCids.size() == 1) {
                // Single campaign - simple query
                Map.Entry<UUID, Set<Long>> entry = ownerToCaseCids.entrySet().iterator().next();
                query.where(qCase.ownerOid.eq(entry.getKey()).and(qCase.cid.in(entry.getValue())));
            } else {
                // Multiple campaigns - build OR conditions
                var predicate = qCase.ownerOid.isNull(); // Start with false-like predicate
                for (Map.Entry<UUID, Set<Long>> entry : ownerToCaseCids.entrySet()) {
                    predicate = predicate.or(
                            qCase.ownerOid.eq(entry.getKey()).and(qCase.cid.in(entry.getValue())));
                }
                query.where(predicate);
            }

            List<Tuple> rows = query.fetch();

            for (Tuple row : rows) {
                UUID ownerOid = row.get(qCase.ownerOid);
                Long cid = row.get(qCase.cid);
                String filteredJson = row.get(2, String.class);
                String caseKey = ownerOid + "_" + cid;

                try {
                    // Parse the filtered Case JSON (includes WorkItems with all attributes)
                    AccessCertificationCaseType caseObj = parseFilteredCaseJson(filteredJson, ownerOid, cid);
                    caseCache.put(caseKey, caseObj);
                } catch (SchemaException e) {
                    throw new SystemException("Failed to parse filtered case " + caseKey, e);
                }
            }
        }

        /**
         * Parse filtered Case JSON back to AccessCertificationCaseType.
         * The JSON needs to be wrapped in {"case": ...} format for parseSchemaObject().
         */
        private AccessCertificationCaseType parseFilteredCaseJson(
                String filteredJson, UUID ownerOid, Long cid) throws SchemaException {
            // Wrap JSON in {"case": ...} format expected by parseSchemaObject
            String wrappedJson = "{\"case\":" + filteredJson + "}";
            byte[] bytes = wrappedJson.getBytes(StandardCharsets.UTF_8);

            QAccessCertificationCaseMapping caseMapping =
                    QAccessCertificationCaseMapping.getAccessCertificationCaseMapping();
            return caseMapping.parseSchemaObject(bytes, ownerOid + "," + cid);
        }

        /**
         * Batch load WorkItem references (assigneeRef, candidateRef) from reference tables.
         * performerRef is in WorkItem row itself, no separate load needed.
         */
        private void loadWorkItemRefsBatch(Set<UUID> ownerOids, Set<Long> workItemCids, Set<String> workItemKeys) {
            if (ownerOids.isEmpty() || workItemCids.isEmpty()) {
                return;
            }

            // Load assigneeRef - filter by both ownerOid AND workItemCid to avoid loading all refs
            QAccessCertificationWorkItemReference qAssignee =
                    QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemAssignee().defaultAlias();
            List<MAccessCertificationWorkItemReference> assigneeRows = jdbcSession.newQuery()
                    .from(qAssignee)
                    .select(qAssignee)
                    .where(qAssignee.ownerOid.in(ownerOids)
                            .and(qAssignee.accessCertWorkItemCid.in(workItemCids)))
                    .fetch();

            for (MAccessCertificationWorkItemReference ref : assigneeRows) {
                String key = ref.ownerOid + "_" + ref.accessCertWorkItemCid;
                if (workItemKeys.contains(key)) {
                    assigneeRefCache.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(objectReference(ref.targetOid, ref.targetType, ref.relationId));
                }
            }

            // Load candidateRef - filter by both ownerOid AND workItemCid to avoid loading all refs
            QAccessCertificationWorkItemReference qCandidate =
                    QAccessCertificationWorkItemReferenceMapping.getForCaseWorkItemCandidate().defaultAlias();
            List<MAccessCertificationWorkItemReference> candidateRows = jdbcSession.newQuery()
                    .from(qCandidate)
                    .select(qCandidate)
                    .where(qCandidate.ownerOid.in(ownerOids)
                            .and(qCandidate.accessCertWorkItemCid.in(workItemCids)))
                    .fetch();

            for (MAccessCertificationWorkItemReference ref : candidateRows) {
                String key = ref.ownerOid + "_" + ref.accessCertWorkItemCid;
                if (workItemKeys.contains(key)) {
                    candidateRefCache.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(objectReference(ref.targetOid, ref.targetType, ref.relationId));
                }
            }
        }

        /**
         * Transform a database row into AccessCertificationWorkItemType with proper Prism hierarchy.
         *
         * <p><b>Why Clone is Required:</b>
         * Each WorkItem must have an isolated Campaign > Case > WorkItem hierarchy because:
         * <ol>
         *   <li>Model layer's security processing operates on root objects (Campaign) and may
         *       prune child elements. Shared hierarchy would cause unintended side effects.</li>
         *   <li>GUI columns traverse Prism hierarchy to find parent Case, expecting isolated structure.</li>
         * </ol>
         *
         * <p><b>Alternative Considered:</b>
         * A shared hierarchy approach was tested but proved ~3.5x slower due to Model layer's
         * security processing overhead on large shared objects.
         */
        @Override
        public AccessCertificationWorkItemType transform(Tuple tuple, QAccessCertificationWorkItem entityPath) {
            MAccessCertificationWorkItem row = Objects.requireNonNull(tuple.get(entityPath));
            UUID ownerOid = row.ownerOid;
            String caseKey = ownerOid + "_" + row.accessCertCaseCid;

            // Reuse isolated Campaign/Case if same as last row (consecutive WorkItems from same Case)
            // This optimization skips Campaign/Case clone when WorkItems are ordered by Case
            AccessCertificationWorkItemType workItem;
            if (caseKey.equals(lastCaseKey)) {
                // Same Case - reuse isolated structures, just clone and add the WorkItem
                workItem = findAndCloneWorkItem(caseKey, row.cid);
            } else {
                // Different Case - create new isolated Campaign/Case structures
                lastCaseKey = caseKey;
                lastIsolatedCampaign = cloneCampaign(ownerOid);
                lastIsolatedCase = cloneCase(caseKey, lastIsolatedCampaign);
                workItem = findAndCloneWorkItem(caseKey, row.cid);
            }

            // Add WorkItem to isolated Case (WorkItem is cloned, so safe to add)
            // This establishes the Prism hierarchy: Campaign > Case > WorkItem
            try {
                lastIsolatedCase.asPrismContainerValue()
                        .findOrCreateContainer(AccessCertificationCaseType.F_WORK_ITEM)
                        .add(workItem.asPrismContainerValue());
            } catch (SchemaException e) {
                throw new SystemException("Failed to add work item to case", e);
            }

            // Set targetName on performerRef
            setDisplayNameOnRef(workItem.getPerformerRef());

            // Replace assigneeRef and candidateRef with resolved ones from reference cache
            // (fullObject contains references but without targetName/displayName resolved)
            String refKey = ownerOid + "_" + row.cid;
            List<ObjectReferenceType> assigneeRefs = assigneeRefCache.get(refKey);
            List<ObjectReferenceType> candidateRefs = candidateRefCache.get(refKey);

            workItem.getAssigneeRef().clear();
            workItem.getCandidateRef().clear();
            if (assigneeRefs != null) {
                workItem.getAssigneeRef().addAll(assigneeRefs);
            }
            if (candidateRefs != null) {
                workItem.getCandidateRef().addAll(candidateRefs);
            }

            attachContainerIdPath(workItem, tuple, entityPath);
            return workItem;
        }

        /** Clone Campaign from cache to create an isolated hierarchy for this WorkItem. */
        private PrismObject<AccessCertificationCampaignType> cloneCampaign(UUID ownerOid) {
            PrismObject<AccessCertificationCampaignType> cached = campaignCache.get(ownerOid);
            if (cached == null) {
                return null;
            }
            return cached.clone();
        }

        /** Clone Case from cache and attach it to the isolated Campaign. WorkItems are cleared. */
        private AccessCertificationCaseType cloneCase(String caseKey, PrismObject<AccessCertificationCampaignType> isolatedCampaign) {
            AccessCertificationCaseType cached = caseCache.get(caseKey);
            if (cached == null) {
                return null;
            }

            AccessCertificationCaseType cloned = cached.clone();
            cloned.getWorkItem().clear();

            if (isolatedCampaign != null) {
                try {
                    PrismContainer<AccessCertificationCaseType> caseContainer =
                            isolatedCampaign.findOrCreateContainer(AccessCertificationCampaignType.F_CASE);
                    caseContainer.add(cloned.asPrismContainerValue());
                } catch (SchemaException e) {
                    throw new SystemException("Failed to attach case to campaign: " + caseKey, e);
                }
            }
            return cloned;
        }

        /** Find and clone a specific WorkItem from the cached Case (which has full attributes from fullObject). */
        private AccessCertificationWorkItemType findAndCloneWorkItem(String caseKey, Long workItemCid) {
            AccessCertificationCaseType cached = caseCache.get(caseKey);
            if (cached == null) {
                throw new SystemException("Case not found in cache: " + caseKey);
            }
            for (AccessCertificationWorkItemType wi : cached.getWorkItem()) {
                if (wi.getId().equals(workItemCid)) {
                    return wi.clone();
                }
            }
            throw new SystemException("WorkItem not found in cached case: " + caseKey + ", workItemCid: " + workItemCid);
        }
    }

    @Override
    protected List<Object> containerIdPath(Tuple tuple, QAccessCertificationWorkItem e) {
        var full = tuple.get(e);
        if (full != null) {
            return List.of(full.ownerOid.toString(), full.accessCertCaseCid, full.cid);
        }

        return List.of(tuple.get(e.ownerOid).toString(), tuple.get(e.accessCertCaseCid), tuple.get(e.cid));
    }

    @Override
    public int containerDepth() {
        return 2;
    }
}
