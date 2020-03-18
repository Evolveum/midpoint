/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.closure;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

@ContextConfiguration(locations = { "../../../../../../ctx-test.xml" })
public class OrgClosurePerformanceTest1 extends AbstractOrgClosureTest {

    // relatively smaller graph
    private static final int[] ORG_CHILDREN_IN_LEVEL = { 1, 2, 3, 4, 5, 0 };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL = { 0, 1, 2, 3, 3, 3 };
    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 5, 15, 15, 15, 0 };
    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 5, 15, 15, 15, 0 };

    private OrgClosureTestConfiguration configuration;

    public OrgClosurePerformanceTest1() {
        configuration = new OrgClosureTestConfiguration();
        configuration.setCheckChildrenSets(false);
        configuration.setCheckClosureMatrix(false);
        configuration.setDeletionsToClosureTest(15);
        configuration.setOrgChildrenInLevel(ORG_CHILDREN_IN_LEVEL);
        configuration.setUserChildrenInLevel(USER_CHILDREN_IN_LEVEL);
        configuration.setParentsInLevel(PARENTS_IN_LEVEL);
        configuration.setLinkRoundsForLevel(LINK_ROUNDS_FOR_LEVELS);
        configuration.setNodeRoundsForLevel(NODE_ROUNDS_FOR_LEVELS);
    }

    @Test
    public void test100LoadOrgStructure() throws Exception {
        _test100LoadOrgStructure();
    }

    @Test
    public void test150CheckClosure() throws Exception {
        _test150CheckClosure();
    }

    @Test
    public void test200AddRemoveLinks() throws Exception {
        _test200AddRemoveLinks();
    }

    @Test
    public void test300AddRemoveOrgs() throws Exception {
        _test300AddRemoveOrgs();
    }

    @Test
    public void test410RandomUnloadOrgStructure() throws Exception {
        _test410RandomUnloadOrgStructure();
    }

    @Override
    public OrgClosureTestConfiguration getConfiguration() {
        return configuration;
    }
}
