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

import static org.junit.Assert.*;

public class PasswordPolicyValidatorTest {

	public PasswordPolicyValidatorTest() {
	}

	public static final String BASE_PATH = "src/test/resources/";
/*
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
		pp.getComplexity().setLimitLowers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setLimitUppers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setLimitNumbers(new LimitationsType());
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setLimitSpecial(new LimitationsType());
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
		pp.getComplexity().getLimitLowers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitUppers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitNumbers().setCanBeFirst(false);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitSpecial().setCanBeFirst(false);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitSpecial().setCanBeFirst(true);
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
		pp.getComplexity().getLimitNumbers().setMinOccurence(2);
		pp.getComplexity().getLimitLowers().setMinOccurence(2);
		pp.getComplexity().getLimitUppers().setMinOccurence(2);
		pp.getComplexity().getLimitSpecial().setMinOccurence(2);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setMaxSize(8);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// consitency test for sum(max) >= min
		pp.getComplexity().setMinSize(9);
		pp.getComplexity().getLimitNumbers().setMaxOccurence(2);
		pp.getComplexity().getLimitLowers().setMaxOccurence(2);
		pp.getComplexity().getLimitUppers().setMaxOccurence(2);
		pp.getComplexity().getLimitSpecial().setMaxOccurence(2);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().setMinSize(8);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));

		// Class min <= max testing
		pp.getComplexity().getLimitNumbers().setMinOccurence(2);
		pp.getComplexity().getLimitNumbers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitNumbers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitLowers().setMinOccurence(2);
		pp.getComplexity().getLimitLowers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitLowers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitUppers().setMinOccurence(2);
		pp.getComplexity().getLimitUppers().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitUppers().setMaxOccurence(2);
		assertTrue(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitSpecial().setMinOccurence(2);
		pp.getComplexity().getLimitSpecial().setMaxOccurence(1);
		assertFalse(com.evolveum.midpoint.common.password.PolicyValidator.validatePolicy(pp));
		pp.getComplexity().getLimitSpecial().setMaxOccurence(2);
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
	
	@Test
	public void complexXMLPolicyTest() throws JAXBException {
		String filename = "password-policy-complex.xml";
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
	*/
}