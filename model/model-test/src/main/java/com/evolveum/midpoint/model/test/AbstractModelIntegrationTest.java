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

import static com.evolveum.midpoint.schema.GetOperationOptions.retrieveItemsNamed;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
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
import com.evolveum.midpoint.model.common.stringpolicy.UserValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConflictResolutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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

	@Autowired protected ModelService modelService;
	@Autowired protected ModelInteractionService modelInteractionService;
	@Autowired protected ModelDiagnosticService modelDiagnosticService;
	@Autowired protected ModelAuditService modelAuditService;
	@Autowired protected ModelPortType modelWeb;
	@Autowired protected RepositoryService repositoryService;
	@Autowired protected SystemObjectCache systemObjectCache;
	@Autowired protected ProvisioningService provisioningService;
	@Autowired protected HookRegistry hookRegistry;
	@Autowired protected Clock clock;
	@Autowired protected PrismContext prismContext;
    @Autowired protected DummyTransport dummyTransport;
	@Autowired protected SecurityEnforcer securityEnforcer;
	@Autowired protected SecurityContextManager securityContextManager;
	@Autowired protected MidpointFunctions libraryMidpointFunctions;
	@Autowired protected ValuePolicyProcessor valuePolicyProcessor;
	
	@Autowired(required = false)
	@Qualifier("modelObjectResolver")
	protected ObjectResolver modelObjectResolver;

	@Autowired(required = false)
	protected NotificationManager notificationManager;

	@Autowired(required = false)
	protected UserProfileService userProfileService;

	protected DummyResourceCollection dummyResourceCollection;

	protected DummyAuditService dummyAuditService;

	protected boolean verbose = false;

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
		InternalsConfig.reset();
		InternalsConfig.setAvoidLoggingChange(true);
		// Make sure the checks are turned on
		InternalsConfig.turnOnAllChecks();
        // By default, notifications are turned off because of performance implications. Individual tests turn them on for themselves.
        if (notificationManager != null) {
            notificationManager.setDisabled(true);
        }
	}

	@Override
	public void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
		super.postInitSystem(initTask, initResult);
		if (dummyResourceCollection != null) {
			dummyResourceCollection.resetResources();
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
	
	protected DummyResourceContoller initDummyResourceAd(String name, File resourceFile, String resourceOid,
			Task task, OperationResult result) throws Exception {
		return initDummyResource(name, resourceFile, resourceOid, controller -> controller.extendSchemaAd(), task, result);
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
		subResult.addParam("filename", file.getPath());
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

	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, String filename, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return importAndGetObjectFromFile(type, new File(filename), oid, task, result);
	}

	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(Class<T> type, File file, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		importObjectFromFile(file, result);
		OperationResult importResult = result.getLastSubresult();
		TestUtil.assertSuccess("Import of "+file+" has failed", importResult);
		return modelService.getObject(type, oid, null, task, result);
	}
	
	protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFileIgnoreWarnings(Class<T> type, File file, String oid, Task task, OperationResult result) throws FileNotFoundException, ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		importObjectFromFile(file, result);
		OperationResult importResult = result.getLastSubresult();
		OperationResultStatus status = importResult.getStatus();
		if (status == OperationResultStatus.FATAL_ERROR || status == OperationResultStatus.PARTIAL_ERROR) {
			fail("Import of "+file+" has failed: "+importResult.getMessage());
		}
		return modelService.getObject(type, oid, null, task, result);
	}

    /**
     * This is not the real thing. It is just for the tests.
     */
    protected void applyResourceSchema(ShadowType accountType, ResourceType resourceType) throws SchemaException {
    	IntegrationTestTools.applyResourceSchema(accountType, resourceType, prismContext);
    }


    protected void assertUsers(int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	assertObjects(UserType.class, expectedNumberOfUsers);
    }

    protected void assertRoles(int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	assertObjects(RoleType.class, expectedNumberOfUsers);
    }

    protected <O extends ObjectType> void assertObjects(Class<O> type, int expectedNumberOfUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertEquals("Unexpected number of "+type.getSimpleName()+"s", expectedNumberOfUsers, getObjectCount(type));
    }

    protected <O extends ObjectType> int getObjectCount(Class<O> type) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".assertObjects");
        OperationResult result = task.getResult();
    	List<PrismObject<O>> users = modelService.searchObjects(type, null, null, task, result);
        if (verbose) display(type.getSimpleName()+"s", users);
        return users.size();
    }

    protected <O extends ObjectType> void searchObjectsIterative(Class<O> type, ObjectQuery query, Consumer<PrismObject<O>> handler, Integer expectedNumberOfObjects) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".assertObjects");
        OperationResult result = task.getResult();
        final MutableInt count = new MutableInt(0);
		// Cannot convert to lambda here. Java does not want to understand the generic types properly.
		SearchResultMetadata searchMetadata = modelService.searchObjectsIterative(type, query, new ResultHandler<O>() {
				@Override
				public boolean handle(PrismObject<O> object, OperationResult oresult) {
					count.increment();
					if (handler != null) {
						handler.accept(object);
					}
					return true;
				}
			}, null, task, result);
        if (verbose) display(type.getSimpleName()+"s", count.getValue());
        assertEquals("Unexpected number of "+type.getSimpleName()+"s", expectedNumberOfObjects, count.getValue());
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
	
	protected <F extends FocusType> void assertEffectiveStatus(PrismObject<F> focus, ActivationStatusType expected) {
		ActivationType activation = focus.asObjectable().getActivation();
		assertNotNull("No activation in "+focus, activation);
		assertEquals("Unexpected effective activation status in "+focus, expected, activation.getEffectiveStatus());
	}

	protected void modifyUserReplace(String userOid, QName propertyName, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(userOid, new ItemPath(propertyName), task, result, newRealValue);
	}

	protected void modifyUserReplace(String userOid, QName propertyName, ModelExecuteOptions options, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(userOid, new ItemPath(propertyName), options, task, result, newRealValue);
	}

	protected void modifyUserReplace(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserReplace(userOid, propertyPath, null, task, result, newRealValue);
	}

	protected void modifyUserReplace(String userOid, ItemPath propertyPath, ModelExecuteOptions options, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(userOid, propertyPath, newRealValue);
		executeChanges(objectDelta, options, task, result);
	}

	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, QName propertyName, Task task, OperationResult result, Object... newRealValue) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceProperty(type, oid, new ItemPath(propertyName), task, result, newRealValue);
	}

	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Task task, OperationResult result, Object... newRealValue) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceProperty(type, oid, new ItemPath(propertyName), options, task, result, newRealValue);
	}

	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyObjectReplaceProperty(type, oid, propertyPath, null, task, result, newRealValue);
	}

	protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, ItemPath propertyPath, ModelExecuteOptions options, Task task, OperationResult result, Object... newRealValue)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, propertyPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		modelService.executeChanges(deltas, options, task, result);
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

	protected void recomputeUser(String userOid) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		Task task = createTask("recomputeUser");
		OperationResult result = task.getResult();
		modelService.recompute(UserType.class, userOid, null, task, result);
		assertSuccess(result);
	}
	
	protected void recomputeUser(String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		modelService.recompute(UserType.class, userOid, null, task, result);
	}

	protected void recomputeFocus(Class<? extends FocusType> clazz, String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		modelService.recompute(clazz, userOid, null, task, result);
	}

	protected void recomputeUser(String userOid, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException  {
		modelService.recompute(UserType.class, userOid, options, task, result);
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
		assignRole(UserType.class, userOid, roleOid, (ActivationType) null, task, result);
	}

	protected void assignRole(String userOid, String roleOid, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
		assignRole(UserType.class, userOid, roleOid, (ActivationType) null, task, options, result);
	}

	protected void assignRole(Class<? extends FocusType> focusClass, String focusOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
		assignRole(focusClass, focusOid, roleOid, (ActivationType) null, task, result);
	}

	protected void assignRole(String userOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, result);
	}

	protected void unassignRole(String userOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, false, result);
	}

	protected void assignRole(Class<? extends FocusType> focusClass, String focusOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(focusClass, focusOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, result);
	}

	protected void assignRole(Class<? extends FocusType> focusClass, String focusOid, String roleOid, ActivationType activationType, Task task, ModelExecuteOptions options, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(focusClass, focusOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, options, result);
	}

	protected void assignFocus(Class<? extends FocusType> focusClass, String focusOid, QName targetType, String targetOid, QName relation, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(focusClass, focusOid, targetOid, targetType, relation, task, null, activationType, true, result);
	}

	protected void assignRole(String userOid, String roleOid, QName relation, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
	PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, true, result);
	}

	protected void assignRole(String userOid, String roleOid, QName relation, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, true, options, result);
	}

	protected void assignRole(String userOid, String roleOid, QName relation) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assignRole");
		OperationResult result = task.getResult();
		assignRole(userOid, roleOid, relation, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void assignRole(String userOid, String roleOid, QName relation, Consumer<AssignmentType> modificationBlock, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, modificationBlock, true, result);
	}

	protected void unassignRole(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		Task task = createTask("unassignRole");
		OperationResult result = task.getResult();
		unassignRole(userOid, roleOid, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void unassignRoleByAssignmentValue(PrismObject<? extends FocusType> focus, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		AssignmentType assignment = findAssignmentByTargetRequired(focus, roleOid);
		ObjectDelta<? extends FocusType> delta = DeltaBuilder.deltaFor(focus.getCompileTimeClass(), prismContext)
				.item(FocusType.F_ASSIGNMENT)
				.delete(assignment.clone())
				.asObjectDeltaCast(focus.getOid());
		modelService.executeChanges(singleton(delta), null, task, result);
	}

	protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>)null, false, result);
	}

	protected void unassignRole(String userOid, String roleOid, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, false, options, result);
	}

	protected void assignRole(String userOid, String roleOid, PrismContainer<?> extension, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, true, result);
	}

	protected void assignRole(String userOid, String roleOid, PrismContainer<?> extension, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(UserType.class, userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, null, true, options, result);
	}

	protected void unassignRole(String userOid, String roleOid, PrismContainer<?> extension, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, false, result);
	}

	protected void unassignRole(String userOid, String roleOid, PrismContainer<?> extension, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(UserType.class, userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, null, false, options, result);
	}

	protected void unassignRole(String userOid, String roleOid, QName relation, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
			modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, false, result);
	}

	protected void unassignRole(String userOid, String roleOid, QName relation, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
		SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
		PolicyViolationException, SecurityViolationException {
			modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, relation, task, null, null, false, options, result);
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
					PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
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

	protected <F extends FocusType> void assignOrg(Class<F> focusType, String focusOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		assignOrg(focusType, focusOid, orgOid, null, task, result);
	}

	protected void assignOrg(String userOid, String orgOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		assignOrg(userOid, orgOid,  SchemaConstants.ORG_DEFAULT);
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

	protected <F extends FocusType> void assignOrg(Class<F> focusType, String focusOid, String orgOid, QName relation, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(focusType, focusOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, (Consumer<AssignmentType>)null, true, result);
	}

	protected void unassignOrg(String userOid, String orgOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		unassignOrg(userOid, orgOid, SchemaConstants.ORG_DEFAULT);
	}

	protected void unassignOrg(String userOid, String orgOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
			CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		unassignOrg(userOid, orgOid, null, task, result);
	}

	protected void unassignOrg(String userOid, String orgOid, QName relation) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".unassignOrg");
		OperationResult result = task.getResult();
		unassignOrg(userOid, orgOid, relation, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
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
    	modifyUserAssignment(userOid, roleOid, refType, relation, task, extension, activationType, add, null, result);
	}

	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task,
			PrismContainer<?> extension, ActivationType activationType, boolean add, ModelExecuteOptions options,
			OperationResult result)
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, refType, relation, extension, activationType, add);
		executeChanges(userDelta, options, task, result);
	}

	protected <F extends FocusType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
			PrismContainer<?> extension, ActivationType activationType, boolean add, OperationResult result)
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
    	modifyFocusAssignment(focusClass, focusOid, roleOid, refType, relation, task, extension, activationType, add, null, result);
	}

	protected <F extends FocusType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
			PrismContainer<?> extension, ActivationType activationType, boolean add, ModelExecuteOptions options, OperationResult result)
			throws ObjectNotFoundException,
			SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
			PolicyViolationException, SecurityViolationException {
		ObjectDelta<F> delta = createAssignmentFocusDelta(focusClass, focusOid, roleOid, refType, relation, extension, activationType, add);
		executeChanges(delta, options, task, result);
	}

	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task,
			Consumer<AssignmentType> modificationBlock, boolean add, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(UserType.class, userOid, roleOid, refType, relation, task, modificationBlock, add, result);
	}

	protected void modifyUserAssignment(String userOid, String roleOid, QName refType, QName relation, Task task,
			Consumer<AssignmentType> modificationBlock, boolean add, ModelExecuteOptions options, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyFocusAssignment(UserType.class, userOid, roleOid, refType, relation, task, modificationBlock, add, options, result);
	}

	protected <F extends FocusType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
			Consumer<AssignmentType> modificationBlock, boolean add, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
    	modifyFocusAssignment(focusClass, focusOid, roleOid, refType, relation, task, modificationBlock, add, null, result);
	}

	protected <F extends FocusType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
			Consumer<AssignmentType> modificationBlock, boolean add, ModelExecuteOptions options, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<F> focusDelta = createAssignmentFocusDelta(focusClass, focusOid, roleOid, refType, relation, modificationBlock, add);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(focusDelta);
		executeChanges(focusDelta, options, task, result);
	}
		
	protected <F extends FocusType> void deleteFocusAssignmentEmptyDelta(PrismObject<F> existingFocus, String targetOid, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		deleteFocusAssignmentEmptyDelta(existingFocus, targetOid, null, null, task, result);
	}
	
	protected <F extends FocusType> void deleteFocusAssignmentEmptyDelta(PrismObject<F> existingFocus, String targetOid, QName relation, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		deleteFocusAssignmentEmptyDelta(existingFocus, targetOid, relation, null, task, result);
	}
	
	protected <F extends FocusType> void deleteFocusAssignmentEmptyDelta(PrismObject<F> existingFocus, String targetOid, QName relation, 
			ModelExecuteOptions options, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<F> focusDelta = createAssignmentFocusEmptyDeleteDelta(existingFocus, targetOid, relation);
		executeChanges(focusDelta, options, task, result);
	}
	
	protected <F extends FocusType> void unassign(Class<F> focusClass, String focusOid, long assignmentId, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		unassign(focusClass, focusOid, assignmentId, null, task, result);
	}

	protected <F extends FocusType> void unassign(Class<F> focusClass, String focusOid, long assignmentId, ModelExecuteOptions options, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		unassign(focusClass, focusOid, createAssignmentIdOnly(assignmentId), options, task, result);
	}
	
	protected <F extends FocusType> void unassign(Class<F> focusClass, String focusOid, AssignmentType currentAssignment, ModelExecuteOptions options, Task task, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
		assignmentDelta.addValuesToDelete(currentAssignment.asPrismContainerValue().clone());
		modifications.add(assignmentDelta);
		ObjectDelta<F> focusDelta = ObjectDelta.createModifyDelta(focusOid, modifications, focusClass, prismContext);
		executeChanges(focusDelta, options, task, result);
	}
	
	/**
	 * Executes unassign delta by removing each assignment individually by id.
	 */
	protected <F extends FocusType> void unassignAll(PrismObject<F> focusBefore, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		executeChanges(createUnassignAllDelta(focusBefore), null, task, result);
	}

	/**
	 * Creates unassign delta by removing each assignment individually by id.
	 */
	protected <F extends FocusType> ObjectDelta<F> createUnassignAllDelta(PrismObject<F> focusBefore) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		for (AssignmentType assignmentType: focusBefore.asObjectable().getAssignment()) {
			modifications.add((createAssignmentModification(assignmentType.getId(), false)));
		}
		return ObjectDelta.createModifyDelta(focusBefore.getOid(), modifications, focusBefore.getCompileTimeClass(), prismContext);
	}

	/**
	 * Executes assignment replace delta with empty values.
	 */
	protected void unassignAllReplace(String userOid, Task task, OperationResult result)
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
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
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
	
	protected ContainerDelta<AssignmentType> createAssignmentModification(long id, boolean add) throws SchemaException {
		ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
		cval.setId(id);
		if (add) {
			assignmentDelta.addValueToAdd(cval);
		} else {
			assignmentDelta.addValueToDelete(cval);
		}
		return assignmentDelta;
	}
	
	protected <F extends FocusType> ContainerDelta<AssignmentType> createAssignmentEmptyDeleteModification(PrismObject<F> existingFocus, String roleOid, QName relation) throws SchemaException {
		AssignmentType existingAssignment = findAssignment(existingFocus, roleOid, relation);
		return createAssignmentModification(existingAssignment.getId(), false);
	}

	protected <F extends FocusType> AssignmentType findAssignment(PrismObject<F> existingFocus, String targetOid, QName relation) {
		for (AssignmentType assignmentType : existingFocus.asObjectable().getAssignment()) {
			if (assignmentMatches(assignmentType, targetOid, relation)) {
				return assignmentType;
			}
		}
		return null;
	}

	protected boolean assignmentMatches(AssignmentType assignmentType, String targetOid, QName relation) {
		ObjectReferenceType targetRef = assignmentType.getTargetRef();
		if (targetRef == null) {
			return false;
		}
		return referenceMatches(targetRef, targetOid, relation);
	}

	private boolean referenceMatches(ObjectReferenceType ref, String targetOid, QName relation) {
		if (targetOid != null && !targetOid.equals(ref.getOid())) {
			return false;
		}
		if (relation != null && !QNameUtil.match(relation, ref.getRelation())) {
			return false;
		}
		return true;
	}

	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation,
			PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(userOid, modifications, UserType.class, prismContext);
		return userDelta;
	}

	protected <F extends FocusType> ObjectDelta<F> createAssignmentFocusDelta(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation,
			PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
		return ObjectDelta.createModifyDelta(focusOid, modifications, focusClass, prismContext);
	}

	protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation,
			Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
		return createAssignmentFocusDelta(UserType.class, userOid, roleOid, refType, relation, modificationBlock, add);
	}

	protected <F extends FocusType> ObjectDelta<F> createAssignmentFocusDelta(Class<F> focusClass, String userOid, String roleOid, QName refType, QName relation,
			Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(roleOid, refType, relation, modificationBlock, add)));
		return ObjectDelta.createModifyDelta(userOid, modifications, focusClass, prismContext);
	}

	protected <F extends FocusType> ObjectDelta<F> createAssignmentFocusEmptyDeleteDelta(PrismObject<F> existingFocus, String roleOid, QName relation) throws SchemaException {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentEmptyDeleteModification(existingFocus, roleOid, relation)));
		return ObjectDelta.createModifyDelta(existingFocus.getOid(), modifications, existingFocus.getCompileTimeClass(), prismContext);
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
		PrismContainerValue<AssignmentType> cval = new PrismContainerValue<>(prismContext);
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

	protected void assignParametricRole(String userOid, String roleOid, String orgOid, String tenantOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assignParametricRole(userOid, roleOid, orgOid, tenantOid, null, task, result);
	}

	protected void assignParametricRole(String userOid, String roleOid, String orgOid, String tenantOid, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(
				createParametricAssignmentDelta(userOid, roleOid, orgOid, tenantOid, true));
		modelService.executeChanges(deltas, options, task, result);
	}

	protected void unassignParametricRole(String userOid, String roleOid, String orgOid, String tenantOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		unassignParametricRole(userOid, roleOid, orgOid, tenantOid, null, task, result);
	}

	protected void unassignParametricRole(String userOid, String roleOid, String orgOid, String tenantOid, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(
				createParametricAssignmentDelta(userOid, roleOid, orgOid, tenantOid, false));
		modelService.executeChanges(deltas, options, task, result);
	}

	protected void assertAssignees(String targetOid, int expectedAssignees) throws SchemaException {
		assertAssignees(targetOid, SchemaConstants.ORG_DEFAULT, expectedAssignees);
	}

	protected void assertAssignees(String targetOid, QName relation, int expectedAssignees) throws SchemaException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName()+".assertAssignees");
		int count = countAssignees(targetOid, relation, result);
		if (count != expectedAssignees) {
			SearchResultList<PrismObject<FocusType>> assignees = listAssignees(targetOid, result);
			AssertJUnit.fail("Unexpected number of assignees of "+targetOid+" as '"+relation+"', expected "+expectedAssignees+", but was " + count+ ": "+assignees);
		}

	}

	protected int countAssignees(String targetOid, OperationResult result) throws SchemaException {
		return countAssignees(targetOid, SchemaConstants.ORG_DEFAULT, result);
	}

	protected int countAssignees(String targetOid, QName relation, OperationResult result) throws SchemaException {
		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setOid(targetOid);
		refVal.setRelation(relation);
		ObjectQuery query = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(refVal)
				.build();
		return repositoryService.countObjects(FocusType.class, query, null, result);
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
        ContainerDelta<ConstructionType> acDelta = new ContainerDelta<>(new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT), new IdItemPathSegment(id), new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)), pcd, prismContext);
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
		display("Executing delta", objectDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		return modelService.executeChanges(deltas, options, task, result);
	}

	protected <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesAssertSuccess(ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Collection<ObjectDeltaOperation<? extends ObjectType>> rv = executeChanges(objectDelta, options, task, result);
		assertSuccess(result);
		return rv;
	}

	protected void assignAccount(String userOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assignAccount");
		OperationResult result = task.getResult();
		assignAccount(userOid, resourceOid, intent, task, result);
		assertSuccess(result);
	}

	protected void assignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, true);
		executeChanges(userDelta, null, task, result);
	}

	protected void unassignAccount(String userOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assignAccount");
		OperationResult result = task.getResult();
		unassignAccount(userOid, resourceOid, intent, task, result);
		assertSuccess(result);
	}
	
	protected void unassignAccount(String userOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(userOid, resourceOid, intent, false);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, null, task, result);
	}

	protected PrismObject<UserType> getUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected <O extends ObjectType> PrismObject<O> findObjectByName(Class<O> type, String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected PrismObject<UserType> findUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return findObjectByName(UserType.class, username);
	}

	protected RoleType getRoleSimple(String oid) {
		try {
			return getRole(oid).asObjectable();
		} catch (CommonException e) {
			throw new SystemException("Unexpected exception while getting role " + oid + ": " + e.getMessage(), e);
		}
	}

    protected PrismObject<RoleType> getRole(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getRole");
        OperationResult result = task.getResult();
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, oid, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess("getObject(Role) result not success", result);
        return role;
    }

	protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".findAccountByUsername");
        OperationResult result = task.getResult();
        return findAccountByUsername(username, resource, task, result);
	}

	protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = createAccountShadowQuery(username, resource);
		List<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
		if (accounts.isEmpty()) {
			return null;
		}
		assert accounts.size() == 1 : "Too many accounts found for username "+username+" on "+resource+": "+accounts;
		return accounts.iterator().next();
	}

	protected Collection<PrismObject<ShadowType>> listAccounts(PrismObject<ResourceType> resource,
			Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

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

	protected PrismObject<ShadowType> getShadowModel(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return getShadowModel(accountOid, false, true);
	}

	protected PrismObject<ShadowType> getShadowModelNoFetch(String accountOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return getShadowModel(accountOid, true, true);
	}

	protected PrismObject<ShadowType> getShadowModel(String accountOid, boolean noFetch, boolean assertSuccess) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected <O extends ObjectType> void assertNoObject(Class<O> type, String oid) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".assertNoObject");
		assertNoObject(type, oid, task, task.getResult());
	}

	protected <O extends ObjectType> void assertNoObject(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected String getLinkRefOid(String userOid, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return getLinkRefOid(getUser(userOid), resourceOid);
	}

	protected <F extends FocusType> String getLinkRefOid(PrismObject<F> focus, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismReferenceValue linkRef = getLinkRef(focus, resourceOid);
		if (linkRef == null) {
			return null;
		}
        return linkRef.getOid();
	}

	protected <F extends FocusType> PrismReferenceValue getLinkRef(PrismObject<F> focus, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected <F extends FocusType> String getLinkRefOid(PrismObject<F> focus, String resourceOid, ShadowKindType kind, String intent) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

	protected String assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String accountOid = getLinkRefOid(user, resourceOid);
		assertNotNull("User " + user + " has no account on resource " + resourceOid, accountOid);
		return accountOid;
	}

	protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertAccounts");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertLinks(user, numAccounts);
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

	protected AssignmentType getUserAssignment(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return getAssignment(getUser(userOid), roleOid);
	}

	protected AssignmentType getUserAssignment(String userOid, String roleOid, QName relation)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = getUser(userOid);
		List<AssignmentType> assignments = user.asObjectable().getAssignment();
		for (AssignmentType assignment: assignments) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			if (targetRef != null && roleOid.equals(targetRef.getOid()) && ObjectTypeUtil.relationMatches(relation,
					targetRef.getRelation())) {
				return assignment;
			}
		}
		return null;
	}
	
	protected <F extends FocusType> AssignmentType getAssignment(PrismObject<F> focus, String roleOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment: assignments) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			if (targetRef != null && roleOid.equals(targetRef.getOid())) {
				return assignment;
			}
		}
		return null;
	}

	protected ItemPath getAssignmentPath(long id) {
		return new ItemPath(
				new NameItemPathSegment(FocusType.F_ASSIGNMENT),
				new IdItemPathSegment(id));
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

	protected AssignmentType assertAssignedRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		return assertAssignedRole(user, roleOid);
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
	
	protected static <F extends FocusType> void assertAssignedRoles(PrismObject<F> user, Collection<String> roleOids) {
		MidPointAsserts.assertAssignedRoles(user, roleOids);
	}
	
	protected <R extends AbstractRoleType> AssignmentType assertInducedRole(PrismObject<R> role, String roleOid) {
		return MidPointAsserts.assertInducedRole(role, roleOid);
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
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, true, null, task, result, limitTargets);
	}
	
	protected void assignDeputyLimits(String userDeputyOid, String userTargetOid, Consumer<AssignmentType> assignmentMutator, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, true, assignmentMutator, task, result, limitTargets);
	}

	protected void unassignDeputyLimits(String userDeputyOid, String userTargetOid, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, false, null, task, result, limitTargets);
	}
	
	protected void unassignDeputyLimits(String userDeputyOid, String userTargetOid, Consumer<AssignmentType> assignmentMutator, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyDeputyAssignmentLimits(userDeputyOid, userTargetOid, false, assignmentMutator, task, result, limitTargets);
	}

	protected void modifyDeputyAssignmentLimits(String userDeputyOid, String userTargetOid, boolean add, Consumer<AssignmentType> assignmentMutator, Task task, OperationResult result, ObjectReferenceType... limitTargets) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		modifyUserAssignment(userDeputyOid, userTargetOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY, task,
				assignment -> {
					AssignmentSelectorType limitTargetContent = new AssignmentSelectorType();
					assignment.setLimitTargetContent(limitTargetContent);
					for (ObjectReferenceType limitTarget: limitTargets) {
						limitTargetContent.getTargetRef().add(limitTarget);
					}
					if (assignmentMutator != null) {
						assignmentMutator.accept(assignment);
					}
				}, add, result);
	}

	protected <F extends FocusType> void assertAssignedDeputy(PrismObject<F> focus, String targetUserOid) {
		MidPointAsserts.assertAssigned(focus, targetUserOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
	}
	
	protected static <F extends FocusType> void assertAssignedOrgs(PrismObject<F> user, String... orgOids) {
		MidPointAsserts.assertAssignedOrgs(user, orgOids);
	}

	protected <F extends FocusType> void assertObjectRefs(String contextDesc, Collection<ObjectReferenceType> real, ObjectType... expected) {
		assertObjectRefs(contextDesc, real, objectsToOids(expected));
	}

	protected <F extends FocusType> void assertPrismRefValues(String contextDesc, Collection<PrismReferenceValue> real, ObjectType... expected) {
		assertPrismRefValues(contextDesc, real, objectsToOids(expected));
	}

	protected <F extends FocusType> void assertPrismRefValues(String contextDesc, Collection<PrismReferenceValue> real, Collection<? extends ObjectType> expected) {
		assertPrismRefValues(contextDesc, real, objectsToOids(expected));
	}

	protected void assertObjectRefs(String contextDesc, Collection<ObjectReferenceType> real, String... expected) {
		assertObjectRefs(contextDesc, true, real, expected);
	}

	protected void assertObjectRefs(String contextDesc, boolean checkNames, Collection<ObjectReferenceType> real, String... expected) {
		List<String> refOids = new ArrayList<>();
		for (ObjectReferenceType ref: real) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in "+ref.getOid()+" in "+contextDesc, ref.getType());
			if (checkNames) {
				assertNotNull("Missing name in " + ref.getOid() + " in " + contextDesc, ref.getTargetName());
			}
		}
		PrismAsserts.assertSets("Wrong values in "+contextDesc, refOids, expected);
	}

	protected void assertPrismRefValues(String contextDesc, Collection<PrismReferenceValue> real, String... expected) {
		List<String> refOids = new ArrayList<>();
		for (PrismReferenceValue ref: real) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in "+ref.getOid()+" in "+contextDesc, ref.getTargetType());
			assertNotNull("Missing name in "+ref.getOid()+" in "+contextDesc, ref.getTargetName());
		}
		PrismAsserts.assertSets("Wrong values in "+contextDesc, refOids, expected);
	}

	private String[] objectsToOids(ObjectType[] objects) {
		return Arrays.stream(objects)
				.map(o -> o.getOid())
				.toArray(String[]::new);
	}

	private String[] objectsToOids(Collection<? extends ObjectType> objects) {
		return objects.stream()
				.map(o -> o.getOid())
				.toArray(String[]::new);
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

	protected <F extends FocusType> AssignmentType assertAssignedOrg(PrismObject<F> focus, String orgOid) {
		return MidPointAsserts.assertAssignedOrg(focus, orgOid);
	}

	protected <F extends FocusType> void assertNotAssignedOrg(PrismObject<F> focus, String orgOid) {
		MidPointAsserts.assertNotAssignedOrg(focus, orgOid);
	}

	protected AssignmentType assertAssignedOrg(PrismObject<UserType> user, PrismObject<OrgType> org) {
		return MidPointAsserts.assertAssignedOrg(user, org.getOid());
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

	protected void assertSubOrgs(String baseOrgOid, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrgOid, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEquals("Unexpected number of suborgs of org "+baseOrgOid+", has suborgs "+subOrgs, expected, subOrgs.size());
	}

	protected void assertSubOrgs(PrismObject<OrgType> baseOrg, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrg.getOid(), task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEquals("Unexpected number of suborgs of"+baseOrg+", has suborgs "+subOrgs, expected, subOrgs.size());
	}

	protected List<PrismObject<OrgType>> getSubOrgs(String baseOrgOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
				.isDirectChildOf(baseOrgOid)
				.build();
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}
	
	protected List<PrismObject<UserType>> getSubOrgUsers(String baseOrgOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
				.isDirectChildOf(baseOrgOid)
				.build();
		return modelService.searchObjects(UserType.class, query, null, task, result);
	}

	protected String dumpOrgTree(String topOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return dumpOrgTree(topOrgOid, false);
	}
	
	protected String dumpOrgTree(String topOrgOid, boolean dumpUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".assertSubOrgs");
		OperationResult result = task.getResult();
		PrismObject<OrgType> topOrg = modelService.getObject(OrgType.class, topOrgOid, null, task, result);
		String dump = dumpOrgTree(topOrg, dumpUsers, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return dump;
	}

	protected String dumpOrgTree(PrismObject<OrgType> topOrg, boolean dumpUsers, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		StringBuilder sb = new StringBuilder();
		dumpOrg(sb, topOrg, 0);
		sb.append("\n");
		dumpSubOrgs(sb, topOrg.getOid(), dumpUsers, 1, task, result);
		return sb.toString();
	}

	private void dumpSubOrgs(StringBuilder sb, String baseOrgOid, boolean dumpUsers, int indent, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrgOid, task, result);
		for (PrismObject<OrgType> suborg: subOrgs) {
			dumpOrg(sb, suborg, indent);
			if (dumpUsers) {
				dumpOrgUsers(sb, suborg.getOid(), dumpUsers, indent + 1, task, result);
			}
			sb.append("\n");
			dumpSubOrgs(sb, suborg.getOid(), dumpUsers, indent + 1, task, result);
		}
	}
	
	private void dumpOrgUsers(StringBuilder sb, String baseOrgOid, boolean dumpUsers, int indent, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		List<PrismObject<UserType>> subUsers = getSubOrgUsers(baseOrgOid, task, result);
		for (PrismObject<UserType> subuser: subUsers) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent);
			sb.append(subuser);
		}
	}

	private void dumpOrg(StringBuilder sb, PrismObject<OrgType> org, int indent) {
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(org);
		List<ObjectReferenceType> linkRefs = org.asObjectable().getLinkRef();
		sb.append(": ").append(linkRefs.size()).append(" links");
	}

	protected void displayUsers() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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


	protected <O extends ObjectType> PrismObject<O> createObject(Class<O> type, String name) throws SchemaException {
		PrismObject<O> object = getObjectDefinition(type).instantiate();
		object.asObjectable().setName(createPolyStringType(name));
		return object;
	}

	protected PrismObject<UserType> createUser(String name, String fullName) throws SchemaException {
		return createUser(name, fullName, null);
	}

	protected PrismObject<UserType> createUser(String name, String fullName, Boolean enabled) throws SchemaException {
		PrismObject<UserType> user = createObject(UserType.class, name);
		UserType userType = user.asObjectable();
		userType.setFullName(createPolyStringType(fullName));
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
		userType.setName(createPolyStringType(name));
		userType.setGivenName(createPolyStringType(givenName));
		userType.setFamilyName(createPolyStringType(familyName));
		userType.setFullName(createPolyStringType(givenName + " " + familyName));
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
		user.asObjectable().setName(createPolyStringType(name));
		user.asObjectable().setFullName(createPolyStringType(fullName));
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
		Collection<? extends ItemDelta> modifications = new ArrayList<>();

		if (oldValue != null) {
			ObjectPolicyConfigurationType oldPolicy = oldValue.asContainerable();
			ObjectReferenceType oldObjectTemplateRef = oldPolicy.getObjectTemplateRef();
			if (oldObjectTemplateRef != null) {
				if (oldObjectTemplateRef.getOid().equals(objectTemplateOid)) {
					// Already set
					return;
				}
			}
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
				PrismContainerValue<ObjectPolicyConfigurationType> newValue = oldValue.cloneComplex(CloneStrategy.REUSE);
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

	protected void setConflictResolution(QName objectType, String subType, ConflictResolutionType conflictResolution, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObject<SystemConfigurationType> systemConfig = repositoryService.getObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, parentResult);

		PrismContainerValue<ObjectPolicyConfigurationType> oldValue = null;
		for (ObjectPolicyConfigurationType focusPolicyType: systemConfig.asObjectable().getDefaultObjectPolicyConfiguration()) {
			if (QNameUtil.match(objectType, focusPolicyType.getType()) && MiscUtil.equals(subType, focusPolicyType.getSubtype())) {
				oldValue = focusPolicyType.asPrismContainerValue();
			}
		}
		Collection<? extends ItemDelta> modifications = new ArrayList<>();

		if (oldValue != null) {
			ContainerDelta<ObjectPolicyConfigurationType> deleteDelta = ContainerDelta.createModificationDelete(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
					SystemConfigurationType.class, prismContext, oldValue.clone());
			((Collection)modifications).add(deleteDelta);
		}

		ObjectPolicyConfigurationType newFocusPolicyType = new ObjectPolicyConfigurationType();
		newFocusPolicyType.setType(objectType);
		newFocusPolicyType.setSubtype(subType);
		if (oldValue != null) {
			ObjectReferenceType oldObjectTemplateRef = oldValue.asContainerable().getObjectTemplateRef();
			if (oldObjectTemplateRef != null) {
				newFocusPolicyType.setObjectTemplateRef(oldObjectTemplateRef.clone());
			}
		}

		newFocusPolicyType.setConflictResolution(conflictResolution);

		ContainerDelta<ObjectPolicyConfigurationType> addDelta = ContainerDelta.createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
				SystemConfigurationType.class, prismContext, newFocusPolicyType);

		((Collection)modifications).add(addDelta);

		modifySystemObjectInRepo(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);

	}
	
	protected void setConflictResolutionAction(QName objectType, String subType, ConflictResolutionActionType conflictResolutionAction, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		ConflictResolutionType conflictResolutionType = new ConflictResolutionType();
		conflictResolutionType.action(conflictResolutionAction);
		setConflictResolution(objectType, subType, conflictResolutionType, parentResult);
	}

	protected void setGlobalSecurityPolicy(String securityPolicyOid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		Collection modifications = new ArrayList<>();

		ReferenceDelta refDelta = ReferenceDelta.createModificationReplace(SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF,
				SystemConfigurationType.class, prismContext, securityPolicyOid);
		modifications.add(refDelta);

		modifySystemObjectInRepo(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);

	}

	protected <O extends ObjectType> void modifySystemObjectInRepo(Class<O> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		display("Modifications of system object "+oid, modifications);
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
        PrismContainerValue<ActivationType> emptyValue = new PrismContainerValue<>(prismContext);
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

    protected List<PrismObject<OrgType>> searchOrg(String baseOrgOid, OrgFilter.Scope scope, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		ObjectQuery query = QueryBuilder.queryFor(OrgType.class, prismContext)
				.isInScopeOf(baseOrgOid, scope)
				.build();
		return modelService.searchObjects(OrgType.class, query, null, task, result);
	}

    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName()+".searchObjectByName");
    	OperationResult result = task.getResult();
    	PrismObject<T> out = searchObjectByName(type, name, task, result);
    	result.computeStatus();
    	TestUtil.assertSuccess(result);
    	return out;
    }

    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
	
	protected ObjectDelta<UserType> createModifyUserAddDummyAccount(String userOid, String dummyResourceName) throws SchemaException {
		return createModifyUserAddAccount(userOid, getDummyResourceObject(dummyResourceName));
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

	protected ObjectDelta<UserType> createModifyUserDeleteDummyAccount(String userOid, String dummyResourceName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return createModifyUserDeleteAccount(userOid, getDummyResourceObject(dummyResourceName));
	}
	
	protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		return createModifyUserDeleteAccount(userOid, resource.getOid());
	}
	
	protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, String resourceOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String accountOid = getLinkRefOid(userOid, resourceOid);
		PrismObject<ShadowType> account = getShadowModel(accountOid);

		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);

		return userDelta;
	}

	protected ObjectDelta<UserType> createModifyUserUnlinkAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String accountOid = getLinkRefOid(userOid, resource.getOid());

		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setOid(accountOid);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);

		return userDelta;
	}
	
	protected void deleteUserAccount(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(userOid, resourceOid);
        executeChanges(userDelta, null, task, result);
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
//				Task freshTask = taskManager.getTaskWithResult(task.getOid(), waitResult);
				OperationResult result = task.getResult();
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+task+": "+TestUtil.getErrorMessage(result);
				assert !isUnknown(result, checkSubresult) : "Unknown result in "+task+": "+TestUtil.getErrorMessage(result);
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
				Task task = taskManager.getTaskWithResult(taskOid, waitResult);
				waitResult.summarize();
				display("Task execution status = " + task.getExecutionStatus());
				return task.getExecutionStatus() == TaskExecutionStatus.CLOSED
						|| task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED;
			}
			@Override
			public void timeout() {
				Task task = null;
				try {
					task = taskManager.getTaskWithResult(taskOid, waitResult);
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

	protected Task waitForTaskFinish(String taskOid, boolean checkSubresult) throws CommonException {
		return waitForTaskFinish(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}

	protected Task waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout) throws CommonException {
		return waitForTaskFinish(taskOid, checkSubresult, timeout, false);
	}

	protected Task waitForTaskFinish(final String taskOid, final boolean checkSubresult, final int timeout, final boolean errorOk) throws CommonException {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskFinish");
		TaskFinishChecker checker = new TaskFinishChecker(taskOid, waitResult, checkSubresult, errorOk, timeout);
		IntegrationTestTools.waitFor("Waiting for task " + taskOid + " finish", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
		return checker.getLastTask();
	}

	private class TaskFinishChecker implements Checker {
		private final String taskOid;
		private final OperationResult waitResult;
		private final boolean checkSubresult;
		private final boolean errorOk;
		private final int timeout;
		private Task freshTask;
		
		public TaskFinishChecker(String taskOid, OperationResult waitResult, boolean checkSubresult,
				boolean errorOk, int timeout) {
			super();
			this.taskOid = taskOid;
			this.waitResult = waitResult;
			this.checkSubresult = checkSubresult;
			this.errorOk = errorOk;
			this.timeout = timeout;
		}

		@Override
		public boolean check() throws CommonException {
			freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
			OperationResult result = freshTask.getResult();
			if (verbose) display("Check result", result);
			if (isError(result, checkSubresult)) {
				if (errorOk) {
					return true;
				} else {
					AssertJUnit.fail("Error in "+freshTask+": "+TestUtil.getErrorMessage(result));
				}
			}
			if (isUnknown(result, checkSubresult)) {
				return false;
			}
//			assert !isUnknown(result, checkSubresult) : "Unknown result in "+freshTask+": "+IntegrationTestTools.getErrorMessage(result);
			return !isInProgress(result, checkSubresult);
		}
		
		@Override
		public void timeout() {
			try {
				Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
				assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to finish. Last result "+result;
			} catch (ObjectNotFoundException e) {
				LOGGER.error("Exception during task refresh: {}", e,e);
			} catch (SchemaException e) {
				LOGGER.error("Exception during task refresh: {}", e,e);
			}
		}
		
		public Task getLastTask() {
			return freshTask;
		}
	}
	
	protected void waitForTaskStart(String taskOid, boolean checkSubresult) throws Exception {
		waitForTaskStart(taskOid, checkSubresult, DEFAULT_TASK_WAIT_TIMEOUT);
	}

	protected void waitForTaskStart(final String taskOid, final boolean checkSubresult,final int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskStart");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
				Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
				OperationResult result = freshTask.getResult();
				if (verbose) display("Check result", result);
				assert !isError(result, checkSubresult) : "Error in "+freshTask+": "+TestUtil.getErrorMessage(result);
				if (isUnknown(result, checkSubresult)) {
					return false;
				}
				return freshTask.getLastRunStartTimestamp() != null;
			}
			@Override
			public void timeout() {
				try {
					Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" to start. Last result "+result;
				} catch (ObjectNotFoundException | SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e, e);
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
		Task origTask = taskManager.getTaskWithResult(taskOid, waitResult);
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
				Task freshTask = taskManager.getTaskWithResult(origTask.getOid(), waitResult);
				OperationResult taskResult = freshTask.getResult();
//				display("Times", longTimeToString(origLastRunStartTimestamp) + "-" + longTimeToString(origLastRunStartTimestamp)
//						+ " : " + longTimeToString(freshTask.getLastRunStartTimestamp()) + "-" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
				if (verbose) display("Check result", taskResult);
				taskResultHolder.setValue(taskResult);
				if (isError(taskResult, checkSubresult)) {
                    return true;
                }
				if (isUnknown(taskResult, checkSubresult)) {
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
					Task freshTask = taskManager.getTaskWithResult(origTask.getOid(), waitResult);
					OperationResult result = freshTask.getResult();
					LOGGER.debug("Timed-out task:\n{}", freshTask.debugDump());
					display("Times", "origLastRunStartTimestamp="+longTimeToString(origLastRunStartTimestamp)
					+ ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp)
					+ ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
					+ ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
					assert false : "Timeout ("+timeout+") while waiting for "+freshTask+" next run. Last result "+result;
				} catch (ObjectNotFoundException | SchemaException e) {
					LOGGER.error("Exception during task refresh: {}", e, e);
				}
			}
		};
		IntegrationTestTools.waitFor("Waiting for task " + origTask + " next run", checker, timeout, DEFAULT_TASK_SLEEP_TIME);

		Task freshTask = taskManager.getTaskWithResult(origTask.getOid(), waitResult);
		LOGGER.debug("Final task:\n{}", freshTask.debugDump());
		display("Times", "origLastRunStartTimestamp="+longTimeToString(origLastRunStartTimestamp)
		+ ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp)
		+ ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
		+ ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));

		return taskResultHolder.getValue();
	}

	protected OperationResult waitForTaskTreeNextFinishedRun(String rootTaskOid, int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskTreeNextFinishedRun");
		Task origRootTask = taskManager.getTaskWithResult(rootTaskOid, waitResult);
		return waitForTaskTreeNextFinishedRun(origRootTask, timeout, waitResult);
	}

	// a bit experimental
	protected OperationResult waitForTaskTreeNextFinishedRun(Task origRootTask, int timeout, OperationResult waitResult) throws Exception {
		Long origLastRunStartTimestamp = origRootTask.getLastRunStartTimestamp();
		Long origLastRunFinishTimestamp = origRootTask.getLastRunFinishTimestamp();
		long start = System.currentTimeMillis();
		Holder<Boolean> triggered = new Holder<>(false);    // to avoid repeated checking for start-finish timestamps
		OperationResult aggregateResult = new OperationResult("aggregate");
		Checker checker = () -> {
			Task freshRootTask = taskManager.getTaskWithResult(origRootTask.getOid(), waitResult);

			String s = TaskDebugUtil.dumpTaskTree(freshRootTask, waitResult);
			display("task tree", s);

			long waiting = (System.currentTimeMillis() - start) / 1000;
			String description =
					freshRootTask.getName().getOrig() + " (" + freshRootTask.getExecutionStatus() + "/" + freshRootTask
							.getNode() + "/" + freshRootTask.getProgress() + ") ["
							+ waiting + "]";
			// was the whole task tree refreshed at least once after we were called?
			if (!triggered.getValue() && (freshRootTask.getLastRunStartTimestamp() == null
					|| freshRootTask.getLastRunStartTimestamp().equals(origLastRunStartTimestamp)
					|| freshRootTask.getLastRunFinishTimestamp() == null
					|| freshRootTask.getLastRunFinishTimestamp().equals(origLastRunFinishTimestamp)
					|| freshRootTask.getLastRunStartTimestamp() >= freshRootTask.getLastRunFinishTimestamp())) {
				display("Root (triggering) task next run has not been completed yet: " + description);
				return false;
			}
			triggered.setValue(true);

			aggregateResult.getSubresults().clear();
			List<Task> subtasks = freshRootTask.listSubtasksDeeply(waitResult);
			for (Task subtask : subtasks) {
				subtask.refresh(waitResult);        // quick hack to get operation results
			}
			Task failedTask = null;
			for (Task subtask : subtasks) {
				if (subtask.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
					display("Found runnable/running subtasks during waiting => continuing waiting: " + description, subtask);
					return false;
				}
				if (subtask.getExecutionStatus() == TaskExecutionStatus.WAITING) {
					display("Found waiting subtasks during waiting => continuing waiting: " + description, subtask);
					return false;
				}
				OperationResult subtaskResult = subtask.getResult();
				if (subtaskResult.getStatus() == OperationResultStatus.IN_PROGRESS) {
					display("Found 'in_progress' subtask operation result during waiting => continuing waiting: " + description, subtask);
					return false;
				}
				if (subtaskResult.getStatus() == OperationResultStatus.UNKNOWN) {
					display("Found 'unknown' subtask operation result during waiting => continuing waiting: " + description, subtask);
					return false;
				}
				aggregateResult.addSubresult(subtaskResult);
				if (subtaskResult.isError()) {
					failedTask = subtask;
				}
			}
			if (failedTask != null) {
				display("Found 'error' subtask operation result during waiting => done waiting: " + description, failedTask);
				return true;
			}
			if (freshRootTask.getExecutionStatus() == TaskExecutionStatus.WAITING) {
				display("Found WAITING root task during wait for next finished run => continuing waiting: " + description);
				return false;
			}
			return true;        // all executive subtasks are closed
		};
		IntegrationTestTools.waitFor("Waiting for task tree " + origRootTask + " next finished run", checker, timeout, DEFAULT_TASK_SLEEP_TIME);

		Task freshTask = taskManager.getTaskWithResult(origRootTask.getOid(), waitResult);
		LOGGER.debug("Final root task:\n{}", freshTask.debugDump());
		aggregateResult.computeStatusIfUnknown();
		return aggregateResult;
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

	private boolean isUnknown(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult != null ? subresult.isUnknown() : false;			// TODO or return true?
	}

	private boolean isInProgress(OperationResult result, boolean checkSubresult) {
		OperationResult subresult = getSubresult(result, checkSubresult);
		return subresult != null ? subresult.isInProgress() : true;		// "true" if there are no subresults
	}

	private OperationResult getSubresult(OperationResult result, boolean checkSubresult) {
		if (checkSubresult) {
			return result != null ? result.getLastSubresult() : null;
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
		Task task = taskManager.getTaskWithResult(taskOid, result);
		LOGGER.info("Restarting task {}", taskOid);
		if (task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
			LOGGER.debug("Task {} is suspended, resuming it", task);
			taskManager.resumeTask(task, result);
		} else if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
			LOGGER.debug("Task {} is closed, scheduling it to run now", task);
			taskManager.scheduleTasksNow(singleton(taskOid), result);
		} else if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
			if (taskManager.getLocallyRunningTaskByIdentifier(task.getTaskIdentifier()) != null) {
				// Task is really executing. Let's wait until it finishes; hopefully it won't start again (TODO)
				LOGGER.debug("Task {} is running, waiting while it finishes before restarting", task);
				waitForTaskFinish(taskOid, false);
			}
			LOGGER.debug("Task {} is finished, scheduling it to run now", task);
			taskManager.scheduleTasksNow(singleton(taskOid), result);
		} else {
			throw new IllegalStateException("Task " + task + " cannot be restarted, because its state is: " + task.getExecutionStatus());
		}
	}

	protected void setSecurityContextUser(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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
		for (ItemDelta<?,?> modification: focusDelta.getModifications()) {
			if (isSideEffectDelta(modification)) {
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
			if (isSideEffectDelta(modification)) {
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

	private boolean isSideEffectDelta(ItemDelta<?, ?> modification) {
		if (modification.getPath().containsName(ObjectType.F_METADATA) ||
				(modification.getPath().containsName(FocusType.F_ASSIGNMENT) && modification.getPath().containsName(ActivationType.F_EFFECTIVE_STATUS))) {
			return true;
		} else if (modification.getPath().containsName(FocusType.F_ASSIGNMENT) && modification.getPath().containsName(AssignmentType.F_ACTIVATION) && modification.isReplace() && (modification instanceof ContainerDelta<?>)) {
			Collection<PrismContainerValue<ActivationType>> valuesToReplace = ((ContainerDelta<ActivationType>)modification).getValuesToReplace();
			if (valuesToReplace != null && valuesToReplace.size() == 1) {
				PrismContainerValue<ActivationType> cval = valuesToReplace.iterator().next();
				if (cval.getItems().size() == 1) {
					Item<?, ?> item = cval.getItems().iterator().next();
					if (ActivationType.F_EFFECTIVE_STATUS.equals(item.getElementName())) {
						return true;
					}
				}
			}
		}
		return false;
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

	protected PrismObject<TaskType> getTask(String taskOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getTask");
        OperationResult result = task.getResult();
		PrismObject<TaskType> retTask = modelService.getObject(TaskType.class, taskOid, retrieveItemsNamed(TaskType.F_RESULT), task, result);
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

	protected <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
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

    // not going through model to avoid conflicts (because the task starts execution during the clockwork operation)
	protected void addTask(File file) throws SchemaException, IOException, ObjectAlreadyExistsException {
		taskManager.addTask(prismContext.parseObject(file), new OperationResult("addTask"));
	}

	protected <O extends ObjectType> void addObject(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		PrismObject<O> object = prismContext.parseObject(file);
		addObject(object);
	}

	protected <O extends ObjectType> PrismObject<O> addObject(File file, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		PrismObject<O> object = prismContext.parseObject(file);
		addObject(object, task, result);
		return object;
	}

	protected <O extends ObjectType> String addObject(PrismObject<O> object) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".addObject");
        OperationResult result = task.getResult();
        String oid = addObject(object, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return oid;
	}

	protected <O extends ObjectType> String addObject(PrismObject<O> object, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		return addObject(object, null, task, result);
	}

	protected <O extends ObjectType> String addObject(PrismObject<O> object, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> addDelta = object.createAddDelta();
        modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
        object.setOid(addDelta.getOid());
        return addDelta.getOid();
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
	
	protected <O extends ObjectType> void deleteObjectRepo(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		OperationResult result = new OperationResult(AbstractModelIntegrationTest.class.getName() + ".deleteObjectRepo");
		repositoryService.deleteObject(type, oid, result);
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

	protected void addTriggers(String oid, Collection<XMLGregorianCalendar> timestamps, String uri) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".addTriggers");
        OperationResult result = task.getResult();
        Collection<TriggerType> triggers = timestamps.stream()
		        .map(ts -> new TriggerType().timestamp(ts).handlerUri(uri))
		        .collect(Collectors.toList());
        ObjectDelta<ObjectType> delta = DeltaBuilder.deltaFor(ObjectType.class, prismContext)
		       .item(ObjectType.F_TRIGGER).addRealValues(triggers)
		       .asObjectDeltaCast(oid);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	protected void replaceTriggers(String oid, Collection<XMLGregorianCalendar> timestamps, String uri) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".replaceTriggers");
        OperationResult result = task.getResult();
        Collection<TriggerType> triggers = timestamps.stream()
		        .map(ts -> new TriggerType().timestamp(ts).handlerUri(uri))
		        .collect(Collectors.toList());
        ObjectDelta<ObjectType> delta = DeltaBuilder.deltaFor(ObjectType.class, prismContext)
		       .item(ObjectType.F_TRIGGER).replaceRealValues(triggers)
		       .asObjectDeltaCast(oid);
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
            	printNotifyMessages(messages);
                fail(messages.size() + " unexpected message(s) recorded in dummy transport '" + name + "'");
            }
        } else {
            assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
            if (expectedCount != messages.size()) {
            	LOGGER.error("Invalid number of messages recorded in dummy transport '" + name + "', expected: "+expectedCount+", actual: "+messages.size());
            	logNotifyMessages(messages);
            	printNotifyMessages(messages);
            	assertEquals("Invalid number of messages recorded in dummy transport '" + name + "'", expectedCount, messages.size());
            }
        }
    }

    protected void assertSingleDummyTransportMessage(String name, String expectedBody) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        if (messages.size() != 1) {
        	fail("Invalid number of messages recorded in dummy transport '" + name + "', expected: 1, actual: "+messages.size());
        }
        Message message = messages.get(0);
        assertEquals("Unexpected notifier "+name+" message body", expectedBody, message.getBody());
    }

    protected void assertSingleDummyTransportMessageContaining(String name, String expectedSubstring) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        if (messages.size() != 1) {
        	fail("Invalid number of messages recorded in dummy transport '" + name + "', expected: 1, actual: "+messages.size());
        }
        Message message = messages.get(0);
        assertTrue("Notifier "+name+" message body does not contain text: " + expectedSubstring + ", it is:\n" + message.getBody(),
		        message.getBody().contains(expectedSubstring));
    }

    protected String getDummyTransportMessageBody(String name, int index) {
    	List<Message> messages = dummyTransport.getMessages("dummy:" + name);
    	Message message = messages.get(index);
    	return message.getBody();
    }

    protected void assertHasDummyTransportMessage(String name, String expectedBody) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        for (Message message: messages) {
        	if (expectedBody.equals(message.getBody())) {
        		return;
        	}
        }
        fail("Notifier "+name+" message body " + expectedBody + " not found");
    }

    protected void displayAllNotifications() {
    	for (java.util.Map.Entry<String,List<Message>> entry: dummyTransport.getMessages().entrySet()) {
    		List<Message> messages = entry.getValue();
    		if (messages != null && !messages.isEmpty()) {
    			display("Notification messages: "+entry.getKey(), messages);
    		}
    	}
    }

    protected void displayNotifications(String name) {
    	List<Message> messages = dummyTransport.getMessages("dummy:" + name);
    	display("Notification messages: "+name, messages);
    }

    private void logNotifyMessages(List<Message> messages) {
		for (Message message: messages) {
			LOGGER.debug("Notification message:\n{}", message.getBody());
		}
	}

    private void printNotifyMessages(List<Message> messages) {
		for (Message message: messages) {
			System.out.println(message);
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
		// display("account", account);
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
		assertEquals("Unexpected number of values for attribute " + attributeName + " of "+dummyInstanceName+" dummy account " + username +
				". Expected: " + Arrays.toString(expectedAttributeValues) + ", was: " + values,
				expectedAttributeValues.length, values.size());
		for (Object expectedValue: expectedAttributeValues) {
			if (!values.contains(expectedValue)) {
				AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of "+dummyInstanceName+" dummy account "+username+
						" but not found. Values found: "+values);
			}
		}
	}

	protected void assertNoDummyAccountAttribute(String dummyInstanceName, String username, String attributeName) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(dummyInstanceName, username);
		assertNotNull("No dummy "+dummyInstanceName+" account for username "+username, account);
		Set<Object> values = account.getAttributeValues(attributeName, Object.class);
		if (values == null || values.isEmpty()) {
			return;
		}
		AssertJUnit.fail("Expected no value in attribute " + attributeName + " of "+dummyInstanceName+" dummy account " + username +
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

	protected String getPassword(PrismObject<UserType> user) throws EncryptionException {
		CredentialsType credentialsType = user.asObjectable().getCredentials();
		assertNotNull("No credentials in "+user, credentialsType);
		PasswordType passwordType = credentialsType.getPassword();
		assertNotNull("No password in "+user, passwordType);
		ProtectedStringType protectedStringType = passwordType.getValue();
		assertNotNull("No password value in "+user, protectedStringType);
		return protector.decryptString(protectedStringType);
	}

	protected void assertPassword(PrismObject<UserType> user, String expectedPassword) throws EncryptionException {
		String decryptedUserPassword = getPassword(user);
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
			assertTrue("Wrong password hash in "+source+": "+decryptedUserPassword+", expected "+expectedPassword, ldapShaPasswordEncoder.matches(decryptedUserPassword, expectedPassword));
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

	protected void login(String principalName) throws ObjectNotFoundException, SchemaException {
		MidPointPrincipal principal = userProfileService.getPrincipal(principalName);
		login(principal);
	}

	protected void login(PrismObject<UserType> user) throws SchemaException {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		login(principal);
	}

	protected void login(MidPointPrincipal principal) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
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

	protected void assertLoggedInUsername(String username) {
		MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
		UserType user = midPointPrincipal.getUser();
		if (user == null) {
			if (username == null) {
				return;
			} else {
				AssertJUnit.fail("Expected logged in user '"+username+"' but there was no user in the spring security context");
			}
		}
		assertEquals("Wrong logged-in user", username, user.getName().getOrig());
	}
	
	protected void assertLoggedInUserOid(String userOid) {
		MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
		assertPrincipalUserOid(midPointPrincipal, userOid);
	}
	
	protected void assertPrincipalUserOid(MidPointPrincipal principal, String userOid) {
		UserType user = principal.getUser();
		if (user == null) {
			if (userOid == null) {
				return;
			} else {
				AssertJUnit.fail("Expected user "+userOid+" in principal "+principal+" but there was none");
			}
		}
		assertEquals("Wrong user OID in principal", userOid, user.getOid());
	}
	
	protected MidPointPrincipal getSecurityContextPrincipal() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication == null) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal == null) {
			return null;
		}
		if (principal instanceof MidPointPrincipal) {
			return (MidPointPrincipal)principal;
		} else {
			AssertJUnit.fail("Unknown principal in the spring security context: "+principal);
			return null; // not reached
		}
	}
	
	protected void assertAuthenticated() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		assertTrue("Security context is not authenticated", authentication.isAuthenticated());
	}

	protected void resetAuthentication() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		securityContext.setAuthentication(null);
	}

	protected void assertNoAuthentication() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		assertNull("Unexpected authentication", securityContext.getAuthentication());
	}
	
	protected void assertSecurityContextPrincipalAttorneyOid(String attotrneyOid) {
		MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
		assertPrincipalAttorneyOid(midPointPrincipal, attotrneyOid);
	}
		
	protected void assertPrincipalAttorneyOid(MidPointPrincipal principal, String attotrneyOid) {
		UserType attorney = principal.getAttorney();
		if (attorney == null) {
			if (attotrneyOid == null) {
				return;
			} else {
				AssertJUnit.fail("Expected attorney "+attotrneyOid+" in principal "+principal+" but there was none");
			}
		}
		assertEquals("Wrong attroney OID in principal", attotrneyOid, attorney.getOid());
	}

	protected Collection<Authorization> getSecurityContextAuthorizations() {
		MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
		if (midPointPrincipal == null) {
			return null;
		}
		return midPointPrincipal.getAuthorities();
	}
	
	protected void assertAuthorizationActions(String message, Collection<Authorization> autzs, String... expectedActions) {
		Collection<String> actualActions = autzs.stream()
			.map(a -> a.getAction())
			.flatMap(List::stream)
			.collect(Collectors.toList());
		PrismAsserts.assertEqualsCollectionUnordered(message, actualActions, expectedActions);
	}
	
	protected void assertSecurityContextAuthorizationActions(String... expectedActions) {
		Collection<Authorization> securityContextAuthorizations = getSecurityContextAuthorizations();
		assertAuthorizationActions("Wrong authorizations in security context", securityContextAuthorizations, expectedActions);
	}
	
	protected void assertSecurityContextAuthorizationActions(ModelAuthorizationAction... expectedModelActions) {
		Collection<Authorization> securityContextAuthorizations = getSecurityContextAuthorizations();
		String[] expectedActions = new String[expectedModelActions.length];
		for (int i=0;i<expectedModelActions.length;i++) {
			expectedActions[i] = expectedModelActions[i].getUrl();
		}
		assertAuthorizationActions("Wrong authorizations in security context", securityContextAuthorizations, expectedActions);
	}
	
	protected void assertSecurityContextNoAuthorizationActions() {
		Collection<Authorization> securityContextAuthorizations = getSecurityContextAuthorizations();
		if (securityContextAuthorizations != null && !securityContextAuthorizations.isEmpty()) {
			fail("Unexpected authorizations in security context: "+securityContextAuthorizations);
		}
	}
	
	protected void displayAllUsers() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
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
		Task task = super.createTask(operationName);
		PrismObject<UserType> defaultActor = getDefaultActor();
		if (defaultActor != null) {
			task.setOwner(defaultActor);
		}
		task.setChannel(DEFAULT_CHANNEL);
		return task;
	}
	
	protected Task createTask(String operationName, MidPointPrincipal principal) {
		Task task = super.createTask(operationName);
		task.setOwner(principal.getUser().asPrismObject());
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

	protected void modifyRoleAddInducementTarget(String roleOid, String targetOid, boolean reconcileAffected,
			ModelExecuteOptions defaultOptions, Task task) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
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
		ModelExecuteOptions options = nullToEmpty(defaultOptions);
        options.setReconcileAffected(reconcileAffected);
        executeChanges(roleDelta, options, task, result);
        result.computeStatus();
        if (reconcileAffected) {
            TestUtil.assertInProgressOrSuccess(result);
        } else {
            TestUtil.assertSuccess(result);
        }
	}
	
	protected void modifyRoleAddExclusion(String roleOid, String excludedRoleOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		modifyRoleExclusion(roleOid, excludedRoleOid, true, task, result);
	}
	
	protected void modifyRoleDeleteExclusion(String roleOid, String excludedRoleOid, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		modifyRoleExclusion(roleOid, excludedRoleOid, false, task, result);
	}
	
	protected void modifyRoleExclusion(String roleOid, String excludedRoleOid, boolean add, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		modifyRolePolicyRule(roleOid, createExclusionPolicyRule(excludedRoleOid), add, task, result);
	}
	
	protected void modifyRolePolicyRule(String roleOid, PolicyRuleType exclusionPolicyRule, boolean add, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        AssignmentType assignment = new AssignmentType();
		assignment.setPolicyRule(exclusionPolicyRule);
		ObjectDelta<RoleType> roleDelta;
		if (add) {
			roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, roleOid,
        		new ItemPath(new NameItemPathSegment(RoleType.F_ASSIGNMENT)),
        		prismContext, assignment);
		} else {
			roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_ASSIGNMENT)),
	        		prismContext, assignment);
		}
        executeChanges(roleDelta, null, task, result);
	}
	
	protected void modifyRoleAddAssignment(String roleOid, AssignmentType assignment, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_ASSIGNMENT)),
	        		prismContext, assignment);
        executeChanges(roleDelta, null, task, result);
	}
	
	protected void modifyRoleDeleteAssignment(String roleOid, AssignmentType assignment, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_ASSIGNMENT)),
	        		prismContext, assignment);
        executeChanges(roleDelta, null, task, result);
	}

	protected PolicyRuleType createExclusionPolicyRule(String excludedRoleOid) {
		PolicyRuleType policyRule = new PolicyRuleType();
		PolicyConstraintsType policyContraints = new PolicyConstraintsType();
		ExclusionPolicyConstraintType exclusionConstraint = new ExclusionPolicyConstraintType();
		ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(excludedRoleOid);
        targetRef.setType(RoleType.COMPLEX_TYPE);
		exclusionConstraint.setTargetRef(targetRef);
		policyContraints.getExclusion().add(exclusionConstraint);
		policyRule.setPolicyConstraints(policyContraints);
		return policyRule;
	}
	
	protected PolicyRuleType createMinAssigneePolicyRule(int minAssignees) {
		PolicyRuleType policyRule = new PolicyRuleType();
		PolicyConstraintsType policyContraints = new PolicyConstraintsType();
		MultiplicityPolicyConstraintType minAssigneeConstraint = new MultiplicityPolicyConstraintType();
		minAssigneeConstraint.setMultiplicity(Integer.toString(minAssignees));
		policyContraints.getMinAssignees().add(minAssigneeConstraint);
		policyRule.setPolicyConstraints(policyContraints);
		return policyRule;
	}
	
	protected PolicyRuleType createMaxAssigneePolicyRule(int maxAssignees) {
		PolicyRuleType policyRule = new PolicyRuleType();
		PolicyConstraintsType policyContraints = new PolicyConstraintsType();
		MultiplicityPolicyConstraintType maxAssigneeConstraint = new MultiplicityPolicyConstraintType();
		maxAssigneeConstraint.setMultiplicity(Integer.toString(maxAssignees));
		policyContraints.getMaxAssignees().add(maxAssigneeConstraint);
		policyRule.setPolicyConstraints(policyContraints);
		return policyRule;
	}
	
	protected AssignmentType createAssignmentIdOnly(long id) {
		AssignmentType assignment = new AssignmentType();
		assignment.asPrismContainerValue().setId(id);
		return assignment;
	}
	
	protected void modifyRoleAddPolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationAddContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_POLICY_EXCEPTION)),
	        		prismContext, policyException);
        executeChanges(roleDelta, null, task, result);
	}
	
	protected void modifyRoleDeletePolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_POLICY_EXCEPTION)),
	        		prismContext, policyException);
        executeChanges(roleDelta, null, task, result);
	}
	
	protected void modifyRoleReplacePolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationReplaceContainer(RoleType.class, roleOid,
	        		new ItemPath(new NameItemPathSegment(RoleType.F_POLICY_EXCEPTION)),
	        		prismContext, policyException);
        executeChanges(roleDelta, null, task, result);
	}
	
	protected PolicyExceptionType createPolicyException(String ruleName, String policySituation) {
		PolicyExceptionType policyException = new PolicyExceptionType();
		policyException.setPolicySituation(policySituation);
		policyException.setRuleName(ruleName);
		return policyException;
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

	protected void modifyRoleDeleteInducementTarget(String roleOid, String targetOid,
			ModelExecuteOptions options) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleDeleteInducementTarget");
        OperationResult result = task.getResult();
        AssignmentType inducement = findInducementByTarget(roleOid, targetOid);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
        		new ItemPath(new NameItemPathSegment(RoleType.F_INDUCEMENT)),
        		prismContext, inducement.asPrismContainerValue().clone());
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
	}

	protected void modifyRoleDeleteInducement(String roleOid, long inducementId, boolean reconcileAffected,
			ModelExecuteOptions defaultOptions, Task task) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		if (task == null) {
            task = createTask(AbstractModelIntegrationTest.class.getName() + ".modifyRoleDeleteInducement");
        }
        OperationResult result = task.getResult();

		AssignmentType inducement = new AssignmentType();
		inducement.setId(inducementId);
        ObjectDelta<RoleType> roleDelta = ObjectDelta.createModificationDeleteContainer(RoleType.class, roleOid,
				RoleType.F_INDUCEMENT, prismContext, inducement);
        ModelExecuteOptions options = nullToEmpty(defaultOptions);
        options.setReconcileAffected(reconcileAffected);
        executeChanges(roleDelta, options, task, result);
        result.computeStatus();
        if (reconcileAffected) {
            TestUtil.assertInProgressOrSuccess(result);
        } else {
            TestUtil.assertSuccess(result);
        }
	}

	@NotNull
	protected ModelExecuteOptions nullToEmpty(ModelExecuteOptions options) {
		return options != null ? options : new ModelExecuteOptions();
	}

	protected void modifyUserAddAccount(String userOid, File accountFile, Task task, OperationResult result) throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<ShadowType> account = prismContext.parseObject(accountFile);

        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userOid, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);

		modelService.executeChanges(deltas, null, task, result);
	}

	protected void assertAuthorized(MidPointPrincipal principal, String action) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertAuthorized", principal);
		assertAuthorized(principal, action, null, task, task.getResult());
		assertAuthorized(principal, action, AuthorizationPhaseType.REQUEST, task, task.getResult());
		assertAuthorized(principal, action, AuthorizationPhaseType.EXECUTION, task, task.getResult());
		assertSuccess(task.getResult());
	}

	protected void assertAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertAuthorized", principal);
		assertAuthorized(principal, action, phase, task, task.getResult());
		assertSuccess(task.getResult());
	}
	
	protected void assertAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		try {
			assertTrue("AuthorizationEvaluator.isAuthorized: Principal "+principal+" NOT authorized for action "+action,
					securityEnforcer.isAuthorized(action, phase, AuthorizationParameters.EMPTY, null, task, result));
			if (phase == null) {
				List<String> requiredActions = new ArrayList<>(1);
				requiredActions.add(action);
				securityEnforcer.decideAccess(getSecurityContextPrincipal(), requiredActions, task, result);
			}
		} finally {
			SecurityContextHolder.setContext(origContext);
		}
	}

	protected void assertNotAuthorized(MidPointPrincipal principal, String action) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertNotAuthorized", principal);
		assertNotAuthorized(principal, action, null, task, task.getResult());
		assertNotAuthorized(principal, action, AuthorizationPhaseType.REQUEST, task, task.getResult());
		assertNotAuthorized(principal, action, AuthorizationPhaseType.EXECUTION, task, task.getResult());
		assertSuccess(task.getResult());
	}

	protected void assertNotAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertNotAuthorized", principal);
		assertNotAuthorized(principal, action, phase, task, task.getResult());
		assertSuccess(task.getResult());
	}
	
	protected void assertNotAuthorized(MidPointPrincipal principal, String action, AuthorizationPhaseType phase, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		SecurityContext origContext = SecurityContextHolder.getContext();
		createSecurityContext(principal);
		boolean isAuthorized = securityEnforcer.isAuthorized(action, phase, AuthorizationParameters.EMPTY, null, task, result);
		SecurityContextHolder.setContext(origContext);
		assertFalse("AuthorizationEvaluator.isAuthorized: Principal " + principal + " IS authorized for action " + action + " (" + phase + ") but he should not be", isAuthorized);
	}

	protected void assertAuthorizations(PrismObject<UserType> user, String... expectedAuthorizations) throws ObjectNotFoundException, SchemaException {
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


	protected void assertNoAuthorizations(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException {
		MidPointPrincipal principal = userProfileService.getPrincipal(user);
		assertNotNull("No principal for "+user, principal);
		assertNoAuthorizations(principal);
	}

	protected void assertNoAuthorizations(MidPointPrincipal principal) {
		if (principal.getAuthorities() != null && !principal.getAuthorities().isEmpty()) {
			AssertJUnit.fail("Unexpected authorizations in "+principal+": "+principal.getAuthorities());
		}
	}

	protected AdminGuiConfigurationType assertAdminGuiConfigurations(MidPointPrincipal principal, int expectedMenuLinks,
			int expectedDashboardLinks, int expectedObjectLists, int expectedObjectForms, int expecteduserDashboardWidgets) {
		AdminGuiConfigurationType adminGuiConfiguration = principal.getAdminGuiConfiguration();
		display("Admin GUI config for "+principal.getUsername(), adminGuiConfiguration);
		assertAdminGuiConfigurations(adminGuiConfiguration,
				expectedMenuLinks, expectedDashboardLinks, expectedObjectLists, expectedObjectForms, expecteduserDashboardWidgets);
		return adminGuiConfiguration;
	}

	protected void assertAdminGuiConfigurations(AdminGuiConfigurationType adminGuiConfiguration,
			int expectedMenuLinks, int expectedDashboardLinks, int expectedObjectLists, int expectedObjectForms, int expecteduserDashboardWidgets) {
		assertNotNull("No admin GUI configuration", adminGuiConfiguration);
		assertEquals("Wrong number of menu links in",
				expectedMenuLinks, adminGuiConfiguration.getAdditionalMenuLink().size());
		assertEquals("Wrong number of menu links in",
				expectedDashboardLinks, adminGuiConfiguration.getUserDashboardLink().size());
		if ( adminGuiConfiguration.getObjectLists() == null ) {
			if (expectedObjectLists != 0) {
				AssertJUnit.fail("Wrong number of object lists in user dashboard admin GUI configuration, expected "
						+ expectedObjectLists + " but there was none");
			}
		} else {
			assertEquals("Wrong number of object lists in admin GUI configuration",
				expectedObjectLists, adminGuiConfiguration.getObjectLists().getObjectList().size());
		}
		assertEquals("Wrong number of object forms in admin GUI configuration",
				expectedObjectForms, adminGuiConfiguration.getObjectForms().getObjectForm().size());
		if ( adminGuiConfiguration.getUserDashboard() == null) {
			if (expecteduserDashboardWidgets != 0) {
				AssertJUnit.fail("Wrong number of widgets in user dashboard admin GUI configuration, expected "
						+ expecteduserDashboardWidgets + " but there was none");
			}
		} else {
			assertEquals("Wrong number of widgets in user dashboard admin GUI configuration",
				expectedObjectForms, adminGuiConfiguration.getUserDashboard().getWidget().size());
		}
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
		Collection<ConfigAttribute> attrs = new ArrayList<>();
		attrs.add(new SecurityConfig(action));
		return attrs;
	}

	protected <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".getEditObjectDefinition");
		OperationResult result = task.getResult();
		PrismObjectDefinition<O> editSchema = modelInteractionService.getEditObjectDefinition(object, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return editSchema;
	}

	protected <F extends FocusType> void assertRoleTypes(PrismObject<F> focus, String... expectedRoleTypes) throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		assertRoleTypes(getAssignableRoleSpecification(focus), expectedRoleTypes);
	}

	protected <F extends FocusType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<F> focus) throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class+".getAssignableRoleSpecification");
		OperationResult result = task.getResult();
		RoleSelectionSpecification spec = modelInteractionService.getAssignableRoleSpecification(focus, task, result);
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
	
	protected void assertAllowRequestAssignmentItems(String userOid, String targetRoleOid, ItemPath... expectedAllowedItemPaths)
			throws SchemaException, SecurityViolationException, CommunicationException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
		Task task = createTask(AbstractModelIntegrationTest.class.getName() + ".assertAllowRequestItems");
        OperationResult result = task.getResult();
        assertAllowRequestAssignmentItems(userOid, targetRoleOid, task, result, expectedAllowedItemPaths);
        assertSuccess(result);
	}

	protected void assertAllowRequestAssignmentItems(String userOid, String targetRoleOid, Task task, OperationResult result, ItemPath... expectedAllowedItemPaths) 
			throws SchemaException, SecurityViolationException, CommunicationException, ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<UserType> user = getUser(userOid);
		PrismObject<RoleType> target = getRole(targetRoleOid);

		ItemSecurityConstraints constraints = modelInteractionService.getAllowedRequestAssignmentItems(user, target, task, result);
		display("Request decisions for "+target, constraints);

		for (ItemPath expectedAllowedItemPath: expectedAllowedItemPaths) {
			AuthorizationDecisionType decision = constraints.findItemDecision(expectedAllowedItemPath);
			assertEquals("Wrong decision for item "+expectedAllowedItemPath, AuthorizationDecisionType.ALLOW, decision);
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

	@Deprecated
	protected void assertPasswordMetadata(PrismObject<UserType> user, boolean create, XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
		PrismContainer<MetadataType> metadataContainer = user.findContainer(new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_METADATA));
		assertNotNull("No password metadata in "+user, metadataContainer);
		MetadataType metadataType = metadataContainer.getValue().asContainerable();
		assertMetadata("password metadata in "+user, metadataType, create, false, start, end, actorOid, channel);
	}

	protected void assertPasswordMetadata(PrismObject<UserType> user, QName credentialType, boolean create, XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
		PrismContainer<MetadataType> metadataContainer = user.findContainer(new ItemPath(UserType.F_CREDENTIALS, credentialType, PasswordType.F_METADATA));
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

	protected void assertDummyPassword(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		assertEquals("Wrong password in dummy '"+instance+"' account "+userId, expectedClearPassword, account.getPassword());
	}

	protected void assertDummyPasswordNotEmpty(String instance, String userId) throws SchemaViolationException, ConflictException {
		DummyAccount account = getDummyAccount(instance, userId);
		assertNotNull("No dummy account "+userId, account);
		String actualPassword = account.getPassword();
		if (actualPassword == null || actualPassword.isEmpty()) {
			fail("Empty password in dummy '"+instance+"' account "+userId);
		}
	}

	protected void reconcileUser(String oid, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		reconcileUser(oid, null, task, result);
	}

	protected void reconcileUser(String oid, ModelExecuteOptions options, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		ObjectDelta<UserType> emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, oid, prismContext);
		modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), ModelExecuteOptions.createReconcile(options), task, result);
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

	protected List<AuditEventRecord> getAllAuditRecords(Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Map<String,Object> params = new HashMap<>();
		return modelAuditService.listRecords("from RAuditEventRecord as aer order by aer.timestamp asc", params, task, result);
	}

	protected List<AuditEventRecord> getObjectAuditRecords(String oid) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Task task = createTask("getObjectAuditRecords");
		OperationResult result = task.getResult();
		return getObjectAuditRecords(oid, task, result);
	}

	protected List<AuditEventRecord> getObjectAuditRecords(String oid, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Map<String,Object> params = new HashMap<>();
		params.put("targetOid", oid);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.targetOid = :targetOid) order by aer.timestamp asc",
        		params, task, result);
	}

	protected List<AuditEventRecord> getParamAuditRecords(String paramName, String paramValue, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Map<String,Object> params = new HashMap<>();
		params.put("paramName", paramName);
		params.put("paramValue", paramValue);
		return modelAuditService.listRecords("from RAuditEventRecord as aer left join aer.propertyValues as pv where (pv.name = :paramName and pv.value = :paramValue) order by aer.timestamp asc",
        		params, task, result);
	}

	protected List<AuditEventRecord> getAuditRecordsFromTo(XMLGregorianCalendar from, XMLGregorianCalendar to) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Task task = createTask("getAuditRecordsFromTo");
		OperationResult result = task.getResult();
		return getAuditRecordsFromTo(from, to, task, result);
	}

	protected List<AuditEventRecord> getAuditRecordsFromTo(XMLGregorianCalendar from, XMLGregorianCalendar to, Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
		Map<String,Object> params = new HashMap<>();
		params.put("from", from);
		params.put("to", to);
		return modelAuditService.listRecords("from RAuditEventRecord as aer where (aer.timestamp >= :from) and (aer.timestamp <= :to) order by aer.timestamp asc",
        		params, task, result);
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
		HashSet<String> realApproversSet = new HashSet<>();
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
		modelService.recompute(object.asObjectable().getClass(), object.getOid(), null, task, result);
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

	protected void resetTriggerTask(String taskOid, File taskFile, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, FileNotFoundException {
		taskManager.suspendAndDeleteTasks(Collections.singletonList(taskOid), 60000L, true, result);
		importObjectFromFile(taskFile, result);
		taskManager.suspendTasks(Collections.singletonList(taskOid), 60000L, result);
		modifySystemObjectInRepo(TaskType.class, taskOid,
				DeltaBuilder.deltaFor(TaskType.class, prismContext)
						.item(TaskType.F_SCHEDULE).replace()
						.asItemDeltas(),
				result);
		taskManager.resumeTasks(singleton(taskOid), result);
	}

	protected void repoAddObjects(List<ObjectType> objects, OperationResult result)
			throws EncryptionException, ObjectAlreadyExistsException, SchemaException {
		for (ObjectType object : objects) {
			repoAddObject(object.asPrismObject(), result);
		}
	}

	protected void recomputeAndRefreshObjects(List<ObjectType> objects, Task task, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
			SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
		for (int i = 0; i < objects.size(); i++) {
			ObjectType object = objects.get(i);
			modelService.recompute(object.getClass(), object.getOid(), null, task, result);
			objects.set(i, repositoryService.getObject(object.getClass(), object.getOid(), null, result).asObjectable());
		}
	}

	protected void dumpAllUsers(OperationResult initResult) throws SchemaException {
		SearchResultList<PrismObject<UserType>> users = repositoryService
				.searchObjects(UserType.class, null, null, initResult);
		display("Users", users);
	}

	protected <F extends FocusType, P extends FocusType> PrismObject<P> assertLinkedPersona(PrismObject<F> focus, Class<P> personaClass, String subtype) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinkedPersona");
		for (ObjectReferenceType personaRef: focus.asObjectable().getPersonaRef()) {
			PrismObject<P> persona = repositoryService.getObject((Class<P>)ObjectTypes.getObjectTypeFromTypeQName(personaRef.getType()).getClassDefinition(),
					personaRef.getOid(), null, result);
			if (isTypeAndSubtype(persona, personaClass, subtype)) {
				return persona;
			}
		}
		fail("No persona "+personaClass.getSimpleName()+"/"+subtype+" in "+focus);
		return null; // not reached
	}

	protected <F extends FocusType> boolean isTypeAndSubtype(PrismObject<F> focus, Class<F> expectedType, String subtype) {
		if (!expectedType.isAssignableFrom(focus.getCompileTimeClass())) {
			return false;
		}
		if (!FocusTypeUtil.hasSubtype(focus, subtype)) {
			return false;
		}
		return true;
	}

	protected PrismObject<OrgType> getOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, createPolyString(orgName));
		return org;
	}

	protected void dumpOrgTree() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		display("Org tree", dumpOrgTree(getTopOrgOid()));
	}
	
	protected void dumpOrgTreeAndUsers() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		display("Org tree", dumpOrgTree(getTopOrgOid(), true));
	}

	protected String getTopOrgOid() {
		return null;
	}

	protected void transplantGlobalPolicyRulesAdd(File configWithGlobalRulesFile, Task task, OperationResult parentResult) throws SchemaException, IOException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// copy rules from the file into live system config object
		PrismObject<SystemConfigurationType> rules = prismContext.parserFor(configWithGlobalRulesFile).parse();
		ObjectDelta<SystemConfigurationType> delta = DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
				.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE).add(
					rules.asObjectable().getGlobalPolicyRule().stream()
							.map(r -> r.clone().asPrismContainerValue())
							.collect(Collectors.toList()))
				.asObjectDeltaCast(SystemObjectsType.SYSTEM_CONFIGURATION.value());
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, parentResult);
	}

	protected void displayProvisioningScripts() {
		displayProvisioningScripts(null);
	}

	protected void displayProvisioningScripts(String dummyName) {
		display("Provisioning scripts "+dummyName, getDummyResource(dummyName).getScriptHistory());
	}

	protected void purgeProvisioningScriptHistory() {
		purgeProvisioningScriptHistory(null);
	}

	protected void purgeProvisioningScriptHistory(String dummyName) {
		getDummyResource(dummyName).purgeScriptHistory();
	}

	protected void assertNoProvisioningScripts() {
		if (!getDummyResource().getScriptHistory().isEmpty()) {
			IntegrationTestTools.displayScripts(getDummyResource().getScriptHistory());
			AssertJUnit.fail(getDummyResource().getScriptHistory().size()+" provisioning scripts were executed while not expected any");
		}
	}

	protected void assertDummyProvisioningScriptsNone() {
		IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
	}

	protected void applyPasswordPolicy(String passwordPolicyOid, String securityPolicyOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
		PrismReferenceValue passPolicyRef = new PrismReferenceValue(passwordPolicyOid, ValuePolicyType.COMPLEX_TYPE);
		modifyObjectReplaceReference(SecurityPolicyType.class, securityPolicyOid,
				new ItemPath(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD, PasswordCredentialsPolicyType.F_VALUE_POLICY_REF),
        		task, result, passPolicyRef);
	}

	protected void assertPasswordCompliesWithPolicy(PrismObject<UserType> user, String passwordPolicyOid) throws EncryptionException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = createTask("assertPasswordCompliesWithPolicy");
		OperationResult result = task.getResult();
		assertPasswordCompliesWithPolicy(user, passwordPolicyOid, task, result);
		assertSuccess(result);
	}

	protected void assertPasswordCompliesWithPolicy(PrismObject<UserType> user, String passwordPolicyOid, Task task, OperationResult result) throws EncryptionException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		String password = getPassword(user);
		display("Password of "+user, password);
		PrismObject<ValuePolicyType> passwordPolicy = repositoryService.getObject(ValuePolicyType.class, passwordPolicyOid, null, result);
		List<LocalizableMessage> messages = new ArrayList<>();
		boolean valid = valuePolicyProcessor.validateValue(password, passwordPolicy.asObjectable(), createUserOriginResolver(user), messages, "validating password of "+user, task, result);
		if (!valid) {
			fail("Password for "+user+" does not comply with password policy: "+messages);
		}
	}

	protected UserValuePolicyOriginResolver createUserOriginResolver(PrismObject<UserType> user) {
		if (user == null) {
			return null;
		}
		return new UserValuePolicyOriginResolver(user, modelObjectResolver);
	}

	protected XMLGregorianCalendar getTimestamp(String duration) {
		return XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), duration);
	}

	protected void clockForward(String duration) {
		XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
		clock.overrideDuration(duration);
		XMLGregorianCalendar after = clock.currentTimeXMLGregorianCalendar();
		display("Clock going forward", before + " --[" + duration + "]--> " + after);
	}

	protected List<PrismObject<TaskType>> getTasksForObject(String oid, QName type,
			Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
			ConfigurationException, ExpressionEvaluationException {
		ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
				.item(TaskType.F_OBJECT_REF).ref(new PrismReferenceValue(oid, type))
				.build();
		return modelService.searchObjects(TaskType.class, query, options, task, result);
	}

	protected TaskType getApprovalTask(List<PrismObject<TaskType>> tasks) {
		List<TaskType> rv = tasks.stream()
				.map(o -> o.asObjectable())
				.filter(t -> t.getWorkflowContext() != null && t.getWorkflowContext().getProcessInstanceId() != null)
				.collect(Collectors.toList());
		if (rv.isEmpty()) {
			throw new AssertionError("No approval task found");
		} else if (rv.size() > 1) {
			throw new AssertionError("More than one approval task found: " + rv);
		} else {
			return rv.get(0);
		}
	}

	protected TaskType getRootTask(List<PrismObject<TaskType>> tasks) {
		List<TaskType> rv = tasks.stream()
				.map(o -> o.asObjectable())
				.filter(t -> t.getParent() == null)
				.collect(Collectors.toList());
		if (rv.isEmpty()) {
			throw new AssertionError("No root task found");
		} else if (rv.size() > 1) {
			throw new AssertionError("More than one root task found: " + rv);
		} else {
			return rv.get(0);
		}
	}

	// highly experimental
	public class TestCtx {
		public final String name;

		public final Task task;
		public final OperationResult result;

		TestCtx(Object testCase, String name) {
			this.name = name;
			TestUtil.displayTestTitle(testCase, name);
			task = taskManager.createTaskInstance(testCase.getClass().getName() + "." + name);
			result = task.getResult();
			dummyAuditService.clear();
		}

		public void displayWhen() {
			TestUtil.displayWhen(name);
		}

		public void displayThen() {
			TestUtil.displayThen(name);
		}
	}

	protected TestCtx createContext(Object testCase, String testName) {
		return new TestCtx(testCase, testName);
	}
	
	protected <T> T runPrivileged(Producer<T> producer) {
		return securityContextManager.runPrivileged(producer);
	}

	protected void assertPendingOperationDeltas(PrismObject<ShadowType> shadow, int expectedNumber) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wrong number of pending operations in "+shadow, expectedNumber, pendingOperations.size());
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertSinglePendingOperation(shadow, requestStart, requestEnd,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0), 
				null, null, expectedExecutionStatus, expectedResultStatus, null, null);
	}

	protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertPendingOperationDeltas(shadow, 1);
		return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
				requestStart, requestEnd, expectedStatus, completionStart, completionEnd);
	}

	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
		return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResultStatus) {
		return assertPendingOperation(shadow, pendingOperation, null, null,
				expectedExecutionStatus, expectedResultStatus, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation) {
		return assertPendingOperation(shadow, pendingOperation, null, null,
				OperationResultStatusType.IN_PROGRESS, null, null);
	}
	
	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			OperationResultStatusType expectedResultStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		PendingOperationExecutionStatusType expectedExecutionStatus;
		if (expectedResultStatus == OperationResultStatusType.IN_PROGRESS) {
			expectedExecutionStatus = PendingOperationExecutionStatusType.EXECUTING;
		} else {
			expectedExecutionStatus = PendingOperationExecutionStatusType.COMPLETED;
		}
		return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd, expectedExecutionStatus, expectedResultStatus, completionStart, completionEnd);
	}

	protected PendingOperationType assertPendingOperation(
			PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
			XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
			PendingOperationExecutionStatusType expectedExecutionStatus,
			OperationResultStatusType expectedResultStatus,
			XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
		assertNotNull("No operation ", pendingOperation);

		ObjectDeltaType deltaType = pendingOperation.getDelta();
		assertNotNull("No delta in pending operation in "+shadow, deltaType);
		// TODO: check content of pending operations in the shadow

		TestUtil.assertBetween("No request timestamp in pending operation in "+shadow, requestStart, requestEnd, pendingOperation.getRequestTimestamp());

		PendingOperationExecutionStatusType executiontStatus = pendingOperation.getExecutionStatus();
		assertEquals("Wrong execution status in pending operation in "+shadow, expectedExecutionStatus, executiontStatus);
		
		OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
		assertEquals("Wrong result status in pending operation in "+shadow, expectedResultStatus, resultStatus);

		// TODO: assert other timestamps
		
		if (expectedExecutionStatus == PendingOperationExecutionStatusType.COMPLETED) {
			TestUtil.assertBetween("No completion timestamp in pending operation in "+shadow, completionStart, completionEnd, pendingOperation.getCompletionTimestamp());
		}

		return pendingOperation;
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStaus) {
		return findPendingOperation(shadow, expectedExecutionStaus, null, null, null);
	}
	
	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult) {
		return findPendingOperation(shadow, null, expectedResult, null, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ItemPath itemPath) {
		return findPendingOperation(shadow, null, expectedResult, null, itemPath);
	}
	
	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStaus, ItemPath itemPath) {
		return findPendingOperation(shadow, expectedExecutionStaus, null, null, itemPath);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			OperationResultStatusType expectedResult, ChangeTypeType expectedChangeType) {
		return findPendingOperation(shadow, null, expectedResult, expectedChangeType, null);
	}

	protected PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
			PendingOperationExecutionStatusType expectedExecutionStatus, OperationResultStatusType expectedResult,
			ChangeTypeType expectedChangeType, ItemPath itemPath) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		for (PendingOperationType pendingOperation: pendingOperations) {
			if (expectedExecutionStatus != null && !expectedExecutionStatus.equals(pendingOperation.getExecutionStatus())) {
				continue;
			}
			if (expectedResult != null && !expectedResult.equals(pendingOperation.getResultStatus())) {
				continue;
			}
			ObjectDeltaType delta = pendingOperation.getDelta();
			if (expectedChangeType != null) {
				if (!expectedChangeType.equals(delta.getChangeType())) {
					continue;
				}
			}
			if (itemPath == null) {
				return pendingOperation;
			}
			assertNotNull("No delta in pending operation in "+shadow, delta);
			for (ItemDeltaType itemDelta: delta.getItemDelta()) {
				ItemPath deltaPath = itemDelta.getPath().getItemPath();
				if (itemPath.equivalent(deltaPath)) {
					return pendingOperation;
				}
			}
		}
		return null;
	}

}
