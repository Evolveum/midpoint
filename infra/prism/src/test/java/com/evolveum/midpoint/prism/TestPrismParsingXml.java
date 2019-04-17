package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_ADHOC_BASENAME;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_FILE_BASENAME;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_OID;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.assertPropertyValue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.assertUserJack;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.constructInitializedPrismContext;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.DOMUtil;

public class TestPrismParsingXml extends TestPrismParsing {

	public static final String USER_JACK_XXE_BASENAME = "user-jack-xxe";
	
	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Override
	protected String getOutputFormat() {
		return PrismContext.LANG_XML;
	}

	@Test
	public void testPrismParseDom() throws Exception {
		final String TEST_NAME = "testPrismParseDom";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Document document = DOMUtil.parseFile(getFile(USER_JACK_FILE_BASENAME));
		Element userElement = DOMUtil.getFirstChildElement(document);

		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserJack(user, true);
	}

	@Test
	public void testPrismParseDomAdhoc() throws Exception {
		final String TEST_NAME = "testPrismParseDomAdhoc";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Document document = DOMUtil.parseFile(getFile(USER_JACK_ADHOC_BASENAME));
		Element userElement = DOMUtil.getFirstChildElement(document);

		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserAdhoc(user, true);
	}
	
	@Test
	public void testPrismParseXxe() throws Exception {
		final String TEST_NAME = "testPrismParseXxe";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		PrismContext prismContext = constructInitializedPrismContext();
		
		try {
			// WHEN
			prismContext.parseObject(getFile(USER_JACK_XXE_BASENAME));

			AssertJUnit.fail("Unexpected success");
		} catch (IllegalStateException e) {
			// THEN
			System.out.println("Expected exception: "+e);
			assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("DOCTYPE"));
		}
			
	}
	
	@Test
	public void testPrismParseDomXxe() throws Exception {
		final String TEST_NAME = "testPrismParseDomXxe";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		try {
			// WHEN
			DOMUtil.parseFile(getFile(USER_JACK_XXE_BASENAME));

			AssertJUnit.fail("Unexpected success");
		} catch (IllegalStateException e) {
			// THEN
			System.out.println("Expected exception: "+e);
			assertTrue("Unexpected exception message: "+e.getMessage(), e.getMessage().contains("DOCTYPE"));
		}
			
	}

	@Override
	protected void validateXml(String xmlString, PrismContext prismContext) throws SAXException, IOException {
//		Document xmlDocument = DOMUtil.parseDocument(xmlString);
//		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
//		Validator validator = javaxSchema.newValidator();
//		validator.setResourceResolver(prismContext.getEntityResolver());
//		validator.validate(new DOMSource(xmlDocument));
	}
}
