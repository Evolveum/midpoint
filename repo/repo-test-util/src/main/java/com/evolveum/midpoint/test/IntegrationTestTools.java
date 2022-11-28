/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.AssertJUnit.*;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.cache.local.LocalRepoCacheCollection;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
public class IntegrationTestTools {

    // Constants from test-config.xml.
    public static final String CONST_USELESS = "xUSEless";
    public static final String CONST_DRINK = "rum";
    public static final String CONST_BLABLA = "Bla bla bla";

    public static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";
    public static final String DUMMY_CONNECTOR_LEGACY_UPDATE_TYPE = "com.evolveum.icf.dummy.connector.DummyConnectorLegacyUpdate";
    public static final String DBTABLE_CONNECTOR_TYPE = "org.identityconnectors.databasetable.DatabaseTableConnector";
    public static final String CONNECTOR_LDAP_TYPE = "com.evolveum.polygon.connector.ldap.LdapConnector";
    public static final String LDAP_CONNECTOR_TYPE = "com.evolveum.polygon.connector.ldap.LdapConnector";

    public static final String NS_RESOURCE_DUMMY_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.icf.dummy/com.evolveum.icf.dummy.connector.DummyConnector";
    public static final QName RESOURCE_DUMMY_CONFIGURATION_USELESS_STRING_ELEMENT_NAME = new QName(NS_RESOURCE_DUMMY_CONFIGURATION, "uselessString");

    // public and not final - to allow changing it in tests
    public static final Trace LOGGER = TraceManager.getTrace(IntegrationTestTools.class);

    private static final String OBJECT_TITLE_OUT_PREFIX = "\n*** ";
    private static final String OBJECT_TITLE_LOG_PREFIX = "*** ";
    static final String LOG_MESSAGE_PREFIX = "";
    private static final String OBJECT_LIST_SEPARATOR = "---";
    private static final long WAIT_FOR_LOOP_SLEEP_MILLIS = 500;

    private static boolean silentConsole;

    public static OperationResult assertSingleConnectorTestResult(OperationResult testResult) {
        List<OperationResult> connectorSubresults = getConnectorSubresults(testResult);
        assertEquals("Unexpected number of connector tests in test result", 1, connectorSubresults.size());
        return connectorSubresults.get(0);
    }

    private static List<OperationResult> getConnectorSubresults(OperationResult testResult) {
        return testResult.getSubresults().stream()
                .filter(r -> r.getOperation().equals(TestResourceOpNames.CONNECTOR_TEST.getOperation()))
                .collect(Collectors.toList());
    }

    public static void assertTestResourceSuccess(OperationResult testResult, TestResourceOpNames operation) {
        OperationResult opResult = testResult.findSubresult(operation.getOperation());
        assertNotNull("No result for " + operation, opResult);
        TestUtil.assertSuccess("Test resource failed (result): " + operation, opResult, 1);
    }

    public static void assertTestResourceFailure(OperationResult testResult, TestResourceOpNames operation) {
        OperationResult opResult = testResult.findSubresult(operation.getOperation());
        assertNotNull("No result for " + operation, opResult);
        TestUtil.assertFailure("Test resource succeeded while expected failure (result): " + operation, opResult);
    }

    public static void assertTestResourceNotApplicable(OperationResult testResult, TestResourceOpNames operation) {
        OperationResult opResult = testResult.findSubresult(operation.getOperation());
        assertNotNull("No result for " + operation, opResult);
        assertEquals("Test resource status is not 'not applicable', it is " + opResult.getStatus() + ": " + operation,
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
        assertNotEmpty(message, qname.getNamespaceURI());
        assertNotEmpty(message, qname.getLocalPart());
    }

    public static void assertNotEmpty(QName qname) {
        assertNotNull(qname);
        assertNotEmpty(qname.getNamespaceURI());
        assertNotEmpty(qname.getLocalPart());
    }

    @SafeVarargs
    public static <T> void assertAttribute(
            ShadowType shadow, ResourceType resource, String name, T... expectedValues) {
        assertAttribute("Wrong attribute " + name + " in " + shadow, shadow,
                toRiQName(name), expectedValues);
    }

    @NotNull
    private static QName toRiQName(String name) {
        return new QName(MidPointConstants.NS_RI, name);
    }

    @SafeVarargs
    public static <T> void assertAttribute(PrismObject<? extends ShadowType> shadow, String name, T... expectedValues) {
        assertAttribute("Wrong attribute " + name + " in " + shadow, shadow, toRiQName(name), expectedValues);
    }

    @SafeVarargs
    public static <T> void assertAttribute(ShadowType shadowType, QName name, T... expectedValues) {
        assertAttribute(shadowType.asPrismObject(), name, expectedValues);
    }

    @SafeVarargs
    public static <T> void assertAttribute(
            PrismObject<? extends ShadowType> shadow, QName name, T... expectedValues) {
        Collection<T> values = getAttributeValues(shadow, name);
        assertEqualsCollection("Wrong value for attribute " + name + " in " + shadow, expectedValues, values);
    }

    @SafeVarargs
    public static <T> void assertAttribute(
            String message, ShadowType repoShadow, QName name, T... expectedValues) {
        Collection<T> values = getAttributeValues(repoShadow, name);
        assertEqualsCollection(message, expectedValues, values);
    }

    @SafeVarargs
    public static <T> void assertAttribute(String message,
            PrismObject<? extends ShadowType> repoShadow, QName name, T... expectedValues) {
        Collection<T> values = getAttributeValues(repoShadow, name);
        assertEqualsCollection(message, expectedValues, values);
    }

    public static void assertNoAttribute(PrismObject<? extends ShadowType> shadow, QName name) {
        assertNull("Found attribute " + name + " in " + shadow + " while not expecting it", getAttributeValues(shadow, name));
    }

    public static <T> void assertEqualsCollection(String message, Collection<T> expectedValues, Collection<T> actualValues) {
        if (expectedValues == null && actualValues == null) {
            return;
        }

        assert !(expectedValues == null) : "Expecting null values but got " + actualValues;
        assert actualValues != null : message + ": Expecting " + expectedValues + " but got null";
        assertEquals(message + ": Wrong number of values in " + actualValues, expectedValues.size(), actualValues.size());
        for (T actualValue : actualValues) {
            boolean found = false;
            for (T value : expectedValues) {
                if (value.equals(actualValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail(message + ": Unexpected value " + actualValue + "; expected " + expectedValues + "; has " + actualValues);
            }
        }
    }

    @UnusedTestElement
    public static <T> void assertEqualsCollection(String message, Collection<T> expectedValues, T[] actualValues) {
        assertEqualsCollection(message, expectedValues, Arrays.asList(actualValues));
    }

    public static <T> void assertEqualsCollection(String message, T[] expectedValues, Collection<T> actualValues) {
        assertEqualsCollection(message, Arrays.asList(expectedValues), actualValues);
    }

    @UnusedTestElement
    public static String getIcfsNameAttribute(PrismObject<ShadowType> shadow) {
        return getIcfsNameAttribute(shadow.asObjectable());
    }

    public static String getIcfsNameAttribute(ShadowType shadowType) {
        return getAttributeValue(shadowType, SchemaConstants.ICFS_NAME);
    }

    public static String getSecondaryIdentifier(PrismObject<ShadowType> shadow) {
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
        if (secondaryIdentifiers == null || secondaryIdentifiers.isEmpty()) {
            return null;
        }
        if (secondaryIdentifiers.size() > 1) {
            throw new IllegalArgumentException("Too many secondary indentifiers in " + shadow);
        }
        return (String) secondaryIdentifiers.iterator().next().getRealValue();
    }

    public static void assertSecondaryIdentifier(PrismObject<ShadowType> repoShadow, String value) {
        assertEquals("Wrong secondary indetifier in " + repoShadow, value, getSecondaryIdentifier(repoShadow));
    }

    @UnusedTestElement
    public static void assertIcfsNameAttribute(ShadowType repoShadow, String value) {
        assertAttribute(repoShadow, SchemaConstants.ICFS_NAME, value);
    }

    public static void assertIcfsNameAttribute(PrismObject<ShadowType> repoShadow, String value) {
        assertAttribute(repoShadow, SchemaConstants.ICFS_NAME, value);
    }

    public static void assertAttributeNotNull(PrismObject<ShadowType> repoShadow, QName name) {
        Collection<String> values = getAttributeValues(repoShadow, name);
        assertFalse("No values for " + name + " in " + repoShadow, values == null || values.isEmpty());
        assertEquals(1, values.size());
        assertNotNull(values.iterator().next());
    }

    public static void assertAttributeNotNull(ShadowType repoShadow, QName name) {
        Collection<String> values = getAttributeValues(repoShadow, name);
        assertFalse("No values for " + name + " in " + repoShadow, values == null || values.isEmpty());
        assertEquals(1, values.size());
        assertNotNull(values.iterator().next());
    }

    public static void assertAttributeNotNull(String message, ShadowType repoShadow, QName name) {
        Collection<String> values = getAttributeValues(repoShadow, name);
        assertFalse("No values for " + name + " in " + repoShadow, values == null || values.isEmpty());
        assertEquals(message, 1, values.size());
        assertNotNull(message, values.iterator().next());
    }

    public static void assertAttributeDefinition(ResourceAttribute<?> attr, QName expectedType, int minOccurs, int maxOccurs,
            boolean canRead, boolean canCreate, boolean canUpdate, Class<?> expectedAttributeDefinitionClass) {
        ResourceAttributeDefinition definition = attr.getDefinition();
        QName attrName = attr.getElementName();
        assertNotNull("No definition for attribute " + attrName, definition);
        //assertEquals("Wrong class of definition for attribute"+attrName, expetcedAttributeDefinitionClass, definition.getClass());
        assertTrue("Wrong class of definition for attribute" + attrName + " (expected: " + expectedAttributeDefinitionClass
                        + ", real: " + definition.getClass() + ")",
                expectedAttributeDefinitionClass.isAssignableFrom(definition.getClass()));
        assertEquals("Wrong type in definition for attribute" + attrName, expectedType, definition.getTypeName());
        assertEquals("Wrong minOccurs in definition for attribute" + attrName, minOccurs, definition.getMinOccurs());
        assertEquals("Wrong maxOccurs in definition for attribute" + attrName, maxOccurs, definition.getMaxOccurs());
        assertEquals("Wrong canRead in definition for attribute" + attrName, canRead, definition.canRead());
        assertEquals("Wrong canCreate in definition for attribute" + attrName, canCreate, definition.canAdd());
        assertEquals("Wrong canUpdate in definition for attribute" + attrName, canUpdate, definition.canModify());
    }

    public static void assertProvisioningAccountShadow(PrismObject<ShadowType> account, ResourceType resourceType,
            Class<?> expetcedAttributeDefinitionClass) {

        assertProvisioningShadow(account, expetcedAttributeDefinitionClass,
                toRiQName(SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
    }

    public static void assertProvisioningShadow(PrismObject<ShadowType> account,
            Class<?> expectedAttributeDefinitionClass, QName objectClass) {
        // Check attribute definition
        PrismContainer<?> attributesContainer = account.findContainer(ShadowType.F_ATTRIBUTES);
        PrismAsserts.assertClass("Wrong attributes container class", ResourceAttributeContainer.class, attributesContainer);
        ResourceAttributeContainer rAttributesContainer = (ResourceAttributeContainer) attributesContainer;
        PrismContainerDefinition attrsDef = attributesContainer.getDefinition();
        assertNotNull("No attributes container definition", attrsDef);
        assertTrue("Wrong attributes definition class " + attrsDef.getClass().getName(), attrsDef instanceof ResourceAttributeContainerDefinition);
        ResourceAttributeContainerDefinition rAttrsDef = (ResourceAttributeContainerDefinition) attrsDef;
        ResourceObjectClassDefinition objectClassDef = rAttrsDef.getComplexTypeDefinition().getObjectClassDefinition();
        assertNotNull("No object class definition in attributes definition", objectClassDef);
        assertEquals("Wrong object class in attributes definition", objectClass, objectClassDef.getTypeName());
        ResourceAttributeDefinition primaryIdDef = objectClassDef.getPrimaryIdentifiers().iterator().next();
        ResourceAttribute<?> primaryIdAttr = rAttributesContainer.findAttribute(primaryIdDef.getItemName());
        assertNotNull("No primary ID " + primaryIdDef.getItemName() + " in " + account, primaryIdAttr);
        assertAttributeDefinition(primaryIdAttr, DOMUtil.XSD_STRING, 0, 1, true, false, false, expectedAttributeDefinitionClass);

        ResourceAttributeDefinition secondaryIdDef = objectClassDef.getSecondaryIdentifiers().iterator().next();
        ResourceAttribute<Object> secondaryIdAttr = rAttributesContainer.findAttribute(secondaryIdDef.getItemName());
        assertNotNull("No secondary ID " + secondaryIdDef.getItemName() + " in " + account, secondaryIdAttr);
        assertAttributeDefinition(secondaryIdAttr, DOMUtil.XSD_STRING, 1, 1, true, true, true, expectedAttributeDefinitionClass);
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
        PrismProperty<T> attrProp = attrCont.findProperty(ItemName.fromQName(name));
        if (attrProp == null) {
            return null;
        }
        return attrProp.getRealValues();
    }

    public static String getAttributeValue(ShadowType repoShadow, QName name) {

        Collection<String> values = getAttributeValues(repoShadow, name);
        if (values == null || values.isEmpty()) {
            AssertJUnit.fail("Attribute " + name + " not found in shadow " + ObjectTypeUtil.toShortString(repoShadow));
        }
        if (values.size() > 1) {
            AssertJUnit.fail("Too many values for attribute " + name + " in shadow " + ObjectTypeUtil.toShortString(repoShadow));
        }
        return values.iterator().next();
    }

    public static void waitFor(String message, Checker checker, long timeoutInterval) throws CommonException {
        waitFor(message, checker, timeoutInterval, WAIT_FOR_LOOP_SLEEP_MILLIS);
    }

    public static void waitFor(String message, Checker checker, long timeoutInterval, long sleepInterval) throws CommonException {
        long startTime = System.currentTimeMillis();
        waitFor(message, checker, startTime, timeoutInterval, sleepInterval);
    }

    public static void waitFor(String message, Checker checker, long startTime, long timeoutInterval, long sleepInterval) throws CommonException {
        println(message);
        LOGGER.debug(LOG_MESSAGE_PREFIX + message);
        while (System.currentTimeMillis() < startTime + timeoutInterval) {
            boolean done = checker.check();
            if (done) {
                println("... done");
                LOGGER.trace(LOG_MESSAGE_PREFIX + "... done " + message);
                return;
            }
            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                LOGGER.warn("Sleep interrupted: {}", e.getMessage(), e);
            }
        }
        // we have timeout
        println("Timeout while " + message);
        LOGGER.error(LOG_MESSAGE_PREFIX + "Timeout while " + message);
        // Invoke callback
        checker.timeout();
        throw new RuntimeException("Timeout while " + message);
    }

    public static void displayJaxb(String title, Object o, QName defaultElementName) throws SchemaException {
        String serialized = o != null ? PrismTestUtil.serializeAnyData(o, defaultElementName) : "(null)";
        println(OBJECT_TITLE_OUT_PREFIX + title);
        println(serialized);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n" + serialized);
    }

    public static void display(String message) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
    }

    public static void display(String message, SearchResultEntry response) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        display(response);
    }

    public static void display(Entry response) {
        println(response == null ? "null" : response.toLDIFString());
        LOGGER.debug(response == null ? "null" : response.toLDIFString());
    }

    public static void display(String message, Task task) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        println(task.debugDump());
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n"
                + task.debugDump());
    }

    public static void display(String message, ObjectType o) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        println(ObjectTypeUtil.dump(o));
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n"
                + ObjectTypeUtil.dump(o));
    }

    public static void display(String message, Collection<?> collection) {
        String dump;
        if (collection == null) {
            dump = ": null";
        } else {
            dump = " (" + collection.size() + ")\n" + DebugUtil.dump(collection);
        }
        println(OBJECT_TITLE_OUT_PREFIX + message + dump);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + dump);
    }

    public static void display(String title, Entry entry) {
        println(OBJECT_TITLE_OUT_PREFIX + title);
        String ldif = null;
        if (entry != null) {
            ldif = entry.toLDIFString();
        }
        println(ldif);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                + ldif);
    }

    public static void display(String message, PrismContainer<?> propertyContainer) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        println(propertyContainer == null ? "null" : propertyContainer.debugDump());
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message + "\n"
                + (propertyContainer == null ? "null" : propertyContainer.debugDump()));
    }

    public static void display(OperationResult result) {
        display("Result of " + result.getOperation(), result);
    }

    public static void display(String title, OperationResult result) {
        println(OBJECT_TITLE_OUT_PREFIX + title);
        String debugDump = result != null ? result.debugDump() : "(null)";
        println(debugDump);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                + debugDump);
    }

    public static void display(String title, OperationResultType result) throws SchemaException {
        displayJaxb(title, result, SchemaConstants.C_RESULT);
    }

    public static void display(String title, List<Element> elements) {
        println(OBJECT_TITLE_OUT_PREFIX + title);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title);
        for (Element e : elements) {
            String s = DOMUtil.serializeDOMToString(e);
            println(s);
            LOGGER.debug(s);
        }
    }

    public static void display(String title, String value) {
        println(OBJECT_TITLE_OUT_PREFIX + title);
        println(value);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                + value);
    }

    public static void display(String title, Object value) {
        println(OBJECT_TITLE_OUT_PREFIX + title);
        println(SchemaDebugUtil.prettyPrint(value));
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                + SchemaDebugUtil.prettyPrint(value));
    }

    public static void display(String title, Containerable value) {
        if (value == null) {
            println(OBJECT_TITLE_OUT_PREFIX + title + ": null");
            LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + ": null");
        } else {
            println(OBJECT_TITLE_OUT_PREFIX + title);
            println(SchemaDebugUtil.prettyPrint(value.asPrismContainerValue().debugDump()));
            LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + title + "\n"
                    + SchemaDebugUtil.prettyPrint(value.asPrismContainerValue().debugDump(1)));
        }
    }

    public static void displayPrismValuesCollection(String message, Collection<? extends PrismValue> collection) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        for (PrismValue v : collection) {
            println(DebugUtil.debugDump(v));
            LOGGER.debug("{}", DebugUtil.debugDump(v));
            println(OBJECT_LIST_SEPARATOR);
            LOGGER.debug(OBJECT_LIST_SEPARATOR);
        }
    }

    public static void displayContainerablesCollection(String message, Collection<? extends Containerable> collection) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        for (Containerable c : CollectionUtils.emptyIfNull(collection)) {
            String s = DebugUtil.debugDump(c.asPrismContainerValue());
            println(s);
            LOGGER.debug("{}", s);
            println(OBJECT_LIST_SEPARATOR);
            LOGGER.debug(OBJECT_LIST_SEPARATOR);
        }
    }

    public static void displayCollection(String message, Collection<? extends DebugDumpable> collection) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        for (DebugDumpable c : CollectionUtils.emptyIfNull(collection)) {
            String s = DebugUtil.debugDump(c);
            println(s);
            LOGGER.debug("{}", s);
            println(OBJECT_LIST_SEPARATOR);
            LOGGER.debug(OBJECT_LIST_SEPARATOR);
        }
    }

    public static <K> void displayMap(String message, Map<K, ? extends DebugDumpable> map) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        for (Map.Entry<K, ? extends DebugDumpable> entry : map.entrySet()) {
            String s = entry.getKey() + " -> " + DebugUtil.debugDump(entry.getValue());
            println(s);
            LOGGER.debug("{}", s);
            println(OBJECT_LIST_SEPARATOR);
            LOGGER.debug(OBJECT_LIST_SEPARATOR);
        }
    }

    public static void displayObjectTypeCollection(String message, Collection<? extends ObjectType> collection) {
        println(OBJECT_TITLE_OUT_PREFIX + message);
        LOGGER.debug(OBJECT_TITLE_LOG_PREFIX + message);
        for (ObjectType o : CollectionUtils.emptyIfNull(collection)) {
            println(ObjectTypeUtil.dump(o));
            LOGGER.debug(ObjectTypeUtil.dump(o));
            println(OBJECT_LIST_SEPARATOR);
            LOGGER.debug(OBJECT_LIST_SEPARATOR);
        }
    }

    public static <O extends ObjectType> void assertSearchResultNames(SearchResultList<PrismObject<O>> resultList, MatchingRule<String> matchingRule, String... expectedNames) throws SchemaException {
        List<String> names = new ArrayList<>(expectedNames.length);
        for (PrismObject<O> obj : resultList) {
            names.add(obj.asObjectable().getName().getOrig());
        }
        PrismAsserts.assertSets("Unexpected search result", matchingRule, names, expectedNames);
    }

    @UnusedTestElement
    public static <O extends ObjectType> void assertSearchResultNames(
            SearchResultList<PrismObject<O>> resultList, String... expectedNames) {
        List<String> names = new ArrayList<>(expectedNames.length);
        for (PrismObject<O> obj : resultList) {
            names.add(obj.asObjectable().getName().getOrig());
        }
        PrismAsserts.assertSets("Unexpected search result", names, expectedNames);
    }

    public static void checkAllShadows(ResourceType resourceType, RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker, PrismContext prismContext) throws SchemaException, ConfigurationException {
        OperationResult result = new OperationResult(IntegrationTestTools.class.getName() + ".checkAllShadows");

        ObjectQuery query = createAllShadowsQuery(resourceType, prismContext);

        List<PrismObject<ShadowType>> allShadows = repositoryService.searchObjects(ShadowType.class, query, GetOperationOptions.createRawCollection(), result);
        LOGGER.trace("Checking {} shadows, query:\n{}", allShadows.size(), query.debugDump());

        for (PrismObject<ShadowType> shadow : allShadows) {
            checkShadow(shadow.asObjectable(), resourceType, repositoryService, checker, prismContext, result);
        }
    }

    public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, PrismContext prismContext) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
                .build();
    }

    public static ObjectQuery createAllShadowsQuery(ResourceType resourceType, QName objectClass, PrismContext prismContext) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                .build();
    }

    public static ObjectQuery createAllShadowsQuery(
            ResourceType resourceType, String objectClassLocalName, PrismContext prismContext) {
        return createAllShadowsQuery(resourceType, toRiQName(objectClassLocalName), prismContext);
    }

    @UnusedTestElement
    public static void checkAccountShadow(ShadowType shadowType, ResourceType resourceType, RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker, PrismContext prismContext, OperationResult parentResult)
            throws SchemaException, ConfigurationException {
        checkAccountShadow(shadowType, resourceType, repositoryService, checker, null, prismContext, parentResult);
    }

    public static void checkAccountShadow(
            ShadowType shadowType,
            ResourceType resourceType,
            RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker,
            MatchingRule<String> uidMatchingRule,
            PrismContext prismContext,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException {
        checkShadow(shadowType, resourceType, repositoryService, checker, uidMatchingRule, prismContext, parentResult);
        assertEquals(toRiQName(SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME),
                shadowType.getObjectClass());
    }

    @UnusedTestElement
    public static void checkEntitlementShadow(
            ShadowType shadowType,
            ResourceType resourceType,
            RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker,
            String objectClassLocalName,
            PrismContext prismContext,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException {
        checkEntitlementShadow(shadowType, resourceType, repositoryService, checker, objectClassLocalName, null, prismContext, parentResult);
    }

    public static void checkEntitlementShadow(
            ShadowType shadowType,
            ResourceType resourceType,
            RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker,
            String objectClassLocalName,
            MatchingRule<String> uidMatchingRule,
            PrismContext prismContext,
            OperationResult parentResult) throws SchemaException, ConfigurationException {
        checkShadow(shadowType, resourceType, repositoryService, checker, uidMatchingRule, prismContext, parentResult);
        assertEquals(toRiQName(objectClassLocalName),
                shadowType.getObjectClass());
    }

    public static void checkShadow(
            ShadowType shadowType,
            ResourceType resourceType,
            RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker,
            PrismContext prismContext,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException {
        checkShadow(shadowType, resourceType, repositoryService, checker, null, prismContext, parentResult);
    }

    public static void checkShadow(
            ShadowType shadowType,
            ResourceType resourceType,
            RepositoryService repositoryService,
            ObjectChecker<ShadowType> checker,
            MatchingRule<String> uidMatchingRule,
            PrismContext prismContext,
            OperationResult parentResult) throws SchemaException, ConfigurationException {
        LOGGER.trace("Checking shadow:\n{}", shadowType.asPrismObject().debugDump());
        shadowType.asPrismObject().checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
        assertNotNull("no OID", shadowType.getOid());
        assertNotNull("no name", shadowType.getName());
        assertEquals(resourceType.getOid(), shadowType.getResourceRef().getOid());
        PrismContainer<?> attrs = shadowType.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("no attributes", attrs);
        assertFalse("empty attributes", attrs.isEmpty());

        ResourceSchema rschema = ResourceSchemaFactory.getCompleteSchema(resourceType);
        ResourceObjectDefinition objectClassDef = rschema.findDefinitionForObjectClass(shadowType.getObjectClass());
        assertNotNull("cannot determine object class for " + shadowType, objectClassDef);

        String icfUid = ShadowUtil.getSingleStringAttributeValue(shadowType, SchemaConstants.ICFS_UID);
        if (icfUid == null) {
            Collection<? extends ResourceAttributeDefinition> identifierDefs = objectClassDef.getPrimaryIdentifiers();
            assertFalse("No identifiers for " + objectClassDef, identifierDefs == null || identifierDefs.isEmpty());
            for (ResourceAttributeDefinition idDef : identifierDefs) {
                String id = ShadowUtil.getSingleStringAttributeValue(shadowType, idDef.getItemName());
                assertNotNull("No identifier " + idDef.getItemName() + " in " + shadowType, id);
            }
        }

        String resourceOid = ShadowUtil.getResourceOid(shadowType);
        assertNotNull("No resource OID in " + shadowType, resourceOid);

        assertNotNull("Null OID in " + shadowType, shadowType.getOid());
        PrismObject<ShadowType> repoShadow = null;
        try {
            repoShadow = repositoryService.getObject(ShadowType.class, shadowType.getOid(), null, parentResult);
        } catch (Exception e) {
            AssertJUnit.fail("Got exception while trying to read " + shadowType +
                    ": " + e.getCause() + ": " + e.getMessage());
        }

        checkShadowUniqueness(shadowType, objectClassDef, repositoryService, uidMatchingRule, prismContext, parentResult);

        String repoResourceOid = ShadowUtil.getResourceOid(repoShadow.asObjectable());
        assertNotNull("No resource OID in the repository shadow " + repoShadow, repoResourceOid);
        assertEquals("Resource OID mismatch", resourceOid, repoResourceOid);

        try {
            repositoryService.getObject(ResourceType.class, resourceOid, null, parentResult);
        } catch (Exception e) {
            AssertJUnit.fail("Got exception while trying to read resource " + resourceOid + " as specified in current shadow " + shadowType +
                    ": " + e.getCause() + ": " + e.getMessage());
        }

        if (checker != null) {
            checker.check(shadowType);
        }
    }

    /**
     * Checks i there is only a single shadow in repo for this account.
     */
    private static void checkShadowUniqueness(
            ShadowType resourceShadow, ResourceObjectDefinition objectClassDef, RepositoryService repositoryService,
            MatchingRule<String> uidMatchingRule, PrismContext prismContext, OperationResult parentResult) {
        try {
            ObjectQuery query = createShadowQuery(resourceShadow, objectClassDef, uidMatchingRule, prismContext);
            List<PrismObject<ShadowType>> results = repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
            LOGGER.trace("Shadow check with filter\n{}\n found {} objects", query.debugDump(), results.size());
            if (results.size() == 0) {
                AssertJUnit.fail("No shadow found with query:\n" + query.debugDump());
            }
            if (results.size() == 1) {
                return;
            }
            if (results.size() > 1) {
                for (PrismObject<ShadowType> result : results) {
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

    private static ObjectQuery createShadowQuery(
            ShadowType resourceShadow, ResourceObjectDefinition objectClassDef,
            MatchingRule<String> uidMatchingRule, PrismContext prismContext)
            throws SchemaException {

        PrismContainer<?> attributesContainer = resourceShadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        QName identifierName = objectClassDef.getPrimaryIdentifiers().iterator().next().getItemName();
        PrismProperty<String> identifier = attributesContainer.findProperty(ItemName.fromQName(identifierName));
        if (identifier == null) {
            throw new SchemaException("No identifier in " + resourceShadow);
        }
        String identifierValue = identifier.getRealValue();
        if (uidMatchingRule != null) {
            identifierValue = uidMatchingRule.normalize(identifierValue);
        }

        PrismPropertyDefinition<String> identifierDef = identifier.getDefinition();
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(ShadowUtil.getResourceOid(resourceShadow))
                .and().item(ItemPath.create(ShadowType.F_ATTRIBUTES, identifierDef.getItemName()), identifierDef).eq(identifierValue)
                .build();
    }

    public static void applyResourceSchema(ShadowType accountType, ResourceType resourceType) throws SchemaException {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resourceType);
        ShadowUtil.applyResourceSchema(accountType.asPrismObject(), resourceSchema);
    }

    public static void assertInMessageRecursive(Throwable e, String substring) {
        assert hasInMessageRecursive(e, substring) : "The substring '" + substring + "' was NOT found in the message of exception " + e + " (including cause exceptions)";
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

    @UnusedTestElement
    public static void assertNotInMessageRecursive(Throwable e, String substring) {
        assert !e.getMessage().contains(substring) : "The substring '" + substring + "' was found in the message of exception " + e + ": " + e.getMessage();
        if (e.getCause() != null) {
            assertNotInMessageRecursive(e.getCause(), substring);
        }
    }

    public static void assertNoRepoThreadLocalCache() {
        if (LocalRepoCacheCollection.exists()) {
            AssertJUnit.fail("Cache exists! " + LocalRepoCacheCollection.debugDump());
        }
    }

    public static void assertScripts(List<ScriptHistoryEntry> scriptsHistory, ProvisioningScriptSpec... expectedScripts) {
        displayScripts(scriptsHistory);
        assertEquals("Wrong number of scripts executed", expectedScripts.length, scriptsHistory.size());
        Iterator<ScriptHistoryEntry> historyIter = scriptsHistory.iterator();
        for (ProvisioningScriptSpec expecedScript : expectedScripts) {
            ScriptHistoryEntry actualScript = historyIter.next();
            assertEquals("Wrong script code", expecedScript.getCode(), actualScript.getCode());
            if (expecedScript.getLanguage() == null) {
                assertEquals("We talk only gibberish here", "Gibberish", actualScript.getLanguage());
            } else {
                assertEquals("Wrong script language", expecedScript.getLanguage(), actualScript.getLanguage());
            }
            assertEquals("Wrong number of arguments", expecedScript.getArgs().size(), actualScript.getParams().size());
            for (java.util.Map.Entry<String, Object> expectedEntry : expecedScript.getArgs().entrySet()) {
                Object expectedValue = expectedEntry.getValue();
                Object actualVal = actualScript.getParams().get(expectedEntry.getKey());
                assertEquals("Wrong value for argument '" + expectedEntry.getKey() + "'", expectedValue, actualVal);
            }
        }
    }

    public static void displayScripts(List<ScriptHistoryEntry> scriptsHistory) {
        for (ScriptHistoryEntry script : scriptsHistory) {
            display("Script", script);
        }
    }

    public static <T> void assertExtensionProperty(
            PrismObject<? extends ObjectType> object, QName propertyName, T... expectedValues) {
        PrismContainer<?> extension = object.getExtension();
        PrismAsserts.assertPropertyValue(extension, ItemName.fromQName(propertyName), expectedValues);
    }

    public static void assertNoExtensionProperty(
            PrismObject<? extends ObjectType> object, QName propertyName) {
        PrismContainer<?> extension = object.getExtension();
        PrismAsserts.assertNoItem(extension, ItemName.fromQName(propertyName));
    }

    public static void assertIcfResourceSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType)
            throws SchemaException {
        assertNotNull("No resource schema in " + resourceType, resourceSchema);
        ResourceObjectClassDefinition accountDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        assertNotNull("No object class definition for " + RI_ACCOUNT_OBJECT_CLASS + " in resource schema", accountDefinition);

        assertNotNull("No object class definition " + RI_ACCOUNT_OBJECT_CLASS, accountDefinition);
        assertTrue("Object class " + RI_ACCOUNT_OBJECT_CLASS + " is not default account", accountDefinition.isDefaultAccountDefinition());
        assertFalse("Object class " + RI_ACCOUNT_OBJECT_CLASS + " is empty", accountDefinition.isEmpty());
        assertFalse("Object class " + RI_ACCOUNT_OBJECT_CLASS + " is empty", accountDefinition.isIgnored());

        Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getPrimaryIdentifiers();
        assertNotNull("Null identifiers for " + RI_ACCOUNT_OBJECT_CLASS, identifiers);
        assertFalse("Empty identifiers for " + RI_ACCOUNT_OBJECT_CLASS, identifiers.isEmpty());

        ResourceAttributeDefinition uidAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaConstants.ICFS_UID);
        assertNotNull("No definition for attribute " + SchemaConstants.ICFS_UID, uidAttributeDefinition);
        assertTrue("Attribute " + SchemaConstants.ICFS_UID + " in not an identifier",
                accountDefinition.isPrimaryIdentifier(uidAttributeDefinition.getItemName()));
        assertTrue("Attribute " + SchemaConstants.ICFS_UID + " in not in identifiers list", identifiers.contains(uidAttributeDefinition));
        assertEquals("Wrong displayName for attribute " + SchemaConstants.ICFS_UID, "ConnId UID", uidAttributeDefinition.getDisplayName());
        assertEquals("Wrong displayOrder for attribute " + SchemaConstants.ICFS_UID, (Integer) 100, uidAttributeDefinition.getDisplayOrder());

        Collection<? extends ResourceAttributeDefinition> secondaryIdentifiers = accountDefinition.getSecondaryIdentifiers();
        assertNotNull("Null secondary identifiers for " + RI_ACCOUNT_OBJECT_CLASS, secondaryIdentifiers);
        assertFalse("Empty secondary identifiers for " + RI_ACCOUNT_OBJECT_CLASS, secondaryIdentifiers.isEmpty());

        ResourceAttributeDefinition nameAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertNotNull("No definition for attribute " + SchemaConstants.ICFS_NAME, nameAttributeDefinition);
        assertTrue("Attribute " + SchemaConstants.ICFS_NAME + " in not an identifier",
                accountDefinition.isSecondaryIdentifier(
                        nameAttributeDefinition.getItemName()));
        assertTrue("Attribute " + SchemaConstants.ICFS_NAME + " in not in identifiers list", secondaryIdentifiers.contains(nameAttributeDefinition));
        assertEquals("Wrong displayName for attribute " + SchemaConstants.ICFS_NAME, "ConnId Name", nameAttributeDefinition.getDisplayName());
        assertEquals("Wrong displayOrder for attribute " + SchemaConstants.ICFS_NAME, (Integer) 110, nameAttributeDefinition.getDisplayOrder());

        assertNotNull("Null identifiers in account", accountDefinition.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDefinition.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountDefinition.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDefinition.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDefinition.getNamingAttribute());
        assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDefinition.getNativeObjectClass()));

        ResourceAttributeDefinition uidDef = accountDefinition
                .findAttributeDefinition(SchemaConstants.ICFS_UID);
        assertEquals(1, uidDef.getMaxOccurs());
        assertEquals(0, uidDef.getMinOccurs());
        assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
        assertFalse("UID has create", uidDef.canAdd());
        assertFalse("UID has update", uidDef.canModify());
        assertTrue("No UID read", uidDef.canRead());
        assertTrue("UID definition not in identifiers", accountDefinition.getPrimaryIdentifiers().contains(uidDef));
        assertEquals("Wrong refined displayName for attribute " + SchemaConstants.ICFS_UID, "ConnId UID", uidDef.getDisplayName());
        assertEquals("Wrong refined displayOrder for attribute " + SchemaConstants.ICFS_UID, (Integer) 100, uidDef.getDisplayOrder());

        ResourceAttributeDefinition nameDef = accountDefinition
                .findAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertEquals(1, nameDef.getMaxOccurs());
        assertEquals(1, nameDef.getMinOccurs());
        assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
        assertTrue("No NAME create", nameDef.canAdd());
        assertTrue("No NAME update", nameDef.canModify());
        assertTrue("No NAME read", nameDef.canRead());
        assertTrue("NAME definition not in identifiers", accountDefinition.getSecondaryIdentifiers().contains(nameDef));
        assertEquals("Wrong refined displayName for attribute " + SchemaConstants.ICFS_NAME, "ConnId Name", nameDef.getDisplayName());
        assertEquals("Wrong refined displayOrder for attribute " + SchemaConstants.ICFS_NAME, (Integer) 110, nameDef.getDisplayOrder());

        assertNull("The _PASSWORD_ attribute sneaked into schema", accountDefinition.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS, "password")));
    }

    //TODO: add language parameter..for now, use xml serialization
    public static void displayXml(String message, PrismObject<? extends ObjectType> object) throws SchemaException {
        String xml = PrismTestUtil.serializeToXml(object.asObjectable());
        display(message, xml);
    }

    public static ObjectDelta<ShadowType> createEntitleDelta(
            String accountOid, QName associationName, String groupOid) throws SchemaException {
        ShadowAssociationType association = new ShadowAssociationType();
        association.setName(associationName);
        ObjectReferenceType shadowRefType = new ObjectReferenceType();
        shadowRefType.setOid(groupOid);
        shadowRefType.setType(ShadowType.COMPLEX_TYPE);
        association.setShadowRef(shadowRefType);
        return PrismContext.get().deltaFactory().object().createModificationAddContainer(
                ShadowType.class, accountOid, ShadowType.F_ASSOCIATION, association);
    }

    public static ObjectDelta<ShadowType> createDetitleDelta(
            String accountOid, QName associationName, String groupOid) throws SchemaException {
        ShadowAssociationType association = new ShadowAssociationType();
        association.setName(associationName);
        ObjectReferenceType shadowRefType = new ObjectReferenceType();
        shadowRefType.setOid(groupOid);
        shadowRefType.setType(ShadowType.COMPLEX_TYPE);
        association.setShadowRef(shadowRefType);
        return PrismContext.get().deltaFactory().object().createModificationDeleteContainer(
                ShadowType.class, accountOid, ShadowType.F_ASSOCIATION, association);
    }

    public static ObjectDelta<ShadowType> createEntitleDeltaIdentifiers(String accountOid, QName associationName, QName identifierQname, String identifierValue) throws SchemaException {
        ShadowAssociationType association = new ShadowAssociationType();
        association.setName(associationName);
        ObjectDelta<ShadowType> delta = PrismContext.get().deltaFactory().object().createModificationAddContainer(ShadowType.class,
                accountOid, ShadowType.F_ASSOCIATION, association);
        PrismContainer<ShadowIdentifiersType> identifiersContainer = association.asPrismContainerValue().findOrCreateContainer(ShadowAssociationType.F_IDENTIFIERS);
        PrismContainerValue<ShadowIdentifiersType> identifiersContainerValue = identifiersContainer.createNewValue();
        PrismProperty<String> identifier = PrismContext.get().itemFactory().createProperty(identifierQname);
        identifier.addRealValue(identifierValue);
        identifiersContainerValue.add(identifier);
        return delta;
    }

    public static ObjectDelta<ShadowType> createDetitleDeltaIdentifiers(String accountOid, QName associationName, QName identifierQname, String identifierValue) throws SchemaException {
        ShadowAssociationType association = new ShadowAssociationType();
        association.setName(associationName);
        ObjectDelta<ShadowType> delta = PrismContext.get().deltaFactory().object().createModificationDeleteContainer(ShadowType.class,
                accountOid, ShadowType.F_ASSOCIATION, association);
        PrismContainer<ShadowIdentifiersType> identifiersContainer = association.asPrismContainerValue().findOrCreateContainer(ShadowAssociationType.F_IDENTIFIERS);
        PrismContainerValue<ShadowIdentifiersType> identifiersContainerValue = identifiersContainer.createNewValue();
        PrismProperty<String> identifier = PrismContext.get().itemFactory().createProperty(identifierQname);
        identifier.addRealValue(identifierValue);
        identifiersContainerValue.add(identifier);
        return delta;
    }

    public static void assertGroupMember(DummyGroup group, String accountId) {
        assertGroupMember(group, accountId, false);
    }

    public static void assertGroupMember(DummyGroup group, String accountId, boolean caseIgnore) {
        Collection<String> members = group.getMembers();
        assertNotNull("No members in group " + group.getName() + ", expected that " + accountId + " will be there", members);
        if (caseIgnore) {
            for (String member : members) {
                if (StringUtils.equalsIgnoreCase(accountId, member)) {
                    return;
                }
            }
            AssertJUnit.fail("Account " + accountId + " is not member of group " + group.getName() + ", members: " + members);
        } else {
            assertTrue("Account " + accountId + " is not member of group " + group.getName() + ", members: " + members, members.contains(accountId));
        }
    }

    public static void assertNoGroupMember(DummyGroup group, String accountId) {
        Collection<String> members = group.getMembers();
        if (members == null) {
            return;
        }
        assertFalse("Account " + accountId + " IS member of group " + group.getName() + " while not expecting it, members: " + members, members.contains(accountId));
    }

    public static void assertNoGroupMembers(DummyGroup group) {
        Collection<String> members = group.getMembers();
        assertTrue("Group " + group.getName() + " has members while not expecting it, members: " + members, members == null || members.isEmpty());
    }

    public static ShadowAssociationType assertAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
        ShadowType accountType = shadow.asObjectable();
        List<ShadowAssociationType> associations = accountType.getAssociation();
        assertNotNull("Null associations in " + shadow, associations);
        assertFalse("Empty associations in " + shadow, associations.isEmpty());
        for (ShadowAssociationType association : associations) {
            if (associationName.equals(association.getName()) &&
                    association.getShadowRef() != null &&
                    entitlementOid.equals(association.getShadowRef().getOid())) {
                return association;
            }
        }
        AssertJUnit.fail("No association for entitlement " + entitlementOid + " in " + shadow);
        throw new IllegalStateException("not reached");
    }

    public static void assertNoAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
        ShadowType accountType = shadow.asObjectable();
        List<ShadowAssociationType> associations = accountType.getAssociation();
        if (associations == null) {
            return;
        }
        for (ShadowAssociationType association : associations) {
            if (associationName.equals(association.getName()) &&
                    entitlementOid.equals(association.getShadowRef().getOid())) {
                AssertJUnit.fail("Unexpected association for entitlement " + entitlementOid + " in " + shadow);
            }
        }
    }

    public static void assertNoSchema(ResourceType resourceType) {
        assertNoSchema("Found schema in resource " + resourceType + " while not expecting it", resourceType);
    }

    public static void assertNoSchema(String message, ResourceType resourceType) {
        Element resourceXsdSchema = ResourceTypeUtil.getResourceXsdSchema(resourceType);
        AssertJUnit.assertNull(message, resourceXsdSchema);
    }

    public static void assertConnectorSanity(ConnectorType conn) throws SchemaException {
        assertNotNull("Connector name is missing in "+conn, conn.getName());
        assertNotNull("Connector framework is missing in "+conn, conn.getFramework());
        assertNotNull("Connector type is missing in "+conn, conn.getConnectorType());
        assertNotNull("Connector version is missing in "+conn, conn.getConnectorVersion());
        assertNotNull("Connector bundle is missing in "+conn, conn.getConnectorBundle());
        assertNotNull("Connector namespace is missing in "+conn, conn.getNamespace());
    }

    public static void assertConnectorSchemaSanity(ConnectorType conn, PrismContext prismContext) throws SchemaException {
        XmlSchemaType xmlSchemaType = conn.getSchema();
        assertNotNull("xmlSchemaType is null", xmlSchemaType);
        Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
        assertNotNull("No schema", connectorXsdSchemaElement);
        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);
        assertNotNull("No xsd:schema element in xmlSchemaType", xsdElement);
        display("XSD schema of " + conn, DOMUtil.serializeDOMToString(xsdElement));
        // Try to parse the schema
        PrismSchema schema;
        try {
            schema = PrismSchemaImpl.parse(xsdElement, true, "schema of " + conn, prismContext);
        } catch (SchemaException e) {
            throw new SchemaException("Error parsing schema of " + conn + ": " + e.getMessage(), e);
        }
        assertConnectorSchemaSanity(schema, conn.toString(), SchemaConstants.ICF_FRAMEWORK_URI.equals(conn.getFramework()));
    }

    public static void assertConnectorSchemaSanity(PrismSchema schema, String connectorDescription, boolean expectConnIdSchema) {
        assertNotNull("Cannot parse connector schema of " + connectorDescription, schema);
        assertFalse("Empty connector schema in " + connectorDescription, schema.isEmpty());
        PrismTestUtil.display("Parsed connector schema of " + connectorDescription, schema);

        // Local schema namespace is used here.
        PrismContainerDefinition configurationDefinition =
                schema.findItemDefinitionByElementName(new QName(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart()),
                        PrismContainerDefinition.class);
        assertNotNull("Definition of <configuration> property container not found in connector schema of " + connectorDescription,
                configurationDefinition);
        assertFalse("Empty definition of <configuration> property container in connector schema of " + connectorDescription,
                configurationDefinition.isEmpty());

        if (expectConnIdSchema) {
            // ICFC schema is used on other elements
            PrismContainerDefinition configurationPropertiesDefinition =
                    configurationDefinition.findContainerDefinition(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
            assertNotNull("Definition of <configurationProperties> property container not found in connector schema of " + connectorDescription,
                    configurationPropertiesDefinition);
            assertFalse("Empty definition of <configurationProperties> property container in connector schema of " + connectorDescription,
                    configurationPropertiesDefinition.isEmpty());
            assertFalse("No definitions in <configurationProperties> in " + connectorDescription, configurationPropertiesDefinition.getDefinitions().isEmpty());

            // TODO: other elements
        }
    }

    public static void assertProtectedString(String message, String expectedClearValue, ProtectedStringType actualValue, CredentialsStorageTypeType storageType, Protector protector) throws EncryptionException, SchemaException {
        switch (storageType) {

            case NONE:
                assertNull(message + ": unexpected value: " + actualValue, actualValue);
                break;

            case ENCRYPTION:
                assertNotNull(message + ": no value", actualValue);
                assertTrue(message + ": unencrypted value: " + actualValue, actualValue.isEncrypted());
                String actualClearPassword = protector.decryptString(actualValue);
                assertEquals(message + ": wrong value", expectedClearValue, actualClearPassword);
                assertFalse(message + ": unexpected hashed value: " + actualValue, actualValue.isHashed());
                assertNull(message + ": unexpected clear value: " + actualValue, actualValue.getClearValue());
                break;

            case HASHING:
                assertNotNull(message + ": no value", actualValue);
                assertTrue(message + ": value not hashed: " + actualValue, actualValue.isHashed());
                ProtectedStringType expectedPs = new ProtectedStringType();
                expectedPs.setClearValue(expectedClearValue);
                assertTrue(message + ": hash does not match, expected " + expectedClearValue + ", but was " + actualValue,
                        protector.compareCleartext(actualValue, expectedPs));
                assertFalse(message + ": unexpected encrypted value: " + actualValue, actualValue.isEncrypted());
                assertNull(message + ": unexpected clear value: " + actualValue, actualValue.getClearValue());
                break;

            default:
                throw new IllegalArgumentException("Unknown storage " + storageType);
        }
    }

    public static void assertHasProtectedString(String message,
            ProtectedStringType actualValue, CredentialsStorageTypeType storageType, Protector protector)
            throws EncryptionException {
        switch (storageType) {

            case NONE:
                assertNull(message + ": unexpected value: " + actualValue, actualValue);
                break;

            case ENCRYPTION:
                assertNotNull(message + ": no value", actualValue);
                assertTrue(message + ": unencrypted value: " + actualValue, actualValue.isEncrypted());
                protector.decryptString(actualValue);           // just checking it can be decrypted
                assertFalse(message + ": unexpected hashed value: " + actualValue, actualValue.isHashed());
                assertNull(message + ": unexpected clear value: " + actualValue, actualValue.getClearValue());
                break;

            case HASHING:
                assertNotNull(message + ": no value", actualValue);
                assertTrue(message + ": value not hashed: " + actualValue, actualValue.isHashed());
                assertFalse(message + ": unexpected encrypted value: " + actualValue, actualValue.isEncrypted());
                assertNull(message + ": unexpected clear value: " + actualValue, actualValue.getClearValue());
                break;

            default:
                throw new IllegalArgumentException("Unknown storage " + storageType);
        }
    }

    @UnusedTestElement
    public static boolean isSilentConsole() {
        return silentConsole;
    }

    public static void setSilentConsole(boolean silentConsole) {
        IntegrationTestTools.silentConsole = silentConsole;
    }

    static void println(String s) {
        if (!silentConsole) {
            System.out.println(s);
        }
    }
}
