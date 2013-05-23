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
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.transports.DummyTransport;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.notifications.notifiers.DummyNotifier;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
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
	protected static final String CONNECTOR_DUMMY_VERSION = "2.0";
	protected static final String CONNECTOR_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	
	protected static final ItemPath ACTIVATION_ADMINISTRATIVE_STATUS_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_ADMINISTRATIVE_STATUS);
	protected static final ItemPath ACTIVATION_VALID_FROM_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_VALID_FROM);
	protected static final ItemPath ACTIVATION_VALID_TO_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_VALID_TO);
	
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
	protected Clock clock;
	
	@Autowired(required = true)
	protected PrismContext prismContext;

    @Autowired(required = false)  // dummyNotifier is currently used only in model-intest,
                                  // but AbstractModelIntegrationTest is used in other modules as well.
                                  // So until all POMs are modified we keep required=false here.
    protected DummyNotifier dummyNotifier;

    @Autowired(required = false)
    protected DummyTransport dummyTransport;
	
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
		// Make sure the checks are turned on
		InternalsConfig.turnOnChecks();
	}

	protected void importObjectFromFile(String filename) throws FileNotFoundException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".importObjectFromFile");
		importObjectFromFile(filename, result);
		result.computeStatus();
		assertSuccess(result);
	}

	protected void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		OperationResult subResult = result.createSubresult(AbstractModelIntegrationTest.class+".importObjectFromFile");
		subResult.addParam("filename", filename);
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, subResult);
		subResult.computeStatus();
		if (subResult.isError()) {
			throw new SystemException("Import of file "+filename+" failed: "+subResult.getMessage());
		}
	}
	
	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, String filename, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		importObjectFromFile(filename, result);
		OperationResult importResult = result.getLastSubresult();
		assertSuccess("Import of "+filename+" has failed", importResult);
		return modelService.getObject(type, oid, null, task, result);
	}
	    
    /**
     * This is not the real thing. It is just for the tests. 
     */
    protected void applyResourceSchema(ShadowType accountType, ResourceType resourceType) throws SchemaException {
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
    	assertUser(user, oid, name, fullName, givenName, familyName, null);
    }
		
	protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName, String location) {
		assertObject(user);
		assertEquals("Wrong "+user+" OID (prism)", oid, user.getOid());
		UserType userType = user.asObjectable();
		assertEquals("Wrong "+user+" OID (jaxb)", oid, userType.getOid());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" name", name, userType.getName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" fullName", fullName, userType.getFullName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" givenName", givenName, userType.getGivenName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" familyName", familyName, userType.getFamilyName());
		
		if (location != null) {
			PrismAsserts.assertEqualsPolyString("Wrong " + user + " location", location,
					userType.getLocality());
		}
	}
	
	protected void assertShadow(PrismObject<? extends ShadowType> shadow) {
		assertObject(shadow);
	}
	
	protected void assertObject(PrismObject<? extends ObjectType> object) {
		object.checkConsistence(true, true);
		assertTrue("Incomplete definition in "+object, object.hasCompleteDefinition());
		assertFalse("No OID", StringUtils.isEmpty(object.getOid()));
		assertNotNull("Null name in "+object, object.asObjectable().getName());
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
	
	protected void assertLinked(PrismObject<UserType> user, PrismObject<ShadowType> account) throws ObjectNotFoundException, SchemaException {
		assertLinked(user, account.getOid());
	}
	
	protected void assertLinked(PrismObject<UserType> user, String accountOid) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = user.findReference(UserType.F_LINK_REF);
		assertNotNull("No linkRefs in "+user, linkRef);
		boolean found = false; 
		for (PrismReferenceValue val: linkRef.getValues()) {
			if (val.getOid().equals(accountOid)) {
				found = true;
			}
		}
		assertTrue("User "+user+" is not linked to account "+accountOid, found);
	}
	
	protected void assertNoLinkedAccount(PrismObject<UserType> user) {
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		if (accountRef == null) {
			return;
		}
		assert accountRef.isEmpty() : "Expected that "+user+" has no linked account but it has "+accountRef.size()+" linked accounts: "
			+ accountRef.getValues();
	}
	
	protected void assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getUserAccountRef(user, resourceOid);
		assertNotNull("User "+user+" has no account on resource "+resourceOid, accountOid);
	}
	
	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
		assertAccounts(user, numAccounts);
	}
	
	protected void assertAccounts(PrismObject<UserType> user, int numAccounts) throws ObjectNotFoundException, SchemaException {
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		if (accountRef == null) {
			assert numAccounts == 0 : "Expected "+numAccounts+" but "+user+" has no accountRef";
			return;
		}
		assertEquals("Wrong number of accounts linked to "+user, numAccounts, accountRef.size());
	}
	
	protected void assertAdministrativeEnabled(PrismObject<UserType> user) {
		PrismProperty<ActivationStatusType> statusProperty = user.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		assert statusProperty != null : "No status property in "+user;
		ActivationStatusType status = statusProperty.getRealValue();
		assert status != null : "status property is null in "+user;
		assert status == ActivationStatusType.ENABLED : "status property is "+status+" in "+user+", expected ENABLED";
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
		PrismObject<ShadowType> account = getAccountShadowDefinition().instantiate();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		account.asObjectable().setResourceRef(resourceRef);
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		account.asObjectable().setObjectClass(refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition().getTypeName());
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		
		return userDelta;
	}
	
	protected ObjectDelta<ShadowType> createModifyAccountShadowEmptyDelta(String accountOid) {
		return ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
	}
	
	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid, 
			PrismObject<ResourceType> resource, String attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceAttributeDelta(accountOid, resource, getAttributeQName(resource, attributeName), newRealValue);
	}
	
	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid, 
			PrismObject<ResourceType> resource, QName attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceDelta(accountOid, resource, new ItemPath(ShadowType.F_ATTRIBUTES, attributeName), newRealValue);
	}
	
	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceDelta(String accountOid, PrismObject<ResourceType> resource, ItemPath itemPath, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (ShadowType.F_ATTRIBUTES.equals(ItemPath.getName(itemPath.first()))) {
			PropertyDelta<?> attributeDelta = createAttributeReplaceDelta(resource, ((NameItemPathSegment)itemPath.last()).getName(), newRealValue);
			ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModifyDelta(accountOid, attributeDelta, ShadowType.class, prismContext);
			return accountDelta;
		} else {
			ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(
					ShadowType.class, accountOid, itemPath, prismContext, newRealValue);
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
		return PropertyDelta.createModificationReplaceProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
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
		return PropertyDelta.createModificationAddProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
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
		return PropertyDelta.createModificationDeleteProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}
	
	protected ResourceAttributeDefinition getAttributeDefinition(PrismObject<ResourceType> resource, QName attributeName) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		if (refinedSchema == null) {
			throw new SchemaException("No refined schema for "+resource);
		}
		RefinedObjectClassDefinition accountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		return accountDefinition.findAttributeDefinition(attributeName);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowAddDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(ShadowType.class, accountOid, propertyName, prismContext, newRealValue);
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
		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, accountOid, result);
		String resourceOid = shadow.asObjectable().getResourceRef().getOid();
		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result);
		ObjectDelta<ShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountOid, resource, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void assignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, true, result);
	}
	
	protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, false, result);
	}

	protected void assignRole(String userOid, String roleOid, PrismContainer<?> extension, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, true, result);
	}

	protected void unassignRole(String userOid, String roleOid, PrismContainer<?> extension, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, false, result);
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
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, null, true, result);
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
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, null, false, result);
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task, PrismContainer<?> extension, boolean add, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, relation, extension, add);
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
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, QName relation, PrismContainer<?> extension, boolean add) throws SchemaException {
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
		if (extension != null) {
			cval.add(extension.clone());
		}
		return assignmentDelta;
	}
	
	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation, PrismContainer<?> extension, boolean add) throws SchemaException {
		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, extension, add)));
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
		ConstructionType accountConstructionType = new ConstructionType();
		accountConstructionType.setKind(ShadowKindType.ACCOUNT);
		assignmentType.setConstruction(accountConstructionType);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
		accountConstructionType.setResourceRef(resourceRef);
		accountConstructionType.setIntent(intent);
		return assignmentDelta;
	}

    protected ConstructionType createAccountConstruction(String resourceOid, String intent) throws SchemaException {
        ConstructionType accountConstructionType = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        accountConstructionType.setResourceRef(resourceRef);
        accountConstructionType.setIntent(intent);
        return accountConstructionType;
    }

    protected ObjectDelta<UserType> createReplaceAccountConstructionUserDelta(String userOid, Long id, ConstructionType newValue) throws SchemaException {
        PrismPropertyDefinition ppd = getAssignmentDefinition().findPropertyDefinition(AssignmentType.F_CONSTRUCTION);
        PropertyDelta<ConstructionType> acDelta =
                PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
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
	
	protected PrismObject<UserType> getUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
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

	protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findAccountByUsername");
        OperationResult result = task.getResult();
        return findAccountByUsername(username, resource, task, result);
	}
	
	protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
		if (accounts.isEmpty()) {
			return null;
		}
		assert accounts.size() == 1 : "Too many accounts found for username "+username+" on "+resource+": "+accounts;
		return accounts.iterator().next();
	}
	
	protected Collection<PrismObject<ShadowType>> listAccounts(PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        
        RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        EqualsFilter ocFilter = EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_OBJECT_CLASS, 
        		rAccount.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.class, 
        		ShadowType.F_RESOURCE_REF, resource);
        AndFilter filter = AndFilter.createAnd(ocFilter, resourceRefFilter);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
        
		List<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
		
		return accounts;
	}
	
	protected PrismObject<ShadowType> getAccount(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getAccount(accountOid, false, true);
	}
	
	protected PrismObject<ShadowType> getAccountNoFetch(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getAccount(accountOid, true, true);
	}
	
	protected PrismObject<ShadowType> getAccount(String accountOid, boolean noFetch, boolean assertSuccess) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getAccount");
        OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> opts = null;
		if (noFetch) {
			GetOperationOptions rootOpts = new GetOperationOptions();
			rootOpts.setNoFetch(true);
			opts = SelectorOptions.createCollection(rootOpts);
		}
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, opts , task, result);
		result.computeStatus();
		if (assertSuccess) {
			IntegrationTestTools.assertSuccess("getObject(Account) result not success", result);
		}
		return account;
	}
	
	protected void assertNoShadow(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, result);
		if (accounts.isEmpty()) {
			return;
		}
		LOGGER.error("Found shadow for "+username+" on "+resource+" while not expecting it:\n"+accounts.get(0).dump());
		assert false : "Found shadow for "+username+" on "+resource+" while not expecting it: "+accounts;
	}
	
	private ObjectQuery createAccountShadowQuery(String username, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
        EqualsFilter idFilter = EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), identifierDef, null,username);
        EqualsFilter ocFilter = EqualsFilter.createEqual(ShadowType.class, prismContext, 
        		ShadowType.F_OBJECT_CLASS, rAccount.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.class, 
        		ShadowType.F_RESOURCE_REF, resource);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        return ObjectQuery.createObjectQuery(filter);
	}

	protected String getSingleUserAccountRef(PrismObject<UserType> user) {
        UserType userType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userType.getLinkRef().size());
        ObjectReferenceType accountRefType = userType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        return accountOid;
	}
	
	protected String getUserAccountRef(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        UserType userType = user.asObjectable();
        for (ObjectReferenceType accountRefType: userType.getLinkRef()) {
        	String accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        // Do not assert success yet. This may not be the right account.
	        PrismObject<ShadowType> account = getAccount(accountOid, true, false);
	        if (resourceOid.equals(account.asObjectable().getResourceRef().getOid())) {
	        	// This is noFetch. Therefore there is no fetchResult
	        	return accountOid;
	        }
        }
        AssertJUnit.fail("Account for resource "+resourceOid+" not found in "+user);
        return null; // Never reached. But compiler complains about missing return 
	}
	
	protected void assertUserNoAccountRefs(PrismObject<UserType> user) {
		UserType userJackType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
	}
	
	protected void assertNoAccountShadow(String accountOid) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoAccountShadow");
		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
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

    protected void assertNotAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, result);
        MidPointAsserts.assertNotAssignedRole(user, roleOid);
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
			ConstructionType construction = assignmentType.getConstruction();
			if (construction != null) {
				if (construction.getKind() != null && construction.getKind() != ShadowKindType.ACCOUNT) {
					continue;
				}
				if (resourceOid.equals(construction.getResourceRef().getOid())) {
					return;
				}
			}
		}
		AssertJUnit.fail(user.toString()+" does not have account assignment for resource "+resourceOid);
	}
	
	protected void assertAssignedNoAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ConstructionType construction = assignmentType.getConstruction();
			if (construction != null) {
				if (construction.getKind() != null && construction.getKind() != ShadowKindType.ACCOUNT) {
					continue;
				}
				if (resourceOid.equals(construction.getResourceRef().getOid())) {
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
    
    protected PrismContainerDefinition<?> getAssignmentExtensionDefinition() {
    	PrismContainerDefinition<AssignmentType> assignmentDefinition = getAssignmentDefinition();
    	return assignmentDefinition.findContainerDefinition(AssignmentType.F_EXTENSION);
    }
    
    protected PrismContainer<?> getAssignmentExtensionInstance() {
    	return getAssignmentExtensionDefinition().instantiate();
    }

    protected PrismObjectDefinition<ResourceType> getResourceDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
	}
	
	protected PrismObjectDefinition<ShadowType> getAccountShadowDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
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
        ConstructionType accountConstruntion = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
		accountConstruntion.setResourceRef(resourceRef);
		accountConstruntion.setKind(ShadowKindType.ACCOUNT);
		assignmentType.setConstruction(accountConstruntion);
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
				ShadowType.F_ATTRIBUTES,
				SchemaTestConstants.ICFS_NAME);
		
	}
	
	protected void assertResolvedResourceRefs(ModelContext<UserType,ShadowType> context) {
		for (ModelProjectionContext<ShadowType> projectionContext: context.getProjectionContexts()) {
			assertResolvedResourceRefs(projectionContext.getObjectOld(), "objectOld in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getObjectNew(), "objectNew in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getPrimaryDelta(), "primaryDelta in "+projectionContext);
			assertResolvedResourceRefs(projectionContext.getSecondaryDelta(), "secondaryDelta in "+projectionContext);
		}
	}

	private void assertResolvedResourceRefs(ObjectDelta<ShadowType> delta, String desc) {
		if (delta == null) {
			return;
		}
		if (delta.isAdd()) {
			assertResolvedResourceRefs(delta.getObjectToAdd(), desc);
		} else if (delta.isModify()) {
			ReferenceDelta referenceDelta = delta.findReferenceModification(ShadowType.F_RESOURCE_REF);
			if (referenceDelta != null) {
				assertResolvedResourceRefs(referenceDelta.getValuesToAdd(), "valuesToAdd in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToDelete(), "valuesToDelete in "+desc);
				assertResolvedResourceRefs(referenceDelta.getValuesToReplace(), "valuesToReplace in "+desc);
			}
		}
	}

	private void assertResolvedResourceRefs(PrismObject<ShadowType> shadow, String desc) {
		if (shadow == null) {
			return;
		}
		PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
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
    
    protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertShadowCommon(accountShadow, oid, username, resourceType);
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ShadowType.F_ATTRIBUTES);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertEquals("Unexpected number of attributes in repo shadow", 2, attributes.size());
	}	
	
	protected void assertShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertShadowCommon(accountShadow, oid, username, resourceType);
		IntegrationTestTools.assertProvisioningAccountShadow(accountShadow, resourceType, RefinedAttributeDefinition.class);
	}

	private void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
		assertShadow(accountShadow);
		assertEquals("Account shadow OID mismatch (prism)", oid, accountShadow.getOid());
		ShadowType ResourceObjectShadowType = accountShadow.asObjectable();
		assertEquals("Account shadow OID mismatch (jaxb)", oid, ResourceObjectShadowType.getOid());
		assertEquals("Account shadow objectclass", new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"), ResourceObjectShadowType.getObjectClass());
		assertEquals("Account shadow resourceRef OID", resourceType.getOid(), accountShadow.asObjectable().getResourceRef().getOid());
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());
		// TODO: assert name and UID
	}
	
	protected QName getAttributeQName(PrismObject<ResourceType> resource, String attributeLocalName) {
		String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
		return new QName(resourceNamespace, attributeLocalName);
	}
	
	protected ItemPath getAttributePath(PrismObject<ResourceType> resource, String attributeLocalName) {
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeQName(resource, attributeLocalName));
	}
	
	// TASKS
	
	protected void waitForTaskFinish(Task task, boolean checkSubresult) throws Exception {
		waitForTaskFinish(task, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final Task task, final boolean checkSubresult,final int timeout) throws Exception {
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
				assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker , timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskFinish(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskFinish(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout) throws Exception {
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
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to finish. Last result "+result;
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
	
	protected void waitForTaskStart(final String taskOid, final boolean checkSubresult,final int timeout) throws Exception {
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
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to start. Last result "+result;
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
	
	protected void waitForTaskNextRun(final String taskOid, final boolean checkSubresult, final int timeout) throws Exception {
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
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" next run. Last result "+result;
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
	
	protected void setSecurityContextUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance("get administrator");
        PrismObject<UserType> object = modelService.getObject(UserType.class, userOid, null, task, task.getResult());

        assertNotNull("User "+userOid+" is null", object.asObjectable());
        SecurityContextHolder.getContext().setAuthentication(
        		new UsernamePasswordAuthenticationToken(
        				new MidPointPrincipal(object.asObjectable()), null));
	}
	
	protected void assertEffectiveActivationDeltaOnly(ObjectDelta<UserType> userDelta, String desc, ActivationStatusType expectedEfficientActivation) {
		int expectedModifications = 0;
		// There may be metadata modification, we tolerate that
		Collection<? extends ItemDelta<?>> metadataDelta = userDelta.findItemDeltasSubPath(new ItemPath(UserType.F_METADATA));
		if (metadataDelta != null && !metadataDelta.isEmpty()) {
			expectedModifications++;
		}
		if (userDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (userDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (userDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		PropertyDelta<ActivationStatusType> effectiveStatusDelta = userDelta.findPropertyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		if (effectiveStatusDelta != null) {
			expectedModifications++;
			PrismAsserts.assertReplace(effectiveStatusDelta, expectedEfficientActivation);
		}
		assertEquals("Unexpected modifications in "+desc+": "+userDelta, expectedModifications, userDelta.getModifications().size());		
	}
	
	protected void assertValidFrom(PrismObject<? extends ObjectType> obj, Date expectedDate) {
		assertEquals("Wrong validFrom in "+obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate), 
				getActivation(obj).getValidFrom());
	}

	protected void assertValidTo(PrismObject<? extends ObjectType> obj, Date expectedDate) {
		assertEquals("Wrong validTo in "+obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate), 
				getActivation(obj).getValidTo());
	}
	
	private ActivationType getActivation(PrismObject<? extends ObjectType> obj) {
		ObjectType objectType = obj.asObjectable();
		ActivationType activation;
		if (objectType instanceof ShadowType) {
			activation = ((ShadowType)objectType).getActivation();
		} else if (objectType instanceof UserType) {
			activation = ((UserType)objectType).getActivation();
		} else {
			throw new IllegalArgumentException("Cannot get activation from "+obj);
		}
		assertNotNull("No activation in "+obj, activation);
		return activation;
	}

	protected PrismObject<TaskType> getTask(String taskOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getTask");
        OperationResult result = task.getResult();
		PrismObject<TaskType> retTask = modelService.getObject(TaskType.class, taskOid, null, task, result);
		result.computeStatus();
		IntegrationTestTools.assertSuccess("getObject(Task) result not success", result);
		return retTask;
	}

	protected <O extends ObjectType> void addObject(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<O> object = PrismTestUtil.parseObject(file);
		addObject(object);
	}
	
	protected <O extends ObjectType> void addObject(PrismObject<O> object) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".addObject");
        OperationResult result = task.getResult();
        ObjectDelta<O> addDelta = object.createAddDelta();
        modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), null, task, result);
        result.computeStatus();
        assertSuccess(result);
	}

}
