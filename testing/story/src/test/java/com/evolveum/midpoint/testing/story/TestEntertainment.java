/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestEntertainment extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "entertainment");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final File ROLE_META_CREATE_ORG_GROUPS_FILE = new File(TEST_DIR,
            "role-meta-org-groups.xml");
    private static final String ROLE_META_CREATE_ORG_GROUPS_OID = "10000000-ent0-0000-0000-000000006601";

    private static final File ORG_GAMES_TOP_FILE = new File(TEST_DIR, "org-games-top.xml");
    private static final String ORG_GAMES_TOP_OID = "00000000-8888-6666-ent0-100000000001";

    private static final File ORG_POKER_FILE = new File(TEST_DIR, "org-poker.xml");
    private static final String ORG_POKER_OID = "00000000-8888-6666-ent0-100000000002";

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {

        super.initSystem(initTask, initResult);

        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
                RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        importObjectFromFile(ROLE_META_CREATE_ORG_GROUPS_FILE);

    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        assertSuccess(testResultOpenDj);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);

        PrismObject<RoleType> metaRole = modelService.getObject(RoleType.class,
                ROLE_META_CREATE_ORG_GROUPS_OID, null, task, result);
        assertNotNull("No metarole, probably probelm with initialization", metaRole);
        result.computeStatus();
        assertSuccess("Role not fetch successfully", result);

    }

    @Test
    public void test001AddParentOrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        addObject(ORG_GAMES_TOP_FILE);

        // THEN
        PrismObject<OrgType> orgGames = modelService.getObject(OrgType.class, ORG_GAMES_TOP_OID, null, task,
                result);
        assertNotNull("No top org for games found", orgGames);
        result.computeStatus();
        assertSuccess("Error while getting top games org", result);

        OrgType orgGamesType = orgGames.asObjectable();
        assertLinks(orgGames, 2);

        List<ObjectReferenceType> linkRefs = orgGamesType.getLinkRef();

        // SHADOW 1
        ShadowType shadowType1 = getAndAssertShadowSuccess(linkRefs.get(0), task, result);

        // SHADOW 2
        ShadowType shadowType2 = getAndAssertShadowSuccess(linkRefs.get(1), task, result);

        assertIntents(shadowType1, shadowType2);

    }

    @Test
    public void test002AddChildOrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        addObject(ORG_POKER_FILE);

        // THEN
        Collection<String> uniqueMembers = openDJController.getGroupUniqueMembers("cn=Games,ou=groups,dc=example,dc=com");
        assertNotNull("null unique members", uniqueMembers);
        assertEquals("Expected exactly one member", 1, uniqueMembers.size());

        openDJController.assertUniqueMember("cn=Games,ou=groups,dc=example,dc=com", "cn=Poker,ou=groups,dc=example,dc=com");

        PrismObject<OrgType> orgGames = modelService.getObject(OrgType.class, ORG_POKER_OID, null, task,
                result);
        assertNotNull("No top org for games found", orgGames);
        result.computeStatus();
        assertSuccess("Error while getting top games org", result);

        OrgType orgGamesType = orgGames.asObjectable();
        assertLinks(orgGames, 2);

        List<ObjectReferenceType> linkRefs = orgGamesType.getLinkRef();

        // SHADOW 1
        ShadowType shadowType1 = getAndAssertShadowSuccess(linkRefs.get(0), task, result);

        // SHADOW 2
        ShadowType shadowType2 = getAndAssertShadowSuccess(linkRefs.get(1), task, result);

        assertIntents(shadowType1, shadowType2);

    }

    private ShadowType getAndAssertShadowSuccess(ObjectReferenceType ort, Task task, OperationResult result) throws Exception {
        assertNotNull("Unexpected (null) reference to shadow", ort);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, ort.getOid(),
                null, task, result);
        assertNotNull("No shadow for org found", shadow);
        ShadowType shadowType = shadow.asObjectable();
        result.computeStatus();
        assertSuccess("Overal error while getting shadow", result);
        TestUtil.assertSuccess("Problem with getting concrete shadow: fetchResult", shadowType.getFetchResult());
        // TODO FIX THIS!!!
        //assertNull("Unexpected error in shadow: result", shadowType.getResult());
        return shadowType;
    }

    private void assertIntents(ShadowType shadowType1, ShadowType shadowType2) {
        String intentShadow1 = shadowType1.getIntent();
        String intentShadow2 = shadowType2.getIntent();
        if ((intentShadow1.equals("group-org-local") && intentShadow2.equals("group-org-global"))
                || (intentShadow1.equals("group-org-global") && intentShadow2.equals("group-org-local"))) {
            // EVERYTHING OK
        } else {
            fail("Shadow intents are not correct, expected one group-org-local and one group-org-global, but got: "
                    + intentShadow1 + ", " + intentShadow2);
        }
    }
}
