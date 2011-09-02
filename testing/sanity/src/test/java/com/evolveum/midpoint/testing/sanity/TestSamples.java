/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.sanity;

import org.testng.annotations.Test;
import org.testng.Assert;
import static org.testng.AssertJUnit.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.validator.Validator;

/**
 * Test validity of the samples in the trunk/samples directory.
 * 
 * @author Radovan Semancik
 *
 */
public class TestSamples {

	public static final String SAMPLES_DIRECTORY_NAME = "../../samples/";
	public static final String[] IGNORE_PATTERNS = new String[]{ "\\.svn", "old", "experimental" };
	public static final String[] CHECK_PATTERNS = new String[]{ ".*.xml" };
	public static final String OBJECT_RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateObject";
	private static final String RESULT_OPERATION_NAME = TestSamples.class.getName()+".validateFile";
	
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

		Validator validator = new Validator();
		validator.setVerbose(false);
        FileInputStream fis = new FileInputStream(file);
        OperationResult result = new OperationResult(RESULT_OPERATION_NAME);
        
		validator.validate(fis, result, OBJECT_RESULT_OPERATION_NAME);

        if (!result.isSuccess()) {
        	// The error is most likely the first inner result. Therefore let's pull it out for convenience
        	String errorMessage = result.getMessage();
        	if (result.getSubresults()!=null && !result.getSubresults().isEmpty()) {
        		errorMessage = result.getSubresults().get(0).getMessage();
        	}
        	System.out.println("ERROR: "+errorMessage);
        	System.out.println(result.dump());
        	Assert.fail(file.getPath()+": "+errorMessage);
        } else {
            System.out.println("OK");
            //System.out.println(result.dump());
        }
        
        System.out.println();
        
	}
	
}
