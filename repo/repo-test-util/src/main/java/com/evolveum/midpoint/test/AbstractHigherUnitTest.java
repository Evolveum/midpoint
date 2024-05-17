/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.test.util.TestUtil.getAttrQName;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Abstract unit test for use in higher layers of the system (repo and above).
 * Does not initialize any parts of the system except for prism.
 * Implements some common convenience methods.
 *
 * @author Radovan Semancik
 */
public abstract class AbstractHigherUnitTest extends AbstractUnitTest implements InfraTestMixin {

    public static final String COMMON_DIR_NAME = "common";
    public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        SchemaDebugUtil.initializePrettyPrinter();
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    protected PrismContext getPrismContext() {
        return PrismTestUtil.getPrismContext();
    }

    protected <T extends ObjectType> T parseObjectTypeFromFile(String fileName, Class<T> clazz) throws SchemaException, IOException {
        return parseObjectType(new File(fileName), clazz);
    }

    protected <T extends ObjectType> T parseObjectType(File file) throws SchemaException, IOException {
        PrismObject<T> prismObject = getPrismContext().parseObject(file);
        return prismObject.asObjectable();
    }

    protected <T extends ObjectType> T parseObjectType(File file, Class<T> clazz) throws SchemaException, IOException {
        PrismObject<T> prismObject = getPrismContext().parseObject(file);
        return prismObject.asObjectable();
    }

    protected static <T> T unmarshallValueFromFile(File file, Class<T> clazz) throws IOException, SchemaException {
        return PrismTestUtil.parseAnyValue(file);
    }

    protected static <T> T unmarshallValueFromFile(String filePath, Class<T> clazz) throws IOException, SchemaException {
        return PrismTestUtil.parseAnyValue(new File(filePath));
    }

    protected static ObjectType unmarshallValueFromFile(String filePath) throws IOException, SchemaException {
        return unmarshallValueFromFile(filePath, ObjectType.class);
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

    protected void assertEffectiveActivation(AssignmentType assignmentType, ActivationStatusType expected) {
        ActivationType activationType = assignmentType.getActivation();
        assertNotNull("No activation in " + assignmentType, activationType);
        assertEquals("Wrong effectiveStatus in activation in " + assignmentType, expected, activationType.getEffectiveStatus());
    }

    protected <F extends FocusType> void assertValidityStatus(PrismObject<F> focus, TimeIntervalStatusType expected) {
        ActivationType activationType = focus.asObjectable().getActivation();
        assertNotNull("No activation in " + focus, activationType);
        assertEquals("Wrong validityStatus in activation in " + focus, expected, activationType.getValidityStatus());
    }

    protected void assertShadow(PrismObject<? extends ShadowType> shadow) {
        assertObject(shadow);
    }

    protected void assertObject(PrismObject<? extends ObjectType> object) {
        object.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
        assertTrue("Incomplete definition in " + object, object.hasCompleteDefinition());
        assertFalse("No OID", StringUtils.isEmpty(object.getOid()));
        assertNotNull("Null name in " + object, object.asObjectable().getName());
    }

    protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName) {
        assertUser(user, oid, name, fullName, givenName, familyName, null);
    }

    protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName, String location) {
        assertObject(user);
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

    protected void assertShadowCommon(
            PrismObject<ShadowType> shadow,
            String oid,
            String username,
            ResourceType resourceType,
            QName objectClass) throws SchemaException, ConfigurationException {
        assertShadow(shadow);
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

        PrismAsserts.assertPropertyValue(shadow, ShadowType.F_NAME, PrismTestUtil.createPolyString(username));

        ResourceSchema rSchema = ResourceSchemaFactory.getCompleteSchema(resourceType);
        ResourceObjectDefinition ocDef = rSchema.findDefinitionForObjectClass(objectClass);
        if (ocDef.getSecondaryIdentifiers().isEmpty()) {
            ShadowSimpleAttributeDefinition idDef = ocDef.getPrimaryIdentifiers().iterator().next();
            PrismProperty<String> idProp = attributesContainer.findProperty(idDef.getItemName());
            assertNotNull("No primary identifier (" + idDef.getItemName() + ") attribute in shadow for " + username, idProp);
            assertEquals("Unexpected primary identifier in shadow for " + username, username, idProp.getRealValue());
        } else {
            boolean found = false;
            String expected = username;
            List<String> wasValues = new ArrayList<>();
            for (ShadowSimpleAttributeDefinition idSecDef : ocDef.getSecondaryIdentifiers()) {
                PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getItemName());
                wasValues.addAll(idProp.getRealValues());
                assertNotNull("No secondary identifier (" + idSecDef.getItemName() + ") attribute in shadow for " + username, idProp);
                if (username.equals(idProp.getRealValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("Unexpected secondary identifier in shadow for " + username + ", expected " + expected + " but was " + wasValues);
            }
        }
    }

    protected void assertShadowSecondaryIdentifier(
            PrismObject<ShadowType> shadow,
            String expectedIdentifier,
            ResourceType resourceType,
            MatchingRule<String> nameMatchingRule) throws SchemaException, ConfigurationException {
        ResourceSchema rSchema = ResourceSchemaFactory.getCompleteSchema(resourceType);
        ResourceObjectDefinition ocDef = rSchema.findDefinitionForObjectClass(shadow.asObjectable().getObjectClass());
        ShadowSimpleAttributeDefinition idSecDef = ocDef.getSecondaryIdentifiers().iterator().next();
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

    protected <O extends ObjectType> PrismObjectDefinition<O> getObjectDefinition(Class<O> type) {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
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

//    // objectClassName may be null
//    protected ResourceAttributeDefinition<?> getAttributeDefinition(ResourceType resourceType,
//            ShadowKindType kind,
//            QName objectClassName,
//            String attributeLocalName) throws SchemaException {
//        ResourceSchema refinedResourceSchema = ResourceSchemaFactory.getRefinedSchema(resourceType);
//        ResourceObjectTypeDefinition refinedObjectClassDefinition =
//                refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClassName);
//        return refinedObjectClassDefinition.findAttributeDefinition(attributeLocalName);
//    }

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
        return PrismTestUtil.createPolyString(string);
    }

    protected PolyStringType createPolyStringType(String string) {
        return PrismTestUtil.createPolyStringType(string);
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

    protected <O extends ObjectType> PrismObject<O> instantiateObject(Class<O> type) throws SchemaException {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type).instantiate();
    }

    protected <O extends ObjectType> PrismObject<O> parseObject(File file) throws SchemaException, IOException {
        return getPrismContext().parseObject(file);
    }

    protected void displayCleanup(String testName) {
        TestUtil.displayCleanup(testName);
    }

    protected void displaySkip(String testName) {
        TestUtil.displaySkip(testName);
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

    public static void display(String message, Collection collection) {
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

    public static void display(String title, Object value) {
        IntegrationTestTools.display(title, value);
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

    protected void assertSuccess(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertSuccess(result);
    }

    protected void assertSuccess(String message, OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertSuccess(message, result);
    }

    protected String assertInProgress(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertStatus(result, OperationResultStatus.IN_PROGRESS);
        return result.getAsynchronousOperationReference();
    }

    protected void assertFailure(OperationResult result) {
        if (result.isUnknown()) {
            result.computeStatus();
        }
        TestUtil.assertFailure(result);
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

    protected void assertTestResourceSuccess(OperationResult testResult, TestResourceOpNames operation) {
        IntegrationTestTools.assertTestResourceSuccess(testResult, operation);
    }

    protected void assertTestResourceFailure(OperationResult testResult, TestResourceOpNames operation) {
        IntegrationTestTools.assertTestResourceFailure(testResult, operation);
    }

    protected void assertTestResourceNotApplicable(OperationResult testResult, TestResourceOpNames operation) {
        IntegrationTestTools.assertTestResourceNotApplicable(testResult, operation);
    }

    @SafeVarargs
    protected final <T> void assertAttribute(ShadowType shadow, QName attrQname, T... expectedValues) {
        List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
        PrismAsserts.assertSets("attribute " + attrQname + " in " + shadow, actualValues, expectedValues);
    }

    @SafeVarargs
    protected final <T> void assertAttribute(ShadowType shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow, getAttrQName(attrName), expectedValues);
    }

    @SafeVarargs
    protected final <T> void assertAttribute(
            ShadowType shadow,
            MatchingRule<T> matchingRule,
            QName attrQname,
            T... expectedValues) throws SchemaException {
        List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
        PrismAsserts.assertSets("attribute " + attrQname + " in " + shadow, matchingRule, actualValues, expectedValues);
    }

    protected void assertNoAttribute(ShadowType shadow, ItemName attrQname) {
        PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null || attributesContainer.isEmpty()) {
            return;
        }
        PrismProperty attribute = attributesContainer.findProperty(attrQname);
        assertNull("Unexpected attribute " + attrQname + " in " + shadow + ": " + attribute, attribute);
    }

    protected void assertNoAttribute(ShadowType shadow, String attrName) {
        assertNoAttribute(shadow, getAttrQName(attrName));
    }

    protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int expectedNumLinks) throws ObjectNotFoundException, SchemaException {
        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            assert expectedNumLinks == 0 : "Expected " + expectedNumLinks + " but " + focus + " has no linkRef";
            return;
        }
        assertEquals("Wrong number of links in " + focus, expectedNumLinks, linkRef.size());
    }

    protected <O extends ObjectType> void assertObjectOids(String message, Collection<PrismObject<O>> objects, String... oids) {
        List<String> objectOids = objects.stream().map(PrismObject::getOid).collect(Collectors.toList());
        PrismAsserts.assertEqualsCollectionUnordered(message, objectOids, oids);
    }

    protected S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass) {
        return getPrismContext().queryFor(queryClass);
    }

    protected void assertMessageContains(String message, String string) {
        assert message.contains(string) : "Expected message to contain '" + string + "' but it does not; message: " + message;
    }

    protected void assertExceptionUserFriendly(CommonException e, String expectedMessage) {
        LocalizableMessage userFriendlyMessage = e.getUserFriendlyMessage();
        assertNotNull("No user friendly exception message", userFriendlyMessage);
        assertEquals("Unexpected user friendly exception fallback message", expectedMessage, userFriendlyMessage.getFallbackMessage());
    }

}
