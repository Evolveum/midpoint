/**
 * 
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.util.DOMUtil;

/**
 * @author Radovan Semancik
 *
 */
public class TestSchemaRegistry {

	@Test
	public void testBasic() throws SAXException, IOException {
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		Schema midPointSchema = reg.getMidPointSchema();
		assertNotNull(midPointSchema);
		
		// Try to use the schema to validate Jack
		Document document = DOMUtil.parseFile("src/test/resources/examples/user-jack.xml");
		Validator validator = midPointSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(document),validationResult);
		System.out.println("Validation result:");
		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
	@Test
	public void testExtraSchema() throws SAXException, IOException {
		Document extraSchemaDoc = DOMUtil.parseFile("src/test/resources/schema-registry/extra-schema.xsd");
		Document dataDoc = DOMUtil.parseFile("src/test/resources/schema-registry/data.xml");

		SchemaRegistry reg = new SchemaRegistry();
		reg.registerSchema(extraSchemaDoc);
		reg.initialize();
		Schema midPointSchema = reg.getMidPointSchema();
		assertNotNull(midPointSchema);
		
		Validator validator = midPointSchema.newValidator();
		DOMResult validationResult = new DOMResult();
		validator.validate(new DOMSource(dataDoc),validationResult);
		System.out.println("Validation result:");
		System.out.println(DOMUtil.serializeDOMToString(validationResult.getNode()));
	}
	
}
