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

import ch.qos.logback.classic.db.names.TableName;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.*;

/**
 * This class and its subclasses provides org. closure table handling.
 *
 * Data structures used are:
 *
 *  (1) Repo object graph G = (V, E) where V is a set of vertices (repo objects) and E is a set of edges (parentRef relations).
 *      There is an edge e = (V1, V2) in E [i.e. edge from V1 to V2] if and only if V1.parentRef contains V2 [i.e. V2 is a parent of V1].
 *
 *  (2) OrgClosure binary relation. OrgClosure(D, A) iff there is a path in object graph from D (descendant) to A (ascendant).
 *      This path should be of length at least 0.
 *
 * @author lazyman
 * @author mederly
 */
public class OrgClosureManager {

    public static enum Operation {ADD, DELETE, MODIFY}

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureManager.class);

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

        if (maybeNonLeaf) {
            // delete all edges (<child> -> OID) from the closure
            List<String> children = getChildren(oid, session);
            for (String childOid : children) {
                removeEdge(childOid, oid, session);
            }
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'child' links.", children.size());

            // we know there are no children, so the removal may be much quicker
            if (children.isEmpty()) {
                maybeNonLeaf = false;
            }
        }

        // delete all edges (OID -> <parent>) from the closure
        List<String> parents = getParents(oid, session);
        removeEdges(oid, new HashSet<>(parents), maybeNonLeaf, session);
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} 'parent' links.", parents.size());

        // delete (OID, OID) record TODO remove if unnecessary
        Query deleteSelfQuery = session.createSQLQuery("delete from m_org_closure " +
                "where descendant_oid=:oid and ancestor_oid=:oid");
        deleteSelfQuery.setString("oid", oid);
        int count = deleteSelfQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} self-record from m_org_closure table.", count);

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
        Query query = session.createQuery("select o.oid from RObject o where o.oid in (:oids)");
        query.setParameterList("oids", oids);
        return query.list();
    }

    private <T extends ObjectType> void handleAdd(String oid, Set<String> parents, Class<T> type, Session session) {

        // adding self-record TODO remove if unnecessary
        session.save(new ROrgClosure(oid, oid));
        session.flush();
        session.clear();

        boolean maybeNonLeaf = isTypeNonLeaf(type);

        if (maybeNonLeaf) {
            List<String> livingChildren = retainExistingOids(getChildren(oid, session), session);
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

        if (!maybeNonLeaf) {
            // very simple case: we only remove a few entries from the closure table
            Query removeFromClosureQuery = session.createSQLQuery(
                    "delete from M_ORG_CLOSURE " +
                            "where descendant_oid = :oid and ancestor_oid IN (:parents) ");
            removeFromClosureQuery.setString("oid", oid);
            removeFromClosureQuery.setParameterList("parents", parents);
            int count = removeFromClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} records from closure table.", count);
        } else {
            // generalized version (quite expensive)
            for (String parent : parents) {
                removeEdge(oid, parent, session);
            }
        }
    }

    // expects that all parents are really existing
    private void addEdges(String oid, Collection<String> parents, boolean maybeNonLeaf, Session session) {

        if (!maybeNonLeaf) {
            // very simple case: we only add a few entries to the closure table
            Query addToClosureQuery = session.createSQLQuery(
                    "insert into M_ORG_CLOSURE (descendant_oid, ancestor_oid) " +
                            "select :oid as descendant, M_ORG_CLOSURE.ancestor_oid as ancestor " +
                            "from M_ORG_CLOSURE " +
                            "where M_ORG_CLOSURE.descendant_oid IN (:parents) ");
            addToClosureQuery.setString("oid", oid);
            addToClosureQuery.setParameterList("parents", parents);
            int count = addToClosureQuery.executeUpdate();
            if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to closure table.", count);
            session.flush();
            session.clear();
        } else {
            // general case
            for (String parent : parents) {
                addEdge(oid, parent, maybeNonLeaf, session);
            }
        }
    }

    // "a" is child, "b" is parent
    private void addEdge(String a, String b, boolean maybeNonLeaf, Session session) {

        String tempTableName = "m_org_closure_temp_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        String deltaTempTableName = "m_org_closure_delta_temp_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        Query query1 = session.createSQLQuery(
                "create cached local temporary table " + tempTableName + " as select * from (" +
                        "select M_ORG_CLOSURE.descendant_oid as descendant, :b as ancestor " +
                        "from M_ORG_CLOSURE " +
                        "where M_ORG_CLOSURE.ancestor_oid = :a " +
                     "UNION " +
                        "select :a as descendant, M_ORG_CLOSURE.ancestor_oid as ancestor " +
                        "from M_ORG_CLOSURE " +
                        "where M_ORG_CLOSURE.descendant_oid = :b " +
                     "UNION " +
                        "select M_ORG_CLOSURE_1.descendant_oid as descendant, M_ORG_CLOSURE_2.ancestor_oid as ancestor  " +
                        "from M_ORG_CLOSURE as M_ORG_CLOSURE_1, M_ORG_CLOSURE as M_ORG_CLOSURE_2 " +
                        "where M_ORG_CLOSURE_1.ancestor_oid = :a and M_ORG_CLOSURE_2.descendant_oid = :b " +
                ")");
        query1.setString("a", a);
        query1.setString("b", b);
        int count = query1.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary table {}.", count, tempTableName);

        // TODO put query2 here if self-records are not used

        Query deltaQuery = session.createSQLQuery(
                "create cached local temporary table " + deltaTempTableName + " as select descendant, ancestor from " + tempTableName + " as T " +
                "where not exists (select M_ORG_CLOSURE.descendant_oid as descendant, M_ORG_CLOSURE.ancestor_oid as ancestor " +
                                  "from M_ORG_CLOSURE " +
                                  "where M_ORG_CLOSURE.descendant_oid = descendant and M_ORG_CLOSURE.ancestor_oid = ancestor) "
                );
        count = deltaQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary delta table {}.", count, deltaTempTableName);

        Query addToClosureQuery = session.createSQLQuery(
                "insert into M_ORG_CLOSURE (descendant_oid, ancestor_oid)" +
                        "select descendant, ancestor from " + deltaTempTableName);
        count = addToClosureQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to closure table.", count);
        session.flush();
        session.clear();
    }

    private void removeEdge(String oid, String parent, Session session) {

        String suspectsTableName = "m_org_closure_suspects_temp_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        String trustyTableName = "m_org_closure_trusty_temp_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        String tcNewTableName = "m_org_closure_tc_new_temp_" + System.currentTimeMillis() + "_" + ((int) (Math.random() * 10000000.0));
        /*
         *  Table SUSPECT contains a (x, y) pair iff there is a path from x to y (in original graph) that goes through oid->parent edge
         */
        long start = System.currentTimeMillis();
        Query suspectsQuery = session.createSQLQuery(
// version when (X,X) is not in closure
//                "create local temporary table " + suspectsTableName + " as select * from (" +
//                        "select M_ORG_CLOSURE_1.descendant_oid as descendant, M_ORG_CLOSURE_2.ancestor_oid as ancestor " +
//                        "from M_ORG_CLOSURE as M_ORG_CLOSURE_1, M_ORG_CLOSURE as M_ORG_CLOSURE_2 " +
//                        "where M_ORG_CLOSURE_1.ancestor_oid = :oid and M_ORG_CLOSURE_2.descendant_oid = :parent " +
//                    "union " +
//                        "select M_ORG_CLOSURE.descendant_oid as descendant, :parent as ancestor " +
//                        "from M_ORG_CLOSURE " +
//                        "where M_ORG_CLOSURE.ancestor_oid = :oid " +
//                    "union " +
//                        "select :oid as descendant, M_ORG_CLOSURE.ancestor_oid as ancestor " +
//                        "from M_ORG_CLOSURE " +
//                        "where M_ORG_CLOSURE.descendant_oid = :parent " +
//                    "union " +
//                        "select :oid as descendant, :parent as ancestor " +
//                        "from M_ORG_CLOSURE " +
//                        "where M_ORG_CLOSURE.descendant_oid = :oid and M_ORG_CLOSURE.ancestor_oid = :parent" +
//                        ")");

                // version when (X,X) are in closure
                "create cached local temporary table " + suspectsTableName + " as select * from (" +
                        "select M_ORG_CLOSURE_1.descendant_oid as descendant, M_ORG_CLOSURE_2.ancestor_oid as ancestor " +
                        "from M_ORG_CLOSURE as M_ORG_CLOSURE_1, M_ORG_CLOSURE as M_ORG_CLOSURE_2 " +
                        "where M_ORG_CLOSURE_1.ancestor_oid = :oid and M_ORG_CLOSURE_2.descendant_oid = :parent " +
                        ")");

        suspectsQuery.setString("oid", oid);
        suspectsQuery.setString("parent", parent);
        int count = suspectsQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary table {} in {}ms.", new Object[]{count, suspectsTableName, System.currentTimeMillis()-start});

        start = System.currentTimeMillis();
        Query suspectsIdxQuery = session.createSQLQuery("create index " + suspectsTableName + "_idx on " + suspectsTableName + " (descendant, ancestor)");
        suspectsIdxQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Created index on temporary table {} in {}ms.", suspectsTableName, System.currentTimeMillis()-start);

        /*
         *  Table TRUSTY contains paths that are surely unaffected by the deletion of oid->parent edge.
         */
        start = System.currentTimeMillis();
        Query trustyQuery = session.createSQLQuery(
                "create cached local temporary table " + trustyTableName + " as select * from (" +
                        "select M_ORG_CLOSURE.descendant_oid as descendant, M_ORG_CLOSURE.ancestor_oid as ancestor " +
                        "from M_ORG_CLOSURE " +
                        "where not exists (select descendant, ancestor from " + suspectsTableName + " as SUSPECT " +
                        "                  where SUSPECT.descendant = M_ORG_CLOSURE.descendant_oid " +
                                                 "and SUSPECT.ancestor = M_ORG_CLOSURE.ancestor_oid) " +
                     "union " +
                        "select owner_oid as descendant, targetOid as ancestor " +
                        "from m_reference " +
                        "where m_reference.owner_oid <> :oid and m_reference.targetOid <> :parent and reference_type=0)");
        trustyQuery.setString("oid", oid);
        trustyQuery.setString("parent", parent);
        count = trustyQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary table {} in {}ms.", new Object[]{count, trustyTableName, System.currentTimeMillis()-start});

        start = System.currentTimeMillis();
        Query trustyIdx1Query = session.createSQLQuery("create index " + trustyTableName + "_idx1 on " + trustyTableName + " (descendant, ancestor)");
        trustyIdx1Query.executeUpdate();
        Query trustyIdx2Query = session.createSQLQuery("create index " + trustyTableName + "_idx2 on " + trustyTableName + " (descendant)");
        trustyIdx2Query.executeUpdate();
        Query trustyIdx3Query = session.createSQLQuery("create index " + trustyTableName + "_idx3 on " + trustyTableName + " (ancestor)");
        trustyIdx3Query.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Created indices on temporary table {} in {}ms.", trustyTableName, System.currentTimeMillis()-start);

        /*
         *  New closure will contain
         *   (1) all TRUSTY paths
         *   (2) all paths constructed by concatenating 2 consecutive TRUSTY paths
         *   (3) all paths constructed by concatenating 3 consecutive TRUSTY paths
         */
        start = System.currentTimeMillis();
        Query tcNewQuery = session.createSQLQuery(
                "create cached local temporary table " + tcNewTableName + " as select * from (" +
                        "select * from " + trustyTableName + " " +
                    "union " +
                        "select T1.descendant, T2.ancestor " +
                        "from " + trustyTableName + " T1, " + trustyTableName + " T2 " +
                        "where T1.ancestor = T2.descendant " +
                    "union " +
                        "select T1.descendant, T3.ancestor " +
                        "from " + trustyTableName + " T1, " + trustyTableName + " T2, " + trustyTableName + " T3 " +
                        "where T1.ancestor = T2.descendant and T2.ancestor = T3.descendant)");
        count = tcNewQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Added {} records to temporary table {} in {}ms.", new Object[]{count, tcNewTableName, System.currentTimeMillis()-start});

        start = System.currentTimeMillis();
        Query tcNewIdxQuery = session.createSQLQuery("create index " + tcNewTableName + "_idx on " + tcNewTableName+ " (descendant, ancestor)");
        tcNewIdxQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Created index on temporary table {} in {}ms.", tcNewTableName, System.currentTimeMillis()-start);

        /*
         *  Now remove all edges from TC that should not be there.
         */
        start = System.currentTimeMillis();
        Query purgeQuery = session.createSQLQuery(
                "delete from M_ORG_CLOSURE " +
                "where not exists (select * from " + tcNewTableName + " as T " +
                                  "where T.descendant = M_ORG_CLOSURE.descendant_oid and T.ancestor = M_ORG_CLOSURE.ancestor_oid)");
        count = purgeQuery.executeUpdate();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Removed {} records from closure table in {}ms.", new Object[]{count, System.currentTimeMillis()-start});
    }

    private void bulkSave(List objects, Session session) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        LOGGER.trace("Bulk saving {} objects {}", objects.size(), objects.get(0).getClass().getSimpleName());

        for (int i = 0; i < objects.size(); i++) {
            LOGGER.trace("{}", objects.get(i));     //todo delete

            session.save(objects.get(i));
            if (i > 0 && i % RUtil.JDBC_BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }
        }
        session.flush();
        session.clear();
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

    /***********************************************************************/
    /**
     * OLD STUFF *
     */
    private Map<String, Set<String>> createDescendantAncestorsMap(List<String[]> ocList) {
        Map<String, Set<String>> descAncMap = new HashMap<>();
        for (String[] array : ocList) {
            Set<String> ancestors = descAncMap.get(array[1]);
            if (ancestors == null) {
                ancestors = new HashSet<>();
                descAncMap.put(array[1], ancestors);
            }
            ancestors.add(array[0]);
        }

        return descAncMap;
    }

    /**
     * This method copies data from incorrect table to closure table. Firstly it loads all descendant oids
     * (from records which ancestorOid points to currently adding object), then:
     * 1/ add them to clsure table like (oid, descendant)
     * 2/ do a "cross join" (cartesian product) on ancestors (currently adding, method parameter) and previously
     *      selected descendants, to fix all transient parent-child relationship.
     * 3/ deletes all records which were created for "incorrect" (ancestorOid=oid)
     *
     * @param oid identifier of object we're adding
     * @param newClosureAncestors ancestors currently adding to org. closure table based on parents
     * @param session
     * @return new closure records
     */
    private List<ROrgClosure> addClosuresFromIncorrects(String oid, List<String> newClosureAncestors, Session session) {
        List<ROrgClosure> closures = new ArrayList<>();
        //this can be probably improved by some insert select with union all
        Query query = session.createQuery("select descendantOid from ROrgIncorrect where ancestorOid=:oid");
        query.setString("oid", oid);
        List<String> descendants = query.list();
        if (LOGGER.isTraceEnabled()) LOGGER.trace("Found {} descendants for oid {}. {}",
                new Object[]{descendants.size(), oid, Arrays.toString(descendants.toArray())});

        if (!descendants.isEmpty()) {
            for (String descendant : descendants) {
                closures.add(new ROrgClosure(oid, descendant));
                for (String ancestor : newClosureAncestors) {
                    closures.add(new ROrgClosure(ancestor, descendant));
                }
            }

            query = session.createQuery("delete from ROrgIncorrect where ancestorOid=:oid");
            query.setString("oid", oid);
            query.executeUpdate();
        }

        return closures;
    }

    private void overwriteAddObjectAttempt(Collection<? extends ItemDelta> modifications, RObject merged,
                                           Session session, ObjectType objectType) throws DtoTranslationException, SchemaException {
        //update org. unit hierarchy based on modifications
        if (modifications == null || modifications.isEmpty()) {
            //we're not overwriting object - we fill new hierarchy
            if (objectType instanceof OrgType || !objectType.getParentOrgRef().isEmpty()) {
                long time = System.currentTimeMillis();
                LOGGER.trace("Org. structure closure table update started.");
                objectType.setOid(merged.getOid());
                fillHierarchy(merged, session, true);
                LOGGER.trace("Org. structure closure table update finished ({} ms).",
                        new Object[]{(System.currentTimeMillis() - time)});
            }
        } else {
            //we have to recompute actual hierarchy because we've changed object
            recomputeHierarchy(merged, session, modifications);
        }
    }

    private <T extends ObjectType> void nonOverwriteAddObjectAttempt(ObjectType objectType, RObject rObject, String oid,
                                                                     Session session) throws DtoTranslationException, SchemaException {

        if (objectType instanceof OrgType || !objectType.getParentOrgRef().isEmpty()) {
            long time = System.currentTimeMillis();
            LOGGER.trace("Org. structure closure table update started.");
            objectType.setOid(oid);
            fillHierarchy(rObject, session, true);
            LOGGER.trace("Org. structure closure table update finished ({} ms).",
                    new Object[]{(System.currentTimeMillis() - time)});
        }
    }

    private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, RObject object, String oid, Session session)
            throws DtoTranslationException, SchemaException {
        List<RObject> objectsToRecompute = null;
        if (type.isAssignableFrom(OrgType.class)) {
            objectsToRecompute = deleteTransitiveHierarchy(object, session);
        }

        if (objectsToRecompute != null) {
            recompute(objectsToRecompute, session);
        }
    }

    private boolean existOrgCLosure(Session session, String ancestorOid, String descendantOid) {
        // if not exist pair with same depth, then create else nothing do
        Query qExistClosure = session.getNamedQuery("existOrgClosure");
        qExistClosure.setParameter("ancestorOid", ancestorOid);
        qExistClosure.setParameter("descendantOid", descendantOid);

        return (Long) qExistClosure.uniqueResult() != 0;

    }

    private boolean existIncorrect(Session session, String ancestorOid, String descendantOid) {
        // if not exist pair with same depth, then create else nothing do
        Query qExistIncorrect = session.getNamedQuery("existIncorrect");
        qExistIncorrect.setParameter("ancestorOid", ancestorOid);
        qExistIncorrect.setParameter("descendantOid", descendantOid);

        return (Long) qExistIncorrect.uniqueResult() != 0;
    }

    private <T extends ObjectType> void fillHierarchy(RObject<T> rOrg, Session session, boolean withIncorrect)
            throws SchemaException {

        if (!existOrgCLosure(session, rOrg.getOid(), rOrg.getOid())) {
            ROrgClosure closure = new ROrgClosure(rOrg, rOrg);
            session.save(closure);
        }

        for (RObjectReference orgRef : rOrg.getParentOrgRef()) {
            fillTransitiveHierarchy(rOrg, orgRef.getTargetOid(), session, withIncorrect);
        }

        if (withIncorrect) {
            Query qIncorrect = session.getNamedQuery("fillHierarchy");
            qIncorrect.setString("oid", rOrg.getOid());

            List<ROrgIncorrect> orgIncorrect = qIncorrect.list();
            for (ROrgIncorrect orgInc : orgIncorrect) {
//                Query qObject = session.createQuery("from RObject where oid = :oid");
//                qObject.setString("oid", orgInc.getDescendantOid());
//                RObject rObjectI = (RObject) qObject.uniqueResult();
//                if (rObjectI != null) {
//                    fillTransitiveHierarchy(rObjectI, rOrg.getOid(), session, !withIncorrect);
//                    session.delete(orgInc);
//                }
            }
        }
    }

    private <T extends ObjectType> void fillTransitiveHierarchy(
            RObject descendant, String ancestorOid, Session session,
            boolean withIncorrect) throws SchemaException {

        Criteria cOrgClosure = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("oid", ancestorOid));

        List<ROrgClosure> orgClosure = cOrgClosure.list();

        if (orgClosure.size() > 0) {
            for (ROrgClosure o : orgClosure) {
                String anc = "null";
                if (o != null && o.getAncestor() != null) {
                    anc = o.getAncestor().getOid();
                }
                LOGGER.trace(
                        "adding {}\t{}",
                        new Object[]{anc, descendant == null ? null : descendant.getOid()});

                boolean existClosure = existOrgCLosure(session, o.getAncestor().getOid(),
                        descendant.getOid());
                if (!existClosure)
                    session.save(new ROrgClosure(o.getAncestor(), descendant));
            }
        } else if (withIncorrect) {
            boolean existIncorrect = existIncorrect(session, ancestorOid, descendant.getOid());
            if (!existIncorrect) {
                LOGGER.trace("adding incorrect {}", new Object[]{ancestorOid, descendant.getOid()});
                session.save(new ROrgIncorrect(ancestorOid, descendant.getOid()));
            }
        }
    }

    private PrismContext getPrismContext() {
        return null;
    }

    private void recompute(List<RObject> objectsToRecompute, Session session)
            throws SchemaException, DtoTranslationException {

        LOGGER.trace("Recomputing organization structure closure table after delete.");

        for (RObject object : objectsToRecompute) {
            Criteria query = session.createCriteria(ClassMapper
                    .getHQLTypeClass(object.toJAXB(getPrismContext(), null)
                            .getClass()));

            // RObject.toJAXB will be deprecated and this query can't be replaced by:
            // Criteria query = session.createCriteria(object.getClass());
            // Because this will cause deadlock. It's the same query without unnecessary object loading, fuck. [lazyman]

            query.add(Restrictions.eq("oid", object.getOid()));
            RObject obj = (RObject) query.uniqueResult();
            if (obj == null) {
                // object not found..probably it was just deleted.
                continue;
            }
            deleteAncestors(object, session);
            fillHierarchy(object, session, false);
        }
        LOGGER.trace("Closure table for organization structure recomputed.");
    }

    private void deleteAncestors(RObject object, Session session) {
        Criteria criteria = session.createCriteria(ROrgClosure.class);
        criteria.add(Restrictions.eq("descendant", object));
        List<ROrgClosure> objectsToDelete = criteria.list();

        for (ROrgClosure objectToDelete : objectsToDelete) {
            session.delete(objectToDelete);
        }

//        Query query = session.createQuery("delete from ROrgClosure as c where c.descendantOid = :dOid");
//        query.setParameter("dOid", object.getOid());
//
//        query.executeUpdate();
    }

    private <T extends ObjectType> void recomputeHierarchy(
            RObject<T> rObjectToModify, Session session,
            Collection<? extends ItemDelta> modifications)
            throws SchemaException, DtoTranslationException {

        for (ItemDelta delta : modifications) {
            if (!QNameUtil.match(delta.getElementName(), OrgType.F_PARENT_ORG_REF)) continue;

            // if modification is one of the modify or delete, delete old
            // record in org closure table and in the next step fill the
            // closure table with the new records
            if (delta.isReplace() || delta.isDelete()) {
                for (Object orgRefDValue : delta.getValuesToDelete()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue))
                        throw new SchemaException("Couldn't modify organization structure hierarchy (adding new " +
                                "records). Expected instance of prism reference value but got " + orgRefDValue);

                    if (rObjectToModify.getClass().isAssignableFrom(ROrg.class)) {
                        List<RObject> objectsToRecompute = deleteTransitiveHierarchy(rObjectToModify, session);
                        refillHierarchy(rObjectToModify, objectsToRecompute, session);
                    } else {
                        deleteHierarchy(rObjectToModify, session);
                        if (rObjectToModify.getParentOrgRef() != null
                                && !rObjectToModify.getParentOrgRef().isEmpty()) {
                            for (RObjectReference orgRef : rObjectToModify.getParentOrgRef()) {
                                fillTransitiveHierarchy(rObjectToModify, orgRef.getTargetOid(), session, true);
                            }
                        }
                    }
                }
            } else if (delta.isAdd()) {
                // fill closure table with new transitive relations
                for (Object orgRefDValue : delta.getValuesToAdd()) {
                    if (!(orgRefDValue instanceof PrismReferenceValue)) {
                        throw new SchemaException(
                                "Couldn't modify organization structure hierarchy (adding new records). Expected " +
                                        "instance of prism reference value but got " + orgRefDValue
                        );
                    }

                    PrismReferenceValue value = (PrismReferenceValue) orgRefDValue;

                    LOGGER.trace("filling transitive hierarchy for descendant {}, ref {}",
                            new Object[]{rObjectToModify.getOid(), value.getOid()});
                    // todo remove
                    fillTransitiveHierarchy(rObjectToModify, value.getOid(), session, true);
                }
            }
        }
    }

    private List<RObject> deleteTransitiveHierarchy(RObject rObjectToModify,
                                                    Session session) throws SchemaException, DtoTranslationException {

        Criteria cDescendant = session.createCriteria(ROrgClosure.class)
                .setProjection(Projections.property("descendant"))
                .add(Restrictions.eq("ancestor", rObjectToModify));

        Criteria cAncestor = session.createCriteria(ROrgClosure.class)
                .setProjection(Projections.property("ancestor"))
                .createCriteria("ancestor", "anc")
                .add(Restrictions.and(Restrictions.eq("this.descendant",
                        rObjectToModify), Restrictions.not(Restrictions.eq(
                        "anc.oid", rObjectToModify.getOid()))));

        Criteria cOrgClosure = session.createCriteria(ROrgClosure.class);

        List<RObject> ocAncestor = cAncestor.list();
        List<RObject> ocDescendant = cDescendant.list();

        if (ocAncestor != null && !ocAncestor.isEmpty()) {
            cOrgClosure.add(Restrictions.in("ancestor", ocAncestor));
        } else {
            LOGGER.trace("No ancestors for object: {}", rObjectToModify.getOid());
        }

        if (ocDescendant != null && !ocDescendant.isEmpty()) {
            cOrgClosure.add(Restrictions.in("descendant", ocDescendant));
        } else {
            LOGGER.trace("No descendants for object: {}", rObjectToModify.getOid());
        }

        List<ROrgClosure> orgClosure = cOrgClosure.list();

        for (ROrgClosure o : orgClosure) {
            if (LOGGER.isTraceEnabled()) {
                RObject ancestor = o.getAncestor();
                RObject descendant = o.getDescendant();
                LOGGER.trace("deleting from hierarchy: A:{} D:{}",
                        new Object[]{RUtil.getDebugString(ancestor), RUtil.getDebugString(descendant)});
            }
            session.delete(o);
        }
        deleteHierarchy(rObjectToModify, session);
        return ocDescendant;
    }

    private void refillHierarchy(RObject parent, List<RObject> descendants,
                                 Session session) throws SchemaException, DtoTranslationException {
        fillHierarchy(parent, session, false);

        for (RObject descendant : descendants) {
            LOGGER.trace("ObjectToRecompute {}", descendant);
            if (!parent.getOid().equals(descendant.getOid())) {
                fillTransitiveHierarchy(descendant, parent.getOid(),
                        session, false);
            }
        }

    }

    private void deleteHierarchy(RObject objectToDelete, Session session) {
        session.getNamedQuery("sqlDeleteOrgClosure").setParameter("oid", objectToDelete.getOid()).executeUpdate();
        session.getNamedQuery("sqlDeleteOrgIncorrect").setParameter("oid", objectToDelete.getOid()).executeUpdate();
    }
}
