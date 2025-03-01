/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers;

import static java.util.Collections.singletonList;

import java.util.*;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * This class and its subclasses provides org. closure table handling.
 * <p>
 * Data structures used are:
 * <p>
 * (1) Repo object graph G = (V, E) where V is a set of vertices (repo objects) and E is a set of edges (parentRef relations).
 * There is an edge e = (V1, V2) in E [i.e. edge from V1 to V2] if and only if V1.parentRef contains V2 [i.e. V2 is a parent of V1].
 * <p>
 * (2) OrgClosure table. OrgClosure(D, A, N) iff there are exactly N paths in object graph from D (descendant) to A (ancestor).
 * It is transitive reflexive closure, i.e. OrgClosure(V, V, 1) items are there as well.
 * <p>
 * Algorithms taken from "SQL Design Patterns" book by Vadim Tropashko (http://vadimtropashko.wordpress.com/)
 * namely from Chapter 6 (http://vadimtropashko.files.wordpress.com/2014/01/book_sql_chap6_v1.pdf).
 * SQL queries were then optimized by hand for various database engines.
 *
 * @author lazyman
 */
@Component
@DependsOn("schemaChecker")
public class OrgClosureManager {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureManager.class);

    @Autowired
    @Qualifier("repositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private BaseHelper baseHelper;

    private static final boolean DUMP_TABLES = false;
    private static final boolean COUNT_CLOSURE_RECORDS = false;
    private static final String CLOSURE_TABLE_NAME = "m_org_closure";
    private static final String TEMP_DELTA_TABLE_NAME_FOR_ORACLE = "m_org_closure_temp_delta";

    // only for single-thread performance testing
    private long lastOperationDuration;

    //region Public interface

    /**
     * Main method called from SQL repository service to update the closure table during an operation.
     *
     * @param originalObject Original state of the object - before applying modification in the repository.
     * It is used only in case of MODIFY (note that "overwriting ADD" is present here as MODIFY!)
     * @param modifications Collection of modifications to be applied to the object.
     * @param em Database entity manager to use.
     * @param oid OID of the object.
     * @param type Type of the object.
     * @param operation Operation that is carried out.
     */
    public <T extends ObjectType> void updateOrgClosure(PrismObject<? extends ObjectType> originalObject,
            Collection<? extends ItemDelta> modifications, EntityManager em, String oid, Class<T> type, Operation operation,
            Context closureContext) {
        if (!isEnabled() || !OrgType.class.isAssignableFrom(type)) {
            return;
        }
        em.flush();
        em.clear();

        long time = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("################# Starting {} for org. closure for {} oid={}.", operation, type.getSimpleName(), oid);
        }

        List<ReferenceDelta> deltas = filterParentRefDeltas(modifications);

        switch (operation) {
            case ADD:
                handleAdd(oid, deltas, closureContext, em);
                break;
            case DELETE:
                handleDelete(oid, closureContext, em);
                break;
            case MODIFY:
                handleModify(oid, deltas, originalObject, closureContext, em);
        }

        long duration = System.currentTimeMillis() - time;
        LOGGER.debug("################# Org. closure update finished in {} ms.", duration);
        lastOperationDuration = duration;
    }

    /*
     *  Note: the interface for onBeginTransactionXYZ is a bit different from the one used for updateOrgClosure.
     *  For example, "overwriting ADD" is indicated as onBeginTransactionAdd(..., overwrite:=true) but as
     *  updateOrgClosure(..., operation:=MODIFY). Also, onBeginTransactionXYZ gets raw data, while
     *  updateOrgClosure gets data after some pre-processing (e.g. exctraction of parentOrgRef reference values).
     *
     *  This will be perhaps unified in the future.
     */
    public <T extends ObjectType> Context onBeginTransactionAdd(EntityManager em, PrismObject<T> object, boolean overwrite) {
        if (!isEnabled() || !(OrgType.class.isAssignableFrom(object.getCompileTimeClass()))) {
            return null;
        }
        // we have to be ready for closure-related operation even if there are no known parents (because there may be orphans pointing to this org!)
        return onBeginTransaction(em);
    }

    public <T extends ObjectType> Context onBeginTransactionModify(EntityManager em, Class<T> type, String oid, Collection<? extends ItemDelta> modifications) {
        if (!isEnabled()) {
            return null;
        }
        if (!(OrgType.class.isAssignableFrom(type))) {
            return null;
        }
        if (filterParentRefDeltas(modifications).isEmpty()) {
            return null;
        }
        return onBeginTransaction(em);
    }

    public <T extends ObjectType> Context onBeginTransactionDelete(EntityManager em, Class<T> type, String oid) {
        if (!isEnabled() || !(OrgType.class.isAssignableFrom(type))) {
            return null;
        }
        return onBeginTransaction(em);
    }

    private Context onBeginTransaction(EntityManager em) {
        // table locking
        if (isOracle() || isSQLServer()) {
            lockClosureTable(em);
        }
        // other
        return new Context();
    }

    public void cleanUpAfterOperation(Context closureContext, EntityManager em) {
        if (closureContext == null) {
            return;
        }
        if (closureContext.temporaryTableName == null) {
            return;
        }
    }

    @PostConstruct
    public void initialize() {
        OperationResult result = new OperationResult(OrgClosureManager.class.getName() + ".initialize");
        if (!isEnabled()) {
            return;
        }

        SqlRepositoryConfiguration repoConfiguration = baseHelper.getConfiguration();

        if (isOracle()) {
            initializeOracleTemporaryTable();
        }

        boolean check, rebuild;
        switch (repoConfiguration.getOrgClosureStartupAction()) {
            case NONE:
                return;
            case CHECK:
                check = true;
                rebuild = false;
                break;
            case REBUILD_IF_NEEDED:
                check = true;
                rebuild = true;
                break;
            case ALWAYS_REBUILD:
                check = false;
                rebuild = true;
                break;
            default:
                throw new IllegalArgumentException("Invalid value: " + repoConfiguration.getOrgClosureStartupAction());
        }
        checkAndOrRebuild(check, rebuild, repoConfiguration.isStopOnOrgClosureStartupFailure(), true, result);
    }

    public boolean isEnabled() {
        return !baseHelper.getConfiguration().isIgnoreOrgClosure();
    }

    /**
     * Does a consistency check (either quick or thorough one) and rebuilds the closure table if necessary.
     * <p>
     * Quick check is conducted by comparing whether are there any orgs without entries in closure
     * (the reverse direction is ensured via foreign keys in m_org_closure).
     * <p>
     * Thorough check is conducted by recomputing the closure table.
     */
    public void checkAndOrRebuild(boolean check, boolean rebuild, boolean stopOnFailure, boolean quickCheckOnly, OperationResult result) {
        LOGGER.debug("Org closure check/rebuild request: check={}, rebuild={}", check ? (quickCheckOnly ? "quick" : "thorough") : "none", rebuild);
        if (!isEnabled()) {
            result.recordWarning("Organizational closure processing is disabled.");
            return;
        }
        if (!check && !rebuild) {
            result.recordWarning("Neither 'check' nor 'rebuild' option was requested.");
            return;         // nothing to do here
        }
        EntityManager em = baseHelper.getEntityManagerFactory().createEntityManager();
        Context context = null;
        boolean rebuilt = false;
        try {
            em.getTransaction().begin();
            if (rebuild || (check && !quickCheckOnly)) {
                // thorough check requires the temporary table as well
                context = onBeginTransaction(em);
            }

            if (quickCheckOnly) {
                boolean ok = false;
                if (check) {
                    int problems = quickCheck(em);
                    if (problems != 0) {
                        LOGGER.warn("Content of M_ORG_CLOSURE table is not consistent with the content of M_ORG one. Missing OIDs: {}", problems);
                        if (!rebuild && stopOnFailure) {
                            throw new IllegalStateException("Content of M_ORG_CLOSURE table is not consistent with the content of M_ORG one. Missing OIDs: " + problems);
                        }
                    } else {
                        LOGGER.debug("Org closure quick test passed.");
                        ok = true;
                    }
                }
                if (!ok && rebuild) {
                    rebuild(false, true, stopOnFailure, context, em, result);
                    rebuilt = true;
                }
            } else {
                // if the check has to be thorough
                rebuild(check, rebuild, stopOnFailure, context, em, result);
                rebuilt = rebuild;          // if we are here this means the CL was rebuilt if it was to be rebuilt
                if (stopOnFailure && result.isError()) {
                    throw new IllegalStateException(result.getMessage());
                }
            }

            if (rebuilt) {
                em.getTransaction().commit();
                LOGGER.info("Recomputed org closure table was successfully committed into database.");
            } else {
                // if !rebuilt, we either didn't do any modifications (in quick check mode)
                // or we did, but we want them to disappear (although this wish is a bit strange...)
                em.getTransaction().rollback();
            }
        } catch (SchemaException | RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Exception during check and/or recomputation of closure table", e);
            em.getTransaction().rollback();
            if (stopOnFailure) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new IllegalStateException("Unexpected exception during check and/or recomputation of closure table: " + e.getMessage(), e);
                }
            }
        } finally {
            cleanUpAfterOperation(context, em);
            em.close();
        }
    }

    //endregion

    //region Rebuilding or checking org closure

    // we are already in the context of a transaction (and the org struct table is locked if possible)
    // "check" here means "thorough check" (i.e. comparing with recomputed closure)
    private void rebuild(boolean check, boolean rebuild, boolean stopOnFailure, final Context context, final EntityManager em, OperationResult result) throws SchemaException {

        List existingEntries = null;
        if (check) {
            LOGGER.info("Reading from existing org closure table");
            Query selectQuery = em.createNativeQuery(
                    "SELECT descendant_oid, ancestor_oid, val from " + CLOSURE_TABLE_NAME, "OrgClosureBasic");
            existingEntries = selectQuery.getResultList();
            LOGGER.info("{} entries read", existingEntries.size());
        }

        LOGGER.info("Computing org closure table from scratch");

        Query deleteQuery = em.createNativeQuery("delete from " + CLOSURE_TABLE_NAME);
        deleteQuery.executeUpdate();
        LOGGER.trace("Closure table content deleted");

        final int orgsTotal = repositoryService.countObjects(OrgType.class, null, null, result);
        final MutableInt orgsProcessed = new MutableInt(0);

        ResultHandler<OrgType> handler = (object, parentResult) -> {

            LOGGER.trace("Processing {}", object);
            handleAdd(object.getOid(), getParentOidsFromObject(object), context, em);
            orgsProcessed.add(1);
            int currentState = orgsProcessed.intValue();
            if (currentState % 100 == 0) {
                LOGGER.info("{} organizations processed (out of {})", currentState, orgsTotal);
            }
            return true;
        };
        repositoryService.searchObjectsIterative(OrgType.class, null, handler, null, true, result);

        LOGGER.info("Org closure table was successfully recomputed (not committed yet); all {} organizations processed", orgsTotal);

        if (check) {
            LOGGER.info("Reading from recomputed org closure table");
            Query selectQuery = em.createNativeQuery(
                    "SELECT descendant_oid, ancestor_oid, val from " + CLOSURE_TABLE_NAME, "OrgClosureBasic");
            List recomputedEntries = selectQuery.getResultList();
            LOGGER.info("{} entries read", recomputedEntries.size());
            compareOrgClosureTables(existingEntries, recomputedEntries, rebuild, result);
        } else {
            result.recordSuccess();
        }
    }

    private void compareOrgClosureTables(List existingEntries, List recomputedEntries, boolean rebuild, OperationResult result) {
        Set<List> existing = convertEntries(existingEntries);
        Set<List> recomputed = convertEntries(recomputedEntries);
        if (!existing.equals(recomputed)) {
            String addendum;
            OperationResultStatus status;
            if (rebuild) {
                status = OperationResultStatus.HANDLED_ERROR;
                addendum = " The table has been recomputed and now it is OK.";
            } else {
                status = OperationResultStatus.FATAL_ERROR;
                addendum = " Please recompute the table as soon as possible.";
            }
            String m = "Closure table is not consistent with the repository. Expected size: " + recomputed.size() + " actual size: " + existing.size() + "." + addendum;
            result.recordStatus(status, m);
            LOGGER.info(m);
        } else {
            String m = "Closure table is OK (" + existing.size() + " entries)";
            result.recordStatus(OperationResultStatus.SUCCESS, m);
            LOGGER.info(m);
        }
    }

    private Set<List> convertEntries(List<Object[]> entries) {
        Set<List> rv = new HashSet<>();
        for (Object[] entry : entries) {
            rv.add(Arrays.asList(entry));
        }
        return rv;
    }

    private int quickCheck(EntityManager em) {
        Query q = em.createNativeQuery(
                "select count(m_org.oid) as problems from m_org left join m_org_closure cl " +
                        "on cl.descendant_oid = m_org.oid and cl.ancestor_oid = m_org.oid " +
                        "where cl.descendant_oid is null", "OrgClosureQuickCheck");
        List problemsList = q.getResultList();
        if (problemsList == null || problemsList.size() != 1) {
            throw new IllegalStateException("Unexpected return value from the closure check query: " + problemsList + " (a 1-item list of Integer expected)");
        }
        return (int) problemsList.get(0);
    }
    //endregion

    //region Handling ADD operation

    // we can safely expect that the object didn't exist before (because the "overwriting add" is sent to us as MODIFY operation)
    private void handleAdd(String oid, List<ReferenceDelta> deltas, Context context, EntityManager em) {
        handleAdd(oid, getParentOidsToAdd(deltas, null), context, em);
    }

    // parents may be non-existent at this point
    private void handleAdd(String oid, Set<String> parents, Context context, EntityManager em) {
        // adding self-record
        em.persist(new ROrgClosure(oid, oid, 1));
        em.flush();
        em.clear();

        List<String> livingChildren = getChildren(oid, em);        // no need to check existence of these oids, as owner is a FK pointing to RObject in RParentRef
        LOGGER.trace("Living children = {}", livingChildren);
        addChildrenEdges(oid, livingChildren, context, em);

        // all parents are "new", so we should just select which do really exist at this moment
        Collection<String> livingParents = retainExistingOids(parents, em);
        LOGGER.trace("Living parents = {} (parents = {})", livingParents, parents);

        if (livingParents.size() <= 1 && (livingChildren == null || livingChildren.isEmpty())) {
            String parent;
            if (livingParents.isEmpty()) {
                parent = null;
            } else {
                parent = livingParents.iterator().next();
            }
            addEdgeSimple(oid, parent, em);
        } else {
            addParentEdges(oid, livingParents, context, em);
        }
    }

    // we expect that all livingChildren do exist and
    // that none of the links child->oid does exist yet
    private void addChildrenEdges(String oid, List<String> livingChildren, Context context, EntityManager em) {
        List<Edge> edges = childrenToEdges(oid, livingChildren);
        addIndependentEdges(edges, context, em);
    }

    private List<Edge> childrenToEdges(String oid, List<String> livingChildren) {
        List<Edge> edges = new ArrayList<>();
        for (String child : livingChildren) {
            edges.add(new Edge(child, oid));
        }
        return edges;
    }

    // we expect that all livingParents do exist and
    // that none of the links oid->parent does exist yet
    private void addParentEdges(String oid, Collection<String> livingParents, Context context, EntityManager em) {
        List<Edge> edges = parentsToEdges(oid, livingParents);
        addIndependentEdges(edges, context, em);
    }

    private List<Edge> parentsToEdges(String oid, Collection<String> parents) {
        List<Edge> edges = new ArrayList<>();
        for (String parent : parents) {
            edges.add(new Edge(oid, parent));
        }
        return edges;
    }

    // we expect that the link oid->parent does not exist yet and the parent exists
    private void addEdgeSimple(String oid, String parent, EntityManager em) {
        if (parent != null) {
            long start = System.currentTimeMillis();
            Query addToClosureQuery = em.createNativeQuery(
                    "insert into " + CLOSURE_TABLE_NAME + " (descendant_oid, ancestor_oid, val) " +
                            "select :oid as descendant_oid, CL.ancestor_oid as ancestor_oid, CL.val as val " +
                            "from " + CLOSURE_TABLE_NAME + " CL " +
                            "where CL.descendant_oid = :parent");
            addToClosureQuery.setParameter("oid", oid);
            addToClosureQuery.setParameter("parent", parent);
            int count = addToClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("addEdges simplified: Added {} records to closure table ({} ms).", count,
                        System.currentTimeMillis() - start);
            }
        }
        em.flush();
        em.clear();
    }

    // IMPORTANT PRECONDITIONS:
    //  - all edges are "real", i.e. their tails and heads (as objects) do exist in repository
    //  - none of the edges does exist yet
    private void addIndependentEdges(List<Edge> edges, Context context, EntityManager em) {
        long start = System.currentTimeMillis();
        LOGGER.trace("===================== ADD INDEPENDENT EDGES: {} ================", edges);

        if (!edges.isEmpty()) {
            addIndependentEdgesInternal(edges, context, em);
        }

        em.flush();
        em.clear();

        LOGGER.trace("--------------------- DONE ADD EDGES: {} ({} ms) ----------------", edges, System.currentTimeMillis() - start);
    }

    private void addIndependentEdgesInternal(List<Edge> edges, Context context, EntityManager em) {

        checkForCycles(edges, em);
        String deltaTempTableName = computeDeltaTable(edges, context, em);
        try {
            int count;

            if (isOracle() || isSQLServer()) {

                long startUpsert = System.currentTimeMillis();
                String upsertQueryText;

                if (isSQLServer()) {
                    // TODO try if this one (without prefixes in INSERT clause does not work for Oracle)
                    upsertQueryText = "merge into " + CLOSURE_TABLE_NAME + " closure " +
                            "using (select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ") delta " +
                            "on (closure.descendant_oid = delta.descendant_oid and closure.ancestor_oid = delta.ancestor_oid) " +
                            "when matched then update set closure.val = closure.val + delta.val " +
                            "when not matched then insert (descendant_oid, ancestor_oid, val) " +
                            "values (delta.descendant_oid, delta.ancestor_oid, delta.val);";
                } else { // Oracle
                    upsertQueryText = "merge into " + CLOSURE_TABLE_NAME + " closure " +
                            "using (select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ") delta " +
                            "on (closure.descendant_oid = delta.descendant_oid and closure.ancestor_oid = delta.ancestor_oid) " +
                            "when matched then update set closure.val = closure.val + delta.val " +
                            "when not matched then insert (closure.descendant_oid, closure.ancestor_oid, closure.val) " +
                            "values (delta.descendant_oid, delta.ancestor_oid, delta.val)";
                }
                Query upsertQuery = em.createNativeQuery(upsertQueryText);
                int countUpsert = upsertQuery.executeUpdate();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Added/updated {} records to closure table ({} ms)", countUpsert, System.currentTimeMillis() - startUpsert);
                }
                if (DUMP_TABLES) {
                    dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
                }

            } else {    // separate update and insert

                long startUpdate = System.currentTimeMillis();
                String updateInClosureQueryText;
                if (isPostgreSQL()) {
                    updateInClosureQueryText = "update " + CLOSURE_TABLE_NAME + " " +
                            "set val = val + (select val from " + deltaTempTableName + " td " +
                            "where td.descendant_oid=" + CLOSURE_TABLE_NAME + ".descendant_oid and td.ancestor_oid=" + CLOSURE_TABLE_NAME + ".ancestor_oid) " +
                            "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from " + deltaTempTableName + ")";
                } else {
                    throw new UnsupportedOperationException("Org. closure manager - unsupported database operation");
                }
                Query updateInClosureQuery = em.createNativeQuery(updateInClosureQueryText);
                int countUpdate = updateInClosureQuery.executeUpdate();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Updated {} records to closure table ({} ms)", countUpdate, System.currentTimeMillis() - startUpdate);
                }

                if (DUMP_TABLES) {
                    dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
                }

                long startAdd = System.currentTimeMillis();
                String addQuery =
                        "insert into " + CLOSURE_TABLE_NAME + " (descendant_oid, ancestor_oid, val) " +
                                "select descendant_oid, ancestor_oid, val from " + deltaTempTableName + " delta ";
                if (countUpdate > 0) {
                    if (isPostgreSQL()) {
                        addQuery += " where not exists (select 1 from " + CLOSURE_TABLE_NAME + " cl where cl.descendant_oid=delta.descendant_oid and cl.ancestor_oid=delta.ancestor_oid)";
                    } else {
                        throw new UnsupportedOperationException("Org. closure manager - unsupported database operation");
                    }
                }
                Query addToClosureQuery = em.createNativeQuery(addQuery);
                count = addToClosureQuery.executeUpdate();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Added {} records to closure table ({} ms)", count, System.currentTimeMillis() - startAdd);
                }

                if (DUMP_TABLES) {
                    dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
                }
            }
        } finally {
            dropDeltaTableIfNecessary(em, deltaTempTableName);
        }

    }

    // Checks that there is no edge=(D,A) such that A->D exists in the transitive closure
    // (this would yield a cycle D->A->D in the graph)
    private void checkForCycles(List<Edge> edges, EntityManager em) {
        String queryText = "select descendant_oid, ancestor_oid from " + CLOSURE_TABLE_NAME + " where " + getWhereClauseForCycleCheck(edges);
        Query query = em.createNativeQuery(queryText, "OrgClosureCheckCycles");
        long start = System.currentTimeMillis();
        List list = query.getResultList();
        LOGGER.trace("Cycles checked in {} ms, {} conflicts found", System.currentTimeMillis() - start, list.size());
        if (!list.isEmpty()) {
            throw new IllegalArgumentException("Modification couldn't be executed, because a cycle in org structure graph would be created. Cycle-creating edges being added: " + formatList(list));
        }
    }

    private String formatList(List list) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Object o : list) {
            Object[] pair = (Object[]) o;
            String descendant = (String) pair[0];
            String ancestor = (String) pair[1];
            if (count++ > 0) {
                sb.append(", ");
            }
            sb.append(ancestor).append(" -> ").append(descendant);          // results contains "existing" paths, so the newly added ones are in reverse
            if (count == 10) {
                sb.append("... (and ").append(list.size() - 10).append(" more)");
                break;
            }
        }
        return sb.toString();
    }

    private String getWhereClauseForCycleCheck(List<Edge> edges) {
        StringBuilder whereClause = new StringBuilder();
        boolean first = true;
        for (Edge edge : edges) {
            if (first) {
                first = false;
            } else {
                whereClause.append(" or ");
            }
            checkOidSyntax(edge.getAncestor());
            checkOidSyntax(edge.getDescendant());
            whereClause.append("(ancestor_oid = '").append(edge.getDescendant()).append("'");
            whereClause.append(" and descendant_oid = '").append(edge.getAncestor()).append("')");
        }
        return whereClause.toString();
    }

    // temporary solution, until we find out how to send a list of pairs to the query
    private void checkOidSyntax(String oid) {
        for (int i = 0; i < oid.length(); i++) {
            char c = oid.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != ':' && c != '.') {        // strictly speaking, : and . are not legal either, but we use them in tests
                throw new IllegalArgumentException("Illegal character(s) in OID " + oid);
            }
        }
    }

    private void dropDeltaTableIfNecessary(EntityManager em, String deltaTempTableName) {
        // postgresql deletes the table automatically on commit
        if (isSQLServer()) {
            // TODO drop temporary if using SQL Server
            Query dropQuery = em.createNativeQuery(
                    "if (exists (" +
                            "select * " +
                            "from sys.tables " +
                            "where name like '" + deltaTempTableName + "%'))\n" +
                            "drop table " + deltaTempTableName + ";");
            dropQuery.executeUpdate();
        }
    }

    //endregion

    //region Handling DELETE operation
    private void handleDelete(String oid, Context context, EntityManager em) {

        List<String> livingChildren = getChildren(oid, em);
        if (livingChildren.isEmpty()) {
            handleDeleteLeaf(oid, em);
            return;
        }

        // delete all edges "<child> -> OID" from the closure
        removeChildrenEdges(oid, livingChildren, context, em);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Deleted {} 'child' links.", livingChildren.size());
        }

        // delete all edges "OID -> <parent>" from the closure
        List<String> livingParents = retainExistingOids(getParents(oid, em), em);
        removeParentEdges(oid, livingParents, context, em);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Deleted {} 'parent' links.", livingParents.size());
        }

        // delete (OID, OID) record
        Query deleteSelfQuery = em.createNativeQuery("delete from " + CLOSURE_TABLE_NAME + " " +
                "where descendant_oid=:oid and ancestor_oid=:oid");
        deleteSelfQuery.setParameter("oid", oid);
        int count = deleteSelfQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removed {} self-record from closure table.", count);
        }
    }

    private void handleDeleteLeaf(String oid, EntityManager em) {
        Query removeFromClosureQuery = em.createNativeQuery(
                "delete from " + CLOSURE_TABLE_NAME + " " +
                        "where descendant_oid = :oid");
        removeFromClosureQuery.setParameter("oid", oid);
        int count = removeFromClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("DeleteLeaf: Removed {} records from closure table.", count);
        }
    }

    private void removeChildrenEdges(String oid, List<String> livingChildren, Context context, EntityManager em) {
        List<Edge> edges = childrenToEdges(oid, livingChildren);
        removeIndependentEdges(edges, context, em);
    }

    private void removeParentEdges(String oid, Collection<String> parents, Context context, EntityManager em) {
        List<Edge> edges = parentsToEdges(oid, parents);
        removeIndependentEdges(edges, context, em);
    }

    private void removeIndependentEdges(List<Edge> edges, Context context, EntityManager em) {
        long start = System.currentTimeMillis();
        LOGGER.trace("===================== REMOVE INDEPENDENT EDGES: {} ================", edges);

        if (!edges.isEmpty()) {
            removeIndependentEdgesInternal(edges, context, em);
        }
        em.flush();
        em.clear();

        LOGGER.trace("--------------------- DONE REMOVE EDGES: {} ({} ms) ----------------", edges, System.currentTimeMillis() - start);
    }

    private void removeIndependentEdgesInternal(List<Edge> edges, Context context, EntityManager em) {

        String deltaTempTableName = computeDeltaTable(edges, context, em);
        try {
            int count;

            String deleteFromClosureQueryText, updateInClosureQueryText;
            if (isPostgreSQL() || isOracle()) {
                deleteFromClosureQueryText = "delete from " + CLOSURE_TABLE_NAME + " " +
                        "where (descendant_oid, ancestor_oid, val) in " +
                        "(select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ")";
                updateInClosureQueryText = "update " + CLOSURE_TABLE_NAME + " " +
                        "set val = val - (select val from " + deltaTempTableName + " td " +
                        "where td.descendant_oid=" + CLOSURE_TABLE_NAME + ".descendant_oid and td.ancestor_oid=" + CLOSURE_TABLE_NAME + ".ancestor_oid) " +
                        "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from " + deltaTempTableName + ")";
            } else if (isSQLServer()) {
                deleteFromClosureQueryText = "delete " + CLOSURE_TABLE_NAME + " from " + CLOSURE_TABLE_NAME + " " +
                        "inner join " + deltaTempTableName + " td on " +
                        "td.descendant_oid = " + CLOSURE_TABLE_NAME + ".descendant_oid and td.ancestor_oid = " + CLOSURE_TABLE_NAME + ".ancestor_oid and " +
                        "td.val = " + CLOSURE_TABLE_NAME + ".val";
                updateInClosureQueryText = "update " + CLOSURE_TABLE_NAME + " " +
                        "set " + CLOSURE_TABLE_NAME + ".val = " + CLOSURE_TABLE_NAME + ".val - td.val " +
                        "from " + CLOSURE_TABLE_NAME + " " +
                        "inner join " + deltaTempTableName + " td " +
                        "on td.descendant_oid=" + CLOSURE_TABLE_NAME + ".descendant_oid and " +
                        "td.ancestor_oid=" + CLOSURE_TABLE_NAME + ".ancestor_oid";
            } else {
                throw new UnsupportedOperationException("Org. closure manager - unsupported database operation");
            }
            long startDelete = System.currentTimeMillis();
            Query deleteFromClosureQuery = em.createNativeQuery(deleteFromClosureQueryText);
            count = deleteFromClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Deleted {} records from closure table in {} ms", count, System.currentTimeMillis() - startDelete);
            }
            if (DUMP_TABLES) {
                dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
            }

            long startUpdate = System.currentTimeMillis();
            Query updateInClosureQuery = em.createNativeQuery(updateInClosureQueryText);
            count = updateInClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Updated {} records in closure table in {} ms", count, System.currentTimeMillis() - startUpdate);
            }
            if (DUMP_TABLES) {
                dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
            }
        } finally {
            dropDeltaTableIfNecessary(em, deltaTempTableName);
        }
    }
    //endregion

    //region Handling MODIFY

    private void handleModify(String oid, Collection<? extends ItemDelta> modifications,
            PrismObject<? extends ObjectType> originalObject, Context context, EntityManager em) {
        if (modifications.isEmpty()) {
            return;
        }

        Set<String> parentsToDelete = getParentOidsToDelete(modifications, originalObject);
        Set<String> parentsToAdd = getParentOidsToAdd(modifications, originalObject);

        Collection<String> livingParentsToDelete = retainExistingOids(parentsToDelete, em);
        Collection<String> livingParentsToAdd = retainExistingOids(parentsToAdd, em);

        parentsToDelete.removeAll(parentsToAdd);            // if something is deleted and the re-added we can skip this operation

        removeParentEdges(oid, livingParentsToDelete, context, em);
        addParentEdges(oid, livingParentsToAdd, context, em);
    }

    //endregion

    //region Misc

    private void lockClosureTable(EntityManager em) {
        long start = System.currentTimeMillis();
        LOGGER.trace("Locking closure table");
        if (isOracle()) {
            Query q = em.createNativeQuery(
                    "LOCK TABLE " + CLOSURE_TABLE_NAME + " IN EXCLUSIVE MODE");
            q.executeUpdate();
        } else if (isSQLServer()) {
            Query q = em.createNativeQuery(
                    "SELECT count(*) FROM " + CLOSURE_TABLE_NAME + " WITH (TABLOCK, XLOCK)");
            q.getResultList();
        } else {
            throw new AssertionError("Neither Oracle nor SQL Server");
        }
        LOGGER.trace("...locked in {} ms", System.currentTimeMillis() - start);

    }

    // returns table name
    private String computeDeltaTable(List<Edge> edges, Context context, EntityManager em) {

        if (edges.isEmpty()) {
            throw new IllegalArgumentException("No edges to add/remove");
        }

        String deltaTempTableName;

        if (context.temporaryTableName != null) {
            deltaTempTableName = context.temporaryTableName;                  // table was created on the beginning of transaction
        } else if (isOracle()) {
            deltaTempTableName = TEMP_DELTA_TABLE_NAME_FOR_ORACLE;            // table definition is global
        } else {
            deltaTempTableName = generateDeltaTempTableName();                // table will be created now
        }

        if (COUNT_CLOSURE_RECORDS && LOGGER.isTraceEnabled()) {
            Query q = em.createNativeQuery("select count(*) from " + CLOSURE_TABLE_NAME);
            List list = q.getResultList();
            LOGGER.trace("OrgClosure has {} rows", list.toString());
        }

        long start;
        int count;

        String selectClause = "select t1.descendant_oid as descendant_oid, t2.ancestor_oid as ancestor_oid, " +
                "sum(t1.val*t2.val) as val " +
                "from " + CLOSURE_TABLE_NAME + " t1, " + CLOSURE_TABLE_NAME + " t2 " +
                "where " + getWhereClause(edges) + " " +
                "group by t1.descendant_oid, t2.ancestor_oid";

        if (isSQLServer()) {
            // we create the table manually, because we want to have an index on it, and
            // with serializable transactions it is not possible to create index within the transaction (after inserting data)
            start = System.currentTimeMillis();
            final String createTableSql = "create table " + deltaTempTableName + " (" +
                    "descendant_oid NVARCHAR(36) COLLATE database_default, " +
                    "ancestor_oid NVARCHAR(36) COLLATE database_default, " +
                    "val INT, " +
                    "PRIMARY KEY (descendant_oid, ancestor_oid))";
//            NativeQuery createTableQuery = em.createNativeQuery(createTableSql);
//            createTableQuery.executeUpdate();  <--- this does not work because the temporary table gets deleted when the command terminates (preparedStatement issue - maybe something like this: https://support.microsoft.com/en-us/kb/280134 ?)
            Session session = em.unwrap(Session.class);
            session.doWork(connection -> RUtil.executeStatement(connection, createTableSql));
            LOGGER.trace("Empty delta table created in {} ms", System.currentTimeMillis() - start);

            Query insertQuery = em.createNativeQuery("insert into " + deltaTempTableName + " " + selectClause);
            start = System.currentTimeMillis();
            count = insertQuery.executeUpdate();
        } else {
            String createTablePrefix;
            if (isPostgreSQL()) {
                createTablePrefix = "create local temporary table " + deltaTempTableName + " on commit drop as ";
            } else if (isOracle()) {
                // todo skip if this is first in this transaction
                Query q = em.createNativeQuery("delete from " + deltaTempTableName);
                int c = q.executeUpdate();
                LOGGER.trace("Deleted {} rows from temporary table {}", c, deltaTempTableName);
                createTablePrefix = "insert into " + deltaTempTableName + " ";
            } else {
                throw new UnsupportedOperationException("Org. closure manager - unsupported database operation");
            }
            Query query1 = em.createNativeQuery(createTablePrefix + selectClause);
            start = System.currentTimeMillis();
            count = query1.executeUpdate();
        }
        LOGGER.trace("Added {} records to temporary delta table {} ({} ms).", count, deltaTempTableName,
                System.currentTimeMillis() - start);

        if (isPostgreSQL()) {
            start = System.currentTimeMillis();
            Query qIndex = em.createNativeQuery("CREATE INDEX " + deltaTempTableName + "_idx " +
                    "  ON " + deltaTempTableName +
                    "  USING btree " +
                    "  (descendant_oid, ancestor_oid)");
            qIndex.executeUpdate();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Index created in {} ms", System.currentTimeMillis() - start);
            }
        }

        if (DUMP_TABLES) {
            dumpOrgClosureTypeTable(em, CLOSURE_TABLE_NAME);
        }
        if (DUMP_TABLES) {
            dumpOrgClosureTypeTable(em, deltaTempTableName);
        }

        // TODO drop delta table in case of exception

        return deltaTempTableName;
    }

    private String generateDeltaTempTableName() {
        String deltaTempTableName;
        deltaTempTableName =
                (isSQLServer() ? "#" : "") +
                        "m_org_closure_delta_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        return deltaTempTableName;
    }

    private String getWhereClause(List<Edge> edges) {
        StringBuilder whereClause = new StringBuilder();
        boolean first = true;
        for (Edge edge : edges) {
            if (first) {
                first = false;
            } else {
                whereClause.append(" or ");
            }
            checkOidSyntax(edge.getDescendant());
            checkOidSyntax(edge.getAncestor());
            whereClause.append("(t1.ancestor_oid = '").append(edge.getTail()).append("'");
            whereClause.append(" and t2.descendant_oid = '").append(edge.getHead()).append("')");
        }
        return whereClause.toString();
    }

    private void dumpOrgClosureTypeTable(EntityManager em, String tableName) {
        Query q = em.createNativeQuery(
                "select descendant_oid, ancestor_oid, val from " + tableName, "OrgClosureBasic");
        List<Object[]> list = q.getResultList();
        LOGGER.trace("{} ({} rows):", tableName, list.size());
        for (Object[] row : list) {
            LOGGER.trace(" - [d={}, a={}, val={}]", row);
        }
    }

    private void initializeOracleTemporaryTable() {
        try (EntityManager em = baseHelper.getEntityManagerFactory().createEntityManager()) {
            Query qCheck = em.createNativeQuery(
                    "select table_name from user_tables"
                            + " where table_name = upper('" + TEMP_DELTA_TABLE_NAME_FOR_ORACLE + "')");
            if (qCheck.getResultList().isEmpty()) {
                LOGGER.info("Creating temporary table {}", TEMP_DELTA_TABLE_NAME_FOR_ORACLE);
                em.getTransaction().begin();
                Query qCreate = em.createNativeQuery(
                        "CREATE GLOBAL TEMPORARY TABLE " + TEMP_DELTA_TABLE_NAME_FOR_ORACLE
                                + "    (descendant_oid VARCHAR2(36 CHAR), "
                                + "     ancestor_oid VARCHAR2(36 CHAR), "
                                + "     val NUMBER (10, 0), "
                                + "     PRIMARY KEY (descendant_oid, ancestor_oid)) "
                                + "  ON COMMIT DELETE ROWS");
                qCreate.executeUpdate();
                em.getTransaction().commit();
            }
        } catch (RuntimeException e) {
            String m = "Couldn't create temporary table " + TEMP_DELTA_TABLE_NAME_FOR_ORACLE + ". Please create the table manually.";
            LoggingUtils.logException(LOGGER, m, e);
            throw new SystemException(m, e);
        }
    }

    private List<ReferenceDelta> filterParentRefDeltas(Collection<? extends ItemDelta> modifications) {
        boolean containsAdd = false, containsDelete = false, containsReplace = false;
        List<ReferenceDelta> deltas = new ArrayList<>();
        if (modifications == null) {
            return deltas;
        }

        for (ItemDelta delta : modifications) {
            if (!QNameUtil.match(ObjectType.F_PARENT_ORG_REF, delta.getElementName())) {            // TODO is this OK?
                continue;
            }
            if (delta.isAdd()) {
                containsAdd = true;
            }
            if (delta.isDelete()) {
                containsDelete = true;
            }
            if (delta.isReplace()) {
                if (containsReplace) {
                    throw new IllegalStateException("Unsupported combination of parentOrgRef operations: more REPLACE ItemDeltas");
                }
                containsReplace = true;
            }
            deltas.add((ReferenceDelta) delta);
        }

        if (containsReplace && (containsAdd || containsDelete)) {
            throw new IllegalStateException("Unsupported combination of parentOrgRef operations: REPLACE with either ADD or DELETE");
        }

        return deltas;
    }

    private Set<String> getParentOidsToDelete(Collection<? extends ItemDelta> modifications, PrismObject<? extends ObjectType> originalObject) {
        Objects.requireNonNull(originalObject);

        Set<String> oids = new HashSet<>();

        Set<String> existingOids = getParentOidsFromObject(originalObject);

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToDelete() != null) {
                for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToDelete()) {
                    String oid = val.getOid();
                    if (existingOids.contains(oid)) {           // if it's not there, we do not want to delete it!
                        oids.add(oid);
                    }
                }
            }
            if (delta.getValuesToReplace() != null) {
                // at this point we can assume this is not mixed with DELETE or ADD deltas
                // we mark all existing values not present in the new set as being removed!
                oids = new HashSet<>();     // and we do this for the latest REPLACE ItemDelta
                for (String existingOid : existingOids) {
                    boolean found = false;
                    for (PrismReferenceValue newVal : (Collection<PrismReferenceValue>) delta.getValuesToReplace()) {
                        if (existingOid.equals(newVal.getOid())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        oids.add(existingOid);      // if existing OID was not found in values to REPLACE, it should be deleted
                    }
                }
            }
        }

        return oids;
    }

    // filters out those OIDs that are already present in originalObject (beware, it may be null)
    private Set<String> getParentOidsToAdd(Collection<? extends ItemDelta> modifications, PrismObject<? extends ObjectType> originalObject) {
        Set<String> oids = new HashSet<>();

        Set<String> existingOids = getParentOidsFromObject(originalObject);

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToAdd() != null) {
                for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToAdd()) {
                    String oid = val.getOid();
                    if (!existingOids.contains(oid)) {          // if it's already there, we don't want to add it
                        oids.add(oid);
                    }
                }
            }
            if (delta.getValuesToReplace() != null) {
                // at this point we can assume this is not mixed with DELETE or ADD deltas
                // we mark all 'new' values in REPLACE delta not present in existing object as being added
                oids = new HashSet<>();     // and we do this for the latest REPLACE ItemDelta
                for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToReplace()) {
                    String oid = val.getOid();
                    if (!existingOids.contains(oid)) {          // if it's already there, we don't want to add it
                        oids.add(oid);
                    }
                }
            }
        }

        return oids;
    }

    private Set<String> getParentOidsFromObject(PrismObject<? extends ObjectType> originalObject) {
        Set<String> retval = new HashSet<>();
        if (originalObject != null) {
            for (ObjectReferenceType ort : originalObject.asObjectable().getParentOrgRef()) {
                retval.add(ort.getOid());
            }
        }
        return retval;
    }

    // only for performance testing (doesn't account for multithreading!)
    public long getLastOperationDuration() {
        return lastOperationDuration;
    }

    private <T extends ObjectType> boolean isTypeNonLeaf(Class<T> type) {
        return OrgType.class.equals(type);
    }

    private List<String> getParents(String oid, EntityManager em) {
        Query parentsQuery = em.createQuery("select distinct targetOid from RObjectReference where ownerOid=:oid  and referenceType=0");
        parentsQuery.setParameter("oid", oid);
        return parentsQuery.getResultList();
    }

    private List<String> getChildren(String oid, EntityManager em) {
        Query childrenQuery = em.createQuery("select distinct parentRef.ownerOid from RObjectReference as parentRef" +
                " join parentRef.owner as owner where parentRef.targetOid=:oid and parentRef.referenceType=0" +
                " and owner.objectTypeClass = :orgType");
        childrenQuery.setParameter("orgType", RObjectType.ORG);         // TODO eliminate use of parameter here
        childrenQuery.setParameter("oid", oid);
        return childrenQuery.getResultList();
    }

    private List<String> retainExistingOids(Collection<String> oids, EntityManager em) {
        if (!oids.isEmpty()) {
            Query query = em.createQuery("select o.oid from RObject o where o.oid in (:oids)");
            query.setParameter("oids", oids);
            return query.getResultList();
        } else {
            return new ArrayList<>();
        }
    }

    private boolean isOracle() {
        return baseHelper.getConfiguration().isUsingOracle();
    }

    private boolean isSQLServer() {
        return baseHelper.getConfiguration().isUsingSQLServer();
    }

    private boolean isPostgreSQL() {
        return baseHelper.getConfiguration().isUsingPostgreSQL();
    }

    //endregion

    //region Helper classes

    public static class Edge {
        private final String descendant; // i.e. child, or technically, edge tail
        private final String ancestor; // i.e. parent, or technically, edge head

        public Edge(String descendant, String ancestor) {
            this.descendant = descendant;
            this.ancestor = ancestor;
        }

        public String getDescendant() {
            return descendant;
        }

        public String getAncestor() {
            return ancestor;
        }

        public String getTail() {
            return descendant;
        }

        public String getHead() {
            return ancestor;
        }

        @Override
        public String toString() {
            return descendant + "->" + ancestor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Edge edge = (Edge) o;

            if (!ancestor.equals(edge.ancestor)) {
                return false;
            }
            return descendant.equals(edge.descendant);
        }

        @Override
        public int hashCode() {
            int result = descendant.hashCode();
            result = 31 * result + ancestor.hashCode();
            return result;
        }
    }

    public enum Operation {ADD, DELETE, MODIFY}

    public enum StartupAction {

        NONE("none"), CHECK("check"), REBUILD_IF_NEEDED("rebuildIfNeeded"), ALWAYS_REBUILD("alwaysRebuild");

        private final String value;

        StartupAction(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static StartupAction fromValue(String v) {
            for (StartupAction a : StartupAction.values()) {
                if (a.value.equals(v)) {
                    return a;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }

    public static class Context {
        String temporaryTableName;
    }
    //endregion
}
