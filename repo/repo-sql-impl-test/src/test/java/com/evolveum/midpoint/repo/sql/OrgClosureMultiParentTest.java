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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.hibernate.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureMultiParentTest extends AbstractOrgClosureTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureMultiParentTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

//    private static final int[] ORG_CHILDREN_IN_LEVEL = {1, 5, 5, 20, 20, 4};
//    private static final int[] USER_CHILDREN_IN_LEVEL = {5, 10, 4, 20, 20, 15};

    // university; it has 13 faculties, each faculty has 30 departments/projects, each department has 5 subdepartments/subprojects
    // in each department/project there is 20 employees
    // employee can have up to 3 relations
//    private static final int[] ORG_CHILDREN_IN_LEVEL  = {1, 13, 30, 5,  0 };
//    private static final int[] USER_CHILDREN_IN_LEVEL = {0,  0,  0, 0, 20 };
//    private static final int[] PARENTS_IN_LEVEL       = {0,  1,  2, 2,  3 };

    // UK!
    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 5, 3, 3, 5, 4,  0};
    private static final int[] USER_CHILDREN_IN_LEVEL = { 0, 3, 4, 5, 6, 7, 10};
    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 2, 2, 2, 2,  2};
    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 10, 15,15,15,15, 0 };
    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 5, 10, 15,15,15,15, 0 };
    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 10,10,20,20,20, 20};

//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 10, 2 ,2 ,2 ,2 , 0 };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 0, 0, 0 ,0 ,0 ,0 , 0 };
//    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 5 ,5 ,5 ,5 ,5 , 5 };
//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 2, 0 ,0 ,0 ,0 , 0 };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 0, 0, 0 ,0 ,0 ,0 , 0 };
//    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 0 ,0 ,0 ,0 ,0 , 0 };

//    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 2, 3, 4, 5, 0};
//    private static final int[] USER_CHILDREN_IN_LEVEL = { 0, 1, 2, 3, 4, 5};
//    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 2, 3, 3, 3};
//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 5, 15,15,15,0 };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 5, 15,15,15,0 };
//    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 10,10,20,20,20};

//    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 2, 3, 0  };
//    private static final int[] USER_CHILDREN_IN_LEVEL = { 0, 1, 2, 3  };
//    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 2, 3  };
//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 5, 15    };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 5, 15    };
//    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 10,10,20 };

    // quick access to enabling/disabling tests

    private static final String TEST_19x_CHILD_OID = "o2000...-....-....-....-............";
    private static final String TEST_19x_PARENT_OID = "o000....-....-....-....-............";

    private static boolean CHECK_CHILDREN_SETS = false;
    private static boolean CHECK_CLOSURE_MATRIX = false;
    private long closureSize;

    @Test(enabled = true) public void test100LoadOrgStructure() throws Exception { _test100LoadOrgStructure(); }
    @Test(enabled = false) public void test110ScanOrgStructure() throws Exception { _test110ScanOrgStructure() ; }
    @Test(enabled = true) public void test150CheckClosure() throws Exception { _test150CheckClosure(); }
    @Test(enabled = false) public void test180RemoveAddCycle() throws Exception { _test180RemoveAddCycle(); }
    @Test(enabled = false) public void test190AddLink() throws Exception { _test190AddLink(); }
    @Test(enabled = false) public void test195RemoveLink() throws Exception { _test195RemoveLink(); }
    @Test(enabled = true) public void test200AddRemoveLinks() throws Exception { _test200AddRemoveLinks(); }
    @Test(enabled = true) public void test300AddRemoveOrgs() throws Exception { _test300AddRemoveOrgs(); }
    @Test(enabled = true) public void test310AddRemoveUsers() throws Exception { _test310AddRemoveUsers(); }
    @Test(enabled = false) public void test400UnloadOrgStructure() throws Exception { _test400UnloadOrgStructure(); }
    @Test(enabled = false) public void test410RandomUnloadOrgStructure() throws Exception { _test410RandomUnloadOrgStructure(); }

    @Override
    public boolean isCheckChildrenSets() {
        return CHECK_CHILDREN_SETS;
    }

    @Override
    public boolean isCheckClosureMatrix() {
        return CHECK_CLOSURE_MATRIX;
    }

    private void _test100LoadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test100LoadOrgStructure ]===");

        LOGGER.info("Start.");

        long start = System.currentTimeMillis();
        loadOrgStructure(0, null, ORG_CHILDREN_IN_LEVEL, USER_CHILDREN_IN_LEVEL, PARENTS_IN_LEVEL, "", opResult);
        System.out.println("Loaded " + allOrgCreated.size() + " orgs and " + (count - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        openSessionIfNeeded();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
        closureSize = Long.parseLong(q.list().get(0).toString());
    }

    private void _test110ScanOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test110ScanOrgStructure ]===");

        openSessionIfNeeded();

        long start = System.currentTimeMillis();
        scanOrgStructure(opResult);
        System.out.println("Found " + allOrgCreated.size() + " orgs and " + (count - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
        closureSize = Long.parseLong(q.list().get(0).toString());
    }

    private void _test150CheckClosure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test110CheckClosure ]===");
        boolean v = CHECK_CHILDREN_SETS;
        CHECK_CHILDREN_SETS = true;
        try {
            checkClosure(orgGraph.vertexSet());
        } finally {
            CHECK_CHILDREN_SETS = v;
        }
    }

    private void _test180RemoveAddCycle() throws Exception {
        for (int i = 0; i < 30; i++) {
            test195RemoveLink();
            test190AddLink();
        }
    }

    private void _test190AddLink() throws Exception {
        OperationResult opResult = new OperationResult("===[ test190AddLink ]===");

        //checkClosure(orgGraph.vertexSet());

        ObjectType child = repositoryService.getObject(ObjectType.class, TEST_19x_CHILD_OID, null, opResult).asObjectable();
        ObjectReferenceType parentOrgRef = new ObjectReferenceType();
        parentOrgRef.setOid(TEST_19x_PARENT_OID);
        parentOrgRef.setType(OrgType.COMPLEX_TYPE);
        System.out.println("Adding link " + TEST_19x_CHILD_OID + " -> " + TEST_19x_PARENT_OID);
        long start = System.currentTimeMillis();
        if (child instanceof OrgType) {
            addOrgParent((OrgType) child, parentOrgRef, opResult);
        } else {
            addUserParent((UserType) child, parentOrgRef, opResult);
        }
        long timeAddition = System.currentTimeMillis() - start;
        System.out.println(" ... done in " + timeAddition + " ms" + getNetDurationMessage());

        //checkClosure(orgGraph.vertexSet());
    }


    public void _test195RemoveLink() throws Exception {
        OperationResult opResult = new OperationResult("===[ test195RemoveLink ]===");

        //checkClosure(orgGraph.vertexSet());

        System.out.println("Removing link " + TEST_19x_CHILD_OID + " -> " + TEST_19x_PARENT_OID);
        ObjectType child = repositoryService.getObject(ObjectType.class, TEST_19x_CHILD_OID, null, opResult).asObjectable();
        ObjectReferenceType parentOrgRef = null;
        for (ObjectReferenceType ort : child.getParentOrgRef()) {
            if (TEST_19x_PARENT_OID.equals(ort.getOid())) {
                parentOrgRef = ort;
            }
        }
        assertNotNull(TEST_19x_PARENT_OID + " is not a parent of " + TEST_19x_CHILD_OID, parentOrgRef);
        long start = System.currentTimeMillis();
        removeObjectParent(child, parentOrgRef, opResult);
        long timeAddition = System.currentTimeMillis() - start;
        System.out.println(" ... done in " + timeAddition + " ms" + getNetDurationMessage());

        //checkClosure(orgGraph.vertexSet());
    }

    protected String getNetDurationMessage() {
        return " (closure update: " + getNetDuration() + " ms)";
    }

    private void _test200AddRemoveLinks() throws Exception {
        OperationResult opResult = new OperationResult("===[ addRemoveLinks ]===");

        int totalRounds = 0;
        OrgClosureStatistics stat = new OrgClosureStatistics();

        // parentRef link removal + addition
        long totalTimeLinkRemovals = 0, totalTimeLinkAdditions = 0;
        for (int level = 0; level < LINK_ROUNDS_FOR_LEVELS.length; level++) {
            for (int round = 0; round < LINK_ROUNDS_FOR_LEVELS[level]; round++) {

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

                System.out.println("Removing parent from org #" + totalRounds + "(" + level + "/" + round + "): " + org.getOid() + ", parent: " + parentOrgRef.getOid());
                long start = System.currentTimeMillis();
                removeObjectParent(org, parentOrgRef, opResult);
                long timeRemoval = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeRemoval + " ms " + getNetDurationMessage());
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks", level, false, getNetDuration());
                totalTimeLinkRemovals += getNetDuration();

                checkClosure(orgGraph.vertexSet());

                // addition
                System.out.println("Re-adding parent for org #" + totalRounds);
                start = System.currentTimeMillis();
                addOrgParent(org, parentOrgRef, opResult);
                long timeAddition = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeAddition + " ms " + getNetDurationMessage());
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks", level, true, getNetDuration());

                checkClosure(orgGraph.vertexSet());

                totalTimeLinkAdditions += getNetDuration();
                totalRounds++;
            }
        }

        if (totalRounds > 0) {
            System.out.println("Avg time for an arbitrary link removal: " + ((double) totalTimeLinkRemovals / totalRounds) + " ms");
            System.out.println("Avg time for an arbitrary link re-addition: " + ((double) totalTimeLinkAdditions / totalRounds) + " ms");
            LOGGER.info("===================================================");
            LOGGER.info("Statistics for org link removal/addition:");
            stat.dump(LOGGER, repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveLinks");
        }
    }

    private void _test300AddRemoveOrgs() throws Exception {
        OperationResult opResult = new OperationResult("===[ test300AddRemoveOrgs ]===");

        int totalRounds = 0;
        OrgClosureStatistics stat = new OrgClosureStatistics();

        // OrgType node removal + addition
        long totalTimeNodeRemovals = 0, totalTimeNodeAdditions = 0;
        for (int level = 0; level < NODE_ROUNDS_FOR_LEVELS.length; level++) {
            for (int round = 0; round < NODE_ROUNDS_FOR_LEVELS[level]; round++) {

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
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs", level, false, getNetDuration());
                totalTimeNodeRemovals += getNetDuration();

                checkClosure(orgGraph.vertexSet());

                // addition
                System.out.println("Re-adding org #" + totalRounds);
                start = System.currentTimeMillis();
                reAddOrg(org, opResult);
                long timeAddition = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeAddition + "ms" + getNetDurationMessage());
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs", level, true, getNetDuration());

                checkClosure(orgGraph.vertexSet());

                totalTimeNodeAdditions += getNetDuration();
                totalRounds++;
            }
        }

        if (totalRounds > 0) {
            System.out.println("Avg time for an arbitrary node removal: " + ((double) totalTimeNodeRemovals / totalRounds) + " ms");
            System.out.println("Avg time for an arbitrary node re-addition: " + ((double) totalTimeNodeAdditions / totalRounds) + " ms");
            LOGGER.info("===================================================");
            LOGGER.info("Statistics for org node removal/addition:");
            stat.dump(LOGGER, repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveOrgs");
        }
    }

    public void _test310AddRemoveUsers() throws Exception {
        OperationResult opResult = new OperationResult("===[ test310AddRemoveUsers ]===");

        int totalRounds = 0;
        OrgClosureStatistics stat = new OrgClosureStatistics();

        long totalTimeNodeRemovals = 0, totalTimeNodeAdditions = 0;
        for (int level = 0; level < USER_ROUNDS_FOR_LEVELS.length; level++) {
            for (int round = 0; round < USER_ROUNDS_FOR_LEVELS[level]; round++) {

                // removal
                List<String> levelOids = usersByLevels.get(level);
                if (levelOids.isEmpty()) {
                    continue;
                }
                int index = (int) Math.floor(Math.random() * levelOids.size());
                String oid = levelOids.get(index);
                UserType user = repositoryService.getObject(UserType.class, oid, null, opResult).asObjectable();

                System.out.println("Removing user #" + totalRounds + " (" + level + "/" + round + "): " + user.getOid() + " (parents: " + getParentsOids(user.asPrismObject()) + ")");
                long start = System.currentTimeMillis();
                removeUser(user.getOid(), opResult);
                long timeRemoval = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeRemoval + " ms" + getNetDurationMessage());
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveUsers", level, false, getNetDuration());
                totalTimeNodeRemovals += getNetDuration();

                checkClosure(orgGraph.vertexSet());

                // addition
                System.out.println("Re-adding user #" + totalRounds);
                start = System.currentTimeMillis();
                reAddUser(user, opResult);
                long timeAddition = System.currentTimeMillis() - start;
                System.out.println(" ... done in " + timeAddition + "ms" + getNetDurationMessage());
                stat.recordExtended(repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveUsers", level, true, getNetDuration());

                checkClosure(orgGraph.vertexSet());

                totalTimeNodeAdditions += getNetDuration();
                totalRounds++;
            }
        }

        if (totalRounds > 0) {
            System.out.println("Avg time for an arbitrary user removal: " + ((double) totalTimeNodeRemovals / totalRounds) + " ms");
            System.out.println("Avg time for an arbitrary user re-addition: " + ((double) totalTimeNodeAdditions / totalRounds) + " ms");
            LOGGER.info("===================================================");
            LOGGER.info("Statistics for user node removal/addition:");
            stat.dump(LOGGER, repositoryService.getConfiguration().getHibernateDialect(), allOrgCreated.size(), allUsersCreated.size(), closureSize, "AddRemoveUsers");
        }
    }

    private void _test400UnloadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ unloadOrgStruct ]===");
        long start = System.currentTimeMillis();
        removeOrgStructure(opResult);
        System.out.println("Removed in " + (System.currentTimeMillis() - start) + " ms");

        openSessionIfNeeded();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");

        LOGGER.info("Finish.");
    }

    private void _test410RandomUnloadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test410RandomUnloadOrgStructure ]===");
        long start = System.currentTimeMillis();
        randomRemoveOrgStructure(opResult);
        System.out.println("Removed in " + (System.currentTimeMillis() - start) + " ms");

        openSessionIfNeeded();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");

        LOGGER.info("Finish.");
    }

}
