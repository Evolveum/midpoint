/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterInvocation;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Abstract framework for an integration test that is placed on top of a model API.
 * This provides complete environment that the test should need, e.g model service instance, repository, provisioning,
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

	protected static final String CONNECTOR_LDAP_TYPE = "com.evolveum.polygon.connector.ldap.LdapConnector";
	protected static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-ldap/com.evolveum.polygon.connector.ldap.LdapConnector";
	
	protected static final String CONNECTOR_AD_TYPE = "Org.IdentityConnectors.ActiveDirectory.ActiveDirectoryConnector";
	protected static final String CONNECTOR_AD_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/ActiveDirectory.Connector/Org.IdentityConnectors.ActiveDirectory.ActiveDirectoryConnector";
	
	protected static final ItemPath ACTIVATION_ADMINISTRATIVE_STATUS_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_ADMINISTRATIVE_STATUS);
	protected static final ItemPath ACTIVATION_VALID_FROM_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_VALID_FROM);
	protected static final ItemPath ACTIVATION_VALID_TO_PATH = new ItemPath(UserType.F_ACTIVATION, 
			ActivationType.F_VALID_TO);
	
	protected static final ItemPath PASSWORD_VALUE_PATH = new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
	
	private static final String DEFAULT_CHANNEL = SchemaConstants.CHANNEL_GUI_USER_URI;
	
	@Autowired(required = true)
	protected ModelService modelService;
	
	@Autowired(required = true)
	protected ModelInteractionService modelInteractionService;
	
	@Autowired(required = true)
	protected ModelDiagnosticService modelDiagnosticService;
	
	@Autowired(required = true)
	protected ModelAuditService modelAuditService;
	
	@Autowired(required = true)
	protected ModelPortType modelWeb;
	
	@Autowired(required = true)
	protected RepositoryService repositoryService;
	
	@Autowired(required = true)
	protected SystemObjectCache systemObjectCache;
	
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
    
    @Autowired(required = false)
    protected UserProfileService userProfileService;
    
	@Autowired(required=true)
	protected SecurityEnforcer securityEnforcer;
	
	@Autowired(required=true)
	protected MidpointFunctions libraryMidpointFunctions;
	
	protected DummyResourceCollection dummyResourceCollection;
	
	protected DummyAuditService dummyAuditService;
	
	protected boolean verbose = true;
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
			
	public AbstractModelIntegrationTest() {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		dummyResourceCollection = new DummyResourceCollection(modelService);
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
	
	@AfterClass
	protected void cleanUpSecurity() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(null);
	}
	
	protected DummyResourceContoller initDummyResource(String name, File resourceFile, String resourceOid, 
			FailableProcessor<DummyResourceContoller> controllerInitLambda,
			Task task, OperationResult result) throws Exception {
		return dummyResourceCollection.initDummyResource(name, resourceFile, resourceOid, controllerInitLambda, task, result);
	}

	protected DummyResourceContoller initDummyResource(String name, File resourceFile, String resourceOid, 
			Task task, OperationResult result) throws Exception {
		return dummyResourceCollection.initDummyResource(name, resourceFile, resourceOid, null, task, result);
	}

	protected DummyResourceContoller initDummyResourcePirate(String name, File resourceFile, String resourceOid, 
			Task task, OperationResult result) throws Exception {
		return initDummyResource(name, resourceFile, resourceOid, controller -> controller.extendSchemaPirate(), task, result);
	}
	
	protected DummyResourceContoller getDummyResourceController(String name) {
		return dummyResourceCollection.get(name);
	}
	
	protected DummyResourceContoller getDummyResourceController() {
		return getDummyResourceController(null);
	}
	
	protected DummyResource getDummyResource(String name) {
		return dummyResourceCollection.getDummyResource(name);
	}
	
	protected DummyResource getDummyResource() {
		return getDummyResource(null);
	}
	
	protected PrismObject<ResourceType> getDummyResourceObject(String name) {
		return dummyResourceCollection.getResourceObject(name);
	}
	
	protected PrismObject<ResourceType> getDummyResourceObject() {
		return getDummyResourceObject(null);
	}
	
	protected ResourceType getDummyResourceType(String name) {
		return dummyResourceCollection.getResourceType(name);
	}
	
	protected ResourceType getDummyResourceType() {
		return getDummyResourceType(null);
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
		importObjectFromFile(file, task, result);
		subResult.computeStatus();
		if (subResult.isError()) {
			LOGGER.error("Import of file "+file+" failed:\n{}", subResult.debugDump());
			Throwable cause = findCause(subResult);
			throw new SystemException("Import of file "+file+" failed: "+subResult.getMessage(), cause);
		}
	}
	
	protected void importObjectFromFile(File file, Task task, OperationResult result) throws FileNotFoundException {
		FileInputStream stream = new FileInputStream(file);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
	}
	
	protected Throwable findCause(OperationResult result) {
		if (result.getCause() != null) {
			return result.getCause();
		}
		for (OperationResult sub: result.getSubresults()) {
			Throwable cause = findCause(sub);
			if (cause != null) {
				return cause;
			}
		}
		return null;
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
    	assertObjects(UserType.class, expectedNumberOfUsers);
    }
    
    protected void assertRoles(int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
    	assertObjects(RoleType.class, expectedNumberOfUsers);
    }
    
    protected <O extends ObjectType> void assertObjects(Class<O> type, int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".assertObjects");
        OperationResult result = task.getResult();
    	List<PrismObject<O>> users = modelService.searchObjects(type, null, null, task, result);
        if (verbose) display(type.getSimpleName()+"s", users);
        assertEquals("Unexpected number of "+type.getSimpleName()+"s", expectedNumberOfUsers, users.size());
    }

	protected void assertUserProperty(String userOid, QName propertyName, Object... expectedPropValues) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getObject");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertUserProperty(user, propertyName, expectedPropValues);
	}

	protected void assertUserNoProperty(String userOid, QName propertyName) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getObject");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertUserNoProperty(user, propertyName);
	}

	protected void assertUserProperty(PrismObject<UserType> user, QName propertyName, Object... expectedPropValues) {
		PrismProperty<Object> property = user.findProperty(propertyName);
		assert property != null : "No property "+propertyName+" in "+user;  
		PrismAsserts.assertPropertyValue(property, expectedPropValues);
	}
	
	protected void assertUserNoProperty(PrismObject<UserType> user, QName propertyName) {
		PrismProperty<Object> property = user.findProperty(propertyName);
		assert property == null : "Property "+propertyName+" present in "+user+": "+property;
	}

	protected void assertLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		assertLinked(UserType.class, userOid, accountOid);
	}
	
	protected <F extends FocusType> void assertLinked(Class<F> type, String focusOid, String projectionOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<F> user = repositoryService.getObject(type, focusOid, null, result);
		assertLinked(user, projectionOid);
	}
	
	protected <F extends FocusType> void assertLinked(PrismObject<F> focus, PrismObject<ShadowType> projection) throws ObjectNotFoundException, SchemaException {
		assertLinked(focus, projection.getOid());
	}
	
	protected <F extends FocusType> void assertLinked(PrismObject<F> focus, String projectionOid) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		assertNotNull("No linkRefs in "+focus, linkRef);
		boolean found = false; 
		for (PrismReferenceValue val: linkRef.getValues()) {
			if (val.getOid().equals(projectionOid)) {
				found = true;
			}
		}
		assertTrue("Focus " + focus + " is not linked to shadow " + projectionOid, found);
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
	
	protected String assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getLinkRefOid(user, resourceOid);
		assertNotNull("User " + user + " has no account on resource " + resourceOid, accountOid);
		return accountOid;
	}
	
	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertLinks(user, numAccounts);
	}
	
	protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int expectedNumLinks) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			assert expectedNumLinks == 0 : "Expected "+expectedNumLinks+" but "+focus+" has no linkRef";
			return;
		}
		assertEquals("Wrong number of links in " + focus, expectedNumLinks, linkRef.size());
	}
	
	protected void assertAdministrativeStatusEnabled(PrismObject<? extends ObjectType> user) {
		assertAdministrativeStatus(user, ActivationStatusType.ENABLED);
	}
	
	protected void assertAdministrativeStatusDisabled(PrismObject<? extends ObjectType> user) {
		assertAdministrativeStatus(user, ActivationStatusType.DISABLED);
	}
	
	protected void assertAdministrativeStatus(PrismObject<? extends ObjectType> object, ActivationStatusType expected) {
		PrismProperty<ActivationStatusType> statusProperty = object.findProperty(ACTIVATION_ADMINISTRATIVE_STATUS_PATH);
		if (expected == null && statusProperty == null) {
			return;
		}
		assert statusProperty != null : "No status property in "+object;
		ActivationStatusType status = statusProperty.getRealValue();
		if (expected == null && status == null) {
			return;
		}
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
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
		account.asObjectable().setObjectClass(refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition().getTypeName());
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		
		return userDelta;
	}
	
	protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getLinkRefOid(userOid, resource.getOid());
		PrismObject<ShadowType> account = getShadowModel(accountOid);
		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		
		return userDelta;
	}
	
	protected ObjectDelta<UserType> createModifyUserUnlinkAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		String accountOid = getLinkRefOid(userOid, resource.getOid());
		
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
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
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
	
	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, QName propertyName, Task task, OperationResult result, Object... newRealValue) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceProperty(type, oid, new ItemPath(propertyName), task, result, newRealValue);
	}
	
	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected <O extends ObjectType> void modifyObjectDeleteProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationDeleteProperty(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected <O extends ObjectType> void modifyObjectAddProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationAddProperty(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected <O extends ObjectType, C extends Containerable> void modifyObjectReplaceContainer(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceContainer(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected <O extends ObjectType, C extends Containerable> void modifyObjectAddContainer(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationAddContainer(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}

	protected <O extends ObjectType, C extends Containerable> void modifyObjectDeleteContainer(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationDeleteContainer(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected <O extends ObjectType> void modifyObjectReplaceReference(Class<O> type, String oid, QName refName, Task task, OperationResult result, PrismReferenceValue... refVals) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceReference(type, oid, new ItemPath(refName), task, result, refVals);
	}
	
	protected <O extends ObjectType> void modifyObjectReplaceReference(Class<O> type, String oid, ItemPath refPath, Task task, OperationResult result, PrismReferenceValue... refVals) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceReference(type, oid, refPath, prismContext, refVals);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void modifyUserAdd(String userOid, QName propertyName, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAdd(userOid, new ItemPath(propertyName), task, result, newRealValue);
	}
	
	protected void modifyUserAdd(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> objectDelta = createModifyUserAddDelta(userOid, propertyPath, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, null, task, result);	
	}
	
	protected void modifyUserDelete(String userOid, QName propertyName, Task task, OperationResult result, Object... newRealValue) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, 
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserDelete(userOid, new ItemPath(propertyName), task, result, newRealValue);
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
	
	protected void modifyUserChangePassword(String userOid, String newPassword, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        modifyUserReplace(userOid, PASSWORD_VALUE_PATH, task,  result, userPasswordPs);
	}
	
	protected void modifyAccountChangePassword(String accountOid, String newPassword, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        modifyAccountShadowReplace(accountOid, PASSWORD_VALUE_PATH, task,  result, userPasswordPs);
	}
	
	protected <O extends ObjectType> void renameObject(Class<O> type, String oid, String newName, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceProperty(type, oid, ObjectType.F_NAME, task, result, createPolyString(newName));
	}
	
	protected void recomputeUser(String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		modelService.recompute(UserType.class, userOid, task, result);
	}
	
	protected void assignRole(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assignRole");
		OperationResult result = task.getResult();
		assignRole(userOid, roleOid, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	protected void assignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
		assignRole(userOid, roleOid, (ActivationType) null, task, result);
	}
	
	protected void assignRole(String userOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, result);
	}

	protected void assignRole(String userOid, String roleOid, QName relation, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, true, result);
	}
	
	protected void assignRole(String userOid, String roleOid, QName relation, Consumer<AssignmentType> modificationBlock, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, modificationBlock, true, result);
	}

	protected void unassignRole(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".unassignRole");
		OperationResult result = task.getResult();
		unassignRole(userOid, roleOid, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>)null, false, result);
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
	
	protected void unassignRole(String userOid, String roleOid, QName relation, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
			modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, false, result);
	}
	
	protected void unassignRole(String userOid, String roleOid, QName relation, Consumer<AssignmentType> modificationBlock, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, modificationBlock, false, result);
	}
	
	protected void unassignAllRoles(String userOid) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
    	unassignAllRoles(userOid, false);
	}

	protected void unassignAllRoles(String userOid, boolean useRawPlusRecompute) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".unassignAllRoles");
		OperationResult result = task.getResult();
		PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, result);
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		for (AssignmentType assignment: user.asObjectable().getAssignment()) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			if (targetRef != null) {
				if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
					ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
					PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
					cval.setId(assignment.getId());
					assignmentDelta.addValueToDelete(cval);
					modifications.add(assignmentDelta);
				}
			}
		}
		if (modifications.isEmpty()) {
			return;
		}
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, useRawPlusRecompute ? ModelExecuteOptions.createRaw() : null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		if (useRawPlusRecompute) {
			recomputeUser(userOid, task, result);
			result.computeStatus();
			TestUtil.assertSuccess(result);
		}
	}
	
	protected void assignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		assignOrg(userOid, orgOid, null, task, result);
	}
	
	protected void assignOrg(String userOid, String orgOid, QName relation) 
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, 
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
			PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractIntegrationTest.class.getName()+".assignOrg");
		OperationResult result = task.getResult();
		assignOrg(userOid, orgOid, relation, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	protected void assignOrg(String userOid, String orgOid, QName relation, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, (Consumer<AssignmentType>)null, true, result);
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
		modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, (Consumer<AssignmentType>)null, false, result);
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task, 
			PrismContainer<?> extension, boolean add, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, refType, relation, task, extension, null, add, result);
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task, 
			PrismContainer<?> extension, ActivationType activationType, boolean add, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, relation, extension, activationType, add);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);		
	}
	
	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task, 
			Consumer<AssignmentType> modificationBlock, boolean add, OperationResult result) 
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, relation, modificationBlock, add);
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
				UserType.F_ASSIGNMENT, prismContext, new PrismContainerValue[0]);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);		
	}
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, QName relation, 
			PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
		try {
			return createAssignmentModification(roleOid, refType, relation, 
					assignment -> {
						if (extension != null) {
							try {
								assignment.asPrismContainerValue().add(extension.clone());
							} catch (SchemaException e) {
								throw new TunnelException(e);
							}
						}
						assignment.setActivation(activationType);
					}, add);
		} catch (TunnelException te) {
			throw (SchemaException)te.getCause();
		}
	}
		
		
	protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, QName relation, 
			Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
		targetRef.getValue().setOid(roleOid);
		targetRef.getValue().setTargetType(refType);
		targetRef.getValue().setRelation(relation);
		if (modificationBlock != null) {
			modificationBlock.accept(cval.asContainerable());
		}
		return assignmentDelta;
	}
		
	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation, 
			PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation, 
			Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, modificationBlock, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}
	
	protected ContainerDelta<AssignmentType> createAccountAssignmentModification(String resourceOid, String intent, boolean add) throws SchemaException {
		return createAssignmentModification(resourceOid, ShadowKindType.ACCOUNT, intent, add);
	}

	protected <V> PropertyDelta<V> createUserPropertyReplaceModification(QName propertyName, V... values) {
		return PropertyDelta.createReplaceDelta(getUserDefinition(), propertyName, values);
	}
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(String resourceOid, ShadowKindType kind, 
			String intent, boolean add) throws SchemaException {
		AssignmentType assignmentType = createConstructionAssignment(resourceOid, kind, intent);
		return createAssignmentModification(assignmentType, add);
	}

	protected ContainerDelta<AssignmentType> createAssignmentModification(AssignmentType assignmentType, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());

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
		return createConstructionAssignment(resourceOid, ShadowKindType.ACCOUNT, intent);
	}
	
	protected AssignmentType createConstructionAssignment(String resourceOid, ShadowKindType kind, String intent) {
		AssignmentType assignmentType = new AssignmentType();
		ConstructionType constructionType = new ConstructionType();
		constructionType.setKind(kind);
		assignmentType.setConstruction(constructionType);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resourceOid);
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
		constructionType.setResourceRef(resourceRef);
		constructionType.setIntent(intent);
		return assignmentType;
	}
	
	protected AssignmentType createTargetAssignment(String targetOid, QName targetType) {
		AssignmentType assignmentType = new AssignmentType();
		ObjectReferenceType targetRef = new ObjectReferenceType();
		targetRef.setOid(targetOid);
		targetRef.setType(targetType);
		assignmentType.setTargetRef(targetRef);
		return assignmentType;
	}
	
	protected ObjectDelta<UserType> createParametricAssignmentDelta(String userOid, String roleOid, String orgOid, String tenantOid, boolean adding) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
		if (adding) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
		targetRef.getValue().setOid(roleOid);
		targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
		
		if (orgOid != null) {
			PrismReference orgRef = cval.findOrCreateReference(AssignmentType.F_ORG_REF);
			orgRef.getValue().setOid(orgOid);
		}
		
		if (tenantOid != null) {
			PrismReference tenantRef = cval.findOrCreateReference(AssignmentType.F_TENANT_REF);
			tenantRef.getValue().setOid(tenantOid);
		}
		
		
		modifications.add(assignmentDelta);
		return  ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
	}
	
	protected void assignPrametricRole(String userOid, String roleOid, String orgOid, String tenantOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(
				createParametricAssignmentDelta(userOid, roleOid, orgOid, tenantOid, true));
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void unassignPrametricRole(String userOid, String roleOid, String orgOid, String tenantOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(
				createParametricAssignmentDelta(userOid, roleOid, orgOid, tenantOid, false));
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void assertAssignees(String targetOid, int expectedAssignees) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName()+".assertAssignees");
		int count = countAssignees(targetOid, result);
		if (count != expectedAssignees) {
			SearchResultList<PrismObject<FocusType>> assignees = listAssignees(targetOid, result);
			AssertJUnit.fail("Unexpected number of assignees of "+targetOid+", expected "+expectedAssignees+", but was " + count+ ": "+assignees);
		}
		
	}
	
	protected int countAssignees(String targetOid, OperationResult result) throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(targetOid)
				.build();
		return repositoryService.countObjects(FocusType.class, query, result);
	}
	
	protected SearchResultList<PrismObject<FocusType>> listAssignees(String targetOid, OperationResult result) throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(targetOid)
				.build();
		return repositoryService.searchObjects(FocusType.class, query, null, result);
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
        ContainerDelta<ConstructionType> acDelta = new ContainerDelta<ConstructionType>(new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)), pcd, prismContext);
//                ContainerDelta.createDelta(prismContext, ConstructionType.class, AssignmentType.F_CONSTRUCTION);
        acDelta.setValueToReplace(newValue.asPrismContainerValue());
//        PropertyDelta.createModificationReplaceProperty(
//                        new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
//                        ppd,
//                        newValue);

        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(acDelta);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
        return userDelta;
    }
	
	protected ObjectDelta<UserType> createAccountAssignmentUserDelta(String userOid, String resourceOid, String intent, boolean add) throws SchemaException {
		return createAssignmentDelta(UserType.class, userOid, resourceOid, ShadowKindType.ACCOUNT, intent, add);
	}
	
	protected <F extends FocusType> ObjectDelta<F> createAssignmentDelta(Class<F> type, String focusOid,
			String resourceOid, ShadowKindType kind, String intent, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add(createAssignmentModification(resourceOid, kind, intent, add));
		ObjectDelta<F> userDelta = ObjectDelta.createModifyDelta(focusOid, modifications, type, prismContext);
		return userDelta;
	}
	
	protected <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		return modelService.executeChanges(deltas, options, task, result);
	}

	protected <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesAssertSuccess(ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDeltaOperation<? extends ObjectType>> rv = executeChanges(objectDelta, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return rv;
	}

	protected void assignAccount(String userOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assignAccount");
		OperationResult result = task.getResult();
		assignAccount(userOid, resourceOid, intent, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
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

	protected PrismObject<UserType> getUserFromRepo(String userOid) throws ObjectNotFoundException, SchemaException {
		return repositoryService.getObject(UserType.class, userOid, null, new OperationResult("dummy"));
	}
	
	protected <O extends ObjectType> PrismObject<O> findObjectByName(Class<O> type, String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findObjectByName");
        OperationResult result = task.getResult();
		List<PrismObject<O>> objects = modelService.searchObjects(type, createNameQuery(name), null, task, result);
		if (objects.isEmpty()) {
			return null;
		}
		assert objects.size() == 1 : "Too many objects found for name "+name+": "+objects;
		return objects.iterator().next();
	}
	
	protected ObjectQuery createNameQuery(String name) throws SchemaException {
		return ObjectQueryUtil.createNameQuery(PrismTestUtil.createPolyString(name), prismContext);
	}
	
	protected PrismObject<UserType> findUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		return findObjectByName(UserType.class, username);
	}

	protected RoleType getRoleSimple(String oid) {
		try {
			return getRole(oid).asObjectable();
		} catch (CommonException e) {
			throw new SystemException("Unexpected exception while getting role " + oid + ": " + e.getMessage(), e);
		}
	}

    protected PrismObject<RoleType> getRole(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getRole");
        OperationResult result = task.getResult();
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, oid, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess("getObject(Role) result not success", result);
        return role;
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
        
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getPrimaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getObjectClassDefinition().getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.build();
		List<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
		return accounts;
	}
	
	protected PrismObject<ShadowType> getShadowModel(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getShadowModel(accountOid, false, true);
	}
	
	protected PrismObject<ShadowType> getShadowModelNoFetch(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getShadowModel(accountOid, true, true);
	}
	
	protected PrismObject<ShadowType> getShadowModel(String accountOid, boolean noFetch, boolean assertSuccess) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
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

	protected <O extends ObjectType> void assertNoObject(Class<O> type, String oid) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".assertNoObject");
		assertNoObject(type, oid, task, task.getResult());
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
		LOGGER.error("Found shadow for "+username+" on "+resource+" while not expecting it:\n"+accounts.get(0).debugDump());
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

	protected ObjectQuery createAccountShadowQuery(String username, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getPrimaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(identifierDef, ShadowType.F_ATTRIBUTES, identifierDef.getName()).eq(username)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getObjectClassDefinition().getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.build();
	}

	protected <F extends FocusType> String getSingleLinkOid(PrismObject<F> focus) {
        PrismReferenceValue accountRefValue = getSingleLinkRef(focus);
        assertNull("Unexpected object in linkRefValue", accountRefValue.getObject());
        return accountRefValue.getOid();
	}

    protected <F extends FocusType> PrismReferenceValue getSingleLinkRef(PrismObject<F> focus) {
        F focusType = focus.asObjectable();
        assertEquals("Unexpected number of linkRefs", 1, focusType.getLinkRef().size());
        ObjectReferenceType linkRefType = focusType.getLinkRef().get(0);
        String accountOid = linkRefType.getOid();
        assertFalse("No linkRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = linkRefType.asReferenceValue();
        assertEquals("OID mismatch in linkRefValue", accountOid, accountRefValue.getOid());
        return accountRefValue;
    }
	
	protected String getLinkRefOid(String userOid, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		return getLinkRefOid(getUser(userOid), resourceOid);
	}
	
	protected <F extends FocusType> String getLinkRefOid(PrismObject<F> focus, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismReferenceValue linkRef = getLinkRef(focus, resourceOid);
		if (linkRef == null) {
			return null;
		}
        return linkRef.getOid(); 
	}
	
	protected <F extends FocusType> PrismReferenceValue getLinkRef(PrismObject<F> focus, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        F focusType = focus.asObjectable();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
        	String linkTargetOid = linkRefType.getOid();
	        assertFalse("No linkRef oid", StringUtils.isBlank(linkTargetOid));
	        PrismObject<ShadowType> account = getShadowModel(linkTargetOid, true, false);
	        if (resourceOid.equals(account.asObjectable().getResourceRef().getOid())) {
	        	// This is noFetch. Therefore there is no fetchResult
	        	return linkRefType.asReferenceValue();
	        }
        }
        AssertJUnit.fail("Account for resource "+resourceOid+" not found in "+focus);
        return null; // Never reached. But compiler complains about missing return 
	}
	
	protected <F extends FocusType> String getLinkRefOid(PrismObject<F> focus, String resourceOid, ShadowKindType kind, String intent) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        F focusType = focus.asObjectable();
        for (ObjectReferenceType linkRefType: focusType.getLinkRef()) {
        	String linkTargetOid = linkRefType.getOid();
	        assertFalse("No linkRef oid", StringUtils.isBlank(linkTargetOid));
	        PrismObject<ShadowType> account = getShadowModel(linkTargetOid, true, false);
	        ShadowType shadowType = account.asObjectable();
	        if (kind != null && !kind.equals(shadowType.getKind())) {
	        	continue;
	        }
	        if (!MiscUtil.equals(intent, shadowType.getIntent())) {
	        	continue;
	        }
	        if (resourceOid.equals(shadowType.getResourceRef().getOid())) {
	        	// This is noFetch. Therefore there is no fetchResult
	        	return linkTargetOid;
	        }
        }
        AssertJUnit.fail("Linked shadow for resource "+resourceOid+", kind "+kind+" and intent "+intent+" not found in "+focus);
        return null; // Never reached. But compiler complains about missing return 
	}
	
	protected void assertUserNoAccountRefs(PrismObject<UserType> user) {
		UserType userJackType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
	}
	
	protected void assertNoShadow(String shadowOid) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoShadow");
		// Check is shadow is gone
        try {
        	PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        	AssertJUnit.fail("Shadow "+shadowOid+" still exists");
        } catch (ObjectNotFoundException e) {
        	// This is OK
        }
	}
	
	protected AssignmentType getUserAssignment(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<UserType> user = getUser(userOid);
		List<AssignmentType> assignments = user.asObjectable().getAssignment();
		for (AssignmentType assignment: assignments) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			if (targetRef != null && roleOid.equals(targetRef.getOid())) {
				return assignment;
			}
		}
		return null;
	}
	
	protected <F extends FocusType> void assertNoAssignments(PrismObject<F> user) {
		MidPointAsserts.assertNoAssignments(user);
	}
	
	protected void assertNoAssignments(String userOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertNoAssignments(user);
	}
	
	protected void assertNoAssignments(String userOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".assertNoShadow");
		assertNoAssignments(userOid, result);
	}
	
	protected void assertAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedRole(user, roleOid);
	}

	protected <F extends FocusType> AssignmentType assertAssignedRole(PrismObject<F> focus, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		return assertAssignedRole(focus, roleOid);
	}

	protected <F extends FocusType> AssignmentType assertAssignedRole(PrismObject<F> user, String roleOid) {
		return MidPointAsserts.assertAssignedRole(user, roleOid);
	}
	
	protected static <F extends FocusType> void assertAssignedRoles(PrismObject<F> user, String... roleOids) {
		MidPointAsserts.assertAssignedRoles(user, roleOids);
	}
	
	protected void assignDeputy(String userDeputyOid, String userTargetOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		assignDeputy(userDeputyOid, userTargetOid, null, task, result);
	}

	protected void assignDeputy(String userDeputyOid, String userTargetOid, Consumer<AssignmentType> modificationBlock, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task, modificationBlock, true, result);
	}

	protected void unassignDeputy(String userDeputyOid, String userTargetOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		unassignDeputy(userDeputyOid, userTargetOid, null, task, result);
	}
	
	protected void unassignDeputy(String userDeputyOid, String userTargetOid, Consumer<AssignmentType> modificationBlock, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task, modificationBlock, false, result);
	}
	
	protected void assignDeputyLimits(String userDeputyOid, String userTargetOid, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, true, task, result, limitTargets);
	}
	
	protected void unassignDeputyLimits(String userDeputyOid, String userTargetOid, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, false, task, result, limitTargets);
	}
	
	protected void modifyDeputyAssignmentLimits(String userDeputyOid, String userTargetOid, boolean add, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task, 
				assignment -> {
					AssignmentSelectorType limitTargetContent = new AssignmentSelectorType();
					assignment.setLimitTargetContent(limitTargetContent);
					for (ObjectReferenceType limitTarget: limitTargets) {
						limitTargetContent.getTargetRef().add(limitTarget);
					}
				}, add, result);
	}

	protected <F extends FocusType> void assertAssignedDeputy(PrismObject<F> focus, String targetUserOid) {
		MidPointAsserts.assertAssigned(focus, targetUserOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
	}
	
	protected static <F extends FocusType> void assertAssignedOrgs(PrismObject<F> user, String... orgOids) {
		MidPointAsserts.assertAssignedOrgs(user, orgOids);
	}
	
	protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, String... roleOids) {
		List<String> refOids = new ArrayList<String>();
		for (ObjectReferenceType ref: focus.asObjectable().getRoleMembershipRef()) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getType());
			// Name is not stored now
//			assertNotNull("Missing name in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getTargetName());
		}
		PrismAsserts.assertSets("Wrong values in roleMembershipRef in "+focus, refOids, roleOids);
	}
	
	protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, QName relation, String... roleOids) {
		if (!MiscUtil.unorderedCollectionEquals(Arrays.asList(roleOids), focus.asObjectable().getRoleMembershipRef(),
				(expectedOid, hasRef) -> {
					if (!expectedOid.equals(hasRef.getOid())) {
						return false;
					}
					if (!QNameUtil.match(relation, hasRef.getRelation())) {
						return false;
					}
					return true;
				})) {
			AssertJUnit.fail("Wrong values in roleMembershipRef in "+focus
					+", expected relation "+relation+", OIDs "+Arrays.toString(roleOids)
					+", but was "+focus.asObjectable().getRoleMembershipRef());
		}
	}

	protected <F extends FocusType> void assertDelegatedRef(PrismObject<F> focus, String... oids) {
		List<String> refOids = new ArrayList<>();
		for (ObjectReferenceType ref: focus.asObjectable().getDelegatedRef()) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in delegatedRef "+ref.getOid()+" in "+focus, ref.getType());
		}
		PrismAsserts.assertSets("Wrong values in delegatedRef in "+focus, refOids, oids);
	}

    protected <F extends FocusType> void assertNotAssignedRole(PrismObject<F> focus, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        MidPointAsserts.assertNotAssignedRole(focus, roleOid);
    }

    protected void assertNotAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        MidPointAsserts.assertNotAssignedRole(user, roleOid);
    }

    protected void assertNotAssignedResource(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        MidPointAsserts.assertNotAssignedResource(user, resourceOid);
    }

    protected void assertAssignedResource(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        MidPointAsserts.assertAssignedResource(user, resourceOid);
    }

    protected <F extends FocusType> void assertNotAssignedRole(PrismObject<F> user, String roleOid) {
		MidPointAsserts.assertNotAssignedRole(user, roleOid);
	}

    protected <F extends FocusType> void assertNotAssignedOrg(PrismObject<F> user, String orgOid, QName relation) {
        MidPointAsserts.assertNotAssignedOrg(user, orgOid, relation);
    }

	protected void assertAssignedOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedOrg(user, orgOid);
	}
	
	protected void assertAssignedOrg(PrismObject<? extends FocusType> focus, String orgOid, QName relation) {
		MidPointAsserts.assertAssignedOrg(focus, orgOid, relation);
	}
	
	protected <F extends FocusType> void assertAssignedOrg(PrismObject<F> focus, String orgOid) {
		MidPointAsserts.assertAssignedOrg(focus, orgOid);
	}

	protected void assertAssignedOrg(PrismObject<UserType> user, PrismObject<OrgType> org) {
		MidPointAsserts.assertAssignedOrg(user, org.getOid());
	}

	protected void assertHasOrg(String userOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedOrg(user, orgOid);
	}
	
    protected <F extends FocusType> void assertHasOrgs(PrismObject<F> user, String... orgOids) throws Exception {
        for (String orgOid: orgOids) {
            assertHasOrg(user, orgOid);
        }
        assertHasOrgs(user, orgOids.length);
    }
	
	protected <O extends ObjectType> void assertHasOrg(PrismObject<O> focus, String orgOid) {
		MidPointAsserts.assertHasOrg(focus, orgOid);
	}
	
	protected <O extends ObjectType> void assertHasOrg(PrismObject<O> user, String orgOid, QName relation) {
		MidPointAsserts.assertHasOrg(user, orgOid, relation);
	}

	protected <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user, String orgOid) {
		MidPointAsserts.assertHasNoOrg(user, orgOid, null);
	}
	
	protected <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user, String orgOid, QName relation) {
		MidPointAsserts.assertHasNoOrg(user, orgOid, relation);
	}

	protected <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user) {
		MidPointAsserts.assertHasNoOrg(user);
	}
	
	protected <O extends ObjectType> void assertHasOrgs(PrismObject<O> user, int expectedNumber) {
		MidPointAsserts.assertHasOrgs(user, expectedNumber);
	}
	
	protected void assertSubOrgs(String baseOrgOid, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrgOid, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEquals("Unexpected number of suborgs of org "+baseOrgOid+", has suborgs "+subOrgs, expected, subOrgs.size());
	}

	protected void assertSubOrgs(PrismObject<OrgType> baseOrg, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrg.getOid(), task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEquals("Unexpected number of suborgs of"+baseOrg+", has suborgs "+subOrgs, expected, subOrgs.size());
	}

	protected List<PrismObject<OrgType>> getSubOrgs(String baseOrgOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
				.isDirectChildOf(baseOrgOid)
				.build();
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}
	
	protected String dumpOrgTree(String topOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		PrismObject<OrgType> topOrg = modelService.getObject(OrgType.class, topOrgOid, null, task, result);
		String dump = dumpOrgTree(topOrg, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return dump;
	}
	
	protected String dumpOrgTree(PrismObject<OrgType> topOrg, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		StringBuilder sb = new StringBuilder();
		dumpOrg(sb, topOrg, 0);
		sb.append("\n");
		dumpSubOrgs(sb, topOrg.getOid(), 1, task, result);
		return sb.toString();
	}

	private void dumpSubOrgs(StringBuilder sb, String baseOrgOid, int indent, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrgOid, task, result);
		for (PrismObject<OrgType> suborg: subOrgs) {
			dumpOrg(sb, suborg, indent);
			sb.append("\n");
			dumpSubOrgs(sb, suborg.getOid(), indent + 1, task, result);
		}
	}
	
	private void dumpOrg(StringBuilder sb, PrismObject<OrgType> org, int indent) {
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(org);
		List<ObjectReferenceType> linkRefs = org.asObjectable().getLinkRef();
		sb.append(": ").append(linkRefs.size()).append(" links");
	}
	
	protected void displayUsers() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".displayUsers");
		OperationResult result = task.getResult();
		ResultHandler<UserType> handler = new ResultHandler<UserType>() {
			@Override
			public boolean handle(PrismObject<UserType> user, OperationResult parentResult) {
				display("User", user);
				return true;
			}
		};
		modelService.searchObjectsIterative(UserType.class, null, handler, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	protected <F extends FocusType> void dumpFocus(String message, PrismObject<F> focus) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".dumpFocus");
		StringBuilder sb = new StringBuilder();
		sb.append(focus.debugDump(0));
		sb.append("\nOrgs:");
		for (ObjectReferenceType parentOrgRef: focus.asObjectable().getParentOrgRef()) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, 1);
			PrismObject<OrgType> org = repositoryService.getObject(OrgType.class, parentOrgRef.getOid(), null, result);
			sb.append(org);
			PolyStringType displayName = org.asObjectable().getDisplayName();
			if (displayName != null) {
				sb.append(": ").append(displayName);
			}
		}
		sb.append("\nProjections:");
		for (ObjectReferenceType linkRef: focus.asObjectable().getLinkRef()) {
			PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, linkRef.getOid(), null, result);
			ObjectReferenceType resourceRef = shadow.asObjectable().getResourceRef();
			PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, resourceRef.getOid(), null, result);
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, 1);
			sb.append(resource);
			sb.append("/");
			sb.append(shadow.asObjectable().getKind());
			sb.append("/");
			sb.append(shadow.asObjectable().getIntent());
			sb.append(": ");
			sb.append(shadow.asObjectable().getName());
		}
		sb.append("\nAssignments:");
		for (AssignmentType assignmentType: focus.asObjectable().getAssignment()) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, 1);
			if (assignmentType.getConstruction() != null) {
				sb.append("Constr(").append(assignmentType.getConstruction().getDescription()).append(") ");
			}
			if (assignmentType.getTargetRef() != null) {
				sb.append("-[");
				if (assignmentType.getTargetRef().getRelation() != null) {
					sb.append(assignmentType.getTargetRef().getRelation().getLocalPart());
				}
				sb.append("]-> ");
				Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(assignmentType.getTargetRef().getType()).getClassDefinition();
				;
				PrismObject<? extends ObjectType> target = repositoryService.getObject(targetClass, assignmentType.getTargetRef().getOid(), null, result);
				sb.append(target);
			}
		}
		display(message, sb.toString());
	}

	protected <F extends FocusType> void assertAssignments(PrismObject<F> user, int expectedNumber) {
		MidPointAsserts.assertAssignments(user, expectedNumber);
	}
	
	protected <R extends AbstractRoleType> void assertInducements(PrismObject<R> role, int expectedNumber) {
		MidPointAsserts.assertInducements(role, expectedNumber);
	}
	
	protected <F extends FocusType> void assertAssignments(PrismObject<F> user, Class expectedType, int expectedNumber) {
		MidPointAsserts.assertAssignments(user, expectedType, expectedNumber);
	}
	
	protected <F extends FocusType> void assertAssigned(PrismObject<F> user, String targetOid, QName refType) {
		MidPointAsserts.assertAssigned(user, targetOid, refType);
	}
	
	protected void assertAssignedNoOrg(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedNoOrg(user);
	}
	
	protected void assertAssignedNoOrg(PrismObject<UserType> user) {
		assertAssignedNo(user, OrgType.COMPLEX_TYPE);
	}
	
	protected <F extends FocusType> void assertAssignedNoRole(PrismObject<F> focus, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		assertAssignedNoRole(focus);
	}
	
	protected void assertAssignedNoRole(String userOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertAssignedNoRole(user);
	}
	
	protected <F extends FocusType> void assertAssignedNoRole(PrismObject<F> user) {
		assertAssignedNo(user, RoleType.COMPLEX_TYPE);
	}
		
	protected <F extends FocusType> void assertAssignedNo(PrismObject<F> user, QName refType) {
		F userType = user.asObjectable();
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
	
	protected AssignmentType assertAssignedAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException {
		UserType userType = user.asObjectable();
		for (AssignmentType assignmentType: userType.getAssignment()) {
			ConstructionType construction = assignmentType.getConstruction();
			if (construction != null) {
				if (construction.getKind() != null && construction.getKind() != ShadowKindType.ACCOUNT) {
					continue;
				}
				if (resourceOid.equals(construction.getResourceRef().getOid())) {
					return assignmentType;
				}
			}
		}
		AssertJUnit.fail(user.toString() + " does not have account assignment for resource " + resourceOid);
		return null; // not reached
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
	
	protected PrismObjectDefinition<RoleType> getRoleDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
	}
	
	protected PrismObjectDefinition<ShadowType> getShadowDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}

    protected PrismContainerDefinition<AssignmentType> getAssignmentDefinition() {
        return prismContext.getSchemaRegistry().findContainerDefinitionByType(AssignmentType.COMPLEX_TYPE);
    }
    
    protected PrismContainerDefinition<?> getAssignmentExtensionDefinition() {
    	PrismContainerDefinition<AssignmentType> assignmentDefinition = getAssignmentDefinition();
    	return assignmentDefinition.findContainerDefinition(AssignmentType.F_EXTENSION);
    }
    
    protected PrismContainer<?> getAssignmentExtensionInstance() throws SchemaException {
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
	
	protected PrismObject<UserType> createUser(String name, String givenName, String familyName, Boolean enabled) throws SchemaException {
		PrismObject<UserType> user = getUserDefinition().instantiate();
		UserType userType = user.asObjectable();
		userType.setName(PrismTestUtil.createPolyStringType(name));
		userType.setGivenName(PrismTestUtil.createPolyStringType(givenName));
		userType.setFamilyName(PrismTestUtil.createPolyStringType(familyName));
		userType.setFullName(PrismTestUtil.createPolyStringType(givenName + " " + familyName));
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
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
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
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
		RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		shadowType.setObjectClass(objectClassDefinition.getTypeName());
		shadowType.setKind(ShadowKindType.ACCOUNT);
		ResourceAttributeContainer attrCont = ShadowUtil.getOrCreateAttributesContainer(shadow, objectClassDefinition);
		RefinedAttributeDefinition idSecondaryDef = objectClassDefinition.getSecondaryIdentifiers().iterator().next();
		ResourceAttribute icfsNameAttr = idSecondaryDef.instantiate();
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
	
	protected void setDefaultUserTemplate(String userTemplateOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, userTemplateOid);
	}
	
	protected void setDefaultObjectTemplate(QName objectType, String userTemplateOid)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName()+".setDefaultObjectTemplate");
		setDefaultObjectTemplate(objectType, userTemplateOid, result);
//		display("Aplying default user template result", result);
		result.computeStatus();
		TestUtil.assertSuccess("Aplying default object template failed (result)", result);
	}
	

	protected void setDefaultObjectTemplate(QName objectType, String objectTemplateOid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		setDefaultObjectTemplate(objectType, null, objectTemplateOid, parentResult);
	}
	
	protected void setDefaultObjectTemplate(QName objectType, String subType, String objectTemplateOid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObject<SystemConfigurationType> systemConfig = repositoryService.getObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, parentResult);
		
		PrismContainerValue<ObjectPolicyConfigurationType> oldValue = null;
		for (ObjectPolicyConfigurationType focusPolicyType: systemConfig.asObjectable().getDefaultObjectPolicyConfiguration()) {
			if (QNameUtil.match(objectType, focusPolicyType.getType()) && MiscUtil.equals(subType, focusPolicyType.getSubtype())) {
				oldValue = focusPolicyType.asPrismContainerValue();
			}
		}
		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>();
		
		if (oldValue != null) {
			ContainerDelta<ObjectPolicyConfigurationType> deleteDelta = ContainerDelta.createModificationDelete(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION, 
					SystemConfigurationType.class, prismContext, oldValue.clone());
			((Collection)modifications).add(deleteDelta);
		}
		
		if (objectTemplateOid != null) {
			ObjectPolicyConfigurationType newFocusPolicyType;
			ContainerDelta<ObjectPolicyConfigurationType> addDelta;
			if (oldValue == null) {
				newFocusPolicyType = new ObjectPolicyConfigurationType();
				newFocusPolicyType.setType(objectType);
				newFocusPolicyType.setSubtype(subType);
				addDelta = ContainerDelta.createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION, 
						SystemConfigurationType.class, prismContext, newFocusPolicyType);
			} else {
				PrismContainerValue<ObjectPolicyConfigurationType> newValue = oldValue.clone();
				addDelta = ContainerDelta.createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION, 
						SystemConfigurationType.class, prismContext, newValue);
				newFocusPolicyType = newValue.asContainerable();
			}
			ObjectReferenceType templateRef = new ObjectReferenceType();
			templateRef.setOid(objectTemplateOid);
			newFocusPolicyType.setObjectTemplateRef(templateRef);
			((Collection)modifications).add(addDelta);
		}
		
		modifySystemObjectInRepo(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);
		
	}
	
	protected <O extends ObjectType> void modifySystemObjectInRepo(Class<O> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		repositoryService.modifyObject(type, oid, modifications, parentResult);
		invalidateSystemObjectsCache();
	}
	
	@Override
	protected void invalidateSystemObjectsCache() {
		systemObjectCache.invalidateCaches();
	}
	
	protected ItemPath getIcfsNameAttributePath() {
		return new ItemPath(
				ShadowType.F_ATTRIBUTES,
				SchemaTestConstants.ICFS_NAME);
		
	}
	
	protected <F extends ObjectType> void assertResolvedResourceRefs(ModelContext<F> context) {
		for (ModelProjectionContext projectionContext: context.getProjectionContexts()) {
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
		breakAssignmentDelta((ObjectDelta<? extends FocusType>)deltas.iterator().next());
	}
	
	/**
	 * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
	 * existing user values. 
	 */
	protected <F extends FocusType> void breakAssignmentDelta(ObjectDelta<F> userDelta) throws SchemaException {
        ContainerDelta<?> assignmentDelta = userDelta.findContainerDelta(UserType.F_ASSIGNMENT);
        PrismContainerValue<?> assignmentDeltaValue = null;
        if (assignmentDelta.getValuesToAdd() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToAdd().iterator().next();
        }
        if (assignmentDelta.getValuesToDelete() != null) {
        	assignmentDeltaValue = assignmentDelta.getValuesToDelete().iterator().next();
        }
        PrismContainer<ActivationType> activationContainer = assignmentDeltaValue.findOrCreateContainer(AssignmentType.F_ACTIVATION);
        PrismContainerValue<ActivationType> emptyValue = new PrismContainerValue<ActivationType>(prismContext);
		activationContainer.add(emptyValue);		
	}
	
	
	protected void purgeResourceSchema(String resourceOid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".purgeResourceSchema");
        OperationResult result = task.getResult();
        
        ObjectDelta<ResourceType> resourceDelta = ObjectDelta.createModificationReplaceContainer(ResourceType.class, 
        		resourceOid, ResourceType.F_SCHEMA, prismContext, new PrismContainerValue[0]);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);
        
        modelService.executeChanges(deltas, null, task, result);
        
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
    protected List<PrismObject<OrgType>> searchOrg(String baseOrgOid, OrgFilter.Scope scope, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
				.isInScopeOf(baseOrgOid, scope)
				.build();
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}
    
    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName()+".searchObjectByName");
    	OperationResult result = task.getResult();
    	PrismObject<T> out = searchObjectByName(type, name, task, result);
    	result.computeStatus();
    	TestUtil.assertSuccess(result);
    	return out;
    }
    
    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectQuery query = ObjectQueryUtil.createNameQuery(name, prismContext);
		List<PrismObject<T>> foundObjects = modelService.searchObjects(type, query, null, task, result);
		if (foundObjects.isEmpty()) {
			return null;
		}
		if (foundObjects.size() > 1) {
			throw new IllegalStateException("More than one object found for type "+type+" and name '"+name+"'");
		}
		return foundObjects.iterator().next();
	}

    protected void assertAccountShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowModel(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null);
    }

    protected void assertAccountShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, MatchingRule<String> matchingRule) throws SchemaException {
        assertShadowModel(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), matchingRule);
    }
    
    protected void assertShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                     QName objectClass) throws SchemaException {
    	assertShadowModel(accountShadow, oid, username, resourceType, objectClass, null);
    }
    
	protected void assertShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                     QName objectClass, MatchingRule<String> nameMatchingRule) throws SchemaException {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, nameMatchingRule, false);
		IntegrationTestTools.assertProvisioningShadow(accountShadow, resourceType, RefinedAttributeDefinition.class, objectClass);
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

	protected void waitForTaskFinish(Task task, boolean checkSubresult, final int timeout) throws Exception {
		waitForTaskFinish(task, checkSubresult, timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskFinish(final Task task, final boolean checkSubresult, final int timeout, long sleepTime) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
				task.refresh(waitResult);
				waitResult.summarize();
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
				LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
				assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for " + task + " finish", checker, timeout, sleepTime);
	}

	protected void waitForTaskCloseOrSuspend(String taskOid) throws Exception {
		waitForTaskCloseOrSuspend(taskOid, DEFAULT_TASK_WAIT_TIMEOUT);
	}

	protected void waitForTaskCloseOrSuspend(String taskOid, final int timeout) throws Exception {
		waitForTaskCloseOrSuspend(taskOid, timeout, DEFAULT_TASK_SLEEP_TIME);
	}

	protected void waitForTaskCloseOrSuspend(final String taskOid, final int timeout, long sleepTime) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskCloseOrSuspend");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
				Task task = taskManager.getTask(taskOid, waitResult);
				waitResult.summarize();
				display("Task execution status = " + task.getExecutionStatus());
				return task.getExecutionStatus() == TaskExecutionStatus.CLOSED
						|| task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED;
			}
			@Override
			public void timeout() {
				Task task = null;
				try {
					task = taskManager.getTask(taskOid, waitResult);
				} catch (ObjectNotFoundException|SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
				OperationResult result = null;
				if (task != null) {
					result = task.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
				}
				assert false : "Timeout ("+timeout+") while waiting for "+taskOid+" to close or suspend. Last result "+result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for " + taskOid + " close/suspend", checker, timeout, sleepTime);
	}

	protected void waitForTaskFinish(String taskOid, boolean checkSubresult) throws CommonException {
		waitForTaskFinish(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout) throws CommonException {
		waitForTaskFinish(taskOid, checkSubresult, timeout, false);
	}
	
	protected void waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout, final boolean errorOk) throws CommonException {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
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
					LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to finish. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task " + taskOid + " finish", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected void waitForTaskStart(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskStart(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected void waitForTaskStart(final String taskOid, final boolean checkSubresult,final int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskStart");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
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
					LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to start. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task " + taskOid + " start", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
	}
	
	protected OperationResult waitForTaskNextRunAssertSuccess(String taskOid, boolean checkSubresult) throws Exception {
		return waitForTaskNextRunAssertSuccess(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected OperationResult waitForTaskNextRunAssertSuccess(Task origTask, boolean checkSubresult) throws Exception {
		return waitForTaskNextRunAssertSuccess(origTask, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}
	
	protected OperationResult waitForTaskNextRunAssertSuccess(final String taskOid, final boolean checkSubresult, final int timeout) throws Exception {
		OperationResult taskResult = waitForTaskNextRun(taskOid, checkSubresult, timeout);
		if (isError(taskResult, checkSubresult)) {
            assert false : "Error in task "+taskOid+": "+TestUtil.getErrorMessage(taskResult)+"\n\n"+taskResult.debugDump();
        }
		return taskResult;
	}
	
	protected OperationResult waitForTaskNextRunAssertSuccess(Task origTask, final boolean checkSubresult, final int timeout) throws Exception {
		OperationResult taskResult = waitForTaskNextRun(origTask, checkSubresult, timeout);
		if (isError(taskResult, checkSubresult)) {
            assert false : "Error in task "+origTask+": "+TestUtil.getErrorMessage(taskResult)+"\n\n"+taskResult.debugDump();
        }
		return taskResult;
	}
	
	protected OperationResult waitForTaskNextRun(final String taskOid, final boolean checkSubresult, final int timeout) throws Exception {
		return waitForTaskNextRun(taskOid, checkSubresult, timeout, false);
	}

	protected OperationResult waitForTaskNextRun(final String taskOid, final boolean checkSubresult, final int timeout, boolean kickTheTask) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskNextRun");
		Task origTask = taskManager.getTask(taskOid, waitResult);
		return waitForTaskNextRun(origTask, checkSubresult, timeout, waitResult, kickTheTask);
	}
	
	protected OperationResult waitForTaskNextRun(final Task origTask, final boolean checkSubresult, final int timeout) throws Exception {
		return waitForTaskNextRun(origTask, checkSubresult, timeout, false);
	}

	protected OperationResult waitForTaskNextRun(final Task origTask, final boolean checkSubresult, final int timeout, boolean kickTheTask) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskNextRun");
		return waitForTaskNextRun(origTask, checkSubresult, timeout, waitResult, kickTheTask);
	}
	
	protected OperationResult waitForTaskNextRun(final Task origTask, final boolean checkSubresult, final int timeout, final OperationResult waitResult, boolean kickTheTask) throws Exception {
		final Long origLastRunStartTimestamp = origTask.getLastRunStartTimestamp();
		final Long origLastRunFinishTimestamp = origTask.getLastRunFinishTimestamp();
		if (kickTheTask) {
			taskManager.scheduleTaskNow(origTask, waitResult);
		}
		final Holder<OperationResult> taskResultHolder = new Holder<>();
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
				Task freshTask = taskManager.getTask(origTask.getOid(), waitResult);
				OperationResult taskResult = freshTask.getResult();
//				display("Times", longTimeToString(origLastRunStartTimestamp) + "-" + longTimeToString(origLastRunStartTimestamp) 
//						+ " : " + longTimeToString(freshTask.getLastRunStartTimestamp()) + "-" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
				if (verbose) display("Check result", taskResult);
				taskResultHolder.setValue(taskResult);
				if (isError(taskResult, checkSubresult)) {
                    return true;
                }
				if (isUknown(taskResult, checkSubresult)) {
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
					Task freshTask = taskManager.getTask(origTask.getOid(), waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Timed-out task:\n{}", freshTask.debugDump());
					display("Times", "origLastRunStartTimestamp="+longTimeToString(origLastRunStartTimestamp) 
					+ ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp) 
					+ ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
					+ ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" next run. Last result "+result;
				} catch (ObjectNotFoundException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				} catch (SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e,e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task " + origTask + " next run", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
		
		Task freshTask = taskManager.getTask(origTask.getOid(), waitResult);
		LOGGER.debug("Final task:\n{}", freshTask.debugDump());
		display("Times", "origLastRunStartTimestamp="+longTimeToString(origLastRunStartTimestamp) 
		+ ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp) 
		+ ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
		+ ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
		
		return taskResultHolder.getValue();
	}
	
	private String longTimeToString(Long longTime) {
		if (longTime == null) {
			return "null";
		}
		return longTime.toString();
	}
	
	private boolean isError(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult != null ? subresult.isError() : false;
	}
	
	private boolean isUknown(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult != null ? subresult.isUnknown() : false;			// TODO or return true?
	}

	private boolean isInProgress(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult != null ? subresult.isInProgress() : true;		// "true" if there are no subresults
	}

	private OperationResult getSubresult(OperationResult result, boolean checkSubresult) {
		if (checkSubresult) {
			return result.getLastSubresult();
		}
		return result;
	}
	
	protected void restartTask(String taskOid) throws CommonException {
		
		// Wait at least 1ms here. We have the timestamp in the tasks with a millisecond granularity. If the tasks is started,
		// executed and then resstarted and excecuted within the same millisecond then the second execution will not be
		// detected and the wait for task finish will time-out. So waiting one millisecond here will make sure that the
		// timestamps are different. And 1ms is not that long to significantly affect test run times.
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			LOGGER.warn("Sleep interrupted: {}", e.getMessage(), e);
		}
		
		final OperationResult result = new OperationResult(AbstractIntegrationTest.class+".restartTask");
		Task task = taskManager.getTask(taskOid, result);
		LOGGER.info("Restarting task {}", taskOid);
		if (task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
			LOGGER.debug("Task {} is suspended, resuming it", task);
			taskManager.resumeTask(task, result);
		} else if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
			LOGGER.debug("Task {} is closed, scheduling it to run now", task);
			taskManager.scheduleTasksNow(Collections.singleton(taskOid), result);
		} else if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
			if (taskManager.getLocallyRunningTaskByIdentifier(task.getTaskIdentifier()) != null) {
				// Task is really executing. Let's wait until it finishes; hopefully it won't start again (TODO)
				LOGGER.debug("Task {} is running, waiting while it finishes before restarting", task);
				waitForTaskFinish(taskOid, false);
			}
			LOGGER.debug("Task {} is finished, scheduling it to run now", task);
			taskManager.scheduleTasksNow(Collections.singleton(taskOid), result);
		} else {
			throw new IllegalStateException("Task " + task + " cannot be restarted, because its state is: " + task.getExecutionStatus());
		}
	}
	
	protected void setSecurityContextUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance("get administrator");
        PrismObject<UserType> object = modelService.getObject(UserType.class, userOid, null, task, task.getResult());

        assertNotNull("User " + userOid + " is null", object.asObjectable());
        SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						new MidPointPrincipal(object.asObjectable()), null));
	}

	protected String getSecurityContextUserOid() {
		return ((MidPointPrincipal) (SecurityContextHolder.getContext().getAuthentication().getPrincipal())).getOid();
	}
	
	protected <F extends FocusType> void assertSideEffectiveDeltasOnly(String desc, ObjectDelta<F> focusDelta) {
		if (focusDelta == null) {
			return;
		}
		int expectedModifications = 0;
		// There may be metadata modification, we tolerate that
		Collection<? extends ItemDelta<?,?>> metadataDelta = focusDelta.findItemDeltasSubPath(new ItemPath(UserType.F_METADATA));
		if (metadataDelta != null && !metadataDelta.isEmpty()) {
			expectedModifications+=metadataDelta.size();
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		PropertyDelta<ActivationStatusType> effectiveStatusDelta = focusDelta.findPropertyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		if (effectiveStatusDelta != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ITERATION)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ROLE_MEMBERSHIP_REF)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_DELEGATED_REF)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ITERATION_TOKEN)) != null) {
			expectedModifications++;
		}
		assertEquals("Unexpected modifications in " + desc + ": " + focusDelta, expectedModifications, focusDelta.getModifications().size());
	}

	protected <F extends FocusType> void assertSideEffectiveDeltasOnly(ObjectDelta<F> focusDelta, String desc, ActivationStatusType expectedEfficientActivation) {
		assertEffectualDeltas(focusDelta, desc, expectedEfficientActivation, 0);
	}
	
	protected <F extends FocusType> void assertEffectualDeltas(ObjectDelta<F> focusDelta, String desc, ActivationStatusType expectedEfficientActivation, int expectedEffectualModifications) {
		if (focusDelta == null) {
			return;
		}
		int expectedModifications = expectedEffectualModifications;
		// There may be metadata modification, we tolerate that
		for (ItemDelta<?,?> modification: focusDelta.getModifications()) {
			if (modification.getPath().containsName(ObjectType.F_METADATA)) {
				expectedModifications++;
			}
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP)) != null) {
			expectedModifications++;
		}
		PropertyDelta<ActivationStatusType> effectiveStatusDelta = focusDelta.findPropertyDelta(new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		if (effectiveStatusDelta != null) {
			expectedModifications++;
			PrismAsserts.assertReplace(effectiveStatusDelta, expectedEfficientActivation);
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ROLE_MEMBERSHIP_REF)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_DELEGATED_REF)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ITERATION)) != null) {
			expectedModifications++;
		}
		if (focusDelta.findItemDelta(new ItemPath(FocusType.F_ITERATION_TOKEN)) != null) {
			expectedModifications++;
		}
		assertEquals("Unexpected modifications in "+desc+": "+focusDelta, expectedModifications, focusDelta.getModifications().size());		
	}
	
	protected void assertValidFrom(PrismObject<? extends ObjectType> obj, Date expectedDate) {
		assertEquals("Wrong validFrom in "+obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate), 
				getActivation(obj).getValidFrom());
	}

	protected void assertValidTo(PrismObject<? extends ObjectType> obj, Date expectedDate) {
		assertEquals("Wrong validTo in "+obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate), 
				getActivation(obj).getValidTo());
	}
	
	protected ActivationType getActivation(PrismObject<? extends ObjectType> obj) {
		ObjectType objectType = obj.asObjectable();
		ActivationType activation;
		if (objectType instanceof ShadowType) {
			activation = ((ShadowType)objectType).getActivation();
		} else if (objectType instanceof UserType) {
			activation = ((UserType)objectType).getActivation();
		} else {
			throw new IllegalArgumentException("Cannot get activation from "+obj);
		}
		assertNotNull("No activation in " + obj, activation);
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

	protected <T extends ObjectType> void assertObjectExists(Class<T> clazz, String oid) {
		OperationResult result = new OperationResult("assertObjectExists");
		try {
			repositoryService.getObject(clazz, oid, null, result);
		} catch (ObjectNotFoundException e) {
			fail("Object of type " + clazz.getName() + " with OID " + oid + " doesn't exist: " + e.getMessage());
		} catch (SchemaException e) {
			throw new SystemException("Object of type " + clazz.getName() + " with OID " + oid + " probably exists but couldn't be read: " + e.getMessage(), e);
		}
	}

	protected <T extends ObjectType> void assertObjectDoesntExist(Class<T> clazz, String oid) {
		OperationResult result = new OperationResult("assertObjectDoesntExist");
		try {
			PrismObject<T> object = repositoryService.getObject(clazz, oid, null, result);
			fail("Object of type " + clazz.getName() + " with OID " + oid + " exists even if it shouldn't: " + object.debugDump());
		} catch (ObjectNotFoundException e) {
			// ok
		} catch (SchemaException e) {
			throw new SystemException("Object of type " + clazz.getName() + " with OID " + oid + " probably exists, and moreover it couldn't be read: " + e.getMessage(), e);
		}
	}

	protected <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getObject");
        OperationResult result = task.getResult();
		PrismObject<O> object = modelService.getObject(type, oid, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return object;
	}

	protected <O extends ObjectType> PrismObject<O> getObjectViaRepo(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getObject");
		OperationResult result = task.getResult();
		PrismObject<O> object = repositoryService.getObject(type, oid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return object;
	}

    protected <O extends ObjectType> void addObjects(File... files) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        for (File file : files) {
            addObject(file);
        }
    }

	protected <O extends ObjectType> void addObject(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		PrismObject<O> object = PrismTestUtil.parseObject(file);
		addObject(object);
	}
	
	protected <O extends ObjectType> PrismObject<O> addObject(File file, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		PrismObject<O> object = PrismTestUtil.parseObject(file);
		addObject(object, task, result);
		return object;
	}
	
	protected <O extends ObjectType> void addObject(PrismObject<O> object) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".addObject");
        OperationResult result = task.getResult();
        addObject(object, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	protected <O extends ObjectType> void addObject(PrismObject<O> object, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> addDelta = object.createAddDelta();
        modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), null, task, result);
        object.setOid(addDelta.getOid());
	}
		
	protected <O extends ObjectType> void deleteObject(Class<O> type, String oid, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
	}

	protected <O extends ObjectType> void deleteObjectRaw(Class<O> type, String oid, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), ModelExecuteOptions.createRaw(), task, result);
	}

	protected <O extends ObjectType> void deleteObject(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".deleteObject");
		OperationResult result = task.getResult();
		ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
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
		AssertJUnit.fail("Expected that " + object + " will have a trigger but it has not");
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
    
    protected DummyAccount getDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		try {
			return dummyResource.getAccountByUsername(username);
		} catch (ConnectException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
    
    protected DummyAccount getDummyAccountById(String dummyInstanceName, String id) throws SchemaViolationException, ConflictException {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		try {
			return dummyResource.getAccountById(id);
		} catch (ConnectException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
	
	protected void assertDefaultDummyAccount(String username, String fullname, boolean active) throws SchemaViolationException, ConflictException {
		assertDummyAccount(null, username, fullname, active);
	}
	
	protected DummyAccount assertDummyAccount(String dummyInstanceName, String username, String fullname, Boolean active) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
		assertEquals("Wrong fullname for dummy("+dummyInstanceName+") account "+username, fullname, account.getAttributeValue("fullname"));
		assertEquals("Wrong activation for dummy(" + dummyInstanceName + ") account " + username, active, account.isEnabled());
		return account;
	}
	
	protected DummyAccount assertDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy(" + dummyInstanceName + ") account for username " + username, account);
		return account;
	}
	
	protected void assertDummyAccountById(String dummyInstanceName, String id) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccountById(dummyInstanceName, id);
		assertNotNull("No dummy("+dummyInstanceName+") account for id "+id, account);
	}
	
	protected void assertNoDummyAccountById(String dummyInstanceName, String id) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccountById(dummyInstanceName, id);
		assertNull("Dummy(" + dummyInstanceName + ") account for id " + id + " exists while not expecting it", account);
	}
	
	protected void assertDummyAccountActivation(String dummyInstanceName, String username, Boolean active) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy("+dummyInstanceName+") account for username "+username, account);
		assertEquals("Wrong activation for dummy(" + dummyInstanceName + ") account " + username, active, account.isEnabled());
	}

	protected void assertNoDummyAccount(String username) throws SchemaViolationException, ConflictException {
		assertNoDummyAccount(null, username);
	}
	
	protected void assertNoDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNull("Dummy account for username " + username + " exists while not expecting it (" + dummyInstanceName + ")", account);
	}
	
	protected void assertDefaultDummyAccountAttribute(String username, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException {
		assertDummyAccountAttribute(null, username, attributeName, expectedAttributeValues);
	}
	
	protected void assertDummyAccountAttribute(String dummyInstanceName, String username, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
			return;
		}
		assertNotNull("No values for attribute "+attributeName+" of "+dummyInstanceName+" dummy account "+username, values);
		assertEquals("Unexpected number of values for attribute " + attributeName + " of dummy account " + username +
				". Expected: " + Arrays.toString(expectedAttributeValues) + ", was: " + values,
				expectedAttributeValues.length, values.size());
		for (Object expectedValue: expectedAttributeValues) {
			if (!values.contains(expectedValue)) {
				AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of dummy account "+username+
						" but not found. Values found: "+values);
			}
		}
	}

	protected void assertNoDummyAccountAttribute(String dummyInstanceName, String username, String attributeName) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		if (values == null || values.isEmpty()) {
			return;
		}
		AssertJUnit.fail("Expected no value in attribute " + attributeName + " of dummy account " + username +
				". Values found: " + values);
	}

	protected void assertDummyAccountAttributeGenerated(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Integer generated = account.getAttributeValue(DummyAccount.ATTR_INTERNAL_ID, Integer.class);
		if (generated == null) {
			AssertJUnit.fail("No value in generated attribute dir of " + dummyInstanceName + " dummy account " + username);
		}
	}

	protected DummyGroup getDummyGroup(String dummyInstanceName, String name) throws SchemaViolationException, ConflictException {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		try {
			return dummyResource.getGroupByName(name);
		} catch (ConnectException e) {
			throw new IllegalStateException(e.getMessage(),e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
	
	protected void assertDummyGroup(String username, String description) throws SchemaViolationException, ConflictException {
		assertDummyGroup(null, username, description, null);
	}
	
	protected void assertDummyGroup(String username, String description, Boolean active) throws SchemaViolationException, ConflictException {
		assertDummyGroup(null, username, description, active);
	}
	
	protected void assertDummyGroup(String dummyInstanceName, String groupname, String description, Boolean active) throws SchemaViolationException, ConflictException {
		DummyGroup group = getDummyGroup(dummyInstanceName, groupname);
		assertNotNull("No dummy("+dummyInstanceName+") group for name "+groupname, group);
		assertEquals("Wrong fullname for dummy(" + dummyInstanceName + ") group " + groupname, description,
				group.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
		if (active != null) {
			assertEquals("Wrong activation for dummy("+dummyInstanceName+") group "+groupname, active, group.isEnabled());
		}
	}

	protected void assertNoDummyGroup(String groupname) throws SchemaViolationException, ConflictException {
		assertNoDummyGroup(null, groupname);
	}
	
	protected void assertNoDummyGroup(String dummyInstanceName, String groupname) throws SchemaViolationException, ConflictException {
		DummyGroup group = getDummyGroup(dummyInstanceName, groupname);
		assertNull("Dummy group '" + groupname + "' exists while not expecting it (" + dummyInstanceName + ")", group);
    }

    protected void assertDummyGroupAttribute(String dummyInstanceName, String groupname, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException {
        DummyGroup group = getDummyGroup(dummyInstanceName, groupname);
        assertNotNull("No dummy group for groupname "+groupname, group);
        Set<Object> values = group.getAttributeValues(attributeName, Object.class);
        if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
            return;
        }
        assertNotNull("No values for attribute "+attributeName+" of "+dummyInstanceName+" dummy group "+groupname, values);
        assertEquals("Unexpected number of values for attribute " + attributeName + " of dummy group " + groupname + ": " + values, expectedAttributeValues.length, values.size());
        for (Object expectedValue: expectedAttributeValues) {
            if (!values.contains(expectedValue)) {
                AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of dummy group "+groupname+
                        " but not found. Values found: "+values);
            }
        }
    }

    protected void assertDummyGroupMember(String dummyInstanceName, String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
    	DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		DummyGroup group = dummyResource.getGroupByName(dummyGroupName);
		assertNotNull("No dummy group "+dummyGroupName, group);
		IntegrationTestTools.assertGroupMember(group, accountId);
	}
	
	protected void assertDefaultDummyGroupMember(String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		assertDummyGroupMember(null, dummyGroupName, accountId);
	}

	protected void assertNoDummyGroupMember(String dummyInstanceName, String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
		DummyGroup group = dummyResource.getGroupByName(dummyGroupName);
		IntegrationTestTools.assertNoGroupMember(group, accountId);
	}
	
	protected void assertNoDefaultDummyGroupMember(String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		assertNoDummyGroupMember(null, dummyGroupName, accountId);
	}
    
	protected void assertDummyAccountNoAttribute(String dummyInstanceName, String username, String attributeName) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		assertTrue("Unexpected values for attribute " + attributeName + " of dummy account " + username + ": " + values, values == null || values.isEmpty());
	}
    
	protected Entry assertOpenDjAccount(String uid, String cn, Boolean active) throws DirectoryException {
		Entry entry = openDJController.searchByUid(uid);
		assertNotNull("OpenDJ accoun with uid "+uid+" not found", entry);
		openDJController.assertAttribute(entry, "cn", cn);
		if (active != null) {
			openDJController.assertActive(entry, active);
		}
		return entry;
	}
	
	protected void assertNoOpenDjAccount(String uid) throws DirectoryException {
		Entry entry = openDJController.searchByUid(uid);
		assertNull("Expected that OpenDJ account with uid " + uid + " will be gone, but it is still there", entry);
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
		TestUtil.assertBetween("Wrong user enableTimestamp in "+focus, 
				startTime, endTime, userDisableTimestamp);
	}

	protected void assertDisableTimestampFocus(PrismObject<? extends FocusType> focus, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		XMLGregorianCalendar userDisableTimestamp = focus.asObjectable().getActivation().getDisableTimestamp();
		TestUtil.assertBetween("Wrong user disableTimestamp in "+focus, 
				startTime, endTime, userDisableTimestamp);
	}
	
	protected void assertEnableTimestampShadow(PrismObject<? extends ShadowType> shadow, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		assertNotNull("No activation in "+shadow, activationType);
		XMLGregorianCalendar userDisableTimestamp = activationType.getEnableTimestamp();
		TestUtil.assertBetween("Wrong shadow enableTimestamp in "+shadow, 
				startTime, endTime, userDisableTimestamp);
	}

	protected void assertDisableTimestampShadow(PrismObject<? extends ShadowType> shadow, 
			XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
		XMLGregorianCalendar userDisableTimestamp = shadow.asObjectable().getActivation().getDisableTimestamp();
		TestUtil.assertBetween("Wrong shadow disableTimestamp in "+shadow, 
				startTime, endTime, userDisableTimestamp);
	}
	
	protected void assertDisableReasonShadow(PrismObject<? extends ShadowType> shadow, String expectedReason) {
		String disableReason = shadow.asObjectable().getActivation().getDisableReason();
		assertEquals("Wrong shadow disableReason in " + shadow, expectedReason, disableReason);
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

	protected void assertUserLdapPassword(PrismObject<UserType> user, String expectedPassword) throws EncryptionException {
		CredentialsType credentialsType = user.asObjectable().getCredentials();
		assertNotNull("No credentials in "+user, credentialsType);
		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password in "+user, passwordType);
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertLdapPassword(protectedStringType, expectedPassword, user);
	}

	protected void assertShadowLdapPassword(PrismObject<ShadowType> shadow, String expectedPassword) throws EncryptionException {
		CredentialsType credentialsType = shadow.asObjectable().getCredentials();
		assertNotNull("No credentials in "+shadow, credentialsType);
		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password in "+shadow, passwordType);
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertLdapPassword(protectedStringType, expectedPassword, shadow);
	}
	
	protected <O extends ObjectType> void assertLdapPassword(ProtectedStringType protectedStringType, String expectedPassword, PrismObject<O> source) throws EncryptionException {
		assertNotNull("No password value in "+source, protectedStringType);
		String decryptedUserPassword = protector.decryptString(protectedStringType);
		assertNotNull("Null password in " + source, decryptedUserPassword);
		if (decryptedUserPassword.startsWith("{") || decryptedUserPassword.contains("}")) {
			assertTrue("Wrong password hash in "+source+": "+decryptedUserPassword+", expected "+expectedPassword, ldapShaPasswordEncoder.isPasswordValid(decryptedUserPassword, expectedPassword, null));
		} else {
			assertEquals("Wrong password in "+source, expectedPassword, decryptedUserPassword);
		}
	}

	protected void assertGroupMember(DummyGroup group, String accountId) {
		IntegrationTestTools.assertGroupMember(group, accountId);
	}

	protected void assertNoGroupMember(DummyGroup group, String accountId) {
		IntegrationTestTools.assertNoGroupMember(group, accountId);
	}
	
	protected void assertNoGroupMembers(DummyGroup group) {
		IntegrationTestTools.assertNoGroupMembers(group);
	}
	
	protected void login(String principalName) throws ObjectNotFoundException {
		MidPointPrincipal principal = userProfileService.getPrincipal(principalName);
		login(principal);
	}
	
	protected void login(PrismObject<UserType> user) {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		login(principal);
	}
	
	protected void login(MidPointPrincipal principal) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
		securityContext.setAuthentication(authentication);
	}
	
	protected void loginSuperUser(String principalName) throws SchemaException, ObjectNotFoundException {
		MidPointPrincipal principal = userProfileService.getPrincipal(principalName);
		loginSuperUser(principal);
	}

	protected void loginSuperUser(PrismObject<UserType> user) throws SchemaException {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		loginSuperUser(principal);
	}

	protected void loginSuperUser(MidPointPrincipal principal) throws SchemaException {
		AuthorizationType superAutzType = new AuthorizationType();
		prismContext.adopt(superAutzType, RoleType.class, new ItemPath(RoleType.F_AUTHORIZATION));
		superAutzType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
		Authorization superAutz = new Authorization(superAutzType);
		Collection<Authorization> authorities = principal.getAuthorities();
		authorities.add(superAutz);
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
		securityContext.setAuthentication(authentication);
	}
	
	protected void loginAnonymous() {
		Authentication authentication = new AnonymousAuthenticationToken("foo", 
				"anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
	
	protected void assertLoggedInUser(String username) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication == null) {
			if (username == null) {
				return;
			} else {
				AssertJUnit.fail("Expected logged in user '"+username+"' but there was no authentication in the spring security context");
			}
		}
		Object principal = authentication.getPrincipal();
		if (principal == null) {
			if (username == null) {
				return;
			} else {
				AssertJUnit.fail("Expected logged in user '"+username+"' but there was no principal in the spring security context");
			}
		}
		if (principal instanceof MidPointPrincipal) {
			MidPointPrincipal midPointPrincipal = (MidPointPrincipal)principal;
			UserType user = midPointPrincipal.getUser();
			if (user == null) {
				if (username == null) {
					return;
				} else {
					AssertJUnit.fail("Expected logged in user '"+username+"' but there was no user in the spring security context");
				}
			}
			assertEquals("Wrong logged-in user", username, user.getName().getOrig());
		} else {
			AssertJUnit.fail("Expected logged in user '"+username+"' but there was unknown principal in the spring security context: "+principal);
		}
	}
	
	protected void resetAuthentication() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(null);
	}
	
	protected void assertNoAuthentication() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		assertNull("Unexpected authentication", securityContext.getAuthentication());
	}
	
	protected void displayAllUsers() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".displayAllUsers");
		OperationResult result = task.getResult();
		ResultHandler<UserType> handler = new ResultHandler<UserType>() {
			@Override
			public boolean handle(PrismObject<UserType> object, OperationResult parentResult) {
				display("User", object);
				return true;
			}
		};
		modelService.searchObjectsIterative(UserType.class, null, handler, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	/**
	 * Returns appropriate object synchronization settings for the class.
	 * Assumes single sync setting for now.
	 */
	protected ObjectSynchronizationType determineSynchronization(ResourceType resource, Class<UserType> type, String name) {
		SynchronizationType synchronization = resource.getSynchronization();
		if (synchronization == null) {
			return null;
		}
		List<ObjectSynchronizationType> objectSynchronizations = synchronization.getObjectSynchronization();
		if (objectSynchronizations.isEmpty()) {
			return null;
		}
		for (ObjectSynchronizationType objSyncType: objectSynchronizations) {
			QName focusTypeQName = objSyncType.getFocusType();
			if (focusTypeQName == null) {
				if (type != UserType.class) {
					continue;
				}
			} else {
				ObjectTypes focusType = ObjectTypes.getObjectTypeFromTypeQName(focusTypeQName);
				if (type != focusType.getClassDefinition()) {
					continue;
				}
			}
			if (name == null) {
				// we got it
				return objSyncType;
			} else {
				if (name.equals(objSyncType.getName())) {
					return objSyncType;
				}
			}
		}
		throw new IllegalArgumentException("Synchronization setting for "+type+" and name "+name+" not found in "+resource);
	}
	
	protected void assertShadowKindIntent(String shadowOid, ShadowKindType expectedKind,
			String expectedIntent) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertShadowKindIntent");
		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertShadowKindIntent(shadow, expectedKind, expectedIntent);
	}
	
	protected void assertShadowKindIntent(PrismObject<ShadowType> shadow, ShadowKindType expectedKind,
			String expectedIntent) {
		assertEquals("Wrong kind in " + shadow, expectedKind, shadow.asObjectable().getKind());
		assertEquals("Wrong intent in " + shadow, expectedIntent, shadow.asObjectable().getIntent());
	}
	
	protected PrismObject<UserType> getDefaultActor() {
		return null;
	}
	
	protected Task createTask(String operationName) {
		if (!operationName.contains(".")) {
			operationName = this.getClass().getName() + "." + operationName;
		}
		Task task = taskManager.createTaskInstance(operationName);
		PrismObject<UserType> defaultActor = getDefaultActor();
		if (defaultActor != null) {
			task.setOwner(defaultActor);
		}
		task.setChannel(DEFAULT_CHANNEL);
		return task;
	}
	
	protected void modifyRoleAddConstruction(String roleOid, long inducementId, String resourceOid) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleAddConstruction");
        OperationResult result = task.getResult();
		ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRedRef = new ObjectReferenceType();
        resourceRedRef.setOid(resourceOid);
		construction.setResourceRef(resourceRedRef);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, roleOid, 
        		new ItemPath(
        				new NameItemPathSegment(RoleType.F_INDUCEMENT),
        				new IdItemPathSegment(inducementId),
        				new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
        		prismContext, construction);
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	protected void modifyRoleAddInducementTarget(String roleOid, String targetOid, boolean reconcileAffected, Task task) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        if (task == null) {
            task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleAddInducementTarget");
        }
        OperationResult result = task.getResult();
        AssignmentType inducement = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(targetOid);
        inducement.setTargetRef(targetRef);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, roleOid, 
        		new ItemPath(new NameItemPathSegment(RoleType.F_INDUCEMENT)),
        		prismContext, inducement);
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setReconcileAffected(reconcileAffected);
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), options, task, result);
        result.computeStatus();
        if (reconcileAffected) {
            TestUtil.assertInProgressOrSuccess(result);
        } else {
            TestUtil.assertSuccess(result);
        }
	}

	protected Optional<AssignmentType> findAssignmentByTarget(PrismObject<? extends FocusType> focus, String targetOid) {
		return focus.asObjectable().getAssignment().stream()
				.filter(a -> a.getTargetRef() != null && targetOid.equals(a.getTargetRef().getOid()))
				.findFirst();
	}

	protected AssignmentType findAssignmentByTargetRequired(PrismObject<? extends FocusType> focus, String targetOid) {
		return findAssignmentByTarget(focus, targetOid)
				.orElseThrow(() -> new IllegalStateException("No assignment to " + targetOid + " in " + focus));
	}


	protected AssignmentType findInducementByTarget(String roleOid, String targetOid) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".findInducementByTarget");
        OperationResult result = task.getResult();
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, roleOid, null, task, result);
        for (AssignmentType inducement: role.asObjectable().getInducement()) {
        	ObjectReferenceType targetRef = inducement.getTargetRef();
        	if (targetRef != null && targetOid.equals(targetRef.getOid())) {
        		return inducement;
        	}
        }
        return null;
	}
	
	protected void modifyRoleDeleteInducementTarget(String roleOid, String targetOid) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleDeleteInducementTarget");
        OperationResult result = task.getResult();
        AssignmentType inducement = findInducementByTarget(roleOid, targetOid);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid, 
        		new ItemPath(new NameItemPathSegment(RoleType.F_INDUCEMENT)),
        		prismContext, inducement.asPrismContainerValue().clone());
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	protected void modifyRoleDeleteInducement(String roleOid, long inducementId, boolean reconcileAffected, Task task) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		if (task == null) {
            task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleDeleteInducement");
        }
        OperationResult result = task.getResult();
        
		AssignmentType inducement = new AssignmentType();
		inducement.setId(inducementId);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
				RoleType.F_INDUCEMENT, prismContext, inducement);
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setReconcileAffected(reconcileAffected);
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), options, task, result);
        result.computeStatus();
        if (reconcileAffected) {
            TestUtil.assertInProgressOrSuccess(result);
        } else {
            TestUtil.assertSuccess(result);
        }
	}
	
	protected void modifyUserAddAccount(String userOid, File accountFile, Task task, OperationResult result) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(accountFile);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
        
		modelService.executeChanges(deltas, null, task, result);
	}
	
	protected void assertAuthorized(MidPointPrincipal principal, String action) throws SchemaException {
		assertAuthorized(principal, action, null);
		assertAuthorized(principal, action, AuthorizationPhaseType.REQUEST);
		assertAuthorized(principal, action, AuthorizationPhaseType.EXECUTION);
	}

	protected void assertAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase) throws SchemaException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		try {
			assertTrue("AuthorizationEvaluator.isAuthorized: Principal "+principal+" NOT authorized for action "+action, 
					securityEnforcer.isAuthorized(action, phase, null, null, null, null));
			if (phase == null) {
				securityEnforcer.decide(SecurityContextHolder.getContext().getAuthentication(), createSecureObject(), 
					createConfigAttributes(action));
			}
		} finally {
			SecurityContextHolder.setContext(origContext);
		}
	}
	
	protected void assertNotAuthorized(MidPointPrincipal principal, String action) throws SchemaException {
		assertNotAuthorized(principal, action, null);
		assertNotAuthorized(principal, action, AuthorizationPhaseType.REQUEST);
		assertNotAuthorized(principal, action, AuthorizationPhaseType.EXECUTION);
	}
	
	protected void assertNotAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase) throws SchemaException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		boolean isAuthorized = securityEnforcer.isAuthorized(action, phase, null, null, null, null);
		SecurityContextHolder.setContext(origContext);
		assertFalse("AuthorizationEvaluator.isAuthorized: Principal " + principal + " IS authorized for action " + action + " (" + phase + ") but he should not be", isAuthorized);
	}
	
	protected void assertAuthorizations(PrismObject<UserType> user, String... expectedAuthorizations) throws ObjectNotFoundException {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		assertNotNull("No principal for "+user, principal);
		assertAuthorizations(principal, expectedAuthorizations);
	}
	
	protected void assertAuthorizations(MidPointPrincipal principal, String... expectedAuthorizations) {
		List<String> actualAuthorizations = new ArrayList<>();
		for (Authorization authorization: principal.getAuthorities()) {
			actualAuthorizations.addAll(authorization.getAction());
		}
		PrismAsserts.assertSets("Wrong authorizations in "+principal, actualAuthorizations, expectedAuthorizations);
	}

	
	protected void assertNoAuthorizations(PrismObject<UserType> user) throws ObjectNotFoundException {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		assertNotNull("No principal for "+user, principal);
		assertNoAuthorizations(principal);
	}
    
	protected void assertNoAuthorizations(MidPointPrincipal principal) {
		if (principal.getAuthorities() != null && !principal.getAuthorities().isEmpty()) {
			AssertJUnit.fail("Unexpected authorizations in "+principal+": "+principal.getAuthorities());
		}
	}
	
	protected void assertAdminGuiConfigurations(MidPointPrincipal principal, int expectedMenuLinks, 
			int expectedDashboardLinks, int expectedObjectForms) {
		AdminGuiConfigurationType adminGuiConfiguration = principal.getAdminGuiConfiguration();
		display("Admin GUI config for "+principal.getUsername(), adminGuiConfiguration);
		assertAdminGuiConfigurations(adminGuiConfiguration, expectedMenuLinks, expectedDashboardLinks, expectedObjectForms);
	}
	
	protected void assertAdminGuiConfigurations(AdminGuiConfigurationType adminGuiConfiguration, int expectedMenuLinks, 
			int expectedDashboardLinks, int expectedObjectForms) {
		assertNotNull("No admin GUI configuration", adminGuiConfiguration);
		assertEquals("Wrong number of menu links in",
				expectedMenuLinks, adminGuiConfiguration.getAdditionalMenuLink().size());
		assertEquals("Wrong number of menu links in",
				expectedDashboardLinks, adminGuiConfiguration.getUserDashboardLink().size());
		assertEquals("Wrong number of object forms in admin GUI configuration",
				expectedObjectForms, adminGuiConfiguration.getObjectForms().getObjectForm().size());
	}

	protected void createSecurityContext(MidPointPrincipal principal) {
		SecurityContext context = new SecurityContextImpl();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}
	
	protected Object createSecureObject() {
		return new FilterInvocation("/midpoint", "whateverServlet", "doSomething");
	}
	
	protected Collection<ConfigAttribute> createConfigAttributes(String action) {
		Collection<ConfigAttribute> attrs = new ArrayList<ConfigAttribute>();
		attrs.add(new SecurityConfig(action));
		return attrs;
	}
	
	protected <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object) throws SchemaException, ConfigurationException, ObjectNotFoundException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class+".getEditObjectDefinition");
		PrismObjectDefinition<O> editSchema = modelInteractionService.getEditObjectDefinition(object, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return editSchema;
	}
	
	protected <F extends FocusType> void assertRoleTypes(PrismObject<F> focus, String... expectedRoleTypes) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		assertRoleTypes(getAssignableRoleSpecification(focus), expectedRoleTypes);
	}
	
	protected <F extends FocusType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<F> focus) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".getAssignableRoleSpecification");
		RoleSelectionSpecification spec = modelInteractionService.getAssignableRoleSpecification(focus, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return spec;
	}

	protected void assertRoleTypes(RoleSelectionSpecification roleSpec, String... expectedRoleTypes) {
		assertNotNull("Null role spec", roleSpec);
        display("Role spec", roleSpec);
        List<? extends DisplayableValue<String>> roleTypes = roleSpec.getRoleTypes();
        if ((roleTypes == null || roleTypes.isEmpty()) && expectedRoleTypes.length == 0) {
        	return;
        }
        assertNotNull("Null roleTypes in roleSpec "+roleSpec, roleTypes);
        if (roleTypes.size() != expectedRoleTypes.length) {
        	AssertJUnit.fail("Expected role types "+Arrays.toString(expectedRoleTypes)+" but got "+roleTypes);
        }
        for(String expectedRoleType: expectedRoleTypes) {
        	boolean found = false;
        	for (DisplayableValue<String> roleTypeDval: roleTypes) {
        		if (expectedRoleType.equals(roleTypeDval.getValue())) {
        			found = true;
        			break;
        		}
        	}
        	if (!found) {
        		AssertJUnit.fail("Expected role type "+expectedRoleType+" but it was not present (got "+roleTypes+")");
        	}
        }
	}
	
	protected void assertEncryptedUserPassword(String userOid, String expectedClearPassword) throws EncryptionException, ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertEncryptedUserPassword");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEncryptedUserPassword(user, expectedClearPassword);
	}
	
	protected void assertEncryptedUserPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException {
		UserType userType = user.asObjectable();
		ProtectedStringType protectedActualPassword = userType.getCredentials().getPassword().getValue();
		String actualClearPassword = protector.decryptString(protectedActualPassword);
		assertEquals("Wrong password for "+user, expectedClearPassword, actualClearPassword);
	}

	protected void assertPasswordMetadata(PrismObject<UserType> user, boolean create, XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
		PrismContainer<MetadataType> metadataContainer = user.findContainer(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA));
		assertNotNull("No password metadata in "+user, metadataContainer);
		MetadataType metadataType = metadataContainer.getValue().asContainerable();
		assertMetadata("password metadata in "+user, metadataType, create, false, start, end, actorOid, channel);
	}
	
	protected <O extends ObjectType> void assertCreateMetadata(PrismObject<O> object, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		MetadataType metadataType = object.asObjectable().getMetadata();
		PrismObject<UserType> defaultActor = getDefaultActor();
		assertMetadata(object.toString(), metadataType, true, true, start, end, 
				defaultActor==null?null:defaultActor.getOid(), DEFAULT_CHANNEL);
	}
		
	protected <O extends ObjectType> void assertModifyMetadata(PrismObject<O> object, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		MetadataType metadataType = object.asObjectable().getMetadata();
		PrismObject<UserType> defaultActor = getDefaultActor();
		assertMetadata(object.toString(), metadataType, false, false, start, end, 
				defaultActor==null?null:defaultActor.getOid(), DEFAULT_CHANNEL);
	}
	
	protected <O extends ObjectType> void assertCreateMetadata(AssignmentType assignmentType, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		MetadataType metadataType = assignmentType.getMetadata();
		PrismObject<UserType> defaultActor = getDefaultActor();
		assertMetadata(assignmentType.toString(), metadataType, true, true, start, end, 
				defaultActor==null?null:defaultActor.getOid(), DEFAULT_CHANNEL);
	}
	
	protected void assertMetadata(String message, MetadataType metadataType, boolean create, boolean assertRequest, 
			XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
		assertNotNull("No metadata in " + message, metadataType);
		if (create) {
			TestUtil.assertBetween("Wrong create timestamp in " + message, start, end, metadataType.getCreateTimestamp());
			if (actorOid != null) {
				ObjectReferenceType creatorRef = metadataType.getCreatorRef();
				assertNotNull("No creatorRef in " + message, creatorRef);
				assertEquals("Wrong creatorRef OID in " + message, actorOid, creatorRef.getOid());
				if (assertRequest) {
					TestUtil.assertBetween("Wrong request timestamp in " + message, start, end, metadataType.getRequestTimestamp());
					ObjectReferenceType requestorRef = metadataType.getRequestorRef();
					assertNotNull("No requestorRef in " + message, requestorRef);
					assertEquals("Wrong requestorRef OID in " + message, actorOid, requestorRef.getOid());
				}
			}
			assertEquals("Wrong create channel in " + message, channel, metadataType.getCreateChannel());
		} else {
			if (actorOid != null) {
				ObjectReferenceType modifierRef = metadataType.getModifierRef();
				assertNotNull("No modifierRef in " + message, modifierRef);
				assertEquals("Wrong modifierRef OID in " + message, actorOid, modifierRef.getOid());
			}
			TestUtil.assertBetween("Wrong password modify timestamp in " + message, start, end, metadataType.getModifyTimestamp());
			assertEquals("Wrong modification channel in " + message, channel, metadataType.getModifyChannel());
		}
	}
	
	protected void assertDummyPassword(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		assertEquals("Wrong password in dummy '"+instance+"' account "+userId, expectedClearPassword, account.getPassword());
	}

	protected void reconcileUser(String oid, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		ObjectDelta<UserType> emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), ModelExecuteOptions.createReconcile(), task, result);
	}
	
	protected void reconcileOrg(String oid, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		ObjectDelta<OrgType> emptyDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), ModelExecuteOptions.createReconcile(), task, result);
	}

	protected void assertRefEquals(String message, ObjectReferenceType expected, ObjectReferenceType actual) {
		if (expected == null && actual == null) {
			return;
		}
		if (expected == null || actual == null) {
			fail(message + ": expected=" + expected + ", actual=" + actual);
		}
		PrismAsserts.assertRefEquivalent(message, expected.asReferenceValue(), actual.asReferenceValue());
	}
	
	protected void assertTaskClosed(PrismObject<TaskType> task) {
		assertEquals("Wrong executionStatus in "+task, TaskExecutionStatusType.CLOSED, task.asObjectable().getExecutionStatus());
	}
	
	protected List<AuditEventRecord> getAllAuditRecords(OperationResult result) throws SecurityViolationException, SchemaException {
		Map<String,Object> params = new HashMap<>();
		return modelAuditService.listRecords("from RAuditEventRecord as aer order by aer.timestamp asc", params, result);
	}
	
	protected List<AuditEventRecord> getObjectAuditRecords(String oid) throws SecurityViolationException, SchemaException {
		OperationResult result = new OperationResult("getObjectAuditRecords");
		return getObjectAuditRecords(oid, result);
	}
	
	protected List<AuditEventRecord> getObjectAuditRecords(String oid, OperationResult result) throws SecurityViolationException, SchemaException {
		Map<String,Object> params = new HashMap<>();
		params.put("targetOid", oid);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.targetOid = :targetOid) order by aer.timestamp asc", 
        		params, result);
	}

	protected List<AuditEventRecord> getParamAuditRecords(String paramName, String paramValue, OperationResult result) throws SecurityViolationException, SchemaException {
		Map<String,Object> params = new HashMap<>();
		params.put("paramName", paramName);
		params.put("paramValue", paramValue);
		return modelAuditService.listRecords("from RAuditEventRecord as aer left join aer.propertyValues as pv where (pv.name = :paramName and pv.value = :paramValue) order by aer.timestamp asc",
        		params, result);
	}

	protected List<AuditEventRecord> getAuditRecordsFromTo(XMLGregorianCalendar from, XMLGregorianCalendar to) throws SecurityViolationException, SchemaException {
		OperationResult result = new OperationResult("getAuditRecordsFromTo");
		return getAuditRecordsFromTo(from, to, result);
	}
	
	protected List<AuditEventRecord> getAuditRecordsFromTo(XMLGregorianCalendar from, XMLGregorianCalendar to, OperationResult result) throws SecurityViolationException, SchemaException {
		Map<String,Object> params = new HashMap<>();
		params.put("from", from);
		params.put("to", to);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.timestamp >= :from) and (aer.timestamp <= :to) order by aer.timestamp asc", 
        		params, result);
	}

	protected void checkUserApprovers(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
		checkApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getModifyApproverRef(), result);
	}

	protected void checkUserApproversForCreate(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
		checkApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getCreateApproverRef(), result);
	}

	protected void checkApproversForCreate(Class<? extends ObjectType> clazz, String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
		PrismObject<? extends ObjectType> object = repositoryService.getObject(clazz, oid, null, result);
		checkApprovers(object, expectedApprovers, object.asObjectable().getMetadata().getCreateApproverRef(), result);
	}

	protected void checkApprovers(PrismObject<? extends ObjectType> object, List<String> expectedApprovers, List<ObjectReferenceType> realApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
		HashSet<String> realApproversSet = new HashSet<String>();
		for (ObjectReferenceType approver : realApprovers) {
			realApproversSet.add(approver.getOid());
			assertEquals("Unexpected target type in approverRef", UserType.COMPLEX_TYPE, approver.getType());
		}
		assertEquals("Mismatch in approvers in metadata", new HashSet(expectedApprovers), realApproversSet);
	}

	protected PrismObject<UserType> findUserInRepo(String name, OperationResult result) throws SchemaException {
		List<PrismObject<UserType>> users = findUserInRepoUnchecked(name, result);
		assertEquals("Didn't find exactly 1 user object with name " + name, 1, users.size());
		return users.get(0);
	}

	protected List<PrismObject<UserType>> findUserInRepoUnchecked(String name, OperationResult result) throws SchemaException {
		ObjectQuery q = QueryBuilder.queryFor(UserType.class, prismContext).item(UserType.F_NAME).eqPoly(name).matchingOrig().build();
		return repositoryService.searchObjects(UserType.class, q, null, result);
	}

	protected List<PrismObject<RoleType>> findRoleInRepoUnchecked(String name, OperationResult result) throws SchemaException {
		ObjectQuery q = QueryBuilder.queryFor(RoleType.class, prismContext).item(RoleType.F_NAME).eqPoly(name).matchingOrig().build();
		return repositoryService.searchObjects(RoleType.class, q, null, result);
	}

	protected <F extends FocusType> void assertFocusModificationSanity(ModelContext<F> context) throws JAXBException {
		ModelElementContext<F> focusContext = context.getFocusContext();
	    PrismObject<F> focusOld = focusContext.getObjectOld();
	    if (focusOld == null) {
	    	return;
	    }
	    ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
	    if (focusPrimaryDelta != null) {
		    assertEquals("No OID in old focus object", focusOld.getOid(), focusPrimaryDelta.getOid());
		    assertEquals(ChangeType.MODIFY, focusPrimaryDelta.getChangeType());
		    assertNull(focusPrimaryDelta.getObjectToAdd());
		    for (ItemDelta itemMod : focusPrimaryDelta.getModifications()) {
		        if (itemMod.getValuesToDelete() != null) {
		            Item property = focusOld.findItem(itemMod.getPath());
		            assertNotNull("Deleted item " + itemMod.getParentPath() + "/" + itemMod.getElementName() + " not found in focus", property);
		            for (Object valueToDelete : itemMod.getValuesToDelete()) {
		                if (!property.containsRealValue((PrismValue) valueToDelete)) {
		                    display("Deleted value " + valueToDelete + " is not in focus item " + itemMod.getParentPath() + "/" + itemMod.getElementName());
		                    display("Deleted value", valueToDelete);
		                    display("HASHCODE: " + valueToDelete.hashCode());
		                    for (Object value : property.getValues()) {
		                        display("Existing value", value);
		                        display("EQUALS: " + valueToDelete.equals(value));
		                        display("HASHCODE: " + value.hashCode());
		                    }
		                    fail("Deleted value " + valueToDelete + " is not in focus item " + itemMod.getParentPath() + "/" + itemMod.getElementName());
		                }
		            }
		        }

		    }
	    }
	}

	protected PrismObject<UserType> getUserFromRepo(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
		return repositoryService.getObject(UserType.class, oid, null, result);
	}

	protected boolean assignmentExists(List<AssignmentType> assignmentList, String targetOid) {
		for (AssignmentType assignmentType : assignmentList) {
			if (assignmentType.getTargetRef() != null && targetOid.equals(assignmentType.getTargetRef().getOid())) {
				return true;
			}
		}
		return false;
	}

	// just guessing (good enough for testing)
	protected boolean isH2() {
		Task task = createTask("isH2");
		RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, task.getResult());
		return diag.isEmbedded() || "org.h2.Driver".equals(diag.getDriverShortName());
	}

	protected void assertNoPostponedOperation(PrismObject<ShadowType> shadow) {
		Item<PrismValue, ItemDefinition> objectChangeItem = shadow.findItem(ShadowType.F_OBJECT_CHANGE);
		if (objectChangeItem != null) {
			AssertJUnit.fail("Expected no postponed operation in "+shadow+", but found one: "+objectChangeItem);
		}
	}

	protected String addAndRecomputeUser(File file, Task initTask, OperationResult initResult) throws Exception {
		String oid = repoAddObjectFromFile(file, initResult).getOid();
		recomputeUser(oid, initTask, initResult);
		display("User " + file, getUser(oid));
		return oid;
	}

	protected String addAndRecompute(File file, Task task, OperationResult result) throws Exception {
		PrismObject<ObjectType> object = repoAddObjectFromFile(file, result);
		modelService.recompute(object.asObjectable().getClass(), object.getOid(), task, result);
		display("Object: " + file, getObject(object.asObjectable().getClass(), object.getOid()));
		return object.getOid();
	}

	protected void assertAuditReferenceValue(List<AuditEventRecord> events, String refName, String oid, QName type, String name) {
		if (events.size() != 1) {
			display("Events", events);
			assertEquals("Wrong # of events", 1, events.size());
		}
		assertAuditReferenceValue(events.get(0), refName, oid, type, name);
	}

	protected void assertAuditReferenceValue(AuditEventRecord event, String refName, String oid, QName type, String name) {
		Set<AuditReferenceValue> values = event.getReferenceValues(refName);
		assertEquals("Wrong # of reference values of '" + refName + "'", 1, values.size());
		AuditReferenceValue value = values.iterator().next();
		assertEquals("Wrong OID", oid, value.getOid());
		assertEquals("Wrong type", type, value.getType());
		assertEquals("Wrong name", name, PolyString.getOrig(value.getTargetName()));
	}

	protected void assertAuditTarget(AuditEventRecord event, String oid, QName type, String name) {
		PrismReferenceValue target = event.getTarget();
		assertNotNull("No target", target);
		assertEquals("Wrong OID", oid, target.getOid());
		assertEquals("Wrong type", type, target.getTargetType());
		assertEquals("Wrong name", name, PolyString.getOrig(target.getTargetName()));
	}

	protected List<AuditEventRecord> filter(List<AuditEventRecord> records, AuditEventStage stage) {
		return records.stream()
				.filter(r -> r.getEventStage() == stage)
				.collect(Collectors.toList());
	}

	protected List<AuditEventRecord> filter(List<AuditEventRecord> records, AuditEventType type, AuditEventStage stage) {
		return records.stream()
				.filter(r -> r.getEventType() == type && r.getEventStage() == stage)
				.collect(Collectors.toList());
	}
}
