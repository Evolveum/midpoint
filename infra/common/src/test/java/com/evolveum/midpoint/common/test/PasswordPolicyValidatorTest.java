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

import org.junit.Test;

import com.evolveum.midpoint.common.password.PasswordPolicyException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;

public class PasswordPolicyValidatorTest {

	public PasswordPolicyValidatorTest() {
	}

	public static final String BASE_PATH = "src/test/resources/";

	@Test
	public void synteticValidatorTest() throws PasswordPolicyException {

		PasswordPolicyType pp = new PasswordPolicyType();
		com.evolveum.midpoint.common.password.Utils.initialize(pp);
		com.evolveum.midpoint.common.password.Utils.generatePassword(pp);
		
	}
		
/*
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