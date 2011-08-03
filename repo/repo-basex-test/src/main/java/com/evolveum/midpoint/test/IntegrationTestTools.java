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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.junit.Assert;
import org.opends.server.types.Attribute;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
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
	public static Trace logger = TraceManager.getTrace(IntegrationTestTools.class);
	
	private static final String TEST_OUT_PREFIX = "\n\n=====[ ";
	private static final String TEST_OUT_SUFFIX = " ]======================================\n";
	private static final String TEST_LOG_PREFIX = "=====[ ";
	private static final String TEST_LOG_SUFFIX = " ]======================================";
	private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
	private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";
	private static final String LOG_MESSAGE_PREFIX = "";
	private static final long WAIT_FOR_LOOP_SLEEP_MILIS = 500;

	public static void assertSuccess(String message, OperationResultType result) {
		if (!checkResults) {
			return;
		}
		assertEquals(message + ": " + result.getMessage(), OperationResultStatusType.SUCCESS,
				result.getStatus());
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			assertSuccess(message, subResult);
		}
	}

	public static void assertSuccess(String message, OperationResult result) {
		if (!checkResults) {
			return;
		}
		assertTrue(message + ": " + result.getMessage(), result.isSuccess());
		List<OperationResult> partialResults = result.getSubresults();
		for (OperationResult subResult : partialResults) {
			assertSuccess(message, subResult);
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

	public static void assertAttribute(ResourceObjectShadowType repoShadow, ResourceType resource, String name,
			String value) {
		assertAttribute("Wrong attribute " + name + " in shadow", repoShadow,
				new QName(resource.getNamespace(), name), value);
	}

	public static void assertAttribute(ResourceObjectShadowType repoShadow, QName name, String value) {
		List<String> values = getAttributeValues(repoShadow, name);
		assertEquals(1, values.size());
		assertEquals(value, values.get(0));
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
		List<Element> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Element element : xmlAttributes) {
			if (element.getNamespaceURI().equals(name.getNamespaceURI())
					&& element.getLocalName().equals(name.getLocalPart())) {
				values.add(element.getTextContent());
			}
		}
		return values;
	}

	public static void assertAttribute(SearchResultEntry response, String name, String value) {
		Assert.assertNotNull(response.getAttribute(name.toLowerCase()));
		Assert.assertEquals(1, response.getAttribute(name.toLowerCase()).size());
		Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
		Assert.assertEquals(value, attribute.iterator().next().getValue().toString());
	}

	public static void displayTestTile(String title) {
		System.out.println(TEST_OUT_PREFIX + title + TEST_OUT_SUFFIX);
		logger.info(TEST_LOG_PREFIX + title + TEST_LOG_SUFFIX);
	}

	public static void waitFor(String message, Checker checker, int timeoutInterval) throws Exception {
		System.out.println(message);
		logger.debug(LOG_MESSAGE_PREFIX + message);
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + timeoutInterval) {
			boolean done = checker.check();
			if (done) {
				System.out.println("... done");
				logger.debug(LOG_MESSAGE_PREFIX + "... done " + message);
				return;
			}
			Thread.sleep(WAIT_FOR_LOOP_SLEEP_MILIS);
		}
		// we have timeout
		System.out.println("Timeout while "+message);
		logger.error(LOG_MESSAGE_PREFIX + "Timeout while " + message);
		throw new RuntimeException("Timeout while "+message);
	}

	public static void displayJaxb(String title, Object o, QName qname) throws JAXBException {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		logger.debug(OBJECT_TITLE_LOG_PREFIX + title);
		displayJaxb(o, qname);
	}

	public static void displayJaxb(Object o, QName qname) throws JAXBException {
		Document doc = DOMUtil.getDocument();
		Element element = JAXBUtil.jaxbToDom(o, qname, doc);
		System.out.println(DOMUtil.serializeDOMToString(element));
		logger.debug(DOMUtil.serializeDOMToString(element));
	}

	public static void display(String message, SearchResultEntry response) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		logger.debug(OBJECT_TITLE_LOG_PREFIX + message);
		display(response);
	}

	public static void display(SearchResultEntry response) {
		System.out.println(response.toLDIFString());
		logger.debug(response.toLDIFString());
	}

	public static void display(String message, Task task) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(task.dump());
		logger.debug(OBJECT_TITLE_LOG_PREFIX + message);
		logger.debug(task.dump());
	}

	public static void display(String message, ObjectType o) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(ObjectTypeUtil.dump(o));
		logger.debug(OBJECT_TITLE_LOG_PREFIX + message);
		logger.debug(ObjectTypeUtil.dump(o));
	}

	public static void display(String title, Entry entry) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(entry.toLDIFString());
		logger.debug(OBJECT_TITLE_LOG_PREFIX + title);
		logger.debug(entry.toLDIFString());
	}

	public static void display(String message, PropertyContainer propertyContainer) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + message);
		System.out.println(propertyContainer.dump());
		logger.debug(OBJECT_TITLE_LOG_PREFIX + message);
		logger.debug(propertyContainer.dump());
	}

}
