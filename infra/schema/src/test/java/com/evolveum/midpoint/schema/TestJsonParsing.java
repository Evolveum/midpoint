package com.evolveum.midpoint.schema;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.json.PrismJasonProcessorDeprecated;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
//import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Deprecated
public class TestJsonParsing {

	// private PrismJasonProcessorDeprecated jsonProcessor;

	
	@Test(enabled = false)
	public void test000SerializeUserJson() throws Exception {
		System.out.println("===[ testParseUserJson ]===");
//		PrismContext prismContext = PrismInternalTestUtil.constructInitializedPrismContext();
		PrismJasonProcessorDeprecated jsonProcessor = new PrismJasonProcessorDeprecated();
		PrismContext prismContext = MidPointPrismContextFactory.FACTORY
				.createInitializedPrismContext();
//		prismContext.initialize();
		jsonProcessor.setPrismContext(prismContext);
		jsonProcessor.setSchemaRegistry(prismContext.getSchemaRegistry());
		// PrismTestUtil.setFactory(MidPointPrismContextFactory.FACTORY);
		// PrismContext prismContext = PrismTestUtil.createPrismContext();
		// jsonProcessor.setPrismContext(prismContext);
		
		PrismObject<UserType> userType = prismContext.parseObject(new File("src/test/resources/common/user-jack.xml"));
		
		System.out.println("parsed");
		
		FileOutputStream fos = new FileOutputStream(new File("D:/file"+userType.asObjectable().getName()+"3.json"));
		try{
		jsonProcessor.serializeToJson(userType, fos);
		}catch (Exception ex){
			System.out.println("exception: " + ex);
			throw ex;
		}
		
		
		fos.close();
//		PrismObject<UserType> user = userType.asPrismObject();
//		System.out.println("object");
//		System.out.println(userType.dump());
//		assertUserPrism(userType);
	}
	
	@Test(enabled = false)
	public void test001ParseUserJson() throws Exception {
		System.out.println("===[ testParseUserJson ]===");
		PrismJasonProcessorDeprecated jsonProcessor = new PrismJasonProcessorDeprecated();
		PrismContext prismContext = MidPointPrismContextFactory.FACTORY
				.createInitializedPrismContext();
		prismContext.initialize();
		jsonProcessor.setPrismContext(prismContext);
		jsonProcessor.setSchemaRegistry(prismContext.getSchemaRegistry());
		// PrismTestUtil.setFactory(MidPointPrismContextFactory.FACTORY);
		// PrismContext prismContext = PrismTestUtil.createPrismContext();
		// jsonProcessor.setPrismContext(prismContext);
//		PrismObject<UserType> userType = jsonProcessor.parseObject(new File("src/test/resources/common/json/user-jack.json"),
//				UserType.class);
//		PrismObject<UserType> userType = jsonProcessor.parseObject(new File("D:/filejack.json"),
//				UserType.class);
//		
		try{
		PrismObject<UserType> userType = jsonProcessor.parseObject(new FileInputStream(new File("D:/filejack3.json")),
				UserType.class);
		
		System.out.println("parsed");

//		PrismObject<UserType> user = userType.asPrismObject();
		System.out.println("object");
		System.out.println(userType.debugDump());
	
		
//		System.out.println("DOM: " + prismContext.getPrismDomProcessor().serializeObjectToString(userType));
//		System.out.println("JAXB:" + SchemaDebugUtil.dumpJaxbObject(userType.asObjectable(), "user", prismContext));
		
		
		assertUserPrism(userType);
		} catch (Exception ex){
			System.out.println("exception " + ex);
			throw ex;
		}
	}
	
	String resourceFileName = null;
	
	@Test(enabled = false)
	public void test002ParseResourceJson() throws Exception {
		System.out.println("===[ test002ParseResourceJson ]===");
		PrismJasonProcessorDeprecated jsonProcessor = new PrismJasonProcessorDeprecated();
		PrismContext prismContext = MidPointPrismContextFactory.FACTORY
				.createInitializedPrismContext();
		jsonProcessor.setPrismContext(prismContext);
		jsonProcessor.setSchemaRegistry(prismContext.getSchemaRegistry());
		// PrismTestUtil.setFactory(MidPointPrismContextFactory.FACTORY);
		// PrismContext prismContext = PrismTestUtil.createPrismContext();
		// jsonProcessor.setPrismContext(prismContext);
//		PrismObject<UserType> userType = jsonProcessor.parseObject(
//				new File("src/test/resources/common/json/resource-opendj.json"),
//				UserType.class);
		
PrismObject<ResourceType> resourceType = prismContext.parseObject(new File("src/test/resources/common/resource-opendj.xml"));

System.out.println("=====================objectTYPE");
//resourceType.asObjectable().getSchemaHandling().getObjectType();
//System.out.println(SchemaDebugUtil.dumpJaxbObject(resourceType.asObjectable(), "resource", prismContext));
//System.out.println(prismContext.getPrismDomProcessor().serializeObjectToString(resourceType));
		
		System.out.println("parsed");
		System.out.println(resourceType.debugDump());
		resourceFileName = "D:/file"+resourceType.asObjectable().getName()+"3.json";
		FileOutputStream fos = new FileOutputStream(new File(resourceFileName));
		
		jsonProcessor.serializeToJson(resourceType, fos);
		
		
		
		fos.close();
		
		System.out.println("parsed");

//		PrismObject<UserType> user = userType.asPrismObject();
		System.out.println("object");
		System.out.println(resourceType.debugDump());
//		assertUserPrism(userType);
	}
	
	@Test(enabled = false)
	public void test003ParseResourceJson() throws Exception {
		System.out.println("===[ testParseUserJson ]===");
		PrismJasonProcessorDeprecated jsonProcessor = new PrismJasonProcessorDeprecated();
		PrismContext prismContext = MidPointPrismContextFactory.FACTORY
				.createInitializedPrismContext();
		prismContext.initialize();
		jsonProcessor.setPrismContext(prismContext);
		jsonProcessor.setSchemaRegistry(prismContext.getSchemaRegistry());
		// PrismTestUtil.setFactory(MidPointPrismContextFactory.FACTORY);
		// PrismContext prismContext = PrismTestUtil.createPrismContext();
		// jsonProcessor.setPrismContext(prismContext);
//		PrismObject<UserType> userType = jsonProcessor.parseObject(new File("src/test/resources/common/json/user-jack.json"),
//				UserType.class);
//		PrismObject<UserType> userType = jsonProcessor.parseObject(new File("D:/filejack.json"),
//				UserType.class);
//		
		try{
		PrismObject<ResourceType> resourceType = jsonProcessor.parseObject(new FileInputStream(new File("D:/fileEmbedded Test OpenDJ3.json")),
				ResourceType.class);
		
		System.out.println("parsed");

//		PrismObject<UserType> user = userType.asPrismObject();
		System.out.println("object");
		System.out.println(resourceType.debugDump());
		
		
		System.out.println("=====================objectTYPE");
//		resourceType.asObjectable().getSchemaHandling().getObjectType();
//		System.out.println(SchemaDebugUtil.dumpJaxbObject(resourceType.asObjectable(), "resource", prismContext));
//		System.out.println(prismContext.getPrismDomProcessor().serializeObjectToString(resourceType));
		} catch (Exception ex){
			System.out.println("exception " + ex);
			throw ex;
		}
		
//		assertUserPrism(resourceType);
	}
	
private void assertUserPrism(PrismObject<UserType> user) {
		
//		assertEquals("Wrong oid", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", user.getOid());
//		assertEquals("Wrong version", "42", user.getVersion());
		PrismObjectDefinition<UserType> usedDefinition = user.getDefinition();
		assertNotNull("No user definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "user"),
				UserType.COMPLEX_TYPE, UserType.class);
		assertEquals("Wrong class in user", UserType.class, user.getCompileTimeClass());
		UserType userType = user.asObjectable();
		assertNotNull("asObjectable resulted in null", userType);
		
//		assertPropertyValue(user, "name", PrismTestUtil.createPolyString("jack"));
//		assertPropertyDefinition(user, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
//		assertPropertyValue(user, "fullName", new PolyString("Jack Sparrow", "jack sparrow"));
//		assertPropertyDefinition(user, "fullName", PolyStringType.COMPLEX_TYPE, 0, 1);
//		assertPropertyValue(user, "givenName", new PolyString("Jack", "jack"));
//		assertPropertyDefinition(user, "givenName", PolyStringType.COMPLEX_TYPE, 0, 1);
//		assertPropertyValue(user, "familyName", new PolyString("Sparrow", "sparrow"));
//		assertPropertyDefinition(user, "familyName", PolyStringType.COMPLEX_TYPE, 0, 1);
	
		assertPropertyDefinition(user, "organizationalUnit", PolyStringType.COMPLEX_TYPE, 0, -1);
		assertPropertyValues(user, "organizationalUnit", 
				new PolyString("Brethren of the Coast", "brethren of the coast"),
				new PolyString("Davie Jones' Locker", "davie jones locker"));
		
//		PrismContainer extension = user.getExtension();
//		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
//		PrismContainerValue extensionValue = extension.getValue();
//		assertTrue("Extension parent", extensionValue.getParent() == extension);
//		assertNull("Extension ID", extensionValue.getId());
		
		ItemPath admStatusPath = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
		PrismProperty<ActivationStatusType> admStatusProperty1 = user.findProperty(admStatusPath);
		PrismAsserts.assertDefinition(admStatusProperty1.getDefinition(), ActivationType.F_ADMINISTRATIVE_STATUS, SchemaConstants.C_ACTIVATION_STATUS_TYPE, 0, 1);
		assertNotNull("Property "+admStatusPath+" not found", admStatusProperty1);
		PrismAsserts.assertPropertyValue(admStatusProperty1, ActivationStatusType.ENABLED);
		
//		PrismProperty validFromProperty = user.findProperty(new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_VALID_FROM));
//		assertNotNull("Property "+ActivationType.F_VALID_FROM+" not found", validFromProperty);
//		PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);
//		
//		PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
//		PrismAsserts.assertDefinition(assignmentContainer.getDefinition(), UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
//		assertEquals("Wrong number of assignment values", 1, assignmentContainer.getValues().size());
//		PrismContainerValue<AssignmentType> firstAssignmentValue = assignmentContainer.getValues().iterator().next();
//		
//		PrismContainer<Containerable> assignmentExtensionContainer = firstAssignmentValue.findContainer(AssignmentType.F_EXTENSION);
//		PrismAsserts.assertDefinition(assignmentExtensionContainer.getDefinition(), AssignmentType.F_EXTENSION, ExtensionType.COMPLEX_TYPE, 0, 1);
//		List<Item<?>> assignmentExtensionItems = assignmentExtensionContainer.getValue().getItems();
//		assertEquals("Wrong number of assignment extension items", 1, assignmentExtensionItems.size());
//		PrismProperty<String> firstAssignmentExtensionItem = (PrismProperty<String>) assignmentExtensionItems.get(0);
//		PrismAsserts.assertDefinition(firstAssignmentExtensionItem.getDefinition(), EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
//		PrismPropertyValue<String> firstValueOfFirstAssignmentExtensionItem = firstAssignmentExtensionItem.getValues().get(0);
//		assertEquals("Wrong value of "+EXTENSION_INT_TYPE_ELEMENT+" in assignment extension", 42, firstValueOfFirstAssignmentExtensionItem.getValue());
//		
		// TODO: check accountConstruction
		
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		assertEquals("Wrong number of accountRef values", 2, accountRef.getValues().size());
//		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_1_OID);
//		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_2_OID);
//		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_3_OID);
		
//		PrismReferenceValue accountRef1Val = accountRef.findValueByOid(USER_ACCOUNT_REF_2_OID);
//		assertNotNull("No object in ref1 (prism)", accountRef1Val.getObject());
//		assertNotNull("No object definition in ref1 (prism)", accountRef1Val.getObject().getDefinition());
//		assertEquals("Wrong ref1 oid (prism)", USER_ACCOUNT_REF_2_OID, accountRef1Val.getOid());
//		assertEquals("Wrong ref1 type (prism)", ShadowType.COMPLEX_TYPE, accountRef1Val.getTargetType());
		
//		PrismReferenceValue accountRef3Val = accountRef.findValueByOid(USER_ACCOUNT_REF_3_OID);
//		assertEquals("Wrong ref3 oid (prism)",  USER_ACCOUNT_REF_3_OID, accountRef3Val.getOid());
//		assertEquals("Wrong ref3 type (prism)", ShadowType.COMPLEX_TYPE, accountRef3Val.getTargetType());
//		assertEquals("Wrong ref3 description (prism)", "This is third accountRef", accountRef3Val.getDescription());
//		Element accountRef3ValFilterElement = accountRef3Val.getFilter();
//		assertFilter("ref3", accountRef3ValFilterElement);
	}

private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
		int maxOccurs) {
	QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
	PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
}

public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
	QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
	PrismAsserts.assertPropertyValue(container, propQName, propValue);
}

public static <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
	QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
	PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
}

private void assertFilter(String message, Element filterElement) {
	assertNotNull("No "+message+" filter", filterElement);
	assertEquals("Wrong "+message+" filter namespace", PrismConstants.NS_QUERY, filterElement.getNamespaceURI());
	assertEquals("Wrong "+message+" filter localName", "equal", filterElement.getLocalName());
}

}
