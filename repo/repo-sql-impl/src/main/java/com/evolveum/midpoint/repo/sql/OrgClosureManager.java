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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
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
 * @author lazyman
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
        Query query = session.createQuery("delete from ROrgClosure o where o.descendant=:oid");
        query.setString("oid", oid);
        int count = query.executeUpdate();

        if (LOGGER.isTraceEnabled()) LOGGER.trace("Deleted {} records from org. closure table.", count);

        //if there is still parentRef pointing to this oid, we have to add oid to incorrect table
        query = session.createQuery("select count(*) from RParentOrgRef r where r.targetOid=:oid");
        query.setString("oid", oid);

        Number parentCount = (Number) query.uniqueResult();
        if (parentCount != null && parentCount.intValue() != 0) {
            query = session.createSQLQuery("insert into m_org_incorrect (ancestor_oid) values (:oid)");
            query.setString("oid", oid);
            query.executeUpdate();
        }
    }

    private <T extends ObjectType> void handleAdd(String oid, Set<String> parents, Class<T> type, Session session) {
        addParents(oid, parents, true, session);
    }

    private <T extends ObjectType> void handleModify(Collection<? extends ItemDelta> modifications, Session session,
                                                     String oid, Class<T> type) {
        if (modifications.isEmpty()) {
            return;
        }

        Set<String> parents = getOidFromDeleteDeltas(modifications);
        removeParents(oid, parents, session);

        parents = getOidFromAddDeltas(modifications);
        addParents(oid, parents, false, session);
    }

    private void removeParents(String oid, Set<String> parents, Session session) {
        //todo implement
    }

    private void addParents(String oid, Set<String> parents, boolean addingObject, Session session) {
        List<ROrgClosure> closures = new ArrayList<>();
        List<ROrgIncorrect> incorrects = new ArrayList<>();
        if (addingObject) {
            closures.add(new ROrgClosure(oid, oid));
        }

        if (parents.isEmpty()) {
            if (addingObject) {
                closures.addAll(addClosuresFromIncorrects(oid, new ArrayList<String>(), session));
            }
            bulkSave(closures, session);
            return;
        }

        if (LOGGER.isTraceEnabled()) LOGGER.trace("add parents {} for {}", Arrays.toString(parents.toArray()), oid);

        Query query = session.createQuery("select o.oid from RObject o where o.oid in (:oids)");
        query.setParameterList("oids", parents);
        List<String> existing = query.list();

        List<String> newClosureAncestors = new ArrayList<>();
        if (!existing.isEmpty()) {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("adding for existing {} for {}", Arrays.toString(existing.toArray()), oid);

            query = session.createQuery("select ancestorOid from ROrgClosure where descendantOid in (:existing) group by ancestorOid");
            query.setParameterList("existing", existing);
            List<String> ancestors = new ArrayList<String>(query.list());

            query = session.createQuery("select ancestorOid from ROrgClosure where descendantOid = :oid");
            query.setString("oid", oid);
            ancestors.removeAll(new ArrayList<String>(query.list()));
            newClosureAncestors = ancestors;

            for (String a : ancestors) {
                closures.add(new ROrgClosure(a, oid));
            }
        }

        parents.removeAll(existing);

        if (!parents.isEmpty()) {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("adding incorrects {} for {}", Arrays.toString(parents.toArray()), oid);
            for (String nonexisting : parents) {
                incorrects.add(new ROrgIncorrect(nonexisting, oid));
            }

            query = session.createQuery("select ancestorOid from ROrgIncorrect where descendantOid in (:nonexisting) group by ancestorOid");
            query.setParameterList("nonexisting", parents);
            List<String> ancestors = new ArrayList<String>(query.list());

            if (!ancestors.isEmpty()) {
                query = session.createQuery("select ancestorOid from ROrgIncorrect where descendantOid = :oid");
                query.setString("oid", oid);
                ancestors.removeAll(new ArrayList<String>(query.list()));

                for (String a : ancestors) {
                    incorrects.add(new ROrgIncorrect(a, oid));
                }
            }
        }

        if (addingObject) {
            closures.addAll(addClosuresFromIncorrects(oid, newClosureAncestors, session));
        }

        bulkSave(closures, session);
        bulkSave(incorrects, session);
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

    private void bulkSave(List objects, Session session) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        LOGGER.trace("Bulk saving {} objects {}", objects.size(), objects.get(0).getClass().getSimpleName());

        for (int i = 0; i < objects.size(); i++) {
            LOGGER.trace("{}", objects.get(i));

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
