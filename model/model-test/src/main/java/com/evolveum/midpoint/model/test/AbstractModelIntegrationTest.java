/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test;

import static com.evolveum.midpoint.model.api.validator.StringLimitationResult.extractMessages;
import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.Referencable.getOid;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.OPERATION_GET_PRINCIPAL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ORIGINAL_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismObject.asObjectableList;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieveCollection;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType.F_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType.F_ROLE_MANAGEMENT;

import java.io.*;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.authentication.api.AutheticationFailedData;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.model.common.MarkManager;
import com.evolveum.midpoint.model.test.util.ShadowFindRequest.ShadowFindRequestBuilder;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.*;

import com.evolveum.midpoint.security.enforcer.api.ValueAuthorizationParameters;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.model.api.util.ReferenceResolver;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.common.stringpolicy.FocusValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.test.asserter.*;
import com.evolveum.midpoint.model.test.util.SynchronizationRequest.SynchronizationRequestBuilder;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingManager;
import com.evolveum.midpoint.repo.common.activity.run.reports.ActivityReportUtil;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ProvisioningStatistics;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.test.asserter.*;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerDefinitionAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * Abstract framework for an integration test that is placed on top of a model API.
 * This provides complete environment that the test should need, e.g model service instance, repository, provisioning,
 * dummy auditing, etc. It also implements lots of useful methods to make writing the tests easier.
 *
 * @author Radovan Semancik
 */
public abstract class AbstractModelIntegrationTest extends AbstractIntegrationTest
        implements ResourceTester {

    protected static final String CONNECTOR_DUMMY_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";
    protected static final String CONNECTOR_DUMMY_VERSION = "2.0";
    protected static final String CONNECTOR_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";

    protected static final ItemPath ACTIVATION_ADMINISTRATIVE_STATUS_PATH = SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;
    protected static final ItemPath ACTIVATION_VALID_FROM_PATH = SchemaConstants.PATH_ACTIVATION_VALID_FROM;
    protected static final ItemPath ACTIVATION_VALID_TO_PATH = SchemaConstants.PATH_ACTIVATION_VALID_TO;
    protected static final ItemPath PASSWORD_VALUE_PATH = SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

    private static final String DEFAULT_CHANNEL = SchemaConstants.CHANNEL_USER_URI;

    protected static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
    protected static final ItemName EXT_SEA = new ItemName(NS_PIRACY, "sea");
    protected static final ItemName EXT_RESOURCE_NAME = new ItemName(NS_PIRACY, "resourceName");

    protected static final String NS_LINKED = "http://midpoint.evolveum.com/xml/ns/samples/linked";
    public static final ItemName RECOMPUTE_MEMBERS_NAME = new ItemName(NS_LINKED, "recomputeMembers");

    protected static final String LOG_PREFIX_FAIL = "SSSSS=X ";
    protected static final String LOG_PREFIX_ATTEMPT = "SSSSS=> ";
    protected static final String LOG_PREFIX_DENY = "SSSSS=- ";
    protected static final String LOG_PREFIX_ALLOW = "SSSSS=+ ";

    @Autowired protected TaskActivityManager activityManager;
    @Autowired protected ModelService modelService;
    @Autowired protected ModelInteractionService modelInteractionService;
    @Autowired protected ModelDiagnosticService modelDiagnosticService;
    @Autowired protected DashboardService dashboardService;
    @Autowired protected ModelAuditService modelAuditService;
    @Autowired protected ActivityBasedTaskHandler activityBasedTaskHandler;
    @Autowired protected ArchetypeManager archetypeManager;
    @Autowired protected RoleAnalysisService roleAnalysisService;

    @Autowired
    @Qualifier("repositoryService")
    protected RepositoryService plainRepositoryService;

    @Autowired protected BucketingManager bucketingManager;

    @Autowired protected ReferenceResolver referenceResolver;
    @Autowired protected SystemObjectCache systemObjectCache;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected HookRegistry hookRegistry;
    @Autowired protected Clock clock;
    @Autowired protected SchemaService schemaService;
    @Autowired protected SecurityEnforcer securityEnforcer;
    @Autowired protected SecurityContextManager securityContextManager;
    @Autowired protected MidpointFunctions libraryMidpointFunctions;
    @Autowired protected ValuePolicyProcessor valuePolicyProcessor;
    @Autowired protected SimulationResultManager simulationResultManager;
    @Autowired protected MarkManager markManager;

    @Autowired(required = false)
    @Qualifier("modelObjectResolver")
    protected ObjectResolver modelObjectResolver;

    @Autowired(required = false)
    protected TransportService transportService;

    @Autowired(required = false)
    protected NotificationManager notificationManager;

    protected final DummyTransport dummyTransport = new DummyTransport();

    @Autowired(required = false)
    protected GuiProfiledPrincipalManager focusProfileService;

    @Autowired protected CommonTaskBeans commonTaskBeans;

    protected DummyResourceCollection dummyResourceCollection;

    protected DummyAuditService dummyAuditService;

    /**
     * Is the computation of access metadata (on `roleMembershipRef`) enabled? Currently, it influences some asserts
     * on audit events.
     *
     * Set only _AFTER_ the initialization is complete - in {@link #postInitSystem(Task, OperationResult)} method.
     */
    private boolean accessesMetadataEnabled;

    public AbstractModelIntegrationTest() {
        dummyAuditService = DummyAuditService.getInstance();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        dummyResourceCollection = new DummyResourceCollection(modelService);
        startResources();
        InternalsConfig.reset();
        InternalsConfig.setAvoidLoggingChange(isAvoidLoggingChange());
        // Make sure the checks are turned on
        InternalsConfig.turnOnAllChecks();
        // By default, notifications are turned off because of performance implications.
        // Individual tests turn them on for themselves.
        if (notificationManager != null) {
            notificationManager.setDisabled(true);
        }
        if (transportService != null) {
            transportService.registerTransport(dummyTransport);
        }

        // This is generally useful in tests, to avoid long waiting for bucketed tasks.
        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(100L);

        // We generally do not import all the archetypes for all kinds of tasks (at least not now).
        activityBasedTaskHandler.setAvoidAutoAssigningArchetypes(true);
    }

    @Override
    public void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
        if (dummyResourceCollection != null) {
            dummyResourceCollection.resetResources();
        }
        accessesMetadataEnabled = SystemConfigurationTypeUtil.isAccessesMetadataEnabled(
                systemObjectCache.getSystemConfigurationBean(initResult));
    }

    protected boolean isAvoidLoggingChange() {
        return true;
    }

    protected void startResources() throws Exception {
        // Nothing to do by default
    }

    @AfterClass
    protected void cleanUpSecurity() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(null);
    }

    @BeforeMethod
    protected void prepareBeforeTestMethod() {
        dummyAuditService.clear();
    }

    protected void initDummyResource(String name, DummyResourceContoller controller) {
        dummyResourceCollection.initDummyResource(name, controller);
    }

    protected DummyResourceContoller initDummyResource(String name, File resourceFile, String resourceOid,
            FailableProcessor<DummyResourceContoller> controllerInitLambda,
            Task task, OperationResult result) throws Exception {
        return dummyResourceCollection.initDummyResource(name, resourceFile, resourceOid, controllerInitLambda, task, result);
    }

    @Override
    public DummyResourceContoller initDummyResource(DummyTestResource resource, Task task, OperationResult result)
            throws Exception {
        resource.controller = dummyResourceCollection.initDummyResourceRepeatable(resource, task, result);
        resource.reload(result); // To have schema, etc
        return resource.controller;
    }

    @Override
    public void initAndTestDummyResource(DummyTestResource resource, Task task, OperationResult result)
            throws Exception {
        resource.controller = dummyResourceCollection.initDummyResourceRepeatable(resource, task, result);
        assertSuccess(
                modelService.testResource(resource.controller.getResource().getOid(), task, result));
        resource.reload(result); // To have schema, etc
    }

    protected DummyResourceContoller initDummyResource(String name, File resourceFile, String resourceOid,
            Task task, OperationResult result) throws Exception {
        return dummyResourceCollection.initDummyResource(name, resourceFile, resourceOid, null, task, result);
    }

    protected DummyResourceContoller initDummyResourcePirate(
            String name, File resourceFile, String resourceOid, Task task, OperationResult result)
            throws Exception {
        return initDummyResource(name, resourceFile, resourceOid, DummyResourceContoller::extendSchemaPirate, task, result);
    }

    protected DummyResourceContoller initDummyResourceAd(
            String name, File resourceFile, String resourceOid, Task task, OperationResult result)
            throws Exception {
        return initDummyResource(name, resourceFile, resourceOid, DummyResourceContoller::extendSchemaAd, task, result);
    }

    protected DummyResourceContoller getDummyResourceController(String name) {
        return dummyResourceCollection.get(name);
    }

    protected DummyResourceContoller getDummyResourceController() {
        return getDummyResourceController(null);
    }

    protected DummyAccountAsserter<Void> assertDummyAccountByUsername(
            String dummyResourceName, String username)
            throws ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        return getDummyResourceController(dummyResourceName).assertAccountByUsername(username);
    }

    protected DummyGroupAsserter<Void> assertDummyGroupByName(String dummyResourceName, String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        return getDummyResourceController(dummyResourceName).assertGroupByName(name);
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

    // TODO reconcile with TestObject.importObject method (they use similar but distinct model APIs)
    protected void importObject(TestObject<?> testObject, Task task, OperationResult result) throws IOException {
        OperationResult subresult = result.createSubresult("importObject");
        try (InputStream stream = testObject.getInputStream()) {
            modelService.importObjectsFromStream(
                    stream, PrismContext.LANG_XML, MiscSchemaUtil.getDefaultImportOptions(), task, subresult);
        }
        subresult.close();
        TestUtil.assertSuccess(subresult);
    }

    protected void importObjectFromFile(File file) throws FileNotFoundException {
        OperationResult result = new OperationResult(contextName() + ".importObjectFromFile");
        importObjectFromFile(file, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
        importObjectFromFile(new File(filename), result);
    }

    protected void importObjectFromFile(File file, OperationResult result) throws FileNotFoundException {
        OperationResult subResult = result.createSubresult(contextName() + ".importObjectFromFile");
        subResult.addParam("filename", file.getPath());
        logger.trace("importObjectFromFile: {}", file);
        Task task = createPlainTask("importObjectFromFile");
        importObjectFromFile(file, task, result);
        subResult.computeStatus();
        if (subResult.isError()) {
            logger.error("Import of file " + file + " failed:\n{}", subResult.debugDump());
            Throwable cause = findCause(subResult);
            throw new SystemException("Import of file " + file + " failed: " + subResult.getMessage(), cause);
        }
    }

    protected void importObjectFromFile(File file, Task task, OperationResult result) throws FileNotFoundException {
        importObjectFromFile(file, MiscSchemaUtil.getDefaultImportOptions(), task, result);
    }

    protected void importObjectFromFile(File file, ImportOptionsType options, Task task, OperationResult result) throws FileNotFoundException {
        FileInputStream stream = new FileInputStream(file);
        modelService.importObjectsFromStream(stream, PrismContext.LANG_XML, options, task, result);
    }

    protected void importObjectsFromFileNotRaw(File file, Task task, OperationResult result) throws FileNotFoundException {
        ImportOptionsType options = MiscSchemaUtil.getDefaultImportOptions();
        ModelExecuteOptionsType modelOptions = new ModelExecuteOptionsType();
        modelOptions.setRaw(false);
        options.setModelExecutionOptions(modelOptions);
        importObjectFromFile(file, options, task, result);
    }

    protected void importObjectsFromFileRaw(File file, Task task, OperationResult result) throws FileNotFoundException {
        ImportOptionsType options = MiscSchemaUtil.getDefaultImportOptions();
        ModelExecuteOptionsType modelOptions = new ModelExecuteOptionsType();
        modelOptions.setRaw(true);
        options.setModelExecutionOptions(modelOptions);
        importObjectFromFile(file, options, task, result);
    }

    protected Throwable findCause(OperationResult result) {
        if (result.getCause() != null) {
            return result.getCause();
        }
        for (OperationResult sub : result.getSubresults()) {
            Throwable cause = findCause(sub);
            if (cause != null) {
                return cause;
            }
        }
        return null;
    }

    protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(
            Class<T> type, String filename, String oid, Task task, OperationResult result)
            throws FileNotFoundException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return importAndGetObjectFromFile(type, new File(filename), oid, task, result);
    }

    protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFile(
            Class<T> type, File file, String oid, Task task, OperationResult result)
            throws FileNotFoundException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        importObjectFromFile(file, result);
        OperationResult importResult = result.getLastSubresult();
        TestUtil.assertSuccess("Import of " + file + " has failed", importResult);
        return modelService.getObject(type, oid, null, task, result);
    }

    protected <T extends ObjectType> PrismObject<T> importAndGetObjectFromFileIgnoreWarnings(
            Class<T> type, File file, String oid, Task task, OperationResult result)
            throws FileNotFoundException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        importObjectFromFile(file, result);
        OperationResult importResult = result.getLastSubresult();
        OperationResultStatus status = importResult.getStatus();
        if (status == OperationResultStatus.FATAL_ERROR || status == OperationResultStatus.PARTIAL_ERROR) {
            fail("Import of " + file + " has failed: " + importResult.getMessage());
        }
        return modelService.getObject(type, oid, null, task, result);
    }

    protected void applyResourceSchema(ShadowType accountType, ResourceType resourceType)
            throws SchemaException, ConfigurationException {
        IntegrationTestTools.applyResourceSchema(accountType, resourceType);
    }

    protected void assertUsers(int expectedNumberOfUsers)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertObjects(UserType.class, expectedNumberOfUsers);
    }

    protected void assertRoles(int expectedNumberOfUsers)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertObjects(RoleType.class, expectedNumberOfUsers);
    }

    protected void assertServices(int expectedNumberOfUsers)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertObjects(ServiceType.class, expectedNumberOfUsers);
    }

    protected <O extends ObjectType> void assertObjects(Class<O> type, int expectedNumberOfUsers)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertObjects(type, null, expectedNumberOfUsers);
    }

    protected <O extends ObjectType> void assertObjects(
            Class<O> type, ObjectQuery query, int expectedNumberOfUsers)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertEquals("Unexpected number of " + type.getSimpleName() + "s", expectedNumberOfUsers, getObjectCount(type, query));
    }

    protected <O extends ObjectType> int getObjectCount(Class<O> type)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getObjectCount(type, null);
    }

    protected <O extends ObjectType> int getObjectCount(Class<O> type, ObjectQuery query)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("getObjectCount");
        OperationResult result = task.getResult();
        List<PrismObject<O>> users = modelService.searchObjects(type, query, null, task, result);
        if (verbose) {display(type.getSimpleName() + "s", users);}
        return users.size();
    }

    protected <O extends ObjectType> void searchObjectsIterative(Class<O> type,
            ObjectQuery query, Consumer<PrismObject<O>> handler, Integer expectedNumberOfObjects)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("searchObjectsIterative");
        OperationResult result = task.getResult();
        final MutableInt count = new MutableInt(0);
        // result is ignored
        modelService.searchObjectsIterative(type, query, (object, oresult) -> {
            count.increment();
            if (handler != null) {
                handler.accept(object);
            }
            return true;
        }, null, task, result);
        if (verbose) {displayValue(type.getSimpleName() + "s", count.getValue());}
        assertEquals("Unexpected number of " + type.getSimpleName() + "s", expectedNumberOfObjects, count.getValue());
    }

    protected void assertUserProperty(String userOid, QName propertyName, Object... expectedPropValues) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getObject");
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertUserProperty(user, propertyName, expectedPropValues);
    }

    protected void assertUserNoProperty(String userOid, QName propertyName)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getObject");
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertUserNoProperty(user, propertyName);
    }

    protected void assertUserProperty(PrismObject<UserType> user, QName propertyName, Object... expectedPropValues) {
        PrismProperty<Object> property = user.findProperty(ItemName.fromQName(propertyName));
        assert property != null : "No property " + propertyName + " in " + user;
        PrismAsserts.assertPropertyValue(property, expectedPropValues);
    }

    protected void assertUserNoProperty(PrismObject<UserType> user, QName propertyName) {
        PrismProperty<Object> property = user.findProperty(ItemName.fromQName(propertyName));
        assert property == null : "Property " + propertyName + " present in " + user + ": " + property;
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
        assert statusProperty != null : "No status property in " + object;
        ActivationStatusType status = statusProperty.getRealValue();
        if (expected == null && status == null) {
            return;
        }
        assert status != null : "No status property is null in " + object;
        assert status == expected : "status property is " + status + ", expected " + expected + " in " + object;
    }

    protected <F extends FocusType> void assertEffectiveStatus(PrismObject<F> focus, ActivationStatusType expected) {
        ActivationType activation = focus.asObjectable().getActivation();
        assertNotNull("No activation in " + focus, activation);
        assertEquals("Unexpected effective activation status in " + focus, expected, activation.getEffectiveStatus());
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

    protected <O extends ObjectType> void modifyObjectReplaceReference(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Referencable newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceReference(type, oid, propertyPath, newRealValue.asReferenceValue());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        modifyObjectReplaceProperty(type, oid, propertyPath, null, task, result, newRealValue);
    }

    protected <O extends ObjectType> void modifyObjectReplaceProperty(Class<O> type, String oid, ItemPath propertyPath, ModelExecuteOptions options, Task task, OperationResult result, Object... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, options, task, result);
    }

    protected <O extends ObjectType> void modifyObjectDeleteProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationDeleteProperty(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected <O extends ObjectType> void modifyObjectAddProperty(Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationAddProperty(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    @SafeVarargs
    protected final <O extends ObjectType, C extends Containerable> void modifyObjectReplaceContainer(
            Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceContainer(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    @SafeVarargs
    protected final <O extends ObjectType, C extends Containerable> void modifyObjectAddContainer(
            Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    @SafeVarargs
    protected final <O extends ObjectType, C extends Containerable> void modifyObjectDeleteContainer(
            Class<O> type, String oid, ItemPath propertyPath, Task task, OperationResult result, C... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(type, oid, propertyPath, newRealValue);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected <O extends ObjectType> void modifyObjectReplaceReference(
            Class<O> type, String oid, ItemPath refPath, Task task, OperationResult result, PrismReferenceValue... refVals)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceReference(type, oid, refPath, refVals);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected void modifyUserAdd(String userOid, ItemPath propertyPath, Task task, OperationResult result, Object... newRealValue)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<UserType> objectDelta = createModifyUserAddDelta(userOid, propertyPath, newRealValue);
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

    protected ObjectDelta<UserType> createOldNewPasswordDelta(
            String oid, String oldPassword, String newPassword) throws SchemaException {
        ProtectedStringType oldPasswordPs = new ProtectedStringType();
        oldPasswordPs.setClearValue(oldPassword);

        ProtectedStringType newPasswordPs = new ProtectedStringType();
        newPasswordPs.setClearValue(newPassword);

        return deltaFor(UserType.class)
                .item(PASSWORD_VALUE_PATH)
                .oldRealValue(oldPasswordPs)
                .replace(newPasswordPs)
                .asObjectDelta(oid);
    }

    protected void modifyUserChangePassword(String userOid, String newPassword) throws CommonException {
        Task task = createPlainTask("modifyUserChangePassword");
        OperationResult result = task.getResult();
        modifyUserChangePassword(userOid, newPassword, task, result);
        assertSuccess(result);
    }

    protected void modifyUserChangePassword(String userOid, String newPassword, Task task, OperationResult result) throws CommonException {
        modifyFocusChangePassword(UserType.class, userOid, newPassword, task, result);
    }

    protected void modifyFocusChangePassword(Class<? extends ObjectType> type, String oid, String newPassword, Task task, OperationResult result) throws CommonException {
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newPassword);
        modifyObjectReplaceProperty(type, oid, PASSWORD_VALUE_PATH, task, result, passwordPs);
    }

    protected void modifyUserSetPassword(String userOid, String newPassword, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        PasswordType passwordType = new PasswordType();
        passwordType.setValue(userPasswordPs);
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(SchemaConstants.PATH_PASSWORD).add(passwordType)
                .asObjectDelta(userOid);
        executeChanges(delta, null, task, result);
    }

    protected void modifyAccountChangePassword(String accountOid, String newPassword, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        modifyAccountShadowReplace(accountOid, PASSWORD_VALUE_PATH, task, result, userPasswordPs);
    }

    protected void clearUserPassword(String userOid)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        Task task = createPlainTask("clearUserPassword");
        OperationResult result = task.getResult();
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(UserType.class)
                .item(SchemaConstants.PATH_PASSWORD).replace()
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, itemDeltas, result);
        assertSuccess(result);
    }

    protected <O extends ObjectType> void renameObject(Class<O> type, String oid, String newName, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        modifyObjectReplaceProperty(type, oid, ObjectType.F_NAME, task, result, PolyString.fromOrig(newName));
    }

    protected void recomputeUser(String userOid) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = createPlainTask("recomputeUser");
        OperationResult result = task.getResult();
        modelService.recompute(UserType.class, userOid, null, task, result);
        assertSuccess(result);
    }

    protected void recomputeUser(String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        modelService.recompute(UserType.class, userOid, null, task, result);
    }

    protected void recomputeFocus(Class<? extends FocusType> clazz, String userOid, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        modelService.recompute(clazz, userOid, null, task, result);
    }

    protected void recomputeUser(String userOid, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
        modelService.recompute(UserType.class, userOid, options, task, result);
    }

    protected void assignRole(String userOid, String roleOid)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("assignRole");
        OperationResult result = task.getResult();
        assignRole(userOid, roleOid, task, result);
        assertSuccess(result);
    }

    protected void assignRole(String userOid, String roleOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        assignRole(UserType.class, userOid, roleOid, null, task, result);
    }

    protected void assignService(String userOid, String targetOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        assignService(UserType.class, userOid, targetOid, null, task, result);
    }

    protected void assignRole(String userOid, String roleOid, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        assignRole(UserType.class, userOid, roleOid, null, task, options, result);
    }

    protected void assignRole(Class<? extends AssignmentHolderType> focusClass, String focusOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        assignRole(focusClass, focusOid, roleOid, null, task, result);
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

    protected void unassignRole(Class<? extends AssignmentHolderType> focusClass, String focusOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        unassignRole(focusClass, focusOid, roleOid, null, task, result);
    }

    protected void unassignRole(Class<? extends AssignmentHolderType> focusClass, String focusOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, false, result);
    }

    protected void assignRole(Class<? extends AssignmentHolderType> focusClass, String focusOid, String roleOid, ActivationType activationType, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, result);
    }

    protected void assignService(Class<? extends AssignmentHolderType> focusClass, String focusOid,
            String targetOid, ActivationType activationType, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, targetOid, ServiceType.COMPLEX_TYPE, null, task, null, activationType, true, result);
    }

    protected void assignRole(Class<? extends FocusType> focusClass, String focusOid, String roleOid, ActivationType activationType, Task task, ModelExecuteOptions options, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, roleOid, RoleType.COMPLEX_TYPE, null, task, null, activationType, true, options, result);
    }

    protected void assignFocus(Class<? extends FocusType> focusClass,
            String focusOid, QName targetType, String targetOid, QName relation,
            ActivationType activationType, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, targetOid, targetType, relation, task, null, activationType, true, result);
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
        Task task = createPlainTask("assignRole");
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
        Task task = createPlainTask("unassignRole");
        OperationResult result = task.getResult();
        unassignRole(userOid, roleOid, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void unassignRoleByAssignmentValue(PrismObject<? extends FocusType> focus, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        AssignmentType assignment = findAssignmentByTargetRequired(focus, roleOid);
        ObjectDelta<? extends FocusType> delta = prismContext.deltaFor(focus.getCompileTimeClass())
                .item(FocusType.F_ASSIGNMENT)
                .delete(assignment.clone())
                .asObjectDelta(focus.getOid());
        modelService.executeChanges(singleton(delta), null, task, result);
    }

    protected void unassignRole(String userOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, false, result);
    }

    protected void unassignService(String userOid, String serviceOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, serviceOid, ServiceType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, false, result);
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
        modifyAssignmentHolderAssignment(UserType.class, userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, null, true, options, result);
    }

    protected void unassignRole(String userOid, String roleOid, PrismContainer<?> extension, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, false, result);
    }

    protected void unassignRole(String userOid, String roleOid, PrismContainer<?> extension, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(UserType.class, userOid, roleOid, RoleType.COMPLEX_TYPE, null, task, extension, null, false, options, result);
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
        Task task = createPlainTask("unassignAllRoles");
        OperationResult result = task.getResult();
        PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, result);
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (AssignmentType assignment : user.asObjectable().getAssignment()) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                    ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container()
                            .createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
                    PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
                    cval.setId(assignment.getId());
                    assignmentDelta.addValueToDelete(cval);
                    modifications.add(assignmentDelta);
                }
            }
        }
        if (modifications.isEmpty()) {
            return;
        }
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(userOid, modifications, UserType.class);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, useRawPlusRecompute ? executeOptions().raw() : null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        if (useRawPlusRecompute) {
            recomputeUser(userOid, task, result);
            result.computeStatus();
            TestUtil.assertSuccess(result);
        }
    }

    protected void assignArchetype(String userOid, String archetypeOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, archetypeOid, ArchetypeType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, true, result);
    }

    protected void unassignArchetype(String userOid, String archetypeOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, archetypeOid, ArchetypeType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, false, result);
    }

    protected void assignPolicy(String userOid, String policyOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, policyOid, PolicyType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, true, result);
    }

    protected void unassignPolicy(String userOid, String policyOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, policyOid, PolicyType.COMPLEX_TYPE, null, task, (Consumer<AssignmentType>) null, false, result);
    }

    protected void induceRole(String focusRoleOid, String targetRoleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        induceRole(RoleType.class, focusRoleOid, targetRoleOid, task, result);
    }

    protected <F extends FocusType> void induceRole(Class<F> focusType, String focusOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusType, focusOid, AbstractRoleType.F_INDUCEMENT, roleOid, RoleType.COMPLEX_TYPE, null, task, null, true, null, result);
    }

    protected <F extends FocusType> void induceOrg(Class<F> focusType, String focusOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusType, focusOid, AbstractRoleType.F_INDUCEMENT, orgOid, OrgType.COMPLEX_TYPE, null, task, null, true, null, result);
    }

    protected <F extends FocusType> void uninduceRole(Class<F> focusType, String focusOid, String roleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusType, focusOid, AbstractRoleType.F_INDUCEMENT, roleOid, RoleType.COMPLEX_TYPE, null, task, null, false, null, result);
    }

    protected void uninduceRole(String focusRoleOid, String targetRoleOid, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        uninduceRole(RoleType.class, focusRoleOid, targetRoleOid, task, result);
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
        assignOrg(userOid, orgOid, prismContext.getDefaultRelation());
    }

    protected void assignOrg(String userOid, String orgOid, QName relation)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("assignOrg");
        OperationResult result = task.getResult();
        assignOrg(userOid, orgOid, relation, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assignOrg(String userOid, String orgOid, QName relation, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, (Consumer<AssignmentType>) null, true, result);
    }

    protected <F extends FocusType> void assignOrg(Class<F> focusType, String focusOid, String orgOid, QName relation, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusType, focusOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, null, true, result);
    }

    protected void unassignOrg(String userOid, String orgOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        unassignOrg(userOid, orgOid, prismContext.getDefaultRelation());
    }

    protected void unassignOrg(String userOid, String orgOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        unassignOrg(userOid, orgOid, null, task, result);
    }

    protected void unassignOrg(String userOid, String orgOid, QName relation) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("unassignOrg");
        OperationResult result = task.getResult();
        unassignOrg(userOid, orgOid, relation, task, result);
        assertSuccess(result);
    }

    protected void unassignOrg(String userOid, String orgOid, QName relation, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyUserAssignment(userOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, (Consumer<AssignmentType>) null, false, result);
    }

    protected <F extends FocusType> void unassignOrg(Class<F> type, String focusOid, String orgOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        unassignOrg(type, focusOid, orgOid, null, task, result);
    }

    protected <F extends FocusType> void unassignOrg(Class<F> type, String focusOid, String orgOid, QName relation, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(type, focusOid, orgOid, OrgType.COMPLEX_TYPE, relation, task, null, false, result);
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

    protected <F extends AssignmentHolderType> void modifyAssignmentHolderAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
            PrismContainer<?> extension, ActivationType activationType, boolean add, OperationResult result)
            throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        modifyAssignmentHolderAssignment(focusClass, focusOid, roleOid, refType, relation, task, extension, activationType, add, null, result);
    }

    protected <F extends AssignmentHolderType> void modifyAssignmentHolderAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
            PrismContainer<?> extension, ActivationType activationType, boolean add, ModelExecuteOptions options, OperationResult result)
            throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> delta = createAssignmentAssignmentHolderDelta(focusClass, focusOid, roleOid, refType, relation, extension, activationType, add);
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

    protected <F extends AssignmentHolderType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
            Consumer<AssignmentType> modificationBlock, boolean add, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusClass, focusOid, roleOid, refType, relation, task, modificationBlock, add, null, result);
    }

    protected <F extends AssignmentHolderType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation, Task task,
            Consumer<AssignmentType> modificationBlock, boolean add, ModelExecuteOptions options, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        modifyFocusAssignment(focusClass, focusOid, FocusType.F_ASSIGNMENT, roleOid, refType, relation, task, modificationBlock, add, options, result);
    }

    protected <F extends AssignmentHolderType> void modifyFocusAssignment(Class<F> focusClass, String focusOid, QName elementName, String roleOid, QName refType, QName relation, Task task,
            Consumer<AssignmentType> modificationBlock, boolean add, ModelExecuteOptions options, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> focusDelta = createAssignmentFocusDelta(focusClass, focusOid, elementName, roleOid, refType, relation, modificationBlock, add);
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

    protected <F extends AssignmentHolderType> void unassign(Class<F> focusClass, String focusOid, String roleId, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> focusDelta = createAssignmentAssignmentHolderDelta(focusClass, focusOid, roleId, null, null, null, null, false);
        executeChanges(focusDelta, null, task, result);
    }

    protected <F extends AssignmentHolderType> void unassign(Class<F> focusClass, String focusOid, long assignmentId, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        unassign(focusClass, focusOid, assignmentId, null, task, result);
    }

    protected <F extends AssignmentHolderType> void unassign(Class<F> focusClass, String focusOid, long assignmentId, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        unassign(focusClass, focusOid, createAssignmentIdOnly(assignmentId), options, task, result);
    }

    protected <F extends AssignmentHolderType> void unassign(Class<F> focusClass, String focusOid, AssignmentType currentAssignment, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container().createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
        assignmentDelta.addValuesToDelete(currentAssignment.asPrismContainerValue().clone());
        modifications.add(assignmentDelta);
        ObjectDelta<F> focusDelta = prismContext.deltaFactory().object().createModifyDelta(focusOid, modifications, focusClass
        );
        executeChanges(focusDelta, options, task, result);
    }

    protected <F extends FocusType> void unlink(Class<F> focusClass, String focusOid, String targetOid, Task task, OperationResult result)
            throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> delta = prismContext.deltaFactory().object()
                .createModificationDeleteReference(focusClass, focusOid, FocusType.F_LINK_REF,
                        targetOid);
        executeChanges(delta, null, task, result);
    }

    protected void unlinkUser(String userOid, String targetOid)
            throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("unlinkUser");
        OperationResult result = task.getResult();
        unlink(UserType.class, userOid, targetOid, task, result);
        assertSuccess(result);
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
    protected <F extends FocusType> ObjectDelta<F> createUnassignAllDelta(PrismObject<F> focusBefore) {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (AssignmentType assignmentType : focusBefore.asObjectable().getAssignment()) {
            modifications.add((createAssignmentModification(assignmentType.getId(), false)));
        }
        return prismContext.deltaFactory().object()
                .createModifyDelta(focusBefore.getOid(), modifications, focusBefore.getCompileTimeClass());
    }

    /**
     * Executes assignment replace delta with empty values.
     */
    protected void unassignAllReplace(String userOid, Task task, OperationResult result)
            throws ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationReplaceContainer(
                        UserType.class, userOid, UserType.F_ASSIGNMENT, new PrismContainerValue[0]);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected ContainerDelta<AssignmentType> createAssignmentModification(String roleOid, QName refType, QName relation,
            PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
        return createAssignmentModification(UserType.class, FocusType.F_ASSIGNMENT, roleOid, refType, relation, extension, activationType, add);
    }

    protected <F extends FocusType> ContainerDelta<AssignmentType> createAssignmentModification(Class<F> type, QName elementName, String roleOid, QName refType, QName relation,
            PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
        try {
            return createAssignmentModification(type, elementName, roleOid, refType, relation,
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
            throw (SchemaException) te.getCause();
        }
    }

    protected <F extends AssignmentHolderType> ContainerDelta<AssignmentType> createAssignmentModification(Class<F> type, QName elementName, String roleOid, QName refType, QName relation,
            Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container().createDelta(ItemName.fromQName(elementName), getObjectDefinition(type));
        PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
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

    protected ContainerDelta<AssignmentType> createAssignmentModification(long id, boolean add) {
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container().createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
        PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
        cval.setId(id);
        if (add) {
            assignmentDelta.addValueToAdd(cval);
        } else {
            assignmentDelta.addValueToDelete(cval);
        }
        return assignmentDelta;
    }

    protected <F extends FocusType> ContainerDelta<AssignmentType> createAssignmentEmptyDeleteModification(
            PrismObject<F> existingFocus, String roleOid, QName relation) {
        AssignmentType existingAssignment = findAssignment(existingFocus, roleOid, relation);
        return createAssignmentModification(existingAssignment.getId(), false);
    }

    protected <F extends FocusType> AssignmentType findAssignment(
            PrismObject<F> existingFocus, String targetOid, QName relation) {
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

    protected ObjectDelta<UserType> createAssignmentUserDelta(
            String userOid, String roleOid, QName refType, QName relation,
            PrismContainer<?> extension, ActivationType activationType, boolean add)
            throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(userOid, modifications, UserType.class);
        return userDelta;
    }

    protected <F extends AssignmentHolderType> ObjectDelta<F> createAssignmentAssignmentHolderDelta(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation,
            PrismContainer<?> extension, ActivationType activationType, boolean add) throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(roleOid, refType, relation, extension, activationType, add)));
        return prismContext.deltaFactory().object().createModifyDelta(focusOid, modifications, focusClass);
    }

    protected ObjectDelta<UserType> createAssignmentUserDelta(String userOid, String roleOid, QName refType, QName relation,
            Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
        return createAssignmentFocusDelta(UserType.class, userOid, roleOid, refType, relation, modificationBlock, add);
    }

    protected <F extends FocusType> ObjectDelta<F> createAssignmentFocusDelta(Class<F> focusClass, String focusOid, String roleOid, QName refType, QName relation,
            Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
        return createAssignmentFocusDelta(focusClass, focusOid, FocusType.F_ASSIGNMENT, roleOid, refType, relation, modificationBlock, add);
    }

    protected <F extends AssignmentHolderType> ObjectDelta<F> createAssignmentFocusDelta(Class<F> focusClass, String focusOid, QName elementName, String roleOid, QName refType, QName relation,
            Consumer<AssignmentType> modificationBlock, boolean add) throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentModification(focusClass, elementName, roleOid, refType, relation, modificationBlock, add)));
        return prismContext.deltaFactory().object().createModifyDelta(focusOid, modifications, focusClass);
    }

    protected <F extends FocusType> ObjectDelta<F> createAssignmentFocusEmptyDeleteDelta(
            PrismObject<F> existingFocus, String roleOid, QName relation) {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add((createAssignmentEmptyDeleteModification(existingFocus, roleOid, relation)));
        return prismContext.deltaFactory().object().createModifyDelta(
                existingFocus.getOid(), modifications, existingFocus.getCompileTimeClass());
    }

    protected ContainerDelta<AssignmentType> createAccountAssignmentModification(
            String resourceOid, String intent, boolean add) throws SchemaException {
        return createAssignmentModification(resourceOid, ShadowKindType.ACCOUNT, intent, add);
    }

    protected <V> PropertyDelta<V> createUserPropertyReplaceModification(QName propertyName, V... values) {
        return prismContext.deltaFactory().property().createReplaceDelta(getUserDefinition(), propertyName, values);
    }

    protected ContainerDelta<AssignmentType> createAssignmentModification(String resourceOid, ShadowKindType kind,
            String intent, boolean add) throws SchemaException {
        AssignmentType assignmentType = createConstructionAssignment(resourceOid, kind, intent);
        return createAssignmentModification(assignmentType, add);
    }

    protected ContainerDelta<AssignmentType> createAssignmentModification(AssignmentType assignmentType, boolean add) throws SchemaException {
        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container().createDelta(UserType.F_ASSIGNMENT, getUserDefinition());

        if (add) {
            assignmentDelta.addValueToAdd(assignmentType.asPrismContainerValue());
        } else {
            assignmentDelta.addValueToDelete(assignmentType.asPrismContainerValue());
        }

        PrismContainerDefinition<AssignmentType> assignmentDef = getUserDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        if (assignmentDef != null) {
            assignmentDelta.applyDefinition(assignmentDef);
        }

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
        return FocusTypeUtil.createTargetAssignment(targetOid, targetType);
    }

    protected ObjectDelta<UserType> createParametricAssignmentDelta(
            String userOid, String roleOid, String orgOid, String tenantOid, boolean adding)
            throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();

        ContainerDelta<AssignmentType> assignmentDelta = prismContext.deltaFactory().container().createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
        PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
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
            orgRef.getValue().setTargetType(OrgType.COMPLEX_TYPE);
            orgRef.getValue().setRelation(ORG_DEFAULT);
        }

        if (tenantOid != null) {
            PrismReference tenantRef = cval.findOrCreateReference(AssignmentType.F_TENANT_REF);
            tenantRef.getValue().setOid(tenantOid);
            tenantRef.getValue().setTargetType(OrgType.COMPLEX_TYPE);
            tenantRef.getValue().setRelation(ORG_DEFAULT);
        }

        modifications.add(assignmentDelta);
        return prismContext.deltaFactory().object().createModifyDelta(userOid, modifications, UserType.class
        );
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
        assertAssignees(targetOid, prismContext.getDefaultRelation(), expectedAssignees);
    }

    protected void assertAssignees(String targetOid, QName relation, int expectedAssignees) throws SchemaException {
        OperationResult result = new OperationResult(contextName() + ".assertAssignees");
        int count = countAssignees(targetOid, relation, result);
        if (count != expectedAssignees) {
            SearchResultList<PrismObject<FocusType>> assignees = listAssignees(targetOid, result);
            AssertJUnit.fail("Unexpected number of assignees of " + targetOid + " as '" + relation + "', expected " + expectedAssignees + ", but was " + count + ": " + assignees);
        }

    }

    protected int countAssignees(String targetOid, QName relation, OperationResult result) throws SchemaException {
        PrismReferenceValue refVal = itemFactory().createReferenceValue();
        refVal.setOid(targetOid);
        refVal.setRelation(relation);
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(refVal)
                .build();
        return repositoryService.countObjects(FocusType.class, query, null, result);
    }

    protected SearchResultList<PrismObject<FocusType>> listAssignees(String targetOid, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(targetOid)
                .build();
        return repositoryService.searchObjects(FocusType.class, query, null, result);
    }

    protected ConstructionType createAccountConstruction(String resourceOid, String intent) {
        ConstructionType accountConstructionType = new ConstructionType();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        accountConstructionType.setResourceRef(resourceRef);
        accountConstructionType.setIntent(intent);
        return accountConstructionType;
    }

    protected ObjectDelta<UserType> createReplaceAccountConstructionUserDelta(
            String userOid, Long id, ConstructionType newValue) {
        PrismContainerDefinition<ConstructionType> pcd =
                getAssignmentDefinition().findContainerDefinition(AssignmentType.F_CONSTRUCTION);
        ContainerDelta<ConstructionType> acDelta = prismContext.deltaFactory().container().create(
                ItemPath.create(UserType.F_ASSIGNMENT, id, AssignmentType.F_CONSTRUCTION), pcd);
        acDelta.setValueToReplace(newValue.asPrismContainerValue());

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(acDelta);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(userOid, modifications, UserType.class);
        return userDelta;
    }

    protected ObjectDelta<UserType> createAccountAssignmentUserDelta(String focusOid, String resourceOid, String intent, boolean add) throws SchemaException {
        return createAssignmentDelta(UserType.class, focusOid, resourceOid, ShadowKindType.ACCOUNT, intent, add);
    }

    protected <F extends FocusType> ObjectDelta<F> createAssignmentDelta(Class<F> type, String focusOid,
            String resourceOid, ShadowKindType kind, String intent, boolean add) throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(resourceOid, kind, intent, add));
        return prismContext.deltaFactory().object()
                .createModifyDelta(focusOid, modifications, type);
    }

    protected <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        displayDumpable("Executing delta", objectDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        return modelService.executeChanges(deltas, options, task, result);
    }

    protected Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        display("Executing deltas", deltas);
        return modelService.executeChanges(deltas, options, task, result);
    }

    protected <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> executeChangesAssertSuccess(
            ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ObjectDeltaOperation<? extends ObjectType>> rv = executeChanges(objectDelta, options, task, result);
        assertSuccess(result);
        return rv;
    }

    protected <O extends ObjectType> ModelContext<O> previewChanges(ObjectDelta<O> objectDelta, ModelExecuteOptions options, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        displayDumpable("Preview changes for delta", objectDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        return modelInteractionService.previewChanges(deltas, options, task, result);
    }

    protected void assignAccountToUser(
            String focusOid, String resourceOid, String intent)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        assignAccount(UserType.class, focusOid, resourceOid, intent);
    }

    protected <F extends FocusType> void assignAccount(Class<F> type, String focusOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("assignAccount");
        OperationResult result = task.getResult();
        assignAccount(type, focusOid, resourceOid, intent, task, result);
        assertSuccess(result);
    }

    protected void assignAccountToUser(
            String focusOid, String resourceOid, String intent, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        assignAccount(UserType.class, focusOid, resourceOid, intent, task, result);
    }

    protected <F extends FocusType> void assignAccount(Class<F> type, String focusOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> userDelta = createAssignmentDelta(type, focusOid, resourceOid, ShadowKindType.ACCOUNT, intent, true);
        executeChanges(userDelta, null, task, result);
    }

    protected void unassignAccountFromUser(String focusOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        unassignAccount(UserType.class, focusOid, resourceOid, intent);
    }

    protected <F extends FocusType> void unassignAccount(Class<F> type, String focusOid, String resourceOid, String intent) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("unassignAccount");
        OperationResult result = task.getResult();
        unassignAccount(type, focusOid, resourceOid, intent, task, result);
        assertSuccess(result);
    }

    protected void unassignAccountFromUser(String focusOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        unassignAccount(UserType.class, focusOid, resourceOid, intent, task, result);
    }

    protected <F extends FocusType> void unassignAccount(Class<F> type, String focusOid, String resourceOid, String intent, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<F> userDelta = createAssignmentDelta(type, focusOid, resourceOid, ShadowKindType.ACCOUNT, intent, false);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);
    }

    protected <F extends FocusType> void assignPolicyRule(Class<F> type, String focusOid, PolicyRuleType policyRule, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        AssignmentType assignmentType = new AssignmentType();
        assignmentType.setPolicyRule(policyRule);
        assign(type, focusOid, assignmentType, task, result);
    }

    protected void assign(TestObject<?> assignee, TestObject<?> assigned, QName relation, ModelExecuteOptions options,
            Task task, OperationResult result) throws SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta<UserType> delta = deltaFor(assignee.getType())
                .item(UserType.F_ASSIGNMENT)
                .add(ObjectTypeUtil.createAssignmentTo(assigned.get(), relation))
                .asObjectDelta(assignee.oid);
        executeChanges(delta, options, task, result);
    }

    protected void unassignIfSingle(TestObject<?> assignee, TestObject<?> assigned, QName relation, ModelExecuteOptions options,
            Task task, OperationResult result) throws SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        List<AssignmentType> assignments = ((AssignmentHolderType) assignee.getObjectable()).getAssignment().stream()
                .filter(a -> a.getTargetRef() != null && assigned.oid.equals(a.getTargetRef().getOid())
                        && QNameUtil.match(a.getTargetRef().getRelation(), relation))
                .collect(Collectors.toList());
        assertThat(assignments).size().as("# of assignments of " + assigned).isEqualTo(1);
        AssignmentType assignment = MiscUtil.extractSingleton(assignments);
        ObjectDelta<UserType> delta = deltaFor(assignee.getType())
                .item(UserType.F_ASSIGNMENT)
                .delete(assignment.clone())
                .asObjectDelta(assignee.oid);
        executeChanges(delta, options, task, result);
    }

    protected <F extends FocusType> void assign(Class<F> type, String focusOid,
            AssignmentType assignmentType, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(assignmentType, true));
        ObjectDelta<F> userDelta = prismContext.deltaFactory().object().createModifyDelta(focusOid, modifications, type
        );
        executeChanges(userDelta, null, task, result);
    }

    protected PrismObject<UserType> getUserFull(String userOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getUser(userOid, createRetrieveCollection());
    }

    protected PrismObject<UserType> getUser(String userOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getUser(userOid, null);
    }

    protected PrismObject<UserType> getUser(String userOid, Collection<SelectorOptions<GetOperationOptions>> options)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("getUser");
        OperationResult result = task.getResult();
        PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess("getObject(User) result not success", result);
        return user;
    }

    protected PrismObject<UserType> getUserFromRepo(String userOid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, userOid, null, new OperationResult("dummy"));
    }

    protected <O extends ObjectType> PrismObject<O> findObjectByNameFullRequired(Class<O> type, String name)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return findObjectByNameRequired(type, name, createRetrieveCollection());
    }

    protected <O extends ObjectType> PrismObject<O> findObjectByName(Class<O> type, String name)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return findObjectByName(type, name, null);
    }

    protected <O extends ObjectType> PrismObject<O> findObjectByNameRequired(
            Class<O> type, String name, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return MiscUtil.requireNonNull(
                findObjectByName(type, name, options),
                () -> new AssertionError("Object of type " + type.getSimpleName() + " named '" + name + "' does not exist"));
    }

    protected <O extends ObjectType> PrismObject<O> findObjectByName(
            Class<O> type, String name, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("findObjectByName");
        OperationResult result = task.getResult();
        List<PrismObject<O>> objects = modelService.searchObjects(type, createNameQuery(name), options, task, result);
        if (objects.isEmpty()) {
            return null;
        }
        assert objects.size() == 1 : "Too many objects found for name " + name + ": " + objects;
        return objects.iterator().next();
    }

    protected ObjectQuery createNameQuery(String name) throws SchemaException {
        return ObjectQueryUtil.createNameQuery(PolyString.fromOrig(name));
    }

    protected PrismObject<UserType> findUserByUsernameFullRequired(String username)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return findObjectByNameFullRequired(UserType.class, username);
    }

    protected PrismObject<UserType> findUserByUsername(String username) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return findObjectByName(UserType.class, username);
    }

    protected PrismObject<ServiceType> findServiceByName(String name) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return findObjectByName(ServiceType.class, name);
    }

    protected RoleType getRoleSimple(String oid) {
        try {
            return getRole(oid).asObjectable();
        } catch (CommonException e) {
            throw new SystemException("Unexpected exception while getting role " + oid + ": " + e.getMessage(), e);
        }
    }

    protected PrismObject<RoleType> getRole(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("getRole");
        OperationResult result = task.getResult();
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, oid, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess("getObject(Role) result not success", result);
        return role;
    }

    protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("findAccountByUsername");
        OperationResult result = task.getResult();
        return findAccountByUsername(username, resource, task, result);
    }

    /** Looks for default `ACCOUNT` object class. */
    protected PrismObject<ShadowType> findAccountByUsername(String username, PrismObject<ResourceType> resource,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = defaultAccountPrimaryIdentifierQuery(username, resource);
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);
        if (accounts.isEmpty()) {
            return null;
        }
        assert accounts.size() == 1 : "Too many accounts found for username " + username + " on " + resource + ": " + accounts;
        return accounts.iterator().next();
    }

    protected Collection<PrismObject<ShadowType>> listAccounts(
            PrismObject<ResourceType> resource, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectDefinition accountDef = schema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        assertThat(accountDef.getPrimaryIdentifiers())
                .as("primary identifiers")
                .hasSize(1);
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_OBJECT_CLASS).eq(accountDef.getObjectClassName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .build();
        return modelService.searchObjects(ShadowType.class, query, null, task, result);
    }

    protected PrismObject<ShadowType> getShadowModel(String accountOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getShadowModel(accountOid, null, true);
    }

    // TODO better name
    protected @NotNull AbstractShadow getAbstractShadowModel(String accountOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return AbstractShadow.of(getShadowModel(accountOid, null, true));
    }

    protected PrismObject<ShadowType> getShadowModelNoFetch(String accountOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getShadowModel(accountOid, GetOperationOptions.createNoFetch(), true);
    }

    protected PrismObject<ShadowType> getShadowModelFuture(String accountOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getShadowModel(accountOid, GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE), true);
    }

    protected PrismObject<ShadowType> getShadowModel(
            String shadowOid, GetOperationOptions rootOptions, boolean assertSuccess)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("getShadowModel");
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> opts = null;
        if (rootOptions != null) {
            opts = SelectorOptions.createCollection(rootOptions);
        }
        logger.info("Getting model shadow {} with options {}", shadowOid, opts);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, shadowOid, opts, task, result);
        logger.info("Got model shadow (options {})\n{}", shadowOid, shadow.debugDumpLazily(1));
        result.computeStatus();
        if (assertSuccess) {
            TestUtil.assertSuccess("getObject(shadow) result not success", result);
        }
        return shadow;
    }

    protected <O extends ObjectType> void assertNoObject(Class<O> type, String oid)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("assertNoObject");
        assertNoObject(type, oid, task, task.getResult());
    }

    protected <O extends ObjectType> void assertNoObject(
            Class<O> type, String oid, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        try {
            PrismObject<O> object = modelService.getObject(type, oid, null, task, result);
            display("Unexpected object", object);
            AssertJUnit.fail("Expected that " + object + " does not exist, but it does");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }
    }

    protected <O extends ObjectType> SearchResultList<PrismObject<O>> assertObjectByName(
            Class<O> type, String name, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        SearchResultList<PrismObject<O>> objects = modelService
                .searchObjects(type, prismContext.queryFor(type).item(ObjectType.F_NAME).eqPoly(name).build(), null, task,
                        result);
        if (objects.isEmpty()) {
            fail("Expected that " + type + " " + name + " did exist but it did not");
        }
        return objects;
    }

    @SuppressWarnings("unused")
    protected <O extends ObjectType> PrismObject<O> assertSingleObjectByName(
            Class<O> type, String name, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        SearchResultList<PrismObject<O>> objects = assertObjectByName(type, name, task, result);
        assertThat(objects.size()).as("# of objects found").isEqualTo(1);
        return objects.get(0);
    }

    protected <O extends ObjectType> void assertNoObjectByName(
            Class<O> type, String name, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        SearchResultList<PrismObject<O>> objects = modelService
                .searchObjects(type, prismContext.queryFor(type).item(ObjectType.F_NAME).eqPoly(name).build(), null, task,
                        result);
        if (!objects.isEmpty()) {
            fail("Expected that " + type + " " + name + " did not exists but it did: " + objects);
        }
    }

    /** Looks for default `ACCOUNT` object class. */
    protected void assertNoShadow(
            String username, PrismObject<ResourceType> resource, OperationResult result)
            throws SchemaException, ConfigurationException {
        ObjectQuery query = defaultAccountPrimaryIdentifierQuery(username, resource);
        List<PrismObject<ShadowType>> accounts =
                repositoryService.searchObjects(ShadowType.class, query, null, result);
        if (accounts.isEmpty()) {
            return;
        }
        logger.error("Found shadow for " + username + " on " + resource + " while not expecting it:\n" + accounts.get(0).debugDump());
        assert false : "Found shadow for " + username + " on " + resource + " while not expecting it: " + accounts;
    }

    /** Looks for default `ACCOUNT` object class. */
    protected ShadowAsserter<Void> assertShadow(String username, PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        ObjectQuery query = defaultAccountPrimaryIdentifierQuery(username, resource);
        OperationResult result = new OperationResult("assertShadow");
        List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, null, result);
        if (accounts.isEmpty()) {
            AssertJUnit.fail("No shadow for " + username + " on " + resource);
        } else if (accounts.size() > 1) {
            AssertJUnit.fail("Too many shadows for " + username + " on " + resource + " (" + accounts.size() + "): " + accounts);
        }
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(accounts.get(0), "shadow for username " + username + " on " + resource);
        initializeAsserter(asserter);
        return asserter;
    }

    protected PrismObject<ShadowType> findShadowByNameViaModel(
            ShadowKindType kind, String intent, String name, PrismObject<ResourceType> resource,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceSchema rSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectDefinition rOcDef = intent != null ?
                rSchema.findObjectDefinitionRequired(kind, intent) :
                rSchema.findDefaultDefinitionForKindRequired(kind);
        ObjectQuery query = createShadowQuerySecondaryIdentifier(rOcDef, name, resource, false, false);
        List<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, options, task, result);
        if (shadows.isEmpty()) {
            return null;
        }
        assert shadows.size() == 1 : "Too many shadows found for name " + name + " on " + resource + ": " + shadows;
        return shadows.iterator().next();
    }

    protected <F extends FocusType> String getSingleLinkOid(PrismObject<F> focus) {
        PrismReferenceValue accountRefValue = getSingleLinkRef(focus);
        assertNull("Unexpected object in linkRefValue", accountRefValue.getObject());
        return accountRefValue.getOid();
    }

    protected <F extends FocusType> String getSingleLiveLinkOid(PrismObject<F> focus) {
        PrismReferenceValue accountRefValue = getSingleLiveLinkRef(focus);
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

    protected <F extends FocusType> PrismReferenceValue getSingleLiveLinkRef(PrismObject<F> focus) {
        F focusType = focus.asObjectable();
        List<ObjectReferenceType> liveLinkRefs = FocusTypeUtil.getLiveLinkRefs(focusType);
        assertEquals("Unexpected number of live linkRefs", 1, liveLinkRefs.size());
        ObjectReferenceType linkRefType = liveLinkRefs.get(0);
        String accountOid = linkRefType.getOid();
        assertFalse("No linkRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = linkRefType.asReferenceValue();
        assertEquals("OID mismatch in linkRefValue", accountOid, accountRefValue.getOid());
        return accountRefValue;
    }

    protected String getLiveLinkRefOid(String userOid, String resourceOid) throws ObjectNotFoundException, SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getLiveLinkRefOid(getUser(userOid), resourceOid);
    }

    protected <F extends FocusType> String getLiveLinkRefOid(PrismObject<F> focus, String resourceOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        PrismReferenceValue linkRef = getLiveLinkRef(focus, resourceOid);
        if (linkRef == null) {
            return null;
        }
        return linkRef.getOid();
    }

    protected <F extends FocusType> PrismReferenceValue getLiveLinkRef(PrismObject<F> focus, String resourceOid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        F focusType = focus.asObjectable();
        for (ObjectReferenceType linkRefType : focusType.getLinkRef()) {
            if (!relationRegistry.isMember(linkRefType.getRelation())) {
                continue;
            }
            String linkTargetOid = linkRefType.getOid();
            assertFalse("No linkRef oid", StringUtils.isBlank(linkTargetOid));
            PrismObject<ShadowType> account = getShadowModel(linkTargetOid, GetOperationOptions.createNoFetch(), false);
            if (resourceOid.equals(account.asObjectable().getResourceRef().getOid())) {
                // This is noFetch. Therefore there is no fetchResult
                return linkRefType.asReferenceValue();
            }
        }
        AssertJUnit.fail("Account for resource " + resourceOid + " not found in " + focus);
        return null; // Never reached. But compiler complains about missing return
    }

    protected <F extends FocusType> String getLinkRefOid(PrismObject<F> focus, String resourceOid, ShadowKindType kind, String intent) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        F focusType = focus.asObjectable();
        for (ObjectReferenceType linkRefType : focusType.getLinkRef()) {
            String linkTargetOid = linkRefType.getOid();
            assertFalse("No linkRef oid", StringUtils.isBlank(linkTargetOid));
            PrismObject<ShadowType> account = getShadowModel(linkTargetOid, GetOperationOptions.createNoFetch(), false);
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
        AssertJUnit.fail("Linked shadow for resource " + resourceOid + ", kind " + kind + " and intent " + intent + " not found in " + focus);
        return null; // Never reached. But compiler complains about missing return
    }

    protected void assertUserNoAccountRefs(PrismObject<UserType> user) {
        UserType userJackType = user.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());
    }

    protected String assertAccount(PrismObject<UserType> user, String resourceOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String accountOid = getLiveLinkRefOid(user, resourceOid);
        assertNotNull("User " + user + " has no account on resource " + resourceOid, accountOid);
        return accountOid;
    }

    protected void assertAccounts(String userOid, int numAccounts) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("assertAccounts");
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertLiveLinks(user, numAccounts);
    }

    protected void assertNoShadows(Collection<String> shadowOids) throws SchemaException {
        for (String shadowOid : shadowOids) {
            assertNoShadow(shadowOid);
        }
    }

    protected void assertNoShadow(String shadowOid) throws SchemaException {
        OperationResult result = new OperationResult(contextName() + ".assertNoShadow");
        // Check is shadow is gone
        try {
            PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
            display("Unexpected shadow", shadow);
            AssertJUnit.fail("Shadow " + shadowOid + " still exists");
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
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && roleOid.equals(targetRef.getOid()) && prismContext.relationMatches(relation,
                    targetRef.getRelation())) {
                return assignment;
            }
        }
        return null;
    }

    protected <F extends FocusType> AssignmentType getAssignment(PrismObject<F> focus, String roleOid) {
        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && roleOid.equals(targetRef.getOid())) {
                return assignment;
            }
        }
        return null;
    }

    protected ItemPath getAssignmentPath(long id) {
        return ItemPath.create(FocusType.F_ASSIGNMENT, id);
    }

    protected <F extends FocusType> void assertNoAssignments(PrismObject<F> user) {
        new AssignmentAsserts(prismContext).assertNoAssignments(user);
    }

    protected void assertNoAssignments(String userOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertNoAssignments(user);
    }

    protected void assertNoAssignments(String userOid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(contextName() + ".assertNoShadow");
        assertNoAssignments(userOid, result);
    }

    protected AssignmentType assertAssignedRole(
            String userOid, String roleOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        return assertAssignedRole(user, roleOid);
    }

    protected <F extends FocusType> AssignmentType assertAssignedRole(
            PrismObject<F> user, String roleOid) {
        return new AssignmentAsserts(prismContext).assertAssignedRole(user, roleOid);
    }

    protected <F extends FocusType> void assertAssignedRoles(
            PrismObject<F> user, String... roleOids) {
        new AssignmentAsserts(prismContext).assertAssignedRoles(user, roleOids);
    }

    protected <F extends FocusType> void assertAssignedRoles(
            PrismObject<F> user, Collection<String> roleOids) {
        new AssignmentAsserts(prismContext).assertAssignedRoles(user, roleOids);
    }

    protected <R extends AbstractRoleType> AssignmentType assertInducedRole(
            PrismObject<R> role, String roleOid) {
        return new AssignmentAsserts(prismContext).assertInducedRole(role, roleOid);
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
                    for (ObjectReferenceType limitTarget : limitTargets) {
                        limitTargetContent.getTargetRef().add(limitTarget);
                    }
                    if (assignmentMutator != null) {
                        assignmentMutator.accept(assignment);
                    }
                }, add, result);
    }

    protected <F extends FocusType> void assertAssignedDeputy(
            PrismObject<F> focus, String targetUserOid) {
        new AssignmentAsserts(prismContext).assertAssigned(
                focus, targetUserOid, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEPUTY);
    }

    protected <F extends FocusType> void assertAssignedOrgs(
            PrismObject<F> user, String... orgOids) {
        new AssignmentAsserts(prismContext).assertAssignedOrgs(user, orgOids);
    }

    @UnusedTestElement
    protected void assertObjectRefs(String contextDesc, Collection<ObjectReferenceType> real, ObjectType... expected) {
        assertObjectRefs(contextDesc, real, objectsToOids(expected));
    }

    @UnusedTestElement
    protected void assertPrismRefValues(
            String contextDesc, Collection<PrismReferenceValue> real, ObjectType... expected) {
        assertPrismRefValues(contextDesc, real, objectsToOids(expected));
    }

    protected void assertPrismRefValues(String contextDesc,
            Collection<PrismReferenceValue> real, Collection<? extends ObjectType> expected) {
        assertPrismRefValues(contextDesc, real, objectsToOids(expected));
    }

    protected void assertObjectRefs(
            String contextDesc, Collection<ObjectReferenceType> real, String... expected) {
        assertObjectRefs(contextDesc, true, real, expected);
    }

    protected void assertObjectRefs(String contextDesc,
            boolean checkNames, Collection<ObjectReferenceType> real, String... expected) {
        List<String> refOids = new ArrayList<>();
        for (ObjectReferenceType ref : real) {
            refOids.add(ref.getOid());
            assertNotNull("Missing type in " + ref.getOid() + " in " + contextDesc, ref.getType());
            if (checkNames) {
                assertNotNull("Missing name in " + ref.getOid() + " in " + contextDesc, ref.getTargetName());
            }
        }
        PrismAsserts.assertSets("Wrong values in " + contextDesc, refOids, expected);
    }

    protected void assertPrismRefValues(String contextDesc, Collection<PrismReferenceValue> real, String... expected) {
        List<String> refOids = new ArrayList<>();
        for (PrismReferenceValue ref : real) {
            refOids.add(ref.getOid());
            assertNotNull("Missing type in " + ref.getOid() + " in " + contextDesc, ref.getTargetType());
            assertNotNull("Missing name in " + ref.getOid() + " in " + contextDesc, ref.getTargetName());
        }
        PrismAsserts.assertSets("Wrong values in " + contextDesc, refOids, expected);
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
        for (ObjectReferenceType ref : focus.asObjectable().getDelegatedRef()) {
            refOids.add(ref.getOid());
            assertNotNull("Missing type in delegatedRef " + ref.getOid() + " in " + focus, ref.getType());
        }
        PrismAsserts.assertSets("Wrong values in delegatedRef in " + focus, refOids, oids);
    }

    protected void assertNotAssignedRole(String userOid, String roleOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        new AssignmentAsserts(prismContext).assertNotAssignedRole(user, roleOid);
    }

    protected <F extends FocusType> void assertAssignedResource(
            Class<F> type, String userOid, String resourceOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<F> focus = repositoryService.getObject(type, userOid, null, result);
        new AssignmentAsserts(prismContext).assertAssignedResource(focus, resourceOid);
    }

    protected <F extends FocusType> void assertNotAssignedRole(PrismObject<F> user, String roleOid) {
        new AssignmentAsserts(prismContext).assertNotAssignedRole(user, roleOid);
    }

    protected <F extends FocusType> void assertNotAssignedOrg(
            PrismObject<F> user, String orgOid, QName relation) {
        new AssignmentAsserts(prismContext).assertNotAssignedOrg(user, orgOid, relation);
    }

    protected void assertAssignedOrg(
            String userOid, String orgOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertAssignedOrg(user, orgOid);
    }

    protected void assertAssignedOrg(
            PrismObject<? extends FocusType> focus, String orgOid, QName relation) {
        new AssignmentAsserts(prismContext).assertAssignedOrg(focus, orgOid, relation);
    }

    protected <F extends FocusType> AssignmentType assertAssignedOrg(
            PrismObject<F> focus, String orgOid) {
        return new AssignmentAsserts(prismContext).assertAssignedOrg(focus, orgOid);
    }

    protected <F extends FocusType> void assertNotAssignedOrg(
            PrismObject<F> focus, String orgOid) {
        new AssignmentAsserts(prismContext).assertNotAssignedOrg(focus, orgOid);
    }

    protected AssignmentType assertAssignedOrg(
            PrismObject<UserType> user, PrismObject<OrgType> org) {
        return new AssignmentAsserts(prismContext).assertAssignedOrg(user, org.getOid());
    }

    protected void assertHasOrg(String userOid, String orgOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user =
                repositoryService.getObject(UserType.class, userOid, null, result);
        assertAssignedOrg(user, orgOid);
    }

    protected <F extends FocusType> void assertHasOrgs(PrismObject<F> user, String... orgOids) {
        for (String orgOid : orgOids) {
            assertHasOrg(user, orgOid);
        }
        assertHasOrgs(user, orgOids.length);
    }

    protected <O extends ObjectType> void assertHasOrg(PrismObject<O> focus, String orgOid) {
        new AssignmentAsserts(prismContext).assertHasOrg(focus, orgOid);
    }

    protected <O extends ObjectType> void assertHasOrg(
            PrismObject<O> user, String orgOid, QName relation) {
        new AssignmentAsserts(prismContext).assertHasOrg(user, orgOid, relation);
    }

    protected <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user, String orgOid) {
        new AssignmentAsserts(prismContext).assertHasNoOrg(user, orgOid, null);
    }

    protected <O extends ObjectType> void assertHasNoOrg(
            PrismObject<O> user, String orgOid, QName relation) {
        new AssignmentAsserts(prismContext).assertHasNoOrg(user, orgOid, relation);
    }

    protected <O extends ObjectType> void assertHasNoOrg(PrismObject<O> user) {
        new AssignmentAsserts(prismContext).assertHasNoOrg(user);
    }

    protected <O extends ObjectType> void assertHasOrgs(PrismObject<O> user, int expectedNumber) {
        new AssignmentAsserts(prismContext).assertHasOrgs(user, expectedNumber);
    }

    @UnusedTestElement
    protected <AH extends AssignmentHolderType> void assertHasArchetypes(PrismObject<AH> object, String... oids) {
        for (String oid : oids) {
            assertHasArchetype(object, oid);
        }
        assertHasArchetypes(object, oids.length);
    }

    protected <O extends AssignmentHolderType> void assertHasArchetypes(
            PrismObject<O> object, int expectedNumber) {
        new AssignmentAsserts(prismContext).assertHasArchetypes(object, expectedNumber);
    }

    protected <AH extends AssignmentHolderType> void assertHasArchetype(
            PrismObject<AH> object, String oid) {
        new AssignmentAsserts(prismContext).assertHasArchetype(object, oid);
    }

    protected void assertSubOrgs(String baseOrgOid, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("assertSubOrgs");
        OperationResult result = task.getResult();
        List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrgOid, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Unexpected number of suborgs of org " + baseOrgOid + ", has suborgs " + subOrgs, expected, subOrgs.size());
    }

    protected void assertSubOrgs(PrismObject<OrgType> baseOrg, int expected) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("assertSubOrgs");
        OperationResult result = task.getResult();
        List<PrismObject<OrgType>> subOrgs = getSubOrgs(baseOrg.getOid(), task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Unexpected number of suborgs of" + baseOrg + ", has suborgs " + subOrgs, expected, subOrgs.size());
    }

    protected List<PrismObject<OrgType>> getSubOrgs(String baseOrgOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(OrgType.class)
                .isDirectChildOf(baseOrgOid)
                .build();
        return modelService.searchObjects(OrgType.class, query, null, task, result);
    }

    protected List<PrismObject<UserType>> getSubOrgUsers(String baseOrgOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .isDirectChildOf(baseOrgOid)
                .build();
        return modelService.searchObjects(UserType.class, query, null, task, result);
    }

    protected String dumpOrgTree(String topOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return dumpOrgTree(topOrgOid, false);
    }

    protected String dumpOrgTree(String topOrgOid, boolean dumpUsers) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("dumpOrgTree");
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
        for (PrismObject<OrgType> suborg : subOrgs) {
            dumpOrg(sb, suborg, indent);
            if (dumpUsers) {
                dumpOrgUsers(sb, suborg.getOid(), indent + 1, task, result);
            }
            sb.append("\n");
            dumpSubOrgs(sb, suborg.getOid(), dumpUsers, indent + 1, task, result);
        }
    }

    private void dumpOrgUsers(
            StringBuilder sb, String baseOrgOid, int indent, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<UserType>> subUsers = getSubOrgUsers(baseOrgOid, task, result);
        for (PrismObject<UserType> subuser : subUsers) {
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
        Task task = createPlainTask("displayUsers");
        OperationResult result = task.getResult();
        ResultHandler<UserType> handler = (user, parentResult) -> {
            display("User", user);
            return true;
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
        for (ObjectReferenceType parentOrgRef : focus.asObjectable().getParentOrgRef()) {
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
        for (ObjectReferenceType linkRef : focus.asObjectable().getLinkRef()) {
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
        for (AssignmentType assignmentType : focus.asObjectable().getAssignment()) {
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
                PrismObject<? extends ObjectType> target = repositoryService.getObject(targetClass, assignmentType.getTargetRef().getOid(), null, result);
                sb.append(target);
            }
        }
        displayValue(message, sb.toString());
    }

    protected <F extends AssignmentHolderType> void assertAssignments(PrismObject<F> user, int expectedNumber) {
        new AssignmentAsserts(prismContext).assertAssignments(user, expectedNumber);
    }

    protected <R extends AbstractRoleType> void assertInducements(PrismObject<R> role, int expectedNumber) {
        new AssignmentAsserts(prismContext).assertInducements(role, expectedNumber);
    }

    protected <R extends AbstractRoleType> void assertInducedRoles(PrismObject<R> role, String... roleOids) {
        assertInducements(role, roleOids.length);
        for (String roleOid : roleOids) {
            assertInducedRole(role, roleOid);
        }
    }

    protected <F extends AssignmentHolderType> void assertAssignments(
            PrismObject<F> user, Class expectedType, int expectedNumber) {
        new AssignmentAsserts(prismContext).assertAssignments(user, expectedType, expectedNumber);
    }

    protected <F extends AssignmentHolderType> void assertAssigned(PrismObject<F> user, String targetOid, QName refType) {
        new AssignmentAsserts(prismContext).assertAssigned(user, targetOid, refType);
    }

    protected void assertAssignedNoOrg(PrismObject<UserType> user) {
        assertAssignedNo(user, OrgType.COMPLEX_TYPE);
    }

    protected void assertAssignedNoRole(String userOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertAssignedNoRole(user);
    }

    protected <F extends FocusType> void assertAssignedNoRole(PrismObject<F> user) {
        assertAssignedNo(user, RoleType.COMPLEX_TYPE);
    }

    protected void assertAssignedNoPolicy(String userOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertAssignedNoPolicy(user);
    }

    protected <F extends FocusType> void assertAssignedNoPolicy(PrismObject<F> user) {
        assertAssignedNo(user, PolicyType.COMPLEX_TYPE);
    }

    protected <F extends FocusType> void assertAssignedNo(PrismObject<F> user, QName refType) {
        F userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null) {
                if (refType.equals(targetRef.getType())) {
                    AssertJUnit.fail(user + " has role " + targetRef.getOid() + " while expected no roles");
                }
            }
        }
    }

    protected void assertAssignedAccount(
            String userOid, String resourceOid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        assertAssignedAccount(user, resourceOid);
    }

    protected AssignmentType assertAssignedAccount(PrismObject<UserType> user, String resourceOid) {
        UserType userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
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
        AssertJUnit.fail(user + " does not have account assignment for resource " + resourceOid);
        return null; // not reached
    }

    protected void assertAssignedNoAccount(PrismObject<UserType> user, String resourceOid) {
        UserType userType = user.asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ConstructionType construction = assignmentType.getConstruction();
            if (construction != null) {
                if (construction.getKind() != null && construction.getKind() != ShadowKindType.ACCOUNT) {
                    continue;
                }
                if (resourceOid.equals(construction.getResourceRef().getOid())) {
                    AssertJUnit.fail(user + " has account assignment for resource " + resourceOid + " while expecting no such assignment");
                }
            }
        }
    }

    @Override
    protected PrismObjectDefinition<RoleType> getRoleDefinition() {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
    }

    @Override
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

    protected PrismObject<ShadowType> createAccount(PrismObject<ResourceType> resource, String name, boolean enabled)
            throws SchemaException, ConfigurationException {
        PrismObject<ShadowType> shadow = getShadowDefinition().instantiate();
        ShadowType shadowType = shadow.asObjectable();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        shadowType.setResourceRef(resourceRef);
        ResourceSchema rSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource.asObjectable());
        ResourceObjectDefinition objectClassDefinition = rSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        shadowType.setObjectClass(objectClassDefinition.getTypeName());
        shadowType.setKind(ShadowKindType.ACCOUNT);
        shadowType.setIntent(INTENT_DEFAULT);
        ShadowAttributesContainer attrCont = ShadowUtil.getOrCreateAttributesContainer(shadow, objectClassDefinition);
        ShadowSimpleAttributeDefinition<?> idSecondaryDef = objectClassDefinition.getSecondaryIdentifiers().iterator().next();
        ShadowSimpleAttribute icfsNameAttr = idSecondaryDef.instantiate();
        icfsNameAttr.setRealValue(name);
        attrCont.addAttribute(icfsNameAttr);
        ActivationType activation = new ActivationType();
        shadowType.setActivation(activation);
        if (enabled) {
            activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        } else {
            activation.setAdministrativeStatus(ActivationStatusType.DISABLED);
        }
        return shadow;
    }

    protected <T> void addAttributeToShadow(PrismObject<ShadowType> shadow, String attrName, T attrValue) throws SchemaException {
        ShadowUtil.getAttributesContainer(shadow).addSimpleAttribute(
                ItemName.from(MidPointConstants.NS_RI, attrName),
                attrValue);
    }

    protected void setDefaultUserTemplate(String userTemplateOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, userTemplateOid);
    }

    protected void setDefaultObjectTemplate(QName objectType, String userTemplateOid)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = new OperationResult(contextName() + ".setDefaultObjectTemplate");
        setDefaultObjectTemplate(objectType, userTemplateOid, result);
        result.computeStatus();
        TestUtil.assertSuccess("Applying default object template failed (result)", result);
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
        for (ObjectPolicyConfigurationType focusPolicyType : systemConfig.asObjectable().getDefaultObjectPolicyConfiguration()) {
            if (QNameUtil.match(objectType, focusPolicyType.getType()) && MiscUtil.equals(subType, focusPolicyType.getSubtype())) {
                oldValue = focusPolicyType.asPrismContainerValue();
            }
        }
        Collection<ContainerDelta<ObjectPolicyConfigurationType>> modifications = new ArrayList<>();

        if (oldValue != null) {
            ObjectPolicyConfigurationType oldPolicy = oldValue.asContainerable();
            ObjectReferenceType oldObjectTemplateRef = oldPolicy.getObjectTemplateRef();
            if (oldObjectTemplateRef != null) {
                if (oldObjectTemplateRef.getOid().equals(objectTemplateOid)) {
                    // Already set
                    return;
                }
            }
            ContainerDelta<ObjectPolicyConfigurationType> deleteDelta =
                    prismContext.deltaFactory().container().createModificationDelete(
                            SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                            SystemConfigurationType.class, oldValue.clone());
            modifications.add(deleteDelta);
        }

        if (objectTemplateOid != null) {
            ObjectPolicyConfigurationType newFocusPolicyType;
            ContainerDelta<ObjectPolicyConfigurationType> addDelta;
            if (oldValue == null) {
                newFocusPolicyType = new ObjectPolicyConfigurationType();
                newFocusPolicyType.setType(objectType);
                newFocusPolicyType.setSubtype(subType);
                addDelta = prismContext.deltaFactory().container().createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                        SystemConfigurationType.class, newFocusPolicyType);
            } else {
                PrismContainerValue<ObjectPolicyConfigurationType> newValue = oldValue.cloneComplex(CloneStrategy.REUSE);
                addDelta = prismContext.deltaFactory().container().createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                        SystemConfigurationType.class, newValue);
                newFocusPolicyType = newValue.asContainerable();
            }
            ObjectReferenceType templateRef = new ObjectReferenceType();
            templateRef.setOid(objectTemplateOid);
            newFocusPolicyType.setObjectTemplateRef(templateRef);
            modifications.add(addDelta);
        }

        modifySystemObjectInRepo(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);

    }

    protected void setConflictResolution(QName objectType, String subType, ConflictResolutionType conflictResolution, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        PrismObject<SystemConfigurationType> systemConfig = repositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, parentResult);

        PrismContainerValue<ObjectPolicyConfigurationType> oldValue = null;
        for (ObjectPolicyConfigurationType focusPolicyType : systemConfig.asObjectable().getDefaultObjectPolicyConfiguration()) {
            if (QNameUtil.match(objectType, focusPolicyType.getType()) && MiscUtil.equals(subType, focusPolicyType.getSubtype())) {
                oldValue = focusPolicyType.asPrismContainerValue();
            }
        }
        Collection<ContainerDelta<ObjectPolicyConfigurationType>> modifications = new ArrayList<>();

        if (oldValue != null) {
            ContainerDelta<ObjectPolicyConfigurationType> deleteDelta = prismContext.deltaFactory().container().createModificationDelete(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                    SystemConfigurationType.class, oldValue.clone());
            modifications.add(deleteDelta);
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

        ContainerDelta<ObjectPolicyConfigurationType> addDelta = prismContext.deltaFactory().container()
                .createModificationAdd(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION,
                        SystemConfigurationType.class, newFocusPolicyType);

        modifications.add(addDelta);

        modifySystemObjectInRepo(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);

    }

    protected void setConflictResolutionAction(QName objectType, String subType,
            ConflictResolutionActionType conflictResolutionAction, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        ConflictResolutionType conflictResolutionType = new ConflictResolutionType();
        conflictResolutionType.action(conflictResolutionAction);
        setConflictResolution(objectType, subType, conflictResolutionType, parentResult);
    }

    protected void setGlobalSecurityPolicy(String securityPolicyOid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        Collection<ReferenceDelta> modifications = new ArrayList<>();

        ReferenceDelta refDelta = prismContext.deltaFactory().reference()
                .createModificationReplace(SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF,
                        SystemConfigurationType.class, securityPolicyOid);
        modifications.add(refDelta);

        modifySystemObjectInRepo(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, parentResult);

    }

    protected <O extends ObjectType> void modifySystemObjectInRepo(Class<O> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        display("Modifications of system object " + oid, modifications);
        repositoryService.modifyObject(type, oid, modifications, parentResult);
    }

    protected ItemPath getIcfsNameAttributePath() {
        return ItemPath.create(
                ShadowType.F_ATTRIBUTES,
                ICFS_NAME);
    }

    /**
     * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
     * existing user values.
     */
    protected void breakAssignmentDelta(Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
        breakAssignmentDelta((ObjectDelta<? extends FocusType>) deltas.iterator().next());
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
        PrismContainerValue<ActivationType> emptyValue = prismContext.itemFactory().createContainerValue();
        activationContainer.add(emptyValue);
    }

    protected void purgeResourceSchema(String resourceOid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("purgeResourceSchema");
        OperationResult result = task.getResult();

        ObjectDelta<ResourceType> resourceDelta =
                prismContext.deltaFactory().object().createModificationReplaceContainer(
                        ResourceType.class, resourceOid, ResourceType.F_SCHEMA, new PrismContainerValue[0]);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(resourceDelta);

        modelService.executeChanges(deltas, null, task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected List<PrismObject<OrgType>> searchOrg(String baseOrgOid, OrgFilter.Scope scope, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(OrgType.class)
                .isInScopeOf(baseOrgOid, scope)
                .build();
        return modelService.searchObjects(OrgType.class, query, null, task, result);
    }

    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("searchObjectByName");
        OperationResult result = task.getResult();
        PrismObject<T> out = searchObjectByName(type, name, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return out;
    }

    protected <T extends ObjectType> PrismObject<T> searchObjectByName(Class<T> type, String name, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = ObjectQueryUtil.createNameQuery(name);
        List<PrismObject<T>> foundObjects = modelService.searchObjects(type, query, null, task, result);
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next();
    }

    protected void assertAccountShadowModel(
            PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType)
            throws SchemaException, ConfigurationException {
        assertShadowModel(accountShadow, oid, username, resourceType, RI_ACCOUNT_OBJECT_CLASS, null);
    }

    protected void assertAccountShadowModel(
            PrismObject<ShadowType> accountShadow,
            String oid,
            String username,
            ResourceType resourceType,
            MatchingRule<String> matchingRule) throws SchemaException, ConfigurationException {
        assertShadowModel(accountShadow, oid, username, resourceType, RI_ACCOUNT_OBJECT_CLASS, matchingRule);
    }

    protected void assertShadowModel(
            PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, QName objectClass)
            throws SchemaException, ConfigurationException {
        assertShadowModel(accountShadow, oid, username, resourceType, objectClass, null);
    }

    protected void assertShadowModel(
            PrismObject<ShadowType> accountShadow,
            String oid,
            String username,
            ResourceType resourceType,
            QName objectClass,
            MatchingRule<String> nameMatchingRule) throws SchemaException, ConfigurationException {
        assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, nameMatchingRule);
        IntegrationTestTools.assertProvisioningShadow(accountShadow, ShadowSimpleAttributeDefinition.class, objectClass);
    }

    protected ObjectDelta<UserType> createModifyUserAddDummyAccount(String userOid, String dummyResourceName)
            throws SchemaException, ConfigurationException {
        return createModifyUserAddAccount(userOid, getDummyResourceObject(dummyResourceName));
    }

    protected ObjectDelta<UserType> createModifyUserAddAccount(String userOid, PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        return createModifyUserAddAccount(userOid, resource, INTENT_DEFAULT);
    }

    protected ObjectDelta<UserType> createModifyUserAddAccount(
            String userOid, PrismObject<ResourceType> resource, @NotNull String intent)
            throws SchemaException, ConfigurationException {
        PrismObject<ShadowType> account = getAccountShadowDefinition().instantiate();
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        account.asObjectable().setResourceRef(resourceRef);
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectDefinition rocd = refinedSchema.findObjectDefinitionRequired(ShadowKindType.ACCOUNT, intent);
        account.asObjectable().setIntent(intent);
        account.asObjectable().setObjectClass(rocd.getObjectClassName());
        account.asObjectable().setKind(ShadowKindType.ACCOUNT);
        account.asObjectable().setIntent(intent);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);

        return userDelta;
    }

    protected ObjectDelta<UserType> createModifyUserAssignAccount(String userOid, String resourceOid)
            throws SchemaException {
        return createModifyUserAssignAccount(userOid, resourceOid, INTENT_DEFAULT);
    }

    @SuppressWarnings("SameParameterValue")
    protected ObjectDelta<UserType> createModifyUserAssignAccount(String userOid, String resourceOid, @NotNull String intent)
            throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                .kind(ShadowKindType.ACCOUNT)
                                .intent(intent)))
                .asObjectDelta(userOid);
    }

    /** Simplistic: expects the exact assignment being present (no variations). */
    protected ObjectDelta<UserType> createModifyUserUnassignAccount(String userOid, String resourceOid)
            throws SchemaException {
        return createModifyUserUnassignAccount(userOid, resourceOid, INTENT_DEFAULT);
    }

    /** Simplistic: expects the exact assignment being present (no variations). */
    @SuppressWarnings("SameParameterValue")
    protected ObjectDelta<UserType> createModifyUserUnassignAccount(
            String userOid, String resourceOid, @NotNull String intent)
            throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                .kind(ShadowKindType.ACCOUNT)
                                .intent(intent)))
                .asObjectDelta(userOid);
    }

    protected ObjectDelta<UserType> createModifyUserDeleteDummyAccount(String userOid, String dummyResourceName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return createModifyUserDeleteAccount(userOid, getDummyResourceObject(dummyResourceName));
    }

    protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, PrismObject<ResourceType> resource) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return createModifyUserDeleteAccount(userOid, resource.getOid());
    }

    protected ObjectDelta<UserType> createModifyUserDeleteAccount(String userOid, String resourceOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String accountOid = getLiveLinkRefOid(userOid, resourceOid);
        PrismObject<ShadowType> account = getShadowModel(accountOid);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid
        );
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference().createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);

        return userDelta;
    }

    protected ObjectDelta<UserType> createModifyUserUnlinkAccount(String userOid, PrismObject<ResourceType> resource)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return createModifyUserUnlinkAccount(userOid, resource.getOid());
    }

    protected ObjectDelta<UserType> createModifyUserUnlinkAccount(String userOid, String resourceOid)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        String accountOid = getLiveLinkRefOid(userOid, resourceOid);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setOid(accountOid);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);

        return userDelta;
    }

    protected void deleteUserAccount(String userOid, String resourceOid, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(userOid, resourceOid);
        executeChanges(userDelta, null, task, result);
    }

    // TASKS

    protected List<CaseType> getSubcases(String parentCaseOid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException {
        return asObjectableList(
                repositoryService.searchObjects(CaseType.class,
                        prismContext.queryFor(CaseType.class).item(CaseType.F_PARENT_REF).ref(parentCaseOid).build(),
                        options, result));
    }

    protected void deleteCaseTree(String rootCaseOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<CaseType> subcases = getSubcases(rootCaseOid, null, result);
        for (CaseType subcase : subcases) {
            deleteCaseTree(subcase.getOid(), result);
        }
        repositoryService.deleteObject(CaseType.class, rootCaseOid, result);
    }

    protected void displayTaskWithOperationStats(String message, PrismObject<TaskType> task) throws SchemaException {
        display(message, task);
        String stats = prismContext.xmlSerializer()
                .serializeRealValue(task.asObjectable().getOperationStats(), TaskType.F_OPERATION_STATS);
        displayValue(message + ": Operational stats", stats);
    }

    protected void displayTaskWithOperationStats(String message, Task task) throws SchemaException {
        display(message, task);
        String stats = prismContext.xmlSerializer()
                .serializeRealValue(task.getStoredOperationStatsOrClone(), TaskType.F_OPERATION_STATS);
        displayValue(message + ": Operational stats", stats);
    }

    protected void assertJpegPhoto(Class<? extends FocusType> clazz, String oid, byte[] expectedValue, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        PrismObject<? extends FocusType> object = repositoryService
                .getObject(clazz, oid, schemaService.getOperationOptionsBuilder().retrieve().build(), result);
        byte[] actualValue = object.asObjectable().getJpegPhoto();
        if (expectedValue == null) {
            if (actualValue != null) {
                fail("Photo present even if it should not be: " + Arrays.toString(actualValue));
            }
        } else {
            assertNotNull("No photo", actualValue);
            if (!Arrays.equals(actualValue, expectedValue)) {
                fail("Photo is different than expected.\nExpected = " + Arrays.toString(expectedValue)
                        + "\nActual value = " + Arrays.toString(actualValue));
            }
        }
    }

    protected void waitForTaskStart(String taskOid) throws Exception {
        waitForTaskStart(taskOid, DEFAULT_TASK_WAIT_TIMEOUT);
    }

    protected void waitForTaskStart(String taskOid, int timeout) throws Exception {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskStart");
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
                OperationResult result = freshTask.getResult();
                if (verbose) {display("Task checked (result=" + result + ")", freshTask);}
                assert !isError(result) : "Error in " + freshTask + ": " + TestUtil.getErrorMessage(result);
                if (isUnknown(result)) {
                    return false;
                }
                return freshTask.getLastRunStartTimestamp() != null;
            }

            @Override
            public void timeout() {
                try {
                    Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
                    OperationResult result = freshTask.getResult();
                    logger.debug("Result of timed-out task:\n{}", DebugUtil.debugDump(result));
                    assert false : "Timeout (" + timeout + ") while waiting for " + freshTask + " to start. Last result " + result;
                } catch (ObjectNotFoundException | SchemaException e) {
                    logger.error("Exception during task refresh: {}", e, e);
                }
            }
        };
        IntegrationTestTools.waitFor("Waiting for task " + taskOid + " start", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
    }

    protected void waitForTaskNextStart(String taskOid, int timeout, boolean kickTheTask) throws Exception {
        OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskNextStart");
        Task origTask = taskManager.getTaskWithResult(taskOid, waitResult);
        Long origLastRunStartTimestamp = origTask.getLastRunStartTimestamp();
        if (kickTheTask) {
            taskManager.scheduleTaskNow(origTask, waitResult);
        }
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
                OperationResult result = freshTask.getResult();
                if (verbose) {display("Check result", result);}
                assert !isError(result) : "Error in " + freshTask + ": " + TestUtil.getErrorMessage(result);
                return !isUnknown(result) &&
                        !java.util.Objects.equals(freshTask.getLastRunStartTimestamp(), origLastRunStartTimestamp);
            }

            @Override
            public void timeout() {
                try {
                    Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
                    OperationResult result = freshTask.getResult();
                    logger.debug("Result of timed-out task:\n{}", result.debugDump());
                    assert false : "Timeout (" + timeout + ") while waiting for " + freshTask + " to start. Last result " + result;
                } catch (ObjectNotFoundException | SchemaException e) {
                    logger.error("Exception during task refresh: {}", e, e);
                }
            }
        };
        IntegrationTestTools.waitFor("Waiting for task " + taskOid + " next start", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
    }

    protected OperationResult waitForTaskNextRunAssertSuccess(String taskOid) throws Exception {
        return waitForTaskNextRunAssertSuccess(taskOid, DEFAULT_TASK_WAIT_TIMEOUT);
    }

    protected OperationResult waitForTaskNextRunAssertSuccess(Task origTask) throws Exception {
        return waitForTaskNextRunAssertSuccess(origTask, DEFAULT_TASK_WAIT_TIMEOUT);
    }

    protected OperationResult waitForTaskNextRunAssertSuccess(String taskOid, int timeout) throws Exception {
        OperationResult taskResult = waitForTaskNextRun(taskOid, timeout);
        if (isError(taskResult)) {
            assert false : "Error in task " + taskOid + ": " + TestUtil.getErrorMessage(taskResult) + "\n\n" + taskResult.debugDump();
        }
        return taskResult;
    }

    protected OperationResult waitForTaskNextRunAssertSuccess(Task origTask, int timeout) throws CommonException {
        OperationResult taskResult = waitForTaskNextRun(origTask, timeout);
        if (isError(taskResult)) {
            assert false : "Error in task " + origTask + ": " + TestUtil.getErrorMessage(taskResult) + "\n\n" + taskResult.debugDump();
        }
        return taskResult;
    }

    protected OperationResult waitForTaskNextRun(final String taskOid) throws CommonException {
        return waitForTaskNextRun(taskOid, DEFAULT_TASK_WAIT_TIMEOUT, false);
    }

    protected OperationResult waitForTaskNextRun(String taskOid, int timeout) throws CommonException {
        return waitForTaskNextRun(taskOid, timeout, false);
    }

    protected OperationResult waitForTaskActivityCompleted(final String taskOid, long startedAfter, OperationResult waitResult, long timeout) throws CommonException {
        final Holder<OperationResult> taskResultHolder = new Holder<>();
        waitForTaskStatusUpdated(taskOid, "Waiting for task " + taskOid, new Checker() {
            @Override
            public boolean check() throws CommonException {
                var task = taskManager.getTaskWithResult(taskOid, waitResult);
                var activitiesState = task.getActivitiesStateOrClone();
                if (activitiesState == null) {
                    return false;
                }
                var activity = activitiesState.getActivity();
                if (activity == null) {
                    return false;
                }
                var activityStart = XmlTypeConverter.toMillis(activity.getRealizationStartTimestamp());
                var activityEnd = XmlTypeConverter.toMillis(activity.getRealizationEndTimestamp());
                if (activityStart > startedAfter && activityEnd > activityStart) {
                    taskResultHolder.setValue(task.getResult());
                    return true;
                }
                return false;
            }

            @Override
            public void timeout() {
                assert false : "Timeouted while waiting for task " + taskOid + " activity to complete.";
            }
        }, timeout);
        return taskResultHolder.getValue();
    }

    protected OperationResult waitForTaskActivityCompleted(final String taskOid) throws CommonException {
        var result = waitForTaskActivityCompleted(taskOid, System.currentTimeMillis(), createOperationResult(), DEFAULT_TASK_WAIT_TIMEOUT);
        if (isError(result)) {
            assert false : "Task failed";
        }
        return result;
    }

    protected OperationResult waitForTaskNextRun(String taskOid, int timeout, boolean kickTheTask) throws CommonException {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskNextRun");
        Task origTask = taskManager.getTaskWithResult(taskOid, waitResult);
        return waitForTaskNextRun(origTask, timeout, waitResult, kickTheTask);
    }

    protected OperationResult waitForTaskNextRun(Task origTask, int timeout) throws CommonException {
        return waitForTaskNextRun(origTask, timeout, false);
    }

    protected OperationResult waitForTaskNextRun(Task origTask, int timeout, boolean kickTheTask) throws CommonException {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskNextRun");
        return waitForTaskNextRun(origTask, timeout, waitResult, kickTheTask);
    }

    protected OperationResult waitForTaskNextRun(Task origTask, int timeout, OperationResult waitResult, boolean kickTheTask)
            throws CommonException {
        final long waitStartTime = System.currentTimeMillis();
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
                if (verbose) {display("Check result", taskResult);}
                taskResultHolder.setValue(taskResult);
                if (isError(taskResult)) {
                    return true;
                }
                if (isUnknown(taskResult)) {
                    return false;
                }
                if (freshTask.getLastRunFinishTimestamp() == null) {
                    return false;
                }
                if (freshTask.getLastRunStartTimestamp() == null) {
                    return false;
                }
                if (freshTask.getLastRunStartTimestamp() < waitStartTime) {
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
                    logger.debug("Timed-out task:\n{}", freshTask.debugDump());
                    displayValue("Times", "origLastRunStartTimestamp=" + longTimeToString(origLastRunStartTimestamp)
                            + ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp)
                            + ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
                            + ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));
                    assert false : "Timeout (" + timeout + ") while waiting for " + freshTask + " next run. Last result " + result;
                } catch (ObjectNotFoundException | SchemaException e) {
                    logger.error("Exception during task refresh: {}", e, e);
                }
            }
        };

        waitForTaskStatusUpdated(origTask.getOid(), "Waiting for task " + origTask + " next run", checker, timeout);

        Task freshTask = taskManager.getTaskWithResult(origTask.getOid(), waitResult);
        logger.debug("Final task:\n{}", freshTask.debugDump());
        displayValue("Times", "origLastRunStartTimestamp=" + longTimeToString(origLastRunStartTimestamp)
                + ", origLastRunFinishTimestamp=" + longTimeToString(origLastRunFinishTimestamp)
                + ", freshTask.getLastRunStartTimestamp()=" + longTimeToString(freshTask.getLastRunStartTimestamp())
                + ", freshTask.getLastRunFinishTimestamp()=" + longTimeToString(freshTask.getLastRunFinishTimestamp()));

        return taskResultHolder.getValue();
    }

    // We assume the task is runnable/running.
    // Uses heartbeat method to determine the progress; so the progress may not be reflected in the repo after returning
    // from this method.
    @Experimental
    protected Task waitForTaskProgress(String taskOid, long progressToReach, int timeout, OperationResult waitResult) throws Exception {
        return waitForTaskProgress(taskOid, progressToReach, null, timeout, (int) DEFAULT_TASK_SLEEP_TIME, waitResult);
    }

    // Uses heartbeat method to determine the progress; so the progress may not be reflected in the repo after returning
    // from this method.
    @Experimental
    protected Task waitForTaskProgress(String taskOid, long progressToReach, CheckedProducer<Boolean> extraTest,
            int timeout, int sleepTime, OperationResult waitResult) throws Exception {
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                Task freshRepoTask = taskManager.getTaskWithResult(taskOid, waitResult);
                displaySingleTask("Repo task while waiting for progress reach " + progressToReach, freshRepoTask);
                Long heartbeat = activityBasedTaskHandler.heartbeat(freshRepoTask);
                if (heartbeat != null) {
                    displayValue("Heartbeat", heartbeat);
                }
                long progress = heartbeat != null ? heartbeat : freshRepoTask.getLegacyProgress();
                boolean extraTestSuccess = extraTest != null && Boolean.TRUE.equals(extraTest.get());
                return extraTestSuccess ||
                        freshRepoTask.getExecutionState() == TaskExecutionStateType.SUSPENDED ||
                        freshRepoTask.getExecutionState() == TaskExecutionStateType.CLOSED ||
                        progress >= progressToReach;
            }

            @Override
            public void timeout() {
                try {
                    Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
                    OperationResult result = freshTask.getResult();
                    logger.debug("Timed-out task:\n{}", freshTask.debugDump());
                    assert false : "Timeout (" + timeout + ") while waiting for " + freshTask + " progress. Last result " + result;
                } catch (ObjectNotFoundException | SchemaException e) {
                    logger.error("Exception during task refresh: {}", e, e);
                }
            }
        };
        IntegrationTestTools.waitFor("Waiting for task " + taskOid + " progress reaching " + progressToReach,
                checker, timeout, sleepTime);

        Task freshTask = taskManager.getTaskWithResult(taskOid, waitResult);
        logger.debug("Final task:\n{}", freshTask.debugDump());
        return freshTask;
    }

    protected void runTaskTreeAndWaitForFinish(String rootTaskOid, int timeout) throws Exception {
        OperationResult result = getTestOperationResult();
        Task origRootTask = taskManager.getTask(rootTaskOid, null, result);
        restartTask(rootTaskOid, result);
        waitForRootActivityCompletion(
                rootTaskOid,
                origRootTask.getRootActivityCompletionTimestamp(),
                timeout);
    }

    protected void resumeTaskTreeAndWaitForFinish(String rootTaskOid, int timeout) throws Exception {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".runTaskTreeAndWaitForFinish");
        Task origRootTask = taskManager.getTaskWithResult(rootTaskOid, waitResult);
        taskManager.resumeTaskTree(rootTaskOid, waitResult);
        waitForRootActivityCompletion(
                rootTaskOid,
                origRootTask.getRootActivityCompletionTimestamp(),
                timeout);
    }

    /**
     * Simplified version of {@link #waitForRootActivityCompletion(String, XMLGregorianCalendar, long)}.
     *
     * To be used on tasks that are scheduled to be run in regular intervals. (So it needs not be absolutely precise:
     * if the task realization completes between the method is started and the current completion timestamp is determined,
     * it's no problem: the task will be started again in the near future.)
     *
     * @return root task in the moment of completion
     */
    protected Task waitForNextRootActivityCompletion(@NotNull String rootTaskOid, int timeout) throws CommonException {
        OperationResult result = getTestOperationResult();
        XMLGregorianCalendar currentCompletionTimestamp = taskManager.getTaskWithResult(rootTaskOid, result)
                .getRootActivityCompletionTimestamp();
        return waitForRootActivityCompletion(rootTaskOid, currentCompletionTimestamp, timeout);
    }

    /**
     * Simplified version of {@link #waitForRootActivityCompletion(String, XMLGregorianCalendar, long)}.
     *
     * To be used on tasks that were _not_ executed before. I.e. we are happy with any task completion.
     */
    protected void waitForRootActivityCompletion(@NotNull String rootTaskOid, long timeout) throws CommonException {
        waitForRootActivityCompletion(rootTaskOid, null, timeout);
    }

    /**
     * Waits for the completion of the root activity realization. Useful for task trees.
     *
     * Stops also if there's a failed/suspended activity - see {@link #findSuspendedActivity(Task)}.
     * This is to account for suspended multi-node tasks like `TestThresholdsStoryReconExecuteMultinode`.
     *
     * TODO reconcile with {@link #waitForTaskActivityCompleted(String, long, OperationResult, long)}
     *
     * @param lastKnownCompletionTimestamp The completion we know about - and are _not_ interested in. If null,
     * we are interested in any completion.
     */
    protected Task waitForRootActivityCompletion(
            @NotNull String rootTaskOid,
            @Nullable XMLGregorianCalendar lastKnownCompletionTimestamp,
            long timeout) throws CommonException {
        OperationResult waitResult = getTestOperationResult();
        Task freshRootTask = taskManager.getTaskWithResult(rootTaskOid, waitResult);
        argCheck(freshRootTask.getParent() == null, "Non-root task: %s", freshRootTask);
        Checker checker = () -> {

            // This is just to display the progress (we don't have the completion timestamp there)
            assertProgress(rootTaskOid, "waiting for activity completion")
                    .display();

            // Now do the real check now
            freshRootTask.refresh(waitResult);
            var rootState = freshRootTask.getActivityStateOrClone(ActivityPath.empty());

            if (verbose) {
                displayValueAsXml("overview", freshRootTask.getActivityTreeStateOverviewOrClone());
            }

            if (rootState != null
                    && rootState.getRealizationState() == ActivityRealizationStateType.COMPLETE
                    && isDifferent(lastKnownCompletionTimestamp, rootState.getRealizationEndTimestamp())) {
                return true;
            }

            ActivityStateOverviewType suspended = findSuspendedActivity(freshRootTask);
            if (suspended != null) {
                displayValueAsXml("Suspended activity -> not waiting anymore", suspended);
                return true;
            }

            // The task lives: let's continue waiting
            return false;
        };

        IntegrationTestTools.waitFor("Waiting for task tree " + freshRootTask + " next finished run",
                checker, timeout, DEFAULT_TASK_TREE_SLEEP_TIME);
        // We must NOT update the task. It should be in the "completed" state. (Because the task may be recurring,
        // so updating could get the state from a subsequent run.)
        logger.debug("Final root task:\n{}", freshRootTask.debugDump());
        stabilize(); // TODO needed?
        return freshRootTask;
    }

    /**
     * Finds an activity that:
     *
     * - is in progress,
     * - has at least one worker task,
     * - all of the workers are marked as "not running", at least one of them is marked as failed, and is suspended.
     *
     * This is to avoid waiting for multi-node tasks that will never complete.
     *
     * It is ugly and not 100% reliable: in theory, the failure may be expected, and the current state may be transient.
     * But it's probably the best we can do now.
     */
    private @Nullable ActivityStateOverviewType findSuspendedActivity(Task task) throws SchemaException, ObjectNotFoundException {
        ActivityStateOverviewType root = task.getActivityTreeStateOverviewOrClone();
        return root != null ? findSuspendedActivity(root) : null;
    }

    private @Nullable ActivityStateOverviewType findSuspendedActivity(@NotNull ActivityStateOverviewType activityStateOverview)
            throws SchemaException, ObjectNotFoundException {
        if (activityStateOverview.getRealizationState() != ActivitySimplifiedRealizationStateType.IN_PROGRESS) {
            return null; // Not started or complete
        }
        if (isSuspended(activityStateOverview)) {
            return activityStateOverview;
        }
        for (ActivityStateOverviewType child : activityStateOverview.getActivity()) {
            ActivityStateOverviewType suspendedInChild = findSuspendedActivity(child);
            if (suspendedInChild != null) {
                return suspendedInChild;
            }
        }
        return null;
    }

    private boolean isSuspended(ActivityStateOverviewType activityStateOverview) throws SchemaException, ObjectNotFoundException {
        List<ActivityTaskStateOverviewType> tasks = activityStateOverview.getTask();
        if (tasks.isEmpty()) {
            return false;
        }
        if (tasks.stream().anyMatch(task -> task.getExecutionState() != ActivityTaskExecutionStateType.NOT_RUNNING)) {
            return false;
        }
        for (ActivityTaskStateOverviewType task : tasks) {
            if (isSuspended(task)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuspended(ActivityTaskStateOverviewType taskInfo) throws SchemaException, ObjectNotFoundException {
        if (taskInfo.getResultStatus() != OperationResultStatusType.FATAL_ERROR) {
            return false;
        }
        if (taskInfo.getTaskRef() == null || taskInfo.getTaskRef().getOid() == null) {
            return false; // shouldn't occur
        }
        Task task = taskManager.getTaskPlain(taskInfo.getTaskRef().getOid(), getTestOperationResult());
        return task.getExecutionState() == TaskExecutionStateType.SUSPENDED;
    }

    private boolean isDifferent(@Nullable XMLGregorianCalendar lastKnownTimestamp, XMLGregorianCalendar realTimestamp) {
        return lastKnownTimestamp == null
                || !lastKnownTimestamp.equals(realTimestamp);
    }

    public void waitForCaseClose(String caseOid) throws CommonException {
        CaseType aCase = repositoryService
                .getObject(CaseType.class, caseOid, null, getTestOperationResult())
                .asObjectable();
        waitForCaseClose(aCase, 60000);
    }

    public void waitForCaseClose(CaseType aCase) throws Exception {
        waitForCaseClose(aCase, 60000);
    }

    public void waitForCaseClose(CaseType aCase, final int timeout) throws CommonException {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForCaseClose");
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                CaseType currentCase = repositoryService.getObject(CaseType.class, aCase.getOid(), null, waitResult).asObjectable();
                if (verbose) {AbstractIntegrationTest.display("Case", currentCase);}
                return SchemaConstants.CASE_STATE_CLOSED.equals(currentCase.getState());
            }

            @Override
            public void timeout() {
                PrismObject<CaseType> currentCase;
                try {
                    currentCase = repositoryService.getObject(CaseType.class, aCase.getOid(), null, waitResult);
                } catch (ObjectNotFoundException | SchemaException e) {
                    throw new AssertionError("Couldn't retrieve case " + aCase, e);
                }
                logger.debug("Timed-out case:\n{}", currentCase.debugDump());
                assert false : "Timeout (" + timeout + ") while waiting for " + currentCase + " to finish";
            }
        };
        IntegrationTestTools.waitFor("Waiting for " + aCase + " finish", checker, timeout, 1000);
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
        for (ItemDelta<?, ?> modification : focusDelta.getModifications()) {
            if (isSideEffectDelta(modification)) {
                expectedModifications++;
            }
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_ENABLE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_ARCHIVE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        PropertyDelta<ActivationStatusType> effectiveStatusDelta = focusDelta.findPropertyDelta(PATH_ACTIVATION_EFFECTIVE_STATUS);
        if (effectiveStatusDelta != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_ITERATION) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_ROLE_MEMBERSHIP_REF) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_DELEGATED_REF) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_ITERATION_TOKEN) != null) {
            expectedModifications++;
        }
        assertEquals("Unexpected modifications in " + desc + ": " + focusDelta, expectedModifications, focusDelta.getModifications().size());
    }

    protected <F extends FocusType> void assertSideEffectiveDeltasOnly(
            ObjectDelta<F> focusDelta, String desc, ActivationStatusType expectedEfficientActivation) {
        assertEffectualDeltas(focusDelta, desc, expectedEfficientActivation, 0);
    }

    protected <F extends FocusType> void assertEffectualDeltas(
            ObjectDelta<F> focusDelta, String desc, ActivationStatusType expectedEfficientActivation, int expectedEffectualModifications) {
        if (focusDelta == null) {
            return;
        }
        int expectedModifications = expectedEffectualModifications;
        // There may be metadata modification, we tolerate that
        for (ItemDelta<?, ?> modification : focusDelta.getModifications()) {
            if (isSideEffectDelta(modification)) {
                expectedModifications++;
            }
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_ENABLE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_DISABLE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(PATH_ACTIVATION_ARCHIVE_TIMESTAMP) != null) {
            expectedModifications++;
        }
        PropertyDelta<ActivationStatusType> effectiveStatusDelta = focusDelta.findPropertyDelta(PATH_ACTIVATION_EFFECTIVE_STATUS);
        if (effectiveStatusDelta != null) {
            expectedModifications++;
            PrismAsserts.assertReplace(effectiveStatusDelta, expectedEfficientActivation);
        }
        if (focusDelta.findItemDelta(FocusType.F_ROLE_MEMBERSHIP_REF) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_DELEGATED_REF) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_ITERATION) != null) {
            expectedModifications++;
        }
        if (focusDelta.findItemDelta(FocusType.F_ITERATION_TOKEN) != null) {
            expectedModifications++;
        }
        assertEquals("Unexpected modifications in " + desc + ": " + focusDelta, expectedModifications, focusDelta.getModifications().size());
    }

    private boolean isSideEffectDelta(ItemDelta<?, ?> modification) {
        if (modification.getPath().containsNameExactly(ObjectType.F_METADATA) // TODO remove
                || modification.getPath().containsNameExactly(InfraItemName.METADATA)
                || (modification.getPath().containsNameExactly(FocusType.F_ASSIGNMENT)
                && modification.getPath().containsNameExactly(ActivationType.F_EFFECTIVE_STATUS))) {
            return true;
        } else if (modification.getPath().containsNameExactly(FocusType.F_ASSIGNMENT)
                && modification.getPath().containsNameExactly(AssignmentType.F_ACTIVATION)
                && modification.isReplace() && (modification instanceof ContainerDelta<?>)) {
            Collection<PrismContainerValue<ActivationType>> valuesToReplace =
                    ((ContainerDelta<ActivationType>) modification).getValuesToReplace();
            if (valuesToReplace != null && valuesToReplace.size() == 1) {
                PrismContainerValue<ActivationType> cval = valuesToReplace.iterator().next();
                if (cval.size() == 1) {
                    Item<?, ?> item = cval.getItems().iterator().next();
                    return ActivationType.F_EFFECTIVE_STATUS.equals(item.getElementName());
                }
            }
        }
        return false;
    }

    protected void assertValidFrom(PrismObject<? extends ObjectType> obj, Date expectedDate) {
        assertEquals("Wrong validFrom in " + obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate),
                getActivation(obj).getValidFrom());
    }

    protected void assertValidTo(PrismObject<? extends ObjectType> obj, Date expectedDate) {
        assertEquals("Wrong validTo in " + obj, XmlTypeConverter.createXMLGregorianCalendar(expectedDate),
                getActivation(obj).getValidTo());
    }

    protected ActivationType getActivation(PrismObject<? extends ObjectType> obj) {
        ObjectType objectType = obj.asObjectable();
        ActivationType activation;
        if (objectType instanceof ShadowType) {
            activation = ((ShadowType) objectType).getActivation();
        } else if (objectType instanceof UserType) {
            activation = ((UserType) objectType).getActivation();
        } else {
            throw new IllegalArgumentException("Cannot get activation from " + obj);
        }
        assertNotNull("No activation in " + obj, activation);
        return activation;
    }

    protected CaseType getCase(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(CaseType.class, oid, null, new OperationResult("dummy")).asObjectable();
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

    protected <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = createPlainTask("getObject");
        OperationResult result = task.getResult();
        PrismObject<O> object = modelService.getObject(type, oid, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return object;
    }

    protected <O extends ObjectType> PrismObject<O> getObjectViaRepo(Class<O> type, String oid)
            throws ObjectNotFoundException, SchemaException {
        Task task = createPlainTask("getObjectViaRepo");
        OperationResult result = task.getResult();
        PrismObject<O> object = repositoryService.getObject(type, oid, null, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return object;
    }

    protected void addObjects(File... files)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException, IOException {
        for (File file : files) {
            addObject(file);
        }
    }

    protected <T extends ObjectType> PrismObject<T> addObject(TestObject<T> resource, Task task, OperationResult result)
            throws IOException, ObjectNotFoundException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException,
            SchemaException {
        return addObject(resource, task, result, null);
    }

    // not going through model to avoid conflicts (because the task starts execution during the clockwork operation)
    protected void addTask(File file) throws SchemaException, IOException, ObjectAlreadyExistsException {
        taskManager.addTask(prismContext.parseObject(file), new OperationResult("addTask"));
    }

    // Returns the object as originally parsed, to avoid race conditions regarding last start timestamp.
    protected PrismObject<TaskType> addTask(TestObject<TaskType> resource, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, IOException {
        PrismObject<TaskType> taskBefore = resource.getFresh();
        taskManager.addTask(taskBefore, result);
        return taskBefore;
    }

    protected <O extends ObjectType> String addObject(File file)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        PrismObject<O> object = prismContext.parseObject(file);
        return addObject(object);
    }

    protected <O extends ObjectType> PrismObject<O> addObject(File file, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        return addObject(file, task, result, null);
    }

    protected <O extends ObjectType> PrismObject<O> addObject(File file, Task task, OperationResult result, Consumer<PrismObject<O>> customizer)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        PrismObject<O> object = prismContext.parseObject(file);
        if (customizer != null) {
            customizer.accept(object);
        }
        addObject(object, task, result);
        return object;
    }

    protected <O extends ObjectType> PrismObject<O> addObject(
            TestObject<O> testResource, Task task, OperationResult result, Consumer<PrismObject<O>> customizer)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        PrismObject<O> object = testResource.getFresh();
        if (customizer != null) {
            customizer.accept(object);
        }
        addObject(object, task, result);
        testResource.reload(createSimpleModelObjectResolver(), result); // via model to e.g. complete the resource
        return testResource.get();
    }

    protected <O extends ObjectType> String addObject(PrismObject<O> object)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("addObject");
        OperationResult result = task.getResult();
        String oid = addObject(object, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return oid;
    }

    public <O extends ObjectType> String addObject(PrismObject<O> object, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        return addObject(object, null, task, result);
    }

    /** Not showing the ADD delta on the console. */
    public String addObjectSilently(ObjectType objectBean, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        var object = objectBean.asPrismObject();
        var executedDeltas = modelService.executeChanges(List.of(object.createAddDelta()), null, task, result);
        object.setOid(ObjectDeltaOperation.findAddDeltaOid(executedDeltas, object));
        return object.getOid();
    }

    public String addObject(ObjectType object, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        return addObject(object.asPrismObject(), task, result);
    }

    protected <O extends ObjectType> String addObject(
            PrismObject<O> object, ModelExecuteOptions options, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> addDelta = object.createAddDelta();
        assertFalse("Immutable object provided?", addDelta.getObjectToAdd().isImmutable());
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = executeChanges(addDelta, options, task, result);
        object.setOid(ObjectDeltaOperation.findAddDeltaOid(executedDeltas, object));
        return object.getOid();
    }

    protected <O extends ObjectType> void deleteObject(
            Class<O> type, String oid, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        executeChanges(delta, null, task, result);
    }

    protected <O extends ObjectType> void deleteObjectRaw(
            Class<O> type, String oid, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        executeChanges(delta, executeOptions().raw(), task, result);
    }

    protected <O extends ObjectType> void deleteObject(Class<O> type, String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("deleteObject");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        executeChanges(delta, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected <O extends ObjectType> void deleteObjectRepo(Class<O> type, String oid) throws ObjectNotFoundException {
        OperationResult result = new OperationResult(contextName() + "deleteObjectRepo");
        repositoryService.deleteObject(type, oid, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void forceDeleteShadow(String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("forceDeleteShadow");
        OperationResult result = task.getResult();
        forceDeleteObject(ShadowType.class, oid, task, result);
        assertSuccess(result);
    }

    protected <O extends ObjectType> void forceDeleteObject(
            Class<O> type, String oid, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        ModelExecuteOptions options = ModelExecuteOptions.create().force();
        executeChanges(delta, options, task, result);
    }

    protected void addTrigger(String oid, XMLGregorianCalendar timestamp, String uri)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("addTrigger");
        OperationResult result = task.getResult();
        TriggerType triggerType = new TriggerType();
        triggerType.setTimestamp(timestamp);
        triggerType.setHandlerUri(uri);
        ObjectDelta<ObjectType> delta = prismContext.deltaFactory().object()
                .createModificationAddContainer(ObjectType.class, oid, ObjectType.F_TRIGGER,
                        triggerType);
        executeChanges(delta, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void addTriggers(String oid, Collection<XMLGregorianCalendar> timestamps, String uri, boolean makeDistinct) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("addTriggers");
        OperationResult result = task.getResult();
        Collection<TriggerType> triggers = timestamps.stream()
                .map(ts -> new TriggerType().timestamp(ts).handlerUri(uri))
                .map(ts -> makeDistinct ? addRandomValue(ts) : ts)
                .collect(Collectors.toList());
        ObjectDelta<ObjectType> delta = prismContext.deltaFor(ObjectType.class)
                .item(ObjectType.F_TRIGGER).addRealValues(triggers)
                .asObjectDelta(oid);
        executeChanges(delta, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    private TriggerType addRandomValue(TriggerType trigger) {
        //noinspection unchecked
        @NotNull PrismPropertyDefinition<Long> workItemIdDef =
                prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
        PrismProperty<Long> workItemIdProp = workItemIdDef.instantiate();
        workItemIdProp.addRealValue((long) (Math.random() * 100000000000L));
        try {
            //noinspection unchecked
            trigger.asPrismContainerValue().findOrCreateContainer(TriggerType.F_EXTENSION).add(workItemIdProp);
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        return trigger;
    }

    protected void replaceTriggers(String oid, Collection<XMLGregorianCalendar> timestamps, String uri) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("replaceTriggers");
        OperationResult result = task.getResult();
        Collection<TriggerType> triggers = timestamps.stream()
                .map(ts -> new TriggerType().timestamp(ts).handlerUri(uri))
                .collect(Collectors.toList());
        ObjectDelta<ObjectType> delta = prismContext.deltaFor(ObjectType.class)
                .item(ObjectType.F_TRIGGER).replaceRealValues(triggers)
                .asObjectDelta(oid);
        executeChanges(delta, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected <O extends ObjectType> void assertTrigger(PrismObject<O> object,
            String handlerUri, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        for (TriggerType trigger : object.asObjectable().getTrigger()) {
            if (handlerUri.equals(trigger.getHandlerUri())
                    && MiscUtil.isBetween(trigger.getTimestamp(), start, end)) {
                return;
            }
        }
        AssertJUnit.fail("Expected that " + object + " will have a trigger but it has not");
    }

    protected <O extends ObjectType> void assertTrigger(
            PrismObject<O> object, String handlerUri, XMLGregorianCalendar mid, long tolerance) {
        XMLGregorianCalendar start = XmlTypeConverter.addMillis(mid, -tolerance);
        XMLGregorianCalendar end = XmlTypeConverter.addMillis(mid, tolerance);
        for (TriggerType trigger : object.asObjectable().getTrigger()) {
            if (handlerUri.equals(trigger.getHandlerUri())
                    && MiscUtil.isBetween(trigger.getTimestamp(), start, end)) {
                return;
            }
        }
        AssertJUnit.fail("Expected that " + object + " will have a trigger but it has not");
    }

    protected <O extends ObjectType> void assertNoTrigger(Class<O> type, String oid)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(contextName() + "assertNoTrigger");
        PrismObject<O> object = repositoryService.getObject(type, oid, null, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNoTrigger(object);
    }

    protected <O extends ObjectType> void assertNoTrigger(PrismObject<O> object) {
        List<TriggerType> triggers = object.asObjectable().getTrigger();
        if (triggers != null && !triggers.isEmpty()) {
            AssertJUnit.fail("Expected that " + object + " will have no triggers but it has " + triggers.size() + " trigger: " + triggers);
        }
    }

    public void prepareNotifications() {
        notificationManager.setDisabled(false);
        dummyTransport.clearMessages();
    }

    protected void checkDummyTransportMessages(String name, int expectedCount) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        if (expectedCount == 0) {
            if (messages != null && !messages.isEmpty()) {
                logger.error(messages.size() + " unexpected message(s) recorded in dummy transport '" + name + "'");
                logNotifyMessages(messages);
                printNotifyMessages(messages);
                fail(messages.size() + " unexpected message(s) recorded in dummy transport '" + name + "'");
            }
        } else {
            assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
            if (expectedCount != messages.size()) {
                logger.error("Invalid number of messages recorded in dummy transport '" + name + "', expected: " + expectedCount + ", actual: " + messages.size());
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
            fail("Invalid number of messages recorded in dummy transport '" + name + "', expected: 1, actual: " + messages.size());
        }
        Message message = messages.get(0);
        assertThat(message.getBody()).startsWith(expectedBody); // there can be subscription footer
    }

    protected void assertSingleDummyTransportMessageContaining(String name, String expectedSubstring) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        if (messages.size() != 1) {
            fail("Invalid number of messages recorded in dummy transport '" + name + "', expected: 1, actual: " + messages.size());
        }
        Message message = messages.get(0);
        assertTrue("Notifier " + name + " message body does not contain text: " + expectedSubstring + ", it is:\n" + message.getBody(),
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
        for (Message message : messages) {
            // there can be subscription footer, so we're only interested in the start of the message
            if (message.getBody().startsWith(expectedBody)) {
                return;
            }
        }
        fail("Notifier " + name + " message body " + expectedBody + " not found");
    }

    protected void assertHasDummyTransportMessageContaining(String name, String expectedBodySubstring) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        assertNotNull("No messages recorded in dummy transport '" + name + "'", messages);
        for (Message message : messages) {
            if (message.getBody() != null && message.getBody().contains(expectedBodySubstring)) {
                return;
            }
        }
        fail("Notifier " + name + " message body containing '" + expectedBodySubstring + "' not found");
    }

    protected void displayAllNotifications() {
        for (java.util.Map.Entry<String, List<Message>> entry : dummyTransport.getMessages().entrySet()) {
            List<Message> messages = entry.getValue();
            if (messages != null && !messages.isEmpty()) {
                display("Notification messages: " + entry.getKey(), messages);
            }
        }
    }

    protected void displayNotifications(String name) {
        List<Message> messages = dummyTransport.getMessages("dummy:" + name);
        display("Notification messages: " + name, messages);
    }

    private void logNotifyMessages(List<Message> messages) {
        for (Message message : messages) {
            logger.debug("Notification message:\n{}", message.getBody());
        }
    }

    private void printNotifyMessages(List<Message> messages) {
        for (Message message : messages) {
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

    protected DummyAccount getDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
        try {
            return dummyResource.getAccountByName(username);
        } catch (ConnectException | FileNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected DummyAccount getDummyAccountById(String dummyInstanceName, String id) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
        try {
            return dummyResource.getAccountById(id);
        } catch (ConnectException | FileNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected DummyAccount assertDefaultDummyAccount(String username, String fullname, boolean active) throws SchemaViolationException, ConflictException, InterruptedException {
        return assertDummyAccount(null, username, fullname, active);
    }

    protected DummyAccount assertDummyAccount(String dummyInstanceName, String username, String fullname, Boolean active) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        // display("account", account);
        assertNotNull("No dummy(" + dummyInstanceName + ") account for username " + username, account);
        assertEquals("Wrong fullname for dummy(" + dummyInstanceName + ") account " + username, fullname, account.getAttributeValue("fullname"));
        assertEquals("Wrong activation for dummy(" + dummyInstanceName + ") account " + username, active, account.isEnabled());
        return account;
    }

    protected DummyAccount assertDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy(" + dummyInstanceName + ") account for username " + username, account);
        return account;
    }

    protected DummyAccountAsserter<Void> assertDummyAccountById(String dummyResourceName, String id) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getDummyResourceController(dummyResourceName).assertAccountById(id);
    }

    protected void assertNoDummyAccountById(String dummyInstanceName, String id) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccountById(dummyInstanceName, id);
        assertNull("Dummy(" + dummyInstanceName + ") account for id " + id + " exists while not expecting it", account);
    }

    protected void assertDummyAccountActivation(String dummyInstanceName, String username, Boolean active) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy(" + dummyInstanceName + ") account for username " + username, account);
        assertEquals("Wrong activation for dummy(" + dummyInstanceName + ") account " + username, active, account.isEnabled());
    }

    protected void assertNoDummyAccount(String username) throws SchemaViolationException, ConflictException, InterruptedException {
        assertNoDummyAccount(null, username);
    }

    protected void assertNoDummyAccount(String dummyInstanceName, String username) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNull("Dummy account for username " + username + " exists while not expecting it (" + dummyInstanceName + ")", account);
    }

    protected void assertDefaultDummyAccountAttribute(String username, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyAccountAttribute(null, username, attributeName, expectedAttributeValues);
    }

    protected void assertDummyAccountAttribute(String dummyInstanceName, String username, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy account for username " + username, account);
        Set<Object> values = account.getAttributeValues(attributeName, Object.class);
        if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
            return;
        }
        assertNotNull("No values for attribute " + attributeName + " of " + dummyInstanceName + " dummy account " + username, values);
        assertEquals("Unexpected number of values for attribute " + attributeName + " of " + dummyInstanceName + " dummy account " + username +
                        ". Expected: " + Arrays.toString(expectedAttributeValues) + ", was: " + values,
                expectedAttributeValues.length, values.size());
        for (Object expectedValue : expectedAttributeValues) {
            if (!values.contains(expectedValue)) {
                AssertJUnit.fail("Value '" + expectedValue + "' expected in attribute " + attributeName + " of " + dummyInstanceName + " dummy account " + username +
                        " but not found. Values found: " + values);
            }
        }
    }

    protected void assertNoDummyAccountAttribute(String dummyInstanceName, String username, String attributeName)
            throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy " + dummyInstanceName + " account for username " + username, account);
        Set<Object> values = account.getAttributeValues(attributeName, Object.class);
        if (values == null || values.isEmpty()) {
            return;
        }
        AssertJUnit.fail("Expected no value in attribute " + attributeName + " of " + dummyInstanceName + " dummy account " + username +
                ". Values found: " + values);
    }

    protected void assertDummyAccountAttributeGenerated(String dummyInstanceName, String username)
            throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy account for username " + username, account);
        Integer generated = account.getAttributeValue(DummyAccount.ATTR_INTERNAL_ID, Integer.class);
        if (generated == null) {
            AssertJUnit.fail("No value in generated attribute dir of " + dummyInstanceName + " dummy account " + username);
        }
    }

    protected DummyGroup getDummyGroup(String dummyInstanceName, String name)
            throws SchemaViolationException, ConflictException, InterruptedException {
        DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
        try {
            return dummyResource.getGroupByName(name);
        } catch (ConnectException | FileNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void assertDummyGroup(String username, String description)
            throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyGroup(null, username, description, null);
    }

    protected void assertDummyGroup(String dummyInstanceName, String groupName, String description, Boolean active)
            throws SchemaViolationException, ConflictException, InterruptedException {
        DummyGroup group = getDummyGroup(dummyInstanceName, groupName);
        assertNotNull("No dummy(" + dummyInstanceName + ") group for name " + groupName, group);
        assertEquals("Wrong fullname for dummy(" + dummyInstanceName + ") group " + groupName, description,
                group.getAttributeValue(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION));
        if (active != null) {
            assertEquals("Wrong activation for dummy(" + dummyInstanceName + ") group " + groupName, active, group.isEnabled());
        }
    }

    protected void assertNoDummyGroup(String groupName) throws SchemaViolationException, ConflictException, InterruptedException {
        assertNoDummyGroup(null, groupName);
    }

    protected void assertNoDummyGroup(String dummyInstanceName, String groupName)
            throws SchemaViolationException, ConflictException, InterruptedException {
        DummyGroup group = getDummyGroup(dummyInstanceName, groupName);
        assertNull("Dummy group '" + groupName + "' exists while not expecting it (" + dummyInstanceName + ")", group);
    }

    protected void assertDummyGroupAttribute(String dummyInstanceName, String groupName, String attributeName, Object... expectedAttributeValues) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyGroup group = getDummyGroup(dummyInstanceName, groupName);
        assertNotNull("No dummy group for group name " + groupName, group);
        Set<Object> values = group.getAttributeValues(attributeName, Object.class);
        if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
            return;
        }
        assertNotNull("No values for attribute " + attributeName + " of " + dummyInstanceName + " dummy group " + groupName, values);
        assertEquals("Unexpected number of values for attribute " + attributeName + " of dummy group " + groupName + ": " + values, expectedAttributeValues.length, values.size());
        for (Object expectedValue : expectedAttributeValues) {
            if (!values.contains(expectedValue)) {
                AssertJUnit.fail("Value '" + expectedValue + "' expected in attribute " + attributeName + " of dummy group " + groupName +
                        " but not found. Values found: " + values);
            }
        }
    }

    protected void assertDummyGroupMember(String dummyInstanceName, String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
        DummyGroup group = dummyResource.getGroupByName(dummyGroupName);
        assertNotNull("No dummy group " + dummyGroupName, group);
        IntegrationTestTools.assertGroupMember(group, accountId);
    }

    protected void assertDefaultDummyGroupMember(String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        assertDummyGroupMember(null, dummyGroupName, accountId);
    }

    protected void assertNoDummyGroupMember(String dummyInstanceName, String dummyGroupName, String accountId) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyResource dummyResource = DummyResource.getInstance(dummyInstanceName);
        DummyGroup group = dummyResource.getGroupByName(dummyGroupName);
        IntegrationTestTools.assertNoGroupMember(group, accountId);
    }

    protected void assertNoDefaultDummyGroupMember(String dummyGroupName, String accountId)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        assertNoDummyGroupMember(null, dummyGroupName, accountId);
    }

    protected void assertDummyAccountNoAttribute(String dummyInstanceName, String username, String attributeName) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(dummyInstanceName, username);
        assertNotNull("No dummy account for username " + username, account);
        Set<Object> values = account.getAttributeValues(attributeName, Object.class);
        assertTrue("Unexpected values for attribute " + attributeName + " of dummy account " + username + ": " + values, values == null || values.isEmpty());
    }

    protected Entry assertOpenDjAccount(String uid, String cn, Boolean active) throws DirectoryException {
        Entry entry = openDJController.searchByUid(uid);
        assertNotNull("OpenDJ account with uid " + uid + " not found", entry);
        OpenDJController.assertAttribute(entry, "cn", cn);
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
        TestUtil.assertBetween("User enableTimestamp in " + focus,
                startTime, endTime, userDisableTimestamp);
    }

    protected void assertDisableTimestampFocus(PrismObject<? extends FocusType> focus,
            XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        XMLGregorianCalendar userDisableTimestamp = focus.asObjectable().getActivation().getDisableTimestamp();
        TestUtil.assertBetween("User disableTimestamp in " + focus,
                startTime, endTime, userDisableTimestamp);
    }

    protected void assertEnableTimestampShadow(
            RawRepoShadow shadow, XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        assertEnableTimestampShadow(shadow.getPrismObject(), startTime, endTime);
    }

    protected void assertEnableTimestampShadow(
            PrismObject<? extends ShadowType> shadow, XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        ActivationType activationType = shadow.asObjectable().getActivation();
        assertNotNull("No activation in " + shadow, activationType);
        XMLGregorianCalendar userDisableTimestamp = activationType.getEnableTimestamp();
        TestUtil.assertBetween("Shadow enableTimestamp in " + shadow,
                startTime, endTime, userDisableTimestamp);
    }

    protected void assertDisableTimestampShadow(PrismObject<? extends ShadowType> shadow,
            XMLGregorianCalendar startTime, XMLGregorianCalendar endTime) {
        XMLGregorianCalendar userDisableTimestamp = shadow.asObjectable().getActivation().getDisableTimestamp();
        TestUtil.assertBetween("Shadow disableTimestamp in " + shadow,
                startTime, endTime, userDisableTimestamp);
    }

    protected void assertDisableReasonShadow(PrismObject<? extends ShadowType> shadow, String expectedReason) {
        String disableReason = shadow.asObjectable().getActivation().getDisableReason();
        assertEquals("Wrong shadow disableReason in " + shadow, expectedReason, disableReason);
    }

    protected String getPassword(PrismObject<UserType> user) throws EncryptionException {
        CredentialsType credentialsType = user.asObjectable().getCredentials();
        assertNotNull("No credentials in " + user, credentialsType);
        PasswordType passwordType = credentialsType.getPassword();
        assertNotNull("No password in " + user, passwordType);
        ProtectedStringType protectedStringType = passwordType.getValue();
        assertNotNull("No password value in " + user, protectedStringType);
        return protector.decryptString(protectedStringType);
    }

    protected void assertPassword(PrismObject<UserType> user, String expectedPassword) throws EncryptionException {
        String decryptedUserPassword = getPassword(user);
        assertEquals("Wrong password in " + user, expectedPassword, decryptedUserPassword);
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

    protected void login(String principalName) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = focusProfileService.getPrincipal(principalName, UserType.class);
        login(principal);
    }

    protected void login(TestObject<UserType> testObject) throws CommonException {
        login(testObject.getNameOrig());
    }

    protected void login(PrismObject<UserType> user)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        MidPointPrincipal principal = getMidPointPrincipal(user);
        login(principal);
    }

    /** This should be maybe automatic during "login" method call. But currently it is not. */
    protected void setPrincipalAsTestTaskOwner() throws NotLoggedInException {
        Task testTask = Objects.requireNonNull(getTestTask(), "no test task");
        testTask.setOwnerRef(
                AuthUtil.getPrincipalRefRequired());
    }

    protected MidPointPrincipal getMidPointPrincipal(PrismObject<UserType> user)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        // If we are brave enough, we can use getTestOperationResult here -- later.
        return focusProfileService.getPrincipal(user, ProfileCompilerOptions.create(), new OperationResult(OPERATION_GET_PRINCIPAL));
    }

    protected void login(MidPointPrincipal principal) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        securityContext.setAuthentication(createMpAuthentication(authentication));
    }

    protected Authentication createMpAuthentication(Authentication authentication) {
        MidpointAuthentication mpAuthentication = new MidpointAuthentication(SecurityPolicyUtil.createDefaultSequence());
        ModuleAuthentication moduleAuthentication = new ModuleAuthentication() {
            @Override
            public String getModuleIdentifier() {
                return SecurityPolicyUtil.DEFAULT_MODULE_IDENTIFIER;
            }

            @Override
            public String getModuleTypeName() {
                return AuthenticationModuleNameConstants.LOGIN_FORM;
            }

            @Override
            public AuthenticationModuleState getState() {
                return AuthenticationModuleState.SUCCESSFULLY;
            }

            @Override
            public void setState(AuthenticationModuleState state) {
            }

            @Override
            public Authentication getAuthentication() {
                return authentication;
            }

            @Override
            public void setAuthentication(Authentication authentication) {
            }

            @Override
            public boolean applicable() {
                return true;
            }

            @Override
            public boolean isSufficient() {
                return true;
            }

            @Override
            public void setSufficient(boolean sufficient) {

            }

            @Override
            public void setFailureData(AutheticationFailedData autheticationFailedData) {

            }

            @Override
            public AutheticationFailedData getFailureData() {
                return null;
            }

            @Override
            public void recordFailure(AuthenticationException exception) {

            }

            @Override
            public void setFocusType(QName focusType) {

            }

            @Override
            public String getPrefix() {
                return ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH
                        + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_FOR_DEFAULT_MODULE + SecurityPolicyUtil.DEFAULT_MODULE_IDENTIFIER + "/";
            }

            @Override
            public QName getFocusType() {
                return null;
            }

            @Override
            public AuthenticationSequenceModuleNecessityType getNecessity() {
                return AuthenticationSequenceModuleNecessityType.SUFFICIENT;
            }

            @Override
            public Integer getOrder() {
                return 10;
            }

            @Override
            public DisplayType getDisplay() {
                return new DisplayType();
            }

            @Override
            public GuiActionType getAction() {
                return new GuiActionType();
            }

            @Override
            public ModuleAuthentication clone() {
                return null;
                //TODO
            }
        };
        mpAuthentication.addAuthentication(moduleAuthentication);
        AuthModule authModule = new AuthModule() {
            @Override
            public ModuleAuthentication getBaseModuleAuthentication() {
                return moduleAuthentication;
            }

            @Override
            public String getModuleIdentifier() {
                return SecurityPolicyUtil.DEFAULT_MODULE_IDENTIFIER;
            }

            @Override
            public Integer getOrder() {
                return 1;
            }

            @Override
            public List<AuthenticationProvider> getAuthenticationProviders() {
                //TODO
                return null;
            }

            @Override
            public SecurityFilterChain getSecurityFilterChain() {
                return null;
            }
        };
        mpAuthentication.setAuthModules(Collections.singletonList(authModule));
        mpAuthentication.setPrincipal(authentication.getPrincipal());
        return mpAuthentication;
    }

    protected void loginSuperUser(String principalName)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = focusProfileService.getPrincipal(principalName, UserType.class);
        loginSuperUser(principal);
    }

    protected void loginSuperUser(PrismObject<UserType> user)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        loginSuperUser(
                getMidPointPrincipal(user));
    }

    protected void loginSuperUser(MidPointPrincipal principal) {
        principal.addExtraAuthorizationIfMissing(SecurityUtil.createPrivilegedAuthorization(), true);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);
        securityContext.setAuthentication(createMpAuthentication(authentication));
    }

    protected void loginAnonymous() {
        Authentication authentication = new AnonymousAuthenticationToken("foo",
                AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(createMpAuthentication(authentication));
    }

    protected void assertLoggedInUsername(@NotNull String username) {
        MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
        FocusType user = midPointPrincipal.getFocus();
        assertEquals("Wrong logged-in user", username, user.getName().getOrig());
    }

    protected void assertLoggedInUserOid(@NotNull String userOid) {
        MidPointPrincipal midPointPrincipal = getSecurityContextPrincipal();
        assertPrincipalUserOid(midPointPrincipal, userOid);
    }

    protected void assertPrincipalUserOid(
            @NotNull MidPointPrincipal principal, @NotNull String userOid) {
        FocusType user = principal.getFocus();
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
            return (MidPointPrincipal) principal;
        } else {
            AssertJUnit.fail("Unknown principal in the spring security context: " + principal);
            return null; // not reached
        }
    }

    protected PrismObject<? extends FocusType> getSecurityContextPrincipalFocus() {
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
            FocusType focusType = ((MidPointPrincipal) principal).getFocus();
            return focusType.asPrismObject();
        } else {
            return null;
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
        FocusType attorney = principal.getAttorney();
        if (attorney == null) {
            if (attotrneyOid == null) {
                return;
            } else {
                AssertJUnit.fail("Expected attorney " + attotrneyOid + " in principal " + principal + " but there was none");
            }
        }
        assertEquals("Wrong attroney OID in principal", attotrneyOid, attorney.getOid());
    }

    private Collection<Authorization> getSecurityContextAuthorizations() {
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
        for (int i = 0; i < expectedModelActions.length; i++) {
            expectedActions[i] = expectedModelActions[i].getUrl();
        }
        assertAuthorizationActions("Wrong authorizations in security context", securityContextAuthorizations, expectedActions);
    }

    protected void assertSecurityContextNoAuthorizationActions() {
        Collection<Authorization> securityContextAuthorizations = getSecurityContextAuthorizations();
        if (securityContextAuthorizations != null && !securityContextAuthorizations.isEmpty()) {
            fail("Unexpected authorizations in security context: " + securityContextAuthorizations);
        }
    }

    protected void displayAllUsers() throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        displayAllUsers(null);
    }

    protected void displayAllUsersFull() throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        displayAllUsers(createRetrieveCollection());
    }

    private void displayAllUsers(Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Task task = createPlainTask("displayAllUsers");
        OperationResult result = task.getResult();
        ResultHandler<UserType> handler = (object, parentResult) -> {
            display("User", object);
            return true;
        };
        modelService.searchObjectsIterative(UserType.class, null, handler, options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    /**
     * Returns appropriate object synchronization settings for the class.
     * Assumes single sync setting for now.
     */
    protected ObjectSynchronizationType determineSynchronization(
            ResourceType resource, QName focusTypeName, String name) {
        List<ObjectSynchronizationType> objectSynchronizations = ResourceTypeUtil.getAllSynchronizationBeans(resource);
        if (objectSynchronizations.isEmpty()) {
            return null;
        }
        for (ObjectSynchronizationType objSyncBean : objectSynchronizations) {
            QName configuredFocusTypeName = Objects.requireNonNullElse(objSyncBean.getFocusType(), UserType.COMPLEX_TYPE);
            if (QNameUtil.match(configuredFocusTypeName, focusTypeName)
                    && (name == null || name.equals(objSyncBean.getName()))) {
                return objSyncBean;
            }
        }
        throw new IllegalArgumentException(
                "Synchronization setting for " + focusTypeName + " and name " + name + " not found in " + resource);
    }

    protected void assertShadowKindIntent(String shadowOid, ShadowKindType expectedKind,
            String expectedIntent) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".assertShadowKindIntent");
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

    @Override
    protected void customizeTask(Task task) {
        PrismObject<UserType> defaultActor = getDefaultActor();
        if (defaultActor != null) {
            task.setOwner(defaultActor);
        }
        task.setChannel(DEFAULT_CHANNEL);
    }

    protected Task createTask(String operationName, MidPointPrincipal principal) {
        Task task = createPlainTask(operationName);
        task.setOwner(principal.getFocus().asPrismObject());
        task.setChannel(DEFAULT_CHANNEL);
        return task;
    }

    protected Task createTask(MidPointPrincipal principal) {
        Task task = createPlainTask();
        task.setOwner(principal.getFocus().asPrismObject());
        task.setChannel(DEFAULT_CHANNEL);
        return task;
    }

    protected void modifyRoleAddConstruction(String roleOid, long inducementId, String resourceOid) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createTask("modifyRoleAddConstruction");
        OperationResult result = task.getResult();
        ConstructionType construction = new ConstructionType();
        ObjectReferenceType resourceRedRef = new ObjectReferenceType();
        resourceRedRef.setOid(resourceOid);
        construction.setResourceRef(resourceRedRef);
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(RoleType.class, roleOid,
                        ItemPath.create(RoleType.F_INDUCEMENT, inducementId, AssignmentType.F_CONSTRUCTION),
                        construction);
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void modifyRoleAddInducementTarget(String roleOid, String targetOid, ModelExecuteOptions defaultOptions)
            throws CommonException {
        modifyRoleAddInducementTarget(roleOid, targetOid, false, defaultOptions);
    }

    protected void modifyRoleAddInducementTargetAndRecomputeMembers(
            String roleOid, String targetOid, ModelExecuteOptions defaultOptions)
            throws CommonException {
        modifyRoleAddInducementTarget(roleOid, targetOid, true, defaultOptions);
    }

    protected void modifyRoleAddInducementTarget(String roleOid, String targetOid, boolean recomputeMembers,
            ModelExecuteOptions defaultOptions)
            throws CommonException {
        OperationResult result = createSubresult("modifyRoleAddInducementTarget");
        AssignmentType inducement = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(targetOid);
        inducement.setTargetRef(targetRef);
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(RoleType.class, roleOid,
                        RoleType.F_INDUCEMENT, inducement);

        ModelExecuteOptions options = setRecomputeMembers(defaultOptions, recomputeMembers);
        executeChanges(roleDelta, options, getTestTask(), result);

        result.computeStatus();
        if (recomputeMembers) {
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
            roleDelta = prismContext.deltaFactory().object()
                    .createModificationAddContainer(RoleType.class, roleOid, RoleType.F_ASSIGNMENT,
                            assignment);
        } else {
            roleDelta = prismContext.deltaFactory().object()
                    .createModificationDeleteContainer(RoleType.class, roleOid, RoleType.F_ASSIGNMENT,
                            assignment);
        }
        executeChanges(roleDelta, null, task, result);
    }

    protected void modifyRoleDeleteAssignment(String roleOid, AssignmentType assignment, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, roleOid, RoleType.F_ASSIGNMENT,
                        assignment);
        executeChanges(roleDelta, null, task, result);
    }

    protected PolicyRuleType createExclusionPolicyRule(String excludedRoleOid) {
        PolicyRuleType policyRule = new PolicyRuleType();
        PolicyConstraintsType policyConstraints = new PolicyConstraintsType();
        ExclusionPolicyConstraintType exclusionConstraint = new ExclusionPolicyConstraintType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(excludedRoleOid);
        targetRef.setType(RoleType.COMPLEX_TYPE);
        exclusionConstraint.setTargetRef(targetRef);
        policyConstraints.getExclusion().add(exclusionConstraint);
        policyRule.setPolicyConstraints(policyConstraints);
        return policyRule;
    }

    protected PolicyRuleType createMinAssigneePolicyRule(int minAssignees) {
        PolicyRuleType policyRule = new PolicyRuleType();
        PolicyConstraintsType policyConstraints = new PolicyConstraintsType();
        MultiplicityPolicyConstraintType minAssigneeConstraint = new MultiplicityPolicyConstraintType();
        minAssigneeConstraint.setMultiplicity(Integer.toString(minAssignees));
        policyConstraints.getMinAssignees().add(minAssigneeConstraint);
        policyRule.setPolicyConstraints(policyConstraints);
        return policyRule;
    }

    protected PolicyRuleType createMaxAssigneePolicyRule(int maxAssignees) {
        PolicyRuleType policyRule = new PolicyRuleType();
        PolicyConstraintsType policyConstraints = new PolicyConstraintsType();
        MultiplicityPolicyConstraintType maxAssigneeConstraint = new MultiplicityPolicyConstraintType();
        maxAssigneeConstraint.setMultiplicity(Integer.toString(maxAssignees));
        policyConstraints.getMaxAssignees().add(maxAssigneeConstraint);
        policyRule.setPolicyConstraints(policyConstraints);
        return policyRule;
    }

    protected AssignmentType createAssignmentIdOnly(long id) {
        AssignmentType assignment = new AssignmentType();
        assignment.asPrismContainerValue().setId(id);
        return assignment;
    }

    protected void modifyRoleAddPolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(RoleType.class, roleOid, RoleType.F_POLICY_EXCEPTION,
                        policyException);
        executeChanges(roleDelta, null, task, result);
    }

    protected void modifyRoleDeletePolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, roleOid, RoleType.F_POLICY_EXCEPTION,
                        policyException);
        executeChanges(roleDelta, null, task, result);
    }

    protected void modifyRoleReplacePolicyException(String roleOid, PolicyExceptionType policyException, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationReplaceContainer(RoleType.class, roleOid, RoleType.F_POLICY_EXCEPTION,
                        policyException);
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

    protected Optional<AssignmentType> findAssignmentByResource(PrismObject<? extends FocusType> focus, String resourceOid) {
        return focus.asObjectable().getAssignment().stream()
                .filter(a -> {
                    ConstructionType construction = a.getConstruction();
                    return construction != null && resourceOid.equals(getOid(construction.getResourceRef()));
                }).findFirst();
    }

    protected AssignmentType findAssignmentByResourceRequired(PrismObject<? extends FocusType> focus, String resourceOid) {
        return findAssignmentByResource(focus, resourceOid)
                .orElseThrow(() -> new IllegalStateException("No assignment to " + resourceOid + " in " + focus));
    }

    protected AssignmentType findInducementByTarget(String roleOid, String targetOid)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = createTask("findInducementByTarget");
        OperationResult result = task.getResult();
        PrismObject<RoleType> role = modelService.getObject(RoleType.class, roleOid, null, task, result);
        for (AssignmentType inducement : role.asObjectable().getInducement()) {
            ObjectReferenceType targetRef = inducement.getTargetRef();
            if (targetRef != null && targetOid.equals(targetRef.getOid())) {
                return inducement;
            }
        }
        return null;
    }

    protected void modifyRoleDeleteInducementTarget(String roleOid, String targetOid,
            ModelExecuteOptions options) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createTask("modifyRoleDeleteInducementTarget");
        OperationResult result = task.getResult();
        AssignmentType inducement = findInducementByTarget(roleOid, targetOid);
        ObjectDelta<RoleType> roleDelta =
                prismContext.deltaFactory().object().createModificationDeleteContainer(
                        RoleType.class, roleOid, RoleType.F_INDUCEMENT,
                        inducement.asPrismContainerValue().clone());
        modelService.executeChanges(MiscSchemaUtil.createCollection(roleDelta), options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void modifyRoleDeleteInducement(String roleOid, long inducementId, ModelExecuteOptions options, Task task)
            throws CommonException {
        modifyRoleDeleteInducement(roleOid, inducementId, false, options, task);
    }

    protected void modifyRoleDeleteInducementAndRecomputeMembers(String roleOid, long inducementId,
            ModelExecuteOptions options, Task task) throws CommonException {
        modifyRoleDeleteInducement(roleOid, inducementId, true, options, task);
    }

    protected void modifyRoleDeleteInducement(String roleOid, long inducementId, boolean recomputeMembers,
            ModelExecuteOptions defaultOptions, Task task) throws CommonException {
        OperationResult result = task.getResult();

        AssignmentType inducement = new AssignmentType();
        inducement.setId(inducementId);
        ObjectDelta<RoleType> roleDelta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(RoleType.class, roleOid,
                        RoleType.F_INDUCEMENT, inducement);

        ModelExecuteOptions options = setRecomputeMembers(defaultOptions, recomputeMembers);

        executeChanges(roleDelta, options, task, result);
        result.computeStatus();
        if (recomputeMembers) {
            TestUtil.assertInProgressOrSuccess(result);
        } else {
            TestUtil.assertSuccess(result);
        }
    }

    private ModelExecuteOptions setRecomputeMembers(ModelExecuteOptions defaultOptions, boolean recomputeMembers)
            throws SchemaException {
        if (recomputeMembers) {
            return nullToEmpty(defaultOptions)
                    .setExtensionPropertyRealValues(PrismContext.get(), RECOMPUTE_MEMBERS_NAME, true);
        } else {
            return defaultOptions;
        }
    }

    private @NotNull ModelExecuteOptions nullToEmpty(ModelExecuteOptions options) {
        return options != null ? options : executeOptions();
    }

    protected void modifyUserAddAccount(String userOid, File accountFile, Task task, OperationResult result)
            throws SchemaException, IOException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        modelService.executeChanges(
                List.of(createAddAccountDelta(userOid, accountFile)), null, task, result);
    }

    protected @NotNull ObjectDelta<UserType> createAddAccountDelta(String userOid, File accountFile)
            throws SchemaException, IOException {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(accountFile);
        return createAddAccountDelta(userOid, account);
    }

    protected @NotNull ObjectDelta<UserType> createAddAccountDelta(String userOid, PrismObject<ShadowType> account)
            throws SchemaException {
        return prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .add(ObjectTypeUtil.createObjectRefWithFullObject(account))
                .asObjectDelta(userOid);
    }

    protected @NotNull ObjectDelta<UserType> createDeleteAccountDelta(String userOid, PrismObject<ShadowType> account)
            throws SchemaException {
        return prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .delete(ObjectTypeUtil.createObjectRefWithFullObject(account))
                .asObjectDelta(userOid);
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
            assertTrue("AuthorizationEvaluator.isAuthorized: Principal " + principal + " NOT authorized for action " + action,
                    securityEnforcer.isAuthorized(
                            action, phase, AuthorizationParameters.EMPTY, SecurityEnforcer.Options.create(), task, result));
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
        boolean isAuthorized = securityEnforcer.isAuthorized(
                action, phase, AuthorizationParameters.EMPTY, SecurityEnforcer.Options.create(), task, result);
        SecurityContextHolder.setContext(origContext);
        assertFalse("AuthorizationEvaluator.isAuthorized: Principal " + principal + " IS authorized for action " + action + " (" + phase + ") but he should not be", isAuthorized);
    }

    protected void assertAuthorizations(PrismObject<UserType> user, String... expectedAuthorizations)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = getMidPointPrincipal(user);
        assertNotNull("No principal for " + user, principal);
        assertAuthorizations(principal, expectedAuthorizations);
    }

    protected void assertAuthorizations(MidPointPrincipal principal, String... expectedAuthorizations) {
        List<String> actualAuthorizations = new ArrayList<>();
        for (Authorization authorization : principal.getAuthorities()) {
            actualAuthorizations.addAll(authorization.getAction());
        }
        PrismAsserts.assertSets("Wrong authorizations in " + principal, actualAuthorizations, expectedAuthorizations);
    }

    protected void assertNoAuthorizations(PrismObject<UserType> user)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        MidPointPrincipal principal = getMidPointPrincipal(user);
        assertNotNull("No principal for " + user, principal);
        assertNoAuthorizations(principal);
    }

    protected void assertNoAuthorizations(MidPointPrincipal principal) {
        if (!principal.getAuthorities().isEmpty()) {
            AssertJUnit.fail("Unexpected authorizations in " + principal + ": " + principal.getAuthorities());
        }
    }

    protected CompiledGuiProfileAsserter<Void> assertCompiledGuiProfile(MidPointPrincipal principal) {
        if (!(principal instanceof GuiProfiledPrincipal)) {
            fail("Expected GuiProfiledPrincipal, but got " + principal.getClass());
        }
        CompiledGuiProfile compiledGuiProfile = ((GuiProfiledPrincipal) principal).getCompiledGuiProfile();
        CompiledGuiProfileAsserter<Void> asserter = new CompiledGuiProfileAsserter<>(compiledGuiProfile, null, "in principal " + principal);
        initializeAsserter(asserter);
        return asserter;
    }

    protected CompiledGuiProfileAsserter<Void> assertCompiledGuiProfile(CompiledGuiProfile compiledGuiProfile) {
        CompiledGuiProfileAsserter<Void> asserter = new CompiledGuiProfileAsserter<>(compiledGuiProfile, null, null);
        initializeAsserter(asserter);
        return asserter;
    }

    protected EvaluatedPolicyRulesAsserter<Void> assertEvaluatedPolicyRules(Collection<EvaluatedPolicyRule> evaluatedPolicyRules, PrismObject<?> sourceObject) {
        EvaluatedPolicyRulesAsserter<Void> asserter = new EvaluatedPolicyRulesAsserter<>(evaluatedPolicyRules, null, sourceObject.toString());
        initializeAsserter(asserter);
        return asserter;
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
        Task task = createPlainTask("getEditObjectDefinition");
        OperationResult result = task.getResult();
        PrismObjectDefinition<O> editSchema = modelInteractionService.getEditObjectDefinition(object, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return editSchema;
    }

    protected <H extends AssignmentHolderType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<H> targetHolder) throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
        return getAssignableRoleSpecification(targetHolder, 0);
    }

    protected <H extends AssignmentHolderType> RoleSelectionSpecification getAssignableRoleSpecification(
            PrismObject<H> focus, int assignmentOrder)
            throws ObjectNotFoundException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException {
        return getAssignableRoleSpecification(focus, AbstractRoleType.class, assignmentOrder);
    }

    protected <H extends AssignmentHolderType, R extends AbstractRoleType> RoleSelectionSpecification getAssignableRoleSpecification(
            PrismObject<H> focus, Class<R> targetType, int assignmentOrder)
            throws ObjectNotFoundException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException {
        Task task = createPlainTask("getAssignableRoleSpecification");
        OperationResult result = task.getResult();
        RoleSelectionSpecification spec = modelInteractionService.getAssignableRoleSpecification(
                focus, targetType, assignmentOrder, task, result);
        assertSuccess(result);
        return spec;
    }

    protected <H extends AssignmentHolderType> RoleSelectionSpecificationAsserter<Void> assertAssignableRoleSpecification(PrismObject<H> targetHolder) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        RoleSelectionSpecification spec = getAssignableRoleSpecification(targetHolder);
        RoleSelectionSpecificationAsserter<Void> asserter = new RoleSelectionSpecificationAsserter<>(spec, null, "for holder " + targetHolder);
        initializeAsserter(asserter);
        return asserter;
    }

    protected <H extends AssignmentHolderType> RoleSelectionSpecificationAsserter<Void> assertAssignableRoleSpecification(
            PrismObject<H> targetHolder, Class<? extends AbstractRoleType> roleType, int order)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        RoleSelectionSpecification spec = getAssignableRoleSpecification(targetHolder, roleType, order);
        RoleSelectionSpecificationAsserter<Void> asserter = new RoleSelectionSpecificationAsserter<>(spec, null, "for holder " + targetHolder + ", role type " + roleType.getSimpleName() + ",  order " + order);
        initializeAsserter(asserter);
        return asserter;
    }

    protected void assertAllowRequestAssignmentItems(
            String userOid, String targetRoleOid, ItemPath... expectedAllowedItemPaths)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
        Task task = createTask("assertAllowRequestAssignmentItems");
        OperationResult result = task.getResult();
        assertAllowRequestAssignmentItems(userOid, targetRoleOid, task, result, expectedAllowedItemPaths);
        assertSuccess(result);
    }

    protected void assertAllowRequestAssignmentItems(String userOid, String targetRoleOid,
            Task task, OperationResult result, ItemPath... expectedAllowedItemPaths)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ObjectNotFoundException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = getUser(userOid);
        PrismObject<RoleType> target = getRole(targetRoleOid);

        ItemSecurityConstraints constraints = modelInteractionService.getAllowedRequestAssignmentItems(user, target, task, result);
        displayDumpable("Request decisions for " + target, constraints);

        for (ItemPath expectedAllowedItemPath : expectedAllowedItemPaths) {
            AuthorizationDecisionType decision = constraints.findItemDecision(expectedAllowedItemPath);
            assertEquals("Wrong decision for item " + expectedAllowedItemPath, AuthorizationDecisionType.ALLOW, decision);
        }
    }

    protected void assertPasswordMetadata(PrismObject<UserType> user, ItemName credentialType, boolean create,
            XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
        var credentials = user.asObjectable().getCredentials();
        assertThat(credentials).isNotNull();
        //noinspection unchecked
        PrismContainer<AbstractCredentialType> credentialContainer = credentials.asPrismContainerValue().findContainer(credentialType);
        assertThat(credentialContainer).isNotNull();
        var credential = credentialContainer.getRealValue(AbstractCredentialType.class);
        assertMetadata("password metadata in " + user, ValueMetadataTypeUtil.getMetadata(credential), create,
                false, start, end, actorOid, channel);
    }

    protected void assertDummyPassword(String instance, String userId, String expectedClearPassword) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(instance, userId);
        assertNotNull("No dummy account " + userId, account);
        assertEquals("Wrong password in dummy '" + instance + "' account " + userId, expectedClearPassword, account.getPassword());
    }

    protected void assertDummyPasswordNotEmpty(String instance, String userId) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(instance, userId);
        assertNotNull("No dummy account " + userId, account);
        String actualPassword = account.getPassword();
        if (actualPassword == null || actualPassword.isEmpty()) {
            fail("Empty password in dummy '" + instance + "' account " + userId);
        }
    }

    protected void reconcileUser(String oid, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, PolicyViolationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        reconcileUser(oid, null, task, result);
    }

    protected void reconcileUser(String oid, ModelExecuteOptions options, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta<UserType> emptyDelta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, oid);
        modelService.executeChanges(
                MiscSchemaUtil.createCollection(emptyDelta), ModelExecuteOptions.create(options).reconcile(), task, result);
    }

    protected void reconcileOrg(String oid, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta<OrgType> emptyDelta = prismContext.deltaFactory().object().createEmptyModifyDelta(OrgType.class, oid);
        modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), executeOptions().reconcile(), task, result);
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
        assertEquals("Wrong executionState in " + task, TaskExecutionStateType.CLOSED, task.asObjectable().getExecutionState());
    }

    protected void assertTaskClosed(Task task) {
        assertEquals("Wrong executionState in " + task, TaskExecutionStateType.CLOSED, task.getExecutionState());
    }

    protected List<AuditEventRecordType> getAllAuditRecords(Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .asc(F_TIMESTAMP)
                        .build(),
                null, task, result);
    }

    protected SearchResultList<AuditEventRecordType> getAuditRecords(
            int maxRecords, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .asc(F_TIMESTAMP)
                        .maxSize(maxRecords)
                        .build(),
                null, task, result);
    }

    protected @NotNull SearchResultList<AuditEventRecordType> getObjectAuditRecords(String oid)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Task task = createTask("getObjectAuditRecords");
        OperationResult result = task.getResult();
        return modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_TARGET_REF).ref(oid)
                        .asc(F_TIMESTAMP)
                        .build(),
                null, task, result);
    }

    protected List<AuditEventRecordType> getAuditRecordsFromTo(
            XMLGregorianCalendar from, XMLGregorianCalendar to, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(F_TIMESTAMP).ge(from)
                        .and()
                        .item(F_TIMESTAMP).le(to)
                        .asc(AuditEventRecordType.F_PARAMETER)
                        .build(),
                null, task, result);
    }

    protected SearchResultList<AuditEventRecordType> getAuditRecordsAfterId(
            long afterId, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        return modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID).gt(afterId)
                        .asc(AuditEventRecordType.F_REPO_ID)
                        .build(),
                null, task, result);
    }

    protected long getAuditRecordsMaxId(Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<AuditEventRecordType> latestEvent = modelAuditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .desc(AuditEventRecordType.F_REPO_ID)
                        .maxSize(1)
                        .build(),
                null, task, result);
        return latestEvent.size() == 1 ? latestEvent.get(0).getRepoId() : 0;
    }

    protected <F extends FocusType> void assertFocusModificationSanity(ModelContext<F> context) {
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
                        if (!property.contains((PrismValue) valueToDelete, EquivalenceStrategy.REAL_VALUE)) {
                            display("Deleted value " + valueToDelete + " is not in focus item " + itemMod.getParentPath() + "/" + itemMod.getElementName());
                            displayValue("Deleted value", valueToDelete);
                            display("HASHCODE: " + valueToDelete.hashCode());
                            for (Object value : property.getValues()) {
                                displayValue("Existing value", value);
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

    protected void assertNoPostponedOperation(PrismObject<ShadowType> shadow) {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        if (!pendingOperations.isEmpty()) {
            AssertJUnit.fail("Expected no pending operations in " + shadow + ", but found one: " + pendingOperations);
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

    protected <O extends ObjectType> String addAndRecompute(TestObject<O> testObject, Task task, OperationResult result)
            throws Exception {
        PrismObject<O> object = repoAdd(testObject, result);
        modelService.recompute(object.asObjectable().getClass(), object.getOid(), null, task, result);
        testObject.reload(createSimpleModelObjectResolver(), result);
        return testObject.get().getOid();
    }

    protected void assertAuditReferenceValue(AuditEventRecord event, String refName, String oid, QName type, String name) {
        Set<AuditReferenceValue> values = event.getReferenceValues(refName);
        assertEquals("Wrong # of reference values of '" + refName + "'", 1, values.size());
        AuditReferenceValue value = values.iterator().next();
        assertEquals("Wrong OID", oid, value.getOid());
        assertEquals("Wrong type", type, value.getType());
        assertEquals("Wrong name", name, PolyString.getOrig(value.getTargetName()));
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

    // Use this when you want to start the task manually.
    protected void reimportRecurringWithNoSchedule(String taskOid, File taskFile, Task opTask, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException,
            PolicyViolationException {
        taskManager.suspendAndDeleteTasks(Collections.singletonList(taskOid), 60000L, true, result);
        addObject(taskFile, opTask, result, taskObject ->
                ((TaskType) taskObject.asObjectable())
                        .schedule(new ScheduleType().recurrence(TaskRecurrenceType.RECURRING))
                        .binding(TaskBindingType.LOOSE)); // tightly-bound tasks must have interval set
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

    protected <F extends FocusType, P extends FocusType> PrismObject<P> assertLinkedPersona(
            PrismObject<F> focus, Class<P> personaClass, String archetypeOid)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("assertLinkedPersona");
        for (ObjectReferenceType personaRef : focus.asObjectable().getPersonaRef()) {
            PrismObject<P> persona = repositoryService.getObject(
                    ObjectTypes.getObjectTypeFromTypeQName(personaRef.getType()).getClassDefinition(),
                    personaRef.getOid(), null, result);
            if (isTypeAndArchetype(persona, personaClass, archetypeOid)) {
                return persona;
            }
        }
        fail("No persona " + personaClass.getSimpleName() + "/" + archetypeOid + " in " + focus);
        return null; // not reached
    }

    protected <F extends FocusType> boolean isTypeAndArchetype(PrismObject<F> focus, Class<F> expectedType, String archetypeOid) {
        if (!expectedType.isAssignableFrom(focus.asObjectable().getClass())) {
            return false;
        }
        return archetypeOid != null && ObjectTypeUtil.hasArchetypeRef(focus, archetypeOid);
    }

    protected PrismObject<OrgType> getOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
        assertNotNull("The org " + orgName + " is missing!", org);
        display("Org " + orgName, org);
        PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PolyString.fromOrig(orgName));
        return org;
    }

    protected void dumpOrgTree() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        displayValue("Org tree", dumpOrgTree(getTopOrgOid()));
    }

    protected void dumpOrgTreeAndUsers() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        displayValue("Org tree", dumpOrgTree(getTopOrgOid(), true));
    }

    protected String getTopOrgOid() {
        return null;
    }

    protected void transplantGlobalPolicyRulesAdd(File configWithGlobalRulesFile, Task task, OperationResult parentResult) throws SchemaException, IOException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        // copy rules from the file into live system config object
        PrismObject<SystemConfigurationType> rules = prismContext.parserFor(configWithGlobalRulesFile).parse();
        ObjectDelta<SystemConfigurationType> delta = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_GLOBAL_POLICY_RULE).add(
                        rules.asObjectable().getGlobalPolicyRule().stream()
                                .map(r -> r.clone().asPrismContainerValue())
                                .collect(Collectors.toList()))
                .asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, parentResult);
    }

    protected void displayProvisioningScripts() {
        displayProvisioningScripts(null);
    }

    protected void displayProvisioningScripts(String dummyName) {
        display("Provisioning scripts " + dummyName, getDummyResource(dummyName).getScriptHistory());
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
            AssertJUnit.fail(getDummyResource().getScriptHistory().size() + " provisioning scripts were executed while not expected any");
        }
    }

    protected void assertDummyProvisioningScriptsNone() {
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());
    }

    protected void applyPasswordPolicy(String passwordPolicyOid, String securityPolicyOid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        if (passwordPolicyOid == null) {
            modifyObjectReplaceReference(SecurityPolicyType.class, securityPolicyOid, PATH_CREDENTIALS_PASSWORD_VALUE_POLICY_REF,
                    task, result /* no value */);
        } else {
            PrismReferenceValue passPolicyRef = itemFactory().createReferenceValue(passwordPolicyOid, ValuePolicyType.COMPLEX_TYPE);
            modifyObjectReplaceReference(SecurityPolicyType.class, securityPolicyOid, PATH_CREDENTIALS_PASSWORD_VALUE_POLICY_REF,
                    task, result, passPolicyRef);
        }
    }

    protected void assertPasswordCompliesWithPolicy(
            PrismObject<UserType> user, String passwordPolicyOid)
            throws EncryptionException, ObjectNotFoundException, ExpressionEvaluationException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = createTask("assertPasswordCompliesWithPolicy");
        OperationResult result = task.getResult();
        assertPasswordCompliesWithPolicy(user, passwordPolicyOid, task, result);
        assertSuccess(result);
    }

    protected void assertPasswordCompliesWithPolicy(PrismObject<UserType> user, String passwordPolicyOid, Task task, OperationResult result) throws EncryptionException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        String password = getPassword(user);
        displayValue("Password of " + user, password);
        PrismObject<ValuePolicyType> passwordPolicy = repositoryService.getObject(ValuePolicyType.class, passwordPolicyOid, null, result);
        var results = valuePolicyProcessor.validateValue(
                password, passwordPolicy.asObjectable(), createUserOriginResolver(user), "validating password of " + user, task, result);
        boolean valid = result.isAcceptable();
        if (!valid) {
            fail("Password for " + user + " does not comply with password policy: " + extractMessages(results));
        }
    }

    protected FocusValuePolicyOriginResolver<UserType> createUserOriginResolver(PrismObject<UserType> user) {
        if (user != null) {
            return new FocusValuePolicyOriginResolver<>(user, modelObjectResolver);
        } else {
            return null;
        }
    }

    protected List<PrismObject<CaseType>> getCasesForObject(String oid, QName type,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_OBJECT_REF).ref(itemFactory().createReferenceValue(oid, type))
                .build();
        return modelService.searchObjects(CaseType.class, query, options, task, result);
    }

    protected CaseType getApprovalCase(List<PrismObject<CaseType>> cases) {
        List<CaseType> rv = cases.stream()
                .filter(o -> ObjectTypeUtil.hasArchetypeRef(o, SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value()))
                .map(o -> o.asObjectable())
                .collect(Collectors.toList());
        if (rv.isEmpty()) {
            throw new AssertionError("No approval case found");
        } else if (rv.size() > 1) {
            throw new AssertionError("More than one approval case found: " + rv);
        } else {
            return rv.get(0);
        }
    }

    protected CaseType getRootCase(List<PrismObject<CaseType>> cases) {
        List<CaseType> rv = cases.stream()
                .map(o -> o.asObjectable())
                .filter(c -> c.getParentRef() == null)
                .collect(Collectors.toList());
        if (rv.isEmpty()) {
            throw new AssertionError("No root case found");
        } else if (rv.size() > 1) {
            throw new AssertionError("More than one root case found: " + rv);
        } else {
            return rv.get(0);
        }
    }

    @SuppressWarnings("unused")     // maybe in the future :)
    protected Consumer<Task> createShowTaskTreeConsumer(long period) {
        AtomicLong lastTimeShown = new AtomicLong(0);
        return task -> {
            try {
                if (lastTimeShown.get() + period < System.currentTimeMillis()) {
                    dumpTaskTree(task.getOid(), createOperationResult());
                    lastTimeShown.set(System.currentTimeMillis());
                }
            } catch (CommonException e) {
                throw new AssertionError(e);
            }
        };
    }

    protected <T> T runPrivileged(Producer<T> producer) {
        return securityContextManager.runPrivileged(producer);
    }

    protected void assertPendingOperationDeltas(PrismObject<ShadowType> shadow, int expectedNumber) {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        assertEquals("Wrong number of pending operations in " + shadow, expectedNumber, pendingOperations.size());
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
        assertNotNull("No delta in pending operation in " + shadow, deltaType);
        // TODO: check content of pending operations in the shadow

        TestUtil.assertBetween("Request timestamp in pending operation in " + shadow, requestStart, requestEnd, pendingOperation.getRequestTimestamp());

        PendingOperationExecutionStatusType executiontStatus = pendingOperation.getExecutionStatus();
        assertEquals("Wrong execution status in pending operation in " + shadow, expectedExecutionStatus, executiontStatus);

        OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
        assertEquals("Wrong result status in pending operation in " + shadow, expectedResultStatus, resultStatus);

        // TODO: assert other timestamps

        if (expectedExecutionStatus == PendingOperationExecutionStatusType.COMPLETED) {
            TestUtil.assertBetween("Completion timestamp in pending operation in " + shadow, completionStart, completionEnd, pendingOperation.getCompletionTimestamp());
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
        for (PendingOperationType pendingOperation : pendingOperations) {
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
            assertNotNull("No delta in pending operation in " + shadow, delta);
            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                ItemPath deltaPath = itemDelta.getPath().getItemPath();
                if (itemPath.equivalent(deltaPath)) {
                    return pendingOperation;
                }
            }
        }
        return null;
    }

    protected UserAsserter<Void> assertUserAfter(PrismObject<UserType> user)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertUser(user, "after")
                .display();
    }

    protected UserAsserter<Void> assertUserAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertUser(oid, "after")
                .display();
    }

    protected UserAsserter<Void> assertUserAfterByUsername(String username) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertUserByUsername(username, "after")
                .display();
    }

    protected UserAsserter<Void> assertUserBefore(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertUser(oid, "before")
                .display();
    }

    protected UserAsserter<Void> assertUserBeforeByUsername(String username) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertUserByUsername(username, "before")
                .display();
    }

    protected UserAsserter<Void> assertUser(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = getUser(oid);
        return assertUser(user, message)
                .assertOid(oid);
    }

    protected <O extends ObjectType> PrismObjectAsserter<O, Void> assertObject(Class<O> clazz, String oid, String message) throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<O> object = modelService.getObject(clazz, oid, null, getTestTask(), getTestOperationResult());
        return assertObject(object, message)
                .assertOid();
    }

    protected <O extends ObjectType> PrismObjectAsserter<O, Void> assertObject(PrismObject<O> object, String message) {
        return PrismObjectAsserter.forObject(object, message);
    }

    protected RepoOpAsserter createRepoOpAsserter() {
        PerformanceInformation repoPerformanceInformation =
                repositoryService.getPerformanceMonitor().getThreadLocalPerformanceInformation();
        return new RepoOpAsserter(repoPerformanceInformation, getTestNameShort());
    }

    protected <F extends FocusType> FocusAsserter<F, Void> assertFocus(PrismObject<F> focus, String message) {
        FocusAsserter<F, Void> asserter = FocusAsserter.forFocus(focus, message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected UserAsserter<Void> assertUser(UserType user, String message) {
        return assertUser(user.asPrismObject(), message);
    }

    protected UserAsserter<Void> assertUser(PrismObject<UserType> user, String message) {
        UserAsserter<Void> asserter = UserAsserter.forUser(user, message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected <A extends AbstractAsserter<?>> A initializeAsserter(A asserter) {
        super.initializeAsserter(asserter);
        asserter.setExpectedActor(
                asObjectable(getDefaultActor()));
        return asserter;
    }

    protected void assertNoUserByUsername(String username)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        assertThat(findUserByUsername(username)).as("user named '" + username + "'").isNull();
    }

    protected UserAsserter<Void> assertUserByUsername(String username, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = findUserByUsername(username);
        assertNotNull("User with username '" + username + "' was not found", user);
        UserAsserter<Void> asserter = UserAsserter.forUser(user, message);
        initializeAsserter(asserter);
        asserter.assertName(username);
        return asserter;
    }

    protected OrgAsserter<Void> assertOrg(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<OrgType> org = getObject(OrgType.class, oid);
        OrgAsserter<Void> asserter = assertOrg(org, message);
        asserter.assertOid(oid);
        return asserter;
    }

    protected OrgAsserter<Void> assertOrg(PrismObject<OrgType> org, String message) {
        OrgAsserter<Void> asserter = OrgAsserter.forOrg(org, message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected OrgAsserter<Void> assertOrgAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertOrg(oid, "after")
                .display();
    }

    protected OrgAsserter<Void> assertOrgByName(String name, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<OrgType> org = findObjectByName(OrgType.class, name);
        assertNotNull("No org with name '" + name + "'", org);
        OrgAsserter<Void> asserter = OrgAsserter.forOrg(org, message);
        initializeAsserter(asserter);
        asserter.assertName(name);
        return asserter;
    }

    protected RoleAsserter<Void> assertRole(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<RoleType> role = getObject(RoleType.class, oid);
        RoleAsserter<Void> asserter = assertRole(role, message);
        asserter.assertOid(oid);
        return asserter;
    }

    protected RoleAsserter<Void> assertRoleByName(String name, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<RoleType> role = findObjectByName(RoleType.class, name);
        assertNotNull("No role with name '" + name + "'", role);
        RoleAsserter<Void> asserter = assertRole(role, message);
        asserter.assertName(name);
        return asserter;
    }

    protected RoleAsserter<Void> assertRole(PrismObject<RoleType> role, String message) {
        RoleAsserter<Void> asserter = RoleAsserter.forRole(role, message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected RoleAsserter<Void> assertRoleBefore(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertRole(oid, "before")
                .display();
    }

    protected RoleAsserter<Void> assertRoleAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertRole(oid, "after")
                .display();
    }

    protected RoleAsserter<Void> assertRoleAfterByName(String name) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertRoleByName(name, "after")
                .display();
    }

    protected FocusAsserter<ServiceType, Void> assertService(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ServiceType> service = getObject(ServiceType.class, oid);
        // TODO: change to ServiceAsserter later
        FocusAsserter<ServiceType, Void> asserter = FocusAsserter.forFocus(service, message);
        initializeAsserter(asserter);
        asserter.assertOid(oid);
        return asserter;
    }

    protected FocusAsserter<ServiceType, Void> assertServiceByName(String name, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ServiceType> service = findServiceByName(name);
        // TODO: change to ServiceAsserter later
        FocusAsserter<ServiceType, Void> asserter = FocusAsserter.forFocus(service, message);
        initializeAsserter(asserter);
        asserter.assertName(name);
        return asserter;
    }

    protected FocusAsserter<ServiceType, Void> assertServiceAfterByName(String name) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertServiceByName(name, "after");
    }

    protected FocusAsserter<ServiceType, Void> assertServiceAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertService(oid, "after")
                .display();
    }

    protected void assertNoServiceByName(String name) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ServiceType> service = findServiceByName(name);
        if (service != null) {
            display("Unexpected service", service);
            fail("Unexpected " + service);
        }
    }

    protected AccCertCampaignAsserter<Void> assertCampaignFull(String oid, String message) throws CommonException {
        var campaign = getObject(
                AccessCertificationCampaignType.class,
                oid,
                getOperationOptionsBuilder()
                        .retrieve()
                        .build());
        AccCertCampaignAsserter<Void> asserter = AccCertCampaignAsserter.forCampaign(campaign, message);
        initializeAsserter(asserter);
        asserter.assertOid(oid);
        return asserter;
    }

    protected AccCertCampaignAsserter<Void> assertCampaignFullAfter(String oid) throws CommonException {
        return assertCampaignFull(oid, "after")
                .display();
    }

    protected CaseAsserter<Void> assertCase(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<CaseType> acase = getObject(CaseType.class, oid);
        CaseAsserter<Void> asserter = CaseAsserter.forCase(acase, message);
        initializeAsserter(asserter);
        asserter.assertOid(oid);
        return asserter;
    }

    protected CaseAsserter<Void> assertCase(CaseType aCase, String message) {
        CaseAsserter<Void> asserter = CaseAsserter.forCase(aCase.asPrismObject(), message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected CaseAsserter<Void> assertCaseAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertCase(oid, "after")
                .display();
    }

    protected CaseAsserter<Void> assertCaseAfter(CaseType aCase) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertCase(aCase, "after")
                .display();
    }

    protected ShadowAsserter<Void> assertModelShadow(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = getShadowModel(oid);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(repoShadow, "model");
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertModelShadowNoFetch(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = getShadowModelNoFetch(oid);
        return ShadowAsserter.forShadow(repoShadow, "model(noFetch)").display();
    }

    protected ShadowAsserter<Void> assertModelShadowFuture(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = getShadowModelFuture(oid);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(repoShadow, "model(future)");
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertModelShadowFutureNoFetch(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        GetOperationOptions options = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
        options.setNoFetch(true);
        PrismObject<ShadowType> repoShadow = getShadowModel(oid, options, true);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(repoShadow, "model(future,noFetch)");
        asserter.display();
        return asserter;
    }

    protected ResourceAsserter<Void> assertResource(String oid, String message) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ResourceType> resource = getObject(ResourceType.class, oid);
        return assertResource(resource, message);
    }

    @Override
    protected ResourceAsserter<Void> assertResource(PrismObject<ResourceType> resource, String message) {
        ResourceAsserter<Void> asserter = ResourceAsserter.forResource(resource, message);
        initializeAsserter(asserter);
        return asserter;
    }

    protected ResourceAsserter<Void> assertResourceAfter(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertResource(oid, "after")
                .display();
    }

    protected ResourceAsserter<Void> assertResourceAfter(PrismObject<ResourceType> resource) {
        return assertResource(resource, "after")
                .display();
    }

    protected ResourceAsserter<Void> assertResourceAfter(ResourceType resource) {
        return assertResource(resource, "after")
                .display();
    }

    protected ResourceAsserter<Void> assertResourceBefore(String oid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return assertResource(oid, "before")
                .display();
    }

    // Change to PrismObjectDefinitionAsserter later
    protected <O extends ObjectType> PrismContainerDefinitionAsserter<O, Void> assertObjectDefinition(PrismObjectDefinition<O> objectDef) {
        return assertContainerDefinition(objectDef);
    }

    protected <C extends Containerable> PrismContainerDefinitionAsserter<C, Void> assertContainerDefinition(PrismContainerDefinition<C> containerDef) {
        PrismContainerDefinitionAsserter<C, Void> asserter = PrismContainerDefinitionAsserter.forContainerDefinition(containerDef);
        initializeAsserter(asserter);
        return asserter;
    }

    protected <O extends ObjectType> ModelContextAsserter<O, Void> assertPreviewContext(ModelContext<O> previewContext) {
        return assertModelContext(previewContext, "preview context");
    }

    protected <O extends ObjectType> ModelContextAsserter<O, Void> assertModelContext(ModelContext<O> modelContext, String desc) {
        ModelContextAsserter<O, Void> asserter = ModelContextAsserter.forContext(modelContext, desc);
        initializeAsserter(asserter);
        return asserter;
    }

    // SECURITY

    protected <O extends ObjectType> void assertGetDeny(Class<O> type, String oid) throws CommonException {
        assertGetDeny(type, oid, null);
    }

    protected <O extends ObjectType> void assertGetDeny(
            Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws CommonException {
        Task task = createPlainTask("assertGetDeny");
        OperationResult result = task.getResult();
        try {
            logAttempt("get", type, oid, null);
            PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
            failDeny("get", type, oid, null);
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny("get", type, oid, null);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    protected <O extends ObjectType> PrismObject<O> assertGetAllow(Class<O> type, String oid) throws CommonException {
        assertGetAllow(type, oid, createReadOnly());
        return assertGetAllow(type, oid, null);
    }

    protected <O extends ObjectType> PrismObject<O> assertGetAllow(
            Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws CommonException {
        Task task = createPlainTask("assertGetAllow");
        OperationResult result = task.getResult();
        logAttempt("get", type, oid, null);
        PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        logAllow("get", type, oid, null);
        return object;
    }

    protected void assertCompleteAndDelegateAllow(@NotNull WorkItemId id) throws CommonException {
        assertCompleteAllow(id);
        assertDelegateAllow(id);
    }

    protected void assertCompleteAndDelegateAllow(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertCompleteAllow(id);
        assertDelegateAllow(id);
    }

    private void assertCompleteAllow(@NotNull WorkItemId id) throws CommonException {
        assertActionAllow(id, ModelAuthorizationAction.COMPLETE_WORK_ITEM);
    }

    protected void assertCompleteAllow(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertActionAllow(id, ModelAuthorizationAction.COMPLETE_WORK_ITEM);
    }

    private void assertDelegateAllow(@NotNull WorkItemId id) throws CommonException {
        assertActionAllow(id, ModelAuthorizationAction.DELEGATE_WORK_ITEM);
    }

    protected void assertDelegateAllow(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertActionAllow(id, ModelAuthorizationAction.DELEGATE_WORK_ITEM);
    }

    private void assertActionAllow(WorkItemId id, ModelAuthorizationAction modelAuthorizationAction) throws CommonException {
        Task task = createPlainTask("assertActionAllow");
        OperationResult result = task.getResult();
        var workItem = repositoryService.getWorkItem(id, null, result); // using repo to avoid checking read authorizations
        attemptWorkItemAction(workItem, id, modelAuthorizationAction, task, result);
        result.recomputeStatus();
        assertSuccess(result);
        logAllow(modelAuthorizationAction.name(), CaseType.class, id.caseOid, id.asItemPath());
    }

    private void attemptWorkItemAction(
            CaseWorkItemType workItem, WorkItemId id, ModelAuthorizationAction modelAuthorizationAction,
            Task task, OperationResult result) throws CommonException {
        logAttempt(modelAuthorizationAction.name(), CaseType.class, id.caseOid, id.asItemPath());
        securityEnforcer.authorize(
                modelAuthorizationAction.getUrl(),
                null,
                ValueAuthorizationParameters.of(workItem),
                task, result);
    }

    private void assertActionAllow(AccessCertificationWorkItemId id, ModelAuthorizationAction modelAuthorizationAction) throws CommonException {
        Task task = createPlainTask("assertActionAllow");
        OperationResult result = task.getResult();
        var workItem = repositoryService.getAccessCertificationWorkItem(id, null, result); // using repo to avoid checking read authorizations
        attemptWorkItemAction(workItem, id, modelAuthorizationAction, task, result);
        result.recomputeStatus();
        assertSuccess(result);
        logAllow(modelAuthorizationAction.name(), AccessCertificationCampaignType.class, id.campaignOid(), id.asItemPath());
    }

    private void attemptWorkItemAction(
            AccessCertificationWorkItemType workItem, AccessCertificationWorkItemId id,
            ModelAuthorizationAction modelAuthorizationAction,
            Task task, OperationResult result) throws CommonException {
        logAttempt(modelAuthorizationAction.name(), AccessCertificationCampaignType.class, id.campaignOid(), id.asItemPath());
        securityEnforcer.authorize(
                modelAuthorizationAction.getUrl(),
                null,
                ValueAuthorizationParameters.of(workItem),
                task, result);
    }

    protected void assertCompleteAndDelegateDeny(@NotNull WorkItemId id) throws CommonException {
        assertCompleteDeny(id);
        assertDelegateDeny(id);
    }

    protected void assertCompleteAndDelegateDeny(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertCompleteDeny(id);
        assertDelegateDeny(id);
    }

    private void assertCompleteDeny(@NotNull WorkItemId id) throws CommonException {
        assertActionDeny(id, ModelAuthorizationAction.COMPLETE_WORK_ITEM);
    }

    protected void assertCompleteDeny(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertActionDeny(id, ModelAuthorizationAction.COMPLETE_WORK_ITEM);
    }

    private void assertDelegateDeny(@NotNull WorkItemId id) throws CommonException {
        assertActionDeny(id, ModelAuthorizationAction.DELEGATE_WORK_ITEM);
    }

    protected void assertDelegateDeny(@NotNull AccessCertificationWorkItemId id) throws CommonException {
        assertActionDeny(id, ModelAuthorizationAction.DELEGATE_WORK_ITEM);
    }

    private void assertActionDeny(WorkItemId id, ModelAuthorizationAction modelAuthorizationAction) throws CommonException {
        Task task = createPlainTask("assertActionDeny");
        OperationResult result = task.getResult();
        var workItem = repositoryService.getWorkItem(id, null, result); // using repo to avoid checking read authorizations
        try {
            attemptWorkItemAction(workItem, id, modelAuthorizationAction, task, result);
            failDeny(modelAuthorizationAction.name(), CaseType.class, id.caseOid, id.asItemPath());
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny(modelAuthorizationAction.name(), CaseType.class, id.caseOid, id.asItemPath());
            result.recomputeStatus();
            assertFailure(result);
        }
    }

    private void assertActionDeny(AccessCertificationWorkItemId id, ModelAuthorizationAction modelAuthorizationAction)
            throws CommonException {
        Task task = createPlainTask("assertActionDeny");
        OperationResult result = task.getResult();
        // using repo to avoid checking read authorizations
        var workItem = repositoryService.getAccessCertificationWorkItem(id, null, result);
        try {
            attemptWorkItemAction(workItem, id, modelAuthorizationAction, task, result);
            failDeny(modelAuthorizationAction.name(), AccessCertificationCampaignType.class, id.campaignOid(), id.asItemPath());
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny(modelAuthorizationAction.name(), AccessCertificationCampaignType.class, id.campaignOid(), id.asItemPath());
            result.recomputeStatus();
            assertFailure(result);
        }
    }

    protected <O extends ObjectType> void assertSearchFilter(Class<O> type, ObjectFilter filter, int expectedResults) throws Exception {
        assertSearch(type, prismContext.queryFactory().createQuery(filter), null, expectedResults);
    }

    protected <O extends ObjectType> SearchResultList<PrismObject<O>> assertSearch(Class<O> type, ObjectQuery query, int expectedResults) throws Exception {
        assertSearch(type, query, null, expectedResults);
        return assertSearch(type, query, createReadOnly(), expectedResults);
    }

    protected void assertVisibleUsers(int expectedNumAllUsers) throws Exception {
        assertSearch(UserType.class, null, expectedNumAllUsers);
    }

    protected void assertSearchCases(String... expectedOids) throws Exception {
        assertSearch(CaseType.class, null, expectedOids);
    }

    protected void assertSearchCampaigns(String... expectedOids) throws Exception {
        assertSearch(AccessCertificationCampaignType.class, null, expectedOids);
    }

    protected <C extends Containerable> List<C> assertContainerSearch(Class<C> type, ObjectQuery query, int expectedResults)
            throws CommonException {
        return assertContainerSearch(type, query, null, expectedResults);
    }

    protected <C extends Containerable>
    List<C> assertContainerSearch(Class<C> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        try {
            logAttempt("searchContainers", type, query);
            List<C> objects = modelService.searchContainers(type, query, options, task, result);
            displayValue("Search returned", objects.toString());
            if (objects.size() > expectedResults) {
                failDeny("search", type, query, expectedResults, objects.size());
            } else if (objects.size() < expectedResults) {
                failAllow("search", type, query, expectedResults, objects.size());
            }
            result.computeStatus();
            TestUtil.assertSuccess(result);
            return objects;
        } catch (SecurityViolationException e) {
            // this should not happen
            result.computeStatus();
            TestUtil.assertFailure(result);
            failAllow("search", type, query, e);
            throw new NotHereAssertionError();
        }
    }

    protected List<AccessCertificationCaseType> assertSearchCertCases(int expectedNumber) throws CommonException {
        return assertContainerSearch(AccessCertificationCaseType.class, null, expectedNumber);
    }

    protected List<AccessCertificationCaseType> assertSearchCertCases(ObjectQuery query, int expectedNumber) throws CommonException {
        return assertContainerSearch(AccessCertificationCaseType.class, query, expectedNumber);
    }

    protected ObjectQuery queryForCertCasesByCampaignOwner(String ownerOid) {
        return queryFor(AccessCertificationCaseType.class)
                .item(PrismConstants.T_PARENT, AccessCertificationCampaignType.F_OWNER_REF).ref(ownerOid)
                .build();
    }

    protected ObjectQuery queryForCertCasesByStageNumber(int number) {
        return queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_STAGE_NUMBER).eq(number)
                .build();
    }

    protected ObjectQuery queryForCertCasesByWorkItemOutcome(String outcomeUri) {
        return queryFor(AccessCertificationCaseType.class)
                .exists(AccessCertificationCaseType.F_WORK_ITEM)
                .block()
                .item(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME).eq(outcomeUri)
                .endBlock()
                .build();
    }

    protected void assertCertCasesSearch(AccessCertificationCaseId... ids) throws CommonException {
        var cases = assertSearchCertCases(ids.length);
        var realIds = AccessCertificationCaseId.of(cases);
        assertThat(realIds).as("certification cases IDs").containsExactlyInAnyOrder(ids);
    }

    protected void assertSearchCertWorkItems(ObjectQuery query, int expectedResults) throws CommonException {
        assertContainerSearch(AccessCertificationWorkItemType.class, query, expectedResults);
    }

    protected void assertSearchCertWorkItems(AccessCertificationWorkItemId... ids) throws CommonException {
        assertSearchCertWorkItems(null, ids);
    }

    protected void assertSearchCertWorkItems(ObjectQuery query, AccessCertificationWorkItemId... ids) throws CommonException {
        var workItems = assertContainerSearch(AccessCertificationWorkItemType.class, query, ids.length);
        var realIds = AccessCertificationWorkItemId.of(workItems);
        assertThat(realIds).as("certification work items IDs").containsExactlyInAnyOrder(ids);
    }

    protected void assertSearchCases(int expectedNumber) throws Exception {
        assertSearch(CaseType.class, null, expectedNumber);
    }

    protected List<CaseWorkItemType> assertSearchWorkItems(int expectedNumber) throws CommonException {
        return assertContainerSearch(CaseWorkItemType.class, null, expectedNumber);
    }

    protected List<CaseWorkItemType> assertSearchWorkItems(WorkItemId... expectedIdentifiers) throws CommonException {
        var workItems = assertContainerSearch(CaseWorkItemType.class, null, expectedIdentifiers.length);
        var identifiers = workItems.stream()
                .map(wi -> WorkItemId.of(wi))
                .toList();
        assertThat(identifiers).as("work item identifiers").containsExactlyInAnyOrder(expectedIdentifiers);
        return workItems;
    }

    @NotNull
    private Collection<SelectorOptions<GetOperationOptions>> createReadOnly() {
        return schemaService.getOperationOptionsBuilder()
                .readOnly()
                .build();
    }

    protected <O extends ObjectType> void assertSearchRaw(Class<O> type, ObjectQuery query, int expectedResults) throws Exception {
        assertSearch(type, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), expectedResults);
    }

    protected <O extends ObjectType> void assertSearchDeny(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
        try {
            assertSearch(type, query, options, 0);
        } catch (SecurityViolationException e) {
            // This is expected. The search should either return zero results or throw an exception.
            logDeny("search");
        }
    }

    protected <O extends ObjectType> SearchResultList<PrismObject<O>> assertSearch(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws Exception {
        ObjectQuery originalQuery = query != null ? query.clone() : null;
        return assertSearch(type, query, options,
                new SearchAssertion<>() {

                    @Override
                    public void assertObjects(String message, List<PrismObject<O>> objects) throws Exception {
                        if (objects.size() > expectedResults) {
                            failDeny(message, type, originalQuery, expectedResults, objects.size());
                        } else if (objects.size() < expectedResults) {
                            failAllow(message, type, originalQuery, expectedResults, objects.size());
                        }
                    }

                    @Override
                    public void assertCount(int count) throws Exception {
                        if (count > expectedResults) {
                            failDeny("count", type, originalQuery, expectedResults, count);
                        } else if (count < expectedResults) {
                            failAllow("count", type, originalQuery, expectedResults, count);
                        }
                    }

                });
    }

    protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, String... expectedOids) throws Exception {
        assertSearch(type, query, null, expectedOids);
    }

    protected <O extends ObjectType> void assertSearchFilter(Class<O> type, ObjectFilter filter, String... expectedOids) throws Exception {
        assertSearch(type, prismContext.queryFactory().createQuery(filter), expectedOids);
    }

    protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, String... expectedOids) throws Exception {
        assertSearch(type, query, options,
                new SearchAssertion<>() {

                    @Override
                    public void assertObjects(String message, List<PrismObject<O>> objects) throws Exception {
                        if (!MiscUtil.unorderedCollectionEquals(objects, Arrays.asList(expectedOids),
                                (object, expectedOid) -> expectedOid.equals(object.getOid()))) {
                            failAllow(message, type, (query == null ? "null" : query.toString()) + ", expected " + Arrays.toString(expectedOids) + ", actual " + objects, null);
                        }
                    }

                    @Override
                    public void assertCount(int count) throws Exception {
                        if (count != expectedOids.length) {
                            failAllow("count", type, query, expectedOids.length, count);
                        }
                    }

                });
    }

    protected <O extends ObjectType> SearchResultList<PrismObject<O>> assertSearch(Class<O> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, SearchAssertion<O> assertion) throws Exception {
        Task task = createPlainTask("assertSearchObjects");
        OperationResult result = task.getResult();
        SearchResultList<PrismObject<O>> objects;
        try {
            logAttempt("search", type, query);
            objects = modelService.searchObjects(type, query, options, task, result);
            displayValue("Search returned", objects.toString());
            assertion.assertObjects("search", objects);
            assertSuccess(result);
        } catch (SecurityViolationException e) {
            // this should not happen
            result.computeStatus();
            TestUtil.assertFailure(result);
            failAllow("search", type, query, e);
            return null; // not reached
        }

        task = createPlainTask("assertSearchObjectsIterative");
        result = task.getResult();
        try {
            logAttempt("searchIterative", type, query);
            final List<PrismObject<O>> iterativeObjects = new ArrayList<>();
            ResultHandler<O> handler = (object, parentResult) -> {
                iterativeObjects.add(object);
                return true;
            };
            modelService.searchObjectsIterative(type, query, handler, options, task, result);
            displayValue("Search iterative returned", iterativeObjects.toString());
            assertion.assertObjects("searchIterative", iterativeObjects);
            assertSuccess(result);
        } catch (SecurityViolationException e) {
            // this should not happen
            result.computeStatus();
            TestUtil.assertFailure(result);
            failAllow("searchIterative", type, query, e);
            return null; // not reached
        }

        task = createPlainTask("assertSearchObjects.count");
        result = task.getResult();
        try {
            logAttempt("count", type, query);
            int numObjects = modelService.countObjects(type, query, options, task, result);
            displayValue("Count returned", numObjects);
            assertion.assertCount(numObjects);
            assertSuccess(result);
        } catch (SecurityViolationException e) {
            // this should not happen
            result.computeStatus();
            TestUtil.assertFailure(result);
            failAllow("search", type, query, e);
            return null; // not reached
        }

        return objects;
    }

    protected void assertAddDeny(TestObject<? extends ObjectType> testObject) throws CommonException, IOException {
        assertAddDeny(testObject, null);
    }

    protected void assertAddDenyRaw(TestObject<? extends ObjectType> testObject) throws CommonException, IOException {
        assertAddDeny(testObject, executeOptions().raw());
    }

    protected <O extends ObjectType> void assertAddDeny(TestObject<O> testObject, ModelExecuteOptions options)
            throws CommonException, IOException {
        assertAddDeny(
                testObject.get(),
                options);
    }

    protected <O extends ObjectType> void assertAddDeny(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
        PrismObject<O> object = PrismTestUtil.parseObject(file);
        assertAddDeny(object, options);
    }

    protected <O extends ObjectType> void assertAddDeny(PrismObject<O> object, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        Task task = createPlainTask("assertAddDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> addDelta = object.createAddDelta();
        try {
            logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
            modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
            failDeny("add", object.getCompileTimeClass(), object.getOid(), null);
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny("add", object.getCompileTimeClass(), object.getOid(), null);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    protected void assertAddAllow(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        assertAddAllow(file, null);
    }

    protected void assertAddAllow(TestObject<? extends ObjectType> testObject) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        assertAddAllow(testObject, null);
    }

    protected OperationResult assertAddAllowTracing(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        return assertAddAllowTracing(file, null);
    }

    protected <O extends ObjectType> void assertAddAllow(File file, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        PrismObject<O> object = PrismTestUtil.parseObject(file);
        assertAddAllow(object, options);
    }

    protected <O extends ObjectType> void assertAddAllow(TestObject<O> testObject, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        assertAddAllow(
                testObject.get(),
                options);
    }

    protected <O extends ObjectType> OperationResult assertAddAllowTracing(File file, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        PrismObject<O> object = PrismTestUtil.parseObject(file);
        return assertAddAllowTracing(object, options);
    }

    protected <O extends ObjectType> OperationResult assertAddAllow(PrismObject<O> object, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createAllowDenyTask("assertAddAllow");
        return assertAddAllow(object, options, task);
    }

    protected <O extends ObjectType> OperationResult assertAddAllowTracing(PrismObject<O> object, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createAllowDenyTask(contextName() + ".assertAddAllow");
        setTracing(task, createDefaultTracingProfile());
        return assertAddAllow(object, options, task);
    }

    protected <O extends ObjectType> OperationResult assertAddAllow(PrismObject<O> object, ModelExecuteOptions options, Task task)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        OperationResult result = task.getResult();
        ObjectDelta<O> addDelta = object.createAddDelta();
        logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
        try {
            modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
        } catch (SecurityViolationException e) {
            failAllow("add", object.getCompileTimeClass(), object.getOid(), null, e);
        }
        result.computeStatus();
        TestUtil.assertSuccess(result);
        logAllow("add", object.getCompileTimeClass(), object.getOid(), null);
        return result;
    }

    protected <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid)
            throws ObjectAlreadyExistsException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        assertDeleteDeny(type, oid, null);
    }

    protected <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        Task task = createPlainTask("assertDeleteDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        try {
            logAttempt("delete", type, oid, null);
            modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
            failDeny("delete", type, oid, null);
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny("delete", type, oid, null);
            result.computeStatus();
            TestUtil.assertFailure(result);
        } catch (ObjectNotFoundException e) {
            // MID-3221
            // still consider OK ... for now
            logError("delete", type, oid, null);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    protected <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        assertDeleteAllow(type, oid, null);
    }

    protected <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = createPlainTask("assertDeleteAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        logAttempt("delete", type, oid, null);
        try {
            modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
        } catch (SecurityViolationException e) {
            failAllow("delete", type, oid, null, e);
        }
        result.computeStatus();
        TestUtil.assertSuccess(result);
        logAllow("delete", type, oid, null);
    }

    private Task createAllowDenyTask(String opname) {
        Task task = createTask("createAllowDenyTask." + opname);
        task.setOwner(getSecurityContextPrincipalFocus());
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        return task;
    }

    protected OperationResult assertAllow(String opname, Attempt attempt) throws Exception {
        Task task = createAllowDenyTask(opname);
        return assertAllow(opname, attempt, task);
    }

    protected OperationResult assertAllowTracing(String opname, Attempt attempt) throws Exception {
        Task task = createAllowDenyTask(opname);
        setTracing(task, createDefaultTracingProfile());
        return assertAllow(opname, attempt, task);
    }

    protected OperationResult assertAllow(String opname, Attempt attempt, Task task) throws Exception {
        OperationResult result = task.getResult();
        try {
            logAttempt(opname);
            attempt.run(task, result);
        } catch (SecurityViolationException e) {
            failAllow(opname, e);
        }
        result.computeStatus();
        TestUtil.assertSuccess(result);
        logAllow(opname);
        return result;
    }

    protected OperationResult assertDeny(String opname, Attempt attempt) throws Exception {
        Task task = createAllowDenyTask(opname);
        OperationResult result = task.getResult();
        try {
            logAttempt(opname);
            attempt.run(task, result);
            failDeny(opname);
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny(opname);
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
        return result;
    }

    protected void asAdministrator(Attempt attempt) throws Exception {
        Task task = createPlainTask("asAdministrator");
        OperationResult result = task.getResult();
        MidPointPrincipal origPrincipal = getSecurityContextPrincipal();
        login(USER_ADMINISTRATOR_USERNAME);
        task.setOwner(getSecurityContextPrincipalFocus());
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        try {
            attempt.run(task, result);
        } catch (Throwable e) {
            login(origPrincipal);
            throw e;
        }
        login(origPrincipal);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void logAttempt(String action) {
        String msg = LOG_PREFIX_ATTEMPT + "Trying " + action;
        System.out.println(msg);
        logger.info(msg);
    }

    protected <O extends ObjectType> void logDeny(String action, Class<O> type, String oid, ItemPath itemPath) {
        logDeny(action, type, oid + " prop " + itemPath);
    }

    protected <O extends ObjectType> void logDeny(String action, Class<O> type, String desc) {
        String msg = LOG_PREFIX_DENY + "Denied " + action + " of " + type.getSimpleName() + ":" + desc;
        System.out.println(msg);
        logger.info(msg);
    }

    protected void logDeny(String action) {
        String msg = LOG_PREFIX_DENY + "Denied " + action;
        System.out.println(msg);
        logger.info(msg);
    }

    protected <O extends ObjectType> void logAllow(String action, Class<O> type, String oid, ItemPath itemPath) {
        logAllow(action, type, oid + " prop " + itemPath);
    }

    protected <O extends ObjectType> void logAllow(String action, Class<O> type, String desc) {
        String msg = LOG_PREFIX_ALLOW + "Allowed " + action + " of " + type.getSimpleName() + ":" + desc;
        System.out.println(msg);
        logger.info(msg);
    }

    protected void logAllow(String action) {
        String msg = LOG_PREFIX_ALLOW + "Allowed " + action;
        System.out.println(msg);
        logger.info(msg);
    }

    protected <O extends ObjectType> void logError(String action, Class<O> type, String desc, Throwable e) {
        String msg = LOG_PREFIX_DENY + "Error " + action + " of " + type.getSimpleName() + ":" + desc + "(" + e + ")";
        System.out.println(msg);
        logger.info(msg);
    }

    protected void logAttempt(String action, Class<?> type, ObjectQuery query) {
        logAttempt(action, type, query == null ? "null" : query.toString());
    }

    protected void logAttempt(String action, Class<?> type, String oid, ItemPath itemPath) {
        logAttempt(action, type, oid + " prop " + itemPath);
    }

    protected void logAttempt(String action, Class<?> type, String desc) {
        String msg = LOG_PREFIX_ATTEMPT + "Trying " + action + " of " + type.getSimpleName() + ":" + desc;
        System.out.println(msg);
        logger.info(msg);
    }

    protected void failDeny(String action, Class<?> type, ObjectQuery query, int expected, int actual) {
        failDeny(action, type, (query == null ? "null" : query.toString()) + ", expected " + expected + ", actual " + actual);
    }

    protected void failDeny(String action, Class<?> type, String oid, ItemPath itemPath) {
        failDeny(action, type, oid + " prop " + itemPath);
    }

    protected void failDeny(String action, Class<?> type, String desc) {
        String msg = "Failed to deny " + action + " of " + type.getSimpleName() + ":" + desc;
        System.out.println(LOG_PREFIX_FAIL + msg);
        logger.error(LOG_PREFIX_FAIL + msg);
        AssertJUnit.fail(msg);
    }

    protected void failDeny(String action) {
        String msg = "Failed to deny " + action;
        System.out.println(LOG_PREFIX_FAIL + msg);
        logger.error(LOG_PREFIX_FAIL + msg);
        AssertJUnit.fail(msg);
    }

    protected void failAllow(String action, Class<?> type, ObjectQuery query, SecurityViolationException e) throws SecurityViolationException {
        failAllow(action, type, query == null ? "null" : query.toString(), e);
    }

    protected void failAllow(String action, Class<?> type, ObjectQuery query, int expected, int actual) throws SecurityViolationException {
        failAllow(action, type, (query == null ? "null" : query.toString()) + ", expected " + expected + ", actual " + actual, null);
    }

    protected void failAllow(String action, Class<?> type, String oid, ItemPath itemPath, SecurityViolationException e) throws SecurityViolationException {
        failAllow(action, type, oid + " prop " + itemPath, e);
    }

    protected void failAllow(String action, Class<?> type, String desc, SecurityViolationException e) throws SecurityViolationException {
        String msg = "Failed to allow " + action + " of " + type.getSimpleName() + ":" + desc;
        System.out.println(LOG_PREFIX_FAIL + msg);
        logger.error(LOG_PREFIX_FAIL + msg);
        if (e != null) {
            throw new SecurityViolationException(msg + ": " + e.getMessage(), e);
        } else {
            AssertJUnit.fail(msg);
        }
    }

    protected void failAllow(String action, SecurityViolationException e) throws SecurityViolationException {
        String msg = "Failed to allow " + action;
        System.out.println(LOG_PREFIX_FAIL + msg);
        logger.error(LOG_PREFIX_FAIL + msg);
        if (e != null) {
            throw new SecurityViolationException(msg + ": " + e.getMessage(), e);
        } else {
            AssertJUnit.fail(msg);
        }
    }

    protected <O extends AssignmentHolderType> ArchetypePolicyAsserter<Void> assertArchetypePolicy(PrismObject<O> object) throws SchemaException, ConfigurationException {
        OperationResult result = new OperationResult("assertArchetypePolicy");
        ArchetypePolicyType archetypePolicy = modelInteractionService.determineArchetypePolicy(object, result);
        ArchetypePolicyAsserter<Void> asserter = new ArchetypePolicyAsserter<>(archetypePolicy, null, "for " + object);
        initializeAsserter(asserter);
        return asserter;
    }

    protected <O extends AssignmentHolderType> AssignmentCandidatesSpecificationAsserter<Void> assertAssignmentTargetSpecification(PrismObject<O> object) throws SchemaException, ConfigurationException {
        OperationResult result = new OperationResult("assertAssignmentTargetSpecification");
        AssignmentCandidatesSpecification targetSpec = modelInteractionService.determineAssignmentTargetSpecification(object, result);
        AssignmentCandidatesSpecificationAsserter<Void> asserter = new AssignmentCandidatesSpecificationAsserter<>(targetSpec, null, "targets for " + object);
        initializeAsserter(asserter);
        return asserter;
    }

    protected <O extends AbstractRoleType> AssignmentCandidatesSpecificationAsserter<Void> assertAssignmentHolderSpecification(PrismObject<O> object) throws SchemaException, ConfigurationException {
        OperationResult result = new OperationResult("assertAssignmentHolderSpecification");
        AssignmentCandidatesSpecification targetSpec = modelInteractionService.determineAssignmentHolderSpecification(object, result);
        AssignmentCandidatesSpecificationAsserter<Void> asserter = new AssignmentCandidatesSpecificationAsserter<>(targetSpec, null, "holders for " + object);
        initializeAsserter(asserter);
        return asserter;
    }

    protected VariablesMap createVariables(Object... params) {
        return VariablesMap.create(prismContext, params);
    }

    protected void dumpStatistics(Task task) {
        dumpStatistics(task.getStoredOperationStatsOrClone());
    }

    protected void dumpStatistics(PrismObject<TaskType> task) {
        dumpStatistics(task.asObjectable().getOperationStats());
    }

    private void dumpStatistics(OperationStatsType stats) {
        displayValue("Provisioning statistics", ProvisioningStatistics.format(
                stats.getEnvironmentalPerformanceInformation().getProvisioningStatistics()));
    }

    protected void dumpShadowSituations(String resourceOid, OperationResult result) throws SchemaException {
        ObjectQuery query = queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .build();
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        System.out.println("Current shadows for " + resourceOid + " (" + shadows.size() + "):\n");
        for (PrismObject<ShadowType> shadowObject : shadows) {
            ShadowType shadow = shadowObject.asObjectable();
            System.out.printf("%30s%20s%20s %s%n",
                    shadow.getName(),
                    shadow.getKind(),
                    shadow.getSynchronizationSituation(),
                    Boolean.TRUE.equals(shadow.isProtectedObject()) ? " (protected)" : "");
        }
    }

    protected <T extends ObjectType> void refresh(TestObject<T> testObject, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        testObject.reload(createSimpleModelObjectResolver(), result);
    }

    protected ModelExecuteOptions executeOptions() {
        return ModelExecuteOptions.create();
    }

    protected String determineSingleInducedRuleId(String roleOid, OperationResult result)
            throws CommonException {
        RoleType role = repositoryService.getObject(RoleType.class, roleOid, null, result).asObjectable();
        List<AssignmentType> ruleInducements = role.getInducement().stream()
                .filter(i -> i.getPolicyRule() != null)
                .collect(Collectors.toList());
        assertThat(ruleInducements).as("policy rule inducements in " + role).hasSize(1);
        Long id = ruleInducements.get(0).getId();
        argCheck(id != null, "Policy rule inducement in %s has no PCV ID", roleOid);
        return roleOid + ":" + id;
    }

    protected void switchAccessesMetadata(Boolean value, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        executeChanges(prismContext.deltaFor(SystemConfigurationType.class)
                        .item(F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_ACCESSES_METADATA_ENABLED).replace(value)
                        .<SystemConfigurationType>asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
                null, task, result);
    }

    public interface FunctionCall<X> {
        X execute() throws CommonException, IOException;
    }

    public interface ProcedureCall {
        void execute() throws CommonException, IOException;
    }

    protected <X> X traced(FunctionCall<X> tracedCall)
            throws CommonException, IOException {
        return traced(createModelLoggingTracingProfile(), tracedCall);
    }

    protected void traced(ProcedureCall tracedCall) throws CommonException, IOException {
        traced(createModelLoggingTracingProfile(), tracedCall);
    }

    public void traced(TracingProfileType profile, ProcedureCall tracedCall) throws CommonException, IOException {
        setGlobalTracingOverride(profile);
        try {
            tracedCall.execute();
        } finally {
            unsetGlobalTracingOverride();
        }
    }

    public <X> X traced(TracingProfileType profile, FunctionCall<X> tracedCall)
            throws CommonException, IOException {
        setGlobalTracingOverride(profile);
        try {
            return tracedCall.execute();
        } finally {
            unsetGlobalTracingOverride();
        }
    }

    protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int live, int related) {
        assertFocus(focus, "")
                .assertLinks(live, related);
    }

    /**
     * Returns model object resolver disguised as {@link SimpleObjectResolver} to be used in asserts.
     */
    protected SimpleObjectResolver createSimpleModelObjectResolver() {
        return new SimpleObjectResolver() {
            @Override
            public <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid,
                    Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws ObjectNotFoundException, SchemaException {
                try {
                    //noinspection unchecked
                    return (PrismObject<O>) modelObjectResolver.getObject(type, oid, options, getTestTask(), result).asPrismObject();
                } catch (CommunicationException | ConfigurationException | SecurityViolationException
                        | ExpressionEvaluationException e) {
                    throw new SystemException(e);
                }
            }
        };
    }

    protected ActivityProgressInformationAsserter<Void> assertProgress(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertProgress(rootOid, InformationSource.TREE_OVERVIEW_PREFERRED, message);
    }

    protected ActivityProgressInformationAsserter<Void> assertProgress(String rootOid,
            @NotNull InformationSource source, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertProgress(
                activityManager.getProgressInformation(rootOid, source, getTestOperationResult()),
                message);
    }

    protected ActivityPerformanceInformationAsserter<Void> assertPerformance(String rootOid, String message)
            throws SchemaException, ObjectNotFoundException {
        return assertPerformance(
                activityManager.getPerformanceInformation(rootOid, getTestOperationResult()),
                message);
    }

    protected @NotNull String getBucketReportDataOid(TaskType taskAfter, ActivityPath path) {
        return requireNonNull(
                ActivityReportUtil.getReportDataOid(taskAfter.getActivityState(), path,
                        ActivityReportsType.F_BUCKETS, taskManager.getNodeId()),
                () -> "no bucket report data in " + taskAfter + " (activity path " + path.toDebugName() + ")");
    }

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    @Override
    public OperationResult testResource(@NotNull String oid, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, CommunicationException {
        return modelService.testResource(oid, task, result);
    }

    /** Import a single or multiple accounts (or other kind of object) by creating a specialized task - or on foreground. */
    protected SynchronizationRequestBuilder importAccountsRequest() {
        return new SynchronizationRequestBuilder(this);
    }

    /** Reconcile accounts (or other kind of object) by creating a specialized task. */
    protected SynchronizationRequestBuilder reconcileAccountsRequest() {
        return new SynchronizationRequestBuilder(this)
                .withUsingReconciliation();
    }

    /** Reclassification of shadows by creating a specialized task. */
    protected SynchronizationRequestBuilder shadowReclassificationRequest() {
        return new SynchronizationRequestBuilder(this)
                .withUsingShadowReclassification();
    }

    protected ShadowFindRequestBuilder findShadowRequest() {
        return new ShadowFindRequestBuilder(this);
    }

    /**
     * Executes a set of deltas in {@link TaskExecutionMode#SIMULATED_PRODUCTION} mode.
     *
     * Simulation deltas are stored in returned {@link TestSimulationResult} and optionally also in the storage provided by the
     * {@link SimulationResultManager} - if `simulationDefinition` is present.
     */
    protected TestSimulationResult executeInProductionSimulationMode(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @NotNull SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        assert isNativeRepository();
        return executeWithSimulationResult(deltas, TaskExecutionMode.SIMULATED_PRODUCTION, simulationDefinition, task, result);
    }

    /**
     * As {@link #executeInProductionSimulationMode(Collection, SimulationDefinitionType, Task, OperationResult)} but
     * in development simulation mode.
     */
    protected TestSimulationResult executeDeltasInDevelopmentSimulationMode(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @NotNull SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        assert isNativeRepository();
        return executeWithSimulationResult(deltas, TaskExecutionMode.SIMULATED_DEVELOPMENT, simulationDefinition, task, result);
    }

    protected TestSimulationResult executeWithSimulationResult(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        assert isNativeRepository();
        return executeWithSimulationResult(deltas, null, mode, simulationDefinition, task, result);
    }

    protected TestSimulationResult executeWithSimulationResult(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @Nullable ModelExecuteOptions options,
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        assert isNativeRepository();
        return executeWithSimulationResult(
                mode,
                simulationDefinition,
                task,
                result,
                (simResult) ->
                        modelService.executeChanges(
                                deltas, options, task, List.of(simResult.contextRecordingListener()), result));
    }

    /** Convenience method */
    protected TestSimulationResult executeInProductionSimulationMode(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        assert isNativeRepository();
        return executeInProductionSimulationMode(
                deltas, simulationResultManager.defaultDefinition(), task, result);
    }

    /**
     * Executes a {@link ProcedureCall} in {@link TaskExecutionMode#SIMULATED_PRODUCTION} mode.
     */
    @SuppressWarnings("SameParameterValue")
    protected TestSimulationResult executeInProductionSimulationMode(
            SimulationDefinitionType simulationDefinition, Task task, OperationResult result, ProcedureCall simulatedCall)
            throws CommonException {
        assert isNativeRepository();
        return executeWithSimulationResult(
                TaskExecutionMode.SIMULATED_PRODUCTION,
                simulationDefinition,
                task,
                result,
                (simResult) -> simulatedCall.execute());
    }

    /** As {@link ProcedureCall} but has {@link TestSimulationResult} as a parameter. Currently for internal purposes. */
    public interface SimulatedProcedureCall {
        void execute(TestSimulationResult simResult) throws CommonException, IOException;
    }

    /**
     * Something like this could be (maybe) provided for production code directly by {@link SimulationResultManager}.
     */
    public @NotNull TestSimulationResult executeWithSimulationResult(
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull SimulatedProcedureCall simulatedCall)
            throws CommonException {

        assert isNativeRepository();

        Holder<TestSimulationResult> simulationResultHolder = new Holder<>();
        simulationResultManager.executeWithSimulationResult(mode, simulationDefinition, task, result, () -> {
            TestSimulationResult testSimulationResult = new TestSimulationResult(
                    Objects.requireNonNull(task.getSimulationTransaction())
                            .getResultOid());
            try {
                simulatedCall.execute(testSimulationResult);
            } catch (IOException e) {
                throw SystemException.unexpected(e);
            }
            simulationResultHolder.setValue(testSimulationResult);
            return null;
        });
        return Objects.requireNonNull(
                simulationResultHolder.getValue(),
                "No simulation result after execution?");
    }

    /**
     * Simplified version of {@link #executeWithSimulationResult(TaskExecutionMode, SimulationDefinitionType,
     * Task, OperationResult, SimulatedProcedureCall)}.
     */
    public @NotNull TestSimulationResult executeWithSimulationResult(
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull ProcedureCall call)
            throws CommonException, IOException {
        return executeWithSimulationResult(
                TaskExecutionMode.SIMULATED_PRODUCTION,
                defaultSimulationDefinition(),
                task, result,
                (sr) -> call.execute());
    }

    /**
     * Simplified version of {@link #executeWithSimulationResult(Collection, TaskExecutionMode, SimulationDefinitionType,
     * Task, OperationResult)}.
     */
    public @NotNull TestSimulationResult executeWithSimulationResult(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommonException {
        return executeWithSimulationResult(
                deltas,
                TaskExecutionMode.SIMULATED_PRODUCTION,
                defaultSimulationDefinition(),
                task, result);
    }

    protected @NotNull SimulationDefinitionType defaultSimulationDefinition() throws ConfigurationException {
        return simulationResultManager.defaultDefinition();
    }

    /** Returns {@link TestSimulationResult} based on the information stored in the task (activity state containing result ref) */
    protected @NotNull TestSimulationResult getTaskSimResult(String taskOid, OperationResult result)
            throws CommonException {
        return TestSimulationResult.fromSimulationResultOid(
                getTaskSimulationResultOid(taskOid, result));
    }

    @SuppressWarnings("WeakerAccess")
    protected @NotNull String getTaskSimulationResultOid(String taskOid, OperationResult result)
            throws CommonException {
        Task task = taskManager.getTaskPlain(taskOid, result);
        return getSimulationResultOid(task, ActivityPath.empty());
    }

    protected @NotNull String getSimulationResultOid(Task task, ActivityPath activityPath) {
        ActivitySimulationStateType simState =
                Objects.requireNonNull(task.getActivityStateOrClone(activityPath))
                        .getSimulation();
        assertThat(simState).as("simulation state in " + task).isNotNull();
        ObjectReferenceType simResultRef = simState.getResultRef();
        assertThat(simResultRef).as("simulation result ref in " + task).isNotNull();
        return Objects.requireNonNull(simResultRef.getOid(), "no OID in simulation result ref");
    }

    protected ProcessedObjectsAsserter<Void> assertProcessedObjects(String taskOid, String desc) throws CommonException {
        return assertProcessedObjects(
                getTaskSimResult(taskOid, getTestOperationResult()),
                desc);
    }

    protected ProcessedObjectsAsserter<Void> assertProcessedObjectsAfter(TestSimulationResult simResult) throws CommonException {
        return assertProcessedObjects(simResult, "after")
                .display();
    }

    protected ProcessedObjectsAsserter<Void> assertProcessedObjects(TestSimulationResult simResult)
            throws CommonException {
        return assertProcessedObjects(simResult, "processed objects");
    }

    protected ProcessedObjectsAsserter<Void> assertProcessedObjects(TestSimulationResult simResult, String desc)
            throws CommonException {
        return assertProcessedObjects(
                getProcessedObjects(simResult),
                desc);
    }

    protected Collection<? extends ProcessedObject<?>> getProcessedObjects(TestSimulationResult simResult)
            throws CommonException {
        return simResult.getProcessedObjects(
                getTestOperationResult());
    }

    protected ProcessedObjectsAsserter<Void> assertProcessedObjects(
            Collection<? extends ProcessedObject<?>> objects, String message) {
        return initializeAsserter(
                ProcessedObjectsAsserter.forObjects(objects, message));
    }

    protected CaseWorkItemsAsserter<Void, CaseWorkItemType> assertWorkItems(
            Collection<CaseWorkItemType> workItems, String message) {
        return initializeAsserter(
                CaseWorkItemsAsserter.forWorkItems(workItems, message));
    }

    // TODO will we need this method?
    // TODO do we need the resolution?
    protected List<CaseWorkItemType> getOpenWorkItemsResolved(Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        final Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .item(T_PARENT, F_OBJECT_REF).resolve()
                .item(T_PARENT, F_TARGET_REF).resolve()
                .item(F_ASSIGNEE_REF).resolve()
                .item(F_ORIGINAL_ASSIGNEE_REF).resolve()
                .item(T_PARENT, F_REQUESTOR_REF).resolve()
                .build();

        return new ArrayList<>( // to assure modifiable result list
                modelService.searchContainers(CaseWorkItemType.class,
                        ObjectQueryUtil.openItemsQuery(), options, task, result));
    }

    protected CaseAsserter<Void> assertReferencedCase(OperationResult result, String message)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return assertCase(getReferencedCaseOidRequired(result), message);
    }

    protected @NotNull String getReferencedCaseOidRequired(OperationResult result) {
        String caseOid = result.findCaseOid();
        assertThat(caseOid).as("Case OID referenced by operation result").isNotNull();
        return caseOid;
    }

    protected CaseAsserter<Void> assertReferencedCase(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        return assertReferencedCase(result, "after"); // intentionally not displaying
    }

    protected SimulationResultAsserter<Void> assertSimulationResultAfter(TestSimulationResult simResult)
            throws SchemaException, ObjectNotFoundException {
        return assertSimulationResult(simResult, "after")
                .display();
    }

    protected SimulationResultAsserter<Void> assertSimulationResult(String taskOid, String desc)
            throws CommonException {
        return assertSimulationResult(
                getTaskSimResult(taskOid, getTestOperationResult()),
                desc);
    }

    protected SimulationResultAsserter<Void> assertSimulationResult(TestSimulationResult simResult, String desc)
            throws SchemaException, ObjectNotFoundException {
        return initializeAsserter(
                SimulationResultAsserter.forResult(
                        simResult.getSimulationResultBean(getTestOperationResult()), desc));
    }

    protected SimulationResultAsserter<Void> assertSimulationResult(SimulationResultType simResult, String desc) {
        return initializeAsserter(
                SimulationResultAsserter.forResult(simResult, desc));
    }

    // FIXME does not call applySchemasAndSecurity!
    public <O extends ObjectType> PrismObject<O> getObjectNoAutz(
            Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return createSimpleModelObjectResolver().getObject(type, oid, options, result);
    }

    public <O extends ObjectType> PrismObject<O> getObject(
            Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options)
            throws CommonException {
        return modelService.getObject(type, oid, options, getTestTask(), getTestOperationResult());
    }

    protected @NotNull TestSimulationResult findTestSimulationResultRequired(OperationResult result)
            throws SchemaException {
        return TestSimulationResult.fromSimulationResultOid(
                findSimulationResultRequired(result).getOid());
    }

    /**
     * Returns the input number if accesses metadata are enabled, otherwise returns 0.
     * Usable for audit record count assertions depending on the state of accesses metadata.
     */
    protected int accessesMetadataAuditOverhead(int relevantExecutionRecords) {
        return accessesMetadataEnabled && !isNativeRepository() ? relevantExecutionRecords : 0;
    }

    /**
     * Provides a model-level objects creator.
     *
     * @see AbstractIntegrationTest#realRepoCreator()
     */
    private <O extends ObjectType> ObjectCreator.RealCreator<O> realModelCreator() {
        return (o, result) -> addObject(o.asPrismObject(), getTestTask(), result);
    }

    protected <O extends ObjectType> ObjectCreatorBuilder<O> modelObjectCreatorFor(Class<O> type) {
        return ObjectCreator.forType(type)
                .withRealCreator(realModelCreator());
    }

    protected CsvAsserter<Void> assertCsv(List<String> lines, String message) {
        CsvAsserter<Void> asserter = new CsvAsserter<>(lines, null, message);
        initializeAsserter(asserter);
        return asserter;
    }

    @Override
    public SimpleObjectResolver getResourceReloader() {
        return createSimpleModelObjectResolver();
    }

    // temporary
    protected File findReportOutputFile(PrismObject<TaskType> reportTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ReportTestUtil.findOutputFile(reportTask, createSimpleModelObjectResolver(), result);
    }

    // temporary
    protected List<String> getLinesOfOutputFile(PrismObject<TaskType> reportTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException, IOException {
        return ReportTestUtil.getLinesOfOutputFile(reportTask, createSimpleModelObjectResolver(), result);
    }

    protected void markShadow(String oid, String markOid, Task task, OperationResult result) throws CommonException {
        markShadow(oid, PolicyStatementTypeType.APPLY, markOid, null, task, result);
    }

    protected void markShadowExcluded(String oid, String markOid, Task task, OperationResult result) throws CommonException {
        markShadow(oid, PolicyStatementTypeType.EXCLUDE, markOid, null, task, result);
    }

    protected void markShadow(
            String oid, PolicyStatementTypeType type, String markOid, String lifecycleState, Task task, OperationResult result)
            throws CommonException {
        var statement = new PolicyStatementType()
                .markRef(markOid, MarkType.COMPLEX_TYPE)
                .type(type)
                .lifecycleState(lifecycleState);
        modifyObjectAddContainer(ShadowType.class, oid, ShadowType.F_POLICY_STATEMENT, task, result, statement);
    }

    protected @NotNull CaseType getOpenCaseRequired(List<CaseType> cases) {
        var openCases = cases.stream()
                .filter(c -> QNameUtil.matchUri(c.getState(), CASE_STATE_OPEN_URI))
                .toList();
        return MiscUtil.extractSingletonRequired(
                openCases,
                () -> new AssertionError("More than one open case: " + openCases),
                () -> new AssertionError("No open case in: " + cases));
    }

    protected @NotNull CaseWorkItemType getOpenWorkItemRequired(CaseType aCase) {
        var openWorkItems = aCase.getWorkItem().stream()
                .filter(wi -> CaseTypeUtil.isCaseWorkItemNotClosed(wi))
                .toList();
        return MiscUtil.extractSingletonRequired(
                openWorkItems,
                () -> new AssertionError("More than one open work item: " + openWorkItems),
                () -> new AssertionError("No open work items in: " + aCase));
    }

    protected ObjectDelta<ShadowType> createEntitleDelta(String subjectOid, QName assocName, String objectOid)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var subject = AbstractShadow.of(
                provisioningService.getObject(
                        ShadowType.class, subjectOid, createNoFetchCollection(), getTestTask(), getTestOperationResult()));
        var object = AbstractShadow.of(
                provisioningService.getObject(
                        ShadowType.class, objectOid, createNoFetchCollection(), getTestTask(), getTestOperationResult()));
        var assocDef = subject.getObjectDefinition().findAssociationDefinitionRequired(assocName);
        return deltaFor(ShadowType.class)
                .item(ShadowType.F_ASSOCIATIONS.append(assocName), assocDef)
                .add(assocDef.createValueFromFullDefaultObject(object))
                .asObjectDelta(subjectOid);
    }

    protected void refreshShadowIfNeeded(@NotNull String shadowOid) throws CommonException {
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            provisioningService.getShadow(shadowOid, null, getTestTask(), getTestOperationResult());
        }
    }

    protected void refreshAllShadowsIfNeeded(@NotNull String resourceOid, @NotNull ResourceObjectTypeIdentification type)
            throws CommonException {
        if (!InternalsConfig.isShadowCachingFullByDefault()) {
            return;
        }
        provisioningService.searchShadows(
                prismContext.queryFor(ShadowType.class)
                        .item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                        .and().item(ShadowType.F_KIND).eq(type.getKind())
                        .and().item(ShadowType.F_INTENT).eq(type.getIntent())
                        .build(),
                null, getTestTask(), getTestOperationResult());
    }
}
