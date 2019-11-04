/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.samples.test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import com.evolveum.midpoint.util.DOMUtil;
import org.testng.annotations.BeforeSuite;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Test validity of the samples in the trunk/samples directory.
 *
 * @author Radovan Semancik
 *
 */
public class TestSamples extends AbstractSampleTest {

    public static final String[] IGNORE_PATTERNS = new String[] {
        "META-INF", "experimental", "json", "misc", "rest", "samples-test", "model-.*", "bulk-actions", "bulk", "legacy", "audit"
    };
    public static final String[] CHECK_PATTERNS = new String[]{ ".*.xml" };
    public static final String OBJECT_RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateObject";
    private static final String RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateFile";

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testSamples() throws Exception {
        testSamplesDirectory(SAMPLES_DIRECTORY);
    }

    private void testSamplesDirectory(File directory) throws Exception {
        for (String fileName : Objects.requireNonNull(directory.list())) {
            if (matches(fileName,IGNORE_PATTERNS)) {
                // Ignore. Don't even dive inside
                continue;
            }
            File file = new File(directory, fileName);
            if (file.isFile() && matches(fileName,CHECK_PATTERNS)) {
                parse(file);
//                validate(file);
            }
            if (file.isDirectory()) {
                // Descend inside
                testSamplesDirectory(file);
            }
        }
    }

    private boolean matches(String s, String[] patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (s.matches(patterns[i])) {
                return true;
            }
        }
        return false;
    }

    private void parse(File file) throws IOException, SchemaException {
        System.out.println("===> DOM Parsing file "+file.getPath());
        Document dom = DOMUtil.parseFile(file);
        Element firstChildElement = DOMUtil.getFirstChildElement(dom);
        if (firstChildElement.getLocalName().equals("objects")) {
            parseObjectsElements(file, firstChildElement);
        } else {
            parseObjectFile(file);
        }
    }

    private void parseObjectsElements(File file, Element topElement) throws SchemaException {
        List<Element> objectElements = DOMUtil.listChildElements(topElement);
        for (int i = 0; i <  objectElements.size(); i++ ) {
            try {
                PrismObject<Objectable> object = prismContext
                        .parserFor(objectElements.get(i))
                        .strict()
                        .parse();
            } catch (SchemaException e) {
                throw new SchemaException("Error parsing "+file.getPath()+", element "+objectElements.get(i).getLocalName()+" (#"+(i+1)+"): "+e.getMessage(), e);
            }
        }
    }

    private void parseObjectFile(File file) throws IOException, SchemaException {
        System.out.println("===> Parsing file "+file.getPath());
        try {
            System.out.println("===> Parsing file " + file.getPath());
            PrismObject<Objectable> object = prismContext
                    .parserFor(file)
                    .strict()
                    .parse();
        } catch (Exception e) {
            throw new SchemaException("Error parsing "+file.getPath()+": "+e.getMessage(), e);
        }
    }


    private void validate(File file) throws FileNotFoundException {
        System.out.println("===> Validating file "+file.getPath());

        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
                    OperationResult objectResult) {

                // Try to marshall it back. This may detect some JAXB miscofiguration problems.
                try {
                    String serializedString = PrismTestUtil.serializeObjectToString(object, PrismContext.LANG_XML);
                } catch (SchemaException e) {
                    objectResult.recordFatalError("Object serialization failed", e);
                }

                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
                // no reaction
            }

        };

        LegacyValidator validator = new LegacyValidator(PrismTestUtil.getPrismContext());
        validator.setVerbose(false);
        validator.setAllowAnyType(true);
        validator.setHandler(handler);
        FileInputStream fis = new FileInputStream(file);
        OperationResult result = new OperationResult(RESULT_OPERATION_NAME);

        validator.validate(fis, result, OBJECT_RESULT_OPERATION_NAME);

        if (!result.isSuccess()) {
            // The error is most likely the first inner result. Therefore let's pull it out for convenience
            String errorMessage = result.getMessage();
            if (result.getSubresults()!=null && !result.getSubresults().isEmpty()) {
                if (result.getSubresults().get(0).getMessage() != null) {
                    errorMessage = result.getSubresults().get(0).getMessage();
                }
            }
            System.out.println("ERROR: "+errorMessage);
            System.out.println(result.debugDump());
            Assert.fail(file.getPath()+": "+errorMessage);
        } else {
            System.out.println("OK");
            //System.out.println(result.dump());
        }

        System.out.println();

    }

}
