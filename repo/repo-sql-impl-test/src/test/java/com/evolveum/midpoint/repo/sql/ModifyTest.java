/*
  * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/modify");
    private static final File ACCOUNT_FILE = new File(TEST_DIR, "account.xml");
    private static final File MODIFY_USER_ADD_LINK = new File(TEST_DIR, "change-add.xml");

	private static final Trace LOGGER = TraceManager.getTrace(ModifyTest.class);

    private static final QName QNAME_LOOT = new QName("http://example.com/p", "loot");
    private static final QName QNAME_WEAPON = new QName("http://example.com/p", "weapon");

	@Override
    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

	@BeforeClass
	public void setupClass() {
		InternalsConfig.encryptionChecks = false;
	}

	protected RepoModifyOptions getModifyOptions() {
		return null;
	}

    @Test(expectedExceptions = SystemException.class, enabled = false)
    public void test010ModifyWithExistingName() throws Exception {
    	final String TEST_NAME = "test010ModifyWithExistingName";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult("MODIFY");

        File userFile = new File(TEST_DIR, "modify-user.xml");
        //add first user
        PrismObject<UserType> user = prismContext.parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "existingName");
        repositoryService.addObject(user, null, result);

        //add second user
        user = prismContext.parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "otherName");
        String oid = repositoryService.addObject(user, null, result);

        //modify second user name to "existingName"
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "change-name.xml"),
                ObjectModificationType.COMPLEX_TYPE);
        modification.setOid(oid);
        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);
    }

    @Test(expectedExceptions = ObjectNotFoundException.class, enabled = false)
    public void test020ModifyNotExistingUser() throws Exception {
    	final String TEST_NAME = "test020ModifyNotExistingUser";
    	TestUtil.displayTestTitle(TEST_NAME);

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        OperationResult result = new OperationResult("MODIFY");
        repositoryService.modifyObject(UserType.class, "1234", deltas, getModifyOptions(), result);
    }

    @Test(enabled = false) // MID-3483
    public void test030ModifyUserOnNonExistingAccountTest() throws Exception {
    	final String TEST_NAME = "test030ModifyUserOnNonExistingAccountTest";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult("MODIFY");

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName(new PolyStringType("non-existing-account-user"));

        String oid = repositoryService.addObject(user, null, result);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "change-add-non-existing.xml"),
                ObjectModificationType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, getModifyOptions(), result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test(enabled=false) // MID-3483
    public void test031ModifyUserOnExistingAccountTest() throws Exception {
    	final String TEST_NAME = "test031ModifyUserOnExistingAccountTest";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
        OperationResult result = new OperationResult(TEST_NAME);

        //add account
        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_FILE);
        repositoryService.addObject(account, null, result);

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.parseObject(userFile);

        String userOid = user.getOid();
        String oid = repositoryService.addObject(user, null, result);
        assertEquals(userOid, oid);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectDeltaType objectDeltaType = PrismTestUtil.parseAnyValue(MODIFY_USER_ADD_LINK);
        ObjectDelta<Objectable> objectDelta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);
        Collection<? extends ItemDelta<?, ?>> deltas = objectDelta.getModifications();

        // WHEN
        repositoryService.modifyObject(UserType.class, oid, deltas, getModifyOptions(), result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test
    public void test032ModifyTaskObjectRef() throws Exception {
    	final String TEST_NAME = "test032ModifyTaskObjectRef";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult(TEST_NAME);
        File taskFile = new File(TEST_DIR, "task.xml");
        System.out.println("ADD");
        PrismObject<TaskType> task = prismContext.parseObject(taskFile);
        repositoryService.addObject(task, null, result);
        final String taskOid = "00000000-0000-0000-0000-123450000001";
        AssertJUnit.assertNotNull(taskOid);
        System.out.println("GET");
        PrismObject<TaskType> getTask = null;
        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        String lastVersion = getTask.getVersion();
        AssertJUnit.assertTrue(task.equivalent(getTask));
        TaskType taskType = null;
        taskType = getTask.asObjectable();
        AssertJUnit.assertNull(taskType.getObjectRef());

        Collection modifications = new ArrayList();

        PrismObjectDefinition objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(
                TaskType.class);
        PrismReferenceDefinition def = objectDef.findReferenceDefinition(TaskType.F_OBJECT_REF);
        System.out.println("MODIFY");
        ObjectReferenceType objectRef = null;
        ReferenceDelta delta = new ReferenceDelta(def, prismContext);
        delta.addValueToAdd(new PrismReferenceValue("1", ResourceType.COMPLEX_TYPE));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, getModifyOptions(), result);
        System.out.println("GET");
        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        assertEquals("1", objectRef.getOid());
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();

        checkReference(taskOid);
        System.out.println("MODIFY");
        modifications.clear();
        delta = new ReferenceDelta(def, prismContext);
        delta.addValueToDelete(new PrismReferenceValue("1", ResourceType.COMPLEX_TYPE));
        delta.addValueToAdd(new PrismReferenceValue("2", ResourceType.COMPLEX_TYPE));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, getModifyOptions(), result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        assertEquals("2", objectRef.getOid());
        LOGGER.info(PrismTestUtil.serializeObjectToString(taskType.asPrismObject()));
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();

        modifications.clear();
        delta = new ReferenceDelta(def, prismContext);
        delta.addValueToDelete(new PrismReferenceValue("2", ResourceType.COMPLEX_TYPE));
        delta.addValueToAdd(new PrismReferenceValue("1", ResourceType.COMPLEX_TYPE));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, getModifyOptions(), result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        assertEquals("1", objectRef.getOid());
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();
    }

    private void checkReference(String taskOid) {
//        Session session = getFactory().openSession();
//        try {
//            Criteria criteria = session.createCriteria(RObjectReferenceTaskObject.class);
//            criteria.add(Restrictions.eq("ownerId", 0L));
//            criteria.add(Restrictions.eq("ownerOid", taskOid));
//
//            criteria.uniqueResult();
//        } catch (Exception ex) {
//            session.close();
//            AssertJUnit.fail(ex.getMessage());
//        }
    }

    @Test
    public void test100ModifyUserAddRole() throws Exception {
    	final String TEST_NAME = "test100ModifyUserAddRole";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult parentResult = new OperationResult("Modify user -> add roles");
        String userToModifyOid = "f65963e3-9d47-4b18-aaf3-bfc98bdfa000";

        PrismObject<ResourceType> csvResource = prismContext.parseObject(new File(TEST_DIR + "/resource-csv.xml"));
        repositoryService.addObject(csvResource, null, parentResult);

        PrismObject<ResourceType> openDjResource = prismContext.parseObject(new File(TEST_DIR + "/resource-opendj.xml"));
        repositoryService.addObject(openDjResource, null, parentResult);

        PrismObject<UserType> user = prismContext.parseObject(new File(TEST_DIR + "/user.xml"));
        repositoryService.addObject(user, null, parentResult);

        PrismObject<RoleType> roleCsv = prismContext.parseObject(new File(TEST_DIR + "/role-csv.xml"));
        repositoryService.addObject(roleCsv, null, parentResult);

        String ldapRoleOid = "12345678-d34d-b33f-f00d-987987987988";
        PrismObject<RoleType> roleLdap = prismContext.parseObject(new File(TEST_DIR + "/role-ldap.xml"));
        repositoryService.addObject(roleLdap, null, parentResult);

        RoleType ldapRole = repositoryService.getObject(RoleType.class, ldapRoleOid, null, parentResult).asObjectable();
        assertEquals("Expected that the role has one approver.", 1, ldapRole.getApproverRef().size());
        assertEquals("Actual approved not equals to expected one.", userToModifyOid, ldapRole.getApproverRef().get(0).getOid());

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(TEST_DIR + "/modify-user-add-roles.xml"),
                ObjectModificationType.COMPLEX_TYPE);


        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);


        repositoryService.modifyObject(UserType.class, userToModifyOid, delta.getModifications(), getModifyOptions(), parentResult);

        UserType modifiedUser = repositoryService.getObject(UserType.class, userToModifyOid, null, parentResult).asObjectable();
        assertEquals("wrong number of assignments", 3, modifiedUser.getAssignment().size());

    }

    @Test
    public void test110ModifyDeleteObjectChangeFromAccount() throws Exception {
    	final String TEST_NAME = "test110ModifyDeleteObjectChangeFromAccount";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult parentResult = new OperationResult("testModifyDeleteObjectChnageFromAccount");
        PrismObject<ShadowType> accShadow = prismContext.parseObject(new File(TEST_DIR + "/account-delete-object-change.xml"));
        String oid = repositoryService.addObject(accShadow, null, parentResult);
        System.out.println("\nAcc shadow");
        System.out.println(accShadow.debugDump());

        accShadow.asObjectable().setObjectChange(null);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.debugDump());
        prismContext.adopt(repoShadow);
        prismContext.adopt(accShadow);
        AssertJUnit.assertTrue("repo shadow must have full definitions", repoShadow.hasCompleteDefinition());
        AssertJUnit.assertTrue("shadow must have full definitions", repoShadow.hasCompleteDefinition());
        ObjectDelta d = repoShadow.diff(accShadow);
        System.out.println("\nDelta");
        System.out.println(d.debugDump());

        repositoryService.modifyObject(ShadowType.class, oid, d.getModifications(), getModifyOptions(), parentResult);

        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());
    }

    /**
     * Modify account metadata. Make sure that no unrelated item has changed.
     */
    @Test(enabled = false) // MID-3484
    public void test120ModifyAccountMetadata() throws Exception {
    	final String TEST_NAME = "test120ModifyAccountMetadata";
    	TestUtil.displayTestTitle(TEST_NAME);

    	// GIVEN
        OperationResult parentResult = new OperationResult(TEST_NAME);

        PrismObject<ShadowType> shadowBefore = prismContext.parseObject(ACCOUNT_FILE);

        MetadataType metaData = new MetadataType();
        metaData.setCreateChannel("channel");
        metaData.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        shadowBefore.asObjectable().setMetadata(metaData);

        // The parsed shadow has attributes that have xsi:type specification. Add another one that has
        // fully dynamic definition

        QName attrBazQName = new QName(MidPointConstants.NS_RI, "baz");
        PrismContainer<Containerable> attributesContainerBefore = shadowBefore.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<String> attrBazBefore = new PrismProperty<>(new QName(MidPointConstants.NS_RI, "baz"), prismContext);
        PrismPropertyDefinitionImpl<String> attrBazDefBefore = new PrismPropertyDefinitionImpl<>(attrBazQName, DOMUtil.XSD_STRING, prismContext);
        attrBazDefBefore.setMaxOccurs(-1);
        attrBazBefore.setDefinition(attrBazDefBefore);
        attrBazBefore.addRealValue("BaZ1");
        attrBazBefore.addRealValue("BaZ2");
        attrBazBefore.addRealValue("BaZ3");
        attributesContainerBefore.add(attrBazBefore);

        System.out.println("\nAcc shadow");
        System.out.println(shadowBefore.debugDump());

        String oid = repositoryService.addObject(shadowBefore, null, parentResult);


        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.debugDump());

        ObjectDelta<ShadowType> d = repoShadow.diff(shadowBefore);
        System.out.println("\nDelta");
        System.out.println(d.debugDump());
        assertTrue("Delta after add is not empty", d.isEmpty());

        PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

        Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
        PropertyDelta pdelta = PropertyDelta.createModificationReplaceProperty(
        		(new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_CHANNEL)), accountDefinition, "channel");
        modifications.add(pdelta);

        XMLGregorianCalendar modifyTimestampBefore = XmlTypeConverter
                .createXMLGregorianCalendar(System.currentTimeMillis());
		pdelta = PropertyDelta.createModificationReplaceProperty((new ItemPath(ObjectType.F_METADATA,
                MetadataType.F_MODIFY_TIMESTAMP)), accountDefinition, modifyTimestampBefore);
        modifications.add(pdelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        repositoryService.modifyObject(ShadowType.class, oid, modifications, getModifyOptions(), parentResult);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class,
                oid, null, parentResult);
        System.out.println("\nAfter modify 1");
        System.out.println(afterModify.debugDump());

        MetadataType metadataAfter = afterModify.asObjectable().getMetadata();
        assertEquals("Wrong modifyTimestamp", modifyTimestampBefore, metadataAfter.getModifyTimestamp());

        PrismAsserts.assertEqualsPolyString("Wrong shadow name", "1234", afterModify.asObjectable().getName());
        assertAttribute(afterModify, new QName(SchemaConstants.NS_ICF_SCHEMA, "uid"), "8daaeeae-f0c7-41c9-b258-2a3351aa8876");
        assertAttribute(afterModify, "foo", "FOO");
        assertAttribute(afterModify, "bar", "Bar1", "Bar2");
        assertAttribute(afterModify, "baz", "BaZ1", "BaZ2", "BaZ3");

        // GIVEN
        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationUtils.
                createSynchronizationSituationDescriptionDelta(repoShadow, SynchronizationSituationType.LINKED, timestamp, null, false);
        PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationUtils.
                createSynchronizationSituationDelta(repoShadow, SynchronizationSituationType.LINKED);
        syncSituationDeltas.add(syncSituationDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        repositoryService.modifyObject(ShadowType.class, oid, syncSituationDeltas, getModifyOptions(), parentResult);
//        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());

        // THEN
        TestUtil.displayThen(TEST_NAME);
        afterModify = repositoryService.getObject(ShadowType.class,
                oid, null, parentResult);
        System.out.println("\nAfter modify 2");
        System.out.println(afterModify.debugDump());
    }

	@Test
    public void test130ExtensionModify() throws Exception {
    	final String TEST_NAME = "test130ExtensionModify";
    	TestUtil.displayTestTitle(TEST_NAME);

        File userFile = new File(TEST_DIR, "user-with-extension.xml");
        //add first user
        PrismObject<UserType> user = prismContext.parseObject(userFile);

        OperationResult result = new OperationResult("test extension modify");
        final String oid = repositoryService.addObject(user, null, result);

        user = prismContext.parseObject(userFile);
        PrismObject<UserType> readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not saved correctly", user.diff(readUser).isEmpty());
        String lastVersion = readUser.getVersion();

        Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
        ItemPath path = new ItemPath(UserType.F_EXTENSION, QNAME_LOOT);
        PrismProperty loot = user.findProperty(path);
        PropertyDelta lootDelta = new PropertyDelta(path, loot.getDefinition(), prismContext);
        lootDelta.setValueToReplace(new PrismPropertyValue(456));
        modifications.add(lootDelta);

        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        //check read after modify operation
        user = prismContext.parseObject(userFile);
        loot = user.findProperty(new ItemPath(UserType.F_EXTENSION, QNAME_LOOT));
        loot.setValue(new PrismPropertyValue(456));

        readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not modified correctly", user.diff(readUser).isEmpty());

        SqlRepoTestUtil.assertVersionProgress(lastVersion, readUser.getVersion());
    }

    @Test
    public void test140ModifyAccountSynchronizationSituation() throws Exception {
    	final String TEST_NAME = "test140ModifyAccountSynchronizationSituation";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult("testModifyAccountSynchronizationSituation");

        //add account
        File accountFile = new File(TEST_DIR, "account-synchronization-situation.xml");
        PrismObject<ShadowType> account = prismContext.parseObject(accountFile);
        repositoryService.addObject(account, null, result);

//        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationUtils.
                createSynchronizationSituationAndDescriptionDelta(account, SynchronizationSituationType.LINKED, null, false);
//        PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.
//                createSynchronizationSituationDelta(account, SynchronizationSituationType.LINKED);
//        syncSituationDeltas.add(syncSituationDelta);

        repositoryService.modifyObject(ShadowType.class, account.getOid(), syncSituationDeltas, getModifyOptions(), result);

        PrismObject<ShadowType> afterFirstModify = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
        AssertJUnit.assertNotNull(afterFirstModify);
        ShadowType afterFirstModifyType = afterFirstModify.asObjectable();
        assertEquals(1, afterFirstModifyType.getSynchronizationSituationDescription().size());
        SynchronizationSituationDescriptionType description = afterFirstModifyType.getSynchronizationSituationDescription().get(0);
        assertEquals(SynchronizationSituationType.LINKED, description.getSituation());


//        timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        syncSituationDeltas = SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta(afterFirstModify, null, null, false);
//        syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(afterFirstModify, null);
//        syncSituationDeltas.add(syncSituationDelta);

//        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
//        PropertyDelta syncTimestap = SynchronizationSituationUtil.createSynchronizationTimestampDelta(afterFirstModify, timestamp);
//        PropertyDelta.createModificationReplaceProperty(
//                ShadowType.F_SYNCHRONIZATION_TIMESTAMP, afterFirstModify.getDefinition(), timestamp);
//        syncSituationDeltas.add(syncTimestap);

        repositoryService.modifyObject(ShadowType.class, account.getOid(), syncSituationDeltas, getModifyOptions(), result);

        PrismObject<ShadowType> afterSecondModify = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
        AssertJUnit.assertNotNull(afterSecondModify);
        ShadowType afterSecondModifyType = afterSecondModify.asObjectable();
        assertEquals(1, afterSecondModifyType.getSynchronizationSituationDescription().size());
        description = afterSecondModifyType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertNull(description.getSituation());
        XMLGregorianCalendar afterModifytimestamp = afterSecondModifyType.getSynchronizationTimestamp();
        AssertJUnit.assertNotNull(afterModifytimestamp);
        assertEquals(afterSecondModifyType.getSynchronizationTimestamp(), description.getTimestamp());

        ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
                .item(ShadowType.F_SYNCHRONIZATION_TIMESTAMP).le(afterModifytimestamp)
                .build();
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        AssertJUnit.assertNotNull(shadows);
        assertEquals(1, shadows.size());

        System.out.println("shadow: " + shadows.get(0).debugDump());
    }

    private String accountOid;

    @Test
    public void test142ModifyAccountAttributeSameValue() throws Exception {
        final String TEST_NAME = "test142ModifyAccountAttributeSameValue";
        TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult(TEST_NAME);

        PrismObject<ShadowType> account = prismContext.parseObject(new File(TEST_DIR, "account-attribute.xml"));
        repositoryService.addObject(account, null, result);
        accountOid = account.getOid();

        PrismPropertyDefinition<String> definition = new PrismPropertyDefinitionImpl<>(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING, prismContext);

        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
                .item(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), definition)
                .replace("account123")
                .asItemDeltas();

        repositoryService.modifyObject(ShadowType.class, accountOid, itemDeltas, getModifyOptions(), result);

        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        AssertJUnit.assertNotNull(afterModify);
        ShadowType afterFirstModifyType = afterModify.asObjectable();
        System.out.println("shadow: " + afterModify.debugDump());
    }

    @Test
    public void test144ModifyAccountAttributeDifferent() throws Exception {
        final String TEST_NAME = "test144ModifyAccountAttributeDifferent";
        TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult(TEST_NAME);

        assertNotNull("account-attribute was not imported in previous tests", accountOid);

        PrismPropertyDefinition<String> definition = new PrismPropertyDefinitionImpl<>(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING, prismContext);

        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
                .item(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME), definition)
                .replace("account-new")
                .asItemDeltas();

        repositoryService.modifyObject(ShadowType.class, accountOid, itemDeltas, getModifyOptions(), result);

        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        AssertJUnit.assertNotNull(afterModify);
        ShadowType afterFirstModifyType = afterModify.asObjectable();
        System.out.println("shadow: " + afterModify.debugDump());
    }

    @Test
    public void test148ModifyAssignmentExtension() throws Exception {
        final String TEST_NAME = "test148ModifyAssignmentExtension";
        TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult(TEST_NAME);

        PrismObject<UserType> user = prismContext.parseObject(new File(TEST_DIR, "user-with-assignment-extension.xml"));
        repositoryService.addObject(user, null, result);

        PrismPropertyDefinition<String> definition = new PrismPropertyDefinitionImpl<>(QNAME_WEAPON, DOMUtil.XSD_STRING, prismContext);

        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(new ItemPath(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, QNAME_WEAPON), definition)
                .replace("knife")
                .asItemDeltas();

        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltas, getModifyOptions(), result);

        PrismObject<UserType> afterModify = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        AssertJUnit.assertNotNull(afterModify);
        UserType afterFirstModifyType = afterModify.asObjectable();
        System.out.println("user: " + afterModify.debugDump());
    }

    private String roleOid;

    @Test
    public void test150ModifyRoleAddInducements() throws Exception {
    	final String TEST_NAME = "test150ModifyRoleAddInducements";
    	TestUtil.displayTestTitle(TEST_NAME);

        OperationResult result = new OperationResult(TEST_NAME);

        File roleFile = new File(TEST_DIR, "role-modify.xml");
        //add first user
        PrismObject<RoleType> role = prismContext.parseObject(roleFile);
        String oid = repositoryService.addObject(role, null, result);
        roleOid = oid;

        //modify second user name to "existingName"
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "role-modify-change.xml"),
                ObjectModificationType.COMPLEX_TYPE);
        modification.setOid(oid);
        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                RoleType.class, prismContext);

        repositoryService.modifyObject(RoleType.class, oid, deltas, getModifyOptions(), result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        role = repositoryService.getObject(RoleType.class, oid, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer container = role.findContainer(RoleType.F_INDUCEMENT);
        assertEquals(2, container.size());

        AssertJUnit.assertNotNull(container.getValue(2L));
        AssertJUnit.assertNotNull(container.getValue(3L));

		// modify role once more to check version progress
		String version = role.getVersion();
		repositoryService.modifyObject(RoleType.class, oid, new ArrayList<ItemDelta>(), getModifyOptions(), result);
		result.recomputeStatus();
		AssertJUnit.assertTrue(result.isSuccess());
		role = repositoryService.getObject(RoleType.class, oid, null, result);
		result.recomputeStatus();
		AssertJUnit.assertTrue(result.isSuccess());
		if (RepoModifyOptions.isExecuteIfNoChanges(getModifyOptions())) {
			SqlRepoTestUtil.assertVersionProgress(version, role.getVersion());
		} else {
			assertEquals("Version has changed", version, role.getVersion());
		}
	}

    @Test
    public void test160ModifyWithPrecondition() throws Exception {
        final String TEST_NAME = "test160ModifyWithPrecondition";
        TestUtil.displayTestTitle(TEST_NAME);
        OperationResult result = new OperationResult(TEST_NAME);

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = o -> { throw new PreconditionViolationException("hello"); };

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(RoleType.class, prismContext)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, null, result);
            // THEN
            fail("unexpected success");
        } catch (PreconditionViolationException e) {
            assertEquals("Wrong exception message", "hello", e.getMessage());
        }

        String versionAfter = repositoryService.getVersion(RoleType.class, roleOid, result);
        assertEquals("unexpected version change", versionBefore, versionAfter);
    }

    @Test
    public void test162ModifyWithPrecondition2() throws Exception {
        final String TEST_NAME = "test162ModifyWithPrecondition2";
        TestUtil.displayTestTitle(TEST_NAME);
        OperationResult result = new OperationResult(TEST_NAME);

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = o -> false;

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(RoleType.class, prismContext)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, null, result);
            // THEN
            fail("unexpected success");
        } catch (PreconditionViolationException e) {
            // ok
            System.out.println("got expected exception: " + e.getMessage());
        }

        String versionAfter = repositoryService.getVersion(RoleType.class, roleOid, result);
        assertEquals("unexpected version change", versionBefore, versionAfter);
    }

    @Test
    public void test164ModifyWithVersionPreconditionFalse() throws Exception {
        final String TEST_NAME = "test164ModifyWithVersionPreconditionFalse";
        TestUtil.displayTestTitle(TEST_NAME);
        OperationResult result = new OperationResult(TEST_NAME);

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = new VersionPrecondition<>("9999");

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(RoleType.class, prismContext)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, null, result);
            // THEN
            fail("unexpected success");
        } catch (PreconditionViolationException e) {
            // ok
            System.out.println("got expected exception: " + e.getMessage());
        }

        String versionAfter = repositoryService.getVersion(RoleType.class, roleOid, result);
        assertEquals("unexpected version change", versionBefore, versionAfter);
    }

    @Test
    public void test166ModifyWithVersionPreconditionTrue() throws Exception {
        final String TEST_NAME = "test166ModifyWithVersionPreconditionTrue";
        TestUtil.displayTestTitle(TEST_NAME);
        OperationResult result = new OperationResult(TEST_NAME);

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = new VersionPrecondition<>(versionBefore);

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = DeltaBuilder.deltaFor(RoleType.class, prismContext)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, null, result);

        String versionAfter = repositoryService.getVersion(RoleType.class, roleOid, result);
        assertEquals("unexpected version change", Integer.parseInt(versionBefore)+1, Integer.parseInt(versionAfter));
        String description = repositoryService.getObject(RoleType.class, roleOid, null, result).asObjectable().getDescription();
        assertEquals("description was not set", "123456", description);
    }

    private <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
    	assertAttribute(shadow, new QName(MidPointConstants.NS_RI, attrName), expectedValues);
    }

    private <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrQName, T... expectedValues) {
    	PrismProperty<T> attr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attrQName));
    	if (expectedValues.length == 0) {
    		assertTrue("Expected no value for attribute "+attrQName+" in "+shadow+", but it has "+attr, attr == null);
    	} else {
    		assertNotNull("No attribute "+attrQName+" in "+shadow, attr);
    		PrismAsserts.assertPropertyValue(attr, expectedValues);
    	}
	}
}
