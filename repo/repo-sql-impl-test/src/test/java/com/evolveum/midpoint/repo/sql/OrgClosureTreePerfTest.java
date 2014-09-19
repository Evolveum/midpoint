/*
 * Copyright (c) 2010-2013 Evolveum
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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureTreePerfTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureTreePerfTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    //50531 OU, 810155 U
//    private static final int[] TREE_LEVELS = {1, 5, 5, 20, 20, 4};
//    private static final int[] TREE_LEVELS_USERS = {5, 10, 4, 20, 20, 15};

    //1191 OU, 10943 U  =>  428585 queries ~ 6min, h2
//    private static final int[] TREE_LEVELS = {1, 5, 3, 3, 5, 4};
//    private static final int[] TREE_LEVELS_USERS = {3, 4, 5, 6, 7, 10};

    /*
        H2

        Loaded 1191 orgs and 10943 users in 199693 ms
        OrgClosure table has 80927 rows
        Avg time for an arbitrary link removal: 399.3 ms
        Avg time for an arbitrary link re-addition: 2728.85 ms
        Avg time for an arbitrary node removal: 3076.8 ms
        Avg time for an arbitrary node re-addition: 3672.65 ms
        Removed in 164146 ms

        PostgreSQL

        Loaded 1191 orgs and 10943 users in 239635 ms
        OrgClosure table has 80927 rows
        Avg time for an arbitrary link removal: 57.25 ms
        Avg time for an arbitrary link re-addition: 106.6 ms
        Avg time for an arbitrary node removal: 271.25 ms
        Avg time for an arbitrary node re-addition: 308.9 ms
        Removed in 333095 ms
     */

    private static final int[] TREE_LEVELS = {1, 2, 3, 4, 5};
    private static final int[] TREE_LEVELS_USERS = {1, 2, 3, 4, 5};

    /*
        H2

        Loaded 153 orgs and 719 users in 16676 ms
        OrgClosure table has 4885 rows
        Avg time for an arbitrary link removal: 47.75 ms
        Avg time for an arbitrary link re-addition: 50.35 ms
        Avg time for an arbitrary node removal: 144.6 ms
        Avg time for an arbitrary node re-addition: 87.45 ms
        Removed in 15238 ms

        PostgreSQL

        Loaded 153 orgs and 719 users in 16852 ms
        OrgClosure table has 4885 rows
        Avg time for an arbitrary link removal: 32.15 ms
        Avg time for an arbitrary link re-addition: 46.35 ms
        Avg time for an arbitrary node removal: 95.65 ms
        Avg time for an arbitrary node re-addition: 77.3 ms
        Removed in 25519 ms

     */

    //9 OU, 23 U        =>  773 queries ~ 50s, h2
//    private static final int[] TREE_LEVELS = {1, 2, 3};
//    private static final int[] TREE_LEVELS_USERS = {1, 2, 3};

    /*
        H2

        Loaded 9 orgs and 23 users in 1171ms
        OrgClosure table has 109 rows
        Avg time for an arbitrary link removal: 56.8 ms
        Avg time for an arbitrary link re-addition: 59.6 ms
        Avg time for an arbitrary node removal: 75.3 ms
        Avg time for an arbitrary node re-addition: 49.1 ms
        Removed in 872ms

        PostgreSQL

     */

    private int count = 0;

    private List<String> rootOids = new ArrayList<>();

    private List<OrgType> allOrgCreated = new ArrayList<>();

    private SimpleDirectedGraph<String, DefaultEdge> orgGraph = new SimpleDirectedGraph<>(DefaultEdge.class);

    @Test(enabled = true)
    public void test100LoadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ loadOrgStruct ]===");

        LOGGER.info("Start.");

        long start = System.currentTimeMillis();
        loadOrgStructure(null, TREE_LEVELS, TREE_LEVELS_USERS, "", opResult);
        System.out.println("Loaded " + allOrgCreated.size() + " orgs and " + (count - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        Session session = repositoryService.getSessionFactory().openSession();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
        session.close();
    }

    @Test(enabled = true)
    public void test110CheckClosure() throws Exception {
        checkClosure();
    }

    private void checkClosure() {
        OperationResult opResult = new OperationResult("===[ test110CheckClosure ]===");
        Session session = repositoryService.getSessionFactory().openSession();
        SimpleDirectedGraph<String,DefaultEdge> tc = (SimpleDirectedGraph) orgGraph.clone();
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(tc);
        for (String subroot : tc.vertexSet()) {
            LOGGER.info("Checking descendants of {}", subroot);
            Set<String> expectedChildren = new HashSet<>();
            for (DefaultEdge edge : tc.incomingEdgesOf(subroot)) {
                expectedChildren.add(tc.getEdgeSource(edge));
            }
            expectedChildren.add(subroot);
            LOGGER.trace("Expected children: {}", expectedChildren);
            Set<String> actualChildren = getActualChildrenOf(subroot, session);
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
        session.close();
    }

    private Set<String> getActualChildrenOf(String ancestor, Session session) {
        List<ROrgClosure> descendantRecords = getOrgClosureByAncestor(ancestor, session);
        Set<String> rv = new HashSet<String>();
        for (ROrgClosure c : descendantRecords) {
            rv.add(c.getDescendantOid());
        }
        return rv;
    }

    private List<ROrgClosure> getOrgClosureByDescendant(String descendantOid, Session session) {
        Query query = session.createQuery("from ROrgClosure where descendantOid=:oid");
        query.setString("oid", descendantOid);
        return query.list();
    }

    private List<ROrgClosure> getOrgClosureByAncestor(String ancestorOid, Session session) {
        Query query = session.createQuery("from ROrgClosure where ancestorOid=:oid");
        query.setString("oid", ancestorOid);
        return query.list();
    }

    @Test(enabled = true)
    public void test200AddRemoveLinks() throws Exception {
        OperationResult opResult = new OperationResult("===[ addRemoveLinks ]===");

        final int LINK_ROUNDS = 20;

        // parentRef link removal + addition
        long totalTimeLinkRemovals = 0, totalTimeLinkAdditions = 0;
        for (int round = 1; round <= LINK_ROUNDS; round++) {

            // removal
            System.out.println("Removing parent from org #" + round);
            int index = 1 + (int) Math.floor(Math.random() * (allOrgCreated.size() - 1));          // assuming node 0 is the root
            OrgType org = allOrgCreated.get(index);

            // check if it's a root (by chance)
            if (org.getParentOrgRef().isEmpty()) {
                round--;
                continue;
            }

            long start = System.currentTimeMillis();
            removeOrgParent(org, opResult);
            long timeRemoval = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeRemoval + " ms");

            checkClosure();

            // addition
            System.out.println("Re-adding parent for org #" + round);
            start = System.currentTimeMillis();
            addOrgParent(org, opResult);
            long timeAddition = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeAddition + " ms");

            checkClosure();

            totalTimeLinkRemovals += timeRemoval;
            totalTimeLinkAdditions += timeAddition;
        }

        if (LINK_ROUNDS > 0) {
            System.out.println("Avg time for an arbitrary link removal: " + ((double) totalTimeLinkRemovals / LINK_ROUNDS) + " ms");
            System.out.println("Avg time for an arbitrary link re-addition: " + ((double) totalTimeLinkAdditions / LINK_ROUNDS) + " ms");
        }
    }

    @Test(enabled = true)
    public void test300AddRemoveNodes() throws Exception {
        OperationResult opResult = new OperationResult("===[ addRemoveNodes ]===");

        final int NODE_ROUNDS = 20;

        // OrgType node removal + addition
        long totalTimeNodeRemovals = 0, totalTimeNodeAdditions = 0;
        for (int round = 1; round <= NODE_ROUNDS; round++) {

            // removal
            System.out.println("Removing org #" + round);
            int index = (int) Math.floor(Math.random() * allOrgCreated.size());
            OrgType org = allOrgCreated.get(index);
            long start = System.currentTimeMillis();
            removeOrg(org.getOid(), opResult);
            long timeRemoval = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeRemoval + " ms");

            //checkClosure();

            // addition
            System.out.println("Re-adding org #" + round);
            start = System.currentTimeMillis();
            addOrg(org, opResult);
            long timeAddition = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeAddition + "ms");

            //checkClosure();

            totalTimeNodeRemovals += timeRemoval;
            totalTimeNodeAdditions += timeAddition;
        }

        if (NODE_ROUNDS > 0) {
            System.out.println("Avg time for an arbitrary node removal: " + ((double) totalTimeNodeRemovals / NODE_ROUNDS) + " ms");
            System.out.println("Avg time for an arbitrary node re-addition: " + ((double) totalTimeNodeAdditions / NODE_ROUNDS) + " ms");
        }
    }

    @Test(enabled = true)
    public void test400UnloadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ unloadOrgStruct ]===");
        long start = System.currentTimeMillis();
        removeOrgStructure(opResult);
        System.out.println("Removed in " + (System.currentTimeMillis() - start) + " ms");

        Session session = repositoryService.getSessionFactory().openSession();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");

        LOGGER.info("Finish.");
    }

    private void removeOrgParent(OrgType org, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = org.getParentOrgRef().get(0).asReferenceValue();
        ItemDelta removeParent = ReferenceDelta.createModificationDelete(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(removeParent);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.removeEdge(org.getOid(), existingValue.getOid());
    }

    private void addOrgParent(OrgType org, OperationResult opResult) throws Exception {
        List<ItemDelta> modifications = new ArrayList<>();
        PrismReferenceValue existingValue = org.getParentOrgRef().get(0).asReferenceValue();
        ItemDelta readdParent = ReferenceDelta.createModificationAdd(OrgType.class, OrgType.F_PARENT_ORG_REF, prismContext, existingValue.clone());
        modifications.add(readdParent);
        repositoryService.modifyObject(OrgType.class, org.getOid(), modifications, opResult);
        orgGraph.addEdge(org.getOid(), existingValue.getOid());
    }

    private void removeOrg(String oid, OperationResult opResult) throws Exception {
        repositoryService.deleteObject(OrgType.class, oid, opResult);
        orgGraph.removeVertex(oid);
    }

    private void addOrg(OrgType org, OperationResult opResult) throws Exception {
        repositoryService.addObject(org.asPrismObject(), null, opResult);
        registerObject(org);
    }

    private void loadOrgStructure(String parentOid, int[] TREE_SIZE, int[] USER_SIZE, String oidPrefix,
                                  OperationResult result) throws Exception {
        if (TREE_SIZE.length == 0) {
            return;
        }

        for (int i = 0; i < TREE_SIZE[0]; i++) {
            String newOidPrefix = i + oidPrefix;
            PrismObject<OrgType> org = createOrg(parentOid, newOidPrefix);
            LOGGER.info("Creating {}, total {}", org, count);
            String oid = repositoryService.addObject(org, null, result);
            org.setOid(oid);
            if (parentOid == null) {
                rootOids.add(oid);
            }
            allOrgCreated.add(org.asObjectable());
            registerObject(org.asObjectable());
            count++;

            for (int u = 0; u < USER_SIZE[0]; u++) {
                PrismObject<UserType> user = createUser(oid, u + ":" + newOidPrefix);
                String uoid = repositoryService.addObject(user, null, result);
                user.setOid(uoid);
                registerObject(user.asObjectable());
                count++;
            }

            loadOrgStructure(oid, ArrayUtils.remove(TREE_SIZE, 0), ArrayUtils.remove(USER_SIZE, 0),
                    newOidPrefix, result);
        }
    }

    private void registerObject(ObjectType objectType) {
        orgGraph.addVertex(objectType.getOid());
        for (ObjectReferenceType ort : objectType.getParentOrgRef()) {
            orgGraph.addEdge(objectType.getOid(), ort.getOid());
        }
    }

    private void removeOrgStructure(OperationResult result) throws Exception {
        for (String rootOid : rootOids) {
            removeOrgStructure(rootOid, result);
        }
    }

    private void removeOrgStructure(String nodeOid, OperationResult result) throws Exception {
        removeUsersFromOrg(nodeOid, result);
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
        List<PrismObject<OrgType>> subOrgs = repositoryService.searchObjects(OrgType.class, query, null, result);
        for (PrismObject<OrgType> subOrg : subOrgs) {
            removeOrgStructure(subOrg.getOid(), result);
        }
        repositoryService.deleteObject(OrgType.class, nodeOid, result);
        LOGGER.trace("Org " + nodeOid + " was removed");
    }

    private void removeUsersFromOrg(String nodeOid, OperationResult result) throws Exception {
        ObjectQuery query = new ObjectQuery();
        ObjectFilter filter = OrgFilter.createOrg(nodeOid, OrgFilter.Scope.ONE_LEVEL);
        query.setFilter(filter);
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);
        for (PrismObject<UserType> user : users) {
            repositoryService.deleteObject(UserType.class, user.getOid(), result);
            LOGGER.trace("User " + user.getOid() + " was removed");
        }
    }


    private PrismObject<UserType> createUser(String parentOid, String oidPrefix)
            throws Exception {
        UserType user = new UserType();
        user.setOid("u" + createOid(oidPrefix));
        user.setName(createPolyString("u" + oidPrefix));
        user.setFullName(createPolyString("fu" + oidPrefix));
        user.setFamilyName(createPolyString("fa" + oidPrefix));
        user.setGivenName(createPolyString("gi" + oidPrefix));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            user.getParentOrgRef().add(ref);
        }

        PrismObject<UserType> object = user.asPrismObject();
        prismContext.adopt(user);

        addExtensionProperty(object, "shipName", "Ship " + oidPrefix);
        addExtensionProperty(object, "weapon", "weapon " + oidPrefix);
        //addExtensionProperty(object, "loot", oidPrefix);
        addExtensionProperty(object, "funeralDate", XMLGregorianCalendarType.asXMLGregorianCalendar(new Date()));

        return object;
    }

    private void addExtensionProperty(PrismObject object, String name, Object value) throws SchemaException {
        String NS = "http://example.com/p";
        PrismProperty p = object.findOrCreateProperty(new ItemPath(UserType.F_EXTENSION, new QName(NS, name)));
        p.setRealValue(value);
    }

    private PrismObject<OrgType> createOrg(String parentOid, String oidPrefix)
            throws Exception {
        OrgType org = new OrgType();
        org.setOid("o" + createOid(oidPrefix));
        org.setDisplayName(createPolyString("o" + oidPrefix));
        org.setName(createPolyString("o" + oidPrefix));
        if (parentOid != null) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(parentOid);
            ref.setType(OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);
        }

        prismContext.adopt(org);
        return org.asPrismContainer();
    }

    private String createOid(String oidPrefix) {
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

    private PolyStringType createPolyString(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }
}
