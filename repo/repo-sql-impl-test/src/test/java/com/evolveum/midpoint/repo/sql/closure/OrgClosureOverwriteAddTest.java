/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.closure;

import static org.testng.AssertJUnit.assertFalse;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * MID-4407
 *
 * @author mederly
 */
@ContextConfiguration(locations = { "../../../../../../ctx-test.xml" })
public class OrgClosureOverwriteAddTest extends AbstractOrgClosureTest {

    private static final int[] ORG_CHILDREN_IN_LEVEL = { 1, 1 };
    private static final int[] USER_CHILDREN_IN_LEVEL = null;
    private static final int[] PARENTS_IN_LEVEL = { 0, 1 };

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

        logger.info("+++ adding object with 'overwrite' option +++");
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
