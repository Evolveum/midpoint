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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.test;

import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatusType;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeDefinition;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 *
 */
public class IntegrationTestTools {
	
	public static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";
	public static final String DBTABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";
	
	public static boolean checkResults = true;
	// public and not final - to allow changing it in tests
	public static Trace LOGGER = TraceManager.getTrace(IntegrationTestTools.class);
	
	private static final String TEST_OUT_PREFIX = "\n\n=====[ ";
	private static final String TEST_OUT_SUFFIX = " ]======================================\n";
	private static final String TEST_LOG_PREFIX = "=====[ ";
	private static final String TEST_LOG_SUFFIX = " ]======================================";
	private static final String TEST_OUT_SECTION_PREFIX = "\n\n----- ";
	private static final String TEST_OUT_SECTION_SUFFIX = " --------------------------------------\n";
	private static final String TEST_LOG_SECTION_PREFIX = "----- ";
	private static final String TEST_LOG_SECTION_SUFFIX = " --------------------------------------";
	private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
	private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";
	private static final String LOG_MESSAGE_PREFIX = "";
	private static final String OBJECT_LIST_SEPARATOR = "---";
	private static final long WAIT_FOR_LOOP_SLEEP_MILIS = 500;

	public static void assertSuccess(OperationResult result) {
		assertSuccess("Operation "+result.getOperation()+" result", result);
	}
	
	public static void assertSuccess(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		// Ignore top-level if the operation name is not set
		if (result.getOperation()!=null) {
			if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
				fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
			}
			if (result.getStatus() != OperationResultStatusType.SUCCESS
                    && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                    && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
				fail(message + ": " + result.getMessage() + " ("+result.getStatus()+")");
			}
		}
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			if (subResult==null) {
				fail(message+": null subresult under operation "+result.getOperation());
			}
			if (subResult.getOperation()==null) {
				fail(message+": null subresult operation under operation "+result.getOperation());
			}
			assertSuccess(message, subResult);
		}
	}
	
	public static void assertWarning(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		assert hasWarningAssertSuccess(message, result) : message + ": does not have warning";
	}
	
	public static boolean hasWarningAssertSuccess(String message, OperationResultType result) {
		boolean hasWarning = false;
		// Ignore top-level if the operation name is not set
		if (result.getOperation()!=null) {
			if (result.getStatus() == OperationResultStatusType.WARNING) {
				// Do not descent into warnings. There may be lions inside. Or errors.
				return true;
			} else {
				if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
					fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
				} 
				if (result.getStatus() != OperationResultStatusType.SUCCESS
                        && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE
                        && result.getStatus() != OperationResultStatusType.HANDLED_ERROR) {
					fail(message + ": " + result.getMessage() + " ("+result.getStatus()+")");
				}
			}
		}
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			if (subResult==null) {
				fail(message+": null subresult under operation "+result.getOperation());
			}
			if (subResult.getOperation()==null) {
				fail(message+": null subresult operation under operation "+result.getOperation());
			}
			if (hasWarningAssertSuccess(message, subResult)) {
				hasWarning = true;
			}
		}
		return hasWarning;
	}

	public static void assertSuccess(String message, OperationResult result) {
		assertSuccess(message, result,-1);
	}
	
	public static void assertFailure(String message, OperationResult result) {
		assertTrue(message, result.isError());
		assertNoUnknown(result);
	}
	
	public static void assertFailure(OperationResult result) {
		assertTrue("Expected that operation "+result.getOperation()+" fails, but the result was "+result.getStatus(), result.isError());
		assertNoUnknown(result);
	}
	
	public static void assertPartialError(OperationResult result) {
		assertTrue("Expected that operation "+result.getOperation()+" fails partially, but the result was "+result.getStatus(), result.getStatus() == OperationResultStatus.PARTIAL_ERROR);
		assertNoUnknown(result);
	}
	
	public static void assertFailure(OperationResultType result) {
		assertFailure(null, result);
	}
	
	public static void assertFailure(String message, OperationResultType result) {
		assertTrue((message == null ? "" : message + ": ") + 
				"Expected that operation "+result.getOperation()+" fails, but the result was "+result.getStatus(), 
				OperationResultStatusType.FATAL_ERROR == result.getStatus() || 
				OperationResultStatusType.PARTIAL_ERROR == result.getStatus()) ;
		assertNoUnknown(result);
	}
	
	public static void assertNoUnknown(OperationResult result) {
		if (result.isUnknown()) {
			AssertJUnit.fail("Unkwnown status for operation "+result.getOperation());
		}
		for (OperationResult subresult: result.getSubresults()) {
			assertNoUnknown(subresult);
		}
	}
	
	public static void assertNoUnknown(OperationResultType result) {
		if (result.getStatus() == OperationResultStatusType.UNKNOWN) {
			AssertJUnit.fail("Unkwnown status for operation "+result.getOperation());
		}
		for (OperationResultType subresult: result.getPartialResults()) {
			assertNoUnknown(subresult);
		}
	}
	
	/**
	 * level=-1 - check all levels
	 * level=0 - check only the top-level
	 * level=1 - check one level below top-level
	 * ...
	 * 
	 * @param message
	 * @param result
	 * @param level
	 */
	public static void assertSuccess(String message, OperationResult result, int level) {
		assertSuccess(message, result, result, level, 0, false);
	}
	
	public static void assertSuccessOrWarning(String message, OperationResult result, int level) {
		assertSuccess(message, result, result, level, 0, true);
	}
	
	public static void assertSuccessOrWarning(String message, OperationResult result) {
		assertSuccess(message, result, result, -1, 0, true);
	}
	
	private static void assertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel, boolean warningOk) {
		if (!checkResults) {
			return;
		}
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			String logmsg = message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.dump());
			System.out.println(logmsg + "\n" + originalResult.dump());
			fail(logmsg);
		}
		
		if (result.isSuccess() || result.isHandledError() || result.isNotApplicable()) {
			// OK ... expected error is as good as success
		} else if (warningOk && result.getStatus() == OperationResultStatus.WARNING) {
			// OK
		} else {
			String logmsg = message + ": " + result.getStatus() + ": " + result.getMessage();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.dump());
			System.out.println(logmsg + "\n" + originalResult.dump());
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
	
	public static void assertWarning(String message, OperationResult result) {
		assertWarning(message, result, -1, 0);
	}
	
	private static void assertWarning(String message, OperationResult result, int stopLevel, int currentLevel) {
		if (!checkResults) {
			return;
		}
		hasWarningAssertSuccess(message, result, result, -1, 0);
	}
	
	private static boolean hasWarningAssertSuccess(String message, OperationResult result, OperationResult originalResult, int stopLevel, int currentLevel) {
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			String logmsg = message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation();
			LOGGER.error(logmsg);
			LOGGER.trace(logmsg + "\n" + originalResult.dump());
			System.out.println(logmsg + "\n" + originalResult.dump());
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
			LOGGER.trace(logmsg + "\n" + originalResult.dump());
			System.out.println(logmsg + "\n" + originalResult.dump());
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
	
	public static void assertInProgress(String message, OperationResult result) {
		assertTrue("Expected result IN_PROGRESS but it was "+result.getStatus()+" in "+message,
				result.getStatus() == OperationResultStatus.IN_PROGRESS);
	}
	
	public static String getErrorMessage(OperationResult result) {
		if (result.isError()) {
			return result.getMessage();
		}
		for (OperationResult subresult: result.getSubresults()) {
			String message = getErrorMessage(subresult);
			if (message != null) {
				return message;
			}
		}
		return null;
	}
	
	public static void assertTestResourceSuccess(OperationResult testResult, ConnectorTestOperation operation) {
		OperationResult opResult = testResult.findSubresult(operation.getOperation());
		assertNotNull("No result for "+operation, opResult);
		assertSuccess("Test resource failed (result): "+operation, opResult, 1);
	}

	public static void assertTestResourceFailure(OperationResult testResult, ConnectorTestOperation operation) {
		OperationResult opResult = testResult.findSubresult(operation.getOperation());
		assertNotNull("No result for "+operation, opResult);
		assertFailure("Test resource succeeded while expected failure (result): "+operation, opResult);
	}
	
	public static void assertTestResourceNotApplicable(OperationResult testResult, ConnectorTestOperation operation) {
		OperationResult opResult = testResult.findSubresult(operation.getOperation());
		assertNotNull("No result for "+operation, opResult);
		assertEquals("Test resource status is not 'not applicable', it is "+opResult.getStatus()+": "+operation, 
				OperationResultStatus.NOT_APPLICABLE, opResult.getStatus());
	}

	public static void assertNotEmpty(String message, String s) {
		assertNotNull(message, s);
		assertFalse(message, s.isEmpty());
	}
	
	public static void assertNotEmpty(PolyString ps) {
		assertNotNull(ps);
		assertFalse(ps.isEmpty());
	}
	
	public static void assertNotEmpty(PolyStringType ps) {
		assertNotNull(ps);
		assertFalse(PrismUtil.isEmpty(ps));
	}
	
	public static void assertNotEmpty(String message, PolyString ps) {
		assertNotNull(message, ps);
		assertFalse(message, ps.isEmpty());
	}
	
	public static void assertNotEmpty(String message, PolyStringType ps) {
		assertNotNull(message, ps);
		assertFalse(message, PrismUtil.isEmpty(ps));
	}

	public static void assertNotEmpty(String s) {
		assertNotNull(s);
		assertFalse(s.isEmpty());
	}

	public static void assertNotEmpty(String message, QName qname) {
		assertNotNull(message, qname);
		assertNotEmpty(message,qname.getNamespaceURI());
		assertNotEmpty(message,qname.getLocalPart());
	}

	public static void assertNotEmpty(QName qname) {
		assertNotNull(qname);
		assertNotEmpty(qname.getNamespaceURI());
		assertNotEmpty(qname.getLocalPart());
	}

	public static <T> void assertAttribute(ShadowType shadow, ResourceType resource, String name,
			T... expectedValues) {
		assertAttribute("Wrong attribute " + name + " in "+shadow, shadow,
				new QName(ResourceTypeUtil.getResourceNamespace(resource), name), expectedValues);
	}
	
	public static <T> void assertAttribute(ShadowType shadowType, QName name, T... expectedValues) {
		assertAttribute(shadowType.asPrismObject(), name, expectedValues);
	}
	
	public static <T> void assertAttribute(PrismObject<? extends ShadowType> shadow, QName name, T... expectedValues) {	
		Collection<T> values = getAttributeValues(shadow, name);
		assertEqualsCollection("Wrong value for attribute "+name+" in "+shadow, expectedValues, values);
	}

	public static <T> void assertAttribute(String message, ShadowType repoShadow, QName name, T... expectedValues) {
		Collection<T> values = getAttributeValues(repoShadow, name);
		assertEqualsCollection(message, expectedValues, values);
	}
	
	public static <T> void assertNoAttribute(PrismObject<? extends ShadowType> shadow, QName name) {	
		assertNull("Found attribute "+name+" in "+shadow+" while not expecting it", getAttributeValues(shadow, name));
	}
	
	public static <T> void assertEqualsCollection(String message, Collection<T> expectedValues, Collection<T> actualValues) {
		if (expectedValues == null && actualValues == null) {
			return;
		}
		assert !(expectedValues == null && actualValues != null) : "Expecting null values but got "+actualValues;
		assert actualValues != null : message+": Expecting "+expectedValues+" but got null";
		assertEquals(message+": Wrong number of values in " + actualValues, expectedValues.size(), actualValues.size());
		for (T actualValue: actualValues) {
			boolean found = false;
			for (T value: expectedValues) {
				if (value.equals(actualValue)) {
					found = true;
				}
			}
			if (!found) {
				fail(message + ": Unexpected value "+actualValue+"; expected "+expectedValues+"; has "+actualValues);
			}
		}
	}

	public static <T> void assertEqualsCollection(String message, Collection<T> expectedValues, T[] actualValues) {
		assertEqualsCollection(message, expectedValues, Arrays.asList(actualValues));
	}

	public static <T> void assertEqualsCollection(String message, T[] expectedValues, Collection<T> actualValues) {
		assertEqualsCollection(message, Arrays.asList(expectedValues), actualValues);
	}
	
	public static void assertIcfsNameAttribute(ShadowType repoShadow, String value) {
		assertAttribute(repoShadow, SchemaTestConstants.ICFS_NAME, value);
	}
	
	public static void assertIcfsNameAttribute(PrismObject<? extends ShadowType> repoShadow, String value) {
		assertAttribute(repoShadow, SchemaTestConstants.ICFS_NAME, value);
	}

	public static void assertAttributeNotNull(ShadowType repoShadow, QName name) {
		Collection<String> values = getAttributeValues(repoShadow, name);
		assertEquals(1, values.size());
		assertNotNull(values.iterator().next());
	}

	public static void assertAttributeNotNull(String message, ShadowType repoShadow, QName name) {
		Collection<String> values = getAttributeValues(repoShadow, name);
		assertEquals(message, 1, values.size());
		assertNotNull(message, values.iterator().next());
	}
	
	public static void assertAttributeDefinition(ResourceAttribute<?> attr, QName expectedType, int minOccurs, int maxOccurs,
			boolean canRead, boolean canCreate, boolean canUpdate, Class<?> expetcedAttributeDefinitionClass) {
		ResourceAttributeDefinition definition = attr.getDefinition();
		QName attrName = attr.getName();
		assertNotNull("No definition for attribute "+attrName, definition);
		assertEquals("Wrong class of definition for attribute"+attrName, expetcedAttributeDefinitionClass, definition.getClass());
		assertEquals("Wrong type in definition for attribute"+attrName, expectedType, definition.getTypeName());
		assertEquals("Wrong minOccurs in definition for attribute"+attrName, minOccurs, definition.getMinOccurs());
		assertEquals("Wrong maxOccurs in definition for attribute"+attrName, maxOccurs, definition.getMaxOccurs());
		assertEquals("Wrong canRead in definition for attribute"+attrName, canRead, definition.canRead());
		assertEquals("Wrong canCreate in definition for attribute"+attrName, canCreate, definition.canCreate());
		assertEquals("Wrong canUpdate in definition for attribute"+attrName, canUpdate, definition.canUpdate());
	}
	
	public static void assertProvisioningAccountShadow(PrismObject<ShadowType> account, ResourceType resourceType,
			Class<?> expetcedAttributeDefinitionClass) {
		// Check attribute definition
		PrismContainer attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
		assertEquals("Wrong attributes container class", ResourceAttributeContainer.class, attributesContainer.getClass());
		ResourceAttributeContainer rAttributesContainer = (ResourceAttributeContainer)attributesContainer;
		PrismContainerDefinition attrsDef = attributesContainer.getDefinition();
		assertNotNull("No attributes container definition", attrsDef);				
		assertTrue("Wrong attributes definition class "+attrsDef.getClass().getName(), attrsDef instanceof ResourceAttributeContainerDefinition);
		ResourceAttributeContainerDefinition rAttrsDef = (ResourceAttributeContainerDefinition)attrsDef;
		ObjectClassComplexTypeDefinition objectClassDef = rAttrsDef.getComplexTypeDefinition();
		assertNotNull("No object class definition in attributes definition", objectClassDef);
		assertEquals("Wrong object class in attributes definition", 
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME), 
				objectClassDef.getTypeName());
		ResourceAttribute<?> icfsNameUid = rAttributesContainer.findAttribute(SchemaTestConstants.ICFS_UID);
		assertAttributeDefinition(icfsNameUid, DOMUtil.XSD_STRING, 0, 1, true, false, false, expetcedAttributeDefinitionClass);
		
		ResourceAttribute<Object> icfsNameAttr = rAttributesContainer.findAttribute(SchemaTestConstants.ICFS_NAME);
		assertAttributeDefinition(icfsNameAttr, DOMUtil.XSD_STRING, 1, 1, true, true, true, expetcedAttributeDefinitionClass);
	}

	public static <T> Collection<T> getAttributeValues(ShadowType shadowType, QName name) {
		return getAttributeValues(shadowType.asPrismObject(), name);
	}
	
	public static <T> Collection<T> getAttributeValues(PrismObject<? extends ShadowType> shadow, QName name) {
		if (shadow == null) {
			throw new IllegalArgumentException("No shadow");
		}
		PrismContainer<?> attrCont = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attrCont == null) {
			return null;
		}
		PrismProperty<T> attrProp = attrCont.findProperty(name);
		if (attrProp == null) {
			return null;
		}
		return attrProp.getRealValues();
	}

	public static String getAttributeValue(ShadowType repoShadow, QName name) {
		
		Collection<String> values = getAttributeValues(repoShadow, name);
		if (values == null || values.isEmpty()) {
			AssertJUnit.fail("Attribute "+name+" not found in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		if (values.size() > 1) {
			AssertJUnit.fail("Too many values for attribute "+name+" in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		return values.iterator().next();
	}

	public static void displayTestTile(String testName) {
		System.out.println(TEST_OUT_PREFIX + testName + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + testName + TEST_LOG_SUFFIX);
	}

	public static void displayTestTile(Object testCase, String testName) {
		System.out.println(TEST_OUT_PREFIX + testCase.getClass().getSimpleName() + "." + testName + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + testCase.getClass().getSimpleName() + "." + testName + TEST_LOG_SUFFIX);
	}
	
	public static void displayWhen(String testName) {
		System.out.println(TEST_OUT_SECTION_PREFIX + " WHEN " + testName + TEST_OUT_SECTION_SUFFIX);
		LOGGER.info(TEST_LOG_SECTION_PREFIX + " WHEN " + testName + TEST_LOG_SECTION_SUFFIX);
	}

	public static void displayThen(String testName) {
		System.out.println(TEST_OUT_SECTION_PREFIX + " THEN " + testName + TEST_OUT_SECTION_SUFFIX);
		LOGGER.info(TEST_LOG_SECTION_PREFIX + " THEN " + testName + TEST_LOG_SECTION_SUFFIX);
	}

    public static void waitFor(String message, Checker checker, int timeoutInterval) throws Exception {
        waitFor(message, checker, timeoutInterval, WAIT_FOR_LOOP_SLEEP_MILIS);
    }

	public static void waitFor(String message, Checker checker, int timeoutInterval, long sleepInterval) throws Exception {
		System.out.println(message);
		LOGGER.debug(LOG_MESSAGE_PREFIX + message);
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeoutInterval) {
			boolean done = checker.check();
			if (done) {
				System.out.println("... done");
				LOGGER.trace(LOG_MESSAGE_PREFIX + "... done " + message);
				return;
			}
			Thread.sleep(sleepInterval);
		}
		// we have timeout
		System.out.println("Timeout while "+message);
		LOGGER.error(LOG_MESSAGE_PREFIX + "Timeout while " + message);
		// Invoke callback
		checker.timeout();
		throw new RuntimeException("Timeout while "+message);
	}

	public static void displayJaxb(String title, Object o, QName qname) throws JAXBException {
		Document doc = DOMUtil.getDocument();
		Element element = PrismTestUtil.marshalObjectToDom(o, qname, doc);
		String serialized = DOMUtil.serializeDOMToString(element);
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(serialized);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n" + serialized);
	}

	public static void display(String message) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
	}
	
	public static void display(String message, SearchResultEntry response) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
		display(response);
	}

	public static void display(SearchResultEntry response) {
		System.out.println(response == null ? "null" : response.toLDIFString());
		LOGGER.debug(response == null ? "null" : response.toLDIFString());
	}

	public static void display(String message, Task task) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(task.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message  + "\n" 
				+ task.dump());
	}

	public static void display(String message, ObjectType o) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(ObjectTypeUtil.dump(o));
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n" 
				+ ObjectTypeUtil.dump(o));
	}

	public static void display(String message, Collection collection) {
		String dump = DebugUtil.dump(collection);
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message + "\n" + dump);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n" + dump);
	}

	public static void displayObjectTypeCollection(String message, Collection<? extends ObjectType> collection) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
		for (ObjectType o : collection) {
			System.out.println(ObjectTypeUtil.dump(o));
			LOGGER.debug(ObjectTypeUtil.dump(o));
			System.out.println(OBJECT_LIST_SEPARATOR);
			LOGGER.debug(OBJECT_LIST_SEPARATOR);			
		}
	}

	public static void display(String title, Entry entry) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(entry.toLDIFString());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n" 
				+ entry.toLDIFString());
	}

	public static void display(String message, PrismContainer<?> propertyContainer) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(propertyContainer == null ? "null" : propertyContainer.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n" 
				+ (propertyContainer == null ? "null" : propertyContainer.dump()));
	}
	
	public static void display(OperationResult result) {
		display("Result of "+result.getOperation(), result);
	}
	
	public static void display(String title, OperationResult result) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(result.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n" 
				+ result.dump());
	}
	
	public static void display(String title, OperationResultType result) throws JAXBException {
		displayJaxb(title, result, SchemaConstants.C_RESULT);
	}

	public static void display(String title, List<Element> elements) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		for(Element e : elements) {
			String s = DOMUtil.serializeDOMToString(e);
			System.out.println(s);
			LOGGER.debug(s);
		}
	}
	
	public static void display(String title, Dumpable dumpable) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(dumpable == null ? "null" : dumpable.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n" 
				+ (dumpable == null ? "null" : dumpable.dump()));
	}
	
	public static void display(String title, String value) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(value);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n" 
				+ value);
	}

	public static void display(String title, Object value) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(SchemaDebugUtil.prettyPrint(value));
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n" 
				+ SchemaDebugUtil.prettyPrint(value));
	}
	
	public static void display(String title, Throwable e) {
		String stackTrace = ExceptionUtils.getStackTrace(e);
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title + ": "+e.getClass() + " " + e.getMessage());
		System.out.println(stackTrace);
		LOGGER.debug("{}{}: {} {}\n{}", new Object[]{
				OBJECT_TITLE_LOG_PREFIX, title, e.getClass(), e.getMessage(),
				stackTrace});
	}
	
	
	public static void checkAllShadows(ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(IntegrationTestTools.class.getName() + ".checkAllShadows");
		
		ObjectQuery query = createAllShadowsQuery(resourceType, prismContext);
		
		List<PrismObject<ShadowType>> allShadows = repositoryService.searchObjects(ShadowType.class, query, result);
		LOGGER.trace("Checking {} shadows, query:\n{}", allShadows.size(), query.dump());

		for (PrismObject<ShadowType> shadow: allShadows) {
            checkShadow(shadow.asObjectable(), resourceType, repositoryService, checker, prismContext, result);
		}
	}
	
	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		RefFilter equal = RefFilter.createReferenceEqual(ShadowType.class, ShadowType.F_RESOURCE_REF, prismContext, resourceType.getOid());
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		return query;
	}
	
	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, QName objectClass, PrismContext prismContext) throws SchemaException {
		AndFilter and = AndFilter.createAnd(
				RefFilter.createReferenceEqual(ShadowType.class, ShadowType.F_RESOURCE_REF, prismContext, resourceType.getOid()),
				EqualsFilter.createEqual(ShadowType.class, prismContext, ShadowType.F_OBJECT_CLASS, objectClass));
		ObjectQuery query = ObjectQuery.createObjectQuery(and);
		return query;
	}

	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, String objectClassLocalName, PrismContext prismContext) throws SchemaException {
		return createAllShadowsQuery(resourceType, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), objectClassLocalName), prismContext);
	}

	public static void checkAccountShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		checkShadow(shadowType, resourceType, repositoryService, checker, prismContext, parentResult);
		assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME),
				shadowType.getObjectClass());
	}
	
	public static void checkGroupShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		checkShadow(shadowType, resourceType, repositoryService, checker, prismContext, parentResult);
		assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_GROUP_OBJECT_CLASS_LOCAL_NAME),
				shadowType.getObjectClass());
	}
	
	public static void checkShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		LOGGER.trace("Checking shadow:\n{}",shadowType.asPrismObject().dump());
		shadowType.asPrismObject().checkConsistence(true, true);
		assertNotNull("no OID",shadowType.getOid());
		assertNotNull("no name",shadowType.getName());
        assertEquals(resourceType.getOid(), shadowType.getResourceRef().getOid());
        PrismContainer<?> attrs = shadowType.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("no attributes",attrs);
		assertFalse("empty attributes",attrs.isEmpty());
		String icfUid = ShadowUtil.getSingleStringAttributeValue(shadowType, SchemaTestConstants.ICFS_UID);
        assertNotNull("No ICF UID", icfUid);
		
		String resourceOid = ShadowUtil.getResourceOid(shadowType);
        assertNotNull("No resource OID in "+shadowType, resourceOid);
        
        assertNotNull("Null OID in "+shadowType, shadowType.getOid());
        PrismObject<ShadowType> repoShadow = null;
        try {
        	repoShadow = repositoryService.getObject(ShadowType.class, shadowType.getOid(), parentResult);
		} catch (Exception e) {
			AssertJUnit.fail("Got exception while trying to read "+shadowType+
					": "+e.getCause()+": "+e.getMessage());
		}
		
		checkShadowUniqueness(shadowType, repositoryService, prismContext, parentResult);
		
		String repoResourceOid = ShadowUtil.getResourceOid(repoShadow.asObjectable());
		assertNotNull("No resource OID in the repository shadow "+repoShadow);
		assertEquals("Resource OID mismatch", resourceOid, repoResourceOid);
		
		try {
        	repositoryService.getObject(ResourceType.class, resourceOid, parentResult);
		} catch (Exception e) {
			AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+shadowType+
					": "+e.getCause()+": "+e.getMessage());
		}
		
		if (checker != null) {
        	checker.check(shadowType);
        }
	}
	
	/**
	 * Checks i there is only a single shadow in repo for this account.
	 */
	private static void checkShadowUniqueness(ShadowType resourceShadow, RepositoryService repositoryService, 
			PrismContext prismContext, OperationResult parentResult) {
		try {
			ObjectQuery query = createShadowQuery(resourceShadow, prismContext);
			List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, parentResult);
			LOGGER.trace("Shadow check with filter\n{}\n found {} objects", query.dump(), results.size());
			if (results.size() == 0) {
				AssertJUnit.fail("No shadow found with query:\n"+query.dump());
			}
			if (results.size() == 1) {
				return;
			}
			if (results.size() > 1) {
				for (PrismObject<ShadowType> result: results) {
					LOGGER.trace("Search result:\n{}", result.dump());
				}
				LOGGER.error("More than one shadows found for " + resourceShadow);
				// TODO: Better error handling later
				throw new IllegalStateException("More than one shadows found for " + resourceShadow);
			}
		} catch (SchemaException e) {
			throw new SystemException(e);
		}
	}

	private static ObjectQuery createShadowQuery(ShadowType resourceShadow, PrismContext prismContext) throws SchemaException {
		
		XPathHolder xpath = new XPathHolder(ShadowType.F_ATTRIBUTES);
		PrismContainer<?> attributesContainer = resourceShadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> identifier = attributesContainer.findProperty(SchemaTestConstants.ICFS_UID);
		if (identifier == null) {
			throw new SchemaException("No identifier in "+resourceShadow);
		}

		Document doc = DOMUtil.getDocument();
//		Element filter;
		ObjectFilter filter;
		List<Element> identifierElements = prismContext.getPrismDomProcessor().serializeItemToDom(identifier, doc);
//		try {
//			filter = QueryUtil.createAndFilter(doc, QueryUtil.createEqualRefFilter(doc, null,
//					SchemaConstants.I_RESOURCE_REF, resourceShadow.getResourceRef().getOid()),
//					QueryUtil.createEqualFilterFromElements(doc, xpath, identifierElements, 
//							resourceShadow.asPrismObject().getPrismContext()));

//			filter = QueryUtil.createEqualFilterFromElements(doc, xpath, identifierElements, 
//					resourceShadow.asPrismObject().getPrismContext());
			ItemDefinition itemDef = resourceShadow.asPrismObject().getDefinition().findItemDefinition(ShadowType.F_RESOURCE_REF);
			filter = AndFilter.createAnd(
					RefFilter.createReferenceEqual(null, itemDef, resourceShadow.getResourceRef().getOid()),
					EqualsFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES), identifier.getDefinition(), identifier.getValue()));
			
//		} catch (SchemaException e) {
//			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
//		}

//		QueryType query = new QueryType();
//		query.setFilter(filter);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);

		return query;
		
	}
	
    public static void applyResourceSchema(ShadowType accountType, ResourceType resourceType, PrismContext prismContext) throws SchemaException {
    	ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
    	ShadowUtil.applyResourceSchema(accountType.asPrismObject(), resourceSchema);
    }
    
    public static void assertInMessageRecursive(Throwable e, String substring) {
    	assert hasInMessageRecursive(e, substring) : "The substring '"+substring+"' was NOT found in the message of exception "+e+" (including cause exceptions)";
    }
    
    public static boolean hasInMessageRecursive(Throwable e, String substring) {
    	if (e.getMessage().contains(substring)) {
    		return true;
    	}
    	if (e.getCause() != null) {
    		return hasInMessageRecursive(e.getCause(), substring);
    	}
    	return false;
    }
    
    public static void assertNotInMessageRecursive(Throwable e, String substring) {
    	assert !e.getMessage().contains(substring) : "The substring '"+substring+"' was found in the message of exception "+e+": "+e.getMessage();
    	if (e.getCause() != null) {
    		assertNotInMessageRecursive(e.getCause(), substring);
    	}
    }
    
    public static void assertNoRepoCache() {
		if (RepositoryCache.exists()) {
			AssertJUnit.fail("Cache exists! " + RepositoryCache.dump());
		}
	}
    
    public static void assertScripts(List<ScriptHistoryEntry> scriptsHistory, ProvisioningScriptSpec... expectedScripts) {
		displayScripts(scriptsHistory);
		assertEquals("Wrong number of scripts executed", expectedScripts.length, scriptsHistory.size());
		Iterator<ScriptHistoryEntry> historyIter = scriptsHistory.iterator();
		for (ProvisioningScriptSpec expecedScript: expectedScripts) {
			ScriptHistoryEntry actualScript = historyIter.next();
			assertEquals("Wrong script code", expecedScript.getCode(), actualScript.getCode());
			if (expecedScript.getLanguage() == null) {
				assertEquals("We talk only gibberish here", "Gibberish", actualScript.getLanguage());
			} else {
				assertEquals("Wrong script language", expecedScript.getLanguage(), actualScript.getLanguage());
			}
			assertEquals("Wrong number of arguments", expecedScript.getArgs().size(), actualScript.getParams().size());
			for (java.util.Map.Entry<String,Object> expectedEntry: expecedScript.getArgs().entrySet()) {
				Object expectedValue = expectedEntry.getValue();
				Object actualVal = actualScript.getParams().get(expectedEntry.getKey());
				assertEquals("Wrong value for argument '"+expectedEntry.getKey()+"'", expectedValue, actualVal);
			}
		}
	}

    public static void displayScripts(List<ScriptHistoryEntry> scriptsHistory) {
		for (ScriptHistoryEntry script : scriptsHistory) {
			display("Script", script);
		}
	}

}
