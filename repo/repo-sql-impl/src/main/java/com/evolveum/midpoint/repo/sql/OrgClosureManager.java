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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        LOGGER.debug("Starting update for org. closure for {} {}.", oid, type);
        List<ReferenceDelta> deltas = filterParentRefDeltas(modifications);
        if (deltas.isEmpty()) {
            return;
        }

        switch (operation) {
            case DELETE:
                handleDelete(modifications, session, oid, type);
                break;
            case ADD:
                handleAdd(modifications, session, oid, type);
                break;
            case MODIFY:
                handleModify(modifications, session, oid, type);
        }

        LOGGER.debug("Org. closure update finished.");
    }

    /**
     * 1->2
     * 1->3
     * 2->4
     * 3->4
     * 3->5
     * <p/>
     * a d
     * 1 1 0
     * 1 2 1
     * 1 3 1  -
     * 1 4 2
     * 1 5 2  *****
     * 2 2 0
     * 2 4 1
     * 3 3 0  -
     * 3 4 1  -
     * 3 5 1  -
     * 4 4 0
     * 5 5 0
     * <p/>
     * deleting 3
     * <p/>
     * 1->2
     * 2->4
     * 5
     * <p/>
     * a d
     * 1 1 0
     * 1 2 1
     * 1 4 2
     * 2 2 0
     * 2 4 1
     * 4 4 0
     * 5 5 0
     * <p/>
     * <p/>
     * remove all with ancestor = 3
     * remove all with descendant = 3
     * <p/>
     * deleting 5
     * remove all with ancestor = 5
     * remove all with descendant = 5
     * <p/>
     * deleting ref 1->3 (parentRef = 1 in object 3)
     * update depth all children of 3 and new depth = depth - (1->3 depth which is 1 based on 1 3 1)
     * remove 1 3 1
     */
    private <T extends ObjectType> void handleDelete(Collection<? extends ItemDelta> modifications, Session session,
                                                     String oid, Class<T> type) {

//        "select distinct o.descendantOid from ROrgClosure as o where o.ancestorId=0 and o.ancestorOid=:oid";
//        "select distinct o.ancestorOid from ROrgClosure as o " +
//                "where o.descendantId=0 and o.descendantOid=:oid and and o.ancestorId=0 o.ancestorOid != :oid";


        LOGGER.trace("Deleting org. closure for object {} {}", oid, type.getSimpleName());
        Query query = session.createQuery("select distinct o.ancestorOid from ROrgClosure as o where o.descendantId=0 and o.descendantOid=:dOid");

        query = session.createQuery("delete from ROrgClosure as o where (o.ancestorId=0 and " +
                "o.ancestorOid=:aOid) or (o.descendantId=0 and o.descendantOid=:dOid)");
        query.setString("aOid", oid);
        query.setString("dOid", oid);
        int count = query.executeUpdate();
        LOGGER.trace("Deleted {} records.", count);
    }

    private <T extends ObjectType> void handleAdd(Collection<? extends ItemDelta> modifications, Session session,
                                                  String oid, Class<T> type) {

    }

    private <T extends ObjectType> void handleModify(Collection<? extends ItemDelta> modifications, Session session,
                                                     String oid, Class<T> type) {

    }

    private List<ReferenceDelta> filterParentRefDeltas(Collection<? extends ItemDelta> modifications) {
        List<ReferenceDelta> deltas = new ArrayList<>();

        for (ItemDelta delta : modifications) {
            if (!ObjectType.F_PARENT_ORG_REF.equals(delta.getElementName())) {
                continue;
            }
            deltas.add((ReferenceDelta) delta);
        }

        return deltas;
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

    private boolean existOrgCLosure(Session session, String ancestorOid, String descendantOid, int depth) {
        // if not exist pair with same depth, then create else nothing do
        Query qExistClosure = session.getNamedQuery("existOrgClosure");
        qExistClosure.setParameter("ancestorOid", ancestorOid);
        qExistClosure.setParameter("descendantOid", descendantOid);
        qExistClosure.setParameter("depth", depth);

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

        if (!existOrgCLosure(session, rOrg.getOid(), rOrg.getOid(), 0)) {
            ROrgClosure closure = new ROrgClosure(rOrg, rOrg, 0);
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
                Query qObject = session.createQuery("from RObject where oid = :oid");
                qObject.setString("oid", orgInc.getDescendantOid());
                RObject rObjectI = (RObject) qObject.uniqueResult();
                if (rObjectI != null) {
                    fillTransitiveHierarchy(rObjectI, rOrg.getOid(), session, !withIncorrect);
                    session.delete(orgInc);
                }
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
                        "adding {}\t{}\t{}",
                        new Object[]{anc, descendant == null ? null : descendant.getOid(), o.getDepth() + 1});

                boolean existClosure = existOrgCLosure(session, o.getAncestor().getOid(),
                        descendant.getOid(), o.getDepth() + 1);
                if (!existClosure)
                    session.save(new ROrgClosure(o.getAncestor(), descendant, o.getDepth() + 1));
            }
        } else if (withIncorrect) {
            boolean existIncorrect = existIncorrect(session, ancestorOid, descendant.getOid());
            if (!existIncorrect) {
                LOGGER.trace("adding incorrect {}\t{}", new Object[]{ancestorOid,
                        descendant.getOid()});
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
                LOGGER.trace("deleting from hierarchy: A:{} D:{} depth:{}",
                        new Object[]{RUtil.getDebugString(ancestor), RUtil.getDebugString(descendant), o.getDepth()});
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
