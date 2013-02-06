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
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.sql.data.common.RAccountShadow;
import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import org.hibernate.SQLQuery;
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
    public void testModifyDeleteObjectChangeFromAccount() throws Exception {
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
        PrismObject<AccountShadowType> account = prismContext.getPrismDomProcessor().parseObject(accountFile);
        repositoryService.addObject(account, result);

        List<PropertyDelta<?>> syncSituationDeltas = SynchronizationSituationUtil.
                createSynchronizationSituationDescriptionDelta(account, SynchronizationSituationType.LINKED, null);
        PropertyDelta<SynchronizationSituationType> syncSituationDelta = SynchronizationSituationUtil.
                createSynchronizationSituationDelta(account, SynchronizationSituationType.LINKED);
        syncSituationDeltas.add(syncSituationDelta);

        repositoryService.modifyObject(AccountShadowType.class, account.getOid(), syncSituationDeltas, result);

        PrismObject<AccountShadowType> afterFirstModify = repositoryService.getObject(AccountShadowType.class, account.getOid(), result);
        AssertJUnit.assertNotNull(afterFirstModify);
        AccountShadowType afterFirstModifyType = afterFirstModify.asObjectable();
        AssertJUnit.assertEquals(1, afterFirstModifyType.getSynchronizationSituationDescription().size());
        SynchronizationSituationDescriptionType description = afterFirstModifyType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, description.getSituation());


        syncSituationDeltas = SynchronizationSituationUtil.createSynchronizationSituationDescriptionDelta(afterFirstModify, null, null);
        syncSituationDelta = SynchronizationSituationUtil.createSynchronizationSituationDelta(afterFirstModify, null);
        syncSituationDeltas.add(syncSituationDelta);

        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        PropertyDelta syncTimestap = PropertyDelta.createModificationReplaceProperty(
                AccountShadowType.F_SYNCHRONIZATION_TIMESTAMP, afterFirstModify.getDefinition(), timestamp);
        syncSituationDeltas.add(syncTimestap);

        repositoryService.modifyObject(AccountShadowType.class, account.getOid(), syncSituationDeltas, result);

        PrismObject<AccountShadowType> afterSecondModify = repositoryService.getObject(AccountShadowType.class, account.getOid(), result);
        AssertJUnit.assertNotNull(afterSecondModify);
        AccountShadowType afterSecondModifyType = afterSecondModify.asObjectable();
        AssertJUnit.assertEquals(1, afterSecondModifyType.getSynchronizationSituationDescription().size());
        description = afterSecondModifyType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertEquals(null, description.getSituation());

        LessFilter filter = LessFilter.createLessFilter(null, afterSecondModify.findItem(
                AccountShadowType.F_SYNCHRONIZATION_TIMESTAMP).getDefinition(), timestamp, true);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<PrismObject<AccountShadowType>> shadows = repositoryService.searchObjects(AccountShadowType.class, query, result);
        AssertJUnit.assertNotNull(shadows);
        AssertJUnit.assertEquals(1, shadows.size());

        System.out.println("shadow: " + shadows.get(0).dump());
    }

    @Test
    public void testModifyAccountSynchronizationSituationSimplyfied() {
        //add
        RAccountShadow s1 = new RAccountShadow();
        s1.setName(new RPolyString("acc", "acc"));

        LOGGER.info("add:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        Session session = getFactory().openSession();
        session.beginTransaction();
        final RContainerId ID = (RContainerId) session.save(s1);
        session.getTransaction().commit();
        session.close();

        //modify1
        s1 = new RAccountShadow();
        s1.setId(0L);
        s1.setOid(ID.getOid());
        s1.setName(new RPolyString("acc", "acc"));
        RSynchronizationSituationDescription desc = new RSynchronizationSituationDescription();
        desc.setSituation(RSynchronizationSituation.LINKED);
        XMLGregorianCalendar date1 = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        LOGGER.info("Date is: {}, {} {}", new Object[]{date1, date1, date1.getClass()});
        desc.setTimestampValue(date1);
        s1.getSynchronizationSituationDescription().add(desc);

        LOGGER.info("modify1:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        session = getFactory().openSession();
        session.beginTransaction();
        session.merge(s1);
        session.getTransaction().commit();
        session.close();

        //get1
        session = getFactory().openSession();
        session.beginTransaction();
        RAccountShadow shadow = (RAccountShadow) session.get(RAccountShadow.class, ID);
        LOGGER.info("get1:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(shadow, ToStringStyle.MULTI_LINE_STYLE)});
        AssertJUnit.assertEquals(1, shadow.getSynchronizationSituationDescription().size());

        Iterator<RSynchronizationSituationDescription> i = shadow.getSynchronizationSituationDescription().iterator();
        Date t;
        while (i.hasNext()) {
            t = XMLGregorianCalendarType.asDate(i.next().getTimestampValue());
            LOGGER.info("Date from result: {}, {}", new Object[]{t, t.getTime()});
        }
        session.getTransaction().commit();
        session.close();

        //modify2
        s1 = new RAccountShadow();
        s1.setId(0L);
        s1.setOid(ID.getOid());
        s1.setName(new RPolyString("acc", "acc"));
        desc = new RSynchronizationSituationDescription();
        desc.setSituation(RSynchronizationSituation.LINKED);
        XMLGregorianCalendar date2 = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        LOGGER.info("Date is: {}, {} {}", new Object[]{date2, date2, date2.getClass()});
        desc.setTimestampValue(date2);
        s1.getSynchronizationSituationDescription().add(desc);
        s1.setSynchronizationTimestamp(date2);

        LOGGER.info("modify2:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(s1, ToStringStyle.MULTI_LINE_STYLE)});
        session = getFactory().openSession();
        session.beginTransaction();
        session.merge(s1);
        session.getTransaction().commit();
        session.close();

        //get2
        session = getFactory().openSession();
        session.beginTransaction();
        shadow = (RAccountShadow) session.get(RAccountShadow.class, ID);
        LOGGER.info("get2:\n{}", new Object[]{ReflectionToStringBuilder.reflectionToString(shadow, ToStringStyle.MULTI_LINE_STYLE)});

        i = shadow.getSynchronizationSituationDescription().iterator();
        while (i.hasNext()) {
            t = XMLGregorianCalendarType.asDate(i.next().getTimestampValue());
            LOGGER.info("Date from result: {}, {}", new Object[]{t, t.getTime()});
        }

        AssertJUnit.assertEquals(1, shadow.getSynchronizationSituationDescription().size());
        session.getTransaction().commit();
        session.close();
    }
}
