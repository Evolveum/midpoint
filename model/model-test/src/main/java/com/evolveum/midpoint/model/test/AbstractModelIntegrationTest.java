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

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
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
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
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
import com.evolveum.midpoint.prism.match.MatchingRule;
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
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.AssertJUnit;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

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

    @Autowired(required = true)
    protected DummyTransport dummyTransport;

    @Autowired(required = false)
    protected NotificationManager notificationManager;
	
	protected DummyAuditService dummyAuditService;
	
	protected boolean verbose = false; 
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
			
	public AbstractModelIntegrationTest() {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		startResources();
		dummyAuditService = DummyAuditService.getInstance();
		// Make sure the checks are turned on
		InternalsConfig.turnOnAllChecks();
        // By default, notifications are turned off because of performance implications. Individual tests turn them on for themselves.
        if (notificationManager != null) {
            notificationManager.setDisabled(true);
        }
	}

	protected void startResources() throws Exception {
		// Nothing to do by default
	}
	
	protected void importObjectFromFile(String filename) throws FileNotFoundException {
		importObjectFromFile(new File(filename));
	}
	
	protected void importObjectFromFile(File file) throws FileNotFoundException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".importObjectFromFile");
		importObjectFromFile(file, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		importObjectFromFile(new File(filename), result);
	}
	
	protected void importObjectFromFile(File file, OperationResult result) throws FileNotFoundException {
		OperationResult subResult = result.createSubresult(AbstractModelIntegrationTest.class+".importObjectFromFile");
		subResult.addParam("filename", file);
		LOGGER.trace("importObjectFromFile: {}", file);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(file);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, subResult);
		subResult.computeStatus();
		if (subResult.isError()) {
			LOGGER.error("Import of file "+file+" failed:\n{}", subResult.dump());
			Throwable cause = subResult.getCause();
			throw new SystemException("Import of file "+file+" failed: "+subResult.getMessage(), cause);
		}
	}
	
	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, String filename, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return importAndGetObjectFromFile(type, new File(filename), oid, task, result);
	}
	
	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, File file, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		importObjectFromFile(file, result);
		OperationResult importResult = result.getLastSubresult();
		TestUtil.assertSuccess("Import of "+file+" has failed", importResult);
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

	protected void assertUserProperty(String userOid, QName propertyName, Object... expectedPropValues) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getObject");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertUserProperty(user, propertyName, expectedPropValues);
	}
	
	protected void assertUserProperty(PrismObject<UserType> user, QName propertyName, Object... expectedPropValues) {
		PrismProperty<Object> property = user.findProperty(propertyName);
		assert property != null : "No property "+propertyName+" in "+user;  
		PrismAsserts.assertPropertyValue(property, expectedPropValues);
	}
	
	protected void assertLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
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
		assertTrue("User " + user + " is not linked to account " + accountOid, found);
	}
	
	protected void assertNotLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertNotLinked(user, accountOid);
	}
	
	protected void assertNotLinked(PrismObject<UserType> user, PrismObject<ShadowType> account) throws ObjectNotFoundException, SchemaException {
		assertNotLinked(user, account.getOid());
	}
	
	protected void assertNotLinked(PrismObject<UserType> user, String accountOid) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = user.findReference(UserType.F_LINK_REF);
		if (linkRef == null) {
			return;
		}
		boolean found = false; 
		for (PrismReferenceValue val: linkRef.getValues()) {
			if (val.getOid().equals(accountOid)) {
				found = true;
			}
		}
		assertFalse("User " + user + " IS linked to account " + accountOid + " but not expecting it", found);
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
		String accountOid = getAccountRef(user, resourceOid);
		assertNotNull("User " + user + " has no account on resource " + resourceOid, accountOid);
	}
	
	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAccounts(user, numAccounts);
	}
	
	protected void assertAccounts(PrismObject<UserType> user, int numAccounts) throws ObjectNotFoundException, SchemaException {
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		if (accountRef == null) {
			assert numAccounts == 0 : "Expected "+numAccounts+" but "+user+" has no accountRef";
			return;
		}
		assertEquals("Wrong number of accounts linked to " + user, numAccounts, accountRef.size());
	}
	
	protected void assertAdministrativeStatusEnabled(PrismObject<? extends ObjectType> user) {
		assertAdministrativeStatus(user, ActivationStatusType.ENABLED);
	}
	
	protected void assertAdministrativeStatusDisabled(PrismObject<? extends ObjectType> user) {
		assertAdministrativeStatus(user, ActivationStatusType.DISABLED);
	}
	
	protected void assertAdministrativeStatus(PrismObject<? extends ObjectType> object, ActivationStatusType expected) {
		PrismProperty<ActivationStatusType> statusProperty = object.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		assert statusProperty != null : "No status property in "+object;
		ActivationStatusType status = statusProperty.getRealValue();
		assert status != null : "No status property is null in "+object;
		assert status == expected : "status property is "+status+", expected "+expected+" in "+object;
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
	
	protected ObjectDelta<UserType> createModifyUserDeleteDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationDeleteProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
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
	
	protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getAccountRef(userOid, resource.getOid());
		PrismObject<ShadowType> account = getAccount(accountOid);
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		
		return userDelta;
	}
	
	protected ObjectDelta<UserType> createModifyUserUnlinkAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getAccountRef(userOid, resource.getOid());
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setOid(accountOid);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
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
	
	protected void modifyUserDelete(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> objectDelta = createModifyUserDeleteDelta(userOid, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void modifyAccountShadowReplace(String accountOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		String resourceOid = shadow.asObjectable().getResourceRef().getOid();
		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceOid, null, task, result);
		ObjectDelta<ShadowType> objectDelta = createModifyAccountShadowReplaceDelta(accountOid, resource, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void recomputeUser(String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		modelService.recompute(UserType.class, userOid, task, result);
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
		
		AssignmentType assignmentType = createAccountAssignment(resourceOid, intent);
		
		if (add) {
			assignmentDelta.addValueToAdd(assignmentType.asPrismContainerValue());
		} else {
			assignmentDelta.addValueToDelete(assignmentType.asPrismContainerValue());
		}
		
		PrismContainerDefinition<AssignmentType> assignmentDef = getUserDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		assignmentDelta.applyDefinition(assignmentDef);
		
		return assignmentDelta;
	}
	
	protected AssignmentType createAccountAssignment(String resourceOid, String intent) {
		AssignmentType assignmentType = new AssignmentType();//.asContainerable();
		ConstructionType accountConstructionType = new ConstructionType();
		accountConstructionType.setKind(ShadowKindType.ACCOUNT);
		assignmentType.setConstruction(accountConstructionType);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
		accountConstructionType.setResourceRef(resourceRef);
		accountConstructionType.setIntent(intent);
		return assignmentType;
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
        PrismContainerDefinition pcd = getAssignmentDefinition().findContainerDefinition(AssignmentType.F_CONSTRUCTION);
        ContainerDelta<ConstructionType> acDelta = new ContainerDelta<ConstructionType>(new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)), pcd);
//                ContainerDelta.createDelta(prismContext, ConstructionType.class, AssignmentType.F_CONSTRUCTION);
        acDelta.setValueToReplace(newValue.asPrismContainerValue());
//        PropertyDelta.createModificationReplaceProperty(
//                        new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
//                        ppd,
//                        newValue);

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
		TestUtil.assertSuccess("getObject(User) result not success", result);
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
			TestUtil.assertSuccess("getObject(Account) result not success", result);
		}
		return account;
	}
	
	protected <O extends ObjectType> void assertNoObject(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		try {
			PrismObject<O> object = modelService.getObject(type, oid, null, task, result);
			
			AssertJUnit.fail("Expected that "+object+" does not exist, but it does");
		} catch (ObjectNotFoundException e) {
			// This is expected
			return;
		}
	}
	
	protected void assertNoShadow(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, null, result);
		if (accounts.isEmpty()) {
			return;
		}
		LOGGER.error("Found shadow for "+username+" on "+resource+" while not expecting it:\n"+accounts.get(0).dump());
		assert false : "Found shadow for "+username+" on "+resource+" while not expecting it: "+accounts;
	}
	
	protected void assertHasShadow(String username, PrismObject<ResourceType> resource, 
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, null, result);
		if (accounts.isEmpty()) {
			AssertJUnit.fail("No shadow for "+username+" on "+resource);
		} else if (accounts.size() > 1) {
			AssertJUnit.fail("Too many shadows for "+username+" on "+resource+" ("+accounts.size()+"): "+accounts);
		}
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
	
	protected String getAccountRef(String userOid, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getAccountRef(getUser(userOid), resourceOid);
	}
	
	protected String getAccountRef(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        UserType userType = user.asObjectable();
        for (ObjectReferenceType accountRefType: userType.getLinkRef()) {
        	String accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
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
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        	AssertJUnit.fail("Shadow "+accountOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
	}
	
	protected void assertAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedRole(user, roleOid);
	}
	
	protected void assertAssignedRole(PrismObject<UserType> user, String roleOid) {
		MidPointAsserts.assertAssignedRole(user, roleOid);
	}

    protected void assertNotAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        MidPointAsserts.assertNotAssignedRole(user, roleOid);
    }

    protected void assertNotAssignedRole(PrismObject<UserType> user, String roleOid) {
		MidPointAsserts.assertNotAssignedRole(user, roleOid);
	}

	protected void assertAssignedOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertAssignedOrg(PrismObject<UserType> user, String orgOid, QName relation) {
		MidPointAsserts.assertAssignedOrg(user, orgOid, relation);
	}
	
	protected void assertAssignedOrg(PrismObject<UserType> user, String orgOid) {
		MidPointAsserts.assertAssignedOrg(user, orgOid);
	}
	
	protected void assertHasOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
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
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedNoOrg(user);
	}
	
	protected void assertAssignedNoOrg(PrismObject<UserType> user) {
		assertAssignedNo(user, OrgType.COMPLEX_TYPE);
	}
	
	protected void assertAssignedNoRole(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
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
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
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
		AssertJUnit.fail(user.toString() + " does not have account assignment for resource " + resourceOid);
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
		return createUser(name, fullName, null);
	}
	
	protected PrismObject<UserType> createUser(String name, String fullName, Boolean enabled) throws SchemaException {
		PrismObject<UserType> user = getUserDefinition().instantiate();
		UserType userType = user.asObjectable();
		userType.setName(PrismTestUtil.createPolyStringType(name));
		userType.setFullName(PrismTestUtil.createPolyStringType(fullName));
		if (enabled != null) {
			ActivationType activation = new ActivationType();
			userType.setActivation(activation);
			if (enabled) {
				activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
			} else {
				activation.setAdministrativeStatus(ActivationStatusType.DISABLED);
			}
		}
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
	
	protected PrismObject<ShadowType> createAccount(PrismObject<ResourceType> resource, String name, boolean enabled) throws SchemaException {
		PrismObject<ShadowType> shadow = getShadowDefinition().instantiate();
		ShadowType shadowType = shadow.asObjectable();
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		shadowType.setObjectClass(objectClassDefinition.getTypeName());
		shadowType.setKind(ShadowKindType.ACCOUNT);
		ResourceAttributeContainer attrCont = ShadowUtil.getOrCreateAttributesContainer(shadow, objectClassDefinition);
		RefinedAttributeDefinition icfsNameDef = objectClassDefinition.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		ResourceAttribute icfsNameAttr = icfsNameDef.instantiate();
		icfsNameAttr.setRealValue(name);
		attrCont.add(icfsNameAttr);
		ActivationType activation = new ActivationType();
		shadowType.setActivation(activation);
		if (enabled) {
			activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
		} else {
			activation.setAdministrativeStatus(ActivationStatusType.DISABLED);
		}
		return shadow;
	}
	
	protected <T> void addAttributeToShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, String attrName, T attrValue) throws SchemaException {
		ResourceAttributeContainer attrs = ShadowUtil.getAttributesContainer(shadow);
        ResourceAttributeDefinition attrSnDef = attrs.getDefinition().findAttributeDefinition(
        		 new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName));
        ResourceAttribute<T> attr = attrSnDef.instantiate();
        attr.setRealValue(attrValue);
        attrs.add(attr);
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
		TestUtil.assertSuccess("Aplying default user template failed (result)", result);
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
        TestUtil.assertSuccess(result);
	}
	
    protected List<PrismObject<OrgType>> searchOrg(String baseOrgOid, Integer minDepth, Integer maxDepth, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectFilter filter = OrgFilter.createOrg(baseOrgOid, minDepth, maxDepth);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}
	
    protected void assertShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
    	assertShadowModel(accountShadow, oid, username, resourceType, null);
    }
    
	protected void assertShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, MatchingRule<String> nameMatchingRule) {
		assertShadowCommon(accountShadow, oid, username, resourceType, nameMatchingRule);
		IntegrationTestTools.assertProvisioningAccountShadow(accountShadow, resourceType, RefinedAttributeDefinition.class);
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
				assert !isError(result, checkSubresult) : "Error in "+task+": "+TestUtil.getErrorMessage(result);
				assert !isUknown(result, checkSubresult) : "Unknown result in "+task+": "+TestUtil.getErrorMessage(result);
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
		waitForTaskFinish(taskOid, checkSubresult, timeout, false);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout, final boolean errorOk) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws Exception {
				Task freshTask = taskManager.getTask(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				if (verbose) display("Check result", result);
				if (isError(result, checkSubresult)) {
					if (errorOk) {
						return true;
					} else {
						AssertJUnit.fail("Error in "+freshTask+": "+TestUtil.getErrorMessage(result));
					}
				}
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
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+TestUtil.getErrorMessage(result);
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
				if (isError(result, checkSubresult)) {
                    assert false : "Error in "+freshTask+": "+TestUtil.getErrorMessage(result)+"\n\n"+result.debugDump();
                }
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
	
	protected void restartTask(String taskOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		final OperationResult result = new OperationResult(AbstractIntegrationTest.class+".restartTask");
		ObjectDelta<TaskType> taskDelta = ObjectDelta.createModificationReplaceProperty(TaskType.class, taskOid, TaskType.F_EXECUTION_STATUS, prismContext, TaskExecutionStatusType.RUNNABLE);
		taskDelta.addModificationReplaceProperty(TaskType.F_RESULT_STATUS);
		taskDelta.addModificationReplaceProperty(TaskType.F_RESULT);
		taskManager.modifyTask(taskOid, taskDelta.getModifications(), result);
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
		TestUtil.assertSuccess("getObject(Task) result not success", result);
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
        TestUtil.assertSuccess(result);
        object.setOid(addDelta.getOid());
	}
	
	protected <O extends ObjectType> void deleteObject(Class<O> type, String oid, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
	}
	
	protected void addTrigger(String oid, XMLGregorianCalendar timestamp, String uri) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".addTrigger");
        OperationResult result = task.getResult();
        TriggerType triggerType = new TriggerType();
        triggerType.setTimestamp(timestamp);
        triggerType.setHandlerUri(uri);
        ObjectDelta<ObjectType> delta = ObjectDelta.createModificationAddContainer(ObjectType.class, oid, 
        		new ItemPath(ObjectType.F_TRIGGER), prismContext, triggerType);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	protected <O extends ObjectType> void assertTrigger(PrismObject<O> object, String handlerUri, XMLGregorianCalendar start, XMLGregorianCalendar end) throws ObjectNotFoundException, SchemaException {
		for (TriggerType trigger: object.asObjectable().getTrigger()) {
			if (handlerUri.equals(trigger.getHandlerUri()) 
					&& MiscUtil.isBetween(trigger.getTimestamp(), start, end)) {
				return;
			}
		}
		AssertJUnit.fail("Expected that "+object+" will have a trigger but it has not");
	}
	
	protected <O extends ObjectType> void assertTrigger(PrismObject<O> object, String handlerUri, XMLGregorianCalendar mid, long tolerance) throws ObjectNotFoundException, SchemaException {
		XMLGregorianCalendar start = XmlTypeConverter.addMillis(mid, -tolerance);
		XMLGregorianCalendar end = XmlTypeConverter.addMillis(mid, tolerance);
		for (TriggerType trigger: object.asObjectable().getTrigger()) {
			if (handlerUri.equals(trigger.getHandlerUri()) 
					&& MiscUtil.isBetween(trigger.getTimestamp(), start, end)) {
				return;
			}
		}
		AssertJUnit.fail("Expected that "+object+" will have a trigger but it has not");
	}
	
	protected <O extends ObjectType> void assertNoTrigger(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoTrigger");
		PrismObject<O> object = repositoryService.getObject(type, oid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNoTrigger(object);
	}
	
	protected <O extends ObjectType> void assertNoTrigger(PrismObject<O> object) throws ObjectNotFoundException, SchemaException {
		List<TriggerType> triggers = object.asObjectable().getTrigger();
		if (triggers != null && !triggers.isEmpty()) {
			AssertJUnit.fail("Expected that "+object+" will have no triggers but it has "+triggers.size()+ " trigger: "+ triggers);
		}
	}

    protected void prepareNotifications() {
        notificationManager.setDisabled(false);
        dummyTransport.clearMessages();
    }

    protected void checkDummyTransportMessages(String name, int expectedCount) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        if (expectedCount == 0) {
            if (messages != null && !messages.isEmpty()) {
            	LOGGER.error(messages.size() + " unexpected message(s) recorded in dummy transport '" + name + "'");
            	logNotifyMessages(messages);
                assertFalse(messages.size() + " unexpected message(s) recorded in dummy transport '" + name + "'", true);
            }
        } else {
            assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
            if (expectedCount != messages.size()) {
            	LOGGER.error("Invalid number of messages recorded in dummy transport '" + name + "', expected: "+expectedCount+", actual: "+messages.size());
            	logNotifyMessages(messages);
            	assertEquals("Invalid number of messages recorded in dummy transport '" + name + "'", expectedCount, messages.size());
            }
        }
    }

    private void logNotifyMessages(List<Message> messages) {
		for (Message message: messages) {
			LOGGER.debug("Notification message:\n{}", message.getBody());
		}
	}

	protected void checkDummyTransportMessagesAtLeast(String name, int expectedCount) {
        if (expectedCount == 0) {
            return;
        }
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        assertTrue("Number of messages recorded in dummy transport '" + name + "' (" + messages.size() + ") is not at least " + expectedCount, messages.size() >= expectedCount);
    }
    
    protected DummyAccount getDummyAccount(String dummyInstanceName, String username) {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		try {
			return dummyResource.getAccountByUsername(username);
		} catch (ConnectException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
    
    protected DummyAccount getDummyAccountById(String dummyInstanceName, String id) {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		try {
			return dummyResource.getAccountById(id);
		} catch (ConnectException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
	
	protected void assertDummyAccount(String username, String fullname, boolean active) {
		assertDummyAccount(null, username, fullname, active);
	}
	
	protected void assertDummyAccount(String dummyInstanceName, String username, String fullname, boolean active) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
		assertEquals("Wrong fullname for dummy("+dummyInstanceName+") account "+username, fullname, account.getAttributeValue("fullname"));
		assertEquals("Wrong activation for dummy("+dummyInstanceName+") account "+username, active, account.isEnabled());
	}
	
	protected void assertDummyAccount(String dummyInstanceName, String username) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
	}
	
	protected void assertDummyAccountById(String dummyInstanceName, String id) {
		DummyAccount account = getDummyAccountById(dummyInstanceName, id);
		assertNotNull("No dummy("+dummyInstanceName+") account for id "+id, account);
	}
	
	protected void assertNoDummyAccountById(String dummyInstanceName, String id) {
		DummyAccount account = getDummyAccountById(dummyInstanceName, id);
		assertNull("Dummy("+dummyInstanceName+") account for id "+id+" exists while not expecting it", account);
	}
	
	protected void assertDummyAccountActivation(String dummyInstanceName, String username, boolean active) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
		assertEquals("Wrong activation for dummy("+dummyInstanceName+") account "+username, active, account.isEnabled());
	}

	protected void assertNoDummyAccount(String username) {
		assertNoDummyAccount(null, username);
	}
	
	protected void assertNoDummyAccount(String dummyInstanceName, String username) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNull("Dummy account for username "+username+" exists while not expecting it ("+dummyInstanceName+")", account);
	}
	
	protected void assertDefaultDummyAccountAttribute(String username, String attributeName, Object... expectedAttributeValues) {
		assertDummyAccountAttribute(null, username, attributeName, expectedAttributeValues);
	}
	
	protected void assertDummyAccountAttribute(String dummyInstanceName, String username, String attributeName, Object... expectedAttributeValues) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
			return;
		}
		assertNotNull("No values for attribute "+attributeName+" of "+dummyInstanceName+" dummy account "+username, values);
		assertEquals("Unexpected number of values for attribute "+attributeName+" of dummy account "+username+": "+values, expectedAttributeValues.length, values.size());
		for (Object expectedValue: expectedAttributeValues) {
			if (!values.contains(expectedValue)) {
				AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of dummy account "+username+
						" but not found. Values found: "+values);
			}
		}
	}
	
	protected void assertDummyAccountNoAttribute(String dummyInstanceName, String username, String attributeName) {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		assertTrue("Unexpected values for attribute "+attributeName+" of dummy account "+username+": "+values, values == null || values.isEmpty());
	}
    
	protected void assertOpenDjAccount(String uid, String cn, Boolean active) throws DirectoryException {
		SearchResultEntry entry = openDJController.searchByUid(uid);
		assertNotNull("OpenDJ accoun with uid "+uid+" not found", entry);
		openDJController.assertAttribute(entry, "cn", cn);
		if (active != null) {
			openDJController.assertActive(entry, active);
		}
	}
	
	protected void assertNoOpenDjAccount(String uid) throws DirectoryException {
		SearchResultEntry entry = openDJController.searchByUid(uid);
		assertNull("Expected that OpenDJ account with uid "+uid+" will be gone, but it is still there", entry);
	}
	
	protected void assertIteration(PrismObject<ShadowType> shadow, Integer expectedIteration, String expectedIterationToken) {
		PrismAsserts.assertPropertyValue(shadow, ShadowType.F_ITERATION, expectedIteration);
		PrismAsserts.assertPropertyValue(shadow, ShadowType.F_ITERATION_TOKEN, expectedIterationToken);
	}
	
	protected void assertIterationDelta(ObjectDelta<ShadowType> shadowDelta, Integer expectedIteration, String expectedIterationToken) {
		PrismAsserts.assertPropertyReplace(shadowDelta, ShadowType.F_ITERATION, expectedIteration);
		PrismAsserts.assertPropertyReplace(shadowDelta, ShadowType.F_ITERATION_TOKEN, expectedIterationToken);
	}
	
	protected void assertSituation(PrismObject<ShadowType> shadow, SynchronizationSituationType expectedSituation) {
		if (expectedSituation == null) {
			PrismAsserts.assertNoItem(shadow, ShadowType.F_SYNCHRONIZATION_SITUATION);
		} else {
			PrismAsserts.assertPropertyValue(shadow, ShadowType.F_SYNCHRONIZATION_SITUATION, expectedSituation);
		}
	}
	
	protected void assertEnableTimestampFocus(PrismObject<? extends FocusType> focus, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		XMLGregorianCalendar userDisableTimestamp = focus.asObjectable().getActivation().getEnableTimestamp();
		IntegrationTestTools.assertBetween("Wrong user enableTimestamp in "+focus, 
				startTime, endTime, userDisableTimestamp);
	}

	protected void assertDisableTimestampFocus(PrismObject<? extends FocusType> focus, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		XMLGregorianCalendar userDisableTimestamp = focus.asObjectable().getActivation().getDisableTimestamp();
		IntegrationTestTools.assertBetween("Wrong user disableTimestamp in "+focus, 
				startTime, endTime, userDisableTimestamp);
	}
	
	protected void assertEnableTimestampShadow(PrismObject<? extends ShadowType> shadow, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		assertNotNull("No activation in "+shadow, activationType);
		XMLGregorianCalendar userDisableTimestamp = activationType.getEnableTimestamp();
		IntegrationTestTools.assertBetween("Wrong shadow enableTimestamp in "+shadow, 
				startTime, endTime, userDisableTimestamp);
	}

	protected void assertDisableTimestampShadow(PrismObject<? extends ShadowType> shadow, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		XMLGregorianCalendar userDisableTimestamp = shadow.asObjectable().getActivation().getDisableTimestamp();
		IntegrationTestTools.assertBetween("Wrong shadow disableTimestamp in "+shadow, 
				startTime, endTime, userDisableTimestamp);
	}
	
	protected void assertDisableReasonShadow(PrismObject<? extends ShadowType> shadow, String expectedReason) {
		String disableReason = shadow.asObjectable().getActivation().getDisableReason();
		assertEquals("Wrong shadow disableReason in "+shadow, expectedReason, disableReason);
	}
	
	protected void assertPassword(PrismObject<UserType> user, String expectedPassword) throws EncryptionException {
		CredentialsType credentialsType = user.asObjectable().getCredentials();
		assertNotNull("No credentials in "+user, credentialsType);
		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password in "+user, passwordType);
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertNotNull("No password value in "+user, protectedStringType);
		String decryptedUserPassword = protector.decryptString(protectedStringType);
		assertEquals("Wrong password in "+user, expectedPassword, decryptedUserPassword);
	}

}
