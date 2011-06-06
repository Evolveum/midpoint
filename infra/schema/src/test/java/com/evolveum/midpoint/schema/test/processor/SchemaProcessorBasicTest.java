/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.evolveum.midpoint.schema.test.processor;

import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

/**
 *
 * @author semancik
 */
public class SchemaProcessorBasicTest {
	private static final String SCHEMA1_FILENAME = "src/test/resources/processor/schema1.xsd";
	
	public SchemaProcessorBasicTest() {
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}

	@Test
	public void parseSchema() throws SchemaProcessorException {
		Document schemaDom = DOMUtil.parseFile(SCHEMA1_FILENAME);
		Schema schema = Schema.parse(DOMUtil.getFirstChildElement(schemaDom));
		assertNotNull(schema);
		System.out.println("Parsed schema from "+SCHEMA1_FILENAME+":");
		System.out.println(schema.debugDump());
	}
}
