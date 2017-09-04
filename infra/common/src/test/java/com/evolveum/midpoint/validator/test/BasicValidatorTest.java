/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.validator.test;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Test few basic cases of validation.
 *
 * @author Radovan Semancik
 */
public class BasicValidatorTest {

    public static final String BASE_PATH = "src/test/resources/validator/";
	private static final String OBJECT_RESULT_OPERATION_NAME = BasicValidatorTest.class.getName() + ".validateObject";

    public BasicValidatorTest() {
    }

    @BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void resource1Valid() throws Exception {
    	System.out.println("\n===[ resource1Valid ]=====");

    	OperationResult result = new OperationResult(this.getClass().getName()+".resource1Valid");

        EventHandler handler = new EventHandler() {

			@Override
			public EventResult preMarshall(Element objectElement, Node postValidationTree,
					OperationResult objectResult) {
				return EventResult.cont();
			}

			@Override
			public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
					OperationResult objectResult) {
				System.out.println("Validating resorce:");
				System.out.println(object.debugDump());
				object.checkConsistence();

				PrismContainer<?> extensionContainer = object.getExtension();
				PrismProperty<Integer> menProp = extensionContainer.findProperty(new QName("http://myself.me/schemas/whatever","menOnChest"));
				assertNotNull("No men on a dead man chest!", menProp);
				assertEquals("Wrong number of men on a dead man chest", (Integer)15, menProp.getAnyRealValue());
				PrismPropertyDefinition menPropDef = menProp.getDefinition();
				assertNotNull("Men on a dead man chest NOT defined", menPropDef);
				assertEquals("Wrong type for men on a dead man chest definition", DOMUtil.XSD_INT, menPropDef.getTypeName());
				assertTrue("Men on a dead man chest definition not dynamic", menPropDef.isDynamic());

				return EventResult.cont();
			}

			@Override
			public void handleGlobalError(OperationResult currentResult) { /* nothing */ }
		};

		validateFile("resource-1-valid.xml", handler, result);
        AssertJUnit.assertTrue(result.isSuccess());

    }

    @Test
    public void handlerTest() throws Exception {
    	System.out.println("\n===[ handlerTest ]=====");

    	OperationResult result = new OperationResult(this.getClass().getName()+".handlerTest");

        final List<String> postMarshallHandledOids = new ArrayList<String>();
        final List<String> preMarshallHandledOids = new ArrayList<String>();

        EventHandler handler = new EventHandler() {

			@Override
			public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
				preMarshallHandledOids.add(objectElement.getAttribute("oid"));
				return EventResult.cont();
			}

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult) {
            	System.out.println("Handler processing " + object + ", result:");
				System.out.println(objectResult.debugDump());
                postMarshallHandledOids.add(object.getOid());
                return EventResult.cont();
            }

			@Override
			public void handleGlobalError(OperationResult currentResult) {
				System.out.println("Handler got global error:");
				System.out.println(currentResult.debugDump());
			}

        };

        validateFile("three-objects.xml",handler,result);

        System.out.println(result.debugDump());
        AssertJUnit.assertTrue("Result is not success", result.isSuccess());
        AssertJUnit.assertTrue(postMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111111"));
        AssertJUnit.assertTrue(preMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111111"));
        AssertJUnit.assertTrue(postMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111112"));
        AssertJUnit.assertTrue(preMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111112"));
        AssertJUnit.assertTrue(postMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111113"));
        AssertJUnit.assertTrue(preMarshallHandledOids.contains("c0c010c0-d34d-b33f-f00d-111111111113"));
    }

    @Test
    public void notWellFormed() throws Exception {
    	System.out.println("\n===[ notWellFormed ]=====");

    	OperationResult result = new OperationResult(this.getClass().getName()+".notWellFormed");

        validateFile("not-well-formed.xml",result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        AssertJUnit.assertTrue(result.getMessage().contains("Unexpected close tag"));
        // Check if line number is in the error
        AssertJUnit.assertTrue("Line number not found in error message", result.getMessage().contains("35"));

    }

    @Test
    public void undeclaredPrefix() throws Exception {
    	System.out.println("\n===[ undeclaredPrefix ]=====");

        OperationResult result = new OperationResult(this.getClass().getName()+".undeclaredPrefix");

        validateFile("undeclared-prefix.xml",result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        AssertJUnit.assertTrue(result.getMessage().contains("Undeclared namespace prefix"));
        // Check if line number is in the error
        AssertJUnit.assertTrue("Line number not found in error message", result.getMessage().contains("37"));

    }

    @Test
    public void schemaViolation() throws Exception {
    	System.out.println("\n===[ schemaViolation ]=====");

        OperationResult result = new OperationResult(this.getClass().getName()+".schemaViolation");

        validateFile("three-users-schema-violation.xml",result);

        System.out.println(result.debugDump());
        assertFalse(result.isSuccess());
        assertTrue(result.getSubresults().get(0).getMessage().contains("Invalid content was found starting with element 'foo'"));
        assertTrue(result.getSubresults().get(1).getMessage().contains("Invalid content was found starting with element 'givenName'"));
        assertTrue(result.getSubresults().get(2).getMessage().contains("Invalid content was found starting with element 'fullName'"));
    }

    /**
     * Same data as schemaViolation test, but this will set s lower threshold to stop after just two erros.
     */
    @Test
    public void testStopOnErrors() throws Exception {
    	System.out.println("\n===[ testStopOnErrors ]=====");

        OperationResult result = new OperationResult(this.getClass().getName()+".testStopOnErrors");

        Validator validator = new Validator(PrismTestUtil.getPrismContext());
        validator.setVerbose(false);
        validator.setStopAfterErrors(2);

        validateFile("three-users-schema-violation.xml", null, validator, result);

        System.out.println(result.debugDump());
        assertFalse(result.isSuccess());
        assertEquals(2,result.getSubresults().size());
    }

    @Test
    public void noName() throws Exception {
    	System.out.println("\n===[ noName ]=====");

        OperationResult result = new OperationResult(this.getClass().getName()+".noName");

        validateFile("no-name.xml",result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        AssertJUnit.assertTrue(result.getSubresults().get(0).getSubresults().get(1).getMessage().contains("Null property"));
        AssertJUnit.assertTrue(result.getSubresults().get(0).getSubresults().get(1).getMessage().contains("name"));

    }

    private void validateFile(String filename, OperationResult result) throws FileNotFoundException {
        validateFile(filename,null,result);
    }

    private void validateFile(String filename,EventHandler handler, OperationResult result) throws FileNotFoundException {
        Validator validator = new Validator(PrismTestUtil.getPrismContext());
        if (handler!=null) {
            validator.setHandler(handler);
        }
        validator.setVerbose(false);
        validateFile(filename, handler, validator, result);
    }

    private void validateFile(String filename,EventHandler handler, Validator validator, OperationResult result) throws FileNotFoundException {

        String filepath = BASE_PATH + filename;

        System.out.println("Validating " + filename);

        FileInputStream fis = null;

        File file = new File(filepath);
        fis = new FileInputStream(file);

		validator.validate(fis, result, OBJECT_RESULT_OPERATION_NAME);

        if (!result.isSuccess()) {
        	System.out.println("Errors:");
        	System.out.println(result.debugDump());
        } else {
            System.out.println("No errors");
            System.out.println(result.debugDump());
        }

    }

}
