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

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.hibernate.Query;
import org.hibernate.Session;

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
        private Edge(String descendant, String ancestor) {
            this.descendant = descendant;
            this.ancestor = ancestor;
        }
        public String getDescendant() {
            return descendant;
        }
        public void setDescendant(String descendant) {
            this.descendant = descendant;
        }
        public String getAncestor() {
            return ancestor;
        }
        public void setAncestor(String ancestor) {
            this.ancestor = ancestor;
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
    }

    public OrgClosureManager(SqlRepositoryConfiguration repoConfiguration) {
        this.repoConfiguration = repoConfiguration;
    }

    // only for performance testing
    long lastOperationDuration;

    public <T extends ObjectType> void updateOrgClosure(Collection<? extends ItemDelta> modifications, Session session,
                                                        String oid, Class<T> type, Operation operation) {
        session.flush();
        session.clear();

        long time = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("################# Starting {} for org. closure for {} oid={}.", new Object[]{operation, type.getSimpleName(), oid});
        }

        List<ReferenceDelta> deltas = filterParentRefDeltas(modifications);

        switch (operation) {
            case ADD:
                Set<String> parents = getOidFromAddDeltas(deltas);
                handleAdd(oid, parents, type, session);
                break;
            case DELETE:
                handleDelete(oid, type, session);
                break;
            case MODIFY:
                handleModify(deltas, session, oid, type);
        }

        long duration = System.currentTimeMillis() - time;
        LOGGER.debug("################# Org. closure update finished in {} ms.", duration);
        lastOperationDuration = duration;
    }

    //region Handling ADD operation

    private <T extends ObjectType> void handleAdd(String oid, Set<String> parents, Class<T> type, Session session) {

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

        if (parents.size() <= 1 && (livingChildren == null || livingChildren.isEmpty())) {
            String parent;
            if (parents.isEmpty()) {
                parent = null;
            } else {
                parent = parents.iterator().next();
            }
            addEdgeSimple(oid, parent, session);
        } else {
            addParentEdges(oid, retainExistingOids(parents, session), session);
        }
    }

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

    private void addParentEdges(String oid, List<String> parents, Session session) {
        List<Edge> edges = parentsToEdges(oid, parents);
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

    private void addIndependentEdges(List<Edge> edges, Session session) {

        long start = System.currentTimeMillis();
        LOGGER.trace("===================== ADD INDEPENDENT EDGES: {} ================", edges);

        if (!edges.isEmpty()) {
            String deltaTempTableName = computeDeltaTable(edges, session);
            try {
                int count;

                if (isMySQL() || isOracle()) {

                    long startUpsert = System.currentTimeMillis();
                    String upsertQueryText;

                    if (isMySQL()) {
                        upsertQueryText = "insert into " + closureTableName + " (descendant_oid, ancestor_oid, val) " +
                                "select descendant_oid, ancestor_oid, val from " + deltaTempTableName + " delta " +
                                "on duplicate key update " + closureTableName + ".val = " + closureTableName + ".val + values(val)";
                    } else {
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

        LOGGER.trace("--------------------- DONE ADD EDGES: {} ({} ms) ----------------", edges, System.currentTimeMillis()-start);
    }

    private void dropDeltaTableIfNecessary(Session session, String deltaTempTableName) {
        // postgresql deletes the table automatically on commit
        // TODO what in case of H2?
        if (isMySQL()) {
            Query dropQuery = session.createSQLQuery("drop temporary table " + deltaTempTableName);
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

        List<String> children = getChildren(oid, session);
        if (children.isEmpty()) {
            handleDeleteLeaf(oid, session);
            return;
        }

        // delete all edges "<child> -> OID" from the closure
        removeChildrenEdges(oid, children, session);
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'child' links.", children.size());

        // delete all edges "OID -> <parent>" from the closure
        List<String> parents = getParents(oid, session);
        removeParentEdges(oid, parents, session);               // todo check for parents' existence?
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'parent' links.", parents.size());

        // delete (OID, OID) record
        Query deleteSelfQuery = session.createSQLQuery("delete from "+closureTableName+" " +
                "where descendant_oid=:oid and ancestor_oid=:oid");
        deleteSelfQuery.setString("oid", oid);
        int count = deleteSelfQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} self-record from closure table.", count);
    }

    private <T extends ObjectType> void handleDeleteLeaf(String oid, Session session) {
        Query removeFromClosureQuery = session.createSQLQuery(
                "delete from "+closureTableName+" " +
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
                    deleteFromClosureQueryText = "delete from " + closureTableName + " " +
                            "where (descendant_oid, ancestor_oid, val) in " +
                            "(select (descendant_oid, ancestor_oid, val) from " + deltaTempTableName + ")";
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
                } else if (isMySQL()) {
                    // http://stackoverflow.com/questions/652770/delete-with-join-in-mysql
                    // TODO consider this for postgresql/h2 as well
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


    private boolean isH2() {
        return repoConfiguration.isUsingH2();
    }

    private boolean isPostgreSQL() {
        return repoConfiguration.isUsingPostgreSQL();
    }
    //endregion

    //region Handling MODIFY

    private <T extends ObjectType> void handleModify(Collection<? extends ItemDelta> modifications, Session session,
                                                     String oid, Class<T> type) {
        if (modifications.isEmpty()) {
            return;
        }

        boolean maybeNonLeaf = isTypeNonLeaf(type);

        Set<String> parentsToDelete = getOidFromDeleteDeltas(modifications);
        Set<String> parentsToAdd = getOidFromAddDeltas(modifications);

        parentsToDelete.removeAll(parentsToAdd);            // if something is deleted and the re-added we can skip this operation

        removeParentEdges(oid, parentsToDelete, session);
        addParentEdges(oid, retainExistingOids(parentsToAdd, session), session);
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
        Query childrenQuery = session.createQuery("select distinct ownerOid from RParentOrgRef where targetOid=:oid");
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

        if (!isOracle()) {
            deltaTempTableName = "m_org_closure_delta_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        } else {
            deltaTempTableName = "m_org_closure_temp_delta";            // table definition is global
        }

        if (COUNT_CLOSURE_RECORDS && LOGGER.isTraceEnabled()) {
            Query q = session.createSQLQuery("select count(*) from " + closureTableName);
            List list = q.list();
            LOGGER.trace("OrgClosure has {} rows", list.toString());
        }

        long start = System.currentTimeMillis();
        String createTablePrefix;
        if (isH2()) {
            createTablePrefix = "create cached local temporary " + deltaTempTableName + " on commit drop as";
        } else if (isPostgreSQL()) {
            createTablePrefix = "create temporary table " + deltaTempTableName + " as ";
        } else if (isMySQL()) {
            createTablePrefix = "create temporary table " + deltaTempTableName + " engine=memory as ";
        } else if (isOracle()) {
            // todo skip if this is first in this transaction
            Query q = session.createSQLQuery("delete from " + deltaTempTableName);
            int count = q.executeUpdate();
            LOGGER.trace("Deleted {} rows from temporary table {}", count, deltaTempTableName);
            createTablePrefix = "insert into " + deltaTempTableName;
        } else {
            throw new UnsupportedOperationException("define other databases");
        }
        // t1.ancestor_oid = :tail and t2.descendant_oid = :head
        StringBuilder whereClause = new StringBuilder();
        boolean first = true;
        for (Edge edge : edges) {
            if (first) {
                first = false;
            } else {
                whereClause.append(" or ");
            }
            whereClause.append("(t1.ancestor_oid = '").append(edge.getTail()).append("'");
            whereClause.append("and t2.descendant_oid = '").append(edge.getHead()).append("')");
        }
        Query query1 = session.createSQLQuery(
                        createTablePrefix + " " +
                        "select t1.descendant_oid as descendant_oid, t2.ancestor_oid as ancestor_oid, " +
                               "sum(t1.val*t2.val) as val " +
                        "from "+closureTableName+" t1, "+closureTableName+" t2 " +
                        "where " + whereClause.toString() + " " +
                        "group by t1.descendant_oid, t2.ancestor_oid");
        int count = query1.executeUpdate();

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
        // TODO index for MySQL!!!

        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, closureTableName);
        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, deltaTempTableName);

        // TODO drop delta table in case of exception

        return deltaTempTableName;
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

    private Set<String> getOidFromDeleteDeltas(Collection<? extends ItemDelta> modifications) {
        Set<String> oids = new HashSet<>();

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToDelete() == null) {
                continue;
            }
            for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToDelete()) {
                oids.add(val.getOid());
            }
        }

        return oids;
    }

    private Set<String> getOidFromAddDeltas(Collection<? extends ItemDelta> modifications) {
        Set<String> oids = new HashSet<>();

        for (ItemDelta delta : modifications) {
            if (delta.getValuesToAdd() == null) {
                continue;
            }
            for (PrismReferenceValue val : (Collection<PrismReferenceValue>) delta.getValuesToAdd()) {
                oids.add(val.getOid());
            }
        }

        return oids;
    }

    // only for performance testing (doesn't account for multithreading!)
    public long getLastOperationDuration() {
        return lastOperationDuration;
    }

}
