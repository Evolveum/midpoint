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

package com.evolveum.midpoint.repo.sql;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author lazyman
 */
public abstract class AbstractOrgClosureTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractOrgClosureTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    protected int count = 0;

    protected List<String> rootOids = new ArrayList<>();

    protected List<OrgType> allOrgCreated = new ArrayList<>();

    protected List<UserType> allUsersCreated = new ArrayList<>();

    protected List<List<String>> orgsByLevels = new ArrayList<>();

    protected List<List<String>> usersByLevels = new ArrayList<>();

    protected SimpleDirectedGraph<String, DefaultEdge> orgGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    protected Session session;            // used exclusively for read-only operations

    private int maxLevel = 0;

    protected void openSessionIfNeeded() {
        if (session == null || !session.isConnected()) {
            session = repositoryService.getSessionFactory().openSession();
        }
    }

    protected void checkClosure(Set<String> oidsToCheck) {
        if (isCheckClosureMatrix()) {
            openSessionIfNeeded();
            checkClosureMatrix(session);
        }
        if (isCheckChildrenSets()) {
            openSessionIfNeeded();
            checkChildrenSets(oidsToCheck);
        }
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
    protected void checkClosureMatrix(Session session) {
        // we compute the closure table "by hand" as 1 + A + A^2 + A^3 + ... + A^n where n is the greatest expected path length
        int vertices = orgGraph.vertexSet().size();

        long start = System.currentTimeMillis();

        // used to give indices to vertices
        List<String> vertexList = new ArrayList<>(orgGraph.vertexSet());
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

        Query q = session.createSQLQuery("select descendant_oid, ancestor_oid, val from m_org_closure");
        List<Object[]> list = q.list();
        LOGGER.info("OrgClosure has {} rows", list.size());

        DoubleMatrix2D closureInDatabase = new SparseDoubleMatrix2D(vertices, vertices);
        for (Object[] item : list) {
            closureInDatabase.set(vertexList.indexOf(item[0]),
                    vertexList.indexOf(item[1]),
                    (Integer) item[2]);
        }

        double zSumResultBefore = result.zSum();
        double zSumClosureInDb = closureInDatabase.zSum();
        result.assign(closureInDatabase, Functions.minus);
        double zSumResultAfter = result.zSum();
        LOGGER.info("Summary of items in closure computed: {}, in DB-stored closure: {}, delta: {}", new Object[]{zSumResultBefore, zSumClosureInDb, zSumResultAfter});

        boolean problem = false;
        for (int i = 0; i < vertices; i++) {
            for (int j = 0; j < vertices; j++) {
                double delta = result.get(i, j);
                if (Math.round(delta) != 0) {
                    System.err.println("delta("+vertexList.get(i)+","+vertexList.get(j)+") = " + delta);
                    LOGGER.error("delta("+vertexList.get(i)+","+vertexList.get(j)+") = " + delta);
                    problem = true;
                }
            }
        }
        assertFalse("Difference found", problem);
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
        Query query = session.createQuery("from ROrgClosure where descendantOid=:oid");
        query.setString("oid", descendantOid);
        return query.list();
    }

    private List<ROrgClosure> getOrgClosureByAncestor(String ancestorOid) {
        Query query = session.createQuery("from ROrgClosure where ancestorOid=:oid");
        query.setString("oid", ancestorOid);
        return query.list();
    }

    protected void removeObjectParent(ObjectType object, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta removeParent = ReferenceDelta.createModificationDelete(object.getClass(), OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(removeParent);
        repositoryService.modifyObject(object.getClass(), object.getOid(), modifications, opResult);
        orgGraph.removeEdge(object.getOid(), existingValue.getOid());
    }

    // TODO generalzie to addObjectParent
    protected void addOrgParent(OrgType org, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta readdParent = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(readdParent);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.addEdge(org.getOid(), existingValue.getOid());
    }

    protected void addUserParent(UserType user, ObjectReferenceType parentOrgRef, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = parentOrgRef.asReferenceValue();
        ItemDelta readdParent = ReferenceDelta.createModificationAdd(UserType.class, UserType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(readdParent);
        repositoryService.modifyObject(UserType.class, user.getOid(), modifications, opResult);
        orgGraph.addEdge(user.getOid(), existingValue.getOid());
    }

    protected void removeOrg(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(OrgType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    protected void removeUser(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(UserType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    protected void reAddOrg(OrgType org, OperationResult opResult) throws Exception {
        repositoryService.addObject(org.asPrismObject(), null, opResult);
        registerObject(org, true);
    }

    protected void reAddUser(UserType user, OperationResult opResult) throws Exception {
        repositoryService.addObject(user.asPrismObject(), null, opResult);
        registerObject(user, false);
    }

    // parentsInLevel may be null (in that case, a simple tree is generated)
    protected void loadOrgStructure(int level, String parentOid, int[] orgChildrenInLevel, int[] userChildrenInLevel, int[] parentsInLevel, String oidPrefix,
                                  OperationResult result) throws Exception {
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
            LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{org, count, getParentsOids(org)});
            String oid = repositoryService.addObject(org, null, result);
            org.setOid(oid);
            if (parentOid == null) {
                rootOids.add(oid);
            }
            allOrgCreated.add(org.asObjectable());
            registerObject(org.asObjectable(), false);
            orgsAtThisLevel.add(oid);
            count++;

            loadOrgStructure(level+1, oid, orgChildrenInLevel, userChildrenInLevel, parentsInLevel, newOidPrefix, result);
        }

        if (parentOid != null) {

            List<String> usersAtThisLevel = getUsersAtThisLevelSafe(level);

            for (int u = 0; u < userChildrenInLevel[level]; u++) {
                int numberOfParents = parentsInLevel==null ? 1 : parentsInLevel[level];
                PrismObject<UserType> user = createUser(generateParentsForLevel(parentOid, level, numberOfParents), getOidCharFor(u) + ":" + oidPrefix);
                LOGGER.info("Creating {}, total {}; parents = {}", new Object[]{user, count, getParentsOids(user)});
                String uoid = repositoryService.addObject(user, null, result);
                user.setOid(uoid);
                allUsersCreated.add(user.asObjectable());
                registerObject(user.asObjectable(), false);
                usersAtThisLevel.add(uoid);
                count++;
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
                count++;
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
            count++;
            System.out.println("#" + count + ": parent level = " + level + ", childOid = " + childOid);
            ObjectType objectType = repositoryService.getObject(ObjectType.class, childOid, null, opResult).asObjectable();
            registerObject(objectType, false);          // children will be registered to graph later
            if (objectType instanceof OrgType) {
                allOrgCreated.add((OrgType) objectType);
                registerOrgToLevels(level+1, objectType.getOid());
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
        String oid = objectType.getOid();
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
            List<String> children = getChildren(oid);
            for (String child : children) {
                registerVertexIfNeeded(child);
                orgGraph.addEdge(child, oid);
            }
        }
    }

    private void registerVertexIfNeeded(String oid) {
        if (!orgGraph.containsVertex(oid)) {
            orgGraph.addVertex(oid);
        }
    }

    protected List<String> getChildren(String oid) {
        Query childrenQuery = session.createQuery("select distinct ownerOid from RParentOrgRef where targetOid=:oid");
        childrenQuery.setString("oid", oid);
        return childrenQuery.list();
    }


    protected void removeOrgStructure(OperationResult result) throws Exception {
        for (String rootOid : rootOids) {
            removeOrgStructure(rootOid, result);
        }
    }

    protected void removeOrgStructure(String nodeOid, OperationResult result) throws Exception {
        removeUsersFromOrg(nodeOid, result);
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
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
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
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
        openSessionIfNeeded();

        int count = 0;
        long totalTime = 0;
        List<String> vertices = new ArrayList<>(orgGraph.vertexSet());
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
            checkClosureMatrix(session);
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
        return repositoryService.getClosureManager().getLastOperationDuration();
    }

    public abstract boolean isCheckChildrenSets();

    public abstract boolean isCheckClosureMatrix();
}
