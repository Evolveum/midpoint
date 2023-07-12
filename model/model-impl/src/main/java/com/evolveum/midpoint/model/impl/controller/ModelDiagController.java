/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.SystemUtil;
import com.evolveum.midpoint.model.api.DataModelVisualizer;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 */
@Component
public class ModelDiagController implements ModelDiagnosticService {

    public static final String CLASS_NAME_WITH_DOT = ModelDiagController.class.getName() + ".";
    private static final String REPOSITORY_SELF_TEST_USER = CLASS_NAME_WITH_DOT + "repositorySelfTest.user";
    private static final String REPOSITORY_SELF_TEST_LOOKUP_TABLE = CLASS_NAME_WITH_DOT + "repositorySelfTest.lookupTable";
    private static final String EXPORT_DATA_MODEL = CLASS_NAME_WITH_DOT + "exportDataModel";

    private static final String NAME_PREFIX = "selftest";
    private static final int NAME_RANDOM_LENGTH = 5;

    private static final String USER_FULL_NAME = "Grăfula Fèlix Teleke z Tölökö";
    private static final String USER_GIVEN_NAME = "Fëľïx";
    private static final String USER_FAMILY_NAME = "Ţæĺêké";
    private static final String[] USER_ORGANIZATION = { "COMITATVS NOBILITVS HVNGARIÆ", "Salsa Verde ğomorula prïvatûła" };
    private static final String[] USER_SUBTYPE = { "Ģŗąfųŀą", "CANTATOR" };

    private static final String INSANE_NATIONAL_STRING = "Pørúga ném nå väšȍm apârátula";

    private static final Trace LOGGER = TraceManager.getTrace(ModelDiagController.class);

    private static final long HEAP_DIAG_TIMEOUT = 120000;
    private static final String DEFAULT_HEAP_INFO_COMMAND = "${jhsdb} jmap --heap --pid ${pid}";
    private static final String DEFAULT_HEAP_OBJECTS_HISTOGRAM_COMMAND = "${jmap} -histo:live ${pid}";

    @Autowired private DataModelVisualizer dataModelVisualizer;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;

    @Autowired
    @Qualifier("repositoryService")
    private RepositoryService repositoryService;

    @Autowired private ProvisioningService provisioningService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private MappingDiagEvaluator mappingDiagEvaluator;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private SystemObjectCache systemObjectCache;

    private final RandomString randomString;

    ModelDiagController() {
        randomString = new RandomString(NAME_RANDOM_LENGTH, true);
    }

    @Override
    public RepositoryDiag getRepositoryDiag(Task task, OperationResult parentResult) {
        return repositoryService.getRepositoryDiag();
    }

    @Override
    public OperationResult repositorySelfTest(Task task) {
        OperationResult testResult = new OperationResult(REPOSITORY_SELF_TEST);
        // Give repository chance to run its own self-test if available
        repositoryService.repositorySelfTest(testResult);

        repositorySelfTestUser(testResult);
        repositorySelfTestLookupTable(testResult);

        testResult.computeStatus();
        return testResult;
    }

    @Override
    public void repositoryTestOrgClosureConsistency(
            Task task, boolean repairIfNecessary, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.createSubresult(REPOSITORY_TEST_ORG_CLOSURE_CONSISTENCY);
        try {
            securityEnforcer.authorizeAll(task, result); // only admin can do this
            repositoryService.testOrgClosureConsistency(repairIfNecessary, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public RepositoryQueryDiagResponse executeRepositoryQuery(
            RepositoryQueryDiagRequest request, Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {
        OperationResult result = parentResult.createSubresult(EXECUTE_REPOSITORY_QUERY);
        try {
            boolean isAdmin;
            if (request.isTranslateOnly()) {
                // special case - no hibernate query and translate-only: does not require authorization
                isAdmin = false;
            } else {
                // otherwise admin authorization is required
                securityEnforcer.authorizeAll(task, result);
                isAdmin = true;
            }
            RepositoryQueryDiagResponse response = repositoryService.executeQueryDiagnostics(request, result);
            if (!isAdmin && response.getQueryResult() != null) {
                // double check we don't leak any data
                throw new IllegalStateException("Unauthorized access yields returning data from the repository");
            }
            return response;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public MappingEvaluationResponseType evaluateMapping(
            MappingEvaluationRequestType request, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, SecurityViolationException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(EVALUATE_MAPPING);
        try {
            securityEnforcer.authorizeAll(task, result);
            return mappingDiagEvaluator.evaluateMapping(request, task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public @NotNull AuthorizationEvaluationResponseType evaluateAuthorizations(
            @NotNull AuthorizationEvaluationRequestType request, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException {
        OperationResult result = parentResult.createSubresult(EVALUATE_AUTHORIZATIONS);
        try {
            securityEnforcer.authorizeAll(task, result); // May be lifted in the future to allow e.g. power users to do this
            return AuthorizationDiagEvaluation.of(request, task)
                    .evaluate(result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResult provisioningSelfTest(Task task) {
        OperationResult testResult = new OperationResult(PROVISIONING_SELF_TEST);
        // Give provisioning chance to run its own self-test
        provisioningService.provisioningSelfTest(testResult, task);

        testResult.computeStatus();
        return testResult;
    }

    @Override
    public ProvisioningDiag getProvisioningDiag(Task task, OperationResult parentResult) {
        return provisioningService.getProvisioningDiag();
    }

    private void repositorySelfTestUser(OperationResult testResult) {
        OperationResult result = testResult.createSubresult(REPOSITORY_SELF_TEST_USER);

        PrismObject<UserType> user;
        try {
            user = getObjectDefinition(UserType.class).instantiate();
        } catch (SchemaException e) {
            result.recordFatalError(e);
            return;
        }
        UserType userType = user.asObjectable();

        String name = generateRandomName();
        PolyStringType namePolyStringType = toPolyStringType(name);
        userType.setName(namePolyStringType);
        result.addContext("name", name);
        userType.setDescription(SelfTestData.POLICIJA);
        userType.setFullName(toPolyStringType(USER_FULL_NAME));
        userType.setGivenName(toPolyStringType(USER_GIVEN_NAME));
        userType.setFamilyName(toPolyStringType(USER_FAMILY_NAME));
        userType.setTitle(toPolyStringType(INSANE_NATIONAL_STRING));
        userType.subtype(USER_SUBTYPE[0]);
        userType.subtype(USER_SUBTYPE[1]);
        userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[0]));
        userType.getOrganization().add(toPolyStringType(USER_ORGANIZATION[1]));

        String oid;
        try {
            oid = repositoryService.addObject(user, null, result);
        } catch (ObjectAlreadyExistsException | SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            return;
        }

        try {

            if (repositorySelfTestUserGet(name, oid, result)) {
                return;
            }

            if (repositorySelfTestUserSearch(name, UserType.F_FULL_NAME, toPolyString(USER_FULL_NAME), result)) {
                return;
            }

            // MID-1116
            if (repositorySelfTestUserSearch(name, UserType.F_SUBTYPE, USER_SUBTYPE[0], result)) {
                return;
            }

            // MID-1116
            if (repositorySelfTestUserSearch(name, UserType.F_ORGANIZATION, toPolyString(USER_ORGANIZATION[1]), result)) {
                return;
            }

        } finally {

            try {
                repositoryService.deleteObject(UserType.class, oid, testResult);
            } catch (ObjectNotFoundException | RuntimeException e) {
                result.recordFatalError(e);
                return;
            }

            result.computeStatus();
        }

    }

    private boolean repositorySelfTestUserSearch(
            String userName, ItemName itemName, Object itemValue, OperationResult result) {
        OperationResult subresult = result.createSubresult(
                result.getOperation() + ".searchObjects" + itemName.getLocalPart());
        try {

            ObjectQuery query = prismContext.queryFor(UserType.class)
                    .item(itemName).eq(itemValue)
                    .build();
            subresult.addParam("query", query);
            List<PrismObject<UserType>> foundObjects = repositoryService.searchObjects(UserType.class, query, null, subresult);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Self-test:user searchObjects:\n{}", DebugUtil.debugDump(foundObjects));
            }
            assertSingleSearchResult("user", foundObjects, subresult);

            PrismObject<UserType> userRetrieved = foundObjects.iterator().next();
            checkUser(userRetrieved, userName, subresult);

            subresult.recordSuccessIfUnknown();
        } catch (SchemaException | RuntimeException e) {
            subresult.recordFatalError(e);
            return true;
        }
        return false;
    }

    private boolean repositorySelfTestUserGet(String name, String oid, OperationResult result) {
        OperationResult subresult = result.createSubresult(result.getOperation() + ".getObject");

        PrismObject<UserType> userRetrieved;
        try {
            userRetrieved = repositoryService.getObject(UserType.class, oid, null, subresult);
        } catch (ObjectNotFoundException | SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            return true;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Self-test:user getObject:\n{}", userRetrieved.debugDump());
        }

        checkUser(userRetrieved, name, subresult);

        subresult.recordSuccessIfUnknown();
        return false;
    }

    private void checkUser(PrismObject<UserType> userRetrieved, String name, OperationResult subresult) {
        checkObjectPropertyPolyString(userRetrieved, UserType.F_NAME, subresult, name);
        checkObjectProperty(userRetrieved, UserType.F_DESCRIPTION, subresult, SelfTestData.POLICIJA);
        checkObjectPropertyPolyString(userRetrieved, UserType.F_FULL_NAME, subresult, USER_FULL_NAME);
        checkObjectPropertyPolyString(userRetrieved, UserType.F_GIVEN_NAME, subresult, USER_GIVEN_NAME);
        checkObjectPropertyPolyString(userRetrieved, UserType.F_FAMILY_NAME, subresult, USER_FAMILY_NAME);
        checkObjectPropertyPolyString(userRetrieved, UserType.F_TITLE, subresult, INSANE_NATIONAL_STRING);
        checkObjectProperty(userRetrieved, UserType.F_SUBTYPE, subresult, USER_SUBTYPE);
        checkObjectPropertyPolyString(userRetrieved, UserType.F_ORGANIZATION, subresult, USER_ORGANIZATION);
    }

    private void repositorySelfTestLookupTable(OperationResult testResult) {
        OperationResult result = testResult.createSubresult(REPOSITORY_SELF_TEST_LOOKUP_TABLE);

        PrismObject<LookupTableType> lookupTable;
        try {
            lookupTable = getObjectDefinition(LookupTableType.class).instantiate();
        } catch (SchemaException e) {
            result.recordFatalError(e);
            return;
        }
        LookupTableType lookupTableType = lookupTable.asObjectable();

        String name = generateRandomName();
        PolyStringType namePolyStringType = toPolyStringType(name);
        lookupTableType.setName(namePolyStringType);
        result.addContext("name", name);
        lookupTableType.setDescription(SelfTestData.POLICIJA);

        LookupTableRowType rowType = new LookupTableRowType(prismContext);
        rowType.setKey(INSANE_NATIONAL_STRING);
        rowType.setValue(INSANE_NATIONAL_STRING);
        rowType.setLabel(toPolyStringType(INSANE_NATIONAL_STRING));
        lookupTableType.getRow().add(rowType);

        String oid;
        try {
            oid = repositoryService.addObject(lookupTable, null, result);
        } catch (ObjectAlreadyExistsException | SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            return;
        }

        try {
            if (repositorySelfTestLookupTableGet(result, name, oid)) {
                return;
            }

            if (repositorySelfTestLookupTableGetKey(result, name, oid)) {
                return;
            }

        } finally {
            try {
                repositoryService.deleteObject(LookupTableType.class, oid, testResult);
            } catch (ObjectNotFoundException | RuntimeException e) {
                result.recordFatalError(e);
                return;
            }
            result.computeStatus();
        }
    }

    private boolean repositorySelfTestLookupTableGetKey(OperationResult result, String name, String oid) {
        OperationResult subresult = result.createSubresult(result.getOperation() + ".getObject.key");
        try {
            // @formatter:off
            GetOperationOptionsBuilder optionsBuilder = schemaService.getOperationOptionsBuilder()
                    .item(LookupTableType.F_ROW)
                    .retrieveQuery()
                            .item(LookupTableRowType.F_KEY)
                            .eq(INSANE_NATIONAL_STRING)
                    .end();
            // @formatter:on
            PrismObject<LookupTableType> lookupTableRetrieved = repositoryService.getObject(LookupTableType.class, oid, optionsBuilder.build(), result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Self-test:lookupTable getObject by row key:\n{}", DebugUtil.debugDump(lookupTableRetrieved));
            }
            checkLookupTable(lookupTableRetrieved, name, subresult);
            subresult.recordSuccessIfUnknown();
        } catch (ObjectNotFoundException | SchemaException | RuntimeException e) {
            subresult.recordFatalError(e);
            return true;
        }
        return false;
    }

    private boolean repositorySelfTestLookupTableGet(OperationResult result, String name, String oid) {
        OperationResult subresult = result.createSubresult(result.getOperation() + ".getObject");

        PrismObject<LookupTableType> lookupTableRetrieved;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                    .item(LookupTableType.F_ROW).retrieve()
                    .build();
            lookupTableRetrieved = repositoryService.getObject(LookupTableType.class, oid, options, subresult);
        } catch (ObjectNotFoundException | SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            return true;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Self-test:lookupTable getObject:\n{}", lookupTableRetrieved.debugDump());
        }
        checkLookupTable(lookupTableRetrieved, name, subresult);
        subresult.recordSuccessIfUnknown();
        return false;
    }

    private void checkLookupTable(PrismObject<LookupTableType> lookupTable, String name, OperationResult subresult) {
        checkObjectPropertyPolyString(lookupTable, LookupTableType.F_NAME, subresult, name);
        checkObjectProperty(lookupTable, LookupTableType.F_DESCRIPTION, subresult, SelfTestData.POLICIJA);
        LookupTableType lookupTableType = lookupTable.asObjectable();
        assertEquals("Unexpected number of rows", 1, lookupTableType.getRow().size(), subresult);
        LookupTableRowType rowType = lookupTableType.getRow().get(0);
        assertEquals("Unexpected key value", INSANE_NATIONAL_STRING, rowType.getKey(), subresult);
        assertEquals("Unexpected 'value' value", INSANE_NATIONAL_STRING, rowType.getValue(), subresult);
        assertPolyStringType("Unexpected label value", INSANE_NATIONAL_STRING, rowType.getLabel(), subresult);
    }

    private void assertSingleSearchResult(String objectTypeMessage,
            List<PrismObject<UserType>> foundObjects, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + ".numberOfResults");
        assertTrue("Found no " + objectTypeMessage, !foundObjects.isEmpty(), result);
        assertTrue("Expected to find a single " + objectTypeMessage + " but found " + foundObjects.size(), foundObjects.size() == 1, result);
        result.recordSuccessIfUnknown();
    }

    private <O extends ObjectType, T> void checkObjectProperty(
            PrismObject<O> object, QName propQName, OperationResult parentResult, T... expectedValues) {
        String propName = propQName.getLocalPart();
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + ".checkObjectProperty." + propName);
        PrismProperty<T> prop = object.findProperty(ItemName.fromQName(propQName));
        Collection<T> actualValues = prop.getRealValues();
        result.addArbitraryObjectCollectionAsParam("actualValues", actualValues);
        assertMultivalue("User, property '" + propName + "'", expectedValues, actualValues, result);
        result.recordSuccessIfUnknown();
    }

    private <T> void assertMultivalue(String message,
            T[] expectedVals, Collection<T> actualVals, OperationResult result) {
        if (expectedVals.length != actualVals.size()) {
            fail(message + ": expected " + expectedVals.length + " values but has "
                    + actualVals.size() + " values: " + actualVals, result);
            return;
        }
        for (T expected : expectedVals) {
            boolean found = false;
            for (T actual : actualVals) {
                if (expected.equals(actual)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail(message + ": expected value '" + expected + "' not found in actual values " + actualVals, result);
                return;
            }
        }
    }

    private <O extends ObjectType> void checkObjectPropertyPolyString(PrismObject<O> object, QName propQName, OperationResult parentResult, String... expectedValues) {
        String propName = propQName.getLocalPart();
        OperationResult result = parentResult.createSubresult(parentResult.getOperation() + "." + propName);
        PrismProperty<PolyString> prop = object.findProperty(ItemName.fromQName(propQName));
        Collection<PolyString> actualValues = prop.getRealValues();
        result.addArbitraryObjectCollectionAsParam("actualValues", actualValues);
        assertMultivaluePolyString("User, property '" + propName + "'", expectedValues, actualValues, result);
        result.recordSuccessIfUnknown();
    }

    private void assertMultivaluePolyString(String message, String[] expectedOrigs, Collection<PolyString> actualPolyStrings, OperationResult result) {
        if (expectedOrigs.length != actualPolyStrings.size()) {
            fail(message + ": expected " + expectedOrigs.length + " values but has " + actualPolyStrings.size() + " values: " + actualPolyStrings, result);
            return;
        }
        for (String expectedOrig : expectedOrigs) {
            boolean found = false;
            for (PolyString actualPolyString : actualPolyStrings) {
                if (expectedOrig.equals(actualPolyString.getOrig())) {
                    found = true;
                    assertEquals(message + ", norm", polyStringNorm(expectedOrig), actualPolyString.getNorm(), result);
                    break;
                }
            }
            if (!found) {
                fail(message + ": expected value '" + expectedOrig + "' not found in actual values " + actualPolyStrings, result);
                return;
            }
        }
    }

    private void assertPolyStringType(String message,
            String expectedName, PolyStringType actualPolyStringType, OperationResult result) {
        assertEquals(message + ", orig", expectedName, actualPolyStringType.getOrig(), result);
        assertEquals(message + ", norm", polyStringNorm(expectedName), actualPolyStringType.getNorm(), result);
    }

    private String polyStringNorm(String orig) {
        return prismContext.getDefaultPolyStringNormalizer().normalize(orig);
    }

    private void assertTrue(String message, boolean condition, OperationResult result) {
        if (!condition) {
            fail(message, result);
        }
    }

    private void assertEquals(String message, Object expected, Object actual, OperationResult result) {
        if (!MiscUtil.equals(expected, actual)) {
            fail(message + "; expected " + expected + ", actual " + actual, result);
        }
    }

    private void fail(String message, OperationResult result) {
        result.recordFatalError(message);
        LOGGER.error("Repository self-test assertion failed: {}", message);
    }

    private String generateRandomName() {
        return NAME_PREFIX + randomString.nextString();
    }

    private PolyStringType toPolyStringType(String orig) {
        PolyStringType polyStringType = new PolyStringType();
        polyStringType.setOrig(orig);
        return polyStringType;
    }

    private PolyString toPolyString(String orig) {
        return PolyString.fromOrig(orig);
    }

    private <T extends ObjectType> PrismObjectDefinition<T> getObjectDefinition(Class<T> type) {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    }

    @Override
    public String exportDataModel(Collection<String> resourceOids,
            DataModelVisualizer.Target target, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(EXPORT_DATA_MODEL);
        try {
            String rv = dataModelVisualizer.visualize(resourceOids, DataModelVisualizer.Target.DOT, task, result);
            result.computeStatusIfUnknown();
            return rv;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        }
    }

    @Override
    public String exportDataModel(ResourceType resource, DataModelVisualizer.Target target,
            Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
        OperationResult result = parentResult.createSubresult(EXPORT_DATA_MODEL);
        try {
            String rv = dataModelVisualizer.visualize(resource, DataModelVisualizer.Target.DOT, task, result);
            result.computeStatusIfUnknown();
            return rv;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        }
    }

    @Override
    public LogFileContentType getLogFileContent(Long fromPosition, Long maxSize, Task task, OperationResult parentResult)
            throws SecurityViolationException, IOException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        OperationResult result = parentResult.createSubresult(GET_LOG_FILE_CONTENT);
        try {
            securityEnforcer.authorizeAll(task, result);
            File logFile = getLogFile(result);
            LogFileContentType rv = getLogFileFragment(logFile, fromPosition, maxSize);
            result.recordSuccess();
            return rv;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        }
    }

    private LogFileContentType getLogFileFragment(File logFile, Long fromPosition, Long maxSize) throws IOException {
        LogFileContentType rv = new LogFileContentType();
        try (RandomAccessFile log = new RandomAccessFile(logFile, "r")) {
            long currentLength = log.length();
            rv.setLogFileSize(currentLength);

            long start;
            if (fromPosition == null) {
                start = 0;
            } else if (fromPosition >= 0) {
                start = fromPosition;
            } else {
                start = Math.max(currentLength + fromPosition, 0);
            }
            rv.setAt(start);
            log.seek(start);
            long bytesToRead = Math.max(currentLength - start, 0);
            if (maxSize != null && maxSize < bytesToRead) {
                bytesToRead = maxSize;
                rv.setComplete(false);
            } else {
                rv.setComplete(true);
            }
            if (bytesToRead == 0) {
                return rv;
            } else if (bytesToRead > Integer.MAX_VALUE) {
                throw new IllegalStateException("Too many bytes to read from log file: " + bytesToRead);
            }
            byte[] buffer = new byte[(int) bytesToRead];
            log.readFully(buffer);
            rv.setContent(new String(buffer));
            return rv;
        }
    }

    @Override
    public long getLogFileSize(Task task, OperationResult parentResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            ConfigurationException, CommunicationException {
        OperationResult result = parentResult.createSubresult(GET_LOG_FILE_SIZE);
        try {
            securityEnforcer.authorizeAll(task, result);
            File logFile = getLogFile(result);
            long size = logFile.length();
            result.recordSuccess();
            return size;
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        }
    }

    private File getLogFile(OperationResult result) throws SchemaException {
        String configuredLogFile = midpointConfiguration.getSystemSection().getLogFile();
        if (configuredLogFile != null) {
            return new File(configuredLogFile);
        }
        PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(result);
        LoggingConfigurationType logging = configuration != null ? configuration.asObjectable().getLogging() : null;
        if (logging != null) {
            String rootLoggerAppender = logging.getRootLoggerAppender();
            if (rootLoggerAppender != null) {
                AppenderConfigurationType rootFileAppender = logging.getAppender().stream()
                        .filter(a -> rootLoggerAppender.equals(a.getName())).findFirst().orElse(null);
                if (rootFileAppender instanceof FileAppenderConfigurationType) {
                    String fileName = ((FileAppenderConfigurationType) rootFileAppender).getFileName();
                    return new File(StringSubstitutor.replaceSystemProperties(fileName));
                }
            }
        }
        throw new SchemaException("No log file specified in system configuration file not in system configuration object. Please set logFile in <midpoint><system> section or specify file appender.");
    }

    @Override
    public String getMemoryInformation(Task task, OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, IOException {
        OperationResult result = parentResult.createSubresult(GET_MEMORY_INFORMATION);
        try {
            securityEnforcer.authorizeAll(task, result);
            String pid = SystemUtil.getMyPid();
            if (pid == null) {
                result.recordPartialError("Cannot determine current process ID");
                return "Cannot determine current process ID";
            }
            StringSubstitutor substitutor = createCommandStringSubstitutor(pid);
            PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(result);

            StringBuilder sb = new StringBuilder();
            String heapInfoCommand = getHeapInfoCommand(configuration, substitutor);
            sb.append("Getting heap information via: ").append(heapInfoCommand).append("\n\n");
            boolean finished1 = SystemUtil.executeCommand(heapInfoCommand, "", sb, HEAP_DIAG_TIMEOUT);
            if (!finished1) {
                sb.append("\nBeware: the command execution has not finished in given time limit.\n\n");
            } else {
                String histogramCommand = getHeapObjectsHistogramCommand(configuration, substitutor);
                sb.append("\n------------------------------------------------------------------------------------------------------------\n\n");
                sb.append("Getting heap object histogram via: ").append(histogramCommand).append("\n\n");
                boolean finished2 = SystemUtil.executeCommand(histogramCommand, "", sb, HEAP_DIAG_TIMEOUT);
                if (!finished2) {
                    sb.append("\nBeware: the command execution has not finished in given time limit.\n\n");
                }
            }
            result.recordSuccess();
            return sb.toString();
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private StringSubstitutor createCommandStringSubstitutor(String pid) {
        Map<String, String> variableMap = new HashMap<>();
        variableMap.put("pid", pid);
        variableMap.put("jmap", midpointConfiguration.getSystemSection().getJmap());
        variableMap.put("jhsdb", midpointConfiguration.getSystemSection().getJhsdb());

        return new StringSubstitutor(variableMap, "${", "}");
    }

    private String getHeapObjectsHistogramCommand(PrismObject<SystemConfigurationType> configuration, StringSubstitutor substitutor) {
        MemoryDiagnosticsConfigurationType memoryDiagConfiguration = getMemoryDiagConfiguration(configuration);
        String template;
        if (memoryDiagConfiguration != null && memoryDiagConfiguration.getHeapObjectsHistogramCommand() != null) {
            template = memoryDiagConfiguration.getHeapObjectsHistogramCommand();
        } else {
            template = DEFAULT_HEAP_OBJECTS_HISTOGRAM_COMMAND;
        }
        return substitutor.replace(template);
    }

    private String getHeapInfoCommand(PrismObject<SystemConfigurationType> configuration, StringSubstitutor substitutor) {
        MemoryDiagnosticsConfigurationType memoryDiagConfiguration = getMemoryDiagConfiguration(configuration);
        String template;
        if (memoryDiagConfiguration != null && memoryDiagConfiguration.getHeapInfoCommand() != null) {
            template = memoryDiagConfiguration.getHeapInfoCommand();
        } else {
            template = DEFAULT_HEAP_INFO_COMMAND;
        }
        return substitutor.replace(template);
    }

    private MemoryDiagnosticsConfigurationType getMemoryDiagConfiguration(PrismObject<SystemConfigurationType> configuration) {
        return configuration != null && configuration.asObjectable().getInternals() != null ?
                configuration.asObjectable().getInternals().getMemoryDiagnostics() : null;
    }
}
