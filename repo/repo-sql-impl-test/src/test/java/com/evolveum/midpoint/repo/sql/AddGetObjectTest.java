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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.hibernate.stat.Statistics;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(
                OBJECTS_FILE);

        long previousCycle = 0;
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            if (i % 500 == 0) {
                LOGGER.info("Previous cycle time {}. Next cycle: {}", new Object[]{
                        (System.currentTimeMillis() - time - previousCycle), i});
                previousCycle = System.currentTimeMillis() - time;
            }

            PrismObject<T> object = (PrismObject<T>) elements.get(i);
            repositoryService.addObject(object, new OperationResult("add performance test"));
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
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Simple Add Get Test");
        long time = System.currentTimeMillis();
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{(i + 1),
                    object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, result));
        }
        LOGGER.info("Time to add objects ({}): {}", new Object[]{elements.size(),
                (System.currentTimeMillis() - time),});

        int count = 0;
        elements = prismContext.getPrismDomProcessor().parseObjects(file);
        for (int i = 0; i < elements.size(); i++) {
            try {
                PrismObject object = elements.get(i);
                object.asObjectable().setOid(oids.get(i));

                Class<? extends ObjectType> clazz = object.getCompileTimeClass();
                PrismObject<? extends ObjectType> newObject = repositoryService.getObject(clazz, oids.get(i), result);
                LOGGER.info("Old\n{}\nnew\n{}", new Object[]{object.debugDump(3), newObject.debugDump(3)});
                checkContainersSize(newObject, object);

                ObjectDelta delta = object.diff(newObject);
                if (delta == null) {
                    continue;
                }

                count += delta.getModifications().size();
                if (delta.getModifications().size() > 0) {
                    if (delta.getModifications().size() == 1) {
                        ItemDelta d = (ItemDelta) delta.getModifications().iterator().next();

                        if (AccountShadowType.F_DEAD.equals(d.getName())) {
                            count -= delta.getModifications().size();
                            continue;
                        }
                    }
                    LOGGER.error(">>> {} Found {} changes for {}\n{}", new Object[]{(i + 1),
                            delta.getModifications().size(), newObject.toString(), delta.debugDump(3)});
                    LOGGER.error("{}", prismContext.getPrismDomProcessor().serializeObjectToString(newObject));
                }
            } catch (Exception ex) {
                LOGGER.error("Exception occurred", ex);
            }
        }

        AssertJUnit.assertEquals("Found changes during add/get test " + count, 0, count);
    }

    private void checkContainerValuesSize(QName parentName, PrismContainerValue newValue, PrismContainerValue oldValue) {
        LOGGER.info("Checking: " + parentName);
        AssertJUnit.assertEquals("Count doesn't match for '" + parentName + "'",
                oldValue.getItems().size(), newValue.getItems().size());

        List<QName> checked = new ArrayList<QName>();

        for (Item item : (List<Item>) newValue.getItems()) {
            if (!(item instanceof PrismContainer)) {
                continue;
            }

            PrismContainer newContainer = (PrismContainer) item;
            PrismContainer oldContainer = oldValue.findContainer(newContainer.getName());
            AssertJUnit.assertNotNull("Container '" + newContainer.getName() + "' doesn't exist.", oldContainer);

            checkContainersSize(newContainer, oldContainer);
            checked.add(oldContainer.getName());
        }

        for (Item item : (List<Item>) oldValue.getItems()) {
            if (!(item instanceof PrismContainer) || checked.contains(item.getName())) {
                continue;
            }

            PrismContainer oldContainer = (PrismContainer) item;
            PrismContainer newContainer = newValue.findContainer(oldContainer.getName());
            checkContainersSize(newContainer, oldContainer);
        }
    }

    private void checkContainersSize(PrismContainer newContainer, PrismContainer oldContainer) {
        AssertJUnit.assertEquals(newContainer.size(), oldContainer.size());

        List<String> checked = new ArrayList<String>();
        List<PrismContainerValue> newValues = newContainer.getValues();
        for (PrismContainerValue value : newValues) {
            PrismContainerValue oldValue = oldContainer.getValue(value.getId());

            checkContainerValuesSize(newContainer.getName(), value, oldValue);
            checked.add(value.getId());
        }

        List<PrismContainerValue> oldValues = oldContainer.getValues();
        for (PrismContainerValue value : oldValues) {
            if (checked.contains(value.getId())) {
                continue;
            }

            PrismContainerValue newValue = newContainer.getValue(value.getId());
            checkContainerValuesSize(newContainer.getName(), newValue, value);
        }
    }

    @Test
    public void addUserWithAssignmentExtension() throws Exception {
        LOGGER.info("===[ addUserWithAssignmentExtension ]===");
        File file = new File(FOLDER_BASIC, "user-assignment-extension.xml");
        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject((PrismObject) elements.get(0), result);

        PrismObject<UserType> fileUser = (PrismObject<UserType>) prismContext.getPrismDomProcessor().parseObjects(file)
                .get(0);
        int id = 1;
        for (AssignmentType assignment : fileUser.asObjectable().getAssignment()) {
            assignment.setId(Integer.toString(id));
            id++;
        }

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, oid, result);

        ObjectDelta<UserType> delta = fileUser.diff(repoUser);
        AssertJUnit.assertNotNull(delta);
        LOGGER.info("delta\n{}", new Object[]{delta.debugDump(3)});
        AssertJUnit.assertTrue(delta.isEmpty());
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
        PrismObject<AccountShadowType> fileAccount = prismContext.parseObject(new File(FOLDER_BASIC, "account-full.xml"));

        // apply appropriate schema
        PrismObject<ResourceType> resource = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        ResourceObjectShadowUtil.applyResourceSchema(fileAccount, resourceSchema);

        OperationResult result = new OperationResult("ADD");
        String oid = repositoryService.addObject(fileAccount, result);

        PrismObject<AccountShadowType> repoAccount = repositoryService.getObject(AccountShadowType.class, oid, result);

        ObjectDelta<AccountShadowType> delta = fileAccount.diff(repoAccount);
        AssertJUnit.assertNotNull(delta);
        LOGGER.info("delta\n{}", new Object[]{delta.debugDump(3)});
        AssertJUnit.assertTrue(delta.isEmpty());
        AccountShadowType repoShadow = repoAccount.asObjectable();
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
        PrismObject<AccountShadowType> filePasswordPolicy = prismContext.parseObject(new File(FOLDER_BASIC, "password-policy.xml"));

        OperationResult result = new OperationResult("ADD");
        String pwdPolicyOid = "00000000-0000-0000-0000-000000000003";
        String oid = repositoryService.addObject(filePasswordPolicy, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(pwdPolicyOid, oid);
        PrismObject<ValuePolicyType> repoPasswordPolicy = repositoryService.getObject(ValuePolicyType.class, oid, result);
        AssertJUnit.assertNotNull(repoPasswordPolicy);

        String systemCongigOid = "00000000-0000-0000-0000-000000000001";
        PrismObject<SystemConfigurationType> fileSystemConfig = prismContext.parseObject(new File(FOLDER_BASIC, "systemConfiguration.xml"));
        LOGGER.info("System config from file: {}", fileSystemConfig.dump());
        oid = repositoryService.addObject(fileSystemConfig, result);
        AssertJUnit.assertNotNull(oid);
        AssertJUnit.assertEquals(systemCongigOid, oid);

        PrismObject<SystemConfigurationType> repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, result);
//		AssertJUnit.assertNotNull("global password policy null", repoSystemConfig.asObjectable().getGlobalPasswordPolicy());
        LOGGER.info("System config from repo: {}", repoSystemConfig.dump());
        AssertJUnit.assertNull("global password policy not null", repoSystemConfig.asObjectable()
                .getGlobalPasswordPolicyRef());

        ReferenceDelta refDelta = ReferenceDelta.createModificationAdd(
                SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, repoSystemConfig.getDefinition(),
                PrismReferenceValue.createFromTarget(repoPasswordPolicy));
        List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
        refDeltas.add(refDelta);
        repositoryService.modifyObject(SystemConfigurationType.class, systemCongigOid, refDeltas, result);
        repoSystemConfig = repositoryService.getObject(SystemConfigurationType.class, systemCongigOid, result);
        LOGGER.info("system config after modify: {}", repoSystemConfig.dump());
        AssertJUnit.assertNotNull("global password policy null", repoSystemConfig.asObjectable()
                .getGlobalPasswordPolicyRef());
        AssertJUnit.assertNull("default user template not null", repoSystemConfig.asObjectable()
                .getDefaultUserTemplateRef());

        AssertJUnit.assertNotNull("org root ref is null.", repoSystemConfig.asObjectable().getOrgRootRef());
        AssertJUnit.assertEquals(2, repoSystemConfig.asObjectable().getOrgRootRef().size());
        List<ObjectReferenceType> orgRootRefs = repoSystemConfig.asObjectable().getOrgRootRef();
        String[] refs = {"10000000-0000-0000-0000-000000000003", "20000000-0000-0000-0000-000000000003"};
        for (String ref : refs) {
            boolean found = false;
            for (ObjectReferenceType orgRootRef : orgRootRefs) {
                if (ref.equals(orgRootRef.getOid())) {
                    found = true;
                    break;
                }
            }
            AssertJUnit.assertTrue(ref  + " was not found in org. root refs in system configuration.",found);
        }
    }


}
