/**
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.model.test;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.notifiers.DummyNotifier;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.schema.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * Abstract framework for an integration test that is placed on top of a model API.
 * This provides complete environment that the test should need, e.g model service instance, repository, provisionig,
 * dummy auditing, etc. It also implements lots of useful methods to make writing the tests easier.
 *  
 * @author Radovan Semancik
 *
 */
public abstract class AbstractModelIntegrationTest extends AbstractIntegrationTest {
		
	protected static final int DEFAULT_TASK_WAIT_TIMEOUT = 25000;
	protected static final long DEFAULT_TASK_SLEEP_TIME = 200;
			
	protected static final String CONNECTOR_DUMMY_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";
	
	protected static final ItemPath ACTIVATION_ENABLED_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_ENABLED);
	
	@Autowired(required = true)
	protected ModelService modelService;
	
	@Autowired(required = true)
	protected ModelInteractionService modelInteractionService;
	
	@Autowired(required = true)
	protected ModelDiagnosticService modelDiagnosticService;
	
	@Autowired(required = true)
	protected ModelPortType modelWeb;
	
	@Autowired(required = true)
	protected RepositoryService repositoryService;
	
	@Autowired(required = true)
	protected ProvisioningService provisioningService;
	
	@Autowired(required = true)
	protected HookRegistry hookRegistry;
	
	@Autowired(required = true)
	protected PrismContext prismContext;

    @Autowired(required = false)  // dummyNotifier is currently used only in model-intest,
                                  // but AbstractModelIntegrationTest is used in other modules as well.
                                  // So until all POMs are modified we keep required=false here.
    protected DummyNotifier dummyNotifier;
	
	protected DummyAuditService dummyAuditService;
	
	protected boolean verbose = false; 
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
			
	public AbstractModelIntegrationTest() {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		dummyAuditService = DummyAuditService.getInstance();
	}

	protected void importObjectFromFile(String filename) throws FileNotFoundException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".importObjectFromFile");
		importObjectFromFile(filename, result);
		result.computeStatus();
		assertSuccess(result);
	}

	protected void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
	}
	
	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, String filename, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException {
		importObjectFromFile(filename, result);
		OperationResult importResult = result.getLastSubresult();
		assertSuccess("Import of "+filename+" has failed", importResult);
		return modelService.getObject(type, oid, null, task, result);
	}
	    
    /**
     * This is not the real thing. It is just for the tests. 
     */
    protected void applyResourceSchema(AccountShadowType accountType, ResourceType resourceType) throws SchemaException {
    	IntegrationTestTools.applyResourceSchema(accountType, resourceType, prismContext);
    }
    
    protected void assertUsers(int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".assertUsers");
        OperationResult result = task.getResult();
    	List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        if (verbose) display("Users", users);
        assertEquals("Unexpected number of users", expectedNumberOfUsers, users.size());
    }
		
	protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName) {
		assertEquals("Wrong "+user+" OID (prism)", oid, user.getOid());
		UserType userType = user.asObjectable();
		assertEquals("Wrong "+user+" OID (jaxb)", oid, userType.getOid());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" name", name, userType.getName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" fullName", fullName, userType.getFullName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" givenName", givenName, userType.getGivenName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" familyName", familyName, userType.getFamilyName());
	}
	
	protected void assertUserProperty(String userOid, QName propertyName, Object... expectedPropValues) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getObject");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertUserProperty(user, propertyName, expectedPropValues);
	}
	
	protected void assertUserProperty(PrismObject<UserType> user, QName propertyName, Object... expectedPropValues) {
		PrismProperty<Object> property = user.findProperty(propertyName);
		assert property != null : "No property "+propertyName+" in "+user;  
		PrismAsserts.assertPropertyValue(property, expectedPropValues);
	}
	
	protected void assertLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertLinked(user, accountOid);
	}
	
	protected void assertLinked(PrismObject<UserType> user, PrismObject<AccountShadowType> account) throws ObjectNotFoundException, SchemaException {
		assertLinked(user, account.getOid());
	}
	
	protected void assertLinked(PrismObject<UserType> user, String accountOid) throws ObjectNotFoundException, SchemaException {
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		boolean found = false; 
		for (PrismReferenceValue val: accountRef.getValues()) {
			if (val.getOid().equals(accountOid)) {
				found = true;
			}
		}
		assertTrue("User "+user+" is not linked to account "+accountOid, found);
	}
	
	protected void assertNoLinkedAccount(PrismObject<UserType> user) {
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		if (accountRef == null) {
			return;
		}
		assert accountRef.isEmpty() : "Expected that "+user+" has no linked account but it has "+accountRef.size()+" linked accounts: "
			+ accountRef.getValues();
	}
	
	protected void assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		String accountOid = getUserAccountRef(user, resourceOid);
		assertNotNull("User "+user+" has no account on resource "+resourceOid, accountOid);
	}
	
	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAccounts(user, numAccounts);
	}
	
	protected void assertAccounts(PrismObject<UserType> user, int numAccounts) throws ObjectNotFoundException, SchemaException {
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		if (accountRef == null) {
			assert numAccounts == 0 : "Expected "+numAccounts+" but "+user+" has no accountRef";
			return;
		}
		assertEquals("Wrong number of accounts linked to "+user, numAccounts, accountRef.size());
	}
	
	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, QName propertyName, Object... newRealValue) {
		return createModifyUserReplaceDelta(userOid, new ItemPath(propertyName), newRealValue);
	}
	
	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationReplaceProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}
	
	protected ObjectDelta<UserType> createModifyUserAddDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}
	
	protected ObjectDelta<UserType> createModifyUserAddAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException {
		PrismObject<AccountShadowType> account = getAccountShadowDefinition().instantiate();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		account.asObjectable().setResourceRef(resourceRef);
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		account.asObjectable().setObjectClass(refinedSchema.getDefaultAccountDefinition().getObjectClassDefinition().getTypeName());
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		
		return userDelta;
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowEmptyDelta(String accountOid) {
		return ObjectDelta.createEmptyModifyDelta(AccountShadowType.class, accountOid, prismContext);
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid, 
			PrismObject<ResourceType> resource, String attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceAttributeDelta(accountOid, resource, getAttributeQName(resource, attributeName), newRealValue);
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid, 
			PrismObject<ResourceType> resource, QName attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceDelta(accountOid, resource, new ItemPath(AccountShadowType.F_ATTRIBUTES, attributeName), newRealValue);
	}
	
	protected ObjectDelta<AccountShadowType> createModifyAccountShadowReplaceDelta(String accountOid, PrismObject<ResourceType> resource, ItemPath itemPath, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (AccountShadowType.F_ATTRIBUTES.equals(ItemPath.getName(itemPath.first()))) {
			PropertyDelta<?> attributeDelta = createAttributeReplaceDelta(resource, ((NameItemPathSegment)itemPath.last()).getName(), newRealValue);
			ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModifyDelta(accountOid, attributeDelta, AccountShadowType.class, prismContext);
			return accountDelta;
		} else {
			ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(
					AccountShadowType.class, accountOid, itemPath, prismContext, newRealValue);
			return accountDelta;
		}
	}
	
	protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeReplaceDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}
	
	protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationReplaceProperty(new ItemPath(AccountShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}
	
	protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeAddDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}
	
	protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationAddProperty(new ItemPath(AccountShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}
	
	protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeDeleteDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}
	
	protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationDeleteProperty(new ItemPath(AccountShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}
	
	protected ResourceAttributeDefinition getAttributeDefinition(PrismObject<ResourceType> resource, QName attributeName) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		if (refinedSchema == null) {
			throw new SchemaException("No refined schema for "+resource);
		}
		RefinedAccountDefinition accountDefinition = refinedSchema.getDefaultAccountDefinition();
		return accountDefinition.findAttributeDefinition(attributeName);
	}

	protected ObjectDelta<AccountShadowType> createModifyAccountShadowAddDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(AccountShadowType.class, accountOid, propertyName, prismContext, newRealValue);
	}
	
	protected void modifyUserReplace(String userOid, QName propertyName, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(userOid, new ItemPath(propertyName), task, result, newRealValue);
	}
	
	protected void modifyUserReplace(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(userOid, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void modifyAccountShadowReplace(String accountOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		PrismObject<AccountShadowType> shadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
		String resourceOid = shadow.asObjectable().getResourceRef().getOid();
		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result);
		ObjectDelta<AccountShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountOid, resource, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void assignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, true, result);
	}
	
	protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
	SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, false, result);
	}
	
	protected void assignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		assignOrg(userOid, orgOid, null, task, result);
	}
	
	protected void assignOrg(String userOid, String orgOid, QName relation, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, true, result);
	}

	protected void unassignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		unassignOrg(userOid, orgOid, null, task, result);
	}
	
	protected void unassignOrg(String userOid, String orgOid, QName relation, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, false, result);
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task, boolean add, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, relation, add);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);		
	}
	
	/**
	 * Executes assignment replace delta with empty values.
	 */
	protected void unassignAll(String userOid, Task task, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceContainer(UserType.class, userOid, 
				UserType.F_ASSIGNMENT, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);		
	}
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, QName relation, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(getUserDefinition(), UserType.F_ASSIGNMENT);
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>();
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
		targetRef.getValue().setOid(roleOid);
		targetRef.getValue().setTargetType(refType);
		targetRef.getValue().setRelation(relation);
		return assignmentDelta;
	}
	
	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation, boolean add) throws SchemaException {
		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected ContainerDelta<AssignmentType> createAccountAssignmentModification(String resourceOid, String intent, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(getUserDefinition(), UserType.F_ASSIGNMENT);
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>();
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		AssignmentType assignmentType = cval.asContainerable();
		AccountConstructionType accountConstructionType = new AccountConstructionType();
		assignmentType.setAccountConstruction(accountConstructionType);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
		accountConstructionType.setResourceRef(resourceRef);
		accountConstructionType.setIntent(intent);
		return assignmentDelta;
	}

    protected AccountConstructionType createAccountConstruction(String resourceOid, String intent) throws SchemaException {
        AccountConstructionType accountConstructionType = new AccountConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        accountConstructionType.setResourceRef(resourceRef);
        accountConstructionType.setIntent(intent);
        return accountConstructionType;
    }

    protected ObjectDelta<UserType> createReplaceAccountConstructionUserDelta(String userOid, String id, AccountConstructionType newValue) throws SchemaException {
        PrismPropertyDefinition ppd = getAssignmentDefinition().findPropertyDefinition(AssignmentType.F_ACCOUNT_CONSTRUCTION);
        PropertyDelta<AccountConstructionType> acDelta =
                PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_ACCOUNT_CONSTRUCTION)),
                        ppd,
                        newValue);

        Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
        modifications.add(acDelta);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
        return userDelta;
    }
	
	protected ObjectDelta<UserType> createAccountAssignmentUserDelta(String userOid, String resourceOid, String intent, boolean add) throws SchemaException {
		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
		modifications.add((createAccountAssignmentModification(resourceOid, intent, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected void assignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, true);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void unassignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, false);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected PrismObject<UserType> getUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getUser");
        OperationResult result = task.getResult();
		PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, result);
		result.computeStatus();
		IntegrationTestTools.assertSuccess("getObject(User) result not success", result);
		return user;
	}
	
	protected PrismObject<UserType> findUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findUserByUsername");
        OperationResult result = task.getResult();
        ObjectQuery query = QueryUtil.createNameQuery(PrismTestUtil.createPolyString(username), prismContext);
		List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);
		if (users.isEmpty()) {
			return null;
		}
		assert users.size() == 1 : "Too many users found for username "+username+": "+users;
		return users.iterator().next();
	}

	protected PrismObject<AccountShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findAccountByUsername");
        OperationResult result = task.getResult();
        return findAccountByUsername(username, resource, task, result);
	}
	
	protected PrismObject<AccountShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<AccountShadowType>> accounts = modelService.searchObjects(AccountShadowType.class, query, null, task, result);
		if (accounts.isEmpty()) {
			return null;
		}
		assert accounts.size() == 1 : "Too many accounts found for username "+username+" on "+resource+": "+accounts;
		return accounts.iterator().next();
	}
	
	protected Collection<PrismObject<AccountShadowType>> listAccounts(PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        
        RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedAccountDefinition rAccount = rSchema.getDefaultAccountDefinition();
        Collection<ResourceAttributeDefinition> identifierDefs = rAccount.getIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        EqualsFilter ocFilter = EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext, ResourceObjectShadowType.F_OBJECT_CLASS, 
        		rAccount.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ResourceObjectShadowType.class, 
        		ResourceObjectShadowType.F_RESOURCE_REF, resource);
        AndFilter filter = AndFilter.createAnd(ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
        
		List<PrismObject<AccountShadowType>> accounts = modelService.searchObjects(AccountShadowType.class, query, null, task, result);
		
		return accounts;
	}
	
	protected PrismObject<AccountShadowType> getAccount(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		return getAccount(accountOid, false);
	}
	
	protected PrismObject<AccountShadowType> getAccountNoFetch(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		return getAccount(accountOid, true);
	}
	
	protected PrismObject<AccountShadowType> getAccount(String accountOid, boolean noFetch) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getAccount");
        OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> opts = null;
		if (noFetch) {
			GetOperationOptions rootOpts = new GetOperationOptions();
			rootOpts.setNoFetch(true);
			opts = SelectorOptions.createCollection(rootOpts);
		}
		PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, accountOid, opts , task, result);
		result.computeStatus();
		IntegrationTestTools.assertSuccess("getObject(Account) result not success", result);
		return account;
	}
	
	protected void assertNoShadow(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<AccountShadowType>> accounts = repositoryService.searchObjects(AccountShadowType.class, query, result);
		if (accounts.isEmpty()) {
			return;
		}
		LOGGER.error("Found shadow for "+username+" on "+resource+" while not expecting it:\n"+accounts.get(0).dump());
		assert false : "Found shadow for "+username+" on "+resource+" while not expecting it: "+accounts;
	}
	
	private ObjectQuery createAccountShadowQuery(String username, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedAccountDefinition rAccount = rSchema.getDefaultAccountDefinition();
        Collection<ResourceAttributeDefinition> identifierDefs = rAccount.getIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        EqualsFilter idFilter = EqualsFilter.createEqual(new ItemPath(ResourceObjectShadowType.F_ATTRIBUTES), identifierDef, username);
        EqualsFilter ocFilter = EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext, 
        		ResourceObjectShadowType.F_OBJECT_CLASS, rAccount.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ResourceObjectShadowType.class, 
        		ResourceObjectShadowType.F_RESOURCE_REF, resource);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        return ObjectQuery.createObjectQuery(filter);
	}

	protected String getSingleUserAccountRef(PrismObject<UserType> user) {
        UserType userType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userType.getAccountRef().size());
        ObjectReferenceType accountRefType = userType.getAccountRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        return accountOid;
	}
	
	protected String getUserAccountRef(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
        UserType userType = user.asObjectable();
        for (ObjectReferenceType accountRefType: userType.getAccountRef()) {
        	String accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismObject<AccountShadowType> account = getAccountNoFetch(accountOid);
	        if (resourceOid.equals(account.asObjectable().getResourceRef().getOid())) {
	        	return accountOid;
	        }
        }
        AssertJUnit.fail("Account for resource "+resourceOid+" not found in "+user);
        return null; // Never reached. But compiler complains about missing return 
	}
	
	protected void assertUserNoAccountRefs(PrismObject<UserType> user) {
		UserType userJackType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getAccountRef().size());
	}
	
	protected void assertNoAccountShadow(String accountOid) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoAccountShadow");
		// Check is shadow is gone
        try {
        	PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
	}
	
	protected void assertAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedRole(user, roleOid);
	}
	
	protected void assertAssignedRole(PrismObject<UserType> user, String roleOid) {
		MidPointAsserts.assertAssignedRole(user, roleOid);
	}
	
	protected void assertNotAssignedRole(PrismObject<UserType> user, String roleOid) {
		MidPointAsserts.assertNotAssignedRole(user, roleOid);
	}

	protected void assertAssignedOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertAssignedOrg(PrismObject<UserType> user, String orgOid, QName relation) {
		MidPointAsserts.assertAssignedOrg(user, orgOid, relation);
	}
	
	protected void assertAssignedOrg(PrismObject<UserType> user, String orgOid) {
		MidPointAsserts.assertAssignedOrg(user, orgOid);
	}
	
	protected void assertHasOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertHasOrg(PrismObject<UserType> user, String orgOid) {
		MidPointAsserts.assertHasOrg(user, orgOid);
	}
	
	protected void assertHasOrg(PrismObject<UserType> user, String orgOid, QName relation) {
		MidPointAsserts.assertHasOrg(user, orgOid, relation);
	}
	
	protected void assertHasNoOrg(PrismObject<UserType> user) {
		MidPointAsserts.assertHasNoOrg(user);
	}
	
	protected void assertHasOrgs(PrismObject<UserType> user, int expectedNumber) {
		MidPointAsserts.assertHasOrgs(user, expectedNumber);
	}

	protected void assertAssignments(PrismObject<UserType> user, int expectedNumber) {
		MidPointAsserts.assertAssignments(user, expectedNumber);
	}
	
	protected void assertAssigned(PrismObject<UserType> user, String targetOid, QName refType) {
		MidPointAsserts.assertAssigned(user, targetOid, refType);
	}
	
	protected void assertAssignedNoOrg(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedNoOrg(user);
	}
	
	protected void assertAssignedNoOrg(PrismObject<UserType> user) {
		assertAssignedNo(user, OrgType.COMPLEX_TYPE);
	}
	
	protected void assertAssignedNoRole(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedNoRole(user);
	}
	
	protected void assertAssignedNoRole(PrismObject<UserType> user) {
		assertAssignedNo(user, RoleType.COMPLEX_TYPE);
	}
		
	protected void assertAssignedNo(PrismObject<UserType> user, QName refType) {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ObjectReferenceType targetRef = assignmentType.getTargetRef();
			if (targetRef != null) {
				if (refType.equals(targetRef.getType())) {
					AssertJUnit.fail(user+" has role "+targetRef.getOid()+" while expected no roles");
				}
			}
		}
	}
	
	protected void assertAssignedAccount(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAssignedAccount(user, resourceOid);
	}
	
	protected void assertAssignedAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			AccountConstructionType accountConstruction = assignmentType.getAccountConstruction();
			if (accountConstruction != null) {
				if (resourceOid.equals(accountConstruction.getResourceRef().getOid())) {
					return;
				}
			}
		}
		AssertJUnit.fail(user.toString()+" does not have account assignment for resource "+resourceOid);
	}
	
	protected void assertAssignedNoAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			AccountConstructionType accountConstruction = assignmentType.getAccountConstruction();
			if (accountConstruction != null) {
				if (resourceOid.equals(accountConstruction.getResourceRef().getOid())) {
					AssertJUnit.fail(user.toString()+" has account assignment for resource "+resourceOid+" while expecting no such assignment");
				}
			}
		}
	}

	protected PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}

    protected PrismContainerDefinition<AssignmentType> getAssignmentDefinition() {
        return prismContext.getSchemaRegistry().findContainerDefinitionByType(AssignmentType.COMPLEX_TYPE);
    }

    protected PrismObjectDefinition<ResourceType> getResourceDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
	}
	
	protected PrismObjectDefinition<AccountShadowType> getAccountShadowDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
	}
	
	protected PrismObject<UserType> createUser(String name, String fullName) throws SchemaException {
		PrismObject<UserType> user = getUserDefinition().instantiate();
		user.asObjectable().setName(PrismTestUtil.createPolyStringType(name));
		user.asObjectable().setFullName(PrismTestUtil.createPolyStringType(fullName));
		return user;
	}
	
	protected void fillinUser(PrismObject<UserType> user, String name, String fullName) {
		user.asObjectable().setName(PrismTestUtil.createPolyStringType(name));
		user.asObjectable().setFullName(PrismTestUtil.createPolyStringType(fullName));
	}
	
	protected void fillinUserAssignmentAccountConstruction(PrismObject<UserType> user, String resourceOid) {
		AssignmentType assignmentType = new AssignmentType();
        AccountConstructionType accountConstruntion = new AccountConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
		accountConstruntion.setResourceRef(resourceRef );
		assignmentType.setAccountConstruction(accountConstruntion );
		user.asObjectable().getAssignment().add(assignmentType);
	}
	
	protected void setDefaultUserTemplate(String userTemplateOid)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<SystemConfigurationType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);

		PrismReferenceValue userTemplateRefVal = new PrismReferenceValue(userTemplateOid);
		
		Collection<? extends ItemDelta> modifications = ReferenceDelta.createModificationReplaceCollection(
						SystemConfigurationType.F_DEFAULT_USER_TEMPLATE_REF,
						objectDefinition, userTemplateRefVal);

		OperationResult result = new OperationResult("Aplying default user template");

		repositoryService.modifyObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, result);
		display("Aplying default user template result", result);
		result.computeStatus();
		assertSuccess("Aplying default user template failed (result)", result);
	}
	
	protected ItemPath getIcfsNameAttributePath() {
		return new ItemPath(
				ResourceObjectShadowType.F_ATTRIBUTES,
				SchemaTestConstants.ICFS_NAME);
		
	}
	
	protected void assertResolvedResourceRefs(ModelContext<UserType,AccountShadowType> context) {
		for (ModelProjectionContext<AccountShadowType> projectionContext: context.getProjectionContexts()) {
			assertResolvedResourceRefs(projectionContext.getObjectOld(), "objectOld in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getObjectNew(), "objectNew in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getPrimaryDelta(), "primaryDelta in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getSecondaryDelta(), "secondaryDelta in "+projectionContext);
		}
	}

	private void assertResolvedResourceRefs(ObjectDelta<AccountShadowType> delta, String desc) {
		if (delta == null) {
			return;
		}
		if (delta.isAdd()) {
			assertResolvedResourceRefs(delta.getObjectToAdd(), desc);
		} else if (delta.isModify()) {
			ReferenceDelta referenceDelta = delta.findReferenceModification(ResourceObjectShadowType.F_RESOURCE_REF);
			if (referenceDelta != null) {
				assertResolvedResourceRefs(referenceDelta.getValuesToAdd(), "valuesToAdd in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToDelete(), "valuesToDelete in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToReplace(), "valuesToReplace in "+desc);
			}
		}
	}

	private void assertResolvedResourceRefs(PrismObject<AccountShadowType> shadow, String desc) {
		if (shadow == null) {
			return;
		}
		PrismReference resourceRef = shadow.findReference(ResourceObjectShadowType.F_RESOURCE_REF);
		if (resourceRef == null) {
			AssertJUnit.fail("No resourceRef in "+desc);
		}
		assertResolvedResourceRefs(resourceRef.getValues(), desc);
	}

	private void assertResolvedResourceRefs(Collection<PrismReferenceValue> values, String desc) {
		if (values == null) {
			return;
		}
		for (PrismReferenceValue pval: values) {
			assertNotNull("resourceRef in "+desc+" does not contain object", pval.getObject());
		}
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
		breakAssignmentDelta((ObjectDelta<UserType>)deltas.iterator().next());
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected void breakAssignmentDelta(ObjectDelta<UserType> userDelta) throws SchemaException {
        ContainerDelta<?> assignmentDelta = userDelta.findContainerDelta(UserType.F_ASSIGNMENT);
        PrismContainerValue<?> assignmentDeltaValue = null;
        if (assignmentDelta.getValuesToAdd() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToAdd().iterator().next();
        }
        if (assignmentDelta.getValuesToDelete() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToDelete().iterator().next();
        }
        PrismContainer<ActivationType> activationContainer = assignmentDeltaValue.findOrCreateContainer(AssignmentType.F_ACTIVATION);
        PrismContainerValue<ActivationType> emptyValue = new PrismContainerValue<ActivationType>();
		activationContainer.add(emptyValue);		
	}
	
	
	protected void purgeResourceSchema(String resourceOid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".purgeResourceSchema");
        OperationResult result = task.getResult();
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createModificationReplaceContainer(ResourceType.class, resourceOid, ResourceType.F_SCHEMA, prismContext);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
        modelService.executeChanges(deltas, null, task, result);
        
        result.computeStatus();
        assertSuccess(result);
	}
	
    protected List<PrismObject<OrgType>> searchOrg(String baseOrgOid, Integer minDepth, Integer maxDepth, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectFilter filter = OrgFilter.createOrg(baseOrgOid, minDepth, maxDepth);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}
    
    protected void assertShadowRepo(PrismObject<AccountShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertShadowCommon(accountShadow, oid, username, resourceType);
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertEquals("Unexpected number of attributes in repo shadow", 2, attributes.size());
	}	
	
	protected void assertShadowModel(PrismObject<AccountShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertShadowCommon(accountShadow, oid, username, resourceType);
		IntegrationTestTools.assertProvisioningAccountShadow(accountShadow, resourceType, RefinedAttributeDefinition.class);
	}

	private void assertShadowCommon(PrismObject<AccountShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertEquals("Account shadow OID mismatch (prism)", oid, accountShadow.getOid());
		AccountShadowType accountShadowType = accountShadow.asObjectable();
		assertEquals("Account shadow OID mismatch (jaxb)", oid, accountShadowType.getOid());
		assertEquals("Account shadow objectclass", new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"), accountShadowType.getObjectClass());
		assertEquals("Account shadow resourceRef OID", resourceType.getOid(), accountShadow.asObjectable().getResourceRef().getOid());
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());
		// TODO: assert name and UID
	}
	
	protected QName getAttributeQName(PrismObject<ResourceType> resource, String attributeLocalName) {
		String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
		return new QName(resourceNamespace, attributeLocalName);
	}
	
	protected ItemPath getAttributePath(PrismObject<ResourceType> resource, String attributeLocalName) {
		return new ItemPath(AccountShadowType.F_ATTRIBUTES, getAttributeQName(resource, attributeLocalName));
	}
	
	// TASKS
	
	protected void waitForTaskFinish(Task task, boolean checkSubresult) throws Exception {
		waitForTaskFinish(task, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final Task task, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				task.refresh(waitResult);
//				Task freshTask = taskManager.getTask(task.getOid(), waitResult);
				OperationResult result = task.getResult();
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+task+": "+IntegrationTestTools.getErrorMessage(result);
				assert !isUknown(result, checkSubresult) : "Unknown result in "+task+": "+IntegrationTestTools.getErrorMessage(result);
				return !isInProgress(result, checkSubresult);
			}
			@Override
			public void timeout() {
				try {
					task.refresh(waitResult);
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
				OperationResult result = task.getResult();
				LOGGER.debug("Result of timed-out task:\n{}", result.dump());
				assert false : "Timeout while waiting for "+task+" to finish. Last result "+result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskFinish(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskFinish(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				Task freshTask = taskManager.getTask(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				if (isUknown(result, checkSubresult)) {
					return false;
				}
//				assert !isUknown(result, checkSubresult) : "Unknown result in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				return !isInProgress(result, checkSubresult);
			}
			@Override
			public void timeout() {
				try {
					Task freshTask = taskManager.getTask(taskOid, waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.dump());
					assert false : "Timeout while waiting for "+freshTask+" to finish. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task "+taskOid+" finish", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskStart(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskStart(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskStart(final String taskOid, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskStart");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				Task freshTask = taskManager.getTask(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				if (isUknown(result, checkSubresult)) {
					return false;
				}
				return freshTask.getLastRunStartTimestamp() != null;
			}
			@Override
			public void timeout() {
				try {
					Task freshTask = taskManager.getTask(taskOid, waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.dump());
					assert false : "Timeout while waiting for "+freshTask+" to start. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task "+taskOid+" start", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskNextRun(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskNextRun(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskNextRun(final String taskOid, final boolean checkSubresult, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskNextRun");
		Task origTask = taskManager.getTask(taskOid, waitResult);
		final Long origLastRunStartTimestamp = origTask.getLastRunStartTimestamp();
		final Long origLastRunFinishTimestamp = origTask.getLastRunFinishTimestamp();
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				Task freshTask = taskManager.getTask(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
//				display("Times", longTimeToString(origLastRunStartTimestamp) + "-" + longTimeToString(origLastRunStartTimestamp) 
//						+ " : " + longTimeToString(freshTask.getLastRunStartTimestamp()) + "-" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
				if (isUknown(result, checkSubresult)) {
					return false;
				}
				if (freshTask.getLastRunFinishTimestamp() == null) {
					return false;
				}
				if (freshTask.getLastRunStartTimestamp() == null) {
					return false;
				}
				return !freshTask.getLastRunStartTimestamp().equals(origLastRunStartTimestamp)
						&& !freshTask.getLastRunFinishTimestamp().equals(origLastRunFinishTimestamp)
						&& freshTask.getLastRunStartTimestamp() < freshTask.getLastRunFinishTimestamp();
			}
			@Override
			public void timeout() {
				try {
					Task freshTask = taskManager.getTask(taskOid, waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.dump());
					assert false : "Timeout while waiting for "+freshTask+" next run. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task "+taskOid+" next run", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	private String longTimeToString(Long longTime) {
		if (longTime == null) {
			return "null";
		}
		return longTime.toString();
	}
	
	private boolean isError(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isError();
	}
	
	private boolean isUknown(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isUnknown();
	}

	private boolean isInProgress(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult.isInProgress();
	}

	private OperationResult getSubresult(OperationResult result, boolean checkSubresult) {
		if (checkSubresult) {
			return result.getLastSubresult();
		}
		return result;
	}

}
