/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import com.evolveum.midpoint.schema.util.RawRepoShadow;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Unit test utilities.
 *
 * @author Radovan Semancik
 */
public class TestUtil {

    public static final int MAX_EXCEPTION_MESSAGE_LENGTH = 500;

    public static final String TEST_OUT_SECTION_PREFIX = "\n\n----- ";
    public static final String TEST_OUT_SECTION_SUFFIX = " --------------------------------------\n";
    public static final String TEST_LOG_SECTION_PREFIX = "----- ";
    public static final String TEST_LOG_SECTION_SUFFIX = " --------------------------------------";

    /**
     * Obviously, don't create objects with this OID.
     * This is not random so it can be used also in XMLs, e.g. for refs to nonexistent objects.
     */
    public static final String NON_EXISTENT_OID = "4e4f4e5f-4558-4953-5445-4e545f4f4944";

    private static final boolean CHECK_RESULTS = true;

    private static DatatypeFactory datatypeFactory = null;

    private static final Trace LOGGER = TraceManager.getTrace(TestUtil.class);

    private static final Random RND = new Random();

    @SuppressWarnings("unchecked")
    public static <T> void assertPropertyValueSetEquals(Collection<PrismPropertyValue<T>> actual, T... expected) {
        Set<T> set = new HashSet<>();
        for (PrismPropertyValue<T> value : actual) {
            set.add(value.getValue());
        }
        assertSetEquals(set, expected);
    }

    @SuppressWarnings("unchecked")
    public static <T> void assertSetEquals(Collection<T> actual, T... expected) {
        assertSetEquals(null, actual, expected);
    }

    @SuppressWarnings("unchecked")
    public static <T> void assertSetEquals(String message, Collection<T> actual, T... expected) {
        Set<T> expectedSet = new HashSet<>(Arrays.asList(expected));
        Set<T> actualSet = new HashSet<>(actual);
        if (message != null) {
            assertEquals(message, expectedSet, actualSet);
        } else {
            assertEquals(expectedSet, actualSet);
        }
    }

    public static <T> void assertSetEquals(String message, T[] actual, T[] expected) {
        assertTrue(message + "expected " + Arrays.toString(expected) + ", was " + Arrays.toString(actual),
                MiscUtil.unorderedArrayEquals(actual, expected));
    }

    public static void setAttribute(PrismObject<ShadowType> account, QName attrName, QName typeName, String value)
            throws SchemaException, ConfigurationException {
        PrismContainer<Containerable> attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        ShadowSimpleAttributeDefinition<String> attrDef = ObjectFactory.createSimpleAttributeDefinition(attrName, typeName);
        ShadowSimpleAttribute<String> attribute = attrDef.instantiate();
        attribute.setRealValue(value);
        attributesContainer.add(attribute);
    }

    public static void assertElement(List<Object> elements, QName elementQName, String value) {
        for (Object element : elements) {
            QName thisElementQName = JAXBUtil.getElementQName(element);
            if (elementQName.equals(thisElementQName)) {
                if (element instanceof Element element1) {
                    String thisElementContent = element1.getTextContent();
                    if (value.equals(thisElementContent)) {
                        return;
                    } else {
                        AssertJUnit.fail("Wrong value for element with name " + elementQName + "; expected " + value + "; was " + thisElementContent);
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected type of element " + elementQName + ": " + element.getClass());
                }
            }
        }
        AssertJUnit.fail("No element with name " + elementQName);
    }

    public static void assertExceptionSanity(ObjectAlreadyExistsException e) {
        LOGGER.debug("Exception (expected) {}", e, e);
        System.out.println("Exception (expected)");
        System.out.println(ExceptionUtils.getStackTrace(e));
        assert !e.getMessage().isEmpty() : "Empty exception message";
        assert e.getMessage().length() < MAX_EXCEPTION_MESSAGE_LENGTH : "Exception message too long ("
                + e.getMessage().length() + " characters): " + e.getMessage();
    }

    public static void displayCleanup(String testName) {
        System.out.println(TEST_OUT_SECTION_PREFIX + " CLEANUP " + testName + TEST_OUT_SECTION_SUFFIX);
        LOGGER.info(TEST_LOG_SECTION_PREFIX + " CLEANUP " + testName + TEST_LOG_SECTION_SUFFIX);
    }

    public static void displaySkip(String testName) {
        System.out.println(TEST_OUT_SECTION_PREFIX + " SKIP " + testName + TEST_OUT_SECTION_SUFFIX);
        LOGGER.info(TEST_LOG_SECTION_PREFIX + " SKIP " + testName + TEST_LOG_SECTION_SUFFIX);
    }

    public static void info(String message) {
        System.out.println(TEST_OUT_SECTION_PREFIX + message + TEST_OUT_SECTION_SUFFIX);
        LOGGER.info(TEST_LOG_SECTION_PREFIX + message + TEST_LOG_SECTION_SUFFIX);
    }

    public static void assertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel, boolean warningOk) {
        if (!CHECK_RESULTS) {
            return;
        }
        if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
            String logmsg = message + ": undefined status (" + result.getStatus() + ") on operation " + result.getOperation();
            LOGGER.error(logmsg);
            LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
            System.out.println(logmsg + "\n" + originalResult.debugDump());
            fail(logmsg);
        }

        if (result.isHandledError()) {
            // There may be errors deeper in this result, even fatal errors. that's ok, we can ignore them.
            return;
        } else if (result.isSuccess() || result.isNotApplicable()) {
            // OK ... expected error is as good as success
        } else if (warningOk && result.getStatus() == OperationResultStatus.WARNING) {
            // OK
        } else {
            String logmsg = message + ": " + result.getStatus() + ": " + result.getMessage();
            LOGGER.error(logmsg);
            LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
            System.out.println(logmsg + "\n" + originalResult.debugDump());
            assert false : logmsg;
        }

        if (stopLevel == currentLevel) {
            return;
        }
        List<OperationResult> partialResults = result.getSubresults();
        for (OperationResult subResult : partialResults) {
            assertSuccess(message, subResult, originalResult, stopLevel, currentLevel + 1, warningOk);
        }
    }

    /**
     * level=-1 - check all levels
     * level=0 - check only the top-level
     * level=1 - check one level below top-level
     * ...
     */
    public static void assertSuccess(String message, OperationResult result, int level) {
        assertSuccess(message, result, result, level, 0, false);
    }

    public static void assertSuccess(String message, OperationResult result) {
        assertSuccess(message, result, -1);
    }

    public static void assertSuccess(OperationResultType result) {
        assertSuccess(result.getOperation(), result);
    }

    public static void assertSuccess(String message, OperationResultType result) {
        assertSuccess(message, result, 0, -1);
    }

    public static void assertSuccess(String message, OperationResultType result, int stopLevel) {
        assertSuccess(message, result, 0, stopLevel);
    }

    private static void assertSuccess(String message, OperationResultType result, int currentLevel, int stopLevel) {
        if (!CHECK_RESULTS) {
            return;
        }
        if (stopLevel >= 0 && currentLevel > stopLevel) {
            return;
        }
        assertNotNull(message + ": null result", result);
        // Ignore top-level if the operation name is not set
        if (result.getOperation() != null) {
            if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
                fail(message + ": undefined status (" + result.getStatus() + ") on operation " + result.getOperation());
            }
            if (result.getStatus() != OperationResultStatusType.SUCCESS
                    && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                    && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
                LOGGER.error("Failing operation result:\n{}", OperationResult.createOperationResult(result).debugDump(1));
                fail(message + ": " + result.getMessage() + " (" + result.getStatus() + ")");
            }
        }
        List<OperationResultType> partialResults = result.getPartialResults();
        for (OperationResultType subResult : partialResults) {
            if (subResult == null) {
                fail(message + ": null subresult under operation " + result.getOperation());
            }
            if (subResult.getOperation() == null) {
                fail(message + ": null subresult operation under operation " + result.getOperation());
            }
            if (subResult.getStatus() == OperationResultStatusType.HANDLED_ERROR) {
                // HANDLED_ERROR means there might be an error (partial/fatal) inside.
                continue;
            }
            assertSuccess(message, subResult, currentLevel + 1, stopLevel);
        }
    }

    public static void assertInProgressOrSuccess(OperationResult result) {
        if (!result.isInProgress()) {
            assertSuccess("Operation " + result.getOperation() + " result", result);
        }
    }

    public static void assertSuccess(OperationResult result) {
        assertSuccess("Operation " + result.getOperation() + " result", result);
    }

    public static void assertSuccess(OperationResult result, int depth) {
        assertSuccess("Operation " + result.getOperation() + " result", result, depth);
    }

    public static void assertStatus(OperationResult result, OperationResultStatus expectedStatus) {
        assertEquals("Operation " + result.getOperation() + " result", expectedStatus, result.getStatus());
    }

    public static void assertStatus(OperationResultType result, OperationResultStatusType expectedStatus) {
        assertEquals("Operation " + result.getOperation() + " result", expectedStatus, result.getStatus());
    }

    public static boolean hasWarningAssertSuccess(String message, OperationResultType result) {
        boolean hasWarning = false;
        // Ignore top-level if the operation name is not set
        if (result.getOperation() != null) {
            if (result.getStatus() == OperationResultStatusType.WARNING) {
                // Do not descent into warnings. There may be lions inside. Or errors.
                return true;
            } else {
                if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
                    fail(message + ": undefined status (" + result.getStatus() + ") on operation " + result.getOperation());
                }
                if (result.getStatus() != OperationResultStatusType.SUCCESS
                        && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                        && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
                    fail(message + ": " + result.getMessage() + " (" + result.getStatus() + ")");
                }
            }
        }
        List<OperationResultType> partialResults = result.getPartialResults();
        for (OperationResultType subResult : partialResults) {
            if (subResult == null) {
                fail(message + ": null subresult under operation " + result.getOperation());
            }
            if (subResult.getOperation() == null) {
                fail(message + ": null subresult operation under operation " + result.getOperation());
            }
            if (hasWarningAssertSuccess(message, subResult)) {
                hasWarning = true;
            }
        }
        return hasWarning;
    }

    public static void assertWarning(String message, OperationResultType result) {
        if (!CHECK_RESULTS) {
            return;
        }
        assert hasWarningAssertSuccess(message, result) : message + ": does not have warning";
    }

    public static void assertFailure(String message, OperationResult result) {
        assertTrue(message, result.isError());
        assertNoUnknown(result);
    }

    public static void assertFailure(OperationResult result) {
        if (!result.isError()) {
            String message = "Expected that operation " + result.getOperation() + " fails, but the result was " + result.getStatus();
            System.out.println(message);
            System.out.println(result.debugDump());
            LOGGER.error("{}", message);
            LOGGER.error("{}", result.debugDump());
            AssertJUnit.fail(message);
        }
        assertNoUnknown(result);
    }

    public static void assertPartialError(OperationResultType result) {
        assertTrue("Expected that operation " + result.getOperation() +
                        " fails partially, but the result was " + result.getStatus(),
                result.getStatus() == OperationResultStatusType.PARTIAL_ERROR);
    }

    public static void assertPartialError(OperationResult result) {
        assertTrue("Expected that operation " + result.getOperation() + " fails partially, but the result was " + result.getStatus(), result.getStatus() == OperationResultStatus.PARTIAL_ERROR);
        assertNoUnknown(result);
    }

    public static void assertFatalError(OperationResultType result) {
        assertTrue("Expected that operation " + result.getOperation() +
                        " fails fatally, but the result was " + result.getStatus(),
                result.getStatus() == OperationResultStatusType.FATAL_ERROR);
    }

    public static void assertResultStatus(OperationResult result, OperationResultStatus expectedStatus) {
        assertTrue("Expected that operation " + result.getOperation() + " will result with " + expectedStatus + ", but the result was " + result.getStatus(), result.getStatus() == expectedStatus);
        assertNoUnknown(result);
    }

    public static void assertFailure(OperationResultType result) {
        assertFailure(null, result);
    }

    public static void assertFailure(String message, OperationResultType result) {
        assertTrue((message == null ? "" : message + ": ") +
                        "Expected that operation " + result.getOperation() + " fails, but the result was " + result.getStatus(),
                OperationResultStatusType.FATAL_ERROR == result.getStatus() ||
                        OperationResultStatusType.PARTIAL_ERROR == result.getStatus());
        assertNoUnknown(result);
    }

    public static void assertNoUnknown(OperationResult result) {
        if (result.isUnknown()) {
            AssertJUnit.fail("Unknown status for operation " + result.getOperation());
        }
        for (OperationResult subresult : result.getSubresults()) {
            assertNoUnknown(subresult);
        }
    }

    public static void assertNoUnknown(OperationResultType result) {
        if (result.getStatus() == OperationResultStatusType.UNKNOWN) {
            AssertJUnit.fail("Unknown status for operation " + result.getOperation());
        }
        for (OperationResultType subresult : result.getPartialResults()) {
            assertNoUnknown(subresult);
        }
    }

    public static void assertSuccessOrWarning(String message, OperationResult result, int level) {
        assertSuccess(message, result, result, level, 0, true);
    }

    public static void assertSuccessOrWarning(String message, OperationResult result) {
        assertSuccess(message, result, result, -1, 0, true);
    }

    public static void assertWarning(String message, OperationResult result) {
        assertWarning(message, result, -1, 0);
    }

    public static boolean hasWarningAssertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel) {
        if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
            String logmsg = message + ": undefined status (" + result.getStatus() + ") on operation " + result.getOperation();
            LOGGER.error(logmsg);
            LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
            System.out.println(logmsg + "\n" + originalResult.debugDump());
            fail(logmsg);
        }

        if (result.isWarning()) {
            // Do not descent into warnings. There may be lions inside. Or errors.
            return true;
        }

        if (result.isSuccess() || result.isHandledError() || result.isNotApplicable()) {
            // OK ... expected error is as good as success
        } else {
            String logmsg = message + ": " + result.getStatus() + ": " + result.getMessage();
            LOGGER.error(logmsg);
            LOGGER.trace(logmsg + "\n" + originalResult.debugDump());
            System.out.println(logmsg + "\n" + originalResult.debugDump());
            assert false : logmsg;
        }

        if (stopLevel == currentLevel) {
            return false;
        }
        boolean hasWarning = false;
        List<OperationResult> partialResults = result.getSubresults();
        for (OperationResult subResult : partialResults) {
            if (hasWarningAssertSuccess(message, subResult, originalResult, stopLevel, currentLevel + 1)) {
                hasWarning = true;
            }
        }
        return hasWarning;
    }

    public static void assertWarning(String message, OperationResult result, int stopLevel, int currentLevel) {
        if (!CHECK_RESULTS) {
            return;
        }
        hasWarningAssertSuccess(message, result, result, -1, 0);
    }

    public static void assertInProgress(String message, OperationResult result) {
        assertTrue("Expected result IN_PROGRESS but it was " + result.getStatus() + " in " + message,
                result.getStatus() == OperationResultStatus.IN_PROGRESS);
    }

    public static String getErrorMessage(OperationResult result) {
        if (result.isError()) {
            return result.getMessage();
        }
        for (OperationResult subresult : result.getSubresults()) {
            String message = getErrorMessage(subresult);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    public static List<OperationResult> selectSubresults(OperationResult result, String... operationNames) {
        List<OperationResult> retval = new ArrayList<>();
        selectSubresultsInternal(retval, result, operationNames);
        return retval;
    }

    private static void selectSubresultsInternal(List<OperationResult> retval, OperationResult result, String... operationNames) {
        if (result == null) {
            return;            // should not occur actually
        }
        for (String operationName : operationNames) {
            if (operationName.equals(result.getOperation())) {
                retval.add(result);
                break;
            }
        }
        for (OperationResult subresult : result.getSubresults()) {
            selectSubresultsInternal(retval, subresult, operationNames);
        }
    }

    public static String execSystemCommand(String command) throws IOException, InterruptedException {
        return execSystemCommand(command, false);
    }

    public static String execSystemCommand(String command, boolean ignoreExitCode) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        LOGGER.debug("Executing system command: {}", command);
        Process process = runtime.exec(command);
        int exitCode = process.waitFor();
        LOGGER.debug("Command exit code: {}", exitCode);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }
        reader.close();
        String outstring = output.toString();
        LOGGER.debug("Command output:\n{}", outstring);
        if (!ignoreExitCode && exitCode != 0) {
            String msg = "Execution of command '" + command + "' failed with exit code " + exitCode;
            LOGGER.error("{}", msg);
            throw new IOException(msg);
        }
        return outstring;
    }

    public static void assertBetween(String message, XMLGregorianCalendar start, XMLGregorianCalendar end,
            XMLGregorianCalendar actual) {
        assertNotNull(message + " is null", actual);
        if (start != null) {
            assertTrue(message + ": expected time to be after " + start + " but it was " + actual,
                    actual.compare(start) == DatatypeConstants.GREATER || actual.compare(start) == DatatypeConstants.EQUAL);
        }
        if (end != null) {
            assertTrue(message + ": expected time to be before " + end + " but it was " + actual,
                    actual.compare(end) == DatatypeConstants.LESSER || actual.compare(end) == DatatypeConstants.EQUAL);
        }
    }

    public static void assertBetween(String message, Long start, Long end, Long actual) {
        assertNotNull(message + " is null", actual);
        if (start != null) {
            assertTrue(message + ": expected time to be after " + start + " but it was " + actual, actual >= start);
        }
        if (end != null) {
            assertTrue(message + ": expected time to be before " + end + " but it was " + actual, actual <= end);
        }
    }

    public static void assertEqualsTimestamp(String message, XMLGregorianCalendar expected, XMLGregorianCalendar actual) {
        assertNotNull(message + "; expected " + expected, actual);
        assertEquals(message + "; expected " + expected + " but was " + actual, 0, expected.compare(actual));
    }

    public static void assertCreateTimestamp(
            PrismObject<? extends ObjectType> object, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        assertBetween(
                "createTimestamp in " + object, start, end,
                ValueMetadataTypeUtil.getCreateTimestamp(object.asObjectable()));
    }

    public static void assertCreateTimestamp(
            RawRepoShadow object, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        assertCreateTimestamp(object.getPrismObject(), start, end);
    }

    public static void assertModifyTimestamp(PrismObject<? extends ObjectType> object, XMLGregorianCalendar start,
            XMLGregorianCalendar end) {
        assertBetween(
                "modifyTimestamp in " + object, start, end,
                ValueMetadataTypeUtil.getModifyTimestamp(object.asObjectable()));
    }

    public static XMLGregorianCalendar currentTime() {
        // This cannot use XmlTypeConverter as we want to use also in tests that do not depend on prism
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(System.currentTimeMillis());
        return getDatatypeFactory().newXMLGregorianCalendar(gregorianCalendar);
    }

    private static DatatypeFactory getDatatypeFactory() {
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException ex) {
                throw new IllegalStateException("Cannot construct DatatypeFactory: " + ex.getMessage(), ex);
            }
        }
        return datatypeFactory;
    }

    public static void assertMessageContains(String message, String expectedSubstring) {
        assertTrue("Expected that message will contain substring '" + expectedSubstring + "', but it did not. Message: " + message,
                message.contains(expectedSubstring));
    }

    // WARNING! Only works on Linux
    public static int getPid() throws NumberFormatException, IOException {
        return Integer.parseInt(new File("/proc/self").getCanonicalFile().getName());
    }

    public static void assertPrivateFilePermissions(File f) throws IOException {
        try {
            Set<PosixFilePermission> configPermissions = Files.getPosixFilePermissions(Paths.get(f.getPath()));
            LOGGER.info("File {} permissions: {}", f, configPermissions);
            assertPermission(f, configPermissions, PosixFilePermission.OWNER_READ);
            assertPermission(f, configPermissions, PosixFilePermission.OWNER_WRITE);
            assertNoPermission(f, configPermissions, PosixFilePermission.OWNER_EXECUTE);
            assertNoPermission(f, configPermissions, PosixFilePermission.GROUP_READ);
            assertNoPermission(f, configPermissions, PosixFilePermission.GROUP_WRITE);
            assertNoPermission(f, configPermissions, PosixFilePermission.GROUP_EXECUTE);
            assertNoPermission(f, configPermissions, PosixFilePermission.OTHERS_READ);
            assertNoPermission(f, configPermissions, PosixFilePermission.OTHERS_WRITE);
            assertNoPermission(f, configPermissions, PosixFilePermission.OTHERS_EXECUTE);
        } catch (UnsupportedOperationException e) {
            // Windows. Sorry.
        }
    }

    private static void assertPermission(File f, Set<PosixFilePermission> permissions, PosixFilePermission permission) {
        assertTrue(permissions.contains(permission), f.getPath() + ": missing permission " + permission);
    }

    private static void assertNoPermission(File f, Set<PosixFilePermission> permissions, PosixFilePermission permission) {
        assertFalse(permissions.contains(permission), f.getPath() + ": unexpected permission " + permission);
    }

    public static ParallelTestThread[] multithread(
            MultithreadRunner lambda, int numberOfThreads, Integer randomStartDelayRange) {
        ParallelTestThread[] threads = new ParallelTestThread[numberOfThreads];
        System.out.println("Going to create " + numberOfThreads + " threads...");
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new ParallelTestThread(i,
                    (ii) -> {
                        randomDelay(randomStartDelayRange);
                        LOGGER.info("{} starting", Thread.currentThread().getName());
                        lambda.run(ii);
                    });
            threads[i].setName("Thread " + (i + 1) + " of " + numberOfThreads);
            threads[i].start();
        }
        return threads;
    }

    public static void randomDelay(Integer range) {
        if (range == null || range == 0) {
            return;
        }
        try {
            Thread.sleep(RND.nextInt(range));
        } catch (InterruptedException e) {
            // Nothing to do, really
        }
    }

    public static void waitForThreads(ParallelTestThread[] threads, long timeout) throws InterruptedException {
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].isAlive()) {
                System.out.println("Waiting for " + threads[i]);
                threads[i].join(timeout);
            }
            Throwable threadException = threads[i].getException();
            if (threadException != null) {
                throw new AssertionError("Test thread " + i + " failed: " + threadException.getMessage(), threadException);
            }
        }
    }

    public static ItemDefinition createPrimitivePropertyDefinition(PrismContext prismContext, String name, PrimitiveType pType) {
        return prismContext.definitionFactory().newPropertyDefinition(new ItemName(SchemaConstants.NS_C, name), pType.getQname());
    }

    public static void waitForCompletion(List<Thread> threads, long timeout) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout) {
            boolean anyAlive = threads.stream().anyMatch(Thread::isAlive);
            if (!anyAlive) {
                break;
            } else {
                Thread.sleep(100);
            }
        }
    }

    public static @NotNull ItemName getAttrQName(@NotNull String attrName) {
        return new ItemName(MidPointConstants.NS_RI, attrName);
    }
}
