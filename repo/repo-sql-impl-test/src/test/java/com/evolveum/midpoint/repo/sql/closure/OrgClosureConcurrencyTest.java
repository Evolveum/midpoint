/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.closure;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.repo.sql.helpers.OrgClosureManager.Edge;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureConcurrencyTest extends AbstractOrgClosureTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureConcurrencyTest.class);

    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 5, 3, 3, 3  };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL       = { 0, 2, 2, 3  };
    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 15, 20, 30  };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 3,  3,  3,  3  };           // small number of deletes
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 3, 15, 20, 30  };           // average number of deletes
    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 5, 15, 45, 100  };            // large number of deletes
    public static final int THREADS = 4;

    /*
     *  H2 seems to have a problem in that one of the worker threads freezes when running the following statement:
     *
     *  select distinct rparentorg0_.owner_oid as col_0_0_ from m_reference rparentorg0_
     *      inner join m_object robject1_ on rparentorg0_.owner_oid=robject1_.oid where rparentorg0_.reference_type=0 and rparentorg0_.targetOid=? and robject1_.objectTypeClass=?
     *
     *  (selecting child nodes to be registered into orgGraph)
     *
     *  Dunno why. Let's use a timeout of 30 minutes so that the tests would not loop indefinitely.
     */
    public static final long TIMEOUT = 1800L*1000L;

    // very small scenario
//    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 2, 1  };
//    private static final int[] USER_CHILDREN_IN_LEVEL = null;
//    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 2  };
//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 1, 1    };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 2, 1    };

    private OrgClosureTestConfiguration configuration;

    public OrgClosureConcurrencyTest() {
        configuration = new OrgClosureTestConfiguration();
        configuration.setCheckChildrenSets(true);
        configuration.setCheckClosureMatrix(true);
        configuration.setDeletionsToClosureTest(15);
        configuration.setOrgChildrenInLevel(ORG_CHILDREN_IN_LEVEL);
        configuration.setUserChildrenInLevel(USER_CHILDREN_IN_LEVEL);
        configuration.setParentsInLevel(PARENTS_IN_LEVEL);
        configuration.setLinkRoundsForLevel(LINK_ROUNDS_FOR_LEVELS);
        configuration.setNodeRoundsForLevel(NODE_ROUNDS_FOR_LEVELS);
    }

    @Override
    public OrgClosureTestConfiguration getConfiguration() {
        return configuration;
    }

    @Test(enabled = true) public void test100LoadOrgStructure() throws Exception { _test100LoadOrgStructure(); }
    @Test(enabled = true) public void test150CheckClosure() throws Exception { _test150CheckClosure(); }
    @Test(enabled = true) public void test200AddRemoveLinksSeq() throws Exception { _test200AddRemoveLinksMT(false); }
    @Test(enabled = true) public void test201AddRemoveLinksRandom() throws Exception { _test200AddRemoveLinksMT(true); }
    @Test(enabled = true) public void test300AddRemoveNodesSeq() throws Exception { _test300AddRemoveNodesMT(false); }
    @Test(enabled = true) public void test301AddRemoveNodesRandom() throws Exception { _test300AddRemoveNodesMT(true); }

    /**
     * We randomly select a set of links to be removed.
     * Then we remove them, using a given set of threads.
     * After all threads are done, we will check the closure table consistency.
     *
     * And after that, we will do the reverse, re-adding all the links previously removed.
     * In the end, we again check the consistency.
     */
    protected void _test200AddRemoveLinksMT(final boolean random) throws Exception {
        OperationResult opResult = new OperationResult("===[ test200AddRemoveLinksMT ]===");

        info("test200AddRemoveLinks starting with random = " + random);

        final Set<Edge> edgesToRemove = Collections.synchronizedSet(new HashSet<Edge>());
        final Set<Edge> edgesToAdd = Collections.synchronizedSet(new HashSet<Edge>());

        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        // parentRef link removal + addition
        for (int level = 0; level < getConfiguration().getLinkRoundsForLevel().length; level++) {
            int rounds = getConfiguration().getLinkRoundsForLevel()[level];
            List<String> levelOids = orgsByLevels.get(level);
            int retries = 0;
            for (int round = 0; round < rounds; round++) {

                int index = (int) Math.floor(Math.random() * levelOids.size());
                String oid = levelOids.get(index);

                OrgType org = repositoryService.getObject(OrgType.class, oid, null, opResult).asObjectable();

                // check if it has no parents (shouldn't occur here!)
                if (org.getParentOrgRef().isEmpty()) {
                    throw new IllegalStateException("No parents in " + org);
                }

                int i = (int) Math.floor(Math.random() * org.getParentOrgRef().size());
                ObjectReferenceType parentOrgRef = org.getParentOrgRef().get(i);

                Edge edge = new Edge(oid, parentOrgRef.getOid());
                if (edgesToRemove.contains(edge)) {
                    round--;
                    if (++retries == 1000) {
                        throw new IllegalStateException("Too many retries");    // primitive attempt to break potential cycles when there is not enough edges to process
                    } else {
                        continue;
                    }
                }
                edgesToRemove.add(edge);
                edgesToAdd.add(edge);
            }
        }

        int numberOfRunners = THREADS;
        info("Edges to remove/add (" + edgesToRemove.size() + ": " + edgesToRemove);
        info("Number of runners: " + numberOfRunners);
        final List<Thread> runners = Collections.synchronizedList(new ArrayList<Thread>());

        for (int i = 0; i < numberOfRunners; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Edge edge = getNext(edgesToRemove, random);
                            if (edge == null) {
                                break;
                            }
                            LOGGER.info("Removing {}", edge);
                            removeEdge(edge);
                            int remaining;
                            synchronized (OrgClosureConcurrencyTest.this) {
                                edgesToRemove.remove(edge);
                                remaining = edgesToRemove.size();
                            }
                            info(Thread.currentThread().getName() + " removed " + edge + "; remaining: " + remaining);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    } finally {
                        runners.remove(Thread.currentThread());
                    }
                }
            };
            Thread t = new Thread(runnable);
            runners.add(t);
            t.start();
        }

        waitForRunnersCompletion(runners);

        if (!edgesToRemove.isEmpty()) {
            throw new AssertionError("Edges to remove is not empty, see the console or log: " + edgesToRemove);
        }

        if (!exceptions.isEmpty()) {
            throw new AssertionError("Found exceptions: " + exceptions);
        }

        checkClosure(orgGraph.vertexSet());
        info("Consistency after removal OK");

        for (int i = 0; i < numberOfRunners; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Edge edge = getNext(edgesToAdd, random);
                            if (edge == null) {
                                break;
                            }
                            LOGGER.info("Adding {}", edge);
                            addEdge(edge);
                            int remaining;
                            synchronized (OrgClosureConcurrencyTest.this) {
                                edgesToAdd.remove(edge);
                                remaining = edgesToAdd.size();
                            }
                            info(Thread.currentThread().getName() + " re-added " + edge + "; remaining: " + remaining);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    } finally {
                        runners.remove(Thread.currentThread());
                    }
                }
            };
            Thread t = new Thread(runnable);
            runners.add(t);
            t.start();
        }

        waitForRunnersCompletion(runners);

        if (!edgesToAdd.isEmpty()) {
            throw new AssertionError("Edges to add is not empty, see the console or log: " + edgesToAdd);
        }

        if (!exceptions.isEmpty()) {
            throw new AssertionError("Found exceptions: " + exceptions);
        }

        checkClosure(orgGraph.vertexSet());

        info("Consistency after re-adding OK");
    }

    private void waitForRunnersCompletion(List<Thread> runners) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!runners.isEmpty()) {
            Thread.sleep(100);          // primitive way of waiting
            if (System.currentTimeMillis()-start > TIMEOUT) {
                throw new AssertionError("Test is running for too long. Probably caused by a locked-up thread. Runners = " + runners);
            }
        }
    }

    private synchronized <T> T getNext(Set<T> items, boolean random) {
        if (items.isEmpty()) {
            return null;
        }
        Iterator<T> iterator = items.iterator();
        if (random) {
            int i = (int) Math.floor(Math.random() * items.size());
            while (i-- > 0) {
                iterator.next();
            }
        }
        return iterator.next();
    }

    private void removeEdge(Edge edge) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta> modifications = new ArrayList<>();
        ObjectReferenceType parentOrgRef = new ObjectReferenceType();
        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
        parentOrgRef.setOid(edge.getAncestor());
        ItemDelta removeParent = ReferenceDelta.createModificationDelete(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, parentOrgRef.asReferenceValue());
        modifications.add(removeParent);
        repositoryService.modifyObject(OrgType.class, edge.getDescendant(), modifications, new OperationResult("dummy"));
        synchronized(this) {
            orgGraph.removeEdge(edge.getDescendant(), edge.getAncestor());
        }
    }

    private void addEdge(Edge edge) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        List<ItemDelta> modifications = new ArrayList<>();
        ObjectReferenceType parentOrgRef = new ObjectReferenceType();
        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
        parentOrgRef.setOid(edge.getAncestor());
        ItemDelta itemDelta = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, parentOrgRef.asReferenceValue());
        modifications.add(itemDelta);
        repositoryService.modifyObject(OrgType.class, edge.getDescendant(), modifications, new OperationResult("dummy"));
        synchronized(this) {
            orgGraph.addEdge(edge.getDescendant(), edge.getAncestor());
        }
    }

    protected void _test300AddRemoveNodesMT(final boolean random) throws Exception {
        OperationResult opResult = new OperationResult("===[ test300AddRemoveNodesMT ]===");

        info("test300AddRemoveNodes starting with random = " + random);

        final Set<ObjectType> nodesToRemove = Collections.synchronizedSet(new HashSet<ObjectType>());
        final Set<ObjectType> nodesToAdd = Collections.synchronizedSet(new HashSet<ObjectType>());

        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        for (int level = 0; level < getConfiguration().getNodeRoundsForLevel().length; level++) {
            int rounds = getConfiguration().getNodeRoundsForLevel()[level];
            List<String> levelOids = orgsByLevels.get(level);
            generateNodesAtOneLevel(nodesToRemove, nodesToAdd, OrgType.class, rounds, levelOids, opResult);
        }

        int numberOfRunners = THREADS;
        final List<Thread> runners = Collections.synchronizedList(new ArrayList<Thread>());

        for (int i = 0; i < numberOfRunners; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            ObjectType objectType = getNext(nodesToRemove, random);
                            if (objectType == null) {
                                break;
                            }
                            LOGGER.info("Removing {}", objectType);
                            int remaining;
                            try {
                                removeObject(objectType);
                                synchronized (OrgClosureConcurrencyTest.this) {
                                    nodesToRemove.remove(objectType);
                                    remaining = nodesToRemove.size();
                                }
                                info(Thread.currentThread().getName() + " removed " + objectType + "; remaining: " + remaining);
                            } catch (ObjectNotFoundException e) {
                                // this is OK
                                info(Thread.currentThread().getName() + ": " + objectType + " already deleted");
                                Thread.sleep(300);      // give other threads a chance
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    } finally {
                        runners.remove(Thread.currentThread());
                    }
                }
            };
            Thread t = new Thread(runnable);
            runners.add(t);
            t.start();
        }

        waitForRunnersCompletion(runners);

        if (!nodesToRemove.isEmpty()) {
            throw new AssertionError("Nodes to remove is not empty, see the console or log: " + nodesToRemove);
        }

        if (!exceptions.isEmpty()) {
            throw new AssertionError("Found exceptions: " + exceptions);
        }

        rebuildGraph();
        checkClosure(orgGraph.vertexSet());
        info("Consistency after removing OK");

        numberOfRunners = THREADS;
        for (int i = 0; i < numberOfRunners; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            ObjectType objectType = getNext(nodesToAdd, random);
                            if (objectType == null) {
                                break;
                            }
                            LOGGER.info("Adding {}", objectType);
                            try {
                                addObject(objectType.clone());
//                                rebuildGraph();
//                                checkClosure(orgGraph.vertexSet());
                                int remaining;
                                synchronized (OrgClosureConcurrencyTest.this) {
                                    nodesToAdd.remove(objectType);
                                    remaining = nodesToAdd.size();
                                }
                                info(Thread.currentThread().getName() + " re-added " + objectType + "; remaining: " + remaining);
                            } catch (ObjectAlreadyExistsException e) {
                                // this is OK
                                info(Thread.currentThread().getName() + ": " + objectType + " already exists");
                                Thread.sleep(300);      // give other threads a chance
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        exceptions.add(e);
                    } finally {
                        runners.remove(Thread.currentThread());
                    }
                }
            };
            Thread t = new Thread(runnable);
            runners.add(t);
            t.start();
        }

        waitForRunnersCompletion(runners);

        if (!nodesToAdd.isEmpty()) {
            throw new AssertionError("Nodes to add is not empty, see the console or log: " + nodesToAdd);
        }

        if (!exceptions.isEmpty()) {
            throw new AssertionError("Found exceptions: " + exceptions);
        }

        rebuildGraph();
        checkClosure(orgGraph.vertexSet());
        info("Consistency after re-adding OK");
    }

    private void rebuildGraph() {
        OperationResult result = new OperationResult("dummy");
        info("Graph before rebuilding: " + orgGraph.vertexSet().size() + " vertices, " + orgGraph.edgeSet().size() + " edges");
        orgGraph.removeAllVertices(new HashSet<>(orgGraph.vertexSet()));
        List<PrismObject> objects = null;
        try {
            objects = (List) repositoryService.searchObjects(OrgType.class, new ObjectQuery(), null, result);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        for (PrismObject object : objects) {
            String oid = object.getOid();
            orgGraph.addVertex(oid);
        }
        for (PrismObject<ObjectType> object : objects) {
            for (ObjectReferenceType ort : object.asObjectable().getParentOrgRef()) {
                if (orgGraph.containsVertex(ort.getOid())) {
                    String oid = object.getOid();
                    try {
                        orgGraph.addEdge(oid, ort.getOid());
                    } catch (RuntimeException e) {
                        System.err.println("Couldn't add edge " + oid + " -> " + ort.getOid() + " into the graph");
                        throw e;
                    }
                }
            }
        }
        info("Graph after rebuilding: "+orgGraph.vertexSet().size()+" vertices, "+orgGraph.edgeSet().size()+" edges");
    }

    private void generateNodesAtOneLevel(Set<ObjectType> nodesToRemove, Set<ObjectType> nodesToAdd,
                                         Class<? extends ObjectType> clazz,
                                         int rounds, List<String> candidateOids,
                                         OperationResult opResult) throws ObjectNotFoundException, SchemaException {
        if (candidateOids.isEmpty()) {
            return;
        }
        int retries = 0;
        for (int round = 0; round < rounds; round++) {

            int index = (int) Math.floor(Math.random() * candidateOids.size());
            String oid = candidateOids.get(index);
            ObjectType objectType = repositoryService.getObject(clazz, oid, null, opResult).asObjectable();

            if (nodesToRemove.contains(objectType)) {
                round--;
                if (++retries == 1000) {
                    throw new IllegalStateException("Too many retries");    // primitive attempt to break potential cycles when there is not enough edges to process
                } else {
                    continue;
                }
            }

            nodesToRemove.add(objectType);
            nodesToAdd.add(objectType);
        }
    }

    void removeObject(ObjectType objectType) throws Exception {
        repositoryService.deleteObject(objectType.getClass(), objectType.getOid(), new OperationResult("dummy"));
        synchronized(orgGraph) {
            if (objectType instanceof OrgType) {
                orgGraph.removeVertex(objectType.getOid());
            }
        }
    }

    void addObject(ObjectType objectType) throws Exception {
        repositoryService.addObject(objectType.asPrismObject(), null, new OperationResult("dummy"));
        synchronized(orgGraph) {
            if (objectType instanceof OrgType) {
                registerObject(objectType, true);
            }
        }
    }

}
