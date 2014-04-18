package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.schema.TestConstants.USER_FILE;

import javax.xml.bind.JAXBElement;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

public class TestParseUserXml extends TestParseUser{

	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getLanguage() {
		return PrismContext.LANG_XML;
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}
	
	@Test
	public void testPrismParseJaxb() throws Exception {
		final String TEST_NAME = "testPrismParseJaxb";
		PrismTestUtil.displayTestTitle(TEST_NAME);
		
		// GIVEN
//		PrismTestUtil.resetPrismContext();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbUtil = JaxbTestUtil.getInstance();
		
//		prismContext.getSchemaRegistry().registerPrismSchemaFile(file);
		
		
		// WHEN
		UserType userType = jaxbUtil.unmarshalObject(USER_FILE, UserType.class);
		
		System.out.println("Parsed user (before adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		prismContext.adopt(userType);
		
		// THEN
		System.out.println("Parsed user (after adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		assertUser(userType.asPrismObject());
		
		userType.asPrismObject().checkConsistence(true, true);
	}
	
	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 */
	@Test
	public void testPrismParseJaxbObjectType() throws Exception {
		final String TEST_NAME = "testPrismParseJaxbObjectType";
		PrismTestUtil.displayTestTitle(TEST_NAME);
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

        // WHEN
		ObjectType userType = jaxbProcessor.unmarshalObject(USER_FILE, ObjectType.class);
		
		System.out.println("Parsed user (before adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		prismContext.adopt(userType);
		
		// THEN
		System.out.println("Parsed user (after adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		assertUser(userType.asPrismObject());
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
	@Test
	public void testPrismParseJaxbElement() throws Exception {
		final String TEST_NAME = "testPrismParseJaxbElement";
		PrismTestUtil.displayTestTitle(TEST_NAME);
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

        // WHEN
		JAXBElement<UserType> jaxbElement = jaxbProcessor.unmarshalElement(USER_FILE, UserType.class);
		UserType userType = jaxbElement.getValue();
		
		System.out.println("Parsed user (before adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		prismContext.adopt(userType);
		
		// THEN
		System.out.println("Parsed user (after adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		assertUser(userType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
	@Test
	public void testPrismParseJaxbElementObjectType() throws Exception {
		final String TEST_NAME = "testPrismParseJaxbElementObjectType";
		PrismTestUtil.displayTestTitle(TEST_NAME);
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

        // WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(USER_FILE, ObjectType.class);
		ObjectType userType = jaxbElement.getValue();
		
		System.out.println("Parsed user (before adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		prismContext.adopt(userType);
		
		// THEN
		System.out.println("Parsed user (after adopt):");
		System.out.println(userType.asPrismObject().debugDump());
		
		assertUser(userType.asPrismObject());		
	}


}
