/*
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.web;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Test that initial objects are parseable, correct, that they are not deprecated and so on.
 * 
 * @author Radovan Semancik
 */
public class TestInitialObjects extends AbstractGuiUnitTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestInitialObjects.class);
	
	private static final File DIR_INITIAL_OBJECTS = new File("src/main/resources/initial-objects");

    @Test
    public void testInitialObjects() throws Exception {
    	final String TEST_NAME = "testInitialObjects";
		displayTestTitle(TEST_NAME);
		
		ObjectValidator validator = new ObjectValidator(getPrismContext());
		validator.setAllWarnings();
		
		StringBuilder errorsSb = new StringBuilder();

		for (File file : DIR_INITIAL_OBJECTS.listFiles()) {
		    if (file.isFile()) {
		        try {
					testInitialObject(validator, errorsSb, file);
				} catch (Throwable e) {
					String msg = "Error processing file "+file.getName()+": "+e.getMessage();
					LOGGER.error(msg, e);
					display(msg, e);
					throw e;
				}
		    }
		}
		
		if (errorsSb.length() != 0) {
			throw new SchemaException(errorsSb.toString());
		}
    }

	private <O extends ObjectType> void testInitialObject(ObjectValidator validator, StringBuilder errorsSb, File file) throws SchemaException, IOException {
		PrismObject<O> object = getPrismContext().parseObject(file);
		ValidationResult validationResult = validator.validate(object);
		if (validationResult.isEmpty()) {
			display("Checked "+object+": no warnings");
			return;
		}
		display("Validation warnings for "+object, validationResult);
		for (ValidationItem valItem : validationResult.getItems()) {
			errorsSb.append(file.getName());
			errorsSb.append(" ");
			errorsSb.append(object);
			errorsSb.append(" ");
			valItem.shortDump(errorsSb);
			errorsSb.append("\n");
		}
	}
}
