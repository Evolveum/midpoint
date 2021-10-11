/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import org.jetbrains.annotations.Nullable;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUnix extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "unix");

    protected static final String EXTENSION_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/story/unix/ext";
    protected static final ItemName EXTENSION_UID_NUMBER_NAME = new ItemName(EXTENSION_NAMESPACE, "uidNumber");
    protected static final ItemName EXTENSION_GID_NUMBER_NAME = new ItemName(EXTENSION_NAMESPACE, "gidNumber");

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
    protected static final ItemName OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson");
    protected static final ItemName OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "posixAccount");
    protected static final ItemName OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "labeledURIObject");
    protected static final ItemName OPENDJ_GROUP_STRUCTURAL_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "groupOfUniqueNames");
    protected static final ItemName OPENDJ_GROUP_UNIX_STRUCTURAL_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "groupOfNames");
    protected static final ItemName OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "posixGroup");
    protected static final ItemName OPENDJ_ASSOCIATION_LDAP_GROUP_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "ldapGroup");
    protected static final ItemName OPENDJ_ASSOCIATION_UNIX_GROUP_NAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, "unixGroup");
    protected static final String OPENDJ_UIDNUMBER_ATTRIBUTE_NAME = "uidNumber";
    protected static final String OPENDJ_GIDNUMBER_ATTRIBUTE_NAME = "gidNumber";
    protected static final String OPENDJ_UID_ATTRIBUTE_NAME = "uid";
    protected static final String OPENDJ_LABELED_URI_ATTRIBUTE_NAME = "labeledURI";
    protected static final String OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_NAME = "modifyTimestamp";
    protected static final ItemName OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_QNAME = new ItemName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_NAME);

    public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
    public static final String ROLE_BASIC_OID = "10000000-0000-0000-0000-000000000601";

    public static final File ROLE_UNIX_FILE = new File(TEST_DIR, "role-unix.xml");
    public static final String ROLE_UNIX_OID = "744a54f8-18e5-11e5-808f-001e8c717e5b";

    public static final File ROLE_META_UNIXGROUP_FILE = new File(TEST_DIR, "role-meta-unix-group.xml");
    public static final String ROLE_META_UNIXGROUP_OID = "31ea66ac-1a8e-11e5-8ab8-001e8c717e5b";

    public static final File ROLE_META_UNIXGROUP2_FILE = new File(TEST_DIR, "role-meta-unix-group2.xml");
    public static final String ROLE_META_UNIXGROUP2_OID = "4ab1e1aa-d0c4-11e5-b0c2-3c970e44b9e2";

    public static final File ROLE_META_LDAPGROUP_FILE = new File(TEST_DIR, "role-meta-ldap-group.xml");
    public static final String ROLE_META_LDAPGROUP_OID = "9c6d1dbe-1a87-11e5-b107-001e8c717e5b";

    protected static final String USER_HERMAN_USERNAME = "ht";
    protected static final String USER_HERMAN_FIST_NAME = "Herman";
    protected static final String USER_HERMAN_LAST_NAME = "Toothrot";

    protected static final String USER_MANCOMB_USERNAME = "mancomb";
    protected static final String USER_MANCOMB_FIST_NAME = "Mancomb";
    protected static final String USER_MANCOMB_LAST_NAME = "Seepgood";

    protected static final String USER_LARGO_USERNAME = "largo";
    protected static final String USER_LARGO_FIST_NAME = "Largo";
    protected static final String USER_LARGO_LAST_NAME = "LaGrande";
    protected static final int USER_LARGO_UID_NUMBER = 1002;

    protected static final String USER_CAPSIZE_USERNAME = "capsize";
    protected static final String USER_CAPSIZE_FIST_NAME = "Kate";
    protected static final String USER_CAPSIZE_LAST_NAME = "Capsize";
    protected static final int USER_CAPSIZE_UID_NUMBER = 1004;

    protected static final String USER_WALLY_USERNAME = "wally";
    protected static final String USER_WALLY_FIST_NAME = "Wally";
    protected static final String USER_WALLY_LAST_NAME = "Feed";
    protected static final int USER_WALLY_UID_NUMBER = 1004;

    protected static final String USER_RANGER_USERNAME = "ranger";
    protected static final String USER_RANGER_USERNAME_RENAMED = "usranger";
    protected static final String USER_RANGER_FIST_NAME = "Super";
    protected static final String USER_RANGER_LAST_NAME = "Ranger";
    protected static final int USER_RANGER_UID_NUMBER = 1003;

    protected static final File STRUCT_LDIF_FILE = new File(TEST_DIR, "struct.ldif");

    protected static final String ROLE_MONKEY_ISLAND_NAME = "Monkey Island";

    protected static final String ROLE_VILLAINS_NAME = "villains";
    protected static final Integer ROLE_VILLAINS_GID = 999;
    protected static final String ROLE_RANGERS_NAME = "rangers";
    protected static final Integer ROLE_RANGERS_GID = 998;
    protected static final String ROLE_SEALS_NAME = "seals";
    protected static final Integer ROLE_SEALS_GID = 997;
    protected static final String ROLE_WALRUSES_NAME = "walruses";

    public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
    public static final String OBJECT_TEMPLATE_USER_OID = "9cd03eda-66bd-11e5-866c-f3bc34108fdf";

    public static final File SEQUENCE_UIDNUMBER_FILE = new File(TEST_DIR, "sequence-uidnumber.xml");
    public static final String SEQUENCE_UIDNUMBER_OID = "7d4acb8c-65e3-11e5-9ef4-6382ba96fe6c";

    public static final File SEQUENCE_GIDNUMBER_FILE = new File(TEST_DIR, "sequence-gidnumber.xml");

    protected static final String USER_STAN_USERNAME = "stan";
    protected static final String USER_STAN_FIST_NAME = "Stan";
    protected static final String USER_STAN_LAST_NAME = "Salesman";

    @Autowired
    private ReconciliationTaskHandler reconciliationTaskHandler;

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    protected String accountMancombOid;
    protected String accountMancombDn;

    protected String accountLargoOid;
    protected String accountLargoDn;

    protected String accountRangerOid;
    protected String accountRangerDn;

    protected String accountWallyOid;
    protected String accountWallyDn;

    protected String roleMonkeyIslandOid;
    protected String groupMonkeyIslandDn;
    protected String groupMonkeyIslandOid;

    protected String roleVillainsOid;
    protected String groupVillainsDn;

    protected String roleRangersOid;
    protected String groupRangersDn;
    protected String groupRangersOid;

    protected String roleSealsOid;
    protected String groupSealsDn;
    protected String groupSealsOid;

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

        DebugReconciliationTaskResultListener reconciliationTaskResultListener = new DebugReconciliationTaskResultListener();
        reconciliationTaskHandler.setReconciliationTaskResultListener(reconciliationTaskResultListener);

        // Resources
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceFile(), getResourceOid(), initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        // LDAP content
        openDJController.addEntriesFromLdifFile(STRUCT_LDIF_FILE.getPath());

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);

        // Role
        importObjectFromFile(ROLE_BASIC_FILE, initResult);
        importObjectFromFile(ROLE_UNIX_FILE, initResult);
        importObjectFromFile(ROLE_META_LDAPGROUP_FILE, initResult);
        importObjectFromFile(ROLE_META_UNIXGROUP_FILE, initResult);
        importObjectFromFile(ROLE_META_UNIXGROUP2_FILE, initResult);

        // Sequence
        importObjectFromFile(SEQUENCE_UIDNUMBER_FILE, initResult);
        importObjectFromFile(SEQUENCE_GIDNUMBER_FILE, initResult);

//        DebugUtil.setDetailedDebugDump(true);
    }

    protected File getResourceFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    protected String getResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(getResourceOid(), task);
        TestUtil.assertSuccess(testResultOpenDj);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
    }

    @Test
    public void test010Schema() throws Exception {
        resourceOpenDj = getObject(ResourceType.class, getResourceOid());
        resourceOpenDjType = resourceOpenDj.asObjectable();

        IntegrationTestTools.displayXml("Initialized resource", resourceOpenDj);

        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceOpenDj, prismContext);
        displayDumpable("OpenDJ schema (resource)", resourceSchema);

        ObjectClassComplexTypeDefinition ocDefPosixAccount = resourceSchema.findObjectClassDefinition(OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        assertNotNull("No objectclass " + OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME + " in resource schema", ocDefPosixAccount);
        assertTrue("Objectclass " + OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME + " is not auxiliary", ocDefPosixAccount.isAuxiliary());

        ObjectClassComplexTypeDefinition ocDefPosixGroup = resourceSchema.findObjectClassDefinition(OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        assertNotNull("No objectclass " + OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME + " in resource schema", ocDefPosixGroup);
        assertTrue("Objectclass " + OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME + " is not auxiliary", ocDefPosixGroup.isAuxiliary());

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceOpenDj);
        displayDumpable("OpenDJ schema (refined)", refinedSchema);

        RefinedObjectClassDefinition rOcDefPosixAccount = refinedSchema.getRefinedDefinition(OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        assertNotNull("No refined objectclass " + OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME + " in resource schema", rOcDefPosixAccount);
        assertTrue("Refined objectclass " + OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME + " is not auxiliary", rOcDefPosixAccount.isAuxiliary());

        RefinedObjectClassDefinition rOcDefPosixGroup = refinedSchema.getRefinedDefinition(OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        assertNotNull("No refined objectclass " + OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME + " in resource schema", rOcDefPosixGroup);
        assertTrue("Refined objectclass " + OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME + " is not auxiliary", rOcDefPosixGroup.isAuxiliary());

    }

    @Test
    public void test100AddUserHermanBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_HERMAN_USERNAME, USER_HERMAN_FIST_NAME, USER_HERMAN_LAST_NAME, ROLE_BASIC_OID);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_HERMAN_USERNAME);
        assertNotNull("No herman user", userAfter);
        display("User after", userAfter);
        assertUserHerman(userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);
    }

    @Test
    public void test110AddUserMancombUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_MANCOMB_USERNAME, USER_MANCOMB_FIST_NAME, USER_MANCOMB_LAST_NAME, ROLE_UNIX_OID);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_MANCOMB_USERNAME);
        assertNotNull("No mancomb user", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_MANCOMB_USERNAME, USER_MANCOMB_FIST_NAME, USER_MANCOMB_LAST_NAME, 1001);
        accountMancombOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountMancombOid);
        display("Shadow (model)", shadow);
        accountMancombDn = assertPosixAccount(shadow, 1001);
    }

    @Test
    public void test111AccountMancombEditObjectClassDefinition() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadow = getShadowModel(accountMancombOid);
        display("shadow", shadow);

        // WHEN
        when();
        RefinedObjectClassDefinition editObjectClassDefinition = modelInteractionService.getEditObjectClassDefinition(shadow, resourceOpenDj, AuthorizationPhaseType.REQUEST, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        displayDumpable("OC def", editObjectClassDefinition);

        PrismAsserts.assertPropertyDefinition(editObjectClassDefinition,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "cn"), DOMUtil.XSD_STRING, 1, -1);
        PrismAsserts.assertPropertyDefinition(editObjectClassDefinition,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "o"), DOMUtil.XSD_STRING, 0, -1);
        PrismAsserts.assertPropertyDefinition(editObjectClassDefinition,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "uidNumber"), DOMUtil.XSD_INT, 1, 1);
        PrismAsserts.assertPropertyDefinition(editObjectClassDefinition,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "gidNumber"), DOMUtil.XSD_INT, 1, 1);
    }

    @Test
    public void test119DeleteUserMancombUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_MANCOMB_USERNAME);

        // WHEN
        when();
        deleteObject(UserType.class, userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_MANCOMB_USERNAME);
        display("User after", userAfter);
        assertNull("User mancomb sneaked in", userAfter);

        assertNoObject(ShadowType.class, accountMancombOid, task, result);

        openDJController.assertNoEntry(accountMancombDn);
    }

    @Test
    public void test120AddUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, (String) null);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);
        assertLinks(userAfter, 0);
    }

    @Test
    public void test122AssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);

        accountLargoOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountLargoOid);
        display("Shadow (model)", shadow);
        accountLargoDn = assertBasicAccount(shadow);
    }

    @Test
    public void test124AssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        long startTs = System.currentTimeMillis();

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        long endTs = System.currentTimeMillis();

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);

        assertModifyTimestamp(shadow, startTs, endTs);
    }

    @Test
    public void test125RecomputeUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
    }

    @Test
    public void test126UnAssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);
    }

    @Test
    public void test127RecomputeUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);
    }

    @Test
    public void test128UnAssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertLinks(userAfter, 0);

        assertNoObject(ShadowType.class, accountLargoOid, task, result);

        openDJController.assertNoEntry(accountLargoDn);
    }

    @Test
    public void test129RecomputeUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertLinks(userAfter, 0);

        assertNoObject(ShadowType.class, accountLargoOid, task, result);

        openDJController.assertNoEntry(accountLargoDn);
    }

    @Test
    public void test130AssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        accountLargoDn = assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
    }

    @Test
    public void test131ReconcileUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(0);
    }

    /**
     * Modify the account directly on resource: remove aux object class, remove the
     * attributes. Then reconcile the user. The recon should fix it.
     */
    @Test
    public void test132MeddleWithAccountAndReconcileUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        openDJController.executeLdifChange(
                "dn: " + accountLargoDn + "\n" +
                        "changetype: modify\n" +
                        "delete: objectClass\n" +
                        "objectClass: posixAccount\n" +
                        "-\n" +
                        "delete: homeDirectory\n" +
                        "homeDirectory: /home/largo\n" +
                        "-\n" +
                        "delete: uidNumber\n" +
                        "uidNumber: " + USER_LARGO_UID_NUMBER + "\n" +
                        "-\n" +
                        "delete: gidNumber\n" +
                        "gidNumber: " + USER_LARGO_UID_NUMBER + "\n"
        );

        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertTest132User(userAfter);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);

        assertTest132Audit();
    }

    protected void assertTest132User(PrismObject<UserType> userAfter) {
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
    }

    protected void assertTest132Audit() {
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
    }

    /**
     * Reconcile user again. Without any meddling.
     * Just to make sure that the second run will not destroy anything.
     */
    @Test
    public void test133ReconcileUserLargoAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(0);
    }

    @Test
    public void test134AssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        dummyAuditService.clear();

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
    }

    @Test
    public void test135UnAssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        dummyAuditService.clear();

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);

        accountLargoOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountLargoOid);
        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);

        assertTest135Audit();
    }

    protected void assertTest135Audit() {
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
    }

    /**
     * Modify the account directly on resource: add aux object class, add the
     * attributes. Then reconcile the user. The recon should fix it.
     */
    @Test // MID-2883
    public void test136MeddleWithAccountAndReconcileUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        openDJController.executeLdifChange(
                "dn: " + accountLargoDn + "\n" +
                        "changetype: modify\n" +
                        "add: objectClass\n" +
                        "objectClass: posixAccount\n" +
                        "-\n" +
                        "add: homeDirectory\n" +
                        "homeDirectory: /home/largo\n" +
                        "-\n" +
                        "add: uidNumber\n" +
                        "uidNumber: " + USER_LARGO_UID_NUMBER + "\n" +
                        "-\n" +
                        "add: gidNumber\n" +
                        "gidNumber: " + USER_LARGO_UID_NUMBER + "\n"
        );

        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccountTest136(shadow);

        // TODO: check audit
    }

    protected void assertAccountTest136(PrismObject<ShadowType> shadow) throws Exception {
        assertBasicAccount(shadow);
    }

    /**
     * Reconcile user again. Without any meddling.
     * Just to make sure that the second run will not destroy anything.
     */
    @Test
    public void test137ReconcileUserLargoAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertTest137User(userAfter);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertTest137Account(shadow);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertExecutionDeltas(0);
    }

    protected void assertTest137User(PrismObject<UserType> userAfter) {
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
    }

    protected void assertTest137Account(PrismObject<ShadowType> shadow) throws Exception {
        assertBasicAccount(shadow);
    }

    @Test
    public void test138UnAssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertLinks(userAfter, 0);

        assertNoObject(ShadowType.class, accountLargoOid, task, result);

        openDJController.assertNoEntry(accountLargoDn);
    }

    // test140-150 are in subclass

    @Test
    public void test200AddLdapGroupMonkeyIsland() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = createLdapGroupRole(ROLE_MONKEY_ISLAND_NAME);

        // WHEN
        when();
        addObject(role, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, role.getOid());
        assertNotNull("No role", roleAfter);
        display("Role after", roleAfter);
        new PrismObjectAsserter<>(roleAfter)
                .assertSanity();
        roleMonkeyIslandOid = roleAfter.getOid();
        groupMonkeyIslandOid = getSingleLinkOid(roleAfter);

        PrismObject<ShadowType> shadow = getShadowModel(groupMonkeyIslandOid);
        display("Shadow (model)", shadow);
        groupMonkeyIslandDn = assertLdapGroup(shadow);
    }

    @Test
    public void test202AssignUserHermanMonkeyIsland() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_USERNAME);

        // WHEN
        when();
        assignRole(user.getOid(), roleMonkeyIslandOid);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_HERMAN_USERNAME);
        assertNotNull("No herman user", userAfter);
        display("User after", userAfter);
        assertUserHerman(userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        String accountHermanDn = assertBasicAccount(shadow);
        openDJController.assertUniqueMember(groupMonkeyIslandDn, accountHermanDn);
    }

    @Test
    public void test210AddUnixGroupVillains() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = createUnixGroupRole(ROLE_VILLAINS_NAME, ROLE_META_UNIXGROUP_OID);

        // WHEN
        when();
        addObject(role, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, role.getOid());
        assertNotNull("No role", roleAfter);
        display("Role after", roleAfter);
        new PrismObjectAsserter<>(roleAfter)
                .assertSanity();
        roleVillainsOid = roleAfter.getOid();
        String ldapGroupOid = getSingleLinkOid(roleAfter);

        PrismObject<ShadowType> shadow = getShadowModel(ldapGroupOid);
        display("Shadow (model)", shadow);
        groupVillainsDn = assertUnixGroup(shadow, ROLE_VILLAINS_GID);
    }

    @Test
    public void test211AssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
    }

    @Test
    public void test212AssignUserLargoVillains() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(user.getOid(), roleVillainsOid);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        String accountLArgoDn = assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
        Entry groupVillains = openDJController.fetchEntry(groupVillainsDn);
        OpenDJController.assertAttribute(groupVillains, "memberUid", USER_LARGO_USERNAME);
        //openDJController.assertAttribute(groupVillains, "memberUid", Integer.toString(USER_LARGO_UID_NUMBER));
    }

    @Test
    public void test250AddUserRangerBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME, ROLE_BASIC_OID);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME);
        assertNotNull("No ranger user", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);
    }

    @Test
    public void test251AssignUserRangerBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_RANGER_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME);

        accountRangerOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountRangerOid);
        display("Shadow (model)", shadow);
        accountRangerDn = assertBasicAccount(shadow);
    }

    @Test
    public void test252AddUnixGroupRangers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = createUnixGroupRole(ROLE_RANGERS_NAME, ROLE_META_UNIXGROUP2_OID);

        // WHEN
        when();
        addObject(role, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<RoleType> roleAfter = getObject(RoleType.class, role.getOid());
        assertNotNull("No role", roleAfter);
        display("Role after", roleAfter);
        new PrismObjectAsserter<>(roleAfter)
                .assertSanity();
        roleRangersOid = roleAfter.getOid();
        groupRangersOid = getSingleLinkOid(roleAfter);

        PrismObject<ShadowType> shadow = getShadowModel(groupRangersOid);
        display("Shadow (model)", shadow);
        groupRangersDn = assertUnixGroup(shadow, ROLE_RANGERS_GID);
    }

    @Test
    public void test253AddUnixGroupSeals() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = createUnixGroupRole(ROLE_SEALS_NAME, ROLE_META_UNIXGROUP2_OID);

        // WHEN
        when();
        addObject(role, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<RoleType> roleAfter = getObject(RoleType.class, role.getOid());
        assertNotNull("No role", roleAfter);
        display("Role after", roleAfter);
        new PrismObjectAsserter<>(roleAfter)
                .assertSanity();
        roleSealsOid = roleAfter.getOid();
        groupSealsOid = getSingleLinkOid(roleAfter);

        PrismObject<ShadowType> shadow = getShadowModel(groupSealsOid);
        display("Shadow (model)", shadow);
        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, groupSealsOid, null, result);
        display("Shadow (repo)", shadowRepo);
        groupSealsDn = assertUnixGroup(shadow, ROLE_SEALS_GID);
    }

    @Test
    public void test254AssignUserRangerRangers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_RANGER_USERNAME);

        // WHEN
        when();
        assignRole(user.getOid(), roleRangersOid);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME);
        assertNotNull("No user", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME);
        String accountOid = getSingleLinkOid(userAfter);

        then();
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        String accountRangerDn = assertPosixAccount(shadow, USER_RANGER_UID_NUMBER);
        Entry groupRangers = openDJController.fetchEntry(groupRangersDn);
        //openDJController.assertAttribute(groupRangers, "memberUid", Integer.toString(USER_RANGER_UID_NUMBER));
        OpenDJController.assertAttribute(groupRangers, "memberUid", USER_RANGER_USERNAME);

        assertGroupAssociation(shadow, groupRangersOid);

        PrismObject<ShadowType> repoShadow = provisioningService.getObject(ShadowType.class, accountOid, SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
        display("Shadow (repo)", repoShadow);
        //PrismProperty<Integer> uidNumberRepoAttr = repoShadow.findProperty(prismContext.path(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_UIDNUMBER_ATTRIBUTE_NAME)));
        //PrismAsserts.assertPropertyValue(uidNumberRepoAttr, USER_RANGER_UID_NUMBER);
        PrismProperty<String> uidRepoAttr = repoShadow.findProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_UID_ATTRIBUTE_NAME)));
        PrismAsserts.assertPropertyValue(uidRepoAttr, USER_RANGER_USERNAME);
    }

    @Test
    public void test255AssignUserRangerSeals() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = findUserByUsername(USER_RANGER_USERNAME);

        // WHEN
        when();
        assignRole(user.getOid(), roleSealsOid);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME);
        assertNotNull("No user", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        String accountLArgoDn = assertPosixAccount(shadow, USER_RANGER_UID_NUMBER);
        Entry groupSeals = openDJController.fetchEntry(groupSealsDn);
        //openDJController.assertAttribute(groupSeals, "memberUid", Integer.toString(USER_RANGER_UID_NUMBER));
        OpenDJController.assertAttribute(groupSeals, "memberUid", USER_RANGER_USERNAME);

        assertGroupAssociation(shadow, groupRangersOid);
        assertGroupAssociation(shadow, groupSealsOid);
    }

    @Test
    public void test256UnAssignUserRangerSealsKeepRangers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_RANGER_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), roleSealsOid);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_RANGER_USERNAME, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME, USER_RANGER_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_RANGER_UID_NUMBER);

        // account should still be in the rangers group
        Entry groupRangers = openDJController.fetchEntry(groupRangersDn);
        //openDJController.assertAttribute(groupRangers, "memberUid", Integer.toString(USER_RANGER_UID_NUMBER));
        OpenDJController.assertAttribute(groupRangers, "memberUid", USER_RANGER_USERNAME);

        // account should not be in the group anymore. memberUid should be
        // empty...
        Entry groupSeals = openDJController.fetchEntry(groupSealsDn);
        OpenDJController.assertNoAttribute(groupSeals, "memberUid");
    }

    @Test
    public void test257RenameUserAndAccountsCheckGroupmembership() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_RANGER_USERNAME);

        // WHEN
        when();
        modifyUserReplace(userBefore.getOid(), UserType.F_NAME, task, result, new PolyString("usranger", "usranger"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME_RENAMED);
        assertNotNull("User not renamed", userAfter);
        display("User after rename", userAfter);
        assertUserPosix(userAfter, USER_RANGER_USERNAME_RENAMED, USER_RANGER_FIST_NAME, USER_RANGER_LAST_NAME, USER_RANGER_UID_NUMBER);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_RANGER_UID_NUMBER);

        // account should still be in the rangers group, but renamed from
        // ranger to usranger
        PrismObject<ShadowType> shadowGroup = getShadowModel(groupRangersOid);
        display("Shadow rangers group (model)", shadowGroup);
        Entry groupRangers = openDJController.fetchEntry(groupRangersDn);
        assertUnixGroup(shadowGroup, ROLE_RANGERS_GID);

        OpenDJController.assertAttribute(groupRangers, "memberUid", USER_RANGER_USERNAME_RENAMED);

    }

    @Test
    public void test260DeleteUserUsrangerUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_RANGER_USERNAME_RENAMED);

        // WHEN
        when();
        deleteObject(UserType.class, userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_RANGER_USERNAME_RENAMED);
        display("User after", userAfter);
        assertNull("User usranger sneaked in", userAfter);

        assertNoObject(ShadowType.class, accountRangerOid, task, result);

        openDJController.assertNoEntry(accountRangerDn);
    }

    /**
     * MID-3535
     */
    @Test
    public void test270RenameUnixGroupSeals() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        renameObject(RoleType.class, roleSealsOid, ROLE_WALRUSES_NAME, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<RoleType> roleAfter = getObject(RoleType.class, roleSealsOid);
        assertNotNull("No role", roleAfter);
        display("Role after", roleAfter);
        new PrismObjectAsserter<>(roleAfter)
                .assertSanity();
        assertEquals("link OID changed", groupSealsOid, getSingleLinkOid(roleAfter));

        PrismObject<ShadowType> shadow = getShadowModel(groupSealsOid);
        display("Shadow (model)", shadow);
        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, groupSealsOid, null, result);
        display("Shadow (repo)", shadowRepo);
        assertUnixGroup(shadow, ROLE_SEALS_GID);
    }

    @Test
    public void test300AddUserCapsizeUnixFail() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<SequenceType> sequenceBefore = getObject(SequenceType.class, SEQUENCE_UIDNUMBER_OID);
        display("Sequence before", sequenceBefore);
        assertEquals("Wrong sequence counter (precondition)", USER_CAPSIZE_UID_NUMBER, sequenceBefore.asObjectable().getCounter().intValue());
        assertTrue("Unexpected unused values in the sequence (precondition)", sequenceBefore.asObjectable().getUnusedValues().isEmpty());

        PrismObject<UserType> user = createUser(USER_CAPSIZE_USERNAME, USER_CAPSIZE_FIST_NAME, USER_CAPSIZE_LAST_NAME, ROLE_UNIX_OID);
        user.asObjectable().getEmployeeType().add("troublemaker");

        try {
            // WHEN
            when();
            addObject(user, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (ExpressionEvaluationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_CAPSIZE_USERNAME);
        display("User after", userAfter);
        assertNull("User capsize sneaked in", userAfter);

        PrismObject<SequenceType> sequenceAfter = getObject(SequenceType.class, SEQUENCE_UIDNUMBER_OID);
        display("Sequence after", sequenceAfter);
        assertEquals("Sequence haven't moved", USER_CAPSIZE_UID_NUMBER + 1, sequenceAfter.asObjectable().getCounter().intValue());
        assertFalse("No unused values in the sequence", sequenceAfter.asObjectable().getUnusedValues().isEmpty());
    }

    /**
     * This should go well. It should reuse the identifier that was originally assigned to
     * Kate Capsise, but not used.
     */
    @Test
    public void test310AddUserWallyUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<SequenceType> sequenceBefore = getObject(SequenceType.class, SEQUENCE_UIDNUMBER_OID);
        display("Sequence before", sequenceBefore);
        assertEquals("Wrong sequence counter (precondition)", USER_WALLY_UID_NUMBER + 1, sequenceBefore.asObjectable().getCounter().intValue());
        assertFalse("Missing unused values in the sequence (precondition)", sequenceBefore.asObjectable().getUnusedValues().isEmpty());

        PrismObject<UserType> user = createUser(USER_WALLY_USERNAME, USER_WALLY_FIST_NAME, USER_WALLY_LAST_NAME, ROLE_UNIX_OID);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_WALLY_USERNAME);
        assertNotNull("No wally user", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_WALLY_USERNAME, USER_WALLY_FIST_NAME, USER_WALLY_LAST_NAME, USER_WALLY_UID_NUMBER);
        accountWallyOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountWallyOid);
        display("Shadow (model)", shadow);
        accountWallyDn = assertPosixAccount(shadow, USER_WALLY_UID_NUMBER);

        PrismObject<SequenceType> sequenceAfter = getObject(SequenceType.class, SEQUENCE_UIDNUMBER_OID);
        display("Sequence after", sequenceAfter);
        assertEquals("Sequence has moved", USER_WALLY_UID_NUMBER + 1, sequenceAfter.asObjectable().getCounter().intValue());
        assertTrue("Unexpected unused values in the sequence", sequenceAfter.asObjectable().getUnusedValues().isEmpty());
    }

    /**
     * Remove posixAccount directly in LDAP server. Then try to get the account. MidPoint should survive that.
     */
    @Test
    public void test312AccountWallyRemovePosixObjectclassNative() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.executeLdifChange("dn: " + accountWallyDn + "\n" +
                "changetype: modify\n" +
                "delete: objectclass\n" +
                "objectclass: posixAccount\n" +
                "-\n" +
                "delete: uidNumber\n" +
                "uidNumber: " + USER_WALLY_UID_NUMBER + "\n" +
                "-\n" +
                "delete: gidNumber\n" +
                "gidNumber: " + USER_WALLY_UID_NUMBER + "\n" +
                "-\n" +
                "delete: homeDirectory\n" +
                "homeDirectory: /home/wally");

        Entry entryWallyBefore = openDJController.fetchEntry(accountWallyDn);
        display("Wally LDAP account before", entryWallyBefore);

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountWallyOid, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Shadow (model)", shadow);
        assertBasicAccount(shadow);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountWallyOid, null, result);
        display("Shadow (repo)", repoShadow);
        PrismAsserts.assertNoItem(repoShadow, ShadowType.F_AUXILIARY_OBJECT_CLASS);
//        PrismAsserts.assertPropertyValue(repoShadow, ShadowType.F_AUXILIARY_OBJECT_CLASS);

        PrismObject<UserType> userAfter = findUserByUsername(USER_WALLY_USERNAME);
        assertNotNull("No wally user", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_WALLY_USERNAME, USER_WALLY_FIST_NAME, USER_WALLY_LAST_NAME, USER_WALLY_UID_NUMBER);
        accountMancombOid = getSingleLinkOid(userAfter);
    }

    /**
     * Add posixAccount directly in LDAP server. Then try to get the account. MidPoint should survive that.
     */
    @Test
    public void test314AccountWallyAddPosixObjectclassNative() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.executeLdifChange("dn: " + accountWallyDn + "\n" +
                "changetype: modify\n" +
                "add: objectclass\n" +
                "objectclass: posixAccount\n" +
                "-\n" +
                "add: uidNumber\n" +
                "uidNumber: " + USER_WALLY_UID_NUMBER + "\n" +
                "-\n" +
                "add: gidNumber\n" +
                "gidNumber: " + USER_WALLY_UID_NUMBER + "\n" +
                "-\n" +
                "add: homeDirectory\n" +
                "homeDirectory: /home/wally");

        Entry entryWallyBefore = openDJController.fetchEntry(accountWallyDn);
        display("Wally LDAP account before", entryWallyBefore);

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountWallyOid, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, USER_WALLY_UID_NUMBER);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountWallyOid, null, result);
        display("Shadow (repo)", repoShadow);
        PrismAsserts.assertPropertyValue(repoShadow, ShadowType.F_AUXILIARY_OBJECT_CLASS, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);

        PrismObject<UserType> userAfter = findUserByUsername(USER_WALLY_USERNAME);
        assertNotNull("No wally user", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_WALLY_USERNAME, USER_WALLY_FIST_NAME, USER_WALLY_LAST_NAME, USER_WALLY_UID_NUMBER);
        accountMancombOid = getSingleLinkOid(userAfter);
    }

    @Test
    public void test400ListAllAccountsObjectClass() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(),
                OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME, prismContext);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("found objects", objects);
        assertEquals("Wrong number of objects found", 7, objects.size());
    }

    @Test
    public void test401ListAllAccountsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(),
                ShadowKindType.ACCOUNT, "default", prismContext);
        displayDumpable("query", query);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("found objects", objects);
        assertEquals("Wrong number of objects found", 7, objects.size());
    }

    @Test
    public void test402ListLdapGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(),
                ShadowKindType.ENTITLEMENT, "ldapGroup", prismContext);
        displayDumpable("query", query);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("found objects", objects);
        assertEquals("Wrong number of objects found", 2, objects.size());
    }

    @Test
    public void test403ListUnixGroupsKindIntent() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(),
                ShadowKindType.ENTITLEMENT, "unixGroup", prismContext);
        displayDumpable("query", query);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("found objects", objects);
        assertEquals("Wrong number of objects found", 3, objects.size());
    }

    @Test
    public void test500AddUserStan() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user = createUser(USER_STAN_USERNAME, USER_STAN_FIST_NAME, USER_STAN_LAST_NAME, roleRangersOid);
        addRoleAssignment(user, roleMonkeyIslandOid);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_STAN_USERNAME);
        assertNotNull("No stan user", userAfter);
        display("User after", userAfter);
        assertUserStan(userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertPosixAccount(shadow, null);

        assertGroupAssociation(shadow, groupRangersOid);
        assertGroupAssociation(shadow, groupMonkeyIslandOid);

        display("Rangers", getShadowModel(groupRangersOid));
    }

    @Test
    public void test510StanDisablePosixAssocAndReconcile() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userStan = findUserByUsername(USER_STAN_USERNAME);
        Long rangersAssignmentId = null;
        for (AssignmentType assignment : userStan.asObjectable().getAssignment()) {
            if (assignment.getTargetRef() != null && roleRangersOid.equals(assignment.getTargetRef().getOid())) {
                rangersAssignmentId = assignment.getId();
            }
        }
        assertNotNull("No 'rangers' assignment for stan", rangersAssignmentId);
        final List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, rangersAssignmentId, AssignmentType.F_ACTIVATION,
                        ActivationType.F_ADMINISTRATIVE_STATUS)
                .replace(ActivationStatusType.DISABLED)
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userStan.getOid(), itemDeltas, result);

        // WHEN
        when();
        reconcileUser(userStan.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_STAN_USERNAME);
        assertNotNull("No stan user", userAfter);
        display("User after", userAfter);
        assertUserStan(userAfter);
        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);

        assertAccountTest510(shadow);

        display("Rangers", getShadowModel(groupRangersOid));

        /*

          Actually, stan is technically still a member of Rangers.
          (Although not shown to midPoint, as he is no longer "posixAccount".)
          This can be avoided by setting the associations as non-tolerant.

        attributes:
        dn:
          cn=rangers,ou=unixgroups,dc=example,dc=com
        cn: [ rangers ]
        gidNumber: 998
        memberUid: [ stan ]
        entryUUID: 8647ca7a-2b7a-4948-9e9b-a1657028fbfe
         */
    }

    protected void assertAccountTest510(PrismObject<ShadowType> shadow) throws Exception {
        assertBasicAccount(shadow);

        assertNoGroupAssociation(shadow, groupRangersOid);
        assertGroupAssociation(shadow, groupMonkeyIslandOid);
    }

    private PrismObject<UserType> createUser(String username, String givenName, String familyName, String roleOid) throws SchemaException {
        PrismObject<UserType> user = createUser(username, givenName, familyName, true);
        if (roleOid != null) {
            addRoleAssignment(user, roleOid);
        }
        return user;
    }

    private void addRoleAssignment(PrismObject<UserType> user, String roleOid) {
        AssignmentType roleAssignemnt = new AssignmentType();
        ObjectReferenceType roleTargetRef = new ObjectReferenceType();
        roleTargetRef.setOid(roleOid);
        roleTargetRef.setType(RoleType.COMPLEX_TYPE);
        roleAssignemnt.setTargetRef(roleTargetRef);
        user.asObjectable().getAssignment().add(roleAssignemnt);
    }

    protected void assertUserHerman(PrismObject<UserType> user) {
        assertUser(user, USER_HERMAN_USERNAME, USER_HERMAN_FIST_NAME, USER_HERMAN_LAST_NAME);
    }

    protected void assertUserStan(PrismObject<UserType> user) {
        assertUser(user, USER_STAN_USERNAME, USER_STAN_FIST_NAME, USER_STAN_LAST_NAME);
    }

    protected void assertUser(PrismObject<UserType> user, String username, String firstName, String lastName) {
        assertUser(user, user.getOid(), username, firstName + " " + lastName,
                firstName, lastName);
    }

    protected void assertUserPosix(PrismObject<UserType> user, String username, String firstName, String lastName, int uidNumber) {
        assertUser(user, user.getOid(), username, firstName + " " + lastName,
                firstName, lastName);
        PrismContainer<?> extension = user.getExtension();
        assertNotNull("No extension in " + user, extension);
        PrismAsserts.assertPropertyValue(extension, EXTENSION_UID_NUMBER_NAME, Integer.toString(uidNumber));
    }

    protected String assertBasicAccount(PrismObject<ShadowType> shadow) throws DirectoryException {
        ShadowType shadowType = shadow.asObjectable();
        assertEquals("Wrong objectclass in " + shadow, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME, shadowType.getObjectClass());
        assertTrue("Unexpected auxiliary objectclasses in " + shadow + ": " + shadowType.getAuxiliaryObjectClass(),
                shadowType.getAuxiliaryObjectClass().isEmpty());
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No ou LDAP entry for " + dn, entry);
        display("Posix account entry", entry);
        OpenDJController.assertObjectClass(entry, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME.getLocalPart());
        OpenDJController.assertNoObjectClass(entry, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME.getLocalPart());

        return entry.getDN().toString();
    }

    protected String assertAccount(PrismObject<ShadowType> shadow, QName... expectedAuxObjectClasses) throws DirectoryException {
        ShadowType shadowType = shadow.asObjectable();
        assertEquals("Wrong objectclass in " + shadow, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME, shadowType.getObjectClass());
        PrismAsserts.assertEqualsCollectionUnordered("Wrong auxiliary objectclasses in " + shadow,
                shadowType.getAuxiliaryObjectClass(), expectedAuxObjectClasses);
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No ou LDAP entry for " + dn, entry);
        display("Posix account entry", entry);
        OpenDJController.assertObjectClass(entry, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME.getLocalPart());

        return entry.getDN().toString();
    }

    protected String assertPosixAccount(PrismObject<ShadowType> shadow, Integer expectedUid) throws DirectoryException {
        ShadowType shadowType = shadow.asObjectable();
        assertEquals("Wrong objectclass in " + shadow, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME, shadowType.getObjectClass());
        PrismAsserts.assertEqualsCollectionUnordered("Wrong auxiliary objectclasses in " + shadow,
                shadowType.getAuxiliaryObjectClass(), OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();
        if (expectedUid != null) {
            ResourceAttribute<Integer> uidNumberAttr = ShadowUtil
                    .getAttribute(shadow, new QName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_UIDNUMBER_ATTRIBUTE_NAME));
            PrismAsserts.assertPropertyValue(uidNumberAttr, expectedUid);
            ResourceAttribute<Integer> gidNumberAttr = ShadowUtil
                    .getAttribute(shadow, new QName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_GIDNUMBER_ATTRIBUTE_NAME));
            PrismAsserts.assertPropertyValue(gidNumberAttr, expectedUid);
        }

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No ou LDAP entry for " + dn, entry);
        display("Posix account entry", entry);
        OpenDJController.assertObjectClass(entry, OPENDJ_ACCOUNT_STRUCTURAL_OBJECTCLASS_NAME.getLocalPart());
        OpenDJController.assertObjectClass(entry, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME.getLocalPart());
        if (expectedUid != null) {
            OpenDJController.assertAttribute(entry, OPENDJ_UIDNUMBER_ATTRIBUTE_NAME, Integer.toString(expectedUid));
            OpenDJController.assertAttribute(entry, OPENDJ_GIDNUMBER_ATTRIBUTE_NAME, Integer.toString(expectedUid));
        }

        return entry.getDN().toString();
    }

    protected ShadowAssociationType assertGroupAssociation(PrismObject<ShadowType> accountShadow, String groupShadowOid) {
        ShadowAssociationType association = findAssociation(accountShadow, groupShadowOid);
        if (association != null) {
            return association;
        }
        AssertJUnit.fail("No association for " + groupShadowOid + " in " + accountShadow);
        return null; // NOT REACHED
    }

    protected void assertNoGroupAssociation(PrismObject<ShadowType> accountShadow, String groupShadowOid) {
        ShadowAssociationType association = findAssociation(accountShadow, groupShadowOid);
        assertNull("Unexpected association for " + groupShadowOid + " in " + accountShadow, association);
    }

    @Nullable
    private ShadowAssociationType findAssociation(PrismObject<ShadowType> accountShadow, String groupShadowOid) {
        ShadowType accountShadowType = accountShadow.asObjectable();
        for (ShadowAssociationType association : accountShadowType.getAssociation()) {
            assertNotNull("Association without shadowRef in " + accountShadow + ": " + association, association.getShadowRef());
            assertNotNull("Association without shadowRef OID in " + accountShadow + ": " + association, association.getShadowRef().getOid());
            if (association.getShadowRef().getOid().equals(groupShadowOid)) {
                return association;
            }
        }
        return null;
    }

    private PrismObject<RoleType> createLdapGroupRole(String name) throws SchemaException {
        PrismObject<RoleType> role = getRoleDefinition().instantiate();
        RoleType roleType = role.asObjectable();
        roleType.setName(new PolyStringType(name));
        AssignmentType roleAssignemnt = new AssignmentType();
        ObjectReferenceType roleTargetRef = new ObjectReferenceType();
        roleTargetRef.setOid(ROLE_META_LDAPGROUP_OID);
        roleTargetRef.setType(RoleType.COMPLEX_TYPE);
        roleAssignemnt.setTargetRef(roleTargetRef);
        roleType.getAssignment().add(roleAssignemnt);
        return role;
    }

    private PrismObject<RoleType> createUnixGroupRole(String name, String metaRoleOid) throws SchemaException {
        PrismObject<RoleType> role = getRoleDefinition().instantiate();
        RoleType roleType = role.asObjectable();
        roleType.setName(new PolyStringType(name));

        AssignmentType roleAssignemnt = new AssignmentType();
        ObjectReferenceType roleTargetRef = new ObjectReferenceType();
        roleTargetRef.setOid(metaRoleOid);
        roleTargetRef.setType(RoleType.COMPLEX_TYPE);
        roleAssignemnt.setTargetRef(roleTargetRef);
        roleType.getAssignment().add(roleAssignemnt);

        return role;
    }

    private String assertLdapGroup(PrismObject<ShadowType> shadow) throws DirectoryException {
        ShadowType shadowType = shadow.asObjectable();
        assertEquals("Wrong objectclass in " + shadow, OPENDJ_GROUP_STRUCTURAL_OBJECTCLASS_NAME, shadowType.getObjectClass());
        assertTrue("Unexpected auxiliary objectclasses in " + shadow + ": " + shadowType.getAuxiliaryObjectClass(),
                shadowType.getAuxiliaryObjectClass().isEmpty());
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No group LDAP entry for " + dn, entry);
        display("Ldap group entry", entry);
        OpenDJController.assertObjectClass(entry, OPENDJ_GROUP_STRUCTURAL_OBJECTCLASS_NAME.getLocalPart());
        OpenDJController.assertNoObjectClass(entry, OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME.getLocalPart());

        return entry.getDN().toString();
    }

    private String assertUnixGroup(PrismObject<ShadowType> shadow, Integer expectedGidNumber) throws DirectoryException {
        ShadowType shadowType = shadow.asObjectable();
        assertEquals("Wrong objectclass in " + shadow, OPENDJ_GROUP_UNIX_STRUCTURAL_OBJECTCLASS_NAME, shadowType.getObjectClass());
        PrismAsserts.assertEqualsCollectionUnordered("Wrong auxiliary objectclasses in " + shadow,
                shadowType.getAuxiliaryObjectClass(), OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME);
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();
        ResourceAttribute<Integer> gidNumberAttr = ShadowUtil.getAttribute(shadow, new QName(RESOURCE_OPENDJ_NAMESPACE, OPENDJ_GIDNUMBER_ATTRIBUTE_NAME));
        PrismAsserts.assertPropertyValue(gidNumberAttr, expectedGidNumber);

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No group LDAP entry for " + dn, entry);
        display("Posix account entry", entry);
        OpenDJController.assertObjectClass(entry, OPENDJ_GROUP_UNIX_STRUCTURAL_OBJECTCLASS_NAME.getLocalPart());
        OpenDJController.assertObjectClass(entry, OPENDJ_GROUP_POSIX_AUXILIARY_OBJECTCLASS_NAME.getLocalPart());
        OpenDJController.assertAttribute(entry, "gidNumber", expectedGidNumber.toString());

        return entry.getDN().toString();
    }

    protected void assertModifyTimestamp(PrismObject<ShadowType> shadow, long startTs, long endTs) throws Exception {
        Long actual = getTimestampAttribute(shadow);
        TestUtil.assertBetween("Wrong modify timestamp attribute in " + shadow, startTs - 1000, endTs + 1000, actual);
    }

    protected Long getTimestampAttribute(PrismObject<ShadowType> shadow) throws Exception {
        XMLGregorianCalendar attributeValue = ShadowUtil.getAttributeValue(shadow, OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_QNAME);
        return MiscUtil.asLong(attributeValue);
    }
}
