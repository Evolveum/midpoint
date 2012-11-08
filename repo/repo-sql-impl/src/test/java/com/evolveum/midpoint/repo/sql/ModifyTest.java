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
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RTask;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../application-context-sql-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})
public class ModifyTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyTest.class);
    private static final File TEST_DIR = new File("src/test/resources/modify");

    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;
    @Autowired
    SessionFactory factory;

    @Test(expectedExceptions = SystemException.class, enabled = false)
    public void test010ModifyWithExistingName() throws Exception {
        LOGGER.info("=== [ modifyWithExistingName ] ===");

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
        LOGGER.info("=== [ modifyNotExistingUser ] ===");

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
    public void testModifyUserAddRole() throws Exception{
    	 LOGGER.info("=== [ testModifyUserAddRole ] ===");
    	OperationResult parentResult = new OperationResult("Modify user -> add roles");
    	String userToModifyOid = "f65963e3-9d47-4b18-aaf3-bfc98bdfa000";
    	
    	PrismObject<ResourceType> csvResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR+"/resource-csv.xml"));
    	repositoryService.addObject(csvResource, parentResult);
    	
    	PrismObject<ResourceType> openDjResource = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR+"/resource-opendj.xml"));
    	repositoryService.addObject(openDjResource, parentResult);
    	
    	PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR+"/user.xml"));
    	repositoryService.addObject(user, parentResult);
    	
    	PrismObject<RoleType> roleCsv = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR+"/role-csv.xml"));
    	repositoryService.addObject(roleCsv, parentResult);
    	
    	String ldapRoleOid = "12345678-d34d-b33f-f00d-987987987988";
    	PrismObject<RoleType> roleLdap = prismContext.getPrismDomProcessor().parseObject(new File(TEST_DIR+"/role-ldap.xml"));
    	repositoryService.addObject(roleLdap, parentResult);
    	
    	RoleType ldapRole = repositoryService.getObject(RoleType.class, ldapRoleOid, parentResult).asObjectable();
    	AssertJUnit.assertEquals("Expected that the role has one approver.", 1, ldapRole.getApproverRef().size());
    	AssertJUnit.assertEquals("Actual approved not equals to expected one.", userToModifyOid, ldapRole.getApproverRef().get(0).getOid());
    	
    	ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(TEST_DIR+"/modify-user-add-roles.xml"),
				ObjectModificationType.class);
    	
    	
    	ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);
    	
    	
    	
    	repositoryService.modifyObject(UserType.class, userToModifyOid, delta.getModifications(), parentResult);
    	
    	UserType modifiedUser = repositoryService.getObject(UserType.class, userToModifyOid, parentResult).asObjectable();
    	AssertJUnit.assertEquals("assertion failed", 3, modifiedUser.getAssignment().size());
    	
    	
    	
    	
    	
    }
}
