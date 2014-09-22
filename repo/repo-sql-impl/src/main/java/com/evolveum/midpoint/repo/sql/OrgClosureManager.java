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
import com.evolveum.midpoint.repo.sql.util.RUtil;
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
 * namely from Chapter 6 (http://vadimtropashko.files.wordpress.com/2014/01/book_sql_chap6_v1.pdf)
 *
 * @author lazyman
 * @author mederly
 */
public class OrgClosureManager {

    private boolean postgresql = true;

    public static enum Operation {ADD, DELETE, MODIFY}

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureManager.class);

    private static boolean DUMP_TABLES = false;

    private SqlRepositoryConfiguration repoConfiguration;

    public OrgClosureManager(SqlRepositoryConfiguration repoConfiguration) {
        this.repoConfiguration = repoConfiguration;
    }

    public <T extends ObjectType> void updateOrgClosure(Collection<? extends ItemDelta> modifications, Session session,
                                                        String oid, Class<T> type, Operation operation) {
        session.flush();
        session.clear();
        long time = System.currentTimeMillis();
        LOGGER.debug("Starting {} for org. closure for {} oid={}.", new Object[]{operation, type.getSimpleName(), oid});

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

        LOGGER.debug("Org. closure update finished in {}ms.", (System.currentTimeMillis() - time));
    }

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
        for (String childOid : children) {
            removeEdge(childOid, oid, session);
        }
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'child' links.", children.size());

        // delete all edges "OID -> <parent>" from the closure
        List<String> parents = getParents(oid, session);
        removeEdges(oid, new HashSet<>(parents), maybeNonLeaf, session);
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'parent' links.", parents.size());

        // delete (OID, OID) record
        Query deleteSelfQuery = session.createSQLQuery("delete from m_org_closure " +
                "where descendant_oid=:oid and ancestor_oid=:oid");
        deleteSelfQuery.setString("oid", oid);
        int count = deleteSelfQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} self-record from m_org_closure table.", count);
    }

    private <T extends ObjectType> void handleDeleteLeaf(String oid, Session session) {
        Query removeFromClosureQuery = session.createSQLQuery(
                "delete from M_ORG_CLOSURE " +
                        "where descendant_oid = :oid");
        removeFromClosureQuery.setString("oid", oid);
        int count = removeFromClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("DeleteLeaf: Removed {} records from closure table.", count);
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
            Query query = session.createQuery("select o.oid from RObject o where o.oid in " + (postgresql ? ":oids" : "(:oids)"));
            query.setParameterList("oids", oids);
            return query.list();
        } else {
            return new ArrayList<String>();
        }
    }

    private <T extends ObjectType> void handleAdd(String oid, Set<String> parents, Class<T> type, Session session) {

        // adding self-record
        session.save(new ROrgClosure(oid, oid, 1));
        session.flush();
        session.clear();

        boolean maybeNonLeaf = isTypeNonLeaf(type);

        if (maybeNonLeaf) {
            List<String> livingChildren = getChildren(oid, session);        // no need to check existence of these oids, as owner is a FK pointing to RObject in RParentRef
            for (String child : livingChildren) {
                addEdge(child, oid, maybeNonLeaf, session);
            }
        }

        addEdges(oid, retainExistingOids(parents, session), maybeNonLeaf, session);
    }

    private <T extends ObjectType> boolean isTypeNonLeaf(Class<T> type) {
        return OrgType.class.equals(type);
    }

    private <T extends ObjectType> void handleModify(Collection<? extends ItemDelta> modifications, Session session,
                                                     String oid, Class<T> type) {
        if (modifications.isEmpty()) {
            return;
        }

        boolean maybeNonLeaf = isTypeNonLeaf(type);

        // TODO optimize this!

        Set<String> parents = getOidFromDeleteDeltas(modifications);
        removeEdges(oid, parents, maybeNonLeaf, session);

        parents = getOidFromAddDeltas(modifications);
        addEdges(oid, retainExistingOids(parents, session), maybeNonLeaf, session);
    }

    private void removeEdges(String oid, Set<String> parents, boolean maybeNonLeaf, Session session) {

//        if (!maybeNonLeaf) {
//            // very simple case: we only remove a few entries from the closure table
//            Query removeFromClosureQuery = session.createSQLQuery(
//                    "delete from M_ORG_CLOSURE " +
//                            "where descendant_oid = :oid and ancestor_oid IN (:parents) ");
//            removeFromClosureQuery.setString("oid", oid);
//            removeFromClosureQuery.setParameterList("parents", parents);
//            int count = removeFromClosureQuery.executeUpdate();
//            if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} records from closure table.", count);
//        } else {
            // generalized version (quite expensive)
            for (String parent : parents) {
                removeEdge(oid, parent, session);
            }
//        }
    }

    // expects that all parents are really existing
    private void addEdges(String oid, Collection<String> parents, boolean maybeNonLeaf, Session session) {

        if (!maybeNonLeaf && parents.size() <= 1) {
            // very simple case: we only add a few entries to the closure table
            if (!parents.isEmpty()) {
                long start = System.currentTimeMillis();
                Query addToClosureQuery = session.createSQLQuery(
                        "insert into M_ORG_CLOSURE (descendant_oid, ancestor_oid, val) " +
                                "select :oid as descendant_oid, M_ORG_CLOSURE.ancestor_oid as ancestor_oid, 1 as val " +
                                "from M_ORG_CLOSURE " +
                                "where M_ORG_CLOSURE.descendant_oid IN " + (postgresql ? ":parents" : "(:parents) "));
                addToClosureQuery.setString("oid", oid);
                addToClosureQuery.setParameterList("parents", parents);
                int count = addToClosureQuery.executeUpdate();
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("addEdges simplified: Added {} records to closure table ({} ms).", count,
                            System.currentTimeMillis() - start);
            }
            session.flush();
            session.clear();
        } else {
            // general case
            for (String parent : parents) {
                addEdge(oid, parent, maybeNonLeaf, session);
            }
        }
    }

    // "tail" is child, "head" is parent
    private void addEdge(String tail, String head, boolean maybeNonLeaf, Session session) {

        long start = System.currentTimeMillis();
        LOGGER.trace("===================== ADD EDGE: {} -> {} ================", tail, head);

        String deltaTempTableName = "m_org_closure_delta_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        computeDeltaTable(tail, head, session, deltaTempTableName);
        int count;

        long startUpdate = System.currentTimeMillis();
        Query updateInClosureQuery = session.createSQLQuery(
                postgresql ?
                        "update M_ORG_CLOSURE " +
                                "set val = val + (select val from " + deltaTempTableName + " td " +
                                                            "where td.descendant_oid=m_org_closure.descendant_oid and td.ancestor_oid=m_org_closure.ancestor_oid) " +
                                "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from " + deltaTempTableName + ")"
                    :
                        "update M_ORG_CLOSURE " +
                                "set val = val + (select val from " + deltaTempTableName + " td " +
                                                            "where td.descendant_oid=m_org_closure.descendant_oid and td.ancestor_oid=m_org_closure.ancestor_oid) " +
                                "where (descendant_oid, ancestor_oid) in (select (descendant_oid, ancestor_oid) from " + deltaTempTableName + ")");
        int countUpdate = updateInClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Updated {} records to closure table ({} ms)", countUpdate, System.currentTimeMillis()-startUpdate);

        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, "m_org_closure");

        long startAdd = System.currentTimeMillis();
        String addQuery =
                "insert into M_ORG_CLOSURE (descendant_oid, ancestor_oid, val) " +
                        "select descendant_oid, ancestor_oid, val from " + deltaTempTableName;
        if (countUpdate > 0) {
            addQuery += postgresql ?
                    " where (descendant_oid, ancestor_oid) not in (select descendant_oid, ancestor_oid from m_org_closure)" :
                    " where (descendant_oid, ancestor_oid) not in (select (descendant_oid, ancestor_oid) from m_org_closure)";
        }
        Query addToClosureQuery = session.createSQLQuery(addQuery);
        count = addToClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to closure table ({} ms)", count, System.currentTimeMillis()-startAdd);

        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, "m_org_closure");

        session.flush();
        session.clear();

        LOGGER.trace("--------------------- DONE ADD EDGE: {} -> {} ({} ms) ----------------", new Object[]{tail, head, System.currentTimeMillis()-start});
    }

    private void computeDeltaTable(String tail, String head, Session session, String deltaTempTableName) {

        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        List list = q.list();
        LOGGER.trace("OrgClosure has {} rows", list.toString());

        long start = System.currentTimeMillis();
        Query query1 = session.createSQLQuery(
                (postgresql ?
                    "create temporary " :
                    "create cached local temporary ") + "table " + deltaTempTableName +

                        (postgresql ? " on commit drop " : "") +

                        " as " +
                        "select t1.descendant_oid as descendant_oid, t2.ancestor_oid as ancestor_oid, " +
                               "sum(t1.val*t2.val) as val " +
                        "from m_org_closure t1, m_org_closure t2 " +
                        "where t1.ancestor_oid = :tail and t2.descendant_oid = :head " +
                        "group by t1.descendant_oid, t2.ancestor_oid");
        query1.setString("tail", tail);
        query1.setString("head", head);
        int count = query1.executeUpdate();

        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary delta table {} ({} ms).",
                new Object[] {count, deltaTempTableName, System.currentTimeMillis()-start});

        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, "m_org_closure");
        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, deltaTempTableName);
    }

    private void dumpOrgClosureTypeTable(Session session, String tableName) {
        Query q = session.createSQLQuery("select descendant_oid, ancestor_oid, val from " + tableName);
        List<Object[]> list = q.list();
        LOGGER.trace("{} ({} rows):", tableName, list.size());
        for (Object[] row : list) {
            LOGGER.trace(" - [d={}, a={}, val={}]", row);
        }
    }

    // "tail" is child, "head" is parent
    private void removeEdge(String tail, String head, Session session) {

        long start = System.currentTimeMillis();
        LOGGER.trace("===================== REMOVE EDGE: {} -> {} ================", tail, head);

        String deltaTempTableName = "m_org_closure_delta_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        computeDeltaTable(tail, head, session, deltaTempTableName);
        int count;

        Query deleteFromClosureQuery = session.createSQLQuery(
                postgresql ?
                        "delete from M_ORG_CLOSURE " +
                                "where (descendant_oid, ancestor_oid, val) in " +
                                        "(select descendant_oid, ancestor_oid, val from " + deltaTempTableName + ")"
                        :
                        "delete from M_ORG_CLOSURE " +
                                "where (descendant_oid, ancestor_oid, val) in " +
                                   "(select (descendant_oid, ancestor_oid, val) from " + deltaTempTableName + ")");
        count = deleteFromClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} records from closure table.", count);
        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, "m_org_closure");

        Query updateInClosureQuery = session.createSQLQuery(
                postgresql ?
                        "update M_ORG_CLOSURE " +
                                "set val = val - (select val from " + deltaTempTableName + " td " +
                                                    "where td.descendant_oid=m_org_closure.descendant_oid and td.ancestor_oid=m_org_closure.ancestor_oid) " +
                                "where (descendant_oid, ancestor_oid) in (select descendant_oid, ancestor_oid from "+deltaTempTableName+")"
                :
                "update M_ORG_CLOSURE " +
                        "set val = val - (select val from " + deltaTempTableName + " td " +
                                          "where td.descendant_oid=m_org_closure.descendant_oid and td.ancestor_oid=m_org_closure.ancestor_oid) " +
                        "where (descendant_oid, ancestor_oid) in (select (descendant_oid, ancestor_oid) from "+deltaTempTableName+")");
        count = updateInClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Updated {} records in closure table.", count);
        if (DUMP_TABLES) dumpOrgClosureTypeTable(session, "m_org_closure");
        session.flush();
        session.clear();

        LOGGER.trace("--------------------- DONE REMOVE EDGE: {} -> {} ({} ms) ----------------", new Object[]{tail, head, System.currentTimeMillis()-start});
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



}
