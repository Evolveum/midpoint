/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.closure;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;

/**
 * MID-4407
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgClosureOverwriteAddTest extends AbstractOrgClosureTest {

	private static final Trace LOGGER = TraceManager.getTrace(OrgClosureOverwriteAddTest.class);

    private static final int[] ORG_CHILDREN_IN_LEVEL  = { 1, 1  };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL       = { 0, 1  };

    private OrgClosureTestConfiguration configuration;

    public OrgClosureOverwriteAddTest() {
        configuration = new OrgClosureTestConfiguration();
        configuration.setCheckChildrenSets(true);
        configuration.setCheckClosureMatrix(true);
        configuration.setDeletionsToClosureTest(1);
        configuration.setOrgChildrenInLevel(ORG_CHILDREN_IN_LEVEL);
        configuration.setUserChildrenInLevel(USER_CHILDREN_IN_LEVEL);
        configuration.setParentsInLevel(PARENTS_IN_LEVEL);
    }

    @Test
    public void test100AddWithOverwrite() throws Exception {
        OperationResult opResult = new OperationResult("===[ test100AddWithOverwrite ]===");

        _test100LoadOrgStructure();
        _test150CheckClosure();

        String parentOid = orgsByLevels.get(0).get(0);
        String childOid = orgsByLevels.get(1).get(0);
        PrismObject<OrgType> childOrg = repositoryService.getObject(OrgType.class, childOid, null, opResult);
        childOrg.asObjectable().getParentOrgRef().clear();

        LOGGER.info("+++ adding object with 'overwrite' option +++");
        repositoryService.addObject(childOrg, RepoAddOptions.createOverwrite(), opResult);

	    orgGraph.removeEdge(childOid, parentOid);
	    checkOrgGraph();
        boolean problem = checkClosureMatrix();
        assertFalse("Closure problem was detected", problem);
    }

    @Override
    public OrgClosureTestConfiguration getConfiguration() {
        return configuration;
    }
}
