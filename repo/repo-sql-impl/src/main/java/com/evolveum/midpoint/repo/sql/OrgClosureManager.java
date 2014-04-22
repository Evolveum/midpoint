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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.hibernate.Query;
import org.hibernate.Session;

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
}
