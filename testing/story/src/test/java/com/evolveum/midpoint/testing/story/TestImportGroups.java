/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestImportGroups extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "import-group");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-import-group.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final File TASK_IMPORT_GROUPS = new File(TEST_DIR, "task-opendj-import-groups.xml");
    private static final String TASK_IMPORT_GROUPS_OID = "7d79f012-d861-11e8-b788-07bda6c5bb24";

    private static final File LDIF_GROUPS = new File(TEST_DIR, "groups.ldif");

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    private static final int GROUPS = 6;
    private static final int DEFAULT_GROUPS = 3; // 3 ou by default: ou=Administratos, ou=People, ou=groups

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE,
                RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        openDJController.addEntriesFromLdifFile(LDIF_GROUPS);

    }

    @Test
    public void test001importGroups() throws Exception {
        addObject(TASK_IMPORT_GROUPS);

        waitForTaskFinish(TASK_IMPORT_GROUPS_OID);

        assertObjects(OrgType.class, DEFAULT_GROUPS + GROUPS);
    }
}
