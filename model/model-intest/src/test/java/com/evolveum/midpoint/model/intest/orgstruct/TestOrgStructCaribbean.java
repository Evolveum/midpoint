/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.intest.orgstruct;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestOrgStructCaribbean extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/orgstruct");
    
    protected static final File ORG_CARIBBEAN_FILE = new File(TEST_DIR, "org-caribbean.xml");
	protected static final String ORG_CARIBBEAN_TOP_OID = "00000000-8888-6666-0000-c00000000001";
	protected static final String ORG_CARIBBEAN_THE_CROWN_OID = "00000000-8888-6666-0000-c00000000002";
	protected static final String ORG_CARIBBEAN_JAMAICA_OID = "00000000-8888-6666-0000-c00000000003";
	protected static final String ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID = "00000000-8888-6666-0000-c00000000004";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        //DebugUtil.setDetailedDebugDump(true);
    }

    
    /**
     * MID-3448
     */
    @Test
    public void test100AddOrgCaribbean() throws Exception {
        final String TEST_NAME = "test100AddOrgCaribbean";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        repoAddObjectsFromFile(ORG_CARIBBEAN_FILE, OrgType.class, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Moneky Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasNoOrg(orgDoT, ORG_GOVERNOR_OFFICE_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test102RecomputeJamaica() throws Exception {
        final String TEST_NAME = "test102RecomputeJamaica";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);        
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test103ReconcileJamaica() throws Exception {
        final String TEST_NAME = "test103ReconcileJamaica";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_CARIBBEAN_JAMAICA_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test104RecomputeGovernor() throws Exception {
        final String TEST_NAME = "test104RecomputeGovernor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_GOVERNOR_OFFICE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test105ReconcileGovernor() throws Exception {
        final String TEST_NAME = "test105ReconcileGovernor";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_GOVERNOR_OFFICE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test106RecomputeDoT() throws Exception {
        final String TEST_NAME = "test106RecomputeDoT";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.recompute(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }
    
    /**
     * MID-3448
     */
    @Test
    public void test107ReconcileDoT() throws Exception {
        final String TEST_NAME = "test107ReconcileDoT";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestOrgStructCaribbean.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        reconcileOrg(ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgJamaica = getObject(OrgType.class, ORG_CARIBBEAN_JAMAICA_OID);
        display("Jamaica", orgJamaica);
        assertAssignedOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasOrgs(orgJamaica, ORG_CARIBBEAN_THE_CROWN_OID);
        assertHasNoOrg(orgJamaica, ORG_GOVERNOR_OFFICE_OID);
        
        PrismObject<OrgType> orgMonkeyGovernor = getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID);
        display("Monkey Governor", orgMonkeyGovernor);
        assertHasNoOrg(orgMonkeyGovernor, ORG_CARIBBEAN_JAMAICA_OID);
        
        PrismObject<OrgType> orgDoT = getObject(OrgType.class, ORG_CARIBBEAN_DEPARTMENT_OF_THINGS_OID);
        display("Department of Things", orgDoT);
        assertAssignedOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID);
        assertHasOrgs(orgDoT, ORG_CARIBBEAN_JAMAICA_OID, ORG_GOVERNOR_OFFICE_OID);
    }
   

}
