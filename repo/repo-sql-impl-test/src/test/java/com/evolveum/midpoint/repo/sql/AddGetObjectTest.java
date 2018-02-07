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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddGetObjectTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(AddGetObjectTest.class);

    @Test(enabled = false)
    public <T extends ObjectType> void perfTest() throws Exception {
        Statistics stats = getFactory().getStatistics();
        stats.setStatisticsEnabled(true);

        final File OBJECTS_FILE = new File("./src/test/resources/10k-users.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(OBJECTS_FILE).parseObjects();

        long previousCycle = 0;
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            if (i % 500 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{
                        (System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            PrismObject<T> object = (PrismObject<T>) elements.get(i);
            repositoryService.addObject(object, null, new OperationResult("add performance test"));
        }
        LOGGER.info("Time to add objects ({}): {}",
                new Object[]{elements.size(), (System.currentTimeMillis() - time)});

        stats.logSummary();
    }

    @Test
    public void addSameName() throws Exception {
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
    public void addGetDSEESyncDoubleTest() throws Exception {
        final File OBJECTS_FILE = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!OBJECTS_FILE.exists()) {
            LOGGER.warn("skipping addGetDSEESyncDoubleTest, file {} not found.",
                    new Object[]{OBJECTS_FILE.getPath()});
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
    public void simpleAddGetTest() throws Exception {
        LOGGER.info("===[ simpleAddGetTest ]===");
        final File OBJECTS_FILE = new File(FOLDER_BASIC, "objects.xml");
        addGetCompare(OBJECTS_FILE);
    }

    private void addGetCompare(File file) throws Exception {
        List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(file).parseObjects();
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{(i + 1),
                    object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, null, result));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(),
                (System.currentTimeMillis() - time),});

        int count = 0;
        elements = prismContext.parserFor(file).parseObjects();
        for (int i = 0; i < elements.size(); i++) {
        	PrismObject object = elements.get(i);
            try {
                object.asObjectable().setOid(oids.get(i));

                Class<? extends ObjectType> clazz = object.getCompileTimeClass();

                Collection o = null;
                if (UserType.class.equals(clazz)) {
                    o = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
                            GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
                } else if (LookupTableType.class.equals(clazz)) {
                    o = SelectorOptions.createCollection(LookupTableType.F_ROW,
                            GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
                } else if (AccessCertificationCampaignType.class.equals(clazz)) {
                    o = SelectorOptions.createCollection(AccessCertificationCampaignType.F_CASE,
                            GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
                }
                PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), o, result);

                LOGGER.info("AFTER READ: {}\nOld\n{}\nnew\n{}", object, object.debugDump(3), newObject.debugDump(3));
                checkContainersSize(newObject, object);
                System.out.println("OLD: " + object.findProperty(ObjectType.F_NAME).getValue());
                System.out.println("NEW: " + newObject.findProperty(ObjectType.F_NAME).getValue());

                ObjectDelta delta = object.diff(newObject);
                if (delta == null) {
                    continue;
                }

                count += delta.getModifications().size();
                if (delta.getModifications().size() > 0) {
                    if (delta.getModifications().size() == 1) {
                        ItemDelta d = (ItemDelta) delta.getModifications().iterator().next();

                        if (ShadowType.F_DEAD.equals(d.getElementName())) {
                            count -= delta.getModifications().size();
                            continue;
                        }
                    }
                    LOGGER.error(">>> {} Found {} changes for {}\n{}", new Object[]{(i + 1),
                            delta.getModifications().size(), newObject.toString(), delta.debugDump(3)});
                    ItemDelta id = (ItemDelta) delta.getModifications().iterator().next();
                    if (id.isReplace()) {
                        LOGGER.debug("{}", id.getValuesToReplace().iterator().next());
                    }
                    LOGGER.error("{}", prismContext.serializeObjectToString(newObject, PrismContext.LANG_XML));
                }
            } catch (Throwable ex) {
                LOGGER.error("Exception occurred for {}", object, ex);
                throw new RuntimeException("Exception during processing of "+object+": "+ex.getMessage(), ex);
            }
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }

    private Integer size(PrismContainerValue value) {
        if (value == null) {
            return null;
        }

        return value.getItems() != null ? value.getItems().size() : 0;
    }

    private void checkContainerValuesSize(QName parentName, PrismContainerValue newValue, PrismContainerValue oldValue) {
        LOGGER.info("Checking: " + parentName);
        AssertJUnit.assertEquals("Count doesn't match for '" + parentName + "' id="+newValue.getId(), size(oldValue), size(newValue));

        List<QName> checked = new ArrayList<QName>();

        for (Item item : (List<Item>) newValue.getItems()) {
            if (!(item instanceof PrismContainer)) {
                continue;
            }

            PrismContainer newContainer = (PrismContainer) item;
            PrismContainer oldContainer = oldValue.findContainer(newContainer.getElementName());
            AssertJUnit.assertNotNull("Container '" + newContainer.getElementName() + "' doesn't exist.", oldContainer);

            checkContainersSize(newContainer, oldContainer);
            checked.add(oldContainer.getElementName());
        }

        for (Item item : (List<Item>) oldValue.getItems()) {
            if (!(item instanceof PrismContainer) || checked.contains(item.getElementName())) {
                continue;
            }

            PrismContainer oldContainer = (PrismContainer) item;
            PrismContainer newContainer = newValue.findContainer(oldContainer.getElementName());
            checkContainersSize(newContainer, oldContainer);
        }
    }

    private void checkContainersSize(PrismContainer newContainer, PrismContainer oldContainer) {
        LOGGER.info("checkContainersSize {} new {}  old {}",
                newContainer.getElementName(), newContainer.size(), oldContainer.size());
        AssertJUnit.assertEquals(newContainer.size(), oldContainer.size());

        PrismContainerDefinition def = oldContainer.getDefinition();
        if (def != null && def.isMultiValue()) {
        	// Comparison item-by-item is not reliable
        	return;
        }
        List<Long> checked = new ArrayList<Long>();
        List<PrismContainerValue> newValues = newContainer.getValues();
        for (PrismContainerValue value : newValues) {
            PrismContainerValue oldValue = oldContainer.getValue(value.getId());

            checkContainerValuesSize(newContainer.getElementName(), value, oldValue);
            checked.add(value.getId());
        }

        List<PrismContainerValue> oldValues = oldContainer.getValues();
        for (PrismContainerValue value : oldValues) {
            if (checked.contains(value.getId())) {
                continue;
            }

            PrismContainerValue newValue = newContainer.getValue(value.getId());
            checkContainerValuesSize(newContainer.getElementName(), newValue, value);
        }
    }

    @Test
    public void addUserWithAssignmentExtension() throws Exception {
        LOGGER.info("===[ addUserWithAssignmentExtension ]===");
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
        LOGGER.info("delta\n{}", delta.debugDump(3));
        assertTrue(delta.isEmpty());
    }

    /**
     * Attempt to store full account in the repo and then get it out again. The
     * potential problem is that there are attributes that do not have a fixed
     * (static) definition.
     */
    @Test
    public void addGetFullAccount() throws Exception {
        LOGGER.info("===[ addGetFullAccount ]===");
        File file = new File(FOLDER_BASIC, "account-full.xml");
        PrismObject<ShadowType> fileAccount = prismContext.parseObject(new File(FOLDER_BASIC, "account-full.xml"));

        // apply appropriate schema
        PrismObject<ResourceType> resource = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));
        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ShadowUtil.applyResourceSchema(fileAccount, resourceSchema);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject(fileAccount, null, result);

        PrismObject<ShadowType> repoAccount = repositoryService.getObject(ShadowType.class, oid, null, result);

        ObjectDelta<ShadowType> delta = fileAccount.diff(repoAccount);
        AssertJUnit.assertNotNull(delta);
        LOGGER.info("delta\n{}", new Object[]{delta.debugDump(3)});
        assertTrue(delta.isEmpty());
        ShadowType repoShadow = repoAccount.asObjectable();
        AssertJUnit.assertNotNull(repoShadow.getSynchronizationSituation());
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, repoShadow.getSynchronizationSituation());
        AssertJUnit.assertNotNull(repoShadow.getSynchronizationSituationDescription());
        AssertJUnit.assertEquals(1, repoShadow.getSynchronizationSituationDescription().size());
        AssertJUnit.assertEquals(SynchronizationSituationType.LINKED, repoShadow.getSynchronizationSituationDescription().get(0).getSituation());
        AssertJUnit.assertEquals("syncChannel", repoShadow.getSynchronizationSituationDescription().get(0).getChannel());
    }

    @Test
    public void addGetSystemConfigFile() throws Exception {
        LOGGER.info("===[ addGetPasswordPolicy ]===");
        File file = new File(FOLDER_BASIC, "password-policy.xml");
        PrismObject<ValuePolicyType> filePasswordPolicy = prismContext.parseObject(new File(FOLDER_BASIC, "password-policy.xml"));

        OperationResult result = new OperationResult("ADD");
        String pwdPolicyOid = "00000000-0000-0000-0000-000000000003";
        String oid = repositoryService.addObject(filePasswordPolicy, null, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(pwdPolicyOid, oid);
        PrismObject<ValuePolicyType> repoPasswordPolicy = repositoryService.getObject(ValuePolicyType.class, oid, null, result);
        AssertJUnit.assertNotNull(repoPasswordPolicy);

        String systemCongigOid = "00000000-0000-0000-0000-000000000001";
        PrismObject<SystemConfigurationType> fileSystemConfig = prismContext.parseObject(new File(FOLDER_BASIC, "systemConfiguration.xml"));
        LOGGER.info("System config from file: {}", fileSystemConfig.debugDump());
        oid = repositoryService.addObject(fileSystemConfig, null, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(systemCongigOid, oid);

        PrismObject<SystemConfigurationType> repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, null, result);
//		AssertJUnit.assertNotNull("global password policy null", repoSystemConfig.asObjectable().getGlobalPasswordPolicy());
        LOGGER.info("System config from repo: {}", repoSystemConfig.debugDump());
        AssertJUnit.assertNull("global password policy not null", repoSystemConfig.asObjectable()
                .getGlobalPasswordPolicyRef());

        ReferenceDelta refDelta = ReferenceDelta.createModificationAdd(
                SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, repoSystemConfig.getDefinition(),
                PrismReferenceValue.createFromTarget(repoPasswordPolicy));
        List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
        refDeltas.add(refDelta);
        repositoryService.modifyObject(SystemConfigurationType.class, systemCongigOid, refDeltas, result);
        repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, null, result);
        LOGGER.info("system config after modify: {}", repoSystemConfig.debugDump());
        AssertJUnit.assertNotNull("global password policy null", repoSystemConfig.asObjectable()
                .getGlobalPasswordPolicyRef());
        AssertJUnit.assertNull("default user template not null", repoSystemConfig.asObjectable()
                .getDefaultUserTemplateRef());
    }

    @Test
    public void addGetSyncDescription() throws Exception {
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
    public void addGetRoleWithResourceRefFilter() throws Exception{
    	PrismObject<RoleType> role = prismContext.parseObject(new File("src/test/resources/basic/role-resource-filter.xml"));

    	System.out.println("role: " + role.debugDump());
    	System.out.println("role: " + role.asObjectable().getInducement().get(0).getConstruction().getResourceRef().getFilter());

        OperationResult result = new OperationResult("sync desc test");
        String oid = repositoryService.addObject(role, null, result);

        role = repositoryService.getObject(RoleType.class, oid, null, result);
        RoleType roleType = role.asObjectable();
        System.out.println("role: " + role.debugDump());
        System.out.println("role: " + role.asObjectable().getInducement().get(0).getConstruction().getResourceRef().getFilter());
//        desc = roleType.getSynchronizationSituationDescription().get(0);
//        AssertJUnit.assertEquals("Times don't match", TIME, XMLGregorianCalendarType.asDate(desc.getTimestamp()));
    }

    /**
     * creates <iterationToken/> element in shadow
     */
    @Test
    public void emtpyIterationToken() throws Exception {
        String token = testIterationToken("");
        AssertJUnit.assertNotNull(token);
        assertTrue(token.equals(""));
    }

    /**
     * doesn't create <iterationToken/> element in shadow
     */
    @Test
    public void nullIterationToken() throws Exception {
        String token = testIterationToken(null);
        AssertJUnit.assertNull(token);
    }

    /**
     * creates <iterationToken>some value</iterationToken> element in shadow
     */
    @Test
    public void valueInIterationToken() throws Exception {
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

//    @Test(enabled = false)
//    public void deltaOperationSerializationPerformanceTest() throws Exception {
//        List<PrismObject<? extends Objectable>> elements =
//                prismContext.processorFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
//
//        //get user from objects.xml
//        ObjectDelta delta = ObjectDelta.createAddDelta(elements.get(0));
//
//        final int COUNT = 10000;
//        //first conversion option
//        System.out.println(DeltaConvertor.toObjectDeltaTypeXml(delta));
//        //second conversion option
//        //System.out.println("\n" + toRepo(DeltaConvertor.toObjectDeltaType(delta), prismContext));
//
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < COUNT; i++) {
//            String xml = DeltaConvertor.toObjectDeltaTypeXml(delta);
//        }
//        time = System.currentTimeMillis() - time;
//        System.out.println(">>> " + time);
//
//        time = System.currentTimeMillis();
//        for (int i = 0; i < COUNT; i++) {
//            ObjectDeltaType type = DeltaConvertor.toObjectDeltaType(delta);
//            String xml = toRepo(type, prismContext);
//        }
//        time = System.currentTimeMillis() - time;
//        System.out.println(">>> " + time);
//    }


    @Test
    public void test() throws Exception {
        OperationResult result = new OperationResult("asdf");
        final List<PrismObject> objects = new ArrayList<PrismObject>();
        ResultHandler<ObjectType> handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> object, OperationResult parentResult) {
                objects.add(object);
                return true;
            }
        };

        repositoryService.searchObjectsIterative(ObjectType.class, null, handler, null, false, result);
        assertTrue(!objects.isEmpty());
    }

    @Test
    private void addGetFullAccountShadow() throws Exception {
        LOGGER.info("===[ simpleAddAccountShadowTest ]===");
        OperationResult result = new OperationResult("testAddAccountShadow");
        File file = new File(FOLDER_BASIC, "account-accountTypeShadow.xml");
        try {
            PrismObject<ShadowType> account = prismContext.parseObject(file);

            // apply appropriate schema
            PrismObject<ResourceType> resource = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));
            ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
            ShadowUtil.applyResourceSchema(account, resourceSchema);

            repositoryService.addObject(account, null, result);

            PrismObject<ShadowType> afterAdd = repositoryService.getObject(ShadowType.class, account.getOid(), null, result);
            AssertJUnit.assertNotNull(afterAdd);

        } catch (Exception ex) {
            LOGGER.error("Exception occurred", ex);
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

        PrismContainer pc = user.findContainer(new ItemPath(UserType.F_ASSIGNMENT, 1,
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

        Session session = open();
        try {
            Query query = session.createNativeQuery("select id from m_assignment where owner_oid=:oid");
            query.setParameter("oid", OID);
            List<Short> dbShorts = new ArrayList<>();
            for (Number n : (List<Number>) query.list()) {
                dbShorts.add(n.shortValue());
            }
            Collections.sort(dbShorts);

            LOGGER.info("assigments ids: expected {} db {}", Arrays.toString(xmlShorts.toArray()),
                    Arrays.toString(dbShorts.toArray()));
            AssertJUnit.assertArrayEquals(xmlShorts.toArray(), dbShorts.toArray());
        } finally {
            close(session);
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
            String OID = repositoryService.addObject(user, null, result);
            throw new AssertionError("Two container values with the same ID were accepted even they shouldn't be");
        } catch (RuntimeException e) {
            // this was expected
        } catch (Exception e) {
            throw new AssertionError("Two container values with the same ID resulted in unexpected exception", e);
        }
    }

	private final String OID_200 = "70016628-2c41-4a2d-9558-8340014adaab";

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
        OperationResult result = new OperationResult("test202WatcherOverwriteWithOidNoVersion2");

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
        OperationResult result = new OperationResult("test203WatcherOverwriteWithOidAndVersion");

        // GIVEN
        UserType user = new UserType(prismContext).name("t200").oid(OID_200).version("1000");

        // WHEN
        ConflictWatcherImpl watcher = (ConflictWatcherImpl) repositoryService.createAndRegisterConflictWatcher(OID_200);
        repositoryService.addObject(user.asPrismObject(), RepoAddOptions.createOverwrite(), result);

        // THEN
        assertTrue("watcher is not initialized", watcher.isInitialized());
        assertFalse("watcher is marked as deleted", watcher.isObjectDeleted());
        assertEquals("expectedVersion is wrong", 3, watcher.getExpectedVersion());      // the version is ignored when overwriting
        boolean hasConflict = repositoryService.hasConflict(watcher, result);
        assertFalse("false conflict reported for " + watcher, hasConflict);
    }

    @Test
    public void test210WatcherAddWithOidAndVersion() throws Exception {
        OperationResult result = new OperationResult("test210WatcherAddWithOidAndVersion");

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
        OperationResult result = new OperationResult("test220WatcherAddWithNoOidNorVersion");

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
        OperationResult result = new OperationResult("test230WatcherAddWithVersion");

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
        OperationResult result = new OperationResult("test300ContainerIds");

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
    public void test990AddResourceWithEmptyConnectorConfiguration() throws Exception {
        OperationResult result = new OperationResult("test990AddResourceWithEmptyConnectorConfiguration");

        PrismObject<ResourceType> prismResource = PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class).instantiate();

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

