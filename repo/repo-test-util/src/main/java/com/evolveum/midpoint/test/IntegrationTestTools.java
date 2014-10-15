/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 *
 */
public class IntegrationTestTools {
	
	public static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";
	public static final String DBTABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";
	
	public static final String NS_RESOURCE_DUMMY_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
	public static final QName RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME = new QName(NS_RESOURCE_DUMMY_CONFIGURATION ,"uselessString");
	
	// public and not final - to allow changing it in tests
	public static Trace LOGGER = TraceManager.getTrace(IntegrationTestTools.class);
	
	private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
	private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";
	private static final String LOG_MESSAGE_PREFIX = "";
	private static final String OBJECT_LIST_SEPARATOR = "---";
	private static final long WAIT_FOR_LOOP_SLEEP_MILIS = 500;

	public static void assertTestResourceSuccess(OperationResult testResult, ConnectorTestOperation operation) {
		OperationResult opResult = testResult.findSubresult(operation.getOperation());
		assertNotNull("No result for "+operation, opResult);
		TestUtil.assertSuccess("Test resource failed (result): "+operation, opResult, 1);
	}

	public static void assertTestResourceFailure(OperationResult testResult, ConnectorTestOperation operation) {
		OperationResult opResult = testResult.findSubresult(operation.getOperation());
		assertNotNull("No result for "+operation, opResult);
		TestUtil.assertFailure("Test resource succeeded while expected failure (result): "+operation, opResult);
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
	
	public static <T> void assertAttribute(PrismObject<? extends ShadowType> shadow, ResourceType resource, String name,
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
	
	public static <T> void assertAttribute(String message, PrismObject<? extends ShadowType> repoShadow, QName name, T... expectedValues) {
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

	public static String getIcfsNameAttribute(PrismObject<ShadowType> shadow) {
		return getIcfsNameAttribute(shadow.asObjectable());
	}

	public static String getIcfsNameAttribute(ShadowType shadowType) {
		return getAttributeValue(shadowType, SchemaTestConstants.ICFS_NAME);
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
		QName attrName = attr.getElementName();
		assertNotNull("No definition for attribute "+attrName, definition);
		assertEquals("Wrong class of definition for attribute"+attrName, expetcedAttributeDefinitionClass, definition.getClass());
		assertEquals("Wrong type in definition for attribute"+attrName, expectedType, definition.getTypeName());
		assertEquals("Wrong minOccurs in definition for attribute"+attrName, minOccurs, definition.getMinOccurs());
		assertEquals("Wrong maxOccurs in definition for attribute"+attrName, maxOccurs, definition.getMaxOccurs());
		assertEquals("Wrong canRead in definition for attribute"+attrName, canRead, definition.canRead());
		assertEquals("Wrong canCreate in definition for attribute"+attrName, canCreate, definition.canAdd());
		assertEquals("Wrong canUpdate in definition for attribute"+attrName, canUpdate, definition.canModify());
	}
	
	public static void assertProvisioningAccountShadow(PrismObject<ShadowType> account, ResourceType resourceType,
			Class<?> expetcedAttributeDefinitionClass) {

        assertProvisioningShadow(account,resourceType,expetcedAttributeDefinitionClass,
        new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
	}

    public static void assertProvisioningShadow(PrismObject<ShadowType> account, ResourceType resourceType,
                                                       Class<?> expetcedAttributeDefinitionClass, QName objectClass) {
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
        assertEquals("Wrong object class in attributes definition", objectClass, objectClassDef.getTypeName());
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

	public static void displayJaxb(String title, Object o, QName defaultElementName) throws SchemaException {
		String serialized = PrismTestUtil.serializeAnyData(o, defaultElementName);
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
		System.out.println(task.debugDump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message  + "\n" 
				+ task.debugDump());
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
		System.out.println(propertyContainer == null ? "null" : propertyContainer.debugDump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n" 
				+ (propertyContainer == null ? "null" : propertyContainer.debugDump()));
	}
	
	public static void display(OperationResult result) {
		display("Result of "+result.getOperation(), result);
	}
	
	public static void display(String title, OperationResult result) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(result.debugDump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n" 
				+ result.debugDump());
	}
	
	public static void display(String title, OperationResultType result) throws SchemaException {
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
	
	public static void display(String title, DebugDumpable dumpable) {
		System.out.println(OBJECT_TITLE_OUT_PREFIX + title);
		System.out.println(dumpable == null ? "null" : dumpable.debugDump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title  + "\n" 
				+ (dumpable == null ? "null" : dumpable.debugDump()));
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
		
		List<PrismObject<ShadowType>> allShadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
		LOGGER.trace("Checking {} shadows, query:\n{}", allShadows.size(), query.debugDump());

		for (PrismObject<ShadowType> shadow: allShadows) {
            checkShadow(shadow.asObjectable(), resourceType, repositoryService, checker, prismContext, result);
		}
	}
	
	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, PrismContext prismContext) throws SchemaException {
		RefFilter equal = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, prismContext, resourceType.getOid());
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		return query;
	}
	
	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, QName objectClass, PrismContext prismContext) throws SchemaException {
		AndFilter and = AndFilter.createAnd(
				RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, prismContext, resourceType.getOid()),
				EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null, objectClass));
		ObjectQuery query = ObjectQuery.createObjectQuery(and);
		return query;
	}

	public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, String objectClassLocalName, PrismContext prismContext) throws SchemaException {
		return createAllShadowsQuery(resourceType, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), objectClassLocalName), prismContext);
	}

	
	public static void checkAccountShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		checkAccountShadow(shadowType, resourceType, repositoryService, checker, null, prismContext, parentResult);
	}
	
	public static void checkAccountShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, MatchingRule<String> uidMatchingRule, PrismContext prismContext, OperationResult parentResult) {
		checkShadow(shadowType, resourceType, repositoryService, checker, uidMatchingRule, prismContext, parentResult);
		assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME),
				shadowType.getObjectClass());
	}
	
	public static void checkEntitlementShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, String objectClassLocalName, PrismContext prismContext, OperationResult parentResult) {
		checkEntitlementShadow(shadowType, resourceType, repositoryService, checker, objectClassLocalName, null, prismContext, parentResult);
	}
	
	public static void checkEntitlementShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, String objectClassLocalName, MatchingRule<String> uidMatchingRule, PrismContext prismContext, OperationResult parentResult) {
		checkShadow(shadowType, resourceType, repositoryService, checker, uidMatchingRule, prismContext, parentResult);
		assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), objectClassLocalName),
				shadowType.getObjectClass());
	}
	
	public static void checkShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		checkShadow(shadowType, resourceType, repositoryService, checker, null, prismContext, parentResult);
	}
	
	public static void checkShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<ShadowType> checker, MatchingRule<String> uidMatchingRule, PrismContext prismContext, OperationResult parentResult) {
		LOGGER.trace("Checking shadow:\n{}",shadowType.asPrismObject().debugDump());
		shadowType.asPrismObject().checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
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
        	repoShadow = repositoryService.getObject(ShadowType.class, shadowType.getOid(), null, parentResult);
		} catch (Exception e) {
			AssertJUnit.fail("Got exception while trying to read "+shadowType+
					": "+e.getCause()+": "+e.getMessage());
		}
		
		checkShadowUniqueness(shadowType, repositoryService, uidMatchingRule, prismContext, parentResult);
		
		String repoResourceOid = ShadowUtil.getResourceOid(repoShadow.asObjectable());
		assertNotNull("No resource OID in the repository shadow "+repoShadow);
		assertEquals("Resource OID mismatch", resourceOid, repoResourceOid);
		
		try {
        	repositoryService.getObject(ResourceType.class, resourceOid, null, parentResult);
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
			MatchingRule<String> uidMatchingRule, PrismContext prismContext, OperationResult parentResult) {
		try {
			ObjectQuery query = createShadowQuery(resourceShadow, uidMatchingRule, prismContext);
			List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
			LOGGER.trace("Shadow check with filter\n{}\n found {} objects", query.debugDump(), results.size());
			if (results.size() == 0) {
				AssertJUnit.fail("No shadow found with query:\n"+query.debugDump());
			}
			if (results.size() == 1) {
				return;
			}
			if (results.size() > 1) {
				for (PrismObject<ShadowType> result: results) {
					LOGGER.trace("Search result:\n{}", result.debugDump());
				}
				LOGGER.error("More than one shadows found for " + resourceShadow);
				// TODO: Better error handling later
				throw new IllegalStateException("More than one shadows found for " + resourceShadow);
			}
		} catch (SchemaException e) {
			throw new SystemException(e);
		}
	}

	private static ObjectQuery createShadowQuery(ShadowType resourceShadow, MatchingRule<String> uidMatchingRule, PrismContext prismContext) throws SchemaException {
		
		PrismContainer<?> attributesContainer = resourceShadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> identifier = attributesContainer.findProperty(SchemaTestConstants.ICFS_UID);
		if (identifier == null) {
			throw new SchemaException("No identifier in "+resourceShadow);
		}
		String uidValue = identifier.getRealValue();
		if (uidMatchingRule != null) {
			uidValue = uidMatchingRule.normalize(uidValue);
		}

		ObjectFilter filter;
		PrismPropertyDefinition identifierDef = identifier.getDefinition();
		PrismReferenceDefinition itemDef = resourceShadow.asPrismObject().getDefinition().findReferenceDefinition(ShadowType.F_RESOURCE_REF);
		filter = AndFilter.createAnd(
					RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, prismContext, ShadowUtil.getResourceOid(resourceShadow)),
					EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, identifierDef.getName()), identifierDef, new PrismPropertyValue(uidValue)));
			
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
			AssertJUnit.fail("Cache exists! " + RepositoryCache.debugDump());
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

	public static void assertBetween(String message, XMLGregorianCalendar start, XMLGregorianCalendar end,
			XMLGregorianCalendar actual) {
		assertNotNull(message + " is null", actual);
		if (start != null) {
			assertTrue(message+": expected time to be after "+start+" but it was "+actual, 
				actual.compare(start) == DatatypeConstants.GREATER || actual.compare(start) == DatatypeConstants.EQUAL);
		}
		if (end != null) {
			assertTrue(message+": expected time to be before "+end+" but it was "+actual, 
				actual.compare(end) == DatatypeConstants.LESSER || actual.compare(end) == DatatypeConstants.EQUAL);
		}
	}

	public static void assertEqualsTimestamp(String message, XMLGregorianCalendar expected, XMLGregorianCalendar actual) {
		assertNotNull(message+"; expected "+expected, actual);
		assertTrue(message+"; expected "+expected+" but was "+actual, expected.compare(actual) == 0);
	}

	public static void assertCreateTimestamp(PrismObject<? extends ObjectType> object, XMLGregorianCalendar start,
			XMLGregorianCalendar end) {
		MetadataType metadata = object.asObjectable().getMetadata();
		assertNotNull("No metadata in "+object, metadata);
		assertBetween("createTimestamp in "+object, start, end, metadata.getCreateTimestamp());
	}

	public static void assertModifyTimestamp(PrismObject<? extends ObjectType> object, XMLGregorianCalendar start,
			XMLGregorianCalendar end) {
		MetadataType metadata = object.asObjectable().getMetadata();
		assertNotNull("No metadata in "+object, metadata);
		assertBetween("modifyTimestamp in "+object, start, end, metadata.getModifyTimestamp());
	}

	public static <T> void assertExtensionProperty(PrismObject<? extends ObjectType> object, QName propertyName, T... expectedValues) {
		PrismContainer<?> extension = object.getExtension();
		PrismAsserts.assertPropertyValue(extension, propertyName, expectedValues);
	}

	public static <T> void assertNoExtensionProperty(PrismObject<? extends ObjectType> object, QName propertyName) {
		PrismContainer<?> extension = object.getExtension();
		PrismAsserts.assertNoItem(extension, propertyName);
	}
	
	public static void assertIcfResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) {
		assertNotNull("No resource schema in "+resourceType, resourceSchema);
		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition for "+objectClassQname+" in resource schema", accountDefinition);
		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertTrue("Mismatched account definition: "+accountDefinition+" <-> "+accountDef, accountDefinition == accountDef);
		
		assertNotNull("No object class definition " + objectClassQname, accountDefinition);
		assertEquals("Object class " + objectClassQname + " is not account", ShadowKindType.ACCOUNT, accountDefinition.getKind());
		assertTrue("Object class " + objectClassQname + " is not default account", accountDefinition.isDefaultInAKind());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());
		
		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getIdentifiers();
		assertNotNull("Null identifiers for " + objectClassQname, identifiers);
		assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());

		
		ResourceAttributeDefinition icfAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertNotNull("No definition for attribute "+SchemaTestConstants.ICFS_UID, icfAttributeDefinition);
		assertTrue("Attribute "+SchemaTestConstants.ICFS_UID+" in not an identifier",icfAttributeDefinition.isIdentifier(accountDefinition));
		assertTrue("Attribute "+SchemaTestConstants.ICFS_UID+" in not in identifiers list",identifiers.contains(icfAttributeDefinition));
		
		Collection<? extends ResourceAttributeDefinition> secondaryIdentifiers = accountDefinition.getSecondaryIdentifiers();
		assertNotNull("Null secondary identifiers for " + objectClassQname, secondaryIdentifiers);
		assertFalse("Empty secondary identifiers for " + objectClassQname, secondaryIdentifiers.isEmpty());
		
		ResourceAttributeDefinition nameAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertNotNull("No definition for attribute "+SchemaTestConstants.ICFS_NAME, nameAttributeDefinition);
		assertTrue("Attribute "+SchemaTestConstants.ICFS_NAME+" in not an identifier",nameAttributeDefinition.isSecondaryIdentifier(accountDefinition));
		assertTrue("Attribute "+SchemaTestConstants.ICFS_NAME+" in not in identifiers list",secondaryIdentifiers.contains(nameAttributeDefinition));

		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceAttributeDefinition uidDef = accountDef
				.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canAdd());
		assertFalse("UID has update",uidDef.canModify());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		ResourceAttributeDefinition nameDef = accountDef
				.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canAdd());
		assertTrue("No NAME update",nameDef.canModify());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));
	}

	//TODO: add language parameter..for now, use xml serialization
	public static void displayXml(String message, PrismObject<? extends ObjectType> object) throws SchemaException {
		String xml = PrismTestUtil.serializeObjectToString(object, PrismContext.LANG_XML);
		display(message, xml);
	}

	public static ObjectDelta<ShadowType> createEntitleDelta(String accountOid, QName associationName, String groupOid, PrismContext prismContext) throws SchemaException {
		ShadowAssociationType association = new ShadowAssociationType();
		association.setName(associationName);
		ObjectReferenceType shadowRefType = new ObjectReferenceType();
		shadowRefType.setOid(groupOid);
		shadowRefType.setType(ShadowType.COMPLEX_TYPE);
		association.setShadowRef(shadowRefType);
		ItemPath entitlementAssociationPath = new ItemPath(ShadowType.F_ASSOCIATION);
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationAddContainer(ShadowType.class, 
				accountOid, entitlementAssociationPath, prismContext, association);
		return delta;
	}

	public static ObjectDelta<ShadowType> createDetitleDelta(String accountOid, QName associationName, String groupOid, PrismContext prismContext) throws SchemaException {
		ShadowAssociationType association = new ShadowAssociationType();
		association.setName(associationName);
		ObjectReferenceType shadowRefType = new ObjectReferenceType();
		shadowRefType.setOid(groupOid);
        shadowRefType.setType(ShadowType.COMPLEX_TYPE);
		association.setShadowRef(shadowRefType);
		ItemPath entitlementAssociationPath = new ItemPath(ShadowType.F_ASSOCIATION);
		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationDeleteContainer(ShadowType.class, 
				accountOid, entitlementAssociationPath, prismContext, association);
		return delta;
	}
	
	public static void assertGroupMember(DummyGroup group, String accountId) {
		assertGroupMember(group, accountId, false);
	}
	
	public static void assertGroupMember(DummyGroup group, String accountId, boolean caseIgnore) {
		Collection<String> members = group.getMembers();
		assertNotNull("No members in group "+group.getName()+", expected that "+accountId+" will be there", members);
		if (caseIgnore) {
			for (String member: members) {
				if (StringUtils.equalsIgnoreCase(accountId, member)) {
					return;
				}
			}
			AssertJUnit.fail("Account "+accountId+" is not member of group "+group.getName()+", members: "+members);
		} else {
			assertTrue("Account "+accountId+" is not member of group "+group.getName()+", members: "+members, members.contains(accountId));
		}
	}
	
	public static void assertNoGroupMember(DummyGroup group, String accountId) {
		Collection<String> members = group.getMembers();
		if (members == null) {
			return;
		}
		assertFalse("Account "+accountId+" IS member of group "+group.getName()+" while not expecting it, members: "+members, members.contains(accountId));
	}
	
	public static void assertNoGroupMembers(DummyGroup group) {
		Collection<String> members = group.getMembers();
		assertTrue("Group "+group.getName()+" has members while not expecting it, members: "+members, members == null || members.isEmpty());
	}
	
	public static void assertAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
		ShadowType accountType = shadow.asObjectable();
		List<ShadowAssociationType> associations = accountType.getAssociation();
		assertNotNull("Null associations in "+shadow, associations);
		assertFalse("Empty associations in "+shadow, associations.isEmpty());
		for (ShadowAssociationType association: associations) {
			if (associationName.equals(association.getName()) &&
					entitlementOid.equals(association.getShadowRef().getOid())) {
				return;
			}
		}
		AssertJUnit.fail("No association for entitlement "+entitlementOid+" in "+shadow);
	}
}
