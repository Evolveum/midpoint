/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.PredefinedTestMethodTracing.OFF;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.evolveum.midpoint.schema.result.CompiledTracingProfile;

import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.CachingStatistics;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ResourceAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.PolyStringAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.asserter.refinedschema.RefinedResourceSchemaAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.*;
import com.evolveum.midpoint.tools.testng.CurrentTestResultHolder;
import com.evolveum.midpoint.tools.testng.MidpointTestContext;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author Radovan Semancik
 */
@Listeners({ CurrentTestResultHolder.class })
public abstract class AbstractIntegrationTest extends AbstractSpringTest
        implements InfraTestMixin {

    protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    public static final String COMMON_DIR_NAME = "common";
    public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);

    protected static final String DEFAULT_INTENT = "default";

    protected static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";
    protected static final String OPENDJ_GROUPS_SUFFIX = "ou=groups,dc=example,dc=com";

    protected static final Random RND = new Random();

    private static final String MACRO_TEST_NAME_TRACER_PARAM = "testName";
    private static final String MACRO_TEST_NAME_SHORT_TRACER_PARAM = "testNameShort";

    private static final float FLOAT_EPSILON = 0.001f;

    // Values used to check if something is unchanged or changed properly

    protected LdapShaPasswordEncoder ldapShaPasswordEncoder = new LdapShaPasswordEncoder();

    private Map<InternalCounters, Long> lastCountMap = new HashMap<>();

    private CachingStatistics lastResourceCacheStats;

    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    private long lastDummyResourceGroupMembersReadCount;
    private long lastDummyResourceWriteOperationCount;

    @Autowired protected Tracer tracer;

    /**
     * Task manager can be used directly in subclasses but prefer various provided methods
     * like {@link #getTestTask()}, {@link #createPlainTask}, etc.
     */
    @Autowired protected TaskManager taskManager;

    @Autowired protected Protector protector;
    @Autowired protected Clock clock;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaHelper schemaHelper;
    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected LocalizationService localizationService;

    @Autowired(required = false)
    @Qualifier("repoSimpleObjectResolver")
    protected SimpleObjectResolver repoSimpleObjectResolver;

    // Controllers for embedded OpenDJ and Derby. The abstract test will configure it, but
    // it will not start only tests that need OpenDJ or derby should start it
    protected static OpenDJController openDJController = new OpenDJController();
    protected static DerbyController derbyController = new DerbyController();

    /**
     * Fast and simple way how to enable tracing of test methods.
     * (Assuming that auto task management is enabled.)
     */
    protected PredefinedTestMethodTracing predefinedTestMethodTracing;

    private volatile boolean initSystemExecuted = false;

    /**
     * With TestNG+Spring we can use {@code PostConstruct} for class-wide initialization.
     * All test methods run on a single instance (unlike with JUnit).
     * Using {@code BeforeClass} is not good as the Spring wiring happens later.
     * <p>
     * There is still danger that annotation processor will run this multiple times
     * for web/servlet-based tests, see text context attributes
     * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
     * and {@link ServletTestExecutionListener#RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} for more.
     * That's why we start with the guard enforcing once-only initSystem execution.
     */
    @PostConstruct
    public void initSystem() throws Exception {
        if (initSystemExecuted) {
            logger.trace("initSystem: already called for class {} - IGNORING", getClass().getName());
            return;
        }
        initSystemExecuted = true;

        // Check whether we are already initialized
        assertNotNull("Repository is not wired properly", repositoryService);
        assertNotNull("Task manager is not wired properly", taskManager);
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.setPrismContext(prismContext);
        Task initTask = createPlainTask("INIT");
        initTask.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);
        OperationResult result = initTask.getResult();

        InternalMonitor.reset();
        InternalsConfig.setPrismMonitoring(true);
        prismContext.setMonitor(new InternalMonitor());

        ((LocalizationServiceImpl) localizationService).setOverrideLocale(Locale.US);

        initSystem(initTask, result);
        postInitSystem(initTask, result);

        result.computeStatus();
        IntegrationTestTools.display("initSystem result", result);
        TestUtil.assertSuccessOrWarning("initSystem failed (result)", result, 1);
    }

    @Override
    @Nullable
    public MidpointTestContext getTestContext() {
        return MidpointTestContextWithTask.get();
    }

    /**
     * Test class initialization.
     */
    protected void initSystem(Task task, OperationResult initResult) throws Exception {
        // nothing by default
    }

    /**
     * May be used to clean up initialized objects as all of the initialized objects should be
     * available at this time.
     */
    protected void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
        // Nothing to do by default
    }

    /**
     * Creates test method context which includes customized {@link Task}
     * (see {@link #createTask(String)}) and other test related info wrapped as
     * {@link MidpointTestContextWithTask} and stores it in thread-local variable for future access.
     * This implementation fully overrides version from {@link AbstractSpringTest}.
     */
    @BeforeMethod
    public void startTestContext(ITestResult testResult) throws SchemaException {
        Class<?> testClass = testResult.getMethod().getTestClass().getRealClass();
        String testMethodName = testResult.getMethod().getMethodName();
        String testName = testClass.getSimpleName() + "." + testMethodName;
        displayTestTitle(testName);

        Task task = createTask(testMethodName);
        TracingProfileType tracingProfile = getTestMethodTracingProfile();
        if (tracingProfile != null) {
            CompiledTracingProfile compiledTracingProfile = tracer.compileProfile(tracingProfile, task.getResult());
            task.getResult().tracingProfile(compiledTracingProfile);
        }

        MidpointTestContextWithTask.create(testClass, testMethodName, task, task.getResult());
        tracer.setTemplateParametersCustomizer(params -> {
            params.put(MACRO_TEST_NAME_TRACER_PARAM, testName);
            params.put(MACRO_TEST_NAME_SHORT_TRACER_PARAM, testMethodName);
        });
    }

    /**
     * Finish and destroy the test context, output duration and store the operation trace.
     * This implementation fully overrides (without use) the one from {@link AbstractSpringTest}.
     */
    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        MidpointTestContextWithTask context = MidpointTestContextWithTask.get();
        MidpointTestContextWithTask.destroy(); // let's destroy it before anything else in this method

        displayTestFooter(context.getTestName(), testResult);

        Task task = context.getTask();
        if (task != null) {
            OperationResult result = context.getResult();
            if (result != null) {
                result.computeStatusIfUnknown();
                if (result.isTraced()) {
                    display("Storing the trace.");
                    tracer.storeTrace(task, result, null);
                }
                task.getResult().computeStatusIfUnknown();
            }
        }
    }

    protected TracingProfileType getTestMethodTracingProfile() {
        if (predefinedTestMethodTracing == null || predefinedTestMethodTracing == OFF) {
            return null;
        } else {
            TracingProfileType profile;
            switch (predefinedTestMethodTracing) {
                case MODEL_LOGGING:
                    profile = createModelLoggingTracingProfile();
                    break;
                case MODEL_WORKFLOW_LOGGING:
                    profile = createModelAndWorkflowLoggingTracingProfile();
                    break;
                case MODEL_PROVISIONING_LOGGING:
                    profile = createModelAndProvisioningLoggingTracingProfile();
                    break;
                default:
                    throw new AssertionError(predefinedTestMethodTracing.toString());
            }
            return profile
                    .fileNamePattern(TEST_METHOD_TRACING_FILENAME_PATTERN);
        }
    }

    public PredefinedTestMethodTracing getPredefinedTestMethodTracing() {
        return predefinedTestMethodTracing;
    }

    public void setPredefinedTestMethodTracing(PredefinedTestMethodTracing predefinedTestMethodTracing) {
        this.predefinedTestMethodTracing = predefinedTestMethodTracing;
    }

    /**
     * Returns default pre-created test-method-scoped {@link Task}.
     * This fails if test-method context is not available.
     */
    protected Task getTestTask() {
        return MidpointTestContextWithTask.get().getTask();
    }

    /**
     * Creates new {@link Task} with operation name prefixed with {@link #contextName()}.
     * For many tests this is not necessary and the default test-method-scoped task
     * that can be obtained with {@link #getTestTask()} should be enough.
     * If more tasks are needed this method creates plain task without customization, see
     * also {@link #createTask} for customized task.
     * <p>
     * This is useful for multi-threaded tests where we need local task.
     * It is recommended to include method name and/or other info into parameter as only
     * the current class name will be available as default contextual information - even
     * this may be name of a inner class (e.g. anonymous runnable).
     */
    protected Task createPlainTask(String operationName) {
        String rootOpName = operationName != null
                ? contextName() + "." + operationName
                : contextName();
        return taskManager.createTaskInstance(rootOpName);
    }

    /**
     * Creates new {@link Task} with {@link #contextName()} as root operation name.
     */
    protected Task createPlainTask() {
        return createPlainTask(null);
    }

    /**
     * Just like {@link #createPlainTask(String)} but also calls overridable {@link #customizeTask}.
     */
    protected Task createTask(String operationName) {
        Task task = createPlainTask(operationName);
        customizeTask(task);
        return task;
    }

    /**
     * Just like {@link #createPlainTask()} but also calls overridable {@link #customizeTask}.
     */
    protected Task createTask() {
        return createPlainTask(null);
    }

    /**
     * Subclasses may override this if test task needs additional customization.
     * Each task used or created by the test is customized - this applies to default
     * method-scoped task and also to any task created by {@link #createTask(String)}.
     */
    protected void customizeTask(Task task) {
        // nothing by default
    }

    /**
     * Creates new subresult for default pre-created test-method-scoped {@link Task}.
     */
    protected OperationResult createSubresult(String subresultSuffix) {
        return getTestOperationResult().createSubresult(getTestNameShort() + "." + subresultSuffix);
    }

    /**
     * Returns default {@link OperationResult} for pre-created test-method-scoped {@link Task}.
     * This result can be freely used in test for some "main scope", it is not asserted in any
     * after method, only displayed.
     */
    protected OperationResult getTestOperationResult() {
        return MidpointTestContextWithTask.get().getResult();
    }

    public <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException {
        return prismContext.deltaFor(objectClass);
    }

    protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(String filePath,
            OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        return repoAddObjectFromFile(new File(filePath), parentResult);
    }

    protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file,
            OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        return repoAddObjectFromFile(file, false, parentResult);
    }

    protected <T extends ObjectType> PrismObject<T> repoAdd(TestResource resource, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, IOException, EncryptionException {
        PrismObject<T> object = repoAddObjectFromFile(resource.file, parentResult);
        resource.object = object;
        return object;
    }

    protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(
            File file, @SuppressWarnings("unused") Class<T> type, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

        return repoAddObjectFromFile(file, false, parentResult);
    }

    protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(
            File file, @SuppressWarnings("unused") Class<T> type, boolean metadata, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

        return repoAddObjectFromFile(file, metadata, parentResult);
    }

    protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(
            File file, boolean metadata, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

        OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
                + ".repoAddObjectFromFile");
        result.addParam("file", file.getPath());
        logger.debug("addObjectFromFile: {}", file);
        PrismObject<T> object;
        try {
            object = prismContext.parseObject(file);
        } catch (SchemaException e) {
            throw new SchemaException("Error parsing file " + file.getPath() + ": " + e.getMessage(), e);
        }

        if (metadata) {
            addBasicMetadata(object);
        }

        logger.trace("Adding object:\n{}", object.debugDump());
        repoAddObject(object, "from file " + file, result);
        result.recordSuccess();
        return object;
    }

    protected PrismObject<ShadowType> repoAddShadowFromFile(File file, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

        OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
                + ".repoAddShadowFromFile");
        result.addParam("file", file.getPath());
        logger.debug("addShadowFromFile: {}", file);
        PrismObject<ShadowType> object = prismContext.parseObject(file);

        PrismContainer<Containerable> attrCont = object.findContainer(ShadowType.F_ATTRIBUTES);
        for (PrismProperty<?> attr : attrCont.getValue().getProperties()) {
            if (attr.getDefinition() == null) {
                ResourceAttributeDefinition<String> attrDef = ObjectFactory.createResourceAttributeDefinition(attr.getElementName(),
                        DOMUtil.XSD_STRING, prismContext);
                attr.setDefinition((PrismPropertyDefinition) attrDef);
            }
        }

        addBasicMetadata(object);

        logger.trace("Adding object:\n{}", object.debugDump());
        repoAddObject(object, "from file " + file, result);
        result.recordSuccess();
        return object;
    }

    protected <T extends ObjectType> void addBasicMetadata(PrismObject<T> object) {
        // Add at least the very basic meta-data
        MetadataType metaData = new MetadataType();
        metaData.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());
        object.asObjectable().setMetadata(metaData);
    }

    protected <T extends ObjectType> void repoAddObject(
            PrismObject<T> object, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
        repoAddObject(object, null, result);
    }

    protected <T extends ObjectType> void repoAddObject(
            PrismObject<T> object, String contextDesc, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
        if (object.canRepresent(TaskType.class)) {
            Assert.assertNotNull(taskManager, "Task manager is not initialized");
            try {
                taskManager.addTask((PrismObject<TaskType>) object, result);
            } catch (ObjectAlreadyExistsException | SchemaException ex) {
                result.recordFatalError(ex.getMessage(), ex);
                throw ex;
            }
        } else {
            Assert.assertNotNull(repositoryService, "Repository service is not initialized");
            try {
                CryptoUtil.encryptValues(protector, object);
                String oid = repositoryService.addObject(object, null, result);
                object.setOid(oid);
            } catch (ObjectAlreadyExistsException | SchemaException | EncryptionException ex) {
                result.recordFatalError(ex.getMessage() + " while adding " + object + (contextDesc == null ? "" : " " + contextDesc), ex);
                throw ex;
            }
        }
    }

    protected <T extends ObjectType> List<PrismObject<T>> repoAddObjectsFromFile(
            String filePath, Class<T> type, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, IOException, EncryptionException {

        return repoAddObjectsFromFile(new File(filePath), type, parentResult);
    }

    protected <T extends ObjectType> List<PrismObject<T>> repoAddObjectsFromFile(
            File file, Class<T> type, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, IOException, EncryptionException {

        OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
                + ".addObjectsFromFile");
        result.addParam("file", file.getPath());
        logger.trace("addObjectsFromFile: {}", file);
        List<PrismObject<T>> objects = (List) prismContext.parserFor(file).parseObjects();
        for (PrismObject<T> object : objects) {
            try {
                repoAddObject(object, result);
            } catch (ObjectAlreadyExistsException e) {
                throw new ObjectAlreadyExistsException(e.getMessage() + " while adding " + object + " from file " + file, e);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while adding " + object + " from file " + file, e);
            } catch (EncryptionException e) {
                throw new EncryptionException(e.getMessage() + " while adding " + object + " from file " + file, e);
            }
        }
        result.recordSuccess();
        return objects;
    }

    // these objects can be of various types
    protected List<PrismObject> repoAddObjectsFromFile(File file, OperationResult parentResult)
            throws SchemaException, ObjectAlreadyExistsException, IOException, EncryptionException {

        OperationResult result = parentResult.createSubresult(
                AbstractIntegrationTest.class.getName() + ".addObjectsFromFile");
        result.addParam("file", file.getPath());
        logger.trace("addObjectsFromFile: {}", file);
        List<PrismObject> objects = (List) prismContext.parserFor(file).parseObjects();
        for (PrismObject object : objects) {
            try {
                repoAddObject(object, result);
            } catch (ObjectAlreadyExistsException e) {
                throw new ObjectAlreadyExistsException(e.getMessage() + " while adding " + object + " from file " + file, e);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while adding " + object + " from file " + file, e);
            } catch (EncryptionException e) {
                throw new EncryptionException(e.getMessage() + " while adding " + object + " from file " + file, e);
            }
        }
        result.recordSuccess();
        return objects;
    }

    protected <T extends ObjectType> T parseObjectTypeFromFile(String fileName, Class<T> clazz) throws SchemaException, IOException {
        return parseObjectType(new File(fileName), clazz);
    }

    protected <T extends ObjectType> T parseObjectType(File file) throws SchemaException, IOException {
        PrismObject<T> prismObject = prismContext.parseObject(file);
        return prismObject.asObjectable();
    }

    protected <T extends ObjectType> T parseObjectType(File file, Class<T> clazz) throws SchemaException, IOException {
        PrismObject<T> prismObject = prismContext.parseObject(file);
        return prismObject.asObjectable();
    }

    protected static <T> T unmarshalValueFromFile(File file, Class<T> clazz)
            throws IOException, SchemaException {
        return PrismTestUtil.parseAnyValue(file);
    }

    protected static <T> T unmarshalValueFromFile(String filePath, Class<T> clazz)
            throws IOException, SchemaException {
        return PrismTestUtil.parseAnyValue(new File(filePath));
    }

    protected static ObjectType unmarshalValueFromFile(String filePath)
            throws IOException, SchemaException {
        return unmarshalValueFromFile(filePath, ObjectType.class);
    }

    protected PrismObject<ResourceType> addResourceFromFile(
            File file, String connectorType, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        return addResourceFromFile(file, connectorType, false, result);
    }

    protected PrismObject<ResourceType> addResourceFromFile(
            File file, String connectorType, boolean overwrite, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        return addResourceFromFile(file, Collections.singletonList(connectorType), overwrite, result);
    }

    protected PrismObject<ResourceType> addResourceFromFile(
            File file, List<String> connectorTypes, boolean overwrite, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        logger.trace("addObjectFromFile: {}, connector types {}", file, connectorTypes);
        PrismObject<ResourceType> resource = prismContext.parseObject(file);
        return addResourceFromObject(resource, connectorTypes, overwrite, result);
    }

    @NotNull
    protected PrismObject<ResourceType> addResourceFromObject(PrismObject<ResourceType> resource, List<String> connectorTypes,
            boolean overwrite, OperationResult result)
            throws SchemaException, EncryptionException,
            ObjectAlreadyExistsException {
        for (int i = 0; i < connectorTypes.size(); i++) {
            String type = connectorTypes.get(i);
            if (i == 0) {
                fillInConnectorRef(resource, type, result);
            } else {
                fillInAdditionalConnectorRef(resource, i - 1, type, result);
            }
        }
        CryptoUtil.encryptValues(protector, resource);
        display("Adding resource ", resource);
        RepoAddOptions options = null;
        if (overwrite) {
            options = RepoAddOptions.createOverwrite();
        }
        String oid = repositoryService.addObject(resource, options, result);
        resource.setOid(oid);
        return resource;
    }

    protected PrismObject<ConnectorType> findConnectorByType(String connectorType, OperationResult result)
            throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ConnectorType.class)
                .item(ConnectorType.F_CONNECTOR_TYPE).eq(connectorType)
                .build();
        List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
        if (connectors.size() != 1) {
            throw new IllegalStateException("Cannot find connector type " + connectorType + ", got " + connectors);
        }
        return connectors.get(0);
    }

    protected PrismObject<ConnectorType> findConnectorByTypeAndVersion(String connectorType, String connectorVersion, OperationResult result)
            throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ConnectorType.class)
                .item(ConnectorType.F_CONNECTOR_TYPE).eq(connectorType)
                .and().item(ConnectorType.F_CONNECTOR_VERSION).eq(connectorVersion)
                .build();
        List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
        if (connectors.size() != 1) {
            throw new IllegalStateException("Cannot find connector type " + connectorType + ", version " + connectorVersion + ", got "
                    + connectors);
        }
        return connectors.get(0);
    }

    protected void fillInConnectorRef(PrismObject<ResourceType> resource, String connectorType, OperationResult result)
            throws SchemaException {
        ResourceType resourceType = resource.asObjectable();
        PrismObject<ConnectorType> connector = findConnectorByType(connectorType, result);
        if (resourceType.getConnectorRef() == null) {
            resourceType.setConnectorRef(new ObjectReferenceType());
        }
        resourceType.getConnectorRef().setOid(connector.getOid());
        resourceType.getConnectorRef().setType(ObjectTypes.CONNECTOR.getTypeQName());
    }

    protected void fillInAdditionalConnectorRef(PrismObject<ResourceType> resource,
            String connectorName, String connectorType, OperationResult result)
            throws SchemaException {
        ResourceType resourceType = resource.asObjectable();
        PrismObject<ConnectorType> connectorPrism = findConnectorByType(connectorType, result);
        for (ConnectorInstanceSpecificationType additionalConnector : resourceType.getAdditionalConnector()) {
            if (connectorName.equals(additionalConnector.getName())) {
                ObjectReferenceType ref = new ObjectReferenceType().oid(connectorPrism.getOid());
                additionalConnector.setConnectorRef(ref);
            }
        }
    }

    protected void fillInAdditionalConnectorRef(PrismObject<ResourceType> resource, int connectorIndex, String connectorType, OperationResult result)
            throws SchemaException {
        ResourceType resourceType = resource.asObjectable();
        PrismObject<ConnectorType> connectorPrism = findConnectorByType(connectorType, result);
        ConnectorInstanceSpecificationType additionalConnector = resourceType.getAdditionalConnector().get(connectorIndex);
        ObjectReferenceType ref = new ObjectReferenceType().oid(connectorPrism.getOid());
        additionalConnector.setConnectorRef(ref);
    }

    protected SystemConfigurationType getSystemConfiguration() throws SchemaException {
        OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".getSystemConfiguration");
        try {
            PrismObject<SystemConfigurationType> sysConf = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
            result.computeStatus();
            TestUtil.assertSuccess("getObject(systemConfig) not success", result);
            return sysConf.asObjectable();
        } catch (ObjectNotFoundException e) {
            // No big deal
            return null;
        }
    }

    protected void assumeAssignmentPolicy(AssignmentPolicyEnforcementType policy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        AssignmentPolicyEnforcementType currentPolicy = getAssignmentPolicyEnforcementType(systemConfiguration);
        if (currentPolicy == policy) {
            return;
        }
        ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        applySyncSettings(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS, syncSettings);
    }

    // very limited approach -- assumes that we set conflict resolution on a global level only
    protected void assumeConflictResolutionAction(ConflictResolutionActionType action) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        List<ObjectPolicyConfigurationType> current = new ArrayList<>();
        List<ObjectPolicyConfigurationType> currentForTasks = new ArrayList<>();
        final ConflictResolutionActionType actionForTasks = ConflictResolutionActionType.NONE;
        for (ObjectPolicyConfigurationType c : systemConfiguration.getDefaultObjectPolicyConfiguration()) {
            if (c.getType() == null && c.getSubtype() == null && c.getConflictResolution() != null) {
                current.add(c);
            } else if (QNameUtil.match(c.getType(), TaskType.COMPLEX_TYPE) && c.getSubtype() == null && c.getConflictResolution() != null) {
                currentForTasks.add(c);
            }
        }
        List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
        if (current.size() != 1 || current.get(0).getConflictResolution().getAction() != action) {
            ObjectPolicyConfigurationType newPolicy = new ObjectPolicyConfigurationType(prismContext)
                    .beginConflictResolution()
                    .action(action)
                    .end();
            itemDeltas.add(prismContext.deltaFor(SystemConfigurationType.class)
                    .item(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION)
                    .add(newPolicy)
                    .deleteRealValues(current)
                    .asItemDelta());
        }
        if (currentForTasks.size() != 1 || currentForTasks.get(0).getConflictResolution().getAction() != actionForTasks) {
            ObjectPolicyConfigurationType newPolicyForTasks = new ObjectPolicyConfigurationType(prismContext)
                    .type(TaskType.COMPLEX_TYPE)
                    .beginConflictResolution()
                    .action(actionForTasks)
                    .end();
            itemDeltas.add(prismContext.deltaFor(SystemConfigurationType.class)
                    .item(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION)
                    .add(newPolicyForTasks)
                    .deleteRealValues(currentForTasks)
                    .asItemDelta());
        }
        if (!itemDeltas.isEmpty()) {
            OperationResult result = new OperationResult("assumeConflictResolutionAction");
            repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), itemDeltas, result);
            invalidateSystemObjectsCache();
            display("Applying conflict resolution action result", result);
            result.computeStatus();
            TestUtil.assertSuccess("Applying conflict resolution action failed (result)", result);
        }
    }

    protected void assumeResourceAssigmentPolicy(
            String resourceOid, AssignmentPolicyEnforcementType policy, boolean legalize)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(legalize);
        applySyncSettings(ResourceType.class, resourceOid, ResourceType.F_PROJECTION, syncSettings);
    }

    protected void deleteResourceAssigmentPolicy(
            String oid, AssignmentPolicyEnforcementType policy, boolean legalize)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(legalize);
        ContainerDelta<ProjectionPolicyType> deleteAssigmentEnforcement = prismContext.deltaFactory().container()
                .createModificationDelete(ResourceType.F_PROJECTION, ResourceType.class,
                        syncSettings.clone());

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(deleteAssigmentEnforcement);

        OperationResult result = createOperationResult("Applying sync settings");

        repositoryService.modifyObject(ResourceType.class, oid, modifications, result);
        display("Applying sync settings result", result);
        result.computeStatus();
        TestUtil.assertSuccess("Applying sync settings failed (result)", result);
    }

    protected AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(SystemConfigurationType systemConfiguration) {
        ProjectionPolicyType globalAccountSynchronizationSettings = systemConfiguration.getGlobalAccountSynchronizationSettings();
        if (globalAccountSynchronizationSettings == null) {
            return null;
        }
        return globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
    }

    protected void applySyncSettings(Class clazz, String oid, ItemName itemName, ProjectionPolicyType syncSettings)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        PrismObjectDefinition<?> objectDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(clazz);

        Collection<? extends ItemDelta> modifications = prismContext.deltaFactory().container()
                .createModificationReplaceContainerCollection(itemName, objectDefinition, syncSettings.asPrismContainerValue());

        OperationResult result = new OperationResult("Applying sync settings");

        repositoryService.modifyObject(clazz, oid, modifications, result);
        invalidateSystemObjectsCache();
        display("Applying sync settings result", result);
        result.computeStatus();
        TestUtil.assertSuccess("Applying sync settings failed (result)", result);
    }

    protected void invalidateSystemObjectsCache() {
        // Nothing to do here. For subclasses in model-common and higher components.
    }

    protected void assertNoChanges(ObjectDelta<?> delta) {
        assertNull("Unexpected changes: " + delta, delta);
    }

    protected void assertNoChanges(String desc, ObjectDelta<?> delta) {
        assertNull("Unexpected changes in " + desc + ": " + delta, delta);
    }

    protected <F extends FocusType> void assertEffectiveActivation(PrismObject<F> focus, ActivationStatusType expected) {
        ActivationType activationType = focus.asObjectable().getActivation();
        assertNotNull("No activation in " + focus, activationType);
        assertEquals("Wrong effectiveStatus in activation in " + focus, expected, activationType.getEffectiveStatus());
    }

    protected <F extends FocusType> void assertEffectiveActivation(AssignmentType assignmentType, ActivationStatusType expected) {
        ActivationType activationType = assignmentType.getActivation();
        assertNotNull("No activation in " + assignmentType, activationType);
        assertEquals("Wrong effectiveStatus in activation in " + assignmentType, expected, activationType.getEffectiveStatus());
    }

    protected <F extends FocusType> void assertValidityStatus(PrismObject<F> focus, TimeIntervalStatusType expected) {
        ActivationType activationType = focus.asObjectable().getActivation();
        assertNotNull("No activation in " + focus, activationType);
        assertEquals("Wrong validityStatus in activation in " + focus, expected, activationType.getValidityStatus());
    }

    protected ResourceAsserter<Void> assertResource(PrismObject<ResourceType> resource, String message) {
        ResourceAsserter<Void> asserter = ResourceAsserter.forResource(resource, message);
        initializeAsserter(asserter);
        asserter.display();
        return asserter;
    }

    protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName) {
        assertUser(user, oid, name, fullName, givenName, familyName, null);
    }

    protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName, String location) {
        new PrismObjectAsserter<>((PrismObject<? extends ObjectType>) user)
                .assertSanity();
        UserType userType = user.asObjectable();
        if (oid != null) {
            assertEquals("Wrong " + user + " OID (prism)", oid, user.getOid());
            assertEquals("Wrong " + user + " OID (jaxb)", oid, userType.getOid());
        }
        PrismAsserts.assertEqualsPolyString("Wrong " + user + " name", name, userType.getName());
        PrismAsserts.assertEqualsPolyString("Wrong " + user + " fullName", fullName, userType.getFullName());
        PrismAsserts.assertEqualsPolyString("Wrong " + user + " givenName", givenName, userType.getGivenName());
        PrismAsserts.assertEqualsPolyString("Wrong " + user + " familyName", familyName, userType.getFamilyName());

        if (location != null) {
            PrismAsserts.assertEqualsPolyString("Wrong " + user + " location", location,
                    userType.getLocality());
        }
    }

    protected <O extends ObjectType> void assertSubtype(PrismObject<O> object, String subtype) {
        assertTrue("Object " + object + " does not have subtype " + subtype, FocusTypeUtil.hasSubtype(object, subtype));
    }

    protected void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, QName objectClass) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, null, false);
    }

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null, false);
    }

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
            MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), nameMatchingRule, requireNormalizedIdentfiers);
    }

    protected QName getAccountObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
    }

    protected QName getGroupObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "GroupObjectClass");
    }

    protected void assertShadowCommon(PrismObject<ShadowType> shadow, String oid, String username, ResourceType resourceType,
            QName objectClass, MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers) throws SchemaException {
        assertShadowCommon(shadow, oid, username, resourceType, objectClass, nameMatchingRule, requireNormalizedIdentfiers, false);
    }

    protected void assertShadowCommon(PrismObject<ShadowType> shadow, String oid, String username, ResourceType resourceType,
            QName objectClass, final MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers, boolean useMatchingRuleForShadowName) throws SchemaException {
        new PrismObjectAsserter<>((PrismObject<? extends ObjectType>) shadow)
                .assertSanity();
        if (oid != null) {
            assertEquals("Shadow OID mismatch (prism)", oid, shadow.getOid());
        }
        ShadowType resourceObjectShadowType = shadow.asObjectable();
        if (oid != null) {
            assertEquals("Shadow OID mismatch (jaxb)", oid, resourceObjectShadowType.getOid());
        }
        assertEquals("Shadow objectclass", objectClass, resourceObjectShadowType.getObjectClass());
        assertEquals("Shadow resourceRef OID", resourceType.getOid(), shadow.asObjectable().getResourceRef().getOid());
        PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("Null attributes in shadow for " + username, attributesContainer);
        assertFalse("Empty attributes in shadow for " + username, attributesContainer.isEmpty());

        if (useMatchingRuleForShadowName) {
            MatchingRule<PolyString> polyMatchingRule = new MatchingRule<PolyString>() {

                @Override
                public QName getName() {
                    return nameMatchingRule.getName();
                }

                @Override
                public boolean isSupported(QName xsdType) {
                    return nameMatchingRule.isSupported(xsdType);
                }

                @Override
                public boolean match(PolyString a, PolyString b) throws SchemaException {
                    return nameMatchingRule.match(a.getOrig(), b.getOrig());
                }

                @Override
                public boolean matchRegex(PolyString a, String regex) throws SchemaException {
                    return nameMatchingRule.matchRegex(a.getOrig(), regex);
                }

                @Override
                public PolyString normalize(PolyString original) throws SchemaException {
                    return new PolyString(nameMatchingRule.normalize(original.getOrig()));
                }

            };
            PrismAsserts.assertPropertyValueMatch(shadow, ShadowType.F_NAME, polyMatchingRule, PrismTestUtil.createPolyString(username));
        } else {
            PrismAsserts.assertPropertyValue(shadow, ShadowType.F_NAME, PrismTestUtil.createPolyString(username));
        }

        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        ObjectClassComplexTypeDefinition ocDef = rSchema.findObjectClassDefinition(objectClass);
        if (ocDef.getSecondaryIdentifiers().isEmpty()) {
            ResourceAttributeDefinition idDef = ocDef.getPrimaryIdentifiers().iterator().next();
            PrismProperty<String> idProp = attributesContainer.findProperty(idDef.getItemName());
            assertNotNull("No primary identifier (" + idDef.getItemName() + ") attribute in shadow for " + username, idProp);
            if (nameMatchingRule == null) {
                assertEquals("Unexpected primary identifier in shadow for " + username, username, idProp.getRealValue());
            } else {
                if (requireNormalizedIdentfiers) {
                    assertEquals("Unexpected primary identifier in shadow for " + username, nameMatchingRule.normalize(username), idProp.getRealValue());
                } else {
                    PrismAsserts.assertEquals("Unexpected primary identifier in shadow for " + username, nameMatchingRule, username, idProp.getRealValue());
                }
            }
        } else {
            boolean found = false;
            String expected = username;
            if (requireNormalizedIdentfiers && nameMatchingRule != null) {
                expected = nameMatchingRule.normalize(username);
            }
            List<String> wasValues = new ArrayList<>();
            for (ResourceAttributeDefinition idSecDef : ocDef.getSecondaryIdentifiers()) {
                PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getItemName());
                wasValues.addAll(idProp.getRealValues());
                assertNotNull("No secondary identifier (" + idSecDef.getItemName() + ") attribute in shadow for " + username, idProp);
                if (nameMatchingRule == null) {
                    if (username.equals(idProp.getRealValue())) {
                        found = true;
                        break;
                    }
                } else {
                    if (requireNormalizedIdentfiers) {
                        if (expected.equals(idProp.getRealValue())) {
                            found = true;
                            break;
                        }
                    } else if (nameMatchingRule.match(username, idProp.getRealValue())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                fail("Unexpected secondary identifier in shadow for " + username + ", expected " + expected + " but was " + wasValues);
            }
        }
    }

    protected void assertShadowSecondaryIdentifier(PrismObject<ShadowType> shadow, String expectedIdentifier, ResourceType resourceType, MatchingRule<String> nameMatchingRule) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        ObjectClassComplexTypeDefinition ocDef = rSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
        ResourceAttributeDefinition idSecDef = ocDef.getSecondaryIdentifiers().iterator().next();
        PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getItemName());
        assertNotNull("No secondary identifier (" + idSecDef.getItemName() + ") attribute in shadow for " + expectedIdentifier, idProp);
        if (nameMatchingRule == null) {
            assertEquals("Unexpected secondary identifier in shadow for " + expectedIdentifier, expectedIdentifier, idProp.getRealValue());
        } else {
            PrismAsserts.assertEquals("Unexpected secondary identifier in shadow for " + expectedIdentifier, nameMatchingRule, expectedIdentifier, idProp.getRealValue());
        }

    }

    protected void assertShadowName(PrismObject<ShadowType> shadow, String expectedName) {
        PrismAsserts.assertEqualsPolyString("Shadow name is wrong in " + shadow, expectedName, shadow.asObjectable().getName());
    }

    protected void assertShadowName(ShadowType shadowType, String expectedName) {
        assertShadowName(shadowType.asPrismObject(), expectedName);
    }

    protected void assertShadowRepo(String oid, String username, ResourceType resourceType, QName objectClass) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".assertShadowRepo");
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, oid, null, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertShadowRepo(shadow, oid, username, resourceType, objectClass);
    }

    protected void assertAccountShadowRepo(String oid, String username, ResourceType resourceType) throws ObjectNotFoundException, SchemaException {
        assertShadowRepo(oid, username, resourceType, getAccountObjectClass(resourceType));
    }

    protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
            QName objectClass) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, resourceType, objectClass, null);
    }

    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null);
    }

    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, MatchingRule<String> matchingRule) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), matchingRule);
    }

    protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
            QName objectClass, MatchingRule<String> nameMatchingRule) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, nameMatchingRule, true);
        PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ShadowType.F_ATTRIBUTES);
        Collection<Item<?, ?>> attributes = attributesContainer.getValue().getItems();
//        Collection secIdentifiers = ShadowUtil.getSecondaryIdentifiers(accountShadow);
        RefinedResourceSchema refinedSchema = null;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        } catch (SchemaException e) {
            AssertJUnit.fail(e.getMessage());
        }
        ObjectClassComplexTypeDefinition objClassDef = refinedSchema.getRefinedDefinition(objectClass);
        Collection secIdentifiers = objClassDef.getSecondaryIdentifiers();
        if (secIdentifiers == null) {
            AssertJUnit.fail("No secondary identifiers in repo shadow");
        }
        // repo shadow should contains all secondary identifiers + ICF_UID
        assertRepoShadowAttributes(attributes, secIdentifiers.size() + 1);
    }

    protected void assertRepoShadowAttributes(Collection<Item<?, ?>> attributes, int expectedNumberOfIdentifiers) {
        assertEquals("Unexpected number of attributes in repo shadow", expectedNumberOfIdentifiers, attributes.size());
    }

    protected String getIcfUid(PrismObject<ShadowType> shadow) {
        PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("Null attributes in " + shadow, attributesContainer);
        assertFalse("Empty attributes in " + shadow, attributesContainer.isEmpty());
        PrismProperty<String> icfUidProp = attributesContainer.findProperty(new ItemName(SchemaConstants.NS_ICF_SCHEMA, "uid"));
        assertNotNull("No ICF name attribute in " + shadow, icfUidProp);
        return icfUidProp.getRealValue();
    }

    protected void rememberCounter(InternalCounters counter) {
        lastCountMap.put(counter, InternalMonitor.getCount(counter));
    }

    protected long getLastCount(InternalCounters counter) {
        Long lastCount = lastCountMap.get(counter);
        if (lastCount == null) {
            return 0;
        } else {
            return lastCount;
        }
    }

    protected long getCounterIncrement(InternalCounters counter) {
        return InternalMonitor.getCount(counter) - getLastCount(counter);
    }

    protected void assertCounterIncrement(InternalCounters counter, int expectedIncrement) {
        long currentCount = InternalMonitor.getCount(counter);
        long actualIncrement = currentCount - getLastCount(counter);
        assertEquals("Unexpected increment in " + counter.getLabel(), expectedIncrement, actualIncrement);
        lastCountMap.put(counter, currentCount);
    }

    protected void assertCounterIncrement(InternalCounters counter, int expectedIncrementMin, int expectedIncrementMax) {
        long currentCount = InternalMonitor.getCount(counter);
        long actualIncrement = currentCount - getLastCount(counter);
        assertTrue("Unexpected increment in " + counter.getLabel() + ". Expected "
                        + expectedIncrementMin + "-" + expectedIncrementMax + " but was " + actualIncrement,
                actualIncrement >= expectedIncrementMin && actualIncrement <= expectedIncrementMax);
        lastCountMap.put(counter, currentCount);
    }

    protected void rememberResourceCacheStats() {
        lastResourceCacheStats = InternalMonitor.getResourceCacheStats().clone();
    }

    protected void assertResourceCacheHitsIncrement(int expectedIncrement) {
        assertCacheHits(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resource cache", expectedIncrement);
    }

    protected void assertResourceCacheMissesIncrement(int expectedIncrement) {
        assertCacheMisses(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resource cache", expectedIncrement);
    }

    protected void assertCacheHits(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
        long actualIncrement = currentStats.getHits() - lastStats.getHits();
        assertEquals("Unexpected increment in " + desc + " hit count", expectedIncrement, actualIncrement);
        lastStats.setHits(currentStats.getHits());
    }

    protected void assertCacheMisses(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
        long actualIncrement = currentStats.getMisses() - lastStats.getMisses();
        assertEquals("Unexpected increment in " + desc + " miss count", expectedIncrement, actualIncrement);
        lastStats.setMisses(currentStats.getMisses());
    }

    protected void assertSteadyResources() {
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
    }

    protected void rememberSteadyResources() {
        rememberCounter(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT);
        rememberCounter(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
    }

    protected void rememberDummyResourceGroupMembersReadCount(String instanceName) {
        lastDummyResourceGroupMembersReadCount = DummyResource.getInstance(instanceName).getGroupMembersReadCount();
    }

    protected void assertDummyResourceGroupMembersReadCountIncrement(String instanceName, int expectedIncrement) {
        long currentDummyResourceGroupMembersReadCount = DummyResource.getInstance(instanceName).getGroupMembersReadCount();
        long actualIncrement = currentDummyResourceGroupMembersReadCount - lastDummyResourceGroupMembersReadCount;
        assertEquals("Unexpected increment in group members read count in dummy resource '" + instanceName + "'", expectedIncrement, actualIncrement);
        lastDummyResourceGroupMembersReadCount = currentDummyResourceGroupMembersReadCount;
    }

    protected void rememberDummyResourceWriteOperationCount(String instanceName) {
        lastDummyResourceWriteOperationCount = DummyResource.getInstance(instanceName).getWriteOperationCount();
    }

    protected void assertDummyResourceWriteOperationCountIncrement(String instanceName, int expectedIncrement) {
        long currentCount = DummyResource.getInstance(instanceName).getWriteOperationCount();
        long actualIncrement = currentCount - lastDummyResourceWriteOperationCount;
        assertEquals("Unexpected increment in write operation count in dummy resource '" + instanceName + "'", expectedIncrement, actualIncrement);
        lastDummyResourceWriteOperationCount = currentCount;
    }

    protected PrismObject<ShadowType> createShadow(PrismObject<ResourceType> resource, String id) throws SchemaException {
        return createShadow(resource, id, id);
    }

    protected PrismObject<ShadowType> createShadowNameOnly(PrismObject<ResourceType> resource, String name) throws SchemaException {
        return createShadow(resource, null, name);
    }

    protected PrismObject<ShadowType> createShadow(PrismObject<ResourceType> resource, String uid, String name) throws SchemaException {
        PrismObject<ShadowType> shadow = getShadowDefinition().instantiate();
        ShadowType shadowType = shadow.asObjectable();
        if (name != null) {
            shadowType.setName(PrismTestUtil.createPolyStringType(name));
        }
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resource.getOid());
        shadowType.setResourceRef(resourceRef);
        shadowType.setKind(ShadowKindType.ACCOUNT);
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        shadowType.setObjectClass(objectClassDefinition.getTypeName());
        ResourceAttributeContainer attrContainer = ShadowUtil.getOrCreateAttributesContainer(shadow, objectClassDefinition);
        if (uid != null) {
            RefinedAttributeDefinition uidAttrDef = objectClassDefinition.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA, "uid"));
            ResourceAttribute<String> uidAttr = uidAttrDef.instantiate();
            uidAttr.setRealValue(uid);
            attrContainer.add(uidAttr);
        }
        if (name != null) {
            RefinedAttributeDefinition nameAttrDef = objectClassDefinition.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA, "name"));
            ResourceAttribute<String> nameAttr = nameAttrDef.instantiate();
            nameAttr.setRealValue(name);
            attrContainer.add(nameAttr);
        }
        return shadow;
    }

    protected PrismObject<ShadowType> findAccountShadowByUsername(
            String username, PrismObject<ResourceType> resource, OperationResult result)
            throws SchemaException {
        ObjectQuery query = createAccountShadowQuerySecondaryIdentifier(username, resource);
        List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, null, result);
        if (accounts.isEmpty()) {
            return null;
        }
        assert accounts.size() == 1 : "Too many accounts found for username " + username + " on " + resource + ": " + accounts;
        return accounts.iterator().next();
    }

    protected PrismObject<ShadowType> findShadowByName(ShadowKindType kind, String intent, String name, PrismObject<ResourceType> resource, OperationResult result) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rOcDef = rSchema.getRefinedDefinition(kind, intent);
        ObjectQuery query = createShadowQuerySecondaryIdentifier(rOcDef, name, resource);
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        if (shadows.isEmpty()) {
            return null;
        }
        assert shadows.size() == 1 : "Too many shadows found for name " + name + " on " + resource + ": " + shadows;
        return shadows.iterator().next();
    }

    protected PrismObject<ShadowType> findShadowByName(QName objectClass, String name, PrismObject<ResourceType> resource, OperationResult result) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rOcDef = rSchema.getRefinedDefinition(objectClass);
        ObjectQuery query = createShadowQuerySecondaryIdentifier(rOcDef, name, resource);
        List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
        if (shadows.isEmpty()) {
            return null;
        }
        assert shadows.size() == 1 : "Too many shadows found for name " + name + " on " + resource + ": " + shadows;
        return shadows.iterator().next();
    }

    protected ObjectQuery createAccountShadowQuery(String identifier, PrismObject<ResourceType> resource) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getPrimaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in " + resource + " refined schema: " + identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(identifierDef, ShadowType.F_ATTRIBUTES, identifierDef.getItemName()).eq(identifier)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getObjectClassDefinition().getTypeName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .build();
    }

    protected ObjectQuery createAccountShadowQuerySecondaryIdentifier(
            String identifier, PrismObject<ResourceType> resource) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        assertThat(rAccount)
                .withFailMessage("No RefinedObjectClassDefinition for %s", rSchema)
                .isNotNull();
        return createShadowQuerySecondaryIdentifier(rAccount, identifier, resource);
    }

    protected ObjectQuery createShadowQuerySecondaryIdentifier(
            ObjectClassComplexTypeDefinition rAccount, String identifier, PrismObject<ResourceType> resource) {
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getSecondaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in " + resource + " refined schema: " + identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(identifierDef, ShadowType.F_ATTRIBUTES, identifierDef.getItemName()).eq(identifier)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getTypeName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .build();
    }

    protected ObjectQuery createAccountShadowQueryByAttribute(String attributeName, String attributeValue, PrismObject<ResourceType> resource) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        return createShadowQueryByAttribute(rAccount, attributeName, attributeValue, resource);
    }

    protected ObjectQuery createShadowQueryByAttribute(ObjectClassComplexTypeDefinition rAccount, String attributeName, String attributeValue, PrismObject<ResourceType> resource) {
        ResourceAttributeDefinition<Object> attrDef = rAccount.findAttributeDefinition(attributeName);
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq(attributeValue)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getTypeName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
                .build();
    }

    protected ObjectQuery createOrgSubtreeQuery(String orgOid) {
        return queryFor(ObjectType.class)
                .isChildOf(orgOid)
                .build();
    }

    protected <O extends ObjectType> PrismObjectDefinition<O> getObjectDefinition(Class<O> type) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    }

    protected PrismObjectDefinition<UserType> getUserDefinition() {
        return getObjectDefinition(UserType.class);
    }

    protected PrismObjectDefinition<RoleType> getRoleDefinition() {
        return getObjectDefinition(RoleType.class);
    }

    protected PrismObjectDefinition<ShadowType> getShadowDefinition() {
        return getObjectDefinition(ShadowType.class);
    }

    // objectClassName may be null
    protected RefinedAttributeDefinition getAttributeDefinition(ResourceType resourceType,
            ShadowKindType kind, QName objectClassName, String attributeLocalName)
            throws SchemaException {
        RefinedResourceSchema refinedResourceSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition refinedObjectClassDefinition =
                refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClassName);
        return refinedObjectClassDefinition.findAttributeDefinition(attributeLocalName);
    }

    protected void assertPassword(ShadowType shadow, String expectedPassword) throws SchemaException, EncryptionException {
        CredentialsType credentials = shadow.getCredentials();
        assertNotNull("No credentials in " + shadow, credentials);
        PasswordType password = credentials.getPassword();
        assertNotNull("No password in " + shadow, password);
        ProtectedStringType passwordValue = password.getValue();
        assertNotNull("No password value in " + shadow, passwordValue);
        protector.decrypt(passwordValue);
        assertEquals("Wrong password in " + shadow, expectedPassword, passwordValue.getClearValue());
    }

    protected void assertPasswordDelta(ObjectDelta<ShadowType> shadowDelta) {
        ItemDelta<PrismValue, ItemDefinition> passwordDelta = shadowDelta.findItemDelta(SchemaConstants.PATH_PASSWORD_VALUE);
        assertNotNull("No password delta in " + shadowDelta, passwordDelta);

    }

    protected void assertFilter(ObjectFilter filter, Class<? extends ObjectFilter> expectedClass) {
        if (expectedClass == null) {
            assertNull("Expected that filter is null, but it was " + filter, filter);
        } else {
            assertNotNull("Expected that filter is of class " + expectedClass.getName() + ", but it was null", filter);
            if (!(expectedClass.isAssignableFrom(filter.getClass()))) {
                AssertJUnit.fail("Expected that filter is of class " + expectedClass.getName() + ", but it was " + filter);
            }
        }
    }

    protected void assertSyncToken(String syncTaskOid, Object expectedValue) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".assertSyncToken");
        Task task = taskManager.getTaskPlain(syncTaskOid, result);
        assertSyncToken(task, expectedValue);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assertSyncToken(String syncTaskOid, Object expectedValue, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Task task = taskManager.getTaskPlain(syncTaskOid, result);
        assertSyncToken(task, expectedValue);
    }

    protected void assertSyncToken(Task task, Object expectedValue) {
        PrismProperty<Object> syncTokenProperty = task.getExtensionPropertyOrClone(SchemaConstants.SYNC_TOKEN);
        if (expectedValue == null && syncTokenProperty == null) {
            return;
        }
        Object syncTokenPropertyValue = syncTokenProperty.getAnyRealValue();
        if (!MiscUtil.equals(expectedValue, syncTokenPropertyValue)) {
            AssertJUnit.fail("Wrong sync token, expected: " + expectedValue + (expectedValue == null ? "" : (", " + expectedValue.getClass().getName())) +
                    ", was: " + syncTokenPropertyValue + (syncTokenPropertyValue == null ? "" : (", " + syncTokenPropertyValue.getClass().getName())));
        }
    }

    protected void assertShadows(int expected) throws SchemaException {
        OperationResult result = new OperationResult("assertShadows");
        assertShadows(expected, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assertShadows(int expected, OperationResult result) throws SchemaException {
        int actual = repositoryService.countObjects(ShadowType.class, null, null, result);
        if (expected != actual) {
            if (actual > 20) {
                AssertJUnit.fail("Unexpected number of (repository) shadows. Expected " + expected + " but was " + actual + " (too many to display)");
            }
            ResultHandler<ShadowType> handler = (object, parentResult) -> {
                display("found shadow", object);
                return true;
            };
            repositoryService.searchObjectsIterative(ShadowType.class, null, handler, null, true, result);
            AssertJUnit.fail("Unexpected number of (repository) shadows. Expected " + expected + " but was " + actual);
        }
    }

    protected void assertShadowDead(PrismObject<ShadowType> shadow) {
        assertEquals("Shadow not dead: " + shadow, Boolean.TRUE, shadow.asObjectable().isDead());
    }

    protected void assertShadowNotDead(PrismObject<ShadowType> shadow) {
        assertTrue("Shadow not dead, but should not be: " + shadow, shadow.asObjectable().isDead() == null || Boolean.FALSE.equals(shadow.asObjectable().isDead()));
    }

    protected void assertShadowExists(PrismObject<ShadowType> shadow, Boolean expectedValue) {
        assertEquals("Wrong shadow 'exists': " + shadow, expectedValue, shadow.asObjectable().isExists());
    }

    protected void assertActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
        ActivationType activationType = shadow.asObjectable().getActivation();
        if (activationType == null) {
            if (expectedStatus == null) {
                return;
            } else {
                AssertJUnit.fail("Expected activation administrative status of " + shadow + " to be " + expectedStatus + ", but there was no activation administrative status");
            }
        } else {
            assertEquals("Wrong activation administrative status of " + shadow, expectedStatus, activationType.getAdministrativeStatus());
        }
    }

    protected void assertShadowLockout(PrismObject<ShadowType> shadow, LockoutStatusType expectedStatus) {
        ActivationType activationType = shadow.asObjectable().getActivation();
        if (activationType == null) {
            if (expectedStatus == null) {
                return;
            } else {
                AssertJUnit.fail("Expected lockout status of " + shadow + " to be " + expectedStatus + ", but there was no lockout status");
            }
        } else {
            assertEquals("Wrong lockout status of " + shadow, expectedStatus, activationType.getLockoutStatus());
        }
    }

    protected void assertUserLockout(PrismObject<UserType> user, LockoutStatusType expectedStatus) {
        ActivationType activationType = user.asObjectable().getActivation();
        if (activationType == null) {
            if (expectedStatus == null) {
                return;
            } else {
                AssertJUnit.fail("Expected lockout status of " + user + " to be " + expectedStatus + ", but there was no lockout status");
            }
        } else {
            assertEquals("Wrong lockout status of " + user, expectedStatus, activationType.getLockoutStatus());
        }
    }

    protected PolyString createPolyString(String string) {
        PolyString polyString = new PolyString(string);
        polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
        return polyString;
    }

    protected PolyStringType createPolyStringType(String string) {
        return new PolyStringType(createPolyString(string));
    }

    protected ItemPath getExtensionPath(QName propName) {
        return ItemPath.create(ObjectType.F_EXTENSION, propName);
    }

    protected void assertNumberOfAttributes(PrismObject<ShadowType> shadow, Integer expectedNumberOfAttributes) {
        PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("No attributes in repo shadow " + shadow, attributesContainer);
        Collection<Item<?, ?>> attributes = attributesContainer.getValue().getItems();

        assertFalse("Empty attributes in repo shadow " + shadow, attributes.isEmpty());
        if (expectedNumberOfAttributes != null) {
            assertEquals("Unexpected number of attributes in repo shadow " + shadow, (int) expectedNumberOfAttributes, attributes.size());
        }
    }

    protected ObjectReferenceType createRoleReference(String oid) {
        return createObjectReference(oid, RoleType.COMPLEX_TYPE, null);
    }

    protected ObjectReferenceType createOrgReference(String oid) {
        return createObjectReference(oid, OrgType.COMPLEX_TYPE, null);
    }

    protected ObjectReferenceType createOrgReference(String oid, QName relation) {
        return createObjectReference(oid, OrgType.COMPLEX_TYPE, relation);
    }

    protected ObjectReferenceType createObjectReference(String oid, QName type, QName relation) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        ref.setType(type);
        ref.setRelation(relation);
        return ref;
    }

    protected void assertNotReached() {
        AssertJUnit.fail("Unexpected success");
    }

    protected CredentialsStorageTypeType getPasswordStorageType() {
        return CredentialsStorageTypeType.ENCRYPTION;
    }

    protected CredentialsStorageTypeType getPasswordHistoryStorageType() {
        return CredentialsStorageTypeType.HASHING;
    }

    protected void assertEncryptedUserPassword(String userOid, String expectedClearPassword) throws EncryptionException, ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".assertEncryptedUserPassword");
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEncryptedUserPassword(user, expectedClearPassword);
    }

    protected void assertEncryptedUserPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException, SchemaException {
        assertUserPassword(user, expectedClearPassword, CredentialsStorageTypeType.ENCRYPTION);
    }

    protected PasswordType assertUserPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException, SchemaException {
        return assertUserPassword(user, expectedClearPassword, getPasswordStorageType());
    }

    protected PasswordType assertUserPassword(PrismObject<UserType> user, String expectedClearPassword, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
        UserType userType = user.asObjectable();
        CredentialsType creds = userType.getCredentials();
        assertNotNull("No credentials in " + user, creds);
        PasswordType password = creds.getPassword();
        assertNotNull("No password in " + user, password);
        ProtectedStringType protectedActualPassword = password.getValue();
        assertProtectedString("Password for " + user, expectedClearPassword, protectedActualPassword, storageType);
        return password;
    }

    protected void assertUserNoPassword(PrismObject<UserType> user) {
        UserType userType = user.asObjectable();
        CredentialsType creds = userType.getCredentials();
        if (creds != null) {
            PasswordType password = creds.getPassword();
            if (password != null) {
                assertNull("Unexpected password value in " + user, password.getValue());
            }
        }
    }

    protected void assertProtectedString(String message, String expectedClearValue, ProtectedStringType actualValue, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
        IntegrationTestTools.assertProtectedString(message, expectedClearValue, actualValue, storageType, protector);
    }

    protected boolean compareProtectedString(String expectedClearValue, ProtectedStringType actualValue, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
        switch (storageType) {

            case NONE:
                return actualValue == null;

            case ENCRYPTION:
                if (actualValue == null) {
                    return false;
                }
                if (!actualValue.isEncrypted()) {
                    return false;
                }
                String actualClearPassword = protector.decryptString(actualValue);
                return expectedClearValue.equals(actualClearPassword);

            case HASHING:
                if (actualValue == null) {
                    return false;
                }
                if (!actualValue.isHashed()) {
                    return false;
                }
                ProtectedStringType expectedPs = new ProtectedStringType();
                expectedPs.setClearValue(expectedClearValue);
                return protector.compareCleartext(actualValue, expectedPs);

            default:
                throw new IllegalArgumentException("Unknown storage " + storageType);
        }

    }

    protected <F extends FocusType> void assertPasswordHistoryEntries(PrismObject<F> focus, String... changedPasswords) {
        CredentialsType credentials = focus.asObjectable().getCredentials();
        assertNotNull("Null credentials in " + focus, credentials);
        PasswordType passwordType = credentials.getPassword();
        assertNotNull("Null passwordType in " + focus, passwordType);
        assertPasswordHistoryEntries(focus.toString(), passwordType.getHistoryEntry(), getPasswordHistoryStorageType(), changedPasswords);
    }

    protected void assertPasswordHistoryEntries(PasswordType passwordType, String... changedPasswords) {
        assertPasswordHistoryEntries(passwordType.getHistoryEntry(), changedPasswords);
    }

    protected void assertPasswordHistoryEntries(List<PasswordHistoryEntryType> historyEntriesType,
            String... changedPasswords) {
        assertPasswordHistoryEntries(null, historyEntriesType, getPasswordHistoryStorageType(), changedPasswords);
    }

    protected void assertPasswordHistoryEntries(String message, List<PasswordHistoryEntryType> historyEntriesType,
            CredentialsStorageTypeType storageType, String... changedPasswords) {
        if (message == null) {
            message = "";
        } else {
            message = message + ": ";
        }
        if (changedPasswords.length != historyEntriesType.size()) {
            AssertJUnit.fail(message + "Unexpected number of history entries, expected "
                    + Arrays.toString(changedPasswords) + "(" + changedPasswords.length + "), was "
                    + getPasswordHistoryHumanReadable(historyEntriesType) + "(" + historyEntriesType.size() + ")");
        }
        assertEquals(message + "Unexpected number of history entries", changedPasswords.length, historyEntriesType.size());
        for (PasswordHistoryEntryType historyEntry : historyEntriesType) {
            boolean found = false;
            try {
                for (String changedPassword : changedPasswords) {
                    if (compareProtectedString(changedPassword, historyEntry.getValue(), storageType)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    AssertJUnit.fail(message + "Unexpected value saved in between password hisotry entries: "
                            + getHumanReadablePassword(historyEntry.getValue())
                            + ". Expected " + Arrays.toString(changedPasswords) + "(" + changedPasswords.length + "), was "
                            + getPasswordHistoryHumanReadable(historyEntriesType) + "(" + historyEntriesType.size() + "); expected storage type: " + storageType);
                }
            } catch (EncryptionException | SchemaException e) {
                AssertJUnit.fail(message + "Could not encrypt password: " + e.getMessage());
            }

        }
    }

    protected String getPasswordHistoryHumanReadable(List<PasswordHistoryEntryType> historyEntriesType) {
        return historyEntriesType.stream()
                .map(historyEntry -> {
                    try {
                        return getHumanReadablePassword(historyEntry.getValue());
                    } catch (EncryptionException e) {
                        throw new SystemException(e.getMessage(), e);
                    }
                })
                .collect(Collectors.joining(", "));
    }

    protected String getHumanReadablePassword(ProtectedStringType ps) throws EncryptionException {
        if (ps == null) {
            return null;
        }
        if (ps.isEncrypted()) {
            return "[E:" + protector.decryptString(ps) + "]";
        }
        if (ps.isHashed()) {
            return "[H:" + ps.getHashedDataType().getDigestValue().length * 8 + "bit]";
        }
        return ps.getClearValue();
    }

    protected void logTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
                logger.debug("TrustManager(X509): {}", x509TrustManager);
                X509Certificate[] acceptedIssuers = x509TrustManager.getAcceptedIssuers();
                if (acceptedIssuers != null) {
                    for (X509Certificate acceptedIssuer : acceptedIssuers) {
                        logger.debug("    acceptedIssuer: {}", acceptedIssuer);
                    }
                }
            } else {
                logger.debug("TrustManager: {}", trustManager);
            }
        }
    }

    protected void setPassword(PrismObject<UserType> user, String password) {
        UserType userType = user.asObjectable();
        CredentialsType creds = userType.getCredentials();
        if (creds == null) {
            creds = new CredentialsType();
            userType.setCredentials(creds);
        }
        PasswordType passwordType = creds.getPassword();
        if (passwordType == null) {
            passwordType = new PasswordType();
            creds.setPassword(passwordType);
        }
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue(password);
        passwordType.setValue(ps);
    }

    protected void assertIncompleteShadowPassword(PrismObject<ShadowType> shadow) {
        PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
        assertNotNull("No password value property in " + shadow, passValProp);
        assertTrue("Password value property does not have 'incomplete' flag in " + shadow, passValProp.isIncomplete());
    }

    protected void assertNoShadowPassword(PrismObject<ShadowType> shadow) {
        PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
        assertNull("Unexpected password value property in " + shadow + ": " + passValProp, passValProp);
    }

    protected <O extends ObjectType> PrismObject<O> instantiateObject(Class<O> type) throws SchemaException {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type).instantiate();
    }

    protected void assertMetadata(String message, MetadataType metadataType, boolean create, boolean assertRequest,
            XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
        assertNotNull("No metadata in " + message, metadataType);
        if (create) {
            assertBetween("Wrong create timestamp in " + message, start, end, metadataType.getCreateTimestamp());
            if (actorOid != null) {
                ObjectReferenceType creatorRef = metadataType.getCreatorRef();
                assertNotNull("No creatorRef in " + message, creatorRef);
                assertEquals("Wrong creatorRef OID in " + message, actorOid, creatorRef.getOid());
                if (assertRequest) {
                    assertBetween("Wrong request timestamp in " + message, start, end, metadataType.getRequestTimestamp());
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
            assertBetween("Wrong password modify timestamp in " + message, start, end, metadataType.getModifyTimestamp());
            assertEquals("Wrong modification channel in " + message, channel, metadataType.getModifyChannel());
        }
    }

    protected void assertShadowPasswordMetadata(PrismObject<ShadowType> shadow, boolean passwordCreated,
            XMLGregorianCalendar startCal, XMLGregorianCalendar endCal, String actorOid, String channel) {
        CredentialsType creds = shadow.asObjectable().getCredentials();
        assertNotNull("No credentials in shadow " + shadow, creds);
        PasswordType password = creds.getPassword();
        assertNotNull("No password in shadow " + shadow, password);
        MetadataType metadata = password.getMetadata();
        assertNotNull("No metadata in shadow " + shadow, metadata);
        assertMetadata("Password metadata in " + shadow, metadata, passwordCreated, false, startCal, endCal, actorOid, channel);
    }

    protected <O extends ObjectType> void assertLastProvisioningTimestamp(
            PrismObject<O> object, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        MetadataType metadata = object.asObjectable().getMetadata();
        assertNotNull("No metadata in " + object, metadata);
        assertBetween("Wrong last provisioning timestamp in " + object, start, end, metadata.getLastProvisioningTimestamp());
    }

    // Convenience

    protected <O extends ObjectType> PrismObject<O> parseObject(File file) throws SchemaException, IOException {
        return prismContext.parseObject(file);
    }

    protected void displayCleanup() {
        TestUtil.displayCleanup(getTestNameShort());
    }

    protected void displaySkip() {
        TestUtil.displaySkip(getTestNameShort());
    }

    /**
     * Used to display string HEREHERE both in test output and in logfiles. This can be used
     * to conveniently correlate a place in the test, output and logfiles.
     */
    @SuppressWarnings("unused")
    protected void displayHEREHERE() {
        display(contextName() + "HEREHERE");
    }

    public static void display(String message, SearchResultEntry response) {
        IntegrationTestTools.display(message, response);
    }

    public static void display(Entry response) {
        IntegrationTestTools.display(response);
    }

    public static void display(String message, Task task) {
        IntegrationTestTools.display(message, task);
    }

    public static void display(String message, ObjectType o) {
        IntegrationTestTools.display(message, o);
    }

    public static void display(String message, Collection<?> collection) {
        IntegrationTestTools.display(message, collection);
    }

    public static void display(String title, Entry entry) {
        IntegrationTestTools.display(title, entry);
    }

    public static void display(String message, PrismContainer<?> propertyContainer) {
        IntegrationTestTools.display(message, propertyContainer);
    }

    public static void display(OperationResult result) {
        IntegrationTestTools.display(result);
    }

    public static void display(String title, OperationResult result) {
        IntegrationTestTools.display(title, result);
    }

    public static void display(String title, OperationResultType result) throws SchemaException {
        IntegrationTestTools.display(title, result);
    }

    public static void display(String title, List<Element> elements) {
        IntegrationTestTools.display(title, elements);
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }

    public static void display(String title, Containerable value) {
        IntegrationTestTools.display(title, value);
    }

    public static void displayPrismValuesCollection(String message, Collection<? extends PrismValue> collection) {
        IntegrationTestTools.displayPrismValuesCollection(message, collection);
    }

    public static void displayContainerablesCollection(String message, Collection<? extends Containerable> collection) {
        IntegrationTestTools.displayContainerablesCollection(message, collection);
    }

    public static void displayCollection(String message, Collection<? extends DebugDumpable> collection) {
        IntegrationTestTools.displayCollection(message, collection);
    }

    public static void displayObjectTypeCollection(String message, Collection<? extends ObjectType> collection) {
        IntegrationTestTools.displayObjectTypeCollection(message, collection);
    }

    protected void assertBetween(String message, XMLGregorianCalendar start, XMLGregorianCalendar end,
            XMLGregorianCalendar actual) {
        TestUtil.assertBetween(message, start, end, actual);
    }

    protected void assertBetween(String message, Long start, Long end,
            Long actual) {
        TestUtil.assertBetween(message, start, end, actual);
    }

    protected void assertFloat(String message, Integer expectedIntPercentage, Float actualPercentage) {
        assertFloat(message, expectedIntPercentage == null ? null : new Float(expectedIntPercentage), actualPercentage);
    }

    protected void assertFloat(String message, Float expectedPercentage, Float actualPercentage) {
        if (expectedPercentage == null) {
            if (actualPercentage == null) {
                return;
            } else {
                fail(message + ", expected: " + expectedPercentage + ", but was " + actualPercentage);
            }
        }
        //noinspection ConstantConditions
        if (actualPercentage > expectedPercentage + FLOAT_EPSILON || (actualPercentage < (expectedPercentage - FLOAT_EPSILON))) {
            fail(message + ", expected: " + expectedPercentage + ", but was " + actualPercentage);
        }
    }

    protected void setDefaultTracing(Task task) {
        setTracing(task, createDefaultTracingProfile());
    }

    protected void setModelLoggingTracing(Task task) {
        setTracing(task, createModelLoggingTracingProfile());
    }

    protected void setModelAndWorkflowLoggingTracing(Task task) {
        setTracing(task, addWorkflowLogging(createModelLoggingTracingProfile()));
        task.addTracingRequest(TracingRootType.WORKFLOW_OPERATION);
    }

    protected void setModelAndProvisioningLoggingTracing(Task task) {
        setTracing(task, addProvisioningLogging(createModelLoggingTracingProfile()));
    }

    protected void setHibernateLoggingTracing(Task task) {
        setTracing(task, createHibernateLoggingTracingProfile());
    }

    protected void setTracing(Task task, TracingProfileType profile) {
        task.addTracingRequest(TracingRootType.CLOCKWORK_RUN);
        task.setTracingProfile(profile);
    }

    protected static final String TEST_METHOD_TRACING_FILENAME_PATTERN =
            "trace %{timestamp} %{operationNameShort} %{milliseconds}";
    protected static final String DEFAULT_TRACING_FILENAME_PATTERN =
            "trace %{timestamp} %{testNameShort} %{operationNameShort} %{focusName} %{milliseconds}";

    protected TracingProfileType createModelLoggingTracingProfile() {
        return createDefaultTracingProfile()
                .beginLoggingOverride()
                .beginLevelOverride()
                .logger("com.evolveum.midpoint.model")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .end();
    }

    protected TracingProfileType addWorkflowLogging(TracingProfileType profile) {
        return profile.getLoggingOverride()
                .beginLevelOverride()
                .logger("com.evolveum.midpoint.wf")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .end();
    }

    protected TracingProfileType addNotificationsLogging(TracingProfileType profile) {
        return profile.getLoggingOverride()
                .beginLevelOverride()
                .logger("com.evolveum.midpoint.notifications")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .end();
    }

    protected TracingProfileType createModelAndWorkflowLoggingTracingProfile() {
        return addWorkflowLogging(createModelLoggingTracingProfile());
    }

    protected TracingProfileType createModelAndProvisioningLoggingTracingProfile() {
        return addProvisioningLogging(createModelLoggingTracingProfile());
    }

    protected TracingProfileType addProvisioningLogging(TracingProfileType profile) {
        return profile.getLoggingOverride()
                .beginLevelOverride()
                .logger("com.evolveum.midpoint.provisioning")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .end();
    }

    protected TracingProfileType createHibernateLoggingTracingProfile() {
        return createDefaultTracingProfile()
                .beginLoggingOverride()
                .beginLevelOverride()
                .logger("org.hibernate.SQL")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .beginLevelOverride()
                .logger("org.hibernate.type")
                .level(LoggingLevelType.TRACE)
                .<LoggingOverrideType>end()
                .end();
    }

    protected TracingProfileType createDefaultTracingProfile() {
        return new TracingProfileType()
                .collectLogEntries(true)
                .beginTracingTypeProfile()
                .level(TracingLevelType.NORMAL)
                .<TracingProfileType>end()
                .fileNamePattern(DEFAULT_TRACING_FILENAME_PATTERN);
    }

    protected TracingProfileType createPerformanceTracingProfile() {
        return new TracingProfileType()
                .beginTracingTypeProfile()
                .level(TracingLevelType.MINIMAL)
                .<TracingProfileType>end()
                .fileNamePattern(DEFAULT_TRACING_FILENAME_PATTERN);
    }

    protected Task createTracedTask() {
        return createTracedTask(null);
    }

    protected Task createTracedTask(String operationName) {
        Task task = createTask(operationName);
        task.addTracingRequest(TracingRootType.CLOCKWORK_RUN);
        task.setTracingProfile(new TracingProfileType()
                .collectLogEntries(true)
                .createRepoObject(false)        // to avoid influencing repo statistics
                .beginTracingTypeProfile()
                .level(TracingLevelType.NORMAL)
                .<TracingProfileType>end()
                .fileNamePattern(DEFAULT_TRACING_FILENAME_PATTERN));
        return task;
    }

    protected void assertSuccess(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        displayValue("Operation " + result.getOperation() + " result status", result.getStatus());
        TestUtil.assertSuccess(result);
    }

    protected void assertHadnledError(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        displayValue("Operation " + result.getOperation() + " result status", result.getStatus());
        TestUtil.assertResultStatus(result, OperationResultStatus.HANDLED_ERROR);
    }

    protected void assertSuccess(OperationResult result, int depth) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        displayValue("Operation " + result.getOperation() + " result status", result.getStatus());
        TestUtil.assertSuccess(result, depth);
    }

    protected void assertSuccess(String message, OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertSuccess(message, result);
    }

    protected void assertSuccess(String message, OperationResultType resultType) {
        TestUtil.assertSuccess(message, resultType);
    }

    protected void assertResultStatus(OperationResult result, OperationResultStatus expectedStatus) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        assertEquals("Unexpected operation " + result.getOperation() + " result status", expectedStatus, result.getStatus());
    }

    protected String assertInProgress(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        if (!OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
            String message = "Expected operation " + result.getOperation() + " status IN_PROGRESS, but result status was " + result.getStatus();
            display(message, result);
            fail(message);
        }
        return result.getAsynchronousOperationReference();
    }

    protected void assertFailure(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertFailure(result);
    }

    protected void assertFailure(String message, OperationResultType result) {
        TestUtil.assertFailure(message, result);
    }

    protected void assertPartialError(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertPartialError(result);
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    protected OperationResult assertSingleConnectorTestResult(OperationResult testResult) {
        return IntegrationTestTools.assertSingleConnectorTestResult(testResult);
    }

    protected void assertTestResourceSuccess(OperationResult testResult, ConnectorTestOperation operation) {
        IntegrationTestTools.assertTestResourceSuccess(testResult, operation);
    }

    protected void assertTestResourceFailure(OperationResult testResult, ConnectorTestOperation operation) {
        IntegrationTestTools.assertTestResourceFailure(testResult, operation);
    }

    protected void assertTestResourceNotApplicable(OperationResult testResult, ConnectorTestOperation operation) {
        IntegrationTestTools.assertTestResourceNotApplicable(testResult, operation);
    }

    protected <T> void assertAttribute(ShadowType shadow, QName attrQname, T... expectedValues) {
        List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
        PrismAsserts.assertSets("attribute " + attrQname + " in " + shadow, actualValues, expectedValues);
    }

    protected <T> void assertAttribute(ResourceType resourceType, ShadowType shadowType, String attrName,
            T... expectedValues) {
        assertAttribute(resourceType.asPrismObject(), shadowType, attrName, expectedValues);
    }

    protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName,
            T... expectedValues) {
        QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
        assertAttribute(shadow, attrQname, expectedValues);
    }

    protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, MatchingRule<T> matchingRule,
            QName attrQname, T... expectedValues) throws SchemaException {
        List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
        PrismAsserts.assertSets("attribute " + attrQname + " in " + shadow, matchingRule, actualValues, expectedValues);
    }

    protected void assertNoAttribute(ShadowType shadow, QName attrQname) {
        PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null || attributesContainer.isEmpty()) {
            return;
        }
        PrismProperty attribute = attributesContainer.findProperty(ItemName.fromQName(attrQname));
        assertNull("Unexpected attribute " + attrQname + " in " + shadow + ": " + attribute, attribute);
    }

    protected void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName) {
        QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
        assertNoAttribute(shadow, attrQname);
    }

    protected void assertNoPendingOperation(PrismObject<ShadowType> shadow) {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        assertEquals("Wrong number of pending operations in " + shadow, 0, pendingOperations.size());
    }

    protected void assertCaseState(String oid, String expectedState) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("assertCase");
        PrismObject<CaseType> acase = repositoryService.getObject(CaseType.class, oid, null, result);
        display("Case", acase);
        CaseType caseType = acase.asObjectable();
        String realState = caseType.getState();
        if (SchemaConstants.CASE_STATE_OPEN.equals(expectedState)) {
            assertTrue("Wrong state of " + acase + "; expected was open/created, real is " + realState,
                    SchemaConstants.CASE_STATE_OPEN.equals(realState) || SchemaConstants.CASE_STATE_CREATED.equals(realState));
        } else {
            assertEquals("Wrong state of " + acase, expectedState, realState);
        }
    }

    protected void closeCase(String caseOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = new OperationResult("closeCase");
        Collection modifications = new ArrayList<>(1);

        PrismPropertyDefinition<String> statePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_STATE);
        PropertyDelta<String> statusDelta = statePropertyDef.createEmptyDelta(CaseType.F_STATE);
        statusDelta.setRealValuesToReplace(SchemaConstants.CASE_STATE_CLOSED);
        modifications.add(statusDelta);

        PrismPropertyDefinition<String> outcomePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_OUTCOME);
        PropertyDelta<String> outcomeDelta = outcomePropertyDef.createEmptyDelta(CaseType.F_OUTCOME);
        outcomeDelta.setRealValuesToReplace(OperationResultStatusType.SUCCESS.value());
        modifications.add(outcomeDelta);

        repositoryService.modifyObject(CaseType.class, caseOid, modifications, null, result);

        PrismObject<CaseType> caseClosed = repositoryService.getObject(CaseType.class, caseOid, null, result);
        display("Case closed", caseClosed);
    }

    /**
     * Parses and adds full account into user as an object in linkRef.
     */
    protected void addAccountLinkRef(PrismObject<UserType> user, File accountFile) throws SchemaException, IOException {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(accountFile);
        ObjectReferenceType linkRef = new ObjectReferenceType();
        linkRef.asReferenceValue().setObject(account);
        user.asObjectable().getLinkRef().add(linkRef);
    }

    protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int expectedNumLinks) {
        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            assert expectedNumLinks == 0 : "Expected " + expectedNumLinks + " but " + focus + " has no linkRef";
            return;
        }
        assertEquals("Wrong number of links in " + focus, expectedNumLinks, linkRef.size());
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

    protected <F extends FocusType> void assertLinked(PrismObject<F> focus, String projectionOid) {
        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        assertNotNull("No linkRefs in " + focus, linkRef);
        boolean found = false;
        for (PrismReferenceValue val : linkRef.getValues()) {
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

    protected <F extends FocusType> void assertNotLinked(PrismObject<F> user, PrismObject<ShadowType> account) throws ObjectNotFoundException, SchemaException {
        assertNotLinked(user, account.getOid());
    }

    protected <F extends FocusType> void assertNotLinked(PrismObject<F> user, String accountOid) {
        PrismReference linkRef = user.findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            return;
        }
        boolean found = false;
        for (PrismReferenceValue val : linkRef.getValues()) {
            if (val.getOid().equals(accountOid)) {
                found = true;
            }
        }
        assertFalse("User " + user + " IS linked to account " + accountOid + " but not expecting it", found);
    }

    protected <F extends FocusType> void assertNoLinkedAccount(PrismObject<F> user) {
        PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
        if (accountRef == null) {
            return;
        }
        assert accountRef.isEmpty() : "Expected that " + user + " has no linked account but it has " + accountRef.size() + " linked accounts: "
                + accountRef.getValues();
    }

    protected <F extends FocusType> void assertPersonaLinks(PrismObject<F> focus, int expectedNumLinks) {
        PrismReference linkRef = focus.findReference(FocusType.F_PERSONA_REF);
        if (linkRef == null) {
            assert expectedNumLinks == 0 : "Expected " + expectedNumLinks + " but " + focus + " has no personaRef";
            return;
        }
        assertEquals("Wrong number of persona links in " + focus, expectedNumLinks, linkRef.size());
    }

    protected <F extends FocusType> void removeLinks(PrismObject<F> focus) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            return;
        }
        OperationResult result = new OperationResult("removeLinks");
        ReferenceDelta refDelta = linkRef.createDelta();
        refDelta.addValuesToDelete(linkRef.getClonedValues());
        repositoryService.modifyObject(focus.getCompileTimeClass(),
                focus.getOid(), MiscSchemaUtil.createCollection(refDelta), result);
        assertSuccess(result);
    }

    protected <O extends ObjectType> void assertObjectOids(String message, Collection<PrismObject<O>> objects, String... oids) {
        List<String> objectOids = objects.stream().map(o -> o.getOid()).collect(Collectors.toList());
        PrismAsserts.assertEqualsCollectionUnordered(message, objectOids, oids);
    }

    protected <T> void assertExpression(PrismProperty<T> prop, String evaluatorName) {
        PrismPropertyValue<T> pval = prop.getValue();
        ExpressionWrapper expressionWrapper = pval.getExpression();
        assertNotNull("No expression wrapper in " + prop, expressionWrapper);
        Object expressionObj = expressionWrapper.getExpression();
        assertNotNull("No expression in " + prop, expressionObj);
        assertTrue("Wrong expression type: " + expressionObj.getClass(), expressionObj instanceof ExpressionType);
        ExpressionType expressionType = (ExpressionType) expressionObj;
        JAXBElement<?> evaluatorElement = expressionType.getExpressionEvaluator().iterator().next();
        assertEquals("Wrong expression evaluator name", evaluatorName, evaluatorElement.getName().getLocalPart());
    }

    protected <O extends ObjectType> void assertNoRepoObject(Class<O> type, String oid) throws SchemaException {
        try {
            OperationResult result = createSubresult("assertNoRepoObject");
            PrismObject<O> object = repositoryService.getObject(type, oid, null, result);

            AssertJUnit.fail("Expected that " + object + " does not exist, in repo but it does");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }
    }

    protected void assertAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
        IntegrationTestTools.assertAssociation(shadow, associationName, entitlementOid);
    }

    protected void assertNoAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
        IntegrationTestTools.assertNoAssociation(shadow, associationName, entitlementOid);
    }

    protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, String... roleOids) {
        List<String> refOids = new ArrayList<>();
        for (ObjectReferenceType ref : focus.asObjectable().getRoleMembershipRef()) {
            refOids.add(ref.getOid());
            assertNotNull("Missing type in roleMembershipRef " + ref.getOid() + " in " + focus, ref.getType());
            // Name is not stored now
//            assertNotNull("Missing name in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getTargetName());
        }
        PrismAsserts.assertSets("Wrong values in roleMembershipRef in " + focus, refOids, roleOids);
    }

    protected <F extends FocusType> void assertRoleMembershipRefs(PrismObject<F> focus, Collection<String> roleOids) {
        List<String> refOids = new ArrayList<>();
        for (ObjectReferenceType ref : focus.asObjectable().getRoleMembershipRef()) {
            refOids.add(ref.getOid());
            assertNotNull("Missing type in roleMembershipRef " + ref.getOid() + " in " + focus, ref.getType());
            // Name is not stored now
//            assertNotNull("Missing name in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getTargetName());
        }
        PrismAsserts.assertSets("Wrong values in roleMembershipRef in " + focus, refOids, roleOids);
    }

    protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, QName relation, String... roleOids) {
        if (!MiscUtil.unorderedCollectionEquals(Arrays.asList(roleOids), focus.asObjectable().getRoleMembershipRef(),
                (expectedOid, hasRef) -> {
                    if (!expectedOid.equals(hasRef.getOid())) {
                        return false;
                    }
                    return prismContext.relationMatches(relation, hasRef.getRelation());
                })) {
            AssertJUnit.fail("Wrong values in roleMembershipRef in " + focus
                    + ", expected relation " + relation + ", OIDs " + Arrays.toString(roleOids)
                    + ", but was " + focus.asObjectable().getRoleMembershipRef());
        }
    }

    protected <F extends FocusType> void assertRoleMembershipRefs(PrismObject<F> focus, int expectedNumber) {
        List<ObjectReferenceType> roleMembershipRefs = focus.asObjectable().getRoleMembershipRef();
        assertEquals("Wrong number of roleMembershipRefs in " + focus, expectedNumber, roleMembershipRefs.size());
    }

    protected <F extends FocusType> void assertNoRoleMembershipRef(PrismObject<F> focus) {
        PrismReference memRef = focus.findReference(FocusType.F_ROLE_MEMBERSHIP_REF);
        assertNull("No roleMembershipRef expected in " + focus + ", but found: " + memRef, memRef);
    }

    protected void generateRoles(int numberOfRoles, String nameFormat, String oidFormat, BiConsumer<RoleType, Integer> mutator, OperationResult result) throws Exception {
        generateObjects(RoleType.class, numberOfRoles, nameFormat, oidFormat, mutator, role -> repositoryService.addObject(role, null, result), result);
    }

    protected void generateUsers(int numberOfUsers, String nameFormat, String oidFormat, BiConsumer<UserType, Integer> mutator, OperationResult result) throws Exception {
        generateObjects(UserType.class, numberOfUsers, nameFormat, oidFormat, mutator, user -> repositoryService.addObject(user, null, result), result);
    }

    protected <O extends ObjectType> void generateObjects(Class<O> type, int numberOfObjects, String nameFormat, String oidFormat, BiConsumer<O, Integer> mutator, FailableProcessor<PrismObject<O>> adder, OperationResult result) throws Exception {
        long startMillis = System.currentTimeMillis();

        PrismObjectDefinition<O> objectDefinition = getObjectDefinition(type);
        for (int i = 0; i < numberOfObjects; i++) {
            PrismObject<O> object = objectDefinition.instantiate();
            O objectType = object.asObjectable();
            String name = String.format(nameFormat, i);
            objectType.setName(createPolyStringType(name));
            if (oidFormat != null) {
                String oid = String.format(oidFormat, i);
                objectType.setOid(oid);
            }
            if (mutator != null) {
                mutator.accept(objectType, i);
            }
            logger.info("Adding {}:\n{}", object, object.debugDump(1));
            adder.process(object);
        }

        long endMillis = System.currentTimeMillis();
        long duration = (endMillis - startMillis);
        displayValue(type.getSimpleName() + " import", "import of " + numberOfObjects + " roles took " + (duration / 1000) + " seconds (" + (duration / numberOfObjects) + "ms per object)");
    }

    protected String assignmentSummary(PrismObject<UserType> user) {
        Map<String, Integer> assignmentRelations = new HashMap<>();
        for (AssignmentType assignment : user.asObjectable().getAssignment()) {
            relationToMap(assignmentRelations, assignment.getTargetRef());
        }
        Map<String, Integer> memRelations = new HashMap<>();
        for (ObjectReferenceType ref : user.asObjectable().getRoleMembershipRef()) {
            relationToMap(memRelations, ref);
        }
        Map<String, Integer> parents = new HashMap<>();
        for (ObjectReferenceType ref : user.asObjectable().getParentOrgRef()) {
            relationToMap(parents, ref);
        }
        return "User " + user
                + "\n  " + user.asObjectable().getAssignment().size() + " assignments\n    " + assignmentRelations
                + "\n  " + user.asObjectable().getRoleMembershipRef().size() + " roleMembershipRefs\n    " + memRelations
                + "\n  " + user.asObjectable().getParentOrgRef().size() + " parentOrgRefs\n    " + parents;
    }

    private void relationToMap(Map<String, Integer> map, ObjectReferenceType ref) {
        if (ref != null) {
            String relation = null;
            if (ref.getRelation() != null) {
                relation = ref.getRelation().getLocalPart();
            }
            Integer i = map.get(relation);
            if (i == null) {
                i = 0;
            }
            i++;
            map.put(relation, i);
        }
    }

    protected S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass) {
        return prismContext.queryFor(queryClass);
    }

    protected void displayCounters(InternalCounters... counters) {
        StringBuilder sb = new StringBuilder();
        for (InternalCounters counter : counters) {
            sb
                    .append("  ")
                    .append(counter.getLabel()).append(": ")
                    .append("+").append(getCounterIncrement(counter))
                    .append(" (").append(InternalMonitor.getCount(counter)).append(")")
                    .append("\n");
        }
        displayValue("Counters", sb.toString());
    }

    protected void assertMessageContains(String message, String string) {
        assert message.contains(string) : "Expected message to contain '" + string + "' but it does not; message: " + message;
    }

    protected void assertExceptionUserFriendly(CommonException e, String expectedMessage) {
        LocalizableMessage userFriendlyMessage = e.getUserFriendlyMessage();
        assertNotNull("No user friendly exception message", userFriendlyMessage);
        assertEquals("Unexpected user friendly exception fallback message", expectedMessage, userFriendlyMessage.getFallbackMessage());
    }

    protected ParallelTestThread[] multithread(MultithreadRunner lambda, int numberOfThreads, Integer randomStartDelayRange) {
        return TestUtil.multithread(lambda, numberOfThreads, randomStartDelayRange);
    }

    protected void randomDelay(Integer range) {
        TestUtil.randomDelay(range);
    }

    protected void waitForThreads(ParallelTestThread[] threads, long timeout) throws InterruptedException {
        TestUtil.waitForThreads(threads, timeout);
    }

    protected ItemPath getMetadataPath(QName propName) {
        return ItemPath.create(ObjectType.F_METADATA, propName);
    }

    protected boolean isOsUnix() {
        return SystemUtils.IS_OS_UNIX;
    }

    protected String getTranslatedMessage(CommonException e) {
        if (e.getUserFriendlyMessage() != null) {
            return localizationService.translate(e.getUserFriendlyMessage(), Locale.US);
        } else {
            return e.getMessage();
        }
    }

    protected void assertMessage(CommonException e, String expectedMessage) {
        String realMessage = getTranslatedMessage(e);
        assertEquals("Wrong message", expectedMessage, realMessage);
    }

    protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
        return prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, userOid, propertyName, newRealValue);
    }

    protected ObjectDelta<UserType> createModifyUserAddDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
        return prismContext.deltaFactory().object()
                .createModificationAddProperty(UserType.class, userOid, propertyName, newRealValue);
    }

    protected ObjectDelta<UserType> createModifyUserDeleteDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
        return prismContext.deltaFactory().object()
                .createModificationDeleteProperty(UserType.class, userOid, propertyName, newRealValue);
    }

    protected ObjectDelta<ShadowType> createModifyAccountShadowEmptyDelta(String accountOid) {
        return prismContext.deltaFactory().object().createEmptyModifyDelta(ShadowType.class, accountOid);
    }

    protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid,
            PrismObject<ResourceType> resource, String attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        return createModifyAccountShadowReplaceAttributeDelta(accountOid, resource, getAttributeQName(resource, attributeName), newRealValue);
    }

    protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid,
            PrismObject<ResourceType> resource, QName attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        return createModifyAccountShadowReplaceDelta(accountOid, resource, ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName), newRealValue);
    }

    protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceDelta(String accountOid, PrismObject<ResourceType> resource, ItemPath itemPath, Object... newRealValue) throws SchemaException {
        if (itemPath.startsWithName(ShadowType.F_ATTRIBUTES)) {
            PropertyDelta<?> attributeDelta = createAttributeReplaceDelta(resource, ItemPath.toName(itemPath.last()), newRealValue);
            ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                    .createModifyDelta(accountOid, attributeDelta, ShadowType.class);
            return accountDelta;
        } else {
            ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                    ShadowType.class, accountOid, itemPath, newRealValue);
            return accountDelta;
        }
    }

    protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
        return createAttributeReplaceDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
    }

    protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
        PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
        if (attributeDefinition == null) {
            throw new SchemaException("No definition for attribute " + attributeQName + " in " + resource);
        }
        return prismContext.deltaFactory().property().createModificationReplaceProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeQName),
                attributeDefinition, newRealValue);
    }

    protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
        return createAttributeAddDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
    }

    protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
        PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
        if (attributeDefinition == null) {
            throw new SchemaException("No definition for attribute " + attributeQName + " in " + resource);
        }
        return prismContext.deltaFactory().property().createModificationAddProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeQName),
                attributeDefinition, newRealValue);
    }

    protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
        return createAttributeDeleteDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
    }

    protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
        PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
        if (attributeDefinition == null) {
            throw new SchemaException("No definition for attribute " + attributeQName + " in " + resource);
        }
        return prismContext.deltaFactory().property().createModificationDeleteProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeQName),
                attributeDefinition, newRealValue);
    }

    protected ResourceAttributeDefinition getAttributeDefinition(PrismObject<ResourceType> resource, QName attributeName) throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        if (refinedSchema == null) {
            throw new SchemaException("No refined schema for " + resource);
        }
        RefinedObjectClassDefinition accountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        return accountDefinition.findAttributeDefinition(attributeName);
    }

    protected ObjectDelta<ShadowType> createModifyAccountShadowAddDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
        return prismContext.deltaFactory().object()
                .createModificationAddProperty(ShadowType.class, accountOid, propertyName, newRealValue);
    }

    protected QName getAttributeQName(PrismObject<ResourceType> resource, String attributeLocalName) {
        String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
        return new QName(resourceNamespace, attributeLocalName);
    }

    protected ItemPath getAttributePath(PrismObject<ResourceType> resource, String attributeLocalName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeQName(resource, attributeLocalName));
    }

    protected ObjectDelta<ShadowType> createAccountPaswordDelta(String shadowOid, String newPassword, String oldPassword) throws SchemaException {
        ProtectedStringType newPasswordPs = new ProtectedStringType();
        newPasswordPs.setClearValue(newPassword);
        ProtectedStringType oldPasswordPs = null;
        if (oldPassword != null) {
            oldPasswordPs = new ProtectedStringType();
            oldPasswordPs.setClearValue(oldPassword);
        }
        return deltaFor(ShadowType.class)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .oldRealValue(oldPasswordPs)
                .replace(newPasswordPs)
                .asObjectDelta(shadowOid);
    }

    protected PrismObject<ShadowType> getShadowRepo(String shadowOid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getShadowRepo");
        // We need to read the shadow as raw, so repo will look for some kind of rudimentary attribute
        // definitions here. Otherwise we will end up with raw values for non-indexed (cached) attributes
        logger.info("Getting repo shadow {}", shadowOid);
        PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowOid, GetOperationOptions.createRawCollection(), result);
        logger.info("Got repo shadow\n{}", shadow.debugDumpLazily(1));
        assertSuccess(result);
        return shadow;
    }

    protected Collection<ObjectDelta<? extends ObjectType>> createDetlaCollection(ObjectDelta<?>... deltas) {
        return (Collection) MiscUtil.createCollection(deltas);
    }

    public static String getAttributeValue(ShadowType repoShadow, QName name) {
        return IntegrationTestTools.getAttributeValue(repoShadow, name);
    }

    /**
     * Convenience method for shadow values that are read directly from repo (post-3.8).
     * This may ruin the "rawness" of the value. But it is OK for test asserts.
     */
    protected <T> T getAttributeValue(PrismObject<? extends ShadowType> shadow, QName attrName, Class<T> expectedClass) throws SchemaException {
        Object value = ShadowUtil.getAttributeValue(shadow, attrName);
        if (value == null) {
            return (T) value;
        }
        if (expectedClass.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        if (value instanceof RawType) {
            T parsedRealValue = ((RawType) value).getParsedRealValue(expectedClass);
            return parsedRealValue;
        }
        fail("Expected that attribute " + attrName + " is " + expectedClass + ", but it was " + value.getClass() + ": " + value);
        return null; // not reached
    }

    protected void assertApproxNumberOfAllResults(SearchResultMetadata searchMetadata, Integer expectedNumber) {
        if (expectedNumber == null) {
            if (searchMetadata == null) {
                return;
            }
            assertNull("Unexpected approximate number of search results in search metadata, expected null but was " + searchMetadata.getApproxNumberOfAllResults(), searchMetadata.getApproxNumberOfAllResults());
        } else {
            assertEquals("Wrong approximate number of search results in search metadata", expectedNumber, searchMetadata.getApproxNumberOfAllResults());
        }
    }

    protected void assertEqualTime(String message, String isoTime, ZonedDateTime actualTime) {
        assertEqualTime(message, ZonedDateTime.parse(isoTime), actualTime);
    }

    protected void assertEqualTime(String message, ZonedDateTime expectedTime, ZonedDateTime actualTime) {
        assertTrue(message + "; expected " + expectedTime + ", but was " + actualTime, expectedTime.isEqual(actualTime));
    }

    protected XMLGregorianCalendar getTimestamp(String duration) {
        return XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), duration);
    }

    protected void clockForward(String duration) {
        XMLGregorianCalendar before = clock.currentTimeXMLGregorianCalendar();
        clock.overrideDuration(duration);
        XMLGregorianCalendar after = clock.currentTimeXMLGregorianCalendar();
        displayValue("Clock going forward", before + " --[" + duration + "]--> " + after);
    }

    protected void assertRelationDef(List<RelationDefinitionType> relations, QName qname, String expectedLabel) {
        RelationDefinitionType relDef = ObjectTypeUtil.findRelationDefinition(relations, qname);
        assertNotNull("No definition for relation " + qname, relDef);
        assertEquals("Wrong relation " + qname + " label", expectedLabel, relDef.getDisplay().getLabel().getOrig());
    }

    protected void initializeAsserter(AbstractAsserter<?> asserter) {
        asserter.setPrismContext(prismContext);
        asserter.setObjectResolver(repoSimpleObjectResolver);
        asserter.setRepositoryService(repositoryService);
        asserter.setProtector(protector);
        asserter.setClock(clock);
    }

    protected PolyStringAsserter<Void> assertPolyString(PolyString polystring, String desc) {
        PolyStringAsserter<Void> asserter = new PolyStringAsserter<>(polystring, desc);
        initializeAsserter(asserter);
        return asserter;
    }

    protected RefinedResourceSchemaAsserter<Void> assertRefinedResourceSchema(PrismObject<ResourceType> resource, String details) throws SchemaException {
        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, prismContext);
        assertNotNull("No refined schema for " + resource + " (" + details + ")", refinedSchema);
        RefinedResourceSchemaAsserter<Void> asserter = new RefinedResourceSchemaAsserter(refinedSchema, resource.toString() + " (" + details + ")");
        initializeAsserter(asserter);
        return asserter;
    }

    protected ShadowAsserter<Void> assertShadow(PrismObject<ShadowType> shadow, String details) throws ObjectNotFoundException {
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(shadow, details);
        initializeAsserter(asserter);
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertRepoShadow(String oid) throws ObjectNotFoundException, SchemaException {
        PrismObject<ShadowType> repoShadow = getShadowRepo(oid);
        ShadowAsserter<Void> asserter = assertShadow(repoShadow, "repository");
        initializeAsserter(asserter);
        asserter.assertBasicRepoProperties();
        return asserter;
    }

    protected void assertNoRepoShadow(String oid) throws SchemaException {
        OperationResult result = new OperationResult("assertNoRepoShadow");
        try {
            PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, oid, GetOperationOptions.createRawCollection(), result);
            fail("Expected that shadow " + oid + " will not be in the repo. But it was: " + shadow);
        } catch (ObjectNotFoundException e) {
            // Expected
            assertFailure(result);
        }
    }

    protected void markShadowTombstone(String oid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = createSubresult("markShadowTombstone");
        List<ItemDelta<?, ?>> deadModifications = deltaFor(ShadowType.class)
                .item(ShadowType.F_DEAD).replace(true)
                .item(ShadowType.F_EXISTS).replace(false)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, oid, deadModifications, result);
        assertSuccess(result);
    }

    protected XMLGregorianCalendar addDuration(XMLGregorianCalendar time, Duration duration) {
        return XmlTypeConverter.addDuration(time, duration);
    }

    protected XMLGregorianCalendar addDuration(XMLGregorianCalendar time, String duration) {
        return XmlTypeConverter.addDuration(time, duration);
    }

    protected void displayCurrentTime() {
        displayValue("Current time", clock.currentTimeXMLGregorianCalendar());
    }

    protected QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> retrieveItemsNamed(Object... items) {
        return schemaHelper.getOperationOptionsBuilder()
                .items(items).retrieve()
                .build();
    }

    protected GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return schemaHelper.getOperationOptionsBuilder();
    }

    @NotNull
    protected Collection<SelectorOptions<GetOperationOptions>> retrieveTaskResult() {
        return getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .build();
    }

    // use only if necessary (use ItemPath.create instead)
    protected ItemPath path(Object... components) {
        return ItemPath.create(components);
    }

    protected ItemFactory itemFactory() {
        return prismContext.itemFactory();
    }

    protected void setModelAndProvisioningLoggers(Level level) {
        setModelLoggers(level);
        setProvisioningLoggers(level);
    }

    protected void setModelLoggers(Level level) {
        setLoggers(Collections.singletonList("com.evolveum.midpoint.model"), level);
    }

    protected void setProvisioningLoggers(Level level) {
        setLoggers(Collections.singletonList("com.evolveum.midpoint.provisioning"), level);
    }

    protected void setLoggers(List<String> prefixes, Level level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        int set = 0;
        for (Logger logger : context.getLoggerList()) {
            if (prefixes.stream().anyMatch(prefix -> logger.getName().startsWith(prefix))) {
                logger.setLevel(level);
                set++;
            }
        }
        System.out.println("Loggers set to " + level + ": " + set);
    }

    protected void clearLogFile() {
        try {
            RandomAccessFile file = new RandomAccessFile(new File("target/test.log"), "rw");
            file.setLength(0);
            file.close();
            System.out.println("Log file truncated");
        } catch (IOException e) {
            System.err.println("Couldn't truncate log file");
            e.printStackTrace(System.err);
        }
    }

    protected void setGlobalTracingOverride(@NotNull TracingProfileType profile) {
        List<TracingRootType> roots = Arrays.asList(
                TracingRootType.CLOCKWORK_RUN,
                TracingRootType.ITERATIVE_TASK_OBJECT_PROCESSING,
                TracingRootType.ASYNCHRONOUS_MESSAGE_PROCESSING,
                TracingRootType.LIVE_SYNC_CHANGE_PROCESSING,
                TracingRootType.WORKFLOW_OPERATION
                // RETRIEVED_RESOURCE_OBJECT_PROCESSING is invoked too frequently to be universally enabled
        );
        taskManager.setGlobalTracingOverride(roots, profile);
    }

    protected void setGlobalTracingOverrideAll(@NotNull TracingProfileType profile) {
        taskManager.setGlobalTracingOverride(Arrays.asList(TracingRootType.values()), profile);
    }

    protected void unsetGlobalTracingOverride() {
        taskManager.unsetGlobalTracingOverride();
    }

    protected Consumer<PrismObject<TaskType>> workerThreadsCustomizer(int threads) {
        return taskObject -> {
            if (threads != 0) {
                //noinspection unchecked
                PrismProperty<Integer> workerThreadsProperty = prismContext.getSchemaRegistry()
                        .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS)
                        .instantiate();
                workerThreadsProperty.setRealValue(threads);
                try {
                    taskObject.addExtensionItem(workerThreadsProperty);
                } catch (SchemaException e) {
                    throw new AssertionError(e);
                }
            }
        };
    }
}
