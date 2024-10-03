/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.displayCollection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.SerializationOptions.createSerializeForExport;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRawCollection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.util.TestUtil;

import jakarta.persistence.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.VersionPrecondition;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/modify");
    private static final File ACCOUNT_ATTRIBUTE_FILE = new File(TEST_DIR, "account-attribute.xml");
    private static final File ACCOUNT_FILE = new File(TEST_DIR, "account.xml");
    private static final File MODIFY_USER_ADD_LINK = new File(TEST_DIR, "change-add.xml");

    private static final QName QNAME_LOOT = new QName("http://example.com/p", "loot");
    private static final QName QNAME_WEAPON = new QName("http://example.com/p", "weapon");
    private static final QName QNAME_FUNERAL_DATE = new QName("http://example.com/p", "funeralDate");

    private static final File SYSTEM_CONFIGURATION_BEFORE_FILE = new File(TEST_DIR, "system-configuration-before.xml");
    private static final File SYSTEM_CONFIGURATION_AFTER_FILE = new File(TEST_DIR, "system-configuration-after.xml");

    private static final File USER_ADAM_FILE = new File(TEST_DIR, "user-adam.xml");
    private static final File USER_ADAM_NEW_ASSIGNMENT_NO_ID_FILE = new File(TEST_DIR, "user-adam-new-assignment-no-id.xml");

    private PrismObject<ObjectType> userAdam;

    @Override
    public void initSystem() throws Exception {
        super.initSystem();

        OperationResult result = new OperationResult("initSystem");

        // This is an experimental feature, so it needs to be explicitly enabled. This will be eliminated later,
        // when we make it enabled by default.
        sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);
        InternalsConfig.encryptionChecks = false;

        userAdam = prismContext.parseObject(USER_ADAM_FILE);
        repositoryService.addObject(userAdam, null, result);
    }

    protected RepoModifyOptions getModifyOptions() {
        return null;
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test010ModifyWithExistingName() throws Exception {
        OperationResult result = new OperationResult("MODIFY");

        File userFile = new File(TEST_DIR, "modify-user.xml");
        //add first user
        PrismObject<UserType> user = prismContext.parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName(PolyStringType.fromOrig("existingName"));
        repositoryService.addObject(user, null, result);

        //add second user
        user = prismContext.parseObject(userFile);
        user.setOid(null);
        user.asObjectable().setName(PolyStringType.fromOrig("otherName"));
        String oid = repositoryService.addObject(user, null, result);

        //modify second user name to "existingName"
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "change-name.xml"),
                ObjectModificationType.COMPLEX_TYPE);
        modification.setOid(oid);
        Collection<? extends ItemDelta<?, ?>> deltas =
                DeltaConvertor.toModifications(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, result);
    }

    @Test(expectedExceptions = ObjectNotFoundException.class)
    public void test020ModifyNotExistingUser() throws Exception {
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                MODIFY_USER_ADD_LINK, ObjectModificationType.COMPLEX_TYPE);

        Collection<? extends ItemDelta<?, ?>> deltas =
                DeltaConvertor.toModifications(modification, UserType.class, prismContext);
        OperationResult result = new OperationResult("MODIFY");
        repositoryService.modifyObject(UserType.class, "1234", deltas, getModifyOptions(), result);
    }

    @Test
    public void test030ModifyUserOnNonExistingAccountTest() throws Exception {
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

        Collection<? extends ItemDelta<?, ?>> deltas =
                DeltaConvertor.toModifications(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, oid, deltas, getModifyOptions(), result);

        ItemDeltaCollectionsUtil.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        logger.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test
    public void test031ModifyUserOnExistingAccountTest() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

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

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                MODIFY_USER_ADD_LINK, ObjectModificationType.COMPLEX_TYPE);
        Collection<? extends ItemDelta<?, ?>> deltas =
                DeltaConvertor.toModifications(modification, UserType.class, prismContext);

        // WHEN
        repositoryService.modifyObject(UserType.class, oid, deltas, getModifyOptions(), result);

        ItemDeltaCollectionsUtil.applyTo(deltas, userOld);

        PrismObject<UserType> userNew = repositoryService.getObject(UserType.class, oid, null, result);
        ObjectDelta<UserType> delta = userOld.diff(userNew);
        logger.debug("Modify diff \n{}", delta.debugDump(3));
        AssertJUnit.assertTrue("Modify was unsuccessful, diff size: "
                + delta.getModifications().size(), delta.isEmpty());
        AssertJUnit.assertTrue("User is not equivalent.", userOld.equivalent(userNew));
    }

    @Test
    public void test032ModifyTaskObjectRef() throws Exception {
        OperationResult result = createOperationResult();
        File taskFile = new File(TEST_DIR, "task.xml");
        System.out.println("ADD");
        PrismObject<TaskType> task = prismContext.parseObject(taskFile);
        repositoryService.addObject(task, null, result);
        final String taskOid = "00000000-0000-0000-0000-123450000001";
        AssertJUnit.assertNotNull(taskOid);
        System.out.println("GET");
        PrismObject<TaskType> getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        String lastVersion = getTask.getVersion();
        AssertJUnit.assertTrue(task.equivalent(getTask));
        TaskType taskType = getTask.asObjectable();
        AssertJUnit.assertNull(taskType.getObjectRef());

        Collection modifications = new ArrayList();

        PrismObjectDefinition objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
        PrismReferenceDefinition def = objectDef.findReferenceDefinition(TaskType.F_OBJECT_REF);
        System.out.println("MODIFY");
        ObjectReferenceType objectRef;
        ReferenceDelta delta = prismContext.deltaFactory().reference().create(def);
        delta.addValueToAdd(itemFactory().createReferenceValue("1", ResourceType.COMPLEX_TYPE));
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

        System.out.println("MODIFY");
        modifications.clear();
        delta = prismContext.deltaFactory().reference().create(def);
        delta.addValueToDelete(itemFactory().createReferenceValue("1", ResourceType.COMPLEX_TYPE));
        delta.addValueToAdd(itemFactory().createReferenceValue("2", ResourceType.COMPLEX_TYPE));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, getModifyOptions(), result);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        assertEquals("2", objectRef.getOid());
        logger.info(PrismTestUtil.serializeObjectToString(taskType.asPrismObject()));
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();

        modifications.clear();
        delta = prismContext.deltaFactory().reference().create(def);
        delta.addValueToDelete(itemFactory().createReferenceValue("2", ResourceType.COMPLEX_TYPE));
        delta.addValueToAdd(itemFactory().createReferenceValue("1", ResourceType.COMPLEX_TYPE));
        modifications.add(delta);
        repositoryService.modifyObject(TaskType.class, taskOid, modifications, getModifyOptions(), result);

        getTask = repositoryService.getObject(TaskType.class, taskOid, null, result);
        taskType = getTask.asObjectable();
        AssertJUnit.assertNotNull(taskType.getObjectRef());
        objectRef = taskType.getObjectRef();
        assertEquals("1", objectRef.getOid());
        SqlRepoTestUtil.assertVersionProgress(lastVersion, getTask.getVersion());
        lastVersion = getTask.getVersion();
    }

    @Test // MID-4801 (this passed even before fixing that issue)
    public void test035ModifyTaskOwnerRef() throws Exception {
        OperationResult result = createOperationResult();
        TaskType task = new TaskType(prismContext)
                .oid("035")
                .name("task035")
                .ownerRef("owner-old", UserType.COMPLEX_TYPE)
                .taskIdentifier("task035");

        repositoryService.addObject(task.asPrismObject(), null, result);
        assertTaskOwner(task.getOid(), "owner-old");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_OWNER_REF).replace(ObjectTypeUtil.createObjectRef("owner-new", ObjectTypes.USER))
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, task.getOid(), modifications, getModifyOptions(), result);
        assertTaskOwner(task.getOid(), "owner-new");
    }

    @Test   // MID-4801 (this failed before fixing that issue)
    public void test036ModifyTaskOwnerRefAddAndDelete() throws Exception {
        OperationResult result = createOperationResult();
        TaskType task = new TaskType(prismContext)
                .oid("036")
                .name("task036")
                .ownerRef("owner-old", UserType.COMPLEX_TYPE)
                .taskIdentifier("task036");

        repositoryService.addObject(task.asPrismObject(), null, result);
        assertTaskOwner(task.getOid(), "owner-old");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_OWNER_REF)
                .delete(ObjectTypeUtil.createObjectRef("owner-old", ObjectTypes.USER))
                .add(ObjectTypeUtil.createObjectRef("owner-new", ObjectTypes.USER))
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, task.getOid(), modifications, getModifyOptions(), result);
        assertTaskOwner(task.getOid(), "owner-new");
    }

    @Test   // MID-4801
    public void test050ModifyUserEmployeeNumber() throws Exception {
        OperationResult result = createOperationResult();
        UserType user = new UserType(prismContext)
                .oid("050")
                .name("user050")
                .employeeNumber("old");

        repositoryService.addObject(user.asPrismObject(), null, result);
        assertUserEmployeeNumber(user.getOid(), "old");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .delete("old")
                .add("new")
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), modifications, getModifyOptions(), result);
        assertUserEmployeeNumber(user.getOid(), "new");
    }

    @Test
    public void test052ModifyUserEmployeeNumberDynamically() throws Exception {
        OperationResult result = createOperationResult();
        UserType user = new UserType(prismContext)
                .oid("052")
                .name("user052")
                .employeeNumber("old");

        repositoryService.addObject(user.asPrismObject(), null, result);
        assertUserEmployeeNumber(user.getOid(), "old");

        repositoryService.modifyObjectDynamically(UserType.class, user.getOid(), null, userBefore -> {
            String employeeNumber = userBefore.getEmployeeNumber();
            if ("old".equals(employeeNumber)) {
                return prismContext.deltaFor(UserType.class)
                        .item(UserType.F_EMPLOYEE_NUMBER)
                        .delete("old")
                        .add("new")
                        .asItemDeltas();
            } else {
                throw new IllegalStateException("employeeNumber is not 'old': " + employeeNumber);
            }
        }, getModifyOptions(), result);
        assertUserEmployeeNumber(user.getOid(), "new");
    }

    @Test   // MID-4801
    public void test055DeleteUserEmployeeNumberWrong() throws Exception {
        OperationResult result = createOperationResult();
        UserType user = new UserType(prismContext)
                .oid("055")
                .name("user055")
                .employeeNumber("old");

        repositoryService.addObject(user.asPrismObject(), null, result);
        assertUserEmployeeNumber(user.getOid(), "old");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .delete("oldWrong")
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), modifications, getModifyOptions(), result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        displayValue("user after", userAfter);
        assertEquals("Wrong employeeNumber after operation", "old", userAfter.asObjectable().getEmployeeNumber());

        assertUserEmployeeNumber(user.getOid(), "old");
    }

    @Test   // MID-4801
    public void test056EmptyUserEmployeeNumberDelta() throws Exception {
        OperationResult result = createOperationResult();
        UserType user = new UserType(prismContext)
                .oid("056")
                .name("user056")
                .employeeNumber("old");

        repositoryService.addObject(user.asPrismObject(), null, result);
        assertUserEmployeeNumber(user.getOid(), "old");

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(prismContext.deltaFactory().property().createDelta(UserType.F_EMPLOYEE_NUMBER, UserType.class));
        repositoryService.modifyObject(UserType.class, user.getOid(), modifications, getModifyOptions(), result);

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        displayValue("user after", userAfter);
        assertEquals("Wrong employeeNumber after operation", "old", userAfter.asObjectable().getEmployeeNumber());

        assertUserEmployeeNumber(user.getOid(), "old");
    }

    private void assertTaskOwner(String taskOid, String expectedOwnerOid) {
        EntityManager em = open();
        //noinspection unchecked
        List<String> ownerOidList = em.createQuery("select t.ownerRefTask.targetOid from RTask t where t.oid = '" + taskOid + "'").getResultList();
        assertEquals("Wrong # of owner OIDs found", 1, ownerOidList.size());
        assertEquals("Wrong owner OID", expectedOwnerOid, ownerOidList.get(0));
        close(em);
    }

    private void assertUserEmployeeNumber(String userOid, String expectedValue) {
        EntityManager em = open();
        //noinspection unchecked
        List<String> ownerOidList = em.createQuery("select u.employeeNumber from RUser u where u.oid = '" + userOid + "'").getResultList();
        assertEquals("Wrong # of users found", 1, ownerOidList.size());
        assertEquals("Wrong employee number", expectedValue, ownerOidList.get(0));
        close(em);
    }

    @Test
    public void test100ModifyUserAddRole() throws Exception {
        OperationResult parentResult = new OperationResult("Modify user -> add roles");
        String userToModifyOid = "f65963e3-9d47-4b18-aaf3-bfc98bdfa000";

        PrismObject<ResourceType> csvResource = prismContext.parseObject(new File(TEST_DIR + "/resource-csv.xml"));
        repositoryService.addObject(csvResource, null, parentResult);

        PrismObject<ResourceType> openDjResource = prismContext.parseObject(new File(TEST_DIR + "/resource-opendj.xml"));
        repositoryService.addObject(openDjResource, null, parentResult);

        PrismObject<UserType> user = prismContext.parseObject(new File(TEST_DIR + "/user.xml"));
        repositoryService.deleteObject(UserType.class, "f65963e3-9d47-4b18-aaf3-bfc98bdfa000", parentResult);       // from earlier test
        repositoryService.addObject(user, null, parentResult);

        PrismObject<RoleType> roleCsv = prismContext.parseObject(new File(TEST_DIR + "/role-csv.xml"));
        repositoryService.addObject(roleCsv, null, parentResult);

        String ldapRoleOid = "12345678-d34d-b33f-f00d-987987987988";
        PrismObject<RoleType> roleLdap = prismContext.parseObject(new File(TEST_DIR + "/role-ldap.xml"));
        repositoryService.addObject(roleLdap, null, parentResult);

        RoleType ldapRole = repositoryService.getObject(RoleType.class, ldapRoleOid, null, parentResult).asObjectable();
        assertNotNull("null role", ldapRole);

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(TEST_DIR + "/modify-user-add-roles.xml"),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, userToModifyOid, delta.getModifications(), getModifyOptions(), parentResult);

        UserType modifiedUser = repositoryService.getObject(UserType.class, userToModifyOid, null, parentResult).asObjectable();
        assertEquals("wrong number of assignments", 3, modifiedUser.getAssignment().size());

        // MID-4820
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("modifyUser")
                .and().item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref("12345678-d34d-b33f-f00d-987987987989")   // role-csv
                .build();
        SearchResultList<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, parentResult);
        assertEquals("Wrong # of returned users", 1, users.size());
    }

    /**
     * Modify account metadata. Make sure that no unrelated item has changed.
     */
    @Test
    public void test120ModifyAccountMetadata() throws Exception {
        // GIVEN
        OperationResult parentResult = createOperationResult();

        repositoryService.deleteObject(ShadowType.class, "1234567890", parentResult);       // from earlier test

        PrismObject<ShadowType> shadowBefore = prismContext.parseObject(ACCOUNT_FILE);

        MetadataType metaData = new MetadataType();
        metaData.setCreateChannel("channel");
        metaData.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        shadowBefore.asObjectable().setMetadata(metaData);

        // The parsed shadow has attributes that have xsi:type specification. Add another one that has
        // fully dynamic definition

        QName attrBazQName = new QName(MidPointConstants.NS_RI, "baz");
        PrismContainer<Containerable> attributesContainerBefore = shadowBefore.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<String> attrBazBefore = prismContext.itemFactory().createProperty(new QName(MidPointConstants.NS_RI, "baz"));
        MutablePrismPropertyDefinition<String> attrBazDefBefore = prismContext.definitionFactory().createPropertyDefinition(attrBazQName, DOMUtil.XSD_STRING);
        attrBazDefBefore.setMaxOccurs(-1);
        // Unless marked as dynamic, the repo XML object will not have xsi:type (and so the repo will parse them as raw when
        // fetching). Normally, the provisioning module is responsible for applying the definition ... but we have
        // no provisioning available here.
        attrBazDefBefore.setDynamic(true);
        attrBazBefore.setDefinition(attrBazDefBefore);
        attrBazBefore.addRealValue("BaZ1");
        attrBazBefore.addRealValue("BaZ2");
        attrBazBefore.addRealValue("BaZ3");
        attributesContainerBefore.add(attrBazBefore);

        System.out.println("\nAcc shadow");
        System.out.println(shadowBefore.debugDump());

        String oid = repositoryService.addObject(shadowBefore, null, parentResult);

        // WHEN
        when();

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, oid, null, parentResult);

        // THEN
        then();
        System.out.println("\nRepo shadow");
        System.out.println(repoShadow.debugDump());

        ObjectDelta<ShadowType> d = repoShadow.diff(shadowBefore);
        System.out.println("\nDelta");
        System.out.println(d.debugDump());
        assertTrue("Delta after add is not empty", d.isEmpty());

        PrismObjectDefinition<ShadowType> accountDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        PropertyDelta<?> pdelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                (ItemPath.create(ObjectType.F_METADATA, MetadataType.F_MODIFY_CHANNEL)), accountDefinition, "channel");
        modifications.add(pdelta);

        XMLGregorianCalendar modifyTimestampBefore = XmlTypeConverter
                .createXMLGregorianCalendar(System.currentTimeMillis());
        pdelta = prismContext.deltaFactory().property().createModificationReplaceProperty((ItemPath.create(ObjectType.F_METADATA,
                MetadataType.F_MODIFY_TIMESTAMP)), accountDefinition, modifyTimestampBefore);
        modifications.add(pdelta);

        when();
        repositoryService.modifyObject(ShadowType.class, oid, modifications, getModifyOptions(), parentResult);

        then();
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
        XMLGregorianCalendar now = XmlTypeConverter
                .createXMLGregorianCalendar(System.currentTimeMillis());
        List<ItemDelta<?, ?>> syncSituationDeltas =
                SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta(
                        repoShadow.asObjectable(), SynchronizationSituationType.LINKED, null, false, now);

        // WHEN
        when();
        repositoryService.modifyObject(ShadowType.class, oid, syncSituationDeltas, getModifyOptions(), parentResult);
//        AssertJUnit.assertNull(afterModify.asObjectable().getObjectChange());

        // THEN
        then();
        afterModify = repositoryService.getObject(ShadowType.class,
                oid, null, parentResult);
        System.out.println("\nAfter modify 2");
        System.out.println(afterModify.debugDump());
    }

    @Test
    public void test130ExtensionModify() throws Exception {
        File userFile = new File(TEST_DIR, "user-with-extension.xml");
        //add first user
        PrismObject<UserType> user = prismContext.parseObject(userFile);

        OperationResult result = new OperationResult("test extension modify");
        final String oid = repositoryService.addObject(user, null, result);

        user = prismContext.parseObject(userFile);
        PrismObject<UserType> readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not saved correctly", user.diff(readUser).isEmpty());
        String lastVersion = readUser.getVersion();

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        ItemPath path = ItemPath.create(UserType.F_EXTENSION, QNAME_LOOT);
        PrismProperty<Integer> loot = user.findProperty(path);
        PropertyDelta lootDelta = prismContext.deltaFactory().property().create(path, loot.getDefinition());
        lootDelta.setRealValuesToReplace(456);
        modifications.add(lootDelta);

        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        //check read after modify operation
        user = prismContext.parseObject(userFile);
        loot = user.findProperty(ItemPath.create(UserType.F_EXTENSION, QNAME_LOOT));
        loot.setRealValue(456);

        readUser = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertTrue("User was not modified correctly", user.diff(readUser).isEmpty());

        SqlRepoTestUtil.assertVersionProgress(lastVersion, readUser.getVersion());
    }

    @Test
    public void test140ModifyAccountSynchronizationSituation() throws Exception {
        OperationResult result = new OperationResult("testModifyAccountSynchronizationSituation");

        //add account
        File accountFile = new File(TEST_DIR, "account-synchronization-situation.xml");
        PrismObject<ShadowType> account = prismContext.parseObject(accountFile);
        repositoryService.addObject(account, null, result);

        XMLGregorianCalendar timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        List<ItemDelta<?, ?>> syncSituationDeltas =
                SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta(
                        account.asObjectable(), SynchronizationSituationType.LINKED, null, false, timestamp);

        repositoryService.modifyObject(ShadowType.class, account.getOid(), syncSituationDeltas, getModifyOptions(), result);

        PrismObject<ShadowType> afterFirstModify = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
        AssertJUnit.assertNotNull(afterFirstModify);
        ShadowType afterFirstModifyType = afterFirstModify.asObjectable();
        assertEquals(1, afterFirstModifyType.getSynchronizationSituationDescription().size());
        SynchronizationSituationDescriptionType description = afterFirstModifyType.getSynchronizationSituationDescription().get(0);
        assertEquals(SynchronizationSituationType.LINKED, description.getSituation());

        timestamp = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        syncSituationDeltas = SynchronizationUtils.createSynchronizationSituationAndDescriptionDelta(
                afterFirstModify.asObjectable(), null, null, false, timestamp);

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

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_SYNCHRONIZATION_TIMESTAMP).lt(description.getTimestamp())
                .build();
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        AssertJUnit.assertNotNull(shadows);
        assertEquals(1, shadows.size());

        System.out.println("shadow: " + shadows.get(0).debugDump());
    }

    private String accountOid;

    @Test
    public void test142ModifyAccountAttributeSameValue() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ATTRIBUTE_FILE);
        repositoryService.addObject(account, null, result);
        accountOid = account.getOid();

        PrismPropertyDefinition<String> definition = prismContext.definitionFactory().createPropertyDefinition(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(SchemaConstants.ICFS_NAME_PATH, definition)
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
        OperationResult result = createOperationResult();

        assertNotNull("account-attribute was not imported in previous tests", accountOid);

        PrismPropertyDefinition<String> definition = prismContext.definitionFactory().createPropertyDefinition(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(SchemaConstants.ICFS_NAME_PATH, definition)
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
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.parseObject(new File(TEST_DIR, "user-with-assignment-extension.xml"));
        repositoryService.addObject(user, null, result);

        PrismPropertyDefinition<String> definition = prismContext.definitionFactory().createPropertyDefinition(QNAME_WEAPON, DOMUtil.XSD_STRING);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, QNAME_WEAPON), definition)
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
        OperationResult result = createOperationResult();

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
        Collection<? extends ItemDelta<?, ?>> deltas = DeltaConvertor.toModifications(modification,
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
        repositoryService.modifyObject(RoleType.class, oid, new ArrayList<>(), getModifyOptions(), result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        role = repositoryService.getObject(RoleType.class, oid, null, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
        if (RepoModifyOptions.isForceReindex(getModifyOptions())) {
            SqlRepoTestUtil.assertVersionProgress(version, role.getVersion());
        } else {
            assertEquals("Version has changed", version, role.getVersion());
        }
    }

    @Test
    public void test160ModifyWithPrecondition() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = o -> {
            throw new PreconditionViolationException("hello");
        };

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, getModifyOptions(), result);
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
        OperationResult result = createOperationResult();

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = o -> false;

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, getModifyOptions(), result);
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
        OperationResult result = createOperationResult();

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = new VersionPrecondition<>("9999");

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        try {
            repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, getModifyOptions(), result);
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
        OperationResult result = createOperationResult();

        // GIVEN
        String versionBefore = repositoryService.getVersion(RoleType.class, roleOid, result);
        ModificationPrecondition<RoleType> precondition = new VersionPrecondition<>(versionBefore);

        // WHEN
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("123456")
                .asItemDeltas();
        repositoryService.modifyObject(RoleType.class, roleOid, itemDeltas, precondition, getModifyOptions(), result);

        String versionAfter = repositoryService.getVersion(RoleType.class, roleOid, result);
        assertEquals("unexpected version change", Integer.parseInt(versionBefore) + 1, Integer.parseInt(versionAfter));
        String description = repositoryService.getObject(RoleType.class, roleOid, null, result).asObjectable().getDescription();
        assertEquals("description was not set", "123456", description);
    }

    @Test
    public void test200ReplaceAttributes() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ATTRIBUTE_FILE);
        account.setOid(null);
        repositoryService.addObject(account, null, result);
        accountOid = account.getOid();

        QName ATTR1_QNAME = new QName(MidPointConstants.NS_RI, "attr1");
        PrismPropertyDefinition<String> def1 = prismContext.definitionFactory().createPropertyDefinition(ATTR1_QNAME, DOMUtil.XSD_STRING);
        ShadowAttributesType attributes = new ShadowAttributesType(prismContext);
        PrismProperty<String> attr1 = def1.instantiate();
        attr1.setRealValue("value1");
        attributes.asPrismContainerValue().add(attr1);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .replace(attributes)
                .asItemDeltas();

        repositoryService.modifyObject(ShadowType.class, accountOid, itemDeltas, getModifyOptions(), result);

        EntityManager em = open();
        List shadows = em.createQuery("from RShadow").getResultList();
        logger.info("shadows:\n{}", shadows);
        //noinspection unchecked
        List<Object[]> extStrings = em.createQuery("select e.owner.oid, e.itemId, e.value from ROExtString e").getResultList();
        for (Object[] extString : extStrings) {
            logger.info("-> {}", Arrays.asList(extString));
        }
        close(em);

        ObjectQuery query1 = prismContext.queryFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR1_QNAME), def1).eq("value1")
                .build();
        List list1 = repositoryService.searchObjects(ShadowType.class, query1, null, result);
        logger.info("*** query1 result:\n{}", DebugUtil.debugDump(list1));
        assertEquals("Wrong # of query1 results", 1, list1.size());

        em = open();
        RObject obj = (RObject) em.createQuery("from RObject where oid = :o").setParameter("o", account.getOid()).getSingleResult();
        assertEquals(1, obj.getStrings().size());
        close(em);

        // delete
        itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .delete(attributes.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, accountOid, itemDeltas, getModifyOptions(), result);

        em = open();
        obj = (RObject) em.createQuery("from RObject where oid = :o").setParameter("o", account.getOid()).getSingleResult();
        assertEquals(0, obj.getStrings().size());
        close(em);

        // add
        itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .add(attributes.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, accountOid, itemDeltas, getModifyOptions(), result);

        em = open();
        obj = (RObject) em.createQuery("from RObject where oid = :o").setParameter("o", account.getOid()).getSingleResult();
        assertEquals(1, obj.getStrings().size());
        close(em);

        // now test the "export" serialization option

        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, accountOid, createRawCollection(), result);
        String xml = prismContext.xmlSerializer().options(createSerializeForExport()).serialize(shadow);
        System.out.println("Serialized for export:\n" + xml);
        PrismObject<Objectable> shadowReparsed = prismContext.parseObject(xml);
        System.out.println("Reparsed:\n" + shadowReparsed.debugDump());
        Item<PrismValue, ItemDefinition<?>> attr1Reparsed = shadowReparsed.findItem(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR1_QNAME));
        assertNotNull(attr1Reparsed);
        assertFalse("Reparsed attribute is raw", attr1Reparsed.getAnyValue().isRaw());
    }

    private <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow, new QName(MidPointConstants.NS_RI, attrName), expectedValues);
    }

    private <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrQName, T... expectedValues) {
        PrismProperty<T> attr = shadow.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName));
        if (expectedValues.length == 0) {
            assertThat(attr)
                    .withFailMessage("Expected no value for attribute " + attrQName + " in " + shadow + ", but it has " + attr)
                    .isNull();
        } else {
            assertNotNull("No attribute " + attrQName + " in " + shadow, attr);
            PrismAsserts.assertPropertyValue(attr, expectedValues);
        }
    }

    @Test
    public void test210ModifyObjectCollection() throws Exception {
        OperationResult result = new OperationResult("test210ModifyObjectCollection");

        ObjectCollectionType collection = prismContext.createObjectable(ObjectCollectionType.class)
                .name("collection")
                .type(UserType.COMPLEX_TYPE);
        repositoryService.addObject(collection.asPrismObject(), null, result);

        List<ItemDelta<?, ?>> deltas1 = prismContext.deltaFor(ObjectCollectionType.class)
                .item(ObjectCollectionType.F_NAME).replace(PolyString.fromOrig("collection2"))
                .asItemDeltas();
        repositoryService.modifyObject(ObjectCollectionType.class, collection.getOid(), deltas1, getModifyOptions(), result);

        ItemDeltaCollectionsUtil.applyTo(deltas1, collection.asPrismObject());
        PrismObject<ObjectCollectionType> afterChange1 = repositoryService
                .getObject(ObjectCollectionType.class, collection.getOid(), null, result);
        assertTrue("Objects differ after change 1", collection.asPrismObject().equals(afterChange1, EquivalenceStrategy.DATA));

        ObjectFilter filter = prismContext.queryFor(UserType.class)
                .item(UserType.F_COST_CENTER).eq("100")
                .buildFilter();
        SearchFilterType filterBean = prismContext.getQueryConverter().createSearchFilterType(filter);

        List<ItemDelta<?, ?>> deltas2 = prismContext.deltaFor(ObjectCollectionType.class)
                .item(ObjectCollectionType.F_DESCRIPTION).replace("description")
                .item(ObjectCollectionType.F_FILTER).replace(filterBean)
                .asItemDeltas();
        repositoryService.modifyObject(ObjectCollectionType.class, collection.getOid(), deltas2, getModifyOptions(), result);

        ItemDeltaCollectionsUtil.applyTo(deltas2, collection.asPrismObject());
        PrismObject<ObjectCollectionType> afterChange2 = repositoryService
                .getObject(ObjectCollectionType.class, collection.getOid(), null, result);

        // it's hard to compare filters, so we have to do the test in a special way
        PrismObject<ObjectCollectionType> fromRepoWithoutFilter = afterChange2.clone();
        fromRepoWithoutFilter.asObjectable().setFilter(null);
        PrismObject<ObjectCollectionType> expectedWithoutFilter = collection.asPrismObject().clone();
        expectedWithoutFilter.asObjectable().setFilter(null);
        assertTrue("Objects (without filter) differ after change 2", expectedWithoutFilter.equals(fromRepoWithoutFilter, EquivalenceStrategy.DATA));

        SearchFilterType filterFromRepo = afterChange2.asObjectable().getFilter();
        SearchFilterType filterExpected = collection.getFilter();
        ObjectFilter filterFromRepoParsed = prismContext.getQueryConverter().createObjectFilter(UserType.class, filterFromRepo);
        ObjectFilter filterExpectedParsed = prismContext.getQueryConverter().createObjectFilter(UserType.class, filterExpected);
        assertTrue("Filters differ", filterExpectedParsed.equals(filterFromRepoParsed, false));
    }

    /**
     * Add shadow pendingOperations; MID-4831
     */
    @Test
    public void test250AddShadowPendingOperations() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<ShadowType> shadow = prismContext.createObjectable(ShadowType.class)
                .name("shadow1")
                .oid("000-aaa-bbb-ccc")
                .asPrismObject();
        repositoryService.addObject(shadow, null, result);

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_NAME).eqPoly("shadow1")
                .and().exists(ShadowType.F_PENDING_OPERATION)
                .build();

        List<PrismObject<ShadowType>> objectsBefore = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of shadows found (before)", 0, objectsBefore.size());

        // WHEN

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).add(new PendingOperationType(prismContext).executionStatus(COMPLETED))
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadow.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        List<PrismObject<ShadowType>> objectsAfter = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of shadows found (after)", 1, objectsAfter.size());
        displayValue("object found (after)", objectsAfter.get(0));
    }

    /**
     * Delete shadow pendingOperations; MID-4831
     */
    @Test
    public void test260DeleteShadowPendingOperations() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<ShadowType> shadow = prismContext.createObjectable(ShadowType.class)
                .name("shadow2")
                .oid("000-aaa-bbb-ddd")
                .beginPendingOperation()
                .executionStatus(COMPLETED)
                .<ShadowType>end()
                .asPrismObject();
        repositoryService.addObject(shadow, null, result);

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_NAME).eqPoly("shadow2")
                .and().exists(ShadowType.F_PENDING_OPERATION)
                .build();
        List<PrismObject<ShadowType>> objectsBefore = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of shadows found (before)", 1, objectsBefore.size());
        displayValue("object found (before)", objectsBefore.get(0));

        // WHEN

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).replace()
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadow.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        List<PrismObject<ShadowType>> objectsAfter = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong # of shadows found (after)", 0, objectsAfter.size());
    }

    /**
     * Add case work items
     */
    @Test
    public void test300AddCaseWorkItem() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<CaseType> caseObject = prismContext.createObjectable(CaseType.class)
                .name("testcase1")
                .oid("de8857ab-0220-4f8c-b627-8d6bd6c679a9")
                .state("open")
                .asPrismObject();
        repositoryService.addObject(caseObject, null, result);

        CaseWorkItemType workItem = new CaseWorkItemType(prismContext)
                .assigneeRef(new ObjectReferenceType().oid("f3285c65-a4fa-4bf3-bd78-3008bcf99d3c").type(UserType.COMPLEX_TYPE));

        // WHEN

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM).add(workItem)
                .asItemDeltas();
        repositoryService.modifyObject(CaseType.class, caseObject.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_NAME).eqPoly("testcase1")
                .build();
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        assertEquals("Wrong # of cases found", 1, cases.size());

        displayValue("case fetched from repository", cases.get(0));
    }

    @Test
    public void test310ModifyCaseWorkItemAssignee() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        final String OLD_OID = "0f4082cf-c0b6-4cd2-a3db-544c668bab0c";
        final String NEW_OID = "4cbdc40b-5693-4174-8634-acd3e0b96168";

        PrismObject<CaseType> caseObject = prismContext.createObjectable(CaseType.class)
                .name("case310")
                .oid("7d0c37f8-26e5-4213-af95-cfde175f3ff7")
                .state("open")
                .beginWorkItem()
                .id(1L)
                .assigneeRef(new ObjectReferenceType().oid(OLD_OID).type(UserType.COMPLEX_TYPE))
                .<CaseType>end()
                .asPrismObject();
        repositoryService.addObject(caseObject, null, result);

        // WHEN

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, 1L, CaseWorkItemType.F_ASSIGNEE_REF)
                .replace(new ObjectReferenceType().oid(NEW_OID).type(UserType.COMPLEX_TYPE))
                .asItemDeltas();
        repositoryService.modifyObject(CaseType.class, caseObject.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        List<CaseWorkItemType> workItems = assertAssignee(NEW_OID, 1, result);
        assertAssignee(OLD_OID, 0, result);

        System.out.println(workItems.get(0).asPrismContainerValue().debugDump());
    }

    @Test
    public void test320ModifyCaseWorkItemAssigneeAndCandidate() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        final String OLD_OID = "d886a44c-732d-44d4-8403-e7c44bbfba8b";
        final String OLD2_OID = "9a28119e-1283-4328-9e2f-ef383728a4d1";
        final String NEW_OID = "fba02f49-d019-4642-9941-6d8482be5d58";

        PrismObject<CaseType> caseObject = prismContext.createObjectable(CaseType.class)
                .name("case320")
                .state("open")
                .beginWorkItem()
                .id(1L)
                .assigneeRef(new ObjectReferenceType().oid(OLD_OID).type(UserType.COMPLEX_TYPE))
                .candidateRef(new ObjectReferenceType().oid(OLD_OID).type(UserType.COMPLEX_TYPE))       // intentionally the same
                .candidateRef(new ObjectReferenceType().oid(OLD2_OID).type(UserType.COMPLEX_TYPE))
                .<CaseType>end()
                .asPrismObject();
        repositoryService.addObject(caseObject, null, result);

        assertAssignee(OLD_OID, 1, result);
        assertCandidate(OLD_OID, 1, result);
        assertCandidate(OLD2_OID, 1, result);

        // WHEN

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, 1L, CaseWorkItemType.F_ASSIGNEE_REF)
                .replace(new ObjectReferenceType().oid(NEW_OID).type(UserType.COMPLEX_TYPE))
                .item(CaseType.F_WORK_ITEM, 1L, CaseWorkItemType.F_CANDIDATE_REF)
                .add(new ObjectReferenceType().oid(NEW_OID).type(UserType.COMPLEX_TYPE))
                .asItemDeltas();
        repositoryService.modifyObject(CaseType.class, caseObject.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        assertAssignee(OLD_OID, 0, result);
        assertAssignee(NEW_OID, 1, result);
        assertCandidate(OLD_OID, 1, result);
        assertCandidate(OLD2_OID, 1, result);
        List<CaseWorkItemType> workItems = assertCandidate(NEW_OID, 1, result);

        System.out.println(workItems.get(0).asPrismContainerValue().debugDump());
    }

    @NotNull
    private List<CaseWorkItemType> assertAssignee(String assigneeOid, int expected, OperationResult result)
            throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_ASSIGNEE_REF).ref(assigneeOid)
                .build();
        List<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        assertEquals("Wrong # of work items found", expected, workItems.size());
        return workItems;
    }

    @NotNull
    private List<CaseWorkItemType> assertCandidate(String candidateOid, int expected, OperationResult result)
            throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_CANDIDATE_REF).ref(candidateOid)
                .build();
        List<CaseWorkItemType> workItems = repositoryService.searchContainers(CaseWorkItemType.class, query, null, result);
        assertEquals("Wrong # of work items found", expected, workItems.size());
        return workItems;
    }

    @Test   // MID-5104
    public void test350ReplaceAssignmentModifyApprover() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name("test350")
                .oid("oid-350")
                .beginAssignment()
                .id(123L)
                .targetRef("oid0", RoleType.COMPLEX_TYPE)
                .<UserType>end()
                .asPrismObject();
        repositoryService.addObject(user, null, result);

        // WHEN

        ObjectReferenceType approver1 = new ObjectReferenceType()
                .oid("approver1-oid").type(UserType.COMPLEX_TYPE).relation(SchemaConstants.ORG_DEFAULT);

        List<ItemDelta<?, ?>> itemDeltas1 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 123L, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approver1.clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltas1, getModifyOptions(), result);

        // THEN
    }

    @Test   // MID-5105
    public void test360ReplaceModifyApprovers() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name("test360")
                .oid("oid-360")
                .asPrismObject();
        repositoryService.addObject(user, null, result);

        // WHEN

        ObjectReferenceType approver1 = new ObjectReferenceType().oid("approver1-oid");
        ObjectReferenceType approver1AsUser = new ObjectReferenceType().oid("approver1-oid").type(UserType.COMPLEX_TYPE);
        ObjectReferenceType approver2AsUser = new ObjectReferenceType().oid("approver2-oid").type(UserType.COMPLEX_TYPE);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approver1.clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltas, getModifyOptions(), result);

        itemDeltas = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approver1AsUser.clone(), approver2AsUser.clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltas, getModifyOptions(), result);

        // THEN

        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, user.getOid(), null, result);
        displayValue("user after", userAfter);

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).ref(approver1.getOid())
                .build();
        SearchResultList<PrismObject<UserType>> users = repositoryService
                .searchObjects(UserType.class, query, null, result);
        assertEquals("Wrong # of users found", 1, users.size());
    }

    // Normally this would be in schema module but we don't have initialized protector there
    @Test   // MID-5516
    public void test400RemoveCoreProtectedStringValueInMemory() throws Exception {
        ProtectedStringType passwordValue = protector.encryptString("hi");
        UserType jack = new UserType(prismContext)
                .name("jack")
                .beginCredentials()
                .beginPassword()
                .value(passwordValue.clone())
                .<CredentialsType>end()
                .end();

        displayValue("jack before", jack.asPrismObject());

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)
                .delete(passwordValue.clone())
                .asObjectDelta("");

        delta.applyTo(jack.asPrismObject());

        displayValue("jack after", jack.asPrismObject());

        jack.asPrismObject().checkConsistence();
    }

    @Test   // MID-5516
    public void test410RemoveExtensionProtectedStringValueInMemory() throws Exception {
        ProtectedStringType protectedValue = protector.encryptString("hi");
        UserType jack = new UserType(prismContext)
                .name("jack");
        PrismPropertyDefinition<ProtectedStringType> definition = jack.asPrismObject().getDefinition()
                .findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, "protected"));
        PrismProperty<ProtectedStringType> protectedProperty = definition.instantiate();
        protectedProperty.setRealValue(protectedValue.clone());
        jack.asPrismObject().addExtensionItem(protectedProperty);

        displayValue("jack before", jack.asPrismObject());

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, "protected")
                .delete(protectedValue.clone())
                .asObjectDelta("");

        delta.applyTo(jack.asPrismObject());

        displayValue("jack after", jack.asPrismObject());

        jack.asPrismObject().checkConsistence();
    }

    @Test   // MID-5516
    public void test420RemoveExtensionProtectedStringValueInRepo() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        ProtectedStringType protectedValue = protector.encryptString("hi");
        UserType jack = new UserType(prismContext)
                .name("jack");
        PrismPropertyDefinition<ProtectedStringType> definition = jack.asPrismObject().getDefinition()
                .findPropertyDefinition(ItemPath.create(UserType.F_EXTENSION, "protected"));
        PrismProperty<ProtectedStringType> protectedProperty = definition.instantiate();
        protectedProperty.setRealValue(protectedValue.clone());
        jack.asPrismObject().addExtensionItem(protectedProperty);

        repositoryService.addObject(jack.asPrismObject(), null, result);

        displayValue("jack before", jack.asPrismObject());

        // WHEN

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, "protected")
                .delete(protectedValue.clone())
                .asObjectDelta("");

        repositoryService.modifyObject(UserType.class, jack.getOid(), delta.getModifications(), result);

        // THEN

        PrismObject<UserType> jackAfter = repositoryService.getObject(UserType.class, jack.getOid(), null, result);

        displayValue("jack after", jackAfter);

        jackAfter.checkConsistence();
    }

    @Test  // MID-5826 - fortunately, in 4.0 this works due to reworked handling of extension values in repo
    public void test450ReplaceExtensionItem() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
                .name("test400")
                .oid("oid-400")
                .asPrismObject();
        repositoryService.addObject(user, null, result);

        assertExtensionDateValue(user.getOid(), 0);

        XMLGregorianCalendar dateTime = XmlTypeConverter.createXMLGregorianCalendar("2022-04-05T16:14:58");

        List<ItemDelta<?, ?>> itemDeltasSet = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, QNAME_FUNERAL_DATE).replace(dateTime)
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltasSet, getModifyOptions(), result);

        assertExtensionDateValue(user.getOid(), 1);

        List<ItemDelta<?, ?>> itemDeltasUnset = prismContext.deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, QNAME_FUNERAL_DATE).replace()
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, user.getOid(), itemDeltasUnset, getModifyOptions(), result);

        assertExtensionDateValue(user.getOid(), 0);
    }

    private void assertExtensionDateValue(String objectOid, int expected) {
        EntityManager em = open();
        //noinspection unchecked
        List<Timestamp> values = em.createQuery("select d.value from ROExtDate d where d.ownerOid = '" + objectOid + "'").getResultList();
        System.out.println("Values: " + values);
        assertEquals("Wrong # of extension values found", expected, values.size());
        close(em);
    }

    /**
     * MID-6063
     */
    @Test
    public void test510ModifySystemConfiguration() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        PrismObject<SystemConfigurationType> before = prismContext.parseObject(SYSTEM_CONFIGURATION_BEFORE_FILE);
        repositoryService.addObject(before, null, result);

        PrismObject<SystemConfigurationType> after = prismContext.parseObject(SYSTEM_CONFIGURATION_AFTER_FILE);
        ObjectDelta<SystemConfigurationType> delta = before.diff(after, EquivalenceStrategy.LITERAL);

        String oid = before.getOid();

        // WHEN
        repositoryService.modifyObject(SystemConfigurationType.class, oid, delta.getModifications(), getModifyOptions(), result);

        // THEN
        PrismObject<SystemConfigurationType> read = repositoryService.getObject(SystemConfigurationType.class, oid, null, result);

        normalize(after);
        normalize(read);
        PrismAsserts.assertEquals("System configuration was stored incorrectly", after, read);
    }

    /**
     * Adding equivalent assignment (without ID) to a user. It should replace the existing one.
     */
    @Test
    public void test520AddEquivalentAssignmentNoId() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();

        PrismValue assignmentValue = prismContext.parserFor(USER_ADAM_NEW_ASSIGNMENT_NO_ID_FILE).parseItemValue();
        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(assignmentValue)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertThat(userAfter.asObjectable().getAssignment().size()).as("# of assignments").isEqualTo(1);
    }

    /**
     * Adding equivalent assignment (with ID) to a user. It should replace the existing one.
     */
    @Test
    public void test530AddEquivalentAssignmentExistingId() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();

        PrismValue assignmentValue = prismContext.parserFor(USER_ADAM_NEW_ASSIGNMENT_NO_ID_FILE).parseItemValue();

        PrismObject<UserType> userBefore = repositoryService.getObject(UserType.class, oid, null, result);
        displayDumpable("user before", userBefore);

        Long id = userBefore.findContainer(UserType.F_ASSIGNMENT).getValue().getId();
        System.out.println("Setting ID " + id + " to assignment being added");
        ((PrismContainerValue) assignmentValue).setId(id);

        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(assignmentValue)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertThat(userAfter.asObjectable().getAssignment().size()).as("# of assignments").isEqualTo(1);
    }

    /**
     * Adding equivalent assignment (with ID) to a user. Uses ADD+DELETE.
     */
    @Test
    public void test532AddEquivalentAssignmentExistingId() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();
        String channel = "test532";

        PrismObject<UserType> userBefore = repositoryService.getObject(UserType.class, oid, null, result);
        displayDumpable("user before", userBefore);

        PrismValue assignmentValueOld = userBefore.findItem(UserType.F_ASSIGNMENT).getValue().clone();
        PrismValue assignmentValueNew = assignmentValueOld.clone();
        assignmentValueNew.setValueMetadata(
                new ValueMetadataType(prismContext)
                        .beginStorage()
                        .createChannel(channel)
                        .<ValueMetadataType>end());
        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(assignmentValueOld).add(assignmentValueNew)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertThat(userAfter.asObjectable().getAssignment().size()).as("# of assignments").isEqualTo(1);
        assertValueMetadataChannel(userAfter, UserType.F_ASSIGNMENT, channel);
    }

    /**
     * Add 'name' metadata using simple 'add'
     */
    @Test
    public void test540AddNameMetadataUsingAdd() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();

        String channel = "test540";
        PrismValue newNameValue = createCloneWithMetadata(UserType.F_NAME, channel);

        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_NAME).add(newNameValue)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertValueMetadataChannel(userAfter, UserType.F_NAME, channel);
    }

    /**
     * Add 'name' metadata using 'delete' plus 'add'
     */
    @Test
    public void test542AddNameMetadataUsingDeleteAndAdd() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();

        String channel = "test542";
        PrismValue oldNameValue = userAdam.findItem(UserType.F_NAME).getValue().clone();
        PrismValue newNameValue = createCloneWithMetadata(UserType.F_NAME, channel);

        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_NAME).delete(oldNameValue).add(newNameValue)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertValueMetadataChannel(userAfter, UserType.F_NAME, channel);
    }

    /**
     * Add 'name' metadata using 'replace'
     */
    @Test
    public void test544AddNameMetadataUsingReplace() throws Exception {
        given();
        OperationResult result = createOperationResult();

        String oid = userAdam.getOid();

        String channel = "test544";
        PrismValue newNameValue = createCloneWithMetadata(UserType.F_NAME, channel);

        List<ItemDelta<?, ?>> modifications = deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(newNameValue)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(UserType.class, oid, modifications, getModifyOptions(), result);

        then();
        PrismObject<UserType> userAfter = repositoryService.getObject(UserType.class, oid, null, result);

        displayDumpable("user after", userAfter);
        assertValueMetadataChannel(userAfter, UserType.F_NAME, channel);
    }

    private void assertValueMetadataChannel(PrismObject<?> object, ItemName itemName, String expectedValue) {
        Item<PrismValue, ItemDefinition<?>> item = object.findItem(itemName);
        assertThat(item).isNotNull();
        assertThat(item.size()).isEqualTo(1);
        ValueMetadataType metadata = item.getValue().getValueMetadata().getRealValue(ValueMetadataType.class);
        assertThat(metadata).isNotNull();
        assertThat(metadata.getStorage()).isNotNull();
        assertThat(metadata.getStorage().getCreateChannel()).isEqualTo(expectedValue);
    }

    private PrismValue createCloneWithMetadata(ItemName itemName, String channel) throws SchemaException {
        ValueMetadataType metadata = new ValueMetadataType(prismContext)
                .beginStorage()
                .createChannel(channel)
                .end();

        PrismValue clonedValue = userAdam.findItem(itemName).getValue().clone();
        clonedValue.setValueMetadata(metadata);
        return clonedValue;
    }

    private void normalize(PrismObject<?> object) {
        object.setVersion(null);
        //noinspection unchecked
        object.accept(value -> {
            if (value instanceof PrismContainerValue) {
                ((PrismContainerValue<?>) value).setId(null);
            }
        });
    }
}
