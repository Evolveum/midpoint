/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.mutable.MutableInt;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.IntegerType;

import java.util.*;

/**
 * This class and its subclasses provides org. closure table handling.
 *
 * Data structures used are:
 *
 *  (1) Repo object graph G = (V, E) where V is a set of vertices (repo objects) and E is a set of edges (parentRef relations).
 *      There is an edge e = (V1, V2) in E [i.e. edge from V1 to V2] if and only if V1.parentRef contains V2 [i.e. V2 is a parent of V1].
 *
 *  (2) OrgClosure table. OrgClosure(D, A, N) iff there are exactly N paths in object graph from D (descendant) to A (ascendant).
 *      It is transitive reflexive closure, i.e. OrgClosure(V, V, 1) items are there as well.
 *
 * Algorithms taken from "SQL Design Patterns" book by Vadim Tropashko (http://vadimtropashko.wordpress.com/)
 * namely from Chapter 6 (http://vadimtropashko.files.wordpress.com/2014/01/book_sql_chap6_v1.pdf).
 * SQL queries were then optimized by hand for various database engines.
 *
 * @author lazyman
 * @author mederly
 */
public class OrgClosureManager {

    private static final String closureTableName = "m_org_closure";

    public static enum Operation {ADD, DELETE, MODIFY}

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureManager.class);

    private static boolean DUMP_TABLES = false;
    private static final boolean COUNT_CLOSURE_RECORDS = false;

    private SqlRepositoryConfiguration repoConfiguration;

    public static class Edge {
        private String descendant;              // i.e. child, or technically, edge tail
        private String ancestor;                // i.e. parent, or technically, edge head
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
        public String toString() {
            return descendant + "->" + ancestor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Edge edge = (Edge) o;

            if (!ancestor.equals(edge.ancestor)) return false;
            if (!descendant.equals(edge.descendant)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = descendant.hashCode();
            result = 31 * result + ancestor.hashCode();
            return result;
        }
    }

    public OrgClosureManager(SqlRepositoryConfiguration repoConfiguration) {
        this.repoConfiguration = repoConfiguration;
    }

    // only for single-thread performance testing
    long lastOperationDuration;

    /**
     * Main method called from SQL repository service to update the closure table during an operation.
     *
     * @param originalObject Original state of the object - before applying modification in the repository.
     *                       It is used only in case of MODIFY (note that "overwriting ADD" is present here as MODIFY!)
     * @param modifications Collection of modifications to be applied to the object.
     * @param session Database session to use.
     * @param oid OID of the object.
     * @param type Type of the object.
     * @param operation Operation that is carried out.
     * @param <T>
     */
    public <T extends ObjectType> void updateOrgClosure(PrismObject<? extends ObjectType> originalObject,
                                                        Collection<? extends ItemDelta> modifications, Session session,
                                                        String oid, Class<T> type, Operation operation) {

        switch (repoConfiguration.getOrgClosureObjects()) {
            case NONE:
                return;
            case FOCUS:
                if (!FocusType.class.isAssignableFrom(type)) {
                return;
            }
        }

        session.flush();
        session.clear();

        long time = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("################# Starting {} for org. closure for {} oid={}.", new Object[]{operation, type.getSimpleName(), oid});
        }

        List<ReferenceDelta> deltas = filterParentRefDeltas(modifications);

        switch (operation) {
            case ADD:
                handleAdd(oid, type, deltas, session);
                break;
            case DELETE:
                handleDelete(oid, type, session);
                break;
            case MODIFY:
                handleModify(oid, type, deltas, originalObject, session);
        }

        long duration = System.currentTimeMillis() - time;
        LOGGER.debug("################# Org. closure update finished in {} ms.", duration);
        lastOperationDuration = duration;
    }

    public void executeStartupAction(SqlRepositoryServiceImpl service) {
        if (!isEnabled()) {
            return;
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
        checkAndOrRebuild(service, check, rebuild, repoConfiguration.isStopOnOrgClosureStartupFailure());
    }

    public boolean isEnabled() {
        return repoConfiguration.getOrgClosureObjects() != OrgClosureObjects.NONE;
    }


    /**
     * Does a basic consistency check by comparing whether are there any orgs without entries in closure
     * (the reverse direction is ensured via foreign keys in m_org_closure).
     */
    public void checkAndOrRebuild(SqlRepositoryServiceImpl service, boolean check, boolean rebuild, boolean stopOnFailure) {
        Session session = service.getSessionFactory().openSession();
        try {
            boolean ok = false;
            if (check) {
                Query q = session.createSQLQuery(
                        "select count(m_org.oid) as problems from m_org left join m_org_closure cl " +
                                "on cl.descendant_oid = m_org.oid and cl.ancestor_oid = m_org.oid " +
                                "where cl.descendant_oid is null").addScalar("problems", IntegerType.INSTANCE);
                List problemsList = q.list();
                if (problemsList == null || problemsList.size() != 1) {
                    throw new IllegalStateException("Unexpected return value from the closure check query: " + problemsList + " (a 1-item list of Integer expected)");
                }
                int problems = (int) problemsList.get(0);
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
                rebuild(service, stopOnFailure);
            }
        } finally {
            session.close();
        }
    }

    //region Rebuilding org closure
    private void rebuild(SqlRepositoryServiceImpl service, boolean stopOnFailure) {
        OperationResult result = new OperationResult("rebuild");

        LOGGER.info("Rebuilding org closure table");

        final Session session = service.beginTransaction();
        try {
            Query deleteQuery = session.createSQLQuery("delete from " + closureTableName);
            deleteQuery.executeUpdate();
            LOGGER.trace("Closure table content deleted");

            final int orgsTotal = service.countObjects(OrgType.class, new ObjectQuery(), result);
            final MutableInt orgsProcessed = new MutableInt(0);

            ResultHandler<OrgType> handler = new ResultHandler<OrgType>() {
                @Override
                public boolean handle(PrismObject<OrgType> object, OperationResult parentResult) {
                    LOGGER.trace("Processing {}", object);
                    handleAdd(object.getOid(), object.getCompileTimeClass(), getExistingParentOids(object), session);
                    orgsProcessed.add(1);
                    int currentState = orgsProcessed.intValue();
                    if (currentState % 100 == 0) {
                        LOGGER.info("{} organizations processed (out of {})", currentState, orgsTotal);
                    }
                    return true;
                }
            };
            service.searchObjectsIterative(OrgType.class, new ObjectQuery(), handler, null, result);

            session.getTransaction().commit();
            LOGGER.info("Org closure table was successfully rebuilt; all {} organizations processed", orgsTotal);

        } catch (SchemaException|RuntimeException e) {
            session.getTransaction().rollback();
            LoggingUtils.logException(LOGGER, "Rebuilding closure failed", e);
            if (stopOnFailure) {
                throw new SystemException("Rebuilding closure failed: " + e.getMessage(), e);
            }
        }
    }
    //endregion

    //region Handling ADD operation

    // we can safely expect that the object didn't exist before (because the "overwriting add" is sent to us as MODIFY operation)
    private <T extends ObjectType> void handleAdd(String oid, Class<T> type, List<ReferenceDelta> deltas, Session session) {
        handleAdd(oid, type, getParentOidsFromAddDeltas(deltas, null), session);
    }

    // parents may be non-existent at this point
    private <T extends ObjectType> void handleAdd(String oid, Class<T> type, Set<String> parents, Session session) {
        // adding self-record
        session.save(new ROrgClosure(oid, oid, 1));
        session.flush();
        session.clear();

        boolean maybeNonLeaf = isTypeNonLeaf(type);

        List<String> livingChildren = null;
        if (maybeNonLeaf) {
            livingChildren = getChildren(oid, session);        // no need to check existence of these oids, as owner is a FK pointing to RObject in RParentRef
            addChildrenEdges(oid, livingChildren, session);
        }

        // all parents are "new", so we should just select which do really exist at this moment
        Collection<String> livingParents = retainExistingOids(parents, session);

        if (livingParents.size() <= 1 && (livingChildren == null || livingChildren.isEmpty())) {
            String parent;
            if (livingParents.isEmpty()) {
                parent = null;
            } else {
                parent = livingParents.iterator().next();
            }
            addEdgeSimple(oid, parent, session);
        } else {
            addParentEdges(oid, livingParents, session);
        }
    }

    // we expect that all livingChildren do exist and
    // that none of the links child->oid does exist yet
    private void addChildrenEdges(String oid, List<String> livingChildren, Session session) {
        List<Edge> edges = childrenToEdges(oid, livingChildren);
        addIndependentEdges(edges, session);
    }

    private void removeChildrenEdges(String oid, List<String> livingChildren, Session session) {
        List<Edge> edges = childrenToEdges(oid, livingChildren);
        removeIndependentEdges(edges, session);
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
    private void addParentEdges(String oid, Collection<String> livingParents, Session session) {
        List<Edge> edges = parentsToEdges(oid, livingParents);
        addIndependentEdges(edges, session);
    }

    private void removeParentEdges(String oid, Collection<String> parents, Session session) {
        List<Edge> edges = parentsToEdges(oid, parents);
        removeIndependentEdges(edges, session);
    }

    private List<Edge> parentsToEdges(String oid, Collection<String> parents) {
        List<Edge> edges = new ArrayList<>();
        for (String parent : parents) {
            edges.add(new Edge(oid, parent));
        }
        return edges;
    }

    // we expect that the link oid->parent does not exist yet and the parent exists
    private void addEdgeSimple(String oid, String parent, Session session) {
        if (parent != null) {
            long start = System.currentTimeMillis();
            Query addToClosureQuery = session.createSQLQuery(
                    "insert into "+closureTableName+" (descendant_oid, ancestor_oid, val) " +
                            "select :oid as descendant_oid, CL.ancestor_oid as ancestor_oid, 1 as val " +
                            "from "+closureTableName+" CL " +
                            "where CL.descendant_oid = :parent");
            addToClosureQuery.setString("oid", oid);
            addToClosureQuery.setParameter("parent", parent);
            int count = addToClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("addEdges simplified: Added {} records to closure table ({} ms).", count,
                        System.currentTimeMillis() - start);
        }
        session.flush();
        session.clear();
    }

    // IMPORTANT PRECONDITIONS:
    //  - all edges are "legal", i.e. their tails and heads (as objects) do exist in repository
    //  - none of the edges does exist yet
    private void addIndependentEdges(List<Edge> edges, Session session) {

        long start = System.currentTimeMillis();
        LOGGER.trace("===================== ADD INDEPENDENT EDGES: {} ================", edges);

        if (!edges.isEmpty()) {
            String deltaTempTableName = computeDeltaTable(edges, session);
            try {
                int count;

                if (isMySQL() || isOracle() || isSQLServer()) {

                    long startUpsert = System.currentTimeMillis();
                    String upsertQueryText;

                    if (isMySQL()) {
                        upsertQueryText = "insert into " + closureTableName + " (descendant_oid, ancestor_oid, val) " +
                                "select descendant_oid, ancestor_oid, val from " + deltaTempTableName + " delta " +
                                "on duplicate key update " + closureTableName + ".val = " + closureTableName + ".val + values(val)";
                    } else if (isSQLServer()) {
                        // TODO try if this one (without prefixes in INSERT clause does not work for Oracle)
                        upsertQueryText = "merge into " + closureTableName + " closure " +
                                "using (select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ") delta " +
                                "on (closure.descendant_oid = delta.descendant_oid and closure.ancestor_oid = delta.ancestor_oid) " +
                                "when matched then update set closure.val = closure.val + delta.val " +
                                "when not matched then insert (descendant_oid, ancestor_oid, val) " +
                                "values (delta.descendant_oid, delta.ancestor_oid, delta.val);";
                    } else { // Oracle
                        upsertQueryText = "merge into " + closureTableName + " closure " +
                            "using (select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ") delta " +
                            "on (closure.descendant_oid = delta.descendant_oid and closure.ancestor_oid = delta.ancestor_oid) " +
                            "when matched then update set closure.val = closure.val + delta.val " +
                            "when not matched then insert (closure.descendant_oid, closure.ancestor_oid, closure.val) " +
                                "values (delta.descendant_oid, delta.ancestor_oid, delta.val)";
                    }
                    Query upsertQuery = session.createSQLQuery(upsertQueryText);
                    int countUpsert = upsertQuery.executeUpdate();
                    if (LOGGER.isTraceEnabled())
                        LOGGER.trace("Added/updated {} records to closure table ({} ms)", countUpsert, System.currentTimeMillis() - startUpsert);
                    if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);

                } else {    // separate update and insert

                    long startUpdate = System.currentTimeMillis();
                    String updateInClosureQueryText;
                    if (isH2()) {
                        updateInClosureQueryText = "update " + closureTableName + " " +
                                "set val = val + (select val from " + deltaTempTableName + " td " +
                                "where td.descendant_oid=" + closureTableName + ".descendant_oid and td.ancestor_oid=" + closureTableName + ".ancestor_oid) " +
                                "where (descendant_oid, ancestor_oid) in (select (descendant_oid, ancestor_oid) from " + deltaTempTableName + ")";
                    } else if (isPostgreSQL()) {
                        updateInClosureQueryText = "update " + closureTableName + " " +
                                "set val = val + (select val from " + deltaTempTableName + " td " +
                                "where td.descendant_oid=" + closureTableName + ".descendant_oid and td.ancestor_oid=" + closureTableName + ".ancestor_oid) " +
                                "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from " + deltaTempTableName + ")";
                    } else {
                        throw new UnsupportedOperationException("implement other databases");
                    }
                    Query updateInClosureQuery = session.createSQLQuery(updateInClosureQueryText);
                    int countUpdate = updateInClosureQuery.executeUpdate();
                    if (LOGGER.isTraceEnabled())
                        LOGGER.trace("Updated {} records to closure table ({} ms)", countUpdate, System.currentTimeMillis() - startUpdate);

                    if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);

                    long startAdd = System.currentTimeMillis();
                    String addQuery =
                            "insert into " + closureTableName + " (descendant_oid, ancestor_oid, val) " +
                                    "select descendant_oid, ancestor_oid, val from " + deltaTempTableName + " delta ";
                    if (countUpdate > 0) {
                        if (isH2()) {
                            addQuery += " where (descendant_oid, ancestor_oid) not in (select (descendant_oid, ancestor_oid) from " + closureTableName + ")";
                        } else if (isPostgreSQL()) {
                            addQuery += " where not exists (select 1 from " + closureTableName + " cl where cl.descendant_oid=delta.descendant_oid and cl.ancestor_oid=delta.ancestor_oid)";
                        } else {
                            throw new UnsupportedOperationException("implement other databases");
                        }
                    }
                    Query addToClosureQuery = session.createSQLQuery(addQuery);
                    count = addToClosureQuery.executeUpdate();
                    if (LOGGER.isTraceEnabled())
                        LOGGER.trace("Added {} records to closure table ({} ms)", count, System.currentTimeMillis() - startAdd);

                    if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);
                }
            } finally {
                dropDeltaTableIfNecessary(session, deltaTempTableName);
            }
        }

        session.flush();
        session.clear();

        LOGGER.trace("--------------------- DONE ADD EDGES: {} ({} ms) ----------------", edges, System.currentTimeMillis() - start);
    }

    private void dropDeltaTableIfNecessary(Session session, String deltaTempTableName) {
        // postgresql deletes the table automatically on commit
        // TODO what in case of H2?
        if (isMySQL()) {
            Query dropQuery = session.createSQLQuery("drop temporary table " + deltaTempTableName);
            dropQuery.executeUpdate();
        } else if (isSQLServer()) {
            // TODO drop temporary if using SQL Server
            Query dropQuery = session.createSQLQuery(
                    "if (exists (" +
                            "select * " +
                            "from sys.tables " +
                            "where name like '"+deltaTempTableName+"%'))\n" +
                            "drop table " + deltaTempTableName + ";");
            dropQuery.executeUpdate();
        }
    }

    //endregion

    //region Handling DELETE operation
    private <T extends ObjectType> void handleDelete(String oid, Class<T> type, Session session) {

        boolean maybeNonLeaf = isTypeNonLeaf(type);
        if (!maybeNonLeaf) {
            handleDeleteLeaf(oid, session);
            return;
        }

        List<String> livingChildren = getChildren(oid, session);
        if (livingChildren.isEmpty()) {
            handleDeleteLeaf(oid, session);
            return;
        }

        // delete all edges "<child> -> OID" from the closure
        removeChildrenEdges(oid, livingChildren, session);
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'child' links.", livingChildren.size());

        // delete all edges "OID -> <parent>" from the closure
        List<String> livingParents = retainExistingOids(getParents(oid, session), session);
        removeParentEdges(oid, livingParents, session);
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'parent' links.", livingParents.size());

        // delete (OID, OID) record
        Query deleteSelfQuery = session.createSQLQuery("delete from "+closureTableName+" " +
                "where descendant_oid=:oid and ancestor_oid=:oid");
        deleteSelfQuery.setString("oid", oid);
        int count = deleteSelfQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} self-record from closure table.", count);
    }

    private <T extends ObjectType> void handleDeleteLeaf(String oid, Session session) {
        Query removeFromClosureQuery = session.createSQLQuery(
                "delete from " + closureTableName + " " +
                        "where descendant_oid = :oid");
        removeFromClosureQuery.setString("oid", oid);
        int count = removeFromClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("DeleteLeaf: Removed {} records from closure table.", count);
    }

    private void removeIndependentEdges(List<Edge> edges, Session session) {

        long start = System.currentTimeMillis();
        LOGGER.trace("===================== REMOVE INDEPENDENT EDGES: {} ================", edges);

        if (!edges.isEmpty()) {
            String deltaTempTableName = computeDeltaTable(edges, session);
            try {
                int count;

                String deleteFromClosureQueryText, updateInClosureQueryText;
                if (isH2()) {
                    // delete with join is not supported by H2
                    deleteFromClosureQueryText = "delete from " + closureTableName + " cl " +
                            "where exists (" +
                            "select 0 from " + deltaTempTableName + " delta " +
                            "where cl.descendant_oid = delta.descendant_oid and cl.ancestor_oid = delta.ancestor_oid and cl.val = delta.val)";
                    updateInClosureQueryText = "update " + closureTableName + " " +
                            "set val = val - (select val from " + deltaTempTableName + " td " +
                            "where td.descendant_oid=" + closureTableName + ".descendant_oid and td.ancestor_oid=" + closureTableName + ".ancestor_oid) " +
                            "where (descendant_oid, ancestor_oid) in (select (descendant_oid, ancestor_oid) from " + deltaTempTableName + ")";
                } else if (isPostgreSQL() || isOracle()) {
                    deleteFromClosureQueryText = "delete from " + closureTableName + " " +
                            "where (descendant_oid, ancestor_oid, val) in " +
                            "(select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ")";
                    updateInClosureQueryText = "update " + closureTableName + " " +
                            "set val = val - (select val from " + deltaTempTableName + " td " +
                            "where td.descendant_oid=" + closureTableName + ".descendant_oid and td.ancestor_oid=" + closureTableName + ".ancestor_oid) " +
                            "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from " + deltaTempTableName + ")";
                } else if (isSQLServer()) {
                    // delete is the same as for MySQL
                    deleteFromClosureQueryText = "delete " + closureTableName + " from " + closureTableName + " " +
                            "inner join " + deltaTempTableName + " td on " +
                            "td.descendant_oid = "+closureTableName+".descendant_oid and td.ancestor_oid = "+closureTableName+".ancestor_oid and "+
                                "td.val = "+closureTableName+".val";
                    // update is also done via inner join (as in MySQL), but using slightly different syntax
                    updateInClosureQueryText = "update " + closureTableName + " " +
                            "set "+closureTableName+".val = "+closureTableName+".val - td.val " +
                            "from "+closureTableName + " " +
                            "inner join " + deltaTempTableName + " td " +
                            "on td.descendant_oid=" + closureTableName + ".descendant_oid and " +
                                    "td.ancestor_oid=" + closureTableName + ".ancestor_oid";
                } else if (isMySQL()) {
                    // http://stackoverflow.com/questions/652770/delete-with-join-in-mysql
                    // TODO consider this for other databases as well
                    deleteFromClosureQueryText = "delete " + closureTableName + " from " + closureTableName + " " +
                            "inner join " + deltaTempTableName + " td on " +
                            "td.descendant_oid = "+closureTableName+".descendant_oid and td.ancestor_oid = "+closureTableName+".ancestor_oid and "+
                            "td.val = "+closureTableName+".val";
                    // it is not possible to use temporary table twice in a query
                    // TODO consider using this in postgresql and h2 as well...
                    updateInClosureQueryText = "update " + closureTableName +
                            " join " + deltaTempTableName + " td " +
                            "on td.descendant_oid=" + closureTableName + ".descendant_oid and td.ancestor_oid=" + closureTableName + ".ancestor_oid " +
                            "set "+closureTableName+".val = "+closureTableName+".val - td.val";
                } else {
                    throw new UnsupportedOperationException("implement other databases");
                }
                long startDelete = System.currentTimeMillis();
                Query deleteFromClosureQuery = session.createSQLQuery(deleteFromClosureQueryText);
                count = deleteFromClosureQuery.executeUpdate();
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Deleted {} records from closure table in {} ms", count, System.currentTimeMillis() - startDelete);
                if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);

                long startUpdate = System.currentTimeMillis();
                Query updateInClosureQuery = session.createSQLQuery(updateInClosureQueryText);
                count = updateInClosureQuery.executeUpdate();
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("Updated {} records in closure table in {} ms", count, System.currentTimeMillis() - startUpdate);
                if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);
            } finally {
                dropDeltaTableIfNecessary(session, deltaTempTableName);
            }
        }
        session.flush();
        session.clear();

        LOGGER.trace("--------------------- DONE REMOVE EDGES: {} ({} ms) ----------------", edges, System.currentTimeMillis()-start);
    }

    private boolean isMySQL() {
        return repoConfiguration.isUsingMySQL();
    }

    private boolean isOracle() {
        return repoConfiguration.isUsingOracle();
    }

    private boolean isSQLServer() {
        return repoConfiguration.isUsingSQLServer();
    }

    private boolean isH2() {
        return repoConfiguration.isUsingH2();
    }

    private boolean isPostgreSQL() {
        return repoConfiguration.isUsingPostgreSQL();
    }
    //endregion

    //region Handling MODIFY

    private <T extends ObjectType> void handleModify(String oid, Class<T> type,
                                                     Collection<? extends ItemDelta> modifications,
                                                     PrismObject<? extends ObjectType> originalObject,
                                                     Session session) {
        if (modifications.isEmpty()) {
            return;
        }

        Set<String> parentsToDelete = getParentOidsFromDeleteDeltas(modifications, originalObject);
        Set<String> parentsToAdd = getParentOidsFromAddDeltas(modifications, originalObject);

        Collection<String> livingParentsToDelete = retainExistingOids(parentsToDelete, session);
        Collection<String> livingParentsToAdd = retainExistingOids(parentsToAdd, session);

        parentsToDelete.removeAll(parentsToAdd);            // if something is deleted and the re-added we can skip this operation

        removeParentEdges(oid, livingParentsToDelete, session);
        addParentEdges(oid, livingParentsToAdd, session);
    }

    //endregion

    //region Misc

    private <T extends ObjectType> boolean isTypeNonLeaf(Class<T> type) {
        return OrgType.class.equals(type);
    }

    private List<String> getParents(String oid, Session session) {
        Query parentsQuery = session.createQuery("select distinct targetOid from RParentOrgRef where ownerOid=:oid");
        parentsQuery.setString("oid", oid);
        return parentsQuery.list();
    }

    private List<String> getChildren(String oid, Session session) {
        Query childrenQuery;
        if (repoConfiguration.getOrgClosureObjects() == OrgClosureObjects.ALL) {
            childrenQuery = session.createQuery("select distinct ownerOid from RParentOrgRef where targetOid=:oid");
        } else if (repoConfiguration.getOrgClosureObjects() == OrgClosureObjects.FOCUS) {
            childrenQuery = session.createQuery("select distinct parentRef.ownerOid from RParentOrgRef as parentRef" +
                    " join parentRef.owner as owner where parentRef.targetOid=:oid" +
                    " and owner.objectTypeClass in (:types)");
            childrenQuery.setParameterList("types", Arrays.asList(RObjectType.USER, RObjectType.ORG, RObjectType.ROLE));
        } else {
            throw new IllegalStateException("Unknown option: " + repoConfiguration.getOrgClosureObjects());
        }
        childrenQuery.setString("oid", oid);
        return childrenQuery.list();
    }

    private List<String> retainExistingOids(Collection<String> oids, Session session) {
        if (!oids.isEmpty()) {
            Query query = session.createQuery("select o.oid from RObject o where o.oid in (:oids)");
            query.setParameterList("oids", oids);
            return query.list();
        } else {
            return new ArrayList<String>();
        }
    }

    //endregion

    // returns table name
    private String computeDeltaTable(List<Edge> edges, Session session) {

        if (edges.isEmpty()) {
            throw new IllegalArgumentException("No edges to add/remove");
        }

        String deltaTempTableName;

        if (isOracle()) {
            deltaTempTableName = "m_org_closure_temp_delta";            // table definition is global
        } else {
            deltaTempTableName =
                    (isSQLServer()?"##":"") +
                    "m_org_closure_delta_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        }

        if (COUNT_CLOSURE_RECORDS && LOGGER.isTraceEnabled()) {
            Query q = session.createSQLQuery("select count(*) from " + closureTableName);
            List list = q.list();
            LOGGER.trace("OrgClosure has {} rows", list.toString());
        }

        long start;
        int count;

        String selectClause = "select t1.descendant_oid as descendant_oid, t2.ancestor_oid as ancestor_oid, " +
                "sum(t1.val*t2.val) as val " +
                "from " + closureTableName + " t1, " + closureTableName + " t2 " +
                "where " + getWhereClause(edges) + " " +
                "group by t1.descendant_oid, t2.ancestor_oid";

        if (isSQLServer()) {
            // we create the table manually, because we want to have an index on it, and
            // with serializable transactions it is not possible to create index within the transaction (after inserting data)
            start = System.currentTimeMillis();
            Query createTableQuery = session.createSQLQuery("create table " + deltaTempTableName + " (" +
                    "descendant_oid NVARCHAR(36), " +
                    "ancestor_oid NVARCHAR(36), " +
                    "val INT, " +
                    "PRIMARY KEY (descendant_oid, ancestor_oid))");
            createTableQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Empty delta table created in {} ms", System.currentTimeMillis() - start);

            Query insertQuery = session.createSQLQuery("insert into " + deltaTempTableName + " " + selectClause);
            start = System.currentTimeMillis();
            count = insertQuery.executeUpdate();
        } else {
            String createTablePrefix;
            if (isPostgreSQL()) {
                createTablePrefix = "create cached local temporary " + deltaTempTableName + " on commit drop as ";
            } else if (isH2()) {
                createTablePrefix = "create temporary table " + deltaTempTableName + " as ";
            } else if (isMySQL()) {
                createTablePrefix = "create temporary table " + deltaTempTableName + " engine=memory as ";
            } else if (isOracle()) {
                // todo skip if this is first in this transaction
                Query q = session.createSQLQuery("delete from " + deltaTempTableName);
                int c = q.executeUpdate();
                LOGGER.trace("Deleted {} rows from temporary table {}", c, deltaTempTableName);
                createTablePrefix = "insert into " + deltaTempTableName + " ";
            } else {
                throw new UnsupportedOperationException("define other databases");
            }
            Query query1 = session.createSQLQuery(createTablePrefix + selectClause);
            start = System.currentTimeMillis();
            count = query1.executeUpdate();
        }
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary delta table {} ({} ms).",
                new Object[] {count, deltaTempTableName, System.currentTimeMillis()-start});

        if (isPostgreSQL()) {
            start = System.currentTimeMillis();
            Query qIndex = session.createSQLQuery("CREATE INDEX " + deltaTempTableName + "_idx " +
                    "  ON " + deltaTempTableName +
                    "  USING btree " +
                    "  (descendant_oid, ancestor_oid)");
            qIndex.executeUpdate();
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Index created in {} ms", System.currentTimeMillis()-start);
        }

        // TODO index for MySQL !!!

        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);
        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, deltaTempTableName);

        // TODO drop delta table in case of exception

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
            whereClause.append("(t1.ancestor_oid = '").append(edge.getTail()).append("'");
            whereClause.append(" and t2.descendant_oid = '").append(edge.getHead()).append("')");
        }
        return whereClause.toString();
    }

    private void dumpOrgClosureTypeTable(Session session, String tableName) {
        Query q = session.createSQLQuery("select descendant_oid, ancestor_oid, val from " + tableName);
        List<Object[]> list = q.list();
        LOGGER.trace("{} ({} rows):", tableName, list.size());
        for (Object[] row : list) {
            LOGGER.trace(" - [d={}, a={}, val={}]", row);
        }
    }

    private List<ReferenceDelta> filterParentRefDeltas(Collection<? extends ItemDelta> modifications) {
        List<ReferenceDelta> deltas = new ArrayList<>();
        if (modifications == null) {
            return deltas;
        }

        for (ItemDelta delta : modifications) {
            if (!ObjectType.F_PARENT_ORG_REF.equals(delta.getElementName())) {
                continue;
            }
            deltas.add((ReferenceDelta) delta);
        }

        return deltas;
    }

    private Set<String> getParentOidsFromDeleteDeltas(Collection<? extends ItemDelta> modifications, PrismObject<? extends ObjectType> originalObject) {
        Validate.notNull(originalObject);

        Set<String> oids = new HashSet<>();

        Set<String> existingOids = getExistingParentOids(originalObject);

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToDelete() == null) {
                continue;
            }
            for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToDelete()) {
                String oid = val.getOid();
                if (existingOids.contains(oid)) {           // if it's not there, we do not want to delete it!
                    oids.add(oid);
                }
            }
        }

        return oids;
    }

    // filters out those OIDs that are already present in originalObject (beware, it may be null)
    private Set<String> getParentOidsFromAddDeltas(Collection<? extends ItemDelta> modifications, PrismObject<? extends ObjectType> originalObject) {
        Set<String> oids = new HashSet<>();

        Set<String> existingOids = getExistingParentOids(originalObject);

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToAdd() == null) {
                continue;
            }
            for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToAdd()) {
                String oid = val.getOid();
                if (!existingOids.contains(oid)) {          // if it's already there, we don't want to add it
                    oids.add(oid);
                }
            }
        }

        return oids;
    }

    private Set<String> getExistingParentOids(PrismObject<? extends ObjectType> originalObject) {
        Set<String> retval = new HashSet<>();
        if (originalObject != null) {
            for (ObjectReferenceType ort : originalObject.asObjectable().getParentOrgRef()) {
                retval.add(ort.getOid());
            }
            // is this really necessary?
            for (OrgType org : originalObject.asObjectable().getParentOrg()) {
                retval.add(org.getOid());
            }
        }
        return retval;
    }

    // only for performance testing (doesn't account for multithreading!)
    public long getLastOperationDuration() {
        return lastOperationDuration;
    }

    public static enum StartupAction {

        NONE("none"), CHECK("check"), REBUILD_IF_NEEDED("rebuidIfNeeded"), ALWAYS_REBUILD("alwaysRebuild");

        private String value;

        StartupAction(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static StartupAction fromValue(String v) {
            for (StartupAction a: StartupAction.values()) {
                if (a.value.equals(v)) {
                    return a;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }

    public static enum OrgClosureObjects {

        NONE("none"), FOCUS("focus"), ALL("all");

        private String value;

        OrgClosureObjects(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static OrgClosureObjects fromValue(String v) {
            for (OrgClosureObjects a: OrgClosureObjects.values()) {
                if (a.value.equals(v)) {
                    return a;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }
}
