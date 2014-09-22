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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.hibernate.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureSimpleTreeTest extends AbstractOrgClosureTest {

    private static final Trace LOGGER = TraceManager.getTrace(OrgClosureSimpleTreeTest.class);

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");
    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_SIMPLE_TEST = TEST_DIR + "/org-simple-test.xml";

    private static final boolean CHECK_CLOSURE = true;

    //50531 OU, 810155 U
//    private static final int[] TREE_LEVELS = {1, 5, 5, 20, 20, 4};
//    private static final int[] TREE_LEVELS_USERS = {5, 10, 4, 20, 20, 15};

    //1191 OU, 10943 U  =>  428585 queries ~ 6min, h2
//    private static final int[] TREE_LEVELS = {1, 5, 3, 3, 5, 4};
//    private static final int[] TREE_LEVELS_USERS = {3, 4, 5, 6, 7, 10};

    private static final int[] TREE_LEVELS = {1, 2, 3, 4, 5};
    private static final int[] TREE_LEVELS_USERS = {1, 2, 3, 4, 5};

    //9 OU, 23 U        =>  773 queries ~ 50s, h2
//    private static final int[] TREE_LEVELS = {1, 2, 3};
//    private static final int[] TREE_LEVELS_USERS = {1, 2, 3};

    @Test(enabled = true)
    public void test100LoadOrgStructure() throws Exception {
        OperationResult opResult = new OperationResult("===[ loadOrgStruct ]===");

        LOGGER.info("Start.");

        long start = System.currentTimeMillis();
        loadOrgStructure(0, null, TREE_LEVELS, TREE_LEVELS_USERS, null, "", opResult);
        System.out.println("Loaded " + allOrgCreated.size() + " orgs and " + (count - allOrgCreated.size()) + " users in " + (System.currentTimeMillis() - start) + " ms");
        openSessionIfNeeded();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");
    }

    @Test(enabled = true)
    public void test110CheckClosure() throws Exception {
        OperationResult opResult = new OperationResult("===[ test110CheckClosure ]===");
        if (CHECK_CLOSURE) checkClosure(orgGraph.vertexSet());
    }

    @Test(enabled = true)
    public void test200AddRemoveLinks() throws Exception {
        OperationResult opResult = new OperationResult("===[ addRemoveLinks ]===");

        final int LINK_ROUNDS = 20;

        // parentRef link removal + addition
        long totalTimeLinkRemovals = 0, totalTimeLinkAdditions = 0;
        for (int round = 1; round <= LINK_ROUNDS; round++) {

            // removal
            int index = 1 + (int) Math.floor(Math.random() * (allOrgCreated.size() - 1));          // assuming node 0 is the root
            OrgType org = allOrgCreated.get(index);

            // check if it's a root (by chance)
            if (org.getParentOrgRef().isEmpty()) {
                round--;
                continue;
            }
            System.out.println("Removing parent from org #" + round + ": " + org.getOid());
            long start = System.currentTimeMillis();
            ObjectReferenceType parentOrgRef = org.getParentOrgRef().get(0);
            removeOrgParent(org, parentOrgRef, opResult);
            long timeRemoval = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeRemoval + " ms");

            if (CHECK_CLOSURE) checkClosure(orgGraph.vertexSet());

            // addition
            System.out.println("Re-adding parent for org #" + round);
            start = System.currentTimeMillis();
            addOrgParent(org, parentOrgRef, opResult);
            long timeAddition = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeAddition + " ms");

            if (CHECK_CLOSURE) checkClosure(orgGraph.vertexSet());

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
            int index = (int) Math.floor(Math.random() * allOrgCreated.size());
            OrgType org = allOrgCreated.get(index);
            System.out.println("Removing org #" + round + ": " + org.getOid());
            long start = System.currentTimeMillis();
            removeOrg(org.getOid(), opResult);
            long timeRemoval = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeRemoval + " ms");

            if (CHECK_CLOSURE) checkClosure(orgGraph.vertexSet());

            // addition
            System.out.println("Re-adding org #" + round);
            start = System.currentTimeMillis();
            reAddOrg(org, opResult);
            long timeAddition = System.currentTimeMillis() - start;
            System.out.println(" ... done in " + timeAddition + "ms");

            if (CHECK_CLOSURE) checkClosure(orgGraph.vertexSet());

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

        openSessionIfNeeded();
        Query q = session.createSQLQuery("select count(*) from m_org_closure");
        System.out.println("OrgClosure table has " + q.list().get(0) + " rows");

        LOGGER.info("Finish.");
    }

}
