/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.validator.test;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Test few basic cases of validation.
 *
 * @author Radovan Semancik
 */
public class BasicValidatorTest extends AbstractUnitTest
        implements InfraTestMixin {

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
        OperationResult result = createOperationResult();
        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
                    OperationResult objectResult) {
                displayDumpable("Validating resource:", object);
                object.checkConsistence();

                PrismContainer<?> extensionContainer = object.getExtension();
                PrismProperty<Integer> menProp = extensionContainer.findProperty(new ItemName("http://myself.me/schemas/whatever", "menOnChest"));
                assertNotNull("No men on a dead man chest!", menProp);
                assertEquals("Wrong number of men on a dead man chest", (Integer) 15, menProp.getAnyRealValue());
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
        OperationResult result = createOperationResult();

        final List<String> postMarshallHandledOids = new ArrayList<>();
        final List<String> preMarshallHandledOids = new ArrayList<>();

        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
                preMarshallHandledOids.add(objectElement.getAttribute("oid"));
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult) {
                displayDumpable("Handler processing " + object + ", result:", objectResult);
                postMarshallHandledOids.add(object.getOid());
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
                displayDumpable("Handler got global error:", currentResult);
            }

        };

        validateFile("three-objects.xml", handler, result);

        displayDumpable("Result:", result);
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
        OperationResult result = createOperationResult();

        validateFile("not-well-formed.xml", result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        String message = result.getMessage();
        AssertJUnit.assertTrue(message.contains("Unexpected close tag"));
        // Check if line number is in the error
        AssertJUnit.assertTrue("Line number not found in error message: " + message, message.contains("26"));

    }

    @Test
    public void undeclaredPrefix() throws Exception {
        OperationResult result = createOperationResult();

        validateFile("undeclared-prefix.xml", result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        String message = result.getMessage();
        AssertJUnit.assertTrue(message.contains("Undeclared namespace prefix"));
        // Check if line number is in the error
        AssertJUnit.assertTrue("Line number not found in error message: " + message, message.contains("28"));

    }

    @Test
    public void schemaViolation() throws Exception {
        OperationResult result = createOperationResult();

        validateFile("three-users-schema-violation.xml", result);

        System.out.println(result.debugDump());
        assertFalse(result.isSuccess());
        assertTrue(result.getSubresults().get(0).getMessage().contains("Invalid content was found starting with element 'foo'") ||
                result.getSubresults().get(0).getMessage().contains("Invalid content was found starting with element '{\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\":foo}'"));
        assertTrue(result.getSubresults().get(1).getMessage().contains("Invalid content was found starting with element 'givenName'") ||
                result.getSubresults().get(1).getMessage().contains("Invalid content was found starting with element '{\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\":givenName}'"));
        assertTrue(result.getSubresults().get(2).getMessage().contains("Invalid content was found starting with element 'fullName'") ||
                result.getSubresults().get(2).getMessage().contains("Invalid content was found starting with element '{\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\":fullName}'"));
    }

    /**
     * Same data as schemaViolation test, but this will set s lower threshold to stop after just two erros.
     */
    @Test
    public void testStopOnErrors() throws Exception {
        OperationResult result = createOperationResult();

        LegacyValidator validator = new LegacyValidator(PrismTestUtil.getPrismContext());
        validator.setVerbose(false);
        validator.setStopAfterErrors(2);

        validateFile("three-users-schema-violation.xml", validator, result);

        System.out.println(result.debugDump());
        assertFalse(result.isSuccess());
        assertEquals(2, result.getSubresults().size());
    }

    @Test
    public void noName() throws Exception {
        OperationResult result = createOperationResult();

        validateFile("no-name.xml", result);

        System.out.println(result.debugDump());
        AssertJUnit.assertFalse(result.isSuccess());
        AssertJUnit.assertTrue(result.getSubresults().get(0).getSubresults().get(1).getMessage().contains("Null property"));
        AssertJUnit.assertTrue(result.getSubresults().get(0).getSubresults().get(1).getMessage().contains("name"));

    }

    private void validateFile(String filename, OperationResult result) throws FileNotFoundException {
        validateFile(filename, (EventHandler) null, result);
    }

    private void validateFile(String filename, EventHandler handler, OperationResult result) throws FileNotFoundException {
        LegacyValidator validator = new LegacyValidator(PrismTestUtil.getPrismContext());
        if (handler != null) {
            validator.setHandler(handler);
        }
        validator.setVerbose(false);
        validateFile(filename, validator, result);
    }

    private void validateFile(String filename, LegacyValidator validator, OperationResult result) throws FileNotFoundException {
        String filepath = BASE_PATH + filename;

        display("Validating " + filename);

        File file = new File(filepath);
        FileInputStream fis = new FileInputStream(file);

        validator.validate(fis, result, OBJECT_RESULT_OPERATION_NAME);

        if (!result.isSuccess()) {
            displayDumpable("Errors:", result);
        } else {
            displayDumpable("No errors:", result);
        }
    }
}
