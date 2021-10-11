/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.closure;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosurePerformanceTest2 extends AbstractOrgClosureTest {

    // relatively bigger graph
    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 2, 5, 3, 3, 5, 4 };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 2, 2, 2, 2 };
    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 10, 15,15,15,15 };
    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 5, 10, 15,15,15,15 };

    private OrgClosureTestConfiguration configuration;

    public OrgClosurePerformanceTest2() {
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

    @Test public void test100LoadOrgStructure() throws Exception { _test100LoadOrgStructure(); }
    @Test public void test150CheckClosure() throws Exception { _test150CheckClosure(); }
    @Test public void test200AddRemoveLinks() throws Exception { _test200AddRemoveLinks(); }
    @Test public void test300AddRemoveOrgs() throws Exception { _test300AddRemoveOrgs(); }
    @Test public void test410RandomUnloadOrgStructure() throws Exception { _test410RandomUnloadOrgStructure(); }

    @Override
    public OrgClosureTestConfiguration getConfiguration() {
        return configuration;
    }
}
