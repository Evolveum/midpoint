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

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.sql.BaseSQLRepoTest;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.OrgClosureManager;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author lazyman
 * @author mederly
 */
public abstract class AbstractOrgClosureTest extends BaseSQLRepoTest {

	@Autowired
	private OrgClosureManager closureManager;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractOrgClosureTest.class);

    // The following attributes describe the object graph as originally created/scanned.
    // Subsequent operations (add/remove node or link) DO NOT change these.

    protected int objectCount = 0;

    protected List<String> rootOids = new ArrayList<>();

    protected List<OrgType> allOrgCreated = new ArrayList<>();

    protected List<UserType> allUsersCreated = new ArrayList<>();

    protected List<List<String>> orgsByLevels = new ArrayList<>();

    protected List<List<String>> usersByLevels = new ArrayList<>();

    private int maxLevel = 0;

    protected long closureSize;

    // Describes current state of the org graph
    // Beware! Access to this object should be synchronized (for multithreaded tests).
    protected SimpleDirectedGraph<String, DefaultEdge> orgGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    // database session, used exclusively for read-only operations
    protected ThreadLocal<Session> sessionTl = new ThreadLocal<>();

    protected Session getSession() {
        Session session = sessionTl.get();
        if (session == null || !session.isConnected()) {
            session = baseHelper.getSessionFactory().openSession();
            sessionTl.set(session);
        }
        return session;
    }

    protected void checkClosure(Set<String> oidsToCheck) throws SchemaException {
        boolean matrixProblem = false;
        if (getConfiguration().isCheckClosureMatrix()) {
            matrixProblem = checkClosureMatrix();
        }
        if (getConfiguration().isCheckChildrenSets()) {
            checkChildrenSets(oidsToCheck);
        }
        assertFalse("A difference in transitive closure matrix was detected", matrixProblem);
    }

    protected void checkClosureUnconditional(Set<String> oidsToCheck) {
        checkChildrenSets(oidsToCheck);
    }

    private void checkChildrenSets(Set<String> oidsToCheck) {
        SimpleDirectedGraph<String,DefaultEdge> tc = (SimpleDirectedGraph) orgGraph.clone();
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(tc);
        for (String subroot : oidsToCheck) {
            LOGGER.info("Checking descendants of {}", subroot);
            Set<String> expectedChildren = new HashSet<>();
            for (DefaultEdge edge : tc.incomingEdgesOf(subroot)) {
                expectedChildren.add(tc.getEdgeSource(edge));
            }
            expectedChildren.add(subroot);
            LOGGER.trace("Expected children: {}", expectedChildren);
            Set<String> actualChildren = getActualChildrenOf(subroot);
            LOGGER.trace("Actual children: {}", actualChildren);

            Set<String> expectedMinusActual = new HashSet<>(expectedChildren);
            expectedMinusActual.removeAll(actualChildren);
            if (!expectedMinusActual.isEmpty()) {
                System.out.println("Expected-Actual = " + expectedMinusActual);
            }
            Set<String> actualMinusExpected = new HashSet<>(actualChildren);
            actualMinusExpected.removeAll(expectedChildren);
            if (!actualMinusExpected.isEmpty()) {
                System.out.println("Actual-Expected = " + actualMinusExpected);
            }
            assertEquals("Incorrect children for " + subroot, expectedChildren, actualChildren);
        }
    }

    /**
     * Recomputes closure table from scratch (using matrix multiplication) and compares it with M_ORG_CLOSURE.
     */
    private static final boolean DUMP_TC_MATRIX_DETAILS = true;

    protected boolean checkClosureMatrix() throws SchemaException {
        Session session = getSession();
        // we compute the closure table "by hand" as 1 + A + A^2 + A^3 + ... + A^n where n is the greatest expected path length
        int vertices = getVertices().size();

        long start = System.currentTimeMillis();

        // used to give indices to vertices
        List<String> vertexList = new ArrayList<>(getVertices());

        if (DUMP_TC_MATRIX_DETAILS) LOGGER.info("Vertex list = {}", vertexList);

        DoubleMatrix2D a = new SparseDoubleMatrix2D(vertices, vertices);
//        for (int i = 0; i < vertices; i++) {
//            a.setQuick(i, i, 1.0);
//        }
        for (DefaultEdge edge : orgGraph.edgeSet()) {
            a.set(vertexList.indexOf(orgGraph.getEdgeSource(edge)),
                  vertexList.indexOf(orgGraph.getEdgeTarget(edge)),
                  1.0);
        }

        DoubleMatrix2D result = new SparseDoubleMatrix2D(vertices, vertices);
        for (int i = 0; i < vertices; i++) {
            result.setQuick(i, i, 1.0);
        }

        DoubleMatrix2D power = result.copy();
        Algebra alg = new Algebra();
        for (int level = 1; level <= maxLevel; level++) {
            power = alg.mult(power, a);
            result.assign(power, Functions.plus);
//            System.out.println("a=" + a);
//            System.out.println("a^"+level+"="+power);
        }
        LOGGER.info("TC matrix computed in {} ms", System.currentTimeMillis() - start);

        if (DUMP_TC_MATRIX_DETAILS) LOGGER.info("TC matrix expected = {}", result);

        Query q = session.createNativeQuery("select descendant_oid, ancestor_oid, val from m_org_closure")
                .addScalar("descendant_oid", StringType.INSTANCE)
                .addScalar("ancestor_oid", StringType.INSTANCE)
                .addScalar("val", LongType.INSTANCE);
        List<Object[]> list = q.list();
        LOGGER.info("OrgClosure has {} rows", list.size());

        DoubleMatrix2D closureInDatabase = new SparseDoubleMatrix2D(vertices, vertices);
        for (Object[] item : list) {
            int val = Integer.parseInt(item[2].toString());
            if (val == 0) {
                throw new IllegalStateException("Row with val == 0 in closure table: " + list);
            }
            closureInDatabase.set(vertexList.indexOf(item[0]),
                    vertexList.indexOf(item[1]),
                    val);
        }

        if (DUMP_TC_MATRIX_DETAILS) LOGGER.info("TC matrix fetched from db = {}", closureInDatabase);

        double zSumResultBefore = result.zSum();
        double zSumClosureInDb = closureInDatabase.zSum();
        result.assign(closureInDatabase, Functions.minus);
        double zSumResultAfter = result.zSum();
        LOGGER.info("Summary of items in closure computed: {}, in DB-stored closure: {}, delta: {}", new Object[]{zSumResultBefore, zSumClosureInDb, zSumResultAfter});

        if (DUMP_TC_MATRIX_DETAILS) LOGGER.info("Difference matrix = {}", result);

        boolean problem = false;
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                double delta = result.get(i, j);
                if (Math.round(delta) != 0) {
                    System.err.println("delta("+vertexList.get(i)+","+vertexList.get(j)+") = " + delta +
                            " (closureInDB=" + closureInDatabase.get(i, j) + ", expected=" + (result.get(i, j) + closureInDatabase.get(i, j)) + ")");
                    LOGGER.error("delta("+vertexList.get(i)+","+vertexList.get(j)+") = " + delta);
                    problem = true;
                }
            }
        }
        if (problem) {
            checkOrgGraph();
        }
        return problem;
    }

    // checks org graph w.r.t. real org/parentref situation in repo
    protected void checkOrgGraph() throws SchemaException {
        OperationResult result = new OperationResult("temp");
        int numberOfOrgsInRepo = repositoryService.countObjects(OrgType.class, new ObjectQuery(), null, result);
        info("Checking graph with repo. Orgs in repo: " + numberOfOrgsInRepo + ", orgs in graph: " + orgGraph.vertexSet().size());
        assertTrue("# of orgs in repo (" + numberOfOrgsInRepo + ") is different from # of orgs in graph (" + orgGraph.vertexSet().size() + ")",
                numberOfOrgsInRepo == orgGraph.vertexSet().size());
        for (String oid : orgGraph.vertexSet()) {
            //info("Checking " + oid);
            OrgType orgType = null;
            try {
                orgType = repositoryService.getObject(OrgType.class, oid, null, result).asObjectable();
            } catch (ObjectNotFoundException|SchemaException e) {
                throw new AssertionError("Couldn't fetch " + oid, e);
            }
            assertTrue(orgGraph.vertexSet().contains(orgType.getOid()));

            Set<String> parentOidsInRepo = new HashSet<>();
            for (ObjectReferenceType ort : orgType.getParentOrgRef()) {
                if (orgGraph.vertexSet().contains(ort.getOid())) {      // i.e. the parent does exist
                    parentOidsInRepo.add(ort.getOid());
                }
            }
            Set<String> parentOidsInGraph = new HashSet<>();
            for (DefaultEdge edge : orgGraph.outgoingEdgesOf(oid)) {
                parentOidsInGraph.add(orgGraph.getEdgeTarget(edge));
            }
            assertEquals("Unexpected parentRefOrg set in " + orgType, parentOidsInGraph, parentOidsInRepo);
        }
        info("Graph is OK w.r.t. repo");
    }

    protected Set<String> getActualChildrenOf(String ancestor) {
        List<ROrgClosure> descendantRecords = getOrgClosureByAncestor(ancestor);
        Set<String> rv = new HashSet<String>();
        for (ROrgClosure c : descendantRecords) {
            rv.add(c.getDescendantOid());
        }
        return rv;
    }

    private List<ROrgClosure> getOrgClosureByDescendant(String descendantOid) {
        Query query = getSession().createQuery("from ROrgClosure where descendantOid=:oid");
        query.setParameter("oid", descendantOid);
        return query.list();
    }

    private List<ROrgClosure> getOrgClosureByAncestor(String ancestorOid) {
        Query query = getSession().createQuery("from ROrgClosure where ancestorOid=:oid");
        query.setParameter("oid", ancestorOid);
        return query.list();
    }

    protected void removeObjectParent(ObjectType object, ObjectReferenceType parentOrgRef, boolean useReplace, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        if (!useReplace) {          // standard case
            PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
            ItemDelta removeParent = ReferenceDelta.createModificationDelete(object.getClass(), OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
            modifications.add(removeParent);
        } else {                    // using REPLACE modification
            List<PrismReferenceValue> newValues = new ArrayList<>();
            for (ObjectReferenceType ort : object.getParentOrgRef()) {
                if (!ort.getOid().equals(parentOrgRef.getOid())) {
                    newValues.add(ort.asReferenceValue().clone());
                }
            }
            PrismObjectDefinition objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(object.getClass());
            ItemDelta replaceParent = ReferenceDelta.createModificationReplace(new ItemPath(OrgType.F_PARENT_ORG_REF), objectDefinition, newValues);
            modifications.add(replaceParent);
        }
        repositoryService.modifyObject(object.getClass(), object.getOid(), modifications, opResult);
        if (object instanceof OrgType) {
            orgGraph.removeEdge(object.getOid(), parentOrgRef.getOid());
        }
    }

    // TODO generalzie to addObjectParent
    protected void addOrgParent(OrgType org, ObjectReferenceType parentOrgRef, boolean useReplace, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta itemDelta;
        if (!useReplace) {
            itemDelta = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        } else {
            List<PrismReferenceValue> newValues = new ArrayList<>();
            for (ObjectReferenceType ort : org.getParentOrgRef()) {
                newValues.add(ort.asReferenceValue().clone());
            }
            newValues.add(existingValue.clone());
            PrismObjectDefinition objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class);
            itemDelta = ReferenceDelta.createModificationReplace(new ItemPath(OrgType.F_PARENT_ORG_REF), objectDefinition, newValues);
        }
        modifications.add(itemDelta);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.addEdge(org.getOid(), existingValue.getOid());
    }

    protected void addUserParent(UserType user, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta readdParent = ReferenceDelta.createModificationAdd(UserType.class, UserType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(readdParent);
        repositoryService.modifyObject(UserType.class, user.getOid(), modifications, opResult);
    }

    protected void removeOrg(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(OrgType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    protected void removeUser(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(UserType.class, oid, opResult);
    }

    protected void reAddOrg(OrgType org, OperationResult opResult) throws Exception {
        repositoryService.addObject(org.asPrismObject(), null, opResult);
        registerObject(org, true);
    }

    protected void reAddUser(UserType user, OperationResult opResult) throws Exception {
        repositoryService.addObject(user.asPrismObject(), null, opResult);
    }

    // parentsInLevel may be null (in that case, a simple tree is generated)
    protected void loadOrgStructure(int level, String parentOid, String oidPrefix,
                                    OperationResult result) throws Exception {

        int[] orgChildrenInLevel = getConfiguration().getOrgChildrenInLevel();
        int[] userChildrenInLevel = getConfiguration().getUserChildrenInLevel();
        int[] parentsInLevel = getConfiguration().getParentsInLevel();

        if (level == orgChildrenInLevel.length) {
            return;
        }

        if (level > maxLevel) {
            maxLevel = level;
        }

        List<String> orgsAtThisLevel = getOrgsAtThisLevelSafe(level);

        for (int i = 0; i < orgChildrenInLevel[level]; i++) {
            String newOidPrefix = getOidCharFor(i) + oidPrefix;
            int numberOfParents = parentsInLevel==null ? (parentOid != null ? 1 : 0) : parentsInLevel[level];
            PrismObject<OrgType> org = createOrg(generateParentsForLevel(parentOid, level, numberOfParents), newOidPrefix);
            LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{org, objectCount, getParentsOids(org)});
            String oid = repositoryService.addObject(org, null, result);
            org.setOid(oid);
            if (parentOid == null) {
                rootOids.add(oid);
            }
            allOrgCreated.add(org.asObjectable());
            registerObject(org.asObjectable(), false);
            orgsAtThisLevel.add(oid);
            objectCount++;
            if (objectCount % 20 == 0) {
                info(objectCount + " objects created");
            }

            loadOrgStructure(level+1, oid, newOidPrefix, result);
        }

        if (parentOid != null && userChildrenInLevel != null) {

            List<String> usersAtThisLevel = getUsersAtThisLevelSafe(level);

            for (int u = 0; u < userChildrenInLevel[level]; u++) {
                int numberOfParents = parentsInLevel==null ? 1 : parentsInLevel[level];
                PrismObject<UserType> user = createUser(generateParentsForLevel(parentOid, level, numberOfParents), getOidCharFor(u) + ":" + oidPrefix);
                LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{user, objectCount, getParentsOids(user)});
                String uoid = repositoryService.addObject(user, null, result);
                user.setOid(uoid);
                allUsersCreated.add(user.asObjectable());
                usersAtThisLevel.add(uoid);
                objectCount++;
                if (objectCount % 20 == 0) {
                    info(objectCount + " objects created");
                }
            }
        }

    }

    protected List<String> getUsersAtThisLevelSafe(int level) {
        while (usersByLevels.size() <= level) {
            usersByLevels.add(new ArrayList<String>());
        }
        return usersByLevels.get(level);
    }

    protected List<String> getOrgsAtThisLevelSafe(int level) {
        while (orgsByLevels.size() <= level) {
            orgsByLevels.add(new ArrayList<String>());
        }
        return orgsByLevels.get(level);
    }


//    // todo better name
//    protected void prepareOrgStructureOids(int level, String parentOid, int[] orgChildrenInLevel, int[] userChildrenInLevel, int[] parentsInLevel, String oidPrefix,
//                                    OperationResult result) throws Exception {
//        if (level == orgChildrenInLevel.length) {
//            return;
//        }
//
//        List<String> orgsAtThisLevel = getOrgsAtThisLevelSafe(level);
//        for (int i = 0; i < orgChildrenInLevel[level]; i++) {
//            String newOidPrefix = getOidCharFor(i) + oidPrefix;
//            int numberOfParents = parentsInLevel==null ? (parentOid != null ? 1 : 0) : parentsInLevel[level];
//            PrismObject<OrgType> org = createOrg(generateParentsForLevel(parentOid, level, numberOfParents), newOidPrefix);
//            LOGGER.info("'Creating' {}, total {}; parents = {}", new Object[]{org, count, getParentsOids(org)});
//            String oid = org.getOid();
//            if (parentOid == null) {
//                rootOids.add(oid);
//            }
//            allOrgCreated.add(org.asObjectable());
//            registerObject(org.asObjectable(), false);
//            orgsAtThisLevel.add(oid);
//            count++;
//
//            prepareOrgStructureOids(level + 1, oid, orgChildrenInLevel, userChildrenInLevel, parentsInLevel, newOidPrefix, result);
//        }
//
//        if (parentOid != null) {
//
//            List<String> usersAtThisLevel = getUsersAtThisLevelSafe(level);
//
//            for (int u = 0; u < userChildrenInLevel[level]; u++) {
//                int numberOfParents = parentsInLevel==null ? 1 : parentsInLevel[level];
//                PrismObject<UserType> user = createUser(generateParentsForLevel(parentOid, level, numberOfParents), getOidCharFor(u) + ":" + oidPrefix);
//                LOGGER.info("'Creating' {}, total {}; parents = {}", new Object[]{user, count, getParentsOids(user)});
//                String uoid = user.getOid();
//                registerObject(user.asObjectable(), false);
//                usersAtThisLevel.add(uoid);
//                count++;
//            }
//        }
//
//    }

    protected void scanOrgStructure(OperationResult opResult) throws SchemaException, ObjectNotFoundException {

        // determine rootOids
        for (int i = 0; ; i++) {
            String oid = "o" + createOid(""+getOidCharFor(i));
            try {
                System.out.println("Trying to find " + oid + " as a root");
                OrgType org = repositoryService.getObject(OrgType.class, oid, null, opResult).asObjectable();
                rootOids.add(org.getOid());
                allOrgCreated.add(org);
                registerOrgToLevels(0, org.getOid());
                registerObject(org, false);
                objectCount++;
            } catch (ObjectNotFoundException e) {
                break;
            }
        }

        for (String rootOid : rootOids) {
            scanChildren(0, rootOid, opResult);
        }
    }

    protected void registerOrgToLevels(int level, String oid) {
        getOrgsAtThisLevelSafe(level).add(oid);
    }

    protected void registerUserToLevels(int level, String oid) {
        getUsersAtThisLevelSafe(level).add(oid);
    }

    protected void scanChildren(int level, String parentOid, OperationResult opResult) throws SchemaException, ObjectNotFoundException {

        if (level > maxLevel) {
            maxLevel = level;
        }

        List<String> children = getChildren(parentOid);
        for (String childOid : children) {
            if (alreadyKnown(childOid)) {
                continue;
            }
            objectCount++;
            System.out.println("#" + objectCount + ": parent level = " + level + ", childOid = " + childOid);
            ObjectType objectType = repositoryService.getObject(ObjectType.class, childOid, null, opResult).asObjectable();
            if (objectType instanceof OrgType) {
                allOrgCreated.add((OrgType) objectType);
                registerOrgToLevels(level + 1, objectType.getOid());
                registerObject(objectType, false);          // children will be registered to graph later
                scanChildren(level + 1, objectType.getOid(), opResult);
            } else if (objectType instanceof UserType) {
                allUsersCreated.add((UserType) objectType);
                registerUserToLevels(level + 1, objectType.getOid());
            } else {
                throw new IllegalStateException("Object with unexpected type: " + objectType);
            }
        }
    }


    private static String SPECIAL="!@#$%^&*()";
    protected char getOidCharFor(int i) {
        if (i < 10) {
            return (char) ('0'+i);
        } else if (i < 36) {
            return (char) ('A'+i-10);
        } else if (i < 46) {
            return SPECIAL.charAt(i-36);
        } else {
            throw new IllegalArgumentException("Too many items in a level: " + i);
        }
    }

    protected Collection<String> getParentsOids(PrismObject<? extends ObjectType> object) {
        List<String> retval = new ArrayList<String>();
        for(ObjectReferenceType objectReferenceType : object.asObjectable().getParentOrgRef()) {
            retval.add(objectReferenceType.getOid());
        }
        return retval;
    }

    private List<String> generateParentsForLevel(String explicitParentOid, int level, int totalParents) {
        List<String> rv = new ArrayList<>();
        if (totalParents == 0) {
            return rv;
        }
        List<String> potentialParents = level > 0 ? new ArrayList<String>(orgsByLevels.get(level-1)) : new ArrayList<String>();
        if (explicitParentOid != null) {
            rv.add(explicitParentOid);
            potentialParents.remove(explicitParentOid);
            totalParents--;
        }
        while (totalParents > 0 && !potentialParents.isEmpty()) {
            int i = (int) Math.floor(Math.random()*potentialParents.size());
            rv.add(potentialParents.get(i));
            potentialParents.remove(i);
            totalParents--;
        }
        return rv;
    }

    protected void registerObject(ObjectType objectType, boolean registerChildrenLinks) {
        if (!(objectType instanceof OrgType)) {
            return;
        }
        String oid = objectType.getOid();
        LOGGER.info("Registering {} into memory graph", oid);
        registerVertexIfNeeded(oid);
        for (ObjectReferenceType ort : objectType.getParentOrgRef()) {
            registerVertexIfNeeded(ort.getOid());
            try {
                orgGraph.addEdge(oid, ort.getOid());
            } catch (RuntimeException e) {
                System.err.println("Couldn't add edge " + oid + " -> " + ort.getOid() + " into the graph");
                throw e;
            }
        }

        if (registerChildrenLinks) {
            // let's check for existing children
            List<String> children = getOrgChildren(oid);
            LOGGER.info("Registering children of {}: {} into memory graph", oid, children);
            for (String child : children) {
                registerVertexIfNeeded(child);
                orgGraph.addEdge(child, oid);
            }
        }
        LOGGER.info("Registration of {} done.", oid);
    }

    private void registerVertexIfNeeded(String oid) {
        if (!orgGraph.containsVertex(oid)) {
            orgGraph.addVertex(oid);
        }
    }

    protected List<String> getChildren(String oid) {
        Query childrenQuery = getSession().createQuery("select distinct ownerOid from RObjectReference where targetOid=:oid and referenceType=0");
        childrenQuery.setParameter("oid", oid);
        return childrenQuery.list();
    }

    private List<String> getOrgChildren(String oid) {
        Query childrenQuery = getSession().createQuery("select distinct parentRef.ownerOid from RObjectReference as parentRef" +
                " join parentRef.owner as owner where parentRef.targetOid=:oid and parentRef.referenceType=0" +
                " and owner.objectTypeClass = :orgType");
        childrenQuery.setParameter("orgType", RObjectType.ORG);         // TODO eliminate use of parameter here
        childrenQuery.setParameter("oid", oid);
        return childrenQuery.list();
    }


    protected void removeOrgStructure(OperationResult result) throws Exception {
        for (String rootOid : rootOids) {
            removeOrgStructure(rootOid, result);
        }
    }

    protected void removeOrgStructure(String nodeOid, OperationResult result) throws Exception {
        removeUsersFromOrg(nodeOid, result);
        ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
                .isDirectChildOf(nodeOid)
                .build();
        List<PrismObject<OrgType>> subOrgs = repositoryService.searchObjects(OrgType.class, query, null, result);
        for (PrismObject<OrgType> subOrg : subOrgs) {
            removeOrgStructure(subOrg.getOid(), result);
        }
        try {
            repositoryService.deleteObject(OrgType.class, nodeOid, result);
        } catch (Exception e) {
            System.err.println("error while deleting " + nodeOid + ": " + e.getMessage());
        }
        LOGGER.trace("Org " + nodeOid + " was removed");
    }

    protected void removeUsersFromOrg(String nodeOid, OperationResult result) throws Exception {
        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .isDirectChildOf(nodeOid)
                .build();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        for (PrismObject<UserType> user : users) {
            try {
                repositoryService.deleteObject(UserType.class, user.getOid(), result);
                LOGGER.trace("User " + user.getOid() + " was removed");
            } catch (Exception e) {
                System.err.println("error while deleting " + user.getOid() + ": " + e.getMessage());
            }
        }
    }

    protected void randomRemoveOrgStructure(OperationResult result) throws Exception {
        int count = 0;
        long totalTime = 0;
        List<String> vertices = new ArrayList<>(getVertices());
        while (!vertices.isEmpty()) {
            int i = (int) Math.floor(vertices.size()*Math.random());
            String oid = vertices.get(i);
            Class<? extends ObjectType> clazz = oid.startsWith("o") ? OrgType.class : UserType.class;       // hack!
            try {
                repositoryService.deleteObject(clazz, oid, result);
                count++;
                totalTime += getNetDuration();
                System.out.println("#" + count + ": " + oid + " deleted in " + getNetDuration() + " ms (net), remaining: " + (vertices.size() - 1));
            } catch (Exception e) {
                System.err.println("Error deleting " + oid + ": " + e.getMessage());
            }
            orgGraph.removeVertex(oid);
            vertices.remove(oid);
            if (count%getConfiguration().getDeletionsToClosureTest() == 0) {
                checkClosure(getVertices());
            }
        }
        System.out.println(count + " objects deleted in avg time " + ((float) totalTime/count) + " ms (net)");
    }

    protected PrismObject<UserType> createUser(List<String> parentOids, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("u" + createOid(oidPrefix));
        user.setName(createPolyString("u" + oidPrefix));
        user.setFullName(createPolyString("fu" + oidPrefix));
        user.setFamilyName(createPolyString("fa" + oidPrefix));
        user.setGivenName(createPolyString("gi" + oidPrefix));
        if (parentOids != null) {
            for (String parentOid : parentOids) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(parentOid);
                ref.setType(OrgType.COMPLEX_TYPE);
                user.getParentOrgRef().add(ref);
            }
        }

        PrismObject<UserType> object = user.asPrismObject();
        prismContext.adopt(user);

        addExtensionProperty(object, "shipName", "Ship " + oidPrefix);
        addExtensionProperty(object, "weapon", "weapon " + oidPrefix);
        //addExtensionProperty(object, "loot", oidPrefix);
        addExtensionProperty(object, "funeralDate", XMLGregorianCalendarType.asXMLGregorianCalendar(new Date()));

        return object;
    }

    protected void addExtensionProperty(PrismObject object, String name, Object value) throws SchemaException {
        String NS = "http://example.com/p";
        PrismProperty p = object.findOrCreateProperty(new ItemPath(UserType.F_EXTENSION, new QName(NS, name)));
        p.setRealValue(value);
    }

    protected PrismObject<OrgType> createOrg(List<String> parentOids, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("o" + createOid(oidPrefix));
        org.setDisplayName(createPolyString("o" + oidPrefix));
        org.setName(createPolyString("o" + oidPrefix));
        if (parentOids != null) {
            for (String parentOid : parentOids) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setOid(parentOid);
                ref.setType(OrgType.COMPLEX_TYPE);
                org.getParentOrgRef().add(ref);
            }
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    protected String createOid(String oidPrefix) {
        String oid = StringUtils.rightPad(oidPrefix, 31, '.');

        StringBuilder sb = new StringBuilder();
        sb.append(oid.substring(0, 7));
        sb.append('-');
        sb.append(oid.substring(7, 11));
        sb.append('-');
        sb.append(oid.substring(11, 15));
        sb.append('-');
        sb.append(oid.substring(15, 19));
        sb.append('-');
        sb.append(oid.substring(19, 31));

        return sb.toString();
    }

    protected PolyStringType createPolyString(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }

    protected boolean alreadyKnown(String oid) {
        return knownIn(orgsByLevels, oid) || knownIn(usersByLevels, oid);
    }

    private boolean knownIn(List<List<String>> byLevels, String oid) {
        for (List<String> oneLevel : byLevels) {
            if (oneLevel.contains(oid)) {
                return true;
            }
        }
        return false;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    protected long getNetDuration() {
        return closureManager.getLastOperationDuration();
    }

    public abstract OrgClosureTestConfiguration getConfiguration();

    protected void _test100LoadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test100LoadOrgStructure ]===");

        LOGGER.info("Start.");

        long start = System.currentTimeMillis();
        loadOrgStructure(0, null, "", opResult);
        System.out.println("Loaded " + allOrgCreated.size() + " orgs and " + (objectCount - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        Query q = getSession().createNativeQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
        closureSize = Long.parseLong(q.list().get(0).toString());
    }

    protected void _test110ScanOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test110ScanOrgStructure ]===");

        long start = System.currentTimeMillis();
        scanOrgStructure(opResult);
        System.out.println("Found " + allOrgCreated.size() + " orgs and " + (objectCount - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        Query q = getSession().createNativeQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
        closureSize = Long.parseLong(q.list().get(0).toString());
    }

    protected void _test150CheckClosure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test110CheckClosure ]===");
        checkClosureUnconditional(getVertices());
    }

    protected synchronized Set<String> getVertices() {
        return new HashSet<>(orgGraph.vertexSet());
    }

    protected void _test190AddLink(String childOid, String parentOid) throws Exception {
        OperationResult opResult = new OperationResult("===[ test190AddLink ]===");

        //checkClosure(orgGraph.vertexSet());

        ObjectType child = repositoryService.getObject(ObjectType.class, childOid, null, opResult).asObjectable();
        ObjectReferenceType parentOrgRef = new ObjectReferenceType();
        parentOrgRef.setOid(parentOid);
        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
        System.out.println("Adding link " + childOid + " -> " + parentOid);
        long start = System.currentTimeMillis();
        if (child instanceof OrgType) {
            addOrgParent((OrgType) child, parentOrgRef, false, opResult);
        } else {
            addUserParent((UserType) child, parentOrgRef, opResult);
        }
        long timeAddition = System.currentTimeMillis() - start;
        System.out.println(" ... done in " + timeAddition + " ms" + getNetDurationMessage());

        //checkClosure(orgGraph.vertexSet());
    }


    protected void _test195RemoveLink(String childOid, String parentOid) throws Exception {
        OperationResult opResult = new OperationResult("===[ test195RemoveLink ]===");

        //checkClosure(orgGraph.vertexSet());

        System.out.println("Removing link " + childOid + " -> " + parentOid);
        ObjectType child = repositoryService.getObject(ObjectType.class, childOid, null, opResult).asObjectable();
        ObjectReferenceType parentOrgRef = null;
        for (ObjectReferenceType ort : child.getParentOrgRef()) {
            if (parentOid.equals(ort.getOid())) {
                parentOrgRef = ort;
            }
        }
        assertNotNull(parentOid + " is not a parent of " + childOid, parentOrgRef);
        long start = System.currentTimeMillis();
        removeObjectParent(child, parentOrgRef, false, opResult);
        long timeAddition = System.currentTimeMillis() - start;
        System.out.println(" ... done in " + timeAddition + " ms" + getNetDurationMessage());

        //checkClosure(orgGraph.vertexSet());
    }

    protected String getNetDurationMessage() {
        return " (closure update: " + getNetDuration() + " ms)";
    }

    protected void _test200AddRemoveLinks() throws Exception {
        _test200AddRemoveLinks(false);
    }

    protected void _test200AddRemoveLinks(boolean useReplace) throws Exception {
        OperationResult opResult = new OperationResult("===[ addRemoveLinks ]===");

        int totalRounds = 0;
        OrgClosureStatistics stat = new OrgClosureStatistics();

        // parentRef link removal + addition
        long totalTimeLinkRemovals = 0, totalTimeLinkAdditions = 0;
        for (int level = 0; level < getConfiguration().getLinkRoundsForLevel().length; level++) {
            for (int round = 0; round < getConfiguration().getLinkRoundsForLevel()[level]; round++) {

                // removal
                List<String> levelOids = orgsByLevels.get(level);
                if (levelOids.isEmpty()) {
                    continue;
                }
                int index = (int) Math.floor(Math.random() * levelOids.size());
                String oid = levelOids.get(index);
                OrgType org = repositoryService.getObject(OrgType.class, oid, null, opResult).asObjectable();

                // check if it has no parents (shouldn't occur here!)
                if (org.getParentOrgRef().isEmpty()) {
                    throw new IllegalStateException("No parents in " + org);
                }

                int i = (int) Math.floor(Math.random() * org.getParentOrgRef().size());
                ObjectReferenceType parentOrgRef = org.getParentOrgRef().get(i);

                info("Removing parent from org #" + totalRounds + "(" + level + "/" + round + "): "
                        + org.getOid() + ", parent: " + parentOrgRef.getOid()
                        + (useReplace ? " using replace" : ""));
                long start = System.currentTimeMillis();
                removeObjectParent(org, parentOrgRef, useReplace, opResult);
                long timeRemoval = System.currentTimeMillis() - start;
                info(" ... done in " + timeRemoval + " ms " + getNetDurationMessage());
                stat.recordExtended(baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks", level, false, getNetDuration());
                totalTimeLinkRemovals += getNetDuration();

                checkClosure(getVertices());

                // addition
                info("Re-adding parent for org #" + totalRounds + (useReplace ? " using replace" : ""));
                start = System.currentTimeMillis();
                addOrgParent(org, parentOrgRef, useReplace, opResult);
                long timeAddition = System.currentTimeMillis() - start;
                info(" ... done in " + timeAddition + " ms " + getNetDurationMessage());
                stat.recordExtended(baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks", level, true, getNetDuration());

                checkClosure(getVertices());

                totalTimeLinkAdditions += getNetDuration();
                totalRounds++;
            }
        }

        if (totalRounds > 0) {
            System.out.println("Avg time for an arbitrary link removal: " + ((double) totalTimeLinkRemovals / totalRounds) + " ms");
            System.out.println("Avg time for an arbitrary link re-addition: " + ((double) totalTimeLinkAdditions / totalRounds) + " ms");
            LOGGER.info("===================================================");
            LOGGER.info("Statistics for org link removal/addition:");
            stat.dump(LOGGER, baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks");
        }
    }

    protected void _test300AddRemoveOrgs() throws Exception {
        OperationResult opResult = new OperationResult("===[ test300AddRemoveOrgs ]===");

        int totalRounds = 0;
        OrgClosureStatistics stat = new OrgClosureStatistics();

        // OrgType node removal + addition
        long totalTimeNodeRemovals = 0, totalTimeNodeAdditions = 0;
        for (int level = 0; level < getConfiguration().getNodeRoundsForLevel().length; level++) {
            for (int round = 0; round < getConfiguration().getNodeRoundsForLevel()[level]; round++) {

                // removal
                List<String> levelOids = orgsByLevels.get(level);
                if (levelOids.isEmpty()) {
                    continue;
                }
                int index = (int) Math.floor(Math.random() * levelOids.size());
                String oid = levelOids.get(index);
                OrgType org = repositoryService.getObject(OrgType.class, oid, null, opResult).asObjectable();

                System.out.println("Removing org #" + totalRounds + " (" + level + "/" + round + "): " + org.getOid() + " (parents: " + getParentsOids(org.asPrismObject()) + ")");
                long start = System.currentTimeMillis();
                removeOrg(org.getOid(), opResult);
                long timeRemoval = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeRemoval + " ms" + getNetDurationMessage());
                stat.recordExtended(baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs", level, false, getNetDuration());
                totalTimeNodeRemovals += getNetDuration();

                checkClosure(getVertices());

                // addition
                System.out.println("Re-adding org #" + totalRounds);
                start = System.currentTimeMillis();
                reAddOrg(org, opResult);
                long timeAddition = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeAddition + "ms" + getNetDurationMessage());
                stat.recordExtended(baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs", level, true, getNetDuration());

                checkClosure(getVertices());

                totalTimeNodeAdditions += getNetDuration();
                totalRounds++;
            }
        }

        if (totalRounds > 0) {
            System.out.println("Avg time for an arbitrary node removal: " + ((double) totalTimeNodeRemovals / totalRounds) + " ms");
            System.out.println("Avg time for an arbitrary node re-addition: " + ((double) totalTimeNodeAdditions / totalRounds) + " ms");
            LOGGER.info("===================================================");
            LOGGER.info("Statistics for org node removal/addition:");
            stat.dump(LOGGER, baseHelper.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs");
        }
    }

    protected void _test390CyclePrevention() throws Exception {
        OperationResult opResult = new OperationResult("===[ test390CyclePrevention ]===");
        String childOid = orgsByLevels.get(1).get(0);       // we hope it exists

        OrgType child = repositoryService.getObject(OrgType.class, childOid, null, opResult).asObjectable();
        ObjectReferenceType parentOrgRef = child.getParentOrgRef().get(0);      // we hope it exists too
        String parentOid = parentOrgRef.getOid();

        System.out.println("Adding cycle-introducing link from " + parentOid + " to " + childOid);
        List<ItemDelta> modifications = new ArrayList<>();
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setOid(childOid);
        ort.setType(OrgType.COMPLEX_TYPE);
        ItemDelta addParent = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, ort.asReferenceValue());
        modifications.add(addParent);
        try {
            repositoryService.modifyObject(OrgType.class, parentOid, modifications, opResult);
            throw new AssertionError("Cycle-introducing link from " + parentOid + " to " + childOid + " was successfully added!");
        } catch (Exception e) {
            // ok, expected
            System.out.println("Got exception (as expected): " + e);        // would be fine to check the kind of exception...
        }

        checkClosure(getVertices());
    }

    protected void _test400UnloadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ unloadOrgStruct ]===");
        long start = System.currentTimeMillis();
        removeOrgStructure(opResult);
        System.out.println("Removed in " + (System.currentTimeMillis() - start) + " ms");

        Query q = getSession().createNativeQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");

        LOGGER.info("Finish.");
    }

    protected void _test410RandomUnloadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test410RandomUnloadOrgStructure ]===");
        long start = System.currentTimeMillis();
        randomRemoveOrgStructure(opResult);
        System.out.println("Removed in " + (System.currentTimeMillis() - start) + " ms");

        Query q = getSession().createNativeQuery("select count(*) from m_org_closure");
        Object count = q.list().get(0);
        System.out.println("OrgClosure table has " + count + " rows");
        assertEquals("Closure is not empty", "0", count.toString());

        LOGGER.info("Finish.");
    }

    protected void info(String s) {
        System.out.println(s);
        LOGGER.info(s);
    }


}
