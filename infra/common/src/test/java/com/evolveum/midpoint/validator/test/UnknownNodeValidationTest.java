package com.evolveum.midpoint.validator.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

// TODO testing: missing from suite, all passing
public class UnknownNodeValidationTest extends AbstractUnitTest {

    public static final String BASE_PATH = "src/test/resources/validator/unknown/";
    private static final String OBJECT_RESULT_OPERATION_NAME = BasicValidatorTest.class.getName() + ".validateObject";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }


    @Test
    public void attributeInReference() throws Exception {
        validateNodeFailure("AttributeInReference", "reference-attribute.xml",
                "Attribute 'undeclared' is not allowed");
    }

    @Test
    public void attributeInObject() throws Exception {
        validateNodeFailure("AttributeInReference", "object-attribute.xml",
                "Attribute 'undeclared' is not allowed");
    }

    @Test
    public void attributeInConnector() throws Exception {
        validateNodeFailure("AttributeInConnector", "connector-attribute.xml",
                "Attribute 'undeclared' is not allowed");
    }

    @Test
    public void attributeInFilter() throws Exception {
        validateNodeFailure("AttributeInFilter", "filter-attribute.xml",
                "Attribute 'undeclared' is not allowed");
    }

    @Test
    public void attributeInProperty() throws Exception {
        validateNodeFailure("attributeInProperty", "property-attribute.xml",
                "Attribute 'undeclared' is not allowed");
    }

    @Test
    public void elementInReference() throws Exception {
        validateNodeFailure("ElementInReference", "reference-element.xml", "unknown");
    }

    @Test
    public void elementInFilter() throws Exception {
        validateNodeFailure("ElementInFilter", "filter-element.xml", "");
    }

    @Test
    public void elementInObject() throws Exception {
        validateNodeFailure("ElementInObject", "object-element.xml", "");
    }

    @Test
    public void elementInProperty() throws Exception {
        validateNodeFailure("ElementInProperty", "property-element.xml", "");
    }

    @Test
    public void elementInConnector() throws Exception {
        validateNodeFailure("ElementInConnector", "connector-element.xml", "");
    }

    protected void validateNodeFailure(String name, String file, String expected) throws Exception {
        OperationResult result = new OperationResult(this.getClass().getName()+"." + name);
        validateFile(file,result);
        System.out.println(result.debugDump());
        AssertJUnit.assertFalse("Result should not be successful", result.isSuccess());
        String message = result.getMessage();
        AssertJUnit.assertTrue(message.contains("undeclared"));
        //AssertJUnit.assertTrue(message.contains(expected));
    }

    protected void validateFile(String filename, OperationResult result) throws FileNotFoundException {
        validateFile(filename,(EventHandler) null,result);
    }

    protected void validateFile(String filename,EventHandler handler, OperationResult result) throws FileNotFoundException {
        LegacyValidator validator = new LegacyValidator(PrismTestUtil.getPrismContext());
        if (handler!=null) {
            validator.setHandler(handler);
        }
        validator.setVerbose(false);
        customizeValidator(validator);
        validateFile(filename, validator, result);
    }

    protected void customizeValidator(LegacyValidator validator) {

    }

    private void validateFile(String filename, LegacyValidator validator, OperationResult result) throws FileNotFoundException {

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
