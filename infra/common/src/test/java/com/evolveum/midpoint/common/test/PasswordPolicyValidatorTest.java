/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
 */

package com.evolveum.midpoint.common.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.junit.Test;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordComplexityType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordValidCharClassesType;

import static org.junit.Assert.*;

public class PasswordPolicyValidatorTest {

	public PasswordPolicyValidatorTest() {
	}

	public static final String BASE_PATH = "src/test/resources/";

	@Test
	public void synteticValidatorTest() {

		PasswordPolicyType pp = new PasswordPolicyType();
		pp.setName("Testing policy 1");
		assertNotNull(pp);

		// Complexity for classes testing
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(null));
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.setComplexity(new PasswordComplexityType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setLowers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setUppers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setNumbers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setSpecial(new LimitationsType());
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.setValidCharClasses(new PasswordValidCharClassesType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// test characters classes for minum characters
		pp.getValidCharClasses().setLowerChars("");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setLowerChars("abc");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setUpperChars("");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setUpperChars("ABC");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setNumberChars("");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setNumberChars("!@#");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setSpecialChars("");
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getValidCharClasses().setSpecialChars("123");

		// testing at least one can be first
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLowers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getUppers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getNumbers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getSpecial().setCanBeFirst(false);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getSpecial().setCanBeFirst(true);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// testing wrong definition of min max
		pp.getComplexity().setMaxSize(5);
		pp.getComplexity().setMinSize(6);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setMaxSize(6);
		pp.getComplexity().setMinSize(4);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// consitency test for sum(min) < max
		pp.getComplexity().setMaxSize(7);
		pp.getComplexity().getNumbers().setMinOccurence(2);
		pp.getComplexity().getLowers().setMinOccurence(2);
		pp.getComplexity().getUppers().setMinOccurence(2);
		pp.getComplexity().getSpecial().setMinOccurence(2);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setMaxSize(8);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// consitency test for sum(max) >= min
		pp.getComplexity().setMinSize(9);
		pp.getComplexity().getNumbers().setMaxOccurence(2);
		pp.getComplexity().getLowers().setMaxOccurence(2);
		pp.getComplexity().getUppers().setMaxOccurence(2);
		pp.getComplexity().getSpecial().setMaxOccurence(2);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setMinSize(8);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// Class min <= max testing
		pp.getComplexity().getNumbers().setMinOccurence(2);
		pp.getComplexity().getNumbers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getNumbers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLowers().setMinOccurence(2);
		pp.getComplexity().getLowers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLowers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getUppers().setMinOccurence(2);
		pp.getComplexity().getUppers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getUppers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getSpecial().setMinOccurence(2);
		pp.getComplexity().getSpecial().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getSpecial().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
	}

	@Test
	public void minimalXMLPolicyTest() throws JAXBException {
		String filename = "password-policy-minimal.xml";
		String pathname = BASE_PATH + filename;
		File file = new File(pathname);
		JAXBElement<PasswordPolicyType> jbe = (JAXBElement<PasswordPolicyType>) JAXBUtil.unmarshal(file);
		PasswordPolicyType pp = jbe.getValue();
		try { 
			com.evolveum.midpoint.common.password.PolicyValidator.validate(pp);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
	}
}