/*
 * Copyright (c) 2019-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.grouper;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Superclass for all Grouper-like test scenarios.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractGrouperTest extends AbstractStoryTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "grouper");

    protected static final File LDIF_INITIAL_OBJECTS_FILE = new File(TEST_DIR, "ldif-initial-objects.ldif");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    protected static final TestResource LIB_GROUPER = new TestResource(TEST_DIR, "function-library-grouper.xml", "2eef4181-25fa-420f-909d-846a36ca90f3");

    protected static final TestResource RESOURCE_LDAP = new TestResource(TEST_DIR, "resource-ldap.xml", "0a37121f-d515-4a23-9b6d-554c5ef61272");
    protected static final TestResource RESOURCE_GROUPER = new TestResource(TEST_DIR, "resource-grouper.xml", "1eff65de-5bb6-483d-9edf-8cc2c2ee0233");

    protected static final TestResource METAROLE_GROUPER_PROVIDED_GROUP = new TestResource(TEST_DIR, "metarole-grouper-provided-group.xml", "bcaec940-50c8-44bb-aa37-b2b5bb2d5b90");
    protected static final TestResource METAROLE_LDAP_GROUP = new TestResource(TEST_DIR, "metarole-ldap-group.xml", "8da46694-bd71-4e1e-bfd7-73865ae2ea9a");

    protected static final TestResource ARCHETYPE_AFFILIATION = new TestResource(TEST_DIR, "archetype-affiliation.xml", "56f53812-047d-4b69-83e8-519a73d161e1");
    protected static final TestResource ORG_AFFILIATIONS = new TestResource(TEST_DIR, "org-affiliations.xml", "1d7c0e3a-4456-409c-9f50-95407b2eb785");

    protected static final TestResource ROLE_LDAP_BASIC = new TestResource(TEST_DIR, "role-ldap-basic.xml", "c89f31dd-8d4f-4e0a-82cb-58ff9d8c1b2f");

    protected static final TestResource TEMPLATE_USER = new TestResource(TEST_DIR, "template-user.xml", "8098b124-c20c-4965-8adf-e528abedf7a4");

    protected static final TestResource USER_BANDERSON = new TestResource(TEST_DIR, "user-banderson.xml", "4f439db5-181e-4297-9f7d-b3115524dbe8");
    protected static final TestResource USER_JLEWIS685 = new TestResource(TEST_DIR, "user-jlewis685.xml", "8b7bd936-b863-45d0-aabe-734fa3e22081");

    protected static final TestResource TASK_GROUP_SCAVENGER = new TestResource(TEST_DIR, "task-group-scavenger.xml", "1d7bef40-953e-443e-8e9a-ec6e313668c4");

    protected static final String NS_EXT = "http://grouper-demo.tier.internet2.edu";
    protected static final ItemName EXT_GROUPER_NAME = new ItemName(NS_EXT, "grouperName");
    protected static final ItemName EXT_LDAP_DN = new ItemName(NS_EXT, "ldapDn");

    protected static final String BANDERSON_USERNAME = "banderson";
    protected static final String JLEWIS685_USERNAME = "jlewis685";
    protected static final String NOBODY_USERNAME = "nobody";

    protected static final String ALUMNI_ID = "321931093132132alumni";
    protected static final String ALUMNI_NAME = "ref:affiliation:alumni";

    protected static final String STAFF_ID = "9789654960496542staff";
    protected static final String STAFF_NAME = "ref:affiliation:staff";
    protected static final String STAFF2_NAME = "ref:affiliation:staff2";
    protected static final String MEMBER_NAME = "ref:affiliation:member";

    protected static final ItemName ATTR_MEMBER = new ItemName(MidPointConstants.NS_RI, "members");

    protected static final String DN_BANDERSON = "uid=banderson,ou=people,dc=example,dc=com";
    protected static final String DN_JLEWIS685 = "uid=jlewis685,ou=people,dc=example,dc=com";
    protected static final String DN_ALUMNI = "cn=alumni,ou=Affiliations,ou=Groups,dc=example,dc=com";
    protected static final String DN_STAFF = "cn=staff,ou=Affiliations,ou=Groups,dc=example,dc=com";
    protected static final String DN_STAFF2 = "cn=staff2,ou=Affiliations,ou=Groups,dc=example,dc=com";

    protected static final QName GROUPER_GROUP_OBJECT_CLASS_NAME = new QName(MidPointConstants.NS_RI, "GroupObjectClass");

    protected static final String GROUPER_DUMMY_RESOURCE_ID = "grouper";
    public static final String USERNAME_FORMAT = "user-%08d";
    protected PrismObject<ResourceType> resourceGrouper;
    protected DummyResourceContoller grouperResourceCtl;
    protected DummyResource grouperDummyResource;

    protected PrismObject<ResourceType> resourceLdap;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        //TracerImpl.checkHashCodeEqualsRelation = true;

        // These are experimental features, so they need to be explicitly enabled. This will be eliminated later,
        // when we make them enabled by default.
        sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);
        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesDeletion(true);

        openDJController.addEntriesFromLdifFile(LDIF_INITIAL_OBJECTS_FILE);

        repoAddObject(LIB_GROUPER, initResult);

        grouperResourceCtl = DummyResourceContoller.create(GROUPER_DUMMY_RESOURCE_ID);
        grouperResourceCtl.populateWithDefaultSchema();
        grouperDummyResource = grouperResourceCtl.getDummyResource();
        resourceGrouper = importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER.file, RESOURCE_GROUPER.oid, initTask, initResult);
        grouperResourceCtl.setResource(resourceGrouper);

        resourceLdap = importAndGetObjectFromFile(ResourceType.class, RESOURCE_LDAP.file, RESOURCE_LDAP.oid, initTask, initResult);
        openDJController.setResource(resourceLdap);

        addObject(METAROLE_GROUPER_PROVIDED_GROUP, initTask, initResult);
        addObject(METAROLE_LDAP_GROUP, initTask, initResult);

        addObject(ARCHETYPE_AFFILIATION, initTask, initResult);
        addObject(ORG_AFFILIATIONS, initTask, initResult);

        addObject(ROLE_LDAP_BASIC, initTask, initResult);
        addObject(TEMPLATE_USER, initTask, initResult);

        addObject(TASK_GROUP_SCAVENGER, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }

    void createGroup(String groupId, String groupName, int users) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("Creating the group");
        DummyGroup group = new DummyGroup();
        group.setId(groupId);
        group.setName(groupName);
        for (int i = 0; i < users; i++) {
            group.addMember(String.format(USERNAME_FORMAT, i));
        }
        grouperDummyResource.addGroup(group);
        System.out.println("Group created in " + (System.currentTimeMillis() - start) + " ms");
    }

    void addGroupMember(String groupName, int number)
            throws InterruptedException, FileNotFoundException, ConnectException, SchemaViolationException, ConflictException {
        DummyGroup group = grouperDummyResource.getGroupByName(groupName);
        assertNotNull("No group " + groupName, group);
        group.addMember(String.format(USERNAME_FORMAT, number));
    }

    void deleteGroupMember(String groupName, int number)
            throws InterruptedException, FileNotFoundException, ConnectException, SchemaViolationException, ConflictException {
        DummyGroup group = grouperDummyResource.getGroupByName(groupName);
        assertNotNull("No group " + groupName, group);
        group.removeMember(String.format(USERNAME_FORMAT, number));
    }
}
