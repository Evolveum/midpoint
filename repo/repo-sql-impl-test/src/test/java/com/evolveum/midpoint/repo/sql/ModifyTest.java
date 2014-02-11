/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.repo.sql.data.common.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SynchronizationSituationUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyTest.class);
    private static final File TEST_DIR = new File("src/test/resources/modify");

    @Test(expectedExceptions = SystemException.class, enabled = false)
    public void test010ModifyWithExistingName() throws Exception {
        OperationResult result = new OperationResult("MODIFY");

        File userFile = new File(TEST_DIR, "modify-user.xml");
        //add first user
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "existingName");
        repositoryService.addObject(user, null, result);

        //add second user
        user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.setPropertyRealValue(ObjectType.F_NAME, "otherName");
        String oid = repositoryService.addObject(user, null, result);

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
        OperationResult result = new OperationResult("MODIFY");

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName(new PolyStringType("non-existing-account-user"));

        String oid = repositoryService.addObject(user, null, result);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add-non-existing.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

        PropertyDelta.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        LOGGER.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test(enabled = false)
    public void test031ModifyUserOnExistingAccountTest() throws Exception {
        OperationResult result = new OperationResult("MODIFY");

        //add account
        File accountFile = new File(TEST_DIR, "account.xml");
        PrismObject<ShadowType> account = prismContext.getPrismDomProcessor().parseObject(accountFile);
        repositoryService.addObject(account, null, result);

        //add user
        File userFile = new File(TEST_DIR, "modify-user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);

        String userOid = user.getOid();
        String oid = repositoryService.addObject(user, null, result);
        AssertJUnit.assertEquals(userOid, oid);

        PrismObject<UserType> userOld = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "change-add.xml"),
                ObjectModificationType.class);

        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);

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
        OperationResult result = new OperationResult("MODIFY");
        File taskFile = new File(TEST_DIR, "task.xml");
        System.out.println("ADD");
        PrismObject<TaskType> task = prismContext.getPrismDomProcessor().parseObject(taskFile);
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
        ReferenceDelta delta = new ReferenceDelta(def);
        delta.addValueToAdd(new PrismReferenceValue("1", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);
        System.out.println("GET");
        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("1", objectRef.getOid());
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();

        checkReference(taskOid);
        System.out.println("MODIFY");
        modifications.clear();
        delta = new ReferenceDelta(def);
        delta.addValueToDelete(new PrismReferenceValue("1", null, null));
        delta.addValueToAdd(new PrismReferenceValue("2", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("2", objectRef.getOid());
        LOGGER.info(prismContext.silentMarshalObject(taskType, LOGGER));
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();

        modifications.clear();
        delta = new ReferenceDelta(def);
        delta.addValueToDelete(new PrismReferenceValue("2", null, null));
        delta.addValueToAdd(new PrismReferenceValue("1", null, null));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, result);

        checkReference(taskOid);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        AssertJUnit.assertEquals("1", objectRef.getOid());
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
    public void testModifyUserAddRole() throws Exception {
        OperationResult parentResult = new OperationResult("Modify user -> add roles");
        String userToModifyOid = "f65963e3-9d47-4b18-aaf3-bfc98bdfa000";

        PrismObject<ResourceType> csvResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/resource-csv.xml"));
        repositoryService.addObject(csvResource, null, parentResult);

        PrismObject<ResourceType> openDjResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/resource-opendj.xml"));
        repositoryService.addObject(openDjResource, null, parentResult);

        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/user.xml"));
        repositoryService.addObject(user, null, parentResult);

        PrismObject<RoleType> roleCsv = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/role-csv.xml"));
        repositoryService.addObject(roleCsv, null, parentResult);

        String ldapRoleOid = "12345678-d34d-b33f-f00d-987987987988";
        PrismObject<RoleType> roleLdap = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/role-ldap.xml"));
        repositoryService.addObject(roleLdap, null, parentResult);

        RoleType ldapRole = repositoryService.getObject(RoleType.class, ldapRoleOid, null, parentResult).asObjectable();
        AssertJUnit.assertEquals("Expected that the role has one approver.", 1, ldapRole.getApproverRef().size());
        AssertJUnit.assertEquals("Actual approved not equals to expected one.", userToModifyOid, ldapRole.getApproverRef().get(0).getOid());

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(TEST_DIR + "/modify-user-add-roles.xml"),
                ObjectModificationType.class);


        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);


        repositoryService.modifyObject(UserType.class, userToModifyOid, delta.getModifications(), parentResult);

        UserType modifiedUser = repositoryService.getObject(UserType.class, userToModifyOid, null, parentResult).asObjectable();
        AssertJUnit.assertEquals("assertion failed", 3, modifiedUser.getAssignment().size());

    }

    @Test
    public void testModifyDeleteObjectChangeFromAccount() throws Exception {
        OperationResult parentResult = new OperationResult("testModifyDeleteObjectChnageFromAccount");
        PrismObject<ShadowType> accShadow = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/account-delete-object-change.xml"));
        String oid = repositoryService.addObject(accShadow, null, parentResult);
        System.out.println("\nAcc shadow");
        System.out.println(accShadow.debugDump());

        accShadow.asObjectable().setObjectChange(null);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.debugDump());

        ObjectDelta d = repoShadow.diff(accShadow);
        System.out.println("\nDelta");
        System.out.println(d.debugDump());

        repositoryService.modifyObject(ShadowType.class, oid, d.getModifications(), parentResult);

        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());
    }

    @Test(enabled = false)
    public void testModifyAccountMetadata() throws Exception {
        OperationResult parentResult = new OperationResult("testModifyAccountMetadata");

        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/user-modify-link-account.xml"));


        PrismObject<ShadowType> accShadow = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR + "/account-modify-metadata.xml"));

        MetadataType metaData = new MetadataType();
        metaData.setCreateChannel("channel");
        metaData.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        accShadow.asObjectable().setMetadata(metaData);

        System.out.println("\nAcc shadow");
        System.out.println(accShadow.debugDump());

        String oid = repositoryService.addObject(accShadow, null, parentResult);
        System.out.println("\nAcc shadow");
        System.out.println(accShadow.debugDump());

        accShadow.asObjectable().setObjectChange(null);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.debugDump());

        ObjectDelta d = repoShadow.diff(accShadow);
        System.out.println("\nDelta");
        System.out.println(d.debugDump());

        PrismObjectDefinition accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismReferenceValue accountRef = new PrismReferenceValue();
        accountRef.setOid(oid);
        accountRef.setTargetType(ShadowType.COMPLEX_TYPE);

        Collection<? extends ItemDelta> accountRefDeltas = ReferenceDelta.createModificationAddCollection(
                UserType.F_LINK_REF, user.getDefinition(), accountRef);

        repositoryService.modifyObject(ShadowType.class, oid, accountRefDeltas, parentResult);

        PrismObject<ShadowType> afterModify = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        System.out.println("\nAfter modify");
        System.out.println(afterModify.debugDump());

        Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
        PropertyDelta pdelta = PropertyDelta.createModificationReplaceProperty((new ItemPath(ObjectType.F_METADATA, MetadataType.F_MODIFY_CHANNEL)), accountDefinition, "channel");
        modifications.add(pdelta);

        pdelta = PropertyDelta.createModificationReplaceProperty((new ItemPath(ObjectType.F_METADATA,
                MetadataType.F_MODIFY_TIMESTAMP)), accountDefinition, XmlTypeConverter
                .createXMLGregorianCalendar(System.currentTimeMillis()));
        modifications.add(pdelta);

        repositoryService.modifyObject(ShadowType.class, oid, modifications, parentResult);

        afterModify = repositoryService.getObject(ShadowType.class,
                oid, null, parentResult);
        System.out.println("\nAfter modify");
        System.out.println(afterModify.debugDump());


        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.
                createSynchronizationSituationDescriptionDelta(repoShadow, SynchronizationSituationType.LINKED, timestamp, null, false);
        PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.
                createSynchronizationSituationDelta(repoShadow, SynchronizationSituationType.LINKED);
        syncSituationDeltas.add(syncSituationDelta);

        repositoryService.modifyObject(ShadowType.class, oid, syncSituationDeltas, parentResult);
//        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());
        afterModify = repositoryService.getObject(ShadowType.class,
                oid, null, parentResult);
        System.out.println("\nAfter modify");
        System.out.println(afterModify.debugDump());
    }

    @Test
    public void testExtensionModify() throws Exception {
        final QName QNAME_LOOT = new QName("http://example.com/p", "loot");

        File userFile = new File(TEST_DIR, "user-with-extension.xml");
        //add first user
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(userFile);

        OperationResult result = new OperationResult("test extension modify");
        final String oid = repositoryService.addObject(user, null, result);

        user = prismContext.getPrismDomProcessor().parseObject(userFile);
        PrismObject<UserType> readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not saved correctly", user.diff(readUser).isEmpty());
        String lastVersion = readUser.getVersion();

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

        readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not modified correctly", user.diff(readUser).isEmpty());

        SqlRepoTestUtil.assertVersionProgress(lastVersion, readUser.getVersion());
    }

    @Test
    public void simpleModifyExtensionDateTest() throws Exception {
        final Timestamp DATE = new Timestamp(new Date().getTime());
        RUser user = createUser(123L, DATE);

        Session session = null;
        try {
            LOGGER.info(">>>SAVE");
            session = getFactory().openSession();
            session.beginTransaction();
            RContainerId id = (RContainerId) session.save(user);
            session.getTransaction().commit();
            session.close();

            LOGGER.info(">>>MERGE");
            session = getFactory().openSession();
            session.beginTransaction();
            user = createUser(456L, DATE);

            user.setId(0L);
            user.setOid(id.getOid());
            session.merge(user);
            session.getTransaction().commit();
            session.close();

            LOGGER.info(">>>GET");
            session = getFactory().openSession();
            session.beginTransaction();
            user = (RUser) session.createQuery("from RUser as u where u.oid = :oid").setParameter("oid", id.getOid()).uniqueResult();

            RAnyContainer extension = user.getExtension();
            AssertJUnit.assertEquals(1, extension.getClobs().size());
            AssertJUnit.assertEquals(1, extension.getDates().size());
            AssertJUnit.assertEquals(2, extension.getStrings().size());
            AssertJUnit.assertEquals(1, extension.getLongs().size());
            hasLong(extension.getLongs(), 456L);

            session.getTransaction().commit();
            session.close();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    private void hasLong(Set<RAnyLong> longs, Long value) {
        for (RAnyLong any : longs) {
            Long other = any.getValue();

            if (other == null) {
                if (value == null) {
                    return;
                }
            } else {
                if (other.equals(value)) {
                    return;
                }
            }
        }
        AssertJUnit.fail("Longs doesn't contain value '" + value + "'");
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

        Set<RAnyDate> dates = new HashSet<RAnyDate>();
        any.setDates(dates);

        final String namespace = "http://example.com/p";

        RAnyDate date = new RAnyDate();
        date.setAnyContainer(any);
        date.setDynamic(false);
        date.setName(new QName(namespace, "funeralDate"));
        date.setType(new QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        date.setValue(dateValue);
        dates.add(date);
        date.setValueType(RValueType.PROPERTY);

        Set<RAnyLong> longs = new HashSet<RAnyLong>();
        any.setLongs(longs);

        RAnyLong l = new RAnyLong();
        l.setAnyContainer(any);
        longs.add(l);
        l.setDynamic(false);
        l.setName(new QName(namespace, "loot"));
        l.setType(new QName("http://www.w3.org/2001/XMLSchema", "int"));
        l.setValue(lootValue);
        l.setValueType(RValueType.PROPERTY);

        Set<RAnyString> strings = new HashSet<RAnyString>();
        any.setStrings(strings);

        RAnyString s1 = new RAnyString();
        s1.setAnyContainer(any);
        strings.add(s1);
        s1.setDynamic(false);
        s1.setName(new QName(namespace, "weapon"));
        s1.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        s1.setValue("gun");
        s1.setValueType(RValueType.PROPERTY);

        RAnyString s2 = new RAnyString();
        s2.setAnyContainer(any);
        strings.add(s2);
        s2.setDynamic(false);
        s2.setName(new QName(namespace, "shipName"));
        s2.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        s2.setValue("pltka");
        s2.setValueType(RValueType.PROPERTY);

        Set<RAnyClob> clobs = new HashSet<RAnyClob>();
        any.setClobs(clobs);

        RAnyClob c1 = new RAnyClob();
        clobs.add(c1);
        c1.setAnyContainer(any);
        c1.setDynamic(false);
        c1.setName(new QName(namespace, "someContainer"));
        c1.setType(new QName("http://www.w3.org/2001/XMLSchema", "string"));
        c1.setValue("some container xml as clob or what...");
        c1.setValueType(RValueType.CONTAINER);

        return user;
    }

    @Test
    public void testModifyAccountSynchronizationSituation() throws Exception {
        OperationResult result = new OperationResult("testModifyAccountSynchronizationSituation");

        //add account
        File accountFile = new File(TEST_DIR, "account-synchronization-situation.xml");
        PrismObject<ShadowType> account = prismContext.getPrismDomProcessor().parseObject(accountFile);
        repositoryService.addObject(account, null, result);

//        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.
                createSynchronizationSituationAndDescriptionDelta(account, SynchronizationSituationType.LINKED, null, false);
//        PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.
//                createSynchronizationSituationDelta(account, SynchronizationSituationType.LINKED);
//        syncSituationDeltas.add(syncSituationDelta);

        repositoryService.modifyObject(ShadowType.class, account.getOid(), syncSituationDeltas, result);

        PrismObject<ShadowType> afterFirstModify = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
        AssertJUnit.assertNotNull(afterFirstModify);
        ShadowType afterFirstModifyType = afterFirstModify.asObjectable();
        AssertJUnit.assertEquals(1, afterFirstModifyType.getSynchronizationSituationDescription().size());
        SynchronizationSituationDescriptionType description = afterFirstModifyType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, description.getSituation());


//        timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        syncSituationDeltas = SynchronizationSituationUtil.createSynchronizationSituationAndDescriptionDelta(afterFirstModify, null, null, false);
//        syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(afterFirstModify, null);
//        syncSituationDeltas.add(syncSituationDelta);

//        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
//        PropertyDelta syncTimestap = SynchronizationSituationUtil.createSynchronizationTimestampDelta(afterFirstModify, timestamp);
//        PropertyDelta.createModificationReplaceProperty(
//                ShadowType.F_SYNCHRONIZATION_TIMESTAMP, afterFirstModify.getDefinition(), timestamp);
//        syncSituationDeltas.add(syncTimestap);

        repositoryService.modifyObject(ShadowType.class, account.getOid(), syncSituationDeltas, result);

        PrismObject<ShadowType> afterSecondModify = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
        AssertJUnit.assertNotNull(afterSecondModify);
        ShadowType afterSecondModifyType = afterSecondModify.asObjectable();
        AssertJUnit.assertEquals(1, afterSecondModifyType.getSynchronizationSituationDescription().size());
        description = afterSecondModifyType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertNull(description.getSituation());
        XMLGregorianCalendar afterModifytimestamp = afterSecondModifyType.getSynchronizationTimestamp();
        AssertJUnit.assertNotNull(afterModifytimestamp);
        AssertJUnit.assertEquals(afterSecondModifyType.getSynchronizationTimestamp(), description.getTimestamp());

        LessFilter filter = LessFilter.createLess(ShadowType.F_SYNCHRONIZATION_TIMESTAMP, afterSecondModify.findProperty(
                ShadowType.F_SYNCHRONIZATION_TIMESTAMP).getDefinition(), afterModifytimestamp, true);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        AssertJUnit.assertNotNull(shadows);
        AssertJUnit.assertEquals(1, shadows.size());

        System.out.println("shadow: " + shadows.get(0).debugDump());
    }

    @Test
    public void testModifyAccountSynchronizationSituationSimplyfied() {
        //add
        RShadow s1 = new RShadow();
        s1.setName(new RPolyString("acc", "acc"));

        LOGGER.info("add:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        Session session = getFactory().openSession();
        final RContainerId ID;
        try {
            session.beginTransaction();
            ID = (RContainerId) session.save(s1);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //modify1
        s1 = new RShadow();
        s1.setId(0L);
        s1.setOid(ID.getOid());
        s1.setName(new RPolyString("acc", "acc"));
        RSynchronizationSituationDescription desc = new RSynchronizationSituationDescription();
        desc.setShadow(s1);
        desc.setSituation(RSynchronizationSituation.LINKED);
        XMLGregorianCalendar date1 = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        LOGGER.info("Date is: {}, {} {}", new Object[]{date1, date1, date1.getClass()});
        desc.setTimestampValue(date1);
        s1.getSynchronizationSituationDescription().add(desc);

        LOGGER.info("modify1:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        session = getFactory().openSession();
        try {
            session.beginTransaction();
            session.merge(s1);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //get1
        session = getFactory().openSession();
        try {
            session.beginTransaction();
            RShadow shadow = (RShadow) session.get(RShadow.class, ID);
            LOGGER.info("get1:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(shadow, ToStringStyle.MULTI_LINE_STYLE)});
            AssertJUnit.assertEquals(1, shadow.getSynchronizationSituationDescription().size());

            Iterator<RSynchronizationSituationDescription> i = shadow.getSynchronizationSituationDescription().iterator();
            Date t;
            while (i.hasNext()) {
                t = XMLGregorianCalendarType.asDate(i.next().getTimestampValue());
                LOGGER.info("Date from result: {}, {}", new Object[]{t, t.getTime()});
            }
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //modify2
        s1 = new RShadow();
        s1.setId(0L);
        s1.setOid(ID.getOid());
        s1.setName(new RPolyString("acc", "acc"));
        desc = new RSynchronizationSituationDescription();
        desc.setShadow(s1);
        desc.setSituation(RSynchronizationSituation.LINKED);
        XMLGregorianCalendar date2 = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        LOGGER.info("Date is: {}, {} {}", new Object[]{date2, date2, date2.getClass()});
        desc.setTimestampValue(date2);
        s1.getSynchronizationSituationDescription().add(desc);
        s1.setSynchronizationTimestamp(date2);

        LOGGER.info("modify2:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        session = getFactory().openSession();
        try {
            session.beginTransaction();
            session.merge(s1);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

        //get2
        session = getFactory().openSession();
        try {
            session.beginTransaction();
            RShadow shadow = (RShadow) session.get(RShadow.class, ID);
            LOGGER.info("get2:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(shadow, ToStringStyle.MULTI_LINE_STYLE)});

            Date t;
            Iterator<RSynchronizationSituationDescription> i = shadow.getSynchronizationSituationDescription().iterator();
            while (i.hasNext()) {
                t = XMLGregorianCalendarType.asDate(i.next().getTimestampValue());
                LOGGER.info("Date from result: {}, {}", new Object[]{t, t.getTime()});
            }

            AssertJUnit.assertEquals(1, shadow.getSynchronizationSituationDescription().size());
            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    @Test
    public void modifyRoleAddInducements() throws Exception {
        OperationResult result = new OperationResult("MODIFY");

        File roleFile = new File(TEST_DIR, "role-modify.xml");
        //add first user
        PrismObject<RoleType> role = prismContext.getPrismDomProcessor().parseObject(roleFile);
        String oid = repositoryService.addObject(role, null, result);

        //modify second user name to "existingName"
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "role-modify-change.xml"),
                ObjectModificationType.class);
        modification.setOid(oid);
        Collection<? extends ItemDelta> deltas = DeltaConvertor.toModifications(modification,
                RoleType.class, prismContext);

        repositoryService.modifyObject(RoleType.class, oid, deltas, result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        role = repositoryService.getObject(RoleType.class, oid, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer container = role.findContainer(RoleType.F_INDUCEMENT);
        AssertJUnit.assertEquals(2, container.size());

        AssertJUnit.assertNotNull(container.getValue(2L));
        AssertJUnit.assertNotNull(container.getValue(3L));
    }
}
