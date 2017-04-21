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
package com.evolveum.midpoint.samples.test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.Assert;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Test validity of the samples in the trunk/samples directory.
 * 
 * @author Radovan Semancik
 *
 */
public class TestSamples {

	public static final String SAMPLES_DIRECTORY_NAME = "..";
	// TODO: FIXME: remove the "org" dir once the schema is updated
	public static final String[] IGNORE_PATTERNS = new String[]{ "\\.svn", "pom.xml", "old", 
		"experimental", "json", "misc", "rest", "samples-test", "model-.*", "bulk-actions",
		"testng.*\\.xml", "target", "extended-notifications\\.xml"};
	public static final String[] CHECK_PATTERNS = new String[]{ ".*.xml" };
	public static final String OBJECT_RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateObject";
	private static final String RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateFile";
	
	@BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }
	
	@Test
	public void testSamples() throws FileNotFoundException {
		testSamplesDirectory(new File(SAMPLES_DIRECTORY_NAME));
	}

	@Test(enabled = false)
	private void testSamplesDirectory(File directory) throws FileNotFoundException {
		String[] fileNames = directory.list();
		for (int i = 0; i < fileNames.length; i++) {
			String fileName = fileNames[i];
			if (matches(fileName,IGNORE_PATTERNS)) {
				// Ignore. Don't even dive inside
				continue;
			}
			File file = new File(directory, fileName);
			if (file.isFile() && matches(fileName,CHECK_PATTERNS)) {
				validate(file);
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
		
		Validator validator = new Validator(PrismTestUtil.getPrismContext());
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
