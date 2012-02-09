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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.*;
import org.opends.server.types.Attribute;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.Schema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author Radovan Semancik
 *
 */
public class IntegrationTestTools {
	
	public static boolean checkResults = true;
	// public and not final - to allow changing it in tests
	public static Trace LOGGER = TraceManager.getTrace(IntegrationTestTools.class);
	
	private static final String TEST_OUT_PREFIX = "\n\n=====[ ";
	private static final String TEST_OUT_SUFFIX = " ]======================================\n";
	private static final String TEST_LOG_PREFIX = "=====[ ";
	private static final String TEST_LOG_SUFFIX = " ]======================================";
	private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
	private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";
	private static final String LOG_MESSAGE_PREFIX = "";
	private static final String OBJECT_LIST_SEPARATOR = "---";
	private static final long WAIT_FOR_LOOP_SLEEP_MILIS = 500;

	public static void assertSuccess(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		// Ignore top-level if the operation name is not set
		if (result.getOperation()!=null) {
			if (result.getStatus() == null || result.getStatus() == OperationResultStatusType.UNKNOWN) {
				fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
			}
			if (result.getStatus() != OperationResultStatusType.SUCCESS && result.getStatus() != OperationResultStatusType.NOT_APPLICABLE) {
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

	public static void assertSuccess(String message, OperationResult result) {
		assertSuccess(message, result,-1);
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
		assertSuccess(message, result, level, 0);
	}
	
	private static void assertSuccess(String message, OperationResult result, int stopLevel, int currentLevel) {
		if (!checkResults) {
			return;
		}
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
		}
		assertTrue(message + ": " + result.getMessage(), result.isSuccess());
		if (stopLevel == currentLevel) {
			return;
		}
		List<OperationResult> partialResults = result.getSubresults();
		for (OperationResult subResult : partialResults) {
			assertSuccess(message, subResult, stopLevel, currentLevel + 1);
		}
	}

	public static void assertNotEmpty(String message, String s) {
		assertNotNull(message, s);
		assertFalse(message, s.isEmpty());
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

	public static void assertAttribute(ResourceObjectShadowType repoShadow, ResourceType resource, String name,
			String value) {
		assertAttribute("Wrong attribute " + name + " in shadow", repoShadow,
				new QName(resource.getNamespace(), name), value);
	}

	public static void assertAttribute(ResourceObjectShadowType repoShadow, QName name, String value) {
		List<String> values = getAttributeValues(repoShadow, name);
		if (values == null || values.isEmpty()) {
			AssertJUnit.fail("Attribute "+name+" is not present in "+ObjectTypeUtil.toShortString(repoShadow));
		}
		if (values.size() > 1) {
			AssertJUnit.fail("Too many values for attribute "+name+" in "+ObjectTypeUtil.toShortString(repoShadow));	
		}
		assertEquals("Wrong value for attribute "+name+" in "+ObjectTypeUtil.toShortString(repoShadow), value, values.get(0));
	}

	public static void assertAttribute(String message, ResourceObjectShadowType repoShadow, QName name, String value) {
		List<String> values = getAttributeValues(repoShadow, name);
		assertEquals(message, 1, values.size());
		assertEquals(message, value, values.get(0));
	}

	public static void assertAttributeNotNull(ResourceObjectShadowType repoShadow, QName name) {
		List<String> values = getAttributeValues(repoShadow, name);
		assertEquals(1, values.size());
		assertNotNull(values.get(0));
	}

	public static void assertAttributeNotNull(String message, ResourceObjectShadowType repoShadow, QName name) {
		List<String> values = getAttributeValues(repoShadow, name);
		assertEquals(message, 1, values.size());
		assertNotNull(message, values.get(0));
	}

	public static List<String> getAttributeValues(ResourceObjectShadowType repoShadow, QName name) {
		List<String> values = new ArrayList<String>();
		List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Object element : xmlAttributes) {
			if (name.equals(JAXBUtil.getElementQName(element))) {
				values.add(((Element)element).getTextContent());
			}			
		}
		return values;
	}

	public static String getAttributeValue(ResourceObjectShadowType repoShadow, QName name) {
		
		List<String> values = getAttributeValues(repoShadow, name);
		if (values == null || values.isEmpty()) {
			AssertJUnit.fail("Attribute "+name+" not found in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		if (values.size() > 1) {
			AssertJUnit.fail("Too many values for attribute "+name+" in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		return values.get(0);
	}

	public static void displayTestTile(String title) {
		System.out.println(TEST_OUT_PREFIX + title + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + title + TEST_LOG_SUFFIX);
	}

	public static void displayTestTile(Object testCase, String title) {
		System.out.println(TEST_OUT_PREFIX + testCase.getClass().getSimpleName() + "." + title + TEST_OUT_SUFFIX);
		LOGGER.info(TEST_LOG_PREFIX + testCase.getClass().getSimpleName() + "." + title + TEST_LOG_SUFFIX);
	}
	
	public static void waitFor(String message, Checker checker, int timeoutInterval) throws Exception {
		System.out.println(message);
		LOGGER.debug(LOG_MESSAGE_PREFIX + message);
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeoutInterval) {
			boolean done = checker.check();
			if (done) {
				System.out.println("... done");
				LOGGER.debug(LOG_MESSAGE_PREFIX + "... done " + message);
				return;
			}
			Thread.sleep(WAIT_FOR_LOOP_SLEEP_MILIS);
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
		Element element = JAXBUtil.jaxbToDom(o, qname, doc);
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
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
		LOGGER.debug(task.dump());
	}

	public static void display(String message, ObjectType o) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(ObjectTypeUtil.dump(o));
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
		LOGGER.debug(ObjectTypeUtil.dump(o));
	}

	public static void display(String message, Collection<? extends ObjectType> collection) {
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
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(entry.toLDIFString());
	}

	public static void display(String message, PrismContainer propertyContainer) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(propertyContainer.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
		LOGGER.debug(propertyContainer.dump());
	}
	
	public static void display(String title, OperationResult result) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(result.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(result.dump());
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
		System.out.println(dumpable.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(dumpable.dump());
	}
	
	public static void display(String title, String value) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(value);
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(value);
	}

	public static void display(String title, Object value) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(SchemaDebugUtil.prettyPrint(value));
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(SchemaDebugUtil.prettyPrint(value));
	}

}
