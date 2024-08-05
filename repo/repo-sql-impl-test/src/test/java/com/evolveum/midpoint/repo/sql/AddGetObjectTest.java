/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.*;
import javax.xml.namespace.QName;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sqlbase.ConflictWatcherImpl;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddGetObjectTest extends BaseSQLRepoTest {

    @Test
    public void test001AddSameName() throws Exception {
        final File user = new File(FOLDER_BASIC, "objects-user.xml");
        addGetCompare(user);
        try {
            // WHEN
            addGetCompare(user);

            assert false : "Unexpected success";
        } catch (ObjectAlreadyExistsException e) {
            TestUtil.assertExceptionSanity(e);
        }
    }

    @Test
    public void test002AddGetDSEESyncDouble() throws Exception {
        final File OBJECTS_FILE = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!OBJECTS_FILE.exists()) {
            logger.warn("skipping addGetDSEESyncDoubleTest, file {} not found.", OBJECTS_FILE.getPath());
            return;
        }
        addGetCompare(OBJECTS_FILE);
        try {
            // WHEN
            addGetCompare(OBJECTS_FILE);

            assert false : "Unexpected success";
        } catch (ObjectAlreadyExistsException e) {
            TestUtil.assertExceptionSanity(e);
        }
    }

    @Test
    public void test005SimpleAddGet() throws Exception {
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        List<PrismObject<?>> objects = addGetCompare(OBJECTS_FILE);

        boolean foundAtestuserX00003 = false;
        for (PrismObject<?> object : objects) {
            // adhoc check whether reference.targetName is preserved
            if ("atestuserX00003".equals(PolyString.getOrig(object.getName()))) {
                String personaName = PolyString.getOrig(((UserType) object.asObjectable()).getPersonaRef().get(0).getTargetName());
                assertNull("Wrong personaRef.targetName on atestuserX00003", personaName);
                foundAtestuserX00003 = true;
                break;
            }
        }
        assertTrue("User atestuserX00003 was not found", foundAtestuserX00003);
    }

    private List<PrismObject<?>> addGetCompare(File file) throws Exception {
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();
        List<String> oids = new ArrayList<>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            logger.info("Adding object {}, type {}", i + 1, object.getCompileTimeClass().getSimpleName());
            oids.add(repositoryService.addObject(object, null, result));
        }
        logger.info("Time to add objects ({}): {}", elements.size(), System.currentTimeMillis() - time);

        List<PrismObject<?>> objectsRead = new ArrayList<>();
        int count = 0;
        elements = prismContext.parserFor(file).parseObjects();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            try {
                object.asObjectable().setOid(oids.get(i));

                Class<? extends ObjectType> clazz = object.getCompileTimeClass();

                GetOperationOptionsBuilder optionsBuilder = getOperationOptionsBuilder();
                if (UserType.class.equals(clazz)) {
                    optionsBuilder = optionsBuilder.item(UserType.F_JPEG_PHOTO).retrieve();
                } else if (LookupTableType.class.equals(clazz)) {
                    optionsBuilder = optionsBuilder.item(LookupTableType.F_ROW).retrieve();
                } else if (AccessCertificationCampaignType.class.equals(clazz)) {
                    optionsBuilder = optionsBuilder.item(AccessCertificationCampaignType.F_CASE).retrieve();
                } else if (TaskType.class.equals(clazz)) {
                    optionsBuilder = optionsBuilder.item(TaskType.F_RESULT).retrieve();
                }
                PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), optionsBuilder.build(), result);

                logger.info("AFTER READ: {}\nOld\n{}\nnew\n{}", object, object.debugDump(3), newObject.debugDump(3));
                checkContainersSize(newObject, object);
                System.out.println("OLD: " + object.findProperty(ObjectType.F_NAME).getValue());
                System.out.println("NEW: " + newObject.findProperty(ObjectType.F_NAME).getValue());

                objectsRead.add(newObject);
                ObjectDelta delta = object.diff(newObject);

                count += delta.getModifications().size();
                if (delta.getModifications().size() > 0) {
                    if (delta.getModifications().size() == 1) {
                        ItemDelta d = (ItemDelta) delta.getModifications().iterator().next();

                        if (ShadowType.F_DEAD.equals(d.getElementName())) {
                            count -= delta.getModifications().size();
                            continue;
                        }
                    }
                    logger.error(">>> {} Found {} changes for {}\n{}", (i + 1),
                            delta.getModifications().size(), newObject, delta.debugDump(3));
                    ItemDelta id = (ItemDelta) delta.getModifications().iterator().next();
                    if (id.isReplace()) {
                        logger.debug("{}", id.getValuesToReplace().iterator().next());
                    }
                    logger.error("{}", prismContext.xmlSerializer().serialize(newObject));
                }
            } catch (Throwable ex) {
                logger.error("Exception occurred for {}", object, ex);
                throw new RuntimeException("Exception during processing of " + object + ": " + ex.getMessage(), ex);
            }
        }
        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
        return objectsRead;
    }

    private Integer size(PrismContainerValue value) {
        if (value == null) {
            return null;
        }

        return value.getItems().size();
    }

    private void checkContainerValuesSize(QName parentName, PrismContainerValue<?> newValue, PrismContainerValue<?> oldValue) {
        logger.info("Checking: " + parentName);
        AssertJUnit.assertEquals("Count doesn't match for '" + parentName + "' id=" + newValue.getId(), size(oldValue), size(newValue));

        List<QName> checked = new ArrayList<>();

        for (Item item : newValue.getItems()) {
            if (!(item instanceof PrismContainer)) {
                continue;
            }

            PrismContainer newContainer = (PrismContainer) item;
            PrismContainer oldContainer = oldValue.findContainer(newContainer.getElementName());
            AssertJUnit.assertNotNull("Container '" + newContainer.getElementName() + "' doesn't exist.", oldContainer);

            checkContainersSize(newContainer, oldContainer);
            checked.add(oldContainer.getElementName());
        }

        for (Item item : oldValue.getItems()) {
            if (!(item instanceof PrismContainer) || checked.contains(item.getElementName())) {
                continue;
            }

            PrismContainer oldContainer = (PrismContainer) item;
            PrismContainer newContainer = newValue.findContainer(oldContainer.getElementName());
            checkContainersSize(newContainer, oldContainer);
        }
    }

    private void checkContainersSize(PrismContainer<?> newContainer, PrismContainer<?> oldContainer) {
        logger.info("checkContainersSize {} new {}  old {}",
                newContainer.getElementName(), newContainer.size(), oldContainer.size());
        AssertJUnit.assertEquals(newContainer.size(), oldContainer.size());

        PrismContainerDefinition def = oldContainer.getDefinition();
        if (def != null && def.isMultiValue()) {
            // Comparison item-by-item is not reliable
            return;
        }
        List<Long> checked = new ArrayList<>();
        for (PrismContainerValue value : newContainer.getValues()) {
            PrismContainerValue oldValue = oldContainer.getValue(value.getId());

            checkContainerValuesSize(newContainer.getElementName(), value, oldValue);
            checked.add(value.getId());
        }

        for (PrismContainerValue value : oldContainer.getValues()) {
            if (checked.contains(value.getId())) {
                continue;
            }

            PrismContainerValue newValue = newContainer.getValue(value.getId());
            checkContainerValuesSize(newContainer.getElementName(), newValue, value);
        }
    }

    @Test
    public void test020AddUserWithAssignmentExtension() throws Exception {
        File file = new File(FOLDER_BASIC, "user-assignment-extension.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject((PrismObject) elements.get(0), null, result);

        PrismObject<UserType> fileUser = (PrismObject<UserType>) prismContext.parserFor(file).parseObjects().get(0);
        long id = 1;
        for (AssignmentType assignment : fileUser.asObjectable().getAssignment()) {
            assignment.setId(id);
            id++;
        }

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, oid, null, result);

        ObjectDelta<UserType> delta = fileUser.diff(repoUser);
        AssertJUnit.assertNotNull(delta);
        logger.info("delta\n{}", delta.debugDump(3));
        assertTrue(delta.isEmpty());
    }

    /**
     * Attempt to store full account in the repo and then get it out again. The
     * potential problem is that there are attributes that do not have a fixed
     * (static) definition.
     */
    @Test
    public void test030AddGetFullAccount() throws Exception {
        PrismObject<ShadowType> fileAccount = prismContext.parseObject(new File(FOLDER_BASIC, "account-full.xml"));

        // apply appropriate schema
        PrismObject<ResourceType> resource = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        ShadowUtil.applyResourceSchema(fileAccount, resourceSchema);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject(fileAccount, null, result);

        PrismObject<ShadowType> repoAccount = repositoryService.getObject(ShadowType.class, oid, null, result);

        ObjectDelta<ShadowType> delta = fileAccount.diff(repoAccount);
        AssertJUnit.assertNotNull(delta);
        logger.info("delta\n{}", delta.debugDump(3));
        if (!delta.isEmpty()) {
            fail("delta is not empty: " + delta.debugDump());
        }
        ShadowType repoShadow = repoAccount.asObjectable();
        AssertJUnit.assertNotNull(repoShadow.getSynchronizationSituation());
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, repoShadow.getSynchronizationSituation());
        AssertJUnit.assertNotNull(repoShadow.getSynchronizationSituationDescription());
        AssertJUnit.assertEquals(1, repoShadow.getSynchronizationSituationDescription().size());
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, repoShadow.getSynchronizationSituationDescription().get(0).getSituation());
        AssertJUnit.assertEquals("syncChannel", repoShadow.getSynchronizationSituationDescription().get(0).getChannel());
    }

    @Test
    public void test040AddGetSystemConfigFile() throws Exception {
        PrismObject<SecurityPolicyType> securityPolicy = prismContext.parseObject(new File(FOLDER_BASIC, "security-policy-special.xml"));

        OperationResult result = createOperationResult();
        String securityPolicyOid = "ce74cb86-c8e8-11e9-bee8-b37bf7a7ab4a";
        String oid = repositoryService.addObject(securityPolicy, null, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(securityPolicyOid, oid);
        PrismObject<SecurityPolicyType> repoSecurityPolicy = repositoryService.getObject(SecurityPolicyType.class, oid, null, result);
        AssertJUnit.assertNotNull(repoSecurityPolicy);

        String systemCongigOid = "00000000-0000-0000-0000-000000000001";
        PrismObject<SystemConfigurationType> fileSystemConfig = prismContext.parseObject(new File(FOLDER_BASIC, "systemConfiguration.xml"));
        logger.info("System config from file: {}", fileSystemConfig.debugDump());
        oid = repositoryService.addObject(fileSystemConfig, null, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(systemCongigOid, oid);

        PrismObject<SystemConfigurationType> repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, null, result);
        logger.info("System config from repo: {}", repoSystemConfig.debugDump());
        AssertJUnit.assertNull("global security policy not null", repoSystemConfig.asObjectable()
                .getGlobalSecurityPolicyRef());

        ReferenceDelta refDelta = prismContext.deltaFactory().reference().createModificationAdd(
                SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF, repoSystemConfig.getDefinition(),
                prismContext.itemFactory().createReferenceValue(repoSecurityPolicy));
        List<ReferenceDelta> refDeltas = new ArrayList<>();
        refDeltas.add(refDelta);

        // WHEN
        repositoryService.modifyObject(SystemConfigurationType.class, systemCongigOid, refDeltas, result);

        // THEN
        repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, null, result);
        logger.info("system config after modify: {}", repoSystemConfig.debugDump());
        AssertJUnit.assertNotNull("global security policy null", repoSystemConfig.asObjectable().getGlobalSecurityPolicyRef());
    }

    @Test
    public void test050AddGetSyncDescription() throws Exception {
        PrismObjectDefinition accDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismObject<ShadowType> shadow = accDef.instantiate();
        final Date TIME = new Date();
        ShadowType shadowType = shadow.asObjectable();
        shadowType.setName(new PolyStringType("sync desc test"));
        SynchronizationSituationDescriptionType desc = new SynchronizationSituationDescriptionType();
        desc.setChannel("channel");
        desc.setSituation(SynchronizationSituationType.LINKED);
        desc.setTimestamp(XMLGregorianCalendarType.asXMLGregorianCalendar(TIME));
        shadowType.getSynchronizationSituationDescription().add(desc);

        OperationResult result = new OperationResult("sync desc test");
        String oid = repositoryService.addObject(shadowType.asPrismObject(), null, result);

        shadow = repositoryService.getObject(ShadowType.class, oid, null, result);
        shadowType = shadow.asObjectable();
        desc = shadowType.getSynchronizationSituationDescription().get(0);
        AssertJUnit.assertEquals("Times don't match", TIME, XMLGregorianCalendarType.asDate(desc.getTimestamp()));
    }

    @Test
    public void test060AddGetRoleWithResourceRefFilter() throws Exception {
        PrismObject<RoleType> role = prismContext.parseObject(new File("src/test/resources/basic/role-resource-filter.xml"));

        System.out.println("role: " + role.debugDump());
        System.out.println("role: " + role.asObjectable().getInducement().get(0).getConstruction().getResourceRef().getFilter());

        OperationResult result = new OperationResult("sync desc test");
        String oid = repositoryService.addObject(role, null, result);

        role = repositoryService.getObject(RoleType.class, oid, null, result);
        System.out.println("role: " + role.debugDump());
        System.out.println("role: " + role.asObjectable().getInducement().get(0).getConstruction().getResourceRef().getFilter());
    }

    /**
     * creates <iterationToken/> element in shadow
     */
    @Test
    public void test080EmptyIterationToken() throws Exception {
        String token = testIterationToken("");
        AssertJUnit.assertNotNull(token);
        assertEquals(token, "");
    }

    /**
     * doesn't create <iterationToken/> element in shadow
     */
    @Test
    public void test081NullIterationToken() throws Exception {
        String token = testIterationToken(null);
        AssertJUnit.assertNull(token);
    }

    /**
     * creates <iterationToken>some value</iterationToken> element in shadow
     */
    @Test
    public void test082ValueInIterationToken() throws Exception {
        String token = testIterationToken("foo");
        AssertJUnit.assertNotNull(token);
        AssertJUnit.assertEquals(token, "foo");
    }

    private String testIterationToken(String token) throws Exception {
        PrismObjectDefinition accDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismObject<ShadowType> shadow = accDef.instantiate();

        ShadowType shadowType = shadow.asObjectable();
        shadowType.setName(new PolyStringType("testIterationToken"));
        shadowType.setIterationToken(token);

        OperationResult result = new OperationResult("sync desc test");
        final String oid = repositoryService.addObject(shadowType.asPrismObject(), null, result);

        shadow = repositoryService.getObject(ShadowType.class, oid, null, result);
        shadowType = shadow.asObjectable();

        token = shadowType.getIterationToken();
        repositoryService.deleteObject(ShadowType.class, oid, result);

        return token;
    }

    @Test
    public void test085SearchObjectsIterative() throws Exception {
        final List<PrismObject<?>> objects = new ArrayList<>();
        ResultHandler<ObjectType> handler = (object, parentResult) -> {
            objects.add(object);
            return true;
        };

        repositoryService.searchObjectsIterative(
                ObjectType.class, null, handler, null, true, createOperationResult());
        assertThat(objects).isNotEmpty();
    }

    @Test
    public void test090AddGetFullAccountShadow() throws Exception {
        OperationResult result = new OperationResult("testAddAccountShadow");
        File file = new File(FOLDER_BASIC, "account-accountTypeShadow.xml");
        try {
            PrismObject<ShadowType> account = prismContext.parseObject(file);

            // apply appropriate schema
            PrismObject<ResourceType> resource = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));
            ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
            ShadowUtil.applyResourceSchema(account, resourceSchema);

            repositoryService.addObject(account, null, result);

            PrismObject<ShadowType> afterAdd = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
            AssertJUnit.assertNotNull(afterAdd);

        } catch (Exception ex) {
            logger.error("Exception occurred", ex);
            throw ex;
        }
    }

    @Test
    public void test100AddUserWithoutAssignmentIds() throws Exception {
        OperationResult result = new OperationResult("test100AddUserWithoutAssignmentIds");
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user-big.xml"));

        //remove ids from assignment values
        PrismContainer container = user.findContainer(UserType.F_ASSIGNMENT);
        for (PrismContainerValue value : (List<PrismContainerValue>) container.getValues()) {
            value.setId(null);
        }
        final String OID = repositoryService.addObject(user, null, result);
        result.computeStatusIfUnknown();

        //get user
        user = repositoryService.getObject(UserType.class, OID, null, result);
        result.computeStatusIfUnknown();

        PrismContainer pc = user.findContainer(ItemPath.create(UserType.F_ASSIGNMENT, 1,
                AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_OBJECT_STATE));
        AssertJUnit.assertNotNull(pc);
        AssertJUnit.assertNotNull(pc.getValue().getId());

        container = user.findContainer(UserType.F_ASSIGNMENT);
        List<Short> xmlShorts = new ArrayList<>();
        for (PrismContainerValue value : (List<PrismContainerValue>) container.getValues()) {
            AssertJUnit.assertNotNull(value.getId());
            xmlShorts.add(value.getId().shortValue());
        }
        Collections.sort(xmlShorts);

        EntityManager em = open();
        try {
            Query query = em.createNativeQuery("select id from m_assignment where owner_oid=:oid");
            query.setParameter("oid", OID);
            List<Short> dbShorts = new ArrayList<>();
            for (Number n : (List<Number>) query.getResultList()) {
                dbShorts.add(n.shortValue());
            }
            Collections.sort(dbShorts);

            logger.info("assigments ids: expected {} db {}", Arrays.toString(xmlShorts.toArray()),
                    Arrays.toString(dbShorts.toArray()));
            AssertJUnit.assertArrayEquals(xmlShorts.toArray(), dbShorts.toArray());
        } finally {
            close(em);
        }
    }

    @Test
    public void test110AddUserWithDuplicateAssignmentIds() throws Exception {
        OperationResult result = new OperationResult("test110AddUserWithDuplicateAssignmentIds");
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(FOLDER_BASIC, "user-same-ids.xml"));

        //create duplicate ids in assignment values
        PrismContainer container = user.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue> values = (List<PrismContainerValue>) container.getValues();
        values.get(0).setId(999L);      // setting it manually because object with duplicate IDs could not be parsed at all
        values.get(1).setId(999L);
        try {
            repositoryService.addObject(user, null, result);
            throw new AssertionError("Two container values with the same ID were accepted even they shouldn't be");
        } catch (RuntimeException e) {
            // this was expected
        } catch (Exception e) {
            throw new AssertionError("Two container values with the same ID resulted in unexpected exception", e);
        }
    }

    private static final String OID_200 = "70016628-2c41-4a2d-9558-8340014adaab";

    @Test
    public void test200WatcherAddWithOid() throws Exception {
        OperationResult result = new OperationResult("test200WatcherAddWithOid");

        // GIVEN
        UserType user = new UserType(prismContext).name("t200").oid(OID_200);

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID_200);
        repositoryService.addObject(user.asPrismObject(), null, result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 0, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test201WatcherOverwriteWithOidNoVersion() throws Exception {
        OperationResult result = new OperationResult("test201WatcherOverwriteWithOidNoVersion");

        // GIVEN
        UserType user = new UserType(prismContext).name("t200").oid(OID_200);

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID_200);
        repositoryService.addObject(user.asPrismObject(), RepoAddOptions.createOverwrite(), result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 1, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test202WatcherOverwriteWithOidNoVersion2() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        UserType user = new UserType(prismContext).name("t200").oid(OID_200);

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID_200);
        repositoryService.addObject(user.asPrismObject(), RepoAddOptions.createOverwrite(), result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 2, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test203WatcherOverwriteWithOidAndVersion() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        UserType user = new UserType(prismContext).name("t200").oid(OID_200).version("1000");

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID_200);
        repositoryService.addObject(user.asPrismObject(), RepoAddOptions.createOverwrite(), result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 3, watcher.getExpectedVersion()); // the version is ignored when overwriting
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test210WatcherAddWithOidAndVersion() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        final String OID = "f82cdad5-8748-43c1-b20b-7f679fbc1995";
        UserType user = new UserType(prismContext).name("t210").oid(OID).version("443");

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID);
        repositoryService.addObject(user.asPrismObject(), null, result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 443, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test220WatcherAddWithNoOidNorVersion() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        UserType user = new UserType(prismContext).name("t220");

        // WHEN
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(oid);
        watcher.setExpectedVersion(user.getVersion());      // the version should be set by repo here

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 0, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test230WatcherAddWithVersion() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        UserType user = new UserType(prismContext).name("t230").version("2000");

        // WHEN
        String oid = repositoryService.addObject(user.asPrismObject(), null, result);
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(oid);
        watcher.setExpectedVersion(user.getVersion());      // the version should be preserved here

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 2000, watcher.getExpectedVersion());
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test300ContainerIds() throws Exception {
        OperationResult result = createOperationResult();

        // GIVEN
        UserType user = new UserType(prismContext)
                .name("t300")
                .beginAssignment()
                .description("a1")
                .<UserType>end()
                .beginAssignment()
                .description("a2")
                .end();

        // WHEN
        repositoryService.addObject(user.asPrismObject(), null, result);

        // THEN
        System.out.println(user.asPrismObject().debugDump());
        assertNotNull(user.getAssignment().get(0).asPrismContainerValue().getId());
        assertNotNull(user.getAssignment().get(1).asPrismContainerValue().getId());
    }

    @Test
    public void test400AddModifyTask() throws Exception {
        File file = new File(FOLDER_BASIC, "task.xml");
        PrismObject<TaskType> task = PrismTestUtil.parseObject(file);
        TaskType taskType = task.asObjectable();
        AssertJUnit.assertNotNull(taskType.getResult());

        OperationResult result = createOperationResult();
        String oid = repositoryService.addObject(task, null, result);

        EntityManager em = open();
        try {
            RTask rTask = em.createQuery("from RTask t where t.oid=:oid", RTask.class)
                    .setParameter("oid", oid).getSingleResult();
            AssertJUnit.assertNotNull(rTask.getFullResult());
            AssertJUnit.assertEquals(ROperationResultStatus.IN_PROGRESS, rTask.getStatus());

            String serializedForm = RUtil.getSerializedFormFromBytes(rTask.getFullObject());
            PrismObject<TaskType> obj = getPrismContext().parserFor(serializedForm)
                    .parse();
            TaskType objType = obj.asObjectable();
            AssertJUnit.assertNull(objType.getResult());
        } finally {
            close(em);
        }

        task = repositoryService.getObject(TaskType.class, oid, null, result);
        taskType = task.asObjectable();
        AssertJUnit.assertNull(taskType.getResult());
        AssertJUnit.assertEquals(OperationResultStatusType.IN_PROGRESS, taskType.getResultStatus());

        task = repositoryService.getObject(TaskType.class, oid,
                getOperationOptionsBuilder().item(TaskType.F_RESULT).retrieve().build(), result);
        taskType = task.asObjectable();
        AssertJUnit.assertNotNull(taskType.getResult());
        AssertJUnit.assertEquals(OperationResultStatusType.IN_PROGRESS, taskType.getResultStatus());

        OperationResultType res = new OperationResultType();
        res.setOperation("asdf");
        res.setStatus(OperationResultStatusType.FATAL_ERROR);
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_RESULT).replace(res)
                .item(TaskType.F_RESULT_STATUS).replace(res.getStatus())
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, oid, itemDeltas, result);

        task = repositoryService.getObject(TaskType.class, oid, null, result);
        taskType = task.asObjectable();
        AssertJUnit.assertNull(taskType.getResult());
        AssertJUnit.assertEquals(OperationResultStatusType.FATAL_ERROR, taskType.getResultStatus());

        task = repositoryService.getObject(TaskType.class, oid,
                getOperationOptionsBuilder().item(TaskType.F_RESULT).retrieve().build(), result);
        taskType = task.asObjectable();
        AssertJUnit.assertNotNull(taskType.getResult());
        AssertJUnit.assertEquals(OperationResultStatusType.FATAL_ERROR, taskType.getResultStatus());

        OperationResultType r = taskType.getResult();
        AssertJUnit.assertEquals("asdf", r.getOperation());
        AssertJUnit.assertEquals(OperationResultStatusType.FATAL_ERROR, r.getStatus());
    }

    @Test
    public void test990AddResourceWithEmptyConnectorConfiguration() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<ResourceType> prismResource = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class).instantiate();

        PolyStringType name = new PolyStringType();
        name.setOrig("Test Resource");
        name.setNorm("test resource");

        prismResource.asObjectable().setName(name);

        prismResource
                .findOrCreateContainer(ResourceType.F_CONNECTOR_CONFIGURATION)
                .findOrCreateContainer(SchemaConstants.ICF_CONFIGURATION_PROPERTIES)
                .createNewValue();

        System.out.println("Original data before saving: " + prismResource.debugDump());
        String oid = repositoryService.addObject(prismResource, null, result);
        PrismObject<ResourceType> fetchedResource = repositoryService.getObject(ResourceType.class, oid, null, result);
        System.out.println("Original data after saving: " + prismResource.debugDump());
        System.out.println("Fetched data: " + fetchedResource.debugDump());

        AssertJUnit.assertEquals(prismResource, fetchedResource);
    }

    /**
     * MID-3999
     */
    @Test(expectedExceptions = SchemaException.class)
    public void test950AddBinary() throws Exception {
        final File user = new File(FOLDER_BASE, "./get/user-binary.xml");
        addGetCompare(user);
    }
}

