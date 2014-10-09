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

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureCorrectnessTest extends AbstractOrgClosureTest {

    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 4, 3, 3  };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL       = { 0, 2, 2  };
    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 5, 10 };
    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 5, 10 };

    // trivial case for debugging
//    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 2, 0  };
//    private static final int[] USER_CHILDREN_IN_LEVEL = { 0, 0, 0  };
//    private static final int[] PARENTS_IN_LEVEL       = { 0, 1, 1  };
//    private static final int[] LINK_ROUNDS_FOR_LEVELS = { 0, 1, 0    };
//    private static final int[] NODE_ROUNDS_FOR_LEVELS = { 1, 5, 10    };
//    private static final int[] USER_ROUNDS_FOR_LEVELS = { 0, 5 ,5, 10 };

    private OrgClosureTestConfiguration configuration;

    public OrgClosureCorrectnessTest() {
        configuration = new OrgClosureTestConfiguration();
        configuration.setCheckChildrenSets(true);
        configuration.setCheckClosureMatrix(true);
        configuration.setDeletionsToClosureTest(1);
        configuration.setOrgChildrenInLevel(ORG_CHILDREN_IN_LEVEL);
        configuration.setUserChildrenInLevel(USER_CHILDREN_IN_LEVEL);
        configuration.setParentsInLevel(PARENTS_IN_LEVEL);
        configuration.setLinkRoundsForLevel(LINK_ROUNDS_FOR_LEVELS);
        configuration.setNodeRoundsForLevel(NODE_ROUNDS_FOR_LEVELS);
    }

    @Test(enabled = true) public void test100LoadOrgStructure() throws Exception { _test100LoadOrgStructure(); }
    @Test(enabled = true) public void test150CheckClosure() throws Exception { _test150CheckClosure(); }
    @Test(enabled = true) public void test200AddRemoveLinks() throws Exception { _test200AddRemoveLinks(); }
    @Test(enabled = true) public void test300AddRemoveOrgs() throws Exception { _test300AddRemoveOrgs(); }
    @Test(enabled = true) public void test390CyclePrevention() throws Exception { _test390CyclePrevention(); }
    @Test(enabled = true) public void test410RandomUnloadOrgStructure() throws Exception { _test410RandomUnloadOrgStructure(); }

    @Override
    public OrgClosureTestConfiguration getConfiguration() {
        return configuration;
    }
}
