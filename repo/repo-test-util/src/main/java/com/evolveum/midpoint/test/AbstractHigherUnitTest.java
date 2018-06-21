/*
 * Copyright (c) 2010-2018 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Abstract unit test for use in higher layes of the system (repo and abobe).
 * Does not initialize any parts of the system except for prism.
 * Implements some common convenience methods.
 * 
 * @author Radovan Semancik
 */
public abstract class AbstractHigherUnitTest {

	public static final String COMMON_DIR_NAME = "common";
	public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractHigherUnitTest.class);
	protected static final Random RND = new Random();
	
	@BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
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

	protected static <T> T unmarshallValueFromFile(File file, Class<T> clazz)
            throws IOException, JAXBException, SchemaException {
        return PrismTestUtil.parseAnyValue(file);
	}

	protected static <T> T unmarshallValueFromFile(String filePath, Class<T> clazz)
            throws IOException, JAXBException, SchemaException {
        return PrismTestUtil.parseAnyValue(new File(filePath));
	}

	protected static ObjectType unmarshallValueFromFile(String filePath) throws IOException,
            JAXBException, SchemaException {
		return unmarshallValueFromFile(filePath, ObjectType.class);
	}

	protected void assertNoChanges(ObjectDelta<?> delta) {
        assertNull("Unexpected changes: "+ delta, delta);
	}

	protected void assertNoChanges(String desc, ObjectDelta<?> delta) {
        assertNull("Unexpected changes in "+desc+": "+ delta, delta);
	}

	protected <F extends FocusType> void  assertEffectiveActivation(PrismObject<F> focus, ActivationStatusType expected) {
		ActivationType activationType = focus.asObjectable().getActivation();
		assertNotNull("No activation in "+focus, activationType);
		assertEquals("Wrong effectiveStatus in activation in "+focus, expected, activationType.getEffectiveStatus());
	}

	protected <F extends FocusType> void  assertEffectiveActivation(AssignmentType assignmentType, ActivationStatusType expected) {
		ActivationType activationType = assignmentType.getActivation();
		assertNotNull("No activation in "+assignmentType, activationType);
		assertEquals("Wrong effectiveStatus in activation in "+assignmentType, expected, activationType.getEffectiveStatus());
	}

	protected <F extends FocusType> void  assertValidityStatus(PrismObject<F> focus, TimeIntervalStatusType expected) {
		ActivationType activationType = focus.asObjectable().getActivation();
		assertNotNull("No activation in "+focus, activationType);
		assertEquals("Wrong validityStatus in activation in "+focus, expected, activationType.getValidityStatus());
	}

	protected void assertShadow(PrismObject<? extends ShadowType> shadow) {
		assertObject(shadow);
	}

	protected void assertObject(PrismObject<? extends ObjectType> object) {
		object.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
		assertTrue("Incomplete definition in "+object, object.hasCompleteDefinition());
		assertFalse("No OID", StringUtils.isEmpty(object.getOid()));
		assertNotNull("Null name in "+object, object.asObjectable().getName());
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
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" name", name, userType.getName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" fullName", fullName, userType.getFullName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" givenName", givenName, userType.getGivenName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" familyName", familyName, userType.getFamilyName());

		if (location != null) {
			PrismAsserts.assertEqualsPolyString("Wrong " + user + " location", location,
					userType.getLocality());
		}
	}

	protected <O extends ObjectType> void assertSubtype(PrismObject<O> object, String subtype) {
		assertTrue("Object "+object+" does not have subtype "+subtype, FocusTypeUtil.hasSubtype(object, subtype));
	}

	protected void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, QName objectClass) throws SchemaException {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, null, false);
	}

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null, false);
    }

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                      MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers) throws SchemaException {
        assertShadowCommon(accountShadow,oid,username,resourceType,getAccountObjectClass(resourceType),nameMatchingRule, requireNormalizedIdentfiers);
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
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());

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
			PrismProperty<String> idProp = attributesContainer.findProperty(idDef.getName());
			assertNotNull("No primary identifier ("+idDef.getName()+") attribute in shadow for "+username, idProp);
			if (nameMatchingRule == null) {
				assertEquals("Unexpected primary identifier in shadow for "+username, username, idProp.getRealValue());
			} else {
				if (requireNormalizedIdentfiers) {
					assertEquals("Unexpected primary identifier in shadow for "+username, nameMatchingRule.normalize(username), idProp.getRealValue());
				} else {
					PrismAsserts.assertEquals("Unexpected primary identifier in shadow for "+username, nameMatchingRule, username, idProp.getRealValue());
				}
			}
		} else {
			boolean found = false;
			String expected = username;
			if (requireNormalizedIdentfiers && nameMatchingRule != null) {
				expected = nameMatchingRule.normalize(username);
			}
			List<String> wasValues = new ArrayList<>();
			for (ResourceAttributeDefinition idSecDef: ocDef.getSecondaryIdentifiers()) {
				PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getName());
				wasValues.addAll(idProp.getRealValues());
				assertNotNull("No secondary identifier ("+idSecDef.getName()+") attribute in shadow for "+username, idProp);
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
				fail("Unexpected secondary identifier in shadow for "+username+", expected "+expected+" but was "+wasValues);
			}
		}
	}

    protected void assertShadowSecondaryIdentifier(PrismObject<ShadowType> shadow, String expectedIdentifier, ResourceType resourceType, MatchingRule<String> nameMatchingRule) throws SchemaException {
    	RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
    	ObjectClassComplexTypeDefinition ocDef = rSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
    	ResourceAttributeDefinition idSecDef = ocDef.getSecondaryIdentifiers().iterator().next();
    	PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getName());
		assertNotNull("No secondary identifier ("+idSecDef.getName()+") attribute in shadow for "+expectedIdentifier, idProp);
		if (nameMatchingRule == null) {
			assertEquals("Unexpected secondary identifier in shadow for "+expectedIdentifier, expectedIdentifier, idProp.getRealValue());
		} else {
			PrismAsserts.assertEquals("Unexpected secondary identifier in shadow for "+expectedIdentifier, nameMatchingRule, expectedIdentifier, idProp.getRealValue());
		}

    }

	protected void assertShadowName(PrismObject<ShadowType> shadow, String expectedName) {
		PrismAsserts.assertEqualsPolyString("Shadow name is wrong in "+shadow, expectedName, shadow.asObjectable().getName());
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

	// objectClassName may be null
	protected RefinedAttributeDefinition getAttributeDefinition(ResourceType resourceType,
																ShadowKindType kind,
																QName objectClassName,
																String attributeLocalName) throws SchemaException {
		RefinedResourceSchema refinedResourceSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
		RefinedObjectClassDefinition refinedObjectClassDefinition =
				refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClassName);
		return refinedObjectClassDefinition.findAttributeDefinition(attributeLocalName);
	}

	protected void assertFilter(ObjectFilter filter, Class<? extends ObjectFilter> expectedClass) {
		if (expectedClass == null) {
			assertNull("Expected that filter is null, but it was "+filter, filter);
		} else {
			assertNotNull("Expected that filter is of class "+expectedClass.getName()+", but it was null", filter);
			if (!(expectedClass.isAssignableFrom(filter.getClass()))) {
				AssertJUnit.fail("Expected that filter is of class "+expectedClass.getName()+", but it was "+filter);
			}
		}
	}


	protected void assertActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected activation administrative status of "+shadow+" to be "+expectedStatus+", but there was no activation administrative status");
			}
		} else {
			assertEquals("Wrong activation administrative status of "+shadow, expectedStatus, activationType.getAdministrativeStatus());
		}
	}

	protected void assertShadowLockout(PrismObject<ShadowType> shadow, LockoutStatusType expectedStatus) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected lockout status of "+shadow+" to be "+expectedStatus+", but there was no lockout status");
			}
		} else {
			assertEquals("Wrong lockout status of "+shadow, expectedStatus, activationType.getLockoutStatus());
		}
	}

	protected void assertUserLockout(PrismObject<UserType> user, LockoutStatusType expectedStatus) {
		ActivationType activationType = user.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected lockout status of "+user+" to be "+expectedStatus+", but there was no lockout status");
			}
		} else {
			assertEquals("Wrong lockout status of "+user, expectedStatus, activationType.getLockoutStatus());
		}
	}

	protected PolyString createPolyString(String string) {
		return PrismTestUtil.createPolyString(string);
	}

	protected PolyStringType createPolyStringType(String string) {
		return PrismTestUtil.createPolyStringType(string);
	}

	protected ItemPath getExtensionPath(QName propName) {
		return new ItemPath(ObjectType.F_EXTENSION, propName);
	}

	protected void assertNumberOfAttributes(PrismObject<ShadowType> shadow, Integer expectedNumberOfAttributes) {
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("No attributes in repo shadow "+shadow, attributesContainer);
		List<Item<?,?>> attributes = attributesContainer.getValue().getItems();

		assertFalse("Empty attributes in repo shadow "+shadow, attributes.isEmpty());
		if (expectedNumberOfAttributes != null) {
			assertEquals("Unexpected number of attributes in repo shadow "+shadow, (int)expectedNumberOfAttributes, attributes.size());
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

	protected void displayTestTitle(String testName) {
		TestUtil.displayTestTitle(this, testName);
	}
	
	protected void displayWhen(String testName) {
		TestUtil.displayWhen(testName);
	}

	protected void displayThen(String testName) {
		TestUtil.displayThen(testName);
	}

	protected void displayCleanup(String testName) {
		TestUtil.displayCleanup(testName);
	}

	protected void displaySkip(String testName) {
		TestUtil.displaySkip(testName);
	}

	protected void display(String str) {
		IntegrationTestTools.display(str);
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

	public static void display(String title, DebugDumpable dumpable) {
		IntegrationTestTools.display(title, dumpable);
	}

	public static void display(String title, String value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Object value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Containerable value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Throwable e) {
		IntegrationTestTools.display(title, e);
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

	protected void assertTestResourceSuccess(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceSuccess(testResult, operation);
	}

	protected void assertTestResourceFailure(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceFailure(testResult, operation);
	}

	protected void assertTestResourceNotApplicable(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceNotApplicable(testResult, operation);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname,
			T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}

	protected <T> void assertAttribute(ResourceType resourceType, ShadowType shadowType, String attrName,
			T... expectedValues) {
		assertAttribute(resourceType.asPrismObject(), shadowType, attrName, expectedValues);
	}

	protected <T> void assertAttribute(ResourceType resourceType, ShadowType shadowType, QName attrName,
			T... expectedValues) {
		assertAttribute(resourceType.asPrismObject(), shadowType, attrName, expectedValues);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName,
			T... expectedValues) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertAttribute(resource, shadow, attrQname, expectedValues);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, MatchingRule<T> matchingRule,
			QName attrQname, T... expectedValues) throws SchemaException {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, matchingRule, actualValues, expectedValues);
	}

	protected void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname) {
		PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			return;
		}
		PrismProperty attribute = attributesContainer.findProperty(attrQname);
		assertNull("Unexpected attribute "+attrQname+" in "+shadow+": "+attribute, attribute);
	}

	protected void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertNoAttribute(resource, shadow, attrQname);
	}

	protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int expectedNumLinks) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			assert expectedNumLinks == 0 : "Expected "+expectedNumLinks+" but "+focus+" has no linkRef";
			return;
		}
		assertEquals("Wrong number of links in " + focus, expectedNumLinks, linkRef.size());
	}

    protected <O extends ObjectType> void assertObjectOids(String message, Collection<PrismObject<O>> objects, String... oids) {
    	List<String> objectOids = objects.stream().map( o -> o.getOid()).collect(Collectors.toList());
    	PrismAsserts.assertEqualsCollectionUnordered(message, objectOids, oids);
    }

	protected S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass) {
		return QueryBuilder.queryFor(queryClass, getPrismContext());
	}

	protected void assertMessageContains(String message, String string) {
		assert message.contains(string) : "Expected message to contain '"+string+"' but it does not; message: " + message;
	}

	protected void assertExceptionUserFriendly(CommonException e, String expectedMessage) {
		LocalizableMessage userFriendlyMessage = e.getUserFriendlyMessage();
		assertNotNull("No user friendly exception message", userFriendlyMessage);
		assertEquals("Unexpected user friendly exception fallback message", expectedMessage, userFriendlyMessage.getFallbackMessage());
	}

}
