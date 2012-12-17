/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../ctx-sql-server-mode-test.xml",
        "../../../../../ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "../../../../../ctx-configuration-sql-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyTest.class);
    private static final File TEST_DIR = new File("src/test/resources/modify");

    @Test(expectedExceptions = SystemException.class, enabled = false)
    public void test010ModifyWithExistingName() throws Exception {
        System.out.println("====[ test010ModifyWithExistingName ]====");
        LOGGER.info("=== [ test010ModifyWithExistingName ] ===");

        OperationResult result = new OperationResult("MODIFY");

        File userFile = new File(TEST_DIR, "modify-user.xml");
        //add first user
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "existingName");
        repositoryService.addObject(user, result);

        //add second user
        user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "otherName");
        String oid = repositoryService.addObject(user, result);

        //modify second user name to "existingName"
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-name.xml"),
                ObjectModificationType.class);
        modification.setOid(oid);
        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);
    }

    @Test(expectedExceptions = ObjectNotFoundException.class, enabled = false)
    public void test020ModifyNotExistingUser() throws Exception {
        System.out.println("====[ test020ModifyNotExistingUser ]====");
        LOGGER.info("=== [ test020ModifyNotExistingUser ] ===");

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        OperationResult result = new OperationResult("MODIFY");
        repositoryService.modifyObject(UserType.class, "1234", deltas, result);
    }

    @Test(enabled = false)
    public void test030ModifyUserOnNonExistingAccountTest() throws Exception {
        System.out.println("====[ test030ModifyUserOnNonExistingAccountTest ]====");
        LOGGER.info("=== [ test030ModifyUserOnNonExistingAccountTest ] ===");

        OperationResult result = new OperationResult("MODIFY");

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName(new PolyStringType("non-existing-account-user"));

        String oid = repositoryService.addObject(user, result);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add-non-existing.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test(enabled = false)
    public void test031ModifyUserOnExistingAccountTest() throws Exception {
        System.out.println("====[ test031ModifyUserOnExistingAccountTest ]====");
        LOGGER.info("=== [ test031ModifyUserOnExistingAccountTest ] ===");

        OperationResult result = new OperationResult("MODIFY");

        //add account
        File accountFile = new File(TEST_DIR, "account.xml");
        PrismObject<AccountShadowType> account = prismContext.getPrismDomProcessor().parseObject(accountFile);
        repositoryService.addObject(account, result);

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);

        String userOid = user.getOid();
        String oid = repositoryService.addObject(user, result);
        AssertJUnit.assertEquals(userOid, oid);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test
    public void test032ModifyTaskObjectRef() throws Exception {
        System.out.println("====[ test032ModifyTaskObjectRef ]====");
        LOGGER.info("=== [ test032ModifyTaskObjectRef ] ===");

        ClassMetadata metadata = factory.getClassMetadata(RTask.class);

        OperationResult result = new OperationResult("MODIFY");
        File taskFile = new File(TEST_DIR, "task.xml");
        System.out.println("ADD");
        PrismObject<TaskType> task = prismContext.getPrismDomProcessor().parseObject(taskFile);
        repositoryService.addObject(task, result);
        final String taskOid = "00000000-0000-0000-0000-123450000001";
        AssertJUnit.assertNotNull(taskOid);
        System.out.println("GET");
        PrismObject<TaskType> getTask = null;
        getTask = repositoryService.getObject(TaskType.class, taskOid, result);
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
        ReferenceDelta delta = new ReferenceDelta(def);
        delta.addValueToAdd(new PrismReferenceValue("1", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);
        System.out.println("GET");
        getTask = repositoryService.getObject(TaskType.class, taskOid, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("1", objectRef.getOid());

        checkReference(taskOid);
        System.out.println("MODIFY");
        modifications.clear();
        delta = new ReferenceDelta(def);
        delta.addValueToDelete(new PrismReferenceValue("1", null, null));
        delta.addValueToAdd(new PrismReferenceValue("2", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("2", objectRef.getOid());
        LOGGER.info(prismContext.silentMarshalObject(taskType, LOGGER));

        modifications.clear();
        delta = new ReferenceDelta(def);
        delta.addValueToDelete(new PrismReferenceValue("2", null, null));
        delta.addValueToAdd(new PrismReferenceValue("1", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("1", objectRef.getOid());
    }

    private void checkReference(String taskOid) {
//        Session session = factory.openSession();
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
    public void testModifyUserAddRole() throws Exception {
        System.out.println("====[ testModifyUserAddRole ]====");
        LOGGER.info("=== [ testModifyUserAddRole ] ===");
        OperationResult parentResult = new OperationResult("Modify user -> add roles");
        String userToModifyOid = "f65963e3-9d47-4b18-aaf3-bfc98bdfa000";

        PrismObject<ResourceType> csvResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/resource-csv.xml"));
        repositoryService.addObject(csvResource, parentResult);

        PrismObject<ResourceType> openDjResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/resource-opendj.xml"));
        repositoryService.addObject(openDjResource, parentResult);

        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/user.xml"));
        repositoryService.addObject(user, parentResult);

        PrismObject<RoleType> roleCsv = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/role-csv.xml"));
        repositoryService.addObject(roleCsv, parentResult);

        String ldapRoleOid = "12345678-d34d-b33f-f00d-987987987988";
        PrismObject<RoleType> roleLdap = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/role-ldap.xml"));
        repositoryService.addObject(roleLdap, parentResult);

        RoleType ldapRole = repositoryService.getObject(RoleType.class, ldapRoleOid, parentResult).asObjectable();
        AssertJUnit.assertEquals("Expected that the role has one approver.", 1, ldapRole.getApproverRef().size());
        AssertJUnit.assertEquals("Actual approved not equals to expected one.", userToModifyOid, ldapRole.getApproverRef().get(0).getOid());

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(TEST_DIR + "/modify-user-add-roles.xml"),
                ObjectModificationType.class);


        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);


        repositoryService.modifyObject(UserType.class, userToModifyOid, delta.getModifications(), parentResult);

        UserType modifiedUser = repositoryService.getObject(UserType.class, userToModifyOid, parentResult).asObjectable();
        AssertJUnit.assertEquals("assertion failed", 3, modifiedUser.getAssignment().size());

    }

    @Test
    public void testModifyDeleteObjectChnageFromAccount() throws Exception {
        System.out.println("====[ testModifyDeleteObjectChnageFromAccount ]====");
        OperationResult parentResult = new OperationResult("testModifyDeleteObjectChnageFromAccount");
        PrismObject<AccountShadowType> accShadow = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/account-delete-object-change.xml"));
        String oid = repositoryService.addObject(accShadow, parentResult);
        System.out.println("\nAcc shadow");
        System.out.println(accShadow.dump());

        accShadow.asObjectable().setObjectChange(null);

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, oid, parentResult);
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.dump());

        ObjectDelta d = repoShadow.diff(accShadow);
        System.out.println("\nDelta");
        System.out.println(d.dump());

        repositoryService.modifyObject(AccountShadowType.class, oid, d.getModifications(), parentResult);

        PrismObject<AccountShadowType> afterModify = repositoryService.getObject(AccountShadowType.class, oid, parentResult);
        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());
    }

    @Test
    public void testExtensionModify() throws Exception {
        System.out.println("====[ testExtensionModify ]====");
        LOGGER.info("====[ testExtensionModify ]====");

        final QName QNAME_LOOT = new QName("http://example.com/p", "loot");

        File userFile = new File(TEST_DIR, "user-with-extension.xml");
        //add first user
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);

        OperationResult result = new OperationResult("test extension modify");
        final String oid = repositoryService.addObject(user, result);

        user = prismContext.getPrismDomProcessor().parseObject(userFile);
        PrismObject<UserType> readUser = repositoryService.getObject(UserType.class, oid, result);
        AssertJUnit.assertTrue("User was not saved correctly", user.diff(readUser).isEmpty());

        Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
        ItemPath path = new ItemPath(UserType.F_EXTENSION, QNAME_LOOT);
        PrismProperty loot = user.findProperty(path);
        PropertyDelta lootDelta = new PropertyDelta(path, loot.getDefinition());
        lootDelta.setValueToReplace(new PrismPropertyValue(456));
        modifications.add(lootDelta);

        repositoryService.modifyObject(UserType.class, oid, modifications, result);

        //check read after modify operation
        user = prismContext.getPrismDomProcessor().parseObject(userFile);
        loot = user.findProperty(new ItemPath(UserType.F_EXTENSION, QNAME_LOOT));
        loot.setValue(new PrismPropertyValue(456));

        readUser = repositoryService.getObject(UserType.class, oid, result);
        AssertJUnit.assertTrue("User was not modified correctly", user.diff(readUser).isEmpty());
    }

    @Test
    public void simpleModifyExtensionDateTest() throws Exception {
        final Timestamp DATE = new Timestamp(new Date().getTime());
        RUser user = createUser(123L, DATE);

        Session session = null;
        try {
            LOGGER.info(">>>SAVE");
            session = factory.openSession();
            session.beginTransaction();
            RContainerId id = (RContainerId) session.save(user);
            session.getTransaction().commit();
            session.close();

            LOGGER.info(">>>MERGE");
            session = factory.openSession();
            session.beginTransaction();
            user = createUser(456L, DATE);
            user.setId(0L);
            user.setOid(id.getOid());
            session.merge(user);
            session.getTransaction().commit();
            session.close();

            LOGGER.info(">>>GET");
            session = factory.openSession();
            session.beginTransaction();
            user = (RUser) session.createQuery("from RUser as u where u.oid = :oid").setParameter("oid", id.getOid()).uniqueResult();

            RAnyContainer extension = user.getExtension();
            AssertJUnit.assertEquals(1, extension.getClobs().size());
            AssertJUnit.assertEquals(1, extension.getDates().size());
            AssertJUnit.assertEquals(2, extension.getStrings().size());
            AssertJUnit.assertEquals(1, extension.getLongs().size());

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    private RUser createUser(Long lootValue, Timestamp dateValue) {
        RUser user = new RUser();
        user.setName(new RPolyString("u1", "u1"));
        user.setFullName(new RPolyString("fu1", "fu1"));
        user.setFamilyName(new RPolyString("fa1", "fa1"));
        user.setGivenName(new RPolyString("gi1", "gi1"));

        RAnyContainer any = new RAnyContainer();
        any.setOwnerType(RContainerType.USER);
        any.setOwner(user);
        user.setExtension(any);

        Set<RDateValue> dates = new HashSet<RDateValue>();
        any.setDates(dates);

        final String namespace = "http://example.com/p";

        RDateValue date = new RDateValue();
        date.setDynamic(false);
        date.setName(new QName(namespace, "funeralDate"));
        date.setType(new QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        date.setValue(dateValue);
        dates.add(date);
        date.setValueType(RValueType.PROPERTY);

        Set<RLongValue> longs = new HashSet<RLongValue>();
        any.setLongs(longs);

        RLongValue l = new RLongValue();
        longs.add(l);
        l.setDynamic(false);
        l.setName(new QName(namespace, "loot"));
        l.setType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        l.setValue(lootValue);
        l.setValueType(RValueType.PROPERTY);

        Set<RStringValue> strings = new HashSet<RStringValue>();
        any.setStrings(strings);

        RStringValue s1 = new RStringValue();
        strings.add(s1);
        s1.setDynamic(false);
        s1.setName(new QName(namespace, "weapon"));
        s1.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        s1.setValue("gun");
        s1.setValueType(RValueType.PROPERTY);

        RStringValue s2 = new RStringValue();
        strings.add(s2);
        s2.setDynamic(false);
        s2.setName(new QName(namespace, "shipName"));
        s2.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        s2.setValue("pltka");
        s2.setValueType(RValueType.PROPERTY);

        Set<RClobValue> clobs = new HashSet<RClobValue>();
        any.setClobs(clobs);

        RClobValue c1 = new RClobValue();
        clobs.add(c1);
        c1.setDynamic(false);
        c1.setName(new QName(namespace, "someContainer"));
        c1.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        c1.setValue("some container xml as clob or what...");
        c1.setValueType(RValueType.CONTAINER);

        return user;
    }
}
