/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValuePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportRecon extends AbstractModelIntegrationTest {
		
	public TestImportRecon() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		super.initSystem(initResult);
		
		// Create an account that midPoint does not know about yet
		addDummyAccount(dummyResource, USER_RAPP_USERNAME, "Rapp Scallion", "Scabb Island");
		
		// And a user that will be correlated to that account
		addObjectFromFile(USER_RAPP_FILENAME, UserType.class, initResult);
	}



	@Test
    public void test100ImportFromResourceDummy() throws Exception {
		final String TEST_NAME = "test100ImportFromResourceDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TestImportRecon.class.getName() + ".test100ImportFromResourceDummy");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        // Preconditions
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before import", users);
        assertEquals("Unexpected number of users", 5, users.size());
        
        PrismObject<UserType> rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp has dummy account but it is not linked
        assertAccounts(rapp, 0);
        
		// WHEN
        displayWhen(TEST_NAME);
        modelService.importAccountsFromResource(RESOURCE_DUMMY_OID, new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), task, result);
		
        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        IntegrationTestTools.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task);
        
        displayThen(TEST_NAME);
        
        users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);
        
        assertEquals("Unexpected number of users", 6, users.size());
        
        PrismObject<UserType> admin = getUser(USER_ADMINISTRATOR_OID);
        assertNotNull("No admin", admin);
        assertAccounts(admin, 0);
        
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        assertNotNull("No jack", jack);
        assertAccounts(jack, 0);
        
        PrismObject<UserType> barbossa = getUser(USER_BARBOSSA_OID);
        assertNotNull("No barbossa", barbossa);
        assertAccounts(barbossa, 1);
        // Barbossa had opendj account before
        assertAccount(barbossa, RESOURCE_OPENDJ_OID);
        
        PrismObject<UserType> guybrush = getUser(USER_GUYBRUSH_OID);
        assertNotNull("No guybrush", guybrush);
        assertAccounts(guybrush, 1);
        assertAccount(guybrush, RESOURCE_DUMMY_OID);
        
        rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp account should be linked
        assertAccounts(rapp, 1);
        assertAccount(rapp, RESOURCE_DUMMY_OID);
        
        PrismObject<UserType> herman = findUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertNotNull("No herman", herman);
        assertAccounts(herman, 1);
        assertAccount(herman, RESOURCE_DUMMY_OID);
        
        // These are protected accounts, they should not be imported
        PrismObject<UserType> daviejones = findUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNull("Jones sneaked in", daviejones);
        PrismObject<UserType> calypso = findUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertNull("Calypso sneaked in", calypso);
	}
		

}
