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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeDefinition;
import static org.testng.AssertJUnit.*;

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
		assertSuccess(message, result, level, 0, false);
	}
	
	public static void assertSuccessOrWarning(String message, OperationResult result, int level) {
		assertSuccess(message, result, level, 0, true);
	}
	
	public static void assertSuccessOrWarning(String message, OperationResult result) {
		assertSuccess(message, result, -1, 0, true);
	}
	
	private static void assertSuccess(String message, OperationResult result, int stopLevel, int currentLevel, boolean warningOk) {
		if (!checkResults) {
			return;
		}
		if (result.getStatus() == null || result.getStatus().equals(OperationResultStatus.UNKNOWN)) {
			fail(message + ": undefined status ("+result.getStatus()+") on operation "+result.getOperation());
		}
		
		if (result.isSuccess()) {
			// OK
		} else if (warningOk && result.getStatus() == OperationResultStatus.WARNING) {
			// OK
		} else {
			assert false : message + ": " + result.getStatus() + ": " + result.getMessage();	
		}
		
		if (stopLevel == currentLevel) {
			return;
		}
		List<OperationResult> partialResults = result.getSubresults();
		for (OperationResult subResult : partialResults) {
			assertSuccess(message, subResult, stopLevel, currentLevel + 1, warningOk);
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
				new QName(ResourceTypeUtil.getResourceNamespace(resource), name), value);
	}

	public static void assertAttribute(ResourceObjectShadowType repoShadow, QName name, String value) {
		Collection<String> values = getAttributeValues(repoShadow, name);
		if (values == null || values.isEmpty()) {
			AssertJUnit.fail("Attribute "+name+" is not present in "+ObjectTypeUtil.toShortString(repoShadow));
		}
		if (values.size() > 1) {
			AssertJUnit.fail("Too many values for attribute "+name+" in "+ObjectTypeUtil.toShortString(repoShadow));	
		}
		assertEquals("Wrong value for attribute "+name+" in "+ObjectTypeUtil.toShortString(repoShadow), value, values.iterator().next());
	}

	public static void assertAttribute(String message, ResourceObjectShadowType repoShadow, QName name, String value) {
		Collection<String> values = getAttributeValues(repoShadow, name);
		assertEquals(message, 1, values.size());
		assertEquals(message, value, values.iterator().next());
	}

	public static void assertAttributeNotNull(ResourceObjectShadowType repoShadow, QName name) {
		Collection<String> values = getAttributeValues(repoShadow, name);
		assertEquals(1, values.size());
		assertNotNull(values.iterator().next());
	}

	public static void assertAttributeNotNull(String message, ResourceObjectShadowType repoShadow, QName name) {
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
	
	public static void assertProvisioningAccountShadow(PrismObject<AccountShadowType> account, ResourceType resourceType,
			Class<?> expetcedAttributeDefinitionClass) {
		// Check attribute definition
		PrismContainer attributesContainer = account.findContainer(AccountShadowType.F_ATTRIBUTES);
		assertEquals("Wrong attributes container class", ResourceAttributeContainer.class, attributesContainer.getClass());
		ResourceAttributeContainer rAttributesContainer = (ResourceAttributeContainer)attributesContainer;
		PrismContainerDefinition attrsDef = attributesContainer.getDefinition();
		assertNotNull("No attributes container definition", attrsDef);				
		assertTrue("Wrong attributes definition class "+attrsDef.getClass().getName(), attrsDef instanceof ResourceAttributeContainerDefinition);
		ResourceAttributeContainerDefinition rAttrsDef = (ResourceAttributeContainerDefinition)attrsDef;
		ObjectClassComplexTypeDefinition objectClassDef = rAttrsDef.getComplexTypeDefinition();
		assertNotNull("No object class definition in attributes definition", objectClassDef);
		assertEquals("Wrong object class in attributes definition", 
				new QName(resourceType.getNamespace(), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME), 
				objectClassDef.getTypeName());
		ResourceAttribute<?> icfsNameUid = rAttributesContainer.findAttribute(SchemaTestConstants.ICFS_UID);
		assertAttributeDefinition(icfsNameUid, DOMUtil.XSD_STRING, 0, 1, true, false, false, expetcedAttributeDefinitionClass);
		
		ResourceAttribute<Object> icfsNameAttr = rAttributesContainer.findAttribute(SchemaTestConstants.ICFS_NAME);
		assertAttributeDefinition(icfsNameAttr, DOMUtil.XSD_STRING, 1, 1, true, true, true, expetcedAttributeDefinitionClass);
	}

	public static Collection<String> getAttributeValues(ResourceObjectShadowType shadowType, QName name) {
		PrismObject shadow = shadowType.asPrismObject();
		PrismContainer attrCont = shadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		if (attrCont == null) {
			return null;
		}
		PrismProperty attrProp = attrCont.findProperty(name);
		if (attrProp == null) {
			return null;
		}
		return attrProp.getRealValues(String.class);
	}

	public static String getAttributeValue(ResourceObjectShadowType repoShadow, QName name) {
		
		Collection<String> values = getAttributeValues(repoShadow, name);
		if (values == null || values.isEmpty()) {
			AssertJUnit.fail("Attribute "+name+" not found in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		if (values.size() > 1) {
			AssertJUnit.fail("Too many values for attribute "+name+" in shadow "+ObjectTypeUtil.toShortString(repoShadow));
		}
		return values.iterator().next();
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
		System.out.println(dumpable == null ? "null" : dumpable.dump());
		LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
		LOGGER.debug(dumpable == null ? "null" : dumpable.dump());
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
	
	
	public static void checkAllShadows(ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<AccountShadowType> checker, PrismContext prismContext) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		OperationResult result = new OperationResult(IntegrationTestTools.class.getName() + ".checkAllShadows");
		
		QueryType query = createAllShadowsQuery(resourceType);
		
		List<PrismObject<AccountShadowType>> allShadows = repositoryService.searchObjects(AccountShadowType.class, query, null , result);
		LOGGER.trace("Checking {} shadows, query:\n{}", allShadows.size(), DOMUtil.serializeDOMToString(query.getFilter()));

		for (PrismObject<AccountShadowType> shadow: allShadows) {
            checkShadow(shadow.asObjectable(), resourceType, repositoryService, checker, prismContext, result);
		}
	}
	
	public static QueryType createAllShadowsQuery(ResourceType resourceType) throws SchemaException {
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		query.setFilter(QueryUtil.createAndFilter(doc,
				QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF, resourceType.getOid())
//				QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS, new QName(resourceType.getNamespace(), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME))
				));
		return query;
	}
	
	public static QueryType createAllShadowsQuery(ResourceType resourceType, QName objectClass) throws SchemaException {
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		query.setFilter(QueryUtil.createAndFilter(doc,
				QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF, resourceType.getOid()),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS, objectClass)
				));
		return query;
	}

	public static QueryType createAllShadowsQuery(ResourceType resourceType, String objectClassLocalName) throws SchemaException {
		return createAllShadowsQuery(resourceType, new QName(ResourceTypeUtil.getResourceNamespace(resourceType), objectClassLocalName));
	}

	
	public static void checkShadow(AccountShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService, 
			ObjectChecker<AccountShadowType> checker, PrismContext prismContext, OperationResult parentResult) {
		LOGGER.trace("Checking shadow:\n{}",shadowType.asPrismObject().dump());
		assertNotNull("no OID",shadowType.getOid());
		assertNotNull("no name",shadowType.getName());
		assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME),
				shadowType.getObjectClass());
        assertEquals(resourceType.getOid(), shadowType.getResourceRef().getOid());
        PrismContainer<?> attrs = shadowType.asPrismObject().findContainer(AccountShadowType.F_ATTRIBUTES);
		assertNotNull("no attributes",attrs);
		assertFalse("empty attributes",attrs.isEmpty());
		String icfUid = ResourceObjectShadowUtil.getSingleStringAttributeValue(shadowType, SchemaTestConstants.ICFS_UID);
        assertNotNull("No ICF UID", icfUid);
		
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(shadowType);
        assertNotNull("No resource OID in "+shadowType, resourceOid);
        
        assertNotNull("Null OID in "+shadowType, shadowType.getOid());
        PrismObject<AccountShadowType> repoShadow = null;
        try {
        	repoShadow = repositoryService.getObject(AccountShadowType.class, shadowType.getOid(), parentResult);
		} catch (Exception e) {
			AssertJUnit.fail("Got exception while trying to read "+shadowType+
					": "+e.getCause()+": "+e.getMessage());
		}
		
		checkShadowUniqueness(shadowType, repositoryService, prismContext, parentResult);
		
		String repoResourceOid = ResourceObjectShadowUtil.getResourceOid(repoShadow.asObjectable());
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
	private static void checkShadowUniqueness(AccountShadowType resourceShadow, RepositoryService repositoryService, 
			PrismContext prismContext, OperationResult parentResult) {
		try {
			QueryType query = createShadowQuery(resourceShadow, prismContext);
			List<PrismObject<AccountShadowType>> results = repositoryService.searchObjects(AccountShadowType.class, query, null, parentResult);
			LOGGER.trace("Shadow check with filter\n{}\n found {} objects", DOMUtil.serializeDOMToString(query.getFilter()), results.size());
			if (results.size() == 0) {
				AssertJUnit.fail("No shadow found with query:\n"+DOMUtil.serializeDOMToString(query.getFilter()));
			}
			if (results.size() == 1) {
				return;
			}
			if (results.size() > 1) {
				for (PrismObject<AccountShadowType> result: results) {
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

	private static QueryType createShadowQuery(AccountShadowType resourceShadow, PrismContext prismContext) throws SchemaException {
		
		XPathHolder xpath = new XPathHolder(AccountShadowType.F_ATTRIBUTES);
		PrismContainer<?> attributesContainer = resourceShadow.asPrismObject().findContainer(AccountShadowType.F_ATTRIBUTES);
		PrismProperty<String> identifier = attributesContainer.findProperty(SchemaTestConstants.ICFS_UID);

		Document doc = DOMUtil.getDocument();
		Element filter;
		List<Element> identifierElements = prismContext.getPrismDomProcessor().serializeItemToDom(identifier, doc);
		try {
			filter = QueryUtil.createAndFilter(doc, QueryUtil.createEqualRefFilter(doc, null,
					SchemaConstants.I_RESOURCE_REF, resourceShadow.getResourceRef().getOid()),
					QueryUtil.createEqualFilterFromElements(doc, xpath, identifierElements, 
							resourceShadow.asPrismObject().getPrismContext()));

//			filter = QueryUtil.createEqualFilterFromElements(doc, xpath, identifierElements, 
//					resourceShadow.asPrismObject().getPrismContext());

		} catch (SchemaException e) {
			throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
		}

		QueryType query = new QueryType();
		query.setFilter(filter);

		return query;
		
	}
	
    public static void applyResourceSchema(AccountShadowType accountType, ResourceType resourceType, PrismContext prismContext) throws SchemaException {
    	ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
    	ResourceObjectShadowUtil.applyResourceSchema(accountType.asPrismObject(), resourceSchema);
    }

}
